package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.extreme.CompactionRecipe;
import com.theoreticalsciences.extreme.ExtremeRegistry;
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

public class CompactionCategory implements IRecipeCategory<CompactionRecipe> {
    public static final RecipeType<CompactionRecipe> TYPE = RecipeType.create("theoreticalsciences", "compaction", CompactionRecipe.class);
    private final IDrawable icon;

    public CompactionCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(ExtremeRegistry.COMPACTOR_ITEM.get()));
    }

    @Override
    public RecipeType<CompactionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences.singularity_compactor");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 52;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, CompactionRecipe r, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 10, 12).addIngredients(r.ingredient())
                .addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("gui.theoreticalsciences.compactor_needs", String.format("%,d", r.count()))));
        var level = Minecraft.getInstance().level;
        b.addSlot(RecipeIngredientRole.OUTPUT, 124, 12).addItemStack(level == null ? r.result() : r.getResultItem(level.registryAccess()));
    }

    @Override
    public void draw(CompactionRecipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        for (int x : new int[]{9, 123}) {
            g.fill(x, 11, x + 18, 29, 0xFF8B8B8B);
            g.fill(x + 1, 12, x + 17, 28, 0xFF373737);
        }
        g.fill(34, 18, 112, 22, 0xFFB060FF);
        for (int k = 0; k < 4; k++) g.fill(112 + k, 15 + k, 113 + k, 25 - k, 0xFFB060FF);
        var font = Minecraft.getInstance().font;
        String n = "× " + String.format("%,d", r.count());
        g.drawString(font, n, 73 - font.width(n) / 2, 5, 0xFF6020A0, false);
        // round 35: wrapped to the page instead of running off its edge
        com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.compactor_instant"), 75, 32, 146, 0xFF606060, 2, true);
    }
}
