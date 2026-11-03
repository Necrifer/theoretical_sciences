package com.theoreticalsciences.compat.ae2;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 1.1.0: a player's tool reaching into an ME network it was bound to (the Structure Assembler), the way a wireless
 * terminal does: through any block of the network, with the player as the one asking (so AE2's own security and
 * power apply). Every AE2 type stays in here; MEBridge only calls in when AE2 is installed.
 */
public final class Ae2Remote {
    /** The online network the block at {@code pos} belongs to, or null. */
    @Nullable
    public static Object grid(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return null;
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
        if (host == null) return null;
        IGridNode node = host.getGridNode(null);
        if (node == null)
            for (Direction d : Direction.values()) {
                node = host.getGridNode(d);
                if (node != null) break;
            }
        if (node == null || !node.isActive()) return null;
        return node.getGrid();
    }

    public static long count(Object grid, Item item) {
        if (!(grid instanceof IGrid g)) return 0;
        return g.getStorageService().getCachedInventory().get(AEItemKey.of(item));
    }

    /** Takes {@code n} of the item, all or nothing; returns whether it did. */
    public static boolean take(Object grid, Item item, int n, Player player) {
        if (!(grid instanceof IGrid g)) return false;
        AEItemKey key = AEItemKey.of(item);
        IActionSource src = IActionSource.ofPlayer(player);
        var inv = g.getStorageService().getInventory();
        var energy = g.getEnergyService();
        if (StorageHelper.poweredExtraction(energy, inv, key, n, src, appeng.api.config.Actionable.SIMULATE) < n) return false;
        return StorageHelper.poweredExtraction(energy, inv, key, n, src, appeng.api.config.Actionable.MODULATE) >= n;
    }

    /** Stores what it can of the stack; returns how many went in. */
    public static long put(Object grid, ItemStack s, Player player) {
        if (!(grid instanceof IGrid g) || s.isEmpty()) return 0;
        AEItemKey key = AEItemKey.of(s);
        if (key == null) return 0;
        return StorageHelper.poweredInsert(g.getEnergyService(), g.getStorageService().getInventory(), key, s.getCount(), IActionSource.ofPlayer(player));
    }

    private Ae2Remote() {}
}
