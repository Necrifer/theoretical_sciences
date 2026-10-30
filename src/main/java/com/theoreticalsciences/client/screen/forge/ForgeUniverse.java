package com.theoreticalsciences.client.screen.forge;

import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.transcend.ForgeLaws;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * Round 29: the little universe the Omnipotence Core simulates for a Forge recipe, drawn in a window of the Forge.
 *
 * <p>It is made of background stars, a few spiral galaxies joined by faint filaments (the "cosmic web"), a central
 * star with an accretion ring, and a few hundred particles orbiting it. Everything is generated from the recipe's id,
 * so each recipe has its own universe that always looks the same.
 *
 * <p>How healthy it looks comes from the six "observables" (the residual bars): the further they are from zero, the
 * more the universe falls apart. Each observable also has its own symptom, so every constant of nature visibly does
 * something:
 * <ol start="0">
 *   <li>stellar balance - the central star throbs, swells and splits into red/blue ghosts;</li>
 *   <li>causal order - the picture tears into sideways-shifted strips;</li>
 *   <li>quantum coherence - everything jitters and fizzes;</li>
 *   <li>atomic binding - particles break out of their orbits and fly away;</li>
 *   <li>vacuum stability - galaxies and filaments breathe in and out, the web snaps;</li>
 *   <li>arrow of time - the orbits stutter and run backwards.</li>
 * </ol>
 * When everything is within tolerance it settles into calm orbits with soft colours, and once the laws truly converge
 * it "locks": a slow halo pulse and gentle ripples. All of this eases in and out (nothing jumps), and it is cheap: about
 * a thousand simple shapes in one batch per frame.
 */
public final class ForgeUniverse {
    private static final int STARS = 140, GALAXIES = 5, ARM_POINTS = 36, NODES = 12, PARTICLES = 240;
    private static final double TWO_PI = Math.PI * 2;

    // ---- what is generated from the recipe id
    private String id;
    private int tag;
    private final float[] starX = new float[STARS], starY = new float[STARS], starB = new float[STARS], starP = new float[STARS];
    private final float[] galX = new float[GALAXIES], galY = new float[GALAXIES], galR = new float[GALAXIES], galRot = new float[GALAXIES], galSpin = new float[GALAXIES];
    private final int[] galCol = new int[GALAXIES];
    private final float[] nodeX = new float[NODES], nodeY = new float[NODES];
    private int[][] edges = new int[0][];
    private final float[] pR = new float[PARTICLES], pW = new float[PARTICLES], pPh = new float[PARTICLES], pSq = new float[PARTICLES],
            pCol = new float[PARTICLES], pEsc = new float[PARTICLES], pEscPh = new float[PARTICLES], pEscSp = new float[PARTICLES], pTilt = new float[PARTICLES];
    private int calmA, calmB, starCol;

    // ---- eased state (glides; never jumps)
    private double instability = 1, lock, chi2, worst;
    private final double[] part = new double[ForgeLaws.N];
    private long last = System.nanoTime();
    private double simTime, wall;

    /** The short hexadecimal name of this universe (shown as universe#7a3f). */
    public static int tagOf(String recipeId) {
        return (recipeId.hashCode() * 0x45D9F3B >>> 8) & 0xFFFF;
    }

    public int tag() {
        return tag;
    }

    /** 0 = perfectly calm, 1 = falling apart (eased). */
    public double instability() {
        return instability;
    }

    public double lock() {
        return lock;
    }

    /**
     * How unstable a set of readings is, from 0 to 1. A reading inside the tolerance counts as nothing; beyond that,
     * the root-mean-square of the readings is squeezed onto 0..1 (about 0.3 at 10, 0.7 at 30, almost 1 at 100).
     */
    public static double instabilityOf(double[] res) {
        double s = 0;
        for (double v : res) s += v * v;
        double rms = Math.sqrt(s / Math.max(1, res.length));
        return 1 - Math.exp(-Math.max(0, rms - 1) / 25);
    }

    private static double partOf(double v) {
        return 1 - Math.exp(-Math.max(0, Math.abs(v) - ForgeLaws.TOLERANCE * 0.5) / 22);
    }

