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
 * Sky of Hyperdimensional Reality Space: no stars of any one universe, but the glow of countless bubble
 * universes, aurora-like ribbons of newly-born laws, and vast rings of glyphs - the axioms themselves -
 * turning slowly overhead.
 */
public class HyperrealSkyEffects extends DimensionSpecialEffects {
    private static final float[][] STARS = new float[2200][5];

    static {
        Random r = new Random(9091);
        for (float[] s : STARS) {
            double th = Math.acos(2 * r.nextDouble() - 1), ph = r.nextDouble() * Math.PI * 2;
            s[0] = (float) (Math.sin(th) * Math.cos(ph));
            s[1] = (float) Math.cos(th);
            s[2] = (float) (Math.sin(th) * Math.sin(ph));
            s[3] = 0.15F + r.nextFloat() * r.nextFloat() * 0.9F;
            s[4] = r.nextFloat();
        }
    }

    public HyperrealSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3(0.06, 0.04, 0.12);
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
            // background gradient dome
            RenderSystem.defaultBlendFunc();
            BufferBuilder bg = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int seg = 32;
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                for (int band = 0; band < 6; band++) {
                    double t0 = Math.PI * band / 6, t1 = Math.PI * (band + 1) / 6;
                    int c0 = skyColor(t0), c1 = skyColor(t1);
                    quad(bg, a0, a1, t0, t1, c0, c1);
                }
            }
            draw(bg);

            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            // nebula bands
            BufferBuilder neb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int k = 0; k < 3; k++) {
                float tilt = 0.4F + k * 0.7F, drift = time * 0.004F * (k + 1);
                int col = k == 0 ? 0x40FFC0 : k == 1 ? 0xC080FF : 0xFFD070;
                for (int i = 0; i < 64; i++) {
                    double a0 = i * Math.PI * 2 / 64 + drift, a1 = (i + 1) * Math.PI * 2 / 64 + drift;
                    float w = 0.12F + 0.1F * (float) Math.sin(i * 0.7 + k);
                    float alpha = 0.14F + 0.12F * (float) Math.sin(i * 0.33 + k * 2 + time * 0.2);
                    bandVert(neb, a0, -w, tilt, col, 0);
                    bandVert(neb, a0, 0, tilt, col, alpha);
                    bandVert(neb, a1, 0, tilt, col, alpha);
                    bandVert(neb, a1, -w, tilt, col, 0);
                    bandVert(neb, a0, 0, tilt, col, alpha);
                    bandVert(neb, a0, w, tilt, col, 0);
                    bandVert(neb, a1, w, tilt, col, 0);
                    bandVert(neb, a1, 0, tilt, col, alpha);
                }
            }
            draw(neb);
            // stars
            BufferBuilder st = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (float[] s : STARS) {
                float tw = 0.6F + 0.4F * (float) Math.sin(time * (1 + s[4] * 3) + s[4] * 40);
                float size = 0.35F + s[3] * 1.1F; // each 'star' is a whole bubble universe
                int col = s[4] < 0.3F ? 0xFFD080 : s[4] < 0.6F ? 0x80FFE0 : 0xE0C0FF;
                billboard(st, s[0] * 100, s[1] * 100, s[2] * 100, size, col, s[3] * tw);
            }
            draw(st);
            // the axioms: great rings of glyphs turning against each other overhead
            BufferBuilder gl = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int ring = 0; ring < 3; ring++) {
                float rad = 40 + ring * 22, h = 75 - ring * 8;
                double spin = time * (ring % 2 == 0 ? 0.01 : -0.014) * (ring + 1);
                int n = 72 + ring * 24;
                int c = ring == 0 ? 0xFFE0A0 : ring == 1 ? 0x80FFE0 : 0xD0A0FF;
                for (int i = 0; i < n; i++) {
                    double a0 = i * Math.PI * 2 / n + spin;
                    float x0 = (float) Math.cos(a0) * rad, z0 = (float) Math.sin(a0) * rad;
                    float glyph = (float) ((Math.sin(i * 12.9898 + ring * 78.233) * 43758.5453) % 1 + 1) % 1;
                    float len = 1.0F + glyph * 3.5F;
                    float a = 0.25F + 0.35F * (float) Math.max(0, Math.sin(time * 0.7 + i * 0.4 + ring));
                    skyLine(gl, x0, h, z0, x0, h - len, z0, 0.35F, c, a);
                    if (glyph > 0.55F) skyLine(gl, x0, h - len, z0, x0 * 1.02F, h - len, z0 * 1.02F, 0.35F, c, a);
                }
                double b0 = spin;
                for (int i = 0; i < 128; i++) {
                    double a0 = b0 + i * Math.PI * 2 / 128, a1 = b0 + (i + 1) * Math.PI * 2 / 128;
                    skyLine(gl, (float) Math.cos(a0) * rad, h + 1.5F, (float) Math.sin(a0) * rad, (float) Math.cos(a1) * rad, h + 1.5F, (float) Math.sin(a1) * rad, 0.15F, c, 0.3F);
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

    private static int skyColor(double theta) {
        double t = theta / Math.PI; // 0 top .. 1 bottom
        int r = (int) (14 + 20 * t), g = (int) (8 + 40 * Math.pow(t, 3)), b = (int) (36 + 40 * (1 - Math.abs(t - 0.5) * 2));
        return (r << 16) | (g << 8) | b;
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
