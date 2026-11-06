package com.theoreticalsciences.client;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.science.Wear;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 1.2.0: how much life a catalyst or instrument has left (or a source's activity) - a line in its tooltip and a bar on the slot. */
public final class WearClient {
    static final IItemDecorator BAR = (g, font, stack, x, y) -> {
        if (stack.isBarVisible()) return false;
        float f = Wear.barFraction(stack, Minecraft.getInstance().level);
        if (f < 0 || f >= 0.999F) return false;
        int w = Math.round(13 * f);
        int color = Mth.hsvToRgb(Math.max(0, f) / 3F, 1, 1);
        g.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), x + 2, y + 13, x + 15, y + 15, 0xFF000000);
        g.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), x + 2, y + 13, x + 2 + w, y + 14, color | 0xFF000000);
        return false;
    };

    @EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Mod {
        @SubscribeEvent
        public static void decorations(RegisterItemDecorationsEvent e) {
            for (Item item : BuiltInRegistries.ITEM)
                if (TheoreticalSciences.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) e.register(item, BAR);
        }
    }

    @EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
    public static final class Game {
        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent e) {
            if (e.getItemStack().isEmpty()) return;
            java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
            Wear.tooltip(e.getItemStack(), Minecraft.getInstance().level, lines);
            if (!lines.isEmpty()) e.getToolTip().addAll(Math.min(1, e.getToolTip().size()), lines);
        }
    }

    private WearClient() {}
}
