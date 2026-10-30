package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Procedural models for the creatures of the Quantum Foam, the Transfinite Hierarchy and the black / white holes. */
public final class ExpansionMobRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int FULL = 0xF000F0;

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
            float die = e.deathTime > 0 ? 1F - Math.min(1F, (e.deathTime + partial) / 20F) : 1F;
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            ps.scale(die, die, die);
            switch (id) {
                case "virtual_pair" -> virtualPair(ps, glow, t);
                case "hypercube_drifter" -> hypercube(ps, solid, glow, t, light, ov);
                case "photon_moth" -> moth(ps, trans, glow, t, light, ov);
                case "planck_mite" -> mite(ps, solid, trans, glow, t, limb, amt, light, ov);
                case "vacuum_drifter" -> jelly(ps, trans, glow, t, light, ov);
                case "tidal_horror" -> horror(ps, solid, glow, t, light, ov);
                case "ordinal_hound" -> hound(ps, solid, glow, t, limb, amt, light, ov);
                case "hilbert_bellhop" -> bellhop(ps, solid, glow, t, limb, amt, light, ov);
                case "annihilator" -> annihilator(ps, solid, trans, glow, t, light, ov);
                case "positronium_skitterer" -> skitterer(ps, solid, glow, t, limb, amt, light, ov);
                case "antihydrogen_wisp" -> wisp(ps, trans, glow, t);
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

    static void cube(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float s, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, cx - s, cy - s, cz - s, cx + s, cy + s, cz + s, argb, light, ov);
    }

    /** A particle and its antiparticle orbiting each other; every few seconds they meet and flash. */
    static void virtualPair(PoseStack ps, VertexConsumer glow, float t) {
        float sep = 0.22F + 0.12F * Mth.sin(t * 0.07F);
        float a = t * 0.25F;
        ps.translate(0, 0.35F + Mth.sin(t * 0.1F) * 0.06F, 0);
        cube(ps, glow, Mth.cos(a) * sep, Mth.sin(a * 0.7F) * 0.08F, Mth.sin(a) * sep, 0.08F, 0xFFFF7090, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, -Mth.cos(a) * sep, -Mth.sin(a * 0.7F) * 0.08F, -Mth.sin(a) * sep, 0.08F, 0xFF80F0FF, FULL, OverlayTexture.NO_OVERLAY);
        if (sep < 0.12F) cube(ps, glow, 0, 0, 0, 0.16F, 0xFFFFFFFF, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A tesseract turning through the fourth dimension; its edges are rods, its corners glow. */
    static void hypercube(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.5F + Mth.sin(t * 0.08F) * 0.1F, 0);
        float[][] v = Tesseract4D.project(t * 0.02F, 2.6F);
        float s = 0.38F;
        for (int[] ed : Tesseract4D.EDGES) {
            float[] a = v[ed[0]], b = v[ed[1]];
            BulkCreatureRenderers.Strider.limb(ps, solid, a[0] * s, a[1] * s, a[2] * s, b[0] * s, b[1] * s, b[2] * s, 0.018F, 0xFF304060, light, ov);
        }
        for (float[] p : v) cube(ps, glow, p[0] * s, p[1] * s, p[2] * s, 0.035F, p[3] > 0 ? 0xFFFF80E0 : 0xFF60E0FF, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A moth made of light: a bright body and four pale wings beating. */
    static void moth(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.25F + Mth.sin(t * 0.15F) * 0.08F, 0);
        ProcMesh.box(ps, glow, -0.05F, -0.04F, -0.18F, 0.05F, 0.05F, 0.18F, 0xFFFFF4D0, FULL, OverlayTexture.NO_OVERLAY);
        float flap = Mth.sin(t * 1.2F) * 45F;
        for (int side = -1; side <= 1; side += 2)
            for (int k = 0; k < 2; k++) {
                ps.pushPose();
                ps.translate(0, 0.02F, k == 0 ? -0.06F : 0.08F);
                ps.mulPose(Axis.ZP.rotationDegrees(side * flap));
                float w = k == 0 ? 0.42F : 0.3F;
                ProcMesh.box(ps, trans, side > 0 ? 0 : -w, -0.005F, -0.1F, side > 0 ? w : 0, 0.005F, 0.1F, 0x90FFE890, light, ov);
                ps.popPose();
            }
    }

    /** A knot of foam membrane on six thin legs, with a cluster of glowing eyes. */
    static void mite(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        ps.translate(0, 0.22F, 0);
        cube(ps, trans, 0, 0.05F, 0.05F, 0.2F, 0xB0A0E8FF, light, ov);
        cube(ps, solid, 0, 0.05F, 0.05F, 0.13F, 0xFF3A6080, light, ov);
        cube(ps, solid, 0, 0.02F, -0.2F, 0.1F, 0xFF2A4060, light, ov);
        for (int i = 0; i < 3; i++) cube(ps, glow, (i - 1) * 0.05F, 0.06F, -0.3F, 0.02F, 0xFF80F0FF, FULL, OverlayTexture.NO_OVERLAY);
        for (int i = 0; i < 6; i++) {
            float sx = i < 3 ? 1 : -1, z = ((i % 3) - 1) * 0.14F;
            float sw = Mth.cos(limb * 0.9F + i * 2.1F) * 0.12F * amt;
            BulkCreatureRenderers.Strider.limb(ps, solid, sx * 0.12F, 0.05F, z, sx * 0.32F, 0.18F, z + sw, 0.015F, 0xFF3A6080, light, ov);
            BulkCreatureRenderers.Strider.limb(ps, solid, sx * 0.32F, 0.18F, z + sw, sx * 0.45F, -0.22F, z + sw * 1.5F, 0.012F, 0xFF3A6080, light, ov);
        }
    }

    /**
     * An Annihilator: a cage of magnetic bands around an antihelium core. The bands spin; where they cross, matter in
     * the air annihilates in white flashes.
     */
    static void annihilator(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.75F + Mth.sin(t * 0.09F) * 0.08F, 0);
        cube(ps, glow, 0, 0, 0, 0.16F + 0.03F * Mth.sin(t * 0.4F), 0xFFFF80FF, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, trans, 0, 0, 0, 0.26F, 0x60FFB070, light, ov);
        for (int band = 0; band < 3; band++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(t * (6 + band * 3) + band * 60));
            ps.mulPose(Axis.XP.rotationDegrees(band * 60));
            for (int i = 0; i < 16; i++) {
                float a = i * Mth.TWO_PI / 16, r = 0.5F;
                cube(ps, solid, Mth.cos(a) * r, 0, Mth.sin(a) * r, 0.045F, 0xFF5A3A2A, light, ov);
            }
            ps.popPose();
        }
        if (((int) (t * 0.25F)) % 5 == 0) cube(ps, glow, Mth.sin(t) * 0.4F, Mth.cos(t * 1.3F) * 0.3F, Mth.cos(t) * 0.4F, 0.06F, 0xFFFFFFFF, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A Positronium Skitterer: a pale body with a glowing electron-positron pair orbiting in its head. */
    static void skitterer(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        ps.translate(0, 0.25F, 0);
        cube(ps, solid, 0, 0.05F, 0.05F, 0.16F, 0xFF4A3020, light, ov);
        cube(ps, solid, 0, 0.06F, -0.22F, 0.11F, 0xFF5A3A2A, light, ov);
        float a = t * 0.5F;
        cube(ps, glow, Mth.cos(a) * 0.08F, 0.08F, -0.24F + Mth.sin(a) * 0.08F, 0.03F, 0xFFFF70B0, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, -Mth.cos(a) * 0.08F, 0.08F, -0.24F - Mth.sin(a) * 0.08F, 0.03F, 0xFF70B0FF, FULL, OverlayTexture.NO_OVERLAY);
        for (int i = 0; i < 8; i++) {
            float sx = i < 4 ? 1 : -1, z = ((i % 4) - 1.5F) * 0.11F;
            float sw = Mth.cos(limb * 1.1F + i * 1.7F) * 0.12F * amt;
            BulkCreatureRenderers.Strider.limb(ps, solid, sx * 0.13F, 0.05F, z, sx * 0.34F, 0.2F, z + sw, 0.014F, 0xFF5A3A2A, light, ov);
            BulkCreatureRenderers.Strider.limb(ps, solid, sx * 0.34F, 0.2F, z + sw, sx * 0.46F, -0.24F, z + sw * 1.4F, 0.011F, 0xFF5A3A2A, light, ov);
        }
    }

    /** An Antihydrogen Wisp: one bright antiproton with a positron cloud breathing around it. */
    static void wisp(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t) {
        ps.translate(0, 0.4F + Mth.sin(t * 0.07F) * 0.1F, 0);
        cube(ps, glow, 0, 0, 0, 0.05F, 0xFFFF70B0, FULL, OverlayTexture.NO_OVERLAY);
        float r = 0.22F + 0.04F * Mth.sin(t * 0.2F);
        cube(ps, trans, 0, 0, 0, r, 0x5080F0FF, FULL, OverlayTexture.NO_OVERLAY);
        for (int i = 0; i < 6; i++) {
            float a = t * 0.3F + i * Mth.TWO_PI / 6;
            cube(ps, glow, Mth.cos(a) * r, Mth.sin(a * 1.7F) * r * 0.6F, Mth.sin(a) * r, 0.025F, 0xFF80F0FF, FULL, OverlayTexture.NO_OVERLAY);
        }
    }

    /** A jellyfish of false vacuum: a translucent dome and slow trailing threads. */
    static void jelly(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, int light, int ov) {
        float pulse = 1F + 0.1F * Mth.sin(t * 0.15F);
        ps.translate(0, 0.7F, 0);
        for (int k = 0; k < 4; k++) {
            float r = (0.5F - k * 0.1F) * pulse, y = k * 0.1F;
            ProcMesh.box(ps, trans, -r, y, -r, r, y + 0.1F, r, 0x6080A0FF, light, ov);
        }
        cube(ps, glow, 0, 0.1F, 0, 0.12F, 0xFF202040, FULL, OverlayTexture.NO_OVERLAY);
        for (int i = 0; i < 8; i++) {
            float a = i * Mth.TWO_PI / 8, x = Mth.cos(a) * 0.35F, z = Mth.sin(a) * 0.35F;
            float sway = Mth.sin(t * 0.1F + i) * 0.1F;
            BulkCreatureRenderers.Strider.limb(ps, trans, x, 0, z, x * 1.2F + sway, -0.8F, z * 1.2F, 0.012F, 0x806080C0, light, ov);
        }
    }

    /** A thing that fell too close: stretched into a long writhing strand, glowing red at the bottom where the tide is strongest. */
    static void horror(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, int light, int ov) {
        int n = 14;
        for (int i = 0; i < n; i++) {
            float y = 0.1F + i * 0.13F;
            float w = 0.06F + 0.12F * Mth.sin((float) i / n * Mth.PI);
            float x = Mth.sin(t * 0.12F + i * 0.5F) * 0.12F;
            boolean hot = i < 3;
            ProcMesh.box(ps, hot ? glow : solid, x - w, y, -w * 0.6F, x + w, y + 0.12F, w * 0.6F, hot ? 0xFFFF5020 : 0xFF300808, hot ? FULL : light,
                    hot ? OverlayTexture.NO_OVERLAY : ov);
        }
        cube(ps, glow, Mth.sin(t * 0.12F + 7F) * 0.12F, 1.85F, -0.08F, 0.05F, 0xFFFFA060, FULL, OverlayTexture.NO_OVERLAY);
    }

    /** A hound whose body is a sequence of shrinking blocks - 1, 1/2, 1/4... - so its tail never quite ends. */
    static void hound(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        ProcMesh.box(ps, solid, -0.22F, 0.45F, -0.4F, 0.22F, 0.8F, 0.25F, 0xFF30281A, light, ov);
        ProcMesh.box(ps, solid, -0.17F, 0.6F, -0.72F, 0.17F, 0.9F, -0.4F, 0xFF3A3020, light, ov);
        cube(ps, glow, -0.08F, 0.8F, -0.73F, 0.03F, 0xFFFFD060, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, 0.08F, 0.8F, -0.73F, 0.03F, 0xFFFFD060, FULL, OverlayTexture.NO_OVERLAY);
        float z = 0.25F, s = 0.13F;
        for (int k = 0; k < 7; k++) {
            z += s;
            cube(ps, k % 2 == 0 ? glow : solid, 0, 0.72F + k * 0.02F + Mth.sin(t * 0.3F + k) * 0.02F, z, s, k % 2 == 0 ? 0xFFFFD060 : 0xFF30281A,
                    k % 2 == 0 ? FULL : light, k % 2 == 0 ? OverlayTexture.NO_OVERLAY : ov);
            z += s;
            s *= 0.6F;
        }
        for (int i = 0; i < 4; i++) {
            float sx = i % 2 == 0 ? 1 : -1, lz = i < 2 ? -0.3F : 0.15F;
            float sw = Mth.cos(limb * 0.66F + (i == 0 || i == 3 ? 0 : Mth.PI)) * 0.25F * amt;
            BulkCreatureRenderers.Strider.limb(ps, solid, sx * 0.15F, 0.5F, lz, sx * 0.17F, 0, lz + sw, 0.05F, 0xFF241E14, light, ov);
        }
    }

    /** The bellhop of Hilbert's Hotel: red uniform, pillbox hat, a brass key glinting in one hand. */
    static void bellhop(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        float sw = Mth.cos(limb * 0.66F) * 0.5F * amt;
        for (int side = -1; side <= 1; side += 2) {      // legs
            ps.pushPose();
            ps.translate(side * 0.1F, 0.75F, 0);
            ps.mulPose(Axis.XP.rotation(sw * side));
            ProcMesh.box(ps, solid, -0.08F, -0.75F, -0.08F, 0.08F, 0, 0.08F, 0xFF201818, light, ov);
            ps.popPose();
        }
        ProcMesh.box(ps, solid, -0.22F, 0.75F, -0.13F, 0.22F, 1.45F, 0.13F, 0xFFA01818, light, ov);
        ProcMesh.box(ps, solid, -0.22F, 1.18F, -0.135F, 0.22F, 1.22F, 0.135F, 0xFFE0B040, light, ov);
        for (int side = -1; side <= 1; side += 2) {      // arms
            ps.pushPose();
            ps.translate(side * 0.29F, 1.42F, 0);
            ps.mulPose(Axis.XP.rotation(-sw * side * 0.8F));
            ProcMesh.box(ps, solid, -0.07F, -0.62F, -0.07F, 0.07F, 0, 0.07F, 0xFFA01818, light, ov);
            if (side > 0) cube(ps, glow, 0, -0.66F, -0.06F, 0.04F, 0xFFFFD060, FULL, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ProcMesh.box(ps, solid, -0.18F, 1.45F, -0.18F, 0.18F, 1.8F, 0.18F, 0xFFE8C8A8, light, ov);
        ProcMesh.box(ps, solid, -0.15F, 1.8F, -0.15F, 0.15F, 1.92F, 0.15F, 0xFFA01818, light, ov);
        cube(ps, glow, -0.07F, 1.66F, -0.185F, 0.025F, 0xFF203060, FULL, OverlayTexture.NO_OVERLAY);
        cube(ps, glow, 0.07F, 1.66F, -0.185F, 0.025F, 0xFF203060, FULL, OverlayTexture.NO_OVERLAY);
    }

    private ExpansionMobRenderers() {}
}
