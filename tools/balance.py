"""
1.14.2: the recipe cost audit.

Every recipe in the mod is priced the way the player pays for it:

* almost every vanilla material costs 1 (a vanilla item, or a bucket of a vanilla fluid); the few that are truly tedious
  to get - a Nether Star, Netherite Scrap or Ingot, a Dragon Head, the Dragon Egg, an Elytra - cost RARE;
* a material of the mod costs what its cheapest route costs, per item (found by relaxing every recipe until nothing
  gets cheaper); one mined or dropped in the world costs 1 (2 for a creature's drop);
* running a machine costs its energy (ENERGY FE = 1, about what one lump of coal gives in the mod's generators) and its
  time (TIME ticks = 1);
* the machine itself costs its own recipe, and a multiblock also every block of its structure.

Then every material with more than one route is checked: a route on a dearer machine must run substantially cheaper per
item (at most BETTER x the cheaper machine's route), and so must a newer route on a machine of the same price (at most
NEWER x the older one) - otherwise nobody would use it. Routes that fail are reported (and, with --fix, given what they
need: their outputs raised, or their inputs cut, until they pass).
"""
import glob
import json
import math
import os
import re
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
D = os.path.join(RES, 'data', 'theoreticalsciences')
REC = os.path.join(D, 'recipe')
MB = os.path.join(RES, 'assets', 'theoreticalsciences', 'multiblocks')
NS = 'theoreticalsciences:'

RARE = 64
RARE_IDS = {'minecraft:nether_star', 'minecraft:netherite_scrap', 'minecraft:netherite_ingot', 'minecraft:dragon_head', 'minecraft:dragon_egg',
            'minecraft:elytra', 'minecraft:netherite_block'}
ENERGY = 200_000
# the Black Swan Feather is a shortcut on purpose; routes using it are not a bar for the others to clear
SHORTCUT_INPUTS = {'theoreticalsciences:black_swan_feather'}
# drops of bosses: as rare as the rarest vanilla things
BOSS_LOOT = ('binary_serpent', 'alpha_omega', 'omnilex', 'proof')
BOSS_ITEMS = set()
# a creature's drop: hunting cannot be automated as simply as a machine runs, so it counts as dear
DROP = 8.0
TIME = 2400
BETTER = 0.6        # a route on a dearer machine must cost at most this much per item
NEWER = 0.8         # a newer route on an equally dear machine: at most this much
SAME = 1.25         # two routes of the same age on one machine: within a quarter of each other
# recipe types that are not real routes for a material (generators burn, convergence/extreme/catalysts are special)
SKIP_TYPES = {'thermoelectric', 'induction', 'accretion_power', 'fusion_power', 'magnetohydrodynamic', 'hawking_evaporation', 'lensing',
              'zero_point', 'research_synthesis', 'extreme', 'convergence', 'convergence_automation'}


# 1.1.0: machines that can only run part of the day. Counted the way the player pays: the run takes that much longer in
# real time, and keeping up with an all-day machine takes that many more of them. The Solar Furnace needs the Sun up (and
# yields less when it is low); the Reflecting Telescope needs the night.
PART_TIME = {'solar_melting': 2.5, 'astrophotography': 2.0}

FIRST_AFTER_1_0 = 37      # recipes of rounds before 37 shipped in 1.0


def age(name):
    """0 for anything that shipped in 1.0; otherwise the round that added it (so newer compares higher)."""
    m = re.match(r'r(\d+)_', name)
    n = int(m.group(1)) if m else 0
    return n if n >= FIRST_AFTER_1_0 else 0


