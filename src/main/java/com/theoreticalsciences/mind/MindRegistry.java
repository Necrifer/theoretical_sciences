package com.theoreticalsciences.mind;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** 1.4.0 Mind and Language: the Dreaming Colony, the Neural Cortex's neurons and the Overworld's library ruins. */
public final class MindRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<DreamingColony>> DREAMING_COLONY = ENTITIES.register("dreaming_colony",
            () -> EntityType.Builder.of(DreamingColony::new, MobCategory.MONSTER).sized(1.3F, 1.3F).eyeHeight(0.7F).clientTrackingRange(10)
                    .build("dreaming_colony"));

    public static final DeferredHolder<Feature<?>, MindFeatures.NeuronArbor> NEURON_ARBOR = FEATURES.register("neuron_arbor",
            () -> new MindFeatures.NeuronArbor(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, MindFeatures.LibraryRuin> LIBRARY_RUIN = FEATURES.register("library_ruin",
            () -> new MindFeatures.LibraryRuin(NoneFeatureConfiguration.CODEC));

    static {
        TAB.add(ITEMS.register("dreaming_colony_spawn_egg", () -> new DeferredSpawnEggItem(DREAMING_COLONY, 0xE8D8F0, 0xB070FF, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(DREAMING_COLONY.get(), DreamingColony.createAttributes().build()));
        // Inner Space is never dark: it spawns where its biome lists it, in open air, with no light check
        bus.addListener((RegisterSpawnPlacementsEvent e) -> e.register(DREAMING_COLONY.get(), SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING, (type, level, reason, pos, rnd) -> level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private MindRegistry() {}
}
