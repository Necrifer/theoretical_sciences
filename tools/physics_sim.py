"""
Python twin of the Java machine-physics runtime (machine/physics/Expr, PhysicsModel, PhysicsState).

  * parse(expr)               - same grammar and functions as Expr.java
  * Model(json)               - same evaluation order as PhysicsModel / PhysicsState.evaluate
  * Model.run(...)            - simulates ticks exactly like the server (temperature relaxation, coolant, states)
  * solve(model, recipe, ...) - searches the operator's parameters for a working point (used to prove that every recipe
                                can be run with the coils and coolants available when it unlocks)
"""
import math
import random
import re

BUILTINS = ['T', 'T_c', 'T_amb', 'coil', 'emitter', 'sc_Tc', 'sc_B0', 'oc', 'par', 'thr', 'd', 'on', 'size',
            'sky', 'night', 'sun', 'moon', 'rain', 'alt', 'rfi']
# 1.4.0: the world around the machine, judged at its worst ordinary case - a clear night under a full Moon at sea level,
# a morning Sun, two machines running nearby - so every recipe works on any clear night or day
WORLD = dict(sky=1.0, night=1.0, sun=0.7, moon=1.0, rain=0.0, alt=64.0, rfi=2.0)
SC_TC = {1: 9.2, 2: 18.3, 3: 92.0, 4: 290.0, 5: 4000.0}
SC_B0 = {1: 14.5, 2: 30.0, 3: 120.0, 4: 400.0, 5: 5000.0}
NAN = float('nan')
TOK = re.compile(r'\s*(?:(\d+\.?\d*(?:[eE][+-]?\d+)?|\.\d+(?:[eE][+-]?\d+)?)|([A-Za-z_][A-Za-z0-9_]*)|([-+*/^(),]))')
ARITY = {f: 1 for f in 'sqrt cbrt ln log10 exp sin cos tan atan tanh abs pos step'.split()}
ARITY.update(min=2, max=2, clamp=3)


def lex(s):
    out, i = [], 0
    s = s.strip()
    while i < len(s):
        m = TOK.match(s, i)
        if not m or m.end() == i:
            raise ValueError(f'bad character at {i} in {s!r}')
        out.append(m.group(m.lastindex))
        i = m.end()
        while i < len(s) and s[i].isspace():
            i += 1
    return out


def _nan(*xs):
    return any(x != x for x in xs)


def _f(name, args):
    """Same results as Expr.java (Java Math semantics: NaN propagates through min/max/clamp/pos, exp overflows to inf)."""
    x = args[0]
    if name == 'neg': return -x
    if name in ('min', 'max', 'clamp', 'pos') and _nan(*args): return NAN
    if x != x and name != 'step': return NAN
    try:
        if name == 'sqrt': return math.sqrt(x) if x >= 0 else NAN
        if name == 'cbrt':
            if math.isinf(x) or x == 0: return x
            r = math.copysign(abs(x) ** (1 / 3), x)
            r = r - (r * r * r - x) / (3 * r * r)          # one Newton step: exact cubes come out exact, like Math.cbrt
            return r
        if name == 'ln': return math.log(x) if x > 0 else (NAN if x < 0 else -math.inf)
        if name == 'log10': return math.log10(x) if x > 0 else (NAN if x < 0 else -math.inf)
        if name == 'exp':
            try:
                return math.exp(x)
            except OverflowError:
                return math.inf
        if name == 'sin': return math.sin(x) if math.isfinite(x) else NAN
        if name == 'cos': return math.cos(x) if math.isfinite(x) else NAN
        if name == 'tan': return math.tan(x) if math.isfinite(x) else NAN
        if name == 'atan': return math.atan(x)
        if name == 'tanh': return math.tanh(x)
        if name == 'abs': return abs(x)
        if name == 'pos': return max(0.0, x)
        if name == 'step': return 1.0 if x >= 0 else 0.0
        if name == 'min': return min(x, args[1])
        if name == 'max': return max(x, args[1])
        if name == 'clamp': return max(args[1], min(args[2], x))
    except (ValueError, OverflowError):
        return NAN
    return NAN


def _pow(a, b):
    """java.lang.Math.pow semantics."""
    if b != b: return NAN
    if b == 0: return 1.0
    if a != a: return NAN
    try:
        return math.pow(a, b)
    except OverflowError:
        neg = a < 0 and float(b).is_integer() and int(b) % 2 == 1
        return -math.inf if neg else math.inf
    except ValueError:
        if a == 0 and b < 0:
            neg = math.copysign(1, a) < 0 and float(b).is_integer() and int(b) % 2 == 1
            return -math.inf if neg else math.inf
        return NAN


