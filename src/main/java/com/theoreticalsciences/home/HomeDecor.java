package com.theoreticalsciences.home;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.9.0: the three things in Omnipotency's buildings that are more than stone: the urn, the bell, and the stone that can be read. */
public final class HomeDecor {
    private HomeDecor() {}

    /** How many remembrances there are (the stones speak them; the tablets in the urns carry them). */
    public static final int REMEMBRANCES = 12;

    /** A sealed urn. Whatever was put by in it comes out when it is broken, and breaking it is noticed. */
    public static class Urn extends Block {
        private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(3, 2, 3, 13, 11, 13), Block.box(5, 11, 5, 11, 14, 11), Block.box(4, 14, 4, 12, 16, 12));

        public Urn(Properties props) {
            super(props);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }
    }

    /** A bell. Used, it rings one clear note; each bell has its own, and a row of them is a scale. */
    public static class Bell extends Block {
        private static final VoxelShape SHAPE = Shapes.or(Block.box(7, 13, 7, 9, 16, 9), Block.box(5, 9, 5, 11, 13, 11), Block.box(4, 4, 4, 12, 9, 12), Block.box(3, 2, 3, 13, 4, 13));
        private static final float[] SCALE = {0.5F, 0.561F, 0.63F, 0.749F, 0.841F, 1.0F, 1.122F, 1.26F, 1.498F, 1.682F};      // a pentatonic scale, two octaves

        public Bell(Properties props) {
            super(props);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel sl) {
                float pitch = SCALE[Math.floorMod(pos.getX() * 31 + pos.getY() * 17 + pos.getZ() * 7, SCALE.length)];
                sl.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, pitch);
                sl.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.5F, pitch);
                sl.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 1, 0, 0, 0, pitch);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** A standing stone with something cut into it. Used, it is read to you; which of the twelve depends on where it stands. */
    public static class Stone extends Block {
        public Stone(Properties props) {
            super(props);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide) {
                int n = Math.floorMod(pos.getX() * 13 + pos.getZ() * 5 + pos.getY(), REMEMBRANCES) + 1;
                player.sendSystemMessage(Component.translatable("remembrance.theoreticalsciences.title." + n).withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.translatable("remembrance.theoreticalsciences." + n).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
