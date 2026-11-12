package com.theoreticalsciences.home;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/** The Empyrean Assembly Table as an item (1.8.2): a table that was broken while holding things says what is in it. */
public class EmpyreanTableItem extends BlockItem {
    public EmpyreanTableItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return;
        CompoundTag tag = data.copyTag();
        var held = tag.getList("Held", Tag.TAG_COMPOUND);
        long out = tag.getLong("OutCount");
        if (held.isEmpty() && out <= 0) return;
        tip.add(Component.translatable("tooltip.theoreticalsciences.empyrean.holds").withStyle(ChatFormatting.GOLD));
        int shown = 0;
        for (Tag raw : held) {
            CompoundTag t = (CompoundTag) raw;
            if (shown++ >= 8) {
                tip.add(Component.translatable("tooltip.theoreticalsciences.empyrean.more", held.size() - 8).withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            line(tip, t.getString("Item"), t.getLong("Count"), ChatFormatting.GRAY);
        }
        if (out > 0) line(tip, tag.getString("Out"), out, ChatFormatting.GREEN);
    }

    private static void line(List<Component> tip, String id, long n, ChatFormatting colour) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return;
        tip.add(Component.literal("  " + String.format(Locale.ROOT, "%,d", n) + " x ").append(BuiltInRegistries.ITEM.get(rl).getDescription()).withStyle(colour));
    }
}
