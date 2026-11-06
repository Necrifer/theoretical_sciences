package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.theoreticalsciences.client.render.QHShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1.2.0: a star: three spheres, one inside the next, each turning slowly about its own axis. The inner one is the
 * opaque surface (granulated plasma, darker at the edge); the two outer ones are faint, added-on layers of corona,
 * brighter at the rim. The surface is a noise shader on the sphere ({@code ts_star}), so no texture is shipped; without
 * the shader (it failed to load) the spheres are drawn in plain colour.
 */
public final class StarEffect {
    // layer: alpha, scale, axis, starting angle (degrees), turn (degrees a second)
    private static final float[][] LAYERS = {{1.0F, 1.00F, 0, 1, 1, 130, 3.0F}, {0.4F, 1.02F, 1, 1, 0, -49, -4.5F}, {0.2F, 1.04F, 1, 0, 1, 67, 6.0F}};

    /** The opaque surface only (draw it before anything translucent). mv: machine space, centre in blocks. */
    public static void drawSurface(FxFrame f, Matrix4f mv, Vector3f centre, float radius, int rgb, float power) {
        if (power <= 0.001F || radius <= 0) return;
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        layer(f, mv, centre, radius * size(power), rgb, 0, 1F);
    }

    /** The two added-on corona layers (draw them after the opaque things). */
    public static void drawCorona(FxFrame f, Matrix4f mv, Vector3f centre, float radius, int rgb, float power) {
        if (power <= 0.001F || radius <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        float r = radius * size(power);
        layer(f, mv, centre, r, rgb, 1, power);
        layer(f, mv, centre, r, rgb, 2, power);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** It swells up from a spark as it starts and sinks back as it stops. */
    static float size(float power) {
        float p = Math.min(1F, power);
        return 0.15F + 0.85F * p * p * (3 - 2 * p);
    }

    private static void layer(FxFrame f, Matrix4f mv, Vector3f c, float r, int rgb, int k, float alpha) {
        float[] L = LAYERS[k];
        Matrix4f m = new Matrix4f(mv).translate(c)
                .rotate((float) Math.toRadians(L[5] + L[6] * f.time), new Vector3f(L[2], L[3], L[4]).normalize())
                .scale(r * L[1]);
        ShaderInstance sh = QHShaders.STAR != null ? QHShaders.STAR : GameRenderer.getPositionColorShader();
        if (sh == null) return;
        if (sh == QHShaders.STAR) {
            sh.safeGetUniform("FxTime").set(f.time);
            sh.safeGetUniform("Layer").set((float) k);
        }
        RenderSystem.setShaderColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, L[0] * alpha);
        VertexBuffer vb = FxMesh.sphere();
        vb.bind();
        vb.drawWithShader(m, f.proj, sh);
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private StarEffect() {}
}
