package com.theoreticalsciences.recipe;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

/** Greedy matching of sized ingredients against slots. When {@code consume} is true the stacks are shrunk in place. */
public final class RecipeMatcher {
    public static boolean matchItems(List<SizedIngredient> wanted, List<ItemStack> available, boolean consume) {
        int[] left = new int[available.size()];
        for (int i = 0; i < left.length; i++) left[i] = available.get(i).getCount();
        int[][] take = new int[wanted.size()][available.size()];
        for (int w = 0; w < wanted.size(); w++) {
            SizedIngredient ing = wanted.get(w);
            int need = ing.count();
            for (int i = 0; i < available.size() && need > 0; i++) {
                ItemStack s = available.get(i);
                if (left[i] <= 0 || s.isEmpty() || !ing.ingredient().test(s)) continue;
                int t = Math.min(need, left[i]);
                left[i] -= t;
                take[w][i] += t;
                need -= t;
            }
            if (need > 0) return false;
        }
        if (consume) {
            for (int[] row : take)
                for (int i = 0; i < row.length; i++)
                    if (row[i] > 0) available.get(i).shrink(row[i]);
        }
        return true;
    }

    public static boolean matchFluids(List<SizedFluidIngredient> wanted, List<FluidStack> available, boolean consume) {
        int[] left = new int[available.size()];
        for (int i = 0; i < left.length; i++) left[i] = available.get(i).getAmount();
        int[][] take = new int[wanted.size()][available.size()];
        for (int w = 0; w < wanted.size(); w++) {
            SizedFluidIngredient ing = wanted.get(w);
            int need = ing.amount();
            for (int i = 0; i < available.size() && need > 0; i++) {
                FluidStack s = available.get(i);
                if (left[i] <= 0 || s.isEmpty() || !ing.ingredient().test(s)) continue;
                int t = Math.min(need, left[i]);
                left[i] -= t;
                take[w][i] += t;
                need -= t;
            }
            if (need > 0) return false;
        }
        if (consume) {
            for (int[] row : take)
                for (int i = 0; i < row.length; i++)
                    if (row[i] > 0) available.get(i).shrink(row[i]);
        }
        return true;
    }

    private RecipeMatcher() {}
}
