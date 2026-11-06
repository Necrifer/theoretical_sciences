package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.2.0: the impact craters of Selk on Titan. A bowl sunk below the plain, a raised rim thrown up round it, and - in the
 * bigger ones - a central peak, where the rock rebounded after the impact (a complex crater, as Selk itself is). The
 * floor is tholin sand blown in since; the rim is bare water-ice bedrock, dusted with tholin. Built at world generation
 * only, and never over a lake or the sea.
 */
public class SelkCraterFeature extends Feature<NoneFeatureConfiguration> {
    public SelkCraterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos o = ctx.origin();
        // centred in its chunk, so that the whole crater (up to 16 blocks out) stays where generation may write
        int cx = (o.getX() & ~15) + 8, cz = (o.getZ() & ~15) + 8;
        int base = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        if (base <= level.getMinBuildHeight() + 12) return false;
        if (!level.getFluidState(new BlockPos(cx, base, cz)).isEmpty() || !level.getFluidState(new BlockPos(cx, base - 1, cz)).isEmpty()) return false;
        BlockState ice = QHBlocks.get("titan_ice").defaultBlockState(), sand = QHBlocks.get("tholin_sand").defaultBlockState();
        int R = 8 + rand.nextInt(6);                                      // 8 to 13 blocks to the rim
        double depth = R * 0.42, rimH = R * 0.22, peak = R >= 11 ? R * 0.35 : 0;
        int reach = Math.min(16, R + 4);
        for (int dx = -reach; dx <= reach; dx++)
            for (int dz = -reach; dz <= reach; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / R;
                if (d > reach / (double) R) continue;
                int x = cx + dx, z = cz + dz;
                int g = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                if (!level.getFluidState(new BlockPos(x, g, z)).isEmpty()) continue;
                // the profile: a bowl inside, a rim falling away outside, a peak in the middle of the big ones
                double h;
                if (d < 1) h = -depth * (1 - d * d) + rimH * Math.exp(-Math.pow((d - 1) / 0.18, 2));
                else h = rimH * Math.exp(-Math.pow((d - 1) / 0.25, 2));
                if (peak > 0 && d < 0.22) h += peak * (1 - d / 0.22);
                h += (rand.nextDouble() - 0.5) * 0.6;
                int top = (int) Math.round(base + h);
                if (d >= 1 && top <= g) continue;                         // outside the rim it only ever builds up
                for (int y = Math.max(top + 1, level.getMinBuildHeight() + 6); y <= Math.max(g, top) + 2; y++)
                    setBlock(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                for (int y = Math.min(g, top) - 1; y <= top; y++)
                    setBlock(level, new BlockPos(x, y, z), ice);
                boolean floor = d < 0.92 && !(peak > 0 && d < 0.18);
                if (floor || rand.nextInt(4) == 0) {
                    setBlock(level, new BlockPos(x, top, z), sand);
                    if (floor && d < 0.7) setBlock(level, new BlockPos(x, top - 1, z), sand);
                }
            }
        return true;
    }
}
