package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Creatures and events born from containment failures. */
public final class QHEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> SPAWN_EGGS = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<DestabilisedHusk>> DESTABILISED_HUSK = ENTITIES.register("destabilised_husk",
            () -> EntityType.Builder.<DestabilisedHusk>of(DestabilisedHusk::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).build("destabilised_husk"));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidWraith>> VOID_WRAITH = ENTITIES.register("void_wraith",
            () -> EntityType.Builder.<VoidWraith>of(VoidWraith::new, MobCategory.MONSTER).sized(0.5F, 1.0F).fireImmune()
                    .clientTrackingRange(8).build("void_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<NaniteSwarmEntity>> NANITE_SWARM = ENTITIES.register("nanite_swarm",
            () -> EntityType.Builder.<NaniteSwarmEntity>of(NaniteSwarmEntity::new, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F)
                    .clientTrackingRange(10).build("nanite_swarm"));
    public static final DeferredHolder<EntityType<?>, EntityType<ParadoxEcho>> PARADOX_ECHO = ENTITIES.register("paradox_echo",
            () -> EntityType.Builder.<ParadoxEcho>of(ParadoxEcho::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.74F)
                    .clientTrackingRange(8).build("paradox_echo"));
    public static final DeferredHolder<EntityType<?>, EntityType<LawWraith>> LAW_WRAITH = ENTITIES.register("law_wraith",
            () -> EntityType.Builder.<LawWraith>of(LawWraith::new, MobCategory.MONSTER).sized(0.6F, 1.8F).fireImmune()
                    .clientTrackingRange(8).build("law_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<GravitonWisp>> GRAVITON_WISP = ENTITIES.register("graviton_wisp",
            () -> EntityType.Builder.<GravitonWisp>of(GravitonWisp::new, MobCategory.MONSTER).sized(0.8F, 0.8F).fireImmune()
                    .clientTrackingRange(10).build("graviton_wisp"));
    public static final DeferredHolder<EntityType<?>, EntityType<BraneStrider>> BRANE_STRIDER = ENTITIES.register("brane_strider",
            () -> EntityType.Builder.<BraneStrider>of(BraneStrider::new, MobCategory.MONSTER).sized(0.9F, 2.9F).eyeHeight(2.6F)
                    .clientTrackingRange(10).build("brane_strider"));
    public static final DeferredHolder<EntityType<?>, EntityType<ModuliCrawler>> MODULI_CRAWLER = ENTITIES.register("moduli_crawler",
            () -> EntityType.Builder.<ModuliCrawler>of(ModuliCrawler::new, MobCategory.MONSTER).sized(0.7F, 0.45F).eyeHeight(0.3F)
                    .clientTrackingRange(8).build("moduli_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<ContainmentRupture>> CONTAINMENT_RUPTURE = ENTITIES.register("containment_rupture",
            () -> EntityType.Builder.<ContainmentRupture>of(ContainmentRupture::new, MobCategory.MISC).sized(1.0F, 1.0F).fireImmune()
                    .clientTrackingRange(16).updateInterval(2).build("containment_rupture"));

    static {
        egg("destabilised_husk", DESTABILISED_HUSK::get, 0x2A3A20, 0x7CFF40);
        egg("void_wraith", VOID_WRAITH::get, 0x0C0418, 0xB04DFF);
        egg("nanite_swarm", NANITE_SWARM::get, 0x40484F, 0x40FFE0);
        egg("paradox_echo", PARADOX_ECHO::get, 0x3A2A10, 0xFFC23D);
        egg("law_wraith", LAW_WRAITH::get, 0x1A0838, 0x80FFE0);
        egg("graviton_wisp", GRAVITON_WISP::get, 0x101030, 0xA0B0FF);
        egg("brane_strider", BRANE_STRIDER::get, 0x301040, 0xE080FF);
        egg("moduli_crawler", MODULI_CRAWLER::get, 0x103030, 0x60FFD0);
    }

    private static void egg(String id, Supplier<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        SPAWN_EGGS.add(QHItems.ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(type, bg, fg, new Item.Properties())));
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(DESTABILISED_HUSK.get(), DestabilisedHusk.createAttributes().build());
        e.put(VOID_WRAITH.get(), VoidWraith.createAttributes().build());
        e.put(NANITE_SWARM.get(), NaniteSwarmEntity.createAttributes().build());
        e.put(PARADOX_ECHO.get(), ParadoxEcho.createAttributes().build());
        e.put(LAW_WRAITH.get(), LawWraith.createAttributes().build());
        e.put(GRAVITON_WISP.get(), GravitonWisp.createAttributes().build());
        e.put(BRANE_STRIDER.get(), BraneStrider.createAttributes().build());
        e.put(MODULI_CRAWLER.get(), ModuliCrawler.createAttributes().build());
    }

    /** Natural spawning rules for the Bulk's native creatures (darkness is not required: the Bulk is always dark). */
    public static void spawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e) {
        var ground = net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND;
        var hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        var op = net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE;
        e.register(GRAVITON_WISP.get(), ground, hm, Monster::checkAnyLightMonsterSpawnRules, op);
        e.register(BRANE_STRIDER.get(), ground, hm, Monster::checkAnyLightMonsterSpawnRules, op);
        e.register(MODULI_CRAWLER.get(), ground, hm, Monster::checkAnyLightMonsterSpawnRules, op);
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(QHEntities::attributes);
        bus.addListener(QHEntities::spawnPlacements);
    }

    private QHEntities() {}
}