def _div(a, b):
    if b == 0:
        if a == 0 or a != a: return NAN
        return math.copysign(math.inf, a) * math.copysign(1, b)
    return a / b


def parse(src, names):
    """Compiles to a Python closure over an env dict; raises on unknown names (like Expr.java)."""
    t = lex(src)
    i = [0]

    def peek():
        return t[i[0]] if i[0] < len(t) else None

    def eat(x=None):
        s = peek()
        if s is None or (x is not None and s != x): raise ValueError(f'expected {x!r} in {src!r}')
        i[0] += 1
        return s

    def expr():
        v = term()
        while peek() in ('+', '-'):
            op = eat()
            a, b = v, term()
            v = (lambda a, b: lambda e: a(e) + b(e))(a, b) if op == '+' else (lambda a, b: lambda e: a(e) - b(e))(a, b)
        return v

    def term():
        v = unary()
        while peek() in ('*', '/'):
            op = eat()
            a, b = v, unary()
            v = (lambda a, b: lambda e: a(e) * b(e))(a, b) if op == '*' else (lambda a, b: lambda e: _div(a(e), b(e)))(a, b)
        return v

    def unary():
        if peek() == '-':
            eat()
            a = unary()
            return lambda e: -a(e)
        return power()

    def power():
        b = atom()
        if peek() == '^':
            eat()
            x = unary()
            return lambda e: _pow(b(e), x(e))
        return b

    def atom():
        s = peek()
        if s is None: raise ValueError(f'unexpected end of {src!r}')
        if s[0].isdigit() or s[0] == '.':
            eat()
            v = float(s)
            return lambda e: v
        if s == '(':
            eat()
            v = expr()
            eat(')')
            return v
        if s[0].isalpha() or s[0] == '_':
            eat()
            if peek() == '(':
                eat()
                args = [expr()]
                while peek() == ',':
                    eat()
                    args.append(expr())
                eat(')')
                if s not in ARITY: raise ValueError(f'unknown function {s} in {src!r}')
                if ARITY[s] != len(args): raise ValueError(f'{s} takes {ARITY[s]} args in {src!r}')
                return lambda e: _f(s, [a(e) for a in args])
            if s == 'pi': return lambda e: math.pi
            if s not in names: raise ValueError(f'unknown name {s!r} in {src!r}')
            return lambda e: e[s]
        raise ValueError(f'unexpected {s!r} in {src!r}')

    v = expr()
    if i[0] != len(t): raise ValueError(f'trailing {t[i[0]]!r} in {src!r}')
    return v


def names_in(src):
    return {x for x in lex(src) if (x[0].isalpha() or x[0] == '_') and x not in ARITY and x != 'pi'}


