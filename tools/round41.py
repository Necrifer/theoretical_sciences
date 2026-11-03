"""
Round 41 (1.3.3, "Deep Riches"): discovery in the Planetary Interior.

* Crystal pockets (geodes, but bigger) of four gems: Corundum and Garnet in the crust, Peridot and - rarer, deeper - Moissanite
  in the mantle. The crystal blocks are impure masses that break into gems.
* Rich veins: Rich Iron, Copper and Gold ores, in bigger veins than the Overworld's. Their chunks smelt to two ingots, and
  the Spectrograph's assay gets three plus what rides with them in nature (iron: magnetite; copper: gold; gold: uraninite).
* The Moissanite Drill: the first pick that can mine Iron-Nickel and Core Magnetite Ore.
"""
import json
import os

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


def _B(i, n, art, c1, c2, kind='simple', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=kw.pop('light', 0), hardness=kw.pop('hardness', 3.0), tool=kw.pop('tool', 'stone'))
    d.update(kw)
    return d


GEMS = {
    # gem: (name, colours, lore, crystal block name, where)
    'corundum': ('Corundum', '#3050c0', '#a0c0ff', 'Aluminium oxide, as hard as anything but diamond. Coloured by a trace of iron and titanium it is '
                 'sapphire; by chromium, ruby. Clear and hard, it makes the best lenses and bearings.', 'Corundum Crystal Mass'),
    'garnet': ('Garnet', '#801828', '#ff6070', 'Pyrope garnet, born where crust is dragged down and squeezed. Hard, dense and cheap: the grit of '
               'every cutting wheel.', 'Garnet Crystal Mass'),
    'peridot': ('Peridot', '#507010', '#c0ff40', 'Gem olivine, the mineral the upper mantle is mostly made of. It forms only deep down and only '
                'reaches the surface carried up in lava.', 'Peridot Crystal Mass'),
    'moissanite': ('Moissanite', '#2a5060', '#e0ffff', 'Silicon carbide, found in nature only in a few meteorites and in mantle rock. Harder than any '
                   'gem but diamond, and far tougher: a drill bit of it cuts iron.', 'Moissanite Crystal Mass'),
}
ITEMS = [_I(g, n, 'gem', c1, c2, lore) for g, (n, c1, c2, lore, _) in GEMS.items()] + [
    _I('rich_iron_chunk', 'Rich Iron Chunk', 'chunk', '#6a4030', '#d8a080', 'Banded iron from deep down: richer than any Overworld ore, with magnetite '
       'in its layers.'),
    _I('rich_copper_chunk', 'Rich Copper Chunk', 'chunk', '#6a3a20', '#e09050', 'Porphyry copper, veined with gold that came up with it.'),
    _I('rich_gold_chunk', 'Rich Gold Chunk', 'chunk', '#6a5010', '#ffe060', 'Reef gold in quartz pebbles - the kind of ore that, on Earth, carries '
       'uraninite too.'),
    _I('moissanite_drill', 'Moissanite Drill', 'probe', '#2a5060', '#e0ffff', 'A moissanite-tipped drill on a netherite shank: it mines Iron-Nickel '
       'and Core Magnetite Ore, which nothing softer can.', special='moissanitedrill', stack=1),
]
BLOCKS = [_B(g + '_crystal', n_block, 'crystalblock', c1, c2, light=4, hardness=4.0, tool='iron') for g, (n, c1, c2, lore, n_block) in GEMS.items()] + [
    _B('geode_shell', 'Geode Shell', 'polished', '#9a9080', '#d8d0c0', hardness=2.0),
    dict(id='rich_iron_ore', name='Rich Iron Ore', art='ore', base='crustal_rock', c1='#6a4030', c2='#d8a080', kind='ore', drops='rich_iron_chunk',
         light=0, hardness=4.0, tool='stone'),
    dict(id='rich_copper_ore', name='Rich Copper Ore', art='ore', base='crustal_rock', c1='#6a3a20', c2='#e09050', kind='ore', drops='rich_copper_chunk',
         light=0, hardness=4.0, tool='stone'),
    dict(id='rich_gold_ore', name='Rich Gold Ore', art='ore', base='basalt_column', c1='#6a5010', c2='#ffe060', kind='ore', drops='rich_gold_chunk',
         light=0, hardness=4.5, tool='iron'),
]
FLUIDS = []

# where each gem's pockets form: (biome, y range, 1-in-N chunks, size)
GEODES = {
    'corundum': ('crustal_caverns', (80, 120), 10, 1.0),
    'garnet': ('crustal_caverns', (76, 120), 8, 1.0),
    'peridot': ('mantle_convection_zone', (24, 48), 6, 1.2),
    'moissanite': ('mantle_convection_zone', (18, 32), 14, 0.9),
}
RICH = [('rich_iron', 'crustal_rock', 'crustal_caverns', 14, 10, 50, 126), ('rich_copper', 'crustal_rock', 'crustal_caverns', 12, 8, 50, 126),
        ('rich_gold', 'basalt_column', 'magma_chambers', 10, 7, 12, 96)]
