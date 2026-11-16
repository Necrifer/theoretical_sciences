package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.theoreticalsciences.QHConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Random;

/**
 * 1.8.0, the sky of Omnipotency: an endless noon. Pale gold overhead, white at the horizon, and deep blue below the
 * islands; at the zenith a still light with slow rays and three wide halos turning round it, and far off, even by
 * day, the glints of everything that was made from here.
 */
public class EmpyreanSkyEffects extends DimensionSpecialEffects {
    private static final float[][] STARS = new float[700][5];

    static {
        Random r = new Random(33550337L);
        for (float[] s : STARS) {
            double th = Math.acos(2 * r.nextDouble() - 1), ph = r.nextDouble() * Math.PI * 2;
            s[0] = (float) (Math.sin(th) * Math.cos(ph));
            s[1] = (float) Math.cos(th);
            s[2] = (float) (Math.sin(th) * Math.sin(ph));
            s[3] = 0.2F + r.nextFloat() * r.nextFloat() * 0.8F;
            s[4] = r.nextFloat();
        }
    }

    public EmpyreanSkyEffects() {
        super(72F, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3(1.0, 0.97, 0.88);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        if (!QHConfig.FANCY_SKY.get()) return false;
        float time = (ticks + partialTick) / 20F;
        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            RenderSystem.defaultBlendFunc();
            BufferBuilder bg = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int seg = 32;
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                for (int band = 0; band < 12; band++) {
                    double t0 = Math.PI * band / 12, t1 = Math.PI * (band + 1) / 12;
                    quad(bg, a0, a1, t0, t1, skyColor(t0), skyColor(t1));
                }
            }
            draw(bg);

            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            // the far glints
            BufferBuilder st = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (float[] s : STARS) {
                float tw = 0.5F + 0.5F * (float) Math.sin(time * (0.4 + s[4] * 1.5) + s[4] * 40);
                billboard(st, s[0] * 100, s[1] * 100, s[2] * 100, 0.2F + s[3] * 0.5F, s[4] < 0.5F ? 0xFFFFFF : 0xFFE9A8, s[3] * tw * 0.7F);
            }
            draw(st);
            // the light at the zenith: a soft disc, then rays that turn very slowly
            BufferBuilder sun = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            float h = 100;
            for (int layer = 0; layer < 3; layer++) {
                float rad = 9 + layer * 9;
                float a = layer == 0 ? 0.95F : layer == 1 ? 0.45F : 0.22F;
                for (int i = 0; i < 48; i++) {
                    double a0 = i * Math.PI * 2 / 48, a1 = (i + 1) * Math.PI * 2 / 48;
                    col(sun.addVertex(0, h, 0), 0xFFFFFF, a);
                    col(sun.addVertex((float) Math.cos(a0) * rad, h, (float) Math.sin(a0) * rad), 0xFFF0C0, 0);
                    col(sun.addVertex((float) Math.cos(a1) * rad, h, (float) Math.sin(a1) * rad), 0xFFF0C0, 0);
                }
            }
            int rays = 24;
            for (int i = 0; i < rays; i++) {
                double a = i * Math.PI * 2 / rays + time * 0.01;
                float len = (i % 2 == 0 ? 78 : 50) + 8 * (float) Math.sin(time * 0.3 + i * 1.7);
                double w = 0.035 + 0.015 * Math.sin(time * 0.21 + i);
                float al = (i % 2 == 0 ? 0.20F : 0.12F);
                col(sun.addVertex((float) Math.cos(a - w) * 8, h, (float) Math.sin(a - w) * 8), 0xFFF4D0, al);
                col(sun.addVertex((float) Math.cos(a + w) * 8, h, (float) Math.sin(a + w) * 8), 0xFFF4D0, al);
                col(sun.addVertex((float) Math.cos(a) * len, h - len * 0.25F, (float) Math.sin(a) * len), 0xFFE9A8, 0);
            }
            draw(sun);
            // three halos round it
            BufferBuilder gl = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int ring = 0; ring < 3; ring++) {
                float rad = 30 + ring * 20, hh = 92 - ring * 10;
                double spin = time * (ring % 2 == 0 ? 0.006 : -0.008);
                int c = ring == 0 ? 0xFFFFFF : ring == 1 ? 0xFFE9A8 : 0xD8ECFF;
                int n = 160;
                for (int i = 0; i < n; i++) {
                    double a0 = spin + i * Math.PI * 2 / n, a1 = spin + (i + 1) * Math.PI * 2 / n;
                    float a = 0.22F + 0.14F * (float) Math.sin(i * Math.PI * 2 * (ring + 2) / n + time * 0.25);
                    skyLine(gl, (float) Math.cos(a0) * rad, hh, (float) Math.sin(a0) * rad, (float) Math.cos(a1) * rad, hh, (float) Math.sin(a1) * rad, 0.3F, c, a);
                }
                for (int i = 0; i < 12 * (ring + 1); i++) {            // marks on the halo, like the hours on a dial
                    double a0 = spin + i * Math.PI * 2 / (12 * (ring + 1));
                    float x0 = (float) Math.cos(a0) * rad, z0 = (float) Math.sin(a0) * rad;
                    skyLine(gl, x0, hh, z0, x0 * 1.05F, hh - 1.2F, z0 * 1.05F, 0.3F, c, 0.4F);
                }
            }
            draw(gl);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        return true;
    }

    /** Gold-white above, white at the horizon, a clear deep blue beneath. */
    private static int skyColor(double theta) {
        double t = theta / Math.PI;                       // 0 top .. 1 bottom
        double[] top = {255, 236, 178}, mid = {255, 252, 240}, low = {120, 176, 236}, deep = {52, 92, 180};
        double[] c;
        if (t < 0.5) c = mix(top, mid, Math.pow(t / 0.5, 1.6));
        else if (t < 0.62) c = mix(mid, low, (t - 0.5) / 0.12);
        else c = mix(low, deep, (t - 0.62) / 0.38);
        return ((int) c[0] << 16) | ((int) c[1] << 8) | (int) c[2];
    }

    private static double[] mix(double[] a, double[] b, double t) {
        return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static void quad(BufferBuilder bb, double a0, double a1, double t0, double t1, int c0, int c1) {
        float[] p00 = sph(a0, t0), p10 = sph(a1, t0), p01 = sph(a0, t1), p11 = sph(a1, t1);
        tri(bb, p00, c0, p01, c1, p11, c1);
        tri(bb, p00, c0, p11, c1, p10, c0);
    }

    private static float[] sph(double a, double t) {
        return new float[]{(float) (Math.sin(t) * Math.cos(a) * 120), (float) (Math.cos(t) * 120), (float) (Math.sin(t) * Math.sin(a) * 120)};
    }

    private static void tri(BufferBuilder bb, float[] a, int ca, float[] b, int cb, float[] c, int cc) {
        col(bb.addVertex(a[0], a[1], a[2]), ca, 1);
        col(bb.addVertex(b[0], b[1], b[2]), cb, 1);
        col(bb.addVertex(c[0], c[1], c[2]), cc, 1);
    }

    private static void col(VertexConsumer v, int rgb, float a) {
        v.setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, a);
    }

    private static void bandVert(BufferBuilder bb, double ang, float off, float tilt, int rgb, float a) {
        double x = Math.cos(ang) * 100, z = Math.sin(ang) * 100, y = off * 100;
        double y2 = y * Math.cos(tilt) - z * Math.sin(tilt), z2 = y * Math.sin(tilt) + z * Math.cos(tilt);
        col(bb.addVertex((float) x, (float) y2, (float) z2), rgb, a);
    }

    private static void billboard(BufferBuilder bb, float x, float y, float z, float s, int rgb, float a) {
        // axis-aligned cross facing the origin (camera)
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ay >= ax && ay >= az) {
            col(bb.addVertex(x - s, y, z - s), rgb, a); col(bb.addVertex(x + s, y, z - s), rgb, a);
            col(bb.addVertex(x + s, y, z + s), rgb, a); col(bb.addVertex(x - s, y, z + s), rgb, a);
        } else if (ax >= az) {
            col(bb.addVertex(x, y - s, z - s), rgb, a); col(bb.addVertex(x, y + s, z - s), rgb, a);
            col(bb.addVertex(x, y + s, z + s), rgb, a); col(bb.addVertex(x, y - s, z + s), rgb, a);
        } else {
            col(bb.addVertex(x - s, y - s, z), rgb, a); col(bb.addVertex(x + s, y - s, z), rgb, a);
            col(bb.addVertex(x + s, y + s, z), rgb, a); col(bb.addVertex(x - s, y + s, z), rgb, a);
        }
    }

    private static void skyLine(BufferBuilder bb, float x0, float y0, float z0, float x1, float y1, float z1, float w, int rgb, float a) {
        col(bb.addVertex(x0 - w, y0, z0), rgb, a); col(bb.addVertex(x0 + w, y0, z0), rgb, a);
        col(bb.addVertex(x1 + w, y1, z1), rgb, a); col(bb.addVertex(x1 - w, y1, z1), rgb, a);
        col(bb.addVertex(x0, y0, z0 - w), rgb, a); col(bb.addVertex(x0, y0, z0 + w), rgb, a);
        col(bb.addVertex(x1, y1, z1 + w), rgb, a); col(bb.addVertex(x1, y1, z1 - w), rgb, a);
    }

    private static void draw(BufferBuilder bb) {
        MeshData m = bb.build();
        if (m != null) BufferUploader.drawWithShader(m);
    }
}
