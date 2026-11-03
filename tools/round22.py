"""
Round 22.

* Research questions: the given values are stated with the question, at the top, and they are different in every
  world - each puzzle gets a "vary" range for every value that is not a constant of nature (ResearchPuzzle.given draws
  them from the world seed; it re-draws any set that would make the question meaningless).
* The Cantor room of the Alpha Omega fight rewrites its list after a wrong answer (boss/ProofArena).
* No recipe lists the same ingredient twice: duplicates are merged into one entry (a slot holds any number). Checked
  here for every recipe file, and merged again in QHRecipeSerializer for pack-made recipes.
* The Core Forge: every recipe its own, and the creative items of each mod need a Simulated Universe of that mod
  (transcend/CoreForge, transcend/SimulatedUniverseItem).
"""
import glob
import json
import math
import os
import random
import re

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('simulated_universe', 'Simulated Universe', 'orb', '#0a0c20', '#9ad0ff',
       'A whole other world, run from its first instant to its last inside the Forge. Each mod has its own.', special='simuniverse'),
]
BEYOND = ['simulated_universe']

LANG = {
    'item.theoreticalsciences.simulated_universe.of': 'Simulated Universe: %s',
    'tooltip.theoreticalsciences.simulated_universe': 'The universe of %s, simulated to completion. Its creative items are forged from it.',
    'tooltip.theoreticalsciences.simulated_universe.empty': 'An empty simulation.',
    'puzzle.theoreticalsciences.given': 'Given:',
    'puzzle.theoreticalsciences.answer_in': 'Answer in %s',
    'proof.theoreticalsciences.diagonal.wrong': 'The list already knows that number. It rewrites itself - read it again.',
    'theoreticalsciences.configuration.puzzleValuesPerWorld': 'Puzzle values differ per world',
}

# ---------------------------------------------------------------------------------------------- per-world values
# constants of nature (and fixed unit conversions): never varied. Matched by value, to 0.2 %.
CONSTANTS = [6.674e-11, 2.998e8, 3e8, 299792458, 299792, 2.998, 2.998e23, 1.0546e-34, 1.055e-34, 0.658, 658.2, 6.626e-34, 6.626,
             4.1357e-21, 1.380649e-23, 1.381e-23, 8.617e-5, 1.602e-19, 8.854e-12, 8.314, 96485, 5.67e-8, 1.496e11, 3.828e26, 1.989e30,
             197.327, 197.3, 1.202, 3.156e16, 9.109e-31, 9.109, 1.6726e-27, 52917.7, 13.606, 2.179e-18, 1836.15, 206.77, 1.22e19, 6.646e-27,
             0.2998, 0.3, 511, 938.3, 91.19, 91.2, 246, 246.22, 0.129, 0.652, 0.357, 2.612, 0.118, 0.007, 0.2375, 1.13, 0.246]
# by name, whatever the value
CONST_NAMES = {'π', 'G', 'ħ', 'h', 'kB', 'ε0', 'F', 'AU', 'Ls', 'Ms', 'hc', 'ħc', 'Z3', 'Gyr', 'me', 'χ'}
# literal phrases in the question that only make sense below the values
WORDING = [(' below,', ' given,'), ('value below', 'value given'), ('gap below', 'gap given'), ('plates below', 'plates given'),
           ('field below', 'field given'), ('values below', 'values given'), ('is below, ', 'is given, ')]


# values the wording depends on in a way a number swap cannot follow (e.g. "0.6c ... so v = 1.8")
KEEP = {'special_relativity': ('v',), 'causality_violation': ('v', 'Δt', 'Δx'), 'multiverse': ('H', 't'), 'extremophiles': ('T',),
        'island_of_stability': ('Z', 'A'), 'annihilation_power': ('m',), 'calculus': ('n',), 'string_theory': ('n',), 'antiatom_chemistry': ('n1', 'n2'),
        'formal_logic': ('n',), 'set_theory': ('n',), 'transfinite_mathematics': ('n',)}


def _toks(expr):
    return expr.split()


def _eval(expr, env):
    import puzzle_check as pc
    return pc.evaluate(_toks(expr), env)


