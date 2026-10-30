package com.theoreticalsciences.client.render;

/**
 * A hypercube (tesseract) rotated in 4D and projected into 3D with a perspective divide along W.
 * What you see is the tesseract's 3D "shadow": an inner cube that turns itself inside out through the outer one.
 */
public final class Tesseract4D {
    public static final float[][] VERTS = new float[16][4];
    public static final int[][] EDGES = new int[32][2];

    static {
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 4; k++) VERTS[i][k] = ((i >> k) & 1) == 0 ? -1 : 1;
        int e = 0;
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 4; k++) {
                int j = i ^ (1 << k);
                if (j > i) EDGES[e++] = new int[]{i, j};
            }
    }

    /**
     * @param t time in seconds
     * @return 16 projected vertices as {x, y, z, w} where w is the rotated 4th coordinate (for colouring)
     */
    public static float[][] project(float t, float wDistance) {
        float[][] out = new float[16][4];
        float a = t * 0.55F, b = t * 0.31F, c = t * 0.17F;
        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
        float cb = (float) Math.cos(b), sb = (float) Math.sin(b);
        float cc = (float) Math.cos(c), sc = (float) Math.sin(c);
        for (int i = 0; i < 16; i++) {
            float x = VERTS[i][0], y = VERTS[i][1], z = VERTS[i][2], w = VERTS[i][3];
            // XW rotation
            float x1 = x * ca - w * sa, w1 = x * sa + w * ca;
            // YW rotation
            float y1 = y * cb - w1 * sb, w2 = y * sb + w1 * cb;
            // ZW rotation (slow)
            float z1 = z * cc - w2 * sc, w3 = z * sc + w2 * cc;
            // ordinary XZ spin
            float x2 = x1 * cc - z1 * sc, z2 = x1 * sc + z1 * cc;
            float k = 1.0F / (wDistance - w3);
            out[i][0] = x2 * k;
            out[i][1] = y1 * k;
            out[i][2] = z2 * k;
            out[i][3] = w3;
        }
        return out;
    }

    private Tesseract4D() {}
}
