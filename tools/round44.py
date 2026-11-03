"""
Round 44 (1.3.7): the Planetary Interior comes alive.

* Four creatures (Java: interior/InteriorMobs): the Magma Crawler, the Plume Elemental, the Fission Husk and the
  Geodynamo Wraith - each a piece of the planet's physics.
* Three suits of armour (interior/InteriorGear), the counters to the Interior's hazards: the Refractory Suit (magma and
  fire), the Pressure Suit (Lithostatic Pressure) and the Lead-Lined Suit (radiation).
* Two structures: the Deep Borehole (Crustal Caverns) and the Oklo Reactor Ruin (Natural Reactor), built of the mod's own
  blocks with Containment Caches of the mod's own loot, like the 1.0 ruins.
* Tool rules: the Omnipotence Casing only yields to tools in #theoreticalsciences:omnipotent_tools; the Dodecaract
  Excavator breaks anything breakable at once (item/ToolRules).
"""
import json
import math
import os

import round39 as _r39

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))

BLOCKS, FLUIDS, RESEARCH = [], [], []
# 1.3.8: radiation plating for any armour, as NuclearCraft does it (interior/RadiationPlating)
ITEMS = [
    dict(id='light_radiation_plating', name='Light Radiation Plating', art='plate', c1='#4a5060', c2='#a0a8b8',
         lore='Lead plate shaped to fit armour. Put it with any armour piece in a crafting grid: that piece then stops 10% of the radiation dose.'),
    dict(id='medium_radiation_plating', name='Medium Radiation Plating', art='plate', c1='#2c2e32', c2='#80868e',
         lore='Lead backed with graphite, which slows the neutrons lead lets through. A plated piece stops 18% of the dose.'),
    dict(id='heavy_radiation_plating', name='Heavy Radiation Plating', art='plate', c1='#30363c', c2='#c8d4e0',
         lore='Lead, graphite and boron carbide, which swallows the slowed neutrons. A plated piece stops 25% of the dose - four of them, all but 5%.'),
]
PLATING_RECIPES = [
    ('light_radiation_plating', ['LLL', 'LIL', 'LLL'], {'L': 'lead_shielding_plate', 'I': 'minecraft:iron_ingot'}),
    ('medium_radiation_plating', [' G ', 'GPG', ' G '], {'G': 'graphite_moderator', 'P': 'light_radiation_plating'}),
    ('heavy_radiation_plating', [' B ', 'BPB', ' B '], {'B': 'boron_control_rod', 'P': 'medium_radiation_plating'}),
]
MACHINE_TEXT = {}

# =================================================================================== CREATURES
CREATURES = {
    'magma_crawler': ('Magma Crawler', [('basalt_column', 1, 3, 1.0), ('rich_gold_chunk', 1, 1, 0.08)]),
    'plume_elemental': ('Plume Elemental', [('ringwoodite', 0, 2, 1.0), ('peridotite', 1, 2, 1.0)]),
    'fission_husk': ('Fission Husk', [('fission_fragment_mix', 0, 1, 1.0), ('raw_pitchblende', 1, 1, 0.2)]),
    'geodynamo_wraith': ('Geodynamo Wraith', [('magnetite', 1, 3, 1.0), ('ferrite_magnet', 1, 1, 0.15)]),
}
SPAWNS = {   # biome -> [(entity, weight, min, max)]
    'magma_chambers': [('magma_crawler', 40, 1, 2)],
    'mantle_convection_zone': [('plume_elemental', 30, 1, 1)],
    'natural_reactor': [('fission_husk', 50, 1, 3)],
    'geodynamo_core': [('geodynamo_wraith', 25, 1, 1)],
}

