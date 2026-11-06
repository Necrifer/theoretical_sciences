package com.theoreticalsciences.math;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.ElementsFeatures;
import com.theoreticalsciences.world.QHDimensions;
import com.theoreticalsciences.world.Strata;
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

/** 1.2.0: registrations for the Mathematics branch's world: the Elements' creature and landmarks. */
public final class MathRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    /** Shown in the mod's creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<EuclidsAutomaton>> EUCLIDS_AUTOMATON = ENTITIES.register("euclids_automaton",
            () -> EntityType.Builder.of(EuclidsAutomaton::new, MobCategory.CREATURE).sized(0.8F, 2.1F).eyeHeight(1.7F).clientTrackingRange(10)
                    .build("euclids_automaton"));

    static {
        TAB.add(ITEMS.register("euclids_automaton_spawn_egg", () -> new DeferredSpawnEggItem(EUCLIDS_AUTOMATON, 0xE8E0D0, 0xD0A040, new Item.Properties())));
    }

    // 1.2.0: the creatures of the Axioms, Curved Space and the Continuum
    public static final DeferredHolder<EntityType<?>, EntityType<LayerMobs.ParadoxShade>> PARADOX_SHADE = ENTITIES.register("paradox_shade",
            () -> EntityType.Builder.of(LayerMobs.ParadoxShade::new, MobCategory.MONSTER).sized(0.7F, 1.9F).clientTrackingRange(10).build("paradox_shade"));
    public static final DeferredHolder<EntityType<?>, EntityType<LayerMobs.Tessellate>> TESSELLATE = ENTITIES.register("tessellate",
            () -> EntityType.Builder.of(LayerMobs.Tessellate::new, MobCategory.MONSTER).sized(1.1F, 1.1F).clientTrackingRange(10).build("tessellate"));
    public static final DeferredHolder<EntityType<?>, EntityType<LayerMobs.FourierWisp>> FOURIER_WISP = ENTITIES.register("fourier_wisp",
            () -> EntityType.Builder.of(LayerMobs.FourierWisp::new, MobCategory.CREATURE).sized(1.4F, 1.4F).fireImmune().clientTrackingRange(10).build("fourier_wisp"));

    static {
        TAB.add(ITEMS.register("paradox_shade_spawn_egg", () -> new DeferredSpawnEggItem(PARADOX_SHADE, 0xF0F0F0, 0x101010, new Item.Properties())));
        TAB.add(ITEMS.register("tessellate_spawn_egg", () -> new DeferredSpawnEggItem(TESSELLATE, 0x305070, 0xA0E0FF, new Item.Properties())));
        TAB.add(ITEMS.register("fourier_wisp_spawn_egg", () -> new DeferredSpawnEggItem(FOURIER_WISP, 0x102028, 0x80FFC0, new Item.Properties())));
    }

    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.LayerFeatures.LiarsVault> LIARS_VAULT = FEATURES.register("liars_vault",
            () -> new com.theoreticalsciences.world.LayerFeatures.LiarsVault(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.LayerFeatures.ProofStairs> PROOF_STAIRS = FEATURES.register("proof_stairs",
            () -> new com.theoreticalsciences.world.LayerFeatures.ProofStairs(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.LayerFeatures.MobiusArch> MOBIUS_ARCH = FEATURES.register("mobius_arch",
            () -> new com.theoreticalsciences.world.LayerFeatures.MobiusArch(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.LayerFeatures.HyperbolicFloor> HYPERBOLIC_FLOOR = FEATURES.register("hyperbolic_floor",
            () -> new com.theoreticalsciences.world.LayerFeatures.HyperbolicFloor(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.LayerFeatures.FourierHarp> FOURIER_HARP = FEATURES.register("fourier_harp",
            () -> new com.theoreticalsciences.world.LayerFeatures.FourierHarp(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, ElementsFeatures.CompassTower> COMPASS_TOWER = FEATURES.register("compass_tower",
            () -> new ElementsFeatures.CompassTower(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, ElementsFeatures.GeometersRuin> GEOMETERS_RUIN = FEATURES.register("geometers_ruin",
            () -> new ElementsFeatures.GeometersRuin(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(EUCLIDS_AUTOMATON.get(), EuclidsAutomaton.createAttributes().build());
            e.put(PARADOX_SHADE.get(), LayerMobs.ParadoxShade.createAttributes().build());
            e.put(TESSELLATE.get(), LayerMobs.Tessellate.createAttributes().build());
            e.put(FOURIER_WISP.get(), LayerMobs.FourierWisp.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            var hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            e.register(EUCLIDS_AUTOMATON.get(), SpawnPlacementTypes.ON_GROUND, hm,
                    (type, level, reason, pos, r) -> in(level, pos, Strata.Layer.ELEMENTS) && level.getBlockState(pos.below()).isSolid(), op);
            e.register(PARADOX_SHADE.get(), SpawnPlacementTypes.ON_GROUND, hm,
                    (type, level, reason, pos, r) -> in(level, pos, Strata.Layer.AXIOMS) && level.getBlockState(pos.below()).isSolid()
                            && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL, op);
            e.register(TESSELLATE.get(), SpawnPlacementTypes.ON_GROUND, hm,
                    (type, level, reason, pos, r) -> in(level, pos, Strata.Layer.CURVED_SPACE) && level.getBlockState(pos.below()).isSolid()
                            && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL, op);
            e.register(FOURIER_WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, hm,
                    (type, level, reason, pos, r) -> in(level, pos, Strata.Layer.CONTINUUM) && level.getBlockState(pos).isAir(), op);
        });
    }

    /** In this layer of the Platonic Realm. */
    static boolean in(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.core.BlockPos pos, Strata.Layer layer) {
        return level.getLevel().dimension() == QHDimensions.PLATONIC && Strata.layer(pos.getY()) == layer;
    }

    private MathRegistry() {}
}
