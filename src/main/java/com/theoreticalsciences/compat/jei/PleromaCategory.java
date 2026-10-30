package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.transcend.ConfluenceBlockEntity;
import com.theoreticalsciences.transcend.TranscendRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** The Pleroma Confluence's one "recipe": a million mB of every other fluid -> a thousand mB of Pleroma. */
public class PleromaCategory implements IRecipeCategory<PleromaCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create("theoreticalsciences", "pleroma", Recipe.class);

    public record Recipe(List<FluidStack> inputs) {
        public static Recipe build() {
            return new Recipe(ConfluenceBlockEntity.wanted());
        }
    }

    private final IDrawable icon;

    public PleromaCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(TranscendRegistry.CONFLUENCE_ITEM.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences.pleroma_confluence");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        List<FluidStack> w = ConfluenceBlockEntity.wanted();
        int e = essences(w);
        return Math.max(112, essenceTop(w.size() - e) + ((e + 7) / 8) * 18 + 2);
    }

    /** How many of the wanted fluids are mod Essences (they come last). */
    private static int essences(List<FluidStack> w) {
        int n = 0;
        for (FluidStack f : w) if (com.theoreticalsciences.transcend.Essences.isEssence(f)) n++;
        return n;
    }

    /** Where the Essences' rows start: under this mod's fluids and their own heading. */
    private static int essenceTop(int plain) {
        return 12 + ((plain + 7) / 8) * 18 + 12;
    }

    private static int[] pos(int i, int plain) {
        if (i < plain) return new int[]{1 + (i % 8) * 18, 12 + (i / 8) * 18};
        int j = i - plain;
        return new int[]{1 + (j % 8) * 18, essenceTop(plain) + (j / 8) * 18};
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Recipe r, IFocusGroup focuses) {
        int plain = r.inputs().size() - essences(r.inputs());
        for (int i = 0; i < r.inputs().size(); i++)
            b.addSlot(RecipeIngredientRole.INPUT, pos(i, plain)[0], pos(i, plain)[1]).setStandardSlotBackground()
                    .setFluidRenderer(ConfluenceBlockEntity.target(r.inputs().get(i)), false, 16, 16)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, r.inputs().get(i).copyWithAmount(ConfluenceBlockEntity.target(r.inputs().get(i))));
        b.addSlot(RecipeIngredientRole.OUTPUT, 156, 40).setStandardSlotBackground()
                .setFluidRenderer(ConfluenceBlockEntity.BATCH, false, 16, 16)
                .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(QHFluids.ALL.get("pleroma").source.get(), ConfluenceBlockEntity.BATCH));
    }

    @Override
    public void draw(Recipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        int e = essences(r.inputs()), plain = r.inputs().size() - e;
        g.drawString(font, Component.translatable("jei.theoreticalsciences.pleroma_each", String.format("%,d", ConfluenceBlockEntity.TARGET)), 1, 0, 0xFF606060, false);
        if (e > 0) g.drawString(font, Component.translatable("jei.theoreticalsciences.pleroma_essences", e, String.format("%,d", ConfluenceBlockEntity.ESSENCE_TARGET)),
                1, essenceTop(plain) - 10, 0xFF606060, false);
        g.fill(148, 47, 154, 49, 0xFFC080FF);
    }
}
