package com.theoreticalsciences.deeptime;

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
 * 1.3.0: the Overworld's two new ruins, both rare and only in newly generated chunks. Like the 1.0 ruins they are built
 * from the mod's own blocks and keep a Containment Cache of the mod's own loot.
 */
public final class DeepTimeFeatures {

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    /** True if the ground under a w-by-w square is solid and roughly level (no more than 2 blocks of relief). */
    static int flatGround(WorldGenLevel level, BlockPos o, int r) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += r)
            for (int dz = -r; dz <= r; dz += r) {
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX() + dx, o.getZ() + dz);
                // the worldgen heightmap only exists while a chunk generates; placed by command, read the live one
                if (y <= level.getMinBuildHeight() + 1) y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, o.getX() + dx, o.getZ() + dz);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        return hi - lo <= 2 ? lo - 1 : Integer.MIN_VALUE;
    }

    /**
     * An abandoned palaeontological dig: a stepped pit cut into the ground, a survey grid over it, plaster-jacketed
     * bones waiting to be lifted, fossil and amber showing in the walls, and the team's cache.
     */
    public static class DigSite extends Feature<NoneFeatureConfiguration> {
        public DigSite(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = flatGround(level, o, 5);
            if (y == Integer.MIN_VALUE) return false;
            BlockPos c = new BlockPos(o.getX(), y, o.getZ());
            if (!level.getBlockState(c).isSolid() || !level.getFluidState(c.above()).isEmpty()) return false;
            BlockState grid = b("excavation_grid"), fossil = b("fossil_ore"), amber = b("amber_ore"), jacket = b("plaster_jacket");
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -5; dz <= 5; dz++) {
                    int edge = Math.max(Math.abs(dx), Math.abs(dz));
                    int depth = edge <= 2 ? 3 : edge <= 4 ? 2 : 0;          // a stepped pit: deeper in the middle
                    for (int k = 0; k < depth; k++) setBlock(level, c.offset(dx, -k, dz), Blocks.AIR.defaultBlockState());
                    for (int k = 1; k <= 5; k++) {
                        BlockPos above = c.offset(dx, k, dz);
                        if (!level.getBlockState(above).isAir() && !level.getBlockState(above).is(net.minecraft.tags.BlockTags.LOGS))
                            setBlock(level, above, Blocks.AIR.defaultBlockState());
                    }
                    BlockPos floor = c.offset(dx, -depth, dz);
                    if (depth > 0 && r.nextFloat() < 0.18F) setBlock(level, floor, r.nextFloat() < 0.75F ? fossil : amber);   // the bone bed
                    if (edge == 5 && (dx + dz) % 2 == 0) {
                        setBlock(level, c.offset(dx, 1, dz), grid);          // survey stakes round the rim
                        if (Math.abs(dx) == 5 && Math.abs(dz) == 5) setBlock(level, c.offset(dx, 2, dz), grid);
                    }
                }
            // jacketed finds, ready to lift, in the deepest part
            for (int i = 0; i < 3; i++) setBlock(level, c.offset(r.nextInt(5) - 2, -2, r.nextInt(5) - 2), jacket);
            // fossils still in the pit walls
            for (int i = 0; i < 6; i++) {
                int side = r.nextInt(4), along = r.nextInt(9) - 4, dep = -r.nextInt(2);
                BlockPos w = switch (side) {
                    case 0 -> c.offset(5, dep - 1, along);
                    case 1 -> c.offset(-5, dep - 1, along);
                    case 2 -> c.offset(along, dep - 1, 5);
                    default -> c.offset(along, dep - 1, -5);
                };
                if (level.getBlockState(w).isSolid()) setBlock(level, w, r.nextFloat() < 0.7F ? fossil : amber);
            }
            com.theoreticalsciences.dungeon.Caches.place(level, c.offset(4, 1, 4), "chests/dig_site", r);
            return true;
        }
    }

    /**
     * A research greenhouse left to the weeds: a cracked glass hall on a Hydroponic Frame, its grow lights dark, its
     * trays still holding soil, and a cache of the seed lines it was breeding.
     */
    public static class GreenhouseRuin extends Feature<NoneFeatureConfiguration> {
        public GreenhouseRuin(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel level = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = flatGround(level, o, 6);
            if (y == Integer.MIN_VALUE) return false;
            BlockPos c = new BlockPos(o.getX(), y, o.getZ());
            if (!level.getBlockState(c).isSolid() || !level.getFluidState(c.above()).isEmpty()) return false;
            BlockState frame = b("hydroponic_frame"), glass = b("containment_field_glass"), tray = b("grow_tray"), light = b("grow_light");
            int hx = 6, hz = 4;                                          // a 13 x 9 hall with a barrel roof
            for (int dx = -hx; dx <= hx; dx++)
                for (int dz = -hz; dz <= hz; dz++) {
                    BlockPos f = c.offset(dx, 0, dz);
                    for (int k = 1; k <= 3; k++) if (!level.getBlockState(f.below(k)).isSolid()) setBlock(level, f.below(k), frame);
                    setBlock(level, f, frame);
                    boolean wall = Math.abs(dz) == hz || Math.abs(dx) == hx;
                    int roof = 4 + (int) Math.round(Math.sqrt(Math.max(0, hz * hz - dz * dz)) * 0.6);
                    for (int h = 1; h <= roof; h++) {
                        BlockPos p = f.above(h);
                        boolean shell = wall || h == roof;
                        if (shell) {
                            if (h > 2 && r.nextFloat() < 0.35F) { setBlock(level, p, Blocks.AIR.defaultBlockState()); continue; }   // the cracked panes
                            boolean rib = dx % 3 == 0 || h == 1;
                            setBlock(level, p, rib ? frame : glass);
                        } else {
                            setBlock(level, p, Blocks.AIR.defaultBlockState());
                        }
                    }
                    // two benches of trays down the hall, lights hanging over them
                    if (!wall && (dz == -2 || dz == 2) && Math.abs(dx) <= hx - 2) {
                        setBlock(level, f.above(1), tray);
                        if (dx % 2 == 0 && r.nextFloat() < 0.7F) setBlock(level, f.above(roof - 1 > 3 ? 4 : 3), light);
                    }
                }
            // the door gap
            for (int h = 1; h <= 2; h++) setBlock(level, c.offset(0, h, -hz), Blocks.AIR.defaultBlockState());
            com.theoreticalsciences.dungeon.Caches.place(level, c.offset(hx - 2, 1, 0), "chests/greenhouse_ruin", r);
            return true;
        }
    }

    private DeepTimeFeatures() {}
}
