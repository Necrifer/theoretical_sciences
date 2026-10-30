package com.theoreticalsciences.extreme;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Autonomous Extreme Assembler: see {@link AutoAssemblerBlockEntity}. Use it to see what it is doing. */
public class AutoAssemblerBlock extends BaseEntityBlock {
    public static final MapCodec<AutoAssemblerBlock> CODEC = simpleCodec(AutoAssemblerBlock::new);

    public AutoAssemblerBlock(Properties p) {
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
        return new AutoAssemblerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ExtremeRegistry.AUTO_ASSEMBLER_BE.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AutoAssemblerBlockEntity be) {
            int n = 0;
            for (int i = 0; i < AutoAssemblerBlockEntity.IN; i++) n += be.input.getStackInSlot(i).getCount();
            player.displayClientMessage(be.describe().copy().withStyle(ChatFormatting.AQUA)
                    .append(Component(" (" + n + ")")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static net.minecraft.network.chat.Component Component(String s) {
        return net.minecraft.network.chat.Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof AutoAssemblerBlockEntity be) be.dropAll();
        super.onRemove(state, level, pos, next, moving);
    }
}
