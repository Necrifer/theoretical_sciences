package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.machine.MachineType;

/**
 * 1.14.2: the operator console's simulation window draws what each machine actually does. Before this, every multiblock
 * outside four families shared one picture (a stirred beaker). Now each kind of machine has its own small animated
 * diagram: a dish catching radio waves, a crystal being pulled from its melt, a drill string going down through strata,
 * an L-shaped interferometer with its arms breathing, and so on.
 *
 * <p>Every scene is drawn around a centre (cx, cy) inside a radius R, in additive light, from the console's phase (it
 * runs faster when the machine runs faster) and the cycle's progress f (0..1). {@link #draw} returns false for effects
 * it does not handle, which keep the console's family pictures.
 */
final class SimScenes {
    private final Fx2D fx;
    private final float cx, cy, R, f, t, lit;
    private final int col, hot;
    private final boolean run;
    private static final float TAU = (float) (Math.PI * 2);
    private static final int W = 0xFFFFFF;

    private SimScenes(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase) {
        this.fx = fx;
        this.cx = cx;
        this.cy = cy;
        this.R = R;
        this.col = col;
        this.hot = hot;
        this.run = run;
        this.f = f;
        this.t = phase * 0.05F;
        this.lit = run ? 1F : 0.35F;
    }

    static boolean draw(Fx2D fx, MachineType.Effect e, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase) {
        SimScenes s = new SimScenes(fx, cx, cy, R, col, hot, run, f, phase);
        switch (e) {
            case ASSEMBLY -> s.assembly();
            case HEARTH -> s.press();
            case DRILL_STRING -> s.drill();
            case CENTRIFUGE -> s.centrifuge();
            case SALT_GLOW -> s.saltLoop();
            case MHD_FLOW -> s.mhd();
            case HEAT_DEATH -> s.heatDeath();
            case STARLIGHT -> s.telescope();
            case SOLAR -> s.solar();
            case RADIO -> s.radio();
            case DIFFRACTION -> s.diffraction();
            case CZOCHRALSKI -> s.czochralski();
            case FRINGES -> s.newtonRings();
            case LASER_ARMS -> s.interferometer();
            case GAMMA_RISE -> s.redshiftTower();
            case BROWNIAN -> s.brownian();
            case MONTE_CARLO -> s.monteCarlo();
            case CYCLOTRON -> s.cyclotron();
            case TESLA_ARCS -> s.tesla();
            case NEUTRON_PATHS -> s.neutronPaths();
            case SONIC_HORIZON -> s.sonicHorizon();
            case QUASAR_JETS -> s.quasar();
            case TOKAMAK_PLASMA -> s.tokamak();
            case PULSED_FIELD -> s.pulsedField();
            case RAILGUN -> s.railgun();
            case GAMMA_BURST -> s.gammaBurst();
            case CLIMATE -> s.climate();
            case EINSTEIN_RING -> s.einsteinRing();
            case DILATION_RINGS -> s.dilationClocks();
            case WARP_BUBBLE -> s.warp();
            case UNRUH_BATH -> s.unruh();
            case HAWKING_SPARK -> s.hawking();
            case ER_BRIDGE -> s.erBridge();
            case MARKOV_HOPS -> s.markov();
            case RIFT -> s.rift();
            case POCKET_UNIVERSE -> s.bubbles();
            case TIME_LOOP -> s.timeLoop();
            case EXPANSION -> s.hubbleFlow();
            case MATRIX -> s.digitalRain();
            case BREACH -> s.breach();
            case OMNILEX -> s.nestedLaws();
            case CONCEPT -> s.compress();
            case FOAM -> s.foam();
            case ANNIHILATION -> s.annihilation();
            case PULSAR -> s.pulsar();
            case SYNCHROTRON -> s.synchrotron();
            case DARK_LENS -> s.darkLens();
            case WORMHOLE -> s.wormhole();
            case EYE -> s.eye();
            case STAR_FORGE -> s.starForge();
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers
    private static float sin(double a) {
        return (float) Math.sin(a);
    }

    private static float cos(double a) {
        return (float) Math.cos(a);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** A repeatable pseudo-random number in [0, 1) for (i, j). */
    private static float rnd(int i, int j) {
        int h = i * 374761393 + j * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0x1000000;
    }

    private void dot(float x, float y, float r, float a) {
        fx.disc(x, y, r, 8, W, a * lit, hot, 0F);
    }

    private void ellipse(float x, float y, float rx, float ry, float w, int c, float a) {
        float sq = ry / Math.max(rx, 0.01F);
        fx.band(x, y, rx - w / 2, rx + w / 2, sq, 0, TAU, 40, c, a, c, a);
    }

    private void arrow(float x0, float y0, float x1, float y1, int c, float a) {
        fx.line(x0, y0, x1, y1, 1F, c, a);
        float ang = (float) Math.atan2(y1 - y0, x1 - x0);
        fx.line(x1, y1, x1 - 4 * cos(ang - 0.5), y1 - 4 * sin(ang - 0.5), 1F, c, a);
        fx.line(x1, y1, x1 - 4 * cos(ang + 0.5), y1 - 4 * sin(ang + 0.5), 1F, c, a);
    }

    private void wave(float x0, float y0, float x1, float y1, float amp, float lambda, float ph, int c, float a) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0), ux = (x1 - x0) / len, uy = (y1 - y0) / len;
        float px = x0, py = y0;
        int n = Math.max(8, (int) (len / 2));
        for (int i = 1; i <= n; i++) {
            float s = len * i / n, o = amp * sin(s / lambda * TAU - ph);
            float qx = x0 + ux * s - uy * o, qy = y0 + uy * s + ux * o;
            fx.line(px, py, qx, qy, 1F, c, a);
            px = qx;
            py = qy;
        }
    }

    private void bar(float x0, float y0, float x1, float y1, float fill) {
        fx.rect(x0, y0, x1, y1, col, 0.15F);
        fx.rect(x0, y0, x0 + (x1 - x0) * Math.max(0, Math.min(1, fill)), y1, hot, 0.6F);
    }

    // ------------------------------------------------------------------ industry
    /** A conveyor of parts, two arms picking and placing on it. */
    private void assembly() {
        float y = cy + R * 0.45F;
        fx.line(cx - R, y, cx + R, y, 2F, col, 0.6F);
        fx.line(cx - R, y + 5, cx + R, y + 5, 1F, col, 0.4F);
        for (int i = 0; i < 7; i++) {
            float x = cx - R + frac(t * 0.25F + i / 7F) * 2 * R;
            fx.rect(x - 3, y - 6, x + 3, y, i % 2 == 0 ? hot : col, 0.6F * lit);
        }
        for (int a = -1; a <= 1; a += 2) {
            float bx = cx + a * R * 0.45F, by = cy - R * 0.7F, k = 0.5F + 0.5F * sin(t * 3 + a);
            float ex = bx + a * 6 * cos(t * 2), ey = by + R * (0.4F + 0.5F * k);
            fx.rect(bx - 5, by - 3, bx + 5, by, col, 0.6F);
            fx.line(bx, by, (bx + ex) / 2 - a * 6, (by + ey) / 2, 2F, col, 0.7F);
            fx.line((bx + ex) / 2 - a * 6, (by + ey) / 2, ex, ey, 2F, hot, 0.7F);
            fx.line(ex - 3, ey, ex + 3, ey, 1.5F, W, 0.8F * lit);
        }
    }

    /** Two platens closing on a rock that glows as the pressure and heat build. */
    private void press() {
        float gap = R * (0.55F - 0.3F * (run ? f : 0));
        fx.rect(cx - R * 0.7F, cy - gap - 8, cx + R * 0.7F, cy - gap, col, 0.6F);
        fx.rect(cx - R * 0.7F, cy + gap, cx + R * 0.7F, cy + gap + 8, col, 0.6F);
        fx.rect(cx - 3, cy - R, cx + 3, cy - gap - 8, col, 0.4F);
        fx.rect(cx - 3, cy + gap + 8, cx + 3, cy + R, col, 0.4F);
        float w = R * (0.35F + 0.15F * (run ? f : 0));
        fx.rect(cx - w, cy - gap + 1, cx + w, cy + gap - 1, Fx2D.lerpRgb(col, 0xFF9040, run ? f : 0), 0.5F);
        for (int i = 0; i < 3; i++) {
            float x = cx - R * 0.5F + i * R * 0.5F;
            arrow(x, cy - gap - 18, x, cy - gap - 10, hot, 0.6F * lit);
            arrow(x, cy + gap + 18, x, cy + gap + 10, hot, 0.6F * lit);
        }
    }

    /** Strata, a turning bit going down through them, and cuttings coming back up. */
    private void drill() {
        int[] layers = {0x806040, 0x606060, 0x404858, 0x705030, 0x503020};
        for (int i = 0; i < 5; i++) {
            float y0 = cy - R + 2 * R * i / 5, y1 = cy - R + 2 * R * (i + 1) / 5;
            fx.rect(cx - R, y0, cx + R, y1, Fx2D.lerpRgb(layers[i], col, 0.3F), 0.18F);
        }
        float depth = cy - R + 2 * R * (run ? 0.2F + 0.75F * f : 0.2F);
        fx.line(cx, cy - R - 6, cx, depth, 2.5F, col, 0.8F);
        fx.polygon(cx, depth + 3, 4, 3, t * 6, 1.5F, hot, 0.9F * lit);
        for (int i = 0; i < 8; i++) {
            float u = frac(t * 0.6F + i / 8F), y = depth - (depth - cy + R) * u;
            dot(cx + (i % 2 == 0 ? -5 : 5), y, 1.2F, 0.6F * (1 - u));
        }
    }

    /** A cascade of spinning rotors; the heavy isotope drifts out to the walls, the light one is passed along. */
    private void centrifuge() {
        for (int k = 0; k < 5; k++) {
            float x = cx - R * 0.8F + k * R * 0.4F, h = R * 0.75F, w = R * 0.13F;
            fx.line(x - w, cy - h, x - w, cy + h, 1.5F, col, 0.6F);
            fx.line(x + w, cy - h, x + w, cy + h, 1.5F, col, 0.6F);
            for (int s = 0; s < 4; s++) {
                float u = frac(t * 2.5F + s / 4F + k * 0.13F);
                float sx = x - w + 2 * w * (0.5F + 0.5F * sin(u * TAU));
                fx.line(sx, cy - h, sx, cy + h, 1F, hot, 0.25F * lit * (0.5F + 0.5F * cos(u * TAU)));
            }
            if (k < 4) {
                float u = frac(t * 0.5F + k * 0.25F);
                dot(x + w + (R * 0.4F - 2 * w) * u, cy - h * 0.6F, 1.6F, 0.9F);
                dot(x + w + (R * 0.4F - 2 * w) * u, cy + h * 0.6F, 1.2F, 0.4F);
            }
        }
    }

    /** A core of channels with molten salt carried round a loop to a heat exchanger. */
    private void saltLoop() {
        float rx = cx - R * 0.35F;
        fx.rect(rx - R * 0.4F, cy - R * 0.6F, rx + R * 0.4F, cy + R * 0.6F, col, 0.15F);
        for (int i = -2; i <= 2; i++)
            fx.line(rx + i * R * 0.14F, cy - R * 0.55F, rx + i * R * 0.14F, cy + R * 0.55F, 2F, 0x60C0FF, (0.4F + 0.3F * sin(t * 2 + i)) * lit);
        float hx = cx + R * 0.6F;
        fx.rect(hx - 6, cy - R * 0.4F, hx + 6, cy + R * 0.4F, col, 0.5F);
        float[][] path = {{rx, cy - R * 0.6F}, {rx, cy - R * 0.85F}, {hx, cy - R * 0.85F}, {hx, cy - R * 0.4F}, {hx, cy + R * 0.4F},
                {hx, cy + R * 0.85F}, {rx, cy + R * 0.85F}, {rx, cy + R * 0.6F}};
        for (int i = 0; i + 1 < path.length; i++) fx.line(path[i][0], path[i][1], path[i + 1][0], path[i + 1][1], 1.5F, col, 0.5F);
        for (int k = 0; k < 10; k++) {
            float u = frac(t * 0.3F + k / 10F) * (path.length - 1);
            int i = (int) u;
            float v = u - i;
            float x = path[i][0] + (path[i + 1][0] - path[i][0]) * v, y = path[i][1] + (path[i + 1][1] - path[i][1]) * v;
            fx.disc(x, y, 1.8F, 8, Fx2D.lerpRgb(0xFFB060, 0x60C0FF, u / (path.length - 1)), 0.9F * lit, hot, 0F);
        }
    }

    /** Plasma flowing through a channel across a magnetic field; the charges are pushed to the electrodes. */
    private void mhd() {
        float h = R * 0.45F;
        fx.rect(cx - R, cy - h - 4, cx + R, cy - h, col, 0.6F);
        fx.rect(cx - R, cy + h, cx + R, cy + h + 4, col, 0.6F);
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 2; j++) {
                float x = cx - R * 0.8F + i * R * 0.4F, y = cy - h * 0.5F + j * h;
                fx.line(x - 2, y - 2, x + 2, y + 2, 1F, col, 0.4F);
                fx.line(x - 2, y + 2, x + 2, y - 2, 1F, col, 0.4F);
            }
        for (int k = 0; k < 14; k++) {
            float u = frac(t * 0.5F + rnd(k, 1)), x = cx - R + 2 * R * u;
            boolean pos = k % 2 == 0;
            float drift = (pos ? -1 : 1) * h * 0.85F * u;
            fx.disc(x, cy + drift, 1.6F, 8, pos ? 0xFF8060 : 0x60A0FF, 0.9F * lit, hot, 0F);
        }
        arrow(cx, cy - h - 5, cx, cy - R, hot, 0.5F * lit);
        arrow(cx, cy + R, cx, cy + h + 5, hot, 0.5F * lit);
    }

