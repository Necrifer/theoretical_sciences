"""
1.7.0: the endgame rework, first step (design document, 10 November). The seven Field Syntheses, each made by a chain
of one field's machines (every step keeps its console's physics window), their lettered levels, and the endgame's
recipes rebuilt round them: cosmic engineering needs two plain syntheses, beyond physics an Alpha one, the final tier
a Beta one, and between them the final tier needs all seven. The Dyson Array and the Stellar Engine get new recipes.

Run:  python3 tools/round170.py
"""
import copy, glob, json, os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tsgen import *          # noqa: F401,F403
import tsgen as T

L = T.LANG
R = os.path.join(D, 'recipe')


def template(path):
    return json.load(open(os.path.join(R, path), encoding='utf-8'))


def reachable_template(machine, tpl):
    """The given template, or another recipe of the same machine, whose physics windows the checker can reach."""
    model_path = os.path.join(D, 'ts_physics', machine + '.json')
    t = template(tpl)
    if 'physics' not in t or not os.path.exists(model_path):
        return t
    m = json.load(open(model_path, encoding='utf-8'))
    folder = os.path.dirname(os.path.join(R, tpl))
    for f in [os.path.join(R, tpl)] + sorted(glob.glob(os.path.join(folder, '*.json'))):
        c = json.load(open(f, encoding='utf-8'))
        if 'physics' not in c:
            continue
        c['_id'] = os.path.basename(f)
        try:
            check_reachable(machine, m, c)
            return c
        except SystemExit:
            continue
    print('  (unchecked: %s needs its structure\'s coils or tiers, which the checker cannot model)' % machine)
    t['_unchecked'] = True
    return t


def step(machine, tpl, rid, ins, outs, fins=None, energy=None, duration=None, research=None):
    """A chain step: a new recipe for `machine` copying a template's physics, so it is tuned the same way."""
    t = reachable_template(machine, tpl)
    r = {'type': t['type']}
    r['item_inputs'] = [{'item': ts(i), 'count': n} for i, n in ins]
    f = [{'fluid': ts(x), 'amount': a} for x, a in fins] if fins is not None else t.get('fluid_inputs', [])
    if f:
        r['fluid_inputs'] = f
    r['item_outputs'] = [{'id': ts(i), 'count': n} for i, n in outs]
    r['energy'] = energy or t.get('energy', 1024)
    r['duration'] = duration or t.get('duration', 400)
    r['research'] = ts(research) if research else t.get('research', ts('femtoengineering'))
    r['unscaled'] = True
    if 'physics' in t:
        r['physics'] = copy.deepcopy(t['physics'])
    rtype = t['type'].split(':')[1]
    dump(os.path.join(R, rtype, rid + '.json'), r)
    if 'physics' in t and not t.get('_unchecked') and os.path.exists(os.path.join(D, 'ts_physics', machine + '.json')):
        T.RECIPES.append((machine, rid, r))
    return r


# ====================================================================== the chains
# physics: Cyclotron -> Synchrotron -> Femtoscale Assembly Line
step('cyclotron', 'cyclotron_irradiation/r49_cobalt_57.json', 'r170_charged_lattice_seed',
     [('resonant_shard', 4), ('qubit_die', 1)], [('charged_lattice_seed', 1)], research='cyclotron_physics')
step('synchrotron', 'synchrotron/bottom_top_quarks.json', 'r170_coherent_field_beam',
     [('charged_lattice_seed', 2), ('photonic_board', 4), ('maxwell_demon_chip', 2)], [('coherent_field_beam', 1)],
     fins=[('quark_gluon_plasma', 8000), ('superconducting_solder', 2000)], energy=65536, duration=900, research='accelerator_physics')
step('femtoscale_assembly_line', 'femto_assembly/abiogenesis_crucible.json', 'r170_unified_field_lattice',
     [('coherent_field_beam', 2), ('entangled_bus_bar', 8), ('atto_capacitor', 8), ('qubit_die', 4)],
     [('unified_field_lattice', 1)], fins=[], energy=1048576, duration=1800, research='femtoengineering')
