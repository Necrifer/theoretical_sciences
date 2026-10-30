package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.screen.forge.CoreCode;
import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.transcend.AttunePayloads;
import com.theoreticalsciences.transcend.ForgedCoreBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Round 34c: attuning a forged Omnipotence Core to the place it stands in (opened with the Hand of the Demiurge in
 * Tune mode). Seven branches, three parameters each; every branch panel shows only how in tune the branch is as a
 * whole. The rules each branch follows are in {@link ForgedCoreBlockEntity}; this screen only knows what the server
 * tells it.
 * <p>
 * Round 35: drawn as the Core's own terminal (the same full-window green monitor as {@link OmnipotenceScreen}):
 * the branches are windows of the console, with a terminal log and the Core's live program listing underneath.
 * Controls: drag a track, or scroll on it (shift: x10, ctrl: x100); [-] / [+] at its ends step by one.
 */
public class AttuneScreen extends Screen {
    private static final int B = ForgedCoreBlockEntity.BRANCHES, P = ForgedCoreBlockEntity.PER;
    private static final int GREEN = 0xFF5CFF8A, DIM = 0xFF2F8A4C, FAINT = 0xFF1A4A2A, AMBER = 0xFFFFC24A, RED = 0xFFFF5A4A;
    /** Each branch's own colour: only its tag on the panel, the rest of the console stays green. */
    private static final int[] COLOUR = {0x66CCFF, 0x66FF99, 0xFF9A66, 0xFFE066, 0xFFD8A0, 0x9A7CFF, 0xFF4D6A};
    private static final String[] KEY = {"physics", "chemistry", "biology", "mathematics", "origin", "finality", "abnormality"};

    private final BlockPos core;
    private final int[] v = new int[ForgedCoreBlockEntity.N];
    private final float[] h = new float[B], hShown = new float[B];
    private final boolean[] wasTuned = new boolean[B];
    private int lamps;
    private boolean clouded, active, wasClouded, wasActive, first = true;
    private final Smooth smooth = new Smooth();
    private final long opened = System.currentTimeMillis();
    private float time;
    private int ticks;
    /** The parameter being dragged (-1 none), and parameters changed here but not sent yet. */
    private int dragging = -1;
    private final boolean[] pending = new boolean[ForgedCoreBlockEntity.N];
    private final List<int[]> tracks = new ArrayList<>();    // per frame: {index, trackX, y, trackW, h, rowX, rowW}
    private final List<String> log = new ArrayList<>();
    private final CoreCode code = new CoreCode();
    private String tip;

    public AttuneScreen(BlockPos core, CompoundTag tag) {
        super(Component.translatable("gui.theoreticalsciences.attune.title"));
        this.core = core;
        say("OMNIPOTENCE CORE :: ATTUNEMENT v∞");
        say("core at " + core.getX() + " " + core.getY() + " " + core.getZ() + " :: reading the laws of this place");
        code.emit("import core.omnipotence, attune  // v∞");
        code.emit("let core = Core.attach(" + core.getX() + ", " + core.getY() + ", " + core.getZ() + ")");
        code.emit("let laws = place.read()  // hidden: 21 parameters, 7 branches");
        receive(tag);
        System.arraycopy(h, 0, hShown, 0, B);
    }

    public BlockPos core() {
        return core;
    }

    private void say(String s) {
        log.add(s);
        while (log.size() > 60) log.remove(0);
    }

    private static String name(int b) {
        return Component.translatable("gui.theoreticalsciences.attune.branch." + KEY[b]).getString();
    }

    private static String param(int i) {
        return Component.translatable("gui.theoreticalsciences.attune.param." + KEY[i / P] + "." + i % P).getString();
    }

    private boolean tuned(int b) {
        return b == ForgedCoreBlockEntity.MATHEMATICS ? lamps == 15 : h[b] >= ForgedCoreBlockEntity.IN_TUNE;
    }

