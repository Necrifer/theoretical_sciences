package com.theoreticalsciences.home;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

/** The Empyrean Assembly Table (1.8.0). Broken, it keeps what it holds inside the item: millions of items cannot be dropped. */
public class EmpyreanTableBlock extends BaseEntityBlock {
    public static final MapCodec<EmpyreanTableBlock> CODEC = simpleCodec(EmpyreanTableBlock::new);

    public EmpyreanTableBlock(Properties props) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmpyreanTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, HomeRegistry.TABLE_BE.get(), EmpyreanTableBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player p && level.getBlockEntity(pos) instanceof EmpyreanTableBlockEntity be) be.setOwner(p.getUUID());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof EmpyreanTableBlockEntity be)
            sp.openMenu(be, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof EmpyreanTableBlockEntity be) {
            // 1.8.2: one item comes out, whatever the table held. The contents are a few counts written on that item
            // (never stacks, never item entities), so breaking a full table cannot flood a chunk.
            ItemStack drop = new ItemStack(this);
            boolean full = be.holdsAnything();
            if (full) be.saveToItem(drop, level.registryAccess());
            net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            ie.setDefaultPickUpDelay();
            if (full) {                      // and that one item cannot be lost: it never despawns and nothing destroys it
                ie.setUnlimitedLifetime();
                ie.setInvulnerable(true);
            }
            level.addFreshEntity(ie);
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
