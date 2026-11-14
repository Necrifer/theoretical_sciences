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

    static OrbitData load(CompoundTag t, HolderLookup.Provider r) {
        OrbitData d = new OrbitData();
        d.modules = t.getInt("Modules");
        d.built = t.getBoolean("Built");
        if (t.contains("Charted")) {
            for (net.minecraft.nbt.Tag e : t.getList("Charted", net.minecraft.nbt.Tag.TAG_STRING)) d.charted.add(e.getAsString());
            d.legacy = t.getBoolean("Legacy");
        } else d.legacy = d.built;                          // built before 1.12.0: keep what was free free
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
}
