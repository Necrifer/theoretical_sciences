package com.theoreticalsciences.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.transcend.CoreForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Core Forge recipes from datapacks / KubeJS: {@code data/<any namespace>/ts_forge/<name>.json}.
 * <pre>
 * {
 *   "result": {"item": "minecraft:dragon_egg", "count": 1},
 *   "ingredients": [
 *     {"item": "theoreticalsciences:omnipotent_ingot", "count": 16},
 *     {"item": "theoreticalsciences:rekindled_spark", "count": 0},        // 0 = must be carried, but is not used up
 *     {"universe": "mekanism", "count": 1}                           // a Simulated Universe of that mod
 *   ]
 * }
 * </pre>
 * The recipe's id is the file's id ({@code mypack:ts_forge/dragon_egg.json} = {@code mypack:dragon_egg}). A file named
 * like one of the mod's own recipes in the {@code theoreticalsciences} namespace replaces it (e.g.
 * {@code theoreticalsciences:ts_forge/omnipotent_ingot.json}). To delete recipes, any file can say
 * {@code {"remove": ["omnipotent_ingot", "theoreticalsciences:echo_lattice", "creative/mekanism:creative_bin"]}};
 * {@code {"remove": true}} deletes the recipe with the file's own id.
 * With no files, the Forge is exactly as built. Reloaded by /reload and sent to every client.
 */
