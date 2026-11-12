"""
1.7.0: the cost check. Walks every recipe of the mod and gives every item a cost, the mod's way:

    C(item) = (sum of its inputs' costs + steps' time and energy) / how many the recipe makes

Vanilla items cost 1 each, except the tedious ones (Nether Star, Netherite, Dragon Head, Dragon Egg, Elytra), which
cost 64. Things nothing makes (ores, mob drops, world finds) cost WORLD each. Fluids cost per 1000 mB. Of an item's
recipes, the cheapest counts (a player uses the cheapest route).

It then checks the endgame's tiers (see ENDGAME below): each tier's typical item costs about STEP times the tier below,
at least half of it (steeper is fine), and prints any newer route that costs more than the older route it was meant to undercut.

Run:  python3 tools/check_costs.py [--report]       (exit code 1 if a check fails)
"""
import glob, json, math, os, re, sys, collections

HERE = os.path.dirname(os.path.abspath(__file__))
D = os.path.join(HERE, '..', 'src/main/resources/data/theoreticalsciences')
NS = 'theoreticalsciences:'
WORLD = 4.0
TEDIOUS = {'minecraft:nether_star': 64, 'minecraft:netherite_scrap': 32, 'minecraft:netherite_ingot': 128, 'minecraft:dragon_head': 64,
           'minecraft:dragon_egg': 512, 'minecraft:elytra': 128}
E0 = 100_000.0           # FE that count as one unit of cost
T0 = 200.0               # ticks that count as one unit of cost

# the endgame tiers (design document, 10 November): E1 Field Syntheses, E2 cosmic engineering, E3 beyond physics, E4 final
ENDGAME = {
    'E1': ['unified_field_lattice', 'perennial_catalyst_matrix', 'proof_lattice', 'living_archive', 'engineered_mind',
           'planetary_survey', 'deep_record'],
    'E2': ['dyson_lattice_panel', 'stellar_wind_condensate', 'recursive_thought_shell', 'embryonic_vacuum'],
    'E3': ['compiled_law', 'lex_prima', 'breach_shard', 'pure_concept_ingot', 'diagonal_ingot', 'distilled_axiom'],
    'E4': ['transcendent_alloy', 'event_horizon_lens', 'pleromatic_weave', 'confluent_essence', 'eschaton_steel', 'convergent_matter'],
    'E5': ['consilience_core'],            # 1.7.2: the last material, all of E1 and all of E4 together
}
STEP = {'E2': 4.0, 'E3': 6.0, 'E4': 25.0, 'E5': 6.0}


def norm(x):
    return x if ':' in x else NS + x


