package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.transcend.ConfluenceBlockEntity;
import com.theoreticalsciences.transcend.ConfluenceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** One small gauge per fluid; when every gauge is full, a batch of Pleroma is made. */
public class ConfluenceScreen extends AbstractContainerScreen<ConfluenceMenu> {
    private static final int GX = 8, GY = 30;
    /** Round 26: with every mod's Essence the list can be long - the cells shrink until they all fit. */
    private int cols = 12, cw = 20, ch = 26;

    private void layout() {
        int n = menu.be.required.size(), w = 210, h = 106;
        int[][] sizes = {{20, 26}, {16, 21}, {13, 17}, {10, 13}, {8, 10}, {6, 8}};
        for (int[] sz : sizes) {
            cw = sz[0];
            ch = sz[1];
            cols = w / cw;
            if (((n + cols - 1) / cols) * ch <= h) return;
        }
    }

    public ConfluenceScreen(ConfluenceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = ConfluenceMenu.WIDTH;
        imageHeight = ConfluenceMenu.HEIGHT;
        inventoryLabelX = ConfluenceMenu.INV_X;
        inventoryLabelY = ConfluenceMenu.INV_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = HoloStyle.time(partial);
        HoloStyle.window(g, x, y, imageWidth, imageHeight, 0xC080FF, HoloStyle.ENDGAME, time, 14);
        HoloStyle.slots(g, x, y, menu.slots, 0, i -> false, 0xC080FF, false, time);
        HoloStyle.divider(g, x, y + inventoryLabelY - 3, imageWidth, x + inventoryLabelX, font.width(playerInventoryTitle), 0xC080FF);
        ConfluenceBlockEntity be = menu.be;
        layout();
        int bw = cw - 4, bh = ch - 4;
        for (int i = 0; i < be.required.size(); i++) {
            int cx = x + GX + (i % cols) * cw, cy = y + GY + (i / cols) * ch;
            int fcx = cx, fcy = cy;
            g.drawManaged(() -> HoloStyle.tubeBack(g, fcx, fcy, bw, bh, 0xC080FF));
            float f = (float) Math.min(1.0, be.stored[i] / (double) be.target(i));
            int h = (int) (bh * f);
            int tint = IClientFluidTypeExtensions.of(be.required.get(i).getFluid()).getTintColor(be.required.get(i));
            int col = (tint & 0xFFFFFF) == 0xFFFFFF ? (i == 0 ? 0x3F76E4 : 0xFF6A00) : tint & 0xFFFFFF;
            g.fill(cx, cy + bh - h, cx + bw, cy + bh, 0xFF000000 | col);
            int ftop = h > 0 ? cy + bh - h : -1;
            g.drawManaged(() -> HoloStyle.tubeFront(g, fcx, fcy, bw, bh, f >= 1 ? 0x60FF90 : 0xC080FF, false, ftop));
        }
        // the output
        int ox = x + imageWidth - 30, oy = y + GY;
        g.drawManaged(() -> HoloStyle.tubeBack(g, ox, oy, 20, 80, 0xFFE0FF));
        int ph = (int) (80L * be.pleroma / ConfluenceBlockEntity.OUT_CAP);
        g.fillGradient(ox, oy + 80 - ph, ox + 20, oy + 80, 0xFFFFFFFF, 0xFFC080FF);
        int ptop = ph > 0 ? oy + 80 - ph : -1;
        g.drawManaged(() -> HoloStyle.tubeFront(g, ox, oy, 20, 80, 0xFFE0FF, true, ptop));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        TextFit.draw(g, font, title, 10, 5, imageWidth - 18, HoloStyle.bright(0xC080FF));
        ConfluenceBlockEntity be = menu.be;
        TextFit.draw(g, font, Component.translatable("gui.theoreticalsciences.confluence_status", be.filledCount(), be.required.size(),
                String.format("%,d", ConfluenceBlockEntity.TARGET), String.format("%,d", ConfluenceBlockEntity.ESSENCE_TARGET)), 8, 17, imageWidth - 16, 0xFFA0A0C0);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HoloStyle.mix(0x8899AA, 0xC080FF, 0.35F), false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        renderTooltip(g, mx, my);
        ConfluenceBlockEntity be = menu.be;
        for (int i = 0; i < be.required.size(); i++) {
            int cx = leftPos + GX + (i % cols) * cw, cy = topPos + GY + (i / cols) * ch;
            if (mx >= cx && my >= cy && mx < cx + cw - 4 && my < cy + ch - 4) {
                FluidStack fs = be.required.get(i);
                TextFit.tooltip(g, font, java.util.List.of(fs.getHoverName(),
                        Component.literal(String.format("%,d / %,d mB", be.stored[i], be.target(i)))), mx, my);
            }
        }
        int ox = leftPos + imageWidth - 30, oy = topPos + GY;
        if (mx >= ox && my >= oy && mx < ox + 20 && my < oy + 80)
            TextFit.tooltip(g, font, java.util.List.of(Component.translatable("fluid_type.theoreticalsciences.pleroma"),
                    Component.literal(String.format("%,d / %,d mB", be.pleroma, ConfluenceBlockEntity.OUT_CAP))), mx, my);
    }
}
