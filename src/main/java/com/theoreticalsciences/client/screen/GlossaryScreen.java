package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.ClientResearch;
import com.theoreticalsciences.research.GlyphRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;

/**
 * The book of symbols: every glyph a derivation can use, grouped by subject, with what it means and its unit. Symbols
 * not learned yet show how to learn them (an item to hold, or a research to complete). Opened from the research tree
 * and from any derivation; symbols the open derivation still needs are listed first.
 */
public class GlossaryScreen extends Screen {
    private final Screen parent;
    private final List<String> wanted;
    private String cat = "";
    private final ScrollBar bar = new ScrollBar();
    private EditBox search;
    private int left, top, w, listX;

    public GlossaryScreen(Screen parent, List<String> wanted) {
        super(Component.translatable("glossary.theoreticalsciences.title"));
        this.parent = parent;
        this.wanted = wanted;
    }

    private List<String> cats() {
        TreeSet<String> s = new TreeSet<>();
        for (GlyphRegistry.Glyph g : GlyphRegistry.client().values()) s.add(g.cat().isEmpty() ? "other" : g.cat());
        List<String> out = new ArrayList<>();
        if (!wanted.isEmpty()) out.add("#needed");
        out.add("");
        out.addAll(s);
        return out;
    }

    @Override
    protected void init() {
        w = Math.min(520, width - 20);
        left = (width - w) / 2;
        top = 24;
        listX = left + 104;
        search = new EditBox(font, listX, top, w - 104, 14, Component.translatable("glossary.theoreticalsciences.search"));
        search.setHint(Component.translatable("glossary.theoreticalsciences.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(s -> bar.value = 0);
        addRenderableWidget(search);
        int y = top;
        if (!wanted.isEmpty() && cat.isEmpty()) cat = "#needed";
        List<String> cs = cats();
        int room = height - 30 - top, rowH = 16;
        boolean two = cs.size() * rowH > room;
        if (two && (cs.size() + 1) / 2 * rowH > room) rowH = Math.max(11, room / ((cs.size() + 1) / 2));
        for (int i = 0; i < cs.size(); i++) {
            String c = cs.get(i);
            String label = c.equals("#needed") ? "» Needed" : c.isEmpty() ? "All" : Character.toUpperCase(c.charAt(0)) + c.substring(1);
            int bx = two ? left + (i % 2) * 50 : left, by = top + (two ? i / 2 : i) * rowH;
            addRenderableWidget(new HoloButton(bx, by, two ? 48 : 98, rowH - 2, Component.literal(label), 0x40C0FF, b -> {
                cat = c;
                bar.value = 0;
            }));
        }
        addRenderableWidget(new HoloButton(left, height - 24, 98, 18, Component.translatable("gui.back"), 0x40C0FF, b -> onClose()));
    }

    private List<GlyphRegistry.Glyph> shown() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<GlyphRegistry.Glyph> out = new ArrayList<>();
        Collection<GlyphRegistry.Glyph> src = GlyphRegistry.client().values();
        if (cat.equals("#needed")) {
            for (String t : wanted) {
                GlyphRegistry.Glyph g = GlyphRegistry.get(t, true);
                if (g != null) out.add(g);
            }
        } else
            for (GlyphRegistry.Glyph g : src) if (cat.isEmpty() || g.cat().equals(cat) || (cat.equals("other") && g.cat().isEmpty())) out.add(g);
        if (!q.isEmpty())
            out.removeIf(g -> !(g.token().toLowerCase(Locale.ROOT).contains(q) || g.name().toLowerCase(Locale.ROOT).contains(q) || g.meaning().toLowerCase(Locale.ROOT).contains(q)));
        // learned first, then alphabetical by name
        out.sort(Comparator.comparing((GlyphRegistry.Glyph g) -> !ClientResearch.knows(g.token())).thenComparing(g -> g.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    /** "Learn it: hold a Qubit" / "Learn it: complete Calculus" / "Known from the start". */
    public static Component unlockText(GlyphRegistry.Glyph g) {
        return switch (g.unlock()) {
            case START -> Component.translatable("glossary.theoreticalsciences.unlock_start");
            case ITEM -> {
                ResourceLocation r = ResourceLocation.tryParse(g.unlockId());
                var it = r == null ? null : BuiltInRegistries.ITEM.getOptional(r).orElse(null);
                yield Component.translatable("glossary.theoreticalsciences.unlock_item", it == null ? Component.literal(g.unlockId()) : it.getDescription());
            }
            case RESEARCH -> {
                ResourceLocation r = ResourceLocation.tryParse(g.unlockId());
                var d = r == null ? null : ClientResearch.defs.get(r);
                yield Component.translatable("glossary.theoreticalsciences.unlock_research", d == null ? Component.literal(g.unlockId()) : d.titleComponent(r));
            }
        };
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, width, height, 0xF0100A04, 0xF0201408);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left, 8, 0xFFFFFF, false);
        int known = 0, total = GlyphRegistry.client().size();
        for (String t : GlyphRegistry.client().keySet()) if (ClientResearch.knows(t)) known++;
        String count = known + " / " + total;
        g.drawString(font, count, left + w - font.width(count), 8, 0xC0A060, false);
        int x = listX, y = top + 20, bottom = height - 8, colW = w - 104 - ScrollBar.W - 4;
        List<GlyphRegistry.Glyph> list = shown();
        // measure first, so the bar and the clamp agree with what is drawn this frame
        int content = 0;
        for (GlyphRegistry.Glyph gl : list) content += height(gl, colW);
        bar.set(x + colW + 4, y, bottom - y, content, bottom - y);
        int scroll = bar.value;
        g.enableScissor(x, y, x + colW, bottom);
        int yy = y - scroll;
        for (GlyphRegistry.Glyph gl : list) {
            boolean k = ClientResearch.knows(gl.token());
            List<FormattedCharSequence> mean = font.split(Component.literal(k ? gl.meaning() : "???"), colW - 70);
            int h = height(gl, colW);
            if (yy + h > y && yy < bottom) {
                g.fill(x, yy, x + colW, yy + h - 3, k ? 0x40302010 : 0x40100808);
                MathTypeset.Box b = MathTypeset.tile(font, gl.token(), 1.6F);
                MathTypeset.draw(g, font, b, x + 6, yy + 18, 56, k ? 0xFFFFE6B0 : 0xFF605040);
                g.drawString(font, Component.literal(k ? gl.name() : "???").withStyle(k ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY), x + 66, yy + 3, 0, false);
                if (k && !gl.unit().isEmpty()) g.drawString(font, "[" + gl.unit() + "]", x + colW - 6 - font.width("[" + gl.unit() + "]"), yy + 3, 0x908060, false);
                int ly = yy + 14;
                for (FormattedCharSequence line : mean) {
                    g.drawString(font, line, x + 66, ly, k ? 0xE0D8C8 : 0x605850, false);
                    ly += 10;
                }
                g.drawString(font, unlockText(gl).copy().withStyle(k ? ChatFormatting.DARK_GREEN : ChatFormatting.RED), x + 66, ly, 0, false);
            }
            yy += h;
        }
        g.disableScissor();
        bar.draw(g, mx, my, 0x60201408, 0xC0806030, 0xFFC09050);
        if (list.isEmpty()) g.drawString(font, Component.translatable("glossary.theoreticalsciences.empty"), x + 4, y + 4, 0x807060, false);
    }

    private int height(GlyphRegistry.Glyph gl, int colW) {
        boolean k = ClientResearch.knows(gl.token());
        return 14 + 10 * Math.max(1, font.split(Component.literal(k ? gl.meaning() : "???"), colW - 70).size()) + 12;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        bar.scroll(sy, 24);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && bar.click(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && bar.drag(my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && bar.release()) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (search != null && search.isFocused()) return super.keyPressed(key, scan, mods);
        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN -> bar.value += bar.view - 20;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP -> bar.value -= bar.view - 20;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> bar.value = 0;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> bar.value = bar.max();
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        bar.clamp();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
