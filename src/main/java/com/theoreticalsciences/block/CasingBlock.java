package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Structural block for multiblocks. When the structure it belongs to is complete, the controller flips
 * {@link #FORMED} on every casing so the whole machine "transforms" into a seamless, energised shell
 * (MMCR-style: the blocks stay real blocks, only their look changes, so nothing can desync or vanish).
 */
public class CasingBlock extends Block {
    public static final MapCodec<CasingBlock> CODEC = simpleCodec(CasingBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /**
     * 1.2.0: part of a moving piece of the formed structure. The block is still there (it collides and drops as ever),
     * but draws nothing, lets light through and hides nothing beside it: the renderer draws it, moving, instead.
     */
    public static final BooleanProperty MOVING = BooleanProperty.create("moving");

    public CasingBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(MOVING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, MOVING);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(MOVING);
    }

    /** A hidden block no formed controller is drawing shows itself again (see MovingClaims). */
    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(MOVING) && !com.theoreticalsciences.multiblock.MovingClaims.claimed(level, pos))
            level.setBlock(pos, state.setValue(MOVING, false), 2);
    }

    static boolean hidden(BlockState s) {
        return s.getValue(MOVING) && s.getValue(FORMED);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return hidden(state) ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return hidden(state) || super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    protected int getLightBlock(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return hidden(state) ? 0 : super.getLightBlock(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return hidden(state) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction dir) {
        return hidden(state) || super.skipRendering(state, adjacent, dir);
    }
}
