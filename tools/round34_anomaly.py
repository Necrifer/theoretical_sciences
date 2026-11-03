"""
Round 34 (round34_anomaly): Abnormality & Infection - the one branch that belongs to every science at once.

It studies the anomalous: effects and systems that should not exist under the laws we know, and that nonetheless
arise, copy themselves, escape the things that should destroy them, and change the world around them. Cancer is its
model organism; containment failures, fractured reality, the nanite infection and the wraiths are its field samples.

The branch is a journey in six stages, and each stage draws on all four sciences:

* ORIGIN - how anomalies arise. Physics: spontaneous symmetry breaking leaves topological defects and pockets of
  false vacuum. Chemistry: autocatalysis - molecules (and prions) that template their own making. Biology: oncogenesis,
  mutation by mutation, the hallmarks of cancer. Mathematics: pathological objects, chaos and undecidability.
* REPLICATION & ESCAPE - immune evasion, metastasis, telomerase immortality and angiogenesis; self-reference (quines,
  von Neumann replicators, memes) and the branching process that decides whether a lineage survives; clonal evolution
  and drug resistance.
* SYSTEMS - invasion fronts in a tumour microenvironment (reaction-diffusion), epidemics, R0, percolation and phase
  transitions.
* INTERACTION WITH NORMALITY - local law violation (a conservation law broken by a quantum anomaly), and the line
  between symbiosis and parasitism.
* EXTENT - a catalogue of what anomalies can do, and the power laws they obey.
* CONTAINMENT, STUDY, ISOLATION, RESOLUTION - quarantine fields that hold R below 1, targeted therapy (CRISPR-made
  apoptosis triggers, CAR-T cells, fractionated dose) and finally the anomaly explained: renormalised, its anomalous
  dimension measured, folded back into the laws.

Two multiblocks: the Anomaly Containment Ward (grow a specimen as fast as you dare while holding its reproduction
number below 1) and the Anomaly Resolution Engine (kill every clonogen without destroying the normal tissue around
it). Every new material comes from renewable inputs; the intended route starts from what containment failures leave
behind (Fractured Essence, Void Essence, Paradox Residue, Nanite Swarms, Law Crystals), turned into Contained
Anomaly Specimens in the Ward.

The research JSON, puzzles (tools/puzzles2/group_anomaly.json, written from PUZZLES here), recipes and Codex chapters
(category 'anomaly', shown on all four science tabs and on Discovery) are all generated from this file.
"""
import json
import os

import shapes
import round12 as _r12
from round12 import P as _P, D as _D, C as _C

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
CAT = 'anomaly'


# =================================================================================== CONTENT
def _I(i, n, art, c1, c2, lore, **kw):
    d = dict(id=i, name=n, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    _I('anomaly_specimen', 'Contained Anomaly Specimen', 'contained', '#40ffa0', '#e8fff0',
       'A sample of something that should not be, held in a quarantine field small enough to carry. It is still trying to get out.'),
    # ---- origin
    _I('topological_defect_core', 'Topological Defect Core', 'loop', '#ff6040', '#fff0c0',
       'A knot in a broken symmetry: the field could not agree which vacuum to fall into, so it kept both - and the seam between them cannot untie.'),
    _I('prion_lattice', 'Prion Lattice', 'lattice', '#d060a0', '#ffe0f0',
       'Misfolded protein stacked into amyloid. Touch a healthy protein to it and the healthy one misfolds too: shape is the infection.'),
    _I('anomalous_strain_culture', 'Anomalous Strain Culture', 'dots', '#80ff40', '#f0ffd0',
       'Stem cells exposed to a contained anomaly. Their checkpoints are failing one by one: no p53, no brakes, no reason to stop dividing.'),
    _I('undecidable_kernel', 'Undecidable Kernel', 'code', '#202040', '#ff40ff',
       'A program no other program can predict: whether it halts is true or false, and provably unprovable. It runs on anyway.'),
    # ---- replication & escape
    _I('immortal_cell_line', 'Immortal Cell Line', 'foam', '#ff8080', '#fff0f0',
       'Telomerase switched back on: the chromosome ends rebuild themselves, and the cells forget how to grow old. They have outlived their donor.'),
    _I('metastatic_spore', 'Metastatic Spore', 'seed', '#a040ff', '#ffd0ff',
       'A cluster of cells that has learned to leave: to slip its tissue, ride the blood and grow again somewhere it was never meant to be.'),
    _I('quine_replicator', 'Quine Replicator', 'progmatter', '#40e0ff', '#f0ffff',
       'A machine whose instructions contain a description of itself and an order to copy it. A von Neumann replicator, a virus, a meme.'),
    _I('resistant_clone', 'Resistant Clone Colony', 'catom', '#ffb040', '#fff0d0',
       'The cells that survived the last treatment - and so the ones that make the next tumour. Selection, not malice.'),
    # ---- systems
    _I('microenvironment_organoid', 'Tumour Microenvironment Organoid', 'pellet', '#ff6080', '#ffe0e8',
       'An anomaly is never alone: here it has grown its own blood vessels, recruited its own stroma and taught the local immune cells to look away.'),
    _I('contagion_network_map', 'Contagion Network Map', 'holo', '#40ff80', '#e0ffe8',
       'Every contact, every transmission, every node an infection passed through. Somewhere in it is the threshold where R crosses 1.'),
    # ---- interaction
    _I('law_violation_shard', 'Local Law Violation', 'shard', '#ff2050', '#ffe0ff',
       'A sliver of space where a conservation law quietly fails: charge appears from nothing, and the rest of physics has to make room.'),
    _I('symbiont_culture', 'Symbiont Culture', 'swarm', '#60ffc0', '#f0fff8',
       'The same anomaly, turned: it still copies itself, but now it pays its host back. Parasite and partner differ by one sign.'),
    # ---- extent
    _I('anomaly_catalogue', 'Anomaly Catalogue', 'codex', '#402060', '#ffd060',
       'Every kind of anomaly you have contained, what it can do, how far it can spread and how big the largest one will be. Most are small; a few are not.'),
    # ---- containment & resolution
    _I('quarantine_field_matrix', 'Quarantine Field Matrix', 'realanchor', '#40a0ff', '#e0f0ff',
       'A field that clears an anomaly faster than it can copy itself. It does not destroy anything - it only keeps R below one.'),
    _I('apoptosis_trigger', 'Apoptosis Trigger', 'probe', '#ffffff', '#ff4060',
       'A CRISPR-written instruction that only an anomalous cell will read: a reminder of the death it was always supposed to accept.'),
    _I('car_t_culture', 'CAR-T Cell Culture', 'pair', '#60a0ff', '#e0f0ff',
       'Killer T cells taught a new receptor: they hunt one marker, the anomaly\'s own, and they multiply as they find it.'),
    _I('resolved_anomaly', 'Resolved Anomaly', 'omega', '#fff0d0', '#ffffff',
       'An anomaly explained: its strangeness measured, renormalised, and written back into the laws. It is just physics now. It always was.'),
]
BLOCKS = []
FLUIDS = [
    dict(id='autocatalytic_broth', name='Autocatalytic Broth', c1='#ff8030', c2='#ffe0a0', gas=False, light=6, temp=320, density=1100,
         viscosity=1400, art='exotic'),
    dict(id='renormalised_essence', name='Renormalised Essence', c1='#f0e8c0', c2='#ffffff', gas=False, light=12, temp=300, density=800,
         viscosity=600, art='law'),
]

# =================================================================================== MACHINES
MACHINE_TEXT = {
    'anomaly_containment_ward': ('Anomaly Containment Ward',
                                 'A quarantine cell of chronoplastic walls, causal shielding and windows of containment glass, with dimensional '
                                 'stabilisers at every corner. It lets what it holds grow - just slower than its field can clear it.'),
    'anomaly_resolution_engine': ('Anomaly Resolution Engine',
                                  'A tower of reality-lattice casing around a column of lensed, error-corrected fields. It doses an anomaly fraction '
                                  'by fraction until not one of its cells is left, and writes what it learned back into the laws.'),
}
MACHINE_CAT = {'anomaly_containment_ward': CAT, 'anomaly_resolution_engine': CAT}
MACHINE_RESEARCH = {'anomaly_containment_ward': 'anomalous_phenomena', 'anomaly_resolution_engine': 'targeted_therapy'}
TIER = {'anomaly_containment_ward': 5, 'anomaly_resolution_engine': 5}
MC = {
    'anomaly_containment_ward': (['GSG', 'ZPZ', 'OCO'], {'G': 'containment_field_glass', 'S': 'stabiliser_matrix', 'Z': 'causal_shield_lattice',
                                                         'P': 'quantum_processor', 'O': 'chronoplastic_casing', 'C': 'containment_cell'}),
    'anomaly_resolution_engine': (['APA', 'LEL', 'ANA'], {'A': 'reality_lattice_casing', 'P': 'quantum_processor', 'L': 'gravitic_lens_element',
                                                          'E': 'error_correction_lattice', 'N': 'negentropy_cell'}),
}
EXTRA = {
    'anomaly_containment_ward': 'Turns what containment failures leave behind - $(item)Fractured Essence$(), $(item)Void Essence$(), $(item)Paradox Residue$(), '
                                '$(item)Nanite Swarms$(), $(item)Law Crystals$() - into $(item)Contained Anomaly Specimens$(), and later grows $(item)Local Law Violations$(), '
                                '$(item)Symbiont Cultures$(), the $(item)Anomaly Catalogue$() and $(item)Quarantine Field Matrices$(). Keep R = β/(γ + q) below 1.',
    'anomaly_resolution_engine': 'Cures: resistant clones become ordinary stem cells again and leave $(fluid)Renormalised Essence$(). Then resolves: a catalogued, '
                                 'quarantined anomaly - or a Local Law Violation, renormalised - becomes a $(item)Resolved Anomaly$().',
}


