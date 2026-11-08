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
 * 1.4.0: the sky over the Orbital Station, four hundred kilometres up.
 * <ul>
 *   <li>Black space, and stars that do not twinkle (there is no air to make them).</li>
 *   <li>The Earth filling the lower sky: deep blue ocean, the browns and greens of land and white weather, turning slowly
 *   past (one orbit takes an hour and a half; here, a few minutes).</li>
 *   <li>Round its edge the <b>limb</b>: the thin blue shell of atmosphere seen edge-on, brightest where it is thickest.</li>
 * </ul>
 * Respects the client config {@code fancySky} like the mod's other skies (vanilla's void sky otherwise).
 */
public class OrbitSkyEffects extends DimensionSpecialEffects {
    private static final float[][] STARS = new float[2200][4];
    /** The Earth's angular radius seen from low orbit: it fills most of the lower sky. */
    private static final double EARTH = Math.toRadians(70);

    static {
        Random r = new Random(1404);
        for (float[] s : STARS) {
            double th = Math.acos(2 * r.nextDouble() - 1), ph = r.nextDouble() * Math.PI * 2;
            s[0] = (float) (Math.sin(th) * Math.cos(ph));
            s[1] = (float) Math.cos(th);
            s[2] = (float) (Math.sin(th) * Math.sin(ph));
            s[3] = 0.15F + r.nextFloat() * r.nextFloat() * 0.85F;
        }
    }

    public OrbitSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(0.1, 0.1, 0.15);
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
            // stars, steady
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            BufferBuilder st = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (float[] s : STARS) {
                if (Math.acos(-s[1]) < EARTH) continue;            // hidden behind the Earth
                billboard(st, s[0] * 100, s[1] * 100, s[2] * 100, 0.2F + s[3] * 0.5F, 0xFFFFFF, s[3]);
            }
            draw(st);

            // the Earth: a cap of the sky sphere round straight down, patterned and turning slowly
            RenderSystem.defaultBlendFunc();
            BufferBuilder e = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int seg = 64, rings = 18;
            double spin = time * 0.004;
            for (int i = 0; i < seg; i++)
                for (int k = 0; k < rings; k++) {
                    double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                    double t0 = EARTH * k / rings, t1 = EARTH * (k + 1) / rings;
                    vtx(e, down(a0, t0), surface(a0, t0, spin), 1);
                    vtx(e, down(a1, t0), surface(a1, t0, spin), 1);
                    vtx(e, down(a1, t1), surface(a1, t1, spin), 1);
                    vtx(e, down(a0, t1), surface(a0, t1, spin), 1);
                }
            draw(e);

            // the limb: a thin band of blue air round the Earth's edge, fading out into space
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            BufferBuilder limb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            double w = Math.toRadians(4);
            for (int i = 0; i < seg; i++) {
                double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                vtx(limb, down(a0, EARTH - w * 0.3), 0x80C8FF, 0.9F);
                vtx(limb, down(a1, EARTH - w * 0.3), 0x80C8FF, 0.9F);
                vtx(limb, down(a1, EARTH + w), 0x2050C0, 0);
                vtx(limb, down(a0, EARTH + w), 0x2050C0, 0);
            }
            draw(limb);
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        return true;
    }

    /** A point on the sky sphere at angle t from straight down, round at azimuth a. */
    private static float[] down(double a, double t) {
        return new float[]{(float) (Math.sin(t) * Math.cos(a) * 120), (float) (-Math.cos(t) * 120), (float) (Math.sin(t) * Math.sin(a) * 120)};
    }

    /** Ocean, land and cloud by a cheap value noise on the turning surface; darker towards the edge (seen slantwise). */
    private static int surface(double a, double t, double spin) {
        double u = Math.sin(t) * Math.cos(a) + spin, v = Math.sin(t) * Math.sin(a);
        double land = noise(u * 3.1, v * 3.1), cloud = noise(u * 7.3 + 11, v * 7.3 - 5);
        int r, g, b;
        if (cloud > 0.62) { r = 232; g = 238; b = 245; }
        else if (land > 0.58) { r = land > 0.7 ? 150 : 80; g = land > 0.7 ? 120 : 112; b = land > 0.7 ? 80 : 60; }
        else { r = 20; g = 60; b = 140; }
        double shade = 0.45 + 0.55 * Math.cos(t / EARTH * Math.PI / 2);
        return ((int) (r * shade) << 16) | ((int) (g * shade) << 8) | (int) (b * shade);
    }

    private static double noise(double x, double y) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }

    private static double hash(int x, int y) {
        long h = x * 374761393L + y * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0;
    }

    private static void vtx(BufferBuilder bb, float[] p, int rgb, float a) {
        bb.addVertex(p[0], p[1], p[2]).setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, a);
    }

    private static void billboard(BufferBuilder bb, float x, float y, float z, float s, int rgb, float a) {
        float ax = Math.abs(x), ay = Math.abs(y), az = Math.abs(z);
        if (ay >= ax && ay >= az) {
            vtx(bb, new float[]{x - s, y, z - s}, rgb, a); vtx(bb, new float[]{x + s, y, z - s}, rgb, a);
            vtx(bb, new float[]{x + s, y, z + s}, rgb, a); vtx(bb, new float[]{x - s, y, z + s}, rgb, a);
        } else if (ax >= az) {
            vtx(bb, new float[]{x, y - s, z - s}, rgb, a); vtx(bb, new float[]{x, y + s, z - s}, rgb, a);
            vtx(bb, new float[]{x, y + s, z + s}, rgb, a); vtx(bb, new float[]{x, y - s, z + s}, rgb, a);
        } else {
            vtx(bb, new float[]{x - s, y - s, z}, rgb, a); vtx(bb, new float[]{x + s, y - s, z}, rgb, a);
            vtx(bb, new float[]{x + s, y + s, z}, rgb, a); vtx(bb, new float[]{x - s, y + s, z}, rgb, a);
        }
    }

    private static void draw(BufferBuilder bb) {
        MeshData m = bb.build();
        if (m != null) BufferUploader.drawWithShader(m);
    }
}
