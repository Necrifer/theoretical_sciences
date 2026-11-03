package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** 1.3.7: the Planetary Interior's creatures and its three suits of armour. */
public final class InteriorRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    /** Shown in the mod's creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<InteriorMobs.MagmaCrawler>> MAGMA_CRAWLER = ENTITIES.register("magma_crawler",
            () -> EntityType.Builder.<InteriorMobs.MagmaCrawler>of(InteriorMobs.MagmaCrawler::new, MobCategory.MONSTER).sized(1.1F, 0.6F).eyeHeight(0.45F)
                    .fireImmune().clientTrackingRange(10).build("magma_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<InteriorMobs.PlumeElemental>> PLUME_ELEMENTAL = ENTITIES.register("plume_elemental",
            () -> EntityType.Builder.<InteriorMobs.PlumeElemental>of(InteriorMobs.PlumeElemental::new, MobCategory.MONSTER).sized(1.0F, 2.2F).eyeHeight(1.6F)
                    .fireImmune().clientTrackingRange(10).build("plume_elemental"));
    public static final DeferredHolder<EntityType<?>, EntityType<InteriorMobs.FissionHusk>> FISSION_HUSK = ENTITIES.register("fission_husk",
            () -> EntityType.Builder.<InteriorMobs.FissionHusk>of(InteriorMobs.FissionHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).build("fission_husk"));
    public static final DeferredHolder<EntityType<?>, EntityType<InteriorMobs.GeodynamoWraith>> GEODYNAMO_WRAITH = ENTITIES.register("geodynamo_wraith",
            () -> EntityType.Builder.<InteriorMobs.GeodynamoWraith>of(InteriorMobs.GeodynamoWraith::new, MobCategory.MONSTER).sized(1.2F, 1.2F).eyeHeight(0.6F)
                    .fireImmune().clientTrackingRange(10).build("geodynamo_wraith"));

    public static final DeferredHolder<EntityType<?>, EntityType<InteriorMobs.RandomWalker>> RANDOM_WALKER = ENTITIES.register("random_walker",
            () -> EntityType.Builder.<InteriorMobs.RandomWalker>of(InteriorMobs.RandomWalker::new, MobCategory.MONSTER).sized(0.9F, 0.9F).eyeHeight(0.45F)
                    .clientTrackingRange(8).build("random_walker"));

    static {
        egg("random_walker", RANDOM_WALKER, 0x303034, 0xE0E0E0);
        egg("magma_crawler", MAGMA_CRAWLER, 0x2A1C14, 0xFF7020);
        egg("plume_elemental", PLUME_ELEMENTAL, 0x401808, 0xFFB040);
        egg("fission_husk", FISSION_HUSK, 0x2A3020, 0xA0FF40);
        egg("geodynamo_wraith", GEODYNAMO_WRAITH, 0x30343C, 0x60C0FF);
        for (InteriorGear.Suit s : InteriorGear.Suit.values())
            for (ArmorItem.Type t : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS})
                TAB.add(ITEMS.register(s.id + "_" + InteriorGear.piece(t), () -> new InteriorGear.SuitItem(s, t)));
    }

    private static void egg(String id, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Mob>> t, int bg, int fg) {
        TAB.add(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(t, bg, fg, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        InteriorGear.MATERIALS.register(bus);
        RadiationPlating.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(MAGMA_CRAWLER.get(), InteriorMobs.MagmaCrawler.createAttributes().build());
            e.put(PLUME_ELEMENTAL.get(), InteriorMobs.PlumeElemental.createAttributes().build());
            e.put(FISSION_HUSK.get(), InteriorMobs.FissionHusk.createAttributes().build());
            e.put(GEODYNAMO_WRAITH.get(), InteriorMobs.GeodynamoWraith.createAttributes().build());
            e.put(RANDOM_WALKER.get(), InteriorMobs.RandomWalker.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            var hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            e.register(MAGMA_CRAWLER.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(FISSION_HUSK.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(PLUME_ELEMENTAL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(GEODYNAMO_WRAITH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(RANDOM_WALKER.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkMonsterSpawnRules, op);   // (in the dark only)
        });
    }

    private InteriorRegistry() {}
}