def containment_ward():
    """A quarantine cell: a chronoplastic box with a glass window band, a coil ring, causal shielding under the floor,
    dimensional stabilisers on the corner pillars and field emitters aimed into an empty core."""
    s = shapes.Shape(9, 8, 9)
    c = 4
    s.box(0, 0, 0, 8, 0, 8, 'O')                          # the floor
    s.box(1, 1, 1, 7, 1, 7, 'z')                          # causal shielding under the cell
    for y, ch in ((2, 'O'), (3, 'G'), (4, 'K'), (5, 'G'), (6, 'O')):
        for x in range(1, 8):
            for z in range(1, 8):
                if x in (1, 7) or z in (1, 7):
                    s.put(x, y, z, ch)                    # the walls: a window band between two coil-ringed courses
    s.box(1, 7, 1, 7, 7, 7, 'O')                          # the roof
    for x, z in ((0, 0), (8, 0), (0, 8), (8, 8)):
        s.column(x, z, 1, 6, 'O')
        s.put(x, 7, z, 's')                               # stabilisers on the corner pillars
    s.air_box(2, 2, 2, 6, 6, 6)
    for x, z in ((2, 2), (6, 2), (2, 6), (6, 6)):
        s.put(x, 6, z, 'F')                               # emitters at the top corners, aimed at the specimen
    s.put(c, 7, c, 'F')
    s.put(c, 2, c, 'F')
    s.center = (c, 4, c)
    s.ports('O', 'J', 4, y=0)
    s.controller(c, 2, 1)
    return s


def resolution_engine():
    """A tower: a reality-lattice drum, two coil rings, error-correction cores and graviton lenses focusing down an
    open central column onto the anomaly."""
    s = shapes.Shape(9, 11, 9)
    c = 4
    s.disc(c, c, 0, 4, 'A')
    s.disc(c, c, 1, 3.6, 'A', r_in=2.4)
    s.disc(c, c, 2, 3.6, 'K', r_in=2.4)
    s.disc(c, c, 3, 3.6, 'G', r_in=2.4)
    s.disc(c, c, 4, 3.6, 'A', r_in=2.4)
    s.disc(c, c, 5, 3.6, 'K', r_in=2.4)
    s.disc(c, c, 6, 3.6, 'A', r_in=2.4)
    s.disc(c, c, 7, 2.6, 'A', r_in=1.4)
    s.disc(c, c, 8, 2.6, 'A', r_in=1.4)
    s.disc(c, c, 9, 1.6, 'A')
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        s.put(c + dx, 7, c + dz, 'n')                     # error correction keeps the fields honest
        s.put(c + dx, 8, c + dz, 'l')                     # lenses focus the dose
    s.put(c, 10, c, 'F')
    for y in range(1, 9):
        s.disc(c, c, y, 1.3 if y >= 7 else 2.2, '.')
    s.put(c, 1, c, 'F')
    s.center = (c, 4, c)
    s.ports('A', 'D', 4, y=0)
    s.controller(c, 0, 0)
    return s


shapes.BUILDERS['anomaly_containment_ward'] = containment_ward
shapes.BUILDERS['anomaly_resolution_engine'] = resolution_engine

_r12.model('anomaly_containment_ward', 'Anomaly Containment Ward',
           'A specimen copies itself at a rate β; the ward clears it at its natural rate γ = 0.4 per day plus the quarantine field\'s own rate q. Each anomalous unit '
           'then makes R = β/(γ + q) more before it is cleared. Below 1 the specimen is contained and can be studied; the faster it grows the more there is to study - '
           'but at R above 1 it spreads through the containment. Clamp the field too hard and the specimen goes dormant and there is nothing to learn.',
           'The specimen\'s growth rate wanders by ±10%·d.',
           [_P('beta', 'Growth rate β', 'day⁻¹', 0, 4, 0.01, 1.0, 'How fast the specimen is fed and allowed to copy itself.'),
            _P('q', 'Quarantine rate q', 'day⁻¹', 0, 4, 0.01, 1.0, 'How fast the quarantine field clears anomalous units.')],
           [_D('be', 'Effective growth rate', 'day⁻¹', 'beta*(1 + 0.1*d)', 'β_eff = β·(1 + 0.1·d)', 'The growth rate the drift leaves you with.'),
            _D('Re', 'Reproduction number R', '', 'be/(0.4 + q)', 'R = β/(γ + q)', 'How many new anomalous units each one makes before it is cleared. Contained below 1.'),
            _D('act', 'Observable activity', '', 'be*Re', 'A = β·R', 'How much the specimen does while you watch it: what the study is made of.')],
           [_C('breach', 'damage', '1 - Re', 'R above 1: the anomaly is spreading through the containment', 'R < 1'),
            _C('dormant', 'stop', 'Re - 0.3', 'The quarantine is so strong the specimen has gone dormant: nothing to study', 'R ≥ 0.3'),
            _C('overheat', 'damage', '1300 - T', 'The quarantine field is overheating the ward', 'T < 1300 K')],
           rate='clamp(act, 0, 3)', rate_formula='rate = β·R', rate_desc='A faster-growing specimen held just below R = 1 gives the most to study.',
           heat='0.4 + 0.25*q', idle_heat='0.4', kappa=50, heat_capacity=60)
_r12.WINDOWS['anomaly_containment_ward'] = {}

_r12.model('anomaly_resolution_engine', 'Anomaly Resolution Engine',
           'The anomaly is dosed in n fractions of D. Each fraction kills a fraction exp(−αD − βD²) of its 10⁹ clonogenic cells (α = 0.3 Gy⁻¹, β = 0.03 Gy⁻²); '
           'it is resolved when none survives, with probability TCP = exp(−N₀·e^{−n(αD + βD²)}). The normal tissue around it repairs between fractions, but its damage '
           'grows as BED = nD(1 + D/3): small fractions spare it, large ones do not.',
           'The anomaly\'s sensitivity α wanders by ±5%·d.',
           [_P('D', 'Dose per fraction D', 'Gy', 0, 10, 0.1, 2, 'How hard each fraction hits. Bigger fractions kill more per fraction, and harm normal tissue faster.'),
            _P('n', 'Fractions n', '', 1, 60, 1, 33, 'How many fractions the dose is split into.')],
           [_D('al', 'Sensitivity α', 'Gy⁻¹', '0.3*(1 + 0.05*d)', 'α = 0.3·(1 + 0.05·d)', 'How sensitive this anomaly is to the dose.'),
            _D('lk', 'Log cell kill', '', 'n*(al*D + 0.03*D^2)', 'ln(1/S) = n(αD + βD²)', 'The natural logarithm of the fraction of clonogens killed.'),
            _D('tcp', 'Resolution probability TCP', '', 'exp(-1e9*exp(-lk))', 'TCP = exp(−N₀·e^{−n(αD + βD²)})', 'The chance that not one anomalous cell survives.'),
            _D('bed', 'Normal-tissue BED', 'Gy', 'n*D*(1 + D/3)', 'BED₃ = nD(1 + D/3)', 'The biologically effective dose the normal tissue around the anomaly receives.')],
           [_C('survive', 'stop', 'tcp - 0.5', 'The anomaly survives: too many of its clonogens are left', 'TCP ≥ 50%'),
            _C('toxic', 'damage', '120 - bed', 'The dose is destroying the normal tissue around the anomaly', 'BED₃ < 120 Gy'),
            _C('overheat', 'damage', '1300 - T', 'The resolution fields are overheating', 'T < 1300 K')],
           rate='clamp(2*tcp, 0, 2)', rate_formula='rate = 2·TCP', rate_desc='The surer the resolution, the faster the work goes.',
           heat='0.3 + 0.008*bed', idle_heat='0.3', kappa=50, heat_capacity=60)
_r12.WINDOWS['anomaly_resolution_engine'] = {}

