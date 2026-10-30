package com.theoreticalsciences.extreme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singularities anyone can add (round 21), like Avaritia's and Extended Crafting's.
 * <ul>
 *   <li>Datapack files {@code data/<ns>/ts_singularities/<name>.json}:
 *   {@code {"item": "mod:thing_ingot"} or {"tag": "c:ingots/thing"}, "count": 16384, "name": "Thing Singularity",
 *   "core": "#202020", "glow": "#ffcc80", "enabled": true}} - colours left out are taken from the item's texture.</li>
 *   <li>Every item tagged {@code c:ingots} gets one automatically (unless it already has one, or is in the config
 *   list {@code autoSingularityBlacklist}).</li>
 *   <li>Each is the item {@code theoreticalsciences:dynamic_singularity} with custom data {@code Singularity: <id>}, made in
 *   the Singularity Compactor, and the Omniscience Catalyst needs one of every one. When there are more than the
 *   Extreme Assembly Table can take, they are condensed nine at a time into <i>Singularity Clusters</i> (and clusters
 *   into Hyperclusters...), which the catalyst then asks for instead. Clusters have default names; rename them with a
 *   resource pack (lang keys {@code cluster.theoreticalsciences.<level>.<index>}) or in a datapack file
 *   {@code ts_singularities/_clusters.json}: {@code {"names": {"1.0": "Ferrous Cluster", ...}}}.</li>
 * </ul>
 * The server works out the final list and sends it to every client, so both always agree.
 */
public final class Singularities extends SimpleJsonResourceReloadListener {
    public static final Singularities INSTANCE = new Singularities();
    private static final Gson GSON = new GsonBuilder().create();
    public static final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots"));
    public static final int CLUSTER = 9;
    /** Bumped whenever either side's list changes: cached lists below are thrown away. */
    private static volatile int version;
    private static final Map<String, List<ItemStack>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** One singularity: id, what it is made of (item or tag), how many, its name and colours (-1: from the texture). */
    public record Def(String id, @Nullable String item, @Nullable String tag, int count, @Nullable String name, int core, int glow) {
        public net.minecraft.world.item.crafting.Ingredient ingredient() {
            if (item != null) {
                Item it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(item)).orElse(Items.AIR);
                return it == Items.AIR ? net.minecraft.world.item.crafting.Ingredient.EMPTY : net.minecraft.world.item.crafting.Ingredient.of(it);
            }
            ResourceLocation t = tag == null ? null : ResourceLocation.tryParse(tag);
            return t == null ? net.minecraft.world.item.crafting.Ingredient.EMPTY : net.minecraft.world.item.crafting.Ingredient.of(TagKey.create(Registries.ITEM, t));
        }

