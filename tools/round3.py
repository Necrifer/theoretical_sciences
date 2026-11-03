"""
Round 3 expansion data:
  * the Quantum Foam dimension (ephemeral virtual-particle ores, foam membranes) and the Vacuum Fluctuation Harvester
  * the Platonic Realm rebuilt as fractal space (self-similar archipelago, fractal landmarks, Fractal Wyrms, Axiom Sentinels)
  * the Bulk opened up into an archipelago instead of a single island
  * display miniatures (black hole, tesseract, ...), the Pleroma Confluence, the Omnium Singularity
  * the impossible recipe and the Transcendental dimension with Omnipotency
"""
import json
import os
import shapes


def _I(id, name, art, c1, c2, lore, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


VIRTUAL = [('virtual_quark', 'Virtual Quark', '#401020', '#ff7090', 'A quark borrowed from the vacuum. It wants to go back.'),
           ('virtual_graviton', 'Virtual Graviton', '#101838', '#90a8ff', 'A quantum of gravity that has not yet decided to exist.'),
           ('virtual_photon', 'Virtual Photon', '#383010', '#fff090', 'Light that never left anything and will never arrive.'),
           ('virtual_monopole', 'Virtual Monopole', '#203020', '#90ffb0', 'A magnet with one pole. The vacuum only lends these briefly.')]

ITEMS = [_I(i, n, 'ghostorb', c1, c2, lore) for i, n, c1, c2, lore in VIRTUAL] + [
    _I('planck_diver', 'Planck Diver', 'shifter', '#103040', '#80f0ff', 'Shrinks you past the Planck length into the Quantum Foam. Use again to come back.', special='foamdiver', stack=1),
    _I('resonance_probe', 'Resonance Probe', 'analyzer', '#102838', '#80e0ff', 'Reads the four virtual-particle resonances where you stand in the Quantum Foam.', special='probe', stack=1),
    _I('omnium_singularity', 'Omnium Singularity', 'singularity', '#101010', '#ffffff', 'Two hundred and fifty-six Omnium Ingots in one point. There is nothing left to compress it into.', stack=1),
    _I('fractal_core', 'Fractal Core', 'crystal', '#203050', '#ffd060', 'The smallest copy of a Fractal Wyrm. It is also the largest.'),
    _I('proof_fragment', 'Proof Fragment', 'shard', '#d0d8f0', '#60a0ff', 'Part of a proof an Axiom Sentinel was writing about you. QED.'),
]

BLOCKS = [
    dict(id='fractal_lattice_block', name='Fractal Lattice Block', art='fractal', c1='#d8dcec', c2='#e0b040', kind='deco', light=4, hardness=4.0, tool='iron'),
    dict(id='koch_crystal', name='Koch Crystal', art='koch', c1='#a0c8ff', c2='#ffffff', kind='glass', light=10, hardness=2.0, tool='none'),
    dict(id='foam_substrate', name='Foam Substrate', art='foam', c1='#3a6080', c2='#a0e8ff', kind='foam', light=3, hardness=1.5, tool='none'),
    dict(id='foam_bubble', name='False Vacuum Bubble', art='foambubble', c1='#80d0ff', c2='#e0f8ff', kind='foam_bubble', light=6, hardness=0.3, tool='none'),
    dict(id='planck_casing', name='Planck Casing', art='planck', c1='#203848', c2='#80f0ff', kind='casing', light=4, hardness=8.0, tool='diamond'),
    dict(id='transcendent_stone', name='Transcendent Stone', art='transcendent', c1='#f0ece0', c2='#ffd070', kind='deco', light=12, hardness=50.0, tool='diamond'),
    dict(id='transcendent_glass', name='Transcendent Glass', art='koch', c1='#fff4d0', c2='#ffffff', kind='glass', light=15, hardness=5.0, tool='none'),
    dict(id='omnium_block', name='Block of Omnium', art='omnium', c1='#c0c0ff', c2='#ffffff', kind='deco', light=8, hardness=100.0, tool='diamond'),
    dict(id='omnipotence_casing', name='Omnipotence Casing', art='omnicasing', c1='#fff8e8', c2='#ffc040', kind='deco', light=12, hardness=80.0, tool='diamond'),
    dict(id='return_rift', name='Return Rift', art='rift', c1='#200830', c2='#c080ff', kind='return_rift', light=15, hardness=1.0, tool='none'),
] + [dict(id=i + '_ore', name=n.replace('Virtual ', 'Ephemeral ') + ' Ore', art='ore', base='foam_substrate', c1=c1, c2=c2, kind='ore', drops=i,
          light=9, hardness=2.0, tool='iron') for i, n, c1, c2, _ in VIRTUAL]

FLUIDS = [dict(id='pleroma', name='Pleroma', art='law', c1='#ffffff', c2='#e0c0ff', gas=False, light=15, temp=0, density=0, viscosity=500)]

KEY_ADD = {'5': ['planck_casing'], '6': ['planck_casing', 'interface_port']}

MACHINE_TEXT = {'vacuum_harvester': ('Vacuum Fluctuation Harvester',
                                     'Holds a bubble of false vacuum open in the Quantum Foam and catches the virtual particles that flicker through it. '
                                     'What it catches depends on where it stands and how it is tuned.')}
MACHINE_CAT = {'vacuum_harvester': 'spacetime'}
MACHINE_RESEARCH = {'vacuum_harvester': 'quantum_foam'}
TIER = {'vacuum_harvester': 5}
MC = {'vacuum_harvester': (['GLG', 'FSF', 'PCP'], {'G': 'containment_field_glass', 'L': 'graviton_lens_array', 'F': 'field_emitter',
                                                    'S': 'planck_casing', 'P': 'planck_shard', 'C': 'casimir_plate'})}
EXTRA = {'vacuum_harvester': 'Only works in the $(thing)Quantum Foam$(). Each virtual particle has a $(thing)resonance$() (0-100) that changes from place to place; '
                             'the harvester catches whichever particle its dial is tuned closest to, and the closer the better. Use a $(item)Resonance Probe$() '
                             'where the harvester stands to read the four values - then move it, or retune it, to choose what it makes.'}

RESEARCH = [
    ('quantum_foam', 'The Quantum Foam', 'Below the Planck length spacetime is a boiling foam where particles borrow existence from the vacuum. '
                                         'Unlocks the Planck Diver and the Vacuum Fluctuation Harvester.', 'planck_diver', 12, -3, 160, ['quantum_gravity'], [('planck_shard', 4)]),
    ('pleroma', 'Pleroma', 'The fullness of everything: every fluid there is, poured together. Unlocks the Pleroma Confluence.',
     'pleroma_confluence', 21, -2, 2000, ['omnilex'], [('omnium_ingot', 1)]),
]

CREATURES = {
    'fractal_wyrm': ('Fractal Wyrm', [('fractal_core', 1, 1, 0.5), ('fractal_dust', 1, 3, 1.0), ('golden_ratio_glass', 0, 2, 1.0)]),
    'axiom_sentinel': ('Axiom Sentinel', [('proof_fragment', 1, 2, 1.0), ('axiom_schema', 1, 1, 0.3), ('prime_shard', 0, 2, 1.0)]),
}

DECOR = {  # kind -> (name, lore, centre ingredient, virtual particle)
    'black_hole': ('Miniature Black Hole', 'A real one, very small, held on a pedestal. It bends the light behind it.', 'micro_singularity', 'virtual_graviton'),
    'white_hole': ('Miniature White Hole', 'Nothing can enter it. Light pours out forever.', 'white_hole_fragment', 'virtual_photon'),
    'wormhole': ('Miniature Wormhole', 'The other mouth is somewhere else. Do not put your hand in.', 'wormhole_throat', 'virtual_graviton'),
    'pocket_universe': ('Pocket Universe Display', 'A whole universe, still expanding, in a jar.', 'pocket_universe', 'virtual_photon'),
    'tesseract': ('Tesseract Display', 'A four-dimensional cube turning through itself.', 'tesseract_core', 'virtual_monopole'),
    'pulsar': ('Miniature Pulsar', 'A neutron star spinning hundreds of times a second, sweeping its beams across the room.', 'magnetar_shard', 'virtual_photon'),
    'galaxy': ('Galaxy in a Box', 'Two hundred billion stars, more or less, in two spiral arms.', 'stellar_core', 'virtual_photon'),
    'quasar': ('Miniature Quasar', 'A feeding black hole outshining its host galaxy.', 'quark_star_core', 'virtual_quark'),
    'dyson_sphere': ('Dyson Sphere Model', 'A star wrapped in a swarm of collectors.', 'dyson_shell_segment', 'virtual_photon'),
    'penteract': ('Penteract Display', 'The shadow of a five-dimensional cube. It has 32 corners and 80 edges.', 'm_brane', 'virtual_monopole'),
}

LANG = {
    'block.theoreticalsciences.omnipotence_core': 'Omnipotence Core',
    'block.theoreticalsciences.pleroma_confluence': 'Pleroma Confluence',
    'entity.theoreticalsciences.hyperobject': 'Hyperobject',
    'biome.theoreticalsciences.quantum_foam': 'Quantum Foam',
    'biome.theoreticalsciences.transcendental_expanse': 'The Transcendental',
    'status.theoreticalsciences.awaiting_confirmation': 'Awaiting confirmation',
    'status.theoreticalsciences.declined': 'Declined',
    'gui.theoreticalsciences.omega_tip': 'An overclock that should not exist',
    'gui.theoreticalsciences.overclock_omega': 'Overclock: ℵ₀ / ∞',
    'gui.theoreticalsciences.collapse_review': 'Review the impossible',
    'gui.theoreticalsciences.collapse_title': 'THE OMNILEX ENGINE WILL COLLAPSE',
    'gui.theoreticalsciences.collapse_body': 'Running this recipe cannot be undone. The engine and its whole structure will be destroyed and fold into a '
                                        'five-dimensional object that drags everything near it inside. Proceed?',
    'gui.theoreticalsciences.collapse_yes': 'Yes - do the impossible',
    'gui.theoreticalsciences.collapse_no': 'No',
    'gui.theoreticalsciences.omnipotence': 'Omnipotence',
    'gui.theoreticalsciences.omnipotence_search': 'Search everything...',
    'gui.theoreticalsciences.confluence_status': '%s / %s fluids at %s mB',
    'message.theoreticalsciences.collapse_accepted': 'So be it.',
    'message.theoreticalsciences.collapse_declined': 'The engine stands down. (Toggle the overclock to be asked again.)',
    'message.theoreticalsciences.collapse_begins': 'The Omnilex Engine is collapsing into a higher dimension!',
    'message.theoreticalsciences.transcendental_arrival': 'You are in the Transcendental. Everything here has already been made.',
    'message.theoreticalsciences.core_inert': 'Outside the Transcendental, the Core is only a stone.',
    'message.theoreticalsciences.probe_outside': 'No resonances: this only works in the Quantum Foam.',
    'message.theoreticalsciences.probe_header': 'Virtual-particle resonances at %s, %s, %s:',
    'jei.theoreticalsciences.secret_1': 'IRREVERSIBLE - the Omnilex Engine is destroyed',
    'jei.theoreticalsciences.secret_2': 'Needs an overclock that should not exist',
    'jei.theoreticalsciences.secret_3': 'Asks for confirmation before it starts',
    'jei.theoreticalsciences.secret_4': 'Result: ???',
    'jei.theoreticalsciences.vacuum_note_1': 'Makes ONE of these: whichever is tuned closest here',
    'tooltip.theoreticalsciences.decor_live': 'A working miniature, drawn live.',
    'tooltip.theoreticalsciences.decor_lag_lens': 'Heavy: bends the light behind it (screen-space lensing). Many in view can lower your frame rate.',
    'tooltip.theoreticalsciences.decor_lag_many': 'Light, but dozens in view at once can lower your frame rate.',
    'tuning.theoreticalsciences.vacuum_resonance': 'Vacuum Resonance',
}
for k, (name, lore, _, _) in DECOR.items():
    LANG[f'block.theoreticalsciences.display_{k}'] = name
    LANG[f'block.theoreticalsciences.display_{k}.lore'] = lore


# ------------------------------------------------------------------ the Vacuum Fluctuation Harvester's structure
def vacuum_harvester():
    """A bubble chamber: a sphere of field glass on a Planck-casing plinth, six graviton lenses aimed at the centre,
    a coil ring around its equator and a field emitter on top. The inside is kept empty: that is the false vacuum."""
    s = shapes.Shape(11, 12, 11)
    c, cy = 5, 6
    s.disc(c, c, 0, 5.2, '5')
    s.disc(c, c, 1, 3.6, '5')
    s.sphere(c, cy, c, 4.1, 'G', shell=0.55)
    s.great_circle(c, cy, c, 4.6, 'K', 'y')
    for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)):
        s.put(c + d[0] * 4, cy + d[1] * 4, c + d[2] * 4, 'l')
    s.put(c, cy + 5, c, 'F')
    s.sphere_air(c, cy, c, 3.6)
    s.ports('5', '6', 4, y=0)
    s.v[(c, 1, c - 4)] = 'C'
    s.ctrl = (c, 1, c - 4)
    s.center = (c, cy, c)
    return s


