package com.theoreticalsciences.interior;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Round 40 (1.2.0): the way down and back. A Mantle Breach in the Overworld's floor leads into the Planetary Interior at
 * the same x and z, into the crust near its roof; one in the Interior leads back up to the bottom of the Overworld, into
 * a small chamber cut beside the breach. A breach always opens next to where you arrive, so the way back is never lost.
 */
public final class InteriorTravel {
    private InteriorTravel() {}

    /**
     * 1.3.2: a place to stand inside the Interior - under its bedrock roof, never on top of it (the general landing search
     * prefers open sky, which here is only above the roof). Two blocks of air over solid, dry rock, as near y 100 as can
     * be found within 24 blocks; failing that, a small chamber is cut at y 100.
     */
    static BlockPos landing(ServerLevel l, int x, int z) {
        BlockPos best = null;
        int bestScore = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= 24 && best == null; r += 3)
            for (int dx = -r; dx <= r; dx += 3)
                for (int dz = -r; dz <= r; dz += 3) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = x + dx, cz = z + dz;
                    l.getChunk(cx >> 4, cz >> 4);
                    for (int y = 118; y >= 36; y--) {
                        p.set(cx, y, cz);
                        if (!l.getBlockState(p).isAir() || !l.getBlockState(p.above()).isAir()) continue;
                        BlockState under = l.getBlockState(p.below());
                        if (!under.isSolid() || !under.getFluidState().isEmpty() || under.is(Blocks.BEDROCK)) continue;
                        int score = Math.abs(y - 100) + r;
                        if (score < bestScore) {
                            bestScore = score;
                            best = p.immutable();
                        }
                    }
                }
        if (best != null) return best;
        BlockPos c = new BlockPos(x, 100, z);
        BlockState rock = QHBlocks.get("crustal_rock").defaultBlockState();
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                l.setBlockAndUpdate(c.offset(dx, -1, dz), rock);
                for (int dy = 0; dy < 3; dy++) l.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return c;
    }

    /** 1.10.1: through the travel animation (descending through the crust, or rising out of it). */
    public static void travel(ServerPlayer sp) {
        boolean down = sp.level().dimension() != QHDimensions.INTERIOR;
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.INTERIOR, !down, InteriorTravel::travelNow);
    }

    static void travelNow(ServerPlayer sp) {
        boolean down = sp.level().dimension() != QHDimensions.INTERIOR;
        ServerLevel target = sp.server.getLevel(down ? QHDimensions.INTERIOR : Level.OVERWORLD);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest;
        if (down) {
            dest = landing(target, x, z);
        } else {
            // the bottom of the Overworld: a chamber of air in the deepslate just above the bedrock
            int y = target.getMinBuildHeight() + 6;
            dest = new BlockPos(x, y, z);
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    target.setBlockAndUpdate(dest.offset(dx, -1, dz), Blocks.DEEPSLATE.defaultBlockState());
                    for (int dy = 0; dy < 3; dy++) target.setBlockAndUpdate(dest.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
        }
        // the way back: a breach in the floor beside the landing place
        BlockPos back = dest.offset(1, -1, 0);
        BlockState breach = QHBlocks.get("mantle_breach").defaultBlockState();
        if (!target.getBlockState(back).is(breach.getBlock())) target.setBlockAndUpdate(back, breach);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        target.playSound(null, dest, SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, 1F, 0.5F);
        if (down) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.interior_arrival").withStyle(ChatFormatting.GOLD), false);
    }
}
