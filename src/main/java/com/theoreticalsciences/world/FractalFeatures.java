package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * The Platonic Realm's landmarks are mathematical objects built to their definitions:
 * Menger sponges, Sierpinski tetrahedra, Mandelbulbs, Cantor dust, recursive trees and a Hilbert-curve archive.
 * Each is centred on its chunk so a 32-block object stays inside the area a feature may write to.
 */
public final class FractalFeatures {
    public static final ResourceKey<LootTable> ARCHIVE = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/hilbert_archive"));
    public static final ResourceKey<LootTable> VAULT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/euclids_vault"));

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    /** Chunk centre at a floating height. */
    static BlockPos centre(FeaturePlaceContext<?> ctx, int lo, int hi) {
        BlockPos o = ctx.origin();
        return new BlockPos((o.getX() & ~15) + 8, lo + ctx.random().nextInt(hi - lo + 1), (o.getZ() & ~15) + 8);
    }

    static void put(WorldGenLevel l, BlockPos p, BlockState s) {
        if (l.getBlockState(p).isAir() || l.getBlockState(p).canBeReplaced()) l.setBlock(p, s, 2);
    }

    static void chest(WorldGenLevel l, BlockPos p, ResourceKey<LootTable> loot, RandomSource r) {
        com.theoreticalsciences.dungeon.Caches.place(l, p, loot, r);
    }