class Model:
    def __init__(self, machine, j):
        self.machine, self.j = machine, j
        order = list(BUILTINS)
        self.params = j.get('params', [])
        order += [p['id'] for p in self.params]
        self.rvars = j.get('recipe_vars', {})
        order += list(self.rvars)
        self.states = j.get('states', [])
        order += [s['id'] for s in self.states]
        self.consts = j.get('consts', [])
        order += [c['id'] for c in self.consts]
        self.derived = j.get('derived', [])
        order += [d['id'] for d in self.derived]
        if len(set(order)) != len(order):
            dup = [x for x in order if order.count(x) > 1]
            raise ValueError(f'{machine}: duplicate names {set(dup)}')
        self.names = set(order)
        # strict ordering: consts see builtins/params/recipe vars/earlier consts; state inits see consts too;
        # derived see everything before them; constraints, rate, heat see everything
        early = set(BUILTINS) | {p['id'] for p in self.params} | set(self.rvars)
        seen = set(early)
        for c in self.consts:
            bad = names_in(c['expr']) - seen
            if bad: raise ValueError(f'{machine}: const {c["id"]} uses {bad} before they exist')
            seen.add(c['id'])
        for s in self.states:
            bad = names_in(s['init']) - seen
            if bad: raise ValueError(f'{machine}: state {s["id"]} init uses {bad}')
        seen |= {s['id'] for s in self.states}
        for d in self.derived:
            bad = names_in(d['expr']) - seen
            if bad: raise ValueError(f'{machine}: derived {d["id"]} uses {bad} before they exist')
            seen.add(d['id'])
        P = lambda s: parse(s, self.names)
        self.c_consts = [(c['id'], P(c['expr'])) for c in self.consts]
        self.c_init = [(s['id'], P(s['init'])) for s in self.states]
        self.c_ddt = [(s['id'], P(s['ddt'])) for s in self.states]
        self.c_derived = [(d['id'], P(d['expr'])) for d in self.derived]
        self.c_cons = [(c['id'], c['kind'], P(c['expr'])) for c in j.get('constraints', [])]
        self.c_rate = P(j.get('rate', '1'))
        self.c_heat = P(j.get('heat', '0'))
        self.c_idle = P(j.get('idle_heat', '0'))
        self.c_qe = P(j.get('quench_energy', '0'))
        self.kappa = j.get('kappa', 40)
        self.C = j.get('heat_capacity', 40)
        self.timescale = j.get('timescale', 1)
        self.rate_max = j.get('rate_max', 4)
        assert len(self.params) <= 6, f'{machine}: at most 6 parameters fit the console'

    def evaluate(self, params, rvars, states, T, Tc, coil, emitter=1, oc=0, par=1, d=0.0, on=True):
        e = dict(T=T, T_c=Tc, T_amb=300.0, coil=coil, emitter=emitter, sc_Tc=SC_TC.get(coil, 1e-9), sc_B0=SC_B0.get(coil, 0.0),
                 oc=oc, par=par, thr=(1 << oc) * par, d=d, on=1.0 if on else 0.0,
                 size=float(getattr(self, 'size', 1)), **WORLD)
        for p in self.params: e[p['id']] = params[p['id']]
        for k, v in self.rvars.items(): e[k] = rvars.get(k, v)
        for k, f in self.c_consts: e[k] = f(e)
        if states is None:
            states = {k: f(e) for k, f in self.c_init}
        for k, v in states.items(): e[k] = v
        for k, f in self.c_derived: e[k] = f(e)
        cons = []
        for cid, kind, f in self.c_cons:
            v = f(e)
            cons.append((cid, kind, v))
        r = self.c_rate(e)
        rate = min(self.rate_max, max(0.0, r)) if math.isfinite(r) else 0.0
        h = self.c_heat(e) * (1 if on else 0) + self.c_idle(e)
        heat = max(0.0, h) if math.isfinite(h) else 0.0
        return e, states, cons, rate, heat

    def run(self, params, rvars, Tc, coil, seconds=120, d=0.0, emitter=1, oc=0, par=1, T0=None):
        """Simulates the machine running with coolant at Tc. Returns (env, cons, rate, heat, T, events)."""
        T = Tc if T0 is None else T0
        states = None
        events = []
        dt = 0.05
        e = cons = None
        rate = heat = 0
        ticks = int(seconds * 20)
        for k in range(ticks):
            e, states, cons, rate, heat = self.evaluate(params, rvars, states, T, Tc, coil, emitter, oc, par, d, True)
            # thermal (exact exponential relaxation, like PhysicsState.tickThermal)
            tau = self.C / self.kappa
            eq = Tc + heat / self.kappa
            T = eq + (T - eq) * math.exp(-dt / tau)
            if not math.isfinite(T): T = 300.0
            # states
            ns = dict(states)
            for (sid, f), sd in zip(self.c_ddt, self.states):
                dv = f(e) * dt * self.timescale
                if math.isfinite(dv): ns[sid] += dv
                lo, hi = sd.get('min', -1e308), sd.get('max', 1e308)
                if ns[sid] < lo or ns[sid] > hi:
                    events.append(('state', sid, k))
                    ns[sid] = min(hi, max(lo, ns[sid]))
            states = ns
            if k > ticks // 2:
                for cid, kind, v in cons:
                    if not (v >= 0) and kind in ('quench', 'damage', 'stop'):
                        events.append((kind, cid, k))
        return e, cons, rate, heat, T, events


def window_ok(e, windows):
    for w in windows:
        v = e.get(w['q'], NAN)
        if not (v >= w.get('min', -1e308) and v <= w.get('max', 1e308)): return False
    return True


HELD = {'M'}      # states the operator sets directly (unstable equilibria): not relaxed by the checker


