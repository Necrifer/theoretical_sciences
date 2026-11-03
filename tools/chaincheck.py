"""
Progression checker. Starting from vanilla resources and the mod's ores, repeatedly applies every recipe whose
machine is buildable (its controller AND all its multiblock casings are obtainable) and whose inputs, catalysts and
fluids are obtainable, until nothing new appears. Anything left over is unreachable - a broken chain.
Also checks that no recipe needs more slots than its machine has.
"""
import json, os, glob, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from complexity import COILS

ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, '..', 'src/main/resources')
D = os.path.join(RES, 'data/theoreticalsciences')
A = os.path.join(RES, 'assets/theoreticalsciences')
NS = 'theoreticalsciences:'
C = json.load(open(os.path.join(ROOT, 'content.json')))
M = json.load(open(os.path.join(ROOT, 'machines.json')))
MBY = {m['recipe']: m for m in M}
# TS_PHASE=sundered: the world after the Shutdown Sequence and before the Rekindling. The Transcendental and its Core are
# gone, so nothing that only came from there (its blocks, the Core's "make anything") may be needed to rebuild it.
SUNDERED = os.environ.get('TS_PHASE') == 'sundered'
UNBREAKABLE = {NS + b['id'] for b in C['blocks'] if b['kind'] in ('unbreakable', 'proof_pedestal', 'proof_toggle', 'proof_lamp', 'proof_membrane', 'mantle_breach')}


def nid(x):
    return x if ':' in x else NS + x


have = set()
fluids = {'minecraft:water', 'minecraft:lava'}
# ores (normal world access; void lattice needs the Bulk - modelled below)
bulk_ores = set()
interior_ores = {NS + x for x in ('crustal_rock', 'basalt_column', 'peridotite', 'core_iron', 'oklo_rock', 'geode_shell',
                                  'corundum', 'garnet', 'peridot', 'moissanite', 'corundum_crystal', 'garnet_crystal', 'peridot_crystal',
                                  'moissanite_crystal')}
platonic_ores = set()
foam_ores = set()
ordinal_ores = set()
hole_ores = set()
FRONTIER_BASES = ('frontier_regolith', 'nebular_ice', 'asteroid_rock', 'pulsar_crust', 'rim_slag', 'void_glass', 'dust_cloud', 'hydrogen_cloud', 'oxygen_cloud')
frontier_ores = {NS + x for x in FRONTIER_BASES}
_LATE_WORLD = set()
if os.environ.get('TS_WAVES_PRE135') == '1' or os.environ.get('TS_LENIENT') == '1' or os.environ.get('TS_WAVES_BASE') == '1' or __name__ != '__main__':
    import round45 as _w45                  # 1.4.0's meteorites: not part of the waves the older recipes were built from
    _LATE_WORLD = {b_['id'] for b_ in _w45.BLOCKS}
    import round51 as _w51                  # 1.10.0's Accretion Frontier: likewise
    _LATE_WORLD |= {b_['id'] for b_ in _w51.BLOCKS}
for b in C['blocks']:
    if b['id'] in _LATE_WORLD:
        continue
    if b['kind'] == 'ore':
        if b.get('base') == 'bulk_stone':
            bulk_ores.add(nid(b['drops']))
            bulk_ores |= {NS + 'bulk_stone', NS + 'bulk_crystal'}
        elif b.get('base') == 'ordinal_stone':
            ordinal_ores.add(nid(b['drops']))
        elif b.get('base') in ('infall_rock', 'ejecta_rock'):
            hole_ores.add(nid(b['drops']))
        elif b.get('base') == 'foam_substrate':
            foam_ores.add(nid(b['drops']))
            foam_ores |= {NS + 'foam_substrate', NS + 'foam_bubble'}
        elif b.get('base') == 'antistone':
            pass                                  # the Antiverse: modelled below
        elif b.get('base') in ('peridotite', 'core_iron', 'oklo_rock', 'crustal_rock', 'basalt_column'):
            interior_ores.add(nid(b['drops']))                # 1.3: the Planetary Interior - modelled below
        elif b.get('base') in FRONTIER_BASES:
            frontier_ores.add(nid(b['drops']))                # 1.10: the Accretion Frontier - modelled below
        elif b.get('base') == 'axiom_stone':
            platonic_ores.add(nid(b['drops']))
            platonic_ores |= {NS + 'axiom_stone', NS + 'golden_ratio_glass'}
        else:
            have.add(nid(b['drops']))
            have.add(nid(b['id']))
