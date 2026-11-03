"""
Research puzzles, version 2 (round 13). The Java side (research/FormExpr.java, research/ResearchPuzzle.java,
client/screen/MathTypeset.java) implements exactly the same grammar and the same canonical comparison.

A puzzle (tools/puzzles/<group>.json, keyed by research id):
  goal         Step 1 text: what is happening physically / mathematically and what to derive. It must NOT name the law,
               equation, effect or person (no "Planck", "Schrödinger", "Navier-Stokes", "Casimir"...), so it cannot be
               looked up. Describe the situation instead.
  name         The formula's name, revealed once Step 1 is solved (e.g. "Stefan-Boltzmann law (from Planck's spectrum)").
  lhs          left-hand side, in FORM grammar (fixed, not filled by the player), e.g. "u", "pd ( ψ , t )", "F _ μν"
  form         the right-hand side in FORM grammar: the full, accurate form of the relation (integrals, derivatives,
               vectors, complex numbers, operators...). The template is drawn from it; every SLOT (see below) becomes a
               box the player fills with a tile.
  expr         Step 2: a closed-form expression in the old NUMERIC grammar (see puzzle_check.py) that gives the number
               the player must compute from `given` (e.g. the integral already done). Must use only the `given` names.
  ask          Step 2 text: what number to compute, e.g. "Find the energy density u for the values below."
  vars         every NAME used in form/lhs/expr -> what it means in this puzzle (a hint reveals these)
  given        values for every variable of expr
  unit         unit of the answer
  method       1-3 sentences on how to derive it (a hint reveals it). May name techniques, not the law.
  distractors  2-6 extra tiles (glyph tokens) that are not needed

FORM grammar - tokens separated by single spaces:
  expr    := term ( ('+' | '-') term )*
  term    := unary ( ('*' | '/' | '·' | '×') unary )*        '*' = ordinary product (printed implicitly),
                                                            '·' = dot product, '×' = cross product, '/' = fraction
  unary   := '-' unary | postfix
  postfix := atom ( '^' unary | '_' atom | '!' )*          '^' superscript/power, '_' subscript, '!' factorial
  atom    := NUMBER | LEAF | '(' expr ')' | FUNC '(' expr ')' | HEAD '(' expr ( ',' expr )* ')'
  NUMBER  := digits with optional decimals
  FUNC    := sin cos tan exp ln log sinh cosh tanh arcsin arccos arctan Re Im det tr erf Γ arg     (1 argument)
  HEAD    := structural constructs (fixed in the template, drawn as real notation):
     int(a,b,f,x) ∫ₐᵇ f dx      int(f,x) ∫ f dx       oint(f,x) ∮ f dx    iint(f,S) ∬_S f    iiint(f,V) ∭_V f
     sum(k,a,b,f) Σ_{k=a}^{b} f    prod(k,a,b,f) Π     sumk(k,f) Σ_k f      lim(x,a,f) lim_{x→a} f
     d(f,x) df/dx    d2(f,x) d²f/dx²    pd(f,x) ∂f/∂x    pd2(f,x) ∂²f/∂x²    pdm(f,x,y) ∂²f/∂x∂y
     grad(f) ∇f   div(F) ∇·F   curl(F) ∇×F   lap(f) ∇²f   dal(f) □f
     vec(x) x⃗   hat(x) x̂   bar(x) x̄   tdot(x) ẋ   conj(x) x*   dag(x) x†   abs(x) |x|   norm(x) ‖x‖
     bra(x) ⟨x|   ket(x) |x⟩   braket(a,b) ⟨a|b⟩   mel(a,O,b) ⟨a|O|b⟩   avg(O) ⟨O⟩   comm(A,B) [A,B]   acomm(A,B) {A,B}
     sqrt(x) √x   root(n,x) ⁿ√x   binom(n,k) (n k)   hc() "+ h.c."  (no-argument: written "hc ( )")
  LEAF    := any other token: a variable, constant or index (x, v, ħ, c, kB, π, i, e, ∞, μν, ψ, γ ...).
             A leaf's characters after the first are drawn as a subscript (kB -> k_B), except Greek index pairs
             like μν which are drawn whole. Use '_' for anything more elaborate.

SLOTS (boxes the player fills) are: every LEAF and NUMBER, every FUNC name, and every binary '+' / '-'.
Everything else (structure, fraction bars, powers, ∫, ∂, ∇, brackets...) is drawn as part of the template.

The player's answer is accepted when its canonical form equals the reference's: sums and ordinary products are
compared as unordered collections (so a+b = b+a, x·y = y·x for '*' and '·'), a − b is a + (−b), everything else is
positional. So any arrangement of the tiles that is the same expression up to commutation is accepted.

GLYPHS: every token that may appear on a tile or in a template is a glyph in the glossary (tools/glyphs/*.json):
  { "<token>": { "name": "...", "cat": "<category>", "meaning": "...", "unit": "...",
                 "unlock": {"start": true} | {"research": "<id>"} | {"item": "<item id>"} } }
A puzzle can only be attempted once every glyph in its lhs, form and expr... is unlocked (distractors that are still
locked are simply left out of the tray). The validator checks that each glyph unlocks strictly before every research
that needs it (by progression wave).
"""
import glob
import json
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))

