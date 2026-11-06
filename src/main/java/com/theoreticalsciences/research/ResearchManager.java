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
    /** 1.2.0: each entry's own JSON, and which are secret (grant only): a client is sent those only once it has them. */
    private Map<ResourceLocation, JsonElement> jsons = Map.of();
    private java.util.Set<ResourceLocation> secret = java.util.Set.of();

    private ResearchManager() {
        super(GSON, "ts_research");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, ResearchDefinition> out = new LinkedHashMap<>();
        JsonObject sync = new JsonObject();
        Map<ResourceLocation, JsonElement> js = new LinkedHashMap<>();
        java.util.Set<ResourceLocation> sec = new java.util.HashSet<>();
        map.forEach((id, json) -> ResearchDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> TheoreticalSciences.LOGGER.error("Bad research entry {}: {}", id, err))
                .ifPresent(def -> {
                    out.put(id, def);
                    js.put(id, json);
                    if (def.grantOnly()) sec.add(id);
                    else sync.add(id.toString(), json);
                }));
        entries = Collections.unmodifiableMap(out);
        jsons = Collections.unmodifiableMap(js);
        secret = Collections.unmodifiableSet(sec);
        syncJson = GSON.toJson(sync);
        TheoreticalSciences.LOGGER.info("Loaded {} Theoretical Sciences research entries", out.size());
    }

    public Map<ResourceLocation, ResearchDefinition> entries() {
        return entries;
    }

    public ResearchDefinition get(ResourceLocation id) {
        return entries.get(id);
    }

    /** Every entry but the secret ones (as sent to a client that has none of them). */
    public String syncJson() {
        return syncJson;
    }

    public java.util.Set<ResourceLocation> secret() {
        return secret;
    }

    /** What one player's client is sent: every open entry, and the secret ones it has been granted. */
    public String syncJsonFor(java.util.Set<ResourceLocation> unlocked) {
        boolean any = false;
        for (ResourceLocation id : secret) if (unlocked.contains(id)) { any = true; break; }
        if (!any) return syncJson;
        JsonObject o = new JsonObject();
        for (Map.Entry<ResourceLocation, JsonElement> e : jsons.entrySet())
            if (!secret.contains(e.getKey()) || unlocked.contains(e.getKey())) o.add(e.getKey().toString(), e.getValue());
        return GSON.toJson(o);
    }
}
