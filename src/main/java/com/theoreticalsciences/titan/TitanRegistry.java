package com.theoreticalsciences.titan;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
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
import java.util.Set;

/** 1.2.0: Titan's creatures and the Ouroboros (see {@link TitanMobs}, {@link OuroborosBoss}). */
public final class TitanRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    /** Shown in the mod's creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();
    static final Set<String> IDS = Set.of("tholin_drifter", "catalyst_sprite", "methane_skater", "cryolava_slug", "ouroboros");

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String id, EntityType.EntityFactory<T> f, float w, float h, int bg, int fg) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(id, () ->
                EntityType.Builder.of(f, MobCategory.MONSTER).sized(w, h).eyeHeight(h * 0.6F).clientTrackingRange(10).build(id));
        TAB.add(ITEMS.register(id + "_spawn_egg", () -> new DeferredSpawnEggItem(holder, bg, fg, new Item.Properties())));
        return holder;
    }

    public static final DeferredHolder<EntityType<?>, EntityType<TitanMobs.TholinDrifter>> THOLIN_DRIFTER =
            mob("tholin_drifter", TitanMobs.TholinDrifter::new, 0.9F, 0.7F, 0x5A3418, 0xE09040);
    public static final DeferredHolder<EntityType<?>, EntityType<TitanMobs.CatalystSprite>> CATALYST_SPRITE =
            mob("catalyst_sprite", TitanMobs.CatalystSprite::new, 0.9F, 0.9F, 0x808890, 0x80FFC0);
    public static final DeferredHolder<EntityType<?>, EntityType<TitanMobs.MethaneSkater>> METHANE_SKATER =
            mob("methane_skater", TitanMobs.MethaneSkater::new, 1.3F, 0.6F, 0x3A2A18, 0xA08050);
    public static final DeferredHolder<EntityType<?>, EntityType<TitanMobs.CryolavaSlug>> CRYOLAVA_SLUG =
            mob("cryolava_slug", TitanMobs.CryolavaSlug::new, 1.1F, 0.7F, 0x708088, 0xC0E8F0);
    public static final DeferredHolder<EntityType<?>, EntityType<OuroborosBoss.Serpent>> OUROBOROS =
            mob("ouroboros", OuroborosBoss.Serpent::new, 2.4F, 1.6F, 0x603818, 0xFFD060);

    /** Whether this is one of Titan's own creatures (the Catalyst Sprite's healing helps only them). */
    public static boolean isTitanCreature(Entity e) {
        var k = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return k.getNamespace().equals(TheoreticalSciences.MODID) && IDS.contains(k.getPath());
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(THOLIN_DRIFTER.get(), TitanMobs.TholinDrifter.createAttributes().build());
            e.put(CATALYST_SPRITE.get(), TitanMobs.CatalystSprite.createAttributes().build());
            e.put(METHANE_SKATER.get(), TitanMobs.MethaneSkater.createAttributes().build());
            e.put(CRYOLAVA_SLUG.get(), TitanMobs.CryolavaSlug.createAttributes().build());
            e.put(OUROBOROS.get(), OuroborosBoss.Serpent.createAttributes().build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent e) -> {
            var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
            // any light (Titan is dim by day and night alike), on any ground - or, for the floaters and the skater, over a lake
            e.register(THOLIN_DRIFTER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, TitanRegistry::anywhere, op);
            e.register(CATALYST_SPRITE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, TitanRegistry::anywhere, op);
            e.register(CRYOLAVA_SLUG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TitanRegistry::onGround, op);
            e.register(METHANE_SKATER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING,
                    (type, level, reason, pos, rnd) -> anywhere(type, level, reason, pos, rnd)
                            && (TitanMobs.MethaneSkater.methane(level.getFluidState(pos.below())) || level.getBlockState(pos.below()).isSolid()), op);
        });
    }

    static <T extends Entity> boolean anywhere(EntityType<T> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason,
                                               BlockPos pos, net.minecraft.util.RandomSource rnd) {
        // never inside a lake or the sea: in open air, with air above it too
        return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.above()).isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    static <T extends Entity> boolean onGround(EntityType<T> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason,
                                               BlockPos pos, net.minecraft.util.RandomSource rnd) {
        return anywhere(type, level, reason, pos, rnd) && level.getBlockState(pos.below()).isSolid();
    }

    private TitanRegistry() {}
}
