"""
Round 8:
  * the mod's own advancement tab (a root, and every research as an advancement in a tree that mirrors the Research Matrix)
  * GregTech-style fluid handling: an Empty Containment Cell scoops our fluids; buckets refuse them (see Java)
  * void fall-through messages
  * ~55 late-game crafting recipes moved to the Extreme Assembly Table, with three new tiers of frame components
"""
import glob
import json
import os

NS = 'theoreticalsciences'

ITEMS = [
    dict(id='containment_cell', name='Empty Containment Cell', art='lens', c1='#283038', c2='#a0c8e0', special='emptycell', stack=16,
         lore='A magnetically sealed flask for fluids too hot, too cold or too strange for a bucket. Scoop a source block or fill it from a tank.'),
    dict(id='assembly_matrix', name='Assembly Matrix', art='chip', c1='#203048', c2='#80d0ff',
         lore='A femtoscale jig that holds a hundred parts in exactly the right place while the Extreme Assembly Table joins them.'),
    dict(id='hyperlattice_frame', name='Hyperlattice Frame', art='tesseract', c1='#301848', c2='#d090ff',
         lore='A scaffold folded through a fourth dimension, so a nine-by-nine assembly has room for its insides.'),
    dict(id='transfinite_weave', name='Transfinite Weave', art='manifold', c1='#402008', c2='#ffd070',
         lore='Cloth with more threads than there are numbers to count them. Endgame assemblies are stitched on it.'),
]
BLOCKS, FLUIDS = [], []

LANG = {
    'message.theoreticalsciences.bucket_refused': '%s is %s - a bucket cannot hold it. Use an Empty Containment Cell.',
    'message.theoreticalsciences.hazard.hot': 'far too hot (%s K)',
    'message.theoreticalsciences.hazard.cold': 'far too cold (%s K)',
    'message.theoreticalsciences.hazard.gas': 'a gas',
    'message.theoreticalsciences.hazard.exotic': 'too exotic',
    'message.theoreticalsciences.cell_ordinary': 'Ordinary fluids go in ordinary buckets.',
    'tooltip.theoreticalsciences.empty_cell': 'Right-click a Theoretical Sciences fluid, or use on a tank',
    'message.theoreticalsciences.void_proof': 'The proof will not let you fall out of it.',
    'message.theoreticalsciences.void_the_bulk': 'You fall off the edge of the Bulk - and land back on our own brane.',
    'message.theoreticalsciences.void_platonic_realm': 'Beneath the last Form lies the first infinity. You fall into the Transfinite Hierarchy.',
    'message.theoreticalsciences.void_transfinite_hierarchy': 'Below every ordinal there is only foam. You fall into the Quantum Foam.',
    'message.theoreticalsciences.void_quantum_foam': 'The foam lets go of you, and you settle into the Bulk.',
    'message.theoreticalsciences.void_black_hole_interior': 'The singularity crushes you through - and a white hole spits you out.',
    'message.theoreticalsciences.void_white_hole_interior': 'The white hole expels you back into ordinary space.',
    'message.theoreticalsciences.void_hyperdimensional_reality': 'You fall between the branches of the World Tree, into the Bulk.',
    'message.theoreticalsciences.void_transcendental': 'You fall out of the Transcendental, back into Hyperdimensional Reality.',
    'advancements.theoreticalsciences.root.title': 'Theoretical Sciences',
    'advancements.theoreticalsciences.root.description': 'Physics, past its edges',
}

# ---------------------------------------------------------------------------------------- the Extreme Assembly Table
MOVE = ['synchrotron', 'particle_spectrometer', 'tensor_engine', 'higgs_field_modulator', 'nucleosynthesis_chamber', 'holographic_encoder',
        'graviton_condenser', 'string_resonance_loom', 'graviton_lens_array', 'hawking_radiation_collector', 'singularity_compactor',
        'white_hole_emitter', 'chronal_anchor', 'wormhole_frame_casing', 'brane_shifter', 'horizon_diver', 'stellar_containment_plate',
        'dimensional_stabiliser', 'brane_tether', 'chronal_field_projector', 'compactification_lathe', 'dimensional_lift_projector',
        'geodesic_anchor', 'graviton_field_projector', 'graviton_harness', 'planck_diver', 'rift_frame_casing', 'singularity_energy_cell',
        'vacuum_energy_cell', 'vacuum_state_projector', 'coherence_stabiliser', 'cosmogenic_casing', 'reality_lattice_casing',
        'spacetime_anchor_pylon', 'display_black_hole', 'display_dyson_sphere', 'display_galaxy', 'display_penteract',
        'display_pocket_universe', 'display_pulsar', 'display_quasar', 'display_tesseract', 'display_white_hole', 'display_wormhole',
        'entangled_transceiver', 'einstein_rosen_bridge', 'cantor_diagonalizer', 'omnium_energy_cell', 'cantor_key', 'well_ordering_charm',
        'axiom_distiller', 'conceptual_casing', 'identity_kernel', 'ordinal_recursor', 'transfinite_casing', 'primordial_charter',
        'omnilex_casing', 'pleroma_confluence']