    /** Stars dimming and a gas spreading out until it is even everywhere - entropy at its maximum. */
    private void heatDeath() {
        float k = run ? f : 0.3F;
        for (int i = 0; i < 40; i++) {
            float ox = (rnd(i, 1) - 0.5F) * 2 * R, oy = (rnd(i, 2) - 0.5F) * 2 * R * 0.8F;
            float x = cx + ox * (0.25F + 0.75F * k) + sin(t + i) * 1.5F, y = cy + oy * (0.25F + 0.75F * k) + cos(t * 1.3F + i) * 1.5F;
            fx.disc(x, y, 1.3F, 6, Fx2D.lerpRgb(0xFFD080, 0x404860, k), 0.8F * lit, hot, 0F);
        }
        for (int i = 0; i < 4; i++) fx.disc(cx + (rnd(i, 7) - 0.5F) * R, cy + (rnd(i, 8) - 0.5F) * R, 4, 12, W, (1 - k) * 0.8F * lit, hot, 0F);
        bar(cx - R, cy + R * 0.9F, cx + R, cy + R * 0.9F + 3, k);
    }

    // ------------------------------------------------------------------ observatories
    /** Starlight entering a reflector, bouncing off the primary mirror to its focus. */
    private void telescope() {
        float mx = cx, my = cy + R * 0.8F, fy = cy - R * 0.25F;
        fx.band(mx, my - R * 1.6F, R * 1.55F, R * 1.62F, 1, (float) (Math.PI * 0.35), (float) (Math.PI * 0.3), 24, hot, 0.8F, hot, 0.8F);
        fx.line(cx - R * 0.65F, cy - R, cx - R * 0.65F, my, 1F, col, 0.4F);
        fx.line(cx + R * 0.65F, cy - R, cx + R * 0.65F, my, 1F, col, 0.4F);
        for (int i = -2; i <= 2; i++) {
            float x = cx + i * R * 0.25F, u = frac(t * 0.6F + i * 0.13F);
            fx.line(x, cy - R, x, my - Math.abs(i) * 2, 1F, W, 0.25F * lit);
            fx.line(x, my - Math.abs(i) * 2, cx, fy, 1F, hot, 0.3F * lit);
            float px = u < 0.5F ? x : x + (cx - x) * (u - 0.5F) * 2, py = u < 0.5F ? cy - R + (my - cy + R) * u * 2 : my + (fy - my) * (u - 0.5F) * 2;
            dot(px, py, 1.3F, 0.9F);
        }
        dot(cx, fy, 2.5F, 0.6F + 0.4F * sin(t * 4));
        for (int i = 0; i < 6; i++) dot(cx + (rnd(i, 3) - 0.5F) * 2 * R, cy - R - 4 + rnd(i, 4) * 3, 0.9F, 0.4F + 0.4F * sin(t * 5 + i));
    }

