package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.home.EmpyreanRecipe;
import com.theoreticalsciences.home.HomeClient;
import com.theoreticalsciences.home.HomeRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 15 by 15 Empyrean Assembly Table recipes (1.8.0). The picture is drawn at twelve pixels a slot, so the page is no
 * larger than the 9 by 9 one, and only the slots the picture uses exist. How many each slot takes is in its tooltip.
 */
public class EmpyreanCategory implements IRecipeCategory<EmpyreanRecipe> {
    public static final RecipeType<EmpyreanRecipe> TYPE = RecipeType.create("theoreticalsciences", "empyrean", EmpyreanRecipe.class);
    private static final int CELL = 12, N = EmpyreanRecipe.SIZE;
    private final IDrawable icon;

    /** An item drawn at three quarters size. */
    private static final IIngredientRenderer<ItemStack> SMALL = new IIngredientRenderer<>() {
        @Override
        public void render(GuiGraphics g, ItemStack st) {
            g.pose().pushPose();
            g.pose().scale(0.6875F, 0.6875F, 1F);
            g.renderItem(st, 0, 0);
            g.pose().popPose();
        }

        @SuppressWarnings("removal")
        @Override
        public List<Component> getTooltip(ItemStack st, TooltipFlag flag) {
            return Screen.getTooltipFromItem(Minecraft.getInstance(), st);
        }

        @Override
        public int getWidth() {
            return CELL - 1;
        }

        @Override
        public int getHeight() {
            return CELL - 1;
        }
    };

    public EmpyreanCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(HomeRegistry.TABLE_ITEM.get()));
    }

    @Override
    public RecipeType<EmpyreanRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences.empyrean_assembly_table");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return N * CELL + 46;
    }

    @Override
    public int getHeight() {
        return N * CELL + 2;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, EmpyreanRecipe recipe, IFocusGroup focuses) {
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                EmpyreanRecipe.Slot s = recipe.cell(x, y);
                if (s == null) continue;
                long total = recipe.totals().get(s.item());
                b.addSlot(RecipeIngredientRole.INPUT, 1 + x * CELL, 1 + y * CELL)
                        .setCustomRenderer(VanillaTypes.ITEM_STACK, SMALL)
                        .addItemStack(new ItemStack(s.item()))
                        .addRichTooltipCallback((view, tip) -> {
                            tip.add(Component.translatable("gui.theoreticalsciences.empyrean.tip_slot", HomeClient.full(s.count())).withStyle(ChatFormatting.GOLD));
                            tip.add(Component.translatable("gui.theoreticalsciences.empyrean.tip_recipe_total", HomeClient.full(total)).withStyle(ChatFormatting.GRAY));
                        });
            }
        b.addSlot(RecipeIngredientRole.OUTPUT, N * CELL + 26, N * CELL / 2 - 8)
                .addItemStack(new ItemStack(recipe.result.item()))
                .addRichTooltipCallback((view, tip) -> tip.add(Component.literal("x" + HomeClient.full(recipe.result.count())).withStyle(ChatFormatting.GOLD)));
    }

    @Override
    public void draw(EmpyreanRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        g.fill(0, 0, N * CELL + 1, N * CELL + 1, 0xFF6A5A38);
        for (int i = 0; i < N * N; i++) {
            int x = 1 + (i % N) * CELL, y = 1 + (i / N) * CELL;
            g.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF221A0C);
        }
        int ax = N * CELL + 5, ay = N * CELL / 2 - 2;
        g.fill(ax, ay, ax + 12, ay + 3, 0xFFFFE9A8);
        for (int k = 0; k < 4; k++) g.fill(ax + 12 + k, ay - 3 + k, ax + 13 + k, ay + 6 - k, 0xFFFFE9A8);
        g.fill(N * CELL + 25, N * CELL / 2 - 9, N * CELL + 43, N * CELL / 2 + 9, 0xFFFFE9A8);
        g.fill(N * CELL + 26, N * CELL / 2 - 8, N * CELL + 42, N * CELL / 2 + 8, 0xFF221A0C);
        var font = Minecraft.getInstance().font;
        String n = "x" + HomeClient.compact(recipe.result.count());
        g.drawString(font, n, N * CELL + 34 - font.width(n) / 2, N * CELL / 2 + 13, 0xFFB89C5C, false);
        if (recipe.needsHome) {
            int ty = N * CELL / 2 + 26;
            for (var line : font.split(Component.translatable("gui.theoreticalsciences.empyrean.needs_home_short"), 44)) {
                g.drawString(font, line, N * CELL + 3, ty, 0xFFFF9A6A, false);
                ty += 9;
            }
        }
    }
}
