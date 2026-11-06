package com.theoreticalsciences.science;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 1.2.0: the MOF Gas Storage Cell (see {@link GasCellBlockEntity}). Right-click to read what it holds. */
public class GasCellBlock extends BaseEntityBlock {
    public static final MapCodec<GasCellBlock> CODEC = simpleCodec(GasCellBlock::new);

    public GasCellBlock(Properties p) {
        super(p);
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
        return new GasCellBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GasCellBlockEntity be) {
            var f = be.tank.getFluid();
            player.displayClientMessage(f.isEmpty() ? Component.translatable("message.theoreticalsciences.gas_cell.empty")
                    : Component.translatable("message.theoreticalsciences.gas_cell", f.getHoverName(), String.format("%,d", f.getAmount()),
                    String.format("%,d", GasCellBlockEntity.CAPACITY)).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
