package com.theoreticalsciences;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class QHConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.DoubleValue ENERGY_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GENERATOR_OUTPUT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DURATION_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue RESEARCH_REQUIRED;
    public static final ModConfigSpec.DoubleValue RESEARCH_TIME_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue RESEARCH_PUZZLES;
    public static final ModConfigSpec.BooleanValue PUZZLE_VALUES_PER_WORLD;
    public static final ModConfigSpec.BooleanValue BRANE_SHIFTER_NEEDS_RESEARCH;
    /** Round 33: a new player gets the Quantum Codex the first time they join a world. */
    public static final ModConfigSpec.BooleanValue GIVE_CODEX_ON_FIRST_JOIN;
    public static final ModConfigSpec.IntValue SINGULARITY_PULL_RADIUS;
    public static final ModConfigSpec.DoubleValue TUNING_DIFFICULTY;
    public static final ModConfigSpec.DoubleValue TUNING_WEAR;
    public static final ModConfigSpec.BooleanValue FAILURE_HAZARDS;
    public static final ModConfigSpec.IntValue NANITE_CAP;
    public static final ModConfigSpec.BooleanValue FAILURE_DESTROYS_BLOCKS;
    public static final ModConfigSpec.DoubleValue INGREDIENT_MULTIPLIER;
    public static final ModConfigSpec.IntValue CHRONAL_MAX_SPEED, CHRONAL_MAX_RADIUS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> CREATIVE_DUPLICATOR_BLOCKS, CREATIVE_DUPLICATOR_HOSTS;
    public static final ModConfigSpec.BooleanValue AUTO_SINGULARITIES;
    public static final ModConfigSpec.IntValue AUTO_SINGULARITY_COUNT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> AUTO_SINGULARITY_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> ESSENCE_BLACKLIST;
    public static final ModConfigSpec.IntValue CHRONAL_ENERGY_PER_TICK, CHRONAL_ENERGY_PER_RANDOM_TICK;   // (int: config screens such as Jupiter can show it)

    public static final ModConfigSpec.BooleanValue LENSING_ENABLED;
    public static final ModConfigSpec.IntValue LENSING_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue FANCY_SKY;
    public static final ModConfigSpec.IntValue PARTICLE_DENSITY;
    public static final ModConfigSpec.BooleanValue STRUCTURE_DISTORTION;

    /** Whether space bends inside higher-dimensional structures (safe default before the config loads). */
    public static boolean distortionEnabled() {
        try {
            return STRUCTURE_DISTORTION.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("machines");
        ENERGY_COST_MULTIPLIER = b.comment("Multiplier on FE/t consumed by every Theoretical Sciences machine.").defineInRange("energyCostMultiplier", 1.0, 0.0, 1000.0);
        GENERATOR_OUTPUT_MULTIPLIER = b.comment("Multiplier on FE/t produced by generators (Zero-Point Extractor, Penrose Ergosphere).").defineInRange("generatorOutputMultiplier", 1.0, 0.0, 1000.0);
        DURATION_MULTIPLIER = b.comment("Multiplier on recipe durations.").defineInRange("durationMultiplier", 1.0, 0.01, 100.0);
        INGREDIENT_MULTIPLIER = b.comment("Multiplier on every machine recipe's item and fluid inputs (not generator fuel), applied when recipes load (/reload after changing).",
                "Item inputs are capped at what one slot holds, fluids at what one tank holds (2,147,483,647 each). Use it to tune the whole mod's cost for a pack.").defineInRange("recipeIngredientMultiplier", 1.0, 0.01, 64.0);
        b.pop();
        b.push("tuning");
        TUNING_DIFFICULTY = b.comment("How fast multiblock tuning targets drift (0 = never drifts: tuning is set-and-forget).").defineInRange("driftSpeed", 1.0, 0.0, 10.0);
        TUNING_WEAR = b.comment("How fast containment integrity wears down while multiblocks run (0 = never: no recalibration needed).").defineInRange("containmentWear", 1.0, 0.0, 10.0);
        FAILURE_HAZARDS = b.comment("If true, a containment failure releases its hazard (reality destabilisation, ruptures, nanites, paradox echoes, law wraiths).").define("containmentFailureHazards", true);
        NANITE_CAP = b.comment("Hard cap on how many Nanite Swarms may exist in one dimension at once (they stop replicating at the cap).")
                .defineInRange("naniteSwarmCap", 40, 0, 1000);
        FAILURE_DESTROYS_BLOCKS = b.comment("If true, explosions and spacetime ruptures from containment failures destroy blocks (the mobGriefing game rule is also respected).").define("containmentFailureDestroysBlocks", true);
        b.pop();
        b.push("research");
        RESEARCH_REQUIRED = b.comment("If false, recipes that name a research entry can run without it.").define("researchRequired", true);
        RESEARCH_TIME_MULTIPLIER = b.comment("Multiplier on how long research entries take to study after paying for them (0 = instant).")
                .defineInRange("researchTimeMultiplier", 1.0, 0.0, 100.0);
        RESEARCH_PUZZLES = b.comment("If true, research entries with a derivation puzzle must have it solved (equation built and answer found) before they can be researched.")
                .define("researchPuzzles", true);
        PUZZLE_VALUES_PER_WORLD = b.comment("If true, the given values of every research question are different in each world (drawn from the world seed), so answers cannot be shared or looked up. Constants of nature never change.")
                .define("puzzleValuesPerWorld", true);
        BRANE_SHIFTER_NEEDS_RESEARCH = b.comment("If true, the Brane Shifter needs the 'Brane Cosmology' research.").define("braneShifterNeedsResearch", true);
        GIVE_CODEX_ON_FIRST_JOIN = b.comment("If true, every player gets a Quantum Codex (the mod's guide book, which opens on its Getting Started steps) the first time they join a world.")
                .define("giveCodexOnFirstJoin", true);
        b.pop();
        b.push("singularities");
        AUTO_SINGULARITIES = b.comment("Every item tagged c:ingots (of any mod but this one) gets a singularity, which the Omniscience Catalyst then needs too. Packs add their own in data/<ns>/ts_singularities/*.json.")
                .define("autoSingularities", true);
        AUTO_SINGULARITY_COUNT = b.comment("How many of the ingot the Singularity Compactor needs for an automatic singularity (a pack file can set its own count).")
                .defineInRange("autoSingularityCount", 16384, 1, Integer.MAX_VALUE);
        AUTO_SINGULARITY_BLACKLIST = b.comment("Ingots that never get an automatic singularity (\"modid:item\", or \"modid:*\" for a whole mod).")
                .defineListAllowEmpty("autoSingularityBlacklist", java.util.List.of("avaritia:infinity_ingot", "extendedcrafting:the_ultimate_ingot", "avaritia_integration:*",
                        "projectexpansion:*", "extendedcrafting:ultimate_ingot"), () -> "modid:item", o -> o instanceof String);
        ESSENCE_BLACKLIST = b.comment("Fluids that are not distilled into a mod's Essence (and so are not needed for Pleroma): \"modid:fluid\", or \"modid:*\" for a whole mod. Only fluids with a bucket count at all.")
                .defineListAllowEmpty("essenceBlacklist", java.util.List.of(), () -> "modid:fluid", o -> o instanceof String);
        b.pop();
        b.push("beyond_copying");
        CREATIVE_DUPLICATOR_BLOCKS = b.comment("Blocks that copy items or fluids without end (creative chests, bins, tanks, drums). They refuse what lies beyond Omnipotence (item / fluid tag theoreticalsciences:beyond_omnipotence). Changes to this list take effect for pipes after a restart.")
                .defineListAllowEmpty("creativeDuplicatorBlocks", com.theoreticalsciences.transcend.Uncopyable.DEFAULT_BLOCKS, () -> "modid:block", o -> o instanceof String);
        CREATIVE_DUPLICATOR_HOSTS = b.comment("Mods whose storage blocks become duplicators while a vending-style part (item tag theoreticalsciences:creative_duplicator_parts) is installed in them, e.g. Storage Drawers with a Creative Vending Upgrade. Ordinary blocks of these mods are not affected.")
                .defineListAllowEmpty("creativeDuplicatorHosts", com.theoreticalsciences.transcend.Uncopyable.DEFAULT_HOSTS, () -> "modid", o -> o instanceof String);
        b.pop();
        b.push("chronal_accelerator");
        CHRONAL_MAX_SPEED = b.comment("Highest time acceleration the Chronal Accelerator may be set to (2..256).").defineInRange("maxSpeed", 256, 2, 256);
        CHRONAL_MAX_RADIUS = b.comment("Largest field radius (the field is a cube of side 2r+1).").defineInRange("maxRadius", 4, 1, 8);
        CHRONAL_ENERGY_PER_TICK = b.comment("FE per extra tick given to one block entity in the field.").defineInRange("energyPerExtraTick", 65536, 0, Integer.MAX_VALUE);
        CHRONAL_ENERGY_PER_RANDOM_TICK = b.comment("FE per extra random tick given to a growing block in the field.").defineInRange("energyPerRandomTick", 16384, 0, Integer.MAX_VALUE);
        b.pop();
        b.push("hazards");
        SINGULARITY_PULL_RADIUS = b.comment("Radius in blocks within which an active singularity pulls loose items and mobs (0 disables).").defineInRange("singularityPullRadius", 6, 0, 32);
        b.pop();
        COMMON_SPEC = b.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        c.push("effects");
        LENSING_ENABLED = c.comment("Screen-space gravitational lensing around black/white holes. Turn off if it conflicts with a shader pack.").define("gravitationalLensing", true);
        LENSING_MAX_DISTANCE = c.comment("Maximum distance (blocks) at which lensing and singularity effects are drawn.").defineInRange("effectDistance", 128, 16, 512);
        FANCY_SKY = c.comment("Custom animated sky in The Bulk.").define("fancyBulkSky", true);
        PARTICLE_DENSITY = c.comment("Particle density for machine effects (0 = none, 3 = maximum).").defineInRange("particleDensity", 2, 0, 3);
        STRUCTURE_DISTORTION = c.comment("Inside higher-dimensional structures (tesseracts, fractals, Platonic solids, the Hilbert Hotel...) the view itself is folded to match. Turn off if it conflicts with a shader pack or causes discomfort.").define("structureDistortion", true);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    private QHConfig() {}
}
