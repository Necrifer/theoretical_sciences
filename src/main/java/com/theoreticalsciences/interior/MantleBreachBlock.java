package com.theoreticalsciences.interior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Round 40 (1.3.0): a hole drilled through bedrock. Step into it to pass between the Overworld and the Planetary Interior. */
public class MantleBreachBlock extends Block {
    public MantleBreachBlock(Properties p) {
        super(p);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !(e instanceof ServerPlayer sp) || sp.isOnPortalCooldown()) return;
        sp.setPortalCooldown(100);
        InteriorTravel.travel(sp);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 2; i++)
            level.addParticle(r.nextBoolean() ? ParticleTypes.LAVA : ParticleTypes.LARGE_SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 1,
                    pos.getZ() + r.nextDouble(), 0, 0.05, 0);
    }
}
