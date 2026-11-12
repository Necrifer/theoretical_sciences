package com.theoreticalsciences.machine;

import java.util.Locale;

/**
 * Every processing block in the mod. Single-block machines and multiblock controllers share
 * the same recipe-driven engine; a multiblock just has to be "formed" before it runs.
 */
public enum MachineType {
    //                       recipe type             in fin out fout gen    multiblock                     energy      io     effect             colour
    QUANTUM_ENTANGLER       ("entangling",           2, 0, 2, 0, false, null,                         100_000,    2_000, Effect.NONE,        0x20E0FF),
    CASIMIR_PRESS           ("casimir_pressing",     2, 0, 1, 0, false, null,                         100_000,    2_000, Effect.NONE,        0x7A3CFF),
    BOSE_EINSTEIN_CONDENSER ("condensing",           1, 1, 1, 1, false, null,                         200_000,    4_000, Effect.NONE,        0x7AD8FF),
    METAMATERIAL_WEAVER     ("weaving",              3, 1, 1, 0, false, null,                         200_000,    4_000, Effect.NONE,        0x40FFD0),
    TACHYON_RESONATOR       ("tachyon_resonance",    2, 1, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xFFE14D),
    ANTIMATTER_PENNING_TRAP ("antimatter_trapping",  2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFF3DF0),
    CHRONON_FIELD_INVERTER  ("chrono_inversion",     2, 1, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xFFC23D),
    TOPOLOGICAL_TRANSMUTER  ("phase_transmutation",  3, 1, 2, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFFB030),
    ZERO_POINT_EXTRACTOR    ("zero_point",           1, 0, 0, 0, true,  null,                         500_000,   20_000, Effect.NONE,        0x9A7CFF),
    STRING_RESONANCE_LOOM   ("string_weaving",       3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xFF8AE0),
    GRAVITON_CONDENSER      ("graviton_condensing",  2, 1, 2, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x8AB4FF),
    NEURAL_SYNTHESIZER      ("neural_synthesis",     3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x60FF9A),
    MAXWELL_DEMON_SORTER    ("entropy_sorting",      2, 1, 2, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xFF6040),
    NANITE_ASSEMBLER        ("nanofabrication",      3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x40FFE0),
    NEUTRINO_CONDENSER      ("neutrino_capture",     1, 0, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0x6080FF),
    HOLOGRAPHIC_ENCODER     ("holographic_encoding", 2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x80E0FF),
    HIGGS_FIELD_MODULATOR   ("mass_modulation",      2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xFFEF70),
    PSIONIC_RESONATOR       ("psionic_attunement",   2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xFF80FF),
    LATTICE_GROWTH_VAT      ("crystal_growth",       2, 1, 1, 1, false, null,                         200_000,    4_000, Effect.NONE,        0x9FD8FF),
    PLANCK_CUTTER           ("planck_cutting",       1, 1, 2, 1, false, null,                         200_000,    4_000, Effect.NONE,        0xFFE060),
    QUANTUM_LITHOGRAPHER    ("lithography",          2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xA0FF80),
    FEMTO_FABRICATOR        ("femtofabrication",     4, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x60FFD0),
    CANTOR_DIAGONALIZER     ("transfinite_computation", 4, 1, 2, 0, false, "cantor_diagonalizer",                2_000_000_000, 200_000_000, Effect.NONE,   0xD0C0FF),
    AXIOM_DISTILLER         ("axiom_distillation",   3, 2, 2, 1, false, "axiom_distiller",                 2_000_000_000, 200_000_000, Effect.NONE,      0xA060FF),
    SPECTROGRAPH            ("spectroscopy",         2, 0, 2, 0, false, null,                         100_000,    2_000, Effect.NONE,        0xB080FF),
    // round 38 (1.2): the Tier II instruments of the new sciences (ScienceMachineBlockEntity gives each its physics)
    SEISMOGRAPH             ("seismography",         2, 0, 1, 0, false, null,                         100_000,    2_000, Effect.NONE,        0xC09060),
    DECAY_CHAMBER           ("radioactive_decay",    1, 1, 2, 1, false, null,                         100_000,    2_000, Effect.NONE,        0x90FF60),
    MAGNETIZER              ("magnetizing",          1, 0, 1, 0, false, null,                         200_000,    4_000, Effect.NONE,        0xFF5050),
    INDUCTION_GENERATOR     ("induction",            2, 0, 0, 0, true,  null,                         100_000,    2_000, Effect.NONE,        0x60A0FF),
    GALTON_BOARD            ("galton_sorting",       1, 0, 3, 0, false, null,                         100_000,    2_000, Effect.NONE,        0xF0E0A0),
    BAYESIAN_ANALYZER       ("bayesian_inference",   2, 0, 1, 0, false, null,                         100_000,    2_000, Effect.NONE,        0x80C0FF),
    ATOMIC_CLOCK            ("clock_comparison",     2, 0, 1, 0, false, null,                         100_000,    2_000, Effect.NONE,        0xFFD080),

