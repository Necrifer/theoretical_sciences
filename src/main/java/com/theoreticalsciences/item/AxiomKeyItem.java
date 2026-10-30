package com.theoreticalsciences.item;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.HyperrealTravel;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reusable passage to Hyperdimensional Reality Space, forged by the Axiomatic Breach Engine. */
public class AxiomKeyItem extends LoreItem {
    public AxiomKeyItem(Properties props) {
        super(props.fireResistant());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!ResearchService.has(sp, TheoreticalSciences.id("reality_breach"))) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.key_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level.dimension() == QHDimensions.HYPERREAL) HyperrealTravel.leave(sp);
        else if (!HyperrealTravel.enter(sp)) return InteractionResultHolder.fail(stack);
        sp.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }
}
