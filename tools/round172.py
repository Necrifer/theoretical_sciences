"""
1.7.2: the endgame rework, last step (design document, 10 November).

  * The Consilience Core: all seven Field Syntheses (Gamma) and all six final-tier signature materials, brought together
    on the Convergence Engine's thirteen altars. The Core Forge needs one present to forge an Eschaton Core, and the
    Forged Omnipotence Core takes four into itself.
  * The balance pass, from the cost check's report: the seven Field Syntheses now cost about the same as each other,
    and each signature material is made from modest things of its own structure (several had copied the dearest
    recipe of theirs; the Eschaton Steel asked for sixteen Eschaton Cores).
  * Narrower console windows towards the end (tools/r172_windows.py).
  * The four magic mods' optional routes: with Thaumaturge, Botania, Nature's Aura or Astral Sorcery installed, each
    Field Synthesis has a second recipe that skips one machine of its chain.

Run:  python3 tools/r172_windows.py apply     (first: the optional routes copy the narrowed windows)
      python3 tools/round172.py
"""
import copy, glob, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T
import check_progression as CP
import r172_windows as W

L = T.LANG
R = os.path.join(D, 'recipe')
SYN = ['unified_field_lattice', 'perennial_catalyst_matrix', 'proof_lattice', 'living_archive', 'engineered_mind', 'planetary_survey', 'deep_record']
E2 = ['dyson_lattice_panel', 'stellar_wind_condensate', 'recursive_thought_shell', 'embryonic_vacuum']
E3 = ['compiled_law', 'lex_prima', 'breach_shard', 'pure_concept_ingot', 'diagonal_ingot', 'distilled_axiom']
E4 = ['transcendent_alloy', 'event_horizon_lens', 'pleromatic_weave', 'confluent_essence', 'eschaton_steel', 'convergent_matter']
SIGS = {ts(x) for x in E2 + E3 + E4}


def is_syn(i):
    return 'items' in i or i.get('item', '').split(':')[-1] in SYN


# ====================================================================== the balance pass: the Field Syntheses
def set_inputs(path, ins):
    def f(r):
        r['item_inputs'] = [{'item': ts(i), 'count': n} for i, n in ins]
    edit_json(os.path.join(R, path), f)


# (was: 2 beams, 8 Entangled Bus Bars, 8 Atto Capacitors, 4 Qubit Dies: thirty times the others)
set_inputs('femto_assembly/r170_unified_field_lattice.json', [('coherent_field_beam', 2), ('photonic_board', 8), ('atto_capacitor', 1), ('qubit_die', 4)])
# (was 2 Atto Capacitors)
set_inputs('framework_assembly/r170_framework_catalyst.json', [('doped_catalyst_bed', 1), ('metal_organic_framework', 2), ('atto_capacitor', 1)])
# (was 2 Certified Theorems)
set_inputs('automaton_growth/r170_proof_lattice.json', [('certified_theorem', 4), ('strange_attractor', 4), ('knotted_polymer', 2)])
# (was 1 Chronon Capacitor)
set_inputs('revival/r170_living_archive.json', [('reconstructed_genome', 2), ('dated_stromatolite', 2), ('field_ration', 16), ('photonic_neuron', 8), ('chronon_capacitor', 6)])
# (was 1 Mapped Mind)
set_inputs('load_testing/r170_engineered_mind.json', [('mapped_mind', 2), ('rated_i_beam', 8)])


# ====================================================================== the balance pass: the signature materials
def resign(rtype, out, own, fluids=None, catalysts='keep'):
    """Replace what a signature recipe takes of its own structure; its syntheses and the tier below's materials stay."""
    def f(r):
        keep = [i for i in r['item_inputs'] if is_syn(i) or i.get('item') in SIGS]
        r['item_inputs'] = [{'item': ts(i), 'count': n} for i, n in own] + keep
        if fluids is not None:
            if fluids:
                r['fluid_inputs'] = [{'fluid': ts(x), 'amount': a} for x, a in fluids]
            else:
                r.pop('fluid_inputs', None)
        if catalysts is None:
            r.pop('catalysts', None)
        r.pop('scaled', None)
        r.pop('expanded', None)
        r['unscaled'] = True
        assert len(r['item_inputs']) <= 16, out
    edit_json(os.path.join(R, rtype, 'r171_' + out + '.json'), f)


