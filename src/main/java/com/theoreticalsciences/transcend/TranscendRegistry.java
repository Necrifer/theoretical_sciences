package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.entity.AxiomSentinel;
import com.theoreticalsciences.entity.FractalWyrm;
import com.theoreticalsciences.world.FractalFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registrations for the expansion: Omnipotency, the Pleroma Confluence, display miniatures, the hyperobject, Platonic natives and fractal landmarks. */
public final class TranscendRegistry {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TheoreticalSciences.MODID);
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);

    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    // ---- Omnipotency
    public static final DeferredHolder<Block, OmnipotenceCoreBlock> CORE = BLOCKS.registerBlock("omnipotence_core", OmnipotenceCoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 15).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL));

    // ---- round 27: the Omnipotence Core forged anew - unbreakable; only the Hand of the Demiurge tunes, wakes and moves it
    public static final DeferredHolder<Block, ForgedCoreBlock> FORGED_CORE = BLOCKS.registerBlock("forged_omnipotence_core", ForgedCoreBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1.0F, 3600000.0F).noLootTable()
                    .lightLevel(s -> s.hasProperty(ForgedCoreBlock.ACTIVE) && s.getValue(ForgedCoreBlock.ACTIVE) ? 15 : 7).sound(com.theoreticalsciences.registry.QHSounds.CRYSTAL)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredHolder<Item, BlockItem> FORGED_CORE_ITEM = tab(ITEMS.registerSimpleBlockItem(FORGED_CORE,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgedCoreBlockEntity>> FORGED_CORE_BE = BES.register("forged_omnipotence_core",
            () -> BlockEntityType.Builder.of(ForgedCoreBlockEntity::new, FORGED_CORE.get()).build(null));

    // ---- Pleroma Confluence
    public static final DeferredHolder<Block, ConfluenceBlock> CONFLUENCE = BLOCKS.registerBlock("pleroma_confluence", ConfluenceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(10.0F, 1200.0F).requiresCorrectToolForDrops().lightLevel(s -> 10).sound(com.theoreticalsciences.registry.QHSounds.MACHINE));
    public static final DeferredHolder<Item, BlockItem> CONFLUENCE_ITEM = tab(ITEMS.registerSimpleBlockItem(CONFLUENCE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConfluenceBlockEntity>> CONFLUENCE_BE = BES.register("pleroma_confluence",
            () -> BlockEntityType.Builder.of(ConfluenceBlockEntity::new, CONFLUENCE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ConfluenceMenu>> CONFLUENCE_MENU = MENUS.register("pleroma_confluence",
            () -> IMenuTypeExtension.create(ConfluenceMenu::fromNetwork));

    // ---- display miniatures
    public static final Map<DecorBlock.Kind, DeferredHolder<Block, DecorBlock>> DECOR = new EnumMap<>(DecorBlock.Kind.class);

    static {
        for (DecorBlock.Kind k : DecorBlock.Kind.values()) {
            DeferredHolder<Block, DecorBlock> h = BLOCKS.registerBlock(k.id(), p -> new DecorBlock(p, k),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0F, 30.0F).requiresCorrectToolForDrops()
                            .noOcclusion().lightLevel(s -> 12).sound(com.theoreticalsciences.registry.QHSounds.MACHINE));
            DECOR.put(k, h);
            tab(ITEMS.register(k.id(), () -> new DecorBlockItem(h.get(), new Item.Properties().rarity(Rarity.EPIC))));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecorBlockEntity>> DECOR_BE = BES.register("display_miniature",
            () -> BlockEntityType.Builder.of(DecorBlockEntity::new, DECOR.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));

    // ---- entities
    public static final DeferredHolder<EntityType<?>, EntityType<HyperobjectEntity>> HYPEROBJECT = ENTITIES.register("hyperobject",
            () -> EntityType.Builder.<HyperobjectEntity>of(HyperobjectEntity::new, MobCategory.MISC).sized(2.0F, 2.0F).fireImmune()
                    .clientTrackingRange(32).updateInterval(2).build("hyperobject"));
    public static final DeferredHolder<EntityType<?>, EntityType<FractalWyrm>> FRACTAL_WYRM = ENTITIES.register("fractal_wyrm",
            () -> EntityType.Builder.<FractalWyrm>of(FractalWyrm::new, MobCategory.MONSTER).sized(1.2F, 1.0F).fireImmune()
                    .clientTrackingRange(12).build("fractal_wyrm"));
    public static final DeferredHolder<EntityType<?>, EntityType<AxiomSentinel>> AXIOM_SENTINEL = ENTITIES.register("axiom_sentinel",
            () -> EntityType.Builder.<AxiomSentinel>of(AxiomSentinel::new, MobCategory.MONSTER).sized(1.4F, 1.4F).fireImmune()
                    .clientTrackingRange(12).build("axiom_sentinel"));

    // ---- creatures of the Quantum Foam, the Transfinite Hierarchy and the black / white holes
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Floater>> VIRTUAL_PAIR = floater("virtual_pair", 0.6F, 0.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Floater>> HYPERCUBE_DRIFTER = floater("hypercube_drifter", 1.0F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Floater>> PHOTON_MOTH = floater("photon_moth", 0.7F, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.PlanckMite>> PLANCK_MITE = ENTITIES.register("planck_mite",
            () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.PlanckMite>of(com.theoreticalsciences.entity.ExpansionMobs.PlanckMite::new, MobCategory.MONSTER)
                    .sized(0.7F, 0.45F).clientTrackingRange(8).build("planck_mite"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Striker>> VACUUM_DRIFTER = striker("vacuum_drifter", 1.1F, 1.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Striker>> TIDAL_HORROR = striker("tidal_horror", 1.2F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.OrdinalHound>> ORDINAL_HOUND = ENTITIES.register("ordinal_hound",
            () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.OrdinalHound>of(com.theoreticalsciences.entity.ExpansionMobs.OrdinalHound::new, MobCategory.MONSTER)
                    .sized(0.8F, 0.9F).clientTrackingRange(10).build("ordinal_hound"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Bellhop>> HILBERT_BELLHOP = ENTITIES.register("hilbert_bellhop",
            () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.Bellhop>of(com.theoreticalsciences.entity.ExpansionMobs.Bellhop::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.9F).clientTrackingRange(10).build("hilbert_bellhop"));

    // ---- creatures of the Antiverse
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Striker>> ANNIHILATOR = striker("annihilator", 1.0F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.PlanckMite>> POSITRONIUM_SKITTERER = ENTITIES.register("positronium_skitterer",
            () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.PlanckMite>of(com.theoreticalsciences.entity.ExpansionMobs.PlanckMite::new, MobCategory.MONSTER)
                    .sized(0.8F, 0.5F).clientTrackingRange(8).build("positronium_skitterer"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Floater>> ANTIHYDROGEN_WISP = floater("antihydrogen_wisp", 0.6F, 0.6F);

    // ---- Alpha Omega
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.boss.GenesisEntity>> GENESIS = ENTITIES.register("alpha_omega_genesis",
            () -> EntityType.Builder.<com.theoreticalsciences.boss.GenesisEntity>of(com.theoreticalsciences.boss.GenesisEntity::new, MobCategory.MISC).sized(2.0F, 2.0F)
                    .fireImmune().clientTrackingRange(16).updateInterval(2).build("alpha_omega_genesis"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.boss.AlphaOmegaEntity>> ALPHA_OMEGA = ENTITIES.register("alpha_omega",
            () -> EntityType.Builder.<com.theoreticalsciences.boss.AlphaOmegaEntity>of(com.theoreticalsciences.boss.AlphaOmegaEntity::new, MobCategory.MONSTER).sized(3.0F, 4.0F)
                    .fireImmune().clientTrackingRange(16).build("alpha_omega"));

    private static DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Floater>> floater(String id, float w, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.Floater>of(com.theoreticalsciences.entity.ExpansionMobs.Floater::new,
                MobCategory.AMBIENT).sized(w, h).fireImmune().clientTrackingRange(8).build(id));
    }

    private static DeferredHolder<EntityType<?>, EntityType<com.theoreticalsciences.entity.ExpansionMobs.Striker>> striker(String id, float w, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<com.theoreticalsciences.entity.ExpansionMobs.Striker>of(com.theoreticalsciences.entity.ExpansionMobs.Striker::new,
                MobCategory.MONSTER).sized(w, h).fireImmune().clientTrackingRange(10).build(id));
    }

    static {
        egg("virtual_pair", VIRTUAL_PAIR, 0xFF7090, 0x80F0FF);
        egg("hypercube_drifter", HYPERCUBE_DRIFTER, 0x203048, 0x60E0FF);
        egg("photon_moth", PHOTON_MOTH, 0xFFF8E0, 0xFFD060);
        egg("planck_mite", PLANCK_MITE, 0x3A6080, 0xA0E8FF);
        egg("vacuum_drifter", VACUUM_DRIFTER, 0x101828, 0x6080C0);
        egg("tidal_horror", TIDAL_HORROR, 0x200808, 0xFF5020);
        egg("annihilator", ANNIHILATOR, 0x2A1408, 0xFFB070);
        egg("positronium_skitterer", POSITRONIUM_SKITTERER, 0x302018, 0xFF70B0);
        egg("antihydrogen_wisp", ANTIHYDROGEN_WISP, 0x081838, 0x80F0FF);
        egg("ordinal_hound", ORDINAL_HOUND, 0x30281A, 0xFFD060);
        egg("hilbert_bellhop", HILBERT_BELLHOP, 0x802020, 0xFFD060);
        egg("alpha_omega", ALPHA_OMEGA, 0x100818, 0xFFE080);
    }

    private static void egg(String id, DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> t, int bg, int fg) {
        tab(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(t, bg, fg, new Item.Properties())));
    }

    static {
        tab(ITEMS.register("fractal_wyrm_spawn_egg", () -> new DeferredSpawnEggItem(FRACTAL_WYRM, 0x203050, 0xFFD060, new Item.Properties())));
        tab(ITEMS.register("axiom_sentinel_spawn_egg", () -> new DeferredSpawnEggItem(AXIOM_SENTINEL, 0xE0E8FF, 0x60A0FF, new Item.Properties())));
    }

    // ---- fractal landmarks of the Platonic Realm
    public static final DeferredHolder<Feature<?>, FractalFeatures.Menger> MENGER = FEATURES.register("menger_sponge", () -> new FractalFeatures.Menger(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FractalFeatures.Sierpinski> SIERPINSKI = FEATURES.register("sierpinski_tetrahedron", () -> new FractalFeatures.Sierpinski(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FractalFeatures.Mandelbulb> MANDELBULB = FEATURES.register("mandelbulb", () -> new FractalFeatures.Mandelbulb(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FractalFeatures.CantorDust> CANTOR = FEATURES.register("cantor_dust", () -> new FractalFeatures.CantorDust(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FractalFeatures.FractalTree> TREE = FEATURES.register("fractal_tree", () -> new FractalFeatures.FractalTree(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, FractalFeatures.HilbertArchive> ARCHIVE = FEATURES.register("hilbert_archive", () -> new FractalFeatures.HilbertArchive(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.gr.BridgeBuilder.Feature> ER_BRIDGE = FEATURES.register("einstein_rosen_bridge",
            () -> new com.theoreticalsciences.gr.BridgeBuilder.Feature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.TransfiniteFeatures.BoundaryLayers> BOUNDARY = FEATURES.register("foam_boundary_layers",
            () -> new com.theoreticalsciences.world.TransfiniteFeatures.BoundaryLayers(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.TransfiniteFeatures.HilbertHotel> HOTEL = FEATURES.register("hilbert_hotel",
            () -> new com.theoreticalsciences.world.TransfiniteFeatures.HilbertHotel(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.TransfiniteFeatures.TesseractFrame> TESSERACT_FRAME = FEATURES.register("tesseract_frame",
            () -> new com.theoreticalsciences.world.TransfiniteFeatures.TesseractFrame(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.TransfiniteFeatures.HopfRings> HOPF = FEATURES.register("hopf_rings",
            () -> new com.theoreticalsciences.world.TransfiniteFeatures.HopfRings(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.TransfiniteFeatures.OrdinalSpire> SPIRE = FEATURES.register("ordinal_spire",
            () -> new com.theoreticalsciences.world.TransfiniteFeatures.OrdinalSpire(NoneFeatureConfiguration.CODEC));

    private static <T extends Item> DeferredHolder<Item, T> tab(DeferredHolder<Item, T> h) {
        TAB.add(h);
        return h;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        FEATURES.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) ->
                e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CONFLUENCE_BE.get(), (be, side) -> be.fluids));
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(FRACTAL_WYRM.get(), FractalWyrm.createAttributes().build());
            e.put(AXIOM_SENTINEL.get(), AxiomSentinel.createAttributes().build());
            for (var t : List.of(VIRTUAL_PAIR, HYPERCUBE_DRIFTER, PHOTON_MOTH, ANTIHYDROGEN_WISP)) e.put(t.get(), com.theoreticalsciences.entity.ExpansionMobs.Floater.createAttributes().build());
            for (var t : List.of(VACUUM_DRIFTER, TIDAL_HORROR, ANNIHILATOR)) e.put(t.get(), com.theoreticalsciences.entity.ExpansionMobs.Striker.createAttributes().build());
            e.put(PLANCK_MITE.get(), com.theoreticalsciences.entity.ExpansionMobs.PlanckMite.createAttributes().build());
            e.put(POSITRONIUM_SKITTERER.get(), com.theoreticalsciences.entity.ExpansionMobs.PlanckMite.createAttributes().build());
            e.put(ORDINAL_HOUND.get(), com.theoreticalsciences.entity.ExpansionMobs.OrdinalHound.createAttributes().build());
            e.put(HILBERT_BELLHOP.get(), com.theoreticalsciences.entity.ExpansionMobs.Bellhop.createAttributes().build());
            e.put(ALPHA_OMEGA.get(), com.theoreticalsciences.boss.AlphaOmegaEntity.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            e.register(FRACTAL_WYRM.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(AXIOM_SENTINEL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules, op);
            var air = SpawnPlacementTypes.NO_RESTRICTIONS;
            var hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            for (var t : List.of(VIRTUAL_PAIR, HYPERCUBE_DRIFTER, PHOTON_MOTH, ANTIHYDROGEN_WISP))
                e.register(t.get(), air, hm, (type, level, reason, pos, r) -> level.getBlockState(pos).isAir(), op);
            for (var t : List.of(VACUUM_DRIFTER, TIDAL_HORROR, ANNIHILATOR))
                e.register(t.get(), air, hm, (type, level, reason, pos, r) -> level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && level.getBlockState(pos).isAir(), op);
            e.register(PLANCK_MITE.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(POSITRONIUM_SKITTERER.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(ORDINAL_HOUND.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(HILBERT_BELLHOP.get(), SpawnPlacementTypes.ON_GROUND, hm, (type, level, reason, pos, r) -> level.getBlockState(pos.below()).isSolid(), op);
        });
    }

    private TranscendRegistry() {}
}