if not _LATE_WORLD:
    have.add(NS + 'impact_breccia')    # 1.4.0: meteorite craters in the Overworld
TAGS = {'#c:ingots/copper': 'minecraft:copper_ingot', '#c:glass_blocks': 'minecraft:glass'}
# drops of containment-failure creatures (and their fallout) appear only once a multiblock can fail (round 29: they used
# to be given from the start, which hid that the first research and early recipes needed them)
HAZARD_DROPS = {NS + 'fractured_essence', NS + 'void_essence', NS + 'fractured_reality'}
MULTIBLOCKS = {NS + m['id'] for m in M if m.get('multiblock')}


def ok_item(x):
    if isinstance(x, dict):
        x = x.get('item') or ('#' + x['tag'])
    if x.startswith('#'):
        return True  # vanilla tags
    return x.startswith('minecraft:') or x in have


recipes = []
# 1.2: the recipe expansion (round16) reads the waves without the newer releases' recipes (r38_...), so adding content
# never reshuffles what earlier recipes ask for (the full check below still includes them)
BASE_WAVES = os.environ.get('TS_WAVES_BASE') == '1' or __name__ != '__main__'      # (imported = read by a generator)
_NEWER_RESEARCH, _NEWER_MACHINES = set(), set()
if BASE_WAVES:
    import round38 as _r38, round39 as _r39, round40 as _r40, round41 as _r41, round42 as _r42, round43 as _r43
    _NEWER_RESEARCH = {r[0] for r in _r38.RESEARCH} | {r[0] for r in _r40.RESEARCH} | {r[0] for r in _r42.RESEARCH} | {r[0] for r in _r43.RESEARCH}
    import round45 as _r45, round46 as _r46, round47 as _r47, round48 as _r48, round49 as _r49, round50 as _r50, round51 as _r51, round52 as _r52, round53 as _r53, round54 as _r54, round55 as _r55, round56 as _r56, round57 as _r57
    _NEWER_RESEARCH |= {r[0] for r in _r45.RESEARCH} | {r[0] for r in _r46.RESEARCH} | {r[0] for r in _r47.RESEARCH} | {r[0] for r in _r48.RESEARCH} | {r[0] for r in _r49.RESEARCH} | {r[0] for r in _r50.RESEARCH} | {r[0] for r in _r51.RESEARCH} | {r[0] for r in _r52.RESEARCH} | {r[0] for r in _r53.RESEARCH} | {r[0] for r in _r54.RESEARCH} | {r[0] for r in _r55.RESEARCH} | {r[0] for r in _r56.RESEARCH} | {r[0] for r in _r57.RESEARCH}
    _NEWER_MACHINES = set(_r38.MACHINE_TEXT) | {b_['id'] for b_ in _r39.BLOCKS} | {b_['id'] + '_back' for b_ in _r39.BLOCKS} | set(_r42.MACHINE_TEXT) \
        | set(_r43.MACHINE_TEXT) | set(_r45.MACHINE_TEXT) | set(_r46.MACHINE_TEXT) | set(_r47.MACHINE_TEXT) | set(_r48.MACHINE_TEXT) | set(_r49.MACHINE_TEXT) | set(_r50.MACHINE_TEXT) | set(_r51.MACHINE_TEXT) | set(_r52.MACHINE_TEXT) | set(_r53.MACHINE_TEXT) | set(_r54.MACHINE_TEXT) | set(_r55.MACHINE_TEXT) | set(_r56.MACHINE_TEXT) | set(_r57.MACHINE_TEXT)
