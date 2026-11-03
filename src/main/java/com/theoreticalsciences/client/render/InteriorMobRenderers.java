package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.interior.InteriorMobs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** 1.3.7: procedural models for the Planetary Interior's creatures. */
public final class InteriorMobRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int FULL = 0xF000F0;

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
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {         // 1.10.1: each creature dies its own way
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (id) {
                case "magma_crawler" -> crawler(ps, solid, glow, t, limb, amt, e.isInFluidType(), light, ov);
                case "plume_elemental" -> plume(ps, solid, trans, glow, t, light, ov);
                case "fission_husk" -> husk(ps, solid, glow, t, limb, amt, light, ov, e instanceof InteriorMobs.FissionHusk h ? h.scale() : 1F);
                case "geodynamo_wraith" -> wraith(ps, solid, trans, glow, t, light, ov);
                case "random_walker" -> walker(ps, glow, trans, t, e.getId(), light, ov);
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

    /**
     * 1.7.0, the Random Walker: fourteen grains, each wandering on its own random walk about the cloud's centre (smooth
     * noise from incommensurate frequencies, seeded by the entity), round a faint core.
     */
    static void walker(PoseStack ps, VertexConsumer glow, VertexConsumer trans, float t, int seed, int light, int ov) {
        java.util.Random r = new java.util.Random(seed * 7919L);
        for (int i = 0; i < 14; i++) {
            float a = r.nextFloat() * 6.28F, b = r.nextFloat() * 6.28F, c = r.nextFloat() * 6.28F;
            float f1 = 0.07F + r.nextFloat() * 0.09F, f2 = 0.11F + r.nextFloat() * 0.07F, f3 = 0.05F + r.nextFloat() * 0.11F;
            float x = 0.28F * (Mth.sin(t * f1 + a) + 0.5F * Mth.sin(t * f2 * 2.3F + b));
            float y = 0.45F + 0.22F * (Mth.sin(t * f2 + b) + 0.5F * Mth.sin(t * f3 * 1.9F + c));
            float z = 0.28F * (Mth.sin(t * f3 + c) + 0.5F * Mth.sin(t * f1 * 2.7F + a));
            float jx = (r.nextFloat() - 0.5F) * 0.04F * Mth.sin(t * 1.7F + i), jz = (r.nextFloat() - 0.5F) * 0.04F * Mth.cos(t * 1.3F + i);
            cube(ps, glow, x + jx, y, z + jz, 0.035F + 0.015F * (i % 3), i % 4 == 0 ? 0xFFFFFFFF : 0xFFC8C8D0, FULL, OverlayTexture.NO_OVERLAY);
        }
        cube(ps, trans, 0, 0.45F, 0, 0.22F, 0x30E0E0E8, light, ov);
    }

    static void cube(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float s, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, cx - s, cy - s, cz - s, cx + s, cy + s, cz + s, argb, light, ov);
    }

    /**
     * Seven basalt segments riding a wave that travels down the body - a centipede's gait - with each segment's pair of
     * legs stepping in turn. 1.3.8: the gait is driven by the distance it has actually moved (as vanilla legs are), not
     * by the clock, so a crawler that is not going anywhere lies still - only its antennae and glow stir.
     */
    static void crawler(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, boolean swimming, int light, int ov) {
        float sp = Math.min(1F, amt * 4F);                  // it is slow: a gentle swim already counts as full stride
        float ph0 = limb * 2.0F;                            // one step cycle per ~3 blocks crawled
        float amp = sp * (swimming ? 0.24F : 0.16F);
        ps.translate(0, swimming ? 0.3F : 0.25F, 0);
        float px = 0, pz = -0.62F;
        for (int i = 0; i < 7; i++) {
            float z = -0.5F + i * 0.17F;
            float wave = Mth.sin(ph0 - i * 0.9F);
            float sway = wave * amp * (0.5F + i * 0.12F);
            float bob = (swimming ? Mth.cos(ph0 - i * 0.9F) * 0.05F : Mth.abs(wave) * 0.03F) * sp;
            float s = i == 0 ? 0.15F : 0.13F - i * 0.008F;
            ps.pushPose();
            ps.translate(sway, bob, z);
            ps.mulPose(Axis.YP.rotation(Mth.cos(ph0 - i * 0.9F) * amp * 1.4F));
            ProcMesh.box(ps, solid, -s, -s * 0.8F, -0.08F, s, s * 0.8F, 0.08F, 0xFF2A2420, light, ov);
            ProcMesh.box(ps, glow, -s * 0.7F, s * 0.8F, -0.02F, s * 0.7F, s * 0.8F + 0.01F, 0.02F, 0xFFFF7020, FULL, OverlayTexture.NO_OVERLAY);
            for (int sd = -1; sd <= 1; sd += 2) {
                // each leg lifts and swings on its own phase; left and right alternate; swimming, they paddle wider
                float ph = ph0 * 2.2F - i * 1.1F + (sd > 0 ? 0 : Mth.PI);
                float swing = Mth.sin(ph) * sp * (swimming ? 0.28F : 0.2F);
                float lift = Math.max(0F, Mth.cos(ph)) * sp * 0.12F;
                float kx = sd * (s + 0.14F), ky = 0.05F + lift;
                BulkCreatureRenderers.Strider.limb(ps, solid, sd * s, 0, 0, kx, ky, swing * 0.5F, 0.025F, 0xFF3A3028, light, ov);
                BulkCreatureRenderers.Strider.limb(ps, solid, kx, ky, swing * 0.5F, sd * (s + 0.22F), -0.24F + lift, swing, 0.02F, 0xFF3A3028, light, ov);
            }
            ps.popPose();
            if (i == 0) { px = sway; pz = z - 0.1F; }
        }
        cube(ps, glow, px - 0.06F, 0.04F, pz, 0.025F, 0xFFFFC040, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, px + 0.06F, 0.04F, pz, 0.025F, 0xFFFFC040, FULL, OverlayTexture.NO_OVERLAY);
        for (int sd = -1; sd <= 1; sd += 2)                       // antennae: the one thing that moves at rest - feeling the heat
            BulkCreatureRenderers.Strider.limb(ps, solid, px + sd * 0.05F, 0.08F, pz, px + sd * 0.16F, 0.2F + Mth.sin(t * 0.15F + sd * 2F) * 0.03F, pz - 0.2F,
                    0.01F, 0xFF4A3A30, light, ov);
    }

    /** A column of hot rock: crust chunks orbiting a rising core of light. */
    static void plume(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, int light, int ov) {
        for (int i = 0; i < 6; i++) {
            float y = ((t * 0.04F + i / 6F) % 1F) * 2.0F;
            float s = 0.32F - y * 0.1F;
            cube(ps, glow, Mth.sin(t * 0.1F + i) * 0.05F, y + 0.1F, Mth.cos(t * 0.1F + i) * 0.05F, s, i % 2 == 0 ? 0xFFFF9030 : 0xFFFFD060, FULL, OverlayTexture.NO_OVERLAY);
        }
        for (int i = 0; i < 8; i++) {
            float a = t * 0.06F + i * 0.785F, r = 0.45F + 0.08F * Mth.sin(t * 0.13F + i);
            float y = 0.3F + (i % 4) * 0.45F;
            cube(ps, solid, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.11F, 0xFF3A2418, light, ov);
        }
        cube(ps, trans, 0, 1.0F, 0, 0.42F, 0x60FF6020, light, ov);
        cube(ps, glow, -0.1F, 1.55F, -0.28F, 0.04F, 0xFFFFFFC0, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, 0.1F, 1.55F, -0.28F, 0.04F, 0xFFFFFFC0, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A gaunt figure of ashen flesh, its cracks lit the green of what is splitting inside it. Smaller with each fission. */
    static void husk(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov, float sc) {
        ps.scale(sc, sc, sc);
        float sw = Mth.cos(limb * 0.66F) * 0.5F * amt;
        int skin = 0xFF4A5040, crack = 0xFFA0FF40;
        // legs
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.12F, 0.75F, 0);
            ps.mulPose(Axis.XP.rotation(sd * sw));
            ProcMesh.box(ps, solid, -0.1F, -0.75F, -0.1F, 0.1F, 0, 0.1F, skin, light, ov);
            ps.popPose();
        }
        ProcMesh.box(ps, solid, -0.25F, 0.75F, -0.13F, 0.25F, 1.45F, 0.13F, skin, light, ov);
        ProcMesh.box(ps, glow, -0.05F, 0.8F, -0.135F, 0.05F, 1.4F, -0.13F, crack, FULL, OverlayTexture.NO_OVERLAY);
        ProcMesh.box(ps, glow, -0.2F, 1.1F, -0.135F, 0.2F, 1.14F, -0.13F, crack, FULL, OverlayTexture.NO_OVERLAY);
        // arms, held out
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.33F, 1.38F, 0);
            ps.mulPose(Axis.XP.rotation(1.4F + Mth.sin(t * 0.08F + sd) * 0.1F));      // held out in front (-z)
            ProcMesh.box(ps, solid, -0.08F, -0.7F, -0.08F, 0.08F, 0, 0.08F, skin, light, ov);
            ps.popPose();
        }
        ProcMesh.box(ps, solid, -0.2F, 1.45F, -0.2F, 0.2F, 1.85F, 0.2F, skin, light, ov);
        cube(ps, glow, -0.08F, 1.68F, -0.205F, 0.035F, crack, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, 0.08F, 1.68F, -0.205F, 0.035F, crack, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A loop of current: an iron ring turning about a magnetite core, with field lines arcing from pole to pole. */
    static void wraith(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.6F + Mth.sin(t * 0.1F) * 0.08F, 0);
        cube(ps, solid, 0, 0, 0, 0.16F, 0xFF202028, light, ov);
        cube(ps, glow, 0, 0, -0.17F, 0.03F, 0xFF80E0FF, FULL, OverlayTexture.NO_OVERLAY);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(0.5F));
        ps.mulPose(Axis.YP.rotation(t * 0.08F));
        for (int i = 0; i < 16; i++) {
            float a = i * Mth.TWO_PI / 16;
            cube(ps, solid, Mth.cos(a) * 0.5F, 0, Mth.sin(a) * 0.5F, 0.06F, 0xFF6A6E78, light, ov);
        }
        ps.popPose();
        for (int k = 0; k < 4; k++) {                      // field lines: dipole loops from the top pole round to the bottom
            float az = k * Mth.HALF_PI + t * 0.03F;
            for (int i = 1; i < 12; i++) {
                float th = i * Mth.PI / 12, r = 0.75F * Mth.sin(th) * Mth.sin(th);
                float x = r * Mth.sin(th) * Mth.cos(az), y = r * Mth.cos(th), z = r * Mth.sin(th) * Mth.sin(az);
                float pulse = 0.5F + 0.5F * Mth.sin(t * 0.3F - i * 0.6F);
                cube(ps, glow, x * 1.4F, y * 1.4F, z * 1.4F, 0.02F, (0xFF << 24) | ((int) (0x40 + 0x60 * pulse) << 16) | (0xB0 << 8) | 0xFF, FULL, OverlayTexture.NO_OVERLAY);
            }
        }
    }

    private InteriorMobRenderers() {}
}
