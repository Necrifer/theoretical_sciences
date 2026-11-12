"""
1.7.2: narrower console windows towards the end (design document, 10 November): a Field Synthesis step allows about a
third of what its console can reach, cosmic engineering a quarter, beyond physics a sixth, the final tier a tenth.

For every windowed recipe of those tiers this samples the console (all settings, the recipe's own variables), takes the
span each windowed quantity can reach, and shrinks the window to the tier's share of that span:
  * a threshold ("at least", "at most") keeps its bound, which is the physics, and gets a limit on its open side;
  * a band is shrunk round the middle of the settings that met the old windows;
  * a window that is already that narrow is left alone;
  * the new windows always lie inside the old ones, and a setting that meets all of them at once has been found.
A recipe whose windows the sampler cannot meet as they stand (they depend on the structure's coils or tiers) is left alone.

Run:  python3 tools/r172_windows.py            (report only)
      python3 tools/r172_windows.py apply
"""
import glob, json, math, os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tsgen as T
import check_progression as CP

R = os.path.join(T.D, 'recipe')
SHARE = {1: 1 / 3.0, 2: 1 / 4.0, 3: 1 / 6.0, 4: 1 / 10.0}
# (not the two generators: their windows are what they need to burn at all, and a ceiling would only cap their output)
TIERS = {'matrioshka': 2, 'universe_forging': 2,
         'reality_compilation': 3, 'omnilex_simulation': 3, 'reality_breach': 3, 'concept_compression': 3, 'transfinite_computation': 3, 'axiom_distillation': 3,
         'transcendent_forging': 4, 'horizon_simulation': 4, 'pleroma_reconstitution': 4, 'eschaton_founding': 4}
# (the Pleroma Confluence's band is set by hand in round172.py: 3 to 5 blend times)
N = 5000


def sig(x, up):
    """Three significant figures, rounded outwards (down for a lower bound, up for an upper one)."""
    if x == 0 or not math.isfinite(x):
        return x
    e = math.floor(math.log10(abs(x))) - 2
    f = 10.0 ** e
    v = (math.ceil(x / f - 1e-9) if up else math.floor(x / f + 1e-9)) * f
    return round(v, max(0, -e))


_CODE = {}
WHY = ['']
COIL = [1]


def _ev(expr, env):
    c = _CODE.get(expr)
    if c is None:
        c = _CODE[expr] = compile(T.pyexpr(expr), '<expr>', 'eval')
    return eval(c, {}, env)


SC = {1: (9.2, 14.5), 2: (18.3, 30.0), 3: (92.0, 120.0), 4: (290.0, 400.0), 5: (4000.0, 5000.0)}      # PhysicsModel.scTc / scB0


def evaluate(model, params, rvars, coil=1, d=0.0):
    """tsgen.evaluate, with the expressions compiled once."""
    env = dict(T.FUNCS)
    env.update({'pi': math.pi, 'e': math.e})
    env.update({'d': d, 'T': 300.0, 'T_amb': 300.0, 'on': 1.0, 'size': 1.0, 'sky': 1.0, 'night': 0.0, 'sun': 0.5, 'moon': 0.5, 'rain': 0.0,
                'alt': 64.0, 'rfi': 0.0, 'coil': 1.0, 'emitter': 1.0, 'oc': 0.0, 'par': 1.0, 'thr': 1.0, 'T_c': 300.0, 'sc_Tc': 0.0, 'sc_B0': 0.0})
    tc, b0 = SC[coil]
    env.update({'coil': float(coil), 'emitter': float(min(coil, 3)), 'sc_Tc': tc, 'sc_B0': b0, 'T': min(300.0, 0.5 * tc)})
    env.update(rvars)
    env.update(params)
    for c in model.get('consts', []):
        env[c['id']] = _ev(c['expr'], env)
    for x in model['derived']:
        env[x['id']] = _ev(x['expr'], env)
    ok = all(_ev(c['expr'], env) > 0 for c in model['constraints'])
    return env, ok, _ev(model['rate'], env)