FUNCS = {'sin', 'cos', 'tan', 'exp', 'ln', 'log', 'sinh', 'cosh', 'tanh', 'arcsin', 'arccos', 'arctan', 'Re', 'Im', 'det', 'tr', 'erf', 'Γ', 'arg'}
HEADS = {'int': (2, 4), 'oint': (2,), 'iint': (2,), 'iiint': (2,), 'sum': (4,), 'prod': (4,), 'sumk': (2,), 'lim': (3,),
         'd': (2,), 'd2': (2,), 'pd': (2,), 'pd2': (2,), 'pdm': (3,), 'grad': (1,), 'div': (1,), 'curl': (1,), 'lap': (1,), 'dal': (1,),
         'vec': (1,), 'hat': (1,), 'bar': (1,), 'tdot': (1,), 'conj': (1,), 'dag': (1,), 'abs': (1,), 'norm': (1,),
         'bra': (1,), 'ket': (1,), 'braket': (2,), 'mel': (3,), 'avg': (1,), 'comm': (2,), 'acomm': (2,),
         'sqrt': (1,), 'root': (2,), 'binom': (2,), 'hc': (0,)}
PUNCT = {'(', ')', ',', '^', '_', '!', '/', '*', '·', '×', '+', '-'}
NUM = re.compile(r'\d+(\.\d+)?$')


# ------------------------------------------------------------------ parser -> tree (tuples)
#   ('num', s) ('leaf', s) ('neg', a) ('add', a, b) ('sub', a, b) ('mul', a, b) ('dot', a, b) ('cross', a, b)
#   ('div', a, b) ('pow', a, b) ('sub_', a, b) ('fact', a) ('fn', name, a) ('head', name, [args]) ('par', a)
class FormError(ValueError):
    pass


def kind(t):
    if NUM.match(t): return 'num'
    if t in PUNCT: return 'punct'
    if t in FUNCS: return 'func'
    if t in HEADS: return 'head'
    return 'leaf'


class Parser:
    def __init__(self, toks):
        self.t, self.i = toks, 0

    def peek(self, k=0):
        return self.t[self.i + k] if self.i + k < len(self.t) else None

    def eat(self, x=None):
        s = self.peek()
        if s is None or (x is not None and s != x): raise FormError(f'expected {x!r} at token {self.i}, found {s!r}')
        self.i += 1
        return s

    def expr(self):
        v = self.term()
        while self.peek() in ('+', '-'):
            op = self.eat()
            v = ('add' if op == '+' else 'sub', v, self.term())
        return v

    def term(self):
        v = self.unary()
        while self.peek() in ('*', '/', '·', '×'):
            op = self.eat()
            v = ({'*': 'mul', '/': 'div', '·': 'dot', '×': 'cross'}[op], v, self.unary())
        return v

    def unary(self):
        if self.peek() == '-':
            self.eat()
            return ('neg', self.unary())
        return self.postfix()

    def postfix(self):
        v = self.atom()
        while self.peek() in ('^', '_', '!'):
            op = self.eat()
            if op == '^': v = ('pow', v, self.unary())
            elif op == '_': v = ('sub_', v, self.atom())
            else: v = ('fact', v)
        return v

    def atom(self):
        s = self.peek()
        if s is None: raise FormError('unexpected end')
        k = kind(s)
        if k == 'num':
            self.eat()
            return ('num', s)
        if s == '(':
            self.eat()
            v = self.expr()
            self.eat(')')
            return ('par', v)
        if k == 'func':
            self.eat()
            self.eat('(')
            v = self.expr()
            self.eat(')')
            return ('fn', s, v)
        if k == 'head':
            self.eat()
            self.eat('(')
            args = []
            if self.peek() != ')':
                args.append(self.expr())
                while self.peek() == ',':
                    self.eat()
                    args.append(self.expr())
            self.eat(')')
            if len(args) not in HEADS[s]: raise FormError(f'{s} takes {HEADS[s]} arguments, got {len(args)}')
            return ('head', s, args)
        if k == 'leaf':
            self.eat()
            return ('leaf', s)
        raise FormError(f'unexpected {s!r} at token {self.i}')


