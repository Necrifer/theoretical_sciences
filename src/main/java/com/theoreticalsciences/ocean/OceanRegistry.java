package com.theoreticalsciences.ocean;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** 1.2.0: Titan's hidden ocean: its two creatures and its hydrothermal chimneys. */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class OceanRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<OceanMobs.VentGrazer>> VENT_GRAZER = ENTITIES.register("vent_grazer",
            () -> EntityType.Builder.of(OceanMobs.VentGrazer::new, MobCategory.WATER_CREATURE).sized(0.7F, 0.4F).clientTrackingRange(10).build("vent_grazer"));
    public static final DeferredHolder<EntityType<?>, EntityType<OceanMobs.IceEel>> ICE_EEL = ENTITIES.register("ice_eel",
            () -> EntityType.Builder.of(OceanMobs.IceEel::new, MobCategory.WATER_CREATURE).sized(0.8F, 0.5F).clientTrackingRange(10).build("ice_eel"));

    public static final DeferredHolder<EntityType<?>, EntityType<OceanMobs.BrineJelly>> BRINE_JELLY = ENTITIES.register("brine_jelly",
            () -> EntityType.Builder.of(OceanMobs.BrineJelly::new, MobCategory.WATER_CREATURE).sized(0.9F, 0.7F).clientTrackingRange(10).build("brine_jelly"));
    public static final DeferredHolder<EntityType<?>, EntityType<OceanMobs.VentCrab>> VENT_CRAB = ENTITIES.register("vent_crab",
            () -> EntityType.Builder.of(OceanMobs.VentCrab::new, MobCategory.WATER_CREATURE).sized(1.2F, 0.6F).clientTrackingRange(10).build("vent_crab"));

    // 1.3.0: the Hadal Trench's creatures
    public static final DeferredHolder<EntityType<?>, EntityType<HadalMobs.Anglerfish>> HADAL_ANGLERFISH = ENTITIES.register("hadal_anglerfish",
            () -> EntityType.Builder.of(HadalMobs.Anglerfish::new, MobCategory.WATER_CREATURE).sized(1.0F, 0.8F).clientTrackingRange(10).build("hadal_anglerfish"));
    public static final DeferredHolder<EntityType<?>, EntityType<HadalMobs.Siphonophore>> SIPHONOPHORE = ENTITIES.register("siphonophore",
            () -> EntityType.Builder.of(HadalMobs.Siphonophore::new, MobCategory.WATER_CREATURE).sized(0.8F, 0.8F).clientTrackingRange(10).build("siphonophore"));

    static {
        TAB.add(ITEMS.register("hadal_anglerfish_spawn_egg", () -> new DeferredSpawnEggItem(HADAL_ANGLERFISH, 0x101418, 0x80FFE0, new Item.Properties())));
        TAB.add(ITEMS.register("siphonophore_spawn_egg", () -> new DeferredSpawnEggItem(SIPHONOPHORE, 0xD8C8F0, 0xFF60C0, new Item.Properties())));
        TAB.add(ITEMS.register("brine_jelly_spawn_egg", () -> new DeferredSpawnEggItem(BRINE_JELLY, 0xA0E0F0, 0xFF80D0, new Item.Properties())));
        TAB.add(ITEMS.register("vent_crab_spawn_egg", () -> new DeferredSpawnEggItem(VENT_CRAB, 0xF4EEE4, 0xB0A090, new Item.Properties())));
        TAB.add(ITEMS.register("vent_grazer_spawn_egg", () -> new DeferredSpawnEggItem(VENT_GRAZER, 0xE8D8C8, 0xFF9070, new Item.Properties())));
        TAB.add(ITEMS.register("ice_eel_spawn_egg", () -> new DeferredSpawnEggItem(ICE_EEL, 0x203848, 0x60FFE0, new Item.Properties())));
    }

    // 1.2.0: the vent fields (the id is kept from the first build), brinicles and boulders
    public static final DeferredHolder<Feature<?>, OceanFeatures.VentField> HYDROTHERMAL_CHIMNEY = FEATURES.register("hydrothermal_chimney",
            () -> new OceanFeatures.VentField(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, OceanFeatures.Brinicles> BRINICLES = FEATURES.register("brinicles",
            () -> new OceanFeatures.Brinicles(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, OceanFeatures.Boulders> BOULDERS = FEATURES.register("ocean_boulders",
            () -> new OceanFeatures.Boulders(NoneFeatureConfiguration.CODEC));
    // 1.3.0: the Hadal Trench's own vent fields, lone chimneys and pillow basalt
    public static final DeferredHolder<Feature<?>, HadalFeatures.VentField> HADAL_VENT_FIELD = FEATURES.register("hadal_vent_field",
            () -> new HadalFeatures.VentField(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HadalFeatures.LoneChimney> HADAL_LONE_CHIMNEY = FEATURES.register("hadal_lone_chimney",
            () -> new HadalFeatures.LoneChimney(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, HadalFeatures.PillowBasalt> PILLOW_BASALT = FEATURES.register("pillow_basalt",
            () -> new HadalFeatures.PillowBasalt(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(VENT_GRAZER.get(), OceanMobs.VentGrazer.createAttributes().build());
            e.put(ICE_EEL.get(), OceanMobs.IceEel.createAttributes().build());
            e.put(BRINE_JELLY.get(), OceanMobs.BrineJelly.createAttributes().build());
            e.put(VENT_CRAB.get(), OceanMobs.VentCrab.createAttributes().build());
            e.put(HADAL_ANGLERFISH.get(), HadalMobs.Anglerfish.createAttributes().build());
            e.put(SIPHONOPHORE.get(), HadalMobs.Siphonophore.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            // the world spawns them in the water, as it does fish (and the top-up below keeps every kind about)
            water(e, VENT_GRAZER.get());
            water(e, ICE_EEL.get());
            water(e, BRINE_JELLY.get());
            water(e, VENT_CRAB.get());
            water(e, HADAL_ANGLERFISH.get());
            water(e, SIPHONOPHORE.get());
        });
    }

    static <T extends net.minecraft.world.entity.Mob> void water(RegisterSpawnPlacementsEvent e, EntityType<T> t) {
        e.register(t, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, r) -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /**
     * Life drifts in from the wider ocean: every fifteen seconds, if there are fewer than four grazers within 48 blocks
     * of a player, two swim in; if there is no eel and at least six grazers, one eel. Neither population can die out for
     * good, but within the area each rises and falls with the other.
     */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.level().dimension() != QHDimensions.TITAN_OCEAN) return;
        ServerLevel l = p.serverLevel();
        if (p.tickCount % 10 == 0) plume(p, l);
        if ((p.tickCount + p.getId()) % 200 != 0) return;
        var box = p.getBoundingBox().inflate(48);
        int grazers = l.getEntitiesOfClass(OceanMobs.VentGrazer.class, box).size();
        int eels = l.getEntitiesOfClass(OceanMobs.IceEel.class, box).size();
        int jellies = l.getEntitiesOfClass(OceanMobs.BrineJelly.class, box).size();
        int crabs = l.getEntitiesOfClass(OceanMobs.VentCrab.class, box).size();
        if (grazers < 8) for (int i = 0; i < 3; i++) arrive(l, p, VENT_GRAZER.get(), false);
        if (eels < 2 && grazers >= 5) arrive(l, p, ICE_EEL.get(), false);
        if (jellies < 6) for (int i = 0; i < 2; i++) arrive(l, p, BRINE_JELLY.get(), false);
        if (crabs < 4) arrive(l, p, VENT_CRAB.get(), true);
    }

    /**
     * The black smokers' plumes: water at 400 C gushes up out of each, and lifts and scalds whoever swims into it (within
     * 12 blocks above a smoker, and within a block of its column). Effects only.
     */
    static void plume(ServerPlayer p, ServerLevel l) {
        if (p.isCreative()) return;
        var smoker = QHBlocks.get("black_smoker");
        BlockPos at = p.blockPosition();
        for (int dy = 0; dy <= 12; dy++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (!l.getBlockState(at.offset(dx, -dy, dz)).is(smoker)) continue;
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 0.08, 0));
                    p.hurtMarked = true;
                    if (dy <= 5 && p.tickCount % 20 == 0) {
                        p.hurt(l.damageSources().hotFloor(), 2F);
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.vent_scald")
                                .withStyle(net.minecraft.ChatFormatting.GOLD), true);
                    }
                    return;
                }
    }

    /** One creature swims in, 8 to 24 blocks away (on the floor, for a crab). */
    static void arrive(ServerLevel l, ServerPlayer p, EntityType<? extends OceanMobs.Swimmer> type, boolean floor) {
        var r = l.random;
        for (int k = 0; k < 10; k++) {
            double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 16;
            BlockPos pos = BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY() + r.nextInt(13) - 6, p.getZ() + Math.sin(a) * d);
            if (!l.hasChunkAt(pos) || l.getFluidState(pos).isEmpty() || l.getFluidState(pos.above()).isEmpty()) continue;
            if (floor) {
                BlockPos.MutableBlockPos m = pos.mutable();
                while (m.getY() > 2 && !l.getFluidState(m.below()).isEmpty()) m.move(0, -1, 0);
                pos = m.immutable();
            }
            var m = type.create(l);
            if (m == null) return;
            m.moveTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, r.nextFloat() * 360, 0);
            m.finalizeSpawn(l, l.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            l.addFreshEntity(m);
            return;
        }
    }

    private OceanRegistry() {}
}