def _const(name, v):
    if name in CONST_NAMES:
        return True
    if name == 'c' and v in (1.0, 3.0):          # the speed of light in natural / scaled units
        return True
    if v == 0:
        return True
    for c in CONSTANTS:
        if abs(v - c) <= 0.002 * abs(c):
            return True
    return False


def _step(v):
    a = abs(v)
    return 10 ** (math.floor(math.log10(a)) - 2) if a > 0 else 1


def _spec(name, v, width):
    """[low, high, step] around v: integers stay whole, fractions stay below 1, signs never change."""
    if float(v).is_integer() and abs(v) < 1e6:
        v = int(v)
        if abs(v) <= 3:
            lo, hi = (max(1, v - 1), v + 2) if v > 0 else (v - 2, min(-1, v + 1))
        else:
            d = max(1, round(abs(v) * width))
            lo, hi = v - d, v + d
            if v > 0:
                lo = max(1, lo)
            else:
                hi = min(-1, hi)
        return [float(lo), float(hi), 1.0]
    s = _step(v)
    lo, hi = sorted((v * (1 - width), v * (1 + width)))
    if 0 < v < 1:
        hi = min(hi, 1 - s)
    lo, hi = math.floor(lo / s) * s, math.ceil(hi / s) * s
    if v > 0:
        lo = max(lo, s)
    return [float(f'{lo:.6g}'), float(f'{hi:.6g}'), float(f'{s:.3g}')]


def _draw(spec, rnd):
    lo, hi, st = spec
    n = int(math.floor((hi - lo) / st + 1e-9))
    return lo + rnd.randint(0, max(0, n)) * st


def _ok(z, env, base):
    try:
        a = _eval(z['expr'], env)
    except Exception:
        return False
    if not math.isfinite(a) or a == 0 or (a > 0) != (base > 0):
        return False
    return abs(math.log10(abs(a / base))) <= 3


def _lits(v):
    g = f'{v:g}'
    out = [g, re.sub(r'e\+?(-?)0*(\d)', r'e\1\2', g)]
    if v.is_integer() and abs(v) < 1e12:
        n = int(v)
        out += [f'{n:,}'.replace(',', ' '), f'{n:,}'.replace(',', '\u202f'), f'{n:,}', str(n)]
    else:
        out += [f'{v:.6g}'.rstrip('0')]
    return [l for l in dict.fromkeys(out) if l]


_AFTER = r'(?![\w\u00b2\u00b3\u2070-\u209f]|\.\d)'


def _find(text, lit):
    # (never inside a placeholder already made)
    text = re.sub(r'@\{[^}]*\}', lambda m: '\0' * len(m.group(0)), text)
    return list(re.finditer(r'(?<![\w.\u00b2\u00b3\u2070-\u209f\0])' + re.escape(lit) + _AFTER, text))


def template(z, rid, keys, report):
    """Values written into the question become @{name} (filled in with this world's value). Returns the vars that
    could not be placed safely: they stay as written (fixed)."""
    given = {k: float(z['given'][k]) for k in keys}
    allv = {k: float(v) for k, v in z['given'].items()}
    texts = {f: z.get(f, '') for f in ('ask', 'goal')}
    fixed = set()
    for f, t in texts.items():
        # 1) "name = 7000" (any spacing)
        for k, v in given.items():
            for lit in _lits(v):
                t = re.sub(r'(?<![\w])(' + re.escape(k) + r'\s*=\s*)' + re.escape(lit) + _AFTER, lambda m, k=k, lit=lit: m.group(1) + '@{' + k + '|' + lit + '}', t)
        # 2) a bare number that stands for exactly one of the values (never a lone digit: too easily something else)
        for k, v in given.items():
            others = {allv[o] for o in allv if o != k}
            hits = {m.span(): lit for lit in _lits(v) if len(lit) >= 2 for m in _find(t, lit)}
            if len(hits) == 1 and v not in others:
                (st, en), lit = next(iter(hits.items()))
                t = t[:st] + '@{' + k + '|' + lit + '}' + t[en:]
        texts[f] = t
    for k, v in given.items():
        for f, t in texts.items():
            # still written somewhere as a number, or "k = <something else>" (10¹⁶, 2e16...): the text depends on it
            if any(_find(t, lit) for lit in _lits(v)) or re.search(r'(?<![\w])' + re.escape(k) + r'\s*=\s*(?!\s|@\{)', t):
                fixed.add(k)
    for f in texts:
        # back to exactly what was written for the fixed ones; @{name} for the rest
        texts[f] = re.sub(r'@\{([^|}]+)\|([^}]*)\}', lambda m: m.group(2) if m.group(1) in fixed else '@{' + m.group(1) + '}', texts[f])
    for k in sorted(fixed):
        report.append(f'{rid}:{k}')
    z.update(texts)
    return fixed


