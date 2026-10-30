package com.theoreticalsciences.dungeon;

import java.util.Random;

/**
 * The four conditions a Breached Anomaly Engine must hold to pull one kind of creature into being (round 21). Each
 * species has its own; each reading answers mostly to its own control and a little to one other, so they can always be
 * tuned in. Shared by client and server.
 */
public final class SpawnTuning {
    public static final int N = 4, MIN = -100, MAX = 100;
    public static final double TOLERANCE = 3.0;

    public record Problem(int[] target, double[][] mix) {}

    public static Problem problem(String species) {
        Random r = new Random(0xA20_4A11L ^ species.hashCode() * 0x9E3779B97F4A7C15L);
        int[] t = new int[N];
        for (int i = 0; i < N; i++) t[i] = r.nextInt(161) - 80;
        double[][] m = new double[N][N];
        for (int i = 0; i < N; i++) {
            m[i][i] = r.nextBoolean() ? 1.0 : -1.0;
            int a = (i + 1 + r.nextInt(N - 1)) % N;
            m[i][a] = (r.nextBoolean() ? 1 : -1) * (0.2 + 0.35 * r.nextDouble());
        }
        return new Problem(t, m);
    }

    /** For knobs still gliding on screen (round 28). */
    public static double[] residuals(Problem p, double[] k) {
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0;
            for (int j = 0; j < N; j++) s += p.mix()[i][j] * (k[j] - p.target()[j]);
            out[i] = s;
        }
        return out;
    }

    public static double[] residuals(Problem p, int[] k) {
        double[] out = new double[N];
        for (int i = 0; i < N; i++) {
            double s = 0;
            for (int j = 0; j < N; j++) s += p.mix()[i][j] * (k[j] - p.target()[j]);
            out[i] = s;
        }
        return out;
    }

    public static boolean tuned(Problem p, int[] k) {
        for (double v : residuals(p, k)) if (Math.abs(v) > TOLERANCE) return false;
        return true;
    }

    private SpawnTuning() {}
}