ASSAY = [('rich_iron_chunk', 'minecraft:iron_ingot', 'magnetite'), ('rich_copper_chunk', 'minecraft:copper_ingot', 'minecraft:gold_nugget'),
         ('rich_gold_chunk', 'minecraft:gold_ingot', 'raw_pitchblende')]
HARD = [f'{NS}:core_iron', f'{NS}:core_magnetite_ore']          # only the Moissanite Drill gets their drops

LANG = {
    'death.attack.theoreticalsciences.basaltic_magma': '%1$s was melted into the mantle',
    'death.attack.theoreticalsciences.basaltic_magma.player': '%1$s was melted into the mantle whilst fighting %2$s',
    'tooltip.theoreticalsciences.magma_proof': 'A full set protects against Basaltic Magma',
}


def _loot_gems(g):
    LT = os.path.join(g['D'], 'loot_table', 'blocks')
    for gem in GEMS:
        bid = gem + '_crystal'
        g['w'](os.path.join(LT, bid + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': f'{NS}:{bid}', 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {
                'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
            {'type': 'minecraft:item', 'name': f'{NS}:{gem}', 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': 2, 'max': 4}},
                {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:uniform_bonus_count',
                 'parameters': {'bonusMultiplier': 1}},
                {'function': 'minecraft:explosion_decay'}]}]}]}]})


def _worldgen(g):
    w, WG, q = g['w'], g['WG'], g['q']
    per_biome = {}
    for gem, (biome, (lo, hi), rarity, size) in GEODES.items():
        name = gem + '_pocket'
        st = lambda b: {'type': 'minecraft:simple_state_provider', 'state': {'Name': b}}
        w(os.path.join(WG, 'configured_feature', name + '.json'), {'type': 'minecraft:geode', 'config': {
            'blocks': {'filling_provider': st('minecraft:air'), 'inner_layer_provider': st(q(gem + '_crystal')),
                       'alternate_inner_layer_provider': st(q(gem + '_crystal')), 'middle_layer_provider': st(q('geode_shell')),
                       'outer_layer_provider': st(q('basalt_column') if biome == 'crustal_caverns' else q('peridotite')),
                       'inner_placements': [{'Name': q(gem + '_crystal')}], 'cannot_replace': '#minecraft:features_cannot_replace',
                       'invalid_blocks': '#minecraft:geode_invalid_blocks'},
            'layers': {'filling': 1.7 * size * 1.4, 'inner_layer': 2.2 * size * 1.4, 'middle_layer': 3.2 * size * 1.4, 'outer_layer': 4.2 * size * 1.4},
            'crack': {'generate_crack_chance': 0.6, 'base_crack_size': 2.0, 'crack_point_offset': 2},
            'use_potential_placements_chance': 0.5, 'use_alternate_layer0_chance': 0.0, 'placements_require_layer0_alternate': False,
            'outer_wall_distance': {'type': 'minecraft:uniform', 'min_inclusive': 5, 'max_inclusive': 7},
            'distribution_points': {'type': 'minecraft:uniform', 'min_inclusive': 4, 'max_inclusive': 5},
            'point_offset': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 2},
            'min_gen_offset': -16, 'max_gen_offset': 16, 'noise_multiplier': 0.05, 'invalid_blocks_threshold': 1}})
        w(os.path.join(WG, 'placed_feature', name + '.json'), {'feature': q(name), 'placement': [
            {'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
            {'type': 'minecraft:biome'}]})
        per_biome.setdefault(biome, []).append(q(name))
    ores = {}
    for name, host, biome, size, count, lo, hi in RICH:
        g['ore_feature']('ore_' + name, {'predicate_type': 'minecraft:block_match', 'block': q(host)}, name + '_ore', size, count, lo, hi)
        ores.setdefault(biome, []).append(q('ore_' + name))
    return per_biome, ores


def _patch_biomes(g, geodes, ores):
    WG = g['WG']
    for b in set(geodes) | set(ores):
        p = os.path.join(WG, 'biome', b + '.json')
        d = json.load(open(p))
        for f in geodes.get(b, []):
            if f not in d['features'][2]: d['features'][2].append(f)            # local modifications (where vanilla puts geodes)
        for f in ores.get(b, []):
            if f not in d['features'][6]: d['features'][6].append(f)
        g['w'](p, d)


def _recipes(g):
    REC, w, q = g['REC'], g['w'], g['q']
    for chunk, ingot, extra in ASSAY:
        for kind, t in (('smelting', 200), ('blasting', 100)):
            w(os.path.join(REC, 'crafting', f'r41_{chunk}_{kind}.json'), {
                'type': f'minecraft:{kind}', 'category': 'misc', 'ingredient': {'item': q(chunk)}, 'result': {'id': ingot, 'count': 2},
                'experience': 0.8, 'cookingtime': t})
        g['mr']('r41_assay_' + chunk, 'spectroscopy', [(chunk, 2), ('spectral_plate', 1)], [], [(ingot, 6), (extra, 1)], [], energy=20000,
                duration=200, research='spectroscopy')
        p = os.path.join(REC, 'spectroscopy', 'r41_assay_' + chunk + '.json')
        r = json.load(open(p)); r['unscaled'] = True; w(p, r)
    g['shaped']('r41_moissanite_drill', ['MMM', 'CNC', ' I '], {'M': 'moissanite', 'C': 'corundum', 'N': 'minecraft:netherite_ingot',
                                                                 'I': 'minecraft:iron_block'}, 'moissanite_drill', 1)
    for gem in GEMS:
        g['shaped'](f'r41_{gem}_crystal', ['GG', 'GG'], {'G': gem}, gem + '_crystal', 1)


def _tags(g):
    w, D, RES = g['w'], g['D'], g['RES']

    def ext(p, ids):
        t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
        for i in ids:
            if i not in t['values']: t['values'].append(i)
        w(p, t)
    # Iron-Nickel and Core Magnetite Ore: wrong for every vanilla pick, right for the Moissanite Drill
    for tier in ('wooden', 'stone', 'iron', 'golden', 'diamond', 'netherite'):
        ext(os.path.join(RES, 'data', 'minecraft', 'tags', 'block', f'incorrect_for_{tier}_tool.json'), HARD)
    ext(os.path.join(D, 'tags', 'block', 'incorrect_for_moissanite_tool.json'), [])
    ext(os.path.join(RES, 'data', 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'), [f'{NS}:{b["id"]}' for b in BLOCKS])
    ext(os.path.join(D, 'tags', 'item', 'magma_proof.json'), [])
    for gem in GEMS:
        ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'gems', gem + '.json'), [f'{NS}:{gem}'])
        ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'gems.json'), [f'{NS}:{gem}'])
    ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'ores.json'), [f'{NS}:{b["id"]}' for b in BLOCKS if b['kind'] == 'ore'])
    ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'raw_materials', 'iron.json'), [f'{NS}:rich_iron_chunk'])
    ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'raw_materials', 'copper.json'), [f'{NS}:rich_copper_chunk'])
    ext(os.path.join(RES, 'data', 'c', 'tags', 'item', 'raw_materials', 'gold.json'), [f'{NS}:rich_gold_chunk'])
    w(os.path.join(D, 'damage_type', 'basaltic_magma.json'), {'message_id': 'theoreticalsciences.basaltic_magma', 'exhaustion': 0.1,
                                                              'scaling': 'when_caused_by_living_non_player', 'effects': 'burning'})


