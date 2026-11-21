package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.machine.tuning.CalibrationGames;
import com.theoreticalsciences.machine.tuning.ChemGames;
import com.theoreticalsciences.machine.tuning.TuningForkItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 1.2.0: a calibration game (see {@link CalibrationGames}), opened by the Tuning Fork, drawn as the machine's own
 * console (its colour, the holographic panel of the machine screens). Values are set on spinners: click the arrows at
 * either end or scroll over them (shift: five at a time). The answer is checked here first, with the same rules as on
 * the server, so a wrong one just says "not yet" (the Resonant Tuning Fork says how far off) and the game stays open.
 */
public class CalibrationScreen extends Screen {
    private static final int W = 340, H = 228, C = 6;
    private static final int TEXT = 0xFFE6F2FF, DIM = 0xFF8C9CB0, FAINT = 0xFF34404E, GOLD = 0xFFFFC860, GOOD = 0xFF70FF9A, BAD = 0xFFFF7060;

    private final BlockPos pos;
    private final int kind, tier;
    private final long seed;
    private final boolean hints;
    private final int accent, bright;
    private final long opened = System.currentTimeMillis();
    private int x0, y0;
    private Component note = Component.empty();
    private int noteColour = DIM;
    private long noteAt;

    /** A number set by clicking its arrows or scrolling over it. */
    private record Spinner(int x, int y, int w, int lo, int hi, IntSupplier get, IntConsumer set, java.util.function.IntFunction<String> fmt) {
        boolean over(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + 16;
        }
    }

    private final List<Spinner> spinners = new ArrayList<>();

    // WAVE
    private int[][] target, guess;
    // SIEVE
    private int start;
    private final int[] marked = new int[CalibrationGames.SIEVE_N];
    // CHAIN
    private CalibrationGames.Chain chain;
    private final List<Integer> proof = new ArrayList<>();
    // BALANCE, EQUILIBRIUM
    private ChemGames.Reaction rx;
    private int[] coef;
    // BUILD: per pool species, its side (0 unused, 1 reactant, 2 product) and coefficient
    private ChemGames.Build build;
    private int[] side, bcoef;
    // OXIDATION
    private int[] oxq, ox;
    // VERTEX
    private CalibrationGames.Vertex vertex;
    private final int[] poly = new int[CalibrationGames.SIDES.length];
    // CIPHER
    private CalibrationGames.Cipher cipher;
    private int[] key;
    // SYMMETRY
    private CalibrationGames.Symmetry sym;
    private int[] cells;
    // SPECTRUM
    private CalibrationGames.Spectrum spectrum;
    private final int[] spec = {0, 100};
    // FLOW
    private CalibrationGames.Flow flow;
    private int[] turns;
    // PATH, TREE
    private CalibrationGames.Graph graph;
    private int[] links;
    // NASH
    private CalibrationGames.Game game;
    private int pick = -1;

    public CalibrationScreen(BlockPos pos, int kind, long seed, int tier, boolean hints, int colour) {
        super(Component.translatable("gui.theoreticalsciences.calibrate.title"));
        this.pos = pos;
        this.kind = kind;
        this.seed = seed;
        this.tier = tier;
        this.hints = hints;
        this.accent = (colour & 0xFFFFFF) == 0 ? 0x60E0FF : colour & 0xFFFFFF;
        this.bright = 0xFF000000 | HoloStyle.bright(accent);
        switch (kind) {
            case CalibrationGames.WAVE -> {
                target = CalibrationGames.wave(seed, tier);
                guess = new int[target.length][];
                for (int i = 0; i < guess.length; i++) guess[i] = new int[]{1 + i, 0, 1};
            }
            case CalibrationGames.SIEVE -> start = CalibrationGames.sieveStart(seed, tier);
            case CalibrationGames.CHAIN -> chain = CalibrationGames.chain(seed, tier);
            case CalibrationGames.BALANCE -> {
                rx = ChemGames.balance(seed, tier);
                coef = new int[rx.species().size()];
                java.util.Arrays.fill(coef, 1);
            }
            case CalibrationGames.EQUILIBRIUM -> {
                rx = ChemGames.equilibrium(seed, tier);
                coef = new int[rx.species().size()];
            }
            case CalibrationGames.BUILD -> {
                build = ChemGames.build(seed, tier);
                side = new int[build.pool().size()];
                bcoef = new int[build.pool().size()];
                java.util.Arrays.fill(bcoef, 1);
            }
            case CalibrationGames.OXIDATION -> {
                oxq = ChemGames.oxidation(seed, tier);
                ox = new int[oxq.length];
            }
            case CalibrationGames.VERTEX -> vertex = CalibrationGames.vertex(seed, tier);
            case CalibrationGames.CIPHER -> {
                cipher = CalibrationGames.cipher(seed, tier);
                key = new int[cipher.key().length];
            }
            case CalibrationGames.SYMMETRY -> {
                sym = CalibrationGames.symmetry(seed, tier);
                cells = new int[sym.n() * sym.n()];
            }
            case CalibrationGames.SPECTRUM -> spectrum = CalibrationGames.spectrum(seed, tier);
            case CalibrationGames.FLOW -> {
                flow = CalibrationGames.flow(seed, tier);
                turns = new int[flow.tiles().length];
            }
            case CalibrationGames.NASH -> game = CalibrationGames.game(seed, tier);
            case CalibrationGames.PATH, CalibrationGames.TREE -> {
                graph = CalibrationGames.graph(seed, tier, kind == CalibrationGames.TREE);
                links = new int[graph.edges().length];
                for (int k = 0; k < links.length; k++) if (graph.forced()[k]) links[k] = 1;
            }
            default -> {}
        }
    }

