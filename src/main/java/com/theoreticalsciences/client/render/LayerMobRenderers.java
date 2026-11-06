package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.math.LayerMobs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import static com.theoreticalsciences.client.render.MathMobRenderers.cbox;

/** 1.2.0: procedural models for the creatures of the Platonic Realm's new layers. */
public final class LayerMobRenderers {
    private static final int FULL = 0xF000F0, NO = OverlayTexture.NO_OVERLAY;
    static final int[] PALE = {0xFFF6F4EE, 0xFFC8C4BC}, DARK = {0xFF2A2A30, 0xFF0C0C10}, TILE = {0xFF5A90C0, 0xFF203858},
            TILE_EDGE = {0xFFA0E0FF, 0xFF5090C0}, GLOW = {0xFFB0FFE0, 0xFF60E0A0};

    public static class Mob<T extends LivingEntity> extends EntityRenderer<T> {
        public Mob(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.4F;
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(QHEntityRenderers.WHITE));
            VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucent(QHEntityRenderers.WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(QHEntityRenderers.WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (e) {
                case LayerMobs.ParadoxShade s -> shade(ps, solid, trans, glow, t, limb, amt, s.isTrue(), s.flipping(partial), light, ov);
                case LayerMobs.Tessellate s -> tessellate(ps, solid, glow, t, limb, amt, s.scale(), light, ov);
                case LayerMobs.FourierWisp w -> wisp(ps, trans, glow, t, light);
                default -> {}
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return QHEntityRenderers.WHITE;
        }
    }

    /**
     * The Paradox Shade: a tall hooded figure, one side pale and one dark. True, it is solid, pale side forward, the dark
     * half a thin seam down its back; false, it is a dark see-through outline with a pale glowing edge. Each time it
     * changes it turns about, as if the statement had been turned over.
     */
    static void shade(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, float limb, float amt,
                      boolean isTrue, float flip, int light, int ov) {
        ps.translate(0, 0.05F * Mth.sin(t * 0.08F), 0);
        ps.mulPose(Axis.YP.rotation(Mth.PI * (1 - MathMobRenderers.smooth(flip))));          // turning over
        float sway = 0.25F * amt * Mth.sin(limb * 0.7F);
        VertexConsumer body = isTrue ? solid : trans;
        int[] front = isTrue ? PALE : new int[]{0x60202028, 0x40101014};
        int[] back = isTrue ? DARK : new int[]{0x50101014, 0x30080808};
        // the robe: a stack of slabs widening to the hem, pale in front and dark behind
        for (int i = 0; i < 6; i++) {
            float y0 = 0.1F + i * 0.22F, w = 0.32F - i * 0.025F, d = 0.2F - i * 0.012F;
            float sw = sway * (1 - i / 6F) * 0.3F;
            cbox(ps, body, sw, y0 + 0.11F, -d * 0.5F, w, 0.11F, d * 0.5F, front, light, ov);
            cbox(ps, body, sw, y0 + 0.11F, d * 0.5F, w, 0.11F, d * 0.5F, back, light, ov);
        }
        // the hood, and its face: a slit of light, pale on the dark side or dark on the pale
        cbox(ps, body, 0, 1.62F, -0.06F, 0.17F, 0.18F, 0.1F, front, light, ov);
        cbox(ps, body, 0, 1.62F, 0.1F, 0.17F, 0.18F, 0.07F, back, light, ov);
        cbox(ps, glow, 0, 1.6F, -0.163F, 0.09F, 0.02F, 0.003F, isTrue ? new int[]{0xFF202020, 0xFF202020} : new int[]{0xFFF0F0FF, 0xFFF0F0FF}, FULL, NO);
        // arms, held out a little, swinging
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.36F, 1.35F, 0);
            ps.mulPose(Axis.XP.rotation(side * sway + 0.2F));
            ps.mulPose(Axis.ZP.rotation(side * 0.12F));
            MathMobRenderers.sbox(ps, body, -0.06F, -0.6F, -0.06F, 0.06F, 0, 0.06F, side < 0 ? front : back, light, ov);
            ps.popPose();
        }
        if (!isTrue) {                                                                         // false: only an edge of light
            for (int i = 0; i <= 12; i++) {
                float y = 0.1F + i * 0.13F, w = 0.33F - Math.min(i, 9) * 0.02F;
                cbox(ps, glow, -w, y, 0, 0.008F, 0.06F, 0.008F, new int[]{0xFFE0E0F0, 0xFFE0E0F0}, FULL, NO);
                cbox(ps, glow, w, y, 0, 0.008F, 0.06F, 0.008F, new int[]{0xFFE0E0F0, 0xFFE0E0F0}, FULL, NO);
            }
        }
    }

