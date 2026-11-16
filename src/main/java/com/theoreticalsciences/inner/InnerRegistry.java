package com.theoreticalsciences.inner;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
import java.util.Set;

/** 1.2.0: Inner Space's creatures, the Tumour Hydra and the landmarks (see {@link InnerMobs}, {@link TumourHydra}, {@link InnerFeatures}). */
public final class InnerRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    /** Shown in the mod's creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();
    static final Set<String> IDS = Set.of("red_cell", "macrophage", "t_cell", "polymerase", "tumour_hydra", "dreaming_colony");

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, MobCategory cat,
                                                                                   float w, float h, int bg, int fg) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(id, () ->
                EntityType.Builder.of(f, cat).sized(w, h).eyeHeight(h * 0.6F).clientTrackingRange(10).build(id));
        TAB.add(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(holder, bg, fg, new Item.Properties())));
        return holder;
    }

    public static final DeferredHolder<EntityType<?>, EntityType<InnerMobs.RedCell>> RED_CELL =
            mob("red_cell", InnerMobs.RedCell::new, MobCategory.CREATURE, 1.3F, 0.5F, 0xB01820, 0xFF6060);
    public static final DeferredHolder<EntityType<?>, EntityType<InnerMobs.Macrophage>> MACROPHAGE =
            mob("macrophage", InnerMobs.Macrophage::new, MobCategory.MONSTER, 1.6F, 1.4F, 0xD8C8B0, 0x8060A0);
    public static final DeferredHolder<EntityType<?>, EntityType<InnerMobs.TCell>> T_CELL =
            mob("t_cell", InnerMobs.TCell::new, MobCategory.MONSTER, 0.9F, 1.1F, 0xA0C0FF, 0x4060C0);
    public static final DeferredHolder<EntityType<?>, EntityType<InnerMobs.Polymerase>> POLYMERASE =
            mob("polymerase", InnerMobs.Polymerase::new, MobCategory.CREATURE, 1.0F, 0.9F, 0x80C0A0, 0xE0FFF0);
    /** 1.2.0: the Antiverse's Mirror Microbe (registered with Inner Space's creatures; it lives in the Antiverse). */
    public static final DeferredHolder<EntityType<?>, EntityType<MirrorMicrobe>> MIRROR_MICROBE =
            mob("mirror_microbe", MirrorMicrobe::new, MobCategory.MONSTER, 0.7F, 0.7F, 0xE0F8FF, 0x40C0E0);
    public static final DeferredHolder<EntityType<?>, EntityType<TumourHydra.Hydra>> TUMOUR_HYDRA =
            mob("tumour_hydra", TumourHydra.Hydra::new, MobCategory.MONSTER, 4.4F, 4.6F, 0x803048, 0xFF90B0);

    public static final DeferredHolder<Feature<?>, InnerFeatures.CellCluster> CELL_CLUSTER = FEATURES.register("cell_cluster",
            () -> new InnerFeatures.CellCluster(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.RibosomeWorkshop> RIBOSOME_WORKSHOP = FEATURES.register("ribosome_workshop",
            () -> new InnerFeatures.RibosomeWorkshop(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.GerminalCentre> GERMINAL_CENTRE = FEATURES.register("germinal_centre",
            () -> new InnerFeatures.GerminalCentre(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.HelixSpire> HELIX_SPIRE = FEATURES.register("helix_spire",
            () -> new InnerFeatures.HelixSpire(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.NuclearPore> NUCLEAR_PORE = FEATURES.register("nuclear_pore",
            () -> new InnerFeatures.NuclearPore(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.TumourGrowth> TUMOUR_GROWTH = FEATURES.register("tumour_growth",
            () -> new InnerFeatures.TumourGrowth(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, InnerFeatures.FieldStation> BSL4_FIELD_STATION = FEATURES.register("bsl4_field_station",
            () -> new InnerFeatures.FieldStation(NoneFeatureConfiguration.CODEC));

    /** Whether this is one of Inner Space's own creatures (they are spawned a few at a time, see InnerHazards). */
    public static boolean isInnerCreature(Entity e) {
        var k = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return k.getNamespace().equals(TheoreticalSciences.MODID) && IDS.contains(k.getPath());
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(RED_CELL.get(), InnerMobs.RedCell.createAttributes().build());
            e.put(MACROPHAGE.get(), InnerMobs.Macrophage.createAttributes().build());
            e.put(T_CELL.get(), InnerMobs.TCell.createAttributes().build());
            e.put(POLYMERASE.get(), InnerMobs.Polymerase.createAttributes().build());
            e.put(TUMOUR_HYDRA.get(), TumourHydra.Hydra.createAttributes().build());
            e.put(MIRROR_MICROBE.get(), MirrorMicrobe.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            // Inner Space spawns its own (InnerHazards.populate); the world's spawner never places them
            own(e, RED_CELL.get(), false);           // red cells: in the plasma, or wherever there is room
            ground(e, MACROPHAGE.get());              // the walkers spawn standing on something, never in mid-air
            ground(e, T_CELL.get());
            ground(e, POLYMERASE.get());
            ground(e, TUMOUR_HYDRA.get());
            own(e, MIRROR_MICROBE.get(), false);
        });
    }

    static <T extends Mob> void own(RegisterSpawnPlacementsEvent e, EntityType<T> t, boolean unused) {
        // the world spawns them as their biomes list them (no light check: Inner Space is never dark)
        e.register(t, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, (type, level, reason, pos, rnd) ->
                level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    static <T extends Mob> void ground(RegisterSpawnPlacementsEvent e, EntityType<T> t) {
        e.register(t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, rnd) ->
                level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private InnerRegistry() {}
}
