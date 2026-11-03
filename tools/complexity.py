"""
Multiblock complexity pass (GT: New Horizons / NuclearCraft particle-chamber style).

* Functional component blocks - accelerator magnets, RF cavities, beam pipes, cryogenics, vacuum, detectors,
  plasma injectors, gravitational lens arrays, Hawking collectors, error-correction cores, spacetime anchors,
  dimensional stabilisers, causal shielding, stellar containment plating - each with its own crafting chain.
* GT-style tiered components: five coil tiers and three field-emitter tiers. The LOWEST tier in a structure is the
  structure's tier: coils gate recipes ("min_tier") and add speed, emitters multiply the parallel limit.
* Three flagship multiblocks: the Synchrotron, the Transcendent Plasma Forge and the Eye of the Event Horizon.

Everything here is data; gen_data.py and add_complexity_content.py consume it.
"""

# ------------------------------------------------------------------------------------------------ content.json
ITEMS = [
    # accelerator chain
    dict(id='dipole_winding', name='Dipole Winding', art='coil', c1='#c06030', c2='#ff7060',
         lore='Superconducting cable wound into a saddle coil. Its field bends a particle beam into a ring.'),
    dict(id='quadrupole_pole_tip', name='Quadrupole Pole Tip', art='plate', c1='#80889a', c2='#ffe080',
         lore='A hyperbolic pole piece ground to within ten nanometres. Four of them squeeze a beam into focus.'),
    dict(id='rf_resonator_cell', name='RF Resonator Cell', art='capacitor', c1='#c08050', c2='#ffb060',
         lore='A pill-box cavity of topological superconductor. The standing wave inside kicks each passing bunch forward.'),
    dict(id='tachyon_klystron', name='Tachyon Klystron', art='emitter', c1='#303848', c2='#ffe14d',
         lore='A microwave amplifier whose electron beam has been replaced with tachyons. It drives RF cavities from slightly in the future.'),
    dict(id='cryocooler_unit', name='Cryocooler Unit', art='negentropy', c1='#a0c0d0', c2='#c0f0ff',
         lore='A closed-cycle superfluid refrigerator. Superconductors stop superconducting without one.'),
    dict(id='turbomolecular_rotor', name='Turbomolecular Rotor', art='spindle', c1='#8890a0', c2='#60ffd0',
         lore='Bladed discs spinning at ninety thousand rpm, batting gas molecules out of the beamline one at a time.'),
    dict(id='scintillator_crystal', name='Scintillator Crystal', art='crystal', c1='#2050ff', c2='#a0e0ff',
         lore='Flashes when a particle passes through it. Count the flashes and you know what was made in the collision.'),
    dict(id='pixel_tracker_module', name='Pixel Tracker Module', art='chip', c1='#20283a', c2='#ffd040',
         lore='A qubit-array sensor that records the exact path of every charged particle crossing it.'),
    # stellar / singularity chain
    dict(id='plasma_nozzle_tip', name='Plasma Nozzle Tip', art='warp', c1='#503020', c2='#ff8040',
         lore='A converging magnetic nozzle that accelerates plasma to a few percent of light speed.'),
    dict(id='gravitic_lens_element', name='Gravitic Lens Element', art='lens', c1='#100820', c2='#a080ff',
         lore='A dark-matter lens trimmed so its Einstein ring falls exactly where you need it.'),
    dict(id='hawking_absorber_plate', name='Hawking Absorber Plate', art='plate', c1='#0a0a0e', c2='#ff9a30',
         lore='Metamaterial tuned to soak up the thermal glow a black hole gives off as it evaporates.'),
    dict(id='error_correction_lattice', name='Error-Correction Lattice', art='hexgrid', c1='#0a1420', c2='#40c0ff',
         lore='Thousands of physical qubits standing guard over one logical qubit, voting out every error.'),
    dict(id='anchor_core', name='Spacetime Anchor Core', art='anchor', c1='#100a20', c2='#80c0ff',
         lore='Negative mass pinned inside spacetime alloy. It holds the metric still while everything around it bends.'),
    dict(id='stabiliser_matrix', name='Dimensional Stabiliser Matrix', art='manifold', c1='#1c1030', c2='#ff40e0',
         lore='Compactified dimensions woven tight with quantum strings, so they stay rolled up under load.'),
    dict(id='causal_shield_lattice', name='Causal Shield Lattice', art='lattice', c1='#2a2010', c2='#ffc23d',
         lore='Chronosteel threaded with stored chronons. Paradoxes that hit it are deflected into somebody else\'s timeline.'),
    dict(id='degenerate_armour_plate', name='Degenerate Armour Plate', art='plate', c1='#303438', c2='#e0f0ff',
         lore='Neutronium held at stellar-core pressure by a spacetime-alloy frame. Starlight does not dent it.'),
    # particles from the Synchrotron
    dict(id='pentaquark_shard', name='Pentaquark Shard', art='shard', c1='#ff4060', c2='#ffd0e0',
         lore='Five quarks bound into one exotic hadron. They only exist for an instant - unless you crystallise them.'),
    dict(id='glueball_core', name='Glueball Core', art='pellet', c1='#40ff80', c2='#e0ffe0',
         lore='A particle made of nothing but the force carriers that hold quarks together.'),
    dict(id='hyperon_crystal', name='Hyperon Crystal', art='crystal', c1='#ff80ff', c2='#fff0ff',
         lore='Strange baryons locked into a lattice. The strangeness makes them heavier than they have any right to be.'),
    # Transcendent Plasma Forge / Eye of the Event Horizon products
    dict(id='hypernova_alloy_ingot', name='Hypernova Alloy Ingot', art='ingot', c1='#ff6080', c2='#ffe0a0',
         lore='Forged in a plasma hotter than a collapsing star. Still glowing, and always will be.'),
    dict(id='transcendent_metal_ingot', name='Transcendent Metal Ingot', art='ingot', c1='#d8d8ff', c2='#ffffff',
         lore='A metal whose properties are set by the forge, not by physics.'),
    dict(id='horizon_shard', name='Horizon Shard', art='shard', c1='#0a0a10', c2='#ffb040',
         lore='A sliver of event horizon brought back out of a simulated universe. Everything that falls into it is remembered.'),
]