# the fluids each keeps (smaller batches than the recipes they were first copied from)
FL = {
    'embryonic_vacuum': [('reheating_plasma', 1000), ('inflaton_field', 1000)],
    'compiled_law': [('psionic_ether', 4000)],
    'breach_shard': [('dark_energy', 64000), ('antihydrogen', 16000), ('relativity_breakdown', 16000), ('hypernova_plasma', 16000), ('superfluid_helium', 16000)],
    'diagonal_ingot': [('synaptic_fluid', 16000)],
    'distilled_axiom': [('primordial_law', 16000), ('strange_matter', 8000)],
    'transcendent_alloy': [('hypernova_plasma', 16000), ('causal_solder', 9216), ('liquid_spacetime', 4000), ('neutrino_flux', 4000), ('antihydrogen', 4000)],
    'pleromatic_weave': [('pleroma', 16000)],
    'eschaton_steel': [('pleroma', 16000)],
}


def fl(rtype, out, scale=None):
    return FL[out]


# cosmic engineering
resign('dyson', 'dyson_lattice_panel', [('stellar_core', 1), ('stellar_frame_casing', 16)])       # (not satellites or shell segments: those slots are the swarm's)
resign('stellar_thrust', 'stellar_wind_condensate', [('stellar_core', 1), ('shkadov_mirror', 64), ('stellar_frame_casing', 16)])
resign('matrioshka', 'recursive_thought_shell', [('engram', 16), ('computronium_ingot', 32), ('computronium_block', 1), ('superintelligence_core', 1)])
resign('universe_forging', 'embryonic_vacuum', [('true_vacuum_seed', 1), ('baryon_asymmetry_crystal', 16)], fluids=fl('universe_forging', 'embryonic_vacuum', 1 / 256.0))
# beyond physics
resign('reality_compilation', 'compiled_law', [('source_code_fragment', 2)], fluids=fl('reality_compilation', 'compiled_law', 1 / 64.0))
resign('omnilex_simulation', 'lex_prima', [('eternity_catalyst', 1), ('bubble_universe_sample', 4)])
resign('reality_breach', 'breach_shard', [('paradox_residue', 16), ('ordinal_shard', 16)], fluids=fl('reality_breach', 'breach_shard', 1 / 16.0))
resign('concept_compression', 'pure_concept_ingot', [('surreal_number', 1)])
resign('transfinite_computation', 'diagonal_ingot', [('godel_sentence', 1), ('power_set', 16), ('formal_proof_tree', 16)], fluids=fl('transfinite_computation', 'diagonal_ingot', 1 / 16.0))
resign('axiom_distillation', 'distilled_axiom', [('axiom_fragment', 64), ('source_code_fragment', 2)], fluids=fl('axiom_distillation', 'distilled_axiom', 1 / 16.0))
# the final tier
resign('transcendent_forging', 'transcendent_alloy', [('hypernova_alloy_ingot', 64), ('transcendent_metal_ingot', 1)], fluids=fl('transcendent_forging', 'transcendent_alloy', 1 / 32.0))
resign('horizon_simulation', 'event_horizon_lens', [('micro_singularity', 16), ('graviton_crystal', 16)])
resign('pleroma_reconstitution', 'pleromatic_weave', [('transcendent_metal_ingot', 1), ('reality_anchor', 4)], fluids=fl(0, 'pleromatic_weave'), catalysts=None)
resign('eschaton_founding', 'eschaton_steel', [('cosmic_neutronium_ingot', 64), ('hypernova_alloy_ingot', 64), ('transcendent_metal_ingot', 1)], fluids=fl(0, 'eschaton_steel'), catalysts=None)
resign('convergence_automation', 'convergent_matter', [('coherent_wiring_harness', 16), ('entangled_bus_bar', 4), ('chronoplastic_gasket', 4)])