# 1.3.5: the generators' lenient passes (round 19's altar recipes, round 21's reuse, round 24's storage blocks) read the
# waves as they were before 1.3.5, so content from 1.3.5 on never reshuffles what older recipes ask for
PRE135 = os.environ.get('TS_WAVES_PRE135') == '1' or os.environ.get('TS_LENIENT') == '1'
LATE_PREFIXES = ('r42_', 'r43_', 'r44_', 'r45_', 'r46_', 'r47_', 'r48_', 'r49_', 'r50_', 'r51_', 'r52_', 'r53_', 'r54_', 'r55_', 'r56_', 'r57_')
if PRE135:
    import round42 as _q42, round43 as _q43, round44 as _q44, round45 as _q45, round46 as _q46, round47 as _q47, round48 as _q48, round49 as _q49, round50 as _q50, round51 as _q51, round52 as _q52, round53 as _q53, round54 as _q54, round55 as _q55, round56 as _q56, round57 as _q57
    for _q in (_q42, _q43, _q44, _q45, _q46, _q47, _q48, _q49, _q50, _q51, _q52, _q53, _q54, _q55, _q56, _q57):
        _NEWER_RESEARCH |= {r[0] for r in _q.RESEARCH}
        _NEWER_MACHINES |= set(_q.MACHINE_TEXT)
for f in glob.glob(os.path.join(D, 'recipe/**/*.json'), recursive=True):
    if PRE135 and (os.path.basename(f).startswith(LATE_PREFIXES) or (os.path.basename(os.path.dirname(f)) == 'crafting'
                                                              and os.path.basename(f)[:-5] in _NEWER_MACHINES)):
        continue
    if BASE_WAVES and (os.path.basename(f).startswith(('r38_', 'r39_', 'r40_', 'r41_', 'r42_', 'r43_', 'r44_', 'r45_', 'r46_', 'r47_', 'r48_', 'r49_', 'r50_', 'r51_', 'r52_', 'r53_', 'r54_', 'r55_', 'r56_', 'r57_')) or (os.path.basename(os.path.dirname(f)) == 'crafting'
                                                                  and os.path.basename(f)[:-5] in _NEWER_MACHINES)):
        continue
    r = json.load(open(f))
    r['_file'] = os.path.relpath(f, D)
    recipes.append(r)

machine_parts = {}
for m in M:
    parts = {nid(m['id'])}
    if m['multiblock']:
        mb = json.load(open(os.path.join(A, 'multiblocks', m['mb_id'] + '.json')))
        for k, ids in mb['key'].items():
            parts.add(ids[0])  # first option is the casing (others are ports)
    machine_parts[m['recipe']] = parts

errors = []
for r in recipes:
    t = r['type'].split(':')[1]
    if t in MBY:
        m = MBY[t]
        n_in = len(r.get('item_inputs', [])) + len(r.get('catalysts', [])) + len(r.get('optional_inputs', []))
        n_fin = len(r.get('fluid_inputs', [])) + len(r.get('optional_fluid_inputs', []))
        checks = [('item inputs (+catalysts, other mods)', n_in, m['inputs']), ('fluid inputs (+other mods)', n_fin, m['fluid_inputs']),
                  ('item outputs', len(r.get('item_outputs', [])), m['outputs']), ('fluid outputs', len(r.get('fluid_outputs', [])), m['fluid_outputs'])]
        for what, n, cap in checks:
            if n > cap:
                errors.append(f"{r['_file']}: {n} {what} but {m['id']} only has {cap}")

changed = True
dim_open = False
# ---------------------------------------------------------------- research (round 18): recipes need their research, and
# research needs its prerequisites, its item cost, the symbols of its derivation and Insight - which comes from
# observations with the Spectral Analyzer (finite) and, renewably, from machines that think (see INSIGHT_RECIPES).
RESEARCH = {os.path.basename(f)[:-5]: json.load(open(f)) for f in glob.glob(os.path.join(D, 'ts_research', '*.json'))
            if os.path.basename(f)[:-5] not in _NEWER_RESEARCH}
GLYPHS = {}
for _gf in glob.glob(os.path.join(D, 'ts_glyphs', '*.json')):
    GLYPHS.update(json.load(open(_gf)))
known = set()
spent = 0
LENIENT = os.environ.get('TS_LENIENT') == '1'      # report every research gap at once instead of stopping at the first
GLYPH_LATE, INSIGHT_SHORT = [], []
ran = set()                    # machine types that have run at least one recipe (observable while working)
INSIGHT_RENEWABLE = [False]
INSIGHT_RECIPES = {'matrioshka/contemplate.json'}
for _r in recipes:
    if _r.get('insight_per_run'):
        INSIGHT_RECIPES.add(_r['_file'].replace('recipe/', '').replace(os.sep, '/'))