COMPONENT_BLOCKS = [
    # tiered coils (art coilblock): the first two already exist
    dict(id='spacetime_alloy_coil_block', name='Spacetime Alloy Coil Block', art='coilblock', c1='#1a2a40', c2='#60d0ff', kind='casing', light=8, hardness=12.0, tool='diamond'),
    dict(id='cosmic_neutronium_coil_block', name='Cosmic Neutronium Coil Block', art='coilblock', c1='#303438', c2='#e0f0ff', kind='casing', light=9, hardness=16.0, tool='diamond'),
    dict(id='transfinite_coil_block', name='Transfinite Coil Block', art='coilblock', c1='#120a24', c2='#d0a0ff', kind='casing', light=10, hardness=20.0, tool='diamond'),
    # tiered field emitters
    dict(id='graviton_field_emitter', name='Graviton Field Emitter', art='emitterblock', c1='#1a1a30', c2='#8ab4ff', kind='casing', light=10, hardness=12.0, tool='diamond'),
    dict(id='axiomatic_field_emitter', name='Axiomatic Field Emitter', art='emitterblock', c1='#100818', c2='#fff0c0', kind='casing', light=12, hardness=20.0, tool='diamond'),
    # functional components
    dict(id='vacuum_beam_pipe', name='Vacuum Beam Pipe', art='beampipe', c1='#303438', c2='#80e0ff', kind='casing', light=4, hardness=6.0, tool='iron'),
    dict(id='dipole_bending_magnet', name='Dipole Bending Magnet', art='dipole', c1='#30343c', c2='#80d0ff', kind='casing', light=4, hardness=8.0, tool='iron'),
    dict(id='quadrupole_focusing_magnet', name='Quadrupole Focusing Magnet', art='quadrupole', c1='#30343c', c2='#ffe080', kind='casing', light=4, hardness=8.0, tool='iron'),
    dict(id='rf_accelerating_cavity', name='RF Accelerating Cavity', art='rfcavity', c1='#40302a', c2='#ffb060', kind='casing', light=6, hardness=8.0, tool='iron'),
    dict(id='cryogenic_cooling_jacket', name='Cryogenic Cooling Jacket', art='cryo', c1='#203040', c2='#c0f0ff', kind='casing', light=3, hardness=6.0, tool='iron'),
    dict(id='turbomolecular_vacuum_pump', name='Turbomolecular Vacuum Pump', art='vacuum', c1='#303440', c2='#60ffd0', kind='casing', light=3, hardness=6.0, tool='iron'),
    dict(id='calorimeter_detector_array', name='Calorimeter Detector Array', art='detector', c1='#20283a', c2='#ffd040', kind='casing', light=5, hardness=6.0, tool='iron'),
    dict(id='plasma_injection_nozzle', name='Plasma Injection Nozzle', art='injector', c1='#2a1810', c2='#ff8040', kind='casing', light=8, hardness=10.0, tool='diamond'),
    dict(id='graviton_lens_array', name='Graviton Lens Array', art='lensarray', c1='#100820', c2='#a080ff', kind='casing', light=6, hardness=10.0, tool='diamond'),
    dict(id='hawking_radiation_collector', name='Hawking Radiation Collector', art='emitterblock', c1='#0a0a0e', c2='#ff9a30', kind='casing', light=8, hardness=10.0, tool='diamond'),
    dict(id='quantum_error_correction_core', name='Quantum Error-Correction Core', art='circuit', c1='#0a1420', c2='#40c0ff', kind='casing', light=6, hardness=8.0, tool='iron'),
    dict(id='spacetime_anchor_pylon', name='Spacetime Anchor Pylon', art='wormframe', c1='#0c0818', c2='#80c0ff', kind='casing', light=6, hardness=14.0, tool='diamond'),
    dict(id='dimensional_stabiliser', name='Dimensional Stabiliser', art='hyperframe', c1='#200830', c2='#ffb0ff', kind='casing', light=7, hardness=14.0, tool='diamond'),
    dict(id='causal_shielding_plate', name='Causal Shielding Plate', art='chrono', c1='#2a2010', c2='#ffe060', kind='casing', light=6, hardness=14.0, tool='diamond'),
    dict(id='stellar_containment_plate', name='Stellar Containment Plate', art='stellarframe', c1='#302418', c2='#ffd080', kind='casing', light=8, hardness=16.0, tool='diamond'),
]

