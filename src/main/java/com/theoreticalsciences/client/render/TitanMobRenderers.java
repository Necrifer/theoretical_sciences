package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.titan.OuroborosBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** 1.2.0: procedural models for Titan's creatures and the Ouroboros. */
public final class TitanMobRenderers {
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
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!id.equals("ouroboros") && !DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (id) {
                case "tholin_drifter" -> drifter(ps, trans, solid, t, e.getId(), light, ov);
                case "catalyst_sprite" -> sprite(ps, solid, glow, t, light, ov);
                case "methane_skater" -> skater(ps, solid, glow, e, t, limb, amt, light, ov);
                case "cryolava_slug" -> slug(ps, solid, trans, glow, t, limb, amt, light, ov);
                case "ouroboros" -> {
                    OuroborosBoss.Serpent s = (OuroborosBoss.Serpent) e;
                    ouroboros(ps, solid, glow, trans, t, s.open(), s.rise(partial), e.deathTime > 0 ? (e.deathTime + partial) / OuroborosBoss.DEATH_TICKS : 0, light, ov);
                }
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

    static void box(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz, argb, light, ov);
    }

    /** A flake of haze: soft orange-brown puffs round a few dark grains of tar, tumbling slowly. */
    static void drifter(PoseStack ps, VertexConsumer trans, VertexConsumer solid, float t, int seed, int light, int ov) {
        java.util.Random r = new java.util.Random(seed * 17L);
        ps.translate(0, 0.35F, 0);
        ps.mulPose(Axis.YP.rotation(t * 0.01F));
        for (int i = 0; i < 9; i++) {
            float a = r.nextFloat() * 6.28F, rr = 0.1F + r.nextFloat() * 0.3F, y = (r.nextFloat() - 0.5F) * 0.4F;
            int col = (0x50 << 24) | (i % 2 == 0 ? 0xC07030 : 0xE09848);
            cube(ps, trans, Mth.cos(a) * rr, y + 0.03F * Mth.sin(t * 0.06F + i), Mth.sin(a) * rr, 0.14F + r.nextFloat() * 0.1F, col, light, ov);
        }
        for (int i = 0; i < 4; i++) cube(ps, solid, (r.nextFloat() - 0.5F) * 0.3F, (r.nextFloat() - 0.5F) * 0.3F, (r.nextFloat() - 0.5F) * 0.3F, 0.04F, 0xFF3A2010, light, ov);
    }

