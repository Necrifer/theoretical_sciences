package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.BulkMonolithFeature;
import com.theoreticalsciences.world.CrystalSpireFeature;
import com.theoreticalsciences.world.ObservatoryRuinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class QHFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);

    public static final DeferredHolder<Feature<?>, ObservatoryRuinFeature> OBSERVATORY_RUIN = FEATURES.register("observatory_ruin",
            () -> new ObservatoryRuinFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BulkMonolithFeature> BULK_MONOLITH = FEATURES.register("bulk_monolith",
            () -> new BulkMonolithFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, CrystalSpireFeature> CRYSTAL_SPIRE = FEATURES.register("crystal_spire",
            () -> new CrystalSpireFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.WorldTreeFeature> WORLD_TREE = FEATURES.register("world_tree",
            () -> new com.theoreticalsciences.world.WorldTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.GravitonReefFeature> GRAVITON_REEF = FEATURES.register("graviton_reef",
            () -> new com.theoreticalsciences.world.GravitonReefFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.DerelictStationFeature> DERELICT_STATION = FEATURES.register("derelict_station",
            () -> new com.theoreticalsciences.world.DerelictStationFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.CalabiYauKnotFeature> CALABI_YAU_KNOT = FEATURES.register("calabi_yau_knot",
            () -> new com.theoreticalsciences.world.CalabiYauKnotFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.PlatonicSolidFeature> PLATONIC_SOLID = FEATURES.register("platonic_solid",
            () -> new com.theoreticalsciences.world.PlatonicSolidFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.world.OreVeinFeature> ORE_VEIN = FEATURES.register("ore_vein",
            () -> new com.theoreticalsciences.world.OreVeinFeature(com.theoreticalsciences.world.OreVeinFeature.Config.CODEC));
    public static final DeferredHolder<Feature<?>, com.theoreticalsciences.dungeon.AnomalyVaultFeature> ANOMALY_VAULT = FEATURES.register("anomaly_vault",
            () -> new com.theoreticalsciences.dungeon.AnomalyVaultFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    private QHFeatures() {}
}