def load_recipes():
    out = []
    for f in glob.glob(os.path.join(REC, '**', '*.json'), recursive=True):
        try:
            d = json.load(open(f))
        except ValueError:
            continue
        rel = os.path.relpath(f, REC)
        typ = d.get('type', '').split(':')[-1]
        folder = rel.split(os.sep)[0]
        ins, outs, cats = defaultdict(float), defaultdict(float), set()
        energy = ticks = 0
        if typ in ('crafting_shaped', 'extreme_shaped'):
            cnt = defaultdict(int)
            for row in d.get('pattern', []):
                for ch in row:
                    if ch != ' ': cnt[ch] += 1
            for ch, n in cnt.items():
                k = d['key'].get(ch)
                if isinstance(k, list): k = k[0]
                if isinstance(k, dict): ins[k.get('item') or '#' + k.get('tag', '?')] += n
                elif isinstance(k, str): ins[k] += n
            r = d.get('result', {})
            outs[r.get('id') or r.get('item')] += r.get('count', 1)
        elif typ in ('crafting_shapeless', 'extreme_shapeless'):
            for k in d.get('ingredients', []):
                if isinstance(k, list): k = k[0]
                ins[k.get('item') or '#' + k.get('tag', '?')] += 1
            r = d.get('result', {})
            outs[r.get('id') or r.get('item')] += r.get('count', 1)
        elif typ in ('smelting', 'blasting', 'smoking', 'campfire_cooking', 'stonecutting'):
            k = d.get('ingredient', {})
            if isinstance(k, list): k = k[0]
            ins[k.get('item') or '#' + k.get('tag', '?')] += 1
            r = d.get('result', {})
            outs[r.get('id') or r.get('item')] += r.get('count', 1)
            ticks = d.get('cookingtime', 0)
        elif d.get('type', '').startswith(NS) and ('item_inputs' in d or 'fluid_inputs' in d or 'item_outputs' in d or 'fluid_outputs' in d):
            for i in d.get('item_inputs', []):
                ins[i.get('item') or '#' + i.get('tag', '?')] += i.get('count', 1)
            for x in d.get('fluid_inputs', []):
                ins[x['fluid']] += x['amount'] / 1000.0
            ch = d.get('chances') or []
            for n, o in enumerate(d.get('item_outputs', [])):
                outs[o['id']] += o.get('count', 1) * (ch[n] if n < len(ch) else 1)
            for x in d.get('fluid_outputs', []):
                outs[x['id']] += x['amount'] / 1000.0
            for c in d.get('catalysts', []):
                cats.add(c.get('item') or '#' + c.get('tag', '?'))
            energy = d.get('energy', 0) * d.get('duration', 0)
            ticks = d.get('duration', 0)
        else:
            continue
        items_o = [o['id'] for o in d.get('item_outputs', [])] if 'item_outputs' in d or 'fluid_outputs' in d else []
        fl_o = [o['id'] for o in d.get('fluid_outputs', [])]
        main = (items_o or fl_o or list(outs) or [None])[0]
        # packing into, or unpacking out of, a storage block is not a route for anything
        if typ.endswith(('shaped', 'shapeless')) and len(ins) == 1 and len(outs) == 1:
            n_in, n_out = sum(ins.values()), sum(outs.values())
            if (n_in == 1 and n_out == 9) or (n_in == 9 and n_out == 1) or (n_in == 1 and n_out == 4) or (n_in == 4 and n_out == 1):
                main = None
        out.append(dict(main=main, file=rel, name=os.path.basename(f)[:-5], type=typ, folder=folder, ins=dict(ins), outs=dict(outs), cats=cats,
                        energy=energy, ticks=ticks, age=age(os.path.basename(f))))
    return out


def machines():
    M = json.load(open(os.path.join(HERE, 'machines.json')))
    by_type = defaultdict(list)
    for m in M:
        by_type[m['recipe']].append(m)
        if m['generator']:
            SKIP_TYPES.add(m['recipe'])
    return M, by_type


def base_cost(i):
    if i is None: return None
    if i in RARE_IDS: return RARE
    if i.startswith('minecraft:') or i.startswith('#minecraft:') or i.startswith('#c:') or i.startswith('#neoforge:'):
        return 1.0
    return None


