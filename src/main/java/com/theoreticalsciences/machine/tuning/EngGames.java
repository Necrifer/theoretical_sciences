package com.theoreticalsciences.machine.tuning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.4.0: five more calibration games, built like the others from (seed, tier) on both sides.
 * <ul>
 *   <li>LOGIC: choose each gate of a small circuit so its truth table matches the target; computers and redundancy.</li>
 *   <li>LOOP: tune a PID controller on a plant with lag until its step response meets an overshoot and a settling limit;
 *       control engineering, fusion and containment.</li>
 *   <li>DECAY: walk a heavy nuclide down a decay chain (α: Z − 2, A − 4; β⁻: Z + 1; β⁺: Z − 1) to the target, avoiding
 *       nuclides too short lived to pass through; nuclear machines.</li>
 *   <li>CODON: write the mRNA for a peptide with the standard genetic code, from AUG to a stop codon, in a GC range;
 *       genetics and biology.</li>
 *   <li>CIRCUIT: choose standard resistors so a current (total, or through one branch by the current divider) comes within
 *       2% of the target; electromagnetism and the bench instruments.</li>
 * </ul>
 */
public final class EngGames {
    private EngGames() {}

    // ------------------------------------------------------------------ LOGIC
    public static final String[] GATES = {"AND", "OR", "XOR", "NAND", "NOR", "XNOR"};

    /** inputs: how many (A, B, C); src: per gate its two sources (an input index, or inputs + gate index); target: gate types. */
    public record Logic(int inputs, int[][] src, int[] target, int types) {
        public int gates() {
            return src.length;
        }
    }

    public static int gate(int type, int a, int b) {
        return switch (type) {
            case 0 -> a & b;
            case 1 -> a | b;
            case 2 -> a ^ b;
            case 3 -> 1 - (a & b);
            case 4 -> 1 - (a | b);
            default -> 1 - (a ^ b);
        };
    }

    /** The output of each gate for one row of inputs (bit i of row = input i, A first). */
    public static int[] evalGates(Logic l, int[] types, int row) {
        int n = l.inputs();
        int[] v = new int[n + l.gates()];
        for (int i = 0; i < n; i++) v[i] = (row >> (n - 1 - i)) & 1;
        for (int j = 0; j < l.gates(); j++) {
            int t = j < types.length ? Math.floorMod(types[j], l.types()) : 0;
            v[n + j] = gate(t, v[l.src()[j][0]], v[l.src()[j][1]]);
        }
        int[] out = new int[l.gates()];
        System.arraycopy(v, n, out, 0, l.gates());
        return out;
    }

    public static int output(Logic l, int[] types, int row) {
        int[] g = evalGates(l, types, row);
        return g[g.length - 1];
    }

    public static Logic logic(long seed, int tier) {
        Random r = new Random(seed);
        int[][] src;
        int n;
        if (tier <= 2) {
            n = 2;
            src = new int[][]{{0, 1}, {2, 1}};
        } else if (tier <= 4) {
            n = 3;
            src = new int[][]{{0, 1}, {1, 2}, {3, 4}};
        } else {
            n = 3;
            src = new int[][]{{0, 1}, {1, 2}, {3, 2}, {4, 5}};
        }
        int types = tier >= 3 ? 6 : 5;
        int[] t = new int[src.length];
        Logic l;
        int tries = 0;
        do {
            for (int j = 0; j < t.length; j++) t[j] = r.nextInt(types);
            l = new Logic(n, src, t.clone(), types);
        } while (constant(l) && ++tries < 100);
        return l;
    }

    private static boolean constant(Logic l) {
        int first = output(l, l.target(), 0);
        for (int row = 1; row < (1 << l.inputs()); row++) if (output(l, l.target(), row) != first) return false;
        return true;
    }

