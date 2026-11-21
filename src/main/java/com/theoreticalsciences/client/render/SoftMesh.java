package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * 1.2.0: smooth living shapes for the Biology creatures: ellipsoids, lumpy blobs, tapering tubes along curves and tori,
 * each with a true surface normal at every vertex (so the game's lighting shades them as round, not as boxes) and a
 * colour that runs from a lit top to a darker underside.
 */
public final class SoftMesh {
    private SoftMesh() {}

    static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float nx, float ny, float nz, int argb, int light, int ov) {
        vc.addVertex(p, x, y, z).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255).setUv(0, 0)
                .setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** a to b by t (ARGB). */
    static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 255, cb = (b >>> s) & 255;
            r |= ((int) (ca + (cb - ca) * t) & 255) << s;
        }
        return r;
    }

    /** An ellipsoid (rings x segs), from latitude lat0 to lat1 (-PI/2 bottom, PI/2 top); bump adds lumps of that size. */
    static void ellipsoid(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float rx, float ry, float rz, int top, int bot,
                          float lat0, float lat1, float bump, int seed, int light, int ov) {
        int rings = 10, segs = 16;
        PoseStack.Pose p = ps.last();
        float[][] px = new float[rings + 1][segs + 1], py = new float[rings + 1][segs + 1], pz = new float[rings + 1][segs + 1];
        float[][] nx = new float[rings + 1][segs + 1], ny = new float[rings + 1][segs + 1], nz = new float[rings + 1][segs + 1];
        for (int i = 0; i <= rings; i++) {
            float lat = lat0 + (lat1 - lat0) * i / rings;
            for (int j = 0; j <= segs; j++) {
                float lon = Mth.TWO_PI * j / segs;
                float ux = Mth.cos(lat) * Mth.cos(lon), uy = Mth.sin(lat), uz = Mth.cos(lat) * Mth.sin(lon);
                float k = 1 + bump * (Mth.sin(lon * 3 + seed) * Mth.sin(lat * 4 + seed * 0.7F) + 0.5F * Mth.sin(lon * 5 - lat * 3 + seed * 1.3F));
                px[i][j] = cx + ux * rx * k;
                py[i][j] = cy + uy * ry * k;
                pz[i][j] = cz + uz * rz * k;
                float gx = ux / rx, gy = uy / ry, gz = uz / rz, l = Mth.sqrt(gx * gx + gy * gy + gz * gz);
                nx[i][j] = gx / l;
                ny[i][j] = gy / l;
                nz[i][j] = gz / l;
            }
        }
        for (int i = 0; i < rings; i++)
            for (int j = 0; j < segs; j++) {
                int[][] q = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : q) {
                    int a = c[0], b = c[1];
                    v(p, vc, px[a][b], py[a][b], pz[a][b], nx[a][b], ny[a][b], nz[a][b], lerp(bot, top, 0.5F + 0.5F * ny[a][b]), light, ov);
                }
            }
    }

    static void ball(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float r, int top, int bot, int light, int ov) {
        ellipsoid(ps, vc, cx, cy, cz, r, r, r, top, bot, -Mth.HALF_PI, Mth.HALF_PI, 0, 0, light, ov);
    }

    static void blob(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float rx, float ry, float rz, int top, int bot, float bump, int seed,
                     int light, int ov) {
        ellipsoid(ps, vc, cx, cy, cz, rx, ry, rz, top, bot, -Mth.HALF_PI, Mth.HALF_PI, bump, seed, light, ov);
    }

    /** A tube through the points (x, y, z triples), radius r0 at the start tapering to r1 at the end; colour start to end. */
    static void tube(PoseStack ps, VertexConsumer vc, float[] pts, float r0, float r1, int c0, int c1, int light, int ov) {
        int n = pts.length / 3, sides = 8;
        if (n < 2) return;
        PoseStack.Pose p = ps.last();
        float[] nPrev = null;
        float[][] ring = new float[n][], norm = new float[n][];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1), b = Math.min(n - 1, i + 1);
            float tx = pts[b * 3] - pts[a * 3], ty = pts[b * 3 + 1] - pts[a * 3 + 1], tz = pts[b * 3 + 2] - pts[a * 3 + 2];
            float tl = Mth.sqrt(tx * tx + ty * ty + tz * tz);
            if (tl < 1e-6F) tl = 1;
            tx /= tl;
            ty /= tl;
            tz /= tl;
            float[] nn;
            if (nPrev == null) {
                float rx = Math.abs(ty) < 0.9F ? 0 : 1, ry = Math.abs(ty) < 0.9F ? 1 : 0;
                nn = cross(tx, ty, tz, rx, ry, 0);
            } else {
                float d = nPrev[0] * tx + nPrev[1] * ty + nPrev[2] * tz;       // parallel transport: no twisting
                nn = new float[]{nPrev[0] - d * tx, nPrev[1] - d * ty, nPrev[2] - d * tz};
            }
            float nl = Mth.sqrt(nn[0] * nn[0] + nn[1] * nn[1] + nn[2] * nn[2]);
            nn = new float[]{nn[0] / nl, nn[1] / nl, nn[2] / nl};
            float[] bb = cross(tx, ty, tz, nn[0], nn[1], nn[2]);
            nPrev = nn;
            float r = r0 + (r1 - r0) * i / (n - 1);
            ring[i] = new float[(sides + 1) * 3];
            norm[i] = new float[(sides + 1) * 3];
            for (int s = 0; s <= sides; s++) {
                float an = Mth.TWO_PI * s / sides, ca = Mth.cos(an), sa = Mth.sin(an);
                float ox = nn[0] * ca + bb[0] * sa, oy = nn[1] * ca + bb[1] * sa, oz = nn[2] * ca + bb[2] * sa;
                norm[i][s * 3] = ox;
                norm[i][s * 3 + 1] = oy;
                norm[i][s * 3 + 2] = oz;
                ring[i][s * 3] = pts[i * 3] + ox * r;
                ring[i][s * 3 + 1] = pts[i * 3 + 1] + oy * r;
                ring[i][s * 3 + 2] = pts[i * 3 + 2] + oz * r;
            }
        }
        for (int i = 0; i < n - 1; i++) {
            int ca = lerp(c0, c1, i / (float) (n - 1)), cb = lerp(c0, c1, (i + 1) / (float) (n - 1));
            for (int s = 0; s < sides; s++) {
                int[][] q = {{i, s, ca}, {i, s + 1, ca}, {i + 1, s + 1, cb}, {i + 1, s, cb}};
                for (int[] c : q) {
                    float[] rr = ring[c[0]], nm = norm[c[0]];
                    int k = c[1] * 3;
                    int col = lerp(ProcMesh.darker(c[2]), c[2], 0.5F + 0.5F * nm[k + 1]);
                    v(p, vc, rr[k], rr[k + 1], rr[k + 2], nm[k], nm[k + 1], nm[k + 2], col, light, ov);
                }
            }
        }
    }

    static float[] cross(float ax, float ay, float az, float bx, float by, float bz) {
        return new float[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    /** A torus round the y axis (big radius R, tube radius r), squashed in y by sy. */
    static void torus(PoseStack ps, VertexConsumer vc, float R, float r, float sy, int top, int bot, int light, int ov) {
        int segs = 20, sides = 10;
        PoseStack.Pose p = ps.last();
        for (int i = 0; i < segs; i++)
            for (int j = 0; j < sides; j++) {
                for (int[] c : new int[][]{{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}}) {
                    float u = Mth.TWO_PI * c[0] / segs, w = Mth.TWO_PI * c[1] / sides;
                    float cx = Mth.cos(u), cz = Mth.sin(u);
                    float nx = Mth.cos(w) * cx, ny = Mth.sin(w), nz = Mth.cos(w) * cz;
                    v(p, vc, (R + r * Mth.cos(w)) * cx, r * Mth.sin(w) * sy, (R + r * Mth.cos(w)) * cz, nx, ny, nz, lerp(bot, top, 0.5F + 0.5F * ny), light, ov);
                }
            }
    }

    /** Points along a quadratic curve from a through control m to b (n points). */
    static float[] curve(int n, float ax, float ay, float az, float mx, float my, float mz, float bx, float by, float bz) {
        float[] out = new float[n * 3];
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1), u = 1 - t;
            out[i * 3] = u * u * ax + 2 * u * t * mx + t * t * bx;
            out[i * 3 + 1] = u * u * ay + 2 * u * t * my + t * t * by;
            out[i * 3 + 2] = u * u * az + 2 * u * t * mz + t * t * bz;
        }
        return out;
    }
}
