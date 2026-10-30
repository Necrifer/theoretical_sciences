package com.theoreticalsciences.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The geometry of the World Tree in Hyperdimensional Reality Space.
 * <p>
 * The whole tree is computed once, deterministically, as a list of tapered capsules (trunk, roots, and
 * four levels of fractal branches reaching more than a thousand blocks out) plus the positions of the
 * bubble universes hanging from its twigs. Every segment is filed into a coarse 64x64 column grid, so
 * world generation for any chunk only has to look at the handful of segments that can touch it -
 * which means chunks can be generated in any order and the tree is always seamless.
 */
public final class WorldTree {
    public static final int TRUNK_TOP = 430;
    public static final int COLLAR_Y = 70;
    public static final int COLLAR_INNER = 24, COLLAR_OUTER = 42;
    private static final int CELL = 64;

    /** A tapered capsule from (x0,y0,z0) radius r0 to (x1,y1,z1) radius r1. */
    public record Seg(double x0, double y0, double z0, double x1, double y1, double z1, double r0, double r1, boolean hollow) {
        double minX() { return Math.min(x0, x1) - Math.max(r0, r1); }
        double maxX() { return Math.max(x0, x1) + Math.max(r0, r1); }
        double minZ() { return Math.min(z0, z1) - Math.max(r0, r1); }
        double maxZ() { return Math.max(z0, z1) + Math.max(r0, r1); }
        public double minY() { return Math.min(y0, y1) - Math.max(r0, r1); }
        public double maxY() { return Math.max(y0, y1) + Math.max(r0, r1); }
    }

    /** A bubble universe hanging {@code stalk} blocks below a twig. */
    public record Bubble(int x, int y, int z, int stalk) {}

    public static final List<Seg> SEGMENTS = new ArrayList<>();
    public static final List<Bubble> BUBBLES = new ArrayList<>();
    private static final Map<Long, List<Seg>> SEG_GRID = new HashMap<>();
    private static final Map<Long, List<Bubble>> BUBBLE_GRID = new HashMap<>();

    static {
        Random r = new Random(0x0A4B0E5L);
        // trunk: rises out of the ocean floor in gently swaying, hollow sections
        double px = 0, pz = 0, py = 0, pr = 26;
        int sections = 7;
        for (int i = 1; i <= sections; i++) {
            double ny = TRUNK_TOP * i / (double) sections;
            double nx = Math.sin(i * 0.9) * 4, nz = Math.cos(i * 1.3) * 4;
            double nr = 26 - 17 * i / (double) sections;
            SEGMENTS.add(new Seg(px, py, pz, nx, ny, nz, pr, nr, true));
            px = nx; py = ny; pz = nz; pr = nr;
        }
        // crown
        SEGMENTS.add(new Seg(px, py, pz, px, py + 30, pz, pr, 2, false));
        // roots: sprawl across the ocean surface, then dive into it
        for (int i = 0; i < 9; i++) {
            double a = i * Math.PI * 2 / 9 + r.nextDouble() * 0.3;
            double len = 150 + r.nextDouble() * 140;
            double mx = Math.cos(a) * 22, mz = Math.sin(a) * 22;
            double ex = Math.cos(a) * len, ez = Math.sin(a) * len;
            double hx = (mx + ex) / 2 + Math.cos(a + 1.57) * 25 * (r.nextDouble() - 0.5), hz = (mz + ez) / 2 + Math.sin(a + 1.57) * 25 * (r.nextDouble() - 0.5);
            SEGMENTS.add(new Seg(mx, 95, mz, hx, 64, hz, 9, 6, false));
            SEGMENTS.add(new Seg(hx, 64, hz, ex, 30, ez, 6, 2, false));
        }
        // branches: nine great limbs, each splitting three ways four more times
        for (int i = 0; i < 9; i++) {
            double h = 170 + i * 28 + r.nextDouble() * 12;
            double a = i * 2.39996 + r.nextDouble() * 0.4; // golden angle spiral
            double pitch = 0.05 + r.nextDouble() * 0.2;
            double tr = trunkRadiusAt(h);
            branch(r, Math.cos(a) * tr * 0.5, h, Math.sin(a) * tr * 0.5, a, pitch, 300 + r.nextDouble() * 80, Math.max(8, tr * 0.55), 4);
        }
        for (Seg s : SEGMENTS) {
            for (int cx = Math.floorDiv((int) Math.floor(s.minX()), CELL); cx <= Math.floorDiv((int) Math.ceil(s.maxX()), CELL); cx++)
                for (int cz = Math.floorDiv((int) Math.floor(s.minZ()), CELL); cz <= Math.floorDiv((int) Math.ceil(s.maxZ()), CELL); cz++)
                    SEG_GRID.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(s);
        }
        for (Bubble b : BUBBLES)
            BUBBLE_GRID.computeIfAbsent(key(Math.floorDiv(b.x, CELL), Math.floorDiv(b.z, CELL)), k -> new ArrayList<>()).add(b);
    }

