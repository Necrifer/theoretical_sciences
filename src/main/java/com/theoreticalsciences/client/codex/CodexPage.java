package com.theoreticalsciences.client.codex;

import com.theoreticalsciences.client.screen.HoloStyle;
import com.theoreticalsciences.client.screen.TextFit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * Round 30: one page of a Codex entry, laid out for a given width as a column of pieces (a heading, items in the
 * spotlight, wrapped text, a recipe, a structure viewed layer by layer). Everything is measured before it is drawn,
 * so the page knows its full height: the screen scrolls it when it is taller than the window.
 */
public final class CodexPage {
    /** What the mouse is over: an item (tooltip, and a click opens its entry), text, or a link. */
    public static final class Hover {
        public ItemStack stack = ItemStack.EMPTY;
        public List<Component> lines;
        public String link;
    }

    /** A piece of the page. Coordinates are relative to the piece's own top left. */
    private interface Piece {
        int height();

        void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h);

        /** A click at (mx, my) relative to the piece: a link to follow, "" if handled here, null if not. */
        default String click(double mx, double my) {
            return null;
        }
    }

    private final List<Piece> pieces = new ArrayList<>();
    private final List<Integer> tops = new ArrayList<>();
    public final int width;
    private int height;
    private final int accent;

    private void add(Piece p, int gapAfter) {
        tops.add(height);
        pieces.add(p);
        height += p.height() + gapAfter;
    }

    public int height() {
        return Math.max(0, height);
    }

    /** A page of the entry; {@code recipe} is the recipe of an automatic "how it is made" page (else null). */
    public CodexPage(Font f, CodexData.Page p, Object recipe, int width, int accent, int recipeNo, int recipeCount) {
        this.width = width;
        this.accent = accent;
        switch (p.type) {
            case "spotlight" -> {
                List<ItemStack> items = new ArrayList<>();
                for (String id : p.items) {
                    ItemStack s = stack(id);
                    if (!s.isEmpty()) items.add(s);
                }
                if (!items.isEmpty()) add(new Items_(items), 4);
                String t = !p.title.isEmpty() ? p.title : items.size() == 1 ? items.get(0).getHoverName().getString() : "";
                if (!t.isEmpty()) add(heading(f, Component.literal(t)), 4);
                text(f, p.text);
            }
            case "multiblock" -> {
                if (!p.name.isEmpty()) add(heading(f, Component.literal(p.name)), 4);
                if (!p.pattern.isEmpty()) add(new Structure(f, p.pattern, p.mapping), 6);
                text(f, p.text);
            }
            case "recipe", "auto_recipe" -> {
                Component t = recipeCount > 1 ? Component.translatable("gui.theoreticalsciences.codex.recipe_n", recipeNo, recipeCount)
                        : Component.translatable("gui.theoreticalsciences.codex.recipe");
                add(heading(f, t), 6);
                List<Object> rs = new ArrayList<>();
                if (recipe != null) rs.add(recipe);
                for (String id : p.recipes) {
                    Object r = CodexRecipes.byId(id);
                    if (r != null) rs.add(r);
                    else text(f, Component.translatable("gui.theoreticalsciences.codex.no_recipe", id).getString());
                }
                for (Object r : rs) {
                    CodexRecipes.View v = CodexRecipes.view(r, width, f);
                    if (v != null) add(new RecipePiece(v), 8);
                }
                text(f, p.text);
            }
            default -> {
                if (!p.title.isEmpty()) add(heading(f, Component.literal(p.title)), 4);
                text(f, p.text);
            }
        }
    }

    private void text(Font f, String s) {
        if (s == null || s.isBlank()) return;
        CodexText.Layout l = CodexText.layout(f, s, width);
        add(new Piece() {
            public int height() {
                return l.height();
            }

            public void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h) {
                l.render(g, f, x, y, mx, my);
                String link = l.linkAt(mx - x, my - y);
                if (link != null && my >= y && my < y + l.height()) h.link = link;
            }

            public String click(double mx, double my) {
                return l.linkAt(mx, my);
            }
        }, 6);
    }

    private Piece heading(Font f, Component t) {
        Component c = t.copy().withStyle(Style.EMPTY.withBold(true).withColor(HoloStyle.bright(accent) & 0xFFFFFF));
        List<FormattedCharSequence> lines = f.split(c, width);
        return new Piece() {
            public int height() {
                return lines.size() * 10 + 3;
            }

            public void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h) {
                for (int i = 0; i < lines.size(); i++) g.drawString(f, lines.get(i), x + (width - f.width(lines.get(i))) / 2, y + i * 10, 0xFFFFFFFF, false);
                int ly = y + lines.size() * 10 + 1;
                g.fill(x + width / 4, ly, x + width * 3 / 4, ly + 1, HoloStyle.argb(0x70, accent));
            }
        };
    }

    public static ItemStack stack(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item it = BuiltInRegistries.ITEM.get(rl);
        return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
    }

    /** Spotlight items: one shown large, several in a row (wrapping). */
    private final class Items_ implements Piece {
        final List<ItemStack> items;
        final int size, cols;

        Items_(List<ItemStack> items) {
            this.items = items;
            this.size = items.size() == 1 ? 34 : 20;
            this.cols = Math.max(1, Math.min(items.size(), width / size));
        }

        public int height() {
            return ((items.size() + cols - 1) / cols) * size;
        }

        public void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h) {
            int rowW = cols * size;
            int x0 = x + (width - rowW) / 2;
            for (int i = 0; i < items.size(); i++) {
                int sx = x0 + (i % cols) * size, sy = y + (i / cols) * size;
                HoloStyle.chamferFill(g, sx, sy, size - 2, size - 2, 3, HoloStyle.argb(0x50, accent));
                HoloStyle.chamferFrame(g, sx, sy, size - 2, size - 2, 3, HoloStyle.argb(0xA0, accent));
                float k = (size - 4) / 16F;
                g.pose().pushPose();
                g.pose().translate(sx + 1, sy + 1, 0);
                g.pose().scale(k, k, 1F);
                g.renderItem(items.get(i), 0, 0);
                g.pose().popPose();
                if (mx >= sx && my >= sy && mx < sx + size - 2 && my < sy + size - 2) h.stack = items.get(i);
            }
        }

        public String click(double mx, double my) {
            int rowW = cols * size;
            int x0 = (width - rowW) / 2;
            int c = (int) Math.floor((mx - x0) / size), r = (int) Math.floor(my / size);
            int i = r * cols + c;
            if (mx < x0 || c >= cols || i < 0 || i >= items.size()) return null;
            String e = CodexData.entryForItem(BuiltInRegistries.ITEM.getKey(items.get(i).getItem()).toString());
            return e == null ? "" : e;
        }
    }

    private final class RecipePiece implements Piece {
        final CodexRecipes.View v;

        RecipePiece(CodexRecipes.View v) {
            this.v = v;
        }

        public int height() {
            return v.height;
        }

        public void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h) {
            CodexRecipes.Slot s = CodexRecipes.render(g, f, v, x, y, width, mx, my, accent);
            if (s != null && !s.current().isEmpty()) h.stack = s.current();
        }

        public String click(double mx, double my) {
            for (CodexRecipes.Slot s : v.slots) {
                int sx = s.x(), sy = 14 + s.y();
                if (mx >= sx && my >= sy && mx < sx + s.size() && my < sy + s.size()) {
                    ItemStack st = s.current();
                    if (st.isEmpty()) return "";
                    String e = CodexData.entryForItem(BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
                    return e == null ? "" : e;
                }
            }
            return null;
        }
    }

    /**
     * A multiblock, one layer at a time (bottom first), with the blocks it needs listed below. The pattern is
     * Patchouli's: a list of layers from the top down, each a list of rows; a letter stands for what its mapping says
     * (a block, or a tag "#..." of blocks any of which fits); "_" is anything, a space is air.
     */
    private final class Structure implements Piece {
        final List<List<String>> layers;
        final Map<Character, List<ItemStack>> what = new HashMap<>();
        final Map<Character, Integer> counts = new LinkedHashMap<>();
        final int cols, rows, cell;
        int layer;           // 0 = the bottom layer
        final int listRows, listCols;
        final List<Character> listed = new ArrayList<>();

        Structure(Font f, List<List<String>> layers, Map<String, String> mapping) {
            this.layers = layers;
            int c = 1, r = 1;
            for (List<String> l : layers) {
                r = Math.max(r, l.size());
                for (String row : l) {
                    c = Math.max(c, row.length());
                    for (char ch : row.toCharArray()) if (ch != ' ' && ch != '_') counts.merge(ch, 1, Integer::sum);
                }
            }
            cols = c;
            rows = r;
            cell = Math.max(3, Math.min(16, width / cols));
            for (Map.Entry<String, String> m : mapping.entrySet()) {
                if (m.getKey().isEmpty()) continue;
                what.put(m.getKey().charAt(0), resolve(m.getValue()));
            }
            for (Character ch : counts.keySet()) if (what.containsKey(ch) && !what.get(ch).isEmpty()) listed.add(ch);
            listCols = width >= 220 ? 2 : 1;
            listRows = (listed.size() + listCols - 1) / listCols;
        }

        List<ItemStack> resolve(String v) {
            String s = v.trim();
            int b = s.indexOf('[');
            if (b >= 0) s = s.substring(0, b);
            b = s.indexOf('{');
            if (b >= 0) s = s.substring(0, b);
            List<ItemStack> out = new ArrayList<>();
            if (s.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(s.substring(1));
                if (rl != null)
                    BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, rl)).ifPresent(set -> set.forEach(hb -> {
                        Item it = hb.value().asItem();
                        if (it != Items.AIR) out.add(new ItemStack(it));
                    }));
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl != null) {
                    Block bl = BuiltInRegistries.BLOCK.get(rl);
                    Item it = bl.asItem();
                    if (it == Items.AIR) it = BuiltInRegistries.ITEM.get(rl);
                    if (it != Items.AIR) out.add(new ItemStack(it));
                }
            }
            return out;
        }

        ItemStack shown(char ch) {
            List<ItemStack> l = what.get(ch);
            if (l == null || l.isEmpty()) return ItemStack.EMPTY;
            return l.get((int) ((System.currentTimeMillis() / 1200) % l.size()));
        }

        int gridH() {
            return rows * cell;
        }

        public int height() {
            return 14 + gridH() + 6 + 12 + listRows * 18;
        }

        public void render(GuiGraphics g, Font f, int x, int y, int mx, int my, Hover h) {
            int n = layers.size();
            // the layer controls
            Component lt = Component.translatable("gui.theoreticalsciences.codex.layer", layer + 1, n);
            g.drawCenteredString(f, lt, x + width / 2, y + 2, 0xFFD8E4F0);
            boolean up = layer < n - 1, down = layer > 0;
            button(g, f, x, y, "▼", down, mx, my);
            button(g, f, x + width - 14, y, "▲", up, mx, my);
            // the layer itself
            List<String> rowsOf = layers.get(n - 1 - layer);
            int gx = x + (width - cols * cell) / 2, gy = y + 14;
            g.fill(gx - 1, gy - 1, gx + cols * cell + 1, gy + gridH() + 1, 0x50000810);
            for (int r = 0; r < rowsOf.size(); r++) {
                String row = rowsOf.get(r);
                for (int c = 0; c < row.length(); c++) {
                    char ch = row.charAt(c);
                    int sx = gx + c * cell, sy = gy + r * cell;
                    if ((r + c) % 2 == 0) g.fill(sx, sy, sx + cell, sy + cell, 0x14FFFFFF);
                    if (ch == ' ' || ch == '_') continue;
                    ItemStack st = shown(ch);
                    if (st.isEmpty()) {
                        g.fill(sx + 1, sy + 1, sx + cell - 1, sy + cell - 1, HoloStyle.argb(0x90, accent));
                        continue;
                    }
                    g.pose().pushPose();
                    g.pose().translate(sx, sy, 0);
                    g.pose().scale(cell / 16F, cell / 16F, 1F);
                    g.renderItem(st, 0, 0);
                    g.pose().popPose();
                    if (mx >= sx && my >= sy && mx < sx + cell && my < sy + cell) {
                        h.stack = st;
                        g.renderOutline(sx, sy, cell, cell, 0xFFFFFFFF);
                    }
                }
            }
            // what it needs
            int ly = gy + gridH() + 6;
            g.drawString(f, Component.translatable("gui.theoreticalsciences.codex.blocks_needed").withStyle(ChatFormatting.GRAY), x, ly, 0xFFB0C0D0, false);
            ly += 12;
            int colW = width / listCols;
            for (int i = 0; i < listed.size(); i++) {
                char ch = listed.get(i);
                int lx = x + (i % listCols) * colW, yy = ly + (i / listCols) * 18;
                ItemStack st = shown(ch);
                g.renderItem(st, lx, yy);
                List<ItemStack> all = what.get(ch);
                Component name = Component.literal(counts.get(ch) + " × ").append(st.getHoverName());
                if (all.size() > 1) name = name.copy().append(Component.literal(" *").withStyle(ChatFormatting.GOLD));
                TextFit.draw(g, f, name, lx + 18, yy + 4, colW - 20, 0xFFD8E4F0);
                if (mx >= lx && my >= yy && mx < lx + colW && my < yy + 18) {
                    h.stack = st;
                    if (all.size() > 1) {
                        h.lines = new ArrayList<>();
                        h.lines.add(Component.translatable("gui.theoreticalsciences.codex.any_of").withStyle(ChatFormatting.GOLD));
                        for (int k = 0; k < Math.min(12, all.size()); k++) h.lines.add(Component.literal(" - ").append(all.get(k).getHoverName()));
                        h.stack = ItemStack.EMPTY;
                    }
                }
            }
        }

        void button(GuiGraphics g, Font f, int x, int y, String s, boolean on, int mx, int my) {
            boolean hov = on && mx >= x && my >= y && mx < x + 14 && my < y + 12;
            g.fill(x, y, x + 14, y + 12, hov ? HoloStyle.argb(0x90, accent) : HoloStyle.argb(on ? 0x40 : 0x14, accent));
            g.renderOutline(x, y, 14, 12, HoloStyle.argb(on ? 0xC0 : 0x40, accent));
            g.drawCenteredString(f, s, x + 7, y + 2, on ? 0xFFFFFFFF : 0xFF606870);
        }

        public String click(double mx, double my) {
            if (my >= 0 && my < 12) {
                if (mx >= 0 && mx < 14 && layer > 0) { layer--; return ""; }
                if (mx >= width - 14 && mx < width && layer < layers.size() - 1) { layer++; return ""; }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------ drawing and clicks for the whole page
    public Hover render(GuiGraphics g, Font f, int x, int y, int mx, int my) {
        Hover h = new Hover();
        for (int i = 0; i < pieces.size(); i++) pieces.get(i).render(g, f, x, y + tops.get(i), mx, my, h);
        return h;
    }

    /** A click relative to the page's top left: the entry to open, "" if the click was used, null otherwise. */
    public String click(double mx, double my) {
        for (int i = 0; i < pieces.size(); i++) {
            int t = tops.get(i);
            Piece p = pieces.get(i);
            if (my >= t && my < t + p.height()) {
                String r = p.click(mx, my - t);
                if (r != null) return r;
            }
        }
        return null;
    }
}
