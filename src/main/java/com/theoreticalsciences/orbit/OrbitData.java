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

    static OrbitData load(CompoundTag t, HolderLookup.Provider r) {
        OrbitData d = new OrbitData();
        d.modules = t.getInt("Modules");
        d.built = t.getBoolean("Built");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putInt("Modules", modules);
        t.putBoolean("Built", built);
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

    /** One more module docked; returns the new count. */
    public int addModule() {
        modules++;
        setDirty();
        return modules;
    }
}