    FEMTOSCALE_ASSEMBLY_LINE("femto_assembly",       9, 3, 1, 0, false, "femtoscale_assembly_line",  64_000_000,  2_000_000, Effect.ASSEMBLY, 0x60FFD0),
    STELLAR_PLASMA_CRUCIBLE ("plasma_smelting",      3, 2, 2, 2, false, "stellar_plasma_crucible",  32_000_000,  1_000_000, Effect.PLASMA,   0xFF8040),
    FRACTIONATION_SPIRE     ("fractionation",        0, 1, 1, 4, false, "fractionation_spire",      16_000_000,    500_000, Effect.FRACTIONATION, 0x9080FF),
    QUARK_GLUON_COLLIDER    ("collider",             2, 1, 2, 1, false, "quark_gluon_collider",     4_000_000,  100_000, Effect.COLLIDER,    0xFF6A00),
    DARK_MATTER_COLLECTOR   ("halo_collection",      1, 0, 1, 1, false, "dark_matter_collector",    4_000_000,  100_000, Effect.DARK_LENS,   0x6A55FF),
    KUGELBLITZ_CHAMBER      ("kugelblitz",           3, 1, 1, 1, false, "kugelblitz_chamber",      16_000_000,  500_000, Effect.BLACK_HOLE,  0xFFEF9A),
    PENROSE_ERGOSPHERE      ("penrose",              2, 1, 1, 1, true,  "penrose_ergosphere",      64_000_000, 2_000_000, Effect.BLACK_HOLE_DISK, 0xFF9A30),
    WHITE_HOLE_EMITTER      ("white_hole",           2, 1, 3, 2, false, "white_hole_emitter",      32_000_000, 1_000_000, Effect.WHITE_HOLE,  0xFFE9B0),
    TESSERACT_ASSEMBLER     ("hyperdimensional",     4, 2, 1, 1, false, "tesseract_assembler",     64_000_000, 2_000_000, Effect.TESSERACT,   0x40E0FF),
    MULTIVERSE_RIFT         ("rift_harvesting",      2, 1, 4, 1, false, "multiverse_rift",        128_000_000, 4_000_000, Effect.RIFT,        0xC040FF),
    UNIVERSE_FORGE          ("universe_forging",     4, 2, 4, 1, false, "universe_forge",         256_000_000, 8_000_000, Effect.POCKET_UNIVERSE, 0xFFD080),
    MATRIOSHKA_BRAIN        ("matrioshka",           3, 1, 2, 0, false, "matrioshka_brain",       512_000_000, 16_000_000, Effect.MATRIOSHKA, 0x60FF9A),
    DYSON_ARRAY             ("dyson",                3, 0, 1, 0, true,  "dyson_array",          1_000_000_000, 50_000_000, Effect.DYSON,      0xFFB040),
    ANNIHILATION_REACTOR    ("annihilation",         2, 1, 0, 1, true,  "annihilation_reactor",   256_000_000, 16_000_000, Effect.ANNIHILATION, 0xFF40C0),
    NEUTRON_STAR_FORGE      ("stellar_compression",  3, 1, 2, 1, false, "neutron_star_forge",     128_000_000, 4_000_000, Effect.PULSAR,      0x80FFFF),
    WORMHOLE_PROJECTOR      ("wormhole_transit",     2, 1, 4, 1, false, "wormhole_projector",     128_000_000, 4_000_000, Effect.WORMHOLE,    0x80C0FF),
    CAUSALITY_LOOP_ENGINE   ("causality_loop",       2, 1, 2, 1, false, "causality_loop_engine",  256_000_000, 8_000_000, Effect.TIME_LOOP,   0xFFC23D),
    DARK_ENERGY_SIPHON      ("dark_energy_extraction", 1, 0, 1, 1, false, "dark_energy_siphon",   128_000_000, 4_000_000, Effect.EXPANSION,   0x5070FF),
    REALITY_COMPILER        ("reality_compilation",  4, 2, 2, 1, false, "reality_compiler",     1_000_000_000, 50_000_000, Effect.MATRIX,    0x60FF9A),
    AXIOMATIC_BREACH_ENGINE ("reality_breach",       4, 2, 1, 0, false, "axiomatic_breach_engine", 2_000_000_000, 500_000_000, Effect.BREACH, 0xFFF0C0),
    OMNILEX_ENGINE          ("omnilex_simulation",   4, 2, 4, 1, false, "omnilex_engine",       2_000_000_000, 500_000_000, Effect.OMNILEX,   0x80FFE0),
    SYNCHROTRON             ("synchrotron",          3, 1, 2, 1, false, "synchrotron",            64_000_000,  2_000_000, Effect.SYNCHROTRON, 0xFF9A40),
    TRANSCENDENT_PLASMA_FORGE("transcendent_forging", 6, 3, 2, 2, false, "transcendent_plasma_forge", 2_000_000_000, 200_000_000, Effect.STAR_FORGE, 0xFF4080),
    EYE_OF_THE_HORIZON      ("horizon_simulation",   2, 2, 6, 3, false, "eye_of_the_horizon",   2_000_000_000, 500_000_000, Effect.EYE,       0xFFB040),
    PEANO_ENGINE            ("arithmetic", 3, 1, 2, 0, false, null, 200_000, 4_000, Effect.NONE, 0xA0D0FF),
    ANALYTIC_CONTINUATOR    ("analysis", 3, 1, 2, 1, false, null, 400_000, 8_000, Effect.NONE, 0xFFD070),
    PROOF_ASSISTANT         ("formal_proof", 4, 1, 2, 0, false, null, 400_000, 8_000, Effect.NONE, 0x80FF80),
    PARTICLE_SPECTROMETER   ("spectrometry", 2, 1, 3, 1, false, null, 400_000, 8_000, Effect.NONE, 0xFF8060),
    TENSOR_ENGINE           ("tensor_calculus", 3, 1, 1, 0, false, null, 1_000_000, 20_000, Effect.NONE, 0x80C0FF),
    NUCLEOSYNTHESIS_CHAMBER ("nucleosynthesis", 2, 2, 2, 1, false, null, 1_000_000, 20_000, Effect.NONE, 0xFFA040),
    COMPACTIFICATION_LATHE  ("compactification", 3, 1, 1, 1, false, null, 2_000_000, 50_000, Effect.NONE, 0xE080FF),
    CHRONO_LOOM             ("chronoweaving", 3, 1, 1, 1, false, null, 1_000_000, 20_000, Effect.NONE, 0xFFC23D),
    CONCEPT_COMPRESSOR      ("concept_compression", 9, 3, 1, 0, false, "concept_compressor", 2_000_000_000, 500_000_000, Effect.CONCEPT, 0xE0E0FF),
    VACUUM_HARVESTER        ("vacuum_harvesting", 2, 1, 4, 0, false, "vacuum_harvester", 256_000_000, 8_000_000, Effect.FOAM, 0x80F0FF),
    ORDINAL_RECURSOR        ("ordinal_recursion", 3, 1, 1, 0, false, null, 8_000_000, 200_000, Effect.NONE, 0xFFD060),
    RESEARCH_NEXUS          ("research_synthesis", 1, 0, 0, 0, false, "research_nexus", 16_000_000, 400_000, Effect.MATRIOSHKA, 0x80C0FF),
    CHEMICAL_REACTOR        ("chemical_synthesis", 3, 2, 2, 1, false, null, 400_000, 8_000, Effect.NONE, 0x60E0A0),
    ELECTROLYTIC_CELL       ("electrolysis", 2, 1, 2, 1, false, null, 400_000, 8_000, Effect.NONE, 0x4080FF),
    BIOREACTOR              ("bioculture", 3, 2, 2, 1, false, null, 1_000_000, 20_000, Effect.NONE, 0x80FF60),
    GENE_SEQUENCER          ("gene_sequencing", 3, 1, 2, 0, false, null, 1_000_000, 20_000, Effect.NONE, 0xA0FF80),
    NUCLIDE_FORGE           ("nuclide_synthesis", 3, 1, 2, 0, false, "nuclide_forge", 64_000_000, 2_000_000, Effect.COLLIDER, 0xD0E0FF),
    EVOLUTION_ENGINE        ("directed_evolution", 4, 2, 2, 1, false, "evolution_engine", 64_000_000, 2_000_000, Effect.TIME_LOOP, 0x80FFA0),
    ABIOGENESIS_CRUCIBLE    ("abiogenesis", 4, 3, 2, 1, false, "abiogenesis_crucible", 256_000_000, 8_000_000, Effect.PLASMA, 0xFFFFA0),
    ANTIPROTON_DECELERATOR  ("antiproton_deceleration", 2, 1, 2, 1, false, null, 4_000_000, 100_000, Effect.NONE, 0xFF70B0),
    MIRROR_FOUNDRY          ("mirror_synthesis", 4, 2, 2, 1, false, "mirror_foundry", 256_000_000, 8_000_000, Effect.ANNIHILATION, 0xFFB070),
    OMEGA_SIMULATOR         ("omega_simulation", 4, 1, 1, 0, false, null, 256_000_000, 16_000_000, Effect.CONCEPT, 0xFFE080),
    PLEROMA_RECONSTITUTOR   ("pleroma_reconstitution", 6, 2, 1, 0, false, "pleroma_reconstitutor", 2_000_000_000, 500_000_000, Effect.POCKET_UNIVERSE, 0xFFE8A0),
    CONVERGENCE_ENGINE      ("convergence_automation", 13, 1, 1, 0, false, "convergence_engine", 2_000_000_000, 100_000_000, Effect.NONE, 0xB080FF),
    ESSENCE_DISTILLERY      ("essence_distillation", 1, 12, 1, 1, false, "essence_distillery", 256_000_000, 8_000_000, Effect.FRACTIONATION, 0x80FFE0),
    // round 29: the first power there is - burns ordinary furnace fuel, so the mod can start without another mod's generators
    THERMOELECTRIC_GENERATOR("thermoelectric",       1, 0, 0, 0, true,  null,                         200_000,    4_000, Effect.NONE,        0xFF8040),
    // round 32 (round32_branches): the Origin and Finality branches - a universe's beginning and its end
    INFLATION_CHAMBER       ("inflation",            4, 2, 2, 2, false, "inflation_chamber",      256_000_000, 8_000_000, Effect.POCKET_UNIVERSE, 0xFF70D0),
    ENTROPY_HORIZON         ("eschatology",          4, 2, 2, 2, false, "entropy_horizon",        256_000_000, 8_000_000, Effect.EXPANSION,   0x6070C0),
    // round 34 (round34_anomaly): Abnormality & Infection - containing anomalies, and resolving them
    ANOMALY_CONTAINMENT_WARD ("anomaly_containment", 4, 2, 2, 2, false, "anomaly_containment_ward", 256_000_000, 8_000_000, Effect.RIFT, 0x40FFA0),
    ANOMALY_RESOLUTION_ENGINE("anomaly_resolution",  4, 2, 2, 2, false, "anomaly_resolution_engine", 256_000_000, 8_000_000, Effect.MATRIX, 0xFFF0C0),
    // round 42 (1.3.5): the Planetary Interior's machines - each with its own silhouette and physics
    TECTONIC_PRESS          ("tectonic_pressing",    2, 1, 2, 0, false, "tectonic_press",          16_000_000,   500_000, Effect.HEARTH,    0xFF9040),
    DEEP_DRILLING_RIG       ("deep_drilling",        1, 1, 3, 1, false, "deep_drilling_rig",       16_000_000,   500_000, Effect.DRILL_STRING,      0xE0A040),
    GAS_CENTRIFUGE_CASCADE  ("isotope_enrichment",   3, 1, 2, 1, false, "gas_centrifuge_cascade",  16_000_000,   500_000, Effect.CENTRIFUGE,      0x90FF60),
    MOLTEN_SALT_REACTOR     ("molten_salt_fission",  0, 1, 0, 1, true,  "molten_salt_reactor",     64_000_000, 2_000_000, Effect.SALT_GLOW,    0x60FF90),
    MHD_GENERATOR           ("magnetohydrodynamic",  0, 1, 1, 0, true,  "mhd_generator",           64_000_000, 2_000_000, Effect.MHD_FLOW,      0x60A0FF),
    // round 43 (1.3.6): the Forge's last materials, made in bulk - each only once its owner has forged it by hand
    ESCHATON_FOUNDRY        ("eschaton_founding",    6, 2, 1, 0, false, "eschaton_foundry",     2_000_000_000, 500_000_000, Effect.HEAT_DEATH, 0xFFD0A0),
    // round 45 (1.2.0): Astrophysics, Tier II - instruments that need the real sky above them
    REFLECTING_TELESCOPE    ("astrophotography",     2, 0, 2, 0, false, "reflecting_telescope",     4_000_000,   100_000, Effect.STARLIGHT, 0xC0D8FF),
    SOLAR_FURNACE           ("solar_melting",        2, 0, 2, 0, false, "solar_furnace",            4_000_000,   100_000, Effect.SOLAR,     0xFFE080),
    RADIO_TELESCOPE         ("radio_observation",    1, 0, 2, 0, false, "radio_telescope",          8_000_000,   200_000, Effect.RADIO,     0x80FFC0),
    // round 46 (1.5.0): Optics & Crystals - study, grow and cut the crystals
    X_RAY_DIFFRACTOMETER    ("xray_diffraction",     2, 0, 2, 0, false, "x_ray_diffractometer",     4_000_000,   100_000, Effect.DIFFRACTION, 0xA0E0FF),
    CRYSTAL_GROWTH_FURNACE  ("crystal_pulling",      2, 0, 1, 0, false, "crystal_growth_furnace",   8_000_000,   200_000, Effect.CZOCHRALSKI,    0xFFB070),
    OPTICAL_WORKSHOP        ("optical_finishing",    3, 0, 1, 0, false, "optical_workshop",         4_000_000,   100_000, Effect.FRINGES,   0xC8E8FF),
    // round 47 (1.6.0): General Relativity - gravitational waves and the gravitational redshift
    LASER_INTERFEROMETER    ("gw_detection",         2, 0, 1, 0, false, "laser_interferometer",    32_000_000,   800_000, Effect.LASER_ARMS, 0xFF5060),
    POUND_REBKA_TOWER       ("redshift_measurement", 1, 0, 1, 0, false, "pound_rebka_tower",        8_000_000,   200_000, Effect.GAMMA_RISE, 0xE0FF80),
    // round 48 (1.7.0): Probability - Brownian motion and Monte Carlo
    BROWNIAN_MICROSCOPE     ("brownian_tracking",    2, 0, 2, 0, false, "brownian_microscope",      4_000_000,   100_000, Effect.BROWNIAN,  0xF0E0B0),
    MONTE_CARLO_ENGINE      ("monte_carlo",          1, 0, 1, 0, false, "monte_carlo_engine",      16_000_000,   400_000, Effect.MONTE_CARLO, 0x40FF80),
    // round 49 (1.8.0): Electromagnetism - the cyclotron and the Tesla coil
    CYCLOTRON               ("cyclotron_irradiation", 2, 0, 1, 0, false, "cyclotron",               8_000_000,   200_000, Effect.CYCLOTRON, 0xFFB070),
    TESLA_COIL              ("arc_discharge",        1, 1, 1, 0, false, "tesla_coil",               8_000_000,   200_000, Effect.TESLA_ARCS, 0xC0E0FF),
    // round 50 (1.9.0): Unification - quantum mechanics and gravity in one measurement
    NEUTRON_INTERFEROMETER  ("gravity_interferometry", 3, 0, 1, 0, false, "neutron_interferometer", 16_000_000,   400_000, Effect.NEUTRON_PATHS, 0xC0D8FF),
    ANALOG_BLACK_HOLE       ("analog_horizon",       1, 1, 1, 0, false, "analog_black_hole",       16_000_000,   400_000, Effect.SONIC_HORIZON, 0xC0F0FF),
    // round 51 (1.10.0): the Accretion Frontier's astrophysics
    INTERSTELLAR_SCOOP      ("interstellar_scooping", 2, 0, 1, 1, false, null,                        100_000,    2_000, Effect.NONE,        0x8090C0),
    ACCRETION_DISK_REACTOR  ("accretion_power",      1, 1, 1, 1, true,  "accretion_disk_reactor",  64_000_000, 2_000_000, Effect.QUASAR_JETS, 0xFFB060),
    // round 52 (1.11.0): Fields & Nuclei - the rest of Electromagnetism and Nuclear Physics, and the bursts
    TOKAMAK                 ("fusion_power",         1, 2, 1, 2, true,  "tokamak",                128_000_000, 4_000_000, Effect.TOKAMAK_PLASMA, 0xFF80C0),
    PULSED_MAGNET_LAB       ("pulsed_field",         2, 0, 2, 0, false, "pulsed_magnet_lab",        32_000_000, 1_000_000, Effect.PULSED_FIELD, 0x80C0FF),
    HYPERVELOCITY_IMPACT_FORGE("shock_synthesis",    3, 0, 1, 0, false, "hypervelocity_impact_forge", 32_000_000, 1_000_000, Effect.RAILGUN, 0xFFE0A0),
    BURST_OBSERVATORY       ("burst_detection",      2, 0, 1, 0, false, "burst_observatory",         8_000_000,   200_000, Effect.GAMMA_BURST, 0xE0E0FF),
    LASER_CUTTER            ("laser_cutting",        2, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFF5060),
    ISOMER_TRIGGER          ("isomer_triggering",    2, 0, 1, 0, true,  null,                       2_000_000,   40_000, Effect.NONE,        0xE0FF80),
    DECONTAMINATION_SCRUBBER("decontamination",      2, 1, 2, 0, false, null,                         400_000,    8_000, Effect.NONE,        0x60FFA0),
    IRRADIATION_CHAMBER     ("irradiation",          2, 0, 2, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xA0FF40),
    // round 53 (1.12.0): Earth & Spacetime - the rest of Geography and General Relativity
    AQUIFER_PUMP            ("aquifer_pumping",      1, 0, 1, 1, false, null,                         100_000,    2_000, Effect.NONE,        0x4080C0),
    CLIMATE_ENGINE          ("climate_forcing",      2, 1, 2, 0, false, "climate_engine",           16_000_000,   400_000, Effect.CLIMATE,   0xA0D0FF),
    LENSING_CONCENTRATOR    ("lensing",              2, 0, 1, 0, true,  "lensing_concentrator",     64_000_000, 2_000_000, Effect.EINSTEIN_RING, 0xE0E8FF),
    TIME_DILATION_CHAMBER   ("time_dilation",        2, 1, 2, 1, false, "time_dilation_chamber",    32_000_000, 1_000_000, Effect.DILATION_RINGS, 0xFFD080),
    WARP_FIELD_FORGE        ("warp_forging",         3, 1, 1, 0, false, "warp_field_forge",        128_000_000, 4_000_000, Effect.WARP_BUBBLE,  0xC080FF),
    // round 54 (1.13.0): Horizons - where quantum mechanics meets gravity
    UNRUH_ACCELERATOR       ("unruh_heating",        2, 1, 1, 1, false, "unruh_accelerator",        64_000_000, 2_000_000, Effect.UNRUH_BATH, 0xFFB060),
    MICRO_BLACK_HOLE_EVAPORATOR("hawking_evaporation", 2, 0, 1, 1, true, "micro_black_hole_evaporator", 256_000_000, 8_000_000, Effect.HAWKING_SPARK, 0xFFE0A0),
    ER_EPR_WEAVER           ("er_weaving",           2, 0, 1, 0, false, "er_epr_weaver",           128_000_000, 4_000_000, Effect.ER_BRIDGE,  0xFF80FF),
    SPIN_NETWORK_LOOM       ("spin_weaving",         2, 1, 1, 1, false, null,                       4_000_000,   80_000, Effect.NONE,        0xA0C0FF),
    PAGE_CURVE_ANALYZER     ("page_analysis",        2, 0, 2, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0x80E0FF),
    // round 55 (1.14.0): Ensembles - the rest of Statistical Probability
    MARKOV_CHAIN_REFINERY   ("markov_refining",      1, 1, 1, 1, false, "markov_chain_refinery",    32_000_000, 1_000_000, Effect.MARKOV_HOPS, 0xC0C0FF),
    // 1.2.0: Chemistry - restoring worn catalysts
    REGENERATION_KILN       ("catalyst_regeneration", 2, 1, 1, 0, false, null,                        400_000,    8_000, Effect.NONE,        0xFF9040),
    // 1.2.0: Kinetics & Equilibrium
    HABER_BOSCH_REACTOR     ("ammonia_synthesis",    1, 2, 1, 1, false, "haber_bosch_reactor",       8_000_000,   200_000, Effect.NONE,        0xA0C0FF),
    OSTWALD_CONVERTER       ("ammonia_oxidation",    1, 2, 1, 1, false, "ostwald_converter",         8_000_000,   200_000, Effect.NONE,        0xFF8040),
    CONTACT_PROCESS_TOWER   ("contact_process",      1, 3, 1, 1, false, "contact_process_tower",     8_000_000,   200_000, Effect.NONE,        0xFFE060),
    AIR_SEPARATION_UNIT     ("air_separation",       1, 0, 1, 2, false, null,                         400_000,    8_000, Effect.NONE,        0x80C0FF),
    // 1.2.0: Acids, Bases & Solutions (single blocks on a console)
    TITRATION_BENCH         ("titration",            1, 1, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xE080FF),
    BUFFER_TANK             ("buffering",            2, 2, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0x80FFC0),
    BRINE_EVAPORATOR        ("brine_evaporation",    1, 1, 2, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xF0F0E0),
    REVERSE_OSMOSIS_CELL    ("reverse_osmosis",      1, 1, 1, 2, false, null,                         400_000,    8_000, Effect.NONE,        0x60C0FF),
    // 1.2.0: Thermochemistry
    BOMB_CALORIMETER        ("calorimetry",          2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFFB060),
    HEAT_PACK_REACTOR       ("heat_pack",            2, 2, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0x80E0FF),
    // 1.2.0: Electrochemistry
    FUEL_CELL               ("fuel_cell_power",      1, 2, 1, 1, true,  null,                       1_000_000,   20_000, Effect.NONE,        0x80FFA0),
    ELECTROPLATING_BATH     ("electroplating",       2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0x60FFC0),
    // 1.2.0: Analytical Chemistry and the nebula gases
    CHROMATOGRAPHY_COLUMN   ("chromatography",       1, 1, 1, 2, false, "chromatography_column",     8_000_000,   200_000, Effect.NONE,        0x80E0F0),
    MASS_SPECTROMETER       ("mass_spectrometry",    1, 1, 1, 1, false, null,                       1_000_000,   20_000, Effect.NONE,        0xC080FF),
    CLOUD_SIPHON            ("cloud_siphoning",      1, 0, 1, 1, false, null,                         100_000,    2_000, Effect.NONE,        0xA0B0FF),
    // 1.2.0: Catalysis & Surfaces
    CATALYST_FORMULATOR     ("catalyst_formulation", 2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFFC080),
    // 1.2.0: Organic Synthesis & Polymers, Frameworks & Porous Materials
    CRACKING_TOWER          ("cracking",             1, 2, 1, 2, false, "cracking_tower",            8_000_000,   200_000, Effect.NONE,        0xFF9050),
    POLYMERISER             ("polymerisation",       2, 3, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xE0E8F0),
    SYNTHESIS_BENCH         ("organic_synthesis",    2, 2, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xC0A0FF),
    FRAMEWORK_ASSEMBLER     ("framework_assembly",   2, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0x80E0FF),
    QUASICRYSTAL_FURNACE    ("quasicrystal_casting", 3, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFFE0B0),
    // 1.2.0: Inorganic & Coordination, Spectroscopy, Materials & Solid-State
    COORDINATION_REACTOR    ("coordination",         3, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0x4080FF),
    CHELATION_COLUMN        ("chelation",            1, 1, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xA0FFA0),
    INFRARED_SPECTROMETER   ("infrared_spectroscopy", 1, 0, 2, 0, false, null,                        400_000,    8_000, Effect.NONE,        0xFF6040),
    NMR_SPECTROMETER        ("nmr_spectroscopy",     1, 0, 2, 0, false, "nmr_spectrometer",          8_000_000,   200_000, Effect.NONE,        0x80C0FF),
    PHASE_DIAGRAM_KILN      ("eutectic_alloying",    2, 0, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xC0C0C0),
    DOPING_FURNACE          ("doping",               2, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xC0C0FF),
    SINTERING_PRESS         ("sintering",            1, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xF0F0E0),
    // 1.2.0: Titan
    LAKE_PUMP               ("lake_pumping",         1, 0, 1, 1, false, null,                         100_000,    2_000, Effect.NONE,        0xE0A040),
    PHOTOCHEMICAL_REACTOR   ("photolysis",           1, 2, 1, 1, false, null,                         400_000,    8_000, Effect.NONE,        0xC080FF),
    // 1.2.0: Asymmetric Synthesis, Oscillating Reactions, Alkahest
    ASYMMETRIC_REACTOR      ("asymmetric_synthesis", 2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x80FFE0),
    OSCILLATOR_VESSEL       ("oscillation",          1, 1, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFF8040),
    ALKAHEST_STILL          ("alkahest_distillation", 3, 2, 1, 1, false, null,                     16_000_000,  400_000, Effect.NONE,        0xFFE0FF),
    // 1.2.0: Mathematics
    ERATOSTHENES_SIEVE      ("sieving",              2, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0xFFD060),
    GEOMETERS_TABLE         ("construction",         2, 0, 1, 0, false, null,                         400_000,    8_000, Effect.NONE,        0x80D0FF),
    SYMMETRY_LATHE          ("symmetry_turning",     3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xE0A0FF),
    TOPOLOGY_KILN           ("surface_firing",       3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xFF9A60),
    INTEGRATOR              ("integration",          3, 0, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xA0FF80),
    ENZYME_ASSAY_BENCH      ("enzyme_assay",         2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xFFD080),
    STATISTICS_ENGINE       ("statistical_inference", 3, 0, 1, 0, false, null,                      1_000_000,   20_000, Effect.NONE,        0xFFE080),
    CIPHER_LOOM             ("encoding",             3, 0, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0x80FF90),
    // 1.2.0, Mathematics Phase 5: optimisation, graphs, control and games
    OPTIMISER               ("gradient_descent",     3, 0, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xFF9050),
    GRAPH_LOOM              ("graph_weaving",        4, 0, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0x50C0FF),
    STRATEGY_TABLE          ("strategy_play",        3, 0, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xE060A0),
    PATTERN_BASIN           ("pattern_forming",      3, 0, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0x60E0A0),
    // 1.2.0: Biology, Phase 1 (the cell) and Phase 2 (microbes)
    AUTOCLAVE               ("sterilising",          2, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xE0E8F0),
    PATCH_CLAMP_RIG         ("membrane_clamping",    3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xF0D070),
    FERMENTER               ("fermentation",         3, 1, 1, 1, false, null,                       1_000_000,   20_000, Effect.NONE,        0xE0B060),
    PHOTOBIOREACTOR         ("photosynthesis",       3, 1, 1, 1, false, null,                       1_000_000,   20_000, Effect.NONE,        0x60E060),
    RIBOSOME_ASSEMBLER      ("translation",          3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xF080A0),
    FOLDING_CHAMBER         ("protein_folding",      3, 1, 1, 0, false, null,                       1_000_000,   20_000, Effect.NONE,        0xA0A0FF),
    PHAGE_LAB               ("phage_culture",        3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xC060FF),
    MYCELIUM_BED            ("mycelium_growth",      3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xD0C0A0),
    // 1.2.0: Biology, Phase 4 (defence)
    ANTIBODY_BENCH          ("adaptive_immunity",    3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xF0A0F0),
    // 1.2.0: Biology, Phase 3 (the body)
    PERFUSION_RIG           ("perfusion",            3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xF07070),
    REGENERATION_TANK       ("regeneration",         3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xF06090),
    CIRCADIAN_INCUBATOR     ("chronobiology",        3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0xB080FF),
    DOSING_PUMP             ("pharmacokinetics",     3, 1, 1, 0, false, null,                       2_000_000,   40_000, Effect.NONE,        0x80C0A0),
    // 1.2.0: Biology, Phase 5 (life at scale)
    ECOSPHERE               ("ecology",              3, 1, 1, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0x60D060),
    SELECTION_CHAMBER       ("population_genetics",  3, 1, 1, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0xD0B070),
    GENE_CIRCUIT_LOOM       ("gene_circuits",        3, 1, 1, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0x6090D0),
    MIRROR_CULTURE_VAT      ("mirror_life",          3, 1, 1, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0xD0D0F0),
    // 1.2.0: Biology, Phase 4 part 2 (the Anomaly, lived)
    CLONAL_SELECTOR         ("affinity_maturation",  3, 1, 1, 0, false, null,                       4_000_000,   80_000, Effect.NONE,        0xA0B0FF),
    // 1.3.0: Fluids and Flight (appended last, so every older machine keeps its ordinal)
    WIND_TUNNEL             ("aerodynamic_testing",  2, 0, 1, 0, false, "wind_tunnel",              8_000_000,  200_000, Effect.NONE,        0xB0E0FF),
    // 1.3.0: Rocketry
    ROCKET_ASSEMBLY         ("orbital_launch",       2, 2, 1, 0, false, "rocket_assembly",         32_000_000, 1_000_000, Effect.NONE,        0xFFB060),
    // 1.3.0: the Bussard Ramscoop
    BUSSARD_RAMSCOOP        ("ram_scooping",         0, 0, 0, 2, false, "bussard_ramscoop",        16_000_000,   400_000, Effect.NONE,        0xC090FF),
    // 1.3.0: the Living Planet
    WAVE_ENERGY_CONVERTER   ("wave_power",           0, 1, 0, 0, true,  null,                       4_000_000,    80_000, Effect.NONE,        0x4080C0),
    GEOTHERMAL_PLANT        ("geothermal_power",     0, 2, 1, 1, true,  null,                       8_000_000,   160_000, Effect.NONE,        0xE06030),
    ICE_CORE_DRILL          ("ice_core_drilling",    0, 1, 1, 0, false, "ice_core_drill",           4_000_000,    80_000, Effect.NONE,        0xC0E8FF),
    STORM_CHAMBER           ("storm_synthesis",      0, 3, 0, 1, false, "storm_chamber",            8_000_000,   200_000, Effect.NONE,        0xA0A0E0),
    // 1.3.0 (Green Kingdom)
    HYDROPONIC_TOWER        ("hydroponics",          1, 3, 2, 0, false, "hydroponic_tower",         4_000_000,    80_000, Effect.NONE,        0x70C060),
    PHYTOTRON               ("phytotron",            1, 1, 0, 1, false, null,                       2_000_000,    40_000, Effect.NONE,        0x90D070),
    BREEDING_BENCH          ("plant_breeding",       2, 0, 1, 0, false, null,                       1_000_000,    20_000, Effect.NONE,        0x80C050),
    DATING_SPECTROMETER     ("radiometric_dating",   1, 0, 1, 0, false, null,                       1_000_000,    20_000, Effect.NONE,        0xD0A040),
    ANCIENT_DNA_LAB         ("ancient_dna",          1, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xC0D8A0),
    REVIVAL_VAT             ("revival",              3, 1, 1, 0, false, "revival_vat",             16_000_000,   400_000, Effect.NONE,        0x80D0C0),
    // 1.4.0: Mind and Language
    NEURAL_CULTURE_ARRAY    ("neural_culture",       2, 1, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xE8A0C8),
    SYNAPSE_TRAINER         ("synaptic_training",    3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xC090FF),
    PSYCHOPHYSICS_BOOTH     ("psychophysics",        3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xF0D070),
    DECIPHERMENT_DESK       ("decipherment",         3, 0, 1, 0, false, null,                       1_000_000,    20_000, Effect.NONE,        0xD8B880),
    CONNECTOME_ARRAY        ("connectome_mapping",   3, 1, 1, 0, false, "connectome_array",        64_000_000, 1_000_000, Effect.NONE,        0xB070FF),
    // 1.4.0: Engineering of the Large
    LOAD_TEST_RIG           ("load_testing",         2, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xA0A8B0),
    COMPOSITE_LAYUP_PRESS   ("composite_layup",      3, 1, 1, 0, false, null,                       4_000_000,    80_000, Effect.NONE,        0x404850),
    TRIBOMETER              ("tribology",            2, 1, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x8090A0),
    CORROSION_CELL          ("corrosion_testing",    2, 1, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xC07040),
    SPACE_ELEVATOR          ("elevator_lift",        2, 0, 1, 0, false, "space_elevator",          64_000_000, 2_000_000, Effect.NONE,        0xD0E0F0),
    ONEILL_CYLINDER         ("habitat_spin",         3, 1, 2, 0, false, "oneill_cylinder",        256_000_000, 4_000_000, Effect.NONE,        0x80C0A0),
    STELLAR_ENGINE          ("stellar_thrust",       1, 1, 1, 1, true,  "stellar_engine",       2_000_000_000, 200_000_000, Effect.NONE,    0xFFD060),
    // 1.5.0: Small Wonders
    RESONANCE_CHAMBER       ("acoustic_resonance",   1, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x90C8E0),
    HELIOGRAPH              ("heliography",          3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x70F0A0),
    CHAOS_CONTROLLER        ("chaos_control",        3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xF07070),
    KNOT_LOOM               ("knot_tying",           3, 1, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xD0A060),
    AUTOMATON_LATTICE       ("automaton_growth",     3, 0, 1, 0, false, null,                       4_000_000,    80_000, Effect.NONE,        0x70E070),
    CERTIFICATE_VERIFIER    ("certificate_check",    3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x8080F0),
    DOSE_RESPONSE_ASSAY     ("dose_response",        3, 1, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xF0A0A0),
    QUANTUM_COMPUTER        ("quantum_search",       3, 1, 1, 0, false, "quantum_computer",        32_000_000,   800_000, Effect.NONE,        0x80E0FF),
    CT_SCANNER              ("tomography",           2, 0, 1, 0, false, "ct_scanner",               8_000_000,   200_000, Effect.NONE,        0xE0E0F0),
    // 1.6.0: Archaeology
    RADAR_SURVEY_BENCH      ("radar_survey",         3, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x70C0A0),
    LUMINESCENCE_READER     ("luminescence_dating",  2, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x60A0FF),
    TREE_RING_CORRELATOR    ("cross_dating",         2, 0, 1, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0xC09060),
    CONSERVATION_BATH       ("conservation",         2, 0, 2, 0, false, null,                       2_000_000,    40_000, Effect.NONE,        0x60C0C0),
    ISOTOPE_PROVENANCER     ("provenancing",         2, 0, 1, 0, false, null,                       4_000_000,    80_000, Effect.NONE,        0xD080F0),
    BLOOMERY_SHAFT          ("bloomery_smelting",    3, 0, 1, 0, false, "bloomery_shaft",           8_000_000,   200_000, Effect.NONE,        0xFF7030),
    PHOTOGRAMMETRY_GANTRY   ("photogrammetry",       4, 0, 1, 0, false, "photogrammetry_gantry",   16_000_000,   400_000, Effect.NONE,        0xE0D0A0),
    // 1.7.1: the Pleroma Confluence, rebuilt as a multiblock (it was a block with its own screen): five fluids in, Pleroma out
    PLEROMA_CONFLUENCE      ("pleroma_confluence",   2, 5, 1, 1, false, "pleroma_confluence",   2_000_000_000, 200_000_000, Effect.NONE,      0xFFE8C0);

