package com.theoreticalsciences.transcend;

import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Into the Transcendental. The first arrival builds it. */
public final class TranscendentalTravel {
    public static boolean enter(ServerPlayer sp) {
        ServerLevel target = sp.server.getLevel(QHDimensions.TRANSCENDENTAL);
        if (target == null) {
            sp.displayClientMessage(Component.literal("The Transcendental dimension is missing (datapack disabled?)").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Component closed = Omnipotence.closedReason(sp.server);
        if (closed != null) {
            sp.displayClientMessage(closed, true);
            return false;
        }
        // round 27: the ascension animation plays for 1.75 s first, then the same arrival as before
        return com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.TRANSCENDENTAL, false, p -> enterNow(p, target));
    }

    /** The actual arrival (unchanged from before round 27, only run after the animation). */
    private static void enterNow(ServerPlayer sp, ServerLevel target) {
        Component closed = Omnipotence.closedReason(sp.server);
        if (closed != null) {               // it closed during the animation
            sp.displayClientMessage(closed, true);
            com.theoreticalsciences.world.transit.Transit.cancel(sp);
            return;
        }
        TranscendentalBuilder.ensureBuilt(target);
        BlockPos dest = com.theoreticalsciences.world.SafeLanding.clear(target, TranscendentalBuilder.ARRIVAL);
        ((ServerLevel) sp.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
        // round 35: the intended way in - the only one (world/DimensionGate turns any other away)
        com.theoreticalsciences.world.DimensionGate.allow(sp, QHDimensions.TRANSCENDENTAL);
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, 180F, 0F, DimensionTransition.DO_NOTHING));
        com.theoreticalsciences.world.DimensionGate.revoke(sp);
        target.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 300, 2, 2, 2, 0.3);
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.5F, 0.4F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.transcendental_arrival").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
    }

    private TranscendentalTravel() {}
}
