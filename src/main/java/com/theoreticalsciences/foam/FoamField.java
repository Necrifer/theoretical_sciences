package com.theoreticalsciences.foam;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Quantum Foam is not the same everywhere. Each kind of virtual particle has a <b>resonance</b> at every point
 * (a number from 0 to 100 that drifts smoothly through space, on a scale of about a hundred blocks).
 * <ul>
 *   <li>Where a particle's field is strongest, its ephemeral ore bubbles up out of the foam.</li>
 *   <li>A Vacuum Fluctuation Harvester materialises whichever particle's resonance its dial is tuned closest to,
 *       so the right setting depends on where the harvester stands.</li>
 * </ul>
 */
public final class FoamField {
    public static final String[] MATERIALS = {"virtual_quark", "virtual_graviton", "virtual_photon", "virtual_monopole"};
    public static final String[] NAMES = {"Quark", "Graviton", "Photon", "Monopole"};

    private static long cachedSeed = Long.MIN_VALUE;
    private static SimplexNoise[] noises;

    private static SimplexNoise[] noises(long seed) {
        if (seed != cachedSeed || noises == null) {
            noises = new SimplexNoise[MATERIALS.length * 2];
            for (int i = 0; i < noises.length; i++) noises[i] = new SimplexNoise(new LegacyRandomSource(seed * 31 + 7919L * (i + 1)));
            cachedSeed = seed;
        }
        return noises;
    }

    /** 0..100: where the dial must sit to pull this particle out of the vacuum here. */
    public static double resonance(ServerLevel level, int material, BlockPos p) {
        SimplexNoise n = noises(level.getSeed())[material * 2];
        double v = n.getValue(p.getX() / 96.0, p.getY() / 64.0, p.getZ() / 96.0) * 0.75
                + n.getValue(p.getX() / 31.0 + 100, p.getY() / 23.0, p.getZ() / 31.0) * 0.25;
        return Math.max(0, Math.min(100, 50 + 55 * v));
    }

    /** 0..1: how strongly this particle's field dominates here (decides which ore grows). */
    public static double strength(ServerLevel level, int material, BlockPos p) {
        SimplexNoise n = noises(level.getSeed())[material * 2 + 1];
        return 0.5 + 0.5 * n.getValue(p.getX() / 72.0, p.getY() / 48.0, p.getZ() / 72.0);
    }

    public static int dominant(ServerLevel level, BlockPos p) {
        int best = 0;
        double bs = -1;
        for (int i = 0; i < MATERIALS.length; i++) {
            double s = strength(level, i, p);
            if (s > bs) {
                bs = s;
                best = i;
            }
        }
        return best;
    }

    /** The material whose resonance here is closest to the given dial setting, and how far off the dial is. */
    public static int nearest(ServerLevel level, BlockPos p, float setting, double[] distanceOut) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < MATERIALS.length; i++) {
            double d = Math.abs(resonance(level, i, p) - setting);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        if (distanceOut != null && distanceOut.length > 0) distanceOut[0] = bd;
        return best;
    }

    public static Block ore(int material) {
        return QHBlocks.get(MATERIALS[material] + "_ore");
    }

    public static Item item(int material) {
        return QHItems.get(MATERIALS[material]);
    }

    @SuppressWarnings("unused")
    private static RandomSource unused() {
        return null;
    }

    private FoamField() {}
}
