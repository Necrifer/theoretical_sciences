"""Generates every JSON resource for Theoretical Sciences (assets + data)."""
import json, os, shutil, zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, '..', 'src/main/resources')
A = os.path.join(RES, 'assets/theoreticalsciences')
D = os.path.join(RES, 'data/theoreticalsciences')
NS = 'theoreticalsciences'
C = json.load(open(os.path.join(ROOT, 'content.json')))
M = json.load(open(os.path.join(ROOT, 'machines.json')))
ITEMS = {i['id']: i for i in C['items']}
BLOCKS = {b['id']: b for b in C['blocks']}
FLUIDS = {f['id']: f for f in C['fluids']}
MACH = {m['id']: m for m in M}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)


def q(x):
    return x if ':' in x else f'{NS}:{x}'


def title(s):
    return ' '.join(p.capitalize() for p in s.split('_'))


# clean generated dirs
for p in ['models', 'blockstates', 'lang', 'patchouli_books', 'multiblocks', 'particles']:
    shutil.rmtree(os.path.join(A, p), ignore_errors=True)
for p in ['recipe', 'loot_table', 'tags', 'ts_research', 'advancement', 'worldgen', 'neoforge', 'dimension', 'dimension_type', 'kubejs', 'patchouli_books']:
    shutil.rmtree(os.path.join(D, p), ignore_errors=True)
# 1.1.0 (audit): the mod's entries in vanilla's shared tags are generated too, so start them clean - a leftover entry for an
# item that no longer exists (the old lead-lined suit) makes the game drop the whole tag for every mod in the pack.
for p in ['minecraft', 'c']:
    shutil.rmtree(os.path.join(RES, 'data', p, 'tags'), ignore_errors=True)

import complexity
import concepts
import round3
import round4
import round5
import round7
import round8
import round10
import round11
import round12
import round13
import round14
import round15
import round16
import concepts_gen
LANG = {'itemGroup.theoreticalsciences': 'Theoretical Sciences'}

# =================================================================================== MACHINE TEXT
MACHINE_TEXT = {
    'quantum_entangler': ('Quantum Entangler', 'Spontaneous parametric down-conversion splits photons into entangled pairs, then binds them into qubits.'),
    'casimir_press': ('Casimir Vacuum Press', 'Presses matter between plates so close the vacuum itself pushes back.'),
    'bose_einstein_condenser': ('Bose-Einstein Condenser', 'Cools atoms to nanokelvin until they share a single quantum state.'),
    'metamaterial_weaver': ('Metamaterial Weaver', 'Lays down sub-wavelength lattices that bend light (and gravity) the wrong way.'),
    'tachyon_resonator': ('Tachyon Resonator', 'Resonates a crystal lattice until it emits particles that travel faster than light.'),
    'antimatter_penning_trap': ('Antimatter Penning Trap', 'Pair-produces antimatter from gamma photons and holds it in a magnetic bottle.'),
    'chronon_field_inverter': ('Chronon Field Inverter', 'Inverts the local arrow of time to harvest chronons.'),
    'topological_transmuter': ('Topological Phase Transmuter', 'Changes a material\'s topology without changing its chemistry.'),
    'zero_point_extractor': ('Zero-Point Energy Extractor', 'Generates power from vacuum fluctuations. Burns Casimir plates and exotic matter as catalysts.'),
    'string_resonance_loom': ('String Resonance Loom', 'Plucks cosmic strings into fundamental quantum strings and weaves them into membranes.'),
    'graviton_condenser': ('Graviton Condenser', 'Quantises gravity itself - gravitons, spin foam and the anchors of reality.'),
    'neural_synthesizer': ('Neural Synthesizer', 'Grows photonic neural networks and the artificial minds that run on them.'),
    'quark_gluon_collider': ('Quark-Gluon Collider', 'A ring accelerator that smashes heavy nuclei into quark-gluon plasma.'),
    'dark_matter_collector': ('Dark Matter Halo Collector', 'Gravitationally focuses the invisible halo surrounding every galaxy.'),
    'kugelblitz_chamber': ('Kugelblitz Chamber', 'Focuses so much light into one point that it collapses into a black hole.'),
    'penrose_ergosphere': ('Penrose Ergosphere Generator', 'Extracts the rotational energy of a spinning black hole via the Penrose process.'),
    'white_hole_emitter': ('White Hole Emitter', 'Runs a black hole backwards in time. Nothing can enter; everything comes out.'),
    'tesseract_assembler': ('Tesseract Assembler', 'Folds matter through a fourth spatial dimension.'),
    'multiverse_rift': ('Multiverse Rift Stabilizer', 'Holds open a tear into neighbouring universes and harvests what leaks through.'),
    'universe_forge': ('Universe Forge', 'Triggers false-vacuum decay and cosmic inflation to grow - and later harvest - a pocket universe.'),
    'matrioshka_brain': ('Matrioshka Brain', 'Nested shells of computronium around a star. It thinks; you gain Insight.'),
    'dyson_array': ('Dyson Array', 'Surrounds a captive star with a swarm, and eventually a sphere, of collectors.'),
    'maxwell_demon_sorter': ("Maxwell's Demon Sorter", 'A molecular gatekeeper that sorts fast particles from slow ones, defying the second law of thermodynamics.'),
    'nanite_assembler': ('Nanite Assembler', 'A molecular foundry that builds machines atom by atom - including machines that build themselves.'),
    'neutrino_condenser': ('Neutrino Condenser', 'Traps the ghost particles that pass through planets without noticing.'),
    'holographic_encoder': ('Holographic Encoder', 'Writes a volume of matter onto its own boundary surface, per the holographic principle.'),
    'higgs_field_modulator': ('Higgs Field Modulator', 'Tunes the Higgs field locally to strip mass from matter - or make it negative.'),
    'psionic_resonator': ('Psionic Resonator', 'Amplifies and crystallises thought itself.'),
    'annihilation_reactor': ('Matter-Antimatter Annihilation Reactor', 'Converts mass entirely into energy, E = mc^2, regulated by dilithium crystals.'),
    'neutron_star_forge': ('Neutron Star Forge', 'Recreates the crushing interior of a collapsed star: neutronium, metallic hydrogen and quark matter.'),
    'wormhole_projector': ('Einstein-Rosen Bridge Projector', 'Opens a traversable wormhole to distant stars and drags their matter through.'),
    'causality_loop_engine': ('Causality Loop Engine', 'Sends objects back along a closed timelike curve so they exist twice. The universe is not amused.'),
    'dark_energy_siphon': ('Dark Energy Siphon', 'Taps the energy of empty space that drives the expansion of the universe.'),
    'reality_compiler': ('Reality Compiler', 'If the universe is a simulation, this decompiles it.'),
    'lattice_growth_vat': ('Lattice Growth Vat', 'Grows flawless single-crystal boules atom by atom in a superfluid bath.'),
    'planck_cutter': ('Planck-Scale Cutter', 'Slices boules into wafers, and wafers into dies, along cuts one Planck length wide.'),
    'quantum_lithographer': ('Quantum Lithographer', 'Engraves logic onto wafers through a lens. The lens is a catalyst and is never consumed.'),
    'femto_fabricator': ('Femto Fabricator', 'Builds components smaller than a proton - and assembles them into processors.'),
    'femtoscale_assembly_line': ('Femtoscale Assembly Line', 'A long line of robotic arms and fabrication heads for the most complex assemblies: high-tier processors and late-game machine controllers.'),
    'stellar_plasma_crucible': ('Stellar Plasma Crucible', 'Melts and alloys matter in plasma as hot as a stellar core. Makes Chronosteel, Spacetime Alloy, Cosmic Neutronium and Exotic Solder.'),
    'fractionation_spire': ('Fractionation Spire', 'A tall distillation spire that separates Dimensional Residue and Warm Helium into useful fractions.'),
    'cantor_diagonalizer': ('Cantor Diagonalizer', 'Lists every number, then builds one that is on no list. Manufactures infinities - and then bigger ones.'),
    'axiom_distiller': ('Axiom Distiller', 'Boils reality down past its source code to the axioms underneath, and condenses them into fragments.'),
    'axiomatic_breach_engine': ('Axiomatic Breach Engine', 'Breaks every law of physics and mathematics at once. While it runs, step into its heart to fall out of reality.'),
    'omnilex_engine': ('Omnilex Engine', 'Simulates the World Tree and the Ocean of Primordial Law - and so controls them. Only works in Hyperdimensional Reality Space.'),
}

# =================================================================================== MULTIBLOCK SHAPES
# layers bottom->top, rows front->back (front row holds the controller), chars left->right.
# S spacetime casing, E event horizon casing, H hyperframe, G containment glass, K coil block, F field emitter,
# P casing OR interface port, Q computronium, R rift frame, X cosmogenic casing, T stellar frame, . air, ' ' anything
KEY_BASE = {
    'S': ['spacetime_casing'], 'E': ['event_horizon_casing'], 'H': ['hyperframe_casing'], 'G': ['containment_field_glass'],
    'K': ['superconducting_coil_block'], 'F': ['field_emitter'], 'P': ['spacetime_casing', 'interface_port'],
    'Q': ['computronium_block'], 'R': ['rift_frame_casing'], 'X': ['cosmogenic_casing'], 'T': ['stellar_frame_casing'],
    'B': ['computronium_block', 'interface_port'], 'Y': ['rift_frame_casing', 'interface_port'], 'Z': ['cosmogenic_casing', 'interface_port'],
    'W': ['stellar_frame_casing', 'interface_port'], 'U': ['hyperframe_casing', 'interface_port'], 'V': ['event_horizon_casing', 'interface_port'],
    'M': ['magnetic_confinement_casing'], 'N': ['magnetic_confinement_casing', 'interface_port'],
    'O': ['chronoplastic_casing'], 'J': ['chronoplastic_casing', 'interface_port'],
    'L': ['wormhole_frame_casing'], 'I': ['wormhole_frame_casing', 'interface_port'],
    'A': ['reality_lattice_casing'], 'D': ['reality_lattice_casing', 'interface_port'],
    't': ['transfinite_casing'], 'u': ['transfinite_casing', 'interface_port'],
    'o': ['omnilex_casing'], 'p': ['omnilex_casing', 'interface_port'], 'h': ['arbor_heartwood'],
    'a': ['precision_assembly_casing'], 'b': ['precision_assembly_casing', 'interface_port'],
    'c': ['plasma_crucible_casing'], 'd': ['plasma_crucible_casing', 'interface_port'], 'k': ['chronosteel_coil_block'],
    'f': ['fractionation_casing'], 'g': ['fractionation_casing', 'interface_port'],
}

KEY_BASE.update(complexity.KEY_ADD)
KEY_BASE.update({'0': ['conceptual_casing'], '9': ['conceptual_casing', 'interface_port']})
KEY_BASE.update(round3.KEY_ADD)
KEY_BASE.update(round15.KEY_ADD)
import round42
KEY_BASE.update(round42.KEY_ADD)
import round45
KEY_BASE.update(round45.KEY_ADD)
import round46
KEY_BASE.update(round46.KEY_ADD)
import round47
KEY_BASE.update(round47.KEY_ADD)
import round48
KEY_BASE.update(round48.KEY_ADD)
import round49
KEY_BASE.update(round49.KEY_ADD)
import round50
KEY_BASE.update(round50.KEY_ADD)
import round51
KEY_BASE.update(round51.KEY_ADD)
import round52
KEY_BASE.update(round52.KEY_ADD)
import round53
KEY_BASE.update(round53.KEY_ADD)
import round54
KEY_BASE.update(round54.KEY_ADD)
import round55
KEY_BASE.update(round55.KEY_ADD)
import round56
KEY_BASE.update(round56.KEY_ADD)
import round57
KEY_BASE.update(round57.KEY_ADD)
MACHINE_TEXT.update(complexity.MACHINE_TEXT)
MACHINE_TEXT.update(concepts.MACHINE_TEXT)
MACHINE_TEXT.update(round3.MACHINE_TEXT)
MACHINE_TEXT.update(round4.MACHINE_TEXT)
MACHINE_TEXT.update(round5.MACHINE_TEXT)
MACHINE_TEXT.update(round7.MACHINE_TEXT)
MACHINE_TEXT.update(round11.MACHINE_TEXT)
MACHINE_TEXT.update(round15.MACHINE_TEXT)
import round19
MACHINE_TEXT.update(round19.MACHINE_TEXT)
import round26
MACHINE_TEXT.update(round26.MACHINE_TEXT)
import round29
MACHINE_TEXT.update(round29.MACHINE_TEXT)
import round32_branches
MACHINE_TEXT.update(round32_branches.MACHINE_TEXT)
import round34_anomaly
MACHINE_TEXT.update(round34_anomaly.MACHINE_TEXT)
import round37
MACHINE_TEXT.update(round37.MACHINE_TEXT)
import round38
MACHINE_TEXT.update(round38.MACHINE_TEXT)
import round42
MACHINE_TEXT.update(round42.MACHINE_TEXT)
import round45
MACHINE_TEXT.update(round45.MACHINE_TEXT)
import round46
MACHINE_TEXT.update(round46.MACHINE_TEXT)
import round47
MACHINE_TEXT.update(round47.MACHINE_TEXT)
import round48
MACHINE_TEXT.update(round48.MACHINE_TEXT)
import round49
MACHINE_TEXT.update(round49.MACHINE_TEXT)
import round50
MACHINE_TEXT.update(round50.MACHINE_TEXT)
import round51
MACHINE_TEXT.update(round51.MACHINE_TEXT)
import round52
MACHINE_TEXT.update(round52.MACHINE_TEXT)
import round53
MACHINE_TEXT.update(round53.MACHINE_TEXT)
import round54
MACHINE_TEXT.update(round54.MACHINE_TEXT)
import round55
MACHINE_TEXT.update(round55.MACHINE_TEXT)
import round56
MACHINE_TEXT.update(round56.MACHINE_TEXT)
import round57
MACHINE_TEXT.update(round57.MACHINE_TEXT)
import round43
MACHINE_TEXT.update(round43.MACHINE_TEXT)

ring9_floor = ["SSSSPSSSS", "SKKKKKKKS", "SKSSSSSKS", "SKS...SKS", "PKS...SKP", "SKS...SKS", "SKSSSSSKS", "SKKKKKKKS", "SSSSPSSSS"]
SHAPES = {
    'quark_gluon_collider': dict(layers=[
        ring9_floor,
        ["SGGGCGGGS", "G.......G", "G.GGGGG.G", "G.G...G.G", "G.G...G.G", "G.G...G.G", "G.GGGGG.G", "G.......G", "SGGGGGGGS"],
        [r.replace('P', 'S') for r in ring9_floor]], center=[4, 1, 4]),
    'dark_matter_collector': dict(layers=[
        ["SSPSS", "SEEES", "PEKEP", "SEEES", "SSPSS"],
        ["S.C.S", ".....", ".....", ".....", "S...S"],
        ["S...S", ".....", ".....", ".....", "S...S"],
        ["SFFFS", "F...F", "F...F", "F...F", "SFFFS"]], center=[2, 2, 2]),
    'kugelblitz_chamber': dict(layers=[
        ["SSPSS", "SEEES", "PEFEP", "SEEES", "SSPSS"],
        ["SGGGS", "G...G", "G...G", "G...G", "SGGGS"],
        ["SGCGS", "G...G", "F...F", "G...G", "SGFGS"],
        ["SGGGS", "G...G", "G...G", "G...G", "SGGGS"],
        ["SSSSS", "SEEES", "SEFES", "SEEES", "SSSSS"]], center=[2, 2, 2]),
    'penrose_ergosphere': dict(layers=[
        ["SSSPSSS", "SEEEEES", "SEKKKES", "PEKEKEP", "SEKKKES", "SEEEEES", "SSSPSSS"],
        ["SGGCGGS", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "SGGGGGS"],
        ["SGGGGGS", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "SGGGGGS"],
        ["SSSSSSS", "SEEEEES", "SEKKKES", "SEKFKES", "SEKKKES", "SEEEEES", "SSSSSSS"]], center=[3, 1.5, 3]),
    'white_hole_emitter': dict(layers=[
        ["SSPSS", "SHHHS", "PHFHP", "SHHHS", "SSPSS"],
        ["SGCGS", "G...G", "G...G", "G...G", "SGGGS"],
        ["SGGGS", "G...G", "G...G", "G...G", "SGGGS"],
        ["SGGGS", "G...G", "G...G", "G...G", "SGGGS"],
        ["SSSSS", "SHHHS", "SHKHS", "SHHHS", "SSSSS"]], center=[2, 2, 2]),
    'tesseract_assembler': dict(layers=[
        ["HHUHH", "HGGGH", "UGKGU", "HGGGH", "HHUHH"],
        ["HGGGH", "G...G", "G...G", "G...G", "HGGGH"],
        ["HGCGH", "G...G", "G...G", "G...G", "HGGGH"],
        ["HGGGH", "G...G", "G...G", "G...G", "HGGGH"],
        ["HHHHH", "HGGGH", "HGKGH", "HGGGH", "HHHHH"]], center=[2, 2, 2]),
    'multiverse_rift': dict(layers=[
        ["RRRYRRR", "R.....R", "R.....R", "Y..F..Y", "R.....R", "R.....R", "RRRYRRR"],
        ["R..C..R", ".......", ".......", ".......", ".......", ".......", "R.....R"],
        ["R.....R", ".......", ".......", ".......", ".......", ".......", "R.....R"],
        ["R.....R", ".......", ".......", ".......", ".......", ".......", "R.....R"],
        ["RHHHHHR", "H.....H", "H.....H", "H..F..H", "H.....H", "H.....H", "RHHHHHR"]], center=[3, 2, 3]),
    'universe_forge': dict(layers=[
        ["XXXZXXX", "XKKKKKX", "XKXXXKX", "ZKXFXKZ", "XKXXXKX", "XKKKKKX", "XXXZXXX"],
        ["XGGCGGX", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "XGGGGGX"],
        ["XGGGGGX", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "XGGGGGX"],
        ["XGGGGGX", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "XGGGGGX"],
        ["XXXXXXX", "XHHHHHX", "XHXXXHX", "XHXFXHX", "XHXXXHX", "XHHHHHX", "XXXXXXX"]], center=[3, 2, 3]),
    'matrioshka_brain': dict(layers=[
        ["QQQBQQQ", "QTTTTTQ", "QTQQQTQ", "BTQFQTB", "QTQQQTQ", "QTTTTTQ", "QQQBQQQ"],
        ["QGGCGGQ", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "QGGGGGQ"],
        ["QGGGGGQ", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "QGGGGGQ"],
        ["QGGGGGQ", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "QGGGGGQ"],
        ["QGGGGGQ", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "QGGGGGQ"],
        ["QQQQQQQ", "QTTTTTQ", "QTQQQTQ", "QTQFQTQ", "QTQQQTQ", "QTTTTTQ", "QQQQQQQ"]], center=[3, 2.5, 3]),
    'dyson_array': dict(layers=[
        ["TTTTWTTTT", "T.......T", "T.......T", "T.......T", "W...F...W", "T.......T", "T.......T", "T.......T", "TTTTWTTTT"],
        ["T...C...T", ".........", ".........", ".........", ".........", ".........", ".........", ".........", "T.......T"],
        ["T.......T", ".........", ".........", ".........", ".........", ".........", ".........", ".........", "T.......T"],
        ["T.......T", ".........", ".........", ".........", ".........", ".........", ".........", ".........", "T.......T"],
        ["T.......T", ".........", ".........", ".........", ".........", ".........", ".........", ".........", "T.......T"],
        ["TTTTTTTTT", "T.......T", "T.......T", "T.......T", "T...F...T", "T.......T", "T.......T", "T.......T", "TTTTTTTTT"]], center=[4, 2.5, 4]),
    'annihilation_reactor': dict(layers=[
        ["MMNMM", "MKKKM", "NKFKN", "MKKKM", "MMNMM"],
        ["MGGGM", "G...G", "G...G", "G...G", "MGGGM"],
        ["MGCGM", "G...G", "G...G", "G...G", "MGGGM"],
        ["MGGGM", "G...G", "G...G", "G...G", "MGGGM"],
        ["MMMMM", "MKKKM", "MKFKM", "MKKKM", "MMMMM"]], center=[2, 2, 2]),
    'neutron_star_forge': dict(layers=[
        ["MMMNMMM", "MKKKKKM", "MKEEEKM", "NKEFEKN", "MKEEEKM", "MKKKKKM", "MMMNMMM"],
        ["MGGCGGM", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "MGGGGGM"],
        ["MGGGGGM", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "MGGGGGM"],
        ["MGGGGGM", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "MGGGGGM"],
        ["MMMMMMM", "MKKKKKM", "MKEEEKM", "MKEFEKM", "MKEEEKM", "MKKKKKM", "MMMMMMM"]], center=[3, 2, 3]),
    'wormhole_projector': dict(layers=[
        ["SSSCSSS", "SSSSSSS", "SSSSSSS"],
        ["       ", "  LLL  ", "       "],
        ["       ", " L...L ", "       "],
        ["       ", "L.....L", "       "],
        ["       ", "I.....I", "       "],
        ["       ", "L.....L", "       "],
        ["       ", " L...L ", "       "],
        ["       ", "  LLL  ", "       "]], center=[3, 4, 1]),
    'causality_loop_engine': dict(layers=[
        ["OOJOO", "OKKKO", "JKFKJ", "OKKKO", "OOJOO"],
        ["OGGGO", "G...G", "G...G", "G...G", "OGGGO"],
        ["OGCGO", "G...G", "G...G", "G...G", "OGGGO"],
        ["OGGGO", "G...G", "G...G", "G...G", "OGGGO"],
        ["OOOOO", "OKKKO", "OKFKO", "OKKKO", "OOOOO"]], center=[2, 2, 2]),
    'dark_energy_siphon': dict(layers=[
        ["EEVEE", "E...E", "V.F.V", "E...E", "EEVEE"],
        ["E.C.E", ".....", ".....", ".....", "E...E"],
        ["E...E", ".....", ".....", ".....", "E...E"],
        ["E...E", ".....", ".....", ".....", "E...E"],
        ["EFFFE", "F...F", "F...F", "F...F", "EFFFE"]], center=[2, 2, 2]),
    'reality_compiler': dict(layers=[
        ["AAADAAA", "AQQQQQA", "AQAAAQA", "DQAFAQD", "AQAAAQA", "AQQQQQA", "AAADAAA"],
        ["AGGCGGA", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "AGGGGGA"],
        ["AGGGGGA", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "AGGGGGA"],
        ["AGGGGGA", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "AGGGGGA"],
        ["AGGGGGA", "G.....G", "G.....G", "G.....G", "G.....G", "G.....G", "AGGGGGA"],
        ["AAAAAAA", "AQQQQQA", "AQAAAQA", "AQAFAQA", "AQAAAQA", "AQQQQQA", "AAAAAAA"]], center=[3, 2.5, 3]),
    # an open pavilion: walk in through the front and stand in the middle while the breach is open
    'axiomatic_breach_engine': dict(layers=[
        ["tttuttt", "tAAAAAt", "tAKKKAt", "uAKFKAu", "tAKKKAt", "tAAAAAt", "tttuttt"],
        ["t.....t", ".......", ".......", ".......", ".......", ".......", "t.....t"],
        ["t.....t", ".......", ".......", ".......", ".......", ".......", "t.....t"],
        ["t..C..t", ".......", ".......", ".......", ".......", ".......", "t.....t"],
        ["t.....t", ".......", ".......", ".......", ".......", ".......", "t.....t"],
        ["ttttttt", "tAAAAAt", "tA...At", "tA.F.At", "tA...At", "tAAAAAt", "ttttttt"]], center=[3, 2, 3]),
    'omnilex_engine': dict(layers=[
        ["oooopoooo", "ohhhhhhho", "ohtttttho", "ohtFFFtho", "phtFhFthp", "ohtFFFtho", "ohtttttho", "ohhhhhhho", "oooopoooo"],
        ["oGGGCGGGo"] + ["G.......G"] * 7 + ["oGGGGGGGo"],
        ["oGGGGGGGo"] + ["G.......G"] * 7 + ["oGGGGGGGo"],
        ["oGGGGGGGo"] + ["G.......G"] * 7 + ["oGGGGGGGo"],
        ["ooooooooo", "oAAAAAAAo"] + ["oA.....Ao"] * 5 + ["oAAAAAAAo", "ooooooooo"]], center=[4, 2.2, 4]),
}


# Distinctive silhouettes from tools/shapes.py replace the old box designs.
import shapes as _shapes
import round43_shapes as _r43s          # 1.3.6: the reworked late/endgame shapes win over every older registration
_shapes.BUILDERS.update(_r43s.REWORKED)
for _k, _v in _shapes.build_all().items():
    SHAPES[_k] = dict(layers=_v['layers'], center=_v['center'], repeat=_v.get('repeat'))


import effect_place as _effect_place


def mb_key(layers):
    used = set(''.join(''.join(l) for l in layers)) - set('C. ')
    return {k: [q(b) for b in KEY_BASE[k]] for k in sorted(used)}


