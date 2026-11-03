"""
Round 40 (1.3.0, "The Planetary Interior"): a dimension inside the planet, layered by depth like the real one.

* Crustal Caverns (top)      - crustal rock, deep brine.
* Natural Reactor (patches)  - in the crust: Oklo rock, uraninite seams, a radiation background (based on Oklo, Gabon).
* Magma Chambers             - columnar basalt over a sea of basaltic magma.
* Mantle Convection Zone     - peridotite with ringwoodite and bridgmanite; plumes push you up.
* Geodynamo Core (bottom)    - iron-nickel with magnetite; magnetic storms.

Entered with the Core Drill, the first tool that can break bedrock: drill through the Overworld's floor and a Mantle
Breach opens. Research: Plate Tectonics (the Core Drill) and Mantle Geodynamics.
"""
import json
import os

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
DIM = 'planetary_interior'
BIOMES = ['crustal_caverns', 'natural_reactor', 'magma_chambers', 'mantle_convection_zone', 'geodynamo_core']


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


def _B(i, n, art, c1, c2, kind='simple', **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, kind=kind, light=kw.pop('light', 0), hardness=kw.pop('hardness', 3.0), tool=kw.pop('tool', 'stone'))
    d.update(kw)
    return d


ITEMS = [
    _I('core_drill', 'Core Drill', 'analyzer', '#504030', '#ffb040',
       'A diamond-set coring bit on a hydraulic hammer. Hold it against bedrock and it cuts through - the first tool that can.',
       special='coredrill', stack=1),
    _I('ringwoodite', 'Ringwoodite', 'gem', '#203880', '#60a0ff',
       'A high-pressure form of olivine from 520 km down. Its crystals hold water: there may be more of it in the mantle than in every ocean.'),
    _I('bridgmanite', 'Bridgmanite', 'crystal', '#605040', '#c0a080',
       'The most abundant mineral on Earth - and almost never seen, because it only exists under the pressure of the lower mantle.'),
    _I('fission_fragment_mix', 'Fission Fragment Mix', 'dots', '#304020', '#c0ff40',
       'Caesium, strontium, iodine and xenon: what a uranium nucleus becomes when it splits. Two billion years ago, Oklo made this on its own.'),
]
BLOCKS = [
    _B('crustal_rock', 'Crustal Rock', 'bulkstone', '#3a3028', '#6a5a48'),
    _B('basalt_column', 'Columnar Basalt', 'bulkstone', '#18181c', '#3a3a44', hardness=3.5),
    _B('peridotite', 'Peridotite', 'bulkstone', '#283820', '#5a7a40', hardness=4.0),
    _B('core_iron', 'Iron-Nickel', 'metalblock', '#40403c', '#a0a090', hardness=6.0, tool='iron'),
    _B('oklo_rock', 'Oklo Rock', 'bulkstone', '#2a2a18', '#7a8a30', light=2, hardness=4.0),
    dict(id='ringwoodite_ore', name='Ringwoodite Ore', art='ore', base='peridotite', c1='#102060', c2='#60a0ff', kind='ore', drops='ringwoodite',
         light=0, hardness=4.5, tool='iron'),
    dict(id='bridgmanite_ore', name='Bridgmanite Ore', art='ore', base='peridotite', c1='#403020', c2='#d0b090', kind='ore', drops='bridgmanite',
         light=0, hardness=5.0, tool='iron'),
    dict(id='core_magnetite_ore', name='Core Magnetite Ore', art='ore', base='core_iron', c1='#101014', c2='#707084', kind='ore', drops='magnetite',
         light=0, hardness=6.0, tool='iron'),
    dict(id='oklo_uraninite_ore', name='Oklo Uraninite Ore', art='ore', base='oklo_rock', c1='#202010', c2='#c8ff40', kind='ore',
         drops='raw_pitchblende', light=3, hardness=4.5, tool='iron'),
    dict(id='fission_fragment_ore', name='Fission Fragment Seam', art='ore', base='oklo_rock', c1='#203010', c2='#e0ff80', kind='ore',
         drops='fission_fragment_mix', light=5, hardness=4.5, tool='iron'),
    _B('inner_core_boundary', 'Inner Core Boundary', 'metalblock', '#2a2418', '#d0a040', kind='unbreakable', light=3, hardness=-1, tool='none'),
    _B('mantle_breach', 'Mantle Breach', 'stars', '#401000', '#ff8020', kind='mantle_breach', light=12, hardness=-1.0, tool='none'),
]
FLUIDS = [
    dict(id='basaltic_magma', name='Basaltic Magma', c1='#401000', c2='#ff7020', gas=False, light=15, temp=1450, density=2700, viscosity=6000,
         art='plasma'),
    dict(id='deep_brine', name='Deep Brine', c1='#103040', c2='#60c0d0', gas=False, light=2, temp=330, density=1200, viscosity=1200,
         art='condensate'),
]

