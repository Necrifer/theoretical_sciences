package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mod Essences (round 26): every fluid of every other mod, distilled per mod into one fluid that stands for it - like
 * singularities for ingots and specimen bundles for drops. The Essence Distillery takes up to {@link #PART} of one
 * mod's fluids at a time ({@link #PER} mB each) and makes {@link #OUT} mB of that mod's Essence; a mod with more
 * fluids than that is distilled in parts first, and the parts (four thousand mB of each) into the whole. The Pleroma
 * Confluence then wants every mod's Essence too. All of it is worked out from the fluid registry (the same on server
 * and client), so any mod's fluids join in by themselves.
 * <p>
 * Only fluids that can be carried in a bucket count (a fluid without one is usually an internal, unobtainable one);
 * the config {@code essenceBlacklist} leaves out fluids ({@code mod:fluid}) or whole mods ({@code mod:*}).
 */
public final class Essences {
    public static final int PART = 12, PER = 16_000, OUT = 1_000, PART_IN = 4_000;
    public static final String TYPE = "essence_distillation";

    /** One distillation: level 1 takes fluids, higher levels take the parts below. */
    public record Step(String mod, int level, int index, int levels, List<FluidStack> inputs, FluidStack output) {
        public boolean last() {
            return level == levels;
        }
    }

    private static volatile List<Step> steps;
    private static volatile Map<String, List<Fluid>> byMod;
    private static boolean early;

    public static Fluid essenceFluid() {
        return QHFluids.ALL.get("mod_essence").source.get();
    }

    public static boolean isEssence(FluidStack s) {
        return !s.isEmpty() && s.getFluid().isSame(essenceFluid());
    }