def parse(src):
    toks = src.split() if isinstance(src, str) else list(src)
    p = Parser(toks)
    v = p.expr()
    if p.i != len(toks): raise FormError(f'trailing tokens from {p.i}: {toks[p.i:]}')
    return v


# ------------------------------------------------------------------ canonical form
def canon(n):
    """A string that is equal for two trees exactly when they are the same up to commutation of + and of '*' / '·'."""
    t = n[0]
    if t == 'par': return canon(n[1])
    if t in ('num', 'leaf'): return n[1]
    if t in ('add', 'sub', 'neg'):
        terms = []
        collect_sum(n, 1, terms)
        if len(terms) == 1 and terms[0][0] == 1: return terms[0][1]
        return 'S[' + ','.join(sorted(('+' if s > 0 else '-') + c for s, c in terms)) + ']'
    if t == 'mul':
        fs = []
        collect(n, 'mul', fs)
        return 'P[' + ','.join(sorted(canon(f) for f in fs)) + ']'
    if t == 'dot':
        return 'D[' + ','.join(sorted([canon(n[1]), canon(n[2])])) + ']'
    if t in ('cross', 'div', 'pow', 'sub_'):
        return {'cross': 'X', 'div': 'Q', 'pow': 'W', 'sub_': 'U'}[t] + '[' + canon(n[1]) + ',' + canon(n[2]) + ']'
    if t == 'fact': return 'F[' + canon(n[1]) + ']'
    if t == 'fn': return n[1] + '(' + canon(n[2]) + ')'
    if t == 'head': return n[1] + '{' + ';'.join(canon(a) for a in n[2]) + '}'
    raise FormError(f'bad node {t}')


def collect(n, op, out):
    if n[0] == op:
        collect(n[1], op, out)
        collect(n[2], op, out)
    elif n[0] == 'par' and n[1][0] == op:
        collect(n[1], op, out)
    else:
        out.append(n)


def collect_sum(n, sign, out):
    t = n[0]
    if t == 'par' and n[1][0] in ('add', 'sub', 'neg'):
        return collect_sum(n[1], sign, out)
    if t == 'add':
        collect_sum(n[1], sign, out)
        collect_sum(n[2], sign, out)
    elif t == 'sub':
        collect_sum(n[1], sign, out)
        collect_sum(n[2], -sign, out)
    elif t == 'neg':
        collect_sum(n[1], -sign, out)
    else:
        out.append((sign, canon(n)))


def slots(src):
    """Token positions that are boxes in the template: leaves, numbers, function names, binary + and -."""
    toks = src.split()
    out = []
    for i, t in enumerate(toks):
        k = kind(t)
        if k in ('num', 'leaf', 'func'): out.append(i)
        elif t in ('+', '-'):
            prev = toks[i - 1] if i > 0 else None
            binary = prev is not None and (kind(prev) in ('num', 'leaf') or prev in (')', '!'))
            if binary: out.append(i)
    return out


def glyph_tokens(src):
    """Every token of a form that needs a glossary glyph (punctuation that is pure structure excluded)."""
    return {t for t in src.split() if t not in ('(', ')', ',')}


