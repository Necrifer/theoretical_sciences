"""
Round 56 (1.10.1): fixes from the 1.10 playtest.

* The Molecular Cloud Nebula is now made of cloud: Dust, Hydrogen and Oxygen Cloud you can pass through (with Nebular
  Ice still in it), and Protostar Fragments - small parts of stars - glowing inside the clouds.
* Every Frontier block has a use and a Codex page; each region's hazard has a counter: the Frontier Suit (Adaptive Optics
  Visor, Thermal Shroud, Tidal Brace, Gravity Compensators) beside the Whipple Shield, Radiation Plating, the Atomic Clock
  and the Precision Gyroscope.
* Every material the expansion added can be made again by machine, and every one is used by something.
* The Black Swan Feather is a shortcut, not a crop: one per swan, and no recipe multiplies it.
"""
import json
import os

import round12 as _r12
from round12 import V as _V
import kit
from kit import I, B, ORE

NS = 'theoreticalsciences'
M = NS + ':'

ITEMS = [
    I('protostellar_ember', 'Protostellar Ember', 'star', '#602010', '#ffc060',
      'A shard of a star not yet lit, still glowing with the heat of its own collapse. A furnace of a fuel: it burns for a long time.'),
]
BLOCKS = [
    B('dust_cloud', 'Dust Cloud', 'foambubble', '#403028', '#a08870', kind='cloud', light=0, hardness=0.3, tool='none'),
    B('hydrogen_cloud', 'Hydrogen Cloud', 'foambubble', '#601020', '#ff6080', kind='cloud', light=4, hardness=0.3, tool='none'),
    B('oxygen_cloud', 'Oxygen Cloud', 'foambubble', '#104040', '#60ffd0', kind='cloud', light=4, hardness=0.3, tool='none'),
    ORE('protostar_fragment', 'Protostar Fragment', 'nebular_ice', '#602010', '#ffe080', 'protostellar_ember', light=13, hardness=3.0, tool='stone'),
]
FLUIDS = []
RESEARCH = []
PUZZLES = {}
KEY_ADD = {}
MACHINE_TEXT, MACHINE_CAT, MACHINE_RESEARCH, TIER, MC, EXTRA = {}, {}, {}, {}, {}, {}

# the Frontier Suit, registered in Java (InteriorGear.Suit.FRONTIER): each piece answers one region of the Frontier
SUIT = ('frontier_suit', 'station_hull', 'einstein_ring_lens', '#384050', '#c0d0f0',
        'Each piece answers one region of the Accretion Frontier.')
PIECES = {
    'helmet': ('Adaptive Optics Visor', ['BMB', 'B B'], 'einstein_ring_lens',
               'A visor that measures how the light reaching it is bent and unbends it: you see through nebular dust and the Lensing Void\'s '
               'warped sight lines, and a Nebuloid cannot blind you.'),
    'chestplate': ('Thermal Shroud', ['B B', 'BMB', 'BBB'], 'accretion_disk_fragment',
                   'Layers of reflective foil round a coolant loop: the Accretion Rim\'s heat cannot set you alight.'),
    'leggings': ('Tidal Brace', ['BMB', 'B B', 'B B'], 'tidal_shear_crystal',
                 'A frame that takes the stretch for you: the Accretion Rim\'s tidal pull no longer strains you.'),
    'boots': ('Gravity Compensators', ['M M', 'B B'], 'planetary_core_sample',
              'Boots that pull you down where gravity is weak and hold you up where it is strong: you walk normally in the Asteroid Belt and '
              'the Pulsar Field.'),
}

# (name, type, item ins, fluid ins, item outs, fluid outs, FE/t, ticks, research, insight[, catalysts[, chances]])
TP, HF, PM, TD, EV, IS, SP, BA = ('tectonic_pressing', 'shock_synthesis', 'pulsed_field', 'time_dilation', 'hawking_evaporation',
                                  'interstellar_scooping', 'spectroscopy', 'bayesian_inference')
