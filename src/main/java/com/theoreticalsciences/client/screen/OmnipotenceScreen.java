package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.screen.forge.CoreCode;
import com.theoreticalsciences.client.screen.forge.ForgeUniverse;
import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.transcend.CoreForge;
import com.theoreticalsciences.transcend.CorePayloads;
import com.theoreticalsciences.transcend.ForgeLaws;
import com.theoreticalsciences.transcend.OmnipotencePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Omnipotence Core's terminal (round 19): a vast console, not a shop.
 * <ul>
 *   <li><b>MATERIALIZE</b>: query the registry of everything that can exist and have it written into being (click; shift
 *   for a stack). Beyond Omnipotence is not in the registry.</li>
 *   <li><b>FORGE</b> (only in a rebuilt Core): to forge what lies beyond, the Core must simulate a universe in which it can
 *   exist. Set the six constants of nature until all six observables of the simulation are stable; then COMPILE.
 *   Each recipe needs its own universe, and every constant moves several observables at once.</li>
 *   <li><b>SHUTDOWN</b> (an intact Core): destroys the Transcendental in 60 seconds, after a warning.</li>
 * </ul>
 * The server checks everything again (that you stand at the Core, its state, the laws, your materials).
 *
 * <p>Round 29: the terminal fills the whole window, like a computer monitor. The Forge gets a search box, a live view
 * of the simulated universe (chaotic while the laws are wrong, calm once they are right) and a "code display" showing
 * the program the Core is running, which reacts to everything the player does. Everything new is only for show on this
 * screen: what is sent to the server is exactly as before.
 */
public class OmnipotenceScreen extends Screen {
    private static final int INTACT = 0, SHUTDOWN = 1, REBORN = 3;
    private static final int GREEN = 0xFF5CFF8A, DIM = 0xFF2F8A4C, FAINT = 0xFF1A4A2A, AMBER = 0xFFFFC24A, RED = 0xFFFF5A4A, BG = 0xF0020A05;
    private static final int ROW = 18;          // height of one recipe in the catalogue (an item icon and a little air)
    private final BlockPos core;
    private final int phase;
    private final long shutdownEnds, opened = System.currentTimeMillis();
    private final List<ItemStack> all = new ArrayList<>();
    private List<ItemStack> shown;
    private EditBox query, forgeQuery;
    private int scroll, forgeScroll, ingScroll;
    private int tab;                              // 0 materialize, 1 forge, 2 shutdown warning
    /** Law settings, per recipe, kept while the game runs. */
    private static final Map<String, int[]> LAWS = new HashMap<>();
    // round 28: what the bars show glides to the real settings (a shift-click of ten moves the marker, the number and
    // the simulated universe smoothly, not in a jump)
    private final Smooth smooth = new Smooth();
    private double[] shownLaws;
    private String shownFor;
    private int dragLaw = -1;
    private final List<String> log = new ArrayList<>();
    // round 29: the universe on screen and the program listing
    private final ForgeUniverse universe = new ForgeUniverse();
    private final CoreCode code = new CoreCode();
    private double[] lastRes = new double[ForgeLaws.N];
    private String shownEpochFor;
    private int shownEpoch;

    private double[] shownLaws(int[] k, String id) {
        if (shownLaws == null || !id.equals(shownFor)) {
            shownLaws = new double[k.length];
            for (int i = 0; i < k.length; i++) shownLaws[i] = k[i];
            shownFor = id;
        }
        for (int i = 0; i < k.length; i++) shownLaws[i] = smooth.toward(shownLaws[i], k[i]);
        return shownLaws;
    }

    public OmnipotenceScreen(BlockPos core, int phase, int seconds) {
        super(Component.translatable("gui.theoreticalsciences.omnipotence"));
        this.core = core;
        this.phase = phase;
        this.shutdownEnds = System.currentTimeMillis() + seconds * 1000L;
        for (Item i : BuiltInRegistries.ITEM) {
            if (i == Items.AIR) continue;
            ItemStack s = new ItemStack(i);
            // round 28: everything of this mod is listed (and denied when asked for); other mods' as before
            if (OmnipotencePayload.denied(i) || !s.is(OmnipotencePayload.BEYOND) && !CoreForge.forgeOnly(i)) all.add(s);
        }
        shown = all;
        tab = phase == REBORN ? 1 : 0;
        say("OMNIPOTENCE CORE :: TERMINAL v∞");
        say(phase == REBORN ? "core state: REKINDLED - forge online" : phase == SHUTDOWN ? "core state: SHUTTING DOWN" : "core state: INTACT");
        // the program starts the way a computer boots
        code.emit("import core.omnipotence, forge, registry  // v∞");
        code.emit("let core = Core.attach(" + core.getX() + ", " + core.getY() + ", " + core.getZ() + ")");
        code.emit(phase == REBORN ? "core.state -> REKINDLED  // forge online" : phase == SHUTDOWN ? "core.state -> SHUTDOWN  // ERROR: collapse in progress" : "core.state -> INTACT");
        code.emit("exec tab." + tabName(tab) + "()");
    }

    private void say(String s) {
        log.add(s);
        while (log.size() > 60) log.remove(0);
    }

    // ------------------------------------------------------------------ layout
    // Everything is placed from the size of the window, so the terminal fills it at any GUI scale. The numbers below are
    // worked out once (when the screen opens or the window is resized), and the drawing and the mouse use the same ones.
    private int L, T, W, H;                       // the monitor
    private int inL, inW;                         // its usable inside
    private int CT, BY, BH, contentBottom;        // top of the content, top and height of the bottom strip
    // forge
    private int listX, listW, listRowsTop, listRows;
    private int centreX, centreW, rightX, rightW;
    private int ingY, ingCols, ingCellW, ingRowsShown;
    private int lawsX, lawsY, lawsW, barX, barW;
    private int viewY, viewH, obsY, obsH;
    private int compileX, compileY;
    // materialize
    private int gridCols, gridRows, gridX, gridY;
    // bottom
    private int logW, codeX, codeW;
    // shutdown warning (placed while drawing, as it depends on how the warning wraps)
    private int warnX, warnYes = -100, warnNo = -100;

    private static final int OBS_ROW = 9;
    private int lawRow = 11;                      // rows of the constants: a little tighter on a small window

