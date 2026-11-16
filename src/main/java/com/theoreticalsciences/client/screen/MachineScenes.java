package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.transit.Fx2D;

import java.util.Locale;
import java.util.Map;

/**
 * 1.5.1: a simulation of its own for every machine added since 1.3.0. They had no effect family, so every one of them
 * fell back to the stirred beaker ("reaction vessel"). Each scene below draws what that machine actually does, in the
 * console's additive light, around (cx, cy) inside a radius R, from the console's phase t and the run's progress f.
 */
final class MachineScenes {
    private final Fx2D fx;
    private final float cx, cy, R, f, t, lit;
    private final int col, hot;
    private final boolean run;
    private static final float TAU = (float) (Math.PI * 2);
    private static final int W = 0xFFFFFF;

    /** The title shown over each scene (in place of "reaction vessel"). */
    static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("ancient_dna_lab", "genome assembly"), Map.entry("automaton_lattice", "cellular automaton"),
            Map.entry("breeding_bench", "selection response"), Map.entry("bussard_ramscoop", "magnetic scoop"),
            Map.entry("certificate_verifier", "interactive proof"), Map.entry("chaos_controller", "lorenz attractor"),
            Map.entry("composite_layup_press", "ply layup"), Map.entry("connectome_array", "connectome"),
            Map.entry("corrosion_cell", "pourbaix map"), Map.entry("ct_scanner", "back-projection"),
            Map.entry("dating_spectrometer", "decay clock"), Map.entry("decipherment_desk", "symbol frequencies"),
            Map.entry("dose_response_assay", "hill curve"), Map.entry("geothermal_plant", "geothermal loop"),
            Map.entry("heliograph", "solar wind"), Map.entry("hydroponic_tower", "nutrient film"),
            Map.entry("ice_core_drill", "ice core"), Map.entry("knot_loom", "trefoil"), Map.entry("load_test_rig", "euler buckling"),
            Map.entry("neural_culture_array", "saltatory conduction"), Map.entry("oneill_cylinder", "spin gravity"),
            Map.entry("phytotron", "photosynthesis"), Map.entry("psychophysics_booth", "signal detection"),
            Map.entry("quantum_computer", "grover amplification"), Map.entry("resonance_chamber", "standing wave"),
            Map.entry("revival_vat", "gestation"), Map.entry("rocket_assembly", "staging"), Map.entry("space_elevator", "tether climb"),
            Map.entry("stellar_engine", "shkadov thrust"), Map.entry("storm_chamber", "thunderstorm"),
            Map.entry("synapse_trainer", "hebbian learning"), Map.entry("tribometer", "pin on disc"),
            Map.entry("wave_energy_converter", "ocean swell"), Map.entry("wind_tunnel", "streamlines"));

    private MachineScenes(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase) {
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

    static String title(String id) {
        return TITLES.get(id);
    }

    static boolean draw(Fx2D fx, String id, float cx, float cy, float R, int col, int hot, boolean run, float f, float phase) {
        MachineScenes s = new MachineScenes(fx, cx, cy, R, col, hot, run, f, phase);
        switch (id.toLowerCase(Locale.ROOT)) {
            case "ancient_dna_lab" -> s.genome();
            case "automaton_lattice" -> s.automaton();
            case "breeding_bench" -> s.breeding();
            case "bussard_ramscoop" -> s.ramscoop();
            case "certificate_verifier" -> s.proof();
            case "chaos_controller" -> s.lorenz();
            case "composite_layup_press" -> s.layup();
            case "connectome_array" -> s.connectome();
            case "corrosion_cell" -> s.pourbaix();
            case "ct_scanner" -> s.ct();
            case "dating_spectrometer" -> s.decay();
            case "decipherment_desk" -> s.zipf();
            case "dose_response_assay" -> s.hill();
            case "geothermal_plant" -> s.geothermal();
            case "heliograph" -> s.solarWind();
            case "hydroponic_tower" -> s.hydroponics();
            case "ice_core_drill" -> s.iceCore();
            case "knot_loom" -> s.trefoil();
            case "load_test_rig" -> s.buckling();
            case "neural_culture_array" -> s.saltatory();
            case "oneill_cylinder" -> s.oneill();
            case "phytotron" -> s.photosynthesis();
            case "psychophysics_booth" -> s.signalDetection();
            case "quantum_computer" -> s.grover();
            case "resonance_chamber" -> s.standingWave();
            case "revival_vat" -> s.gestation();
            case "rocket_assembly" -> s.rocket();
            case "space_elevator" -> s.elevator();
            case "stellar_engine" -> s.shkadov();
            case "storm_chamber" -> s.storm();
            case "synapse_trainer" -> s.hebb();
            case "tribometer" -> s.pinOnDisc();
            case "wave_energy_converter" -> s.swell();
            case "wind_tunnel" -> s.windTunnel();
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

    private static float rnd(int i, int j) {
        int h = i * 374761393 + j * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0x1000000;
    }

    private float p() {
        return run ? f : 0;
    }

    private void dot(float x, float y, float r, float a) {
        fx.disc(x, y, r, 8, W, a * lit, hot, 0F);
    }

    private void blob(float x, float y, float r, int c, float a) {
        fx.disc(x, y, r, 12, c, a, c, a * 0.3F);
    }

    private void ellipse(float x, float y, float rx, float ry, float w, int c, float a) {
        fx.band(x, y, rx - w / 2, rx + w / 2, ry / Math.max(rx, 0.01F), 0, TAU, 40, c, a, c, a);
    }

    private void arc(float x, float y, float r, float a0, float sweep, float w, int c, float a) {
        fx.band(x, y, r - w / 2, r + w / 2, 1, a0, sweep, 24, c, a, c, a);
    }

    private void arrow(float x0, float y0, float x1, float y1, int c, float a) {
        fx.line(x0, y0, x1, y1, 1F, c, a);
        float ang = (float) Math.atan2(y1 - y0, x1 - x0);
        fx.line(x1, y1, x1 - 4 * cos(ang - 0.5), y1 - 4 * sin(ang - 0.5), 1F, c, a);
        fx.line(x1, y1, x1 - 4 * cos(ang + 0.5), y1 - 4 * sin(ang + 0.5), 1F, c, a);
    }

    private void axes(float x0, float y0, float x1, float y1) {
        fx.line(x0, y1, x1, y1, 1F, col, 0.5F);
        fx.line(x0, y0, x0, y1, 1F, col, 0.5F);
    }

    /** Plots y = fn(u) for u in [0, 1] inside a box. */
    private void plot(float x0, float y0, float x1, float y1, java.util.function.DoubleUnaryOperator fn, int c, float a) {
        float px = x0, py = y1 - (y1 - y0) * (float) fn.applyAsDouble(0);
        for (int i = 1; i <= 40; i++) {
            float u = i / 40F, qx = x0 + (x1 - x0) * u, qy = y1 - (y1 - y0) * (float) fn.applyAsDouble(u);
            fx.line(px, py, qx, qy, 1.2F, c, a);
            px = qx;
            py = qy;
        }
    }

    // ------------------------------------------------------------------ the scenes
    /** Short fragments drifting in and locking into a growing double helix. */
    private void genome() {
        float L = R * 1.8F, x0 = cx - L / 2;
        float done = 0.2F + 0.8F * p();
        for (int i = 0; i < 48; i++) {
            float u = i / 47F, x = x0 + L * u;
            float a = u * TAU * 3 + t;
            float y1 = cy + sin(a) * R * 0.25F, y2 = cy - sin(a) * R * 0.25F;
            boolean have = u <= done;
            if (i > 0) {
                float up = (i - 1) / 47F, xp = x0 + L * up, ap = up * TAU * 3 + t;
                fx.line(xp, cy + sin(ap) * R * 0.25F, x, y1, 1.2F, have ? hot : col, have ? 0.8F : 0.25F);
                fx.line(xp, cy - sin(ap) * R * 0.25F, x, y2, 1.2F, have ? col : col, have ? 0.6F : 0.2F);
            }
            if (have && i % 3 == 0) fx.line(x, y1, x, y2, 0.8F, col, 0.35F);
        }
        for (int k = 0; k < 5; k++) {                                 // fragments arriving
            float u = frac(t * 0.4F + k / 5F);
            float x = x0 + L * done + (1 - u) * R * 0.6F, y = cy - R * 0.8F + u * R * 0.8F + (k - 2) * 3;
            fx.line(x - 5, y, x + 5, y + 2, 1.2F, hot, 0.6F * lit * (1 - u));
        }
    }

    /** A grid of cells running a glider gun: gliders march diagonally across. */
    private void automaton() {
        int n = 14;
        float s = R * 1.7F / n, x0 = cx - s * n / 2, y0 = cy - s * n / 2;
        int step = (int) (t * 4);
        for (int i = 0; i <= n; i++) {
            fx.line(x0 + i * s, y0, x0 + i * s, y0 + n * s, 0.5F, col, 0.12F);
            fx.line(x0, y0 + i * s, x0 + n * s, y0 + i * s, 0.5F, col, 0.12F);
        }
        int[][][] phases = {{{1, 0}, {2, 1}, {0, 2}, {1, 2}, {2, 2}}, {{0, 1}, {2, 1}, {1, 2}, {2, 2}, {1, 3}},
                {{2, 1}, {0, 2}, {2, 2}, {1, 3}, {2, 3}}, {{1, 1}, {2, 2}, {3, 2}, {1, 3}, {2, 3}}};
        for (int g = 0; g < 3; g++) {
            int gen = step + g * 9, cycle = gen / 4;
            int[][] cells = phases[gen % 4];
            for (int[] c : cells) {
                int gx = (c[0] + cycle + g * 4) % n, gy = (c[1] + cycle + g * 2) % n;
                fx.rect(x0 + gx * s + 1, y0 + gy * s + 1, x0 + (gx + 1) * s - 1, y0 + (gy + 1) * s - 1, g == 0 ? hot : col, 0.7F * lit);
            }
        }
    }

    /** A bell curve of a trait, the selected tail shaded, and the next generation's curve moving right. */
    private void breeding() {
        float x0 = cx - R, x1 = cx + R, y0 = cy - R * 0.7F, y1 = cy + R * 0.7F;
        axes(x0, y0, x1, y1);
        float shift = 0.18F * p();
        plot(x0, y0, x1, y1, u -> Math.exp(-Math.pow((u - 0.4) / 0.13, 2)), col, 0.6F);
        plot(x0, y0, x1, y1, u -> Math.exp(-Math.pow((u - 0.4 - shift) / 0.13, 2)), hot, 0.9F * lit);
        float cut = x0 + (x1 - x0) * 0.55F;
        fx.rect(cut, y0, x1, y1, hot, 0.08F);
        fx.line(cut, y0, cut, y1, 1F, hot, 0.5F);
        for (int i = 0; i < 4; i++) {                                     // wheat heads along the bottom
            float x = x0 + 10 + i * (x1 - x0 - 20) / 3, h = 6 + 6 * p();
            fx.line(x, y1 - 2, x, y1 - 2 - h, 1F, 0x90D070, 0.6F);
            blob(x, y1 - 3 - h, 2F, 0xE0C060, 0.6F * lit);
        }
    }

    /** A funnel of magnetic field lines, interstellar hydrogen streaming in down it. */
    private void ramscoop() {
        for (int k = -3; k <= 3; k++) {
            float y = cy + k * R * 0.25F;
            fx.line(cx - R, y, cx + R * 0.2F, cy + k * 2.5F, 1F, col, 0.45F);
        }
        fx.rect(cx + R * 0.2F, cy - 8, cx + R, cy + 8, col, 0.35F);
        for (int i = 0; i < 18; i++) {
            float u = frac(t * 0.5F + rnd(i, 1)), k = (rnd(i, 2) - 0.5F) * 6;
            float x = cx - R + u * R * 1.2F, y = cy + k * R * 0.25F * (1 - u) + k * 2.5F * u;
            dot(x, y, 1.2F, 0.8F);
        }
        blob(cx + R * 0.7F, cy, 5F + 2 * sin(t * 6), hot, 0.5F * lit);
    }

    /** A prover and a verifier: challenges go over, answers come back, a tick for each round passed. */
    private void proof() {
        float lx = cx - R * 0.75F, rx = cx + R * 0.75F;
        fx.rect(lx - 10, cy - 14, lx + 10, cy + 14, col, 0.3F);
        fx.rect(rx - 10, cy - 14, rx + 10, cy + 14, col, 0.3F);
        float u = frac(t * 0.7F);
        boolean back = u > 0.5F;
        float k = back ? (u - 0.5F) * 2 : u * 2;
        float x = back ? rx - 12 - (rx - lx - 24) * k : lx + 12 + (rx - lx - 24) * k;
        arrow(lx + 12, cy - 5, rx - 12, cy - 5, col, 0.3F);
        arrow(rx - 12, cy + 5, lx + 12, cy + 5, col, 0.3F);
        dot(x, back ? cy + 5 : cy - 5, 2.2F, 1F);
        int rounds = (int) (t * 0.7F) % 10;
        for (int i = 0; i < 10; i++) {
            float bx = cx - R * 0.8F + i * R * 0.18F, by = cy + R * 0.7F;
            fx.rect(bx, by, bx + R * 0.12F, by + 4, i < rounds ? hot : col, i < rounds ? 0.7F * lit : 0.15F);
        }
    }

    /** The Lorenz butterfly, a bright bead running round it, small kicks flashing as the controller holds it. */
    private void lorenz() {
        double x = 1, y = 1, z = 1, dt = 0.01;
        float px = 0, py = 0;
        int head = (int) (t * 120) % 1500;
        for (int i = 0; i < 1500; i++) {
            double dx = 10 * (y - x), dy = x * (28 - z) - y, dz = x * y - 8.0 / 3 * z;
            x += dx * dt;
            y += dy * dt;
            z += dz * dt;
            float sx = cx + (float) x * R / 22, sy = cy + R * 0.9F - (float) z * R / 28;
            if (i > 0) {
                float age = ((head - i) % 1500 + 1500) % 1500 / 1500F;
                fx.line(px, py, sx, sy, 0.8F, age < 0.1F ? hot : col, (age < 0.1F ? 0.9F : 0.25F) * (i % 2 == 0 ? 1 : 0.8F));
            }
            if (i == head) dot(sx, sy, 2.4F, 1F);
            px = sx;
            py = sy;
        }
    }

    /** Plies laid at 0, 45, 90 and -45 degrees, the stack growing, a heated platen coming down. */
    private void layup() {
        float w = R * 1.4F, h = R * 0.12F, x0 = cx - w / 2, yb = cy + R * 0.6F;
        int plies = 1 + (int) (p() * 7);
        float[] ang = {0, 0.785F, 1.571F, -0.785F};
        for (int i = 0; i < plies; i++) {
            float y = yb - i * h;
            fx.rect(x0, y - h + 1, x0 + w, y, col, 0.25F);
            float a = ang[i % 4];
            for (int k = 0; k < 10; k++) {
                float xx = x0 + 4 + k * (w - 8) / 9;
                fx.line(xx - cos(a) * 4, y - h / 2 - sin(a) * 2, xx + cos(a) * 4, y - h / 2 + sin(a) * 2, 0.8F, hot, 0.5F * lit);
            }
        }
        float top = yb - plies * h - 6 - 6 * (1 - p());
        fx.rect(x0 - 4, top - 6, x0 + w + 4, top, Fx2D.lerpRgb(col, 0xFF8040, p()), 0.6F);
        for (int i = 0; i < 4; i++) arrow(x0 + w * (i + 0.5F) / 4, top - 16, x0 + w * (i + 0.5F) / 4, top - 7, hot, 0.5F * lit);
    }

    /** A network of neurons, waves of firing spreading through it from module to module. */
    private void connectome() {
        int n = 18;
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            int m = i % 3;
            float mx = cx + cos(m * TAU / 3 - 1.57F) * R * 0.55F, my = cy + sin(m * TAU / 3 - 1.57F) * R * 0.55F;
            xs[i] = mx + (rnd(i, 3) - 0.5F) * R * 0.6F;
            ys[i] = my + (rnd(i, 4) - 0.5F) * R * 0.6F;
        }
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                boolean same = i % 3 == j % 3;
                if (!same && rnd(i, j) > 0.12F) continue;
                if (same && rnd(i, j) > 0.55F) continue;
                fx.line(xs[i], ys[i], xs[j], ys[j], 0.7F, col, same ? 0.3F : 0.5F);
            }
        float wave = frac(t * 0.35F) * 3;
        for (int i = 0; i < n; i++) {
            float d = Math.abs(i % 3 - wave);
            float a = Math.max(0, 1 - d * 1.5F);
            blob(xs[i], ys[i], 2F + 2 * a, Fx2D.lerpRgb(col, W, a), (0.4F + 0.6F * a) * lit);
        }
    }

    /** Potential against pH: immunity below, passivity in its window, corrosion elsewhere, the test point wandering. */
    private void pourbaix() {
        float x0 = cx - R, x1 = cx + R, y0 = cy - R * 0.8F, y1 = cy + R * 0.8F;
        axes(x0, y0, x1, y1);
        fx.quad(x0, y1, 0x40A060, 0.2F, x1, y1, 0x40A060, 0.2F, x1, cy + R * 0.35F, 0x40A060, 0.2F, x0, cy + R * 0.15F, 0x40A060, 0.2F);
        fx.rect(x0 + (x1 - x0) * 0.62F, y0, x0 + (x1 - x0) * 0.92F, cy + R * 0.25F, 0x8080C0, 0.2F);
        fx.rect(x0, y0, x0 + (x1 - x0) * 0.62F, cy + R * 0.15F, 0xC06040, 0.12F);
        fx.line(x0, cy - R * 0.1F, x1, cy + R * 0.3F, 1F, hot, 0.4F);
        float px = cx + cos(t * 0.6F) * R * 0.6F, py = cy + sin(t * 0.9F) * R * 0.5F;
        dot(px, py, 2.6F, 1F);
        for (int i = 0; i < 6; i++) {                                     // bubbles at the electrode
            float u = frac(t + i / 6F);
            blob(x1 - 6 + (i % 2) * 3, y1 - u * R * 1.4F, 1.2F, W, 0.4F * (1 - u) * lit);
        }
    }

    /** The gantry turning, a fan beam from the tube to the detectors, the slice filling in behind it. */
    private void ct() {
        float a = t * 2.5F;
        fx.ring(cx, cy, R * 0.92F, 3F, 48, col, 0.5F);
        float sx = cx + cos(a) * R * 0.92F, sy = cy + sin(a) * R * 0.92F;
        for (int k = -4; k <= 4; k++) {
            float b = a + (float) Math.PI + k * 0.12F;
            fx.line(sx, sy, cx + cos(b) * R * 0.92F, cy + sin(b) * R * 0.92F, 0.8F, hot, 0.25F * lit);
        }
        blob(sx, sy, 3.5F, hot, 0.9F * lit);
        arc(cx, cy, R * 0.92F, a + (float) Math.PI - 0.55F, 1.1F, 4F, W, 0.6F * lit);
        float fill = 0.25F + 0.75F * p();
        fx.disc(cx, cy, R * 0.55F, 32, 0x9098A8, 0.35F * fill, 0x9098A8, 0.25F * fill);
        blob(cx - R * 0.15F, cy - R * 0.1F, R * 0.15F, W, 0.5F * fill);
        blob(cx + R * 0.2F, cy + R * 0.12F, R * 0.08F, hot, 0.6F * fill);
    }

    /** An exponential decay curve, nuclei winking out, and the age read off where the curve stands. */
    private void decay() {
        float x0 = cx - R, x1 = cx + R * 0.2F, y0 = cy - R * 0.7F, y1 = cy + R * 0.7F;
        axes(x0, y0, x1, y1);
        plot(x0, y0, x1, y1, u -> Math.exp(-3 * u), col, 0.7F);
        float u = 0.15F + 0.75F * p();
        float py = y1 - (y1 - y0) * (float) Math.exp(-3 * u);
        fx.line(x0 + (x1 - x0) * u, y1, x0 + (x1 - x0) * u, py, 1F, hot, 0.6F);
        dot(x0 + (x1 - x0) * u, py, 2.4F, 1F);
        for (int i = 0; i < 16; i++) {
            float bx = cx + R * 0.45F + (i % 4) * R * 0.15F, by = cy - R * 0.45F + (i / 4) * R * 0.3F;
            boolean alive = rnd(i, 9) < Math.exp(-3 * u);
            blob(bx, by, 2.5F, alive ? hot : col, alive ? 0.8F * lit : 0.15F);
        }
    }

    /** Symbol counts sorted by rank, falling off as 1/rank (Zipf), glyph rows above. */
    private void zipf() {
        float x0 = cx - R, x1 = cx + R, yb = cy + R * 0.75F;
        int n = 16;
        for (int i = 0; i < n; i++) {
            float h = R * 1.0F / (i + 1) * (0.4F + 0.6F * Math.min(1, p() * 1.5F + 0.2F));
            float x = x0 + i * (x1 - x0) / n;
            fx.rect(x + 1, yb - h, x + (x1 - x0) / n - 1, yb, i == (int) (t * 3) % n ? hot : col, 0.55F * lit);
        }
        plot(x0, yb - R, x1, yb, u -> 1 / (u * 16 + 1), hot, 0.5F);
        for (int r = 0; r < 2; r++)
            for (int k = 0; k < 10; k++) {
                float gx = x0 + 6 + k * (x1 - x0 - 12) / 9, gy = cy - R * 0.75F + r * 8;
                fx.line(gx, gy, gx + 3 * rnd(k, r), gy + 4, 1F, col, 0.6F);
                fx.line(gx + 3, gy, gx, gy + 4 * rnd(r, k), 1F, col, 0.6F);
            }
    }

    /** The Hill curve and the toxicity curve beyond it, the therapeutic window shaded, the dose marker sweeping. */
    private void hill() {
        float x0 = cx - R, x1 = cx + R, y0 = cy - R * 0.7F, y1 = cy + R * 0.7F;
        axes(x0, y0, x1, y1);
        fx.rect(x0 + (x1 - x0) * 0.35F, y0, x0 + (x1 - x0) * 0.65F, y1, 0x60C080, 0.12F);
        plot(x0, y0, x1, y1, u -> 1 / (1 + Math.exp(-12 * (u - 0.25))), hot, 0.8F);
        plot(x0, y0, x1, y1, u -> 1 / (1 + Math.exp(-12 * (u - 0.8))), 0xFF5050, 0.6F);
        float u = 0.5F + 0.4F * sin(t * 0.8F);
        float x = x0 + (x1 - x0) * u;
        fx.line(x, y0, x, y1, 1F, W, 0.4F * lit);
        dot(x, y1 - (y1 - y0) / (1 + (float) Math.exp(-12 * (u - 0.25))), 2.2F, 1F);
    }

    /** Wells down to hot rock, water going down cold and coming up as steam to a turning turbine. */
    private void geothermal() {
        fx.rectV(cx - R, cy + R * 0.3F, cx + R, cy + R, col, 0.1F, 0xFF6020, 0.5F);
        fx.line(cx - R * 0.4F, cy - R * 0.5F, cx - R * 0.4F, cy + R * 0.85F, 2F, 0x60A0FF, 0.6F);
        fx.line(cx + R * 0.1F, cy + R * 0.85F, cx + R * 0.1F, cy - R * 0.5F, 2F, 0xFFA060, 0.6F);
        for (int i = 0; i < 6; i++) {
            float u = frac(t * 0.8F + i / 6F);
            dot(cx - R * 0.4F, cy - R * 0.5F + u * R * 1.35F, 1.2F, 0.6F);
            dot(cx + R * 0.1F, cy + R * 0.85F - u * R * 1.35F, 1.4F, 0.8F);
        }
        float tx = cx + R * 0.6F, ty = cy - R * 0.5F;
        fx.line(cx + R * 0.1F, ty, tx - 8, ty, 2F, 0xFFA060, 0.5F);
        for (int k = 0; k < 6; k++) {
            float a = t * 5 + k * TAU / 6;
            fx.line(tx, ty, tx + cos(a) * 8, ty + sin(a) * 8, 1.5F, hot, 0.8F * lit);
        }
        fx.ring(tx, ty, 9, 1F, 20, col, 0.6F);
    }

    /** The Sun throwing out its wind, the Earth's field lines pressed in on the dayside, the magnetopause arc. */
    private void solarWind() {
        float sx = cx - R * 1.05F, ex = cx + R * 0.45F;
        fx.disc(sx, cy, R * 0.35F, 20, 0xFFE080, 0.8F, 0xFF9020, 0.2F);
        for (int i = 0; i < 20; i++) {
            float u = frac(t * 0.5F + rnd(i, 5)), y = cy + (rnd(i, 6) - 0.5F) * R * 1.4F;
            float x = sx + R * 0.35F + u * (ex - sx - R * 0.7F);
            fx.line(x, y, x + 4, y, 1F, hot, 0.6F * (1 - u) * lit);
        }
        float squeeze = 0.65F - 0.25F * p();
        for (int k = 1; k <= 3; k++) {
            float r = R * 0.15F * k;
            fx.band(ex, cy, r, r + 1, 1.3F, (float) Math.PI * 0.5F, (float) Math.PI, 16, col, 0.5F, col, 0.5F);
            fx.band(ex, cy, r * (1 + squeeze), r * (1 + squeeze) + 1, 1.3F, -(float) Math.PI * 0.5F, (float) Math.PI, 16, col, 0.5F, col, 0.5F);
        }
        arc(ex, cy, R * 0.55F * (1.1F - 0.3F * p()), (float) Math.PI * 0.6F, (float) Math.PI * 0.8F, 2F, hot, 0.7F * lit);
        fx.disc(ex, cy, R * 0.12F, 16, 0x4080FF, 0.9F, 0x40C080, 0.6F);
    }

    /** Stacked trays with plants, nutrient film running down from tray to tray. */
    private void hydroponics() {
        for (int k = 0; k < 4; k++) {
            float y = cy - R * 0.75F + k * R * 0.5F;
            fx.rect(cx - R * 0.8F, y, cx + R * 0.8F, y + 3, col, 0.5F);
            for (int i = 0; i < 6; i++) {
                float x = cx - R * 0.65F + i * R * 0.26F, h = 4 + 5 * p() + 2 * sin(t + i + k);
                fx.line(x, y, x, y - h, 1F, 0x70D060, 0.7F);
                fx.line(x, y - h * 0.6F, x + 3, y - h, 1F, 0x90E070, 0.5F);
            }
            float u = frac(t * 0.6F + k * 0.25F);
            dot(cx + R * 0.85F, y + u * R * 0.5F, 1.4F, 0.8F);
        }
    }

    /** Annual bands of ice, a drill going down through them, the core drawn up and read. */
    private void iceCore() {
        for (int i = 0; i < 14; i++) {
            float y = cy - R + i * R * 2 / 14;
            fx.rect(cx - R, y, cx + R * 0.3F, y + R * 2 / 14 - 1, Fx2D.lerpRgb(0xC0E0FF, col, rnd(i, 0) * 0.6F), 0.15F + 0.1F * rnd(i, 1));
        }
        float depth = cy - R + 2 * R * (0.1F + 0.85F * p());
        fx.line(cx - R * 0.35F, cy - R - 4, cx - R * 0.35F, depth, 3F, col, 0.7F);
        fx.polygon(cx - R * 0.35F, depth + 2, 4, 6, t * 8, 1.5F, hot, 0.9F * lit);
        float x0 = cx + R * 0.5F, x1 = cx + R;
        plot(x0, cy - R, x1, cy + R, u -> 0.5 + 0.35 * Math.sin(u * 18) * Math.exp(-u), hot, 0.6F);
        fx.line(x0, cy - R, x0, cy + R, 1F, col, 0.4F);
    }

    /** A trefoil traced by a bead, the crossings lit as it passes over them. */
    private void trefoil() {
        float s = R * 0.3F, px = 0, py = 0;
        float head = frac(t * 0.25F);
        for (int i = 0; i <= 120; i++) {
            float u = i / 120F, a = u * TAU;
            float x = cx + s * (sin(a) + 2 * sin(2 * a)), y = cy + s * (cos(a) - 2 * cos(2 * a));
            if (i > 0) {
                float d = Math.min(Math.abs(u - head), 1 - Math.abs(u - head));
                fx.line(px, py, x, y, 2F, d < 0.08F ? hot : col, d < 0.08F ? 0.9F : 0.45F);
            }
            if (Math.abs(u - head) < 0.005F) dot(x, y, 2.5F, 1F);
            px = x;
            py = y;
        }
    }

    /** A column under end load, bowing into Euler's half sine as the load nears critical. */
    private void buckling() {
        float top = cy - R * 0.8F, bot = cy + R * 0.8F;
        fx.rect(cx - R * 0.5F, top - 8, cx + R * 0.5F, top - 4, col, 0.5F);
        fx.rect(cx - R * 0.5F, bot + 4, cx + R * 0.5F, bot + 8, col, 0.5F);
        float amp = R * 0.35F * (float) Math.pow(p(), 2) + 1.5F * sin(t * 3) * lit;
        float px = cx, py = top;
        for (int i = 1; i <= 30; i++) {
            float u = i / 30F, x = cx + amp * sin(u * Math.PI), y = top + (bot - top) * u;
            fx.line(px, py, x, y, 3F, Fx2D.lerpRgb(col, 0xFF6040, p()), 0.8F);
            px = x;
            py = y;
        }
        for (int i = -1; i <= 1; i++) arrow(cx + i * 8, top - 22, cx + i * 8, top - 10, hot, 0.6F * lit);
    }

    /** An axon in its myelin sheath, the spike jumping from node to node. */
    private void saltatory() {
        float x0 = cx - R, x1 = cx + R;
        blob(x0 + 4, cy, 7F, col, 0.5F);
        int nodes = 6;
        for (int i = 0; i < nodes; i++) {
            float a = x0 + 12 + i * (x1 - x0 - 12) / nodes, b = a + (x1 - x0 - 12) / nodes - 3;
            fx.rect(a, cy - 4, b, cy + 4, col, 0.35F);
        }
        fx.line(x0 + 10, cy, x1, cy, 1F, W, 0.3F);
        float u = frac(t * 0.7F);
        int at = (int) (u * nodes);
        for (int i = 0; i < nodes; i++) {
            float x = x0 + 12 + i * (x1 - x0 - 12) / nodes - 1.5F;
            blob(x, cy, i == at ? 4F : 1.5F, i == at ? W : hot, i == at ? lit : 0.3F);
        }
        float x2 = x0 + 12, x3 = x0 + 12 + at * (x1 - x0 - 12) / nodes;
        fx.line(x2, cy - R * 0.5F, x3, cy - R * 0.5F, 1.5F, hot, 0.6F * lit);
    }

    /** The cylinder seen end on, turning, its land and window strips, gravity pressing out on the floor. */
    private void oneill() {
        float a0 = t * 0.6F;
        for (int k = 0; k < 6; k++) {
            float a = a0 + k * TAU / 6;
            arc(cx, cy, R * 0.85F, a, TAU / 6 - 0.05F, 4F, k % 2 == 0 ? 0x70C070 : 0xA0D8FF, 0.6F);
        }
        fx.ring(cx, cy, R * 0.92F, 1F, 48, col, 0.5F);
        for (int k = 0; k < 3; k++) {
            float a = a0 + k * TAU / 3 + 0.3F;
            arrow(cx + cos(a) * R * 0.45F, cy + sin(a) * R * 0.45F, cx + cos(a) * R * 0.75F, cy + sin(a) * R * 0.75F, hot, 0.6F * lit);
        }
        blob(cx, cy, 3F, hot, 0.6F);
    }

    /** A leaf under the lamp, light in, carbon dioxide in, oxygen bubbling out. */
    private void photosynthesis() {
        fx.rect(cx - R * 0.6F, cy - R, cx + R * 0.6F, cy - R + 5, 0xFFE0A0, 0.7F * lit);
        for (int i = 0; i < 5; i++) {
            float x = cx - R * 0.5F + i * R * 0.25F, u = frac(t + i / 5F);
            fx.line(x, cy - R + 6, x, cy - R + 6 + u * R * 0.8F, 1F, 0xFFE0A0, 0.4F * lit);
        }
        fx.line(cx, cy + R, cx, cy - R * 0.1F, 2F, 0x70B050, 0.8F);
        for (int s = -1; s <= 1; s += 2)
            fx.band(cx + s * R * 0.3F, cy + R * 0.1F, 0, R * 0.32F, 0.45F, 0, TAU, 20, 0x60C050, 0.45F, 0x309030, 0.25F);
        for (int i = 0; i < 5; i++) {
            float u = frac(t * 0.6F + i / 5F);
            blob(cx + R * 0.7F, cy + R * 0.5F - u * R * 1.2F, 1.5F, W, 0.6F * (1 - u) * lit);
            blob(cx - R * 0.75F + u * R * 0.35F, cy + R * 0.3F, 1.5F, 0x808080, 0.5F * (1 - u));
        }
    }

    /** Signal and noise distributions with the criterion between them, the ROC curve beside. */
    private void signalDetection() {
        float x0 = cx - R, x1 = cx + R * 0.15F, y0 = cy - R * 0.6F, y1 = cy + R * 0.6F;
        axes(x0, y0, x1, y1);
        plot(x0, y0, x1, y1, u -> Math.exp(-Math.pow((u - 0.35) / 0.13, 2)), col, 0.6F);
        plot(x0, y0, x1, y1, u -> Math.exp(-Math.pow((u - 0.35 - 0.3 * (0.4 + 0.6 * p())) / 0.13, 2)), hot, 0.8F);
        float c = x0 + (x1 - x0) * (0.5F + 0.1F * sin(t * 0.8F));
        fx.line(c, y0, c, y1, 1F, W, 0.6F * lit);
        float rx0 = cx + R * 0.35F, rx1 = cx + R, ry0 = cy - R * 0.32F, ry1 = cy + R * 0.32F;
        axes(rx0, ry0, rx1, ry1);
        plot(rx0, ry0, rx1, ry1, u -> Math.pow(u, 0.2 + 0.3 * (1 - p())), hot, 0.8F);
    }

    /** Amplitude bars over every answer, the marked one growing with each Grover step, a Bloch arrow turning. */
    private void grover() {
        int n = 16, mark = 11;
        float x0 = cx - R, x1 = cx + R * 0.35F, yb = cy + R * 0.6F;
        float k = (float) (Math.PI / 2 * (0.15 + 0.85 * p()));
        float good = sin(k), rest = cos(k) / (float) Math.sqrt(n - 1);
        for (int i = 0; i < n; i++) {
            float h = (i == mark ? good : rest) * R * 1.2F;
            float x = x0 + i * (x1 - x0) / n;
            fx.rect(x + 1, yb - Math.abs(h), x + (x1 - x0) / n - 1, yb, i == mark ? hot : col, i == mark ? 0.8F * lit : 0.4F);
        }
        float bx = cx + R * 0.7F, by = cy;
        fx.ring(bx, by, R * 0.28F, 1F, 32, col, 0.6F);
        ellipse(bx, by, R * 0.28F, R * 0.09F, 1F, col, 0.35F);
        float a = t * 1.5F;
        fx.line(bx, by, bx + cos(a) * R * 0.2F, by - sin(a * 0.7F) * R * 0.26F, 1.5F, hot, 0.9F * lit);
        dot(bx + cos(a) * R * 0.2F, by - sin(a * 0.7F) * R * 0.26F, 2F, 1F);
    }

    /** A bar ringing in its first mode: the standing wave between two nodes, its swing growing, cracks spreading. */
    private void standingWave() {
        float x0 = cx - R, x1 = cx + R;
        float amp = R * 0.45F * (0.3F + 0.7F * p()) * sin(t * 10);
        fx.rect(x0, cy - 5, x1, cy + 5, col, 0.25F);
        for (int s = -1; s <= 1; s += 2) {
            float px = x0, py = cy;
            for (int i = 1; i <= 40; i++) {
                float u = i / 40F, x = x0 + (x1 - x0) * u, y = cy + s * amp * sin(u * Math.PI);
                fx.line(px, py, x, y, 1.2F, hot, 0.7F * lit);
                px = x;
                py = y;
            }
        }
        if (p() > 0.5F)
            for (int i = 0; i < 5; i++) {
                float x = x0 + (x1 - x0) * (0.3F + 0.1F * i);
                fx.line(x, cy - 5, x + 3 * (rnd(i, 7) - 0.5F) * 4, cy + 5, 1F, W, (p() - 0.5F) * 2 * lit);
            }
        dot(x0, cy, 2F, 0.8F);
        dot(x1, cy, 2F, 0.8F);
    }

    /** A creature growing in its tank, bubbles of oxygenated broth rising round it. */
    private void gestation() {
        fx.rect(cx - R * 0.55F, cy - R * 0.85F, cx + R * 0.55F, cy + R * 0.85F, col, 0.18F);
        fx.line(cx - R * 0.55F, cy - R * 0.85F, cx - R * 0.55F, cy + R * 0.85F, 1F, col, 0.6F);
        fx.line(cx + R * 0.55F, cy - R * 0.85F, cx + R * 0.55F, cy + R * 0.85F, 1F, col, 0.6F);
        float g = 0.3F + 0.7F * p(), pulse = 1 + 0.04F * sin(t * 6);
        fx.band(cx, cy, 0, R * 0.32F * g * pulse, 0.75F, 0, TAU, 24, Fx2D.lerpRgb(col, 0xFFC0B0, 0.6F), 0.5F, col, 0.2F);
        blob(cx + R * 0.12F * g, cy - R * 0.08F * g, R * 0.12F * g, 0xFFD0C0, 0.5F);
        fx.line(cx, cy + R * 0.2F * g, cx, cy + R * 0.85F, 1F, 0xC08080, 0.4F);
        for (int i = 0; i < 8; i++) {
            float u = frac(t * 0.4F + i / 8F);
            blob(cx - R * 0.45F + rnd(i, 2) * R * 0.9F, cy + R * 0.8F - u * R * 1.6F, 1.3F, W, 0.4F * (1 - u) * lit);
        }
    }

    /** A rocket on the pad, its stages, the countdown filling, then the plume. */
    private void rocket() {
        float x = cx, base = cy + R * 0.8F, up = run && f > 0.85F ? (f - 0.85F) / 0.15F * R * 0.6F : 0;
        fx.rect(cx - R * 0.6F, base, cx + R * 0.6F, base + 4, col, 0.5F);
        fx.line(cx + R * 0.25F, base, cx + R * 0.25F, cy - R * 0.6F, 2F, col, 0.4F);
        float y = base - up;
        fx.rect(x - 5, y - R * 0.55F, x + 5, y, col, 0.5F);
        fx.rect(x - 4, y - R * 1.0F, x + 4, y - R * 0.57F, col, 0.6F);
        fx.tri(x - 4, y - R * 1.0F, col, 0.7F, x + 4, y - R * 1.0F, col, 0.7F, x, y - R * 1.15F, hot, 0.8F);
        if (run) for (int i = 0; i < 8; i++) {
            float u = frac(t * 2 + i / 8F);
            blob(x + (rnd(i, 3) - 0.5F) * 6 * u, y + 3 + u * (10 + up), 2F + 3 * u, 0xFFA040, (1 - u) * (0.3F + f));
        }
        float bx0 = cx - R, bx1 = cx - R * 0.3F;
        fx.rect(bx0, cy - R * 0.9F, bx1, cy - R * 0.9F + 4, col, 0.15F);
        fx.rect(bx0, cy - R * 0.9F, bx0 + (bx1 - bx0) * p(), cy - R * 0.9F + 4, hot, 0.6F);
    }

    /** The tether rising from the curve of the Earth, the counterweight above, a climber riding up it. */
    private void elevator() {
        fx.band(cx, cy + R * 2.2F, R * 1.35F, R * 1.4F, 1, (float) Math.PI * 1.25F, (float) Math.PI * 0.5F, 32, 0x4080FF, 0.6F, 0x40C080, 0.6F);
        fx.line(cx, cy + R * 0.85F, cx, cy - R * 0.85F, 1.5F, col, 0.7F);
        fx.rect(cx - 6, cy - R * 0.95F, cx + 6, cy - R * 0.85F, col, 0.6F);
        float u = run ? f : frac(t * 0.1F) * 0.2F;
        float y = cy + R * 0.8F - u * R * 1.6F;
        fx.rect(cx - 4, y - 3, cx + 4, y + 3, hot, 0.9F * lit);
        fx.line(cx, cy + R * 0.85F, cx - 4, y + 3, 0.8F, 0xFFE080, 0.3F * lit);           // the power beam from the ground
        fx.ring(cx, cy - R * 0.2F, R * 0.55F, 0.8F, 40, col, 0.2F);
    }

    /** A star, the mirror cupped on one side of it, the jets on the other, the star drifting away from the mirror. */
    private void shkadov() {
        float drift = R * 0.08F * sin(t * 0.3F);
        float sx = cx - R * 0.1F + drift;
        fx.disc(sx, cy, R * 0.28F, 24, 0xFFF0C0, 0.9F, 0xFFA030, 0.3F);
        arc(sx, cy, R * 0.75F, -0.9F, 1.8F, 3F, 0xF0E0A0, 0.7F);
        for (int k = 0; k < 6; k++) {
            float a = -0.8F + k * 0.32F;
            fx.line(sx + cos(a) * R * 0.3F, cy + sin(a) * R * 0.3F, sx + cos(a) * R * 0.72F, cy + sin(a) * R * 0.72F, 0.8F, 0xFFE0A0, 0.3F * lit);
        }
        for (int s = -1; s <= 1; s += 2)
            for (int i = 0; i < 6; i++) {
                float u = frac(t * 1.2F + i / 6F);
                blob(sx - R * 0.3F - u * R * 0.8F, cy + s * R * 0.1F, 1.5F + 2 * u, 0xFF8040, (1 - u) * lit);
            }
        arrow(sx - R * 0.2F, cy + R * 0.55F, sx - R * 0.6F, cy + R * 0.55F, hot, 0.6F * lit);
    }

    /** A thundercloud, rain under it, lightning forking down every few seconds. */
    private void storm() {
        for (int i = 0; i < 6; i++) blob(cx - R * 0.6F + i * R * 0.24F, cy - R * 0.55F + (i % 2) * 4, R * 0.25F, 0x8088A0, 0.35F);
        for (int i = 0; i < 20; i++) {
            float u = frac(t * 1.5F + rnd(i, 1)), x = cx - R * 0.7F + rnd(i, 2) * R * 1.4F;
            fx.line(x, cy - R * 0.3F + u * R, x - 1, cy - R * 0.25F + u * R, 1F, 0x80A0FF, 0.5F * lit);
        }
        float k = frac(t * 0.4F);
        if (k < 0.12F) {
            float a = 1 - k / 0.12F, x = cx + (rnd((int) (t * 0.4F), 5) - 0.5F) * R;
            float px = x, py = cy - R * 0.35F;
            for (int s = 1; s <= 6; s++) {
                float qx = x + (rnd(s, (int) (t * 0.4F)) - 0.5F) * 14, qy = cy - R * 0.35F + s * R * 0.2F;
                fx.line(px, py, qx, qy, 2F, W, a);
                px = qx;
                py = qy;
            }
        }
    }

    /** Two layers of neurons and the weights between them, the ones that fire together strengthening. */
    private void hebb() {
        int n = 5;
        float xl = cx - R * 0.7F, xr = cx + R * 0.7F;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                float w = (float) (0.15 + 0.85 * p() * (i == j || i == (j + 1) % n ? 1 : 0.15));
                fx.line(xl, cy - R * 0.7F + i * R * 0.35F, xr, cy - R * 0.7F + j * R * 0.35F, 0.6F + 1.6F * w, Fx2D.lerpRgb(col, hot, w), 0.15F + 0.5F * w);
            }
        int fire = (int) (t * 2) % n;
        for (int i = 0; i < n; i++) {
            blob(xl, cy - R * 0.7F + i * R * 0.35F, 3.5F, i == fire ? W : col, i == fire ? lit : 0.5F);
            blob(xr, cy - R * 0.7F + i * R * 0.35F, 3.5F, i == fire || i == (fire + n - 1) % n ? hot : col, 0.6F);
        }
    }

    /** A disc turning under a loaded pin, the wear track deepening, friction plotted beside. */
    private void pinOnDisc() {
        float dx = cx - R * 0.3F;
        ellipse(dx, cy + R * 0.2F, R * 0.6F, R * 0.22F, 2F, col, 0.5F);
        ellipse(dx, cy + R * 0.2F, R * 0.4F, R * 0.15F, 1.5F + 2 * p(), hot, 0.5F * lit);
        float a = t * 4;
        dot(dx + cos(a) * R * 0.4F, cy + R * 0.2F + sin(a) * R * 0.15F, 1.3F, 0.5F);
        fx.line(dx + R * 0.4F, cy + R * 0.2F, dx + R * 0.4F, cy - R * 0.6F, 2.5F, col, 0.7F);
        arrow(dx + R * 0.4F, cy - R * 0.85F, dx + R * 0.4F, cy - R * 0.62F, hot, 0.6F * lit);
        float x0 = cx + R * 0.45F, x1 = cx + R, y0 = cy - R * 0.4F, y1 = cy + R * 0.4F;
        axes(x0, y0, x1, y1);
        plot(x0, y0, x1, y1, u -> 0.25 + 0.05 * Math.sin(u * 30 + t), hot, 0.6F);
    }

    /** Swell rolling past a buoy that rides up and down on it, power pulses running ashore. */
    private void swell() {
        float px = cx - R, py = cy;
        for (int i = 1; i <= 48; i++) {
            float u = i / 48F, x = cx - R + 2 * R * u, y = cy + sin(u * TAU * 1.5F - t * 2) * R * 0.18F;
            fx.line(px, py, x, y, 1.5F, col, 0.7F);
            px = x;
            py = y;
        }
        float by = cy + sin(0.5F * TAU * 1.5F - t * 2) * R * 0.18F;
        fx.rect(cx - 5, by - 8, cx + 5, by + 2, hot, 0.7F * lit);
        fx.line(cx, by + 2, cx, cy + R * 0.8F, 1F, col, 0.4F);
        fx.line(cx, cy + R * 0.8F, cx + R, cy + R * 0.8F, 1F, col, 0.4F);
        float u = frac(t * 0.8F);
        dot(cx + u * R, cy + R * 0.8F, 1.6F, 0.9F);
    }

    /** An aerofoil in the tunnel, streamlines bending over it, smoke puffs running along them. */
    private void windTunnel() {
        float a = 0.12F + 0.1F * sin(t * 0.3F);
        float px = 0, py = 0;
        for (int i = 0; i <= 30; i++) {
            float u = i / 30F, x = cx - R * 0.45F + u * R * 0.9F;
            float th = 0.12F * (0.2969F * (float) Math.sqrt(u) - 0.126F * u - 0.3516F * u * u + 0.2843F * u * u * u - 0.1015F * u * u * u * u) * R * 4;
            float y = cy - th - u * a * R * 0.4F;
            if (i > 0) fx.line(px, py, x, y, 1.5F, hot, 0.8F);
            px = x;
            py = y;
        }
        fx.line(cx - R * 0.45F, cy, cx + R * 0.45F, cy - a * R * 0.4F, 1.5F, hot, 0.6F);
        for (int k = -4; k <= 4; k++) {
            float y0 = cy + k * R * 0.18F;
            float lx = cx - R, ly = y0;
            for (int i = 1; i <= 30; i++) {
                float u = i / 30F, x = cx - R + 2 * R * u;
                float bump = (float) Math.exp(-Math.pow((x - cx) / (R * 0.5F), 2)) * R * 0.2F / (1 + Math.abs(k) * 0.6F);
                float y = y0 - (k <= 0 ? bump * 1.4F : bump * 0.5F);
                fx.line(lx, ly, x, y, 0.8F, col, 0.4F);
                lx = x;
                ly = y;
            }
            float u = frac(t * 0.7F + (k + 4) / 9F), x = cx - R + 2 * R * u;
            float bump = (float) Math.exp(-Math.pow((x - cx) / (R * 0.5F), 2)) * R * 0.2F / (1 + Math.abs(k) * 0.6F);
            dot(x, y0 - (k <= 0 ? bump * 1.4F : bump * 0.5F), 1.4F, 0.8F);
        }
    }
}
