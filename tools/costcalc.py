"""
Rough "raw cost" estimator: how many base items (vanilla items, ore drops, mob drops = 1 each; water/lava ~free)
go into one of each item, taking the cheapest known recipe. Fluids are costed per 1000 mB.
Multi-output recipes split their cost evenly over their outputs. Catalysts are not consumed and cost nothing.
Usage: python3 costcalc.py [item ...]      (default: a set of milestone items)
"""
import json, os, glob, sys, math

ROOT = os.path.dirname(os.path.abspath(__file__))
D = os.path.join(ROOT, '..', 'src/main/resources/data/theoreticalsciences')
NS = 'theoreticalsciences:'
C = json.load(open(os.path.join(ROOT, 'content.json')))


def nid(x):
    return x if ':' in x else NS + x


def load_recipes(scale=None):
    out = []
    for f in glob.glob(os.path.join(D, 'recipe/**/*.json'), recursive=True):
        r = json.load(open(f))
        t = r['type']
        ins, outs = [], []
        if t.startswith('minecraft:crafting'):
            if 'key' in r:
                cnt = {}
                for row in r['pattern']:
                    for ch in row:
                        if ch != ' ': cnt[ch] = cnt.get(ch, 0) + 1
                ins = [(_k(r['key'][k]), n) for k, n in cnt.items()]
            else:
                ins = [(_k(i), 1) for i in r['ingredients']]
            outs = [(r['result']['id'], r['result'].get('count', 1))]
        elif t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:stonecutting'):
            ins = [(_k(r['ingredient']), 1)]
            outs = [(r['result']['id'], r['result'].get('count', 1))]
        elif t == NS + 'extreme_shaped':
            cnt = {}
            for row in r['pattern']:
                for ch in row:
                    if ch != ' ': cnt[ch] = cnt.get(ch, 0) + 1
            ins = [(_k(r['key'][k]), n) for k, n in cnt.items()]
            outs = [(r['result']['id'], r['result'].get('count', 1))]
        elif t == NS + 'extreme_shapeless':
            cnt = {}
            for i in r['ingredients']:
                cnt[_k(i)] = cnt.get(_k(i), 0) + 1
            ins = list(cnt.items())
            outs = [(r['result']['id'], r['result'].get('count', 1))]
        elif t == NS + 'compaction':
            ins = [(_k(r['ingredient']), r['count'])]
            outs = [(r['result']['id'], 1)]
        else:
            ins = [(_k(i), i.get('count', 1)) for i in r.get('item_inputs', [])]
            ins += [('fluid:' + f['fluid'], f['amount'] / 1000) for f in r.get('fluid_inputs', [])]
            ch = r.get('chances') or []
            outs = [(o['id'], o['count'] * (ch[k] if k < len(ch) else 1)) for k, o in enumerate(r.get('item_outputs', []))]
            outs += [('fluid:' + o['id'], o['amount'] / 1000) for o in r.get('fluid_outputs', [])]
        if outs:
            out.append((os.path.relpath(f, D), ins, outs))
    return out


def _k(x):
    if isinstance(x, dict):
        return x.get('item') or ('#' + x['tag'])
    return x


def costs(recipes):
    cost = {}
    base = set()
    for b in C['blocks']:
        if b['kind'] == 'ore':
            base.add(nid(b['drops']))
    base |= {NS + x for x in ('fractured_essence', 'void_essence', 'fractured_reality', 'brane_tension_fiber', 'graviton_wisp_core',
                              'moduli_crystal', 'arbor_heartwood_sample', 'bubble_universe_sample', 'bulk_stone', 'axiom_stone')}

    def c(x):
        if x.startswith('minecraft:') or x.startswith('#'):
            return 1.0
        if x in ('fluid:minecraft:water', 'fluid:minecraft:lava'):
            return 0.01
        if x == 'fluid:' + NS + 'primordial_law':
            return 10.0
        if x in base:
            return 1.0
        return cost.get(x, math.inf)

    for _ in range(60):
        changed = False
        for name, ins, outs in recipes:
            total = sum(c(i) * n for i, n in ins)
            if math.isinf(total):
                continue
            outn = sum(n for _, n in outs)
            if outn <= 0:
                continue
            for o, n in outs:
                per = total / outn
                if per < cost.get(o, math.inf) * 0.999 and not (o.startswith('minecraft:')):
                    cost[o] = per
                    changed = True
        if not changed:
            break
    return cost, c


MILESTONES = ['neutronium_ingot', 'casimir_plate', 'quantum_processor', 'chronosteel_ingot', 'spacetime_alloy_ingot', 'cosmic_neutronium_ingot',
              'hypernova_alloy_ingot', 'transcendent_metal_ingot', 'mathematics_singularity', 'theory_of_everything', 'absolute_infinity',
              'omega_point', 'omniscience_catalyst', 'omnium_ingot', 'omnilex']

if __name__ == '__main__':
    cost, c = costs(load_recipes())
    for it in (sys.argv[1:] or MILESTONES):
        v = c(nid(it))
        print(f'{it:32s} {v:>22,.0f}' if not math.isinf(v) else f'{it:32s} unreachable')