    private void layout() {
        int m = width >= 640 && height >= 360 ? 8 : 4;
        L = m;
        T = m;
        W = width - 2 * m;
        H = height - 2 * m;
        inL = L + 4;
        inW = W - 8;
        CT = T + 33;
        BH = Mth.clamp((int) (H * 0.22), 46, 150);
        BY = T + H - BH;
        contentBottom = BY - 6;
        // forge: catalogue | recipe | universe
        int gap = 4;
        listW = Mth.clamp((int) (W * 0.2), 96, 180);
        centreW = Mth.clamp((int) (inW * 0.34), 156, 270);
        int rest = inW - listW - centreW - 2 * gap;
        if (rest < 100) centreW = Math.max(120, centreW - (100 - rest));
        listX = inL;
        centreX = listX + listW + gap;
        rightX = centreX + centreW + gap;
        rightW = inL + inW - rightX;
        listRowsTop = CT + 22;
        listRows = Math.max(1, (contentBottom - 12 - listRowsTop) / ROW);
        // recipe column: the result at the top, the constants of nature at the bottom, the materials in between
        lawsX = centreX + 5;
        lawsW = centreW - 10;
        lawRow = H < 280 ? 10 : 12;
        lawsY = contentBottom - 4 - (10 + ForgeLaws.N * lawRow);
        barX = lawsX + 34;
        barW = Math.max(20, lawsW - 34 - 22 - 26);
        ingY = CT + 24;
        ingCols = Math.max(1, (centreW - 10) / 48);
        ingCellW = (centreW - 10) / ingCols;
        ingRowsShown = Math.max(1, (lawsY - 4 - ingY) / 17);
        // right column: the universe on top, the observables below it
        obsH = 7 + ForgeLaws.N * OBS_ROW + 24;
        obsY = contentBottom - obsH;
        viewY = CT;
        viewH = Math.max(20, obsY - 6 - CT);
        compileX = rightX + 5;
        compileY = obsY + 7 + ForgeLaws.N * OBS_ROW + 12;
        // materialize: as many slots as fit
        gridY = CT + 22;
        gridCols = Math.max(1, Math.min(48, (inW - 12) / 18));
        gridRows = Math.max(1, (contentBottom - 12 - gridY) / 18);
        gridX = inL + (inW - gridCols * 18) / 2;
        // bottom: the terminal log and the code display side by side
        logW = (int) ((inW - 4) * 0.42);
        codeX = inL + logW + 4;
        codeW = inL + inW - codeX;
    }

    @Override
    protected void init() {
        layout();
        String q0 = query != null ? query.getValue() : "", f0 = forgeQuery != null ? forgeQuery.getValue() : "";
        int lx = inL + 5 + font.width("registry> query:") + 6;
        query = new EditBox(font, lx, CT + 7, Math.max(20, inL + inW - 6 - lx), 10, Component.translatable("gui.theoreticalsciences.omnipotence_search"));
        query.setBordered(false);
        query.setTextColor(GREEN);
        query.setHint(Component.translatable("gui.theoreticalsciences.omnipotence_search").withStyle(ChatFormatting.DARK_GREEN));
        query.setValue(q0);
        query.setResponder(s -> filter());
        addRenderableWidget(query);
        // round 29: the Forge's own search box, at the top of its catalogue
        Component hint = Component.translatableWithFallback("gui.theoreticalsciences.forge_search", "Search recipes...");
        forgeQuery = new EditBox(font, listX + 13, CT + 7, listW - 18, 10, hint);
        forgeQuery.setBordered(false);
        forgeQuery.setTextColor(GREEN);
        forgeQuery.setMaxLength(64);
        forgeQuery.setHint(hint.copy().withStyle(ChatFormatting.DARK_GREEN));
        forgeQuery.setValue(f0);
        forgeQuery.setResponder(s -> forgeSearched());
        addRenderableWidget(forgeQuery);
        refresh();
        if (tab == 0) setInitialFocus(query);
        else if (tab == 1 && phase == REBORN) setInitialFocus(forgeQuery);
    }

    private void refresh() {
        boolean q = tab == 0 && phase != SHUTDOWN;
        query.visible = q;
        query.setEditable(q);
        if (!q) query.setFocused(false);
        boolean f = tab == 1 && phase == REBORN;
        forgeQuery.visible = f;
        forgeQuery.setEditable(f);
        if (!f) forgeQuery.setFocused(false);
    }

    private void filter() {
        String q = query.getValue().toLowerCase(Locale.ROOT).trim();
        scroll = 0;
        if (q.isEmpty()) {
            shown = all;
        } else {
            List<ItemStack> out = new ArrayList<>();
            for (ItemStack s : all) {
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                if (id.contains(q) || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) out.add(s);
            }
            shown = out;
        }
        code.emit("query", "registry.query(\"" + q + "\") -> " + String.format("%,d", shown.size()) + " entries");
    }

    private int secondsLeft() {
        return (int) Math.max(0, (shutdownEnds - System.currentTimeMillis() + 999) / 1000);
    }

    // ------------------------------------------------------------------ the Forge's recipes, the search and the selection
    // The selection is remembered by the recipe's id, not by its place in the list, so filtering never changes which
    // recipe is open. The search looks through a text made once per recipe (its id, the name of what it makes and the
    // names of everything it needs), and the filtered list is only worked out again when the search text changes.
    private String selectedId;
    private CoreForge.Recipe selCache;
    private List<CoreForge.Recipe> selCacheList;
    private List<CoreForge.Recipe> indexFor, forgeShown;
    private String[] index;
    private String forgeShownQ;
    private final Map<String, Boolean> makeable = new HashMap<>();
    private long makeableAt;

    private CoreForge.Recipe recipe() {
        List<CoreForge.Recipe> rs = CoreForge.all();
        if (selCache != null && selCacheList == rs && selCache.id().equals(selectedId)) return selCache;
        CoreForge.Recipe found = null;
        if (selectedId != null) for (CoreForge.Recipe r : rs) if (r.id().equals(selectedId)) { found = r; break; }
        if (found == null) found = rs.get(0);
        selectedId = found.id();
        selCache = found;
        selCacheList = rs;
        return found;
    }

    private int[] laws() {
        return LAWS.computeIfAbsent(recipe().id(), k -> new int[ForgeLaws.N]);
    }

    private List<CoreForge.Recipe> forgeShown() {
        List<CoreForge.Recipe> rs = CoreForge.all();
        if (rs != indexFor) {                       // the recipes were (re)loaded: build the search text again
            index = new String[rs.size()];
            for (int i = 0; i < rs.size(); i++) index[i] = searchText(rs.get(i));
            indexFor = rs;
            forgeShown = null;
        }
        String q = forgeQuery == null ? "" : forgeQuery.getValue().toLowerCase(Locale.ROOT).trim();
        if (forgeShown == null || !q.equals(forgeShownQ)) {
            String[] words = q.isEmpty() ? new String[0] : q.split("\\s+");
            List<CoreForge.Recipe> out = new ArrayList<>();
            outer:
            for (int i = 0; i < rs.size(); i++) {
                for (String w : words) if (!index[i].contains(w)) continue outer;      // every word must match somewhere
                out.add(rs.get(i));
            }
            forgeShown = out;
            forgeShownQ = q;
        }
        return forgeShown;
    }