for m in M:
    if not m['multiblock']:
        continue
    sh = SHAPES[m['mb_id']]
    for li, layer in enumerate(sh['layers']):
        assert len(set(len(r) for r in layer)) == 1, (m['id'], li)
    _used = set(''.join(''.join(l) for l in sh['layers']))
    # 1.3.8: the working animation is kept in view, and grows with the machine (tools/effect_place.py)
    _ec, _es = _effect_place.place(sh['layers'], sh['center'], m.get('effect', 'NONE'))
    w(os.path.join(A, 'multiblocks', m['mb_id'] + '.json'), {
        'controller_block': q(m['id']), 'controller': 'C', 'layers': sh['layers'], 'key': mb_key(sh['layers']), 'effect_center': _ec,
        **({'effect_scale': _es} if _es != 1.0 else {}),
        'tiers': {ch: {'family': fam, 'base': base} for ch, (fam, base) in complexity.TIERS.items() if ch in _used},
        **({'repeat': dict(zip(('from', 'to', 'max'), sh['repeat']))} if sh.get('repeat') else {}),
        **({'legacy': [_r43s.legacy(m['mb_id'])]} if _r43s.legacy(m['mb_id']) else {})})

# =================================================================================== BLOCKSTATES & MODELS
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}


def simple_block(bid, translucent=False):
    model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{bid}'}}
    if translucent:
        model['render_type'] = 'minecraft:translucent'
    w(os.path.join(A, 'models/block', bid + '.json'), model)
    w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
    w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})


for b in C['blocks']:
    if b['kind'] in ('deco_stairs', 'deco_slab', 'deco_wall', 'deco_pillar'):
        LANG[f'block.{NS}.{b["id"]}'] = b['name']
        continue
    simple_block(b['id'], b['kind'] in ('glass', 'foam_bubble', 'return_rift', 'cloud'))
    LANG[f'block.{NS}.{b["id"]}'] = b['name']
    if b['kind'] in ('casing', 'glass'):
        # formed variant: the structure "transforms" into seamless energised panels once complete
        fm = {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{b["id"]}_formed'}}
        if b['kind'] == 'glass':
            fm['render_type'] = 'minecraft:translucent'
        w(os.path.join(A, 'models/block', b['id'] + '_formed.json'), fm)
        w(os.path.join(A, 'blockstates', b['id'] + '.json'), {'variants': {'formed=false': {'model': f'{NS}:block/{b["id"]}'},
                                                                         'formed=true': {'model': f'{NS}:block/{b["id"]}_formed'}}})

# ---- decorative shapes: stairs / slabs / walls / pillars, using vanilla's block model templates
import zipfile as _zz
with _zz.ZipFile(__import__("mcpaths").CLIENT_JAR) as _z:
    _STAIRS_BS = _z.read('assets/minecraft/blockstates/stone_brick_stairs.json').decode()
    _WALL_BS = _z.read('assets/minecraft/blockstates/stone_brick_wall.json').decode()
for b in C['blocks']:
    bid, k = b['id'], b['kind']
    tex = f'{NS}:block/{b.get("base") or bid}'
    if k == 'deco_pillar':
        w(os.path.join(A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/cube_column', 'textures': {'end': tex + '_top', 'side': tex + '_side'}})
        w(os.path.join(A, 'models/block', bid + '_horizontal.json'), {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'end': tex + '_top', 'side': tex + '_side'}})
        w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'axis=y': {'model': f'{NS}:block/{bid}'},
                                                                     'axis=z': {'model': f'{NS}:block/{bid}_horizontal', 'x': 90},
                                                                     'axis=x': {'model': f'{NS}:block/{bid}_horizontal', 'x': 90, 'y': 90}}})
        w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    elif k == 'deco_stairs':
        for suffix, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
            w(os.path.join(A, 'models/block', bid + suffix + '.json'), {'parent': f'minecraft:block/{parent}', 'textures': {'bottom': tex, 'top': tex, 'side': tex}})
        w(os.path.join(A, 'blockstates', bid + '.json'), json.loads(_STAIRS_BS.replace('minecraft:block/stone_brick_stairs', f'{NS}:block/{bid}')))
        w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    elif k == 'deco_slab':
        w(os.path.join(A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/slab', 'textures': {'bottom': tex, 'top': tex, 'side': tex}})
        w(os.path.join(A, 'models/block', bid + '_top.json'), {'parent': 'minecraft:block/slab_top', 'textures': {'bottom': tex, 'top': tex, 'side': tex}})
        w(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'type=bottom': {'model': f'{NS}:block/{bid}'}, 'type=top': {'model': f'{NS}:block/{bid}_top'},
                                                                     'type=double': {'model': f'{NS}:block/{b["base"]}'}}})
        w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}'})
    elif k == 'deco_wall':
        for suffix, parent in (('_post', 'template_wall_post'), ('_side', 'template_wall_side'), ('_side_tall', 'template_wall_side_tall'), ('_inventory', 'wall_inventory')):
            w(os.path.join(A, 'models/block', bid + suffix + '.json'), {'parent': f'minecraft:block/{parent}', 'textures': {'wall': tex}})
        w(os.path.join(A, 'blockstates', bid + '.json'), json.loads(_WALL_BS.replace('minecraft:block/stone_brick_wall', f'{NS}:block/{bid}')))
        w(os.path.join(A, 'models/item', bid + '.json'), {'parent': f'{NS}:block/{bid}_inventory'})

# the World Tree's heartwood has four phases, each with its own texture
w(os.path.join(A, 'blockstates', 'arbor_heartwood.json'), {'variants': {f'phase={i}': {'model': f'{NS}:block/arbor_heartwood_{i}'} for i in range(4)}})
for i in range(4):
    w(os.path.join(A, 'models/block', f'arbor_heartwood_{i}.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/arbor_heartwood_{i}'}})
w(os.path.join(A, 'models/item', 'arbor_heartwood.json'), {'parent': f'{NS}:block/arbor_heartwood_0'})
w(os.path.join(A, 'models/block', 'bubble_universe.json'), {
    'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
    'textures': {'particle': f'{NS}:block/bubble_universe', 'all': f'{NS}:block/bubble_universe'},
    'elements': [{'from': [3, 3, 3], 'to': [13, 13, 13], 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}}]})

for m in M:
    mid = m['id']
    for state in ('off', 'on'):
        w(os.path.join(A, 'models/block/machine', f'{mid}_{state}.json'), {
            'parent': 'minecraft:block/orientable',
            'textures': {'top': f'{NS}:block/machine/{mid}_top', 'front': f'{NS}:block/machine/{mid}_front_{state}',
                         'side': f'{NS}:block/machine/{mid}_side'}})
    variants = {}
    for f, r in ROT.items():
        for act in ('false', 'true'):
            v = {'model': f'{NS}:block/machine/{mid}_{"on" if act == "true" else "off"}'}
            if r:
                v['y'] = r
            variants[f'active={act},facing={f}'] = v
    w(os.path.join(A, 'blockstates', mid + '.json'), {'variants': variants})
    w(os.path.join(A, 'models/item', mid + '.json'), {'parent': f'{NS}:block/machine/{mid}_on'})
    name, lore = MACHINE_TEXT[mid]
    LANG[f'block.{NS}.{mid}'] = name
    LANG[f'block.{NS}.{mid}.lore'] = lore

# interface port
for linked in ('false', 'true'):
    tex = 'interface_port_linked' if linked == 'true' else 'interface_port'
    w(os.path.join(A, 'models/block', tex + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{tex}'}})
w(os.path.join(A, 'blockstates/interface_port.json'), {'variants': {'linked=false': {'model': f'{NS}:block/interface_port'},
                                                                     'linked=true': {'model': f'{NS}:block/interface_port_linked'}}})
w(os.path.join(A, 'models/item/interface_port.json'), {'parent': f'{NS}:block/interface_port_linked'})
LANG[f'block.{NS}.interface_port'] = 'Interface Port'

# research terminal
w(os.path.join(A, 'models/block/research_terminal.json'), {
    'parent': 'minecraft:block/block',
    'textures': {'particle': f'{NS}:block/research_terminal_side', 'side': f'{NS}:block/research_terminal_side', 'top': f'{NS}:block/research_terminal_top'},
    'elements': [
        {'from': [1, 0, 1], 'to': [15, 10, 15], 'faces': {d: {'texture': '#side'} for d in ('north', 'south', 'east', 'west', 'down')} | {'up': {'texture': '#side'}}},
        {'from': [3, 10, 3], 'to': [13, 14, 13], 'faces': {d: {'texture': '#side'} for d in ('north', 'south', 'east', 'west')} | {'up': {'texture': '#top'}}},
    ]})
w(os.path.join(A, 'blockstates/research_terminal.json'), {'variants': {f'facing={f}': ({'model': f'{NS}:block/research_terminal', 'y': r} if r else {'model': f'{NS}:block/research_terminal'}) for f, r in ROT.items()}})
w(os.path.join(A, 'models/item/research_terminal.json'), {'parent': f'{NS}:block/research_terminal'})
LANG[f'block.{NS}.research_terminal'] = 'Research Terminal'

# items
BUILTIN_DISPLAY = {
    'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 2], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 2], 'scale': [0.5, 0.5, 0.5]},
    'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [1, 3, 0], 'scale': [0.6, 0.6, 0.6]},
    'firstperson_lefthand': {'rotation': [0, -20, 0], 'translation': [1, 3, 0], 'scale': [0.6, 0.6, 0.6]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 10, 0], 'scale': [0.8, 0.8, 0.8]},
}
for it in C['items']:
    iid = it['id']
    if it.get('special') in ('singularity', 'whitehole', 'tesseract', 'universe', 'star'):
        w(os.path.join(A, 'models/item', iid + '.json'), {'parent': 'minecraft:builtin/entity', 'gui_light': 'front',
                                                         'textures': {'particle': f'{NS}:item/{iid}'}, 'display': BUILTIN_DISPLAY})
    else:
        parent = 'minecraft:item/handheld' if it.get('special') in ('analyzer', 'shifter') else 'minecraft:item/generated'
        w(os.path.join(A, 'models/item', iid + '.json'), {'parent': parent, 'textures': {'layer0': f'{NS}:item/{iid}'}})
    LANG[f'item.{NS}.{iid}'] = it['name']
    LANG[f'item.{NS}.{iid}.lore'] = it['lore']

# fluids
for fl in C['fluids']:
    fid = fl['id']
    w(os.path.join(A, 'models/item', fid + '_cell.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/{fid}_cell'}})
    LANG[f'item.{NS}.{fid}_cell'] = fl['name'] + (' Gas Cell' if fl['gas'] else ' Containment Cell')
    LANG[f'fluid_type.{NS}.{fid}'] = fl['name']
    if not fl['gas']:
        w(os.path.join(A, 'models/block', fid + '.json'), {'textures': {'particle': f'{NS}:block/fluid/{fid}_still'}})
        w(os.path.join(A, 'blockstates', fid + '.json'), {'variants': {'': {'model': f'{NS}:block/{fid}'}}})
        LANG[f'block.{NS}.{fid}'] = fl['name']

# particles
for p in ('tachyon', 'quantum_foam', 'hawking', 'infall', 'dark_mote', 'photon', 'phase_mote', 'arc_spark', 'flash', 'glyph', 'void_ink',
          'shockwave', 'burst', 'vapour', 'fault', 'resolve', 'plasma', 'glow'):
    w(os.path.join(A, 'particles', p + '.json'), {'textures': [f'{NS}:{p}_{i}' for i in range(8)]})

# =================================================================================== LANG (misc)
LANG.update({
    'status.theoreticalsciences.idle': 'Idle', 'status.theoreticalsciences.running': 'Running', 'status.theoreticalsciences.generating': 'Generating',
    'status.theoreticalsciences.no_power': 'No Power', 'status.theoreticalsciences.output_full': 'Output Full',
    'status.theoreticalsciences.needs_research': 'Needs Research', 'status.theoreticalsciences.incomplete': 'Structure Incomplete',
    'status.theoreticalsciences.blocked': 'Blocked',
    'status.theoreticalsciences.fault': 'Containment Fault',
    'gui.theoreticalsciences.tuning': 'Field Tuning',
    'gui.theoreticalsciences.efficiency': 'Efficiency: %s%%',
    'gui.theoreticalsciences.integrity': 'Containment',
    'gui.theoreticalsciences.overclock': 'Overclock %s/%s',
    'gui.theoreticalsciences.parallel': 'Parallel %s/%s',
    'gui.theoreticalsciences.fault': 'CONTAINMENT FAULT',
    'gui.theoreticalsciences.fault_hint': 'Use a Field Calibrator',
    'gui.theoreticalsciences.auto_calibrating': 'Autonomous calibrator online',
    'message.theoreticalsciences.containment_failure': 'Containment failure in the %s at %s, %s, %s! Recalibrate it with a Field Calibrator.',
    'message.theoreticalsciences.calibrated': 'Containment restored. %s: ideal %s (yours: %s). The ideal is marked on the tuning panel for 30 s.',
    'message.theoreticalsciences.calibrated_fault': 'Fault cleared and containment restored. %s: ideal %s (yours: %s).',
    'message.theoreticalsciences.calibrator_nothing': 'Use this on a multiblock controller.',
    'message.theoreticalsciences.autocalibrator_installed': 'Autonomous calibrator installed. This machine now tunes and repairs itself.',
    'message.theoreticalsciences.autocalibrator_present': 'An autonomous calibrator is already installed.',
    'message.theoreticalsciences.assembler_not_controller': 'Use this on a multiblock controller.',
    'message.theoreticalsciences.assembler_complete': 'The structure is already complete.',
    'message.theoreticalsciences.assembler_mode_disassemble': 'Structure Assembler: DISASSEMBLE mode (sneak-use in the air to switch back)',
    'message.theoreticalsciences.assembler_mode_build': 'Structure Assembler: build mode',
    'tooltip.theoreticalsciences.assembler_mode_disassemble': 'Mode: disassemble (sneak-use in the air to switch)',
    'tooltip.theoreticalsciences.assembler_mode_build': 'Mode: build (sneak-use in the air to switch)',
    'message.theoreticalsciences.assembler_disassemble_preview': 'Taking this apart would return %s block(s):',
    'message.theoreticalsciences.assembler_disassemble_confirm': 'Use again within 5 seconds to take apart %s block(s).',
    'message.theoreticalsciences.assembler_disassembled': 'Took apart %s block(s) and put them in your inventory.',
    'message.theoreticalsciences.assembler_disassemble_kept': '%s block(s) could not be removed here (protected or unbreakable).',
    'message.theoreticalsciences.assembler_disassemble_dropped': 'Your inventory was full: the rest is on the ground at your feet.',
    'message.theoreticalsciences.assembler_placed': 'Assembled %s block(s).',
    'message.theoreticalsciences.assembler_blocked': '%s position(s) are obstructed (marked) - clear them yourself; the assembler never breaks blocks.',
    'message.theoreticalsciences.assembler_missing': 'Not enough materials for the rest (sparks mark the gaps):',
    'message.theoreticalsciences.assembler_needs': 'Still needed (sparks mark the gaps):',
    'tuning.theoreticalsciences.beam_focus': 'Beam Focus', 'tuning.theoreticalsciences.halo_lensing_angle': 'Halo Lensing Angle',
    'tuning.theoreticalsciences.photon_convergence': 'Photon Convergence', 'tuning.theoreticalsciences.ergosphere_phase': 'Ergosphere Phase',
    'tuning.theoreticalsciences.time_reversal_symmetry': 'Time-Reversal Symmetry', 'tuning.theoreticalsciences.w_axis_alignment': 'W-Axis Alignment',
    'tuning.theoreticalsciences.brane_separation': 'Brane Separation', 'tuning.theoreticalsciences.inflation_rate': 'Inflation Rate',
    'tuning.theoreticalsciences.shell_synchrony': 'Shell Synchrony', 'tuning.theoreticalsciences.orbital_phasing': 'Orbital Phasing',
    'tuning.theoreticalsciences.dilithium_alignment': 'Dilithium Matrix Alignment', 'tuning.theoreticalsciences.compression_ratio': 'Compression Ratio',
    'tuning.theoreticalsciences.throat_radius': 'Throat Radius', 'tuning.theoreticalsciences.loop_phase': 'Loop Phase',
    'tuning.theoreticalsciences.vacuum_polarity': 'Vacuum Polarity', 'tuning.theoreticalsciences.instruction_pipeline': 'Instruction Pipeline',
    'tuning.theoreticalsciences.axiom_interference': 'Axiom Interference', 'tuning.theoreticalsciences.law_coherence': 'Law Coherence',
    'tuning.theoreticalsciences.arm_synchronisation': 'Arm Synchronisation', 'tuning.theoreticalsciences.plasma_temperature': 'Plasma Temperature',
    'tuning.theoreticalsciences.reflux_ratio': 'Reflux Ratio',
    'tooltip.theoreticalsciences.multiblock': 'Multiblock controller - sneak-right-click to diagnose the structure',
    'tooltip.theoreticalsciences.generator': 'Generator - buffer %s FE',
    'tooltip.theoreticalsciences.consumer': 'Consumes FE - buffer %s FE',
    'tooltip.theoreticalsciences.gas_cell': 'Contained gas. Venting it releases it harmlessly.',
    'tooltip.theoreticalsciences.fluid_cell': 'Contained exotic fluid.',
    'tooltip.theoreticalsciences.analyzer_usage': 'Use on blocks, machines or creatures. Sneak-use to observe this dimension.',
    'message.theoreticalsciences.structure_complete': 'Structure complete. %s interface port(s) linked.',
    'message.theoreticalsciences.structure_missing': 'Structure incomplete: %s block(s) wrong or missing (first few shown, sparks mark them):',
    'message.theoreticalsciences.air': 'Air',
    'message.theoreticalsciences.research_unlocked': 'Research complete: %s',
    'message.theoreticalsciences.research_unknown': 'Unknown research.',
    'message.theoreticalsciences.research_missing_prereq': 'Prerequisite research missing.',
    'message.theoreticalsciences.research_not_enough_insight': 'Not enough Insight. Use the Spectral Analyzer on new phenomena.',
    'message.theoreticalsciences.research_missing_items': 'Missing the required materials.',
    'message.theoreticalsciences.analyzer_new': 'New observation: %s (+%s Insight)',
    'message.theoreticalsciences.analyzer_known': 'Already catalogued: %s',
    'message.theoreticalsciences.analyzer_idle': 'The machine is idle - nothing to observe.',
    'message.theoreticalsciences.analyzer_nothing': 'Nothing of scientific interest here.',
    'message.theoreticalsciences.shifter_locked': 'You need to research Brane Cosmology first.',
    'message.theoreticalsciences.key_locked': 'You need to research The Axiomatic Breach first.',
    'message.theoreticalsciences.breach_arrival': 'Every law you knew has stopped applying. Above: the World Tree. Below: the Ocean of Primordial Law.',
    'tooltip.theoreticalsciences.omnilex_dimension': 'Only operates in Hyperdimensional Reality Space',
    'gui.theoreticalsciences.research': 'Research Matrix',
    'gui.theoreticalsciences.insight': 'Insight: %s   Observations: %s',
    'gui.theoreticalsciences.no_research': 'No research loaded.',
    'gui.theoreticalsciences.researched': 'Researched',
    'gui.theoreticalsciences.cost_insight': 'Costs %s Insight',
    'gui.theoreticalsciences.requires': 'Requires: %s',
    'gui.theoreticalsciences.click_to_research': 'Click to research',
    'gui.theoreticalsciences.generating': 'Producing %s FE/t',
    'gui.theoreticalsciences.empty': 'Empty',
    'gui.theoreticalsciences.dyson': 'Swarm %s/%s  Shell %s/%s',
    'jei.theoreticalsciences.research': 'Research: %s',
    'jei.theoreticalsciences.not_consumed': 'Catalyst - not consumed',
    'jei.theoreticalsciences.chance': 'Chance: %s%%',
    'jei.theoreticalsciences.multiblock': 'Multiblock',
    'jei.theoreticalsciences.multiblocks': 'Theoretical Sciences Structures',
    'jei.theoreticalsciences.multiblock_hint': 'Interface Ports may replace marked casings',
})