# ------------------------------------------------------------------ glyphs, waves, validation
def load_glyphs():
    """base.json first; where group files define the same token, the earliest unlock wins (so every check that
    passed for one group still passes after merging)."""
    g = {}
    files = sorted(glob.glob(os.path.join(HERE, 'glyphs', '*.json')), key=lambda f: (os.path.basename(f) != 'base.json', f))
    for f in files:
        try:
            data = json.load(open(f))
        except (ValueError, OSError):
            continue                     # another author is mid-write: skip it this time
        for k, v in data.items():
            if k not in g:
                g[k] = v
            elif os.path.basename(f) != 'base.json' and k in g:
                a, b = glyph_wave(g[k]), glyph_wave(v)
                if a is not None and b is not None and b < a and not _in_base(k):
                    g[k] = v
    return g


_BASE = {}


def _in_base(k):
    if not _BASE:
        f = os.path.join(HERE, 'glyphs', 'base.json')
        _BASE.update(json.load(open(f)) if os.path.exists(f) else {'#': 0})
    return k in _BASE


_WAVES = {}


def waves():
    """Progression waves: items (from chaincheck) and research (max of prerequisites and costs)."""
    if _WAVES: return _WAVES
    old = sys.argv
    sys.argv = ['x']
    os.environ['TS_LENIENT'] = '1'      # symbols and Insight must not hide items from the symbol checks themselves
    import importlib
    cc = importlib.reload(sys.modules['chaincheck']) if 'chaincheck' in sys.modules else importlib.import_module('chaincheck')
    os.environ.pop('TS_LENIENT', None)
    sys.argv = old
    W = dict(cc.WAVE)
    rdir = os.path.join(HERE, '..', 'src/main/resources/data/theoreticalsciences/ts_research')
    R = {}
    for f in glob.glob(os.path.join(rdir, '*.json')):
        if os.path.basename(f).startswith('_'): continue
        R['theoreticalsciences:' + os.path.basename(f)[:-5]] = json.load(open(f))
    RW = {}

    def rw(r, seen=()):
        if r in RW: return RW[r]
        if r in seen or r not in R: return 0
        d = R[r]
        w = 0
        for q in d.get('requires', []): w = max(w, rw(q, seen + (r,)))
        for c in d.get('cost', []): w = max(w, W.get(c.get('item', ''), 0))
        RW[r] = w
        return w

    for r in R: rw(r)
    _WAVES.update(item=W, research=RW, defs=R)
    return _WAVES


def glyph_wave(gl):
    u = gl.get('unlock', {})
    if u.get('start'): return 0
    WV = waves()
    if 'research' in u:
        r = u['research'] if ':' in u['research'] else 'theoreticalsciences:' + u['research']
        if r not in WV['defs']: return None
        return WV['research'][r] + 0.5          # after its own wave
    if 'item' in u:
        it = u['item'] if ':' in u['item'] else 'theoreticalsciences:' + u['item']
        if it.startswith('minecraft:'): return 0
        return WV['item'].get(it)
    return None


def ancestors(r, seen=None):
    """Every research that must be complete before r can be studied."""
    seen = set() if seen is None else seen
    for q in waves()['defs'].get(r, {}).get('requires', []):
        if q not in seen:
            seen.add(q)
            ancestors(q, seen)
    return seen


def names_in_form(src):
    return {t for t in src.split() if kind(t) == 'leaf'}


