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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A crashed, half-buried research observatory: a cracked containment dome of field glass on a
 * spacetime-casing ring, a broken field emitter, Cherenkov quartz shards and a supply cache.
 */
public class ObservatoryRuinFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/observatory_ruin"));

    public ObservatoryRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos origin = ctx.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos c = new BlockPos(origin.getX(), y, origin.getZ());
        if (!level.getBlockState(c).isSolid()) return false;
        BlockState casing = QHBlocks.get("spacetime_casing").defaultBlockState();
        BlockState glass = QHBlocks.get("containment_field_glass").defaultBlockState();
        int r = 5;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) continue;
                BlockPos floor = c.offset(dx, 0, dz);
                // foundation
                for (int k = 1; k <= 3; k++) if (!level.getBlockState(floor.below(k)).isSolid()) setBlock(level, floor.below(k), casing);
                if (rand.nextFloat() < 0.85F) setBlock(level, floor, rand.nextFloat() < 0.15F ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : casing);
                // dome
                for (int h = 1; h <= r; h++) {
                    double shell = Math.sqrt(dx * dx + dz * dz + h * h * 1.4);
                    BlockPos p = floor.above(h);
                    if (Math.abs(shell - r) < 0.6) {
                        if (h > 2 && rand.nextFloat() < 0.45F) continue; // cracked
                        setBlock(level, p, h <= 1 ? casing : glass);
                    } else if (shell < r - 0.6) {
                        setBlock(level, p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        // broken emitter and quartz
        setBlock(level, c.above(1), QHBlocks.get("field_emitter").defaultBlockState());
        for (int i = 0; i < 6; i++) {
            BlockPos q = c.offset(rand.nextInt(7) - 3, 1, rand.nextInt(7) - 3);
            if (level.getBlockState(q).isAir()) setBlock(level, q, QHBlocks.get("cherenkov_quartz_block").defaultBlockState());
        }
        // cache
        BlockPos chest = c.offset(2, 1, 0);
        com.theoreticalsciences.dungeon.Caches.place(level, chest, LOOT, rand);
        // the observatory's instrument never quite shut down
        setBlock(level, c.offset(-2, 1, 0), QHBlocks.get("breached_anomaly_engine").defaultBlockState());
        return true;
    }
}
