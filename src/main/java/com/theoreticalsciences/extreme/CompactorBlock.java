package com.theoreticalsciences.extreme;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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

/** Singularity Compactor: swallows thousands of one material and squeezes them into a singularity. No timer. */
public class CompactorBlock extends BaseEntityBlock {
    public static final MapCodec<CompactorBlock> CODEC = simpleCodec(CompactorBlock::new);

    public CompactorBlock(Properties p) {
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
        return new CompactorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ExtremeRegistry.COMPACTOR_BE.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CompactorBlockEntity be && player instanceof ServerPlayer sp)
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        // the absorbed counter is lost - like Avaritia's compressor - but the slots drop
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CompactorBlockEntity be)
            for (int i = 0; i < 2; i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.slots.getStackInSlot(i));
        super.onRemove(state, level, pos, next, moving);
    }
}