def vary(z, rid, report):
    given = {k: float(v) for k, v in z['given'].items()}
    try:
        base = _eval(z['expr'], given)
    except Exception:
        return None
    if base == 0 or not math.isfinite(base):
        return None
    keys = [k for k, v in given.items() if not _const(k, v) and k not in KEEP.get(rid, ())]
    fixed = template(z, rid, keys, report)
    keys = [k for k in keys if k not in fixed]
    out = {k: _spec(k, given[k], 0.4) for k in keys}
    if not out:
        return None
    # narrow the ranges until almost every draw is a sensible question (Java re-draws the rest)
    for width in (0.4, 0.25, 0.15, 0.08):
        for k in out:
            out[k] = _spec(k, given[k], width)
        rnd = random.Random(rid)
        good = 0
        for _ in range(300):
            env = dict(given)
            for k, sp in out.items():
                env[k] = _draw(sp, rnd)
            good += _ok(z, env, base)
        if good >= 240:
            return out
    return out


def apply(g):
    g['LANG'].update(LANG)
    D, w = g['D'], g['w']
    tf = os.path.join(D, 'tags', 'item', 'beyond_omnipotence.json')
    t = json.load(open(tf))
    for b in BEYOND:
        if f'{NS}:{b}' not in t['values']:
            t['values'].append(f'{NS}:{b}')
    w(tf, t)


# ---------------------------------------------------------------------------------------------- duplicate entries
LISTS = ('item_inputs', 'fluid_inputs', 'optional_inputs', 'optional_fluid_inputs', 'catalysts')


def _key(e):
    return json.dumps({k: v for k, v in e.items() if k not in ('count', 'amount')}, sort_keys=True)


def merge_entries(lst, amount_key):
    """Same ingredient twice -> once, with the amounts added. Returns (new list, merged?)."""
    seen, out = {}, []
    for e in lst:
        if not isinstance(e, dict):
            out.append(e)
            continue
        k = _key(e)
        if k in seen:
            if amount_key:
                prev = seen[k]
                prev[amount_key] = prev.get(amount_key, 1) + e.get(amount_key, 1)
            continue           # a catalyst named twice is needed once
        e = dict(e)
        seen[k] = e
        out.append(e)
    return out, len(out) != len(lst)


def duplicates(r):
    """What a recipe lists twice (for the checker)."""
    bad = []
    for k in LISTS:
        keys = [_key(e) for e in r.get(k, []) or [] if isinstance(e, dict)]
        bad += [f'{k}: {x}' for x in sorted(set(x for x in keys if keys.count(x) > 1))]
    # an optional input that is the same item as a required one
    req = {e.get('item') for e in r.get('item_inputs', []) or [] if isinstance(e, dict)}
    bad += [f'optional_inputs repeats item_inputs: {e["item"]}' for e in r.get('optional_inputs', []) or [] if isinstance(e, dict) and e.get('item') in req]
    reqf = {e.get('fluid') for e in r.get('fluid_inputs', []) or [] if isinstance(e, dict)}
    bad += [f'optional_fluid_inputs repeats fluid_inputs: {e["fluid"]}' for e in r.get('optional_fluid_inputs', []) or [] if isinstance(e, dict) and e.get('fluid') in reqf]
    return bad


