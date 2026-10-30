package com.theoreticalsciences.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Opens the Quantum Codex - since round 30 always the mod's own Codex screen (client/codex/CodexScreen), whether or
 * not Patchouli or Modonomicon are installed (their books are still generated, but their maps could not scroll far
 * enough to reach every node). Sneak-use opens the research tree, as before; the Codex has a Research button too.
 * <p>
 * Everything happens on the player's own game: using an item runs on both the client and the server, and the client
 * side simply opens the screen, so no network message is needed.
 * <p>
 * Round 33: the tooltip says all three ways in - use, sneak-use, and the look-up key in inventories.
 */
public class CodexItem extends LoreItem {
    public CodexItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) com.theoreticalsciences.client.ClientHooks.openResearchScreen();
            else com.theoreticalsciences.client.codex.CodexScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.codex_use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.codex_sneak").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.codex_lookup",
                Component.keybind("key.theoreticalsciences.codex_lookup").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
    }
}
