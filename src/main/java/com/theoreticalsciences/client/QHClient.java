package com.theoreticalsciences.client;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.client.particle.QHParticle;
import com.theoreticalsciences.client.render.BulkSkyEffects;
import com.theoreticalsciences.client.render.EffectRenderer;
import com.theoreticalsciences.client.render.QHShaders;
import com.theoreticalsciences.client.render.SingularItemRenderer;
import com.theoreticalsciences.client.screen.MachineScreen;
import com.theoreticalsciences.item.SingularItem;
import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.registry.QHMenus;
import com.theoreticalsciences.registry.QHParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TheoreticalSciences.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class QHClient {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // round 30: Clusters, Hyperclusters and Ultraclusters each show their own animated glow (model overrides)
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(com.theoreticalsciences.registry.QHItems.get("singularity_cluster"),
                TheoreticalSciences.id("cluster_level"), (stack, level, entity, seed) -> com.theoreticalsciences.extreme.Singularities.clusterLevel(stack) / 10F));
        event.enqueueWork(() -> QHFluids.ALL.values().forEach(e -> {
            ItemBlockRenderTypes.setRenderLayer(e.source.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(e.flowing.get(), RenderType.translucent());
        }));
        NeoForge.EVENT_BUS.addListener(EffectRenderer::onRenderLevel);
        // round 23: the mod's music is played (and faded in and out) by QHMusic; vanilla stays silent where it is in charge
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.SelectMusicEvent e) -> {
            if (com.theoreticalsciences.client.QHMusic.ours()) e.setMusic(null);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> com.theoreticalsciences.client.QHMusic.tick());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> com.theoreticalsciences.client.QHMusic.reset());
        // standing near a meltdown shakes the view
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null || com.theoreticalsciences.client.ClientHooks.FAILURES.isEmpty()) return;
            double tick = mc.level.getGameTime() + e.getPartialTick();
            float a = EffectRenderer.shake(e.getCamera().getPosition(), tick);
            if (a <= 0) return;
            e.setYaw(e.getYaw() + (float) Math.sin(tick * 2.9) * a);
            e.setPitch(e.getPitch() + (float) Math.cos(tick * 3.7) * a);
            e.setRoll(e.getRoll() + (float) Math.sin(tick * 1.7) * a * 0.6F);
        });
        com.theoreticalsciences.client.render.DestabilisationRenderer.register();
        // inside a higher-dimensional structure, space is folded to match (round 14)
        NeoForge.EVENT_BUS.addListener(DistortionRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(DistortionRenderer::onFov);
        NeoForge.EVENT_BUS.addListener(DistortionRenderer::onAngles);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> DistortionRenderer.reset());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientHooks.SINGULARITIES.clear());
        // the next server may have a different singularity list (and colours)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> {
            com.theoreticalsciences.extreme.Singularities.clearClient();
            com.theoreticalsciences.client.SingularityColours.clear();
        });
        com.theoreticalsciences.endgame.EndgameClient.setup();
        // the Forge's simulated-universe recipes read this game's recipes (round 22)
        com.theoreticalsciences.transcend.CoreForge.CLIENT_RECIPES = () -> {
            var c = net.minecraft.client.Minecraft.getInstance().getConnection();
            return c == null ? null : c.getRecipeManager();
        };
        com.theoreticalsciences.transcend.CoreForge.CLIENT_REGISTRIES = () -> {
            var l = net.minecraft.client.Minecraft.getInstance().level;
            return l == null ? null : l.registryAccess();
        };
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RecipesUpdatedEvent e) -> com.theoreticalsciences.transcend.CoreForge.invalidate());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> com.theoreticalsciences.transcend.CoreForge.invalidate());
        // round 31: the next server may have other (or no) datapack tiers, forge recipes and machine numbers
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> {
            // (not in single player: the built-in server shares this data and may still be saving its machines)
            if (net.minecraft.client.Minecraft.getInstance().getSingleplayerServer() == null) com.theoreticalsciences.custom.PackData.clearClient();
        });
        // round 27: the Pleroma Reconstitutor's finale is drawn in the world; travel animations forget themselves on disconnect
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.client.cinematic.RekindleCinematicClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> {
            com.theoreticalsciences.client.transit.TransitClient.reset();
            com.theoreticalsciences.client.cinematic.RekindleCinematicClient.reset();
        });
    }

    /** Round 27: full-screen overlays for dimension travel and the Rekindling cinematic. */
    @SubscribeEvent
    public static void guiLayers(RegisterGuiLayersEvent event) {
        com.theoreticalsciences.client.transit.TransitClient.registerLayers(event);
        com.theoreticalsciences.client.cinematic.RekindleCinematicClient.registerLayers(event);
    }

    /** Round 27: each of our dimensions gets its own loading page, entering and leaving. */
    @SubscribeEvent
    public static void dimensionScreens(RegisterDimensionTransitionScreenEvent event) {
        com.theoreticalsciences.client.transit.TransitClient.registerScreens(event);
    }

    /** A resource pack change can change item textures: forget the colours read from them. */
    @SubscribeEvent
    public static void reloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> com.theoreticalsciences.client.SingularityColours.clear());
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> com.theoreticalsciences.client.codex.CodexData.clear());   // round 30: re-read the Codex
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        com.theoreticalsciences.client.render.QHEntityRenderers.register(event);
        event.registerBlockEntityRenderer(com.theoreticalsciences.registry.QHBlockEntities.FRACTURED_REALITY.get(),
                com.theoreticalsciences.client.render.FracturedRealityRenderer::new);
        event.registerBlockEntityRenderer(com.theoreticalsciences.transcend.TranscendRegistry.FORGED_CORE_BE.get(), com.theoreticalsciences.client.render.ForgedCoreRenderer::new);
        event.registerBlockEntityRenderer(com.theoreticalsciences.registry.QHLogistics.CONV_PEDESTAL.get(), com.theoreticalsciences.client.render.ConvergenceRenderers.Pedestal::new);
        event.registerBlockEntityRenderer(com.theoreticalsciences.registry.QHLogistics.CONV_ALTAR.get(), com.theoreticalsciences.client.render.ConvergenceRenderers.Altar::new);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(QHMenus.MACHINE.get(), MachineScreen::new);
        event.register(QHMenus.INFINITY_CACHE.get(), com.theoreticalsciences.client.screen.InfinityCacheScreen::new);
        event.register(QHMenus.ANOMALY_ENGINE.get(), com.theoreticalsciences.client.screen.AnomalyEngineScreen::new);
        event.register(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_MENU.get(), com.theoreticalsciences.extreme.ExtremeTableScreen::new);
        event.register(com.theoreticalsciences.extreme.ExtremeRegistry.COMPACTOR_MENU.get(), com.theoreticalsciences.extreme.CompactorScreen::new);
        event.register(com.theoreticalsciences.transcend.TranscendRegistry.CONFLUENCE_MENU.get(), com.theoreticalsciences.client.screen.ConfluenceScreen::new);
    }

    @SubscribeEvent
    public static void itemColours(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        SingularityColours.register(event);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) {
        QHShaders.register(event);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(QHParticles.TACHYON.get(), s -> new QHParticle.Provider(s, QHParticle.Kind.TACHYON));
        event.registerSpriteSet(QHParticles.QUANTUM_FOAM.get(), s -> new QHParticle.Provider(s, QHParticle.Kind.FOAM));
        event.registerSpriteSet(QHParticles.HAWKING.get(), s -> new QHParticle.Provider(s, QHParticle.Kind.HAWKING));
        event.registerSpriteSet(QHParticles.INFALL.get(), s -> new QHParticle.Provider(s, QHParticle.Kind.INFALL));
        event.registerSpriteSet(QHParticles.DARK_MOTE.get(), s -> new QHParticle.Provider(s, QHParticle.Kind.DARK));
        // round 36: the mod's own replacements for the vanilla particles
        java.util.Map<net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.particles.ParticleType<?>, net.minecraft.core.particles.SimpleParticleType>,
                com.theoreticalsciences.client.particle.QHFxParticle.Kind> fx = new java.util.LinkedHashMap<>();
        fx.put(QHParticles.PHOTON, com.theoreticalsciences.client.particle.QHFxParticle.Kind.PHOTON);
        fx.put(QHParticles.PHASE_MOTE, com.theoreticalsciences.client.particle.QHFxParticle.Kind.PHASE);
        fx.put(QHParticles.ARC_SPARK, com.theoreticalsciences.client.particle.QHFxParticle.Kind.ARC);
        fx.put(QHParticles.FLASH, com.theoreticalsciences.client.particle.QHFxParticle.Kind.FLASH);
        fx.put(QHParticles.GLYPH, com.theoreticalsciences.client.particle.QHFxParticle.Kind.GLYPH);
        fx.put(QHParticles.VOID_INK, com.theoreticalsciences.client.particle.QHFxParticle.Kind.INK);
        fx.put(QHParticles.SHOCKWAVE, com.theoreticalsciences.client.particle.QHFxParticle.Kind.RING);
        fx.put(QHParticles.BURST, com.theoreticalsciences.client.particle.QHFxParticle.Kind.BURST);
        fx.put(QHParticles.VAPOUR, com.theoreticalsciences.client.particle.QHFxParticle.Kind.VAPOUR);
        fx.put(QHParticles.FAULT, com.theoreticalsciences.client.particle.QHFxParticle.Kind.FAULT);
        fx.put(QHParticles.RESOLVE, com.theoreticalsciences.client.particle.QHFxParticle.Kind.RESOLVE);
        fx.put(QHParticles.PLASMA, com.theoreticalsciences.client.particle.QHFxParticle.Kind.PLASMA);
        fx.forEach((h, k) -> event.registerSpriteSet(h.get(), s -> new com.theoreticalsciences.client.particle.QHFxParticle.Provider(s, k)));
        event.registerSpriteSet(QHParticles.GLOW.get(), com.theoreticalsciences.client.particle.QHFxParticle.GlowProvider::new);
    }

    @SubscribeEvent
    public static void dimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(TheoreticalSciences.id("the_bulk"), new BulkSkyEffects());
        event.register(TheoreticalSciences.id("hyperdimensional_reality"), new com.theoreticalsciences.client.render.HyperrealSkyEffects());
        event.register(TheoreticalSciences.id("platonic_realm"), new com.theoreticalsciences.client.render.FractalSkyEffects(false));
        event.register(TheoreticalSciences.id("transcendental"), new com.theoreticalsciences.client.render.FractalSkyEffects(true));
        event.register(TheoreticalSciences.id("black_hole_interior"), new com.theoreticalsciences.client.render.FractalSkyEffects(2));
        event.register(TheoreticalSciences.id("white_hole_interior"), new com.theoreticalsciences.client.render.FractalSkyEffects(3));
        event.register(TheoreticalSciences.id("transfinite_hierarchy"), new com.theoreticalsciences.client.render.FractalSkyEffects(4));
        event.register(TheoreticalSciences.id("living_proof"), new com.theoreticalsciences.client.render.FractalSkyEffects(5));
    }

    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        for (QHFluids.Entry e : QHFluids.ALL.values()) {
            String id = e.def.id();
            int fog = e.def.color();
            ResourceLocation still = TheoreticalSciences.id("block/fluid/" + id + "_still");
            ResourceLocation flow = TheoreticalSciences.id("block/fluid/" + id + "_flow");
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flow;
                }

                // round 30: a mod's Essence looks like its mod (EssenceLook) - shown untinted then; otherwise its colour
                @Override
                public ResourceLocation getStillTexture(net.neoforged.neoforge.fluids.FluidStack stack) {
                    ResourceLocation t = id.equals("mod_essence") ? EssenceLook.texture(stack) : null;
                    return t != null ? t : still;
                }

                @Override
                public ResourceLocation getFlowingTexture(net.neoforged.neoforge.fluids.FluidStack stack) {
                    ResourceLocation t = id.equals("mod_essence") ? EssenceLook.texture(stack) : null;
                    return t != null ? t : flow;
                }

                @Override
                public int getTintColor(net.neoforged.neoforge.fluids.FluidStack stack) {
                    // a mod's Essence is coloured for its mod (round 26)
                    if (!id.equals("mod_essence")) return 0xFFFFFFFF;
                    return EssenceLook.texture(stack) != null ? 0xFFFFFFFF : com.theoreticalsciences.transcend.Essences.colour(stack);
                }

                @Override
                public Vector3f modifyFogColor(Camera camera, float partialTick, net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    return new Vector3f(((fog >> 16) & 255) / 255F, ((fog >> 8) & 255) / 255F, (fog & 255) / 255F);
                }
            }, e.type.get());
        }
        BlockEntityWithoutLevelRenderer[] renderer = new BlockEntityWithoutLevelRenderer[1];
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer[0] == null) renderer[0] = new SingularItemRenderer();
                return renderer[0];
            }
        };
        List<Item> singular = new ArrayList<>();
        QHItems.ALL.values().forEach(h -> {
            if (h.get() instanceof SingularItem) singular.add(h.get());
        });
        event.registerItem(ext, singular.toArray(Item[]::new));
    }

    @SuppressWarnings("unused")
    private static boolean isOurs(FluidState s) {
        return false;
    }

    private QHClient() {}
}
