"""
Shared writers for the 1.10+ rounds (51 onward): items/blocks, research with automatic map placement, puzzles, machine
and crafting recipes. Each round module keeps the same interface gen_data expects (KEY_ADD, MACHINE_TEXT, MC, TIER, ...).
"""
import glob
import json
import math
import os

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
RDIR = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', NS, 'ts_research')

# every research node placed by a kit round, in placement order (filled as rounds are imported)
KIT_NODES = {}
# every id any kit round has ever placed (kept on disk), so that their own earlier files never count as occupied cells
_IDS_FILE = os.path.join(HERE, 'kit_nodes.json')
KIT_IDS = set(json.load(open(_IDS_FILE))) if os.path.exists(_IDS_FILE) else set()


def I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


def B(i, n, art, c1, c2, kind='casing', light=2, hardness=5.0, tool='iron', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=light, hardness=hardness, tool=tool)
    d.update(kw)
    return d


def ORE(i, n, base, c1, c2, drops, light=0, hardness=4.0, tool='iron'):
    return dict(id=i, name=n, art='ore', base=base, c1=c1, c2=c2, kind='ore', drops=drops, light=light, hardness=hardness, tool=tool)


def FL(i, n, c1, c2, gas=False, light=0, temp=300, density=1000, viscosity=1000, art='condensate'):
    return dict(id=i, name=n, c1=c1, c2=c2, gas=gas, light=light, temp=temp, density=density, viscosity=viscosity, art=art)


def _old_positions():
    pos = {}
    for f in glob.glob(os.path.join(RDIR, '*.json')):
        rid = os.path.basename(f)[:-5]
        if rid in KIT_NODES or rid in KIT_IDS: continue
        d = json.load(open(f))
        pos[rid] = (d['x'], d['y'])
    return pos


def place(research):
    """Give each (rid, title, desc, icon, x, y, ...) tuple with x None a free map cell near its prerequisites. Deterministic:
    only nodes from before the kit rounds, and kit nodes placed earlier, are taken as occupied."""
    new = {r[0] for r in research} - KIT_IDS
    if new:
        KIT_IDS.update(new)
        json.dump(sorted(KIT_IDS), open(_IDS_FILE, 'w'), indent=0)
    pos = _old_positions()
    for rid, xy in KIT_NODES.items():
        pos[rid] = xy
    taken = set(pos.values())
    out = []
    for r in research:
        rid, t, desc, icon, x, y, ins, req, cost = r
        if rid in KIT_NODES:
            x, y = KIT_NODES[rid]
        elif x is None:
            ps = [pos[q] for q in req if q in pos]
            cx = sum(p[0] for p in ps) / len(ps) + 1.5 if ps else 12
            cy = sum(p[1] for p in ps) / len(ps) if ps else 0
            best = None
            for dx in range(-8, 9):
                for dy in range(-8, 9):
                    c = (round(cx) + dx, round(cy) + dy)
                    if c in taken or c[0] < 0 or c[1] < -12 or c[1] > 20: continue
                    crowd = sum(1 for e in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (c[0] + e[0], c[1] + e[1]) in taken)
                    score = math.hypot(c[0] - cx, c[1] - cy) + 1.5 * crowd + (0 if c[0] >= max([p[0] for p in ps] or [0]) else 2)
                    if best is None or score < best[0]: best = (score, c)
            x, y = best[1]
        KIT_NODES[rid] = (x, y)
        pos[rid] = (x, y)
        taken.add((x, y))
        out.append((rid, t, desc, icon, x, y, ins, req, cost))
    return out


def write_research(g, research):
    w, D, q = g['w'], g['D'], g['q']
    for rid, t, desc, icon, x, y, ins, req, cost in research:
        w(os.path.join(D, 'ts_research', rid + '.json'), {
            'title': t, 'description': desc, 'icon': q(icon), 'x': x, 'y': y, 'insight': ins,
            'requires': [q(r) for r in req], 'cost': [{'item': q(c), 'count': n} for c, n in cost],
            'time': 0 if ins < 70 else int(round(0.6 * ins ** 1.35 / 30.0)) * 30})
        g['LANG'][f'research.{NS}.{rid}'] = t
        g['LANG'][f'research.{NS}.{rid}.desc'] = desc
        w(os.path.join(D, 'advancement/research', rid + '.json'), {
            'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]})
        r = (rid, t, desc, icon, x, y, ins, list(req), cost)
        g['DEFS'][rid] = r
        if all(e[0] != rid for e in g['RESEARCH']):
            g['RESEARCH'].append(r)


def write_puzzles(tag, puzzles):
    p = os.path.join(HERE, 'puzzles2', f'group_{tag}.json')
    data = json.dumps(puzzles, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def recipes(g, prefix, recs):
    """recs: (name, type, item ins, fluid ins, item outs, fluid outs, FE/t, ticks, research, insight[, catalysts[, chances]])."""
    REC, w = g['REC'], g['w']
    for r in recs:
        name, typ, ins, fins, outs, fouts, fe, t, res, insight = r[:10]
        cats = r[10] if len(r) > 10 else []
        ch = r[11] if len(r) > 11 else None
        g['mr'](prefix + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res, catalysts=cats, chances=ch)
        p = os.path.join(REC, typ, prefix + name + '.json')
        j = json.load(open(p))
        j['unscaled'] = True
        if insight: j['insight_per_run'] = insight
        w(p, j)


def crafting(g, prefix, shaped=(), shapeless=()):
    for name, pat, key, res, n in shaped:
        g['shaped'](prefix + name, pat, key, res, n)
    for name, items, res, n in shapeless:
        g['shapeless'](prefix + name, items, res, n)


def extend_tag(g, p, ids):
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for i in ids:
        if i not in t['values']:
            t['values'].append(i)
    g['w'](p, t)


def pickaxe(g, blocks):
    extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), [f'{NS}:{b["id"]}' for b in blocks])
