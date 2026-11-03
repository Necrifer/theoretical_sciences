"""
Round 11 - the Antiverse and antimatter chemistry.

Charge, parity and time reversed together (CPT) turn our universe into one that obeys exactly the same laws, made of
antimatter. A CPT Mirror opens the way to such a mirror universe - the Antiverse: an Earth-like world of antistone
under a copper sky, with seas of antiwater. Without a Penning Harness (a magnetic cage that keeps the visitor's matter
from ever touching anything) a visitor annihilates. Its rock holds anti-iron, anti-gold, anticarbon, antisilicon and
frozen antihydrogen; its creatures are made of antimatter too.

Chemistry links: antiprotons are decelerated and combined with positrons into antihydrogen; antiwater is split by
electrolysis; the Mirror Matter Foundry refines anti-ores into metals and binds matter and antimatter into the Mirror
Alloy (held apart by a Penning field in every grain); Baryogenesis - making a universe of matter at all - needs all of it.
"""
import json
import os

import shapes

NS = 'theoreticalsciences'


def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('cpt_mirror', 'CPT Mirror', 'shifter', '#2a1408', '#ffb070', 'Reverses charge, parity and time at once. Through it lies a universe exactly like ours, made of antimatter.', special='cptmirror', stack=1),
    _I('penning_harness', 'Penning Harness', 'anchor', '#101828', '#80d0ff', 'A wearable Penning trap: magnetic and electric fields that keep every atom of you from touching the Antiverse.', stack=1),
    _I('antiproton_bundle', 'Antiproton Bundle', 'pellet', '#2a0818', '#ff70b0', 'A few billion antiprotons, slowed from near light speed and held in a magnetic bottle.'),
    _I('raw_antiiron', 'Raw Anti-Iron', 'chunk', '#40281c', '#ffb080', 'Ore from the Antiverse. Iron, exactly - except that every particle in it is the opposite one.'),
    _I('antiiron_ingot', 'Anti-Iron Ingot', 'ingot', '#5a3a2a', '#ffc090', 'Anti-iron, refined in levitation. Its surface never touches the mould.'),
    _I('raw_antigold', 'Raw Anti-Gold', 'chunk', '#2a3048', '#80b0ff', 'Gold of the mirror universe, cold blue where ours is warm yellow - only the light it reflects is the same.'),
    _I('antigold_ingot', 'Anti-Gold Ingot', 'ingot', '#30406a', '#a0d0ff', 'Anti-gold: as soft, as dense and as noble as gold. Do not wear it.'),
    _I('anticarbon_crystal', 'Anticarbon Crystal', 'gem', '#101010', '#ff9050', 'Antidiamond. Four antibonds per anti-atom, as hard as diamond.'),
    _I('antisilicon_shard', 'Antisilicon Shard', 'shard', '#302018', '#ffd0a0', 'Semiconductor of the Antiverse - its holes are positrons.'),
    _I('antihydrogen_crystal', 'Antihydrogen Crystal', 'crystal', '#081838', '#80f0ff', 'Frozen antihydrogen: one antiproton, one positron, a few kelvin.'),
    _I('antihelium_nucleus', 'Antihelium Nucleus', 'orb', '#2a0830', '#ff80ff', 'Two antiprotons and two antineutrons. Our own detectors have glimpsed a handful; the Antiverse is full of them.'),
    _I('antiquark_shard', 'Antiquark Shard', 'multishard', '#200818', '#ff4080', 'Confined anti-colour charge, chipped from a creature of the Antiverse.'),
    _I('mirror_alloy_ingot', 'Mirror Alloy Ingot', 'ingot', '#283040', '#f0a0ff', 'Matter and antimatter in alternating grains, a Penning field between every pair. It hums.'),
    _I('mirror_processor', 'Mirror Processor', 'chip', '#2a1810', '#ffb070', 'Antisilicon logic on an anticarbon die: it computes with positrons.'),
    _I('baryogenesis_seed', 'Baryogenesis Seed', 'seed', '#100818', '#fff0c0', 'Out of equilibrium, violating baryon number, C and CP: the three conditions for a universe to end up made of something.'),
]