# =================================================================================== ARMOUR
SUITS = {
    # suit id: (name, accent item M, body item B, tint dark, tint light, lore)
    'refractory_suit': ('Refractory', 'moissanite', 'corundum', '#3050c0', '#a0c0ff',
                        'Corundum and moissanite over basalt fibre. A full suit is proof against Basaltic Magma, Deep Brine and every fire.'),
    'pressure_suit': ('Pressure', 'ringwoodite', 'bridgmanite', '#605040', '#c0a080',
                      'A shell of high-pressure minerals. Each piece takes a level off Lithostatic Pressure; a full suit cancels it at any depth.'),
}
PIECES = [('helmet', 'Helmet', ['BMB', 'B B']), ('chestplate', 'Chestplate', ['B B', 'BMB', 'BBB']),
          ('leggings', 'Leggings', ['BMB', 'B B', 'B B']), ('boots', 'Boots', ['M M', 'B B'])]

# =================================================================================== STRUCTURES
M = NS + ':'


def deep_borehole():
    """A drill hole abandoned two dozen blocks deep: a cased shaft through crustal rock, a broken drill floor on top with
    the stumps of its derrick, cores and pipe left lying about, and a cache at the bottom of the hole."""
    t = _r39.T(11, 30, 11, 44)
    c = 5
    for y in range(0, 30):
        for x in range(11):
            for z in range(11):
                d = math.hypot(x - c, z - c)
                if d <= 3.6:
                    t.set(x, y, z, M + 'crustal_rock')
    for y in range(1, 27):
        for x in range(c - 1, c + 2):
            for z in range(c - 1, c + 2):
                t.set(x, y, z, 'air')
        if y % 3 == 0:                                       # the casing, every few metres
            for x in range(c - 2, c + 3):
                for z in range(c - 2, c + 3):
                    if max(abs(x - c), abs(z - c)) == 2:
                        t.set(x, y, z, M + ('lithostatic_casing' if (x + z) % 2 else 'drill_string'))
    t.fill(0, 26, 0, 10, 26, 10, M + 'lithostatic_casing')     # the drill floor
    for x in range(c - 1, c + 2):
        for z in range(c - 1, c + 2):
            t.set(x, 26, z, 'air')
    t.fill(0, 27, 0, 10, 29, 10, 'air')
    for x, z, h in ((1, 1, 3), (9, 1, 1), (1, 9, 2), (9, 9, 0)):  # derrick stumps
        for y in range(27, 27 + h):
            t.set(x, y, z, M + 'derrick_frame')
    for x in range(2, 8):                                    # pipe laid down on the floor
        t.set(x, 27, 8, M + 'drill_string')
    t.fill(c - 1, 1, c - 1, c + 1, 1, c + 1, M + 'peridotite')   # the hole bottomed out in mantle rock
    t.cache(c, 2, c, 'deep_borehole')
    t.cache(2, 27, 2, 'deep_borehole_surface')
    t.set(c, 27, c + 3, M + 'radium_lamp')
    return t


def oklo_reactor_ruin():
    """A lead-walled station dug round a natural reactor: the seam itself in the middle - oklo rock, uraninite and fission
    fragments - framed by graphite and an old vessel ring, radium lamps, hazard panels, and two caches."""
    t = _r39.T(15, 8, 15, 45)
    c = 7
    t.fill(0, 0, 0, 14, 7, 14, M + 'lead_bricks')
    t.fill(1, 1, 1, 13, 6, 13, 'air')
    for x in range(1, 14):
        for z in range(1, 14):
            if (x in (1, 13) or z in (1, 13)) and (x + z) % 4 == 0:
                for y in (2, 3):
                    t.set(x, y, z, M + 'lead_hazard_panel')
    for x in range(15):
        for z in range(15):
            d = math.hypot(x - c, z - c)
            if d <= 4.5:
                t.set(x, 0, z, M + ('fission_fragment_ore' if d < 1.5 else 'oklo_uraninite_ore' if d < 3 and (x * 7 + z) % 3 == 0 else 'oklo_rock'))
            if 4.5 < d <= 5.4:
                t.set(x, 1, z, M + 'reactor_vessel_casing')
    for dx, dz in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        t.set(c + dx, 1, c + dz, M + 'graphite_moderator_block')
        t.set(c + dx, 2, c + dz, M + 'graphite_moderator_block')
    for x, z in ((2, 2), (12, 2), (2, 12), (12, 12)):
        t.set(x, 6, z, M + 'radium_lamp')
    t.fill(6, 1, 0, 8, 3, 0, 'air')                             # the doorway, partly fallen in
    t.set(8, 3, 0, M + 'lead_bricks')
    t.cache(3, 1, 11, 'oklo_reactor_ruin')
    t.cache(11, 1, 3, 'oklo_reactor_ruin')
    return t


