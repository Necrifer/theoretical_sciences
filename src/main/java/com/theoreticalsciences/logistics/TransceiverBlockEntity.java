package com.theoreticalsciences.logistics;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransceiverBlockEntity extends BlockEntity {
    /** Every loaded transceiver on the server, by frequency. */
    static final Map<Integer, Set<TransceiverBlockEntity>> CHANNELS = new ConcurrentHashMap<>();

    public int frequency;
    private int rr;      // round-robin start among receivers

    public TransceiverBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.TRANSCEIVER.get(), pos, state);
    }

    boolean tier2() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath().equals("entangled_transceiver");
    }

    public int itemRate() {
        return tier2() ? 8192 : 256;
    }

    public int fluidRate() {
        return tier2() ? 4_096_000 : 64_000;
    }

    public int energyRate() {
        return tier2() ? Integer.MAX_VALUE : 64_000_000;
    }

    public boolean sending() {
        return getBlockState().getValue(BlockStateProperties.ENABLED);
    }

    Direction facing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    BlockPos target() {
        return worldPosition.relative(facing());
    }

    public void setFrequency(int f) {
        unregister();
        frequency = f;
        register();
        setChanged();
    }

    void register() {
        if (level != null && !level.isClientSide && frequency != 0) CHANNELS.computeIfAbsent(frequency, k -> ConcurrentHashMap.newKeySet()).add(this);
    }

    void unregister() {
        var set = CHANNELS.get(frequency);
        if (set != null) set.remove(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        register();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregister();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unregister();
    }

    List<TransceiverBlockEntity> receivers() {
        var set = CHANNELS.get(frequency);
        if (set == null) return List.of();
        List<TransceiverBlockEntity> out = new ArrayList<>();
        for (TransceiverBlockEntity t : set)
            if (t != this && !t.isRemoved() && t.level != null && t.level.getServer() == level.getServer() && !t.sending()) out.add(t);
        if (out.size() > 1) Collections.rotate(out, -(rr++ % out.size()));
        return out;
    }

    void serverTick() {
        if (frequency == 0 || !sending() || level == null) return;
        List<TransceiverBlockEntity> rx = receivers();
        if (rx.isEmpty()) return;
        Direction f = facing(), in = f.getOpposite();
        BlockPos src = target();
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, src, in);
        IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, src, in);
        IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, src, in);
        int itemBudget = itemRate(), fluidBudget = fluidRate();
        long energyBudget = energyRate();
        for (TransceiverBlockEntity r : rx) {
            var rl = r.level;
            Direction rf = r.facing();
            BlockPos dst = r.target();
            if (!rl.isLoaded(dst)) continue;
            if (items != null && itemBudget > 0) {
                IItemHandler d = rl.getCapability(Capabilities.ItemHandler.BLOCK, dst, rf.getOpposite());
                if (d != null) itemBudget -= moveItems(items, d, itemBudget, this::spill);
            }
            if (fluids != null && fluidBudget > 0) {
                IFluidHandler d = rl.getCapability(Capabilities.FluidHandler.BLOCK, dst, rf.getOpposite());
                if (d != null) fluidBudget -= moveFluids(fluids, d, fluidBudget);
            }
            if (energy != null && energyBudget > 0 && energy.canExtract()) {
                IEnergyStorage d = rl.getCapability(Capabilities.EnergyStorage.BLOCK, dst, rf.getOpposite());
                if (d != null && d.canReceive()) {
                    int can = energy.extractEnergy((int) Math.min(Integer.MAX_VALUE, energyBudget), true);
                    int took = d.receiveEnergy(can, true);
                    if (took > 0) {
                        int real = energy.extractEnergy(took, false);
                        d.receiveEnergy(real, false);
                        energyBudget -= real;
                    }
                }
            }
        }
    }

    /** Last resort for items that neither side would take back: they drop out of the transceiver, never vanish. */
    private void spill(ItemStack s) {
        if (level != null && !s.isEmpty())
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, s);
    }

    static int moveItems(IItemHandler src, IItemHandler dst, int budget, java.util.function.Consumer<ItemStack> spill) {
        int moved = 0;
        for (int slot = 0; slot < src.getSlots() && moved < budget; slot++) {
            ItemStack sim = src.extractItem(slot, budget - moved, true);
            if (sim.isEmpty()) continue;
            ItemStack rem = ItemHandlerHelper.insertItemStacked(dst, sim, true);
            int n = sim.getCount() - rem.getCount();
            if (n <= 0) continue;
            ItemStack real = src.extractItem(slot, n, false);
            ItemStack left = ItemHandlerHelper.insertItemStacked(dst, real, false);
            moved += real.getCount() - left.getCount();
            if (!left.isEmpty()) {
                // the target took less than it said it would: put the rest back where it came from (or any slot of the
                // source), and only if nothing will hold it, drop it - before, the rest was silently lost
                left = src.insertItem(slot, left, false);
                if (!left.isEmpty()) left = ItemHandlerHelper.insertItemStacked(src, left, false);
                if (!left.isEmpty()) spill.accept(left);
            }
        }
        return moved;
    }

    static int moveFluids(IFluidHandler src, IFluidHandler dst, int budget) {
        FluidStack sim = src.drain(budget, IFluidHandler.FluidAction.SIMULATE);
        if (sim.isEmpty()) return 0;
        int fit = dst.fill(sim, IFluidHandler.FluidAction.SIMULATE);
        if (fit <= 0) return 0;
        FluidStack real = src.drain(sim.copyWithAmount(fit), IFluidHandler.FluidAction.EXECUTE);
        if (real.isEmpty()) return 0;
        int filled = dst.fill(real.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (filled < real.getAmount()) {
            // the target took less than it promised: pour what is left back into the source instead of losing it
            src.fill(real.copyWithAmount(real.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.putInt("Frequency", frequency);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        frequency = t.getInt("Frequency");
    }
}
