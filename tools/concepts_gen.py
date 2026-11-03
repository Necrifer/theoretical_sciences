"""World generation and guidebook pages for the concept-field expansion. Called from gen_data.py with its globals."""
import json
import os


def worldgen(g):
    w, q, WG, D, LANG = g['w'], g['q'], g['WG'], g['D'], g['LANG']
    ore_feature, custom_feature, loot_chest, end = g['ore_feature'], g['custom_feature'], g['loot_chest'], g['end']
    NS = 'theoreticalsciences'
    bulk = {'predicate_type': 'minecraft:block_match', 'block': q('bulk_stone')}
    axiom = {'predicate_type': 'minecraft:block_match', 'block': q('axiom_stone')}

    # ------------------------------------------------------------------ the Bulk: three biomes, new ores, structures and natives
    ore_feature('ore_kaluza_klein', bulk, 'kaluza_klein_ore', 6, 10, 0, 160)
    ore_feature('ore_dilaton', bulk, 'dilaton_ore', 5, 14, 0, 160)
    ore_feature('ore_moduli', bulk, 'moduli_ore', 5, 14, 0, 160)
    surf = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]
    custom_feature('graviton_reef', 'graviton_reef', [{'type': 'minecraft:count', 'count': 3}] + surf)
    custom_feature('derelict_station', 'derelict_station', [{'type': 'minecraft:rarity_filter', 'chance': 36}] + surf)
    custom_feature('derelict_station_common', 'derelict_station', [{'type': 'minecraft:rarity_filter', 'chance': 18}] + surf)
    custom_feature('calabi_yau_knot', 'calabi_yau_knot', [{'type': 'minecraft:rarity_filter', 'chance': 22}] + surf)

    def feats(**steps):
        f = [[] for _ in range(11)]
        for step, names in steps.items():
            f[int(step[1:])] = [q(n) for n in names]
        return f

    def spawner(entity, weight, lo, hi):
        return {'type': entity if ':' in entity else q(entity), 'weight': weight, 'minCount': lo, 'maxCount': hi}

    biomes = {
        'the_bulk': dict(sky=0x140828, fog=0x0C0418, particle='minecraft:reverse_portal', p=0.004,
                         features=feats(s4=['bulk_monolith', 'derelict_station'], s6=['ore_void_lattice', 'ore_kaluza_klein'], s9=['crystal_spire']),
                         spawns=[spawner('brane_strider', 8, 1, 2),
                                 spawner('moduli_crawler', 5, 2, 4), spawner('graviton_wisp', 4, 1, 2)], name='The Bulk'),
        'graviton_reef': dict(sky=0x0A1030, fog=0x081028, particle='minecraft:portal', p=0.006,
                              features=feats(s6=['ore_void_lattice', 'ore_kaluza_klein', 'ore_dilaton'], s9=['graviton_reef', 'crystal_spire']),
                              spawns=[spawner('graviton_wisp', 12, 1, 3), spawner('brane_strider', 3, 1, 1)],
                              name='Graviton Reef'),
        'moduli_wastes': dict(sky=0x0A2020, fog=0x061414, particle='minecraft:mycelium', p=0.008,
                              features=feats(s4=['bulk_monolith', 'derelict_station_common', 'calabi_yau_knot'],
                                             s6=['ore_void_lattice', 'ore_kaluza_klein', 'ore_moduli']),
                              spawns=[spawner('moduli_crawler', 14, 3, 5), spawner('brane_strider', 6, 1, 2)],
                              name='Moduli Wastes'),
    }
    for bid, b in biomes.items():
        w(os.path.join(WG, 'biome', bid + '.json'), {
            'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
            'effects': {'sky_color': b['sky'], 'fog_color': b['fog'], 'water_color': 0x7A3CFF, 'water_fog_color': 0x1A0830,
                        'ambient_sound': 'minecraft:ambient.basalt_deltas.loop',
                        'particle': {'options': {'type': b['particle']}, 'probability': b['p']}},
            'spawners': {'monster': b['spawns']}, 'spawn_costs': {}, 'carvers': {}, 'features': b['features']})
        LANG[f'biome.{NS}.{bid}'] = b['name']

    def params(t0, t1):
        return {'temperature': [t0, t1], 'humidity': [-1.0, 1.0], 'continentalness': [-1.0, 1.0], 'erosion': [-1.0, 1.0],
                'weirdness': [-1.0, 1.0], 'depth': 0.0, 'offset': 0.0}
    w(os.path.join(D, 'dimension', 'the_bulk.json'), {'type': q('the_bulk'), 'generator': {
        'type': 'minecraft:noise', 'settings': q('the_bulk'), 'biome_source': {'type': 'minecraft:multi_noise', 'biomes': [
            {'biome': q('graviton_reef'), 'parameters': params(-1.0, -0.2)},
            {'biome': q('the_bulk'), 'parameters': params(-0.2, 0.25)},
            {'biome': q('moduli_wastes'), 'parameters': params(0.25, 1.0)}]}}})
    # the End's noise router has no climate; give the Bulk a temperature field so its biomes can vary
    bulk_ns = json.loads(json.dumps(end))
    bulk_ns['noise_router']['temperature'] = {'type': 'minecraft:shifted_noise', 'noise': 'minecraft:temperature', 'xz_scale': 0.25,
                                             'y_scale': 0.0, 'shift_x': 'minecraft:shift_x', 'shift_y': 0.0, 'shift_z': 'minecraft:shift_z'}
    w(os.path.join(WG, 'noise_settings', 'the_bulk.json'), bulk_ns)

    loot_chest('derelict_station', [('void_lattice_shard', 16, 2, 8), ('kaluza_klein_shard', 14, 2, 6), ('quantum_processor', 8, 1, 3),
                                    ('topological_processor', 3, 1, 1), ('superconducting_coil', 12, 2, 6), ('cryocooler_unit', 6, 1, 2),
                                    ('dipole_winding', 4, 1, 2), ('graviton_crystal', 4, 1, 2), ('brane_tension_fiber', 8, 1, 3),
                                    ('structure_assembler', 1, 1, 1), ('field_calibrator', 2, 1, 1)])
    loot_chest('calabi_yau_knot', [('moduli_crystal', 18, 2, 6), ('calabi_yau_manifold', 6, 1, 2), ('compactified_circle', 3, 1, 1),
                                   ('dilaton_crystal', 10, 1, 4), ('quantum_string', 8, 1, 4), ('graviton_wisp_core', 4, 1, 2),
                                   ('multiverse_shard', 2, 1, 1), ('void_lattice_shard', 10, 2, 6)])
    loot_chest('euclids_vault', [('prime_shard', 18, 4, 12), ('transcendental_crystal', 14, 2, 6), ('fractal_dust', 14, 4, 12),
                                 ('natural_number_lattice', 12, 4, 16), ('axiom_schema', 8, 1, 4), ('real_continuum', 4, 1, 2),
                                 ('function_graph', 4, 1, 2), ('golden_ratio_glass', 6, 4, 8), ('platonic_compass', 1, 1, 1)])

    # ------------------------------------------------------------------ the Platonic Realm
    ore_feature('ore_prime_number', axiom, 'prime_number_ore', 7, 16, 0, 200)
    ore_feature('ore_transcendental', axiom, 'transcendental_ore', 5, 10, 0, 200)
    ore_feature('ore_fractal', axiom, 'fractal_ore', 6, 10, 0, 200)
    custom_feature('platonic_solid', 'platonic_solid', [{'type': 'minecraft:rarity_filter', 'chance': 3}, {'type': 'minecraft:in_square'},
                                                        {'type': 'minecraft:biome'}])
    pf = feats(s4=['platonic_solid'], s6=['ore_prime_number', 'ore_transcendental', 'ore_fractal'])
    w(os.path.join(WG, 'biome', 'platonic_realm.json'), {
        'has_precipitation': False, 'temperature': 0.7, 'downfall': 0.0,
        'effects': {'sky_color': 0xC8D8FF, 'fog_color': 0xE8F0FF, 'water_color': 0xA0C0FF, 'water_fog_color': 0xC0D0FF,
                    'ambient_sound': 'minecraft:ambient.warped_forest.loop',
                    'particle': {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.002}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': pf})
    LANG[f'biome.{NS}.platonic_realm'] = 'Platonic Realm'
    pns = json.loads(json.dumps(end))
    pns['default_block'] = {'Name': q('axiom_stone')}
    pns['surface_rule'] = {'type': 'minecraft:block', 'result_state': {'Name': q('axiom_stone')}}
    pns['disable_mob_generation'] = True
    w(os.path.join(WG, 'noise_settings', 'platonic_realm.json'), pns)
    w(os.path.join(D, 'dimension_type', 'platonic_realm.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
        'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.45, 'logical_height': 256,
        'effects': 'minecraft:overworld', 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': 6000,
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'dimension', 'platonic_realm.json'), {'type': q('platonic_realm'), 'generator': {
        'type': 'minecraft:noise', 'settings': q('platonic_realm'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('platonic_realm')}}})


def book(g):
    text_entry = g['text_entry']
    text_entry('mathematics', 'math_overview', 'The Road to Infinity', 'natural_number_lattice', [
        'Absolute Infinity is no longer a shortcut. To build it you have to build mathematics: $(thing)numbers$(), $(thing)analysis$(), $(thing)number theory$(), $(thing)topology$(), $(thing)logic$(), $(thing)sets$() and $(thing)categories$() - each with its own machine and its own symbols.',
        'The $(item)Peano Arithmetic Engine$() builds number systems, the $(item)Analytic Continuator$() does calculus and analysis, the $(item)Formal Proof Assistant$() does logic, set theory and category theory, and the $(item)Cantor Diagonalizer$() takes it all to infinity.',
        'Finally, whole stacks of every branch are compressed into a $(item)Mathematics Singularity$(), which - with Surreal Numbers, Toposes and Continuum Shards - becomes $(item)Absolute Infinity$().'],
        adv='number_systems', sort=0, spot=['natural_number_lattice', 'complex_plane', 'absolute_infinity'])
    text_entry('mathematics', 'numbers', 'Number Systems', 'real_continuum', [
        'Each number system is built from the one before it.',
        '$(li)$(item)ℕ$() from qubits and quartz$(li)$(item)ℤ$() adds negatives (antimatter)$(li)$(item)ℚ$() adds division (Casimir plates)$(li)$(item)ℝ$() fills the gaps - with $(item)Transcendental Crystals$() from the Platonic Realm$(li)$(item)i$() from a tachyon\'s imaginary mass$(li)$(item)ℂ$() = ℝ + iℝ$(li)$(item)Quaternions$() and $(item)octonions$() give up commutativity, then associativity$(li)$(item)Surreal Numbers$() hold all of them, and every infinity.'],
        adv='number_systems', sort=1)
    text_entry('mathematics', 'analysis', 'Analysis & Calculus', 'integral_operator', [
        '$(item)Infinitesimals$() come from the reals. With a $(item)Function Graph$() they become $(item)Derivative$() and $(item)Integral Operators$(), and together those make $(item)Differential Manifolds$() - the stage relativity is written on.',
        'Complex analysis gives $(item)Fourier Series$() and $(item)Fractal Sets$(). Number theory needs $(item)Prime Shards$() - mined in the Platonic Realm, or sieved slowly from naturals - for $(item)Modular Forms$() and the $(item)Riemann Zeta Crystal$().'],
        adv='real_analysis', sort=2)
    text_entry('mathematics', 'logic_sets', 'Logic, Sets & Categories', 'topos', [
        'Start from $(item)Axiom Schemas$() and the $(item)Empty Set$() (Void Essence, or plenty of Void Lattice). Proofs, $(item)Power Sets$(), $(item)Aleph-Null$(), the $(item)Continuum$() and the $(item)Axiom of Choice$() follow - choice is what Banach and Tarski needed to double a sphere.',
        'Category theory sits on top: $(item)Category Lattices$(), $(item)Functor Bridges$() and finally the $(item)Topos$() - a universe of mathematics with its own logic.'],
        adv='formal_logic', sort=3)
    text_entry('mathematics', 'platonic_realm', 'The Platonic Realm', 'platonic_compass', [
        'Mathematical objects have to exist somewhere. The $(item)Platonic Compass$() takes you there: a bright realm of floating axiom stone where the five Platonic solids hang in the sky.',
        '$(item)Prime Number Ore$(), $(item)Transcendental Ore$() (π and e) and $(item)Fractal Ore$() grow here. Each solid is hollow, with $(thing)Euclid\'s Vault$() at its exact centre.'],
        adv='platonic_realm', sort=4, spot=['platonic_compass', 'prime_number_ore', 'transcendental_ore'])
    text_entry('exotic', 'standard_model', 'The Standard Model', 'standard_model_matrix', [
        'The $(item)Particle Spectrometer$() splits quark-gluon plasma into $(item)quark flavours$(), $(item)leptons$() and $(item)gauge bosons$(). The heaviest quarks and the $(item)Higgs$() need the Synchrotron.',
        'All of them together make the $(item)Standard Model Matrix$(). The Collider turns it and dark matter into $(item)Superpartner Shards$(), and the Tesseract Assembler unifies everything into a $(item)Grand Unified Core$().$(br2)The Higgs Catalyst now needs a real Higgs boson.'],
        adv='standard_model', sort=20, spot=['standard_model_matrix', 'higgs_boson_crystal', 'grand_unified_core'])
    text_entry('spacetime', 'relativity', 'Relativity', 'einstein_field_core', [
        'The $(item)Tensor Calculus Engine$() works up from $(item)Lorentz Transforms$() and the $(item)Minkowski Lattice$() to the $(item)Metric$() and $(item)Riemann Curvature Tensors$() and finally the $(item)Einstein Field Core$().',
        'Wormhole throats need an Einstein Field Core and Alcubierre coils need metric tensors. Kugelblitz chambers can merge black holes into $(item)Gravitational Wave Crystals$().'],
        adv='special_relativity', sort=20, spot=['lorentz_transform_crystal', 'metric_tensor', 'einstein_field_core'])
    text_entry('stellar', 'stellar_evolution', 'Stellar Evolution', 'protostar_core', [
        'Harvest $(fluid)Nebular Gas$() with the Dark Matter Halo Collector and collapse it into a $(item)Protostar$() in the $(item)Nucleosynthesis Chamber$(). Ignite it into a $(item)Stellar Core$(), let it swell into a $(item)Red Giant$() and keep its $(item)White Dwarf$().',
        'White dwarfs forge $(item)Stellar Iron$() - the end of fusion. A red giant crushed onto stellar iron goes supernova; its remnant feeds the $(item)r-process$() (gold and heavier) and is needed for every quark star. Dyson shells are made of stellar iron.'],
        adv='stellar_evolution', sort=20, spot=['protostar_core', 'stellar_iron_ingot', 'supernova_remnant'])
    text_entry('strings', 'compactification', 'Compactification', 'compactified_circle', [
        'The extra dimensions are mined in the Bulk: $(item)Kaluza-Klein$() everywhere, $(item)Dilaton$() on the Graviton Reefs, $(item)Moduli$() in the Moduli Wastes. Brane Striders drop $(item)Brane Tension Fiber$(); Graviton Wisps drop their cores.',
        'The $(item)Compactification Lathe$() turns them into $(item)Compactified Circles$(), $(item)D-Brane Sheets$() and - with an octonion - $(item)G₂ Manifolds$(). Membranes are now made from those, and the $(item)M-Theory Core$() unites all eleven dimensions.'],
        adv='compactification', sort=20, spot=['kaluza_klein_ore', 'dilaton_ore', 'moduli_ore'])
    text_entry('spacetime', 'temporal_mechanics', 'Temporal Mechanics', 'novikov_crystal', [
        'The $(item)Chrono Loom$() reverses the $(item)Arrow of Time$(), ties $(item)Temporal Knots$() (a Möbius strip helps) and crystallises the $(item)Novikov$() self-consistency principle.',
        'Closed timelike curves now need a Novikov Crystal: a time loop is only allowed if it can never produce a paradox.'],
        adv='temporal_mechanics', sort=21)
    text_entry('endgame', 'conceptual_singularities', 'Conceptual Singularities', 'mathematics_singularity', [
        'At the end of each field, the $(item)Conceptual Singularity Compressor$() takes whole stacks of everything it produced and compresses them into a $(item)Singularity$(): mathematics, the Standard Model, spacetime, stars, dimensions, time, quantum, cognition and exotic matter.',
        'Four singularities plus the field capstones make the $(item)Theory of Everything$(). That, the other five singularities, Absolute Infinity and the Omega Point fuse into the $(item)Omniscience Catalyst$().',
        'The catalyst and stacks of every late-game metal make the $(item)Omnium Ingot$() - and the $(item)Omnilex$() is forged from four of them. It costs everything, but nothing here waits on a timer.'],
        adv='conceptual_singularities', sort=10, spot=['mathematics_singularity', 'omniscience_catalyst', 'omnium_ingot'])
    text_entry('world', 'bulk_life', 'Life in the Bulk', 'kaluza_klein_ore', [
        'The Bulk now has regions: the open $(thing)Bulk$(), the crystal $(thing)Graviton Reefs$() and the $(thing)Moduli Wastes$(). $(thing)Derelict Stations$() hold salvage and supplies; $(thing)Collapsed Calabi-Yau Knots$() hang over the Wastes with caches at their hearts.',
        '$(thing)Graviton Wisps$() pull you toward them and crush you up close. $(thing)Brane Striders$() slip between branes when hurt and leave you seeing double. $(thing)Moduli Crawlers$() hunt in packs and climb anything.'],
        adv='brane_cosmology', sort=5, spot=['kaluza_klein_ore', 'dilaton_ore', 'moduli_ore'])
