package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.endgame.CodexActionPayload;
import com.theoreticalsciences.endgame.CodexLaws;
import com.theoreticalsciences.endgame.CodexStatePayload;
import com.theoreticalsciences.endgame.EndgameClient;
import com.theoreticalsciences.endgame.LawgiversCodexItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Lawgiver's Codex. A single scrolling page of small option chips, one section per law: gravity, time and weather,
 * spawning, mob strength, creation, rewriting the creature you looked at when you opened it, and restoring natural law.
 * Every change is a request to the server, which checks that you still hold the Codex and answers with the new laws.
 */
public class LawgiverScreen extends Screen {
    private static final int GOLD = 0xFFFFE8A0, TEXT = 0xFFE8E0D0, DIM = 0xFF9890A0, HEAD = 0xFFFFD060;
    private static final int CHIP_H = 14, LABEL_W = 50, ROW_GAP = 3, PAD = 8;
    private static final String[] GRAVITY_LABELS = {"0×", "0.1×", "0.25×", "0.5×", "1×", "2×", "4×"};
    private static final String[] STRENGTH_LABELS = {"0.25×", "0.5×", "1×", "2×", "5×", "10×"};
    /** kept between openings, like a bookmark */
    private static String lastEntity = "minecraft:pig";
    private static int lastCount = 1;

    private final int targetId;
    private final ScrollBar bar = new ScrollBar();
    private EditBox entityBox;
    private List<String> candidates = List.of();
    private List<String> suggestions = List.of();
    private int count = lastCount;
    private int px, py, pw, ph, cx, cy, cw, ch;   // panel and content area
    private int boxContentY;                        // where the entity box sits in content coordinates

    /** One clickable option. */
    private record Chip(Component text, Component tip, boolean selected, boolean enabled, int accent, Runnable click) {}

    private interface Line {}

    private record Heading(Component text) implements Line {}

    private record Row(Component label, List<Chip> chips) implements Line {}

    private record BoxLine() implements Line {}

    private record Gap(int h) implements Line {}

    /** Laid-out chip rectangle in content coordinates. */
    private record Placed(Chip chip, int x, int y, int w) {}

    public LawgiverScreen(int targetId) {
        super(Component.translatable("gui.theoreticalsciences.codex.title"));
        this.targetId = targetId;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 320);
        ph = Math.min(height - 16, 300);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + PAD;
        cy = py + 30;
        cw = pw - PAD * 2 - ScrollBar.W - 4;
        ch = py + ph - PAD - cy;

