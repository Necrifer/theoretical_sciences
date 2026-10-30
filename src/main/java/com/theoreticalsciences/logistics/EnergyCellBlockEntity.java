package com.theoreticalsciences.logistics;

import com.theoreticalsciences.machine.QHEnergyStorage;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyCellBlockEntity extends BlockEntity {
    /** Capacity and transfer rate (FE per tick) of each tier. */
    public static long capacityOf(String id) {
        return switch (id) {
            case "casimir_energy_cell" -> 4_000_000_000L;
            case "vacuum_energy_cell" -> 256_000_000_000L;
            case "singularity_energy_cell" -> 16_000_000_000_000L;
            default -> 4_000_000_000_000_000_000L;      // omnium: as good as bottomless
        };
    }

    public static long rateOf(String id) {
        return switch (id) {
            case "casimir_energy_cell" -> 16_000_000L;
            case "vacuum_energy_cell" -> 512_000_000L;
            default -> Integer.MAX_VALUE;
        };
    }

    public final QHEnergyStorage energy;
    private final IEnergyStorage in, out;

    public EnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.ENERGY_CELL.get(), pos, state);
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        energy = new QHEnergyStorage(capacityOf(id), rateOf(id), rateOf(id), this::setChanged);
        in = new Side(true);
        out = new Side(false);
    }

    public long rate() {
        return rateOf(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath());
    }

    Direction facing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    public static IEnergyStorage side(EnergyCellBlockEntity be, Direction side) {
        return side == null ? be.energy : side == be.facing() ? be.out : be.in;
    }

    /** One face's view: the output face only gives, the others only take. */
    final class Side implements IEnergyStorage {
        final boolean input;

        Side(boolean input) {
            this.input = input;
        }

        public int receiveEnergy(int a, boolean sim) {
            return input ? energy.receiveEnergy(a, sim) : 0;
        }

        public int extractEnergy(int a, boolean sim) {
            return input ? 0 : energy.extractEnergy(a, sim);
        }

        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        public boolean canExtract() {
            return !input;
        }

        public boolean canReceive() {
            return input;
        }
    }

    void serverTick() {
        if (energy.stored() <= 0 || level == null) return;
        Direction f = facing();
        if (!level.isLoaded(worldPosition.relative(f))) return;
        IEnergyStorage t = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(f), f.getOpposite());
        if (t == null || !t.canReceive()) return;
        long send = Math.min(energy.stored(), Math.min(rate(), Integer.MAX_VALUE));
        int got = t.receiveEnergy((int) send, false);
        if (got > 0) energy.drain(got);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.putLong("Energy", energy.stored());
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        energy.setRaw(t.getLong("Energy"));
    }

    // keep the charge in the item when broken (loot table copies minecraft:custom_data)
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        super.collectImplicitComponents(b);
        CompoundTag t = new CompoundTag();
        t.putLong("Energy", energy.stored());
        b.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput in) {
        super.applyImplicitComponents(in);
        CustomData d = in.get(DataComponents.CUSTOM_DATA);
        if (d != null) energy.setRaw(d.copyTag().getLong("Energy"));
    }
}
