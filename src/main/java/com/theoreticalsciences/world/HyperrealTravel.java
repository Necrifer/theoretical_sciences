package com.theoreticalsciences.world;

import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Moving players into and out of Hyperdimensional Reality Space. */
public final class HyperrealTravel {
    /** Sends the player to the arrival collar around the World Tree's trunk. */
    public static boolean enter(ServerPlayer sp) {
        ServerLevel target = sp.server.getLevel(QHDimensions.HYPERREAL);
        if (target == null) {
            sp.displayClientMessage(Component.literal("Hyperdimensional Reality Space is missing (datapack disabled?)").withStyle(ChatFormatting.RED), true);
            return false;
        }
        // round 27: the tesseract animation plays for 1.75 s first, then the same arrival as before
        return com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.HYPERREAL, false, p -> enterNow(p, target));
    }

    /** The actual arrival (unchanged from before round 27, only run after the animation). */
    private static void enterNow(ServerPlayer sp, ServerLevel target) {
        double a = sp.getRandom().nextDouble() * Math.PI * 2;
        int r = (WorldTree.COLLAR_INNER + WorldTree.COLLAR_OUTER) / 2;
        BlockPos dest = new BlockPos((int) Math.round(Math.cos(a) * r), WorldTree.COLLAR_Y + 1, (int) Math.round(Math.sin(a) * r));
        target.getChunk(dest);
        if (!target.getBlockState(dest.below()).isSolid()) {
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++)
                    target.setBlockAndUpdate(dest.offset(dx, -1, dz), QHBlocks.get("arbor_heartwood").defaultBlockState());
        }
        com.theoreticalsciences.world.SafeLanding.clear(target, dest);
        go(sp, target, dest, (float) Math.toDegrees(a) + 90F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.breach_arrival").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /** Back to the player's respawn point (or world spawn) in the Overworld. */
    public static void leave(ServerPlayer sp) {
        // round 27: the "leaving" animation of wherever the player is (Hyperdimensional Reality, or another of our
        // dimensions a Return Rift stands in) plays for 1.75 s first, then the same trip home as before
        var here = sp.level().dimension();
        com.theoreticalsciences.world.transit.Transit.start(sp, com.theoreticalsciences.world.transit.Transit.ours(here) ? here : QHDimensions.HYPERREAL, true,
                HyperrealTravel::leaveNow);
    }

    /** The actual trip home (unchanged from before round 27). */
    private static void leaveNow(ServerPlayer sp) {
        ServerLevel ow = sp.server.overworld();
        BlockPos spawn = ow.getSharedSpawnPos();
        go(sp, ow, com.theoreticalsciences.world.SafeLanding.find(ow, spawn.getX(), spawn.getZ(), spawn.getY(), null, null), sp.getYRot());
    }

    private static void go(ServerPlayer sp, ServerLevel target, BlockPos dest, float yaw) {
        ((ServerLevel) sp.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
        ((ServerLevel) sp.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 120, 0.6, 1.0, 0.6, 0.25);
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, yaw, sp.getXRot(), DimensionTransition.DO_NOTHING));
        target.sendParticles(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 200, 1.0, 1.5, 1.0, 1.0);
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    private HyperrealTravel() {}
}
