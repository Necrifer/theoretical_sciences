"""
Round 7: Chemistry and Biology, woven into the physics.

Chemistry grows out of quantum mechanics (bonds are wavefunctions, orbitals are eigenstates) and ends at the island
of stability, where nuclear physics makes elements that should not exist. Biology starts with molecules, passes
through quantum biology (photosynthesis and bird navigation run on coherence), takes samples from the creatures of
every dimension, is accelerated in chronal fields, and ends at abiogenesis and panspermia - life seeded across
universes.
"""
import json
import os

import shapes


def _I(id, name, art, c1, c2, lore, **kw):
    d = dict(id=id, name=name, art=art, c1=c1, c2=c2, lore=lore)
    d.update(kw)
    return d


ITEMS = [
    # ---------------------------------------------------------------- chemistry
    _I('zeolite_catalyst', 'Zeolite Catalyst', 'lattice', '#304040', '#c0f0e0', 'A crystal full of molecule-sized pores; reactions that fit inside go faster.'),
    _I('molecular_orbital_lattice', 'Molecular Orbital Lattice', 'hexgrid', '#102040', '#80c0ff', 'Electron wavefunctions shared between atoms, frozen into a grid. This is what a chemical bond is.'),
    _I('chiral_crystal', 'Chiral Crystal', 'crystal', '#302010', '#ffb060', 'Left-handed or right-handed, never both. Life only uses one hand.'),
    _I('electride_crystal', 'Electride Crystal', 'crystal', '#101830', '#60a0ff', 'A salt whose negative ions are bare electrons sitting in holes in the lattice.'),
    _I('graphene_sheet', 'Graphene Sheet', 'plate', '#202020', '#a0a0a0', 'Carbon one atom thick. Stronger than steel, conducts better than copper.'),
    _I('carbon_nanotube_bundle', 'Carbon Nanotube Bundle', 'spindle', '#181818', '#909090', 'Graphene rolled into tubes, the tubes twisted into rope.'),
    _I('buckyball_cage', 'Buckyball Cage', 'orb', '#202020', '#e0c060', 'Sixty carbon atoms arranged like a football - with room inside for one more atom.'),
    _I('metal_organic_framework', 'Metal-Organic Framework', 'lattice', '#203040', '#ffa040', 'Metal nodes, organic struts: a sponge with more inner surface than a football pitch per gram.'),
    _I('noble_gas_clathrate', 'Noble Gas Clathrate', 'contained', '#102030', '#c0e0ff', 'Xenon caged in ice. The noble gases are not as aloof as the textbooks said.'),
    _I('self_healing_polymer', 'Self-Healing Polymer', 'chunk', '#203018', '#80ff80', 'Cut it and the chains find each other again.'),
    _I('room_temperature_superconductor', 'Room-Temperature Superconductor', 'plate', '#101828', '#80f0ff', 'Zero resistance without cryogenics. Physicists have chased this for a century.'),
    _I('positronium_hydride', 'Positronium Hydride', 'pellet', '#301030', '#ff80ff', 'A molecule made of a hydrogen atom and an atom of matter bound to antimatter. It lasts a nanosecond - unless you trap it.'),
    _I('muonic_catalyst', 'Muonic Catalyst', 'pellet', '#203040', '#80ffff', 'Swap an electron for a muon and atoms shrink 200 times - close enough to fuse cold.'),
    _I('flerovium_ingot', 'Flerovium Ingot', 'ingot', '#404858', '#d0e0ff', 'Element 114, held together by magic numbers of protons and neutrons.'),
    _I('unbihexium_ingot', 'Unbihexium Ingot', 'ingot', '#403858', '#ffe0ff', 'Element 126, the heart of the island of stability. Nothing should be this heavy and last this long.'),
    # ---------------------------------------------------------------- biology
    _I('gene_sampler', 'Gene Sampler', 'probe', '#203020', '#80ff80', 'Use on any creature to take a genome sample. Creatures from other dimensions give xenogenome samples.', special='genesampler', stack=1),
    _I('genome_sample', 'Genome Sample', 'contained', '#203020', '#80ff80', 'Every instruction for building one creature, in a vial.'),
    _I('xenogenome_sample', 'Xenogenome Sample', 'contained', '#201030', '#ff80ff', 'A genome from a creature of another dimension. Its code is not written in DNA.'),
    _I('recombinant_dna', 'Recombinant DNA', 'loop', '#102010', '#60ff80', 'A double helix spliced from more than one creature.'),
    _I('engineered_enzyme', 'Engineered Enzyme', 'chip', '#202810', '#c0ff60', 'A protein designed to speed up exactly one reaction a millionfold.'),
    _I('folded_protein_crystal', 'Folded Protein Crystal', 'crystal', '#302018', '#ffc080', 'A chain of amino acids that found its one shape out of more than the universe has atoms.'),
    _I('stem_cell_culture', 'Stem Cell Culture', 'contained', '#302028', '#ffc0d0', 'Cells that have not yet decided what to become.'),
    _I('synthetic_cell', 'Minimal Synthetic Cell', 'orb', '#203028', '#a0ffc0', 'The smallest genome that can still be alive, written from scratch.'),
    _I('xenobot_swarm', 'Xenobot Swarm', 'swarm', '#302020', '#ff9080', 'Living machines made of cells, designed by an evolutionary algorithm.'),
    _I('tardigrade_culture', 'Tardigrade Culture', 'contained', '#283020', '#d0e0a0', 'Survives vacuum, radiation and freezing by simply pausing being alive.'),
    _I('quantum_chlorophyll', 'Quantum Chlorophyll', 'crystal', '#103010', '#60ff60', 'Photosynthesis moves energy by trying every path at once - coherently.'),
    _I('cryptochrome_magnetoreceptor', 'Cryptochrome Magnetoreceptor', 'lens', '#201838', '#a080ff', 'The protein in a robin\'s eye that feels Earth\'s field through entangled radical pairs.'),
    _I('neural_organoid', 'Neural Organoid', 'brain', '#302028', '#ffa0c0', 'A pea-sized brain grown in a dish. It has started to dream.'),
    _I('turing_morphogen', 'Turing Morphogen', 'dots', '#302810', '#ffd060', 'Two chemicals, one fast and one slow, that paint stripes and spots by themselves.'),
    _I('silicon_life_spore', 'Silicon Life Spore', 'shard', '#203040', '#a0e0ff', 'Life built on silicon instead of carbon. It grew in the Bulk.'),
    _I('telomerase_complex', 'Telomerase Complex', 'spindle', '#302020', '#ff6060', 'Rebuilds the clocks at the ends of chromosomes. Cells that have it do not age.'),
    _I('spark_of_life', 'Spark of Life', 'seed', '#303010', '#ffffa0', 'The moment chemistry became biology, caught and kept.'),
    _I('panspermia_seed', 'Panspermia Seed', 'useed', '#102030', '#a0ffd0', 'Life packed to survive the trip between universes.'),
    _I('omnicellular_genome', 'Omnicellular Genome', 'manifold', '#101010', '#c0ffc0', 'One genome that can build any creature in any dimension.'),
]