    /** A field of heliostats turning to throw the sun onto a receiver on a tower. */
    private void solar() {
        float tx = cx, ty = cy - R * 0.55F;
        fx.rect(tx - 2, ty, tx + 2, cy + R * 0.8F, col, 0.6F);
        fx.disc(tx, ty, 4 + 3 * (run ? f : 0), 16, W, 0.9F * lit, hot, 0F);
        float sx = cx - R * 0.9F, sy = cy - R * 0.95F;
        dot(sx, sy, 4, 1F);
        for (int i = 0; i < 6; i++) {
            float x = cx - R * 0.9F + i * R * 0.36F;
            if (Math.abs(x - tx) < 4) continue;
            float y = cy + R * 0.75F;
            float ang = (float) Math.atan2(ty - y, tx - x) * 0.5F + (float) Math.atan2(sy - y, sx - x) * 0.5F + (float) Math.PI / 2;
            fx.line(x - 4 * cos(ang), y - 4 * sin(ang), x + 4 * cos(ang), y + 4 * sin(ang), 2F, col, 0.8F);
            fx.line(sx, sy, x, y, 1F, W, 0.12F * lit);
            fx.line(x, y, tx, ty, 1F, hot, 0.35F * lit * (0.7F + 0.3F * sin(t * 6 + i)));
        }
    }

    /** A dish catching radio waves, and the 21 cm line rising out of the noise on its spectrum. */
    private void radio() {
        float dx = cx - R * 0.35F, dy = cy + R * 0.1F;
        fx.band(dx, dy - R * 0.9F, R * 0.85F, R * 0.92F, 1, (float) (Math.PI * 0.3), (float) (Math.PI * 0.4), 24, col, 0.8F, col, 0.8F);
        fx.line(dx, dy, dx, dy - R * 0.55F, 1F, col, 0.6F);
        dot(dx, dy - R * 0.55F, 2F, 0.8F);
        fx.line(dx, dy, dx, cy + R, 2F, col, 0.5F);
        for (int i = -1; i <= 1; i++) wave(dx + i * R * 0.3F, cy - R, dx + i * R * 0.3F, dy - 2, 2.5F, 6, t * 8, hot, 0.4F * lit);
        float sx0 = cx + R * 0.15F, sx1 = cx + R, base = cy + R * 0.6F;
        float px = sx0, py = base;
        for (int i = 1; i <= 30; i++) {
            float x = sx0 + (sx1 - sx0) * i / 30F, u = i / 30F - 0.5F;
            float peak = (run ? 0.3F + 0.7F * f : 0.2F) * R * 0.9F * (float) Math.exp(-u * u * 120);
            float y = base - peak - rnd(i, (int) (t * 6)) * 4;
            fx.line(px, py, x, y, 1F, hot, 0.7F);
            px = x;
            py = y;
        }
        fx.line(sx0, base, sx1, base, 1F, col, 0.4F);
    }

    /** An X-ray beam scattered by a crystal's planes into spots on a detector. */
    private void diffraction() {
        float lx = cx - R * 0.1F;
        for (int i = -2; i <= 2; i++)
            for (int j = -2; j <= 2; j++) dot(lx + i * 4, cy + j * 4, 0.9F, 0.7F);
        float[] beam = {cx - R, cy};
        fx.line(beam[0], beam[1], lx - 10, cy, 2F, hot, 0.8F * lit);
        float dxp = cx + R * 0.85F;
        fx.line(dxp, cy - R * 0.9F, dxp, cy + R * 0.9F, 2F, col, 0.6F);
        for (int k = -3; k <= 3; k++) {
            float y = cy + k * R * 0.26F, ph = frac(t * 0.8F + Math.abs(k) * 0.17F);
            fx.line(lx + 10, cy, dxp, y, 1F, hot, (k == 0 ? 0.6F : 0.3F) * lit);
            fx.disc(dxp, y, 2.2F, 10, W, lit * (0.4F + 0.6F * (1 - ph)) * (k == 0 ? 1 : 1 - Math.abs(k) * 0.2F), hot, 0F);
        }
    }

    /** A seed dipped into the melt and drawn slowly up, turning, a boule growing beneath it. */
    private void czochralski() {
        float base = cy + R * 0.8F;
        fx.line(cx - R * 0.6F, base - R * 0.4F, cx - R * 0.6F, base, 2F, col, 0.7F);
        fx.line(cx + R * 0.6F, base - R * 0.4F, cx + R * 0.6F, base, 2F, col, 0.7F);
        fx.line(cx - R * 0.6F, base, cx + R * 0.6F, base, 2F, col, 0.7F);
        fx.rectV(cx - R * 0.58F, base - R * 0.25F, cx + R * 0.58F, base - 1, 0xFFA040, 0.6F, 0xFF4020, 0.5F);
        float len = R * (run ? 0.15F + 0.75F * f : 0.15F), top = base - R * 0.25F - len;
        float bw = R * 0.18F;
        fx.rect(cx - bw, top, cx + bw, base - R * 0.25F, Fx2D.lerpRgb(col, W, 0.4F), 0.55F);
        for (int i = 0; i < 4; i++) {
            float x = cx - bw + 2 * bw * frac(t * 0.4F + i / 4F);
            fx.line(x, top, x, base - R * 0.25F, 1F, W, 0.3F * lit);
        }
        fx.line(cx, top, cx, cy - R, 1F, col, 0.7F);
        arrow(cx + bw + 6, top + 10, cx + bw + 6, top, hot, 0.6F * lit);
    }

    /** A lens being figured: Newton's rings in the gap breathe as the surface is ground true. */
    private void newtonRings() {
        for (int k = 1; k < 9; k++) {
            float r = R * 0.95F * (float) Math.sqrt(k / 9F) * (1 + 0.02F * sin(t * 2 + k));
            fx.ring(cx, cy, r, 1.4F, 48, k % 2 == 0 ? hot : col, (0.25F + 0.4F * (1 - k / 9F)) * lit);
        }
        dot(cx, cy, 3F, 0.9F);
        fx.line(cx - R, cy + R * 0.95F, cx + R, cy + R * 0.95F, 2F, col, 0.6F);
    }

    /** An L of two long arms: a laser split, sent down both, and recombined; a passing wave stretches one and squeezes the other. */
    private void interferometer() {
        float ox = cx - R * 0.7F, oy = cy + R * 0.7F, L = R * 1.5F, h = (run ? 0.06F : 0.02F) * sin(t * 3);
        float ex = ox + L * (1 + h), ny = oy - L * (1 - h);
        fx.line(ox, oy, ex, oy, 1.5F, col, 0.6F);
        fx.line(ox, oy, ox, ny, 1.5F, col, 0.6F);
        fx.rect(ex, oy - 4, ex + 2, oy + 4, W, 0.8F);
        fx.rect(ox - 4, ny - 2, ox + 4, ny, W, 0.8F);
        fx.line(ox - 4, oy + 4, ox + 4, oy - 4, 1.5F, W, 0.7F);
        for (int k = 0; k < 4; k++) {
            float u = frac(t + k / 4F), bu = u < 0.5F ? u * 2 : 2 - u * 2;
            dot(ox + (ex - ox) * bu, oy, 1.4F, 0.9F);
            dot(ox, oy + (ny - oy) * bu, 1.4F, 0.9F);
        }
        float bright = 0.5F + 0.5F * sin(h * 80);
        fx.disc(ox, oy + 10, 3F, 10, W, bright * lit, hot, 0F);
    }

