package com.theoreticalsciences.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A Containment Cache: the sealed supply locker of the mod's ruins and vaults, in the look of the place it was found
 * (each dimension has its own). Holds 27 stacks, opens with a hiss of its containment field, works with hoppers and
 * pipes. Craftable, too.
 */
public class CacheBlock extends BaseEntityBlock {
    public static final MapCodec<CacheBlock> CODEC = simpleCodec(CacheBlock::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public CacheBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource r) {
        if (level.getBlockEntity(pos) instanceof CacheBlockEntity c) c.recheckOpen();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CacheBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof CacheBlockEntity be) {
            player.openMenu(be);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CacheBlockEntity be) {
            Containers.dropContents(level, pos, be);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CacheBlockEntity be ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        // the seal glimmers; open, it vents
        if (r.nextInt(state.getValue(OPEN) ? 1 : 6) == 0)
            level.addParticle(state.getValue(OPEN) ? com.theoreticalsciences.registry.QHParticles.PHOTON.get() : com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(),
                    pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.02, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
    }
}
