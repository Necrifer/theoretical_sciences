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
        // 1.15.0: the narrowband candidate from the water hole; the Core knows where it came from
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(com.theoreticalsciences.TheoreticalSciences.id("narrowband_candidate"))
                && level.dimension() == QHDimensions.TRANSCENDENTAL) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) hearSignal(sp, stack, pos);
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    public static final String HEARD_HOME = "ts_heard_home";

    static void hearSignal(net.minecraft.server.level.ServerPlayer sp, net.minecraft.world.item.ItemStack stack, BlockPos pos) {
        var tag = sp.getPersistentData();
        if (tag.getBoolean(HEARD_HOME)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.core_hears_again").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            return;
        }
        tag.putBoolean(HEARD_HOME, true);
        if (!sp.isCreative()) stack.shrink(1);
        for (int i = 1; i <= 3; i++)
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.core_hears_" + i)
                    .withStyle(i == 3 ? new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC}), false);
        net.minecraft.world.item.ItemStack sig = new net.minecraft.world.item.ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(com.theoreticalsciences.TheoreticalSciences.id("signal_from_home")));
        if (!sp.getInventory().add(sig)) sp.drop(sig, false);
        sp.serverLevel().playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 0.6F);
        sp.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.05);
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