FLUIDS = [
    dict(id='supercritical_solvent', name='Supercritical Solvent', art='condensate', c1='#d0e8f0', c2='#ffffff', gas=False, light=0, temp=310, density=600, viscosity=300),
    dict(id='ionic_liquid', name='Ionic Liquid', art='synaptic', c1='#4060ff', c2='#a0c0ff', gas=False, light=2, temp=300, density=1200, viscosity=3000),
    dict(id='carborane_superacid', name='Carborane Superacid', art='exotic', c1='#c0ff40', c2='#f0ffa0', gas=False, light=4, temp=300, density=1600, viscosity=1500),
    dict(id='nutrient_broth', name='Nutrient Broth', art='condensate', c1='#c0a040', c2='#f0e0a0', gas=False, light=0, temp=310, density=1020, viscosity=1200),
    dict(id='xenoplasm', name='Xenoplasm', art='exotic', c1='#a040ff', c2='#ffa0ff', gas=False, light=6, temp=290, density=1100, viscosity=2500),
    dict(id='primordial_soup', name='Primordial Soup', art='synaptic', c1='#406020', c2='#c0e060', gas=False, light=3, temp=340, density=1050, viscosity=2000),
]

BLOCKS = []

MACHINE_TEXT = {
    'chemical_reactor': ('Chemical Reactor', 'A pressure vessel with a catalyst bed, a heater and a stirrer. Most of chemistry is getting the right molecules to meet at the right energy.'),
    'electrolytic_cell': ('Electrolytic Cell', 'Two electrodes in a conducting liquid: drive a current through it and it comes apart into what it was made of.'),
    'bioreactor': ('Bioreactor', 'A sterile, stirred, temperature-held vat where cells grow and do the chemistry for you.'),
    'gene_sequencer': ('Gene Sequencer', 'Reads a genome letter by letter - and, run backwards, writes one.'),
    'nuclide_forge': ('Nuclide Forge', 'A heavy-ion accelerator firing into a spinning target: fuses nuclei into elements beyond the periodic table, aiming for the island of stability.'),
    'evolution_engine': ('Evolution Engine', 'A biodome where generations of designed life live and die in seconds. Run it inside a fast chronal field and a million years pass in a minute.'),
    'abiogenesis_crucible': ('Abiogenesis Crucible', 'A crucible of primordial soup under lightning and ultraviolet light, held where chemistry tips over into life.'),
}
MACHINE_CAT = {k: 'chemistry' for k in ('chemical_reactor', 'electrolytic_cell', 'nuclide_forge')}
MACHINE_CAT.update({k: 'biology' for k in ('bioreactor', 'gene_sequencer', 'evolution_engine', 'abiogenesis_crucible')})
MACHINE_RESEARCH = {'chemical_reactor': 'chemical_bonding', 'electrolytic_cell': 'electrochemistry', 'nuclide_forge': 'island_of_stability',
                    'bioreactor': 'molecular_biology', 'gene_sequencer': 'genomics', 'evolution_engine': 'directed_evolution',
                    'abiogenesis_crucible': 'abiogenesis'}