# chemistry: Catalyst Formulator -> Framework Assembler -> Chelation Column -> Regeneration Kiln
step('catalyst_formulator', 'catalyst_formulation/r64_iron_catalyst.json', 'r170_doped_catalyst_bed',
     [('iron_catalyst', 4), ('rare_earth_oxide', 2)], [('doped_catalyst_bed', 1)], energy=2048, research='heterogeneous_catalysis')
step('framework_assembler', 'framework_assembly/r65_metal_organic_framework.json', 'r170_framework_catalyst',
     [('doped_catalyst_bed', 1), ('metal_organic_framework', 2), ('atto_capacitor', 2)], [('framework_catalyst', 1)], energy=4096, research='reticular_chemistry')
step('chelation_column', 'chelation/r66_clean_fuel_salt.json', 'r170_chelated_catalyst',
     [('framework_catalyst', 1)], [('chelated_catalyst', 1)], energy=8192, research='chelation')
step('regeneration_kiln', 'catalyst_regeneration/r58_restore_iron_catalyst.json', 'r170_perennial_catalyst_matrix',
     [('chelated_catalyst', 2), ('knotted_polymer', 2)], [('perennial_catalyst_matrix', 1)], energy=65536, duration=600, research='heterogeneous_catalysis')
# mathematics: Proof Assistant -> Certificate Verifier -> Automaton Lattice
step('proof_assistant', 'formal_proof/aleph_glass_power_set.json', 'r170_verified_lemma',
     [('proof_certificate', 2), ('zero_knowledge_proof', 4), ('formal_proof_tree', 1)], [('verified_lemma', 1)], fins=[], energy=4096, duration=600,
     research='formal_logic')
step('certificate_verifier', 'certificate_check/r150_grover_oracle.json', 'r170_certified_theorem',
     [('verified_lemma', 2), ('grover_oracle', 1), ('one_time_pad', 2)], [('certified_theorem', 1)], energy=8192, research='complexity_theory')
step('automaton_lattice', 'automaton_growth/r150_von_neumann_constructor.json', 'r170_proof_lattice',
     [('certified_theorem', 2), ('strange_attractor', 4), ('knotted_polymer', 2)], [('proof_lattice', 1)], energy=65536, duration=900,
     research='cellular_automata')
# biology: Gene Sequencer -> Ancient DNA Lab -> Revival Vat
step('gene_sequencer', 'gene_sequencing/cryptochrome_magnetoreceptor.json', 'r170_genome_library',
     [('mammoth_genome', 1), ('meganeura_genome', 1), ('tomogram', 2)], [('genome_library', 1)], fins=[('nutrient_broth', 4000)], energy=16384,
     research='genomics')
step('ancient_dna_lab', 'ancient_dna/r110_mammoth_genome.json', 'r170_reconstructed_genome',
     [('genome_library', 1)], [('reconstructed_genome', 1)], energy=8192, research='ancient_dna')
step('revival_vat', 'revival/r110_mammoth.json', 'r170_living_archive',
     [('reconstructed_genome', 2), ('dated_stromatolite', 2), ('field_ration', 16), ('photonic_neuron', 8), ('chronon_capacitor', 1)], [('living_archive', 1)], energy=65536, research='de_extinction')
# mind and engineering: Neural Culture Array -> Synapse Trainer -> Connectome Array -> Load Test Rig
step('neural_culture_array', 'neural_culture/r140_myelinated_axon.json', 'r170_cultured_cortex',
     [('neural_organoid', 2), ('lipid_bilayer', 4)], [('cultured_cortex', 1)], energy=2048, research='neurobiology')
step('synapse_trainer', 'synaptic_training/r140_synaptic_lattice.json', 'r170_trained_cortex',
     [('cultured_cortex', 2), ('neural_lattice', 1), ('photonic_neuron', 2)], [('trained_cortex', 1)], energy=8192, research='synaptic_plasticity')
step('connectome_array', 'connectome_mapping/r140_engram.json', 'r170_mapped_mind',
     [('trained_cortex', 2), ('universal_grammar', 1), ('engram', 1)], [('mapped_mind', 1)], energy=65536, research='integrated_information')
step('load_test_rig', 'load_testing/r140_rated_i_beam.json', 'r170_engineered_mind',
     [('mapped_mind', 1), ('rated_i_beam', 8)], [('engineered_mind', 1)], energy=32768, duration=600, research='structural_mechanics')