# =================================================================================== RESEARCH
RESEARCH = [
    # id, title, description, icon, x, y, insight, requires, cost
    ('quantum_foundations', 'Quantum Foundations', 'Everything is a wave until it is measured. Catalogue phenomena with the Spectral Analyzer to begin.', 'qubit', 0, 0, 5, [], []),
    ('casimir_effect', 'The Casimir Effect', 'Two plates in a vacuum attract: empty space is full of energy. Unlocks the Casimir Press and Zero-Point Extractor.', 'casimir_plate', 1, -1, 5, ['quantum_foundations'], [('cherenkov_quartz', 4)]),
    ('quantum_entanglement', 'Quantum Entanglement', 'Spooky action at a distance, put to work. Unlocks qubits and the Quantum Entangler.', 'entangled_photon_pair', 1, 1, 5, ['quantum_foundations'], [('cherenkov_quartz', 4)]),
    ('cryogenics', 'Cryogenics', 'Near absolute zero, matter forgets it is many particles. Superfluids and Bose-Einstein condensates.', 'superfluid_helium_cell', 2, -1, 10, ['casimir_effect'], [('neutronium_plate', 2)]),
    ('metamaterials', 'Metamaterials', 'Engineered structures with a negative refractive index.', 'metamaterial_sheet', 3, 0, 15, ['cryogenics', 'quantum_entanglement'], [('quantum_dot', 4)]),
    ('topology', 'Topological Matter', 'Phases of matter protected by topology rather than symmetry.', 'topological_insulator', 4, 1, 20, ['metamaterials'], [('metamaterial_sheet', 1)]),
    ('tachyonics', 'Tachyonics', 'Hypothetical faster-than-light particles. Their imaginary mass makes them slow down as they gain energy.', 'tachyon_crystal', 4, -1, 20, ['metamaterials'], [('cherenkov_quartz', 8)]),
    ('antimatter', 'Antimatter Containment', 'Pair production and Penning traps.', 'antimatter_pellet', 3, 2, 20, ['quantum_entanglement', 'cryogenics'], [('superconducting_coil', 2)]),
    ('chronodynamics', 'Chronodynamics', 'If time is quantised, its quanta can be stored.', 'chronon_capacitor', 5, -1, 30, ['tachyonics'], [('tachyon_crystal', 1)]),
    ('particle_physics', 'High-Energy Particle Physics', 'Colliders, quark-gluon plasma, strange matter and magnetic monopoles.', 'strange_matter_pellet', 5, 2, 30, ['antimatter'], [('antimatter_pellet', 1)]),
    ('dark_sector', 'The Dark Sector', 'Most of the universe\'s mass does not interact with light. Unlocks the Dark Matter Halo Collector.', 'dark_matter_lens', 6, 1, 40, ['particle_physics', 'topology'], [('void_lattice_shard', 4)]),
    ('singularity_physics', 'Singularity Physics', 'Event horizons, Hawking radiation and the Kugelblitz: a black hole made from light alone.', 'micro_singularity', 7, 1, 50, ['dark_sector'], [('exotic_matter_crystal', 2)]),
    ('penrose_process', 'The Penrose Process', 'Energy can be stolen from a rotating black hole\'s ergosphere.', 'penrose_spindle', 8, 2, 60, ['singularity_physics'], [('magnetic_monopole', 1)]),
    ('white_holes', 'White Holes', 'The time-reverse of a black hole: a horizon nothing can enter.', 'white_hole_fragment', 8, 0, 70, ['singularity_physics', 'chronodynamics'], [('chronon_capacitor', 1)]),
    ('stellar_engineering', 'Stellar Engineering', 'Ignite a captive star from white-hole ejecta.', 'stellar_core', 9, 1, 80, ['white_holes'], [('hawking_emitter', 1)]),
    ('dyson_swarm', 'Dyson Swarm', 'Millions of statites harvesting starlight.', 'dyson_swarm_satellite', 10, 1, 90, ['stellar_engineering'], [('solar_statite', 4)]),
    ('dyson_sphere', 'Dyson Sphere', 'Close the swarm into a shell and capture a star\'s entire output.', 'dyson_shell_segment', 11, 1, 120, ['dyson_swarm'], [('higgs_field_capacitor', 2)]),
    ('hyperdimensional_geometry', 'Hyperdimensional Geometry', 'Tesseracts and Calabi-Yau manifolds: geometry beyond three dimensions.', 'tesseract_core', 9, -1, 80, ['white_holes', 'topology'], [('superposition_crystal', 4)]),
    ('brane_cosmology', 'Brane Cosmology', 'Our universe is a membrane floating in the Bulk. The Brane Shifter can take you there.', 'brane_shifter', 10, -2, 100, ['hyperdimensional_geometry'], [('void_lattice_shard', 16)]),
    ('string_theory', 'String Theory', 'Every particle is a vibrating string. Unlocks the String Resonance Loom.', 'quantum_string', 10, -1, 100, ['hyperdimensional_geometry'], [('cosmic_string_segment', 1)]),
    ('quantum_gravity', 'Quantum Gravity', 'Gravitons and spin foam: spacetime made of quanta. Unlocks the Graviton Condenser.', 'graviton_crystal', 11, -1, 120, ['string_theory'], [('planck_shard', 1)]),
    ('m_theory', 'M-Theory', 'Strings are the edges of membranes in eleven dimensions.', 'm_brane', 11, -2, 120, ['string_theory', 'brane_cosmology'], [('quantum_string', 4)]),
    ('multiverse', 'The Multiverse', 'Other branes, other constants, other universes - and a way to reach them.', 'multiverse_shard', 12, -1, 150, ['quantum_gravity', 'm_theory'], [('graviton_crystal', 4)]),
    ('cosmogenesis', 'Cosmogenesis', 'False vacuum decay plus inflation equals a new universe. Then harvest it.', 'pocket_universe', 13, -1, 200, ['multiverse'], [('multiverse_shard', 8)]),
    ('artificial_intelligence', 'Artificial Intelligence', 'Photonic neural networks and narrow AI. Unlocks the Neural Synthesizer.', 'narrow_ai_core', 2, 3, 25, ['quantum_entanglement', 'cryogenics'], [('quantum_dot', 8)]),
    ('general_intelligence', 'Artificial General Intelligence', 'A mind that can learn anything.', 'general_ai_core', 5, 3, 60, ['artificial_intelligence', 'topology'], [('neural_lattice', 2)]),
    ('superintelligence', 'Superintelligence', 'A mind that improves itself. Computronium becomes possible.', 'superintelligence_core', 10, 3, 140, ['general_intelligence', 'hyperdimensional_geometry'], [('general_ai_core', 1)]),
    ('matrioshka_brain', 'Matrioshka Brain', 'Wrap a star in nested shells of computronium and let it think.', 'computronium_block', 12, 2, 200, ['superintelligence', 'dyson_sphere'], [('superintelligence_core', 1)]),
    ('thermodynamics_of_information', 'Thermodynamics of Information', "Information is physical. A demon that knows which molecules are fast can sort them and reverse entropy.", 'maxwell_demon_chip', 2, 4, 20, ['cryogenics'], [('qubit', 2)]),
    ('nanotechnology', 'Molecular Nanotechnology', 'Assemblers that build anything atom by atom - including copies of themselves.', 'nanite_swarm', 4, 4, 35, ['thermodynamics_of_information', 'metamaterials'], [('maxwell_demon_chip', 1)]),
    ('neutrino_physics', 'Neutrino Physics', 'Ghost particles, trillions of which pass through you every second.', 'sterile_neutrino', 6, 4, 35, ['particle_physics'], [('superconducting_coil', 4)]),
    ('mass_manipulation', 'Mass Manipulation', 'Massless matter, negative mass and the inertial dampers every starship needs.', 'negative_mass_ingot', 7, 3, 70, ['particle_physics', 'nanotechnology'], [('higgs_field_capacitor', 1)]),
    ('psionics', 'Psionics', 'Thought, crystallised. A science that should not exist.', 'psionic_crystal', 6, 5, 60, ['general_intelligence'], [('neural_lattice', 1)]),
    ('holographic_principle', 'The Holographic Principle', 'Everything inside a volume is encoded on its boundary - black holes included.', 'holographic_matrix', 8, 4, 80, ['thermodynamics_of_information', 'singularity_physics'], [('hawking_emitter', 1)]),
    ('annihilation_power', 'Annihilation Power', 'Matter plus antimatter equals pure energy. Dilithium keeps it from equalling a crater.', 'annihilation_core', 4, 2, 40, ['antimatter'], [('antimatter_pellet', 2)]),
    ('compact_stars', 'Compact Stars', 'Neutron stars, magnetars, pulsars and quark stars - the densest objects short of a singularity.', 'quark_star_core', 9, 2, 90, ['penrose_process', 'neutrino_physics'], [('neutronium_block', 4)]),
    ('traversable_wormholes', 'Traversable Wormholes', 'Black and white holes joined at the throat, propped open with negative mass.', 'wormhole_throat', 10, 0, 120, ['white_holes', 'mass_manipulation'], [('negative_mass_ingot', 2)]),
    ('causality_violation', 'Causality Violation', 'Closed timelike curves: send something back to before it was made, and now there are two.', 'closed_timelike_curve', 11, 0, 160, ['traversable_wormholes', 'chronodynamics'], [('chronon_capacitor', 4)]),
    ('dark_energy', 'Dark Energy', 'The energy of empty space itself, pushing galaxies apart.', 'cosmological_constant', 12, 0, 150, ['traversable_wormholes', 'dark_sector'], [('dark_matter_lens', 2)]),
    ('precision_lithography', 'Precision Lithography', 'Grow crystal boules, slice them into wafers, engrave them through lenses and cut them into dies. Unlocks the Lattice Growth Vat, Planck-Scale Cutter and Quantum Lithographer.', 'photonic_logic_wafer', 2, -3, 8, ['casimir_effect'], [('cherenkov_quartz', 8)]),
    ('femtoengineering', 'Femtoengineering', 'Components smaller than a proton, soldered with superconductors. Unlocks the Femto Fabricator and the Qubit Processor.', 'femto_transistor', 3, -3, 15, ['precision_lithography', 'quantum_entanglement'], [('photonic_processor', 2)]),
    ('plasma_metallurgy', 'Plasma Metallurgy', 'Alloys forged in stellar plasma: Chronosteel and Exotic Solder. Unlocks the Stellar Plasma Crucible.', 'chronosteel_ingot', 6, -3, 35, ['particle_physics', 'chronodynamics'], [('quantum_processor', 2)]),
    ('assembly_lines', 'Assembly Lines', 'Attoscale components and the Femtoscale Assembly Line - the only way to build high-tier processors.', 'topological_processor', 7, -3, 45, ['plasma_metallurgy', 'topology', 'femtoengineering'], [('chronosteel_ingot', 4)]),
    ('fractional_distillation', 'Fractional Distillation', 'High-tier processes leave Dimensional Residue. Distil it back into useful matter in the Fractionation Spire.', 'dimensional_residue_cell', 10, -3, 90, ['hyperdimensional_geometry', 'assembly_lines'], [('topological_processor', 2)]),
    ('transfinite_mathematics', 'Transfinite Mathematics', 'Cantor proved that some infinities are bigger than others. Build them. Unlocks the Cantor Diagonalizer.', 'aleph_null_shard', 15, -1, 350, ['simulation_hypothesis'], [('source_code_fragment', 2)]),
    ('incompleteness', 'Incompleteness', 'Any system strong enough to describe arithmetic contains truths it can never prove - reality included.', 'godel_sentence', 16, -2, 400, ['transfinite_mathematics'], [('aleph_null_shard', 2)]),
    ('paradox_engineering', 'Paradox Engineering', 'Russell sets and Banach-Tarski spheres: building with contradictions.', 'russell_paradox_set', 16, 0, 400, ['transfinite_mathematics', 'causality_violation'], [('paradox_residue', 8)]),
    ('axiom_theory', 'Axiom Theory', 'Beneath the source code of reality lie its axioms - and axioms can be distilled, edited and broken. Unlocks the Axiom Distiller.', 'axiom_fragment', 17, -1, 450, ['incompleteness', 'paradox_engineering'], [('godel_sentence', 1)]),
    ('reality_breach', 'The Axiomatic Breach', 'Break every law of physics and mathematics at once, and step through the hole. Unlocks the Axiomatic Breach Engine and the Axiom Key.', 'axiom_key', 18, -1, 600, ['axiom_theory'], [('absolute_infinity', 1)]),
    ('hyperdimensional_reality', 'Hyperdimensional Reality', 'The space outside every universe, where universes grow on a tree and laws are born from an ocean.', 'bubble_universe_sample', 19, -1, 700, ['reality_breach'], [('arbor_heartwood_sample', 4), ('bubble_universe_sample', 1)]),
    ('omnilex', 'The Omnilex', 'Understand the Tree and the Ocean well enough to simulate them, and you control both. Unlocks the Omnilex Engine - the final machine.', 'omnilex', 20, -1, 1000, ['hyperdimensional_reality'], [('law_crystal', 8)]),
    ('simulation_hypothesis', 'The Simulation Hypothesis', 'If reality is computed, it can be decompiled - and edited.', 'source_code_fragment', 14, 0, 300, ['matrioshka_brain', 'cosmogenesis', 'causality_violation', 'holographic_principle'], [('boltzmann_brain', 1)]),
]
RESEARCH += complexity.RESEARCH
RESEARCH += concepts.RESEARCH
RESEARCH += round3.RESEARCH
RESEARCH += round4.RESEARCH
RESEARCH += round5.RESEARCH
RESEARCH += round7.RESEARCH
RESEARCH += round11.RESEARCH
RESEARCH += round15.RESEARCH
RESEARCH = [r[:7] + (list(r[7]) + concepts.RESEARCH_REQ_ADD.get(r[0], []), r[8]) for r in RESEARCH]
DEFS = {r[0]: r for r in RESEARCH}
for rid, t, desc, icon, x, y, ins, req, cost in RESEARCH:
    w(os.path.join(D, 'ts_research', rid + '.json'), {
        'title': t, 'description': desc, 'icon': q(icon), 'x': x, 'y': y, 'insight': ins,
        'requires': [q(r) for r in req], 'cost': [{'item': q(c), 'count': n} for c, n in cost]})
    LANG[f'research.{NS}.{rid}'] = t
    LANG[f'research.{NS}.{rid}.desc'] = desc
    w(os.path.join(D, 'advancement/research', rid + '.json'), {
        'criteria': {'unlocked': {'trigger': 'minecraft:impossible'}}, 'requirements': [['unlocked']]})

# =================================================================================== RECIPES
REC = os.path.join(D, 'recipe')


def ing(x, n=1):
    if x.startswith('#'):
        return {'tag': x[1:], 'count': n}
    return {'item': q(x), 'count': n}


def fing(x, n):
    return {'fluid': q(x), 'amount': n}


MACHINE_RECIPES = []
CRAFTING = []


def mr(name, typ, items_in=(), fluids_in=(), items_out=(), fluids_out=(), energy=0, duration=200, research=None, catalysts=(), chances=None, min_tier=0):
    r = {'type': f'{NS}:{typ}'}
    if min_tier: r['min_tier'] = min_tier
    if catalysts: r['catalysts'] = [({'tag': c[1:]} if c.startswith('#') else {'item': q(c)}) for c in catalysts]
    if chances: r['chances'] = list(chances)
    if items_in: r['item_inputs'] = [ing(i, n) for i, n in items_in]
    if fluids_in: r['fluid_inputs'] = [fing(f, n) for f, n in fluids_in]
    if items_out: r['item_outputs'] = [{'id': q(i), 'count': n} for i, n in items_out]
    if fluids_out: r['fluid_outputs'] = [{'id': q(f), 'amount': n} for f, n in fluids_out]
    r['energy'] = min(int(energy), 2147483647)      # (an int: anything bigger fails to load)
    r['duration'] = duration
    if research: r['research'] = q(research)
    w(os.path.join(REC, typ, name + '.json'), r)
    MACHINE_RECIPES.append(dict(name=name, type=typ, inputs=[i for i, _ in items_in], fluids=[f for f, _ in fluids_in],
                                outputs=[i for i, _ in items_out], fluid_outputs=[f for f, _ in fluids_out],
                                catalysts=list(catalysts), research=research, chances=chances, min_tier=min_tier))


# --- Casimir press
mr('neutronium_plate', 'casimir_pressing', [('neutronium_ingot', 1)], items_out=[('neutronium_plate', 1)], energy=64, duration=100)
mr('casimir_plate', 'casimir_pressing', [('neutronium_plate', 2), ('cherenkov_quartz', 1)], items_out=[('casimir_plate', 1)], energy=128, duration=200, research='casimir_effect')
mr('quantum_dot', 'casimir_pressing', [('cherenkov_quartz', 1), ('minecraft:glowstone_dust', 1)], items_out=[('quantum_dot', 4)], energy=96, duration=160, research='casimir_effect')
mr('solar_statite', 'casimir_pressing', [('metamaterial_sheet', 1), ('neutronium_plate', 1)], items_out=[('solar_statite', 2)], energy=2048, duration=200, research='dyson_swarm')
# --- Entangler
mr('entangled_photon_pair', 'entangling', [('cherenkov_quartz', 2), ('minecraft:amethyst_shard', 1)], items_out=[('entangled_photon_pair', 2)], energy=128, duration=160, research='quantum_entanglement')
mr('qubit', 'entangling', [('entangled_photon_pair', 1), ('quantum_dot', 1)], items_out=[('qubit', 1)], energy=256, duration=200, research='quantum_entanglement')
mr('superposition_crystal', 'entangling', [('qubit', 1), ('cherenkov_quartz', 1)], items_out=[('superposition_crystal', 1)], energy=384, duration=240, research='quantum_entanglement')
# --- Condenser
mr('superfluid_helium', 'condensing', [('casimir_plate', 1)], [('minecraft:water', 1000)], fluids_out=[('superfluid_helium', 500)], energy=256, duration=300, research='cryogenics')
mr('bose_einstein_condensate', 'condensing', [('quantum_dot', 1)], [('superfluid_helium', 250)], fluids_out=[('bose_einstein_condensate', 250)], energy=512, duration=300, research='cryogenics')
mr('synaptic_fluid', 'condensing', [('photonic_neuron', 1)], [('superfluid_helium', 500)], fluids_out=[('synaptic_fluid', 500)], energy=4096, duration=300, research='artificial_intelligence')
# --- Weaver
mr('metamaterial_sheet', 'weaving', [('neutronium_plate', 1), ('quantum_dot', 2), ('cherenkov_quartz', 1)], [('bose_einstein_condensate', 100)], [('metamaterial_sheet', 1)], energy=768, duration=300, research='metamaterials')
mr('superconducting_coil', 'weaving', [('neutronium_plate', 1), ('#c:ingots/copper', 4)], [('superfluid_helium', 100)], [('superconducting_coil', 2)], energy=512, duration=240, research='cryogenics')
mr('dark_matter_lens', 'weaving', [('metamaterial_sheet', 1), ('void_lattice_shard', 1)], [('dark_matter', 500)], [('dark_matter_lens', 1)], energy=4096, duration=400, research='dark_sector')
mr('dyson_swarm_satellite', 'weaving', [('solar_statite', 2), ('quantum_processor', 1), ('superconducting_coil', 1)], [], [('dyson_swarm_satellite', 1)], energy=8192, duration=300, research='dyson_swarm')
mr('dyson_shell_segment', 'weaving', [('neutronium_plate', 4), ('metamaterial_sheet', 2), ('higgs_field_capacitor', 1)], [('stellar_plasma', 1000)], [('dyson_shell_segment', 1)], energy=32768, duration=600, research='dyson_sphere')
# --- Tachyon resonator
mr('tachyon_crystal', 'tachyon_resonance', [('cherenkov_quartz', 4), ('void_lattice_shard', 1)], [('superfluid_helium', 500)], [('tachyon_crystal', 1)], [('tachyon_flux', 250)], 2048, 300, 'tachyonics')
mr('tachyon_flux', 'tachyon_resonance', [('tachyon_crystal', 1)], [('superfluid_helium', 250)], [], [('tachyon_flux', 1000)], 1024, 200, 'tachyonics')
# --- Penning trap
mr('antimatter_pellet', 'antimatter_trapping', [('entangled_photon_pair', 2), ('superconducting_coil', 1)], [('superfluid_helium', 250)], [('antimatter_pellet', 1)], [], 4096, 400, 'antimatter')
# --- Chronon inverter
mr('chronon_capacitor', 'chrono_inversion', [('tachyon_crystal', 1), ('qubit', 1)], [('tachyon_flux', 250)], [('chronon_capacitor', 1)], [('chronon_gas', 250)], 4096, 400, 'chronodynamics')
mr('hawking_emitter', 'chrono_inversion', [('topological_insulator', 1)], [('hawking_radiation', 500)], [('hawking_emitter', 1)], [], 8192, 400, 'singularity_physics')
# --- Topological transmuter
mr('topological_insulator', 'phase_transmutation', [('metamaterial_sheet', 1), ('qubit', 1)], [('bose_einstein_condensate', 250)], [('topological_insulator', 1)], [], 2048, 300, 'topology')
mr('calabi_yau_manifold', 'phase_transmutation', [('topological_insulator', 2), ('void_lattice_shard', 4)], [('bose_einstein_condensate', 500)], [('calabi_yau_manifold', 1)], [], 16384, 600, 'hyperdimensional_geometry')
mr('higgs_field_capacitor', 'phase_transmutation', [('superconducting_coil', 1), ('magnetic_monopole', 1), ('qubit', 1)], [('bose_einstein_condensate', 250)], [('higgs_field_capacitor', 1)], [], 8192, 400, 'particle_physics')
# --- Zero point (generator: energy = FE/t produced)
mr('casimir_plate', 'zero_point', [('casimir_plate', 1)], energy=64, duration=2400, research='casimir_effect')
mr('metamaterial_sheet', 'zero_point', [('metamaterial_sheet', 1)], energy=256, duration=2400, research='casimir_effect')
mr('exotic_matter_crystal', 'zero_point', [('exotic_matter_crystal', 1)], energy=2048, duration=6000, research='casimir_effect')
mr('spin_foam_lattice', 'zero_point', [('spin_foam_lattice', 1)], energy=8192, duration=6000, research='quantum_gravity')
# --- String loom
mr('quantum_string', 'string_weaving', [('cosmic_string_segment', 1), ('tachyon_crystal', 1), ('superposition_crystal', 1)], [('exotic_matter', 250)], [('quantum_string', 4)], [], 32768, 400, 'string_theory')
mr('m_brane', 'string_weaving', [('quantum_string', 6), ('metamaterial_sheet', 1)], [('exotic_matter', 500)], [('m_brane', 1)], [], 65536, 600, 'm_theory')
# --- Graviton condenser
mr('graviton_crystal', 'graviton_condensing', [('micro_singularity', 1), ('quantum_string', 1)], [('hawking_radiation', 1000)], [('graviton_crystal', 4)], [], 65536, 800, 'quantum_gravity')
mr('spin_foam_lattice', 'graviton_condensing', [('graviton_crystal', 2), ('qubit', 1)], [('bose_einstein_condensate', 500)], [('spin_foam_lattice', 2)], [], 32768, 400, 'quantum_gravity')
mr('rift_anchor', 'graviton_condensing', [('graviton_crystal', 2), ('void_lattice_shard', 1)], [('exotic_matter', 1000)], [('rift_anchor', 1)], [], 131072, 600, 'multiverse')
mr('universe_seed', 'graviton_condensing', [('m_brane', 1), ('graviton_crystal', 1)], [('inflaton_field', 1000)], [('universe_seed', 1)], [], 262144, 1200, 'cosmogenesis')
# --- Neural synthesizer
mr('photonic_neuron', 'neural_synthesis', [('quantum_dot', 2), ('cherenkov_quartz', 1)], [('superfluid_helium', 100)], [('photonic_neuron', 2)], [], 8192, 200, 'artificial_intelligence')
mr('neural_lattice', 'neural_synthesis', [('photonic_neuron', 8), ('metamaterial_sheet', 1)], [('synaptic_fluid', 500)], [('neural_lattice', 1)], [], 16384, 400, 'artificial_intelligence')
mr('narrow_ai_core', 'neural_synthesis', [('neural_lattice', 1), ('quantum_processor', 2)], [('synaptic_fluid', 1000)], [('narrow_ai_core', 1)], [], 32768, 600, 'artificial_intelligence')
mr('general_ai_core', 'neural_synthesis', [('narrow_ai_core', 4), ('superposition_crystal', 4), ('topological_insulator', 2)], [('synaptic_fluid', 2000)], [('general_ai_core', 1)], [], 131072, 1200, 'general_intelligence')
mr('superintelligence_core', 'neural_synthesis', [('general_ai_core', 4), ('tesseract_core', 1), ('spin_foam_lattice', 1)], [('synaptic_fluid', 4000)], [('superintelligence_core', 1)], [], 1048576, 2400, 'superintelligence')
mr('computronium_ingot', 'neural_synthesis', [('neutronium_ingot', 2), ('quantum_processor', 1), ('photonic_neuron', 1)], [('synaptic_fluid', 250)], [('computronium_ingot', 2)], [], 65536, 400, 'superintelligence')
# --- Collider
mr('quark_gluon_plasma', 'collider', [('neutronium_ingot', 2)], [('superfluid_helium', 500)], [], [('quark_gluon_plasma', 1000)], 32768, 400, 'particle_physics')
mr('strange_matter', 'collider', [('void_lattice_shard', 1), ('degenerate_matter_chunk', 1)], [('quark_gluon_plasma', 500)], [('strange_matter_pellet', 1)], [('strange_matter', 250)], 65536, 400, 'particle_physics')
mr('magnetic_monopole', 'collider', [('superconducting_coil', 2)], [('quark_gluon_plasma', 1000)], [('magnetic_monopole', 1)], [], 131072, 600, 'particle_physics')
# --- Dark matter
mr('dark_matter', 'halo_collection', [], [], [], [('dark_matter', 250)], 16384, 400, 'dark_sector')
mr('exotic_matter_crystal', 'halo_collection', [('void_lattice_shard', 1)], [], [('exotic_matter_crystal', 1)], [('dark_matter', 100)], 32768, 400, 'dark_sector')
# --- Kugelblitz
mr('micro_singularity', 'kugelblitz', [('entangled_photon_pair', 16), ('metamaterial_sheet', 2)], [('quark_gluon_plasma', 1000)], [('micro_singularity', 1)], [('hawking_radiation', 250)], 524288, 1200, 'singularity_physics')
mr('kugelblitz_seed', 'kugelblitz', [('antimatter_pellet', 4), ('strange_matter_pellet', 2)], [('dark_matter', 1000)], [('kugelblitz_seed', 1)], [], 262144, 800, 'singularity_physics')
mr('micro_singularity_from_seed', 'kugelblitz', [('kugelblitz_seed', 1), ('entangled_photon_pair', 8)], [('quark_gluon_plasma', 500)], [('micro_singularity', 2)], [('hawking_radiation', 500)], 524288, 800, 'singularity_physics')
# --- Penrose (generator)
mr('micro_singularity', 'penrose', [('micro_singularity', 1)], [], [], [('hawking_radiation', 1000)], 65536, 24000, 'penrose_process')
mr('kugelblitz_seed', 'penrose', [('kugelblitz_seed', 1)], [], [], [('hawking_radiation', 250)], 16384, 12000, 'penrose_process')
# --- White hole
mr('white_hole_fragment', 'white_hole', [('micro_singularity', 1), ('chronon_capacitor', 1)], [('chronon_gas', 1000)], [('white_hole_fragment', 1), ('exotic_matter_crystal', 2)], [('exotic_matter', 1000)], 1048576, 1200, 'white_holes')
mr('cosmic_string_segment', 'white_hole', [('white_hole_fragment', 1), ('hawking_emitter', 1)], [('hawking_radiation', 1000)], [('cosmic_string_segment', 2)], [('exotic_matter', 500)], 524288, 800, 'white_holes')
mr('stellar_core', 'white_hole', [('white_hole_fragment', 1), ('neutronium_block', 4)], [('quark_gluon_plasma', 4000)], [('stellar_core', 1)], [('stellar_plasma', 1000)], 2097152, 2400, 'stellar_engineering')
# --- Tesseract assembler
mr('tesseract_core', 'hyperdimensional', [('calabi_yau_manifold', 1), ('quantum_processor', 4), ('exotic_matter_crystal', 4), ('white_hole_fragment', 1)], [('exotic_matter', 1000), ('chronon_gas', 500)], [('tesseract_core', 1)], [], 2097152, 1600, 'hyperdimensional_geometry')
mr('alcubierre_coil', 'hyperdimensional', [('superconducting_coil', 4), ('exotic_matter_crystal', 4), ('magnetic_monopole', 1), ('tachyon_crystal', 1)], [('exotic_matter', 2000)], [('alcubierre_coil', 1)], [], 1048576, 1200, 'hyperdimensional_geometry')
mr('planck_shard', 'hyperdimensional', [('white_hole_fragment', 1), ('micro_singularity', 1), ('cosmic_string_segment', 1)], [('exotic_matter', 1000)], [('planck_shard', 4)], [], 4194304, 2000, 'hyperdimensional_geometry')
# --- Multiverse rift
mr('rift_harvest', 'rift_harvesting', [('rift_anchor', 1), ('m_brane', 1)], [('exotic_matter', 1000)], [('multiverse_shard', 4), ('void_lattice_shard', 8), ('exotic_matter_crystal', 2)], [('inflaton_field', 1000)], 8388608, 2400, 'multiverse')
mr('false_vacuum', 'rift_harvesting', [('multiverse_shard', 1), ('graviton_crystal', 1)], [('dark_matter', 2000)], [('multiverse_shard', 1)], [('false_vacuum', 1000)], 4194304, 1200, 'multiverse')
mr('alien_matter', 'rift_harvesting', [('multiverse_shard', 1)], [('exotic_matter', 500)], [('cherenkov_quartz', 32), ('degenerate_matter_chunk', 16), ('void_lattice_shard', 16), ('minecraft:echo_shard', 4)], [], 2097152, 800, 'multiverse')
# --- Universe forge
mr('pocket_universe', 'universe_forging', [('universe_seed', 1), ('spin_foam_lattice', 4), ('m_brane', 2), ('graviton_crystal', 4)], [('false_vacuum', 4000), ('inflaton_field', 4000)], [('pocket_universe', 1)], [], 33554432, 6000, 'cosmogenesis')
mr('harvest_universe', 'universe_forging', [('pocket_universe', 1)], [], [('degenerate_matter_chunk', 64), ('cherenkov_quartz', 64), ('exotic_matter_crystal', 16), ('heat_death_remnant', 1)], [('stellar_plasma', 8000)], 16777216, 4800, 'cosmogenesis')
mr('recycle_remnants', 'universe_forging', [('heat_death_remnant', 4)], [('inflaton_field', 2000)], [('universe_seed', 1)], [], 8388608, 2400, 'cosmogenesis')
# --- Matrioshka brain
mr('quantum_processors', 'matrioshka', [('computronium_ingot', 2)], [('synaptic_fluid', 500)], [('quantum_processor', 8)], [], 4194304, 400, 'matrioshka_brain')
mr('superintelligence', 'matrioshka', [('general_ai_core', 4), ('computronium_ingot', 8)], [('synaptic_fluid', 4000)], [('superintelligence_core', 2)], [], 33554432, 2400, 'matrioshka_brain')
mr('contemplate', 'matrioshka', [('qubit', 16), ('computronium_ingot', 1)], [('synaptic_fluid', 1000)], [('superposition_crystal', 16)], [], 8388608, 1200, 'matrioshka_brain')
# --- Dyson (generator; output scales with swarm/shell)
mr('stellar_core', 'dyson', [('stellar_core', 1)], energy=4096, duration=72000, research='dyson_swarm')