TIER = {'chemical_reactor': 1, 'electrolytic_cell': 1, 'bioreactor': 2, 'gene_sequencer': 2, 'nuclide_forge': 4, 'evolution_engine': 4, 'abiogenesis_crucible': 5}
MC = {
    'chemical_reactor': (['QGQ', 'CMC', 'NBN'], {'Q': 'cherenkov_quartz', 'G': 'containment_field_glass', 'C': 'casimir_plate', 'M': 'minecraft:cauldron', 'N': 'neutronium_plate', 'B': 'minecraft:blast_furnace'}),
    'electrolytic_cell': (['CRC', 'NWN', 'CRC'], {'C': 'chronon_capacitor', 'R': 'minecraft:redstone_block', 'N': 'neutronium_plate', 'W': 'minecraft:water_bucket'}),
    'bioreactor': (['GZG', 'PEP', 'NCN'], {'G': 'containment_field_glass', 'Z': 'zeolite_catalyst', 'P': 'quantum_processor', 'E': 'chemical_reactor', 'N': 'neutronium_plate', 'C': 'chronon_capacitor'}),
    'gene_sequencer': (['DLD', 'QOQ', 'NPN'], {'D': 'quantum_dot', 'L': 'molecular_orbital_lattice', 'Q': 'quantum_processor', 'O': 'bioreactor', 'N': 'neutronium_plate', 'P': 'precision_assembly_casing'}),
    'nuclide_forge': (['MRM', 'XFX', 'SES'], {'M': 'dipole_bending_magnet', 'R': 'rf_accelerating_cavity', 'X': 'calorimeter_detector_array', 'F': 'chemical_reactor', 'S': 'magnetic_confinement_casing', 'E': 'r_process_cluster'}),
    'evolution_engine': (['GHG', 'ASA', 'OTO'], {'G': 'containment_field_glass', 'H': 'arbor_heartwood_sample', 'A': 'arrow_of_time_shard', 'S': 'gene_sequencer', 'O': 'chronoplastic_casing', 'T': 'temporal_knot'}),
    'abiogenesis_crucible': (['JGJ', 'CSC', 'PUP'], {'J': 'plasma_injection_nozzle', 'G': 'containment_field_glass', 'C': 'plasma_crucible_casing', 'S': 'evolution_engine', 'P': 'precision_assembly_casing', 'U': 'universe_seed'}),
}
EXTRA = {
    'chemical_reactor': 'Most early chemistry runs here: catalysts, solvents, bonds and nanomaterials.',
    'electrolytic_cell': 'Pulls molecules apart with current: electrides, ionic liquids and superacids.',
    'bioreactor': 'Grows cultures in $(fluid)Nutrient Broth$(): cells, enzymes, tardigrades and organoids.',
    'gene_sequencer': 'Reads and writes genomes. Feed it samples from a $(item)Gene Sampler$().',
    'nuclide_forge': 'Fuses superheavy elements - Flerovium and Unbihexium - from the r-process.',
    'evolution_engine': 'Recipes need time running at least 10x: run it inside a $(item)Chronal Field Projector$() field.',
    'abiogenesis_crucible': 'Turns $(fluid)Primordial Soup$() into the $(item)Spark of Life$().',
}

# ---------------------------------------------------------------- research: (id, title, description, icon, x, y, insight, requires, cost)
RESEARCH = [
    ('chemical_bonding', 'Chemical Bonding', 'Atoms share electrons because a shared wavefunction has lower energy. Unlocks the Chemical Reactor.', 'chemical_reactor', 2, 11, 20,
     ['quantum_foundations', 'number_systems'], [('cherenkov_quartz', 8)]),
    ('quantum_chemistry', 'Quantum Chemistry', 'Solve the Schrödinger equation for a molecule and you have its chemistry. Unlocks orbital lattices and chiral crystals.',
     'molecular_orbital_lattice', 4, 11, 35, ['chemical_bonding', 'quantum_entanglement'], [('qubit', 4)]),
    ('electrochemistry', 'Electrochemistry', 'Chemistry is electrons moving; push them with a voltage. Unlocks the Electrolytic Cell.', 'electrolytic_cell', 3, 12, 30,
     ['chemical_bonding', 'cryogenics'], [('chronon_capacitor', 2)]),
    ('nanochemistry', 'Nanochemistry', 'Carbon one atom thick, rolled, folded and caged. Unlocks graphene, nanotubes and buckyballs.', 'graphene_sheet', 6, 11, 45,
     ['quantum_chemistry', 'nanotechnology'], [('molecular_orbital_lattice', 2)]),
    ('superacid_chemistry', 'Superacids', 'Acids a million million times stronger than sulfuric - strong enough to protonate almost anything.', 'chiral_crystal', 5, 12, 50,
     ['electrochemistry', 'particle_physics'], [('electride_crystal', 2)]),
    ('supramolecular_chemistry', 'Supramolecular Chemistry', 'Molecules that build themselves into cages, frameworks and machines. Unlocks MOFs, clathrates and self-healing polymers.',
     'metal_organic_framework', 8, 11, 60, ['nanochemistry', 'topology'], [('graphene_sheet', 4)]),
    ('exotic_chemistry', 'Exotic Chemistry', 'Chemistry with muons, positrons and antimatter instead of electrons. Unlocks positronium hydride and muonic catalysts.',
     'positronium_hydride', 9, 12, 90, ['antimatter', 'particle_physics', 'supramolecular_chemistry'], [('antimatter_pellet', 2)]),
    ('superconductivity', 'Room-Temperature Superconductivity', 'Cooper pairs that hold together at room temperature, inside a hydrogen-rich framework.',
     'room_temperature_superconductor', 10, 11, 110, ['supramolecular_chemistry', 'mass_manipulation'], [('metal_organic_framework', 2)]),
    ('island_of_stability', 'The Island of Stability', 'Past the end of the periodic table, magic numbers promise superheavy nuclei that last. Unlocks the Nuclide Forge.',
     'unbihexium_ingot', 12, 12, 160, ['exotic_chemistry', 'nucleosynthesis'], [('r_process_cluster', 4)]),
    ('molecular_biology', 'Molecular Biology', 'Life is chemistry that copies itself. Unlocks the Bioreactor, Nutrient Broth and the Gene Sampler.', 'bioreactor', 4, 14, 30,
     ['chemical_bonding', 'artificial_intelligence'], [('zeolite_catalyst', 2)]),
    ('genomics', 'Genomics', 'Four letters, three billion of them. Unlocks the Gene Sequencer, recombinant DNA and engineered enzymes.', 'gene_sequencer', 6, 14, 45,
     ['molecular_biology', 'formal_logic'], [('genome_sample', 4)]),
    ('protein_folding', 'Protein Folding', 'A chain finds its one shape among astronomically many. Unlocks folded protein crystals.', 'folded_protein_crystal', 8, 14, 60,
     ['genomics', 'quantum_chemistry'], [('recombinant_dna', 2)]),
    ('extremophiles', 'Extremophiles', 'Life at the edges: boiling vents, frozen vacuum, radiation. Unlocks tardigrade cultures.', 'tardigrade_culture', 7, 15, 70,
     ['genomics', 'cryogenics'], [('genome_sample', 8)]),
    ('quantum_biology', 'Quantum Biology', 'Photosynthesis, enzymes and migrating birds all use quantum coherence. Unlocks quantum chlorophyll and magnetoreceptors.',
     'quantum_chlorophyll', 10, 14, 80, ['protein_folding', 'quantum_entanglement'], [('folded_protein_crystal', 2)]),
    ('synthetic_biology', 'Synthetic Biology', 'Write life instead of reading it. Unlocks synthetic cells, xenobots and stem cell cultures.', 'synthetic_cell', 11, 15, 100,
     ['protein_folding', 'nanochemistry'], [('engineered_enzyme', 4)]),
    ('neurobiology', 'Neurobiology', 'Grow a brain in a dish and ask it what it thinks. Unlocks neural organoids.', 'neural_organoid', 12, 16, 110,
     ['synthetic_biology', 'general_intelligence'], [('stem_cell_culture', 2)]),
    ('morphogenesis', 'Morphogenesis', 'How a blob of identical cells paints itself into a body: reaction and diffusion. Unlocks Turing morphogens.', 'turing_morphogen', 13, 15, 120,
     ['synthetic_biology', 'complex_analysis'], [('synthetic_cell', 2)]),
    ('xenobiology', 'Xenobiology', 'Life in other dimensions does not use DNA. Unlocks xenogenome work, Xenoplasm and silicon life.', 'xenogenome_sample', 13, 14, 130,
     ['synthetic_biology', 'brane_cosmology'], [('genome_sample', 16)]),
    ('directed_evolution', 'Directed Evolution', 'A million generations in a minute, if time runs fast enough. Unlocks the Evolution Engine and telomerase.', 'evolution_engine', 15, 15, 170,
     ['xenobiology', 'chronodynamics', 'morphogenesis'], [('xenogenome_sample', 4)]),
    ('abiogenesis', 'Abiogenesis', 'Where chemistry becomes life. Unlocks the Abiogenesis Crucible and the Spark of Life.', 'abiogenesis_crucible', 17, 15, 300,
     ['directed_evolution', 'simulation_hypothesis'], [('telomerase_complex', 2)]),
    ('panspermia', 'Panspermia', 'Life carried between worlds - and between universes. Unlocks panspermia seeds and the omnicellular genome.', 'panspermia_seed', 19, 15, 450,
     ['abiogenesis', 'multiverse', 'island_of_stability'], [('spark_of_life', 1)]),
]


