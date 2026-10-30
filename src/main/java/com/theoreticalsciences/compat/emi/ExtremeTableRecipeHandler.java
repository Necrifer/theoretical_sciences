package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.extreme.ExtremeTableMenu;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets EMI's "+" button fill the Extreme Assembly Table's 9x9 grid. The table's slots are: 0-80 the grid (row by
 * row), 81 the result, 82-117 the player's inventory - the same numbers the JEI transfer handler uses.
 */
public class ExtremeTableRecipeHandler implements StandardRecipeHandler<ExtremeTableMenu> {
    private static final int GRID = 81, RESULT = 81, INV = 82, INV_SIZE = 36;

    @Override
    public List<Slot> getInputSources(ExtremeTableMenu menu) {
        List<Slot> out = new ArrayList<>(menu.slots.subList(0, GRID));
        out.addAll(menu.slots.subList(INV, Math.min(menu.slots.size(), INV + INV_SIZE)));
        return out;
    }

    @Override
    public List<Slot> getCraftingSlots(ExtremeTableMenu menu) {
        return new ArrayList<>(menu.slots.subList(0, GRID));
    }

    @Override
    public Slot getOutputSlot(ExtremeTableMenu menu) {
        return menu.slots.get(RESULT);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == QHEmiPlugin.EXTREME && recipe.getInputs().size() == GRID;
    }
}
