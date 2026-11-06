package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.util.Mth;

/**
 * 1.2.0: the meshes the effects share, baked once into GPU buffers and only scaled and turned per frame: a unit sphere
 * (the stars, their coronas and the space shell all use it). Freed on a resource reload ({@link #release}) and rebuilt
 * the next time they are drawn.
 */
public final class FxMesh {
    private static VertexBuffer sphere;
    static int sphereQuads;

    /** A unit sphere (POSITION_COLOR, white) of 48 x 24 quads, wound for its outside. */
    public static VertexBuffer sphere() {
        if (sphere != null && !sphere.isInvalid()) return sphere;
        int lon = 48, lat = 24;
        try (ByteBufferBuilder bb = new ByteBufferBuilder(lon * lat * 4 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int j = 0; j < lat; j++) {
                float t0 = Mth.PI * j / lat - Mth.HALF_PI, t1 = Mth.PI * (j + 1) / lat - Mth.HALF_PI;
                for (int i = 0; i < lon; i++) {
                    float p0 = Mth.TWO_PI * i / lon, p1 = Mth.TWO_PI * (i + 1) / lon;
                    v(b, t0, p0);                       // counter-clockwise seen from outside
                    v(b, t1, p0);
                    v(b, t1, p1);
                    v(b, t0, p1);
                }
            }
            sphere = new VertexBuffer(VertexBuffer.Usage.STATIC);
            sphere.bind();
            sphere.upload(b.buildOrThrow());
            VertexBuffer.unbind();
            sphereQuads = lon * lat;
        }
        return sphere;
    }

    private static void v(BufferBuilder b, float th, float ph) {
        float c = Mth.cos(th);
        b.addVertex(c * Mth.cos(ph), Mth.sin(th), c * Mth.sin(ph)).setColor(255, 255, 255, 255);
    }

    public static void release() {
        if (sphere != null) sphere.close();
        sphere = null;
    }

    private FxMesh() {}
}
