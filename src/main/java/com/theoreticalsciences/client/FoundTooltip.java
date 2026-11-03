package com.theoreticalsciences.client;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 1.1.0: where a material is found in the world, under its description - for every one of the mod's items and blocks
 * that tools/found.py located (its "item|block.theoreticalsciences.<id>.found" line). Nothing is shown for the rest.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
public final class FoundTooltip {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) return;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!TheoreticalSciences.MODID.equals(id.getNamespace())) return;
        if (s.getItem() instanceof net.minecraft.world.item.SpawnEggItem) {          // creatures' descriptions (1.1.0)
            String lore = "item." + TheoreticalSciences.MODID + "." + id.getPath() + ".lore";
            if (I18n.exists(lore)) e.getToolTip().add(Math.min(1, e.getToolTip().size()), Component.translatable(lore).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        String key = (s.getItem() instanceof BlockItem ? "block." : "item.") + TheoreticalSciences.MODID + "." + id.getPath() + ".found";
        if (!I18n.exists(key)) return;
        int at = e.getToolTip().size();
        if (e.getFlags().isAdvanced()) at = Math.max(1, at - 2);     // above the registry name and component count (F3+H)
        e.getToolTip().add(at, Component.translatable(key).withStyle(ChatFormatting.GOLD));
    }

    private FoundTooltip() {}
}
