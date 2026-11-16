package com.theoreticalsciences.home;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** 1.8.0, Recreating Home: the Empyrean Assembly Table, the Offering Altar, the portal frame and the portal. */
public final class HomeRegistry {
    static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TheoreticalSciences.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EmpyreanRecipe>> EMPYREAN_TYPE = TYPES.register("empyrean",
            () -> RecipeType.simple(TheoreticalSciences.id("empyrean")));
    public static final DeferredHolder<RecipeSerializer<?>, EmpyreanRecipe.Serializer> EMPYREAN_SERIALIZER = SERIALIZERS.register("empyrean",
            EmpyreanRecipe.Serializer::new);

    public static final DeferredHolder<Block, EmpyreanTableBlock> TABLE = BLOCKS.registerBlock("empyrean_assembly_table", EmpyreanTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(4.0F, 3600000F).sound(SoundType.STONE).lightLevel(s -> 12).noLootTable());
    public static final DeferredHolder<Block, OfferingAltarBlock> ALTAR = BLOCKS.registerBlock("offering_altar", OfferingAltarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(4.0F, 3600000F).sound(SoundType.STONE).lightLevel(s -> 10));
    public static final DeferredHolder<Block, PortalFrameBlock> FRAME = BLOCKS.registerBlock("homeward_portal_frame", PortalFrameBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(6.0F, 3600000F).sound(SoundType.STONE)
                    .lightLevel(s -> s.getValue(PortalFrameBlock.KEYSTONE) ? 14 : 4));
    public static final DeferredHolder<Block, HomePortalBlock> PORTAL = BLOCKS.registerBlock("homeward_portal", HomePortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1.0F, 3600000F).noCollission().noLootTable().lightLevel(s -> 15)
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.GLASS));

    public static final DeferredHolder<Item, BlockItem> TABLE_ITEM = ITEMS.register("empyrean_assembly_table",
            () -> new EmpyreanTableItem(TABLE.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    // ------------------------------------------------------------------ 1.9.0: what stands and who lives in Omnipotency
    public static final DeferredHolder<Block, HomeDecor.Urn> URN = BLOCKS.registerBlock("sealed_urn", HomeDecor.Urn::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.2F).sound(SoundType.DECORATED_POT).noOcclusion());
    public static final DeferredHolder<Block, HomeDecor.Bell> BELL = BLOCKS.registerBlock("choir_bell", HomeDecor.Bell::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredHolder<Block, HomeDecor.Stone> STONE = BLOCKS.registerBlock("remembrance_stone", HomeDecor.Stone::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(3.0F, 3600000F).sound(SoundType.STONE).lightLevel(s -> 6));
    public static final DeferredHolder<Item, BlockItem> URN_ITEM = ITEMS.registerSimpleBlockItem(URN, new Item.Properties());
    public static final DeferredHolder<Item, BlockItem> BELL_ITEM = ITEMS.registerSimpleBlockItem(BELL, new Item.Properties());
    public static final DeferredHolder<Item, BlockItem> STONE_ITEM = ITEMS.registerSimpleBlockItem(STONE, new Item.Properties());

    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.HaloLamb>> LAMB = ENTITIES.register("halo_lamb",
            () -> EntityType.Builder.of(HomeMobs.HaloLamb::new, MobCategory.CREATURE).sized(0.8F, 0.95F).eyeHeight(0.8F).clientTrackingRange(10).build("halo_lamb"));
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.Cherub>> CHERUB = ENTITIES.register("cherub",
            () -> EntityType.Builder.of(HomeMobs.Cherub::new, MobCategory.AMBIENT).sized(0.9F, 1.8F).eyeHeight(1.45F).clientTrackingRange(10).build("cherub"));
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.ChoirSeraph>> SERAPH = ENTITIES.register("choir_seraph",
            () -> EntityType.Builder.of(HomeMobs.ChoirSeraph::new, MobCategory.AMBIENT).sized(0.9F, 2.0F).eyeHeight(1.6F).clientTrackingRange(10).build("choir_seraph"));
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.CloudRay>> RAY = ENTITIES.register("cloud_ray",
            () -> EntityType.Builder.of(HomeMobs.CloudRay::new, MobCategory.AMBIENT).sized(2.6F, 0.6F).eyeHeight(0.3F).clientTrackingRange(12).build("cloud_ray"));
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.Dweller>> DWELLER = ENTITIES.register("dweller",
            () -> EntityType.Builder.of(HomeMobs.Dweller::new, MobCategory.MISC).sized(0.6F, 1.9F).eyeHeight(1.65F).clientTrackingRange(10).build("dweller"));
    public static final DeferredHolder<EntityType<?>, EntityType<HomeMobs.ThresholdWarden>> WARDEN = ENTITIES.register("threshold_warden",
            () -> EntityType.Builder.of(HomeMobs.ThresholdWarden::new, MobCategory.MISC).sized(2.2F, 2.5F).eyeHeight(1.25F).clientTrackingRange(12).fireImmune()
                    .build("threshold_warden"));

    public static final DeferredHolder<Feature<?>, HomeStructures.Shrine> SHRINE = FEATURES.register("wayside_shrine", () -> new HomeStructures.Shrine(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HomeStructures.Hamlet> HAMLET = FEATURES.register("hamlet", () -> new HomeStructures.Hamlet(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HomeStructures.ChoirHall> CHOIR_HALL = FEATURES.register("choir_hall", () -> new HomeStructures.ChoirHall(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HomeStructures.Temple> TEMPLE = FEATURES.register("temple_of_remembrance", () -> new HomeStructures.Temple(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HomeStructures.Beacon> BEACON = FEATURES.register("beacon_of_return", () -> new HomeStructures.Beacon(NoneFeatureConfiguration.CODEC));

    /** The record of Omnipotency's music, found now and then in a sealed urn. */
    public static final DeferredHolder<Item, Item> DISC = ITEMS.register("music_disc_homecoming", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(Registries.JUKEBOX_SONG, TheoreticalSciences.id("homecoming")))));
    public static final List<DeferredHolder<Item, ? extends Item>> TAB_190 = new java.util.ArrayList<>(List.of(URN_ITEM, BELL_ITEM, STONE_ITEM, DISC));

    static {
        Object[][] eggs = {{"halo_lamb", LAMB, 0xFFF4D0, 0xFFD870}, {"cherub", CHERUB, 0xD89A50, 0xFFFFFF}, {"choir_seraph", SERAPH, 0xFFC850, 0xE86018},
                {"cloud_ray", RAY, 0xE8F0FF, 0xB8C8E8}, {"dweller", DWELLER, 0xF4ECD8, 0xD8A040}, {"threshold_warden", WARDEN, 0xF0C050, 0x3C8CC8}};
        for (Object[] e : eggs) {
            @SuppressWarnings("unchecked")
            DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>> type = (DeferredHolder<EntityType<?>, ? extends EntityType<? extends net.minecraft.world.entity.Mob>>) e[1];
            TAB_190.add(ITEMS.register(e[0] + "_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(type, (int) e[2], (int) e[3], new Item.Properties())));
        }
    }

    public static final DeferredHolder<Item, BlockItem> ALTAR_ITEM = ITEMS.registerSimpleBlockItem(ALTAR, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredHolder<Item, BlockItem> FRAME_ITEM = ITEMS.registerSimpleBlockItem(FRAME, new Item.Properties().rarity(Rarity.EPIC));
    public static final List<DeferredHolder<Item, BlockItem>> TAB = List.of(TABLE_ITEM, ALTAR_ITEM, FRAME_ITEM);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmpyreanTableBlockEntity>> TABLE_BE = BES.register("empyrean_assembly_table",
            () -> BlockEntityType.Builder.of(EmpyreanTableBlockEntity::new, TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OfferingAltarBlock.Entity>> ALTAR_BE = BES.register("offering_altar",
            () -> BlockEntityType.Builder.of(OfferingAltarBlock.Entity::new, ALTAR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<EmpyreanTableMenu>> TABLE_MENU = MENUS.register("empyrean_assembly_table",
            () -> IMenuTypeExtension.create(EmpyreanTableMenu::fromNetwork));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        MENUS.register(bus);
        ENTITIES.register(bus);
        FEATURES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
            e.put(LAMB.get(), HomeMobs.HaloLamb.createAttributes().build());
            e.put(CHERUB.get(), HomeMobs.Cherub.createAttributes().build());
            e.put(SERAPH.get(), HomeMobs.ChoirSeraph.createAttributes().build());
            e.put(RAY.get(), HomeMobs.CloudRay.createAttributes().build());
            e.put(DWELLER.get(), HomeMobs.Dweller.createAttributes().build());
            e.put(WARDEN.get(), HomeMobs.ThresholdWarden.createAttributes().build());
        });
        bus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e) -> {
            var and = net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE;
            // the lamb on Halo Grass; the fliers in open air with ground not far under them (the ray anywhere in the open)
            e.register(LAMB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, rnd) -> HomeMobs.isBlock(level.getBlockState(pos.below()), "halo_grass"), and);
            e.register(CHERUB.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (type, level, reason, pos, rnd) -> openAir(level, pos, 6), and);
            e.register(SERAPH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (type, level, reason, pos, rnd) -> openAir(level, pos, 8), and);
            e.register(RAY.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (type, level, reason, pos, rnd) -> pos.getY() > 16 && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                            && level.getBlockState(pos.below()).isAir(), and);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HomeMobs::onBreak);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HomeMobs::onHurt);
        bus.addListener((RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TABLE_BE.get(), (be, side) -> be.automation);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALTAR_BE.get(), (be, side) -> be.automation);
        });
    }

    /** Air here and above, and something to stand over within {@code down} blocks. */
    private static boolean openAir(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos, int down) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        for (int i = 1; i <= down; i++) if (!level.getBlockState(pos.below(i)).isAir()) return i > 1;
        return false;
    }

    private HomeRegistry() {}
}
