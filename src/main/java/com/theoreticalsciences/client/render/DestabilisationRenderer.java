package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.entity.DestabilisedHusk;
import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * How a reality-destabilised creature looks:
 * <ul>
 *   <li>it flickers out of existence for a frame or two, like a 4D object passing through our 3D slice;</li>
 *   <li>its shape stretches and squashes along different axes and jitters sideways;</li>
 *   <li>small copies of itself split off and orbit it, and those copies have copies (a fractal);</li>
 *   <li>when it dies it breaks into shrinking fragments that fly apart and disintegrate.</li>
 * </ul>
 * Nothing here touches the game state - it is purely how the creature is drawn.
 */
public final class DestabilisationRenderer {
    private static final int MAX_DEPTH = 2;
    private static int depth;
    private static final boolean[] PUSHED = new boolean[MAX_DEPTH + 2];

    public static void register() {
        NeoForge.EVENT_BUS.addListener(DestabilisationRenderer::pre);
        NeoForge.EVENT_BUS.addListener(DestabilisationRenderer::post);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientHooks.DESTABILISED.clear());
    }

    public static boolean isDestabilised(LivingEntity e) {
        if (e instanceof DestabilisedHusk) return true;
        if (ClientHooks.DESTABILISED.contains(e.getId())) return true;
        return e == Minecraft.getInstance().player && e.hasEffect(QHEffects.REALITY_DESTABILISATION);
    }

    private static float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535F;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void pre(RenderLivingEvent.Pre event) {
        LivingEntity e = event.getEntity();
        if (depth >= PUSHED.length - 1) return;
        PUSHED[depth] = false;
        if (!isDestabilised(e)) return;
        PoseStack ps = event.getPoseStack();
        float partial = event.getPartialTick();
        float t = e.tickCount + partial;
        int id = e.getId();

        if (depth == 0) {
            // death: shatter into fragments of itself that drift apart, shrink and disintegrate
            if (e.deathTime > 0) {
                event.setCanceled(true);
                float d = Math.min(1F, (e.deathTime + partial) / 20F);
                LivingEntityRenderer r = event.getRenderer();
                int shards = 7;
                depth = MAX_DEPTH;                      // fragments do not split further
                for (int i = 0; i < shards; i++) {
                    if (hash(id + i, e.deathTime) < d * 0.6F) continue;   // fragments wink out one by one
                    float a = i * 2.39996F + id;
                    float out = d * (0.6F + hash(id, i) * 0.9F);
                    ps.pushPose();
                    ps.translate(Mth.cos(a) * out, e.getBbHeight() * (0.15F + hash(i, id) * 0.6F) + d * 0.5F * hash(i, 7), Mth.sin(a) * out);
                    ps.mulPose(Axis.YP.rotationDegrees(d * 300F * (hash(i, 3) - 0.5F)));
                    ps.mulPose(Axis.XP.rotationDegrees(d * 200F * (hash(i, 5) - 0.5F)));
                    float s = (0.42F + hash(i, 9) * 0.2F) * (1F - d * 0.85F);
                    ps.scale(s, s, s);
                    ps.translate(0, -e.getBbHeight() * 0.5F, 0);
                    r.render(e, e.getYRot(), partial, ps, event.getMultiBufferSource(), event.getPackedLight());
                    ps.popPose();
                }
                depth = 0;
                if (!Minecraft.getInstance().isPaused() && e.level().random.nextInt(2) == 0)
                    e.level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), e.getRandomX(0.8), e.getRandomY(), e.getRandomZ(0.8),
                            (e.level().random.nextDouble() - 0.5) * 0.2, 0.05, (e.level().random.nextDouble() - 0.5) * 0.2);
                return;
            }
            // flicker out of our 3D slice for a moment (about 1 frame in 8 slots)
            int slot = (int) (t / 2.5F);
            if (hash(id, slot) < 0.12F) {
                event.setCanceled(true);
                return;
            }
        }

        ps.pushPose();
        PUSHED[depth] = true;
        // jitter: occasionally the whole creature is displaced sideways for a moment
        int jslot = (int) (t / 1.5F);
        if (hash(id * 3 + depth, jslot) < 0.18F) {
            float jx = (hash(id, jslot * 7) - 0.5F) * 0.5F, jz = (hash(jslot, id * 5) - 0.5F) * 0.5F;
            ps.translate(jx, 0, jz);
        }
        // 4D projection breathing: one axis grows while the other shrinks
        float w = Mth.sin(t * 0.21F + id) * 0.09F;
        ps.scale(1F + w, 1F - w * 0.5F, 1F - w);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void post(RenderLivingEvent.Post event) {
        if (depth >= PUSHED.length - 1 || !PUSHED[depth]) return;
        PUSHED[depth] = false;
        PoseStack ps = event.getPoseStack();
        ps.popPose();
        if (depth >= MAX_DEPTH) return;
        LivingEntity e = event.getEntity();
        float partial = event.getPartialTick();
        float t = e.tickCount + partial;
        LivingEntityRenderer r = event.getRenderer();
        int copies = depth == 0 ? 3 : 2;
        int myDepth = depth;
        for (int i = 0; i < copies; i++) {
            // each copy fades in and out on its own rhythm (only drawn while "phased in")
            float phase = Mth.sin(t * (0.09F + i * 0.03F) + i * 2.1F + e.getId());
            if (phase < 0.1F) continue;
            float grow = Math.min(1F, (phase - 0.1F) * 3F);
            float a = t * (0.05F + 0.02F * i) * (i % 2 == 0 ? 1 : -1) + i * 6.283F / copies;
            float rad = e.getBbWidth() * (myDepth == 0 ? 1.1F : 0.9F) + 0.3F;
            ps.pushPose();
            ps.translate(Mth.cos(a) * rad, e.getBbHeight() * (0.5F + 0.35F * Mth.sin(a * 1.7F)), Mth.sin(a) * rad);
            ps.mulPose(Axis.YP.rotationDegrees(-a * 57.3F + 90F));
            float s = 0.38F * grow;
            ps.scale(s, s, s);
            ps.translate(0, -e.getBbHeight() * 0.5F, 0);
            depth = myDepth + 1;
            try {
                r.render(e, e.getYRot(), partial, ps, event.getMultiBufferSource(), event.getPackedLight());
            } finally {
                depth = myDepth;
            }
            ps.popPose();
        }
    }

    private DestabilisationRenderer() {}
}
