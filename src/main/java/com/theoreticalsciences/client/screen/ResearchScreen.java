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
        // links
        for (var e : defs.entrySet()) {
            ResearchDefinition d = e.getValue();
            if (!ClientResearch.visible(e.getKey())) continue;
            for (ResourceLocation req : d.requires()) {
                ResearchDefinition p = defs.get(req);
                if (p == null) continue;
                boolean lit = ClientResearch.unlocked.contains(req);
                line(g, nx(p) + ns() / 2, ny(p) + ns() / 2, nx(d) + ns() / 2, ny(d) + ns() / 2, lit ? 0xFF40E0FF : 0x60406080, t);
            }
        }
        ResourceLocation hovered = null;
        for (var e : defs.entrySet()) {
            ResearchDefinition d = e.getValue();
            if (!ClientResearch.visible(e.getKey())) continue;
            int sx = nx(d), sy = ny(d), S = ns();
            if (sx + S < 0 || sy + S < 0 || sx > width || sy > height) continue;      // off screen
            if (mx >= sx && my >= sy && mx < sx + S && my < sy + S) hovered = e.getKey();
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
        for (var w : children())
            if (w instanceof net.minecraft.client.gui.components.AbstractWidget aw && aw.isMouseOver(mx, my)) hovered = null;

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

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color, float t) {
        // orthogonal circuit-style link with a travelling pulse
        int mx = (x0 + x1) / 2;
        g.fill(Math.min(x0, mx), y0, Math.max(x0, mx) + 1, y0 + 1, color);
        g.fill(mx, Math.min(y0, y1), mx + 1, Math.max(y0, y1) + 1, color);
        g.fill(Math.min(mx, x1), y1, Math.max(mx, x1) + 1, y1 + 1, color);
        if ((color >>> 24) == 0xFF) {
            int len = Math.abs(mx - x0) + Math.abs(y1 - y0) + Math.abs(x1 - mx);
            if (len > 0) {
                int p = (int) (t * 2) % len;
                int px, py;
                if (p < Math.abs(mx - x0)) { px = x0 + Integer.signum(mx - x0) * p; py = y0; }
                else if (p < Math.abs(mx - x0) + Math.abs(y1 - y0)) { px = mx; py = y0 + Integer.signum(y1 - y0) * (p - Math.abs(mx - x0)); }
                else { px = mx + Integer.signum(x1 - mx) * (p - Math.abs(mx - x0) - Math.abs(y1 - y0)); py = y1; }
                g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFFFFFFF);
            }
        }
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
