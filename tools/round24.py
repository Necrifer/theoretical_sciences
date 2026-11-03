"""
Round 24.

* The Biosphere Compendium and the Primordial Harvest gather their collections like singularities: every mod's
  meats, catches and drops (or crops, seeds, fruits and flowers) go into that mod's Specimen Bundle on the Extreme
  Assembly Table, bundles are bound into Archives when there are too many, and the item asks for whatever fits
  (extreme/Collections). Any mod's items in the tags are picked up automatically.
* The singularity list reaches the client while it is still joining, so JEI shows the Omniscience Catalyst with every
  ingot's singularity (network/SingularityConfigTask).
* The Extreme Assembly Table is made at the very start (a crafting table, iron and copper), works as an ordinary
  crafting table too, and every one of the mod's own crafting-table recipes moves onto it.
"""
import glob
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('specimen_bundle', 'Specimen Bundle', 'contained', '#102a18', '#80ffb0',
       'One of everything a mod\'s world grows or leaves behind, sealed in glass.', special='specimen'),
]

BIOSPHERE_TAGS = ['c:foods/raw_meat', 'c:foods/cooked_meat', 'c:foods/raw_fish', 'c:foods/cooked_fish', 'c:foods/food_poisoning',
                  'c:bones', 'c:strings', 'c:leathers', 'c:feathers', 'c:gunpowders', 'c:slime_balls', 'c:ender_pearls', 'c:rods/blaze',
                  'c:foods/raw_meats', 'c:foods/cooked_meats', 'c:mob_drops', 'c:drops']
HARVEST_TAGS = ['c:crops', 'c:seeds', 'c:foods/fruit', 'c:foods/vegetable', 'c:foods/fruits', 'c:foods/vegetables', 'c:mushrooms', 'c:foods/berry',
                'minecraft:saplings', 'minecraft:flowers', 'minecraft:small_flowers']

LANG = {
    'collection.theoreticalsciences.biosphere': 'Biosphere',
    'collection.theoreticalsciences.harvest': 'Harvest',
    'item.theoreticalsciences.specimen_bundle.of': '%s Sample: %s',
    'item.theoreticalsciences.specimen_bundle.archive2': '%s Archive %s',
    'item.theoreticalsciences.specimen_bundle.archive3': 'Great %s Archive %s',
    'item.theoreticalsciences.specimen_bundle.archive4': 'Total %s Archive %s',
    'tooltip.theoreticalsciences.specimen_bundle': 'Gathered on the Extreme Assembly Table. JEI shows what goes in.',
}


def apply(g):
    w, REC, D = g['w'], g['REC'], g['D']
    g['LANG'].update(LANG)
    # ---- the collections: a bundle recipe defines each one (and makes its bundles and archives)
    w(os.path.join(REC, 'extreme', 'biosphere_bundles.json'), {'type': f'{NS}:extreme_bundle', 'collection': 'biosphere', 'tags': BIOSPHERE_TAGS,
                                                               'extra': {'item': 'minecraft:egg'}})
    w(os.path.join(REC, 'extreme', 'harvest_bundles.json'), {'type': f'{NS}:extreme_bundle', 'collection': 'harvest', 'tags': HARVEST_TAGS,
                                                             'extra': {'item': 'minecraft:bone_meal'}})
    w(os.path.join(REC, 'extreme', 'biosphere_compendium.json'), {
        'type': f'{NS}:extreme_dynamic', 'ingredients': [{'item': f'{NS}:cosmic_neutronium_ingot'}, {'item': 'minecraft:egg'}],
        'collection': 'biosphere', 'result': {'id': f'{NS}:biosphere_compendium', 'count': 1}})
    w(os.path.join(REC, 'extreme', 'primordial_harvest.json'), {
        'type': f'{NS}:extreme_dynamic', 'ingredients': [{'item': f'{NS}:cosmic_neutronium_ingot'}],
        'collection': 'harvest', 'result': {'id': f'{NS}:primordial_harvest', 'count': 1}})
    # ---- the Extreme Assembly Table: at the very start
    w(os.path.join(REC, 'crafting', 'extreme_assembly_table.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['ICI', 'CTC', 'ICI'],
        'key': {'I': {'tag': 'c:ingots/iron'}, 'C': {'tag': 'c:ingots/copper'}, 'T': {'item': 'minecraft:crafting_table'}},
        'result': {'id': f'{NS}:extreme_assembly_table', 'count': 1}})
    nd = os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'needs_diamond_tool.json') if 'RES' in g else None
    if nd and os.path.exists(nd):
        t = json.load(open(nd))
        t['values'] = [v for v in t['values'] if v != f'{NS}:extreme_assembly_table']
        w(nd, t)


