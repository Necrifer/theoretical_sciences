package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.endgame.CenserPayload;
import com.theoreticalsciences.endgame.EntropyCenserItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Choose the Censer of Entropy's effect (any harmful one), its level, its duration and the cloud's radius. */
public class CenserScreen extends Screen {
    private static final int ACCENT = 0xB060FF;
    private static final int W = 260, H = 200, ROWS = 8, LIST_Y = 40;
    private final List<ResourceLocation> all = EntropyCenserItem.effects();
    // round 30: a search box above the list; `effects` is what matches it (all of them when it is empty)
    private List<ResourceLocation> effects = all;
    private net.minecraft.client.gui.components.EditBox search;
    private String query = "";
    private String effect;
    private int seconds, level, radius, scroll;

    public CenserScreen(ItemStack stack) {
        super(Component.translatable("item.theoreticalsciences.entropy_censer"));
        EntropyCenserItem.Settings s = EntropyCenserItem.settings(stack);
        effect = s.effect();
        seconds = s.seconds();
        level = s.level();
        radius = s.radius();
        int i = all.indexOf(ResourceLocation.tryParse(effect));
        if (i >= ROWS) scroll = Math.min(i, Math.max(0, effects.size() - ROWS));
    }

    private int x() {
        return (width - W) / 2;
    }

    private int y() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        search = new net.minecraft.client.gui.components.EditBox(font, x() + 6, y() + 20, 134, 14, Component.translatable("gui.theoreticalsciences.censer_search"));
        search.setHint(Component.translatable("gui.theoreticalsciences.censer_search").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        search.setMaxLength(48);
        search.setValue(query);
        search.setResponder(this::filter);
        addRenderableWidget(search);
        int x = x() + 150, y = y() + 24;
        row(x, y, "seconds", d -> seconds = clamp(seconds + d, 1, EntropyCenserItem.MAX_SECONDS));
        row(x, y + 40, "level", d -> level = clamp(level + (Math.abs(d) >= 60 ? (int) Math.signum(d) * 10 : (int) Math.signum(d)), 1, EntropyCenserItem.MAX_LEVEL));
        row(x, y + 80, "radius", d -> radius = clamp(radius + (int) Math.signum(d), 1, EntropyCenserItem.MAX_RADIUS));
        addRenderableWidget(HoloButton.of(x() + 150, y() + H - 28, 100, 18, Component.translatable("gui.done"), ACCENT, this::onClose));
    }

    private void row(int x, int y, String what, java.util.function.IntConsumer f) {
        int[] steps = {-60, -1, 1, 60};
        String[] lab = {"<<", "<", ">", ">>"};
        for (int k = 0; k < 4; k++) {
            int d = steps[k];
            addRenderableWidget(HoloButton.of(x + k * 26, y + 12, 24, 15, lab[k], ACCENT, () -> f.accept(d)));
        }
    }

    /** Keep only the effects whose name, id or mod contains every word typed. */
    private void filter(String q) {
        query = q;
        String[] words = q.toLowerCase(java.util.Locale.ROOT).trim().split("\\s+");
        if (q.isBlank()) effects = all;
        else {
            List<ResourceLocation> out = new java.util.ArrayList<>();
            for (ResourceLocation id : all) {
                var e = BuiltInRegistries.MOB_EFFECT.get(id);
                String hay = ((e == null ? "" : e.getDisplayName().getString()) + " " + id).toLowerCase(java.util.Locale.ROOT);
                boolean ok = true;
                for (String w : words) if (!hay.contains(w)) ok = false;
                if (ok) out.add(id);
            }
            effects = out;
        }
        scroll = 0;
    }

    private static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = x(), y = y();
        g.drawString(font, title, x + 10, y + 5, HoloStyle.bright(ACCENT), false);
        for (int i = 0; i < ROWS && scroll + i < effects.size(); i++) {
            ResourceLocation id = effects.get(scroll + i);
            int ry = y + LIST_Y + i * 18;
            boolean sel = id.toString().equals(effect);
            boolean over = mx >= x + 6 && mx < x + 140 && my >= ry && my < ry + 16;
            if (sel || over) {
                int fry = ry;
                g.drawManaged(() -> {
                    g.fillGradient(x + 6, fry, x + 140, fry + 16, HoloStyle.argb(sel ? 0x60 : 0x20, ACCENT), HoloStyle.argb(sel ? 0x20 : 0x08, ACCENT));
                    if (sel) HoloStyle.brackets(g, x + 6, fry, 134, 16, 3, HoloStyle.argb(0xE0, ACCENT));
                });
            }
            var e = BuiltInRegistries.MOB_EFFECT.get(id);
            String name = e == null ? id.toString() : e.getDisplayName().getString();
            g.drawString(font, font.plainSubstrByWidth(name, 130), x + 10, ry + 4, sel ? 0xFFFFE0FF : 0xFFC0B0D0, false);
        }
        if (effects.isEmpty()) g.drawString(font, "-", x + 10, y + LIST_Y + 4, 0xFF806090, false);
        int cx = x + 150;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.censer_seconds", seconds), cx, y + 24, 0xFFE0D0F0, false);
        g.drawString(font, Component.translatable("gui.theoreticalsciences.censer_level", level), cx, y + 64, 0xFFE0D0F0, false);
        g.drawString(font, Component.translatable("gui.theoreticalsciences.censer_radius", radius), cx, y + 104, 0xFFE0D0F0, false);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        HoloStyle.window(g, x(), y(), W, H, ACCENT, HoloStyle.LATE, HoloStyle.time(pt), 14);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        int x = x(), y = y();
        for (int i = 0; i < ROWS && scroll + i < effects.size(); i++) {
            int ry = y + LIST_Y + i * 18;
            if (mx >= x + 6 && mx < x + 140 && my >= ry && my < ry + 16) {
                effect = effects.get(scroll + i).toString();
                return true;
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // while typing in the search box, letters go to it (so 'E' does not close the screen)
        if (search != null && search.isFocused() && key != 256) return search.keyPressed(key, scan, mods) || true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, effects.size() - ROWS));
        return true;
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new CenserPayload(effect, seconds, level, radius));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