# by the research level of the thing being made: the frame component, the structural metal
TIER_B = {'graviton_harness', 'brane_tether', 'chronal_field_projector', 'compactification_lathe', 'dimensional_lift_projector', 'geodesic_anchor',
          'graviton_field_projector', 'planck_diver', 'rift_frame_casing', 'singularity_energy_cell', 'vacuum_energy_cell',
          'vacuum_state_projector', 'coherence_stabiliser', 'cosmogenic_casing', 'reality_lattice_casing', 'spacetime_anchor_pylon',
          'entangled_transceiver', 'einstein_rosen_bridge', 'cantor_diagonalizer', 'omnium_energy_cell'} | {m for m in MOVE if m.startswith('display_')}
TIER_C = {'cantor_key', 'well_ordering_charm', 'axiom_distiller', 'conceptual_casing', 'identity_kernel', 'ordinal_recursor', 'transfinite_casing',
          'primordial_charter', 'omnilex_casing', 'pleroma_confluence'}
MOVED = set()


TIERS = [('transfinite_weave', 'crystalline_matrix_ingot'), ('hyperlattice_frame', 'cosmic_neutronium_ingot'), ('assembly_matrix', 'chronosteel_ingot')]


def _tiers(o):
    """The tiers this item may use, best first (it falls back to a lower one if a higher one comes too late)."""
    return TIERS[0 if o in TIER_C else 1 if o in TIER_B else 2:]


