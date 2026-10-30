package com.theoreticalsciences.world;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Our dimensions keep their own ecology: no vanilla (or other mods') creatures spawn there on their own - not from
 * biome spawn lists, structures, patrols, reinforcements or jockeys. Spawn eggs, spawners, commands, breeding and
 * conversions still work, so players and pack makers can bring anything they like.
 */
public final class SpawnRules {
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        Level level = e.getEntity().level();
        if (!level.dimension().location().getNamespace().equals(TheoreticalSciences.MODID)) return;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType()).getNamespace().equals(TheoreticalSciences.MODID)) return;
        MobSpawnType t = e.getSpawnType();
        if (t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.STRUCTURE || t == MobSpawnType.PATROL
                || t == MobSpawnType.REINFORCEMENT || t == MobSpawnType.EVENT) {
            e.setSpawnCancelled(true);
            e.setCanceled(true);
        }
    }

    /**
     * Vanilla jockeys (a skeleton riding a spider-type mob, a baby zombie-type mob on a chicken) are stripped off our own
     * creatures as they join our dimensions - the rider or mount is removed outright, never left half-spawned.
     */
    public static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() || !(e.getEntity() instanceof net.minecraft.world.entity.Mob m)) return;
        if (!e.getLevel().dimension().location().getNamespace().equals(TheoreticalSciences.MODID)) return;
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace().equals(TheoreticalSciences.MODID)) return;
        for (net.minecraft.world.entity.Entity p : java.util.List.copyOf(m.getPassengers()))
            if (!BuiltInRegistries.ENTITY_TYPE.getKey(p.getType()).getNamespace().equals(TheoreticalSciences.MODID)) {
                p.stopRiding();
                p.discard();
            }
        net.minecraft.world.entity.Entity v = m.getVehicle();
        if (v != null && !BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()).getNamespace().equals(TheoreticalSciences.MODID)) {
            m.stopRiding();
            v.discard();
        }
    }

    private SpawnRules() {}
}
