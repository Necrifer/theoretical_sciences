package com.theoreticalsciences.transcend;

import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The heart of Omnipotency. It exists only in the Transcendental, cannot be broken or obtained, and makes whatever
 * it is asked for. (Anywhere else it is inert.)
 */
public class OmnipotenceCoreBlock extends Block {
    public OmnipotenceCoreBlock(Properties p) {
        super(p);
    }

    /** The Hand of the Demiurge says it cannot work on this Core (instead of the screen opening). */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                                 net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof com.theoreticalsciences.endgame.DemiurgeHandItem) return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.dimension() != QHDimensions.TRANSCENDENTAL) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.theoreticalsciences.core_inert").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // the server opens the screen, so it can tell the client what state the Core is in
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) CorePayloads.open(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 3; i++) {
            double a = r.nextDouble() * Math.PI * 2, rad = 1.5 + r.nextDouble();
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5 + Math.cos(a) * rad, pos.getY() + 0.5 + r.nextGaussian() * 0.5,
                    pos.getZ() + 0.5 + Math.sin(a) * rad, -Math.cos(a) * 0.08, 0, -Math.sin(a) * 0.08);
        }
    }
}
