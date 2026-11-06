package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.theoreticalsciences.client.render.QHShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * 1.2.0: a sphere of open space seen from inside: dark, starred, with a faint nebula of the tint colour
 * ({@code ts_starfield}). Only its inner face is drawn, so from inside it surrounds you and from outside you see its far
 * half behind whatever is in it (draw it first). Without the shader it is a plain dark sphere.
 */
public final class SpaceShellEffect {
    public static void draw(FxFrame f, Matrix4f mv, Vector3f centre, float radius, int rgb, float power) {
        if (power <= 0.001F || radius <= 0) return;
        ShaderInstance sh = QHShaders.STARFIELD != null ? QHShaders.STARFIELD : GameRenderer.getPositionColorShader();
        if (sh == null) return;
        float p = Math.min(1F, power), r = radius * (0.05F + 0.95F * p * p * (3 - 2 * p));
        Matrix4f m = new Matrix4f(mv).translate(centre).rotateY(f.time * 0.01F).scale(r);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        GL11.glCullFace(GL11.GL_FRONT);
        if (sh == QHShaders.STARFIELD) {
            sh.safeGetUniform("FxTime").set(f.time);
            RenderSystem.setShaderColor((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, p);
        } else RenderSystem.setShaderColor(0.02F, 0.02F, 0.05F, 1);
        VertexBuffer vb = FxMesh.sphere();
        vb.bind();
        vb.drawWithShader(m, f.proj, sh);
        VertexBuffer.unbind();
        GL11.glCullFace(GL11.GL_BACK);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private SpaceShellEffect() {}
}
