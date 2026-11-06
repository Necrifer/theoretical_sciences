package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.2.0: the landmarks of the Elements, the Platonic Realm's layer of Euclid's marble (y 300 to 520, islands from y 320). Built only from
 * the mod's own blocks: Euclidean marble, golden ratio glass and Euclidean ore.
 * <ul>
 *   <li><b>Compass Tower</b>: a marble column with a round platform on top. A circle of glass is drawn on it with the
 *   compass, and a hexagon is stepped round that circle with the same opening (its side equals the radius: Euclid,
 *   Book IV, Proposition 15). A straightedge of marble reaches out from it at a constructible angle.</li>
 *   <li><b>Geometer's Ruin</b>: a marble floor inlaid with Euclid's proof of Pythagoras (Book I, Proposition 47): a
 *   right triangle of sides 6, 8 and 10 with a square on each side, 36 + 64 = 100. Broken columns round it, and the
 *   geometer's cache at the right angle.</li>
 * </ul>
 */
public final class ElementsFeatures {
    private ElementsFeatures() {}

    static BlockState marble() {
        return QHBlocks.get("euclidean_marble").defaultBlockState();
    }

    static BlockState glass() {
        return QHBlocks.get("golden_ratio_glass").defaultBlockState();
    }

    /** The top of an Elements island under (x, z): the highest marble block with air above it, or -1. */
    static int surface(WorldGenLevel l, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, Strata.CURVED - 4, z);
        for (int y = Strata.CURVED - 4; y > Strata.ELEMENTS; y--) {
            p.setY(y);
            BlockState s = l.getBlockState(p);
            if (!s.isAir()) return s.is(QHBlocks.get("euclidean_marble")) && l.getBlockState(p.above()).isAir() ? y : -1;
        }
        return -1;
    }

    // ------------------------------------------------------------------ Compass Tower
    public static class CompassTower extends Feature<NoneFeatureConfiguration> {
        public CompassTower(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int ground = surface(l, o.getX(), o.getZ());
            if (ground < 0) return false;
            int h = 14 + r.nextInt(12);                                    // 1.2.0 (taller Realm): taller towers
            int top = Math.min(Strata.CURVED - 6, ground + h);
            BlockState m = marble(), g = glass();
            // the column: a 3 x 3 shaft with a glass core (the compass's pivot, carried down to the ground)
            for (int y = ground - 2; y <= top; y++)
                for (int x = -1; x <= 1; x++)
                    for (int z = -1; z <= 1; z++)
                        setBlock(l, o.offset(x, 0, z).atY(y), x == 0 && z == 0 && y > ground ? g : m);
            // the platform, radius 5, and the circle drawn on it
            int R = 7;
            for (int x = -R - 1; x <= R + 1; x++)
                for (int z = -R - 1; z <= R + 1; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= R + 0.5) setBlock(l, o.offset(x, 0, z).atY(top), m);
                    if (Math.abs(d - R) < 0.5) setBlock(l, o.offset(x, 0, z).atY(top + 1), g);
                }
            // the hexagon: six posts on the circle, one radius apart
            double turn = r.nextInt(12) * Math.PI / 6;
            for (int i = 0; i < 6; i++) {
                double a = turn + i * Math.PI / 3;
                int x = (int) Math.round(Math.cos(a) * R), z = (int) Math.round(Math.sin(a) * R);
                for (int y = 1; y <= 3; y++) setBlock(l, o.offset(x, 0, z).atY(top + y), y == 3 ? g : m);
            }
            setBlock(l, o.atY(top + 1), g);
            // the straightedge: a beam from the platform's rim, at a multiple of 30 degrees (every one of them constructible)
            double a = r.nextInt(12) * Math.PI / 6;
            for (int s = R + 1; s <= 19; s++) {
                int x = (int) Math.round(Math.cos(a) * s), z = (int) Math.round(Math.sin(a) * s);
                setBlock(l, o.offset(x, 0, z).atY(top), m);
            }
            if (r.nextInt(3) == 0) com.theoreticalsciences.dungeon.Caches.place(l, o.atY(top + 1).offset(0, 0, 2), "chests/geometers_ruin", r);
            return true;
        }
    }

    // ------------------------------------------------------------------ Geometer's Ruin
    public static class GeometersRuin extends Feature<NoneFeatureConfiguration> {
        public GeometersRuin(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        static double[][] sc(int s, double[][] v) {
            double[][] o = new double[v.length][];
            for (int i = 0; i < v.length; i++) o[i] = new double[]{v[i][0] * s, v[i][1] * s};
            return o;
        }

        /** Whether p lies in the convex polygon v (counter-clockwise), and how close it is to an edge. */
        static double inside(double px, double pz, double[][] v) {
            double worst = 1e9;
            for (int i = 0; i < v.length; i++) {
                double[] a = v[i], b = v[(i + 1) % v.length];
                double ex = b[0] - a[0], ez = b[1] - a[1], len = Math.hypot(ex, ez);
                double cross = (ex * (pz - a[1]) - ez * (px - a[0])) / len;     // distance to the left of the edge
                worst = Math.min(worst, cross);
            }
            return worst;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int ground = surface(l, o.getX(), o.getZ());
            if (ground < 0) return false;
            // the right angle at (0, 0); legs 8 along x and 6 along z; each square outward from its side
            // 1.2.0 (taller Realm): drawn at twice the size, legs 16 and 12, hypotenuse 20 (the same 3 : 4 : 5)
            final int S = 2;
            double[][] tri = sc(S, new double[][]{{0, 0}, {8, 0}, {0, 6}});
            double[][] sqA = sc(S, new double[][]{{0, -8}, {8, -8}, {8, 0}, {0, 0}});          // on the long leg
            double[][] sqB = sc(S, new double[][]{{-6, 0}, {0, 0}, {0, 6}, {-6, 6}});          // on the short leg
            double[][] sqC = sc(S, new double[][]{{8, 0}, {14, 8}, {6, 14}, {0, 6}});          // on the hypotenuse
            double[][][] shapes = {tri, sqA, sqB, sqC};
            int cx = -4 * S, cz = -3 * S;                                  // so the figure sits round the origin
            BlockState m = marble(), g = glass();
            for (int x = -7 * S; x <= 15 * S; x++)
                for (int z = -9 * S; z <= 15 * S; z++) {
                    double best = -1e9;
                    for (double[][] s : shapes) best = Math.max(best, inside(x, z, s));
                    if (best < -0.6) continue;
                    BlockPos at = o.offset(x + cx, 0, z + cz);
                    boolean edge = false;
                    for (double[][] s : shapes) if (Math.abs(inside(x, z, s)) < 0.6) edge = true;
                    setBlock(l, at.atY(ground), edge ? g : m);
                    for (int y = 1; y <= 4; y++) setBlock(l, at.atY(ground + y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    for (int y = 1; y <= 4; y++) {                          // a footing, so the floor never floats on nothing
                        BlockPos b = at.atY(ground - y);
                        if (l.getBlockState(b).isAir()) setBlock(l, b, m);
                    }
                }
            // broken columns at the corners of the squares
            double[][] corners = sc(S, new double[][]{{0, -8}, {8, -8}, {-6, 6}, {-6, 0}, {14, 8}, {6, 14}});
            for (double[] c : corners) {
                if (r.nextInt(3) == 0) continue;
                int hgt = 2 + r.nextInt(7);
                for (int y = 1; y <= hgt; y++) setBlock(l, o.offset((int) c[0] + cx, 0, (int) c[1] + cz).atY(ground + y), m);
            }
            com.theoreticalsciences.dungeon.Caches.place(l, o.offset(cx + 1, 0, cz + 1).atY(ground + 1), "chests/geometers_ruin", r);
            return true;
        }
    }
}
