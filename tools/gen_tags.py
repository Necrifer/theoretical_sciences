"""
1.1.0: item and block tags for the whole mod, so every item has the tags a pack maker, KubeJS script or other mod looks
for (seen with /kjs hand). Two kinds:

* Conventional (c:) tags only where they are safe: glass blocks, spawn eggs, and the few real everyday materials
  (lithium, hafnium, magnetite). The mod's exotic materials share names with other mods' unrelated items, and a shared
  c: tag would let unification swap them.
* The mod's own category tags (theoreticalsciences:...), for every family of items: casings, machines, processors,
  singularities, containment cells, lenses, crystals, shards, boules, records, spawn eggs, armour sets...

Adds to the tags gen_data writes (it never removes an entry). Run after gen_data (tools/regenerate.sh).
"""
import json
import os
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
LANG = os.path.join(RES, 'assets', 'theoreticalsciences', 'lang', 'en_us.json')
NS = 'theoreticalsciences'


def write(ns, kind, path, ids):
    p = os.path.join(RES, 'data', ns, 'tags', kind, path + '.json')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    have = {v if isinstance(v, str) else v.get('id') for v in t['values']}
    for i in sorted(ids):
        if i not in have:
            t['values'].append(i)
    json.dump(t, open(p, 'w'), indent=2)


def main():
    L = json.load(open(LANG))
    items = {k.split('.')[2] for k in L if k.startswith(f'item.{NS}.') and k.count('.') == 2}
    blocks = {k.split('.')[2] for k in L if k.startswith(f'block.{NS}.') and k.count('.') == 2}
    machines = {m['id'] for m in json.load(open(os.path.join(HERE, 'machines.json')))}
    C = json.load(open(os.path.join(HERE, 'content.json')))
    kind = {b['id']: b.get('kind') for b in C['blocks']}
    allids = items | blocks
    q = lambda i: f'{NS}:{i}'
    it = defaultdict(set)        # (namespace, path) -> item ids
    bl = defaultdict(set)        # (namespace, path) -> block ids

    # ---------------------------------------------------------------- conventional tags
    # Only real, everyday materials get material tags. Many of the mod's names are shared with other mods' very different
    # items (neutronium, computronium, cosmic neutronium...), and a shared c: tag would let unification (Almost
    # Unified) swap one for the other in every recipe of the pack.
    REAL = {('ingots', 'lithium_ingot', 'lithium'), ('dusts', 'hafnium_dust', 'hafnium'), ('storage_blocks', 'magnetite_block', 'magnetite')}
    for form, i, m in REAL:
        if i in allids:
            it[('c', form)].add(i)
            it[('c', form + '/' + m)].add(i)
            if i in blocks:
                bl[('c', form)].add(i)
                bl[('c', form + '/' + m)].add(i)
    for i in allids:
        if i in blocks and (i.endswith('_glass') or kind.get(i) == 'glass'):
            it[('c', 'glass_blocks')].add(i)
            bl[('c', 'glass_blocks')].add(i)
        if i.endswith('_spawn_egg'):
            it[('c', 'spawn_eggs')].add(i)

    # ---------------------------------------------------------------- the mod's own families
    fam = {
        'casings': lambda i: i.endswith('_casing') or kind.get(i) == 'casing',
        'processors': lambda i: i.endswith('_processor'),
        'singularities': lambda i: i.endswith('_singularity') or i in ('dynamic_singularity', 'singularity_cluster'),
        'containment_cells': lambda i: i.endswith('_cell') and i in items and not i.endswith(('_fuel_cell',)),
        'lenses': lambda i: i.endswith('_lens') or i in ('photonic_lens',),
        'crystals': lambda i: i.endswith('_crystal') and i in items,
        'shards': lambda i: i.endswith('_shard'),
        'boules': lambda i: i.endswith('_boule'),
        'ingots': lambda i: i.endswith('_ingot'),
        'records': lambda i: i.endswith('_record') or i.startswith('gw_record'),
        'samples': lambda i: i.endswith('_sample'),
        'spawn_eggs': lambda i: i.endswith('_spawn_egg'),
        'ores': lambda i: i.endswith('_ore') and i in blocks,
        'research_notes': lambda i: i.endswith(('_notes', '_dossier')) or '_notes_' in i,
        'machines': lambda i: i in machines,
        'multiblock_controllers': lambda i: i in machines and False,
        'decorative_blocks': lambda i: kind.get(i, '').startswith('deco'),
        'glass': lambda i: i in blocks and (i.endswith('_glass') or kind.get(i) == 'glass'),
    }
    mb = {m['id'] for m in json.load(open(os.path.join(HERE, 'machines.json'))) if m.get('multiblock')}
    fam['multiblock_controllers'] = lambda i: i in mb
    for f, test in fam.items():
        for i in allids:
            if test(i):
                it[(NS, f)].add(i)
                if i in blocks: bl[(NS, f)].add(i)
    # by science: the Codex chapter each item is written up in (#theoreticalsciences:field/optics, .../nuclear ...)
    E = json.load(open(os.path.join(RES, 'assets', NS, 'ts_codex', 'entries.json')))
    for e in E.values():
        cat = e.get('category')
        if not cat: continue
        for pg in e.get('pages', []):
            for x in pg.get('items', []) or []:
                if isinstance(x, str) and x.startswith(NS + ':'):
                    i = x.split(':')[1]
                    if i in allids:
                        it[(NS, 'field/' + cat)].add(i)
                        if i in blocks: bl[(NS, 'field/' + cat)].add(i)
    # armour sets: every suit's four pieces
    for i in allids:
        for piece in ('_helmet', '_chestplate', '_leggings', '_boots'):
            if i.endswith(piece) and i in items:
                it[(NS, 'armour')].add(i)
                it[(NS, 'armour/' + i[:-len(piece)])].add(i)

    # only ids the game really registers (an item model / a blockstate): one wrong entry would throw the whole tag away
    A = os.path.join(RES, 'assets', NS)
    is_item = lambda i: os.path.exists(os.path.join(A, 'models', 'item', i + '.json'))
    is_block = lambda i: os.path.exists(os.path.join(A, 'blockstates', i + '.json'))
    n = 0
    for (ns, path), ids in it.items():
        ids = {q(i) for i in ids if is_item(i)}
        if ids:
            write(ns, 'item', path, ids)
            n += 1
    for (ns, path), ids in bl.items():
        ids = {q(i) for i in ids if is_block(i)}
        if ids:
            write(ns, 'block', path, ids)
            n += 1
    tagged = set().union(*it.values())
    print(f'tags: {n} tag files; {len(tagged)} of {len(allids)} items and blocks in at least one family')


if __name__ == '__main__':
    main()