    /** A speck of bare metal: a bright core inside a turning hexagon of surface atoms, with a second, tilted ring. */
    static void sprite(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, 0.45F + 0.08F * Mth.sin(t * 0.15F), 0);
        float pulse = 0.5F + 0.5F * Mth.sin(t * 0.4F);
        cube(ps, glow, 0, 0, 0, 0.16F + 0.04F * pulse, 0xFFC0FFE0, FULL, NO);
        cube(ps, glow, 0, 0, 0, 0.3F + 0.06F * pulse, 0x3060FFA0, FULL, NO);
        for (int ringN = 0; ringN < 2; ringN++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(t * (ringN == 0 ? 0.08F : -0.06F)));
            ps.mulPose(Axis.XP.rotation(ringN == 0 ? 0.5F : -1.0F));
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.PI / 3;
                cube(ps, solid, Mth.cos(a) * 0.42F, 0, Mth.sin(a) * 0.42F, 0.09F, ringN == 0 ? 0xFFB8C0C8 : 0xFF9098A8, light, ov);
                cube(ps, glow, Mth.cos(a + Mth.PI / 6) * 0.42F, 0, Mth.sin(a + Mth.PI / 6) * 0.42F, 0.03F, 0xFF80FFC0, FULL, NO);
            }
            ps.popPose();
        }
    }

    /** A box from a to b, w thick: a limb segment. */
    static void seg(PoseStack ps, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float w, int argb, int light, int ov) {
        float dx = bx - ax, dy = by - ay, dz = bz - az, len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4F) return;
        ps.pushPose();
        ps.translate(ax, ay, az);
        ps.mulPose(new org.joml.Quaternionf().rotationTo(1, 0, 0, dx / len, dy / len, dz / len));
        ProcMesh.box(ps, vc, 0, -w, -w, len, w, w, argb, light, ov);
        ps.popPose();
    }

    /** How fast an entity is really moving, 0 (still) to 1 (running), measured from where it was last tick. */
    static float pace(LivingEntity e, float amt) {
        double moved = Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
        return Mth.clamp(Math.max(amt * 1.5F, (float) moved * 9F), 0F, 1F);
    }

    /**
     * A pond skater for a methane sea. Head with its beak and eyes, a long thorax, a tapering abdomen; short front legs,
     * very long middle legs, long hind legs. Each leg rises from the body to a knee and comes down to a foot.
     * <ul>
     *   <li>On ground it walks on all six in an alternating tripod - front and hind on one side with the middle on the
     *   other - each foot lifting, swinging forward and planting, stride and speed set by how fast it really moves.</li>
     *   <li>On the methane it rows: both middle legs sweep back together in a power stroke and lift forward again, the
     *   body surging on each stroke, the others steering.</li>
     *   <li>At rest it still shifts its feet, its body bobs and its antennae twitch.</li>
     * </ul>
     */
    static void skater(PoseStack ps, VertexConsumer solid, VertexConsumer glow, LivingEntity e, float t, float limb, float amt, int light, int ov) {
        float spd = pace(e, amt);
        boolean water = com.theoreticalsciences.titan.TitanMobs.MethaneSkater.methane(e.level().getFluidState(e.blockPosition()))
                || com.theoreticalsciences.titan.TitanMobs.MethaneSkater.methane(e.level().getFluidState(e.blockPosition().below()));
        float ph = limb * 1.3F + t * (0.04F + 0.32F * spd);                   // the gait's clock
        float bob = 0.02F * Mth.sin(t * 0.21F) + (water ? 0 : 0.015F * Mth.sin(ph * 2) * spd);
        float surge = water ? Math.max(0, Mth.cos(ph)) * 0.07F * (0.3F + spd) : 0;
        float bodyY = 0.34F;
        ps.translate(0, bodyY + bob, -surge);
        int dark = 0xFF2E2014, mid = 0xFF4A3420, stripe = 0xFF8A6A40, pale = 0xFFB0A890;
        // body: abdomen segments, thorax, head - each a little apart, so no two faces lie in one plane
        for (int i = 0; i < 4; i++) {
            float z = 0.22F + i * 0.13F, w = 0.105F - i * 0.022F;
            box(ps, solid, 0, -0.012F * i, z, w, 0.055F - i * 0.008F, 0.062F, i % 2 == 0 ? dark : mid, light, ov);
        }
        box(ps, solid, 0, 0.012F, -0.05F, 0.13F, 0.075F, 0.2F, mid, light, ov);
        box(ps, solid, 0, 0.092F, -0.05F, 0.032F, 0.006F, 0.17F, stripe, light, ov);
        box(ps, solid, 0.09F, 0.06F, -0.05F, 0.02F, 0.006F, 0.15F, pale, light, ov);   // the silvery hair along its sides
        box(ps, solid, -0.09F, 0.06F, -0.05F, 0.02F, 0.006F, 0.15F, pale, light, ov);
        box(ps, solid, 0, 0.035F, -0.335F, 0.078F, 0.058F, 0.085F, dark, light, ov);
        seg(ps, solid, 0, 0.0F, -0.42F, 0, -0.07F, -0.53F, 0.015F, mid, light, ov);    // the beak
        cube(ps, glow, 0.072F, 0.075F, -0.37F, 0.03F, 0xFFFFC060, FULL, NO);
        cube(ps, glow, -0.072F, 0.075F, -0.37F, 0.03F, 0xFFFFC060, FULL, NO);
        for (int sd = -1; sd <= 1; sd += 2) {
            float tw = 0.05F * Mth.sin(t * 0.5F + sd) + 0.04F * spd * Mth.sin(ph * 2 + sd);
            seg(ps, solid, sd * 0.04F, 0.075F, -0.41F, sd * 0.13F, 0.17F + tw, -0.62F, 0.011F, mid, light, ov);
        }
        float ground = -bodyY - bob + 0.02F;                                   // where the feet stand, relative to the body
        // legs: hip (x, z), resting foot (x, z), knee height, stride, and which half of the tripod it belongs to
        float[][] legs = {
                {0.10F, -0.22F, 0.30F, -0.52F, 0.14F, 0.16F, 0},   // front
                {0.13F, -0.08F, 1.00F, 0.02F, 0.30F, 0.34F, 1},    // middle (the oars)
                {0.10F, 0.14F, 0.68F, 0.92F, 0.24F, 0.26F, 0}};    // hind
        for (int sd = -1; sd <= 1; sd += 2)
            for (int k = 0; k < 3; k++) {
                float[] L = legs[k];
                float hx = sd * L[0], hz = L[1], fx = sd * L[2], fz = L[3], kneeH = L[4], stride = L[5];
                float lift;
                if (water && k == 1) {
                    // rowing: both oars together - back on the stroke, then lifted and brought forward
                    float row = Mth.sin(ph);
                    fz += row * stride * (0.5F + spd);
                    lift = Math.max(0, -Mth.cos(ph)) * 0.08F;
                } else {
                    float legPh = ph + ((L[6] == 1) == (sd > 0) ? 0 : Mth.PI);
                    float sw = Mth.sin(legPh) * stride * (0.25F + spd);
                    fz -= sw;
                    lift = Math.max(0, Mth.cos(legPh)) * (0.03F + 0.13F * spd);   // a foot swinging forward is in the air
                    fx += sd * 0.04F * Mth.cos(legPh) * spd;
                }
                float fy = ground + lift;
                float kx = hx + (fx - hx) * 0.45F, kz = hz + (fz - hz) * 0.45F, ky = kneeH + lift * 0.5F;
                seg(ps, solid, hx, 0.02F, hz, kx, ky, kz, k == 1 ? 0.022F : 0.02F, dark, light, ov);
                seg(ps, solid, kx, ky, kz, fx, fy, fz, k == 1 ? 0.018F : 0.016F, mid, light, ov);
                // the foot, lying along the surface (or the ground)
                float tx = fx + sd * (k == 1 ? 0.22F : 0.1F), tz = fz + (k == 2 ? 0.12F : k == 0 ? -0.06F : 0.06F);
                seg(ps, solid, fx, fy, fz, tx, fy - 0.005F, tz, 0.012F, stripe, light, ov);
            }
    }

    /**
     * A slug of frozen ammonia water. It moves the way slugs do: a wave of contraction runs from tail to head along its foot
     * all the time (slowly at rest, quickly when it crawls), each segment bunching up and stretching in turn, so the body
     * lengthens and shortens; its eye stalks sway, and cold blue light glints inside it.
     */
    static void slug(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        float phase = t * (0.08F + 0.25F * amt) + limb * 0.8F;
        int n = 9;
        float z = 0.62F;
        for (int i = 0; i < n; i++) {
            float k = i / (float) (n - 1);                                      // 0 tail .. 1 head
            float wave = Mth.sin(phase * 2 - i * 0.9F);
            float len = 0.13F * (1 + 0.28F * wave);                              // each segment bunches and stretches
            float taper = i < 2 ? 0.55F + 0.22F * i : i > n - 3 ? 0.9F - 0.1F * (i - n + 3) : 1F;
            float hw = 0.24F * taper * (1 - 0.12F * wave) + 0.004F * (i % 2), hh = 0.2F * taper * (1 - 0.1F * wave);
            float lift = Math.max(0, wave) * 0.03F;
            float sway = 0.035F * Mth.sin(phase * 0.5F + k * 2.5F);
            float zc = z - len / 2;
            // the body sits on its foot: the foot from the ground up, the body from just above the foot's top
            box(ps, solid, sway, 0.022F, zc, hw * 0.92F, 0.02F, len * 0.5F, 0xFF587080, light, ov);
            box(ps, solid, sway, 0.045F + hh + lift, zc, hw, hh, len * 0.5F, i % 2 == 0 ? 0xFF7890A0 : 0xFF88A0B0, light, ov);
            if (i % 2 == 1) cube(ps, glow, sway, 0.045F + hh * 2 + lift + 0.02F, zc, 0.035F + 0.01F * wave, 0xFF80D0FF, FULL, NO);
            if (i == n - 1) {
                float top = 0.045F + hh * 2 + lift;
                for (int sd = -1; sd <= 1; sd += 2) {
                    float sw = 0.06F * Mth.sin(t * 0.13F + sd * 1.7F);
                    seg(ps, solid, sway + sd * 0.07F, top - 0.04F, zc - 0.03F, sway + sd * 0.12F + sw, top + 0.22F, zc - 0.12F, 0.022F, 0xFF90A8B8, light, ov);
                    cube(ps, glow, sway + sd * 0.12F + sw, top + 0.245F, zc - 0.12F, 0.035F, 0xFFE0F8FF, FULL, NO);
                    seg(ps, solid, sway + sd * 0.08F, 0.045F + hh + lift, zc - len * 0.5F, sway + sd * 0.14F, hh * 0.6F + lift, zc - len * 0.5F - 0.12F, 0.016F, 0xFF90A8B8, light, ov);
                }
            }
            z -= len;
        }
    }

    static void ouroboros(PoseStack ps, VertexConsumer solid, VertexConsumer glow, VertexConsumer trans, float t, boolean open, float rise, float death,
                          int light, int ov) {
        ps.translate(0, 0.9F, 0);
        // summoned: it coils up out of the crater, growing, its six segments joining one by one until the ring closes
        float grow = 0.25F + 0.75F * rise;
        ps.scale(grow, grow, grow);
        ps.mulPose(Axis.YP.rotation(t * 0.02F + (1 - rise) * 6F));
        ps.mulPose(Axis.XP.rotation(0.35F + 0.1F * Mth.sin(t * 0.03F)));
        float R = 1.3F, gap = open || death > 0 ? 0.5F + 1.2F * death : 0F;
        int shown = rise >= 1 ? 6 : (int) (rise * 6.99F);
        for (int i = 0; i < 6; i++) {
            if (i >= shown) continue;
            // dying, the ring breaks: each segment flies outward, tumbling, and fades
            float out = death * death * (1.5F + 0.3F * i), spin = death * (3 + i);
            float a = i * Mth.PI / 3 + (i == 0 ? gap : 0), b = (i + 1) * Mth.PI / 3 - (i == 5 ? gap : 0);
            float mx = Mth.cos(a + 0.5F) * out, mz = Mth.sin(a + 0.5F) * out, my = death * (0.5F - i * 0.15F);
            ps.pushPose();
            ps.translate(mx, my, mz);
            if (death > 0) ps.mulPose(Axis.ZP.rotation(spin));
            float ax = Mth.cos(a) * R, az = Mth.sin(a) * R;
            int alphaMul = death > 0 ? (int) (255 * Math.max(0, 1 - death)) : 255;
            VertexConsumer body = death > 0.3F ? trans : solid;
            cube(ps, body, ax, 0, az, i == 0 ? 0.3F : 0.24F, (alphaMul << 24) | (i == 0 ? 0x7A4A20 : 0x603818), light, ov);
            for (int k = 1; k < 5; k++) {
                float f = k / 5F, x = Mth.lerp(f, ax, Mth.cos(b) * R), z = Mth.lerp(f, az, Mth.sin(b) * R);
                cube(ps, body, x, 0.02F * Mth.sin(t * 0.2F + k + i), z, 0.15F, (alphaMul << 24) | 0x4A2A10, light, ov);
                if (i % 2 == 0) cube(ps, glow, x * 0.82F, 0.05F, z * 0.82F, 0.05F, 0xFFFFD060, FULL, NO);
            }
            ps.popPose();
        }
        if (death > 0.6F) return;
        float hx = Mth.cos(gap) * R, hz = Mth.sin(gap) * R;
        int eye = open || death > 0 ? 0xFFFF4020 : 0xFFFFE080;
        cube(ps, glow, hx + 0.12F, 0.2F, hz, 0.06F, eye, FULL, NO);
        cube(ps, glow, hx - 0.12F, 0.2F, hz, 0.06F, eye, FULL, NO);
        if (!open && death == 0 && rise >= 1)
            for (int i = 0; i < 24; i++) {
                float a = i * Mth.TWO_PI / 24 + t * 0.05F;
                cube(ps, glow, Mth.cos(a) * R * 0.55F, 0, Mth.sin(a) * R * 0.55F, 0.035F, 0x80FFE0A0, FULL, NO);
            }
    }
}
