package com.theoreticalsciences.logistics;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stores energy far beyond the usual 2.1 billion FE. Every face takes energy in except the marked output face, which
 * pushes it out (and lets cables / transceivers pull it). Placed against a block, the output faces that block.
 * Keeps its charge when broken.
 */
public class EnergyCellBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);

    public EnergyCellBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, QHLogistics.ENERGY_CELL.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyCellBlockEntity be)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.cell", String.format("%,d", be.energy.stored()),
                    String.format("%,d", be.energy.capacity()), String.format("%,d", be.rate())).withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState s) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EnergyCellBlockEntity be ? (int) Math.round(15.0 * be.energy.stored() / Math.max(1, be.energy.capacity())) : 0;
    }
}