def confl(r):
    r['fluid_inputs'] = [{'fluid': ts(f), 'amount': 1000} for f in ('quintessence', 'alkahest', 'mathesis', 'panacea', 'anima_mundi')]
edit_json(os.path.join(R, 'pleroma_confluence', 'r171_confluent_essence.json'), confl)

# ====================================================================== the Consilience Core
item('consilience_core', 'Consilience Core',
     "Every field brought to one point: physics, chemistry, mathematics, biology, the engineered mind, the surveyed planet and the deep record, "
     "with the six last materials to hold them. Consilience is the old word for knowledge from different fields agreeing; this is the agreement, "
     "made solid. Made in the Convergence Engine.")
tpl = json.load(open(os.path.join(R, 'eschaton_founding', 'r43_eschaton_core.json'), encoding='utf-8'))
cc = {'type': ts('convergence_automation'),
      'item_inputs': [{'type': 'neoforge:components', 'items': ts(s), 'components': {ts('synthesis_level'): 3}, 'count': 1} for s in SYN]
      + [{'item': ts(m), 'count': 1} for m in E4],
      'item_outputs': [{'id': ts('consilience_core'), 'count': 1}],
      'energy': 2000000000, 'duration': 12000, 'research': tpl.get('research', ts('pleroma')), 'unscaled': True}
assert len(cc['item_inputs']) == 13
dump(os.path.join(R, 'convergence_automation', 'r172_consilience_core.json'), cc)

# ====================================================================== the Pleroma Confluence: a band, not a threshold
for rid in ('r171_pleroma', 'r171_confluent_essence'):
    def f(r):
        r['physics']['windows'] = [{'q': 'Re', 'min': 10000, 'max': 1e9}, {'q': 'mix', 'min': 3, 'max': 5}]
    edit_json(os.path.join(R, 'pleroma_confluence', rid + '.json'), f)
P = 'physics.%s.pleroma_confluence.' % NS
L[P + 'd.mix.desc'] = 'Time in the basin over blend time: between 3 and 5. Under 3 the five have not become one; over 5 they have sat long enough to part again.'


def conf_model(m):
    for d in m['derived']:
        if d['id'] == 'mix':
            d['formula'] = 'residence time V/Q over blend time: between 3 and 5 the five are one fluid as they leave'
edit_json(os.path.join(D, 'ts_physics', 'pleroma_confluence.json'), conf_model)

# ====================================================================== the magic mods' optional routes
def magic(mod, machine, base, rid, ins, fins=None, duration=None):
    """A second recipe for a Field Synthesis that skips one machine of its chain, loaded only when the mod is installed.
    Built on the chain's own last step (so it is tuned on the same console, to the same windows)."""
    b = json.load(open(os.path.join(R, base), encoding='utf-8'))
    r = {'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': mod}], 'type': b['type']}
    r['item_inputs'] = [{'item': i if ':' in i else ts(i), 'count': n} for i, n in ins]
    f = [{'fluid': x if ':' in x else ts(x), 'amount': a} for x, a in fins] if fins is not None else b.get('fluid_inputs', [])
    if f:
        r['fluid_inputs'] = f
    r['item_outputs'] = b['item_outputs']
    for k in ('energy', 'duration', 'research', 'unscaled', 'physics', 'catalysts'):
        if k in b:
            r[k] = copy.deepcopy(b[k])
    if duration:
        r['duration'] = duration
    dump(os.path.join(R, os.path.dirname(base), rid + '.json'), r)
    return r