SITES = {
    # id: (builder, biomes, spacing, separation, salt, start heights)
    'deep_borehole': (deep_borehole, [f'{NS}:crustal_caverns'], 24, 8, 44_101, (72, 96)),
    'oklo_reactor_ruin': (oklo_reactor_ruin, [f'{NS}:natural_reactor'], 20, 6, 44_102, (60, 100)),
}
LOOT = {
    'deep_borehole': [('ringwoodite', 1, 3, 8), ('peridotite', 2, 8, 10), ('sediment_core', 2, 6, 10), ('moissanite', 1, 2, 4),
                      ('seismic_recorder_drum', 1, 3, 6), ('drill_string', 2, 6, 8), ('bridgmanite', 1, 2, 3), ('core_drill', 1, 1, 1)],
    'deep_borehole_surface': [('derrick_frame', 2, 8, 10), ('drill_string', 2, 6, 10), ('sediment_core', 1, 4, 8), ('survey_map', 1, 2, 4),
                              ('corundum', 1, 3, 6), ('lithostatic_casing', 1, 4, 5)],
    'oklo_reactor_ruin': [('yellowcake', 2, 6, 10), ('fission_fragment_mix', 1, 4, 10), ('uranium_235_pellet', 1, 2, 3), ('boron_control_rod', 1, 2, 4),
                          ('lead_shielding_plate', 2, 6, 8), ('graphite_moderator', 2, 6, 8), ('geiger_tube', 1, 1, 4), ('prussian_blue_tablet', 1, 2, 5),
                          ('radium_salt', 1, 1, 2)],
}

LANG = {
    'message.theoreticalsciences.plume_elemental': 'The plume rises under you and throws you up!',
    'message.theoreticalsciences.geodynamo_wraith_pull': 'The Geodynamo Wraith tears %s out of your hand!',
    'block.theoreticalsciences.omnipotence_casing.lore': 'Material that refuses to be broken by anything made in the ordinary world: no vanilla tool '
                                                         'touches it - only the mod\'s own (the tag theoreticalsciences:omnipotent_tools).',
}


def _q(x):
    return x if ':' in x else f'{NS}:{x}'


def _tag(g, path, ids):
    t = json.load(open(path)) if os.path.exists(path) else {'replace': False, 'values': []}
    for i in ids:
        if i not in t['values']:
            t['values'].append(i)
    g['w'](path, t)


def _armour(g):
    A, LANG_, shaped = g['A'], g['LANG'], g['shaped']
    for sid, (name, mat, body, c1, c2, lore) in SUITS.items():
        LANG_[f'tooltip.{NS}.{sid}'] = lore
        for p, pname, pat in PIECES:
            iid = f'{sid}_{p}'
            LANG_[f'item.{NS}.{iid}'] = f'{name} {pname}'
            g['w'](os.path.join(A, 'models', 'item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{iid}'}})
            shaped('r44_' + iid, pat, {'M': mat, 'B': body}, iid, 1)
    for iid, pat, key in PLATING_RECIPES:
        shaped('r44_' + iid, pat, key, iid, 1)
    g['w'](os.path.join(g['REC'], 'crafting', 'r44_armor_radiation_plating.json'),
           {'type': f'{NS}:armor_radiation_plating', 'category': 'equipment'})
    for t, n in ((1, 'Light'), (2, 'Medium'), (3, 'Heavy')):
        LANG_[f'tooltip.{NS}.radiation_plating.{t}'] = n + ' radiation plating: stops %s%% of the dose'

