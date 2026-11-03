"""
Concept-field expansion: every science gets a real field of study to work through before its end products.

Fields
  Mathematics      numbers (N Z Q R C H O, surreals), number theory, analysis & calculus, functions, topology,
                   logic & set theory, category theory -> Absolute Infinity.  Has its own dimension: the Platonic Realm.
  Particle physics the Standard Model (quarks, leptons, gauge bosons, the Higgs) -> supersymmetry -> grand unification
  Relativity       Lorentz transformations, Minkowski spacetime, metric & curvature tensors, Einstein's field equations
  Stellar physics  protostars, main sequence, red giants, white dwarfs, the iron peak, supernovae, the r-process
  String theory    Kaluza-Klein, the dilaton, moduli, D-branes, Calabi-Yau, G2 manifolds -> M-theory.  Fed by the Bulk.
  Time             the arrow of time, temporal knots, Novikov self-consistency -> closed timelike curves
  Quantum fields   wave packets, spinors, quantum field lattices, Feynman diagrams

Avaritia-style cost (without a timewall): each field is finally compressed into a Conceptual Singularity from whole
stacks of everything it made. All nine singularities fuse into the Omniscience Catalyst, and that plus stacks of every
late metal makes the Omnium Ingot - which the Omnilex itself is now built from.
"""