# --- Maxwell's demon
mr('maxwell_demon_chip', 'entropy_sorting', [('cherenkov_quartz', 1), ('qubit', 1)], [('superfluid_helium', 500)], [('maxwell_demon_chip', 1)], [], 2048, 300, 'thermodynamics_of_information')
mr('negentropy_cell', 'entropy_sorting', [('neutronium_plate', 1), ('maxwell_demon_chip', 1)], [('bose_einstein_condensate', 250)], [('negentropy_cell', 1), ('maxwell_demon_chip', 1)], [], 4096, 400, 'thermodynamics_of_information')
mr('reverse_heat_death', 'entropy_sorting', [('heat_death_remnant', 1), ('negentropy_cell', 4)], [('dark_energy', 1000)], [('universe_seed', 1)], [('inflaton_field', 500)], 16777216, 2400, 'dark_energy')
# --- Nanotech
mr('nanite_swarm', 'nanofabrication', [('quantum_dot', 2), ('neutronium_plate', 1), ('maxwell_demon_chip', 1)], [('superfluid_helium', 250)], [('nanite_swarm', 4)], [], 8192, 300, 'nanotechnology')
mr('programmable_matter', 'nanofabrication', [('nanite_swarm', 4), ('metamaterial_sheet', 1), ('qubit', 1)], [('nanite_slurry', 500)], [('programmable_matter', 1)], [], 32768, 400, 'nanotechnology')
mr('claytronic_catom', 'nanofabrication', [('programmable_matter', 1), ('superposition_crystal', 2)], [('nanite_slurry', 250)], [('claytronic_catom', 4)], [], 32768, 300, 'nanotechnology')
mr('von_neumann_probe', 'nanofabrication', [('claytronic_catom', 4), ('quantum_processor', 1), ('dyson_swarm_satellite', 1)], [('nanite_slurry', 2000)], [('von_neumann_probe', 1)], [], 1048576, 1200, 'dyson_swarm')
mr('nanite_slurry', 'condensing', [('nanite_swarm', 1)], [('minecraft:water', 1000)], fluids_out=[('nanite_slurry', 1000)], energy=4096, duration=200, research='nanotechnology')
# --- Neutrinos
mr('neutrino_flux', 'neutrino_capture', [], [], [], [('neutrino_flux', 100)], 4096, 200, 'neutrino_physics')
mr('sterile_neutrino', 'neutrino_capture', [('degenerate_matter_chunk', 1)], [], [('sterile_neutrino', 1)], [('neutrino_flux', 250)], 16384, 400, 'neutrino_physics')
# --- Mass
mr('massless_alloy', 'mass_modulation', [('neutronium_ingot', 2), ('sterile_neutrino', 1)], [('bose_einstein_condensate', 500)], [('massless_alloy', 2)], [], 65536, 400, 'mass_manipulation')
mr('negative_mass_ingot', 'mass_modulation', [('massless_alloy', 1), ('exotic_matter_crystal', 1)], [('exotic_matter', 500)], [('negative_mass_ingot', 1)], [], 131072, 600, 'mass_manipulation')
mr('inertia_damper', 'mass_modulation', [('negative_mass_ingot', 1), ('superconducting_coil', 1)], [('nanite_slurry', 250)], [('inertia_damper', 1)], [], 65536, 400, 'mass_manipulation')
# --- Holography
mr('holographic_matrix', 'holographic_encoding', [('metamaterial_sheet', 1), ('quantum_processor', 1)], [('bose_einstein_condensate', 500)], [('holographic_matrix', 1)], [], 65536, 400, 'holographic_principle')
mr('bekenstein_crystal', 'holographic_encoding', [('holographic_matrix', 1), ('hawking_emitter', 1)], [('hawking_radiation', 1000)], [('bekenstein_crystal', 1)], [], 262144, 600, 'holographic_principle')
# --- Psionics
mr('psionic_crystal', 'psionic_attunement', [('superposition_crystal', 1), ('photonic_neuron', 1)], [('synaptic_fluid', 500)], [('psionic_crystal', 1)], [], 16384, 300, 'psionics')
mr('thought_matrix', 'psionic_attunement', [('psionic_crystal', 1), ('neural_lattice', 1)], [('psionic_ether', 500)], [('thought_matrix', 1)], [], 65536, 400, 'psionics')
mr('boltzmann_brain', 'psionic_attunement', [('thought_matrix', 1), ('heat_death_remnant', 1)], [('psionic_ether', 1000)], [('boltzmann_brain', 1)], [], 524288, 800, 'psionics')
mr('psionic_ether', 'condensing', [('psionic_crystal', 1)], [('synaptic_fluid', 500)], fluids_out=[('psionic_ether', 1000)], energy=16384, duration=300, research='psionics')
# --- Annihilation (generator)
mr('annihilation_core', 'antimatter_trapping', [('antimatter_pellet', 4), ('dilithium_crystal', 1)], [('superfluid_helium', 1000)], [('annihilation_core', 1)], [], 65536, 600, 'annihilation_power')
mr('antimatter_pellet', 'annihilation', [('antimatter_pellet', 1), ('dilithium_crystal', 1)], [('superfluid_helium', 100)], [], [('neutrino_flux', 250)], 131072, 1200, 'annihilation_power')
mr('annihilation_core', 'annihilation', [('annihilation_core', 1)], [('superfluid_helium', 250)], [], [('neutrino_flux', 1000)], 524288, 2400, 'annihilation_power')
# --- Neutron star forge
mr('neutronium_compression', 'stellar_compression', [('degenerate_matter_chunk', 16)], [('superfluid_helium', 1000)], [('neutronium_block', 2)], [('metallic_hydrogen', 1000)], 1048576, 800, 'compact_stars')
mr('quark_star_core', 'stellar_compression', [('neutronium_block', 4), ('strange_matter_pellet', 1)], [('quark_gluon_plasma', 2000)], [('quark_star_core', 1), ('magnetar_shard', 2)], [], 2097152, 1200, 'compact_stars', min_tier=2)
mr('pulsar_timing_crystal', 'stellar_compression', [('magnetar_shard', 1), ('cherenkov_quartz_block', 1), ('chronon_capacitor', 1)], [('neutrino_flux', 500)], [('pulsar_timing_crystal', 2)], [], 1048576, 600, 'compact_stars')
mr('metallic_hydrogen_ingot', 'condensing', [('casimir_plate', 1)], [('metallic_hydrogen', 1000)], [('metallic_hydrogen_ingot', 1)], [], 32768, 300, 'compact_stars')
# --- Wormholes
mr('wormhole_throat', 'hyperdimensional', [('negative_mass_ingot', 4), ('exotic_matter_crystal', 4), ('white_hole_fragment', 1), ('micro_singularity', 1)], [('exotic_matter', 2000)], [('wormhole_throat', 1)], [], 4194304, 2000, 'traversable_wormholes')
mr('distant_star_system', 'wormhole_transit', [('dilithium_crystal', 1), ('sterile_neutrino', 1)], [('exotic_matter', 1000)], [('degenerate_matter_chunk', 16), ('void_lattice_shard', 8), ('dilithium_crystal', 4), ('magnetar_shard', 1)], [('neutrino_flux', 500)], 4194304, 1200, 'traversable_wormholes')
mr('neighbouring_brane', 'wormhole_transit', [('multiverse_shard', 1), ('pulsar_timing_crystal', 1)], [('exotic_matter', 2000)], [('multiverse_shard', 4), ('exotic_matter_crystal', 4)], [], 8388608, 1600, 'traversable_wormholes')
# --- Causality loops: bootstrap paradox duplication (the curve is returned)
mr('closed_timelike_curve', 'causality_loop', [('chronon_capacitor', 4), ('wormhole_throat', 1)], [('chronon_gas', 4000)], [('closed_timelike_curve', 1), ('paradox_residue', 1)], [], 8388608, 2400, 'causality_violation')
mr('bootstrap_artifact', 'causality_loop', [('paradox_residue', 8), ('closed_timelike_curve', 1)], [('chronon_gas', 2000)], [('bootstrap_artifact', 1), ('closed_timelike_curve', 1)], [], 4194304, 1200, 'causality_violation')
for dup, cost in (('tesseract_core', 8388608), ('micro_singularity', 4194304), ('white_hole_fragment', 8388608), ('superintelligence_core', 16777216),
                  ('stellar_core', 16777216), ('pocket_universe', 67108864), ('wormhole_throat', 16777216)):
    mr('loop_' + dup, 'causality_loop', [(dup, 1), ('closed_timelike_curve', 1)], [('chronon_gas', 2000)], [(dup, 2), ('closed_timelike_curve', 1)], [], cost, 3600, 'causality_violation')
# --- Dark energy
mr('dark_energy', 'dark_energy_extraction', [], [], [], [('dark_energy', 250)], 1048576, 400, 'dark_energy')
mr('cosmological_constant', 'dark_energy_extraction', [('dark_matter_lens', 1)], [], [('cosmological_constant', 1)], [('dark_energy', 1000)], 2097152, 800, 'dark_energy')
# --- Reality compiler
mr('source_code_fragment', 'reality_compilation', [('bekenstein_crystal', 1), ('superintelligence_core', 1), ('holographic_matrix', 1), ('boltzmann_brain', 1)], [('synaptic_fluid', 4000), ('dark_energy', 2000)], [('source_code_fragment', 2)], [], 33554432, 4800, 'simulation_hypothesis')
mr('reality_anchor', 'reality_compilation', [('source_code_fragment', 1), ('bootstrap_artifact', 1), ('claytronic_catom', 4), ('quark_star_core', 1)], [('psionic_ether', 2000)], [('reality_anchor', 1)], [], 16777216, 2400, 'simulation_hypothesis')
mr('omega_point', 'reality_compilation', [('source_code_fragment', 4), ('pocket_universe', 1), ('superintelligence_core', 4), ('reality_anchor', 1)], [('dark_energy', 8000), ('synaptic_fluid', 8000)], [('omega_point', 1)], [], 134217728, 12000, 'simulation_hypothesis')
mr('omega_point', 'zero_point', [('omega_point', 1)], energy=20000, duration=720000, research='simulation_hypothesis')
# --- Cantor diagonalizer: manufacturing infinities
mr('aleph_null_shard', 'transfinite_computation', [('quantum_processor', 1), ('source_code_fragment', 1), ('superposition_crystal', 4)], [('synaptic_fluid', 1000)], [('aleph_null_shard', 2)], [], 4194304, 600, 'transfinite_mathematics')
mr('godel_sentence', 'transfinite_computation', [('aleph_null_shard', 2), ('superintelligence_core', 1), ('holographic_matrix', 1)], [('synaptic_fluid', 2000)], [('godel_sentence', 1)], [], 8388608, 900, 'incompleteness')
mr('russell_paradox_set', 'transfinite_computation', [('godel_sentence', 1), ('aleph_null_shard', 2), ('paradox_residue', 4)], [('chronon_gas', 2000)], [('russell_paradox_set', 1)], [], 8388608, 900, 'paradox_engineering')
mr('banach_tarski_sphere', 'transfinite_computation', [('tesseract_core', 1), ('aleph_null_shard', 2), ('negative_mass_ingot', 2)], [], [('banach_tarski_sphere', 1)], [], 8388608, 900, 'paradox_engineering')
mr('banach_tarski_doubling', 'transfinite_computation', [('banach_tarski_sphere', 1), ('aleph_null_shard', 1)], [], [('banach_tarski_sphere', 2)], [], 16777216, 1200, 'paradox_engineering')
mr('absolute_infinity', 'transfinite_computation', [('aleph_null_shard', 8), ('godel_sentence', 1), ('russell_paradox_set', 1), ('banach_tarski_sphere', 1)], [('dark_energy', 4000)], [('absolute_infinity', 1)], [], 67108864, 2400, 'axiom_theory')
# --- Axiom distiller
mr('axiom_fragment', 'axiom_distillation', [('source_code_fragment', 1), ('paradox_residue', 2)], [('dark_energy', 1000)], [('axiom_fragment', 2)], [], 16777216, 1200, 'axiom_theory')
mr('axiom_fragment_from_law', 'axiom_distillation', [], [('primordial_law', 1000)], [('axiom_fragment', 4), ('law_crystal', 1)], [], 4194304, 400, 'hyperdimensional_reality')
mr('law_crystal', 'axiom_distillation', [('axiom_fragment', 1)], [('primordial_law', 500)], [('law_crystal', 2)], [], 8388608, 600, 'hyperdimensional_reality')
mr('primordial_law_from_crystal', 'axiom_distillation', [('law_crystal', 4), ('absolute_infinity', 1)], [('dark_energy', 8000)], [('absolute_infinity', 1)], [('primordial_law', 1000)], 33554432, 2400, 'omnilex')
# --- Axiomatic breach
mr('axiom_key', 'reality_breach', [('absolute_infinity', 1), ('axiom_fragment', 16), ('reality_anchor', 1), ('godel_sentence', 1)], [('dark_energy', 16000), ('synaptic_fluid', 8000)], [('axiom_key', 1)], [], 268435456, 2400, 'reality_breach')
mr('open_breach', 'reality_breach', [('axiom_fragment', 2)], [('dark_energy', 1000)], [('paradox_residue', 1)], [], 67108864, 600, 'reality_breach')
# --- Omnilex engine: simulating and controlling the Tree and the Ocean
mr('tree_simulacrum', 'omnilex_simulation', [('arbor_heartwood', 32), ('bubble_universe_sample', 8), ('absolute_infinity', 1), ('superintelligence_core', 4)], [('primordial_law', 8000)], [('tree_simulacrum', 1)], [], 536870912, 4800, 'omnilex')
mr('ocean_simulacrum', 'omnilex_simulation', [('law_crystal', 32), ('axiom_fragment', 16), ('russell_paradox_set', 2), ('godel_sentence', 2)], [('primordial_law', 16000)], [('ocean_simulacrum', 1)], [], 536870912, 4800, 'omnilex')
mr('omnilex', 'omnilex_simulation', [('tree_simulacrum', 1), ('ocean_simulacrum', 1), ('omega_point', 1), ('axiom_key', 1)], [('primordial_law', 16000), ('dark_energy', 16000)], [('omnilex', 1), ('axiom_key', 1)], [], 1073741824, 12000, 'omnilex')
mr('grow_bubble_universes', 'omnilex_simulation', [('tree_simulacrum', 1), ('universe_seed', 1)], [('primordial_law', 1000)], [('tree_simulacrum', 1), ('bubble_universe_sample', 4), ('pocket_universe', 1)], [], 134217728, 1200, 'omnilex')
mr('rewrite_laws', 'omnilex_simulation', [('ocean_simulacrum', 1)], [('primordial_law', 2000)], [('ocean_simulacrum', 1), ('law_crystal', 8), ('axiom_fragment', 8)], [], 134217728, 1200, 'omnilex')
mr('simulate_everything', 'omnilex_simulation', [('tree_simulacrum', 1), ('ocean_simulacrum', 1)], [('primordial_law', 8000)], [('tree_simulacrum', 1), ('ocean_simulacrum', 1), ('omega_point', 1), ('absolute_infinity', 1)], [('primordial_law', 4000)], 1073741824, 24000, 'omnilex')

# =================================================================================== GT-STYLE CHAINS
# --- coolant loop: anything that burns superfluid helium hands back warm helium, which must be recondensed
mr('recondense_helium', 'condensing', [], [('warm_helium', 1000)], [], [('superfluid_helium', 900)], 384, 200, 'cryogenics')
# --- process fluids
mr('photoresist', 'condensing', [('quantum_dot', 2)], [('minecraft:water', 1000)], [], [('photoresist', 1000)], 256, 200, 'precision_lithography')
mr('superconducting_solder', 'condensing', [('superconducting_coil', 1)], [('superfluid_helium', 500)], [], [('superconducting_solder', 576)], 1024, 300, 'femtoengineering')
mr('causal_solder', 'chrono_inversion', [('tachyon_crystal', 1), ('chronon_capacitor', 1)], [('exotic_solder', 576)], [], [('causal_solder', 576)], 65536, 600, 'causality_violation')
# --- crystal growth -> wafers -> lithography -> dies (the wafer chain)
mr('lattice_seed', 'casimir_pressing', [('cherenkov_quartz', 2)], items_out=[('lattice_seed', 1)], energy=128, duration=200, research='precision_lithography')
mr('cherenkov_boule', 'crystal_growth', [('lattice_seed', 1), ('cherenkov_quartz', 8)], [('superfluid_helium', 1000)], [('cherenkov_boule', 1)], [('warm_helium', 1000)], 512, 1200, 'precision_lithography')
mr('topological_boule', 'crystal_growth', [('lattice_seed', 1), ('topological_insulator', 2)], [('bose_einstein_condensate', 1000)], [('topological_boule', 1)], [], 8192, 1600, 'topology')
mr('hyperdimensional_boule', 'crystal_growth', [('lattice_seed', 1), ('calabi_yau_manifold', 1)], [('exotic_matter', 1000)], [('hyperdimensional_boule', 1)], [], 131072, 2400, 'hyperdimensional_geometry')
for b, wf in (('cherenkov_boule', 'cherenkov_wafer'), ('topological_boule', 'topological_wafer'), ('hyperdimensional_boule', 'hyperdimensional_wafer')):
    mr('slice_' + b, 'planck_cutting', [(b, 1)], [('superfluid_helium', 250)], [(wf, 16), ('spacetime_slag', 1)], [('warm_helium', 250)], 512, 600, 'precision_lithography', chances=[1.0, 0.1])
LITHO = [('photonic_logic_wafer', 'cherenkov_wafer', 'photonic_lens', 'photonic_die', 'precision_lithography', 256),
         ('qubit_array_wafer', 'cherenkov_wafer', 'metamaterial_sheet', 'qubit_die', 'femtoengineering', 2048),
         ('topological_logic_wafer', 'topological_wafer', 'dark_matter_lens', 'topological_die', 'topology', 16384),
         ('hyperdimensional_logic_wafer', 'hyperdimensional_wafer', 'tesseract_lens', 'hyperdimensional_die', 'hyperdimensional_geometry', 131072),
         ('cosmic_logic_wafer', 'hyperdimensional_wafer', 'cosmic_lens', 'cosmic_die', 'multiverse', 1048576)]
for out, wf, lens, die, res, e in LITHO:
    mr(out, 'lithography', [(wf, 1)], [('photoresist', 100)], [(out, 1)], [], e, 400, res, catalysts=[lens])
    mr(die, 'planck_cutting', [(out, 1)], [('superfluid_helium', 100)], [(die, 8)], [('warm_helium', 100)], e, 300, res)
mr('tesseract_lens', 'hyperdimensional', [('tesseract_core', 1), ('dark_matter_lens', 2), ('photonic_lens', 1)], [('exotic_matter', 1000)], [('tesseract_lens', 1)], [('dimensional_residue', 250)], 2097152, 1600, 'hyperdimensional_geometry')
mr('cosmic_lens', 'string_weaving', [('cosmic_string_segment', 2), ('tesseract_lens', 1), ('graviton_crystal', 1)], [('exotic_matter', 1000)], [('cosmic_lens', 1)], [], 4194304, 2000, 'multiverse')
# --- femtoscale / attoscale components
mr('femto_capacitor', 'femtofabrication', [('casimir_plate', 1), ('neutronium_plate', 2)], [('superconducting_solder', 144)], [('femto_capacitor', 8)], [], 2048, 300, 'femtoengineering')
mr('femto_transistor', 'femtofabrication', [('qubit', 1), ('quantum_dot', 4)], [('photoresist', 250)], [('femto_transistor', 8)], [], 2048, 300, 'femtoengineering')
mr('femto_inductor', 'femtofabrication', [('superconducting_coil', 2), ('neutronium_ingot', 1)], [('superconducting_solder', 144)], [('femto_inductor', 8)], [], 2048, 300, 'femtoengineering')
mr('atto_capacitor', 'femtofabrication', [('femto_capacitor', 4), ('higgs_field_capacitor', 1)], [('exotic_solder', 144)], [('atto_capacitor', 8)], [], 65536, 600, 'assembly_lines')
mr('atto_transistor', 'femtofabrication', [('femto_transistor', 4), ('superposition_crystal', 1), ('topological_insulator', 1)], [('exotic_solder', 144)], [('atto_transistor', 8)], [], 65536, 600, 'assembly_lines')
mr('atto_inductor', 'femtofabrication', [('femto_inductor', 4), ('magnetic_monopole', 1)], [('exotic_solder', 144)], [('atto_inductor', 8)], [], 65536, 600, 'assembly_lines')
# --- boards
mr('circuit_substrate', 'casimir_pressing', [('neutronium_plate', 1), ('quantum_dot', 1)], items_out=[('circuit_substrate', 2)], energy=192, duration=200, research='precision_lithography')
mr('photonic_board', 'weaving', [('circuit_substrate', 1), ('superconducting_coil', 1), ('cherenkov_quartz', 2)], [('photoresist', 250)], [('photonic_board', 1)], energy=512, duration=300, research='precision_lithography')
mr('topological_board', 'phase_transmutation', [('circuit_substrate', 2), ('topological_insulator', 1), ('femto_inductor', 4)], [('photoresist', 500)], [('topological_board', 1)], [], 16384, 600, 'assembly_lines')
mr('cosmic_board', 'hyperdimensional', [('circuit_substrate', 4), ('calabi_yau_manifold', 1), ('cosmic_string_segment', 1), ('atto_inductor', 8)], [('exotic_solder', 576)], [('cosmic_board', 1)], [('dimensional_residue', 500)], 4194304, 1600, 'multiverse', catalysts=[])
# --- processors (each tier needs the tier below it, like GregTech circuits)
mr('photonic_processor', 'weaving', [('photonic_board', 1), ('photonic_die', 2), ('quantum_dot', 2)], [('superconducting_solder', 72)], [('photonic_processor', 2)], energy=768, duration=300, research='precision_lithography')
mr('photonic_processor_early', 'weaving', [('photonic_board', 1), ('photonic_die', 4), ('quantum_dot', 4)], [('superfluid_helium', 250)], [('photonic_processor', 1)], energy=768, duration=600, research='precision_lithography')
mr('quantum_processor', 'femtofabrication', [('photonic_board', 1), ('qubit_die', 2), ('photonic_processor', 2), ('femto_transistor', 4)], [('superconducting_solder', 144)], [('quantum_processor', 2)], [], 8192, 600, 'femtoengineering')
mr('topological_processor', 'femtofabrication', [('topological_board', 1), ('topological_die', 2), ('quantum_processor', 2), ('femto_capacitor', 8)], [('exotic_solder', 144)], [('topological_processor', 2)], [], 131072, 800, 'assembly_lines')
mr('hyperdimensional_processor', 'femto_assembly', [('topological_board', 1), ('hyperdimensional_die', 4), ('topological_processor', 2), ('atto_capacitor', 8), ('atto_transistor', 8), ('atto_inductor', 8), ('tesseract_lens', 1)],
   [('exotic_solder', 576), ('superfluid_helium', 1000)], [('hyperdimensional_processor', 2)], [], 1048576, 1200, 'hyperdimensional_geometry')
mr('cosmic_processor', 'femto_assembly', [('cosmic_board', 1), ('cosmic_die', 4), ('hyperdimensional_processor', 2), ('atto_capacitor', 16), ('atto_transistor', 16), ('atto_inductor', 16), ('graviton_crystal', 2)],
   [('causal_solder', 576), ('superfluid_helium', 2000), ('dark_matter', 1000)], [('cosmic_processor', 2)], [], 8388608, 1600, 'multiverse', catalysts=['spacetime_catalyst'])
mr('ontological_processor', 'femto_assembly', [('cosmic_board', 1), ('source_code_fragment', 1), ('cosmic_processor', 2), ('holographic_matrix', 2), ('atto_transistor', 32), ('bekenstein_crystal', 2), ('boltzmann_brain', 1)],
   [('causal_solder', 1152), ('synaptic_fluid', 4000), ('superfluid_helium', 4000)], [('ontological_processor', 2)], [], 67108864, 2400, 'simulation_hypothesis', catalysts=['spacetime_catalyst'])
mr('transfinite_processor', 'femto_assembly', [('cosmic_board', 2), ('aleph_null_shard', 4), ('ontological_processor', 2), ('godel_sentence', 1), ('atto_capacitor', 64), ('atto_transistor', 64), ('atto_inductor', 64)],
   [('causal_solder', 2304), ('dark_energy', 4000), ('superfluid_helium', 8000)], [('transfinite_processor', 2)], [], 268435456, 3600, 'incompleteness', catalysts=['eternity_catalyst'])
