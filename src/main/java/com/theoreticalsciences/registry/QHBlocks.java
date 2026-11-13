package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.block.CasingBlock;
import com.theoreticalsciences.block.FieldGlassBlock;
import com.theoreticalsciences.machine.MachineBlock;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.machine.PortBlock;
import com.theoreticalsciences.research.ResearchTerminalBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class QHBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TheoreticalSciences.MODID);

    /** Simple content blocks (ores, casings, storage...) keyed by id. */
    public static final Map<String, DeferredBlock<Block>> SIMPLE = new LinkedHashMap<>();
    public static final Map<MachineType, DeferredBlock<MachineBlock>> MACHINES = new EnumMap<>(MachineType.class);

    public static final DeferredBlock<PortBlock> INTERFACE_PORT = BLOCKS.registerBlock("interface_port", PortBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(5.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 5));
    /** 1.7.4: holds Botania mana for a structure; fits wherever an Interface Port does. */
    public static final DeferredBlock<com.theoreticalsciences.machine.ManaPortBlock> MANA_PORT = BLOCKS.registerBlock("mana_port", com.theoreticalsciences.machine.ManaPortBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(5.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 7));
    public static final DeferredBlock<ResearchTerminalBlock> RESEARCH_TERMINAL = BLOCKS.registerBlock("research_terminal", ResearchTerminalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F, 10.0F).requiresCorrectToolForDrops()
                    .sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 9).noOcclusion());

    static final java.util.Set<String> SQUISH = java.util.Set.of("extracellular_matrix", "cell_membrane", "bone_matrix", "lymph_tissue", "chromatin",
            "tumour_mass", "bioluminescent_membrane", "collagen_seam", "nucleotide_cluster", "tumour_nodule",
            "grey_matter", "white_matter", "neuron_soma");                    // 1.4.0: the Neural Cortex

    static {
        for (QHContentList.BlockDef def : QHContentList.BLOCKS) {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.of()
                    .strength(def.hardness(), def.hardness() * 2.5F)
                    .lightLevel(s -> def.light());
            if (!def.tool().equals("none")) p.requiresCorrectToolForDrops();
            switch (def.kind()) {
                case "ore" -> {
                    if (def.base().equals("foam_substrate")) {        // virtual-particle ores blink out of existence
                        p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL);
                        SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.foam.EphemeralOreBlock::new, p));
                    } else {
                        p.mapColor(MapColor.DEEPSLATE).sound(SoundType.DEEPSLATE);
                        SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), pp -> new DropExperienceBlock(UniformInt.of(2, 6), pp), p));
                    }
                }
                case "foam" -> {
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.WOOL);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.foam.FoamSubstrateBlock::new, p));
                }
                case "foam_bubble" -> {
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).noOcclusion()
                            .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isRedstoneConductor((s, l, pos) -> false);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.foam.FoamBubbleBlock::new, p));
                }
                case "unbreakable" -> {
                    p.mapColor(MapColor.SNOW).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(-1.0F, 3600000.0F).noLootTable();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), Block::new, p));
                }
                case "proof_pedestal", "proof_toggle", "proof_lamp", "proof_membrane" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(-1.0F, 3600000.0F).noLootTable();
                    boolean lit = def.kind().equals("proof_toggle") || def.kind().equals("proof_lamp");
                    if (lit) p.lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 3);
                    if (def.kind().equals("proof_membrane")) p.noOcclusion().isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false);
                    java.util.function.Function<BlockBehaviour.Properties, Block> make = switch (def.kind()) {
                        case "proof_pedestal" -> com.theoreticalsciences.boss.ProofBlocks.Pedestal::new;
                        case "proof_toggle" -> com.theoreticalsciences.boss.ProofBlocks.Toggle::new;
                        case "proof_lamp" -> com.theoreticalsciences.boss.ProofBlocks.Lamp::new;
                        default -> FieldGlassBlock::new;
                    };
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), make, p));
                }
                case "energy_cell" -> {
                    p.mapColor(MapColor.COLOR_CYAN).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 8);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.logistics.EnergyCellBlock::new, p));
                }
                case "transceiver" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).noOcclusion();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.logistics.TransceiverBlock::new, p));
                }
                case "field_projector" -> {
                    p.mapColor(MapColor.COLOR_BLUE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE)
                            .lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED) ? 13 : 2);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.field.FieldProjectorBlock::new, p));
                }
                case "infinity_cache" -> {
                    p.mapColor(MapColor.GOLD).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).strength(def.hardness(), 3600000.0F).lightLevel(s -> 10);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.storage.InfinityCacheBlock::new, p));
                }
                case "conv_altar" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(def.hardness(), 1200.0F).lightLevel(s -> 9).noOcclusion();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.converge.AltarBlock::new, p));
                }
                case "conv_pedestal" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(def.hardness(), 1200.0F).lightLevel(s -> 6).noOcclusion();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.converge.PedestalBlock::new, p));
                }
                case "chrono_accel" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).strength(25.0F, 1200.0F).lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 13 : 3);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.chrono.ChronalAcceleratorBlock::new, p));
                }
                case "cache" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).strength(3.0F, 1200.0F).lightLevel(s -> 4);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.dungeon.CacheBlock::new, p));
                }
                case "zeno_anchor" -> {        // 1.2.0: the chunk loader
                    p.mapColor(MapColor.COLOR_CYAN).sound(com.theoreticalsciences.registry.QHSounds.MACHINE)
                            .lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 12 : 2);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.logistics.ZenoAnchorBlock::new, p));
                }
                case "governor" -> {           // 1.2.0: Control Theory
                    p.mapColor(MapColor.COLOR_ORANGE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE)
                            .lightLevel(s -> s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 2);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.machine.tuning.GovernorBlock::new, p));
                }
                case "transfer_console" -> {   // 1.3.0: the Orbital Station
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 8);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.orbit.OrbitTravel.ConsoleBlock::new, p));
                }
                case "flow_conduit" -> {       // 1.3.0: Fluids and Flight
                    p.mapColor(MapColor.COLOR_ORANGE).sound(net.minecraft.world.level.block.SoundType.COPPER).noOcclusion();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.logistics.FlowConduitBlock::new, p));
                }
                case "gas_cell" -> {           // 1.2.0
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).strength(4.0F, 1200.0F).lightLevel(s -> 4);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.science.GasCellBlock::new, p));
                }
                case "chrono_vault" -> {       // 1.2.0
                    p.mapColor(MapColor.COLOR_CYAN).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).strength(5.0F, 1200.0F).lightLevel(s -> 6);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.science.ChronostaticVaultBlock::new, p));
                }
                case "exnihilo" -> {
                    p.mapColor(MapColor.GOLD).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(def.hardness(), 1200.0F).lightLevel(s -> 15);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.endgame.ExNihiloEngineBlock::new, p));
                }
                case "anomaly" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 7);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.dungeon.AnomalyEngineBlock::new, p));
                }
                case "erb" -> {
                    p.mapColor(MapColor.COLOR_BLACK).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).noCollission();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.gr.EinsteinRosenBridgeBlock::new, p));
                }
                case "return_rift" -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).strength(-1.0F, 3600000.0F).noLootTable().noCollission();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.transcend.ReturnRiftBlock::new, p));
                }
                case "glass" -> {
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).noOcclusion()
                            .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isRedstoneConductor((s, l, pos) -> false);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), FieldGlassBlock::new, p));
                }
                case "cloud" -> {
                    // 1.10.1: nebular gas and dust - you walk straight through it, and it never suffocates or hides you
                    p.mapColor(MapColor.COLOR_PINK).sound(SoundType.WOOL).noCollission().noOcclusion().replaceable()
                            .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isRedstoneConductor((s, l, pos) -> false);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.block.NebulaCloudBlock::new, p));
                }
                case "arbor" -> {
                    p.mapColor(MapColor.COLOR_MAGENTA).sound(SoundType.CHERRY_WOOD).explosionResistance(3600000F);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.block.ArborHeartwoodBlock::new, p));
                }
                case "bubble" -> {
                    p.mapColor(MapColor.COLOR_LIGHT_BLUE).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL).noOcclusion()
                            .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.block.BubbleUniverseBlock::new, p));
                }
                case "fracture" -> {
                    p.mapColor(MapColor.COLOR_CYAN).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.block.FracturedRealityBlock::new, p));
                }
                case "mantle_breach" -> {
                    // round 40 (1.2.0): the way between the Overworld and the Planetary Interior
                    p.mapColor(MapColor.COLOR_ORANGE).noCollission().noLootTable().strength(-1F, 3600000F);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.interior.MantleBreachBlock::new, p));
                }
                case "vent" -> {
                    // 1.2.0: a black smoker, Titan's hidden ocean
                    p.mapColor(MapColor.COLOR_BLACK).sound(SoundType.BASALT);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.ocean.OceanBlocks.BlackSmoker::new, p));
                }
                case "ocean_plant", "ocean_plant_hanging" -> {
                    boolean hang = def.kind().equals("ocean_plant_hanging");
                    p.mapColor(hang ? MapColor.ICE : MapColor.COLOR_RED).sound(hang ? SoundType.GLASS : SoundType.WET_GRASS).noCollission().noOcclusion()
                            .instabreak().randomTicks();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), pp -> new com.theoreticalsciences.ocean.OceanBlocks.Growth(pp, hang), p));
                }
                case "deco_model" -> {
                    // round 39 (1.2.6): decorative instruments with their own models
                    p.mapColor(MapColor.METAL).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).noOcclusion();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.block.DecoModelBlock::new, p));
                }
                case "deco" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(com.theoreticalsciences.registry.QHSounds.MACHINE);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), Block::new, p));
                }
                case "foliage" -> {
                    // 1.8.1: leaves with gaps in them. They let light and sight through and nothing suffocates in them.
                    p.mapColor(MapColor.GOLD).sound(SoundType.CHERRY_LEAVES).noOcclusion()
                            .isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false).isRedstoneConductor((s, l, pos) -> false);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), Block::new, p));
                }
                case "deco_pillar" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(com.theoreticalsciences.registry.QHSounds.MACHINE);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), net.minecraft.world.level.block.RotatedPillarBlock::new, p));
                }
                case "deco_stairs" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(def.tool().equals("none") ? SoundType.CHERRY_WOOD : com.theoreticalsciences.registry.QHSounds.MACHINE);
                    // the base block is registered earlier in the same register, so it exists when this supplier runs
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), pp -> new net.minecraft.world.level.block.StairBlock(SIMPLE.get(def.base()).get().defaultBlockState(), pp) {}, p));
                }
                case "deco_slab" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(def.tool().equals("none") ? SoundType.CHERRY_WOOD : com.theoreticalsciences.registry.QHSounds.MACHINE);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), net.minecraft.world.level.block.SlabBlock::new, p));
                }
                case "deco_wall" -> {
                    p.mapColor(MapColor.COLOR_GRAY).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).forceSolidOn();
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), net.minecraft.world.level.block.WallBlock::new, p));
                }
                case "anode" -> {               // 1.4.0: corrosion: eaten in place of the machines
                    p.mapColor(MapColor.METAL).sound(SoundType.METAL);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), com.theoreticalsciences.engineering.SacrificialAnodeBlock::new, p));
                }
                case "brushable" -> {           // 1.6.0: brushed like suspicious sand, turning into its base block
                    p.mapColor(MapColor.DIRT).sound(SoundType.SUSPICIOUS_GRAVEL).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), pp -> new net.minecraft.world.level.block.BrushableBlock(SIMPLE.get(def.base()).get(),
                            net.minecraft.sounds.SoundEvents.BRUSH_GRAVEL, net.minecraft.sounds.SoundEvents.BRUSH_GRAVEL_COMPLETED, pp), p));
                }
                case "casing" -> {
                    p.mapColor(MapColor.COLOR_BLACK).sound(com.theoreticalsciences.registry.QHSounds.MACHINE);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), CasingBlock::new, p));
                }
                default -> {
                    p.mapColor(MapColor.COLOR_PURPLE).sound(def.id().startsWith("bulk") ? com.theoreticalsciences.registry.QHSounds.CRYSTAL : com.theoreticalsciences.registry.QHSounds.MACHINE);
                    SIMPLE.put(def.id(), BLOCKS.registerBlock(def.id(), Block::new, p));
                }
            }
            // 1.3.3: the Planetary Interior is rock, and sounds like it (breaking, placing and walking)
            net.minecraft.world.level.block.SoundType rock = rockSound(def.id());
            if (rock != null) p.sound(rock);
            // 1.10.1: rock sounds like stone, crystal like amethyst, glass and lamps like glass, gas like wool
            net.minecraft.world.level.block.SoundType look = QHBlockSounds.of(def.id());
            if (look != null) p.sound(look);
            // 1.8.1: Omnipotency's blocks sound like what they are
            net.minecraft.world.level.block.SoundType heaven = switch (def.id()) {
                case "empyrean_marble", "empyrean_marble_bricks", "gilded_marble", "empyrean_pillar", "empyrean_marble_tiles" -> SoundType.STONE;
                case "seraph_gold_trim" -> SoundType.METAL;
                case "halo_glass" -> SoundType.GLASS;
                case "heaven_soil" -> SoundType.GRAVEL;                 // (what dirt itself uses)
                case "halo_grass" -> SoundType.GRASS;
                case "cloudstone", "radiant_cloud" -> SoundType.WOOL;
                case "choir_crystal_block" -> com.theoreticalsciences.registry.QHSounds.CHOIR_CRYSTAL;
                case "seraph_gold_block" -> SoundType.METAL;
                case "glory_lantern" -> SoundType.LANTERN;
                case "silver_bark" -> SoundType.WOOD;
                case "aureate_leaves" -> SoundType.CHERRY_LEAVES;
                default -> null;
            };
            if (heaven != null) p.sound(heaven);
            // 1.2.0: Inner Space is living tissue: every block of it squelches like a slime block
            if (SQUISH.contains(def.id())) p.sound(SoundType.SLIME_BLOCK);
        }
        for (MachineType type : MachineType.values()) {
            MACHINES.put(type, BLOCKS.registerBlock(type.id(), p -> new MachineBlock(p, type),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            .strength(type.isMultiblock() ? 8.0F : 5.0F, 20.0F).requiresCorrectToolForDrops()
                            .sound(com.theoreticalsciences.registry.QHSounds.MACHINE)
                            .lightLevel(s -> s.getValue(MachineBlock.ACTIVE) ? 12 : 3)));
        }
    }

    private static net.minecraft.world.level.block.SoundType rockSound(String id) {
        return switch (id) {
            case "basalt_column" -> net.minecraft.world.level.block.SoundType.BASALT;
            case "crustal_rock", "peridotite", "core_iron", "oklo_rock", "inner_core_boundary", "geode_shell", "ringwoodite_ore", "bridgmanite_ore",
                 "core_magnetite_ore", "oklo_uraninite_ore", "fission_fragment_ore", "rich_iron_ore", "rich_copper_ore", "rich_gold_ore" ->
                    net.minecraft.world.level.block.SoundType.STONE;
            case "corundum_crystal", "garnet_crystal", "peridot_crystal", "moissanite_crystal" -> net.minecraft.world.level.block.SoundType.AMETHYST;
            default -> null;
        };
    }

    public static Block get(String id) {
        return SIMPLE.get(id).get();
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private QHBlocks() {}
}
