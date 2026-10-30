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
    CANTOR_DIAGONALIZER     ("transfinite_computation", 4, 1, 2, 0, false, null,                  2_000_000_000, 200_000_000, Effect.NONE,   0xD0C0FF),
    AXIOM_DISTILLER         ("axiom_distillation",   3, 2, 2, 1, false, null,                   2_000_000_000, 200_000_000, Effect.NONE,      0xA060FF),

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
    DYSON_ARRAY             ("dyson",                3, 0, 0, 0, true,  "dyson_array",          1_000_000_000, 50_000_000, Effect.DYSON,      0xFFB040),
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
    CONVERGENCE_ENGINE      ("convergence_automation", 13, 1, 1, 0, false, null, 2_000_000_000, 100_000_000, Effect.NONE, 0xB080FF),
    ESSENCE_DISTILLERY      ("essence_distillation", 1, 12, 1, 1, false, "essence_distillery", 256_000_000, 8_000_000, Effect.FRACTIONATION, 0x80FFE0),
    // round 29: the first power there is - burns ordinary furnace fuel, so the mod can start without another mod's generators
    THERMOELECTRIC_GENERATOR("thermoelectric",       1, 0, 0, 0, true,  null,                         200_000,    4_000, Effect.NONE,        0xFF8040),
    // round 32 (round32_branches): the Origin and Finality branches - a universe's beginning and its end
    INFLATION_CHAMBER       ("inflation",            4, 2, 2, 2, false, "inflation_chamber",      256_000_000, 8_000_000, Effect.POCKET_UNIVERSE, 0xFF70D0),
    ENTROPY_HORIZON         ("eschatology",          4, 2, 2, 2, false, "entropy_horizon",        256_000_000, 8_000_000, Effect.EXPANSION,   0x6070C0),
    // round 34 (round34_anomaly): Abnormality & Infection - containing anomalies, and resolving them
    ANOMALY_CONTAINMENT_WARD ("anomaly_containment", 4, 2, 2, 2, false, "anomaly_containment_ward", 256_000_000, 8_000_000, Effect.RIFT, 0x40FFA0),
    ANOMALY_RESOLUTION_ENGINE("anomaly_resolution",  4, 2, 2, 2, false, "anomaly_resolution_engine", 256_000_000, 8_000_000, Effect.MATRIX, 0xFFF0C0);

    public enum Effect { NONE, COLLIDER, DARK_LENS, BLACK_HOLE, BLACK_HOLE_DISK, WHITE_HOLE, TESSERACT, RIFT, POCKET_UNIVERSE, MATRIOSHKA, DYSON,
        ANNIHILATION, PULSAR, WORMHOLE, TIME_LOOP, EXPANSION, MATRIX, BREACH, OMNILEX, ASSEMBLY, PLASMA, FRACTIONATION, SYNCHROTRON, STAR_FORGE, EYE, CONCEPT, FOAM }

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
        this.itemInputs = mb ? 16 : gen ? in : in >= 6 ? in : Math.min(6, Math.max(in + 2, 4));
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