    public enum Effect { NONE, COLLIDER, DARK_LENS, BLACK_HOLE, BLACK_HOLE_DISK, WHITE_HOLE, TESSERACT, RIFT, POCKET_UNIVERSE, MATRIOSHKA, DYSON,
        ANNIHILATION, PULSAR, WORMHOLE, TIME_LOOP, EXPANSION, MATRIX, BREACH, OMNILEX, ASSEMBLY, PLASMA, FRACTIONATION, SYNCHROTRON, STAR_FORGE, EYE, CONCEPT, FOAM, HEARTH,
        STARLIGHT, SOLAR, RADIO, DIFFRACTION, FRINGES, LASER_ARMS, GAMMA_RISE, BROWNIAN, MONTE_CARLO, CYCLOTRON, TESLA_ARCS, NEUTRON_PATHS, SONIC_HORIZON, TOKAMAK_PLASMA, PULSED_FIELD, RAILGUN, CLIMATE,
        SALT_GLOW, CZOCHRALSKI, DRILL_STRING, CENTRIFUGE, MHD_FLOW, HEAT_DEATH, GAMMA_BURST, MARKOV_HOPS, EINSTEIN_RING, DILATION_RINGS,
        WARP_BUBBLE, UNRUH_BATH, HAWKING_SPARK, ER_BRIDGE, QUASAR_JETS }