def load():
    recipes = []
    for f in glob.glob(os.path.join(D, 'recipe', '**', '*.json'), recursive=True):
        try:
            r = json.load(open(f, encoding='utf-8'))
        except Exception:
            continue
        if r.get('neoforge:conditions'):
            continue                                   # other mods' routes: not part of the base game's costs
        ins, fins, outs, fouts = [], [], [], []
        t = r.get('type', '')
        if 'item_inputs' in r or 'item_outputs' in r or 'fluid_inputs' in r:
            for i in r.get('item_inputs', []):
                k = i.get('item') or ('#' + i['tag'] if 'tag' in i else None)
                if k:
                    ins.append((norm(k) if not k.startswith('#') else k, i.get('count', 1)))
                elif 'items' in i and isinstance(i['items'], str):
                    # 1.7.2: a Field Synthesis of a lettered level is worth nine of the level below
                    lvl = i.get('components', {}).get(NS + 'synthesis_level', 0)
                    ins.append((norm(i['items']), i.get('count', 1) * 9 ** lvl))
            for i in r.get('fluid_inputs', []):
                if 'fluid' in i:
                    fins.append((norm(i['fluid']), i.get('amount', 1000)))
            for o in r.get('item_outputs', []):
                if 'id' in o:
                    outs.append((norm(o['id']), o.get('count', 1)))
            for o in r.get('fluid_outputs', []):
                if 'id' in o:
                    fouts.append((norm(o['id']), o.get('amount', 1000)))
            extra = r.get('energy', 0) * r.get('duration', 200) / E0 / 20 + r.get('duration', 200) / T0
        elif 'pattern' in r and 'key' in r:
            cnt = collections.Counter(ch for row in r['pattern'] for ch in row if ch != ' ')
            for ch, n in cnt.items():
                k = r['key'].get(ch)
                if isinstance(k, list):
                    k = k[0]
                if not k:
                    continue
                key = k.get('item') or ('#' + k['tag'] if 'tag' in k else None)
                if key:
                    ins.append((norm(key) if not key.startswith('#') else key, n))
            res = r.get('result', {})
            if 'id' in res:
                outs.append((norm(res['id']), res.get('count', 1)))
            extra = 0.1
        elif 'ingredient' in r and 'result' in r:              # smelting, blasting, stonecutting
            k = r['ingredient']
            if isinstance(k, list):
                k = k[0]
            key = k.get('item') or ('#' + k['tag'] if 'tag' in k else None)
            if key:
                ins.append((norm(key) if not key.startswith('#') else key, 1))
            res = r['result']
            if isinstance(res, dict) and 'id' in res:
                outs.append((norm(res['id']), res.get('count', 1)))
            elif isinstance(res, str):
                outs.append((norm(res), r.get('count', 1)))
            extra = 0.2
        elif 'ingredients' in r:
            for k in r['ingredients']:
                if isinstance(k, list):
                    k = k[0]
                key = k.get('item') or ('#' + k['tag'] if 'tag' in k else None)
                if key:
                    ins.append((norm(key) if not key.startswith('#') else key, 1))
            res = r.get('result', {})
            if 'id' in res:
                outs.append((norm(res['id']), res.get('count', 1)))
            extra = 0.1
        else:
            continue
        if outs or fouts:
            recipes.append(dict(file=os.path.relpath(f, D), type=t, ins=ins, fins=fins, outs=outs, fouts=fouts, extra=extra))
    return recipes


def base(item):
    if item.startswith('#'):
        return 1.0
    if item in TEDIOUS:
        return float(TEDIOUS[item])
    if item.startswith('minecraft:'):
        return 1.0
    if not item.startswith(NS):
        return 2.0
    return None


def with_world(recipes):
    made = {k for r in recipes for k, _ in r['outs']} | {'fluid:' + k for r in recipes for k, _ in r['fouts']}
    seeds = {}
    for r in recipes:
        for k, _ in r['ins']:
            if k.startswith(NS) and k not in made:
                seeds[k] = WORLD
        for k, _ in r['fins']:
            if k.startswith(NS) and ('fluid:' + k) not in made:
                seeds['fluid:' + k] = WORLD
    return seeds


def loot_items():
    """Items that drop from loot tables (ores, mobs, chests, the Buried Tell's soils): world finds."""
    out = set()
    for f in glob.glob(os.path.join(D, 'loot_table', '**', '*.json'), recursive=True):
        for m in re.findall(r'"name":\s*"([a-z0-9_:]+)"', open(f, encoding='utf-8').read()):
            out.add(norm(m))
    return out


RARE = 16.0               # an item stuck in a loop of recipes that the world also gives (a dimension's fluid, a boss drop)
ASSUMED = []


