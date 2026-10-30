package com.theoreticalsciences.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The glossary of research symbols (data/&lt;ns&gt;/ts_glyphs/*.json): every tile a derivation can use - variables,
 * constants, operators, ∫, ∂, ∇, bra-kets... - with what it means and how a player learns it (from the start, by
 * holding an item, or by completing a research). Synced to clients for the glossary book and the puzzle tray.
 */
public class GlyphRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final GlyphRegistry INSTANCE = new GlyphRegistry();

    public enum Unlock {START, ITEM, RESEARCH}

    public record Glyph(String token, String name, String cat, String meaning, String unit, Unlock unlock, String unlockId) {}

    private static Map<String, Glyph> server = Map.of(), client = Map.of();
    private String syncJson = "{}";

    private GlyphRegistry() {
        super(GSON, "ts_glyphs");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject all = new JsonObject();
        map.forEach((id, json) -> {
            if (!json.isJsonObject()) return;
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet())
                if (!all.has(e.getKey())) all.add(e.getKey(), e.getValue());
        });
        syncJson = GSON.toJson(all);
        server = parse(all);
        TheoreticalSciences.LOGGER.info("Loaded {} research glyphs", server.size());
    }

    private static Map<String, Glyph> parse(JsonObject all) {
        Map<String, Glyph> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                JsonObject u = o.has("unlock") ? o.getAsJsonObject("unlock") : new JsonObject();
                Unlock kind = Unlock.START;
                String uid = "";
                if (u.has("item")) {
                    kind = Unlock.ITEM;
                    uid = u.get("item").getAsString();
                } else if (u.has("research")) {
                    kind = Unlock.RESEARCH;
                    uid = u.get("research").getAsString();
                }
                if (!uid.isEmpty() && !uid.contains(":")) uid = TheoreticalSciences.MODID + ":" + uid;
                out.put(e.getKey(), new Glyph(e.getKey(), str(o, "name"), str(o, "cat"), str(o, "meaning"), str(o, "unit"), kind, uid));
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad research glyph {}: {}", e.getKey(), ex.getMessage());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) ? o.get(k).getAsString() : "";
    }

    public String syncJson() {
        return syncJson;
    }

    public static void loadClient(String json) {
        try {
            client = parse(JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException e) {
            TheoreticalSciences.LOGGER.error("Bad glyph sync: {}", e.getMessage());
        }
    }

    public static Map<String, Glyph> server() {
        return server;
    }

    public static Map<String, Glyph> client() {
        return client;
    }

    @Nullable
    public static Glyph get(String token, boolean clientSide) {
        return (clientSide ? client : server).get(token);
    }
}