        /** The item shown / named after. */
        public ItemStack source() {
            ItemStack[] s = ingredient().getItems();
            return s.length == 0 ? ItemStack.EMPTY : s[0];
        }
    }

    private Map<ResourceLocation, JsonObject> files = Map.of();
    private Map<String, String> clusterNames = Map.of();
    private static volatile List<Def> server, client;
    private static volatile Map<String, String> serverNames = Map.of(), clientNames = Map.of();

    private Singularities() {
        super(GSON, "ts_singularities");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> f = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        map.forEach((id, json) -> {
            try {
                if (!json.isJsonObject()) throw new IllegalArgumentException("not a JSON object");
                if (id.getPath().equals("_clusters")) {
                    JsonElement n = json.getAsJsonObject().get("names");
                    if (n != null && n.isJsonObject()) n.getAsJsonObject().entrySet().forEach(e -> {
                        if (e.getValue().isJsonPrimitive()) names.put(e.getKey(), e.getValue().getAsString());
                    });
                } else f.put(id, json.getAsJsonObject());
            } catch (RuntimeException e) {
                TheoreticalSciences.LOGGER.warn("Skipping singularity file {}: {}", id, e.getMessage());
            }
        });
        files = f;
        clusterNames = names;
        invalidate();          // worked out again once tags are bound
    }

    /** Tags were (re)loaded on this side. */
    public static void invalidate() {
        server = null;
        changed();
    }

    /** Changes on any reload of tags (or of the list). */
    public static int tagVersion() {
        return version;
    }

    private static void changed() {
        version++;
        CACHE.clear();
    }

    private static int colour(JsonObject o, String k) {
        if (!o.has(k)) return -1;
        try {
            String s = o.get(k).getAsString().replace("#", "");
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** Server side: the full list (datapack ones, then the automatic ingots), sorted by id. */
    public static synchronized List<Def> server() {
        if (server != null) return server;
        Map<String, Def> out = new LinkedHashMap<>();
        Set<Item> covered = new HashSet<>();
        // the mod's own curated singularities already cover these
        for (String s : new String[]{"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:copper_ingot", "minecraft:netherite_ingot"})
            covered.add(BuiltInRegistries.ITEM.get(ResourceLocation.parse(s)));
        for (var e : INSTANCE.files.entrySet()) {
            try {
                JsonObject o = e.getValue();
                if (o.has("enabled") && !o.get("enabled").getAsBoolean()) continue;
                String item = o.has("item") ? o.get("item").getAsString() : null, tag = o.has("tag") ? o.get("tag").getAsString().replace("#", "") : null;
                if (item == null && tag == null) continue;
                Def d = new Def(e.getKey().toString(), item, tag, o.has("count") ? Math.max(1, o.get("count").getAsInt()) : autoCount(),
                        o.has("name") ? o.get("name").getAsString() : null, colour(o, "core"), colour(o, "glow"));
                if (d.ingredient().isEmpty()) continue;
                out.put(d.id(), d);
                for (ItemStack s : d.ingredient().getItems()) covered.add(s.getItem());
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.warn("Skipping singularity {}: {}", e.getKey(), ex.getMessage());
            }
        }
        Set<String> black = new HashSet<>();
        try {
            black.addAll(QHConfig.AUTO_SINGULARITY_BLACKLIST.get());
        } catch (IllegalStateException ignored) {
        }
        int auto = 0;
        if (autoEnabled())
            for (Item it : ingots()) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(it);
                if (covered.contains(it) || key.getNamespace().equals(TheoreticalSciences.MODID) || black.contains(key.toString()) || black.contains(key.getNamespace() + ":*"))
                    continue;
                if (new ItemStack(it).is(com.theoreticalsciences.transcend.OmnipotencePayload.BEYOND)) continue;
                String id = TheoreticalSciences.MODID + ":auto/" + key.getNamespace() + "/" + key.getPath();
                if (out.putIfAbsent(id, new Def(id, key.toString(), null, autoCount(), null, -1, -1)) == null) auto++;
                covered.add(it);
            }
        List<Def> l = new ArrayList<>(out.values());
        l.sort(java.util.Comparator.comparing(Def::id));
        serverNames = Map.copyOf(INSTANCE.clusterNames);
        server = List.copyOf(l);
        TheoreticalSciences.LOGGER.info("Singularities: {} in all ({} automatic, from every ingot of every mod)", l.size(), auto);
        changed();
        return server;
    }

    /**
     * Every ingot of every mod. Mods do not all tag their ingots the same way: many put them only in
     * {@code c:ingots/<material>} (not in {@code c:ingots} itself), some only in the old {@code forge:ingots/...},
     * and a few in no tag at all - so all of those count, as does any item whose id ends in "_ingot".
     * (Not hot ingots, which cool into the real thing.)
     */
    static List<Item> ingots() {
        java.util.LinkedHashSet<Item> out = new java.util.LinkedHashSet<>();
        for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(INGOTS)) out.add(h.value());
        BuiltInRegistries.ITEM.getTags().forEach(p -> {
            ResourceLocation t = p.getFirst().location();
            if ((t.getNamespace().equals("c") || t.getNamespace().equals("forge")) && (t.getPath().equals("ingots") || t.getPath().startsWith("ingots/")))
                for (var h : p.getSecond()) out.add(h.value());
        });
        for (Item it : BuiltInRegistries.ITEM) {
            String path = BuiltInRegistries.ITEM.getKey(it).getPath();
            if (path.endsWith("_ingot") && !path.startsWith("hot_") && !path.contains("/")) out.add(it);
        }
        out.removeIf(it -> BuiltInRegistries.ITEM.getKey(it).getPath().startsWith("hot_"));
        out.remove(net.minecraft.world.item.Items.AIR);
        return new ArrayList<>(out);
    }

    private static int autoCount() {
        try {
            return QHConfig.AUTO_SINGULARITY_COUNT.get();
        } catch (IllegalStateException e) {
            return 16384;
        }
    }

    private static boolean autoEnabled() {
        try {
            return QHConfig.AUTO_SINGULARITIES.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** The list on this side: the server's own, or what the server sent. */
    public static List<Def> all(boolean clientSide) {
        if (clientSide) {
            if (client != null) return client;
            // single player: the server is in this game, so its list is the right one even before it was sent
            return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null ? server() : List.of();
        }
        return server();
    }

    /** Whichever list this side has (for names and colours). */
    public static List<Def> any() {
        List<Def> c = client;
        if (c != null) return c;
        // a client still waiting for the server's list shows nothing rather than guessing (and keeping the guess)
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() == null)
            return List.of();
        return server();
    }

    public static Map<String, String> names(boolean clientSide) {
        return clientSide ? clientNames : serverNames;
    }

    @Nullable
    public static Def byId(String id, boolean clientSide) {
        for (Def d : all(clientSide)) if (d.id().equals(id)) return d;
        return null;
    }

    // ------------------------------------------------------------------ the items
    public static ItemStack stack(Def d) {
        ItemStack s = new ItemStack(QHItems.get("dynamic_singularity"));
        CompoundTag t = new CompoundTag();
        t.putString("Singularity", d.id());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    /**
     * A cluster also records a fingerprint of exactly which singularities went into it, so when a mod is added and the
     * groups move, an old cluster does not quietly start standing for different singularities.
     */
    /** A cluster's level (1 Cluster, 2 Hypercluster, 3 Ultracluster...), or 0 for anything else. */
    @SuppressWarnings("deprecation")
    public static int clusterLevel(ItemStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return 0;
        String c = cd.getUnsafe().getString("Cluster");
        int dot = c.indexOf('.');
        try {
            return dot > 0 ? Integer.parseInt(c.substring(0, dot)) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static ItemStack cluster(int level, int index, String sig) {
        ItemStack s = new ItemStack(QHItems.get("singularity_cluster"));
        CompoundTag t = new CompoundTag();
        t.putString("Cluster", level + "." + index);
        t.putString("Sig", sig);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    private static String sig(List<ItemStack> members) {
        StringBuilder b = new StringBuilder();
        for (ItemStack m : members) b.append(idOf(m)).append(';');
        return Integer.toHexString(b.toString().hashCode());
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static String idOf(ItemStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.getUnsafe();          // read only: no copy
        if (t.contains("Singularity")) return t.getString("Singularity");
        if (t.contains("Cluster")) return "#" + t.getString("Cluster") + (t.contains("Sig") ? "@" + t.getString("Sig") : "");
        return null;
    }

    /** Same singularity / cluster (the item and its id). */
    public static boolean same(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        String x = idOf(a);
        return x != null && x.equals(idOf(b));
    }

    public static Component name(ItemStack s, boolean clientSide) {
        String id = idOf(s);
        if (id == null) return Component.translatable(s.getItem().getDescriptionId());
        if (id.startsWith("#")) {
            String key = id.substring(1);
            int at = key.indexOf('@');
            if (at >= 0) key = key.substring(0, at);
            String[] p = key.split("\\.");
            int level = p.length > 0 ? parse(p[0]) : 1, index = p.length > 1 ? parse(p[1]) : 0;
            String custom = names(clientSide).get(key);
            if (custom != null) return Component.literal(custom);
            Component def = Component.translatable("item.theoreticalsciences.singularity_cluster.level" + Math.min(level, 3), index + 1);
            return Component.translatableWithFallback("cluster.theoreticalsciences." + key, def.getString());
        }
        Def d = byId(id, clientSide);
        if (d == null) return Component.translatable("item.theoreticalsciences.dynamic_singularity");
        if (d.name() != null) return Component.literal(d.name());
        ItemStack src = d.source();
        return Component.translatable("item.theoreticalsciences.dynamic_singularity.of", src.isEmpty() ? Component.literal(d.id()) : stripIngot(src.getHoverName()));
    }

    private static Component stripIngot(Component c) {
        String s = c.getString();
        for (String suffix : new String[]{" Ingot", " ingot"}) if (s.endsWith(suffix)) return Component.literal(s.substring(0, s.length() - suffix.length()));
        return c;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ what the Omniscience Catalyst asks for
    /**
     * Every dynamic singularity - or, if there are more than {@code room}, their clusters (nine to a cluster, clusters
     * nine to a hypercluster, ...): the stacks that must be in the grid.
     */
    public static List<ItemStack> requirements(int room, boolean clientSide) {
        List<ItemStack> cur = level(0, clientSide);
        for (int level = 1; cur.size() > Math.max(1, room) && level <= 6; level++) cur = level(level, clientSide);
        return cur;
    }

    /** Everything at one level: the singularities (0), their clusters (1), clusters of clusters (2)... Cached. */
    static List<ItemStack> level(int level, boolean clientSide) {
        String key = level + (clientSide ? "c" : "s");
        List<ItemStack> hit = CACHE.get(key);
        if (hit != null) return hit;
        int v = version;
        List<ItemStack> out = new ArrayList<>();
        if (level == 0) for (Def x : all(clientSide)) out.add(stack(x));
        else {
            List<ItemStack> below = level(level - 1, clientSide);
            for (int g = 0; g * CLUSTER < below.size(); g++)
                out.add(cluster(level, g, sig(below.subList(g * CLUSTER, Math.min(below.size(), g * CLUSTER + CLUSTER)))));
        }
        List<ItemStack> l = List.copyOf(out);
        if (v == version) CACHE.put(key, l);
        return l;
    }

    /** The members of cluster (level, index): singularities (level 1) or clusters of the level below. */
    public static List<ItemStack> members(int level, int index, boolean clientSide) {
        List<ItemStack> cur = level(level - 1, clientSide);
        int from = index * CLUSTER;
        if (level < 1 || from >= cur.size()) return List.of();
        return cur.subList(from, Math.min(cur.size(), from + CLUSTER));
    }

    /** How many places a catalyst leaves free for singularities, for showing clusters (the Omniscience Catalyst: 35). */
    public static final int SHOWN_ROOM = 35;

    /** The clusters worth showing in JEI: only levels some catalyst would ask for. */
    public static List<int[]> shownClusters(boolean clientSide) {
        List<int[]> out = new ArrayList<>();
        for (int[] c : clusters(1, clientSide)) if (level(c[0] - 1, clientSide).size() > SHOWN_ROOM) out.add(c);
        return out;
    }

    /** The cluster item for (level, index) in this game. */
    public static ItemStack clusterStack(int level, int index, boolean clientSide) {
        List<ItemStack> l = level(level, clientSide);
        return index < l.size() ? l.get(index) : ItemStack.EMPTY;
    }

    /** Every cluster that exists for a catalyst needing {@code room} places: level, index pairs. */
    public static List<int[]> clusters(int room, boolean clientSide) {
        List<int[]> out = new ArrayList<>();
        int n = all(clientSide).size(), level = 0;
        while (n > Math.max(1, room) && level < 6) {
            level++;
            int groups = (n + CLUSTER - 1) / CLUSTER;
            for (int g = 0; g < groups; g++) out.add(new int[]{level, g});
            n = groups;
        }
        return out;
    }

    // ------------------------------------------------------------------ sync
    public static String syncJson() {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (Def d : server()) {
            JsonObject j = new JsonObject();
            j.addProperty("id", d.id());
            if (d.item() != null) j.addProperty("item", d.item());
            if (d.tag() != null) j.addProperty("tag", d.tag());
            j.addProperty("count", d.count());
            if (d.name() != null) j.addProperty("name", d.name());
            j.addProperty("core", d.core());
            j.addProperty("glow", d.glow());
            a.add(j);
        }
        o.add("list", a);
        JsonObject n = new JsonObject();
        serverNames.forEach(n::addProperty);
        o.add("names", n);
        return GSON.toJson(o);
    }

    public static void receive(String json) {
        try {
            JsonObject o = GSON.fromJson(json, JsonObject.class);
            List<Def> l = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("list")) {
                JsonObject j = e.getAsJsonObject();
                l.add(new Def(j.get("id").getAsString(), j.has("item") ? j.get("item").getAsString() : null, j.has("tag") ? j.get("tag").getAsString() : null,
                        j.get("count").getAsInt(), j.has("name") ? j.get("name").getAsString() : null, j.get("core").getAsInt(), j.get("glow").getAsInt()));
            }
            Map<String, String> n = new LinkedHashMap<>();
            if (o.has("names")) o.getAsJsonObject("names").entrySet().forEach(e -> n.put(e.getKey(), e.getValue().getAsString()));
            client = List.copyOf(l);
            clientNames = Map.copyOf(n);
            changed();
        } catch (RuntimeException e) {
            TheoreticalSciences.LOGGER.warn("Bad singularity list from the server: {}", e.getMessage());
        }
    }

    public static void clearClient() {
        client = null;
        clientNames = Map.of();
        changed();
    }
}