# earth and space: (Ice Core Drill's cores) -> Rocket Assembly -> Space Elevator -> Heliograph
step('rocket_assembly', 'orbital_launch/r102_microgravity_hydrolox.json', 'r170_survey_probe',
     [('ice_core', 4), ('rocket_stage', 2)], [('survey_probe', 1)], energy=8192, research='rocketry')
step('space_elevator', 'elevator_lift/r140_crew_capsule.json', 'r170_orbital_survey',
     [('survey_probe', 1), ('tether_spool', 1)], [('orbital_survey', 1)], energy=65536, research='space_elevator')
step('heliograph', 'heliography/r150_faraday_mesh.json', 'r170_planetary_survey',
     [('orbital_survey', 2), ('faraday_mesh', 1), ('resonant_shard', 4), ('atto_capacitor', 1)], [('planetary_survey', 1)], energy=65536, duration=900,
     research='space_weather')
# archaeology: (Photogrammetry Gantry's records) -> Chrono Loom -> Causality Loop Engine
step('chrono_loom', 'chronoweaving/arrow_of_time_shard.json', 'r170_stratigraphic_timeline',
     [('excavation_record', 2), ('chronon_capacitor', 2), ('tree_ring_chronology', 2)], [('stratigraphic_timeline', 1)],
     fins=[('chronon_gas', 4000)], energy=8192, research='photogrammetry')
step('causality_loop_engine', 'causality_loop/bootstrap_artifact.json', 'r170_deep_record',
     [('stratigraphic_timeline', 2), ('tachyon_crystal', 4)], [('deep_record', 1)], fins=[('chronon_gas', 2000)], energy=1048576,
     research='causality_violation')
verify_recipes()

# compaction: nine of a level make one of the next, in a crafting grid
dump(os.path.join(R, 'crafting', 'r170_synthesis_compaction.json'), {'type': ts('synthesis_compaction'), 'category': 'misc'})

# ====================================================================== the endgame recipes, rebuilt round them
SYN = {'ufl': 'unified_field_lattice', 'pcm': 'perennial_catalyst_matrix', 'pl': 'proof_lattice', 'la': 'living_archive',
       'em': 'engineered_mind', 'ps': 'planetary_survey', 'dr': 'deep_record'}
# machine recipe type: (tier level, the syntheses its recipes take)
FEEDS = {
    'dyson': (0, ['ps', 'ufl']), 'stellar_thrust': (0, ['ps', 'pcm']), 'matrioshka': (0, ['em', 'pl']), 'universe_forging': (0, ['la', 'ufl']),
    'reality_compilation': (1, ['ufl', 'pl', 'dr']), 'omnilex_simulation': (1, ['la', 'pl', 'em']), 'reality_breach': (1, ['pl', 'dr', 'pcm']),
    'concept_compression': (1, ['em', 'la', 'ufl']), 'transfinite_computation': (1, ['pl', 'em', 'ps']), 'axiom_distillation': (1, ['pl', 'pcm', 'dr']),
    'transcendent_forging': (2, ['ufl', 'pcm', 'ps']), 'horizon_simulation': (2, ['dr', 'ps', 'ufl']), 'pleroma_reconstitution': (2, ['pcm', 'la', 'em']),
    'eschaton_founding': (2, ['dr', 'pcm', 'pl']), 
}
COUNT = {0: 2, 1: 1, 2: 1}         # how many of each synthesis a recipe takes, by tier


def syn_ingredient(key, level, n):
    if level == 0:
        return {'item': ts(SYN[key]), 'count': n}
    return {'type': 'neoforge:components', 'items': ts(SYN[key]), 'components': {ts('synthesis_level'): level}, 'count': n}


changed = []
for rtype, (lvl, keys) in FEEDS.items():
    for f in sorted(glob.glob(os.path.join(R, rtype, '*.json'))):
        r = json.load(open(f, encoding='utf-8'))
        if os.path.basename(f).startswith('r170_') or r.get('neoforge:conditions'):
            continue
        ins = r.setdefault('item_inputs', [])
        have = {i.get('item') or i.get('items') for i in ins}
        # each recipe takes the tier's syntheses in turn, so between them a machine's recipes use all of its set
        n = len([x for x in changed if x[0] == rtype])
        pick = [keys[n % len(keys)]] if lvl > 0 else keys[:2]
        if lvl == 2:
            pick = keys                                                # the final tier takes all three of its set
        for k in pick:
            if ts(SYN[k]) not in have:
                ins.append(syn_ingredient(k, lvl, COUNT[lvl]))
        dump(f, r)
        changed.append((rtype, os.path.basename(f), len(ins)))
