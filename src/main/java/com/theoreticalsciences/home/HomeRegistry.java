package com.theoreticalsciences.home;

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

    public static final DeferredHolder<Item, BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem(TABLE, new Item.Properties().rarity(Rarity.EPIC));
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
        bus.addListener((RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TABLE_BE.get(), (be, side) -> be.automation);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALTAR_BE.get(), (be, side) -> be.automation);
        });
    }

    private HomeRegistry() {}
}