FLUIDS = [
    dict(id='antiwater', name='Antiwater', art='condensate', c1='#d07030', c2='#ffc090', gas=False, light=3, temp=290, density=1000, viscosity=1000),
    dict(id='antihydrogen', name='Antihydrogen', art='exotic', c1='#60d0ff', c2='#e0ffff', gas=True, light=6, temp=20, density=-100, viscosity=10),
    dict(id='liquid_antihelium', name='Liquid Antihelium', art='condensate', c1='#ff80ff', c2='#ffe0ff', gas=False, light=8, temp=2, density=125, viscosity=10),
]

BLOCKS = [
    dict(id='antistone', name='Antistone', art='stars', c1='#3a2418', c2='#ffb070', kind='simple', light=1, hardness=3.0, tool='iron'),
    dict(id='antisoil', name='Antisoil', art='stars', c1='#5a3018', c2='#ff9050', kind='simple', light=0, hardness=1.5, tool='none'),
    dict(id='antistone_bricks', name='Antistone Bricks', art='bricks', c1='#3a2418', c2='#ffb070', kind='deco', light=0, hardness=3.0, tool='iron'),
    dict(id='antiiron_ore', name='Anti-Iron Ore', art='ore', base='antistone', c1='#5a3a2a', c2='#ffc090', kind='ore', drops='raw_antiiron', light=3, hardness=3.5, tool='iron'),
    dict(id='antigold_ore', name='Anti-Gold Ore', art='ore', base='antistone', c1='#30406a', c2='#a0d0ff', kind='ore', drops='raw_antigold', light=3, hardness=3.5, tool='iron'),
    dict(id='anticarbon_ore', name='Anticarbon Ore', art='ore', base='antistone', c1='#101010', c2='#ff9050', kind='ore', drops='anticarbon_crystal', light=4, hardness=4.5, tool='diamond'),
    dict(id='antisilicon_ore', name='Antisilicon Ore', art='ore', base='antistone', c1='#302018', c2='#ffd0a0', kind='ore', drops='antisilicon_shard', light=3, hardness=3.5, tool='iron'),
    dict(id='antihydrogen_ice', name='Antihydrogen Ice', art='ore', base='antistone', c1='#081838', c2='#80f0ff', kind='ore', drops='antihydrogen_crystal', light=8, hardness=3.0, tool='iron'),
]

MACHINE_TEXT = {
    'antiproton_decelerator': ('Antiproton Decelerator', 'A ring that catches antiprotons from collisions at nearly light speed and brakes them, lap after lap, until they can be trapped.'),
    'mirror_foundry': ('Mirror Matter Foundry', 'A stack of Penning traps: anti-ores are melted, refined and cast without ever touching a wall - and matter and antimatter are bound into one alloy.'),
}
MACHINE_CAT = {'antiproton_decelerator': 'antimatter', 'mirror_foundry': 'antimatter'}
MACHINE_RESEARCH = {'antiproton_decelerator': 'antiatom_chemistry', 'mirror_foundry': 'mirror_metallurgy'}
TIER = {'antiproton_decelerator': 3, 'mirror_foundry': 4}
MC = {
    'antiproton_decelerator': (['MRM', 'CTC', 'NSN'], {'M': 'dipole_bending_magnet', 'R': 'rf_accelerating_cavity', 'C': 'superconducting_coil', 'T': 'antimatter_penning_trap',
                                                       'N': 'neutronium_plate', 'S': 'spacetime_casing'}),
    'mirror_foundry': (['XLX', 'AFA', 'MEM'], {'X': 'antimatter_penning_trap', 'L': 'photonic_lens', 'A': 'antiproton_bundle', 'F': 'annihilation_reactor',
                                               'M': 'magnetic_confinement_casing', 'E': 'event_horizon_casing'}),
}
EXTRA = {
    'antiproton_decelerator': 'Turns antimatter pellets and virtual quarks into $(item)Antiproton Bundles$(), and bundles into $(fluid)Antihydrogen$().',
    'mirror_foundry': 'Refines raw anti-metals, binds the $(item)Mirror Alloy$() and grows the $(item)Baryogenesis Seed$().',
}