# --- plasma metallurgy (Stellar Plasma Crucible)
# bootstrap: a slow, expensive way to make the first Chronosteel before the crucible exists (GregTech-style)
mr('chronosteel_bootstrap', 'chrono_inversion', [('neutronium_ingot', 4), ('chronon_capacitor', 2)], [('chronon_gas', 1000)], [('chronosteel_ingot', 1)], [], 16384, 1200, 'plasma_metallurgy')
mr('chronosteel', 'plasma_smelting', [('neutronium_ingot', 2), ('chronon_capacitor', 1)], [('chronon_gas', 500), ('superfluid_helium', 1000)], [('chronosteel_ingot', 2)], [('warm_helium', 1000)], 32768, 600, 'plasma_metallurgy')
mr('exotic_solder', 'plasma_smelting', [('exotic_matter_crystal', 1)], [('superconducting_solder', 576), ('quark_gluon_plasma', 250)], [], [('exotic_solder', 576)], 65536, 400, 'plasma_metallurgy')
mr('spacetime_alloy', 'plasma_smelting', [('neutronium_ingot', 4), ('cosmic_string_segment', 1), ('exotic_matter_crystal', 2)], [('quark_gluon_plasma', 1000), ('exotic_matter', 1000)], [('spacetime_alloy_ingot', 4), ('spacetime_slag', 1)], [('dimensional_residue', 500)], 1048576, 1200, 'hyperdimensional_geometry', chances=[1.0, 0.25], min_tier=2)
mr('cosmic_neutronium', 'plasma_smelting', [('neutronium_block', 2), ('dark_matter_lens', 1), ('stellar_core', 1)], [('stellar_plasma', 2000), ('dark_matter', 2000)], [('cosmic_neutronium_ingot', 16), ('spacetime_slag', 2)], [('dimensional_residue', 1000)], 8388608, 2400, 'stellar_engineering', chances=[1.0, 0.5], catalysts=[], min_tier=3)
mr('stellar_plasma_from_core', 'plasma_smelting', [('stellar_core', 1)], [('quark_gluon_plasma', 2000)], [('stellar_core', 1)], [('stellar_plasma', 4000)], 4194304, 2400, 'plasma_metallurgy')
# --- fractional distillation of residue (GregTech distillation tower style)
mr('dimensional_residue', 'fractionation', [], [('dimensional_residue', 1000)], [('spacetime_slag', 1)], [('dark_matter', 400), ('exotic_matter', 250), ('inflaton_field', 100), ('chronon_gas', 200)], 524288, 600, 'fractional_distillation', chances=[0.3])
mr('warm_helium_fraction', 'fractionation', [], [('warm_helium', 4000)], [], [('superfluid_helium', 3800)], 65536, 400, 'fractional_distillation')
mr('slag_sorting', 'entropy_sorting', [('spacetime_slag', 4)], [('superfluid_helium', 250)], [('void_lattice_shard', 4), ('planck_shard', 1)], [('warm_helium', 250)], 65536, 400, 'thermodynamics_of_information', chances=[1.0, 0.05])
# --- catalysts: forged once, upgraded along the tiers
mr('higgs_catalyst', 'mass_modulation', [('higgs_field_capacitor', 4), ('magnetic_monopole', 2)], [('bose_einstein_condensate', 2000)], [('higgs_catalyst', 1)], [], 1048576, 2400, 'mass_manipulation')
mr('spacetime_catalyst', 'hyperdimensional', [('higgs_catalyst', 1), ('spacetime_alloy_ingot', 8), ('tesseract_core', 1), ('micro_singularity', 1)], [('exotic_matter', 4000), ('chronon_gas', 2000)], [('spacetime_catalyst', 1)], [('dimensional_residue', 1000)], 16777216, 4800, 'hyperdimensional_geometry')
mr('eternity_catalyst', 'reality_compilation', [('spacetime_catalyst', 1), ('closed_timelike_curve', 1), ('bootstrap_artifact', 1), ('cosmological_constant', 1)], [('causal_solder', 2304), ('dark_energy', 8000)], [('eternity_catalyst', 1)], [('dimensional_residue', 2000)], 268435456, 6000, 'simulation_hypothesis')

# --- deeper versions of earlier recipes (override by name)
mr('micro_singularity', 'kugelblitz', [('entangled_photon_pair', 16), ('metamaterial_sheet', 2)], [('quark_gluon_plasma', 1000)], [('micro_singularity', 1)], [('hawking_radiation', 250)], 524288, 1200, 'singularity_physics')
mr('tesseract_core', 'hyperdimensional', [('calabi_yau_manifold', 1), ('topological_processor', 2), ('exotic_matter_crystal', 4), ('white_hole_fragment', 1)], [('exotic_matter', 1000), ('chronon_gas', 500)], [('tesseract_core', 1)], [('dimensional_residue', 250)], 2097152, 1600, 'hyperdimensional_geometry')
mr('pocket_universe', 'universe_forging', [('universe_seed', 1), ('spin_foam_lattice', 4), ('cosmic_neutronium_ingot', 8)], [('false_vacuum', 4000), ('inflaton_field', 4000)], [('pocket_universe', 1)], [('dimensional_residue', 2000)], 33554432, 6000, 'cosmogenesis', catalysts=['spacetime_catalyst'])
mr('wormhole_throat', 'hyperdimensional', [('negative_mass_ingot', 4), ('exotic_matter_crystal', 4), ('white_hole_fragment', 1)], [('exotic_matter', 2000)], [('wormhole_throat', 1)], [('dimensional_residue', 500)], 4194304, 2000, 'traversable_wormholes', catalysts=['higgs_catalyst'])
mr('omega_point', 'reality_compilation', [('source_code_fragment', 4), ('pocket_universe', 1), ('ontological_processor', 2)], [('dark_energy', 8000), ('synaptic_fluid', 8000)], [('omega_point', 1)], [('dimensional_residue', 4000)], 134217728, 12000, 'simulation_hypothesis', catalysts=['eternity_catalyst'])
mr('axiom_key', 'reality_breach', [('absolute_infinity', 1), ('axiom_fragment', 16), ('transfinite_processor', 2)], [('dark_energy', 16000), ('causal_solder', 4608)], [('axiom_key', 1)], [], 268435456, 2400, 'reality_breach', catalysts=['eternity_catalyst'])
mr('absolute_infinity', 'transfinite_computation', [('aleph_null_shard', 8), ('godel_sentence', 1), ('russell_paradox_set', 1)], [('dark_energy', 4000)], [('absolute_infinity', 1)], [], 67108864, 2400, 'axiom_theory', catalysts=['banach_tarski_sphere'])
mr('source_code_fragment', 'reality_compilation', [('bekenstein_crystal', 1), ('cosmic_processor', 1), ('holographic_matrix', 1), ('boltzmann_brain', 1)], [('synaptic_fluid', 4000), ('dark_energy', 2000)], [('source_code_fragment', 2)], [('dimensional_residue', 1000)], 33554432, 4800, 'simulation_hypothesis')

# --- crafting


def shaped(name, pattern, key, result, count=1):
    w(os.path.join(REC, 'crafting', name + '.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
        'key': {k: ({'tag': v[1:]} if v.startswith('#') else {'item': q(v)}) for k, v in key.items()},
        'result': {'id': q(result), 'count': count}})


def shapeless(name, items, result, count=1):
    w(os.path.join(REC, 'crafting', name + '.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [({'tag': v[1:]} if v.startswith('#') else {'item': q(v)}) for v in items],
        'result': {'id': q(result), 'count': count}})


for kind, t in (('smelting', 200), ('blasting', 100)):
    w(os.path.join(REC, 'crafting', f'neutronium_ingot_{kind}.json'), {
        'type': f'minecraft:{kind}', 'category': 'misc', 'ingredient': {'item': q('degenerate_matter_chunk')},
        'result': {'id': q('neutronium_ingot')}, 'experience': 1.0, 'cookingtime': t * 2})
N, Qz, S = 'neutronium_ingot', 'cherenkov_quartz', 'spacetime_casing'
MACH_MB = {m['id']: m['multiblock'] for m in M}
MACHINE_RESEARCH_EARLY = {}
PL = 'neutronium_plate'
shaped('spacetime_casing', ['NQN', 'QEQ', 'NQN'], {'N': N, 'Q': Qz, 'E': 'minecraft:ender_pearl'}, S, 4)
shaped('spectral_analyzer', [' A ', 'NQN', ' N '], {'A': 'minecraft:amethyst_shard', 'N': N, 'Q': Qz}, 'spectral_analyzer')
shapeless('quantum_codex', ['minecraft:book', Qz], 'quantum_codex')
shaped('research_terminal', [' Q ', 'NRN', 'NSN'], {'Q': Qz, 'N': N, 'R': 'minecraft:redstone_block', 'S': S}, 'research_terminal')
shaped('interface_port', ['NHN', 'BSB', 'NRN'], {'N': N, 'H': 'minecraft:hopper', 'B': 'minecraft:bucket', 'S': S, 'R': 'minecraft:redstone'}, 'interface_port', 2)
shaped('neutronium_block', ['NNN', 'NNN', 'NNN'], {'N': N}, 'neutronium_block')
shapeless('neutronium_from_block', ['neutronium_block'], N, 9)
shaped('cherenkov_quartz_block', ['QQ', 'QQ'], {'Q': Qz}, 'cherenkov_quartz_block')
shapeless('cherenkov_from_block', ['cherenkov_quartz_block'], Qz, 4)
shaped('computronium_block', ['CCC', 'CCC', 'CCC'], {'C': 'computronium_ingot'}, 'computronium_block')
shapeless('computronium_from_block', ['computronium_block'], 'computronium_ingot', 9)
shaped('containment_field_glass', ['GGG', 'GQG', 'GGG'], {'G': '#c:glass_blocks', 'Q': Qz}, 'containment_field_glass', 8)
shaped('event_horizon_casing', ['PLP', 'LSL', 'PLP'], {'P': PL, 'L': 'metamaterial_sheet', 'S': S}, 'event_horizon_casing', 4)
shaped('hyperframe_casing', ['TQT', 'QSQ', 'TQT'], {'T': 'topological_insulator', 'Q': 'superposition_crystal', 'S': S}, 'hyperframe_casing', 4)
shaped('superconducting_coil_block', ['CCC', 'CSC', 'CCC'], {'C': 'superconducting_coil', 'S': S}, 'superconducting_coil_block', 2)
shaped('field_emitter', ['NQN', 'DSD', 'NQN'], {'N': PL, 'Q': Qz, 'D': 'quantum_dot', 'S': S}, 'field_emitter', 2)
shaped('rift_frame_casing', ['VGV', 'GHG', 'VGV'], {'V': 'void_lattice_shard', 'G': 'graviton_crystal', 'H': 'hyperframe_casing'}, 'rift_frame_casing', 4)
shaped('cosmogenic_casing', ['XFX', 'FRF', 'XFX'], {'X': 'multiverse_shard', 'F': 'spin_foam_lattice', 'R': 'rift_frame_casing'}, 'cosmogenic_casing', 4)
shaped('stellar_frame_casing', ['NHN', 'HEH', 'NHN'], {'N': PL, 'H': 'higgs_field_capacitor', 'E': 'event_horizon_casing'}, 'stellar_frame_casing', 4)
shaped('penrose_spindle', [' P ', 'CMC', ' P '], {'P': PL, 'C': 'superconducting_coil', 'M': 'magnetic_monopole'}, 'penrose_spindle')
shaped('brane_shifter', [' T ', 'AXA', ' N '], {'T': 'tesseract_core', 'A': 'alcubierre_coil', 'X': 'calabi_yau_manifold', 'N': N}, 'brane_shifter')
shaped('magnetic_confinement_casing', ['CDC', 'DSD', 'CDC'], {'C': 'superconducting_coil', 'D': 'dilithium_crystal', 'S': S}, 'magnetic_confinement_casing', 4)
shaped('chronoplastic_casing', ['TCT', 'CHC', 'TCT'], {'T': 'tachyon_crystal', 'C': 'chronon_capacitor', 'H': 'hyperframe_casing'}, 'chronoplastic_casing', 4)
shaped('wormhole_frame_casing', ['NEN', 'EHE', 'NEN'], {'N': 'negative_mass_ingot', 'E': 'exotic_matter_crystal', 'H': 'hyperframe_casing'}, 'wormhole_frame_casing', 4)
shaped('precision_assembly_casing', ['PXP', 'CSC', 'PXP'], {'P': PL, 'X': 'photonic_processor', 'C': 'femto_inductor', 'S': S}, 'precision_assembly_casing', 4)
shaped('plasma_crucible_casing', ['CMC', 'MSM', 'CMC'], {'C': 'chronosteel_ingot', 'M': 'magnetic_confinement_casing', 'S': 'superconducting_coil_block'}, 'plasma_crucible_casing', 4)
shaped('chronosteel_coil_block', ['CCC', 'CSC', 'CCC'], {'C': 'chronosteel_ingot', 'S': 'superconducting_coil_block'}, 'chronosteel_coil_block', 2)
shaped('fractionation_casing', ['PGP', 'GSG', 'PGP'], {'P': 'chronosteel_ingot', 'G': 'containment_field_glass', 'S': 'event_horizon_casing'}, 'fractionation_casing', 4)
shaped('lithography_glass', ['GDG', 'DGD', 'GDG'], {'G': 'containment_field_glass', 'D': 'quantum_dot'}, 'lithography_glass', 4)
shaped('photonic_lens', [' Q ', 'QMQ', ' Q '], {'Q': 'cherenkov_quartz_block', 'M': 'metamaterial_sheet'}, 'photonic_lens')
# multiblock tools
shaped('field_calibrator', [' QQ', ' NQ', 'N  '], {'Q': Qz, 'N': N}, 'field_calibrator')
shaped('structure_assembler', [' BC', ' NB', 'N  '], {'B': 'cherenkov_quartz_block', 'C': 'minecraft:ender_pearl', 'N': N}, 'structure_assembler')
complexity.recipes(mr, shaped)
mr('autonomous_calibrator', 'femto_assembly', [('field_calibrator', 1), ('superintelligence_core', 1), ('cosmic_processor', 2), ('atto_capacitor', 16), ('atto_transistor', 16), ('atto_inductor', 16)],
   [('causal_solder', 1152), ('superfluid_helium', 2000)], [('autonomous_calibrator', 1)], [], 16777216, 2400, 'superintelligence')
shaped('transfinite_casing', ['ALA', 'RXR', 'ALA'], {'A': 'aleph_null_shard', 'L': 'reality_lattice_casing', 'R': 'russell_paradox_set', 'X': 'banach_tarski_sphere'}, 'transfinite_casing', 4)
shaped('omnilex_casing', ['LHL', 'HTH', 'LHL'], {'L': 'law_crystal', 'H': 'arbor_heartwood', 'T': 'transfinite_casing'}, 'omnilex_casing', 4)
shapeless('arbor_heartwood_sample', ['arbor_heartwood'], 'arbor_heartwood_sample', 2)
shaped('reality_lattice_casing', ['CBC', 'BRB', 'CBC'], {'C': 'computronium_ingot', 'B': 'bekenstein_crystal', 'R': 'cosmogenic_casing'}, 'reality_lattice_casing', 4)
# ---- decorative blocks
def stonecut(name, src, out, n=1):
    w(os.path.join(REC, 'stonecutting', name + '.json'), {'type': 'minecraft:stonecutting', 'ingredient': {'item': q(src)}, 'result': {'id': q(out), 'count': n}})
    CRAFTING.append((name, [src], out))


import deco as _deco
for _fam, (_mat, _ingot, _c1, _c2, _vars) in _deco.FAMILIES.items():
    if _ingot:
        shaped(_mat, ['III', 'III', 'III'], {'I': _ingot}, _mat)
        shapeless(_mat + '_to_' + _ingot, [_mat], _ingot, 9)
for b in C['blocks']:
    if not b.get('deco') or 'material' not in b:
        continue
    bid, k = b['id'], b['kind']
    if k in ('deco', 'deco_pillar', 'glass'):
        if b['art'] == 'lamp':
            shaped(bid, [' G ', 'GMG', ' G '], {'G': 'minecraft:glowstone_dust', 'M': b['material']}, bid, 4)
        else:
            stonecut(bid, b['material'], bid)
            stonecut(bid + '_back', bid, b['material'])
    elif k == 'deco_stairs':
        shaped(bid, ['B  ', 'BB ', 'BBB'], {'B': b['base']}, bid, 4)
        stonecut(bid + '_cut', b['base'], bid)
    elif k == 'deco_slab':
        shaped(bid, ['BBB'], {'B': b['base']}, bid, 6)
        stonecut(bid + '_cut', b['base'], bid, 2)
    elif k == 'deco_wall':
        shaped(bid, ['BBB', 'BBB'], {'B': b['base']}, bid, 6)
        stonecut(bid + '_cut', b['base'], bid)

