package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.field.FieldProjectorBlockEntity;
import com.theoreticalsciences.logistics.EnergyCellBlockEntity;
import com.theoreticalsciences.logistics.TransceiverBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entities for power storage, entangled transport and field projection (the blocks themselves are content-list blocks). */
public final class QHLogistics {
    static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);

    static Block[] ofKind(String kind) {
        return QHContentList.BLOCKS.stream().filter(d -> d.kind().equals(kind)).map(d -> QHBlocks.get(d.id())).toArray(Block[]::new);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldProjectorBlockEntity>> FIELD_PROJECTOR = BES.register("field_projector",
            () -> BlockEntityType.Builder.of(FieldProjectorBlockEntity::new, ofKind("field_projector")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCellBlockEntity>> ENERGY_CELL = BES.register("energy_cell",
            () -> BlockEntityType.Builder.of(EnergyCellBlockEntity::new, ofKind("energy_cell")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransceiverBlockEntity>> TRANSCEIVER = BES.register("entangled_transceiver",
            () -> BlockEntityType.Builder.of(TransceiverBlockEntity::new, ofKind("transceiver")).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.dungeon.AnomalyEngineBlockEntity>> ANOMALY_ENGINE = BES.register("anomaly_engine",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.dungeon.AnomalyEngineBlockEntity::new, ofKind("anomaly")).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.dungeon.CacheBlockEntity>> CACHE = BES.register("containment_cache",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.dungeon.CacheBlockEntity::new, ofKind("cache")).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.chrono.ChronalAcceleratorBlockEntity>> CHRONAL_ACCELERATOR = BES.register("chronal_accelerator",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.chrono.ChronalAcceleratorBlockEntity::new, ofKind("chrono_accel")).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.converge.AltarBlockEntity>> CONV_ALTAR = BES.register("convergence_altar",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.converge.AltarBlockEntity::new, ofKind("conv_altar")).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.converge.PedestalBlockEntity>> CONV_PEDESTAL = BES.register("resonance_pedestal",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.converge.PedestalBlockEntity::new, ofKind("conv_pedestal")).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.storage.InfinityCacheBlockEntity>> INFINITY_CACHE = BES.register("transcendent_cache",
            () -> BlockEntityType.Builder.of(com.theoreticalsciences.storage.InfinityCacheBlockEntity::new, ofKind("infinity_cache")).build(null));

    public static void register(IEventBus bus) {
        BES.register(bus);
        bus.addListener((RegisterCapabilitiesEvent e) -> {
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FIELD_PROJECTOR.get(), (be, side) -> be.energy);
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_CELL.get(), EnergyCellBlockEntity::side);
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHRONAL_ACCELERATOR.get(), (be, side) -> be.energy);
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CONV_ALTAR.get(), (be, side) -> be.energy);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, INFINITY_CACHE.get(), (be, side) -> be.automation);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ANOMALY_ENGINE.get(), (be, side) -> be.isBuilt() ? be.items : null);
            e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ANOMALY_ENGINE.get(), (be, side) -> be.isBuilt() ? be.energy : null);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONV_ALTAR.get(), (be, side) -> be.automation);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CONV_PEDESTAL.get(), (be, side) -> be.automation);
            e.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CHRONAL_ACCELERATOR.get(), (be, side) -> be.chronons);
            e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CACHE.get(), (be, side) -> new net.neoforged.neoforge.items.wrapper.InvWrapper(be));
        });
    }

    private QHLogistics() {}
}
