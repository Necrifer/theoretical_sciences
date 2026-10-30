package com.theoreticalsciences.research;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** World-level storage of every player's research, so machines can check an owner even while they are offline. */
public class ResearchSavedData extends SavedData {
    public static final String NAME = "theoreticalsciences_research";

    public static final class Entry {
        public final Set<ResourceLocation> unlocked = new LinkedHashSet<>();
        public final Set<String> observed = new HashSet<>();
        /** Research symbols this player has learned (round 13). */
        public final Set<String> glyphs = new HashSet<>();
        public int insight;
        /** The entry being studied right now (null = none), when it was started and when it will be finished (overworld game time). */
        public ResourceLocation active;
        /** Puzzle progress: research -> letters (d = derived, s = solved, v/m/f/e = hints bought). */
        public final Map<ResourceLocation, String> puzzle = new HashMap<>();
        public long activeStart, activeEnd;
    }

    private final Map<UUID, Entry> players = new HashMap<>();

    public static ResearchSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResearchSavedData::new, ResearchSavedData::load, null), NAME);
    }

    public Entry of(UUID id) {
        return players.computeIfAbsent(id, k -> new Entry());
    }

    public Entry peek(UUID id) {
        return players.get(id);
    }

    private static ResearchSavedData load(CompoundTag tag, HolderLookup.Provider regs) {
        ResearchSavedData d = new ResearchSavedData();
        CompoundTag ps = tag.getCompound("Players");
        for (String key : ps.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            CompoundTag p = ps.getCompound(key);
            Entry e = d.of(id);
            e.insight = p.getInt("Insight");
            for (Tag t : p.getList("Unlocked", Tag.TAG_STRING)) {
                ResourceLocation r = ResourceLocation.tryParse(t.getAsString());
                if (r != null) e.unlocked.add(r);
            }
            for (Tag t : p.getList("Observed", Tag.TAG_STRING)) e.observed.add(t.getAsString());
            for (Tag t : p.getList("Glyphs", Tag.TAG_STRING)) e.glyphs.add(t.getAsString());
            CompoundTag pz = p.getCompound("Puzzle");
            for (String k : pz.getAllKeys()) {
                ResourceLocation r = ResourceLocation.tryParse(k);
                if (r != null) e.puzzle.put(r, pz.getString(k));
            }
            if (p.contains("Active")) {
                e.active = ResourceLocation.tryParse(p.getString("Active"));
                e.activeStart = p.getLong("ActiveStart");
                e.activeEnd = p.getLong("ActiveEnd");
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        CompoundTag ps = new CompoundTag();
        players.forEach((id, e) -> {
            CompoundTag p = new CompoundTag();
            p.putInt("Insight", e.insight);
            ListTag u = new ListTag();
            e.unlocked.forEach(r -> u.add(StringTag.valueOf(r.toString())));
            p.put("Unlocked", u);
            ListTag o = new ListTag();
            e.observed.forEach(s -> o.add(StringTag.valueOf(s)));
            p.put("Observed", o);
            ListTag gl = new ListTag();
            e.glyphs.forEach(s -> gl.add(StringTag.valueOf(s)));
            p.put("Glyphs", gl);
            CompoundTag pz = new CompoundTag();
            e.puzzle.forEach((k, v) -> pz.putString(k.toString(), v));
            p.put("Puzzle", pz);
            if (e.active != null) {
                p.putString("Active", e.active.toString());
                p.putLong("ActiveStart", e.activeStart);
                p.putLong("ActiveEnd", e.activeEnd);
            }
            ps.put(id.toString(), p);
        });
        tag.put("Players", ps);
        return tag;
    }
}