# =================================================================================== RESEARCH
# id, title, description, icon, x, y, insight, requires, cost
RESEARCH = [
    ('anomalous_phenomena', 'Abnormality & Infection',
     'Every science has its anomalies: effects that the laws we know cannot explain, systems that copy themselves and refuse to die. Containment failures leave them '
     'lying around. Study them properly - how they arise, spread, interact with the normal world, and how they can finally be explained. Unlocks the Anomaly '
     'Containment Ward and Contained Anomaly Specimens.',
     'anomaly_specimen', 14, 8, 260, ['field_engineering', 'simulation_hypothesis'], [('fractured_essence', 16)]),
    # ---------------------------------------------------------------- ORIGIN
    ('topological_defects', 'Origins: Broken Symmetry',
     'When a field cools and must choose between equal vacua, distant regions choose differently - and where they meet, the old symmetry is trapped in a wall, a '
     'string or a knot that cannot be smoothed away. Pockets of false vacuum survive the same way. Unlocks the Topological Defect Core.',
     'topological_defect_core', 15, 5, 280, ['anomalous_phenomena', 'vacuum_decay'], [('anomaly_specimen', 4), ('metastable_vacuum_lattice', 2)]),
    ('autocatalysis', 'Origins: Molecules That Copy Themselves',
     'A reaction whose product speeds up its own making grows exponentially and can oscillate, flare and spread as a wave. A prion does it with shape alone: a '
     'misfolded protein that refolds the healthy ones it touches. Unlocks Autocatalytic Broth and the Prion Lattice.',
     'prion_lattice', 15, 7, 280, ['anomalous_phenomena', 'island_of_stability', 'supramolecular_chemistry'], [('anomaly_specimen', 4), ('zeolite_catalyst', 4)]),
    ('oncogenesis', 'Origins: Oncogenesis',
     'A cancer is not one mistake but several: an oncogene stuck on, a tumour suppressor like p53 lost, a checkpoint skipped - each a mutation that gives one cell '
     'an advantage over its neighbours. The hallmarks of cancer are what that cell has learned. Unlocks the Anomalous Strain Culture.',
     'anomalous_strain_culture', 15, 9, 280, ['anomalous_phenomena', 'directed_evolution'], [('anomaly_specimen', 4), ('stem_cell_culture', 4)]),
    ('pathological_objects', 'Origins: Pathological Objects',
     'Mathematics has its monsters too: curves continuous everywhere and smooth nowhere, sets with no size, systems so sensitive that they are deterministic and '
     'unpredictable at once, and questions no algorithm can decide. They were never errors - only the parts of the theory that had not been explained yet. '
     'Unlocks the Undecidable Kernel.',
     'undecidable_kernel', 15, 11, 300, ['anomalous_phenomena', 'incompleteness'], [('anomaly_specimen', 4), ('godel_sentence', 2)]),
    # ---------------------------------------------------------------- REPLICATION & ESCAPE
    ('self_replication', 'Self-Reference & Replicators',
     'A program that prints itself, a machine that builds itself from a description of itself, a virus, a rumour: every replicator holds a copy of its own '
     'instructions. Whether its lineage survives is a branching process - it dies out unless each copy makes more than one more. Unlocks the Quine Replicator.',
     'quine_replicator', 16, 8, 320, ['autocatalysis', 'pathological_objects'], [('prion_lattice', 2), ('undecidable_kernel', 2)]),
    ('immune_evasion', 'Escape: Evasion, Immortality & Metastasis',
     'To survive, an anomaly must escape everything built to remove it: hide its markers from the immune system, switch telomerase back on and forget how to age, '
     'grow its own blood vessels, and finally leave - slip into the blood and seed itself somewhere new. Unlocks the Immortal Cell Line and the Metastatic Spore.',
     'metastatic_spore', 16, 10, 320, ['oncogenesis'], [('anomalous_strain_culture', 4)]),
    ('clonal_evolution', 'Escape: Clonal Evolution & Resistance',
     'A tumour is a population, and populations evolve. Every treatment selects the few cells that happen to resist it; they inherit the tumour. Drug resistance '
     'is not learned but chosen - by the treatment itself. Unlocks the Resistant Clone Colony.',
     'resistant_clone', 17, 10, 340, ['immune_evasion', 'self_replication'], [('immortal_cell_line', 4), ('metastatic_spore', 2)]),
    # ---------------------------------------------------------------- SYSTEMS
    ('invasion_fronts', 'Systems: The Microenvironment & Invasion Fronts',
     'No anomaly grows alone. It recruits blood vessels and stroma, signals to the cells around it, and invades as a travelling front whose speed is set by how '
     'fast it grows and how far it diffuses - the same mathematics as a cellular automaton spreading on a grid. Unlocks the Tumour Microenvironment Organoid.',
     'microenvironment_organoid', 17, 8, 340, ['immune_evasion', 'self_replication'], [('metastatic_spore', 2), ('quine_replicator', 2)]),
    ('epidemic_dynamics', 'Systems: Epidemics, Percolation & Phase Transitions',
     'Between hosts, an infection is a reproduction number. Above R = 1 it spreads through a population; below it, it dies out - a phase transition, the same '
     'as water freezing or a fluid percolating through porous rock. Unlocks the Contagion Network Map.',
     'contagion_network_map', 18, 9, 360, ['invasion_fronts', 'clonal_evolution'], [('microenvironment_organoid', 2), ('resistant_clone', 2)]),
    # ---------------------------------------------------------------- INTERACTION WITH NORMALITY
    ('anomalous_leakage', 'Where Anomaly Meets Normality',
     'An anomaly in physics is a law that holds classically but fails in the quantum world: a conserved charge that quietly is not. Where a defect touches '
     'ordinary space, such failures leak out - local violations the rest of physics must absorb. Unlocks the Local Law Violation.',
     'law_violation_shard', 18, 6, 360, ['topological_defects', 'invasion_fronts'], [('topological_defect_core', 4), ('law_crystal', 2)]),
    ('symbiosis_parasitism', 'Symbiosis & Parasitism',
     'Two populations that share a world either compete, prey, or help. A parasite and a symbiont can be the same organism with one interaction coefficient '
     'flipped - mitochondria were invaders once. The boundary between anomaly and normality is a sign. Unlocks the Symbiont Culture.',
     'symbiont_culture', 18, 11, 360, ['clonal_evolution', 'epidemic_dynamics'], [('resistant_clone', 2), ('xenobot_swarm', 4)]),
    # ---------------------------------------------------------------- EXTENT
    ('anomaly_scaling', 'The Extent of Anomalies',
     'What can an anomaly do, and how big can it get? Catalogue them, and their sizes - tumours, outbreaks, avalanches, ruptures - follow power laws: most are '
     'tiny, a few are vast, and the mean is set by the exponent. Unlocks the Anomaly Catalogue.',
     'anomaly_catalogue', 19, 8, 400, ['anomalous_leakage', 'epidemic_dynamics', 'symbiosis_parasitism'],
     [('law_violation_shard', 2), ('symbiont_culture', 2), ('contagion_network_map', 2)]),
    # ---------------------------------------------------------------- CONTAINMENT, STUDY, ISOLATION, RESOLUTION
    ('containment_protocols', 'Containment, Study & Isolation',
     'To study an anomaly you must keep it alive; to keep the world safe you must keep it from spreading. Quarantine is the art of clearing it just faster than '
     'it copies itself - R below 1, and not much below. Unlocks the Quarantine Field Matrix.',
     'quarantine_field_matrix', 20, 8, 420, ['anomaly_scaling'], [('anomaly_catalogue', 1)]),
    ('targeted_therapy', 'Targeted Therapy',
     'Kill the anomaly, spare the normal: CRISPR-written triggers that make only anomalous cells accept apoptosis, CAR-T cells taught to hunt one marker, and '
     'dose split into fractions the normal tissue can repair between. Unlocks the Anomaly Resolution Engine, the Apoptosis Trigger and CAR-T Cell Culture.',
     'apoptosis_trigger', 20, 10, 450, ['containment_protocols', 'clonal_evolution'], [('resistant_clone', 4), ('recombinant_dna', 8)]),
    ('anomaly_resolution', 'Resolution: The Anomaly Explained',
     'The last step is not destruction but understanding. An anomaly measured completely - how it scales, where it breaks the law and by exactly how much - can '
     'be renormalised: absorbed into a deeper law that explains it. It was never abnormal, only unexplained. Unlocks the Resolved Anomaly.',
     'resolved_anomaly', 21, 9, 600, ['targeted_therapy', 'anomaly_scaling', 'theory_of_everything'],
     [('quarantine_field_matrix', 2), ('apoptosis_trigger', 2), ('car_t_culture', 2)]),
]
IDS = [r[0] for r in RESEARCH]