# =================================================================================== RESEARCH
RESEARCH = [
    ('plate_tectonics', 'Plate Tectonics',
     'The crust is broken into plates that ride on the hot mantle beneath. Where they part, rock wells up; where they meet, one dives under the '
     'other. Unlocks the Core Drill - the first tool that cuts bedrock - and the way into the Planetary Interior.',
     'core_drill', 5, 15, 60, ['seismology', 'geomorphology'], [('seismic_recorder_drum', 4), ('sediment_core', 4)]),
    ('mantle_geodynamics', 'Mantle Geodynamics',
     'The mantle is solid rock, and yet it flows: a centimetre a year, heated from below, cooled from above, turning over like a pot on a stove. '
     'Its minerals - ringwoodite, bridgmanite - only exist under its pressure.',
     'ringwoodite', 6, 15, 80, ['plate_tectonics'], [('ringwoodite', 4), ('bridgmanite', 2)]),
]
PUZZLES = {
    'plate_tectonics': dict(
        goal='Rock of density ρ lies above you to a depth h, pulled down by gravity g. Write the pressure P it presses on you with.',
        name='Lithostatic pressure', lhs='P',
        form='ρ * g * h',
        expr='R * g * h / 1000000000',
        ask='Find P in gigapascals 100 km down, with rock of density R = 3300 kg/m³ and g = 9.81 m/s².',
        vars={'P': 'pressure (GPa)', 'ρ': 'density', 'g': 'gravity (m/s²)', 'h': 'depth (m)', 'R': 'rock density (kg/m³)'},
        given={'R': 3300, 'g': 9.81, 'h': 100000}, unit='GPa',
        method='Each layer carries the weight of everything above it: a column of area A and height h weighs ρ·A·h·g.',
        distractors=['m', 'v', '2']),
    'mantle_geodynamics': dict(
        goal='A layer of thickness L is heated from below by a temperature difference ΔT. Whether it turns over depends on one number: buoyancy (ρ g α '
             'ΔT) against heat diffusion κ and viscosity μ. Write that number, N.',
        name='Rayleigh number', lhs='N',
        form='ρ * g * α * T * L ^ 3 / ( κ * μ )',
        expr='R * g * a * T * H ^ 3 / ( k * m )',
        ask='Find N for the whole mantle: R = 4000 kg/m³, a = 3e-5 /K, T = 2500 K, H = 2.9e6 m, k = 1e-6 m²/s, m = 1e22 Pa·s.',
        vars={'N': 'Rayleigh number', 'ρ': 'density', 'g': 'gravity (m/s²)', 'α': 'thermal expansion', 'T': 'temperature difference (K)',
              'L': 'layer thickness (m)', 'H': 'mantle thickness (m)', 'κ': 'thermal diffusivity', 'μ': 'viscosity', 'R': 'density (kg/m³)', 'a': 'expansion (1/K)',
              'k': 'diffusivity (m²/s)', 'm': 'viscosity (Pa·s)'},
        given={'R': 4000, 'g': 9.81, 'a': 3e-5, 'T': 2500, 'H': 2.9e6, 'k': 1e-6, 'm': 1e22}, unit='',
        method='Compare how fast a warm blob rises (buoyancy over viscosity) with how fast it loses its heat (diffusion). Above about 1000 the layer '
               'convects.',
        distractors=['ν', '2', 'π']),
}
CRAFTING = [
    ('core_drill', [' D ', 'IPI', 'ISI'], {'D': 'minecraft:diamond_block', 'I': 'minecraft:iron_block', 'P': 'minecraft:piston',
                                           'S': 'seismic_recorder_drum'}, 'core_drill', 1),
]
SHAPELESS = []
RESEARCH_RECIPES = {'r40_core_drill': 'plate_tectonics'}

ITEM_SOURCES = {f'{NS}:fission_fragment_mix': ('cs137', 0.15)}
BLOCK_SOURCES = {f'{NS}:oklo_rock': ('u238_chain', 0.002), f'{NS}:oklo_uraninite_ore': ('u238_chain', 0.006),
                 f'{NS}:fission_fragment_ore': ('cs137', 0.02)}

