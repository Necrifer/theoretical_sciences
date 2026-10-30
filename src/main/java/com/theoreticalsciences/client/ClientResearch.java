package com.theoreticalsciences.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.research.ResearchDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Client copy of research definitions and the local player's progress. */
public final class ClientResearch {
    public static Map<ResourceLocation, ResearchDefinition> defs = Map.of();
    public static Set<ResourceLocation> unlocked = Set.of();
    public static int insight;
    public static int observations;

    static void setDefs(String json) {
        Map<ResourceLocation, ResearchDefinition> map = new LinkedHashMap<>();
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id == null) continue;
                ResearchDefinition.CODEC.parse(JsonOps.INSTANCE, e.getValue()).result().ifPresent(d -> map.put(id, d));
            }
        } catch (Exception ex) {
            TheoreticalSciences.LOGGER.error("Failed to read research definitions from server", ex);
        }
        defs = Collections.unmodifiableMap(map);
    }

    static void setState(List<ResourceLocation> list, int ins, int obs) {
        unlocked = Set.copyOf(list);
        insight = ins;
        observations = obs;
    }

    /** The entry being studied, its total length and when (client millis) it will be done. */
    public static ResourceLocation active;
    public static int activeTotal;
    private static long activeEndMillis;

    static void setActive(String id, int total, int remaining) {
        active = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        activeTotal = total;
        activeEndMillis = System.currentTimeMillis() + remaining * 1000L;
    }

    public static int activeRemaining() {
        return active == null ? 0 : (int) Math.max(0, (activeEndMillis - System.currentTimeMillis()) / 1000);
    }

    /** Puzzle progress: id -> letters (d derived, s solved, v/m/f/e hints bought). */
    public static Map<ResourceLocation, String> puzzles = Map.of();

    /** Whether the server requires derivations (its config, not ours). */
    public static boolean puzzlesEnabled = true;
    /** This world's puzzle numbers (see ResearchService.puzzleSalt). */
    public static long salt;

    /** The given values of a puzzle in this world. */
    public static Map<String, Double> given(ResourceLocation id, com.theoreticalsciences.research.ResearchPuzzle p) {
        Object[] c = GIVEN.get(id);
        if (c != null && c[0] == p && (Long) c[1] == salt) return (Map<String, Double>) c[2];
        Map<String, Double> m = p.given(salt, id.toString());
        GIVEN.put(id, new Object[]{p, salt, m});
        return m;
    }

    private static final Map<ResourceLocation, Object[]> GIVEN = new HashMap<>();

    public static boolean needsProof(ResourceLocation id) {
        ResearchDefinition d = defs.get(id);
        return puzzlesEnabled && d != null && d.puzzle().isPresent() && !has(id, 's');
    }

    /** Research symbols this player has learned. */
    public static Set<String> glyphs = Set.of();

    public static boolean knows(String glyph) {
        return glyphs.contains(glyph) || !com.theoreticalsciences.research.GlyphRegistry.client().containsKey(glyph)
                || (net.minecraft.client.Minecraft.getInstance().player != null && net.minecraft.client.Minecraft.getInstance().player.isCreative());
    }

    public static void setPuzzles(net.minecraft.nbt.CompoundTag t) {
        Map<ResourceLocation, String> m = new HashMap<>();
        Set<String> gl = new java.util.HashSet<>();
        for (net.minecraft.nbt.Tag x : t.getList("#glyphs", net.minecraft.nbt.Tag.TAG_STRING)) gl.add(x.getAsString());
        glyphs = gl;
        puzzlesEnabled = !t.contains("#enabled") || t.getBoolean("#enabled");
        salt = t.getLong("#salt");
        for (String k : t.getAllKeys()) {
            if (k.startsWith("#")) continue;
            ResourceLocation r = ResourceLocation.tryParse(k);
            if (r != null) m.put(r, t.getString(k));
        }
        puzzles = m;
    }

    public static boolean has(ResourceLocation id, char c) {
        return puzzles.getOrDefault(id, "").indexOf(c) >= 0;
    }

    /** Hidden entries stay off the map until every prerequisite is known (or they are researched). */
    public static boolean visible(ResourceLocation id) {
        ResearchDefinition d = defs.get(id);
        if (d == null) return false;
        if (unlocked.contains(id)) return true;
        if (d.grantOnly()) return false;
        if (!d.hidden()) return true;
        for (ResourceLocation r : d.requires()) if (!unlocked.contains(r)) return false;
        return true;
    }

    public static boolean available(ResourceLocation id) {
        ResearchDefinition d = defs.get(id);
        if (d == null || unlocked.contains(id) || d.grantOnly() || id.equals(active)) return false;
        for (ResourceLocation r : d.requires()) if (!unlocked.contains(r)) return false;
        return true;
    }

    private ClientResearch() {}
}
