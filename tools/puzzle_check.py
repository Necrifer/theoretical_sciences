"""
Research puzzles: format, parser and validator. The Java side (research/ResearchPuzzle.java) implements the same
grammar and the same numeric equivalence test.

A puzzle (tools/puzzles/<group>.json, keyed by research id):
  goal         what to find, one sentence
  lhs          the quantity being solved for (shown left of the '=')
  expr         the reference right-hand side, as SPACE-SEPARATED tokens (these are exactly the tiles the player gets)
  vars         every variable token -> what it means (revealed by the 'variables' hint)
  given        a value for every variable (the 'solve' step evaluates expr with these)
  distractors  extra tiles that are not needed (2-5)
  method       one or two sentences on how to derive it (revealed by the 'method' hint)
  unit         optional unit for the answer

Grammar: numbers, variables, + - * / ^ ( ), functions sqrt ln exp sin cos (followed by a parenthesised argument),
the constant π. Implicit multiplication is not allowed. ^ is right-associative; unary minus is allowed.
"""
import json, glob, math, os, random, re, sys

FUNCS = {'sqrt': math.sqrt, 'ln': math.log, 'exp': math.exp, 'sin': math.sin, 'cos': math.cos}
OPS = set('+-*/^()')


def kind(t):
    if re.fullmatch(r'\d+(\.\d+)?', t): return 'num'
    if t in OPS: return t
    if t in FUNCS: return 'fn'
    if t == 'π': return 'pi'
    return 'var'


class P:
    def __init__(self, toks, env):
        self.t, self.i, self.env = toks, 0, env

    def peek(self):
        return self.t[self.i] if self.i < len(self.t) else None

    def eat(self, x=None):
        t = self.peek()
        if t is None or (x is not None and t != x): raise ValueError(f'expected {x} at {self.i}, got {t}')
        self.i += 1
        return t

    def expr(self):
        v = self.term()
        while self.peek() in ('+', '-'):
            v = v + self.term() if self.eat() == '+' else v - self.term()
        return v

    def term(self):
        v = self.unary()
        while self.peek() in ('*', '/'):
            if self.eat() == '*': v *= self.unary()
            else: v /= self.unary()
        return v

    def unary(self):
        if self.peek() == '-':
            self.eat()
            return -self.unary()
        return self.power()

    def power(self):
        b = self.atom()
        if self.peek() == '^':
            self.eat()
            return b ** self.unary()
        return b

    def atom(self):
        t = self.peek()
        if t is None: raise ValueError('unexpected end')
        k = kind(t)
        if k == 'num': self.eat(); return float(t)
        if k == 'pi': self.eat(); return math.pi
        if k == 'var': self.eat(); return self.env[t]
        if k == 'fn':
            self.eat(); self.eat('(')
            v = self.expr(); self.eat(')')
            return FUNCS[t](v)
        if t == '(':
            self.eat(); v = self.expr(); self.eat(')')
            return v
        raise ValueError(f'unexpected {t}')


def evaluate(toks, env):
    p = P(toks, env)
    v = p.expr()
    if p.i != len(toks): raise ValueError('trailing tokens')
    if isinstance(v, complex) or not math.isfinite(v): raise ValueError('not finite')
    return v


def check(rid, z, errs):
    toks = z['expr'].split()
    vs = {t for t in toks if kind(t) == 'var'}
    for k in ('goal', 'lhs', 'expr', 'vars', 'given', 'method'):
        if k not in z: errs.append(f'{rid}: missing {k}')
    if vs - set(z.get('vars', {})): errs.append(f'{rid}: undeclared variables {vs - set(z["vars"])}')
    if set(z.get('vars', {})) - vs: errs.append(f'{rid}: declared but unused variables {set(z["vars"]) - vs}')
    if set(z.get('given', {})) != vs: errs.append(f'{rid}: given must cover exactly the variables')
    if len(toks) > 22: errs.append(f'{rid}: too long ({len(toks)} tokens, max 22)')
    if not 1 <= len(z.get('distractors', [])) <= 6: errs.append(f'{rid}: 1-6 distractors')
    for t in z.get('distractors', []):
        if ' ' in t: errs.append(f'{rid}: distractor with a space')
    for t in toks + z.get('distractors', []):
        if kind(t) == 'var' and (len(t) > 4 or not t.strip()): errs.append(f'{rid}: bad variable token {t!r}')
    try:
        a = evaluate(toks, {k: float(v) for k, v in z['given'].items()})
        if abs(a) < 1e-12 or abs(a) > 1e12: errs.append(f'{rid}: answer {a} out of range')
        z['_answer'] = a
    except Exception as e:
        errs.append(f'{rid}: cannot evaluate with given values: {e}')
    ok = 0
    rng = random.Random(1)
    for _ in range(8):
        try:
            evaluate(toks, {k: rng.uniform(0.3, 3.0) for k in vs}); ok += 1
        except Exception:
            pass
    if ok < 5: errs.append(f'{rid}: fails at random positive values ({ok}/8 ok) - the equivalence test needs a domain of positive values')


def load_all():
    out = {}
    for f in sorted(glob.glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'puzzles', '*.json'))):
        out.update(json.load(open(f)))
    return out


if __name__ == '__main__':
    files = [a for a in sys.argv[1:] if not a.startswith('-')] or sorted(glob.glob(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'puzzles', '*.json')))
    errs = []
    n = 0
    for f in files:
        for rid, z in json.load(open(f)).items():
            n += 1
            check(rid, z, errs)
            if '_answer' in z and '-v' in sys.argv: print(f'{rid:30s} {z["lhs"]} = {z["expr"]}  ->  {z["_answer"]:.6g}')
    print('\n'.join(errs) if errs else f'{n} puzzles OK')
    sys.exit(1 if errs else 0)
