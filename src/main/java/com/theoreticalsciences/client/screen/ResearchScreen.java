package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.ClientResearch;
import com.theoreticalsciences.network.UnlockResearchPayload;
import com.theoreticalsciences.research.ResearchDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Pannable research tree. Click an available node to research it (costs Insight and items). */
public class ResearchScreen extends Screen {
    private static final int SPACING_X = 44, SPACING_Y = 34, NODE = 22;
    private static double panX = Double.NaN, panY;
    /** Round 23: the map zooms (mouse wheel, + and -, or the buttons). */
    private static double zoom = 1.0;
    private static final double ZMIN = 0.3, ZMAX = 2.5;
    private boolean dragging;

    public ResearchScreen() {
        super(Component.translatable("gui.theoreticalsciences.research"));
    }

    @Override
    protected void init() {
        if (Double.isNaN(panX)) {
            panX = width / 2.0 - 120;
            panY = height / 2.0 - 60;
            // 1.4.0: the map runs left to right by depth; open on the first node, a third of the way across
            ResearchDefinition first = null;
            for (ResearchDefinition d : ClientResearch.defs.values())
                if (first == null || d.x() < first.x() || (d.x() == first.x() && Math.abs(d.y()) < Math.abs(first.y()))) first = d;
            if (first != null) {
                panX = width / 3.0 - first.x() * SPACING_X * zoom;
                panY = height / 2.0 - first.y() * SPACING_Y * zoom;
            }
        }
        clampPan();
        // the book of symbols: every glyph a derivation can use, and how to learn the ones still missing
        addRenderableWidget(new HoloButton(width - 108, height - 24, 100, 18, Component.translatable("puzzle.theoreticalsciences.glossary"), 0x40C0FF, b -> minecraft.setScreen(new GlossaryScreen(this, java.util.List.of()))));
        addRenderableWidget(new HoloButton(8, height - 24, 18, 18, Component.literal("+"), 0x40C0FF, b -> zoomAt(1.25, width / 2.0, height / 2.0)).tip(Component.translatable("gui.theoreticalsciences.zoom_tip")));
        addRenderableWidget(new HoloButton(28, height - 24, 18, 18, Component.literal("\u2212"), 0x40C0FF, b -> zoomAt(1 / 1.25, width / 2.0, height / 2.0)).tip(Component.translatable("gui.theoreticalsciences.zoom_tip")));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int nx(ResearchDefinition d) {
        return (int) (panX + d.x() * SPACING_X * zoom);
    }

    private int ny(ResearchDefinition d) {
        return (int) (panY + d.y() * SPACING_Y * zoom);
    }

    /** A node's size on screen. */
    private int ns() {
        return Math.max(4, (int) Math.round(NODE * zoom));
    }

    /** Zoom by a factor, keeping the point under (cx, cy) where it is. */
    private void zoomAt(double factor, double cx, double cy) {
        double z = Math.max(ZMIN, Math.min(ZMAX, zoom * factor));
        double k = z / zoom;
        panX = cx - (cx - panX) * k;
        panY = cy - (cy - panY) * k;
        zoom = z;
        clampPan();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, width, height, 0xF0060410, 0xF0100828);
        float t = (minecraft.level != null ? minecraft.level.getGameTime() : 0) + partial;
        Random r = new Random(99);
        for (int i = 0; i < 220; i++) {
            int sx = (int) ((r.nextInt(width * 2) + panX * 0.2) % width);
            int sy = (int) ((r.nextInt(height * 2) + panY * 0.2) % height);
            if (sx < 0) sx += width;
            if (sy < 0) sy += height;
            int a = (int) (90 + 80 * Math.sin(t * 0.05 + i));
            g.fill(sx, sy, sx + 1, sy + 1, (a << 24) | 0xCCDDFF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        Map<ResourceLocation, ResearchDefinition> defs = ClientResearch.defs;
        float t = (minecraft.level != null ? minecraft.level.getGameTime() : 0) + partial;
        // 1.4.0: the map is a tech tree in bands, one per field; hovering a node lights its whole chain
        Graph gr = graph(defs);
        ResourceLocation hovered = null;
        for (var e : defs.entrySet()) {
            if (!ClientResearch.visible(e.getKey())) continue;
            int sx = nx(e.getValue()), sy = ny(e.getValue()), S = ns();
            if (mx >= sx && my >= sy && mx < sx + S && my < sy + S && my > 22) hovered = e.getKey();
        }
        for (var w : children())
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw && aw.isMouseOver(mx, my)) hovered = null;
        java.util.Set<ResourceLocation> focus = hovered == null ? null : gr.focus(hovered);
        // the bands
        for (var b : gr.bands.entrySet()) {
            int y0 = (int) (panY + b.getValue()[0] * SPACING_Y * zoom) - (int) (10 * zoom), y1 = (int) (panY + b.getValue()[1] * SPACING_Y * zoom) + ns() + (int) (10 * zoom);
            if (y1 < 22 || y0 > height) continue;
            int tint = bandTint(b.getKey());
            g.fill(0, Math.max(22, y0), width, y1, 0x30000000 | tint);
            g.fill(0, Math.max(22, y0), width, Math.max(22, y0) + 1, 0x80000000 | tint);
            int ly = Math.max(26, Math.min(y0 + 2, y1 - 12));
            g.drawString(font, Component.translatableWithFallback("gui.theoreticalsciences.research.field." + b.getKey(),
                    b.getKey().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + b.getKey().substring(1)), 6, ly, 0xC0000000 | brighten(tint), false);
        }
        // the links: direct ones only, through the gaps between columns and rows; a field's links to another field show on hover
        for (var e : gr.direct.entrySet()) {
            ResearchDefinition d = defs.get(e.getKey());
            if (d == null || !ClientResearch.visible(e.getKey())) continue;
            for (ResourceLocation req : e.getValue()) {
                ResearchDefinition p = defs.get(req);
                if (p == null) continue;
                boolean inFocus = focus != null && focus.contains(req) && focus.contains(e.getKey());
                boolean sameField = p.category().equals(d.category());
                if (!sameField && !inFocus) continue;
                boolean lit = ClientResearch.unlocked.contains(req);
                int col = inFocus ? 0xFFFFD060 : focus != null ? 0x30406080 : lit ? 0xC040E0FF : 0x70406080;
                if (!inFocus) route(g, p, d, col);
            }
        }
        if (focus != null)
            for (var e : gr.direct.entrySet())
                if (focus.contains(e.getKey()))
                    for (ResourceLocation req : e.getValue())
                        if (focus.contains(req) && defs.containsKey(req) && ClientResearch.visible(e.getKey())) route(g, defs.get(req), defs.get(e.getKey()), 0xFFFFD060);
        for (var e : defs.entrySet()) {
            ResearchDefinition d = e.getValue();
            if (!ClientResearch.visible(e.getKey())) continue;
            int sx = nx(d), sy = ny(d), S = ns();
            if (sx + S < 0 || sy + S < 0 || sx > width || sy > height) continue;      // off screen
            // each node is drawn at its normal size, scaled about its corner
            g.pose().pushPose();
            g.pose().translate(sx, sy, 0);
            g.pose().scale((float) (S / (double) NODE), (float) (S / (double) NODE), 1F);
            int x = 0, y = 0;
            boolean done = ClientResearch.unlocked.contains(e.getKey());
            boolean avail = ClientResearch.available(e.getKey());
            int pulse = (int) (0xA0 + 0x5F * Math.sin(t * 0.2));
            int border = done ? 0xFF40FFA0 : avail ? (0xFF000000 | (pulse << 16) | (pulse * 3 / 4 << 8) | 0x40) : 0xFF404858;
            g.fill(x, y, x + NODE, y + NODE, done ? 0xE0103828 : avail ? 0xE0302810 : 0xE0101018);
            g.renderOutline(x, y, NODE, NODE, border);
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(d.icon()));
            g.renderItem(icon, x + 3, y + 3);
            if (!done && !avail && !e.getKey().equals(ClientResearch.active)) g.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0x90000000);
            if (e.getKey().equals(ClientResearch.active) && ClientResearch.activeTotal > 0) {
                float f = 1F - ClientResearch.activeRemaining() / (float) ClientResearch.activeTotal;
                g.renderOutline(x - 2, y - 2, NODE + 4, NODE + 4, 0xFF40C0FF);
                g.fill(x, y + NODE + 2, x + NODE, y + NODE + 4, 0xFF102030);
                g.fill(x, y + NODE + 2, x + (int) (NODE * f), y + NODE + 4, 0xFF40E0FF);
            }
            if (focus != null && !focus.contains(e.getKey())) g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, 0xB0050410);
            else if (focus != null && !e.getKey().equals(hovered)) g.renderOutline(x - 1, y - 1, NODE + 2, NODE + 2, 0xFFFFD060);
            g.pose().popPose();
        }
        // header
        g.fill(0, 0, width, 22, 0xC0000000);
        g.drawString(font, title, 8, 7, 0x40E0FF, false);
        Component ins = Component.translatable("gui.theoreticalsciences.insight", ClientResearch.insight, ClientResearch.observations);
        g.drawString(font, ins, width - 8 - font.width(ins), 7, 0xFFD060, false);
        if (ClientResearch.active != null && defs.containsKey(ClientResearch.active)) {
            Component st = Component.translatable("gui.theoreticalsciences.studying", defs.get(ClientResearch.active).titleComponent(ClientResearch.active),
                    com.theoreticalsciences.research.ResearchService.formatTime(ClientResearch.activeRemaining()));
            g.drawCenteredString(font, st, width / 2, 7, 0x80E0FF);
        }
        if (defs.isEmpty()) g.drawCenteredString(font, Component.translatable("gui.theoreticalsciences.no_research"), width / 2, height / 2, 0xAAAAAA);
        // the buttons stay on top of the map (and a node under one is not "hovered")
        for (var r : renderables) r.render(g, mx, my, partial);

        if (hovered != null) {
            ResearchDefinition d = defs.get(hovered);
            List<Component> lines = new ArrayList<>();
            lines.add(d.titleComponent(hovered).copy().withStyle(ChatFormatting.AQUA));
            for (String s : wrap(d.descriptionComponent(hovered).getString(), 38)) lines.add(Component.literal(s).withStyle(ChatFormatting.GRAY));
            if (ClientResearch.unlocked.contains(hovered)) {
                lines.add(Component.translatable("gui.theoreticalsciences.researched").withStyle(ChatFormatting.GREEN));
                if (d.puzzle().isPresent()) lines.add(Component.translatable("gui.theoreticalsciences.review_tip").withStyle(ChatFormatting.YELLOW));
            } else if (hovered.equals(ClientResearch.active)) {
                lines.add(Component.translatable("gui.theoreticalsciences.studying_tip", com.theoreticalsciences.research.ResearchService.formatTime(ClientResearch.activeRemaining()))
                        .withStyle(ChatFormatting.AQUA));
                lines.add(Component.translatable("gui.theoreticalsciences.nexus_tip", d.dossierTier()).withStyle(ChatFormatting.DARK_AQUA));
            } else {
                if (d.time() > 0)
                    lines.add(Component.translatable("gui.theoreticalsciences.cost_time", com.theoreticalsciences.research.ResearchService.formatTime(d.time()))
                            .withStyle(ChatFormatting.BLUE));
                if (d.insight() > 0)
                    lines.add(Component.translatable("gui.theoreticalsciences.cost_insight", d.insight())
                            .withStyle(ClientResearch.insight >= d.insight() ? ChatFormatting.YELLOW : ChatFormatting.RED));
                for (SizedIngredient s : d.cost()) {
                    ItemStack[] items = s.ingredient().getItems();
                    if (items.length > 0) lines.add(Component.literal(" - " + s.count() + "x ").append(items[0].getHoverName()).withStyle(ChatFormatting.GOLD));
                }
                for (ResourceLocation req : d.requires())
                    if (!ClientResearch.unlocked.contains(req) && defs.containsKey(req))
                        lines.add(Component.translatable("gui.theoreticalsciences.requires", defs.get(req).titleComponent(req)).withStyle(ChatFormatting.RED));
                if (ClientResearch.active != null && ClientResearch.available(hovered))
                    lines.add(Component.translatable("gui.theoreticalsciences.busy").withStyle(ChatFormatting.RED));
                else if (ClientResearch.available(hovered)) {
                    if (ClientResearch.needsProof(hovered))
                        lines.add(Component.translatable(ClientResearch.has(hovered, 'd') ? "gui.theoreticalsciences.puzzle_derived_tip" : "gui.theoreticalsciences.puzzle_tip").withStyle(ChatFormatting.YELLOW));
                    lines.add(Component.translatable("gui.theoreticalsciences.click_to_research").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            TextFit.tooltip(g, font, lines, mx, my);
        }
    }

    // ------------------------------------------------------------------ 1.4.0: the tech tree's structure
    /** Direct links (a prerequisite that another prerequisite already needs is left out), and each field's rows. */
    private static final class Graph {
        final Map<ResourceLocation, List<ResourceLocation>> direct = new java.util.HashMap<>();
        final Map<ResourceLocation, java.util.Set<ResourceLocation>> ancestors = new java.util.HashMap<>();
        final Map<ResourceLocation, List<ResourceLocation>> unlocks = new java.util.HashMap<>();
        final Map<String, int[]> bands = new java.util.LinkedHashMap<>();

        java.util.Set<ResourceLocation> ancestors(Map<ResourceLocation, ResearchDefinition> defs, ResourceLocation id, java.util.Set<ResourceLocation> path) {
            java.util.Set<ResourceLocation> got = ancestors.get(id);
            if (got != null) return got;
            java.util.Set<ResourceLocation> out = new java.util.HashSet<>();
            ResearchDefinition d = defs.get(id);
            if (d != null && path.add(id)) {
                for (ResourceLocation r : d.requires())
                    if (defs.containsKey(r)) {
                        out.add(r);
                        out.addAll(ancestors(defs, r, path));
                    }
                path.remove(id);
            }
            ancestors.put(id, out);
            return out;
        }

        /** What a hovered node shows: everything it needs, all the way back, and what it opens next. */
        java.util.Set<ResourceLocation> focus(ResourceLocation id) {
            java.util.Set<ResourceLocation> f = new java.util.HashSet<>(ancestors.getOrDefault(id, java.util.Set.of()));
            f.add(id);
            f.addAll(unlocks.getOrDefault(id, List.of()));
            return f;
        }
    }

    private static Graph cached;
    private static Map<ResourceLocation, ResearchDefinition> cachedFor;
    private static int cachedSize = -1;

    private static Graph graph(Map<ResourceLocation, ResearchDefinition> defs) {
        if (cached != null && cachedFor == defs && cachedSize == defs.size()) return cached;
        Graph gr = new Graph();
        for (ResourceLocation id : defs.keySet()) gr.ancestors(defs, id, new java.util.HashSet<>());
        for (var e : defs.entrySet()) {
            List<ResourceLocation> reqs = new ArrayList<>();
            for (ResourceLocation r : e.getValue().requires()) if (defs.containsKey(r)) reqs.add(r);
            List<ResourceLocation> keep = new ArrayList<>();
            for (ResourceLocation r : reqs) {
                boolean implied = false;
                for (ResourceLocation q : reqs) if (q != r && gr.ancestors.get(q).contains(r)) { implied = true; break; }
                if (!implied) keep.add(r);
            }
            gr.direct.put(e.getKey(), keep);
            for (ResourceLocation r : keep) gr.unlocks.computeIfAbsent(r, k -> new ArrayList<>()).add(e.getKey());
        }
        java.util.TreeMap<Integer, String> order = new java.util.TreeMap<>();
        Map<String, int[]> b = new java.util.HashMap<>();
        for (ResearchDefinition d : defs.values()) {
            int[] r = b.computeIfAbsent(d.category(), k -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
            r[0] = Math.min(r[0], d.y());
            r[1] = Math.max(r[1], d.y());
        }
        for (var e : b.entrySet()) order.put(e.getValue()[0], e.getKey());
        for (String c : order.values()) gr.bands.put(c, b.get(c));
        cached = gr;
        cachedFor = defs;
        cachedSize = defs.size();
        return gr;
    }

    /** A link as the Civilization trees draw them: out of the prerequisite, down or up in the gap after its column,
     *  along the gap between rows just above the target if the target is further away, and into the target. */
    private void route(GuiGraphics g, ResearchDefinition p, ResearchDefinition d, int col) {
        int S = ns();
        int gap = (int) (SPACING_X * zoom) - S;
        int x0 = nx(p) + S, y0 = ny(p) + S / 2;
        int x1 = nx(d), y1 = ny(d) + S / 2;
        int xj = x0 + gap / 2;
        if (d.x() - p.x() <= 3 || x1 - x0 <= gap) {
            hline(g, x0, xj, y0, col);
            vline(g, xj, y0, y1, col);
            hline(g, xj, x1, y1, col);
        } else {
            int ych = ny(d) - ((int) (SPACING_Y * zoom) - S) / 2;
            int xj2 = x1 - gap / 2;
            hline(g, x0, xj, y0, col);
            vline(g, xj, y0, ych, col);
            hline(g, xj, xj2, ych, col);
            vline(g, xj2, ych, y1, col);
            hline(g, xj2, x1, y1, col);
        }
    }

    private static void hline(GuiGraphics g, int a, int b, int y, int col) {
        g.fill(Math.min(a, b), y, Math.max(a, b) + 1, y + 1, col);
    }

    private static void vline(GuiGraphics g, int x, int a, int b, int col) {
        g.fill(x, Math.min(a, b), x + 1, Math.max(a, b) + 1, col);
    }

    private static int bandTint(String field) {
        return switch (field) {
            case "mathematics" -> 0x5040A0;
            case "physics" -> 0x2050A0;
            case "space" -> 0x207080;
            case "chemistry" -> 0x907020;
            case "biology" -> 0x308838;
            case "engineering" -> 0x905830;
            case "anomaly" -> 0x902870;
            default -> 0x404858;
        };
    }

    private static int brighten(int rgb) {
        int r = Math.min(255, ((rgb >> 16) & 255) * 2 + 60), gg = Math.min(255, ((rgb >> 8) & 255) * 2 + 60), b = Math.min(255, (rgb & 255) * 2 + 60);
        return (r << 16) | (gg << 8) | b;
    }

    private static List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : s.split(" ")) {
            if (line.length() + w.length() + 1 > width && !line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(w);
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (var w : children())
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw && aw.isMouseOver(mx, my)) return super.mouseClicked(mx, my, button);
        if (button == 0) {
            for (var e : ClientResearch.defs.entrySet()) {
                if (!ClientResearch.visible(e.getKey())) continue;
                int x = nx(e.getValue()), y = ny(e.getValue());
                if (mx >= x && my >= y && mx < x + ns() && my < y + ns()) {
                    // a finished entry with a derivation: open it again to read the equation, its name and its symbols
                    if (ClientResearch.unlocked.contains(e.getKey()) && e.getValue().puzzle().isPresent()) {
                        minecraft.setScreen(new PuzzleScreen(e.getKey(), this));
                        return true;
                    }
                    if (ClientResearch.available(e.getKey())) {
                        if (ClientResearch.needsProof(e.getKey()))
                            minecraft.setScreen(new PuzzleScreen(e.getKey(), this));
                        else PacketDistributor.sendToServer(new UnlockResearchPayload(e.getKey()));
                    }
                    return true;
                }
            }
            dragging = true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            panX += dx;
            panY += dy;
            clampPan();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    /**
     * The map is larger than the screen: the mouse wheel scrolls it up and down, Shift + wheel (or a sideways wheel /
     * trackpad swipe) scrolls it left and right, and the arrow keys pan too. It never scrolls the map out of sight.
     */
    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        double step = 36;
        if (hasShiftDown()) {
            panX += (scrollY != 0 ? scrollY : scrollX) * step;     // macOS turns shift+wheel into a sideways scroll
        } else if (hasControlDown()) {
            panY += scrollY * step;
            panX += scrollX * step;
        } else if (scrollY != 0) {
            zoomAt(Math.pow(1.15, Math.max(-3, Math.min(3, scrollY))), mx, my);        // the wheel zooms, about the cursor
            return true;
        } else panX += scrollX * step;
        clampPan();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        double step = 40;
        switch (key) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> panX += step;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> panX -= step;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_UP -> panY += step;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN -> panY -= step;
            case org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD -> zoomAt(1.2, width / 2.0, height / 2.0);
            case org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT -> zoomAt(1 / 1.2, width / 2.0, height / 2.0);
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        clampPan();
        return true;
    }

    /** Keep at least part of the research map on screen. */
    private void clampPan() {
        if (ClientResearch.defs.isEmpty()) return;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (ResearchDefinition d : ClientResearch.defs.values()) {
            minX = Math.min(minX, d.x()); maxX = Math.max(maxX, d.x());
            minY = Math.min(minY, d.y()); maxY = Math.max(maxY, d.y());
        }
        double margin = 60;
        double loX = width - margin - maxX * SPACING_X * zoom - ns(), hiX = margin - minX * SPACING_X * zoom;
        double loY = height - margin - maxY * SPACING_Y * zoom - ns(), hiY = margin - minY * SPACING_Y * zoom;
        panX = Math.max(Math.min(loX, hiX), Math.min(Math.max(loX, hiX), panX));
        panY = Math.max(Math.min(loY, hiY), Math.min(Math.max(loY, hiY), panY));
    }
}