print('endgame recipes rebuilt:', len(changed))
need = {}
for rtype, fn, n in changed:
    need[rtype] = max(need.get(rtype, 0), n)
json.dump(need, open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'r170_slots.json'), 'w'), indent=1)

# new recipes for the two that had one each
dump(os.path.join(R, 'dyson', 'r170_dyson_survey.json'),
     {'type': ts('dyson'), 'item_inputs': [{'item': ts('stellar_core'), 'count': 1}, {'item': ts('planetary_survey'), 'count': 2},
                                            {'item': ts('unified_field_lattice'), 'count': 1}],
      'energy': 8192, 'duration': 144000, 'research': ts('dyson_swarm')})
dump(os.path.join(R, 'dyson', 'r170_dyson_lattice.json'),
     {'type': ts('dyson'), 'item_inputs': [{'item': ts('neutron_star_core') if False else ts('stellar_core'), 'count': 2},
                                            {'item': ts('unified_field_lattice'), 'count': 4}],
      'energy': 16384, 'duration': 216000, 'research': ts('dyson_swarm')})
st = template('stellar_thrust/r140_stellar_thrust.json')
for rid, extra, flow, dur in (('r170_stellar_thrust_survey', [('planetary_survey', 2), ('perennial_catalyst_matrix', 1)], 8000, 108000),
                              ('r170_stellar_thrust_matrix', [('perennial_catalyst_matrix', 4)], 12000, 144000)):
    r = copy.deepcopy(st)
    r['item_inputs'] = [{'item': ts('stellar_core'), 'count': 1}] + [{'item': ts(i), 'count': n} for i, n in extra]
    r['fluid_outputs'] = [{'id': ts('stellar_plasma'), 'amount': flow}]
    r['duration'] = dur
    dump(os.path.join(R, 'stellar_thrust', rid + '.json'), r)
    T.RECIPES.append(('stellar_engine', rid, r))
verify_recipes()

# ====================================================================== items and text
FS = "Made in %s. Compacts: nine of one level make one of the next (Greek, then Hebrew letters)."
item('unified_field_lattice', 'Unified Field Lattice', "A lattice in which the four forces are held at the energy where they "
     "meet: a crystal of the early universe, a trillionth of a second old. " + FS % 'the Femtoscale Assembly Line')
item('perennial_catalyst_matrix', 'Perennial Catalyst Matrix', "A catalyst that heals itself: every site that is poisoned is "
     "chelated clean and rebuilt inside its framework as it works. It never wears. " + FS % 'the Regeneration Kiln')
item('proof_lattice', 'Proof Lattice', "A cellular automaton that checks itself: a web of certified theorems, each verifying its "
     "neighbours, with no gap anywhere for a false step. " + FS % 'the Automaton Lattice')
item('living_archive', 'Living Archive', "Every genome the lab has read, kept alive as a single culture that can grow any of them "
     "again. " + FS % 'the Revival Vat')
item('engineered_mind', 'Engineered Mind', "A mapped and trained cortex, built into a frame rated for the loads of a thinking "
     "machine the size of a building. " + FS % 'the Load Test Rig')
item('planetary_survey', 'Planetary Survey', "A whole world measured: its ice, its rock, its orbit and the Sun's weather on it, "
     "from the ground to the magnetopause. " + FS % 'the Heliograph')
item('deep_record', 'Deep Record', "The stratigraphic record carried back past the first town, past the first life, to the first "
     "moment: the arrow of time, read end to end. " + FS % 'the Causality Loop Engine')