RESEARCH = [
    ('cpt_symmetry', 'CPT Symmetry', 'Reverse charge, parity and time together and the laws of physics do not change. Somewhere, then, there is a universe of antimatter. Unlocks the CPT Mirror and the Penning Harness.',
     'cpt_mirror', 12, 17, 120, ['antimatter', 'special_relativity'], [('antimatter_pellet', 4)]),
    ('antiatom_chemistry', 'Antiatoms', 'An antiproton and a positron make antihydrogen - with exactly the same spectrum as hydrogen. Unlocks the Antiproton Decelerator.',
     'antiproton_decelerator', 14, 17, 140, ['cpt_symmetry', 'exotic_chemistry'], [('antihydrogen_crystal', 4)]),
    ('mirror_metallurgy', 'Mirror Metallurgy', 'Metals that must never touch anything. Unlocks the Mirror Matter Foundry, anti-metals and the Mirror Alloy.',
     'mirror_foundry', 16, 17, 200, ['antiatom_chemistry', 'plasma_metallurgy'], [('raw_antiiron', 8)]),
    ('baryon_asymmetry', 'Baryon Asymmetry', 'For every billion antibaryons in the early universe there were a billion and one baryons. We are the one. Unlocks the Baryogenesis Seed.',
     'baryogenesis_seed', 18, 17, 500, ['mirror_metallurgy', 'cosmogenesis'], [('antihelium_nucleus', 2)]),
]

# id -> (name, drops: [(item, min, max, chance)])
CREATURES = {
    'annihilator': ('Annihilator', [('antiquark_shard', 1, 2, 1.0), ('antihelium_nucleus', 1, 1, 0.25)]),
    'positronium_skitterer': ('Positronium Skitterer', [('antisilicon_shard', 1, 2, 1.0), ('antiquark_shard', 0, 1, 1.0)]),
    'antihydrogen_wisp': ('Antihydrogen Wisp', [('antihydrogen_crystal', 1, 2, 1.0)]),
}

LORE = {
    'cpt_mirror': ('A ring of polished anti-gold around nothing at all - look into it and you see yourself reversed in every way a mirror can manage and two ways it cannot.',
                   'CPT symmetry says a universe with charge, parity and time all reversed runs on our exact laws. I built the mirror to test the claim; it opened.'),
    'penning_harness': ('Coils across the shoulders, electrodes at wrist and ankle, a faint blue hum. It holds a bubble of field a hair thick around the wearer, and nothing gets through it.',
                        'The first step into the Antiverse lit me up like a flare. A Penning trap holds a cloud of positrons forever without walls - I scaled one up and wore it.'),
    'antiproton_bundle': ('A magnetic bottle the size of a thumb with a pale pink knot of light at its centre: billions of antiprotons circling, never touching the glass.',
                          'The Decelerator catches them from collisions at nearly light speed and brakes them lap after lap until a trap can hold them.'),
    'raw_antiiron': ('It looks exactly like iron ore in the Antiverse and exactly like a bomb anywhere else. Kept in a Penning field it is perfectly docile.',
                     'Broken out of the antistone in the Antiverse. The harness kept me whole; my pickaxe did not survive.'),
    'antiiron_ingot': ('A bar of anti-iron, refined in levitation and cooled in vacuum. It rings like iron when the field taps it.',
                       'The Mirror Matter Foundry melts the ore without a crucible - the metal floats in a Penning field the whole time.'),
    'raw_antigold': ('Cold blue in the light of the Antiverse sky. Under a white lamp it is gold-coloured after all: colour depends on electrons, and positrons behave exactly the same.',
                     'Found in veins in the antistone, shining blue-white under the copper sky.'),
    'antigold_ingot': ('Soft, dense, incorruptible - and the most dangerous jewellery imaginable.',
                       'Cast in the Foundry from raw anti-gold. The mirror symmetry is perfect: same melting point, same density.'),
    'anticarbon_crystal': ('Antidiamond: the hardest thing in the Antiverse, glowing faintly where stray matter atoms meet its surface.',
                           'Picked out of anticarbon veins deep in the antistone.'),
    'antisilicon_shard': ('Its band gap is the same as silicon. Its holes are positrons. It would make perfectly good chips for an anti-engineer.',
                          'From antisilicon veins - and from the Positronium Skitterers that eat it.'),
    'antihydrogen_crystal': ('The simplest anti-atom, frozen into a crystal. It shines with the same spectral lines as hydrogen, exactly - which is the point.',
                             'Found as ice in the Antiverse. Comparing its spectrum with hydrogen to twelve decimal places was the first test of CPT I ever did.'),
    'antihelium_nucleus': ('Two antiprotons and two antineutrons, tightly bound. On Earth our best detectors argue about a handful of candidates; here the creatures carry them.',
                           'Dropped by an Annihilator - it seems to use them as a core.'),
    'antiquark_shard': ('Anti-colour charge cannot be isolated, so this is a shard of something larger that refuses to come apart further.',
                        'Torn from the shells of Antiverse creatures.'),
    'mirror_alloy_ingot': ('Matter and antimatter grains alternate in it like a chessboard, each pair held apart by its own Penning field. It hums, and it is warm.',
                           'If the field that keeps a grain of anti-iron from touching iron can be made small enough, the two can share an ingot. The Foundry makes it so.'),
    'mirror_processor': ('A chip of antisilicon on an anticarbon die. It runs any program at all - with positrons instead of electrons, and current flowing the other way.',
                         'Etched in the Foundry. It is the first circuit I have made that would work equally well on either side of the mirror.'),
    'baryogenesis_seed': ('A small, warm, impossible thing: a process that makes a little more matter than antimatter, every time. It is why there is anything at all.',
                          'Sakharov wrote down the three conditions in 1967. I met all three in the Foundry, out of equilibrium, with CP violation from the anti-gold lattice.'),
}

