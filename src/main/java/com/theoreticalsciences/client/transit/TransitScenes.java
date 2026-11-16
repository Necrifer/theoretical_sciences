package com.theoreticalsciences.client.transit;

import com.theoreticalsciences.client.transit.Fx2D.Blend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The twelve travel animations, one per dimension, all drawn from code (no textures).
 *
 * <p>Every scene is a function of three numbers:
 * <ul>
 *   <li>{@code t} - the time in seconds, which keeps things moving (waves travel, bubbles grow, wireframes turn);</li>
 *   <li>{@code p} - how far the animation has built up, 0 (nothing, you still see the world) to 1 (the full scene,
 *       which is also what the loading page shows);</li>
 *   <li>{@code rev} - true when leaving the dimension: the same scene, but running backwards or mirrored.</li>
 * </ul>
 * At p = 1 the background is fully opaque, so the moment the game swaps to the loading page there is no visible jump.
 */
public final class TransitScenes {
    private static final float TAU = (float) (Math.PI * 2);

    /** Draws the scene for {@code s} filling a w x h screen. */
    public static void draw(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        p = clamp(p);
        if (p <= 0.001F) return;
        // the backdrop: fades in with the scene and is solid when it is complete
        int bg = s == TransitStyle.WHITE_HOLE ? Fx2D.lerpRgb(0x0A1020, s.bg, whiteFlash(p)) : s.bg;
        Fx2D f = Fx2D.begin(g, Blend.NORMAL);
        f.rect(0, 0, w, h, bg, (float) Math.pow(p, 1.5));
        f.end();
        switch (s) {
            case BULK -> bulk(g, s, w, h, t, p, rev);
            case FOAM -> foam(g, s, w, h, t, p, rev);
            case PLATONIC -> platonic(g, s, w, h, t, p, rev);
            case BLACK_HOLE -> blackHole(g, s, w, h, t, p, rev);
            case WHITE_HOLE -> whiteHole(g, s, w, h, t, p, rev);
            case TRANSFINITE -> transfinite(g, s, w, h, t, p, rev);
            case ANTIVERSE -> antiverse(g, s, w, h, t, p, rev);
            case HYPERREAL -> hyperreal(g, s, w, h, t, p, rev);
            case LIVING_PROOF -> livingProof(g, s, w, h, t, p, rev);
            case TRANSCENDENTAL -> transcendental(g, s, w, h, t, p, rev);
            case INTERIOR -> interior(g, s, w, h, t, p, rev);
            case FRONTIER -> frontier(g, s, w, h, t, p, rev);
            case TITAN -> titan(g, s, w, h, t, p, rev);
            case INNER -> inner(g, s, w, h, t, p, rev);
            case TITAN_OCEAN -> titanOcean(g, s, w, h, t, p, rev);
            case ORBITAL_STATION -> orbit(g, s, w, h, t, p, rev);
            case HADAL_TRENCH -> hadal(g, s, w, h, t, p, rev);
            case OMNIPOTENCY -> omnipotency(g, s, w, h, t, p, rev);
        }
        g.flush();
    }

