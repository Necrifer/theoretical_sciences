package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
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
import java.util.function.Supplier;

/**
 * 1.10.0: the Accretion Frontier's creatures (and the Black Swan, which can turn up anywhere), plus The Binary (1.12.0).
 * Each acts out a piece of physics and thinks for itself (no vanilla mob's mind).
 */
public final class FrontierRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    /** Shown in the mod's creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, MobCategory cat,
                                                                                    float w, float h, boolean fireImmune, int bg, int fg) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(id, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(f, cat).sized(w, h).eyeHeight(h * 0.6F).clientTrackingRange(10);
            if (fireImmune) b = b.fireImmune();
            return b.build(id);
        });
        TAB.add(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(holder, bg, fg, new Item.Properties())));
        return holder;
    }

    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.Nebuloid>> NEBULOID =
            mob("nebuloid", FrontierMobs.Nebuloid::new, MobCategory.MONSTER, 1.4F, 1.2F, true, 0x302848, 0xC090FF);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.PulsarWisp>> PULSAR_WISP =
            mob("pulsar_wisp", FrontierMobs.PulsarWisp::new, MobCategory.MONSTER, 0.8F, 0.8F, true, 0x182040, 0x80E0FF);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.TidalStretcher>> TIDAL_STRETCHER =
            mob("tidal_stretcher", FrontierMobs.TidalStretcher::new, MobCategory.MONSTER, 0.9F, 2.6F, true, 0x401808, 0xFF9040);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.LensPhantom>> LENS_PHANTOM =
            mob("lens_phantom", FrontierMobs.LensPhantom::new, MobCategory.MONSTER, 0.7F, 1.9F, true, 0x101020, 0xE0E0FF);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.HawkingImp>> HAWKING_IMP =
            mob("hawking_imp", FrontierMobs.HawkingImp::new, MobCategory.MONSTER, 0.6F, 0.6F, true, 0x000000, 0xFFE0A0);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.BoltzmannBrain>> BOLTZMANN_BRAIN =
            mob("boltzmann_brain", FrontierMobs.BoltzmannBrain::new, MobCategory.MONSTER, 0.8F, 0.8F, true, 0xC08090, 0xFFE0F0);
    public static final DeferredHolder<EntityType<?>, EntityType<FrontierMobs.BlackSwan>> BLACK_SWAN =
            mob("black_swan", FrontierMobs.BlackSwan::new, MobCategory.CREATURE, 0.8F, 1.2F, false, 0x101014, 0xC02020);
    public static final DeferredHolder<EntityType<?>, EntityType<BinaryBoss.Serpent>> BINARY_SERPENT =
            mob("binary_serpent", BinaryBoss.Serpent::new, MobCategory.MONSTER, 1.2F, 1.2F, true, 0x102040, 0xFFD060);

    /** 1.3.0: the Jet Stream's wake-ribbon (spawned by JetStream's top-up only, never by the biomes). */
    public static final DeferredHolder<EntityType<?>, EntityType<KarmanWraith>> KARMAN_WRAITH =
            mob("karman_wraith", KarmanWraith::new, MobCategory.MONSTER, 1.0F, 0.8F, true, 0xD8E4F0, 0x6090C0);

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(NEBULOID.get(), FrontierMobs.Nebuloid.createAttributes().build());
            e.put(PULSAR_WISP.get(), FrontierMobs.PulsarWisp.createAttributes().build());
            e.put(TIDAL_STRETCHER.get(), FrontierMobs.TidalStretcher.createAttributes().build());
            e.put(LENS_PHANTOM.get(), FrontierMobs.LensPhantom.createAttributes().build());
            e.put(HAWKING_IMP.get(), FrontierMobs.HawkingImp.createAttributes().build());
            e.put(BOLTZMANN_BRAIN.get(), FrontierMobs.BoltzmannBrain.createAttributes().build());
            e.put(BLACK_SWAN.get(), FrontierMobs.BlackSwan.createAttributes().build());
            e.put(BINARY_SERPENT.get(), BinaryBoss.Serpent.createAttributes().build());
            e.put(KARMAN_WRAITH.get(), KarmanWraith.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            var hm = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
            for (Supplier<? extends EntityType<? extends Monster>> t : List.<Supplier<? extends EntityType<? extends Monster>>>of(
                    NEBULOID, PULSAR_WISP, LENS_PHANTOM, HAWKING_IMP, BOLTZMANN_BRAIN, KARMAN_WRAITH))
                e.register(t.get(), SpawnPlacementTypes.NO_RESTRICTIONS, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            e.register(TIDAL_STRETCHER.get(), SpawnPlacementTypes.ON_GROUND, hm, Monster::checkAnyLightMonsterSpawnRules, op);
            // the Black Swan never spawns naturally: FrontierHazards brings it, when nothing improbable has happened for long enough
        });
    }

    private FrontierRegistry() {}
}
