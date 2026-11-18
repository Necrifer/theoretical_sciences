package com.theoreticalsciences.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.home.HomeMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 1.3.1: Recreating Home from datapacks / KubeJS: {@code data/<any namespace>/ts_home/<name>.json}. Every field is
 * optional; files are read in id order and a later file changes only the fields it names.
 * <pre>
 * {
 *   "materials": ["theoreticalsciences:remembered_soil", ...],   // the whole checklist, in order (replaces it)
 *   "add_materials": ["mypack:star_heart"],                      // or: add to it ...
 *   "remove_materials": ["theoreticalsciences:quiet_snow"],      // ... and take out of it
 *   "target": 33550337,                                          // how many of each material
 *   "keystone": "theoreticalsciences:keystone_of_return",        // what the portal frame takes
 *   "unlock": "all_forge_recipes",                               // or "always", or a list of Core Forge recipe ids
 *   "unlock_skip": ["euclidean_anchor"]                          // with all_forge_recipes: these need not be forged
 * }
 * </pre>
 * With no files, Recreating Home is exactly as built. Reloaded by /reload and sent to every client.
 */
public final class HomeSettings extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final HomeSettings INSTANCE = new HomeSettings();
    /** The checklist screen and the saved data stay small. */
    public static final int MAX_MATERIALS = 96;
    public static final long DEFAULT_TARGET = 33_550_337L;

    public enum Unlock { ALL_FORGE_RECIPES, ALWAYS, LISTED }

    public record Settings(List<Item> materials, long target, Item keystone, Unlock unlock, List<String> unlockRecipes,
                           Set<String> unlockSkip, String syncJson) {}

    private static volatile Settings settings;            // (worked out when first asked: items must be registered)

    private HomeSettings() {
        super(GSON, "ts_home");
    }

    private static Settings defaults() {
        List<Item> m = new ArrayList<>();
        for (String id : HomeMaterials.DEFAULT_IDS) m.add(BuiltInRegistries.ITEM.get(TheoreticalSciences.id(id)));
        return new Settings(List.copyOf(m), DEFAULT_TARGET, BuiltInRegistries.ITEM.get(TheoreticalSciences.id("keystone_of_return")),
                Unlock.ALL_FORGE_RECIPES, List.of(), Set.of(), "{}");
    }

    public static Settings get() {
        Settings s = settings;
        if (s == null) settings = s = defaults();
        return s;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject all = new JsonObject();
        new TreeMap<>(map).forEach((id, json) -> all.add(id.toString(), json));
        load(all);
    }

    public static void load(JsonObject all) {
        Settings d = defaults();
        List<Item> materials = new ArrayList<>(d.materials());
        long target = d.target();
        Item keystone = d.keystone();
        Unlock unlock = d.unlock();
        List<String> listed = new ArrayList<>();
        Set<String> skip = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            String file = e.getKey();
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("materials")) {
                    materials.clear();
                    for (Item it : items(file, o.getAsJsonArray("materials"))) if (!materials.contains(it)) materials.add(it);
                }
                if (o.has("add_materials"))
                    for (Item it : items(file, o.getAsJsonArray("add_materials"))) if (!materials.contains(it)) materials.add(it);
                if (o.has("remove_materials")) materials.removeAll(items(file, o.getAsJsonArray("remove_materials")));
                if (o.has("target")) {
                    long t = o.get("target").getAsLong();
                    if (t < 1 || t > 1_000_000_000_000L) throw new IllegalArgumentException("target must be 1 to 1000000000000");
                    target = t;
                }
                if (o.has("keystone")) {
                    Item k = item(file, o.get("keystone").getAsString());
                    if (k != null) keystone = k;
                }
                if (o.has("unlock")) {
                    JsonElement u = o.get("unlock");
                    if (u.isJsonArray()) {
                        unlock = Unlock.LISTED;
                        listed.clear();
                        for (JsonElement x : u.getAsJsonArray()) listed.add(x.getAsString().trim());
                    } else {
                        String s = u.getAsString().trim();
                        unlock = switch (s) {
                            case "always" -> Unlock.ALWAYS;
                            case "all_forge_recipes" -> Unlock.ALL_FORGE_RECIPES;
                            default -> throw new IllegalArgumentException("unlock must be \"all_forge_recipes\", \"always\" or a list of Core Forge recipe ids");
                        };
                    }
                }
                if (o.has("unlock_skip")) for (JsonElement x : o.getAsJsonArray("unlock_skip")) skip.add(x.getAsString().trim());
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad Recreating Home file {}: {}", file, ex.getMessage());
            }
        }
        if (materials.size() > MAX_MATERIALS) {
            TheoreticalSciences.LOGGER.warn("Recreating Home: {} materials listed, only the first {} are used", materials.size(), MAX_MATERIALS);
            materials = new ArrayList<>(materials.subList(0, MAX_MATERIALS));
        }
        settings = new Settings(List.copyOf(materials), target, keystone, unlock, List.copyOf(listed), Set.copyOf(skip), all.toString());
        if (all.size() > 0)
            TheoreticalSciences.LOGGER.info("Recreating Home: {} materials of {} each, keystone {}, unlock {}", materials.size(), target,
                    BuiltInRegistries.ITEM.getKey(keystone), unlock == Unlock.LISTED ? listed : unlock.name().toLowerCase(java.util.Locale.ROOT));
        PackData.changed();
    }

    private static List<Item> items(String file, JsonArray a) {
        List<Item> out = new ArrayList<>();
        for (JsonElement x : a) {
            Item it = item(file, x.getAsString());
            if (it != null) out.add(it);
        }
        return out;
    }

    @Nullable
    private static Item item(String file, String id) {
        ResourceLocation r = ResourceLocation.tryParse(id.contains(":") ? id : TheoreticalSciences.MODID + ":" + id);
        if (r == null || !BuiltInRegistries.ITEM.containsKey(r) || BuiltInRegistries.ITEM.get(r) == Items.AIR) {
            TheoreticalSciences.LOGGER.warn("Recreating Home file {}: item {} does not exist (skipped)", file, id);
            return null;
        }
        return BuiltInRegistries.ITEM.get(r);
    }

    public static String syncJson() {
        return get().syncJson();
    }

    public static void clear() {
        settings = defaults();
        PackData.changed();
    }
}
