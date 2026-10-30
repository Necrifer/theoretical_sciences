package com.theoreticalsciences.logistics;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An Entangled Transceiver sits on the face of a machine, tank, chest or energy store. Transceivers tuned to the same
 * frequency (with an Entanglement Tuner) share a channel: senders pull items, fluids and energy out of what they sit
 * on, and receivers push them into what they sit on - instantly, at any distance, even across dimensions.
 */
public class TransceiverBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<TransceiverBlock> CODEC = simpleCodec(TransceiverBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction d : Direction.values()) {
            double x0 = 3, y0 = 3, z0 = 3, x1 = 13, y1 = 13, z1 = 13;
            switch (d) {
                case DOWN -> { y0 = 0; y1 = 6; }
                case UP -> { y0 = 10; y1 = 16; }
                case NORTH -> { z0 = 0; z1 = 6; }
                case SOUTH -> { z0 = 10; z1 = 16; }
                case WEST -> { x0 = 0; x1 = 6; }
                case EAST -> { x0 = 10; x1 = 16; }
            }
            SHAPES[d.ordinal()] = Block.box(x0, y0, z0, x1, y1, z1);
        }
    }

    public TransceiverBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.DOWN).setValue(BlockStateProperties.ENABLED, true));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.FACING, BlockStateProperties.ENABLED);
    }

    /** FACING points at the block it is attached to. ENABLED = sending (true) or receiving (false). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES[s.getValue(BlockStateProperties.FACING).ordinal()];
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransceiverBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHLogistics.TRANSCEIVER.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0)
            level.addParticle(state.getValue(BlockStateProperties.ENABLED) ? com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get() : com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
    }
}