# ---------------------------------------------------------------- multiblock shapes
def nuclide_forge():
    """A short heavy-ion linac (RF cavities and focusing magnets) firing into a detector-ringed target chamber."""
    s = shapes.Shape(7, 6, 15)
    c = 3
    for z in range(1, 9):
        s.put(c, 2, z, 'e')
        if z % 2 == 0:
            s.put(c - 1, 2, z, 'q'); s.put(c + 1, 2, z, 'q')
        else:
            s.put(c, 3, z, 'r')
        s.put(c, 1, z, 'M')
    s.box(0, 0, 9, 6, 0, 14, 'M')
    s.box(1, 1, 10, 5, 4, 13, 'S', hollow=True)
    for x in (1, 5):
        for y in (2, 3):
            s.put(x, y, 11, 'x'); s.put(x, y, 12, 'x')
    s.put(c, 2, 9, 'e')
    s.air_box(2, 2, 11, 4, 3, 12)
    s.put(c, 5, 11, 'K'); s.put(c, 5, 12, 'K')
    s.v[(c, 1, 0)] = 'M'
    s.center = (c, 2, 11)
    s.ports('M', 'N', 4, y=0)
    s.v[(c, 2, 0)] = 'C'
    s.ctrl = (c, 2, 0)
    return s


def evolution_engine():
    """A biodome: living heartwood ring, chronoplastic base that bends time, glass dome over a garden of change."""
    s = shapes.Shape(11, 8, 11)
    c = 5
    s.disc(c, c, 0, 5.3, 'O')
    s.disc(c, c, 1, 4.6, 'h', r_in=3.4)
    s.sphere(c, 1, c, 5.0, 'G', shell=0.6, filter=lambda x, y, z: y >= 2)
    for (x, z), _ in shapes.ring_cells(c, c, 3.5):
        s.put(x, 2, z, 'i')
    s.sphere_air(c, 2, c, 3.4)
    s.put(c, 6, c, 'F')
    s.center = (c, 3, c)
    s.ports('O', 'J', 4, y=0)
    s.v[(c, 1, c - 5)] = 'C'
    s.ctrl = (c, 1, c - 5)
    return s


def abiogenesis_crucible():
    """A crucible of soup under a dome, four plasma injectors throwing lightning in, an anchor overhead holding it all."""
    s = shapes.Shape(9, 9, 9)
    c = 4
    s.disc(c, c, 0, 4.3, 'c')
    for y in (1, 2):
        s.disc(c, c, y, 3.6, 'c', r_in=2.5)
    s.sphere(c, 2, c, 4.0, 'G', shell=0.6, filter=lambda x, y, z: y >= 3)
    for d in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        s.put(c + d[0], 3, c + d[1], 'j')
    s.sphere_air(c, 3, c, 2.4)
    s.put(c, 7, c, 'y')
    s.put(c, 8, c, 'F')
    s.center = (c, 2, c)
    s.ports('c', 'd', 4, y=0)
    s.v[(c, 1, c - 4)] = 'C'
    s.ctrl = (c, 1, c - 4)
    return s


shapes.BUILDERS['nuclide_forge'] = nuclide_forge
shapes.BUILDERS['evolution_engine'] = evolution_engine
shapes.BUILDERS['abiogenesis_crucible'] = abiogenesis_crucible