def settle(model, params, rvars, T, Tc, coil, emitter, d):
    """Relaxes each state variable to the stable equilibrium reached from its initial value (following the sign of
    d/dt), like the machine would after running a while. Returns the state dict and the names that hit a bound."""
    e, st, cons, rate, heat = model.evaluate(params, rvars, None, T, Tc, coil, emitter, 0, 1, d, True)
    st = dict(st)
    hit = set()
    ddt = dict(model.c_ddt)

    def f(name, x):
        s2 = dict(st)
        s2[name] = x
        e2 = model.evaluate(params, rvars, s2, T, Tc, coil, emitter, 0, 1, d, True)[0]
        v = ddt[name](e2)
        return v if v == v else 0.0

    for _ in range(3):
        for sd in model.states:
            n = sd['id']
            if n in HELD: continue
            lo, hi = sd.get('min', -1e300), sd.get('max', 1e300)
            x = st[n]
            v = f(n, x)
            if v == 0: continue
            up = v > 0
            a = x
            step = max(abs(x) * 0.25, 1e-3)
            b = x
            found = False
            for _k in range(200):
                b = a + step if up else a - step
                if b >= hi or b <= lo:
                    b = min(hi, max(lo, b))
                    vb = f(n, b)
                    if (vb > 0) == up:
                        st[n] = b
                        hit.add(n)
                        break
                    found = True
                    break
                vb = f(n, b)
                if (vb > 0) != up:
                    found = True
                    break
                a = b
                step *= 1.3
            if found:
                for _k in range(60):
                    mid = 0.5 * (a + b)
                    if (f(n, mid) > 0) == up: a = mid
                    else: b = mid
                st[n] = 0.5 * (a + b)
    return st, hit


def score(model, params, rvars, windows, Tc, coil, d=0.0, emitter=1):
    """Steady-state check: temperature fixed point, states relaxed to equilibrium. Lower is better; 0 = all good."""
    T = Tc
    st = None
    hit = set()
    for _ in range(30):
        if model.states:
            st, hit = settle(model, params, rvars, T, Tc, coil, emitter, d)
        e, st2, cons, rate, heat = model.evaluate(params, rvars, st, T, Tc, coil, emitter, 0, 1, d, True)
        T2 = Tc + heat / model.kappa
        if abs(T2 - T) < 1e-6 * max(1, T): break
        T = 0.5 * T + 0.5 * T2
    bad = 0.0
    for sd in model.states:
        if sd['id'] in hit: bad += 3
    for cid, kind, v in cons:
        if not (v >= 0):
            bad += 10 if v != v else min(10, 1 + abs(v) / (1 + abs(v)))
    for w in windows:
        v = e.get(w['q'], NAN)
        lo, hi = w.get('min', -1e308), w.get('max', 1e308)
        if v != v: bad += 10
        elif v < lo: bad += 1 + min(5, (lo - v) / (abs(lo) + 1e-30))
        elif v > hi: bad += 1 + min(5, (v - hi) / (abs(hi) + 1e-30))
    if rate < 0.25: bad += 1 + (0.25 - rate)
    return bad, e, T, rate


def solve(model, rvars, windows, Tc, coil, d=0.0, iters=6000, seed=1, emitter=1, start=None):
    """Random + local search over the operator's parameters, always snapped to their steps (what a player can set)."""
    rnd = random.Random(seed)
    ps = model.params

    def snap(p, v):
        st = p.get('step', 1)
        if st > 0: v = round(v / st) * st
        return min(p['max'], max(p['min'], v))

    def wide(p):
        lo = max(p['min'], p.get('step', 1) if p['min'] <= 0 else p['min'])
        return lo > 0 and p['max'] / lo > 50, lo

    def rand_val(p):
        lg, lo = wide(p)
        if lg and rnd.random() < 0.7:
            return snap(p, math.exp(rnd.uniform(math.log(lo), math.log(p['max']))))
        return snap(p, rnd.uniform(p['min'], p['max']))

    best = dict(start) if start else {p['id']: snap(p, p['default']) for p in ps}
    bs = score(model, best, rvars, windows, Tc, coil, d, emitter)[0]
    for it in range(iters):
        if bs == 0: break
        if it % 4 == 0:
            cand = {p['id']: rand_val(p) for p in ps}
        else:
            cand = dict(best)
            for p in rnd.sample(ps, rnd.choice([1, 1, 2])):
                lg, lo = wide(p)
                v = cand[p['id']]
                if lg and v > 0 and rnd.random() < 0.6:
                    v = v * math.exp(rnd.gauss(0, rnd.choice([0.02, 0.2, 1.0])))
                else:
                    v = v + rnd.gauss(0, (p['max'] - p['min']) * rnd.choice([0.001, 0.01, 0.1]))
                cand[p['id']] = snap(p, v)
        s = score(model, cand, rvars, windows, Tc, coil, d, emitter)[0]
        if s < bs:
            best, bs = cand, s
    s2, e, T, rate = score(model, best, rvars, windows, Tc, coil, d, emitter)
    return best, s2, e, T, rate
