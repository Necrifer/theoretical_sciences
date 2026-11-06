package com.theoreticalsciences.ocean;

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
 * 1.2.0: the landforms of Titan's hidden ocean. A heightmap there finds the ice ceiling, so each feature goes down
 * through the water to the floor itself (or up to the ice, for brinicles).
 */
public final class OceanFeatures {
    private OceanFeatures() {}

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    /** The floor under (x, z): the lowest water block above rock, or null. */
    static BlockPos floor(WorldGenLevel l, BlockPos o) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(o.getX(), 150, o.getZ());
        while (m.getY() > 2 && l.getFluidState(m).isEmpty()) m.move(0, -1, 0);
        while (m.getY() > 2 && !l.getFluidState(m.below()).isEmpty()) m.move(0, -1, 0);
        return m.getY() <= 2 || l.getFluidState(m).isEmpty() ? null : m.immutable();
    }

    /** The water just under the ice above (x, z), or null. */
    static BlockPos ceiling(WorldGenLevel l, BlockPos o) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(o.getX(), 120, o.getZ());
        if (l.getFluidState(m).isEmpty()) return null;
        while (m.getY() < 250 && !l.getFluidState(m.above()).isEmpty()) m.move(0, 1, 0);
        return m.getY() >= 250 ? null : m.immutable();
    }

    /**
     * A vent field: a mound of rock built by the vent's own minerals, two to four tapering chimneys on it each crowned by a
     * black smoker, and a ring of giant tube worms round its foot where the warm water seeps out.
     */
    public static class VentField extends Feature<NoneFeatureConfiguration> {
        public VentField(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = floor(l, ctx.origin());
            if (o == null) return false;
            BlockState rock = b("abyssal_silicate"), chim = b("hydrothermal_chimney"), smoker = b("black_smoker"), worm = b("tube_worms");
            int R = 4 + r.nextInt(3), H = 2 + r.nextInt(2);
            for (int x = -R; x <= R; x++)
                for (int z = -R; z <= R; z++) {
                    double d = Math.sqrt(x * x + z * z) / R;
                    if (d > 1) continue;
                    int h = (int) Math.round(H * (1 - d * d)) + (r.nextInt(4) == 0 ? 1 : 0);
                    for (int y = -1; y < h; y++) setBlock(l, o.offset(x, y, z), y >= h - 1 && r.nextInt(3) == 0 ? chim : rock);
                }
            int n = 2 + r.nextInt(3);
            for (int k = 0; k < n; k++) {
                int cx = r.nextInt(R + 1) - R / 2, cz = r.nextInt(R + 1) - R / 2;
                int tall = 6 + r.nextInt(10);
                int base = H - 1;
                for (int y = 0; y < tall; y++) {
                    double rad = 2.0 * (1 - y / (double) tall) + 0.4;
                    int ri = (int) Math.ceil(rad);
                    for (int x = -ri; x <= ri; x++)
                        for (int z = -ri; z <= ri; z++)
                            if (x * x + z * z <= rad * rad) {
                                BlockPos p = o.offset(cx + x, base + y, cz + z);
                                if (!l.getFluidState(p).isEmpty()) setBlock(l, p, chim);
                            }
                }
                BlockPos top = o.offset(cx, base + tall, cz);
                if (!l.getFluidState(top).isEmpty()) setBlock(l, top, smoker);
            }
            for (int i = 0; i < 26; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = R + 0.5 + r.nextDouble() * 3;
                BlockPos p = floor(l, o.offset((int) Math.round(Math.cos(a) * d), 0, (int) Math.round(Math.sin(a) * d)));
                if (p == null || Math.abs(p.getY() - o.getY()) > 4) continue;
                setBlock(l, p, worm);
                if (r.nextInt(3) == 0 && !l.getFluidState(p.above()).isEmpty()) setBlock(l, p.above(), worm);
            }
            return true;
        }
    }

    /** Brinicles: icicles of frozen brine hanging from the ice into the water, in clusters. */
    public static class Brinicles extends Feature<NoneFeatureConfiguration> {
        public Brinicles(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockState br = b("brinicle");
            boolean any = false;
            for (int i = 0; i < 10; i++) {
                BlockPos c = ceiling(l, ctx.origin().offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4));
                if (c == null) continue;
                int len = 1 + r.nextInt(4);
                for (int y = 0; y < len && !l.getFluidState(c.below(y)).isEmpty(); y++) setBlock(l, c.below(y), br);
                any = true;
            }
            return any;
        }
    }

    /** Boulders of silicate on the open floor, so the plain is not bare. */
    public static class Boulders extends Feature<NoneFeatureConfiguration> {
        public Boulders(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = floor(l, ctx.origin());
            if (o == null) return false;
            BlockState rock = b("abyssal_silicate");
            double R = 1.5 + r.nextDouble() * 2.5;
            int ri = (int) Math.ceil(R);
            for (int x = -ri; x <= ri; x++)
                for (int y = -1; y <= ri; y++)
                    for (int z = -ri; z <= ri; z++)
                        if (x * x + y * y * 1.6 + z * z <= R * R) setBlock(l, o.offset(x, y, z), rock);
            return true;
        }
    }
}
