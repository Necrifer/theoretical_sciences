package com.theoreticalsciences.foam;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reads the four virtual-particle resonances where you stand in the Quantum Foam (the numbers a harvester must be tuned to). */
public class ResonanceProbeItem extends LoreItem {
    public ResonanceProbeItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            if (sl.dimension() != QHDimensions.FOAM) {
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.probe_outside").withStyle(ChatFormatting.GRAY), true);
            } else {
                var pos = player.blockPosition();
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.probe_header", pos.getX(), pos.getY(), pos.getZ())
                        .withStyle(ChatFormatting.AQUA), false);
                int dom = FoamField.dominant(sl, pos);
                for (int i = 0; i < FoamField.MATERIALS.length; i++) {
                    player.displayClientMessage(Component.literal(String.format("  %-9s %5.1f%s", FoamField.NAMES[i], FoamField.resonance(sl, i, pos),
                            i == dom ? "   <- strongest field" : "")).withStyle(i == dom ? ChatFormatting.YELLOW : ChatFormatting.WHITE), false);
                }
            }
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
