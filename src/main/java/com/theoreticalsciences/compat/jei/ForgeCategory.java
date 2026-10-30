package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.transcend.CoreForge;
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

/** The rebuilt Omnipotence Core's Forge, drawn as a page of its terminal. */
public class ForgeCategory implements IRecipeCategory<CoreForge.Recipe> {
    public static final RecipeType<CoreForge.Recipe> TYPE = RecipeType.create("theoreticalsciences", "omnipotence_forge", CoreForge.Recipe.class);
    private static final int GREEN = 0xFF5CFF8A, DIM = 0xFF2F8A4C;
    /** Up to 20 ingredients: five across, four down. */
    private static final int COLS = 5;
    private final IDrawable icon;

    public ForgeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(QHItems.get("eschaton_core")));
    }

    @Override
    public RecipeType<CoreForge.Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.theoreticalsciences.forge");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 170;
    }

    @Override
    public int getHeight() {
        return 148;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, CoreForge.Recipe r, IFocusGroup focuses) {
        int i = 0;
        for (CoreForge.Ing in : r.in()) {
            int x = 6 + (i % COLS) * 24, y = 18 + (i / COLS) * 26;
            var slot = b.addSlot(in.count() == 0 ? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(in.stack(Math.max(1, Math.min(99, in.count()))));
            final int need = in.count();
            slot.addRichTooltipCallback((v, tip) -> tip.add(need == 0 ? Component.translatable("jei.theoreticalsciences.not_consumed")
                    : Component.literal(String.format("× %,d", need))));
            i++;
        }
        b.addSlot(RecipeIngredientRole.OUTPUT, 146, 40).addItemStack(r.result());
    }

    @Override
    public void draw(CoreForge.Recipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        g.fill(0, 0, getWidth(), getHeight(), 0xFF020A05);
        for (int y = 0; y < getHeight(); y += 2) g.fill(0, y, getWidth(), y + 1, 0x18000000);
        g.renderOutline(0, 0, getWidth(), getHeight(), DIM);
        var font = Minecraft.getInstance().font;
        g.drawString(font, font.plainSubstrByWidth("> forge " + r.id(), getWidth() - 8), 4, 4, GREEN, false);
        int i = 0;
        for (CoreForge.Ing in : r.in()) {
            int x = 6 + (i % COLS) * 24, y = 18 + (i / COLS) * 26;
            g.renderOutline(x - 1, y - 1, 18, 18, DIM);
            String n = in.count() == 0 ? "kept" : in.count() > 99 ? com.theoreticalsciences.compat.emi.ShortCount.of(in.count()) : "";
            if (!n.isEmpty()) g.drawString(font, n, x + 8 - font.width(n) / 2, y + 17, DIM, false);
            i++;
        }
        g.renderOutline(145, 39, 18, 18, GREEN);
        g.fill(128, 47, 142, 48, GREEN);
        com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable(com.theoreticalsciences.transcend.ForgeDrift.drifts(r.id()) ? "jei.theoreticalsciences.forge_laws_drift" : "jei.theoreticalsciences.forge_laws"), 4, getHeight() - 22, getWidth() - 8, DIM, 2, false);
    }
}