FLUIDS = [
    dict(id='residual_starlight', name='Residual Starlight', c1='#ffd080', c2='#fff8e0', gas=True, light=15, temp=50000, density=-200, viscosity=500, art='stellar'),
    dict(id='hypernova_plasma', name='Hypernova Plasma', c1='#ff4080', c2='#ffd0e0', gas=False, light=15, temp=10000000, density=500, viscosity=800, art='plasma'),
    dict(id='transcendent_plasma', name='Transcendent Plasma', c1='#e0e0ff', c2='#ffffff', gas=False, light=15, temp=100000000, density=100, viscosity=400, art='law'),
    dict(id='raw_star_matter', name='Raw Star Matter', c1='#ff9040', c2='#ffe0a0', gas=False, light=15, temp=15000000, density=15000, viscosity=4000, art='stellar'),
    dict(id='liquid_spacetime', name='Liquid Spacetime', c1='#4060c0', c2='#c0e0ff', gas=False, light=10, temp=3, density=1, viscosity=200, art='exotic'),
]

# ------------------------------------------------------------------------------------------------ multiblock keys
# Tiered families. Lists are lowest tier first; the base number is the tier of the first entry.
COILS = ['superconducting_coil_block', 'chronosteel_coil_block', 'spacetime_alloy_coil_block', 'cosmic_neutronium_coil_block', 'transfinite_coil_block']
EMITTERS = ['field_emitter', 'graviton_field_emitter', 'axiomatic_field_emitter']
KEY_ADD = {
    'K': COILS, 'k': COILS[1:], '3': COILS[2:], '4': COILS[3:],
    'F': EMITTERS,
    'm': ['dipole_bending_magnet'], 'q': ['quadrupole_focusing_magnet'], 'r': ['rf_accelerating_cavity'], 'e': ['vacuum_beam_pipe'],
    'i': ['cryogenic_cooling_jacket'], 'v': ['turbomolecular_vacuum_pump'], 'x': ['calorimeter_detector_array'],
    'j': ['plasma_injection_nozzle'], 'l': ['graviton_lens_array'], 'w': ['hawking_radiation_collector'],
    'n': ['quantum_error_correction_core'], 'y': ['spacetime_anchor_pylon'], 's': ['dimensional_stabiliser'],
    'z': ['causal_shielding_plate'], '1': ['stellar_containment_plate'],
}
TIERS = {'K': ('coil', 1), 'k': ('coil', 2), '3': ('coil', 3), '4': ('coil', 4), 'F': ('emitter', 1)}
COIL_TIER_NAMES = {i + 1: c for i, c in enumerate(COILS)}

