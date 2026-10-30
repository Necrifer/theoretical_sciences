package com.theoreticalsciences.transcend;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A tear back down into ordinary reality. Walk into it to return to the Overworld. */
public class ReturnRiftBlock extends Block {
    public ReturnRiftBlock(Properties p) {
        super(p);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (e instanceof ServerPlayer sp && !sp.isOnPortalCooldown()) {
            sp.setPortalCooldown();
            com.theoreticalsciences.world.HyperrealTravel.leave(sp);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 2; i++)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0.05, 0);
    }
}