RECIPES = [
    # ---- replenishable by machine what the Frontier gives
    ('cosmic_ray_glass', IS, [('minecraft:glass', 1)], [], [('cosmic_ray_shard', 1)], [], 64, 800, 'interstellar_medium', 0),
    ('brown_dwarf_collapse', TP, [('interstellar_dust', 16)], [('molecular_cloud', 1000)], [('brown_dwarf_core', 1)], [], 8192, 1200,
     'accretion_physics', 4),
    ('quench_accretion_fragment', TP, [('rim_slag', 2)], [('accretion_disk_plasma', 250)], [('accretion_disk_fragment', 2)], [], 4096, 600,
     'accretion_physics', 0),
    ('planetesimal_impact', HF, [('interstellar_dust', 8), ('core_iron', 1), ('rail_armature', 1)], [], [('exoplanet_core_sample', 1)], [], 8192, 600,
     'exoplanetology', 2),
    ('magnetar_crust', PM, [('pulsar_crust', 2), ('core_iron', 2)], [], [('magnetar_crust_fragment', 1)], [], 16384, 800, 'magnetars', 2),
    ('tidal_shear_crystal', TD, [('minecraft:amethyst_shard', 2)], [('tidal_plasma', 250)], [('tidal_shear_crystal', 2)], [], 4096, 800,
     'tidal_physics', 2),
    ('tidal_plasma', TD, [], [('accretion_disk_plasma', 500)], [], [('tidal_plasma', 250)], 4096, 600, 'tidal_physics', 0),
    ('merger_remnant', EV, [('micro_singularity', 2)], [], [('merger_remnant', 1)], [('hawking_radiation', 500)], 131072, 2400, 'binary_mergers', 20,
     ['kugelblitz_seed']),
    ('chirp_record', 'gw_detection', [('gw_record_bbh', 1), ('strain_sensor', 1)], [], [('chirp_signal_record', 2)], [], 8192, 1600, 'binary_mergers', 8),
    ('meteorites_from_asteroids', SP, [('asteroid_rock', 4), ('spectral_plate', 1)], [], [('meteoric_iron', 2), ('chondrite_fragment', 1)], [], 20000,
     300, 'exoplanetology', 1),
    # ---- the Frontier's blocks put to use
    ('regolith_assay', SP, [('frontier_regolith', 8), ('spectral_plate', 1)], [], [('interstellar_dust', 4), ('rare_earth_oxide', 1)], [], 20000, 300,
     'interstellar_medium', 1),
    ('sublimate_ice', 'electrolysis', [('nebular_ice', 2)], [], [], [('molecular_cloud', 250)], 1024, 200, 'interstellar_medium', 0),
    ('void_glass_lens', 'optical_finishing', [('void_glass', 4), ('fused_silica', 1)], [], [('einstein_ring_lens', 1)], [], 512, 800,
     'gravitational_lensing', 2),
    ('ember_accretion', 'accretion_power', [('protostellar_ember', 1)], [('molecular_cloud', 250)], [('quasar_jet_filament', 1)],
     [('accretion_disk_plasma', 400)], 12288, 1800, 'accretion_physics', 4, [], [0.3]),
    # ---- a use for every material
    ('white_dwarf', TP, [('planetary_nebula_shell', 4)], [], [('white_dwarf_core', 1)], [], 16384, 1600, 'compact_stars', 6),
    ('subducted_slab', TP, [('lithospheric_plate_fragment', 2)], [], [('bridgmanite', 2)], [], 8192, 800, 'mantle_geodynamics', 2),
    ('aquifer_radium', SP, [('aquifer_sample', 2), ('spectral_plate', 1)], [], [('radium_salt', 1)], [], 20000, 400, 'radiochemistry', 1),
    ('satellite_geoid', 'seismography', [('satellite_image_plate', 1), ('survey_map', 1)], [], [('geoid_model', 2)], [], 30000, 300, 'remote_sensing', 2),
    ('posterior_transit', BA, [('prior_tablet', 1), ('transit_light_curve', 1)], [], [('posterior_tablet', 2)], [], 20000, 300, 'exoplanetology', 2),
    ('posterior_ice_core', BA, [('prior_tablet', 1), ('ice_core', 1)], [], [('posterior_tablet', 2)], [], 20000, 300, 'climatology', 2),
    ('posterior_permafrost', BA, [('prior_tablet', 1), ('permafrost_sample', 1)], [], [('posterior_tablet', 2)], [], 20000, 300, 'climatology', 2),
    ('posterior_tracer', BA, [('prior_tablet', 1), ('technetium_99m_vial', 1)], [], [('posterior_tablet', 2)], [], 20000, 300, 'decay_chains', 2),
    ('posterior_samples', BA, [('prior_tablet', 2), ('monte_carlo_sample_pack', 1)], [], [('posterior_tablet', 6)], [], 20000, 300,
     'monte_carlo_methods', 3),
    ('rtg_waste_glass', 'thermoelectric', [('nuclear_waste_glass', 1)], [], [], [], 32, 24000, None, 0),
    ('rtg_polonium', 'thermoelectric', [('polonium_210_source', 1)], [], [], [], 96, 12000, None, 0),
    ('rtg_ember', 'thermoelectric', [('protostellar_ember', 1)], [], [], [], 128, 16000, None, 0),
    ('thorium_fuel_salt', 'isotope_enrichment', [('thorium_232_pellet', 2), ('uranium_235_pellet', 1), ('fluorspar', 2)], [('deep_brine', 1000)], [],
     [('molten_salt_fuel', 1500)], 1024, 300, 'nuclear_fission', 0),
    ('hydrothermal_quartz', 'crystal_growth', [('minecraft:quartz', 8)], [('supercritical_water', 1000)], [('cherenkov_quartz', 4)], [], 1024, 400,
     'hydrology', 0),
    ('ferrofluid_colloid', 'chemical_synthesis', [('magnetite', 1)], [('brownian_suspension', 500)], [], [('ferrofluid', 500)], 512, 200,
     'stochastic_processes', 0),
    ('partition_from_gas', 'chemical_synthesis', [('normal_distribution_crystal', 2)], [('maxwell_boltzmann_gas', 1000)], [('partition_function_matrix', 1)],
     [], 4096, 600, 'statistical_mechanics', 4),
    ('ism_plasma', 'fusion_power', [], [('interstellar_medium', 500)], [], [('magnetized_plasma', 100)], 2048, 400, 'magnetic_confinement', 0),
    ('mhd_magnetized', 'magnetohydrodynamic', [], [('magnetized_plasma', 100)], [], [], 0, 400, 'magnetohydrodynamics', 0),
    # 1.14.2: with a fixed point to converge on, the condenser needs only half the material for the same weave of spin foam
    ('spin_foam_fixed_point', 'graviton_condensing', [('graviton_crystal', 12), ('qubit', 6), ('higgs_catalyst', 6)],
     [('bose_einstein_condensate', 32000)], [('spin_foam_lattice', 2)], [], 32768, 700, 'asymptotic_safety', 10, ['fixed_point_lattice']),
    ('holographic_matrix_boundary', 'holographic_encoding', [('quantum_processor', 2), ('metamaterial_sheet', 2)], [], [('holographic_matrix', 1)], [],
     65536, 1200, 'ads_cft', 10, ['boundary_cft_chip']),
    ('hawking_calibrated', 'analog_horizon', [('minecraft:paper', 1)], [('bose_einstein_condensate', 100)], [('hawking_record', 2)], [], 2048, 1200,
     'unruh_effect', 25, ['unruh_thermometer']),
    # 1.14.2: a Poisson clock times the sampling; slices of a silicon boule become a stack of random number chips
    ('clocked_rng', 'monte_carlo', [('silicon_boule', 1)], [], [('random_number_chip', 8)], [], 2048, 400, 'monte_carlo_methods', 2,
     ['poisson_clock']),
    # 1.14.2: a burst's afterglow is evidence about its distance (in place of the pair-production shortcut to antimatter)
    ('posterior_burst', BA, [('prior_tablet', 1), ('burst_afterglow_crystal', 1)], [], [('posterior_tablet', 3)], [], 20000, 300,
     'gamma_ray_bursts', 3),
    ('moissanite_spectra', SP, [('minecraft:glass_pane', 2), ('minecraft:amethyst_shard', 1)], [], [('spectral_plate', 6)], [], 20000, 200,
     'geometric_optics', 0, ['moissanite_lens']),
]
WINDOWS = {
    'tectonic_press': {
        'r56_brown_dwarf_collapse': ({'Ea': 300000, 'Tr': 2300}, [('mB', 0.3, 100), ('Tf', 1800, 2600)]),
        'r56_quench_accretion_fragment': ({'Ea': 150000, 'Tr': 950}, [('Pe', 0.3, 1.2), ('Tf', 700, 1000)]),
        'r56_white_dwarf': ({'Ea': 400000, 'Tr': 2100}, [('mD', 0.5, 100), ('Tf', 1600, 2400)]),
        'r56_subducted_slab': ({'Ea': 300000, 'Tr': 2300}, [('mB', 0.3, 100), ('Tf', 1800, 2600)]),
    },
    'hypervelocity_impact_forge': {'r56_planetesimal_impact': ({}, [('P', 50, 90)])},
    'pulsed_magnet_lab': {'r56_magnetar_crust': ({}, [('Bf', 80, 99)])},
    'time_dilation_chamber': {'r56_tidal_shear_crystal': ({}, [('f', 3, 1e9)]), 'r56_tidal_plasma': ({}, [('f', 5, 1e9)])},
    'micro_black_hole_evaporator': {'r56_merger_remnant': ({'Pref': 10}, [])},
    'monte_carlo_engine': {'r56_clocked_rng': ({'sg': 10, 'opt': 0.5}, [('err', 0, 10)])},
    'laser_interferometer': {'r56_chirp_record': ({'hw': 1e-21}, [('SNR', 8, 1e12)])},
    'optical_workshop': {'r56_void_glass_lens': ({'n': 1.6}, [('Pw', -0.01, 0.01), ('e', 0, 20)])},
    'accretion_disk_reactor': {'r56_ember_accretion': ({'Lref': 8}, [('lam', 0.3, 0.95)])},
    'tokamak': {'r56_ism_plasma': ({'Lref': 2}, [('L3', 1, 1e9)])},
    'mhd_generator': {'r56_mhd_magnetized': ({'sg': 10000, 'rh': 0.01, 'Pref': 2e7}, [])},
    'analog_black_hole': {'r56_hawking_calibrated': ({}, [('mu', 0, 0.9), ('md', 1.2, 1e9), ('SNR', 5, 1e12)])},
}
for _m, _w in WINDOWS.items():
    _r12.WINDOWS.setdefault(_m, {}).update(_w)
