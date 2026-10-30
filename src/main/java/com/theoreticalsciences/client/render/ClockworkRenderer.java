package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.entity.ParadoxEcho;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * The Paradox Echo: a walking clockwork automaton - a clock movement built out in three dimensions.
 * <ul>
 *   <li>legs are pendulum rods with gear knees, swinging with its stride;</li>
 *   <li>the torso is an open brass-and-steel cage holding a mainspring barrel and a gear train whose wheels turn
 *       on all three axes, around a central arbor;</li>
 *   <li>the arms are crank linkages with escapement-fork claws, held forward;</li>
 *   <li>the head is an armillary sphere of turning rings around a clock dial whose hands run backwards, crowned by
 *       an oscillating balance wheel.</li>
 * </ul>
 * When it dies every part flies off on its own, spinning, and fades away.
 */
public class ClockworkRenderer extends EntityRenderer<ParadoxEcho> {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int BRASS = 0xC8A040, BRASS_DARK = 0x8A6A28, STEEL = 0x9AA2B4, DARK = 0x3A3A44, DIAL = 0xE8DCC0, HAND = 0x202020;

    private PoseStack ps;
    private VertexConsumer vc;
    private int light, overlay;
    private float death, alpha;
    private Random rnd;

    public ClockworkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.45F;
    }

    private int col(int rgb) {
        return ProcMesh.withAlpha(rgb, alpha);
    }

    /** Starts a part anchored at (x, y, z): when the echo dies, each part flies off in its own direction and spins. */
    private void begin(int index, float x, float y, float z) {
        ps.pushPose();
        ps.translate(x, y, z);
        if (death > 0) {
            rnd.setSeed(index * 7919L + 13);
            float dx = (rnd.nextFloat() - 0.5F) * 2.6F, dy = 0.4F + rnd.nextFloat() * 1.4F, dz = (rnd.nextFloat() - 0.5F) * 2.6F;
            ps.translate(dx * death, dy * death - death * death * 1.8F, dz * death);
            ps.mulPose(Axis.XP.rotationDegrees(death * (rnd.nextFloat() - 0.5F) * 900F));
            ps.mulPose(Axis.ZP.rotationDegrees(death * (rnd.nextFloat() - 0.5F) * 900F));
        }
    }

    private void end() {
        ps.popPose();
    }

    private void box(float x0, float y0, float z0, float x1, float y1, float z1, int rgb) {
        ProcMesh.box(ps, vc, x0, y0, z0, x1, y1, z1, col(rgb), light, overlay);
    }

    /** Gear with its axle along the given axis ('x', 'y' or 'z'). */
    private void gear(char axis, float r, int teeth, float thick, float angle, int rgb) {
        ps.pushPose();
        if (axis == 'x') ps.mulPose(Axis.YP.rotationDegrees(90));
        else if (axis == 'y') ps.mulPose(Axis.XP.rotationDegrees(90));
        ProcMesh.gear(ps, vc, r, teeth, thick, angle, col(rgb), light, overlay);
        ps.popPose();
    }

    private void ring(char axis, float r, float band, int segs, int rgb) {
        ps.pushPose();
        if (axis == 'x') ps.mulPose(Axis.YP.rotationDegrees(90));
        else if (axis == 'y') ps.mulPose(Axis.XP.rotationDegrees(90));
        ProcMesh.ring(ps, vc, r, band, band, segs, col(rgb), light, overlay);
        ps.popPose();
    }

    @Override
    public void render(ParadoxEcho e, float yaw, float partial, PoseStack poseStack, MultiBufferSource buf, int packedLight) {
        ps = poseStack;
        light = packedLight;
        rnd = new Random();
        float t = e.tickCount + partial;
        death = e.deathTime > 0 ? Math.min(1F, (e.deathTime + partial) / 20F) : 0F;
        alpha = 1F - death;
        overlay = OverlayTexture.pack(0, e.hurtTime > 0 || e.deathTime > 0);
        vc = SafeBuffer.of(buf, death > 0 ? RenderType.entityTranslucent(WHITE) : RenderType.entitySolid(WHITE));
        float limb = e.walkAnimation.position(partial), amt = Math.min(1F, e.walkAnimation.speed(partial));
        float swing = Mth.cos(limb * 0.6662F) * 1.1F * amt;            // radians, like a zombie's stride
        float bob = Math.abs(Mth.sin(limb * 0.6662F)) * 0.04F * amt;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot)));

        // ---------------------------------------------------------------- legs: pendulum rods with gear knees
        for (int s = -1; s <= 1; s += 2) {
            float legSwing = swing * s;
            begin(s < 0 ? 0 : 1, s * 0.15F, 0.95F + bob, 0);
            ps.mulPose(Axis.XP.rotation(legSwing));
            box(-0.04F, -0.47F, -0.04F, 0.04F, 0, 0.04F, STEEL);           // thigh
            ps.translate(0, -0.47F, 0);
            gear('x', 0.07F, 8, 0.05F, t * 6 * s, BRASS);                  // knee gear
            ps.mulPose(Axis.XP.rotation(Math.max(0, -legSwing) * 0.6F));
            box(-0.035F, -0.46F, -0.035F, 0.035F, 0, 0.035F, BRASS);       // shin
            box(-0.08F, -0.48F, -0.13F, 0.08F, -0.44F, 0.07F, DARK);        // foot plate
            end();
        }
        // pelvis
        begin(2, 0, 0.95F + bob, 0);
        box(-0.24F, -0.03F, -0.1F, 0.24F, 0.05F, 0.1F, DARK);
        end();

        // ---------------------------------------------------------------- torso: open cage around a gear train
        float ty = 1.0F + bob;
        begin(3, 0, ty, 0);
        for (int cx = -1; cx <= 1; cx += 2)
            for (int cz = -1; cz <= 1; cz += 2)
                box(cx * 0.2F - 0.02F, 0, cz * 0.13F - 0.02F, cx * 0.2F + 0.02F, 0.6F, cz * 0.13F + 0.02F, STEEL);
        end();
        begin(4, 0, ty + 0.02F, 0);
        ring('y', 0.25F, 0.035F, 18, BRASS);
        end();
        begin(5, 0, ty + 0.58F, 0);
        ring('y', 0.25F, 0.035F, 18, BRASS);
        end();
        begin(6, 0, ty + 0.3F, 0);                                         // slowly turning armature hoop
        ps.mulPose(Axis.YP.rotationDegrees(t * 2F));
        ring('x', 0.26F, 0.025F, 18, BRASS_DARK);
        end();
        begin(7, 0, ty, 0);                                                // central arbor
        box(-0.015F, 0, -0.015F, 0.015F, 0.62F, 0.015F, STEEL);
        end();
        begin(8, 0, ty + 0.16F, 0.02F);                                    // mainspring barrel (axle along X)
        ps.mulPose(Axis.XP.rotationDegrees(t * 1.5F));
        ps.mulPose(Axis.YP.rotationDegrees(90));
        ProcMesh.drum(ps, vc, 0.11F, 0.16F, col(BRASS_DARK), light, overlay);
        ProcMesh.gear(ps, vc, 0.12F, 14, 0.04F, 0, col(BRASS), light, overlay);
        ps.popPose();
        begin(9, 0, ty + 0.36F, -0.09F);                                   // great wheel facing forward (axle Z)
        gear('z', 0.16F, 16, 0.05F, t * 2F, BRASS);
        end();
        begin(10, 0.1F, ty + 0.42F, 0.05F);                                // side wheel (axle X) meshing behind it
        gear('x', 0.11F, 12, 0.05F, -t * 2.9F, STEEL);
        end();
        begin(11, 0, ty + 0.5F, 0);                                        // crown wheel (axle Y)
        gear('y', 0.12F, 14, 0.04F, t * 3.4F, BRASS);
        end();
        begin(12, -0.11F, ty + 0.22F, 0.06F);                              // escape wheel
        gear('z', 0.07F, 10, 0.04F, -t * 7F, STEEL);
        end();

        // ---------------------------------------------------------------- arms: crank linkages with escapement claws
        for (int s = -1; s <= 1; s += 2) {
            begin(s < 0 ? 13 : 14, s * 0.28F, ty + 0.55F, 0);
            gear('x', 0.07F, 8, 0.05F, t * 5 * s, BRASS);                  // shoulder gear
            ps.mulPose(Axis.XP.rotationDegrees(-80F - swing * s * 20F + Mth.sin(t * 0.12F + s) * 4F));
            box(-0.03F, -0.34F, -0.03F, 0.03F, 0, 0.03F, BRASS);           // upper arm
            ps.translate(0, -0.34F, 0);
            box(-0.045F, -0.045F, -0.045F, 0.045F, 0.045F, 0.045F, STEEL); // elbow joint
            ps.mulPose(Axis.XP.rotationDegrees(-10F + Mth.sin(t * 0.3F) * 6F));
            box(-0.025F, -0.3F, -0.025F, 0.025F, 0, 0.025F, STEEL);        // forearm
            ps.translate(0, -0.3F, 0);
            box(-0.07F, -0.03F, -0.025F, 0.07F, 0.01F, 0.025F, STEEL);     // escapement fork claw
            box(-0.07F, -0.11F, -0.02F, -0.04F, -0.03F, 0.02F, STEEL);
            box(0.04F, -0.11F, -0.02F, 0.07F, -0.03F, 0.02F, STEEL);
            end();
        }

        // ---------------------------------------------------------------- head: armillary sphere around a backwards clock
        float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot) - Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float headPitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float hy = ty + 0.78F;
        begin(15, 0, ty + 0.6F, 0);                                        // neck
        box(-0.03F, 0, -0.03F, 0.03F, 0.08F, 0.03F, STEEL);
        end();
        begin(16, 0, hy, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-headYaw));
        ps.mulPose(Axis.XP.rotationDegrees(-headPitch));
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(t * 3F));
        ring('z', 0.18F, 0.022F, 20, BRASS);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(t * 2.2F));
        ring('x', 0.17F, 0.022F, 20, BRASS_DARK);
        ps.popPose();
        ring('y', 0.19F, 0.02F, 20, STEEL);                                 // fixed equator
        // dial facing forward, hands running backwards
        ps.pushPose();
        ps.translate(0, 0, -0.06F);
        ProcMesh.drum(ps, vc, 0.115F, 0.02F, col(DIAL), light, overlay);
        for (int i = 0; i < 12; i++) {
            ps.pushPose();
            ps.mulPose(Axis.ZP.rotationDegrees(30F * i));
            box(-0.006F, 0.085F, -0.016F, 0.006F, 0.105F, -0.011F, HAND);
            ps.popPose();
        }
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(t * 6F));
        box(-0.006F, 0, -0.02F, 0.006F, 0.095F, -0.012F, HAND);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(t * 0.5F));
        box(-0.009F, 0, -0.02F, 0.009F, 0.065F, -0.012F, HAND);
        ps.popPose();
        ps.popPose();
        // balance wheel on top, oscillating, with its hairspring stud
        ps.pushPose();
        ps.translate(0, 0.22F, 0);
        box(-0.012F, -0.05F, -0.012F, 0.012F, 0, 0.012F, STEEL);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.sin(t * 0.8F) * 160F));
        ring('y', 0.1F, 0.018F, 14, BRASS);
        box(-0.1F, -0.006F, -0.006F, 0.1F, 0.006F, 0.006F, BRASS);
        ps.popPose();
        end();
        // temporal jewels: the glowing heart in the head and at the great wheel's arbor
        if (alpha > 0.05F) {
            VertexConsumer prev = vc;
            vc = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            begin(16, 0, hy, 0);
            float j = 0.035F * alpha;
            ProcMesh.box(ps, vc, -j, -j, -j, j, j, j, 0xFFFFC23D, 0xF000F0, OverlayTexture.NO_OVERLAY);
            end();
            begin(9, 0, ty + 0.36F, -0.12F);
            ProcMesh.box(ps, vc, -j, -j, -j, j, j, j, 0xFFFFD070, 0xF000F0, OverlayTexture.NO_OVERLAY);
            end();
            vc = prev;
        }
        ps.popPose();
        super.render(e, yaw, partial, poseStack, buf, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ParadoxEcho entity) {
        return WHITE;
    }
}