def price(recipes, world):
    """Cheapest cost per item of everything, by relaxing every recipe until nothing gets cheaper."""
    cost = {}
    for i in world: cost[i] = world[i]
    for _ in range(60):
        changed = False
        for r in recipes:
            if r['type'] in SKIP_TYPES or not r['outs']: continue
            if set(r['ins']) & set(r['outs']): continue
            total = r['energy'] / ENERGY + r['ticks'] * PART_TIME.get(r['type'], 1.0) / TIME
            ok = True
            for i, n in r['ins'].items():
                c = cost.get(i, base_cost(i))
                if c is None:
                    ok = False
                    break
                total += c * n
            if not ok: continue
            n_out = sum(r['outs'].values())
            for o, n in r['outs'].items():
                if n <= 0: continue
                # shared fairly between the outputs, by count
                u = total / n_out if n_out else total
                cur = cost.get(o, base_cost(o))
                if cur is None or u < cur - 1e-9:
                    cost[o] = u
                    changed = True
        if not changed: break
    return cost


def unit_cost(r, cost, target):
    total = r['energy'] / ENERGY + r['ticks'] * PART_TIME.get(r['type'], 1.0) / TIME
    for i, n in r['ins'].items():
        c = cost.get(i, base_cost(i))
        if c is None: return None
        total += c * n
    # value of the by-products is taken off
    for o, n in r['outs'].items():
        if o != target: total -= 0.5 * cost.get(o, 0) * n
    n = r['outs'].get(target, 0)
    return max(total, 0.01) / n if n > 0 else None


def machine_cost(M, recipes, cost):
    """What a machine costs to build: its own recipe, plus every block of its structure if it is a multiblock."""
    made = {}
    for r in recipes:
        if r['type'] in ('crafting_shaped', 'crafting_shapeless', 'extreme_shaped', 'extreme_shapeless'):
            for o, n in r['outs'].items():
                t = sum(cost.get(i, base_cost(i)) or 4 for i in r['ins']) if False else sum((cost.get(i, base_cost(i)) or 4) * k for i, k in r['ins'].items())
                made.setdefault(o, t / n)
    out = {}
    for m in M:
        mid = NS + m['id']
        c = made.get(mid, cost.get(mid, 20))
        f = os.path.join(MB, m['id'] + '.json')
        if m['multiblock'] and os.path.exists(f):
            d = json.load(open(f))
            key = d.get('key', {})
            for layer in d.get('layers', []):
                for row in layer:
                    for ch in row:
                        if ch in (' ', d.get('controller')) or ch not in key: continue
                        opts = key[ch]
                        b = opts[0] if isinstance(opts, list) else opts
                        c += made.get(b, cost.get(b, base_cost(b) or 4))
        out[m['id']] = c * PART_TIME.get(m['recipe'], 1.0)
    return out


def world_items():
    """Materials only found in the world: mined (1) or dropped by creatures (2)."""
    C = json.load(open(os.path.join(HERE, 'content.json')))
    w = {}
    for b in C['blocks']:
        if b['kind'] == 'ore' and b.get('drops'):
            w[NS + b['drops'].split(':')[-1]] = 1.0
        if b['kind'] in ('simple', 'cloud', 'ore'):
            w.setdefault(NS + b['id'], 1.0)
    w[NS + 'black_swan_feather'] = RARE          # one per swan, and a swan comes rarely
    for f in glob.glob(os.path.join(D, 'loot_table', 'entities', '*.json')):
        d = json.load(open(f))
        boss = os.path.basename(f).startswith(BOSS_LOOT)
        for p in d.get('pools', []):
            for e in p.get('entries', []):
                if e.get('name'):
                    if boss:
                        w[e['name']] = max(w.get(e['name'], 0), RARE)
                        BOSS_ITEMS.add(e['name'])
                    else: w.setdefault(e['name'], DROP)
    return w