    /** A gamma ray climbing a tower, its wavelength stretching as it rises out of the gravity well. */
    private void redshiftTower() {
        fx.rect(cx - R * 0.35F, cy - R, cx - R * 0.3F, cy + R, col, 0.5F);
        fx.rect(cx + R * 0.3F, cy - R, cx + R * 0.35F, cy + R, col, 0.5F);
        float u = frac(t * 0.35F), top = cy + R - 2 * R * u, px = cx, py = cy + R;
        for (int i = 1; i <= 60; i++) {
            float s = i / 60F, y = cy + R - 2 * R * s;
            if (y < top) break;
            float lambda = 5 + 6 * s, x = cx + 5 * sin(s * 2 * R / lambda * TAU - t * 10);
            fx.line(px, py, x, y, 1.2F, Fx2D.lerpRgb(0x80C0FF, 0xFF6040, s), 0.8F * lit);
            px = x;
            py = y;
        }
        fx.rect(cx - R * 0.5F, cy + R, cx + R * 0.5F, cy + R + 3, col, 0.6F);
        fx.rect(cx - R * 0.4F, cy - R - 3, cx + R * 0.4F, cy - R, hot, 0.6F);
    }

    // ------------------------------------------------------------------ statistics and particles
    /** A large particle jostled into a random walk by the small ones around it, its trail drawn behind it. */
    private void brownian() {
        float px = cx, py = cy;
        int steps = 40, cur = (int) (t * 6);
        for (int i = 0; i < steps; i++) {
            int k = cur - steps + i;
            float nx = px + (rnd(k, 1) - 0.5F) * R * 0.18F, ny = py + (rnd(k, 2) - 0.5F) * R * 0.18F;
            nx = Math.max(cx - R, Math.min(cx + R, nx));
            ny = Math.max(cy - R, Math.min(cy + R, ny));
            fx.line(px, py, nx, ny, 1F, hot, 0.5F * (i / (float) steps) * lit);
            px = nx;
            py = ny;
        }
        fx.disc(px, py, 4, 14, W, 0.9F * lit, hot, 0.4F);
        for (int i = 0; i < 30; i++)
            dot(cx + (rnd(i, 5) - 0.5F) * 2 * R + sin(t * 9 + i) * 2, cy + (rnd(i, 6) - 0.5F) * 2 * R + cos(t * 11 + i * 2) * 2, 0.8F, 0.5F);
    }

    /** Darts thrown at a square: the fraction inside the quarter circle gives pi. */
    private void monteCarlo() {
        float s = R * 1.6F, x0 = cx - s / 2, y1 = cy + s / 2;
        fx.line(x0, y1, x0 + s, y1, 1F, col, 0.6F);
        fx.line(x0, y1, x0, y1 - s, 1F, col, 0.6F);
        fx.line(x0 + s, y1, x0 + s, y1 - s, 1F, col, 0.4F);
        fx.line(x0, y1 - s, x0 + s, y1 - s, 1F, col, 0.4F);
        fx.band(x0, y1, s - 1, s + 1, 1, (float) (-Math.PI / 2), (float) (Math.PI / 2), 24, hot, 0.7F, hot, 0.7F);
        int n = 20 + (int) (60 * (run ? f : 0.2F)), shift = (int) (t * 3);
        for (int i = 0; i < n; i++) {
            float u = rnd(i + shift, 1), v = rnd(i + shift, 2);
            boolean in = u * u + v * v <= 1;
            fx.disc(x0 + u * s, y1 - v * s, 1.2F, 6, in ? hot : col, (in ? 0.9F : 0.4F) * lit, hot, 0F);
        }
    }

    /** Two D-shaped electrodes; a particle spirals outward, kicked across the gap each half turn. */
    private void cyclotron() {
        fx.band(cx - 2, cy, R * 0.15F, R * 0.95F, 1, (float) (Math.PI / 2), (float) Math.PI, 24, col, 0.15F, col, 0.3F);
        fx.band(cx + 2, cy, R * 0.15F, R * 0.95F, 1, (float) (-Math.PI / 2), (float) Math.PI, 24, col, 0.15F, col, 0.3F);
        float px = cx, py = cy, turns = 4;
        float end = run ? 0.2F + 0.8F * frac(t * 0.2F) : 0.2F;
        for (int i = 1; i <= 200 * end; i++) {
            float th = i / 200F * turns * TAU, r = R * 0.9F * i / 200F;
            float x = cx + r * cos(th), y = cy + r * sin(th);
            fx.line(px, py, x, y, 1F, hot, 0.6F * lit);
            px = x;
            py = y;
        }
        dot(px, py, 2.2F, 1F);
        fx.line(cx, cy - R, cx, cy + R, 1F, W, 0.15F + 0.25F * (0.5F + 0.5F * sin(t * 12)));
    }

    /** Arcs leaping from the coil's toroid to ground, never quite the same path twice. */
    private void tesla() {
        float tx = cx, ty = cy - R * 0.6F;
        ellipse(tx, ty, R * 0.35F, R * 0.12F, 3F, col, 0.7F);
        fx.rect(tx - 4, ty, tx + 4, cy + R, col, 0.4F);
        for (int i = 0; i < 4; i++) fx.line(tx - 6, ty + 10 + i * 8, tx + 6, ty + 12 + i * 8, 1F, col, 0.4F);
        int seed = (int) (t * 8);
        for (int a = 0; a < (run ? 3 : 1); a++) {
            float px = tx + (rnd(seed, a) - 0.5F) * R * 0.6F, py = ty;
            float gx = cx + (rnd(seed, a + 9) - 0.5F) * 2 * R, gy = cy + R;
            for (int s = 1; s <= 8; s++) {
                float u = s / 8F, x = px + (gx - px) / (9 - s) + (rnd(seed + s, a) - 0.5F) * 10, y = ty + (gy - ty) * u;
                fx.glowLine(px, py, x, y, 1F, s % 2 == 0 ? W : hot, 0.8F);
                px = x;
                py = y;
            }
        }
    }

    /** A neutron beam split by a crystal blade into two paths and brought back together; gravity shifts one path's phase. */
    private void neutronPaths() {
        float ax = cx - R * 0.8F, bx = cx, ex = cx + R * 0.8F, dy = R * 0.55F;
        for (float x : new float[]{ax + R * 0.2F, bx, ex - R * 0.2F}) fx.rect(x - 2, cy - R * 0.8F, x + 2, cy + R * 0.8F, col, 0.5F);
        fx.line(cx - R, cy, ax + R * 0.2F, cy, 1.5F, hot, 0.7F * lit);
        float sx = ax + R * 0.2F;
        fx.line(sx, cy, bx, cy - dy, 1F, hot, 0.5F * lit);
        fx.line(sx, cy, bx, cy + dy, 1F, hot, 0.5F * lit);
        fx.line(bx, cy - dy, ex - R * 0.2F, cy, 1F, hot, 0.5F * lit);
        fx.line(bx, cy + dy, ex - R * 0.2F, cy, 1F, hot, 0.5F * lit);
        float u = frac(t * 0.6F);
        dot(sx + (bx - sx) * u, cy - dy * u, 1.5F, 0.9F);
        dot(sx + (bx - sx) * u, cy + dy * u, 1.5F, 0.9F);
        float out = 0.5F + 0.5F * cos(t * 1.5F);
        fx.disc(cx + R, cy, 3, 10, W, out * lit, hot, 0F);
        arrow(cx + R * 0.5F, cy - R * 0.9F, cx + R * 0.5F, cy - R * 0.6F, col, 0.5F);
    }