    private String key() {
        return switch (kind) {
            case CalibrationGames.SIEVE -> "sieve";
            case CalibrationGames.CHAIN -> "chain";
            case CalibrationGames.BALANCE -> "balance";
            case CalibrationGames.SPECTRUM -> "spectrum";
            case CalibrationGames.FLOW -> "flow";
            case CalibrationGames.BUILD -> "build";
            case CalibrationGames.OXIDATION -> "oxidation";
            case CalibrationGames.EQUILIBRIUM -> "equilibrium";
            case CalibrationGames.VERTEX -> "vertex";
            case CalibrationGames.SYMMETRY -> "symmetry";
            case CalibrationGames.CIPHER -> "cipher";
            case CalibrationGames.PATH -> "path";
            case CalibrationGames.TREE -> "tree";
            case CalibrationGames.NASH -> "nash";
            default -> "wave";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        clearWidgets();
        spinners.clear();
        int fy = y0 + H - 22;
        addRenderableWidget(HoloButton.of(x0 + W - 92, fy, 84, 15, Component.translatable("gui.theoreticalsciences.calibrate.submit"), accent, this::submit));
        addRenderableWidget(HoloButton.of(x0 + 8, fy, 58, 15, Component.translatable("gui.theoreticalsciences.calibrate.cancel"), accent, this::onClose));
        switch (kind) {
            case CalibrationGames.WAVE -> {
                for (int c = 0; c < guess.length; c++) {
                    final int cc = c;
                    int y = y0 + 132 + c * 22;
                    spinners.add(new Spinner(x0 + 70, y, 76, 1, 5, () -> guess[cc][0], v -> guess[cc][0] = v, String::valueOf));
                    spinners.add(new Spinner(x0 + 158, y, 76, 0, 7, () -> guess[cc][1], v -> guess[cc][1] = Math.floorMod(v, 8),
                            v -> v == 0 ? "0" : v == 4 ? "π" : v + "π/4"));
                    spinners.add(new Spinner(x0 + 246, y, 76, 1, 3, () -> guess[cc][2], v -> guess[cc][2] = v, String::valueOf));
                }
            }
            case CalibrationGames.SIEVE -> addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15,
                    Component.translatable("gui.theoreticalsciences.calibrate.clear"), accent, () -> java.util.Arrays.fill(marked, 0)));
            case CalibrationGames.CHAIN -> addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15,
                    Component.translatable("gui.theoreticalsciences.calibrate.undo"), accent, () -> {
                        if (!proof.isEmpty()) proof.remove(proof.size() - 1);
                    }));
            case CalibrationGames.BALANCE -> {
                for (int s = 0; s < coef.length; s++) {
                    final int ss = s;
                    spinners.add(new Spinner(slotX(s, coef.length) - 21, y0 + 96, 42, 1, ChemGames.MAX_COEF, () -> coef[ss], v -> coef[ss] = v, String::valueOf));
                }
            }
            case CalibrationGames.EQUILIBRIUM -> {
                for (int s = 0; s < coef.length; s++) {
                    final int ss = s;
                    spinners.add(new Spinner(slotX(s, coef.length) - 21, y0 + 96, 42, -6, 6, () -> coef[ss], v -> coef[ss] = v,
                            v -> v == 0 ? "–" : (v > 0 ? "+" : "") + v));
                }
            }
            case CalibrationGames.BUILD -> {
                buildSpinners();
                addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15, Component.translatable("gui.theoreticalsciences.calibrate.clear"), accent, () -> {
                    java.util.Arrays.fill(side, 0);
                    buildSpinners();
                }));
            }
            case CalibrationGames.CIPHER -> {
                int n = key.length;
                for (int i = 0; i < n; i++) {
                    final int ii = i;
                    spinners.add(new Spinner(x0 + W / 2 - n * 40 + i * 80 + 10, y0 + 150, 60, 0, 25, () -> key[ii], v -> key[ii] = Math.floorMod(v, 26),
                            v -> String.valueOf((char) ('A' + v)) + " (" + v + ")"));
                }
            }
            case CalibrationGames.VERTEX -> {
                for (int i = 0; i < poly.length; i++) {
                    final int ii = i;
                    spinners.add(new Spinner(x0 + 196, y0 + 52 + i * 19, 56, 0, 12, () -> poly[ii], v -> poly[ii] = v, String::valueOf));
                }
            }
            case CalibrationGames.SYMMETRY -> addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15,
                    Component.translatable("gui.theoreticalsciences.calibrate.clear"), accent, () -> java.util.Arrays.fill(cells, 0)));
            case CalibrationGames.OXIDATION -> {
                for (int i = 0; i < ox.length; i++) {
                    final int ii = i;
                    spinners.add(new Spinner(x0 + 220, y0 + 72 + i * 26, 70, -4, 8, () -> ox[ii], v -> ox[ii] = v, v -> (v > 0 ? "+" : "") + v));
                }
            }
            case CalibrationGames.SPECTRUM -> {
                spinners.add(new Spinner(x0 + 84, y0 + 164, 84, -30, 30, () -> spec[0], v -> spec[0] = v, v -> (v > 0 ? "+" : "") + v + " nm"));
                spinners.add(new Spinner(x0 + 236, y0 + 164, 84, 90, 110, () -> spec[1], v -> spec[1] = v, v -> v + "%"));
            }
            case CalibrationGames.FLOW -> addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15,
                    Component.translatable("gui.theoreticalsciences.calibrate.reset"), accent, () -> java.util.Arrays.fill(turns, 0)));
            case CalibrationGames.PATH, CalibrationGames.TREE -> addRenderableWidget(HoloButton.of(x0 + 72, fy, 58, 15,
                    Component.translatable("gui.theoreticalsciences.calibrate.clear"), accent, () -> {
                        for (int k = 0; k < links.length; k++) links[k] = graph.forced()[k] ? 1 : 0;
                    }));
            default -> {}
        }
    }

    private int slotX(int s, int n) {
        return x0 + W / 2 + (int) ((s - (n - 1) / 2.0) * (W - 40) / Math.max(1, n));
    }

    /** BUILD: the species placed in the equation, reactants first, as pool indices. */
    private List<Integer> placed() {
        List<Integer> out = new ArrayList<>();
        for (int k = 1; k <= 2; k++) for (int i = 0; i < side.length; i++) if (side[i] == k) out.add(i);
        return out;
    }

    /** BUILD: a coefficient spinner under each placed species (rebuilt whenever one is placed or taken out). */
    private void buildSpinners() {
        spinners.clear();
        List<Integer> pl = placed();
        for (int k = 0; k < pl.size(); k++) {
            final int i = pl.get(k);
            spinners.add(new Spinner(slotX(k, pl.size()) - 21, y0 + 96, 42, 1, ChemGames.MAX_COEF, () -> bcoef[i], v -> bcoef[i] = v, String::valueOf));
        }
    }

    private int poolX(int i) {
        return x0 + 12 + (i % 5) * 64;
    }

    private int poolY(int i) {
        return y0 + 150 + (i / 5) * 21;
    }

    private int[] answer() {
        return switch (kind) {
            case CalibrationGames.WAVE -> {
                int[] a = new int[guess.length * 3];
                for (int i = 0; i < guess.length; i++) System.arraycopy(guess[i], 0, a, i * 3, 3);
                yield a;
            }
            case CalibrationGames.SIEVE -> marked.clone();
            case CalibrationGames.CHAIN -> proof.stream().mapToInt(Integer::intValue).toArray();
            case CalibrationGames.BALANCE, CalibrationGames.EQUILIBRIUM -> coef.clone();
            case CalibrationGames.BUILD -> {
                int[] a = new int[side.length * 2];
                for (int i = 0; i < side.length; i++) {
                    a[i] = side[i];
                    a[side.length + i] = bcoef[i];
                }
                yield a;
            }
            case CalibrationGames.OXIDATION -> ox.clone();
            case CalibrationGames.VERTEX -> poly.clone();
            case CalibrationGames.CIPHER -> key.clone();
            case CalibrationGames.SYMMETRY -> cells.clone();
            case CalibrationGames.SPECTRUM -> spec.clone();
            case CalibrationGames.FLOW -> {
                int[] a = new int[turns.length];
                for (int i = 0; i < a.length; i++) a[i] = Math.floorMod(turns[i], 4);
                yield a;
            }
            case CalibrationGames.PATH, CalibrationGames.TREE -> links.clone();
            case CalibrationGames.NASH -> new int[]{pick};
            default -> new int[0];
        };
    }

    private void submit() {
        int[] a = answer();
        if (!CalibrationGames.check(kind, seed, tier, a)) {
            note = hints ? Component.translatable("gui.theoreticalsciences.calibrate.hint." + key(), CalibrationGames.errors(kind, seed, tier, a))
                    : Component.translatable("gui.theoreticalsciences.calibrate.not_yet");
            noteColour = BAD;
            noteAt = System.currentTimeMillis();
            click(0.6F);
            return;
        }
        List<Integer> l = new ArrayList<>();
        for (int v : a) l.add(v);
        PacketDistributor.sendToServer(new TuningForkItem.Answer(pos, seed, l));
        onClose();
    }

    private static void click(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.35F));
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (Spinner s : spinners) {
            if (!s.over(mx, my)) continue;
            int d = mx < s.x() + 14 ? -1 : mx >= s.x() + s.w() - 14 ? 1 : 0;
            if (d != 0) {
                bump(s, d);
                return true;
            }
        }
        switch (kind) {
            case CalibrationGames.SIEVE -> {
                for (int i = 0; i < CalibrationGames.SIEVE_N; i++) {
                    int cx = sieveX(i), cy = sieveY(i);
                    if (mx >= cx && mx < cx + 36 && my >= cy && my < cy + 18) {
                        marked[i] ^= 1;
                        click(marked[i] != 0 ? 1.4F : 1.0F);
                        return true;
                    }
                }
            }
            case CalibrationGames.CHAIN -> {
                for (int i = 0; i < chain.tiles().size(); i++) {
                    int cx = tileX(i), cy = tileY(i);
                    if (mx >= cx && mx < cx + 76 && my >= cy && my < cy + 18 && !proof.contains(i)) {
                        proof.add(i);
                        click(1.2F);
                        return true;
                    }
                }
            }
            case CalibrationGames.SYMMETRY -> {
                int[] gr = symGrid();
                for (int i = 0; i < cells.length; i++) {
                    int cx = gr[0] + (i % sym.n()) * gr[2], cy = gr[1] + (i / sym.n()) * gr[2];
                    if (mx >= cx && mx < cx + gr[2] && my >= cy && my < cy + gr[2]) {
                        if (sym.on()[i] || sym.off()[i]) {
                            click(0.5F);
                            return true;
                        }
                        cells[i] ^= 1;
                        click(cells[i] != 0 ? 1.3F : 1.0F);
                        return true;
                    }
                }
            }
            case CalibrationGames.BUILD -> {
                for (int i = 0; i < side.length; i++) {
                    int cx = poolX(i), cy = poolY(i);
                    if (mx >= cx && mx < cx + 60 && my >= cy && my < cy + 18) {
                        side[i] = Math.floorMod(side[i] + (button == 1 ? -1 : 1), 3);
                        buildSpinners();
                        click(1.0F + 0.15F * side[i]);
                        return true;
                    }
                }
            }
            case CalibrationGames.FLOW -> {
                int[] g = flowGrid();
                for (int i = 0; i < turns.length; i++) {
                    int cx = g[0] + (i % flow.w()) * g[2], cy = g[1] + (i / flow.w()) * g[2];
                    if (mx >= cx && mx < cx + g[2] && my >= cy && my < cy + g[2]) {
                        if (flow.fixed()[i] || flow.tiles()[i] == 0) {
                            click(0.5F);
                            return true;
                        }
                        turns[i] += button == 1 ? -1 : 1;
                        click(1.1F + 0.05F * Math.floorMod(turns[i], 4));
                        return true;
                    }
                }
            }
            case CalibrationGames.NASH -> {
                int[] b = nashBox();
                for (int i = 0; i < game.k(); i++)
                    for (int j = 0; j < game.k(); j++) {
                        int cx = b[0] + j * b[2], cy = b[1] + i * b[3];
                        if (mx >= cx && mx < cx + b[2] && my >= cy && my < cy + b[3]) {
                            pick = i * game.k() + j;
                            click(1.2F);
                            return true;
                        }
                    }
            }
            case CalibrationGames.PATH, CalibrationGames.TREE -> {
                int k = linkAt(mx, my);
                if (k >= 0) {
                    if (graph.closed()[k] || graph.forced()[k]) {
                        click(0.5F);
                        return true;
                    }
                    links[k] ^= 1;
                    click(links[k] != 0 ? 1.3F : 1.0F);
                    return true;
                }
            }
            default -> {}
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (Spinner s : spinners)
            if (s.over(mx, my) && sy != 0) {
                bump(s, (sy > 0 ? 1 : -1) * (hasShiftDown() ? 5 : 1));
                return true;
            }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void bump(Spinner s, int d) {
        int v = s.get().getAsInt() + d;
        if (s.lo() == 0 && s.hi() == 7) v = Math.floorMod(v, 8);            // a phase goes round
        else v = Mth.clamp(v, s.lo(), s.hi());
        s.set().accept(v);
        click(d > 0 ? 1.25F : 0.95F);
    }

    // ------------------------------------------------------------------ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        float time = (System.currentTimeMillis() - opened) / 50F;
        HoloStyle.panel(g, x0, y0, W, H, C, accent, HoloStyle.CLASSIC, time, y0 + 40, y0 + H - 30, 0x10);
        HoloStyle.frame(g, x0, y0, W, H, C, accent, HoloStyle.CLASSIC, time);
        // header: the game, and the machine it calibrates
        var mc = Minecraft.getInstance();
        Component machine = mc.level == null ? Component.empty() : mc.level.getBlockState(pos).getBlock().getName();
        g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate." + key()), x0 + 10, y0 + 8, bright, false);
        Component sub = Component.translatable("gui.theoreticalsciences.calibrate.of", machine, tier);
        g.drawString(font, sub, x0 + W - 10 - font.width(sub), y0 + 8, DIM, false);
        g.fill(x0 + 8, y0 + 19, x0 + W - 8, y0 + 20, HoloStyle.argb(0x60, accent));
        int lines = font.split(Component.translatable("gui.theoreticalsciences.calibrate." + key() + ".how"), W - 20).size();
        g.drawWordWrap(font, Component.translatable("gui.theoreticalsciences.calibrate." + key() + ".how"), x0 + 10, y0 + 24, W - 20, DIM);
        int top = y0 + 26 + lines * 9;
        switch (kind) {
            case CalibrationGames.WAVE -> renderWave(g, top, time);
            case CalibrationGames.SIEVE -> renderSieve(g, mx, my);
            case CalibrationGames.CHAIN -> renderChain(g, mx, my);
            case CalibrationGames.BALANCE -> renderBalance(g);
            case CalibrationGames.EQUILIBRIUM -> renderEquilibrium(g);
            case CalibrationGames.BUILD -> renderBuild(g, mx, my);
            case CalibrationGames.OXIDATION -> renderOxidation(g);
            case CalibrationGames.VERTEX -> renderVertex(g);
            case CalibrationGames.CIPHER -> renderCipher(g);
            case CalibrationGames.SYMMETRY -> renderSymmetry(g, mx, my);
            case CalibrationGames.SPECTRUM -> renderSpectrum(g, mx, my);
            case CalibrationGames.FLOW -> renderFlow(g, mx, my, time);
            case CalibrationGames.PATH, CalibrationGames.TREE -> renderGraph(g, mx, my, time);
            case CalibrationGames.NASH -> renderNash(g, mx, my);
            default -> {}
        }
        for (Spinner s : spinners) spinner(g, s, mx, my);
        if (!note.getString().isEmpty()) {
            float age = (System.currentTimeMillis() - noteAt) / 1000F;
            int col = age < 0.4F ? (((int) (age * 20)) % 2 == 0 ? 0xFFFFFFFF : noteColour) : noteColour;
            g.drawCenteredString(font, note, x0 + W / 2 + 26, y0 + H - 18, col);
        }
        super.render(g, mx, my, pt);
    }

    private void spinner(GuiGraphics g, Spinner s, int mx, int my) {
        boolean over = s.over(mx, my);
        HoloStyle.chamferFill(g, s.x(), s.y(), s.w(), 16, 3, over ? 0xE0182430 : 0xE0101820);
        HoloStyle.chamferFrame(g, s.x(), s.y(), s.w(), 16, 3, HoloStyle.argb(over ? 0xE0 : 0x90, accent));
        boolean l = over && mx < s.x() + 14, r = over && mx >= s.x() + s.w() - 14;
        arrow(g, s.x() + 5, s.y() + 4, -1, l ? 0xFFFFFFFF : bright);
        arrow(g, s.x() + s.w() - 9, s.y() + 4, 1, r ? 0xFFFFFFFF : bright);
        g.drawCenteredString(font, s.fmt().apply(s.get().getAsInt()), s.x() + s.w() / 2, s.y() + 4, TEXT);
    }

    /** A small solid triangle pointing left (-1) or right (1). */
    private static void arrow(GuiGraphics g, int x, int y, int dir, int col) {
        for (int i = 0; i < 4; i++) {                        // column i: the tip is one end, the base the other
            int xx = dir < 0 ? x + i : x + 3 - i;
            g.fill(xx, y + 3 - i, xx + 1, y + 5 + i, col);
        }
    }

    private void label(GuiGraphics g, Component c, int cx, int y) {
        g.drawCenteredString(font, c, cx, y, DIM);
    }

    // WAVE ---------------------------------------------------------------
    private void renderWave(GuiGraphics g, int top, float time) {
        int px = x0 + 12, py = top + 2, pw = W - 24, ph = Math.max(48, y0 + 112 - py);
        g.fill(px, py, px + pw, py + ph, 0xE0060A10);
        for (int k = 1; k < 8; k++) g.fill(px + k * pw / 8, py, px + k * pw / 8 + 1, py + ph, 0xFF141E28);
        for (int k = 1; k < 4; k++) g.fill(px, py + k * ph / 4, px + pw, py + k * ph / 4 + 1, k == 2 ? 0xFF22303E : 0xFF141E28);
        HoloStyle.chamferFrame(g, px - 1, py - 1, pw + 2, ph + 2, 2, HoloStyle.argb(0x80, accent));
        double amax = 0;
        for (int[] c : target) amax += c[2];
        double scale = (ph / 2.0 - 4) / Math.max(3, amax);
        boolean match = CalibrationGames.check(CalibrationGames.WAVE, seed, tier, answer());
        trace(g, target, px, py, pw, ph, scale, HoloStyle.argb(0x50, GOLD), 1);
        trace(g, target, px, py, pw, ph, scale, GOLD, 0);
        trace(g, guess, px, py, pw, ph, scale, match ? GOOD : bright, 0);
        g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.wave.target"), px + 4, py + 3, GOLD, false);
        Component yours = Component.translatable("gui.theoreticalsciences.calibrate.wave.yours");
        g.drawString(font, yours, px + pw - 4 - font.width(yours), py + 3, bright, false);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.wave.f"), x0 + 108, y0 + 121);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.wave.phase"), x0 + 196, y0 + 121);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.wave.amp"), x0 + 284, y0 + 121);
        for (int c = 0; c < guess.length; c++)
            g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.wave.n", c + 1), x0 + 14, y0 + 136 + c * 22, DIM, false);
    }

    private static void trace(GuiGraphics g, int[][] w, int px, int py, int pw, int ph, double scale, int col, int thick) {
        int prev = 0;
        for (int i = 0; i < pw; i++) {
            double x = i * Math.PI * 2 / pw;
            int y = py + ph / 2 - (int) Math.round(CalibrationGames.waveAt(w, x) * scale);
            y = Mth.clamp(y, py + 1, py + ph - 2);
            if (i > 0) g.fill(px + i, Math.min(prev, y) - thick, px + i + 1, Math.max(prev, y) + 1 + thick, col);
            prev = y;
        }
    }

    // SIEVE --------------------------------------------------------------
    private int sieveX(int i) {
        return x0 + 14 + (i % 8) * 39;
    }

    private int sieveY(int i) {
        return y0 + 64 + (i / 8) * 22;
    }

    private void renderSieve(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < CalibrationGames.SIEVE_N; i++) {
            int cx = sieveX(i), cy = sieveY(i);
            boolean m = marked[i] != 0, over = mx >= cx && mx < cx + 36 && my >= cy && my < cy + 18;
            HoloStyle.chamferFill(g, cx, cy, 36, 18, 3, m ? HoloStyle.argb(0xC0, accent & 0x3F3F3F) | 0xFF000000 : over ? 0xE01A2632 : 0xE0101820);
            HoloStyle.chamferFrame(g, cx, cy, 36, 18, 3, m ? bright : HoloStyle.argb(over ? 0xC0 : 0x50, accent));
            g.drawCenteredString(font, String.valueOf(start + i), cx + 18, cy + 5, m ? 0xFFFFFFFF : TEXT);
        }
        int n = 0;
        for (int v : marked) n += v;
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.sieve.count", n), x0 + W / 2, y0 + 178);
    }

    // CHAIN --------------------------------------------------------------
    private int tileX(int i) {
        return x0 + 12 + (i % 4) * 80;
    }

    private int tileY(int i) {
        return y0 + 76 + (i / 4) * 21;
    }

    private static String sym(int i) {
        return String.valueOf((char) ('A' + i));
    }

    private void renderChain(GuiGraphics g, int mx, int my) {
        Component goal = Component.translatable("gui.theoreticalsciences.calibrate.chain.goal", sym(chain.premise()), sym(chain.goal()));
        g.drawCenteredString(font, goal, x0 + W / 2, y0 + 62, GOLD);
        int at = chain.premise();
        for (int i : proof) at = chain.tiles().get(i)[1];
        for (int i = 0; i < chain.tiles().size(); i++) {
            int[] t = chain.tiles().get(i);
            int cx = tileX(i), cy = tileY(i);
            boolean used = proof.contains(i), over = !used && mx >= cx && mx < cx + 76 && my >= cy && my < cy + 18;
            boolean fits = !used && t[0] == at;
            HoloStyle.chamferFill(g, cx, cy, 76, 18, 4, used ? 0x90080C10 : over ? 0xE01C2834 : 0xE0101820);
            HoloStyle.chamferFrame(g, cx, cy, 76, 18, 4, used ? 0xFF26303A : HoloStyle.argb(over ? 0xF0 : fits ? 0xA0 : 0x50, accent));
            g.drawCenteredString(font, sym(t[0]) + "  ⇒  " + sym(t[1]), cx + 38, cy + 5, used ? FAINT : TEXT);
        }
        int y = tileY(chain.tiles().size() - 1) + 26;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.chain.proof"), x0 + 12, y, DIM, false);
        int x = x0 + 12 + font.width(Component.translatable("gui.theoreticalsciences.calibrate.chain.proof")) + 6;
        x = chip(g, sym(chain.premise()), x, y - 3, GOLD);
        for (int i : proof) {
            g.drawString(font, "⇒", x + 1, y, DIM, false);
            int to = chain.tiles().get(i)[1];
            x = chip(g, sym(to), x + 12, y - 3, to == chain.goal() ? GOOD : bright);
        }
    }

    private int chip(GuiGraphics g, String s, int x, int y, int col) {
        int w = font.width(s) + 8;
        HoloStyle.chamferFill(g, x, y, w, 14, 2, 0xE0101820);
        HoloStyle.chamferFrame(g, x, y, w, 14, 2, col);
        g.drawString(font, s, x + 4, y + 3, col, false);
        return x + w + 2;
    }

    // CHEMISTRY ----------------------------------------------------------
    /** One species box (centred at cx); returns nothing. */
    private void species(GuiGraphics g, String text, int cx, int y, int frame) {
        int fw = font.width(text) + 12;
        HoloStyle.chamferFill(g, cx - fw / 2, y, fw, 18, 4, 0xE0101820);
        HoloStyle.chamferFrame(g, cx - fw / 2, y, fw, 18, 4, frame);
        g.drawCenteredString(font, text, cx, y + 5, TEXT);
    }

    /** The equation's species in boxes, with + and the arrow between them. */
    private void equation(GuiGraphics g, List<ChemGames.Species> sp, boolean eqm, boolean phases) {
        int n = sp.size();
        for (int s = 0; s < n; s++) {
            int cx = slotX(s, n);
            ChemGames.Species x = sp.get(s);
            species(g, ChemGames.pretty(x.formula()) + (phases && !x.phase().isEmpty() ? "(" + x.phase() + ")" : ""), cx, y0 + 70,
                    HoloStyle.argb(0xA0, accent));
            if (s < n - 1) {
                boolean arrow = !x.product() && sp.get(s + 1).product();
                g.drawCenteredString(font, arrow ? (eqm ? "\u21CC" : "\u27F6") : "+", (cx + slotX(s + 1, n)) / 2, y0 + 75, GOLD);
            }
        }
    }

    /** Atoms of each element (and the charge) on each side, as chips: green when they match. */
    private void tally(GuiGraphics g, List<ChemGames.Species> sp, int[] c, int y) {
        Map<String, int[]> count = new java.util.LinkedHashMap<>();
        for (int s = 0; s < sp.size(); s++) {
            int k = sp.get(s).product() ? 1 : 0;
            for (var e : ChemGames.atoms(sp.get(s).formula()).entrySet())
                count.computeIfAbsent(e.getKey(), q -> new int[2])[k] += c[s] * e.getValue();
        }
        boolean ionic = false;
        for (ChemGames.Species x : sp) ionic |= x.charge() != 0;
        if (ionic) {
            int[] q = new int[2];
            for (int s = 0; s < sp.size(); s++) q[sp.get(s).product() ? 1 : 0] += c[s] * sp.get(s).charge();
            count.put("\u00B1", q);
        }
        label(g, Component.translatable(ionic ? "gui.theoreticalsciences.calibrate.balance.atoms_charge" : "gui.theoreticalsciences.calibrate.balance.atoms"), x0 + W / 2, y);
        int i = 0, per = Math.min(5, count.size()), cw = 62;
        int bx0 = x0 + W / 2 - per * cw / 2;
        for (var e : count.entrySet()) {
            int bx = bx0 + (i % 5) * cw, by = y + 12 + (i / 5) * 19;
            boolean ok = e.getValue()[0] == e.getValue()[1];
            HoloStyle.chamferFill(g, bx, by, cw - 4, 16, 3, ok ? 0xC0102818 : 0xC0281410);
            HoloStyle.chamferFrame(g, bx, by, cw - 4, 16, 3, ok ? GOOD : BAD);
            String name = e.getKey().equals("\u00B1") ? Component.translatable("gui.theoreticalsciences.calibrate.balance.charge").getString() : e.getKey();
            g.drawCenteredString(font, name + " " + e.getValue()[0] + "|" + e.getValue()[1], bx + (cw - 4) / 2, by + 4, ok ? GOOD : BAD);
            i++;
        }
    }

    private void renderBalance(GuiGraphics g) {
        equation(g, rx.species(), rx.equilibrium(), false);
        tally(g, rx.species(), coef, y0 + 124);
    }

    private void renderEquilibrium(GuiGraphics g) {
        List<ChemGames.Species> sp = rx.species();
        // the coefficients are given here: the game is the powers in K
        int n = sp.size();
        for (int s = 0; s < n; s++) {
            int cx = slotX(s, n);
            ChemGames.Species x = sp.get(s);
            species(g, (x.coef() > 1 ? x.coef() + " " : "") + ChemGames.pretty(x.formula()) + "(" + x.phase() + ")", cx, y0 + 70, HoloStyle.argb(0xA0, accent));
            if (s < n - 1) g.drawCenteredString(font, !x.product() && sp.get(s + 1).product() ? "\u21CC" : "+", (cx + slotX(s + 1, n)) / 2, y0 + 75, GOLD);
        }
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.equilibrium.power"), x0 + W / 2, y0 + 116);
        // K as it stands: numerator over denominator
        StringBuilder num = new StringBuilder(), den = new StringBuilder();
        for (int s = 0; s < n; s++) {
            int p = coef[s];
            if (p == 0) continue;
            String t = "[" + ChemGames.pretty(sp.get(s).formula()) + "]" + (Math.abs(p) > 1 ? ChemGames.pretty("^" + Math.abs(p)).replace("\u207A", "") : "");
            (p > 0 ? num : den).append(t);
        }
        String ns = num.length() == 0 ? "1" : num.toString(), ds = den.toString();
        int cx = x0 + W / 2 + 10, y = y0 + 140;
        g.drawString(font, "K =", cx - Math.max(font.width(ns), font.width(ds)) / 2 - 26, ds.isEmpty() ? y + 6 : y + 6, bright, false);
        if (ds.isEmpty()) g.drawCenteredString(font, ns, cx, y + 6, TEXT);
        else {
            int lw = Math.max(font.width(ns), font.width(ds)) + 6;
            g.drawCenteredString(font, ns, cx, y, TEXT);
            g.fill(cx - lw / 2, y + 10, cx + lw / 2, y + 11, TEXT);
            g.drawCenteredString(font, ds, cx, y + 13, TEXT);
        }
    }

    private void renderBuild(GuiGraphics g, int mx, int my) {
        ChemGames.Reaction r = build.reaction();
        Component prompt = r.mixed() != null
                ? Component.translatable("gui.theoreticalsciences.calibrate.build.mix", ChemGames.pretty(r.mixed()[0]), ChemGames.pretty(r.mixed()[1]))
                : Component.translatable("gui.theoreticalsciences.calibrate.build.make", Component.translatable("gui.theoreticalsciences.calibrate.rxn." + r.id()));
        g.drawCenteredString(font, prompt, x0 + W / 2, y0 + 56, GOLD);
        List<Integer> pl = placed();
        if (pl.isEmpty()) label(g, Component.translatable("gui.theoreticalsciences.calibrate.build.empty"), x0 + W / 2, y0 + 75);
        List<ChemGames.Species> sp = new ArrayList<>();
        int[] c = new int[pl.size()];
        for (int k = 0; k < pl.size(); k++) {
            int i = pl.get(k);
            sp.add(new ChemGames.Species(build.pool().get(i), "", bcoef[i], side[i] == 2));
            c[k] = bcoef[i];
        }
        if (!pl.isEmpty()) {
            boolean hasReactant = side[pl.get(0)] == 1, hasProduct = side[pl.get(pl.size() - 1)] == 2;
            equation(g, sp, r.equilibrium(), false);
            if (!hasReactant || !hasProduct)
                g.drawCenteredString(font, "\u27F6", hasReactant ? x0 + W - 18 : x0 + 18, y0 + 75, GOLD);
            tally(g, sp, c, y0 + 116);
        }
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.build.pool"), x0 + W / 2, y0 + 140);
        for (int i = 0; i < side.length; i++) {
            int cx = poolX(i), cy = poolY(i);
            boolean over = mx >= cx && mx < cx + 60 && my >= cy && my < cy + 18;
            int col = side[i] == 1 ? 0xFF60C0FF : side[i] == 2 ? 0xFFFFA060 : HoloStyle.argb(over ? 0xD0 : 0x60, accent);
            HoloStyle.chamferFill(g, cx, cy, 60, 18, 4, over ? 0xE01C2834 : 0xE0101820);
            HoloStyle.chamferFrame(g, cx, cy, 60, 18, 4, col);
            g.drawCenteredString(font, ChemGames.pretty(build.pool().get(i)), cx + 30, cy + 5, side[i] == 0 ? TEXT : col);
        }
    }

    private void renderOxidation(GuiGraphics g) {
        for (int i = 0; i < oxq.length; i++) {
            String[] q = ChemGames.OXIDATION[oxq[i]];
            int y = y0 + 72 + i * 26;
            species(g, ChemGames.pretty(q[0]), x0 + 70, y - 1, HoloStyle.argb(0xA0, accent));
            g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.oxidation.of", q[1]), x0 + 124, y + 4, GOLD, false);
        }
    }

    // VERTEX -------------------------------------------------------------
    private void renderVertex(GuiGraphics g) {
        int[] sides = CalibrationGames.SIDES;
        String[] names = {"triangle", "square", "pentagon", "hexagon", "octagon", "decagon", "dodecagon"};
        int sum = 0, n = 0;
        for (int i = 0; i < sides.length; i++) {
            int y = y0 + 52 + i * 19;
            boolean req = i == vertex.required();
            g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.vertex." + names[i]), x0 + 100, y + 4, req ? GOLD : TEXT, false);
            small(g, CalibrationGames.angle(sides[i]) + "\u00B0", x0 + 268, y + 6, DIM);
            sum += poly[i] * CalibrationGames.angle(sides[i]);
            n += poly[i];
        }
        // the vertex itself: each polygon's corner as a wedge round the point, in turn
        int cx = x0 + 52, cy = y0 + 104, rad = 40;
        int[] cols = {0xFFFF7060, 0xFF60A0FF, 0xFFFFD060, 0xFF70FF9A, 0xFFC080FF, 0xFFFF9AD0, 0xFF60E0E0};
        // each polygon's corner as a wedge round the point, in turn (one pass over the disc)
        List<double[]> wedges = new ArrayList<>();                 // {start°, end°, colour}
        double at = 0;
        for (int i = 0; i < sides.length; i++)
            for (int k = 0; k < poly[i]; k++) {
                wedges.add(new double[]{at, at + CalibrationGames.angle(sides[i]), cols[i]});
                at += CalibrationGames.angle(sides[i]);
            }
        for (int y = -rad; y <= rad; y++)
            for (int x = -rad; x <= rad; x++) {
                double d = Math.hypot(x, y);
                if (d > rad) continue;
                double deg = Math.toDegrees(Math.atan2(x, -y));          // clockwise from the top
                if (deg < 0) deg += 360;
                int c = 0xFF0E141A;
                for (double[] wd : wedges)
                    if (deg >= wd[0] && deg < wd[1]) {
                        c = (deg - wd[0]) * Math.PI / 180 * d < 1.0 ? 0xFF0A0E14 : (int) (long) wd[2];
                        break;
                    }
                if (at > 360 && deg < at - 360) c = 0xFFFF3030;          // overlap: past a full turn
                g.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, c);
            }
        HoloStyle.outline(g, cx - rad - 2, cy - rad - 2, 2 * rad + 4, 2 * rad + 4, HoloStyle.argb(0x40, accent));
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        int col = sum == 360 ? GOOD : sum > 360 ? BAD : GOLD;
        g.drawCenteredString(font, sum + "\u00B0 / 360\u00B0", cx, cy + rad + 8, col);
        Component rule = vertex.count() > 0
                ? Component.translatable("gui.theoreticalsciences.calibrate.vertex.count", vertex.count(), n)
                : Component.translatable("gui.theoreticalsciences.calibrate.vertex.any", n);
        g.drawCenteredString(font, rule, cx, cy + rad + 20, vertex.count() > 0 && n != vertex.count() ? GOLD : DIM);
    }

    // CIPHER -------------------------------------------------------------
    private void renderCipher(GuiGraphics g) {
        String enc = cipher.encrypted(), dec = CalibrationGames.shift(enc, key, -1);
        int n = enc.length(), cw = Math.min(22, (W - 40) / n), x = x0 + (W - n * cw) / 2;
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.cipher.intercepted"), x0 + W / 2, y0 + 58);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.cipher.decoded"), x0 + W / 2, y0 + 98);
        int[] cols = {0xFF60C0FF, 0xFFFFA060, 0xFFC080FF};
        for (int i = 0; i < n; i++) {
            int cx = x + i * cw, kc = key.length > 1 ? cols[i % key.length] : 0xFF000000 | HoloStyle.bright(accent);
            for (int row = 0; row < 2; row++) {
                int y = y0 + (row == 0 ? 70 : 110);
                HoloStyle.chamferFill(g, cx + 1, y, cw - 2, 18, 3, 0xE0101820);
                HoloStyle.chamferFrame(g, cx + 1, y, cw - 2, 18, 3, row == 0 ? HoloStyle.argb(0x70, accent) : kc);
                g.drawCenteredString(font, String.valueOf((row == 0 ? enc : dec).charAt(i)), cx + cw / 2, y + 5, row == 0 ? DIM : TEXT);
            }
        }
        if (key.length > 1) label(g, Component.translatable("gui.theoreticalsciences.calibrate.cipher.key", key.length), x0 + W / 2, y0 + 138);
        else label(g, Component.translatable("gui.theoreticalsciences.calibrate.cipher.shift"), x0 + W / 2, y0 + 138);
    }

    // SYMMETRY -----------------------------------------------------------
    private int[] symGrid() {
        int cs = Math.min(18, 120 / sym.n());
        return new int[]{x0 + 30, y0 + 58, cs};
    }

    private void renderSymmetry(GuiGraphics g, int mx, int my) {
        int[] gr = symGrid();
        int gx = gr[0], gy = gr[1], cs = gr[2], n = sym.n();
        int[] a = answer();
        for (int i = 0; i < cells.length; i++) {
            int cx = gx + (i % n) * cs, cy = gy + (i / n) * cs;
            boolean over = mx >= cx && mx < cx + cs && my >= cy && my < cy + cs;
            boolean on = sym.on()[i], off = sym.off()[i], f = on || cells[i] != 0;
            boolean ok = true;
            for (int j : CalibrationGames.orbit(n, sym.group(), i)) ok &= (sym.on()[j] || a[j] != 0) == f;
            int fill = on ? 0xFFFFC860 : off ? 0xFF14100C : f ? (ok ? 0xFF000000 | HoloStyle.bright(accent) : 0xFFFF7060) : over ? 0xFF1C2834 : 0xFF0E141A;
            g.fill(cx + 1, cy + 1, cx + cs - 1, cy + cs - 1, fill);
            if (off) {
                for (int k = 3; k < cs - 3; k++) {
                    g.fill(cx + k, cy + k, cx + k + 1, cy + k + 1, 0xFF6A5040);
                    g.fill(cx + cs - 1 - k, cy + k, cx + cs - k, cy + k + 1, 0xFF6A5040);
                }
            }
        }
        // the axes of the symmetry asked for
        int w = n * cs, mid = gx + w / 2;
        int ax = HoloStyle.argb(0x90, GOLD);
        if (sym.group() != 2) g.fill(mid, gy - 4, mid + 1, gy + w + 4, ax);
        if (sym.group() == 1 || sym.group() == 3) g.fill(gx - 4, gy + w / 2, gx + w + 4, gy + w / 2 + 1, ax);
        if (sym.group() >= 2) HoloStyle.outline(g, mid - 4, gy + w / 2 - 4, 9, 9, ax);
        String[] gname = {"mirror", "mirrors", "turn", "turn_mirror"};
        g.drawWordWrap(font, Component.translatable("gui.theoreticalsciences.calibrate.symmetry." + gname[sym.group()]), gx + w + 18, gy + 4, x0 + W - (gx + w + 26), GOLD);
        g.drawWordWrap(font, Component.translatable("gui.theoreticalsciences.calibrate.symmetry.key"), gx + w + 18, gy + 60, x0 + W - (gx + w + 26), DIM);
    }

    // SPECTRUM -----------------------------------------------------------
    private int nmX(double nm) {
        int px = x0 + 14, pw = W - 28;
        return px + (int) Math.round((nm - CalibrationGames.SPEC_LO) / (CalibrationGames.SPEC_HI - CalibrationGames.SPEC_LO) * pw);
    }

    /** A ruler under a strip: a tick every 5 nm, longer every 10, numbered every 50. */
    private void ruler(GuiGraphics g, int y, boolean numbers) {
        for (int nm = CalibrationGames.SPEC_LO; nm <= CalibrationGames.SPEC_HI; nm += 5) {
            int x = nmX(nm), len = nm % 50 == 0 ? 5 : nm % 10 == 0 ? 3 : 1;
            g.fill(x, y, x + 1, y + len, nm % 50 == 0 ? DIM : FAINT);
            if (numbers && nm % 50 == 0) {
                g.pose().pushPose();
                g.pose().translate(x, y + 6, 0);
                g.pose().scale(0.6F, 0.6F, 1);
                g.drawCenteredString(font, String.valueOf(nm), 0, 0, DIM);
                g.pose().popPose();
            }
        }
    }

    private void small(GuiGraphics g, String t, int x, int y, int col) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawCenteredString(font, t, 0, 0, col);
        g.pose().popPose();
    }

    private void smallLeft(GuiGraphics g, String t, int x, int y, int col) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.6F, 0.6F, 1);
        g.drawString(font, t, 0, 0, col, false);
        g.pose().popPose();
    }

    private void renderSpectrum(GuiGraphics g, int mx, int my) {
        int px = x0 + 14, pw = W - 28;
        int ry = y0 + 62, oy = y0 + 106;
        smallLeft(g, Component.translatable("gui.theoreticalsciences.calibrate.spectrum.reference").getString(), px, ry - 6, DIM);
        smallLeft(g, Component.translatable("gui.theoreticalsciences.calibrate.spectrum.yours").getString(), px, oy - 12, DIM);
        for (int y : new int[]{ry, oy}) {
            g.fill(px, y, px + pw, y + 20, 0xF0040608);
            HoloStyle.chamferFrame(g, px - 1, y - 1, pw + 2, 22, 2, HoloStyle.argb(0x80, accent));
        }
        ruler(g, ry + 21, false);
        ruler(g, oy + 21, true);
        int[] lines = spectrum.lines();
        double miss = 0;
        for (int i = 0; i < lines.length; i++) {
            int x = nmX(lines[i]);
            int col = 0xFF000000 | nmColour(lines[i]);
            g.fill(x - 1, ry + 1, x + 1, ry + 19, col);
            // where it should land on the lower strip: a notch on its ruler, and a guide down from the reference
            g.fill(x, ry + 33, x + 1, oy - 1, 0x30FFFFFF);
            g.fill(x - 1, oy + 21, x + 2, oy + 27, col);
            double c = spectrum.corrected(i, spec[0], spec[1]);
            miss += Math.abs(c - lines[i]);
            int xo = nmX(c);
            if (xo >= px && xo < px + pw) {
                g.fill(xo - 1, oy + 1, xo + 1, oy + 19, 0xFF000000 | nmColour((int) Math.round(c)));
                small(g, String.format("%.0f", c), xo, oy - 6, Math.abs(c - lines[i]) < 0.5 ? GOOD : DIM);
            }
            small(g, String.valueOf(lines[i]), x, ry + 27, TEXT);
        }
        miss /= lines.length;
        boolean match = spec[0] == spectrum.offset() && spec[1] == spectrum.stretch();
        if (match) HoloStyle.chamferFrame(g, px - 2, oy - 2, pw + 4, 24, 2, GOOD);
        // a cursor under the mouse, reading the wavelength there across both strips
        if (mx >= px && mx < px + pw && my >= ry - 2 && my < oy + 30) {
            g.fill(mx, ry, mx + 1, oy + 20, 0x80FFFFFF);
            double nm = CalibrationGames.SPEC_LO + (mx - px) / (double) pw * (CalibrationGames.SPEC_HI - CalibrationGames.SPEC_LO);
            String t = String.format("%.0f nm", nm);
            int tw = font.width(t) + 6, tx = Mth.clamp(mx - tw / 2, px, px + pw - tw);
            g.fill(tx, oy + 34, tx + tw, oy + 45, 0xF0101820);
            g.drawString(font, t, tx + 3, oy + 36, TEXT, false);
        } else
            g.drawCenteredString(font, Component.translatable("gui.theoreticalsciences.calibrate.spectrum.miss", String.format("%.1f", miss)),
                    x0 + W / 2, oy + 36, match ? GOOD : DIM);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.spectrum.offset"), x0 + 126, y0 + 154);
        label(g, Component.translatable("gui.theoreticalsciences.calibrate.spectrum.stretch"), x0 + 278, y0 + 154);
    }

    /** Roughly what colour light of this wavelength (nm) looks. */
    private static int nmColour(int nm) {
        double r, gr, b;
        if (nm < 440) { r = (440 - nm) / 60.0; gr = 0; b = 1; }
        else if (nm < 490) { r = 0; gr = (nm - 440) / 50.0; b = 1; }
        else if (nm < 510) { r = 0; gr = 1; b = (510 - nm) / 20.0; }
        else if (nm < 580) { r = (nm - 510) / 70.0; gr = 1; b = 0; }
        else if (nm < 645) { r = 1; gr = (645 - nm) / 65.0; b = 0; }
        else { r = 1; gr = 0; b = 0; }
        double f = nm < 420 ? 0.4 + 0.6 * (nm - 380) / 40.0 : nm > 700 ? 0.4 + 0.6 * (760 - nm) / 60.0 : 1;
        f = Mth.clamp(f, 0.3, 1);
        return ((int) (Mth.clamp(r * f, 0, 1) * 255) << 16) | ((int) (Mth.clamp(gr * f, 0, 1) * 255) << 8) | (int) (Mth.clamp(b * f, 0, 1) * 255);
    }

    // FLOW ---------------------------------------------------------------
    /** {x, y, cell size} of the pipe grid. */
    private int[] flowGrid() {
        int cell = Math.min(26, Math.min((W - 60) / flow.w(), 112 / flow.h()));
        int gx = x0 + (W - cell * flow.w()) / 2, gy = y0 + 58 + (112 - cell * flow.h()) / 2;
        return new int[]{gx, gy, cell};
    }

    private void renderFlow(GuiGraphics g, int mx, int my, float time) {
        int[] gr = flowGrid();
        int gx = gr[0], gy = gr[1], cs = gr[2];
        int[] a = answer();
        int[] masks = CalibrationGames.masks(flow, a);
        boolean[] wet = CalibrationGames.flowWet(flow, a);
        boolean done = CalibrationGames.flowReach(flow, a) < 0;
        int wetCol = done ? GOOD : bright, dry = 0xFF4A5868;
        for (int i = 0; i < turns.length; i++) {
            int cx = gx + (i % flow.w()) * cs, cy = gy + (i / flow.w()) * cs;
            boolean over = mx >= cx && mx < cx + cs && my >= cy && my < cy + cs;
            int m = masks[i];
            if (flow.tiles()[i] == 0) {                       // rock: no pipe can go here
                g.fill(cx, cy, cx + cs, cy + cs, 0xF0242018);
                for (int k = 3; k < cs; k += 5) g.fill(cx + k, cy + (k * 7) % cs, cx + k + 2, cy + (k * 7) % cs + 2, 0xFF3A3428);
                HoloStyle.outline(g, cx, cy, cs, cs, 0xFF2E2A22);
                continue;
            }
            boolean locked = flow.fixed()[i];
            g.fill(cx, cy, cx + cs, cy + cs, locked ? 0xE0141418 : over ? 0xE0182430 : 0xE00C1218);
            HoloStyle.outline(g, cx, cy, cs, cs, HoloStyle.argb(0x40, accent));
            int col = wet[i] ? wetCol : dry, mid = cs / 2, t = Math.max(2, cs / 7);
            g.fill(cx + mid - t, cy + mid - t, cx + mid + t, cy + mid + t, col);
            if ((m & 1) != 0) g.fill(cx + mid - t, cy, cx + mid + t, cy + mid, col);
            if ((m & 4) != 0) g.fill(cx + mid - t, cy + mid, cx + mid + t, cy + cs, col);
            if ((m & 8) != 0) g.fill(cx, cy + mid - t, cx + mid, cy + mid + t, col);
            if ((m & 2) != 0) g.fill(cx + mid, cy + mid - t, cx + cs, cy + mid + t, col);
            if (locked) {                                      // bolts in the corners: this one cannot be turned
                for (int[] b : new int[][]{{2, 2}, {cs - 4, 2}, {2, cs - 4}, {cs - 4, cs - 4}})
                    g.fill(cx + b[0], cy + b[1], cx + b[0] + 2, cy + b[1] + 2, 0xFFB0B8C0);
            }
            if (flow.valve()[i]) {                              // a valve the flow has to pass through
                int vc = wet[i] ? GOOD : GOLD;
                HoloStyle.outline(g, cx + mid - t - 3, cy + mid - t - 3, 2 * t + 6, 2 * t + 6, vc);
                HoloStyle.outline(g, cx + mid - t - 2, cy + mid - t - 2, 2 * t + 4, 2 * t + 4, vc);
            }
        }
        // the inlet and the outlet
        int t = Math.max(2, cs / 7);
        int iy = gy + flow.in() * cs + cs / 2, oy = gy + flow.out() * cs + cs / 2;
        g.fill(gx - 14, iy - t, gx, iy + t, bright);
        Component in = Component.translatable("gui.theoreticalsciences.calibrate.flow.in");
        g.drawString(font, in, gx - 16 - font.width(in), iy - 4, DIM, false);
        g.fill(gx + flow.w() * cs, oy - t, gx + flow.w() * cs + 14, oy + t, done ? GOOD : dry);
        g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.flow.out"), gx + flow.w() * cs + 16, oy - 4, DIM, false);
        HoloStyle.chamferFrame(g, gx - 2, gy - 2, flow.w() * cs + 4, flow.h() * cs + 4, 2, HoloStyle.argb(0x90, accent));
        // the rules in play, under the grid
        List<Component> rules = new ArrayList<>();
        boolean anyValve = false, anyLock = false, anyRock = false;
        for (int i = 0; i < turns.length; i++) {
            anyValve |= flow.valve()[i];
            anyLock |= flow.fixed()[i];
            anyRock |= flow.tiles()[i] == 0;
        }
        if (anyValve) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.flow.valves"));
        if (anyLock) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.flow.locked"));
        if (flow.sealed()) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.flow.sealed",
                CalibrationGames.leaks(flow, a)));
        int ry = gy + flow.h() * cs + 6;
        for (int k = 0; k < rules.size(); k++) small(g, rules.get(k).getString(), x0 + W / 2, ry + k * 7, DIM);
    }

    // PATH, TREE ---------------------------------------------------------
    /** {x, y, w, h} of the map. */
    private int[] graphBox() {
        return new int[]{x0 + 22, y0 + 64, 206, 118};
    }

    private int nodeX(int i) {
        int[] b = graphBox();
        return b[0] + (int) Math.round(graph.x()[i] * b[2]);
    }

    private int nodeY(int i) {
        int[] b = graphBox();
        return b[1] + (int) Math.round(graph.y()[i] * b[3]);
    }

    /** The link under the mouse (within 4 pixels of it), or −1. */
    private int linkAt(double mx, double my) {
        int best = -1;
        double bd = 4.5;
        for (int k = 0; k < graph.edges().length; k++) {
            int[] e = graph.edges()[k];
            double ax = nodeX(e[0]), ay = nodeY(e[0]), bx = nodeX(e[1]), by = nodeY(e[1]);
            double dx = bx - ax, dy = by - ay, l2 = dx * dx + dy * dy;
            double t = l2 == 0 ? 0 : Mth.clamp(((mx - ax) * dx + (my - ay) * dy) / l2, 0.08, 0.92);
            double d = Math.hypot(mx - (ax + t * dx), my - (ay + t * dy));
            if (d < bd) {
                bd = d;
                best = k;
            }
        }
        return best;
    }

    /** A straight line of width w drawn as one rotated bar (clean at any angle); dashed: in 4-pixel dashes. */
    private void line(GuiGraphics g, int ax, int ay, int bx, int by, int w, int col, boolean dashed) {
        float dx = bx - ax, dy = by - ay, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 0.5F) return;
        g.pose().pushPose();
        g.pose().translate(ax + 0.5F, ay + 0.5F, 0);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.atan2(dy, dx)));
        float h = w / 2F;
        if (!dashed) fillF(g, 0, -h, len, h, col);
        else for (float t = 0; t < len; t += 8) fillF(g, t, -h, Math.min(len, t + 4), h, col);
        g.pose().popPose();
    }

    /** fill() with fractional corners (in the current pose). */
    private static void fillF(GuiGraphics g, float x0, float y0, float x1, float y1, int col) {
        var m = g.pose().last().pose();
        var vc = g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        vc.addVertex(m, x0, y0, 0).setColor(col);
        vc.addVertex(m, x0, y1, 0).setColor(col);
        vc.addVertex(m, x1, y1, 0).setColor(col);
        vc.addVertex(m, x1, y0, 0).setColor(col);
        g.flush();
    }

    private void renderGraph(GuiGraphics g, int mx, int my, float time) {
        boolean tree = kind == CalibrationGames.TREE;
        int[] a = answer();
        boolean shape = tree ? CalibrationGames.isTree(graph, a) : CalibrationGames.isRoute(graph, a);
        boolean done = CalibrationGames.check(kind, seed, tier, a);
        int over = linkAt(mx, my);
        int[] b = graphBox();
        HoloStyle.chamferFrame(g, b[0] - 10, b[1] - 10, b[2] + 20, b[3] + 20, 2, HoloStyle.argb(0x70, accent));
        int on = done ? GOOD : shape ? GOLD : bright;
        for (int k = 0; k < graph.edges().length; k++) {
            int[] e = graph.edges()[k];
            int ax = nodeX(e[0]), ay = nodeY(e[0]), bx = nodeX(e[1]), by = nodeY(e[1]);
            boolean closed = graph.closed()[k], chosen = a[k] != 0;
            int col = closed ? 0xFFA04838 : chosen ? on : k == over ? 0xFFB8D0E8 : 0xFF6A8098;
            line(g, ax, ay, bx, by, chosen ? 3 : 2, col, closed);
            if (graph.forced()[k]) line(g, ax, ay, bx, by, 1, 0xFFFFFFFF, true);
            // the length, beside the middle of its link (offset square to the link, so it never sits on a line)
            float ln = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
            float nx = ln < 1 ? 0 : -(by - ay) / ln, ny = ln < 1 ? -1 : (bx - ax) / ln;
            if (ny > 0) {
                nx = -nx;
                ny = -ny;
            }
            int lx = (ax + bx) / 2 + Math.round(nx * 7), ly = (ay + by) / 2 + Math.round(ny * 7) - 2;
            small(g, String.valueOf(e[2]), lx, ly, closed ? 0xFFA04838 : chosen ? on : k == over ? 0xFFFFFFFF : 0xFFB0C0D0);
        }
        for (int i = 0; i < graph.n(); i++) {
            int x = nodeX(i), y = nodeY(i);
            boolean end = !tree && (i == 0 || i == graph.n() - 1), via = i == graph.via();
            int col = via ? GOLD : end ? bright : 0xFFB0C0D0;
            g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF0A0E12);
            HoloStyle.outline(g, x - 3, y - 3, 7, 7, col);
            g.fill(x - 1, y - 1, x + 2, y + 2, col);
            if (end) small(g, i == 0 ? "A" : "B", x, y + 6, bright);
        }
        // the panel on the right: the total so far, and the rules in play
        int px = b[0] + b[2] + 22, pw = x0 + W - 10 - px;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.calibrate.graph.total", CalibrationGames.graphLength(graph, a)), px, b[1] - 4, done ? GOOD : TEXT, false);
        List<Component> rules = new ArrayList<>();
        rules.add(Component.translatable(tree ? "gui.theoreticalsciences.calibrate.tree.rule" : "gui.theoreticalsciences.calibrate.path.rule"));
        if (graph.via() >= 0) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.graph.via"));
        boolean anyClosed = false, anyForced = false;
        for (int k = 0; k < graph.edges().length; k++) {
            anyClosed |= graph.closed()[k];
            anyForced |= graph.forced()[k];
        }
        if (anyClosed) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.graph.closed"));
        if (anyForced) rules.add(Component.translatable("gui.theoreticalsciences.calibrate.graph.forced"));
        rules.add(Component.translatable(shape ? "gui.theoreticalsciences.calibrate.graph.shape_ok" : "gui.theoreticalsciences.calibrate.graph.shape_no"));
        int ry = b[1] + 10;
        for (Component c : rules) {
            g.drawWordWrap(font, c, px, ry, pw, c == rules.get(rules.size() - 1) ? (shape ? GOOD : DIM) : DIM);
            ry += font.split(c, pw).size() * 9 + 4;
        }
    }

    // NASH ---------------------------------------------------------------
    /** {x, y, cell w, cell h} of the payoff table. */
    private int[] nashBox() {
        int cw = game.k() == 2 ? 64 : 52, ch = game.k() == 2 ? 40 : 32;
        return new int[]{x0 + 70, y0 + 74, cw, ch};
    }

    private void renderNash(GuiGraphics g, int mx, int my) {
        int[] b = nashBox();
        int k = game.k(), ROW = GOLD, COL = 0xFF60D0FF;
        String[] moves = {"I", "II", "III"};
        for (int j = 0; j < k; j++) g.drawCenteredString(font, moves[j], b[0] + j * b[2] + b[2] / 2, b[1] - 12, COL);
        for (int i = 0; i < k; i++) g.drawString(font, moves[i], b[0] - 14 - font.width(moves[i]) / 2, b[1] + i * b[3] + b[3] / 2 - 4, ROW, false);
        smallLeft(g, Component.translatable("gui.theoreticalsciences.calibrate.nash.you").getString(), b[0] - 46, b[1] - 12, ROW);
        smallLeft(g, Component.translatable("gui.theoreticalsciences.calibrate.nash.them").getString(), b[0] + k * b[2] + 4, b[1] - 12, COL);
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++) {
                int cx = b[0] + j * b[2], cy = b[1] + i * b[3], idx = i * k + j;
                boolean over = mx >= cx && mx < cx + b[2] && my >= cy && my < cy + b[3];
                g.fill(cx + 1, cy + 1, cx + b[2] - 1, cy + b[3] - 1, idx == pick ? 0xE0283848 : over ? 0xE01C2834 : 0xE00E141A);
                HoloStyle.outline(g, cx, cy, b[2], b[3], idx == pick ? bright : HoloStyle.argb(0x50, accent));
                int[] p = game.pay()[i][j];
                String a = String.valueOf(p[0]), c = String.valueOf(p[1]);
                int tw = font.width(a + ", " + c), tx = cx + (b[2] - tw) / 2, ty = cy + b[3] / 2 - 4;
                g.drawString(font, a, tx, ty, ROW, false);
                g.drawString(font, ", ", tx + font.width(a), ty, DIM, false);
                g.drawString(font, c, tx + font.width(a + ", "), ty, COL, false);
            }
        int px = b[0] + k * b[2] + 24, pw = x0 + W - 12 - px;
        g.drawWordWrap(font, Component.translatable("gui.theoreticalsciences.calibrate.nash.key"), px, b[1] + 4, pw, DIM);
    }
}
