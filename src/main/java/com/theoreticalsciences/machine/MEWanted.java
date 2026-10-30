package com.theoreticalsciences.machine;

import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHRecipes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Which items and fluids any recipe of a machine type can use: what an ME network is asked about. Rebuilt on /reload. */
public record MEWanted(java.util.List<ItemStack> items, java.util.List<FluidStack> fluids) {
    private static RecipeManager seen;
    private static final Map<String, MEWanted> CACHE = new HashMap<>();

    private static String key(ItemStack s) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()) + s.getComponentsPatch().toString();
    }

    public static synchronized MEWanted of(Level level, String recipeType) {
        RecipeManager rm = level.getRecipeManager();
        if (rm != seen) {
            seen = rm;
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(recipeType, t -> {
            Map<String, ItemStack> it = new java.util.LinkedHashMap<>();
            Map<String, FluidStack> fl = new java.util.LinkedHashMap<>();
            java.util.List<RecipeHolder<QHRecipe>> all = new java.util.ArrayList<>(rm.getAllRecipesFor(QHRecipes.type(t)));
            if (t.equals(com.theoreticalsciences.transcend.Essences.TYPE)) all.addAll(com.theoreticalsciences.transcend.Essences.recipes());
            for (RecipeHolder<QHRecipe> h : all) {
                for (SizedIngredient s : h.value().itemInputs) for (ItemStack st : s.ingredient().getItems()) it.putIfAbsent(key(st), st.copyWithCount(1));
                for (Ingredient c : h.value().catalysts) for (ItemStack st : c.getItems()) it.putIfAbsent(key(st), st.copyWithCount(1));
                for (SizedFluidIngredient s : h.value().fluidInputs) for (FluidStack st : s.getFluids())
                    fl.putIfAbsent(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(st.getFluid()) + st.getComponentsPatch().toString(), st.copyWithAmount(1));
            }
            return new MEWanted(java.util.List.copyOf(it.values()), java.util.List.copyOf(fl.values()));
        });
    }

    /** Whether some recipe of this machine type takes this fluid (any components) as an input. */
    public boolean wantsFluid(FluidStack f) {
        for (FluidStack w : fluids) if (w.getFluid().isSame(f.getFluid())) return true;
        return false;
    }
}
