package com.theoreticalsciences.ocean;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.3.0: the landforms of the Hadal Trench. Titan's ocean has an ice lid, so its features search a fixed band; the
 * trench is open water to its surface, so these find the floor from the ocean-floor heightmap and, if that is not
 * water over rock, by going down from the sea's surface. They build in the trench's own rock, Oceanic Basalt.
 */
public final class HadalFeatures {
    private HadalFeatures() {}

    static BlockState b(String id) {
        return OceanFeatures.b(id);
    }

    /** The lowest water block over rock at (x, z), or null. */
    static BlockPos floor(WorldGenLevel l, int x, int z) {
        int y = l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        BlockPos p = new BlockPos(x, y, z);
        if (!l.getFluidState(p).isEmpty() && l.getFluidState(p.below()).isEmpty() && !l.getBlockState(p.below()).isAir()) return p;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, l.getMaxBuildHeight() - 1, z);
        while (m.getY() > l.getMinBuildHeight() + 2 && l.getFluidState(m).isEmpty()) m.move(Direction.DOWN);
        while (m.getY() > l.getMinBuildHeight() + 2 && !l.getFluidState(m.below()).isEmpty()) m.move(Direction.DOWN);
        return m.getY() <= l.getMinBuildHeight() + 2 || l.getFluidState(m).isEmpty() ? null : m.immutable();
    }

    static boolean water(WorldGenLevel l, BlockPos p) {
        return !l.getFluidState(p).isEmpty();
    }

    /**
     * A chimney: a crooked spire of sulfide that leans and wanders as it grows, swelling into ledges (flanges) where hot
     * water leaked out of its side, and capped by a black smoker. Returns the height reached.
     */
    static int chimney(Feature<?> f, WorldGenLevel l, RandomSource r, BlockPos base, int tall, double r0) {
        BlockState chim = b("hydrothermal_chimney"), smoker = b("black_smoker");
        double cx = 0, cz = 0;
        double dx = (r.nextDouble() - 0.5) * 0.3, dz = (r.nextDouble() - 0.5) * 0.3;
        int y = 0;
        for (; y < tall; y++) {
            double t = y / (double) tall;
            double rad = r0 * (1 - t) + 0.45;
            if (r.nextInt(7) == 0 && y > 2 && y < tall - 2) rad += 1.2;            // a flange
            cx += dx + (r.nextDouble() - 0.5) * 0.35;
            cz += dz + (r.nextDouble() - 0.5) * 0.35;
            cx = Math.max(-4, Math.min(4, cx));                                     // stays within the chunks being built
            cz = Math.max(-4, Math.min(4, cz));
            int ri = (int) Math.ceil(rad);
            boolean any = false;
            for (int x = -ri; x <= ri; x++)
                for (int z = -ri; z <= ri; z++) {
                    double ex = x - (cx - Math.round(cx)), ez = z - (cz - Math.round(cz));
                    if (ex * ex + ez * ez > rad * rad) continue;
                    BlockPos p = base.offset((int) Math.round(cx) + x, y, (int) Math.round(cz) + z);
                    if (water(l, p)) {
                        l.setBlock(p, chim, 3);
                        any = true;
                    }
                }
            if (!any && y > 0) break;
        }
        BlockPos top = base.offset((int) Math.round(cx), y, (int) Math.round(cz));
        if (water(l, top)) l.setBlock(top, smoker, 3);
        // a second, smaller smoker breaking out of the side, now and then
        if (tall > 12 && r.nextInt(3) == 0) {
            int sy = tall / 2 + r.nextInt(tall / 3);
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos side = base.offset((int) Math.round(cx * sy / Math.max(1, y)), sy, (int) Math.round(cz * sy / Math.max(1, y)));
            for (int k = 1; k < 4; k++) {
                BlockPos p = side.relative(d, k);
                if (water(l, p)) {
                    l.setBlock(p, smoker, 3);
                    break;
                }
            }
        }
        return y;
    }

    /** Giant tube worms in thickets round a vent, where the warm water seeps out of the rock. */
    static void worms(Feature<?> f, WorldGenLevel l, RandomSource r, BlockPos o, double inner, double outer, int tries) {
        BlockState worm = b("tube_worms");
        for (int i = 0; i < tries; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = inner + r.nextDouble() * (outer - inner);
            BlockPos p = floor(l, o.getX() + (int) Math.round(Math.cos(a) * d), o.getZ() + (int) Math.round(Math.sin(a) * d));
            if (p == null || Math.abs(p.getY() - o.getY()) > 6) continue;
            l.setBlock(p, worm, 3);
            if (r.nextInt(3) == 0 && water(l, p.above())) l.setBlock(p.above(), worm, 3);
        }
    }

    /**
     * A vent field, the oases of the trench: a broad mound of basalt crusted with sulfide, three to six crooked chimneys
     * rising from it ten to thirty blocks, each smoking black at its top, and thickets of tube worms round its foot.
     */
    public static class VentField extends Feature<NoneFeatureConfiguration> {
        public VentField(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = floor(l, ctx.origin().getX(), ctx.origin().getZ());
            if (o == null) return false;
            BlockState rock = b("oceanic_basalt"), chim = b("hydrothermal_chimney");
            int R = 6 + r.nextInt(4), H = 2 + r.nextInt(3);
            for (int x = -R; x <= R; x++)
                for (int z = -R; z <= R; z++) {
                    double d = Math.sqrt(x * x + z * z) / R;
                    if (d > 1) continue;
                    int h = (int) Math.round(H * (1 - d * d)) + (r.nextInt(5) == 0 ? 1 : 0);
                    for (int y = -2; y < h; y++) {
                        BlockPos p = o.offset(x, y, z);
                        if (y >= 0 && !water(l, p)) continue;
                        setBlock(l, p, y >= h - 1 && r.nextInt(d < 0.6 ? 2 : 5) == 0 ? chim : rock);
                    }
                }
            int n = 3 + r.nextInt(4);
            for (int k = 0; k < n; k++) {
                double a = r.nextDouble() * Math.PI * 2, d = r.nextDouble() * R * 0.55;
                int cx = (int) Math.round(Math.cos(a) * d), cz = (int) Math.round(Math.sin(a) * d);
                double dd = Math.sqrt(cx * cx + cz * cz) / R;
                int base = (int) Math.round(H * (1 - dd * dd)) - 1;
                chimney(this, l, r, o.offset(cx, base, cz), 10 + r.nextInt(21), 1.6 + r.nextDouble() * 0.9);
            }
            worms(this, l, r, o, R * 0.7, R + 5, 60);
            return true;
        }
    }

    /** A lone chimney out on the abyssal plain, on a low mound of its own, with a few worms round it. */
    public static class LoneChimney extends Feature<NoneFeatureConfiguration> {
        public LoneChimney(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = floor(l, ctx.origin().getX(), ctx.origin().getZ());
            if (o == null) return false;
            BlockState rock = b("oceanic_basalt");
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++)
                    if (x * x + z * z <= 5) {
                        setBlock(l, o.offset(x, -1, z), rock);
                        if (x * x + z * z <= 2 && water(l, o.offset(x, 0, z))) setBlock(l, o.offset(x, 0, z), rock);
                    }
            chimney(this, l, r, o.above(), 7 + r.nextInt(12), 1.1 + r.nextDouble() * 0.6);
            worms(this, l, r, o, 2, 5, 14);
            return true;
        }
    }

    /**
     * Pillow basalt: lava that erupted under the sea, chilled the instant it met the water and budded out in rounded
     * lobes, one on another, in a winding heap along the fissure it came from.
     */
    public static class PillowBasalt extends Feature<NoneFeatureConfiguration> {
        public PillowBasalt(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = floor(l, ctx.origin().getX(), ctx.origin().getZ());
            if (o == null) return false;
            BlockState rock = b("oceanic_basalt");
            double heading = r.nextDouble() * Math.PI * 2;
            double px = 0, pz = 0;
            int lobes = 4 + r.nextInt(6);
            for (int i = 0; i < lobes; i++) {
                heading += (r.nextDouble() - 0.5) * 1.4;
                double len = 1.8 + r.nextDouble() * 1.6, wid = 1.1 + r.nextDouble() * 0.6, hgt = 0.9 + r.nextDouble() * 0.6;
                double cos = Math.cos(heading), sin = Math.sin(heading);
                if (Math.hypot(px, pz) > 9) break;                                  // stays within the chunks being built
                BlockPos fl = floor(l, o.getX() + (int) Math.round(px), o.getZ() + (int) Math.round(pz));
                if (fl == null || Math.abs(fl.getY() - o.getY()) > 5) break;
                int lift = r.nextInt(3) == 0 ? 1 : 0;                                // a lobe budding over the last
                int ri = (int) Math.ceil(len);
                for (int x = -ri; x <= ri; x++)
                    for (int z = -ri; z <= ri; z++)
                        for (int y = -1; y <= (int) Math.ceil(hgt) + lift; y++) {
                            double u = x * cos + z * sin, v = -x * sin + z * cos;
                            double e = (u * u) / (len * len) + (v * v) / (wid * wid) + ((y - lift) * (y - lift)) / (hgt * hgt + 0.01);
                            if (e > 1) continue;
                            BlockPos p = fl.offset(x, y, z);
                            if (y < 0 || water(l, p)) setBlock(l, p, rock);
                        }
                px += cos * len * 1.3;
                pz += sin * len * 1.3;
            }
            return true;
        }
    }
}
