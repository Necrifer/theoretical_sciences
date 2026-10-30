package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.storage.InfinityCacheBlockEntity;
import com.theoreticalsciences.storage.InfinityCacheMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Transcendent Cache's window: six rows of 27, a scroll bar, and counts written short (1.2M, 3.4G). */
public class InfinityCacheScreen extends AbstractContainerScreen<InfinityCacheMenu> {
    private static final int ACCENT = 0xFFD070;

    public InfinityCacheScreen(InfinityCacheMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 190;
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = HoloStyle.time(pt);
        HoloStyle.window(g, x, y, imageWidth, imageHeight, ACCENT, HoloStyle.LATE, time, 14);
        HoloStyle.slots(g, x, y, menu.slots, menu.slots.size() - 36, i -> false, ACCENT, false, time);
        HoloStyle.divider(g, x, y + inventoryLabelY - 3, imageWidth, x + inventoryLabelX, font.width(playerInventoryTitle), ACCENT);
        // the scroll bar
        int bx = x + 172, by = y + 18, bh = InfinityCacheMenu.VIEW_ROWS * 18;
        int max = InfinityCacheBlockEntity.ROWS - InfinityCacheMenu.VIEW_ROWS;
        int th = Math.max(10, bh * InfinityCacheMenu.VIEW_ROWS / InfinityCacheBlockEntity.ROWS);
        int ty = by + (max <= 0 ? 0 : (bh - th) * menu.offset() / max);
        g.drawManaged(() -> {
            HoloStyle.field(g, bx, by, 10, bh, ACCENT);
            HoloStyle.chamferFill(g, bx + 2, ty + 1, 6, th - 2, 1, HoloStyle.argb(0xE0, ACCENT));
            g.fill(bx + 3, ty + th / 2 - 2, bx + 7, ty + th / 2 - 1, 0x90FFFFFF);
            g.fill(bx + 3, ty + th / 2 + 1, bx + 7, ty + th / 2 + 2, 0x90FFFFFF);
        });
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX + 2, titleLabelY - 1, HoloStyle.bright(ACCENT), false);
        String rows = (menu.offset() + 1) + "-" + (menu.offset() + InfinityCacheMenu.VIEW_ROWS) + "/" + InfinityCacheBlockEntity.ROWS;
        g.drawString(font, rows, imageWidth - 8 - font.width(rows), titleLabelY - 1, 0xFFB0A080, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HoloStyle.mix(0x8899AA, ACCENT, 0.35F), false);
    }

    public static String compact(long n) {
        if (n < 1000) return String.valueOf(n);
        String[] u = {"k", "M", "G"};
        double v = n;
        int i = -1;
        while (v >= 1000 && i < u.length - 1) {
            v /= 1000;
            i++;
        }
        return (v >= 100 ? String.valueOf((int) v) : String.format("%.1f", v)) + u[i];
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        ItemStack s = slot.getItem();
        if (s.getCount() <= 99) {
            super.renderSlot(g, slot);
            return;
        }
        g.renderItem(s, slot.x, slot.y);
        g.renderItemDecorations(font, s.copyWithCount(1), slot.x, slot.y);
        String c = compact(s.getCount());
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, c, (int) ((slot.x + 17) / 0.75F) - font.width(c), (int) ((slot.y + 11) / 0.75F), 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (hoveredSlot != null && hoveredSlot.index < InfinityCacheMenu.VIEW && hoveredSlot.getItem().getCount() > 99)
            g.renderTooltip(font, Component.literal(String.format("%,d", hoveredSlot.getItem().getCount())), mx, my - 14);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (minecraft != null && minecraft.gameMode != null && dy != 0) {
            int steps = hasShiftDown() ? 6 : 1;
            for (int k = 0; k < steps; k++) {
                int id = dy > 0 ? 0 : 1;
                menu.clickMenuButton(minecraft.player, id);           // move the window here too, at once
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