    /** Rows of the truth table that differ (any gates giving the same table count). */
    public static int logicErrors(long seed, int tier, int[] a) {
        Logic l = logic(seed, tier);
        if (a.length != l.gates()) return 1 << l.inputs();
        int bad = 0;
        for (int row = 0; row < (1 << l.inputs()); row++) if (output(l, a, row) != output(l, l.target(), row)) bad++;
        return bad;
    }

    // ------------------------------------------------------------------ LOOP
    public static final double DT = 0.01, HORIZON = 20, BAND = 0.05;
    public static final int KP_MAX = 40, KI_MAX = 40, KD_MAX = 20;

    public static double kp(int i) {
        return i * 0.25;
    }

    public static double ki(int i) {
        return i * 0.1;
    }

    public static double kd(int i) {
        return i * 0.05;
    }

    /** Plant K/((τ1·s + 1)(τ2·s + 1)) (τ2 = 0: one lag); limits on overshoot (percent) and settling time (s). */
    public record Loop(double k, double tau1, double tau2, boolean derivative, int overshootPct, double settle, int[] example) {}

    /** The step response, sampled every {@code every} steps; [last] = {overshoot, settling time (∞ if never)}. */
    public static double[] response(Loop p, int[] a, int every, List<Double> trace) {
        double kp = kp(a[0]), ki = ki(a[1]), kd = a.length > 2 && p.derivative() ? kd(a[2]) : 0;
        double x1 = 0, x2 = 0, integ = 0, yPrev = 0, ymax = 0, lastOut = 0;
        int steps = (int) Math.round(HORIZON / DT);
        boolean blown = false;
        for (int s = 0; s < steps; s++) {
            double y = p.tau2() > 0 ? x2 : x1;
            double e = 1 - y;
            integ += e * DT;
            double u = kp * e + ki * integ - kd * (y - yPrev) / DT;
            yPrev = y;
            x1 += DT * (p.k() * u - x1) / p.tau1();
            if (p.tau2() > 0) x2 += DT * (x1 - x2) / p.tau2();
            double yn = p.tau2() > 0 ? x2 : x1;
            if (!Double.isFinite(yn) || Math.abs(yn) > 50) {
                blown = true;
                break;
            }
            ymax = Math.max(ymax, yn);
            if (Math.abs(yn - 1) > BAND) lastOut = (s + 1) * DT;
            if (trace != null && s % every == 0) trace.add(yn);
        }
        if (blown) return new double[]{9.99, Double.POSITIVE_INFINITY};
        return new double[]{Math.max(0, ymax - 1), lastOut >= HORIZON - 1 ? Double.POSITIVE_INFINITY : lastOut};
    }

    public static Loop loop(long seed, int tier) {
        Random r = new Random(seed);
        double k = Math.round((0.5 + r.nextDouble() * 1.5) * 10) / 10.0;
        double t1 = Math.round((0.5 + r.nextDouble() * 2.5) * 10) / 10.0;
        double t2 = tier >= 3 ? Math.round((0.2 + r.nextDouble() * 1.3) * 10) / 10.0 : 0;
        boolean der = tier >= 5;
        Loop probe = new Loop(k, t1, t2, der, 100, HORIZON, null);
        for (int tries = 0; tries < 400; tries++) {
            int[] a = {1 + r.nextInt(KP_MAX), 1 + r.nextInt(KI_MAX), der ? r.nextInt(KD_MAX + 1) : 0};
            double[] m = response(probe, a, 1 << 30, null);
            if (!Double.isFinite(m[1]) || m[0] > 0.6 || m[1] > HORIZON - 4) continue;
            int os = (int) Math.ceil(m[0] * 100 + Math.max(2, 13 - 2 * tier));
            double ts = Math.ceil(Math.max(m[1] + 0.3, m[1] * (1.6 - 0.08 * tier)) * 10) / 10.0;
            return new Loop(k, t1, t2, der, Math.min(60, os), Math.min(HORIZON - 2, ts), a);
        }
        return new Loop(k, t1, t2, der, 60, HORIZON - 2, new int[]{4, 2, 0});
    }

