package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.frontier.BinaryBoss;
import com.theoreticalsciences.frontier.FrontierMobs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 1.10.0: procedural models for the Accretion Frontier's creatures, the Black Swan and The Binary. */
public final class FrontierMobRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int FULL = 0xF000F0;
    private static final int NO = OverlayTexture.NO_OVERLAY;

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
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            if (id.equals("lens_phantom")) {                       // drawn where its bent light says it is, not where it is
                Vec3 o = FrontierMobs.LensPhantom.imageOffset(e, partial);
                ps.translate(o.x, o.y, o.z);
            }
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {         // 1.10.1: each creature dies its own way
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (id) {
                case "nebuloid" -> nebuloid(ps, trans, glow, t, e.getId(), light, ov);
                case "pulsar_wisp" -> wisp(ps, solid, glow, trans, e, partial, light, ov);
                case "tidal_stretcher" -> stretcher(ps, solid, glow, t, limb, amt, light, ov);
                case "lens_phantom" -> phantom(ps, trans, glow, t, limb, amt, light, ov);
                case "hawking_imp" -> imp(ps, solid, glow, t, e instanceof FrontierMobs.HawkingImp h ? h.mass() : 1F, light, ov);
                case "boltzmann_brain" -> brain(ps, solid, glow, t, light, ov);
                case "black_swan" -> swan(ps, solid, t, limb, amt, e.hurtTime, light, ov);
                case "binary_serpent" -> serpent(ps, solid, glow, t, e instanceof BinaryBoss.Serpent s ? s.separation() : 12F, light, ov);
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

    /** A colour scaled towards black, for additive (glowing) layers where brightness, not alpha, is what shows. */
    static int dim(int rgb, float k) {
        int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static void cube(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float s, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, cx - s, cy - s, cz - s, cx + s, cy + s, cz + s, argb, light, ov);
    }

    /** A knot of gas and dust: soft translucent puffs drifting round a few bright newborn stars. */
    static void nebuloid(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, int seed, int light, int ov) {
        java.util.Random r = new java.util.Random(seed * 31L);
        for (int i = 0; i < 12; i++) {
            float a = r.nextFloat() * 6.28F, rr = 0.2F + r.nextFloat() * 0.45F, y = 0.3F + r.nextFloat() * 0.7F;
            float x = Mth.cos(a + t * 0.01F) * rr, z = Mth.sin(a + t * 0.01F) * rr;
            int col = (0x38 << 24) | (i % 3 == 0 ? 0xC090FF : i % 3 == 1 ? 0x8070C0 : 0xE0A0C0);
            cube(ps, trans, x, y + 0.04F * Mth.sin(t * 0.05F + i), z, 0.22F + r.nextFloat() * 0.15F, col, light, ov);
        }
        for (int i = 0; i < 3; i++) {
            float a = i * 2.1F + t * 0.02F;
            cube(ps, glow, Mth.cos(a) * 0.2F, 0.6F + 0.1F * i, Mth.sin(a) * 0.2F, 0.03F, 0xFFFFFFFF, FULL, NO);
        }
    }

    /** A neutron star in miniature: a small bright core, a magnetic axis, and the twin beam sweeping round on the beat. */
    static void wisp(PoseStack ps, VertexConsumer solid, VertexConsumer glow, VertexConsumer trans, LivingEntity e, float partial, int light, int ov) {
        ps.translate(0, 0.4F, 0);
        // 1.10.1: it pulses - on each beat the core flares and swells, then dims; a shell of light spreads out from it
        float phase = ((e.level().getGameTime() % FrontierMobs.PulsarWisp.PERIOD) + partial) / FrontierMobs.PulsarWisp.PERIOD;
        float flare = (float) Math.exp(-phase * 7);
        cube(ps, glow, 0, 0, 0, 0.11F + 0.09F * flare, 0xFFE0F8FF, FULL, NO);
        // the halo and the spreading shell are pure added light (no depth written), so they never hide what is behind them
        cube(ps, glow, 0, 0, 0, 0.22F + 0.12F * flare, dim(0x60C0FF, 0.25F + 0.45F * flare), FULL, NO);
        if (phase < 0.6F) {
            float q = phase / 0.6F;
            cube(ps, glow, 0, 0, 0, 0.3F + 1.6F * q, dim(0xA0E0FF, 0.22F * (1 - q)), FULL, NO);
        }
        double a = FrontierMobs.PulsarWisp.beamAngle(e.level().getGameTime(), partial);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation((float) a));
        ps.mulPose(Axis.ZP.rotation(0.4F));                  // the magnetic axis is tilted from the spin axis
        for (int sd = -1; sd <= 1; sd += 2)
            for (int i = 1; i < 14; i++) {
                float f = 1F - i / 14F;
                cube(ps, glow, 0, sd * i * 0.18F, 0, (0.02F + 0.03F * f) * (1 + 0.6F * flare), (((int) (0xFF * f * (0.45F + 0.55F * flare))) << 24) | 0x80E0FF, FULL, NO);
            }
        ps.popPose();
    }

    /** Tall and thin, as if pulled lengthwise: long limbs, a narrow body, a glowing seam where it is being stretched. */
    static void stretcher(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        float sw = Mth.cos(limb * 0.5F) * 0.5F * amt;
        int skin = 0xFF3A2010, seam = 0xFFFF9040;
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.1F, 1.2F, 0);
            ps.mulPose(Axis.XP.rotation(sd * sw));
            ProcMesh.box(ps, solid, -0.06F, -1.2F, -0.06F, 0.06F, 0, 0.06F, skin, light, ov);
            ps.popPose();
        }
        float str = 1F + 0.05F * Mth.sin(t * 0.2F);
        ProcMesh.box(ps, solid, -0.16F, 1.2F, -0.1F, 0.16F, 1.2F + 0.9F * str, 0.1F, skin, light, ov);
        ProcMesh.box(ps, glow, -0.02F, 1.25F, -0.105F, 0.02F, 1.15F + 0.9F * str, -0.1F, seam, FULL, NO);
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.22F, 2.05F, 0);
            ps.mulPose(Axis.XP.rotation(1.2F + Mth.sin(t * 0.1F + sd) * 0.2F));
            ProcMesh.box(ps, solid, -0.05F, -1.1F, -0.05F, 0.05F, 0, 0.05F, skin, light, ov);
            ps.popPose();
        }
        ProcMesh.box(ps, solid, -0.12F, 2.1F, -0.12F, 0.12F, 2.55F, 0.12F, skin, light, ov);
        cube(ps, glow, -0.05F, 2.38F, -0.125F, 0.025F, seam, FULL, NO);
        cube(ps, glow, 0.05F, 2.38F, -0.125F, 0.025F, seam, FULL, NO);
    }

    /** A pale, glassy figure, never quite solid - only its image, after all. */
    static void phantom(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        float sw = Mth.cos(limb * 0.66F) * 0.4F * amt;
        int body = 0x70D8E0FF;
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.1F, 0.8F, 0);
            ps.mulPose(Axis.XP.rotation(sd * sw));
            ProcMesh.box(ps, trans, -0.08F, -0.8F, -0.08F, 0.08F, 0, 0.08F, body, light, ov);
            ps.popPose();
        }
        ProcMesh.box(ps, trans, -0.22F, 0.8F, -0.12F, 0.22F, 1.45F, 0.12F, body, light, ov);
        ProcMesh.box(ps, trans, -0.18F, 1.45F, -0.18F, 0.18F, 1.85F, 0.18F, body, light, ov);
        // an Einstein ring round its head: the light of what is behind it, bent into a circle
        ps.pushPose();
        ps.translate(0, 1.65F, 0);
        for (int i = 0; i < 20; i++) {
            float a = i * Mth.TWO_PI / 20 + t * 0.02F;
            cube(ps, glow, Mth.cos(a) * 0.35F, Mth.sin(a) * 0.35F, 0.05F, 0.025F, 0xFFE8E8FF, FULL, NO);
        }
        ps.popPose();
    }

    /** A black sphere ringed by its own glow; it shrinks, and its glow whitens, as it evaporates. */
    static void imp(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float mass, int light, int ov) {
        float s = 0.12F + 0.2F * Math.max(0F, mass);
        ps.translate(0, 0.3F, 0);
        cube(ps, solid, 0, 0, 0, s, 0xFF000000, light, ov);
        int hot = mass > 0.6F ? 0xFFFF9040 : mass > 0.3F ? 0xFFFFE0A0 : 0xFFE0F0FF;
        ps.mulPose(Axis.YP.rotation(t * (0.1F + 0.4F * (1 - mass))));
        for (int i = 0; i < 16; i++) {
            float a = i * Mth.TWO_PI / 16;
            cube(ps, glow, Mth.cos(a) * s * 1.8F, 0, Mth.sin(a) * s * 1.8F, 0.02F + 0.02F * (1 - mass), hot, FULL, NO);
        }
    }

    /** A brain fluctuated out of nothing: pink lobes, faintly lit from within, flickering at the edges. */
    static void brain(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.4F + 0.05F * Mth.sin(t * 0.07F), 0);
        int pink = 0xFFD08898;
        for (int sd = -1; sd <= 1; sd += 2) {
            ProcMesh.box(ps, solid, sd * 0.02F, -0.15F, -0.25F, sd * 0.24F, 0.15F, 0.25F, pink, light, ov);
            for (int i = 0; i < 4; i++)
                ProcMesh.box(ps, solid, sd * 0.05F, 0.15F, -0.22F + i * 0.12F, sd * 0.21F, 0.2F, -0.14F + i * 0.12F, 0xFFC07888, light, ov);
        }
        ProcMesh.box(ps, solid, -0.06F, -0.3F, -0.05F, 0.06F, -0.15F, 0.07F, 0xFFB06878, light, ov);
        if (((int) t / 3) % 7 == 0) cube(ps, glow, 0, 0.05F, 0, 0.3F, 0x30FFE0F0, FULL, NO);
    }

    /**
     * A black swan, built the way vanilla builds its animals - a body with parts hung off it, each turning about its own
     * joint: two legs with webbed feet that swing as it walks; a deep, rounded body; folded wings with the curled feathers
     * of a black swan's back and a flash of white flight feathers at their tips; a raised tail; a long neck of six segments
     * held in an S that sways and bobs with each step; and a head with red eyes and a red bill banded with white. Struck,
     * it "busks" - raises its wings and draws its neck back - as a threatened swan does.
     */
    static void swan(PoseStack ps, VertexConsumer solid, float t, float limb, float amt, int hurt, int light, int ov) {
        int black = 0xFF16161A, sooty = 0xFF24242A, curl = 0xFF34343C, white = 0xFFE8E8E8, red = 0xFFC8202A, leg = 0xFF2A2A2E;
        float step = Mth.cos(limb * 0.6662F) * 1.1F * amt;
        float busk = hurt > 0 ? Math.min(1F, hurt / 6F) : 0F;
        // legs: hip joints under the body, a webbed foot pointing forward
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.09F, 0.3F, 0.06F);
            ps.mulPose(Axis.XP.rotation(sd * step * 0.6F));
            ProcMesh.box(ps, solid, -0.025F, -0.28F, -0.025F, 0.025F, 0, 0.025F, leg, light, ov);
            ProcMesh.box(ps, solid, -0.06F, -0.3F, -0.12F, 0.06F, -0.28F, 0.02F, leg, light, ov);
            ps.popPose();
        }
        // the body: a deep keel, rounded at top and ends, riding a little up and down with the stride
        float ride = Math.abs(Mth.sin(limb * 0.6662F)) * 0.02F * amt;
        ps.pushPose();
        ps.translate(0, ride, 0);
        ProcMesh.box(ps, solid, -0.19F, 0.3F, -0.26F, 0.19F, 0.56F, 0.3F, black, light, ov);
        ProcMesh.box(ps, solid, -0.15F, 0.26F, -0.2F, 0.15F, 0.3F, 0.24F, sooty, light, ov);          // belly
        ProcMesh.box(ps, solid, -0.15F, 0.56F, -0.18F, 0.15F, 0.61F, 0.26F, black, light, ov);        // rounded back
        ProcMesh.box(ps, solid, -0.13F, 0.34F, -0.32F, 0.13F, 0.54F, -0.26F, black, light, ov);       // breast
        // tail: a short raised fan
        ps.pushPose();
        ps.translate(0, 0.52F, 0.3F);
        ps.mulPose(Axis.XP.rotation(-0.5F + 0.05F * Mth.sin(t * 0.15F)));
        ProcMesh.box(ps, solid, -0.08F, -0.03F, 0, 0.08F, 0.03F, 0.14F, sooty, light, ov);
        ps.popPose();
        // wings: folded along the sides, lifted and spread when it busks
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();
            ps.translate(sd * 0.19F, 0.55F, -0.14F);
            ps.mulPose(Axis.ZP.rotation(sd * (0.08F + 0.9F * busk)));
            ps.mulPose(Axis.XP.rotation(-0.12F - 0.25F * busk));
            ProcMesh.box(ps, solid, 0, -0.2F, 0, sd * 0.05F, 0.02F, 0.5F, sooty, light, ov);
            ProcMesh.box(ps, solid, 0, -0.17F, 0.5F, sd * 0.04F, -0.05F, 0.6F, white, light, ov);      // white primaries
            for (int i = 0; i < 3; i++)                                                                 // curled back feathers
                ProcMesh.box(ps, solid, sd * 0.005F, 0.02F, 0.2F + i * 0.1F, sd * 0.045F, 0.06F + 0.02F * (i & 1), 0.27F + i * 0.1F, curl, light, ov);
            ps.popPose();
        }
        // the neck: six segments, each turned a little from the last, making an S; it sways at rest and bobs as it walks
        ps.pushPose();
        ps.translate(0, 0.52F, -0.28F);
        float sway = 0.06F * Mth.sin(t * 0.05F), bob = 0.12F * Mth.sin(limb * 0.6662F * 2) * amt;
        float[] bend = {-0.35F, -0.15F, 0.15F, 0.35F, 0.3F, 0.1F};
        for (int i = 0; i < bend.length; i++) {
            ps.mulPose(Axis.XP.rotation(bend[i] + (i < 3 ? bob : -bob * 0.5F) + busk * (i < 2 ? 0.25F : -0.1F)));
            ps.mulPose(Axis.ZP.rotation(sway));
            float w = 0.055F - i * 0.004F;
            ProcMesh.box(ps, solid, -w, 0, -w, w, 0.11F, w, black, light, ov);
            ps.translate(0, 0.1F, 0);
        }
        // the head, looking forward and a little down, with red eyes and a red bill banded with white near its tip
        ps.mulPose(Axis.XP.rotation(-0.6F));
        ProcMesh.box(ps, solid, -0.055F, -0.02F, -0.06F, 0.055F, 0.08F, 0.08F, black, light, ov);
        for (int sd = -1; sd <= 1; sd += 2)
            ProcMesh.box(ps, solid, sd * 0.055F, 0.03F, -0.02F, sd * 0.06F, 0.05F, 0.0F, red, light, ov);
        ProcMesh.box(ps, solid, -0.035F, 0.0F, -0.17F, 0.035F, 0.045F, -0.06F, red, light, ov);
        ProcMesh.box(ps, solid, -0.036F, 0.0F, -0.2F, 0.036F, 0.044F, -0.17F, white, light, ov);
        ProcMesh.box(ps, solid, -0.03F, 0.005F, -0.23F, 0.03F, 0.035F, -0.2F, red, light, ov);
        ps.popPose();
        ps.popPose();
    }

    /** One of The Binary: a coiled serpent of a compact star, its glow brightening as the orbit closes. */
    static void serpent(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float sep, int light, int ov) {
        float heat = Mth.clamp(1F - (sep - 1.6F) / 10.4F, 0F, 1F);
        int core = heat > 0.7F ? 0xFFFFFFFF : heat > 0.35F ? 0xFFFFE080 : 0xFFFFB040;
        ps.translate(0, 0.6F, 0);
        cube(ps, glow, 0, 0, 0, 0.3F + 0.1F * heat, core, FULL, NO);
        for (int i = 1; i < 10; i++) {
            float a = i * 0.6F - t * (0.1F + 0.3F * heat);
            float r = 0.4F + i * 0.06F;
            cube(ps, solid, Mth.cos(a) * r * 0.5F, Mth.sin(a * 0.5F) * 0.15F, i * 0.18F, 0.18F - i * 0.012F, 0xFF102040, light, ov);
            cube(ps, glow, Mth.cos(a) * r * 0.5F, Mth.sin(a * 0.5F) * 0.15F + 0.12F, i * 0.18F, 0.03F, core, FULL, NO);
        }
    }

    private FrontierMobRenderers() {}
}
