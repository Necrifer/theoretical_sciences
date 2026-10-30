package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.machine.MachineType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Procedural "holographic console" kit used by the late-game and endgame machine screens ({@link MachineScreen},
 * {@link PhysicsPanel}). Everything is drawn with plain fills (no textures), in the machine's accent colour.
 * <p>
 * All the methods here only issue {@code fill}/{@code fillGradient} calls (one render type), so callers wrap a whole
 * group of them in {@link GuiGraphics#drawManaged} to have them batched into a single draw call. Text and texture blits
 * must stay outside such a block (they use other render paths and would be reordered).
 */
public final class HoloStyle {
    private HoloStyle() {
    }

    /** The early-game look: the plain panel the screens always had. */
    public static final int CLASSIC = 0;
    /** Late game: holographic panel, hex lattice, chamfered glowing frame, data bars. */
    public static final int LATE = 1;
    /** Endgame: all of the above plus rotating rings / radar sweep, chromatic frame edge and a second frame shimmer. */
    public static final int ENDGAME = 2;

    /**
     * Which look a machine's screen gets. The machine list has no explicit tier field, so the energy buffer (which the
     * mod scales steeply with progression) decides, together with being a multiblock:
     * <ul>
     *   <li>{@link #ENDGAME}: buffer of at least 1,000,000,000 FE - the Omnilex Engine, Axiomatic Breach Engine, Reality
     *       Compiler, Pleroma Reconstitutor, Transcendent Plasma Forge, Eye of the Horizon, Concept Compressor, Dyson Array,
     *       Convergence Engine, Cantor Diagonalizer and Axiom Distiller.</li>
     *   <li>{@link #LATE}: every other multiblock controller, plus single-block machines with a buffer of at least
     *       4,000,000 FE (Ordinal Recursor, Antiproton Decelerator, Omega Simulator).</li>
     *   <li>{@link #CLASSIC}: all remaining (early / mid game) single-block machines.</li>
     * </ul>
     */
    public static int tier(MachineType t) {
        if (t.energyCapacity >= 1_000_000_000) return ENDGAME;
        if (t.isMultiblock() || t.energyCapacity >= 4_000_000) return LATE;
        return CLASSIC;
    }

    // ------------------------------------------------------------------ colour helpers
    public static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0F, 1F);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Output slots get a colour that is clearly not the accent: cool cyan on warm machines, amber on cool ones. */
    public static int outputColour(int accent) {
        int r = (accent >> 16) & 255, b = accent & 255;
        return r > b ? 0x8FF0FF : 0xFFC45C;
    }

    /** A brighter, slightly whitened accent for text on the tinted header. */
    public static int bright(int accent) {
        return mix(accent, 0xFFFFFF, 0.3F);
    }

    // ------------------------------------------------------------------ shapes
    /** A rectangle with 45 degree cut corners of size c. */
    public static void chamferFill(GuiGraphics g, int x, int y, int w, int h, int c, int col) {
        g.fill(x, y + c, x + w, y + h - c, col);
        for (int i = 0; i < c; i++) {
            int in = c - i;
            g.fill(x + in, y + i, x + w - in, y + i + 1, col);
            g.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, col);
        }
    }

    /** 1 px outline of {@link #chamferFill}. */
    public static void chamferFrame(GuiGraphics g, int x, int y, int w, int h, int c, int col) {
        g.fill(x + c, y, x + w - c, y + 1, col);
        g.fill(x + c, y + h - 1, x + w - c, y + h, col);
        g.fill(x, y + c, x + 1, y + h - c, col);
        g.fill(x + w - 1, y + c, x + w, y + h - c, col);
        for (int i = 1; i < c; i++) {
            int in = c - i;
            g.fill(x + in, y + i, x + in + 1, y + i + 1, col);
            g.fill(x + w - 1 - in, y + i, x + w - in, y + i + 1, col);
            g.fill(x + in, y + h - 1 - i, x + in + 1, y + h - i, col);
            g.fill(x + w - 1 - in, y + h - 1 - i, x + w - in, y + h - i, col);
        }
    }

    /** The chamfer diagonals plus {@code len} px of each edge next to them (highlighted corners). */
    public static void chamferCorners(GuiGraphics g, int x, int y, int w, int h, int c, int len, int col) {
        for (int i = 1; i < c; i++) {
            int in = c - i;
            g.fill(x + in, y + i, x + in + 1, y + i + 1, col);
            g.fill(x + w - 1 - in, y + i, x + w - in, y + i + 1, col);
            g.fill(x + in, y + h - 1 - i, x + in + 1, y + h - i, col);
            g.fill(x + w - 1 - in, y + h - 1 - i, x + w - in, y + h - i, col);
        }
        g.fill(x + c, y, x + c + len, y + 1, col);
        g.fill(x + w - c - len, y, x + w - c, y + 1, col);
        g.fill(x + c, y + h - 1, x + c + len, y + h, col);
        g.fill(x + w - c - len, y + h - 1, x + w - c, y + h, col);
        g.fill(x, y + c, x + 1, y + c + len, col);
        g.fill(x, y + h - c - len, x + 1, y + h - c, col);
        g.fill(x + w - 1, y + c, x + w, y + c + len, col);
        g.fill(x + w - 1, y + h - c - len, x + w, y + h - c, col);
    }

    /** Four L-shaped corner brackets of arm length {@code len} on the rectangle x,y,w,h. */
    public static void brackets(GuiGraphics g, int x, int y, int w, int h, int len, int col) {
        g.fill(x, y, x + len, y + 1, col);
        g.fill(x, y + 1, x + 1, y + len, col);
        g.fill(x + w - len, y, x + w, y + 1, col);
        g.fill(x + w - 1, y + 1, x + w, y + len, col);
        g.fill(x, y + h - 1, x + len, y + h, col);
        g.fill(x, y + h - len, x + 1, y + h - 1, col);
        g.fill(x + w - len, y + h - 1, x + w, y + h, col);
        g.fill(x + w - 1, y + h - len, x + w, y + h - 1, col);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int col) {
        g.fill(x, y, x + w, y + 1, col);
        g.fill(x, y + h - 1, x + w, y + h, col);
        g.fill(x, y + 1, x + 1, y + h - 1, col);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    // ------------------------------------------------------------------ the panel
    /**
     * Layered translucent background: base plate, accent haze, a hex lattice with a slow radial wave between patY0 and
     * patY1, scanlines, a drifting scan band and a little flicker noise.
     */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int c, int accent, int tier, float time,
                             int patY0, int patY1, int latticeAlpha) {
        chamferFill(g, x, y, w, h, c, 0xEC06090F);
        g.fillGradient(x + 1, y + c, x + w - 1, y + Math.min(h - c, 96), argb(0x2C, accent), argb(0x00, accent));
        g.fillGradient(x + 1, y + Math.max(c, h - 48), x + w - 1, y + h - c, argb(0x00, accent), argb(0x12, accent));
        hexLattice(g, x + 3, patY0, x + w - 3, patY1, accent, tier, time, latticeAlpha);
        // scanlines
        for (int yy = y + c; yy < y + h - c; yy += 3) g.fill(x + 1, yy, x + w - 1, yy + 1, 0x14000000);
        // drifting scan band
        int band = (int) (time * 1.2F) % (h + 30) - 15;
        int b0 = Math.max(y + c, y + band), b1 = Math.min(y + h - c, y + band + 14);
        if (b1 > b0) g.fillGradient(x + 1, b0, x + w - 1, b1, argb(0x00, accent), argb(0x12, accent));
        if (y + band + 14 >= y + c && y + band + 14 < y + h - c) g.fill(x + 1, y + band + 14, x + w - 1, y + band + 15, argb(0x1C, accent));
        // flicker noise
        java.util.Random r = new java.util.Random((long) (time / 2) * 341873128712L + x * 31L + y);
        for (int i = 0; i < 18; i++) {
            int nx = x + c + r.nextInt(Math.max(1, w - 2 * c)), ny = y + c + r.nextInt(Math.max(1, h - 2 * c));
            g.fill(nx, ny, nx + 1 + r.nextInt(2), ny + 1, argb(0x18 + r.nextInt(0x28), r.nextBoolean() ? accent : 0xFFFFFF));
        }
    }

    /** Honeycomb of short dashes (hexagon tops with their slanted shoulders) brightening in a travelling ring wave. */
    public static void hexLattice(GuiGraphics g, int x0, int y0, int x1, int y1, int accent, int tier, float time, int base) {
        if (base <= 0) return;
        int cw = 14, ch = 8;
        float cx = (x0 + x1) / 2F, cy = (y0 + y1) / 2F;
        float wave = time * (tier >= ENDGAME ? 0.9F : 0.6F);
        int row = 0;
        for (int yy = y0 + 2; yy < y1 - 2; yy += ch, row++) {
            int off = (row & 1) * (cw / 2);
            for (int xx = x0 + 3 + off; xx + 5 <= x1 - 2; xx += cw) {
                float dx = xx - cx, dy = (yy - cy) * 2F;
                float s = Mth.sin((Mth.sqrt(dx * dx + dy * dy) - wave) * 0.11F);
                int al = base + (s > 0.55F ? (int) ((s - 0.55F) / 0.45F * base * 2.2F) : 0);
                int col = argb(al, accent);
                g.fill(xx, yy, xx + 4, yy + 1, col);
                g.fill(xx - 1, yy + 1, xx, yy + 3, argb(al * 2 / 3, accent));
                g.fill(xx + 4, yy + 1, xx + 5, yy + 3, argb(al * 2 / 3, accent));
            }
        }
    }

    /**
     * Glowing chamfered frame with highlighted corners, ruler ticks, raised centre tabs and an edge shimmer; the endgame
     * tier adds a chromatic (red/cyan split) edge and a second shimmer running the other way along the bottom.
     */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int c, int accent, int tier, float time) {
        if (tier >= ENDGAME) {
            int al = 0x40 + (int) (0x18 * Mth.sin(time * 0.21F));
            chamferFrame(g, x - 1, y, w, h, c, argb(al, 0xFF2A7A));
            chamferFrame(g, x + 1, y, w, h, c, argb(al, 0x10E8FF));
        }
        chamferFrame(g, x - 1, y - 1, w + 2, h + 2, c + 1, argb(0x2C, accent));
        chamferFrame(g, x, y, w, h, c, argb(0xB0, accent));
        chamferFrame(g, x + 2, y + 2, w - 4, h - 4, Math.max(1, c - 1), argb(0x20, accent));
        chamferCorners(g, x, y, w, h, c, 10, argb(0xFF, bright(accent)));
        // raised tabs centred on the top and bottom edge
        int tw = Math.max(12, w / 5);
        int tc = argb(0xD0, accent);
        g.fill(x + w / 2 - tw / 2, y - 2, x + w / 2 + tw / 2, y, tc);
        g.fill(x + w / 2 - tw / 2 - 1, y - 1, x + w / 2 - tw / 2, y, argb(0x80, accent));
        g.fill(x + w / 2 + tw / 2, y - 1, x + w / 2 + tw / 2 + 1, y, argb(0x80, accent));
        g.fill(x + w / 2 - tw / 2, y + h, x + w / 2 + tw / 2, y + h + 2, tc);
        // ruler ticks along the bottom edge
        int tk = argb(0x70, accent);
        int i = 0;
        for (int tx = x + c + 12; tx < x + w / 3; tx += 4, i++) g.fill(tx, y + h - (i % 5 == 0 ? 5 : 3), tx + 1, y + h - 1, tk);
        i = 0;
        for (int tx = x + w - c - 13; tx > x + w * 2 / 3; tx -= 4, i++) g.fill(tx, y + h - (i % 5 == 0 ? 5 : 3), tx + 1, y + h - 1, tk);
        // side ticks around the vertical centre
        int mid = y + h / 2;
        for (int k = -4; k <= 4; k++) {
            int ty = mid + k * 4, len = k == 0 ? 4 : (k & 1) == 0 ? 3 : 2;
            g.fill(x + 1, ty, x + 1 + len, ty + 1, tk);
            g.fill(x + w - 1 - len, ty, x + w - 1, ty + 1, tk);
        }
        // shimmer travelling along the top edge (and back along the bottom on the endgame tier)
        int span = w - 2 * c - 16;
        if (span > 0) {
            int p = (int) (time * 2.2F) % (span + 60);
            if (p < span) shimmer(g, x + c + p, y, accent);
            if (tier >= ENDGAME) {
                int q = (int) (time * 1.7F + 37) % (span + 60);
                if (q < span) shimmer(g, x + w - c - 16 - q, y + h - 1, accent);
            }
        }
    }

    private static void shimmer(GuiGraphics g, int sx, int sy, int accent) {
        g.fill(sx, sy, sx + 16, sy + 1, argb(0x60, bright(accent)));
        g.fill(sx + 4, sy, sx + 12, sy + 1, argb(0xB0, 0xFFFFFF));
        g.fill(sx + 7, sy, sx + 9, sy + 1, 0xFFFFFFFF);
    }

    /**
     * Header strip (y+2 .. y+hh) with a sheen and an underline that doubles as a live progress hairline
     * (progress &lt; 0: plain underline). A pulse runs along the lit part while the machine is working.
     */
    public static void header(GuiGraphics g, int x, int y, int w, int hh, int c, int accent, float progress, boolean running, float time) {
        chamferFill(g, x + 3, y + 2, w - 6, hh - 2, Math.max(1, c - 3), argb(0x24, accent));
        g.fillGradient(x + c, y + 2, x + w - c, y + 2 + (hh - 2) / 2, argb(0x14, 0xFFFFFF), argb(0x00, 0xFFFFFF));
        // title chevron
        int cc = argb(0xFF, bright(accent));
        int my = y + 2 + (hh - 2) / 2;
        for (int k = 0; k < 3; k++) {
            g.fill(x + 3 + k, my - 2 + k, x + 4 + k, my - 1 + k, cc);
            g.fill(x + 3 + k, my + 2 - k, x + 4 + k, my + 3 - k, cc);
        }
        int ux0 = x + 4, ux1 = x + w - 4, uy = y + hh;
        g.fill(ux0, uy, ux1, uy + 1, argb(0x50, accent));
        if (progress > 0) {
            int lx = ux0 + (int) ((ux1 - ux0) * Mth.clamp(progress, 0F, 1F));
            g.fill(ux0, uy, lx, uy + 1, argb(0xE0, bright(accent)));
            g.fill(lx - 1, uy - 1, lx, uy + 2, 0xFFFFFFFF);
            if (running && lx - ux0 > 8) {
                int p = ux0 + (int) (time * 3F) % (lx - ux0);
                g.fill(Math.max(ux0, p - 6), uy, p, uy + 1, 0xC0FFFFFF);
            }
        }
    }

    /** A blinking status lamp with a soft halo. */
    public static void lamp(GuiGraphics g, int x, int y, int colour, boolean blink, float time) {
        int al = blink ? 0x90 + (int) (0x6F * (0.5F + 0.5F * Mth.sin(time * 0.35F))) : 0xFF;
        g.fill(x - 1, y - 1, x + 4, y + 4, argb(al / 4, colour));
        g.fill(x, y, x + 3, y + 3, argb(al, colour));
        g.fill(x, y, x + 1, y + 1, argb(al / 2, 0xFFFFFF));
    }

    /** Equalizer-style animated bars filling x..x+w, standing on the baseline y+h (calmer when idle). */
    public static void equalizer(GuiGraphics g, int x, int y, int w, int h, int accent, boolean active, float time) {
        int n = Math.max(1, w / 3);
        int ox = x + (w - n * 3 + 1) / 2;
        for (int i = 0; i < n; i++) {
            float v = Math.abs(Mth.sin(time * 0.13F + i * 0.9F) * Mth.cos(time * 0.071F + i * 1.37F));
            int bh = active ? 1 + (int) (v * (h - 1)) : 1 + (int) (v * 2);
            int bx = ox + i * 3;
            g.fill(bx, y + h - bh, bx + 2, y + h, argb(0x90, accent));
            g.fill(bx, y + h - bh, bx + 2, y + h - bh + 1, argb(0xE0, bright(accent)));
        }
    }

    /** A column of scrolling "telemetry": rows of bars of varying length with index dots. */
    public static void dataColumn(GuiGraphics g, int x, int y, int w, int h, int accent, boolean active, float time) {
        if (w < 8 || h < 8) return;
        outline(g, x, y, w, h, argb(0x30, accent));
        brackets(g, x, y, w, h, 3, argb(0xB0, accent));
        int rows = (h - 4) / 4;
        float speed = active ? 1F : 0.3F;
        int shift = (int) (time * 0.15F * speed);
        for (int r = 0; r < rows; r++) {
            int k = r + shift;
            float v = 0.5F + 0.5F * Mth.sin(k * 1.73F + time * 0.05F * speed) * Mth.cos(k * 0.61F);
            int ry = y + 3 + r * 4;
            int len = 2 + (int) (v * (w - 9));
            g.fill(x + 3, ry, x + 4, ry + 2, argb((k % 3 == 0) ? 0xD0 : 0x50, accent));
            g.fill(x + 6, ry, x + 6 + len, ry + 2, argb(0x60, accent));
            g.fill(x + 5 + len, ry, x + 6 + len, ry + 2, argb(0xE0, bright(accent)));
        }
        // a sweep line moving down the column
        int sy = y + 1 + (int) (time * 0.6F) % Math.max(1, h - 2);
        g.fill(x + 1, sy, x + w - 1, sy + 1, argb(0x40, 0xFFFFFF));
    }

    /**
     * Rotating rings with a radar sweep: a dotted outer ring with a bright 90 degree arc, a counter-rotating inner arc,
     * crosshair ticks, the sweep with its fading trail and a pulsing core.
     */
    public static void rings(GuiGraphics g, int cx, int cy, int r, int accent, boolean active, float time) {
        float spd = active ? 1F : 0.35F;
        int dim = argb(0x40, accent), hi = argb(0xD0, bright(accent));
        float a0 = time * 0.045F * spd;
        int steps = Math.max(16, (int) (r * 2.6F));
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps * Mth.TWO_PI;
            float rel = Mth.positiveModulo(t - a0, Mth.TWO_PI);
            boolean arc = rel < Mth.HALF_PI || (rel > Mth.PI && rel < Mth.PI + 0.5F);
            if (!arc && (i & 1) == 1) continue;
            dot(g, cx + Mth.cos(t) * r, cy + Mth.sin(t) * r, arc ? hi : dim);
        }
        float ri = r * 0.62F;
        int stepsI = Math.max(10, (int) (ri * 2.6F));
        float a1 = -time * 0.07F * spd;
        for (int i = 0; i < stepsI; i++) {
            float t = i / (float) stepsI * Mth.TWO_PI;
            float rel = Mth.positiveModulo(t - a1, Mth.TWO_PI);
            if (rel < 2.1F) dot(g, cx + Mth.cos(t) * ri, cy + Mth.sin(t) * ri, argb(0xA0, accent));
            else if ((i % 3) == 0) dot(g, cx + Mth.cos(t) * ri, cy + Mth.sin(t) * ri, argb(0x30, accent));
        }
        // crosshair ticks
        int tc = argb(0x90, accent);
        g.fill(cx - r - 2, cy, cx - r + 1, cy + 1, tc);
        g.fill(cx + r, cy, cx + r + 3, cy + 1, tc);
        g.fill(cx, cy - r - 2, cx + 1, cy - r + 1, tc);
        g.fill(cx, cy + r, cx + 1, cy + r + 3, tc);
        // radar sweep with trail
        float sw = time * 0.09F * spd;
        for (int k = 3; k >= 0; k--) {
            float ang = sw - k * 0.14F;
            int al = k == 0 ? 0xD0 : 0x60 - k * 0x14;
            float ca = Mth.cos(ang), sa = Mth.sin(ang);
            for (int d = 2; d < r - 1; d += (k == 0 ? 1 : 2)) dot(g, cx + ca * d, cy + sa * d, argb(al, k == 0 ? 0xFFFFFF : accent));
        }
        // a blip that lights up when the sweep passes it
        float blipA = 2.4F, rel = Mth.positiveModulo(sw - blipA, Mth.TWO_PI);
        if (rel < 1.6F) {
            int al = (int) (0xFF * (1 - rel / 1.6F));
            int bx = cx + (int) (Mth.cos(blipA) * r * 0.45F), by = cy + (int) (Mth.sin(blipA) * r * 0.45F);
            g.fill(bx, by, bx + 2, by + 2, argb(al, 0xFFFFFF));
        }
        int core = 0x80 + (int) (0x7F * (0.5F + 0.5F * Mth.sin(time * 0.25F)));
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, argb(core / 3, accent));
        g.fill(cx, cy, cx + 1, cy + 1, argb(core, 0xFFFFFF));
    }

    private static void dot(GuiGraphics g, float fx, float fy, int col) {
        int px = Mth.floor(fx), py = Mth.floor(fy);
        g.fill(px, py, px + 1, py + 1, col);
    }

    // ------------------------------------------------------------------ components
    /**
     * Holographic slot frame (x,y = top-left of the 18x18 square). {@code simple} is the lighter variant used for the
     * 36 player inventory slots.
     */
    public static void slot(GuiGraphics g, int x, int y, int accent, boolean output, boolean simple, boolean active, float time) {
        int col = output ? outputColour(accent) : accent;
        g.fill(x, y, x + 18, y + 18, 0xD805080D);
        if (simple) {
            outline(g, x, y, 18, 18, argb(0x2C, accent));
            g.fill(x, y, x + 3, y + 1, argb(0x90, accent));
            g.fill(x, y + 1, x + 1, y + 3, argb(0x90, accent));
            g.fill(x + 15, y + 17, x + 18, y + 18, argb(0x90, accent));
            g.fill(x + 17, y + 15, x + 18, y + 17, argb(0x90, accent));
            return;
        }
        g.fillGradient(x + 1, y + 1, x + 17, y + 17, argb(0x0C, col), argb(output ? 0x34 : 0x24, col));
        outline(g, x, y, 18, 18, argb(0x40, col));
        outline(g, x + 1, y + 1, 16, 16, argb(output ? 0x30 : 0x18, col));
        int al = output && active ? 0xA0 + (int) (0x5F * (0.5F + 0.5F * Mth.sin(time * 0.3F))) : 0xE0;
        brackets(g, x, y, 18, 18, 4, argb(al, col));
        if (output) {               // a small notch marking the slot as an output
            g.fill(x + 13, y + 2, x + 16, y + 3, argb(0xC0, col));
            g.fill(x + 15, y + 3, x + 16, y + 5, argb(0xC0, col));
        }
    }

    /** Segmented, glowing vertical energy bar (x,y,w,h is the bar's interior as the tooltip sees it). */
    public static void energy(GuiGraphics g, int x, int y, int w, int h, float frac, int accent, float time) {
        g.fill(x, y, x + w, y + h, 0xE005070C);
        outline(g, x - 1, y - 1, w + 2, h + 2, argb(0x70, accent));
        brackets(g, x - 1, y - 1, w + 2, h + 2, 3, argb(0xFF, bright(accent)));
        // scale ticks on the left, quarter marks longer
        for (int q = 0; q <= 8; q++) {
            int ty = y + h - 1 - (h - 1) * q / 8;
            g.fill(x - (q % 2 == 0 ? 5 : 3), ty, x - 2, ty + 1, argb(q % 2 == 0 ? 0x90 : 0x50, accent));
        }
        int n = Math.max(1, h / 4);
        int lit = frac <= 0 ? 0 : Math.max(1, Math.round(frac * n));
        for (int k = 0; k < n; k++) {
            int sy = y + h - (k + 1) * 4 + 1;
            if (k < lit) {
                int c = mix(0xFF3A5A, 0xFFD040, n == 1 ? 1F : k / (float) (n - 1));
                boolean top = k == lit - 1;
                int al = top ? 0xB0 + (int) (0x4F * (0.5F + 0.5F * Mth.sin(time * 0.4F))) : 0xFF;
                g.fill(x + 1, sy, x + w - 1, sy + 3, argb(0x50, c));
                g.fill(x + 2, sy, x + w - 2, sy + 3, argb(al, c));
                g.fill(x + 2, sy, x + w - 2, sy + 1, argb(0x70, 0xFFFFFF));
            } else {
                g.fill(x + 2, sy, x + w - 2, sy + 3, argb(0x16, accent));
            }
        }
        // level caret on the right
        if (lit > 0) {
            int cy = y + h - lit * 4 + 1;
            g.fill(x + w + 2, cy, x + w + 3, cy + 3, argb(0xE0, 0xFFD040));
            g.fill(x + w + 3, cy + 1, x + w + 4, cy + 2, argb(0xE0, 0xFFD040));
        }
    }

    /** Behind the fluid: the dark tube interior. */
    public static void tubeBack(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xE004070B);
        g.fillGradient(x, y, x + w, y + h, argb(0x00, accent), argb(0x18, accent));
    }

    /**
     * In front of the fluid: glass sheen, graduations (tall tubes), the fluid surface line, and a frame with end caps.
     * {@code fluidTop} is the screen y of the fluid surface, or -1 when empty.
     */
    public static void tubeFront(GuiGraphics g, int x, int y, int w, int h, int accent, boolean graduations, int fluidTop) {
        if (fluidTop >= 0) g.fill(x, fluidTop, x + w, fluidTop + 1, 0x70FFFFFF);
        // sheen
        g.fill(x + 2, y + 1, x + 4, y + h - 1, 0x2CFFFFFF);
        g.fill(x + 4, y + 1, x + 5, y + h - 1, 0x12FFFFFF);
        g.fill(x + w - 3, y, x + w, y + h, 0x30000000);
        g.fill(x + w - 4, y + 1, x + w - 3, y + h - 1, 0x10FFFFFF);
        if (graduations) {
            for (int k = 3, i = 1; k < h - 1; k += 3, i++) {
                boolean major = i % 6 == 0;
                if (!major && i % 2 == 1) continue;
                g.fill(x + w - (major ? 7 : 4), y + h - 1 - k, x + w - 1, y + h - k, major ? argb(0xA0, bright(accent)) : 0x60FFFFFF);
            }
        }
        // frame: rails, rounded (corner-less) outline and bright end caps
        int fc = argb(0x90, accent);
        g.fill(x, y - 1, x + w, y, fc);
        g.fill(x, y + h, x + w, y + h + 1, fc);
        g.fill(x - 1, y, x, y + h, fc);
        g.fill(x + w, y, x + w + 1, y + h, fc);
        int cap = argb(0xFF, bright(accent));
        g.fill(x + 3, y - 1, x + w - 3, y, cap);
        g.fill(x + 3, y + h, x + w - 3, y + h + 1, cap);
    }

    /** Field box (parameter values etc.): dark plate, faint outline, bright corner brackets and a left accent bar. */
    public static void field(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x, y, x + w, y + h, 0xE805070C);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, argb(0x06, accent), argb(0x18, accent));
        outline(g, x, y, w, h, argb(0x40, accent));
        brackets(g, x, y, w, h, 3, argb(0xD0, accent));
        g.fill(x + 1, y + 2, x + 2, y + h - 2, argb(0xC0, bright(accent)));
    }

    /** Segmented horizontal bar (integrity etc.). */
    public static void hbar(GuiGraphics g, int x, int y, int w, int h, float frac, int colour, int accent) {
        g.fill(x, y, x + w, y + h, 0xE805070C);
        int n = Math.max(1, w / 3);
        int lit = frac <= 0 ? 0 : Math.max(1, Math.round(Mth.clamp(frac, 0F, 1F) * n));
        for (int k = 0; k < n; k++) {
            int sx = x + k * 3;
            g.fill(sx, y + 1, sx + 2, y + h - 1, k < lit ? argb(0xFF, colour) : argb(0x18, accent));
        }
        if (lit > 0) g.fill(x, y + 1, x + lit * 3 - 1, y + 2, 0x50FFFFFF);
        outline(g, x - 1, y - 1, w + 2, h + 2, argb(0x60, accent));
        brackets(g, x - 1, y - 1, w + 2, h + 2, 2, argb(0xE0, accent));
    }

    /**
     * Progress conduit: a housed energy channel from the input side to the output side (x,y as the old 24x16 arrow),
     * with emitter / receiver nodes, stencilled flow chevrons, the charge filling up and pulses flowing through it.
     */
    public static void conduit(GuiGraphics g, int ax, int ay, float frac, boolean running, int accent, float time) {
        int len = 24;
        g.fill(ax - 1, ay + 5, ax + len + 1, ay + 11, 0xE005070C);
        g.fill(ax - 1, ay + 5, ax + len + 1, ay + 6, argb(0x90, accent));
        g.fill(ax - 1, ay + 10, ax + len + 1, ay + 11, argb(0x90, accent));
        // nodes
        int pulse = running ? 0x90 + (int) (0x6F * (0.5F + 0.5F * Mth.sin(time * 0.4F))) : 0x60;
        for (int nx : new int[]{ax - 3, ax + len + 1}) {
            g.fill(nx, ay + 3, nx + 2, ay + 13, argb(0xC0, accent));
            g.fill(nx, ay + 6, nx + 2, ay + 10, argb(pulse, 0xFFFFFF));
        }
        // chevrons
        int cv = argb(0x38, accent);
        for (int k = 2; k < len - 1; k += 6) {
            g.fill(ax + k, ay + 6, ax + k + 1, ay + 7, cv);
            g.fill(ax + k + 1, ay + 7, ax + k + 2, ay + 9, cv);
            g.fill(ax + k, ay + 9, ax + k + 1, ay + 10, cv);
        }
        int lit = (int) (Mth.clamp(frac, 0F, 1F) * len);
        if (lit > 0) {
            for (int i = 0; i < lit; i++) g.fill(ax + i, ay + 6, ax + i + 1, ay + 10, argb(0xE0, mix(accent, 0xFFFFFF, i / (float) len * 0.75F)));
            g.fill(ax, ay + 4, ax + lit, ay + 5, argb(0x38, accent));
            g.fill(ax, ay + 11, ax + lit, ay + 12, argb(0x38, accent));
            g.fill(ax + lit - 1, ay + 4, ax + lit, ay + 12, 0xD0FFFFFF);
        }
        float speed = running ? 0.8F : 0.22F;
        for (int k = 0; k < 3; k++) {
            int px = ax + (int) ((time * speed + k * 8) % len);
            int al = running ? 0xFF : 0x50;
            g.fill(Math.max(ax, px - 3), ay + 7, px, ay + 9, argb(al / 3, bright(accent)));
            g.fill(px, ay + 7, Math.min(ax + len, px + 2), ay + 9, argb(al, 0xFFFFFF));
        }
    }

    /** Small text (scaled), optionally centred on x. Not for use inside a managed block. */
    public static void miniText(GuiGraphics g, Font font, String s, int x, int y, int colour, float scale, boolean centred) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, s, centred ? -font.width(s) / 2 : 0, 0, colour, false);
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ simple machine windows
    /** Animation clock for screens (game ticks plus the partial tick; wall clock when no world is loaded). */
    public static float time(float partial) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() + partial : (net.minecraft.Util.getMillis() % 1_000_000L) / 50F;
    }

    /**
     * Background of a smaller machine screen (cache, compactor, anomaly engine...): the same holographic plate, header
     * strip and glowing frame as the big machine consoles, so every machine of the mod looks part of one system.
     */
    public static void window(GuiGraphics g, int x, int y, int w, int h, int accent, int tier, float time, int headerH) {
        g.drawManaged(() -> {
            panel(g, x, y, w, h, 6, accent, tier, time, y + headerH + 2, y + h - 4, tier >= ENDGAME ? 0x16 : tier >= LATE ? 0x10 : 0x0C);
            header(g, x, y, w, headerH, 6, accent, -1, false, time);
            frame(g, x, y, w, h, 6, accent, tier, time);
        });
    }

    /** The divider above the player inventory, brighter under its label. */
    public static void divider(GuiGraphics g, int x, int y, int w, int labelX, int labelW, int accent) {
        g.fill(x + 6, y, x + w - 6, y + 1, argb(0x40, accent));
        g.fill(labelX - 2, y, labelX + labelW + 4, y + 1, argb(0xD0, accent));
        for (int k = 0; k < 3; k++) g.fill(x + w - 14 - k * 4, y - 1, x + w - 12 - k * 4, y + 2, argb(0x90 - k * 0x28, accent));
    }

    /** Holographic slot frames for every slot of a menu: the first {@code machineSlots} are the machine's own. */
    public static void slots(GuiGraphics g, int x, int y, java.util.List<net.minecraft.world.inventory.Slot> slots, int machineSlots,
                             java.util.function.IntPredicate output, int accent, boolean active, float time) {
        g.drawManaged(() -> {
            for (int i = 0; i < slots.size(); i++) {
                var s = slots.get(i);
                boolean mine = i < machineSlots;
                slot(g, x + s.x - 1, y + s.y - 1, accent, mine && output.test(i), !mine, mine && active, time);
            }
        });
    }
}