    private static String searchText(CoreForge.Recipe r) {
        StringBuilder b = new StringBuilder(r.id()).append('\n');
        try {
            ItemStack out = r.result();
            b.append(out.getHoverName().getString()).append('\n').append(BuiltInRegistries.ITEM.getKey(out.getItem())).append('\n');
            for (CoreForge.Ing in : r.in()) {
                b.append(in.stack(1).getHoverName().getString()).append('\n').append(in.item()).append('\n');
                if (in.universe() != null) b.append(in.universe()).append('\n');
            }
        } catch (RuntimeException ignored) {
            // a name that cannot be worked out just isn't searchable
        }
        return b.toString().toLowerCase(Locale.ROOT);
    }

    private void forgeSearched() {
        forgeScroll = 0;
        List<CoreForge.Recipe> s = forgeShown();
        String q = forgeQuery.getValue().trim();
        code.emit("filter", "catalogue.filter(\"" + q + "\") -> " + s.size() + (s.size() == 1 ? " match" : " matches"));
    }

    /** Whether the player has everything for a recipe (worked out a few times a second, not every frame). */
    private boolean makeable(CoreForge.Recipe r) {
        var player = minecraft != null ? minecraft.player : null;
        if (player == null) return false;
        long now = System.currentTimeMillis();
        if (now - makeableAt > 250) {
            makeable.clear();
            makeableAt = now;
        }
        return makeable.computeIfAbsent(r.id(), k -> CoreForge.canMake(player, r));
    }

    private void select(CoreForge.Recipe r) {
        selectedId = r.id();
        ingScroll = 0;
        say("load " + r.id() + " - simulate its universe");
        int[] k = laws();
        ForgeLaws.Problem p = ForgeDriftClient.problem(r.id());
        code.emit("}");
        code.emit("forge.load(\"" + r.id() + "\")  // " + r.in().size() + " materials");
        code.emit(String.format("let u = universe.seed(0x%04x)", ForgeUniverse.tagOf(r.id())));
        code.emit("u.bind(laws = " + lawList(k) + ")");
        code.emit(String.format("while !stable(u) {  // χ² = %.1f", chi(p, k)));
    }

    private static double chi(ForgeLaws.Problem p, int[] k) {
        double s = 0;
        for (double v : ForgeLaws.residuals(p, k)) s += v * v;
        return s;
    }