LANG = {
    'puzzle.theoreticalsciences.drop_here': 'Drag tiles into the boxes',
    'message.theoreticalsciences.law_warning.antiverse': 'You are annihilating! Carry a %s.',
    'message.theoreticalsciences.void_antiverse': 'You fall out of the bottom of the Antiverse - and back into your own universe.',
    'message.theoreticalsciences.cpt_locked': 'You need to research CPT Symmetry first.',
    'message.theoreticalsciences.antiverse_arrival': 'Everything is the same, and everything is the opposite. Do not touch anything.',
    'message.theoreticalsciences.antiwater_burns': 'The antiwater is annihilating against you!',
    'biome.theoreticalsciences.antiverse': 'The Antiverse',
    'dimension.theoreticalsciences.antiverse': 'The Antiverse',
    'advancements.theoreticalsciences.antiverse_arrival.title': 'Through the Mirror',
    'advancements.theoreticalsciences.antiverse_arrival.description': 'Enter the Antiverse',
}

# puzzles for the new research (merged like the others by round7.finish via tools/puzzles/*.json)
PUZZLES = {
    'cpt_symmetry': {
        'goal': 'An electron and a positron annihilate into two photons; find each photon\'s wavelength (the Compton wavelength) in pm. h in 1e-34 J s, m in 1e-31 kg, c in 1e8 m/s.',
        'lhs': 'λ', 'expr': '10 * h / ( m * c )',
        'vars': {'h': "Planck's constant (units of 1e-34 J s)", 'm': 'electron (= positron) mass (units of 1e-31 kg)', 'c': 'speed of light (units of 1e8 m/s)'},
        'given': {'h': 6.626, 'm': 9.109, 'c': 2.998}, 'distractors': ['2', 'E'],
        'method': 'All the rest energy becomes light: each photon carries E = mc², and a photon of energy E has wavelength hc/E. The units leave a factor of ten picometres.',
        'unit': 'pm'},
    'antiatom_chemistry': {
        'goal': 'CPT says antihydrogen has exactly the spectrum of hydrogen. Find the energy of its n-th level in eV, with R the Rydberg energy.',
        'lhs': 'E', 'expr': '- R / n ^ 2',
        'vars': {'R': 'Rydberg energy (eV)', 'n': 'principal quantum number'},
        'given': {'R': 13.6, 'n': 2}, 'distractors': ['2', 'Z'],
        'method': 'Bohr: the bound energy falls off as the square of the level number, and is negative because the positron is bound.',
        'unit': 'eV'},
    'mirror_metallurgy': {
        'goal': 'A Penning trap holds an antiproton by making it circle in a magnetic field. Find the cyclotron frequency in MHz, with q in 1e-19 C and m in 1e-27 kg.',
        'lhs': 'f', 'expr': '100 * q * B / ( 2 * π * m )',
        'vars': {'q': 'antiproton charge magnitude (units of 1e-19 C)', 'B': 'magnetic field (T)', 'm': 'antiproton mass (units of 1e-27 kg)'},
        'given': {'q': 1.602, 'B': 5.0, 'm': 1.673}, 'distractors': ['E', 'v'],
        'method': 'Set the magnetic force qvB equal to the centripetal force mv²/r: the angular frequency qB/m does not depend on speed. Divide by 2π for cycles per second.',
        'unit': 'MHz'},
    'baryon_asymmetry': {
        'goal': 'Find the baryon asymmetry of the early universe: the surplus of baryons over antibaryons per photon.',
        'lhs': 'η', 'expr': '( nB - nA ) / nG',
        'vars': {'nB': 'number of baryons', 'nA': 'number of antibaryons', 'nG': 'number of photons'},
        'given': {'nB': 1000000006, 'nA': 1000000000, 'nG': 10000000000}, 'distractors': ['2', 'c'],
        'method': 'Everything that could annihilate did, leaving only the surplus; compare that surplus with the photons the annihilation left behind.',
        'unit': ''},
}