OBS_BLOCKS = json.load(open(os.path.join(D, 'tags', 'block', 'observable.json')))['values']
OBS_ENT = json.load(open(os.path.join(D, 'tags', 'entity_type', 'observable.json')))['values']
GRANTS = {'conquered_alpha_omega': NS + 'proof_of_everything', 'the_sundering': NS + 'omnipotence_shard', 'the_rekindling': NS + 'rekindled_spark'}
DIMS = [('platonic_realm', 'platonic_compass'), ('the_bulk', 'brane_shifter'), ('quantum_foam', 'planck_diver'), ('transfinite_hierarchy', 'cantor_key'),
        ('black_hole_interior', 'horizon_diver'), ('white_hole_interior', 'horizon_diver'), ('antiverse', 'cpt_mirror'), ('hyperdimensional_reality', 'axiom_key'),
        ('living_proof', 'omnilex'), ('transcendental', 'transcendent_stone')]


def _glyph_ok(tok):
    g = GLYPHS.get(tok)
    if g is None: return True
    u = g.get('unlock', {})
    if u.get('start'): return True
    if 'research' in u: return u['research'].split(':')[-1] in known
    if 'item' in u: return nid(u['item']) in have
    return False


def _puzzle_glyphs(d):
    pz = d.get('puzzle')
    if not pz: return []
    toks = (pz.get('form') or pz.get('expr', '')).split() + pz.get('lhs', '').split()
    return [t for t in toks if t not in ('(', ')', ',') and not _re_num.fullmatch(t)]


import re as _re0
_re_num = _re0.compile(r'-?\d+(\.\d+)?([eE]-?\d+)?')


def insight_available():
    n = 3 * 8                                                          # the overworld, the Nether and the End
    n += sum(8 for dd, key in DIMS if NS + key in have)
    n += 6 * len(OBS_ENT)
    n += sum(3 for b in OBS_BLOCKS if b.startswith('minecraft:') or b in have)
    n += sum(12 if MBY[t]['multiblock'] else 4 for t in ran if t in MBY)
    return n


USE_RESEARCH = {'axiom_key': 'reality_breach', 'brane_shifter': 'brane_cosmology', 'platonic_compass': 'platonic_realm', 'planck_diver': 'quantum_foam', 'cantor_key': 'transfinite_hierarchy', 'horizon_diver': 'black_hole_interior', 'cpt_mirror': 'cpt_symmetry', 'core_drill': 'plate_tectonics', 'light_sail': 'galactic_dynamics'}   # the dimension items refuse to work without their research


def _use(item):
    return NS + item in have and USE_RESEARCH.get(item, '') in known | {''}


def _env_projectors(r):
    e = r.get('environment') or {}
    out = []
    if 'gravity_min' in e or 'gravity_max' in e: out.append('graviton_field_projector')
    if 'dimensions_min' in e: out.append('dimensional_lift_projector')
    if 'time_min' in e or 'time_max' in e: out.append('chronal_field_projector')
    if 'vacuum_min' in e: out.append('vacuum_state_projector')
    return out


PEDESTALS = ['resonance_pedestal', 'harmonic_pedestal', 'absolute_pedestal']


def _conv_ok(r):
    if NS + 'convergence_altar' not in have: return False
    if not any(NS + p in have for p in PEDESTALS[r.get('tier', 1) - 1:]): return False
    return not r.get('research') or r['research'].split(':')[-1] in known


def research_ok(rid):
    return rid in known
# the Core Forge's recipes, read from the Java source: new Recipe("id", i("out", n), List.of(i("a", n), ...))
import re as _re
FORGE = []
_src = open(os.path.join(ROOT, '..', 'src/main/java/com/theoreticalsciences/transcend/CoreForge.java')).read()
for _m in _re.finditer(r'new Recipe\("\w+", i\("(\w+)", \d+\), List\.of\((.*?)\)\)', _src, _re.S):
    FORGE.append((NS + _m.group(1), [NS + x for x in _re.findall(r'i\("(\w+)"', _m.group(2))]))