public final class ForgeRecipes extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final ForgeRecipes INSTANCE = new ForgeRecipes();
    private static final int MAX_INGREDIENTS = 32;

    /** What the files say: recipes replacing built-in ones (by short id), new recipes, and ids to delete. */
    public record Data(Map<String, CoreForge.Recipe> overrides, List<CoreForge.Recipe> added, Set<String> removed, String syncJson) {
        public boolean empty() {
            return overrides.isEmpty() && added.isEmpty() && removed.isEmpty();
        }

        /** Whether a recipe id was deleted ("omnipotent_ingot" and "theoreticalsciences:omnipotent_ingot" are the same). */
        public boolean removed(String id) {
            if (removed.isEmpty()) return false;
            if (removed.contains(id)) return true;
            String pre = TheoreticalSciences.MODID + ":";
            return id.startsWith(pre) ? removed.contains(id.substring(pre.length())) : removed.contains(pre + id);
        }
    }

    private static volatile Data data = new Data(Map.of(), List.of(), Set.of(), "{}");

    private ForgeRecipes() {
        super(GSON, "ts_forge");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject all = new JsonObject();
        new TreeMap<>(map).forEach((id, json) -> all.add(id.toString(), json));
        load(all);
    }

    public static void load(JsonObject all) {
        Set<String> builtIn = new HashSet<>();
        for (CoreForge.Recipe r : CoreForge.RECIPES) builtIn.add(r.id());
        Map<String, CoreForge.Recipe> over = new LinkedHashMap<>();
        List<CoreForge.Recipe> added = new ArrayList<>();
        Set<String> removed = new HashSet<>();
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            String fileId = e.getKey();
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("remove")) {
                    JsonElement rem = o.get("remove");
                    if (rem.isJsonPrimitive() && rem.getAsJsonPrimitive().isBoolean()) {
                        if (rem.getAsBoolean()) {
                            removed.add(fileId);
                            // "mypack:ts_forge/omnipotent_ingot.json" with remove: true means the mod's own one too
                            String path = ResourceLocation.parse(fileId).getPath();
                            if (builtIn.contains(path)) removed.add(path);
                        }
                    } else if (rem.isJsonArray()) {
                        for (JsonElement x : rem.getAsJsonArray()) removed.add(x.getAsString().trim());
                    } else removed.add(rem.getAsString().trim());
                    if (!o.has("result")) continue;              // only removing
                }
                CoreForge.Recipe r = parse(fileId, o, builtIn);
                if (r == null) continue;
                if (builtIn.contains(r.id())) over.put(r.id(), r);
                else added.add(r);
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad Core Forge recipe {}: {}", fileId, ex.getMessage());
            }
        }
        data = new Data(Map.copyOf(over), List.copyOf(added), Set.copyOf(removed), all.toString());
        if (!data.empty())
            TheoreticalSciences.LOGGER.info("Core Forge: {} recipes replaced, {} added, {} ids removed by datapacks", over.size(), added.size(), removed.size());
        CoreForge.invalidate();
        PackData.changed();
    }

    @Nullable
    private static CoreForge.Recipe parse(String fileId, JsonObject o, Set<String> builtIn) {
        ResourceLocation rl = ResourceLocation.parse(fileId);
        // the mod's own recipes keep their short ids (the Forge's puzzle is worked out from the id)
        String id = rl.getNamespace().equals(TheoreticalSciences.MODID) && builtIn.contains(rl.getPath()) ? rl.getPath() : fileId;
        JsonObject res = o.getAsJsonObject("result");
        String item = res.has("item") ? res.get("item").getAsString() : res.get("id").getAsString();
        if (!exists(item)) {
            TheoreticalSciences.LOGGER.warn("Core Forge recipe {}: result {} does not exist (skipped)", fileId, item);
            return null;
        }
        int count = res.has("count") ? Math.max(1, Math.min(64, res.get("count").getAsInt())) : 1;
        String outUniverse = res.has("universe") ? res.get("universe").getAsString() : null;
        if (outUniverse != null && !outUniverse.equals("minecraft") && !net.neoforged.fml.ModList.get().isLoaded(outUniverse)) {
            TheoreticalSciences.LOGGER.warn("Core Forge recipe {}: makes a Simulated Universe of {}, which is not installed (skipped)", fileId, outUniverse);
            return null;
        }
        CoreForge.Ing out = new CoreForge.Ing(item, count, outUniverse);
        List<CoreForge.Ing> in = new ArrayList<>();
        for (JsonElement x : o.getAsJsonArray("ingredients")) {
            JsonObject g = x.getAsJsonObject();
            String uni = g.has("universe") ? g.get("universe").getAsString() : null;
            String it = g.has("item") ? g.get("item").getAsString() : uni != null ? TheoreticalSciences.MODID + ":simulated_universe" : null;
            if (it == null) throw new IllegalArgumentException("an ingredient needs \"item\" (or \"universe\")");
            if (!exists(it)) {
                TheoreticalSciences.LOGGER.warn("Core Forge recipe {}: ingredient {} does not exist (recipe skipped)", fileId, it);
                return null;
            }
            // round 35: a Simulated Universe of a mod that is not installed can never be made: skip, like a missing item
            if (uni != null && !uni.equals("minecraft") && !net.neoforged.fml.ModList.get().isLoaded(uni)) {
                TheoreticalSciences.LOGGER.warn("Core Forge recipe {}: needs a Simulated Universe of {}, which is not installed (recipe skipped)", fileId, uni);
                return null;
            }
            int n = g.has("count") ? g.get("count").getAsInt() : 1;
            if (n < 0 || n > 1_000_000) throw new IllegalArgumentException("count must be 0..1000000");
            in.add(new CoreForge.Ing(it, n, uni));
        }
        if (in.isEmpty() || in.size() > MAX_INGREDIENTS) throw new IllegalArgumentException("needs 1 to " + MAX_INGREDIENTS + " ingredients");
        return new CoreForge.Recipe(id, out, List.copyOf(in));
    }

    private static boolean exists(String item) {
        ResourceLocation r = ResourceLocation.tryParse(item.contains(":") ? item : TheoreticalSciences.MODID + ":" + item);
        return r != null && BuiltInRegistries.ITEM.containsKey(r) && BuiltInRegistries.ITEM.get(r) != Items.AIR;
    }

    public static Data data() {
        return data;
    }

    public static String syncJson() {
        return data.syncJson();
    }

    public static void clear() {
        data = new Data(Map.of(), List.of(), Set.of(), "{}");
        CoreForge.invalidate();
        PackData.changed();
    }
}
