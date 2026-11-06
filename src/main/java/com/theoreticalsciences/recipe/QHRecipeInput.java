package com.theoreticalsciences.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Snapshot of a machine's input slots and input tanks - and (1.2.0) what sits in its catalyst slot. A catalyst that
 * wears (see science/Wear) only counts from the catalyst slot; one that never wears also counts from an input slot.
 */
public record QHRecipeInput(List<ItemStack> items, List<FluidStack> fluids, ItemStack catalyst) implements RecipeInput {
    public QHRecipeInput(List<ItemStack> items, List<FluidStack> fluids) {
        this(items, fluids, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return catalyst.isEmpty() && items.stream().allMatch(ItemStack::isEmpty) && fluids.stream().allMatch(FluidStack::isEmpty);
    }
}
