package com.theoreticalsciences.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

/**
 * One EMI recipe tab. EMI would normally look its name up under "emi.category.&lt;id&gt;"; we hand it the same
 * translation the JEI tab uses instead, so no new lang keys are needed for the tab names.
 */
public class QHEmiCategory extends EmiRecipeCategory {
    private final Component name;

    public QHEmiCategory(ResourceLocation id, ItemLike icon, Component name) {
        super(id, EmiStack.of(icon));
        this.name = name;
    }

    @Override
    public Component getName() {
        return name;
    }
}
