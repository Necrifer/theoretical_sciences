package com.theoreticalsciences.orbit;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 1.3.0: the world's one Orbital Station, kept with the Overworld's data: how many Station Modules have been flown up
 * (by any player's Rocket Assembly) and whether the station has been put together in orbit yet.
 */
public final class OrbitData extends SavedData {
    /** Modules the station needs before a crew can go up: the core, a habitat, power and a docking port. */
    public static final int REQUIRED = 4;
    private static final String NAME = "theoreticalsciences_orbital_station";

    int modules;
    boolean built;
    /** 1.12.0: the routes charted by a Return Beacon (destination ids): the Mission Planner sends you there at no cost. */
    final java.util.Set<String> charted = new java.util.HashSet<>();
    /** A world from before 1.12.0, whose station already flew everywhere free: every route counts as charted. */
    boolean legacy;
    /** 1.13.0: satellites in orbit, by item id path (weather_satellite...), and the debris left there (see SpaceModules). */
    final java.util.Map<String, Integer> satellites = new java.util.TreeMap<>();
    double debris;
    /** 1.13.0: where the station modules stand, by kind and dimension ("habitat_ring_module|theoreticalsciences:orbital_station"). */
    final java.util.Map<String, java.util.Set<Long>> placed = new java.util.HashMap<>();

    static OrbitData load(CompoundTag t, HolderLookup.Provider r) {
        OrbitData d = new OrbitData();
        d.modules = t.getInt("Modules");
        d.built = t.getBoolean("Built");
        if (t.contains("Charted")) {
            for (net.minecraft.nbt.Tag e : t.getList("Charted", net.minecraft.nbt.Tag.TAG_STRING)) d.charted.add(e.getAsString());
            d.legacy = t.getBoolean("Legacy");
        } else d.legacy = d.built;                          // built before 1.12.0: keep what was free free
        CompoundTag sats = t.getCompound("Satellites");
        for (String k : sats.getAllKeys()) d.satellites.put(k, sats.getInt(k));
        d.debris = t.getDouble("Debris");
        CompoundTag pl = t.getCompound("Placed");
        for (String k : pl.getAllKeys()) {
            java.util.Set<Long> set = new java.util.HashSet<>();
            for (long v : pl.getLongArray(k)) set.add(v);
            d.placed.put(k, set);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putInt("Modules", modules);
        t.putBoolean("Built", built);
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (String c : charted) l.add(net.minecraft.nbt.StringTag.valueOf(c));
        t.put("Charted", l);
        t.putBoolean("Legacy", legacy);
        CompoundTag sats = new CompoundTag();
        satellites.forEach(sats::putInt);
        t.put("Satellites", sats);
        t.putDouble("Debris", debris);
        CompoundTag pl = new CompoundTag();
        placed.forEach((k, v) -> pl.putLongArray(k, v.stream().mapToLong(Long::longValue).toArray()));
        t.put("Placed", pl);
        return t;
    }

    public static OrbitData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(OrbitData::new, OrbitData::load, null), NAME);
    }

    public int modules() {
        return modules;
    }

    public boolean ready() {
        return modules >= REQUIRED;
    }

    public boolean charted(net.minecraft.resources.ResourceLocation dest) {
        return legacy || charted.contains(dest.toString());
    }

    /** Charts a route; true if it was new. */
    public boolean chart(net.minecraft.resources.ResourceLocation dest) {
        boolean added = charted.add(dest.toString());
        if (added) setDirty();
        return added;
    }

    /** One more module docked; returns the new count. */
    public int addModule() {
        modules++;
        setDirty();
        return modules;
    }

    // ------------------------------------------------------------------ 1.13.0
    public int satellites(String kind) {
        return satellites.getOrDefault(kind, 0);
    }

    public int addSatellite(String kind, int n) {
        int v = Math.max(0, satellites(kind) + n);
        if (v == 0) satellites.remove(kind);
        else satellites.put(kind, v);
        setDirty();
        return v;
    }

    public double debris() {
        return debris;
    }

    public void debris(double v) {
        debris = Math.max(0, v);
        setDirty();
    }

    java.util.Set<Long> placed(String kind, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
        return placed.getOrDefault(kind + "|" + dim.location(), java.util.Set.of());
    }

    void place(String kind, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, long pos, boolean add) {
        java.util.Set<Long> set = placed.computeIfAbsent(kind + "|" + dim.location(), k -> new java.util.HashSet<>());
        if (add ? set.add(pos) : set.remove(pos)) setDirty();
    }
}
