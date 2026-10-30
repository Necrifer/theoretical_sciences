package com.theoreticalsciences.gr;

import com.theoreticalsciences.item.BraneShifterItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The throat of an Einstein-Rosen bridge. Step into it inside the black hole and you come out inside the white hole
 * (at the same x and z), and the other way round. One stands at the centre of each hole; more can be built.
 */
public class EinsteinRosenBridgeBlock extends Block {
    public EinsteinRosenBridgeBlock(Properties p) {
        super(p);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (!(e instanceof ServerPlayer sp) || sp.isOnPortalCooldown() || !(level instanceof ServerLevel sl)) return;
        var other = sl.dimension() == QHDimensions.BLACK_HOLE ? QHDimensions.WHITE_HOLE : sl.dimension() == QHDimensions.WHITE_HOLE ? QHDimensions.BLACK_HOLE : null;
        if (other == null) return;
        ServerLevel target = sp.server.getLevel(other);
        if (target == null || com.theoreticalsciences.world.transit.Transit.busy(sp)) return;
        sp.setPortalCooldown();
        // round 27: the bridge animation (collapse into the black hole / burst out of the white hole) plays for 1.75 s,
        // then the same jump as before
        final ServerLevel to = target;
        com.theoreticalsciences.world.transit.Transit.start(sp, other, false, p -> cross(p, pos, sl, to, other));
    }

    /** The actual crossing (unchanged from before round 27, only run after the animation). */
    private static void cross(ServerPlayer sp, BlockPos pos, ServerLevel sl, ServerLevel target, net.minecraft.resources.ResourceKey<Level> other) {
        sp.setPortalCooldown();   // again, so the bridge at the far end does not throw the player straight back
        BridgeBuilder.ensureBuilt(target);
        BlockPos dest = pos.getX() == 0 && pos.getZ() == 0 || Math.abs(pos.getX()) <= 2 && Math.abs(pos.getZ()) <= 2
                ? BridgeBuilder.ARRIVAL : BraneShifterItem.findLanding(target, pos, other == QHDimensions.BLACK_HOLE ? "infall_rock" : "ejecta_rock", "einstein_rosen_bridge");
        sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 100, 0.5, 1, 0.5, 0.3);
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(), DimensionTransition.DO_NOTHING));
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.4F, 1.6F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 3; i++)
            level.addParticle(level.dimension() == QHDimensions.WHITE_HOLE ? com.theoreticalsciences.registry.QHParticles.PHOTON.get() : com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(),
                    pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0.03, 0);
    }
}
