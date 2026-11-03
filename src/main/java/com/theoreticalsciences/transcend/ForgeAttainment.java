package com.theoreticalsciences.transcend;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * 1.3.6: what each player has forged by hand in the reborn Core's Forge (the items, by id). The Eschaton Foundry makes
 * an item only for an owner who has forged it at least once: the Forge teaches the recipe, the Foundry repeats it.
 * Kept per world.
 */
public final class ForgeAttainment extends SavedData {
    private final Map<UUID, Set<String>> forged = new HashMap<>();

    private static ForgeAttainment get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ForgeAttainment::new, ForgeAttainment::load, null),
                "theoreticalsciences_forge_attainment");
    }

    /** A Forge recipe was just made by this player: remember its result. */
    public static void record(ServerPlayer sp, String recipeId) {
        CoreForge.Recipe r = CoreForge.byId(recipeId);
        if (r == null) return;
        String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.out().get()).toString();
        ForgeAttainment d = get(sp.server);
        if (d.forged.computeIfAbsent(sp.getUUID(), k -> new HashSet<>()).add(item)) d.setDirty();
    }

    /** Whether this player has ever forged this item by hand. */
    public static boolean has(MinecraftServer s, UUID player, String itemId) {
        if (player == null) return false;
        Set<String> set = get(s).forged.get(player);
        return set != null && set.contains(itemId);
    }

    private static ForgeAttainment load(CompoundTag t, HolderLookup.Provider regs) {
        ForgeAttainment d = new ForgeAttainment();
        CompoundTag p = t.getCompound("Players");
        for (String k : p.getAllKeys()) {
            try {
                Set<String> set = new HashSet<>();
                for (Tag e : p.getList(k, Tag.TAG_STRING)) set.add(e.getAsString());
                d.forged.put(UUID.fromString(k), set);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider regs) {
        CompoundTag p = new CompoundTag();
        forged.forEach((u, set) -> {
            ListTag l = new ListTag();
            for (String s : set) l.add(StringTag.valueOf(s));
            p.put(u.toString(), l);
        });
        t.put("Players", p);
        return t;
    }
}