    /** How many of the two limits the response breaks (0, 1 or 2). */
    public static int loopErrors(long seed, int tier, int[] a) {
        if (a.length != 3) return 2;
        Loop p = loop(seed, tier);
        if (a[0] < 0 || a[0] > KP_MAX || a[1] < 0 || a[1] > KI_MAX || a[2] < 0 || a[2] > KD_MAX) return 2;
        double[] m = response(p, a, 1 << 30, null);
        return (m[0] * 100 > p.overshootPct() + 1e-9 ? 1 : 0) + (m[1] > p.settle() + 1e-9 ? 1 : 0);
    }

    // ------------------------------------------------------------------ DECAY
    public static final int Z_LO = 72;
    public static final String[] SYM = {"Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr",
            "Ra", "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf"};
    public static final int Z_HI = Z_LO + SYM.length - 1;
    /** α, β⁻, β⁺: change in {Z, A}. */
    public static final int[][] MOVES = {{-2, -4}, {1, 0}, {-1, 0}};
    private static final int[][] STARTS = {{92, 238}, {92, 235}, {90, 232}, {93, 237}, {94, 239}, {95, 241}, {94, 244}};

    public static String symbol(int z) {
        return z >= Z_LO && z <= Z_HI ? SYM[z - Z_LO] : "?";
    }

    /** Start and target {Z, A}; forbidden nuclides (each {Z, A}); moves allowed (2 or 3). */
    public record Decay(int z0, int a0, int z1, int a1, List<int[]> forbidden, int moves, int limit, int[] example) {
        public boolean isForbidden(int z, int a) {
            for (int[] f : forbidden) if (f[0] == z && f[1] == a) return true;
            return false;
        }
    }

