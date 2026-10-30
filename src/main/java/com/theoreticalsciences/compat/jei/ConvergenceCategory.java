package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.converge.ConvergenceRecipe;
import com.theoreticalsciences.registry.QHItems;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** The Convergence Altar: the core in the middle, the pedestals' items in a ring around it. */
public class ConvergenceCategory implements IRecipeCategory<ConvergenceRecipe> {
    public static final RecipeType<ConvergenceRecipe> TYPE = RecipeType.create("theoreticalsciences", "convergence", ConvergenceRecipe.class);
    private static final int CX = 56, CY = 52, R = 40;
    private final IDrawable icon;

    public ConvergenceCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(QHItems.get("convergence_altar")));
    }

    @Override
    public RecipeType<ConvergenceRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences.convergence_altar");
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
        return 118;
    }

    private static int[] ring(int i, int n) {
        double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, n);
        return new int[]{CX + (int) Math.round(Math.cos(a) * R) - 8, CY + (int) Math.round(Math.sin(a) * R) - 8};
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, ConvergenceRecipe r, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, CX - 8, CY - 8).addIngredients(r.core)
                .addRichTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.theoreticalsciences.altar_core")));
        int n = r.pedestals.size();
        for (int i = 0; i < n; i++) {
            int[] p = ring(i, n);
            b.addSlot(RecipeIngredientRole.INPUT, p[0], p[1]).addIngredients(r.pedestals.get(i))
                    .addRichTooltipCallback((v, tip) -> tip.add(Component.translatable("jei.theoreticalsciences.altar_pedestal")));
        }
        b.addSlot(RecipeIngredientRole.OUTPUT, 140, CY - 8).addItemStack(r.result);
        b.addSlot(RecipeIngredientRole.CATALYST, 140, 4).addItemStack(new ItemStack(QHItems.get(
                r.tier >= 3 ? "absolute_pedestal" : r.tier == 2 ? "harmonic_pedestal" : "resonance_pedestal")));
    }

    @Override
    public void draw(ConvergenceRecipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        int n = r.pedestals.size();
        for (int i = 0; i < n; i++) {
            int[] p = ring(i, n);
            line(g, p[0] + 8, p[1] + 8, CX, CY, 0x60B080FF);
        }
        for (int i = 0; i < n; i++) {
            int[] p = ring(i, n);
            g.fill(p[0] - 1, p[1] - 1, p[0] + 17, p[1] + 17, 0xFF8B8B8B);
            g.fill(p[0], p[1], p[0] + 16, p[1] + 16, 0xFF373737);
        }
        g.fill(CX - 10, CY - 10, CX + 10, CY + 10, 0xFFB080FF);
        g.fill(CX - 9, CY - 9, CX + 9, CY + 9, 0xFF2A1A40);
        g.fill(139, CY - 9, 157, CY + 9, 0xFF8B8B8B);
        g.fill(140, CY - 8, 156, CY + 8, 0xFF373737);
        g.fill(110, CY - 1, 136, CY + 1, 0xFFB060FF);
        var font = Minecraft.getInstance().font;
        g.drawString(font, fe(r.energy), 104, CY + 16, 0xFFC04040, false);
        g.drawString(font, String.format("%.1fs", r.duration / 20.0), 104, CY + 26, 0xFF606878, false);
        g.drawString(font, Component.translatable("jei.theoreticalsciences.altar_tier", r.tier), 100, 24, 0xFF6020A0, false);
        if (r.hasResearch()) {
            ResourceLocation id = ResourceLocation.tryParse(r.research);
            Component c = Component.translatable("jei.theoreticalsciences.research",
                    id == null ? Component.literal(r.research) : Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath(), id.getPath()));
            g.drawString(font, font.plainSubstrByWidth(c.getString(), 168), 0, 108, 0xFF9060C0, false);
        }
    }

    private static String fe(long e) {
        String[] u = {"", "k", "M", "G", "T", "P"};
        double v = e;
        int i = 0;
        while (v >= 1000 && i < u.length - 1) {
            v /= 1000;
            i++;
        }
        return (i == 0 ? String.valueOf(e) : String.format("%.1f %s", v, u[i])) + "FE";
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 0; s <= steps; s += 2) {
            int x = x0 + (x1 - x0) * s / Math.max(1, steps), y = y0 + (y1 - y0) * s / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }
}
