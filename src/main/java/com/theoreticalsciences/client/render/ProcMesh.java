package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/** Tiny immediate-mode helpers for building entity geometry from shaded boxes (entity vertex format). */
public final class ProcMesh {
    /** Axis-aligned box in the current pose. argb colour; normals per face so world lighting shades it properly. */
    public static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                           int argb, int light, int overlay) {
        PoseStack.Pose p = ps.last();
        float a = ((argb >>> 24) & 255) / 255F, r = ((argb >> 16) & 255) / 255F, g = ((argb >> 8) & 255) / 255F, b = (argb & 255) / 255F;
        // -Y, +Y, -Z, +Z, -X, +X
        quad(p, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r * 0.5F, g * 0.5F, b * 0.5F, a, light, overlay);
        quad(p, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, r, g, b, a, light, overlay);
        quad(p, vc, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0, 0, -1, r * 0.8F, g * 0.8F, b * 0.8F, a, light, overlay);
        quad(p, vc, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0, 0, 1, r * 0.8F, g * 0.8F, b * 0.8F, a, light, overlay);
        quad(p, vc, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0, r * 0.65F, g * 0.65F, b * 0.65F, a, light, overlay);
        quad(p, vc, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0, r * 0.65F, g * 0.65F, b * 0.65F, a, light, overlay);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz,
                             float r, float g, float b, float a, int light, int overlay) {
        v(p, vc, ax, ay, az, 0, 0, nx, ny, nz, r, g, b, a, light, overlay);
        v(p, vc, bx, by, bz, 1, 0, nx, ny, nz, r, g, b, a, light, overlay);
        v(p, vc, cx, cy, cz, 1, 1, nx, ny, nz, r, g, b, a, light, overlay);
        v(p, vc, dx, dy, dz, 0, 1, nx, ny, nz, r, g, b, a, light, overlay);
    }

    private static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float w, float nx, float ny, float nz,
                          float r, float g, float b, float a, int light, int overlay) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, a).setUv(u, w).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
    }

    /**
     * A gear in the XY plane centred on the origin: rim, teeth, spokes and hub. {@code angle} in degrees.
     */
    public static void gear(PoseStack ps, VertexConsumer vc, float radius, int teeth, float thick, float angle, int argb, int light, int overlay) {
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(angle));
        float t = thick / 2, rimW = radius * 0.16F;
        int segs = Math.max(12, teeth);
        float segLen = (float) (Math.PI * 2 * radius / segs) * 0.56F;
        for (int i = 0; i < segs; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(360F * i / segs));
            box(ps, vc, -segLen, radius - rimW, -t, segLen, radius, t, argb, light, overlay);
            ps.popPose();
        }
        float toothW = (float) (Math.PI * radius / teeth) * 0.45F;
        for (int i = 0; i < teeth; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(360F * i / teeth + 180F / teeth));
            box(ps, vc, -toothW, radius, -t * 0.9F, toothW, radius + radius * 0.14F + 0.02F, t * 0.9F, argb, light, overlay);
            ps.popPose();
        }
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(90F * i + 45F));
            box(ps, vc, -radius * 0.05F, 0, -t * 0.6F, radius * 0.05F, radius - rimW, t * 0.6F, argb, light, overlay);
            ps.popPose();
        }
        float h = radius * 0.16F;
        box(ps, vc, -h, -h, -t * 1.4F, h, h, t * 1.4F, darker(argb), light, overlay);
        ps.popPose();
    }

    /** A ring (hoop) in the XY plane: {@code segs} box segments, {@code band} wide radially and {@code depth} thick. */
    public static void ring(PoseStack ps, VertexConsumer vc, float radius, float band, float depth, int segs, int argb, int light, int overlay) {
        float segLen = (float) (Math.PI * 2 * radius / segs) * 0.55F;
        for (int i = 0; i < segs; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(360F * i / segs));
            box(ps, vc, -segLen, radius - band / 2, -depth / 2, segLen, radius + band / 2, depth / 2, argb, light, overlay);
            ps.popPose();
        }
    }

    /** A solid drum (approximated disc) in the XY plane: stacked boxes rotated around Z. */
    public static void drum(PoseStack ps, VertexConsumer vc, float radius, float depth, int argb, int light, int overlay) {
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(22.5F * i));
            float h = radius * 0.92F;
            box(ps, vc, -h * 0.41F, -h, -depth / 2, h * 0.41F, h, depth / 2, argb, light, overlay);
            box(ps, vc, -h, -h * 0.41F, -depth / 2, h, h * 0.41F, depth / 2, argb, light, overlay);
            ps.popPose();
        }
    }

    /** A rod along +Y from 0 to {@code len}, square section {@code w}. */
    public static void rod(PoseStack ps, VertexConsumer vc, float len, float w, int argb, int light, int overlay) {
        box(ps, vc, -w / 2, 0, -w / 2, w / 2, len, w / 2, argb, light, overlay);
    }

    public static int darker(int argb) {
        int a = argb & 0xFF000000;
        int r = (int) (((argb >> 16) & 255) * 0.6F), g = (int) (((argb >> 8) & 255) * 0.6F), b = (int) ((argb & 255) * 0.6F);
        return a | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int rgb, float a) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    private ProcMesh() {}
}