# ------------------------------------------------------------------------------------------------ machines
MACHINE_TEXT = {
    'synchrotron': ('Hyperion Synchrotron', 'A ring accelerator built from real beamline parts: bending dipoles, focusing quadrupoles, RF cavities, cryogenics, vacuum pumps and a detector hall. It makes the exotic hadrons nothing else can.'),
    'transcendent_plasma_forge': ('Transcendent Plasma Forge', 'Four plasma injectors fire into a coil-wrapped chamber hotter than a hypernova. Burns catalyst plasmas to forge metals whose properties are chosen rather than discovered.'),
    'eye_of_the_horizon': ('Eye of the Event Horizon', 'A captive black hole wrapped in anchors, lenses and stabilisers, used as a computer: it simulates entire stars, galaxies and universes, and keeps what falls out.'),
}
MACHINE_CAT = {'synchrotron': 'exotic', 'transcendent_plasma_forge': 'stellar', 'eye_of_the_horizon': 'singularities'}
MACHINE_RESEARCH = {'synchrotron': 'accelerator_physics', 'transcendent_plasma_forge': 'stellar_transcendence', 'eye_of_the_horizon': 'horizon_simulation'}
TIER = {'synchrotron': 3, 'transcendent_plasma_forge': 6, 'eye_of_the_horizon': 7}
MC = {
    'synchrotron': (['DRD', 'QSQ', 'DXD'], {'D': 'dipole_bending_magnet', 'R': 'rf_accelerating_cavity', 'Q': 'quadrupole_focusing_magnet',
                                            'S': 'quark_gluon_collider', 'X': 'quantum_processor'}),
    'transcendent_plasma_forge': (['IGI', 'CSC', 'IHI'], {'I': 'plasma_injection_nozzle', 'G': 'glueball_core', 'C': 'cosmic_neutronium_coil_block',
                                                          'S': 'stellar_plasma_crucible', 'H': 'hyperon_crystal'}),
    'eye_of_the_horizon': (['AHA', 'LSL', 'AWA'], {'A': 'spacetime_anchor_pylon', 'H': 'hypernova_alloy_ingot', 'L': 'graviton_lens_array',
                                                   'S': 'kugelblitz_chamber', 'W': 'hawking_radiation_collector'}),
}
EXTRA = {
    'synchrotron': 'Like the collider, but a real beamline. Every part matters: dipoles bend, quadrupoles focus, RF cavities accelerate, the cryo jackets keep the magnets superconducting, the pumps keep the pipe empty, and the detector hall sees what was made.$(br2)Better $(item)coils$() unlock heavier particles: $(item)Glueball Cores$() need tier 2, $(item)Hyperon Crystals$() tier 3.',
    'transcendent_plasma_forge': 'Bootstrap it with $(fluid)Residual Starlight$() made from a Stellar Core. Every forging burns a catalyst plasma and leaves $(fluid)Dimensional Residue$() behind.$(br2)Coil tier sets what it can forge: tier 3 for Hypernova Alloy, tier 4 for Hypernova Plasma, tier 5 (Transfinite Coils) for Transcendent Metal.',
    'eye_of_the_horizon': 'Three simulations, each gated by coil tier: a $(thing)stellar life cycle$() (tier 4), a $(thing)galaxy$() (tier 5) and a whole $(thing)cosmology$() (tier 5). Each run returns a huge spread of materials - and $(item)Horizon Shards$().$(br2)Field emitter tiers multiply its parallels, just like every other multiblock.',
}
# MachineType params: (recipe type, in, fin, out, fout, gen, mb, energy, io, effect, colour)
TUNING_LANG = {'beam_orbit': 'Beam Orbit Correction', 'plasma_confinement': 'Plasma Confinement', 'horizon_symmetry': 'Horizon Symmetry'}

RESEARCH = [
    ('accelerator_physics', 'Accelerator Physics', 'Bend a beam with dipoles, focus it with quadrupoles, push it with RF cavities - and keep it all at two kelvin in a perfect vacuum. Unlocks the beamline components and the Hyperion Synchrotron.',
     'dipole_winding', 7, -4, 45, ['particle_physics', 'plasma_metallurgy'], [('superconducting_coil', 8)]),
    ('component_tiers', 'Component Tiers', 'Better coils run hotter and unlock heavier recipes; better field emitters let a multiblock run more parallels. A structure is only as good as its weakest part.',
     'spacetime_alloy_coil_block', 8, -4, 40, ['assembly_lines'], [('chronosteel_ingot', 8)]),
    ('stellar_transcendence', 'Stellar Transcendence', 'Above stellar-core temperatures, matter stops having fixed properties. Unlocks the Transcendent Plasma Forge.',
     'hypernova_alloy_ingot', 14, 2, 320, ['simulation_hypothesis', 'compact_stars', 'accelerator_physics'], [('glueball_core', 2), ('hyperon_crystal', 2)]),
    ('horizon_simulation', 'Horizon Simulation', 'A black hole is the densest possible computer. Feed it a star and it will simulate one. Unlocks the Eye of the Event Horizon.',
     'horizon_shard', 15, 2, 420, ['stellar_transcendence', 'cosmogenesis'], [('hypernova_alloy_ingot', 4)]),
]

