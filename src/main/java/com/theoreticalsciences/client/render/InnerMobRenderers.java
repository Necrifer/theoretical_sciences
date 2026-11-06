package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.inner.InnerMobs;
import com.theoreticalsciences.inner.MirrorMicrobe;
import com.theoreticalsciences.inner.TumourHydra;
import com.theoreticalsciences.ocean.OceanMobs;
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
 * 1.2.0: the creatures of Inner Space and Titan's hidden ocean, in the game's own style: chunky shaded blocks laid out on
 * a sixteen-pixel grid, as vanilla's mobs are, with a lit top and a darker underside on every box. Each model faces +z
 * (its head) and is turned to the way the creature is going.
 */
public final class InnerMobRenderers {
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
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(QHEntityRenderers.WHITE));
            VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucent(QHEntityRenderers.WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(QHEntityRenderers.WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            ps.pushPose();
            // the models face +z: turn them to the body's heading (the entity looks along (-sin yaw, cos yaw))
            ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            float pitch = e.getViewXRot(partial);
            switch (e) {
                case InnerMobs.RedCell c -> redCell(ps, solid, t, light, ov);
                case InnerMobs.Macrophage m -> macrophage(ps, trans, solid, t, amt, limb, m.full(), light, ov);
                case InnerMobs.TCell c -> tCell(ps, solid, glow, t, limb, amt, light, ov);
                case InnerMobs.Polymerase p -> polymerase(ps, solid, glow, t, limb, amt, p.working(), light, ov);
                case TumourHydra.Hydra h -> hydra(ps, solid, glow, t, limb, amt, h, h.stumpSlot(), h.rise(partial), h.bitingHead(), h.biteProgress(partial), light, ov);
                case MirrorMicrobe m -> mirrorMicrobe(ps, solid, glow, t, light, ov);
                case OceanMobs.VentGrazer g -> grazer(ps, solid, glow, t, pitch, light, ov);
                case OceanMobs.IceEel el -> eel(ps, solid, glow, t, pitch, light, ov);
                case OceanMobs.BrineJelly j -> jelly(ps, trans, glow, t, OceanMobs.BrineJelly.stroke(e.level(), e.getId(), partial), e, partial, light, ov);
                case OceanMobs.VentCrab c -> crab(ps, solid, t, limb, amt, light, ov);
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

    /** A box by its centre and its size, both in pixels (a sixteenth of a block), shaded from top to foot. */
    static void px(PoseStack ps, VertexConsumer vc, float x, float y, float z, float w, float h, float d, int[] col, int light, int ov) {
        MathMobRenderers.cbox(ps, vc, x / 16F, y / 16F, z / 16F, w / 32F, h / 32F, d / 32F, col, light, ov);
    }

    static int[] c(int top, int bot) {
        return new int[]{top, bot};
    }

    // ================================================================== Inner Space
    static final int[] RC = c(0xFFE0404A, 0xFF901C26), RC_DIMPLE = c(0xFFB02232, 0xFF701018), RC_RIM = c(0xFFF06068, 0xFFA02830);

    /** The Red Cell: a fat ring of a rim round a sunken middle, tumbling slowly as it drifts. */
    static void redCell(PoseStack ps, VertexConsumer vc, float t, int light, int ov) {
        ps.translate(0, 4 / 16F, 0);
        ps.mulPose(Axis.XP.rotation(0.3F * Mth.sin(t * 0.03F)));
        ps.mulPose(Axis.ZP.rotation(0.2F * Mth.sin(t * 0.021F + 1)));
        px(ps, vc, 0, 0, 0, 12, 2, 12, RC_DIMPLE, light, ov);
        px(ps, vc, 0, 0, 7, 12, 5, 3, RC, light, ov);
        px(ps, vc, 0, 0, -7, 12, 5, 3, RC, light, ov);
        px(ps, vc, 7, 0, 0, 3, 5, 12, RC, light, ov);
        px(ps, vc, -7, 0, 0, 3, 5, 12, RC, light, ov);
        for (int sx = -1; sx <= 1; sx += 2)                                   // the rim's rounded corners
            for (int sz = -1; sz <= 1; sz += 2) px(ps, vc, sx * 5.5F, 0, sz * 5.5F, 3, 4, 3, RC_RIM, light, ov);
    }

    static final int[] MEM = c(0xB0F4E6DC, 0x90C8B0A4), NUC = c(0xFF8A60C0, 0xFF4A2C78), GRAN = c(0xFFE8C860, 0xFFA07830),
            MEAL = c(0xFFD03844, 0xFF701020);

    /** The Macrophage: a big pale cell you can see into (a purple nucleus, yellow granules), arms of membrane reaching out and back. */
    static void macrophage(PoseStack ps, VertexConsumer trans, VertexConsumer solid, float t, float amt, float limb, boolean full, int light, int ov) {
        float s = full ? 1.2F : 1F;
        ps.translate(0, 10 / 16F * s, 0);
        ps.scale(s, s, s);
        px(ps, solid, 2, 1, 1, 7, 5, 5, NUC, light, ov);
        px(ps, solid, -2, 0, -1, 6, 4, 5, NUC, light, ov);
        for (int k = 0; k < 6; k++) px(ps, solid, 6 * Mth.cos(k * 2.4F), 3 * Mth.sin(k * 1.7F), 6 * Mth.sin(k * 2.4F), 2, 2, 2, GRAN, light, ov);
        if (full) px(ps, solid, -2, 3, 4, 5, 4, 4, MEAL, light, ov);
        float b = 1 + 0.04F * Mth.sin(t * 0.12F);
        px(ps, trans, 0, 0, 0, 20 * b, 14, 18 * b, MEM, light, ov);
        px(ps, trans, 0, 8, 0, 14, 3, 12, MEM, light, ov);
        px(ps, trans, 0, -8, 0, 16, 3, 14, MEM, light, ov);
        for (int k = 0; k < 4; k++) {                                         // pseudopods, each reaching out in its own time
            float reach = 4 + 5 * (0.5F + 0.5F * Mth.sin(t * 0.06F + k * 1.7F)) + 2 * amt * Mth.sin(limb * 0.5F + k);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(k * Mth.HALF_PI + 0.4F));
            px(ps, trans, 0, -5, 10 + reach / 2, 5, 4, reach, MEM, light, ov);
            px(ps, trans, 0, -6, 10 + reach, 7, 3, 3, MEM, light, ov);
            ps.popPose();
        }
    }

    static final int[] TC = c(0xFFD4E2FF, 0xFF7088C0), TC_DARK = c(0xFFA8BCE8, 0xFF586C9C), TCR = c(0xFFA0F4FF, 0xFF50C0FF);

    /** The T Cell: a round blue-white cell, a glowing receptor on every face, a stubby tail (uropod) behind as it crawls. */
    static void tCell(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, int light, int ov) {
        ps.translate(0, (7 + 1.2F * amt * Math.abs(Mth.sin(limb * 0.6F))) / 16F, 0);
        float sq = 0.06F * Mth.sin(t * 0.2F);
        ps.scale(1 + sq, 1 - sq, 1 + sq);
        ps.mulPose(Axis.YP.rotation(0.15F * Mth.sin(t * 0.07F)));
        px(ps, solid, 0, 0, 0, 12, 10, 12, TC, light, ov);
        px(ps, solid, 0, 0, 0, 10, 12, 10, TC, light, ov);
        px(ps, solid, 0, 0, 0, 14, 8, 10, TC_DARK, light, ov);
        px(ps, solid, 0, -2, -8, 5, 5, 4, TC_DARK, light, ov);
        px(ps, solid, 0, -3, -11, 3, 3, 3, TC_DARK, light, ov);
        float w = 0.5F * Mth.sin(t * 0.3F);
        for (int[] f : new int[][]{{0, 7, 0}, {7, 2, 3}, {-7, 2, -3}, {3, 3, 7}, {-4, 2, 7}, {7, -1, -3}, {-7, -2, 3}, {0, -1, 7}, {3, 5, -5}, {-3, 5, -5}, {5, 6, 3}, {-5, 6, 2}})
            px(ps, glow, f[0] * (1 + w * 0.05F), f[1], f[2], 2, 2, 2, TCR, FULL, ov);
    }

    static final int[] PALM = c(0xFF9CD4BC, 0xFF4E8270), FINGER = c(0xFFAEE0C8, 0xFF5E9480), DNA_A = c(0xFF5080F0, 0xFF3050B0),
            DNA_B = c(0xFF50D090, 0xFF309060), RUNG = c(0xFFFFE890, 0xFFFFA860), RNA = c(0xFFFFB050, 0xFFFF7030);

    /**
     * The Polymerase: an enzyme shaped like a cupped hand closing on the DNA. The palm under it and the fingers and thumb
     * arching over it make a clamp; the double helix slides through, its rungs lit, and the new strand it writes trails out behind.
     */
    static void polymerase(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, boolean working, int light, int ov) {
        ps.translate(0, (8 + 0.6F * Mth.sin(t * 0.1F)) / 16F, 0);
        ps.mulPose(Axis.ZP.rotation(0.12F * Mth.sin(t * 0.13F)));
        float slide = t * (working ? 0.6F : 0.15F);
        for (int i = -7; i <= 7; i++) {                                       // the helix: two strands and their rungs, turning
            float z = i * 1.6F, a = (i + slide) * 0.65F;
            float x = 2.5F * Mth.cos(a), y = 2.5F * Mth.sin(a);
            px(ps, solid, x, y, z, 1.6F, 1.6F, 1.8F, DNA_A, light, ov);
            px(ps, solid, -x, -y, z, 1.6F, 1.6F, 1.8F, DNA_B, light, ov);
            if ((i & 1) == 0) px(ps, glow, 0, 0, z, Math.abs(x) * 2 + 0.6F, Math.abs(y) * 2 + 0.6F, 0.8F, RUNG, FULL, ov);
        }
        float close = working ? -1.2F + 0.5F * Mth.sin(t * 0.6F) : 1.2F * Mth.sin(t * 0.12F);
        px(ps, solid, 0, -5, 0, 12, 4, 12, PALM, light, ov);               // the palm
        px(ps, solid, 0, -7.5F, 0, 10, 2, 10, PALM, light, ov);
        px(ps, solid, -6, -1, 1, 3, 8, 8, FINGER, light, ov);              // the fingers rising on one side
        px(ps, solid, -4, 4 + close, 1, 5, 3, 8, FINGER, light, ov);       // and curling over
        px(ps, solid, 6, -1, -1, 3, 7, 6, FINGER, light, ov);              // the thumb on the other
        px(ps, solid, 4.5F, 3.5F - close, -1, 4, 3, 6, FINGER, light, ov);
        for (int i = 0; i < 7; i++)                                         // the new RNA, written out of its back
            px(ps, glow, 1.6F * Mth.sin(t * 0.2F + i * 0.8F), 2 + i * 0.7F, -7 - i * 1.4F, 1.4F, 1.4F, 1.4F, RNA, FULL, ov);
    }

    static final int[] FLESH = c(0xFFDC6C84, 0xFF8A2A44), FLESH_D = c(0xFFC0506A, 0xFF6A1A30), FLESH_L = c(0xFFEC8CA0, 0xFFA04058),
            EYE = c(0xFFFFF070, 0xFFFFC020), TOOTH = c(0xFFFFF6E8, 0xFFD8CCB4), WOUND = c(0xFFFF3848, 0xFFA00818), NODULE = c(0xFFFF98B8, 0xFFC04070);

    /**
     * The Tumour Hydra: a mound of overgrown tissue as wide as a house, lumps budding on lumps; its heads grow out of every
     * side on long swaying necks, one for each it has. An open stump bleeds.
     */
    static void hydra(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, TumourHydra.Hydra hyd, int stumpSlot, float rise,
                      int biting, float bite, int light, int ov) {
        ps.translate(0, -4.5F * (1 - rise), 0);
        float bob = 1.5F * amt * Mth.sin(limb * 0.4F) + 1.2F * Mth.sin(t * 0.09F), pulse = 1 + 0.07F * Mth.sin(t * 0.11F);
        px(ps, solid, 0, 30 + bob, 0, 64 * pulse, 44, 58 * pulse, FLESH, light, ov);
        px(ps, solid, 0, 30 + bob, 0, 54, 54, 48, FLESH_D, light, ov);
        px(ps, solid, 4, 58 + bob, -3, 36, 14, 32, FLESH_L, light, ov);
        float[][] lumps = {{26, 14, 20, 18}, {-24, 16, -16, 20}, {14, 46, -14, 16}, {-16, 44, 16, 14}, {4, 10, 30, 14}, {-6, 10, -30, 16},
                {30, 12, -14, 12}, {-30, 10, 12, 12}};
        for (int q = 0; q < lumps.length; q++) {
            float[] l = lumps[q];
            float th = 1 + 0.12F * Mth.sin(t * 0.15F + q * 1.1F);             // each lump throbs in its own time
            px(ps, solid, l[0], l[1] + bob, l[2], l[3] * th, l[3] * 0.9F * th, l[3] * th, FLESH_L, light, ov);
        }
        for (int k = 0; k < 10; k++)
            px(ps, glow, 33 * Mth.cos(k * 2.3F), 18 + (k % 3) * 11 + bob, 30 * Mth.sin(k * 2.3F), 3, 3, 3, NODULE, FULL, ov);
        for (int k = 0; k < 9; k++) {
            // each head has its own place round the body; a cut one leaves its stump in that same place
            boolean isStump = k == stumpSlot;
            if (!isStump && !hyd.alive(k)) continue;
            // where the head rests is shared with its hit box (Hydra.headLocal), so you strike what you see
            float[] g = TumourHydra.Hydra.headLocal(k, t);
            float a = g[6], ca = Mth.cos(a), sa = Mth.sin(a);
            // the biting head draws back, then strikes out and down (synced from the server's bite)
            float lunge = k == biting ? (bite < 0.35F ? -0.8F * bite / 0.35F : -0.8F + 2.8F * Mth.sin((bite - 0.35F) / 0.65F * Mth.PI)) : 0;
            float bx = g[0], bz = g[2], by = g[1] + bob / 16F;
            if (isStump) {
                // the cut neck stays standing, out to where the head was struck off, and bleeds at its end
                for (int j = 0; j <= 5; j++) {
                    float[] q = TumourHydra.Hydra.neckLocal(k, j * 0.13F, t);
                    px(ps, solid, q[0] * 16, (q[1] + bob / 16F) * 16, q[2] * 16, 12 - j * 0.4F, 12 - j * 0.4F, 12 - j * 0.4F, (j & 1) == 0 ? FLESH : FLESH_D, light, ov);
                }
                float[] e = TumourHydra.Hydra.neckLocal(k, 0.75F, t);
                px(ps, glow, e[0] * 16, (e[1] + bob / 16F) * 16, e[2] * 16, 11, 11, 11, WOUND, FULL, ov);
                continue;
            }
            float hx = g[3] + ca * lunge, hy = g[4] + bob / 16F - Math.max(0, lunge) * 0.7F, hz = g[5] + sa * lunge;
            for (int j = 0; j <= 10; j++) {                                  // the neck: a run of blocks curving out, then up
                float u = j / 10F, iu = 1 - u;
                float mx = bx + ca * 2.4F, my = by + 0.2F, mz = bz + sa * 2.4F;
                float x = iu * iu * bx + 2 * iu * u * mx + u * u * hx, y = iu * iu * by + 2 * iu * u * my + u * u * hy, z = iu * iu * bz + 2 * iu * u * mz + u * u * hz;
                float w = 12 - 4 * u, wr = 0.35F * Mth.sin(t * 0.2F + k * 1.7F - j * 0.6F) * Mth.sin(u * Mth.PI);   // a ripple runs up the neck
                px(ps, solid, (x + sa * wr) * 16, (y + 0.5F * wr) * 16, (z - ca * wr) * 16, w, w, w, (j & 1) == 0 ? FLESH : FLESH_D, light, ov);
            }
            ps.pushPose();
            ps.translate(hx, hy, hz);
            ps.mulPose(Axis.YP.rotation(-a + Mth.HALF_PI));
            float jaw = 1.5F + 3.5F * Math.max(Math.max(0, lunge) / 2.0F, 0.3F + 0.3F * Mth.sin(t * 0.25F + k));
            ps.mulPose(Axis.XP.rotation(0.25F * Mth.sin(t * 0.1F + k) + Math.max(0, lunge) * 0.35F));
            ps.mulPose(Axis.ZP.rotation(0.2F * Mth.sin(t * 0.08F + k * 2)));
            px(ps, solid, 0, 2, 3, 14, 10, 16, FLESH, light, ov);              // skull and snout
            px(ps, solid, 0, 1, 12, 10, 7, 6, FLESH_L, light, ov);
            px(ps, solid, 0, -5 - jaw, 6, 12, 3, 16, FLESH_D, light, ov);     // the jaw, opening and shutting
            for (int s = -1; s <= 1; s += 2) {
                px(ps, glow, s * 4.5F, 5, 10, 2.5F, 2.5F, 2.5F, EYE, FULL, ov);
                for (int tt = 0; tt < 3; tt++) px(ps, solid, s * (2 + tt * 1.6F), -2.5F - jaw * 0.5F, 13 - tt * 2.5F, 1.2F, 2.5F + jaw * 0.6F, 1.2F, TOOTH, light, ov);
            }
            ps.popPose();
        }
    }

    static final int[] MIR = c(0xFFF4FCFF, 0xFF9CC8D8), MIR_D = c(0xFFD8EEF6, 0xFF80B0C0), FLAG = c(0xFF90F4FF, 0xFF40B0D0);

    /** The Mirror Microbe: a pale glassy rod whose four flagella all spiral left-handed, the mirror of a living cell's. */
    static void mirrorMicrobe(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, int light, int ov) {
        ps.translate(0, (6 + Mth.sin(t * 0.1F)) / 16F, 0);
        ps.mulPose(Axis.ZP.rotation(-t * 0.06F));
        px(ps, solid, 0, 0, 0, 6, 6, 10, MIR, light, ov);
        px(ps, solid, 0, 0, 0, 4, 4, 12, MIR_D, light, ov);
        for (int k = 0; k < 4; k++)
            for (int i = 0; i < 8; i++) {
                float a = -i * 0.9F - t * 0.4F + k * Mth.HALF_PI, r = 1.2F + 0.2F * i;
                px(ps, glow, Mth.cos(a) * r, Mth.sin(a) * r, -6 - i * 1.2F, 1, 1, 1.2F, FLAG, FULL, ov);
            }
    }

    // ================================================================== Titan's hidden ocean
    static final int[] SHELL = c(0xFFF8E6D8, 0xFFC09482), SHELL_D = c(0xFFE8C8B4, 0xFFA87862), LEG = c(0xFFFFB494, 0xFFD06C50), BLACK = c(0xFF202020, 0xFF080808);

    /** The Vent Grazer: a pale shrimp, its arched back in shell segments, feelers forward, paddling legs, a flicking tail fan. */
    static void grazer(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float pitch, int light, int ov) {
        ps.translate(0, 4 / 16F, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        float flick = 15 * Mth.sin(t * 0.25F);
        px(ps, solid, 0, 1, 5, 5, 4, 4, SHELL, light, ov);                  // the head
        px(ps, solid, 0, 0.5F, 8, 2, 2, 2, SHELL_D, light, ov);              // the rostrum
        for (int s = -1; s <= 1; s += 2) {
            px(ps, solid, s * 2, 2.5F, 6, 1, 1, 1, BLACK, light, ov);       // eyes
            for (int i = 0; i < 5; i++) px(ps, solid, s * (1 + i * 0.8F), 2 + i * 0.4F + 0.4F * Mth.sin(t * 0.1F + i), 8 + i * 1.5F, 1, 1, 1.5F, LEG, light, ov);
        }
        for (int k = 0; k < 5; k++) {
            float z = 2 - k * 2.2F, y = 1.5F - k * k * 0.15F, w = 5 - k * 0.6F;
            px(ps, solid, 0, y, z, w, w - 1, 2.4F, (k & 1) == 0 ? SHELL : SHELL_D, light, ov);
            for (int s = -1; s <= 1; s += 2)                                 // swimmerets, paddling in a wave
                px(ps, solid, s * (w / 2 - 0.5F), y - w / 2 - 0.5F + 0.4F * Mth.sin(t * 0.9F + k * 0.8F), z, 1, 2, 1, LEG, light, ov);
        }
        ps.pushPose();
        ps.translate(0, -1 / 16F, -9 / 16F);
        ps.mulPose(Axis.XP.rotationDegrees(flick));
        px(ps, solid, 0, 0, -1.5F, 6, 1, 3, LEG, light, ov);
        ps.popPose();
    }

    static final int[] EEL = c(0xFF3A6478, 0xFF14283A), EEL_B = c(0xFF7C949C, 0xFF3C5058), PHOTO = c(0xFF98FFF2, 0xFF40E0C0);

    /** The Ice Eel: a long body that ripples from head to tail as it swims, rows of glowing photophores, a jaw and a lure. */
    static void eel(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float pitch, int light, int ov) {
        ps.translate(0, 4 / 16F, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        int n = 10;
        for (int i = 0; i < n; i++) {
            float z = 8 - i * 3.4F, x = 3 * Mth.sin(t * 0.25F - i * 0.6F) * (i / (float) n), w = 6 - i * 0.45F;
            px(ps, solid, x, 0.5F, z, w, w * 0.8F, 3.6F, EEL, light, ov);
            px(ps, solid, x, -w * 0.35F, z, w * 0.8F, 1.5F, 3.4F, EEL_B, light, ov);
            if ((i & 1) == 1) for (int s = -1; s <= 1; s += 2) px(ps, glow, x + s * (w / 2 + 0.3F), 0, z, 1, 1, 1, PHOTO, FULL, ov);
        }
        float tail = 3 * Mth.sin(t * 0.25F - n * 0.6F);
        px(ps, solid, tail, 0.5F, 8 - n * 3.4F, 1, 4, 3, EEL, light, ov);  // the tail fin
        float jaw = 1.2F * (0.5F + 0.5F * Mth.sin(t * 0.3F));
        px(ps, solid, 0, 1, 12, 7, 5, 6, EEL, light, ov);                     // head
        px(ps, solid, 0, -2 - jaw, 12.5F, 6, 1.5F, 5, EEL_B, light, ov);    // jaw
        for (int s = -1; s <= 1; s += 2) px(ps, glow, s * 2.5F, 2, 14, 1, 1, 1, PHOTO, FULL, ov);
        px(ps, solid, 0, 4.5F, 13, 1, 3, 1, EEL, light, ov);                 // the lure's stalk
        px(ps, solid, 0, 6, 15, 1, 1, 3, EEL, light, ov);
        px(ps, glow, 0, 6, 17.5F, 2, 2, 2, PHOTO, FULL, ov);
    }

    static final int[] BELL = c(0x90C4F0FF, 0x6080C0E0), BELL_RIM = c(0xA0E0F8FF, 0x7090D0E8), GONAD = c(0xFFFF88D8, 0xFFC040A0),
            TENT = c(0xFF98ECFF, 0xFF4090C0), ARM = c(0xB0F4C4E8, 0x80C080C0);

    /**
     * The Brine Jelly: a clear bell with a glowing ring inside, frilled arms and long tentacles. It swims bell-first: on
     * each stroke the bell snaps narrow and tall, driving it forward, and the tentacles stream out straight behind; as it
     * coasts the bell opens wide again and the tentacles drift and curl.
     */
    static void jelly(PoseStack ps, VertexConsumer trans, VertexConsumer glow, float t, float stroke, LivingEntity e, float partial, int light, int ov) {
        // the stroke: a fast close (the first fifth), then a slow open
        float close = stroke < 0.2F ? Mth.sin(stroke / 0.2F * Mth.HALF_PI) : 1 - (stroke - 0.2F) / 0.8F;
        float wide = 1 - 0.35F * close, tall = 1 + 0.3F * close;
        // bell-first along its motion: tip the bell over toward where it is going
        var v = e.getDeltaMovement();
        float hs = (float) Math.sqrt(v.x * v.x + v.z * v.z);
        float tilt = (float) Math.min(1.1, Math.atan2(hs, Math.max(0.02, v.y + 0.04)));
        ps.translate(0, 14 / 16F, 0);
        ps.mulPose(Axis.XP.rotation(tilt));
        px(ps, trans, 0, 3 * tall, 0, 14 * wide, 4 * tall, 14 * wide, BELL, light, ov);
        px(ps, trans, 0, 6.5F * tall, 0, 10 * wide, 3 * tall, 10 * wide, BELL, light, ov);
        px(ps, trans, 0, 8.5F * tall, 0, 6 * wide, 2, 6 * wide, BELL, light, ov);
        px(ps, trans, 0, 0.5F, 0, 16 * wide, 1.5F, 16 * wide, BELL_RIM, light, ov);
        px(ps, glow, 0, 3 * tall, 0, 6 * wide, 1.5F, 6 * wide, GONAD, FULL, ov);
        // tentacles from the rim: straight back on the stroke, curling as it coasts
        float trail = 0.4F + 0.6F * close;
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8, rx = Mth.cos(a) * 7 * wide, rz = Mth.sin(a) * 7 * wide;
            for (int i = 1; i <= 9; i++) {
                float curl = (1 - trail) * 2.2F * Mth.sin(t * 0.08F + k + i * 0.5F) * i * 0.25F;
                px(ps, glow, rx * (1 - 0.03F * i) + curl, -i * 2.2F * (0.6F + 0.4F * trail), rz * (1 - 0.03F * i) + curl * 0.6F, 0.8F, 2.2F, 0.8F, TENT, FULL, ov);
            }
        }
        for (int k = 0; k < 4; k++) {                                        // the frilled oral arms in the middle
            float a = k * Mth.HALF_PI + 0.4F;
            for (int i = 1; i <= 5; i++)
                px(ps, trans, Mth.cos(a) * 1.5F + 0.6F * Mth.sin(t * 0.07F + i + k), -i * 1.8F, Mth.sin(a) * 1.5F, 2.4F - i * 0.25F, 1.8F, 2.4F - i * 0.25F, ARM, light, ov);
        }
    }

    static final int[] CRAB = c(0xFFF6F0E6, 0xFFB4A494), CRAB_D = c(0xFFE0D6C8, 0xFF988878), HAIR = c(0xFFD0C8B8, 0xFF8C8474);

    /** The Vent Crab: a pale yeti crab scuttling on jointed legs, waving and pinching hairy claws (it farms bacteria on them). */
    static void crab(PoseStack ps, VertexConsumer solid, float t, float limb, float amt, int light, int ov) {
        // steps show only while it is really walking, eased in and out (a drift in the current does not set the legs twitching)
        float g = Mth.clamp((amt - 0.04F) / 0.25F, 0F, 1F), step = g * g * (3 - 2 * g);
        ps.translate(0, (5 + 0.4F * step * Math.abs(Mth.sin(limb * 1.2F))) / 16F, 0);
        px(ps, solid, 0, 0, 0, 12, 4, 10, CRAB, light, ov);
        px(ps, solid, 0, 2.5F, 0, 9, 2, 7, CRAB_D, light, ov);
        for (int k = 0; k < 3; k++)
            for (int s = -1; s <= 1; s += 2) {
                // alternate legs step in turn; standing, the legs are still (one now and then lifts and settles, slowly)
                float ph = limb * 1.2F + k * 2.1F + (s > 0 ? Mth.PI : 0);
                float z = 3 - k * 3, rest = Math.max(0, Mth.sin(t * 0.03F + k * 2.4F + s * 1.1F) - 0.85F) * 4F * (1 - step);
                float up = 1.4F * step * Math.max(0, Mth.sin(ph)) + rest, fw = 1.2F * step * Mth.cos(ph);
                px(ps, solid, s * 6.5F, 0.5F + up, z + fw * 0.5F, 3, 2, 2, CRAB_D, light, ov);       // upper leg, out sideways
                px(ps, solid, s * 8.5F, -2 + up, z + fw, 2, 5, 2, CRAB, light, ov);                  // lower leg, down to the floor
            }
        for (int s = -1; s <= 1; s += 2) {
            float wave = 1.2F * Mth.sin(t * 0.04F + s * 1.3F);                  // the claws wave to stir the water over the bacteria
            float pinch = 1.0F * Math.max(0, Mth.sin(t * 0.07F + s));           // and snip open and shut
            px(ps, solid, s * 4, 1 + wave * 0.5F, 7, 3, 3, 5, CRAB_D, light, ov);                 // arm
            px(ps, solid, s * 4.5F, 1.5F + wave + pinch * 0.5F, 11.5F, 5, 3, 5, CRAB, light, ov); // upper pincer
            px(ps, solid, s * 4.5F, -0.6F + wave - pinch * 0.5F, 12, 4, 1.5F, 4, CRAB_D, light, ov); // lower pincer
            for (int h = 0; h < 4; h++)                                         // the fur, swaying
                px(ps, solid, s * (6.5F + (h & 1)) + 0.4F * Mth.sin(t * 0.1F + h), -0.5F + wave - h * 0.3F, 9.5F + h * 1.2F, 1, 3, 1, HAIR, light, ov);
            float swivel = 0.6F * Mth.sin(t * 0.09F + s * 2);
            px(ps, solid, s * 2 + swivel * 0.3F, 4, 5, 1, 3, 1, CRAB_D, light, ov);              // eye stalks, looking about
            px(ps, solid, s * 2 + swivel, 6, 5 + swivel * 0.4F, 1.5F, 1.5F, 1.5F, BLACK, light, ov);
        }
    }

    private InnerMobRenderers() {}
}
