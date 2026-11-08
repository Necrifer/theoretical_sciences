package com.theoreticalsciences.deeptime;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.4.0 Deep Time: the revived creatures (wild only: they never spawn by themselves, only from a Revival Vat's
 * capsule), their capsules, and the Overworld's dig sites and greenhouse ruins.
 */
public final class DeepTimeRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<RevivedMobs.Mammoth>> MAMMOTH = ENTITIES.register("mammoth",
            () -> EntityType.Builder.of(RevivedMobs.Mammoth::new, MobCategory.CREATURE).sized(2.4F, 3.3F).eyeHeight(2.6F).clientTrackingRange(10)
                    .build("mammoth"));
    public static final DeferredHolder<EntityType<?>, EntityType<RevivedMobs.Meganeura>> MEGANEURA = ENTITIES.register("meganeura",
            () -> EntityType.Builder.of(RevivedMobs.Meganeura::new, MobCategory.CREATURE).sized(1.0F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8)
                    .build("meganeura"));

    public static final DeferredHolder<Feature<?>, DeepTimeFeatures.DigSite> DIG_SITE = FEATURES.register("dig_site",
            () -> new DeepTimeFeatures.DigSite(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, DeepTimeFeatures.GreenhouseRuin> GREENHOUSE_RUIN = FEATURES.register("greenhouse_ruin",
            () -> new DeepTimeFeatures.GreenhouseRuin(NoneFeatureConfiguration.CODEC));

    static {
        TAB.add(ITEMS.register("mammoth_capsule", () -> new RevivedMobs.CapsuleItem(MAMMOTH, new Item.Properties())));
        TAB.add(ITEMS.register("meganeura_capsule", () -> new RevivedMobs.CapsuleItem(MEGANEURA, new Item.Properties())));
        TAB.add(ITEMS.register("mammoth_spawn_egg", () -> new DeferredSpawnEggItem(MAMMOTH, 0x5A3C24, 0xE8DCC0, new Item.Properties())));
        TAB.add(ITEMS.register("meganeura_spawn_egg", () -> new DeferredSpawnEggItem(MEGANEURA, 0x2E5A3A, 0x6AC8D8, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(MAMMOTH.get(), RevivedMobs.Mammoth.createAttributes().build());
            e.put(MEGANEURA.get(), RevivedMobs.Meganeura.createAttributes().build());
        });
    }

    private DeepTimeRegistry() {}
}
