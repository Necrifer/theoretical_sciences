package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 1.2.0: the Governor (Control Theory). Watt's flyball governor held a steam engine's speed by feedback: run too fast
 * and the balls swing out and close the throttle. This one sits against a machine with a console (a multiblock's
 * controller, or a console machine) and, once a second, reads how far each setting is from the machine's ideal and
 * corrects a third of the error (see {@link ControllerBlockEntity#govern}). It holds the machine working at about 75%
 * precision whatever recipe it runs; the console's fine tuning is still worth the rest. It draws {@link #COST} FE a tick.
 */
public class GovernorBlockEntity extends BlockEntity {
    public static final int COST = 32, CAPACITY = 400_000;

    public class Store extends EnergyStorage {
        Store() {
            super(CAPACITY, 4096, 0);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int r = super.receiveEnergy(amount, simulate);
            if (r > 0 && !simulate) setChanged();
            return r;
        }

        boolean spend(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }
    }

    public final Store energy = new Store();
    private boolean holding;

    public GovernorBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.GOVERNOR.get(), pos, state);
    }

    public boolean holding() {
        return holding;
    }

    /** The machine it governs: the first neighbour with a console. */
    public ControllerBlockEntity machine() {
        if (level == null) return null;
        for (Direction d : Direction.values())
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ControllerBlockEntity c && TuningForkItem.tunable(c)) return c;
        return null;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        boolean powered = energy.spend(COST);
        boolean now = holding;
        if (Math.floorMod(sl.getGameTime() + worldPosition.hashCode(), 20L) == 0) {
            ControllerBlockEntity c = powered ? machine() : null;
            now = c != null && c.govern(sl);
            setChanged();
        }
        if (!powered) now = false;
        if (now != holding) {
            holding = now;
            sl.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, now));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Energy", energy.serializeNBT(reg));
        tag.putBoolean("Holding", holding);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Energy")) energy.deserializeNBT(reg, tag.get("Energy"));
        holding = tag.getBoolean("Holding");
    }
}
