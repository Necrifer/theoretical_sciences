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
import org.joml.Vector3f;

import java.util.Random;

/**
 * 1.10.1: the sky of the Accretion Frontier - the black hole the whole region circles, hanging in the south-east sky.
 * <ul>
 *   <li>A black disc (the shadow, 2.6 times the horizon's radius as seen from far away) ringed by a thin, bright
 *   <b>photon ring</b>.</li>
 *   <li>A tilted <b>accretion disk</b>, white-hot inside and red at its edge, drawn in two halves - the far half
 *   behind the shadow and the near half in front - and brighter on the side turning towards you (Doppler beaming).</li>
 *   <li>The far side of the disk <b>lensed</b> up over the top of the shadow and under its bottom, as light from
 *   behind the hole is bent round it.</li>
 *   <li>Stars near the hole pushed outwards from it by the same lensing (an Einstein ring of crowded starlight), and
 *   two faint <b>jets</b> along its spin axis.</li>
 * </ul>
 */
public class FrontierSkyEffects extends DimensionSpecialEffects {
    private static final float[][] STARS = new float[1800][5];
    /** Where the hole sits: a unit vector, and two unit vectors across the sky at it ("right" and "up"). */
    private static final Vector3f D = new Vector3f(0.55F, 0.42F, 0.72F).normalize();
    private static final Vector3f U = new Vector3f(0, 1, 0).cross(D, new Vector3f()).normalize();
    private static final Vector3f V = new Vector3f(D).cross(U, new Vector3f()).normalize();
    /** The shadow's radius and the Einstein radius, as angles (radians). */
    private static final float SHADOW = 0.075F, EINSTEIN = 0.16F;

    static {
        Random r = new Random(1010);
        for (float[] s : STARS) {
            double th = Math.acos(2 * r.nextDouble() - 1), ph = r.nextDouble() * Math.PI * 2;
            Vector3f p = new Vector3f((float) (Math.sin(th) * Math.cos(ph)), (float) Math.cos(th), (float) (Math.sin(th) * Math.sin(ph)));
            // gravitational lensing by a point mass: a star at angle b from the hole appears at (b + sqrt(b^2 + 4 bE^2)) / 2
            float b = (float) Math.acos(Math.max(-1, Math.min(1, p.dot(D))));
            if (b > 1e-3F && b < 1.2F) {
                float b2 = (b + (float) Math.sqrt(b * b + 4 * EINSTEIN * EINSTEIN)) / 2;
                Vector3f axis = new Vector3f(D).cross(p).normalize();
                p.rotateAxis(b2 - b, axis.x, axis.y, axis.z);
            }
            s[0] = p.x;
            s[1] = p.y;
            s[2] = p.z;
            s[3] = 0.12F + r.nextFloat() * r.nextFloat() * 0.9F;
            s[4] = r.nextFloat();
        }
    }

    public FrontierSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(0.25, 0.25, 0.3);
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
            // the backdrop: almost black, a little bluer towards the hole's side of the sky
            BufferBuilder bg = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int seg = 32;
            for (int i = 0; i < seg; i++)
                for (int band = 0; band < 8; band++) {
                    double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg, t0 = Math.PI * band / 8, t1 = Math.PI * (band + 1) / 8;
                    vtx(bg, sph(a0, t0), back(a0, t0), 1);
                    vtx(bg, sph(a1, t0), back(a1, t0), 1);
                    vtx(bg, sph(a1, t1), back(a1, t1), 1);
                    vtx(bg, sph(a0, t1), back(a0, t1), 1);
                }
            draw(bg);

            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            BufferBuilder st = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (float[] s : STARS) {
                float tw = 0.65F + 0.35F * (float) Math.sin(time * (1 + s[4] * 3) + s[4] * 40);
                int col = s[4] < 0.2F ? 0xFFC8A0 : s[4] < 0.5F ? 0xC0D0FF : 0xFFFFFF;
                billboard(st, s[0] * 100, s[1] * 100, s[2] * 100, 0.22F + s[3] * 0.55F, col, s[3] * tw);
            }
            draw(st);

