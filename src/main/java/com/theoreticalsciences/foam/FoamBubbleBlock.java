package com.theoreticalsciences.foam;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A bubble of false vacuum. It lasts a minute or two, then pops - and it pops at once if anything stands on it for long. */
public class FoamBubbleBlock extends TransparentBlock {
    public FoamBubbleBlock(Properties p) {
        // never suffocating: otherwise the game shoves anyone passing through out of it, which is what made movement stutter
        super(p.randomTicks().isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) pop(level, pos);
    }

    /**
     * Solid from above, insubstantial from every other side. You can stand and run on a bubble (sneak to sink through it,
     * like scaffolding), but you walk, run or fly straight through it from the side or below - and it pops behind you.
     * It depends only on where you are, which client and server always agree on, so movement stays smooth.
     */
    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                          net.minecraft.world.phys.shapes.CollisionContext ctx) {
        if (ctx instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ec && ec.getEntity() != null) {
            boolean above = ctx.isAbove(net.minecraft.world.phys.shapes.Shapes.block(), pos, false);
            return above && !ec.getEntity().isShiftKeyDown() ? net.minecraft.world.phys.shapes.Shapes.block() : net.minecraft.world.phys.shapes.Shapes.empty();
        }
        return net.minecraft.world.phys.shapes.Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        // passing through a bubble pops it
        if (level instanceof ServerLevel sl && e.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos))) pop(sl, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        // standing on one is borrowed time; running on one even more so
        if (level instanceof ServerLevel sl && sl.random.nextInt(e.isSprinting() ? 12 : 40) == 0) pop(sl, pos);
        super.stepOn(level, pos, state, e);
    }

    public static void pop(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, QHSounds.FOAM_POP.get(), SoundSource.BLOCKS, 0.7F, 0.8F + level.random.nextFloat() * 0.5F);
    }
}