    /** Feed it the readings shown this frame (already gliding) and whether the real settings converge. */
    public void update(String recipeId, double[] residuals, boolean converged) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1e9);
        last = now;
        wall += dt;
        if (!recipeId.equals(id)) generate(recipeId);
        // how fast the look follows the readings: quick enough to feel each nudge, slow enough to look like a transition
        double k = 1 - Math.exp(-3.2 * dt), kl = 1 - Math.exp(-1.6 * dt);
        instability += (instabilityOf(residuals) - instability) * k;
        lock += ((converged ? 1 : 0) - lock) * kl;
        double c = 0, w = 0;
        for (int i = 0; i < part.length && i < residuals.length; i++) {
            part[i] += (partOf(residuals[i]) - part[i]) * k;
            c += residuals[i] * residuals[i];
            w = Math.max(w, Math.abs(residuals[i]));
        }
        chi2 = c;
        worst = w;
        // the arrow of time: a broken one makes the simulation stutter and even run backwards for a moment
        double stutter = Math.pow(0.5 + 0.5 * Math.sin(wall * 1.3 + tag), 2);
        double rate = 1 - 1.7 * part[5] * stutter;
        simTime += dt * rate * (0.6 + 0.4 * (1 - instability));
    }

    private void generate(String recipeId) {
        id = recipeId;
        tag = tagOf(recipeId);
        Random r = new Random(0x0C05_0A11L ^ (long) recipeId.hashCode() * 0x9E3779B97F4A7C15L);
        // a pair of calm colours per universe (teal, gold, violet, green...) and a colour for its star
        int[] palette = {0x5CFF8A, 0x5CE0FF, 0xFFD27A, 0xB98CFF, 0x8AFFD0, 0xFF9CE0, 0x9CB8FF};
        calmA = palette[r.nextInt(palette.length)];
        calmB = palette[r.nextInt(palette.length)];
        starCol = Fx2D.lerpRgb(0xFFF4D0, calmA, 0.25F);
        for (int i = 0; i < STARS; i++) {
            starX[i] = r.nextFloat();
            starY[i] = r.nextFloat();
            starB[i] = 0.15F + 0.6F * r.nextFloat() * r.nextFloat();
            starP[i] = r.nextFloat() * 6.28F;
        }
        for (int i = 0; i < GALAXIES; i++) {
            // galaxies stay away from the middle, where the central star is
            double a = r.nextDouble() * TWO_PI, d = 0.55 + 0.4 * r.nextDouble();
            galX[i] = (float) (0.5 + Math.cos(a) * d * 0.5);
            galY[i] = (float) (0.5 + Math.sin(a) * d * 0.5);
            galR[i] = 0.05F + 0.06F * r.nextFloat();
            galRot[i] = r.nextFloat() * 6.28F;
            galSpin[i] = (r.nextBoolean() ? 1 : -1) * (0.08F + 0.12F * r.nextFloat());
            galCol[i] = Fx2D.lerpRgb(calmA, calmB, r.nextFloat());
        }
        for (int i = 0; i < NODES; i++) {
            nodeX[i] = r.nextFloat();
            nodeY[i] = r.nextFloat();
        }
        // filaments join each node to its two nearest neighbours
        java.util.List<int[]> e = new java.util.ArrayList<>();
        for (int i = 0; i < NODES; i++) {
            int b1 = -1, b2 = -1;
            float d1 = 9, d2 = 9;
            for (int j = 0; j < NODES; j++) {
                if (j == i) continue;
                float dx = nodeX[i] - nodeX[j], dy = nodeY[i] - nodeY[j], d = dx * dx + dy * dy;
                if (d < d1) { d2 = d1; b2 = b1; d1 = d; b1 = j; } else if (d < d2) { d2 = d; b2 = j; }
            }
            for (int b : new int[]{b1, b2})
                if (b >= 0 && !has(e, i, b) && !has(e, b, i)) e.add(new int[]{i, b});   // each filament once
        }
        edges = e.toArray(new int[0][]);
        for (int i = 0; i < PARTICLES; i++) {
            float rr = 0.16F + 0.82F * (float) Math.sqrt(r.nextFloat());
            pR[i] = rr;
            pW[i] = (float) (0.9 / Math.pow(rr, 1.5)) * (0.85F + 0.3F * r.nextFloat());   // inner orbits are faster (Kepler)
            pPh[i] = r.nextFloat() * 6.28F;
            pSq[i] = 0.42F + 0.12F * r.nextFloat();       // orbits are seen at an angle: ellipses
            pTilt[i] = (r.nextFloat() - 0.5F) * 0.25F;
            pCol[i] = r.nextFloat();
            pEsc[i] = r.nextFloat() * 6.28F;
            pEscPh[i] = r.nextFloat();
            pEscSp[i] = 0.15F + 0.35F * r.nextFloat();
        }
    }

    private static boolean has(java.util.List<int[]> e, int a, int b) {
        for (int[] x : e) if (x[0] == a && x[1] == b) return true;
        return false;
    }

    /** A repeatable "random" number 0..1 from two integers (the same inputs always give the same number). */
    private static float hash(int a, int b) {
        int h = a * 0x27D4EB2D ^ b * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Draws the universe into the window (x, y, w, h). */
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int green, int amber, int red, int dim) {
        if (id == null || w < 8 || h < 8) return;
        float u = (float) instability, lk = (float) lock;
        float t = (float) simTime, wt = (float) wall;
        int q = (int) (wall * 14);           // changes 14 times a second: the rhythm of glitches
        float cx = x + w / 2F, cy = y + h / 2F;
        float R = Math.min(w * 0.46F, h * 0.46F / 0.5F);
        float flick = 1 - u * 0.45F * hash(q, 7) * (hash(q, 8) < u ? 1 : 0);   // a chaotic universe flickers
        int hot = 0xFF3A28, hot2 = 0xFF5AC8;

        g.fill(x, y, x + w, y + h, 0xFF010604);
        g.enableScissor(x, y, x + w, y + h);
        // a faint measuring grid behind it all: the "computer" look
        int gridCol = 0x0C5CFF8A;
        for (int gx = 1; gx < 8; gx++) g.fill(x + gx * w / 8, y, x + gx * w / 8 + 1, y + h, gridCol);
        for (int gy = 1; gy < 6; gy++) g.fill(x, y + gy * h / 6, x + w, y + gy * h / 6 + 1, gridCol);

        // causal order broken: the picture tears into strips shifted sideways
        int bands = Math.max(4, h / 5);
        float[] tear = new float[bands + 1];
        float tearAmt = (float) (part[1] * 0.7 + u * 0.3);
        for (int b = 0; b <= bands; b++) {
            float hv = hash(b, q / 2 + tag);
            tear[b] = hv < tearAmt * 0.55F ? (hash(b, q + 99) - 0.5F) * 2 * w * 0.09F * tearAmt : 0;
        }
        float bandH = h / (float) bands;

        Fx2D f = Fx2D.begin(g, Fx2D.Blend.ADD);
        // --- background stars (they redden and streak when the universe is unstable)
        for (int i = 0; i < STARS; i++) {
            float sx = x + starX[i] * w, sy = y + starY[i] * h;
            sx += tear[Math.min(bands, (int) ((sy - y) / bandH))];
            float a = starB[i] * (0.7F + 0.3F * (float) Math.sin(wt * 2.1 + starP[i])) * flick;
            int c = Fx2D.lerpRgb(0xCFE8FF, hot, u * 0.7F);
            float streak = u * u * 4;
            f.rect(sx - streak, sy, sx + 1 + streak, sy + 1, c, a);
        }
        // --- the cosmic web: filaments between nodes (they breathe with the vacuum and snap when it is unstable)
        float vac = (float) part[4];
        float breathe = 1 + vac * 0.18F * (float) Math.sin(wt * 1.7 + tag);
        for (int[] e : edges) {
            if (vac > 0.3F && hash(e[0] * 31 + e[1], q / 3) < vac * 0.5F) continue;     // a snapped filament
            float ax = cx + (nodeX[e[0]] * w - w / 2F) * breathe, ay = cy + (nodeY[e[0]] * h - h / 2F) * breathe;
            float bx = cx + (nodeX[e[1]] * w - w / 2F) * breathe, by = cy + (nodeY[e[1]] * h - h / 2F) * breathe;
            int c = Fx2D.lerpRgb(calmA, hot, u);
            f.line(ax, ay, bx, by, 1, c, 0.10F * flick * (1 - 0.4F * u), c, 0.04F * flick);
        }
        // --- galaxies: two-armed spirals
        for (int gi = 0; gi < GALAXIES; gi++) {
            float gx = cx + (galX[gi] * w - w / 2F) * breathe, gy = cy + (galY[gi] * h - h / 2F) * breathe;
            float gr = galR[gi] * Math.min(w, h * 1.6F);
            int c = Fx2D.lerpRgb(galCol[gi], hot2, u);
            f.disc(gx, gy, gr * 0.45F, 10, c, 0.35F * flick, c, 0);
            float rot = galRot[gi] + t * galSpin[gi] * (1 + 3 * u);
            float smear = 3.2F + vac * 3;            // an unstable vacuum winds the arms out of shape
            for (int k = 0; k < ARM_POINTS; k++) {
                float s = (k / 2) / (float) (ARM_POINTS / 2);
                double ang = rot + (k % 2) * Math.PI + s * smear;
                float px = gx + (float) Math.cos(ang) * s * gr, py = gy + (float) Math.sin(ang) * s * gr * 0.55F;
                px += tear[Math.max(0, Math.min(bands, (int) ((py - y) / bandH)))];
                f.rect(px, py, px + 1, py + 1, c, (0.55F - s * 0.35F) * flick);
            }
        }
        // --- the guide rings of the orbits, visible only when calm
        float calm = 1 - u;
        if (calm > 0.05F) for (int k = 1; k <= 4; k++) {
            float rr = R * (0.2F + k * 0.19F);
            f.band(cx, cy, rr - 0.5F, rr + 0.5F, 0.48F, 0, (float) TWO_PI, 48, calmA, 0.07F * calm, calmA, 0.07F * calm);
        }
        // --- particles on their orbits
        float jit = (float) (part[2] * 0.8 + u * 0.2) * 3.5F;
        float bind = (float) (part[3] * 0.75 + u * 0.25);
        float wob = u * 0.25F;
        for (int i = 0; i < PARTICLES; i++) {
            double th = pPh[i] + pW[i] * t;
            float rr = pR[i] * R * (1 + wob * (float) Math.sin(t * 2.3 + i));
            float sq = pSq[i];
            float px = cx + (float) Math.cos(th) * rr, py = cy + (float) (Math.sin(th) * rr * sq + Math.cos(th) * rr * pTilt[i]);
            float alpha = 0.75F;
            // broken atomic binding: particles leave their orbit in a straight line and fade, then start again
            float cyc = (float) ((wall * pEscSp[i] + pEscPh[i]) % 1.0);
            float out = bind * bind * cyc * R * 1.3F;
            if (pR[i] > 1 - bind * 1.1F) {
                px += (float) Math.cos(pEsc[i]) * out;
                py += (float) Math.sin(pEsc[i]) * out * 0.7F;
                alpha *= 1 - cyc * bind;
            }
            // broken quantum coherence: everything jitters
            if (jit > 0.05F) {
                px += (hash(i, q) - 0.5F) * 2 * jit;
                py += (hash(i + 7777, q) - 0.5F) * 2 * jit;
            }
            px += tear[Math.max(0, Math.min(bands, (int) ((py - y) / bandH)))];
            int c = Fx2D.lerpRgb(Fx2D.lerpRgb(calmA, calmB, pCol[i]), Fx2D.lerpRgb(hot, hot2, pCol[i]), u);
            float size = 1 + (pR[i] < 0.4F ? 0.5F : 0);
            // a short trail behind each particle while it is on a calm orbit
            if (calm > 0.1F) {
                double tb = th - 0.22 * Math.signum(pW[i]);
                float qx = cx + (float) Math.cos(tb) * rr, qy = cy + (float) (Math.sin(tb) * rr * sq + Math.cos(tb) * rr * pTilt[i]);
                qx += tear[Math.max(0, Math.min(bands, (int) ((qy - y) / bandH)))];
                f.line(qx, qy, px, py, 1, c, 0, c, 0.3F * calm * flick);
            }
            f.rect(px - size / 2, py - size / 2, px + size / 2, py + size / 2, c, alpha * flick);
        }
        // --- the central star: steady and round when balanced; throbbing, swollen and split when not
        float sb = (float) part[0];
        float throb = 1 + sb * 0.45F * (float) (Math.sin(wt * 7.3) * Math.sin(wt * 3.1 + 1));
        float pulse = 1 + lk * 0.06F * (float) Math.sin(wt * 2.2);
        float rs = R * 0.085F * throb * pulse * (1 + sb * 0.4F);
        int core = Fx2D.lerpRgb(starCol, hot, Math.max(u, sb));
        float split = sb * sb * 4 + u * 1.5F;
        if (split > 0.3F) {      // red and blue ghosts: the star cannot decide where it is
            f.disc(cx - split, cy, rs * 1.3F, 18, 0xFF2020, 0.35F * flick, 0xFF2020, 0);
            f.disc(cx + split, cy, rs * 1.3F, 18, 0x2060FF, 0.35F * flick, 0x2060FF, 0);
        }
        f.disc(cx, cy, rs * 4.0F, 24, core, 0.10F * flick, core, 0);           // corona
        f.disc(cx, cy, rs * 1.6F, 20, core, 0.45F * flick, core, 0);
        f.disc(cx, cy, rs * 0.7F, 16, 0xFFFFFF, 0.9F * flick, core, 0.4F);
        // the accretion ring: whole when balanced, broken into pieces when not
        int seg = 40;
        float ringR = rs * 2.6F;
        for (int s = 0; s < seg; s++) {
            if (hash(s, q / 2 + 3) < sb * 0.6F + u * 0.2F) continue;
            float a0 = (float) (TWO_PI * s / seg + t * 0.6), wob2 = 1 + (hash(s, q) - 0.5F) * sb * 0.5F;
            f.band(cx, cy, ringR * wob2 - 1, ringR * wob2 + 1, 0.42F, a0, (float) (TWO_PI / seg), 1, core, 0.55F * flick, core, 0.1F * flick);
        }
        // --- locked: a soft halo and slow ripples (the universe holds)
        if (lk > 0.02F) {
            f.disc(cx, cy, R * 0.55F * pulse, 32, calmA, 0.06F * lk, calmA, 0);
            for (int k = 0; k < 3; k++) {
                float fr = (float) ((wall * 0.18 + k / 3.0) % 1.0);
                f.band(cx, cy, R * 1.1F * fr, R * 1.1F * fr + 1.2F, 0.5F, 0, (float) TWO_PI, 56, calmA, 0.22F * lk * (1 - fr), calmA, 0.22F * lk * (1 - fr));
            }
        }
        f.end();

        // --- glitch scanlines and colour bars when chaotic (ordinary paint, on top)
        int glitches = (int) (u * 7);
        for (int k = 0; k < glitches; k++) {
            if (hash(k, q) > u) continue;
            int gy = y + (int) (hash(k + 50, q) * h);
            int gh = 1 + (int) (hash(k + 90, q) * 2);
            int gx0 = x + (int) (hash(k + 130, q) * w * 0.5F);
            int gx1 = gx0 + (int) (w * (0.2F + 0.6F * hash(k + 170, q)));
            g.fill(gx0, gy, Math.min(x + w, gx1), Math.min(y + h, gy + gh), k % 2 == 0 ? 0x40FF4A3A : 0x305CFF8A);
        }
        if (hash(q, 33) < u * 0.12F) g.fill(x, y, x + w, y + h, 0x30000000);        // a dropped frame
        // the faint scanlines of the monitor
        for (int sy = y; sy < y + h; sy += 2) g.fill(x, sy, x + w, sy + 1, 0x16000000);
        g.disableScissor();

        // --- the readout in the corners
        if (h >= 24 && w >= 60) {
            String tl = "universe#" + String.format("%04x", tag);
            g.drawString(font, cut(font, tl, w - 6), x + 3, y + 3, dim, false);
            String state = lk > 0.5 ? "LOCKED" : u < 0.08 ? "SETTLING" : "DIVERGENT";
            int sc = lk > 0.5 ? green : u < 0.08 ? amber : red;
            boolean blink = lk > 0.5 || (System.currentTimeMillis() / 400) % 2 == 0;
            String bl = String.format("χ² %.1f", chi2);
            if (h >= 36) g.drawString(font, cut(font, bl, w / 2 - 4), x + 3, y + h - 11, lk > 0.5 ? green : u < 0.08 ? amber : red, false);
            if (blink) g.drawString(font, state, x + w - 3 - font.width(state), y + h - 11, sc, false);
            if (w >= 120) {
                String tr = String.format("λ %+.2f", (u - 0.05) * 2);
                g.drawString(font, tr, x + w - 3 - font.width(tr), y + 3, dim, false);
            }
        }
    }

    public double chi2() {
        return chi2;
    }

    public double worst() {
        return worst;
    }

    private static String cut(Font f, String s, int w) {
        if (f.width(s) <= w) return s;
        return f.plainSubstrByWidth(s, Math.max(0, w - f.width("…"))) + "…";
    }
}