    public void receive(CompoundTag t) {
        int[] nv = t.getIntArray("v");
        if (nv.length == v.length)
            for (int i = 0; i < v.length; i++) if (i != dragging && !pending[i]) v[i] = nv[i];
        int[] nh = t.getIntArray("h");
        if (nh.length == B) for (int b = 0; b < B; b++) h[b] = nh[b] / 1000F;
        lamps = t.getInt("lamps");
        clouded = t.getBoolean("clouded");
        active = t.getBoolean("active");
        // the console reports what changed
        if (clouded && !wasClouded) {
            say("WARNING anomaly uncontained :: readings unreliable");
            code.emit("assert anomaly.contained() -> FAIL  // WARNING: spreading");
        } else if (!clouded && wasClouded && !first) {
            say("anomaly contained :: readings clear");
            code.emit("assert anomaly.contained() -> OK");
        }
        for (int b = 0; b < B; b++) {
            boolean on = tuned(b);
            if (!first && !clouded && on != wasTuned[b]) {
                say("branch/" + KEY[b] + (on ? " LOCKED" : " drifted out of tune"));
                code.emit(String.format(Locale.ROOT, "branch.%s.lock() -> %s", KEY[b], on ? "LOCKED" : "FAIL"));
            }
            wasTuned[b] = on;
        }
        if (active && !wasActive && !first) {
            say("CORE AWAKE :: in tune with this place");
            code.emit("core.state -> AWAKE  // OK");
        } else if (!active && wasActive) say("core fell dormant");
        if (first) say(active ? "core state: AWAKE" : "core state: DORMANT");
        wasClouded = clouded;
        wasActive = active;
        first = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;            // entropy and the anomaly keep moving while you work
    }

    @Override
    public void tick() {
        ticks++;
        boolean sent = false;
        for (int i = 0; i < pending.length; i++)
            if (pending[i] && (i != dragging || ticks % 2 == 0)) {
                send(i, v[i]);
                pending[i] = false;
                sent = true;
            }
        if (!sent && ticks % 10 == 0) send(-1, 0);
    }

    private void send(int index, int value) {
        PacketDistributor.sendToServer(new AttunePayloads.Set(core, index, value));
    }

    // ================================================================== layout (from the window, like the Forge)
    private int L, T, W, H, inL, inW, CT, BY, BH, gridBottom, logW, codeX, codeW;
    private int[] wakeBtn = new int[4], exitBtn = new int[4];

    private void layout() {
        int m = width >= 640 && height >= 360 ? 8 : 4;
        L = m;
        T = m;
        W = width - 2 * m;
        H = height - 2 * m;
        inL = L + 6;
        inW = W - 12;
        CT = T + 36;
        BH = Mth.clamp(H / 4, 46, 96);
        BY = T + H - BH - 2;
        gridBottom = BY - 8;
        logW = (int) (inW * 0.42F);
        codeX = inL + logW + 6;
        codeW = inW - logW - 6;
    }

    private static final int GAP = 6;

    private int cw() {
        return (inW - 3 * GAP) / 4;
    }

    private int ch() {
        return (gridBottom - CT - GAP) / 2;
    }

    private int cx(int slot) {
        return inL + (slot % 4) * (cw() + GAP);
    }

    private int cy(int slot) {
        return CT + (slot / 4) * (ch() + GAP);
    }

    @Override
    protected void init() {
        layout();
    }

    // ================================================================== drawing helpers
    /** A thin frame with its title written into the top edge, like a window of an old terminal program. */
    private void panel(GuiGraphics g, int x, int y, int w, int hh, String title, int accent) {
        g.fill(x, y, x + w, y + hh, 0x40000000);
        g.renderOutline(x, y, w, hh, FAINT);
        int c = accent;
        g.fill(x, y, x + 5, y + 1, c);
        g.fill(x, y, x + 1, y + 5, c);
        g.fill(x + w - 5, y, x + w, y + 1, c);
        g.fill(x + w - 1, y, x + w, y + 5, c);
        g.fill(x, y + hh - 1, x + 5, y + hh, c);
        g.fill(x, y + hh - 5, x + 1, y + hh, c);
        g.fill(x + w - 5, y + hh - 1, x + w, y + hh, c);
        g.fill(x + w - 1, y + hh - 5, x + w, y + hh, c);
        if (title != null && w > 24) {
            String t = cut(title, w - 16);
            g.fill(x + 6, y - 1, x + 10 + font.width(t), y + 1, 0xFF020A05);
            g.drawString(font, t, x + 8, y - 4, accent, false);
        }
    }

    private String cut(String s, int w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, Math.max(0, w - font.width("…"))) + "…";
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** A console command used as a button: "[ WAKE ]". */
    private int[] command(GuiGraphics g, int x, int y, String label, boolean enabled, boolean pulse, int mx, int my) {
        int w = font.width(label);
        boolean over = enabled && in(mx, my, x, y - 1, w, 10);
        int col = !enabled ? FAINT : pulse ? ((System.currentTimeMillis() / 400) % 2 == 0 ? AMBER : GREEN) : over ? AMBER : GREEN;
        g.drawString(font, label, x, y, col, false);
        return new int[]{x, y - 1, w, 10};
    }

