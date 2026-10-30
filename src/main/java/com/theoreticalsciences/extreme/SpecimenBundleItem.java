package com.theoreticalsciences.extreme;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Specimen Bundle or Archive of a {@link Collections collection} (round 24). */
public class SpecimenBundleItem extends Item {
    public SpecimenBundleItem(Properties p) {
        super(p);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Collections.idOf(stack) == null ? super.getName(stack) : Collections.name(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.theoreticalsciences.specimen_bundle").withStyle(ChatFormatting.DARK_GREEN));
    }
}