MAGIC = [
    # Astral Sorcery: starlight does the Synchrotron's work
    ('astralsorcery', 'femtoscale_assembly_line', 'femto_assembly/r170_unified_field_lattice.json', 'r172_unified_field_lattice_starlight',
     [('charged_lattice_seed', 4), ('photonic_board', 8), ('atto_capacitor', 1), ('qubit_die', 4), ('astralsorcery:starmetal_ingot', 4)],
     [('astralsorcery:liquid_starlight', 4000)]),
    # Thaumaturge: vis crystals and Salis Mundus do the Chelation Column's work
    ('thaumaturge', 'regeneration_kiln', 'catalyst_regeneration/r170_perennial_catalyst_matrix.json', 'r172_perennial_catalyst_matrix_vis',
     [('framework_catalyst', 2), ('knotted_polymer', 2), ('thaumaturge:essentia_crystal', 8), ('thaumaturge:salis_mundus', 2)], None),
    # Astral Sorcery: a resonating gem and a lens do the Certificate Verifier's work
    ('astralsorcery', 'automaton_lattice', 'automaton_growth/r170_proof_lattice.json', 'r172_proof_lattice_constellation',
     [('verified_lemma', 8), ('strange_attractor', 4), ('knotted_polymer', 2), ('astralsorcery:resonating_gem', 2), ('astralsorcery:glass_lens', 1)], None),
    # Botania: mana does the Ancient DNA Lab's work
    ('botania', 'revival_vat', 'revival/r170_living_archive.json', 'r172_living_archive_mana',
     [('genome_library', 2), ('dated_stromatolite', 2), ('field_ration', 16), ('photonic_neuron', 8), ('chronon_capacitor', 6),
      ('botania:mana_pearl', 4), ('botania:mana_diamond', 2)], None),
    # Thaumaturge: a Brain in a Jar does the Connectome Array's work
    ('thaumaturge', 'load_test_rig', 'load_testing/r170_engineered_mind.json', 'r172_engineered_mind_jar',
     [('trained_cortex', 4), ('rated_i_beam', 8), ('thaumaturge:jar_brain', 1), ('thaumaturge:essentia_crystal', 4)], None),
    # Nature's Aura: the sky's own metal and bottled aura do the Space Elevator's work
    ('naturesaura', 'heliograph', 'heliography/r170_planetary_survey.json', 'r172_planetary_survey_aura',
     [('survey_probe', 2), ('faraday_mesh', 1), ('atto_capacitor', 1), ('naturesaura:sky_ingot', 2), ('naturesaura:aura_bottle', 4)], None),
    # Nature's Aura: the Hand of Time does the Chrono Loom's work
    ('naturesaura', 'causality_loop_engine', 'causality_loop/r170_deep_record.json', 'r172_deep_record_hand_of_time',
     [('excavation_record', 4), ('tachyon_crystal', 4), ('naturesaura:clock_hand', 1), ('naturesaura:depth_ingot', 2)], None),
]
for mod, machine, base, rid, ins, fins in MAGIC:
    magic(mod, machine, base, rid, ins, fins)

# ====================================================================== text
L['item.%s.eschaton_core.found' % NS] = 'Forged at the Core Forge, with a Consilience Core present.'
E = {}
E['materials/consilience_core'] = {'title': 'The Consilience Core', 'icon': ts('consilience_core'), 'category': 'materials', 'research': 'femtoengineering', 'pages': [
    spot(['consilience_core'], "The last material. On the $(item)Convergence Engine$()'s thirteen altars: one $(thing)Gamma$() of each of the seven $(l)Field Syntheses$(), and one of each of the six final-tier $(l)Signature Materials$(). Seven and six: thirteen.$(br2)Made in $(item)Convergence Engine$().", 'Consilience'),
    text("A Gamma is nine Betas, eighty-one Alphas, 729 of the plain item. Whewell's word, consilience, is for when findings from fields that share nothing turn out to agree. I had seven fields that had never needed each other. Now nothing at the end can be made without all of them."),
    text("The $(item)Core Forge$() needs a Consilience Core present to forge an $(item)Eschaton Core$(), and keeps it, as it keeps the Rekindled Spark. Everything the last things are made of comes through the Eschaton Core, so one Consilience Core opens all of them. The $(item)Forged Omnipotence Core$() is the exception: it takes four into itself.")]}
