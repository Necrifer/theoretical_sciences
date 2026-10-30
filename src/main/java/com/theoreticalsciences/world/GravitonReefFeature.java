package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A Graviton Reef colony: branching crystal coral that grows along field lines, studded with dilaton polyps. */
public class GravitonReefFeature extends Feature<NoneFeatureConfiguration> {
    public GravitonReefFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (y <= level.getMinBuildHeight() + 5) return false;
        BlockPos base = new BlockPos(o.getX(), y, o.getZ());
        if (!level.getBlockState(base.below()).isSolid()) return false;
        BlockState crystal = QHBlocks.get("bulk_crystal").defaultBlockState();
        BlockState polyp = QHBlocks.get("dilaton_ore").defaultBlockState();
        BlockState kk = QHBlocks.get("kaluza_klein_ore").defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                if (r.nextInt(3) == 0) setBlock(level, base.offset(dx, -1, dz), kk);
        int branches = 3 + r.nextInt(4);
        for (int b = 0; b < branches; b++) grow(level, r, base, 8 + r.nextInt(10), crystal, polyp, 0);
        com.theoreticalsciences.world.zones.StructureZones.mark(level, base.above(6), 12, com.theoreticalsciences.world.zones.StructureZones.Mode.LENS);
        return true;
    }

    private void grow(WorldGenLevel level, RandomSource r, BlockPos start, int len, BlockState crystal, BlockState polyp, int depth) {
        double x = start.getX() + 0.5, y = start.getY(), z = start.getZ() + 0.5;
        double dx = (r.nextDouble() - 0.5) * 0.8, dz = (r.nextDouble() - 0.5) * 0.8;
        for (int i = 0; i < len; i++) {
            x += dx + (r.nextDouble() - 0.5) * 0.4;
            z += dz + (r.nextDouble() - 0.5) * 0.4;
            y += 0.8 + r.nextDouble() * 0.4;
            BlockPos p = BlockPos.containing(x, y, z);
            if (!level.isEmptyBlock(p)) continue;
            setBlock(level, p, r.nextInt(6) == 0 ? polyp : crystal);
            if (depth < 2 && r.nextInt(5) == 0) grow(level, r, p, len / 2, crystal, polyp, depth + 1);
        }
    }
}
