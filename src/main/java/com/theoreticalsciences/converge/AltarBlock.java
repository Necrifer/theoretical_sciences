package com.theoreticalsciences.converge;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AltarBlock extends BaseEntityBlock {
    public static final com.mojang.serialization.MapCodec<AltarBlock> CODEC = simpleCodec(AltarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 10, 14), Block.box(1, 10, 1, 15, 13, 15));

    public AltarBlock(Properties p) {
        super(p);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, QHLogistics.CONV_ALTAR.get(), level.isClientSide ? (l, p, s, be) -> be.clientTick() : (l, p, s, be) -> be.serverTick());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AltarBlockEntity be) {
            if (by instanceof Player p) be.setOwner(p.getUUID());
            be.setPowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.isEmpty() || !(level.getBlockEntity(pos) instanceof AltarBlockEntity be) || be.crafting() || !be.core().isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            be.items.setStackInSlot(0, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (be.crafting()) {
            if (player instanceof ServerPlayer sp) be.status(sp);
        } else if (!be.output().isEmpty()) {
            ItemStack s = be.items.extractItem(1, 64, false);
            if (!player.getInventory().add(s)) player.drop(s, false);
        } else if (player.isShiftKeyDown() && !be.core().isEmpty()) {
            ItemStack s = be.items.extractItem(0, 1, false);
            if (!player.getInventory().add(s)) player.drop(s, false);
        } else be.start(player);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block from, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AltarBlockEntity be) be.redstone(level.hasNeighborSignal(pos));
        super.neighborChanged(state, level, pos, from, fromPos, moving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState s) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AltarBlockEntity be ? be.comparator() : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moved) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof AltarBlockEntity be) {
            for (BlockPos p : be.usedPedestals()) if (level.getBlockEntity(p) instanceof PedestalBlockEntity pe) pe.lockedUntil = 0;
            for (int i = 0; i < 2; i++) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, be.items.getStackInSlot(i));
        }
        super.onRemove(state, level, pos, now, moved);
    }

    /** Round 29: breaking it keeps the stored energy in the dropped item (see KeepContents). */
    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.theoreticalsciences.block.KeepContents.drops(this, super.getDrops(state, params), params, AltarBlockEntity.class, AltarBlockEntity::keptData);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        com.theoreticalsciences.block.KeepContents.creative(this, level, pos, player, AltarBlockEntity.class, AltarBlockEntity::keptData);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
