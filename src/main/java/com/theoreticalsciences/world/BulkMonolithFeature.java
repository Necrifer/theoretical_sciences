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
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/** A hyperframe monolith - the 3D cross-section of something much larger - with a vault at its base. */
public class BulkMonolithFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/bulk_monolith"));

    public BulkMonolithFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos o = ctx.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (y <= level.getMinBuildHeight() + 5) return false;
        BlockPos base = new BlockPos(o.getX(), y, o.getZ());
        if (!level.getBlockState(base.below()).isSolid()) return false;
        BlockState frame = QHBlocks.get("hyperframe_casing").defaultBlockState();
        BlockState crystal = QHBlocks.get("bulk_crystal").defaultBlockState();
        BlockState stone = QHBlocks.get("bulk_stone").defaultBlockState();
        int height = 14 + rand.nextInt(10);
        for (int h = 0; h < height; h++) {
            int w = h < 3 ? 2 : 1;
            for (int dx = -w; dx <= w; dx++)
                for (int dz = -w; dz <= w; dz++) {
                    boolean edge = Math.abs(dx) == w && Math.abs(dz) == w;
                    BlockState s = edge || h % 5 == 0 ? frame : (dx == 0 && dz == 0 ? crystal : stone);
                    setBlock(level, base.offset(dx, h, dz), s);
                }
        }
        // floating crown: a tesseract-like nested frame
        BlockPos top = base.above(height + 3);
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    int edges = (Math.abs(dx) == 2 ? 1 : 0) + (Math.abs(dy) == 2 ? 1 : 0) + (Math.abs(dz) == 2 ? 1 : 0);
                    if (edges >= 2) setBlock(level, top.offset(dx, dy, dz), frame);
                }
        setBlock(level, top, crystal);
        // vault
        BlockPos chest = base.offset(0, 1, 0);
        setBlock(level, base, frame);
        com.theoreticalsciences.dungeon.Caches.place(level, chest, LOOT, rand);
        com.theoreticalsciences.world.zones.StructureZones.mark(level, base.above(height / 2 + 2), height / 2 + 7, com.theoreticalsciences.world.zones.StructureZones.Mode.TESSERACT);
        return true;
    }
}
