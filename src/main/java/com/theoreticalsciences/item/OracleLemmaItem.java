package com.theoreticalsciences.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Oracle's Lemma - a creative-mode testing tool. Used inside one of Alpha Omega's trial chambers, it marks the chamber
 * solved. It has no recipe, and it does nothing at all unless its holder is in creative mode.
 */
public class OracleLemmaItem extends Item {
    public OracleLemmaItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isCreative()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.theoreticalsciences.oracle_creative").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(held);
        }
        if (player instanceof ServerPlayer sp) {
            boolean ok = com.theoreticalsciences.boss.ProofArena.oracleSolve(sp);
            sp.displayClientMessage(Component.translatable(ok ? "message.theoreticalsciences.oracle_solved" : "message.theoreticalsciences.oracle_nothing")
                    .withStyle(ok ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.oracle_lemma").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