def samples(model, rv, wins, coil=1):
    rng = random.Random(1720)
    out = []
    for i in range(N * 4):
        p = {}
        for q in model['params']:
            v = q['default'] if i == 0 else rng.uniform(q['min'], q['max'])
            p[q['id']] = round(v / q['step']) * q['step']
        try:
            env, ok, rate = evaluate(model, p, rv, coil)
        except (ValueError, ZeroDivisionError, OverflowError):
            continue
        except (NameError, TypeError) as ex:
            WHY[0] = str(ex)[:80]
            return None                                    # the console uses something only the game knows (its states, its structure)
        if not ok or rate <= 0:
            continue
        try:
            out.append((p, [float(env[w['q']]) for w in wins], rate))
        except (KeyError, TypeError) as ex:
            WHY[0] = 'window on %s' % str(ex)[:60]
            return None
        if len(out) >= N:
            break
    return out


def narrow(model, recipe, share):
    wins = recipe['physics']['windows']
    rv = dict(model.get('recipe_vars', {}))
    rv.update(recipe['physics'].get('vars', {}))
    if any(w['q'] in [st.get('id') for st in model.get('states', [])] for w in wins):
        return None, 'a window on something that changes as the machine runs'
    S = P = None
    WHY[0] = ''
    for coil in (1, 2, 3, 4, 5):                           # the lowest coil tier at which the windows can be met as they stand
        S = samples(model, rv, wins, coil)
        if not S or len(S) < 100:
            continue
        P = [s for s in S if all(w['min'] <= s[1][k] <= w['max'] for k, w in enumerate(wins))]
        if len(P) >= 3:
            COIL[0] = coil
            break
    if not S or len(S) < 100:
        return None, 'the sampler cannot run this console (%s)' % WHY[0]
    if not P or len(P) < 3:
        return None, 'depends on the size of the structure: not sampled'
    new = []
    plan = []
    for k, w in enumerate(wins):
        vals = sorted(s[1][k] for s in S)
        lo, hi = vals[int(len(vals) * 0.02)], vals[int(len(vals) * 0.98) - 1]
        log = lo > 0 and hi / lo > 50
        f = (lambda x: math.log10(max(x, 1e-300))) if log else (lambda x: x)
        g = (lambda x: 10.0 ** x) if log else (lambda x: x)
        span = f(hi) - f(lo)
        want = share * span
        L, H = max(w['min'], lo), min(w['max'], hi)
        open_up, open_down = w['max'] >= hi, w['min'] <= lo
        pv = sorted(s[1][k] for s in P)
        mid = pv[len(pv) // 2]
        plan.append(dict(k=k, f=f, g=g, want=want, L=L, H=H, up=open_up, down=open_down, mid=mid, span=span, lo=lo, hi=hi))
    for widen in (1.0, 1.3, 1.7, 2.2, 3.0, 4.0, 6.0, 10.0, 1e9):
        new = []
        for pl, w in zip(plan, wins):
            f, g = pl['f'], pl['g']
            width = pl['want'] * widen
            if span_ok(pl) and f(pl['H']) - f(pl['L']) <= width * 1.05 or pl['span'] <= 0:
                new.append((w['min'], w['max']))                   # already this narrow
            elif pl['up'] and pl['down']:
                new.append((w['min'], w['max']))                   # no window in effect at all: leave it
            elif pl['up']:                                         # "at least": the bound keeps its meaning, and gets a ceiling
                new.append((w['min'], min(w['max'], g(f(pl['L']) + width))))
            elif pl['down']:                                       # "at most": it gets a floor
                new.append((max(w['min'], g(f(pl['H']) - width)), w['max']))
            else:
                c = min(max(f(pl['mid']), f(pl['L']) + width / 2), f(pl['H']) - width / 2)
                new.append((max(w['min'], g(c - width / 2)), min(w['max'], g(c + width / 2))))
        new = [(sig(a, False) if a != w['min'] else a, sig(b, True) if b != w['max'] else b) for (a, b), w in zip(new, wins)]
        new = [(max(a, w['min']), min(b, w['max'])) for (a, b), w in zip(new, wins)]
        hit = [s for s in P if all(a <= s[1][k] <= b for k, (a, b) in enumerate(new))]
        steady = 0
        for s in hit[:60]:                                 # and it has to hold while the disturbance swings (an auto-calibrated one: +-0.3)
            try:
                ok = True
                for dd in (-0.3, 0.3):
                    env, good, rate = evaluate(model, s[0], rv, COIL[0], dd)
                    ok = ok and good and rate > 0 and all(a <= float(env[w['q']]) <= b for w, (a, b) in zip(wins, new))
                steady += ok
            except (ValueError, ZeroDivisionError, OverflowError):
                pass
        if steady >= 1 and len(hit) >= 2:
            return new, ('share x%.1f' % widen if widen > 1 else '')
    return None, 'no setting found'


def span_ok(pl):
    return pl['span'] > 0


def run(apply):
    mach = CP.machines()
    jobs = []
    for f in sorted(glob.glob(os.path.join(R, '**', '*.json'), recursive=True)):
        rtype = os.path.basename(os.path.dirname(f))
        rid = os.path.basename(f)[:-5]
        tier = 1 if rid.startswith('r170_') and rtype not in TIERS and rtype != 'crafting' else TIERS.get(rtype)
        if not tier:
            continue
        r = json.load(open(f, encoding='utf-8'))
        if not r.get('physics', {}).get('windows') or r.get('neoforge:conditions'):
            continue
        jobs.append((tier, rtype, rid, f, r))
    changed = left = 0
    op = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'r172_windows_orig.json')
    ORIG = json.load(open(op)) if os.path.exists(op) else {}
    rows = []
    for tier, rtype, rid, f, r in jobs:
        mid = next((m for m, _ in mach.get(rtype, []) if os.path.exists(os.path.join(T.D, 'ts_physics', m + '.json'))), None)
        if not mid:
            continue
        model = json.load(open(os.path.join(T.D, 'ts_physics', mid + '.json'), encoding='utf-8'))
        old = [(w['q'], w['min'], w['max']) for w in r['physics']['windows']]
        key = rtype + '/' + rid
        if key not in ORIG:
            ORIG[key] = [[w['min'], w['max']] for w in r['physics']['windows']]
        orig = ORIG[key]
        if orig:                                            # (run again: start from the windows as they were at 1.7.1)
            for w, o in zip(r['physics']['windows'], orig):
                w['min'], w['max'] = o[0], o[1]
            old = [(w['q'], w['min'], w['max']) for w in r['physics']['windows']]
        new, note = narrow(model, r, SHARE[tier])
        if new is None:
            left += 1
            rows.append((tier, rtype, rid, old, None, note))
            continue
        if all((a, b) == (o[1], o[2]) for (a, b), o in zip(new, old)):
            rows.append((tier, rtype, rid, old, 'same', note))
            continue
        changed += 1
        rows.append((tier, rtype, rid, old, new, note))
        if apply:
            for w, (a, b) in zip(r['physics']['windows'], new):
                w['min'], w['max'] = a, b
            T.dump(f, r)
    if apply:
        json.dump(ORIG, open(op, 'w'), indent=0, sort_keys=True)
    for tier, rtype, rid, old, new, note in rows:
        if new is None:
            print('E%d %-24s %-40s left alone: %s' % (tier, rtype, rid, note))
        elif new == 'same':
            print('E%d %-24s %-40s already narrow  %s' % (tier, rtype, rid, ', '.join('%s %g..%g' % o for o in old)))
        else:
            print('E%d %-24s %-40s %s  %s' % (tier, rtype, rid, '; '.join('%s %g..%g -> %g..%g' % (o[0], o[1], o[2], a, b) for o, (a, b) in zip(old, new)), note))
    print('windowed recipes: %d; narrowed: %d; left alone: %d' % (len(rows), changed, left))
    json.dump([dict(tier=t, type=a, id=b, old=o, new=(n if isinstance(n, list) else None), note=x) for t, a, b, o, n, x in rows],
              open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'r172_windows.json'), 'w'), indent=0)


if __name__ == '__main__':
    run('apply' in sys.argv)