ENV = {'cosmic_ray_glass': {'min_y': 200, 'dimensions': [M + 'accretion_frontier', 'minecraft:the_end']}}
# liquid sodium: the fast reactor's coolant - hot, but it carries heat away better than anything else
_r12.COOLANTS[M + 'liquid_sodium'] = dict(temperature=400, capacity=200, returns='', name='Liquid Sodium')

SHAPED = [
    ('hafnium_control_rod', ['H', 'H', 'I'], {'H': 'hafnium_dust', 'I': 'minecraft:iron_ingot'}, 'boron_control_rod', 2),
    ('cosmic_record_from_web', ['CWC'], {'C': 'minecraft:paper', 'W': 'cosmic_web_filament'}, 'cosmic_record_fragment', 4),
    ('merger_singularity', ['MM'], {'M': 'merger_remnant'}, 'micro_singularity', 1),
]
SHAPELESS = []

# the jet collimator now uses a plasma thruster's magnetic nozzle; the Whipple Shield is spacecraft hull
REPLACE_CRAFTING = {
    'r51_jet_collimator': (['MNM', 'M M', 'MNM'], {'M': 'neodymium_magnet', 'N': 'plasma_thruster_nozzle'}, 'jet_collimator', 2),
}

LORE = {
    'frontier_regolith': 'Rock ground to dust by billions of years of micrometeorites. Assayed in bulk it gives back its interstellar dust and '
                         'a little rare earth.',
    'nebular_ice': 'Water, carbon monoxide and ammonia frozen together at fifteen kelvin. Warmed in an Electrolytic Cell, it gives off '
                   'Molecular Cloud gas.',
    'asteroid_rock': 'The rubble of planetesimals that never became planets. A Spectrograph sorts its iron meteorites from its chondrules.',
    'pulsar_crust': 'Iron from the crust of a neutron star, magnetised by its field. Squeezed with iron-nickel under 80 tesla, it becomes '
                    'magnetar crust.',
    'rim_slag': 'Slag thrown off the inner edge of the accretion disk. Quenched with disk plasma in the Tectonic Press, it becomes Accretion '
                'Disk Fragments.',
    'void_glass': 'Glass of the Lensing Void, black and dense. Ground in the Optical Workshop, four make an Einstein Ring Lens.',
    'station_hull': 'Aluminium skin and stringers from a spacecraft. Whipple Shields and the Frontier Suit are made of it.',
    'dust_cloud': 'A cloud of dust grains, too thin to stand on. Now and then, broken, it leaves Interstellar Dust.',
    'hydrogen_cloud': 'Ionised hydrogen glowing red at 656 nm, the H-alpha line. You can walk straight through it.',
    'oxygen_cloud': 'Doubly ionised oxygen glowing green-blue at 501 nm. You can walk straight through it.',
    'protostar_fragment': 'A piece of a star not yet lit, buried in a nebula\'s ice. Mined, it gives a Protostellar Ember.',
}
BLOCK_PAGES = [
    # (block, where, uses)
    ('frontier_regolith', 'The top of every island of the Accretion Frontier (except the Lensing Void).',
     'The Spectrograph assays eight into four Interstellar Dust and a Rare Earth Oxide.'),
    ('nebular_ice', 'Patches in the Molecular Cloud Nebula, among its clouds.', 'Two sublimate into 250 mB of Molecular Cloud in the Electrolytic Cell.'),
    ('dust_cloud', 'The Molecular Cloud Nebula: you can pass through it.', 'Breaks into Interstellar Dust, sometimes.'),
    ('hydrogen_cloud', 'The Molecular Cloud Nebula: you can pass through it.', 'Light: it glows. Its gases will be gathered in a later expansion.'),
    ('oxygen_cloud', 'The Molecular Cloud Nebula: you can pass through it.', 'Light: it glows. Its gases will be gathered in a later expansion.'),
    ('protostar_fragment', 'Inside the Nebula\'s ice.', 'Gives a Protostellar Ember: thermoelectric fuel that burns for a long time, or feed for the '
                                                       'Accretion Disk Reactor.'),
    ('dust_ice_ore', 'The Nebula\'s ice.', 'Gives Interstellar Dust.'),
    ('asteroid_rock', 'The Asteroid Belt.', 'The Spectrograph sorts four into two Meteoric Iron and a Chondrite Fragment.'),
    ('planetesimal_core_ore', 'The Asteroid Belt\'s rock.', 'Gives Exoplanet Core Samples (also made in the Hypervelocity Impact Forge).'),
    ('brown_dwarf_ore', 'The Asteroid Belt\'s rock.', 'Gives Brown Dwarf Cores (also made in the Tectonic Press, from dust and cloud gas).'),
    ('pulsar_crust', 'The Pulsar Field.', 'Two with two Iron-Nickel become a Magnetar Crust Fragment in the Pulsed Magnet Lab, at 80 T.'),
    ('magnetar_crust_ore', 'The Pulsar Field\'s crust.', 'Gives Magnetar Crust Fragments.'),
    ('pulsar_timing_ore', 'The Pulsar Field\'s crust.', 'Gives Pulsar Timing Crystals.'),
    ('cosmic_ray_ore', 'The Pulsar Field\'s crust.', 'Gives Cosmic Ray Shards (also made by the Interstellar Scoop, from glass left high up).'),
    ('rim_slag', 'The Accretion Rim, round its plasma lakes.', 'Quenched with Accretion Disk Plasma in the Tectonic Press: Accretion Disk Fragments.'),
    ('accretion_fragment_ore', 'The Accretion Rim\'s slag.', 'Gives Accretion Disk Fragments.'),
    ('void_glass', 'The Lensing Void.', 'Four with Fused Silica make an Einstein Ring Lens in the Optical Workshop.'),
    ('einstein_ring_ore', 'The Lensing Void\'s glass.', 'Gives Einstein Ring Lenses.'),
    ('station_hull', 'Derelict Stations drifting in the Frontier; or crafted from iron.', 'The body of the Whipple Shield and the Frontier Suit.'),
]
LANG = {
    'message.theoreticalsciences.void_accretion_frontier': 'You fall off the edge of the Frontier - and the light sail\'s tether drags you home.',
    'transit.theoreticalsciences.planetary_interior.name': 'The Planetary Interior',
    'transit.theoreticalsciences.planetary_interior.flavour': 'Crust, mantle, and below: the rock grows hotter with every kilometre.',
    'transit.theoreticalsciences.planetary_interior.flavour_leaving': 'The layers fall away beneath you. Cool rock, then daylight.',
    'transit.theoreticalsciences.accretion_frontier.name': 'The Accretion Frontier',
    'transit.theoreticalsciences.accretion_frontier.flavour': 'Starlight presses on the sail. The stars crowd ahead, and something dark waits among them.',
    'transit.theoreticalsciences.accretion_frontier.flavour_leaving': 'The sail turns for home. The black hole shrinks behind you to a point.',
    'message.theoreticalsciences.binary_not_here': 'Nothing answers. Play it out in the Accretion Frontier.',
}


