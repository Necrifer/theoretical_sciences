package com.theoreticalsciences.client.codex;

import com.mojang.math.Axis;
import com.theoreticalsciences.client.screen.HoloStyle;
import com.theoreticalsciences.client.screen.TextFit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Round 30: the Quantum Codex - the mod's own book, needing no other mod.
 * <p>
 * <b>The map.</b> Each tab (Discovery, Physics, ... Machines) is a map of nodes: chapters, and the items, blocks,
 * fluids, machines and creatures around them, joined to what they build on. Drag the map (or use the arrow keys) to
 * move it, without limit until the last node is in the middle of the screen; the mouse wheel zooms about the cursor
 * (small trackpad steps zoom smoothly); "Fit" shows the whole map at once, "Centre" goes back to where it starts.
 * Locked nodes (research not done yet) are dark with a padlock; unread ones carry a gold spark.
 * <p>
 * <b>An entry.</b> Clicking a node opens its pages in a panel over the map: text with highlighted names (those with an
 * entry of their own are links), items in the spotlight (hover for the tooltip, click for their entry), recipes drawn
 * from the game's own recipe list, structures layer by layer. Pages scroll when taller than the window; arrows or the
 * buttons turn them; Back returns along the links followed.
 * <p>
 * <b>Search</b> (bottom right, or Ctrl+F) looks through every title and page; matching nodes light up on the map.
 */
public class CodexScreen extends Screen {
    private static final int TOP = 24, BOT = 24, UNIT = 16, BIG = 24, SMALL = 18;
    private static final double ZMAX = 3.0;

    // ---- remembered while the game runs: tab, how each map was left, the open entry
    private static String tabId;
    private static final Map<String, double[]> VIEWS = new HashMap<>();
    private static String entryId;
    private static int pageNo;
    private static final Deque<int[]> HISTORY_PAGES = new ArrayDeque<>();
    private static final Deque<String> HISTORY = new ArrayDeque<>();

    private CodexData.Tab tab;
    private double panX, panY, zoom = 1;
    private EditBox search;
    private final List<Btn> buttons = new ArrayList<>();
    private final List<int[]> tabRects = new ArrayList<>();
    private boolean tabIcons;
    // map interaction
    private boolean pressed, dragging;
    private double pressX, pressY;
    private String pressedNode;
    private final Map<String, ItemStack> icons = new HashMap<>();
    // search
    private List<CodexData.Entry> results = List.of();
    private Set<String> matches = Set.of();
    private int resultScroll;
    // entry
    private CodexData.Entry entry;
    private final List<CodexData.Page> pages = new ArrayList<>();
    private final List<Object> pageRecipes = new ArrayList<>();
    private CodexPage built;
    private int builtFor = -1, builtWidth = -1;
    private double scroll;
    // round 33: the first-open hint banner, and the "Next steps" panel (research to start now, guide steps to read)
    private boolean intro, introPlaced;
    private static boolean showNext;
    private Btn introOk;
    /** What the Next steps panel shows this frame: research ids (ResourceLocation) or entry ids (String). */
    private final List<Object> nextRows = new ArrayList<>();

    public CodexScreen() {
        super(Component.translatable("gui.theoreticalsciences.codex.title"));
    }

