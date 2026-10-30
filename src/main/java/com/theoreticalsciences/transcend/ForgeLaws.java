package com.theoreticalsciences.transcend;

import java.util.Random;

/**
 * The laws a Forge recipe must be simulated under (round 19). To forge something beyond Omnipotence the Core runs a
 * universe in which it can exist; the operator sets six constants of nature, and six observables of the simulated
 * universe report how far it is from stable. Each recipe has its own target universe, and each observable depends on
 * several constants at once, so turning one knob moves several readings. All six readings within tolerance: the
 * simulation converges and the Core accepts the recipe. Shared by client and server (the server checks again).
 */
public final class ForgeLaws {
    public static final int N = 6, MIN = -100, MAX = 100;
    public static final double TOLERANCE = 2.5;
    /** Symbols and names of the constants (translation keys gui.theoreticalsciences.law.N). */
    public static final String[] SYMBOL = {"G", "c", "ħ", "α", "Λ", "kʙ"};

    public record Problem(int[] target, double[][] mix) {}

    public static Problem problem(String recipeId) {
        return problem(recipeId, 0);
    }

    /** Round 36: a drifting recipe (see {@link ForgeDrift}) gets a new universe after every forging. */
    public static Problem problem(String recipeId, int epoch) {
        long seed = 0x51F0_7A11L ^ recipeId.hashCode() * 0x9E3779B97F4A7C15L;
        if (epoch != 0) seed ^= (epoch * 0xC2B2AE3D27D4EB4FL) + 0x165667B19E3779F9L;
        Random r = new Random(seed);
        int[] t = new int[N];
        for (int i = 0; i < N; i++) t[i] = r.nextInt(181) - 90;
        double[][] m = new double[N][N];
        for (int i = 0; i < N; i++) {
            m[i][i] = r.nextBoolean() ? 1.0 : -1.0;
            int a = (i + 1 + r.nextInt(N - 1)) % N, b = (i + 1 + r.nextInt(N - 1)) % N;
            m[i][a] += (r.nextBoolean() ? 1 : -1) * (0.25 + 0.25 * r.nextDouble());
            if (b != a) m[i][b] += (r.nextBoolean() ? 1 : -1) * (0.1 + 0.2 * r.nextDouble());
        }
        return new Problem(t, m);
    }

    /** How far each observable is from stable, for these settings. */
    /** The same for settings that are still gliding on screen (round 28: the bars move smoothly). */
    public static double[] residuals(Problem p, double[] k) {
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0;
            for (int j = 0; j < N; j++) s += p.mix()[i][j] * ((j < k.length ? k[j] : 0) - p.target()[j]);
            out[i] = s;
        }
        return out;
    }

    public static double[] residuals(Problem p, int[] k) {
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0;
            for (int j = 0; j < N; j++) s += p.mix()[i][j] * ((j < k.length ? k[j] : 0) - p.target()[j]);
            out[i] = s;
        }
        return out;
    }

    public static boolean converged(Problem p, int[] k) {
        for (double v : residuals(p, k)) if (Math.abs(v) > TOLERANCE) return false;
        return true;
    }

    public static String encode(int[] k) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < k.length; i++) b.append(i > 0 ? "," : "").append(k[i]);
        return b.toString();
    }

    public static int[] decode(String s) {
        int[] k = new int[N];
        String[] parts = s.split(",");
        for (int i = 0; i < N && i < parts.length; i++)
            try {
                k[i] = Math.max(MIN, Math.min(MAX, Integer.parseInt(parts[i].trim())));
            } catch (NumberFormatException e) {
                k[i] = 0;
            }
        return k;
    }

    private ForgeLaws() {}
}