E['materials/magic_routes'] = {'title': 'Other Arts', 'icon': ts('planetary_survey'), 'category': 'materials', 'research': 'femtoengineering', 'pages': [
    text("Four other arts can each stand in for one machine of a Field Synthesis's chain. These are second recipes on the chain's last machine, tuned on the same console, and they only exist in a world that has that mod. None is ever needed."),
    text("$(thing)Astral Sorcery$()$(br)Unified Field Lattice: Charged Lattice Seeds, Starmetal and Liquid Starlight, skipping the Synchrotron.$(br)Proof Lattice: Verified Lemmas, Resonating Gems and a Glass Lens, skipping the Certificate Verifier.$(br2)$(thing)Thaumaturge$()$(br)Perennial Catalyst Matrix: Framework Catalysts, Vis Crystals and Salis Mundus, skipping the Chelation Column.$(br)Engineered Mind: Trained Cortices, a Brain in a Jar and Vis Crystals, skipping the Connectome Array."),
    text("$(thing)Botania$()$(br)Living Archive: Genome Libraries, Mana Pearls and Mana Diamonds, skipping the Ancient DNA Lab.$(br2)$(thing)Nature's Aura$()$(br)Planetary Survey: Survey Probes, Ingots of the Skies and Bottled Aura, skipping the Space Elevator.$(br)Deep Record: Excavation Records, the Hand of Time and Ingots of the Depths, skipping the Chrono Loom.")]}


def upd(Eall):
    e = Eall['materials/field_syntheses']
    e['pages'][0]['text'] = ("The endgame has four tiers, and the $(thing)Field Syntheses$() are the first: one per field, each made by a chain of that field's machines. "
                             "Cosmic engineering (the Dyson Array, Stellar Engine, Matrioshka Brain, Universe Forge) needs two plain syntheses per recipe; beyond physics needs an "
                             "$(thing)Alpha$() one; the final tier a $(thing)Beta$() one, and all seven between its machines. The $(l)Consilience Core$() takes a $(thing)Gamma$() of every one.")
    s = Eall['materials/signature_materials']
    s['pages'][0]['text'] = s['pages'][0]['text'].replace(' Each of the six of the final tier takes three each of all six beyond physics.',
                                                          ' Each of the six of the final tier takes three each of all six beyond physics. One of each of those six, with the seven syntheses, makes the $(l)Consilience Core$().')


edit_json(os.path.join(A, 'ts_codex/entries.json'), upd)
codex(E, {'materials': [{'entry': 'materials/consilience_core', 'x': 7, 'y': 1, 'parents': ['materials/signature_materials']},
                        {'entry': 'materials/magic_routes', 'x': 3, 'y': 3, 'parents': ['materials/field_syntheses']}]})
finish()

# ====================================================================== checks
# every changed recipe still has a console setting that meets its windows (the narrowed ones included)
bad = 0
mach = CP.machines()
for f in sorted(glob.glob(os.path.join(R, '*', 'r17[0-2]_*.json'))):
    r = json.load(open(f, encoding='utf-8'))
    rtype = os.path.basename(os.path.dirname(f))
    wins = r.get('physics', {}).get('windows')
    mid = next((m for m, _ in mach.get(rtype, []) if os.path.exists(os.path.join(D, 'ts_physics', m + '.json'))), None)
    if not wins or not mid:
        continue
    model = json.load(open(os.path.join(D, 'ts_physics', mid + '.json'), encoding='utf-8'))
    rv = dict(model.get('recipe_vars', {}))
    rv.update(r['physics'].get('vars', {}))
    if any(w['q'] in [s.get('id') for s in model.get('states', [])] for w in wins):
        continue
    ok = False
    for coil in (1, 2, 3, 4, 5):
        S = W.samples(model, rv, wins, coil)
        if S and any(all(w['min'] <= s[1][k] <= w['max'] for k, w in enumerate(wins)) for s in S):
            ok = True
            break
    if not ok and S and len(S) >= 100:
        note = 'not met by the sampler (needs the structure, as before)' if rtype in ('matrioshka', 'pleroma_reconstitution', 'omnilex_simulation', 'reality_breach', 'eschaton_founding') else 'UNREACHABLE'
        bad += note == 'UNREACHABLE'
        print('  %s %s: %s' % (rtype, os.path.basename(f), note))
if bad:
    raise SystemExit('recipes whose windows cannot be met: %d' % bad)
print('1.7.2 written: Consilience Core, %d optional routes' % len(MAGIC))