CREATURES = {}

LANG = {
    'message.theoreticalsciences.research_needs_proof': 'This research needs its derivation first - click it in the Research Matrix.',
    'puzzle.theoreticalsciences.title': 'Derivation',
    'puzzle.theoreticalsciences.step1': 'Step 1 - Build the equation from the tiles',
    'puzzle.theoreticalsciences.step2': 'Step 2 - Solve it with these values',
    'puzzle.theoreticalsciences.undo': 'Undo',
    'puzzle.theoreticalsciences.clear': 'Clear',
    'puzzle.theoreticalsciences.submit_derivation': 'Check derivation',
    'puzzle.theoreticalsciences.submit_answer': 'Submit answer',
    'puzzle.theoreticalsciences.answer': 'Answer',
    'puzzle.theoreticalsciences.hints': 'Hints',
    'puzzle.theoreticalsciences.hint.v': 'Meaning of symbols (%s Insight)',
    'puzzle.theoreticalsciences.hint.m': 'The method (%s Insight)',
    'puzzle.theoreticalsciences.hint.f': 'First third (%s Insight)',
    'puzzle.theoreticalsciences.hint.e': 'Full equation (%sx %s)',
    'puzzle.theoreticalsciences.skip': 'Creative: skip proof',
    'puzzle.theoreticalsciences.derived': 'Derivation accepted - now solve it.',
    'puzzle.theoreticalsciences.not_equal': 'That expression is not equivalent to the true law.',
    'puzzle.theoreticalsciences.solved': 'Proof complete! The research can begin.',
    'puzzle.theoreticalsciences.wrong_answer': 'Not quite - check your arithmetic (within 1%).',
    'puzzle.theoreticalsciences.need_fee': 'The full equation costs %sx %s.',
    'puzzle.theoreticalsciences.derivation_ok': 'Derivation proven.',
    'puzzle.theoreticalsciences.solved_note': 'Solved.',
    'gui.theoreticalsciences.puzzle_tip': 'Needs a derivation: build and solve its equation.',
    'gui.theoreticalsciences.puzzle_derived_tip': 'Equation derived - solve it to begin.',
    'message.theoreticalsciences.sampled': 'Genome sampled: %s',
    'message.theoreticalsciences.sampled_xeno': 'Xenogenome sampled: %s - it is not written in DNA',
    'message.theoreticalsciences.sample_cooldown': 'The sampler is still sterilising.',
}


