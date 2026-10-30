package com.theoreticalsciences.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** The furniture of the Living Proof. All of it is unbreakable. */
public final class ProofBlocks {
    /** Touch it to give the answer written above it. */
    public static class Pedestal extends Block {
        public Pedestal(Properties p) {
            super(p);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) ProofArena.onPedestal(sp, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** A binary digit you can flip (the Diagonal). */
    public static class Toggle extends Block {
        public Toggle(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(BlockStateProperties.LIT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(BlockStateProperties.LIT), 3);
                if (level instanceof net.minecraft.server.level.ServerLevel sl) ProofArena.onToggle(sl, pos);
                level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.UI_CLICK.get(), SoundSource.BLOCKS, 0.6F, state.getValue(BlockStateProperties.LIT) ? 0.6F : 1.2F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** A binary digit written into the wall (the Diagonal); it only displays. */
    public static class Lamp extends Block {
        public Lamp(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(BlockStateProperties.LIT);
        }
    }

    private ProofBlocks() {}
}