    /** Fluid speeding up to the right; past the line where it outruns sound, ripples are swept away - a horizon for sound. */
    private void sonicHorizon() {
        float hx = cx + R * 0.1F;
        for (int j = -2; j <= 2; j++) {
            float y = cy + j * R * 0.3F;
            for (int k = 0; k < 6; k++) {
                float u = frac(t * 0.35F + k / 6F), x = cx - R + 2 * R * u * u;
                fx.line(x, y, x + 3 + 8 * u, y, 1F, col, 0.5F);
            }
        }
        fx.line(hx, cy - R, hx, cy + R, 1.5F, hot, 0.8F);
        for (int k = 0; k < 4; k++) {
            float age = frac(t * 0.5F + k / 4F), r = 4 + 18 * age, drift = 30 * age * age;
            float sx = cx - R * 0.6F + k * R * 0.35F;
            fx.ring(sx + drift * (sx > hx ? 2 : 0.5F), cy, r, 1F, 24, W, 0.4F * (1 - age) * lit);
        }
        if (run) {
            float p = frac(t);
            dot(hx - 2 - 6 * p, cy - 6, 1.4F, 1 - p);
            dot(hx + 2 + 10 * p, cy - 6, 1.4F, 1 - p);
        }
    }

    // ------------------------------------------------------------------ astrophysics
    /** An accretion disk seen almost edge-on with jets punching out of both poles, knots travelling along them. */
    private void quasar() {
        ellipse(cx, cy, R * 0.6F, R * 0.15F, 4F, hot, 0.7F);
        ellipse(cx, cy, R * 0.35F, R * 0.08F, 3F, W, 0.6F * lit);
        fx.disc(cx, cy, 3, 10, 0, 1F, 0, 1F);
        for (int s = -1; s <= 1; s += 2) {
            fx.line(cx, cy, cx + s * R * 0.12F, cy + s * R, 2.5F, hot, 0.6F * lit, col, 0F);
            for (int k = 0; k < 3; k++) {
                float u = frac(t * 0.6F + k / 3F);
                dot(cx + s * R * 0.12F * u, cy + s * R * u, 2F, 1 - u);
            }
        }
    }

    /** A tokamak's D-shaped cross section, nested magnetic surfaces, the plasma glowing in the middle. */
    private void tokamak() {
        for (int k = 4; k >= 1; k--) {
            float s = k / 4F;
            float px = 0, py = 0;
            for (int i = 0; i <= 40; i++) {
                double th = i / 40.0 * TAU;
                float x = cx + R * 0.45F * s * (cos(th + 0.35 * Math.sin(th))) + R * 0.1F * s, y = cy + R * 0.8F * s * sin(th);
                if (i > 0) fx.line(px, py, x, y, 1F, k == 1 ? W : Fx2D.lerpRgb(hot, col, s), (0.3F + 0.4F * (1 - s)) * lit);
                px = x;
                py = y;
            }
        }
        fx.disc(cx + R * 0.05F, cy, R * 0.18F, 16, W, 0.6F * lit * (0.8F + 0.2F * sin(t * 7)), hot, 0F);
        for (int i = 0; i < 6; i++) {
            double th = i / 6.0 * TAU + t * 0.2;
            fx.rect(cx + R * 0.8F * cos(th) - 2, cy + R * 0.95F * sin(th) - 2, cx + R * 0.8F * cos(th) + 2, cy + R * 0.95F * sin(th) + 2, col, 0.7F);
        }
        fx.rect(cx - R * 0.75F, cy - R * 0.5F, cx - R * 0.68F, cy + R * 0.5F, col, 0.6F);
    }

    /** A capacitor bank charging, then dumping into the coil in one flash of field. */
    private void pulsedField() {
        float c = frac(t * 0.3F), charge = c < 0.85F ? c / 0.85F : 0, pulse = c >= 0.85F ? 1 - (c - 0.85F) / 0.15F : 0;
        float cx0 = cx - R * 0.8F;
        for (int i = 0; i < 4; i++) {
            float y = cy - R * 0.6F + i * R * 0.4F;
            fx.rect(cx0 - 6, y, cx0 + 6, y + R * 0.3F, col, 0.3F);
            fx.rect(cx0 - 6, y + R * 0.3F * (1 - charge), cx0 + 6, y + R * 0.3F, hot, 0.6F * lit);
        }
        float kx = cx + R * 0.3F;
        for (int i = 0; i < 9; i++) ellipse(kx, cy - R * 0.6F + i * R * 0.15F, R * 0.3F, R * 0.06F, 1.5F, col, 0.6F);
        for (int k = 1; k <= 3; k++) {
            float w = R * 0.15F * k;
            fx.band(kx, cy, w * (1 + pulse), w * (1 + pulse) + 1.2F, 2.6F, 0, TAU, 40, W, pulse * 0.7F * lit, W, pulse * 0.7F * lit);
        }
        fx.line(cx0 + 6, cy, kx - R * 0.3F, cy, 1.5F, pulse > 0 ? W : col, 0.4F + 0.6F * pulse);
    }

    /** Two rails, an armature accelerating between them, and the flash of it striking the target. */
    private void railgun() {
        fx.rect(cx - R, cy - 6, cx + R * 0.7F, cy - 4, col, 0.7F);
        fx.rect(cx - R, cy + 4, cx + R * 0.7F, cy + 6, col, 0.7F);
        fx.rect(cx + R * 0.85F, cy - R * 0.4F, cx + R * 0.95F, cy + R * 0.4F, col, 0.6F);
        float u = frac(t * 0.4F), x = cx - R + (R * 1.85F) * u * u;
        fx.rect(x - 3, cy - 3, x + 3, cy + 3, W, 0.9F * lit);
        for (int i = 1; i < 6; i++) fx.line(x - 3 - i * 4 * u, cy, x - 3 - (i - 1) * 4 * u, cy, 1F, hot, 0.4F * (1 - i / 6F) * lit);
        if (u > 0.92F) {
            float s = (u - 0.92F) / 0.08F;
            fx.disc(cx + R * 0.85F, cy, 4 + 10 * s, 16, W, (1 - s) * lit, hot, 0F);
        }
    }

    /** A sky map: a burst flashes somewhere, fades into its afterglow, and the light curve is plotted below. */
    private void gammaBurst() {
        ellipse(cx, cy - R * 0.2F, R * 0.9F, R * 0.55F, 1.5F, col, 0.5F);
        int n = (int) (t * 0.4F);
        float u = frac(t * 0.4F), bx = cx + (rnd(n, 1) - 0.5F) * R * 1.4F, by = cy - R * 0.2F + (rnd(n, 2) - 0.5F) * R * 0.8F;
        float fl = u < 0.08F ? u / 0.08F : (float) Math.pow((u - 0.07F) / 0.93F + 0.05F, -0.7) * 0.15F;
        fx.disc(bx, by, 2 + 6 * Math.min(1, fl), 16, W, Math.min(1, fl) * lit, hot, 0F);
        float px = cx - R, py = cy + R * 0.9F;
        for (int i = 1; i <= 30; i++) {
            float v = i / 30F, x = cx - R + 2 * R * v;
            float lv = v < 0.08F ? v / 0.08F : (float) Math.pow((v - 0.07F) / 0.93F + 0.05F, -0.7) * 0.15F;
            float y = cy + R * 0.9F - Math.min(1, lv) * R * 0.45F;
            fx.line(px, py, x, y, 1F, v <= u ? hot : col, v <= u ? 0.8F : 0.25F);
            px = x;
            py = y;
        }
    }