def _structures(g):
    D, w = g['D'], g['w']
    for sid, (fn, biomes, spacing, sep, salt, height) in SITES.items():
        fn().save(os.path.join(D, 'structure', sid + '.nbt'))
        w(os.path.join(D, 'tags', 'worldgen', 'biome', 'has_structure', sid + '.json'), {'replace': False, 'values': biomes})
        w(os.path.join(D, 'worldgen', 'template_pool', sid, 'start.json'), {
            'fallback': 'minecraft:empty',
            'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{sid}',
                                                   'projection': 'rigid', 'processors': 'minecraft:empty'}}]})
        w(os.path.join(D, 'worldgen', 'structure', sid + '.json'), {
            'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{sid}', 'step': 'underground_structures', 'spawn_overrides': {},
            'terrain_adaptation': 'beard_box', 'start_pool': f'{NS}:{sid}/start', 'size': 1, 'max_distance_from_center': 80,
            'use_expansion_hack': False,
            'start_height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': height[0]}, 'max_inclusive': {'absolute': height[1]}}})
        w(os.path.join(D, 'worldgen', 'structure_set', sid + '.json'), {
            'structures': [{'structure': f'{NS}:{sid}', 'weight': 1}],
            'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': sep, 'salt': salt}})
    for name, entries in LOOT.items():
        w(os.path.join(D, 'loot_table', 'chests', name + '.json'), {
            'type': 'minecraft:chest',
            'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 3, 'max': 6},
                       'entries': [{'type': 'minecraft:item', 'name': _q(i), 'weight': wt,
                                    'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]}
                                   for i, lo, hi, wt in entries]}]})


def _spawns(g):
    WG, w = g['WG'], g['w']
    for biome, entries in SPAWNS.items():
        p = os.path.join(WG, 'biome', biome + '.json')
        d = json.load(open(p))
        mon = d.setdefault('spawners', {}).setdefault('monster', [])
        for eid, wt, lo, hi in entries:
            if not any(s['type'] == f'{NS}:{eid}' for s in mon):
                mon.append({'type': f'{NS}:{eid}', 'weight': wt, 'minCount': lo, 'maxCount': hi})
        w(p, d)


def _tags(g):
    D, RES = g['D'], g['RES']
    _tag(g, os.path.join(D, 'tags', 'block', 'needs_omnipotent_tool.json'), [f'{NS}:omnipotence_casing'])
    _tag(g, os.path.join(D, 'tags', 'item', 'omnipotent_tools.json'), [f'{NS}:dodecaract_excavator', f'{NS}:moissanite_drill'])
    _tag(g, os.path.join(D, 'tags', 'item', 'magma_proof.json'), [f'{NS}:refractory_suit_{p}' for p, _, _ in PIECES])
    _tag(g, os.path.join(D, 'tags', 'entity_type', 'magma_immune.json'), [f'{NS}:magma_crawler'])
    for p, _, _ in PIECES:
        slot = {'helmet': 'head', 'chestplate': 'chest', 'leggings': 'legs', 'boots': 'foot'}[p]
        _tag(g, os.path.join(RES, 'data', 'minecraft', 'tags', 'item', f'{slot}_armor.json'), [f'{NS}:{s}_{p}' for s in SUITS])


# 1.3.8: Fission Fragment Mix had no use. Fission fragments are about a third lanthanides (rare earths), with the
# last unburnt uranium mixed in. A spectrograph sorts out the rare earths; fluorine strips the uranium out as UF6.
FRAGMENT_RECIPES = [
    ('fragment_rare_earths', 'spectroscopy', [('fission_fragment_mix', 2), ('spectral_plate', 1)], [], [('rare_earth_oxide', 3)], [],
     20000, 200, 'isotope_separation'),
    ('fragment_uranium_recovery', 'isotope_enrichment', [('fission_fragment_mix', 8), ('fluorspar', 2)], [], [('rare_earth_oxide', 1)],
     [('uranium_hexafluoride', 250)], 1024, 300, 'isotope_separation'),
]


