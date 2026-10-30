package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.machine.PortBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public final class QHBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);

    public static final Map<MachineType, DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>>> MACHINES = new EnumMap<>(MachineType.class);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortBlockEntity>> PORT = BLOCK_ENTITIES.register("interface_port",
            () -> BlockEntityType.Builder.of(PortBlockEntity::new, QHBlocks.INTERFACE_PORT.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.block.BubbleUniverseBlockEntity>> BUBBLE_UNIVERSE =
            BLOCK_ENTITIES.register("bubble_universe", () -> BlockEntityType.Builder.<com.theoreticalsciences.block.BubbleUniverseBlockEntity>of(
                    (pos, state) -> new com.theoreticalsciences.block.BubbleUniverseBlockEntity(QHBlockEntities.BUBBLE_UNIVERSE.get(), pos, state),
                    QHBlocks.get("bubble_universe")).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.theoreticalsciences.block.FracturedRealityBlockEntity>> FRACTURED_REALITY =
            BLOCK_ENTITIES.register("fractured_reality", () -> BlockEntityType.Builder.of(com.theoreticalsciences.block.FracturedRealityBlockEntity::new,
                    QHBlocks.get("fractured_reality")).build(null));

    static {
        for (MachineType type : MachineType.values()) {
            MACHINES.put(type, BLOCK_ENTITIES.register(type.id(), () -> {
                Block block = QHBlocks.MACHINES.get(type).get();
                @SuppressWarnings({"unchecked", "DataFlowIssue"})
                BlockEntityType<MachineBlockEntity> t = BlockEntityType.Builder.<MachineBlockEntity>of(
                        (pos, state) -> MachineBlockEntity.create(type, pos, state),
                        block).build(null);
                return t;
            }));
        }
    }

    public static BlockEntityType<MachineBlockEntity> machine(MachineType type) {
        return MACHINES.get(type).get();
    }

    public static Iterable<BlockEntityType<MachineBlockEntity>> allMachines() {
        return Arrays.stream(MachineType.values()).map(QHBlockEntities::machine).toList();
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private QHBlockEntities() {}
}
