package com.theoreticalsciences.client.codex;

import com.mojang.blaze3d.platform.InputConstants;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Round 32b: the Codex as a recipe book for players without JEI. Hover any item of this mod in an inventory and press
 * the "Look up in the Quantum Codex" key (K by default, changeable in Controls) to open its page - with every way to
 * make it. Pressed anywhere else in the world, the key just opens the Codex.
 */
public final class CodexKeys {
    /** Round 35: what EMI / JEI have under the mouse (each viewer's plugin adds one; empty when nothing). */
    public static final java.util.List<java.util.function.Supplier<ItemStack>> HOVERED = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static final KeyMapping LOOKUP = new KeyMapping("key.theoreticalsciences.codex_lookup", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
            "key.categories.theoreticalsciences");

    @EventBusSubscriber(modid = TheoreticalSciences.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(LOOKUP);
        }
    }

    @EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
    public static final class Handlers {
        /**
         * In any screen: the item under the mouse - an inventory slot, or (round 35) an item in EMI's / JEI's item list,
         * bookmarks or recipe pages (those are not inventory slots, so before this K did nothing there).
         */
        @SubscribeEvent
        public static void onKey(ScreenEvent.KeyPressed.Pre e) {
            if (!LOOKUP.matches(e.getKeyCode(), e.getScanCode()) || e.getScreen() instanceof CodexScreen) return;
            ItemStack st = ItemStack.EMPTY;
            if (e.getScreen() instanceof AbstractContainerScreen<?> s) {
                Slot slot = s.getSlotUnderMouse();
                if (slot != null && slot.hasItem()) st = slot.getItem();
            }
            for (int i = 0; i < HOVERED.size() && st.isEmpty(); i++) {
                try {
                    ItemStack h = HOVERED.get(i).get();
                    if (h != null) st = h;
                } catch (Throwable ignored) {
                    // a recipe viewer that could not say: try the next
                }
            }
            if (st.isEmpty()) return;
            if (CodexData.ensure() && CodexData.entryForItem(BuiltInRegistries.ITEM.getKey(st.getItem()).toString()) != null) {
                CodexScreen.openAt(st.getItem());
                e.setCanceled(true);
            }
        }

        /** In the world: open the Codex. */
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            while (LOOKUP.consumeClick()) if (mc.screen == null && mc.player != null) mc.setScreen(new CodexScreen());
        }
    }

    private CodexKeys() {}
}
