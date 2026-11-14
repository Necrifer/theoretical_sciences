"""
1.10.0 (Patch A): the cost rework of the pre-endgame chain.

The 1.0 recipes asked for 24 to 48 of an ingredient that was itself 24 to 48 of the one before, so costs grew 20 to 50
times per step and an item fourteen steps up (Source Code Fragment) cost about 6e17 vanilla items. This pass applies
one rule to every machine recipe that is not part of the endgame tiers:

    a recipe's output may cost at most RATIO times its single priciest ingredient.

Where a recipe breaks that, the counts of its pricey ingredients (those costing at least 1/100 of the priciest) are
scaled down together (never below 1, fluids never below 100 mB) until it holds. Cheap ingredients keep their counts, so
the bulk materials of a recipe still ask for the same grinding. Late and endgame recipes are not touched: their size is
deliberate. Those are the ones behind research 16 or more steps deep (Omnilex, Eschaton, Pleroma, Transcendent forging
and the like), the E tiers, the Empyrean table, the 1.7+ routes, and crafting grids.

Run: python3 tools/rescale_200.py [--write]   (prints every change; --write saves them and a changelog table)
"""
import json, math, os, re, sys, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import check_costs as C

RATIO = 4.0
FLOOR = 2_000.0              # recipes cheaper than this are early game: left alone
SIG = {C.NS + i for ids in C.ENDGAME.values() for i in ids}


RES = {os.path.basename(f)[:-5]: json.load(open(f, encoding='utf-8')) for f in __import__('glob').glob(os.path.join(C.D, 'ts_research', '*.json'))}
_depth = {}


def depth(k):
    """How many research steps deep a node is (1 for a root). 26 is the deepest."""
    if k not in _depth:
        _depth[k] = 0
        _depth[k] = 1 + max([depth(x.split(':')[1]) for x in RES[k].get('requires', []) if x.split(':')[1] in RES], default=0)
    return _depth[k]


LATE = 16                    # research this deep or deeper is late game and endgame: its recipes keep their size


def protected(r):
    f = r['file']
    j = json.load(open(os.path.join(C.D, f), encoding='utf-8'))
    if 'item_inputs' not in j:
        return True                                    # crafting grids keep their pictures
    res = j.get('research', '').split(':')[-1]
    if res in RES and depth(res) >= LATE:
        return True
    if re.search(r'/r1[7-9]\d_', f) or 'empyrean' in f or 'empyrean' in r['type']:
        return True
    return any(k in SIG for k, _ in r['outs'])


def main():
    write = '--write' in sys.argv
    recipes = C.load()
    changes = []
    for rnd in range(12):
        cost, route, _ = C.compute(recipes)
        changed = 0
        for r in recipes:
            if protected(r) or not r['outs']:
                continue
            each = C.unit_cost(r, cost)
            if each == math.inf or each < FLOOR:
                continue
            units = sum(n for _, n in r['outs']) + sum(a / 1000 for _, a in r['fouts'])
            parts = [(cost.get(k, C.base(k)) or 0, 'i', idx) for idx, (k, n) in enumerate(r['ins'])]
            parts += [(cost.get('fluid:' + k, C.base(k) if not k.startswith(C.NS) else None) or 0, 'f', idx) for idx, (k, a) in enumerate(r['fins'])]
            top = max((c * (1 if t == 'i' else 1) for c, t, _ in parts if t == 'i'), default=0)
            if top <= 0 or each <= RATIO * top * 1.0001:
                continue
            pricey = [(c, t, i) for c, t, i in parts if c >= top / 100 and (t == 'f' or r['ins'][i][0].startswith(C.NS))]
            fixed = r['extra'] + sum(c * (r['ins'][i][1] if t == 'i' else r['fins'][i][1] / 1000) for c, t, i in parts if (c, t, i) not in pricey)
            lo, hi = 0.0, 1.0
            def total(s):
                tt = fixed
                for c, t, i in pricey:
                    if t == 'i':
                        tt += c * max(1, round(r['ins'][i][1] * s))
                    else:
                        tt += c * max(100, round(r['fins'][i][1] * s / 100) * 100) / 1000
                return tt / units
            for _ in range(40):
                mid = (lo + hi) / 2
                if total(mid) <= RATIO * top: lo = mid
                else: hi = mid
            s = max(lo, 1e-9)
            new_ins = list(r['ins']); new_fins = list(r['fins'])
            for c, t, i in pricey:
                if t == 'i':
                    k, n = r['ins'][i]; new_ins[i] = (k, max(1, round(n * s)))
                else:
                    k, a = r['fins'][i]; new_fins[i] = (k, max(100, round(a * s / 100) * 100))
            if new_ins != r['ins'] or new_fins != r['fins']:
                changes.append((r['file'], list(r['ins']), new_ins, list(r['fins']), new_fins))
                r['ins'], r['fins'] = new_ins, new_fins
                changed += 1
        if not changed:
            break
    # merge per file: first old, last new
    final = collections.OrderedDict()
    for f, oi, ni, of, nf in changes:
        if f in final:
            final[f] = (final[f][0], ni, final[f][2], nf)
        else:
            final[f] = (oi, ni, of, nf)
    cost, _, _ = C.compute(recipes)
    print('recipes changed:', len(final))
    for k in ['general_ai_core', 'standard_model_matrix', 'grand_unified_core', 'g2_manifold', 'empirical_notes_1', 'source_code_fragment']:
        print('  %s %.3g' % (k, cost[C.NS + k]))
    if not write:
        return
    rows = []
    for f, (oi, ni, of, nf) in final.items():
        p = os.path.join(C.D, f)
        j = json.load(open(p, encoding='utf-8'))
        ii = [x for x in j.get('item_inputs', [])]
        # the loader only kept inputs with item/tag/items: map back in the same order
        kept = [x for x in ii if x.get('item') or 'tag' in x or isinstance(x.get('items'), str)]
        for x, (k, n) in zip(kept, ni):
            if x.get('count', 1) != n:
                if 'items' in x and isinstance(x['items'], str):
                    lvl = x.get('components', {}).get(C.NS + 'synthesis_level', 0)
                    n = max(1, round(n / 9 ** lvl))
                x['count'] = n
        fk = [x for x in j.get('fluid_inputs', []) if 'fluid' in x]
        for x, (k, a) in zip(fk, nf):
            x['amount'] = a
        json.dump(j, open(p, 'w', encoding='utf-8'), indent=2)
        open(p, 'a').write('\n')
        diff = ['%s %d to %d' % (k.replace(C.NS, ''), a, b) for (k, a), (_, b) in zip(oi, ni) if a != b]
        diff += ['%s %d to %d mB' % (k.replace(C.NS, ''), a, b) for (k, a), (_, b) in zip(of, nf) if a != b]
        rows.append('| %s | %s |' % (f.replace('recipe/', '').replace('.json', ''), ', '.join(diff)))
    open(os.path.join(os.path.dirname(C.D), '..', '..', '..', '..', 'build', 'rescale_1100.md'), 'w').write('\n'.join(rows) + '\n')


if __name__ == '__main__':
    main()