    /** A globe under the sun, cloud bands drifting round it, the weather being set. */
    private void climate() {
        fx.disc(cx, cy, R * 0.75F, 32, 0x2060A0, 0.5F, col, 0.3F);
        for (int b = -2; b <= 2; b++) {
            float y = cy + b * R * 0.25F, half = R * 0.75F * (float) Math.sqrt(Math.max(0, 1 - (b * 0.33F) * (b * 0.33F)));
            for (int k = 0; k < 3; k++) {
                float u = frac(t * 0.1F * (b % 2 == 0 ? 1 : -1) + k / 3F + b * 0.2F), x = cx - half + 2 * half * u;
                fx.rect(x - 5, y - 1.5F, x + 5, y + 1.5F, W, 0.4F * lit * sin(Math.PI * u));
            }
        }
        dot(cx - R * 0.95F, cy - R * 0.85F, 5F, 1F);
        for (int i = 0; i < 3; i++) fx.line(cx - R * 0.95F, cy - R * 0.85F, cx - R * 0.3F, cy - R * 0.5F + i * R * 0.3F, 1F, hot, 0.2F * lit);
    }

    /** Light from a far galaxy bent round a mass in front of it into a ring. */
    private void einsteinRing() {
        fx.disc(cx, cy, 4, 12, hot, 0.9F, col, 0F);
        float rr = R * 0.5F * (0.95F + 0.05F * sin(t * 2));
        fx.ring(cx, cy, rr, 2F, 48, W, 0.7F * lit);
        dot(cx + R * 0.95F, cy, 2.5F, 0.9F);
        for (int i = -1; i <= 1; i += 2) {
            float px = cx + R * 0.95F, py = cy;
            for (int s = 1; s <= 16; s++) {
                float u = s / 16F, x = cx + R * 0.95F - 2 * R * 0.95F * u, y = cy + i * rr * sin(Math.PI * u);
                fx.line(px, py, x, y, 1F, hot, 0.4F * lit);
                px = x;
                py = y;
            }
        }
        fx.disc(cx - R * 0.95F, cy, 2, 8, col, 0.8F, col, 0F);
    }

    /** Clocks on rings closer and closer to a mass: the inner ones tick slower. */
    private void dilationClocks() {
        fx.disc(cx, cy, R * 0.12F, 16, 0, 1F, 0, 1F);
        fx.ring(cx, cy, R * 0.13F, 1.5F, 24, hot, 0.7F);
        for (int k = 1; k <= 3; k++) {
            float r = R * (0.15F + 0.27F * k), rate = (float) Math.sqrt(1 - 0.15F / (0.15F + 0.27F * k) * 0.95F);
            fx.ring(cx, cy, r, 1F, 48, col, 0.3F);
            for (int c = 0; c < 2 + k; c++) {
                double a = c * TAU / (2 + k) + k;
                float x = cx + r * cos(a), y = cy + r * sin(a);
                fx.ring(x, y, 4, 1F, 12, col, 0.7F);
                float hand = t * 3 * rate * (run ? 1 : 0.3F);
                fx.line(x, y, x + 3.5F * sin(hand), y - 3.5F * cos(hand), 1F, Fx2D.lerpRgb(hot, W, rate), 0.9F);
            }
        }
    }

    /** A grid of spacetime squeezed in front of a craft and stretched behind it - the craft itself never moves locally. */
    private void warp() {
        for (int i = -6; i <= 6; i++) {
            float base = i / 6F;
            float x = cx + R * base * (base > 0 ? 0.6F + 0.4F * Math.abs(base) : 1.4F - 0.4F * Math.abs(base));
            fx.line(x, cy - R * 0.7F, x, cy + R * 0.7F, 1F, col, 0.35F);
        }
        for (int j = -3; j <= 3; j++) fx.line(cx - R, cy + j * R * 0.22F, cx + R, cy + j * R * 0.22F, 1F, col, 0.2F);
        fx.ring(cx, cy, R * 0.35F, 1.5F, 32, hot, 0.5F * lit * (0.8F + 0.2F * sin(t * 4)));
        fx.tri(cx + 7, cy, W, 0.9F, cx - 5, cy - 5, hot, 0.8F, cx - 5, cy + 5, hot, 0.8F);
        for (int k = 0; k < 6; k++) {
            float u = frac(t * 0.8F + k / 6F);
            dot(cx + R - 2 * R * u, cy + (rnd(k, 3) - 0.5F) * R * 1.2F, 1.1F, 0.6F);
        }
    }

    /** An accelerating observer's hyperbola in a spacetime diagram; the vacuum it sweeps through glows as heat. */
    private void unruh() {
        fx.line(cx - R, cy + R, cx + R, cy - R, 1F, col, 0.4F);
        fx.line(cx - R, cy - R, cx + R, cy + R, 1F, col, 0.4F);
        float a = 0.35F;
        float px = 0, py = 0;
        for (int i = 0; i <= 30; i++) {
            float tau = (i / 30F - 0.5F) * 4, x = cx + R * a * (float) Math.cosh(tau), y = cy - R * a * (float) Math.sinh(tau);
            if (Math.abs(y - cy) > R) continue;
            if (i > 0 && px != 0) fx.line(px, py, x, y, 1.5F, hot, 0.8F);
            px = x;
            py = y;
        }
        float tau = (frac(t * 0.25F) - 0.5F) * 3.2F;
        float ox = cx + R * a * (float) Math.cosh(tau), oy = cy - R * a * (float) Math.sinh(tau);
        dot(ox, oy, 2.5F, 1F);
        for (int k = 0; k < 10; k++) {
            float u = frac(t * 0.7F + rnd(k, 1));
            dot(ox + (rnd(k, 2) - 0.5F) * 20, oy + (rnd(k, 3) - 0.5F) * 20, 1F, (1 - u) * 0.7F);
        }
    }

    /** A tiny hole: pairs appear at its horizon, one falls in, the other escapes as radiation; the hole shrinks. */
    private void hawking() {
        float rh = R * (0.3F - 0.12F * (run ? f : 0));
        fx.disc(cx, cy, rh, 24, 0, 1F, 0, 1F);
        fx.ring(cx, cy, rh + 1, 1.5F, 32, hot, 0.8F);
        for (int k = 0; k < 6; k++) {
            float u = frac(t * 0.5F + k / 6F);
            double a = rnd(k, (int) (t * 0.5F + k / 6F)) * TAU;
            float out = rh + 2 + u * (R - rh), in = rh + 2 - Math.min(u * 6, 2);
            dot(cx + cos(a) * out, cy + sin(a) * out, 1.4F, 1 - u);
            dot(cx + cos(a) * in, cy + sin(a) * in, 1F, Math.max(0, 0.6F - u * 3));
        }
    }

    /** Two mouths joined by a throat; an entangled pair appears at both at once. */
    private void erBridge() {
        float lx = cx - R * 0.7F, rx = cx + R * 0.7F;
        ellipse(lx, cy, R * 0.12F, R * 0.4F, 2F, hot, 0.7F);
        ellipse(rx, cy, R * 0.12F, R * 0.4F, 2F, hot, 0.7F);
        for (int s = -1; s <= 1; s += 2) {
            float px = lx, py = cy + s * R * 0.4F;
            for (int i = 1; i <= 20; i++) {
                float u = i / 20F, x = lx + (rx - lx) * u, y = cy + s * R * (0.4F - 0.28F * sin(Math.PI * u));
                fx.line(px, py, x, y, 1F, col, 0.6F);
                px = x;
                py = y;
            }
        }
        float u = frac(t * 0.4F);
        float a = 0.3F + 0.7F * (float) Math.exp(-u * 6);
        fx.disc(lx, cy, 3, 10, W, a * lit, hot, 0F);
        fx.disc(rx, cy, 3, 10, W, a * lit, hot, 0F);
        fx.line(lx, cy, rx, cy, 1F, W, 0.15F * a * lit);
    }

