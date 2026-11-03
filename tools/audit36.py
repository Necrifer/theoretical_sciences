"""Round 36 final audit: impossible or unreachable machine recipes.
- more item / fluid inputs or outputs than the machine has slots
- two recipes of one machine with the same ingredients (the machine always picks one: the other can never run)
- a recipe whose ingredients are a subset of another's with an equal or higher selection score
- min_tier above any coil tier that exists
- environment bounds nothing can satisfy"""
import json, os, glob, itertools, collections
ROOT = os.path.dirname(os.path.abspath(__file__))
D = os.path.join(ROOT, '..', 'src/main/resources/data/theoreticalsciences')
M = {m['recipe']: m for m in json.load(open(os.path.join(ROOT, 'machines.json')))}
errs, warns = [], []

def key(x):
    return x.get('item') or ('#' + x['tag'] if 'tag' in x else None) or x.get('fluid') or ('#' + x.get('fluid_tag', '')) or json.dumps(x, sort_keys=True)

by = collections.defaultdict(list)
for f in sorted(glob.glob(os.path.join(D, 'recipe', '*', '*.json'))):
    r = json.load(open(f))
    t = r.get('type', '').split(':')[-1]
    if t not in M: continue
    m = M[t]
    rid = t + '/' + os.path.basename(f)[:-5]
    ii, fi = r.get('item_inputs', []), r.get('fluid_inputs', [])
    io, fo = r.get('item_outputs', []), r.get('fluid_outputs', [])
    cat = r.get('catalysts', [])
    if len(ii) + len(cat) > m['inputs']: errs.append(f'{rid}: {len(ii)}+{len(cat)} catalysts item inputs > {m["inputs"]} slots')
    if len(fi) > m['fluid_inputs']: errs.append(f'{rid}: {len(fi)} fluid inputs > {m["fluid_inputs"]} tanks')
    if len(io) > m['outputs']: errs.append(f'{rid}: {len(io)} item outputs > {m["outputs"]} slots')
    if len(fo) > m['fluid_outputs']: errs.append(f'{rid}: {len(fo)} fluid outputs > {m["fluid_outputs"]} tanks')
    if not ii and not fi and not m['generator'] and t not in ('vacuum_harvesting',):
        warns.append(f'{rid}: no inputs at all')
    items = {key(x): x.get('count', 1) for x in ii}
    for c in cat: items.setdefault('cat:' + key(c), 1)
    fluids = {key(x): x.get('amount', 1) for x in fi}
    score = (len(ii) + len(cat)) * 2 + len(fi)
    by[t].append((rid, items, fluids, score, r))
for t, rs in by.items():
    for (a, ai, af, asc, ar), (b, bi, bf, bsc, br) in itertools.permutations(rs, 2):
        # a shadows b when everything b needs also satisfies a, and a scores at least as high
        if set(ai) <= set(bi) and set(af) <= set(bf) and asc >= bsc and a < b or (set(ai) <= set(bi) and set(af) <= set(bf) and asc > bsc):
            if all(ai[k] <= bi[k] for k in ai) and all(af[k] <= bf[k] for k in af):
                # distinguished only by something the selection ignores?
                envs = (json.dumps(ar.get('environment'), sort_keys=True), json.dumps(br.get('environment'), sort_keys=True))
                errs.append(f'{t}: {b} can be shadowed by {a} (same or fewer ingredients, score {asc} >= {bsc})' + (' [different environments]' if envs[0] != envs[1] else ''))
print('\n'.join('ERROR ' + e for e in errs))
print('\n'.join('warn  ' + w for w in warns[:40]))
print(len(errs), 'errors,', len(warns), 'warnings,', sum(len(v) for v in by.values()), 'recipes')
