package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.block.ArborHeartwoodBlock;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Carves this chunk's slice of the World Tree. Placed once per chunk; it only ever writes inside its own
 * chunk, reading the precomputed {@link WorldTree} geometry, so neighbouring chunks line up exactly.
 */
public class WorldTreeFeature extends Feature<NoneFeatureConfiguration> {
    public WorldTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int minY = level.getMinBuildHeight() + 1, maxY = level.getMaxBuildHeight() - 1;
        BlockState wood = QHBlocks.get("arbor_heartwood").defaultBlockState();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        boolean any = false;

        for (WorldTree.Seg s : WorldTree.segmentsNear(x0, z0)) {
            int ax = Math.max(x0, (int) Math.floor(Math.min(s.x0(), s.x1()) - Math.max(s.r0(), s.r1())));
            int bx = Math.min(x0 + 15, (int) Math.ceil(Math.max(s.x0(), s.x1()) + Math.max(s.r0(), s.r1())));
            int az = Math.max(z0, (int) Math.floor(Math.min(s.z0(), s.z1()) - Math.max(s.r0(), s.r1())));
            int bz = Math.min(z0 + 15, (int) Math.ceil(Math.max(s.z0(), s.z1()) + Math.max(s.r0(), s.r1())));
            if (ax > bx || az > bz) continue;
            int ay = Math.max(minY, (int) Math.floor(s.minY())), by = Math.min(maxY, (int) Math.ceil(s.maxY()));
            for (int x = ax; x <= bx; x++)
                for (int z = az; z <= bz; z++)
                    for (int y = ay; y <= by; y++) {
                        if (!WorldTree.inside(s, x + 0.5, y + 0.5, z + 0.5)) continue;
                        m.set(x, y, z);
                        level.setBlock(m, wood.setValue(ArborHeartwoodBlock.PHASE, phaseAt(x, y, z)), 2);
                        any = true;
                    }
        }

        // the arrival collar: a broad ring around the trunk just above the ocean, where travellers land
        for (int x = x0; x < x0 + 16; x++)
            for (int z = z0; z < z0 + 16; z++) {
                double d = Math.sqrt(x * x + (double) z * z);
                if (d >= WorldTree.COLLAR_INNER && d <= WorldTree.COLLAR_OUTER) {
                    m.set(x, WorldTree.COLLAR_Y, z);
                    level.setBlock(m, wood.setValue(ArborHeartwoodBlock.PHASE, phaseAt(x, WorldTree.COLLAR_Y, z)), 2);
                    any = true;
                }
            }

        BlockState bubble = QHBlocks.get("bubble_universe").defaultBlockState();
        for (WorldTree.Bubble b : WorldTree.bubblesNear(x0, z0)) {
            if (b.x() < x0 || b.x() > x0 + 15 || b.z() < z0 || b.z() > z0 + 15) continue;
            if (b.y() <= minY || b.y() + b.stalk() >= maxY) continue;
            for (int k = 1; k <= b.stalk(); k++) level.setBlock(m.set(b.x(), b.y() + k, b.z()), wood, 2);
            level.setBlock(m.set(b.x(), b.y(), b.z()), bubble, 2);
            any = true;
        }
        return any;
    }

    /** Mostly ordinary heartwood, with veins of the stranger phases running through it. */
    private static int phaseAt(int x, int y, int z) {
        long h = x * 3129871L ^ z * 116129781L ^ y * 0x5DEECE66DL;
        h = h * h * 42317861L + h * 11L;
        int v = (int) ((h >> 16) & 63);
        return v < 52 ? 0 : v < 57 ? 1 : v < 61 ? 2 : 3;
    }
}
