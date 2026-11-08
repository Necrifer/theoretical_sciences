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
                case "karman_wraith" -> wraith(ps, trans, glow, e, partial, light, ov);   // 1.3.0
                case "storm_elemental" -> storm(ps, solid, trans, glow, e, t, light, ov);
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

    static final int[] WR_HEAD = {0xD8F0F8FF, 0x98B8D0E8}, WR_RIB = {0xB0E4F0FA, 0x6090B0D0}, WR_EDGE = {0xD0F8FCFF, 0x90C8E0F0},
            WR_EDDY = {0x90E8F4FF, 0x5098B8D8};

    static void wraith(PoseStack ps, VertexConsumer trans, VertexConsumer glow, LivingEntity e, float partial, int light, int ov) {
        float t = e.level().getGameTime() + partial;
        wraithAt(ps, trans, glow, t, (float) ((t + e.getId() * 7) / com.theoreticalsciences.frontier.KarmanWraith.PERIOD * Math.PI * 2), light, ov);
    }

    /**
     * The Kármán Wraith: the wake of something no longer there. Its head is the bluff body the wind breaks on, a rounded
     * drum of packed air with two cold eyes and a brow of streaming wisps. Behind it the wake is a tall, thin ribbon
     * (thinning and fading towards the tail) that swings from side to side in a travelling wave, wider as it goes back,
     * its upper and lower edges brighter where the shear is. At the tail it sheds eddies on alternate sides: rolled-up
     * spirals of air that turn, grow and fade as they drift away (a von Kármán vortex street).
     */
    static void wraithAt(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, float ph, int light, int ov) {
        ps.translate(0, 0.42F, 0);
        // the head: a drum lying across the flow, rounded by layers
        InnerMobRenderers.px(ps, trans, 0, 0, 5, 10, 8, 7, WR_HEAD, light, ov);
        InnerMobRenderers.px(ps, trans, 0, 0, 5, 8, 11, 7, WR_HEAD, light, ov);
        InnerMobRenderers.px(ps, trans, 0, 0, 8.6F, 8, 8, 1.6F, WR_HEAD, light, ov);
        InnerMobRenderers.px(ps, glow, -2.3F, 1.3F, 9.5F, 1.4F, 0.9F, 0.4F, new int[]{0xFFB8E8FF, 0xFF70B0E0}, FULL, NO);   // eyes
        InnerMobRenderers.px(ps, glow, 2.3F, 1.3F, 9.5F, 1.4F, 0.9F, 0.4F, new int[]{0xFFB8E8FF, 0xFF70B0E0}, FULL, NO);
        for (int k = 0; k < 3; k++) {                                                // wisps streaming back off the brow
            float w = (float) Math.sin(ph * 2 + k) * 0.6F;
            InnerMobRenderers.px(ps, trans, (k - 1) * 2.6F + w, 5.6F, 3 - k * 0.5F, 1.0F, 1.0F, 5 + k, WR_EDGE, light, ov);
        }
        // the wake: a vertical ribbon of segments, swinging in a travelling wave that grows towards the tail
        float lx = 0, ly = 0;
        for (int k = 1; k <= 14; k++) {
            float f = k / 14F, z = 2 - k * 2.4F;
            float x = (float) Math.sin(ph - k * 0.5F) * (0.6F + 6.5F * f);
            float y = (float) Math.sin(ph * 0.5F - k * 0.3F) * 0.6F;
            float h = 11F * (1 - 0.6F * f), th = 2.0F * (1 - 0.4F * f);
            int al = (int) (0xB8 * (1 - 0.55F * f));
            int[] rib = {(al << 24) | (WR_RIB[0] & 0xFFFFFF), ((al * 6 / 10) << 24) | (WR_RIB[1] & 0xFFFFFF)};
            int[] edge = {((al + 0x18) << 24) | (WR_EDGE[0] & 0xFFFFFF), ((al * 7 / 10) << 24) | (WR_EDGE[1] & 0xFFFFFF)};
            float mx = (x + lx) / 2, my = (y + ly) / 2;
            ps.pushPose();
            ps.translate(mx / 16F, my / 16F, (z + 1.2F) / 16F);
            ps.mulPose(Axis.YP.rotation((float) Math.atan2(x - lx, 2.4F)));
            InnerMobRenderers.px(ps, trans, 0, 0, 0, th, h, 3.2F, rib, light, ov);
            InnerMobRenderers.px(ps, trans, 0, h / 2, 0, th + 0.3F, 0.6F, 3.2F, edge, light, ov);   // the sheared edges, brighter
            InnerMobRenderers.px(ps, trans, 0, -h / 2, 0, th + 0.3F, 0.6F, 3.2F, edge, light, ov);
            ps.popPose();
            lx = x; ly = y;
        }
        // shed eddies: rolled spirals, alternate sides, turning, growing and fading as they drift back
        float half = com.theoreticalsciences.frontier.KarmanWraith.PERIOD / 2F;
        float tq = (float) (ph / (Math.PI * 2) * 2);                                 // half-periods elapsed
        for (int i = 0; i < 4; i++) {
            float q = ((tq % 1) + i) / 4F;
            int side = ((((int) Math.floor(tq)) - i) & 1) == 0 ? 1 : -1;
            float cz = -33 - q * 24, cx = side * (5 + 5 * q), sc = 1 + q * 1.2F;
            int al = (int) (0xA0 * (1 - q));
            int[] col = {(al << 24) | (WR_EDDY[0] & 0xFFFFFF), ((al * 6 / 10) << 24) | (WR_EDDY[1] & 0xFFFFFF)};
            for (int j = 0; j < 14; j++) {                                           // an Archimedean spiral of puffs
                double ang = j * 0.75 + t * 0.25 * side;
                float r = (0.6F + j * 0.32F) * sc;
                InnerMobRenderers.px(ps, trans, cx + (float) Math.cos(ang) * r, (float) Math.sin(j * 0.9 + t * 0.1) * 0.6F, cz + (float) Math.sin(ang) * r,
                        1.8F * sc * (1 - j / 20F), 3.6F * (1 - j / 20F), 1.8F * sc * (1 - j / 20F), col, light, ov);
            }
        }
    }

    static final int[] ST_BASE = {0xFF4E545C, 0xFF262A30}, ST_TOWER = {0xFFD8DDE3, 0xFF7C848E}, ST_TOWER_DARK = {0xFF9AA2AC, 0xFF545B64},
            ST_ANVIL = {0xFFE6EAEE, 0xFFA6AEB8}, ST_MAMMA = {0xFF8C949E, 0xFF5C636C};

    static void storm(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, LivingEntity e, float t, int light, int ov) {
        int flash = e instanceof com.theoreticalsciences.weather.StormElemental s ? s.flashTicks() : 0;
        stormAt(ps, solid, trans, glow, t, e.getId(), flash, light, ov);
    }

    /**
     * The Storm Elemental: a cumulonimbus come to life. A flat, dark, rain-free base with two slits of light for eyes;
     * rising from it, cauliflower towers of cloud, bright where the sun is on them and grey in their folds, swelling and
     * settling as the updraft breathes; at the top the anvil, flattened against the tropopause and spread downwind, with
     * mammatus pouches sagging under it. Rain hangs in a shaft below. Charge flickers deep inside, and when it strikes a
     * forked channel runs from its base to the ground and lights the whole cloud from within.
     */
    static void stormAt(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, int seed, int flash, int light, int ov) {
        ps.translate(0, 0.35F, 0);
        boolean lit = flash > 0;
        int bl = lit ? FULL : light;
        // the base: flat and dark, ragged at the edges
        InnerMobRenderers.px(ps, solid, 0, 1.5F, 0, 30, 3, 26, ST_BASE, bl, ov);
        InnerMobRenderers.px(ps, solid, 0, 1, 0, 24, 2, 32, ST_BASE, bl, ov);
        java.util.Random r = new java.util.Random(seed * 31L);
        for (int i = 0; i < 6; i++)                                                  // lumps hanging from the base
            InnerMobRenderers.px(ps, solid, (r.nextFloat() - 0.5F) * 24, -0.6F, (r.nextFloat() - 0.5F) * 24, 4 + r.nextFloat() * 3, 1.6F, 4 + r.nextFloat() * 3, ST_BASE, bl, ov);
        // eyes: two slits of light in the front of the base
        int eye = lit ? 0xFFFFFFFF : 0xFFF0E890;
        InnerMobRenderers.px(ps, glow, -5, 2, 13.2F, 5, 1.1F, 0.6F, new int[]{eye, 0xFFC0B050}, FULL, NO);
        InnerMobRenderers.px(ps, glow, 5, 2, 13.2F, 5, 1.1F, 0.6F, new int[]{eye, 0xFFC0B050}, FULL, NO);
        // the towers: stacked billows, each smaller and offset, breathing with the updraft
        float[][] towers = {{0, 0, 13}, {-9, -4, 10}, {8, 5, 10.5F}, {-6, 9, 8}, {10, -7, 8}, {-11, 6, 7}, {3, -10, 9}, {-2, 11, 7.5F}};
        for (int i = 0; i < towers.length; i++) {
            float tx = towers[i][0], tz = towers[i][1], w = towers[i][2], y = 3;
            int levels = i == 0 ? 4 : 2 + (i % 3);
            for (int k = 0; k < levels; k++) {
                float br = 1 + 0.06F * (float) Math.sin(t * 0.07F + i * 1.3F + k);
                float ww = w * (1 - k * 0.1F) * br, hh = w * 0.7F * br;
                InnerMobRenderers.px(ps, solid, tx + (k % 2 == 0 ? 0.6F : -0.6F), y + hh / 2, tz + (k % 2 == 0 ? -0.4F : 0.4F), ww, hh, ww, k == levels - 1 ? ST_TOWER : ST_TOWER_DARK, bl, ov);
                InnerMobRenderers.px(ps, solid, tx + ww * 0.35F, y + hh * 0.7F, tz, ww * 0.55F, hh * 0.55F, ww * 0.55F, ST_TOWER, bl, ov);   // a billow on its flank
                y += hh * 0.72F;
            }
        }
        // the anvil, spread flat at the top and drawn out downwind (backwards), with mammatus beneath
        float ay = 27;
        InnerMobRenderers.px(ps, solid, 0, ay - 1.5F, -2, 22, 5, 20, ST_TOWER, bl, ov);          // where the tower flattens out
        InnerMobRenderers.px(ps, solid, 0, ay + 1, -5, 34, 3, 28, ST_ANVIL, bl, ov);
        InnerMobRenderers.px(ps, solid, 0, ay + 3, -6, 24, 2, 22, ST_ANVIL, bl, ov);
        InnerMobRenderers.px(ps, solid, 0, ay + 0.5F, -19, 42, 2, 9, ST_ANVIL, bl, ov);          // drawn out downwind
        InnerMobRenderers.px(ps, solid, 0, ay, -25, 34, 1.4F, 5, ST_ANVIL, bl, ov);
        for (int i = 0; i < 9; i++) {
            float mx = ((i % 3) - 1) * 10, mz = -14 + (i / 3) * 8;
            float sag = 1.2F + 0.4F * (float) Math.sin(t * 0.05F + i);
            InnerMobRenderers.px(ps, solid, mx, ay - 2 - sag, mz, 4, 2 + sag, 4, ST_MAMMA, bl, ov);
        }
        // rain: streaks falling in a shaft under the base
        for (int i = 0; i < 16; i++) {
            float rx = ((i * 37) % 17 - 8) * 1.4F, rz = ((i * 23) % 13 - 6) * 1.6F;
            float fall = ((t * 0.9F + i * 7) % 16);
            InnerMobRenderers.px(ps, trans, rx, -1 - fall, rz, 0.4F, 2.4F, 0.4F, new int[]{0x70B8D0E8, 0x4080A0C0}, light, ov);
        }
        // charge flickering inside, seen through the gaps
        float core = 0.5F + 0.5F * (float) Math.sin(t * 0.9F + seed);
        InnerMobRenderers.px(ps, glow, 0, 10, 0, 6 + core * 2, 6, 6 + core * 2, new int[]{dim(0xF8F0A0, 0.35F + 0.3F * core) | 0xFF000000, dim(0xC0B060, 0.3F) | 0xFF000000}, FULL, NO);
        if (lit) {                                                                   // the stroke: a forked channel to the ground
            java.util.Random b = new java.util.Random(seed * 7L + flash / 3);
            float x = 0, z = 0, y = 0;
            for (int k = 0; k < 9; k++) {
                float nx = x + (b.nextFloat() - 0.5F) * 5, nz = z + (b.nextFloat() - 0.5F) * 5, ny = y - 3.2F;
                InnerMobRenderers.px(ps, glow, (x + nx) / 2, (y + ny) / 2, (z + nz) / 2, 0.8F + Math.abs(nx - x) * 0.5F, 3.4F, 0.8F + Math.abs(nz - z) * 0.5F,
                        new int[]{0xFFFFFFFF, 0xFFC8D8FF}, FULL, NO);
                if (k == 3) InnerMobRenderers.px(ps, glow, nx + 2.5F, ny - 1.5F, nz, 3.5F, 0.8F, 0.8F, new int[]{0xFFE0E8FF, 0xFFA0B0FF}, FULL, NO);
                x = nx; z = nz; y = ny;
            }
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