assert FORGE, 'no Core Forge recipes found'
WAVE, FWAVE = {}, {}       # the pass on which each item / fluid first became obtainable (progression order)
_pass = 0
while changed:
    for x in have: WAVE.setdefault(x, _pass)
    for x in fluids: FWAVE.setdefault(x, _pass)
    _pass += 1
    changed = False
    if not HAZARD_DROPS <= have and have & MULTIBLOCKS:
        have |= HAZARD_DROPS
        changed = True
    if not dim_open and _use('axiom_key'):
        dim_open = True
        for x in ('arbor_heartwood', 'arbor_heartwood_sample', 'bubble_universe_sample', 'bubble_universe'):
            have.add(NS + x)
        fluids.add(NS + 'primordial_law')
        changed = True
    if _use('brane_shifter') and not bulk_ores <= have:
        have |= bulk_ores
        have |= {NS + 'brane_tension_fiber', NS + 'graviton_wisp_core', NS + 'moduli_crystal'}     # Bulk natives' drops
        changed = True
    if _use('platonic_compass') and not platonic_ores <= have:
        have |= platonic_ores
        have |= {NS + 'fractal_core', NS + 'proof_fragment', NS + 'fractal_lattice_block', NS + 'koch_crystal'}   # natives' drops, landmarks
        changed = True
    if _use('planck_diver') and not foam_ores <= have:
        have |= foam_ores
        have |= {NS + 'false_vacuum_membrane'}                         # Vacuum Drifters
        fluids.update({NS + 'schrodinger_limit', NS + 'relativity_breakdown'})   # the Foam's floor and ceiling
        changed = True
    if _use('cantor_key') and not ordinal_ores <= have:
        have |= ordinal_ores | {NS + x for x in ('ordinal_stone', 'hilbert_brick', 'aleph_glass', 'hotel_key')}
        changed = True
    if _use('horizon_diver') and (NS + 'geodesic_anchor') in have and not hole_ores <= have:
        have |= hole_ores | {NS + x for x in ('infall_rock', 'ejecta_rock', 'horizon_glass')}
        changed = True
    if (NS + 'pleroma_confluence') in have and (NS + 'pleroma') not in fluids and \
            all(NS + f['id'] in fluids for f in C['fluids'] if f['id'] not in ('pleroma', 'mod_essence')):
        fluids.add(NS + 'pleroma')      # the Confluence turns every other fluid into Pleroma
        changed = True
    if not _LATE_WORLD and _use('light_sail') and not frontier_ores <= have:
        import round51 as _f51                                 # 1.10: under sail to the Accretion Frontier - its rock, ores, lakes
        have |= frontier_ores                                  # and its creatures (The Binary needs a merger record to summon)
        have |= {nid(d[0]) for c_, (_, ds) in _f51.CREATURES.items() if c_ != 'binary_serpent' for d in ds}
        fluids.update({NS + 'accretion_disk_plasma', NS + 'tidal_plasma'})
        changed = True
    if (NS + 'warp_bubble_core') in have and (NS + 'anchored_warp_bubble') not in have:
        have.add(NS + 'anchored_warp_bubble')                  # 1.12: a core, used, becomes an anchored bubble
        changed = True
    if not _LATE_WORLD and (NS + 'gw_record_bbh') in have and (NS + 'light_sail') in have and (NS + 'merger_remnant') not in have:
        have |= {NS + 'chirp_signal_record', NS + 'merger_remnant'}
        changed = True
    if _use('core_drill') and not interior_ores <= have:
        have |= interior_ores                                  # 1.3: through the Mantle Breach
        fluids.update({NS + 'basaltic_magma', NS + 'deep_brine'})
        changed = True
    if _use('cpt_mirror') and (NS + 'penning_harness') in have and (NS + 'antistone') not in have:
        # the Antiverse: its ores, its creatures' drops and its sea
        have |= {NS + x for x in ('antistone', 'antisoil', 'raw_antiiron', 'raw_antigold', 'anticarbon_crystal', 'antisilicon_shard',
                                  'antihydrogen_crystal', 'antiquark_shard', 'antihelium_nucleus')}
        fluids.add(NS + 'antiwater')
        changed = True
    if (NS + 'gene_sampler') in have and (NS + 'genome_sample') not in have:
        have |= {NS + 'genome_sample', NS + 'xenogenome_sample'}      # sampled from creatures (xeno: any of the mod's own)
        changed = True
    if (NS + 'omnilex') in have and (NS + 'proof_of_everything') not in have:
        # Alpha Omega: an Omnilex thrown into the Foam; the Living Proof's furniture can be picked in creative only (it counts as seen)
        # (its unbreakable walls, floor and observer plates are never obtainable in survival)
        have |= {NS + 'proof_of_everything'}
        changed = True
    if (NS + 'omnium_singularity') in have and (NS + 'pleroma') in fluids and (NS + 'proof_of_everything') in have and (NS + 'transcendent_stone') not in have:
        # the impossible recipe: its "output" is the Transcendental, where Omnipotency makes everything
        if not SUNDERED:
            have |= {NS + 'transcendent_stone', NS + 'transcendent_glass', NS + 'return_rift'}
            have |= {NS + 'omnipotence_casing'}     # unbreakable: only from the Core's "make anything"
        if (NS + 'omnipotence_shard') not in have:
            have |= {NS + 'omnipotence_shard', NS + 'transcendental_echo'}     # what the Shutdown leaves
            changed = True
        if not SUNDERED: changed = True
    if (NS + 'transcendent_stone') in have and (NS + 'omnipotence_shard') not in have:
        # round 15: the Shutdown Sequence leaves Shards of Omnipotence and the Echo of the Transcendental
        have |= {NS + 'omnipotence_shard', NS + 'transcendental_echo'}
        changed = True
    if (NS + 'rekindled_spark') in have and SUNDERED and (NS + 'transcendent_stone') not in have:
        pass                                # the sundered check ends at the Rekindling
    elif (NS + 'rekindled_spark') in have:
        # the reborn Core's Forge (recipes in transcend/CoreForge.java)
        for out_, ins_ in FORGE:
            if out_ not in have and all(x in have for x in ins_):
                have.add(out_)
                changed = True
    for g_, it_ in GRANTS.items():
        if it_ in have and g_ not in known:
            known.add(g_)
            changed = True
    _avail = insight_available()
    for rid in sorted(RESEARCH, key=lambda k: (RESEARCH[k].get('insight', 0), k)):
        d = RESEARCH[rid]
        if rid in known or d.get('grant_only'):
            continue
        if not all(x.split(':')[-1] in known for x in d.get('requires', [])):
            continue
        if not all(ok_item(c) for c in d.get('cost', [])):
            continue
        if not all(_glyph_ok(t) for t in _puzzle_glyphs(d)):
            if not LENIENT: continue
            GLYPH_LATE.append((rid, [t for t in _puzzle_glyphs(d) if not _glyph_ok(t)]))
        if not INSIGHT_RENEWABLE[0] and spent + d.get('insight', 0) > _avail:
            if not LENIENT: continue
            INSIGHT_SHORT.append((rid, spent + d.get('insight', 0) - _avail))
        known.add(rid)
        spent += d.get('insight', 0)
        changed = True
    for r in recipes:
        t = r['type']
        outs = []
        if t.startswith('minecraft:crafting'):
            ins = list(r.get('key', {}).values()) if 'key' in r else r.get('ingredients', [])
            if all(ok_item(i) for i in ins):
                outs = [r['result']['id']]
        elif t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:stonecutting'):
            if ok_item(r['ingredient']):
                outs = [r['result']['id']]
        elif t in (NS + 'extreme_shaped', NS + 'extreme_shapeless', NS + 'compaction'):
            station = NS + ('singularity_compactor' if t.endswith('compaction') else 'extreme_assembly_table')
            ins = list(r['key'].values()) if 'key' in r else r.get('ingredients') or [r['ingredient']]
            if station in have and all(ok_item(i) for i in ins):
                outs = [r['result']['id']]
        elif t in (NS + 'extreme_dynamic', NS + 'extreme_cluster'):
            # the parts that follow the game (other mods' singularities, crops, drops) are other mods' business
            if NS + 'extreme_assembly_table' in have and all(ok_item(i) for i in r.get('ingredients', [])) and 'result' in r:
                outs = [r['result']['id']]
        elif t == NS + 'convergence':
            # the Convergence Altar: the altar, pedestals good enough, the core and every pedestal item, its research
            if _conv_ok(r) and ok_item(r['core']) and all(ok_item(i) for i in r['pedestals']):
                outs = [r['result']['id']]
        else:
            tid = t.split(':')[1]
            if tid not in machine_parts or not all(p in have for p in machine_parts[tid]):
                continue
            if r.get('min_tier', 0) and NS + COILS[r['min_tier'] - 1] not in have:
                continue                    # the structure needs coils of at least this tier
            # round 36: machines that only work in one dimension need the way into it (MachineType.allowedDimensions)
            dim_item = {'vacuum_harvesting': 'planck_diver', 'omnilex_simulation': 'axiom_key'}.get(tid)
            if dim_item and not _use(dim_item):
                continue
            # recipe-level dimension conditions (environment.dimensions): one of them must be enterable
            rdims = (r.get('environment') or {}).get('dimensions') or []
            _rd = dict(DIMS, planetary_interior='core_drill', accretion_frontier='light_sail')     # (1.12: recipes in the new dimensions)
            if rdims and not any(d.split(':')[-1] in ('overworld', 'the_nether', 'the_end') or _use(_rd.get(d.split(':')[-1], '')) for d in rdims):
                continue
            if r.get('research') and r['research'].split(':')[-1] not in known:
                continue                    # its research is not done yet
            if not all(NS + pj in have for pj in _env_projectors(r)):
                continue                    # needs a field (gravity, dimensions, time, vacuum) no projector can make yet
            ins = [i for i in r.get('item_inputs', [])] + r.get('catalysts', [])
            if all(ok_item(i) for i in ins) and all(f['fluid'] in fluids for f in r.get('fluid_inputs', [])):
                outs = [o['id'] for o in r.get('item_outputs', [])]
                if tid not in ran:
                    ran.add(tid)
                    changed = True
                if r['_file'].replace('recipe/', '').replace(os.sep, '/') in INSIGHT_RECIPES and not INSIGHT_RENEWABLE[0]:
                    INSIGHT_RENEWABLE[0] = True
                    changed = True
                for fo in r.get('fluid_outputs', []):
                    if fo['id'] not in fluids:
                        fluids.add(fo['id'])
                        changed = True
        for o in outs:
            if o not in have:
                have.add(o)
                changed = True
    # fluid cells give access to fluids and back
    for fl in C['fluids']:
        if NS + fl['id'] in fluids and NS + fl['id'] + '_cell' not in have:
            have.add(NS + fl['id'] + '_cell')
            changed = True