LANG = {
    'status.theoreticalsciences.low_tier': 'Component Tier Too Low',
    'gui.theoreticalsciences.tiers': 'Coils T%s  Emitters T%s',
    'message.theoreticalsciences.structure_tiers': 'Component tiers: coils T%s, field emitters T%s',
    'message.theoreticalsciences.any_tier': '%s (or better)',
    'jei.theoreticalsciences.min_tier': 'Needs coil tier %s+',
    'jei.theoreticalsciences.layer_all': 'All layers',
    'jei.theoreticalsciences.layer': 'Layer %s / %s',
    'jei.theoreticalsciences.reset_view': 'Reset view',
    'jei.theoreticalsciences.port_legend': 'Port allowed',
    'jei.theoreticalsciences.port_allowed': 'An Interface Port may replace this block',
    'jei.theoreticalsciences.preview_help': 'Drag: rotate  |  Right/Shift-drag: pan  |  Scroll: zoom  |  Hover: inspect',
    'jei.theoreticalsciences.tier_family.coil': 'Coil (any tier, lowest one counts):',
    'jei.theoreticalsciences.tier_family.emitter': 'Field Emitter (any tier, lowest one counts):',
    'jei.theoreticalsciences.tier_hint': 'Coils: speed + recipe tier. Emitters: x parallels.',
    'jei.theoreticalsciences.mb_size': '%s x %s x %s  -  %s blocks',
    'jei.theoreticalsciences.multiblock_hint': 'Cyan frames: Interface Port allowed',
}