def merge_recipe(r):
    changed = False
    for k in LISTS:
        if k in r and isinstance(r[k], list):
            amt = 'amount' if 'fluid' in k else ('count' if k != 'catalysts' else None)
            r[k], c = merge_entries(r[k], amt)
            changed |= c
    # optional inputs naming an item that is already required: fold them into it
    req = {e.get('item'): e for e in r.get('item_inputs', []) or [] if isinstance(e, dict) and 'item' in e}
    keep = []
    for e in r.get('optional_inputs', []) or []:
        if isinstance(e, dict) and e.get('item') in req:
            req[e['item']]['count'] = req[e['item']].get('count', 1) + e.get('count', 1)
            changed = True
        else:
            keep.append(e)
    if 'optional_inputs' in r:
        r['optional_inputs'] = keep
    reqf = {e.get('fluid'): e for e in r.get('fluid_inputs', []) or [] if isinstance(e, dict) and 'fluid' in e}
    keepf = []
    for e in r.get('optional_fluid_inputs', []) or []:
        if isinstance(e, dict) and e.get('fluid') in reqf:
            reqf[e['fluid']]['amount'] = reqf[e['fluid']].get('amount', 1000) + e.get('amount', 1000)
            changed = True
        else:
            keepf.append(e)
    if 'optional_fluid_inputs' in r:
        r['optional_fluid_inputs'] = keepf
    return changed


def finish(g):
    w, D, A, te = g['w'], g['D'], g['A'], g['text_entry']
    # ---- per-world puzzle values
    report, n = [], 0
    for f in sorted(glob.glob(os.path.join(D, 'ts_research', '*.json'))):
        r = json.load(open(f))
        z = r.get('puzzle')
        if not z:
            continue
        for a, b in WORDING:
            z['ask'] = z.get('ask', '').replace(a, b)
        v = vary(z, os.path.basename(f)[:-5], report)
        if v:
            z['vary'] = v
            n += 1
        else:
            z.pop('vary', None)
        w(f, r)
    print('round22 per-world values:', n, 'puzzles; kept as written:', ', '.join(report) or 'none')
    # ---- every recipe: no ingredient twice
    merged = []
    for f in sorted(glob.glob(os.path.join(D, 'recipe', '**', '*.json'), recursive=True)):
        r = json.load(open(f))
        if merge_recipe(r):
            w(f, r)
            merged.append(os.path.relpath(f, os.path.join(D, 'recipe')))
    print('round22 merged duplicate entries in', len(merged), 'recipes', merged[:12], '...' if len(merged) > 12 else '')
    # ---- the Simulated Universe
    w(os.path.join(A, 'models', 'item', 'simulated_universe.json'), {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': f'{NS}:item/simulated_universe', 'layer1': f'{NS}:item/simulated_universe_galaxy', 'layer2': f'{NS}:item/simulated_universe_glass'}})
    te('endgame', 'simulated_universes', 'Simulated Universes', 'simulated_universe', [
        'The rebuilt Core does not simply hand out the creative items of other mods. To forge one, it must first simulate the universe that item belongs to - the whole of that mod, from its first instant to its last.',
        'Each mod with creative items has a $(item)Simulated Universe$() of its own, forged from universe seeds, pocket universes and Omnipotent Ingots - and from that mod\'s own most advanced things (the Forge shows which). Then each creative item asks for its universe, an Eschaton Core, the best ordinary version of itself, and the material that fits what it does: pocket universes for storage, false-vacuum membranes for tanks, negentropy for energy...',
        'No two Forge recipes are the same. Simulated universes are beyond Omnipotence: the Core\'s "make anything" cannot make them.'],
        sort=34, spot=['simulated_universe'])
    te('basics', 'world_numbers', 'Numbers of This World', 'research_terminal', [
        'Every research question gives its values up front, with the question. They are not the same in every world: each world draws its own (from its seed), so the answers cannot be shared or looked up - only worked out.',
        'Constants of nature - the speed of light, Planck\'s constant, the gravitational constant and the rest - are the same everywhere. Server owners can turn this off (puzzleValuesPerWorld).'],
        sort=-1, spot=['research_terminal'])