    /** Three states in a ring with transition arrows; a token hops between them, the occupancy bars filling. */
    private void markov() {
        float[][] p = new float[3][2];
        for (int i = 0; i < 3; i++) {
            double a = -Math.PI / 2 + i * TAU / 3;
            p[i][0] = cx + R * 0.7F * cos(a);
            p[i][1] = cy + R * 0.6F * sin(a);
        }
        for (int i = 0; i < 3; i++) {
            int j = (i + 1) % 3;
            float mx = (p[i][0] * 0.25F + p[j][0] * 0.75F), my = (p[i][1] * 0.25F + p[j][1] * 0.75F);
            arrow(p[i][0], p[i][1], mx, my, col, 0.5F);
            fx.line(mx, my, p[j][0], p[j][1], 1F, col, 0.5F);
            fx.ring(p[i][0], p[i][1], 7, 1.5F, 20, col, 0.7F);
        }
        int hop = (int) (t * 1.5F);
        int at = 0;
        for (int k = 0; k < hop % 50; k++) if (rnd(k, 9) < 0.6F) at = (at + 1) % 3;
        fx.disc(p[at][0], p[at][1], 4, 12, W, 0.9F * lit, hot, 0F);
        for (int i = 0; i < 3; i++) fx.rect(p[i][0] - 6, p[i][1] + 10, p[i][0] - 6 + 12 * (0.3F + 0.2F * i), p[i][1] + 12, hot, 0.6F);
    }

    // ------------------------------------------------------------------ beyond physics
    /** A tear in space, jagged and widening, things leaking through it. */
    private void rift() {
        float open = run ? 0.3F + 0.7F * f : 0.2F;
        float px = cx, py = cy - R;
        for (int i = 1; i <= 10; i++) {
            float y = cy - R + 2 * R * i / 10F, x = cx + (rnd(i, 1) - 0.5F) * R * 0.4F;
            float w = R * 0.15F * open * sin(Math.PI * i / 10F);
            fx.line(px - w, py, x - w, y, 1.5F, hot, 0.8F);
            fx.line(px + w, py, x + w, y, 1.5F, hot, 0.8F);
            px = x;
            py = y;
        }
        for (int k = 0; k < 10; k++) {
            float u = frac(t * 0.4F + k / 10F);
            dot(cx + (k % 2 == 0 ? 1 : -1) * u * R, cy + (rnd(k, 2) - 0.5F) * R * 1.6F, 1.3F, 1 - u);
        }
    }

    /** A parent bubble budding daughter universes that inflate and drift off. */
    private void bubbles() {
        fx.ring(cx, cy, R * 0.45F, 1.5F, 40, col, 0.7F);
        for (int k = 0; k < 4; k++) {
            float u = frac(t * 0.2F + k / 4F);
            double a = k * TAU / 4 + 0.6;
            float d = R * 0.45F + u * R * 0.5F, r = 2 + u * R * 0.3F;
            fx.ring(cx + d * cos(a), cy + d * sin(a), r, 1F, 24, hot, (1 - u * 0.6F) * lit);
            fx.line(cx + R * 0.45F * cos(a), cy + R * 0.45F * sin(a), cx + (d - r) * cos(a), cy + (d - r) * sin(a), 1F, hot, 0.4F * (1 - u));
        }
    }

    /** A closed loop in time: a clock running round a worldline that comes back to where it began. */
    private void timeLoop() {
        fx.ring(cx, cy, R * 0.7F, 1.5F, 48, col, 0.6F);
        float a = t * 0.8F;
        for (int k = 0; k < 8; k++) dot(cx + R * 0.7F * cos(a - k * 0.15F), cy + R * 0.7F * sin(a - k * 0.15F), 2.2F - k * 0.2F, 1 - k / 8F);
        fx.ring(cx, cy, 8, 1F, 20, hot, 0.7F);
        fx.line(cx, cy, cx + 6 * sin(t * 4), cy - 6 * cos(t * 4), 1F, W, 0.8F);
        fx.line(cx, cy, cx + 4 * sin(t * 0.33F), cy - 4 * cos(t * 0.33F), 1.5F, W, 0.8F);
        arrow(cx + R * 0.7F * cos(a + 0.4F), cy + R * 0.7F * sin(a + 0.4F), cx + R * 0.7F * cos(a + 0.6F), cy + R * 0.7F * sin(a + 0.6F), hot, 0.6F);
    }

    /** Galaxies on a grid all moving apart, the farther the faster, reddening as they go (Hubble's law). */
    private void hubbleFlow() {
        float scale = 0.6F + 0.4F * frac(t * 0.15F);
        for (int i = -3; i <= 3; i++)
            for (int j = -3; j <= 3; j++) {
                float x = cx + i * R * 0.3F * scale, y = cy + j * R * 0.3F * scale;
                if (Math.abs(x - cx) > R || Math.abs(y - cy) > R) continue;
                float d = (float) Math.hypot(i, j) / 4.3F;
                fx.disc(x, y, 1.6F, 8, Fx2D.lerpRgb(0xA0C0FF, 0xFF6040, d), 0.9F * lit, hot, 0F);
                if (d > 0) fx.line(x, y, x + i * 1.5F * d, y + j * 1.5F * d, 1F, hot, 0.3F * lit);
            }
    }

    /** Columns of code falling, rewriting what lies beneath. */
    private void digitalRain() {
        for (int c = 0; c < 10; c++) {
            float x = cx - R + (c + 0.5F) * R * 0.2F, head = frac(t * (0.3F + 0.4F * rnd(c, 1)) + rnd(c, 2));
            for (int k = 0; k < 8; k++) {
                float u = head - k * 0.06F;
                if (u < 0) continue;
                float y = cy - R + 2 * R * u;
                fx.rect(x - 1.5F, y - 2, x + 1.5F, y + 2, k == 0 ? W : hot, (1 - k / 8F) * 0.8F * lit);
            }
        }
    }

    /** A wall of axioms, one block cracking out of it and an opening widening behind. */
    private void breach() {
        float open = run ? f : 0.1F;
        for (int i = -3; i <= 3; i++)
            for (int j = -3; j <= 3; j++) {
                float d = (float) Math.hypot(i, j);
                if (d < open * 3.5F) continue;
                float x = cx + i * R * 0.27F + (j % 2 == 0 ? 0 : R * 0.13F), y = cy + j * R * 0.27F;
                fx.rect(x - R * 0.12F, y - R * 0.1F, x + R * 0.12F, y + R * 0.1F, col, 0.35F);
            }
        fx.disc(cx, cy, R * 0.9F * open + 2, 24, W, 0.4F * lit, hot, 0F);
        for (int k = 0; k < 8; k++) {
            double a = k * TAU / 8 + t * 0.3;
            float r = R * 0.9F * open + 4 + 6 * frac(t + k * 0.1F);
            dot(cx + r * cos(a), cy + r * sin(a), 1.2F, 0.8F);
        }
    }

    /** Laws nested inside laws: polygons of three to eight sides turning against each other. */
    private void nestedLaws() {
        for (int n = 3; n <= 8; n++) {
            float r = R * (0.2F + 0.13F * (n - 3)), rot = t * 0.3F * (n % 2 == 0 ? 1 : -1) / (n - 2);
            fx.polygon(cx, cy, r, n, rot, 1.2F, Fx2D.lerpRgb(hot, col, (n - 3) / 5F), (0.4F + 0.4F * (1 - (n - 3) / 5F)) * lit);
        }
        dot(cx, cy, 2.5F, 0.6F + 0.4F * sin(t * 3));
    }

    /** Many ideas drawn in and crushed into one point, which flares when the concept is complete. */
    private void compress() {
        for (int k = 0; k < 24; k++) {
            float u = frac(t * 0.3F + rnd(k, 1));
            double a = rnd(k, 2) * TAU;
            float r = R * (1 - u);
            fx.disc(cx + r * cos(a), cy + r * sin(a), 1.3F, 6, Fx2D.lerpRgb(col, W, u), 0.8F * lit, hot, 0F);
        }
        float fl = run ? (float) Math.pow(f, 4) : 0;
        fx.disc(cx, cy, 3 + 8 * fl, 16, W, 0.5F + 0.5F * fl, hot, 0F);
    }