    private static double trunkRadiusAt(double y) {
        return 26 - 17 * Math.min(1, y / TRUNK_TOP);
    }

    private static void branch(Random r, double x, double y, double z, double yaw, double pitch, double len, double rad, int depth) {
        double ex = x + Math.cos(yaw) * Math.cos(pitch) * len;
        double ey = y + Math.sin(pitch) * len;
        double ez = z + Math.sin(yaw) * Math.cos(pitch) * len;
        if (ey > 470) ey = 455 + r.nextDouble() * 20;   // let the crown spread instead of piling up at the height limit
        ey = Math.max(130, ey);
        double endRad = Math.max(1.5, rad * 0.62);
        SEGMENTS.add(new Seg(x, y, z, ex, ey, ez, rad, endRad, false));
        // bubble universes hang along the outer branches...
        if (depth <= 1) {
            int n = depth == 0 ? 3 : 2;
            for (int k = 1; k <= n; k++) {
                double t = k / (double) (n + 1);
                int stalk = 3 + r.nextInt(6);
                double bx = x + (ex - x) * t, by = y + (ey - y) * t, bz = z + (ez - z) * t;
                BUBBLES.add(new Bubble((int) Math.round(bx), (int) Math.round(by - endRad) - stalk - 1, (int) Math.round(bz), stalk));
            }
        }
        if (depth == 0) {
            // ...and from every twig tip
            int stalk = 4 + r.nextInt(5);
            BUBBLES.add(new Bubble((int) Math.round(ex), (int) Math.round(ey) - stalk - 2, (int) Math.round(ez), stalk));
            return;
        }
        for (int k = 0; k < 3; k++) {
            double ny = yaw + (k - 1) * (0.55 + r.nextDouble() * 0.25);
            // outer limbs droop under their own (impossible) weight, like a great spreading canopy
            double np = Math.max(-0.35, Math.min(0.35, pitch * 0.5 + (r.nextDouble() - 0.62) * 0.45));
            branch(r, ex, ey, ez, ny, np, len * (0.66 + r.nextDouble() * 0.12), endRad, depth - 1);
        }
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    public static List<Seg> segmentsNear(int blockX, int blockZ) {
        return SEG_GRID.getOrDefault(key(Math.floorDiv(blockX, CELL), Math.floorDiv(blockZ, CELL)), List.of());
    }

    public static List<Bubble> bubblesNear(int blockX, int blockZ) {
        return BUBBLE_GRID.getOrDefault(key(Math.floorDiv(blockX, CELL), Math.floorDiv(blockZ, CELL)), List.of());
    }

    /**
     * Signed distance-ish test: 0 = outside, 1 = solid heartwood. Hollow segments (the trunk) are a
     * five-block thick shell around a vast, glowing interior chamber.
     */
    public static boolean inside(Seg s, double x, double y, double z) {
        double dx = s.x1 - s.x0, dy = s.y1 - s.y0, dz = s.z1 - s.z0;
        double len2 = dx * dx + dy * dy + dz * dz;
        double t = len2 < 1e-9 ? 0 : ((x - s.x0) * dx + (y - s.y0) * dy + (z - s.z0) * dz) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = s.x0 + dx * t - x, cy = s.y0 + dy * t - y, cz = s.z0 + dz * t - z;
        double d = Math.sqrt(cx * cx + cy * cy + cz * cz);
        double rad = s.r0 + (s.r1 - s.r0) * t;
        if (d > rad) return false;
        return !s.hollow || d > rad - 5;
    }

    private WorldTree() {}
}