DIM_INFO = ('The Antiverse', (1, 3, 1, 0), 'penning_harness', 'antiwater still burns')


# ---------------------------------------------------------------- multiblock: a stack of Penning traps
def mirror_foundry():
    s = shapes.Shape(9, 9, 9)
    c = 4
    s.disc(c, c, 0, 4.3, 'M')
    for y in range(1, 8):
        if y % 2 == 1:
            s.disc(c, c, y, 3.5, 'M', r_in=2.5)        # the trap's ring electrodes / coils
        else:
            s.disc(c, c, y, 3.5, 'G', r_in=2.5)        # windows between them
    for y in range(1, 8):
        s.disc(c, c, y, 2.4, '.')
    s.put(c, 8, c, 'K')
    s.put(c, 7, c, 'F')
    s.center = (c, 4, c)
    s.ports('M', 'N', 4, y=0)
    s.v[(c, 1, c - 4)] = 'M'
    s.v[(c, 1, c - 3)] = 'C'
    s.ctrl = (c, 1, c - 3)
    return s


shapes.BUILDERS['mirror_foundry'] = mirror_foundry

import round5 as _r5
_r5.DIM_INFO['antiverse'] = DIM_INFO
_r5.DIM_ENTRIES['antimatter/cpt'] = ['antiverse']
_here = os.path.dirname(os.path.abspath(__file__))
json.dump(PUZZLES, open(os.path.join(_here, 'puzzles', 'group_antimatter.json'), 'w'), indent=1, ensure_ascii=False)