    /** Spacetime at the Planck scale: bubbles forming and popping everywhere at once. */
    private void foam() {
        for (int k = 0; k < 26; k++) {
            float u = frac(t * (0.5F + rnd(k, 3)) + rnd(k, 4));
            int gen = (int) (t * (0.5F + rnd(k, 3)) + rnd(k, 4));
            float x = cx + (rnd(k, gen) - 0.5F) * 2 * R, y = cy + (rnd(k + 50, gen) - 0.5F) * 2 * R;
            float r = 1 + 7 * u;
            fx.ring(x, y, r, 1F, 14, u > 0.9F ? W : hot, (1 - u) * 0.7F * lit);
        }
    }

    // ------------------------------------------------------------------ rings, wells and stars
    /** Matter and antimatter streaming in from both sides, meeting in a flash and leaving as gamma rays. */
    private void annihilation() {
        for (int k = 0; k < 6; k++) {
            float u = frac(t * 0.5F + k / 6F);
            fx.disc(cx - R + R * u, cy + (rnd(k, 1) - 0.5F) * 6, 1.6F, 8, 0x60A0FF, 0.9F * lit, hot, 0F);
            fx.disc(cx + R - R * u, cy + (rnd(k, 2) - 0.5F) * 6, 1.6F, 8, 0xFF6060, 0.9F * lit, hot, 0F);
        }
        float fl = 0.6F + 0.4F * sin(t * 9);
        fx.disc(cx, cy, 6 * fl, 14, W, fl * lit, hot, 0F);
        for (int k = 0; k < 8; k++) {
            double a = k * TAU / 8 + 0.4;
            float u = frac(t * 0.8F + k * 0.125F);
            wave(cx + 6 * cos(a), cy + 6 * sin(a), cx + (6 + R * 0.8F * u) * cos(a), cy + (6 + R * 0.8F * u) * sin(a), 1.5F, 4, t * 10, W, 0.4F * (1 - u) * lit);
        }
    }

    /** A spinning neutron star sweeping its two beams round like a lighthouse; the pulse train it makes below. */
    private void pulsar() {
        fx.disc(cx, cy - R * 0.15F, 4, 12, W, 1F, hot, 0.5F);
        double a = t * 2.5;
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * R * 0.9F * cos(a), ey = cy - R * 0.15F + s * R * 0.6F * sin(a);
            fx.line(cx, cy - R * 0.15F, ex, ey, 3F, hot, 0.6F * lit, col, 0F);
        }
        float base = cy + R * 0.85F, px = cx - R, py = base;
        for (int i = 1; i <= 60; i++) {
            float u = i / 60F, x = cx - R + 2 * R * u;
            float ph = frac(u * 3 - t * 2.5F / TAU * 2);
            float y = base - (ph < 0.06F ? R * 0.3F : 0);
            fx.line(px, py, x, y, 1F, hot, 0.7F);
            px = x;
            py = y;
        }
    }

    /** A storage ring of electrons throwing off fans of light at each bend, down the beamlines. */
    private void synchrotron() {
        fx.polygon(cx, cy, R * 0.6F, 8, (float) (Math.PI / 8), 2F, col, 0.6F);
        double a = t * 1.2;
        float bx = cx + R * 0.6F * cos(a), by = cy + R * 0.6F * sin(a);
        dot(bx, by, 2.5F, 1F);
        for (int k = 0; k < 8; k++) {
            double c = (k + 0.5) * TAU / 8;
            float x = cx + R * 0.55F * cos(c), y = cy + R * 0.55F * sin(c);
            double tan = c + Math.PI / 2;
            float near = (float) Math.abs(Math.atan2(Math.sin(a - c), Math.cos(a - c)));
            float b = near < 0.4F ? 1 - near / 0.4F : 0;
            fx.line(x, y, x + R * 0.5F * cos(tan), y + R * 0.5F * sin(tan), 1.5F, W, (0.15F + 0.7F * b) * lit, hot, 0F);
        }
    }

    /** A galaxy cluster's unseen halo of dark matter, betrayed only by the arcs it bends background galaxies into. */
    private void darkLens() {
        for (int k = 0; k < 6; k++) fx.disc(cx + (rnd(k, 1) - 0.5F) * R * 0.5F, cy + (rnd(k, 2) - 0.5F) * R * 0.5F, 2, 8, col, 0.9F, col, 0F);
        for (int k = 0; k < 24; k++) {
            double a = k * TAU / 24 + t * 0.05;
            if (k % 2 == 0) fx.line(cx + R * 0.75F * cos(a), cy + R * 0.75F * sin(a), cx + R * 0.75F * cos(a + 0.13), cy + R * 0.75F * sin(a + 0.13), 1F, col, 0.4F);
        }
        for (int k = 0; k < 4; k++) {
            double a = k * TAU / 4 + 0.7;
            fx.band(cx, cy, R * 0.5F, R * 0.55F, 1, (float) a, 0.5F, 10, hot, 0.8F * lit, hot, 0.3F * lit);
        }
    }

    /** The embedding diagram of a wormhole: two funnels whose throats meet, something passing through. */
    private void wormhole() {
        for (int s = -1; s <= 1; s += 2)
            for (int k = 0; k < 5; k++) {
                float u = k / 4F, rr = R * (0.2F + 0.75F * u * u), y = cy + s * R * 0.75F * u;
                ellipse(cx, y, rr, rr * 0.25F, 1F, Fx2D.lerpRgb(hot, col, u), 0.6F);
            }
        float px = cx - R * 0.95F, py = cy - R * 0.75F;
        for (int s = -1; s <= 1; s += 2) {
            float qx = 0, qy = 0;
            for (int i = 0; i <= 20; i++) {
                float v = -1 + 2 * i / 20F, x = cx + s * R * (0.2F + 0.75F * v * v), y = cy + R * 0.75F * v;
                if (i > 0) fx.line(qx, qy, x, y, 1F, col, 0.5F);
                qx = x;
                qy = y;
            }
        }
        float u = frac(t * 0.3F) * 2 - 1;
        dot(cx, cy + R * 0.75F * u, 2.2F, 1F);
        dot(px, py, 0F, 0F);
    }

    /** An eye that looks back: iris rings of an accretion disk round a pupil that is a horizon. */
    private void eye() {
        float look = 4 * sin(t * 0.4F);
        fx.band(cx, cy, R * 0.95F, R, 0.5F, 0, TAU, 48, col, 0.6F, col, 0.6F);
        for (int k = 0; k < 6; k++) fx.ring(cx + look, cy, R * (0.18F + 0.06F * k), 1F, 36, Fx2D.lerpRgb(W, hot, k / 6F), 0.5F * lit);
        for (int k = 0; k < 18; k++) {
            double a = k * TAU / 18 + t * 0.2;
            fx.line(cx + look + R * 0.2F * cos(a), cy + R * 0.2F * sin(a), cx + look + R * 0.5F * cos(a), cy + R * 0.5F * sin(a), 1F, hot, 0.3F * lit);
        }
        fx.disc(cx + look, cy, R * 0.15F, 20, 0, 1F, 0, 1F);
    }

    /** Streams of matter poured onto a star and hammered into it by rings of field. */
    private void starForge() {
        fx.disc(cx, cy, R * 0.35F, 32, W, 0.9F * lit, hot, 0.5F);
        for (int k = 0; k < 4; k++) {
            double a = k * TAU / 4 + t * 0.15;
            for (int i = 0; i < 5; i++) {
                float u = frac(t * 0.5F + i / 5F), r = R * (1 - 0.6F * u);
                dot(cx + r * cos(a + u * 0.8), cy + r * sin(a + u * 0.8), 1.4F, u);
            }
        }
        for (int k = 0; k < 3; k++) {
            float r = R * (0.5F + 0.12F * k) * (1 + 0.05F * sin(t * 6 - k));
            fx.band(cx, cy, r, r + 1.5F, 0.4F + 0.3F * k, (float) (t * 0.2 * (k + 1)), TAU, 40, hot, 0.5F * lit, col, 0.4F * lit);
        }
    }
}
