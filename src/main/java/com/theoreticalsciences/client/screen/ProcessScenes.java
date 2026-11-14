package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.transit.Fx2D;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.5.1: the simulations of every machine from before 1.3.0 that never had a scene of its own. They all fell back to
 * the stirred beaker ("reaction vessel"), or shared one of four generic pictures. Each machine now gets its own
 * picture built from three parts: the vessel it works in (a flask, a column, a Dewar, a petri dish, a blackboard...),
 * what happens inside it, and a second process laid over the first. No two machines share the same three, and each
 * has its own title. tools/check_scenes.py checks that the combinations stay unique.
 */
final class ProcessScenes {
    enum V { FLASK, COLUMN, TANK, KILN, CHAMBER, CELL, PLATE, BOARD, SCOPE, TRAP, LOOM, UTUBE, PIPE, RING, BENCH, DEWAR }

    enum C { NONE, BUBBLES, MOLECULES, BANDS, FLAME, ARCS, PARTICLES, FLOW, CRYSTAL, HELIX, CELLS, GLOW, PEAKS, CURVE,
        SIEVE, GRAPH, WAVES, SPIN, ORBITS, FIELD, DROPS, LATTICE, TILES, BEAM, FOLD, BARS }

    record Spec(String title, V vessel, C main, C over, int k) {}

    static final Map<String, Spec> SPECS = new HashMap<>();

    private static void e(String id, String title, V v, C a, C b, int k) {
        SPECS.put(id, new Spec(title, v, a, b, k));
    }

