package com.theoreticalsciences.machine.tuning;

import com.mojang.serialization.MapCodec;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/** 1.2.0: the Governor (see {@link GovernorBlockEntity}). Right-click to read what it holds. */
public class GovernorBlock extends BaseEntityBlock {
    public static final MapCodec<GovernorBlock> CODEC = simpleCodec(GovernorBlock::new);

    public GovernorBlock(Properties p) {
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
        return new GovernorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> {
            if (be instanceof GovernorBlockEntity g) g.serverTick();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GovernorBlockEntity g) {
            ControllerBlockEntity c = g.machine();
            Component msg;
            if (c == null) msg = Component.translatable("message.theoreticalsciences.governor.none").withStyle(ChatFormatting.GRAY);
            else if (g.energy.getEnergyStored() < GovernorBlockEntity.COST)
                msg = Component.translatable("message.theoreticalsciences.governor.no_power", GovernorBlockEntity.COST).withStyle(ChatFormatting.GRAY);
            else msg = Component.translatable(g.holding() ? "message.theoreticalsciences.governor.holding" : "message.theoreticalsciences.governor.seeking",
                    c.getBlockState().getBlock().getName(), String.format("%.0f", c.precision.value * 100)).withStyle(g.holding() ? ChatFormatting.AQUA : ChatFormatting.GOLD);
            player.displayClientMessage(msg, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
