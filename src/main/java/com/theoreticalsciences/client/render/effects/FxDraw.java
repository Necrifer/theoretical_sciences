package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1.2.0: light drawn into the world: strips that face the camera (beams, lines, arcs), soft round glows, and glowing
 * boxes, all added onto what is behind them (so they read as light, not paint). World positions; the camera must be set
 * with {@link #begin} first and everything is drawn at {@link #end}.
 */
public final class FxDraw {
    private static Vec3 cam;
    private static Vector3f left, up;
    private static BufferBuilder bb;

    public static void begin(Vec3 camera, Vector3f l, Vector3f u) {
        cam = camera;
        left = l;
        up = u;
        bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    public static void end() {
        if (bb == null) return;
        MeshData mesh = bb.build();
        bb = null;
        if (mesh == null) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    public static boolean active() {
        return bb != null;
    }

    private static void v(Vec3 p, int rgb, float a) {
        bb.addVertex((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, Math.max(0, Math.min(1, a)));
    }

    /** A flat strip from a to b that turns to face the camera. */
    public static void ribbon(Vec3 a, Vec3 b, float width, int rgbA, float alphaA, int rgbB, float alphaB) {
        if (alphaA <= 0.003F && alphaB <= 0.003F) return;
        Vec3 dir = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(view);
        double len = side.length();
        if (len < 1e-6) return;
        side = side.scale(width / 2 / len);
        v(a.subtract(side), rgbA, alphaA);
        v(a.add(side), rgbA, alphaA);
        v(b.add(side), rgbB, alphaB);
        v(b.subtract(side), rgbB, alphaB);
    }

    /** A beam: a wide soft glow with a thin bright core. */
    public static void beam(Vec3 a, Vec3 b, float width, int rgb, float alpha) {
        ribbon(a, b, width * 3, rgb, 0.18F * alpha, rgb, 0.18F * alpha);
        ribbon(a, b, width, rgb, 0.6F * alpha, rgb, 0.6F * alpha);
        ribbon(a, b, width * 0.3F, 0xFFFFFF, 0.9F * alpha, 0xFFFFFF, 0.9F * alpha);
    }

    /** A soft round glow facing the camera, brightest in the middle. */
    public static void glow(Vec3 c, float radius, int rgb, float alpha) {
        if (alpha <= 0.003F || radius <= 0) return;
        int seg = 14;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            Vec3 p0 = c.add(off(a0, radius)), p1 = c.add(off(a1, radius));
            v(c, rgb, alpha);
            v(p0, rgb, 0);
            v(p1, rgb, 0);
            v(p1, rgb, 0);
        }
    }

    /** A ring of light: a circle of radius r round c, in the plane through c with normal n. */
    public static void circle(Vec3 c, Vec3 n, double r, float width, int rgb, float alpha, double phase) {
        if (alpha <= 0.003F || r <= 0.01) return;
        Vec3 nn = n.normalize();
        Vec3 u = Math.abs(nn.y) < 0.9 ? new Vec3(0, 1, 0).cross(nn).normalize() : new Vec3(1, 0, 0).cross(nn).normalize();
        Vec3 w = nn.cross(u);
        int seg = Math.max(16, Math.min(96, (int) (r * 6)));
        Vec3 prev = null;
        for (int i = 0; i <= seg; i++) {
            double a = phase + i * Math.PI * 2 / seg;
            Vec3 p = c.add(u.scale(Math.cos(a) * r)).add(w.scale(Math.sin(a) * r));
            if (prev != null) ribbon(prev, p, width, rgb, alpha, rgb, alpha);
            prev = p;
        }
    }

    /** A glowing box round a block (min corner p, size s), all six faces. */
    public static void box(Vec3 p, double s, int rgb, float a) {
        if (a <= 0.003F) return;
        double x0 = p.x, y0 = p.y, z0 = p.z, x1 = x0 + s, y1 = y0 + s, z1 = z0 + s;
        quad(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), rgb, a);
        quad(new Vec3(x0, y0, z1), new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), rgb, a);
        quad(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y0, z0), rgb, a);
        quad(new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), new Vec3(x0, y0, z1), rgb, a);
        quad(new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), new Vec3(x0, y0, z0), rgb, a);
        quad(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), rgb, a);
    }

    private static void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float al) {
        v(a, rgb, al);
        v(b, rgb, al);
        v(c, rgb, al);
        v(d, rgb, al);
    }

    private static Vec3 off(double a, double r) {
        double c = Math.cos(a) * r, s = Math.sin(a) * r;
        return new Vec3(left.x() * c + up.x() * s, left.y() * c + up.y() * s, left.z() * c + up.z() * s);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) ((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = (int) ((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }

    private FxDraw() {}
}
