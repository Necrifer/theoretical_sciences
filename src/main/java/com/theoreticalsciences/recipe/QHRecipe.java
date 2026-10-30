package com.theoreticalsciences.recipe;

import com.theoreticalsciences.registry.QHRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

/**
 * Shared recipe format for every Theoretical Sciences machine.
 * <pre>
 * {
 *   "type": "theoreticalsciences:entangling",
 *   "item_inputs":   [ {"item": "minecraft:amethyst_shard", "count": 1}, {"tag": "c:gems", "count": 2} ],
 *   "fluid_inputs":  [ {"fluid": "theoreticalsciences:superfluid_helium", "amount": 250} ],
 *   "item_outputs":  [ {"id": "theoreticalsciences:qubit", "count": 2} ],
 *   "fluid_outputs": [ {"id": "theoreticalsciences:dark_matter", "amount": 100} ],
 *   "energy": 512,          // FE per tick (consumed, or produced for generator machines)
 *   "duration": 200,        // ticks
 *   "research": "theoreticalsciences:quantum_entanglement"   // optional
 * }
 * </pre>
 */
public class QHRecipe implements Recipe<QHRecipeInput> {
    private final String typeId;
    public final List<SizedIngredient> itemInputs;
    public final List<SizedFluidIngredient> fluidInputs;
    public final List<ItemStack> itemOutputs;
    public final List<FluidStack> fluidOutputs;
    public final int energy;
    public final int duration;
    public final String research;
    /** Items that must be present in the input slots but are never consumed (GregTech-style catalysts / moulds / lenses). */
    public final List<Ingredient> catalysts;
    /** Per item output chance (0..1). Missing entries mean 100%. Used for byproducts. */
    public final List<Float> chances;
    /** Lowest coil tier a multiblock needs to run this recipe (0 = any). GT "coil heat" style gating. */
    public final int minTier;
    /** An impossible recipe: hidden behind the Omnilex Engine's secret overclock, asks for confirmation, has no output. */
    public boolean secret;
    /** Insight granted to the machine's owner each time this recipe finishes (a "thought experiment"). */
    public int insight;
    /** Physical conditions needed around the controller (gravity, dimensions, time, vacuum). */
    public com.theoreticalsciences.field.Environment environment = com.theoreticalsciences.field.Environment.ANY;
    /** QMD-style operating window: the particle / feed this recipe works with and where the machine must be run. */
    public com.theoreticalsciences.machine.physics.RecipePhysics physics = com.theoreticalsciences.machine.physics.RecipePhysics.NONE;
    private final List<SizedIngredient> matchList;

    public QHRecipe(String typeId, List<SizedIngredient> itemInputs, List<SizedFluidIngredient> fluidInputs,
                    List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int energy, int duration, String research) {
        this(typeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, energy, duration, research, List.of(), List.of());
    }

    public QHRecipe(String typeId, List<SizedIngredient> itemInputs, List<SizedFluidIngredient> fluidInputs,
                    List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int energy, int duration, String research,
                    List<Ingredient> catalysts, List<Float> chances) {
        this(typeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, energy, duration, research, catalysts, chances, 0);
    }

    public QHRecipe(String typeId, List<SizedIngredient> itemInputs, List<SizedFluidIngredient> fluidInputs,
                    List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, int energy, int duration, String research,
                    List<Ingredient> catalysts, List<Float> chances, int minTier) {
        this.minTier = Math.max(0, minTier);
        this.catalysts = List.copyOf(catalysts);
        this.chances = List.copyOf(chances);
        java.util.ArrayList<SizedIngredient> all = new java.util.ArrayList<>(itemInputs);
        for (Ingredient c : catalysts) all.add(new SizedIngredient(c, 1));
        this.matchList = List.copyOf(all);
        this.typeId = typeId;
        this.itemInputs = List.copyOf(itemInputs);
        this.fluidInputs = List.copyOf(fluidInputs);
        this.itemOutputs = itemOutputs.stream().map(ItemStack::copy).toList();
        this.fluidOutputs = fluidOutputs.stream().map(FluidStack::copy).toList();
        this.energy = energy;
        this.duration = Math.max(1, duration);
        this.research = research == null ? "" : research;
    }

    public float chance(int outputIndex) {
        return outputIndex < chances.size() ? chances.get(outputIndex) : 1.0F;
    }

    public String typeId() {
        return typeId;
    }

    public boolean hasResearch() {
        return !research.isEmpty();
    }

    @Override
    public boolean matches(QHRecipeInput input, Level level) {
        return RecipeMatcher.matchItems(matchList, input.items(), false) && RecipeMatcher.matchFluids(fluidInputs, input.fluids(), false);
    }

    @Override
    public ItemStack assemble(QHRecipeInput input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return itemOutputs.isEmpty() ? ItemStack.EMPTY : itemOutputs.get(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        itemInputs.forEach(s -> list.add(s.ingredient()));
        list.addAll(catalysts);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true; // keeps the vanilla recipe book from complaining about unknown categories
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return QHRecipes.serializer(typeId);
    }

    @Override
    public RecipeType<?> getType() {
        return QHRecipes.type(typeId);
    }
}
