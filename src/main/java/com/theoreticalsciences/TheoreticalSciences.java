package com.theoreticalsciences;

import com.mojang.logging.LogUtils;
import com.theoreticalsciences.command.QHCommands;
import com.theoreticalsciences.network.QHNetwork;
import com.theoreticalsciences.registry.*;
import com.theoreticalsciences.research.ResearchManager;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/**
 * Theoretical Sciences - engineering at the edge of theoretical physics.
 * <p>
 * Design rules that keep this mod friendly inside large packs:
 * <ul>
 *   <li>No mixins, no access transformers, no coremods.</li>
 *   <li>All integrations (JEI, EMI, Jade, KubeJS, AE2) are optional and isolated in compat packages.</li>
 *   <li>Standard NeoForge capabilities (FE energy, fluid and item handlers) for all machines.</li>
 *   <li>Every recipe is a normal datapack recipe, so KubeJS / CraftTweaker / datapacks can edit them.</li>
 * </ul>
 */
@Mod(TheoreticalSciences.MODID)
public class TheoreticalSciences {
    public static final String MODID = "theoreticalsciences";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public TheoreticalSciences(IEventBus modBus, ModContainer container) {
        // Inventory Tweaks (Refoxed) puts its "sort" button on any slot group it finds: not on machine slots (round 23)
        modBus.addListener((net.neoforged.fml.event.lifecycle.InterModEnqueueEvent e) -> {
            for (String c : new String[]{"com.theoreticalsciences.machine.MachineMenu", "com.theoreticalsciences.client.screen.MachineScreen",
                    "com.theoreticalsciences.transcend.ConfluenceMenu", "com.theoreticalsciences.client.screen.ConfluenceScreen",
                    "com.theoreticalsciences.storage.InfinityCacheMenu", "com.theoreticalsciences.client.screen.InfinityCacheScreen",
                    "com.theoreticalsciences.extreme.ExtremeTableMenu", "com.theoreticalsciences.extreme.ExtremeTableScreen",
                    "com.theoreticalsciences.extreme.CompactorMenu", "com.theoreticalsciences.extreme.CompactorScreen",
                    "com.theoreticalsciences.dungeon.AnomalyEngineMenu", "com.theoreticalsciences.client.screen.AnomalyEngineScreen"})
                net.neoforged.fml.InterModComms.sendTo("invtweaks", "blacklist-screen", () -> c);
        });
        modBus.addListener(com.theoreticalsciences.network.SingularityConfigTask::register);
        // round 31: component tiers, Core Forge recipes and machine numbers from datapacks / KubeJS
        com.theoreticalsciences.custom.PackData.register(modBus);
        QHFluids.register(modBus);
        QHBlocks.register(modBus);
        QHItems.register(modBus);
        QHBlockEntities.register(modBus);
        QHMenus.register(modBus);
        QHRecipes.register(modBus);
        QHParticles.register(modBus);
        QHFeatures.register(modBus);
        QHTabs.register(modBus);
        QHEffects.register(modBus);
        QHEntities.register(modBus);
        com.theoreticalsciences.registry.QHSounds.register(modBus);
        com.theoreticalsciences.extreme.ExtremeRegistry.register(modBus);
        com.theoreticalsciences.converge.ConvergenceRegistry.register(modBus);
        com.theoreticalsciences.transcend.TranscendRegistry.register(modBus);
        com.theoreticalsciences.registry.QHLogistics.register(modBus);
        com.theoreticalsciences.endgame.EndgameRegistry.register(modBus);

        modBus.addListener(QHNetwork::register);
        modBus.addListener(QHCapabilities::register);
        modBus.addListener(com.theoreticalsciences.compat.MEBridge::registerCaps);
        modBus.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, com.theoreticalsciences.transcend.Uncopyable::registerCaps);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.transcend.Uncopyable::onServerTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> com.theoreticalsciences.transcend.CoreForge.invalidate());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> com.theoreticalsciences.extreme.Collections.invalidate());
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.endgame.EntropyCenserItem::cloudApplies);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.transcend.Uncopyable::onRightClick);

        container.registerConfig(ModConfig.Type.COMMON, QHConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, QHConfig.CLIENT_SPEC);

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(ResearchManager.INSTANCE));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(com.theoreticalsciences.machine.physics.PhysicsManager.INSTANCE));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(com.theoreticalsciences.research.GlyphRegistry.INSTANCE));
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(com.theoreticalsciences.extreme.Singularities.INSTANCE));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> {
            if (e.getUpdateCause() == net.neoforged.neoforge.event.TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) com.theoreticalsciences.extreme.Singularities.invalidate();
        });
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.research.GlyphService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.zones.StructureZones::onServerTick);
        // round 15: the Shutdown Sequence, the Sundering and the Rekindling
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.transcend.Omnipotence::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.transcend.Omnipotence::onLogin);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.transcend.Omnipotence::onRespawn);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.zones.StructureZones::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            com.theoreticalsciences.world.zones.StructureZones.forget(e.getEntity().getUUID());
            com.theoreticalsciences.world.DimensionLaws.forget(e.getEntity().getUUID());
            com.theoreticalsciences.world.VoidFall.forget(e.getEntity().getUUID());                 // round 29
            com.theoreticalsciences.research.ResearchService.forgetAttempts(e.getEntity().getUUID()); // round 29
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> com.theoreticalsciences.research.ResearchService.forgetAttempts(null));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> com.theoreticalsciences.world.zones.StructureZones.forget(e.getEntity().getUUID()));
        // round 35: the Transcendental and the Living Proof only open the intended way
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionGate::onTravel);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionGate::onArrived);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionGate::onPlayerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> com.theoreticalsciences.world.DimensionGate.forget(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> ResearchService.onDatapackSync(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> ResearchService.onLogin(e));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> QHCommands.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.endgame.OmnilexAbilities::onPlayerTick);
        com.theoreticalsciences.network.DestabilisedPayload.registerServerEvents();
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.VoidFall::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.SpawnRules::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.SpawnRules::onJoin);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.machine.tuning.Materialisation::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> com.theoreticalsciences.machine.tuning.Materialisation.clear());
        // structures generated in the last tick still get their zones; nothing carries over into the next world
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> com.theoreticalsciences.world.zones.StructureZones.drain(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) -> com.theoreticalsciences.world.zones.StructureZones.reset());
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.item.ContainmentCells::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onHeal);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onDamage);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onKnockback);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.world.DimensionLaws::onHarvestCheck);
        NeoForge.EVENT_BUS.addListener(ResearchService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.theoreticalsciences.field.FieldHazards::onPlayerTick);
        com.theoreticalsciences.boss.AlphaOmegaEvents.register();
        // Virtual particles dropped in the Quantum Foam give themselves back to the vacuum within half a minute
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity ie && e.getLevel().dimension() == com.theoreticalsciences.world.QHDimensions.FOAM
                    && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath().startsWith("virtual_"))
                ie.lifespan = Math.min(ie.lifespan, 600);
        });
        // a creature that dies while nanite-infected bursts into a fresh swarm
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            var dead = e.getEntity();
            if (dead.level() instanceof net.minecraft.server.level.ServerLevel sl && dead.hasEffect(QHEffects.NANITE_INFECTION)
                    && !(dead instanceof com.theoreticalsciences.entity.NaniteSwarmEntity) && QHConfig.FAILURE_HAZARDS.get()) {
                var sw = com.theoreticalsciences.entity.NaniteSwarmEntity.spawn(sl, dead.getX(), dead.getY() + dead.getBbHeight() * 0.5, dead.getZ(), dead.getBbWidth() > 0.9F ? 2 : 1);
                if (sw != null) sl.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), dead.getX(), dead.getY() + dead.getBbHeight() * 0.5,
                        dead.getZ(), 40, dead.getBbWidth() * 0.4, dead.getBbHeight() * 0.4, dead.getBbWidth() * 0.4, 0.15);
            }
        });
    }
}
