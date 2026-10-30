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

/** Glowing Bulk Crystal spires that grow along brane fault lines. */
public class CrystalSpireFeature extends Feature<NoneFeatureConfiguration> {
    public CrystalSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos pos = ctx.origin();
        while (pos.getY() > level.getMinBuildHeight() + 2 && level.isEmptyBlock(pos)) pos = pos.below();
        if (!level.getBlockState(pos).is(QHBlocks.get("bulk_stone"))) return false;
        BlockState crystal = QHBlocks.get("bulk_crystal").defaultBlockState();
        int h = 3 + rand.nextInt(7);
        double lx = (rand.nextDouble() - 0.5) * 0.6, lz = (rand.nextDouble() - 0.5) * 0.6;
        for (int i = 0; i < h; i++) {
            BlockPos p = pos.offset((int) Math.round(lx * i), i + 1, (int) Math.round(lz * i));
            if (level.isEmptyBlock(p)) setBlock(level, p, crystal);
            if (i < h / 3) for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL)
                if (rand.nextFloat() < 0.4F && level.isEmptyBlock(p.relative(d))) setBlock(level, p.relative(d), crystal);
        }
        return true;
    }
}