LANG = {
    'message.theoreticalsciences.core_drill_locked': 'You need to research Plate Tectonics to cut bedrock.',
    'message.theoreticalsciences.core_drill_through': 'The bit breaks through - heat roars up from below.',
    'message.theoreticalsciences.interior_arrival': 'The Planetary Interior. The deeper you go, the heavier the rock above you presses.',
    'message.theoreticalsciences.geomagnetic_storm': 'A geomagnetic storm sweeps through the core - your compass spins.',
    'message.theoreticalsciences.mantle_plume': 'A mantle plume rises beneath you!',
    'dimname.theoreticalsciences.planetary_interior': 'the Planetary Interior',
    'biome.theoreticalsciences.crustal_caverns': 'Crustal Caverns',
    'biome.theoreticalsciences.natural_reactor': 'Natural Reactor',
    'biome.theoreticalsciences.magma_chambers': 'Magma Chambers',
    'biome.theoreticalsciences.mantle_convection_zone': 'Mantle Convection Zone',
    'biome.theoreticalsciences.geodynamo_core': 'Geodynamo Core',
    'advancements.theoreticalsciences.interior_arrival.title': 'Journey to the Centre',
    'advancements.theoreticalsciences.interior_arrival.description': 'Drill through the floor of the world into the Planetary Interior.',
}


# =================================================================================== WRITERS
def _write_research(g):
    w, D, q = g['w'], g['D'], g['q']
    for rid, t, desc, icon, x, y, ins, req, cost in RESEARCH:
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


def _write_puzzles():
    p = os.path.join(HERE, 'puzzles2', 'group_r40.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _ore(g, name, host, block, size, count, lo, hi):
    g['ore_feature'](name, {'predicate_type': 'minecraft:block_match', 'block': f'{NS}:{host}'}, block, size, count, lo, hi)


def _rock_rule(biome, block):
    return {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [f'{NS}:{biome}']},
            'then_run': {'type': 'minecraft:block', 'result_state': {'Name': f'{NS}:{block}'}}}