shapes.BUILDERS['vacuum_harvester'] = vacuum_harvester


# ------------------------------------------------------------------ worldgen helpers
def _add(a, b):
    return {'type': 'minecraft:add', 'argument1': a, 'argument2': b}


def _mul(a, b):
    return {'type': 'minecraft:mul', 'argument1': a, 'argument2': b}


def _max(a, b):
    return {'type': 'minecraft:max', 'argument1': a, 'argument2': b}


def _grad(y0, y1, v0, v1):
    return {'type': 'minecraft:y_clamped_gradient', 'from_y': y0, 'to_y': y1, 'from_value': v0, 'to_value': v1}


def _noise(nid, xz=1.0, y=0.0):
    return {'type': 'minecraft:noise', 'noise': nid, 'xz_scale': xz, 'y_scale': y}


def _wrap(inner):
    """The End's own post-processing (interpolation, blending, squeeze), around our density."""
    return {'type': 'minecraft:squeeze', 'argument': _mul(0.64, {'type': 'minecraft:interpolated', 'argument': {'type': 'minecraft:blend_density', 'argument': inner}})}


def island_layer(nid, mask_mul, mask_add, y_bottom, y_top, y_cap, y_air, roughness=0.5):
    """A layer of floating islands: a 2D mask decides where islands are; vertical gradients give each a flat-ish top and a hanging, tapering keel."""
    mask = _add(_mul(_noise(nid), mask_mul), mask_add)
    vert = _add(_grad(y_bottom, y_top, -2.2, 0.5), _grad(y_cap, y_air, 0.0, -2.7))
    return _add(_add(mask, vert), _mul('minecraft:end/base_3d_noise', roughness))


