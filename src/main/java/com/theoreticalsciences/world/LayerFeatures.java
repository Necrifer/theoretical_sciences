package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.zones.StructureZones;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.2.0: the landmarks of the Platonic Realm's new layers, built only from the mod's own blocks.
 * <ul>
 *   <li>The Axioms: the <b>Liar's Vault</b> (a sealed hall floored in true and false, light and dark, two doors and only
 *   one of them real, a cache inside) and the <b>Proof Stairs</b> (a spiral where every step stands on the one before:
 *   induction; a cache at the top).</li>
 *   <li>Curved Space: the <b>Möbius Arch</b> (a band arching over the ground with a half twist from one foot to the
 *   other; walk through it and you come out mirrored) and the <b>Hyperbolic Floor</b> (a Poincaré disc laid in stone:
 *   rings and sectors that crowd ever smaller toward the rim, as a hyperbolic plane looks squeezed into a circle).</li>
 *   <li>The Continuum: the <b>Fourier Harp</b> (a row of rods whose heights are a square wave built from its first few
 *   harmonics, overshoot at the edges and all (Gibbs); near it you slow, the closer the slower).</li>
 * </ul>
 */
public final class LayerFeatures {
    private LayerFeatures() {}

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    /** The top of an island of `stone` under (x, z) between lo and hi: the highest such block with air above, or Integer.MIN_VALUE. */
    static int surface(WorldGenLevel l, int x, int z, int lo, int hi, String stone) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, hi, z);
        for (int y = hi; y > lo; y--) {
            p.setY(y);
            BlockState s = l.getBlockState(p);
            if (!s.isAir()) return s.is(QHBlocks.get(stone)) && l.getBlockState(p.above()).isAir() ? y : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    abstract static class Base extends Feature<NoneFeatureConfiguration> {
        Base(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        void put(WorldGenLevel l, BlockPos p, BlockState s) {
            setBlock(l, p, s);
        }

        void clear(WorldGenLevel l, BlockPos p) {
            setBlock(l, p, Blocks.AIR.defaultBlockState());
        }
    }

    // ================================================================== the Axioms
    public static class LiarsVault extends Base {
        public LiarsVault(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int g = surface(l, o.getX(), o.getZ(), Strata.AXIOMS + 2, Strata.AXIOMS + 200, "postulate_slate");
            if (g == Integer.MIN_VALUE) return false;
            BlockState slate = b("postulate_slate"), white = b("axiom_stone"), glass = b("golden_ratio_glass");
            int R = 8, H = 10;                                                  // 1.2.0 (taller Realm): a bigger hall
            for (int x = -R; x <= R; x++)
                for (int z = -R; z <= R; z++)
                    for (int y = 0; y <= H; y++) {
                        BlockPos p = o.offset(x, 0, z).atY(g + y);
                        boolean wall = Math.abs(x) == R || Math.abs(z) == R, roof = y == H;
                        if (y == 0) put(l, p, ((x + z) & 1) == 0 ? white : slate);           // true and false, by turns
                        else if (wall || roof) put(l, p, roof && Math.abs(x) <= 1 && Math.abs(z) <= 1 ? glass : slate);
                        else clear(l, p);
                    }
            // two doors facing each other: one open, one a pane of glass that only looks like a way out
            boolean ew = r.nextBoolean();
            for (int y = 1; y <= 2; y++) {
                BlockPos a = ew ? o.offset(R, y, 0).atY(g + y) : o.offset(0, y, R).atY(g + y);
                BlockPos c = ew ? o.offset(-R, y, 0).atY(g + y) : o.offset(0, y, -R).atY(g + y);
                clear(l, a);
                put(l, c, glass);
            }
            // pillars at the corners of the floor, and the cache in the middle
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2)
                    for (int y = 1; y < H; y++) put(l, o.offset(sx * (R - 3), 0, sz * (R - 3)).atY(g + y), (y & 1) == 0 ? white : slate);
            com.theoreticalsciences.dungeon.Caches.place(l, o.atY(g + 1), "chests/liars_vault", r);
            return true;
        }
    }

    public static class ProofStairs extends Base {
        public ProofStairs(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int g = surface(l, o.getX(), o.getZ(), Strata.AXIOMS + 2, Strata.AXIOMS + 200, "postulate_slate");
            if (g == Integer.MIN_VALUE) return false;
            BlockState slate = b("postulate_slate"), white = b("axiom_stone");
            int steps = 32 + r.nextInt(17);                                   // 1.2.0 (taller Realm): a tall spiral
            double turn = r.nextDouble() * Math.PI * 2;
            for (int k = 0; k < steps; k++) {
                // step k stands on step k − 1, one higher and a little further round: P(k) from P(k − 1)
                double a = turn + k * Math.PI / 8;
                int x = (int) Math.round(Math.cos(a) * 5.5), z = (int) Math.round(Math.sin(a) * 5.5);
                for (int dx = 0; dx <= 1; dx++)
                    for (int dz = 0; dz <= 1; dz++) {
                        BlockPos p = o.offset(x + dx, 0, z + dz).atY(g + 1 + k);
                        put(l, p, k % 2 == 0 ? white : slate);
                        for (int y = 1; y <= 3; y++) if (!l.getBlockState(p.above(y)).isAir()) clear(l, p.above(y));
                    }
            }
            for (int y = 1; y <= steps + 1; y++) put(l, o.atY(g + y), slate);                  // the newel: the base case
            put(l, o.atY(g), white);
            com.theoreticalsciences.dungeon.Caches.place(l, o.atY(g + steps + 2), "chests/proof_stairs", r);
            return true;
        }
    }

    // ================================================================== Curved Space
    public static class MobiusArch extends Base {
        public MobiusArch(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int g = surface(l, o.getX(), o.getZ(), Strata.CURVED + 2, Strata.CONTINUUM - 4, "hyperbolic_stone");
            if (g == Integer.MIN_VALUE) return false;
            BlockState stone = b("hyperbolic_stone"), glass = b("golden_ratio_glass");
            double heading = r.nextInt(4) * Math.PI / 4;
            double R = 11;                                                      // 1.2.0 (taller Realm)
            // a band of width 3 along a half circle; across the band, its direction turns through π from one foot to the other
            for (int i = 0; i <= 160; i++) {
                double t = i / 160.0, a = Math.PI * t;
                double cx = Math.cos(a) * R, cy = Math.sin(a) * R;                // along the arch, in its own plane
                double twist = Math.PI * t;                                          // the half twist
                for (double w = -2.5; w <= 2.5; w += 0.5) {
                    // across: a mix of "out of the plane" and "along the radius", turning as it goes
                    double ox = Math.cos(a) * Math.sin(twist) * w, oy = Math.sin(a) * Math.sin(twist) * w, oz = Math.cos(twist) * w;
                    double px = cx + ox, py = cy + oy, pz = oz;
                    int bx = (int) Math.round(px * Math.cos(heading) - pz * Math.sin(heading));
                    int bz = (int) Math.round(px * Math.sin(heading) + pz * Math.cos(heading));
                    put(l, o.offset(bx, 0, bz).atY(g + 1 + (int) Math.round(py)), Math.abs(w) > 2.2 ? glass : stone);
                }
            }
            StructureZones.mark(l, o.atY(g + 2), 5, StructureZones.Mode.TWIST);
            return true;
        }
    }

    public static class HyperbolicFloor extends Base {
        public HyperbolicFloor(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int g = surface(l, o.getX(), o.getZ(), Strata.CURVED + 2, Strata.CONTINUUM - 4, "hyperbolic_stone");
            if (g == Integer.MIN_VALUE) return false;
            BlockState stone = b("hyperbolic_stone"), dark = b("postulate_slate"), glass = b("golden_ratio_glass");
            int R = 19;                                                          // 1.2.0 (taller Realm)
            for (int x = -R; x <= R; x++)
                for (int z = -R; z <= R; z++) {
                    double rr = Math.hypot(x, z) / (R + 0.5);
                    if (rr >= 1) continue;
                    // hyperbolic distance from the centre of the Poincaré disc: 2·artanh(r)
                    double hd = 2 * 0.5 * Math.log((1 + rr) / (1 - rr));
                    int ring = (int) Math.floor(hd / 0.9);
                    int sectors = 7 << Math.min(4, ring);                          // seven round the middle, doubling outward
                    double a = (Math.atan2(z, x) + Math.PI) / (Math.PI * 2);
                    int sector = (int) Math.floor(a * sectors);
                    boolean edge = rr > 0.93;
                    BlockPos p = o.offset(x, 0, z).atY(g);
                    put(l, p, edge ? glass : ((ring + sector) & 1) == 0 ? stone : dark);
                    for (int y = 1; y <= 2; y++) if (!l.getBlockState(p.above(y)).isAir()) clear(l, p.above(y));
                }
            return true;
        }
    }

    // ================================================================== the Continuum
    public static class FourierHarp extends Base {
        public FourierHarp(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = Strata.chunkCentre(ctx.origin());          // 1.2.0: room to reach ±24 blocks
            int g = surface(l, o.getX(), o.getZ(), Strata.CONTINUUM + 2, Strata.Y - 4, "dense_stone");
            if (g == Integer.MIN_VALUE) return false;
            BlockState stone = b("dense_stone"), glass = b("golden_ratio_glass");
            boolean alongX = r.nextBoolean();
            int n = 33, harmonics = 2 + r.nextInt(3);                          // 1.2.0 (taller Realm): a bigger harp
            for (int i = 0; i < n; i++) {
                double x = (i + 0.5) / n * 2 * Math.PI;
                double s = 0;
                for (int k = 0; k < harmonics; k++) {
                    int m = 2 * k + 1;
                    s += 4 / Math.PI * Math.sin(m * x) / m;                       // a square wave's odd harmonics
                }
                int h = 12 + (int) Math.round(s * 8);
                int off = i - n / 2;
                BlockPos base = (alongX ? o.offset(off, 0, 0) : o.offset(0, 0, off)).atY(g);
                put(l, base, stone);
                for (int y = 1; y <= h; y++) put(l, base.above(y), y == h ? stone : glass);
            }
            StructureZones.mark(l, o.atY(g + 8), 20, StructureZones.Mode.LENS);
            return true;
        }
    }
}