def apply(g):
    w, q, mr, shaped, text_entry, LANG_ = g['w'], g['q'], g['mr'], g['shaped'], g['text_entry'], g['LANG']
    D, WG = g['D'], g['WG']
    LANG_.update(LANG)
    AD, MF, EL = 'antiproton_deceleration', 'mirror_synthesis', 'electrolysis'
    # ---- getting there
    shaped('cpt_mirror', ['APA', 'GLG', 'ANA'], {'A': 'antimatter_pellet', 'P': 'photonic_lens', 'G': 'minecraft:gold_ingot', 'L': 'metamaterial_sheet', 'N': 'neutronium_plate'}, 'cpt_mirror')
    shaped('penning_harness', ['CNC', 'TQT', 'CNC'], {'C': 'superconducting_coil', 'N': 'neutronium_plate', 'T': 'antimatter_penning_trap', 'Q': 'chronon_capacitor'}, 'penning_harness')
    shaped('antistone_bricks', ['AA', 'AA'], {'A': 'antistone'}, 'antistone_bricks', 4)
    # ---- the decelerator
    mr('antiproton_bundle', AD, [('antimatter_pellet', 2), ('virtual_quark', 4)], [], [('antiproton_bundle', 2)], [], 1048576, 300, 'antiatom_chemistry')
    mr('antiproton_from_antihelium', AD, [('antihelium_nucleus', 1)], [], [('antiproton_bundle', 4)], [], 1048576, 200, 'antiatom_chemistry')
    mr('antihydrogen', AD, [('antiproton_bundle', 1), ('antimatter_pellet', 1)], [], [], [('antihydrogen', 1000)], 2097152, 300, 'antiatom_chemistry')
    mr('antihydrogen_from_crystal', AD, [('antihydrogen_crystal', 1)], [], [], [('antihydrogen', 500)], 524288, 100, 'antiatom_chemistry')
    # ---- chemistry of antiwater
    mr('split_antiwater', EL, [('containment_cell', 1)], [('antiwater', 1000)], [('containment_cell', 1)], [('antihydrogen', 1500)], 1048576, 300, 'antiatom_chemistry')
    # ---- the foundry
    mr('antiiron_ingot', MF, [('raw_antiiron', 2)], [('liquid_antihelium', 100)], [('antiiron_ingot', 2)], [], 4194304, 300, 'mirror_metallurgy')
    mr('antigold_ingot', MF, [('raw_antigold', 2)], [('liquid_antihelium', 100)], [('antigold_ingot', 2)], [], 4194304, 300, 'mirror_metallurgy')
    mr('liquid_antihelium', MF, [('antiproton_bundle', 2), ('antihelium_nucleus', 1)], [('antihydrogen', 4000)], [], [('liquid_antihelium', 1000)], 8388608, 400, 'mirror_metallurgy')
    mr('liquid_antihelium_slow', MF, [('antiproton_bundle', 8)], [('antihydrogen', 8000)], [], [('liquid_antihelium', 500)], 8388608, 800, 'mirror_metallurgy')
    mr('mirror_alloy_ingot', MF, [('antiiron_ingot', 1), ('neutronium_ingot', 1), ('superconducting_coil', 2)], [('liquid_antihelium', 250)], [('mirror_alloy_ingot', 1)], [],
       16777216, 400, 'mirror_metallurgy')
    mr('mirror_processor', MF, [('antisilicon_shard', 4), ('anticarbon_crystal', 1), ('antiquark_shard', 1), ('quantum_processor', 1)], [('liquid_antihelium', 250)],
       [('mirror_processor', 1)], [], 16777216, 400, 'mirror_metallurgy')
    mr('baryogenesis_seed', MF, [('antigold_ingot', 4), ('mirror_alloy_ingot', 4), ('arrow_of_time_shard', 2), ('mirror_processor', 1)], [('liquid_antihelium', 1000), ('antihydrogen', 4000)],
       [('baryogenesis_seed', 1)], [], 268435456, 1200, 'baryon_asymmetry')
    # ---- the old chains now pass through the mirror
    import glob as _g

    def add_input(name, item, n):
        for f in _g.glob(os.path.join(g['REC'], '*', name + '.json')):
            r = json.load(open(f))
            if 'ingredients' in r and r['type'].endswith('extreme_shapeless'):
                if not any(i.get('item') == q(item) for i in r['ingredients']):
                    r['ingredients'] += [{'item': q(item)}] * max(0, min(n, 81 - len(r['ingredients'])))
                    w(f, r)
                continue
            if 'item_inputs' not in r or any(i.get('item') == q(item) for i in r['item_inputs']):
                continue
            cap = {m['recipe']: m['inputs'] for m in g['M']}.get(r['type'].split(':')[1], 9)
            ins = r['item_inputs']
            if len(ins) + len(r.get('catalysts', [])) >= cap:
                ins[-1] = {'item': q(item), 'count': n}
            else:
                ins.append({'item': q(item), 'count': n})
            w(f, r)
    for name, item, n in (('hypernova_alloy_ingot', 'mirror_alloy_ingot', 1), ('transcendent_metal_ingot', 'mirror_alloy_ingot', 2),
                          ('omega_point', 'mirror_processor', 2), ('omniscience_catalyst', 'baryogenesis_seed', 1),
                          ('grow_bubble_universes', 'baryogenesis_seed', 1)):
        add_input(name, item, n)
    # ---- the Antiverse in the world of ores and loot
    g['loot_chest']('antiverse_vault', [('raw_antiiron', 14, 2, 6), ('raw_antigold', 10, 1, 4), ('anticarbon_crystal', 6, 1, 3), ('antisilicon_shard', 10, 2, 6),
                                        ('antihydrogen_crystal', 10, 2, 6), ('antihelium_nucleus', 3, 1, 1), ('antiproton_bundle', 6, 1, 3), ('containment_cell', 6, 2, 4)])
    a = {'predicate_type': 'minecraft:block_match', 'block': q('antistone')}
    g['ore_feature']('ore_antiiron', a, 'antiiron_ore', 8, 10, -48, 120)
    g['ore_feature']('ore_antigold', a, 'antigold_ore', 7, 6, -48, 60)
    g['ore_feature']('ore_anticarbon', a, 'anticarbon_ore', 6, 4, -60, 20)
    g['ore_feature']('ore_antisilicon', a, 'antisilicon_ore', 8, 8, -20, 100)
    g['ore_feature']('ore_antihydrogen_ice', a, 'antihydrogen_ice', 7, 6, 20, 160)
    # ---- the Codex
    text_entry('antimatter', 'cpt', 'Through the Mirror', 'cpt_mirror', [
        'Notebook, antimatter. Swap every particle for its antiparticle (C), reflect the world in a mirror (P), and run the film backwards (T). Do all three at once and the laws of physics come out exactly the same: CPT symmetry, as solid as anything we know.',
        'So a universe of antimatter would be a universe like ours. The $(item)CPT Mirror$() opens onto one - the $(thing)Antiverse$(): continents of antistone under a copper sky, seas of antiwater, anti-ores in the rock.',
        'Rule one: do not touch anything. Without a $(item)Penning Harness$() you annihilate in seconds. With one, you are wrapped in a field a hair thick that nothing crosses - except that the sea still burns. Fall off the bottom of the world and you come home.'],
        adv='cpt_symmetry', sort=1, spot=['cpt_mirror', 'penning_harness'])
    text_entry('antimatter', 'antiatoms', 'Antiatoms', 'antiproton_decelerator', [
        'An antiproton and a positron make $(item)antihydrogen$(), and its spectral lines sit exactly where hydrogen\'s do - measured to twelve decimal places, and still CPT holds.',
        'The $(item)Antiproton Decelerator$() brakes antiprotons from collisions until they can be trapped, and combines them with positrons into $(fluid)Antihydrogen$(). $(fluid)Antiwater$() from the Antiverse sea can be split in the $(item)Electrolytic Cell$() for the same gas.'],
        adv='antiatom_chemistry', sort=2, spot=['antiproton_bundle', 'antihydrogen_crystal'])
    text_entry('antimatter', 'mirror_metallurgy', 'Mirror Metallurgy', 'mirror_foundry', [
        'A metal that may never touch a crucible, a mould or the air. The $(item)Mirror Matter Foundry$() is a stack of Penning traps: the anti-ore floats, melts, refines and cools in fields alone.',
        'Cooled by $(fluid)Liquid Antihelium$(), it casts $(item)Anti-Iron$() and $(item)Anti-Gold$(), etches $(item)Mirror Processors$() from antisilicon, and binds matter and antimatter grain by grain into the $(item)Mirror Alloy$().'],
        adv='mirror_metallurgy', sort=3, spot=['antiiron_ingot', 'antigold_ingot', 'mirror_alloy_ingot'])
    text_entry('antimatter', 'baryogenesis', 'Why Anything Exists', 'baryogenesis_seed', [
        'The Big Bang should have made matter and antimatter in equal amounts, and they should have annihilated into light. For every billion antibaryons there was one extra baryon, and everything we are is made of that one.',
        'Sakharov\'s three conditions - baryon number violation, C and CP violation, and departure from equilibrium - can all be met in the Foundry. The $(item)Baryogenesis Seed$() is the result: every universe you seed from now on needs one, or it would be made of light alone.'],
        adv='baryon_asymmetry', sort=4, spot=['baryogenesis_seed'])
    text_entry('antimatter', 'antiverse_life', 'Life of the Antiverse', 'antihelium_nucleus', [
        'Its creatures are made of antimatter like everything else there. $(thing)Annihilators$() are shells around a core of $(item)antihelium$(); $(thing)Positronium Skitterers$() eat antisilicon; $(thing)Antihydrogen Wisps$() drift over the sea and drop frozen antihydrogen.'],
        adv='cpt_symmetry', sort=5)