def apply(g):
    w, q, LANG, D, A, WG, LT, REC = g['w'], g['q'], g['LANG'], g['D'], g['A'], g['WG'], g['LT'], g['REC']
    mr, shaped, shapeless, text_entry, loot_chest, ore_feature, custom_feature = (g['mr'], g['shaped'], g['shapeless'], g['text_entry'],
                                                                                  g['loot_chest'], g['ore_feature'], g['custom_feature'])
    end = g['end']
    NS = 'theoreticalsciences'
    LANG.update(globals()['LANG'])

    def central(ns):
        """The vanilla End terrain (with its central island) as it sits inside the End's final_density."""
        return ns['noise_router']['final_density']['argument']['argument2']['argument']['argument']

    # ================================================================== the Bulk: an archipelago
    for nid, octave in (('bulk_islands_large', -7), ('bulk_islands_high', -6), ('platonic_islands_large', -7),
                        ('platonic_islands_mid', -6), ('platonic_islands_small', -5)):
        w(os.path.join(WG, 'noise', nid + '.json'), {'firstOctave': octave, 'amplitudes': [1.0, 1.0, 0.5]})
    w(os.path.join(WG, 'noise', 'foam_a.json'), {'firstOctave': -5, 'amplitudes': [1.0, 0.6]})
    w(os.path.join(WG, 'noise', 'foam_b.json'), {'firstOctave': -4, 'amplitudes': [1.0]})

    bulk = json.load(open(os.path.join(WG, 'noise_settings', 'the_bulk.json')))
    bulk['noise']['height'] = 256
    shape = _max(central(bulk), _max(island_layer(q('bulk_islands_large'), 1.7, -0.55, 12, 58, 64, 80),
                                     island_layer(q('bulk_islands_high'), 1.6, -0.75, 104, 140, 146, 160, 0.6)))
    bulk['noise_router']['final_density'] = _wrap(shape)
    w(os.path.join(WG, 'noise_settings', 'the_bulk.json'), bulk)

    # ================================================================== the Platonic Realm as fractal space
    pl = json.load(open(os.path.join(WG, 'noise_settings', 'platonic_realm.json')))
    pl['noise']['height'] = 256
    # the same archipelago at three scales: big islands low, smaller ones higher, smallest highest - self-similar
    shape = _max(central(pl), _max(island_layer(q('platonic_islands_large'), 1.7, -0.5, 10, 56, 62, 76),
                                   _max(island_layer(q('platonic_islands_mid'), 1.8, -0.75, 96, 124, 128, 138, 0.35),
                                        island_layer(q('platonic_islands_small'), 2.0, -1.0, 168, 184, 186, 194, 0.2))))
    pl['noise_router']['final_density'] = _wrap(shape)
    pl['disable_mob_generation'] = False
    w(os.path.join(WG, 'noise_settings', 'platonic_realm.json'), pl)
    for fid, rarity in (('menger_sponge', 9), ('sierpinski_tetrahedron', 10), ('mandelbulb', 14), ('cantor_dust', 8), ('hilbert_archive', 16)):
        custom_feature(fid, fid, [{'type': 'minecraft:rarity_filter', 'chance': rarity}, {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}])
    custom_feature('fractal_tree', 'fractal_tree', [{'type': 'minecraft:count', 'count': 1}, {'type': 'minecraft:rarity_filter', 'chance': 2},
                                                    {'type': 'minecraft:in_square'}, {'type': 'minecraft:biome'}])
    f = [[] for _ in range(11)]
    f[4] = [q(x) for x in ('platonic_solid', 'menger_sponge', 'sierpinski_tetrahedron', 'mandelbulb', 'cantor_dust', 'hilbert_archive')]
    f[6] = [q(x) for x in ('ore_prime_number', 'ore_transcendental', 'ore_fractal')]
    f[9] = [q('fractal_tree')]
    w(os.path.join(WG, 'biome', 'platonic_realm.json'), {
        'has_precipitation': False, 'temperature': 0.7, 'downfall': 0.0,
        'effects': {'sky_color': 0xC8D8FF, 'fog_color': 0xB8C8F0, 'water_color': 0xA0C0FF, 'water_fog_color': 0xC0D0FF,
                    'ambient_sound': 'minecraft:ambient.warped_forest.loop',
                    'particle': {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.003}},
        'spawners': {'monster': [{'type': q('fractal_wyrm'), 'weight': 6, 'minCount': 1, 'maxCount': 1},
                                 {'type': q('axiom_sentinel'), 'weight': 10, 'minCount': 1, 'maxCount': 2}]},
        'spawn_costs': {}, 'carvers': {}, 'features': f})
    dt = json.load(open(os.path.join(D, 'dimension_type', 'platonic_realm.json')))
    dt['effects'] = q('platonic_realm')
    w(os.path.join(D, 'dimension_type', 'platonic_realm.json'), dt)
    loot_chest('hilbert_archive', [('prime_shard', 14, 4, 12), ('transcendental_crystal', 12, 2, 6), ('fractal_core', 6, 1, 2),
                                   ('proof_fragment', 10, 2, 5), ('axiom_schema', 8, 1, 4), ('riemann_zeta_crystal', 3, 1, 1),
                                   ('fractal_dust', 14, 4, 12), ('koch_crystal', 8, 2, 8), ('platonic_compass', 1, 1, 1)])

    # ================================================================== the Quantum Foam
    sub = {'predicate_type': 'minecraft:block_match', 'block': q('foam_substrate')}
    for i, *_ in VIRTUAL:
        ore_feature('ore_' + i, sub, i + '_ore', 6, 10, 0, 256)
    ore_feature('foam_bubbles', {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:air'}, 'foam_bubble', 24, 10, 0, 256)
    ff = [[] for _ in range(11)]
    ff[6] = [q('ore_' + i) for i, *_ in VIRTUAL] + [q('foam_bubbles')]
    w(os.path.join(WG, 'biome', 'quantum_foam.json'), {
        'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
        'effects': {'sky_color': 0x0A1A28, 'fog_color': 0x0C2030, 'water_color': 0x80F0FF, 'water_fog_color': 0x0C2030,
                    'ambient_sound': 'minecraft:ambient.soul_sand_valley.loop',
                    'particle': {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.01}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': ff})
    foam = json.loads(json.dumps(end))
    foam['noise'] = {'min_y': 0, 'height': 256, 'size_horizontal': 1, 'size_vertical': 2}
    foam['default_block'] = {'Name': q('foam_substrate')}
    foam['surface_rule'] = {'type': 'minecraft:block', 'result_state': {'Name': q('foam_substrate')}}
    foam['disable_mob_generation'] = True
    # membranes: thin sheets where either of two 3D noises crosses zero - the walls between bubbles of nothing
    sheets = _max(_add(0.07, _mul(-1.0, {'type': 'minecraft:abs', 'argument': _noise(q('foam_a'), 1.0, 1.0)})),
                  _add(0.05, _mul(-1.0, {'type': 'minecraft:abs', 'argument': _noise(q('foam_b'), 1.0, 1.0)})))
    fade = _add(_grad(0, 24, -1.0, 0.0), _grad(232, 256, 0.0, -1.0))
    foam['noise_router']['final_density'] = _wrap(_mul(_add(sheets, fade), 8.0))
    foam['noise_router']['initial_density_without_jaggedness'] = -1.0
    foam['spawn_target'] = []
    w(os.path.join(WG, 'noise_settings', 'quantum_foam.json'), foam)
    w(os.path.join(D, 'dimension_type', 'quantum_foam.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
        'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.3, 'logical_height': 256,
        'effects': 'minecraft:the_end', 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': 18000,
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'dimension', 'quantum_foam.json'), {'type': q('quantum_foam'), 'generator': {
        'type': 'minecraft:noise', 'settings': q('quantum_foam'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('quantum_foam')}}})

    # ================================================================== the Transcendental (empty: it is built when first entered)
    tr = json.loads(json.dumps(end))
    tr['default_block'] = {'Name': q('transcendent_stone')}
    tr['noise_router']['final_density'] = -1.0
    tr['noise_router']['initial_density_without_jaggedness'] = -1.0
    tr['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': []}
    tr['disable_mob_generation'] = True
    tr['spawn_target'] = []
    w(os.path.join(WG, 'noise_settings', 'transcendental.json'), tr)
    w(os.path.join(WG, 'biome', 'transcendental_expanse.json'), {
        'has_precipitation': False, 'temperature': 0.8, 'downfall': 0.0,
        'effects': {'sky_color': 0xFFF4D8, 'fog_color': 0xFFF0D0, 'water_color': 0xFFFFFF, 'water_fog_color': 0xFFF0D0,
                    'particle': {'options': {'type': 'minecraft:end_rod'}, 'probability': 0.004}},
        'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': [[] for _ in range(11)]})
    w(os.path.join(D, 'dimension_type', 'transcendental.json'), {
        'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
        'has_skylight': True, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 1.0, 'logical_height': 256,
        'effects': q('transcendental'), 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': 6000,
        'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
    w(os.path.join(D, 'dimension', 'transcendental.json'), {'type': q('transcendental'), 'generator': {
        'type': 'minecraft:noise', 'settings': q('transcendental'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('transcendental_expanse')}}})
    for aid, title, desc, icon, dim, hidden in (('quantum_foam_arrival', 'Below the Planck Length', 'Dive into the Quantum Foam', 'planck_diver', 'quantum_foam', False),
                                                ('transcendence', 'Transcendence', 'Do the impossible', 'omnium_singularity', 'transcendental', True)):
        w(os.path.join(D, 'advancement', aid + '.json'), {
            'display': {'icon': {'id': q(icon)}, 'title': title, 'description': desc, 'frame': 'challenge', 'show_toast': True,
                        'announce_to_chat': True, 'hidden': hidden},
            'parent': q('omnilex') if hidden else 'minecraft:end/root',
            'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q(dim)}}}, 'requirements': [['arrived']]})

    # ================================================================== blocks registered in Java (models, loot, lang)
    def cube(bid, textures, parent='minecraft:block/cube_all', translucent=False):
        m = {'parent': parent, 'textures': textures}
        if translucent:
            m['render_type'] = 'minecraft:translucent'
        w(os.path.join(A, 'models/block', bid + '.json'), m)
        w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
        w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    cube('omnipotence_core', {'all': f'{NS}:block/omnipotence_core'})
    cube('pleroma_confluence', {'top': f'{NS}:block/pleroma_confluence_top', 'bottom': f'{NS}:block/pleroma_confluence_bottom',
                                'side': f'{NS}:block/pleroma_confluence_side'}, 'minecraft:block/cube_bottom_top')
    g['self_drop']('pleroma_confluence')
    pedestal = {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/display_pedestal', 'p': f'{NS}:block/display_pedestal'},
                'elements': [
                    {'from': [1, 0, 1], 'to': [15, 2, 15], 'faces': {d: {'texture': '#p', 'uv': [1, 1, 15, 15] if d in ('up', 'down') else [1, 14, 15, 16]}
                                                                    for d in ('north', 'south', 'east', 'west', 'up', 'down')}},
                    {'from': [3, 2, 3], 'to': [13, 4, 13], 'faces': {d: {'texture': '#p', 'uv': [3, 3, 13, 13] if d in ('up', 'down') else [3, 12, 13, 14]}
                                                                    for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]}
    w(os.path.join(A, 'models/block', 'display_pedestal.json'), pedestal)
    for k, (name, lore, centre, virt) in DECOR.items():
        bid = 'display_' + k
        w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': f'{NS}:block/display_pedestal'}}})
        # in the hand: the pedestal with the miniature's item floating above it
        w(os.path.join(A, 'models/item', bid + '.json'), {
            'parent': 'minecraft:block/block',
            'textures': {'particle': f'{NS}:block/display_pedestal', 'p': f'{NS}:block/display_pedestal', 'c': f'{NS}:item/{centre}'},
            'elements': pedestal['elements'] + [{'from': [5, 6, 7.5], 'to': [11, 12, 8.5], 'faces': {'north': {'texture': '#c', 'uv': [0, 0, 16, 16]},
                                                                                                   'south': {'texture': '#c', 'uv': [0, 0, 16, 16]}}}],
            'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]}}})
        g['self_drop'](bid)
        shaped(bid, ['GGG', 'GMG', 'PVP'], {'G': 'containment_field_glass', 'M': centre, 'P': 'neutronium_plate', 'V': virt}, bid)
    # mining tags (the main tag files were already written: add to them)
    for tname, extra in (('mineable/pickaxe', [q('pleroma_confluence')] + [q('display_' + k) for k in DECOR]),
                         ('needs_diamond_tool', [q('pleroma_confluence')]), ('needs_iron_tool', [q('display_' + k) for k in DECOR])):
        tp = os.path.join(g['RES'], 'data', 'minecraft', 'tags', 'block', tname + '.json')
        t = json.load(open(tp)) if os.path.exists(tp) else {'replace': False, 'values': []}
        t['values'] += [x for x in extra if x not in t['values']]
        w(tp, t)

    # ================================================================== recipes
    shaped('planck_diver', ['GPG', 'PSP', 'GPG'], {'G': 'graviton_crystal', 'P': 'planck_shard', 'S': 'brane_shifter'}, 'planck_diver')
    shaped('resonance_probe', [' A ', 'QDQ', ' Q '], {'A': 'spectral_analyzer', 'Q': 'qubit', 'D': 'planck_shard'}, 'resonance_probe')
    shaped('planck_casing', ['NPN', 'PSP', 'NPN'], {'N': 'neutronium_plate', 'P': 'planck_shard', 'S': 'spacetime_casing'}, 'planck_casing', 4)
    shaped('omnium_block', ['OOO', 'OOO', 'OOO'], {'O': 'omnium_ingot'}, 'omnium_block')
    shapeless('omnium_from_block', ['omnium_block'], 'omnium_ingot', 9)
    shaped('pleroma_confluence', ['OTO', 'CXC', 'OTO'], {'O': 'transcendent_metal_ingot', 'T': 'transfinite_coil_block', 'C': 'containment_field_glass',
                                                         'X': 'omniscience_catalyst'}, 'pleroma_confluence')
    v = [i for i, *_ in VIRTUAL]
    mr('vacuum_harvest', 'vacuum_harvesting', [('casimir_plate', 1)], [('superfluid_helium', 1000)], [(i, 4) for i in v], [], 1048576, 200, 'quantum_foam')
    mr('virtual_quark_condensation', 'spectrometry', [('virtual_quark', 8)], [('superfluid_helium', 500)], [('up_down_quarks', 2), ('strange_charm_quarks', 1)],
       [], 131072, 200, 'quantum_foam')
    mr('virtual_graviton_condensation', 'graviton_condensing', [('virtual_graviton', 8)], [('superfluid_helium', 500)], [('graviton_crystal', 2)], [], 262144, 200, 'quantum_foam')
    mr('virtual_photon_entangling', 'entangling', [('virtual_photon', 8)], [], [('entangled_photon_pair', 4)], [], 65536, 160, 'quantum_foam')
    mr('virtual_monopole_condensation', 'mass_modulation', [('virtual_monopole', 4)], [('superfluid_helium', 500)], [('magnetic_monopole', 1)], [], 262144, 200, 'quantum_foam')
    w(os.path.join(REC, 'compaction', 'omnium_singularity.json'), {'type': f'{NS}:compaction', 'ingredient': {'item': q('omnium_ingot')}, 'count': 256,
                                                                     'result': {'id': q('omnium_singularity'), 'count': 1}})
    # the impossible recipe: no result, a secret overclock, a confirmation, and the end of the engine
    w(os.path.join(REC, 'omnilex_simulation', 'the_impossible.json'), {
        'type': f'{NS}:omnilex_simulation', 'secret': True,
        'item_inputs': [{'item': q('omnium_singularity'), 'count': 1}, {'item': q('absolute_infinity'), 'count': 16},
                        {'item': q('theory_of_everything'), 'count': 4}, {'item': q('omega_point'), 'count': 4}],
        'fluid_inputs': [{'fluid': q('pleroma'), 'amount': 16000}, {'fluid': q('primordial_law'), 'amount': 512000}],
        'energy': 400000000, 'duration': 12000, 'research': q('omnilex')})
    g['MACHINE_RECIPES'].append(dict(name='the_impossible', type='omnilex_simulation', inputs=['omnium_singularity'], fluids=['pleroma'], outputs=[],
                                     fluid_outputs=[], catalysts=[], research='omnilex', chances=None, min_tier=0))

    # ================================================================== guide
    text_entry('quantum', 'quantum_foam', 'The Quantum Foam', 'planck_diver', [
        'Below the Planck length, spacetime is not smooth. The $(item)Planck Diver$() takes you there: membranes of $(item)Foam Substrate$() between bubbles of nothing, where false-vacuum bubbles swell and pop.',
        'Matter here is borrowed. $(item)Ephemeral Ores$() condense out of the foam and vanish again a few minutes later, and anything dropped dissolves back into the vacuum within half a minute. Mine fast.',
        'Every virtual particle has a $(thing)resonance$() that changes smoothly from place to place. The $(item)Resonance Probe$() reads all four where you stand; the strongest field decides which ore grows there.',
        'The $(item)Vacuum Fluctuation Harvester$() catches whichever particle its dial is tuned closest to - so the same harvester makes quarks in one place and gravitons fifty blocks away. Read the probe, then tune.'],
        adv='quantum_foam', sort=8, spot=['planck_diver', 'resonance_probe', 'vacuum_harvester'])
    text_entry('mathematics', 'fractal_space', 'Fractal Space', 'fractal_lattice_block', [
        'The Platonic Realm is self-similar: big islands low down, smaller ones above them, smaller still at the top. Its landmarks are mathematical objects built exactly to their definitions.',
        '$(li)$(thing)Menger sponges$() with Euclid\'s Vault at their heart$(li)$(thing)Sierpinski tetrahedra$()$(li)$(thing)Mandelbulbs$()$(li)$(thing)Cantor dust$()$(li)$(thing)recursive trees$()$(li)the $(thing)Hilbert Archive$(), a corridor that walks a Hilbert curve to its treasure.',
        'Two natives guard it: the $(thing)Fractal Wyrm$(), a serpent made of smaller copies of itself, and the $(thing)Axiom Sentinel$(), an icosahedron that spends two seconds proving you wrong - break its line of sight.'],
        adv='platonic_realm', sort=5, spot=['fractal_lattice_block', 'koch_crystal'])
    text_entry('endgame', 'miniatures', 'Display Miniatures', 'display_black_hole', [
        'Working miniatures on pedestals: black and white holes, a wormhole, a pocket universe, a tesseract, a pulsar, a galaxy, a quasar, a Dyson sphere and a penteract.',
        'They are drawn live. The ones that bend light (black hole, white hole, wormhole, pulsar) are the heaviest - their tooltips warn you. Only the nearest two dozen are drawn at once.'],
        sort=2, spot=['display_black_hole', 'display_tesseract', 'display_galaxy'])
    text_entry('endgame', 'pleroma', 'Pleroma', 'pleroma_confluence', [
        'Pour every fluid there is - water and lava too - into the $(item)Pleroma Confluence$(). It keeps a running total of each; when every one has reached a million mB, a thousand mB of $(item)Pleroma$() appears.',
        'It accepts fluids from any pipe or AE2 on any side, and Pleroma can be drained from any side.'],
        adv='pleroma', sort=3, spot=['pleroma_confluence'])
