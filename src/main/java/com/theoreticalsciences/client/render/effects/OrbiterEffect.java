package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/**
 * 1.2.0: small bodies circling a star: each a block, on its own orbit (its own tilt, distance and speed), spinning as it
 * goes, at its own size. Orbits are fixed by seed (the machine's position), so each machine has its own system; the
 * bodies move with the machine's power, so they slow to a stop and hold where they are when it stops.
 */
public final class OrbiterEffect {
    /** clock: seconds of running (integrated with power by the caller, so it never jumps). */
    public static void draw(PoseStack ps, Matrix4f local, MultiBufferSource buf, Vector3f centre, int count, float inner, float outer,
                            List<BlockState> blocks, long seed, float clock, float power) {
        if (count <= 0 || blocks.isEmpty() || power <= 0.001F) return;
        var disp = Minecraft.getInstance().getBlockRenderer();
        Random rnd = new Random(seed);
        float grow = Math.min(1F, power);
        for (int k = 0; k < count; k++) {
            float dist = inner + (outer - inner) * rnd.nextFloat();
            float tiltA = rnd.nextFloat() * Mth.TWO_PI, tilt = (rnd.nextFloat() - 0.5F) * 1.6F;
            float speed = (0.15F + 0.6F * rnd.nextFloat()) * (float) Math.pow(inner / dist, 1.5) * (rnd.nextBoolean() ? 1 : -1);
            float phase = rnd.nextFloat() * Mth.TWO_PI, spin = (rnd.nextFloat() - 0.5F) * 120F;
            float size = (0.5F + rnd.nextFloat() * 1.1F) * grow;
            BlockState st = blocks.get(rnd.nextInt(blocks.size()));
            float a = phase + clock * speed;
            ps.pushPose();
            ps.last().pose().set(local);
            ps.last().normal().set(new Matrix3f(local));
            ps.translate(centre.x, centre.y, centre.z);
            ps.mulPose(new org.joml.Quaternionf().rotateY(tiltA).rotateX(tilt));
            ps.translate(Mth.cos(a) * dist, 0, Mth.sin(a) * dist);
            ps.mulPose(new org.joml.Quaternionf().rotateXYZ(clock * spin * 0.017F, clock * spin * 0.013F, k));
            ps.scale(size, size, size);
            ps.translate(-0.5F, -0.5F, -0.5F);
            disp.renderSingleBlock(st, ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            ps.popPose();
        }
    }

    private OrbiterEffect() {}
}