    /** Opens the Codex (client side only). */
    public static void open() {
        Minecraft.getInstance().setScreen(new CodexScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return true;          // round 32b: reading the Codex pauses the game (in single player), like any book
    }

    private float time() {
        return (System.currentTimeMillis() % 3_600_000L) / 50F;
    }

    // ================================================================== layout
    private int mapX0() { return 0; }
    private int mapY0() { return TOP; }
    private int mapX1() { return width; }
    private int mapY1() { return height - BOT; }
    private double mapCX() { return (mapX0() + mapX1()) / 2.0; }
    private double mapCY() { return (mapY0() + mapY1()) / 2.0; }

    private int accent() {
        if (tab == null) return 0x40C0FF;
        return switch (tab.id) {
            case "physics" -> 0x5090FF;
            case "chemistry" -> 0x40E0A0;
            case "biology" -> 0x90E040;
            case "mathematics" -> 0xFFC840;
            case "dimensions" -> 0xB070FF;
            case "materials" -> 0xFF9A40;
            case "mobs" -> 0xFF5060;
            case "machines" -> 0x40E0E0;
            default -> 0x40C0FF;
        };
    }

    /** A flat button drawn in the Codex's style. */
    private final class Btn {
        int x, y, w, h;
        final Component label, tip;
        final Runnable action;
        final java.util.function.BooleanSupplier shown;

        Btn(Component label, Component tip, Runnable action, java.util.function.BooleanSupplier shown) {
            this.label = label;
            this.tip = tip;
            this.action = action;
            this.shown = shown;
        }

        boolean over(double mx, double my) {
            return shown.getAsBoolean() && mx >= x && my >= y && mx < x + w && my < y + h;
        }

        void render(GuiGraphics g, int mx, int my) {
            if (!shown.getAsBoolean()) return;
            boolean hov = over(mx, my);
            int a = accent();
            HoloStyle.chamferFill(g, x, y, w, h, 3, hov ? HoloStyle.argb(0xC0, a) : 0xD0081018);
            HoloStyle.chamferFrame(g, x, y, w, h, 3, HoloStyle.argb(hov ? 0xFF : 0xA0, a));
            Component l = label;
            int tw = font.width(l);
            if (tw > w - 4) {
                TextFit.draw(g, font, l, x + 2, y + (h - 8) / 2, w - 4, 0xFFFFFFFF);
            } else g.drawString(font, l, x + (w - tw) / 2, y + (h - 8) / 2, hov ? 0xFFFFFFFF : 0xFFD8E4F0, false);
        }
    }

    private Btn btn(String label, String tip, Runnable r, java.util.function.BooleanSupplier shown) {
        Btn b = new Btn(Component.literal(label), tip == null ? null : Component.translatable(tip), r, shown);
        buttons.add(b);
        return b;
    }

    private Btn btnT(String key, String tip, Runnable r, java.util.function.BooleanSupplier shown) {
        Btn b = new Btn(Component.translatable(key), tip == null ? null : Component.translatable(tip), r, shown);
        buttons.add(b);
        return b;
    }

    @Override
    protected void init() {
        CodexData.ensure();
        buttons.clear();
        if (tab == null) tab = CodexData.tab(tabId);
        if (tab != null) {
            tabId = tab.id;
            loadView();
        }
        // round 33: the very first time, the Codex opens on the Discovery map at "Start Here", with a short hint
        if (!introPlaced && !CodexData.tabs.isEmpty()) {
            introPlaced = true;
            intro = !CodexState.isRead(CodexState.INTRO);
            if (intro) {
                CodexData.Tab d = CodexData.tab("discovery");
                if (d != null && d.byEntry.containsKey(CodexState.START)) {
                    tab = d;
                    tabId = d.id;
                    centre();
                }
                showNext = true;
            }
        }
        // ---- tabs along the top: names when they fit, icons only on narrow screens
        tabRects.clear();
        int total = 0;
        for (CodexData.Tab t : CodexData.tabs) total += 26 + font.width(t.name);
        tabIcons = total + 8 > width;
        int x = 4;
        int iconW = Math.max(16, Math.min(24, (width - 8) / Math.max(1, CodexData.tabs.size())));
        for (CodexData.Tab t : CodexData.tabs) {
            int w = tabIcons ? iconW : 26 + font.width(t.name);
            tabRects.add(new int[]{x, 2, w - 2, TOP - 4});
            x += w;
        }
        // ---- the bottom bar: map controls on the left, search on the right
        java.util.function.BooleanSupplier onMap = () -> entry == null;
        int by = height - BOT + 3, bh = BOT - 6;
        boolean compact = width < 420;
        List<Btn> row = new ArrayList<>();
        row.add(btnT("gui.theoreticalsciences.codex.research", "gui.theoreticalsciences.codex.research_tip",
                () -> minecraft.setScreen(new com.theoreticalsciences.client.screen.ResearchScreen()), onMap));
        row.add(btnT("gui.theoreticalsciences.codex.next_steps", "gui.theoreticalsciences.codex.next_steps_tip", () -> showNext = !showNext, onMap));
        row.add(btnT("gui.theoreticalsciences.codex.fit", "gui.theoreticalsciences.codex.fit_tip", this::fit, onMap));
        row.add(btnT("gui.theoreticalsciences.codex.centre", "gui.theoreticalsciences.codex.centre_tip", this::centre, onMap));
        row.add(btn("−", "gui.theoreticalsciences.codex.zoom_out", () -> zoomAt(1 / 1.25, mapCX(), mapCY()), onMap));
        row.add(btn("+", "gui.theoreticalsciences.codex.zoom_in", () -> zoomAt(1.25, mapCX(), mapCY()), onMap));
        int bx = 4;
        for (Btn b : row) {
            b.w = Math.max(bh, font.width(b.label) + (compact ? 6 : 10));
            b.h = bh;
            b.x = bx;
            b.y = by;
            bx += b.w + 2;
        }
        int sw = Math.max(60, Math.min(180, width - bx - 10));
        String old = search == null ? "" : search.getValue();
        search = new EditBox(font, width - sw - 4, by, sw, bh, Component.translatable("gui.theoreticalsciences.codex.search"));
        search.setHint(Component.translatable("gui.theoreticalsciences.codex.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(64);
        search.setValue(old);
        search.setResponder(this::searchChanged);
        addRenderableWidget(search);
        // ---- the entry panel's buttons
        bBack = btn("◀", "gui.theoreticalsciences.codex.back_tip", this::back, () -> entry != null && !HISTORY.isEmpty());
        bClose = btn("✕", "gui.theoreticalsciences.codex.map_back_tip", this::closeEntry, () -> entry != null);
        bPrev = btn("◀", "gui.theoreticalsciences.codex.prev", () -> turn(-1), () -> entry != null && pageNo > 0);
        bNext = btn("▶", "gui.theoreticalsciences.codex.next", () -> turn(1), () -> entry != null && pageNo < pages.size() - 1);
        placeEntryButtons();
        introOk = btnT("gui.theoreticalsciences.codex.intro_ok", null, this::dismissIntro, () -> intro && entry == null);
        int[] ib = introBox();
        introOk.w = Math.max(40, font.width(introOk.label) + 12);
        introOk.h = 14;
        introOk.x = ib[0] + ib[2] - introOk.w - 6;
        introOk.y = ib[1] + ib[3] - introOk.h - 5;
        if (entryId != null && entry == null) openEntry(entryId, false, pageNo);
        builtFor = -1;
        searchChanged(search.getValue());
    }

    private Btn bBack, bClose, bPrev, bNext;

    /** The entry panel's buttons follow the panel (its height now fits the entry). */
    private void placeEntryButtons() {
        if (bBack == null) return;
        int[] p = panel();
        bBack.x = p[0] + 4; bBack.y = p[1] + 4; bBack.w = 16; bBack.h = 14;
        bClose.x = p[0] + p[2] - 20; bClose.y = p[1] + 4; bClose.w = 16; bClose.h = 14;
        bPrev.x = p[0] + 6; bPrev.y = p[1] + p[3] - 18; bPrev.w = 22; bPrev.h = 14;
        bNext.x = p[0] + p[2] - 28; bNext.y = p[1] + p[3] - 18; bNext.w = 22; bNext.h = 14;
    }

    // round 32c: the panel is as tall as the entry's tallest page needs (so it does not jump between pages), not
    // always the whole window - a two-line entry gets a small panel. Very long pages still scroll at full height.
    private String fitFor;
    private int fitW, fitH;

    private int contentNeed(int cw) {
        if (entry == null) return 0;
        if (entry.id.equals(fitFor) && fitW == cw && pages.size() > 0) return fitH;
        int h = 0, recipes = 0;
        for (Object r : pageRecipes) if (r != null) recipes++;
        int no = 0;
        for (int i = 0; i < pages.size(); i++) {
            if (pageRecipes.get(i) != null) no++;
            h = Math.max(h, new CodexPage(font, pages.get(i), pageRecipes.get(i), cw, accent(), no, recipes).height());
        }
        fitFor = entry.id;
        fitW = cw;
        fitH = h;
        return h;
    }

    /** The entry panel: x, y, w, h. */
    private int[] panel() {
        int pw = Math.min(width - 8, 380);
        int full = height - 8;
        int ph = full;
        if (entry != null) ph = Math.max(Math.min(full, 120), Math.min(full, contentNeed(pw - 24 - 6) + 26 + 24 + 8));
        return new int[]{(width - pw) / 2, (height - ph) / 2, pw, ph};
    }

    // ================================================================== the map's view
    private void loadView() {
        double[] v = VIEWS.get(tab.id);
        if (v == null) centre();
        else {
            panX = v[0];
            panY = v[1];
            zoom = v[2];
            // the window may have changed size: keep the same point in the middle
            if (v.length > 4) {
                panX += mapCX() - v[3];
                panY += mapCY() - v[4];
            }
        }
        clampPan();
    }

    private void saveView() {
        if (tab != null) VIEWS.put(tab.id, new double[]{panX, panY, zoom, mapCX(), mapCY()});
    }

    private double fitZoom() {
        if (tab == null) return 1;
        double bw = (tab.maxX - tab.minX) * UNIT + BIG * 2, bh = (tab.maxY - tab.minY) * UNIT + BIG * 2;
        double aw = Math.max(40, mapX1() - mapX0() - 24), ah = Math.max(40, mapY1() - mapY0() - 24);
        return Math.min(aw / bw, ah / bh);
    }

    private double zMin() {
        return Math.max(0.03, Math.min(0.3, fitZoom()));
    }

    private void fit() {
        if (tab == null) return;
        zoom = Math.max(zMin(), Math.min(1.5, fitZoom()));
        panX = mapCX() - (tab.minX + tab.maxX) / 2.0 * UNIT * zoom;
        panY = mapCY() - (tab.minY + tab.maxY) / 2.0 * UNIT * zoom;
        clampPan();
        saveView();
    }

    /** Back to where the map begins: its left edge (the first discoveries), at normal size. */
    private void centre() {
        if (tab == null) return;
        zoom = 1;
        CodexData.Node start = tab.byEntry.get(CodexState.START);
        if (start != null) {
            // round 33: the Discovery map begins at "Start Here" - the top of the Getting Started column, near the top left
            panX = mapX0() + Math.min(90, (mapX1() - mapX0()) / 4.0) - start.x() * UNIT * zoom;
            panY = mapY0() + 36 - start.y() * UNIT * zoom;
            clampPan();
            saveView();
            return;
        }
        panX = mapX0() + 40 - tab.minX * UNIT * zoom;
        double bh = (tab.maxY - tab.minY) * UNIT * zoom;
        if (bh <= mapY1() - mapY0() - 40) panY = mapCY() - (tab.minY + tab.maxY) / 2.0 * UNIT * zoom;
        else panY = mapY0() + 30 - tab.minY * UNIT * zoom;
        clampPan();
        saveView();
    }

    /** Puts a node in the middle of the map. */
    private void focus(CodexData.Node n) {
        if (zoom < 0.8) zoom = 1;
        panX = mapCX() - n.x() * UNIT * zoom;
        panY = mapCY() - n.y() * UNIT * zoom;
        clampPan();
        saveView();
    }

    /**
     * The map moves freely, but only so far that the outermost node can still be brought to the middle of the screen
     * (and a little beyond) - so every node can always be reached, and the map can never be lost off screen.
     */
    private void clampPan() {
        if (tab == null) return;
        double mx = Math.max(20, (mapX1() - mapX0()) / 2.0 - 30), my = Math.max(20, (mapY1() - mapY0()) / 2.0 - 30);
        double loX = mapCX() - tab.maxX * UNIT * zoom - mx, hiX = mapCX() - tab.minX * UNIT * zoom + mx;
        double loY = mapCY() - tab.maxY * UNIT * zoom - my, hiY = mapCY() - tab.minY * UNIT * zoom + my;
        panX = Math.max(loX, Math.min(hiX, panX));
        panY = Math.max(loY, Math.min(hiY, panY));
    }

    private void zoomAt(double factor, double cx, double cy) {
        double z = Math.max(zMin(), Math.min(ZMAX, zoom * factor));
        double k = z / zoom;
        panX = cx - (cx - panX) * k;
        panY = cy - (cy - panY) * k;
        zoom = z;
        clampPan();
        saveView();
    }

    private void switchTab(CodexData.Tab t) {
        if (t == null || t == tab) return;
        saveView();
        tab = t;
        tabId = t.id;
        loadView();
    }

    /** Colours of the tier bands: I .. VI (cool to warm, then pale gold for the Transcendence). */
    private static final int[] TIER_COLOURS = {0x4C8CFF, 0x50DCC8, 0xFFC850, 0xC878FF, 0xFF6E6E, 0xFFF0C8};

    private double sx(CodexData.Node n) {
        return panX + n.x() * UNIT * zoom;
    }

    private double sy(CodexData.Node n) {
        return panY + n.y() * UNIT * zoom;
    }

    private int size(CodexData.Node n) {
        return Math.max(4, (int) Math.round((n.small() ? SMALL : BIG) * zoom));
    }

    private ItemStack icon(String id) {
        return icons.computeIfAbsent(id, k -> {
            ItemStack s = CodexPage.stack(k);
            return s.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.BOOK) : s;
        });
    }

    private CodexData.Node nodeAt(double mx, double my) {
        if (tab == null || mx < mapX0() || mx >= mapX1() || my < mapY0() || my >= mapY1()) return null;
        CodexData.Node best = null;
        for (CodexData.Node n : tab.nodes) {
            int S = size(n);
            double x = sx(n), y = sy(n);
            if (!CodexState.visible(CodexData.entries.get(n.entry()))) continue;      // hidden: not there yet
            if (Math.abs(mx - x) <= S / 2.0 && Math.abs(my - y) <= S / 2.0) best = n;     // the last drawn (on top) wins
        }
        return best;
    }

    // ================================================================== search
    private void searchChanged(String s) {
        String q = s.trim().toLowerCase(Locale.ROOT);
        resultScroll = 0;
        if (q.isEmpty()) {
            results = List.of();
            matches = Set.of();
            return;
        }
        List<CodexData.Entry> a = new ArrayList<>(), b = new ArrayList<>(), c = new ArrayList<>();
        for (CodexData.Entry e : CodexData.entries.values()) {
            if (!CodexState.unlocked(e)) continue;              // the search never gives away what is still locked
            if (e.searchTitle.startsWith(q)) a.add(e);
            else if (e.searchTitle.contains(q)) b.add(e);
            else if (e.searchText.contains(q)) c.add(e);
        }
        Comparator<CodexData.Entry> byTitle = Comparator.comparing(e -> e.title);
        a.sort(byTitle);
        b.sort(byTitle);
        c.sort(byTitle);
        List<CodexData.Entry> all = new ArrayList<>(a);
        all.addAll(b);
        all.addAll(c);
        results = all;
        Set<String> m = new HashSet<>();
        for (CodexData.Entry e : all) m.add(e.id);
        matches = m;
    }

    /** The result list's box (x, y, w, h, rows) or null when there is nothing to show. */
    private int[] resultBox() {
        if (entry != null || search == null || search.getValue().isBlank()) return null;
        int rows = Math.max(1, Math.min(Math.max(1, results.size()), (mapY1() - mapY0() - 20) / 12));
        int w = Math.min(width - 8, Math.max(search.getWidth(), 220));
        int h = rows * 12 + 14;
        return new int[]{width - w - 4, mapY1() - h - 2, w, h, rows};
    }

    // ================================================================== entries
    private void openEntry(String id, boolean remember, int page) {
        fitFor = null;                 // (its pages may differ from last time: measure again)
        CodexData.Entry e = CodexData.entries.get(id);
        if (e == null) return;
        if (remember && entry != null) {
            HISTORY.push(entry.id);
            HISTORY_PAGES.push(new int[]{pageNo});
        }
        entry = e;
        entryId = id;
        pages.clear();
        pageRecipes.clear();
        boolean open = CodexState.unlocked(e);
        if (open) {
            for (CodexData.Page p : e.pages) {
                pages.add(p);
                pageRecipes.add(null);
            }
            addRecipePages(e);
            CodexState.markRead(e.id);
        } else {
            CodexData.Page p = new CodexData.Page();
            p.type = "text";
            p.title = I18n.get("gui.theoreticalsciences.codex.locked");
            p.text = I18n.get("gui.theoreticalsciences.codex.locked_page", CodexState.researchName(e).getString());
            pages.add(p);
            pageRecipes.add(null);
        }
        if (pages.isEmpty()) {
            CodexData.Page p = new CodexData.Page();
            p.title = e.title;
            pages.add(p);
            pageRecipes.add(null);
        }
        pageNo = Math.max(0, Math.min(page, pages.size() - 1));
        scroll = 0;
        builtFor = -1;
        // the map follows: show where the entry stands (on this map if it is here, else on the first map that has it)
        CodexData.Tab on = tab != null && tab.byEntry.containsKey(id) ? tab : null;
        if (on == null) for (CodexData.Tab t : CodexData.tabs) if (t.byEntry.containsKey(id)) { on = t; break; }
        boolean moved = on != null && on != tab;
        if (moved) switchTab(on);
        // opened by clicking its node, the map stays where it is; followed from a link or a search, the map goes to it
        if (on != null && (remember || moved)) focus(on.byEntry.get(id));
    }

    /** "How it is made" pages: the recipes of everything the entry shows (its own item first). */
    private void addRecipePages(CodexData.Entry e) {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        if (e.node) items.add(e.icon);
        for (CodexData.Page p : e.pages) if ("spotlight".equals(p.type)) items.addAll(p.items);
        List<Object> found = new ArrayList<>();
        for (String id : items) {
            ItemStack s = CodexPage.stack(id);
            if (s.isEmpty()) continue;
            int k = 0;
            for (Object r : CodexRecipes.producing(s.getItem())) {
                if (found.contains(r)) continue;
                found.add(r);
                // round 32b: every way to make it (JEI may not be installed - the Codex is then the only recipe book)
                if (++k >= 8 || found.size() >= 24) break;
            }
            if (found.size() >= 24) break;
        }
        for (Object r : found) {
            CodexData.Page p = new CodexData.Page();
            p.type = "auto_recipe";
            pages.add(p);
            pageRecipes.add(r);
        }
    }

    private void closeEntry() {
        entry = null;
        entryId = null;
        HISTORY.clear();
        HISTORY_PAGES.clear();
    }

    private void back() {
        if (HISTORY.isEmpty()) {
            closeEntry();
            return;
        }
        String id = HISTORY.pop();
        int[] p = HISTORY_PAGES.isEmpty() ? new int[]{0} : HISTORY_PAGES.pop();
        entry = null;
        openEntry(id, false, p[0]);
        CodexData.Tab on = tab;
        if (on != null && on.byEntry.containsKey(id)) focus(on.byEntry.get(id));
    }

    private void turn(int d) {
        int n = Math.max(0, Math.min(pages.size() - 1, pageNo + d));
        if (n != pageNo) {
            pageNo = n;
            scroll = 0;
            builtFor = -1;
        }
    }

    private int[] content() {
        int[] p = panel();
        return new int[]{p[0] + 12, p[1] + 26, p[2] - 24, p[3] - 26 - 24};
    }

    private CodexPage page() {
        int[] c = content();
        if (built == null || builtFor != pageNo || builtWidth != c[2] - 6) {
            int recipes = 0, no = 0;
            for (int i = 0; i < pages.size(); i++)
                if (pageRecipes.get(i) != null) {
                    recipes++;
                    if (i <= pageNo) no++;
                }
            built = new CodexPage(font, pages.get(pageNo), pageRecipes.get(pageNo), c[2] - 6, accent(), no, recipes);
            builtFor = pageNo;
            builtWidth = c[2] - 6;
        }
        return built;
    }

    private double maxScroll() {
        return Math.max(0, page().height() - content()[3]);
    }

    // ================================================================== drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        int a = accent();
        // (round 32b: fully opaque at both ends - the mix of two colours drops their opacity, which left the bottom
        // of the screen see-through and the game showing behind the Codex)
        g.fillGradient(0, 0, width, height, 0xFF04060E, 0xFF000000 | HoloStyle.mix(0xFF0A0820, 0xFF000000 | a, 0.10F));
        // three layers of stars drifting with the map at different speeds
        float t = time();
        Random r = new Random(1234);
        for (int layer = 0; layer < 3; layer++) {
            double k = 0.08 + layer * 0.12;
            for (int i = 0; i < 90; i++) {
                int x = (int) Math.floorMod((long) (r.nextInt(4096) + panX * k), (long) Math.max(1, width));
                int y = (int) Math.floorMod((long) (r.nextInt(4096) + panY * k), (long) Math.max(1, height));
                int al = (int) (60 + 50 * layer + 40 * Math.sin(t * 0.04 + i * 1.7));
                g.fill(x, y, x + 1 + (layer == 2 && i % 7 == 0 ? 1 : 0), y + 1, (Math.max(0, Math.min(255, al)) << 24) | (i % 5 == 0 ? a : 0xCCDDFF));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        placeEntryButtons();
        renderBackground(g, mx, my, partial);
        float t = time();
        if (tab == null) {
            g.drawCenteredString(font, Component.translatable("gui.theoreticalsciences.codex.missing"), width / 2, height / 2, 0xFFFF8080);
            if (CodexData.error != null) TextFit.draw(g, font, Component.literal(CodexData.error), 8, height / 2 + 14, width - 16, 0xFF808080);
            return;
        }
        if (entry == null) gatherNext();
        boolean overUi = entry != null || my < mapY0() || my >= mapY1() || inBox(resultBox(), mx, my) || inBox(nextBox(), mx, my) || inBox(intro ? introBox() : null, mx, my);
        CodexData.Node hovered = overUi ? null : nodeAt(mx, my);
        drawMap(g, t, hovered);
        drawTop(g, mx, my, t);
        drawBottom(g, mx, my);
        CodexPage.Hover ph = null;
        if (entry != null) ph = drawEntry(g, mx, my, t);
        else {
            drawNext(g, mx, my);
            drawIntro(g);
            drawResults(g, mx, my);
        }
        // the buttons above the entry panel (which is drawn raised, so the map's items stay behind it)
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        for (Btn b : buttons) b.render(g, mx, my);
        if (entry == null) search.render(g, mx, my, partial);
        g.pose().popPose();
        // ---- tooltips, last, over everything
        g.pose().pushPose();
        g.pose().translate(0, 0, 500);
        tooltips(g, mx, my, hovered, ph);
        g.pose().popPose();
    }

    private void tooltips(GuiGraphics g, int mx, int my, CodexData.Node hovered, CodexPage.Hover ph) {
        for (Btn b : buttons)
            if (b.over(mx, my) && b.tip != null) {
                TextFit.tooltip(g, font, List.of(b.tip), mx, my);
                return;
            }
        int ti = entry != null && inBox(panel(), mx, my) ? -1 : tabAt(mx, my);
        if (ti >= 0) {
            CodexData.Tab tt = CodexData.tabs.get(ti);
            TextFit.tooltip(g, font, List.of(Component.literal(tt.name).withStyle(ChatFormatting.AQUA), Component.literal(tt.description).withStyle(ChatFormatting.GRAY)), mx, my);
            return;
        }
        if (ph != null) {
            if (!ph.stack.isEmpty()) g.renderTooltip(font, ph.stack, mx, my);
            else if (ph.lines != null) TextFit.tooltip(g, font, ph.lines, mx, my);
            else if (ph.link != null && CodexData.entries.containsKey(ph.link))
                TextFit.tooltip(g, font, List.of(Component.literal(CodexData.entries.get(ph.link).title).withStyle(ChatFormatting.AQUA),
                        Component.translatable("gui.theoreticalsciences.codex.open_entry").withStyle(ChatFormatting.DARK_PURPLE)), mx, my);
            return;
        }
        int[] rb = resultBox();
        if (inBox(rb, mx, my)) {
            CodexData.Entry e = resultAt(mx, my);
            if (e != null) TextFit.tooltip(g, font, nodeTip(e), mx, my);
            return;
        }
        if (inBox(nextBox(), mx, my)) {
            Object o = nextAt(mx, my);
            if (o instanceof net.minecraft.resources.ResourceLocation rl) {
                var d = com.theoreticalsciences.client.ClientResearch.defs.get(rl);
                List<Component> l = new ArrayList<>();
                if (d != null) {
                    l.add(d.titleComponent(rl).copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    l.add(Component.translatable("gui.theoreticalsciences.codex.next_research_cost", d.insight(), com.theoreticalsciences.client.ClientResearch.insight).withStyle(ChatFormatting.GRAY));
                }
                l.add(Component.translatable("gui.theoreticalsciences.codex.next_open_research").withStyle(ChatFormatting.DARK_PURPLE));
                TextFit.tooltip(g, font, l, mx, my);
            } else if (o instanceof String id) TextFit.tooltip(g, font, nodeTip(CodexData.entries.get(id)), mx, my);
            return;
        }
        if (hovered != null && !pressed) TextFit.tooltip(g, font, nodeTip(CodexData.entries.get(hovered.entry())), mx, my);
    }

    private static boolean inBox(int[] b, double mx, double my) {
        return b != null && mx >= b[0] && my >= b[1] && mx < b[0] + b[2] && my < b[1] + b[3];
    }

    private List<Component> nodeTip(CodexData.Entry e) {
        List<Component> l = new ArrayList<>();
        if (e == null) return l;
        if (!CodexState.unlocked(e) && CodexState.isGuide(e.id)) {          // round 33: a guide step says what it is, and what opens it
            l.add(Component.literal(e.title).withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
            l.add(Component.translatable("gui.theoreticalsciences.codex.guide_locked", CodexState.researchName(e)).withStyle(ChatFormatting.RED));
            return l;
        }
        if (!CodexState.unlocked(e)) {                        // a next step: only that there is one, and what opens it
            l.add(Component.literal("???").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
            l.add(Component.translatable("gui.theoreticalsciences.codex.locked_by", CodexState.researchName(e)).withStyle(ChatFormatting.RED));
            return l;
        }
        l.add(Component.literal(e.title).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        String cat = CodexData.categories.getOrDefault(e.category, e.category);
        if (!cat.isEmpty()) l.add(Component.literal(cat).withStyle(ChatFormatting.DARK_GRAY));
        if (!CodexState.unlocked(e)) l.add(Component.translatable("gui.theoreticalsciences.codex.locked_by", CodexState.researchName(e)).withStyle(ChatFormatting.RED));
        else {
            if (!CodexState.isRead(e.id)) l.add(Component.translatable("gui.theoreticalsciences.codex.new").withStyle(ChatFormatting.GOLD));
            l.add(Component.translatable("gui.theoreticalsciences.codex.click_open").withStyle(ChatFormatting.DARK_PURPLE));
        }
        return l;
    }

    private void line(GuiGraphics g, double x0, double y0, double x1, double y1, float thick, int col) {
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        g.pose().translate(0, -thick / 2F, 0);
        g.pose().scale(1F, thick, 1F);
        g.fill(0, 0, (int) Math.round(len), 1, col);
        g.pose().popPose();
    }

    private void drawMap(GuiGraphics g, float t, CodexData.Node hovered) {
        int a = accent();
        int x0 = mapX0(), y0 = mapY0(), x1 = mapX1(), y1 = mapY1();
        g.enableScissor(x0, y0, x1, y1);
        // a faint grid that moves with the map
        double step = UNIT * 4 * zoom;
        if (step >= 12) {
            int gc = HoloStyle.argb(0x0C, a);
            for (double gx = ((panX % step) + step) % step; gx < width; gx += step) g.fill((int) gx, y0, (int) gx + 1, y1, gc);
            for (double gy = ((panY % step) + step) % step; gy < height; gy += step) g.fill(x0, (int) gy, x1, (int) gy + 1, gc);
        }
        // round 36: the tier bands - each stage of the game kept together, like a quest book's chapters
        for (CodexData.Region r : tab.regions) {
            double rx0 = panX + r.x0() * UNIT * zoom, rx1 = panX + r.x1() * UNIT * zoom;
            double ry0 = panY + r.y0() * UNIT * zoom, ry1 = panY + r.y1() * UNIT * zoom;
            if (rx1 < x0 || rx0 > x1 || ry1 < y0 || ry0 > y1) continue;
            int rc = TIER_COLOURS[Math.floorMod(r.tier(), TIER_COLOURS.length)];
            int bx0 = (int) rx0, by0 = (int) ry0, bw = (int) (rx1 - rx0), bh = (int) (ry1 - ry0);
            int ch = (int) Math.max(3, Math.min(10, 6 * zoom));
            g.drawManaged(() -> {
                HoloStyle.chamferFill(g, bx0, by0, bw, bh, ch, HoloStyle.argb(0x14, rc));
                HoloStyle.chamferFrame(g, bx0, by0, bw, bh, ch, HoloStyle.argb(0x50, rc));
                HoloStyle.chamferCorners(g, bx0, by0, bw, bh, ch, Math.min(14, bw / 6), HoloStyle.argb(0xC0, rc));
            });
            if (bw > font.width(r.label()) + 12) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 50);
                g.drawString(font, r.label(), bx0 + ch + 4, by0 + 4, HoloStyle.argb(0xE0, HoloStyle.bright(rc)), true);
                g.pose().popPose();
            }
        }
        // round 32b: hovering a node lights up everything directly linked to it (what it builds on and what builds
        // on it), and dims the rest, so a crowded map stays readable
        java.util.Set<String> linked = new java.util.HashSet<>();
        String hid = hovered == null ? null : hovered.entry();
        if (hid != null) {
            linked.add(hid);
            linked.addAll(hovered.parents());
            for (CodexData.Node m : tab.nodes) if (m.parents().contains(hid)) linked.add(m.entry());
        }
        // connections: to the chapter it builds on (bright, with a moving spark when open) or to its hub (faint)
        float thick = (float) Math.max(1, Math.min(2.5, zoom * 1.2));
        for (CodexData.Node n : tab.nodes) {
            double nx = sx(n), ny = sy(n);
            for (String pid : n.parents()) {
                CodexData.Node p = tab.byEntry.get(pid);
                if (p == null) continue;
                double px = sx(p), py = sy(p);
                if ((nx < x0 && px < x0) || (nx > x1 && px > x1) || (ny < y0 && py < y0) || (ny > y1 && py > y1)) continue;
                CodexData.Entry e = CodexData.entries.get(n.entry());
                if (!CodexState.visible(e)) continue;               // hidden nodes have no lines either
                boolean open = CodexState.unlocked(e);
                int col = n.small() ? HoloStyle.argb(open ? 0x50 : 0x24, a) : open ? HoloStyle.argb(0xB0, a) : 0x50607080;
                boolean lit = hid != null && (n.entry().equals(hid) || pid.equals(hid));
                if (lit) col = 0xFFFFE070;
                else if (hid != null) col = (Math.max(0x10, ((col >>> 24) & 0xFF) / 3) << 24) | (col & 0xFFFFFF);
                line(g, px, py, nx, ny, lit ? Math.max(2F, thick + 1) : n.small() ? 1 : thick, col);
                if (!n.small() && open) {
                    double f = ((t * 0.02 + (n.x() * 31 + n.y() * 17) * 0.13) % 1 + 1) % 1;
                    int sxp = (int) (px + (nx - px) * f), syp = (int) (py + (ny - py) * f);
                    g.fill(sxp - 1, syp - 1, sxp + 1, syp + 1, 0xFFFFFFFF);
                }
            }
        }
        // nodes
        for (CodexData.Node n : tab.nodes) {
            int S = size(n);
            int cx = (int) Math.round(sx(n)), cy = (int) Math.round(sy(n));
            if (cx + S < x0 || cy + S < y0 || cx - S > x1 || cy - S > y1) continue;
            CodexData.Entry e = CodexData.entries.get(n.entry());
            if (e == null || !CodexState.visible(e)) continue;     // further on than the player has got: not drawn
            boolean open = CodexState.unlocked(e);
            boolean fresh = open && !n.small() && !CodexState.isRead(e.id);      // (chapters only: a spark on every item would drown the map)
            boolean hit = matches.contains(e.id);
            int nx = cx - S / 2, ny = cy - S / 2;
            if (S < 10) {              // far away: just a dot (and a thousand icons would slow the game down)
                g.fill(nx, ny, nx + S, ny + S, hit ? 0xFFFFFF60 : open ? HoloStyle.argb(0xE0, a) : 0xFF404858);
                continue;
            }
            int c = Math.max(1, S / (n.small() ? 6 : 5));
            int bg = open ? (n.small() ? 0xE0081420 : 0xF0102030) : 0xF0080A10;
            boolean near = hid != null && linked.contains(n.entry()) && n != hovered;
            int fr = hit ? 0xFFFFFF60 : n == hovered ? 0xFFFFFFFF : near ? 0xFFFFE070 : open ? HoloStyle.argb(n.small() ? 0x90 : 0xFF, a) : 0xFF404858;
            if (!n.small() && open) HoloStyle.chamferFill(g, nx - 2, ny - 2, S + 4, S + 4, c + 1, HoloStyle.argb(0x30, a));
            boolean guide = CodexState.isGuide(e.id);
            if (guide) {
                // round 33: the Getting Started steps are framed in gold; the one to read next (and "Start Here") pulses
                boolean pulse = open && !CodexState.isRead(e.id);
                int al = pulse ? (int) (0x90 + 0x6F * Math.sin(t * 0.25)) : open ? 0xC0 : 0x60;
                int ring = e.id.equals(CodexState.START) ? 4 : 2;
                HoloStyle.chamferFrame(g, nx - ring, ny - ring, S + ring * 2, S + ring * 2, c + 2, (Math.max(0, Math.min(255, al)) << 24) | 0xFFC840);
                if (e.id.equals(CodexState.START) && S >= 10) {
                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.drawString(font, Component.translatable("gui.theoreticalsciences.codex.start_here").withStyle(ChatFormatting.BOLD), nx + S + 8, cy - 4, 0xFFFFD060, true);
                    g.pose().popPose();
                }
            }
            HoloStyle.chamferFill(g, nx, ny, S, S, c, bg);
            HoloStyle.chamferFrame(g, nx, ny, S, S, c, fr);
            if (hit) HoloStyle.chamferFrame(g, nx - 2, ny - 2, S + 4, S + 4, c + 1, HoloStyle.argb((int) (0x80 + 0x7F * Math.sin(t * 0.3)), 0xFFFF60));
            float k = (S - 4) / 16F;
            g.pose().pushPose();
            g.pose().translate(nx + 2, ny + 2, 0);
            g.pose().scale(k, k, 1F);
            if (open || guide) g.renderItem(icon(e.icon), 0, 0);       // a locked next step does not show what it is (a guide step does)
            g.pose().popPose();
            if (!open) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(nx + 1, ny + 1, nx + S - 1, ny + S - 1, 0xA0000000);
                if (S >= 14) padlock(g, cx, cy);
                g.pose().popPose();
            }
            if (hid != null && !linked.contains(n.entry())) {           // not linked to the hovered node: dimmed
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(nx, ny, nx + S, ny + S, 0x90000000);
                g.pose().popPose();
            }
            if (fresh) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                int al = (int) (0xA0 + 0x5F * Math.sin(t * 0.25 + n.x()));
                int d = Math.max(2, S / 8);
                g.fill(nx + S - d - 1, ny + 1, nx + S - 1, ny + d + 1, (al << 24) | 0xFFD040);
                g.pose().popPose();
            }
            // names under the chapters when zoomed in far enough
            if ((!n.small() && zoom >= 1.3) || zoom >= 2.2) {
                int lw = (int) (UNIT * 2 * zoom) - 2;
                Component nm = Component.literal(open || guide ? e.title : "???");
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                int tw = Math.min(lw, font.width(nm));
                TextFit.draw(g, font, nm, cx - tw / 2, ny + S + 2, lw, open ? 0xFFD8E4F0 : 0xFF707880);
                g.pose().popPose();
            }
        }
        g.disableScissor();
    }

    private void padlock(GuiGraphics g, int cx, int cy) {
        int c = 0xFFB0B8C0;
        g.fill(cx - 3, cy - 1, cx + 4, cy + 4, c);
        g.fill(cx - 2, cy - 4, cx - 1, cy - 1, c);
        g.fill(cx + 2, cy - 4, cx + 3, cy - 1, c);
        g.fill(cx - 2, cy - 5, cx + 3, cy - 4, c);
        g.fill(cx, cy + 1, cx + 1, cy + 3, 0xFF202020);
    }

    private int tabAt(double mx, double my) {
        for (int i = 0; i < tabRects.size(); i++) {
            int[] r = tabRects.get(i);
            if (mx >= r[0] && my >= r[1] && mx < r[0] + r[2] && my < r[1] + r[3]) return i;
        }
        return -1;
    }

    private void drawTop(GuiGraphics g, int mx, int my, float t) {
        int a = accent();
        g.fill(0, 0, width, TOP, 0xE0040810);
        g.fill(0, TOP - 1, width, TOP, HoloStyle.argb(0x90, a));
        for (int i = 0; i < CodexData.tabs.size() && i < tabRects.size(); i++) {
            CodexData.Tab tt = CodexData.tabs.get(i);
            int[] r = tabRects.get(i);
            boolean sel = tt == tab, hov = tabAt(mx, my) == i;
            HoloStyle.chamferFill(g, r[0], r[1], r[2], r[3], 3, sel ? HoloStyle.argb(0xB0, a) : hov ? 0xC0182838 : 0xC00A1018);
            HoloStyle.chamferFrame(g, r[0], r[1], r[2], r[3], 3, sel ? 0xFFFFFFFF : HoloStyle.argb(0x80, a));
            int ix = r[0] + (tabIcons ? (r[2] - 16) / 2 : 3);
            if (r[2] >= 16) g.renderItem(icon(tt.icon), ix, r[1] + (r[3] - 16) / 2);
            if (!tabIcons) g.drawString(font, tt.name, r[0] + 21, r[1] + (r[3] - 8) / 2, sel ? 0xFFFFFFFF : 0xFFB8C8D8, false);
        }
    }

    private void drawBottom(GuiGraphics g, int mx, int my) {
        int a = accent();
        g.fill(0, height - BOT, width, height, 0xE0040810);
        g.fill(0, height - BOT, width, height - BOT + 1, HoloStyle.argb(0x90, a));
        if (entry != null || tab == null) return;
        // the tab's name and how much of it has been read, between the buttons and the search box
        int left = 4;
        for (Btn b : buttons) if (b.shown.getAsBoolean() && b.y >= height - BOT) left = Math.max(left, b.x + b.w + 6);
        int right = search.getX() - 6;
        if (right - left > 40) {
            int read = 0, open = 0;
            for (CodexData.Node n : tab.nodes) {
                CodexData.Entry e = CodexData.entries.get(n.entry());
                if (e == null || !CodexState.unlocked(e)) continue;
                open++;
                if (CodexState.isRead(e.id)) read++;
            }
            Component c = Component.literal(tab.name + "  ").withStyle(ChatFormatting.AQUA)
                    .append(Component.translatable("gui.theoreticalsciences.codex.progress", read, open).withStyle(ChatFormatting.GRAY));
            TextFit.draw(g, font, c, left, height - BOT + 8, right - left, 0xFFD8E4F0);
        }
    }

    // ================================================================== round 33: hint banner and Next steps
    private List<net.minecraft.util.FormattedCharSequence> introLines(int w) {
        List<net.minecraft.util.FormattedCharSequence> out = new ArrayList<>();
        out.addAll(font.split(Component.translatable("gui.theoreticalsciences.codex.intro_1"), w));
        out.addAll(font.split(Component.translatable("gui.theoreticalsciences.codex.intro_2"), w));
        out.addAll(font.split(Component.translatable("gui.theoreticalsciences.codex.intro_3",
                Component.keybind("key.theoreticalsciences.codex_lookup").withStyle(ChatFormatting.YELLOW)), w));
        return out;
    }

    /** The hint banner: centred low over the map. */
    private int[] introBox() {
        int w = Math.min(width - 16, 340);
        int lines = introLines(w - 12).size();
        int h = 16 + lines * 10 + 22;
        return new int[]{(width - w) / 2, Math.max(mapY0() + 4, mapY1() - h - 6), w, h};
    }

    private void drawIntro(GuiGraphics g) {
        if (!intro) return;
        int[] b = introBox();
        int a = accent();
        g.pose().pushPose();
        g.pose().translate(0, 0, 320);
        HoloStyle.chamferFill(g, b[0], b[1], b[2], b[3], 5, 0xF0060A14);
        HoloStyle.chamferFrame(g, b[0], b[1], b[2], b[3], 5, 0xFFFFC840);
        g.drawString(font, Component.translatable("gui.theoreticalsciences.codex.intro_title").withStyle(ChatFormatting.BOLD), b[0] + 6, b[1] + 5, 0xFFFFD060, false);
        int y = b[1] + 17;
        for (var l : introLines(b[2] - 12)) {
            g.drawString(font, l, b[0] + 6, y, 0xFFD8E4F0, false);
            y += 10;
        }
        g.pose().popPose();
    }

    private void dismissIntro() {
        intro = false;
        CodexState.markRead(CodexState.INTRO);
        CodexState.save();
    }

    /** The Next steps panel, on the right of the map, or null when hidden. */
    private int[] nextBox() {
        if (!showNext || entry != null || tab == null) return null;
        int w = Math.min(Math.max(160, width / 3), 230);
        int rows = Math.max(1, Math.min(nextRows.size(), (mapY1() - mapY0() - 60) / 12));
        int h = 8 + rows * 12 + 4;
        return new int[]{width - w - 4, mapY0() + 4, w, h, rows};
    }

    /** Research the player can start now (cheapest first), then the Getting Started steps open but not read yet. */
    private void gatherNext() {
        nextRows.clear();
        if (!showNext) return;
        List<net.minecraft.resources.ResourceLocation> rs = new ArrayList<>();
        for (var en : com.theoreticalsciences.client.ClientResearch.defs.entrySet())
            if (com.theoreticalsciences.client.ClientResearch.available(en.getKey()) && com.theoreticalsciences.client.ClientResearch.visible(en.getKey())) rs.add(en.getKey());
        rs.sort(Comparator.comparingInt((net.minecraft.resources.ResourceLocation r) -> com.theoreticalsciences.client.ClientResearch.defs.get(r).insight()).thenComparing(Object::toString));
        List<String> gs = new ArrayList<>();
        for (CodexData.Entry e : CodexData.entries.values())
            if (CodexState.isGuide(e.id) && CodexState.unlocked(e) && !CodexState.isRead(e.id)) gs.add(e.id);
        Collections.sort(gs);
        if (!gs.isEmpty()) {
            nextRows.add("#head:gui.theoreticalsciences.codex.next_guide");
            nextRows.addAll(gs.subList(0, Math.min(4, gs.size())));
        }
        if (!rs.isEmpty()) {
            nextRows.add("#head:gui.theoreticalsciences.codex.next_research");
            nextRows.addAll(rs.subList(0, Math.min(8, rs.size())));
        }
        if (nextRows.isEmpty()) nextRows.add("#none");
    }

    private Object nextAt(double mx, double my) {
        int[] b = nextBox();
        if (!inBox(b, mx, my)) return null;
        int row = (int) ((my - b[1] - 6) / 12);
        return row >= 0 && row < b[4] && row < nextRows.size() ? nextRows.get(row) : null;
    }

    private void drawNext(GuiGraphics g, int mx, int my) {
        int[] b = nextBox();
        if (b == null) return;
        int a = accent();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        HoloStyle.chamferFill(g, b[0], b[1], b[2], b[3], 4, 0xF0060A12);
        HoloStyle.chamferFrame(g, b[0], b[1], b[2], b[3], 4, HoloStyle.argb(0xC0, a));
        Object hov = nextAt(mx, my);
        for (int r = 0; r < b[4] && r < nextRows.size(); r++) {
            Object o = nextRows.get(r);
            int ry = b[1] + 6 + r * 12;
            if (o instanceof String s && s.startsWith("#")) {
                Component c = s.equals("#none") ? Component.translatable("gui.theoreticalsciences.codex.next_none")
                        : Component.translatable(s.substring(6)).withStyle(ChatFormatting.BOLD);
                TextFit.draw(g, font, c, b[0] + 6, ry + 2, b[2] - 12, s.equals("#none") ? 0xFF90A0B0 : 0xFFFFD060);
                continue;
            }
            if (o == hov) g.fill(b[0] + 2, ry, b[0] + b[2] - 2, ry + 12, HoloStyle.argb(0x50, a));
            ItemStack ic;
            Component label;
            if (o instanceof net.minecraft.resources.ResourceLocation rl) {
                var d = com.theoreticalsciences.client.ClientResearch.defs.get(rl);
                ic = d == null ? ItemStack.EMPTY : icon(d.icon().toString());
                label = d == null ? Component.literal(rl.toString()) : d.titleComponent(rl).copy()
                        .append(Component.literal("  " + d.insight()).withStyle(d.insight() <= com.theoreticalsciences.client.ClientResearch.insight ? ChatFormatting.GREEN : ChatFormatting.RED));
            } else {
                CodexData.Entry e = CodexData.entries.get((String) o);
                ic = e == null ? ItemStack.EMPTY : icon(e.icon);
                label = Component.literal(e == null ? String.valueOf(o) : e.title);
            }
            g.pose().pushPose();
            g.pose().translate(b[0] + 4, ry + 1, 0);
            g.pose().scale(0.625F, 0.625F, 1F);
            if (!ic.isEmpty()) g.renderItem(ic, 0, 0);
            g.pose().popPose();
            TextFit.draw(g, font, label, b[0] + 16, ry + 2, b[2] - 22, 0xFFD8E4F0);
        }
        g.pose().popPose();
    }

    private CodexData.Entry resultAt(double mx, double my) {
        int[] b = resultBox();
        if (!inBox(b, mx, my) || results.isEmpty()) return null;
        int row = (int) ((my - b[1] - 12) / 12);
        int i = resultScroll + row;
        return row >= 0 && row < b[4] && i < results.size() ? results.get(i) : null;
    }

    private void drawResults(GuiGraphics g, int mx, int my) {
        int[] b = resultBox();
        if (b == null) return;
        int a = accent();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        HoloStyle.chamferFill(g, b[0], b[1], b[2], b[3], 4, 0xF0060A12);
        HoloStyle.chamferFrame(g, b[0], b[1], b[2], b[3], 4, HoloStyle.argb(0xC0, a));
        Component head = results.isEmpty() ? Component.translatable("gui.theoreticalsciences.codex.no_results")
                : Component.translatable("gui.theoreticalsciences.codex.results", results.size());
        g.drawString(font, head, b[0] + 6, b[1] + 3, 0xFF90A0B0, false);
        for (int r = 0; r < b[4]; r++) {
            int i = resultScroll + r;
            if (i >= results.size()) break;
            CodexData.Entry e = results.get(i);
            int ry = b[1] + 12 + r * 12;
            boolean hov = mx >= b[0] && mx < b[0] + b[2] && my >= ry && my < ry + 12;
            if (hov) g.fill(b[0] + 2, ry, b[0] + b[2] - 2, ry + 12, HoloStyle.argb(0x50, a));
            g.pose().pushPose();
            g.pose().translate(b[0] + 4, ry + 1, 0);
            g.pose().scale(0.625F, 0.625F, 1F);
            g.renderItem(icon(e.icon), 0, 0);
            g.pose().popPose();
            boolean open = CodexState.unlocked(e);
            String cat = CodexData.categories.getOrDefault(e.category, e.category);
            int cw = Math.min(b[2] / 3, font.width(cat));
            TextFit.draw(g, font, Component.literal(e.title), b[0] + 16, ry + 2, b[2] - 24 - cw, open ? 0xFFD8E4F0 : 0xFF707880);
            TextFit.draw(g, font, Component.literal(cat), b[0] + b[2] - 6 - cw, ry + 2, cw, 0xFF607080);
        }
        if (results.size() > b[4]) {
            int trackH = b[4] * 12, th = Math.max(6, trackH * b[4] / results.size());
            int ty = b[1] + 12 + (trackH - th) * resultScroll / Math.max(1, results.size() - b[4]);
            g.fill(b[0] + b[2] - 3, ty, b[0] + b[2] - 1, ty + th, HoloStyle.argb(0xC0, a));
        }
        g.pose().popPose();
    }

    private CodexPage.Hover drawEntry(GuiGraphics g, int mx, int my, float t) {
        int a = accent();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0x90000000);
        int[] p = panel();
        HoloStyle.panel(g, p[0], p[1], p[2], p[3], 6, a, HoloStyle.LATE, t, p[1] + 24, p[1] + p[3] - 22, 5);
        HoloStyle.frame(g, p[0], p[1], p[2], p[3], 6, a, HoloStyle.LATE, t);
        HoloStyle.header(g, p[0], p[1], p[2], 22, 6, a, pages.size() > 1 ? (pageNo + 1) / (float) pages.size() : -1, false, t);
        // the title row
        int tx = p[0] + (HISTORY.isEmpty() ? 10 : 24);
        boolean known = CodexState.unlocked(entry);
        if (known) g.renderItem(icon(entry.icon), tx, p[1] + 4);
        String cat = CodexData.categories.getOrDefault(entry.category, entry.category);
        int cw = p[2] < 260 ? 0 : Math.min(p[2] / 4, font.width(cat));
        int tw = p[0] + p[2] - 26 - cw - (tx + 20);
        TextFit.draw(g, font, Component.literal(known || CodexState.isGuide(entry.id) ? entry.title : "???").withStyle(ChatFormatting.BOLD), tx + 20, p[1] + 8, tw, 0xFFFFFFFF);
        if (cw > 0) TextFit.draw(g, font, Component.literal(cat), p[0] + p[2] - 24 - cw, p[1] + 8, cw, 0xFF8090A0);
        // the page
        int[] c = content();
        CodexPage pg = page();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        g.enableScissor(c[0], c[1], c[0] + c[2], c[1] + c[3]);
        boolean inside = mx >= c[0] && my >= c[1] && mx < c[0] + c[2] && my < c[1] + c[3];
        CodexPage.Hover h = pg.render(g, font, c[0], c[1] - (int) scroll, inside ? mx : -10000, inside ? my : -10000);
        g.disableScissor();
        if (maxScroll() > 0) {               // a scroll bar beside the page
            int trackX = c[0] + c[2] - 2, th = Math.max(10, c[3] * c[3] / Math.max(1, pg.height()));
            int ty = c[1] + (int) ((c[3] - th) * scroll / maxScroll());
            g.fill(trackX, c[1], trackX + 2, c[1] + c[3], 0x30FFFFFF);
            g.fill(trackX, ty, trackX + 2, ty + th, HoloStyle.argb(0xE0, a));
        }
        // the footer
        Component pn = Component.translatable("gui.theoreticalsciences.codex.page", pageNo + 1, pages.size());
        g.drawCenteredString(font, pn, p[0] + p[2] / 2, p[1] + p[3] - 15, 0xFFB0C0D0);
        g.pose().popPose();
        return inside ? h : null;
    }

    // ================================================================== input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tab == null) return super.mouseClicked(mx, my, button);
        for (Btn b : buttons)
            if (b.over(mx, my)) {
                b.action.run();
                return true;
            }
        if (entry == null && search.isMouseOver(mx, my)) {
            if (button == 1) search.setValue("");
            setFocused(search);
            search.setFocused(true);
            return super.mouseClicked(mx, my, button);
        }
        if (search.isFocused()) {
            search.setFocused(false);
            setFocused(null);
        }
        int ti = entry != null && inBox(panel(), mx, my) ? -1 : tabAt(mx, my);
        if (ti >= 0) {
            closeEntry();
            switchTab(CodexData.tabs.get(ti));
            return true;
        }
        if (entry != null) {
            int[] p = panel();
            if (!inBox(p, mx, my)) {
                closeEntry();
                return true;
            }
            int[] c = content();
            if (button == 0 && mx >= c[0] && my >= c[1] && mx < c[0] + c[2] && my < c[1] + c[3]) {
                String link = page().click(mx - c[0], my - c[1] + scroll);
                if (link != null && !link.isEmpty() && !link.equals(entry.id)) openEntry(link, true, 0);
            }
            return true;
        }
        CodexData.Entry r = resultAt(mx, my);
        if (r != null) {
            openEntry(r.id, true, 0);
            return true;
        }
        if (inBox(resultBox(), mx, my)) return true;
        if (inBox(nextBox(), mx, my)) {                       // round 33: Next steps
            Object o = nextAt(mx, my);
            if (o instanceof net.minecraft.resources.ResourceLocation) minecraft.setScreen(new com.theoreticalsciences.client.screen.ResearchScreen());
            else if (o instanceof String id && !id.startsWith("#")) openEntry(id, true, 0);
            return true;
        }
        if (intro && inBox(introBox(), mx, my)) return true;
        if (my >= mapY0() && my < mapY1()) {
            pressed = true;
            dragging = false;
            pressX = mx;
            pressY = my;
            CodexData.Node n = nodeAt(mx, my);
            pressedNode = n == null ? null : n.entry();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (pressed && entry == null) {
            if (!dragging && Math.abs(mx - pressX) + Math.abs(my - pressY) > 3) dragging = true;
            if (dragging) {
                panX += dx;
                panY += dy;
                clampPan();
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (pressed) {
            pressed = false;
            if (dragging) saveView();
            else if (pressedNode != null && entry == null) {
                CodexData.Node n = nodeAt(mx, my);
                if (n != null && n.entry().equals(pressedNode)) openEntry(pressedNode, false, 0);
            }
            dragging = false;
            pressedNode = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (tab == null) return false;
        if (entry != null) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * 20));
            return true;
        }
        int[] b = resultBox();
        if (inBox(b, mx, my)) {
            resultScroll = Math.max(0, Math.min(Math.max(0, results.size() - b[4]), resultScroll - (int) Math.signum(scrollY) * Math.max(1, (int) Math.abs(scrollY))));
            return true;
        }
        if (inBox(nextBox(), mx, my) || (intro && inBox(introBox(), mx, my))) return true;
        double step = 36;
        if (hasShiftDown()) panX += (scrollY != 0 ? scrollY : scrollX) * step;
        else if (hasControlDown()) {
            panY += scrollY * step;
            panX += scrollX * step;
        } else {
            if (scrollX != 0) panX += scrollX * step;         // a sideways swipe on a trackpad moves the map
            if (scrollY != 0) {
                zoomAt(Math.pow(1.15, Math.max(-3, Math.min(3, scrollY))), mx, my);
                return true;
            }
        }
        clampPan();
        saveView();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (tab == null) return super.keyPressed(key, scan, mods);
        if (search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                if (!search.getValue().isEmpty()) search.setValue("");
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (!results.isEmpty()) openEntry(results.get(0).id, true, 0);
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            return search.keyPressed(key, scan, mods) || true;
        }
        if (entry != null) {
            switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> closeEntry();
                case GLFW.GLFW_KEY_BACKSPACE -> back();
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> turn(-1);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> turn(1);
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> scroll = Math.max(0, scroll - 20);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> scroll = Math.min(maxScroll(), scroll + 20);
                case GLFW.GLFW_KEY_PAGE_UP -> scroll = Math.max(0, scroll - content()[3] + 20);
                case GLFW.GLFW_KEY_PAGE_DOWN -> scroll = Math.min(maxScroll(), scroll + content()[3] - 20);
                default -> {
                    return super.keyPressed(key, scan, mods);
                }
            }
            return true;
        }
        double step = 40;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> panX += step;
            case GLFW.GLFW_KEY_RIGHT -> panX -= step;
            case GLFW.GLFW_KEY_UP -> panY += step;
            case GLFW.GLFW_KEY_DOWN -> panY -= step;
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> zoomAt(1.2, mapCX(), mapCY());
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> zoomAt(1 / 1.2, mapCX(), mapCY());
            case GLFW.GLFW_KEY_F -> {
                if (hasControlDown()) {
                    setFocused(search);
                    search.setFocused(true);
                } else fit();
            }
            case GLFW.GLFW_KEY_SLASH -> {
                setFocused(search);
                search.setFocused(true);
            }
            case GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_HOME -> centre();
            case GLFW.GLFW_KEY_N -> showNext = !showNext;
            case GLFW.GLFW_KEY_TAB -> {
                int i = CodexData.tabs.indexOf(tab);
                int n = CodexData.tabs.size();
                switchTab(CodexData.tabs.get(((i + (hasShiftDown() ? -1 : 1)) % n + n) % n));
            }
            default -> {
                if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9 && key - GLFW.GLFW_KEY_1 < CodexData.tabs.size()) {
                    switchTab(CodexData.tabs.get(key - GLFW.GLFW_KEY_1));
                    return true;
                }
                return super.keyPressed(key, scan, mods);
            }
        }
        clampPan();
        saveView();
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (search != null && search.isFocused()) return search.charTyped(c, mods);
        return super.charTyped(c, mods);
    }

    @Override
    public void removed() {
        saveView();
        CodexState.save();
        super.removed();
    }

    /** Items and blocks of an ItemStack id, for other code that wants to open the Codex at a thing. */
    public static void openAt(Item item) {
        CodexScreen s = new CodexScreen();
        Minecraft.getInstance().setScreen(s);
        String e = CodexData.entryForItem(BuiltInRegistries.ITEM.getKey(item).toString());
        if (e != null) s.openEntry(e, false, 0);
    }
}
