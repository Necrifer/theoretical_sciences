package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * Round 15 endgame: the Omnipotent Edge, the Scepter of Singularities, the Raiment of Transcendence, the Ex Nihilo
 * Engine and the Lawgiver's Codex (and, round 32, Eschaton Charge and the Dodecaract Excavator). Everything this
 * package registers or listens to is wired up here.
 */
public final class EndgameRegistry {
    static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, TheoreticalSciences.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheoreticalSciences.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TRANSCENDENCE = ARMOR_MATERIALS.register("transcendence", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.HELMET, 8);
                m.put(ArmorItem.Type.CHESTPLATE, 14);
                m.put(ArmorItem.Type.LEGGINGS, 11);
                m.put(ArmorItem.Type.BOOTS, 8);
                m.put(ArmorItem.Type.BODY, 20);
            }),
            30, com.theoreticalsciences.registry.QHSounds.EQUIP, () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(TheoreticalSciences.id("transcendence"))), 8.0F, 0.5F));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExNihiloEngineBlockEntity>> EX_NIHILO = BLOCK_ENTITIES.register("ex_nihilo_engine",
            () -> BlockEntityType.Builder.of(ExNihiloEngineBlockEntity::new, QHBlocks.get("ex_nihilo_engine")).build(null));

    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        EschatonCharge.register(modBus);           // round 32: the charge every endgame item runs on
        modBus.addListener((RegisterCapabilitiesEvent e) ->
                e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, EX_NIHILO.get(), (be, side) -> ExNihiloEngineBlockEntity.INFINITE));

        var bus = NeoForge.EVENT_BUS;
        // Omnipotent Edge: Chronal Stasis bookkeeping
        bus.addListener(ChronalStasis::onEntityTick);
        bus.addListener(ChronalStasis::onJoin);
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> CodexLaws.forget(e.getEntity().getUUID()));
        // Scepter of Singularities: lingering black holes
        bus.addListener(ScepterEffects::onServerTick);
        bus.addListener(ScepterEffects::onServerStopped);
        // Raiment of Transcendence
        bus.addListener(Raiment::onPlayerTick);
        bus.addListener(Raiment::onIncomingDamage);
        bus.addListener(Raiment::onFall);
        // Lawgiver's Codex
        bus.addListener(CodexLaws::onServerTick);
        bus.addListener(CodexLaws::onJoin);
        bus.addListener(CodexLaws::onFinalizeSpawn);
        bus.addListener(CodexLaws::onPositionCheck);
        bus.addListener(CodexLaws::onChangeTarget);
        // round 32: Eschaton Charge, the Edge's harvest, the Dodecaract Excavator
        bus.addListener(EschatonCharge::onTooltip);
        bus.addListener(OmnipotentEdgeItem::onDrops);
        bus.addListener(OmnipotentEdgeItem::onExperience);
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, DodecaractExcavatorItem::onBreak);
        bus.addListener(ExcavatorJobs::onServerTick);
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> ExcavatorJobs.onServerStopped());
        bus.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e) -> ExcavatorJobs.clearCache());
    }

    private EndgameRegistry() {}
}