# =================================================================================== PUZZLES (tools/puzzles2/group_anomaly.json)
PUZZLES = {
    'anomalous_phenomena': dict(
        goal='Before calling anything abnormal you must know what normal looks like. A quantity measured many times scatters around its mean μ with spread σ, its '
             'values distributed as the bell-shaped density exp(−(x − μ)²/2σ²)/(σ√(2π)). Write the probability that a single measurement lies more than k standard '
             'deviations from the mean, on either side.',
        name='two-sided tail of the Gaussian distribution (the significance of a deviation)', lhs='P',
        form='2 * int ( μ + k * σ , ∞ , 1 / ( σ * sqrt ( 2 * π ) ) * exp ( - ( x - μ ) ^ 2 / ( 2 * σ ^ 2 ) ) , x )',
        expr='sqrt ( 2 / π ) * exp ( - k ^ 2 / 2 ) / k * 10000000',
        ask='For large k the integral is close to √(2/π)·e^{−k²/2}/k. Physics calls a result a discovery at k = 5: find the chance of a deviation that large by '
            'accident, in units of 10⁻⁷.',
        vars={'P': 'probability of a deviation of k σ or more', 'μ': 'mean', 'k': 'number of standard deviations', 'σ': 'standard deviation', 'x': 'measured value'},
        given={'k': 5}, unit='10⁻⁷',
        method='The two tails are equal by symmetry: integrate one from μ + kσ to infinity and double it. Substitute u = (x − μ)/σ; for large u the tail '
               'integral of e^{−u²/2} is e^{−k²/2}/k.',
        distractors=['ν', 'ln', '3']),
    'topological_defects': dict(
        goal='A real scalar field φ in one dimension has a potential with two equal minima, at φ = ±v, and a coupling λ: the symmetry φ → −φ is broken by whichever '
             'vacuum it falls into. Far to the left it sits at −v, far to the right at +v, and in between it must climb over the hill. Write the total energy of '
             'that static configuration: gradient energy plus potential, along the whole line.',
        name='energy of a kink (domain wall) of a spontaneously broken Z₂ symmetry', lhs='E',
        form='int ( - ∞ , ∞ , 1 / 2 * d ( φ , x ) ^ 2 + λ / 4 * ( φ ^ 2 - v ^ 2 ) ^ 2 , x )',
        expr='2 * sqrt ( 2 ) / 3 * sqrt ( λ ) * v ^ 3',
        ask='The minimum-energy solution is φ = v·tanh(v x √(λ/2)), and its energy is E = (2√2/3)·√λ·v³. Find it for the coupling and vacuum value below (natural units).',
        vars={'E': 'energy of the defect', 'φ': 'scalar field', 'x': 'position', 'λ': 'quartic coupling', 'v': 'vacuum value of the field'},
        given={'λ': 0.5, 'v': 2}, unit='natural units',
        method='Energy = ∫[½φ\'² + V(φ)]dx with V = (λ/4)(φ² − v²)². The minimising solution has ½φ\'² = V, so dφ/dx = √(2V): change the integral to one over φ from −v to v.',
        distractors=['t', 'exp', '3']),
    'autocatalysis': dict(
        goal='Two reagents A and B are fed in at constant concentration. A makes an intermediate X; B turns X into Y; X decays; and - the autocatalytic step - two X '
             'and one Y make three X, so X catalyses its own production. In dimensionless units with every rate constant 1, write how fast X changes.',
        name='Brusselator (autocatalytic chemical oscillator), the X equation', lhs='d ( X , t )',
        form='A - ( B + 1 ) * X + X ^ 2 * Y',
        expr='1 + A ^ 2',
        ask='The steady state is X = A, Y = B/A. It loses stability - and the mixture starts to oscillate on its own, a chemical clock - once B exceeds 1 + A². '
            'Find that critical B for the A below.',
        vars={'X': 'autocatalytic intermediate', 'Y': 'second intermediate', 'A': 'feed concentration of the first reagent',
              'B': 'feed concentration of the second reagent', 't': 'time'},
        given={'A': 1.5}, unit='',
        method='Add up the four steps: A → X adds A, B + X → Y removes BX, X → product removes X, 2X + Y → 3X adds X²Y.',
        distractors=['k', '2', 'exp']),
    'oncogenesis': dict(
        goal='A cell becomes malignant only after n separate rare mutations, in a fixed order, each happening at a small constant rate k_j per year. The chance that a '
             'single cell has collected all n by age t is the product of the rates times the volume of the ordered region of mutation times inside [0, t]. Write '
             'the yearly incidence of the disease at age t: the derivative of that chance.',
        name='Armitage–Doll multistage model of carcinogenesis', lhs='I',
        form='prod ( j , 1 , n , k _ j ) * t ^ ( n - 1 ) / ( n - 1 ) !',
        expr='100000 * k ^ 6 * t ^ 5 / 120',
        ask='Most cancers fit about six stages. With every k_j = k per year and n = 6, find the incidence at age t, in cases per 100,000 people per year (it rises '
            'as the fifth power of age - as measured).',
        vars={'I': 'incidence (new cases per year)', 'j': 'stage index', 'n': 'number of mutations needed', 'k': 'mutation rate of a stage (per year)', 't': 'age'},
        given={'k': 0.01, 't': 70}, unit='per 100,000 per year',
        method='The n mutation times t₁ < t₂ < ... < t_n inside [0, t] fill a simplex of volume tⁿ/n!. Multiply by the rates and differentiate in t.',
        distractors=['exp', 'ln', 'e']),
    'pathological_objects': dict(
        goal='Build a curve as a sum of ever faster, ever smaller waves: the n-th term is a cosine whose frequency grows like bⁿ and whose amplitude shrinks like aⁿ, '
             'with 0 < a < 1 and ab large. The sum converges everywhere and is continuous, yet it has a corner at every point - a monster that broke the '
             'nineteenth-century idea of a function. Write it.',
        name='Weierstrass function (continuous everywhere, differentiable nowhere)', lhs='W',
        form='sum ( n , 0 , ∞ , a ^ n * cos ( b ^ n * π * x ) )',
        expr='2 + ln ( a ) / ln ( b )',
        ask='Its graph is a fractal: zoom in and it never straightens. Its box-counting dimension is D = 2 + ln a / ln b. Find D for the a and b below.',
        vars={'W': 'the curve', 'x': 'position', 'n': 'term index', 'a': 'amplitude ratio (0 < a < 1)', 'b': 'frequency ratio'},
        given={'a': 0.5, 'b': 7}, unit='',
        method='Each term is bounded by aⁿ, so the series converges uniformly (a continuous limit). The derivative of the n-th term grows like (ab)ⁿ, so the '
               'differentiated series diverges everywhere.',
        distractors=['sin', 'exp', 'k']),
    'self_replication': dict(
        goal='A replicator - a self-printing program, a virus particle, a rumour - makes k copies of itself with probability p_k before it is destroyed, and every copy '
             'does the same independently. The lineage dies out only if every one of the first copy\'s offspring lineages dies out. Write the equation for the '
             'probability q that the lineage eventually dies out.',
        name='extinction probability of a Galton–Watson branching process (generating-function fixed point)', lhs='q',
        form='sum ( k , 0 , ∞ , p _ k * q ^ k )',
        expr='1 - ( 1 - p ) / p',
        ask='A replicator that makes either two copies (probability p) or none has q = (1 − p)/p when p > ½. Find the chance that its lineage escapes destruction '
            'forever, 1 − q.',
        vars={'q': 'probability the lineage dies out', 'k': 'number of copies', 'p': 'probability of making copies (p_k: of exactly k)'},
        given={'p': 0.6}, unit='',
        method='Condition on the first generation: with k copies, all k independent lineages must die out, with probability q^k. Sum over k.',
        distractors=['n', 'exp', '1']),
    'immune_evasion': dict(
        goal='A tumour of N cells grows fast while it is small and slows as it outgrows its blood supply: its specific growth rate decays exponentially in time at a '
             'rate α, so that it levels off at a carrying capacity K. Starting from N₀ cells, write the number of cells at time t.',
        name='Gompertz law of tumour growth', lhs='N',
        form='K * exp ( ln ( N _ 0 / K ) * exp ( - α * t ) )',
        expr='K * exp ( ln ( n / K ) * exp ( - α * t ) )',
        ask='With K and the starting size n in units of 10⁹ cells, α per month and t in months, find the size of the tumour.',
        vars={'N': 'number of tumour cells', 'K': 'carrying capacity', 'α': 'decay rate of the growth rate', 't': 'time',
              'n': 'starting number of cells (N₀, 10⁹ cells)'},
        given={'K': 1000, 'n': 1, 'α': 0.1, 't': 20}, unit='10⁹ cells',
        method='Write d ln N/dt = r e^{−αt} and integrate; or note that d ln N/dt = α ln(K/N), so ln(N/K) itself decays exponentially.',
        distractors=['r', 'β', '2']),
    'clonal_evolution': dict(
        goal='A tumour holds n clones; clone i makes up a fraction x_i of it and has fitness f_i - its net growth rate under the current treatment. A clone grows in '
             'share only if it is fitter than the tumour\'s average. Write how the fraction of clone i changes.',
        name='replicator equation (selection among competing clones)', lhs='d ( x _ i , t )',
        form='x _ i * ( f _ i - sum ( j , 1 , n , x _ j * f _ j ) )',
        expr='x * exp ( s * t ) / ( 1 - x + x * exp ( s * t ) )',
        ask='A resistant clone starts as a fraction x and grows faster than the rest by s per day under treatment; its share is then x e^{st}/(1 − x + x e^{st}). '
            'Find its share after t days.',
        vars={'x': 'fraction of the tumour (x in step 2: the resistant clone\'s starting share)', 'i': 'clone index', 'j': 'summation index', 'f': 'fitness',
              'n': 'number of clones', 't': 'time', 's': 'fitness advantage of the resistant clone (per day)'},
        given={'x': 0.001, 's': 0.5, 't': 20}, unit='',
        method='Each clone grows as dN_i/dt = f_i N_i. Differentiate x_i = N_i/ΣN_j with the quotient rule; the denominator brings in the mean fitness.',
        distractors=['μ', 'ln', '2']),
    'invasion_fronts': dict(
        goal='Anomalous cells of density u diffuse through tissue with diffusivity D and divide at a rate r while there is room, up to a carrying density K. Write '
             'how the density changes in space and time: diffusion plus self-limiting growth.',
        name='Fisher–KPP reaction–diffusion equation (a travelling invasion front)', lhs='pd ( u , t )',
        form='D * lap ( u ) + r * u * ( 1 - u / K )',
        expr='2 * sqrt ( D * r )',
        ask='Started from a small seed, the anomaly invades as a front that settles to the speed c = 2√(Dr). With D in mm²/day and r per day, find the speed of '
            'the front in mm/day.',
        vars={'u': 'density of anomalous cells', 't': 'time', 'D': 'diffusivity', 'r': 'growth rate', 'K': 'carrying density'},
        given={'D': 0.05, 'r': 0.2}, unit='mm/day',
        method='Linearise at the leading edge (u small) and look for a front u ∝ e^{−λ(x − ct)}: c = Dλ + r/λ, smallest at λ = √(r/D).',
        distractors=['v', 'exp', '2']),
    'epidemic_dynamics': dict(
        goal='A population of N is split into the susceptible S, the infected I and the recovered. Infected hosts meet others at random and transmit at a rate β per '
             'contact with a susceptible; they recover at a rate γ and die of other causes at a rate μ. Write how the number of infected changes.',
        name='Kermack–McKendrick SIR model with vital dynamics, the infected equation', lhs='d ( I , t )',
        form='β * S * I / N - ( γ + μ ) * I',
        expr='1 - ( γ + μ ) / β',
        ask='At the start S ≈ N, and the infection grows only if R₀ = β/(γ + μ) is above 1 - a phase transition. It stops spreading once a fraction 1 − 1/R₀ is '
            'immune. Find that herd-immunity threshold.',
        vars={'I': 'number infected', 't': 'time', 'β': 'transmission rate', 'S': 'number susceptible', 'N': 'population',
              'γ': 'recovery rate', 'μ': 'background death rate'},
        given={'β': 0.6, 'γ': 0.2, 'μ': 0.01}, unit='',
        method='New infections come from S·I contacts, a fraction I/N of each susceptible\'s meetings; infected leave by recovery or death.',
        distractors=['R', 'exp', 'k']),
    'anomalous_leakage': dict(
        goal='Massless fermions have a chiral charge that the classical equations conserve exactly. Put them in parallel electric and magnetic fields and quantum '
             'effects break that conservation: chiral charge appears out of the vacuum at a rate set by the fine-structure constant through e², and by the fields\' '
             'dot product. Write the source of chiral charge per unit volume, with ħ and c written out.',
        name='Adler–Bell–Jackiw chiral anomaly', lhs='pd ( ρ , t ) + div ( vec ( j ) )',
        form='e ^ 2 / ( 2 * π ^ 2 * ħ ^ 2 * c ) * vec ( E ) · vec ( B )',
        expr='2 * α / π * E * B',
        ask='In natural units (ħ = c = 1, e² = 4πα) the source is (2α/π)·E·B for parallel fields. Find it for the fields below.',
        vars={'ρ': 'chiral charge density', 't': 'time', 'j': 'chiral current', 'e': 'elementary charge', 'E': 'electric field', 'B': 'magnetic field',
              'α': 'fine-structure constant', 'ħ': 'reduced Planck constant', 'c': 'speed of light'},
        given={'α': 0.0073, 'E': 2, 'B': 3}, unit='natural units',
        method='In a magnetic field the fermions fill Landau levels; the lowest one is chiral and one-dimensional. An electric field along B pumps states through it '
               'at a rate eE/2π per unit length, and the level holds eB/2π states per unit area.',
        distractors=['G', 'kB', '4']),
    'symbiosis_parasitism': dict(
        goal='Two populations N₁ and N₂ share a world. Each grows logistically at rate r_i up to its own capacity K_i, and each member of the second population counts '
             'as α members of the first against that capacity - α > 0 is competition, α < 0 is help. Write how the first population changes.',
        name='competitive Lotka–Volterra equations (and mutualism)', lhs='d ( N _ 1 , t )',
        form='r _ 1 * N _ 1 * ( 1 - ( N _ 1 + α * N _ 2 ) / K _ 1 )',
        expr='( K - α * L ) / ( 1 - α * β )',
        ask='The second population has the mirror equation with coefficient β. They coexist at N₁* = (K₁ − αK₂)/(1 − αβ). With K₁ = K and K₂ = L, find N₁*.',
        vars={'N': 'population', 'r': 'growth rate', 'α': 'effect of population 2 on 1', 'K': 'carrying capacity (of 1)', 'L': 'carrying capacity of 2',
              'β': 'effect of population 1 on 2', 't': 'time'},
        given={'K': 100, 'L': 80, 'α': 0.5, 'β': 0.4}, unit='',
        method='Logistic growth r N(1 − N/K) with the other population added to the crowding term. For the equilibrium set both brackets to zero and solve the pair.',
        distractors=['γ', 'exp', '2']),
    'anomaly_scaling': dict(
        goal='Catalogue anomalies by size - tumours, outbreaks, avalanches, ruptures - and above a smallest size s₀ their sizes follow a power law: the probability '
             'density falls off as s^(−τ), normalised by (τ − 1)/s₀. Write the mean size of an anomaly.',
        name='mean of a power-law (Pareto) distribution', lhs='avg ( s )',
        form='int ( s _ 0 , ∞ , s * ( τ - 1 ) / s _ 0 * ( s / s _ 0 ) ^ ( - τ ) , s )',
        expr='s * ( τ - 1 ) / ( τ - 2 )',
        ask='For τ > 2 the integral is s₀(τ − 1)/(τ − 2); at τ ≤ 2 it diverges - the largest anomaly dominates everything. With s₀ = s, find the mean size.',
        vars={'s': 'anomaly size (s₀ in step 2: the smallest size)', 'τ': 'power-law exponent'},
        given={'s': 10, 'τ': 2.5}, unit='',
        method='Substitute u = s/s₀: the integrand becomes (τ − 1)u^{1−τ}, and ∫₁^∞ u^{1−τ}du = 1/(τ − 2).',
        distractors=['exp', 'ln', 'σ']),
    'containment_protocols': dict(
        goal='A contained anomalous unit makes new ones at a constant rate β while it is active, and stops being active either by clearing itself (rate γ) or by being '
             'caught by the quarantine field (rate q), so it is still active a time τ after it appeared with probability exp(−(γ + q)τ). Write the expected number of '
             'new units it makes in its whole active life.',
        name='effective reproduction number under quarantine (renewal integral)', lhs='R _ e',
        form='int ( 0 , ∞ , β * exp ( - ( γ + q ) * τ ) , τ )',
        expr='β - γ',
        ask='The integral is β/(γ + q). Find the smallest quarantine rate q that holds R_e at 1 (containment) for the β and γ below, in units per day.',
        vars={'R': 'reproduction number', 'e': 'effective', 'β': 'rate of making new units', 'γ': 'natural clearance rate', 'q': 'quarantine rate', 'τ': 'age of a unit'},
        given={'β': 0.5, 'γ': 0.1}, unit='per day',
        method='Integrate the rate of making new units times the probability of still being active at each age.',
        distractors=['N', 'ln', '2']),
    'targeted_therapy': dict(
        goal='An anomaly has N₀ cells able to regrow it. It is dosed in n equal fractions of D; each fraction lets each cell survive with probability exp(−αD − βD²). '
             'It is resolved only if not one cell survives, and the survivors are rare, independent events. Write the probability of resolution.',
        name='tumour control probability (linear-quadratic cell kill, Poisson statistics)', lhs='P',
        form='exp ( - N _ 0 * exp ( - n * ( α * D + β * D ^ 2 ) ) )',
        expr='exp ( - N * exp ( - n * ( α * D + β * D ^ 2 ) ) )',
        ask='Find the probability of resolution for 10⁹ clonogens, 30 fractions of 2 Gy, α = 0.3 Gy⁻¹ and β = 0.03 Gy⁻².',
        vars={'P': 'probability of resolution', 'N': 'number of clonogenic cells', 'n': 'number of fractions', 'α': 'linear sensitivity (per Gy)',
              'β': 'quadratic sensitivity (per Gy²)', 'D': 'dose per fraction (Gy)'},
        given={'N': 1000000000, 'n': 30, 'α': 0.3, 'β': 0.03, 'D': 2}, unit='',
        method='The expected number of survivors is N₀ times the survival per fraction to the n-th power. For rare independent survivors the chance of none is '
               'e^{−mean}.',
        distractors=['γ', 't', 'ln']),
    'anomaly_resolution': dict(
        goal='A quantity G measured at energy scale μ seems to break the scaling a dimensionless theory should have. The breaking is absorbed by letting the coupling '
             'g run with μ at the rate β(g) = μ dg/dμ, and by giving each of the n fields in G an extra, anomalous scaling γ. Write the condition that makes the '
             'physics independent of the arbitrary scale μ.',
        name='Callan–Symanzik equation (the anomalous dimension)', lhs='0',
        form='μ * pd ( G , μ ) + β * pd ( G , g ) + n * γ * G',
        expr='r ^ ( - n * γ )',
        ask='At a fixed point β = 0, and G then scales as μ^{−nγ}: the anomaly is just a power law. With n fields of anomalous dimension γ, find G(rμ)/G(μ) for a '
            'change of scale by r.',
        vars={'μ': 'energy scale', 'G': 'Green function (the measured quantity)', 'β': 'beta function', 'g': 'coupling', 'n': 'number of fields', 'γ': 'anomalous dimension',
              'r': 'ratio of scales'},
        given={'r': 10, 'n': 2, 'γ': 0.25}, unit='',
        method='The bare G does not depend on μ; the renormalised one differs from it by a factor Z^{n/2}. Differentiate in μ with the chain rule through g.',
        distractors=['λ', 'ln', 'ħ']),
}