    static {
        // 1.0 physics
        e("quantum_entangler", "bell pair", V.CHAMBER, C.SPIN, C.BEAM, 0);
        e("casimir_press", "casimir plates", V.CELL, C.WAVES, C.NONE, 1);
        e("bose_einstein_condenser", "condensate", V.DEWAR, C.PARTICLES, C.GLOW, 0);
        e("metamaterial_weaver", "split ring lattice", V.LOOM, C.LATTICE, C.WAVES, 0);
        e("tachyon_resonator", "superluminal cavity", V.CHAMBER, C.WAVES, C.BEAM, 2);
        e("antimatter_penning_trap", "penning trap", V.TRAP, C.ORBITS, C.NONE, 0);
        e("chronon_field_inverter", "time inversion", V.CHAMBER, C.FIELD, C.PARTICLES, 1);
        e("topological_transmuter", "topological phase", V.TANK, C.LATTICE, C.SPIN, 1);
        e("zero_point_extractor", "vacuum modes", V.CELL, C.WAVES, C.PARTICLES, 3);
        e("string_resonance_loom", "string harmonics", V.LOOM, C.WAVES, C.NONE, 1);
        e("graviton_condenser", "graviton capture", V.DEWAR, C.FIELD, C.GLOW, 1);
        e("neural_synthesizer", "neural growth", V.PLATE, C.GRAPH, C.CELLS, 0);
        e("maxwell_demon_sorter", "demon's door", V.UTUBE, C.PARTICLES, C.NONE, 0);
        e("nanite_assembler", "nanoassembly", V.BENCH, C.LATTICE, C.BEAM, 2);
        e("neutrino_condenser", "neutrino capture", V.DEWAR, C.BEAM, C.PARTICLES, 0);
        e("holographic_encoder", "hologram", V.SCOPE, C.WAVES, C.LATTICE, 2);
        e("higgs_field_modulator", "higgs potential", V.SCOPE, C.CURVE, C.GLOW, 8);
        e("psionic_resonator", "psionic field", V.CHAMBER, C.GRAPH, C.WAVES, 3);
        e("lattice_growth_vat", "crystal growth", V.TANK, C.CRYSTAL, C.BUBBLES, 0);
        e("planck_cutter", "planck scale cut", V.BENCH, C.BEAM, C.LATTICE, 0);
        e("quantum_lithographer", "lithography", V.BENCH, C.BEAM, C.TILES, 1);
        e("femto_fabricator", "femtofabrication", V.CHAMBER, C.LATTICE, C.BEAM, 4);
        e("cantor_diagonalizer", "diagonal argument", V.BOARD, C.SIEVE, C.TILES, 1);
        e("axiom_distiller", "axiom distillation", V.COLUMN, C.BANDS, C.DROPS, 2);
        e("spectrograph", "emission spectrum", V.SCOPE, C.PEAKS, C.NONE, 0);
        e("seismograph", "seismic trace", V.SCOPE, C.CURVE, C.NONE, 3);
        e("decay_chamber", "decay curve", V.SCOPE, C.CURVE, C.PARTICLES, 1);
        e("magnetizer", "magnetic domains", V.TANK, C.SPIN, C.FIELD, 0);
        e("induction_generator", "faraday induction", V.CHAMBER, C.FIELD, C.ARCS, 0);
        e("galton_board", "galton board", V.BOARD, C.DROPS, C.CURVE, 2);
        e("bayesian_analyzer", "posterior update", V.SCOPE, C.CURVE, C.PEAKS, 2);
        e("atomic_clock", "hyperfine clock", V.DEWAR, C.SPIN, C.WAVES, 2);
        // 1.0 and 1.1 structures that shared a generic picture
        e("stellar_plasma_crucible", "stellar plasma", V.CHAMBER, C.GLOW, C.ARCS, 1);
        e("fractionation_spire", "fractionation", V.COLUMN, C.BANDS, C.FLAME, 0);
        e("quark_gluon_collider", "quark gluon plasma", V.RING, C.BEAM, C.PARTICLES, 0);
        e("kugelblitz_chamber", "kugelblitz", V.CHAMBER, C.GLOW, C.BEAM, 2);
        e("penrose_ergosphere", "ergosphere", V.RING, C.ORBITS, C.GLOW, 0);
        e("white_hole_emitter", "white hole", V.CHAMBER, C.GLOW, C.PARTICLES, 3);
        e("tesseract_assembler", "tesseract", V.BENCH, C.TILES, C.GLOW, 0);
        e("matrioshka_brain", "nested shells", V.CHAMBER, C.ORBITS, C.GRAPH, 1);
        e("dyson_array", "dyson swarm", V.RING, C.ORBITS, C.PARTICLES, 1);
        e("peano_engine", "successor chain", V.BOARD, C.GRAPH, C.NONE, 1);
        e("analytic_continuator", "analytic continuation", V.BOARD, C.CURVE, C.FIELD, 5);
        e("proof_assistant", "proof tree", V.BOARD, C.GRAPH, C.TILES, 2);
        e("particle_spectrometer", "particle spectrum", V.PIPE, C.BEAM, C.PEAKS, 0);
        e("tensor_engine", "tensor contraction", V.BOARD, C.LATTICE, C.GRAPH, 1);
        e("nucleosynthesis_chamber", "nucleosynthesis", V.CHAMBER, C.MOLECULES, C.GLOW, 2);
        e("compactification_lathe", "compact dimensions", V.LOOM, C.ORBITS, C.WAVES, 0);
        e("chrono_loom", "chrono weave", V.LOOM, C.FIELD, C.NONE, 2);
        e("ordinal_recursor", "ordinal tower", V.BOARD, C.TILES, C.NONE, 3);
        e("research_nexus", "research web", V.CHAMBER, C.GRAPH, C.PARTICLES, 2);
        e("convergence_engine", "convergence", V.CHAMBER, C.FLOW, C.GLOW, 4);
        e("essence_distillery", "essence still", V.FLASK, C.DROPS, C.FLAME, 0);
        e("thermoelectric_generator", "seebeck junction", V.CELL, C.FLOW, C.FLAME, 2);
        e("interstellar_scoop", "interstellar gas", V.PIPE, C.PARTICLES, C.FIELD, 1);
        e("laser_cutter", "laser cut", V.BENCH, C.BEAM, C.ARCS, 3);
        e("isomer_trigger", "isomer trigger", V.CHAMBER, C.ORBITS, C.BEAM, 3);
        e("decontamination_scrubber", "scrubbing", V.COLUMN, C.PARTICLES, C.DROPS, 1);
        e("irradiation_chamber", "irradiation", V.CHAMBER, C.BEAM, C.LATTICE, 5);
        e("aquifer_pump", "aquifer flow", V.PIPE, C.FLOW, C.BUBBLES, 2);
        e("spin_network_loom", "spin network", V.LOOM, C.GRAPH, C.SPIN, 0);
        e("page_curve_analyzer", "page curve", V.SCOPE, C.CURVE, C.FIELD, 9);
        e("antiproton_decelerator", "deceleration", V.RING, C.BEAM, C.NONE, 1);
        // endgame structures whose effect scene another structure already used (the older one keeps it)
        e("evolution_engine", "phylogeny", V.SCOPE, C.HELIX, C.GRAPH, 5);
        e("mirror_foundry", "mirror annihilation", V.RING, C.SPIN, C.GLOW, 6);
        e("omega_simulator", "omega point", V.CHAMBER, C.GRAPH, C.GLOW, 7);
        e("pleroma_reconstitutor", "pleroma", V.FLASK, C.ORBITS, C.GLOW, 8);
        e("inflation_chamber", "cosmic inflation", V.RING, C.PARTICLES, C.GLOW, 9);
        e("entropy_horizon", "heat death", V.RING, C.PARTICLES, C.FIELD, 10);
        e("anomaly_containment_ward", "containment field", V.TRAP, C.ARCS, C.GLOW, 11);
        e("anomaly_resolution_engine", "anomaly resolution", V.TRAP, C.GRAPH, C.SPIN, 12);
        // chemistry
        e("chemical_reactor", "stirred reactor", V.TANK, C.MOLECULES, C.BUBBLES, 0);
        e("electrolytic_cell", "electrolysis", V.CELL, C.BUBBLES, C.ARCS, 0);
        e("nuclide_forge", "nuclide forging", V.CHAMBER, C.MOLECULES, C.ARCS, 3);
        e("regeneration_kiln", "catalyst regeneration", V.KILN, C.CRYSTAL, C.FLAME, 1);
        e("haber_bosch_reactor", "ammonia synthesis", V.COLUMN, C.MOLECULES, C.LATTICE, 0);
        e("ostwald_converter", "ammonia oxidation", V.CHAMBER, C.MOLECULES, C.LATTICE, 1);
        e("contact_process_tower", "contact process", V.COLUMN, C.MOLECULES, C.DROPS, 1);
        e("air_separation_unit", "cryogenic distillation", V.COLUMN, C.BANDS, C.BUBBLES, 1);
        e("titration_bench", "titration", V.FLASK, C.DROPS, C.CURVE, 6);
        e("buffer_tank", "buffering", V.TANK, C.DROPS, C.CURVE, 7);
        e("brine_evaporator", "evaporation", V.TANK, C.CRYSTAL, C.FLAME, 2);
        e("reverse_osmosis_cell", "reverse osmosis", V.UTUBE, C.MOLECULES, C.FLOW, 0);
        e("bomb_calorimeter", "calorimetry", V.DEWAR, C.FLAME, C.NONE, 0);
        e("heat_pack_reactor", "crystallising heat", V.FLASK, C.CRYSTAL, C.GLOW, 0);
        e("fuel_cell", "fuel cell", V.CELL, C.MOLECULES, C.FLOW, 1);
        e("electroplating_bath", "electroplating", V.CELL, C.PARTICLES, C.CRYSTAL, 1);
        e("chromatography_column", "chromatography", V.COLUMN, C.BANDS, C.NONE, 3);
        e("mass_spectrometer", "magnetic sector", V.PIPE, C.BEAM, C.FIELD, 2);
        e("cloud_siphon", "nebula siphon", V.PIPE, C.PARTICLES, C.FLOW, 3);
        e("catalyst_formulator", "catalyst surface", V.BENCH, C.MOLECULES, C.LATTICE, 2);
        e("cracking_tower", "cracking", V.COLUMN, C.MOLECULES, C.FLAME, 2);
        e("polymeriser", "polymerisation", V.TANK, C.FOLD, C.NONE, 0);
        e("synthesis_bench", "synthesis", V.BENCH, C.MOLECULES, C.FLAME, 3);
        e("framework_assembler", "framework", V.BENCH, C.LATTICE, C.MOLECULES, 4);
        e("quasicrystal_furnace", "quasicrystal", V.KILN, C.TILES, C.NONE, 0);
        e("coordination_reactor", "coordination sphere", V.FLASK, C.MOLECULES, C.ORBITS, 4);
        e("chelation_column", "chelation", V.COLUMN, C.MOLECULES, C.BANDS, 5);
        e("infrared_spectrometer", "infrared spectrum", V.SCOPE, C.PEAKS, C.WAVES, 1);
        e("nmr_spectrometer", "nuclear resonance", V.DEWAR, C.SPIN, C.PEAKS, 3);
        e("phase_diagram_kiln", "phase change", V.KILN, C.LATTICE, C.FLAME, 0);
        e("doping_furnace", "doping", V.KILN, C.LATTICE, C.PARTICLES, 1);
        e("sintering_press", "sintering", V.KILN, C.CRYSTAL, C.PARTICLES, 3);
        e("lake_pump", "lake water", V.PIPE, C.FLOW, C.NONE, 4);
        e("photochemical_reactor", "photochemistry", V.FLASK, C.MOLECULES, C.BEAM, 5);
        e("asymmetric_reactor", "chirality", V.FLASK, C.MOLECULES, C.SPIN, 6);
        e("oscillator_vessel", "chemical clock", V.PLATE, C.WAVES, C.NONE, 0);
        e("alkahest_still", "alkahest", V.FLASK, C.DROPS, C.GLOW, 1);
        e("autoclave", "autoclave", V.TANK, C.BUBBLES, C.FLAME, 1);
        // mathematics
        e("eratosthenes_sieve", "prime sieve", V.BOARD, C.SIEVE, C.NONE, 0);
        e("geometers_table", "compass construction", V.BOARD, C.ORBITS, C.TILES, 0);
        e("symmetry_lathe", "symmetry group", V.CHAMBER, C.TILES, C.NONE, 1);
        e("topology_kiln", "deformation", V.KILN, C.FOLD, C.NONE, 1);
        e("integrator", "riemann sum", V.SCOPE, C.BARS, C.CURVE, 0);
        e("enzyme_assay_bench", "michaelis menten", V.SCOPE, C.CURVE, C.MOLECULES, 4);
        e("statistics_engine", "histogram", V.SCOPE, C.BARS, C.NONE, 1);
        e("cipher_loom", "cipher", V.LOOM, C.SIEVE, C.NONE, 2);
        e("optimiser", "gradient descent", V.BOARD, C.FIELD, C.PARTICLES, 3);
        e("graph_loom", "graph", V.LOOM, C.GRAPH, C.NONE, 3);
        e("strategy_table", "game tree", V.BENCH, C.GRAPH, C.NONE, 4);
        e("pattern_basin", "turing pattern", V.PLATE, C.TILES, C.NONE, 2);
        // 1.6.0: archaeology
        e("radar_survey_bench", "radar echoes", V.SCOPE, C.WAVES, C.PEAKS, 11);
        e("luminescence_reader", "glow curve", V.CHAMBER, C.GLOW, C.CURVE, 2);
        e("tree_ring_correlator", "cross-dating", V.BENCH, C.ORBITS, C.CURVE, 3);
        e("conservation_bath", "desalination", V.TANK, C.FLOW, C.CURVE, 1);
        e("isotope_provenancer", "isotope ratios", V.PIPE, C.BEAM, C.BARS, 1);
        e("bloomery_shaft", "bloomery", V.KILN, C.FLAME, C.GLOW, 4);
        e("propulsion_test_stand", "static fire", V.PIPE, C.FLAME, C.FLOW, 5);   // 1.12.0
        e("photogrammetry_gantry", "stereo survey", V.BOARD, C.BANDS, C.BEAM, 5);
        // 1.7.1
        e("pleroma_confluence", "confluence", V.UTUBE, C.FLOW, C.GLOW, 6);
        // biology
        e("bioreactor", "bioreactor", V.TANK, C.CELLS, C.BUBBLES, 0);
        e("gene_sequencer", "sequencing", V.SCOPE, C.HELIX, C.PEAKS, 0);
        e("abiogenesis_crucible", "first cells", V.FLASK, C.CELLS, C.ARCS, 1);
        e("patch_clamp_rig", "patch clamp", V.PLATE, C.CELLS, C.ARCS, 2);
        e("fermenter", "fermentation", V.FLASK, C.CELLS, C.BUBBLES, 3);
        e("photobioreactor", "algae culture", V.PIPE, C.CELLS, C.FLOW, 4);
        e("ribosome_assembler", "translation", V.BENCH, C.HELIX, C.FOLD, 1);
        e("folding_chamber", "protein folding", V.CHAMBER, C.FOLD, C.NONE, 2);
        e("phage_lab", "phage infection", V.PLATE, C.CELLS, C.PARTICLES, 5);
        e("mycelium_bed", "mycelium", V.PLATE, C.GRAPH, C.DROPS, 6);
        e("antibody_bench", "antibody binding", V.PLATE, C.MOLECULES, C.CELLS, 7);
        e("perfusion_rig", "perfusion", V.PIPE, C.FLOW, C.CELLS, 5);
        e("regeneration_tank", "regeneration", V.TANK, C.CELLS, C.GLOW, 6);
        e("circadian_incubator", "circadian rhythm", V.CHAMBER, C.WAVES, C.CELLS, 4);
        e("dosing_pump", "dosing", V.PIPE, C.DROPS, C.NONE, 6);
        e("ecosphere", "ecosphere", V.FLASK, C.CELLS, C.FLOW, 7);
        e("selection_chamber", "selection", V.PLATE, C.CELLS, C.BARS, 8);
        e("gene_circuit_loom", "gene circuit", V.LOOM, C.HELIX, C.GRAPH, 4);
        e("mirror_culture_vat", "mirror life", V.TANK, C.CELLS, C.SPIN, 9);
        e("clonal_selector", "clonal selection", V.BENCH, C.CELLS, C.PEAKS, 10);
    }