def audit(verbose=True):
    recipes = load_recipes()
    M, by_type = machines()
    cost = price(recipes, world_items())
    mcost = machine_cost(M, recipes, cost)
    by_out = defaultdict(list)
    for r in recipes:
        if r['type'] in SKIP_TYPES: continue
        if set(r['ins']) & set(r['outs']): continue
        if r['main'] and len(r['outs']) < 3: by_out[r['main']].append(r)

    def where(r):
        if r['type'] in ('crafting_shaped', 'crafting_shapeless'): return ('crafting table', 0)
        if r['type'] in ('extreme_shaped', 'extreme_shapeless'): return ('extreme table', 5)
        if r['type'] in ('smelting', 'blasting', 'smoking', 'stonecutting', 'campfire_cooking'): return (r['type'], 1)
        ms = by_type.get(r['type'], [])
        if not ms: return (r['type'], 50)
        cheapest = min(ms, key=lambda m: mcost.get(m['id'], 50))
        return (cheapest['id'], mcost.get(cheapest['id'], 50))

    problems = []
    for o, rs in by_out.items():
        rs = [r for r in rs if r['type'] != 'stonecutting' and not r['name'].endswith('_back')]
        vanilla = base_cost(o) == 1.0
        if len(rs) < 2 and not vanilla: continue
        rows = []
        if vanilla:      # simply gathering it: cost 1, no machine
            rows.append((dict(file='(gathered in the world)', age=0, name='world'), 1.0, 'world', 0))
        for r in rs:
            u = unit_cost(r, cost, o)
            if u is None: continue
            mname, mc = where(r)
            rows.append((r, u, mname, mc))
        for (rb, ub, mb, cb) in rows:
            for (ra, ua, ma, ca) in rows:
                if ra is rb: continue
                if any(i in SHORTCUT_INPUTS or i in BOSS_ITEMS for i in ra.get('ins', {})): continue
                if cb > ca * 1.25:
                    need = BETTER
                elif abs(cb - ca) <= ca * 0.25 and rb['age'] > ra['age']:
                    need = NEWER
                elif abs(cb - ca) <= ca * 0.25 and rb['age'] == ra['age'] and ra.get('file', '').startswith(rb['folder'] + '/'):
                    need = SAME    # two routes side by side on one machine: neither may be far worse than the other
                else:
                    continue
                if ub > ua * need:
                    # an old route made pointless by a new, cheaper one on a cheaper machine is a shortcut to look at, not a route to buff
                    kind = 'shortcut' if rb['age'] < ra['age'] and ca <= cb and ub > 8 * ua else 'weak'
                    problems.append(dict(item=o, bad=rb, good=ra, ub=ub, ua=ua, mb=mb, ma=ma, cb=cb, ca=ca, need=need, kind=kind))
    if verbose:
        seen = set()
        for p in sorted(problems, key=lambda p: (p['item'], p['bad']['file'])):
            k = (p['item'], p['bad']['file'])
            if k in seen: continue
            seen.add(k)
            print(f"{p['kind']:8s} {p['item'].split(':')[-1]:28s} {p['bad']['file']:58s} {p['ub']:9.2f}/u on {p['mb']}({p['cb']:.0f})  vs  "
                  f"{p['good']['file']:50s} {p['ua']:9.2f}/u on {p['ma']}({p['ca']:.0f})")
        print('problems:', len(seen))
    return problems, cost, mcost, recipes


if __name__ == '__main__':
    audit()


# ---------------------------------------------------------------------------------------------- fixing
# routes that recycle a by-product (their point is getting rid of waste, not making the output) and the few routes
# looked at by hand instead (see round57.py)
LEAVE = {'crafting/r38_lead_shielding_plate.json', 'fractionation/warm_helium_fraction.json', 'entropy_sorting/fallout_sorting.json', 'entropy_sorting/slag_sorting.json',
         'penrose/kugelblitz_seed.json', 'penrose/micro_singularity.json', 'plasma_smelting/chronosteel.json'}
MAX_K = 8


def _scale(path, k, main):
    d = json.load(open(path))
    for o in d.get('item_outputs', []):
        if o['id'] == main:
            o['count'] = int(min(64, math.ceil(o.get('count', 1) * k)))
    for o in d.get('fluid_outputs', []):
        if o['id'] == main:
            o['amount'] = int(math.ceil(o['amount'] * k))
    r = d.get('result')
    if isinstance(r, dict) and (r.get('id') or r.get('item')) == main:
        r['count'] = int(min(64, math.ceil(r.get('count', 1) * k)))
    json.dump(d, open(path, 'w'), indent=2)