def compute(recipes=None):
    """Every item's cheapest cost (and the recipe file that gives it)."""
    recipes = recipes or load()
    seeds = with_world(recipes)
    cost, route = dict(seeds), {}
    while True:
        for _ in range(80):
            changed = False
            for r in recipes:
                each = unit_cost(r, cost)
                if each == math.inf:
                    continue
                for k, n in r['outs']:
                    if k not in seeds and each < cost.get(k, math.inf) - 1e-9:
                        cost[k], route[k] = each, r['file']
                        changed = True
                for k, a in r['fouts']:
                    if ('fluid:' + k) not in seeds and each < cost.get('fluid:' + k, math.inf) - 1e-9:
                        cost['fluid:' + k], route['fluid:' + k] = each, r['file']
                        changed = True
            if not changed:
                break
        # what is still unresolved sits in a loop: break it at the input that blocks most, as a rare world find
        blockers = collections.Counter()
        for r in recipes:
            if unit_cost(r, cost) == math.inf:
                for k, _ in r['ins']:
                    if cost.get(k, base(k)) is None:
                        blockers[k] += 1
                for k, _ in r['fins']:
                    if cost.get('fluid:' + k, base(k) if not k.startswith(NS) else None) is None:
                        blockers['fluid:' + k] += 1
        if not blockers:
            break
        sig = {NS + i for ids in ENDGAME.values() for i in ids}
        # (never one of the endgame's own materials while anything else blocks: their costs are what is being measured)
        k = next((b for b, _ in blockers.most_common() if b not in sig), blockers.most_common(1)[0][0])
        seeds[k] = cost[k] = RARE
        ASSUMED.append(k)
    return cost, route, recipes


def unit_cost(r, cost):
    total = r['extra']
    for k, n in r['ins']:
        c = cost.get(k, base(k))
        if c is None:
            return math.inf
        total += c * n
    for k, a in r['fins']:
        c = cost.get('fluid:' + k, base(k) if not k.startswith(NS) else None)
        if c is None:
            return math.inf
        total += c * a / 1000.0
    units = sum(n for _, n in r['outs']) + sum(a / 1000.0 for _, a in r['fouts'])
    return total / units if units > 0 else math.inf


def main():
    report = '--report' in sys.argv
    cost, route, recipes = compute()
    fails = []
    tiers = {}
    for t, ids in ENDGAME.items():
        cs = [cost.get(NS + i) for i in ids]
        have = [c for c in cs if c is not None]
        tiers[t] = math.exp(sum(math.log(c) for c in have) / len(have)) if have else None
        if report:
            print('%s  %s' % (t, ', '.join('%s %s' % (i, ('%.3g' % c) if c is not None else 'unmade') for i, c in zip(ids, cs))))
    prev = None
    for t in ('E1', 'E2', 'E3', 'E4', 'E5'):
        g = tiers[t]
        if g is None:
            print('%s: no items made yet' % t)
            prev = None
            continue
        if prev is not None and t in STEP:
            ratio = g / prev
            ok = ratio >= STEP[t] * 0.5            # at least the step: steeper is allowed (the final tier is meant to be)
            print('%s typical cost %.4g = %.2f times %s (target %.0f)%s' % (t, g, ratio, 'the tier below', STEP[t], '' if ok else '  FAIL'))
            if not ok:
                fails.append(t)
        else:
            print('%s typical cost %.4g' % (t, g))
        prev = g
    # newer routes (files named r1xx_..._<something>) must not cost more than the item's cheapest older route
    by_out = collections.defaultdict(list)
    for r in recipes:
        for k, n in r['outs']:
            by_out[k].append(r)
    for k, rs in by_out.items():
        if len(rs) < 2:
            continue
        each = lambda r: unit_cost(r, cost)
        newer = [r for r in rs if re.search(r'/r1[6-9]\d_', r['file'])]
        older = [r for r in rs if r not in newer]
        for r in newer:
            if older and each(r) > min(each(o) for o in older) * 1.001:
                print('NEWER ROUTE COSTS MORE: %s (%s) %.4g > %.4g' % (k, r['file'], each(r), min(each(o) for o in older)))
                fails.append(k)
    print('items costed:', len(cost), '; loops broken at', len(ASSUMED), 'items taken as rare world finds')
    sys.exit(1 if fails else 0)


if __name__ == '__main__':
    main()
