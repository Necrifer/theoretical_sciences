package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.math.EuclidsAutomaton;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** 1.2.0: procedural models for the Mathematics branch's creatures. */
public final class MathMobRenderers {
    private static final int FULL = 0xF000F0;
    private static final int NO = OverlayTexture.NO_OVERLAY;
    // two-tone materials: the lit top of a part, and its shaded foot
    static final int[] MARBLE = {0xFFF4EFE4, 0xFFC9C1B1}, MARBLE_WARM = {0xFFEDE4D2, 0xFFB9AE98}, MARBLE_VEIN = {0xFF9C9488, 0xFF8A8276},
            BRASS = {0xFFF0C868, 0xFF8A5E1C}, BRASS_DARK = {0xFFB88A3A, 0xFF5E3E12}, IRON = {0xFF7A808A, 0xFF34383E},
            GOLD_GLOW = {0xFFFFE090, 0xFFFFC850}, SLOT = {0xFF141618, 0xFF0A0B0C};

    public static class Mob<T extends LivingEntity> extends EntityRenderer<T> {
        public Mob(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.6F;
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
            Vec3 pin = null;
            if (e instanceof EuclidsAutomaton a) {
                // the construction on the ground, and its pin, in the world (not turned with the body)
                double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
                Vec3 c = a.centre();
                ps.pushPose();
                ps.translate(c.x - ex, c.y - ey, c.z - ez);
                construction(ps, SafeBuffer.of(buf, RenderType.entityTranslucentEmissive(QHEntityRenderers.WHITE)), a.start(), a.progress(), a.phase(), a.doneAge(partial));
                if (a.phase() != EuclidsAutomaton.IDLE) {
                    pinModel(ps, SafeBuffer.of(buf, RenderType.entitySolid(QHEntityRenderers.WHITE)), light, ov);
                    pin = new Vec3(c.x - ex, c.y - ey + 0.62, c.z - ez);
                }
                ps.popPose();
            }
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(QHEntityRenderers.WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(QHEntityRenderers.WHITE));
            Matrix4f world = new Matrix4f(ps.last().pose());
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));
            if (!(e instanceof EuclidsAutomaton) && !DeathAnim.apply(ps, e, partial, id)) {
                ps.popPose();
                super.render(e, yaw, partial, ps, buf, light);
                return;
            }
            if (e instanceof EuclidsAutomaton a) {
                float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
                Pose pose = new Pose(a.rouse(partial), Mth.wrapDegrees(headYaw - bodyYaw) * Mth.DEG_TO_RAD, Mth.lerp(partial, e.xRotO, e.getXRot()) * Mth.DEG_TO_RAD,
                        e.deathTime > 0 ? e.deathTime + partial : -1, e.getId());
                automaton(ps, solid, glow, t, limb, amt, e.getAttackAnim(partial), a.phase(), world, pin, pose, light, ov);
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            return QHEntityRenderers.WHITE;
        }
    }

    // ------------------------------------------------------------------ shaded boxes
    /** A box shaded from a lit top colour to a darker foot, each face lit by its own direction (so parts read as solid). */
    static void sbox(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int[] col, int light, int ov) {
        if (x0 > x1) { float q = x0; x0 = x1; x1 = q; }
        if (y0 > y1) { float q = y0; y0 = y1; y1 = q; }
        if (z0 > z1) { float q = z0; z0 = z1; z1 = q; }
        PoseStack.Pose p = ps.last();
        int top = col[0], bot = col[1];
        // up, down
        quad(p, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, top, top, top, top, 1.0F, light, ov);
        quad(p, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, bot, bot, bot, bot, 0.55F, light, ov);
        // front (-z), back (+z), left (-x), right (+x): top edge lit colour, bottom edge the foot colour
        quad(p, vc, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0, 0, -1, top, top, bot, bot, 0.9F, light, ov);
        quad(p, vc, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0, 0, 1, top, top, bot, bot, 0.72F, light, ov);
        quad(p, vc, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0, top, top, bot, bot, 0.8F, light, ov);
        quad(p, vc, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0, top, top, bot, bot, 0.8F, light, ov);
    }

    /** sbox by centre and half-sizes. */
    static void cbox(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int[] col, int light, int ov) {
        sbox(ps, vc, cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz, col, light, ov);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                             float dx, float dy, float dz, float nx, float ny, float nz, int ca, int cb, int cc, int cd, float shade, int light, int ov) {
        v(p, vc, ax, ay, az, nx, ny, nz, ca, shade, light, ov);
        v(p, vc, bx, by, bz, nx, ny, nz, cb, shade, light, ov);
        v(p, vc, cx, cy, cz, nx, ny, nz, cc, shade, light, ov);
        v(p, vc, dx, dy, dz, nx, ny, nz, cd, shade, light, ov);
    }

    private static void v(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float nx, float ny, float nz, int argb, float shade, int light, int ov) {
        vc.addVertex(p, x, y, z).setColor(((argb >> 16) & 255) / 255F * shade, ((argb >> 8) & 255) / 255F * shade, (argb & 255) / 255F * shade,
                ((argb >>> 24) & 255) / 255F).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** A brass rivet head on a face (a tiny lit cube). */
    static void rivet(PoseStack ps, VertexConsumer vc, float x, float y, float z, int light, int ov) {
        cbox(ps, vc, x, y, z, 0.018F, 0.018F, 0.012F, BRASS, light, ov);
    }

    // ------------------------------------------------------------------ the construction on the ground
    /**
     * The circle, swept clockwise from where it began as far as it has walked, then the hexagon's sides one by one (each
     * as long as the radius: Book IV, Proposition 15). It glows while drawn and for a while after, then fades.
     */
    static void construction(PoseStack ps, VertexConsumer vc, float start, float progress, int phase, float doneAge) {
        if (progress < 0) return;
        float alpha = 1F;
        if (phase == EuclidsAutomaton.IDLE) {
            if (doneAge < 0 || doneAge > EuclidsAutomaton.STAY + EuclidsAutomaton.FADE) return;
            if (doneAge > EuclidsAutomaton.STAY) alpha = 1F - (doneAge - EuclidsAutomaton.STAY) / EuclidsAutomaton.FADE;
        }
        int a = (int) (0xD0 * Mth.clamp(alpha, 0, 1));
        if (a < 4) return;
        int col = (a << 24) | 0xFFD870;
        float r = EuclidsAutomaton.RADIUS, y = 0.03F, w = 0.05F;
        int n = 72;
        float arc = Mth.clamp(progress, 0, 1);
        for (int i = 0; i < n * arc; i++) {
            float a0 = start - i * Mth.TWO_PI / n, a1 = start - Math.min((i + 1) / (float) n, arc) * Mth.TWO_PI;
            flat(ps, vc, Mth.cos(a0) * r, Mth.sin(a0) * r, Mth.cos(a1) * r, Mth.sin(a1) * r, y, w, col);
        }
        for (int k = 1; k <= 6 && progress > k; k++) {
            float part = Math.min(1, progress - k);
            float a0 = start - (k - 1) * Mth.PI / 3, a1 = start - k * Mth.PI / 3;
            float x0 = Mth.cos(a0) * r, z0 = Mth.sin(a0) * r, x1 = Mth.cos(a1) * r, z1 = Mth.sin(a1) * r;
            flat(ps, vc, x0, z0, x0 + (x1 - x0) * part, z0 + (z1 - z0) * part, y + 0.005F, w * 0.85F, col);
        }
    }

    /** A flat bar on the ground from (x0, z0) to (x1, z1), half-width w. */
    static void flat(PoseStack ps, VertexConsumer vc, float x0, float z0, float x1, float z1, float y, float w, int argb) {
        float dx = x1 - x0, dz = z1 - z0, len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1e-4F) return;
        ps.pushPose();
        ps.translate(x0, y, z0);
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-dz, dx)));
        ProcMesh.box(ps, vc, 0, -0.004F, -w, len + w * 0.5F, 0.004F, w, argb, FULL, NO);
        ps.popPose();
    }

    /** The pin driven into the ground at the circle's centre: a brass spike with a knob that holds the cord. */
    static void pinModel(PoseStack ps, VertexConsumer vc, int light, int ov) {
        cbox(ps, vc, 0, 0.3F, 0, 0.025F, 0.3F, 0.025F, BRASS, light, ov);
        cbox(ps, vc, 0, 0.62F, 0, 0.05F, 0.04F, 0.05F, BRASS_DARK, light, ov);
        cbox(ps, vc, 0, 0.02F, 0, 0.08F, 0.02F, 0.08F, IRON, light, ov);
    }

    // ------------------------------------------------------------------ the automaton
    /** A limb hanging from the current origin, swung forward by `pitch` (radians about x): a shaded box. */
    static void limb(PoseStack ps, VertexConsumer vc, float pitch, float len, float hw, float hd, int[] col, int light, int ov) {
        ps.mulPose(Axis.XP.rotation(pitch));
        sbox(ps, vc, -hw, -len, -hd, hw, 0, hd, col, light, ov);
    }

    /** Where the current origin is in the frame `root` (for joining the cord to a hand). */
    static Vector3f at(PoseStack ps, Matrix4f rootInv) {
        Vector4f v = new Matrix4f(rootInv).mul(ps.last().pose()).transform(new Vector4f(0, 0, 0, 1));
        return new Vector3f(v.x, v.y, v.z);
    }

    /** What the renderer knows of the Automaton's state: how roused it is, where its head looks, how far into its fall. */
    record Pose(float rouse, float headYaw, float headPitch, float death, int seed) {
        static final Pose CALM = new Pose(0, 0, 0, -1, 0);
    }

    static int mix(int a, int b, float u) {
        u = Mth.clamp(u, 0, 1);
        int r = (int) Mth.lerp(u, (a >> 16) & 255, (b >> 16) & 255), g = (int) Mth.lerp(u, (a >> 8) & 255, (b >> 8) & 255),
                bl = (int) Mth.lerp(u, a & 255, b & 255);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /**
     * Euclid's Automaton: a marble and brass golem about two blocks tall. Every part is shaded from a lit top to a darker
     * foot, with seams, veins in the marble, rivets on the brass and iron at the joints.
     * <ul>
     *   <li>Its walk: each leg swings from the hip, the knee bending to lift the foot clear as it comes forward and
     *   straightening as it plants; the planted foot stays flat; the body dips between steps; the arms swing against
     *   the legs.</li>
     *   <li>Its work: while it walks the circle its left hand holds the cord from the pin, taut, and its right holds the
     *   straightedge down to the ground ahead; otherwise it carries the pin in its left hand.</li>
     *   <li>Roused (it has been struck): its eyes and the core behind its chest window burn red, the gear behind the
     *   window races, the vents on its shoulders glow and pour steam, and its head turns to follow whoever struck it.</li>
     *   <li>Its fall (44 ticks): it shudders and its eyes flicker; its knees buckle and it sinks to kneel; it topples
     *   forward onto the ground, its arms falling ahead of it; its head breaks from its neck and rolls aside; the light
     *   in it goes out.</li>
     * </ul>
     */
    static void automaton(PoseStack ps, VertexConsumer solid, VertexConsumer glow, float t, float limb, float amt, float swing, int phase,
                          Matrix4f world, Vec3 pinWorld, Pose st, int light, int ov) {
        float d = st.death();
        boolean dying = d >= 0;
        float gate = dying ? 0 : Mth.clamp(amt * 2.5F, 0, 1);              // how much of a full stride it is taking
        float ph = limb * 0.8F;
        // the fall, in stages
        float shudder = dying ? Mth.clamp(1 - d / 12F, 0, 1) * (d < 12 ? 1 : 0) : 0;
        float kneel = dying ? smooth((d - 6) / 12F) : 0;
        float fallU = dying ? Mth.clamp((d - 18) / 13F, 0, 1) : 0;
        float fall = fallU * fallU * 1.42F;                               // gathering speed, as a toppled weight does
        float bounce = dying && d > 31 && d < 35 ? 0.05F * Mth.sin((d - 31) / 4F * Mth.PI) : 0;
        float headOff = dying ? smooth((d - 32) / 8F) : 0;
        float light01 = dying ? (d < 12 ? (Mth.sin(d * 3.1F + st.seed()) > -0.2F ? 1F : 0.25F) : Mth.clamp(1 - (d - 12) / 14F, 0, 1)) : 1;
        float rz = st.rouse() * light01;
        int eyeCol = mix(mix(0xFF101010, GOLD_GLOW[0], light01), 0xFFFF3020, rz);
        int coreCol = mix(mix(0xFF101010, GOLD_GLOW[1], light01), 0xFFFF4020, rz);

        float bob = -0.06F * gate * (1 - Mth.abs(Mth.cos(ph))) + (dying ? 0 : 0.006F * Mth.sin(t * 0.07F));
        ps.translate(shudder * 0.025F * Mth.sin(d * 9.3F), bob + bounce, shudder * 0.02F * Mth.cos(d * 7.7F));
        ps.mulPose(Axis.ZP.rotation(0.035F * gate * Mth.sin(ph) + shudder * 0.03F * Mth.sin(d * 11F)));
        Matrix4f rootInv = new Matrix4f(ps.last().pose()).invert();
        Vector3f pin = null;
        if (pinWorld != null && world != null) {                   // the pin's knob, in the body's own frame (for the cord)
            Vector4f v = new Matrix4f(rootInv).mul(world).transform(new Vector4f((float) pinWorld.x, (float) pinWorld.y, (float) pinWorld.z, 1));
            pin = new Vector3f(v.x, v.y, v.z);
        }
        float hip = Mth.lerp(kneel, 0.98F, 0.475F);
        float thighK = 0.5F * kneel;                                       // kneeling: thighs forward, shins flat behind
        float kneeY = hip - 0.45F * Mth.cos(thighK), kneeZ = -0.45F * Mth.sin(thighK);
        // ---- legs: the thighs go down with the body as it topples; the shins stay where they knelt
        for (int side = -1; side <= 1; side += 2) {
            float lp = ph + (side > 0 ? 0 : Mth.PI);
            float thigh = dying ? thighK : 0.5F * gate * Mth.sin(lp);       // forward is positive
            float shinAbs = dying ? -Mth.HALF_PI * kneel : thigh - 0.85F * gate * Math.max(0, Mth.cos(lp));   // bent as it swings forward
            float ky = hip - 0.45F * Mth.cos(thigh), kz = -0.45F * Mth.sin(thigh);
            ps.pushPose();
            fallAbout(ps, kneeY, kneeZ, fall);
            ps.translate(side * 0.19F, hip, 0);
            limb(ps, solid, thigh, 0.45F, 0.125F, 0.135F, MARBLE, light, ov);
            cbox(ps, solid, 0, -0.22F, -0.137F, 0.08F, 0.006F, 0.004F, MARBLE_VEIN, light, ov);           // a seam
            ps.popPose();
            ps.pushPose();
            ps.translate(side * 0.19F, ky, kz);
            ps.mulPose(Axis.XP.rotation(shinAbs));
            cbox(ps, solid, 0, 0, -0.02F, 0.135F, 0.075F, 0.145F, BRASS, light, ov);                     // the knee
            rivet(ps, solid, side * 0.06F, 0, -0.168F, light, ov);
            sbox(ps, solid, -0.11F, -0.43F, -0.12F, 0.11F, 0, 0.12F, MARBLE_WARM, light, ov);              // the shin
            cbox(ps, solid, 0, -0.38F, 0, 0.115F, 0.04F, 0.125F, IRON, light, ov);                       // the ankle band
            ps.translate(0, -0.43F, 0);
            ps.mulPose(Axis.XP.rotation(-shinAbs));                                                     // the foot stays flat
            if (dying) ps.mulPose(Axis.XP.rotation(-Mth.HALF_PI * kneel));                              // kneeling: toes down
            sbox(ps, solid, -0.135F, -0.1F, -0.21F, 0.135F, 0.03F, 0.15F, IRON, light, ov);
            sbox(ps, solid, -0.12F, -0.1F, -0.26F, 0.12F, -0.02F, -0.2F, BRASS_DARK, light, ov);         // toe cap
            ps.popPose();
        }
        // ---- everything above the knees: falls forward about them
        ps.pushPose();
        fallAbout(ps, kneeY, kneeZ, fall);
        cbox(ps, solid, 0, hip + 0.05F, 0, 0.32F, 0.075F, 0.18F, BRASS_DARK, light, ov);
        for (int i = -2; i <= 2; i++) rivet(ps, solid, i * 0.12F, hip + 0.05F, -0.187F, light, ov);
        float ty = hip + 0.46F;
        cbox(ps, solid, 0, ty - 0.25F, 0, 0.29F, 0.11F, 0.19F, MARBLE_WARM, light, ov);
        // ---- chest: marble in a brass frame, a window with a turning gear, the first figure of the Elements in gold
        cbox(ps, solid, 0, ty + 0.1F, 0, 0.38F, 0.27F, 0.23F, MARBLE, light, ov);
        for (int side = -1; side <= 1; side += 2) {
            cbox(ps, solid, side * 0.39F, ty + 0.1F, 0, 0.025F, 0.28F, 0.24F, BRASS, light, ov);
            cbox(ps, solid, 0, ty + 0.1F + side * 0.27F, -0.234F, 0.4F, 0.022F, 0.008F, BRASS, light, ov);
            for (int k = -1; k <= 1; k++) rivet(ps, solid, side * 0.39F, ty + 0.1F + k * 0.18F, -0.248F, light, ov);
        }
        float[][] veins = {{-0.3F, 0.2F, -0.16F, 0.08F}, {0.18F, -0.05F, 0.33F, -0.14F}, {-0.32F, -0.1F, -0.22F, -0.12F}};
        for (float[] vn : veins) {
            for (int i = 0; i <= 6; i++) {
                float u = i / 6F;
                cbox(ps, solid, Mth.lerp(u, vn[0], vn[2]), ty + 0.1F + Mth.lerp(u, vn[1], vn[3]), -0.2315F, 0.012F, 0.006F, 0.002F, MARBLE_VEIN, light, ov);
            }
        }
        float cz = -0.233F, gy = ty + 0.16F;
        cbox(ps, solid, 0, gy, cz + 0.004F, 0.15F, 0.15F, 0.004F, SLOT, light, ov);
        if (rz > 0.05F) cbox(ps, glow, 0, gy, cz + 0.001F, 0.14F, 0.14F, 0.002F, new int[]{mix(0xFF180404, 0xFF902010, rz), mix(0xFF0C0202, 0xFF601008, rz)}, FULL, NO);
        ps.pushPose();
        ps.translate(0, gy, cz - 0.004F);
        float spin = dying ? Math.min(d, 14) * (0.05F + 0.4F * st.rouse()) * (1 - d / 28F) : t * (0.05F + 0.4F * st.rouse()) + limb * 0.3F;
        ps.mulPose(Axis.ZP.rotation(spin));
        for (int i = 0; i < 10; i++) {
            float an = i * Mth.TWO_PI / 10;
            cbox(ps, solid, Mth.cos(an) * 0.1F, Mth.sin(an) * 0.1F, 0, 0.025F, 0.025F, 0.012F, BRASS, light, ov);
        }
        cbox(ps, solid, 0, 0, 0, 0.078F, 0.078F, 0.01F, BRASS_DARK, light, ov);
        cbox(ps, glow, 0, 0, -0.012F, 0.025F, 0.025F, 0.004F, new int[]{coreCol, coreCol}, FULL, NO);
        ps.popPose();
        float s = 0.13F, by = ty - 0.23F, h = s * Mth.sqrt(3F);
        int inlay = mix(0xFF6A5420, GOLD_GLOW[0], light01);
        for (int i = 0; i <= 10; i++) {
            float u = i / 10F;
            int[] ic = {inlay, inlay};
            cbox(ps, glow, -s + 2 * s * u, by, cz - 0.004F, 0.011F, 0.011F, 0.003F, ic, FULL, NO);
            cbox(ps, glow, -s + s * u, by + h * u, cz - 0.004F, 0.011F, 0.011F, 0.003F, ic, FULL, NO);
            cbox(ps, glow, s - s * u, by + h * u, cz - 0.004F, 0.011F, 0.011F, 0.003F, ic, FULL, NO);
        }
        // ---- neck and head (it turns to follow; when it falls, it breaks away and rolls)
        float hy = ty + 0.37F;
        if (headOff < 0.5F) cbox(ps, solid, 0, hy + 0.04F, 0, 0.09F, 0.05F, 0.09F, IRON, light, ov);
        ps.pushPose();
        ps.translate(0, hy + 0.24F, 0);
        if (headOff > 0) {
            ps.translate(0.18F * headOff, 0.05F * Mth.sin(headOff * Mth.PI) - 0.1F * headOff, -0.42F * headOff);
            ps.mulPose(Axis.ZP.rotation(-1.6F * headOff));
            ps.mulPose(Axis.XP.rotation(-0.6F * headOff));
        }
        float calm = 1 - st.rouse();
        ps.mulPose(Axis.YP.rotation(-Mth.clamp(st.headYaw(), -1.1F, 1.1F) * (dying ? 0 : 1) + 0.12F * Mth.sin(t * 0.03F) * calm * (dying ? 0 : 1)));
        ps.mulPose(Axis.XP.rotation(-Mth.clamp(st.headPitch(), -0.6F, 0.6F) * (dying ? 0 : 1)));
        cbox(ps, solid, 0, 0, 0, 0.19F, 0.17F, 0.17F, MARBLE, light, ov);
        cbox(ps, solid, 0, 0.075F, -0.005F, 0.2F, 0.03F, 0.18F, BRASS, light, ov);                      // brow band
        for (int k = -1; k <= 1; k += 2) rivet(ps, solid, k * 0.15F, 0.075F, -0.19F, light, ov);
        cbox(ps, solid, 0, -0.12F, -0.03F, 0.15F, 0.045F, 0.13F, MARBLE_WARM, light, ov);              // jaw plate
        cbox(ps, solid, 0, 0.01F, -0.172F, 0.14F, 0.028F, 0.003F, SLOT, light, ov);
        float look = 0.05F * Mth.sin(t * 0.05F) * calm;                                               // calm: it reads; roused: it stares
        float eyeW = 0.06F + 0.05F * st.rouse(), eyeH = 0.016F + 0.006F * st.rouse();
        cbox(ps, glow, look, 0.01F, -0.176F, eyeW, eyeH, 0.003F, new int[]{eyeCol, eyeCol}, FULL, NO);  // the eye slit
        if (rz > 0.3F) cbox(ps, glow, 0, 0.01F, -0.18F, eyeW + 0.03F, eyeH + 0.012F, 0.001F, new int[]{(int) (0x60 * rz) << 24 | 0xFF2010, 0x00FF2010}, FULL, NO);
        cbox(ps, solid, 0, 0.2F, 0, 0.03F, 0.03F, 0.03F, BRASS_DARK, light, ov);                       // crest: a compass point
        cbox(ps, solid, 0, 0.27F, 0, 0.012F, 0.05F, 0.012F, BRASS, light, ov);
        ps.popPose();
        // ---- shoulders, with steam vents on top (they glow when roused)
        float sy = ty + 0.32F;
        for (int side = -1; side <= 1; side += 2) {
            cbox(ps, solid, side * 0.47F, sy + 0.02F, 0, 0.125F, 0.095F, 0.155F, BRASS, light, ov);
            rivet(ps, solid, side * 0.47F, sy + 0.04F, -0.168F, light, ov);
            for (int k = -1; k <= 1; k++) {
                cbox(ps, solid, side * 0.47F, sy + 0.117F, k * 0.06F, 0.08F, 0.004F, 0.012F, SLOT, light, ov);
                if (rz > 0.05F) cbox(ps, glow, side * 0.47F, sy + 0.119F, k * 0.06F, 0.07F, 0.002F, 0.008F,
                        new int[]{mix(0xFF200404, 0xFFFF5020, rz), mix(0xFF200404, 0xFFFF5020, rz)}, FULL, NO);
            }
        }
        boolean working = !dying && (phase == EuclidsAutomaton.CIRCLE || phase == EuclidsAutomaton.HEXAGON);
        float armSwing = 0.45F * gate * Mth.sin(ph);
        float slack = dying ? kneel * 0.2F + fallU * 1.1F : 0;          // in the fall the arms go ahead of it
        // ---- right arm: the straightedge (down to the ground ahead while it draws; swung out level when it strikes)
        ps.pushPose();
        ps.translate(0.47F, sy - 0.04F, 0);
        limb(ps, solid, dying ? slack : working ? 0.55F : -armSwing * 0.8F + swing * 2.2F + st.rouse() * 0.25F, 0.33F, 0.085F, 0.09F, MARBLE_WARM, light, ov);
        ps.translate(0, -0.33F, 0);
        cbox(ps, solid, 0, 0, 0, 0.09F, 0.05F, 0.095F, BRASS, light, ov);
        limb(ps, solid, dying ? 0.1F : working ? 0.35F : 0.3F + swing * 0.8F + st.rouse() * 0.5F, 0.3F, 0.08F, 0.085F, MARBLE, light, ov);
        ps.translate(0, -0.32F, 0);
        cbox(ps, solid, 0, 0, 0, 0.09F, 0.06F, 0.09F, IRON, light, ov);
        sbox(ps, solid, -0.04F, -0.8F, -0.11F, 0.04F, 0.18F, -0.05F, MARBLE_WARM, light, ov);
        int tick = mix(0xFF6A5420, GOLD_GLOW[0], light01);
        for (int i = 0; i < 9; i++)
            sbox(ps, glow, -0.025F, -0.76F + i * 0.1F, -0.114F, 0.025F, -0.75F + i * 0.1F, -0.11F, new int[]{tick, tick}, FULL, NO);
        ps.popPose();
        // ---- left arm: carries the pin; holds the cord out toward it while it walks the circle
        boolean cord = !dying && phase == EuclidsAutomaton.CIRCLE && pin != null;
        boolean pinning = !dying && phase == EuclidsAutomaton.PINNING;
        ps.pushPose();
        ps.translate(-0.47F, sy - 0.04F, 0);
        if (cord) ps.mulPose(Axis.ZP.rotation(-0.75F));
        limb(ps, solid, dying ? slack * 0.9F : pinning ? 0.9F : cord ? 0.15F : armSwing * 0.8F + st.rouse() * 0.35F, 0.33F, 0.085F, 0.09F, MARBLE_WARM, light, ov);
        ps.translate(0, -0.33F, 0);
        cbox(ps, solid, 0, 0, 0, 0.09F, 0.05F, 0.095F, BRASS, light, ov);
        limb(ps, solid, dying ? 0.15F : pinning ? 0.4F : cord ? 0.1F : 0.25F + st.rouse() * 0.4F, 0.3F, 0.08F, 0.085F, MARBLE, light, ov);
        ps.translate(0, -0.32F, 0);
        cbox(ps, solid, 0, 0, 0, 0.09F, 0.06F, 0.09F, IRON, light, ov);
        Vector3f hand = at(ps, rootInv);
        if (phase == EuclidsAutomaton.IDLE) {                    // the pin, carried point down
            cbox(ps, solid, 0, -0.2F, -0.06F, 0.022F, 0.28F, 0.022F, BRASS, light, ov);
            cbox(ps, solid, 0, 0.1F, -0.06F, 0.045F, 0.035F, 0.045F, BRASS_DARK, light, ov);
        }
        ps.popPose();
        ps.popPose();
        // the cord: taut and gold, from the hand to the pin's knob
        if (cord) TitanMobRenderers.seg(ps, glow, hand.x, hand.y, hand.z, pin.x, pin.y, pin.z, 0.012F, GOLD_GLOW[0], FULL, NO);
    }

    /** Tips everything drawn after it forward about the knee line (y, z), by `angle` radians. */
    static void fallAbout(PoseStack ps, float y, float z, float angle) {
        if (angle == 0) return;
        ps.translate(0, y, z);
        ps.mulPose(Axis.XP.rotation(-angle));
        ps.translate(0, -y, -z);
    }

    private MathMobRenderers() {}
}
