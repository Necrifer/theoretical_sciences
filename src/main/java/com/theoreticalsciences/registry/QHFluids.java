package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.QHBucketItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/** All exotic fluids and gases. Liquids get a world block; gases only live in tanks and containment cells. */
public final class QHFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TheoreticalSciences.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, TheoreticalSciences.MODID);

    public static final class Entry {
        public final QHContentList.FluidDef def;
        public DeferredHolder<FluidType, FluidType> type;
        public DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public DeferredHolder<Block, LiquidBlock> block; // null for gases
        public DeferredHolder<Item, BucketItem> bucket;

        Entry(QHContentList.FluidDef def) {
            this.def = def;
        }

        public FlowingFluid still() {
            return source.get();
        }
    }

    public static final Map<String, Entry> ALL = new LinkedHashMap<>();

    static {
        for (QHContentList.FluidDef def : QHContentList.FLUIDS) {
            Entry e = new Entry(def);
            String id = def.id();
            e.type = TYPES.register(id, () -> typeFor(id, FluidType.Properties.create()
                    .descriptionId("fluid_type.theoreticalsciences." + id)
                    .lightLevel(def.light())
                    .density(def.density())
                    .viscosity(Math.max(1, def.viscosity()))
                    .temperature(def.temperature())
                    .rarity(def.gas() ? Rarity.RARE : Rarity.UNCOMMON)
                    .canSwim(!def.gas())
                    .canDrown(!def.gas())
                    .canExtinguish(false)
                    .supportsBoating(false)
                    .canHydrate(false)
                    .sound(SoundActions.BUCKET_FILL, com.theoreticalsciences.registry.QHSounds.FLUID_FILL.get())
                    .sound(SoundActions.BUCKET_EMPTY, com.theoreticalsciences.registry.QHSounds.FLUID_EMPTY.get())));
            BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
            boolean boundary = id.equals("schrodinger_limit") || id.equals("relativity_breakdown");
            e.source = FLUIDS.register(id, () -> boundary ? new com.theoreticalsciences.block.StillSourceFluid(props[0]) : new BaseFlowingFluid.Source(props[0]));
            e.flowing = FLUIDS.register("flowing_" + id, () -> new BaseFlowingFluid.Flowing(props[0]));
            e.bucket = QHItems.ITEMS.register(id + "_cell", () -> new QHBucketItem(e.source.get(), def.gas(),
                    new Item.Properties().stacksTo(1)));
            if (!def.gas()) {
                BlockBehaviour.Properties bp = BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE).replaceable().noCollission().strength(100.0F)
                        .pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                        .lightLevel(s -> def.light());
                if (boundary) bp = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F)
                        .pushReaction(PushReaction.BLOCK).noLootTable().liquid().lightLevel(s -> def.light());
                final BlockBehaviour.Properties fbp = bp;
                e.block = QHBlocks.BLOCKS.register(id, () -> boundary
                        ? new com.theoreticalsciences.block.BoundaryFluidBlock(e.source.get(), fbp)
                        : id.equals("primordial_law")
                        ? new com.theoreticalsciences.block.LawSeaBlock(e.source.get(), fbp)
                        : new com.theoreticalsciences.block.QHLiquidBlock(e.source.get(), fbp));
            }
            props[0] = new BaseFlowingFluid.Properties(e.type, e.source, e.flowing)
                    .bucket(e.bucket)
                    .slopeFindDistance(def.gas() ? 1 : 3)
                    .levelDecreasePerBlock(def.viscosity() > 1500 ? 2 : 1)
                    .tickRate(Math.max(2, def.viscosity() / 200));
            if (e.block != null) props[0].block(e.block);
            ALL.put(id, e);
        }
    }

    static FluidType typeFor(String id, FluidType.Properties p) {
        return id.equals("mod_essence") ? new EssenceFluidType(p) : new BucketlessFluidType(p);
    }

    /** A mod's Essence (round 26): one fluid, named after the mod it was distilled from. */
    static final class EssenceFluidType extends BucketlessFluidType {
        EssenceFluidType(Properties p) {
            super(p);
        }

        @Override
        public net.minecraft.network.chat.Component getDescription(net.neoforged.neoforge.fluids.FluidStack stack) {
            return com.theoreticalsciences.transcend.Essences.name(stack);
        }
    }

    /** No bucket form: buckets (and anything that fills like one) can't take our fluids - only containment cells can. */
    static class BucketlessFluidType extends FluidType {
        BucketlessFluidType(Properties p) {
            super(p);
        }

        @Override
        public net.minecraft.world.item.ItemStack getBucket(net.neoforged.neoforge.fluids.FluidStack stack) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    public static Entry get(String id) {
        return ALL.get(id);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private QHFluids() {}
}
