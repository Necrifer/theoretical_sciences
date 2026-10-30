package com.theoreticalsciences.extreme;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;

/** A recipe for the 9x9 Extreme Assembly Table (Avaritia-style). */
public interface ExtremeRecipe extends Recipe<CraftingInput> {
    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return width <= 9 && height <= 9;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return ExtremeRegistry.EXTREME_TYPE.get();
    }
}
