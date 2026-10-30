package com.theoreticalsciences.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Extra component tiers (coils, field emitters...) added by a datapack or KubeJS.
 * <p>
 * In plain words: every multiblock has some positions that take "tiered" parts - a coil, an emitter. The mod's own
 * parts are listed in the multiblock files (lowest tier first). A file in {@code data/<any namespace>/ts_component_tiers/}
 * lets a modpack say "this block is also a coil, and it counts as tier 6":
 * <pre>
 * {
 *   "family": "coil",                 // which kind of part ("coil" or "emitter")
 *   "tier": 6,                        // what tier the block counts as
 *   "block": "kubejs:dark_coil",      // one block... or "blocks": [...], or "tag": "mypack:tier6_coils"
 *   "critical_temperature": 16000,    // coils only, optional: kelvin, used by the machine physics (sc_Tc)
 *   "critical_field": 20000,          // coils only, optional: tesla (sc_B0)
 *   "speed_bonus": 1.5                // optional: a structure at this coil tier runs 1 + 1.5 = 2.5x as fast
 * }
 * </pre>
 * Any position whose key letter is tiered with that family then also accepts these blocks. The structure's tier is
 * still the LOWEST tier among all of its parts of that family. With no files, nothing changes.
 * Reloaded by /reload, and sent to every client (JEI previews and the machine screens use it).
 */
public final class ComponentTiers extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final ComponentTiers INSTANCE = new ComponentTiers();

    /** One file: the blocks it lists (already looked up), the tag it names, and its optional physics numbers. */
    public record Entry(String id, String family, int tier, List<Block> blocks, @Nullable TagKey<Block> tag,
                        @Nullable Double criticalTemperature, @Nullable Double criticalField, @Nullable Double speedBonus) {
        public boolean matches(BlockState st) {
            for (Block b : blocks) if (st.is(b)) return true;
            return tag != null && st.is(tag);
        }

        /** Every block this entry stands for (the tag's contents included, once tags are loaded). */
        public List<Block> allBlocks() {
            if (tag == null) return blocks;
            List<Block> out = new ArrayList<>(blocks);
            for (Holder<Block> h : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) if (!out.contains(h.value())) out.add(h.value());
            return out;
        }
    }

    /** Everything loaded, highest tier first within each family. */
    private record Data(Map<String, List<Entry>> byFamily, String syncJson) {}

    private static volatile Data data = new Data(Map.of(), "{}");

    private ComponentTiers() {
        super(GSON, "ts_component_tiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject all = new JsonObject();
        new TreeMap<>(map).forEach((id, json) -> all.add(id.toString(), json));
        load(all);
    }

    /** Reads every file (on the server after a reload; on a client from the sync message). */
    public static void load(JsonObject all) {
        Map<String, List<Entry>> fam = new HashMap<>();
        int n = 0;
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            try {
                Entry en = parse(e.getKey(), e.getValue().getAsJsonObject());
                if (en == null) continue;
                fam.computeIfAbsent(en.family(), k -> new ArrayList<>()).add(en);
                n++;
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad component tier file {}: {}", e.getKey(), ex.getMessage());
            }
        }
        Map<String, List<Entry>> frozen = new HashMap<>();
        fam.forEach((k, v) -> {
            v.sort(Comparator.comparingInt(Entry::tier).reversed());       // highest first: a block listed twice counts as its best tier
            frozen.put(k, List.copyOf(v));
        });
        data = new Data(Map.copyOf(frozen), all.toString());
        if (n > 0) TheoreticalSciences.LOGGER.info("Loaded {} custom component tiers", n);
        PackData.changed();
    }

    @Nullable
    private static Entry parse(String id, JsonObject o) {
        String family = o.get("family").getAsString().trim().toLowerCase(Locale.ROOT);
        int tier = o.get("tier").getAsInt();
        if (tier < 1) throw new IllegalArgumentException("tier must be 1 or more");
        List<Block> blocks = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (o.has("block")) names.add(o.get("block").getAsString());
        if (o.has("blocks")) for (JsonElement b : o.getAsJsonArray("blocks")) names.add(b.getAsString());
        for (String s : names) {
            ResourceLocation r = ResourceLocation.tryParse(s);
            Block b = r == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(r);
            if (b == Blocks.AIR) {
                TheoreticalSciences.LOGGER.warn("Component tier file {}: unknown block {} (skipped)", id, s);
                continue;
            }
            blocks.add(b);
        }
        TagKey<Block> tag = null;
        if (o.has("tag")) {
            String t = o.get("tag").getAsString();
            ResourceLocation r = ResourceLocation.tryParse(t.startsWith("#") ? t.substring(1) : t);
            if (r == null) throw new IllegalArgumentException("bad tag " + t);
            tag = TagKey.create(Registries.BLOCK, r);
        }
        if (blocks.isEmpty() && tag == null) {
            TheoreticalSciences.LOGGER.warn("Component tier file {} lists no usable block or tag", id);
            return null;
        }
        return new Entry(id, family, tier, List.copyOf(blocks), tag, num(o, "critical_temperature"), num(o, "critical_field"), num(o, "speed_bonus"));
    }

    @Nullable
    private static Double num(JsonObject o, String k) {
        if (!o.has(k) || o.get(k).isJsonNull()) return null;
        double v = o.get(k).getAsDouble();
        return Double.isFinite(v) ? v : null;
    }

    /** The JSON sent to clients. */
    public static String syncJson() {
        return data.syncJson();
    }

    /** Forget a server's tiers (the client left it). */
    public static void clear() {
        data = new Data(Map.of(), "{}");
        PackData.changed();
    }

    public static boolean isEmpty() {
        return data.byFamily().isEmpty();
    }

    /** The tier a datapack gives this block in this family, or -1 if no file mentions it. */
    public static int tierOf(String family, BlockState st) {
        List<Entry> l = data.byFamily().get(family);
        if (l == null) return -1;
        for (Entry e : l) if (e.matches(st)) return e.tier();
        return -1;
    }

    /** Every data-added block of a family with its tier (for JEI, the hologram and the Structure Assembler). */
    public static List<Map.Entry<Integer, Block>> blocks(String family) {
        List<Entry> l = data.byFamily().get(family);
        if (l == null) return List.of();
        List<Map.Entry<Integer, Block>> out = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        for (Entry e : l) for (Block b : e.allBlocks()) if (seen.add(b)) out.add(Map.entry(e.tier(), b));
        return out;
    }

    private static Optional<Entry> first(String family, int tier, java.util.function.Function<Entry, Double> f) {
        List<Entry> l = data.byFamily().get(family);
        if (l == null) return Optional.empty();
        for (Entry e : l) if (e.tier() == tier && f.apply(e) != null) return Optional.of(e);
        return Optional.empty();
    }

    /** Critical temperature (K) a datapack set for this coil tier, or null. */
    @Nullable
    public static Double criticalTemperature(int tier) {
        return first("coil", tier, Entry::criticalTemperature).map(Entry::criticalTemperature).orElse(null);
    }

    /** Upper critical field (T) a datapack set for this coil tier, or null. */
    @Nullable
    public static Double criticalField(int tier) {
        return first("coil", tier, Entry::criticalField).map(Entry::criticalField).orElse(null);
    }

    /** Speed bonus a datapack set for this tier of this family (1.5 = +150%), or null. */
    @Nullable
    public static Double speedBonus(String family, int tier) {
        return first(family, tier, Entry::speedBonus).map(Entry::speedBonus).orElse(null);
    }
}
