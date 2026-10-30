package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.extreme.ExtremeRecipe;
import com.theoreticalsciences.extreme.ExtremeShapedRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;

/**
 * A 9x9 Extreme Assembly Table recipe. All 81 cells are inputs (empty ones too), in grid order, so EMI's "fill the
 * grid" button puts every item exactly where it belongs.
 */
public class ExtremeEmiRecipe extends BasicEmiRecipe {
    private final boolean shapeless;

    public ExtremeEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, ExtremeRecipe recipe, ItemStack result) {
        super(cat, id, 9 * 18 + 44, 9 * 18);
        Ingredient[] cells = new Ingredient[81];
        Arrays.fill(cells, Ingredient.EMPTY);
        if (recipe instanceof ExtremeShapedRecipe s) {
            // centre the pattern in the 9x9 grid, exactly as the JEI page does
            int ox = (9 - s.width()) / 2, oy = (9 - s.height()) / 2;
            for (int i = 0; i < s.cells().size(); i++) cells[(oy + i / s.width()) * 9 + ox + i % s.width()] = s.cells().get(i);
        } else {
            var ing = recipe.getIngredients();
            for (int i = 0; i < ing.size() && i < 81; i++) cells[i] = ing.get(i);
        }
        for (Ingredient c : cells) inputs.add(QHEmi.item(c));
        outputs.add(EmiStack.of(result));
        shapeless = !(recipe instanceof ExtremeShapedRecipe);
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        for (int i = 0; i < 81; i++) w.addSlot(inputs.get(i), (i % 9) * 18, (i / 9) * 18);
        w.addTexture(EmiTexture.EMPTY_ARROW, 9 * 18 + 3, 9 * 9 - 8);
        if (shapeless) w.addTexture(EmiTexture.SHAPELESS, 9 * 18 + 26, 0);
        w.addSlot(outputs.get(0), 9 * 18 + 26, 9 * 9 - 9).recipeContext(this);
    }
}
