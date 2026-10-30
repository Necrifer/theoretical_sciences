package com.theoreticalsciences.extreme;

import com.theoreticalsciences.registry.QHItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collections of every installed mod's things, gathered like singularities (round 24): the Biosphere Compendium wants
 * one of every meat, catch and mob drop, the Primordial Harvest one of every crop, seed, fruit and flower - far more
 * than the Extreme Assembly Table holds. So they are first gathered <i>per mod</i> into Specimen Bundles (up to
 * {@link #BUNDLE} items each; a mod with more gets several), and when there are still too many bundles, nine bundles
 * are bound into an Archive, nine archives into a greater one... The recipe asks for whatever level fits. New mods'
 * items are picked up automatically (they only have to be in the tags). Worked out from the tags on each side
 * (tags are the same on server and client), so nothing needs to be sent.
 */
public final class Collections {
    public static final int BUNDLE = 64, GROUP = 9;
    /** name -> the tags whose items it gathers (from the bundle recipes). */
    private static final Map<String, List<String>> DEFS = new ConcurrentHashMap<>();
    private static final Map<String, List<ItemStack>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<Item>> MEMBER_CACHE = new ConcurrentHashMap<>();
    private static volatile int version;

    public static void define(String name, List<String> tags) {
        List<String> old = DEFS.put(name, List.copyOf(tags));
        if (!tags.equals(old)) invalidate();
    }

    public static boolean defined(String name) {
        return DEFS.containsKey(name);
    }

    /** Tags (re)loaded on either side. */
    public static void invalidate() {
        version++;
        CACHE.clear();
        MEMBER_CACHE.clear();
    }

    public static int version() {
        return version;
    }

    /** Every different item of the collection, by mod then id. */
    public static List<Item> items(String name) {
        List<Item> hit = MEMBER_CACHE.get(name);
        if (hit != null) return hit;
        int v = version;
        java.util.Set<Item> set = new java.util.LinkedHashSet<>();
        for (String t : DEFS.getOrDefault(name, List.of())) {
            ResourceLocation r = ResourceLocation.tryParse(t.replace("#", ""));
            if (r == null) continue;
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, r))) set.add(h.value());
        }
        List<Item> l = new ArrayList<>(set);
        l.sort(java.util.Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        l = List.copyOf(l);
        if (v == version) MEMBER_CACHE.put(name, l);
        return l;
    }

    /** The items of level-1 bundle {@code index}: one mod's (or part of one mod's) items. */
    public static List<Item> bundleItems(String name, int index) {
        List<List<Item>> b = bundles(name);
        return index >= 0 && index < b.size() ? b.get(index) : List.of();
    }

    private static List<List<Item>> bundles(String name) {
        Map<String, List<Item>> byMod = new TreeMap<>();
        for (Item it : items(name)) byMod.computeIfAbsent(BuiltInRegistries.ITEM.getKey(it).getNamespace(), k -> new ArrayList<>()).add(it);
        List<List<Item>> out = new ArrayList<>();
        for (List<Item> l : byMod.values())
            for (int i = 0; i < l.size(); i += BUNDLE) out.add(l.subList(i, Math.min(l.size(), i + BUNDLE)));
        return out;
    }

    /** Level 1: the bundles; level 2+: archives of nine of the level below. Cached per tag reload. */
    public static List<ItemStack> level(String name, int level) {
        String key = name + "#" + level;
        List<ItemStack> hit = CACHE.get(key);
        if (hit != null) return hit;
        int v = version;
        List<ItemStack> out = new ArrayList<>();
        if (level <= 1) {
            List<List<Item>> b = bundles(name);
            Map<String, Integer> parts = new LinkedHashMap<>();
            for (List<Item> l : b) parts.merge(BuiltInRegistries.ITEM.getKey(l.get(0)).getNamespace(), 1, Integer::sum);
            Map<String, Integer> seen = new LinkedHashMap<>();
            for (int i = 0; i < b.size(); i++) {
                String mod = BuiltInRegistries.ITEM.getKey(b.get(i).get(0)).getNamespace();
                int part = seen.merge(mod, 1, Integer::sum);
                StringBuilder sb = new StringBuilder();
                for (Item it : b.get(i)) sb.append(BuiltInRegistries.ITEM.getKey(it)).append(';');
                out.add(stack(name, 1, i, Integer.toHexString(sb.toString().hashCode()), mod, part, parts.get(mod)));
            }
        } else {
            List<ItemStack> below = level(name, level - 1);
            for (int g = 0; g * GROUP < below.size(); g++) {
                StringBuilder sb = new StringBuilder();
                for (ItemStack s : below.subList(g * GROUP, Math.min(below.size(), g * GROUP + GROUP))) sb.append(idOf(s)).append(';');
                out.add(stack(name, level, g, Integer.toHexString(sb.toString().hashCode()), null, 0, 0));
            }
        }
        List<ItemStack> l = List.copyOf(out);
        if (v == version) CACHE.put(key, l);
        return l;
    }

    /** What a recipe with {@code room} free places asks for: the bundles, or archives of them if those do not fit. */
    public static List<ItemStack> requirements(String name, int room) {
        List<ItemStack> cur = level(name, 1);
        for (int lv = 2; cur.size() > Math.max(1, room) && lv <= 8; lv++) cur = level(name, lv);
        return cur;
    }

    /** Levels a recipe with at least {@code room} places could need (for JEI). */
    public static int neededLevel(String name, int room) {
        List<ItemStack> cur = level(name, 1);
        int lv = 1;
        while (cur.size() > Math.max(1, room) && lv < 8) cur = level(name, ++lv);
        return lv;
    }

    static ItemStack stack(String name, int level, int index, String sig, @Nullable String mod, int part, int parts) {
        ItemStack s = new ItemStack(QHItems.get("specimen_bundle"));
        CompoundTag t = new CompoundTag();
        t.putString("Collection", name);
        t.putString("Bundle", level + "." + index);
        t.putString("Sig", sig);
        if (mod != null) {
            t.putString("Mod", mod);
            if (parts > 1) t.putString("Part", part + "/" + parts);
        }
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static String idOf(ItemStack s) {
        if (!s.is(QHItems.get("specimen_bundle"))) return null;
        var cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.getUnsafe();
        return t.contains("Bundle") ? t.getString("Collection") + "|" + t.getString("Bundle") + "@" + t.getString("Sig") : null;
    }

    public static boolean same(ItemStack a, ItemStack b) {
        String x = idOf(a);
        return x != null && x.equals(idOf(b));
    }

    @SuppressWarnings("deprecation")
    public static Component name(ItemStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return Component.translatable("item.theoreticalsciences.specimen_bundle");
        CompoundTag t = cd.getUnsafe();
        Component col = Component.translatable("collection.theoreticalsciences." + t.getString("Collection"));
        String b = t.getString("Bundle");
        int level = 1, index = 0;
        try {
            String[] p = b.split("\\.");
            level = Integer.parseInt(p[0]);
            index = Integer.parseInt(p[1]);
        } catch (RuntimeException ignored) {
        }
        if (level <= 1 && t.contains("Mod")) {
            String mod = t.getString("Mod");
            String modName = net.neoforged.fml.ModList.get().getModContainerById(mod).map(c -> c.getModInfo().getDisplayName()).orElse(mod);
            Component c = Component.translatable("item.theoreticalsciences.specimen_bundle.of", col, modName);
            return t.contains("Part") ? c.copy().append(" (" + t.getString("Part") + ")") : c;
        }
        return Component.translatable("item.theoreticalsciences.specimen_bundle.archive" + Math.min(level, 4), col, index + 1);
    }

    /** A colour of its own per mod (bundles) or per collection (archives). */
    @SuppressWarnings("deprecation")
    public static int colour(ItemStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        String k = cd == null ? "" : cd.getUnsafe().contains("Mod") ? cd.getUnsafe().getString("Mod") : cd.getUnsafe().getString("Collection") + cd.getUnsafe().getString("Bundle");
        int h = k.hashCode();
        return net.minecraft.util.Mth.hsvToRgb(((h >>> 4) & 0xFFF) / 4096F, 0.6F, 1F) & 0xFFFFFF;
    }

    private Collections() {}
}
