package com.theoreticalsciences.weather;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.3.0: the Living Planet's weather creature, and where it comes from. The Storm Elemental never spawns by the world's
 * own spawning: only here, during a thunderstorm, near a player at sea over a warm ocean, at most one within 64 blocks,
 * checked once every 20 seconds per player.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class WeatherRegistry {
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheoreticalSciences.MODID);
    public static final List<DeferredHolder<Item, ? extends Item>> TAB = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<StormElemental>> STORM_ELEMENTAL = ENTITIES.register("storm_elemental",
            () -> EntityType.Builder.of(StormElemental::new, MobCategory.MONSTER).sized(2.2F, 1.8F).eyeHeight(1.0F).clientTrackingRange(12)
                    .build("storm_elemental"));

    static {
        TAB.add(ITEMS.register("storm_elemental_spawn_egg", () -> new DeferredSpawnEggItem(STORM_ELEMENTAL, 0x50585E, 0xF0F080, new Item.Properties())));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(STORM_ELEMENTAL.get(), StormElemental.createAttributes().build()));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || (p.tickCount + p.getId()) % 400 != 0) return;
        ServerLevel level = p.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !level.getLevelData().isThundering() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        var biome = level.getBiome(p.blockPosition());
        if (!biome.is(BiomeTags.IS_OCEAN) || biome.value().getBaseTemperature() < 0.5F) return;      // warm sea feeds the storm
        if (!level.getEntitiesOfClass(StormElemental.class, p.getBoundingBox().inflate(64)).isEmpty()) return;
        var r = p.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = 20 + r.nextDouble() * 20;
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        if (!level.isLoaded(new BlockPos(x, 64, z))) return;
        BlockPos at = new BlockPos(x, Math.max(level.getSeaLevel() + 14, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 10), z);
        if (!level.getBlockState(at).isAir()) return;
        StormElemental s = STORM_ELEMENTAL.get().create(level);
        if (s == null) return;
        s.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
        s.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
        level.addFreshEntity(s);
    }

    private WeatherRegistry() {}
}