def finish(g):
    """After every recipe exists: the Antiverse dimension, its biome and its advancement."""
    w, q, D, WG = g['w'], g['q'], g['D'], g['WG']
    import zipfile
    # ---- terrain: our own Overworld's noise, made of antistone, with seas of antiwater
    with zipfile.ZipFile(__import__("mcpaths").CLIENT_JAR) as z:
        ns = json.loads(z.read('data/minecraft/worldgen/noise_settings/overworld.json'))
    ns['default_block'] = {'Name': q('antistone')}
    ns['default_fluid'] = {'Name': q('antiwater'), 'Properties': {'level': '0'}}
    ns['ore_veins_enabled'] = False
    ns['aquifers_enabled'] = True          # dry caves and flooded ones, like home; without aquifers every cave would flood
    ns['disable_mob_generation'] = False
    ns['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': [
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:vertical_gradient', 'random_name': 'minecraft:bedrock_floor',
                                                    'true_at_and_below': {'above_bottom': 0}, 'false_at_and_above': {'above_bottom': 5}},
         'then_run': {'type': 'minecraft:block', 'result_state': {'Name': 'minecraft:bedrock'}}},
        {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:stone_depth', 'offset': 0, 'surface_type': 'floor', 'add_surface_depth': True, 'secondary_depth_range': 0},
         'then_run': {'type': 'minecraft:block', 'result_state': {'Name': q('antisoil')}}},
    ]}
    w(os.path.join(WG, 'noise_settings', 'antiverse.json'), ns)
    w(os.path.join(D, 'dimension_type', 'antiverse.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
        'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.15, 'logical_height': 384,
        'effects': 'minecraft:overworld', 'infiniburn': '#minecraft:infiniburn_overworld', 'min_y': -64, 'height': 384,
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'dimension', 'antiverse.json'), {'type': q('antiverse'), 'generator': {
        'type': 'minecraft:noise', 'settings': q('antiverse'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('antiverse')}}})
    feats = [[] for _ in range(11)]
    feats[6] = [q(x) for x in ('ore_antiiron', 'ore_antigold', 'ore_anticarbon', 'ore_antisilicon', 'ore_antihydrogen_ice')]
    w(os.path.join(WG, 'biome', 'antiverse.json'), {
        'has_precipitation': False, 'temperature': 0.6, 'downfall': 0.0,
        'effects': {'sky_color': 0xC8743C, 'fog_color': 0x6A3A20, 'water_color': 0xD07030, 'water_fog_color': 0x5A2810,
                    'grass_color': 0xB060C0, 'foliage_color': 0xB060C0,
                    'ambient_sound': 'minecraft:ambient.crimson_forest.loop', 'music': {'sound': q('music.antiverse'), 'min_delay': 1200, 'max_delay': 3600, 'replace_current_music': True},
                    'particle': {'options': {'type': 'minecraft:electric_spark'}, 'probability': 0.002}},
        'spawners': {'monster': [{'type': q('annihilator'), 'weight': 8, 'minCount': 1, 'maxCount': 2},
                                 {'type': q('positronium_skitterer'), 'weight': 12, 'minCount': 2, 'maxCount': 4}],
                     'ambient': [{'type': q('antihydrogen_wisp'), 'weight': 10, 'minCount': 1, 'maxCount': 3}]},
        'spawn_costs': {}, 'carvers': {}, 'features': feats})
    w(os.path.join(D, 'advancement', 'antiverse_arrival.json'), {
        'display': {'icon': {'id': q('cpt_mirror')}, 'title': {'translate': 'advancements.theoreticalsciences.antiverse_arrival.title'},
                    'description': {'translate': 'advancements.theoreticalsciences.antiverse_arrival.description'}, 'frame': 'goal',
                    'show_toast': True, 'announce_to_chat': True, 'hidden': False},
        'parent': f'{NS}:research/cpt_symmetry',
        'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q('antiverse')}}}, 'requirements': [['arrived']]})
