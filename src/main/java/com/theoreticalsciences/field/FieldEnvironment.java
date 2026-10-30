package com.theoreticalsciences.field;

import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The physical conditions at a place: gravity (g), number of spatial dimensions, how fast time runs, and the state of
 * the vacuum. Every dimension has its own nature (the Bulk is five-dimensional, inside a black hole gravity is
 * crushing and time nearly stops, the Quantum Foam's vacuum is false...). Powered Field Projectors override one
 * property each within 16 blocks.
 */
public final class FieldEnvironment {
    public record State(double gravity, int dimensions, double time, int vacuum) {
        public static final State NORMAL = new State(1, 3, 1, 0);
    }

    public enum Kind { GRAVITY, DIMENSION, CHRONAL, VACUUM }

    public static final int RADIUS = 16;
    /** Projector settings: value and FE per tick, by level. */
    public static final double[][] VALUES = {{0.01, 10, 100, 1000}, {4, 5, 7, 11}, {0.01, 0.1, 10, 100}, {1, 2, 3}};
    public static final long[][] COST = {{1_000_000, 1_000_000, 16_000_000, 256_000_000}, {4_000_000, 16_000_000, 64_000_000, 512_000_000},
            {64_000_000, 4_000_000, 4_000_000, 64_000_000}, {1_000_000, 32_000_000, 512_000_000}};

    /** Powered projectors, by dimension. Maintained by the projectors themselves. */
    static final Map<ResourceKey<Level>, Map<BlockPos, FieldProjectorBlockEntity>> ACTIVE = new ConcurrentHashMap<>();

    static void add(FieldProjectorBlockEntity be) {
        ACTIVE.computeIfAbsent(be.getLevel().dimension(), k -> new ConcurrentHashMap<>()).put(be.getBlockPos(), be);
    }

    static void remove(FieldProjectorBlockEntity be) {
        if (be.getLevel() == null) return;
        var m = ACTIVE.get(be.getLevel().dimension());
        if (m != null) m.remove(be.getBlockPos());
    }

    public static State natural(ResourceKey<Level> d) {
        if (d == QHDimensions.BULK) return new State(0.5, 5, 1, 0);
        if (d == QHDimensions.HYPERREAL) return new State(1, 11, 1, 0);
        if (d == QHDimensions.FOAM) return new State(1, 3, 1, 2);
        if (d == QHDimensions.TRANSFINITE) return new State(1, 7, 1, 0);
        if (d == QHDimensions.BLACK_HOLE) return new State(1000, 3, 0.01, 0);
        if (d == QHDimensions.WHITE_HOLE) return new State(0.01, 3, 100, 0);
        if (d == QHDimensions.TRANSCENDENTAL) return new State(1, 11, 1, 3);
        return State.NORMAL;
    }

    /** Only what projectors impose (null = nothing projected here). */
    public static State projected(Level level, BlockPos pos) {
        var m = ACTIVE.get(level.dimension());
        if (m == null || m.isEmpty()) return null;
        double g = Double.NaN, t = Double.NaN;
        int dims = -1, vac = -1;
        for (FieldProjectorBlockEntity be : m.values()) {
            if (!be.powered() || be.isRemoved() || be.getLevel() != level) continue;
            BlockPos p = be.getBlockPos();
            if (Math.abs(p.getX() - pos.getX()) > RADIUS || Math.abs(p.getY() - pos.getY()) > RADIUS || Math.abs(p.getZ() - pos.getZ()) > RADIUS) continue;
            double v = be.value();
            switch (be.kind()) {
                case GRAVITY -> g = Double.isNaN(g) || extreme(v) > extreme(g) ? v : g;
                case CHRONAL -> t = Double.isNaN(t) || extreme(v) > extreme(t) ? v : t;
                case DIMENSION -> dims = Math.max(dims, (int) v);
                case VACUUM -> vac = Math.max(vac, (int) v);
            }
        }
        if (Double.isNaN(g) && Double.isNaN(t) && dims < 0 && vac < 0) return null;
        return new State(g, dims, t, vac);
    }

    static double extreme(double v) {
        return Math.abs(Math.log(v));
    }

    public static State at(Level level, BlockPos pos) {
        State n = natural(level.dimension()), p = projected(level, pos);
        if (p == null) return n;
        return new State(Double.isNaN(p.gravity()) ? n.gravity() : p.gravity(), p.dimensions() < 0 ? n.dimensions() : Math.max(n.dimensions(), p.dimensions()),
                Double.isNaN(p.time()) ? n.time() : p.time(), p.vacuum() < 0 ? n.vacuum() : Math.max(n.vacuum(), p.vacuum()));
    }

    private FieldEnvironment() {}
}
