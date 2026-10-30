package com.theoreticalsciences.extreme;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Avaritia-style mass-crafting layer: the Extreme Assembly Table and the Singularity Compactor. */
public final class ExtremeRegistry {
    static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TheoreticalSciences.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExtremeRecipe>> EXTREME_TYPE = TYPES.register("extreme_crafting",
            () -> RecipeType.simple(TheoreticalSciences.id("extreme_crafting")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CompactionRecipe>> COMPACTION_TYPE = TYPES.register("compaction",
            () -> RecipeType.simple(TheoreticalSciences.id("compaction")));
    public static final DeferredHolder<RecipeSerializer<?>, ExtremeShapedRecipe.Serializer> SHAPED = SERIALIZERS.register("extreme_shaped",
            ExtremeShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ExtremeShapelessRecipe.Serializer> SHAPELESS = SERIALIZERS.register("extreme_shapeless",
            ExtremeShapelessRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ExtremeDynamicRecipe.Serializer> DYNAMIC = SERIALIZERS.register("extreme_dynamic",
            ExtremeDynamicRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ExtremeClusterRecipe.Serializer> CLUSTER = SERIALIZERS.register("extreme_cluster",
            ExtremeClusterRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, ExtremeBundleRecipe.Serializer> BUNDLE = SERIALIZERS.register("extreme_bundle",
            ExtremeBundleRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, CompactionRecipe.Serializer> COMPACTION_SERIALIZER = SERIALIZERS.register("compaction",
            CompactionRecipe.Serializer::new);

    public static final DeferredHolder<Block, ExtremeTableBlock> EXTREME_TABLE = BLOCKS.registerBlock("extreme_assembly_table", ExtremeTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F, 1200.0F).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 6));
    public static final DeferredHolder<Block, CompactorBlock> COMPACTOR = BLOCKS.registerBlock("singularity_compactor", CompactorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(8.0F, 1200.0F).requiresCorrectToolForDrops().sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 8));
    public static final DeferredHolder<Block, AutoAssemblerBlock> AUTO_ASSEMBLER = BLOCKS.registerBlock("autonomous_extreme_assembler", AutoAssemblerBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0F, 1200.0F).sound(com.theoreticalsciences.registry.QHSounds.MACHINE).lightLevel(s -> 8));
    public static final DeferredHolder<Item, BlockItem> AUTO_ASSEMBLER_ITEM = ITEMS.registerSimpleBlockItem(AUTO_ASSEMBLER, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredHolder<Item, BlockItem> EXTREME_TABLE_ITEM = ITEMS.registerSimpleBlockItem(EXTREME_TABLE, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredHolder<Item, BlockItem> COMPACTOR_ITEM = ITEMS.registerSimpleBlockItem(COMPACTOR, new Item.Properties().rarity(Rarity.EPIC));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtremeTableBlockEntity>> EXTREME_TABLE_BE = BES.register("extreme_assembly_table",
            () -> BlockEntityType.Builder.of(ExtremeTableBlockEntity::new, EXTREME_TABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoAssemblerBlockEntity>> AUTO_ASSEMBLER_BE = BES.register("autonomous_extreme_assembler",
            () -> BlockEntityType.Builder.of(AutoAssemblerBlockEntity::new, AUTO_ASSEMBLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompactorBlockEntity>> COMPACTOR_BE = BES.register("singularity_compactor",
            () -> BlockEntityType.Builder.of(CompactorBlockEntity::new, COMPACTOR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ExtremeTableMenu>> EXTREME_MENU = MENUS.register("extreme_assembly_table",
            () -> IMenuTypeExtension.create(ExtremeTableMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<CompactorMenu>> COMPACTOR_MENU = MENUS.register("singularity_compactor",
            () -> IMenuTypeExtension.create(CompactorMenu::fromNetwork));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        MENUS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, COMPACTOR_BE.get(), (be, side) -> be.automation);
            e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, AUTO_ASSEMBLER_BE.get(), (be, side) -> be.automation);
        });
    }

    private ExtremeRegistry() {}
}
