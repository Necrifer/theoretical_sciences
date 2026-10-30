package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.theoreticalsciences.transcend.ForgedCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Round 34c: the forged Omnipotence Core, dormant and awake.
 * <ul>
 *   <li><b>Dormant</b>: seven dim motes - one per branch it has to be attuned in - drift slowly around it on a single
 *       faint ring, and a slow heartbeat ring swells out every few seconds.</li>
 *   <li><b>Waking</b>: a flash ring bursts out, the motes flare and speed up.</li>
 *   <li><b>Awake</b>: three golden gimbal rings turn on their own axes, a 4-cube's shadow turns around it all, light
 *       beams rise and fall from it, and the motes race round in their branch colours.</li>
 * </ul>
 * The cube itself is the block model (blockstate {@code active}: a dim, slow texture or the bright one).
 * Drawn with the same light quads as the Convergence ritual (no textures, so nothing for Sodium to skip).
 */
public class ForgedCoreRenderer implements BlockEntityRenderer<ForgedCoreBlockEntity> {
    private static final int[] BRANCH = {0x66CCFF, 0x66FF99, 0xFF9A66, 0xFFE066, 0xFFD8A0, 0x9A7CFF, 0xFF4D6A};
    private static final int GOLD = 0xFFD860, PALE = 0xB8A8D8;

    public ForgedCoreRenderer(BlockEntityRendererProvider.Context c) {}

    @Override
    public void render(ForgedCoreBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        float t = be.getLevel().getGameTime() + pt + (be.getBlockPos().hashCode() & 255);
        // ease the waking (about two seconds) and sleeping (one)
        float target = be.active() ? 1 : 0;
        if (be.wakeShown < 0) be.wakeShown = target;
        float dt = Math.max(0, Math.min(2, t - be.lastFrame));
        be.lastFrame = t;
        be.wakeShown += Math.signum(target - be.wakeShown) * Math.min(Math.abs(target - be.wakeShown), dt * (target > be.wakeShown ? 0.025F : 0.05F));
        float a = be.wakeShown, s = a * a * (3 - 2 * a);

        VertexConsumer vc = SafeBuffer.of(buf, RenderType.lightning());
        Matrix4f m = ps.last().pose();
        double cx = 0.5, cy = 0.5, cz = 0.5;

        // ---- the seven motes, one per branch: dim and slow asleep, bright and racing awake
        float speed = 0.006F + 0.05F * s;
        float orbit = 0.95F + 0.25F * s;
        for (int k = 0; k < 7; k++) {
            double an = t * speed + k * Math.PI * 2 / 7;
            double y = Math.sin(t * (0.01 + 0.03 * s) + k * 1.3) * (0.08 + 0.35 * s);
            double x = cx + Math.cos(an) * orbit, z = cz + Math.sin(an) * orbit;
            float pulse = 0.5F + 0.5F * (float) Math.sin(t * 0.08 + k);
            ConvergenceRenderers.star(m, vc, x, cy + y, z, 0.035F + 0.05F * s, BRANCH[k], 0.25F + 0.15F * pulse + 0.5F * s, t + k * 9);
            if (s > 0.05F) {      // a short trail behind each racing mote
                double an2 = an - 0.35, x2 = cx + Math.cos(an2) * orbit, z2 = cz + Math.sin(an2) * orbit;
                double y2 = Math.sin((t - 7) * (0.01 + 0.03 * s) + k * 1.3) * (0.08 + 0.35 * s);
                ConvergenceRenderers.beam(m, vc, x2, cy + y2, z2, x, cy + y, z, 0.012F, BRANCH[k], 0.5F * s);
            }
        }

        // ---- asleep: one faint ring and a slow heartbeat
        float sleep = 1 - s;
        if (sleep > 0.01F) {
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, 0.95F, t * 0.004, 0.25, 0.006F, PALE, 0.22F * sleep);
            float beat = (t % 100) / 100F;          // every five seconds
            if (beat < 0.5F) {
                float u = beat / 0.5F;
                ConvergenceRenderers.ring(m, vc, cx, cy, cz, 0.9F + 0.6F * u, t * 0.004, 0, 0.008F, PALE, 0.3F * (1 - u) * sleep);
            }
        }

        // ---- waking: a flash ring bursting out while it comes up
        if (be.active() && a < 1) {
            float u = a;
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, 0.9F + 2.4F * u, 0, 0, 0.03F * (1 - u) + 0.006F, 0xFFFFFF, 0.9F * (1 - u));
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, 0.9F + 1.6F * u, 1.2, Math.PI / 2, 0.02F * (1 - u) + 0.004F, GOLD, 0.7F * (1 - u));
        }

        // ---- awake: gimbal rings, the 4-cube, beams
        if (s > 0.01F) {
            float r = 1.05F + 0.1F * (float) Math.sin(t * 0.05);
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, r, t * 0.031, 0.0, 0.014F, GOLD, 0.85F * s);
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, r * 1.08F, -t * 0.023, Math.PI / 2 + Math.sin(t * 0.01) * 0.4, 0.012F, 0xFFF0C0, 0.7F * s);
            ConvergenceRenderers.ring(m, vc, cx, cy, cz, r * 1.16F, t * 0.017 + 1.3, Math.PI / 4 + Math.cos(t * 0.013) * 0.5, 0.01F, 0xFFE0FF, 0.55F * s);
            ConvergenceRenderers.tesseract(m, vc, cx, cy, cz, 1.9F * s, t * 0.012, t * 0.008, 0.008F, ConvergenceRenderers.lerpRgb(GOLD, 0xFFFFFF, 0.4F), 0.35F * s);
            float glow = 0.6F + 0.4F * (float) Math.sin(t * 0.07);
            for (int d = -1; d <= 1; d += 2) {
                double y0 = cy + d * 0.55, y1 = cy + d * (1.6 + 0.3 * glow);
                ConvergenceRenderers.beam(m, vc, cx, y0, cz, cx, y1, cz, 0.05F, GOLD, 0.35F * s);
                ConvergenceRenderers.beam(m, vc, cx, y0, cz, cx, y1, cz, 0.13F, GOLD, 0.1F * s);
                ConvergenceRenderers.star(m, vc, cx, y1, cz, 0.12F + 0.06F * glow, 0xFFFFFF, 0.6F * s, t);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(ForgedCoreBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2.5);
    }

    @Override
    public boolean shouldRenderOffScreen(ForgedCoreBlockEntity be) {
        return true;
    }
}