# machines
MC = {
    'casimir_press': (['NPN', 'QSQ', 'NNN'], {'N': N, 'P': 'minecraft:piston', 'Q': Qz, 'S': S}),
    'quantum_entangler': (['QAQ', 'ASA', 'NNN'], {'Q': Qz, 'A': 'minecraft:amethyst_shard', 'S': S, 'N': N}),
    'bose_einstein_condenser': (['CIC', 'ISI', 'NNN'], {'C': 'casimir_plate', 'I': 'minecraft:blue_ice', 'S': S, 'N': N}),
    'metamaterial_weaver': (['QDQ', 'DSD', 'NPN'], {'Q': Qz, 'D': 'quantum_dot', 'S': S, 'N': N, 'P': PL}),
    'tachyon_resonator': (['MTM', 'ESE', 'PCP'], {'M': 'metamaterial_sheet', 'T': 'cherenkov_quartz_block', 'E': 'entangled_photon_pair', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'antimatter_penning_trap': (['CSC', 'MXM', 'PCP'], {'C': 'superconducting_coil', 'S': S, 'M': 'metamaterial_sheet', 'X': 'qubit', 'P': PL}),
    'chronon_field_inverter': (['TCT', 'QSQ', 'PKP'], {'T': 'tachyon_crystal', 'C': 'minecraft:clock', 'Q': 'qubit', 'S': S, 'P': PL, 'K': 'superconducting_coil'}),
    'topological_transmuter': (['MXM', 'DSD', 'PCP'], {'M': 'metamaterial_sheet', 'X': 'qubit', 'D': 'quantum_dot', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'zero_point_extractor': (['CPC', 'CSC', 'NNN'], {'C': 'casimir_plate', 'P': PL, 'S': S, 'N': N}),
    'string_resonance_loom': (['XTX', 'MSM', 'PCP'], {'X': 'quantum_processor', 'T': 'tachyon_crystal', 'M': 'metamaterial_sheet', 'S': 'hyperframe_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'graviton_condenser': (['LXL', 'MSM', 'PCP'], {'L': 'dark_matter_lens', 'X': 'magnetic_monopole', 'M': 'metamaterial_sheet', 'S': 'hyperframe_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'neural_synthesizer': (['XQX', 'DSD', 'PCP'], {'X': 'quantum_processor', 'Q': 'superposition_crystal', 'D': 'quantum_dot', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'quark_gluon_collider': (['CKC', 'XSX', 'CKC'], {'C': 'superconducting_coil', 'K': 'superconducting_coil_block', 'X': 'quantum_processor', 'S': S}),
    'dark_matter_collector': (['MEM', 'XSX', 'MEM'], {'M': 'metamaterial_sheet', 'E': 'field_emitter', 'X': 'quantum_processor', 'S': 'event_horizon_casing'}),
    'kugelblitz_chamber': (['EAE', 'XSX', 'EAE'], {'E': 'entangled_photon_pair', 'A': 'metamaterial_sheet', 'X': 'quantum_processor', 'S': 'event_horizon_casing'}),
    'penrose_ergosphere': (['CPC', 'PSP', 'CPC'], {'C': 'superconducting_coil', 'P': 'penrose_spindle', 'S': 'event_horizon_casing'}),
    'white_hole_emitter': (['CTC', 'ZSZ', 'CTC'], {'C': 'chronon_capacitor', 'T': 'tachyon_crystal', 'Z': 'micro_singularity', 'S': 'event_horizon_casing'}),
    'tesseract_assembler': (['YWY', 'XSX', 'YWY'], {'Y': 'calabi_yau_manifold', 'W': 'white_hole_fragment', 'X': 'quantum_processor', 'S': 'hyperframe_casing'}),
    'multiverse_rift': (['MAM', 'TSG', 'MAM'], {'M': 'm_brane', 'A': 'rift_anchor', 'T': 'tesseract_core', 'S': 'rift_frame_casing', 'G': 'graviton_crystal'}),
    'universe_forge': (['BGB', 'TSU', 'BGB'], {'B': 'm_brane', 'G': 'graviton_crystal', 'T': 'tesseract_core', 'S': 'cosmogenic_casing', 'U': 'universe_seed'}),
    'matrioshka_brain': (['AQA', 'QSQ', 'AQA'], {'A': 'general_ai_core', 'Q': 'computronium_block', 'S': 'stellar_core'}),
    'dyson_array': (['TXT', 'XSX', 'TXT'], {'T': 'stellar_frame_casing', 'X': 'dyson_swarm_satellite', 'S': 'stellar_core'}),
    'maxwell_demon_sorter': (['QXQ', 'DSD', 'PCP'], {'Q': Qz, 'X': 'qubit', 'D': 'quantum_dot', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'nanite_assembler': (['MXM', 'DSD', 'PCP'], {'M': 'metamaterial_sheet', 'X': 'maxwell_demon_chip', 'D': 'quantum_dot', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'neutrino_condenser': (['QKQ', 'KSK', 'PCP'], {'Q': 'cherenkov_quartz_block', 'K': 'superconducting_coil_block', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'holographic_encoder': (['MXM', 'LSL', 'PCP'], {'M': 'metamaterial_sheet', 'X': 'quantum_processor', 'L': 'dark_matter_lens', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'higgs_field_modulator': (['HXH', 'MSM', 'PCP'], {'H': 'higgs_field_capacitor', 'X': 'sterile_neutrino', 'M': 'magnetic_monopole', 'S': 'hyperframe_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'psionic_resonator': (['CNC', 'XSX', 'PCP'], {'C': 'superposition_crystal', 'N': 'neural_lattice', 'X': 'narrow_ai_core', 'S': S, 'P': PL}),
    'annihilation_reactor': (['DAD', 'ASA', 'DAD'], {'D': 'dilithium_crystal', 'A': 'antimatter_pellet', 'S': 'magnetic_confinement_casing'}),
    'neutron_star_forge': (['NMN', 'QSQ', 'NMN'], {'N': 'neutronium_block', 'M': 'magnetic_monopole', 'Q': 'quantum_processor', 'S': 'magnetic_confinement_casing'}),
    'wormhole_projector': (['EWE', 'XSX', 'EWE'], {'E': 'exotic_matter_crystal', 'W': 'wormhole_throat', 'X': 'quantum_processor', 'S': 'wormhole_frame_casing'}),
    'causality_loop_engine': (['CTC', 'PSP', 'CTC'], {'C': 'chronon_capacitor', 'T': 'tachyon_crystal', 'P': 'pulsar_timing_crystal', 'S': 'chronoplastic_casing'}),
    'dark_energy_siphon': (['LFL', 'XSX', 'LFL'], {'L': 'dark_matter_lens', 'F': 'field_emitter', 'X': 'quantum_processor', 'S': 'event_horizon_casing'}),
    'cantor_diagonalizer': (['GAG', 'XSX', 'PCP'], {'G': 'source_code_fragment', 'A': 'superintelligence_core', 'X': 'quantum_processor', 'S': 'reality_lattice_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'axiom_distiller': (['GAG', 'DSD', 'PCP'], {'G': 'godel_sentence', 'A': 'aleph_null_shard', 'D': 'dark_matter_lens', 'S': 'reality_lattice_casing', 'P': PL, 'C': 'superconducting_coil'}),
    'axiomatic_breach_engine': (['IGI', 'RSR', 'IBI'], {'I': 'absolute_infinity', 'G': 'godel_sentence', 'R': 'reality_anchor', 'S': 'transfinite_casing', 'B': 'banach_tarski_sphere'}),
    'omnilex_engine': (['BTB', 'LSL', 'BOB'], {'B': 'bubble_universe_sample', 'T': 'arbor_heartwood', 'L': 'law_crystal', 'S': 'omnilex_casing', 'O': 'omega_point'}),
    'reality_compiler': (['ABA', 'HSH', 'AOA'], {'A': 'superintelligence_core', 'B': 'bekenstein_crystal', 'H': 'holographic_matrix', 'S': 'reality_lattice_casing', 'O': 'boltzmann_brain'}),
}
MC.update({
    'lattice_growth_vat': (['QBQ', 'GSG', 'NCN'], {'Q': Qz, 'B': 'minecraft:blue_ice', 'G': 'containment_field_glass', 'S': S, 'N': N, 'C': 'casimir_plate'}),
    'planck_cutter': (['DPD', 'QSQ', 'NNN'], {'D': 'minecraft:diamond', 'P': 'casimir_plate', 'Q': Qz, 'S': S, 'N': N}),
    'quantum_lithographer': (['GLG', 'DSD', 'NQN'], {'G': 'lithography_glass', 'L': 'photonic_lens', 'D': 'quantum_dot', 'S': S, 'N': N, 'Q': Qz}),
    'femto_fabricator': (['XMX', 'DSD', 'PCP'], {'X': 'photonic_processor', 'M': 'metamaterial_sheet', 'D': 'quantum_dot', 'S': S, 'P': PL, 'C': 'superconducting_coil'}),
    'femtoscale_assembly_line': (['XAX', 'CSC', 'XAX'], {'X': 'quantum_processor', 'A': 'precision_assembly_casing', 'C': 'superconducting_coil_block', 'S': 'femto_fabricator'}),
    'stellar_plasma_crucible': (['KXK', 'MSM', 'KXK'], {'K': 'chronosteel_coil_block', 'X': 'quantum_processor', 'M': 'magnetic_confinement_casing', 'S': 'plasma_crucible_casing'}),
    'fractionation_spire': (['FXF', 'GSG', 'FXF'], {'F': 'fractionation_casing', 'X': 'topological_processor', 'G': 'containment_field_glass', 'S': 'bose_einstein_condenser'}),
})
MC.update(complexity.MC)
MC.update(concepts.MC)
MC.update(round3.MC)
MC.update(round4.MC)
MC.update(round5.MC)
MC.update(round7.MC)
MC.update(round11.MC)
MC.update(round15.MC)
MC.update(round19.MC)
MC.update(round26.MC)
MC.update(round29.MC)
MC.update(round32_branches.MC)
MC.update(round34_anomaly.MC)
MC.update(round37.MC)
MC.update(round38.MC)
MC.update(round42.MC)
MC.update(round45.MC)
MC.update(round46.MC)
MC.update(round47.MC)
MC.update(round48.MC)
MC.update(round49.MC)
MC.update(round50.MC)
MC.update(round51.MC)
MC.update(round52.MC)
MC.update(round53.MC)
MC.update(round54.MC)
MC.update(round55.MC)
MC.update(round56.MC)
MC.update(round57.MC)
MC.update(round43.MC)
# Machine tiers: a tier-N machine needs a tier-(N-1) processor, like GregTech's circuit tiers.
TIER = {}
for m_, t_ in {1: ['casimir_press', 'quantum_entangler', 'bose_einstein_condenser', 'metamaterial_weaver', 'zero_point_extractor', 'maxwell_demon_sorter',
                   'lattice_growth_vat', 'planck_cutter', 'quantum_lithographer'],
               2: ['tachyon_resonator', 'antimatter_penning_trap', 'chronon_field_inverter', 'topological_transmuter', 'neural_synthesizer', 'nanite_assembler',
                   'neutrino_condenser', 'femto_fabricator', 'quark_gluon_collider', 'annihilation_reactor'],
               3: ['dark_matter_collector', 'kugelblitz_chamber', 'penrose_ergosphere', 'white_hole_emitter', 'holographic_encoder', 'higgs_field_modulator',
                   'psionic_resonator', 'stellar_plasma_crucible', 'femtoscale_assembly_line', 'fractionation_spire'],
               4: ['string_resonance_loom', 'graviton_condenser', 'tesseract_assembler', 'neutron_star_forge', 'wormhole_projector'],
               5: ['multiverse_rift', 'universe_forge', 'matrioshka_brain', 'dyson_array', 'causality_loop_engine', 'dark_energy_siphon'],
               6: ['reality_compiler', 'cantor_diagonalizer'],
               7: ['axiom_distiller', 'axiomatic_breach_engine'],
               8: ['omnilex_engine']}.items():
    for x_ in t_: TIER[x_] = m_
TIER.update(complexity.TIER)
TIER.update(concepts.TIER)
TIER.update(round3.TIER)
TIER.update(round4.TIER)
TIER.update(round5.TIER)
TIER.update(round7.TIER)
TIER.update(round11.TIER)
TIER.update(round15.TIER)
TIER.update(round19.TIER)
TIER.update(round26.TIER)
TIER.update(round29.TIER)
TIER.update(round32_branches.TIER)
TIER.update(round34_anomaly.TIER)
TIER.update(round37.TIER)
TIER.update(round38.TIER)
TIER.update(round42.TIER)
TIER.update(round45.TIER)
TIER.update(round46.TIER)
TIER.update(round47.TIER)
TIER.update(round48.TIER)
TIER.update(round49.TIER)
TIER.update(round50.TIER)
TIER.update(round51.TIER)
TIER.update(round52.TIER)
TIER.update(round53.TIER)
TIER.update(round54.TIER)
TIER.update(round55.TIER)
TIER.update(round56.TIER)
TIER.update(round57.TIER)
TIER.update(round43.TIER)
PROC = {1: 'photonic_processor', 2: 'quantum_processor', 3: 'topological_processor', 4: 'hyperdimensional_processor',
        5: 'cosmic_processor', 6: 'ontological_processor', 7: 'transfinite_processor'}
TIER_PLATE = {1: N, 2: PL, 3: 'chronosteel_ingot', 4: 'spacetime_alloy_ingot', 5: 'cosmic_neutronium_ingot', 6: 'cosmic_neutronium_ingot', 7: 'cosmic_neutronium_ingot', 8: 'cosmic_neutronium_ingot'}
ASSEMBLED = set()
for mid, (pat, key) in MC.items():
    tier = TIER[mid]
    key = dict(key)
    if tier >= 2:
        proc = PROC[tier - 1]
        procs = [k for k, v in key.items() if v.endswith('_processor')]
        if procs:
            for k in procs: key[k] = proc
        else:
            # no processor slot yet: the plate/ingot corners become processors
            cand = [k for k, v in key.items() if v in (PL, N, Qz)] or [k for k in key if k != 'S']
            key[cand[0]] = proc
        for k, v in key.items():
            if v in (PL, N) and tier >= 3: key[k] = TIER_PLATE[tier]
    if tier >= 4 and MACH_MB.get(mid):
        # big late-game controllers are built on the Femtoscale Assembly Line, not in a crafting grid
        counts = {}
        for row in pat:
            for ch in row:
                if ch != ' ': counts[key[ch]] = counts.get(key[ch], 0) + 1
        ins = sorted(counts.items(), key=lambda kv: -kv[1])[:6]
        ins = [(k_, n_ * 2) for k_, n_ in ins] + [(PROC[tier - 1], 4), ('atto_capacitor', 16 * (tier - 3)), ('atto_inductor', 16 * (tier - 3))]
        flu = [('exotic_solder' if tier < 5 else 'causal_solder', 1152 * (tier - 3)), ('superfluid_helium', 4000)]
        mr(mid, 'femto_assembly', ins[:9], flu, [(mid, 1)], [], 1048576 * 4 ** (tier - 4), 2400, MACHINE_RESEARCH_EARLY.get(mid))
        ASSEMBLED.add(mid)
    else:
        shaped(mid, pat, key, mid)
assert set(MC) == set(MACH), set(MACH) - set(MC)

concepts.recipes(mr, shaped, shapeless)

import extreme


def _remove_recipe(typ, name):
    f = os.path.join(REC, typ, name + '.json')
    if os.path.exists(f):
        os.remove(f)
    MACHINE_RECIPES[:] = [r for r in MACHINE_RECIPES if not (r['type'] == typ and r['name'] == name)]


extreme.recipes(w, REC, mr, shaped, _remove_recipe)
for _bid, _bname in extreme.BLOCK_LANG.items():
    w(os.path.join(A, 'models/block', _bid + '.json'), {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': f'{NS}:block/{_bid}_top', 'bottom': f'{NS}:block/{_bid}_bottom', 'side': f'{NS}:block/{_bid}_side'}})
    w(os.path.join(A, 'blockstates', _bid + '.json'), {'variants': {'': {'model': f'{NS}:block/{_bid}'}}})
    w(os.path.join(A, 'models/item', _bid + '.json'), {'parent': f'{NS}:block/{_bid}'})
    LANG[f'block.{NS}.{_bid}'] = _bname
LANG.update(extreme.LANG)

# =================================================================================== LOOT + TAGS
LT = os.path.join(D, 'loot_table')


def self_drop(bid):
    w(os.path.join(LT, 'blocks', bid + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': q(bid)}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


for b in C['blocks']:
    if b['kind'] == 'deco_slab':
        w(os.path.join(LT, 'blocks', b['id'] + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': q(b['id']),
            'functions': [{'function': 'minecraft:set_count', 'count': 2, 'conditions': [{'condition': 'minecraft:block_state_property', 'block': q(b['id']), 'properties': {'type': 'double'}}]},
                          {'function': 'minecraft:explosion_decay'}]}]}]})
        continue
    if b['kind'] == 'ore':
        w(os.path.join(LT, 'blocks', b['id'] + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:alternatives', 'children': [
            {'type': 'minecraft:item', 'name': q(b['id']), 'conditions': [{'condition': 'minecraft:match_tool', 'predicate': {'predicates': {'minecraft:enchantments': [{'enchantments': 'minecraft:silk_touch', 'levels': {'min': 1}}]}}}]},
            {'type': 'minecraft:item', 'name': q(b['drops']), 'functions': [{'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'}, {'function': 'minecraft:explosion_decay'}]}]}]}]})
    else:
        self_drop(b['id'])
for m in M:
    self_drop(m['id'])
w(os.path.join(LT, 'blocks', 'bubble_universe.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': q('bubble_universe_sample')}]}]})
self_drop('interface_port')
self_drop('research_terminal')
for _bid in extreme.BLOCK_LANG:
    self_drop(_bid)


def loot_chest(name, entries):
    w(os.path.join(LT, 'chests', name + '.json'), {'type': 'minecraft:chest', 'pools': [{'rolls': {'type': 'minecraft:uniform', 'min': 4, 'max': 8}, 'entries': [
        {'type': 'minecraft:item', 'name': q(i), 'weight': wt, 'functions': [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]} for i, wt, lo, hi in entries]}]})


loot_chest('observatory_ruin', [('cherenkov_quartz', 20, 2, 8), ('neutronium_ingot', 12, 1, 4), ('quantum_dot', 10, 1, 4), ('spectral_analyzer', 3, 1, 1),
                                ('quantum_codex', 4, 1, 1), ('casimir_plate', 6, 1, 2), ('entangled_photon_pair', 6, 1, 3), ('minecraft:amethyst_shard', 10, 2, 6)])
loot_chest('bulk_monolith', [('void_lattice_shard', 20, 2, 8), ('exotic_matter_crystal', 8, 1, 3), ('graviton_crystal', 4, 1, 2), ('multiverse_shard', 2, 1, 1),
                             ('tachyon_crystal', 8, 1, 3), ('calabi_yau_manifold', 2, 1, 1), ('cosmic_string_segment', 3, 1, 2)])

TG = os.path.join(D, '..')


def tag(registry, ns, name, values):
    # 1.1.0: adds to a tag another step already wrote, never replaces it (round 43's one-item write had wiped the whole
    # beyond_omnipotence tag, so the Omnipotence Core could make what lies beyond it)
    p = os.path.join(RES, 'data', ns, 'tags', registry, name + '.json')
    t = json.load(open(p)) if os.path.exists(p) else {'replace': False, 'values': []}
    for v in values:
        if v not in t['values']: t['values'].append(v)
    w(p, t)


all_blocks = [q(b['id']) for b in C['blocks']] + [q(m['id']) for m in M] + [q('interface_port'), q('research_terminal')] + [q(x) for x in extreme.BLOCK_LANG]
_axe = [q(b['id']) for b in C['blocks'] if b.get('deco') and b['tool'] == 'none' and b['id'].startswith('arbor')]
tag('block', 'minecraft', 'mineable/pickaxe', [x for x in all_blocks if x not in _axe])
tag('block', 'minecraft', 'mineable/axe', _axe)
for _kind, _tag in (('deco_stairs', 'stairs'), ('deco_slab', 'slabs'), ('deco_wall', 'walls')):
    for _reg in ('block', 'item'):
        tag(_reg, 'minecraft', _tag, [q(b['id']) for b in C['blocks'] if b['kind'] == _kind])
by_tool = {'stone': [], 'iron': [], 'diamond': []}
for b in C['blocks']:
    if b['tool'] in by_tool: by_tool[b['tool']].append(q(b['id']))
by_tool['iron'] += [q(m['id']) for m in M if not m['multiblock']] + [q('interface_port'), q('research_terminal')]
by_tool['diamond'] += [q(m['id']) for m in M if m['multiblock']] + [q(x) for x in extreme.BLOCK_LANG]
for t_, v in by_tool.items():
    tag('block', 'minecraft', f'needs_{t_}_tool', v)
ores = [b for b in C['blocks'] if b['kind'] == 'ore']
for reg in ('block', 'item'):
    tag(reg, 'c', 'ores', [q(b['id']) for b in ores])
    for b in ores:
        tag(reg, 'c', 'ores/' + b['drops'].replace('_chunk', '').replace('_shard', ''), [q(b['id'])])
tag('item', 'c', 'gems/cherenkov_quartz', [q('cherenkov_quartz')])
tag('item', 'c', 'gems', [q('cherenkov_quartz')])
tag('block', 'minecraft', 'dragon_immune', [q('neutronium_block'), q('event_horizon_casing')])
tag('block', NS, 'observable', [
    'minecraft:beacon', 'minecraft:end_portal_frame', 'minecraft:end_gateway', 'minecraft:budding_amethyst', 'minecraft:amethyst_cluster',
    'minecraft:sculk_shrieker', 'minecraft:sculk_catalyst', 'minecraft:respawn_anchor', 'minecraft:conduit', 'minecraft:crying_obsidian',
    'minecraft:lodestone', 'minecraft:reinforced_deepslate', 'minecraft:end_rod', 'minecraft:enchanting_table', 'minecraft:nether_portal',
    'minecraft:spawner', 'minecraft:trial_spawner', 'minecraft:vault', 'minecraft:ancient_debris', 'minecraft:dragon_egg', 'minecraft:magma_block',
    'minecraft:obsidian', 'minecraft:bedrock', 'minecraft:sea_lantern', 'minecraft:glowstone', 'minecraft:shroomlight', 'minecraft:chorus_flower',
    'minecraft:sculk_sensor', 'minecraft:calibrated_sculk_sensor', 'minecraft:lightning_rod', 'minecraft:tinted_glass', 'minecraft:heavy_core',
] + [q(b['id']) for b in C['blocks'] if b['kind'] in ('ore', 'simple', 'glass')])
tag('entity_type', NS, 'observable', ['minecraft:enderman', 'minecraft:endermite', 'minecraft:shulker', 'minecraft:warden', 'minecraft:ender_dragon',
                                      'minecraft:wither', 'minecraft:allay', 'minecraft:vex', 'minecraft:phantom', 'minecraft:elder_guardian',
                                      'minecraft:breeze', 'minecraft:blaze', 'minecraft:ghast', 'minecraft:end_crystal', 'minecraft:lightning_bolt'])

# =================================================================================== WORLDGEN
WG = os.path.join(D, 'worldgen')


def ore_feature(name, target, block, size, count, lo, hi):
    w(os.path.join(WG, 'configured_feature', name + '.json'), {'type': 'minecraft:ore', 'config': {'size': size, 'discard_chance_on_air_exposure': 0.2, 'targets': [{'target': target, 'state': {'Name': q(block)}}]}})
    w(os.path.join(WG, 'placed_feature', name + '.json'), {'feature': q(name), 'placement': [
        {'type': 'minecraft:count', 'count': count}, {'type': 'minecraft:in_square'},
        {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': lo}, 'max_inclusive': {'absolute': hi}}},
        {'type': 'minecraft:biome'}]})


ore_feature('ore_cherenkov_quartz', {'predicate_type': 'minecraft:tag_match', 'tag': 'minecraft:deepslate_ore_replaceables'}, 'deepslate_cherenkov_quartz_ore', 6, 5, -64, 0)
ore_feature('ore_degenerate_matter', {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:end_stone'}, 'degenerate_matter_ore', 5, 7, 10, 80)
ore_feature('ore_void_lattice', {'predicate_type': 'minecraft:block_match', 'block': q('bulk_stone')}, 'void_lattice_ore', 7, 12, 0, 128)
ore_feature('ore_dilithium', {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:netherrack'}, 'dilithium_ore', 5, 6, 10, 110)


def custom_feature(name, ftype, placement):
    w(os.path.join(WG, 'configured_feature', name + '.json'), {'type': q(ftype), 'config': {}})
    w(os.path.join(WG, 'placed_feature', name + '.json'), {'feature': q(name), 'placement': placement})


custom_feature('observatory_ruin', 'observatory_ruin', [{'type': 'minecraft:rarity_filter', 'chance': 260}, {'type': 'minecraft:in_square'},
                                                        {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])
custom_feature('bulk_monolith', 'bulk_monolith', [{'type': 'minecraft:rarity_filter', 'chance': 24}, {'type': 'minecraft:in_square'},
                                                  {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])
custom_feature('crystal_spire', 'crystal_spire', [{'type': 'minecraft:count', 'count': 4}, {'type': 'minecraft:in_square'},
                                                  {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}])

BM = os.path.join(D, 'neoforge/biome_modifier')
w(os.path.join(BM, 'overworld_ores.json'), {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld', 'features': q('ore_cherenkov_quartz'), 'step': 'underground_ores'})
w(os.path.join(BM, 'end_ores.json'), {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_end', 'features': q('ore_degenerate_matter'), 'step': 'underground_ores'})
w(os.path.join(BM, 'nether_ores.json'), {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_nether', 'features': q('ore_dilithium'), 'step': 'underground_ores'})
w(os.path.join(BM, 'observatory_ruins.json'), {'type': 'neoforge:add_features', 'biomes': '#minecraft:is_overworld', 'features': q('observatory_ruin'), 'step': 'surface_structures'})

feats = [[] for _ in range(11)]
feats[4].append(q('bulk_monolith'))     # surface_structures
feats[6].append(q('ore_void_lattice'))  # underground_ores
feats[9].append(q('crystal_spire'))     # vegetal_decoration
w(os.path.join(WG, 'biome', 'the_bulk.json'), {
    'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
    'effects': {'sky_color': 0x140828, 'fog_color': 0x0C0418, 'water_color': 0x7A3CFF, 'water_fog_color': 0x1A0830,
                'ambient_sound': 'minecraft:ambient.basalt_deltas.loop',
                'particle': {'options': {'type': 'minecraft:reverse_portal'}, 'probability': 0.004}},
    'spawners': {'monster': [{'type': 'minecraft:enderman', 'weight': 10, 'minCount': 1, 'maxCount': 2},
                             {'type': 'minecraft:endermite', 'weight': 4, 'minCount': 1, 'maxCount': 3}]},
    'spawn_costs': {}, 'carvers': {}, 'features': feats})
LANG['biome.theoreticalsciences.the_bulk'] = 'The Bulk'

import zipfile as _z
with _z.ZipFile(__import__("mcpaths").CLIENT_JAR) as z:
    end = json.loads(z.read('data/minecraft/worldgen/noise_settings/end.json'))
end['default_block'] = {'Name': q('bulk_stone')}
end['surface_rule'] = {'type': 'minecraft:block', 'result_state': {'Name': q('bulk_stone')}}
end['disable_mob_generation'] = False
w(os.path.join(WG, 'noise_settings', 'the_bulk.json'), end)
w(os.path.join(D, 'dimension_type', 'the_bulk.json'), {
    'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
    'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.12, 'logical_height': 256,
    'effects': q('the_bulk'), 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 256, 'fixed_time': 18000,
    'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
w(os.path.join(D, 'dimension', 'the_bulk.json'), {'type': q('the_bulk'), 'generator': {
    'type': 'minecraft:noise', 'settings': q('the_bulk'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('the_bulk')}}})

concepts_gen.worldgen(globals())

# ---- Hyperdimensional Reality Space: no ground at all - just the World Tree above an endless Ocean of Primordial Law
hr = json.loads(json.dumps(end))
hr['sea_level'] = 64
hr['default_block'] = {'Name': q('arbor_heartwood'), 'Properties': {'phase': '0'}}
hr['default_fluid'] = {'Name': q('primordial_law'), 'Properties': {'level': '0'}}
hr['aquifers_enabled'] = False
hr['ore_veins_enabled'] = False
hr['disable_mob_generation'] = True
hr['noise'] = {'min_y': 0, 'height': 512, 'size_horizontal': 2, 'size_vertical': 1}
hr['noise_router']['final_density'] = -1.0
hr['noise_router']['initial_density_without_jaggedness'] = -1.0
hr['surface_rule'] = {'type': 'minecraft:sequence', 'sequence': []}
hr['spawn_target'] = []
w(os.path.join(WG, 'noise_settings', 'hyperdimensional_reality.json'), hr)
w(os.path.join(WG, 'configured_feature', 'world_tree.json'), {'type': q('world_tree'), 'config': {}})
w(os.path.join(WG, 'placed_feature', 'world_tree.json'), {'feature': q('world_tree'), 'placement': []})
hfeats = [[] for _ in range(11)]
hfeats[4].append(q('world_tree'))
w(os.path.join(WG, 'biome', 'hyperreal_expanse.json'), {
    'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0,
    'effects': {'sky_color': 0x101830, 'fog_color': 0x0A0A1E, 'water_color': 0x80FFE0, 'water_fog_color': 0x1A0838,
                'ambient_sound': 'minecraft:ambient.warped_forest.loop',
                'particle': {'options': {'type': 'minecraft:enchant'}, 'probability': 0.01}},
    'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': hfeats})
LANG['biome.theoreticalsciences.hyperreal_expanse'] = 'Hyperreal Expanse'
w(os.path.join(D, 'dimension_type', 'hyperdimensional_reality.json'), {
    'ultrawarm': False, 'natural': False, 'piglin_safe': False, 'respawn_anchor_works': False, 'bed_works': False, 'has_raids': False,
    'has_skylight': False, 'has_ceiling': False, 'coordinate_scale': 1.0, 'ambient_light': 0.35, 'logical_height': 512,
    'effects': q('hyperdimensional_reality'), 'infiniburn': '#minecraft:infiniburn_end', 'min_y': 0, 'height': 512, 'fixed_time': 6000,
    'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0})
w(os.path.join(D, 'dimension', 'hyperdimensional_reality.json'), {'type': q('hyperdimensional_reality'), 'generator': {
    'type': 'minecraft:noise', 'settings': q('hyperdimensional_reality'), 'biome_source': {'type': 'minecraft:fixed', 'biome': q('hyperreal_expanse')}}})
w(os.path.join(D, 'advancement', 'hyperreal_arrival.json'), {
    'display': {'icon': {'id': q('axiom_key')}, 'title': 'Beyond Reality', 'description': 'Breach every law of physics and mathematics and enter Hyperdimensional Reality Space',
                'frame': 'challenge', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
    'parent': 'minecraft:end/root',
    'criteria': {'arrived': {'trigger': 'minecraft:changed_dimension', 'conditions': {'to': q('hyperdimensional_reality')}}},
    'requirements': [['arrived']]})
w(os.path.join(D, 'advancement', 'omnilex.json'), {
    'display': {'icon': {'id': q('omnilex')}, 'title': 'Omnilex', 'description': 'Simulate - and so control - the World Tree and the Ocean of Primordial Law',
                'frame': 'challenge', 'show_toast': True, 'announce_to_chat': True, 'hidden': False},
    'parent': q('hyperreal_arrival'),
    'criteria': {'has': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': q('omnilex')}]}}},
    'requirements': [['has']]})

# =================================================================================== KUBEJS SCHEMAS
# round 31: the optional machine-recipe fields (see QHRecipeSerializer). 'default_optional' = left out of the JSON unless set.
KJS_EXTRA_KEYS = [
    {'name': 'min_tier', 'role': 'other', 'type': {'type': 'int', 'min': 0}, 'optional': 0, 'function_names': ['minTier', 'min_tier', 'tier']},
    {'name': 'optional_inputs', 'role': 'input', 'default_optional': True, 'function_names': ['optionalInputs', 'optional_inputs'],
     'type': {'type': 'list', 'component': {'type': 'custom_object', 'keys': [
         {'name': 'item', 'component': 'string'}, {'name': 'count', 'component': 'positive_int', 'optional': True}]}}},
    {'name': 'optional_fluid_inputs', 'role': 'input', 'default_optional': True, 'function_names': ['optionalFluidInputs', 'optional_fluid_inputs'],
     'type': {'type': 'list', 'component': {'type': 'custom_object', 'keys': [
         {'name': 'fluid', 'component': 'string'}, {'name': 'amount', 'component': 'positive_int', 'optional': True}]}}},
    {'name': 'physics', 'role': 'other', 'default_optional': True, 'function_names': ['physics'],
     'type': {'type': 'custom_object', 'keys': [
         {'name': 'vars', 'component': {'type': 'map', 'key': 'string', 'component': 'double'}, 'optional': True},
         {'name': 'windows', 'component': {'type': 'list', 'component': {'type': 'custom_object', 'keys': [
             {'name': 'q', 'component': 'string'}, {'name': 'min', 'component': 'double', 'optional': True},
             {'name': 'max', 'component': 'double', 'optional': True}]}}, 'optional': True}]}},
    # round 35: the conditions around the machine (field/Environment.java), dimension and biome included
    {'name': 'environment', 'role': 'other', 'default_optional': True, 'function_names': ['environment', 'env'],
     'type': {'type': 'custom_object', 'keys': [
         {'name': 'gravity_min', 'component': 'double', 'optional': True},
         {'name': 'gravity_max', 'component': 'double', 'optional': True},
         {'name': 'dimensions_min', 'component': 'int', 'optional': True},
         {'name': 'time_min', 'component': 'double', 'optional': True},
         {'name': 'time_max', 'component': 'double', 'optional': True},
         {'name': 'vacuum_min', 'component': 'int', 'optional': True},
         {'name': 'dimensions', 'component': {'type': 'list', 'component': 'string'}, 'optional': True},
         {'name': 'biomes', 'component': {'type': 'list', 'component': 'string'}, 'optional': True},
         {'name': 'min_y', 'component': 'int', 'optional': True}]}},          # 1.1.0: the Interstellar Scoop's height (KubeJS warned it away)
    {'name': 'scaled', 'role': 'other', 'type': 'double', 'default_optional': True, 'function_names': ['scaled']},
    {'name': 'secret', 'role': 'other', 'type': 'boolean', 'default_optional': True, 'function_names': ['secret']},
    {'name': 'insight_per_run', 'role': 'other', 'type': {'type': 'int', 'min': 0, 'max': 1000000}, 'default_optional': True,
     'function_names': ['insightPerRun', 'insight_per_run', 'insight']},
]
for m in M:
    keys = []
    if m['outputs']:
        keys.append({'name': 'item_outputs', 'role': 'output', 'type': {'type': 'list', 'component': 'item_stack'}, 'optional': []})
    if m['fluid_outputs']:
        keys.append({'name': 'fluid_outputs', 'role': 'output', 'type': {'type': 'list', 'component': 'fluid_stack'}, 'optional': []})
    if m['inputs']:
        keys.append({'name': 'item_inputs', 'role': 'input', 'type': {'type': 'list', 'component': 'flat_sized_ingredient'}, 'optional': []})
    if m['fluid_inputs']:
        keys.append({'name': 'fluid_inputs', 'role': 'input', 'type': {'type': 'list', 'component': 'flat_sized_fluid_ingredient'}, 'optional': []})
    keys.append({'name': 'energy', 'role': 'other', 'type': {'type': 'int', 'min': 0}, 'optional': 256, 'function_names': ['energy', 'fe']})
    keys.append({'name': 'duration', 'role': 'other', 'type': {'type': 'int', 'min': 1}, 'optional': 200, 'function_names': ['duration', 'time']})
    keys.append({'name': 'research', 'role': 'other', 'type': 'optional_string', 'optional': '', 'function_names': ['research']})
    keys.append({'name': 'catalysts', 'role': 'input', 'type': {'type': 'list', 'component': 'ingredient'}, 'optional': [], 'function_names': ['catalysts', 'catalyst']})
    keys.append({'name': 'chances', 'role': 'other', 'type': {'type': 'list', 'component': 'float'}, 'optional': [], 'function_names': ['chances']})
    # round 31: every other field the recipe JSON understands, so scripts can set them too
    # (.minTier(6) .optionalInputs([{item: 'mekanism:alloy_atomic', count: 4}]) .physics({vars: {...}, windows: [...]}) ...)
    keys.extend(KJS_EXTRA_KEYS)
    ctor_keys = [k['name'] for k in keys if k['name'] in ('item_outputs', 'fluid_outputs', 'item_inputs', 'fluid_inputs')]
    schema = {'keys': keys, 'constructors': [{'keys': ctor_keys}]}
    w(os.path.join(D, 'kubejs/recipe_schema', m['recipe'] + '.json'), schema)

# round 31: the mod's other recipe types (Extreme Assembly Table, Singularity Compactor, Convergence Altar)
_RES = {'name': 'result', 'role': 'output', 'type': 'item_stack'}
w(os.path.join(D, 'kubejs/recipe_schema', 'extreme_shaped.json'), {'keys': [
    _RES, {'name': 'pattern', 'role': 'other', 'type': {'type': 'list', 'component': 'string'}},
    {'name': 'key', 'role': 'input', 'type': {'type': 'pattern', 'component': 'ingredient'}}],
    'constructors': [{'keys': ['result', 'pattern', 'key']}]})
w(os.path.join(D, 'kubejs/recipe_schema', 'extreme_shapeless.json'), {'keys': [
    _RES, {'name': 'ingredients', 'role': 'input', 'type': {'type': 'list', 'component': 'ingredient'}}],
    'constructors': [{'keys': ['result', 'ingredients']}]})
w(os.path.join(D, 'kubejs/recipe_schema', 'compaction.json'), {'keys': [
    _RES, {'name': 'ingredient', 'role': 'input', 'type': 'ingredient'},
    {'name': 'count', 'role': 'other', 'type': 'positive_int', 'function_names': ['count']}],
    'constructors': [{'keys': ['result', 'ingredient', 'count']}]})
w(os.path.join(D, 'kubejs/recipe_schema', 'convergence.json'), {'keys': [
    _RES, {'name': 'core', 'role': 'input', 'type': 'ingredient'},
    {'name': 'pedestals', 'role': 'input', 'type': {'type': 'list', 'component': 'ingredient'}},
    {'name': 'energy', 'role': 'other', 'type': {'type': 'long', 'min': 0}, 'optional': 0, 'function_names': ['energy', 'fe']},
    {'name': 'duration', 'role': 'other', 'type': {'type': 'int', 'min': 1}, 'optional': 200, 'function_names': ['duration', 'time']},
    {'name': 'tier', 'role': 'other', 'type': {'type': 'int', 'min': 1, 'max': 3}, 'optional': 1, 'function_names': ['tier']},
    {'name': 'research', 'role': 'other', 'type': 'optional_string', 'optional': '', 'function_names': ['research']}],
    'constructors': [{'keys': ['result', 'core', 'pedestals']}]})

# =================================================================================== PATCHOULI BOOK
BOOK = 'codex'
w(os.path.join(D, 'patchouli_books', BOOK, 'book.json'), {
    'name': 'Quantum Codex', 'landing_text': 'Field notes on engineering at the edge of theoretical physics.$(br2)Begin by crafting a $(item)Spectral Analyzer$() and observing strange phenomena to earn $(thing)Insight$(), then spend it at a $(item)Research Terminal$().',
    'version': 1, 'use_resource_pack': True, 'custom_book_item': q('quantum_codex'), 'show_progress': True,
    'book_texture': 'patchouli:textures/gui/book_blue.png', 'filler_texture': 'patchouli:textures/gui/page_filler.png',
    'text_color': '1A1A2A', 'header_color': '10306A', 'nameplate_color': '40C0FF', 'link_color': '6A2AB0', 'progress_bar_color': '40C0FF',
    'i18n': False})
BA = os.path.join(A, 'patchouli_books', BOOK, 'en_us')
CATS = [
    ('basics', 'Getting Started', 'Insight, research, power and the first machines.', 'spectral_analyzer', 0),
    ('quantum', 'Quantum Engineering', 'Entanglement, vacuum energy, cryogenics and metamaterials.', 'qubit', 1),
    ('exotic', 'Exotic Matter', 'Tachyons, antimatter, chronons, colliders and the dark sector.', 'antimatter_pellet', 2),
    ('singularities', 'Singularities', 'Black holes made of light, white holes and the Penrose process.', 'micro_singularity', 3),
    ('hyperdimensions', 'Hyperdimensions', 'Tesseracts, Calabi-Yau manifolds and the Bulk.', 'tesseract_core', 4),
    ('strings', 'Strings & the Multiverse', 'String theory, quantum gravity, multiverse rifts and the forging of universes.', 'pocket_universe', 5),
    ('minds', 'Artificial Minds', 'From photonic neurons to the Matrioshka Brain.', 'superintelligence_core', 6),
    ('stellar', 'Stellar Engineering', 'Captive stars, Dyson swarms and Dyson spheres.', 'stellar_core', 7),
    ('world', 'The World', 'Ores, ruins and The Bulk.', 'void_lattice_ore', 8),
    ('information', 'Information & Entropy', "Maxwell's demon, negentropy and the holographic principle.", 'maxwell_demon_chip', 9),
    ('nanotech', 'Nanotech & Mass', 'Self-replicating machines, programmable matter, massless and negative-mass alloys.', 'nanite_swarm', 10),
    ('cosmic', 'Compact Stars & Annihilation', 'Neutrinos, neutron stars, quark stars and matter-antimatter power.', 'quark_star_core', 11),
    ('spacetime', 'Wormholes & Causality', 'Traversable wormholes, closed timelike curves and dark energy.', 'wormhole_throat', 12),
    ('simulation', 'Beyond Reality', 'The simulation hypothesis, its source code and the Omega Point.', 'omega_point', 13),
    ('fabrication', 'Fabrication & Processors', 'Wafers, lithography, femtoscale components, the seven processor tiers, plasma metallurgy and distillation.', 'quantum_processor', 1),
    ('endgame', 'Beyond Mathematics', 'Infinities, paradoxes, axioms - the breach out of reality, and what lies beyond it.', 'axiom_key', 14),
]
CATS += concepts.CATS
CATS += [('chemistry', 'Chemistry', 'Bonds, catalysts, nanomaterials, exotic atoms and the island of stability.', 'chemical_reactor', 15),
         ('biology', 'Biology', 'Genomes, quantum biology, synthetic and alien life, evolution and the origin of life.', 'bioreactor', 16),
         ('antimatter', 'Antimatter', 'CPT symmetry, the Antiverse, antiatoms, mirror metals and why there is anything at all.', 'cpt_mirror', 17),
         round12.CATEGORY]
CATS += round37.CATS
import round46
CATS += round46.CATS
for cid, name, desc, icon, sort in CATS:
    w(os.path.join(BA, 'categories', cid + '.json'), {'name': name, 'description': desc, 'icon': q(icon), 'sortnum': sort})

MACHINE_CAT = {'quantum_entangler': 'quantum', 'casimir_press': 'quantum', 'bose_einstein_condenser': 'quantum', 'metamaterial_weaver': 'quantum',
               'topological_transmuter': 'quantum', 'zero_point_extractor': 'basics', 'tachyon_resonator': 'exotic', 'antimatter_penning_trap': 'exotic',
               'chronon_field_inverter': 'exotic', 'quark_gluon_collider': 'exotic', 'dark_matter_collector': 'exotic', 'kugelblitz_chamber': 'singularities',
               'penrose_ergosphere': 'singularities', 'white_hole_emitter': 'singularities', 'tesseract_assembler': 'hyperdimensions',
               'string_resonance_loom': 'strings', 'graviton_condenser': 'strings', 'multiverse_rift': 'strings', 'universe_forge': 'strings',
               'neural_synthesizer': 'minds', 'matrioshka_brain': 'minds', 'dyson_array': 'stellar'}
MACHINE_CAT.update({'maxwell_demon_sorter': 'information', 'holographic_encoder': 'information', 'psionic_resonator': 'minds',
                    'nanite_assembler': 'nanotech', 'higgs_field_modulator': 'nanotech', 'neutrino_condenser': 'cosmic',
                    'annihilation_reactor': 'cosmic', 'neutron_star_forge': 'cosmic', 'wormhole_projector': 'spacetime',
                    'causality_loop_engine': 'spacetime', 'dark_energy_siphon': 'spacetime', 'reality_compiler': 'simulation',
                    'lattice_growth_vat': 'fabrication', 'planck_cutter': 'fabrication', 'quantum_lithographer': 'fabrication', 'femto_fabricator': 'fabrication',
                    'femtoscale_assembly_line': 'fabrication', 'stellar_plasma_crucible': 'fabrication', 'fractionation_spire': 'fabrication',
                    'cantor_diagonalizer': 'endgame', 'axiom_distiller': 'endgame', 'axiomatic_breach_engine': 'endgame', 'omnilex_engine': 'endgame'})
MACHINE_RESEARCH = {'quantum_entangler': 'quantum_entanglement', 'casimir_press': None, 'bose_einstein_condenser': 'cryogenics',
                    'metamaterial_weaver': 'metamaterials', 'topological_transmuter': 'topology', 'zero_point_extractor': 'casimir_effect',
                    'tachyon_resonator': 'tachyonics', 'antimatter_penning_trap': 'antimatter', 'chronon_field_inverter': 'chronodynamics',
                    'quark_gluon_collider': 'particle_physics', 'dark_matter_collector': 'dark_sector', 'kugelblitz_chamber': 'singularity_physics',
                    'penrose_ergosphere': 'penrose_process', 'white_hole_emitter': 'white_holes', 'tesseract_assembler': 'hyperdimensional_geometry',
                    'string_resonance_loom': 'string_theory', 'graviton_condenser': 'quantum_gravity', 'multiverse_rift': 'multiverse',
                    'universe_forge': 'cosmogenesis', 'neural_synthesizer': 'artificial_intelligence', 'matrioshka_brain': 'matrioshka_brain',
                    'dyson_array': 'dyson_swarm'}
MACHINE_RESEARCH.update({'maxwell_demon_sorter': 'thermodynamics_of_information', 'holographic_encoder': 'holographic_principle',
                         'psionic_resonator': 'psionics', 'nanite_assembler': 'nanotechnology', 'higgs_field_modulator': 'mass_manipulation',
                         'neutrino_condenser': 'neutrino_physics', 'annihilation_reactor': 'annihilation_power', 'neutron_star_forge': 'compact_stars',
                         'wormhole_projector': 'traversable_wormholes', 'causality_loop_engine': 'causality_violation',
                         'dark_energy_siphon': 'dark_energy', 'reality_compiler': 'simulation_hypothesis',
                         'lattice_growth_vat': 'precision_lithography', 'planck_cutter': 'precision_lithography', 'quantum_lithographer': 'precision_lithography',
                         'femto_fabricator': 'femtoengineering', 'femtoscale_assembly_line': 'assembly_lines', 'stellar_plasma_crucible': 'plasma_metallurgy',
                         'fractionation_spire': 'fractional_distillation',
                         'cantor_diagonalizer': 'transfinite_mathematics', 'axiom_distiller': 'axiom_theory',
                         'axiomatic_breach_engine': 'reality_breach', 'omnilex_engine': 'omnilex'})
EXTRA = {
    'penrose_ergosphere': 'A $(item)Micro Singularity$() burns for twenty minutes at 65,536 FE/t. Energy leaves through the controller and any $(item)Interface Port$().',
    'dyson_array': 'Put a $(item)Stellar Core$() in the first slot. Feed $(item)Dyson Swarm Satellites$() (up to 500) into the second slot - or leave a $(item)Von Neumann Probe$() there and it will build the swarm by itself and $(item)Dyson Shell Segments$() (64 close the sphere; you need 100 satellites before shells can be mounted) into the third.$(br2)Output = base x satellites/50 x (1 + shells/16), and doubled again once the sphere is complete.',
    'matrioshka_brain': 'Every computation the brain finishes grants its owner $(thing)5 Insight$() - the brain does your science for you.',
    'multiverse_rift': 'Each harvest pulls matter from universes whose physical constants differ slightly from ours, including $(item)Multiverse Shards$() and $(fluid)Inflaton Field$().',
    'universe_forge': 'First forge a $(item)Pocket Universe$() from a $(item)Universe Seed$(), $(fluid)False Vacuum$() and $(fluid)Inflaton Field$(). Then feed it back in to harvest it until only a $(item)Heat Death Remnant$() remains. Four remnants can be re-inflated into a new seed.',
    'kugelblitz_chamber': 'While running, the singularity at its heart bends light around it (gravitational lensing) and pulls in loose items and creatures. Keep your distance.',
    'causality_loop_engine': 'Feed an item together with a $(item)Closed Timelike Curve$() and both come back - plus a second copy of the item. Works on Tesseract Cores, singularities, white hole fragments, superintelligences, stellar cores, wormhole throats and even pocket universes.',
    'annihilation_reactor': 'Burns $(item)Antimatter Pellets$() (with a $(item)Dilithium Crystal$() as regulator) at 131,072 FE/t, or a full $(item)Annihilation Core$() at 524,288 FE/t. Needs superfluid helium cooling.',
    'reality_compiler': 'The final machine. Decompile reality into $(item)Source Code Fragments$(), forge $(item)Reality Anchors$(), and finally compile the $(item)Omega Point$() - which a Zero-Point Extractor can burn for 10 hours.',
    'axiomatic_breach_engine': 'Forge an $(item)Axiom Key$() here - it is reusable, and takes you to Hyperdimensional Reality Space and back. Or run $(thing)Open Breach$() and, while the engine runs, simply walk into its centre.$(br2)Inside, reality shatters into shards that each obey different laws.',
    'omnilex_engine': 'This engine will only run in $(thing)Hyperdimensional Reality Space$(), and is built partly from the World Tree itself. It simulates the $(thing)Tree$() and the $(thing)Ocean$() - first separately as $(item)Simulacra$(), then together - and whoever holds the resulting $(item)Omnilex$() is no longer bound by gravity.',
    'cantor_diagonalizer': 'Takes the computational power of a superintelligence and points it at infinity. $(item)Banach-Tarski Spheres$() can be split and reassembled into two.',
    'wormhole_projector': 'Inside the ring you can see the other end of the bridge. Each transit drags back matter from a distant star system or a neighbouring brane.',
}

MACHINE_CAT.update(complexity.MACHINE_CAT)
MACHINE_CAT.update(concepts.MACHINE_CAT)
MACHINE_CAT.update(round3.MACHINE_CAT)
MACHINE_CAT.update(round4.MACHINE_CAT)
MACHINE_CAT.update(round5.MACHINE_CAT)
MACHINE_CAT.update(round7.MACHINE_CAT)
MACHINE_CAT.update(round11.MACHINE_CAT)
MACHINE_CAT.update(round15.MACHINE_CAT)
MACHINE_CAT.update(round19.MACHINE_CAT)
MACHINE_CAT.update(round26.MACHINE_CAT)
MACHINE_CAT.update(round29.MACHINE_CAT)
MACHINE_CAT.update(round32_branches.MACHINE_CAT)
MACHINE_CAT.update(round34_anomaly.MACHINE_CAT)
MACHINE_CAT.update(round37.MACHINE_CAT)
MACHINE_CAT.update(round38.MACHINE_CAT)
MACHINE_CAT.update(round42.MACHINE_CAT)
MACHINE_CAT.update(round45.MACHINE_CAT)
MACHINE_CAT.update(round46.MACHINE_CAT)
MACHINE_CAT.update(round47.MACHINE_CAT)
MACHINE_CAT.update(round48.MACHINE_CAT)
MACHINE_CAT.update(round49.MACHINE_CAT)
MACHINE_CAT.update(round50.MACHINE_CAT)
MACHINE_CAT.update(round51.MACHINE_CAT)
MACHINE_CAT.update(round52.MACHINE_CAT)
MACHINE_CAT.update(round53.MACHINE_CAT)
MACHINE_CAT.update(round54.MACHINE_CAT)
MACHINE_CAT.update(round55.MACHINE_CAT)
MACHINE_CAT.update(round56.MACHINE_CAT)
MACHINE_CAT.update(round57.MACHINE_CAT)
MACHINE_CAT.update(round43.MACHINE_CAT)
MACHINE_RESEARCH.update(concepts.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round3.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round4.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round5.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round7.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round11.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round15.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round19.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round26.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round29.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round32_branches.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round34_anomaly.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round37.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round38.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round42.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round45.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round46.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round47.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round48.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round49.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round50.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round51.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round52.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round53.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round54.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round55.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round56.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round57.MACHINE_RESEARCH)
MACHINE_RESEARCH.update(round43.MACHINE_RESEARCH)
EXTRA.update(concepts.EXTRA)
EXTRA.update(round3.EXTRA)
EXTRA.update(round4.EXTRA)
EXTRA.update(round5.EXTRA)
EXTRA.update(round7.EXTRA)
EXTRA.update(round11.EXTRA)
EXTRA.update(round15.EXTRA)
EXTRA.update(round19.EXTRA)
EXTRA.update(round26.EXTRA)
EXTRA.update(round29.EXTRA)
EXTRA.update(round32_branches.EXTRA)
EXTRA.update(round34_anomaly.EXTRA)
EXTRA.update(round37.EXTRA)
EXTRA.update(round38.EXTRA)
EXTRA.update(round42.EXTRA)
EXTRA.update(round45.EXTRA)
EXTRA.update(round46.EXTRA)
EXTRA.update(round47.EXTRA)
EXTRA.update(round48.EXTRA)
EXTRA.update(round49.EXTRA)
EXTRA.update(round50.EXTRA)
EXTRA.update(round51.EXTRA)
EXTRA.update(round52.EXTRA)
EXTRA.update(round53.EXTRA)
EXTRA.update(round54.EXTRA)
EXTRA.update(round55.EXTRA)
EXTRA.update(round56.EXTRA)
EXTRA.update(round57.EXTRA)
EXTRA.update(round43.EXTRA)
MACHINE_RESEARCH.update(complexity.MACHINE_RESEARCH)
EXTRA.update(complexity.EXTRA)


def multiblock_page(m):
    sh = SHAPES[m['mb_id']]
    layers = sh['layers']
    width = max(len(r) for l in layers for r in l)
    mapping = {'0': q(m['id'])}
    chars = {}
    nxt = iter('ABDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz')
    pattern = []
    for layer in reversed(layers):     # patchouli wants top layer first
        rows = []
        for row in layer:
            out = ''
            for ch in row.ljust(width):
                if ch == 'C':
                    out += '0'
                elif ch == '.':
                    out += ' '
                elif ch == ' ':
                    out += '_'
                else:
                    if ch not in chars:
                        chars[ch] = next(nxt)
                        opts = KEY_BASE[ch]
                        if len(opts) > 1:
                            # Patchouli cycles through a tag's blocks, so ports / higher tiers show up in the preview
                            tname = 'multiblock/' + opts[0] + ('_or_port' if 'interface_port' in opts else '_or_better')
                            tag('block', NS, tname, [q(o) for o in opts])
                            mapping[chars[ch]] = '#' + q(tname)
                        else:
                            mapping[chars[ch]] = q(opts[0])
                    out += chars[ch]
            rows.append(out)
        pattern.append(rows)
    return {'type': 'patchouli:multiblock', 'name': MACHINE_TEXT[m['id']][0], 'multiblock': {'pattern': pattern, 'mapping': mapping},
            'enable_visualize': True, 'text': 'Cycling blocks: any of them fits (Interface Ports, higher coil/emitter tiers). Rotate, zoom and view layers in $(thing)JEI$().'}


for i, m in enumerate(M):
    name, lore = MACHINE_TEXT[m['id']]
    pages = [{'type': 'patchouli:spotlight', 'item': q(m['id']), 'title': name, 'text': lore + ('$(br2)Generates power.' if m['generator'] else '')
              + f"$(br2)$(thing)Tier {TIER[m['id']]}$() machine."}]
    if m['id'] in ASSEMBLED:
        pages.append({'type': 'patchouli:text', 'text': 'This controller is too complex for a crafting grid. Build it on the $(item)Femtoscale Assembly Line$() - check JEI for the exact parts.'})
    else:
        pages.append({'type': 'patchouli:crafting', 'recipe': f'{NS}:crafting/{m["id"]}'})
    if m['id'] in EXTRA:
        pages.append({'type': 'patchouli:text', 'text': EXTRA[m['id']]})
    if m['multiblock']:
        pages.append(multiblock_page(m))
    entry = {'name': name, 'icon': q(m['id']), 'category': f'{NS}:{MACHINE_CAT[m["id"]]}', 'sortnum': i, 'pages': pages}
    if MACHINE_RESEARCH[m['id']]:
        entry['advancement'] = f'{NS}:research/{MACHINE_RESEARCH[m["id"]]}'
    w(os.path.join(BA, 'entries', MACHINE_CAT[m['id']], m['id'] + '.json'), entry)


def text_entry(cat, eid, name, icon, texts, adv=None, sort=0, spot=None):
    pages = []
    if spot:
        pages.append({'type': 'patchouli:spotlight', 'item': ','.join(q(s) for s in spot), 'text': texts[0]})
        texts = texts[1:]
    pages += [{'type': 'patchouli:text', 'text': t} for t in texts]
    e = {'name': name, 'icon': q(icon), 'category': f'{NS}:{cat}', 'sortnum': sort, 'pages': pages}
    if adv: e['advancement'] = f'{NS}:research/{adv}'
    w(os.path.join(BA, 'entries', cat, eid + '.json'), e)


text_entry('basics', 'insight', 'Insight & Observation', 'spectral_analyzer', [
    'Science starts with looking. Use a $(item)Spectral Analyzer$() on unusual blocks (beacons, end portal frames, amethyst, sculk, Cherenkov ore...), on creatures like Endermen and Wardens, and on $(thing)running$() Theoretical Sciences machines.',
    'Every $(thing)new$() observation grants $(thing)Insight$(). Sneak-use the analyzer to observe the sky of the dimension you are in. Multiblocks are worth the most.'], sort=-2, spot=['spectral_analyzer'])
text_entry('basics', 'research', 'The Research Matrix', 'research_terminal', [
    'The $(item)Research Terminal$() opens the research tree. Each node costs Insight and sometimes materials, and needs its prerequisites.',
    'Machines will refuse to run recipes you have not researched. The machine checks the research of whoever placed it. Pack makers can turn this off in the config, edit entries in $(thing)data/*/ts_research$(), or grant research with $(thing)/theoreticalsciences research grant$().',
    'Sneak-use this Codex to open the research tree from anywhere.'], sort=-1, spot=['research_terminal'])
text_entry('basics', 'power', 'Power & Automation', 'interface_port', [
    'All machines use standard $(thing)FE$() and expose items, fluids and energy on every side, so any pipe, cable or storage mod works. Inputs can be inserted; outputs can be extracted.',
    'Fluids move the same way: pipes, AE2 import/export buses, storage buses and pattern providers can all fill input tanks and drain output tanks. Multiblocks expose items, fluids and energy through $(item)Interface Ports$() placed in the marked positions - one port is enough for everything.',
    'By hand: right-click a machine with a bucket or $(item)Containment Cell$(), or click a tank in its screen while holding one. Containment Cells are proper fluid containers, so tanks, pipes and AE2 can fill and empty them too.'], sort=0, spot=['interface_port'])
text_entry('world', 'ores', 'Ores', 'deepslate_cherenkov_quartz_ore', [
    '$(item)Cherenkov Quartz$() grows deep in the deepslate layer. $(item)Degenerate Matter$() is found in End stone. Smelt or blast it into $(item)Neutronium$().',
    '$(item)Void Lattice$() only forms in the Bulk.'], spot=['deepslate_cherenkov_quartz_ore', 'degenerate_matter_ore', 'void_lattice_ore'])
text_entry('world', 'ruins', 'Observatory Ruins', 'spacetime_casing', [
    'Crashed research observatories dot the Overworld: cracked containment domes with a supply cache. A good source of early Cherenkov Quartz and Neutronium.'])
text_entry('world', 'bulk', 'The Bulk', 'brane_shifter', [
    'Our universe is a brane floating in a higher-dimensional Bulk. The $(item)Brane Shifter$() slips you sideways into it (and back). It requires the Brane Cosmology research.',
    'The Bulk is an endless void of floating islands under the shadow of a vast hypercube. Monoliths hide vaults of exotic materials, and $(item)Void Lattice Ore$() is common.'], adv='brane_cosmology', spot=['brane_shifter'])
text_entry('singularities', 'lensing', 'Gravitational Lensing', 'micro_singularity', [
    'A running singularity bends light around its horizon: you will see the room behind it smeared into an $(thing)Einstein ring$() and a bright $(thing)photon ring$() at the edge of the shadow. White holes do the opposite.',
    'If this conflicts with a shader pack, it can be disabled in the client config ($(thing)gravitationalLensing$()).'], adv='singularity_physics')
text_entry('hyperdimensions', 'tesseract', 'Seeing the Fourth Dimension', 'tesseract_core', [
    'A tesseract is a 4D cube. What you see in the assembler is its 3D shadow, rotating through the W axis. Edge colour shows depth in W: $(#40e0ff)cyan$() near, $(#ff40ff)magenta$() far.'], adv='hyperdimensional_geometry')

text_entry('world', 'decor', 'Decorative Blocks', 'neutronium_bricks', [
    'Every high-tech material can be dressed up. Put a block of it in a $(item)Stonecutter$() for plating, tiles, bricks and pillars - and back again, so nothing is wasted.',
    'Surround a material block with glowstone dust for four $(item)Lamps$(). Most bricks and tiles also come as stairs, slabs and walls. Chronosteel, Spacetime Alloy, Cosmic Neutronium, Dilithium and Void Lattice each have a storage block too.'],
    spot=['neutronium_plating', 'cherenkov_lamp', 'spacetime_panel', 'computronium_panel', 'void_bricks', 'cloaking_glass'])
text_entry('world', 'dilithium', 'Dilithium', 'dilithium_ore', [
    '$(item)Dilithium$() grows in the Nether. Its lattice is porous to antimatter, which makes it the only practical regulator for an annihilation reactor.'], spot=['dilithium_ore', 'dilithium_crystal'])
text_entry('information', 'demon', "Maxwell's Demon", 'maxwell_demon_chip', [
    'In 1867 Maxwell imagined a tiny being that opens a door only for fast molecules, sorting hot from cold for free. Information theory says the demon must pay to forget. Ours never forgets.',
    'With enough $(item)Negentropy Cells$() and $(fluid)Dark Energy$() you can even reverse the heat death of a universe.'], adv='thermodynamics_of_information')
text_entry('simulation', 'omega', 'The Omega Point', 'omega_point', [
    'Every computation the universe will ever perform, converging on a single point at the end of time. It is the last thing you can make - and a Zero-Point Extractor will happily run on it for ten hours.'], adv='simulation_hypothesis', spot=['omega_point'])

text_entry('fabrication', 'processors', 'The Processor Tiers', 'photonic_processor', [
    'Every machine is built around a processor one tier below its own. There are seven tiers:$(br)$(li)I Photonic$(li)II Qubit$(li)III Topological$(li)IV Hyperdimensional$(li)V Cosmic$(li)VI Ontological$(li)VII Transfinite',
    'A processor needs a $(thing)board$(), $(thing)dies$() of its tier, the previous tier\'s processor and femto- or attoscale $(thing)components$(), joined with $(fluid)solder$(). From tier IV on, only the $(item)Femtoscale Assembly Line$() can assemble them.'], sort=-2, spot=['photonic_processor', 'quantum_processor', 'topological_processor', 'hyperdimensional_processor'])
text_entry('fabrication', 'wafers', 'Wafers & Lithography', 'cherenkov_wafer', [
    '$(li)Press Cherenkov quartz into a $(item)Lattice Seed$().$(li)Grow a $(item)Boule$() in the $(item)Lattice Growth Vat$().$(li)Slice it into 16 $(item)Wafers$() with the $(item)Planck-Scale Cutter$().$(li)Engrave in the $(item)Quantum Lithographer$() with $(fluid)Photoresist$().$(li)Cut each engraved wafer into 8 $(item)Dies$().',
    'The lithographer needs a $(thing)lens$() in one input slot. Lenses are $(thing)catalysts$(): never consumed. Better lenses engrave better logic:$(br)$(li)Photonic Lens$(li)Metamaterial Sheet$(li)Dark Matter Lens$(li)Tesseract Lens$(li)Cosmic String Lens'], sort=-1, spot=['cherenkov_boule', 'cherenkov_wafer', 'photonic_logic_wafer', 'photonic_die'])
text_entry('fabrication', 'byproducts', 'Coolant, Residue & Slag', 'spacetime_slag', [
    'Processes cooled by $(fluid)Superfluid Helium$() hand it back as $(fluid)Warm Helium$(). Recondense it in a $(item)Bose-Einstein Condenser$() or the $(item)Fractionation Spire$() - or you will run out.',
    'High-tier processes leave $(fluid)Dimensional Residue$(). The $(item)Fractionation Spire$() splits it into Dark Matter, Exotic Matter, Inflaton Field and Chronon Gas. $(item)Spacetime Slag$() can be sorted by a $(item)Maxwell\'s Demon$() for Void Lattice and, rarely, Planck Shards.'], adv='fractional_distillation')
text_entry('basics', 'tuning', 'Tuning Multiblocks', 'field_calibrator', [
    'Multiblocks are not set-and-forget. Each has a $(thing)tuning panel$() beside its screen with a control unique to it - the Collider has $(thing)Beam Focus$(), the Crucible $(thing)Plasma Temperature$(), the Omnilex $(thing)Law Coherence$().',
    'The ideal setting $(thing)drifts$() while the machine runs, and each machine drifts its own way: some wander, some swing back and forth, some steadily fall, and some sit still then suddenly jump. Match it to keep $(thing)efficiency$() high - the two waves on the panel line up and go clean when you are close.',
    '$(thing)Overclock$() doubles speed but quadruples power draw. $(thing)Parallel$() runs several recipes per cycle. Generators cannot overclock; tuning sets their output instead.',
    '$(thing)Containment$() wears down while running - faster when overclocked or out of tune. At zero the machine $(thing)faults$(): work in progress is lost and it stops. Use a $(item)Field Calibrator$() on the controller to restore it. It also reveals the ideal setting, marked in gold on the panel.',
    'Late in the game, an $(item)Autonomous Calibrator$() can be installed into a controller. From then on the machine follows its own drift and repairs itself, for a 25% power tax and 10% less generator output.$(br2)Pack makers can slow or disable drift and wear in the config.'],
    sort=1, spot=['field_calibrator', 'autonomous_calibrator'])
text_entry('basics', 'assembler', 'Structure Assembler', 'structure_assembler', [
    'Place a multiblock controller, then use the $(item)Structure Assembler$() on it. It builds every missing block, bottom-up, using casings from your inventory (free in creative).',
    '$(thing)Sneak-use$() for a dry run: sparks mark the gaps and you get a shopping list with what you already have. The assembler never breaks blocks - anything in the way is marked, for you to clear yourself. Wherever the structure accepts an $(item)Interface Port$(), it uses one from your inventory first, and the plain casing only once you have no ports left.',
    '$(thing)Taking a structure apart$(): sneak-use the assembler in the air to switch it to disassemble mode (it glints while it is in it). Use it on a controller, then again within five seconds to confirm: every block of the structure\'s pattern - casings, ports, coils, glass, the controller last - comes back to your inventory, and anything that does not fit lands at your feet. Blocks that are not part of the pattern are never touched. Sneak-use on a controller in this mode lists what you would get back.',
    'On a $(thing)tower of variable height$() (the Gas Centrifuge Cascade), using the assembler on a finished structure builds one more stage on top.'],
    sort=2, spot=['structure_assembler'])
text_entry('fabrication', 'catalysts', 'Catalysts', 'higgs_catalyst', [
    'Some recipes need a $(thing)catalyst$() in an input slot. It is never consumed - JEI marks it in gold. Each catalyst is made from the one before it:',
    '$(li)$(item)Higgs Catalyst$(): mass-altering processes$(li)$(item)Spacetime Catalyst$(): forged from a Higgs Catalyst, for cosmic processors and universe forging$(li)$(item)Eternity Catalyst$(): forged from a Spacetime Catalyst, for the Omega Point and everything beyond'], adv='mass_manipulation', spot=['higgs_catalyst', 'spacetime_catalyst', 'eternity_catalyst'])
text_entry('endgame', 'beyond', 'Beyond Mathematics', 'axiom_key', [
    'The Reality Compiler showed that the universe runs on source code. Beneath the code are $(thing)axioms$() - the rules that decide what can be true at all. Physics is just their consequence.',
    'To leave reality you must break all of them at once: build $(item)Aleph-Null Shards$(), the $(item)Godel Sentence$(), $(item)Russell Paradox Sets$() and $(item)Banach-Tarski Spheres$(), fuse them into $(item)Absolute Infinity$(), then distil $(item)Axiom Fragments$() and open the $(thing)Axiomatic Breach$().'], adv='transfinite_mathematics', sort=-1, spot=['aleph_null_shard', 'godel_sentence', 'russell_paradox_set', 'banach_tarski_sphere'])
text_entry('endgame', 'hyperreal', 'Hyperdimensional Reality Space', 'bubble_universe_sample', [
    'Outside every universe there is a single, endless space. A $(thing)World Tree$() of an unknown material towers over it, more than four hundred blocks high, its branches spreading for more than a thousand blocks in every direction.',
    'Every branch carries $(item)Bubble Universes$() - entire cosmoses, each the size of a fist. Break one to take a sample.',
    '$(item)Arbor Heartwood$() does not behave normally. It drifts between four phases: dormant; $(thing)inverted$() (gravity reverses for whatever stands on it); $(thing)luminous$() (blazing light, and it hurries you along); and $(thing)unreal$() (no light at all, and things fall as if through syrup).',
    'Below the Tree lies the $(thing)Ocean of Primordial Law$(). Laws bubble up out of it and pop out of existence again; anything that swims in it obeys whichever law is in force at that moment. Strangely, it is breathable.$(br2)Distil it into $(item)Axiom Fragments$() and $(item)Crystallised Law$().'], adv='hyperdimensional_reality', spot=['arbor_heartwood', 'bubble_universe_sample'])
text_entry('endgame', 'omnilex_goal', 'The Omnilex', 'omnilex', [
    'The final goal: build the $(item)Omnilex Engine$() in Hyperdimensional Reality Space. Simulate the Tree as an $(item)Arbor Simulacrum$() and the Ocean as a $(item)Primordial Ocean Simulacrum$(), then join them with an $(item)Omega Point$().',
    'Hold the $(item)Omnilex$() and you may fly. With the two simulacra the engine can grow new bubble universes, rewrite laws into crystals, or simulate everything that will ever be, down to the Omega Point.'], adv='omnilex', spot=['omnilex', 'tree_simulacrum', 'ocean_simulacrum'])

text_entry('endgame', 'mass_crafting', 'Mass Crafting', 'extreme_assembly_table', [
    'The last ingot is not hard because of one rare thing - it is hard because of $(thing)how much$() goes into it. The $(item)Extreme Assembly Table$() is a 9x9 crafting grid; JEI\'s "+" button fills it for you.',
    'The $(item)Singularity Compactor$() swallows thousands of one material and squeezes them into a $(item)Material Singularity$(): 2,048 netherite, 8,192 diamonds, 65,536 redstone... There is no timer - it absorbs as fast as you (or pipes, or AE2) feed it, and remembers its progress.',
    'Lattices of $(item)Cherenkov Lattice$() and nether stars make the $(item)Crystalline Matrix Ingot$(). Four "everything" items take one of every meat, every crop, a ring of ender pearls, and every music disc.',
    'The $(item)Omniscience Catalyst$() takes one of every material singularity, the conceptual singularities, the four everything-items and more - roughly ' + f'{extreme.total_per_catalyst():,}' + ' raw items each.',
    'The $(item)Omnium Ingot$() needs 16 catalysts, 32 Cosmic Neutronium, 16 Crystalline Matrix and an Omega Point in the 9x9 table. With $(thing)cosmic neutronium coils$() the Conceptual Singularity Compressor forges one from 4 catalysts instead; with $(thing)transfinite coils$(), four.'],
    adv='conceptual_singularities', sort=-1, spot=['extreme_assembly_table', 'singularity_compactor', 'omnium_ingot'])

# =================================================================================== CONTAINMENT FAILURE: creatures, effects, fallout
CREATURES = {
    'graviton_wisp': ('Graviton Wisp', [('graviton_wisp_core', 1, 1, 1.0), ('graviton_crystal', 1, 1, 0.3), ('void_lattice_shard', 0, 2, 1.0)]),
    'brane_strider': ('Brane Strider', [('brane_tension_fiber', 1, 3, 1.0), ('void_lattice_shard', 0, 2, 1.0), ('kaluza_klein_shard', 1, 1, 0.25)]),
    'moduli_crawler': ('Moduli Crawler', [('moduli_crystal', 1, 1, 0.35), ('void_lattice_shard', 0, 1, 1.0)]),
    'destabilised_husk': ('Destabilised Husk', [('fractured_essence', 0, 2, 1.0), ('minecraft:rotten_flesh', 0, 2, 1.0), ('strange_matter_pellet', 1, 1, 0.04)]),
    'void_wraith': ('Void Wraith', [('void_essence', 1, 1, 0.6), ('void_lattice_shard', 0, 2, 1.0), ('exotic_matter_crystal', 1, 1, 0.05)]),
    'nanite_swarm': ('Nanite Swarm', [('nanite_swarm', 0, 1, 0.5), ('quantum_dot', 0, 2, 1.0)]),
    'paradox_echo': ('Paradox Echo', [('paradox_residue', 1, 2, 1.0), ('chronon_capacitor', 1, 1, 0.08), ('closed_timelike_curve', 1, 1, 0.01)]),
    'law_wraith': ('Law Wraith', [('law_crystal', 0, 1, 0.5), ('axiom_fragment', 1, 1, 0.15), ('minecraft:blaze_rod', 0, 1, 1.0)]),
}
CREATURES.update(round3.CREATURES)
CREATURES.update(round4.CREATURES)
CREATURES.update(round5.CREATURES)
CREATURES.update(round7.CREATURES)
CREATURES.update(round11.CREATURES)
import round44
CREATURES.update(round44.CREATURES)
import round48
CREATURES.update(round48.CREATURES)
import round51
CREATURES.update(round51.CREATURES)
NO_LOOTING = {'black_swan_feather'}        # 1.10.1: one feather per swan, whatever the sword
for eid, (ename, drops) in CREATURES.items():
    LANG[f'entity.{NS}.{eid}'] = ename
    LANG[f'item.{NS}.{eid}_spawn_egg'] = ename + ' Spawn Egg'
    w(os.path.join(A, 'models/item', eid + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
    pools = []
    for item, lo, hi, chance in drops:
        entry = {'type': 'minecraft:item', 'name': q(item), 'functions': [
            {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
                 + ([] if item in NO_LOOTING else [
            {'function': 'minecraft:enchanted_count_increase', 'enchantment': 'minecraft:looting', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}])}
        pool = {'rolls': 1, 'entries': [entry]}
        if chance < 1.0:
            pool['conditions'] = [{'condition': 'minecraft:killed_by_player'}, {'condition': 'minecraft:random_chance', 'chance': chance}]
        pools.append(pool)
    w(os.path.join(LT, 'entities', eid + '.json'), {'type': 'minecraft:entity', 'pools': pools})
LANG[f'entity.{NS}.containment_rupture'] = 'Containment Rupture'
LANG.update({
    f'effect.{NS}.reality_destabilisation': 'Reality Destabilisation', f'effect.{NS}.nanite_infection': 'Nanite Infection',
    f'effect.{NS}.temporal_dissonance': 'Temporal Dissonance',
    'message.theoreticalsciences.rupture_start': 'WARNING: spacetime containment breached - a rupture is forming!',
    'message.theoreticalsciences.rupture_growing': 'The rupture is growing. Get clear!',
    'message.theoreticalsciences.rupture_imminent': 'RUPTURE DETONATION IMMINENT',
})
# fractured reality: in the world it is drawn by a block-entity renderer (the original block, glitching and splitting);
# this model is only used for the item form and break particles
w(os.path.join(A, 'models/block', 'fractured_reality.json'), {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                                                'textures': {'all': f'{NS}:block/fractured_reality'}})
w(os.path.join(LT, 'blocks', 'fractured_reality.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [
    {'type': 'minecraft:item', 'name': q('fractured_essence')}], 'conditions': [{'condition': 'minecraft:random_chance', 'chance': 0.3}]}]})
# what the drops are good for
mr('fallout_sorting', 'entropy_sorting', [('fractured_essence', 8)], [('superfluid_helium', 250)], [('degenerate_matter_chunk', 2), ('strange_matter_pellet', 1)],
   [('warm_helium', 250)], 32768, 300, 'thermodynamics_of_information', chances=[1.0, 0.1])
mr('void_essence_lens', 'weaving', [('void_essence', 2), ('metamaterial_sheet', 1)], [('dark_matter', 250)], [('dark_matter_lens', 1)], energy=8192, duration=300, research='dark_sector')
text_entry('basics', 'containment_failure', 'Containment Failure', 'fractured_reality', [
    'A multiblock whose containment reaches zero does not just stop. What it was holding gets out, and what gets out depends on the machine:',
    '$(li)$(thing)Reality Destabilisation$() (colliders, reactors, forges, crucibles): a blast, and the surrounding world starts to come apart. Blocks become $(thing)Fractured Reality$(): they fade in and out and split into smaller copies of themselves, spreading slowly before reality heals. Creatures caught in it glitch and splinter like higher-dimensional shadows, hit harder and move faster; villagers and zombies become $(thing)Destabilised Husks$().$(li)$(thing)Spatial$() (singularities, wormholes, universe forging): a $(thing)rupture$() grows for ten seconds, drags everything in, eats the terrain and detonates. $(thing)Void Wraiths$() - tiny living black and white holes - crawl out, and burst like a creeper when they collapse.',
    '$(li)$(thing)Infectious$() (Matrioshka Brain, Reality Compiler, Assembly Line): flying $(thing)Nanite Swarms$() escape. They split when destroyed, down to single units, and any creature that dies infected bursts into a new swarm.$(li)$(thing)Temporal$() (Causality Loop Engine): $(thing)Paradox Echoes$() - walking clockwork, one for every player nearby - that rewind themselves when hurt.$(li)$(thing)Dimensional$() (Rift, Tesseract, Breach, Omnilex): a rupture plus $(thing)Law Wraiths$(), broken laws of nature shaped like tesseracts.',
    'Overclocked machines fail harder. Keep containment up with a $(item)Field Calibrator$(), or install an $(item)Autonomous Calibrator$().$(br2)Server owners can disable the consequences or the block damage in the config; block damage also respects the $(thing)mobGriefing$() game rule.'],
    sort=2, spot=['fractured_reality', 'fractured_essence', 'void_essence'])

text_entry('basics', 'component_tiers', 'Components & Tiers', 'spacetime_alloy_coil_block', [
    'Big multiblocks are built from $(thing)functional components$(), each with its own crafting chain: magnets, RF cavities, beam pipes, cryogenics, vacuum pumps, detectors, injectors, lenses, anchors, stabilisers and more.',
    '$(thing)Coils$() come in five tiers: Superconducting, Chronosteel, Spacetime Alloy, Cosmic Neutronium and Transfinite. A structure counts its $(l)lowest$() coil. Each tier above the first adds 25% speed, and some recipes need a minimum tier (JEI shows it).',
    '$(thing)Field Emitters$() come in three tiers. The lowest emitter tier multiplies how many parallels the tuning panel allows.$(br2)The $(item)Structure Assembler$() places the best tier you carry; in creative, hold the tier you want in your off-hand. Mixing tiers works, but only the lowest counts.'],
    adv='component_tiers', sort=3, spot=['spacetime_alloy_coil_block', 'graviton_field_emitter', 'dipole_bending_magnet'])

# =================================================================================== QUANTITY INFLATION (see scaling.py)
round3.apply(globals())
round4.apply(globals())
round5.apply(globals())
round7.apply(globals())
round8.apply(globals())
round10.apply(globals())
round11.apply(globals())
round12.apply(globals())
round13.apply(globals())
round14.apply(globals())
round15.apply(globals())
round16.apply(globals())
import round17
round17.apply(globals())
import round18
round18.apply(globals())
round18.untangle(globals())
round19.apply(globals())
import round20
round20.apply(globals())
import round21
round21.apply(globals())
import round22
round22.apply(globals())
import round24
round24.apply(globals())
import round25
round25.apply(globals())
round26.apply(globals())
import round27
round27.apply(globals())
import round28
round28.apply(globals())
round29.apply(globals())
import round30_renew
round30_renew.apply(globals())
import round30_codex
round30_codex.apply(globals())
import round30_misc
round30_misc.apply(globals())
import round31
round31.apply(globals())
import round32_branches
round32_branches.apply(globals())
import round32_gear
round32_gear.apply(globals())
import round33
round33.apply(globals())
import round34_anomaly
round34_anomaly.apply(globals())
import round35
round35.apply(globals())
round37.apply(globals())
round38.apply(globals())
round38.apply_late(globals())
import round39
round39.apply(globals())
import round40
round40.apply(globals())
round40.apply_late(globals())
import round41
round41.apply(globals())
round41.apply_late(globals())
round42.apply(globals())
round42.apply_late(globals())
round43.apply(globals())
round43.apply_late(globals())
round44.apply(globals())
round44.apply_late(globals())
round45.apply(globals())
round45.apply_late(globals())
round46.apply(globals())
round46.apply_late(globals())
round47.apply(globals())
round47.apply_late(globals())
round48.apply(globals())
round48.apply_late(globals())
round49.apply(globals())
round49.apply_late(globals())
round50.apply(globals())
round50.apply_late(globals())
round51.apply(globals())
round51.apply_late(globals())
round52.apply(globals())
round52.apply_late(globals())
round53.apply(globals())
round53.apply_late(globals())
round54.apply(globals())
round54.apply_late(globals())
round55.apply(globals())
round55.apply_late(globals())
round56.apply(globals())
round56.apply_late(globals())
round57.apply(globals())

import scaling
print('scaled recipes:', scaling.apply(REC, M, TIER))
round57.apply_late(globals())      # 1.14.2: the balance pass sees every recipe as it will ship (after the inflation above)

# =================================================================================== LANG write
LANG.update(complexity.LANG)
concepts_gen.book(globals())
LANG['tuning.theoreticalsciences.concept_coherence'] = 'Concept Coherence'
LANG.update(json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'sound_lang.json'))))
LANG.update({'gui.theoreticalsciences.tank_hint_in': 'Click with a fluid container to pour in (or drain)', 'gui.theoreticalsciences.tank_hint_out': 'Click with an empty container to drain'})
LANG.update({f'tuning.theoreticalsciences.{k}': v for k, v in complexity.TUNING_LANG.items()})
LANG['theoreticalsciences.configuration.title'] = 'Theoretical Sciences'
round11.finish(globals())
round10.finish(globals())
round8.finish(globals())
round7.finish(globals())
round5.finish(globals())
round12.finish(globals())
round13.finish(globals())
round14.finish(globals())
round15.finish(globals())
round16.expand(globals())
round21.reuse(globals())
round16.finish(globals())
round17.finish(globals())
round18.finish(globals())
round19.finish(globals())
round20.finish(globals())
round21.finish(globals())
round22.finish(globals())
round24.finish(globals())
round25.finish(globals())
round26.finish(globals())
round27.finish(globals())
round30_renew.finish(globals())
round30_codex.finish(globals())
round30_misc.finish(globals())
round31.finish(globals())
round32_branches.finish(globals())
round32_gear.finish(globals())
round33.finish(globals())
round34_anomaly.finish(globals())
round35.finish(globals())
round37.finish(globals())
round38.finish(globals())
round39.finish(globals())
round40.finish(globals())
round41.finish(globals())
round42.finish(globals())
round43.finish(globals())
round44.finish(globals())
round45.finish(globals())
round46.finish(globals())
round47.finish(globals())
round48.finish(globals())
round49.finish(globals())
round50.finish(globals())
round51.finish(globals())
round52.finish(globals())
round53.finish(globals())
round54.finish(globals())
round55.finish(globals())
round56.finish(globals())
round57.finish(globals())
import round23
round23.finish(globals())
round35.late(globals())
# round 36: the jar's lang file keeps names and interface text. The Modonomicon book's page text (book.<ns>.codex.*)
# is only a build intermediate now that the mod ships its own Codex alone (whose text lives in ts_codex/): it goes to
# tools/out/lang_full.json, which the checks and the next pass read.
os.makedirs(os.path.join(ROOT, 'out'), exist_ok=True)
json.dump(dict(sorted(LANG.items())), open(os.path.join(ROOT, 'out', 'lang_full.json'), 'w'), ensure_ascii=False)
w(os.path.join(A, 'lang', 'en_us.json'), dict(sorted((k, v) for k, v in LANG.items() if not k.startswith(f'book.{NS}.codex.'))))
print('data done:', len(LANG), 'lang keys')
