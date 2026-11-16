package com.theoreticalsciences.engineering;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.4.0: a derelict orbital ring in low Earth orbit: an arc of an old spinning habitat ring, its hull torn open at both
 * ends, a spoke of tether trailing from it, debris drifting round it. Rust Mites have the run of it, and the crew's cache
 * is still aboard. It is far enough from the station (more than 96 blocks) never to hit it.
 */
public final class EngineeringFeatures {
    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    public static class DerelictRing extends Feature<NoneFeatureConfiguration> {
        public DerelictRing(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if ((long) o.getX() * o.getX() + (long) o.getZ() * o.getZ() < 96L * 96L) return false;
            BlockPos c = new BlockPos(o.getX(), com.theoreticalsciences.orbit.OrbitBuilder.Y - 20 + r.nextInt(41), o.getZ());
            BlockState hull = b("habitat_hull"), win = b("habitat_window"), ribbon = b("tether_ribbon"), bearing = b("spin_bearing");
            int R = 13;
            double a0 = r.nextDouble() * Math.PI * 2, span = Math.PI * (0.5 + r.nextDouble() * 0.35);
            double tilt = (r.nextDouble() - 0.5) * 0.5;
            int steps = (int) (span * R * 1.6);
            BlockPos cachePos = null;
            for (int i = 0; i <= steps; i++) {
                double a = a0 + span * i / steps;
                double cx = Math.cos(a) * R, cz = Math.sin(a) * R, cy = Math.sin(a) * R * tilt;
                boolean torn = i < 3 || i > steps - 3;                      // the torn ends: ragged
                // a square tube, three by three, hollow
                for (int u = -1; u <= 1; u++)
                    for (int v = -1; v <= 1; v++) {
                        double rx = Math.cos(a) * u, rz = Math.sin(a) * u;
                        BlockPos p = c.offset((int) Math.round(cx + rx), (int) Math.round(cy + v), (int) Math.round(cz + rz));
                        boolean shell = Math.abs(u) == 1 || Math.abs(v) == 1;
                        if (!shell) {
                            setBlock(l, p, Blocks.AIR.defaultBlockState());
                            if (cachePos == null && i == steps / 2) cachePos = p;
                            continue;
                        }
                        if (torn && r.nextFloat() < 0.5F) continue;
                        // windows on the outward face, as the ring's floor faced out under spin gravity
                        setBlock(l, p, u == 1 && v == 0 && i % 3 != 0 ? win : hull);
                    }
                if (i % 9 == 4) setBlock(l, c.offset((int) Math.round(cx * 0.88), (int) Math.round(cy), (int) Math.round(cz * 0.88)), bearing);
            }
            // a spoke of tether towards where the hub was
            double am = a0 + span / 2;
            for (int k = 2; k < R - 2; k++)
                if (r.nextFloat() < 0.85F) setBlock(l, c.offset((int) Math.round(Math.cos(am) * k), (int) Math.round(Math.sin(am) * k * tilt), (int) Math.round(Math.sin(am) * k)), ribbon);
            // drifting debris
            for (int i = 0; i < 10; i++)
                setBlock(l, c.offset(r.nextInt(25) - 12, r.nextInt(9) - 4, r.nextInt(25) - 12), r.nextInt(4) == 0 ? ribbon : hull);
            if (cachePos != null) {
                com.theoreticalsciences.dungeon.Caches.place(l, cachePos, "chests/derelict_ring", r);
                for (int i = 0; i < 2 + r.nextInt(3); i++) {
                    RustMite m = EngineeringRegistry.RUST_MITE.get().create(l.getLevel());
                    if (m == null) continue;
                    m.moveTo(cachePos.getX() + 0.5, cachePos.getY(), cachePos.getZ() + 0.5, r.nextFloat() * 360F, 0F);
                    m.finalizeSpawn(l, l.getCurrentDifficultyAt(cachePos), MobSpawnType.STRUCTURE, null);
                    m.setPersistenceRequired();
                    l.addFreshEntityWithPassengers(m);
                }
            }
            return true;
        }
    }

    private EngineeringFeatures() {}
}