# =================================================================================== RECIPES
# (name, machine recipe type, item inputs, fluid inputs, item outputs, fluid outputs, energy, ticks, research, catalysts)
# Base amounts; costed at the machine's tier like round32_branches does, then left unscaled. Every input is renewable.
WARD, RES = 'anomaly_containment', 'anomaly_resolution'
RECIPES = [
    # ---------------------------------------------------------------- the intended route: containment-failure drops become study samples
    ('specimen_from_fracture', WARD, [('fractured_essence', 4), ('stabiliser_matrix', 1)], [('superfluid_helium', 50)],
     [('anomaly_specimen', 16)], [], 4194304, 800, 'anomalous_phenomena', []),
    ('specimen_from_void', WARD, [('void_essence', 2), ('stabiliser_matrix', 1)], [('dark_matter', 50)],
     [('anomaly_specimen', 16)], [], 4194304, 800, 'anomalous_phenomena', []),
    ('specimen_from_paradox', WARD, [('paradox_residue', 4), ('causal_shield_lattice', 1)], [('chronon_gas', 50)],
     [('anomaly_specimen', 16)], [], 4194304, 800, 'anomalous_phenomena', []),
    ('specimen_from_nanites', WARD, [('nanite_swarm', 2), ('causal_shield_lattice', 1)], [('nanite_slurry', 50)],
     [('anomaly_specimen', 16)], [], 4194304, 800, 'anomalous_phenomena', []),
    ('specimen_from_law', WARD, [('law_crystal', 1), ('stabiliser_matrix', 1)], [('primordial_law', 50)],
     [('anomaly_specimen', 16)], [], 4194304, 800, 'anomalous_phenomena', []),
    # ---------------------------------------------------------------- ORIGIN
    ('topological_defect', 'mass_modulation', [('metastable_vacuum_lattice', 1), ('anomaly_specimen', 1)], [('false_vacuum', 250)],
     [('topological_defect_core', 4)], [], 1048576, 900, 'topological_defects', []),
    ('autocatalytic_broth', 'chemical_synthesis', [('zeolite_catalyst', 1), ('anomaly_specimen', 1)], [('ionic_liquid', 250), ('minecraft:water', 1000)],
     [], [('autocatalytic_broth', 8000)], 65536, 400, 'autocatalysis', []),
    ('autocatalytic_amplification', 'chemical_synthesis', [], [('autocatalytic_broth', 250), ('minecraft:water', 1000)],
     [], [('autocatalytic_broth', 12000)], 65536, 400, 'autocatalysis', ['zeolite_catalyst']),
    ('prion_misfolding', 'chemical_synthesis', [('folded_protein_crystal', 2)], [('autocatalytic_broth', 250)],
     [('prion_lattice', 4)], [], 65536, 400, 'autocatalysis', []),
    ('prion_templating', 'chemical_synthesis', [('prion_lattice', 1), ('folded_protein_crystal', 2)], [],
     [('prion_lattice', 12)], [], 65536, 400, 'autocatalysis', []),
    ('oncogenic_transformation', 'bioculture', [('stem_cell_culture', 1), ('anomaly_specimen', 1)], [('nutrient_broth', 250)],
     [('anomalous_strain_culture', 4)], [], 262144, 600, 'oncogenesis', []),
    ('undecidable_kernel', 'formal_proof', [('godel_sentence', 1), ('fractal_set', 1), ('anomaly_specimen', 1)], [],
     [('undecidable_kernel', 4)], [], 262144, 600, 'pathological_objects', []),
    # ---------------------------------------------------------------- REPLICATION & ESCAPE
    ('quine_replicator', 'nanofabrication', [('undecidable_kernel', 1), ('nanite_swarm', 1)], [('autocatalytic_broth', 250)],
     [('quine_replicator', 4)], [], 262144, 600, 'self_replication', []),
    ('telomerase_immortalisation', 'bioculture', [('anomalous_strain_culture', 1), ('telomerase_complex', 1)], [('nutrient_broth', 250)],
     [('immortal_cell_line', 4)], [], 262144, 600, 'immune_evasion', []),
    ('immortal_passage', 'bioculture', [('immortal_cell_line', 1)], [('nutrient_broth', 250)],
     [('immortal_cell_line', 12)], [], 262144, 600, 'immune_evasion', []),
    ('metastasis', 'directed_evolution', [('immortal_cell_line', 1), ('silicon_life_spore', 1)], [('xenoplasm', 100)],
     [('metastatic_spore', 6)], [], 4194304, 1200, 'immune_evasion', []),
    ('clonal_selection', 'directed_evolution', [('metastatic_spore', 1), ('immortal_cell_line', 1), ('engineered_enzyme', 1)], [('nutrient_broth', 100)],
     [('resistant_clone', 6)], [], 4194304, 1200, 'clonal_evolution', []),
    # ---------------------------------------------------------------- SYSTEMS
    ('microenvironment', 'bioculture', [('metastatic_spore', 1), ('stem_cell_culture', 1), ('engineered_enzyme', 1)], [('nutrient_broth', 250), ('xenoplasm', 100)],
     [('microenvironment_organoid', 4)], [], 262144, 800, 'invasion_fronts', []),
    ('contagion_mapping', 'holographic_encoding', [('quine_replicator', 1), ('resistant_clone', 1), ('holographic_matrix', 1)], [('synaptic_fluid', 100)],
     [('contagion_network_map', 4)], [], 1048576, 900, 'epidemic_dynamics', []),
    # ---------------------------------------------------------------- INTERACTION WITH NORMALITY
    ('law_leakage', WARD, [('topological_defect_core', 1), ('anomaly_specimen', 2), ('law_crystal', 1)], [('primordial_law', 50)],
     [('law_violation_shard', 8)], [], 16777216, 1200, 'anomalous_leakage', []),
    ('symbiosis', WARD, [('microenvironment_organoid', 1), ('xenobot_swarm', 1), ('prion_lattice', 1)], [('autocatalytic_broth', 100)],
     [('symbiont_culture', 8)], [], 16777216, 1200, 'symbiosis_parasitism', []),
    # ---------------------------------------------------------------- EXTENT
    ('anomaly_catalogue', WARD,
     [('topological_defect_core', 1), ('prion_lattice', 1), ('immortal_cell_line', 1), ('undecidable_kernel', 1), ('contagion_network_map', 1),
      ('law_violation_shard', 1), ('symbiont_culture', 1)], [('synaptic_fluid', 50)],
     [('anomaly_catalogue', 8)], [], 16777216, 1600, 'anomaly_scaling', []),
    # ---------------------------------------------------------------- CONTAINMENT & THERAPY
    ('quarantine_field', WARD, [('stabiliser_matrix', 2), ('causal_shield_lattice', 2), ('topological_defect_core', 1), ('contagion_network_map', 1)],
     [('superfluid_helium', 100)], [('quarantine_field_matrix', 8)], [], 16777216, 1600, 'containment_protocols', []),
    ('apoptosis_trigger', 'gene_sequencing', [('recombinant_dna', 2), ('engineered_enzyme', 1), ('resistant_clone', 1)], [],
     [('apoptosis_trigger', 4)], [], 262144, 600, 'targeted_therapy', []),
    ('car_t_engineering', 'bioculture', [('stem_cell_culture', 2), ('recombinant_dna', 1), ('contagion_network_map', 1)], [('nutrient_broth', 250)],
     [('car_t_culture', 4)], [], 262144, 800, 'targeted_therapy', []),
    ('remission', RES, [('resistant_clone', 1), ('apoptosis_trigger', 1), ('car_t_culture', 1)], [('nutrient_broth', 100)],
     [('stem_cell_culture', 16)], [('renormalised_essence', 16000)], 16777216, 1600, 'targeted_therapy', []),
    # ---------------------------------------------------------------- RESOLUTION
    ('resolve_anomaly', RES, [('anomaly_catalogue', 1), ('quarantine_field_matrix', 1), ('anomaly_specimen', 4), ('apoptosis_trigger', 1)],
     [('renormalised_essence', 100)], [('resolved_anomaly', 16)], [], 33554432, 2400, 'anomaly_resolution', []),
    ('renormalise_law', RES, [('law_violation_shard', 2), ('undecidable_kernel', 1)], [('renormalised_essence', 50)],
     [('resolved_anomaly', 4)], [], 16777216, 1600, 'anomaly_resolution', []),
    # ---------------------------------------------------------------- alternative routes into what came before
    ('morphogenic_serum_from_strain', 'bioculture', [('anomalous_strain_culture', 1), ('engineered_enzyme', 1)], [('nutrient_broth', 250)],
     [('morphogenic_serum', 4)], [], 262144, 600, 'oncogenesis', []),
    ('von_neumann_probe_from_quine', 'nanofabrication', [('quine_replicator', 2), ('claytronic_catom', 2), ('quantum_processor', 1)], [('nanite_slurry', 250)],
     [('von_neumann_probe', 1)], [], 1048576, 1200, 'self_replication', []),
    ('axiom_fragment_from_resolution', 'axiom_distillation', [('resolved_anomaly', 1), ('source_code_fragment', 1)], [('renormalised_essence', 100)],
     [('axiom_fragment', 4)], [], 16777216, 3600, 'anomaly_resolution', []),
    ('reality_anchor_from_resolution', 'reality_compilation', [('quarantine_field_matrix', 1), ('resolved_anomaly', 1), ('source_code_fragment', 1)],
     [('psionic_ether', 500)], [('reality_anchor', 2)], [], 16777216, 4800, 'anomaly_resolution', []),
]