def apply(g):
    mr, shaped, text_entry = g['mr'], g['shaped'], g['text_entry']
    q = g['q']
    g['LANG'].update(LANG)
    CR, EL, BR, GS, NF, EV, AB = ('chemical_synthesis', 'electrolysis', 'bioculture', 'gene_sequencing', 'nuclide_synthesis',
                                  'directed_evolution', 'abiogenesis')
    # ================================================================== chemistry
    mr('supercritical_solvent', CR, [('minecraft:charcoal', 4)], [('minecraft:water', 1000)], [], [('supercritical_solvent', 1000)], 2048, 200, 'chemical_bonding')
    mr('zeolite_catalyst', CR, [('minecraft:clay_ball', 4), ('minecraft:quartz', 2), ('cherenkov_quartz', 1)], [('supercritical_solvent', 250)],
       [('zeolite_catalyst', 2)], [], 4096, 300, 'chemical_bonding')
    mr('molecular_orbital_lattice', CR, [('qubit', 2), ('cherenkov_quartz', 2), ('zeolite_catalyst', 1)], [('supercritical_solvent', 500)],
       [('molecular_orbital_lattice', 1)], [], 8192, 300, 'quantum_chemistry')
    mr('chiral_crystal', CR, [('molecular_orbital_lattice', 1), ('minecraft:amethyst_shard', 4), ('quantum_dot', 1)], [('supercritical_solvent', 250)],
       [('chiral_crystal', 2)], [], 8192, 300, 'quantum_chemistry')
    mr('electride_crystal', EL, [('minecraft:redstone', 4), ('cherenkov_quartz', 1)], [('superfluid_helium', 250)], [('electride_crystal', 1)], [], 8192, 300,
       'electrochemistry')
    mr('ionic_liquid', EL, [('minecraft:quartz', 2)], [('supercritical_solvent', 1000)], [], [('ionic_liquid', 1000)], 8192, 200, 'electrochemistry')
    mr('carborane_superacid', EL, [('electride_crystal', 1), ('chiral_crystal', 1)], [('ionic_liquid', 1000)], [], [('carborane_superacid', 500)], 32768, 300,
       'superacid_chemistry')
    mr('graphene_sheet', CR, [('minecraft:coal', 8), ('zeolite_catalyst', 1), ('nanite_swarm', 1)], [('supercritical_solvent', 250)], [('graphene_sheet', 4)], [],
       16384, 300, 'nanochemistry')
    mr('carbon_nanotube_bundle', CR, [('graphene_sheet', 4), ('molecular_orbital_lattice', 1)], [('ionic_liquid', 250)], [('carbon_nanotube_bundle', 2)], [],
       16384, 300, 'nanochemistry')
    mr('buckyball_cage', CR, [('graphene_sheet', 2), ('minecraft:blaze_powder', 1)], [('superfluid_helium', 250)], [('buckyball_cage', 2)], [], 16384, 300,
       'nanochemistry')
    mr('metal_organic_framework', CR, [('carbon_nanotube_bundle', 1), ('chiral_crystal', 1), ('minecraft:copper_ingot', 4)], [('ionic_liquid', 500)],
       [('metal_organic_framework', 1)], [], 65536, 400, 'supramolecular_chemistry')
    mr('noble_gas_clathrate', CR, [('buckyball_cage', 2), ('minecraft:ice', 4)], [('superfluid_helium', 500)], [('noble_gas_clathrate', 2)], [], 32768, 300,
       'supramolecular_chemistry')
    mr('self_healing_polymer', CR, [('carbon_nanotube_bundle', 1), ('minecraft:slime_ball', 2), ('topological_board', 1)], [('ionic_liquid', 250)],
       [('self_healing_polymer', 2)], [], 32768, 300, 'supramolecular_chemistry')
    mr('positronium_hydride', CR, [('antimatter_pellet', 1), ('buckyball_cage', 1)], [('carborane_superacid', 250)], [('positronium_hydride', 1)], [],
       262144, 400, 'exotic_chemistry')
    mr('muonic_catalyst', CR, [('noble_gas_clathrate', 1), ('magnetic_monopole', 1)], [('quark_gluon_plasma', 100)], [('muonic_catalyst', 2)], [], 262144, 400,
       'exotic_chemistry')
    mr('room_temperature_superconductor', CR, [('metal_organic_framework', 2), ('metallic_hydrogen_ingot', 1), ('electride_crystal', 2)],
       [('bose_einstein_condensate', 500)], [('room_temperature_superconductor', 1)], [], 1048576, 600, 'superconductivity')
    mr('flerovium_ingot', NF, [('r_process_cluster', 4), ('muonic_catalyst', 1), ('neutronium_ingot', 8)], [('quark_gluon_plasma', 500)],
       [('flerovium_ingot', 1)], [], 16777216, 800, 'island_of_stability')
    mr('unbihexium_ingot', NF, [('flerovium_ingot', 2), ('supernova_remnant', 2), ('positronium_hydride', 1)], [('quark_gluon_plasma', 1000)],
       [('unbihexium_ingot', 1)], [], 33554432, 1200, 'island_of_stability')
    # ================================================================== biology
    mr('nutrient_broth', BR, [('minecraft:sugar', 4), ('minecraft:bone_meal', 4), ('zeolite_catalyst', 1)], [('minecraft:water', 1000)], [], [('nutrient_broth', 1000)],
       4096, 300, 'molecular_biology')
    shaped('gene_sampler', [' NQ', ' SN', 'S  '], {'N': 'neutronium_plate', 'Q': 'quantum_dot', 'S': 'minecraft:stick'}, 'gene_sampler')
    mr('recombinant_dna', GS, [('genome_sample', 4), ('engineered_enzyme', 1)], [('nutrient_broth', 500)], [('recombinant_dna', 2)], [], 16384, 400, 'genomics')
    mr('engineered_enzyme', BR, [('genome_sample', 2), ('chiral_crystal', 1), ('zeolite_catalyst', 1)], [('nutrient_broth', 500)], [('engineered_enzyme', 2)], [],
       16384, 400, 'genomics')
    mr('folded_protein_crystal', GS, [('recombinant_dna', 1), ('molecular_orbital_lattice', 2), ('quantum_processor', 1)], [('nutrient_broth', 500)],
       [('folded_protein_crystal', 2)], [], 65536, 400, 'protein_folding')
    mr('tardigrade_culture', BR, [('genome_sample', 4), ('engineered_enzyme', 1), ('minecraft:moss_block', 2)], [('superfluid_helium', 500)],
       [('tardigrade_culture', 2)], [], 65536, 600, 'extremophiles')
    mr('quantum_chlorophyll', BR, [('folded_protein_crystal', 1), ('entangled_photon_pair', 2), ('minecraft:kelp', 8)], [('nutrient_broth', 500)],
       [('quantum_chlorophyll', 2)], [], 131072, 400, 'quantum_biology')
    mr('cryptochrome_magnetoreceptor', GS, [('folded_protein_crystal', 2), ('entangled_photon_pair', 2), ('minecraft:feather', 4)], [('nutrient_broth', 500)],
       [('cryptochrome_magnetoreceptor', 1)], [], 131072, 400, 'quantum_biology')
    mr('stem_cell_culture', BR, [('recombinant_dna', 2), ('engineered_enzyme', 2), ('genome_sample', 4)], [('nutrient_broth', 1000)],
       [('stem_cell_culture', 2)], [], 131072, 600, 'synthetic_biology')
    mr('synthetic_cell', GS, [('recombinant_dna', 2), ('buckyball_cage', 2), ('self_healing_polymer', 1)], [('nutrient_broth', 1000)], [('synthetic_cell', 1)], [],
       262144, 600, 'synthetic_biology')
    mr('xenobot_swarm', BR, [('stem_cell_culture', 2), ('nanite_swarm', 2), ('general_ai_core', 1)], [('nutrient_broth', 1000)], [('xenobot_swarm', 1)], [],
       262144, 600, 'synthetic_biology')
    mr('neural_organoid', BR, [('stem_cell_culture', 4), ('thought_matrix', 1), ('room_temperature_superconductor', 1)], [('synaptic_fluid', 500)],
       [('neural_organoid', 1)], [], 524288, 800, 'neurobiology')
    mr('turing_morphogen', CR, [('synthetic_cell', 1), ('complex_plane', 2), ('chiral_crystal', 2)], [('nutrient_broth', 500)], [('turing_morphogen', 2)], [],
       262144, 600, 'morphogenesis')
    mr('xenoplasm', BR, [('xenogenome_sample', 2), ('foam_substrate', 4)], [('nutrient_broth', 1000)], [], [('xenoplasm', 1000)], 524288, 600, 'xenobiology')
    mr('silicon_life_spore', GS, [('xenogenome_sample', 4), ('bulk_crystal', 4), ('synthetic_cell', 1)], [('xenoplasm', 500)], [('silicon_life_spore', 2)], [],
       524288, 800, 'xenobiology')
    mr('telomerase_complex', EV, [('stem_cell_culture', 4), ('tardigrade_culture', 2), ('turing_morphogen', 2), ('arrow_of_time_shard', 2)], [('xenoplasm', 1000)],
       [('telomerase_complex', 1)], [], 4194304, 1200, 'directed_evolution')
    mr('evolved_xenobots', EV, [('xenobot_swarm', 2), ('silicon_life_spore', 2), ('xenogenome_sample', 8), ('neural_organoid', 1)], [('nutrient_broth', 2000)],
       [('xenobot_swarm', 4), ('silicon_life_spore', 2)], [], 4194304, 1200, 'directed_evolution')
    mr('primordial_soup', CR, [('turing_morphogen', 2), ('chiral_crystal', 4), ('noble_gas_clathrate', 2)], [('carborane_superacid', 500)], [],
       [('primordial_soup', 1000)], 1048576, 600, 'abiogenesis')
    mr('spark_of_life', AB, [('telomerase_complex', 2), ('quantum_chlorophyll', 4), ('synthetic_cell', 4), ('omega_point', 1)], [('primordial_soup', 8000), ('stellar_plasma', 2000)],
       [('spark_of_life', 1)], [], 67108864, 2400, 'abiogenesis')
    mr('panspermia_seed', AB, [('spark_of_life', 1), ('tardigrade_culture', 8), ('bubble_universe_sample', 1), ('unbihexium_ingot', 2)], [('xenoplasm', 4000), ('primordial_soup', 4000)],
       [('panspermia_seed', 1)], [], 134217728, 2400, 'panspermia')
    mr('omnicellular_genome', EV, [('panspermia_seed', 1), ('silicon_life_spore', 8), ('neural_organoid', 4), ('recombinant_dna', 64)], [('xenoplasm', 8000)],
       [('omnicellular_genome', 1)], [], 268435456, 4800, 'panspermia')

    # ================================================================== the old chains now need chemistry and biology
    def add_input(name, item, n):
        import glob as _g
        for f in _g.glob(os.path.join(g['REC'], '*', name + '.json')):
            r = json.load(open(f))
            if 'ingredients' in r and r['type'].endswith('extreme_shapeless'):
                # the Extreme Assembly Table reads a shapeless ingredient list (one entry per item, up to 81)
                if not any(i.get('item') == q(item) for i in r['ingredients']):
                    r['ingredients'] += [{'item': q(item)}] * max(0, min(n, 81 - len(r['ingredients'])))
                    g['w'](f, r)
                continue
            if any(i.get('item') == q(item) for i in r.get('item_inputs', [])):
                continue
            cap = {m['recipe']: m['inputs'] for m in g['M']}.get(r['type'].split(':')[1], 9)
            ins = r.setdefault('item_inputs', [])
            if len(ins) + len(r.get('catalysts', [])) >= cap:
                ins[-1] = {'item': q(item), 'count': n}           # the machine is full: the new ingredient takes the last one's place
            else:
                ins.append({'item': q(item), 'count': n})
            g['w'](f, r)
            for rec in g['MACHINE_RECIPES']:
                if rec['name'] == name and item not in rec['inputs']:
                    rec['inputs'].append(item)
    ADD = [('psionic_crystal', 'cryptochrome_magnetoreceptor', 1), ('boltzmann_brain', 'neural_organoid', 1), ('superintelligence_core', 'neural_organoid', 2),
           ('transfinite_coil_block', 'room_temperature_superconductor', 2), ('hypernova_alloy_ingot', 'unbihexium_ingot', 1),
           ('transcendent_metal_ingot', 'flerovium_ingot', 1), ('antimatter_singularity', 'positronium_hydride', 4), ('stellar_singularity', 'muonic_catalyst', 4),
           ('omniscience_catalyst', 'spark_of_life', 1), ('biosphere_compendium', 'omnicellular_genome', 1), ('cognitive_singularity', 'neural_organoid', 8),
           ('grow_bubble_universes', 'omnicellular_genome', 1)]
    g['ROUND7_ADD'] = ADD
    for name, item, n in ADD:
        add_input(name, item, n)

    # ================================================================== the Codex
    text_entry('chemistry', 'bonds', 'Why Atoms Hold Hands', 'chemical_reactor', [
        'Notebook, chemistry, day one. A bond is not a stick between balls. Two atoms share an electron because the shared $(thing)wavefunction$() spread over both has lower energy than either alone - the same quantum mechanics as everything else in this book.',
        'The $(item)Chemical Reactor$() is where it starts: $(fluid)Supercritical Solvent$() (gas-dense, liquid-thin) and $(item)Zeolite Catalysts$(), whose pores let only the right molecules meet.',
        'Next: $(item)Molecular Orbital Lattices$() - the bond itself, frozen - and $(item)Chiral Crystals$(), molecules that come in a left hand and a right hand. Life, I will find, only ever uses one.'],
        adv='chemical_bonding', sort=0, spot=['chemical_reactor', 'zeolite_catalyst', 'molecular_orbital_lattice'])
    text_entry('chemistry', 'electrochem', 'Pushing Electrons', 'electrolytic_cell', [
        'Every reaction is electrons moving from where they are to where they would rather be. The $(item)Electrolytic Cell$() pushes them with a voltage instead of waiting.',
        'It makes $(item)Electride Crystals$() (salts whose negative ions are bare electrons), $(fluid)Ionic Liquid$() (molten salt at room temperature), and with superacid research, $(fluid)Carborane Superacid$() - a million million times stronger than sulfuric acid.'],
        adv='electrochemistry', sort=1, spot=['electrolytic_cell', 'electride_crystal'])
    text_entry('chemistry', 'carbon', 'Carbon, One Atom Thick', 'graphene_sheet', [
        '$(item)Graphene$() is a single sheet of carbon atoms in hexagons. Roll it and you have $(item)Carbon Nanotubes$(); fold sixty atoms into a ball and you have a $(item)Buckyball$() - hollow, with room for one guest atom inside.',
        'Supramolecular chemistry builds with these: $(item)Metal-Organic Frameworks$() (more inner surface per gram than a football pitch), $(item)Noble Gas Clathrates$() (xenon caged in ice) and $(item)Self-Healing Polymers$().'],
        adv='nanochemistry', sort=2, spot=['graphene_sheet', 'carbon_nanotube_bundle', 'buckyball_cage', 'metal_organic_framework'])
    text_entry('chemistry', 'exotic', 'Chemistry Without Electrons', 'positronium_hydride', [
        'Replace the electron and chemistry changes. A $(thing)positron$() makes positronium - an atom of matter and antimatter - and $(item)Positronium Hydride$() is a molecule of it. A $(thing)muon$() is 200 times heavier than an electron, so a $(item)Muonic Catalyst$() squeezes atoms close enough to fuse cold.',
        'A $(item)Room-Temperature Superconductor$() - hydrogen packed into a framework - carries current with zero resistance and no cryogenics.'],
        adv='exotic_chemistry', sort=3, spot=['positronium_hydride', 'muonic_catalyst', 'room_temperature_superconductor'])
    text_entry('chemistry', 'island', 'The Island of Stability', 'nuclide_forge', [
        'Past uranium, elements get heavier and shorter-lived - until, theory says, an island: nuclei with $(thing)magic numbers$() of protons and neutrons, closed shells that hold them together.',
        'The $(item)Nuclide Forge$() fires heavy ions from the r-process into a target. $(item)Flerovium$() (element 114) first, then $(item)Unbihexium$() (126), the heart of the island. Hypernova Alloy and Transcendent Metal now need them.'],
        adv='island_of_stability', sort=4, spot=['nuclide_forge', 'flerovium_ingot', 'unbihexium_ingot'])
    text_entry('biology', 'life', 'Chemistry That Copies Itself', 'bioreactor', [
        'Notebook, biology. Life is chemistry that makes copies of itself, with mistakes. The $(item)Bioreactor$() grows cultures in $(fluid)Nutrient Broth$(); the $(item)Gene Sampler$() takes a genome from any creature you can reach.',
        'Creatures of other dimensions give $(item)Xenogenome Samples$() instead - their code is not DNA. The $(item)Gene Sequencer$() reads and writes genomes: $(item)Recombinant DNA$(), $(item)Engineered Enzymes$(), $(item)Folded Protein Crystals$().'],
        adv='molecular_biology', sort=0, spot=['bioreactor', 'gene_sampler', 'genome_sample', 'gene_sequencer'])
    text_entry('biology', 'quantum_life', 'Life Uses Quantum Mechanics', 'quantum_chlorophyll', [
        'Photosynthesis moves energy through its antenna with near-perfect efficiency by trying every path at once, coherently. A robin feels north through a pair of electrons entangled in a protein in its eye.',
        'From these: $(item)Quantum Chlorophyll$() and the $(item)Cryptochrome Magnetoreceptor$() - which Psionic Crystals now need. $(item)Tardigrade Cultures$() come from the extremophiles: animals that survive vacuum by pausing life itself.'],
        adv='quantum_biology', sort=1, spot=['quantum_chlorophyll', 'cryptochrome_magnetoreceptor', 'tardigrade_culture'])
    text_entry('biology', 'synthetic', 'Writing Life', 'synthetic_cell', [
        'Synthetic biology writes genomes from scratch: the $(item)Minimal Synthetic Cell$(), $(item)Stem Cell Cultures$(), and $(item)Xenobots$() - living machines of cells. $(item)Neural Organoids$() are brains grown in a dish; Boltzmann Brains and superintelligence cores now need them.',
        '$(item)Turing Morphogens$() paint stripes and spots by reaction and diffusion - one fast chemical, one slow. Xenobiology takes samples from the Bulk and the Foam: $(fluid)Xenoplasm$() and $(item)Silicon Life Spores$().'],
        adv='synthetic_biology', sort=2, spot=['synthetic_cell', 'xenobot_swarm', 'neural_organoid', 'silicon_life_spore'])
    text_entry('biology', 'evolution', 'A Million Years a Minute', 'evolution_engine', [
        'Evolution needs generations, and generations need time. The $(item)Evolution Engine$() only runs where time runs at least ten times as fast - inside a $(item)Chronal Field Projector$() field. Bring a Chronal Anchor.',
        'It evolves $(item)Telomerase Complexes$() (cells that never age) and better xenobots. The $(item)Abiogenesis Crucible$() turns $(fluid)Primordial Soup$() into the $(item)Spark of Life$(); panspermia packs life to cross between universes.'],
        adv='directed_evolution', sort=3, spot=['evolution_engine', 'telomerase_complex', 'abiogenesis_crucible', 'spark_of_life'])


def finish(g):
    """After scaling: the Evolution Engine only works in fast time."""
    import glob as _g
    for f in _g.glob(os.path.join(g['REC'], 'directed_evolution', '*.json')):
        r = json.load(open(f))
        r['environment'] = {'time_min': 10}
        g['w'](f, r)
    # the derivation each research needs (tools/puzzles, checked by puzzle_check.py)
    import sys as _s
    _s.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import puzzle_check
    P = puzzle_check.load_all()
    n = 0
    for f in _g.glob(os.path.join(g['D'], 'ts_research', '*.json')):
        rid = os.path.basename(f)[:-5]
        r = json.load(open(f))
        if rid in P and not r.get('grant_only'):
            r['puzzle'] = {k: v for k, v in P[rid].items() if not k.startswith('_')}
            g['w'](f, r)
            n += 1
    print('research puzzles merged:', n)
