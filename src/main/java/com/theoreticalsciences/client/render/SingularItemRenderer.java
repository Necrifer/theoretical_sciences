package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.SingularItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * 3D animated item renders: micro singularity (shadow + accretion ring), white hole (blazing core + rays),
 * tesseract core (live 4D projection), pocket universe (bubble with spiral galaxy), stellar core (star with corona).
 */
public class SingularItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE = TheoreticalSciences.id("textures/misc/white.png");

    public SingularItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof SingularItem item)) return;
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level != null ? mc.level.getGameTime() : 0) / 20F + mc.getTimer().getGameTimeDeltaPartialTick(false) / 20F;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        switch (item.kind) {
            case "singularity" -> singularity(pose, buffers, t);
            case "whitehole" -> whiteHole(pose, buffers, t);
            case "tesseract" -> tesseract(pose, buffers, t);
            case "universe" -> universe(pose, buffers, t);
            case "star" -> star(pose, buffers, t);
            default -> {}
        }
        pose.popPose();
    }

    // -------------------------------------------------------------- pieces

    private static void singularity(PoseStack pose, MultiBufferSource buf, float t) {
        VertexConsumer glow = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = pose.last().pose();
        disc(glow, m, 0.42F, 0xFF9A30, 0.35F, 0F);
        // accretion ring (tilted, spinning)
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(70));
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(t * 2.5F));
        Matrix4f rm = pose.last().pose();
        ring(glow, rm, 0.2F, 0.45F, 0xFFD080, 0.9F, 0xFF5010, 0.0F, t);
        pose.popPose();
        // shadow
        VertexConsumer solid = SafeBuffer.of(buf, RenderType.entityCutoutNoCull(WHITE));
        sphere(solid, pose, 0.17F, 0, 0, 0);
        VertexConsumer glow2 = SafeBuffer.of(buf, RenderType.lightning());
        ringBillboard(glow2, pose.last().pose(), 0.17F, 0.21F, 0xFFE0B0, 0.9F);
    }

    private static void whiteHole(PoseStack pose, MultiBufferSource buf, float t) {
        VertexConsumer g = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < 12; i++) {
            float a = i * (float) Math.PI * 2 / 12 + t * 0.6F;
            float len = 0.35F + 0.12F * (float) Math.sin(t * 5 + i * 1.7F);
            ray(g, m, a, 0.06F, len, 0xFFF2D0, 0.8F);
        }
        disc(g, m, 0.45F, 0xFFE9B0, 0.5F, 0F);
        disc(g, m, 0.22F, 0xFFFFFF, 1.0F, 0F);
        disc(g, m, 0.12F, 0xFFFFFF, 1.0F, 0F);
    }

    private static void tesseract(PoseStack pose, MultiBufferSource buf, float t) {
        VertexConsumer g = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float[][] v = Tesseract4D.project(t, 2.6F);
        float s = 0.42F;
        for (int[] e : Tesseract4D.EDGES) {
            float[] a = v[e[0]], b = v[e[1]];
            line3(g, m, a[0] * s, a[1] * s, a[2] * s, b[0] * s, b[1] * s, b[2] * s, 0.018F, wColor(a[3]), wColor(b[3]));
        }
        disc(g, m, 0.25F, 0x60D0FF, 0.25F, 0F);
    }

    private static void universe(PoseStack pose, MultiBufferSource buf, float t) {
        VertexConsumer g = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = pose.last().pose();
        disc(g, m, 0.46F, 0x302060, 0.5F, 0F);
        ringBillboard(g, m, 0.42F, 0.46F, 0xFFD080, 0.6F);
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(60));
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(t * 0.8F));
        Matrix4f gm = pose.last().pose();
        // two logarithmic spiral arms made of tiny stars
        for (int arm = 0; arm < 2; arm++)
            for (int i = 0; i < 40; i++) {
                float r = 0.03F + i * 0.009F;
                float a = (float) (arm * Math.PI + Math.log(r / 0.03F) * 2.2F);
                float x = (float) Math.cos(a) * r, y = (float) Math.sin(a) * r;
                int col = i < 12 ? 0xFFE0A0 : 0x90B0FF;
                star(g, gm, x, y, 0.02F, col, 1.0F - i / 50F);
            }
        disc(g, gm, 0.08F, 0xFFE8B0, 1.0F, 0F);
        pose.popPose();
    }

    private static void star(PoseStack pose, MultiBufferSource buf, float t) {
        VertexConsumer g = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < 8; i++) {
            float a = i * (float) Math.PI / 4 + (float) Math.sin(t + i) * 0.2F;
            ray(g, m, a, 0.1F, 0.3F + 0.08F * (float) Math.sin(t * 3 + i), 0xFFB040, 0.5F);
        }
        disc(g, m, 0.44F, 0xFF8000, 0.45F, 0F);
        disc(g, m, 0.28F, 0xFFD060, 0.9F, 0.1F);
        disc(g, m, 0.16F, 0xFFFBE0, 1.0F, 0.6F);
    }

    // -------------------------------------------------------------- geometry helpers (in the item's XY plane)

    private static void vtx(VertexConsumer c, Matrix4f m, float x, float y, float z, int rgb, float a) {
        c.addVertex(m, x, y, z).setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, a);
    }

    /** Radial gradient disc (lightning render type uses quads). */
    private static void disc(VertexConsumer c, Matrix4f m, float r, int rgb, float a, float edgeAlpha) {
        int seg = 20;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            vtx(c, m, 0, 0, 0, rgb, a);
            vtx(c, m, (float) Math.cos(a0) * r, (float) Math.sin(a0) * r, 0, rgb, edgeAlpha);
            vtx(c, m, (float) Math.cos(a1) * r, (float) Math.sin(a1) * r, 0, rgb, edgeAlpha);
            vtx(c, m, 0, 0, 0, rgb, a);
        }
    }

    private static void ringBillboard(VertexConsumer c, Matrix4f m, float r0, float r1, int rgb, float a) {
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            vtx(c, m, (float) Math.cos(a0) * r0, (float) Math.sin(a0) * r0, 0.001F, rgb, a);
            vtx(c, m, (float) Math.cos(a0) * r1, (float) Math.sin(a0) * r1, 0.001F, rgb, 0);
            vtx(c, m, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, 0.001F, rgb, 0);
            vtx(c, m, (float) Math.cos(a1) * r0, (float) Math.sin(a1) * r0, 0.001F, rgb, a);
        }
    }

    private static void ring(VertexConsumer c, Matrix4f m, float r0, float r1, int inner, float ai, int outer, float ao, float t) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            float flick = 0.7F + 0.3F * (float) Math.sin(i * 1.9 + t * 6);
            vtx(c, m, (float) Math.cos(a0) * r0, (float) Math.sin(a0) * r0, 0, inner, ai * flick);
            vtx(c, m, (float) Math.cos(a0) * r1, (float) Math.sin(a0) * r1, 0, outer, ao);
            vtx(c, m, (float) Math.cos(a1) * r1, (float) Math.sin(a1) * r1, 0, outer, ao);
            vtx(c, m, (float) Math.cos(a1) * r0, (float) Math.sin(a1) * r0, 0, inner, ai * flick);
        }
    }

    private static void ray(VertexConsumer c, Matrix4f m, float ang, float width, float len, int rgb, float a) {
        float cx = (float) Math.cos(ang), cy = (float) Math.sin(ang);
        float px = -cy * width / 2, py = cx * width / 2;
        vtx(c, m, px, py, 0, rgb, a);
        vtx(c, m, -px, -py, 0, rgb, a);
        vtx(c, m, cx * len, cy * len, 0, rgb, 0);
        vtx(c, m, cx * len, cy * len, 0, rgb, 0);
    }

    private static void star(VertexConsumer c, Matrix4f m, float x, float y, float s, int rgb, float a) {
        vtx(c, m, x - s, y, 0, rgb, 0);
        vtx(c, m, x, y - s, 0, rgb, a);
        vtx(c, m, x + s, y, 0, rgb, 0);
        vtx(c, m, x, y + s, 0, rgb, a);
    }

    private static void line3(VertexConsumer c, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float w, int ca, int cb) {
        // two crossed quads so the edge reads from any angle
        vtx(c, m, x0 - w, y0, z0, ca, 1);
        vtx(c, m, x0 + w, y0, z0, ca, 1);
        vtx(c, m, x1 + w, y1, z1, cb, 1);
        vtx(c, m, x1 - w, y1, z1, cb, 1);
        vtx(c, m, x0, y0 - w, z0, ca, 1);
        vtx(c, m, x0, y0 + w, z0, ca, 1);
        vtx(c, m, x1, y1 + w, z1, cb, 1);
        vtx(c, m, x1, y1 - w, z1, cb, 1);
    }

    private static int wColor(float w) {
        float t = Math.max(0, Math.min(1, (w + 1.2F) / 2.4F));
        return ((int) (64 + 191 * t) << 16) | ((int) (224 - 160 * t) << 8) | 255;
    }

    /** Black sphere for event horizons. */
    private static void sphere(VertexConsumer c, PoseStack pose, float r, float ox, float oy, float oz) {
        PoseStack.Pose p = pose.last();
        int lat = 10, lon = 16;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI * i / lat, t1 = Math.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = Math.PI * 2 * j / lon, p1 = Math.PI * 2 * (j + 1) / lon;
                sv(c, p, r, t0, p0);
                sv(c, p, r, t1, p0);
                sv(c, p, r, t1, p1);
                sv(c, p, r, t0, p1);
            }
        }
    }

    private static void sv(VertexConsumer c, PoseStack.Pose p, float r, double th, double ph) {
        float x = (float) (Math.sin(th) * Math.cos(ph)), y = (float) Math.cos(th), z = (float) (Math.sin(th) * Math.sin(ph));
        c.addVertex(p, x * r, y * r, z * r).setColor(0, 0, 0, 255).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, x, y, z);
    }
}