LANG = {}


def _write_research(g):
    w, D, q = g['w'], g['D'], g['q']
    for rid, t, desc, icon, x, y, ins, req, cost in RESEARCH:
        w(os.path.join(D, 'ts_research', rid + '.json'), {
            'title': t, 'description': desc, 'icon': q(icon), 'x': x, 'y': y, 'insight': ins,
            'requires': [q(r) for r in req], 'cost': [{'item': q(c), 'count': n} for c, n in cost],
            # (round 5's study time, which ran before this round: 0.6·insight^1.35 seconds, in half minutes)
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
    p = os.path.join(HERE, 'puzzles2', 'group_anomaly.json')
    data = json.dumps(PUZZLES, ensure_ascii=False, indent=1)
    if not os.path.exists(p) or open(p, encoding='utf-8').read() != data:
        with open(p, 'w', encoding='utf-8') as f:
            f.write(data)


def _recipes(g):
    T = json.load(open(os.path.join(HERE, 'recipe_tuning.json')))
    cost = {int(k): v for k, v in T['cost_factor'].items()}
    fcost = {int(k): v for k, v in T.get('fluid_factor', T['cost_factor']).items()}
    dur = {int(k): v for k, v in T.get('duration_factor', {}).items()}
    item_cap = int(T.get('item_cap', 1024))
    by = {m['recipe']: m for m in g['M']}
    for name, typ, ins, fins, outs, fouts, fe, t, res, cats in RECIPES:
        m = by[typ]
        tier = g['TIER'].get(m['id'], 1)
        tank = int(T.get('fluid_cap_multiblock' if m['multiblock'] else 'fluid_cap_single', 2_048_000 if m['multiblock'] else 256_000))
        assert len(ins) <= m['inputs'] and len(fins) <= m['fluid_inputs'] and len(outs) <= m['outputs'] and len(fouts) <= m['fluid_outputs'], name
        assert len({i for i, _ in ins}) == len(ins) and len({f for f, _ in fins}) == len(fins), name
        ins_ = [(i, min(item_cap, n * cost.get(tier, 1))) for i, n in ins]
        fins_ = [(f, min(tank, n * fcost.get(tier, 1))) for f, n in fins]
        g['mr']('r34_' + name, typ, ins_, fins_, outs, fouts, energy=min(fe, 2147483647), duration=int(t * dur.get(tier, 1.0)), research=res,
                catalysts=tuple(cats))
        p = os.path.join(g['REC'], typ, 'r34_' + name + '.json')
        r = json.load(open(p))
        r['unscaled'] = True
        g['w'](p, r)


def _category(g):
    """The branch's own Codex category; round23 shows it on every science tab (see finish)."""
    g['w'](os.path.join(g['BA'], 'categories', CAT + '.json'), {
        'name': 'Abnormality & Infection', 'description': 'Anomalies in every science: how they arise, spread, escape and interact with the normal world - '
                                                          'and how to contain, study and finally explain them.',
        'icon': g['q']('anomaly_specimen'), 'sortnum': 19})


def apply(g):
    g['LANG'].update(LANG)
    _write_puzzles()
    _write_research(g)
    _recipes(g)
    _category(g)
    _chapters(g)


def finish(g):
    import round23
    # the branch links the four sciences: its chapters (and their nodes) sit on every science tab, and on Discovery
    for s in (round23.PHYS, round23.CHEM, round23.BIO, round23.MATH):
        s.add(CAT)


def _chapters(g):
    """Written in apply() - before round 5's node map is built - so every material hangs off the chapter (and is gated by
    the research) that explains it, not off the machine that happens to make it."""
    te = g['text_entry']
    # ---------------------------------------------------------------- the hub
    te(CAT, 'anomaly_intro', 'Abnormality & Infection', 'anomaly_specimen', [
        'Every science keeps a drawer of things that should not happen. Physics has defects and law violations, chemistry has molecules that make themselves, biology '
        'has cancer, mathematics has monsters. $(thing)Abnormality & Infection$() studies them together, because they behave alike: they $(thing)arise$() from a '
        'broken rule, $(thing)replicate$(), $(thing)escape$() what should destroy them, and $(thing)change$() the normal world they live in.',
        'You have met them already. A failing multiblock leaks them: $(thing)Fractured Reality$() spreads block to block like an infection of a higher-dimensional '
        'shadow, Nanite Swarms turn their hosts into new swarms, Husks, Void Wraiths, Paradox Echoes and Law Wraiths crawl out. What they leave - '
        '$(item)Fractured Essence$(), $(item)Void Essence$(), $(item)Paradox Residue$(), $(item)Nanite Swarms$(), $(item)Law Crystals$() - is the field sample.',
        'Build the $(item)Anomaly Containment Ward$(). It turns each of those drops, with a stabiliser matrix or causal shielding, into sixteen $(item)Contained '
        'Anomaly Specimens$() - the raw material of the whole branch. Its Operation console is an epidemic: the specimen grows at β, the ward clears it at γ + q, '
        'and R = β/(γ + q) must stay below 1. The faster it grows (just below 1), the more there is to study.',
        'Is it anomalous at all? Normal noise scatters as a Gaussian. A deviation of kσ happens by chance with probability ≈ √(2/π)e^{−k²/2}/k: one in 1.7 million '
        'at five sigma - the threshold at which physics believes a discovery. The branch runs in six stages: $(li)Origin$(li)Replication & escape$(li)The systems '
        'behind it$(li)Interaction with normality$(li)Extent$(li)Containment, study, isolation - and resolution.'],
        adv='anomalous_phenomena', sort=0, spot=['anomaly_containment_ward', 'anomaly_specimen', 'fractured_essence'])
    # ---------------------------------------------------------------- ORIGIN
    te(CAT, 'anomaly_origin_defects', 'Origins I: Broken Symmetry', 'topological_defect_core', [
        'How does an anomaly begin in physics? With a choice. A field whose potential has two equal valleys must fall into one as the universe cools - and regions '
        'too far apart to agree fall into different ones. Where they meet, the field is stuck on the hill between: a $(thing)domain wall$(). With more dimensions '
        'of choice you get strings, monopoles and textures - $(thing)topological defects$(), which no smooth change can remove.',
        'The wall\'s energy is E = ∫[½φ\'² + λ(φ² − v²)²/4]dx = (2√2/3)√λ v³: finite, localised and permanent. A $(thing)pocket of false vacuum$() is the same idea in '
        'reverse: a region left in the higher valley, held by its own wall, waiting.',
        'The $(item)Higgs Field Modulator$() quenches a $(item)Metastable Vacuum Lattice$() around a specimen in false vacuum and catches the seams as $(item)Topological '
        'Defect Cores$(). They are where the branch\'s physics leaks into everything else.'],
        adv='topological_defects', sort=1, spot=['topological_defect_core'])
    te(CAT, 'anomaly_origin_autocatalysis', 'Origins II: Molecules That Copy Themselves', 'prion_lattice', [
        'In chemistry an anomaly begins as $(thing)autocatalysis$(): a product that speeds up its own making. Its concentration does not rise steadily but explodes, '
        'and with feedback it can oscillate - a chemical clock - or spread as a wave through a still dish. The classic model feeds A and B into dX/dt = A − (B + 1)X + X²Y; '
        'past B = 1 + A² the steady state breaks and the colours start to pulse.',
        'A $(thing)prion$() is autocatalysis by shape. A protein folds wrong, and the wrong fold is the stable one: it touches a healthy copy and refolds it, and the '
        'misfolded stack grows into an amyloid fibre that breaks into more seeds. Nothing is alive, and yet it spreads, resists heat and protease, and kills.',
        'In the $(item)Chemical Reactor$(): a zeolite catalyst and a specimen in ionic liquid and water make $(fluid)Autocatalytic Broth$() - and broth with water and a '
        'zeolite catalyst (kept) makes more broth than went in. Folded protein crystals in broth misfold into a $(item)Prion Lattice$(); one lattice templates two '
        'crystals into twelve.'],
        adv='autocatalysis', sort=2, spot=['prion_lattice', 'autocatalytic_broth_cell'])
    te(CAT, 'anomaly_origin_oncogenesis', 'Origins III: Oncogenesis', 'anomalous_strain_culture', [
        'Cancer is the model anomaly: a part of the body that stops obeying the body. It is not one mistake but several. An $(thing)oncogene$() gets stuck on (RAS, MYC) '
        'and the cell is told to divide; a $(thing)tumour suppressor$() is lost - $(thing)p53$(), the guardian of the genome, which should stop a damaged cell or make it '
        'die - and nothing tells it to stop. Each mutation gives one clone an edge over its neighbours.',
        'That cancer needs several hits shows in the numbers: the incidence of most cancers rises as the fifth or sixth power of age, I(t) = Πk_j·t^{n−1}/(n − 1)!, as if '
        'n ≈ 6 independent steps were needed. What a cell has gained by the end are the $(thing)hallmarks of cancer$(): sustained growth signals, deaf to growth '
        'suppressors, no apoptosis, endless replication, its own blood supply, invasion - and later, an unstable genome, reprogrammed metabolism, hiding from immunity.',
        'In the $(item)Bioreactor$(), stem cells exposed to a specimen in nutrient broth lose their checkpoints: an $(item)Anomalous Strain Culture$(). (They also make the '
        'Breached Anomaly Engine\'s $(item)Morphogenic Serum$() - another way to it.)'],
        adv='oncogenesis', sort=3, spot=['anomalous_strain_culture'])
    te(CAT, 'anomaly_origin_pathology', 'Origins IV: Pathological Objects', 'undecidable_kernel', [
        'Mathematics breeds anomalies from perfectly good rules. In 1872 a sum of cosines, W(x) = Σaⁿcos(bⁿπx), turned out to be continuous everywhere and '
        'differentiable nowhere: a $(thing)monster$(), as Poincaré called such things. Its graph is a fractal of dimension 2 + ln a/ln b. Then came sets with no '
        'measure, space-filling curves, a sphere cut into a few pieces that reassemble into two.',
        '$(thing)Chaos$(): a deterministic system such as the logistic map xₙ₊₁ = rxₙ(1 − xₙ) can amplify every error exponentially - a positive Lyapunov exponent - and '
        'settle onto a $(thing)strange attractor$(), a fractal it never leaves and never repeats. Predicted exactly, it cannot be predicted at all.',
        '$(thing)Undecidability$(): some questions have answers no algorithm can find - whether a program halts, whether a statement follows from the axioms. Gödel '
        'and Turing showed the anomaly is built in. The $(item)Formal Proof Assistant$() compiles a Gödel sentence, a fractal set and a specimen into $(item)Undecidable '
        'Kernels$(): programs no other program can predict.'],
        adv='pathological_objects', sort=4, spot=['undecidable_kernel'])
    # ---------------------------------------------------------------- REPLICATION & ESCAPE
    te(CAT, 'anomaly_selfref', 'Self-Reference & Replicators', 'quine_replicator', [
        'A $(thing)quine$() is a program that prints its own source code. It needs two parts: a description of itself, and instructions to copy the description and '
        'build from it. Von Neumann found the same two parts in any machine that can build itself - years before DNA showed life does it that way. A computer virus, '
        'a gene and a $(thing)meme$() (an idea that spreads because it is good at spreading) are the same trick.',
        'Does a replicator\'s lineage survive? If each copy makes k copies with probability p_k, the chance q that it ever dies out is the smallest root of '
        'q = Σp_k q^k. If the mean number of copies is at most 1, it always dies out; above 1 it escapes destruction with probability 1 − q - for '
        'two-or-nothing copies at p = 0.6, one time in three.',
        'In the $(item)Nanite Assembler$(), an Undecidable Kernel with a nanite swarm in Autocatalytic Broth becomes four $(item)Quine Replicators$(). Two replicators, '
        'catoms and a processor in nanite slurry are another way to a $(item)Von Neumann Probe$().'],
        adv='self_replication', sort=5, spot=['quine_replicator'])
    te(CAT, 'anomaly_escape', 'Escape: Evasion, Immortality & Metastasis', 'metastatic_spore', [
        'The body destroys anomalous cells all the time. A tumour exists because it escaped. $(thing)Immune evasion$(): it sheds the markers T cells look for, shows '
        'PD-L1 to switch them off, and recruits cells that suppress inflammation. $(thing)Immortality$(): every division shortens the telomeres at the chromosome ends '
        'until a cell must stop - unless it switches $(thing)telomerase$() back on.',
        '$(thing)Angiogenesis$(): beyond a millimetre it starves, so it calls in new blood vessels (VEGF). Even then it slows: a tumour grows as N = K·exp(ln(N₀/K)e^{−αt}), '
        'fast when small and levelling off at the capacity of its supply. $(thing)Metastasis$(): cells loosen their grip (the epithelial-mesenchymal transition), '
        'enter the blood, survive the journey and grow again elsewhere. Most die; it only takes one.',
        'In the $(item)Bioreactor$(): a strain culture with a telomerase complex becomes an $(item)Immortal Cell Line$() - and an immortal line in broth is passaged '
        'into twelve, forever. In the $(item)Evolution Engine$(): an immortal line and a silicon spore in xenoplasm make $(item)Metastatic Spores$().'],
        adv='immune_evasion', sort=6, spot=['immortal_cell_line', 'metastatic_spore'])
    te(CAT, 'anomaly_clonal', 'Escape: Clonal Evolution & Resistance', 'resistant_clone', [
        'A tumour is not one enemy but a population of competing clones, each with its own mutations and its own fitness f_i. A clone gains share only when it beats '
        'the average: dx_i/dt = x_i(f_i − Σx_j f_j). Every treatment changes the fitnesses - and so it selects whichever clones it happens not to kill.',
        'That is how $(thing)drug resistance$() arises: a resistant cell one in a thousand, with an advantage of s = 0.5 per day under treatment, is 96% of the tumour '
        'twenty days later. It was there before the drug - the drug only chose it. Hence combination therapy: a clone resistant to two independent drugs at once is '
        'rarer by the product of their odds.',
        'In the $(item)Evolution Engine$(), metastatic spores, an immortal line and an enzyme under selection leave a $(item)Resistant Clone Colony$().'],
        adv='clonal_evolution', sort=7, spot=['resistant_clone'])
    # ---------------------------------------------------------------- SYSTEMS
    te(CAT, 'anomaly_systems', 'Systems: The Microenvironment & Invasion Fronts', 'microenvironment_organoid', [
        'An anomaly is a system, not a cell. A tumour builds a $(thing)microenvironment$(): blood vessels, fibroblasts, a stiffened matrix, immune cells talked into '
        'helping. Signalling networks - growth factors, cytokines, feedback loops - decide which cells divide, move or die, and a mutation in one hub rewires them all.',
        'Seen from far away, it invades as a $(thing)front$(): cells of density u diffuse (D) and grow (r) until crowded, ∂u/∂t = D∇²u + ru(1 − u/K), and any small seed '
        'becomes a front moving at c = 2√(Dr). Fractured Reality does the same on the block grid - a $(thing)cellular automaton$(), each block catching from its '
        'neighbours - until the healing outruns the spread.',
        'In the $(item)Bioreactor$(), a metastatic spore with stem cells and enzymes in broth and xenoplasm grows a $(item)Tumour Microenvironment Organoid$().'],
        adv='invasion_fronts', sort=8, spot=['microenvironment_organoid'])
    te(CAT, 'anomaly_epidemics', 'Systems: Epidemics, Percolation & Phase Transitions', 'contagion_network_map', [
        'Between hosts, an infection is bookkeeping. Split a population into susceptible S, infected I and recovered: dI/dt = βSI/N − (γ + μ)I. At the start everyone '
        'is susceptible, and the infection grows only if each case makes more than one more: $(thing)R₀ = β/(γ + μ) > 1$(). Once a fraction 1 − 1/R₀ is immune, it '
        'can no longer spread.',
        'That threshold is a $(thing)phase transition$(). Let each link of a network pass the infection with probability T: below a critical T, outbreaks stay small; '
        'above it a giant connected cluster appears and the infection $(thing)percolates$() through the whole population - like water through ground coffee, or a magnet '
        'ordering below its Curie point. On networks with hubs the critical T can be very small.',
        'In the $(item)Holographic Encoder$(), a replicator, a resistant clone and a holographic matrix write the $(item)Contagion Network Map$(): every path an '
        'anomaly can take, and where R crosses 1.'],
        adv='epidemic_dynamics', sort=9, spot=['contagion_network_map'])
    # ---------------------------------------------------------------- INTERACTION WITH NORMALITY
    te(CAT, 'anomaly_leakage', 'Where Anomaly Meets Normality', 'law_violation_shard', [
        'In quantum field theory an $(thing)anomaly$() is a technical word: a symmetry that holds in the classical theory and fails in the quantum one. The chiral '
        'charge of massless fermions should be conserved; in parallel electric and magnetic fields it is created from the vacuum at the rate e²E·B/2π² per unit '
        'volume. This $(thing)anomaly$() is real - it makes the neutral pion decay into two photons, and it is measured in crystals today.',
        'So normality can be broken locally, by exactly calculable amounts, without the rest of physics falling apart: the theory as a whole stays consistent '
        'because the anomalies of different particles cancel. Where a defect core touches ordinary space, such violations leak out.',
        'In the $(item)Anomaly Containment Ward$(), a Topological Defect Core, specimens and a Law Crystal in primordial law leave $(item)Local Law Violations$(): '
        'small regions where a conservation law quietly fails.'],
        adv='anomalous_leakage', sort=10, spot=['law_violation_shard'])
    te(CAT, 'anomaly_symbiosis', 'Symbiosis & Parasitism', 'symbiont_culture', [
        'An anomaly that lives inside a normal system is in a relationship with it. Write both as populations: dN₁/dt = r₁N₁(1 − (N₁ + αN₂)/K₁), and the mirror equation '
        'for N₂ with β. With α and β positive they compete; they coexist at N₁* = (K₁ − αK₂)/(1 − αβ) only if neither crowds the other out. Flip the sign of α and '
        'the invader helps its host instead.',
        'Nature has done it: mitochondria and chloroplasts were bacteria that invaded other cells two billion years ago and became their power plants. Gut bacteria, '
        'lichens, the viral genes in our own DNA that now build the placenta - the line between infection and partnership is one coefficient.',
        'In the $(item)Anomaly Containment Ward$(), a microenvironment organoid, a xenobot swarm and a prion lattice in Autocatalytic Broth are turned into a '
        '$(item)Symbiont Culture$(): the same anomaly, paying its host back.'],
        adv='symbiosis_parasitism', sort=11, spot=['symbiont_culture'])
    # ---------------------------------------------------------------- EXTENT
    te(CAT, 'anomaly_extent', 'The Extent of Anomalies', 'anomaly_catalogue', [
        'What can an anomaly do? The catalogue so far:$(li)break a symmetry and stay broken (defects)$(li)make itself from its surroundings (autocatalysis, prions)'
        '$(li)override the rules of its host (oncogenesis)$(li)defy prediction (chaos, undecidability)$(li)copy itself and escape (replicators, metastasis)'
        '$(li)evolve resistance$(li)spread through a population$(li)break a conservation law locally$(li)turn partner.',
        'How big do they get? Sizes of tumours, outbreaks, earthquakes, forest fires and ruptures follow $(thing)power laws$(), p(s) ∝ s^{−τ}: most are tiny, a few are '
        'enormous, and nothing sets a typical size. The mean is s₀(τ − 1)/(τ − 2) - and for τ ≤ 2 it is infinite: the largest anomaly you will ever meet outweighs '
        'all the others together. Systems tuned to the edge of a phase transition do this by themselves (self-organised criticality).',
        'The $(item)Anomaly Containment Ward$() binds one of every kind you have made into the $(item)Anomaly Catalogue$().'],
        adv='anomaly_scaling', sort=12, spot=['anomaly_catalogue'])
    # ---------------------------------------------------------------- CONTAINMENT, STUDY, ISOLATION
    te(CAT, 'anomaly_containment', 'Containment, Study & Isolation', 'quarantine_field_matrix', [
        'Containment is not destruction: you must keep the anomaly alive to study it. A unit that makes new ones at rate β, and is cleared naturally (γ) or by '
        'quarantine (q), makes R_e = ∫β e^{−(γ+q)τ}dτ = β/(γ + q) in its life. Quarantine works by pushing R_e below 1 - every outbreak then dies out - and the '
        'minimum field is q = β − γ.',
        'The protocols, from the clinic and the high-containment lab: $(li)isolate - one specimen, one cell, sealed$(li)reduce contact (q) without starving the '
        'sample$(li)watch R, not the count$(li)never study what you cannot destroy. The Containment Ward\'s console is exactly this: hold R below 1, close to it '
        'for the fastest work.',
        'In the Ward: stabiliser matrices, causal shielding, a defect core and a contagion map become $(item)Quarantine Field Matrices$().'],
        adv='containment_protocols', sort=13, spot=['quarantine_field_matrix'])
    te(CAT, 'anomaly_therapy', 'Targeted Therapy', 'apoptosis_trigger', [
        'Kill the anomaly, spare the normal. $(thing)CRISPR$() cuts DNA where a guide RNA matches, so it can write a trigger only the anomalous genome will read - '
        'restoring $(thing)apoptosis$(), the death a damaged cell was always meant to accept. $(thing)CAR-T$() cells are a patient\'s own T cells given a receptor for the '
        'anomaly\'s marker: living drugs that multiply as they find their target.',
        'And dose: n fractions of D let each cell survive with probability e^{−αD−βD²}. The anomaly is resolved only if not one of its N₀ clonogens survives: '
        'TCP = exp(−N₀e^{−n(αD+βD²)}). Normal tissue repairs between fractions; its damage grows as BED = nD(1 + D/3) - so many small fractions beat a few large ones.',
        'Build the $(item)Anomaly Resolution Engine$(). Its console is that trade: TCP above one half, normal-tissue BED below 120 Gy. The $(item)Gene Sequencer$() writes '
        '$(item)Apoptosis Triggers$(); the $(item)Bioreactor$() engineers $(item)CAR-T Cell Culture$(). In the Engine, a resistant clone with a trigger and CAR-T cells '
        'goes into remission: sixteen ordinary stem cells again, and $(fluid)Renormalised Essence$().'],
        adv='targeted_therapy', sort=14, spot=['anomaly_resolution_engine', 'apoptosis_trigger', 'car_t_culture'])
    te(CAT, 'anomaly_resolution', 'Resolution: The Anomaly Explained', 'resolved_anomaly', [
        'The end of an anomaly is understanding it. Physics has done this before: infinities in quantum field theory looked like a disaster until $(thing)renormalisation$() '
        'showed they only meant the constants depend on the scale you measure at. A measured quantity G then obeys [μ∂/∂μ + β(g)∂/∂g + nγ]G = 0, and at a fixed '
        'point it scales as μ^{−nγ}: the anomaly is just an $(thing)anomalous dimension$(), a number in a deeper law.',
        'Every stage of this branch was a piece of that: where it came from, how it copies, what system carries it, where it breaks the rules and by how much, how '
        'large it gets, how to hold it and how to cure it. With all of it, the $(item)Anomaly Resolution Engine$() resolves a catalogued, quarantined anomaly: '
        '$(item)Resolved Anomalies$(). A Local Law Violation with an Undecidable Kernel, renormalised, is resolved too: the broken law explained.',
        'Resolved anomalies are understood law, and the endgame can use them: in the $(item)Axiom Distiller$() one with source code in Renormalised Essence makes '
        '$(item)Axiom Fragments$(), and in the $(item)Reality Compiler$() a Quarantine Field Matrix and a resolved anomaly make a $(item)Reality Anchor$() - '
        'other ways to both.$(br2)It was never abnormal. It was only not yet explained.'],
        adv='anomaly_resolution', sort=15, spot=['resolved_anomaly', 'renormalised_essence_cell'])
