package com.theoreticalsciences.machine.tuning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.2.0: the calibration minigames, played with the Tuning Fork on a machine. Solving one puts the machine exactly at
 * its working point for the recipe it is running (what Laplace's Demon does in creative). Each game is built the same
 * way on both sides from (kind, seed, tier), so the server only sends those three numbers and checks the answer itself.
 * <ul>
 *   <li>WAVE: match a target waveform with sine waves of your own (frequency, phase, amplitude); oscillators, fields, light.</li>
 *   <li>SIEVE: mark every prime in a run of numbers; number theory.</li>
 *   <li>CHAIN: put implications in an order that proves the goal from the premise; logic and proof.</li>
 *   <li>BALANCE, BUILD, OXIDATION, EQUILIBRIUM: the chemistry games (see {@link ChemGames}).</li>
 *   <li>SPECTRUM: calibrate a spectrometer's scale (offset and stretch) until its lines sit on the reference lines;
 *       telescopes, spectrometers, interferometers and the optical machines.</li>
 *   <li>FLOW: turn pipe tiles until the flow runs from the inlet to the outlet; reactors, plasma, colliders and pumps.</li>
 * </ul>
 *   <li>VERTEX: choose regular polygons that meet round one point with no gap and no overlap (their angles add to 360°);
 *       geometry.</li>
 *   <li>SYMMETRY: complete a pattern so it has the required mirror or rotation symmetry; algebra and crystals.</li>
 *   <li>CIPHER: break a shift cipher (later, a Vigenère cipher with a key of two or three letters); information.</li>
 * {@link #errors} counts how far an answer is off, for the Resonant Tuning Fork's hints.
 * The tier (the machine's stage of the game, 1 to 6) makes a game harder.
 */
public final class CalibrationGames {
    public static final int WAVE = 0, SIEVE = 1, CHAIN = 2, BALANCE = 3, SPECTRUM = 4, FLOW = 5, BUILD = 6, OXIDATION = 7, EQUILIBRIUM = 8, VERTEX = 9, SYMMETRY = 10, CIPHER = 11, PATH = 12, TREE = 13, NASH = 14;
    public static final int SIEVE_N = 40;

    private CalibrationGames() {}

    // ------------------------------------------------------------------ WAVE
    /** The target: {f, phase eighths, amplitude} per component (f distinct, 1..5; phase 0..7; amplitude 1..3). */
    public static int[][] wave(long seed, int tier) {
        Random r = new Random(seed);
        int n = tier >= 3 ? 2 : 1;
        int[][] out = new int[n][];
        int f0 = -1;
        for (int i = 0; i < n; i++) {
            int f;
            do f = 1 + r.nextInt(5); while (f == f0);
            f0 = f;
            out[i] = new int[]{f, r.nextInt(8), 1 + r.nextInt(3)};
        }
        return out;
    }

    public static double waveAt(int[][] comps, double x) {
        double y = 0;
        for (int[] c : comps) y += c[2] * Math.sin(c[0] * x + c[1] * Math.PI / 4);
        return y;
    }

    private static boolean waveOk(long seed, int tier, int[] a) {
        int[][] t = wave(seed, tier);
        if (a.length != t.length * 3) return false;
        // compare as waveforms, so two components given in either order (or the same wave written differently) count
        int[][] g = new int[t.length][];
        for (int i = 0; i < t.length; i++) g[i] = new int[]{a[i * 3], a[i * 3 + 1], a[i * 3 + 2]};
        for (int k = 0; k < 64; k++) {
            double x = k * Math.PI * 2 / 64;
            if (Math.abs(waveAt(t, x) - waveAt(g, x)) > 1e-6) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ SIEVE
    public static int sieveStart(long seed, int tier) {
        return 2 + new Random(seed).nextInt(10 + 30 * Math.max(1, tier));
    }

    public static boolean prime(int n) {
        if (n < 2) return false;
        for (int d = 2; (long) d * d <= n; d++) if (n % d == 0) return false;
        return true;
    }

    /** The answer: one value per cell, 1 = marked as prime. */
    private static boolean sieveOk(long seed, int tier, int[] a) {
        if (a.length != SIEVE_N) return false;
        int s = sieveStart(seed, tier);
        for (int i = 0; i < SIEVE_N; i++) if ((a[i] != 0) != prime(s + i)) return false;
        return true;
    }

    // ------------------------------------------------------------------ CHAIN
    /** Statements are numbered 0..n-1 (shown as letters); the premise is 0 and the goal is chain[last]. Tiles: {from, to}. */
    public record Chain(int premise, int goal, int symbols, List<int[]> tiles) {}

    public static Chain chain(long seed, int tier) {
        Random r = new Random(seed);
        int len = 3 + Math.min(4, Math.max(1, tier));
        int symbols = len + 1 + 2 + tier;
        List<Integer> order = new ArrayList<>();
        for (int i = 1; i < symbols; i++) order.add(i);
        java.util.Collections.shuffle(order, r);
        int[] path = new int[len + 1];
        path[0] = 0;
        for (int i = 1; i <= len; i++) path[i] = order.get(i - 1);
        List<int[]> tiles = new ArrayList<>();
        for (int i = 0; i < len; i++) tiles.add(new int[]{path[i], path[i + 1]});
        // distractors: implications that lead nowhere useful (into the side statements, or backwards)
        int extra = 2 + tier;
        for (int k = 0; k < extra; k++) {
            int a = r.nextInt(symbols), b = r.nextInt(symbols);
            if (a == b || b == 0) { k--; continue; }
            boolean dup = false;
            for (int[] t : tiles) dup |= t[0] == a && t[1] == b;
            if (dup) { k--; continue; }
            tiles.add(new int[]{a, b});
        }
        java.util.Collections.shuffle(tiles, r);
        return new Chain(0, path[len], symbols, tiles);
    }

    /** The answer: tile indices in the order used. Any proof from the premise to the goal counts. */
    private static boolean chainOk(long seed, int tier, int[] a) {
        Chain c = chain(seed, tier);
        if (a.length == 0 || a.length > c.tiles().size()) return false;
        int at = c.premise();
        boolean[] used = new boolean[c.tiles().size()];
        for (int i : a) {
            if (i < 0 || i >= used.length || used[i]) return false;
            used[i] = true;
            int[] t = c.tiles().get(i);
            if (t[0] != at) return false;
            at = t[1];
        }
        return at == c.goal();
    }

    // ------------------------------------------------------------------ BALANCE
    /** Element counts of a formula such as Cu(NO3)2. */
    public static Map<String, Integer> atoms(String f) {
        Map<String, Integer> out = new LinkedHashMap<>();
        parse(f, 0, out, 1);
        return out;
    }

    private static int parse(String f, int i, Map<String, Integer> out, int mult) {
        while (i < f.length()) {
            char ch = f.charAt(i);
            if (ch == '(') {
                Map<String, Integer> inner = new LinkedHashMap<>();
                i = parse(f, i + 1, inner, 1);
                int j = i;
                while (j < f.length() && Character.isDigit(f.charAt(j))) j++;
                int n = j > i ? Integer.parseInt(f.substring(i, j)) : 1;
                i = j;
                for (var e : inner.entrySet()) out.merge(e.getKey(), e.getValue() * n * mult, Integer::sum);
            } else if (ch == ')') {
                return i + 1;
            } else if (Character.isUpperCase(ch)) {
                int j = i + 1;
                while (j < f.length() && Character.isLowerCase(f.charAt(j))) j++;
                String el = f.substring(i, j);
                int k = j;
                while (k < f.length() && Character.isDigit(f.charAt(k))) k++;
                int n = k > j ? Integer.parseInt(f.substring(j, k)) : 1;
                out.merge(el, n * mult, Integer::sum);
                i = k;
            } else i++;
        }
        return i;
    }

    // ------------------------------------------------------------------ SPECTRUM
    /** Reference lines (nm), and the true distortion: offset (nm, -30..30) and stretch (percent, 90..110). */
    public record Spectrum(int[] lines, int offset, int stretch) {
        /** Where line i shows on the uncalibrated scale. */
        public double observed(int i) {
            return lines[i] * stretch / 100.0 + offset;
        }

        /** Where line i shows once calibrated with the player's offset and stretch. */
        public double corrected(int i, int o, int s) {
            return (observed(i) - o) * 100.0 / s;
        }
    }

    public static final int SPEC_LO = 380, SPEC_HI = 760;

    public static Spectrum spectrum(long seed, int tier) {
        Random r = new Random(seed);
        int n = Math.min(8, 3 + Math.max(1, tier));
        int[] lines = new int[n];
        for (int i = 0; i < n; i++) {
            int v;
            boolean near;
            do {
                v = 420 + r.nextInt(300);
                near = false;
                for (int k = 0; k < i; k++) near |= Math.abs(lines[k] - v) < 14;
            } while (near);
            lines[i] = v;
        }
        java.util.Arrays.sort(lines);
        int o, st;
        do {
            o = r.nextInt(61) - 30;
            st = 90 + r.nextInt(21);
        } while (o == 0 && st == 100);
        return new Spectrum(lines, o, st);
    }

    /** The answer: {offset, stretch}. */
    private static int spectrumErrors(long seed, int tier, int[] a) {
        if (a.length != 2) return 2;
        Spectrum sp = spectrum(seed, tier);
        return (a[0] != sp.offset() ? 1 : 0) + (a[1] != sp.stretch() ? 1 : 0);
    }

    // ------------------------------------------------------------------ FLOW
    /**
     * Openings: north 1, east 2, south 4, west 8 (0 = rock, no pipe). The inlet is on the west edge at row in, the outlet on
     * the east edge at row out. Twists, from stage 2 on: fixed tiles cannot be turned; valves must be on the flow; and from
     * stage 4 the flow must be sealed (no open pipe end on a tile it reaches).
     */
    public record Flow(int w, int h, int in, int out, int[] tiles, boolean[] fixed, boolean[] valve, boolean sealed) {}

    public static int rot(int mask, int k) {
        k = Math.floorMod(k, 4);
        return ((mask << k) | (mask >> (4 - k))) & 15;
    }

    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};   // N E S W, as bit 1 << i

    public static Flow flow(long seed, int tier) {
        Random r = new Random(seed);
        int w = 5 + (tier >= 3 ? 1 : 0) + (tier >= 5 ? 1 : 0), h = 4 + (tier >= 4 ? 1 : 0);
        int in = r.nextInt(h), out = r.nextInt(h);
        int[] sol = new int[w * h];
        // a path from the inlet to the outlet that never doubles back on itself
        List<int[]> path = new ArrayList<>();
        boolean[] seen = new boolean[w * h];
        path.add(new int[]{0, in});
        seen[in * w] = true;
        walk(r, w, h, out, path, seen);
        for (int i = 0; i < path.size(); i++) {
            int[] c = path.get(i);
            int m = 0;
            int[] prev = i == 0 ? new int[]{-1, in} : path.get(i - 1);
            int[] next = i == path.size() - 1 ? new int[]{w, out} : path.get(i + 1);
            for (int[] o : new int[][]{prev, next})
                for (int d = 0; d < 4; d++) if (c[0] + DIRS[d][0] == o[0] && c[1] + DIRS[d][1] == o[1]) m |= 1 << d;
            sol[c[1] * w + c[0]] = m;
        }
        int[] shapes = {5, 3, 7, 3, 5};
        for (int i = 0; i < sol.length; i++) if (sol[i] == 0) sol[i] = rot(shapes[r.nextInt(shapes.length)], r.nextInt(4));
        boolean[] onPath = new boolean[sol.length], fixed = new boolean[sol.length], valve = new boolean[sol.length];
        for (int[] c : path) onPath[c[1] * w + c[0]] = true;
        // twists: rock where no pipe is needed, locked tiles on the path (already turned right), valves the flow must pass
        if (tier >= 2) {
            for (int k = 0; k < tier - 1; k++) {
                int i = r.nextInt(sol.length);
                if (!onPath[i]) sol[i] = 0;
            }
            List<Integer> cells = new ArrayList<>();
            for (int i = 0; i < sol.length; i++) if (onPath[i] && i != in * w) cells.add(i);
            java.util.Collections.shuffle(cells, r);
            int nf = Math.min(cells.size() / 3, 1 + tier / 2), nv = Math.min(cells.size() - nf, 1 + tier / 3);
            for (int k = 0; k < nf; k++) fixed[cells.get(k)] = true;
            for (int k = nf; k < nf + nv; k++) valve[cells.get(k)] = true;
            // a few locked tiles off the path too, turned wrong, so a lock is not a give-away
            for (int k = 0; k < tier / 2; k++) {
                int i = r.nextInt(sol.length);
                if (!onPath[i] && sol[i] != 0) fixed[i] = true;
            }
        }
        boolean sealed = tier >= 4;
        int[] shown = new int[sol.length];
        boolean solved;
        do {
            for (int i = 0; i < sol.length; i++) shown[i] = fixed[i] ? sol[i] : rot(sol[i], r.nextInt(4));
            solved = flowReach(new Flow(w, h, in, out, shown, fixed, valve, sealed), new int[sol.length]) < 0;
        } while (solved);
        return new Flow(w, h, in, out, shown, fixed, valve, sealed);
    }

    private static boolean walk(Random r, int w, int h, int out, List<int[]> path, boolean[] seen) {
        int[] c = path.get(path.size() - 1);
        if (c[0] == w - 1 && c[1] == out) return true;
        List<Integer> ds = new ArrayList<>(List.of(0, 1, 2, 3));
        java.util.Collections.shuffle(ds, r);
        ds.sort((a, b) -> Integer.compare(a == 3 ? 1 : 0, b == 3 ? 1 : 0));   // west last: the path mostly runs east
        for (int d : ds) {
            int nx = c[0] + DIRS[d][0], ny = c[1] + DIRS[d][1];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h || seen[ny * w + nx]) continue;
            if (nx == w - 1 && ny != out && path.size() < 2) continue;
            seen[ny * w + nx] = true;
            path.add(new int[]{nx, ny});
            if (walk(r, w, h, out, path, seen)) return true;
            path.remove(path.size() - 1);
        }
        return false;
    }

    /** Follows the flow with each tile turned a[i] quarter turns: -1 if it reaches the outlet, else how many tiles it fills. */
    public static int flowReach(Flow f, int[] a) {
        int w = f.w(), h = f.h();
        int[] m = masks(f, a);
        boolean[] wet = flowWet(f, a);
        int n = 0;
        for (boolean b : wet) if (b) n++;
        int o = f.out() * w + w - 1;
        if (!wet[o] || (m[o] & 2) == 0) return n;
        for (int i = 0; i < wet.length; i++) if (f.valve()[i] && !wet[i]) return n;
        if (f.sealed() && leaks(f, a) > 0) return n;
        return -1;
    }

    /** The tiles as turned by the answer (fixed ones stay put). */
    public static int[] masks(Flow f, int[] a) {
        int[] m = new int[f.tiles().length];
        for (int i = 0; i < m.length; i++) m[i] = f.fixed()[i] ? f.tiles()[i] : rot(f.tiles()[i], i < a.length ? a[i] : 0);
        return m;
    }

    /** Open pipe ends on tiles the flow reaches (not counting the inlet and the outlet). */
    public static int leaks(Flow f, int[] a) {
        int w = f.w(), h = f.h();
        int[] m = masks(f, a);
        boolean[] wet = flowWet(f, a);
        int n = 0;
        for (int i = 0; i < m.length; i++) {
            if (!wet[i]) continue;
            int x = i % w, y = i / w;
            for (int d = 0; d < 4; d++) {
                if ((m[i] & (1 << d)) == 0) continue;
                int nx = x + DIRS[d][0], ny = y + DIRS[d][1];
                if (nx < 0 && y == f.in() || nx >= w && y == f.out()) continue;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || (m[ny * w + nx] & (1 << ((d + 2) % 4))) == 0) n++;
            }
        }
        return n;
    }

    /** Which tiles the flow reaches, for drawing. */
    public static boolean[] flowWet(Flow f, int[] a) {
        int w = f.w(), h = f.h();
        int[] m = masks(f, a);
        boolean[] wet = new boolean[w * h];
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        int s = f.in() * w;
        if ((m[s] & 8) == 0) return wet;
        wet[s] = true;
        q.add(s);
        while (!q.isEmpty()) {
            int i = q.poll();
            int x = i % w, y = i / w;
            for (int d = 0; d < 4; d++) {
                if ((m[i] & (1 << d)) == 0) continue;
                int nx = x + DIRS[d][0], ny = y + DIRS[d][1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int j = ny * w + nx;
                if (wet[j] || (m[j] & (1 << ((d + 2) % 4))) == 0) continue;
                wet[j] = true;
                q.add(j);
            }
        }
        return wet;
    }

    // ------------------------------------------------------------------ VERTEX
    /** The polygons on offer: sides, and interior angle 180(n − 2)/n in degrees. */
    public static final int[] SIDES = {3, 4, 5, 6, 8, 10, 12};

    public static int angle(int sides) {
        return 180 * (sides - 2) / sides;
    }

    /** required: index into SIDES that must be used; count: how many polygons exactly (0 = any). */
    public record Vertex(int required, int count) {}

    /** Every way to fill 360° with 3 or more of the polygons (counts per SIDES index). */
    private static List<int[]> vertexConfigs() {
        List<int[]> out = new ArrayList<>();
        vfill(new int[SIDES.length], 0, 0, out);
        return out;
    }

    private static void vfill(int[] c, int i, int sum, List<int[]> out) {
        if (sum == 360) {
            int n = 0;
            for (int v : c) n += v;
            if (n >= 3) out.add(c.clone());
            return;
        }
        if (i >= SIDES.length || sum > 360) return;
        for (int k = 0; sum + k * angle(SIDES[i]) <= 360; k++) {
            c[i] = k;
            vfill(c, i + 1, sum + k * angle(SIDES[i]), out);
        }
        c[i] = 0;
    }

    public static Vertex vertex(long seed, int tier) {
        Random r = new Random(seed);
        List<int[]> all = vertexConfigs();
        // later stages ask for the rarer polygons (pentagons, decagons, dodecagons) and an exact count
        List<int[]> pool = new ArrayList<>();
        for (int[] c : all) {
            boolean rare = c[2] > 0 || c[5] > 0 || c[6] > 0 || c[4] > 0;
            if (tier >= 3 ? rare : !rare || tier >= 2) pool.add(c);
        }
        int[] pick = pool.get(r.nextInt(pool.size()));
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < pick.length; i++) if (pick[i] > 0) used.add(i);
        int req = used.get(r.nextInt(used.size()));
        int n = 0;
        for (int v : pick) n += v;
        return new Vertex(req, tier >= 3 ? n : 0);
    }

    /** The answer: how many of each polygon (per SIDES index). */
    private static int vertexErrors(long seed, int tier, int[] a) {
        if (a.length != SIDES.length) return 3;
        Vertex v = vertex(seed, tier);
        int sum = 0, n = 0, wrong = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < 0) return 3;
            sum += a[i] * angle(SIDES[i]);
            n += a[i];
        }
        if (sum != 360) wrong++;
        if (a[v.required()] == 0) wrong++;
        if (n < 3 || v.count() > 0 && n != v.count()) wrong++;
        return wrong;
    }

    // ------------------------------------------------------------------ SYMMETRY
    /** 0: a mirror (left-right); 1: two mirrors; 2: a quarter turn; 3: a quarter turn and a mirror. on/off: locked cells. */
    public record Symmetry(int n, int group, boolean[] on, boolean[] off) {}

    /** Where cell i goes under each symmetry of the group (including itself). */
    public static int[] orbit(int n, int group, int i) {
        int x = i % n, y = i / n;
        java.util.Set<Integer> s = new java.util.LinkedHashSet<>();
        s.add(i);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int c : new ArrayList<>(s)) {
                int cx = c % n, cy = c / n;
                List<Integer> imgs = new ArrayList<>();
                imgs.add(cy * n + (n - 1 - cx));                          // left-right mirror
                if (group == 1 || group == 3) imgs.add((n - 1 - cy) * n + cx);   // top-bottom mirror
                if (group >= 2) imgs.add(cx * n + (n - 1 - cy));          // quarter turn
                if (group == 2) imgs.remove(0);                           // a pure rotation has no mirror
                for (int k : imgs) changed |= s.add(k);
            }
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    public static Symmetry symmetry(long seed, int tier) {
        Random r = new Random(seed);
        int n = tier >= 4 ? 8 : 6;
        int group = Math.min(3, Math.max(0, tier - 1));
        boolean[] on = new boolean[n * n], off = new boolean[n * n], closed = new boolean[n * n];
        for (int k = 0; k < 2 + tier; k++) {
            int i = r.nextInt(n * n);
            on[i] = true;
            for (int j : orbit(n, group, i)) closed[j] = true;
        }
        for (int k = 0; k < 2 + tier; k++) {
            int i = r.nextInt(n * n);
            if (!closed[i]) off[i] = true;
        }
        return new Symmetry(n, group, on, off);
    }

    /** The answer: one value per cell, 1 = filled. Counts the cells whose mirror or turn does not match, and broken locks. */
    private static int symmetryErrors(long seed, int tier, int[] a) {
        Symmetry s = symmetry(seed, tier);
        int n = s.n();
        if (a.length != n * n) return 99;
        int wrong = 0;
        for (int i = 0; i < n * n; i++) {
            boolean f = a[i] != 0 || s.on()[i];
            if (s.on()[i] && a[i] == 0 || s.off()[i] && a[i] != 0) wrong++;
            for (int j : orbit(n, s.group(), i))
                if ((a[j] != 0 || s.on()[j]) != f) {
                    wrong++;
                    break;
                }
        }
        return wrong;
    }

    // ------------------------------------------------------------------ CIPHER
    public static final String[] WORDS = {"ENTROPY", "THEOREM", "MATRIX", "VECTOR", "LEMMA", "AXIOM", "INTEGRAL", "SIGNAL", "TENSOR",
            "KERNEL", "PARITY", "ENTANGLED", "QUANTUM", "COUNTING", "BINARY", "LOGARITHM", "SHANNON", "TURING", "ALGORITHM", "CHANNEL",
            "MODULUS", "TOPOLOGY", "MANIFOLD", "SYMMETRY", "CALCULUS", "INFINITY", "EIGENVALUE", "PROBABILITY"};

    /** The plain word, and the key: one shift per key letter (a Caesar cipher has one). */
    public record Cipher(String word, int[] key) {
        public String encrypted() {
            return shift(word, key, 1);
        }
    }

    public static String shift(String w, int[] key, int sign) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length(); i++) {
            int k = key.length == 0 ? 0 : key[i % key.length];
            sb.append((char) ('A' + Math.floorMod(w.charAt(i) - 'A' + sign * k, 26)));
        }
        return sb.toString();
    }

    public static Cipher cipher(long seed, int tier) {
        Random r = new Random(seed);
        int len = tier >= 5 ? 3 : tier >= 3 ? 2 : 1;
        String w = WORDS[r.nextInt(WORDS.length)];
        int[] key = new int[len];
        for (int i = 0; i < len; i++) key[i] = 1 + r.nextInt(25);
        return new Cipher(w, key);
    }

    /** The answer: the key's shifts. Counts the letters that do not come out right. */
    private static int cipherErrors(long seed, int tier, int[] a) {
        Cipher c = cipher(seed, tier);
        if (a.length != c.key().length) return 99;
        String got = shift(c.encrypted(), a, -1);
        int wrong = 0;
        for (int i = 0; i < got.length(); i++) if (got.charAt(i) != c.word().charAt(i)) wrong++;
        return wrong;
    }

    // ------------------------------------------------------------------ PATH and TREE: a weighted network
    /**
     * 1.2.0: a network of junctions (0 is the start, n − 1 the end, laid out left to right) joined by links {a, b, length}.
     * via: a junction the route must pass (or −1); closed: links that cannot be used; forced: a link already laid (TREE).
     */
    public record Graph(int n, double[] x, double[] y, int[][] edges, int via, boolean[] closed, boolean[] forced) {}

    private static int find(int[] p, int i) {
        while (p[i] != i) i = p[i] = p[p[i]];
        return i;
    }

    private static boolean segmentsCross(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double d1 = (dx - cx) * (ay - cy) - (dy - cy) * (ax - cx), d2 = (dx - cx) * (by - cy) - (dy - cy) * (bx - cx);
        double d3 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax), d4 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        return d1 * d2 < 0 && d3 * d4 < 0;
    }

    /** Whether every junction can still reach every other without the closed links. */
    private static boolean connected(int n, int[][] e, boolean[] closed) {
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        int comps = n;
        for (int k = 0; k < e.length; k++) {
            if (closed[k]) continue;
            int a = find(p, e[k][0]), b = find(p, e[k][1]);
            if (a != b) {
                p[a] = b;
                comps--;
            }
        }
        return comps == 1;
    }

    public static Graph graph(long seed, int tier, boolean tree) {
        Random r = new Random(seed);
        int n = Math.min(12, 6 + tier);
        double[][] pts = new double[n][];
        for (int i = 0; i < n; i++) {
            for (int tries = 0; ; tries++) {
                double[] c = {0.04 + 0.92 * r.nextDouble(), 0.06 + 0.88 * r.nextDouble()};
                boolean ok = true;
                for (int j = 0; j < i; j++) ok &= Math.hypot(pts[j][0] - c[0], pts[j][1] - c[1]) > 0.2;
                if (ok || tries > 200) {
                    pts[i] = c;
                    break;
                }
            }
        }
        java.util.Arrays.sort(pts, (a, b) -> Double.compare(a[0], b[0]));
        double[] x = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = pts[i][0];
            y[i] = pts[i][1];
        }
        // links: each junction to its three nearest, never crossing another link (so the map stays readable)
        List<int[]> e = new ArrayList<>();
        java.util.Set<Long> have = new java.util.HashSet<>();
        for (int i = 0; i < n; i++) {
            final int ii = i;
            Integer[] near = new Integer[n];
            for (int j = 0; j < n; j++) near[j] = j;
            java.util.Arrays.sort(near, (a, b) -> Double.compare(Math.hypot(x[a] - x[ii], y[a] - y[ii]), Math.hypot(x[b] - x[ii], y[b] - y[ii])));
            for (int k = 1; k <= Math.min(3, n - 1); k++) {
                int j = near[k], a = Math.min(i, j), b = Math.max(i, j);
                if (!have.add((long) a * 64 + b)) continue;
                boolean cross = false;
                for (int[] o : e)
                    if (o[0] != a && o[0] != b && o[1] != a && o[1] != b && segmentsCross(x[a], y[a], x[b], y[b], x[o[0]], y[o[0]], x[o[1]], y[o[1]])) cross = true;
                // nor passing close by another junction (it would look as if it went there)
                for (int o = 0; o < n && !cross; o++) {
                    if (o == a || o == b) continue;
                    double ex = x[b] - x[a], ey = y[b] - y[a], l2 = ex * ex + ey * ey;
                    double u = Math.max(0, Math.min(1, ((x[o] - x[a]) * ex + (y[o] - y[a]) * ey) / l2));
                    if (Math.hypot(x[o] - (x[a] + u * ex), y[o] - (y[a] + u * ey)) < 0.08) cross = true;
                }
                if (cross) continue;
                int len = Math.max(1, (int) Math.round(Math.hypot(x[a] - x[b], y[a] - y[b]) * 12) + r.nextInt(4) - 1);
                e.add(new int[]{a, b, Math.min(15, len)});
            }
        }
        // join any separate pieces by their nearest pair
        for (boolean joined = true; joined; ) {
            joined = false;
            int[] p = new int[n];
            for (int i = 0; i < n; i++) p[i] = i;
            for (int[] o : e) p[find(p, o[0])] = find(p, o[1]);
            double best = 9;
            int ba = -1, bb = -1;
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (find(p, i) != find(p, j) && Math.hypot(x[i] - x[j], y[i] - y[j]) < best) {
                        best = Math.hypot(x[i] - x[j], y[i] - y[j]);
                        ba = i;
                        bb = j;
                    }
            if (ba >= 0) {
                e.add(new int[]{ba, bb, Math.max(1, (int) Math.round(best * 12))});
                joined = true;
            }
        }
        int[][] edges = e.toArray(new int[0][]);
        boolean[] closed = new boolean[edges.length], forced = new boolean[edges.length];
        if (tier >= 5)                                           // a couple of links closed (never one the network needs)
            for (int k = 0; k < 2; k++) {
                int c = r.nextInt(edges.length);
                closed[c] = true;
                if (!connected(n, edges, closed)) closed[c] = false;
            }
        if (tree && tier >= 4) {                                  // one link already laid, a long one: the best tree with it
            int f = r.nextInt(edges.length);
            if (!closed[f]) forced[f] = true;
        }
        int via = -1;
        if (!tree && tier >= 3) {                                // a junction the route must pass, one that some route can pass
            List<Integer> cand = new ArrayList<>();
            for (int i = 1; i < n - 1; i++) cand.add(i);
            java.util.Collections.shuffle(cand, r);
            for (int c : cand)
                if (bestPath(new Graph(n, x, y, edges, c, closed, forced)) < Integer.MAX_VALUE) {
                    via = c;
                    break;
                }
        }
        return new Graph(n, x, y, edges, via, closed, forced);
    }

    /** PATH: the shortest simple route from start to end through via (every route is tried: the map is small). */
    public static int bestPath(Graph g) {
        int[] best = {Integer.MAX_VALUE};
        boolean[] seen = new boolean[g.n()];
        seen[0] = true;
        pathSearch(g, 0, 0, seen, best);
        return best[0];
    }

    private static void pathSearch(Graph g, int at, int len, boolean[] seen, int[] best) {
        if (len >= best[0]) return;
        if (at == g.n() - 1) {
            if (g.via() < 0 || seen[g.via()]) best[0] = len;
            return;
        }
        for (int k = 0; k < g.edges().length; k++) {
            if (g.closed()[k]) continue;
            int[] e = g.edges()[k];
            int nx = e[0] == at ? e[1] : e[1] == at ? e[0] : -1;
            if (nx < 0 || seen[nx]) continue;
            seen[nx] = true;
            pathSearch(g, nx, len + e[2], seen, best);
            seen[nx] = false;
        }
    }

    /** TREE: the lightest network that joins every junction and keeps the forced links (Kruskal). */
    public static int bestTree(Graph g) {
        int[] p = new int[g.n()];
        for (int i = 0; i < g.n(); i++) p[i] = i;
        int total = 0;
        Integer[] order = new Integer[g.edges().length];
        for (int k = 0; k < order.length; k++) order[k] = k;
        java.util.Arrays.sort(order, (a, b) -> g.forced()[a] != g.forced()[b] ? (g.forced()[a] ? -1 : 1) : Integer.compare(g.edges()[a][2], g.edges()[b][2]));
        for (int k : order) {
            if (g.closed()[k]) continue;
            int a = find(p, g.edges()[k][0]), b = find(p, g.edges()[k][1]);
            if (a == b) continue;
            p[a] = b;
            total += g.edges()[k][2];
        }
        return total;
    }

    /** The total length of the links chosen (a[k] != 0). */
    public static int graphLength(Graph g, int[] a) {
        int t = 0;
        for (int k = 0; k < g.edges().length && k < a.length; k++) if (a[k] != 0) t += g.edges()[k][2];
        return t;
    }

    /** Whether the chosen links are a simple route from start to end (through via), using no closed link. */
    public static boolean isRoute(Graph g, int[] a) {
        if (a.length != g.edges().length) return false;
        int[] deg = new int[g.n()];
        int count = 0;
        for (int k = 0; k < a.length; k++) {
            if (a[k] == 0) continue;
            if (g.closed()[k]) return false;
            deg[g.edges()[k][0]]++;
            deg[g.edges()[k][1]]++;
            count++;
        }
        if (deg[0] != 1 || deg[g.n() - 1] != 1) return false;
        for (int i = 1; i < g.n() - 1; i++) if (deg[i] != 0 && deg[i] != 2) return false;
        if (g.via() >= 0 && deg[g.via()] != 2) return false;
        // walk it from the start: it must reach the end using every chosen link (no separate loops)
        int at = 0, prev = -1, used = 0;
        while (at != g.n() - 1) {
            int next = -1;
            for (int k = 0; k < a.length; k++) {
                if (a[k] == 0) continue;
                int[] e = g.edges()[k];
                int o = e[0] == at ? e[1] : e[1] == at ? e[0] : -1;
                if (o >= 0 && o != prev) {
                    next = o;
                    break;
                }
            }
            if (next < 0) return false;
            prev = at;
            at = next;
            if (++used > count) return false;
        }
        return used == count;
    }

    /** Whether the chosen links join every junction with no loop, keep the forced ones and use no closed one. */
    public static boolean isTree(Graph g, int[] a) {
        if (a.length != g.edges().length) return false;
        int[] p = new int[g.n()];
        for (int i = 0; i < g.n(); i++) p[i] = i;
        int links = 0;
        for (int k = 0; k < a.length; k++) {
            if (g.forced()[k] && a[k] == 0 || g.closed()[k] && a[k] != 0) return false;
            if (a[k] == 0) continue;
            int x = find(p, g.edges()[k][0]), y = find(p, g.edges()[k][1]);
            if (x == y) return false;                         // a loop
            p[x] = y;
            links++;
        }
        return links == g.n() - 1;
    }

    /** How far off: 0 right; otherwise how much longer than the best, or 99 while the links are not yet a route (a tree). */
    private static int graphErrors(int kind, long seed, int tier, int[] a) {
        Graph g = graph(seed, tier, kind == TREE);
        boolean shape = kind == TREE ? isTree(g, a) : isRoute(g, a);
        if (!shape) return 99;
        return graphLength(g, a) - (kind == TREE ? bestTree(g) : bestPath(g));
    }

    // ------------------------------------------------------------------ NASH: a game's equilibrium
    /** 1.2.0: a k x k game: pay[i][j] = {row player's payoff, column player's} when row plays i and column plays j. */
    public record Game(int k, int[][][] pay) {}

    /** Whether neither player can do better by changing alone at cell (i, j). */
    public static boolean equilibrium(Game g, int i, int j) {
        for (int a = 0; a < g.k(); a++) if (g.pay()[a][j][0] > g.pay()[i][j][0]) return false;
        for (int b = 0; b < g.k(); b++) if (g.pay()[i][b][1] > g.pay()[i][j][1]) return false;
        return true;
    }

    /** A game with exactly one pure equilibrium (2 x 2 early, 3 x 3 later), payoffs 0 to 9, no ties in a line. */
    public static Game game(long seed, int tier) {
        Random r = new Random(seed);
        int k = tier >= 4 ? 3 : 2;
        while (true) {
            int[][][] p = new int[k][k][2];
            for (int i = 0; i < k; i++)
                for (int j = 0; j < k; j++) {
                    p[i][j][0] = r.nextInt(10);
                    p[i][j][1] = r.nextInt(10);
                }
            Game g = new Game(k, p);
            int found = 0;
            for (int i = 0; i < k; i++) for (int j = 0; j < k; j++) if (equilibrium(g, i, j)) found++;
            boolean ties = false;
            for (int i = 0; i < k; i++)
                for (int a = 0; a < k; a++)
                    for (int b = a + 1; b < k; b++) ties |= p[a][i][0] == p[b][i][0] || p[i][a][1] == p[i][b][1];
            if (found == 1 && !ties) return g;
        }
    }

    // ------------------------------------------------------------------
    /** How far an answer is off: 0 when it is right. What the number counts depends on the game (see the hint texts). */
    public static int errors(int kind, long seed, int tier, int[] a) {
        switch (kind) {
            case WAVE -> {
                if (waveOk(seed, tier, a)) return 0;
                int[][] t = wave(seed, tier);
                int wrong = 0;
                for (int i = 0; i < t.length; i++) {
                    boolean found = false;
                    for (int k = 0; k < t.length && k * 3 + 2 < a.length; k++)
                        found |= a[k * 3] == t[i][0] && a[k * 3 + 1] == t[i][1] && a[k * 3 + 2] == t[i][2];
                    if (!found) wrong++;
                }
                return Math.max(1, wrong);
            }
            case SIEVE -> {
                int s = sieveStart(seed, tier), wrong = 0;
                for (int i = 0; i < SIEVE_N; i++) if ((i < a.length && a[i] != 0) != prime(s + i)) wrong++;
                return wrong;
            }
            case CHAIN -> {
                if (chainOk(seed, tier, a)) return 0;
                Chain c = chain(seed, tier);
                int at = c.premise(), good = 0;
                for (int i : a) {
                    if (i < 0 || i >= c.tiles().size() || c.tiles().get(i)[0] != at) break;
                    at = c.tiles().get(i)[1];
                    good++;
                }
                return good + 1;                                // the step that fails (1 = the first)
            }
            case BALANCE -> {
                return ChemGames.balanceErrors(seed, tier, a);
            }
            case BUILD -> {
                return ChemGames.buildErrors(seed, tier, a);
            }
            case OXIDATION -> {
                return ChemGames.oxidationErrors(seed, tier, a);
            }
            case EQUILIBRIUM -> {
                return ChemGames.equilibriumErrors(seed, tier, a);
            }
            case SPECTRUM -> {
                return spectrumErrors(seed, tier, a);
            }
            case VERTEX -> {
                return vertexErrors(seed, tier, a);
            }
            case SYMMETRY -> {
                return symmetryErrors(seed, tier, a);
            }
            case CIPHER -> {
                return cipherErrors(seed, tier, a);
            }
            case FLOW -> {
                int n = flowReach(flow(seed, tier), a);
                return n < 0 ? 0 : Math.max(1, n);              // tiles the flow reaches so far
            }
            case PATH, TREE -> {
                return graphErrors(kind, seed, tier, a);
            }
            case NASH -> {
                Game g = game(seed, tier);
                return a.length == 1 && a[0] >= 0 && a[0] < g.k() * g.k() && equilibrium(g, a[0] / g.k(), a[0] % g.k()) ? 0 : 1;
            }
            default -> {
                return 1;
            }
        }
    }

    // ------------------------------------------------------------------
    public static boolean check(int kind, long seed, int tier, int[] answer) {
        return switch (kind) {
            case WAVE -> waveOk(seed, tier, answer);
            case SIEVE -> sieveOk(seed, tier, answer);
            case CHAIN -> chainOk(seed, tier, answer);
            case BALANCE -> ChemGames.balanceErrors(seed, tier, answer) == 0;
            case BUILD -> ChemGames.buildErrors(seed, tier, answer) == 0;
            case OXIDATION -> ChemGames.oxidationErrors(seed, tier, answer) == 0;
            case EQUILIBRIUM -> ChemGames.equilibriumErrors(seed, tier, answer) == 0;
            case SPECTRUM -> spectrumErrors(seed, tier, answer) == 0;
            case VERTEX -> vertexErrors(seed, tier, answer) == 0;
            case SYMMETRY -> symmetryErrors(seed, tier, answer) == 0;
            case CIPHER -> cipherErrors(seed, tier, answer) == 0;
            case FLOW -> flowReach(flow(seed, tier), answer) < 0;
            case PATH, TREE -> graphErrors(kind, seed, tier, answer) == 0;
            case NASH -> errors(NASH, seed, tier, answer) == 0;
            default -> false;
        };
    }
}