        List<String> all = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (t == EntityType.PLAYER || t == EntityType.ENDER_DRAGON || !t.canSummon() || !DefaultAttributes.hasSupplier(t)) continue;
            all.add(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString());
        }
        all.sort(null);
        candidates = all;

        entityBox = new EditBox(font, 0, 0, Math.max(60, cw - 4), 16, Component.translatable("gui.theoreticalsciences.codex.entity"));
        entityBox.setMaxLength(100);
        entityBox.setHint(Component.translatable("gui.theoreticalsciences.codex.entity_hint"));
        entityBox.setValue(lastEntity);
        entityBox.setResponder(s -> {
            lastEntity = s;
            refreshSuggestions();
        });
        addWidget(entityBox);
        refreshSuggestions();
        send(CodexLaws.REQUEST, 0, "");
    }

    private void refreshSuggestions() {
        String q = entityBox == null ? "" : entityBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<String> starts = new ArrayList<>(), contains = new ArrayList<>();
        for (String id : candidates) {
            if (id.equals(q)) continue;
            String path = id.substring(id.indexOf(':') + 1);
            if (q.isEmpty() || path.startsWith(q) || id.startsWith(q)) starts.add(id);
            else if (id.contains(q)) contains.add(id);
        }
        starts.addAll(contains);
        suggestions = starts.subList(0, Math.min(8, starts.size()));
    }

    private void send(int action, int arg, String text) {
        PacketDistributor.sendToServer(new CodexActionPayload(action, arg, targetId, text == null ? "" : text));
    }

    private LivingEntity target() {
        if (minecraft == null || minecraft.level == null || targetId < 0) return null;
        Entity e = minecraft.level.getEntity(targetId);
        return e instanceof LivingEntity le && le.isAlive() ? le : null;
    }

    @Override
    public void tick() {
        // the Codex has to stay in hand while it is open
        if (minecraft == null || minecraft.player == null
                || !(minecraft.player.getMainHandItem().getItem() instanceof LawgiversCodexItem
                || minecraft.player.getOffhandItem().getItem() instanceof LawgiversCodexItem)) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ content

    private static Component tr(String key, Object... args) {
        return Component.translatable("gui.theoreticalsciences.codex." + key, args);
    }

    private Chip chip(Component text, String tipKey, boolean selected, boolean enabled, int accent, Runnable click) {
        return new Chip(text, tipKey == null ? null : tr(tipKey), selected, enabled, accent, click);
    }

    private List<Line> lines() {
        CodexStatePayload s = EndgameClient.state;
        int grav = s == null ? 4 : s.gravity(), hp = s == null ? 2 : s.health(), dmg = s == null ? 2 : s.damage(), spd = s == null ? 2 : s.speed();
        boolean hostile = s == null || s.hostile(), passive = s == null || s.passive();
        List<Line> out = new ArrayList<>();

        out.add(new Heading(tr("gravity")));
        List<Chip> g = new ArrayList<>();
        for (int i = 0; i < GRAVITY_LABELS.length; i++) {
            int idx = i;
            g.add(chip(Component.literal(GRAVITY_LABELS[i]), i == 0 ? "gravity.zero.tip" : i == 4 ? "gravity.natural.tip" : "gravity.tip",
                    i == grav, true, 0xFF60C0FF, () -> send(CodexLaws.SET_GRAVITY, idx, "")));
        }
        out.add(new Row(Component.empty(), g));

        out.add(new Heading(tr("time_weather")));
        String[] times = {"dawn", "noon", "dusk", "midnight"};
        List<Chip> t = new ArrayList<>();
        for (int i = 0; i < times.length; i++) {
            int idx = i;
            t.add(chip(tr("time." + times[i]), "time.tip", false, true, 0xFFFFD060, () -> send(CodexLaws.SET_TIME, idx, "")));
        }
        out.add(new Row(tr("time"), t));
        String[] weathers = {"clear", "rain", "storm"};
        List<Chip> w = new ArrayList<>();
        for (int i = 0; i < weathers.length; i++) {
            int idx = i;
            w.add(chip(tr("weather." + weathers[i]), "weather.tip", false, true, 0xFF80B0FF, () -> send(CodexLaws.SET_WEATHER, idx, "")));
        }
        out.add(new Row(tr("weather"), w));

        out.add(new Heading(tr("spawning")));
        out.add(new Row(tr("hostile"), List.of(
                chip(tr("on"), "hostile.tip", hostile, true, 0xFF80FF80, () -> send(CodexLaws.SET_HOSTILE, 1, "")),
                chip(tr("off"), "hostile.tip", !hostile, true, 0xFFFF8080, () -> send(CodexLaws.SET_HOSTILE, 0, "")))));
        out.add(new Row(tr("passive"), List.of(
                chip(tr("on"), "passive.tip", passive, true, 0xFF80FF80, () -> send(CodexLaws.SET_PASSIVE, 1, "")),
                chip(tr("off"), "passive.tip", !passive, true, 0xFFFF8080, () -> send(CodexLaws.SET_PASSIVE, 0, "")))));

        out.add(new Heading(tr("strength")));
        out.add(strengthRow("health", hp, CodexLaws.SET_HEALTH));
        out.add(strengthRow("damage", dmg, CodexLaws.SET_DAMAGE));
        out.add(strengthRow("speed", spd, CodexLaws.SET_SPEED));

        out.add(new Heading(tr("create")));
        out.add(new BoxLine());
        if (!suggestions.isEmpty()) {
            List<Chip> sug = new ArrayList<>();
            for (String id : suggestions) {
                String shown = id.startsWith("minecraft:") ? id.substring(10) : id;
                sug.add(new Chip(Component.literal(shown), Component.literal(id), false, true, 0xFFB0A0FF, () -> {
                    entityBox.setValue(id);
                    entityBox.moveCursorToEnd(false);
                }));
            }
            out.add(new Row(tr("suggest"), sug));
        }
        out.add(new Row(tr("count"), List.of(
                chip(Component.literal("-"), "count.tip", false, count > 1, 0xFFC0C0C0, () -> count = lastCount = Math.max(1, count - 1)),
                chip(Component.literal(String.valueOf(count)), "count.tip", true, true, 0xFFC0C0C0, () -> {}),
                chip(Component.literal("+"), "count.tip", false, count < CodexLaws.MAX_CREATE, 0xFFC0C0C0, () -> count = lastCount = Math.min(CodexLaws.MAX_CREATE, count + 1)),
                chip(tr("create.go"), "create.tip", false, !entityBox.getValue().isBlank(), 0xFF80FF80, () -> send(CodexLaws.CREATE, count, entityBox.getValue())))));

        LivingEntity tgt = target();
        out.add(new Heading(tgt == null ? tr("rewrite.none") : tr("rewrite", tgt.getDisplayName())));
        boolean ok = tgt != null, notPlayer = ok && !(tgt instanceof net.minecraft.world.entity.player.Player);
        out.add(new Row(Component.empty(), List.of(
                chip(tr("heal"), "heal.tip", false, ok, 0xFF80FF80, () -> send(CodexLaws.HEAL, 0, "")),
                chip(tr("unmake"), "unmake.tip", false, notPlayer, 0xFFFF6060, () -> send(CodexLaws.UNMAKE, 0, "")),
                chip(tr("pacify"), "pacify.tip", false, notPlayer, 0xFF80FFC0, () -> send(CodexLaws.PACIFY, 0, "")),
                chip(tr("transmute"), "transmute.tip", false, notPlayer && !entityBox.getValue().isBlank(), 0xFFD080FF,
                        () -> send(CodexLaws.TRANSMUTE, 0, entityBox.getValue())),
                chip(tr("grow"), "grow.tip", false, notPlayer, 0xFF60C0FF, () -> send(CodexLaws.GROW, 0, "")),
                chip(tr("shrink"), "shrink.tip", false, notPlayer, 0xFF60C0FF, () -> send(CodexLaws.SHRINK, 0, "")))));

        out.add(new Gap(6));
        out.add(new Row(Component.empty(), List.of(
                chip(tr("restore"), "restore.tip", false, true, 0xFFFFB040, () -> send(CodexLaws.RESTORE, 0, "")))));
        return out;
    }

    private Row strengthRow(String key, int sel, int action) {
        List<Chip> c = new ArrayList<>();
        for (int i = 0; i < STRENGTH_LABELS.length; i++) {
            int idx = i;
            c.add(chip(Component.literal(STRENGTH_LABELS[i]), key + ".tip", i == sel, true, 0xFFFF9060, () -> send(action, idx, "")));
        }
        return new Row(tr(key), c);
    }

    // ------------------------------------------------------------------ layout

    /** Lays out every line in content coordinates (0 = top of the content area). Returns the total height. */
    private int layout(List<Line> lines, List<Placed> chips, List<Object[]> texts) {
        int y = 0;
        for (Line l : lines) {
            if (l instanceof Heading h) {
                if (y > 0) y += 4;
                texts.add(new Object[]{h.text(), 0, y + 2, HEAD});
                y += 14;
            } else if (l instanceof Gap g) {
                y += g.h();
            } else if (l instanceof BoxLine) {
                boxContentY = y;
                y += 16 + ROW_GAP + 1;
            } else if (l instanceof Row r) {
                boolean labelled = !r.label().getString().isEmpty();
                int x0 = labelled ? LABEL_W : 0, x = x0;
                if (labelled) texts.add(new Object[]{r.label(), 0, y + 3, DIM});
                for (Chip c : r.chips()) {
                    int w = font.width(c.text()) + 10;
                    if (x > x0 && x + w > cw) {
                        x = x0;
                        y += CHIP_H + 2;
                    }
                    chips.add(new Placed(c, x, y, w));
                    x += w + 3;
                }
                y += CHIP_H + ROW_GAP;
            }
        }
        return y;
    }

    // ------------------------------------------------------------------ rendering and input

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        g.fill(px, py, px + pw, py + ph, 0xF0120E18);
        g.renderOutline(px, py, pw, ph, GOLD);
        g.renderOutline(px + 2, py + 2, pw - 4, ph - 4, 0x60FFE8A0);
        g.drawCenteredString(font, title, px + pw / 2, py + 7, GOLD);
        if (minecraft != null && minecraft.level != null) {
            String dim = minecraft.level.dimension().location().toString();
            g.drawCenteredString(font, Component.translatable("gui.theoreticalsciences.codex.dimension", dim), px + pw / 2, py + 18, DIM);
        }

        List<Placed> chips = new ArrayList<>();
        List<Object[]> texts = new ArrayList<>();
        int total = layout(lines(), chips, texts);
        bar.set(px + pw - PAD - ScrollBar.W, cy, ch, total, ch);
        int off = -bar.value;

        g.enableScissor(cx - 1, cy, cx + cw + 1, cy + ch);
        for (Object[] t : texts) {
            Component c = (Component) t[0];
            g.drawString(font, font.plainSubstrByWidth(c.getString(), cw), cx + (int) t[1], cy + (int) t[2] + off, (int) t[3], false);
        }
        Chip hovered = null;
        boolean inside = mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
        for (Placed p : chips) {
            int x = cx + p.x(), y = cy + p.y() + off;
            if (y + CHIP_H < cy || y > cy + ch) continue;
            Chip c = p.chip();
            boolean over = inside && mx >= x && mx < x + p.w() && my >= y && my < y + CHIP_H;
            if (over) hovered = c;
            int bg = !c.enabled() ? 0x30101010 : c.selected() ? (c.accent() & 0x00FFFFFF) | 0x70000000 : over ? 0x50FFFFFF : 0x28FFFFFF;
            g.fill(x, y, x + p.w(), y + CHIP_H, bg);
            g.renderOutline(x, y, p.w(), CHIP_H, !c.enabled() ? 0x40808080 : c.selected() || over ? c.accent() : 0x60FFFFFF);
            int col = !c.enabled() ? 0xFF606060 : c.selected() ? 0xFFFFFFFF : TEXT;
            g.drawString(font, c.text(), x + 5, y + 3, col, false);
        }
        // the entity name box scrolls with the page and only takes input while fully visible
        int by = cy + boxContentY + off;
        entityBox.setX(cx + 1);
        entityBox.setY(by);
        entityBox.setWidth(Math.max(60, cw - 2));
        boolean boxVisible = by >= cy && by + 16 <= cy + ch;
        entityBox.setVisible(boxVisible);
        if (!boxVisible && entityBox.isFocused()) entityBox.setFocused(false);
        if (boxVisible) entityBox.render(g, mx, my, pt);
        g.disableScissor();

        bar.draw(g, mx, my, 0x40FFFFFF, 0xA0FFE8A0, 0xFFFFE8A0);

        if (hovered != null && hovered.tip() != null)
            g.renderTooltip(font, font.split(hovered.tip(), 220), mx, my);
        else if (boxVisible && entityBox.isMouseOver(mx, my))
            g.renderTooltip(font, font.split(tr("entity.tip"), 220), mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (bar.click(mx, my)) return true;
        if (button == 0 && mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            List<Placed> chips = new ArrayList<>();
            layout(lines(), chips, new ArrayList<>());
            int off = -bar.value;
            for (Placed p : chips) {
                int x = cx + p.x(), y = cy + p.y() + off;
                if (mx >= x && mx < x + p.w() && my >= y && my < y + CHIP_H) {
                    if (p.chip().enabled()) {
                        p.chip().click().run();
                        if (minecraft != null)
                            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    com.theoreticalsciences.registry.QHSounds.UI_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (bar.drag(my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (bar.release()) return true;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        bar.scroll(sy, 20);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // typing an entity name must not close the screen with the inventory key
        if (entityBox.isFocused() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return entityBox.keyPressed(key, scan, mods) || entityBox.canConsumeInput();
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void removed() {
        lastEntity = entityBox == null ? lastEntity : entityBox.getValue();
        super.removed();
    }
}
