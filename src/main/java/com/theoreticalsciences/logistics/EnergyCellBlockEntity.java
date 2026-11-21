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
            case "vanadium_flow_battery" -> 50_000_000L;          // 1.2.0: the first chemical store
            case "casimir_energy_cell" -> 4_000_000_000L;
            case "vacuum_energy_cell" -> 256_000_000_000L;
            case "singularity_energy_cell" -> 16_000_000_000_000L;
            default -> 4_000_000_000_000_000_000L;      // omnium: as good as bottomless
        };
    }

    public static long rateOf(String id) {
        return switch (id) {
            case "vanadium_flow_battery" -> 20_000L;
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

    /** 1.2.0: a Vanadium Flow Battery (its voltage follows its state of charge). */
    public static final double FULL = 0.95;

    public boolean flow() {
        return "vanadium_flow_battery".equals(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath());
    }

    /** Nernst: E = E° + (RT/F)·ln(SoC/(1 − SoC)) for the vanadium couples (E° = 1.26 V), per cell. */
    public double voltage() {
        double soc = Math.max(1e-4, Math.min(1 - 1e-4, (double) energy.stored() / Math.max(1, energy.capacity())));
        return 1.26 + 2 * 0.0257 * Math.log(soc / (1 - soc));
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
            if (input && flow()) {
                // 1.2.0: a flow battery stops taking charge at 95% - charged further, the water in its electrolyte splits
                long room = (long) (energy.capacity() * FULL) - energy.stored();
                a = (int) Math.max(0, Math.min(a, room));
            }
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