all_items = {NS + 'extreme_assembly_table', NS + 'singularity_compactor', NS + 'pleroma_confluence'} | {NS + i['id'] for i in C['items']} | {NS + b['id'] for b in C['blocks'] if b['kind'] not in ('ore',)} | {NS + m['id'] for m in M}
missing = sorted(x for x in all_items - have if x.split(':')[1] not in ('bubble_universe', 'oracle_lemma', 'laplaces_demon', 'proof_wall', 'proof_floor', 'observer_plate', 'proof_membrane', 'answer_pedestal', 'axiom_toggle', 'axiom_lamp', 'mantle_breach', 'inner_core_boundary',
                                                                                     'akashic_record', 'dynamic_singularity', 'singularity_cluster', 'simulated_universe', 'specimen_bundle') and not x.endswith('_ore') and x.split(':')[1] != 'antihydrogen_ice')
missing_research = sorted(r for r in RESEARCH if r not in known and not (SUNDERED and r == 'the_rekindling'))
missing_fluids = sorted(NS + f['id'] for f in C['fluids'] if NS + f['id'] not in fluids and f['id'] != 'mod_essence')  # Essences are made in code (transcend/Essences)
# round 35: what can be had in survival, for hiding the rest from JEI / EMI (round35.py reads it on the next pass)
if __name__ == '__main__' and not SUNDERED:
    os.makedirs(os.path.join(ROOT, 'out'), exist_ok=True)
    # (only the normal run writes it: the after-the-Shutdown check must not leave its smaller set for gen_data)
    if not SUNDERED:
        json.dump(sorted(have), open(os.path.join(ROOT, 'out', 'reachable.json'), 'w'))