    private static String lawList(int[] k) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < k.length; i++) b.append(i > 0 ? "," : "").append(String.format("%+d", k[i]));
        return b.append(']').toString();
    }

    /** A constant of nature was changed (by a click, a drag or the wheel): set it and tell the program. */
    private void setLaw(int i, int value) {
        int[] k = laws();
        value = Mth.clamp(value, ForgeLaws.MIN, ForgeLaws.MAX);
        if (k[i] == value) return;
        ForgeLaws.Problem p = ForgeDriftClient.problem(recipe().id());
        boolean was = ForgeLaws.converged(p, k);
        double before = chi(p, k);
        k[i] = value;
        double after = chi(p, k);
        code.emit("law" + i, String.format("  law[%s] := %+d  // Δχ² %+.1f", ForgeLaws.SYMBOL[i], value, after - before));
        boolean now = ForgeLaws.converged(p, k);
        if (now && !was) {
            code.emit(String.format("  assert stable(u) -> OK  // χ² = %.2f", after));
            code.emit("  break  // universe LOCKED");
            say(recipe().id() + ": the simulation converges");
        } else if (was && !now) {
            code.emit("  // WARNING: u destabilised");
        }
    }

    private int lawFromBar(double mx) {
        return (int) Math.round(ForgeLaws.MIN + (mx - barX) / (barW - 2) * (ForgeLaws.MAX - ForgeLaws.MIN));
    }

    private static String tabName(int t) {
        return t == 0 ? "materialize" : t == 1 ? "forge" : "shutdown";
    }

    // ------------------------------------------------------------------ drawing helpers

    /** A thin frame with its title written into the top edge, like a window of an old terminal program. */
    private void panel(GuiGraphics g, int x, int y, int w, int h, String title, int accent) {
        g.fill(x, y, x + w, y + h, 0x40000000);
        g.renderOutline(x, y, w, h, FAINT);
        // brighter corners
        int c = accent;
        g.fill(x, y, x + 5, y + 1, c);
        g.fill(x, y, x + 1, y + 5, c);
        g.fill(x + w - 5, y, x + w, y + 1, c);
        g.fill(x + w - 1, y, x + w, y + 5, c);
        g.fill(x, y + h - 1, x + 5, y + h, c);
        g.fill(x, y + h - 5, x + 1, y + h, c);
        g.fill(x + w - 5, y + h - 1, x + w, y + h, c);
        g.fill(x + w - 1, y + h - 5, x + w, y + h, c);
        if (title != null && w > 24) {
            String t = cut(title, w - 16);
            g.fill(x + 6, y - 1, x + 10 + font.width(t), y + 1, 0xFF020A05);
            g.drawString(font, t, x + 8, y - 4, accent, false);
        }
    }

    /** The text cut to fit {@code w} pixels, ending in "…" if anything was cut. */
    private String cut(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, Math.max(0, w - font.width("…"))) + "…";
    }

    private static boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // tooltips are drawn last, over everything
    private Runnable tip;

    private void itemTip(ItemStack s, int mx, int my, Component extra) {
        tip = () -> {
            List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, s));
            if (extra != null) lines.add(extra);
            TextFit.tooltip(lastG, font, lines, mx, my);
        };
    }

    private GuiGraphics lastG;

    // what the mouse rests on; the program "inspects" it after a moment
    private String hoverKey, hoverLine, hoverPrev, hoverShown;
    private long hoverSince;

    private void hover(String key, String line) {
        hoverKey = key;
        hoverLine = line;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        boolean alarm = phase == SHUTDOWN || tab == 2;
        // the monitor: bezel, screen, and the frames of its windows
        g.fill(0, 0, width, height, 0xFF070908);
        g.renderOutline(L - 3, T - 3, W + 6, H + 6, 0xFF1E2621);
        g.fill(L, T, L + W, T + H, alarm ? 0xF8100403 : 0xF8020A05);
        g.renderOutline(L, T, W, H, alarm ? 0xFF803020 : DIM);
        g.fill(L + 4, T + 15, L + W - 4, T + 16, FAINT);
        int acc = alarm ? 0xFF803020 : DIM;
        if (phase == SHUTDOWN || tab == 2) panel(g, inL, CT, inW, contentBottom - CT, phase == SHUTDOWN ? "shutdown/" : "shutdown/confirm", acc);
        else if (tab == 0) panel(g, inL, CT, inW, contentBottom - CT, "registry/", acc);
        else if (phase != REBORN) panel(g, inL, CT, inW, contentBottom - CT, "forge/", acc);
        else {
            CoreForge.Recipe r = recipe();
            panel(g, listX, CT, listW, contentBottom - CT, "catalogue/", acc);
            panel(g, centreX, CT, centreW, contentBottom - CT, "recipe/" + r.id(), acc);
            panel(g, rightX, viewY, rightW, viewH, String.format("simulation/universe#%04x", ForgeUniverse.tagOf(r.id())), acc);
            panel(g, rightX, obsY, rightW, obsH, "observables/", acc);
            // the search line of the catalogue
            g.drawString(font, ">", listX + 5, CT + 7, forgeQuery.isFocused() ? GREEN : DIM, false);
            g.fill(listX + 4, CT + 17, listX + listW - 4, CT + 18, FAINT);
        }
        panel(g, inL, BY, logW, BH - 4, "terminal/", acc);
        String ct = "core.exec  ln " + String.format("%04d", code.lines() % 10000);
        if (tab == 1 && phase == REBORN) ct += String.format("  χ² %.1f", universe.chi2());
        panel(g, codeX, BY, codeW, BH - 4, ct, acc);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        smooth.frame();
        lastG = g;
        tip = null;
        hoverKey = null;           // set again below by whatever the mouse is over this frame
        super.render(g, mx, my, partial);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        g.drawString(font, cut("OMNIPOTENCE//CORE  ::  " + (phase == REBORN ? "REKINDLED" : phase == SHUTDOWN ? "SHUTDOWN" : "INTACT"), W - 80), L + 6, T + 5, GREEN, false);
        String clock = String.format("t+%06.1fs", (System.currentTimeMillis() - opened) / 1000.0);
        g.drawString(font, clock, L + W - 6 - font.width(clock), T + 5, DIM, false);
        // tabs, drawn as console commands
        tabButton(g, L + 6, T + 20, "[ MATERIALIZE ]", tab == 0, mx, my, phase != SHUTDOWN);
        tabButton(g, L + 96, T + 20, "[ FORGE ]", tab == 1, mx, my, phase != SHUTDOWN);
        if (phase == INTACT) tabButton(g, L + W - 96, T + 20, "[ SHUTDOWN ]", tab == 2, mx, my, true);
        if (phase == SHUTDOWN) renderShutdown(g);
        else if (tab == 2) renderWarning(g, mx, my);
        else if (tab == 1) renderForge(g, mx, my);
        else renderMaterialize(g, mx, my);
        renderBottom(g, blink);
        // a thing the mouse has rested on for a moment is "inspected" by the program (once)
        long now = System.currentTimeMillis();
        if (hoverKey != null && !hoverKey.equals(hoverPrev)) hoverSince = now;   // the mouse moved onto something new
        hoverPrev = hoverKey;
        if (hoverKey != null && !hoverKey.equals(hoverShown) && now - hoverSince > 350) {
            code.emit("hover", hoverLine);
            hoverShown = hoverKey;
        }
        if (hoverKey == null) hoverShown = null;
        crt(g);
        if (tip != null) tip.run();
    }

    private void tabButton(GuiGraphics g, int bx, int by, String label, boolean on, int mx, int my, boolean enabled) {
        boolean over = enabled && mx >= bx && mx < bx + font.width(label) && my >= by && my < by + 9;
        g.drawString(font, label, bx, by, !enabled ? FAINT : on ? AMBER : over ? GREEN : DIM, false);
    }

    private boolean inTab(double mx, double my, int bx, int by, String label) {
        return mx >= bx && mx < bx + font.width(label) && my >= by && my < by + 9;
    }

    /** The glass of the monitor: faint scanlines, a slow bright sweep, darker edges and a rare flicker. */
    private void crt(GuiGraphics g) {
        for (int sy = T; sy < T + H; sy += 2) g.fill(L, sy, L + W, sy + 1, 0x12000000);
        int sweep = (int) ((System.currentTimeMillis() / 12) % (H + 40)) - 20;
        if (sweep > 0 && sweep < H) g.fill(L, T + sweep, L + W, T + Math.min(H, sweep + 3), 0x0C5CFF8A);
        long q = System.currentTimeMillis() / 90;
        if ((q * 0x9E3779B1L >>> 20 & 63) == 0) g.fill(L, T, L + W, T + H, 0x0A5CFF8A);
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.NORMAL);
        int e = Math.min(28, Math.min(W, H) / 8);
        f.rectH(L, T, L + e, T + H, 0, 0.35F, 0, 0);
        f.rectH(L + W - e, T, L + W, T + H, 0, 0, 0, 0.35F);
        f.rectV(L, T, L + W, T + e, 0, 0.3F, 0, 0);
        f.rectV(L, T + H - e, L + W, T + H, 0, 0, 0, 0.3F);
        f.end();
    }

    private void renderBottom(GuiGraphics g, boolean blink) {
        // the terminal log: as many of the latest lines as fit, the newest one bright with a blinking cursor
        int rows = Math.max(1, (BH - 4 - 10) / 9);
        int from = Math.max(0, log.size() - rows);
        int ly = BY + 6;
        for (int i = from; i < log.size(); i++) {
            boolean lastLine = i == log.size() - 1;
            g.drawString(font, cut("> " + log.get(i), logW - 12) + (lastLine && blink ? "_" : ""), inL + 5, ly, lastLine ? GREEN : DIM, false);
            ly += 9;
        }
        // the code display
        code.frame(this::idleLine, tab == 1 && phase == REBORN ? 0.45 : 1.2);
        code.render(g, font, codeX + 3, BY + 6, codeW - 6, BH - 4 - 10);
    }

    /** What the program does while nobody touches anything: its main loop keeps ticking. */
    private String idleLine(int step) {
        if (phase == SHUTDOWN) return switch (step % 3) {
            case 0 -> "shutdown.tick(" + secondsLeft() + ")  // transcendental collapsing";
            case 1 -> String.format("  entropy.release(%.3f)", 1 - secondsLeft() / 60.0);
            default -> "  assert core.alive() -> FAIL";
        };
        if (tab == 2) return step % 2 == 0 ? "await operator.confirm()  // irreversible" : "  sleep(1000)";
        if (tab == 0) return switch (step % 3) {
            case 0 -> "registry.index(" + String.format("%,d", shown.size()) + ")  // entries in view";
            case 1 -> "  gc.sweep()  // " + (step * 37 % 900 + 100) + "k freed";
            default -> null;
        };
        if (phase != REBORN) return step % 2 == 0 ? "forge.status() -> offline  // the Core was never rekindled" : null;
        int worst = 0;
        for (int i = 1; i < lastRes.length; i++) if (Math.abs(lastRes[i]) > Math.abs(lastRes[worst])) worst = i;
        boolean conv = ForgeLaws.converged(ForgeDriftClient.problem(recipe().id()), laws());
        int tick = step;
        if (conv) return switch (step % 4) {
            case 0 -> String.format("  u.hold()  // tick %05d", tick);
            case 1 -> String.format("  χ² = %.2f  // within tolerance", universe.chi2());
            case 2 -> "  assert stable(u) -> OK";
            default -> "  await compile()";
        };
        String name = Component.translatable("gui.theoreticalsciences.observable." + worst).getString();
        return switch (step % 4) {
            case 0 -> String.format("  u.step(dt=0.016)  // tick %05d", tick);
            case 1 -> String.format("  χ² = %.2f; λ = %+.3f", universe.chi2(), (universe.instability() - 0.05) * 2);
            case 2 -> String.format("  worst = obs[%d] \"%s\" %+.1f", worst, name, lastRes[worst]);
            default -> "  assert stable(u) -> FAIL";
        };
    }

    private void renderMaterialize(GuiGraphics g, int mx, int my) {
        g.drawString(font, "registry> query:", inL + 5, CT + 7, DIM, false);
        g.fill(query.getX() - 2, CT + 17, inL + inW - 5, CT + 18, FAINT);
        int rows = (shown.size() + gridCols - 1) / gridCols;
        scroll = Math.max(0, Math.min(Math.max(0, rows - gridRows), scroll));
        for (int i = 0; i < gridCols * gridRows; i++) {
            int idx = scroll * gridCols + i;
            if (idx >= shown.size()) break;
            int sx = gridX + (i % gridCols) * 18, sy = gridY + (i / gridCols) * 18;
            boolean over = mx >= sx && my >= sy && mx < sx + 18 && my < sy + 18;
            g.renderOutline(sx, sy, 18, 18, over ? GREEN : FAINT);
            g.renderItem(shown.get(idx), sx + 1, sy + 1);
            boolean locked = OmnipotencePayload.denied(shown.get(idx).getItem());
            if (locked) {                                                     // the Core will not hand these out
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x50FF2020);
                g.fill(sx + 12, sy + 12, sx + 17, sy + 17, 0xC0400000);
                g.fill(sx + 13, sy + 14, sx + 16, sy + 16, 0xFFFF5050);
                g.pose().popPose();
            }
            if (over) {
                ItemStack s = shown.get(idx);
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, locked ? 0x30FF3030 : 0x305CFF8A);
                tip = () -> lastG.renderTooltip(font, s, mx, my);
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                hover("m:" + id, "inspect(registry[\"" + id + "\"])" + (locked ? "  // DENIED" : ""));
            }
        }
        String info = String.format("%,d entries  page %d/%d  [click: materialize  shift: a stack]", shown.size(),
                Math.min(scroll + 1, Math.max(1, rows)), Math.max(1, rows - gridRows + 1));
        g.drawString(font, cut(info, inW - 12), inL + 6, contentBottom - 11, DIM, false);
    }

    private void renderShutdown(GuiGraphics g) {
        int s = secondsLeft();
        int cx = L + W / 2, mid = (CT + contentBottom) / 2;
        int scale = Mth.clamp((contentBottom - CT) / 30, 2, 8);
        g.drawCenteredString(font, Component.translatable("title.theoreticalsciences.shutdown").withStyle(ChatFormatting.BOLD), cx, mid - scale * 5 - 14, RED);
        g.pose().pushPose();
        g.pose().translate(cx, mid - scale * 4, 0);
        g.pose().scale(scale, scale, 1);
        String t = String.valueOf(s);
        g.drawString(font, t, -font.width(t) / 2, 0, s <= 10 ? RED : AMBER, false);
        g.pose().popPose();
        TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.core_shutdown_running", s), cx, mid + scale * 5 + 4, inW - 20, AMBER, 2, true);
    }

    private void renderWarning(GuiGraphics g, int mx, int my) {
        int ww = Math.min(inW - 40, 420);
        warnX = L + W / 2 - ww / 2;
        int ty = CT + 10;
        g.drawCenteredString(font, Component.translatable("gui.theoreticalsciences.core_shutdown").withStyle(ChatFormatting.BOLD), L + W / 2, ty, RED);
        ty += 16;
        int room = Math.max(1, (contentBottom - 30 - ty) / 10);
        ty += 10 * TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.core_shutdown_warn"), warnX, ty, ww, 0xFFFFB0A0, room, false);
        String yes = "[ " + Component.translatable("gui.theoreticalsciences.core_shutdown_confirm").getString() + " ]";
        String no = "[ " + Component.translatable("gui.theoreticalsciences.core_shutdown_cancel").getString() + " ]";
        warnYes = Math.min(ty + 6, contentBottom - 24);
        warnNo = warnYes + 12;
        tabButton(g, warnX, warnYes, yes, false, mx, my, true);
        tabButton(g, warnX, warnNo, no, false, mx, my, true);
    }

    // ------------------------------------------------------------------ the Forge

    private void renderForge(GuiGraphics g, int mx, int my) {
        if (phase != REBORN) {
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.core_forge_locked"), L + W / 2, (CT + contentBottom) / 2 - 10, inW - 40, DIM, 4, true);
            return;
        }
        var player = minecraft != null ? minecraft.player : null;
        CoreForge.Recipe r = recipe();
        // ---- left: the catalogue (filtered by the search box)
        List<CoreForge.Recipe> rs = forgeShown();
        forgeScroll = Math.max(0, Math.min(Math.max(0, rs.size() - listRows), forgeScroll));
        int rowR = listX + listW - 5;
        for (int k = 0; k < listRows && forgeScroll + k < rs.size(); k++) {
            CoreForge.Recipe e = rs.get(forgeScroll + k);
            int ry = listRowsTop + k * ROW;
            boolean over = over(mx, my, listX + 2, ry, rowR - listX - 2, ROW - 1);
            boolean ok = makeable(e);
            if (e.id().equals(selectedId)) {
                g.fill(listX + 2, ry, rowR, ry + ROW - 1, 0x405CFF8A);
                g.fill(listX + 2, ry, listX + 3, ry + ROW - 1, AMBER);
            } else if (over) g.fill(listX + 2, ry, rowR, ry + ROW - 1, 0x205CFF8A);
            ItemStack out = e.result().copyWithCount(1);
            g.renderItem(out, listX + 4, ry);
            String name = out.getHoverName().getString();
            g.drawString(font, cut(name, rowR - listX - 25), listX + 23, ry + 5, ok ? GREEN : RED, false);
            if (over) {
                itemTip(out, mx, my, Component.literal(e.id()).withStyle(ChatFormatting.DARK_GRAY));
                hover("r:" + e.id(), "inspect(recipe[\"" + e.id() + "\"]) -> " + (ok ? "materials OK" : "materials missing"));
            }
        }
        if (rs.isEmpty()) g.drawString(font, cut("no match", listW - 12), listX + 6, listRowsTop + 4, FAINT, false);
        String foot = rs.isEmpty() ? "0/" + CoreForge.all().size() : (forgeScroll + 1) + "-" + Math.min(rs.size(), forgeScroll + listRows) + "/" + rs.size();
        g.drawString(font, cut(foot, listW - 12), listX + 6, contentBottom - 11, FAINT, false);
        if (rs.size() > listRows) {                 // a thin scroll bar along the right edge
            int top = listRowsTop, h = listRows * ROW - 1;
            int th = Math.max(6, h * listRows / rs.size());
            int ty = top + (h - th) * forgeScroll / Math.max(1, rs.size() - listRows);
            g.fill(listX + listW - 3, top, listX + listW - 2, top + h, FAINT);
            g.fill(listX + listW - 4, ty, listX + listW - 2, ty + th, DIM);
        }

        // ---- middle: what it makes, what it needs, the constants of nature
        ItemStack out = r.result();
        int ox = centreX + 5, oy = CT + 5;
        g.renderItem(out, ox, oy);
        g.renderItemDecorations(font, out, ox, oy);
        String outName = out.getHoverName().getString();
        g.drawString(font, cut(outName, centreW - 30), ox + 20, oy + 4, AMBER, false);
        if (over(mx, my, ox, oy, centreW - 10, 16)) itemTip(out, mx, my, null);
        g.fill(centreX + 4, ingY - 4, centreX + centreW - 4, ingY - 3, FAINT);
        int n = r.in().size();
        int rowsNeeded = (n + ingCols - 1) / ingCols;
        ingScroll = Math.max(0, Math.min(Math.max(0, rowsNeeded - ingRowsShown), ingScroll));
        for (int k = 0; k < n; k++) {
            int row = k / ingCols - ingScroll;
            if (row < 0 || row >= ingRowsShown) continue;
            CoreForge.Ing in = r.in().get(k);
            int ix = centreX + 5 + (k % ingCols) * ingCellW, iy = ingY + row * 17;
            ItemStack s = in.stack(1);
            int have = player != null ? CoreForge.count(player, in) : 0;
            int need = Math.max(1, in.count());
            g.renderItem(s, ix, iy);
            String full = in.count() == 0 ? (have > 0 ? "held" : "needed") + " (kept)" : have + "/" + need;
            String label = font.width(full) <= ingCellW - 20 ? full : in.count() == 0 ? (have > 0 ? "held" : "needed") : compact(have) + "/" + compact(need);
            g.drawString(font, cut(label, ingCellW - 20), ix + 18, iy + 4, have >= need ? GREEN : RED, false);
            if (over(mx, my, ix, iy, ingCellW - 2, 16)) {
                String hn = in.count() == 0 ? (have > 0 ? "held - not used up" : "needed - not used up") : "have " + have + " / need " + need;
                itemTip(s, mx, my, Component.literal(hn).withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
                hover("i:" + in.item() + k, "inspect(\"" + in.item() + "\") -> " + (in.count() == 0 ? (have > 0 ? "held" : "missing") : have + "/" + need));
            }
        }
        int[] k = laws();
        double[] sk = shownLaws(k, r.id());
        ForgeLaws.Problem p = ForgeDriftClient.problem(r.id());
        double[] res = ForgeLaws.residuals(p, sk);
        lastRes = res;
        boolean conv = ForgeLaws.converged(p, k);
        g.fill(centreX + 4, lawsY - 2, centreX + centreW - 4, lawsY - 1, FAINT);
        // more materials than fit: the wheel scrolls them, and this shows which way there is more
        String more = rowsNeeded <= ingRowsShown ? "" : (ingScroll > 0 ? "▲" : "") + (ingScroll + ingRowsShown < rowsNeeded ? "▼" : "") + " " + n + " mats";
        if (!more.isEmpty()) g.drawString(font, more, lawsX + lawsW - font.width(more), lawsY + 1, AMBER, false);
        g.drawString(font, cut("constants of nature", lawsW - (more.isEmpty() ? 0 : font.width(more) + 4)), lawsX, lawsY + 1, DIM, false);
        for (int i = 0; i < ForgeLaws.N; i++) {
            int ry = lawsY + 11 + i * lawRow;
            boolean drag = dragLaw == i;
            g.drawString(font, ForgeLaws.SYMBOL[i], lawsX, ry + 1, AMBER, false);
            g.drawString(font, "«", lawsX + 14, ry + 1, over(mx, my, lawsX + 12, ry, 9, 10) ? GREEN : DIM, false);
            g.drawString(font, "‹", lawsX + 24, ry + 1, over(mx, my, lawsX + 22, ry, 9, 10) ? GREEN : DIM, false);
            g.fill(barX, ry + 3, barX + barW, ry + 6, FAINT);
            g.fill(barX + barW / 2, ry + 2, barX + barW / 2 + 1, ry + 7, DIM);           // the zero mark
            double fp = (sk[i] - ForgeLaws.MIN) / (double) (ForgeLaws.MAX - ForgeLaws.MIN) * (barW - 2);
            int pos = barX + (int) Math.floor(fp);
            g.fill(barX, ry + 3, pos, ry + 6, 0x4060FF90);                    // the part of the scale it has swept
            g.fill(pos, ry, pos + 2, ry + 9, drag ? AMBER : GREEN);
            if (fp - Math.floor(fp) > 0.25) g.fill(pos + 2, ry + 1, pos + 3, ry + 8, 0x8060FF90);   // sub-pixel glow: no stepping
            g.drawString(font, "›", barX + barW + 3, ry + 1, over(mx, my, barX + barW + 1, ry, 9, 10) ? GREEN : DIM, false);
            g.drawString(font, "»", barX + barW + 13, ry + 1, over(mx, my, barX + barW + 11, ry, 9, 10) ? GREEN : DIM, false);
            String v = String.format("%+d", Math.round(sk[i]));
            g.drawString(font, v, lawsX + lawsW - font.width(v), ry + 1, GREEN, false);
            if (over(mx, my, lawsX, ry, lawsW, lawRow) && dragLaw < 0) {
                int li = i;
                tip = () -> TextFit.tooltip(lastG, font, List.of(Component.translatable("gui.theoreticalsciences.law." + li),
                        Component.translatable("gui.theoreticalsciences.law_hint").withStyle(ChatFormatting.GRAY)), mx, my);
                hover("l:" + i, String.format("watch law[%s]  // = %+d", ForgeLaws.SYMBOL[i], k[i]));
            }
        }

        // ---- right: the simulated universe and its observables
        universe.update(r.id(), res, conv);
        universe.render(g, font, rightX + 3, viewY + 5, rightW - 6, viewH - 8, GREEN, AMBER, RED, DIM);
        int ox2 = rightX + 5, ow = rightW - 10;
        int nameW = Math.min(96, (int) (ow * 0.42));
        boolean values = ow >= 150;
        int bx0 = ox2 + nameW + 4, bx1 = ox2 + ow - (values ? 32 : 2);
        int bc = (bx0 + bx1) / 2, half = Math.max(4, (bx1 - bx0) / 2);
        double chi = 0;
        for (int i = 0; i < ForgeLaws.N; i++) {
            int ry = obsY + 7 + i * OBS_ROW;
            double v = res[i];
            chi += v * v;
            boolean ok = Math.abs(v) <= ForgeLaws.TOLERANCE;
            Component name = Component.translatable("gui.theoreticalsciences.observable." + i);
            TextFit.draw(g, font, name, ox2, ry, nameW, ok ? GREEN : AMBER);
            g.fill(bc - half, ry + 3, bc + half, ry + 5, FAINT);
            g.fill(bc - 1, ry, bc + 1, ry + 8, DIM);
            int len = (int) Math.round(Mth.clamp(v / 60.0, -1, 1) * half);
            if (len != 0) g.fill(Math.min(bc, bc + len), ry + 2, Math.max(bc, bc + len), ry + 6, ok ? GREEN : Math.abs(v) < 15 ? AMBER : RED);
            if (values) {
                String vs = String.format("%+.1f", v);
                g.drawString(font, cut(vs, 30), ox2 + ow - font.width(cut(vs, 30)), ry, ok ? GREEN : DIM, false);
            }
            if (over(mx, my, ox2, ry, ow, OBS_ROW)) {
                String vv = String.format("%+.1f (stable within ±%.1f)", v, ForgeLaws.TOLERANCE);
                tip = () -> TextFit.tooltip(lastG, font, List.of(name, Component.literal(vv).withStyle(ChatFormatting.GRAY)), mx, my);
            }
        }
        int sy = obsY + 7 + ForgeLaws.N * OBS_ROW + 1;
        String state = conv ? "STABLE - recipe can compile" : String.format("DIVERGENT  χ² = %.1f", chi);
        g.drawString(font, cut(state, ow), ox2, sy, conv ? GREEN : RED, false);
        // round 36: the materials the Reconstitutor also makes drift - new laws after each forging, then a wait
        int wait = ForgeDriftClient.secondsLeft(r.id());
        int ep = ForgeDriftClient.epoch(r.id());
        if (r.id().equals(shownEpochFor) && ep != shownEpoch) {
            say("the laws of " + r.id() + " drifted - tune the universe again");
            code.emit("u = universe.reseed(epoch = " + ep + ")  // the last universe is spent");
        }
        shownEpochFor = r.id();
        shownEpoch = ep;
        boolean can = conv && wait == 0 && player != null && CoreForge.canMake(player, r);
        String compile = "[ COMPILE ]";
        boolean overC = can && inTab(mx, my, compileX, compileY, compile);
        g.drawString(font, compile, compileX, compileY, !can ? FAINT : overC ? AMBER : GREEN, false);
        if (can && com.theoreticalsciences.transcend.ForgeDrift.drifts(r.id()) && ow > font.width(compile) + 30)
            g.drawString(font, cut("laws drift after forging", ow - font.width(compile) - 6), compileX + font.width(compile) + 6, compileY, DIM, false);
        if (!can && ow > font.width(compile) + 30) {
            String why = wait > 0 ? String.format("re-seeding %d:%02d", wait / 60, wait % 60) : !conv ? "diverges" : "missing materials";
            g.drawString(font, cut(why, ow - font.width(compile) - 6), compileX + font.width(compile) + 6, compileY, wait > 0 ? AMBER : FAINT, false);
        }
        if (inTab(mx, my, compileX, compileY, compile)) hover("c", "dryrun compile(\"" + r.id() + "\")  // " + (can ? "ready" : "not ready"));
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != SHUTDOWN) {
            if (inTab(mx, my, L + 6, T + 20, "[ MATERIALIZE ]")) return setTab(0);
            if (inTab(mx, my, L + 96, T + 20, "[ FORGE ]")) return setTab(1);
            if (phase == INTACT && inTab(mx, my, L + W - 96, T + 20, "[ SHUTDOWN ]")) return setTab(2);
        }
        if (super.mouseClicked(mx, my, button)) return true;
        if (phase == SHUTDOWN) return false;
        if (tab == 2) {
            String yes = "[ " + Component.translatable("gui.theoreticalsciences.core_shutdown_confirm").getString() + " ]";
            String no = "[ " + Component.translatable("gui.theoreticalsciences.core_shutdown_cancel").getString() + " ]";
            if (inTab(mx, my, warnX, warnYes, yes)) {
                code.emit("shutdown.begin()  // 60 s");
                PacketDistributor.sendToServer(new CorePayloads.Action(core, 0, ""));
                onClose();
                return true;
            }
            if (inTab(mx, my, warnX, warnNo, no)) {
                code.emit("operator.cancel()  // shutdown aborted");
                return setTab(0);
            }
            return false;
        }
        if (tab == 1) return forgeClick(mx, my);
        for (int i = 0; i < gridCols * gridRows; i++) {
            int idx = scroll * gridCols + i;
            if (idx >= shown.size()) break;
            int sx = gridX + (i % gridCols) * 18, sy = gridY + (i / gridCols) * 18;
            if (mx >= sx && my >= sy && mx < sx + 18 && my < sy + 18) {
                ItemStack s = shown.get(idx);
                int count = hasShiftDown() ? s.getMaxStackSize() : 1;
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                PacketDistributor.sendToServer(new OmnipotencePayload(core, id, count));
                if (OmnipotencePayload.denied(s.getItem())) {
                    say("materialize " + id + " ... ACCESS DENIED");
                    code.emit("registry.write(\"" + id + "\", " + count + ") -> DENIED  // beyond the Core's reach");
                } else {
                    say("materialize " + id + " x" + count + " ... ok");
                    code.emit("registry.write(\"" + id + "\", " + count + ") -> OK");
                }
                return true;
            }
        }
        return false;
    }

    private boolean setTab(int t) {
        if (t != tab) code.emit("exec tab." + tabName(t) + "()");
        tab = t;
        refresh();
        if (t == 2) say("WARNING: shutdown requested - confirm below");
        if (t == 0) setFocused(query);
        if (t == 1 && phase == REBORN) setFocused(forgeQuery);
        return true;
    }

    private boolean forgeClick(double mx, double my) {
        if (phase != REBORN) return false;
        List<CoreForge.Recipe> rs = forgeShown();
        for (int k = 0; k < listRows && forgeScroll + k < rs.size(); k++) {
            int ry = listRowsTop + k * ROW;
            if (over(mx, my, listX + 2, ry, listW - 7, ROW - 1)) {
                select(rs.get(forgeScroll + k));
                return true;
            }
        }
        int[] laws = laws();
        for (int i = 0; i < ForgeLaws.N; i++) {
            int ry = lawsY + 11 + i * lawRow;
            int step = over(mx, my, lawsX + 12, ry, 9, 10) ? -10 : over(mx, my, lawsX + 22, ry, 9, 10) ? -1
                    : over(mx, my, barX + barW + 1, ry, 9, 10) ? 1 : over(mx, my, barX + barW + 11, ry, 9, 10) ? 10 : 0;
            if (step != 0) {
                setLaw(i, laws[i] + step);
                return true;
            }
            if (mx >= barX && mx < barX + barW && my >= ry && my < ry + 10) {   // click on the scale: set it there (and drag to slide it)
                setLaw(i, lawFromBar(mx));
                dragLaw = i;
                return true;
            }
        }
        if (inTab(mx, my, compileX, compileY, "[ COMPILE ]")) {
            CoreForge.Recipe r = recipe();
            var player = minecraft != null ? minecraft.player : null;
            ForgeLaws.Problem p = ForgeDriftClient.problem(r.id());
            code.emit("compile(\"" + r.id() + "\", " + lawList(laws) + ")");
            int wait = ForgeDriftClient.secondsLeft(r.id());
            if (wait > 0) {
                say(String.format("compile %s ... WAIT: the Core is re-seeding the universe (%d:%02d)", r.id(), wait / 60, wait % 60));
                code.emit("  await core.reseed()  // the last universe is spent");
                code.emit("  return DEFERRED");
            } else if (!ForgeLaws.converged(p, laws)) {
                say("compile " + r.id() + " ... REJECTED: the simulation diverges");
                code.emit(String.format("  assert stable(u) -> FAIL  // χ² = %.1f", chi(p, laws)));
                code.emit("  return REJECTED");
            } else if (player == null || !CoreForge.canMake(player, r)) {
                say("compile " + r.id() + " ... REJECTED: missing materials");
                code.emit("  assert stable(u) -> OK");
                code.emit("  assert inventory.has(bill) -> FAIL  // missing materials");
                code.emit("  return REJECTED");
            } else {
                PacketDistributor.sendToServer(new CorePayloads.Action(core, 1, r.id() + ";" + ForgeLaws.encode(laws)));
                say("compile " + r.id() + " ... accepted");
                code.emit("  assert stable(u) -> OK");
                code.emit("  net.send(Action(core, 1, \"" + r.id() + "\"))");
                code.emit("  materialize(\"" + r.result().getHoverName().getString() + "\") -> ACCEPTED");
            }
            makeableAt = 0;                       // materials change after a forge: check them again
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (tab == 1 && dragLaw >= 0) {
            setLaw(dragLaw, lawFromBar(mx));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragLaw >= 0) code.emit(String.format("  u.resimulate()  // χ² = %.1f", chi(ForgeDriftClient.problem(recipe().id()), laws())));
        dragLaw = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int d = (int) Math.signum(dy);
        if (tab == 1) {
            if (phase != REBORN) return true;
            for (int i = 0; i < ForgeLaws.N; i++) {
                int ry = lawsY + 11 + i * lawRow;
                if (over(mx, my, lawsX, ry, lawsW, lawRow)) {       // fine-tune a constant with the wheel
                    setLaw(i, laws()[i] + d * (hasShiftDown() ? 10 : 1));
                    return true;
                }
            }
            if (over(mx, my, centreX, ingY, centreW, lawsY - ingY)) {          // more materials than fit: scroll them
                ingScroll = Math.max(0, ingScroll - d);
                return true;
            }
            int before = forgeScroll;
            forgeScroll = Math.max(0, Math.min(Math.max(0, forgeShown().size() - listRows), forgeScroll - d));
            if (forgeScroll != before) code.emit("scroll", "catalogue.seek(" + forgeScroll + ")");
            return true;
        }
        int rows = (shown.size() + gridCols - 1) / gridCols;
        scroll = Math.max(0, Math.min(Math.max(0, rows - gridRows), scroll - d));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // up / down move through the (filtered) catalogue, even while typing in the search box
        if (tab == 1 && phase == REBORN && (key == 264 || key == 265)) {
            List<CoreForge.Recipe> rs = forgeShown();
            if (!rs.isEmpty()) {
                int at = -1;
                for (int i = 0; i < rs.size(); i++) if (rs.get(i).id().equals(selectedId)) { at = i; break; }
                int next = Mth.clamp(at < 0 ? 0 : at + (key == 264 ? 1 : -1), 0, rs.size() - 1);
                if (next != at) {
                    select(rs.get(next));
                    if (next < forgeScroll) forgeScroll = next;
                    if (next >= forgeScroll + listRows) forgeScroll = next - listRows + 1;
                }
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 1234 -> 1.2k */
    private static String compact(int n) {
        if (n < 1000) return String.valueOf(n);
        if (n < 10_000) return String.format(java.util.Locale.ROOT, "%.1fk", n / 1000.0);
        if (n < 1_000_000) return (n / 1000) + "k";
        return String.format(java.util.Locale.ROOT, "%.1fM", n / 1e6);
    }
}
