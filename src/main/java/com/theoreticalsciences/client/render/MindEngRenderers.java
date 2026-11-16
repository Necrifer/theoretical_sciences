package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.engineering.RustMite;
import com.theoreticalsciences.mind.DreamingColony;
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
 * 1.4.0: the creatures of Mind and Language and Engineering of the Large, built of shaded boxes in the game's own style
 * (a lighter top, a darker underside), with parts that move.
 */
public final class MindEngRenderers {
    private static final int FULL = 0xF000F0;

    public static class Mob<T extends LivingEntity> extends EntityRenderer<T> {
        public Mob(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.45F;
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
            ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (e) {
                case DreamingColony c -> colony(ps, solid, trans, glow, t, c.pulseAge + partial, e.getId(), light, ov);
                case RustMite m -> mite(ps, solid, glow, t, limb, amt, m.feedAnim > 0 ? m.feedAnim - partial : 0, light, ov);
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

    static void px(PoseStack ps, VertexConsumer vc, float x, float y, float z, float w, float h, float d, int[] col, int light, int ov) {
        MathMobRenderers.cbox(ps, vc, x / 16F, y / 16F, z / 16F, w / 32F, h / 32F, d / 32F, col, light, ov);
    }

    static int[] c(int top, int bot) {
        return new int[]{top, bot};
    }

    static int lerp(int a, int b, float f) {
        f = Mth.clamp(f, 0, 1);
        int ca = (a >>> 24) & 255, ra = (a >> 16) & 255, ga = (a >> 8) & 255, ba = a & 255;
        int cb = (b >>> 24) & 255, rb = (b >> 16) & 255, gb = (b >> 8) & 255, bb = b & 255;
        return ((int) (ca + (cb - ca) * f) << 24) | ((int) (ra + (rb - ra) * f) << 16) | ((int) (ga + (gb - ga) * f) << 8) | (int) (ba + (bb - ba) * f);
    }

    // ================================================================== the Dreaming Colony
    static final int[] SAC = c(0x90EADCF6, 0x70B49CD4), LOBE = c(0x88E0D0F4, 0x68A890CC), TENDRIL = c(0xFFD2BCE0, 0xFF8A70A8),
            NUCLEUS = c(0xFF6A4890, 0xFF3A2458);
    static final int SOMA_DIM = 0xFFA070E0, SOMA_LIT = 0xFFFFE8FF, TIP = 0xFFFFC0F0;

    /**
     * A knot of neurons in one membrane: a pale sac with dark nuclei inside, cell bodies lighting up one after another as
     * a thought runs round them, three smaller lobes turning slowly about it, and tendrils of dendrite trailing below,
     * swaying, tipped with light. When it dreams aloud (a pulse) it all flares, and a ring of light spreads out.
     */
    static void colony(PoseStack ps, VertexConsumer solid, VertexConsumer trans, VertexConsumer glow, float t, float pulse, int seed, int light, int ov) {
        float flare = pulse < 20 ? 1 - pulse / 20F : 0;
        float bob = Mth.sin(t * 0.06F) * 1.2F;
        ps.pushPose();
        ps.translate(0, (12 + bob) / 16F, 0);
        // the tendrils, hanging and swaying
        for (int k = 0; k < 5; k++) {
            float a = k * 1.2566F + seed;
            float bx = Mth.cos(a) * 4, bz = Mth.sin(a) * 4;
            for (int s = 0; s < 6; s++) {
                float sway = Mth.sin(t * 0.08F + k + s * 0.6F) * (s * 0.7F);
                px(ps, solid, bx + sway * Mth.cos(a + 1.5F), -6 - s * 2.2F, bz + sway * Mth.sin(a + 1.5F), 2 - s * 0.15F, 2.4F, 2 - s * 0.15F, TENDRIL, light, ov);
            }
            float sway = Mth.sin(t * 0.08F + k + 3.6F) * 4.2F;
            int tip = lerp(TIP, 0xFFFFFFFF, flare);
            px(ps, glow, bx + sway * Mth.cos(a + 1.5F), -6 - 6 * 2.2F, bz + sway * Mth.sin(a + 1.5F), 1.8F, 1.8F, 1.8F, c(tip, tip), FULL, ov);
        }
        // the nuclei, dark, inside
        px(ps, solid, -2, 1, 1, 3, 3, 3, NUCLEUS, light, ov);
        px(ps, solid, 2.5F, -1.5F, -1, 3, 3, 3, NUCLEUS, light, ov);
        px(ps, solid, 0.5F, 2.5F, -2.5F, 2.5F, 2.5F, 2.5F, NUCLEUS, light, ov);
        // the cell bodies on the surface, lit in turn as the signal goes round
        float[][] somas = {{0, 6.2F, 0}, {5.8F, 2, 2}, {-5.6F, 1, -2}, {2, -4, 5.6F}, {-2, 3, 5.8F}, {3, 3, -5.8F}, {-3, -3, -5.6F}, {5.6F, -3, -3}};
        for (int i = 0; i < somas.length; i++) {
            float phase = (t * 0.12F - i * 0.8F) % (somas.length * 0.8F);
            float lit = Math.max(flare, phase >= 0 && phase < 1.2F ? 1 - Math.abs(phase - 0.6F) / 0.6F : 0);
            int col = lerp(SOMA_DIM, SOMA_LIT, lit);
            px(ps, glow, somas[i][0], somas[i][1], somas[i][2], 2.2F, 2.2F, 2.2F, c(col, col), FULL, ov);
        }
        // the membrane, translucent, over everything
        float swell = 1 + flare * 0.12F + Mth.sin(t * 0.05F) * 0.03F;
        px(ps, trans, 0, 0, 0, 12 * swell, 11 * swell, 12 * swell, SAC, light, ov);
        px(ps, trans, 0, 0, 0, 9, 13 * swell, 9, SAC, light, ov);
        // the lobes, turning about the middle
        for (int k = 0; k < 3; k++) {
            float a = t * 0.02F + k * 2.094F;
            float lx = Mth.cos(a) * 8.5F, lz = Mth.sin(a) * 8.5F, ly = Mth.sin(t * 0.04F + k) * 2;
            px(ps, trans, lx, ly, lz, 6, 5.5F, 6, LOBE, light, ov);
            int col = lerp(SOMA_DIM, SOMA_LIT, Math.max(flare, 0.5F + 0.5F * Mth.sin(t * 0.1F + k * 2)));
            px(ps, glow, lx, ly + 1, lz, 1.8F, 1.8F, 1.8F, c(col, col), FULL, ov);
        }
        // the dream: a ring of light spreading out
        if (flare > 0) {
            ps.pushPose();
            ps.mulPose(Axis.XP.rotationDegrees(90));
            ProcMesh.ring(ps, glow, (0.4F + (1 - flare) * 3.5F), 0.12F, 0.02F, 32, ProcMesh.withAlpha(0xD0A0FF, flare), FULL, ov);
            ps.popPose();
        }
        ps.popPose();
    }

    // ================================================================== the Rust Mite
    static final int[] SHELL = c(0xFFB0602A, 0xFF6A3216), SHELL_DARK = c(0xFF8A4520, 0xFF4A220E), FLAKE = c(0xFFD08848, 0xFF9A5428),
            LEG = c(0xFF5A2C14, 0xFF3A1A0A), JAW = c(0xFF3A3632, 0xFF201C18);
    static final int EYE = 0xFFFF9030;

    /**
     * A beetle-shaped mite with a shell of flaking rust: three overlapping plates over a low body, scales of oxide lifting
     * at the edges, six short legs in a tripod gait, two pale-orange eyes and grey iron-cutting jaws that work while it
     * feeds.
     */
    static void mite(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, float feed, int light, int ov) {
        float lift = Mth.abs(Mth.sin(limb * 1.6F)) * amt * 0.4F;
        ps.pushPose();
        ps.translate(0, lift / 16F, 0);
        // legs: three a side, front and back of one side moving with the middle of the other (a tripod)
        for (int side = -1; side <= 1; side += 2)
            for (int k = 0; k < 3; k++) {
                float ph = (k == 1 ? 0 : (float) Math.PI) + (side > 0 ? (float) Math.PI : 0);
                float swing = Mth.sin(limb * 1.6F + ph) * amt * 2.2F;
                float z = 3 - k * 3 + swing;
                px(ps, solid, side * 4.6F, 1.5F, z, 3, 1, 1.2F, LEG, light, ov);
                px(ps, solid, side * 6.3F, 0.6F, z, 1, 2.2F, 1, LEG, light, ov);
            }
        // the body under the shell
        px(ps, solid, 0, 2.2F, 0, 7, 2.4F, 9, SHELL_DARK, light, ov);
        // three plates of shell, overlapping like scales, the back one highest
        px(ps, solid, 0, 3.4F, -3, 8, 2.4F, 4.2F, SHELL, light, ov);
        px(ps, solid, 0, 3.6F, 0.3F, 8.4F, 2.6F, 4.2F, SHELL, light, ov);
        px(ps, solid, 0, 3.3F, 3.4F, 7, 2.2F, 3, SHELL_DARK, light, ov);
        // flakes of oxide lifting off the shell
        px(ps, solid, 2.6F, 5.1F, -1.6F, 2, 0.6F, 1.4F, FLAKE, light, ov);
        px(ps, solid, -2.2F, 5.2F, 1.2F, 1.6F, 0.6F, 2, FLAKE, light, ov);
        px(ps, solid, 0.6F, 5.0F, -4.2F, 1.4F, 0.6F, 1.2F, FLAKE, light, ov);
        px(ps, solid, -3.6F, 4.5F, -3.4F, 1, 0.6F, 1.2F, FLAKE, light, ov);
        // the head, its eyes and jaws
        px(ps, solid, 0, 2.6F, 5.4F, 4.4F, 2.2F, 2.2F, SHELL_DARK, light, ov);
        px(ps, glow, -1.3F, 3.2F, 6.5F, 0.9F, 0.9F, 0.4F, c(EYE, EYE), FULL, ov);
        px(ps, glow, 1.3F, 3.2F, 6.5F, 0.9F, 0.9F, 0.4F, c(EYE, EYE), FULL, ov);
        float chew = feed > 0 ? Mth.abs(Mth.sin(t * 0.9F)) * 1.2F : 0.3F;
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 1.1F / 16F, 1.9F / 16F, 6.6F / 16F);
            ps.mulPose(Axis.YP.rotation(side * (0.15F + chew * 0.35F)));
            px(ps, solid, 0, 0, 1, 1, 0.9F, 2.2F, JAW, light, ov);
            ps.popPose();
        }
        ps.popPose();
    }

    private MindEngRenderers() {}
}