    /**
     * The Tessellate: tiles of the hyperbolic plane, a heptagon at its heart and seven smaller heptagons round it,
     * turning slowly; it rolls toward you. Each tile is one heptagonal prism (35 quads in all for a tile), so a crowd of
     * them stays cheap to draw.
     */
    static void tessellate(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, float scale, int light, int ov) {
        ps.translate(0, 0.55F * scale, 0);
        ps.scale(scale, scale, scale);
        ps.mulPose(Axis.XP.rotation(limb * 0.6F));
        ps.mulPose(Axis.YP.rotation(t * 0.02F));
        heptagon(ps, solid, glow, 0.34F, 0.08F, light, ov);
        for (int k = 0; k < 7; k++) {
            float a = k * Mth.TWO_PI / 7;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(a));
            ps.translate(0.5F, 0, 0);
            ps.mulPose(Axis.ZP.rotation(0.9F + 0.08F * Mth.sin(t * 0.1F + k)));
            heptagon(ps, solid, glow, 0.22F, 0.06F, light, ov);
            ps.popPose();
        }
    }

    /** A heptagonal tile of radius r and half-thickness h: a lit top and a darker underside, and a glowing rim. */
    static void heptagon(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float r, float h, int light, int ov) {
        PoseStack.Pose p = ps.last();
        for (int k = 0; k < 7; k++) {
            float a0 = k * Mth.TWO_PI / 7, a1 = (k + 1) * Mth.TWO_PI / 7;
            float x0 = Mth.cos(a0) * r, z0 = Mth.sin(a0) * r, x1 = Mth.cos(a1) * r, z1 = Mth.sin(a1) * r;
            // a wedge of the top and of the bottom (a quad with two corners at the centre)
            q(p, solid, 0, h, 0, x1, h, z1, x0, h, z0, 0, h, 0, 0, 1, 0, TILE[0], 1F, light, ov);
            q(p, solid, 0, -h, 0, x0, -h, z0, x1, -h, z1, 0, -h, 0, 0, -1, 0, TILE[1], 0.6F, light, ov);
            // the rim
            float nx = Mth.cos((a0 + a1) / 2), nz = Mth.sin((a0 + a1) / 2);
            q(p, glow, x0, h, z0, x1, h, z1, x1, -h, z1, x0, -h, z0, nx, 0, nz, TILE_EDGE[0], 1F, FULL, NO);
        }
    }

    private static void q(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                          float dx, float dy, float dz, float nx, float ny, float nz, int argb, float shade, int light, int ov) {
        float rr = ((argb >> 16) & 255) / 255F * shade, gg = ((argb >> 8) & 255) / 255F * shade, bb = (argb & 255) / 255F * shade, aa = ((argb >>> 24) & 255) / 255F;
        vc.addVertex(p, ax, ay, az).setColor(rr, gg, bb, aa).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, bx, by, bz).setColor(rr, gg, bb, aa).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, cx, cy, cz).setColor(rr, gg, bb, aa).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(p, dx, dy, dz).setColor(rr, gg, bb, aa).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
    }

    /**
     * The Fourier Wisp: a glowing core and a chain of three circles, each turning on the rim of the one before at one,
     * three and five times its speed, a third and a fifth its size; the last one's point traces a square wave.
     */
    static void wisp(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, int light) {
        ps.translate(0, 0.7F + 0.08F * Mth.sin(t * 0.07F), 0);
        ps.scale(1.9F, 1.9F, 1.9F);                                                            // 1.2.0: big enough to see and hit
        cbox(ps, glow, 0, 0, 0, 0.08F, 0.08F, 0.08F, GLOW, FULL, NO);
        cbox(ps, trans, 0, 0, 0, 0.16F, 0.16F, 0.16F, new int[]{0x4060FFC0, 0x2040C090}, FULL, NO);
        float base = t * 0.08F;
        float cx = 0, cy = 0, r = 0.35F;
        for (int k = 0; k < 3; k++) {
            int m = 2 * k + 1;
            float rr = r / m, a = m * base;
            for (int i = 0; i < 24; i++) {                                                     // the circle
                float u = i * Mth.TWO_PI / 24;
                cbox(ps, glow, cx + Mth.cos(u) * rr, cy + Mth.sin(u) * rr, 0, 0.008F, 0.008F, 0.008F, new int[]{0xA080FFD0, 0xA080FFD0}, FULL, NO);
            }
            float nx = cx + Mth.cos(a) * rr, ny = cy + Mth.sin(a) * rr;
            TitanMobRenderers.seg(ps, glow, cx, cy, 0, nx, ny, 0, 0.006F, 0xFFC0FFE8, FULL, NO);  // its radius
            cx = nx;
            cy = ny;
        }
        cbox(ps, glow, cx, cy, 0, 0.03F, 0.03F, 0.03F, new int[]{0xFFFFFFFF, 0xFFFFFFFF}, FULL, NO);  // the drawing point
    }

    private LayerMobRenderers() {}
}
