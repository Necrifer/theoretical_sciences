package com.theoreticalsciences.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side registry of research entries (datapack driven). */
public class ResearchManager extends SimpleJsonResourceReloadListener {
    // GSON must be initialised before INSTANCE (static initialisers run in source order).
    private static final Gson GSON = new GsonBuilder().create();
    public static final ResearchManager INSTANCE = new ResearchManager();

    private Map<ResourceLocation, ResearchDefinition> entries = Map.of();
    private String syncJson = "{}";

    private ResearchManager() {
        super(GSON, "ts_research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, ResearchDefinition> out = new LinkedHashMap<>();
        JsonObject sync = new JsonObject();
        map.forEach((id, json) -> ResearchDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> TheoreticalSciences.LOGGER.error("Bad research entry {}: {}", id, err))
                .ifPresent(def -> {
                    out.put(id, def);
                    sync.add(id.toString(), json);
                }));
        entries = Collections.unmodifiableMap(out);
        syncJson = GSON.toJson(sync);
        TheoreticalSciences.LOGGER.info("Loaded {} Theoretical Sciences research entries", out.size());
    }

    public Map<ResourceLocation, ResearchDefinition> entries() {
        return entries;
    }

    public ResearchDefinition get(ResourceLocation id) {
        return entries.get(id);
    }

    public String syncJson() {
        return syncJson;
    }
}
