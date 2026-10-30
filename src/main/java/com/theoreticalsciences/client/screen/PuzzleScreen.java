package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.ClientResearch;
import com.theoreticalsciences.network.PuzzlePayload;
import com.theoreticalsciences.research.GlyphRegistry;
import com.theoreticalsciences.research.ResearchDefinition;
import com.theoreticalsciences.research.ResearchPuzzle;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * The derivation of a research entry (round 13).
 * <p>
 * Step 1 - Derive: the situation is described (the law is not named) above a template of the full relation in real
 * notation; every variable, number, function and ± is a box to fill from the tray. Only symbols the player has learned
 * can be used; the Symbols book explains each one. Any arrangement equal up to commutation is accepted.
 * <p>
 * Step 2 - Solve: the law's name is revealed, and the player computes a number from the given values.
 */
public class PuzzleScreen extends Screen {
    private static final int TILE_H = 16;
    private final ResourceLocation id;
    private final Screen parent;
    private List<String> tiles = List.of();
    /** whether each tray tile is a learned glyph */
    private boolean[] known = new boolean[0];
    private EditBox answer;
    private Button submitDerivation, submitAnswer;
    private int left, top, panelW, hintX;
    /** Opened on research already completed: the finished derivation is shown to read again, nothing can be changed. */
    private final boolean review;
    private final ScrollBar trayBar = new ScrollBar(), hintBar = new ScrollBar();
    private int hintTop, hintBottom;

    public PuzzleScreen(ResourceLocation id, Screen parent) {
        super(Component.translatable("puzzle.theoreticalsciences.title"));
        this.id = id;
        this.parent = parent;
        this.review = ClientResearch.unlocked.contains(id) || ClientResearch.has(id, 's');
    }

    /** Step 1 is done (or this is a finished derivation being read again). */
    private boolean isDerived() {
        return review || ClientResearch.has(id, 'd');
    }

    ResearchDefinition def() {
        return ClientResearch.defs.get(id);
    }