if __name__ == '__main__':
    for e in errors:
        print('SLOT ERROR', e)
    print('unreachable items:', len(missing))
    for x in missing:
        print('  ', x)
if __name__ == '__main__':
    print('unreachable fluids:', missing_fluids)
    if LENIENT:
        print('research whose derivation needs symbols not learnable yet:', len(GLYPH_LATE))
        for rid_, gl_ in GLYPH_LATE: print('   ', rid_, sorted(set(gl_)))
        print('research short of Insight (before any renewable source):', len(INSIGHT_SHORT), INSIGHT_SHORT[:8])
    print('unreachable research:', len(missing_research), '(insight: %d spent, %d from observations, renewable source %s)'
          % (spent, insight_available(), 'reached' if INSIGHT_RENEWABLE[0] else 'NOT reached'))
    for x in missing_research:
        d = RESEARCH[x]
        why_ = []
        if d.get('grant_only'): why_.append('granted by an event that never happens')
        why_ += ['needs research ' + q.split(':')[-1] for q in d.get('requires', []) if q.split(':')[-1] not in known]
        why_ += ['needs item ' + str(c.get('item')) for c in d.get('cost', []) if not ok_item(c)]
        why_ += ['needs symbol ' + t for t in _puzzle_glyphs(d) if not _glyph_ok(t)]
        if not why_: why_.append('not enough Insight (%d)' % d.get('insight', 0))
        print('  ', x, '-', '; '.join(why_))
    if SUNDERED:
        ok_ = NS + 'rekindled_spark' in have
        print('after the Shutdown: the Transcendental', 'CAN' if ok_ else 'CANNOT', 'be rebuilt from what is left')
        if not (len(sys.argv) > 2 and sys.argv[1] == 'why'):
            sys.exit(0 if ok_ and not errors else 1)
    if len(sys.argv) > 2 and sys.argv[1] == 'why':
        pass
    else:
        sys.exit(1 if (errors or missing or missing_fluids or missing_research) else 0)


