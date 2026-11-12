package com.theoreticalsciences.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The light inside a ring of Homeward Portal Frames. Stepping into it goes to Omnipotency, or from there back again. */
public class HomePortalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 6, 0, 16, 12, 16);

    public HomePortalBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (e instanceof ServerPlayer sp && !sp.isOnPortalCooldown() && !sp.isPassenger()) {
            sp.setPortalCooldown(100);
            HomeTravel.through(sp);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + r.nextDouble(), pos.getY() + 0.8, pos.getZ() + r.nextDouble(), 0, 0.03 + r.nextDouble() * 0.04, 0);
    }
}