    ResearchPuzzle puzzle() {
        ResearchDefinition d = def();
        return d == null ? null : d.puzzle().orElse(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        ResearchPuzzle p = puzzle();
        if (p == null) {
            onClose();
            return;
        }
        MathTypeset.Slot.RECTS.clear();
        setupTemplate(p);
        panelW = Math.min(440, width - 190);
        left = Math.max(8, (width - panelW - 180) / 2);
        top = 28;
        hintX = left + panelW + 14;
        int y = height - 48;
        if (review) {
            addRenderableWidget(new HoloButton(left, y + 22, 100, 18, Component.translatable("gui.back"), 0x40C0FF, b -> onClose()));
            addRenderableWidget(new HoloButton(hintX, top + 14, 160, 18, Component.translatable("puzzle.theoreticalsciences.glossary").withStyle(ChatFormatting.AQUA), 0x40C0FF, b -> minecraft.setScreen(new GlossaryScreen(this, List.of()))));
            hintTop = top + 14 + 22 + 4;
            hintBottom = height - 8;
            return;
        }
        addRenderableWidget(new HoloButton(left, y, 54, 18, Component.translatable("puzzle.theoreticalsciences.undo"), 0x40C0FF, b -> {
            held = null;
            dragging = false;
            if (!filledOrder.isEmpty()) fill(filledOrder.peek(), null);
        }));
        addRenderableWidget(new HoloButton(left + 58, y, 54, 18, Component.translatable("puzzle.theoreticalsciences.clear"), 0x40C0FF, b -> {
            Arrays.fill(slots, null);
            filledOrder.clear();
        }));
        submitDerivation = addRenderableWidget(new HoloButton(left + 116, y, panelW - 116, 18, Component.translatable("puzzle.theoreticalsciences.submit_derivation"), 0x40C0FF, b -> PacketDistributor.sendToServer(new PuzzlePayload(id, 0, built(), 0, ""))));
        answer = new EditBox(font, left, y + 22, 120, 18, Component.translatable("puzzle.theoreticalsciences.answer"));
        answer.setFilter(s -> s.matches("-?[0-9]*[.,]?[0-9]*([eE]-?[0-9]*)?"));
        addRenderableWidget(answer);
        submitAnswer = addRenderableWidget(new HoloButton(left + 124, y + 22, panelW - 124, 18, Component.translatable("puzzle.theoreticalsciences.submit_answer"), 0x40C0FF, b -> {
            try {
                double v = Double.parseDouble(answer.getValue().replace(',', '.'));
                PacketDistributor.sendToServer(new PuzzlePayload(id, 1, List.of(), v, ""));
            } catch (NumberFormatException ignored) {
            }
        }));
        int hy = top + 14;
        ResearchDefinition d = def();
        for (String k : new String[]{"v", "m", "f", "e"}) {
            Component label = k.equals("e")
                    ? Component.translatable("puzzle.theoreticalsciences.hint.e", ResearchService.hintFee(d).getCount(), ResearchService.hintFee(d).getHoverName())
                    : Component.translatable("puzzle.theoreticalsciences.hint." + k, ResearchService.hintInsight(d, k));
            addRenderableWidget(new HoloButton(hintX, hy, 160, 18, label, 0x40C0FF, b -> PacketDistributor.sendToServer(new PuzzlePayload(id, 2, List.of(), 0, k)))).active = !ClientResearch.has(id, k.charAt(0));
            hy += 22;
        }
        addRenderableWidget(new HoloButton(hintX, hy, 160, 18, Component.translatable("puzzle.theoreticalsciences.glossary").withStyle(ChatFormatting.AQUA), 0x40C0FF, b -> minecraft.setScreen(new GlossaryScreen(this, missingGlyphs(p)))));
        hintTop = top + 14 + 5 * 22 + 4;
        hintBottom = height - 8;
        if (minecraft != null && minecraft.player != null && minecraft.player.isCreative()) {
            hintBottom = height - 30;
            addRenderableWidget(new HoloButton(hintX, height - 26, 160, 18, Component.translatable("puzzle.theoreticalsciences.skip").withStyle(ChatFormatting.LIGHT_PURPLE), 0x40C0FF, b -> PacketDistributor.sendToServer(new PuzzlePayload(id, 3, List.of(), 0, ""))));
        }
    }

    /** Required symbols the player has not learned yet. */
    static List<String> missingGlyphs(ResearchPuzzle p) {
        List<String> out = new ArrayList<>();
        for (String g : new TreeSet<>(p.requiredGlyphs())) if (!ClientResearch.knows(g)) out.add(g);
        return out;
    }

    @Override
    public void tick() {
        super.tick();
        ResearchPuzzle p = puzzle();
        boolean derived = isDerived(), solved = ClientResearch.has(id, 's');
        if (submitDerivation != null) submitDerivation.active = !derived && full() && p != null && missingGlyphs(p).isEmpty();
        if (submitAnswer != null) submitAnswer.active = derived && !solved;
        if (answer != null) answer.setEditable(derived && !solved);
        if (review) {
            if (p != null && p != setupFor) setupTemplate(p);
            return;
        }
        int i = 0;
        for (var w : children())
            if (w instanceof Button b && b.getX() == hintX && b.getY() >= top + 14 && b.getY() < top + 14 + 4 * 22 && i < 4) {
                b.active = !ClientResearch.has(id, "vmfe".charAt(i));
                i++;
            }
        // a datapack reload changed the puzzle, or new symbols were learned: rebuild the template and the tray
        if (p != null && (p != setupFor || ClientResearch.glyphs != setupGlyphs)) setupTemplate(p);
        if (solved && minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, width, height, 0xF0060410, 0xF0100828);
    }

    // ------------------------------------------------------------------ the template
    private List<String> form = List.of(), lhs = List.of();
    /** slot k sits at token position slotPos[k] */
    private List<Integer> slotPos = List.of();
    private final Map<Integer, Integer> tokToSlot = new HashMap<>();
    private String[] slots = new String[0];
    private final ArrayDeque<Integer> filledOrder = new ArrayDeque<>();

    private ResearchPuzzle setupFor;
    private Set<String> setupGlyphs;

    private void setupTemplate(ResearchPuzzle p) {
        if (setupFor != null && setupFor != p) {
            slots = new String[0];
            filledOrder.clear();
        }
        setupFor = p;
        setupGlyphs = ClientResearch.glyphs;
        form = p.formTokens();
        lhs = p.lhsTokens();
        slotPos = p.slotPositions();
        tokToSlot.clear();
        for (int k = 0; k < slotPos.size(); k++) tokToSlot.put(slotPos.get(k), k);
        if (slots.length != slotPos.size()) {
            slots = new String[slotPos.size()];
            filledOrder.clear();
        }
        if (review) for (int k = 0; k < slotPos.size(); k++) slots[k] = form.get(slotPos.get(k));
        // the tray: learned tiles; unlearned needed ones stay as locked placeholders, unlearned distractors are left out
        Set<String> needed = new HashSet<>();
        for (int k : slotPos) needed.add(form.get(k));
        List<String> t = new ArrayList<>();
        List<Boolean> kn = new ArrayList<>();
        for (String s : p.tiles(id.toString())) {
            boolean k = ClientResearch.knows(s);
            if (!k && !needed.contains(s)) continue;
            t.add(s);
            kn.add(k);
        }
        tiles = t;
        known = new boolean[kn.size()];
        for (int k = 0; k < known.length; k++) known[k] = kn.get(k);
    }

    private List<String> built() {
        List<String> out = new ArrayList<>(form);
        for (int k = 0; k < slotPos.size(); k++) out.set(slotPos.get(k), slots[k] == null ? "?" : slots[k]);
        return out;
    }

    private boolean full() {
        for (String s : slots) if (s == null) return false;
        return slots.length > 0;
    }

    private int tileW(String tok) {
        return Math.max(20, (int) Math.ceil(MathTypeset.tile(font, tok, 1.2F).w) + 10);
    }

    private boolean[] usedMask() {
        boolean[] used = new boolean[tiles.size()];
        for (String p : slots) {
            if (p == null) continue;
            for (int k = 0; k < tiles.size(); k++)
                if (!used[k] && known[k] && tiles.get(k).equals(p)) {
                    used[k] = true;
                    break;
                }
        }
        return used;
    }

    // ---- vertical layout
    private int goalLines() {
        ResearchPuzzle p = puzzle();
        return p == null ? 0 : Math.min(6, font.split(Component.literal(goalText(p)), panelW).size());
    }

    // ---- the layout, worked out once per change instead of once per tile per frame (round 23)
    private int[] layoutMemo;
    private List<Object> layoutMemoKey;

    private int[] layout() {
        ResearchPuzzle p = puzzle();
        List<Object> key = java.util.Arrays.asList(width, height, System.identityHashCode(p), isDerived(), tiles.size(), System.identityHashCode(givenValues()), System.identityHashCode(setupGlyphs));
        if (layoutMemo == null || !key.equals(layoutMemoKey)) {
            layoutMemo = null;
            int[] l = {equationYRaw(), step2YRaw(), trayViewHRaw(), tilesYRaw()};
            layoutMemo = l;
            layoutMemoKey = key;
        }
        return layoutMemo;
    }

    private int equationY() {
        return layout()[0];
    }

    private int equationYRaw() {
        return givenY() + 13 * givenRows() + 4;
    }

    /** Round 22: the given values are part of the question, right under it. */
    private int givenY() {
        return top + 14 + 10 * goalLines() + 2;
    }

    private Map<String, Double> givenValues() {
        ResearchPuzzle p = puzzle();
        return p == null ? Map.of() : ClientResearch.given(id, p);
    }

    /** "q = 1.602e-19 C" pieces: the variable, then the rest. */
    private String givenRest(ResearchPuzzle p, String var, double v) {
        String u = "";
        String desc = p.vars().get(var);
        if (desc != null) {
            int a = desc.lastIndexOf('('), b = desc.lastIndexOf(')');
            if (a >= 0 && b == desc.length() - 1 && b - a - 1 <= 10) u = " " + desc.substring(a + 1, b);
        }
        return " = " + fmt(v) + u;
    }

    private static final int MAX_GIVEN_ROWS = 3;
    private List<int[]> layoutCache;
    private Object layoutKey;

    /** Where each given value goes: x, row (after a "Given:" label; at most three rows). Worked out once per change. */
    private List<int[]> givenLayout() {
        ResearchPuzzle p = puzzle();
        if (p == null) return List.of();
        Map<String, Double> gv = givenValues();
        List<Object> key = List.of(p, gv, panelW);
        if (key.equals(layoutKey) && layoutCache != null) return layoutCache;
        List<int[]> out = new ArrayList<>();
        int x0 = givenLabelW(), x = x0, row = 0;
        for (var en : gv.entrySet()) {
            int w = (int) Math.ceil(MathTypeset.variable(font, en.getKey(), 1F).w) + font.width(givenRest(p, en.getKey(), en.getValue())) + 12;
            if (x > x0 && x + w > panelW && row < MAX_GIVEN_ROWS - 1) {
                x = x0;
                row++;
            }
            out.add(new int[]{x, row});
            x += w;
        }
        layoutCache = out;
        layoutKey = key;
        return out;
    }

    private int givenLabelW() {
        return font.width(Component.translatable("puzzle.theoreticalsciences.given")) + 6;
    }

    private int givenRows() {
        List<int[]> l = givenLayout();
        return l.isEmpty() ? 0 : l.get(l.size() - 1)[1] + 1;
    }

    /** Tiles wrap short of the scroll bar. */
    private int trayW() {
        return panelW - ScrollBar.W - 4;
    }

    private int trayRows() {
        int rows = 1, x = left;
        for (String t : tiles) {
            int w = tileW(t);
            if (x + w > left + trayW()) {
                rows++;
                x = left;
            }
            x += w + 4;
        }
        return rows;
    }

    private int askLines() {
        ResearchPuzzle p = puzzle();
        if (p == null || p.ask().isEmpty()) return 0;
        return Math.min(4, font.split(Component.literal(askText(p)), panelW).size());
    }

    /** Top of step 2: name, question, given values, then the answer row. */
    private int step2Y() {
        return layout()[1];
    }

    private int step2YRaw() {
        return height - 48 - 8 - 12 - 10 * askLines() - 12 - 12 - 10 * Math.max(0, nameLines() - 1);
    }

    private List<FormattedCharSequence> nameSplit(ResearchPuzzle p) {
        int w = panelW - font.width(Component.translatable("puzzle.theoreticalsciences.step2")) - 8;
        List<FormattedCharSequence> l = font.split(Component.literal(p.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), Math.max(40, w));
        return l.size() > 3 ? l.subList(0, 3) : l;
    }

    private int nameLines() {
        ResearchPuzzle p = puzzle();
        return p == null || p.name().isEmpty() || !isDerived() ? 0 : nameSplit(p).size();
    }

    /** Rows of the tray on screen at once: the equation keeps most of the room, the rest of the tray scrolls. */
    private int trayVisRows() {
        if (isDerived()) return 0;
        int room = step2YRaw() - 8 - equationYRaw() - 70 - 8;
        int rows = Math.max(1, Math.min(trayRows(), Math.max(2, room / (TILE_H + 4))));
        // a small screen: never push the tray into step 2 - keep at least 24 px of template and one row of tiles
        int fit = Math.max(1, (step2YRaw() - 8 - equationYRaw() - 24 - 8) / (TILE_H + 4));
        return Math.min(rows, fit);
    }

    private int trayViewH() {
        return layout()[2];
    }

    private int trayViewHRaw() {
        return trayVisRows() * (TILE_H + 4);
    }

    private int templateH() {
        int tray = isDerived() ? 0 : trayViewHRaw() + 8;
        return Math.max(24, step2YRaw() - 8 - tray - equationYRaw());
    }

    private void layoutTray() {
        trayBar.set(left + panelW - ScrollBar.W, tilesY(), Math.max(1, trayViewH() - 4), trayRows() * (TILE_H + 4), trayViewH());
    }

    /** The tile is inside the visible part of the tray. */
    private boolean shown(int[] r) {
        return r[1] >= tilesY() - 1 && r[1] + TILE_H <= tilesY() + trayViewH() + 1;
    }

    private int tilesY() {
        return layout()[3];
    }

    private int tilesYRaw() {
        return equationYRaw() + templateH() + 6;
    }

    private List<int[]> tileRects() {
        List<int[]> out = new ArrayList<>();
        int x = left, y = tilesY() - trayBar.value;
        boolean[] used = usedMask();
        for (int k = 0; k < tiles.size(); k++) {
            int w = tileW(tiles.get(k));
            if (x + w > left + trayW()) {
                x = left;
                y += TILE_H + 4;
            }
            out.add(new int[]{x, y, w, used[k] ? 1 : 0});
            x += w + 4;
        }
        return out;
    }

    // ------------------------------------------------------------------ pick and drop
    private String held;
    private int heldFromSlot = -1;
    private int pressTray = -1, pressSlot = -1;
    private double pressX, pressY;
    private boolean dragging;

    private static boolean in(int[] r, double mx, double my) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + TILE_H;
    }

