package com.theoreticalsciences.extreme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Procedurally drawn 9x9 grid, in the same dark/cyan style as the machine screens. */
public class ExtremeTableScreen extends AbstractContainerScreen<ExtremeTableMenu> {
    static final int ACCENT = 0xB060FF;

    public ExtremeTableScreen(ExtremeTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = ExtremeTableMenu.WIDTH;
        imageHeight = ExtremeTableMenu.HEIGHT;
        inventoryLabelX = ExtremeTableMenu.INV_X;
        inventoryLabelY = ExtremeTableMenu.INV_Y - 11;
        titleLabelX = 8;
        titleLabelY = 6;
    }

    static int a(int alpha) {
        return alpha << 24 | ACCENT;
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h, float time) {
        com.theoreticalsciences.client.screen.HoloStyle.window(g, x, y, w, h, ACCENT, com.theoreticalsciences.client.screen.HoloStyle.LATE, time, 14);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = com.theoreticalsciences.client.screen.HoloStyle.time(partial);
        panel(g, x, y, imageWidth, imageHeight, time);
        com.theoreticalsciences.client.screen.HoloStyle.slots(g, x, y, menu.slots, 82, i -> i == 81, ACCENT, !menu.slots.get(81).getItem().isEmpty(), time);
        // arrow from grid to result
        int ax = x + ExtremeTableMenu.GRID_X + 9 * 18 + 6, ay = y + ExtremeTableMenu.RESULT_Y + 6;
        g.drawManaged(() -> com.theoreticalsciences.client.screen.HoloStyle.conduit(g, ax + 2, ay - 6, menu.slots.get(81).getItem().isEmpty() ? 0F : 1F, false, ACCENT, time));
        com.theoreticalsciences.client.screen.HoloStyle.divider(g, x, y + ExtremeTableMenu.INV_Y - 14, imageWidth, x + inventoryLabelX, font.width(playerInventoryTitle), ACCENT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX + 2, titleLabelY - 1, com.theoreticalsciences.client.screen.HoloStyle.bright(ACCENT), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFA0A8B8, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
    }
}