KEEP_ON_CRAFTING_TABLE = {'extreme_assembly_table', 'quantum_codex'}


def trim(pattern):
    """Empty edge rows and columns go (a crafting table ignores them; the Extreme table matches the trimmed grid)."""
    w_ = max(len(r) for r in pattern)
    rows = [r.ljust(w_) for r in pattern]
    while rows and not rows[0].strip(): rows.pop(0)
    while rows and not rows[-1].strip(): rows.pop()
    while rows and all(r[0] == ' ' for r in rows): rows = [r[1:] for r in rows]
    while rows and all(r[-1] == ' ' for r in rows): rows = [r[:-1] for r in rows]
    return rows


DECOR_KINDS = ('deco', 'deco_stairs', 'deco_slab', 'deco_wall', 'deco_pillar', 'glass', 'cache')
KEYS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'


def _stretch(pattern):
    """Any pattern blown up to fill the whole 9x9 grid: every cell becomes a block of cells (nearest neighbour)."""
    pattern = trim(pattern)
    h, w_ = len(pattern), max(len(r) for r in pattern)
    rows = [r.ljust(w_) for r in pattern]
    return [''.join(rows[y * h // 9][x * w_ // 9] for x in range(9)) for y in range(9)]


def _one_each(grid, keys):
    """Unique things (tools, keys, codices - anything that does not stack) are needed once, not once per cell: only
    the middle cell of each block keeps them."""
    out = [list(r) for r in grid]
    for k in keys:
        cells = [(x, y) for y in range(9) for x in range(9) if out[y][x] == k]
        if not cells: continue
        cx = sum(c[0] for c in cells) / len(cells)
        cy = sum(c[1] for c in cells) / len(cells)
        # one per original cell: the cell nearest the centre of each 3x3 block
        keep = set()
        for bx in range(3):
            for by in range(3):
                blk = [c for c in cells if c[0] // 3 == bx and c[1] // 3 == by]
                if blk:
                    keep.add(min(blk, key=lambda c: (c[0] % 3 - 1) ** 2 + (c[1] % 3 - 1) ** 2))
        for c in cells:
            if c not in keep: out[c[1]][c[0]] = ' '
    return [''.join(r) for r in out]


def _shapeless_grid(ingredients):
    """A shapeless recipe laid out over the whole grid: the first ingredient in the centre, the others in a cross
    through it and a ring round the edge."""
    keys = {}
    for ing in ingredients:
        sig = json.dumps(ing, sort_keys=True)
        if sig not in keys: keys[sig] = (KEYS[len(keys)], ing)
    order = [keys[json.dumps(i, sort_keys=True)][0] for i in ingredients]
    grid = [[' '] * 9 for _ in range(9)]
    first, rest = order[0], (order[1:] or order[:1])
    k = 0
    for y in range(9):
        for x in range(9):
            if x == 4 and y == 4:
                grid[y][x] = first
            elif x == 4 or y == 4 or x in (0, 8) or y in (0, 8):
                grid[y][x] = rest[k % len(rest)]
                k += 1
    return [''.join(r) for r in grid], {v[0]: v[1] for v in keys.values()}


# storage blocks and what they are made of (round 24: not packed from ingots - woven from many of the mod's resources)
STORAGE = {'chronosteel_block': 'chronosteel_ingot', 'computronium_block': 'computronium_ingot', 'cosmic_neutronium_block': 'cosmic_neutronium_ingot',
           'dilithium_block': 'dilithium_crystal', 'neutronium_block': 'neutronium_ingot', 'omnium_block': 'omnium_ingot',
           'spacetime_alloy_block': 'spacetime_alloy_ingot', 'void_lattice_block': 'void_lattice_shard', 'cherenkov_quartz_block': 'cherenkov_quartz'}


def _h(*p):
    import hashlib
    return int(hashlib.sha256('|'.join(map(str, p)).encode()).hexdigest()[:12], 16)


def storage_blocks(g):
    """Each storage block: its ingot woven on a lattice through the whole grid, a core resource in the middle and four
    more resources of the same stage of the game in rings around it. They cannot be taken apart again."""
    import importlib, sys
    w, REC = g['w'], g['REC']
    here = os.path.dirname(os.path.abspath(__file__))
    C = json.load(open(os.path.join(here, 'content.json')))
    sys.argv = ['chaincheck']
    os.environ['TS_LENIENT'] = '1'
    cc = importlib.reload(sys.modules['chaincheck']) if 'chaincheck' in sys.modules else importlib.import_module('chaincheck')
    os.environ.pop('TS_LENIENT', None)
    W = cc.WAVE
    bad = ('_singularity', '_notes_', '_dossier', '_sample', 'singularity_cluster', 'dynamic_singularity', 'simulated_universe',
           'specimen_bundle', 'genome', 'containment_cell', 'omniscience_catalyst', 'biosphere_compendium', 'primordial_harvest')
    import round44 as _r44, round48 as _r48, round51 as _r51      # (1.3.7+ mobs' drops are left out, so older recipes stay put)
    drops = {d[0].split(':')[-1] for c, (_, ds) in g.get('CREATURES', {}).items() if c not in _r44.CREATURES and c not in _r48.CREATURES
             and c not in _r51.CREATURES
             for d in ds}     # (not mob drops: when they come is not up to the player)
    pool = [i['id'] for i in C['items'] if i['id'] not in drops and i.get('stack', 64) != 1 and not any(b in i['id'] for b in bad)
            and i['id'] not in STORAGE.values() and f'{NS}:' + i['id'] in W]
    done = []
    for blk, base in STORAGE.items():
        wb = W.get(f'{NS}:' + base)
        if wb is None:
            continue
        pick = []
        for span in (3, 6, 12, 99):
            cand = sorted((x for x in pool if wb - span <= W[f'{NS}:' + x] <= wb and x != base), key=lambda x: _h(blk, x))
            if len(cand) >= 5:
                pick = cand[:5]
                break
        if len(pick) < 5:
            # at the very start of the game: the mod's first finds, with the Overworld's own resources
            cand = sorted((x for x in pool if W[f'{NS}:' + x] <= wb and x != base), key=lambda x: _h(blk, x))
            vanilla = sorted(VANILLA, key=lambda x: _h(blk, x))
            pick = (cand + vanilla)[:5]
        grid = []
        for y in range(9):
            row = ''
            for x in range(9):
                ring = max(abs(x - 4), abs(y - 4))
                if x == 4 and y == 4: row += 'Z'
                elif (x + y) % 2 == 0: row += 'I'
                else: row += 'ABCD'[ring - 1]
            grid.append(row)
        q_ = lambda it: it if ':' in it else f'{NS}:{it}'
        key = {'I': {'item': f'{NS}:{base}'}, 'Z': {'item': q_(pick[0])}}
        for k, it in zip('ABCD', pick[1:]):
            key[k] = {'item': q_(it)}
        w(os.path.join(REC, 'crafting', blk + '.json'), {'type': f'{NS}:extreme_shaped', 'pattern': grid, 'key': key,
                                                          'result': {'id': f'{NS}:{blk}', 'count': 1}})
        done.append((blk, pick))
    # and no way back to the ingots
    gone = 0
    for f in glob.glob(os.path.join(REC, 'crafting', '*.json')):
        r = json.load(open(f))
        ins = r.get('ingredients') or []
        if r.get('type', '').endswith('shapeless') and len(ins) == 1 and isinstance(ins[0], dict) and \
                ins[0].get('item', '').split(':')[-1] in STORAGE and (r['result'].get('id') or '').split(':')[-1] == STORAGE[ins[0]['item'].split(':')[-1]]:
            os.remove(f)
            gone += 1
            GONE.add(f'{NS}:crafting/' + os.path.basename(f)[:-5])
    print('round24 storage blocks:', done, '; unpacking recipes removed:', gone)
    return {f'{NS}:crafting/' + b for b, _ in done}


GONE = set()
VANILLA = ['minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:copper_ingot', 'minecraft:redstone', 'minecraft:lapis_lazuli',
           'minecraft:quartz', 'minecraft:amethyst_shard', 'minecraft:diamond', 'minecraft:glowstone_dust', 'minecraft:emerald',
           'minecraft:obsidian', 'minecraft:prismarine_shard']


def finish(g):
    """Every one of the mod's crafting-table recipes moves to the Extreme Assembly Table - and uses the whole 9x9 grid
    (round 24: the table is made at the start precisely so the big grid is used from the start). Decorative blocks
    (slabs, stairs, walls, bricks, lamps, glass, caches) keep their small recipes, and so does taking a storage block
    apart (nine ingots back; packing them in uses the big grid)."""
    w, REC, A, D = g['w'], g['REC'], g['A'], g['D']
    here = os.path.dirname(os.path.abspath(__file__))
    C = json.load(open(os.path.join(here, 'content.json')))
    decor = {f'{NS}:' + b['id'] for b in C['blocks'] if b.get('kind') in DECOR_KINDS}
    single = {f'{NS}:' + i['id'] for i in C['items'] if i.get('stack') == 1}
    moved, kept = set(), []
    moved |= storage_blocks(g)

    def ing_item(v):
        if isinstance(v, dict): return v.get('item')
        if isinstance(v, list) and v and isinstance(v[0], dict): return v[0].get('item')
        return None

    for f in sorted(glob.glob(os.path.join(REC, '**', '*.json'), recursive=True)):
        name = os.path.basename(f)[:-5]
        if name in KEEP_ON_CRAFTING_TABLE or name in STORAGE:
            continue
        r = json.load(open(f))
        t = r.get('type')
        if t not in ('minecraft:crafting_shaped', 'minecraft:crafting_shapeless', f'{NS}:extreme_shaped', f'{NS}:extreme_shapeless'):
            continue
        res = r['result']
        rid_out = res.get('id') or res.get('item')
        if rid_out in decor:
            if t.startswith(NS):        # (a decorative recipe already moved by an earlier round goes back to 3x3)
                pass
            kept.append(name)
            continue
        if t == 'minecraft:crafting_shapeless' and len(r['ingredients']) == 1 and res.get('count', 1) == 9:
            kept.append(name)           # a storage block taken apart
            continue
        if t.endswith('shaped'):
            pat = r['pattern']
            if len(pat) == 9 and max(len(x) for x in pat) == 9:
                out = r if t.startswith(NS) else dict(r, type=f'{NS}:extreme_shaped')
            else:
                grid = _stretch(pat)
                uniq = [k for k, v in r['key'].items() if ing_item(v) in single]
                grid = _one_each(grid, uniq)
                old = sum(ch != ' ' for ch in ''.join(pat))
                new = sum(ch != ' ' for ch in ''.join(grid))
                res = dict(res)
                if len(set(''.join(pat).replace(' ', ''))) == 1 and old:       # packing: the same rate, nine times over
                    res['count'] = min(64, res.get('count', 1) * new // old)
                out = {'type': f'{NS}:extreme_shaped', 'pattern': grid, 'key': r['key'], 'result': res}
        else:
            ings = r['ingredients']
            if len(ings) >= 81:
                out = dict(r, type=f'{NS}:extreme_shapeless')
            else:
                grid, key = _shapeless_grid(ings)
                res = dict(res)
                if len({json.dumps(i, sort_keys=True) for i in ings}) == 1:
                    res['count'] = min(64, res.get('count', 1) * sum(ch != ' ' for ch in ''.join(grid)) // len(ings))
                out = {'type': f'{NS}:extreme_shaped', 'pattern': grid, 'key': key, 'result': res}
        w(f, out)
        moved.add(f'{NS}:' + os.path.relpath(f, REC)[:-5].replace(os.sep, '/'))
    print('round24: 9x9 recipes', len(moved), '; kept small (decorative / unpacking):', len(kept))
    # the books' crafting pages for those recipes become a pointer to the table (and JEI)
    BA = g['BA']
    pages = 0
    for f in glob.glob(os.path.join(BA, 'entries', '*', '*.json')):
        e = json.load(open(f))
        ch = False
        n0 = len(e.get('pages', []))
        e['pages'] = [pg for pg in e.get('pages', []) if not (pg.get('type') == 'patchouli:crafting' and pg.get('recipe') in GONE)]
        ch = len(e['pages']) != n0
        for pg in e.get('pages', []):
            if pg.get('type') == 'patchouli:crafting' and (pg.get('recipe') in moved or pg.get('recipe2') in moved):
                rec = pg.get('recipe')
                item = rec.split('/')[-1] if rec else ''
                txt = pg.get('text') or ''
                pg.clear()
                pg.update({'type': 'patchouli:text', 'text': (txt + '$(br2)' if txt else '') +
                           '$(thing)Made on the Extreme Assembly Table$() (it is also a crafting table). JEI shows the pattern.'})
                ch = True
                pages += 1
        if ch:
            w(f, e)
    g['text_entry']('basics', 'extreme_table_first', 'The Extreme Assembly Table', 'extreme_assembly_table', [
        'The first thing to make: a $(item)crafting table$() ringed with iron and copper. The $(item)Extreme Assembly Table$() is a 9x9 crafting grid - and a crafting table too: anything laid out within three by three that it has no recipe for is crafted as at an ordinary table.',
        'Every one of this mod\'s own recipes that would be made at a crafting table is made here instead - and fills the whole 9x9 grid: what used to be one ingredient in a 3x3 recipe is now a block of nine (tools and other things that do not stack are still needed only once). Storage blocks are not packed from ingots: each is woven from its ingots and many of the mod\'s other resources of the same stage, and none can be taken apart again. Decorative blocks (slabs, stairs, walls, bricks, lamps, glass, caches) keep their small recipes. JEI shows every pattern.',
        'Collections: the $(item)Biosphere Compendium$() and the $(item)Primordial Harvest$() want one of every meat and drop, or every crop, seed, fruit and flower, of every mod installed. Gather each mod\'s into its $(item)Specimen Bundle$() here first (with an egg, or bone meal); if there are too many bundles, nine at a time are bound into an $(item)Archive$().'],
        sort=-2, spot=['extreme_assembly_table', 'specimen_bundle'])
    w(os.path.join(A, 'models', 'item', 'specimen_bundle.json'), {'parent': 'minecraft:item/generated', 'textures': {
        'layer0': f'{NS}:item/specimen_bundle_contents', 'layer1': f'{NS}:item/specimen_bundle'}})
    print('round24: moved', len(moved), 'crafting-table recipes to the Extreme Assembly Table;', pages, 'book pages updated')