def apply(g):
    g['LANG'].update(LANG)
    _recipes(g)


def apply_late(g):
    geodes, ores = _worldgen(g)
    _patch_biomes(g, geodes, ores)
    _loot_gems(g)
    _tags(g)


def finish(g):
    te = g['text_entry']
    te('geography', 'r41_riches', 'Deep Riches', 'moissanite', [
        'The Interior is richer than the Overworld. Its ores run in bigger veins: $(item)Rich Iron$() and $(item)Rich Copper$() in the crust, '
        '$(item)Rich Gold$() in the basalt of the Magma Chambers. Their chunks smelt to two ingots; assayed in the $(item)Spectrograph$() they give '
        'three each, and what rides with them in nature - magnetite with iron, gold with copper, uraninite with reef gold.',
        'Its pockets of crystal are far larger than any geode: a shell of pale rock round masses of gem. $(item)Corundum$() and $(item)Garnet$() '
        'grow in the crust; $(item)Peridot$() in the mantle; and, rarer and deeper, $(item)Moissanite$(). Break a Crystal Mass for its gems; it '
        'takes silk touch to keep the mass whole.',
        'Iron-Nickel and Core Magnetite Ore are too hard for any ordinary pick: they drop nothing to netherite. The $(item)Moissanite Drill$() - '
        'silicon carbide on a netherite shank - cuts them. Corundum is the lens material of the instruments to come.',
        '$(thing)Basaltic Magma$() is far hotter than lava, and Fire Resistance is no help against it: it melts what it touches. Only armour made '
        'for it will do, and that comes later. $(thing)Deep Brine$() is scalding: it sets you alight like lava.'],
        adv='mantle_geodynamics', sort=7, spot=['corundum', 'garnet', 'peridot', 'moissanite'])
