package com.theoreticalsciences.endgame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Infinite energy. Every tick it offers Integer.MAX_VALUE FE to each neighbour, and anything may pull as much as it
 * likes from it. Every ten ticks it also tops up every energy-holding item carried by players within 16 blocks.
 */
public class ExNihiloEngineBlockEntity extends BlockEntity {
    /** An endless source: never runs dry, never accepts anything. */
    public static final IEnergyStorage INFINITE = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean simulate) { return 0; }
        @Override public int extractEnergy(int max, boolean simulate) { return Math.max(0, max); }
        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
    private static final double PLAYER_RANGE = 16;

    public ExNihiloEngineBlockEntity(BlockPos pos, BlockState state) {
        super(EndgameRegistry.EX_NIHILO.get(), pos, state);
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!sl.isLoaded(n)) continue;
            IEnergyStorage target = sl.getCapability(Capabilities.EnergyStorage.BLOCK, n, d.getOpposite());
            if (target != null && target != INFINITE && target.canReceive()) target.receiveEnergy(Integer.MAX_VALUE, false);
        }
        if ((sl.getGameTime() + worldPosition.asLong()) % 10 != 0) return;
        List<Player> players = sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(PLAYER_RANGE),
                p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(worldPosition.getCenter()) <= PLAYER_RANGE * PLAYER_RANGE);
        for (Player p : players) {
            var inv = p.getInventory();
            for (List<ItemStack> list : List.of(inv.items, inv.armor, inv.offhand))
                for (ItemStack s : list) charge(s);
        }
    }

    private static void charge(ItemStack s) {
        if (s.isEmpty()) return;
        IEnergyStorage e = s.getCapability(Capabilities.EnergyStorage.ITEM);
        if (e != null && e.canReceive() && e.getEnergyStored() < e.getMaxEnergyStored()) e.receiveEnergy(Integer.MAX_VALUE, false);
    }
}