def _suit(g):
    A, LANG_, shaped = g['A'], g['LANG'], g['shaped']
    sid, body, mat, c1, c2, lore = SUIT
    LANG_[f'tooltip.{NS}.{sid}'] = lore
    for p, (name, pat, accent, plore) in PIECES.items():
        iid = f'{sid}_{p}'
        LANG_[f'item.{NS}.{iid}'] = name
        LANG_[f'item.{NS}.{iid}.lore'] = plore
        g['w'](os.path.join(A, 'models', 'item', iid + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{iid}'}})
        shaped('r56_' + iid, pat, {'M': accent, 'B': body}, iid, 1)
        slot = {'helmet': 'head', 'chestplate': 'chest', 'leggings': 'legs', 'boots': 'foot'}[p]
        kit.extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'item', f'{slot}_armor.json'), [f'{NS}:{iid}'])


def _recipes(g):
    for r in RECIPES:
        if r[0].startswith('rtg_'):
            # thermoelectric fuel: no research, no physics (the generator just burns it)
            g['mr']('r56_' + r[0], r[1], r[2], r[3], r[4], r[5], energy=r[6], duration=r[7])
        else:
            kit.recipes(g, 'r56_', [r])
    for name, env in ENV.items():
        typ = next(r[1] for r in RECIPES if r[0] == name)
        p = os.path.join(g['REC'], typ, 'r56_' + name + '.json')
        j = json.load(open(p))
        j['environment'] = env
        g['w'](p, j)
    kit.crafting(g, 'r56_', SHAPED, SHAPELESS)
    for name, (pat, key, res, n) in REPLACE_CRAFTING.items():
        g['shaped'](name, pat, key, res, n)
    # the gravitational-wave chain: a lithium enrichment needs ferrofluid seals; a warp core needs a geodesic compass to steer by
    for typ, name, edit in (('isotope_enrichment', 'r52_enrich_lithium', lambda j: j.setdefault('fluid_inputs', []).append(
            {'fluid': M + 'ferrofluid', 'amount': 50})),
                            ('warp_forging', 'r53_warp_bubble_core', lambda j: j.setdefault('catalysts', []).append({'item': M + 'geodesic_compass'}))):
        p = os.path.join(g['REC'], typ, name + '.json')
        j = json.load(open(p))
        edit(j)
        g['w'](p, j)