def check(rid, z, glyphs, errs, warn):
    for k in ('goal', 'name', 'lhs', 'form', 'expr', 'ask', 'vars', 'given', 'method', 'distractors'):
        if k not in z: errs.append(f'{rid}: missing {k}')
    if errs and errs[-1].startswith(rid + ': missing'): return
    try:
        tree = parse(z['form'])
        canon(tree)
        parse(z['lhs'])
    except FormError as e:
        errs.append(f'{rid}: form does not parse: {e}')
        return
    if len(z['form'].split()) > 400: errs.append(f'{rid}: form has more than 400 tokens (the network limit)')
    sl = slots(z['form'])
    if len(sl) < 3: errs.append(f'{rid}: only {len(sl)} slots - too easy')
    if len(sl) > (90 if rid == 'beyond_the_omega' else 60): errs.append(f'{rid}: {len(sl)} slots - too many')
    # numeric step
    import puzzle_check as pc
    try:
        a = pc.evaluate(z['expr'].split(), {k: float(v) for k, v in z['given'].items()})
        if not (1e-9 < abs(a) < 1e15): errs.append(f'{rid}: answer {a} out of range')
        z['_answer'] = a
    except Exception as e:
        errs.append(f'{rid}: numeric expr fails: {e}')
    ev = {t for t in z['expr'].split() if pc.kind(t) == 'var'}
    if set(z['given']) != ev: errs.append(f'{rid}: given must be exactly the variables of expr ({sorted(ev)})')
    allnames = names_in_form(z['form']) | names_in_form(z['lhs']) | ev
    undeclared = {n for n in allnames - set(z['vars']) if not (n in glyphs and glyphs[n].get('cat') in ('constants', 'numbers', 'indices'))}
    if undeclared: warn.append(f'{rid}: leaves without a vars entry (fine for generic indices): {sorted(undeclared)}')
    # the law must not be named before step 1 is solved
    nm = re.findall(r"[A-ZÀ-Ž][a-zà-ž'\-]{3,}", z['name'])
    for w in nm:
        if w.lower() in ('law', 'equation', 'theorem', 'effect', 'limit', 'principle', 'from', 'with', 'formula', 'relation', 'model', 'lagrangian'): continue
        if re.search(r'\b' + re.escape(w) + r'\b', z['goal'], re.I): errs.append(f'{rid}: goal names the law ({w!r})')
    # glyphs
    need = {t for t in glyph_tokens(z['form']) | glyph_tokens(z['lhs']) if kind(t) != 'num'}
    for t in sorted(need | {d for d in z['distractors'] if kind(d) != 'num'}):
        if t not in glyphs: errs.append(f'{rid}: no glyph for token {t!r}')
    for t in z['distractors']:
        if ' ' in t or kind(t) not in ('num', 'leaf', 'func') and t not in ('+', '-'): errs.append(f'{rid}: distractor {t!r} is not a tile glyph')
    rw = waves()['research'].get('theoreticalsciences:' + rid)
    if rw is not None:
        for t in sorted(need):
            if t not in glyphs: continue
            gw = glyph_wave(glyphs[t])
            if gw is None: errs.append(f'{rid}: glyph {t!r} has an unknown unlock {glyphs[t].get("unlock")}')
            elif gw > rw and not glyphs[t].get('unlock', {}).get('start'):
                errs.append(f'{rid}: glyph {t!r} unlocks at wave {gw} but the research is available at wave {rw}')
            u = glyphs[t].get('unlock', {})
            if u.get('research'):
                req = 'theoreticalsciences:' + u['research'].split(':')[-1]
                if req not in ancestors('theoreticalsciences:' + rid):
                    errs.append(f'{rid}: glyph {t!r} is learned from research {req.split(":")[1]}, which is not a prerequisite of this one')
            if u.get('research') and ('theoreticalsciences:' + u['research'].split(':')[-1]) == 'theoreticalsciences:' + rid:
                errs.append(f'{rid}: glyph {t!r} is unlocked by this very research')


def check_glyphs(glyphs, errs):
    for k, v in glyphs.items():
        for f in ('name', 'cat', 'meaning', 'unlock'):
            if f not in v: errs.append(f'glyph {k!r}: missing {f}')
        if ' ' in k: errs.append(f'glyph {k!r}: has a space')
        if glyph_wave(v) is None: errs.append(f'glyph {k!r}: bad unlock {v.get("unlock")}')


def load_puzzles(files=None):
    out = {}
    for f in files or sorted(glob.glob(os.path.join(HERE, 'puzzles2', 'group_*.json'))):
        out.update(json.load(open(f)))
    return out


if __name__ == '__main__':
    files = [a for a in sys.argv[1:] if not a.startswith('-')] or None
    glyphs = load_glyphs()
    errs, warn = [], []
    check_glyphs(glyphs, errs)
    pz = load_puzzles(files)
    for rid, z in pz.items():
        check(rid, z, glyphs, errs, warn)
        if '-v' in sys.argv and '_answer' in z:
            print(f'{rid:30s} [{len(slots(z["form"]))} slots] {z["lhs"]} = {z["form"]}  ->  {z["_answer"]:.6g}')
    if '-w' in sys.argv: print('\n'.join(warn))
    print('\n'.join(errs) if errs else f'{len(pz)} puzzles OK, {len(glyphs)} glyphs')
    sys.exit(1 if errs else 0)
