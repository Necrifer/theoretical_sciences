package com.theoreticalsciences.dungeon;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** Places a dimension's Containment Cache, filled from a loot table, wherever the structures used to put a chest. */
public final class Caches {
    /** the cache style of each dimension (the Overworld's ruins, and anywhere else, get the observatory cache) */
    public static String styleFor(ResourceKey<Level> d) {
        if (d == QHDimensions.BULK) return "bulk_cache";
        if (d == QHDimensions.PLATONIC) return "euclid_reliquary";
        if (d == QHDimensions.FOAM) return "foam_cache";
        if (d == QHDimensions.TRANSFINITE) return "hilbert_locker";
        if (d == QHDimensions.BLACK_HOLE) return "horizon_cache";
        if (d == QHDimensions.WHITE_HOLE) return "ejecta_cache";
        if (d == QHDimensions.HYPERREAL) return "hyperreal_cache";
        if (d == QHDimensions.TRANSCENDENTAL) return "hyperreal_cache";      // (the Transcendent Cache is a storage block now)
        if (d == QHDimensions.ANTIVERSE) return "antiverse_cache";
        return "observatory_cache";
    }

    public static void place(LevelAccessor level, BlockPos p, ResourceKey<LootTable> loot, RandomSource r, String style) {
        Block b = null;
        try {
            b = QHBlocks.get(style);
        } catch (RuntimeException ignored) {
        }
        BlockState st = b == null ? Blocks.CHEST.defaultBlockState() : b.defaultBlockState();
        level.setBlock(p, st, 2);
        net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, r, p, loot);
    }

    /** In the style of the dimension being generated. */
    public static void place(net.minecraft.world.level.WorldGenLevel level, BlockPos p, ResourceKey<LootTable> loot, RandomSource r) {
        place(level, p, loot, r, styleFor(level.getLevel().dimension()));
    }

    public static void place(net.minecraft.world.level.WorldGenLevel level, BlockPos p, String lootPath, RandomSource r) {
        place(level, p, ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id(lootPath)), r);
    }

    private Caches() {}
}