def I(id, name, art, c1, c2, lore, glyph=None, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    if glyph:
        d['glyph'] = glyph
    d.update(kw)
    return d


ITEMS = [
    # ------------------------------------------------------------------ mathematics: numbers
    I('natural_number_lattice', 'Natural Number Lattice', 'glyph', '#2a3a5a', '#a0d0ff', 'One, two, three... Peano built all of arithmetic from zero and "the next one".', 'ℕ'),
    I('integer_ring', 'Integer Ring', 'glyph', '#3a2a4a', '#ff90c0', 'The naturals, plus their antimatter reflections: every number now has a negative.', 'ℤ'),
    I('rational_field', 'Rational Field', 'glyph', '#2a4a3a', '#90ffc0', 'Every ratio of integers. Dense everywhere - and still full of holes.', 'ℚ'),
    I('real_continuum', 'Real Continuum', 'glyph', '#4a3a1a', '#ffd070', 'The rationals with every gap filled - including the transcendental numbers mined from the Platonic Realm.', 'ℝ'),
    I('imaginary_unit', 'Imaginary Unit', 'glyph', '#1a2a4a', '#ffe14d', 'The square root of minus one, distilled from the imaginary mass of a tachyon.', 'i'),
    I('complex_plane', 'Complex Plane', 'glyph', '#201840', '#c0a0ff', 'Real and imaginary at right angles. Every polynomial finally has its roots.', 'ℂ'),
    I('quaternion_crystal', 'Quaternion Crystal', 'glyph', '#102a30', '#60ffe0', 'Hamilton carved i² = j² = k² = ijk = -1 into a bridge. Here it is carved into a crystal: pure rotation.', 'ℍ'),
    I('octonion_crystal', 'Octonion Crystal', 'glyph', '#30102a', '#ff60e0', 'Eight dimensions, and multiplication no longer associates. The largest number system that still divides.', '𝕆'),
    I('surreal_number', 'Surreal Number', 'glyph', '#0a0a14', '#ffffff', 'Conway\'s numbers: every real, every ordinal, every infinitesimal - all in one ordered field.', '★'),
    # number theory
    I('prime_shard', 'Prime Shard', 'shard', '#203050', '#80e0ff', 'Divisible only by itself and one. Grows naturally in the Platonic Realm; otherwise it has to be sieved out.'),
    I('modular_form', 'Modular Form', 'glyph', '#2a1a40', '#ff80ff', 'A function so symmetric it is called "the fifth fundamental operation of arithmetic".', 'τ'),
    I('riemann_zeta_crystal', 'Riemann Zeta Crystal', 'glyph', '#401020', '#ffb0b0', 'Every non-trivial zero lies on the critical line. Probably. This crystal grew along it.', 'ζ'),
    # analysis, calculus, functions
    I('transcendental_crystal', 'Transcendental Crystal', 'glyph', '#2a2010', '#ffc040', 'π and e, which no polynomial can reach. Only found in the Platonic Realm.', 'π'),
    I('infinitesimal', 'Infinitesimal', 'glyph', '#101820', '#c0e0ff', 'Smaller than every positive number, yet not zero.', 'ε'),
    I('function_graph', 'Function Graph', 'glyph', '#102030', '#60c0ff', 'A rule that sends every input to exactly one output - drawn as a curve of light.', 'ƒ'),
    I('derivative_operator', 'Derivative Operator', 'glyph', '#1a2030', '#80ffa0', 'Instantaneous rate of change: the slope of a curve at a single point.', '∂'),
    I('integral_operator', 'Integral Operator', 'glyph', '#30201a', '#ffb060', 'The area under a curve, summed from infinitely many infinitesimal slices.', '∫'),
    I('fourier_series', 'Fourier Series', 'glyph', '#102a2a', '#60ffd0', 'Any signal is a sum of waves. This one is a sum of all of them.', '∑'),
    I('fractal_set', 'Fractal Set', 'glyph', '#200a30', '#c060ff', 'z → z² + c, iterated forever. Infinitely detailed at every scale.', '❋'),
    I('fractal_dust', 'Fractal Dust', 'dots', '#200a30', '#c060ff', 'Self-similar grit from the Platonic Realm. Every grain contains smaller copies of the whole pile.'),
    I('differential_manifold', 'Differential Manifold', 'manifold', '#102030', '#80c0ff', 'A space that is flat up close and curved far away - the stage on which relativity is written.'),
    # topology
    I('mobius_strip', 'Möbius Strip', 'brane', '#203040', '#a0e0ff', 'One side, one edge. Walk along it and you come back upside down.'),
    I('klein_bottle', 'Klein Bottle', 'manifold', '#302040', '#ffa0ff', 'A bottle with no inside. It only fits together in four dimensions.'),
    # logic, sets, categories
    I('axiom_schema', 'Axiom Schema', 'glyph', '#202020', '#e0e0e0', 'The rules you agree to before you prove anything.', '⊢'),
    I('empty_set', 'Empty Set', 'glyph', '#0a0a10', '#a0a0ff', 'The set with nothing in it - made from the nothing a Void Wraith leaves behind.', '∅'),
    I('power_set', 'Power Set', 'glyph', '#1a1030', '#d0a0ff', 'The set of all subsets. Always strictly bigger than what you started with.', '℘'),
    I('formal_proof_tree', 'Formal Proof Tree', 'code', '#101820', '#80ff80', 'Every step justified, every axiom cited. Machines can check it; humans rarely can.'),
    I('continuum_shard', 'Continuum Shard', 'glyph', '#200a20', '#ffc0ff', 'The cardinality of the real line. Is it the next infinity after aleph-null? The axioms cannot say.', 'ℶ'),
    I('axiom_of_choice', 'Axiom of Choice', 'glyph', '#2a2a10', '#ffff80', 'From every set of sets you may pick one of each. Accept it and spheres can be doubled.', '∀'),
    I('category_lattice', 'Category Lattice', 'network', '#102030', '#80d0ff', 'Objects and arrows, and nothing else. All of mathematics fits inside.'),
    I('functor_bridge', 'Functor Bridge', 'glyph', '#201030', '#ff80d0', 'A map between categories that keeps every arrow pointing the right way.', '⇒'),
    I('topos', 'Topos', 'glyph', '#0a1018', '#c0ffff', 'A universe of mathematics with its own internal logic. Change the topos and you change what is true.', '⊤'),
    # ------------------------------------------------------------------ particle physics
    I('up_down_quarks', 'Up & Down Quarks', 'dots', '#ff4040', '#4080ff', 'The two lightest quark flavours - everything you have ever touched is made of them.'),
    I('strange_charm_quarks', 'Strange & Charm Quarks', 'dots', '#40ff80', '#ff80ff', 'The second generation: heavier, and they only exist for an instant outside strange matter.'),
    I('bottom_top_quarks', 'Bottom & Top Quarks', 'dots', '#ffd040', '#ffffff', 'The third generation. The top quark weighs as much as a gold atom and decays before it can bind to anything.'),
    I('lepton_triplet', 'Lepton Triplet', 'pair', '#40c0ff', '#e0f8ff', 'Electron, muon and tau - with a neutrino shadowing each.'),
    I('gauge_boson_cluster', 'Gauge Boson Cluster', 'bloch', '#ffe060', '#fffff0', 'Photon, gluon, W and Z: the carriers of every force except gravity.'),
    I('higgs_boson_crystal', 'Higgs Boson Crystal', 'gem', '#40ffa0', '#f0fff0', 'An excitation of the field that gives everything its mass, frozen before it could decay.'),
    I('standard_model_matrix', 'Standard Model Matrix', 'hexgrid', '#10203a', '#80e0ff', 'Every known particle in one lattice. It explains almost everything - except gravity and dark matter.'),
    I('superpartner_shard', 'Superpartner Shard', 'shard', '#1a0a30', '#a080ff', 'A neutralino: the supersymmetric twin of a boson. It turns out to be what dark matter is made of.'),
    I('grand_unified_core', 'Grand Unified Core', 'star', '#3a1060', '#ffd0ff', 'At high enough energy the strong, weak and electromagnetic forces are one force again.'),
    # ------------------------------------------------------------------ relativity
    I('lorentz_transform_crystal', 'Lorentz Transform Crystal', 'gem', '#203060', '#a0c0ff', 'Lengths shrink and clocks slow as you approach light speed. This crystal is always moving at 0.99c.'),
    I('minkowski_lattice', 'Minkowski Lattice', 'tesseract', '#101830', '#60a0ff', 'Space and time as one four-dimensional geometry, with light cones at every point.'),
    I('metric_tensor', 'Metric Tensor', 'glyph', '#102030', '#80e0ff', 'g_μν: the rule for measuring distance in curved spacetime.', 'g'),
    I('riemann_curvature_tensor', 'Riemann Curvature Tensor', 'glyph', '#201030', '#e080ff', 'How much a vector turns when carried around a small loop - curvature itself.', 'R'),
    I('einstein_field_core', 'Einstein Field Core', 'orb', '#10183a', '#c0e0ff', 'Matter tells spacetime how to curve; spacetime tells matter how to move. Both halves, in one core.'),
    I('gravitational_wave_crystal', 'Gravitational Wave Crystal', 'lattice', '#101828', '#80ffff', 'A ripple in spacetime from two merging black holes, caught and frozen mid-oscillation.'),
    I('kerr_metric_shard', 'Kerr Metric Shard', 'shard', '#0a0a10', '#ff9a30', 'The exact geometry around a spinning black hole - ergosphere and all.'),
    # ------------------------------------------------------------------ stellar physics
    I('protostar_core', 'Protostar Core', 'orb', '#602010', '#ff8040', 'A collapsing knot of nebular gas, hot enough to glow but not yet to fuse.'),
    I('red_giant_envelope', 'Red Giant Envelope', 'orb', '#801810', '#ff6040', 'The swollen outer layers of a dying star, a hundred times wider than it once was.'),
    I('white_dwarf_core', 'White Dwarf Core', 'orb', '#a0b0c0', '#ffffff', 'A star\'s carbon heart, the size of a planet and held up only by electron degeneracy.'),
    I('stellar_iron_ingot', 'Stellar Iron Ingot', 'ingot', '#704030', '#ffc080', 'Iron from the core of a star - the end of fusion. Making anything heavier costs energy instead of releasing it.'),
    I('supernova_remnant', 'Supernova Remnant', 'chunk', '#401040', '#ff80c0', 'What is left after a star\'s core collapses and its outer layers are blasted across the galaxy.'),
    I('r_process_cluster', 'R-Process Cluster', 'gem', '#806020', '#fff0a0', 'Gold, platinum and uranium, forged in seconds by the neutron flood of a supernova.'),
    # ------------------------------------------------------------------ string theory & the Bulk
    I('kaluza_klein_shard', 'Kaluza-Klein Shard', 'shard', '#102040', '#60c0ff', 'Momentum around a fifth dimension curled up too small to see. Mined in the Bulk.'),
    I('dilaton_crystal', 'Dilaton Crystal', 'gem', '#2a1040', '#ff80ff', 'A frozen value of the string coupling - how strongly strings interact. Grows on the Graviton Reefs of the Bulk.'),
    I('moduli_crystal', 'Moduli Crystal', 'crystal', '#103030', '#60ffd0', 'The shape and size of the hidden dimensions, crystallised. Found in the Moduli Wastes of the Bulk.'),
    I('brane_tension_fiber', 'Brane Tension Fiber', 'string', '#301040', '#e080ff', 'A thread pulled from the membrane a Brane Strider walks on.'),
    I('graviton_wisp_core', 'Graviton Wisp Core', 'orb', '#101030', '#a0b0ff', 'The heart of a Graviton Wisp: a closed loop of gravity that still pulls on your hand.'),
    I('compactified_circle', 'Compactified Circle', 'loop', '#102040', '#80d0ff', 'A whole dimension rolled up into a circle smaller than a Planck length.'),
    I('string_coupling_crystal', 'String Coupling Crystal', 'gem', '#301030', '#ff60ff', 'Tunes how strongly strings split and join. Turn it up and string theory becomes a different string theory.'),
    I('d_brane_sheet', 'D-Brane Sheet', 'brane', '#1a1030', '#c080ff', 'The surface where open strings end. Our universe might be one.'),
    I('g2_manifold', 'G₂ Manifold', 'manifold', '#2a0a30', '#ff80ff', 'Seven curled-up dimensions whose symmetry is the automorphism group of the octonions.'),
    I('m_theory_core', 'M-Theory Core', 'star', '#100820', '#e0c0ff', 'Eleven dimensions, and all five string theories turn out to be one.'),
    # ------------------------------------------------------------------ time
    I('arrow_of_time_shard', 'Arrow of Time Shard', 'shard', '#3a2a10', '#ffd060', 'Entropy only goes up - that is all "forward" means. This shard points the other way.'),
    I('temporal_knot', 'Temporal Knot', 'loop', '#2a2010', '#ffc23d', 'A worldline tied into a knot. Untie it and something happens twice.'),
    I('novikov_crystal', 'Novikov Crystal', 'gem', '#2a1a08', '#ffe0a0', 'The self-consistency principle, crystallised: any time loop it is part of has no paradoxes.'),
    # ------------------------------------------------------------------ quantum fields
    I('wave_packet', 'Wave Packet', 'streak', '#102030', '#60e0ff', 'A particle, written as a sum of waves - localised, but never exactly.'),
    I('spinor_lattice', 'Spinor Lattice', 'lattice', '#102a2a', '#60ffd0', 'Turn it through 360 degrees and it comes back negative. It takes 720 to restore it.'),
    I('quantum_field_lattice', 'Quantum Field Lattice', 'hexgrid', '#101830', '#80c0ff', 'Particles are only ripples. The field is what is real.'),
    I('feynman_diagram', 'Feynman Diagram', 'code', '#202020', '#ffffff', 'Every possible history, summed. Each line is a particle, each vertex an interaction.'),
    # ------------------------------------------------------------------ conceptual singularities (Avaritia-style sinks)
    I('mathematics_singularity', 'Mathematics Singularity', 'singularity', '#0a0a20', '#a0d0ff', 'Every number, function, proof and infinity, compressed into one point.', stack=16),
    I('standard_model_singularity', 'Standard Model Singularity', 'singularity', '#200a0a', '#ff8080', 'Every particle that exists, compressed into one point.', stack=16),
    I('spacetime_singularity', 'Spacetime Singularity', 'singularity', '#0a1020', '#80c0ff', 'All of relativity, compressed into the one place relativity breaks down.', stack=16),
    I('stellar_singularity', 'Stellar Singularity', 'singularity', '#201008', '#ffb060', 'The whole life of a star, compressed into one point.', stack=16),
    I('dimensional_singularity', 'Dimensional Singularity', 'singularity', '#100820', '#e080ff', 'All eleven dimensions, compressed into none.', stack=16),
    I('temporal_singularity', 'Temporal Singularity', 'singularity', '#201808', '#ffd060', 'Every moment there has ever been, compressed into one instant.', stack=16),
    I('quantum_singularity', 'Quantum Singularity', 'singularity', '#081820', '#60ffe0', 'Every possible state at once, compressed into one point.', stack=16),
    I('cognitive_singularity', 'Cognitive Singularity', 'singularity', '#082010', '#60ff9a', 'Every thought that could be thought, compressed into one point.', stack=16),
    I('exotic_matter_singularity', 'Exotic Matter Singularity', 'singularity', '#200818', '#ff60c0', 'Negative mass, antimatter, strange matter and dark matter, compressed into one point.', stack=16),
    I('theory_of_everything', 'Theory of Everything', 'omega', '#101020', '#ffffff', 'Quantum fields, gravity, strings and the Standard Model in one equation. It fits on a T-shirt.', stack=16),
    I('omniscience_catalyst', 'Omniscience Catalyst', 'omega', '#000000', '#ffffff', 'All nine singularities, fused. Holding it, you know everything - briefly.', stack=16),
    I('omnium_ingot', 'Omnium Ingot', 'ingot', '#c0c0ff', '#ffffff', 'A metal made of every concept there is. The last ingot.', stack=64),
    # ------------------------------------------------------------------ the Platonic Realm
    I('platonic_compass', 'Platonic Compass', 'analyzer', '#e0e0f0', '#80a0ff', 'Points at the realm of ideal forms - and, used, takes you there (and back).', special='compass', stack=1),
]

FLUIDS = [
    dict(id='nebular_gas', name='Nebular Gas', c1='#a060c0', c2='#ffd0ff', gas=True, light=4, temp=40, density=-50, viscosity=200, art='darkgas'),
]

BLOCKS = [
    dict(id='axiom_stone', name='Axiom Stone', art='axiomstone', c1='#d8dce8', c2='#8090c0', kind='simple', hardness=2.5, tool='stone'),
    dict(id='golden_ratio_glass', name='Golden Ratio Glass', art='fieldglass', c1='#c09030', c2='#ffe8a0', kind='glass', light=8, hardness=2.0, tool='none'),
    dict(id='prime_number_ore', name='Prime Number Ore', art='ore', base='axiom_stone', c1='#203050', c2='#80e0ff', kind='ore', drops='prime_shard', light=4, hardness=3.5, tool='iron'),
    dict(id='transcendental_ore', name='Transcendental Ore', art='ore', base='axiom_stone', c1='#4a3010', c2='#ffc040', kind='ore', drops='transcendental_crystal', light=5, hardness=4.0, tool='iron'),
    dict(id='fractal_ore', name='Fractal Ore', art='ore', base='axiom_stone', c1='#200a30', c2='#c060ff', kind='ore', drops='fractal_dust', light=5, hardness=4.0, tool='iron'),
    dict(id='kaluza_klein_ore', name='Kaluza-Klein Ore', art='ore', base='bulk_stone', c1='#102040', c2='#60c0ff', kind='ore', drops='kaluza_klein_shard', light=6, hardness=6.0, tool='diamond'),
    dict(id='dilaton_ore', name='Dilaton Ore', art='ore', base='bulk_stone', c1='#2a1040', c2='#ff80ff', kind='ore', drops='dilaton_crystal', light=6, hardness=6.0, tool='diamond'),
    dict(id='moduli_ore', name='Moduli Ore', art='ore', base='bulk_stone', c1='#103030', c2='#60ffd0', kind='ore', drops='moduli_crystal', light=6, hardness=6.0, tool='diamond'),
    dict(id='conceptual_casing', name='Conceptual Containment Casing', art='reality', c1='#0a0a18', c2='#e0e0ff', kind='casing', light=8, hardness=20.0, tool='diamond'),
]

# ------------------------------------------------------------------------------------------------ machines
# (enum name, recipe type, in, fin, out, fout, gen, multiblock, energy, io, effect, colour)
MACHINE_ENUM = [
    ('PEANO_ENGINE', 'arithmetic', 3, 1, 2, 0, None, 200_000, 4_000, 'NONE', 0xA0D0FF),
    ('ANALYTIC_CONTINUATOR', 'analysis', 3, 1, 2, 1, None, 400_000, 8_000, 'NONE', 0xFFD070),
    ('PROOF_ASSISTANT', 'formal_proof', 4, 1, 2, 0, None, 400_000, 8_000, 'NONE', 0x80FF80),
    ('PARTICLE_SPECTROMETER', 'spectrometry', 2, 1, 3, 1, None, 400_000, 8_000, 'NONE', 0xFF8060),
    ('TENSOR_ENGINE', 'tensor_calculus', 3, 1, 1, 0, None, 1_000_000, 20_000, 'NONE', 0x80C0FF),
    ('NUCLEOSYNTHESIS_CHAMBER', 'nucleosynthesis', 2, 2, 2, 1, None, 1_000_000, 20_000, 'NONE', 0xFFA040),
    ('COMPACTIFICATION_LATHE', 'compactification', 3, 1, 1, 1, None, 2_000_000, 50_000, 'NONE', 0xE080FF),
    ('CHRONO_LOOM', 'chronoweaving', 3, 1, 1, 1, None, 1_000_000, 20_000, 'NONE', 0xFFC23D),
    ('CONCEPT_COMPRESSOR', 'concept_compression', 9, 3, 1, 0, 'concept_compressor', 2_000_000_000, 500_000_000, 'CONCEPT', 0xE0E0FF),
]
MACHINE_TEXT = {
    'peano_engine': ('Peano Arithmetic Engine', 'Builds number systems the way mathematicians did: zero, then "the next one", then negatives, fractions, reals and beyond.'),
    'analytic_continuator': ('Analytic Continuator', 'Calculus in a box: limits, derivatives, integrals, series and the analytic continuation of functions into the complex plane.'),
    'proof_assistant': ('Formal Proof Assistant', 'Checks proofs one axiom at a time. Logic, set theory and category theory are all built here.'),
    'particle_spectrometer': ('Particle Spectrometer', 'Separates a spray of particles by mass and charge: quark flavours, leptons and gauge bosons, one by one.'),
    'tensor_engine': ('Tensor Calculus Engine', 'Computes in curved spacetime: Lorentz transformations, metrics, curvature and finally Einstein\'s field equations.'),
    'nucleosynthesis_chamber': ('Nucleosynthesis Chamber', 'Takes a star through its whole life - collapse, ignition, giant phase, death - and keeps the elements it forges.'),
    'compactification_lathe': ('Compactification Lathe', 'Rolls extra dimensions up small: Kaluza-Klein circles, Calabi-Yau and G₂ spaces, D-branes and finally M-theory itself.'),
    'chrono_loom': ('Chrono Loom', 'Weaves worldlines. Reverses the arrow of time, ties temporal knots and enforces self-consistency on time loops.'),
    'concept_compressor': ('Conceptual Singularity Compressor', 'Takes whole stacks of everything a field of science has produced and compresses them into a single singularity of pure concept.'),
}
MACHINE_CAT = {'peano_engine': 'mathematics', 'analytic_continuator': 'mathematics', 'proof_assistant': 'mathematics',
               'particle_spectrometer': 'exotic', 'tensor_engine': 'spacetime', 'nucleosynthesis_chamber': 'stellar',
               'compactification_lathe': 'strings', 'chrono_loom': 'spacetime', 'concept_compressor': 'endgame'}
MACHINE_RESEARCH = {'peano_engine': 'number_systems', 'analytic_continuator': 'real_analysis', 'proof_assistant': 'formal_logic',
                    'particle_spectrometer': 'standard_model', 'tensor_engine': 'special_relativity', 'nucleosynthesis_chamber': 'stellar_evolution',
                    'compactification_lathe': 'compactification', 'chrono_loom': 'temporal_mechanics', 'concept_compressor': 'conceptual_singularities'}
TIER = {'peano_engine': 1, 'analytic_continuator': 2, 'proof_assistant': 2, 'particle_spectrometer': 2, 'tensor_engine': 3,
        'nucleosynthesis_chamber': 3, 'compactification_lathe': 4, 'chrono_loom': 3, 'concept_compressor': 6}
S, PL, N = 'spacetime_casing', 'neutronium_plate', 'neutronium_ingot'
MC = {
    'peano_engine': (['QDQ', 'NSN', 'PCP'], {'Q': 'cherenkov_quartz', 'D': 'quantum_dot', 'N': N, 'S': S, 'P': PL, 'C': 'minecraft:comparator'}),
    'analytic_continuator': (['FXF', 'MSM', 'PCP'], {'F': 'rational_field', 'X': 'quantum_processor', 'M': 'metamaterial_sheet', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'proof_assistant': (['AXA', 'QSQ', 'PCP'], {'A': 'natural_number_lattice', 'X': 'quantum_processor', 'Q': 'qubit', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'particle_spectrometer': (['MXM', 'DSD', 'PCP'], {'M': 'dipole_bending_magnet', 'X': 'quantum_processor', 'D': 'calorimeter_detector_array', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'tensor_engine': (['MXM', 'LSL', 'PCP'], {'M': 'differential_manifold', 'X': 'quantum_processor', 'L': 'tachyon_crystal', 'S': 'hyperframe_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'nucleosynthesis_chamber': (['JXJ', 'MSM', 'PCP'], {'J': 'plasma_injection_nozzle', 'X': 'quantum_processor', 'M': 'magnetic_confinement_casing', 'S': 'event_horizon_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'compactification_lathe': (['KXK', 'CSC', 'PGP'], {'K': 'kaluza_klein_shard', 'X': 'quantum_processor', 'C': 'calabi_yau_manifold', 'S': 'hyperframe_casing', 'P': PL, 'G': 'graviton_crystal'}),
    'chrono_loom': (['TXT', 'CSC', 'PKP'], {'T': 'tachyon_crystal', 'X': 'quantum_processor', 'C': 'chronon_capacitor', 'S': 'chronoplastic_casing', 'P': PL, 'K': 'superconducting_coil'}),
    'concept_compressor': (['OTO', 'ASA', 'OEO'], {'O': 'ontological_processor', 'T': 'topos', 'A': 'aleph_null_shard', 'S': 'kugelblitz_chamber', 'E': 'conceptual_casing'}),
}
EXTRA = {
    'peano_engine': 'Start with $(item)Natural Number Lattices$(). Antimatter gives you negatives ($(item)Integer Ring$()), Casimir plates divide ($(item)Rational Field$()). For the reals you need $(item)Transcendental Crystals$() from the Platonic Realm.',
    'concept_compressor': 'Each recipe eats whole stacks: nine slots of a field\'s products plus three tanks. Nothing here waits on a timer - it only costs what it costs.$(br2)The Omniscience Catalyst is made in the $(item)Extreme Assembly Table$() from every singularity there is. Here, with cosmic neutronium or transfinite coils, catalysts are forged into $(item)Omnium$() far more efficiently than by hand.',
}

# ------------------------------------------------------------------------------------------------ research
# id, title, description, icon, x, y, insight, requires, cost
RESEARCH = [
    ('number_systems', 'Number Systems', 'Zero, and "the next one". From two rules, all of arithmetic. Unlocks the Peano Arithmetic Engine.', 'natural_number_lattice', 1, 7, 10, ['quantum_foundations'], [('cherenkov_quartz', 8)]),
    ('formal_logic', 'Formal Logic', 'Axioms, inference rules, proofs. Unlocks the Formal Proof Assistant and the Empty Set.', 'axiom_schema', 2, 9, 15, ['number_systems', 'quantum_entanglement'], [('natural_number_lattice', 4)]),
    ('platonic_realm', 'The Platonic Realm', 'If mathematical objects exist somewhere, they exist here. Unlocks the Platonic Compass.', 'platonic_compass', 2, 8, 20, ['number_systems'], [('natural_number_lattice', 8), ('integer_ring', 4)]),
    ('real_analysis', 'Real Analysis', 'Limits, continuity and the completion of the rationals into the reals. Unlocks the Analytic Continuator.', 'real_continuum', 3, 7, 25, ['platonic_realm', 'metamaterials'], [('rational_field', 4), ('transcendental_crystal', 2)]),
    ('complex_analysis', 'Complex Analysis', 'Add the square root of minus one and every polynomial has its roots. Fourier series and fractals follow.', 'complex_plane', 4, 8, 30, ['real_analysis', 'tachyonics'], [('real_continuum', 2)]),
    ('calculus', 'Calculus', 'Infinitesimals, derivatives and integrals - and manifolds, the spaces calculus lives on.', 'integral_operator', 5, 7, 30, ['real_analysis'], [('infinitesimal', 8)]),
    ('abstract_algebra', 'Abstract Algebra', 'Quaternions, octonions and spinors: number systems that give up commutativity, then associativity.', 'quaternion_crystal', 6, 8, 40, ['complex_analysis', 'particle_physics'], [('complex_plane', 2)]),
    ('number_theory', 'Number Theory', 'Primes, modular forms and the Riemann zeta function - the hardest problems in mathematics are about whole numbers.', 'riemann_zeta_crystal', 5, 9, 40, ['complex_analysis', 'formal_logic'], [('prime_shard', 16)]),
    ('set_theory', 'Set Theory', 'Everything is a set. Power sets, the continuum, and the axiom of choice.', 'power_set', 4, 10, 45, ['formal_logic', 'real_analysis'], [('empty_set', 4)]),
    ('geometric_topology', 'Geometric Topology', 'Surfaces with one side and bottles with no inside.', 'klein_bottle', 7, 9, 50, ['calculus', 'topology'], [('mobius_strip', 2)]),
    ('category_theory', 'Category Theory', 'Objects and arrows. Functors between categories, and toposes - universes with their own logic.', 'topos', 9, 9, 120, ['set_theory', 'geometric_topology', 'abstract_algebra'], [('function_graph', 16)]),
    ('quantum_field_theory', 'Quantum Field Theory', 'Particles are excitations of fields. Wave packets, spinors, field lattices and Feynman diagrams.', 'quantum_field_lattice', 6, 6, 50, ['calculus', 'abstract_algebra', 'particle_physics'], [('wave_packet', 4)]),
    ('standard_model', 'The Standard Model', 'Six quarks, six leptons, four gauge bosons and the Higgs. Unlocks the Particle Spectrometer.', 'standard_model_matrix', 7, 5, 45, ['particle_physics', 'accelerator_physics'], [('strange_matter_pellet', 4)]),
    ('supersymmetry', 'Supersymmetry', 'Every boson has a fermion twin. The lightest of them is dark matter.', 'superpartner_shard', 9, 5, 90, ['standard_model', 'dark_sector', 'quantum_field_theory'], [('standard_model_matrix', 1)]),
    ('grand_unification', 'Grand Unification', 'At high enough energy, three forces become one.', 'grand_unified_core', 11, 5, 150, ['supersymmetry', 'string_theory'], [('superpartner_shard', 4)]),
    ('special_relativity', 'Special Relativity', 'The speed of light is the same for everyone - so time and space cannot be. Unlocks the Tensor Calculus Engine.', 'lorentz_transform_crystal', 4, 5, 35, ['tachyonics', 'calculus'], [('real_continuum', 4)]),
    ('general_relativity', 'General Relativity', 'Gravity is the curvature of spacetime. Metrics, curvature and Einstein\'s field equations.', 'einstein_field_core', 8, 6, 80, ['special_relativity', 'singularity_physics'], [('metric_tensor', 2)]),
    ('stellar_evolution', 'Stellar Evolution', 'Stars are born from nebulae, live on the main sequence, swell into giants and die. Unlocks the Nucleosynthesis Chamber.', 'protostar_core', 8, 3, 70, ['dark_sector', 'plasma_metallurgy'], [('degenerate_matter_chunk', 16)]),
    ('nucleosynthesis', 'Nucleosynthesis', 'Every element heavier than helium was made in a star. Iron is where fusion stops; supernovae make the rest.', 'r_process_cluster', 9, 3, 90, ['stellar_evolution'], [('white_dwarf_core', 2)]),
    ('compactification', 'Compactification', 'Where are the extra dimensions? Rolled up too small to see. Unlocks the Compactification Lathe.', 'compactified_circle', 12, -3, 110, ['brane_cosmology', 'string_theory', 'abstract_algebra'], [('kaluza_klein_shard', 8)]),
    ('temporal_mechanics', 'Temporal Mechanics', 'The arrow of time, and how to tie it in knots without causing a paradox. Unlocks the Chrono Loom.', 'temporal_knot', 5, -4, 60, ['chronodynamics', 'thermodynamics_of_information'], [('chronon_capacitor', 2)]),
    ('conceptual_singularities', 'Conceptual Singularities', 'Compress an entire field of knowledge into one point. Unlocks the Conceptual Singularity Compressor.', 'mathematics_singularity', 15, 3, 400, ['category_theory', 'simulation_hypothesis', 'transfinite_mathematics'], [('topos', 1)]),
    ('theory_of_everything', 'Theory of Everything', 'Quantum fields, gravity, strings and the Standard Model as one theory.', 'theory_of_everything', 16, 3, 500, ['conceptual_singularities', 'grand_unification', 'general_relativity', 'm_theory'], [('m_theory_core', 1)]),
]
# extra prerequisites for existing research, so the old shortcuts now need the new fields
RESEARCH_REQ_ADD = {
    'transfinite_mathematics': ['set_theory', 'number_theory'],
    'traversable_wormholes': ['general_relativity'],
    'causality_violation': ['temporal_mechanics'],
    'm_theory': ['compactification'],
    'stellar_engineering': ['stellar_evolution'],
    'incompleteness': ['category_theory'],
    'reality_breach': ['conceptual_singularities'],
    'omnilex': ['theory_of_everything'],
    'mass_manipulation': ['quantum_field_theory'],
}

CATS = [('mathematics', 'Mathematics', 'Numbers, analysis, logic, sets, categories - and the Platonic Realm where they live.', 'natural_number_lattice', 9)]


def recipes(mr, shaped, shapeless):
    # ================================================================== MATHEMATICS
    R = 'number_systems'
    mr('natural_number_lattice', 'arithmetic', [('qubit', 1), ('cherenkov_quartz', 4)], [], [('natural_number_lattice', 2)], energy=128, duration=200, research=R)
    mr('integer_ring', 'arithmetic', [('natural_number_lattice', 2), ('antimatter_pellet', 1)], [], [('integer_ring', 4)], energy=256, duration=200, research=R)
    mr('integer_ring_early', 'arithmetic', [('natural_number_lattice', 4), ('fractured_essence', 1)], [], [('integer_ring', 2)], energy=256, duration=300, research=R)
    mr('rational_field', 'arithmetic', [('integer_ring', 2), ('casimir_plate', 1)], [], [('rational_field', 2)], energy=384, duration=200, research=R)
    mr('prime_sieve', 'arithmetic', [('natural_number_lattice', 16), ('quantum_processor', 1)], [], [('prime_shard', 2)], energy=1024, duration=600, research='number_theory',
       catalysts=['quantum_processor'])
    mr('imaginary_unit', 'arithmetic', [('tachyon_crystal', 1), ('real_continuum', 1)], [('tachyon_flux', 250)], [('imaginary_unit', 2)], energy=1024, duration=300, research='complex_analysis')
    mr('quaternion_crystal', 'arithmetic', [('complex_plane', 2), ('magnetic_monopole', 1)], [('bose_einstein_condensate', 500)], [('quaternion_crystal', 1)], energy=4096, duration=400, research='abstract_algebra')
    mr('octonion_crystal', 'arithmetic', [('quaternion_crystal', 2), ('calabi_yau_manifold', 1), ('prime_shard', 8)], [('exotic_matter', 500)], [('octonion_crystal', 1)], energy=16384, duration=600, research='abstract_algebra')
    mr('spinor_lattice', 'arithmetic', [('quaternion_crystal', 1), ('qubit', 4)], [], [('spinor_lattice', 2)], energy=4096, duration=300, research='quantum_field_theory')
    # analysis
    R = 'real_analysis'
    mr('real_continuum', 'analysis', [('rational_field', 4), ('transcendental_crystal', 1), ('metamaterial_sheet', 1)], [('bose_einstein_condensate', 250)],
       [('real_continuum', 2)], energy=1024, duration=300, research=R)
    mr('infinitesimal', 'analysis', [('real_continuum', 1), ('quantum_dot', 4)], [('superfluid_helium', 100)], [('infinitesimal', 8)], [('warm_helium', 100)], energy=1024, duration=200, research=R)
    mr('function_graph', 'analysis', [('real_continuum', 1), ('photonic_neuron', 1)], [], [('function_graph', 2)], energy=1024, duration=200, research=R)
    mr('complex_plane', 'analysis', [('real_continuum', 2), ('imaginary_unit', 2)], [('bose_einstein_condensate', 250)], [('complex_plane', 1)], energy=2048, duration=300, research='complex_analysis')
    mr('fourier_series', 'analysis', [('function_graph', 2), ('complex_plane', 1), ('integral_operator', 1)], [], [('fourier_series', 2)], energy=4096, duration=300, research='complex_analysis')
    mr('fractal_set', 'analysis', [('complex_plane', 2), ('function_graph', 2), ('fractal_dust', 8)], [], [('fractal_set', 1)], energy=4096, duration=400, research='complex_analysis')
    mr('derivative_operator', 'analysis', [('infinitesimal', 4), ('function_graph', 1)], [], [('derivative_operator', 1)], energy=2048, duration=200, research='calculus')
    mr('integral_operator', 'analysis', [('infinitesimal', 8), ('function_graph', 1)], [], [('integral_operator', 1)], energy=2048, duration=300, research='calculus')
    mr('differential_manifold', 'analysis', [('derivative_operator', 2), ('integral_operator', 1), ('topological_insulator', 1)], [('bose_einstein_condensate', 250)],
       [('differential_manifold', 1)], energy=4096, duration=400, research='calculus')
    mr('modular_form', 'analysis', [('prime_shard', 4), ('complex_plane', 1), ('fourier_series', 1)], [], [('modular_form', 1)], energy=8192, duration=400, research='number_theory')
    mr('riemann_zeta_crystal', 'analysis', [('prime_shard', 16), ('complex_plane', 2), ('modular_form', 2)], [('superfluid_helium', 1000)],
       [('riemann_zeta_crystal', 1)], [('warm_helium', 1000)], energy=16384, duration=800, research='number_theory')
    mr('wave_packet', 'analysis', [('fourier_series', 1), ('entangled_photon_pair', 4)], [], [('wave_packet', 4)], energy=4096, duration=300, research='quantum_field_theory')
    mr('quantum_field_lattice', 'analysis', [('wave_packet', 4), ('spinor_lattice', 2), ('differential_manifold', 1)], [('bose_einstein_condensate', 1000)],
       [('quantum_field_lattice', 1)], energy=8192, duration=600, research='quantum_field_theory')
    # topology
    mr('mobius_strip', 'weaving', [('metamaterial_sheet', 2), ('topological_insulator', 1), ('function_graph', 1)], [('bose_einstein_condensate', 250)],
       [('mobius_strip', 1)], energy=2048, duration=300, research='geometric_topology')
    mr('klein_bottle', 'hyperdimensional', [('mobius_strip', 2), ('differential_manifold', 1), ('tesseract_core', 1)], [('exotic_matter', 500)],
       [('klein_bottle', 1)], [('dimensional_residue', 100)], energy=32768, duration=600, research='geometric_topology')
    # logic & sets
    R = 'formal_logic'
    mr('axiom_schema', 'formal_proof', [('natural_number_lattice', 2), ('qubit', 1)], [], [('axiom_schema', 2)], energy=512, duration=200, research=R)
    mr('empty_set', 'formal_proof', [('void_essence', 1)], [], [('empty_set', 4)], energy=512, duration=200, research=R)
    mr('empty_set_lattice', 'formal_proof', [('void_lattice_shard', 4), ('axiom_schema', 1)], [], [('empty_set', 1)], energy=1024, duration=400, research=R)
    mr('formal_proof_tree', 'formal_proof', [('axiom_schema', 4), ('quantum_processor', 1), ('function_graph', 1)], [('synaptic_fluid', 250)],
       [('formal_proof_tree', 2)], energy=2048, duration=400, research=R)
    mr('power_set', 'formal_proof', [('empty_set', 2), ('natural_number_lattice', 4), ('formal_proof_tree', 1)], [], [('power_set', 1)], energy=4096, duration=400, research='set_theory')
    mr('axiom_of_choice', 'formal_proof', [('axiom_schema', 8), ('continuum_shard', 1), ('formal_proof_tree', 2)], [], [('axiom_of_choice', 1)], energy=65536, duration=600, research='set_theory')
    mr('category_lattice', 'formal_proof', [('function_graph', 4), ('axiom_schema', 2), ('power_set', 1)], [], [('category_lattice', 1)], energy=16384, duration=400, research='category_theory')
    mr('functor_bridge', 'formal_proof', [('category_lattice', 2), ('differential_manifold', 1), ('octonion_crystal', 1)], [], [('functor_bridge', 1)], energy=65536, duration=600, research='category_theory')
    mr('topos', 'formal_proof', [('functor_bridge', 2), ('klein_bottle', 1), ('continuum_shard', 1), ('godel_sentence', 1)], [('synaptic_fluid', 2000)],
       [('topos', 1)], energy=262144, duration=1200, research='category_theory')
    mr('feynman_diagram', 'formal_proof', [('quantum_field_lattice', 1), ('gauge_boson_cluster', 1), ('integral_operator', 2)], [], [('feynman_diagram', 2)],
       energy=16384, duration=400, research='quantum_field_theory')
    # transfinite (Cantor Diagonalizer) - rewired: the old shortcuts are gone
    T = 'transfinite_computation'
    mr('aleph_null_shard', T, [('power_set', 2), ('natural_number_lattice', 32), ('formal_proof_tree', 2), ('superintelligence_core', 1)], [('synaptic_fluid', 1000)],
       [('aleph_null_shard', 2)], energy=262144, duration=600, research='transfinite_mathematics')
    mr('continuum_shard', T, [('aleph_null_shard', 2), ('real_continuum', 16), ('power_set', 4)], [('dark_energy', 1000)], [('continuum_shard', 1)],
       energy=524288, duration=800, research='transfinite_mathematics')
    mr('surreal_number', T, [('octonion_crystal', 1), ('real_continuum', 16), ('aleph_null_shard', 1), ('infinitesimal', 32)], [('dark_energy', 2000)],
       [('surreal_number', 1)], energy=1048576, duration=1000, research='transfinite_mathematics')
    mr('godel_sentence', T, [('formal_proof_tree', 8), ('aleph_null_shard', 2), ('superintelligence_core', 1), ('riemann_zeta_crystal', 1)], [('synaptic_fluid', 2000)],
       [('godel_sentence', 1)], energy=524288, duration=800, research='incompleteness')
    mr('russell_paradox_set', T, [('godel_sentence', 1), ('aleph_null_shard', 2), ('paradox_residue', 4), ('power_set', 8)], [('chronon_gas', 2000)],
       [('russell_paradox_set', 1)], energy=524288, duration=800, research='paradox_engineering')
    mr('banach_tarski_sphere', T, [('tesseract_core', 1), ('aleph_null_shard', 2), ('negative_mass_ingot', 2), ('axiom_of_choice', 1)], [],
       [('banach_tarski_sphere', 1)], energy=524288, duration=800, research='paradox_engineering')
    mr('absolute_infinity', T, [('mathematics_singularity', 1), ('surreal_number', 4), ('topos', 2), ('continuum_shard', 8)], [('dark_energy', 16000)],
       [('absolute_infinity', 1)], energy=67108864, duration=2400, research='transfinite_mathematics')

    # ================================================================== PARTICLE PHYSICS
    R = 'standard_model'
    mr('up_down_quarks', 'spectrometry', [('neutronium_ingot', 1)], [('quark_gluon_plasma', 250)], [('up_down_quarks', 4)], [], energy=2048, duration=200, research=R)
    mr('strange_charm_quarks', 'spectrometry', [('strange_matter_pellet', 1)], [('quark_gluon_plasma', 500)], [('strange_charm_quarks', 2), ('up_down_quarks', 1)],
       [('strange_matter', 100)], energy=4096, duration=300, research=R)
    mr('lepton_triplet', 'spectrometry', [('sterile_neutrino', 1), ('antimatter_pellet', 1)], [('neutrino_flux', 500)], [('lepton_triplet', 2)], [], energy=4096, duration=300, research=R)
    mr('gauge_boson_cluster', 'spectrometry', [('entangled_photon_pair', 4), ('glueball_core', 1)], [('quark_gluon_plasma', 1000)], [('gauge_boson_cluster', 2)], [],
       energy=8192, duration=400, research=R)
    mr('bottom_top_quarks', 'synchrotron', [('strange_charm_quarks', 2), ('pentaquark_shard', 1)], [('quark_gluon_plasma', 1000)], [('bottom_top_quarks', 1)], [],
       energy=131072, duration=600, research=R, min_tier=2)
    mr('higgs_boson_crystal', 'synchrotron', [('gauge_boson_cluster', 2), ('bottom_top_quarks', 1), ('higgs_field_capacitor', 1)], [('quark_gluon_plasma', 2000)],
       [('higgs_boson_crystal', 1)], [], energy=524288, duration=1000, research=R, min_tier=3)
    mr('standard_model_matrix', 'femto_assembly', [('up_down_quarks', 16), ('strange_charm_quarks', 8), ('bottom_top_quarks', 4), ('lepton_triplet', 8),
                                                   ('gauge_boson_cluster', 4), ('higgs_boson_crystal', 1), ('topological_processor', 1)],
       [('exotic_solder', 576), ('quark_gluon_plasma', 2000)], [('standard_model_matrix', 1)], energy=1048576, duration=1200, research=R)
    mr('superpartner_shard', 'collider', [('standard_model_matrix', 1), ('dark_matter_lens', 1)], [('dark_matter', 2000)], [('superpartner_shard', 2)], [('dark_matter', 500)],
       energy=262144, duration=800, research='supersymmetry')
    mr('grand_unified_core', 'hyperdimensional', [('standard_model_matrix', 2), ('superpartner_shard', 4), ('magnetic_monopole', 4), ('quantum_string', 4)],
       [('quark_gluon_plasma', 4000), ('exotic_matter', 2000)], [('grand_unified_core', 1)], [('dimensional_residue', 500)], energy=1048576, duration=1600, research='grand_unification')
    # rewired: the Higgs catalyst needs a real Higgs boson now
    mr('higgs_catalyst', 'mass_modulation', [('higgs_boson_crystal', 1), ('higgs_field_capacitor', 4)], [('bose_einstein_condensate', 2000)], [('higgs_catalyst', 1)],
       energy=65536, duration=600, research='mass_manipulation')

    # ================================================================== RELATIVITY
    R = 'special_relativity'
    mr('lorentz_transform_crystal', 'tensor_calculus', [('entangled_photon_pair', 4), ('tachyon_crystal', 1), ('real_continuum', 1)], [('tachyon_flux', 250)],
       [('lorentz_transform_crystal', 2)], energy=4096, duration=300, research=R)
    mr('minkowski_lattice', 'tensor_calculus', [('lorentz_transform_crystal', 2), ('differential_manifold', 1), ('quaternion_crystal', 1)], [],
       [('minkowski_lattice', 1)], energy=8192, duration=400, research=R)
    R = 'general_relativity'
    mr('metric_tensor', 'tensor_calculus', [('minkowski_lattice', 1), ('derivative_operator', 2), ('neutronium_plate', 4)], [], [('metric_tensor', 2)], energy=16384, duration=400, research=R)
    mr('riemann_curvature_tensor', 'tensor_calculus', [('metric_tensor', 2), ('derivative_operator', 4), ('micro_singularity', 1)], [], [('riemann_curvature_tensor', 1)],
       energy=32768, duration=600, research=R)
    mr('einstein_field_core', 'tensor_calculus', [('riemann_curvature_tensor', 2), ('integral_operator', 2), ('stellar_core', 1)], [('exotic_matter', 500)],
       [('einstein_field_core', 1)], energy=65536, duration=800, research=R)
    mr('kerr_metric_shard', 'tensor_calculus', [('einstein_field_core', 1), ('penrose_spindle', 1), ('micro_singularity', 1)], [], [('kerr_metric_shard', 2)],
       energy=65536, duration=800, research=R)
    mr('gravitational_wave_crystal', 'kugelblitz', [('micro_singularity', 2), ('einstein_field_core', 1), ('kerr_metric_shard', 1)], [('hawking_radiation', 1000)],
       [('gravitational_wave_crystal', 4)], [], energy=131072, duration=800, research=R)
    # rewired: wormholes and warp coils need real general relativity
    mr('wormhole_throat', 'hyperdimensional', [('negative_mass_ingot', 4), ('exotic_matter_crystal', 4), ('white_hole_fragment', 1), ('einstein_field_core', 1)],
       [('exotic_matter', 2000)], [('wormhole_throat', 1)], [('dimensional_residue', 500)], energy=262144, duration=1200, research='traversable_wormholes')
    mr('alcubierre_coil', 'hyperdimensional', [('superconducting_coil', 4), ('exotic_matter_crystal', 4), ('metric_tensor', 2), ('lorentz_transform_crystal', 2)],
       [('exotic_matter', 2000)], [('alcubierre_coil', 1)], [], energy=131072, duration=800, research='hyperdimensional_geometry')

    # ================================================================== STELLAR PHYSICS
    R = 'stellar_evolution'
    mr('nebular_gas', 'halo_collection', [('metamaterial_sheet', 1)], [], [], [('nebular_gas', 1000)], energy=4096, duration=400, research=R)
    mr('protostar_core', 'nucleosynthesis', [('degenerate_matter_chunk', 8), ('graviton_crystal', 1)], [('nebular_gas', 4000)], [('protostar_core', 1)], [],
       energy=65536, duration=800, research=R)
    mr('protostar_core_early', 'nucleosynthesis', [('degenerate_matter_chunk', 16), ('neutronium_block', 1)], [('nebular_gas', 8000)], [('protostar_core', 1)], [],
       energy=65536, duration=1200, research=R)
    mr('main_sequence_ignition', 'nucleosynthesis', [('protostar_core', 1), ('antimatter_pellet', 2)], [('nebular_gas', 8000), ('quark_gluon_plasma', 1000)],
       [('stellar_core', 1)], [('stellar_plasma', 1000)], energy=262144, duration=1600, research=R)
    mr('red_giant_phase', 'nucleosynthesis', [('stellar_core', 1)], [('stellar_plasma', 4000)], [('red_giant_envelope', 2), ('white_dwarf_core', 1)], [],
       energy=262144, duration=1600, research=R)
    R = 'nucleosynthesis'
    mr('stellar_iron_ingot', 'nucleosynthesis', [('white_dwarf_core', 1), ('minecraft:iron_ingot', 16)], [('stellar_plasma', 1000)], [('stellar_iron_ingot', 8)], [],
       energy=131072, duration=800, research=R)
    mr('supernova_remnant', 'stellar_compression', [('red_giant_envelope', 2), ('stellar_iron_ingot', 8), ('neutronium_block', 1)], [('neutrino_flux', 1000)],
       [('supernova_remnant', 1), ('magnetar_shard', 1)], [], energy=1048576, duration=1200, research=R)
    mr('r_process_cluster', 'nucleosynthesis', [('supernova_remnant', 1), ('sterile_neutrino', 1)], [('neutrino_flux', 2000)], [('r_process_cluster', 4), ('minecraft:gold_ingot', 8)], [],
       energy=262144, duration=800, research=R)
    # rewired: compact stars need a supernova first; Dyson shells are made of stellar iron
    mr('quark_star_core', 'stellar_compression', [('neutronium_block', 4), ('strange_matter_pellet', 1), ('supernova_remnant', 1)], [('quark_gluon_plasma', 2000)],
       [('quark_star_core', 1), ('magnetar_shard', 2)], [], energy=2097152, duration=1200, research='compact_stars', min_tier=2)
    mr('dyson_shell_segment', 'weaving', [('stellar_iron_ingot', 4), ('metamaterial_sheet', 2), ('higgs_field_capacitor', 1)], [('stellar_plasma', 1000)],
       [('dyson_shell_segment', 1)], energy=8192, duration=400, research='dyson_sphere')

    # ================================================================== STRING THEORY (fed by the Bulk)
    R = 'compactification'
    mr('compactified_circle', 'compactification', [('kaluza_klein_shard', 4), ('quantum_string', 2)], [('exotic_matter', 250)], [('compactified_circle', 1)], [],
       energy=32768, duration=400, research=R)
    mr('string_coupling_crystal', 'compactification', [('dilaton_crystal', 4), ('quantum_string', 1)], [('exotic_matter', 250)], [('string_coupling_crystal', 1)], [],
       energy=32768, duration=400, research=R)
    mr('d_brane_sheet', 'compactification', [('quantum_string', 4), ('brane_tension_fiber', 2), ('metamaterial_sheet', 1)], [('exotic_matter', 500)], [('d_brane_sheet', 2)], [],
       energy=32768, duration=400, research=R)
    mr('g2_manifold', 'compactification', [('calabi_yau_manifold', 1), ('moduli_crystal', 4), ('octonion_crystal', 1)], [('exotic_matter', 1000)], [('g2_manifold', 1)],
       [('dimensional_residue', 250)], energy=131072, duration=800, research=R)
    mr('graviton_from_wisp', 'compactification', [('graviton_wisp_core', 2), ('compactified_circle', 1)], [('hawking_radiation', 500)], [('graviton_crystal', 4)], [],
       energy=65536, duration=400, research=R)
    mr('m_theory_core', 'compactification', [('m_brane', 2), ('g2_manifold', 1), ('compactified_circle', 4)], [('exotic_matter', 4000)], [('m_theory_core', 1)],
       [('dimensional_residue', 1000)], energy=1048576, duration=1600, research='m_theory')
    # rewired: membranes are made from D-branes in a G2 compactification
    mr('m_brane', 'string_weaving', [('d_brane_sheet', 2), ('g2_manifold', 1), ('string_coupling_crystal', 1)], [('exotic_matter', 1000)], [('m_brane', 1)],
       energy=65536, duration=800, research='m_theory')

    # ================================================================== TIME
    R = 'temporal_mechanics'
    mr('arrow_of_time_shard', 'chronoweaving', [('negentropy_cell', 1), ('chronon_capacitor', 1)], [('chronon_gas', 250)], [('arrow_of_time_shard', 2)], [],
       energy=8192, duration=400, research=R)
    mr('temporal_knot', 'chronoweaving', [('arrow_of_time_shard', 2), ('tachyon_crystal', 2), ('mobius_strip', 1)], [('chronon_gas', 500)], [('temporal_knot', 1)], [],
       energy=16384, duration=600, research=R)
    mr('novikov_crystal', 'chronoweaving', [('temporal_knot', 2), ('formal_proof_tree', 1), ('lorentz_transform_crystal', 2)], [('chronon_gas', 1000)], [('novikov_crystal', 1)],
       [('chronon_gas', 250)], energy=65536, duration=800, research=R)
    # rewired: closed timelike curves need self-consistency
    mr('closed_timelike_curve', 'causality_loop', [('novikov_crystal', 1), ('wormhole_throat', 1)], [('chronon_gas', 4000)],
       [('closed_timelike_curve', 1), ('paradox_residue', 1)], [], energy=524288, duration=1600, research='causality_violation')

    # ================================================================== minds: AGI needs formal logic
    mr('general_ai_core', 'neural_synthesis', [('narrow_ai_core', 4), ('superposition_crystal', 4), ('formal_proof_tree', 2)], [('synaptic_fluid', 2000)],
       [('general_ai_core', 1)], energy=65536, duration=1200, research='general_intelligence')

    # ================================================================== CONCEPTUAL SINGULARITIES (whole stacks, no timers)
    C = 'concept_compression'
    R = 'conceptual_singularities'
    E = 268435456
    mr('mathematics_singularity', C, [('natural_number_lattice', 64), ('complex_plane', 32), ('octonion_crystal', 16), ('riemann_zeta_crystal', 16), ('fractal_set', 32),
                                      ('integral_operator', 32), ('klein_bottle', 16), ('power_set', 32), ('formal_proof_tree', 64)],
       [('synaptic_fluid', 32000), ('dark_energy', 32000), ('bose_einstein_condensate', 32000)], [('mathematics_singularity', 1)], energy=E, duration=2400, research=R)
    mr('standard_model_singularity', C, [('up_down_quarks', 64), ('strange_charm_quarks', 64), ('bottom_top_quarks', 32), ('lepton_triplet', 64), ('gauge_boson_cluster', 32),
                                         ('higgs_boson_crystal', 16), ('standard_model_matrix', 8), ('superpartner_shard', 16), ('grand_unified_core', 4)],
       [('quark_gluon_plasma', 64000), ('strange_matter', 32000), ('neutrino_flux', 32000)], [('standard_model_singularity', 1)], energy=E, duration=2400, research=R)
    mr('spacetime_singularity', C, [('lorentz_transform_crystal', 64), ('minkowski_lattice', 32), ('metric_tensor', 32), ('riemann_curvature_tensor', 32), ('einstein_field_core', 16),
                                    ('kerr_metric_shard', 16), ('gravitational_wave_crystal', 32), ('wormhole_throat', 4), ('alcubierre_coil', 4)],
       [('exotic_matter', 64000), ('hawking_radiation', 32000), ('liquid_spacetime', 16000)], [('spacetime_singularity', 1)], energy=E, duration=2400, research=R)
    mr('stellar_singularity', C, [('protostar_core', 16), ('stellar_core', 8), ('red_giant_envelope', 32), ('white_dwarf_core', 32), ('stellar_iron_ingot', 64),
                                  ('supernova_remnant', 16), ('r_process_cluster', 32), ('magnetar_shard', 32), ('quark_star_core', 4)],
       [('stellar_plasma', 64000), ('nebular_gas', 64000), ('raw_star_matter', 16000)], [('stellar_singularity', 1)], energy=E, duration=2400, research=R)
    mr('dimensional_singularity', C, [('kaluza_klein_shard', 64), ('dilaton_crystal', 64), ('moduli_crystal', 64), ('d_brane_sheet', 32), ('compactified_circle', 32),
                                      ('calabi_yau_manifold', 16), ('g2_manifold', 16), ('m_brane', 16), ('m_theory_core', 4)],
       [('exotic_matter', 64000), ('inflaton_field', 32000), ('dimensional_residue', 32000)], [('dimensional_singularity', 1)], energy=E, duration=2400, research=R)
    mr('temporal_singularity', C, [('arrow_of_time_shard', 64), ('temporal_knot', 32), ('novikov_crystal', 16), ('chronon_capacitor', 64), ('tachyon_crystal', 64),
                                   ('closed_timelike_curve', 4), ('paradox_residue', 64), ('bootstrap_artifact', 8), ('pulsar_timing_crystal', 16)],
       [('chronon_gas', 64000), ('tachyon_flux', 64000), ('causal_solder', 16000)], [('temporal_singularity', 1)], energy=E, duration=2400, research=R)
    mr('quantum_singularity', C, [('qubit', 64), ('superposition_crystal', 64), ('entangled_photon_pair', 64), ('wave_packet', 64), ('spinor_lattice', 32),
                                  ('quantum_field_lattice', 16), ('feynman_diagram', 16), ('quantum_processor', 64), ('quaternion_crystal', 16)],
       [('bose_einstein_condensate', 64000), ('superfluid_helium', 64000), ('photoresist', 32000)], [('quantum_singularity', 1)], energy=E, duration=2400, research=R)
    mr('cognitive_singularity', C, [('neural_lattice', 64), ('general_ai_core', 16), ('superintelligence_core', 4), ('thought_matrix', 16), ('boltzmann_brain', 4),
                                    ('holographic_matrix', 32), ('bekenstein_crystal', 16), ('maxwell_demon_chip', 64), ('computronium_block', 16)],
       [('synaptic_fluid', 64000), ('psionic_ether', 32000), ('nanite_slurry', 32000)], [('cognitive_singularity', 1)], energy=E, duration=2400, research=R)
    mr('exotic_matter_singularity', C, [('exotic_matter_crystal', 64), ('negative_mass_ingot', 32), ('massless_alloy', 64), ('strange_matter_pellet', 64), ('antimatter_pellet', 64),
                                        ('dark_matter_lens', 32), ('magnetic_monopole', 32), ('void_lattice_shard', 64), ('cosmic_string_segment', 16)],
       [('dark_matter', 64000), ('exotic_matter', 64000), ('false_vacuum', 16000)], [('exotic_matter_singularity', 1)], energy=E, duration=2400, research=R)
    mr('theory_of_everything', C, [('standard_model_singularity', 1), ('dimensional_singularity', 1), ('spacetime_singularity', 1), ('quantum_singularity', 1),
                                   ('grand_unified_core', 4), ('m_theory_core', 4), ('einstein_field_core', 8), ('quantum_field_lattice', 8), ('feynman_diagram', 16)],
       [('exotic_matter', 64000), ('dark_energy', 64000), ('quark_gluon_plasma', 64000)], [('theory_of_everything', 1)], energy=E * 2, duration=4800, research='theory_of_everything')
    mr('omniscience_catalyst', C, [('theory_of_everything', 1), ('mathematics_singularity', 1), ('stellar_singularity', 1), ('temporal_singularity', 1),
                                   ('cognitive_singularity', 1), ('exotic_matter_singularity', 1), ('absolute_infinity', 1), ('omega_point', 1), ('transcendent_metal_ingot', 16)],
       [('primordial_law', 64000), ('transcendent_plasma', 16000), ('dark_energy', 64000)], [('omniscience_catalyst', 1)], energy=E * 4, duration=6000, research='omnilex')
    mr('omnium_ingot', C, [('omniscience_catalyst', 1), ('transcendent_metal_ingot', 32), ('hypernova_alloy_ingot', 64), ('cosmic_neutronium_ingot', 64),
                           ('spacetime_alloy_ingot', 64), ('chronosteel_ingot', 64), ('horizon_shard', 16), ('absolute_infinity', 4), ('axiom_fragment', 64)],
       [('primordial_law', 64000), ('hypernova_plasma', 32000), ('liquid_spacetime', 32000)], [('omnium_ingot', 1)], energy=E * 4, duration=6000, research='omnilex')
    # rewired: the Omnilex is built from Omnium
    mr('omnilex', 'omnilex_simulation', [('tree_simulacrum', 1), ('ocean_simulacrum', 1), ('omnium_ingot', 1), ('axiom_key', 1)],
       [('primordial_law', 16000), ('dark_energy', 16000)], [('omnilex', 1), ('axiom_key', 1)], energy=2000000000 // 4, duration=6000, research='omnilex')

    # ================================================================== access & blocks
    shaped('platonic_compass', [' A ', 'NEN', ' Z '], {'A': 'axiom_schema', 'N': 'natural_number_lattice', 'E': 'minecraft:ender_eye', 'Z': 'integer_ring'}, 'platonic_compass')
    shaped('conceptual_casing', ['FTF', 'TCT', 'FTF'], {'F': 'formal_proof_tree', 'T': 'transfinite_casing', 'C': 'category_lattice'}, 'conceptual_casing', 4)
    shaped('golden_ratio_glass', ['GGG', 'GPG', 'GGG'], {'G': 'containment_field_glass', 'P': 'transcendental_crystal'}, 'golden_ratio_glass', 8)
