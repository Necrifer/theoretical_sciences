package com.theoreticalsciences.home;

import com.theoreticalsciences.transcend.CoreForge;
import com.theoreticalsciences.transcend.ForgeAttainment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Each player's Recreating Home checklist: how many of each of the forty-two materials they have given the Core, whether
 * the Core has spoken to them yet, and where they stood when they last went through the portal (so it can bring them back).
 * Kept per world.
 */
public final class HomeProgress extends SavedData {
    private static final class Entry {
        final long[] given = new long[HomeMaterials.IDS.size()];
        boolean greeted, finished, forced;
        String backDim = "";
        long backPos;
    }

    private final Map<UUID, Entry> players = new HashMap<>();

    private static HomeProgress get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(HomeProgress::new, HomeProgress::load, null),
                "theoreticalsciences_recreating_home");
    }

    private Entry of(UUID id) {
        return players.computeIfAbsent(id, k -> new Entry());
    }

    /** The tab appears for a player who has forged every recipe of the Core Forge at least once. */
    public static boolean unlocked(MinecraftServer s, UUID player) {
        Entry had = get(s).players.get(player);
        if (had != null && had.forced) return true;              // (the operator's command, for testing)
        for (CoreForge.Recipe r : CoreForge.RECIPES) {
            ResourceLocation out = BuiltInRegistries.ITEM.getKey(r.out().get());
            if (!ForgeAttainment.has(s, player, out.toString())) return false;
        }
        return true;
    }

    public static long[] given(MinecraftServer s, UUID player) {
        return get(s).of(player).given.clone();
    }

    /** For operators (/theoreticalsciences home ...): show the tab without the forging, fill every line, or start over. */
    public static void command(MinecraftServer s, UUID player, String what) {
        HomeProgress d = get(s);
        Entry e = d.of(player);
        switch (what) {
            case "unlock" -> e.forced = true;
            case "complete" -> {
                e.forced = true;
                java.util.Arrays.fill(e.given, HomeMaterials.TARGET);
            }
            default -> {
                e.forced = false;
                e.greeted = false;
                e.finished = false;
                java.util.Arrays.fill(e.given, 0L);
            }
        }
        d.setDirty();
    }

    /** Every line of the checklist is full. */
    public static boolean complete(MinecraftServer s, UUID player) {
        if (player == null) return false;
        Entry e = get(s).players.get(player);
        if (e == null) return false;
        for (long g : e.given) if (g < HomeMaterials.TARGET) return false;
        return true;
    }

    /** Gives up to {@code count} of material {@code index}; returns how many were taken (what the line still needed). */
    public static long give(MinecraftServer s, UUID player, int index, long count) {
        HomeProgress d = get(s);
        Entry e = d.of(player);
        long take = Math.max(0, Math.min(count, HomeMaterials.TARGET - e.given[index]));
        if (take > 0) {
            e.given[index] += take;
            d.setDirty();
        }
        return take;
    }

    /** Takes from the player's inventory everything the checklist still needs; returns how many items were taken. */
    public static long submitInventory(ServerPlayer sp) {
        long total = 0;
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            int idx = st.isEmpty() ? -1 : HomeMaterials.index(st.getItem());
            if (idx < 0) continue;
            long took = give(sp.server, sp.getUUID(), idx, st.getCount());
            if (took > 0) {
                st.shrink((int) took);
                total += took;
            }
        }
        return total;
    }

    /** True the first time only: the Core speaks once when the tab first appears. */
    public static boolean greetOnce(MinecraftServer s, UUID player) {
        HomeProgress d = get(s);
        Entry e = d.of(player);
        if (e.greeted) return false;
        e.greeted = true;
        d.setDirty();
        return true;
    }

    /** True the first time only: the Core speaks once when the checklist is complete. */
    public static boolean finishOnce(MinecraftServer s, UUID player) {
        HomeProgress d = get(s);
        Entry e = d.of(player);
        if (e.finished) return false;
        e.finished = true;
        d.setDirty();
        return true;
    }

    public static void rememberWayBack(ServerPlayer sp) {
        HomeProgress d = get(sp.server);
        Entry e = d.of(sp.getUUID());
        e.backDim = sp.level().dimension().location().toString();
        e.backPos = sp.blockPosition().asLong();
        d.setDirty();
    }

    /** {dimension id, packed position} of where the player last went in from; dimension "" if never. */
    public static Map.Entry<String, BlockPos> wayBack(MinecraftServer s, UUID player) {
        Entry e = get(s).of(player);
        return Map.entry(e.backDim, BlockPos.of(e.backPos));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        ListTag list = new ListTag();
        players.forEach((id, e) -> {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", id);
            t.putLongArray("Given", e.given);
            t.putBoolean("Greeted", e.greeted);
            t.putBoolean("Finished", e.finished);
            t.putBoolean("Forced", e.forced);
            t.putString("BackDim", e.backDim);
            t.putLong("BackPos", e.backPos);
            list.add(t);
        });
        tag.put("Players", list);
        return tag;
    }

    private static HomeProgress load(CompoundTag tag, HolderLookup.Provider regs) {
        HomeProgress d = new HomeProgress();
        for (Tag raw : tag.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            Entry e = d.of(t.getUUID("Player"));
            long[] g = t.getLongArray("Given");
            System.arraycopy(g, 0, e.given, 0, Math.min(g.length, e.given.length));
            e.greeted = t.getBoolean("Greeted");
            e.finished = t.getBoolean("Finished");
            e.forced = t.getBoolean("Forced");
            e.backDim = t.getString("BackDim");
            e.backPos = t.getLong("BackPos");
        }
        return d;
    }
}