def why(item):
    """Debug helper: python3 chaincheck.py why <item>"""
    for r in recipes:
        outs = [o['id'] for o in r.get('item_outputs', [])] + ([r['result']['id']] if 'result' in r else [])
        if item not in outs:
            continue
        t = r['type']
        lack = []
        if t == NS + 'convergence':
            lack += [str(i) for i in [r['core']] + r['pedestals'] if not ok_item(i)]
            if not _conv_ok(r): lack.append('altar / pedestals of grade %s / research' % r.get('tier', 1))
        elif t in (NS + 'extreme_shaped', NS + 'extreme_shapeless', NS + 'compaction'):
            ins = list(r['key'].values()) if 'key' in r else r.get('ingredients') or [r['ingredient']]
            lack += [str(i) for i in ins if not ok_item(i)]
            lack += [x for x in (NS + 'extreme_assembly_table', NS + 'singularity_compactor') if x not in have]
        elif not t.startswith('minecraft:'):
            tid = t.split(':')[1]
            lack += [p for p in machine_parts.get(tid, []) if p not in have]
            if r.get('research') and r['research'].split(':')[-1] not in known:
                lack.append('research ' + r['research'].split(':')[-1])
            lack += [NS + pj for pj in _env_projectors(r) if NS + pj not in have]
            lack += [str(i) for i in r.get('item_inputs', []) + r.get('catalysts', []) if not ok_item(i)]
            lack += [f['fluid'] for f in r.get('fluid_inputs', []) if f['fluid'] not in fluids]
        else:
            ins = list(r.get('key', {}).values()) if 'key' in r else r.get('ingredients', [r.get('ingredient')])
            lack += [str(i) for i in ins if i and not ok_item(i)]
        print(r['_file'], 'missing:', lack)


if len(sys.argv) > 2 and sys.argv[1] == 'why':
    for it in sys.argv[2:]:
        print('==', it)
        why(NS + it if ':' not in it else it)
