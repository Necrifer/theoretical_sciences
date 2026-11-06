package com.theoreticalsciences.logistics;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 1.2.0: the Zeno Anchor - the quantum Zeno effect, put to work: a system watched closely enough cannot change, and a
 * chunk watched by the anchor cannot unload. While it has power it keeps the 3 x 3 chunks round it loaded and ticking
 * (machines run, transceivers send, crops grow), whether or not anyone is near, and it holds them across restarts.
 * It draws {@link #COST} FE a tick; out of power, it lets go. Breaking it lets go at once.
 */
public class ZenoAnchorBlockEntity extends BlockEntity {
    public static final int COST = 64, RADIUS = 1, CAPACITY = 1_000_000;
    /** Our chunk tickets: on load, any that no longer belong to a working anchor are dropped. */
    public static final TicketController TICKETS = new TicketController(TheoreticalSciences.id("zeno_anchor"), (level, helper) ->
            helper.getBlockTickets().forEach((pos, tickets) -> {
                if (!(level.getBlockEntity(pos) instanceof ZenoAnchorBlockEntity)) helper.removeAllTickets(pos);
            }));

    public static void registerTickets(RegisterTicketControllersEvent e) {
        e.register(TICKETS);
    }

    /** Takes energy in from any side; only the anchor itself spends it. */
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
    private boolean forced;

    public ZenoAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.ZENO_ANCHOR.get(), pos, state);
    }

    public boolean holding() {
        return forced;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        boolean powered = energy.spend(COST);
        if (level.getGameTime() % 40 == 0) setChanged();
        if (powered != forced) {
            set(sl, worldPosition, powered);
            forced = powered;
            sl.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, powered));
            setChanged();
        }
    }

    static void set(ServerLevel l, BlockPos pos, boolean on) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dz = -RADIUS; dz <= RADIUS; dz++)
                TICKETS.forceChunk(l, pos, cx + dx, cz + dz, on, true);
    }

    static void release(ServerLevel l, BlockPos pos) {
        set(l, pos, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Energy", energy.serializeNBT(reg));
        tag.putBoolean("Forced", forced);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Energy")) energy.deserializeNBT(reg, tag.get("Energy"));
        forced = tag.getBoolean("Forced");
    }
}