    // ================================================================== drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        // the monitor: bezel and screen
        g.fill(0, 0, width, height, 0xFF070908);
        g.renderOutline(L - 3, T - 3, W + 6, H + 6, 0xFF1E2621);
        g.fill(L, T, L + W, T + H, clouded ? 0xF80A0504 : 0xF8020A05);
        g.renderOutline(L, T, W, H, clouded ? 0xFF6A3020 : DIM);
        g.fill(L + 4, T + 15, L + W - 4, T + 16, FAINT);
        g.fill(L + 4, T + 30, L + W - 4, T + 31, FAINT);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (W == 0) layout();
        smooth.frame();
        time = ticks + pt;
        tip = null;
        for (int b = 0; b < B; b++) hShown[b] = (float) smooth.toward(hShown[b], h[b]);
        super.render(g, mx, my, pt);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;

        // header
        String state = active ? "AWAKE" : "DORMANT";
        g.drawString(font, cut("OMNIPOTENCE//CORE  ::  ATTUNEMENT  ::  " + state, W - 90), L + 6, T + 5, active ? AMBER : GREEN, false);
        String clock = String.format(Locale.ROOT, "t+%06.1fs", (System.currentTimeMillis() - opened) / 1000.0);
        g.drawString(font, clock, L + W - 6 - font.width(clock), T + 5, DIM, false);
        // the status line, with the commands at its right
        float weakest = 1;
        int worst = 0;
        for (int b = 0; b < B; b++)
            if (hShown[b] < weakest) {
                weakest = hShown[b];
                worst = b;
            }
        boolean ready = !clouded && !active;
        for (int b = 0; b < B; b++) if (!tuned(b)) ready = false;
        String status = active ? "attune> all branches in tune :: the Core is awake"
                : clouded ? "attune> WARNING readings clouded by the anomaly :: contain Abnormality first"
                : String.format(Locale.ROOT, "attune> weakest branch: %s %.1f%%  ::  needs %d%% in every branch", name(worst).toLowerCase(Locale.ROOT),
                weakest * 100, Math.round(ForgedCoreBlockEntity.IN_TUNE * 100));
        exitBtn = command(g, L + W - 6 - font.width("[ EXIT ]"), T + 20, "[ EXIT ]", true, false, mx, my);
        String wl = active ? "[ AWAKE ]" : "[ WAKE ]";
        wakeBtn = command(g, exitBtn[0] - 10 - font.width(wl), T + 20, wl, !active, ready, mx, my);
        if (!active && in(mx, my, wakeBtn[0], wakeBtn[1], wakeBtn[2], wakeBtn[3])) tip = "gui.theoreticalsciences.attune.wake_help";
        g.drawString(font, cut(status, wakeBtn[0] - L - 16) + (blink ? "_" : ""), L + 6, T + 20, clouded ? RED : DIM, false);

        // the branches
        tracks.clear();
        for (int b = 0; b < B; b++) branch(g, b, cx(b), cy(b), cw(), ch(), mx, my);
        ring(g, cx(7), cy(7), cw(), ch());

        // terminal log and the program listing
        int acc = clouded ? 0xFF803020 : DIM;
        panel(g, inL, BY, logW, BH - 4, "terminal/", acc);
        int rows = Math.max(1, (BH - 4 - 10) / 9), from = Math.max(0, log.size() - rows), ly = BY + 6;
        for (int i = from; i < log.size(); i++) {
            boolean last = i == log.size() - 1;
            g.drawString(font, cut("> " + log.get(i), logW - 12) + (last && blink ? "_" : ""), inL + 5, ly, last ? GREEN : DIM, false);
            ly += 9;
        }
        panel(g, codeX, BY, codeW, BH - 4, "core.exec  ln " + String.format("%04d", code.lines() % 10000), acc);
        code.frame(this::idleLine, 0.8);
        code.render(g, font, codeX + 3, BY + 6, codeW - 6, BH - 4 - 10);

