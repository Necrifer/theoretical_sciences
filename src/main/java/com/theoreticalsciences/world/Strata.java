package com.theoreticalsciences.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 1.2.0: the Platonic Realm in layers, one for each part of mathematics, from the bottom up (Y −768 to 1536), with
 * a wide gap of empty air between each and the next, so the Platonic Compass is the way between them (and the widest
 * gap of all under the Infinite, which only the Cantor Key opens):
 * <pre>
 *   1280 .. 1536   the Infinite      the Transfinite Hierarchy's strata (set theory), under the Hierarchy's laws
 *    860 .. 1280   the Continuum     analysis: islands that repeat at every scale (Y 880 to 1040), Fourier harps
 *    520 .. 860    Curved Space      topology and non-Euclidean geometry (Y 560 to 720): Möbius arches, hyperbolic floors
 *    300 .. 520    the Elements      Euclid: marble islands (Y 320 to 400), compass towers (the safe layer)
 *      0 .. 300    the Number Line   the Realm's own islands (Y 10 to 194, as in 1.1)
 *   −768 .. 0      the Axioms        logic: a floor of slate (Y −720 to −650), Liar's Vaults and proof stairs
 * </pre>
 * Each layer runs up to the next, so its gap of air above it counts as its own.
 * Everything that used to ask "is this the Transfinite Hierarchy?" asks here.
 */
public final class Strata {
    public static final int AXIOMS = -768, NUMBER_LINE = 0, ELEMENTS = 300, CURVED = 520, CONTINUUM = 860, TOP = 1536;
    /** Where the Infinite (the Transfinite Hierarchy's strata) begins. */
    public static final int Y = 1280;

    public enum Layer {
        AXIOMS, NUMBER_LINE, ELEMENTS, CURVED_SPACE, CONTINUUM, INFINITE;

        /** {lowest, highest, preferred} height to arrive at in this layer. */
        public int[] band() {
            return switch (this) {
                case AXIOMS -> new int[]{Strata.AXIOMS + 4, Strata.AXIOMS + 200, -680};
                case NUMBER_LINE -> new int[]{1, 200, 80};
                case ELEMENTS -> new int[]{Strata.ELEMENTS + 4, Strata.CURVED - 4, 350};
                case CURVED_SPACE -> new int[]{Strata.CURVED + 4, Strata.CONTINUUM - 4, 610};
                case CONTINUUM -> new int[]{Strata.CONTINUUM + 4, Strata.Y - 4, 930};
                case INFINITE -> new int[]{Strata.Y + 10, Strata.TOP - 8, Strata.Y + 48};
            };
        }
    }

    private Strata() {}

    /**
     * The middle of the chunk a feature is placed in. A feature may only write to its own chunk and the ring around it,
     * so the Realm's larger landmarks are built round the middle (reaching up to 24 blocks either way).
     */
    public static net.minecraft.core.BlockPos chunkCentre(net.minecraft.core.BlockPos p) {
        return new net.minecraft.core.BlockPos((p.getX() & ~15) + 8, p.getY(), (p.getZ() & ~15) + 8);
    }

    public static Layer layer(double y) {
        if (y >= Y) return Layer.INFINITE;
        if (y >= CONTINUUM) return Layer.CONTINUUM;
        if (y >= CURVED) return Layer.CURVED_SPACE;
        if (y >= ELEMENTS) return Layer.ELEMENTS;
        if (y >= NUMBER_LINE) return Layer.NUMBER_LINE;
        return Layer.AXIOMS;
    }

    /** The layer a place in the Realm is in (null anywhere else). */
    public static Layer layer(Level level, double y) {
        return level.dimension() == QHDimensions.PLATONIC ? layer(y) : null;
    }

    /** The dimension whose rules apply at this height: the Hierarchy in the Realm's upper strata, otherwise the dimension. */
    public static ResourceKey<Level> effective(ResourceKey<Level> dim, double y) {
        return dim == QHDimensions.PLATONIC && y >= Y ? QHDimensions.TRANSFINITE : dim;
    }

    /** In the Elements: the safe layer (no hunters spawn there). */
    public static boolean elements(Level level, double y) {
        return layer(level, y) == Layer.ELEMENTS;
    }

    public static boolean upper(Level level, double y) {
        return level.dimension() == QHDimensions.PLATONIC && y >= Y;
    }
}
