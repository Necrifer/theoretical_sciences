package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.extreme.ExtremeRecipe;
import com.theoreticalsciences.extreme.ExtremeRegistry;
import com.theoreticalsciences.extreme.ExtremeShapedRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** 9x9 Extreme Assembly Table recipes. Every grid cell gets a JEI slot (empty or not) so "+" transfer lines up. */
public class ExtremeCraftingCategory implements IRecipeCategory<ExtremeRecipe> {
    public static final RecipeType<ExtremeRecipe> TYPE = RecipeType.create("theoreticalsciences", "extreme_crafting", ExtremeRecipe.class);
    private final IDrawable icon;

    public ExtremeCraftingCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(ExtremeRegistry.EXTREME_TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<ExtremeRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences.extreme_assembly_table");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 9 * 18 + 44;
    }

    @Override
    public int getHeight() {
        return 9 * 18 + 2;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, ExtremeRecipe recipe, IFocusGroup focuses) {
        Ingredient[] cells = new Ingredient[81];
        java.util.Arrays.fill(cells, Ingredient.EMPTY);
        if (recipe instanceof ExtremeShapedRecipe s) {
            // centre the pattern in the 9x9 grid, exactly as a player would most naturally place it
            int ox = (9 - s.width()) / 2, oy = (9 - s.height()) / 2;
            for (int i = 0; i < s.cells().size(); i++) cells[(oy + i / s.width()) * 9 + ox + i % s.width()] = s.cells().get(i);
        } else {
            var ing = recipe.getIngredients();
            for (int i = 0; i < ing.size() && i < 81; i++) cells[i] = ing.get(i);
        }
        for (int i = 0; i < 81; i++) {
            var slot = b.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 9) * 18, 1 + (i / 9) * 18);
            if (!cells[i].isEmpty()) slot.addIngredients(cells[i]);
        }
        var level = Minecraft.getInstance().level;
        b.addSlot(RecipeIngredientRole.OUTPUT, 9 * 18 + 24, 9 * 9 - 8)
                .addItemStack(level == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess()));
        if (!(recipe instanceof ExtremeShapedRecipe)) b.setShapeless(9 * 18 + 24, 4);
    }

    @Override
    public void draw(ExtremeRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        for (int i = 0; i < 81; i++) {
            int x = (i % 9) * 18, y = (i / 9) * 18;
            g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        }
        int ax = 9 * 18 + 4, ay = 9 * 9 - 2;
        g.fill(ax, ay, ax + 14, ay + 3, 0xFFB060FF);
        for (int k = 0; k < 4; k++) g.fill(ax + 14 + k, ay - 3 + k, ax + 15 + k, ay + 6 - k, 0xFFB060FF);
        g.fill(9 * 18 + 23, 9 * 9 - 9, 9 * 18 + 41, 9 * 9 + 9, 0xFFB060FF);
        g.fill(9 * 18 + 24, 9 * 9 - 8, 9 * 18 + 40, 9 * 9 + 8, 0xFF373737);
    }
}
