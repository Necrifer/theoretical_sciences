"""
Quantity inflation: every machine recipe's consumed inputs are multiplied by a factor that grows with the machine's
tier. Outputs stay the same, so each step of a chain costs more, and the costs multiply down the chain.

Edit FACTOR to rebalance the whole mod in one place (then re-run gen_data.py). Pack makers can also override
individual recipes with KubeJS or a datapack.

Left alone:
  * generators (their inputs are fuel),
  * any recipe that gives back one of its own inputs (seeds, catalytic and self-replicating loops),
  * catalysts (never consumed).
Item inputs are capped at the big input-slot size; fluids at the tank size.
"""
import json, os

# round 16: steep, GTNH-style - read from recipe_tuning.json (edit it there)
_T = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'recipe_tuning.json')))
FACTOR = {int(k): v for k, v in _T['cost_factor'].items()}
FLUID_FACTOR = {int(k): v for k, v in _T.get('fluid_factor', _T['cost_factor']).items()}
ITEM_CAP = int(_T.get('item_cap', 1024))      # the generator's limit (machines hold far more)
TANK = {False: int(_T.get('fluid_cap_single', 256_000)), True: int(_T.get('fluid_cap_multiblock', 2_048_000))}   # MachineType.tankCapacity(): single machine / multiblock


def apply(REC, machines, tiers):
    by_recipe = {m['recipe']: m for m in machines}
    n = 0
    for typ, m in by_recipe.items():
        if m['generator']:
            continue
        f = FACTOR.get(tiers.get(m['id'], 1), 1)
        ff = FLUID_FACTOR.get(tiers.get(m['id'], 1), f)
        d = os.path.join(REC, typ)
        if f <= 1 or not os.path.isdir(d):
            continue
        for fn in os.listdir(d):
            p = os.path.join(d, fn)
            r = json.load(open(p))
            ins = {i.get('item') or i.get('tag') for i in r.get('item_inputs', [])} | {x['fluid'] for x in r.get('fluid_inputs', [])}
            outs = {o['id'] for o in r.get('item_outputs', [])} | {o['id'] for o in r.get('fluid_outputs', [])}
            if ins & outs or r.get('secret') or r.get('unscaled'):
                continue
            for i in r.get('item_inputs', []):
                i['count'] = min(ITEM_CAP, i.get('count', 1) * f)
            for x in r.get('fluid_inputs', []):
                x['amount'] = min(TANK[bool(m['multiblock'])], x['amount'] * ff)
            r['scaled'] = f        # informational; ignored by the game
            json.dump(r, open(p, 'w'), indent=2)
            n += 1
    return n
