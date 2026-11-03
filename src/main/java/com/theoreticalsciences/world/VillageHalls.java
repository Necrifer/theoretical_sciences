package com.theoreticalsciences.world;

import com.mojang.datafixers.util.Pair;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Round 39 (1.2.6): adds the Probability Hall to the house pools of the five vanilla village types, at server start.
 * Vanilla's pools cannot be extended by data without replacing them (which would undo every other mod's houses), so
 * the hall is appended to the pool objects themselves: the weighted list the game reads, and the raw list it saves.
 * A village only rarely gets one (weight 1 against the dozens of vanilla houses).
 * 1.1.0: this is the only place that adds it (frontier/VillagePools did the same job with the 1.10.0 hall, so villages
 * had two halls at double the chance); it now builds that newer hall.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class VillageHalls {
    private static final String[] TYPES = {"plains", "desert", "savanna", "snowy", "taiga"};
    private static final int WEIGHT = 1;

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent e) {
        Registry<StructureTemplatePool> pools = e.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        StructurePoolElement hall = StructurePoolElement.legacy(TheoreticalSciences.MODID + ":probability_hall")
                .apply(StructureTemplatePool.Projection.RIGID);
        for (String t : TYPES) {
            StructureTemplatePool pool = pools.get(ResourceLocation.withDefaultNamespace("village/" + t + "/houses"));
            if (pool == null) continue;
            try {
                add(pool, hall);
            } catch (Throwable ex) {
                TheoreticalSciences.LOGGER.warn("Theoretical Sciences: could not add the Probability Hall to {} villages ({})", t, ex.toString());
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void add(StructureTemplatePool pool, StructurePoolElement el) throws ReflectiveOperationException {
        Field templates = null, raw = null;
        for (Field f : StructureTemplatePool.class.getDeclaredFields()) {
            if (f.getType() == List.class && f.getName().equals("rawTemplates")) raw = f;
            if (f.getName().equals("templates")) templates = f;
        }
        if (templates == null || raw == null) throw new NoSuchFieldException("templates / rawTemplates");
        templates.setAccessible(true);
        raw.setAccessible(true);
        List<StructurePoolElement> list = (List<StructurePoolElement>) templates.get(pool);
        for (StructurePoolElement x : list) if (x.toString().contains("probability_hall")) return;     // (once per pool)
        for (int i = 0; i < WEIGHT; i++) list.add(el);
        List<Pair<StructurePoolElement, Integer>> r = new ArrayList<>((List<Pair<StructurePoolElement, Integer>>) raw.get(pool));
        r.add(Pair.of(el, WEIGHT));
        raw.set(pool, r);
    }

    private VillageHalls() {}
}
