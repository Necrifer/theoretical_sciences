package com.theoreticalsciences.engineering;

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

/** 1.4.0 Engineering of the Large: the Rust Mite and the derelict orbital rings. */
public final class EngineeringRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<RustMite>> RUST_MITE = ENTITIES.register("rust_mite",
            () -> EntityType.Builder.of(RustMite::new, MobCategory.MONSTER).sized(0.6F, 0.4F).eyeHeight(0.25F).clientTrackingRange(8)
                    .build("rust_mite"));

    public static final DeferredHolder<Feature<?>, EngineeringFeatures.DerelictRing> DERELICT_RING = FEATURES.register("derelict_ring",
            () -> new EngineeringFeatures.DerelictRing(NoneFeatureConfiguration.CODEC));

    static {
        TAB.add(ITEMS.register("rust_mite_spawn_egg", () -> new DeferredSpawnEggItem(RUST_MITE, 0x8A4A22, 0xD08040, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(RUST_MITE.get(), RustMite.createAttributes().build()));
        // a crawler: it spawns standing on something solid, in any light (it is not a creature of the dark)
        bus.addListener((RegisterSpawnPlacementsEvent e) -> e.register(RUST_MITE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, rnd) -> level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private EngineeringRegistry() {}
}
