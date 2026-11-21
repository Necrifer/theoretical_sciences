package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.home.HomeClient;
import com.theoreticalsciences.home.HomeMaterials;
import com.theoreticalsciences.home.HomePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 1.8.0: the Core's fourth tab, Recreating Home. Forty-two lines, each asking for 33,550,337 of one material; what has
 * been given is kept by the server per player. It is drawn in gold, not the Core's green: this is not the program
 * speaking, it is what the program is the remnant of.
 */
public class HomeScreen extends Screen {
    private static final int GOLD = 0xFFFFE9A8, DIM = 0xFFB89C5C, FAINT = 0xFF6A5A38, BG = 0xF00E0A04, DONE = 0xFF9CFFB0;
    private static final int ROW = 20;
    private final Screen parent;
    private final BlockPos core;
    private int L, T, W, H, cols, colW, listTop, listH, scroll, maxScroll;

    public HomeScreen(Screen parent, BlockPos core) {
        super(Component.translatable("gui.theoreticalsciences.home.title"));
        this.parent = parent;
        this.core = core;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout() {
        int m = width >= 640 && height >= 360 ? 8 : 4;
        L = m;
        T = m;
        W = width - 2 * m;
        H = height - 2 * m;
        cols = W >= 620 ? 3 : W >= 400 ? 2 : 1;
        colW = (W - 16) / cols;
        listTop = T + 62;
        listH = H - 62 - 26;
        int rows = (HomeMaterials.IDS.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * ROW - listH);
        scroll = Mth.clamp(scroll, 0, maxScroll);
    }

    private String offer() {
        return "[ " + Component.translatable("gui.theoreticalsciences.home.offer").getString() + " ]";
    }

    private String back() {
        return "[ " + Component.translatable("gui.theoreticalsciences.home.back").getString() + " ]";
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        layout();
        g.fill(0, 0, width, height, 0xC0000000);
        g.fill(L, T, L + W, T + H, BG);
        g.renderOutline(L, T, W, H, DIM);
        g.renderOutline(L + 2, T + 2, W - 4, H - 4, FAINT);
        g.drawString(font, title, L + 8, T + 7, GOLD, false);
        long[] given = HomeClient.given;
        int full = 0;
        double sum = 0;
        for (int i = 0; i < HomeMaterials.IDS.size(); i++) {
            long v = i < given.length ? given[i] : 0;
            if (v >= HomeMaterials.TARGET) full++;
            sum += Math.min(v, HomeMaterials.TARGET) / (double) HomeMaterials.TARGET;
        }
        String count = full + " / " + HomeMaterials.IDS.size();
        g.drawString(font, count, L + W - 8 - font.width(count), T + 7, full == HomeMaterials.IDS.size() ? DONE : GOLD, false);
        int ty = T + 20;
        for (var line : font.split(Component.translatable(HomeClient.complete() ? "gui.theoreticalsciences.home.done" : "gui.theoreticalsciences.home.intro",
                HomeClient.full(HomeMaterials.TARGET)), W - 16)) {
            if (ty > T + 40) break;
            g.drawString(font, line, L + 8, ty, DIM, false);
            ty += 10;
        }
        // the whole of it, as one bar
        int bw = W - 16, by = T + 52;
        g.fill(L + 8, by, L + 8 + bw, by + 4, 0xFF30260F);
        g.fill(L + 8, by, L + 8 + (int) (bw * sum / HomeMaterials.IDS.size()), by + 4, full == HomeMaterials.IDS.size() ? DONE : GOLD);
        // the lines
        g.enableScissor(L + 4, listTop, L + W - 4, listTop + listH);
        Component tip = null;
        for (int i = 0; i < HomeMaterials.IDS.size(); i++) {
            int x = L + 8 + (i % cols) * colW, y = listTop + (i / cols) * ROW - scroll;
            if (y + ROW < listTop || y > listTop + listH) continue;
            long v = Math.min(i < given.length ? given[i] : 0, HomeMaterials.TARGET);
            boolean done = v >= HomeMaterials.TARGET;
            ItemStack st = new ItemStack(HomeMaterials.item(i));
            g.renderItem(st, x, y);
            String name = font.plainSubstrByWidth(st.getHoverName().getString(), colW - 30);
            g.drawString(font, name, x + 20, y, done ? DONE : GOLD, false);
            int barW = colW - 30;
            g.fill(x + 20, y + 10, x + 20 + barW, y + 16, 0xFF30260F);
            g.fill(x + 20, y + 10, x + 20 + (int) (barW * (v / (double) HomeMaterials.TARGET)), y + 16, done ? 0xFF4C9A60 : 0xFF9A7C30);
            String n = HomeClient.full(v);
            HoloStyle.miniText(g, font, n, x + 22, y + 11, 0xFFFFFFFF, 0.5F, false);
            if (mx >= x && mx < x + colW - 6 && my >= y && my < y + ROW - 2 && my >= listTop && my < listTop + listH)
                tip = Component.literal(st.getHoverName().getString() + ": " + n + " / " + HomeClient.full(HomeMaterials.TARGET));
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int bar = Math.max(12, listH * listH / (listH + maxScroll));
            g.fill(L + W - 6, listTop + (listH - bar) * scroll / maxScroll, L + W - 3, listTop + (listH - bar) * scroll / maxScroll + bar, DIM);
        }
        // the buttons
        int byy = T + H - 16;
        boolean overO = over(mx, my, L + 8, byy, offer()), overB = over(mx, my, L + W - 8 - font.width(back()), byy, back());
        g.drawString(font, offer(), L + 8, byy, overO ? GOLD : DIM, false);
        g.drawString(font, back(), L + W - 8 - font.width(back()), byy, overB ? GOLD : DIM, false);
        String hint = Component.translatable("gui.theoreticalsciences.home.altar_hint").getString();
        int hx = L + 8 + font.width(offer()) + 12;
        g.drawString(font, font.plainSubstrByWidth(hint, L + W - 8 - font.width(back()) - 12 - hx), hx, byy, FAINT, false);
        if (tip != null) g.renderTooltip(font, List.of(tip.getVisualOrderText()), mx, my);
    }

    private boolean over(double mx, double my, int x, int y, String label) {
        return mx >= x && mx < x + font.width(label) && my >= y - 2 && my < y + 11;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int byy = T + H - 16;
        if (over(mx, my, L + 8, byy, offer())) {
            PacketDistributor.sendToServer(new HomePayloads.Submit(core));
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
            return true;
        }
        if (over(mx, my, L + W - 8 - font.width(back()), byy, back())) {
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Mth.clamp(scroll - (int) (dy * ROW), 0, maxScroll);
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
