package com.theoreticalsciences.extreme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CompactorScreen extends AbstractContainerScreen<CompactorMenu> {
    public CompactorScreen(CompactorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = CompactorMenu.WIDTH;
        imageHeight = CompactorMenu.HEIGHT;
        inventoryLabelY = CompactorMenu.INV_Y - 11;
        titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = com.theoreticalsciences.client.screen.HoloStyle.time(partial);
        ExtremeTableScreen.panel(g, x, y, imageWidth, imageHeight, time);
        int req = menu.required();
        com.theoreticalsciences.client.screen.HoloStyle.slots(g, x, y, menu.slots, 2, i -> i == 1, ExtremeTableScreen.ACCENT, req > 0, time);
        // progress bar between input and output
        int bx = x + 50, by = y + 50, bw = 76;
        float frac = req > 0 ? (float) Math.min(1.0, (double) menu.absorbed() / req) : 0F;
        g.drawManaged(() -> com.theoreticalsciences.client.screen.HoloStyle.hbar(g, bx, by, bw, 8, frac, 0xE0C0FF, ExtremeTableScreen.ACCENT));
        ItemStack t = menu.target();
        if (!t.isEmpty()) g.renderItem(t, x + 80, y + 24);
        com.theoreticalsciences.client.screen.HoloStyle.divider(g, x, y + CompactorMenu.INV_Y - 14, imageWidth, x + inventoryLabelX, font.width(playerInventoryTitle), ExtremeTableScreen.ACCENT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX + 2, titleLabelY - 1, com.theoreticalsciences.client.screen.HoloStyle.bright(ExtremeTableScreen.ACCENT), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFA0A8B8, false);
        int req = menu.required();
        Component line = req > 0 ? Component.translatable("gui.theoreticalsciences.compactor_progress", String.format("%,d", menu.absorbed()), String.format("%,d", req))
                : Component.translatable("gui.theoreticalsciences.compactor_idle");
        g.drawCenteredString(font, line, imageWidth / 2, 66, 0xFFD0C0FF);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
        ItemStack t = menu.target();
        if (!t.isEmpty() && mx >= leftPos + 80 && mx < leftPos + 96 && my >= topPos + 24 && my < topPos + 40) g.renderTooltip(font, t, mx, my);
    }
}
