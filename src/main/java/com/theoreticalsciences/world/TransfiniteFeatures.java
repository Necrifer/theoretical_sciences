package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * Landmarks of the Transfinite Hierarchy (infinities and higher dimensions), plus the Quantum Foam's boundary layers.
 */
public final class TransfiniteFeatures {
    public static final ResourceKey<LootTable> HOTEL = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/hilbert_hotel"));
    public static final ResourceKey<LootTable> SPIRE = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/ordinal_spire"));

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    /** Fills the bottom two layers of every Quantum Foam chunk with Schrödinger's Limit and the top two with Relativity Breakdown. */
    public static class BoundaryLayers extends Feature<NoneFeatureConfiguration> {
        public BoundaryLayers(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos o = ctx.origin();
            int x0 = o.getX() & ~15, z0 = o.getZ() & ~15;
            BlockState low = QHFluids.get("schrodinger_limit").block.get().defaultBlockState(), high = QHFluids.get("relativity_breakdown").block.get().defaultBlockState();
            int min = l.getMinBuildHeight(), max = l.getMaxBuildHeight() - 1;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 2; y++) {
                        l.setBlock(p.set(x0 + x, min + y, z0 + z), low, 2);
                        l.setBlock(p.set(x0 + x, max - y, z0 + z), high, 2);
                    }
                }
            return true;
        }
    }

    static BlockPos centre(FeaturePlaceContext<?> ctx, int lo, int hi) {
        BlockPos o = ctx.origin();
        return new BlockPos((o.getX() & ~15) + 8, lo + ctx.random().nextInt(hi - lo + 1), (o.getZ() & ~15) + 8);
    }

    static void chest(WorldGenLevel l, BlockPos p, ResourceKey<LootTable> loot, net.minecraft.util.RandomSource r) {
        com.theoreticalsciences.dungeon.Caches.place(l, p, loot, r);
    }

    /**
     * Hilbert's Hotel: a corridor of rooms that is always full and always has room for one more. Thirty rooms here (the
     * rest, presumably, further down the corridor); a few hold chests.
     */
    public static class HilbertHotel extends Feature<NoneFeatureConfiguration> {
        public HilbertHotel(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 60, 150);
            BlockState wall = b("hilbert_brick"), floor = b("ordinal_stone"), lamp = b("aleph_glass");
            // corridor along x from -15 to 15, rooms either side (z -7..-2 and 2..7), three floors
            for (int floorN = 0; floorN < 3; floorN++) {
                int y0 = c.getY() + floorN * 5;
                for (int x = -15; x <= 15; x++)
                    for (int z = -7; z <= 7; z++) {
                        boolean room = Math.abs(z) >= 2;
                        boolean partition = room && (x + 15) % 5 == 0;
                        l.setBlock(new BlockPos(c.getX() + x, y0, c.getZ() + z), floor, 2);
                        l.setBlock(new BlockPos(c.getX() + x, y0 + 4, c.getZ() + z), (x % 5 == 0 && z == 0) ? lamp : wall, 2);
                        for (int h = 1; h <= 3; h++) {
                            boolean outer = Math.abs(z) == 7 || Math.abs(x) == 15;
                            boolean door = Math.abs(z) == 2 && (x + 15) % 5 == 2 && h <= 2;
                            boolean solid = outer || partition || (Math.abs(z) == 2 && !door);
                            l.setBlock(new BlockPos(c.getX() + x, y0 + h, c.getZ() + z), solid ? wall : Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                for (int k = 0; k < 6; k++) {
                    if (ctx.random().nextInt(3) != 0) continue;
                    int rx = -13 + k * 5 + 1, rz = ctx.random().nextBoolean() ? 5 : -5;
                    chest(l, new BlockPos(c.getX() + rx, y0 + 1, c.getZ() + rz), HOTEL, ctx.random());
                }
                // a guest that should not be staying here: one engine per floor, in a room of its own
                l.setBlock(new BlockPos(c.getX() - 12 + floorN * 10, y0 + 1, c.getZ() + (floorN % 2 == 0 ? 4 : -4)), b("breached_anomaly_engine"), 2);
            }
            // the entrance at one end
            for (int h = 1; h <= 2; h++) l.setBlock(new BlockPos(c.getX() - 15, c.getY() + h, c.getZ()), Blocks.AIR.defaultBlockState(), 2);
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c.above(7), 17, com.theoreticalsciences.world.zones.StructureZones.Mode.INFINITE);
            return true;
        }
    }

    /** A tesseract built in stone: an inner and an outer cube with their corners joined - a 4D cube's shadow in 3D. */
    public static class TesseractFrame extends Feature<NoneFeatureConfiguration> {
        public TesseractFrame(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 90, 180);
            BlockState outer = b("hilbert_brick"), inner = b("aleph_glass"), link = b("hyperspherical_ore");
            int R = 14, r = 6;
            edges(l, c, R, outer);
            edges(l, c, r, inner);
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sy = -1; sy <= 1; sy += 2)
                    for (int sz = -1; sz <= 1; sz += 2)
                        line(l, c.offset(sx * r, sy * r, sz * r), c.offset(sx * R, sy * R, sz * R), link);
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 17, com.theoreticalsciences.world.zones.StructureZones.Mode.TESSERACT);
            return true;
        }

        static void edges(WorldGenLevel l, BlockPos c, int h, BlockState s) {
            for (int a = -h; a <= h; a++)
                for (int s1 = -1; s1 <= 1; s1 += 2)
                    for (int s2 = -1; s2 <= 1; s2 += 2) {
                        l.setBlock(c.offset(a, s1 * h, s2 * h), s, 2);
                        l.setBlock(c.offset(s1 * h, a, s2 * h), s, 2);
                        l.setBlock(c.offset(s1 * h, s2 * h, a), s, 2);
                    }
        }

        static void line(WorldGenLevel l, BlockPos a, BlockPos b, BlockState s) {
            int n = Math.max(Math.abs(b.getX() - a.getX()), Math.max(Math.abs(b.getY() - a.getY()), Math.abs(b.getZ() - a.getZ())));
            for (int i = 0; i <= n; i++) {
                double t = n == 0 ? 0 : i / (double) n;
                l.setBlock(BlockPos.containing(a.getX() + (b.getX() - a.getX()) * t + 0.5, a.getY() + (b.getY() - a.getY()) * t + 0.5,
                        a.getZ() + (b.getZ() - a.getZ()) * t + 0.5), s, 2);
            }
        }
    }

    /**
     * The Hopf fibration, stereographically projected: the 3-sphere is a family of circles, every two of them linked.
     * Built as a set of linked rings of glass on nested tori.
     */
    public static class HopfRings extends Feature<NoneFeatureConfiguration> {
        public HopfRings(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos c = centre(ctx, 100, 180);
            BlockState[] mats = {b("aleph_glass"), b("hilbert_brick"), b("hyperspherical_ore")};
            int fibres = 0;
            for (double eta : new double[]{0.35, 0.75, 1.15}) {
                for (int k = 0; k < 8; k++) {
                    double xi2 = k * Math.PI * 2 / 8;
                    BlockState m = mats[fibres++ % mats.length];
                    for (int i = 0; i < 160; i++) {
                        double xi1 = i * Math.PI * 2 / 160;
                        // point on S^3: (cos(xi1+xi2) sin eta, sin(xi1+xi2) sin eta, cos(xi1) cos eta, sin(xi1) cos eta)
                        double x1 = Math.cos(xi1 + xi2) * Math.sin(eta), x2 = Math.sin(xi1 + xi2) * Math.sin(eta);
                        double x3 = Math.cos(xi1) * Math.cos(eta), x4 = Math.sin(xi1) * Math.cos(eta);
                        double d = 1.0001 - x4;                         // stereographic projection from (0,0,0,1)
                        Vec3 p = new Vec3(x1 / d, x3 / d, x2 / d).scale(5.0);
                        if (p.length() > 14) continue;
                        l.setBlock(BlockPos.containing(c.getX() + p.x + 0.5, c.getY() + p.y + 0.5, c.getZ() + p.z + 0.5), m, 2);
                    }
                }
            }
            com.theoreticalsciences.world.zones.StructureZones.mark(l, c, 15, com.theoreticalsciences.world.zones.StructureZones.Mode.TWIST);
            return true;
        }
    }

    /**
     * The Ordinal Spire: ω floors, each half the height of the one below - so the tower is finite, but never finished.
     * The vault at its top holds what lies past every finite step.
     */
    public static class OrdinalSpire extends Feature<NoneFeatureConfiguration> {
        public OrdinalSpire(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            BlockPos o = ctx.origin();
            BlockPos base = l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, new BlockPos((o.getX() & ~15) + 8, 0, (o.getZ() & ~15) + 8));
            if (base.getY() <= l.getMinBuildHeight() + 4) return false;
            BlockState wall = b("hilbert_brick"), glass = b("aleph_glass");
            int y = base.getY(), h = 16, r = 7;
            while (h >= 1 && r >= 1) {
                for (int dy = 0; dy < h; dy++)
                    for (int dx = -r; dx <= r; dx++)
                        for (int dz = -r; dz <= r; dz++) {
                            boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                            if (dy == 0) l.setBlock(base.offset(dx, y - base.getY(), dz), glass, 2);
                            else if (edge) l.setBlock(base.offset(dx, y - base.getY() + dy, dz), (dy % 2 == 0 && Math.abs(dx) != Math.abs(dz)) ? glass : wall, 2);
                            else l.setBlock(base.offset(dx, y - base.getY() + dy, dz), Blocks.AIR.defaultBlockState(), 2);
                        }
                y += h;
                h /= 2;
                r = Math.max(1, r - 2);
                if (r == 1 && h <= 1) break;
            }
            l.setBlock(new BlockPos(base.getX(), y, base.getZ()), wall, 2);
            chest(l, new BlockPos(base.getX(), y + 1, base.getZ()), SPIRE, ctx.random());
            com.theoreticalsciences.world.zones.StructureZones.mark(l, new BlockPos(base.getX(), (base.getY() + y) / 2, base.getZ()), Math.max(10, (y - base.getY()) / 2 + 4), com.theoreticalsciences.world.zones.StructureZones.Mode.INFINITE);
            return true;
        }
    }

    private TransfiniteFeatures() {}
}