    public final String recipeType;
    public final int itemInputs, fluidInputs, itemOutputs, fluidOutputs;
    /** The slot counts before round 16 (for re-mapping machines saved then). */
    public final int legacyItemInputs, legacyFluidInputs;
    public final boolean generator;
    public final String multiblock;
    public final int energyCapacity, maxIO;
    public final Effect effect;
    public final int color;

    MachineType(String recipeType, int in, int fin, int out, int fout, boolean gen, String multiblock,
                int energyCapacity, int maxIO, Effect effect, int color) {
        this.recipeType = recipeType;
        // Round 16, GTNH-style: every multiblock except generators has 16 item and 16 fluid inputs (and at least 4 + 4 outputs); single-block
        // machines grow a little. The numbers in the list above are the original ones, kept so old saves can be
        // re-mapped (see MachineBlockEntity.loadAdditional).
        this.legacyItemInputs = in;
        this.legacyFluidInputs = fin;
        boolean mb = multiblock != null && !gen;          // generators burn fuel: their slots stay as they were
        // 1.7.2: the two megastructure generators take Field Syntheses and make a signature material, so they need room:
        // the Stellar Engine 6 slots, the Dyson Array 8 (its second and third are the swarm's, which leaves it six)
        this.itemInputs = mb ? 16 : gen ? Math.max(in, "stellar_thrust".equals(recipeType) ? 6 : "dyson".equals(recipeType) ? 8 : 0)
                : in >= 6 ? in : Math.min(6, Math.max(in + 2, 4));
        this.fluidInputs = mb ? 16 : gen ? fin : fin >= 3 ? fin : fin + 1;
        this.itemOutputs = mb ? Math.max(out, 4) : out;
        this.fluidOutputs = mb ? Math.max(fout, 4) : fout;
        this.generator = gen;
        this.multiblock = multiblock;
        this.energyCapacity = energyCapacity;
        this.maxIO = maxIO;
        this.effect = effect;
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Round 35: the dimensions a machine works in - by dimension id, not biome (null: anywhere). The mod's own: the
     * Omnilex Engine simulates the World Tree and the Ocean of Law, so it needs Hyperdimensional Reality; the Vacuum
     * Fluctuation Harvester harvests the Quantum Foam. A pack changes or lifts this with "dimensions" in ts_machines.
     */
    @javax.annotation.Nullable
    public static java.util.List<net.minecraft.resources.ResourceLocation> allowedDimensions(MachineType t) {
        java.util.List<String> ids = com.theoreticalsciences.custom.MachineParams.get(t).dimensions();
        if (ids == null) ids = t == OMNILEX_ENGINE ? java.util.List.of("theoreticalsciences:hyperdimensional_reality")
                : t == VACUUM_HARVESTER ? java.util.List.of("theoreticalsciences:quantum_foam") : java.util.List.of();
        java.util.List<net.minecraft.resources.ResourceLocation> out = new java.util.ArrayList<>();
        for (String s : ids) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(s);
            if (rl != null) out.add(rl);
        }
        return out.isEmpty() ? null : out;
    }

