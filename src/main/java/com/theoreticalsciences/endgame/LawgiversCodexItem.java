package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Lawgiver's Codex: opens a screen where the laws of the dimension you stand in can be rewritten.
 * Round 32: every law written costs Eschaton Charge (see {@link CodexLaws#handle}); an empty Codex will not open.
 */
public class LawgiversCodexItem extends Item implements EschatonPowered {
    public static final int MAX_CHARGE = 10_000;
    public LawgiversCodexItem(Properties p) {
        super(p.fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!EschatonCharge.active(player, s)) {
            if (!level.isClientSide) EschatonCharge.sayDormant(player, s);
            return InteractionResultHolder.fail(s);
        }
        if (level.isClientSide) EndgameClient.openCodex(player);
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public int maxCharge() {
        return MAX_CHARGE;
    }

    @Override
    public boolean isBarVisible(ItemStack s) {
        return EschatonCharge.barVisible(s);
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return EschatonCharge.barWidth(s);
    }

    @Override
    public int getBarColor(ItemStack s) {
        return EschatonCharge.barColour(s);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.lawgivers_codex.use").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.lawgivers_codex.target").withStyle(ChatFormatting.GRAY));
    }
}