def _loot(g):
    """Clouds are mostly empty: with Silk Touch they come away whole; otherwise the gas ones leave nothing (yet), the dust
    one sometimes a pinch of dust."""
    D, w = g['D'], g['w']
    silk = {'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [
        {'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}
    for b in ('hydrogen_cloud', 'oxygen_cloud', 'dust_cloud'):
        pools = [{'rolls': 1, 'conditions': [silk], 'entries': [{'type': 'minecraft:item', 'name': M + b}]}]
        if b == 'dust_cloud':
            pools.append({'rolls': 1, 'conditions': [{'condition': 'minecraft:inverted', 'term': silk},
                                                     {'condition': 'minecraft:random_chance', 'chance': 0.25}],
                          'entries': [{'type': 'minecraft:item', 'name': M + 'interstellar_dust'}]})
        w(os.path.join(D, 'loot_table', 'blocks', b + '.json'), {'type': 'minecraft:block', 'pools': pools})


def _nebula(g):
    """The Molecular Cloud Nebula's islands become cloud: the top six blocks and the underside are Dust, Hydrogen and
    Oxygen Cloud in drifting patches (picked by two noises), round a heart of Nebular Ice, with ice breaking the surface
    here and there. Protostar Fragments glow inside both."""
    D, WG, w, q = g['D'], g['WG'], g['w'], g['q']
    p = os.path.join(WG, 'noise_settings', 'accretion_frontier.json')
    ns = json.load(open(p))

    def blk(b):
        return {'type': 'minecraft:block', 'result_state': {'Name': M + b}}

    def noise(n, lo, hi=1e9):
        return {'type': 'minecraft:noise_threshold', 'noise': n, 'min_threshold': lo, 'max_threshold': hi}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}

    clouds = {'type': 'minecraft:sequence', 'sequence': [
        cond(noise('minecraft:calcite', 0.45), blk('nebular_ice')),
        cond(noise('minecraft:gravel', 0.25), blk('hydrogen_cloud')),
        cond(noise('minecraft:gravel', -1e9, -0.25), blk('oxygen_cloud')),
        blk('dust_cloud')]}
    depth = lambda kind, off: {'type': 'minecraft:stone_depth', 'offset': off, 'surface_type': kind, 'add_surface_depth': False,
                               'secondary_depth_range': 0}
    rule = cond({'type': 'minecraft:biome', 'biome_is': [M + 'molecular_cloud_nebula']}, {'type': 'minecraft:sequence', 'sequence': [
        cond(depth('floor', 5), clouds), cond(depth('ceiling', 2), clouds), blk('nebular_ice')]})
    seq = [r for r in ns['surface_rule']['sequence'] if 'molecular_cloud_nebula' not in json.dumps(r.get('if_true', {}))]
    ns['surface_rule']['sequence'] = [rule] + seq
    w(p, ns)
    # climbable: inside a cloud you can hold jump to rise through it, as up a vine (and you never fall faster than a sink)
    kit.extend_tag(g, os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', 'climbable.json'),
                   [M + b for b in ('dust_cloud', 'hydrogen_cloud', 'oxygen_cloud')])
    kit.extend_tag(g, os.path.join(D, 'tags', 'block', 'nebula_hosts.json'), [M + b for b in ('nebular_ice', 'dust_cloud', 'hydrogen_cloud', 'oxygen_cloud')])
    g['ore_feature']('ore_protostar_fragment', {'predicate_type': 'minecraft:tag_match', 'tag': M + 'nebula_hosts'}, 'protostar_fragment', 4, 3, 0, 255)
    bp = os.path.join(WG, 'biome', 'molecular_cloud_nebula.json')
    b = json.load(open(bp))
    if M + 'ore_protostar_fragment' not in b['features'][6]:
        b['features'][6].append(M + 'ore_protostar_fragment')
    w(bp, b)


def apply(g):
    g['LANG'].update(LANG)
    for b, t in LORE.items():
        g['LANG'][f'block.{NS}.{b}.lore'] = t
    _recipes(g)
    _suit(g)


def apply_late(g):
    kit.pickaxe(g, [b for b in BLOCKS if b['kind'] != 'cloud'])
    _loot(g)
    _nebula(g)
    D = g['D']
    p = os.path.join(D, 'data_maps', 'item', 'radiation_source.json')
    t = json.load(open(p)) if os.path.exists(p) else {'values': {}}
    t['values'][M + 'protostellar_ember'] = {'isotope': 'u238_chain', 'strength': 0.0005}
    g['w'](p, t)


def finish(g):
    _unstack(g)
    te = g['text_entry']
    # these blocks get full pages below; drop the bare automatic ones written before this round ran
    import round5
    BA = os.path.join(g['BA'], 'entries')
    for b, _, _ in BLOCK_PAGES:
        for cat in ('world',):
            f = os.path.join(BA, cat, 'node_' + b + '.json')
            if os.path.exists(f):
                os.remove(f)
            round5.NODES.pop((cat, 'node_' + b), None)
    for i, (b, where, uses) in enumerate(BLOCK_PAGES):
        name = g['LANG'].get(f'block.{NS}.{b}', b)
        lore = LORE.get(b) or g['LANG'].get(f'block.{NS}.{b}.lore', '')
        te('world', 'r56_block_' + b, name, b, [
            f'$(item){name}$(). {lore}',
            f'$(thing)Found$(): {where}$(br2)$(thing)Used$(): {uses}'],
           adv='galactic_dynamics', sort=20 + i, spot=[b])
    te('world', 'r56_frontier_counters', 'Answering the Frontier', 'frontier_suit_helmet', [
        'Every region of the Accretion Frontier has its hazard, and each has an answer.',
        '$(li)$(thing)Nebula$() - blinding dust: the $(item)Adaptive Optics Visor$().$(li)$(thing)Asteroid Belt$() - micrometeorites: the '
        '$(item)Whipple Shield$() (a quarter per piece); low gravity: $(item)Gravity Compensators$().$(li)$(thing)Pulsar Field$() - its radiation '
        'pulse: $(item)Radiation Plating$() on your armour; high gravity: Gravity Compensators; Time Dilation: an $(item)Atomic Clock$() in the off '
        'hand.',
        '$(li)$(thing)Accretion Rim$() - heat: the $(item)Thermal Shroud$() (or a full Refractory Suit); its tidal pull: the $(item)Tidal Brace$() '
        '(or a Precision Gyroscope in the off hand).$(li)$(thing)Lensing Void$() - frame dragging: a $(item)Precision Gyroscope$() in the off '
        'hand; warped sight: the Adaptive Optics Visor.',
        'The four pieces of the $(item)Frontier Suit$() are made of Station Hull, each round a piece of the region it answers: an Einstein Ring '
        'Lens, an Accretion Disk Fragment, a Tidal Shear Crystal and a Planetary Core Sample (the reference mass the boots weigh gravity against).'],
       adv='galactic_dynamics', sort=9, spot=['frontier_suit_helmet', 'frontier_suit_chestplate', 'frontier_suit_leggings', 'frontier_suit_boots'])


def _unstack(g):
    """Two research nodes on one map cell (left over from 1.10-1.14): move the newer one to the nearest free cell."""
    import glob
    D = g['D']
    files = sorted(glob.glob(os.path.join(D, 'ts_research', '*.json')))
    nodes = {os.path.basename(f)[:-5]: json.load(open(f)) for f in files}
    seen = {}
    for rid, d in nodes.items():
        seen.setdefault((d['x'], d['y']), []).append(rid)
    taken = set(seen)
    for cell, rids in seen.items():
        if len(rids) < 2:
            continue
        movers = [r for r in rids if r in kit.KIT_IDS] or rids[1:]
        for rid in movers[: len(rids) - 1]:
            x, y = cell
            best = min(((abs(dx) + abs(dy), (x + dx, y + dy)) for dx in range(-4, 5) for dy in range(-4, 5)
                        if (x + dx, y + dy) not in taken and x + dx >= 0), key=lambda c: c[0])[1]
            taken.add(best)
            d = nodes[rid]
            d['x'], d['y'] = best
            g['w'](os.path.join(D, 'ts_research', rid + '.json'), d)