    public static Decay decay(long seed, int tier) {
        Random r = new Random(seed);
        int[] s = STARTS[r.nextInt(STARTS.length)];
        int moves = tier >= 4 ? 3 : 2;
        int n = 3 + tier;
        List<int[]> path = new ArrayList<>();
        int z = s[0], a = s[1];
        path.add(new int[]{z, a});
        int alphas = 0;
        List<Integer> seq = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int m;
            int tries = 0;
            do {
                m = r.nextInt(10) < 5 ? 0 : (moves == 3 && r.nextInt(4) == 0 ? 2 : 1);
                tries++;
            } while (tries < 20 && (z + MOVES[m][0] < Z_LO + 1 || z + MOVES[m][0] > Z_HI - 1 || (m == 0 && alphas > n / 2 + 1)));
            if (z + MOVES[m][0] < Z_LO + 1 || z + MOVES[m][0] > Z_HI - 1) m = z - 2 >= Z_LO + 1 ? 0 : 1;
            if (m == 0) alphas++;
            seq.add(m);
            z += MOVES[m][0];
            a += MOVES[m][1];
            path.add(new int[]{z, a});
        }
        if (z == s[0] && a == s[1]) {          // never a target the start already is
            z -= 2;
            a -= 4;
            seq.add(0);
            path.add(new int[]{z, a});
        }
        List<int[]> bad = new ArrayList<>();
        if (tier >= 2) {
            int want = 2 * tier, tries = 0;
            while (bad.size() < want && tries++ < 400) {
                int[] p = path.get(1 + r.nextInt(path.size() - 1));
                int fz = p[0] + r.nextInt(3) - 1, fa = p[1] + 4 * (r.nextInt(3) - 1);
                boolean onPath = false, dup = false;
                for (int[] q : path) onPath |= q[0] == fz && q[1] == fa;
                for (int[] q : bad) dup |= q[0] == fz && q[1] == fa;
                if (!onPath && !dup && fz >= Z_LO && fz <= Z_HI) bad.add(new int[]{fz, fa});
            }
        }
        return new Decay(s[0], s[1], z, a, bad, moves, n + 6, seq.stream().mapToInt(Integer::intValue).toArray());
    }

    /** The nuclides visited by a sequence of moves, the start first; stops at the first move not allowed. */
    public static List<int[]> walk(Decay d, int[] seq) {
        List<int[]> out = new ArrayList<>();
        int z = d.z0(), a = d.a0();
        out.add(new int[]{z, a});
        for (int m : seq) {
            if (m < 0 || m >= d.moves()) break;
            z += MOVES[m][0];
            a += MOVES[m][1];
            out.add(new int[]{z, a});
        }
        return out;
    }

    /** Steps short of the target (charge and mass), plus one for each forbidden nuclide passed through. */
    public static int decayErrors(long seed, int tier, int[] seq) {
        Decay d = decay(seed, tier);
        if (seq.length > d.limit()) return 99;
        List<int[]> w = walk(d, seq);
        if (w.size() != seq.length + 1) return 99;
        int[] end = w.get(w.size() - 1);
        int err = Math.abs(end[0] - d.z1()) + Math.abs(end[1] - d.a1()) / 4 + (Math.abs(end[1] - d.a1()) % 4 != 0 ? 1 : 0);
        for (int[] p : w) if (d.isForbidden(p[0], p[1]) || p[0] < Z_LO || p[0] > Z_HI) err++;
        return err;
    }

    // ------------------------------------------------------------------ CODON
    public static final String BASES = "UCAG";
    /** The standard genetic code, codon index 16·first + 4·second + third, bases in the order U C A G; "Stop" for stops. */
    public static final String[] CODE = {
            "Phe", "Phe", "Leu", "Leu", "Ser", "Ser", "Ser", "Ser", "Tyr", "Tyr", "Stop", "Stop", "Cys", "Cys", "Stop", "Trp",
            "Leu", "Leu", "Leu", "Leu", "Pro", "Pro", "Pro", "Pro", "His", "His", "Gln", "Gln", "Arg", "Arg", "Arg", "Arg",
            "Ile", "Ile", "Ile", "Met", "Thr", "Thr", "Thr", "Thr", "Asn", "Asn", "Lys", "Lys", "Ser", "Ser", "Arg", "Arg",
            "Val", "Val", "Val", "Val", "Ala", "Ala", "Ala", "Ala", "Asp", "Asp", "Glu", "Glu", "Gly", "Gly", "Gly", "Gly"};
    public static final int AUG = 35;

    public static String codon(int i) {
        i = Math.floorMod(i, 64);
        return "" + BASES.charAt(i / 16) + BASES.charAt(i / 4 % 4) + BASES.charAt(i % 4);
    }

    public static int gc(int i) {
        String c = codon(i);
        int n = 0;
        for (char ch : c.toCharArray()) if (ch == 'G' || ch == 'C') n++;
        return n;
    }

    /** The peptide to write (the last is "Stop" when a stop codon is asked); GC limits in percent (0 to 100: none). */
    public record Codon(String[] peptide, int gcLo, int gcHi, int[] example) {}

    public static Codon codon(long seed, int tier) {
        Random r = new Random(seed);
        int len = 3 + Math.min(tier, 4) + (tier >= 3 ? 1 : 0);
        len = Math.min(8, len);
        String[] pep = new String[len];
        int[] sol = new int[len];
        for (int i = 0; i < len; i++) {
            int c;
            if (i == 0 && tier >= 2) c = AUG;
            else if (i == len - 1 && tier >= 3) do c = r.nextInt(64); while (!CODE[c].equals("Stop"));
            else do c = r.nextInt(64); while (CODE[c].equals("Stop"));
            sol[i] = c;
            pep[i] = CODE[c];
        }
        int lo = 0, hi = 100;
        if (tier >= 4) {
            int g = 0;
            for (int c : sol) g += gc(c);
            double pct = 100.0 * g / (3 * len);
            int half = tier >= 6 ? 7 : 10;
            lo = Math.max(0, (int) Math.floor(pct - half));
            hi = Math.min(100, (int) Math.ceil(pct + half));
        }
        return new Codon(pep, lo, hi, sol);
    }

    public static int gcPercent(int[] a) {
        int g = 0;
        for (int c : a) g += gc(c);
        return a.length == 0 ? 0 : (int) Math.round(100.0 * g / (3 * a.length));
    }

    /** Codons coding the wrong amino acid, plus one if the GC content is out of range. */
    public static int codonErrors(long seed, int tier, int[] a) {
        Codon c = codon(seed, tier);
        if (a.length != c.peptide().length) return c.peptide().length + 1;
        int bad = 0;
        for (int i = 0; i < a.length; i++) if (a[i] < 0 || a[i] > 63 || !CODE[a[i]].equals(c.peptide()[i])) bad++;
        double pct = 0;
        for (int v : a) pct += gc(v);
        pct = 100.0 * pct / (3 * a.length);
        if (pct < c.gcLo() - 1e-9 || pct > c.gcHi() + 1e-9) bad++;
        return bad;
    }

    // ------------------------------------------------------------------ CIRCUIT
    private static final int[] E12 = {10, 12, 15, 18, 22, 27, 33, 39, 47, 56, 68, 82};
    public static final int R_MAX = E12.length * 3 - 1;

    /** Ohms of standard value i (E12, 10 Ω to 8.2 kΩ). */
    public static int ohms(int i) {
        i = Math.max(0, Math.min(R_MAX, i));
        return E12[i % 12] * (int) Math.pow(10, i / 12);
    }

    public static String ohmsText(int i) {
        int o = ohms(i);
        return o >= 1000 ? (o % 1000 == 0 ? o / 1000 + "k" : String.format("%.1fk", o / 1000.0)) + "Ω" : o + "Ω";
    }

    /**
     * Topology: R1 in series with R2 parallel to (R3, plus R4 in series from stage 5). measure: 0 the total current, 1 the
     * current through the R3 branch. fixed: resistors already in place (their values are given).
     */
    public record Circuit(double volts, int resistors, int measure, double target, boolean[] fixed, int[] given) {}

    /** {total current, branch current through R3} in amperes. */
    public static double[] currents(Circuit c, int[] a) {
        double r1 = ohms(a[0]), r2 = ohms(a[1]), r3 = ohms(a[2]) + (c.resistors() > 3 ? ohms(a[3]) : 0);
        double par = r2 * r3 / (r2 + r3);
        double it = c.volts() / (r1 + par);
        return new double[]{it, it * r2 / (r2 + r3)};
    }

    public static Circuit circuit(long seed, int tier) {
        Random r = new Random(seed);
        double[] vs = {5, 9, 12, 24, 48};
        double v = vs[r.nextInt(vs.length)];
        int n = tier >= 5 ? 4 : 3;
        int[] sol = new int[n];
        for (int i = 0; i < n; i++) sol[i] = r.nextInt(R_MAX + 1);
        int measure = tier >= 3 ? 1 : 0;
        boolean[] fixed = new boolean[n];
        if (tier >= 3) fixed[0] = true;
        if (tier >= 6) fixed[n - 1] = true;
        Circuit c = new Circuit(v, n, measure, 0, fixed, sol);
        double t = currents(c, sol)[measure];
        return new Circuit(v, n, measure, t, fixed, sol);
    }

    /** Percent off the target, rounded up (0 within 2%); a fixed resistor changed counts as 100. */
    public static int circuitErrors(long seed, int tier, int[] a) {
        Circuit c = circuit(seed, tier);
        if (a.length != c.resistors()) return 100;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0 || a[i] > R_MAX) return 100;
            if (c.fixed()[i] && a[i] != c.given()[i]) return 100;
        }
        double off = Math.abs(currents(c, a)[c.measure()] - c.target()) / c.target();
        return off <= 0.02 ? 0 : (int) Math.ceil(off * 100);
    }
}
