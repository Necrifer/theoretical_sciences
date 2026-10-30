package com.theoreticalsciences.transcend;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DecorBlockItem extends BlockItem {
    private final DecorBlock block;

    public DecorBlockItem(DecorBlock block, Properties p) {
        super(block, p);
        this.block = block;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("block.theoreticalsciences." + block.kind.id() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatable("tooltip.theoreticalsciences.decor_live").withStyle(ChatFormatting.DARK_AQUA));
        if (block.kind.lensing) tip.add(Component.translatable("tooltip.theoreticalsciences.decor_lag_lens").withStyle(ChatFormatting.RED));
        else tip.add(Component.translatable("tooltip.theoreticalsciences.decor_lag_many").withStyle(ChatFormatting.GOLD));
    }
}
