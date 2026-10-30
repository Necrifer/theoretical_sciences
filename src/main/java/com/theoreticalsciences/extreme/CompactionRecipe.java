package com.theoreticalsciences.extreme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Singularity Compactor recipe: feed {@code count} of the ingredient (any number at a time, no waiting) and get the
 * result. Avaritia's neutronium compressor, minus the timer.
 */
public class CompactionRecipe implements Recipe<SingleRecipeInput> {
    final Ingredient ingredient;
    final int count;
    final ItemStack result;

    public CompactionRecipe(Ingredient ingredient, int count, ItemStack result) {
        this.ingredient = ingredient;
        this.count = Math.max(1, count);
        this.result = result;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public int count() {
        return count;
    }

    public ItemStack result() {
        return result;
    }

    @Override
    public boolean matches(SingleRecipeInput in, Level level) {
        return ingredient.test(in.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput in, HolderLookup.Provider regs) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.COMPACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ExtremeRegistry.COMPACTION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CompactionRecipe> {
        public static final MapCodec<CompactionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(r -> r.ingredient),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(r -> r.count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, CompactionRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, CompactionRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.ingredient,
                ByteBufCodecs.VAR_INT, r -> r.count,
                ItemStack.STREAM_CODEC, r -> r.result,
                CompactionRecipe::new);

        @Override
        public MapCodec<CompactionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompactionRecipe> streamCodec() {
            return STREAM;
        }
    }
}