    /** Whether a machine of this type may run in this world (true when it has no dimension rule). */
    public static boolean allowedIn(MachineType t, net.minecraft.world.level.Level level) {
        var dims = allowedDimensions(t);
        return dims == null || level == null || dims.contains(level.dimension().location());
    }

    /** "the Quantum Foam" - a dimension's name for sentences (another mod's dimension: its id, made readable). */
    public static net.minecraft.network.chat.Component dimensionName(net.minecraft.resources.ResourceLocation id) {
        String path = id.getPath();
        StringBuilder b = new StringBuilder();
        for (String w : path.split("[_/]")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return net.minecraft.network.chat.Component.translatableWithFallback("dimname." + id.getNamespace() + "." + path, b.toString());
    }

    /** "the Quantum Foam or the Bulk". */
    public static net.minecraft.network.chat.Component dimensionList(java.util.List<net.minecraft.resources.ResourceLocation> ids) {
        net.minecraft.network.chat.MutableComponent c = net.minecraft.network.chat.Component.empty();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) c.append(net.minecraft.network.chat.Component.translatable("env.theoreticalsciences.or"));
            c.append(dimensionName(ids.get(i)));
        }
        return c;
    }

    /**
     * 1.2.0: a single-block machine that runs on a physics console like the multiblocks (the Chemical Reactor): it has a
     * controller's block entity, always complete, with no structure, no containment to wear and no upgrades.
     */
    public boolean standaloneConsole() {
        return this == CHEMICAL_REACTOR || this == TITRATION_BENCH || this == BUFFER_TANK || this == BRINE_EVAPORATOR || this == REVERSE_OSMOSIS_CELL
                || this == BOMB_CALORIMETER || this == HEAT_PACK_REACTOR || this == FUEL_CELL || this == ELECTROPLATING_BATH
                || this == MASS_SPECTROMETER || this == CATALYST_FORMULATOR || this == REGENERATION_KILN
                || this == POLYMERISER || this == SYNTHESIS_BENCH || this == FRAMEWORK_ASSEMBLER || this == QUASICRYSTAL_FURNACE
                || this == COORDINATION_REACTOR || this == CHELATION_COLUMN || this == INFRARED_SPECTROMETER || this == PHASE_DIAGRAM_KILN
                || this == DOPING_FURNACE || this == SINTERING_PRESS || this == PHOTOCHEMICAL_REACTOR
                || this == ASYMMETRIC_REACTOR || this == OSCILLATOR_VESSEL || this == ALKAHEST_STILL
                || this == ERATOSTHENES_SIEVE || this == GEOMETERS_TABLE || this == SYMMETRY_LATHE || this == TOPOLOGY_KILN
                || this == INTEGRATOR || this == ENZYME_ASSAY_BENCH || this == STATISTICS_ENGINE || this == CIPHER_LOOM
                || this == OPTIMISER || this == GRAPH_LOOM || this == STRATEGY_TABLE || this == PATTERN_BASIN
                || this == ANALYTIC_CONTINUATOR          // 1.2.0: revamped as a console
                || this == WAVE_ENERGY_CONVERTER || this == GEOTHERMAL_PLANT || this == PHYTOTRON || this == BREEDING_BENCH || this == DATING_SPECTROMETER || this == ANCIENT_DNA_LAB   // 1.3.0
                || this == SYNAPSE_TRAINER || this == PSYCHOPHYSICS_BOOTH || this == DECIPHERMENT_DESK                         // 1.4.0
                || this == LOAD_TEST_RIG || this == COMPOSITE_LAYUP_PRESS || this == TRIBOMETER || this == CORROSION_CELL
                || this == RESONANCE_CHAMBER || this == HELIOGRAPH || this == CHAOS_CONTROLLER || this == KNOT_LOOM            // 1.5.0
                || this == AUTOMATON_LATTICE || this == CERTIFICATE_VERIFIER || this == DOSE_RESPONSE_ASSAY
                || this == RADAR_SURVEY_BENCH || this == LUMINESCENCE_READER || this == TREE_RING_CORRELATOR           // 1.6.0
                || this == CONSERVATION_BATH || this == ISOTOPE_PROVENANCER
                || biology();
    }

    /**
     * 1.2.0: the Biology consoles. They do not fail and release anything; they get contaminated (see
     * ControllerBlockEntity.sterility): each run handles the culture a little, and a machine far from sterile may catch
     * something and stop until it is cleaned.
     */
    public boolean biology() {
        return this == AUTOCLAVE || this == PATCH_CLAMP_RIG || this == FERMENTER || this == PHOTOBIOREACTOR || this == RIBOSOME_ASSEMBLER
                || this == FOLDING_CHAMBER || this == PHAGE_LAB || this == MYCELIUM_BED || this == ANTIBODY_BENCH
                || this == PERFUSION_RIG || this == REGENERATION_TANK || this == CIRCADIAN_INCUBATOR || this == DOSING_PUMP
                || this == ECOSPHERE || this == SELECTION_CHAMBER || this == GENE_CIRCUIT_LOOM || this == MIRROR_CULTURE_VAT || this == CLONAL_SELECTOR
                || this == NEURAL_CULTURE_ARRAY;       // 1.4.0: a dish of living neurons catches what any culture catches
    }

    public boolean isMultiblock() {
        return multiblock != null;
    }

    public int tankCapacity() {
        return Integer.MAX_VALUE;     // round 20: as far as a fluid amount can go (2,147,483,647 mB)
    }

    public int itemSlots() {
        return itemInputs + itemOutputs;
    }

    public int tanks() {
        return fluidInputs + fluidOutputs;
    }
}
