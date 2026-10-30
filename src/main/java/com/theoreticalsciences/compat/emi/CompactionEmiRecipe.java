package com.theoreticalsciences.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Singularity Compactor: a great many of one item -> a singularity, instantly. */
public class CompactionEmiRecipe extends BasicEmiRecipe {
    private final int count;

    public CompactionEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, Ingredient ingredient, int count, ItemStack result) {
        super(cat, id, 150, 52);
        this.count = count;
        inputs.add(QHEmi.item(ingredient, count));
        outputs.add(EmiStack.of(result));
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        var font = Minecraft.getInstance().font;
        w.addDrawable(0, 0, 150, 52, (g, mx, my, d) -> {
            g.fill(34, 18, 112, 22, 0xFFB060FF);
            for (int k = 0; k < 4; k++) g.fill(112 + k, 15 + k, 113 + k, 25 - k, 0xFFB060FF);
            String n = "× " + String.format("%,d", count);
            g.drawString(font, n, 73 - font.width(n) / 2, 5, 0xFF6020A0, false);
            // round 35: wrapped to the page instead of running off its edge
            com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.compactor_instant"), 75, 32, 146, 0xFF606060, 2, true);
        });
        w.addSlot(inputs.get(0), 9, 11)
                .appendTooltip(Component.translatable("gui.theoreticalsciences.compactor_needs", String.format("%,d", count)));
        w.addSlot(outputs.get(0), 123, 11).recipeContext(this);
    }
}
