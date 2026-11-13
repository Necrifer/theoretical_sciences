package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.home.HomeMobs;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.9.0: those who live in Omnipotency, modelled in shaded boxes like the game's own creatures: each part a lighter
 * and a darker tone, jointed where it moves (legs, wings, arms, a bowing back), and lit parts (halos, eyes, the
 * Warden's seams when it wakes) drawn at full brightness.
 */
public final class HomeMobRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int FULL = 0xF000F0;
    private static final Map<LivingEntity, Float> WAKE = new WeakHashMap<>();

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
            VertexConsumer vc = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer tr = SafeBuffer.of(buf, RenderType.entityTranslucent(WHITE));
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            float pitch = Mth.lerp(partial, e.xRotO, e.getXRot()), head = Mth.wrapDegrees(Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot));
            ps.pushPose();
            // (the wheels "went in any of their four directions without turning as they went": the Ophan is never turned to face its way)
            if (!id.equals("threshold_warden")) ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            switch (id) {
                case "halo_lamb" -> lamb(ps, vc, tr, t, limb, amt, e instanceof HomeMobs.HaloLamb l ? l.grazeAnim : 0, partial, head, light, ov);
                case "cherub" -> cherub(ps, vc, tr, t, e.getId(), head, pitch, light, ov);
                case "choir_seraph" -> seraph(ps, vc, tr, t, e instanceof HomeMobs.ChoirSeraph s ? s.singAnim : 0, partial, head, light, ov);
                case "cloud_ray" -> ray(ps, vc, tr, t, e.getId(), light, ov);
                case "dweller" -> dweller(ps, vc, tr, t, limb, amt, e instanceof HomeMobs.Dweller d ? d.bowAnim : 0, e instanceof HomeMobs.Dweller d2 ? d2.workAnim : 0,
                        partial, e.getId(), head, pitch, light, ov);
                case "threshold_warden" -> {
                    boolean awake = e instanceof HomeMobs.ThresholdWarden w && w.awake();
                    float k = WAKE.getOrDefault(e, awake ? 1F : 0F);
                    k = Mth.clamp(k + (awake ? 0.04F : -0.03F), 0F, 1F);          // it rises quickly and kneels slowly
                    WAKE.put(e, k);
                    float[] sp = SPIN.computeIfAbsent(e, x -> new float[]{t, 0});       // turning that speeds up and slows down without jumping
                    sp[1] += Math.max(0, Math.min(2F, t - sp[0])) * k;
                    sp[0] = t;
                    warden(ps, vc, tr, sp[1] / Math.max(0.001F, k * k * (3 - 2 * k)), limb, amt, k, e instanceof HomeMobs.ThresholdWarden w2 ? w2.strikeAnim : 0, partial, head, light, ov);
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

    static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light, int ov) {
        ProcMesh.box(ps, vc, x0, y0, z0, x1, y1, z1, argb, light, ov);
    }

    /** A part in two tones: the lower third a shade darker, as if lit from above. */
    static void two(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int top, int bot, int light, int ov) {
        float ym = y0 + (y1 - y0) * 0.35F;
        box(ps, vc, x0, y0, z0, x1, ym, z1, bot, light, ov);
        box(ps, vc, x0, ym, z0, x1, y1, z1, top, light, ov);
    }

    /** A halo lying flat at height y: a square ring of four bars, always at full brightness. */
    static void halo(PoseStack ps, VertexConsumer vc, float y, float r, float th, int argb, int ov) {
        box(ps, vc, -r, y, -r, r, y + th, -r + th, argb, FULL, ov);
        box(ps, vc, -r, y, r - th, r, y + th, r, argb, FULL, ov);
        box(ps, vc, -r, y, -r + th, -r + th, y + th, r - th, argb, FULL, ov);
        box(ps, vc, r - th, y, -r + th, r, y + th, r - th, argb, FULL, ov);
    }

    /**
     * A feathered wing hinged at the origin, reaching out along +x (mirror with {@code side}): three rows of feathers,
     * each row longer and paler than the one before, the tips a little apart.
     */
    static void wing(PoseStack ps, VertexConsumer vc, float side, float span, float depth, float angle, float fold, int inner, int mid, int tip, int light, int ov) {
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(side * angle));
        box(ps, vc, 0, -0.03F, -0.04F, side * span * 0.4F, 0.05F, 0.04F, inner, light, ov);                 // the arm of the wing
        box(ps, vc, 0, -depth * 0.5F, -0.02F, side * span * 0.4F, 0, 0.02F, inner, light, ov);
        ps.translate(side * span * 0.4F, 0, 0);
        ps.mulPose(Axis.ZP.rotation(side * fold));
        box(ps, vc, 0, -0.02F, -0.03F, side * span * 0.35F, 0.04F, 0.03F, mid, light, ov);
        box(ps, vc, 0, -depth * 0.8F, -0.015F, side * span * 0.35F, 0, 0.015F, mid, light, ov);
        ps.translate(side * span * 0.35F, 0, 0);
        ps.mulPose(Axis.ZP.rotation(side * fold * 0.7F));
        for (int i = 0; i < 3; i++) {                                                                       // the long feathers
            float x0 = side * span * 0.09F * i, x1 = side * (span * 0.09F * i + span * 0.075F);
            box(ps, vc, x0, -depth * (1.0F - i * 0.22F), -0.01F, x1, 0.02F, 0.01F, tip, light, ov);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Halo Lamb
    static final int WOOL = 0xFFFFF4D2, WOOL_SH = 0xFFEBD9A4, WOOL_DEEP = 0xFFD8BE7C, FACE = 0xFFF2D9B0, FACE_SH = 0xFFD9B98A, HOOF = 0xFFB8904A, EYE = 0xFF2A2018,
            GOLD = 0xFFFFD870, GOLD_HI = 0xFFFFF0B0;

    /** A lamb with a fleece in tufts, a pale face, small golden hooves and horn buds, and a ring of light over its head. Faces +z. */
    static void lamb(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, float limb, float amt, int graze, float partial, float headYaw, int light, int ov) {
        float[][] legs = {{-0.2F, 0.28F, 0}, {0.2F, 0.28F, Mth.PI}, {-0.2F, -0.3F, Mth.PI}, {0.2F, -0.3F, 0}};
        for (float[] l : legs) {
            ps.pushPose();
            ps.translate(l[0], 0.4F, l[1]);
            ps.mulPose(Axis.XP.rotation(Mth.cos(limb * 0.9F + l[2]) * 0.7F * amt));
            two(ps, vc, -0.07F, -0.32F, -0.07F, 0.07F, 0.02F, 0.07F, FACE, FACE_SH, light, ov);
            box(ps, vc, -0.08F, -0.4F, -0.08F, 0.08F, -0.32F, 0.08F, HOOF, light, ov);
            ps.popPose();
        }
        box(ps, vc, -0.3F, 0.36F, -0.45F, 0.3F, 0.5F, 0.4F, WOOL_DEEP, light, ov);                          // belly
        box(ps, vc, -0.33F, 0.5F, -0.47F, 0.33F, 0.78F, 0.42F, WOOL_SH, light, ov);
        box(ps, vc, -0.3F, 0.78F, -0.44F, 0.3F, 0.88F, 0.38F, WOOL, light, ov);
        for (int i = 0; i < 9; i++) {                                                                       // tufts along the back and flanks
            float x = (i % 3 - 1) * 0.2F, z = (i / 3 - 1) * 0.27F - 0.03F, h = 0.03F + 0.03F * ((i * 7) % 3);
            box(ps, vc, x - 0.09F, 0.86F, z - 0.1F, x + 0.09F, 0.88F + h, z + 0.1F, i % 2 == 0 ? WOOL : GOLD_HI, light, ov);
        }
        for (int s = -1; s <= 1; s += 2)
            for (int i = 0; i < 3; i++)
                box(ps, vc, s * 0.33F, 0.52F + (i % 2) * 0.05F, -0.35F + i * 0.27F, s * 0.37F, 0.74F, -0.17F + i * 0.27F, i == 1 ? WOOL : WOOL_SH, light, ov);
        box(ps, vc, -0.06F, 0.6F, -0.55F, 0.06F, 0.74F, -0.45F, WOOL, light, ov);                           // tail
        // head: down when grazing
        float g = graze > 0 ? Mth.sin(Mth.clamp((40 - (graze - partial)) / 40F, 0F, 1F) * Mth.PI) : 0;
        ps.pushPose();
        ps.translate(0, 0.74F, 0.38F);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(headYaw, -40F, 40F) * (1 - g)));
        ps.mulPose(Axis.XP.rotation(g * 1.1F + (g > 0.5F ? Mth.sin(t * 0.9F) * 0.06F : 0)));
        box(ps, vc, -0.17F, -0.08F, -0.08F, 0.17F, 0.24F, 0.2F, WOOL, light, ov);                           // woolly crown
        two(ps, vc, -0.12F, -0.12F, 0.12F, 0.12F, 0.14F, 0.36F, FACE, FACE_SH, light, ov);                 // muzzle
        box(ps, vc, -0.05F, -0.08F, 0.36F, 0.05F, 0.0F, 0.375F, FACE_SH, light, ov);
        for (int s = -1; s <= 1; s += 2) {
            box(ps, vc, s * 0.12F, 0.04F, 0.22F, s * 0.13F, 0.1F, 0.28F, EYE, light, ov);
            box(ps, vc, s * 0.17F, 0.06F, -0.02F, s * 0.27F, 0.12F, 0.08F, FACE, light, ov);                // ear
            box(ps, vc, s * 0.08F - 0.03F, 0.24F, 0.02F, s * 0.08F + 0.03F, 0.3F, 0.08F, GOLD, light, ov);  // horn bud
        }
        ps.popPose();
        ps.pushPose();                                                                                      // the halo keeps level over the head
        ps.translate(0, 1.12F + Mth.sin(t * 0.08F) * 0.015F - g * 0.25F, 0.45F + g * 0.2F);
        halo(ps, vc, 0, 0.17F, 0.035F, GOLD_HI, ov);
        ps.popPose();
    }

    // ------------------------------------------------------------------ the eyes
    static final int EYE_WHITE = 0xFFFFFFF4, IRIS = 0xFF3C8CC8, IRIS_GOLD = 0xFFE8A020, PUPIL = 0xFF10121C, LID = 0xFF8A6A30;

    /** An eye on a surface facing +z at (x, y, z): a white, an iris and a pupil, lit; or, shut, a single dark line. */
    static void eye(PoseStack ps, VertexConsumer vc, float x, float y, float z, float r, int iris, boolean open, int light, int ov) {
        if (!open) {
            box(ps, vc, x - r, y - r * 0.15F, z, x + r, y + r * 0.15F, z + 0.006F, LID, light, ov);
            return;
        }
        box(ps, vc, x - r, y - r * 0.6F, z, x + r, y + r * 0.6F, z + 0.006F, EYE_WHITE, FULL, ov);
        box(ps, vc, x - r * 0.5F, y - r * 0.6F, z + 0.006F, x + r * 0.5F, y + r * 0.6F, z + 0.012F, iris, FULL, ov);
        box(ps, vc, x - r * 0.2F, y - r * 0.35F, z + 0.012F, x + r * 0.2F, y + r * 0.35F, z + 0.018F, PUPIL, light, ov);
    }

    /** A wing as {@link #wing}, with eyes opened along it ("full of eyes all round, even their wings"). */
    static void eyedWing(PoseStack ps, VertexConsumer vc, float side, float span, float depth, float angle, float fold, int inner, int mid, int tip, int iris, int eyes,
                         int light, int ov) {
        wing(ps, vc, side, span, depth, angle, fold, inner, mid, tip, light, ov);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(side * angle));
        for (int i = 0; i < eyes; i++) {
            float x = side * span * (0.08F + 0.3F * i / Math.max(1, eyes - 1));
            eye(ps, vc, x, -depth * 0.25F, 0.021F, span * 0.035F, iris, true, light, ov);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(Mth.PI));                                                           // and on the other face of the wing
            eye(ps, vc, -x, -depth * 0.25F, 0.021F, span * 0.035F, iris, true, light, ov);
            ps.popPose();
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Cherub
    static final int BRONZE = 0xFFD89A50, BRONZE_SH = 0xFFB07030, BRONZE_DEEP = 0xFF8A5220, BRONZE_HI = 0xFFFFD08A, MAN = 0xFFF0D0A8, MAN_SH = 0xFFD8B088,
            LION = 0xFFE0A848, LION_MANE = 0xFFB87828, OX = 0xFF8A5A3A, OX_SH = 0xFF6A4028, HORN = 0xFFF4ECD8, EAGLE = 0xFFF8F4EC, EAGLE_SH = 0xFF7A5A3A, BEAK = 0xFFF0B020,
            FEATHER = 0xFFFFFFFF, FEATHER_MID = 0xFFF4E8D0, FEATHER_IN = 0xFFE0C898;

    /**
     * A cherub as Ezekiel saw them (chapters 1 and 10): four faces on one head (a man's in front, a lion's on its
     * right, an ox's on its left, an eagle's behind); four wings, two spread upward and two folded over the body;
     * a man's hands under the wings; straight legs ending in a calf's hooves; all of it gleaming like burnished
     * bronze, and eyes opened over its body, its hands and its wings. Faces +z.
     */
    static void cherub(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, int seed, float headYaw, float pitch, int light, int ov) {
        ps.translate(0, 0.1F + Mth.sin(t * 0.09F + seed) * 0.06F, 0);
        for (int s = -1; s <= 1; s += 2) {                                                                  // the legs: straight, and a calf's hoof
            two(ps, vc, s * 0.13F - 0.07F, 0.1F, -0.07F, s * 0.13F + 0.07F, 0.62F, 0.07F, BRONZE, BRONZE_SH, light, ov);
            box(ps, vc, s * 0.13F - 0.085F, 0.0F, -0.085F, s * 0.13F + 0.085F, 0.1F, 0.1F, BRONZE_DEEP, light, ov);
            box(ps, vc, s * 0.13F - 0.01F, 0.0F, 0.1F, s * 0.13F + 0.01F, 0.08F, 0.104F, 0xFF3A2410, light, ov);     // the cleft of the hoof
            eye(ps, vc, s * 0.13F, 0.42F, 0.07F, 0.035F, IRIS, true, light, ov);
        }
        two(ps, vc, -0.24F, 0.6F, -0.14F, 0.24F, 1.2F, 0.14F, BRONZE, BRONZE_SH, light, ov);               // the body
        box(ps, vc, -0.25F, 0.6F, -0.15F, 0.25F, 0.68F, 0.15F, BRONZE_DEEP, light, ov);
        box(ps, vc, -0.2F, 1.12F, 0.14F, 0.2F, 1.18F, 0.15F, BRONZE_HI, light, ov);
        eye(ps, vc, 0, 0.98F, 0.14F, 0.07F, IRIS_GOLD, true, light, ov);                                    // eyes over the body, before
        eye(ps, vc, -0.13F, 0.8F, 0.14F, 0.045F, IRIS, true, light, ov);
        eye(ps, vc, 0.13F, 0.8F, 0.14F, 0.045F, IRIS, true, light, ov);
        ps.pushPose();                                                                                      // and behind
        ps.mulPose(Axis.YP.rotation(Mth.PI));
        eye(ps, vc, 0, 0.95F, 0.14F, 0.06F, IRIS, true, light, ov);
        eye(ps, vc, -0.12F, 0.78F, 0.14F, 0.04F, IRIS_GOLD, true, light, ov);
        eye(ps, vc, 0.12F, 0.78F, 0.14F, 0.04F, IRIS_GOLD, true, light, ov);
        ps.popPose();
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();                                                                                  // a man's hand under the wing, an eye in its back
            ps.translate(s * 0.27F, 1.08F, 0.02F);
            ps.mulPose(Axis.ZP.rotation(s * 0.2F));
            ps.mulPose(Axis.XP.rotation(-0.35F + Mth.sin(t * 0.07F + s) * 0.08F));
            two(ps, vc, -0.05F, -0.42F, -0.05F, 0.05F, 0.0F, 0.05F, BRONZE, BRONZE_SH, light, ov);
            box(ps, vc, -0.055F, -0.52F, -0.045F, 0.055F, -0.42F, 0.045F, MAN, light, ov);
            eye(ps, vc, 0, -0.47F, 0.045F, 0.03F, IRIS, true, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // the upper wings: spread upward
            ps.translate(s * 0.16F, 1.14F, -0.14F);
            eyedWing(ps, vc, s, 1.15F, 0.5F, 0.85F + Mth.sin(t * 0.25F) * 0.22F, -0.3F, FEATHER_IN, FEATHER_MID, FEATHER, IRIS, 3, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // the lower wings: folded forward over the body
            ps.translate(s * 0.22F, 1.02F, -0.02F);
            ps.mulPose(Axis.YP.rotation(-s * 1.05F));
            eyedWing(ps, vc, s, 0.8F, 0.5F, -1.2F + Mth.sin(t * 0.09F) * 0.04F, 0.15F, FEATHER_IN, FEATHER_MID, FEATHER, IRIS_GOLD, 2, light, ov);
            ps.popPose();
        }
        // the head: one head, four faces
        ps.pushPose();
        ps.translate(0, 1.2F, 0);
        box(ps, vc, -0.08F, 0.0F, -0.08F, 0.08F, 0.06F, 0.08F, BRONZE_DEEP, light, ov);
        ps.translate(0, 0.05F, 0);
        float h = 0.19F;
        box(ps, vc, -h, 0, -h, h, 2 * h, h, BRONZE_SH, light, ov);
        // a man, in front
        two(ps, vc, -h + 0.02F, 0.02F, h, h - 0.02F, 2 * h - 0.02F, h + 0.03F, MAN, MAN_SH, light, ov);
        for (int s = -1; s <= 1; s += 2) eye(ps, vc, s * 0.075F, 0.23F, h + 0.03F, 0.035F, IRIS, true, light, ov);
        box(ps, vc, -0.02F, 0.13F, h + 0.03F, 0.02F, 0.2F, h + 0.05F, MAN_SH, light, ov);
        box(ps, vc, -0.05F, 0.07F, h + 0.03F, 0.05F, 0.085F, h + 0.036F, 0xFFA06850, light, ov);
        box(ps, vc, -h + 0.02F, 0.3F, h, h - 0.02F, 2 * h, h + 0.04F, BRONZE_DEEP, light, ov);              // hair
        // a lion, on its right (-x is the creature's right when it faces +z)
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(-Mth.HALF_PI));
        box(ps, vc, -h, 0.0F, h, h, 2 * h, h + 0.03F, LION_MANE, light, ov);                                 // the mane, all round
        two(ps, vc, -0.12F, 0.05F, h + 0.03F, 0.12F, 0.31F, h + 0.06F, LION, LION_MANE, light, ov);
        box(ps, vc, -0.07F, 0.06F, h + 0.06F, 0.07F, 0.17F, h + 0.12F, 0xFFF0D090, light, ov);               // muzzle
        box(ps, vc, -0.03F, 0.14F, h + 0.12F, 0.03F, 0.17F, h + 0.13F, 0xFF4A2A18, light, ov);
        for (int s = -1; s <= 1; s += 2) {
            eye(ps, vc, s * 0.075F, 0.24F, h + 0.06F, 0.03F, IRIS_GOLD, true, light, ov);
            box(ps, vc, s * 0.13F - 0.035F, 0.34F, h, s * 0.13F + 0.035F, 0.41F, h + 0.05F, LION, light, ov);        // ear
        }
        ps.popPose();
        // an ox, on its left
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.HALF_PI));
        two(ps, vc, -0.15F, 0.02F, h, 0.15F, 0.36F, h + 0.04F, OX, OX_SH, light, ov);
        box(ps, vc, -0.09F, 0.03F, h + 0.04F, 0.09F, 0.15F, h + 0.11F, 0xFFC8A088, light, ov);               // muzzle
        for (int s = -1; s <= 1; s += 2) {
            box(ps, vc, s * 0.04F - 0.012F, 0.07F, h + 0.11F, s * 0.04F + 0.012F, 0.1F, h + 0.115F, 0xFF3A2018, light, ov);
            eye(ps, vc, s * 0.09F, 0.24F, h + 0.04F, 0.03F, 0xFF4A3020, true, light, ov);
            box(ps, vc, s * 0.15F, 0.3F, h - 0.02F, s * 0.27F, 0.36F, h + 0.04F, HORN, light, ov);          // horns, out and then up
            box(ps, vc, s * 0.23F, 0.36F, h - 0.01F, s * 0.28F, 0.48F, h + 0.03F, HORN, light, ov);
        }
        ps.popPose();
        // an eagle, behind
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.PI));
        two(ps, vc, -0.14F, 0.04F, h, 0.14F, 0.36F, h + 0.04F, EAGLE, 0xFFE0D8C8, light, ov);
        box(ps, vc, -0.15F, 0.0F, h, 0.15F, 0.08F, h + 0.03F, EAGLE_SH, light, ov);                          // the dark feathers of the neck
        box(ps, vc, -0.035F, 0.13F, h + 0.04F, 0.035F, 0.22F, h + 0.14F, BEAK, light, ov);                   // the beak, hooked
        box(ps, vc, -0.025F, 0.08F, h + 0.1F, 0.025F, 0.14F, h + 0.15F, 0xFFD89010, light, ov);
        for (int s = -1; s <= 1; s += 2) {
            eye(ps, vc, s * 0.085F, 0.25F, h + 0.04F, 0.03F, IRIS_GOLD, true, light, ov);
            box(ps, vc, s * 0.085F - 0.045F, 0.285F, h + 0.04F, s * 0.085F + 0.045F, 0.31F, h + 0.06F, EAGLE_SH, light, ov);      // the brow
        }
        ps.popPose();
        halo(ps, vc, 2 * h + 0.1F, 0.2F, 0.03F, GOLD_HI, ov);
        ps.popPose();
    }

    // ------------------------------------------------------------------ Choir Seraph
    static final int FIRE_CORE = 0xFFFFFFF0, FIRE_HI = 0xFFFFF0B0, FIRE = 0xFFFFC850, FIRE_MID = 0xFFFF9A30, FIRE_DEEP = 0xFFE86018, GEM = 0xFF9CD8FF;

    /**
     * A seraph as Isaiah saw them (6:2): six wings, and little else to be seen. With two it covers its face, with two
     * it covers its feet, and with two it flies. The name means the burning ones, so its feathers run from white at
     * the root to flame at the tip and what is behind them is too bright to look at; and, as in Revelation 4:8, the
     * wings are full of eyes. When it sings, the wings over its face part a little, and the light comes out.
     */
    static void seraph(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, int sing, float partial, float headYaw, int light, int ov) {
        float s01 = sing > 0 ? Mth.sin(Mth.clamp((90 - (sing - partial)) / 90F, 0F, 1F) * Mth.PI) : 0;
        ps.translate(0, 0.3F + Mth.sin(t * 0.07F) * 0.08F, 0);
        // what is behind the wings: a column of light, narrowing to nothing
        box(ps, vc, -0.05F, 0.0F, -0.05F, 0.05F, 0.35F, 0.05F, FIRE_MID, FULL, ov);
        box(ps, vc, -0.1F, 0.3F, -0.08F, 0.1F, 0.7F, 0.08F, FIRE, FULL, ov);
        box(ps, vc, -0.15F, 0.65F, -0.1F, 0.15F, 1.25F, 0.1F, FIRE_HI, FULL, ov);
        box(ps, vc, -0.12F, 1.25F, -0.11F, 0.12F, 1.55F, 0.11F, FIRE_CORE, FULL, ov);                       // where a face would be
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();                                                                                  // two to fly with
            ps.translate(sd * 0.13F, 1.05F, -0.1F);
            eyedWing(ps, vc, sd, 1.6F, 0.65F, Mth.sin(t * 0.22F) * 0.38F + 0.12F + s01 * 0.2F, -0.15F, FIRE_HI, FIRE, FIRE_MID, IRIS_GOLD, 4, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // two to cover its face: raised, and crossed before it
            ps.translate(sd * 0.2F, 1.62F, 0.14F + sd * 0.012F);
            ps.mulPose(Axis.YP.rotation(-sd * (2.45F - s01 * 1.0F)));
            eyedWing(ps, vc, sd, 0.7F, 0.5F, 0.1F + s01 * 0.3F, -0.1F, FIRE_HI, FIRE, FIRE_MID, IRIS, 2, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // two to cover its feet: lowered, and crossed before them
            ps.translate(sd * 0.2F, 0.62F, 0.12F + sd * 0.012F);
            ps.mulPose(Axis.YP.rotation(-sd * (2.4F - s01 * 0.5F)));
            eyedWing(ps, vc, sd, 0.75F, 0.62F, -0.15F, -0.1F, FIRE, FIRE_MID, FIRE_DEEP, IRIS, 2, light, ov);
            ps.popPose();
        }
        // a wheel of flame behind it, turning
        ps.pushPose();
        ps.translate(0, 1.4F, -0.22F);
        ps.mulPose(Axis.ZP.rotation(t * 0.015F));
        float r = 0.42F + s01 * 0.1F;
        for (int i = 0; i < 12; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.TWO_PI / 12));
            box(ps, vc, -r * 0.27F, r - 0.035F, -0.012F, r * 0.27F, r, 0.012F, i % 2 == 0 ? FIRE_HI : FIRE, FULL, ov);
            if (i % 3 == 0) box(ps, vc, -0.02F, r, -0.01F, 0.02F, r + 0.1F + s01 * 0.08F, 0.01F, FIRE_MID, FULL, ov);
            ps.popPose();
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Cloud Ray
    static final int RAY_TOP = 0xFFEDF2FF, RAY_MID = 0xFFD2DCF4, RAY_DEEP = 0xFFB4C2E4, RAY_BELLY = 0xFFFFFFFF, RAY_SPOT = 0xFFFFE9A8;

    /** A broad ray: a flat body, wings in four tapering panels that roll in a slow wave, two lobes at the mouth, and a long thin tail. */
    static void ray(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, int seed, int light, int ov) {
        float w = t * 0.09F + seed;
        ps.translate(0, 0.3F + Mth.sin(w) * 0.06F, 0);
        box(ps, vc, -0.42F, -0.1F, -0.7F, 0.42F, 0.0F, 0.75F, RAY_BELLY, light, ov);
        box(ps, vc, -0.42F, 0.0F, -0.7F, 0.42F, 0.12F, 0.75F, RAY_MID, light, ov);
        box(ps, vc, -0.3F, 0.12F, -0.55F, 0.3F, 0.2F, 0.6F, RAY_TOP, light, ov);
        box(ps, vc, -0.14F, 0.2F, -0.3F, 0.14F, 0.25F, 0.4F, RAY_TOP, light, ov);                           // the ridge of the back
        for (int i = 0; i < 5; i++) box(ps, vc, -0.2F + (i * 37 % 5) * 0.09F, 0.2F, -0.45F + i * 0.22F, -0.13F + (i * 37 % 5) * 0.09F, 0.212F, -0.38F + i * 0.22F, RAY_SPOT, FULL, ov);
        for (int s = -1; s <= 1; s += 2) {
            box(ps, vc, s * 0.2F, -0.06F, 0.75F, s * 0.34F, 0.08F, 1.0F, RAY_MID, light, ov);               // lobes
            box(ps, vc, s * 0.36F, 0.06F, 0.5F, s * 0.425F, 0.13F, 0.58F, 0xFF28304A, light, ov);           // eye
            ps.pushPose();                                                                                  // the wing: four panels, each bending a little further
            ps.translate(s * 0.42F, 0.03F, 0);
            float len = 0.3F;
            for (int k = 0; k < 4; k++) {
                ps.mulPose(Axis.ZP.rotation(s * Mth.sin(w - k * 0.7F) * 0.28F));
                float z0 = -0.6F + k * 0.13F, z1 = 0.62F - k * 0.2F, th = 0.09F - k * 0.018F;
                box(ps, vc, 0, -th, z0, s * len, 0, z1, RAY_BELLY, light, ov);
                box(ps, vc, 0, 0, z0, s * len, th, z1, k % 2 == 0 ? RAY_MID : RAY_DEEP, light, ov);
                ps.translate(s * len, 0, 0);
            }
            ps.popPose();
        }
        ps.pushPose();                                                                                      // the tail trails and sways
        ps.translate(0, 0.04F, -0.7F);
        for (int k = 0; k < 6; k++) {
            ps.mulPose(Axis.YP.rotation(Mth.sin(w * 1.3F - k * 0.6F) * 0.12F));
            float th = 0.05F - k * 0.006F;
            box(ps, vc, -th, -th, -0.3F, th, th, 0, k % 2 == 0 ? RAY_MID : RAY_DEEP, light, ov);
            ps.translate(0, 0, -0.3F);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ Dweller
    static final int[] SASH = {0xFFD8A040, 0xFF7098D8, 0xFFC87890, 0xFF80B890};
    static final int D_FACE = 0xFFFFF2D8, D_FACE_SH = 0xFFF0D8B0, D_ROBE = 0xFFFBF7EC, D_ROBE_SH = 0xFFE2DAC6, D_ROBE_DEEP = 0xFFC8BEA6, D_EYE = 0xFF5A4420;

    /**
     * One who lives here: a long pale robe that widens to the ground, wide sleeves, a hood, a calm bright face, a sash
     * of its own colour, and a thin halo. It bows from the waist, and raises a hand to its work.
     */
    static void dweller(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, float limb, float amt, int bow, int work, float partial, int seed,
                        float headYaw, float pitch, int light, int ov) {
        int sash = SASH[Math.floorMod(seed, SASH.length)];
        float b = bow > 0 ? Mth.sin(Mth.clamp((30 - (bow - partial)) / 30F, 0F, 1F) * Mth.PI) : 0;
        float sway = Mth.sin(limb * 0.6F) * 0.04F * amt;
        // the skirt of the robe, in three widening tiers, swinging a little with the walk
        box(ps, vc, -0.27F + sway, 0.0F, -0.2F, 0.27F + sway, 0.3F, 0.2F, D_ROBE_DEEP, light, ov);
        box(ps, vc, -0.24F, 0.3F, -0.17F, 0.24F, 0.6F, 0.17F, D_ROBE_SH, light, ov);
        box(ps, vc, -0.21F, 0.6F, -0.15F, 0.21F, 0.9F, 0.15F, D_ROBE, light, ov);
        box(ps, vc, -0.28F + sway, 0.0F, -0.21F, 0.28F + sway, 0.05F, 0.21F, sash, light, ov);              // a band at the hem
        box(ps, vc, -0.05F, 0.05F, 0.2F, 0.05F, 0.3F, 0.215F, GOLD, light, ov);                              // the stole, down the front of the skirt
        box(ps, vc, -0.05F, 0.3F, 0.17F, 0.05F, 0.6F, 0.185F, GOLD, light, ov);
        box(ps, vc, -0.05F, 0.6F, 0.15F, 0.05F, 0.92F, 0.165F, GOLD, light, ov);
        for (int s2 = -1; s2 <= 1; s2 += 2) {                                                               // folds in the cloth
            box(ps, vc, s2 * 0.14F - 0.02F, 0.02F, 0.2F, s2 * 0.14F + 0.02F, 0.28F, 0.208F, D_ROBE_SH, light, ov);
            box(ps, vc, s2 * 0.12F - 0.02F, 0.3F, 0.17F, s2 * 0.12F + 0.02F, 0.58F, 0.178F, D_ROBE_DEEP, light, ov);
        }
        box(ps, vc, -0.19F, 0.84F, -0.13F, 0.19F, 0.98F, 0.13F, D_ROBE_SH, light, ov);                      // the waist, so that a bow does not open a gap
        ps.pushPose();
        ps.translate(0, 0.9F, 0);
        ps.mulPose(Axis.XP.rotation(b * 0.75F));
        box(ps, vc, -0.22F, 0.0F, -0.15F, 0.22F, 0.08F, 0.15F, sash, light, ov);                            // sash
        box(ps, vc, 0.08F, -0.3F, 0.15F, 0.14F, 0.0F, 0.17F, sash, light, ov);                              // its end, hanging
        two(ps, vc, -0.22F, 0.08F, -0.14F, 0.22F, 0.6F, 0.14F, D_ROBE, D_ROBE_SH, light, ov);
        box(ps, vc, -0.29F, 0.46F, -0.17F, 0.29F, 0.62F, 0.17F, D_ROBE_SH, light, ov);                      // a mantle over the shoulders
        box(ps, vc, -0.3F, 0.44F, -0.18F, 0.3F, 0.48F, 0.18F, GOLD, light, ov);
        box(ps, vc, -0.05F, 0.08F, 0.14F, 0.05F, 0.46F, 0.155F, GOLD, light, ov);
        box(ps, vc, -0.07F, 0.34F, 0.15F, 0.07F, 0.44F, 0.175F, sash, FULL, ov);                            // a clasp, lit
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.27F, 0.55F, 0);
            float swing = Mth.cos(limb * 0.6F + (s > 0 ? 0 : Mth.PI)) * 0.5F * amt;
            float raise = s > 0 && work > 0 ? -1.6F * Mth.sin(Mth.clamp((12 - (work - partial)) / 12F, 0F, 1F) * Mth.PI) : 0;
            ps.mulPose(Axis.XP.rotation(swing + raise - b * 0.5F));
            ps.mulPose(Axis.ZP.rotation(s * 0.08F));
            box(ps, vc, -0.06F, -0.3F, -0.07F, 0.06F, 0.04F, 0.07F, D_ROBE, light, ov);
            box(ps, vc, -0.08F, -0.52F, -0.09F, 0.08F, -0.3F, 0.09F, D_ROBE_SH, light, ov);                 // the wide cuff
            box(ps, vc, -0.04F, -0.6F, -0.04F, 0.04F, -0.52F, 0.04F, D_FACE, light, ov);
            ps.popPose();
        }
        ps.translate(0, 0.6F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(headYaw, -60F, 60F)));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.clamp(pitch, -30F, 30F)));
        box(ps, vc, -0.18F, -0.02F, -0.19F, 0.18F, 0.4F, 0.1F, D_ROBE, light, ov);                          // hood
        box(ps, vc, -0.19F, 0.3F, -0.19F, 0.19F, 0.42F, 0.17F, D_ROBE_SH, light, ov);
        two(ps, vc, -0.14F, 0.02F, 0.02F, 0.14F, 0.32F, 0.16F, D_FACE, D_FACE_SH, light, ov);
        for (int s = -1; s <= 1; s += 2) box(ps, vc, s * 0.04F, 0.17F, 0.16F, s * 0.09F, 0.2F, 0.165F, D_EYE, light, ov);
        box(ps, vc, -0.03F, 0.08F, 0.16F, 0.03F, 0.095F, 0.164F, 0xFFC8A070, light, ov);
        halo(ps, vc, 0.5F, 0.15F, 0.025F, GOLD_HI, ov);
        ps.popPose();
    }

    // ------------------------------------------------------------------ Threshold Ophan
    static final int TOPAZ = 0xFFF0C050, TOPAZ_SH = 0xFFC89028, TOPAZ_DEEP = 0xFF9A6A18, TOPAZ_HI = 0xFFFFE9A0, DULL = 0xFFB0A070, DULL_SH = 0xFF8A7C54, DULL_DEEP = 0xFF6A5E40;
    private static final Map<LivingEntity, float[]> SPIN = new WeakHashMap<>();

    /** A wheel in the XY plane about the origin: sixteen blocks of rim, an eye in every other one on the outer face and on both sides. */
    static void wheel(PoseStack ps, VertexConsumer vc, float r, float w, float th, int a, int b, int iris, boolean open, int light, int ov) {
        int n = 16;
        float half = r * Mth.sin(Mth.PI / n) * 1.04F;
        for (int i = 0; i < n; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.TWO_PI / n));
            box(ps, vc, -half, r - th, -w, half, r, w, i % 2 == 0 ? a : b, light, ov);
            if (i % 2 == 0) {
                eye(ps, vc, 0, r - th * 0.5F, w, th * 0.42F, iris, open, light, ov);                        // on this side of the rim
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(Mth.PI));
                eye(ps, vc, 0, r - th * 0.5F, w, th * 0.42F, iris, open, light, ov);                        // and the other
                ps.popPose();
            } else {
                ps.pushPose();                                                                              // and looking outward from the rim itself
                ps.translate(0, r, 0);
                ps.mulPose(Axis.XP.rotation(-Mth.HALF_PI));
                eye(ps, vc, 0, 0, 0, w * 0.6F, iris, open, light, ov);
                ps.popPose();
            }
            ps.popPose();
        }
    }

    /**
     * An ophan, one of the wheels of Ezekiel 1 and 10: a wheel within a wheel, the colour of topaz, the rims full of
     * eyes all round, going any way without turning. Three rims here, one inside another, about one great eye, with a
     * ring of wings between (the wings are how these are drawn now; Ezekiel gives the wheels none). {@code k} is 0
     * at rest (the rims lie one inside the other and still, every eye shut, the wings folded, the gold dull) and 1
     * awake (every eye open, the rims turning each about its own axis, the wings out).
     */
    static void warden(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, float limb, float amt, float k, int strike, float partial, float headYaw, int light, int ov) {
        float e = k * k * (3 - 2 * k);
        boolean open = e > 0.35F;
        float flare = strike > 0 ? Mth.sin(Mth.clamp((12 - (strike - partial)) / 12F, 0F, 1F) * Mth.PI) : 0;
        int a = open ? TOPAZ : DULL, b = open ? TOPAZ_SH : DULL_SH, c = open ? TOPAZ_DEEP : DULL_DEEP, lit = open ? FULL : light;
        ps.translate(0, 1.25F + (1 - e) * -0.1F + Mth.sin(t * 0.06F) * 0.05F * e, 0);
        float sc = 1F + flare * 0.12F;
        ps.scale(sc, sc, sc);
        float ph = t * 0.045F * e;                                                                          // (the caller keeps the turning continuous: see SPIN)
        // the great eye at the middle
        box(ps, vc, -0.26F, -0.26F, -0.2F, 0.26F, 0.26F, 0.2F, c, light, ov);
        box(ps, vc, -0.2F, -0.3F, -0.16F, 0.2F, 0.3F, 0.16F, b, light, ov);
        box(ps, vc, -0.3F, -0.2F, -0.16F, 0.3F, 0.2F, 0.16F, b, light, ov);
        for (int side = 0; side < 2; side++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(side * Mth.PI));
            if (open) {
                box(ps, vc, -0.2F, -0.13F, 0.2F, 0.2F, 0.13F, 0.215F, EYE_WHITE, FULL, ov);
                box(ps, vc, -0.13F, -0.18F, 0.2F, 0.13F, 0.18F, 0.213F, EYE_WHITE, FULL, ov);
                box(ps, vc, -0.1F, -0.1F, 0.215F, 0.1F, 0.1F, 0.23F, IRIS_GOLD, FULL, ov);
                box(ps, vc, -0.04F, -0.07F, 0.23F, 0.04F, 0.07F, 0.24F, PUPIL, light, ov);
            } else {
                box(ps, vc, -0.2F, -0.025F, 0.2F, 0.2F, 0.025F, 0.212F, LID, light, ov);
            }
            ps.popPose();
        }
        // the ring of wings: six, about the eye
        for (int i = 0; i < 6; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.TWO_PI / 6 + Mth.PI / 6 + ph * 0.3F));
            ps.translate(0.28F, 0, -0.05F);
            float out = Mth.lerp(e, -1.1F, 0.0F) + Mth.sin(t * 0.2F + i) * 0.12F * e;
            ps.mulPose(Axis.YP.rotation(-out));                                                             // folded back flat against the rims, or out
            if (open) eyedWing(ps, vc, 1, 1.25F, 0.5F, 0, -0.12F, FEATHER_MID, FEATHER, FEATHER, IRIS, 3, light, ov);
            else wing(ps, vc, 1, 1.25F, 0.5F, 0, -0.12F, DULL_DEEP, DULL_SH, DULL, light, ov);
            ps.popPose();
        }
        // the wheels within the wheels: each turns about an axis of its own
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(ph));
        wheel(ps, vc, 1.15F, 0.07F, 0.16F, a, b, IRIS, open, light, ov);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.HALF_PI * e + ph * 1.3F));
        ps.mulPose(Axis.ZP.rotation(-ph * 0.7F));
        wheel(ps, vc, 0.92F, 0.065F, 0.14F, a, b, IRIS_GOLD, open, light, ov);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotation(Mth.HALF_PI * e * 0.8F + ph * 1.7F));
        ps.mulPose(Axis.ZP.rotation(ph * 0.9F));
        wheel(ps, vc, 0.7F, 0.06F, 0.12F, b, c, IRIS, open, light, ov);
        ps.popPose();
        if (open) {                                                                                         // fire going between them, as between the creatures
            for (int i = 0; i < 4; i++) {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(ph * 2.3F + i * Mth.HALF_PI));
                ps.mulPose(Axis.ZP.rotation(Mth.sin(t * 0.13F + i * 2) * 0.8F));
                box(ps, vc, 0.4F, -0.03F, -0.03F, 0.52F + flare * 0.4F, 0.03F, 0.03F, FIRE_HI, lit, ov);
                ps.popPose();
            }
        }
    }

    private HomeMobRenderers() {}
}
