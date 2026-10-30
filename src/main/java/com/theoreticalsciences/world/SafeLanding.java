package com.theoreticalsciences.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Round 27: where a player arriving in a dimension can safely stand - never inside blocks, never in mid-air, never on
 * top of a dimension's ceiling (the Quantum Foam's Relativity Breakdown, the build limit) and never in a fluid or on
 * something that burns.
 * <p>
 * Every column within a small radius of the arrival point is searched top to bottom for "two free blocks of air over
 * solid ground". A place under the open sky wins (the surface, not a cave); otherwise the one closest to the height
 * the player came from (so a dimension with a roof drops you inside, not on top). Only when there is nowhere at all
 * is a small platform built, with the room above it cleared.
 */
public final class SafeLanding {
    private static final int RADIUS = 24, STEP = 3;

    /** The feet position to arrive at. {@code platform}/{@code core} build the fallback platform (null: obsidian). */
    public static BlockPos find(ServerLevel l, int x, int z, int preferredY, @Nullable Block platform, @Nullable Block core) {
        BlockPos best = null;
        long bestScore = Long.MAX_VALUE;
        // open sky anywhere in the radius beats a roofed spot (a cave, under an overhang) even right underfoot; the
        // search stops at the first ring that has open sky, otherwise takes the roofed spot nearest in height
        for (int r = 0; r <= RADIUS && bestScore >= 1_000_000; r += STEP) {
            for (int dx = -r; dx <= r; dx += STEP)
                for (int dz = -r; dz <= r; dz += STEP) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;     // only this ring
                    int cx = x + dx, cz = z + dz;
                    l.getChunk(cx >> 4, cz >> 4);                                  // generated, so it is real ground
                    long[] sc = new long[1];
                    BlockPos p = column(l, cx, cz, preferredY, sc);
                    if (p == null) continue;
                    long score = sc[0] + r;
                    if (score < bestScore) {
                        bestScore = score;
                        best = p;
                    }
                }
        }
        return best != null ? best : platform(l, x, z, preferredY, platform, core);
    }

    /** The best standing place in one column, or null. */
    @Nullable
    private static BlockPos column(ServerLevel l, int x, int z, int preferredY, long[] scoreOut) {
        int min = l.getMinBuildHeight() + 1, max = l.getMaxBuildHeight() - 3;
        // nothing above the highest solid-or-fluid block can be stood on: start just above it
        max = Math.min(max, l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) + 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        for (int y = max; y >= min; y--) {
            p.set(x, y, z);
            if (!standable(l, p)) continue;
            BlockPos at = p.immutable();
            if (open(l, at)) {                        // the highest open-sky spot: the surface
                scoreOut[0] = 0;
                return at;
            }
            int score = 1_000_000 + 1_000 * Math.abs(y - preferredY);
            if (score < bestScore) {
                bestScore = score;
                best = at;
            }
        }
        scoreOut[0] = bestScore;
        return best;
    }

    /** Two free blocks (no collision, no fluid) over a solid, safe block. */
    public static boolean standable(ServerLevel l, BlockPos feet) {
        if (feet.getY() <= l.getMinBuildHeight() || feet.getY() + 1 >= l.getMaxBuildHeight()) return false;
        return free(l, feet) && free(l, feet.above()) && ground(l, feet.below());
    }

    private static boolean free(ServerLevel l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.getCollisionShape(l, p).isEmpty() && s.getFluidState().isEmpty() && !hazard(s) && !s.is(Blocks.COBWEB);
    }

    private static boolean ground(ServerLevel l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        if (!s.getFluidState().isEmpty() || hazard(s) || s.is(BlockTags.LEAVES)) return false;
        return s.isFaceSturdy(l, p, net.minecraft.core.Direction.UP);
    }

    private static boolean hazard(BlockState s) {
        return s.is(BlockTags.FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.CACTUS) || s.is(Blocks.SWEET_BERRY_BUSH)
                || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.LAVA) || s.is(Blocks.END_PORTAL) || s.is(Blocks.NETHER_PORTAL);
    }

    /** Nothing but air (or leaves, or non-solid fluids like a fluid ceiling's open sky above it is not) up to the build limit. */
    private static boolean open(ServerLevel l, BlockPos feet) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = feet.getY() + 2; y < l.getMaxBuildHeight(); y++) {
            p.set(feet.getX(), y, feet.getZ());
            BlockState s = l.getBlockState(p);
            if (!s.getFluidState().isEmpty()) return false;               // a fluid ceiling is a roof too
            if (!s.getCollisionShape(l, p).isEmpty() && !s.is(BlockTags.LEAVES)) return false;
        }
        return true;
    }

    /** Nowhere to stand: a 5x5 platform at a sensible height, with three blocks of room cleared above it. */
    private static BlockPos platform(ServerLevel l, int x, int z, int preferredY, @Nullable Block platform, @Nullable Block core) {
        int sea = l.getSeaLevel();
        int y = preferredY;
        if (y <= l.getMinBuildHeight() + 4 || y >= l.getMaxBuildHeight() - 8)
            y = sea > l.getMinBuildHeight() + 8 && sea < l.getMaxBuildHeight() - 8 ? sea + 1 : (l.getMinBuildHeight() + l.getMaxBuildHeight()) / 2;
        y = Math.max(l.getMinBuildHeight() + 4, Math.min(l.getMaxBuildHeight() - 8, y));
        BlockPos base = new BlockPos(x, y, z);
        BlockState pl = (platform != null ? platform : Blocks.OBSIDIAN).defaultBlockState();
        BlockState co = (core != null ? core : platform != null ? platform : Blocks.OBSIDIAN).defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                setSafely(l, base.offset(dx, -1, dz), dx == 0 && dz == 0 ? co : pl);
                for (int dy = 0; dy < 3; dy++) setSafely(l, base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return base;
    }

    /** Never replaces an unbreakable block or one holding something (a chest, a machine). */
    private static void setSafely(ServerLevel l, BlockPos p, BlockState to) {
        BlockState s = l.getBlockState(p);
        if (s.getDestroySpeed(l, p) < 0 || l.getBlockEntity(p) != null) return;
        l.setBlockAndUpdate(p, to);
    }

    /** Makes sure a fixed arrival point (a built structure's) has room to stand, clearing what is in the way. */
    public static BlockPos clear(ServerLevel l, BlockPos feet) {
        l.getChunk(feet);
        for (int dy = 0; dy < 2; dy++) {
            BlockPos p = feet.above(dy);
            BlockState s = l.getBlockState(p);
            if (!s.getCollisionShape(l, p).isEmpty() || !s.getFluidState().isEmpty()) l.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        }
        return feet;
    }

    private SafeLanding() {}
}