    private final Fx2D fx;
    private final float cx, cy, R, f, t, lit;
    private final int col, hot, k;
    private final boolean run;
    private float x0, y0, x1, y1;                // the inside of the vessel, where the process is drawn
    private static final float TAU = (float) (Math.PI * 2);
    private static final int W = 0xFFFFFF;

    private ProcessScenes(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase, int k) {
        this.fx = fx;
        this.cx = cx;
        this.cy = cy;
        this.R = R;
        this.col = col;
        this.hot = hot;
        this.run = run;
        this.f = f;
        this.t = phase * 0.05F;
        this.lit = run ? 1F : 0.35F;
        this.k = k;
    }

    static String title(String id) {
        Spec s = SPECS.get(id);
        return s == null ? null : s.title();
    }

    static boolean draw(Fx2D fx, String id, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase) {
        Spec s = SPECS.get(id.toLowerCase(Locale.ROOT));
        if (s == null) return false;
        ProcessScenes p = new ProcessScenes(fx, cx, cy, R, col, hot, run, f, phase, s.k());
        p.vessel(s.vessel());
        p.content(s.main(), true);
        if (s.over() != C.NONE) p.content(s.over(), false);
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private static float sin(double a) {
        return (float) Math.sin(a);
    }

    private static float cos(double a) {
        return (float) Math.cos(a);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static float rnd(int i, int j) {
        int h = i * 374761393 + j * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0x1000000;
    }

    private float p() {
        return run ? f : 0;
    }

    private float w() {
        return x1 - x0;
    }

    private float h() {
        return y1 - y0;
    }

    private void dot(float x, float y, float r, int c, float a) {
        fx.disc(x, y, r, 10, c, a * lit, c, a * lit * 0.3F);
    }

    private void box(float ax, float ay, float bx, float by, float wd, int c, float a) {
        fx.line(ax, ay, bx, ay, wd, c, a);
        fx.line(bx, ay, bx, by, wd, c, a);
        fx.line(bx, by, ax, by, wd, c, a);
        fx.line(ax, by, ax, ay, wd, c, a);
    }

    private void inside(float ax, float ay, float bx, float by) {
        x0 = ax; y0 = ay; x1 = bx; y1 = by;
    }

    // ------------------------------------------------------------------ vessels
    private void vessel(V v) {
        float a = 0.55F;
        switch (v) {
            case FLASK -> {
                float r = R * 0.62F, fy = cy + R * 0.25F;
                fx.ring(cx, fy, r, 1.2F, 40, col, a);
                fx.line(cx - r * 0.3F, fy - r * 0.95F, cx - r * 0.3F, cy - R, 1.2F, col, a);
                fx.line(cx + r * 0.3F, fy - r * 0.95F, cx + r * 0.3F, cy - R, 1.2F, col, a);
                inside(cx - r * 0.7F, fy - r * 0.6F, cx + r * 0.7F, fy + r * 0.7F);
            }
            case COLUMN -> {
                float hw = R * 0.32F;
                box(cx - hw, cy - R, cx + hw, cy + R, 1.2F, col, a);
                for (int i = 1; i < 6; i++) fx.line(cx - hw, cy - R + i * R / 3, cx + hw, cy - R + i * R / 3, 0.6F, col, 0.2F);
                fx.line(cx + hw, cy - R * 0.8F, cx + hw + 8, cy - R * 0.8F, 1F, col, a);
                fx.line(cx - hw, cy + R * 0.7F, cx - hw - 8, cy + R * 0.7F, 1F, col, a);
                inside(cx - hw + 2, cy - R + 2, cx + hw - 2, cy + R - 2);
            }
            case TANK -> {
                box(cx - R * 0.9F, cy - R * 0.55F, cx + R * 0.9F, cy + R * 0.8F, 1.2F, col, a);
                fx.line(cx, cy - R * 0.9F, cx, cy + R * 0.4F, 1F, col, 0.4F);                 // the stirrer
                float s = t * 3;
                fx.line(cx - R * 0.3F * cos(s), cy + R * 0.4F, cx + R * 0.3F * cos(s), cy + R * 0.4F, 1.6F, col, 0.5F);
                inside(cx - R * 0.85F, cy - R * 0.45F, cx + R * 0.85F, cy + R * 0.75F);
            }
            case KILN -> {
                fx.band(cx, cy, R * 0.8F, R * 0.86F, 1, (float) Math.PI, (float) Math.PI, 20, col, a, col, a);
                fx.line(cx - R * 0.83F, cy, cx - R * 0.83F, cy + R * 0.8F, 1.2F, col, a);
                fx.line(cx + R * 0.83F, cy, cx + R * 0.83F, cy + R * 0.8F, 1.2F, col, a);
                fx.line(cx - R * 0.95F, cy + R * 0.8F, cx + R * 0.95F, cy + R * 0.8F, 1.6F, col, a);
                for (int i = 0; i < 7; i++) fx.line(cx - R * 0.7F + i * R * 0.23F, cy + R * 0.86F, cx - R * 0.7F + i * R * 0.23F, cy + R * 0.95F, 1F, 0xFF8030, 0.4F * lit);
                inside(cx - R * 0.65F, cy - R * 0.55F, cx + R * 0.65F, cy + R * 0.7F);
            }
            case CHAMBER -> {
                fx.ring(cx, cy, R * 0.88F, 1.4F, 48, col, a);
                for (int i = 0; i < 4; i++) {
                    float an = i * TAU / 4 + TAU / 8;
                    fx.line(cx + cos(an) * R * 0.88F, cy + sin(an) * R * 0.88F, cx + cos(an) * R, cy + sin(an) * R, 2.5F, col, a);
                }
                inside(cx - R * 0.6F, cy - R * 0.6F, cx + R * 0.6F, cy + R * 0.6F);
            }
            case CELL -> {
                box(cx - R * 0.9F, cy - R * 0.3F, cx + R * 0.9F, cy + R * 0.8F, 1.2F, col, a);
                fx.rect(cx - R * 0.6F, cy - R * 0.7F, cx - R * 0.5F, cy + R * 0.6F, col, 0.5F);
                fx.rect(cx + R * 0.5F, cy - R * 0.7F, cx + R * 0.6F, cy + R * 0.6F, hot, 0.5F);
                fx.line(cx - R * 0.55F, cy - R * 0.7F, cx + R * 0.55F, cy - R * 0.7F, 1F, col, 0.4F);
                inside(cx - R * 0.45F, cy - R * 0.25F, cx + R * 0.45F, cy + R * 0.75F);
            }
            case PLATE -> {
                fx.band(cx, cy, R * 0.92F, R * 0.97F, 0.55F, 0, TAU, 48, col, a, col, a);
                fx.band(cx, cy + 4, R * 0.92F, R * 0.97F, 0.55F, 0, (float) Math.PI, 24, col, 0.3F, col, 0.3F);
                inside(cx - R * 0.7F, cy - R * 0.35F, cx + R * 0.7F, cy + R * 0.35F);
            }
            case BOARD -> {
                box(cx - R, cy - R * 0.75F, cx + R, cy + R * 0.75F, 1.6F, 0xC8A060, 0.45F);
                fx.rect(cx - R + 1, cy - R * 0.75F + 1, cx + R - 1, cy + R * 0.75F - 1, 0x204030, 0.25F);
                inside(cx - R * 0.9F, cy - R * 0.65F, cx + R * 0.9F, cy + R * 0.65F);
            }
            case SCOPE -> {
                float ax = cx - R, ay = cy - R * 0.7F, bx = cx + R, by = cy + R * 0.7F;
                fx.line(ax, by, bx, by, 1F, col, a);
                fx.line(ax, ay, ax, by, 1F, col, a);
                for (int i = 1; i < 5; i++) fx.line(ax + i * (bx - ax) / 5, ay, ax + i * (bx - ax) / 5, by, 0.5F, col, 0.1F);
                inside(ax + 2, ay, bx, by - 1);
            }
            case TRAP -> {
                fx.band(cx, cy - R * 0.3F, R * 0.5F, R * 0.55F, 0.6F, (float) Math.PI, (float) Math.PI, 20, col, a, col, a);
                fx.band(cx, cy + R * 0.3F, R * 0.5F, R * 0.55F, 0.6F, 0, (float) Math.PI, 20, col, a, col, a);
                fx.band(cx, cy, R * 0.85F, R * 0.9F, 0.35F, 0, TAU, 40, hot, 0.4F, hot, 0.4F);
                inside(cx - R * 0.45F, cy - R * 0.25F, cx + R * 0.45F, cy + R * 0.25F);
            }
            case LOOM -> {
                box(cx - R, cy - R * 0.75F, cx + R, cy + R * 0.75F, 1.4F, col, a);
                for (int i = 1; i < 12; i++) {
                    float x = cx - R + i * R / 6;
                    fx.line(x, cy - R * 0.75F, x, cy + R * 0.75F, 0.5F, col, 0.18F);
                }
                float sx = cx - R + 2 * R * frac(t * 0.3F);
                fx.rect(sx - 3, cy - 2, sx + 3, cy + 2, hot, 0.6F * lit);                     // the shuttle
                inside(cx - R * 0.9F, cy - R * 0.65F, cx + R * 0.9F, cy + R * 0.65F);
            }
            case UTUBE -> {
                float lx = cx - R * 0.55F, rx = cx + R * 0.55F;
                fx.line(lx - 8, cy - R, lx - 8, cy + R * 0.4F, 1.2F, col, a);
                fx.line(rx + 8, cy - R, rx + 8, cy + R * 0.4F, 1.2F, col, a);
                fx.band(cx, cy + R * 0.4F, R * 0.55F - 8, R * 0.55F + 8, 0.8F, 0, (float) Math.PI, 20, col, 0.2F, col, 0.2F);
                fx.line(cx, cy - R * 0.9F, cx, cy + R * 0.6F, 1F, hot, 0.6F);                // the membrane
                for (int i = 0; i < 8; i++) fx.line(cx - 2, cy - R * 0.9F + i * R * 0.2F, cx + 2, cy - R * 0.9F + i * R * 0.2F, 0.6F, hot, 0.4F);
                inside(lx - 6, cy - R * 0.8F, rx + 6, cy + R * 0.4F);
            }
            case PIPE -> {
                fx.line(cx - R, cy - R * 0.3F, cx + R, cy - R * 0.3F, 1.2F, col, a);
                fx.line(cx - R, cy + R * 0.3F, cx + R, cy + R * 0.3F, 1.2F, col, a);
                for (int i = 0; i < 3; i++) {
                    float x = cx - R * 0.6F + i * R * 0.6F;
                    fx.rect(x - 2, cy - R * 0.4F, x + 2, cy - R * 0.3F, col, 0.6F);
                    fx.rect(x - 2, cy + R * 0.3F, x + 2, cy + R * 0.4F, col, 0.6F);
                }
                inside(cx - R, cy - R * 0.27F, cx + R, cy + R * 0.27F);
            }
            case RING -> {
                fx.band(cx, cy, R * 0.75F, R * 0.95F, 0.45F, 0, TAU, 48, col, 0.25F, col, 0.5F);
                inside(cx - R * 0.55F, cy - R * 0.3F, cx + R * 0.55F, cy + R * 0.3F);
            }
            case BENCH -> {
                fx.line(cx - R, cy + R * 0.6F, cx + R, cy + R * 0.6F, 2F, col, a);
                fx.line(cx - R * 0.85F, cy + R * 0.6F, cx - R * 0.85F, cy + R, 1.2F, col, a);
                fx.line(cx + R * 0.85F, cy + R * 0.6F, cx + R * 0.85F, cy + R, 1.2F, col, a);
                fx.line(cx + R * 0.7F, cy - R, cx + R * 0.7F, cy - R * 0.5F, 1.2F, col, 0.5F);   // the tool head
                fx.rect(cx + R * 0.6F, cy - R * 0.5F, cx + R * 0.8F, cy - R * 0.4F, col, 0.5F);
                inside(cx - R * 0.85F, cy - R * 0.55F, cx + R * 0.5F, cy + R * 0.55F);
            }
            case DEWAR -> {
                float hw = R * 0.55F;
                box(cx - hw, cy - R * 0.85F, cx + hw, cy + R * 0.9F, 1.2F, col, a);
                box(cx - hw + 4, cy - R * 0.85F, cx + hw - 4, cy + R * 0.9F - 4, 0.8F, col, 0.35F);
                for (int i = 0; i < 5; i++) {
                    float u = frac(t * 0.2F + i / 5F);
                    fx.line(cx - hw - 3 - u * 4, cy - R * 0.85F - u * 8, cx - hw - 1 - u * 4, cy - R * 0.85F - u * 8, 1F, 0xC0E0FF, 0.4F * (1 - u) * lit);
                }
                inside(cx - hw + 6, cy - R * 0.75F, cx + hw - 6, cy + R * 0.8F);
            }
        }
    }

    // ------------------------------------------------------------------ processes
    private void content(C c, boolean main) {
        int a = main ? hot : col;
        float s = main ? 1F : 0.7F;
        switch (c) {
            case NONE -> {
            }
            case BUBBLES -> {
                for (int i = 0; i < 12; i++) {
                    float u = frac(t * (0.25F + rnd(i, 1) * 0.3F) + rnd(i, 2));
                    float x = x0 + w() * (0.1F + 0.8F * rnd(i, 3)) + sin(u * 9 + i) * 1.5F;
                    fx.ring(x, y1 - u * h(), 1.2F + u * 1.5F, 0.7F, 10, a, 0.6F * s * lit * (1 - u * 0.5F));
                }
            }
            case MOLECULES -> {
                int n = 3 + k % 3;
                for (int i = 0; i < n; i++) {
                    float u = frac(t * 0.3F + i / (float) n);
                    float mx = x0 + w() * 0.5F, my = y0 + h() * (0.2F + 0.6F * rnd(i, 7));
                    float d = (1 - Math.min(1, u * 2)) * w() * 0.45F;
                    if (u < 0.5F) {                                                           // two reactants approach
                        dot(mx - d, my, 2.4F, col, 0.8F * s);
                        dot(mx + d, my + 1, 1.8F, hot, 0.8F * s);
                    } else {                                                                  // the product drifts away
                        float v = (u - 0.5F) * 2;
                        float px = mx + (rnd(i, 9) - 0.5F) * w() * v, py = my - h() * 0.2F * v;
                        dot(px - 2, py, 2.4F, col, 0.8F * s);
                        dot(px + 2, py, 1.8F, hot, 0.8F * s);
                        fx.line(px - 2, py, px + 2, py, 1F, W, 0.5F * lit);
                    }
                }
            }
            case BANDS -> {
                int n = 3 + k % 3;
                for (int i = 0; i < n; i++) {
                    float sp = 0.3F + i * 0.18F;
                    float y = y0 + h() * frac(p() * sp + i * 0.13F + t * 0.02F);
                    int bc = Fx2D.lerpRgb(col, hot, i / (float) (n - 1));
                    fx.rectV(x0, y - 2, x1, y + 2, bc, 0.15F * s, bc, 0.6F * s * lit);
                }
            }
            case FLAME -> {
                for (int i = 0; i < 7; i++) {
                    float x = x0 + w() * (i + 0.5F) / 7, fl = 0.6F + 0.4F * sin(t * 6 + i * 2.1F);
                    fx.tri(x - 3, y1, 0xFF8020, 0.7F * s * lit, x + 3, y1, 0xFF8020, 0.7F * s * lit, x, y1 - 8 * fl, 0xFFE060, 0.1F);
                }
            }
            case ARCS -> {
                for (int j = 0; j < 2; j++) {
                    float px = x0, py = y0 + h() * (0.3F + 0.4F * j);
                    for (int i = 1; i <= 8; i++) {
                        float qx = x0 + w() * i / 8, qy = y0 + h() * (0.3F + 0.4F * j) + (rnd(i + (int) (t * 8), j) - 0.5F) * h() * 0.3F;
                        fx.glowLine(px, py, qx, qy, 1F, a, 0.7F * s * lit);
                        px = qx;
                        py = qy;
                    }
                }
            }
            case PARTICLES -> {
                for (int i = 0; i < 16; i++) {
                    float x = x0 + w() * frac(rnd(i, 4) + sin(t * 0.7F + i) * 0.08F + t * 0.05F * (rnd(i, 5) - 0.5F));
                    float y = y0 + h() * frac(rnd(i, 6) + cos(t * 0.9F + i * 1.3F) * 0.08F);
                    dot(x, y, 1.1F, i % 3 == 0 ? hot : col, 0.7F * s);
                }
            }
            case FLOW -> {
                for (int j = 0; j < 3; j++) {
                    float y = y0 + h() * (j + 1) / 4;
                    fx.line(x0, y, x1, y, 0.6F, a, 0.25F * s);
                    for (int i = 0; i < 4; i++) {
                        float u = frac(t * 0.35F + i / 4F + j * 0.11F);
                        float x = x0 + w() * u;
                        fx.line(x - 4, y + sin(u * 10 + j) * 1.2F, x, y + sin(u * 10 + j) * 1.2F, 1.4F, a, 0.7F * s * lit);
                    }
                }
            }
            case CRYSTAL -> {
                for (int i = 0; i < 4; i++) {
                    float x = x0 + w() * (0.2F + 0.6F * rnd(i, 11)), y = y0 + h() * (0.35F + 0.5F * rnd(i, 12));
                    float r = 2 + 6 * Math.min(1, p() * 1.5F + rnd(i, 13) * 0.3F);
                    fx.polygon(x, y, r, 6, rnd(i, 14), 1F, a, 0.75F * s * lit);
                    fx.disc(x, y, r * 0.8F, 6, a, 0.15F * s, a, 0.05F);
                }
            }
            case HELIX -> {
                for (int i = 0; i < 36; i++) {
                    float u = i / 35F, x = x0 + w() * u, an = u * TAU * 2.5F + t;
                    float ya = (y0 + y1) / 2 + sin(an) * h() * 0.3F, yb = (y0 + y1) / 2 - sin(an) * h() * 0.3F;
                    dot(x, ya, 1F, cos(an) > 0 ? a : col, 0.7F * s);
                    dot(x, yb, 1F, cos(an) > 0 ? col : a, 0.5F * s);
                    if (i % 3 == 0) fx.line(x, ya, x, yb, 0.6F, col, 0.25F * s);
                }
            }
            case CELLS -> {
                int n = 3 + k % 4;
                for (int i = 0; i < n; i++) {
                    float x = x0 + w() * (0.15F + 0.7F * rnd(i, 21)), y = y0 + h() * (0.2F + 0.6F * rnd(i, 22));
                    float ph = frac(t * 0.15F + rnd(i, 23));
                    float sep = ph > 0.7F ? (ph - 0.7F) / 0.3F * 4 : 0;
                    float r = 3.5F * Math.min(1, R / 40F) + 1;
                    fx.ring(x - sep, y, r, 0.8F, 14, a, 0.7F * s * lit);
                    if (sep > 0) fx.ring(x + sep, y, r, 0.8F, 14, a, 0.7F * s * lit);
                    dot(x - sep, y, 1F, col, 0.6F * s);
                }
            }
            case GLOW -> {
                float pu = 0.75F + 0.25F * sin(t * 2.5F);
                float r = Math.min(w(), h()) * 0.45F * pu;
                fx.disc((x0 + x1) / 2, (y0 + y1) / 2, r, 24, a, 0.7F * s * lit, a, 0F);
                fx.disc((x0 + x1) / 2, (y0 + y1) / 2, r * 0.35F, 16, W, 0.6F * lit, W, 0F);
            }
            case PEAKS -> {
                for (int i = 0; i < 5; i++) {
                    float x = x0 + w() * (0.1F + 0.8F * rnd(i + k * 5, 31));
                    float ph = 0.3F + 0.7F * Math.min(1, p() * 1.5F + 0.2F);
                    float hgt = h() * (0.3F + 0.6F * rnd(i + k * 5, 32)) * ph;
                    fx.tri(x - 4, y1, a, 0.1F, x + 4, y1, a, 0.1F, x, y1 - hgt, a, 0.8F * s * lit);
                }
            }
            case CURVE -> {
                float px = x0, py = 0;
                int drawn = (int) (40 * (0.35F + 0.65F * frac(t * 0.08F + p())));
                for (int i = 0; i <= 40; i++) {
                    float u = i / 40F, v = curve(u);
                    float qx = x0 + w() * u, qy = y1 - h() * v;
                    if (i > 0) fx.line(px, py, qx, qy, 1.2F, a, (i <= drawn ? 0.85F : 0.2F) * s * (i <= drawn ? lit : 1));
                    px = qx;
                    py = qy;
                }
            }
            case SIEVE -> {
                int n = 8;
                float cw = w() / n, ch = h() / 6;
                int crossed = (int) (48 * (0.2F + 0.8F * frac(t * 0.05F + p())));
                for (int i = 0; i < 48; i++) {
                    int gx = i % n, gy = i / n;
                    float x = x0 + gx * cw + cw / 2, y = y0 + gy * ch + ch / 2;
                    boolean hit = sieveHit(i + 2) && i < crossed;
                    fx.rect(x - cw * 0.3F, y - ch * 0.3F, x + cw * 0.3F, y + ch * 0.3F, hit ? col : a, (hit ? 0.12F : 0.5F) * s * lit);
                    if (hit) fx.line(x - cw * 0.35F, y + ch * 0.35F, x + cw * 0.35F, y - ch * 0.35F, 0.8F, hot, 0.5F * s);
                }
            }
            case GRAPH -> {
                int n = 7;
                float[] gx = new float[n], gy = new float[n];
                for (int i = 0; i < n; i++) {
                    gx[i] = x0 + w() * (0.1F + 0.8F * rnd(i + k * 7, 41));
                    gy[i] = y0 + h() * (0.1F + 0.8F * rnd(i + k * 7, 42));
                }
                int hotNode = (int) (t * 1.5F) % n;
                for (int i = 0; i < n; i++)
                    for (int j = i + 1; j < n; j++)
                        if (rnd(i * 13 + j + k, 43) < 0.35F)
                            fx.line(gx[i], gy[i], gx[j], gy[j], 0.8F, a, (i == hotNode || j == hotNode ? 0.7F * lit : 0.25F) * s);
                for (int i = 0; i < n; i++) dot(gx[i], gy[i], i == hotNode ? 2.6F : 1.8F, i == hotNode ? W : a, 0.8F * s);
            }
            case WAVES -> {
                for (int j = 0; j < 3; j++) {
                    float py = 0, px = x0;
                    float amp = h() * (0.12F + 0.05F * j), fr = 1.5F + j + k % 3;
                    float yc = y0 + h() * (j + 1) / 4;
                    for (int i = 0; i <= 30; i++) {
                        float u = i / 30F, qx = x0 + w() * u, qy = yc + sin(u * TAU * fr - t * (1.5F + j)) * amp;
                        if (i > 0) fx.line(px, py, qx, qy, 1F, j == 1 ? a : col, 0.6F * s * lit);
                        px = qx;
                        py = qy;
                    }
                }
            }
            case SPIN -> {
                for (int i = 0; i < 6; i++) {
                    float x = x0 + w() * (i % 3 + 0.5F) / 3, y = y0 + h() * (i / 3 + 0.5F) / 2;
                    float an = -TAU / 4 + 0.6F * sin(t * 2 + i) + (k == 6 && i % 2 == 1 ? (float) Math.PI : 0);
                    float L = Math.min(w(), h()) * 0.18F;
                    fx.glowLine(x, y, x + cos(an) * L, y + sin(an) * L, 1F, i % 2 == 0 ? a : col, 0.8F * s * lit);
                    dot(x, y, 1.2F, a, 0.6F * s);
                }
            }
            case ORBITS -> {
                float ox = (x0 + x1) / 2, oy = (y0 + y1) / 2;
                for (int j = 1; j <= 3; j++) {
                    float r = Math.min(w(), h()) * 0.16F * j;
                    fx.ring(ox, oy, r, 0.6F, 30, a, 0.3F * s);
                    float an = t * (1.5F / j) + j * 2;
                    dot(ox + cos(an) * r, oy + sin(an) * r, 1.6F, a, 0.9F * s);
                }
                dot(ox, oy, 2F, W, 0.7F * s);
            }
            case FIELD -> {
                float ox = (x0 + x1) / 2, oy = (y0 + y1) / 2;
                for (int j = 1; j <= 4; j++) {
                    float r = Math.min(w(), h()) * 0.12F * j;
                    fx.band(ox, oy, r - 0.3F, r + 0.3F, 0.55F + 0.1F * j, 0, TAU, 36, a, 0.35F * s * lit, a, 0.35F * s * lit);
                    float an = t * (j % 2 == 0 ? 1 : -1) + j;
                    dot(ox + cos(an) * r, oy + sin(an) * r * (0.55F + 0.1F * j), 1.2F, W, 0.6F * s);
                }
            }
            case DROPS -> {
                for (int i = 0; i < 6; i++) {
                    float u = frac(t * 0.5F + i / 6F);
                    float x = x0 + w() * (0.2F + 0.6F * rnd(i + k, 51));
                    fx.disc(x, y0 + u * h(), 1.5F, 8, a, 0.8F * s * lit, a, 0.3F * s);
                }
            }
            case LATTICE -> {
                int n = 5;
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < 4; j++) {
                        float x = x0 + w() * (i + 0.5F) / n + sin(t * 3 + i + j) * 0.8F, y = y0 + h() * (j + 0.5F) / 4;
                        boolean odd = (i + j + k) % (3 + k % 2) == 0;
                        dot(x, y, odd ? 1.8F : 1.3F, odd ? a : col, 0.7F * s);
                        if (i < n - 1) fx.line(x, y, x + w() / n, y, 0.5F, col, 0.2F * s);
                    }
            }
            case TILES -> {
                int sides = 3 + k % 4;
                for (int i = 0; i < 5; i++) {
                    float x = x0 + w() * (0.15F + 0.7F * rnd(i + k, 61)), y = y0 + h() * (0.2F + 0.6F * rnd(i + k, 62));
                    float r = Math.min(w(), h()) * (0.1F + 0.08F * rnd(i, 63));
                    fx.polygon(x, y, r, i % 2 == 0 ? sides : 5, t * 0.3F * (i % 2 == 0 ? 1 : -1) + i, 1F, i == (int) t % 5 ? W : a, 0.75F * s * lit);
                }
            }
            case BEAM -> {
                float y = (y0 + y1) / 2 + sin(t * 0.5F) * h() * 0.05F;
                fx.glowLine(x0, y, x1, y, 1.6F, a, 0.8F * s * lit);
                for (int i = 0; i < 4; i++) {
                    float u = frac(t * 0.8F + i / 4F);
                    dot(x0 + w() * u, y, 1.5F, W, 0.9F * s);
                }
                for (int i = 0; i < 5; i++) {                                                 // sparks at the target
                    float an = rnd(i + (int) (t * 6), 71) * TAU, l = 3 + 4 * rnd(i, 72);
                    fx.line(x1, y, x1 + cos(an) * l, y + sin(an) * l, 0.8F, hot, 0.5F * s * lit);
                }
            }
            case FOLD -> {
                float done = 0.3F + 0.7F * frac(t * 0.06F + p());
                float px = x0, py = (y0 + y1) / 2;
                for (int i = 1; i <= 24; i++) {
                    float u = i / 24F;
                    float sx = x0 + w() * u, sy = (y0 + y1) / 2 + sin(u * 20 + k) * 2;            // stretched
                    float an = u * TAU * 1.6F + k;
                    float fxp = (x0 + x1) / 2 + cos(an) * w() * 0.25F * u, fyp = (y0 + y1) / 2 + sin(an) * h() * 0.3F * u; // folded
                    float qx = sx + (fxp - sx) * done, qy = sy + (fyp - sy) * done;
                    fx.line(px, py, qx, qy, 1.2F, i % 5 == 0 ? hot : a, 0.75F * s * lit);
                    px = qx;
                    py = qy;
                }
            }
            case BARS -> {
                int n = 8;
                for (int i = 0; i < n; i++) {
                    float u = (i + 0.5F) / n;
                    float v = k == 0 ? curve(u) : (float) Math.exp(-Math.pow((u - 0.5) / 0.22, 2)) * (0.6F + 0.4F * Math.min(1, p() + 0.3F));
                    float bx = x0 + w() * i / n;
                    fx.rectV(bx + 1, y1 - h() * v, bx + w() / n - 1, y1, a, 0.6F * s * lit, a, 0.15F * s);
                }
            }
        }
    }

    private static boolean sieveHit(int n) {
        for (int d = 2; d * d <= n; d++) if (n % d == 0) return true;
        return false;
    }

    /** The plotted law for CURVE (and for an integrator's bars), picked by the spec's k. */
    private float curve(float u) {
        return switch (k % 11) {
            case 0 -> 0.1F + 0.8F * (float) (0.5 + 0.5 * Math.sin(u * Math.PI * 1.5 - 0.6)) * u;                 // something to integrate
            case 1 -> (float) Math.exp(-3.2 * u) * 0.9F;                                                       // decay
            case 2 -> (float) Math.exp(-Math.pow((u - 0.45 - 0.1 * Math.sin(t * 0.3)) / 0.14, 2)) * 0.9F;      // posterior
            case 3 -> 0.5F + 0.42F * (float) (Math.exp(-2.5 * u) * Math.sin(u * 40 + t));                      // seismic
            case 4 -> 0.9F * u / (0.18F + u);                                                                  // Michaelis Menten
            case 5 -> (float) (Math.log1p(u * 9) / Math.log(10)) * 0.9F;                                       // continuation
            case 6 -> (float) (0.05 + 0.9 / (1 + Math.exp(-40 * (u - 0.6))));                                   // titration
            case 7 -> (float) (0.45 + 0.08 * Math.tanh((u - 0.5) * 3) + 0.4 * Math.max(0, u - 0.85) * 4);      // buffer
            case 8 -> (float) (0.1 + 3.2 * Math.pow(Math.pow((u - 0.5) * 2, 2) - 0.5, 2));                   // Higgs well
            case 9 -> (float) (Math.sin(Math.PI * u) * 0.85);                                                  // Page curve
            default -> u;
        };
    }
}
