package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.entity.NaniteSwarmEntity;
import com.theoreticalsciences.entity.ParadoxEcho;
import com.theoreticalsciences.registry.QHEntities;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.Random;

/**
 * Renderers for the containment-failure creatures.
 * <ul>
 *   <li>Destabilised Husk: zombie body, but drawn through the reality-destabilisation glitch (see DestabilisationRenderer).</li>
 *   <li>Void Wraith: a living black hole (or white hole) - drawn by EffectRenderer with the real lensing shader.</li>
 *   <li>Law Wraith: a rotating tesseract - drawn by EffectRenderer.</li>
 *   <li>Paradox Echo: a walking clock mechanism of gears, escapement, pendulum and hands; shatters on death.</li>
 *   <li>Nanite Swarm: a hovering cloud of nanites, some glowing; normal death tilt.</li>
 * </ul>
 */
public final class QHEntityRenderers {
    static final ResourceLocation WHITE = TheoreticalSciences.id("textures/misc/white.png");

    private static ResourceLocation tex(String name) {
        return TheoreticalSciences.id("textures/entity/" + name + ".png");
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(QHEntities.DESTABILISED_HUSK.get(), ctx -> new ZombieRenderer(ctx) {
            {
                addLayer(new EyesLayer<Zombie, ZombieModel<Zombie>>(this) {
                    @Override
                    public RenderType renderType() {
                        return RenderType.eyes(tex("destabilised_husk_glow"));
                    }
                });
            }

            @Override
            public ResourceLocation getTextureLocation(Zombie entity) {
                return tex("destabilised_husk");
            }
        });
        e.registerEntityRenderer(QHEntities.VOID_WRAITH.get(), SpaceWarpRenderer::new);
        e.registerEntityRenderer(QHEntities.LAW_WRAITH.get(), SpaceWarpRenderer::new);
        e.registerEntityRenderer(QHEntities.PARADOX_ECHO.get(), ClockworkRenderer::new);
        e.registerEntityRenderer(QHEntities.NANITE_SWARM.get(), NaniteRenderer::new);
        e.registerEntityRenderer(QHEntities.CONTAINMENT_RUPTURE.get(), NoopRenderer::new);
        e.registerEntityRenderer(QHEntities.GRAVITON_WISP.get(), BulkCreatureRenderers.Wisp::new);
        e.registerEntityRenderer(QHEntities.BRANE_STRIDER.get(), BulkCreatureRenderers.Strider::new);
        e.registerEntityRenderer(QHEntities.MODULI_CRAWLER.get(), BulkCreatureRenderers.Crawler::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.HYPEROBJECT.get(), ExpansionRenderers.Hyperobject::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.GENESIS.get(), AlphaOmegaRenderers.Genesis::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.ALPHA_OMEGA.get(), AlphaOmegaRenderers.Boss::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.FRACTAL_WYRM.get(), ExpansionRenderers.Wyrm::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.AXIOM_SENTINEL.get(), ExpansionRenderers.Sentinel::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.VIRTUAL_PAIR.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.HYPERCUBE_DRIFTER.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.PHOTON_MOTH.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.PLANCK_MITE.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.ANNIHILATOR.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.POSITRONIUM_SKITTERER.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.ANTIHYDROGEN_WISP.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.VACUUM_DRIFTER.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.TIDAL_HORROR.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.ORDINAL_HOUND.get(), ExpansionMobRenderers.Mob::new);
        e.registerEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.HILBERT_BELLHOP.get(), ExpansionMobRenderers.Mob::new);
    }

    /** Hands the entity to EffectRenderer, which draws it after the level with the lensing / tesseract code. */
    static class SpaceWarpRenderer<T extends Entity> extends EntityRenderer<T> {
        SpaceWarpRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0F;
        }

        @Override
        public void render(T entity, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            ClientHooks.WRAITHS.add(entity);     // only reached when the entity is actually in view
            super.render(entity, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return WHITE;
        }
    }

    /** A hovering cloud of nanites; bigger swarms have more of them. Uses the vanilla death tilt. */
    static class NaniteRenderer extends EntityRenderer<NaniteSwarmEntity> {
        NaniteRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.3F;
        }

        @Override
        public void render(NaniteSwarmEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            int size = e.getSize();
            float extent = 0.22F * size + 0.1F;
            int count = 18 + size * 22;
            ps.pushPose();
            ps.translate(0, e.getBbHeight() / 2, 0);
            if (e.deathTime > 0) {          // the normal death animation: topple sideways
                float f = Math.min(1F, Mth.sqrt((e.deathTime + partial - 1F) / 20F * 1.6F));
                ps.mulPose(Axis.ZP.rotationDegrees(f * 90F));
            }
            boolean hurt = e.hurtTime > 0 || e.deathTime > 0;
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            int ov = OverlayTexture.pack(0, hurt);
            Random rnd = new Random(e.getId() * 977L);
            for (int i = 0; i < count; i++) {
                float ph = rnd.nextFloat() * 6.283F, sp = 0.05F + rnd.nextFloat() * 0.1F, rad = extent * (0.3F + rnd.nextFloat() * 0.7F);
                float tilt = (rnd.nextFloat() - 0.5F) * 2.4F;
                float a = ph + t * sp * (rnd.nextBoolean() ? 1 : -1);
                float x = Mth.cos(a) * rad, z = Mth.sin(a) * rad, y = Mth.sin(a * 2 + ph) * rad * 0.5F + Mth.sin(tilt) * z * 0.6F;
                float s = 0.025F + rnd.nextFloat() * 0.03F;
                boolean lit = rnd.nextInt(5) == 0;
                ps.pushPose();
                ps.translate(x, y, z);
                ps.mulPose(Axis.YP.rotationDegrees(t * 9F + i * 37F));
                if (lit) ProcMesh.box(ps, glow, -s, -s, -s, s, s, s, 0xFF40FFE0, 0xF000F0, OverlayTexture.NO_OVERLAY);
                else ProcMesh.box(ps, solid, -s, -s, -s, s, s, s, 0xFF8A949E, light, ov);
                ps.popPose();
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(NaniteSwarmEntity entity) {
            return WHITE;
        }
    }

    @SuppressWarnings("unused")
    private static boolean unused(LivingEntity e) {
        return false;
    }

    private QHEntityRenderers() {}
}
