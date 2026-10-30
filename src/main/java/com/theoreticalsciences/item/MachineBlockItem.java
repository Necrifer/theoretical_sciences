package com.theoreticalsciences.item;

import com.theoreticalsciences.machine.MachineType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MachineBlockItem extends BlockItem {
    private final MachineType type;

    public MachineBlockItem(Block block, Properties props, MachineType type) {
        super(block, props);
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.theoreticalsciences." + type.id() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        if (type.isMultiblock())
            tooltip.add(Component.translatable("tooltip.theoreticalsciences.multiblock").withStyle(ChatFormatting.LIGHT_PURPLE));
        // round 35: a structure that only works in one dimension says so up front
        var dims = MachineType.allowedDimensions(type);
        if (dims != null)
            tooltip.add(Component.translatable("tooltip.theoreticalsciences.only_in_dimension", MachineType.dimensionList(dims)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(type.generator ? "tooltip.theoreticalsciences.generator" : "tooltip.theoreticalsciences.consumer",
                String.format("%,d", com.theoreticalsciences.custom.MachineParams.energyCapacity(type))).withStyle(ChatFormatting.GRAY));
    }
}