for iid, name, lore in (
        ('charged_lattice_seed', 'Charged Lattice Seed', "Resonant crystal and a qubit die, irradiated in the Cyclotron until every site holds a charge. Made in the Cyclotron."),
        ('coherent_field_beam', 'Coherent Field Beam', "A beam of particles in one quantum state, wound into a ring and frozen. Made in the Synchrotron."),
        ('doped_catalyst_bed', 'Doped Catalyst Bed', "An iron catalyst laced with rare earths, its surface pocked with active sites. Made in the Catalyst Formulator."),
        ('framework_catalyst', 'Framework Catalyst', "The doped catalyst caged inside a metal-organic framework, each site held apart. Made in the Framework Assembler."),
        ('chelated_catalyst', 'Chelated Catalyst', "The framework catalyst with every poisoned site stripped by chelation. Made in the Chelation Column."),
        ('verified_lemma', 'Verified Lemma', "A lemma whose every step a machine has checked. Made in the Proof Assistant."),
        ('certified_theorem', 'Certified Theorem', "Lemmas joined into a theorem, its proof certified in a few rounds of challenge. Made in the Certificate Verifier."),
        ('genome_library', 'Genome Library', "Genomes of the revived species, sequenced and indexed together. Made in the Gene Sequencer."),
        ('reconstructed_genome', 'Reconstructed Genome', "The library's gaps filled from the ancient fragments. Made in the Ancient DNA Lab."),
        ('cultured_cortex', 'Cultured Cortex', "A sheet of living cortex grown from organoids. Made in the Neural Culture Array."),
        ('trained_cortex', 'Trained Cortex', "The cultured cortex, its synapses trained. Made in the Synapse Trainer."),
        ('mapped_mind', 'Mapped Mind', "Every connection of the trained cortex traced. Made in the Connectome Array."),
        ('survey_probe', 'Survey Probe', "A probe built round the ice cores' record, to compare a world from above. Made in the Rocket Assembly."),
        ('orbital_survey', 'Orbital Survey', "The probe's measurements from orbit, carried up the tether. Made in the Space Elevator."),
        ('stratigraphic_timeline', 'Stratigraphic Timeline', "Excavation records and tree rings woven into one timeline. Made in the Chrono Loom.")):
    item(iid, name, lore)
L['tooltip.%s.synthesis_level' % NS] = 'Worth %s of the plain item. Nine make the next level.'
L['tooltip.%s.synthesis_level_max' % NS] = 'Worth %s of the plain item. The highest level.'

# the Codex
E = {'materials/field_syntheses': {'title': 'Field Syntheses', 'icon': ts('unified_field_lattice'), 'category': 'materials', 'research': 'femtoengineering', 'pages': [
    text("The endgame has four tiers, and the $(thing)Field Syntheses$() are the first: one per field, each made by a chain of that field's machines. Cosmic engineering (the Dyson Array, Stellar Engine, Matrioshka Brain, Universe Forge) needs two plain syntheses per recipe; beyond physics needs an $(thing)Alpha$() one; the final tier a $(thing)Beta$() one, and all seven between its machines."),
    text("Nine Field Syntheses of one level make one of the next in a crafting grid: Alpha, Beta, Gamma and on through the Greek letters, then Aleph, Bet and on through the Hebrew. The Perennial Catalyst Matrix is an endgame catalyst: it never wears."),
    spot(['unified_field_lattice', 'perennial_catalyst_matrix', 'proof_lattice', 'living_archive'], "Physics: Cyclotron, Synchrotron, Femtoscale Assembly Line. Chemistry: Catalyst Formulator, Framework Assembler, Chelation Column, Regeneration Kiln. Mathematics: Proof Assistant, Certificate Verifier, Automaton Lattice. Biology: Gene Sequencer, Ancient DNA Lab, Revival Vat.", 'The chains (1)'),
    spot(['engineered_mind', 'planetary_survey', 'deep_record'], "Mind and engineering: Neural Culture Array, Synapse Trainer, Connectome Array, Load Test Rig. Earth and space: Rocket Assembly (with ice cores), Space Elevator, Heliograph. Archaeology: Chrono Loom (with Excavation Records), Causality Loop Engine.", 'The chains (2)')]}}
codex(E, {'materials': [{'entry': 'materials/field_syntheses', 'x': 3, 'y': 1, 'parents': []}]})
finish()
print('items', len([k for k in L if k.startswith('item.')]) // 2, 'recipes checked', len(T.RECIPES))