        crt(g);
        if (tip != null) TextFit.tooltip(g, font, List.of(Component.translatable(tip)), mx, my);
    }

    /** One branch: a window of the console. */
    private void branch(GuiGraphics g, int b, int x, int y, int w, int hgt, int mx, int my) {
        boolean ok = tuned(b);
        int acc = ok ? GREEN : b == ForgedCoreBlockEntity.ABNORMALITY && clouded ? RED : DIM;
        panel(g, x, y, w, hgt, "branch/" + KEY[b], acc);
        if (in(mx, my, x, y - 4, w, 14)) tip = "gui.theoreticalsciences.attune.hint." + KEY[b];
        // the branch's tag, name and harmony
        int tagC = 0xFF000000 | COLOUR[b];
        g.fill(x + 6, y + 7, x + 10, y + 11, tagC);
        g.drawString(font, cut(name(b).toUpperCase(Locale.ROOT), w - 70), x + 13, y + 5, ok ? GREEN : 0xFF9CE8B0, false);
        String pct = String.format(Locale.ROOT, "%5.1f%%", hShown[b] * 100);
        g.drawString(font, pct, x + w - 6 - font.width(pct), y + 5, ok ? GREEN : hShown[b] > 0.8F ? AMBER : RED, false);
        // the harmony as a segmented bar, like a progress line of an old program
        int bx = x + 6, bw = w - 12, by = y + 16, n = Math.max(4, bw / 3);
        int lit = Math.round(Mth.clamp(hShown[b], 0, 1) * n);
        for (int k = 0; k < n; k++)
            g.fill(bx + k * 3, by, bx + k * 3 + 2, by + 4, k < lit ? (ok ? GREEN : DIM) : 0xFF0C2414);
        int need = Math.round(ForgedCoreBlockEntity.IN_TUNE * n);
        g.fill(bx + need * 3 - 1, by - 1, bx + need * 3, by + 5, AMBER);
        if (b == ForgedCoreBlockEntity.MATHEMATICS) {
            // the four laws, as console flags
            String[] law = {"a≡7", "ab≡11", "Σ≡13", "n∈ℙ"};
            int lx = x + 6;
            for (int k = 0; k < 4; k++) {
                boolean on = (lamps >> k & 1) != 0;
                String s = (on ? "[✓" : "[ ") + law[k] + "]";
                g.drawString(font, s, lx, y + 24, clouded ? ((System.currentTimeMillis() / 300 + k) % 2 == 0 ? FAINT : DIM) : on ? GREEN : FAINT, false);
                lx += font.width(s) + 3;
            }
        }
        // round 35: a small window (big GUI scale) puts each dial on one line: name, track, value
        boolean compact = hgt < 92;
        int top = y + (b == ForgedCoreBlockEntity.MATHEMATICS ? (compact ? 34 : 35) : (compact ? 25 : 26));
        int row = Math.max(compact ? 11 : 17, (y + hgt - 4 - top) / P);
        for (int j = 0; j < P; j++) {
            if (compact) paramCompact(g, b, j, x + 6, top + j * row, w - 12, mx, my);
            else param(g, b, j, x + 6, top + j * row, w - 12, mx, my);
        }
    }

    private void param(GuiGraphics g, int b, int j, int x, int y, int w, int mx, int my) {
        int i = b * P + j;
        String label = param(i);
        String val = format(i, v[i]);
        g.drawString(font, cut(label, w - font.width(val) - 6), x, y, 0xFF5FA878, false);
        g.drawString(font, val, x + w - font.width(val), y, dragging == i ? AMBER : GREEN, false);
        int ty = y + 10, bw = font.width("[-]");
        boolean hm = in(mx, my, x, ty - 1, bw, 9), hp = in(mx, my, x + w - bw, ty - 1, bw, 9);
        g.drawString(font, "[-]", x, ty - 1, hm ? AMBER : DIM, false);
        g.drawString(font, "[+]", x + w - bw, ty - 1, hp ? AMBER : DIM, false);
        int tx = x + bw + 3, tw = w - 2 * bw - 6;
        float f = v[i] / (float) ForgedCoreBlockEntity.max(i);
        g.fill(tx, ty + 3, tx + tw, ty + 4, FAINT);
        g.fill(tx, ty + 2, tx + (int) (tw * f), ty + 5, DIM);
        // graduations every tenth
        for (int k = 0; k <= 10; k++) g.fill(tx + tw * k / 10, ty + 1, tx + tw * k / 10 + 1, ty + 2, FAINT);
        int kx = tx + (int) (tw * f);
        boolean hot = dragging == i || in(mx, my, tx - 2, ty - 2, tw + 4, 9);
        g.fill(kx - 1, ty, kx + 2, ty + 7, hot ? AMBER : GREEN);
        tracks.add(new int[]{i, tx, ty - 2, tw, 9, x, w, bw});
    }

    private void paramCompact(GuiGraphics g, int b, int j, int x, int y, int w, int mx, int my) {
        int i = b * P + j;
        String val = format(i, v[i]);
        int vw = font.width("000.0%"), lw = (int) (w * 0.36F);
        g.drawString(font, cut(param(i), lw), x, y, 0xFF5FA878, false);
        g.drawString(font, val, x + w - font.width(val), y, dragging == i ? AMBER : GREEN, false);
        int tx = x + lw + 3, tw = Math.max(10, w - lw - vw - 8), ty = y;
        float f = v[i] / (float) ForgedCoreBlockEntity.max(i);
        g.fill(tx, ty + 4, tx + tw, ty + 5, FAINT);
        g.fill(tx, ty + 3, tx + (int) (tw * f), ty + 6, DIM);
        int kx = tx + (int) (tw * f);
        boolean hot = dragging == i || in(mx, my, tx - 2, ty - 1, tw + 4, 9);
        g.fill(kx - 1, ty + 1, kx + 2, ty + 8, hot ? AMBER : GREEN);
        tracks.add(new int[]{i, tx, ty - 1, tw, 9, x, w, 0});
        if (in(mx, my, x, ty - 1, lw, 9)) tip = "gui.theoreticalsciences.attune.compact_help";
    }

    /** The eighth window: the ring the branches lean on each other in. */
    private void ring(GuiGraphics g, int x, int y, int w, int hgt) {
        panel(g, x, y, w, hgt, "ring/", DIM);
        int[] order = {0, 1, 2, 4, 5, 6};
        int rcx = x + w / 2, rcy = y + hgt / 2 + 2, rr = Math.max(10, Math.min(w / 2 - 22, hgt / 2 - 16));
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.ADD);
        f.ring(rcx, rcy, rr, 1, 48, 0x2F8A4C, 0.6F);
        // a pulse running round the ring, the way each branch leans on the next
        double a0 = time * 0.03;
        for (int s = 0; s < 10; s++) {
            double a = a0 - s * 0.06;
            f.disc((float) (rcx + Math.cos(a) * rr), (float) (rcy + Math.sin(a) * rr), 2.2F - s * 0.18F, 8, 0x5CFF8A, 0.6F - s * 0.055F, 0x5CFF8A, 0);
        }
        f.end();
        for (int k = 0; k < order.length; k++) {
            int b = order[k];
            double a = -Math.PI / 2 + k * Math.PI * 2 / order.length;
            int dx = rcx + (int) (Math.cos(a) * rr), dy = rcy + (int) (Math.sin(a) * rr);
            g.fill(dx - 2, dy - 2, dx + 3, dy + 3, 0xFF000000 | COLOUR[b]);
            String s = name(b).substring(0, Math.min(4, name(b).length())).toUpperCase(Locale.ROOT);
            int tx = Math.cos(a) < -0.2 ? dx - 5 - font.width(s) : Math.cos(a) > 0.2 ? dx + 5 : dx - font.width(s) / 2;
            int ty = Math.sin(a) < -0.5 ? dy - 11 : Math.sin(a) > 0.5 ? dy + 4 : dy - 4;
            g.drawString(font, s, tx, ty, tuned(b) ? GREEN : DIM, false);
        }
        String c = Component.translatable("gui.theoreticalsciences.attune.ring").getString();
        HoloStyle.miniText(g, font, cut(c, (int) ((rr * 2 - 8) / 0.7F)), rcx, rcy - 3, 0xFF3F7A52, 0.7F, true);
        String m = (lamps == 15 ? "[✓" : "[ ") + "MATH] stands apart";
        g.drawString(font, cut(m, w - 12), x + 6, y + hgt - 12, tuned(ForgedCoreBlockEntity.MATHEMATICS) ? GREEN : FAINT, false);
    }

    /** The glass of the monitor: faint scanlines, a slow bright sweep, darker edges and a rare flicker. */
    private void crt(GuiGraphics g) {
        for (int sy = T; sy < T + H; sy += 2) g.fill(L, sy, L + W, sy + 1, 0x12000000);
        int sweep = (int) ((System.currentTimeMillis() / 12) % (H + 40)) - 20;
        if (sweep > 0 && sweep < H) g.fill(L, T + sweep, L + W, T + Math.min(H, sweep + 3), 0x0C5CFF8A);
        long q = System.currentTimeMillis() / 90;
        if ((q * 0x9E3779B1L >>> 20 & 63) == 0) g.fill(L, T, L + W, T + H, clouded ? 0x10FF5A4A : 0x0A5CFF8A);
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.NORMAL);
        int e = Math.min(28, Math.min(W, H) / 8);
        f.rectH(L, T, L + e, T + H, 0, 0.35F, 0, 0);
        f.rectH(L + W - e, T, L + W, T + H, 0, 0, 0, 0.35F);
        f.rectV(L, T, L + W, T + e, 0, 0.3F, 0, 0);
        f.rectV(L, T + H - e, L + W, T + H, 0, 0, 0, 0.3F);
        f.end();
    }

    /** What the program does while nobody touches anything. */
    private String idleLine(int step) {
        if (active) return switch (step % 3) {
            case 0 -> "core.hold()  // awake";
            case 1 -> "  assert attuned(place) -> OK";
            default -> null;
        };
        if (clouded && step % 3 == 0) return "  anomaly.spread()  // WARNING: nudging the ring";
        int b = step % B;
        if (b == ForgedCoreBlockEntity.MATHEMATICS)
            return String.format(Locale.ROOT, "  laws.check(a=%d, b=%d, n=%d) -> %d/4", v[9], v[10], v[11], Integer.bitCount(lamps));
        if (b == ForgedCoreBlockEntity.FINALITY && step % 2 == 0) return "  entropy.tick()  // +0.1 per half second";
        return String.format(Locale.ROOT, "  measure(%s) -> %.3f%s", KEY[b], h[b], tuned(b) ? "  // LOCKED" : "");
    }

    private static String format(int i, int x) {
        int b = i / P;
        if (b == ForgedCoreBlockEntity.MATHEMATICS) return Integer.toString(x);
        if (b == ForgedCoreBlockEntity.ORIGIN) return x / 10 + "." + x % 10 + "°";
        return x / 10 + "." + x % 10 + "%";
    }

    // ================================================================== input
    private void change(int i, int to) {
        int nv = i / P == ForgedCoreBlockEntity.ORIGIN ? Math.floorMod(to, 3600) : Mth.clamp(to, 0, ForgedCoreBlockEntity.max(i));
        if (nv == v[i]) return;
        v[i] = nv;
        pending[i] = true;
        String id = param(i).toLowerCase(Locale.ROOT).replace(' ', '_');
        code.emit("set" + i, String.format(Locale.ROOT, "%s.%s = %s  // h=%.3f", KEY[i / P], id, format(i, nv), h[i / P]));
        if (minecraft != null && minecraft.player != null)
            minecraft.player.playSound(com.theoreticalsciences.registry.QHSounds.UI_CLICK.get(), 0.08F, 1.4F + 0.6F * hShown[i / P]);
    }

    private int step() {
        return hasControlDown() ? 100 : hasShiftDown() ? 10 : 1;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (in(mx, my, wakeBtn[0], wakeBtn[1], wakeBtn[2], wakeBtn[3]) && !active) {
                say("core.wake() requested");
                code.emit("core.wake()  // checking every branch");
                send(-2, 0);
                return true;
            }
            if (in(mx, my, exitBtn[0], exitBtn[1], exitBtn[2], exitBtn[3])) {
                onClose();
                return true;
            }
            for (int[] t : tracks) {
                int i = t[0], ty = t[2] + 1, x = t[5], w = t[6], bw = t[7];
                if (bw > 0 && in(mx, my, x, ty, bw, 9)) {
                    change(i, v[i] - step());
                    return true;
                }
                if (bw > 0 && in(mx, my, x + w - bw, ty, bw, 9)) {
                    change(i, v[i] + step());
                    return true;
                }
                if (in(mx, my, t[1] - 2, t[2], t[3] + 4, t[4])) {
                    dragging = i;
                    dragTo(t, mx);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void dragTo(int[] t, double mx) {
        float f = Mth.clamp((float) ((mx - t[1]) / t[3]), 0F, 1F);
        change(t[0], Math.round(f * ForgedCoreBlockEntity.max(t[0])));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0)
            for (int[] t : tracks)
                if (t[0] == dragging) {
                    dragTo(t, mx);
                    return true;
                }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0) {
            send(dragging, v[dragging]);
            pending[dragging] = false;
            dragging = -1;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        for (int[] t : tracks)
            if (in(mx, my, t[5], t[2] - (t[7] > 0 ? 10 : 0), t[6], t[4] + (t[7] > 0 ? 10 : 1))) {
                change(t[0], v[t[0]] + (int) Math.signum(sy) * step());
                return true;
            }
        return super.mouseScrolled(mx, my, sx, sy);
    }
}
