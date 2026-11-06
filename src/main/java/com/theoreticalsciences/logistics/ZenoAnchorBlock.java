package com.theoreticalsciences.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/** 1.2.0: the Zeno Anchor (see {@link ZenoAnchorBlockEntity}). Right-click to read its state. */
public class ZenoAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<ZenoAnchorBlock> CODEC = simpleCodec(ZenoAnchorBlock::new);

    public ZenoAnchorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.LIT);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ZenoAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof ZenoAnchorBlockEntity z) z.serverTick();
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moving) {
        if (!state.is(now.getBlock()) && level instanceof ServerLevel sl) ZenoAnchorBlockEntity.release(sl, pos);
        super.onRemove(state, level, pos, now, moving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ZenoAnchorBlockEntity be)
            player.displayClientMessage(Component.translatable(be.holding() ? "message.theoreticalsciences.zeno_anchor.on" : "message.theoreticalsciences.zeno_anchor.off",
                    String.format("%,d", be.energy.getEnergyStored()), ZenoAnchorBlockEntity.COST, ZenoAnchorBlockEntity.RADIUS * 2 + 1)
                    .withStyle(be.holding() ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
