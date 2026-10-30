package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.entity.BraneStrider;
import com.theoreticalsciences.entity.GravitonWisp;
import com.theoreticalsciences.entity.ModuliCrawler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;

/** Procedural models for the creatures native to the Bulk. */
public final class BulkCreatureRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;

    /** Vanilla-style death: topple sideways over a second. */
    static void deathTilt(LivingEntity e, float partial, PoseStack ps) {
        if (e.deathTime > 0) {
            float f = Math.min(1F, Mth.sqrt((e.deathTime + partial - 1F) / 20F * 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(f * 90F));
        }
    }

    /** Graviton Wisp: a bright core inside three rings turning on different axes; it collapses to a point when it dies. */
    public static class Wisp extends EntityRenderer<GravitonWisp> {
        public Wisp(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.2F;
        }

        @Override
        public void render(GravitonWisp e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            float k = e.deathTime > 0 ? 1F - Math.min(1F, (e.deathTime + partial) / 20F) : 1F;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            ps.pushPose();
            ps.translate(0, 0.4F + Mth.sin(t * 0.1F) * 0.08F, 0);
            ps.scale(k, k, k);
            float pulse = 0.12F + 0.03F * Mth.sin(t * 0.4F);
            ProcMesh.box(ps, glow, -pulse, -pulse, -pulse, pulse, pulse, pulse, 0xFFC0D0FF, 0xF000F0, OverlayTexture.NO_OVERLAY);
            for (int i = 0; i < 3; i++) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(t * (3 + i * 2) + i * 60));
                ps.mulPose(Axis.XP.rotationDegrees(55 * i + t * (1.5F - i)));
                ProcMesh.ring(ps, solid, 0.32F + i * 0.06F, 0.03F, 0.03F, 18, 0xFF6070C0, light, ov);
                ProcMesh.ring(ps, glow, 0.32F + i * 0.06F, 0.012F, 0.036F, 18, 0xFF8090FF, 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            // infalling motes
            Random r = new Random(e.getId());
            for (int i = 0; i < 10; i++) {
                float ph = (t * 0.02F + r.nextFloat()) % 1F, rad = 0.7F * (1 - ph), a = r.nextFloat() * 6.28F + t * 0.05F;
                float s = 0.025F;
                ps.pushPose();
                ps.translate(Mth.cos(a) * rad, (r.nextFloat() - 0.5F) * rad, Mth.sin(a) * rad);
                ProcMesh.box(ps, glow, -s, -s, -s, s, s, s, 0xFFA0B0FF, 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(GravitonWisp entity) {
            return WHITE;
        }
    }

    /**
     * Brane Strider: a narrow diamond-shaped body of stretched membrane panels on four long jointed stilts, with a
     * ring eye. A faint copy of it walks half a block "sideways" - its image in the neighbouring brane.
     */
    public static class Strider extends EntityRenderer<BraneStrider> {
        private static final int FRAME = 0xFF3A2050, MEMBRANE = 0xFFC080FF;

        public Strider(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.6F;
        }

        @Override
        public void render(BraneStrider e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0 || e.deathTime > 0);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            deathTilt(e, partial, ps);
            draw(e, t, partial, ps, SafeBuffer.of(buf, RenderType.entitySolid(WHITE)), SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE)),
                    SafeBuffer.of(buf, RenderType.eyes(WHITE)), light, ov, 1F);
            // its image on the neighbouring brane
            ps.pushPose();
            ps.translate(0.35F + Mth.sin(t * 0.05F) * 0.15F, 0, 0.1F);
            VertexConsumer ghost = SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE));
            draw(e, t + 7, partial, ps, ghost, ghost, ghost, light, ov, 0.18F);
            ps.popPose();
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        private void draw(BraneStrider e, float t, float partial, PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow,
                          int light, int ov, float alpha) {
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            float hipY = 2.0F;
            // four stilt legs that actually stand on the ground: thigh up and out to a high knee, long shin down to a
            // foot pad on the floor; diagonal pairs step together, lifting the foot as it swings forward
            for (int i = 0; i < 4; i++) {
                float sx = (i % 2 == 0 ? 1 : -1), sz = (i < 2 ? 1 : -1);
                float phase = (i == 0 || i == 3) ? 0 : Mth.PI;
                float swing = Mth.cos(limb * 0.6F + phase) * 0.35F * amt;
                float lift = Math.max(0F, Mth.sin(limb * 0.6F + phase)) * 0.35F * amt;
                float hx = sx * 0.18F, hz = sz * 0.14F;
                float fx = sx * 1.05F, fy = lift, fz = sz * 0.95F + swing;
                float kx = hx + sx * 0.5F, ky = hipY + 0.55F + lift * 0.5F, kz = hz + sz * 0.45F + swing * 0.5F;
                limb(ps, solid, hx, hipY, hz, kx, ky, kz, 0.04F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                limb(ps, solid, kx, ky, kz, fx, fy, fz, 0.028F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ps.pushPose();
                ps.translate(kx, ky, kz);
                ProcMesh.box(ps, glow, -0.055F, -0.055F, -0.055F, 0.055F, 0.055F, 0.055F, ProcMesh.withAlpha(MEMBRANE, alpha), 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
                ps.pushPose();
                ps.translate(fx, fy, fz);
                ProcMesh.box(ps, solid, -0.07F, 0, -0.07F, 0.07F, 0.04F, 0.07F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ProcMesh.box(ps, glow, -0.03F, 0.04F, -0.03F, 0.03F, 0.07F, 0.03F, ProcMesh.withAlpha(MEMBRANE, alpha), 0xF000F0, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            // body: two crossed diamond membranes in a frame
            ps.pushPose();
            ps.translate(0, hipY + 0.35F + Mth.sin(t * 0.12F) * 0.04F, 0);
            for (int k = 0; k < 2; k++) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(k * 90F + Mth.sin(t * 0.04F) * 10F));
                ps.mulPose(Axis.ZP.rotationDegrees(45F));
                ProcMesh.box(ps, trans, -0.32F, -0.32F, -0.01F, 0.32F, 0.32F, 0.01F, ProcMesh.withAlpha(MEMBRANE, 0.45F * alpha), light, ov);
                ProcMesh.box(ps, solid, -0.34F, -0.34F, -0.02F, -0.3F, 0.34F, 0.02F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ProcMesh.box(ps, solid, 0.3F, -0.34F, -0.02F, 0.34F, 0.34F, 0.02F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ProcMesh.box(ps, solid, -0.34F, -0.34F, -0.02F, 0.34F, -0.3F, 0.02F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ProcMesh.box(ps, solid, -0.34F, 0.3F, -0.02F, 0.34F, 0.34F, 0.02F, ProcMesh.withAlpha(FRAME, alpha), light, ov);
                ps.popPose();
            }
            // ring eye on top, facing forward
            ps.translate(0, 0.62F, -0.05F);
            float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(-headYaw));
            ProcMesh.ring(ps, solid, 0.14F, 0.04F, 0.04F, 14, ProcMesh.withAlpha(FRAME, alpha), light, ov);
            float p = 0.05F + 0.015F * Mth.sin(t * 0.3F);
            ProcMesh.box(ps, glow, -p, -p, -0.02F, p, p, 0.02F, ProcMesh.withAlpha(0xF0E0FF, alpha), 0xF000F0, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        /** A rod from (ax,ay,az) to (bx,by,bz). */
        public static void limb(PoseStack ps, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float w, int argb, int light, int ov) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4F) return;
            ps.pushPose();
            ps.translate(ax, ay, az);
            ps.mulPose(Axis.YP.rotation((float) Math.atan2(dx, dz)));
            ps.mulPose(Axis.XP.rotation((float) Math.acos(Mth.clamp(dy / len, -1F, 1F))));
            ProcMesh.box(ps, vc, -w, 0, -w, w, len, w, argb, light, ov);
            ps.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(BraneStrider entity) {
            return WHITE;
        }
    }

    /** Moduli Crawler: a knot of cubes that never stops reshaping, scuttling on six short legs. */
    public static class Crawler extends EntityRenderer<ModuliCrawler> {
        public Crawler(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.35F;
        }

        @Override
        public void render(ModuliCrawler e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0 || e.deathTime > 0);
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            deathTilt(e, partial, ps);
            // six legs
            for (int i = 0; i < 6; i++) {
                float side = i < 3 ? 1 : -1, z = (i % 3 - 1) * 0.18F;
                float swing = Mth.cos(limb * 1.2F + i * 2.1F) * 0.6F * amt;
                ps.pushPose();
                ps.translate(side * 0.16F, 0.22F, z);
                ps.mulPose(Axis.YP.rotation(swing * 0.5F));
                ps.mulPose(Axis.ZP.rotationDegrees(side * (60F + swing * 20F)));
                ProcMesh.box(ps, solid, -0.02F, -0.3F, -0.02F, 0.02F, 0, 0.02F, 0xFF204040, light, ov);
                ps.popPose();
            }
            // the reshaping knot of cubes
            ps.translate(0, 0.28F, 0);
            for (int i = 0; i < 7; i++) {
                float a = i * 0.9F + t * 0.06F;
                float rad = 0.12F + 0.06F * Mth.sin(t * 0.11F + i);
                float s = 0.07F + 0.03F * Mth.sin(t * 0.17F + i * 1.7F);
                ps.pushPose();
                ps.translate(Mth.cos(a) * rad, Mth.sin(a * 1.3F) * rad * 0.6F, Mth.sin(a) * rad);
                ps.mulPose(Axis.XP.rotationDegrees(t * (2 + i)));
                ps.mulPose(Axis.YP.rotationDegrees(t * (3 - i * 0.4F)));
                int col = i % 3 == 0 ? 0xFF60FFD0 : 0xFF2A5050;
                ProcMesh.box(ps, i % 3 == 0 ? glow : solid, -s, -s, -s, s, s, s, col, i % 3 == 0 ? 0xF000F0 : light, i % 3 == 0 ? OverlayTexture.NO_OVERLAY : ov);
                ps.popPose();
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(ModuliCrawler entity) {
            return WHITE;
        }
    }

    private BulkCreatureRenderers() {}
}