def _fragments(g):
    import round42
    for name, typ, ins, fins, outs, fouts, fe, t, res in FRAGMENT_RECIPES:
        g['mr']('r44_' + name, typ, ins, fins, outs, fouts, energy=fe, duration=t, research=res)
        round42._unscaled(g, typ, 'r44_' + name)


def apply(g):
    g['LANG'].update({k: v for k, v in LANG.items() if not k.endswith('.lore')})
    _armour(g)
    _structures(g)
    _fragments(g)


def apply_late(g):
    _spawns(g)
    _tags(g)


def finish(g):
    g['LANG'].update(LANG)
    te = g['text_entry']
    te('geography', 'r44_creatures', 'Creatures of the Interior', 'magma_crawler_spawn_egg', [
        'The $(thing)Magma Crawler$() lives in the Magma Chambers\' sea. Basaltic Magma is its water: it swims through it as fast as you walk, '
        'and its bite sets you alight. On dry rock it is slower.',
        'The $(thing)Plume Elemental$() is a blob of mantle rock that convects. Deep down it heats and rises; high up it cools and sinks. Stand '
        'over or beside it and the upwelling throws you into the air.',
        'The $(thing)Fission Husk$() glows with what decays inside it - stand near it and your dose climbs. Kill it and it splits: two lighter '
        'husks fly apart in a burst of neutrons, and each of those splits once more.',
        'The $(thing)Geodynamo Wraith$() is a loop of the core\'s own current. It circles its prey; ferromagnetic armour near it is Magnetized '
        'and pulled in, and it can tear an iron tool out of your hand. Wear no iron near it.'],
        adv='mantle_geodynamics', sort=8, spot=['magma_crawler_spawn_egg', 'plume_elemental_spawn_egg', 'fission_husk_spawn_egg',
                                              'geodynamo_wraith_spawn_egg'])
    te('geography', 'r44_suits', 'Suits for the Deep', 'refractory_suit_chestplate', [
        'Every hazard below has its suit, crafted like ordinary armour.',
        '$(item)Refractory Suit$(): corundum and moissanite. A full suit is proof against $(fluid)Basaltic Magma$() - you can wade in it and mine '
        'the magnetite under it - against $(fluid)Deep Brine$(), lava, and every fire.',
        '$(item)Pressure Suit$(): bridgmanite and ringwoodite, minerals made under the mantle\'s weight. Each piece takes a level off '
        '$(thing)Lithostatic Pressure$(); a full suit cancels it at any depth.',
        '$(thing)Radiation plating$() goes on any armour, of any mod. Put an armour piece and a plate together in a crafting grid: '
        '$(item)Light$() (lead) stops 10% of the radiation dose per piece, $(item)Medium$() (lead and graphite) 18%, $(item)Heavy$() (lead, graphite '
        'and boron carbide) 25% - four heavy-plated pieces stop all but 5%. A better plate replaces a worse one. Wear it in the Natural Reactor.'],
        adv='mantle_geodynamics', sort=9, spot=['refractory_suit_chestplate', 'pressure_suit_chestplate', 'heavy_radiation_plating'])
    te('world', 'interior_sites', 'Sites in the Deep', 'drill_string', [
        'Others have been down here before. In the Crustal Caverns stand $(thing)Deep Boreholes$(): drill holes cased in steel and abandoned two '
        'dozen blocks down, a broken drill floor at the top and a cache at the bottom of the hole.',
        'In the Natural Reactor, lead-walled $(thing)Oklo Reactor Ruins$() are dug round the reactor seam itself - fission fragments and '
        'uraninite in the floor. Bring a Geiger Counter and radiation-plated armour; their caches hold fuel, graphite and control rods.'],
        adv='plate_tectonics', sort=3, spot=['drill_string', 'lead_bricks'])
