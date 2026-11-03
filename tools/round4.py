"""
Round 4 expansion data:
  * the Quantum Foam's boundaries (Schrodinger's Limit below, Relativity Breakdown above) and its creatures
  * Quantum Mechanics and General Relativity concepts made from the two boundary fluids
  * the Transfinite Hierarchy: ordinals, cardinals and higher-dimensional objects (new dimension, machine, landmarks, natives)
  * inside a black hole / inside a white hole, joined by Einstein-Rosen bridges (both sides needed; a Geodesic Anchor to survive)
  * every dimension-only block now feeds a processing chain
"""
import json
import os


def _I(id, name, art, c1, c2, lore, glyph=None, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    if glyph:
        d['glyph'] = glyph
    d.update(kw)
    return d


ITEMS = [
    # ---- Quantum Mechanics, from Schrodinger's Limit
    _I('wavefunction', 'Wavefunction', 'glyph', '#102838', '#80f0ff', 'Everything a particle could be, written down at once.', 'Ψ'),
    _I('hamiltonian_operator', 'Hamiltonian Operator', 'glyph', '#102030', '#a0c0ff', 'Ask it how a system changes and it answers with the energy.', 'Ĥ'),
    _I('eigenstate_lattice', 'Eigenstate Lattice', 'lattice', '#102838', '#60ffe0', 'The states a measurement is allowed to find, and no others.'),
    _I('path_integral', 'Path Integral', 'glyph', '#201838', '#c0a0ff', 'Every way of getting from here to there, added up.', '∮'),
    # ---- General Relativity, from Relativity Breakdown
    _I('geodesic_thread', 'Geodesic Thread', 'string', '#2a1a08', '#ffc060', 'The straightest possible line - through curved spacetime.'),
    _I('christoffel_symbol', 'Christoffel Symbol', 'glyph', '#2a1808', '#ffb040', 'How the coordinates twist from one point to the next.', 'Γ'),
    _I('stress_energy_tensor', 'Stress-Energy Tensor', 'glyph', '#301008', '#ff8040', 'Matter tells spacetime how to curve. This is what it says.', 'T'),
    _I('einstein_rosen_core', 'Einstein-Rosen Core', 'throat', '#100810', '#ffffff', 'Both ends of a bridge between a black hole and a white hole, held in one hand.'),
    # ---- black hole / white hole
    _I('tidal_alloy_scrap', 'Tidal Alloy Scrap', 'chunk', '#300808', '#ff5020', 'Metal pulled thin by tides stronger than its bonds.'),
    _I('frozen_photon', 'Frozen Photon', 'crystal', '#201008', '#ffa040', 'Light that reached the horizon and stopped there, red-shifted to nothing.'),
    _I('retrocausal_crystal', 'Retrocausal Crystal', 'crystal', '#383020', '#fff4c0', 'It formed after you mined it.'),
    _I('primordial_ejecta', 'Primordial Ejecta', 'chunk', '#d8d0c0', '#ffffff', 'Matter a white hole threw out before there was anything to throw.'),
    _I('geodesic_anchor', 'Geodesic Anchor', 'anchor', '#201030', '#c080ff', 'Keeps your worldline your own. Without one, the inside of a black or white hole kills you in two seconds.', stack=1),
    _I('horizon_diver', 'Horizon Diver', 'shifter', '#200808', '#ff7030', 'Crosses an event horizon - inward. Use again to come back out, which should be impossible.', special='horizondiver', stack=1),
    # ---- the Transfinite Hierarchy
    _I('ordinal_shard', 'Ordinal Shard', 'shard', '#202048', '#ffd060', 'A piece of the order in which infinities come.'),
    _I('cardinal_shard', 'Cardinal Shard', 'shard', '#301030', '#ff80c0', 'A piece of how many there are - even when there are infinitely many.'),
    _I('hypersphere_fragment', 'Hypersphere Fragment', 'orb', '#102040', '#60e0ff', 'A curved piece of a sphere with a 3-dimensional surface.'),
    _I('omega_ordinal', 'Omega', 'glyph', '#202048', '#ffd060', 'The first number after all of the counting numbers.', 'ω'),
    _I('epsilon_naught', 'Epsilon-Naught', 'glyph', '#202048', '#ffe080', 'Omega to the omega to the omega... forever. And still countable.', 'ε₀'),
    _I('omega_one', 'Omega-One', 'glyph', '#180838', '#ffc0ff', 'The first ordinal that cannot be counted.', 'ω₁'),
    _I('aleph_one', 'Aleph-One', 'glyph', '#200830', '#ff80ff', 'The next size of infinity after the countable one.', 'ℵ₁'),
    _I('inaccessible_cardinal', 'Inaccessible Cardinal', 'glyph', '#100820', '#e0c0ff', 'An infinity so large nothing smaller can build it.', 'κ'),
    _I('hopf_fibration', 'Hopf Fibration', 'loop', '#102040', '#80f0ff', 'A 3-sphere unwound into circles, every two of them linked.'),
    _I('penteract_lattice', 'Penteract Lattice', 'tesseract', '#101830', '#80ffd0', 'A five-dimensional cube: 32 corners, 80 edges, 40 cells.'),
    _I('e8_lattice', 'E8 Lattice', 'hexgrid', '#181030', '#ffe0ff', 'The densest way to pack spheres in eight dimensions.'),
    _I('cantor_key', 'Cantor Key', 'shifter', '#202048', '#ffd060', 'Opens the Transfinite Hierarchy, where every infinity has its own place. Use again to return.', special='cantorkey', stack=1),
    # ---- natives' drops
    _I('false_vacuum_membrane', 'False Vacuum Membrane', 'brane', '#101828', '#6080c0', 'The skin of a Vacuum Drifter: the wall between two vacua.'),
    _I('hotel_key', "Hilbert's Hotel Key", 'chip', '#802020', '#ffd060', 'Opens room n+1. There is always a room n+1.'),
]

BLOCKS = [
    dict(id='ordinal_stone', name='Ordinal Stone', art='stars', c1='#1a1838', c2='#ffd060', kind='simple', light=2, hardness=4.0, tool='iron'),
    dict(id='hilbert_brick', name='Hilbert Brick', art='bricks', c1='#3a2a20', c2='#ffd060', kind='deco', light=0, hardness=4.0, tool='iron'),
    dict(id='aleph_glass', name='Aleph Glass', art='koch', c1='#c080ff', c2='#ffffff', kind='glass', light=12, hardness=2.0, tool='none'),
    dict(id='infall_rock', name='Infall Rock', art='stars', c1='#200808', c2='#ff5020', kind='simple', light=0, hardness=5.0, tool='iron'),
    dict(id='ejecta_rock', name='Ejecta Rock', art='polished', c1='#e8e0d0', c2='#ffffff', kind='simple', light=8, hardness=5.0, tool='iron'),
    dict(id='horizon_glass', name='Horizon Glass', art='fieldglass', c1='#200808', c2='#ff8040', kind='glass', light=6, hardness=2.0, tool='none'),
    dict(id='einstein_rosen_bridge', name='Einstein-Rosen Bridge', art='rift', c1='#000000', c2='#ffffff', kind='erb', light=15, hardness=5.0, tool='none'),
    dict(id='ordinal_ore', name='Ordinal Ore', art='ore', base='ordinal_stone', c1='#202048', c2='#ffd060', kind='ore', drops='ordinal_shard', light=6, hardness=4.0, tool='iron'),
    dict(id='cardinal_ore', name='Cardinal Ore', art='ore', base='ordinal_stone', c1='#301030', c2='#ff80c0', kind='ore', drops='cardinal_shard', light=6, hardness=4.5, tool='diamond'),
    dict(id='hyperspherical_ore', name='Hyperspherical Ore', art='ore', base='ordinal_stone', c1='#102040', c2='#60e0ff', kind='ore', drops='hypersphere_fragment', light=7, hardness=4.5, tool='diamond'),
    dict(id='tidal_ore', name='Tidal Ore', art='ore', base='infall_rock', c1='#300808', c2='#ff5020', kind='ore', drops='tidal_alloy_scrap', light=5, hardness=6.0, tool='diamond'),
    dict(id='frozen_light_ore', name='Frozen Light Ore', art='ore', base='infall_rock', c1='#201008', c2='#ffa040', kind='ore', drops='frozen_photon', light=9, hardness=6.0, tool='diamond'),
    dict(id='retrocausal_ore', name='Retrocausal Ore', art='ore', base='ejecta_rock', c1='#383020', c2='#fff4c0', kind='ore', drops='retrocausal_crystal', light=10, hardness=6.0, tool='diamond'),
    dict(id='ejecta_ore', name='Ejecta Ore', art='ore', base='ejecta_rock', c1='#a09080', c2='#ffffff', kind='ore', drops='primordial_ejecta', light=10, hardness=6.0, tool='diamond'),
]

FLUIDS = [
    dict(id='schrodinger_limit', name="Schrödinger's Limit", art='superfluid', c1='#40e0ff', c2='#e0ffff', gas=False, light=10, temp=0, density=1000, viscosity=4000),
    dict(id='relativity_breakdown', name='Relativity Breakdown', art='exotic', c1='#ff8040', c2='#fff0c0', gas=False, light=12, temp=0, density=1000, viscosity=4000),
]

MACHINE_TEXT = {'ordinal_recursor': ('Ordinal Recursor', 'Counts past infinity: takes a limit, adds one, and keeps going - '
                                                         'omega, omega plus one, omega times two, omega squared, epsilon-naught...')}
MACHINE_CAT = {'ordinal_recursor': 'mathematics'}
MACHINE_RESEARCH = {'ordinal_recursor': 'transfinite_hierarchy'}
TIER = {'ordinal_recursor': 5}
MC = {'ordinal_recursor': (['OXO', 'ASA', 'PCP'], {'O': 'ordinal_shard', 'X': 'quantum_processor', 'A': 'aleph_null_shard', 'S': 'hyperframe_casing',
                                                  'P': 'neutronium_plate', 'C': 'aleph_glass'})}
EXTRA = {'ordinal_recursor': 'Builds the ordinals from $(item)Ordinal Shards$() mined in the $(thing)Transfinite Hierarchy$(): '
                             'omega, epsilon-naught, omega-one - and the cardinals from them: aleph-one and the inaccessible cardinal that Absolute Infinity needs.'}

RESEARCH = [
    ('transfinite_hierarchy', 'The Transfinite Hierarchy', 'Ordinals count past infinity; cardinals measure it. Both have a place of their own - '
                                                          'with the objects of higher dimensions. Unlocks the Cantor Key and the Ordinal Recursor.',
     'cantor_key', 16, -2, 400, ['transfinite_mathematics'], [('aleph_null_shard', 2)]),
    ('black_hole_interior', 'Inside a Black Hole', 'What is past the horizon - and, through an Einstein-Rosen bridge, what is past the white hole\'s. '
                                                 'Unlocks the Horizon Diver and the Geodesic Anchor.', 'horizon_diver', 9, 7, 200,
     ['general_relativity'], [('kerr_metric_shard', 2)]),
]

CREATURES = {
    'virtual_pair': ('Virtual Pair', [('virtual_photon', 1, 2, 1.0), ('virtual_quark', 0, 1, 1.0)]),
    'planck_mite': ('Planck Mite', [('foam_substrate', 1, 3, 1.0), ('virtual_monopole', 1, 1, 0.25)]),
    'vacuum_drifter': ('Vacuum Drifter', [('false_vacuum_membrane', 1, 2, 1.0), ('virtual_graviton', 0, 2, 1.0)]),
    'hilbert_bellhop': ('Hilbert Bellhop', [('hotel_key', 1, 1, 1.0), ('ordinal_shard', 0, 2, 1.0)]),
    'ordinal_hound': ('Ordinal Hound', [('ordinal_shard', 1, 3, 1.0), ('cardinal_shard', 1, 1, 0.3)]),
    'hypercube_drifter': ('Hypercube Drifter', [('hypersphere_fragment', 1, 2, 1.0), ('tesseract_core', 1, 1, 0.05)]),
    'tidal_horror': ('Tidal Horror', [('tidal_alloy_scrap', 1, 3, 1.0), ('frozen_photon', 1, 1, 0.3)]),
    'photon_moth': ('Photon Moth', [('primordial_ejecta', 1, 2, 1.0), ('retrocausal_crystal', 1, 1, 0.2)]),
}

LANG = {
    'effect.theoreticalsciences.decoherence': 'Decoherence',
    'effect.theoreticalsciences.vacuum_decay': 'Vacuum Decay',
    'effect.theoreticalsciences.infinite_regress': 'Infinite Regress',
    'effect.theoreticalsciences.spaghettification': 'Spaghettification',
    'effect.theoreticalsciences.time_dilation': 'Time Dilation',
    'biome.theoreticalsciences.transfinite_strata': 'Transfinite Strata',
    'biome.theoreticalsciences.black_hole_interior': 'Inside the Black Hole',
    'biome.theoreticalsciences.white_hole_interior': 'Inside the White Hole',
    'message.theoreticalsciences.tidal_warning': 'Tidal forces are tearing you apart! You need a Geodesic Anchor!',
    'message.theoreticalsciences.black_hole_arrival': 'You are inside the black hole. An Einstein-Rosen bridge stands at 0, 121, 0 - it leads to the white hole.',
    'jei.theoreticalsciences.pleroma_each': 'Each fluid, %s mB',
    'jei.theoreticalsciences.info.schrodinger_limit': "Schrödinger's Limit is the floor of the Quantum Foam. Fill a cell from it - it regenerates at once and never flows. Processed into the concepts of quantum mechanics.",
    'jei.theoreticalsciences.info.relativity_breakdown': 'Relativity Breakdown is the ceiling of the Quantum Foam. Fill a cell from it - it regenerates at once and never flows. Processed into the concepts of general relativity.',
    'jei.theoreticalsciences.info.pleroma': 'Made in the Pleroma Confluence from 1,000,000 mB of every other fluid (water and lava included).',
}


def _add(a, b):
    return {'type': 'minecraft:add', 'argument1': a, 'argument2': b}


def _max(a, b):
    return {'type': 'minecraft:max', 'argument1': a, 'argument2': b}


def apply(g):
    import round3
    w, q, LANG, D, A, WG, REC = g['w'], g['q'], g['LANG'], g['D'], g['A'], g['WG'], g['REC']
    mr, shaped, shapeless, text_entry, loot_chest, ore_feature, custom_feature = (g['mr'], g['shaped'], g['shapeless'], g['text_entry'],
                                                                                  g['loot_chest'], g['ore_feature'], g['custom_feature'])
    end = g['end']
    NS = 'theoreticalsciences'
    LANG.update(globals()['LANG'])

    def spawner(e, wt, lo, hi):
        return {'type': q(e), 'weight': wt, 'minCount': lo, 'maxCount': hi}

    def feats(**steps):
        f = [[] for _ in range(11)]
        for step, names in steps.items():
            f[int(step[1:])] = [q(n) for n in names]
        return f

    def biome(bid, sky, fog, particle, prob, features, spawners, sound='minecraft:ambient.warped_forest.loop'):
        w(os.path.join(WG, 'biome', bid + '.json'), {
            'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
            'effects': {'sky_color': sky, 'fog_color': fog, 'water_color': 0x80F0FF, 'water_fog_color': fog, 'ambient_sound': sound,
                        'particle': {'options': {'type': particle}, 'probability': prob}},
            'spawners': spawners, 'spawn_costs': {}, 'carvers': {}, 'features': features})

    def dim_type(did, sky, light, effects, time):
        w(os.path.join(D, 'dimension_type', did + '.json'), {
            'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
            'has_skylight': sky, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': light, 'logical_height': 256,
            'effects': effects, 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': time,
            'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})

    def islands(did, block, layers, keep_centre=True):
        ns = json.loads(json.dumps(end))
        ns['noise']['height'] = 256
        ns['default_block'] = {'Name': q(block)}
        ns['surface_rule'] = {'type': 'minecraft:block', 'result_state': {'Name': q(block)}}
        ns['disable_mob_generation'] = False
        shape = None
        for args in layers:
            lay = round3.island_layer(*args)
            shape = lay if shape is None else _max(shape, lay)
        if keep_centre:
            shape = _max(ns['noise_router']['final_density']['argument']['argument2']['argument']['argument'], shape)
        ns['noise_router']['final_density'] = round3._wrap(shape)
        w(os.path.join(WG, 'noise_settings', did + '.json'), ns)
        w(os.path.join(D, 'dimension', did + '.json'), {'type': q(did), 'generator': {
            'type': 'minecraft:noise', 'settings': q(did), 'biome_source': {'type': 'minecraft:fixed', 'biome': q(BIOME[did])}}})

    BIOME = {'transfinite_hierarchy': 'transfinite_strata', 'black_hole_interior': 'black_hole_interior', 'white_hole_interior': 'white_hole_interior'}
    for nid, octave in (('strata_a', -7), ('strata_b', -6), ('strata_c', -6), ('strata_d', -5), ('debris_a', -6), ('debris_b', -5), ('ejecta_a', -6), ('ejecta_b', -5)):
        w(os.path.join(WG, 'noise', nid + '.json'), {'firstOctave': octave, 'amplitudes': [1.0, 1.0, 0.5]})

    # ================================================================== the Quantum Foam: boundaries and natives
    custom_feature('foam_boundary_layers', 'foam_boundary_layers', [])
    fb = json.load(open(os.path.join(WG, 'biome', 'quantum_foam.json')))
    fb['features'][0] = [q('foam_boundary_layers')]
    fb['spawners'] = {'monster': [spawner('planck_mite', 10, 2, 4), spawner('vacuum_drifter', 5, 1, 2)],
                      'ambient': [spawner('virtual_pair', 12, 2, 4)]}
    w(os.path.join(WG, 'biome', 'quantum_foam.json'), fb)
    fns = json.load(open(os.path.join(WG, 'noise_settings', 'quantum_foam.json')))
    fns['disable_mob_generation'] = False
    w(os.path.join(WG, 'noise_settings', 'quantum_foam.json'), fns)

    # ================================================================== the Transfinite Hierarchy
    o = {'predicate_type': 'minecraft:block_match', 'block': q('ordinal_stone')}
    ore_feature('ore_ordinal', o, 'ordinal_ore', 8, 14, 0, 256)
    ore_feature('ore_cardinal', o, 'cardinal_ore', 6, 8, 0, 256)
    ore_feature('ore_hyperspherical', o, 'hyperspherical_ore', 5, 7, 0, 256)
    for fid, rarity in (('hilbert_hotel', 14), ('tesseract_frame', 12), ('hopf_rings', 14)):
        custom_feature(fid, fid, [{'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}])
    custom_feature('ordinal_spire', 'ordinal_spire', [{'type': 'minecraft:rarity_filter', 'chance': 9}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}])
    biome('transfinite_strata', 0x0A0820, 0x140C30, 'minecraft:enchant', 0.01,
          feats(s4=['hilbert_hotel', 'tesseract_frame', 'hopf_rings', 'ordinal_spire'], s6=['ore_ordinal', 'ore_cardinal', 'ore_hyperspherical']),
          {'monster': [spawner('ordinal_hound', 10, 1, 3)], 'creature': [spawner('hilbert_bellhop', 8, 1, 2)],
           'ambient': [spawner('hypercube_drifter', 10, 1, 2)]})
    # strata: four layers of islands, each higher layer sparser - omega, omega+1, omega*2, omega^2
    islands('transfinite_hierarchy', 'ordinal_stone', [(q('strata_a'), 1.7, -0.45, 10, 46, 50, 60, 0.4),
                                                      (q('strata_b'), 1.7, -0.65, 70, 92, 96, 104, 0.35),
                                                      (q('strata_c'), 1.8, -0.85, 124, 140, 143, 150, 0.3),
                                                      (q('strata_d'), 2.0, -1.05, 176, 186, 188, 194, 0.2)])
    dim_type('transfinite_hierarchy', False, 0.4, q('transfinite_hierarchy'), 18000)
    loot_chest('hilbert_hotel', [('hotel_key', 10, 1, 2), ('ordinal_shard', 16, 2, 8), ('cardinal_shard', 8, 1, 3), ('aleph_null_shard', 4, 1, 2),
                                 ('natural_number_lattice', 12, 8, 32), ('power_set', 4, 1, 2), ('hypersphere_fragment', 6, 1, 3)])
    loot_chest('ordinal_spire', [('omega_ordinal', 8, 1, 2), ('epsilon_naught', 3, 1, 1), ('ordinal_shard', 16, 4, 12), ('cardinal_shard', 10, 2, 5),
                                 ('hopf_fibration', 2, 1, 1), ('continuum_shard', 2, 1, 1)])

    # ================================================================== inside the black hole / the white hole
    ir = {'predicate_type': 'minecraft:block_match', 'block': q('infall_rock')}
    er = {'predicate_type': 'minecraft:block_match', 'block': q('ejecta_rock')}
    ore_feature('ore_tidal', ir, 'tidal_ore', 7, 12, 20, 256)
    ore_feature('ore_frozen_light', ir, 'frozen_light_ore', 6, 10, 20, 256)
    ore_feature('ore_retrocausal', er, 'retrocausal_ore', 6, 10, 0, 256)
    ore_feature('ore_ejecta', er, 'ejecta_ore', 7, 12, 0, 256)
    biome('black_hole_interior', 0x100404, 0x200806, 'minecraft:ash', 0.02, feats(s6=['ore_tidal', 'ore_frozen_light']),
          {'monster': [spawner('tidal_horror', 10, 1, 2)]}, 'minecraft:ambient.basalt_deltas.loop')
    biome('white_hole_interior', 0xFFF8F0, 0xFFF4E8, 'minecraft:end_rod', 0.02, feats(s6=['ore_retrocausal', 'ore_ejecta']),
          {'ambient': [spawner('photon_moth', 14, 2, 4)]})
    # debris falling inward: islands only well above the singularity
    islands('black_hole_interior', 'infall_rock', [(q('debris_a'), 1.6, -0.55, 40, 70, 74, 88, 0.6),
                                                  (q('debris_b'), 1.8, -0.8, 110, 140, 144, 156, 0.5)], keep_centre=False)
    islands('white_hole_interior', 'ejecta_rock', [(q('ejecta_a'), 1.6, -0.55, 30, 64, 68, 80, 0.5),
                                                  (q('ejecta_b'), 1.8, -0.8, 100, 130, 134, 146, 0.4)], keep_centre=False)
    dim_type('black_hole_interior', False, 0.12, q('black_hole_interior'), 18000)
    dim_type('white_hole_interior', True, 0.9, q('white_hole_interior'), 6000)
    for aid, title, desc, icon, dim in (('transfinite_arrival', 'Past Every Finite Step', 'Enter the Transfinite Hierarchy', 'cantor_key', 'transfinite_hierarchy'),
                                        ('black_hole_arrival', 'Past the Horizon', 'Enter a black hole', 'horizon_diver', 'black_hole_interior'),
                                        ('white_hole_arrival', 'Out the Other Side', 'Cross an Einstein-Rosen bridge into a white hole', 'einstein_rosen_core', 'white_hole_interior')):
        w(os.path.join(D, 'advancement', aid + '.json'), {
            'display': {'icon': {'id': q(icon)}, 'title': title, 'description': desc, 'frame': 'challenge', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
            'parent': 'minecraft:end/root', 'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q(dim)}}},
            'requirements': [['arrived']]})

    # ================================================================== recipes
    shaped('cantor_key', ['OAO', 'PSP', 'OAO'], {'O': 'natural_number_lattice', 'A': 'aleph_null_shard', 'P': 'power_set', 'S': 'brane_shifter'}, 'cantor_key')
    shaped('horizon_diver', ['HMH', 'MSM', 'HMH'], {'H': 'hawking_absorber_plate', 'M': 'micro_singularity', 'S': 'brane_shifter'}, 'horizon_diver')
    shaped('geodesic_anchor', ['NGN', 'KCK', 'NGN'], {'N': 'neutronium_plate', 'G': 'graviton_crystal', 'K': 'kerr_metric_shard', 'C': 'chronon_capacitor'}, 'geodesic_anchor')
    shaped('einstein_rosen_bridge', ['HTH', 'TCT', 'HTH'], {'H': 'horizon_glass', 'T': 'tidal_alloy_scrap', 'C': 'einstein_rosen_core'}, 'einstein_rosen_bridge')
    # Quantum Mechanics (Analytic Continuator, with Schrodinger's Limit)
    S = 'schrodinger_limit'
    mr('wavefunction', 'analysis', [('complex_plane', 1), ('qubit', 2), ('foam_substrate', 4)], [(S, 500)], [('wavefunction', 2)], [], 65536, 300, 'quantum_foam')
    mr('hamiltonian_operator', 'analysis', [('wavefunction', 2), ('derivative_operator', 1)], [(S, 500)], [('hamiltonian_operator', 1)], [], 131072, 300, 'quantum_foam')
    mr('eigenstate_lattice', 'analysis', [('hamiltonian_operator', 1), ('spinor_lattice', 1), ('virtual_photon', 2)], [(S, 500)], [('eigenstate_lattice', 2)], [],
       131072, 300, 'quantum_foam')
    mr('path_integral', 'analysis', [('hamiltonian_operator', 2), ('integral_operator', 2), ('virtual_quark', 4)], [(S, 1000)], [('path_integral', 1)], [],
       262144, 400, 'quantum_foam')
    # General Relativity (Chrono Loom, with Relativity Breakdown)
    RB = 'relativity_breakdown'
    mr('geodesic_thread', 'chronoweaving', [('neutronium_plate', 2), ('real_continuum', 1)], [(RB, 500)], [('geodesic_thread', 2)], [],
       65536, 300, 'black_hole_interior')
    mr('christoffel_symbol', 'chronoweaving', [('geodesic_thread', 2), ('derivative_operator', 2), ('koch_crystal', 1)], [(RB, 500)], [('christoffel_symbol', 1)], [],
       131072, 300, 'black_hole_interior')
    mr('stress_energy_tensor', 'chronoweaving', [('christoffel_symbol', 2), ('stellar_core', 1), ('frozen_photon', 2)], [(RB, 1000)], [('stress_energy_tensor', 1)], [],
       262144, 400, 'black_hole_interior')
    mr('einstein_rosen_core', 'hyperdimensional', [('tidal_alloy_scrap', 8), ('retrocausal_crystal', 8), ('frozen_photon', 4), ('primordial_ejecta', 4)], [(RB, 1000)],
       [('einstein_rosen_core', 1)], [], 4194304, 800, 'black_hole_interior')
    # the Transfinite Hierarchy (Ordinal Recursor, Cantor Diagonalizer, Tesseract Assembler)
    OR, T, H = 'ordinal_recursion', 'transfinite_computation', 'hyperdimensional'
    mr('omega_ordinal', OR, [('ordinal_shard', 8), ('natural_number_lattice', 16), ('ordinal_stone', 4)], [], [('omega_ordinal', 1)], [], 262144, 400, 'transfinite_hierarchy')
    mr('epsilon_naught', OR, [('omega_ordinal', 4), ('power_set', 1), ('ordinal_shard', 16)], [('dark_energy', 500)], [('epsilon_naught', 1)], [], 524288, 600, 'transfinite_hierarchy')
    mr('omega_one', OR, [('epsilon_naught', 2), ('aleph_null_shard', 1), ('cardinal_shard', 8)], [('dark_energy', 1000)], [('omega_one', 1)], [], 1048576, 800, 'transfinite_hierarchy')
    mr('aleph_one', OR, [('omega_one', 1), ('cardinal_shard', 16), ('real_continuum', 4)], [('synaptic_fluid', 1000)], [('aleph_one', 1)], [], 1048576, 800, 'transfinite_hierarchy')
    mr('inaccessible_cardinal', T, [('aleph_one', 2), ('topos', 2), ('cardinal_shard', 32), ('hotel_key', 1)], [('dark_energy', 4000)], [('inaccessible_cardinal', 1)], [],
       8388608, 1600, 'transfinite_hierarchy')
    mr('hopf_fibration', H, [('hypersphere_fragment', 4), ('quaternion_crystal', 2), ('compactified_circle', 4), ('aleph_glass', 2)], [('exotic_matter', 500)],
       [('hopf_fibration', 1)], [], 2097152, 600, 'transfinite_hierarchy')
    mr('penteract_lattice', H, [('tesseract_core', 2), ('hypersphere_fragment', 8), ('hopf_fibration', 1)], [('exotic_matter', 1000)], [('penteract_lattice', 1)], [],
       4194304, 800, 'transfinite_hierarchy')
    mr('e8_lattice', H, [('penteract_lattice', 2), ('octonion_crystal', 4), ('hopf_fibration', 2), ('g2_manifold', 1)], [('exotic_matter', 2000)], [('e8_lattice', 1)], [],
       8388608, 1200, 'transfinite_hierarchy')

    # ---- the new concepts are now required by the old chains
    C, E = 'concept_compression', 268435456
    mr('quantum_singularity', C, [('wavefunction', 32), ('superposition_crystal', 64), ('entangled_photon_pair', 64), ('wave_packet', 64), ('spinor_lattice', 32),
                                  ('quantum_field_lattice', 16), ('feynman_diagram', 16), ('quantum_processor', 64), ('eigenstate_lattice', 16)],
       [('bose_einstein_condensate', 64000), ('superfluid_helium', 64000), ('photoresist', 32000)], [('quantum_singularity', 1)], [], E, 2400, 'conceptual_singularities')
    mr('spacetime_singularity', C, [('lorentz_transform_crystal', 64), ('minkowski_lattice', 32), ('metric_tensor', 32), ('riemann_curvature_tensor', 32), ('einstein_field_core', 16),
                                    ('stress_energy_tensor', 16), ('gravitational_wave_crystal', 32), ('wormhole_throat', 4), ('einstein_rosen_core', 4)],
       [('exotic_matter', 64000), ('hawking_radiation', 32000), ('liquid_spacetime', 16000)], [('spacetime_singularity', 1)], [], E, 2400, 'conceptual_singularities')
    mr('mathematics_singularity', C, [('natural_number_lattice', 64), ('complex_plane', 32), ('octonion_crystal', 16), ('riemann_zeta_crystal', 16), ('fractal_set', 32),
                                      ('epsilon_naught', 8), ('klein_bottle', 16), ('power_set', 32), ('formal_proof_tree', 64)],
       [('synaptic_fluid', 32000), ('dark_energy', 32000), ('bose_einstein_condensate', 32000)], [('mathematics_singularity', 1)], [], E, 2400, 'conceptual_singularities')
    mr('dimensional_singularity', C, [('kaluza_klein_shard', 64), ('dilaton_crystal', 64), ('moduli_crystal', 64), ('d_brane_sheet', 32), ('penteract_lattice', 8),
                                      ('calabi_yau_manifold', 16), ('g2_manifold', 16), ('m_brane', 16), ('m_theory_core', 4)],
       [('exotic_matter', 64000), ('inflaton_field', 32000), ('dimensional_residue', 32000)], [('dimensional_singularity', 1)], [], E, 2400, 'conceptual_singularities')
    mr('theory_of_everything', C, [('standard_model_singularity', 1), ('dimensional_singularity', 1), ('spacetime_singularity', 1), ('quantum_singularity', 1),
                                   ('grand_unified_core', 4), ('m_theory_core', 4), ('einstein_field_core', 8), ('e8_lattice', 2), ('path_integral', 8)],
       [('exotic_matter', 64000), ('dark_energy', 64000), ('quark_gluon_plasma', 64000)], [('theory_of_everything', 1)], [], E * 2, 4800, 'theory_of_everything')
    mr('feynman_diagram', 'formal_proof', [('quantum_field_lattice', 1), ('gauge_boson_cluster', 1), ('integral_operator', 2), ('wavefunction', 1)], [], [('feynman_diagram', 2)],
       [], 16384, 400, 'quantum_field_theory')
    mr('continuum_shard', T, [('aleph_null_shard', 2), ('real_continuum', 16), ('power_set', 4), ('omega_one', 1)], [('dark_energy', 1000)], [('continuum_shard', 1)], [],
       524288, 800, 'transfinite_mathematics')
    mr('absolute_infinity', T, [('mathematics_singularity', 1), ('surreal_number', 4), ('inaccessible_cardinal', 2), ('continuum_shard', 8)], [('dark_energy', 16000)],
       [('absolute_infinity', 1)], [], 67108864, 2400, 'transfinite_mathematics')

    # ---- every dimension-only block feeds a chain
    mr('bulk_stone_sorting', 'entropy_sorting', [('bulk_stone', 16)], [('superfluid_helium', 250)], [('void_lattice_shard', 2), ('kaluza_klein_shard', 1)], [],
       65536, 300, 'brane_cosmology', chances=[1.0, 0.15])
    mr('bulk_crystal_compactification', 'compactification', [('bulk_crystal', 4), ('compactified_circle', 1)], [('exotic_matter', 250)], [('string_coupling_crystal', 2)],
       [], 131072, 300, 'compactification')
    mr('axiom_stone_counting', 'arithmetic', [('axiom_stone', 8), ('qubit', 1)], [], [('natural_number_lattice', 6), ('axiom_schema', 1)], [], 1024, 200, 'platonic_realm')
    mr('golden_ratio_continuum', 'analysis', [('golden_ratio_glass', 4), ('rational_field', 2)], [('bose_einstein_condensate', 250)], [('real_continuum', 1)], [],
       8192, 300, 'real_analysis')
    mr('koch_fractal_set', 'analysis', [('koch_crystal', 4), ('complex_plane', 2), ('function_graph', 1)], [], [('fractal_set', 2)], [], 4096, 300, 'complex_analysis')
    mr('fractal_lattice_dust', 'planck_cutting', [('fractal_lattice_block', 1)], [('superfluid_helium', 100)], [('fractal_dust', 6)], [], 2048, 100, 'platonic_realm')
    mr('foam_superposition', 'entangling', [('foam_substrate', 4), ('qubit', 1)], [], [('superposition_crystal', 3)], [], 2048, 200, 'quantum_foam')
    mr('false_vacuum_from_membrane', 'condensing', [('false_vacuum_membrane', 1)], [('superfluid_helium', 500)], [], [('false_vacuum', 1000)], 65536, 200, 'quantum_foam')
    mr('ordinal_stone_cutting', 'planck_cutting', [('ordinal_stone', 4)], [('superfluid_helium', 100)], [('ordinal_shard', 1)], [], 65536, 200, 'transfinite_hierarchy')
    mr('infall_rock_crushing', 'stellar_compression', [('infall_rock', 16), ('neutronium_plate', 1)], [('stellar_plasma', 250)], [('degenerate_matter_chunk', 8), ('frozen_photon', 1)],
       [], 524288, 300, 'black_hole_interior', chances=[1.0, 0.1])
    mr('ejecta_white_hole', 'white_hole', [('ejecta_rock', 16), ('primordial_ejecta', 2)], [('hawking_radiation', 500)], [('white_hole_fragment', 1)], [],
       1048576, 400, 'black_hole_interior')
    mr('horizon_glass_lens', 'weaving', [('horizon_glass', 4), ('metamaterial_sheet', 1)], [('dark_matter', 250)], [('dark_matter_lens', 2)], [], 65536, 300, 'black_hole_interior')
    mr('aleph_glass_power_set', 'formal_proof', [('aleph_glass', 2), ('empty_set', 2), ('natural_number_lattice', 4), ('formal_proof_tree', 1)], [], [('power_set', 2)],
       [], 8192, 300, 'transfinite_hierarchy')
    mr('hilbert_brick_axioms', 'formal_proof', [('hilbert_brick', 4), ('natural_number_lattice', 2)], [], [('axiom_schema', 4)], [], 2048, 200, 'transfinite_hierarchy')

    # ================================================================== guide
    text_entry('quantum', 'foam_boundaries', "The Foam's Edges", 'schrodinger_limit_cell', [
        "The Quantum Foam has a floor and a ceiling you cannot pass. Below: $(item)Schrödinger's Limit$(), where every possibility is still undecided. Above: $(item)Relativity Breakdown$(), where spacetime stops being smooth.",
        'Both are liquids that will not flow and cannot be crossed. Fill a cell from either and it is still there - they regenerate at once.',
        "Schrödinger's Limit becomes the concepts of quantum mechanics in the $(item)Analytic Continuator$(): $(item)Wavefunctions$(), the $(item)Hamiltonian$(), $(item)Eigenstates$(), the $(item)Path Integral$(). Relativity Breakdown becomes $(item)Geodesics$(), $(item)Christoffel Symbols$() and the $(item)Stress-Energy Tensor$() in the $(item)Chrono Loom$().",
        'The Foam is alive: $(thing)Virtual Pairs$() drift harmlessly, $(thing)Vacuum Drifters$() only fight back (their sting is $(thing)Vacuum Decay$(): no healing) and $(thing)Planck Mites$() hunt in packs (their bite $(thing)decoheres$() you).'],
        adv='quantum_foam', sort=9, spot=['schrodinger_limit_cell', 'relativity_breakdown_cell'])
    text_entry('mathematics', 'transfinite', 'The Transfinite Hierarchy', 'cantor_key', [
        'Infinity comes in kinds. The $(item)Cantor Key$() opens a dimension built in strata - each layer of islands higher and sparser than the last - where the ordinals and cardinals are mined.',
        '$(item)Ordinal Shards$() become $(item)Omega$(), $(item)Epsilon-Naught$() and $(item)Omega-One$() in the $(item)Ordinal Recursor$(); $(item)Cardinal Shards$() become $(item)Aleph-One$() and the $(item)Inaccessible Cardinal$() that Absolute Infinity now needs.',
        'Higher dimensions live here too: $(item)Hypersphere Fragments$() make the $(item)Hopf Fibration$(), the $(item)Penteract$() and the $(item)E8 Lattice$(). Look for $(thing)Hilbert\'s Hotel$(), $(thing)Ordinal Spires$(), $(thing)Tesseract Frames$() and $(thing)Hopf Rings$().',
        'Natives: $(thing)Hilbert Bellhops$() (harmless; they carry the hotel\'s keys), $(thing)Hypercube Drifters$() (harmless) and $(thing)Ordinal Hounds$(), whose bites stack $(thing)Infinite Regress$() - every hit after makes you take more.'],
        adv='transfinite_hierarchy', sort=6, spot=['cantor_key', 'omega_ordinal', 'ordinal_recursor'])
    text_entry('spacetime', 'black_hole_interior', 'Inside a Black Hole', 'horizon_diver', [
        'The $(item)Horizon Diver$() takes you through an event horizon. $(thing)Carry a Geodesic Anchor$(): without one, tidal forces kill you within two seconds - inside the black hole and the white hole alike.',
        'Even anchored, time is dilated (you move, swing and dig slower), gravity is 2.5 times as strong, and below y=16 the singularity tears everything apart. $(thing)Tidal Horrors$() hunt here; their touch $(thing)spaghettifies$().',
        'An $(item)Einstein-Rosen Bridge$() stands at 0, 121, 0. Step through and you are inside the $(thing)white hole$(): low gravity, blinding light, everything pushed outward, and harmless $(thing)Photon Moths$().',
        'Both sides are needed: $(item)Tidal Alloy$() and $(item)Frozen Photons$() from the black hole, $(item)Retrocausal Crystals$() and $(item)Primordial Ejecta$() from the white hole make the $(item)Einstein-Rosen Core$() - and with it, new bridges you can build yourself.'],
        adv='black_hole_interior', sort=6, spot=['horizon_diver', 'geodesic_anchor', 'einstein_rosen_core'])
