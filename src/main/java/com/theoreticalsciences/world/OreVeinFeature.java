package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * A winding ore vein: a tube of ore that snakes through the host rock for a few dozen blocks, thickest in the
 * middle. Fewer veins than scattered blobs, but find one and you have a seam to follow instead of a hunt.
 * It never reaches more than one chunk away from where it started, so world generation stays safe.
 */
public class OreVeinFeature extends Feature<OreVeinFeature.Config> {
    public record Config(RuleTest target, BlockState state, int length, float radius, float density, int branches) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                RuleTest.CODEC.fieldOf("target").forGetter(Config::target),
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                Codec.intRange(4, 64).fieldOf("length").forGetter(Config::length),
                Codec.floatRange(0.5F, 4F).fieldOf("radius").forGetter(Config::radius),
                Codec.floatRange(0.05F, 1F).fieldOf("density").forGetter(Config::density),
                Codec.intRange(0, 6).optionalFieldOf("branches", 0).forGetter(Config::branches)
        ).apply(i, Config::new));
    }

    public OreVeinFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        RandomSource r = ctx.random();
        Config c = ctx.config();
        BlockPos o = ctx.origin();
        int cx = (o.getX() & ~15) + 8, cz = (o.getZ() & ~15) + 8;
        double yaw = r.nextDouble() * Math.PI * 2, pitch = (r.nextDouble() - 0.5) * 0.6;
        int len = c.length() / 2 + r.nextInt(c.length() / 2 + 1);
        return tube(ctx.level(), r, c, cx, cz, o.getX() + 0.5, o.getY() + 0.5, o.getZ() + 0.5, yaw, pitch, len, c.radius(), c.branches());
    }

    /** One tube of ore; with branches left, it forks side seams off itself at random points along the way. */
    private static boolean tube(WorldGenLevel level, RandomSource r, Config c, int cx, int cz, double x, double y, double z,
                                double yaw, double pitch, int len, float radius, int branches) {
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int[] forks = new int[branches];
        for (int i = 0; i < branches; i++) forks[i] = len / 5 + r.nextInt(Math.max(1, len * 3 / 5));
        for (int step = 0; step < len; step++) {
            double t = step / (double) Math.max(1, len - 1);
            float rad = radius * (0.55F + 0.45F * Mth.sin((float) (t * Math.PI)));
            int ir = Mth.ceil(rad);
            for (int dx = -ir; dx <= ir; dx++)
                for (int dy = -ir; dy <= ir; dy++)
                    for (int dz = -ir; dz <= ir; dz++) {
                        if (dx * dx + dy * dy + dz * dz > rad * rad + 0.3) continue;
                        if (r.nextFloat() > c.density()) continue;
                        p.set(Mth.floor(x) + dx, Mth.floor(y) + dy, Mth.floor(z) + dz);
                        if (Math.abs(p.getX() - cx) > 22 || Math.abs(p.getZ() - cz) > 22) continue;     // stay within the neighbouring chunks
                        if (p.getY() <= level.getMinBuildHeight() || p.getY() >= level.getMaxBuildHeight()) continue;
                        if (c.target().test(level.getBlockState(p), r)) {
                            level.setBlock(p, c.state(), 2);
                            any = true;
                        }
                    }
            for (int f : forks)
                if (f == step) {
                    double side = yaw + (r.nextBoolean() ? 1 : -1) * (0.7 + r.nextDouble() * 0.9);
                    any |= tube(level, r, c, cx, cz, x, y, z, side, (r.nextDouble() - 0.5) * 0.9,
                            Math.max(6, len / 2 + r.nextInt(Math.max(1, len / 3))), radius * 0.7F, 0);
                }
            yaw += (r.nextDouble() - 0.5) * 0.7;
            pitch = Mth.clamp(pitch + (r.nextDouble() - 0.5) * 0.25, -0.7, 0.7);
            x += Math.cos(yaw) * Math.cos(pitch);
            z += Math.sin(yaw) * Math.cos(pitch);
            y += Math.sin(pitch);
            // turn back toward the start chunk rather than wander out of reach
            if (Math.abs(x - cx) > 18 || Math.abs(z - cz) > 18) yaw = Math.atan2(cz - z, cx - x);
        }
        return any;
    }
}