def _dimension(g):
    w, D, WG, q = g['w'], g['D'], g['WG'], g['q']
    import zipfile
    import mcpaths
    with zipfile.ZipFile(mcpaths.CLIENT_JAR) as z:
        ns = json.loads(z.read('data/minecraft/worldgen/noise_settings/nether.json'))
        dt = json.loads(z.read('data/minecraft/dimension_type/the_nether.json'))
    ns['default_block'] = {'Name': q('crustal_rock')}
    ns['default_fluid'] = {'Name': q('basaltic_magma'), 'Properties': {'level': '0'}}
    # depth sets the biome: +1 at the floor of the world, -1 at its roof (vanilla's cave biomes read the same parameter)
    ns['noise_router']['depth'] = {'type': 'minecraft:y_clamped_gradient', 'from_y': 0, 'to_y': 128, 'from_value': 1.0, 'to_value': -1.0}
    roof = ns['surface_rule']['sequence'][1]
    floor = ns['surface_rule']['sequence'][0]
    # the floor is not bedrock but the Inner Core Boundary: solid iron under 330 GPa, which nothing cuts (yet)
    floor = json.loads(json.dumps(floor))
    floor['then_run'] = {'type': 'minecraft:block', 'result_state': {'Name': q('inner_core_boundary')}}
    ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [
        floor, roof,
        _rock_rule('natural_reactor', 'oklo_rock'), _rock_rule('magma_chambers', 'basalt_column'),
        _rock_rule('mantle_convection_zone', 'peridotite'), _rock_rule('geodynamo_core', 'core_iron')]}
    w(os.path.join(WG, 'noise_settings', DIM + '.json'), ns)
    dt.update({'ultrawarm': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'coordinate_scale': 1.0,
               'effects': 'minecraft:the_nether', 'infiniburn': '#minecraft:infiniburn_overworld', 'monster_spawn_light_level': 0,
               'monster_spawn_block_light_limit': 0, 'ambient_light': 0.12})
    w(os.path.join(D, 'dimension_type', DIM + '.json'), dt)

    def P(depth, temp=(-1.0, 1.0)):
        return {'temperature': list(temp), 'humidity': [-1.0, 1.0], 'continentalness': [-1.0, 1.0], 'erosion': [-1.0, 1.0],
                'weirdness': [-1.0, 1.0], 'depth': list(depth), 'offset': 0.0}
    params = [
        {'biome': q('crustal_caverns'), 'parameters': P((-1.0, -0.15), (-1.0, 0.4))},
        {'biome': q('natural_reactor'), 'parameters': P((-1.0, -0.15), (0.4, 1.0))},
        {'biome': q('magma_chambers'), 'parameters': P((-0.15, 0.25))},
        {'biome': q('mantle_convection_zone'), 'parameters': P((0.25, 0.65))},
        {'biome': q('geodynamo_core'), 'parameters': P((0.65, 1.0))},
    ]
    w(os.path.join(D, 'dimension', DIM + '.json'), {'type': q(DIM), 'generator': {
        'type': 'minecraft:noise', 'settings': q(DIM), 'biome_source': {'type': 'minecraft:multi_noise', 'biomes': params}}})
    # ores
    _ore(g, 'ore_ringwoodite', 'peridotite', 'ringwoodite_ore', 9, 10, 4, 84)
    _ore(g, 'ore_bridgmanite', 'peridotite', 'bridgmanite_ore', 7, 7, 4, 70)
    _ore(g, 'ore_core_magnetite', 'core_iron', 'core_magnetite_ore', 10, 10, 1, 48)
    _ore(g, 'ore_oklo_uraninite', 'oklo_rock', 'oklo_uraninite_ore', 9, 10, 36, 126)
    _ore(g, 'ore_fission_fragments', 'oklo_rock', 'fission_fragment_ore', 6, 6, 36, 126)
    # deep brine: pools of it in the crust's caverns
    w(os.path.join(WG, 'configured_feature', 'deep_brine_pool.json'), {'type': 'minecraft:lake', 'config': {
        'fluid': {'type': 'minecraft:simple_state_provider', 'state': {'Name': q('deep_brine'), 'Properties': {'level': '0'}}},
        'barrier': {'type': 'minecraft:simple_state_provider', 'state': {'Name': q('crustal_rock')}}}})
    w(os.path.join(WG, 'placed_feature', 'deep_brine_pool.json'), {'feature': q('deep_brine_pool'), 'placement': [
        {'type': 'minecraft:count', 'count': 4}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': 75}, 'max_inclusive': {'absolute': 120}}},
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 32,
         'target_condition': {'type': 'minecraft:all_of', 'predicates': [{'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}},
                                                                          {'type': 'minecraft:inside_world_bounds', 'offset': [0, -5, 0]}]},
         'allowed_search_condition': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:air'}},
        {'type': 'minecraft:biome'}]})
    ores = {'crustal_caverns': [], 'natural_reactor': ['ore_oklo_uraninite', 'ore_fission_fragments'], 'magma_chambers': [],
            'mantle_convection_zone': ['ore_ringwoodite', 'ore_bridgmanite'], 'geodynamo_core': ['ore_core_magnetite']}
    look = {  # sky/fog, particle
        'crustal_caverns': (0x3a2a20, 0x2a2018, 'minecraft:white_ash', 0.004),
        'natural_reactor': (0x2a3010, 0x30401a, 'minecraft:electric_spark', 0.003),
        'magma_chambers': (0x401000, 0x5a1a08, 'minecraft:lava', 0.002),
        'mantle_convection_zone': (0x283820, 0x30401c, 'minecraft:ash', 0.01),
        'geodynamo_core': (0x303040, 0x404058, 'minecraft:electric_spark', 0.006),
    }
    for b in BIOMES:
        feats = [[] for _ in range(11)]
        feats[6] = [q(x) for x in ores[b]]
        if b == 'crustal_caverns': feats[1] = [q('deep_brine_pool')]
        sky, fog, part, prob = look[b]
        w(os.path.join(WG, 'biome', b + '.json'), {
            'has_precipitation': False, 'temperature': 2.0, 'downfall': 0.0,
            'effects': {'sky_color': sky, 'fog_color': fog, 'water_color': 0x60c0d0, 'water_fog_color': 0x103040,
                        'ambient_sound': 'minecraft:ambient.basalt_deltas.loop',
                        'particle': {'options': {'type': part}, 'probability': prob}},
            'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': feats})
    w(os.path.join(D, 'advancement', 'interior_arrival.json'), {
        'display': {'icon': {'id': q('core_drill')}, 'title': {'translate': 'advancements.theoreticalsciences.interior_arrival.title'},
                    'description': {'translate': 'advancements.theoreticalsciences.interior_arrival.description'}, 'frame': 'goal',
                    'show_toast': True, 'announce_to_chat': True, 'hidden': False},
        'parent': f'{NS}:research/plate_tectonics',
        'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q(DIM)}}}, 'requirements': [['arrived']]})


def _recipes(g):
    for name, pat, key, res, n in CRAFTING:
        g['shaped']('r40_' + name, pat, key, res, n)
    for name, items, res, n in SHAPELESS:
        g['shapeless']('r40_' + name, items, res, n)


def _radiation(g):
    D, w = g['D'], g['w']
    for kind, extra in (('item', ITEM_SOURCES), ('block', BLOCK_SOURCES)):
        p = os.path.join(D, 'data_maps', kind, 'radiation_source.json')
        t = json.load(open(p)) if os.path.exists(p) else {'values': {}}
        for k, (i, s) in extra.items():
            t['values'][k] = {'isotope': i, 'strength': s}
        w(p, t)
    for name in ('radioactive',):
        p = os.path.join(D, 'tags', 'block', name + '.json')
        t = json.load(open(p))
        for b in BLOCK_SOURCES:
            if b not in t['values']: t['values'].append(b)
        w(p, t)
    p = os.path.join(D, 'tags', 'item', 'strongly_radioactive.json')
    t = json.load(open(p))
    if f'{NS}:fission_fragment_mix' not in t['values']: t['values'].append(f'{NS}:fission_fragment_mix')
    w(p, t)
    p = os.path.join(D, 'tags', 'block', 'magnetic.json')
    t = json.load(open(p))
    for b in (f'{NS}:core_iron', f'{NS}:core_magnetite_ore'):
        if b not in t['values']: t['values'].append(b)
    w(p, t)


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)
    _dimension(g)


def apply_late(g):
    _radiation(g)


def finish(g):
    te = g['text_entry']
    te('world', 'interior_laws', 'Surviving the Interior', 'core_drill', [
        'The $(thing)Planetary Interior$() needs no attunement item - its laws are only the planet\'s own, and they press harder the deeper you go. '
        'You reach it with the $(item)Core Drill$() (research $(thing)Plate Tectonics$()), cutting the Overworld\'s bedrock floor; you leave by '
        'any $(item)Mantle Breach$() - one opens beside you wherever you arrive.',
        '$(li)$(thing)Lithostatic Pressure$(): below y 100, one level for every 25 blocks deeper; you move and mine slower.$(li)$(thing)Tremors$() '
        'in the Crustal Caverns: a few seconds of Seismic Vertigo.$(li)$(thing)Mantle plumes$() in the Mantle Convection Zone throw you upwards.'
        '$(li)$(thing)Geomagnetic storms$() in the Geodynamo Core leave you Magnetized: compasses spin.',
        '$(li)The $(thing)Natural Reactor$() is radioactive - its background is far above the Overworld\'s, and Fission Fragment seams are hot. '
        'Carry a Geiger Counter.$(li)$(fluid)Basaltic Magma$(), the Interior\'s sea, does 10 damage a hit and Fire Resistance does not help. '
        '$(fluid)Deep Brine$() sets you alight like lava.',
        'The floor is the $(item)Inner Core Boundary$(): solid iron under 330 GPa, which no tool yet made can break. The roof is bedrock, which '
        'the Core Drill cuts again from below.'],
        adv='plate_tectonics', sort=2, spot=['core_drill', 'mantle_breach', 'inner_core_boundary'])
    te('geography', 'r40_interior', 'The Planetary Interior', 'core_drill', [
        'Bedrock is not the bottom of the world - it is only as far as any tool could cut. The $(item)Core Drill$() cuts it: hold it against '
        'bedrock and keep holding. At the floor of the Overworld the bit breaks through into a $(item)Mantle Breach$(). Step in.',
        'The $(thing)Planetary Interior$() is layered by depth, like the planet. At the top lie the $(thing)Crustal Caverns$(), with patches of '
        '$(thing)Natural Reactor$() - Oklo rock where uranium seams once ran a chain reaction by themselves, two billion years ago. Below them the '
        '$(thing)Magma Chambers$(): columnar basalt over a sea of basaltic magma.',
        'Deeper still is the $(thing)Mantle Convection Zone$(), peridotite holding $(item)Ringwoodite$() and $(item)Bridgmanite$() - minerals that only '
        'exist under its pressure - where hot plumes rise and throw you upwards. At the bottom is the $(thing)Geodynamo Core$() of iron and nickel, '
        'whose currents make the planet\'s field: magnetic storms sweep through it, Magnetizing everything you carry. Beneath it lies the '
        '$(item)Inner Core Boundary$(): solid iron held at 330 GPa, which no tool yet made can cut.',
        '$(thing)Lithostatic Pressure$(): the deeper you stand, the heavier the rock above presses - P = ρgh - and the slower you move and mine. '
        'The Natural Reactor has a high radiation background, and its Fission Fragment seams are strongly radioactive.',
        'To leave, step back into a Mantle Breach. One opens beside you wherever you arrive, and the Core Drill cuts new ones through the '
        'Interior\'s bedrock.'],
        adv='plate_tectonics', sort=6, spot=['core_drill', 'ringwoodite', 'bridgmanite'])
