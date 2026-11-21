package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 1.2.0: a beam along machine space's back axis, from z0 to z1 blocks behind the controller: an eight-sided prism whose
 * light scrolls along it (the game's own beacon beam texture, so nothing is shipped). The Forge draws two: a soft one in
 * its tint and a thin white core inside it.
 */
public final class BeamEffect {
    private static final ResourceLocation BEAM = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public static void draw(PoseStack ps, Matrix4f local, MultiBufferSource buf, float up, float z0, float z1, float radius, int rgb, float alpha,
                            float scroll, boolean soft) {
        if (alpha <= 0.003F || radius <= 0) return;
        VertexConsumer vc = buf.getBuffer(RenderType.beaconBeam(BEAM, soft));
        ps.pushPose();
        ps.last().pose().set(local);
        Matrix4f m = ps.last().pose();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, a = (int) (Math.min(1F, alpha) * 255);
        float len = z1 - z0, v0 = -scroll, v1 = v0 + len / Math.max(0.5F, radius * 4);
        int n = 8;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            float x0 = (float) Math.cos(a0) * radius, y0 = (float) Math.sin(a0) * radius + up;
            float x1 = (float) Math.cos(a1) * radius, y1 = (float) Math.sin(a1) * radius + up;
            float u0 = i / (float) n, u1 = (i + 1) / (float) n;
            vert(vc, m, x0, y0, z0, r, g, b, a, u0, v0);
            vert(vc, m, x1, y1, z0, r, g, b, a, u1, v0);
            vert(vc, m, x1, y1, z1, r, g, b, a, u1, v1);
            vert(vc, m, x0, y0, z1, r, g, b, a, u0, v1);
        }
        ps.popPose();
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, int a, float u, float v) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    private BeamEffect() {}
}
