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
            // far off, the angels draw fewer feathers and plainer eyes (they are hundreds of parts each up close)
            FAR = this.entityRenderDispatcher.distanceToSqr(e) > 24 * 24;
            ps.pushPose();
            // (the wheels "went in any of their four directions without turning as they went": the Ophan is never turned to face its way)
            if (!id.equals("threshold_warden")) ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!DeathAnim.apply(ps, e, partial, id)) {
                FAR = false;
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
            FAR = false;
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

    /** As {@link #two}, but eight-sided (see {@link #rbox}). */
    static void rtwo(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int top, int bot, int light, int ov) {
        float ym = y0 + (y1 - y0) * 0.35F;
        rbox(ps, vc, x0, y0, z0, x1, ym, z1, bot, light, ov);
        rbox(ps, vc, x0, ym, z0, x1, y1, z1, top, light, ov);
    }

    /** A halo lying flat at height y: a square ring of four bars, always at full brightness. */
    static void halo(PoseStack ps, VertexConsumer vc, float y, float r, float th, int argb, int ov) {
        box(ps, vc, -r, y, -r, r, y + th, -r + th, argb, FULL, ov);
        box(ps, vc, -r, y, r - th, r, y + th, r, argb, FULL, ov);
        box(ps, vc, -r, y, -r + th, -r + th, y + th, r - th, argb, FULL, ov);
        box(ps, vc, r - th, y, -r + th, r, y + th, r - th, argb, FULL, ov);
    }

    /** Set per creature by the renderer: past 24 blocks, wings and eyes are drawn with fewer parts. */
    static boolean FAR = false;

    /** A colour darkened (f < 1) or lightened (f > 1), alpha kept. */
    static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 255) * f)), g = Math.min(255, (int) (((argb >> 8) & 255) * f)), b = Math.min(255, (int) ((argb & 255) * f));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** A box with its four vertical edges cut back: an eight-sided prism, so limbs and bodies read round rather than square. */
    static void rbox(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light, int ov) {
        float lx = Math.min(x0, x1), hx = Math.max(x0, x1), lz = Math.min(z0, z1), hz = Math.max(z0, z1);
        float c = Math.min(hx - lx, hz - lz) * 0.2F;
        box(ps, vc, lx + c, y0, lz, hx - c, y1, hz, argb, light, ov);
        box(ps, vc, lx, y0, lz + c, hx, y1, hz - c, argb, light, ov);
    }

    /** A rounded lump of radius r about (x, y, z): three crossed slabs and a core, so it is round from every side. */
    static void ball(PoseStack ps, VertexConsumer vc, float x, float y, float z, float r, int argb, int light, int ov) {
        float k = r * 0.62F;
        box(ps, vc, x - k, y - r, z - k, x + k, y + r, z + k, shade(argb, 1.05F), light, ov);
        box(ps, vc, x - r, y - k, z - k, x + r, y + k, z + k, argb, light, ov);
        box(ps, vc, x - k, y - k, z - r, x + k, y + k, z + r, argb, light, ov);
        float m = r * 0.85F;
        box(ps, vc, x - m, y - m, z - m, x + m, y + m, z + m, shade(argb, 0.92F), light, ov);
    }

    /**
     * One feather hanging from the origin down -y: a vane that narrows in three steps to its tip, with thickness, and
     * a raised quill down its middle.
     */
    static void feather(PoseStack ps, VertexConsumer vc, float len, float w, float t, int col, int light, int ov) {
        if (FAR) {
            box(ps, vc, -w * 0.45F, -len * 0.9F, -t, w * 0.45F, 0, t, col, light, ov);
            return;
        }
        box(ps, vc, -w * 0.5F, -len * 0.5F, -t, w * 0.5F, 0, t, col, light, ov);
        box(ps, vc, -w * 0.4F, -len * 0.82F, -t * 0.8F, w * 0.4F, -len * 0.5F, t * 0.8F, shade(col, 1.04F), light, ov);
        box(ps, vc, -w * 0.22F, -len, -t * 0.6F, w * 0.22F, -len * 0.82F, t * 0.6F, shade(col, 1.08F), light, ov);
        box(ps, vc, -w * 0.07F, -len * 0.92F, -t * 1.3F, w * 0.07F, 0, t * 1.3F, shade(col, 0.86F), light, ov);
    }

    /** How thick (half) the root of a wing of this span is. */
    static float wingTh(float span) {
        return 0.035F + span * 0.022F;
    }

    /**
     * A feathered wing hinged at the origin, reaching out along +x (mirror with {@code side}) and hanging down -y,
     * built with depth: a thick round leading edge (the bone), two overlapping rows of coverts that thin toward the
     * trailing edge, a row of secondaries each tilted a little in or out so they overlap, and at the tip a fan of
     * primaries that spread outward one after another. The outer part sweeps back a little (camber), so the wing is
     * cupped rather than flat.
     */
    static void wing(PoseStack ps, VertexConsumer vc, float side, float span, float depth, float angle, float fold, int inner, int mid, int tip, int light, int ov) {
        float th = wingTh(span);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(side * angle));
        // the inner part: bone, coverts, secondaries
        float l1 = span * 0.4F;
        rbox(ps, vc, 0, -0.05F, -th, side * l1, 0.06F, th, shade(inner, 1.06F), light, ov);
        box(ps, vc, 0, -depth * 0.3F, -th * 0.8F, side * l1, -0.04F, th * 0.8F, inner, light, ov);
        box(ps, vc, side * 0.02F, -depth * 0.5F, -th * 0.6F, side * l1 * 0.96F, -depth * 0.28F, th * 0.6F, shade(inner, 0.95F), light, ov);
        int n1 = FAR ? 2 : 4;
        for (int i = 0; i < n1; i++) {
            ps.pushPose();
            ps.translate(side * l1 * (i + 0.5F) / n1, -depth * 0.42F, (i % 2 == 0 ? 1 : -1) * th * 0.15F);
            ps.mulPose(Axis.XP.rotation((i % 2 == 0 ? 1 : -1) * 0.07F));
            ps.mulPose(Axis.ZP.rotation(side * 0.06F * i));
            feather(ps, vc, depth * (0.5F + 0.04F * i), l1 / n1 * 1.25F, th * 0.42F, mid, light, ov);
            ps.popPose();
        }
        // the outer part: bent at the wrist, swept back
        ps.translate(side * l1, 0, 0);
        ps.mulPose(Axis.ZP.rotation(side * fold));
        ps.mulPose(Axis.YP.rotation(side * 0.18F));
        float l2 = span * 0.35F;
        rbox(ps, vc, 0, -0.04F, -th * 0.8F, side * l2, 0.05F, th * 0.8F, shade(mid, 1.08F), light, ov);
        box(ps, vc, 0, -depth * 0.32F, -th * 0.65F, side * l2, -0.03F, th * 0.65F, mid, light, ov);
        int n2 = FAR ? 2 : 4;
        for (int i = 0; i < n2; i++) {
            ps.pushPose();
            ps.translate(side * l2 * (i + 0.5F) / n2, -depth * 0.25F, (i % 2 == 0 ? -1 : 1) * th * 0.15F);
            ps.mulPose(Axis.XP.rotation((i % 2 == 0 ? -1 : 1) * 0.07F));
            ps.mulPose(Axis.ZP.rotation(side * (0.1F + 0.07F * i)));
            feather(ps, vc, depth * (0.6F + 0.06F * i), l2 / n2 * 1.3F, th * 0.36F, shade(mid, 1.03F), light, ov);
            ps.popPose();
        }
        // the primaries: a fan at the tip, each turned further out and swept further back than the last
        ps.translate(side * l2, 0, 0);
        ps.mulPose(Axis.ZP.rotation(side * fold * 0.7F));
        ps.mulPose(Axis.YP.rotation(side * 0.12F));
        rbox(ps, vc, 0, -0.035F, -th * 0.6F, side * span * 0.12F, 0.04F, th * 0.6F, shade(tip, 0.94F), light, ov);
        int n3 = FAR ? 3 : 5;
        for (int i = 0; i < n3; i++) {
            ps.pushPose();
            ps.translate(side * span * 0.025F * i, 0.0F, -th * 0.12F * i);
            ps.mulPose(Axis.ZP.rotation(side * (0.35F + 0.2F * i)));
            ps.mulPose(Axis.YP.rotation(side * 0.06F * i));
            feather(ps, vc, depth * (0.75F + 0.08F * i), span * 0.085F, th * 0.32F, i % 2 == 0 ? tip : shade(tip, 0.95F), light, ov);
            ps.popPose();
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

    /**
     * An eye on a surface facing +z at (x, y, z), standing out from it: a rim of lid, the white swelling outward, the
     * iris a dome on the white, the pupil on the iris, and a glint. Shut, it is a closed swelling of lid with the
     * line of the lashes across it.
     */
    static void eye(PoseStack ps, VertexConsumer vc, float x, float y, float z, float r, int iris, boolean open, int light, int ov) {
        float d = r * 0.4F;
        if (!open) {
            box(ps, vc, x - r * 1.1F, y - r * 0.7F, z, x + r * 1.1F, y + r * 0.7F, z + d * 0.6F, shade(LID, 0.9F), light, ov);
            box(ps, vc, x - r * 0.9F, y - r * 0.5F, z + d * 0.6F, x + r * 0.9F, y + r * 0.5F, z + d, LID, light, ov);
            box(ps, vc, x - r, y - r * 0.08F, z + d, x + r, y + r * 0.08F, z + d * 1.15F, 0xFF3A2A14, light, ov);
            return;
        }
        if (FAR) {
            box(ps, vc, x - r, y - r * 0.6F, z, x + r, y + r * 0.6F, z + d, EYE_WHITE, FULL, ov);
            box(ps, vc, x - r * 0.45F, y - r * 0.45F, z + d, x + r * 0.45F, y + r * 0.45F, z + d * 1.4F, iris, FULL, ov);
            return;
        }
        box(ps, vc, x - r * 1.15F, y - r * 0.78F, z, x + r * 1.15F, y + r * 0.78F, z + d * 0.45F, LID, light, ov);
        box(ps, vc, x - r, y - r * 0.6F, z + d * 0.2F, x + r, y + r * 0.6F, z + d, EYE_WHITE, FULL, ov);
        box(ps, vc, x - r * 0.75F, y - r * 0.45F, z + d, x + r * 0.75F, y + r * 0.45F, z + d * 1.2F, EYE_WHITE, FULL, ov);
        box(ps, vc, x - r * 0.48F, y - r * 0.5F, z + d * 0.95F, x + r * 0.48F, y + r * 0.5F, z + d * 1.45F, iris, FULL, ov);
        box(ps, vc, x - r * 0.2F, y - r * 0.3F, z + d * 1.45F, x + r * 0.2F, y + r * 0.3F, z + d * 1.62F, PUPIL, light, ov);
        box(ps, vc, x - r * 0.4F, y + r * 0.16F, z + d * 1.45F, x - r * 0.24F, y + r * 0.32F, z + d * 1.66F, 0xFFFFFFFF, FULL, ov);
    }

    /** A wing as {@link #wing}, with eyes opened along it on both faces ("full of eyes all round, even their wings"). */
    static void eyedWing(PoseStack ps, VertexConsumer vc, float side, float span, float depth, float angle, float fold, int inner, int mid, int tip, int iris, int eyes,
                         int light, int ov) {
        wing(ps, vc, side, span, depth, angle, fold, inner, mid, tip, light, ov);
        float face = wingTh(span) * 0.8F;
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation(side * angle));
        for (int i = 0; i < eyes; i++) {
            float x = side * span * (0.07F + 0.29F * i / Math.max(1, eyes - 1));
            float y = -depth * (0.17F + (i % 2) * 0.12F);
            eye(ps, vc, x, y, face, span * 0.036F, iris, true, light, ov);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(Mth.PI));                                                           // and on the other face of the wing
            eye(ps, vc, -x, y, face, span * 0.036F, iris, true, light, ov);
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
            rtwo(ps, vc, s * 0.13F - 0.075F, 0.1F, -0.075F, s * 0.13F + 0.075F, 0.62F, 0.075F, BRONZE, BRONZE_SH, light, ov);
            rbox(ps, vc, s * 0.13F - 0.085F, 0.36F, -0.085F, s * 0.13F + 0.085F, 0.46F, 0.085F, BRONZE_HI, light, ov);       // the knee
            rbox(ps, vc, s * 0.13F - 0.085F, 0.0F, -0.085F, s * 0.13F + 0.085F, 0.1F, 0.1F, BRONZE_DEEP, light, ov);
            rbox(ps, vc, s * 0.13F - 0.07F, 0.0F, 0.06F, s * 0.13F + 0.07F, 0.06F, 0.13F, BRONZE_DEEP, light, ov);          // the toe of the hoof
            box(ps, vc, s * 0.13F - 0.012F, 0.0F, 0.1F, s * 0.13F + 0.012F, 0.07F, 0.134F, 0xFF3A2410, light, ov);     // the cleft of the hoof
            eye(ps, vc, s * 0.13F, 0.26F, 0.075F, 0.035F, IRIS, true, light, ov);
        }
        rtwo(ps, vc, -0.22F, 0.6F, -0.14F, 0.22F, 1.0F, 0.14F, BRONZE, BRONZE_SH, light, ov);              // the body: a waist,
        rbox(ps, vc, -0.26F, 0.92F, -0.16F, 0.26F, 1.2F, 0.16F, BRONZE, light, ov);                          // a broad chest,
        rbox(ps, vc, -0.31F, 1.08F, -0.13F, 0.31F, 1.2F, 0.13F, BRONZE_HI, light, ov);                       // shoulders
        rbox(ps, vc, -0.18F, 0.95F, 0.12F, 0.18F, 1.15F, 0.18F, shade(BRONZE, 1.08F), light, ov);            // and the swell of the breast
        rbox(ps, vc, -0.24F, 0.6F, -0.16F, 0.24F, 0.68F, 0.16F, BRONZE_DEEP, light, ov);               // a girdle
                eye(ps, vc, 0, 1.05F, 0.18F, 0.07F, IRIS_GOLD, true, light, ov);                                    // eyes over the body, before
        eye(ps, vc, -0.12F, 0.8F, 0.14F, 0.045F, IRIS, true, light, ov);
        eye(ps, vc, 0.12F, 0.8F, 0.14F, 0.045F, IRIS, true, light, ov);
        ps.pushPose();                                                                                      // and behind
        ps.mulPose(Axis.YP.rotation(Mth.PI));
        eye(ps, vc, 0, 1.0F, 0.16F, 0.06F, IRIS, true, light, ov);
        eye(ps, vc, -0.12F, 0.78F, 0.14F, 0.04F, IRIS_GOLD, true, light, ov);
        eye(ps, vc, 0.12F, 0.78F, 0.14F, 0.04F, IRIS_GOLD, true, light, ov);
        ps.popPose();
        for (int s = -1; s <= 1; s += 2) {
            ps.pushPose();                                                                                  // a man's hand under the wing, an eye in its back
            ps.translate(s * 0.27F, 1.08F, 0.02F);
            ps.mulPose(Axis.ZP.rotation(s * 0.2F));
            ps.mulPose(Axis.XP.rotation(-0.35F + Mth.sin(t * 0.07F + s) * 0.08F));
            rtwo(ps, vc, -0.055F, -0.42F, -0.055F, 0.055F, 0.0F, 0.055F, BRONZE, BRONZE_SH, light, ov);
            rbox(ps, vc, -0.065F, -0.24F, -0.065F, 0.065F, -0.18F, 0.065F, BRONZE_HI, light, ov);              // the elbow
            rbox(ps, vc, -0.055F, -0.52F, -0.045F, 0.055F, -0.42F, 0.045F, MAN, light, ov);
            for (int f = 0; f < 4; f++)                                                                     // fingers
                box(ps, vc, -0.05F + f * 0.027F, -0.6F, -0.012F, -0.033F + f * 0.027F, -0.52F, 0.022F, MAN_SH, light, ov);
            box(ps, vc, s * 0.05F, -0.55F, -0.02F, s * 0.075F, -0.47F, 0.02F, MAN_SH, light, ov);                     // thumb
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
        rbox(ps, vc, -0.09F, 0.0F, -0.09F, 0.09F, 0.07F, 0.09F, BRONZE_DEEP, light, ov);
        ps.translate(0, 0.05F, 0);
        float h = 0.19F;
        rbox(ps, vc, -h, 0, -h, h, 2 * h, h, BRONZE_SH, light, ov);
        rbox(ps, vc, -h * 0.8F, 2 * h - 0.01F, -h * 0.8F, h * 0.8F, 2 * h + 0.05F, h * 0.8F, BRONZE_DEEP, light, ov);   // the crown of the head
        // a man, in front
        two(ps, vc, -h + 0.02F, 0.02F, h, h - 0.02F, 2 * h - 0.02F, h + 0.03F, MAN, MAN_SH, light, ov);
        for (int s = -1; s <= 1; s += 2) eye(ps, vc, s * 0.075F, 0.23F, h + 0.03F, 0.035F, IRIS, true, light, ov);
        box(ps, vc, -0.025F, 0.12F, h + 0.03F, 0.025F, 0.22F, h + 0.08F, MAN_SH, light, ov);                // nose
        box(ps, vc, -0.13F, 0.27F, h + 0.03F, 0.13F, 0.3F, h + 0.06F, MAN_SH, light, ov);                   // brow
        box(ps, vc, -0.07F, 0.02F, h + 0.03F, 0.07F, 0.07F, h + 0.05F, MAN_SH, light, ov);                  // chin
        box(ps, vc, -0.17F, 0.3F, h - 0.02F, 0.17F, 0.42F, h + 0.07F, BRONZE_DEEP, light, ov);              // a fringe of hair
        box(ps, vc, -0.05F, 0.075F, h + 0.03F, 0.05F, 0.095F, h + 0.055F, 0xFFA06850, light, ov);
                // a lion, on its right (-x is the creature's right when it faces +z)
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(-Mth.HALF_PI));
        box(ps, vc, -h, 0.0F, h, h, 2 * h, h + 0.03F, LION_MANE, light, ov);                                 // the mane, all round,
        for (int m = 0; m < 10; m++) {                                                                      // in tufts that stand out
            float ang = m * Mth.TWO_PI / 10, mx = Mth.cos(ang) * 0.15F, my = 0.19F + Mth.sin(ang) * 0.15F;
            rbox(ps, vc, mx - 0.05F, my - 0.05F, h - 0.02F, mx + 0.05F, my + 0.05F, h + 0.07F + (m % 2) * 0.03F, m % 2 == 0 ? LION_MANE : shade(LION_MANE, 0.85F), light, ov);
        }
        two(ps, vc, -0.12F, 0.05F, h + 0.03F, 0.12F, 0.31F, h + 0.06F, LION, LION_MANE, light, ov);
        rbox(ps, vc, -0.08F, 0.05F, h + 0.05F, 0.08F, 0.18F, h + 0.15F, 0xFFF0D090, light, ov);               // muzzle
        box(ps, vc, -0.06F, 0.02F, h + 0.05F, 0.06F, 0.06F, h + 0.12F, 0xFFE0B878, light, ov);              // jaw
        box(ps, vc, -0.035F, 0.14F, h + 0.15F, 0.035F, 0.18F, h + 0.17F, 0xFF4A2A18, light, ov);   // nose
        for (int s = -1; s <= 1; s += 2) {
            eye(ps, vc, s * 0.075F, 0.24F, h + 0.06F, 0.03F, IRIS_GOLD, true, light, ov);
            rbox(ps, vc, s * 0.12F - 0.035F, 0.36F, h + 0.02F, s * 0.12F + 0.035F, 0.43F, h + 0.08F, LION, light, ov);       // ear
        }
        ps.popPose();
        // an ox, on its left
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.HALF_PI));
        two(ps, vc, -0.15F, 0.02F, h, 0.15F, 0.36F, h + 0.04F, OX, OX_SH, light, ov);
        rbox(ps, vc, -0.1F, 0.02F, h + 0.03F, 0.1F, 0.17F, h + 0.16F, 0xFFC8A088, light, ov);                // muzzle, long
        box(ps, vc, -0.12F, 0.17F, h + 0.03F, 0.12F, 0.3F, h + 0.07F, OX, light, ov);                       // the brow
        for (int s = -1; s <= 1; s += 2) {
            box(ps, vc, s * 0.045F - 0.016F, 0.07F, h + 0.16F, s * 0.045F + 0.016F, 0.11F, h + 0.17F, 0xFF3A2018, light, ov);
            eye(ps, vc, s * 0.1F, 0.23F, h + 0.04F, 0.03F, 0xFF4A3020, true, light, ov);
            rbox(ps, vc, s * 0.14F, 0.29F, h - 0.04F, s * 0.27F, 0.37F, h + 0.04F, HORN, light, ov);         // horns, out and then up
            rbox(ps, vc, s * 0.22F, 0.36F, h - 0.03F, s * 0.29F, 0.47F, h + 0.03F, HORN, light, ov);
            rbox(ps, vc, s * 0.235F, 0.47F, h - 0.02F, s * 0.275F, 0.54F, h + 0.02F, shade(HORN, 0.9F), light, ov);
        }
        ps.popPose();
        // an eagle, behind
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.PI));
        two(ps, vc, -0.14F, 0.04F, h, 0.14F, 0.36F, h + 0.04F, EAGLE, 0xFFE0D8C8, light, ov);
        box(ps, vc, -0.15F, 0.0F, h, 0.15F, 0.08F, h + 0.03F, EAGLE_SH, light, ov);                          // the dark feathers of the neck
        rbox(ps, vc, -0.045F, 0.12F, h + 0.04F, 0.045F, 0.23F, h + 0.12F, BEAK, light, ov);                  // the beak, hooked:
        rbox(ps, vc, -0.035F, 0.1F, h + 0.12F, 0.035F, 0.21F, h + 0.18F, shade(BEAK, 1.04F), light, ov);
        box(ps, vc, -0.025F, 0.06F, h + 0.16F, 0.025F, 0.12F, h + 0.21F, 0xFFD89010, light, ov);             // the hook
        for (int m = 0; m < 6; m++)                                                                         // feathers of the crown
            rbox(ps, vc, -0.14F + m * 0.05F, 0.33F, h - 0.03F, -0.1F + m * 0.05F, 0.42F + (m % 2) * 0.03F, h + 0.04F, m % 2 == 0 ? EAGLE : 0xFFE8E0D0, light, ov);
        for (int s = -1; s <= 1; s += 2) {
            eye(ps, vc, s * 0.085F, 0.25F, h + 0.04F, 0.03F, IRIS_GOLD, true, light, ov);
            box(ps, vc, s * 0.085F - 0.05F, 0.28F, h + 0.04F, s * 0.085F + 0.05F, 0.315F, h + 0.085F, EAGLE_SH, light, ov);      // the brow, jutting
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
        // what is behind the wings: a body of fire, round and widening to the shoulders, with tongues of flame
        // licking up its sides, and where a face would be, a light too bright to look at
        rbox(ps, vc, -0.06F, 0.0F, -0.06F, 0.06F, 0.35F, 0.06F, FIRE_MID, FULL, ov);
        rbox(ps, vc, -0.12F, 0.3F, -0.1F, 0.12F, 0.7F, 0.1F, FIRE, FULL, ov);
        rbox(ps, vc, -0.17F, 0.65F, -0.13F, 0.17F, 1.25F, 0.13F, FIRE_HI, FULL, ov);
        rbox(ps, vc, -0.22F, 1.0F, -0.12F, 0.22F, 1.2F, 0.12F, FIRE_HI, FULL, ov);
        ball(ps, vc, 0, 1.4F, 0, 0.15F, FIRE_CORE, FULL, ov);
        for (int i = 0; i < 8; i++) {
            float ang = i * Mth.TWO_PI / 8 + t * 0.02F, fx = Mth.cos(ang) * 0.15F, fz = Mth.sin(ang) * 0.11F;
            float hgt = 0.22F + 0.1F * Mth.sin(t * 0.3F + i * 1.7F);
            float y0 = 0.45F + (i % 3) * 0.22F;
            rbox(ps, vc, fx - 0.04F, y0, fz - 0.04F, fx + 0.04F, y0 + hgt, fz + 0.04F, FIRE, FULL, ov);
            rbox(ps, vc, fx * 1.1F - 0.025F, y0 + hgt, fz * 1.1F - 0.025F, fx * 1.1F + 0.025F, y0 + hgt * 1.5F, fz * 1.1F + 0.025F, FIRE_HI, FULL, ov);
        }
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
        // a wheel of flame behind it, turning: a thick ring with a bright inner band, and tongues of fire standing
        // out from its rim
        ps.pushPose();
        ps.translate(0, 1.4F, -0.26F);
        ps.mulPose(Axis.ZP.rotation(t * 0.015F));
        float r = 0.42F + s01 * 0.1F;
        int n = 20;
        float half = r * Mth.sin(Mth.PI / n) * 1.08F;
        for (int i = 0; i < n; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.TWO_PI / n));
            box(ps, vc, -half, r - 0.06F, -0.05F, half, r, 0.05F, i % 2 == 0 ? FIRE : FIRE_MID, FULL, ov);
            box(ps, vc, -half, r - 0.08F, -0.03F, half, r - 0.05F, 0.03F, FIRE_CORE, FULL, ov);
            if (i % 2 == 0) {
                float hgt = 0.08F + 0.05F * Mth.sin(t * 0.25F + i) + s01 * 0.08F;
                rbox(ps, vc, -0.035F, r, -0.035F, 0.035F, r + hgt, 0.035F, FIRE_MID, FULL, ov);
                rbox(ps, vc, -0.02F, r + hgt, -0.02F, 0.02F, r + hgt * 1.6F, 0.02F, FIRE_HI, FULL, ov);
            }
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
        int n = 24;
        float half = r * Mth.sin(Mth.PI / n) * 1.06F;
        for (int i = 0; i < n; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotation(i * Mth.TWO_PI / n));
            int col = i % 2 == 0 ? a : b;
            box(ps, vc, -half, r - th, -w, half, r - th * 0.15F, w, col, light, ov);                              // the rim
            box(ps, vc, -half, r - th * 0.15F, -w * 0.6F, half, r, w * 0.6F, shade(col, 1.12F), light, ov);       // a raised tread round it
            box(ps, vc, -half, r - th * 1.12F, -w * 0.7F, half, r - th, w * 0.7F, shade(col, 0.8F), light, ov);   // and a lip inside
            if (i % 3 == 0) {
                eye(ps, vc, 0, r - th * 0.55F, w, th * 0.36F, iris, open, light, ov);                          // on this side of the rim
                ps.pushPose();
                ps.mulPose(Axis.YP.rotation(Mth.PI));
                eye(ps, vc, 0, r - th * 0.55F, w, th * 0.36F, iris, open, light, ov);                          // and the other
                ps.popPose();
            } else if (i % 3 == 1) {
                ps.pushPose();                                                                              // and looking outward from the rim itself
                ps.translate(0, r, 0);
                ps.mulPose(Axis.XP.rotation(-Mth.HALF_PI));
                eye(ps, vc, 0, 0, 0, w * 0.5F, iris, open, light, ov);
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
        // the great eye at the middle: a ball, lidded on both faces
        ball(ps, vc, 0, 0, 0, 0.3F, b, light, ov);
        rbox(ps, vc, -0.33F, -0.06F, -0.33F, 0.33F, 0.06F, 0.33F, c, light, ov);                          // a band round it
        for (int side = 0; side < 2; side++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotation(side * Mth.PI));
            eye(ps, vc, 0, 0, 0.27F, 0.17F, IRIS_GOLD, open, light, ov);
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
        wheel(ps, vc, 1.15F, 0.1F, 0.17F, a, b, IRIS, open, light, ov);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(Mth.HALF_PI * e + ph * 1.3F));
        ps.mulPose(Axis.ZP.rotation(-ph * 0.7F));
        wheel(ps, vc, 0.92F, 0.09F, 0.15F, a, b, IRIS_GOLD, open, light, ov);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotation(Mth.HALF_PI * e * 0.8F + ph * 1.7F));
        ps.mulPose(Axis.ZP.rotation(ph * 0.9F));
        wheel(ps, vc, 0.7F, 0.08F, 0.13F, b, c, IRIS, open, light, ov);
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
