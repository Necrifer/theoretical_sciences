package com.theoreticalsciences.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Item with a flavour line explaining the physics it is based on. */
public class LoreItem extends Item {
    public LoreItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        // attunement items: which other dimensions this one also shields you from (round 14)
        String shield = getDescriptionId() + ".shield";
        if (net.minecraft.locale.Language.getInstance().has(shield))
            tooltip.add(Component.translatable(shield).withStyle(ChatFormatting.AQUA));
        // genome samples: whose (round 21)
        var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("Species")) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(cd.copyTag().getString("Species"));
            var type = id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            tooltip.add(Component.translatable("tooltip.theoreticalsciences.species", type == null ? Component.literal(String.valueOf(id)) : type.getDescription())
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