    /** Level-3 Menger sponge (27 blocks): a cell is removed when two or more of its base-3 digits at the same place are 1. */
    public static class Menger extends Feature<NoneFeatureConfiguration> {
        public Menger(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 90, 170);
            BlockState stone = b("axiom_stone"), lattice = b("fractal_lattice_block"), glass = b("golden_ratio_glass");
            for (int x = 0; x < 27; x++)
                for (int y = 0; y < 27; y++)
                    for (int z = 0; z < 27; z++) {
                        if (!inMenger(x, y, z)) continue;
                        int lvl = ((x / 9) + (y / 9) + (z / 9)) % 2;
                        boolean edge = (x % 9 == 0 || x % 9 == 8) && (y % 9 == 0 || y % 9 == 8) && (z % 9 == 0 || z % 9 == 8);
                        put(l, c.offset(x - 13, y - 13, z - 13), edge ? glass : lvl == 0 ? stone : lattice);
                    }
            chest(l, c, VAULT, ctx.random());
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 18, com.theoreticalsciences.world.zones.StructureZones.Mode.FRACTAL);
            return true;
        }

        static boolean inMenger(int x, int y, int z) {
            for (int k = 0; k < 3; k++) {
                int ones = (x % 3 == 1 ? 1 : 0) + (y % 3 == 1 ? 1 : 0) + (z % 3 == 1 ? 1 : 0);
                if (ones >= 2) return false;
                x /= 3;
                y /= 3;
                z /= 3;
            }
            return true;
        }
    }

    /** Sierpinski tetrahedron, 32 blocks: the voxels whose coordinates share no binary digit pairwise. */
    public static class Sierpinski extends Feature<NoneFeatureConfiguration> {
        public Sierpinski(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 80, 160);
            BlockState crystal = b("koch_crystal"), stone = b("axiom_stone");
            int n = 32;
            for (int x = 0; x < n; x++)
                for (int y = 0; y < n; y++)
                    for (int z = 0; z < n; z++) {
                        if ((x & y) != 0 || (y & z) != 0 || (x & z) != 0) continue;
                        // tip pointing up: y counts down from the apex
                        put(l, c.offset(x - 16, (n - 1 - y) - 16, z - 16), Integer.bitCount(x | y | z) <= 1 ? stone : crystal);
                    }
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 20, com.theoreticalsciences.world.zones.StructureZones.Mode.FRACTAL);
            return true;
        }
    }

    /** Mandelbulb (power 8), radius 14: points that stay bounded under z -> z^8 + c in spherical coordinates. */
    public static class Mandelbulb extends Feature<NoneFeatureConfiguration> {
        public Mandelbulb(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 90, 170);
            BlockState stone = b("axiom_stone"), glass = b("golden_ratio_glass"), lattice = b("fractal_lattice_block");
            int R = 14;
            for (int x = -R; x <= R; x++)
                for (int y = -R; y <= R; y++)
                    for (int z = -R; z <= R; z++) {
                        double cx = x / (double) R * 1.2, cy = y / (double) R * 1.2, cz = z / (double) R * 1.2;
                        int it = iterate(cx, cy, cz);
                        if (it < 0) continue;
                        // keep only the skin and a few blocks under it: the inside is never seen
                        boolean surface = iterate((x + 1) / (double) R * 1.2, cy, cz) >= 0 && iterate((x - 1) / (double) R * 1.2, cy, cz) >= 0
                                && iterate(cx, (y + 1) / (double) R * 1.2, cz) >= 0 && iterate(cx, (y - 1) / (double) R * 1.2, cz) >= 0
                                && iterate(cx, cy, (z + 1) / (double) R * 1.2) >= 0 && iterate(cx, cy, (z - 1) / (double) R * 1.2) >= 0;
                        put(l, c.offset(x, y, z), !surface ? glass : it % 3 == 0 ? lattice : stone);
                    }
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 18, com.theoreticalsciences.world.zones.StructureZones.Mode.FRACTAL);
            return true;
        }

        /** -1 if it escapes, else how deep the orbit stayed (for colouring). */
        static int iterate(double cx, double cy, double cz) {
            double x = cx, y = cy, z = cz;
            double minR = 10;
            for (int i = 0; i < 8; i++) {
                double r = Math.sqrt(x * x + y * y + z * z);
                if (r > 2) return -1;
                minR = Math.min(minR, r);
                double th = Math.acos(r == 0 ? 0 : z / r) * 8, ph = Math.atan2(y, x) * 8, rn = Math.pow(r, 8);
                x = rn * Math.sin(th) * Math.cos(ph) + cx;
                y = rn * Math.sin(th) * Math.sin(ph) + cy;
                z = rn * Math.cos(th) + cz;
            }
            return (int) (minR * 10);
        }
    }

    /** Cantor dust: the 3D product of three Cantor sets, 27 blocks wide - a floating cloud of 512 cubes. */
    public static class CantorDust extends Feature<NoneFeatureConfiguration> {
        public CantorDust(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        static boolean cantor(int v) {
            for (int k = 0; k < 3; k++) {
                if (v % 3 == 1) return false;
                v /= 3;
            }
            return true;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 100, 180);
            BlockState g = b("golden_ratio_glass"), f = b("fractal_lattice_block");
            for (int x = 0; x < 27; x++)
                for (int y = 0; y < 27; y++)
                    for (int z = 0; z < 27; z++)
                        if (cantor(x) && cantor(y) && cantor(z)) put(l, c.offset(x - 13, y - 13, z - 13), ((x + y + z) & 1) == 0 ? g : f);
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 18, com.theoreticalsciences.world.zones.StructureZones.Mode.FRACTAL);
            return true;
        }
    }

    /** A recursive tree: every branch splits into three copies of itself, each 1/phi as long. */
    public static class FractalTree extends Feature<NoneFeatureConfiguration> {
        public FractalTree(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos o = ctx.origin();
            BlockPos base = l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos((o.getX() & ~15) + 8, 0, (o.getZ() & ~15) + 8));
            if (base.getY() <= l.getMinBuildHeight() + 2) return false;
            branch(l, Vec3.atBottomCenterOf(base), new Vec3(0, 1, 0), 9, 0, ctx.random().nextDouble() * Math.PI, base);
            com.theoreticalsciences.world.zones.StructureZones.mark(l, base.above(12), 16, com.theoreticalsciences.world.zones.StructureZones.Mode.FRACTAL);
            return true;
        }

        void branch(WorldGenLevel l, Vec3 a, Vec3 dir, double len, int depth, double yaw, BlockPos root) {
            BlockState wood = b("fractal_lattice_block"), leaf = b("koch_crystal");
            Vec3 end = a.add(dir.scale(len));
            for (double t = 0; t <= len; t += 0.5) {
                Vec3 p = a.add(dir.scale(t));
                BlockPos bp = BlockPos.containing(p);
                if (Math.abs(bp.getX() - root.getX()) > 15 || Math.abs(bp.getZ() - root.getZ()) > 15) return;
                put(l, bp, wood);
            }
            if (depth >= 4 || len < 1.6) {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++)
                            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 1) {
                                BlockPos bp = BlockPos.containing(end).offset(dx, dy, dz);
                                if (Math.abs(bp.getX() - root.getX()) <= 15 && Math.abs(bp.getZ() - root.getZ()) <= 15) put(l, bp, leaf);
                            }
                return;
            }
            for (int k = 0; k < 3; k++) {
                double az = yaw + k * 2 * Math.PI / 3, tilt = Math.toRadians(32);
                Vec3 side = new Vec3(Math.cos(az), 0, Math.sin(az));
                Vec3 nd = dir.scale(Math.cos(tilt)).add(side.scale(Math.sin(tilt))).normalize();
                branch(l, end, nd, len / 1.618, depth + 1, yaw + 0.6, root);
            }
        }
    }

    /** The Hilbert Archive: a corridor that walks an order-3 Hilbert curve; its last cell holds the archive chest. */
    public static class HilbertArchive extends Feature<NoneFeatureConfiguration> {
        public HilbertArchive(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        static int[] d2xy(int n, int d) {
            int x = 0, y = 0, t = d;
            for (int s = 1; s < n; s *= 2) {
                int rx = 1 & (t / 2), ry = 1 & (t ^ rx);
                if (ry == 0) {
                    if (rx == 1) {
                        x = s - 1 - x;
                        y = s - 1 - y;
                    }
                    int tmp = x;
                    x = y;
                    y = tmp;
                }
                x += s * rx;
                y += s * ry;
                t /= 4;
            }
            return new int[]{x, y};
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 70, 150);
            BlockState wall = b("axiom_stone"), floor = b("golden_ratio_glass"), lamp = b("koch_crystal");
            int cells = 8, size = cells * 3 + 1;    // 25 x 25
            boolean[][] open = new boolean[size][size];
            int[] prev = null;
            for (int d = 0; d < cells * cells; d++) {
                int[] p = d2xy(cells, d);
                int gx = p[0] * 3 + 1, gz = p[1] * 3 + 1;
                for (int a = 0; a < 2; a++) for (int bb = 0; bb < 2; bb++) open[gx + a][gz + bb] = true;
                if (prev != null) {                  // carve the corridor between consecutive cells
                    int px = prev[0] * 3 + 1, pz = prev[1] * 3 + 1;
                    for (int x = Math.min(px, gx); x <= Math.max(px, gx) + 1; x++)
                        for (int z = Math.min(pz, gz); z <= Math.max(pz, gz) + 1; z++) open[x][z] = true;
                }
                prev = p;
            }
            for (int x = 0; x < size; x++)
                for (int z = 0; z < size; z++) {
                    BlockPos p = c.offset(x - 12, 0, z - 12);
                    l.setBlock(p, floor, 2);
                    l.setBlock(p.above(4), (x % 6 == 1 && z % 6 == 1) ? lamp : wall, 2);
                    for (int h = 1; h <= 3; h++) l.setBlock(p.above(h), open[x][z] ? Blocks.AIR.defaultBlockState() : wall, 2);
                }
            int[] last = d2xy(cells, cells * cells - 1), first = d2xy(cells, 0);
            chest(l, c.offset(last[0] * 3 + 1 - 12, 1, last[1] * 3 + 1 - 12), ARCHIVE, ctx.random());
            // the entrance: knock through the outer wall next to the first cell
            BlockPos door = c.offset(first[0] * 3 + 1 - 12, 1, -12);
            for (int h = 0; h < 3; h++) l.setBlock(door.above(h), Blocks.AIR.defaultBlockState(), 2);
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c.above(2), 15, com.theoreticalsciences.world.zones.StructureZones.Mode.INFINITE);
            return true;
        }
    }

    private FractalFeatures() {}
}
