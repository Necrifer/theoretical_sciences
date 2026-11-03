package com.theoreticalsciences.compat;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;

/**
 * The only place that touches the AE2 integration classes, and only when AE2 is installed (every AE2 type stays inside
 * compat.ae2, which is never loaded otherwise).
 */
public final class MEBridge {
    private static Boolean present;

    public static boolean present() {
        if (present == null) present = ModList.get() != null && ModList.get().isLoaded("ae2");
        return present;
    }

    /** A network node for this Interface Port (an Ae2Link), or null without AE2. */
    @Nullable
    public static Object attach(com.theoreticalsciences.machine.PortBlockEntity port, @Nullable CompoundTag saved) {
        return present() ? com.theoreticalsciences.compat.ae2.Ae2Link.create(port, saved) : null;
    }

    public static void detach(@Nullable Object link) {
        if (link != null && present()) com.theoreticalsciences.compat.ae2.Ae2Link.destroy(link);
    }

    public static void save(@Nullable Object link, CompoundTag into) {
        if (link != null && present()) com.theoreticalsciences.compat.ae2.Ae2Link.save(link, into);
    }

    public static void registerCaps(RegisterCapabilitiesEvent e) {
        if (present()) com.theoreticalsciences.compat.ae2.Ae2Link.registerCaps(e);
    }

    // ------------------------------------------------------------------ 1.1.0: a bound tool reaching into a network
    /** The online ME network the block at this position belongs to (an opaque handle), or null. */
    @Nullable
    public static Object grid(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        return present() ? com.theoreticalsciences.compat.ae2.Ae2Remote.grid(level, pos) : null;
    }

    public static long count(@Nullable Object grid, net.minecraft.world.item.Item item) {
        return grid != null && present() ? com.theoreticalsciences.compat.ae2.Ae2Remote.count(grid, item) : 0;
    }

    public static boolean take(@Nullable Object grid, net.minecraft.world.item.Item item, int n, net.minecraft.world.entity.player.Player player) {
        return grid != null && present() && com.theoreticalsciences.compat.ae2.Ae2Remote.take(grid, item, n, player);
    }

    public static long put(@Nullable Object grid, net.minecraft.world.item.ItemStack s, net.minecraft.world.entity.player.Player player) {
        return grid != null && present() ? com.theoreticalsciences.compat.ae2.Ae2Remote.put(grid, s, player) : 0;
    }

    private MEBridge() {}
}
