package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** A whole universe, the size of a fist, hanging from a branch of the World Tree. Its galaxy is drawn by the effect renderer. */
public class BubbleUniverseBlock extends BaseEntityBlock {
    public static final MapCodec<BubbleUniverseBlock> CODEC = simpleCodec(BubbleUniverseBlock::new);
    private static final VoxelShape SHAPE = box(3, 3, 3, 13, 13, 13);

    public BubbleUniverseBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BubbleUniverseBlockEntity(QHBlockEntities.BUBBLE_UNIVERSE.get(), pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.8,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0, 0, 0);
    }
}
