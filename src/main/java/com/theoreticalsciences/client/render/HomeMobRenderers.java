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
            ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
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
                    warden(ps, vc, tr, t, limb, amt, k, e instanceof HomeMobs.ThresholdWarden w2 ? w2.strikeAnim : 0, partial, head, light, ov);
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

    // ------------------------------------------------------------------ Cherub
    static final int SKIN = 0xFFFFE6CC, SKIN_SH = 0xFFEFC8A4, ROBE = 0xFFFDFBF4, ROBE_SH = 0xFFE4DECC, HAIR = 0xFFF2C660, HAIR_SH = 0xFFD8A63C,
            FEATHER = 0xFFFFFFFF, FEATHER_MID = 0xFFF0F0F8, FEATHER_IN = 0xFFDCDCEC, BLUSH = 0xFFF8B8A8, EYE_BLUE = 0xFF3A5A9A;

    /** A small round keeper: a big head of golden curls, a short robe with a sash, bare feet, and two wings that never stop. */
    static void cherub(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, int seed, float headYaw, float pitch, int light, int ov) {
        ps.translate(0, 0.08F + Mth.sin(t * 0.18F + seed) * 0.05F, 0);
        two(ps, vc, -0.15F, 0.12F, -0.11F, 0.15F, 0.42F, 0.11F, ROBE, ROBE_SH, light, ov);
        box(ps, vc, -0.17F, 0.08F, -0.13F, 0.17F, 0.16F, 0.13F, ROBE_SH, light, ov);                        // the hem
        box(ps, vc, -0.16F, 0.26F, -0.12F, 0.16F, 0.31F, 0.12F, GOLD, light, ov);                           // sash
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.08F, 0.1F, 0);
            ps.mulPose(Axis.XP.rotation(0.25F + Mth.sin(t * 0.18F + s) * 0.15F));
            two(ps, vc, -0.045F, -0.14F, -0.045F, 0.045F, 0.0F, 0.045F, SKIN, SKIN_SH, light, ov);
            ps.popPose();
            ps.pushPose();
            ps.translate(s * 0.17F, 0.38F, 0);
            ps.mulPose(Axis.ZP.rotation(s * (0.35F + Mth.sin(t * 0.12F + s * 2) * 0.12F)));
            box(ps, vc, -0.04F, -0.12F, -0.045F, 0.04F, 0.02F, 0.045F, ROBE, light, ov);                    // sleeve
            box(ps, vc, -0.035F, -0.2F, -0.035F, 0.035F, -0.12F, 0.035F, SKIN, light, ov);
            ps.popPose();
            ps.pushPose();
            ps.translate(s * 0.08F, 0.36F, -0.11F);
            ps.mulPose(Axis.YP.rotation(s * 0.5F));
            wing(ps, vc, s, 0.5F, 0.26F, Mth.sin(t * 1.3F) * 0.55F + 0.15F, -0.25F, FEATHER_IN, FEATHER_MID, FEATHER, light, ov);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0, 0.42F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(headYaw, -50F, 50F)));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.clamp(pitch, -30F, 30F)));
        two(ps, vc, -0.16F, 0, -0.15F, 0.16F, 0.3F, 0.15F, SKIN, SKIN_SH, light, ov);
        box(ps, vc, -0.175F, 0.2F, -0.165F, 0.175F, 0.34F, 0.12F, HAIR, light, ov);                         // curls
        box(ps, vc, -0.175F, 0.08F, -0.165F, 0.175F, 0.2F, -0.06F, HAIR_SH, light, ov);
        for (int i = 0; i < 4; i++) box(ps, vc, -0.15F + i * 0.085F, 0.25F, 0.12F, -0.09F + i * 0.085F, 0.33F - (i % 2) * 0.03F, 0.165F, i % 2 == 0 ? HAIR : HAIR_SH, light, ov);
        for (int s = -1; s <= 1; s += 2) {
            box(ps, vc, s * 0.05F, 0.12F, 0.15F, s * 0.1F, 0.18F, 0.155F, EYE_BLUE, light, ov);
            box(ps, vc, s * 0.09F, 0.05F, 0.15F, s * 0.14F, 0.09F, 0.153F, BLUSH, light, ov);
        }
        box(ps, vc, -0.025F, 0.04F, 0.15F, 0.025F, 0.055F, 0.154F, 0xFFC88070, light, ov);
        halo(ps, vc, 0.4F, 0.13F, 0.03F, GOLD_HI, ov);
        ps.popPose();
    }

    // ------------------------------------------------------------------ Choir Seraph
    static final int VEIL = 0xFFF8F8FF, VEIL_SH = 0xFFD8DCF0, VEIL_DEEP = 0xFFB8C0E0, MASK = 0xFFFFE9A8, MASK_SH = 0xFFE0B860, GEM = 0xFF9CD8FF;

    /**
     * Six wings and no face: a tall robe that narrows to nothing, a golden mask, a great upright halo, and a light in
     * the breast. The upper wings are held high, the middle pair beats slowly, the lower pair hangs folded where feet
     * would be. Singing, all six open and the head goes back.
     */
    static void seraph(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, int sing, float partial, float headYaw, int light, int ov) {
        float s01 = sing > 0 ? Mth.sin(Mth.clamp((90 - (sing - partial)) / 90F, 0F, 1F) * Mth.PI) : 0;
        ps.translate(0, 0.25F + Mth.sin(t * 0.07F) * 0.08F, 0);
        // the robe, in tiers
        box(ps, vc, -0.07F, 0.0F, -0.06F, 0.07F, 0.3F, 0.06F, VEIL_DEEP, light, ov);
        box(ps, vc, -0.14F, 0.25F, -0.1F, 0.14F, 0.6F, 0.1F, VEIL_DEEP, light, ov);
        box(ps, vc, -0.2F, 0.55F, -0.13F, 0.2F, 0.95F, 0.13F, VEIL_SH, light, ov);
        two(ps, vc, -0.24F, 0.9F, -0.15F, 0.24F, 1.4F, 0.15F, VEIL, VEIL_SH, light, ov);
        box(ps, vc, -0.27F, 1.32F, -0.16F, 0.27F, 1.42F, 0.16F, MASK_SH, light, ov);                        // collar
        box(ps, vc, -0.03F, 0.5F, 0.13F, 0.03F, 1.3F, 0.15F, MASK, light, ov);                              // a stole down the front
        box(ps, vc, -0.07F, 1.1F, 0.15F, 0.07F, 1.24F, 0.18F, GEM, FULL, ov);                               // the light in the breast
        for (int sd = -1; sd <= 1; sd += 2) {
            ps.pushPose();                                                                                  // upper pair: held high
            ps.translate(sd * 0.12F, 1.3F, -0.14F);
            wing(ps, vc, sd, 1.1F, 0.5F, 0.95F + s01 * 0.25F + Mth.sin(t * 0.09F) * 0.05F, -0.35F, FEATHER_IN, FEATHER_MID, FEATHER, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // middle pair: the ones that beat
            ps.translate(sd * 0.14F, 1.1F, -0.15F);
            wing(ps, vc, sd, 1.4F, 0.6F, Mth.sin(t * 0.22F) * 0.35F + 0.1F + s01 * 0.2F, -0.15F, FEATHER_IN, FEATHER_MID, FEATHER, light, ov);
            ps.popPose();
            ps.pushPose();                                                                                  // lower pair: folded down, opening when it sings
            ps.translate(sd * 0.12F, 0.85F, -0.12F);
            wing(ps, vc, sd, 0.95F, 0.45F, -1.25F + s01 * 0.9F, 0.2F, VEIL_DEEP, FEATHER_IN, FEATHER_MID, light, ov);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0, 1.42F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(headYaw, -40F, 40F)));
        ps.mulPose(Axis.XP.rotation(-s01 * 0.5F));
        two(ps, vc, -0.15F, 0, -0.15F, 0.15F, 0.34F, 0.15F, VEIL, VEIL_SH, light, ov);                     // the hood
        two(ps, vc, -0.11F, 0.04F, 0.15F, 0.11F, 0.3F, 0.18F, MASK, MASK_SH, light, ov);                   // the mask: smooth, with one line for a mouth
        box(ps, vc, -0.05F, 0.09F + s01 * 0.01F, 0.18F, 0.05F, 0.105F + s01 * 0.05F, 0.184F, 0xFF6A4A18, light, ov);
        ps.translate(0, 0.2F, -0.2F);                                                                       // the halo, upright behind the head
        ps.mulPose(Axis.ZP.rotation(t * 0.01F));
        float r = 0.34F + s01 * 0.06F;
        box(ps, vc, -r, r - 0.04F, -0.015F, r, r, 0.015F, GOLD_HI, FULL, ov);
        box(ps, vc, -r, -r, -0.015F, r, -r + 0.04F, 0.015F, GOLD_HI, FULL, ov);
        box(ps, vc, -r, -r + 0.04F, -0.015F, -r + 0.04F, r - 0.04F, 0.015F, GOLD_HI, FULL, ov);
        box(ps, vc, r - 0.04F, -r + 0.04F, -0.015F, r, r - 0.04F, 0.015F, GOLD_HI, FULL, ov);
        for (int i = 0; i < 4; i++) {                                                                       // four points on it
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.HALF_PI + Mth.PI / 4));
            box(ps, vc, -0.02F, r * 1.3F, -0.012F, 0.02F, r * 1.3F + 0.12F, 0.012F, GOLD, FULL, ov);
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

    // ------------------------------------------------------------------ Threshold Warden
    static final int ST = 0xFFE4E0D8, ST_SH = 0xFFC4C0B8, ST_DEEP = 0xFFA09C96, ST_DARK = 0xFF7A7672, SEAM = 0xFFFFD870, SEAM_OFF = 0xFFB89C5C, BLADE = 0xFFF4F4FF, BLADE_SH = 0xFFC8CCE0;

    /**
     * Marble in armour, three blocks tall: greaves, a broad cuirass with a golden seam, heavy shoulders, a helm with
     * one slit, two stone wings folded on its back and a sword as long as a man. {@code k} is 0 kneeling (one knee
     * down, both hands on the sword stood point down before it, head bowed) and 1 standing awake, when the seams and
     * the slit light and the wings come off its back.
     */
    static void warden(PoseStack ps, VertexConsumer vc, VertexConsumer tr, float t, float limb, float amt, float k, int strike, float partial, float headYaw, int light, int ov) {
        float e = k * k * (3 - 2 * k);
        int seam = e > 0.5F ? SEAM : SEAM_OFF, seamLight = e > 0.5F ? FULL : light;
        float drop = (1 - e) * 0.75F;
        // legs
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.26F, 1.45F - drop, 0);
            float walk = Mth.cos(limb * 0.5F + (s > 0 ? 0 : Mth.PI)) * 0.6F * amt * e;
            float thigh = s < 0 ? Mth.lerp(e, -1.45F, 0) : Mth.lerp(e, 0.35F, 0);                           // left knee forward and up, right knee to the ground
            ps.mulPose(Axis.XP.rotation(thigh + walk));
            two(ps, vc, -0.16F, -0.72F, -0.17F, 0.16F, 0.0F, 0.17F, ST, ST_SH, light, ov);
            box(ps, vc, -0.18F, -0.74F, -0.19F, 0.18F, -0.6F, 0.19F, ST_DEEP, light, ov);                   // the knee
            ps.translate(0, -0.72F, 0);
            ps.mulPose(Axis.XP.rotation(s < 0 ? Mth.lerp(e, 1.45F, 0) : Mth.lerp(e, 1.2F, 0)));
            two(ps, vc, -0.15F, -0.66F, -0.16F, 0.15F, 0.0F, 0.16F, ST_SH, ST_DEEP, light, ov);             // greave
            box(ps, vc, -0.02F, -0.6F, 0.16F, 0.02F, -0.05F, 0.175F, seam, seamLight, ov);
            box(ps, vc, -0.17F, -0.73F, -0.18F, 0.17F, -0.62F, 0.3F, ST_DARK, light, ov);                   // foot
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0, 1.45F - drop, 0);
        ps.mulPose(Axis.XP.rotation((1 - e) * 0.22F));
        box(ps, vc, -0.42F, -0.1F, -0.24F, 0.42F, 0.2F, 0.24F, ST_DEEP, light, ov);                         // the belt and tassets
        for (int i = -2; i <= 2; i++) box(ps, vc, i * 0.17F - 0.07F, -0.42F, 0.2F, i * 0.17F + 0.07F, -0.08F, 0.27F, i % 2 == 0 ? ST_SH : ST, light, ov);
        two(ps, vc, -0.5F, 0.2F, -0.28F, 0.5F, 1.1F, 0.28F, ST, ST_SH, light, ov);                          // cuirass
        box(ps, vc, -0.03F, 0.25F, 0.28F, 0.03F, 1.05F, 0.3F, seam, seamLight, ov);                         // the seam down it
        box(ps, vc, -0.3F, 0.82F, 0.28F, 0.3F, 0.88F, 0.3F, seam, seamLight, ov);
        box(ps, vc, -0.12F, 0.78F, 0.29F, 0.12F, 0.92F, 0.32F, e > 0.5F ? GOLD_HI : ST_DEEP, seamLight, ov);
        // the wings: slabs of stone feather, flat to the back until it wakes
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.2F, 0.95F, -0.28F);
            ps.mulPose(Axis.YP.rotation(s * (-1.2F + e * 0.9F + Mth.sin(t * 0.05F) * 0.04F * e)));
            ps.mulPose(Axis.ZP.rotation(s * (-0.5F + e * 0.35F)));
            for (int i = 0; i < 4; i++)
                box(ps, vc, s * i * 0.28F, -1.3F + i * 0.2F, -0.05F, s * (i * 0.28F + 0.27F), 0.15F - i * 0.05F, 0.03F, i % 2 == 0 ? ST_SH : ST_DEEP, light, ov);
            ps.popPose();
        }
        // arms and the sword
        float swing = strike > 0 ? Mth.sin(Mth.clamp((12 - (strike - partial)) / 12F, 0F, 1F) * Mth.PI) : 0;
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();
            ps.translate(s * 0.62F, 0.98F, 0);
            box(ps, vc, -0.24F, -0.08F, -0.26F, 0.24F, 0.26F, 0.26F, ST_SH, light, ov);                     // pauldron
            box(ps, vc, -0.26F, 0.1F, -0.28F, 0.26F, 0.16F, 0.28F, seam, seamLight, ov);
            float rest = Mth.lerp(e, -0.95F, s > 0 ? -0.25F : 0.1F);                                        // kneeling: both reach forward to the hilt
            float walkA = Mth.cos(limb * 0.5F + (s > 0 ? Mth.PI : 0)) * 0.35F * amt * e;
            ps.mulPose(Axis.XP.rotation(rest + walkA - (s > 0 ? swing * 2.0F : 0)));
            ps.mulPose(Axis.ZP.rotation(-s * (1 - e) * 0.5F));
            two(ps, vc, -0.14F, -0.6F, -0.15F, 0.14F, 0.0F, 0.15F, ST, ST_SH, light, ov);
            ps.translate(0, -0.6F, 0);
            ps.mulPose(Axis.XP.rotation(Mth.lerp(e, -0.5F, -0.3F)));
            two(ps, vc, -0.13F, -0.55F, -0.14F, 0.13F, 0.0F, 0.14F, ST_SH, ST_DEEP, light, ov);             // gauntlet
            if (s > 0 && e > 0.5F) {                                                                        // awake: the sword is in the right hand
                ps.translate(0, -0.5F, 0.05F);
                ps.mulPose(Axis.XP.rotation(1.9F));                 // held low, point forward and a little up
                sword(ps, vc, seam, seamLight, light, ov);
            }
            ps.popPose();
        }
        // head
        ps.pushPose();
        ps.translate(0, 1.12F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(headYaw, -50F, 50F) * e));
        ps.mulPose(Axis.XP.rotation((1 - e) * 0.55F));
        box(ps, vc, -0.14F, 0.0F, -0.14F, 0.14F, 0.1F, 0.14F, ST_DEEP, light, ov);                          // neck
        two(ps, vc, -0.24F, 0.08F, -0.25F, 0.24F, 0.56F, 0.25F, ST, ST_SH, light, ov);                     // helm
        box(ps, vc, -0.03F, 0.5F, -0.27F, 0.03F, 0.72F, 0.2F, ST_SH, light, ov);                            // crest
        box(ps, vc, -0.18F, 0.3F, 0.25F, 0.18F, 0.36F, 0.265F, e > 0.5F ? GOLD_HI : ST_DARK, seamLight, ov);   // the slit
        box(ps, vc, -0.025F, 0.1F, 0.25F, 0.025F, 0.3F, 0.265F, ST_DARK, light, ov);
        ps.popPose();
        ps.popPose();
        if (e <= 0.5F) {                                                                                    // kneeling: the sword stands before it, point down
            ps.pushPose();
            ps.translate(0, 2.0F - drop * 0.4F, 0.62F);
            ps.mulPose(Axis.XP.rotation(Mth.PI));
            sword(ps, vc, seam, seamLight, light, ov);
            ps.popPose();
        }
    }

    /** The Warden's sword, pommel at the origin, blade along +y. */
    static void sword(PoseStack ps, VertexConsumer vc, int seam, int seamLight, int light, int ov) {
        box(ps, vc, -0.05F, -0.1F, -0.05F, 0.05F, 0.0F, 0.05F, seam, seamLight, ov);                        // pommel
        box(ps, vc, -0.035F, 0.0F, -0.035F, 0.035F, 0.35F, 0.035F, ST_DARK, light, ov);                     // grip
        box(ps, vc, -0.3F, 0.35F, -0.05F, 0.3F, 0.43F, 0.05F, seam, seamLight, ov);                         // guard
        box(ps, vc, -0.09F, 0.43F, -0.025F, 0.09F, 1.75F, 0.025F, BLADE, light, ov);
        box(ps, vc, -0.02F, 0.43F, -0.03F, 0.02F, 1.6F, 0.03F, BLADE_SH, light, ov);                        // fuller
        box(ps, vc, -0.055F, 1.75F, -0.02F, 0.055F, 1.9F, 0.02F, BLADE, light, ov);                         // tip
        box(ps, vc, -0.02F, 1.9F, -0.015F, 0.02F, 1.98F, 0.015F, BLADE, light, ov);
    }

    private HomeMobRenderers() {}
}
