package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.endgame.Raiment;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * Godform: whoever wears the whole Raiment of Transcendence is drawn as a being of light. The normal player render is
 * cancelled and replaced by a private {@link PlayerRenderer} subclass (so every pose and animation is vanilla's own) whose
 * skin is pure white, full-bright and emissive, wrapped in additive halo shells and a ring of light behind the head.
 * While flying (creative-style or elytra) feathered wings of light unfold behind the torso and flap; they fold away on
 * landing. A few end-rod motes drift around the figure. Client only.
 */
public final class GodformRenderer {
    private static final ResourceLocation WHITE = TheoreticalSciences.id("textures/entity/godform.png");
    private static final ResourceLocation WING = TheoreticalSciences.id("textures/entity/godform_wing.png");

    /** One godform renderer per vanilla player renderer (wide / slim); rebuilt automatically when vanilla rebuilds its own. */
    private static final Map<PlayerRenderer, GodPlayerRenderer> RENDERERS = new WeakHashMap<>();
    /** Per-player wing state: {openness 0..1, last render time in ticks}. */
    private static final Map<Player, float[]> WINGS = new WeakHashMap<>();
    private static boolean broken;

    private GodformRenderer() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GodformRenderer::onRenderPre);
        NeoForge.EVENT_BUS.addListener(GodformRenderer::onRenderArm);
        NeoForge.EVENT_BUS.addListener(GodformRenderer::onClientTick);
    }

    public static boolean godform(Player p) {
        return !p.isSpectator() && Raiment.wearsFullSet(p);
    }

    @Nullable
    private static GodPlayerRenderer godRendererFor(PlayerRenderer vanilla, AbstractClientPlayer p) {
        if (broken) return null;
        GodPlayerRenderer god = RENDERERS.get(vanilla);
        if (god == null) {
            try {
                Minecraft mc = Minecraft.getInstance();
                EntityRendererProvider.Context ctx = new EntityRendererProvider.Context(mc.getEntityRenderDispatcher(), mc.getItemRenderer(),
                        mc.getBlockRenderer(), mc.gameRenderer.itemInHandRenderer, mc.getResourceManager(), mc.getEntityModels(), mc.font);
                god = new GodPlayerRenderer(ctx, p.getSkin().model() == PlayerSkin.Model.SLIM);
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Godform renderer could not be built; falling back to normal player rendering", ex);
                broken = true;
                return null;
            }
            RENDERERS.put(vanilla, god);
        }
        return god;
    }

    private static void onRenderPre(RenderPlayerEvent.Pre e) {
        if (e.getRenderer() instanceof GodPlayerRenderer) return; // our own render re-fires the event
        if (!(e.getEntity() instanceof AbstractClientPlayer p) || !godform(p)) return;
        GodPlayerRenderer god = godRendererFor(e.getRenderer(), p);
        if (god == null) return;
        e.setCanceled(true);
        float pt = e.getPartialTick();
        god.render(p, Mth.lerp(pt, p.yRotO, p.getYRot()), pt, e.getPoseStack(), e.getMultiBufferSource(), LightTexture.FULL_BRIGHT);
    }

    /** First-person arm: white and glowing as well. */
    private static void onRenderArm(RenderArmEvent e) {
        AbstractClientPlayer p = e.getPlayer();
        if (!godform(p)) return;
        EntityRenderer<? super AbstractClientPlayer> r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p);
        if (!(r instanceof PlayerRenderer vanilla)) return;
        GodPlayerRenderer god = godRendererFor(vanilla, p);
        if (god == null) return;
        e.setCanceled(true);
        god.renderGodArm(e.getPoseStack(), e.getMultiBufferSource(), p, e.getArm());
    }

    private static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || mc.player == null) return;
        RandomSource r = mc.level.random;
        for (Player p : mc.level.players()) {
            if (p.isInvisible() || !godform(p) || p.distanceToSqr(mc.player) > 48 * 48) continue;
            if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (r.nextFloat() > 0.3F) continue;
            double x = p.getX() + (r.nextDouble() - 0.5) * 1.2, y = p.getY() + r.nextDouble() * p.getBbHeight(), z = p.getZ() + (r.nextDouble() - 0.5) * 1.2;
            mc.level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), x, y, z, (r.nextDouble() - 0.5) * 0.02, 0.015 + r.nextDouble() * 0.02, (r.nextDouble() - 0.5) * 0.02);
        }
    }

    private static int grey(int c, float warm) {
        c = Mth.clamp(c, 0, 255);
        return 0xFF000000 | c << 16 | (int) (c * (1 - warm * 0.12F)) << 8 | (int) (c * (1 - warm * 0.4F));
    }

    // ---------------------------------------------------------------- the renderer

    private static final class GodPlayerRenderer extends PlayerRenderer {
        GodPlayerRenderer(EntityRendererProvider.Context ctx, boolean slim) {
            super(ctx, slim);
            layers.clear(); // no armour, cape, elytra, arrows...: the figure is light itself
            addLayer(new PlayerItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
            addLayer(new HaloLayer(this));
            addLayer(new WingLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractClientPlayer p) {
            return WHITE;
        }

        @Nullable
        @Override
        protected RenderType getRenderType(AbstractClientPlayer p, boolean bodyVisible, boolean translucent, boolean glowing) {
            if (bodyVisible || translucent) return RenderType.entityTranslucentEmissive(WHITE);
            return glowing ? RenderType.outline(WHITE) : null;
        }

        void renderGodArm(PoseStack ps, MultiBufferSource buf, AbstractClientPlayer p, HumanoidArm side) {
            PlayerModel<AbstractClientPlayer> m = getModel();
            m.setAllVisible(true);
            m.attackTime = 0.0F;
            m.crouching = false;
            m.swimAmount = 0.0F;
            m.setupAnim(p, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            ModelPart arm = side == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
            arm.xRot = 0.0F;
            arm.render(ps, SafeBuffer.of(buf, RenderType.entityTranslucentEmissive(WHITE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            float pulse = 0.85F + 0.15F * Mth.sin((p.tickCount) * 0.1F);
            shell(arm, 1.12F, ps, SafeBuffer.of(buf, RenderType.eyes(WHITE)), grey((int) (90 * pulse), 0.3F));
        }
    }

    /** Renders a part inflated about its own pivot. */
    private static void shell(ModelPart part, float s, PoseStack ps, VertexConsumer vc, int color) {
        float xs = part.xScale, ys = part.yScale, zs = part.zScale;
        part.xScale = xs * s;
        part.yScale = ys * s;
        part.zScale = zs * s;
        part.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        part.xScale = xs;
        part.yScale = ys;
        part.zScale = zs;
    }

    /** Additive glow shells around every limb plus a ring of light behind the head. */
    private static final class HaloLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        HaloLayer(GodPlayerRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbAmount, float pt,
                           float age, float headYaw, float headPitch) {
            if (p.isInvisible()) return;
            PlayerModel<AbstractClientPlayer> m = getParentModel();
            VertexConsumer vc = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float pulse = 0.85F + 0.15F * Mth.sin(age * 0.1F);
            ModelPart[] parts = {m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
            float[][] shells = {{1.10F, 110}, {1.22F, 55}, {1.38F, 24}};
            for (float[] sh : shells) {
                int c = grey((int) (sh[1] * pulse), 0.35F);
                for (ModelPart part : parts) shell(part, sh[0], ps, vc, c);
            }
            // ring of light behind the head
            if (m.head.visible) {
                ps.pushPose();
                m.head.translateAndRotate(ps);
                ps.translate(0.0F, -0.27F, 0.33F);
                ps.mulPose(Axis.ZP.rotationDegrees(age * 1.5F));
                Matrix4f mat = ps.last().pose();
                int seg = 32;
                float r0 = 0.30F, r1 = 0.36F;
                int gold = grey((int) (200 * pulse), 1.0F);
                for (int i = 0; i < seg; i++) {
                    float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
                    float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
                    quad(vc, mat, c0 * r0, s0 * r0, c0 * r1, s0 * r1, c1 * r1, s1 * r1, c1 * r0, s1 * r0, 0, gold, 0.5F, 0.5F, 0.5F, 0.5F);
                }
                ps.popPose();
            }
        }
    }

    /** Feathered wings of light, open while flying, folded away otherwise. */
    private static final class WingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        WingLayer(GodPlayerRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbAmount, float pt,
                           float age, float headYaw, float headPitch) {
            boolean elytra = p.isFallFlying();
            // other players' "flying" ability is not sent to this client: judge by how they move
            boolean hover = p.isLocalPlayer() ? p.getAbilities().flying
                    : !p.onGround() && !p.isInWater() && Math.abs(p.getDeltaMovement().y) < 0.3 && p.fallDistance < 1.5F;
            boolean flying = (hover || elytra) && !p.isPassenger();
            float[] st = WINGS.computeIfAbsent(p, k -> new float[]{0.0F, age});
            float dt = Mth.clamp(age - st[1], 0.0F, 5.0F);
            st[1] = age;
            float target = flying ? 1.0F : 0.0F;
            st[0] = st[0] < target ? Math.min(target, st[0] + dt * 0.09F) : Math.max(target, st[0] - dt * 0.07F);
            float open = st[0];
            if (open < 0.03F || p.isInvisible()) return;
            float ease = open * open * (3 - 2 * open);

            PlayerModel<AbstractClientPlayer> m = getParentModel();
            float phase = age * (elytra ? 0.12F : 0.2F) + (p.getId() % 17);
            float flap = Mth.sin(phase) * (elytra ? 5.0F : 20.0F) * ease;
            // two passes, one buffer at a time (asking for the second closes the first)
            for (int pass = 0; pass < 2; pass++) {
                VertexConsumer vc = SafeBuffer.of(buf, pass == 0 ? RenderType.entityTranslucentEmissive(WING) : RenderType.eyes(WING));
                ps.pushPose();
                m.body.translateAndRotate(ps);
                float s = 0.35F + 0.65F * ease;
                for (int side = -1; side <= 1; side += 2) {
                    ps.pushPose();
                    ps.translate(side * 0.09F, 0.14F, 0.15F);
                    float sweep = Mth.lerp(ease, 80.0F, (elytra ? 38.0F : 24.0F) + flap);
                    ps.mulPose(Axis.YP.rotationDegrees(-side * sweep));
                    ps.mulPose(Axis.ZP.rotationDegrees(side * (75.0F * (1 - ease) - (6.0F + flap * 0.45F) * ease)));
                    ps.scale(s, s, s);
                    wing(ps.last().pose(), pass == 0 ? vc : null, pass == 1 ? vc : null, side, age, ease);
                    ps.popPose();
                }
                ps.popPose();
            }
        }

        /** Primaries hang from an arched spine (longer and splayed further outward toward the tip), gold coverts over them. */
        private static void wing(Matrix4f m, VertexConsumer body, VertexConsumer glow, int side, float age, float open) {
            int n = 10;
            for (int row = 0; row < 2; row++) {
                boolean covert = row == 1;
                for (int i = 0; i < n; i++) {
                    float t = i / (float) (n - 1);
                    float bx = t * 1.15F, by = 0.55F * Mth.sin(t * Mth.PI * 0.85F) + 0.12F * t;
                    float ang = (8.0F + 64.0F * t + Mth.sin(age * 0.3F + i * 0.7F) * 2.5F * open) * Mth.DEG_TO_RAD;
                    float len = covert ? 0.25F + 0.3F * t : 0.5F + 0.95F * (float) Math.pow(t, 0.8);
                    float w = covert ? 0.2F : 0.24F;
                    float dx = Mth.sin(ang), dy = -Mth.cos(ang), px = Mth.cos(ang), py = Mth.sin(ang);
                    if (covert) { bx -= dx * 0.04F; by -= dy * 0.04F; }
                    float tx = bx + dx * len, ty = by + dy * len;
                    float z = (covert ? 0.02F : 0.0F) + i * 0.003F;
                    int a = covert ? 230 : 205 - (int) (35 * t);
                    int c = covert ? (a << 24) | 0xFFE6A0 : (a << 24) | 0xFFFFFF;
                    if (body != null) feather(body, m, side, bx, by, tx, ty, px, py, w, z, c);
                    if (!covert && glow != null) feather(glow, m, side, bx, by, tx + dx * 0.08F, ty + dy * 0.08F, px, py, w * 1.6F, z - 0.005F, grey(70, 0.6F));
                }
            }
        }

        private static void feather(VertexConsumer vc, Matrix4f m, int side, float bx, float by, float tx, float ty, float px, float py,
                                    float w, float z, int color) {
            float hb = w * 0.5F, ht = w * 0.3F;
            // feather base at v=1, tip at v=0; y is "up" here, the model space has y pointing down
            quad(vc, m, side * (bx - px * hb), by - py * hb, side * (bx + px * hb), by + py * hb,
                    side * (tx + px * ht), ty + py * ht, side * (tx - px * ht), ty - py * ht, z, color, 0, 1, 1, 0);
        }
    }

    /** A double-sided quad; (x, y) with y up, mapped to model space (y down). */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                             float z, int c, float u0, float v0, float u1, float v1) {
        v(vc, m, x0, y0, z, u0, v0, c);
        v(vc, m, x1, y1, z, u1, v0, c);
        v(vc, m, x2, y2, z, u1, v1, c);
        v(vc, m, x3, y3, z, u0, v1, c);
        v(vc, m, x3, y3, z, u0, v1, c);
        v(vc, m, x2, y2, z, u1, v1, c);
        v(vc, m, x1, y1, z, u1, v0, c);
        v(vc, m, x0, y0, z, u0, v0, c);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float vv, int c) {
        vc.addVertex(m, x, -y, z).setColor(c).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 0, 1);
    }
}