    public static FluidStack essence(String mod, int level, int index, int levels, int amount) {
        FluidStack s = new FluidStack(essenceFluid(), amount);
        CompoundTag t = new CompoundTag();
        t.putString("Mod", mod);
        if (level < levels) t.putString("Part", level + "." + index);       // the whole essence has no part number
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static String mod(FluidStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null || !cd.getUnsafe().contains("Mod") ? null : cd.getUnsafe().getString("Mod");
    }

    /** Which Essence this is, for telling them apart: "mod" for a whole one, "mod#level.index" for a part. */
    @SuppressWarnings("deprecation")
    @Nullable
    public static String key(FluidStack s) {
        String m = mod(s);
        if (m == null) return null;
        var cd = s.get(DataComponents.CUSTOM_DATA);
        String part = cd == null ? "" : cd.getUnsafe().getString("Part");
        return part.isEmpty() ? m : m + "#" + part;
    }

    @SuppressWarnings("deprecation")
    public static Component name(FluidStack s) {
        String m = mod(s);
        if (m == null) return Component.translatable("fluid_type.theoreticalsciences.mod_essence");
        String n = net.neoforged.fml.ModList.get().getModContainerById(m).map(c -> c.getModInfo().getDisplayName()).orElse(m);
        var cd = s.get(DataComponents.CUSTOM_DATA);
        String part = cd == null ? "" : cd.getUnsafe().getString("Part");
        return part.isEmpty() ? Component.translatable("fluid.theoreticalsciences.mod_essence.of", n)
                : Component.translatable("fluid.theoreticalsciences.mod_essence.part", n, part);
    }

    public static int colour(FluidStack s) {
        String m = mod(s);
        if (m == null) return 0xFF80FFE0;
        int h = m.hashCode();
        return 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(((h >>> 4) & 0xFFF) / 4096F, 0.55F, 1F);
    }

    private static boolean blacklisted(ResourceLocation id) {
        try {
            List<? extends String> l = com.theoreticalsciences.QHConfig.ESSENCE_BLACKLIST.get();
            return l.contains(id.toString()) || l.contains(id.getNamespace() + ":*");
        } catch (IllegalStateException e) {
            early = true;           // the config is not loaded yet: answer, but do not remember this answer
            return false;
        }
    }

    /** Every other mod's obtainable fluids, by mod (sorted). */
    public static synchronized Map<String, List<Fluid>> fluids() {
        if (byMod != null) return byMod;
        Map<String, List<Fluid>> out = new TreeMap<>();
        early = false;
        for (Fluid f : BuiltInRegistries.FLUID) {
            if (f == Fluids.EMPTY) continue;
            if (f instanceof FlowingFluid ff && ff.getSource() != f) continue;          // the flowing half of a fluid
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(f);
            String ns = id.getNamespace();
            if (ns.equals("minecraft") || ns.equals(TheoreticalSciences.MODID) || blacklisted(id)) continue;
            if (f.getBucket() == Items.AIR) continue;                                  // (no bucket: internal, not to be had)
            out.computeIfAbsent(ns, k -> new ArrayList<>()).add(f);
        }
        out.values().forEach(l -> l.sort(java.util.Comparator.comparing(f -> BuiltInRegistries.FLUID.getKey(f).toString())));
        if (!early) byMod = out;
        return out;
    }

    /** Every distillation step there is, for every mod. */
    public static synchronized List<Step> steps() {
        if (steps != null) return steps;
        List<Step> out = new ArrayList<>();
        for (var e : fluids().entrySet()) {
            String mod = e.getKey();
            List<Fluid> fl = e.getValue();
            // how many levels: parts of twelve fluids, parts of twelve parts...
            int levels = 1;
            for (int n = (fl.size() + PART - 1) / PART; n > 1; n = (n + PART - 1) / PART) levels++;
            List<FluidStack> below = new ArrayList<>();
            for (Fluid f : fl) below.add(new FluidStack(f, PER));
            for (int lv = 1; lv <= levels; lv++) {
                List<FluidStack> made = new ArrayList<>();
                for (int g = 0; g * PART < below.size(); g++) {
                    List<FluidStack> in = below.subList(g * PART, Math.min(below.size(), g * PART + PART));
                    FluidStack o = essence(mod, lv, g, levels, OUT);
                    out.add(new Step(mod, lv, g, levels, List.copyOf(in), o));
                    made.add(o.copyWithAmount(PART_IN));
                }
                below = made;
            }
        }
        List<Step> l = List.copyOf(out);
        if (byMod != null) steps = l;
        return l;
    }

    /** The whole essence of every mod: what the Pleroma Confluence wants on top of everything else. */
    public static List<FluidStack> finals() {
        List<FluidStack> out = new ArrayList<>();
        for (Step s : steps()) if (s.last()) out.add(s.output().copyWithAmount(1));
        return out;
    }

    private static volatile List<RecipeHolder<QHRecipe>> recipes;

    /** The steps as machine recipes of the Essence Distillery (not in any datapack: made from the registry). */
    public static synchronized List<RecipeHolder<QHRecipe>> recipes() {
        if (recipes != null) return recipes;
        List<RecipeHolder<QHRecipe>> out = new ArrayList<>();
        for (Step s : steps()) {
            List<SizedFluidIngredient> in = new ArrayList<>();
            for (FluidStack f : s.inputs())
                in.add(new SizedFluidIngredient(isEssence(f) ? DataComponentFluidIngredient.of(true, f) : net.neoforged.neoforge.fluids.crafting.FluidIngredient.of(f.getFluid()), f.getAmount()));
            QHRecipe r = new QHRecipe(TYPE, List.of(), in, List.of(), List.of(s.output().copy()), 262_144 * s.level(), 600 + 40 * s.inputs().size(), "");
            String path = "essence/" + s.mod() + "/" + s.level() + "_" + s.index();
            out.add(new RecipeHolder<>(TheoreticalSciences.id(path), r));
        }
        List<RecipeHolder<QHRecipe>> l = List.copyOf(out);
        if (steps != null) recipes = l;
        return l;
    }

    @Nullable
    public static RecipeHolder<QHRecipe> byId(ResourceLocation id) {
        if (!id.getNamespace().equals(TheoreticalSciences.MODID) || !id.getPath().startsWith("essence/")) return null;
        for (RecipeHolder<QHRecipe> h : recipes()) if (h.id().equals(id)) return h;
        return null;
    }

    private Essences() {}
}