            float spin = time * 0.15F;
            // jets along the spin axis (the sky's "up" at the hole, tilted with the disk)
            BufferBuilder j = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int sgn = -1; sgn <= 1; sgn += 2)
                for (int k = 0; k < 12; k++) {
                    float r0 = SHADOW * (1.2F + k * 0.9F), r1 = SHADOW * (1.2F + (k + 1) * 0.9F), w0 = 0.006F + 0.004F * k, w1 = w0 + 0.004F;
                    float a = 0.35F * (1 - k / 12F) * (0.8F + 0.2F * (float) Math.sin(time * 2 + k));
                    quad(j, -w0, sgn * r0, w0, sgn * r0, w1, sgn * r1, -w1, sgn * r1, 0x90B8FF, a, a * 0.8F);
                }
            draw(j);
            disk(time, spin, true);
            lensedArc(spin);
            // the shadow itself: ordinary black paint, over the far half of the disk and the stars behind it
            RenderSystem.defaultBlendFunc();
            BufferBuilder sh = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 64; i++) {
                double a0 = i * Math.PI * 2 / 64, a1 = (i + 1) * Math.PI * 2 / 64;
                vtx(sh, at(0, 0), 0, 1);
                vtx(sh, at((float) Math.cos(a0) * SHADOW, (float) Math.sin(a0) * SHADOW), 0, 1);
                vtx(sh, at((float) Math.cos(a1) * SHADOW, (float) Math.sin(a1) * SHADOW), 0, 1);
            }
            draw(sh);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            // the photon ring: light that has orbited the hole before escaping
            BufferBuilder pr = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            ring(pr, SHADOW * 1.0F, SHADOW * 1.05F, 0xFFF0D0, 0.95F, 0.95F);
            ring(pr, SHADOW * 1.05F, SHADOW * 1.35F, 0xFFB060, 0.35F, 0);
            draw(pr);
            disk(time, spin, false);
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

    /** The disk, seen 12 degrees from edge-on: an ellipse in the sky. far = the half behind the hole (above its centre). */
    private static void disk(float time, float spin, boolean far) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float squash = 0.22F, in = SHADOW * 1.5F, out = SHADOW * 4.2F;
        int seg = 48, rings = 5;
        for (int i = 0; i < seg; i++) {
            float a0 = (far ? 0 : (float) Math.PI) + (float) Math.PI * i / seg, a1 = a0 + (float) Math.PI / seg;
            float mid = (a0 + a1) / 2;
            // Doppler beaming: the side coming towards you (left) is brighter; hot clumps orbit faster near the inside
            float beam = 0.45F + 0.55F * (float) Math.cos(mid - (float) Math.PI);
            for (int k = 0; k < rings; k++) {
                float r0 = in + (out - in) * k / rings, r1 = in + (out - in) * (k + 1) / rings;
                float clump = 0.75F + 0.25F * (float) Math.sin(mid * 7 - spin * (6 - k) + k * 1.3F);
                int c0 = heat(k / (float) rings), c1 = heat((k + 1) / (float) rings);
                float al0 = 0.9F * beam * clump * (1 - k / (float) rings * 0.6F), al1 = k == rings - 1 ? 0 : al0 * 0.85F;
                v(b, (float) Math.cos(a0) * r0, (float) Math.sin(a0) * r0 * squash, c0, al0);
                v(b, (float) Math.cos(a1) * r0, (float) Math.sin(a1) * r0 * squash, c0, al0);
                v(b, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1 * squash, c1, al1);
                v(b, (float) Math.cos(a0) * r1, (float) Math.sin(a0) * r1 * squash, c1, al1);
            }
        }
        draw(b);
    }

    /** The far side of the disk, bent over the top of the shadow (and, fainter, under it) by the hole's gravity. */
    private static void lensedArc(float spin) {
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 48;
        for (int sgn = 1; sgn >= -1; sgn -= 2)
            for (int i = 0; i < seg; i++) {
                float a0 = (float) Math.PI * i / seg, a1 = (float) Math.PI * (i + 1) / seg, mid = (a0 + a1) / 2;
                float r0 = SHADOW * 1.1F, r1 = SHADOW * (sgn > 0 ? 1.75F : 1.35F);
                float beam = 0.5F + 0.5F * (float) Math.cos(mid - (float) Math.PI), al = (sgn > 0 ? 0.55F : 0.25F) * beam
                        * (0.8F + 0.2F * (float) Math.sin(mid * 5 - spin * 4));
                v(b, (float) Math.cos(a0) * r0, sgn * (float) Math.sin(a0) * r0, 0xFFE0A0, al);
                v(b, (float) Math.cos(a1) * r0, sgn * (float) Math.sin(a1) * r0, 0xFFE0A0, al);
                v(b, (float) Math.cos(a1) * r1, sgn * (float) Math.sin(a1) * r1, 0xFF7030, 0);
                v(b, (float) Math.cos(a0) * r1, sgn * (float) Math.sin(a0) * r1, 0xFF7030, 0);
            }
        draw(b);
    }

    /** White-hot at the inner edge, through yellow and orange, to dull red outside. */
    private static int heat(float f) {
        int[] pal = {0xFFFFF0, 0xFFE090, 0xFFA040, 0xF06020, 0xA02810, 0x601008};
        return pal[Math.min(pal.length - 1, Math.round(f * (pal.length - 1)))];
    }

    private static void ring(BufferBuilder b, float r0, float r1, int rgb, float a0, float a1) {
        for (int i = 0; i < 72; i++) {
            float t0 = (float) (i * Math.PI * 2 / 72), t1 = (float) ((i + 1) * Math.PI * 2 / 72);
            v(b, (float) Math.cos(t0) * r0, (float) Math.sin(t0) * r0, rgb, a0);
            v(b, (float) Math.cos(t1) * r0, (float) Math.sin(t1) * r0, rgb, a0);
            v(b, (float) Math.cos(t1) * r1, (float) Math.sin(t1) * r1, rgb, a1);
            v(b, (float) Math.cos(t0) * r1, (float) Math.sin(t0) * r1, rgb, a1);
        }
    }

    private static void quad(BufferBuilder b, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int rgb, float a0, float a1) {
        v(b, x0, y0, rgb, a0);
        v(b, x1, y1, rgb, a0);
        v(b, x2, y2, rgb, a1);
        v(b, x3, y3, rgb, a1);
    }

    /** A point at (x, y) radians across the sky from the hole's centre, 100 blocks out. */
    private static float[] at(float x, float y) {
        Vector3f p = new Vector3f(D).add(new Vector3f(U).mul(x)).add(new Vector3f(V).mul(y)).normalize(100);
        return new float[]{p.x, p.y, p.z};
    }

    private static void v(BufferBuilder b, float x, float y, int rgb, float a) {
        vtx(b, at(x, y), rgb, a);
    }

    private static int back(double a, double t) {
        Vector3f p = new Vector3f((float) (Math.sin(t) * Math.cos(a)), (float) Math.cos(t), (float) (Math.sin(t) * Math.sin(a)));
        float near = Math.max(0, p.dot(D));
        int r = (int) (3 + 6 * near), g = (int) (3 + 6 * near), bl = (int) (10 + 18 * near);
        return (r << 16) | (g << 8) | bl;
    }

    private static float[] sph(double a, double t) {
        return new float[]{(float) (Math.sin(t) * Math.cos(a) * 120), (float) (Math.cos(t) * 120), (float) (Math.sin(t) * Math.sin(a) * 120)};
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