    private int slotAt(double mx, double my) {
        for (var e : MathTypeset.Slot.RECTS.entrySet()) {
            float[] r = e.getValue();
            if (e.getKey() >= 0 && e.getKey() < slots.length && mx >= r[0] - 1 && mx <= r[2] + 1 && my >= r[1] - 1 && my <= r[3] + 1) return e.getKey();
        }
        return -1;
    }

    private void fill(int slot, String tok) {
        if (slot < 0 || slot >= slots.length) return;
        slots[slot] = tok;
        filledOrder.remove(slot);
        if (tok != null) filledOrder.push(slot);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0) {
            layoutTray();
            if (trayBar.click(mx, my) || hintBar.click(mx, my)) return true;
        }
        if (button != 0 || puzzle() == null || isDerived()) return false;
        pressTray = pressSlot = -1;
        dragging = false;
        pressX = mx;
        pressY = my;
        int sl = slotAt(mx, my);
        if (sl >= 0) {
            pressSlot = sl;
            return true;
        }
        List<int[]> tr = tileRects();
        for (int k = 0; k < tr.size(); k++)
            if (tr.get(k)[3] == 0 && known[k] && shown(tr.get(k)) && in(tr.get(k), mx, my)) {
                pressTray = k;
                return true;
            }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != 0) return super.mouseDragged(mx, my, button, dx, dy);
        if (trayBar.drag(my) || hintBar.drag(my)) return true;
        if (!dragging && (pressTray >= 0 || pressSlot >= 0) && Math.hypot(mx - pressX, my - pressY) > 3) {
            if (pressSlot >= 0 && slots[pressSlot] != null) {
                held = slots[pressSlot];
                heldFromSlot = pressSlot;
                fill(pressSlot, null);
                dragging = true;
            } else if (pressTray >= 0) {
                held = tiles.get(pressTray);
                heldFromSlot = -1;
                dragging = true;
            }
        }
        return dragging || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button != 0) return super.mouseReleased(mx, my, button);
        if (trayBar.release() | hintBar.release()) return true;
        if (dragging) {
            int target = slotAt(mx, my);
            if (held != null && target >= 0) {
                String prev = slots[target];
                fill(target, held);
                if (heldFromSlot >= 0 && prev != null) fill(heldFromSlot, prev);
            }
            held = null;
            heldFromSlot = -1;
            dragging = false;
            pressTray = pressSlot = -1;
            return true;
        }
        boolean handled = pressTray >= 0 || pressSlot >= 0;
        if (pressSlot >= 0 && slots[pressSlot] != null) fill(pressSlot, null);
        else if (pressTray >= 0)
            for (int k = 0; k < slots.length; k++)
                if (slots[k] == null) {
                    fill(k, tiles.get(pressTray));
                    break;
                }
        pressTray = pressSlot = -1;
        return handled || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        layoutTray();
        if (mx >= hintX - 6) hintBar.scroll(sy, 20);
        else if (!isDerived() && my >= tilesY() - 4) trayBar.scroll(sy, TILE_H + 4);
        else return super.mouseScrolled(mx, my, sx, sy);
        return true;
    }

    private void chip(GuiGraphics g, int x, int y, int w, String tok, int bg, int edge, int col) {
        g.fill(x, y, x + w, y + TILE_H, bg);
        g.renderOutline(x, y, w, TILE_H, edge);
        MathTypeset.Box b = MathTypeset.tile(font, tok, 1.2F);
        MathTypeset.draw(g, font, b, x + (w - b.w) / 2F, y + 12.5F, w - 2, col);
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        ResearchPuzzle p = puzzle();
        ResearchDefinition d = def();
        if (p == null || d == null) return;
        if (form.isEmpty()) setupTemplate(p);
        boolean derived = isDerived(), solved = ClientResearch.has(id, 's');
        g.drawString(font, d.titleComponent(id).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left, 10, 0xFFFFFF, false);
        int y = top + 4;
        g.drawString(font, Component.translatable("puzzle.theoreticalsciences.step1").withStyle(derived ? ChatFormatting.GREEN : ChatFormatting.AQUA), left, y - 4, 0xFFFFFF, false);
        y += 10;
        List<FormattedCharSequence> gl = font.split(Component.literal(goalText(p)), panelW);
        for (int k = 0; k < Math.min(6, gl.size()); k++) {
            g.drawString(font, gl.get(k), left, y, 0xE0E0F0, false);
            y += 10;
        }
        // ---- the given values (this world's own numbers)
        {
            int gy = givenY();
            List<int[]> lay = givenLayout();
            int k = 0;
            for (var en : givenValues().entrySet()) {
                int[] at = lay.get(k++);
                float gx = left + at[0];
                int ry = gy + at[1] * 13;
                MathTypeset.Box vb = MathTypeset.variable(font, en.getKey(), 1F);
                MathTypeset.draw(g, font, vb, gx, ry + 8, 80, 0xFFFFE0A0);
                g.drawString(font, givenRest(p, en.getKey(), en.getValue()), (int) (gx + vb.w), ry + 1, 0xFFE0A0, false);
            }
            if (!lay.isEmpty())
                g.drawString(font, Component.translatable("puzzle.theoreticalsciences.given").withStyle(ChatFormatting.GRAY), left, gy + 1, 0, false);
        }
        // ---- the template
        int ey = equationY(), th = templateH();
        g.fill(left - 2, ey, left + panelW + 2, ey + th, 0x60203050);
        int hover = dragging ? slotAt(mx, my) : -1;
        MathTypeset.Slot.RECTS.clear();
        float sc = 1.7F;
        MathTypeset.Box eq = null;
        for (int tries = 0; tries < 8; tries++) {
            final float fs = sc;
            eq = MathTypeset.template(font, lhs, form, fs, (panelW - 12) / 1F, (tok, s) -> {
                Integer k = tokToSlot.get(tok);
                if (k == null) return MathTypeset.tile(font, form.get(tok), s);
                return new MathTypeset.Slot(font, k, slots[k], s,
                        derived ? 0x60208040 : k == hover ? 0xA0507090 : slots[k] == null ? 0x50101828 : 0x70304868,
                        k == hover ? 0xFFFFE070 : slots[k] == null ? 0xFF6070A0 : 0xFF8098C8);
            });
            if (eq == null || (eq.w <= panelW - 12 && eq.h() <= th - 8)) break;
            sc *= Math.min(0.93F, Math.min((panelW - 12) / eq.w, (th - 8) / eq.h()));
        }
        if (eq != null) {
            float k = Math.min(1F, (panelW - 12) / Math.max(1F, eq.w));
            float base = ey + 4 + eq.asc * k + Math.max(0, (th - 8 - eq.h() * k) / 2F);
            MathTypeset.draw(g, font, eq, left + 6, base, panelW - 12, derived ? 0xFF80FFA0 : 0xFFFFFFFF);
        } else g.drawString(font, "(template error)", left + 6, ey + 6, 0xFF6060, false);
        if (derived) g.drawString(font, Component.translatable("puzzle.theoreticalsciences.derivation_ok").withStyle(ChatFormatting.GREEN), left + panelW - 100, ey + 3, 0, false);
        // ---- the tray
        List<String> missing = missingGlyphs(p);
        if (!derived) {
            layoutTray();
            List<int[]> tr = tileRects();
            g.enableScissor(left - 1, tilesY(), left + trayW() + 1, tilesY() + trayViewH());
            for (int k = 0; k < tr.size(); k++) {
                int[] r = tr.get(k);
                if (r[1] + TILE_H < tilesY() || r[1] > tilesY() + trayViewH()) continue;
                if (!known[k]) {      // a symbol not yet learned: a locked tile
                    chip(g, r[0], r[1], r[2], "?", 0xFF201018, 0xFF803040, 0xFF905060);
                    continue;
                }
                boolean used = r[3] == 1, hov = !used && !dragging && in(r, mx, my) && shown(r);
                chip(g, r[0], r[1], r[2], tiles.get(k), used ? 0x40303040 : hov ? 0xFF406080 : 0xFF203048, used ? 0x40606080 : 0xFF6090C0,
                        used ? 0xFF606070 : 0xFFFFFFFF);
            }
            g.disableScissor();
            trayBar.draw(g, mx, my, 0x40203050, 0xC06080B0, 0xFFA0C0F0);
            if (!missing.isEmpty()) {
                int wy = ey + th - 10;
                g.drawString(font, Component.translatable("puzzle.theoreticalsciences.missing_count", missing.size()).withStyle(ChatFormatting.RED), left + 2, wy, 0, false);
            } else if (!full())
                g.drawString(font, Component.translatable("puzzle.theoreticalsciences.drop_here").withStyle(ChatFormatting.DARK_GRAY), left + 2, ey + th - 10, 0, false);
        }
        // ---- step 2: the name, the question, the values
        int sy = step2Y();
        g.drawString(font, Component.translatable("puzzle.theoreticalsciences.step2").withStyle(derived ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY), left, sy, 0xFFFFFF, false);
        int ay = sy + 12;
        if (derived && !p.name().isEmpty()) {
            // the law's name wraps beside the label instead of running into the hints (round 23)
            int nx = left + font.width(Component.translatable("puzzle.theoreticalsciences.step2")) + 8;
            List<FormattedCharSequence> nl = nameSplit(p);
            for (int k = 0; k < nl.size(); k++)
                g.drawString(font, nl.get(k), nx, sy + k * 10, 0xFFAA00, false);
            ay += 10 * Math.max(0, nl.size() - 1);
        }
        if (!p.ask().isEmpty()) {
            List<FormattedCharSequence> al = font.split(Component.literal(derived ? askText(p) : "..."), panelW);
            for (int k = 0; k < Math.min(4, al.size()); k++) {
                g.drawString(font, al.get(k), left, ay, derived ? 0xD0D8E8 : 0x505060, false);
                ay += 10;
            }
        }
        // (the given values are at the top now, with the question)
        if (!p.unit().isEmpty()) g.drawString(font, Component.translatable("puzzle.theoreticalsciences.answer_in", p.unit()), left, ay + 2, derived ? 0xFFE0A0 : 0x505060, false);
        if (solved || review) g.drawString(font, Component.translatable(review ? "puzzle.theoreticalsciences.review_note" : "puzzle.theoreticalsciences.solved_note").withStyle(ChatFormatting.GREEN), left + 130, height - 10, 0, false);
        // ---- hints
        g.drawString(font, Component.translatable("puzzle.theoreticalsciences.hints").withStyle(ChatFormatting.LIGHT_PURPLE), hintX, top, 0, false);
        int hw = width - hintX - 8 - ScrollBar.W - 2;
        int hy = hintTop - hintBar.value;
        g.enableScissor(hintX - 2, hintTop, hintX + hw + 2, hintBottom);
        if (!missing.isEmpty() && !derived) {
            hy = wrap(g, Component.translatable("puzzle.theoreticalsciences.missing_glyphs").getString(), hintX, hy, hw, 0xFF8080);
            for (String m : missing) {
                GlyphRegistry.Glyph gg = GlyphRegistry.get(m, true);
                MathTypeset.Box vb = MathTypeset.tile(font, m, 1F);
                MathTypeset.draw(g, font, vb, hintX, hy + 7, 36, 0xFFFFB0B0);
                hy = wrap(g, gg == null ? "?" : GlossaryScreen.unlockText(gg).getString(), hintX + 40, hy, hw - 40, 0xC08080);
            }
        }
        if (review || ClientResearch.has(id, 'v'))
            for (var en : p.vars().entrySet()) {
                MathTypeset.Box vb = MathTypeset.variable(font, en.getKey(), 1F);
                MathTypeset.draw(g, font, vb, hintX, hy + 7, 40, 0xFFA0E0FF);
                hy = wrap(g, ": " + en.getValue(), hintX + (int) vb.w + 1, hy, hw - (int) vb.w - 1, 0xA0E0FF);
            }
        if (review || ClientResearch.has(id, 'm')) hy = wrap(g, "Method: " + p.method(), hintX, hy + 2, hw, 0xC0FFC0);
        if (ClientResearch.has(id, 'f') && !derived) {
            g.drawString(font, "Starts:", hintX, hy + 2, 0xFFE0A0, false);
            int reveal = (slotPos.size() + 2) / 3;
            Map<Integer, float[]> keep = new HashMap<>(MathTypeset.Slot.RECTS);
            MathTypeset.Box sb = MathTypeset.template(font, lhs, form, 0.9F, hw, (tok, s) -> {
                Integer k = tokToSlot.get(tok);
                if (k == null) return MathTypeset.tile(font, form.get(tok), s);
                return new MathTypeset.Slot(font, -1 - k, k < reveal ? form.get(tok) : null, s, 0x40302010, 0xFF806040);
            });
            if (sb != null) {
                float k = Math.min(1F, hw / Math.max(1F, sb.w));
                MathTypeset.draw(g, font, sb, hintX, hy + 14 + sb.asc * k, hw, 0xFFFFE0A0);
                hy += 18 + (int) (sb.h() * k);
            }
            MathTypeset.Slot.RECTS.clear();
            MathTypeset.Slot.RECTS.putAll(keep);
        }
        if (ClientResearch.has(id, 'e')) {
            MathTypeset.Box fb = MathTypeset.equation(font, lhs, form, 0.9F, hw);
            if (fb != null) {
                float k = Math.min(1F, hw / Math.max(1F, fb.w));
                MathTypeset.draw(g, font, fb, hintX, hy + 4 + fb.asc * k, hw, 0xFFFFD060);
                hy += 8 + (int) (fb.h() * k);
            }
        }
        g.disableScissor();
        hintBar.set(hintX + hw + 2, hintTop, Math.max(1, hintBottom - hintTop), hy + hintBar.value - hintTop, hintBottom - hintTop);
        hintBar.draw(g, mx, my, 0x40302040, 0xC0A060C0, 0xFFE0A0FF);
        // ---- tooltips: hovering a tray tile or a filled box names the symbol
        if (!dragging) {
            String hov = null;
            List<int[]> tr = tileRects();
            for (int k = 0; k < tr.size() && !derived; k++) if (shown(tr.get(k)) && in(tr.get(k), mx, my)) hov = known[k] ? tiles.get(k) : null;
            int sl = slotAt(mx, my);
            if (sl >= 0 && slots[sl] != null) hov = slots[sl];
            GlyphRegistry.Glyph gg = hov == null ? null : GlyphRegistry.get(hov, true);
            if (gg != null)
                TextFit.tooltip(g, font, List.of(Component.literal(gg.name()).withStyle(ChatFormatting.AQUA), Component.literal(gg.meaning()).withStyle(ChatFormatting.GRAY)), mx, my);
        }
        if (dragging && held != null) {
            int w = tileW(held);
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            chip(g, mx - w / 2, my - TILE_H / 2, w, held, 0xE0507090, 0xFFFFE070, 0xFFFFFFFF);
            g.pose().popPose();
        }
    }

    private int wrap(GuiGraphics g, String s, int x, int y, int w, int col) {
        for (FormattedCharSequence line : font.split(Component.literal(s), Math.max(60, w))) {
            g.drawString(font, line, x, y, col, false);
            y += 10;
        }
        return y + 2;
    }

    static String fmt(double v) {
        return ResearchPuzzle.show(v);
    }

    /** The question and the step-2 text with this world's values written in. */
    private String goalText(ResearchPuzzle p) {
        return ResearchPuzzle.fill(p.goal(), givenValues());
    }

    private String askText(ResearchPuzzle p) {
        return ResearchPuzzle.fill(p.ask(), givenValues());
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