    // ================================================================== 1.3.0: the Hadal Trench
    /**
     * Down in the bathyscaphe: the blue above fades to black, marine snow streams upward past the window, a depth gauge
     * ticks round to eleven thousand metres, and far below a few cold lights wait. Coming up, all of it runs the other way.
     */
    private static void hadal(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), ep = smooth(p);
        float deep = rev ? 1 - ep : ep;
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        d.rectV(0, 0, w, h, Fx2D.lerpRgb(0x1060A0, 0x000306, deep), p, Fx2D.lerpRgb(0x063050, 0x00040A, deep), p);
        d.end();
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        int n = (int) (160 * p);                              // marine snow, streaming up past the sphere's window
        for (int i = 0; i < n; i++) {
            float x = rnd(i, 1) * w, y = h * frac(rnd(i, 2) - t * (rev ? -0.3F : 0.3F));
            f.rect(x, y, x + 1.3F, y + 1.3F, 0xD0E8F0, (0.15F + 0.35F * rnd(i, 3)) * p);
        }
        for (int i = 0; i < 6; i++) {                          // lights of the deep, below
            float x = cx + (rnd(i, 7) - 0.5F) * w * 0.8F, y = h * (0.7F + 0.25F * rnd(i, 8));
            f.disc(x, y, 2 + 3 * deep, 12, s.accent, 0.8F * deep * p * (0.6F + 0.4F * (float) Math.sin(t * 2 + i)), s.accent, 0);
        }
        // the depth gauge: a ring with a needle sweeping round to eleven kilometres
        float r = u * 0.12F;
        f.ring(cx, cy, r, 2, 64, s.ink, 0.7F * p);
        float a = (float) (-Math.PI / 2 + deep * Math.PI * 1.8);
        f.line(cx, cy, cx + (float) Math.cos(a) * r * 0.9F, cy + (float) Math.sin(a) * r * 0.9F, 2, s.accent, p);
        f.end();
    }

    // ================================================================== 1.3.0: the Orbital Station
    /**
     * Up on a column of fire: the sky darkens from blue to black, the Earth's curved limb sinks into view below, its thin
     * blue air glowing along the edge, and stars come out; the station's lights grow ahead. Leaving, the Earth swells up
     * to fill the view and the re-entry glow streams past.
     */
    private static void orbit(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, u = Math.min(w, h), ep = smooth(p);
        float up = rev ? 1 - ep : ep;                         // 0 = on the ground, 1 = in orbit
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        d.rectV(0, 0, w, h, Fx2D.lerpRgb(0x3070C0, s.bg, up), p, Fx2D.lerpRgb(0x80B0E0, 0x02040C, up), p);
        // the Earth below: a huge disc whose top edge rises as the rocket climbs
        float R = u * 2.2F, ey = h + R - h * 0.35F * up;
        d.disc(cx, ey, R, 96, 0x103A80, p, 0x2060B0, p);
        d.end();
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        f.ring(cx, ey, R * 1.004F, 6 + 10 * up, 160, 0x80C8FF, 0.8F * p * up);
        int n = (int) (180 * up * p);
        for (int i = 0; i < n; i++) {
            float x = rnd(i, 1) * w, y = rnd(i, 2) * (ey - R);
            f.rect(x, y, x + 1.2F, y + 1.2F, 0xFFFFFF, (0.3F + 0.7F * rnd(i, 3)) * p);
        }
        // the exhaust (going up) or the re-entry plasma (coming down), streaming past the centre
        int col = rev ? 0xFF8040 : 0xFFE0A0;
        for (int i = 0; i < 40; i++) {
            float x = cx + (rnd(i, 5) - 0.5F) * u * 0.12F, q = frac(rnd(i, 6) + t * 1.4F);
            float y0 = h * (rev ? 1 - q : q), len = u * 0.15F;
            f.line(x, y0, x, y0 + (rev ? -len : len), 2, col, 0.6F * p * (1 - up * (rev ? 0 : 0.7F)), col, 0);
        }
        f.end();
    }

    // ================================================================== Titan's hidden ocean
    /**
     * Melting down through the ice: pale walls rush upward past a glowing probe, its tether trailing above; then the ice
     * opens into dark water, and far below the vents glow. Leaving, the shaft runs the other way, up to the haze.
     */
    private static void titanOcean(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, u = Math.min(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        float through = rev ? 1 - ep : ep;                    // 0 = in the ice, 1 = in the ocean
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        float shaft = u * 0.18F * (1 - through * 0.9F);
        for (int i = 0; i < 12; i++) {
            float y = frac(i / 12F - t * 0.3F * dir) * h;
            d.rect(0, y, cx - shaft, y + h / 24F, 0xA0C8D8, p * 0.35F * (1 - through));
            d.rect(cx + shaft, y, w, y + h / 24F, 0xA0C8D8, p * 0.35F * (1 - through));
        }
        d.end();
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        float py = h * (0.35F + 0.15F * through);
        f.line(cx, 0, cx, py, 1, 0xD0F0FF, 0.6F * p);
        f.disc(cx, py, u * 0.02F, 16, 0xFFB060, p, 0xFF6020, 0.2F * p);
        for (int i = 0; i < 14; i++) {
            float x = rnd(i, 1) * w, y = h * (0.8F + 0.2F * rnd(i, 2));
            f.disc(x, y, u * 0.01F * (1 + rnd(i, 3)), 10, s.accent, through * p * (0.5F + 0.5F * (float) Math.sin(t * 2 + i)), s.ink, 0);
        }
        f.end();
    }

    // ================================================================== Inner Space
    /**
     * Shrinking into a body: rings rush outward from the centre as everything grows past you (a zoom of a hundred
     * thousand times), and the red cells of the bloodstream swell from specks to discs and drift by, all to the beat of a
     * heart. Leaving, the rings fall inward and the cells shrink away to nothing.
     */
    private static void inner(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        float beat = (float) Math.pow(Math.max(0, Math.sin(t * 6.0F)), 8);           // lub-dub
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        for (int i = 0; i < 9; i++) {
            float q = frac(i / 9F + t * 0.35F * dir);
            float r = u * 0.05F + q * q * u * 0.9F;
            f.ring(cx, cy, r, 2 + 6 * q, 64, i % 2 == 0 ? s.ink : s.accent, p * (1 - q) * (0.4F + 0.3F * beat));
        }
        f.end();
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        float grow = rev ? 1 - ep : ep;
        for (int i = 0; i < 26; i++) {
            float x = rnd(i, 1) * w, y = frac(rnd(i, 2) + t * (0.04F + 0.05F * rnd(i, 3))) * h;
            float r = u * (0.006F + 0.05F * grow * (0.5F + rnd(i, 4)));
            d.disc(x, y, r, 24, 0x801018, 0.9F * p, 0xE03040, 0.9F * p);
            d.disc(x, y, r * 0.45F, 16, 0x500810, 0.7F * p, 0x801018, 0.0F);     // the dimple of a biconcave disc
        }
        d.end();
    }

    // ================================================================== The Bulk
    /**
     * Branes are membranes floating in a higher-dimensional bulk. Eleven shimmering sheets (seen edge-on, so they look
     * like waving lines) peel apart from a single line - our own brane - as you leave it. Leaving the Bulk, they close
     * back in from the edges of the screen, and the waves travel the other way.
     */
    private static void bulk(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cy = h / 2F, ep = smooth(p), dir = rev ? -1 : 1;
        int n = 11, seg = 56;
        float spread = rev ? 1 + (1 - ep) * 1.2F : ep;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        float[] base = new float[n];
        for (int i = 0; i < n; i++) {
            float k = (i - (n - 1) / 2F) / ((n - 1) / 2F);          // -1 (top) .. 1 (bottom)
            base[i] = cy + k * h * 0.42F * spread;
            float amp = (5 + 10 * Math.abs(k)) * (0.35F + 0.65F * p);
            int col = Fx2D.lerpRgb(s.ink, s.accent, (k + 1) / 2);
            if (i == n / 2) col = Fx2D.lerpRgb(col, 0xFFFFFF, 0.5F);   // our own brane, brightest
            float a = p * (0.55F + 0.45F * (float) Math.sin(t * 3 + i * 1.7F));
            float px = 0, py = 0;
            for (int j = 0; j <= seg; j++) {
                float x = w * j / (float) seg;
                float y = base[i] + amp * (float) Math.sin(x * 0.018F + t * 1.6F * dir + i * 0.9F)
                        + amp * 0.45F * (float) Math.sin(x * 0.047F - t * 2.3F * dir + i);
                if (j > 0) f.glowLine(px, py, x, y, 1.3F, col, a);
                px = x;
                py = y;
            }
        }
        // gravitons (sparks) drifting through the gaps between the branes
        for (int i = 0; i < 50; i++) {
            int lane = (int) (rnd(i, 1) * (n - 1));
            float x = frac(rnd(i, 2) + t * 0.12F * (0.5F + rnd(i, 3)) * dir) * w;
            float y = base[lane] + (base[lane + 1] - base[lane]) * rnd(i, 4);
            f.disc(x, y, 2.2F, 6, 0xFFFFFF, p * 0.8F, s.accent, 0);
        }
        f.end();
    }

    // ================================================================== The Quantum Foam
    /**
     * Below the Planck length space itself boils into bubbles. Bubbles pop into existence all over the screen, swell
     * (because you are shrinking) and burst. Leaving the Foam, each bubble's life runs backwards: they shrink to points.
     */
    private static void foam(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float u = Math.min(w, h), ep = smooth(p);
        float scale = rev ? 1 + 1.5F * (1 - ep) : 0.15F + 0.85F * ep;
        int shown = (int) (80 * Math.min(1, p * 1.3F));
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        for (int i = 0; i < shown; i++) {
            float period = 1.4F + rnd(i, 1) * 1.8F;
            float ph = t / period + rnd(i, 2);
            int cyc = (int) Math.floor(ph);
            float life = ph - cyc;
            if (rev) life = 1 - life;
            float x = rnd(i * 7 + cyc, 3) * w, y = rnd(i * 13 + cyc, 4) * h;
            float rmax = (6 + rnd(i, 5) * u * 0.11F) * scale;
            if (life < 0.88F) {
                float q = life / 0.88F;
                float r = rmax * (1 - (1 - q) * (1 - q));
                float a = p * Math.min(1, q * 4);
                f.disc(x, y, r, 16, s.ink, 0.03F * a, s.ink, 0.12F * a);
                f.ring(x, y, r, 1.2F, 18, Fx2D.lerpRgb(s.ink, s.accent, q), 0.65F * a);
                f.disc(x - r * 0.35F, y - r * 0.35F, r * 0.18F, 8, 0xFFFFFF, 0.5F * a, 0xFFFFFF, 0);
            } else {
                // the pop: a fading ring and four sparks
                float q = (life - 0.88F) / 0.12F;
                float r = rmax * (1 + q * 0.6F);
                f.ring(x, y, r, 0.4F + 1.5F * (1 - q), 18, 0xFFFFFF, p * (1 - q));
                for (int k = 0; k < 4; k++) {
                    float an = k * TAU / 4 + rnd(i, 6) * TAU;
                    float c = (float) Math.cos(an), sn = (float) Math.sin(an);
                    f.line(x + c * r, y + sn * r, x + c * r * 1.3F, y + sn * r * 1.3F, 1, 0xFFFFFF, p * (1 - q));
                }
            }
        }
        f.end();
    }

    // ================================================================== The Platonic Realm
    /**
     * The realm of perfect mathematical forms: the five Platonic solids turn as golden wireframes and converge into
     * an orbit around nested regular polygons and a golden-ratio spiral. Leaving, they fly outwards from the centre.
     */
    private static void platonic(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        f.disc(cx, cy, u * 0.5F, 40, s.accent, 0.22F * p, s.accent, 0);
        // the five solids
        float orbit = u * 0.32F * (rev ? ep : 1 + 1.8F * (1 - ep));
        for (int i = 0; i < 5; i++) {
            float ang = i * TAU / 5 + t * 0.25F * dir;
            float sx = cx + (float) Math.cos(ang) * orbit * 1.25F, sy = cy + (float) Math.sin(ang) * orbit * 0.8F;
            wire(f, SOLID_V[i], SOLID_E[i], sx, sy, u * 0.085F, t * 0.7F * dir + i, t * 0.45F * dir + i * 2, s.ink, p * 0.9F);
        }
        // nested regular polygons, triangle to octagon, turning alternately
        float pr = rev ? ep : 1 + 2 * (1 - ep);
        for (int n = 3; n <= 8; n++)
            f.polygon(cx, cy, u * (0.035F + 0.024F * (n - 3)) * pr, n, t * 0.3F * dir * (n % 2 == 0 ? 1 : -1), 1F,
                    Fx2D.lerpRgb(s.ink, s.accent, (n - 3) / 5F), p * 0.8F);
        // a golden spiral: every quarter turn it grows by the golden ratio
        float b = (float) (Math.log(1.6180339887) / (Math.PI / 2));
        float px = cx, py = cy, rot = t * 0.4F * dir;
        for (int j = 1; j <= 90; j++) {
            float th = j / 90F * TAU * 2.2F;
            float r = u * 0.004F * (float) Math.exp(b * th) * (0.3F + 0.7F * ep);
            float x = cx + (float) Math.cos(th + rot) * r, y = cy + (float) Math.sin(th + rot) * r;
            f.line(px, py, x, y, 1.1F, s.accent, p * 0.6F);
            px = x;
            py = y;
        }
        f.end();
    }

    // ================================================================== Inside a black hole
    /**
     * Falling through an event horizon: light streaks stretch towards the centre (spaghettification), rings of
     * spacetime collapse inwards, and a black centre swells, ringed by a glowing orange accretion disc. Leaving the
     * black hole (which should be impossible) everything runs outward instead.
     */
    private static void blackHole(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), R = (float) Math.hypot(w, h) / 2, ep = smooth(p), dir = rev ? -1 : 1;
        float rc = u * (0.03F + 0.2F * ep);
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        int n = (int) (150 * Math.min(1, p * 1.5F));
        for (int i = 0; i < n; i++) {
            float ang = rnd(i, 1) * TAU;
            float q = frac(rnd(i, 3) + t * (0.3F + 0.5F * rnd(i, 2)) * dir);     // 0 at the edge -> 1 at the horizon
            float r = rc + (R - rc) * (1 - q * q);
            float len = (8 + 70 * q) * (0.5F + p);                             // stretched as it falls
            float c = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            f.line(cx + c * r, cy + sn * r, cx + c * (r + len), cy + sn * (r + len), 1 + q * 1.5F, s.accent, p * 0.8F * Math.min(1, q * 3), s.ink, 0);
        }
        for (int i = 0; i < 7; i++) {
            float q = frac(i / 7F + t * 0.3F * dir);
            f.ring(cx, cy, rc + (R * 0.9F - rc) * (1 - q) * (1 - q), 1.2F + q * 2, 64, s.ink, p * 0.35F * q);
        }
        accretion(f, s, cx, cy, rc, t, p, true);
        f.end();
        // the hole itself is drawn with ordinary (not glowing) black paint
        float hole = Math.min(1, p * 2);
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        d.disc(cx, cy, rc, 48, 0, hole, 0, hole);
        d.band(cx, cy, rc, rc * 1.35F, 1, 0, TAU, 48, 0, hole, 0, 0);
        d.end();
        f = Fx2D.begin(g, Blend.ADD);
        f.ring(cx, cy, rc * 1.04F, 1.5F, 64, s.accent, p * 0.9F);                     // the photon ring
        f.band(cx, cy, rc * 1.04F, rc * 1.6F, 1, 0, TAU, 64, s.ink, 0.25F * p, s.ink, 0); // light bent round the back
        accretion(f, s, cx, cy, rc, t, p, false);
        f.end();
    }

    /** The accretion disc seen almost edge-on; the far half is drawn before the hole and the near half after it. */
    private static void accretion(Fx2D f, TransitStyle s, float cx, float cy, float rc, float t, float p, boolean far) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            float a0 = (far ? (float) Math.PI : 0) + (float) Math.PI * i / seg;
            float mid = a0 + (float) Math.PI / seg / 2;
            // the side turning towards us is brighter (Doppler beaming), with hot streaks swirling round
            float bright = 0.55F + 0.45F * (float) Math.cos(mid) + 0.15F * (float) Math.sin(mid * 6 - t * 4);
            f.band(cx, cy, rc * 1.2F, rc * 2.5F, 0.26F, a0, (float) Math.PI / seg, 1, s.accent, 0.9F * p * bright, s.ink, 0);
        }
    }

    // ================================================================== Inside a white hole
    /**
     * A white hole is a black hole run backwards: nothing can get in, everything is thrown out. Rings and debris burst
     * outwards from a blazing core until the screen floods with white. Leaving it, they implode instead.
     */
    private static void whiteHole(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), R = (float) Math.hypot(w, h) / 2, ep = smooth(p), dir = rev ? -1 : 1;
        float flash = whiteFlash(p);
        int ink = Fx2D.lerpRgb(0xFFFFFF, 0x5A78A8, flash), gold = Fx2D.lerpRgb(s.accent, 0xB08040, flash);
        Fx2D f = Fx2D.begin(g, Blend.NORMAL);
        f.disc(cx, cy, u * (0.05F + 0.15F * ep), 40, 0xFFFFFF, p, gold, 0);
        for (int i = 0; i < 9; i++) {
            float q = frac(i / 9F + t * 0.5F * dir);
            f.ring(cx, cy, u * 0.04F + R * (float) Math.pow(q, 0.7), 1 + q * 7, 72, Fx2D.lerpRgb(ink, gold, q), p * (1 - q) * 0.8F);
        }
        int n = (int) (110 * Math.min(1, p * 1.5F));
        for (int i = 0; i < n; i++) {
            float ang = rnd(i, 1) * TAU;
            float q = frac(rnd(i, 2) + t * (0.35F + 0.4F * rnd(i, 3)) * dir);
            float r = u * 0.05F + R * (float) Math.pow(q, 0.8), len = 6 + 40 * (1 - q);
            float c = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            f.line(cx + c * r, cy + sn * r, cx + c * (r + len), cy + sn * (r + len), 1.2F, ink, p * (1 - q), ink, 0);
        }
        // the flash itself peaks just before the jump
        float bump = Math.max(0, 1 - Math.abs(p - 0.85F) / 0.15F);
        if (bump > 0) f.rect(0, 0, w, h, 0xFFFFFF, 0.6F * bump);
        f.end();
    }

    private static float whiteFlash(float p) {
        return smooth(clamp((p - 0.5F) / 0.5F));
    }

    // ================================================================== The Planetary Interior
    /**
     * Down through the Earth: the layers of the crust and mantle - soil, granite, basalt, peridotite - rush up past
     * you as bands, each warmer in colour than the last, while the glow of the molten outer core rises from below and
     * sparks of magma stream upward. Leaving, the bands fall away downward and the glow sinks out of sight.
     */
    private static void interior(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float ep = smooth(p), dir = rev ? -1 : 1;
        int[] strata = {0x4A3424, 0x6A6460, 0x2E2A2C, 0x3E5A34, 0x6A3018, 0x8A3810};
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        float bandH = h / 3.2F, scroll = t * (0.6F + 1.6F * ep) * dir;
        for (int i = -1; i < 6; i++) {
            float y0 = (i - frac(scroll)) * bandH;
            int k = Math.floorMod(i + (int) Math.floor(scroll), strata.length);
            d.rect(0, y0, w, y0 + bandH, strata[k], 0.85F * p);
            // a wavy contact between layers
            for (int x = 0; x < w; x += 8) {
                float wob = 4 * (float) Math.sin(x * 0.05F + k * 1.7F);
                d.rect(x, y0 + wob - 1.5F, x + 8, y0 + wob + 1.5F, 0x140806, 0.6F * p);
            }
        }
        d.end();
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        // the core's glow: coming up from below when descending, sinking away when rising
        float glow = rev ? 1 - ep : ep;
        f.rectV(0, h * (1 - 0.8F * glow), w, h, s.ink, 0, s.accent, 0.85F * p * glow);
        int n = (int) (90 * Math.min(1, p * 1.5F));
        for (int i = 0; i < n; i++) {
            float x = rnd(i, 1) * w, q = frac(rnd(i, 2) + t * (0.4F + 0.6F * rnd(i, 3)) * dir);
            float y = h * (1 - q), len = 6 + 20 * ep;
            f.line(x, y, x, y + len * dir, 1.4F, s.accent, p * 0.8F * (1 - q), s.ink, 0);
        }
        f.end();
    }

    // ================================================================== The Accretion Frontier
    /**
     * Out past the heliopause on a light sail: stars stream past in lengthening streaks (relativistic aberration
     * crowding them towards the direction of travel), while far ahead a black hole grows - a black disc ringed by its
     * photon ring and a tilted, Doppler-bright accretion disk. Leaving, the streaks run backwards and the hole shrinks
     * into the distance behind you.
     */
    private static void frontier(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), R = (float) Math.hypot(w, h) / 2, ep = smooth(p), dir = rev ? -1 : 1;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        int n = (int) (220 * Math.min(1, p * 1.4F));
        for (int i = 0; i < n; i++) {
            float ang = rnd(i, 1) * TAU, q = frac(rnd(i, 2) + t * (0.15F + 0.5F * ep) * dir);
            float r = R * q * q, len = 2 + 60 * ep * q;
            float c = (float) Math.cos(ang), sn = (float) Math.sin(ang);
            int col = rnd(i, 4) < 0.2F ? s.accent : (rnd(i, 4) < 0.6F ? s.ink : 0xFFFFFF);
            f.line(cx + c * r, cy + sn * r, cx + c * (r + len), cy + sn * (r + len), 1 + q, col, p * 0.9F * Math.min(1, q * 4), col, 0);
        }
        float size = rev ? 1 - ep : ep;
        float rc = u * (0.02F + 0.12F * size);
        disk(f, s, cx, cy, rc, t, p, true);
        f.end();
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        d.disc(cx, cy, rc, 48, 0, p, 0, p);
        d.end();
        f = Fx2D.begin(g, Blend.ADD);
        f.ring(cx, cy, rc * 1.06F, 1.6F, 64, 0xFFE8C0, p * 0.9F);
        f.band(cx, cy, rc * 1.06F, rc * 1.5F, 1, 0, TAU, 64, s.accent, 0.3F * p, s.accent, 0);
        disk(f, s, cx, cy, rc, t, p, false);
        // the jets, straight up and down out of the poles
        f.line(cx, cy - rc, cx, cy - rc * 5, 2.2F, s.ink, 0.7F * p, s.ink, 0);
        f.line(cx, cy + rc, cx, cy + rc * 5, 2.2F, s.ink, 0.7F * p, s.ink, 0);
        f.end();
    }

    private static void disk(Fx2D f, TransitStyle s, float cx, float cy, float rc, float t, float p, boolean far) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            float a0 = (far ? (float) Math.PI : 0) + (float) Math.PI * i / seg, mid = a0 + (float) Math.PI / seg / 2;
            float bright = 0.5F + 0.5F * (float) Math.cos(mid) + 0.2F * (float) Math.sin(mid * 5 - t * 3);
            f.band(cx, cy, rc * 1.3F, rc * 3.2F, 0.3F, a0, (float) Math.PI / seg, 1, s.accent, 0.85F * p * bright, 0xFF4020, 0);
        }
    }

    // ================================================================== Titan
    /**
     * Down through Titan's haze under a parachute: layers of orange smog (tholins, made by sunlight from methane) rise
     * past you, thicker and darker the lower you go, while Saturn's pale crescent and rings fade out overhead. Near the
     * ground the methane drizzle begins, in slow streaks - it falls at a walking pace here. The capsule swings under its
     * canopy at the centre. Leaving, the haze falls away below, the rain stops, and Saturn grows clear.
     */
    private static void titan(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, u = Math.min(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        float depth = rev ? 1 - ep : ep;                      // 0 = above the haze, 1 = at the surface
        // Saturn: a crescent with its rings, high up and fading as the haze closes over it
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        float sx = w * 0.72F, sy = h * (0.18F - 0.12F * depth), sr = u * 0.11F, sa = p * (1 - 0.85F * depth);
        f.disc(sx, sy, sr, 48, 0xFFE8B0, 0.55F * sa, 0xC09050, 0.25F * sa);
        f.band(sx, sy, sr * 1.35F, sr * 2.2F, 0.22F, 0, TAU, 64, 0xFFF0D0, 0.35F * sa, 0xA08060, 0.1F * sa);
        f.end();
        Fx2D d = Fx2D.begin(g, Blend.NORMAL);
        // Saturn's night side, so it reads as a crescent
        d.disc(sx + sr * 0.35F, sy + sr * 0.1F, sr * 0.92F, 48, s.bg, 0.8F * sa, s.bg, 0.8F * sa);
        // haze layers rising past
        float bandH = h / 5F, scroll = t * (0.25F + 0.9F * ep) * dir;
        for (int i = -1; i < 7; i++) {
            float y0 = (i - frac(scroll)) * bandH;
            int k = Math.floorMod(i + (int) Math.floor(scroll), 5);
            int col = Fx2D.lerpRgb(0xC07830, 0x5A3010, (y0 / h) * 0.6F + 0.4F * depth);
            float a = p * (0.25F + 0.35F * depth) * (0.6F + 0.4F * (float) Math.sin(k * 2.1F + t * 0.3F));
            d.rectV(0, y0, w, y0 + bandH, col, 0, col, a);
            d.rectV(0, y0 + bandH * 0.5F, w, y0 + bandH, col, a, col, 0);
        }
        d.end();
        // the capsule under its canopy, swinging
        f = Fx2D.begin(g, Blend.NORMAL);
        float sw = 0.12F * (float) Math.sin(t * 1.3F), py = h * 0.52F + 6 * (float) Math.sin(t * 0.9F);
        float cr = u * 0.07F, ly = py - u * 0.16F, lx = cx + sw * u * 0.16F;
        f.band(lx, ly, cr * 0.2F, cr, 0.55F, (float) Math.PI, (float) Math.PI, 24, 0xF0E0C8, 0.9F * p, 0xD06020, 0.9F * p);
        f.line(lx - cr, ly, cx - 4, py - 5, 1, 0x302010, 0.8F * p);
        f.line(lx + cr, ly, cx + 4, py - 5, 1, 0x302010, 0.8F * p);
        f.tri(cx, py - 7, 0xD0C8B8, p, cx - 7, py + 6, 0x807060, p, cx + 7, py + 6, 0x807060, p);
        f.end();
        // methane drizzle near the ground: slow, fat streaks
        f = Fx2D.begin(g, Blend.ADD);
        int n = (int) (160 * clamp((depth - 0.35F) / 0.65F) * p);
        for (int i = 0; i < n; i++) {
            float x = rnd(i, 1) * w, q = frac(rnd(i, 2) + t * (0.12F + 0.1F * rnd(i, 3)));
            float y = h * q, len = 5 + 6 * rnd(i, 4);
            f.line(x, y, x - 1, y + len, 1.2F, s.accent, 0.35F * p, s.accent, 0);
        }
        f.end();
    }

    // ================================================================== The Transfinite Hierarchy
    /**
     * The Cantor set: take a bar, remove its middle third, repeat forever. Rows of ever finer bars scroll upward while
     * the view zooms into the left third - which looks exactly like the whole, so the descent never ends. Names of
     * infinities (omega, aleph...) drift past. Leaving, the zoom runs outward and the bars fall away.
     */
    private static void transfinite(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cy = h / 2F, W = w * 0.8F, x0 = (w - W) / 2, sp = h * 0.085F, bh = h * 0.032F;
        float fz = frac(t * 0.16F);
        if (rev) fz = 1 - fz;
        float z = (float) Math.pow(3, fz), top = cy - sp * 4;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        for (int k = 0; k <= 9; k++) {
            float y = top + (k - fz) * sp;
            float pos = (y - top) / sp;                          // -1 .. 9: where on screen this row is
            float edge = clamp(Math.min(pos + 1, 9 - pos));        // fade out at the top and bottom
            float build = clamp(p * 11 - (rev ? 9 - pos : pos));   // rows appear one by one as the animation builds
            float a = p * edge * build;
            if (a <= 0.01F) continue;
            int col = Fx2D.lerpRgb(s.ink, s.accent, k / 9F);
            int count = 1 << k;
            double len = Math.pow(3, -k);
            for (int i = 0; i < count; i++) {
                double start = 0, unit = 1;
                for (int b = k - 1; b >= 0; b--) {             // each binary digit of i picks the left or right third
                    unit /= 3;
                    if (((i >> b) & 1) == 1) start += 2 * unit;
                }
                float xs = x0 + (float) (start * z * W);
                if (xs > x0 + W) break;
                float xe = Math.min(x0 + W, x0 + (float) ((start + len) * z * W));
                if (xe - xs < 0.6F) xe = xs + 0.6F;
                f.rectV(xs, y, xe, y + bh, col, a * 0.9F, col, a * 0.3F);
            }
        }
        f.end();
        Font font = Minecraft.getInstance().font;
        String[] glyphs = {"ω", "ω+1", "ω·2", "ω²", "ω^ω", "ε₀", "ℵ₀", "ℵ₁", "ℵ_ω", "ℶ₁", "2^ℵ₀", "κ", "Ω", "∞"};
        g.drawManaged(() -> {
        for (int i = 0; i < 24; i++) {
            float x = rnd(i, 1) * w;
            float y = mod(rnd(i, 2) * h - t * 18 * (rev ? -1 : 1) * (0.5F + rnd(i, 3)), h + 40) - 20;
            text(g, font, glyphs[i % glyphs.length], x, y, 1 + rnd(i, 4) * 1.6F, s.accent, p * 0.55F * (0.5F + 0.5F * (float) Math.sin(t + i)));
        }
        });
    }

    // ================================================================== The Antiverse
    /**
     * The Antiverse is our universe in a mirror (charge, parity and time all flipped). A glowing seam splits the
     * screen; particle tracks spiral away from it like matter/antimatter pairs in a bubble chamber, and everything on
     * one side of the seam turns into its photographic negative. Leaving, the other side inverts and the tracks wind
     * back in (annihilating).
     */
    private static void antiverse(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, u = Math.min(w, h), ep = smooth(p);
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        int n = (int) Math.ceil(16 * Math.min(1, p * 1.4F));
        for (int i = 0; i < n; i++) {
            float y0 = h * (i + 0.5F) / 16 + (rnd(i, 1) - 0.5F) * 10;
            float life = frac(t / (1.8F + rnd(i, 2)) + rnd(i, 3));
            if (rev) life = 1 - life;
            float smax = life * 9, fade = 1 - life;
            float r0 = u * (0.05F + 0.08F * rnd(i, 4)), b = 0.18F + 0.1F * rnd(i, 5);
            boolean up = rnd(i, 6) < 0.5F;                        // curls up or down
            float ccx = cx - r0, ccy = y0;                         // the spiral's centre, so it starts on the seam
            float px = cx, py = y0;
            for (int j = 1; j <= 36; j++) {
                float sv = smax * j / 36;
                float r = r0 * (float) Math.exp(-b * sv);
                float x = ccx + (float) Math.cos(sv) * r, y = ccy + (float) Math.sin(sv) * r * (up ? -1 : 1);
                float a = p * fade * 0.9F;
                f.line(px, py, x, y, 1.2F, s.ink, a);                       // the particle
                f.line(2 * cx - px, py, 2 * cx - x, y, 1.2F, s.ink, a);     // its mirror twin
                px = x;
                py = y;
            }
        }
        f.end();
        // the inversion sweeps from the seam to one edge (right when entering, left when leaving: a mirror image)
        float ex = w / 2F * ep;
        int grey = (int) (255 * Math.min(1, p * 2.5F));
        Fx2D inv = Fx2D.begin(g, Blend.INVERT);
        if (rev) inv.rect(cx - ex, 0, cx, h, (grey << 16) | (grey << 8) | grey, 1);
        else inv.rect(cx, 0, cx + ex, h, (grey << 16) | (grey << 8) | grey, 1);
        inv.end();
        f = Fx2D.begin(g, Blend.ADD);
        float edgeX = rev ? cx - ex : cx + ex;
        f.rectH(edgeX - 8, 0, edgeX, h, s.ink, 0, s.ink, 0.5F * p);
        f.rectH(edgeX, 0, edgeX + 8, h, s.accent, 0.5F * p, s.accent, 0);
        f.glowLine(cx, h / 2F - h * 0.5F * ep, cx, h / 2F + h * 0.5F * ep, 1.5F, 0xFFFFFF, p);
        f.end();
    }

    // ================================================================== Hyperdimensional Reality
    /**
     * Space with more dimensions than ours: tesseracts (four-dimensional cubes) rotate through the fourth dimension,
     * so their inner and outer cubes keep turning inside out. Leaving, they turn the other way and shrink back.
     */
    private static void hyperreal(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        f.disc(cx, cy, u * 0.55F, 40, s.ink, 0.18F * p, s.ink, 0);
        float S = u * 0.3F * (rev ? 1 + 0.8F * (1 - ep) : 0.25F + 0.75F * ep);
        tesseract(f, cx, cy, S, t * dir, 0, s.ink, p);
        tesseract(f, cx, cy, S * 0.5F, -t * dir * 1.3F, 1.7F, s.accent, p * 0.6F);
        tesseract(f, cx, cy, S * 1.9F, t * dir * 0.6F, 3.1F, s.accent, p * 0.3F);
        for (int i = 0; i < 60; i++) {
            float tw = 0.5F + 0.5F * (float) Math.sin(t * 2 + i * 2.1F);
            f.disc(rnd(i, 1) * w, rnd(i, 2) * h, 1.6F, 6, 0xFFFFFF, p * 0.6F * tw, s.accent, 0);
        }
        f.end();
    }

    /** A rotating 4D cube: its 16 corners are turned in 4D, then projected to 3D and then to the screen. */
    public static void tesseract(Fx2D f, float cx, float cy, float size, float time, float phase, int rgb, float alpha) {
        float[][] pts = new float[16][3];
        float a = time * 0.7F + phase, b = time * 0.45F + phase * 0.5F, c = time * 0.2F;
        for (int v = 0; v < 16; v++) {
            float x = (v & 1) == 0 ? -1 : 1, y = (v & 2) == 0 ? -1 : 1, z = (v & 4) == 0 ? -1 : 1, ww = (v & 8) == 0 ? -1 : 1;
            float x1 = x * cos(a) - ww * sin(a), w1 = x * sin(a) + ww * cos(a);       // turn in the x-w plane
            float z1 = z * cos(b) - w1 * sin(b), w2 = z * sin(b) + w1 * cos(b);       // and the z-w plane
            float x2 = x1 * cos(c) - y * sin(c), y2 = x1 * sin(c) + y * cos(c);       // and an ordinary x-y turn
            float k = 1.6F / (2.4F - w2 * 0.6F);                                       // 4D -> 3D perspective
            float X = x2 * k, Y = y2 * k, Z = z1 * k;
            float Y3 = Y * cos(0.45F) - Z * sin(0.45F), Z3 = Y * sin(0.45F) + Z * cos(0.45F);   // tilt so we see it from above
            float k2 = 3F / (3F + Z3);                                                 // 3D -> screen perspective
            pts[v][0] = cx + X * k2 * size;
            pts[v][1] = cy + Y3 * k2 * size;
            pts[v][2] = k2;
        }
        for (int v = 0; v < 16; v++)
            for (int bit = 1; bit < 16; bit <<= 1) {
                int o = v | bit;
                if (o == v) continue;                    // each edge joins two corners differing in one coordinate
                float depth = clamp((pts[v][2] + pts[o][2]) / 2 - 0.4F);
                f.glowLine(pts[v][0], pts[v][1], pts[o][0], pts[o][1], 1.2F, rgb, alpha * (0.4F + 0.6F * depth));
            }
        for (int v = 0; v < 16; v++) f.disc(pts[v][0], pts[v][1], 2.2F, 8, 0xFFFFFF, alpha, rgb, 0);
    }

    // ================================================================== The Living Proof
    /**
     * Alpha Omega's mind is a proof that proves itself: columns of logical symbols rain down the screen, around a
     * great turnstile (the "therefore it is proved" sign). Leaving, the columns rise instead.
     */
    private static void livingProof(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        f.rectV(0, 0, w, h, s.ink, 0, s.ink, 0.1F * p);
        f.end();
        Font font = Minecraft.getInstance().font;
        int cols = Math.max(1, w / 9), rows = h / 10 + 2, trail = 16;
        // "managed" = queue all the symbols and send them to the graphics card together, not one at a time
        g.drawManaged(() -> {
        for (int c = 0; c < cols; c++) {
            if (rnd(c, 1) > p * 1.15F) continue;              // columns join in as the animation builds
            float v = 7 + rnd(c, 2) * 12;
            float head = mod(t * v + rnd(c, 3) * rows * 2, rows + trail);
            for (int j = 0; j < trail; j++) {
                float row = head - j;
                if (row < 0 || row > rows) continue;
                int y = (int) (rev ? h - row * 10 : row * 10 - 10);
                int sym = (int) (rnd(c * 131 + (int) row, (int) (t * 3 + rnd(c, 4) * 10)) * PROOF_SYMBOLS.length);
                float a = p * (1 - j / (float) trail);
                int al = (int) (a * 255);
                if (al < 8) continue;
                g.drawString(font, PROOF_SYMBOLS[Math.min(sym, PROOF_SYMBOLS.length - 1)], c * 9, y, (al << 24) | (j == 0 ? s.accent : s.ink), false);
            }
        }
        });
        text(g, font, "⊢", w / 2F, h / 2F, 7, s.accent, p * 0.25F);
    }

    private static final String[] PROOF_SYMBOLS = {"∀", "∃", "∑", "∫", "⊢", "∴", "∵", "≡", "⇒", "¬", "∧", "∨", "∈", "∅", "∞", "λ", "π", "∂", "∇", "⊨", "□", "◇"};

    // ================================================================== The Transcendental
    /**
     * Ascension: rays of white and gold light fan out from a centre that grows until it fills your sight, rings of
     * light expand, and motes of light rise past you. Leaving, the motes fall and the rays turn the other way.
     */
    private static void transcendental(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h / 2F, u = Math.min(w, h), R = (float) Math.hypot(w, h) / 2, ep = smooth(p), dir = rev ? -1 : 1;
        Fx2D f = Fx2D.begin(g, Blend.ADD);
        int n = 44;
        for (int i = 0; i < n; i++) {
            float ang = i * TAU / n + t * 0.06F * dir + 0.05F * (float) Math.sin(t * 0.7F + i);
            float len = R * (0.55F + 0.45F * rnd(i, 1)) * (0.3F + 0.7F * ep), half = 0.028F + 0.02F * rnd(i, 2);
            int col = i % 2 == 0 ? s.accent : s.ink;
            float a = p * (0.25F + 0.2F * (float) Math.sin(t * 2 + i * 1.3F));
            f.tri(cx, cy, col, a, cx + cos(ang - half) * len, cy + sin(ang - half) * len, col, 0, cx + cos(ang + half) * len, cy + sin(ang + half) * len, col, 0);
        }
        f.disc(cx, cy, u * (0.06F + 0.5F * ep * ep), 48, 0xFFFFFF, p * 0.9F, s.ink, 0);
        f.disc(cx, cy, u * (0.03F + 0.06F * ep), 32, 0xFFFFFF, p, 0xFFFFFF, p * 0.6F);
        for (int i = 0; i < 3; i++) {
            float q = frac(i / 3F + t * 0.2F * dir);
            f.ring(cx, cy, u * 0.1F + q * R * 0.8F, 1.5F, 72, s.ink, p * 0.4F * (1 - q));
        }
        for (int i = 0; i < 80; i++) {
            float x = rnd(i, 3) * w + (float) Math.sin(t + i) * 6;
            float yq = frac(rnd(i, 4) + t * (0.08F + 0.1F * rnd(i, 5)));
            float y = rev ? yq * h * 1.1F - h * 0.05F : h * 1.05F - yq * h * 1.1F;
            f.disc(x, y, (0.8F + 1.6F * rnd(i, 6)) * 2, 6, 0xFFFFFF, p * 0.8F, s.ink, 0);
        }
        f.end();
    }

    /** 1.8.0, Omnipotency: a light at the top of the page, slow gold rays coming down from it, three halos, and motes rising. */
    private static void omnipotency(GuiGraphics g, TransitStyle s, int w, int h, float t, float p, boolean rev) {
        float cx = w / 2F, cy = h * 0.3F, u = Math.min(w, h), R = (float) Math.hypot(w, h), ep = smooth(p), dir = rev ? -1 : 1;
        Fx2D f = Fx2D.begin(g, Blend.NORMAL);
        int n = 28;
        for (int i = 0; i < n; i++) {
            float ang = i * TAU / n + t * 0.03F * dir, half = 0.03F + 0.025F * rnd(i, 2);
            float len = R * (0.5F + 0.5F * rnd(i, 1)) * (0.25F + 0.75F * ep);
            float a = p * (0.22F + 0.12F * (float) Math.sin(t * 1.3F + i * 1.7F));
            f.tri(cx, cy, s.ink, a, cx + cos(ang - half) * len, cy + sin(ang - half) * len, s.ink, 0, cx + cos(ang + half) * len, cy + sin(ang + half) * len, s.ink, 0);
        }
        for (int i = 0; i < 3; i++) {
            float r = u * (0.12F + 0.1F * i) * (0.6F + 0.4F * ep);
            f.ring(cx, cy, r, 1.6F, 96, i == 1 ? 0xFFFFFF : s.ink, p * (0.75F - 0.15F * i));
            int marks = 12 * (i + 1);
            for (int k = 0; k < marks; k++) {
                float a0 = k * TAU / marks + t * (i % 2 == 0 ? 0.05F : -0.07F) * dir;
                f.disc(cx + cos(a0) * r, cy + sin(a0) * r, 1.3F, 6, s.ink, p * 0.8F, s.ink, 0);
            }
        }
        f.disc(cx, cy, u * (0.05F + 0.1F * ep), 48, 0xFFFFFF, p, s.bg, 0);
        for (int i = 0; i < 70; i++) {
            float x = rnd(i, 3) * w + (float) Math.sin(t * 0.6F + i) * 5;
            float yq = frac(rnd(i, 4) + t * (0.03F + 0.05F * rnd(i, 5)));
            float y = rev ? yq * h * 1.1F - h * 0.05F : h * 1.05F - yq * h * 1.1F;
            f.disc(x, y, 1.2F + 1.6F * rnd(i, 6), 6, s.ink, p * 0.7F, s.ink, 0);
        }
        f.end();
    }

    // ================================================================== the loading page's progress element

    /**
     * A small animated "still working" element under the dimension's name, in its style. {@code done} is a rough
     * 0..1 progress (the game does not say how far loading is, so it creeps towards 1 while the page is up).
     */
    public static void progress(GuiGraphics g, TransitStyle s, float cx, float cy, float t, float done) {
        float half = 70;
        int ink = s.bright ? 0x3A5A90 : s.ink, accent = s.bright ? 0xB08040 : s.accent;
        Fx2D f = Fx2D.begin(g, s.bright ? Blend.NORMAL : Blend.ADD);
        // the plain bar every page shares, just under the element
        f.rect(cx - half, cy + 14, cx + half, cy + 15, ink, 0.25F);
        f.rect(cx - half, cy + 14, cx - half + 2 * half * done, cy + 15, accent, 0.9F);
        switch (s) {
            case BULK -> {
                float px = cx - half, py = cy;
                for (int j = 1; j <= 40; j++) {
                    float x = cx - half + 2 * half * j / 40F, y = cy + 5 * (float) Math.sin(j * 0.5F - t * 5);
                    f.glowLine(px, py, x, y, 1.2F, j / 40F <= done ? accent : ink, j / 40F <= done ? 0.9F : 0.35F);
                    px = x;
                    py = y;
                }
            }
            case FOAM -> {
                for (int i = 0; i < 8; i++) {
                    float x = cx - half + 2 * half * (i + 0.5F) / 8, grow = clamp(done * 8 - i);
                    float pulse = grow >= 1 ? 1 + 0.12F * (float) Math.sin(t * 6 + i) : grow;
                    f.ring(x, cy, 1 + 5 * pulse, 1.1F, 16, ink, 0.4F + 0.5F * grow);
                    f.disc(x - 2 * pulse, cy - 2 * pulse, 1.2F * pulse, 6, 0xFFFFFF, 0.7F * grow, 0xFFFFFF, 0);
                }
            }
            case PLATONIC -> f.polygon(cx, cy, 10, 3 + Math.min(9, (int) (done * 10)), t * 1.2F, 1.2F, ink, 0.9F);
            case BLACK_HOLE -> {
                f.ring(cx, cy, 10, 1.2F, 32, ink, 0.6F);
                f.band(cx, cy, 11, 13, 1, -(float) Math.PI / 2, TAU * done, 32, accent, 0.9F, accent, 0.9F);
                float q = frac(t * 0.5F), r = 10 * (1 - q);
                f.disc(cx + cos(t * 6) * r, cy + sin(t * 6) * r, 1.6F, 6, 0xFFFFFF, 1 - q, accent, 0);
            }
            case WHITE_HOLE -> {
                for (int i = 0; i < 3; i++) {
                    float q = frac(t * 0.8F + i / 3F);
                    f.ring(cx, cy, 2 + 12 * q, 1.2F, 32, ink, 1 - q);
                }
                f.band(cx, cy, 13, 14.5F, 1, -(float) Math.PI / 2, TAU * done, 32, accent, 0.9F, accent, 0.9F);
            }
            case TRANSFINITE -> {
                int depth = Math.min(6, (int) (done * 7));
                int count = 1 << depth;
                for (int i = 0; i < count; i++) {
                    double start = 0, unit = 1;
                    for (int b = depth - 1; b >= 0; b--) {
                        unit /= 3;
                        if (((i >> b) & 1) == 1) start += 2 * unit;
                    }
                    float x0 = cx - half + (float) (start * 2 * half), x1 = x0 + (float) (Math.pow(3, -depth) * 2 * half);
                    f.rect(x0, cy - 3, Math.max(x1, x0 + 0.6F), cy + 3, ink, 0.6F + 0.3F * (float) Math.sin(t * 3 + i));
                }
            }
            case ANTIVERSE -> {
                float x = half * (float) Math.sin(t * 2);
                f.disc(cx - x, cy, 3, 10, ink, 1, ink, 0);
                f.disc(cx + x, cy, 3, 10, accent, 1, accent, 0);
                f.glowLine(cx, cy - 7, cx, cy + 7, 1, 0xFFFFFF, 0.8F);
            }
            case HYPERREAL -> tesseract(f, cx, cy, 10, t * 1.5F, 0, ink, 0.9F);
            case INTERIOR -> {
                // a drill bit going down through the bands
                for (int i = 0; i < 5; i++) f.rect(cx - half + i * 28, cy - 4, cx - half + i * 28 + 26, cy + 4, i <= done * 5 ? accent : ink, 0.3F + 0.1F * i);
                float x = cx - half + 2 * half * done;
                f.tri(x, cy + 7, accent, 1, x - 4, cy - 5, ink, 0.8F, x + 4, cy - 5, ink, 0.8F);
            }
            case OMNIPOTENCY -> {
                // twelve keystones in a ring, set one by one (1.8.0)
                for (int i = 0; i < 12; i++) {
                    float a0 = i * TAU / 12 - TAU / 4;
                    boolean set = (i + 0.5F) / 12F <= done;
                    f.disc(cx + cos(a0) * 10, cy + sin(a0) * 10, set ? 2.2F : 1.4F, 8, set ? accent : ink, set ? 0.95F : 0.4F, set ? accent : ink, set ? 0.5F : 0.2F);
                }
            }
            case HADAL_TRENCH -> {
                // a sphere sinking down a line (1.3.0)
                f.rect(cx - half, cy - 1, cx + half, cy + 1, ink, 0.3F);
                float x = cx - half + 2 * half * done;
                f.disc(x, cy, 4, 12, accent, 0.9F, ink, 0.6F);
            }
            case ORBITAL_STATION -> {
                // a capsule climbing to a station's ring (1.3.0)
                f.ring(cx + half, cy, 6, 1.4F, 24, accent, 0.8F);
                float x = cx - half + 2 * half * done;
                f.tri(x + 6, cy, 0xFFFFFF, 0.9F, x - 4, cy - 4, ink, 0.8F, x - 4, cy + 4, ink, 0.8F);
            }
            case TITAN_OCEAN -> {
                // a probe melting down through a band of ice
                f.rect(cx - half, cy - 3, cx + half, cy + 3, ink, 0.3F);
                float x = cx - half + 2 * half * done;
                f.disc(x, cy, 2.5F, 10, accent, 1, accent, 0.4F);
            }
            case INNER -> {
                // a lens shrinking a figure to a speck
                float r = 8 * (1 - done) + 1.5F;
                f.ring(cx, cy, 9, 1.2F, 24, accent, 0.8F);
                f.disc(cx, cy, r, 12, ink, 1, ink, 0.4F);
            }
            case TITAN -> {
                // a parachute coming down to a sea
                f.rect(cx - half, cy + 6, cx + half, cy + 8, accent, 0.5F);
                float x = cx - half + 2 * half * done, y = cy - 6 + 10 * done;
                f.band(x, y - 4, 1, 5, 0.6F, (float) Math.PI, (float) Math.PI, 12, 0xFFFFFF, 0.9F, ink, 0.9F);
                f.disc(x, y + 2, 1.6F, 6, ink, 1, ink, 0.6F);
            }
            case FRONTIER -> {
                // a sail on its way out to a hole
                f.ring(cx + half, cy, 5, 1.2F, 24, accent, 0.8F);
                float x = cx - half + 2 * half * done;
                f.tri(x + 5, cy, 0xFFFFFF, 0.9F, x - 4, cy - 5, ink, 0.8F, x - 4, cy + 5, ink, 0.8F);
            }
            case LIVING_PROOF -> {} // drawn as text below
            case TRANSCENDENTAL -> {
                f.disc(cx, cy, 6 + 3 * done, 20, 0xFFFFFF, 0.9F, ink, 0);
                for (int i = 0; i < 12; i++) {
                    float a = i * TAU / 12 + t * 0.5F, len = 8 + 10 * done + 2 * (float) Math.sin(t * 4 + i);
                    f.line(cx + cos(a) * 8, cy + sin(a) * 8, cx + cos(a) * len, cy + sin(a) * len, 1.2F, ink, 0.8F, ink, 0);
                }
            }
        }
        f.end();
        if (s == TransitStyle.LIVING_PROOF) {
            String line = "∀ε>0 ∃δ>0 : |x-a|<δ ⇒ |f(x)-L|<ε  ∎";
            int shown = Math.min(line.length(), (int) (done * line.length() * 1.05F));
            String txt = line.substring(0, shown) + ((int) (t * 3) % 2 == 0 ? "_" : " ");
            Font font = Minecraft.getInstance().font;
            g.drawString(font, txt, (int) (cx - font.width(line) / 2F), (int) cy - 4, 0xFF000000 | s.ink, false);
        }
        g.flush();
    }

    // ================================================================== the Platonic solids' wireframes

    static final float[][][] SOLID_V = new float[5][][];
    static final int[][][] SOLID_E = new int[5][][];

    static {
        float phi = 1.6180339887F, ip = 1 / phi;
        float[][][] raw = {
                {{1, 1, 1}, {1, -1, -1}, {-1, 1, -1}, {-1, -1, 1}},                                      // tetrahedron
                cube(),                                                                                  // cube
                {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}},                   // octahedron
                concat(cube(), cyc(0, ip, phi)),                                                         // dodecahedron
                cyc(0, 1, phi)                                                                           // icosahedron
        };
        for (int s = 0; s < 5; s++) {
            float[][] v = raw[s];
            for (float[] q : v) {                    // scale every solid to the same size
                float l = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2]);
                for (int k = 0; k < 3; k++) q[k] /= l;
            }
            // the edges are exactly the pairs of corners at the shortest distance apart
            float min = Float.MAX_VALUE;
            for (int i = 0; i < v.length; i++) for (int j = i + 1; j < v.length; j++) min = Math.min(min, d2(v[i], v[j]));
            java.util.List<int[]> e = new java.util.ArrayList<>();
            for (int i = 0; i < v.length; i++) for (int j = i + 1; j < v.length; j++) if (d2(v[i], v[j]) < min * 1.02F) e.add(new int[]{i, j});
            SOLID_V[s] = v;
            SOLID_E[s] = e.toArray(int[][]::new);
        }
    }

    private static float[][] cube() {
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) c[i] = new float[]{(i & 1) == 0 ? -1 : 1, (i & 2) == 0 ? -1 : 1, (i & 4) == 0 ? -1 : 1};
        return c;
    }

    /** All sign combinations of (a, b, c) and its cyclic shifts (b, c, a), (c, a, b). */
    private static float[][] cyc(float a, float b, float c) {
        java.util.List<float[]> out = new java.util.ArrayList<>();
        float[][] shifts = {{a, b, c}, {b, c, a}, {c, a, b}};
        for (float[] sh : shifts)
            for (int sg = 0; sg < 8; sg++) {
                float[] q = {sh[0] * ((sg & 1) == 0 ? 1 : -1), sh[1] * ((sg & 2) == 0 ? 1 : -1), sh[2] * ((sg & 4) == 0 ? 1 : -1)};
                boolean dup = false;
                for (float[] o : out) if (d2(o, q) < 1e-6F) dup = true;
                if (!dup) out.add(q);
            }
        return out.toArray(float[][]::new);
    }

    private static float[][] concat(float[][] a, float[][] b) {
        float[][] r = new float[a.length + b.length][];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static float d2(float[] a, float[] b) {
        float dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /** Draws a turning 3D wireframe at (sx, sy). */
    private static void wire(Fx2D f, float[][] v, int[][] e, float sx, float sy, float size, float ra, float rb, int rgb, float alpha) {
        float[][] pr = new float[v.length][3];
        for (int i = 0; i < v.length; i++) {
            float x = v[i][0], y = v[i][1], z = v[i][2];
            float x1 = x * cos(ra) + z * sin(ra), z1 = -x * sin(ra) + z * cos(ra);
            float y1 = y * cos(rb) - z1 * sin(rb), z2 = y * sin(rb) + z1 * cos(rb);
            float k = 3F / (3F + z2);
            pr[i][0] = sx + x1 * k * size;
            pr[i][1] = sy + y1 * k * size;
            pr[i][2] = z2;
        }
        for (int[] ed : e) {
            float depth = 0.55F - 0.35F * (pr[ed[0]][2] + pr[ed[1]][2]) / 2;      // nearer edges brighter
            f.glowLine(pr[ed[0]][0], pr[ed[0]][1], pr[ed[1]][0], pr[ed[1]][1], 1.1F, rgb, alpha * depth);
        }
    }

    // ================================================================== small maths helpers

    /** Text centred at (x, y), scaled; skipped when nearly invisible. */
    public static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int rgb, float a) {
        int al = (int) (clamp(a) * 255);
        if (al < 8) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, s, -font.width(s) / 2, -4, (al << 24) | (rgb & 0xFFFFFF), false);
        g.pose().popPose();
    }

    /** A repeatable "random" number in [0, 1) for a pair of integers: the same inputs always give the same answer. */
    static float rnd(int a, int b) {
        int h = a * 0x27D4EB2D ^ (b + 0x9E3779B9) * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return (h >>> 8) / 16777216F;
    }

    static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static float smooth(float v) {
        v = clamp(v);
        return v * v * (3 - 2 * v);
    }

    static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    static float mod(float v, float m) {
        return v - (float) Math.floor(v / m) * m;
    }

    private static float cos(float a) {
        return (float) Math.cos(a);
    }

    private static float sin(float a) {
        return (float) Math.sin(a);
    }

    private TransitScenes() {}
}
