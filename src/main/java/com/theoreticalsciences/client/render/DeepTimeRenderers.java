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
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
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

    static final int FUR = 0xFF6A4528, FUR_DARK = 0xFF4A2E18, FUR_LIGHT = 0xFF85583A, HIDE = 0xFF3A2A20, IVORY = 0xFFEADFC2, IVORY_TIP = 0xFFCFC2A0;

    /**
     * The Woolly Mammoth: a high-shouldered body with a fatty hump, a shaggy skirt of long hair, a domed head, small ears
     * (less skin to lose heat through), a swaying trunk and long spiralling tusks. Legs swing in a slow walk; grazing, the
     * head and trunk go down.
     */
    static void mammoth(PoseStack ps, VertexConsumer vc, float t, float limb, float amt, int graze, float partial, int light, int ov) {
        // legs: pillars swinging from the hips and shoulders, opposite corners together
        float[][] legs = {{-0.6F, 0.85F, 0}, {0.6F, 0.85F, (float) Math.PI}, {-0.6F, -0.9F, (float) Math.PI}, {0.6F, -0.9F, 0}};
        for (float[] l : legs) {
            ps.pushPose();
            ps.translate(l[0], 1.35F, l[1]);
            ps.mulPose(Axis.XP.rotation(Mth.cos(limb * 0.5F + l[2]) * 0.45F * amt));
            fur(ps, vc, -0.28F, -1.2F, -0.28F, 0.28F, 0.05F, 0.28F, FUR, FUR_DARK, light, ov);
            box(ps, vc, -0.3F, -1.35F, -0.3F, 0.3F, -1.15F, 0.3F, HIDE, light, ov);              // the broad sole
            ps.popPose();
        }
        // body, hump and the shaggy skirt
        fur(ps, vc, -1.0F, 1.2F, -1.35F, 1.0F, 2.75F, 1.3F, FUR, FUR_DARK, light, ov);
        fur(ps, vc, -0.85F, 2.6F, 0.1F, 0.85F, 3.15F, 1.15F, FUR_LIGHT, FUR, light, ov);         // the shoulder hump
        box(ps, vc, -1.06F, 1.0F, -1.3F, 1.06F, 1.45F, 1.25F, FUR_DARK, light, ov);             // long hair hanging to the knees
        for (int k = -2; k <= 2; k++)
            box(ps, vc, k * 0.4F - 0.12F, 0.85F, -1.0F, k * 0.4F + 0.12F, 1.05F, 1.0F, FUR_DARK, light, ov);
        // the tail, short and tufted
        ps.pushPose();
        ps.translate(0, 2.3F, -1.35F);
        ps.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.08F) * 0.3F));
        box(ps, vc, -0.07F, -0.6F, -0.1F, 0.07F, 0, 0.02F, FUR_DARK, light, ov);
        box(ps, vc, -0.12F, -0.85F, -0.14F, 0.12F, -0.55F, 0.06F, HIDE, light, ov);
        ps.popPose();
        // the head: lowered while grazing
        float g = graze > 0 ? Mth.clamp(Math.min(graze - partial, 60 - graze + partial) / 10F, 0, 1) : 0;
        ps.pushPose();
        ps.translate(0, 2.45F, 1.25F);
        ps.mulPose(Axis.XP.rotation(0.15F + g * 0.7F + Mth.sin(t * 0.03F) * 0.03F));
        fur(ps, vc, -0.62F, -0.7F, 0, 0.62F, 0.62F, 0.95F, FUR, FUR_DARK, light, ov);
        fur(ps, vc, -0.45F, 0.55F, 0.1F, 0.45F, 0.95F, 0.75F, FUR_LIGHT, FUR, light, ov);           // the high dome of the skull
        box(ps, vc, -0.78F, -0.15F, 0.2F, -0.6F, 0.3F, 0.55F, FUR_DARK, light, ov);                 // small ears
        box(ps, vc, 0.6F, -0.15F, 0.2F, 0.78F, 0.3F, 0.55F, FUR_DARK, light, ov);
        box(ps, vc, -0.5F, 0.05F, 0.93F, -0.36F, 0.17F, 0.97F, 0xFF140C08, light, ov);             // eyes
        box(ps, vc, 0.36F, 0.05F, 0.93F, 0.5F, 0.17F, 0.97F, 0xFF140C08, light, ov);
        // tusks: out, down, then curling up and in
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.38F, -0.5F, 0.85F);
            for (int k = 0; k < 6; k++) {
                float w = 0.11F - k * 0.012F;
                box(ps, vc, -w, -w, 0, w, w, 0.3F, k < 4 ? IVORY : IVORY_TIP, light, ov);
                ps.translate(0, 0, 0.27F);
                ps.mulPose(Axis.XP.rotation(-0.32F));
                ps.mulPose(Axis.YP.rotation(-side * 0.16F));
            }
            ps.popPose();
        }
        // the trunk: hanging segments, swaying, the tip curling
        ps.pushPose();
        ps.translate(0, -0.55F, 0.9F);
        float sway = Mth.sin(t * 0.05F) * 0.12F;
        for (int k = 0; k < 6; k++) {
            float w = 0.2F - k * 0.022F;
            box(ps, vc, -w, -0.3F, -w, w, 0.02F, w, k % 2 == 0 ? FUR_DARK : HIDE, light, ov);
            ps.translate(0, -0.28F, 0);
            ps.mulPose(Axis.ZP.rotation(sway * (1 + k * 0.3F)));
            ps.mulPose(Axis.XP.rotation(k >= 4 ? -0.5F : 0.05F - g * 0.08F));
        }
        ps.popPose();
        ps.popPose();
    }

    static final int THORAX = 0xFF2E5A3A, THORAX_DARK = 0xFF1C3A26, SEG_A = 0xFF1F3F5F, SEG_B = 0xFF3A7A88, EYE = 0xFF2A7A60, WING = 0x58D8E8F0,
            VEIN = 0x90506070;

    /**
     * Meganeura: a big-eyed head, a metallic green thorax, a long abdomen banded in steel blue, and four long clear wings,
     * fore and hind pairs beating out of step, as a dragonfly's do.
     */
    static void meganeura(PoseStack ps, VertexConsumer solid, VertexConsumer trans, float t, int seed, int light, int ov) {
        ps.translate(0, 0.2F + Mth.sin(t * 0.15F + seed) * 0.03F, 0);
        // head and its huge compound eyes
        fur(ps, solid, -0.1F, -0.08F, 0.38F, 0.1F, 0.1F, 0.52F, THORAX, THORAX_DARK, light, ov);
        box(ps, solid, -0.17F, -0.04F, 0.4F, -0.07F, 0.12F, 0.54F, EYE, light, ov);
        box(ps, solid, 0.07F, -0.04F, 0.4F, 0.17F, 0.12F, 0.54F, EYE, light, ov);
        // thorax
        fur(ps, solid, -0.13F, -0.11F, 0.05F, 0.13F, 0.13F, 0.4F, THORAX, THORAX_DARK, light, ov);
        // six legs, tucked under, the basket it catches prey in
        for (int k = 0; k < 3; k++)
            for (int side = -1; side <= 1; side += 2)
                box(ps, solid, side * 0.06F, -0.24F, 0.1F + k * 0.1F, side * 0.09F, -0.1F, 0.12F + k * 0.1F, 0xFF101810, light, ov);
        // abdomen: tapering segments with a slight droop
        ps.pushPose();
        ps.translate(0, 0, 0.06F);
        for (int k = 0; k < 9; k++) {
            float w = 0.075F - k * 0.004F;
            box(ps, solid, -w, -w, -0.13F, w, w, 0, k % 2 == 0 ? SEG_A : SEG_B, light, ov);
            ps.translate(0, 0, -0.12F);
            ps.mulPose(Axis.XP.rotation(0.025F + Mth.sin(t * 0.1F + k) * 0.01F));
        }
        box(ps, solid, -0.05F, -0.02F, -0.14F, -0.01F, 0.02F, 0, 0xFF101810, light, ov);         // the cerci at the tip
        box(ps, solid, 0.01F, -0.02F, -0.14F, 0.05F, 0.02F, 0, 0xFF101810, light, ov);
        ps.popPose();
        // wings: fore pair and hind pair, out of phase
        for (int pair = 0; pair < 2; pair++) {
            float z = pair == 0 ? 0.3F : 0.13F, flap = Mth.sin(t * 2.4F + pair * 1.6F + seed) * 0.6F;
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.12F, 0.11F, z);
                ps.mulPose(Axis.ZP.rotation(side * flap));
                float x0 = side > 0 ? 0 : -0.8F, x1 = side > 0 ? 0.8F : 0;
                box(ps, trans, x0, -0.005F, -0.08F, x1, 0.005F, 0.08F, WING, light, ov);
                box(ps, trans, x0, 0.006F, 0.06F, x1, 0.012F, 0.075F, VEIN, light, ov);          // the leading-edge vein
                box(ps, trans, side > 0 ? 0.72F : -0.78F, 0.006F, -0.02F, side > 0 ? 0.78F : -0.72F, 0.012F, 0.05F, 0xC0303840, light, ov);   // the pterostigma
                ps.popPose();
            }
        }
    }

    private DeepTimeRenderers() {}
}