def _grid3(r):
    """The original recipe as a 3x3 grid of ingredient dicts (None = empty)."""
    g = [[None] * 3 for _ in range(3)]
    if r['type'].endswith('shaped'):
        pat = r['pattern']
        oy, ox = (3 - len(pat)) // 2, (3 - max(len(p) for p in pat)) // 2
        for y, row in enumerate(pat):
            for x, ch in enumerate(row):
                if ch != ' ': g[y + oy][x + ox] = r['key'][ch]
    else:
        for k, ing in enumerate(r['ingredients']):
            g[k // 3][k % 3] = ing
    return g


def apply(g):
    w, q, mr, shaped, REC = g['w'], g['q'], g['mr'], g['shaped'], g['REC']
    g['LANG'].update(LANG)
    # ---- the cell
    shaped('containment_cell', ['IGI', 'G G', 'IGI'], {'I': 'minecraft:iron_ingot', 'G': 'containment_field_glass'}, 'containment_cell', 4)
    te = g['text_entry']
    te('basics', 'containment_cells', 'Containment Cells', 'containment_cell', [
        'A bucket is a cup with a handle. It cannot hold a plasma at a hundred million kelvin, a superfluid near absolute zero, or a liquid that is really a law of physics. Try, and it refuses.',
        'An $(item)Empty Containment Cell$() can. Right-click a source block of any Theoretical Sciences fluid to fill it, or click a machine tank with it. Pour it out, feed it to a tank, or use it in a recipe, and you get the empty cell back.$(br2)Ordinary water and lava still go in ordinary buckets.',
        'What keeps a bucket out keeps you out too. $(thing)Hot$() liquids (500 K and up) burn like lava, harder the hotter they are, and destroy anything dropped in them - past 2,000 K not even fire resistance or netherite helps. $(thing)Cold$() ones (200 K and below) freeze like powder snow, faster the colder they are; leather keeps out all but the coldest. $(thing)Exotic$() ones - a false vacuum, liquid spacetime, the broths and reagents - twist whoever wades in with harmful effects.'],
       sort=4, spot=['containment_cell'])
    te('world', 'falling_through', 'Falling Through', 'minecraft:ender_pearl', [
        'None of these places has a bottom you can die on. Fall off the floor of one and you fall into another:$(li)off the $(thing)Bulk$(): back onto our own brane$(li)through the $(thing)Platonic Realm$(): into the Transfinite Hierarchy$(li)below the $(thing)Transfinite$(): into the Quantum Foam',
        '$(li)through a $(thing)black hole$()\'s singularity: out of its white hole$(li)past the top of a $(thing)white hole$(): home$(li)between the branches of $(thing)Hyperdimensional Reality$(): into the Bulk$(li)out of the $(thing)Transcendental$(): into Hyperdimensional Reality$(br2)Carry the right attunement for wherever you land.',
        'One world is different. The $(thing)Planetary Interior$() has a true floor - the $(item)Inner Core Boundary$(), solid iron that nothing yet cuts - and a roof of bedrock. There is nothing to fall through: you leave the way you came, by a $(item)Mantle Breach$().'],
       sort=40)
    # ---- frame components (Femtoscale Assembly Line)
    mr('assembly_matrix', 'femto_assembly', [('photonic_processor', 2), ('metamaterial_sheet', 2), ('chronosteel_ingot', 2), ('quantum_processor', 1)],
       [], [('assembly_matrix', 4)], [], 65536, 400, 'femtoengineering')
    mr('hyperlattice_frame', 'femto_assembly', [('assembly_matrix', 2), ('hyperframe_casing', 1), ('graviton_crystal', 1), ('tesseract_core', 1)],
       [], [('hyperlattice_frame', 4)], [], 1048576, 600, 'hyperdimensional_geometry')
    mr('transfinite_weave', 'femto_assembly', [('hyperlattice_frame', 2), ('aleph_null_shard', 2), ('cosmic_processor', 1), ('crystalline_matrix_ingot', 1)],
       [], [('transfinite_weave', 4)], [], 16777216, 800, 'transfinite_mathematics')
    # ---- move late crafting recipes onto the 9x9 table: each original cell becomes a 3x3 block
    #        s I s
    #        I X I        I = the original ingredient, X = the tier's frame component, s = the tier's structural metal
    #        s I s
    MOVED.clear()
    # only move a recipe if everything it would newly need (the table, its frame, its metal) is obtainable strictly
    # earlier in progression than the thing itself - otherwise the move would create a circular dependency
    import importlib, sys as _s
    _argv = _s.argv
    _s.argv = ['chaincheck']
    import chaincheck
    chaincheck = importlib.reload(chaincheck)
    _s.argv = _argv
    WV = chaincheck.WAVE
    kept = []
    for f in sorted(glob.glob(os.path.join(REC, 'crafting', '*.json'))):
        r = json.load(open(f))
        out = r['result']['id'].split(':')[-1]
        if out not in MOVE: continue
        mine = WV.get(r['result']['id'])
        pick = None
        for frame, metal in _tiers(out):
            need = [WV.get(f'{NS}:{x}') for x in ('extreme_assembly_table', frame, metal)]
            if mine is not None and None not in need and mine > max(need):
                pick = (frame, metal)
                break
        if not pick:
            kept.append(out)
            continue
        frame, metal = pick
        g3 = _grid3(r)
        letters, key = {}, {'X': {'item': q(frame)}, 's': {'item': q(metal)}}
        pool = iter('ABCDEFGHJKLMNOPQRTUVWYZ')
        rows = [''] * 9
        for y in range(3):
            for x in range(3):
                ing = g3[y][x]
                if ing is None:
                    block = ['   ', '   ', '   ']
                else:
                    sig = json.dumps(ing, sort_keys=True)
                    if sig not in letters:
                        letters[sig] = next(pool)
                        key[letters[sig]] = ing
                    L = letters[sig]
                    block = [f's{L}s', f'{L}X{L}', f's{L}s']
                for k in range(3):
                    rows[y * 3 + k] += block[k]
        count = r['result'].get('count', 1)
        if 'casing' in out or out.endswith('_plate'): count *= 4     # structure blocks: four times the material, four times the output
        w(os.path.join(REC, 'extreme', out + '.json'), {'type': f'{NS}:extreme_shaped', 'pattern': rows, 'key': key,
                                                        'result': {'id': r['result']['id'], 'count': count}})
        os.remove(f)
        MOVED.add(os.path.basename(f)[:-5])
    print('moved to the Extreme Assembly Table:', len(MOVED), '- kept in the crafting grid (needed earlier):', kept)


def finish(g):
    w, q, D, BA, LANG = g['w'], g['q'], g['D'], g['BA'], g['LANG']
    # ---- Codex pages that showed a moved crafting recipe now point at the table
    fixed = 0
    for f in glob.glob(os.path.join(BA, 'entries', '*', '*.json')):
        e = json.load(open(f))
        ch = False
        for k, pg in enumerate(e['pages']):
            if pg.get('type') == 'patchouli:crafting' and pg['recipe'].split('/')[-1] in MOVED:
                rid = pg['recipe'].split('/')[-1]
                txt = (pg.get('text', '') + '$(br2)' if pg.get('text') else '') + \
                      f'Assembled on the $(item)Extreme Assembly Table$() from a 9x9 layout - look it up in JEI. Each part of the old recipe now sits in a ring of frame components.'
                e['pages'][k] = {'type': 'patchouli:text', 'text': txt}
                ch = True
                fixed += 1
        if ch: w(f, e)
    print('codex pages pointed at the extreme table:', fixed)

    # ---- the advancement tab
    AD = os.path.join(D, 'advancement')
    root = f'{NS}:root'
    w(os.path.join(AD, 'root.json'), {
        'display': {'icon': {'id': q('quantum_codex')}, 'title': {'translate': 'advancements.theoreticalsciences.root.title'},
                    'description': {'translate': 'advancements.theoreticalsciences.root.description'},
                    'background': f'{NS}:textures/gui/advancements/backgrounds/quantum.png',
                    'frame': 'task', 'show_toast': False, 'announce_to_chat': False, 'hidden': False},
        'criteria': {'tick': {'trigger': 'minecraft:tick'}}, 'requirements': [['tick']]})
    RD = os.path.join(D, 'ts_research')
    res = {os.path.basename(f)[:-5]: json.load(open(f)) for f in glob.glob(os.path.join(RD, '*.json'))}
    for rid, r in res.items():
        ins = r.get('insight', 0)
        req = [x.split(':')[-1] for x in r.get('requires', [])]
        # the parent is the most advanced prerequisite, so the tree reads like the research map
        parent = f'{NS}:research/' + max(req, key=lambda x: (res.get(x, {}).get('insight', 0), x)) if req else root
        frame = 'challenge' if ins >= 600 or r.get('grant_only') else 'goal' if ins >= 150 else 'task'
        hidden = bool(r.get('hidden') or r.get('grant_only'))
        w(os.path.join(AD, 'research', rid + '.json'), {
            'display': {'icon': {'id': r['icon']}, 'title': {'translate': f'research.{NS}.{rid}'},
                        'description': {'translate': f'research.{NS}.{rid}.desc'}, 'frame': frame,
                        'show_toast': True, 'announce_to_chat': False, 'hidden': hidden},
            'parent': parent,
            'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]})
        LANG[f'research.{NS}.{rid}.desc'] = r.get('description', '')
    # the milestone advancements hang off the research that leads to them
    RE = {'quantum_foam_arrival': 'quantum_foam', 'black_hole_arrival': 'black_hole_interior', 'white_hole_arrival': 'white_holes',
          'transfinite_arrival': 'transfinite_hierarchy', 'transcendence': 'beyond_the_omega', 'alpha_omega': 'omnilex'}
    for f in glob.glob(os.path.join(AD, '*.json')):
        a = json.load(open(f))
        name = os.path.basename(f)[:-5]
        if name == 'root': continue
        if a.get('parent', '').startswith('minecraft:') or 'parent' not in a:
            p = RE.get(name)
            a['parent'] = f'{NS}:research/{p}' if p in res else root
            w(f, a)
    # a starfield tile for the tab (16x16, tiled by the advancement screen)
    from PIL import Image
    import random
    rnd = random.Random(7)
    im = Image.new('RGB', (16, 16), (8, 6, 22))
    px = im.load()
    for y in range(16):
        for x in range(16):
            v = 8 + (x * 7 + y * 13) % 5
            px[x, y] = (v, v - 2, 22 + (x + y) % 6)
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        b = rnd.choice((120, 170, 230))
        px[x, y] = (b, b, min(255, b + 25))
    path = os.path.join(g['A'], 'textures', 'gui', 'advancements', 'backgrounds', 'quantum.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
