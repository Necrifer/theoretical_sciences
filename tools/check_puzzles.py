"""Checks every research puzzle's numeric answer the way the game computes it (ResearchPuzzle.eval: tokens split on
spaces; numbers, pi, names from 'given', + - * / ^, parentheses, and sqrt/ln/exp/sin/cos followed by a separate '(')."""
import json, glob, math, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, '..', 'src/main/resources/data/theoreticalsciences/ts_research')


class P:
    def __init__(s, t, env): s.t, s.env, s.i = t, env, 0
    def peek(s): return s.t[s.i] if s.i < len(s.t) else None
    def eat(s, x=None):
        v = s.peek()
        if v is None or (x is not None and v != x): raise ValueError('expected %s at %d got %s' % (x, s.i, v))
        s.i += 1; return v
    def expr(s):
        v = s.term()
        while s.peek() in ('+', '-'): v = v + s.term() if s.eat() == '+' else v - s.term()
        return v
    def term(s):
        v = s.unary()
        while s.peek() in ('*', '/'): v = v * s.unary() if s.eat() == '*' else v / s.unary()
        return v
    def unary(s):
        if s.peek() == '-': s.eat(); return -s.unary()
        return s.power()
    def power(s):
        b = s.atom()
        if s.peek() == '^': s.eat(); return b ** s.unary()
        return b
    def atom(s):
        x = s.peek()
        if x is None: raise ValueError('unexpected end')
        if re.fullmatch(r'\d+(\.\d+)?', x): s.eat(); return float(x)
        if x == 'π': s.eat(); return math.pi
        if x == '(':
            s.eat(); v = s.expr(); s.eat(')'); return v
        if x in ('sqrt', 'ln', 'exp', 'sin', 'cos'):
            s.eat(); s.eat('('); a = s.expr(); s.eat(')')
            return {'sqrt': math.sqrt, 'ln': math.log, 'exp': math.exp, 'sin': math.sin, 'cos': math.cos}[x](a)
        if x not in s.env: raise ValueError('unknown ' + x)
        s.eat(); return s.env[x]


def evaluate(expr, env):
    t = expr.strip().split()
    p = P(t, env)
    v = p.expr()
    if p.i != len(t): raise ValueError('trailing tokens at ' + str(t[p.i:]))
    if not math.isfinite(v): raise ValueError('not finite')
    return v


def main(only=None):
    bad = 0
    for f in sorted(glob.glob(os.path.join(R, '*.json'))):
        rid = os.path.basename(f)[:-5]
        if only and rid not in only: continue
        d = json.load(open(f, encoding='utf-8'))
        pz = d.get('puzzle')
        if not pz: continue
        try:
            v = evaluate(pz['expr'], {k: float(x) for k, x in pz.get('given', {}).items()})
            # (a draw from 'vary' that has no finite answer is drawn again in game, so only the book values must work)
        except Exception as e:
            bad += 1
            print('BROKEN', rid, ':', pz['expr'], '->', e)
    print('puzzles checked; broken:', bad)
    return bad


if __name__ == '__main__':
    sys.exit(1 if main(set(sys.argv[1:]) or None) else 0)