def recipes(mr, shaped):
    """Crafting chains for every new part. Machines used are all available before the structures that need them."""
    # ---------------------------------------------------------------- beamline parts (tier 2 - before the collider)
    mr('dipole_winding', 'weaving', [('superconducting_coil', 3), ('neutronium_plate', 2), ('metamaterial_sheet', 1)], [('superfluid_helium', 500)],
       [('dipole_winding', 1)], energy=1024, duration=300, research='particle_physics')
    mr('quadrupole_pole_tip', 'casimir_pressing', [('neutronium_plate', 2), ('topological_insulator', 1)], items_out=[('quadrupole_pole_tip', 1)],
       energy=768, duration=240, research='particle_physics')
    mr('rf_resonator_cell', 'crystal_growth', [('topological_insulator', 1), ('cherenkov_quartz', 4)], [('superfluid_helium', 250)],
       [('rf_resonator_cell', 1)], [('warm_helium', 250)], energy=1024, duration=400, research='particle_physics')
    mr('tachyon_klystron', 'tachyon_resonance', [('superconducting_coil', 2), ('quantum_dot', 4)], [('superfluid_helium', 250)],
       [('tachyon_klystron', 1)], energy=2048, duration=300, research='particle_physics')
    mr('cryocooler_unit', 'condensing', [('casimir_plate', 1)], [('superfluid_helium', 1000)], [('cryocooler_unit', 1)], [('warm_helium', 500)],
       energy=512, duration=300, research='cryogenics')
    mr('turbomolecular_rotor', 'planck_cutting', [('neutronium_plate', 1)], [('superfluid_helium', 100)], [('turbomolecular_rotor', 2)], [('warm_helium', 100)],
       energy=512, duration=200, research='precision_lithography')
    mr('scintillator_crystal', 'crystal_growth', [('cherenkov_quartz', 4), ('quantum_dot', 2)], [('superfluid_helium', 250)],
       [('scintillator_crystal', 2)], [('warm_helium', 250)], energy=768, duration=400, research='particle_physics')
    mr('pixel_tracker_module', 'femtofabrication', [('cherenkov_wafer', 1), ('qubit', 2), ('quantum_dot', 4)], [('superconducting_solder', 144)],
       [('pixel_tracker_module', 2)], energy=2048, duration=300, research='particle_physics')
    shaped('vacuum_beam_pipe', ['GMG', 'MCM', 'GMG'], {'G': 'containment_field_glass', 'M': 'metamaterial_sheet', 'C': 'casimir_plate'}, 'vacuum_beam_pipe', 4)
    shaped('dipole_bending_magnet', ['PWP', 'WKW', 'PWP'], {'P': 'neutronium_plate', 'W': 'dipole_winding', 'K': 'superconducting_coil_block'}, 'dipole_bending_magnet', 2)
    shaped('quadrupole_focusing_magnet', ['TCT', 'CKC', 'TCT'], {'T': 'quadrupole_pole_tip', 'C': 'superconducting_coil', 'K': 'spacetime_casing'}, 'quadrupole_focusing_magnet', 2)
    shaped('rf_accelerating_cavity', ['RKR', 'RSR', 'RKR'], {'R': 'rf_resonator_cell', 'K': 'tachyon_klystron', 'S': 'spacetime_casing'}, 'rf_accelerating_cavity', 2)
    shaped('cryogenic_cooling_jacket', ['GUG', 'USU', 'GUG'], {'G': 'containment_field_glass', 'U': 'cryocooler_unit', 'S': 'spacetime_casing'}, 'cryogenic_cooling_jacket', 4)
    shaped('turbomolecular_vacuum_pump', ['NRN', 'RSR', 'NCN'], {'N': 'neutronium_plate', 'R': 'turbomolecular_rotor', 'S': 'spacetime_casing', 'C': 'superconducting_coil'},
           'turbomolecular_vacuum_pump', 2)
    shaped('calorimeter_detector_array', ['XTX', 'TPT', 'XTX'], {'X': 'scintillator_crystal', 'T': 'pixel_tracker_module', 'P': 'quantum_processor'}, 'calorimeter_detector_array', 2)
    # ---------------------------------------------------------------- tier 3+ parts
    mr('plasma_nozzle_tip', 'femtofabrication', [('neutronium_plate', 2), ('tachyon_crystal', 1), ('topological_insulator', 1), ('metamaterial_sheet', 1)],
       [('superconducting_solder', 288)], [('plasma_nozzle_tip', 1)], energy=4096, duration=400, research='plasma_metallurgy')
    shaped('plasma_injection_nozzle', ['NMN', 'MCM', 'NUN'], {'N': 'plasma_nozzle_tip', 'M': 'magnetic_confinement_casing', 'C': 'superconducting_coil_block',
                                                             'U': 'cryocooler_unit'}, 'plasma_injection_nozzle', 2)
    mr('gravitic_lens_element', 'weaving', [('dark_matter_lens', 1), ('metamaterial_sheet', 2), ('entangled_photon_pair', 2)], [('dark_matter', 250)],
       [('gravitic_lens_element', 2)], energy=4096, duration=400, research='singularity_physics')
    shaped('graviton_lens_array', ['LEL', 'EHE', 'LEL'], {'L': 'gravitic_lens_element', 'E': 'event_horizon_casing', 'H': 'hyperframe_casing'}, 'graviton_lens_array', 2)
    mr('hawking_absorber_plate', 'casimir_pressing', [('neutronium_plate', 2), ('metamaterial_sheet', 1)], items_out=[('hawking_absorber_plate', 1)],
       energy=2048, duration=300, research='singularity_physics')
    shaped('hawking_radiation_collector', ['PEP', 'EFE', 'PEP'], {'P': 'hawking_absorber_plate', 'E': 'entangled_photon_pair', 'F': 'field_emitter'},
           'hawking_radiation_collector', 2)
    mr('error_correction_lattice', 'neural_synthesis', [('qubit', 8), ('superposition_crystal', 1), ('quantum_processor', 1)], [('synaptic_fluid', 250)],
       [('error_correction_lattice', 1)], energy=4096, duration=400, research='femtoengineering')
    shaped('quantum_error_correction_core', ['XLX', 'LSL', 'XLX'], {'X': 'quantum_processor', 'L': 'error_correction_lattice', 'S': 'spacetime_casing'},
           'quantum_error_correction_core', 2)
    mr('anchor_core', 'graviton_condensing', [('negative_mass_ingot', 1), ('spacetime_alloy_ingot', 2)], [('exotic_matter', 500)],
       [('anchor_core', 1)], energy=32768, duration=600, research='quantum_gravity')
    shaped('spacetime_anchor_pylon', ['AWA', 'WCW', 'AWA'], {'A': 'anchor_core', 'W': 'hyperframe_casing', 'C': 'chronosteel_coil_block'}, 'spacetime_anchor_pylon', 2)
    mr('stabiliser_matrix', 'string_weaving', [('calabi_yau_manifold', 1), ('quantum_string', 2), ('topological_insulator', 2)], [('exotic_matter', 500)],
       [('stabiliser_matrix', 1)], energy=32768, duration=600, research='string_theory')
    shaped('dimensional_stabiliser', ['HMH', 'MCM', 'HMH'], {'H': 'hyperframe_casing', 'M': 'stabiliser_matrix', 'C': 'field_emitter'}, 'dimensional_stabiliser', 2)
    mr('causal_shield_lattice', 'chrono_inversion', [('chronon_capacitor', 2), ('chronosteel_ingot', 2)], [('chronon_gas', 500)],
       [('causal_shield_lattice', 1)], energy=16384, duration=600, research='chronodynamics')
    shaped('causal_shielding_plate', ['LCL', 'COC', 'LCL'], {'L': 'causal_shield_lattice', 'C': 'chronosteel_ingot', 'O': 'chronoplastic_casing'}, 'causal_shielding_plate', 2)
    mr('degenerate_armour_plate', 'casimir_pressing', [('neutronium_block', 1), ('spacetime_alloy_ingot', 1)], items_out=[('degenerate_armour_plate', 1)],
       energy=8192, duration=400, research='compact_stars')
    shaped('stellar_containment_plate', ['DAD', 'AMA', 'DAD'], {'D': 'degenerate_armour_plate', 'A': 'spacetime_alloy_ingot', 'M': 'magnetic_confinement_casing'},
           'stellar_containment_plate', 4)
    # ---------------------------------------------------------------- tiered coils and emitters (assembly line, GT style)
    mr('spacetime_alloy_coil_block', 'femto_assembly', [('chronosteel_coil_block', 1), ('spacetime_alloy_ingot', 8), ('superconducting_coil', 8),
                                                        ('topological_processor', 1), ('cryocooler_unit', 2)],
       [('exotic_solder', 576), ('superfluid_helium', 1000)], [('spacetime_alloy_coil_block', 1)], energy=262144, duration=600, research='component_tiers')
    mr('cosmic_neutronium_coil_block', 'femto_assembly', [('spacetime_alloy_coil_block', 1), ('cosmic_neutronium_ingot', 8), ('pentaquark_shard', 4),
                                                          ('hyperdimensional_processor', 1), ('cryocooler_unit', 4)],
       [('exotic_solder', 1152), ('superfluid_helium', 2000)], [('cosmic_neutronium_coil_block', 1)], energy=1048576, duration=800, research='component_tiers')
    mr('transfinite_coil_block', 'femto_assembly', [('cosmic_neutronium_coil_block', 1), ('hypernova_alloy_ingot', 8), ('aleph_null_shard', 2),
                                                    ('cosmic_processor', 1), ('hyperon_crystal', 4)],
       [('causal_solder', 1152), ('superfluid_helium', 4000)], [('transfinite_coil_block', 1)], energy=4194304, duration=1200, research='stellar_transcendence')
    mr('graviton_field_emitter', 'femto_assembly', [('field_emitter', 1), ('graviton_crystal', 4), ('gravitic_lens_element', 2),
                                                    ('hyperdimensional_processor', 1), ('hyperon_crystal', 1)],
       [('exotic_solder', 576), ('exotic_matter', 1000)], [('graviton_field_emitter', 1)], energy=1048576, duration=800, research='component_tiers')
    mr('axiomatic_field_emitter', 'femto_assembly', [('graviton_field_emitter', 1), ('axiom_fragment', 2), ('horizon_shard', 2),
                                                     ('transcendent_metal_ingot', 2), ('transfinite_processor', 1)],
       [('causal_solder', 1152), ('primordial_law', 500)], [('axiomatic_field_emitter', 1)], energy=16777216, duration=1200, research='axiom_theory')
    # ---------------------------------------------------------------- Synchrotron: exotic hadrons
    mr('pentaquark_shard', 'synchrotron', [('strange_matter_pellet', 1), ('antimatter_pellet', 1), ('entangled_photon_pair', 4)], [('quark_gluon_plasma', 500)],
       [('pentaquark_shard', 2), ('magnetic_monopole', 1)], [], energy=65536, duration=400, research='accelerator_physics', chances=[1.0, 0.15])
    mr('glueball_core', 'synchrotron', [('qubit', 4), ('strange_matter_pellet', 2)], [('quark_gluon_plasma', 1000)],
       [('glueball_core', 1)], [], energy=131072, duration=600, research='accelerator_physics', min_tier=2)
    mr('hyperon_crystal', 'synchrotron', [('sterile_neutrino', 1), ('strange_matter_pellet', 2), ('tachyon_crystal', 1)], [('strange_matter', 500)],
       [('hyperon_crystal', 1), ('pentaquark_shard', 1)], [], energy=262144, duration=800, research='accelerator_physics', chances=[1.0, 0.25], min_tier=3)
    mr('synchrotron_strange_matter', 'synchrotron', [('neutronium_ingot', 4), ('entangled_photon_pair', 2)], [('quark_gluon_plasma', 250)],
       [('strange_matter_pellet', 2)], [('strange_matter', 100)], energy=32768, duration=300, research='accelerator_physics')
    # ---------------------------------------------------------------- Transcendent Plasma Forge (DTPF-style catalysts)
    mr('residual_starlight', 'transcendent_forging', [('stellar_core', 1), ('magnetar_shard', 4), ('pentaquark_shard', 2)],
       [('stellar_plasma', 4000), ('hawking_radiation', 1000)], [], [('residual_starlight', 2000)], energy=1048576, duration=600,
       research='stellar_transcendence', min_tier=3)
    mr('hypernova_alloy_ingot', 'transcendent_forging', [('cosmic_neutronium_ingot', 8), ('spacetime_alloy_ingot', 8), ('glueball_core', 2), ('hyperon_crystal', 2),
                                                         ('supernova_remnant', 1), ('r_process_cluster', 4)],
       [('residual_starlight', 1000), ('stellar_plasma', 2000)], [('hypernova_alloy_ingot', 4)], [('dimensional_residue', 500)],
       energy=2097152, duration=800, research='stellar_transcendence', min_tier=3)
    mr('forge_cosmic_neutronium', 'transcendent_forging', [('spacetime_alloy_ingot', 16), ('neutronium_block', 8), ('quark_star_core', 1)],
       [('residual_starlight', 500), ('stellar_plasma', 4000)], [('cosmic_neutronium_ingot', 16)], [('dimensional_residue', 250)],
       energy=2097152, duration=600, research='stellar_transcendence', min_tier=3)
    mr('hypernova_plasma', 'transcendent_forging', [('hypernova_alloy_ingot', 2), ('quark_star_core', 1), ('pentaquark_shard', 4)],
       [('residual_starlight', 4000), ('exotic_matter', 1000)], [], [('hypernova_plasma', 2000), ('dimensional_residue', 500)],
       energy=4194304, duration=1000, research='stellar_transcendence', min_tier=4)
    mr('transcendent_metal_ingot', 'transcendent_forging', [('hypernova_alloy_ingot', 8), ('aleph_null_shard', 2), ('russell_paradox_set', 1),
                                                            ('cosmic_processor', 1), ('hyperon_crystal', 4), ('grand_unified_core', 1)],
       [('hypernova_plasma', 1000), ('causal_solder', 576)], [('transcendent_metal_ingot', 2)], [('transcendent_plasma', 250), ('dimensional_residue', 500)],
       energy=16777216, duration=1600, research='stellar_transcendence', min_tier=5)
    # ---------------------------------------------------------------- Eye of the Event Horizon (EOH-style simulations)
    mr('simulate_stellar_lifecycle', 'horizon_simulation', [('stellar_core', 1), ('micro_singularity', 1)], [('stellar_plasma', 16000), ('dark_matter', 4000)],
       [('neutronium_ingot', 64), ('degenerate_matter_chunk', 64), ('magnetar_shard', 8), ('quark_star_core', 2), ('pulsar_timing_crystal', 4), ('horizon_shard', 1)],
       [('metallic_hydrogen', 8000), ('hawking_radiation', 4000), ('raw_star_matter', 2000)],
       energy=16777216, duration=1200, research='horizon_simulation', chances=[1.0, 1.0, 1.0, 0.5, 0.5, 1.0], min_tier=4)
    mr('simulate_galaxy', 'horizon_simulation', [('stellar_core', 2), ('cosmic_string_segment', 4)], [('raw_star_matter', 4000), ('residual_starlight', 1000)],
       [('white_hole_fragment', 4), ('micro_singularity', 2), ('cosmic_string_segment', 8), ('multiverse_shard', 4), ('exotic_matter_crystal', 32), ('horizon_shard', 4)],
       [('exotic_matter', 8000), ('dark_energy', 4000), ('liquid_spacetime', 1000)],
       energy=67108864, duration=1600, research='horizon_simulation', chances=[1.0, 1.0, 1.0, 0.5, 1.0, 1.0], min_tier=5)
    mr('simulate_cosmology', 'horizon_simulation', [('universe_seed', 1), ('horizon_shard', 8)], [('transcendent_plasma', 500), ('liquid_spacetime', 2000)],
       [('pocket_universe', 1), ('heat_death_remnant', 2), ('cosmological_constant', 4), ('spin_foam_lattice', 16), ('graviton_crystal', 16), ('horizon_shard', 16)],
       [('inflaton_field', 8000), ('false_vacuum', 4000)],
       energy=268435456, duration=2400, research='horizon_simulation', min_tier=5)
