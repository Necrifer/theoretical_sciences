package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.deeptime.RevivedMobs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.3.0: the revived creatures, modelled in blocks like the game's own animals: shaded boxes, a lighter top and a darker
 * underside, and jointed parts that move (legs, trunk, wings).
 */
public final class DeepTimeRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;

    public static class Mob<T extends LivingEntity> extends EntityRenderer<T> {
        public Mob(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.5F;
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            // the models face +z, so the body's yaw is applied as is (180 minus it turned them around: 1.5.2 fix)
            ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (id) {
                case "mammoth" -> mammoth(ps, solid, t, limb, amt, e instanceof RevivedMobs.Mammoth m ? m.grazeAnim : 0, partial, light, ov);
                case "meganeura" -> meganeura(ps, solid, trans, t, e.getId(), light, ov);
                default -> {}
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return WHITE;
        }
    }

    static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, x0, y0, z0, x1, y1, z1, argb, light, ov);
    }

    /** A shaded slab of fur: the lower part a shade darker, as if lit from above. */
    static void fur(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int top, int bot, int light, int ov) {
        float ym = y0 + (y1 - y0) * 0.35F;
        box(ps, vc, x0, y0, z0, x1, ym, z1, bot, light, ov);
        box(ps, vc, x0, ym, z0, x1, y1, z1, top, light, ov);
    }

    // ------------------------------------------------------------------ Woolly Mammoth
    static final int FUR = 0xFF6A4528, FUR_DARK = 0xFF4A2E18, FUR_DEEP = 0xFF33200F, FUR_LIGHT = 0xFF85583A, FUR_TIP = 0xFFA07048,
            FUR_RED = 0xFF7A4A22, HIDE = 0xFF3A2A20, HIDE_DARK = 0xFF241810, NAIL = 0xFFB8A888, IVORY = 0xFFEADFC2,
            IVORY_MID = 0xFFDCCFAE, IVORY_TIP = 0xFFC2B48E, EYE_DARK = 0xFF140C08, M_EYE_SHINE = 0xFF5A4030;

    private static final int[] SHAG = {FUR_DARK, FUR, FUR_RED, FUR_DEEP, FUR_LIGHT, FUR, FUR_DARK, FUR_RED};

    /**
     * A curtain of long hair hanging from a body's side: strands of different shades and lengths, the tips swinging a
     * little as it walks. x is the side plane, z0..z1 its length, the strands hang from yTop down to between yMin and yMax.
     */
    static void curtain(PoseStack ps, VertexConsumer vc, float x, float side, float z0, float z1, float yTop, float yMin, float yMax,
                        float t, float amt, int light, int ov) {
        int n = Math.max(3, Math.round((z1 - z0) / 0.16F));
        float dz = (z1 - z0) / n;
        for (int i = 0; i < n; i++) {
            float h = (float) ((Math.sin(i * 2.7) * 0.5 + 0.5) * (yMax - yMin)) + yMin;
            float swing = Mth.sin(t * 0.12F + i) * 0.02F * (0.3F + amt);
            float zz = z0 + i * dz;
            int c = SHAG[i % SHAG.length];
            box(ps, vc, x, h, zz, x + side * 0.09F, yTop, zz + dz * 1.05F, c, light, ov);
            box(ps, vc, x + side * 0.02F + swing, h - 0.09F, zz + dz * 0.2F, x + side * 0.08F + swing, h + 0.01F, zz + dz * 0.8F,
                    i % 3 == 0 ? FUR_TIP : FUR_DEEP, light, ov);                                                  // the strand's tip
        }
    }

    /**
     * The Woolly Mammoth: a high-shouldered body with a fatty hump, a shaggy skirt of long hair in many shades, a domed
     * head with a lighter forelock, small furred ears, a ringed swaying trunk and long spiralling tusks. Legs swing in a
     * slow walk, their hair hanging over the broad, nailed feet; grazing, the head and trunk go down. Faces +z.
     */
    static void mammoth(PoseStack ps, VertexConsumer vc, float t, float limb, float amt, int graze, float partial, int light, int ov) {
        // legs: pillars swinging from the hips and shoulders, opposite corners together
        float[][] legs = {{-0.6F, 0.85F, 0}, {0.6F, 0.85F, (float) Math.PI}, {-0.6F, -0.9F, (float) Math.PI}, {0.6F, -0.9F, 0}};
        for (float[] l : legs) {
            ps.pushPose();
            ps.translate(l[0], 1.35F, l[1]);
            ps.mulPose(Axis.XP.rotation(Mth.cos(limb * 0.5F + l[2]) * 0.45F * amt));
            fur(ps, vc, -0.27F, -1.15F, -0.27F, 0.27F, 0.05F, 0.27F, FUR, FUR_DARK, light, ov);
            fur(ps, vc, -0.31F, -0.75F, -0.31F, 0.31F, -0.05F, 0.31F, FUR_RED, FUR_DEEP, light, ov);      // the shaggy thigh
            for (int k = 0; k < 4; k++) {                                                                 // hair over the foot
                float a = k * (float) Math.PI / 2;
                float cx = Mth.sin(a) * 0.26F, cz = Mth.cos(a) * 0.26F;
                box(ps, vc, cx - 0.1F, -1.08F, cz - 0.1F, cx + 0.1F, -0.7F, cz + 0.1F, k % 2 == 0 ? FUR_DEEP : FUR_DARK, light, ov);
            }
            box(ps, vc, -0.3F, -1.35F, -0.3F, 0.3F, -1.12F, 0.3F, HIDE, light, ov);                     // the broad sole
            box(ps, vc, -0.31F, -1.35F, -0.31F, 0.31F, -1.3F, 0.31F, HIDE_DARK, light, ov);
            for (int k = -1; k <= 1; k++)                                                                 // toenails
                box(ps, vc, k * 0.17F - 0.06F, -1.33F, 0.27F, k * 0.17F + 0.06F, -1.2F, 0.32F, NAIL, light, ov);
            ps.popPose();
        }
        // body: a lighter back, a red brown middle and a deep belly, then the hump
        box(ps, vc, -1.0F, 1.2F, -1.35F, 1.0F, 1.7F, 1.3F, FUR_DEEP, light, ov);
        box(ps, vc, -1.02F, 1.7F, -1.35F, 1.02F, 2.35F, 1.3F, FUR_RED, light, ov);
        box(ps, vc, -0.98F, 2.35F, -1.32F, 0.98F, 2.78F, 1.28F, FUR, light, ov);
        box(ps, vc, -0.7F, 2.75F, -1.2F, 0.7F, 2.85F, 0.2F, FUR_LIGHT, light, ov);                     // the sloping back
        fur(ps, vc, -0.85F, 2.6F, 0.1F, 0.85F, 3.15F, 1.15F, FUR_LIGHT, FUR, light, ov);                // the shoulder hump
        box(ps, vc, -0.6F, 3.1F, 0.25F, 0.6F, 3.25F, 0.95F, FUR_TIP, light, ov);                       // sun-bleached tips
        box(ps, vc, -0.88F, 2.6F, 1.1F, 0.88F, 3.05F, 1.32F, FUR_DARK, light, ov);                      // the mane over the neck
        // the long skirt: strands down both flanks, the chest and the rump
        for (int side = -1; side <= 1; side += 2) {
            curtain(ps, vc, side * 1.0F, side, -1.3F, 1.25F, 2.2F, 0.75F, 1.05F, t, amt, light, ov);
            curtain(ps, vc, side * 1.08F, side, -0.9F, 1.0F, 2.65F, 1.6F, 2.0F, t + 3, amt, light, ov);  // a second, shorter layer
        }
        for (int k = -4; k <= 4; k++) {                                                                  // the chest ruff and the rump
            float x = k * 0.21F, hc = 0.85F + (k * k % 3) * 0.12F;
            box(ps, vc, x - 0.11F, hc, 1.25F, x + 0.11F, 2.4F, 1.4F, SHAG[(k + 8) % SHAG.length], light, ov);
            box(ps, vc, x - 0.11F, hc + 0.1F, -1.42F, x + 0.11F, 2.3F, -1.3F, SHAG[(k + 11) % SHAG.length], light, ov);
        }
        // the tail, short and tufted
        ps.pushPose();
        ps.translate(0, 2.3F, -1.4F);
        ps.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.08F) * 0.3F));
        box(ps, vc, -0.07F, -0.6F, -0.1F, 0.07F, 0, 0.02F, FUR_DARK, light, ov);
        box(ps, vc, -0.13F, -0.9F, -0.15F, 0.13F, -0.55F, 0.07F, FUR_DEEP, light, ov);
        box(ps, vc, -0.08F, -0.98F, -0.11F, 0.08F, -0.88F, 0.03F, FUR_TIP, light, ov);
        ps.popPose();
        // the head: lowered while grazing
        float g = graze > 0 ? Mth.clamp(Math.min(graze - partial, 60 - graze + partial) / 10F, 0, 1) : 0;
        ps.pushPose();
        ps.translate(0, 2.45F, 1.3F);
        ps.mulPose(Axis.XP.rotation(0.15F + g * 0.7F + Mth.sin(t * 0.03F) * 0.03F));
        fur(ps, vc, -0.62F, -0.7F, 0, 0.62F, 0.62F, 0.95F, FUR_RED, FUR_DARK, light, ov);
        box(ps, vc, -0.5F, -0.72F, 0.6F, 0.5F, -0.1F, 0.98F, HIDE, light, ov);                          // the bare, darker face
        fur(ps, vc, -0.45F, 0.55F, 0.1F, 0.45F, 0.95F, 0.75F, FUR_LIGHT, FUR, light, ov);                // the high dome of the skull
        box(ps, vc, -0.32F, 0.85F, 0.3F, 0.32F, 1.05F, 0.7F, FUR_TIP, light, ov);                       // the forelock
        box(ps, vc, -0.3F, 0.4F, 0.9F, 0.3F, 0.62F, 1.0F, FUR_DARK, light, ov);                         // the brow
        for (int side = -1; side <= 1; side += 2) {
            box(ps, vc, side * 0.6F, -0.2F, 0.15F, side * 0.8F, 0.32F, 0.58F, FUR_DARK, light, ov);       // small furred ears
            box(ps, vc, side * 0.64F, -0.12F, 0.2F, side * 0.74F, 0.24F, 0.5F, HIDE_DARK, light, ov);
            box(ps, vc, side * 0.36F, 0.05F, 0.95F, side * 0.5F, 0.18F, 1.0F, EYE_DARK, light, ov);         // eyes
            box(ps, vc, side * 0.38F, 0.13F, 0.99F, side * 0.42F, 0.17F, 1.01F, M_EYE_SHINE, light, ov);
            // tusks: a hide sheath, then out, down, and curling up and in
            ps.pushPose();
            ps.translate(side * 0.38F, -0.5F, 0.9F);
            box(ps, vc, -0.15F, -0.15F, -0.05F, 0.15F, 0.15F, 0.12F, HIDE, light, ov);
            for (int k = 0; k < 7; k++) {
                float w = 0.115F - k * 0.011F;
                box(ps, vc, -w, -w, 0, w, w, 0.3F, k < 3 ? IVORY : k < 5 ? IVORY_MID : IVORY_TIP, light, ov);
                box(ps, vc, -w - 0.005F, w - 0.03F, 0.02F, w + 0.005F, w + 0.005F, 0.06F, IVORY_MID, light, ov); // growth ring
                ps.translate(0, 0, 0.26F);
                ps.mulPose(Axis.XP.rotation(-0.3F));
                ps.mulPose(Axis.YP.rotation(-side * 0.17F));
            }
            ps.popPose();
        }
        box(ps, vc, -0.2F, -0.78F, 0.72F, 0.2F, -0.62F, 0.9F, 0xFF6A3A30, light, ov);                     // the lower lip
        // the trunk: ringed, hairy at the root, swaying, the tip curling with its two "fingers"
        ps.pushPose();
        ps.translate(0, -0.55F, 0.95F);
        float sway = Mth.sin(t * 0.05F) * 0.12F;
        for (int k = 0; k < 7; k++) {
            float w = 0.21F - k * 0.02F;
            box(ps, vc, -w, -0.28F, -w, w, 0.02F, w, k < 2 ? FUR_DARK : HIDE, light, ov);
            box(ps, vc, -w - 0.012F, -0.06F, -w - 0.012F, w + 0.012F, -0.02F, w + 0.012F, HIDE_DARK, light, ov); // a wrinkle
            if (k < 2) box(ps, vc, -w - 0.03F, -0.25F, w - 0.04F, w + 0.03F, 0, w + 0.03F, FUR_DEEP, light, ov);
            ps.translate(0, -0.26F, 0);
            ps.mulPose(Axis.ZP.rotation(sway * (1 + k * 0.3F)));
            ps.mulPose(Axis.XP.rotation(k >= 5 ? -0.55F : 0.05F - g * 0.08F));
        }
        box(ps, vc, -0.08F, -0.12F, 0.02F, 0.08F, 0.02F, 0.12F, HIDE_DARK, light, ov);
        box(ps, vc, -0.04F, -0.18F, 0.06F, 0.04F, -0.1F, 0.13F, HIDE, light, ov);                        // the upper finger
        ps.popPose();
        ps.popPose();
    }

    // ------------------------------------------------------------------ Meganeura
    static final int THORAX = 0xFF2E5A3A, THORAX_DARK = 0xFF1C3A26, THORAX_SHINE = 0xFF4E8A5A, STRIPE = 0xFFC8B040,
            SEG_A = 0xFF1F3F5F, SEG_B = 0xFF3A7A88, SEG_JOINT = 0xFF0E1A26, SEG_SPOT = 0xFF8AD0D8, EYE = 0xFF2A7A60,
            EYE_DEEP = 0xFF184A3A, EYE_SHINE = 0xFFA0F0D0, FACE = 0xFFB8A050, LEG = 0xFF101810, LEG_SPINE = 0xFF384030,
            WING = 0x50D8E8F0, WING_BASE = 0x70C8A060, VEIN = 0xA0405060, VEIN_LIGHT = 0x70707C88, STIGMA = 0xD0302020;

    /**
     * Meganeura: a big-eyed head with a yellow face and jaws, a metallic green thorax striped in gold, a long abdomen of
     * steel blue rings with pale spots, jointed spiny legs folded into a basket, and four long veined wings tinted amber at
     * the root, fore and hind pairs beating out of step as a dragonfly's do. Faces +z.
     */
    static void meganeura(PoseStack ps, VertexConsumer solid, VertexConsumer trans, float t, int seed, int light, int ov) {
        ps.translate(0, 0.2F + Mth.sin(t * 0.15F + seed) * 0.03F, 0);
        // head: the face, the jaws and the huge wraparound compound eyes
        box(ps, solid, -0.08F, -0.08F, 0.4F, 0.08F, 0.08F, 0.53F, THORAX_DARK, light, ov);
        box(ps, solid, -0.07F, -0.09F, 0.5F, 0.07F, 0.02F, 0.56F, FACE, light, ov);
        float jaw = 0.02F + Mth.sin(t * 0.4F + seed) * 0.01F;
        box(ps, solid, -0.06F - jaw, -0.12F, 0.52F, -0.01F - jaw, -0.08F, 0.58F, 0xFF3A2A10, light, ov);
        box(ps, solid, 0.01F + jaw, -0.12F, 0.52F, 0.06F + jaw, -0.08F, 0.58F, 0xFF3A2A10, light, ov);
        for (int side = -1; side <= 1; side += 2) {
            box(ps, solid, side * 0.06F, -0.05F, 0.4F, side * 0.18F, 0.13F, 0.55F, EYE, light, ov);
            box(ps, solid, side * 0.07F, -0.06F, 0.41F, side * 0.17F, 0.0F, 0.54F, EYE_DEEP, light, ov);
            box(ps, solid, side * 0.1F, 0.08F, 0.47F, side * 0.14F, 0.135F, 0.52F, EYE_SHINE, light, ov);
        }
        // thorax: metallic green, a shining ridge, gold stripes down the sides
        box(ps, solid, -0.13F, -0.12F, 0.05F, 0.13F, 0.0F, 0.41F, THORAX_DARK, light, ov);
        box(ps, solid, -0.13F, 0.0F, 0.05F, 0.13F, 0.13F, 0.41F, THORAX, light, ov);
        box(ps, solid, -0.04F, 0.13F, 0.08F, 0.04F, 0.15F, 0.38F, THORAX_SHINE, light, ov);
        for (int side = -1; side <= 1; side += 2) {
            box(ps, solid, side * 0.13F, -0.06F, 0.12F, side * 0.135F, 0.1F, 0.16F, STRIPE, light, ov);
            box(ps, solid, side * 0.13F, -0.06F, 0.26F, side * 0.135F, 0.1F, 0.3F, STRIPE, light, ov);
            box(ps, solid, side * 0.05F, 0.13F, 0.3F, side * 0.08F, 0.14F, 0.38F, STRIPE, light, ov);
        }
        // six legs, each a thigh and a spiny shin, folded forward into the basket it catches prey in
        for (int k = 0; k < 3; k++)
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.07F, -0.11F, 0.12F + k * 0.11F);
                ps.mulPose(Axis.ZP.rotation(side * 0.5F));
                box(ps, solid, -0.015F, -0.12F, -0.015F, 0.015F, 0, 0.015F, LEG, light, ov);
                ps.translate(0, -0.12F, 0);
                ps.mulPose(Axis.ZP.rotation(-side * 1.1F));
                ps.mulPose(Axis.XP.rotation(-0.5F));
                box(ps, solid, -0.012F, -0.11F, -0.012F, 0.012F, 0, 0.012F, LEG, light, ov);
                for (int sp = 0; sp < 3; sp++)
                    box(ps, solid, -0.005F, -0.03F - sp * 0.03F, 0.01F, 0.005F, -0.02F - sp * 0.03F, 0.03F, LEG_SPINE, light, ov);
                ps.popPose();
            }
        // abdomen: tapering rings, dark joints, a pale spot pair on each, drooping slightly
        ps.pushPose();
        ps.translate(0, 0, 0.06F);
        for (int k = 0; k < 9; k++) {
            float w = 0.075F - k * 0.004F;
            box(ps, solid, -w, -w, -0.12F, w, w, 0, k % 2 == 0 ? SEG_A : SEG_B, light, ov);
            box(ps, solid, -w - 0.005F, -w - 0.005F, -0.13F, w + 0.005F, w + 0.005F, -0.115F, SEG_JOINT, light, ov);
            box(ps, solid, -w * 0.6F, w, -0.09F, -w * 0.15F, w + 0.006F, -0.04F, SEG_SPOT, light, ov);
            box(ps, solid, w * 0.15F, w, -0.09F, w * 0.6F, w + 0.006F, -0.04F, SEG_SPOT, light, ov);
            box(ps, solid, -w * 0.9F, -w - 0.004F, -0.1F, w * 0.9F, -w, -0.02F, SEG_JOINT, light, ov);  // the dark underside
            ps.translate(0, 0, -0.12F);
            ps.mulPose(Axis.XP.rotation(0.025F + Mth.sin(t * 0.1F + k) * 0.01F));
        }
        box(ps, solid, -0.05F, -0.02F, -0.16F, -0.01F, 0.02F, 0, LEG, light, ov);                     // the cerci at the tip
        box(ps, solid, 0.01F, -0.02F, -0.16F, 0.05F, 0.02F, 0, LEG, light, ov);
        ps.popPose();
        // wings: fore pair and hind pair out of phase, tapering, amber at the root, ribbed with veins
        for (int pair = 0; pair < 2; pair++) {
            float z = pair == 0 ? 0.32F : 0.14F, flap = Mth.sin(t * 2.4F + pair * 1.6F + seed) * 0.6F;
            float chord = pair == 0 ? 0.08F : 0.1F;                                                      // hind wings are broader
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.12F, 0.11F, z);
                ps.mulPose(Axis.ZP.rotation(side * flap));
                ps.mulPose(Axis.YP.rotation(side * (pair == 0 ? -0.08F : 0.1F)));
                float[] span = {0F, 0.12F, 0.45F, 0.72F, 0.85F};
                float[] width = {1F, 1.05F, 0.95F, 0.7F, 0.35F};
                for (int i = 0; i < 4; i++) {
                    float a = span[i] * side, b = span[i + 1] * side, cw = chord * width[i];
                    box(ps, trans, a, -0.004F, -cw, b, 0.004F, cw, i == 0 ? WING_BASE : WING, light, ov);
                }
                box(ps, trans, 0, 0.005F, chord * 0.8F, 0.84F * side, 0.011F, chord * 0.95F, VEIN, light, ov);   // the leading edge
                for (int v = 1; v <= 3; v++) {                                                                   // long veins
                    float zz = chord * (0.8F - v * 0.45F), reach = 0.82F - v * 0.08F;
                    box(ps, trans, 0, 0.005F, zz - 0.004F, reach * side, 0.009F, zz + 0.004F, VEIN_LIGHT, light, ov);
                }
                for (int c = 1; c <= 6; c++) {                                                                   // cross veins
                    float x = c * 0.12F * side, cw = chord * (c < 5 ? 0.9F : 0.6F);
                    box(ps, trans, x - 0.003F, 0.005F, -cw, x + 0.003F, 0.009F, cw * 0.9F, VEIN_LIGHT, light, ov);
                }
                float sx = 0.74F * side;
                box(ps, trans, sx - 0.03F, 0.006F, chord * 0.55F, sx + 0.03F, 0.012F, chord * 0.85F, STIGMA, light, ov); // pterostigma
                ps.popPose();
            }
        }
    }

    private DeepTimeRenderers() {}
}