def _cut_inputs(path, k):
    d = json.load(open(path))
    for i in d.get('item_inputs', []):
        i['count'] = max(1, int(round(i.get('count', 1) / k)))
    for x in d.get('fluid_inputs', []):
        x['amount'] = max(1, int(round(x['amount'] / k)))
    d['energy'] = max(1, int(d.get('energy', 0) / k)) if d.get('energy') else d.get('energy', 0)
    json.dump(d, open(path, 'w'), indent=2)


def fix(report_path=None, rounds=10):
    """Give every weak route what it needs to be worth using; remove machine routes for vanilla items that cannot be.
    Repeats until nothing changes (costs move as routes improve). Returns the report lines."""
    lines, removed = [], []
    grown = defaultdict(lambda: 1.0)       # how much each route's output has been raised so far
    for it in range(rounds):
        problems, cost, mcost, recipes = audit(verbose=False)
        want = {}
        for p in problems:
            b = p['bad']
            if b['file'] in LEAVE or b['file'].startswith('('):
                continue
            k = p['ub'] / (p['need'] * p['ua'] * 0.9)
            if b['file'] not in want or k > want[b['file']][0]:
                want[b['file']] = (k, p)
        if not want:
            break
        uses = defaultdict(int)
        for r in recipes:
            for i in set(r['ins']) | set(r['cats']):
                uses[i] += 1
        for f, (k, p) in sorted(want.items()):
            path = os.path.join(REC, f)
            # the only use of something: it stays (rebalanced), or that material would be left with nothing to do
            sole = any(uses[i] <= 1 for i in set(p['bad']['ins']) | set(p['bad'].get('cats', ())))
            if not os.path.exists(path):
                continue
            vanilla = base_cost(p['item']) == 1.0
            item = p['item'].split(':')[-1]
            folder = os.path.dirname(path)
            others = [x for x in os.listdir(folder) if x != os.path.basename(path)] if os.path.isdir(folder) else []
            # far beyond saving (over 64 times too dear), with something else left for its machine to do: it goes
            old = p['bad']['age'] == 0      # 1.0 recipes are rebalanced, never deleted: progression is built on them
            if not sole and ((vanilla and k > MAX_K) or (k > 64 and others and not old and not f.startswith('crafting/'))):
                os.remove(path)
                removed.append(f)
                lines.append(f'removed  {f}: makes {item} at {p["ub"]:.4g} each, against {p["ua"]:.4g} by {p["good"]["file"]}')
                continue
            ko = max(1.0, min(k, MAX_K / grown[f]))
            if ko > 1.0001:
                _scale(path, ko, p['item'])
                grown[f] *= ko
            msg = f'output x{ko:.2f}'
            if k > ko * 1.0001:
                _cut_inputs(path, k / ko)
                msg += f', inputs and power / {k / ko:.2f}'
            lines.append(f'pass {it + 1}: {f}: {msg}  ({item}: {p["ub"]:.4g} -> target {p["need"] * p["ua"]:.4g} each, vs {p["good"]["file"]})')
    left, _, _, _ = audit(verbose=False)
    shortcuts = [p for p in left if p['kind'] == 'shortcut']
    left = [p for p in left if p['bad']['file'] not in LEAVE and not p['bad']['file'].startswith('(') and p['kind'] != 'shortcut']
    lines.append(f'shortcuts (new routes that undercut an old one - checked by hand): {len(shortcuts)}')
    for p in shortcuts:
        lines.append(f'  {p["good"]["file"]} ({p["item"]}) {p["ua"]:.4g} undercuts {p["bad"]["file"]} {p["ub"]:.4g}')
    lines.append(f'unresolved: {len(left)}')
    for p in left:
        lines.append(f'  {p["bad"]["file"]} ({p["item"]}) {p["ub"]:.4g} vs {p["good"]["file"]} {p["ua"]:.4g}')
    if report_path:
        open(report_path, 'w').write('\n'.join(lines) + '\n')
    return lines, removed
