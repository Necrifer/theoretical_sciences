package com.theoreticalsciences.extreme;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

/** Shapeless recipe of up to 81 ingredients (one item per grid cell). */
public class ExtremeShapelessRecipe implements ExtremeRecipe {
    final NonNullList<Ingredient> ingredients;
    final ItemStack result;

    public ExtremeShapelessRecipe(List<Ingredient> ingredients, ItemStack result) {
        this.ingredients = NonNullList.copyOf(ingredients);
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        if (in.ingredientCount() != ingredients.size()) return false;
        return in.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.SHAPELESS.get();
    }

    public static class Serializer implements RecipeSerializer<ExtremeShapelessRecipe> {
        public static final MapCodec<ExtremeShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.listOf().flatXmap(l -> l.isEmpty() || l.size() > 81
                        ? DataResult.error(() -> "1 to 81 ingredients") : DataResult.success(l), DataResult::success)
                        .fieldOf("ingredients").forGetter(r -> r.ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, ExtremeShapelessRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeShapelessRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
                ItemStack.STREAM_CODEC, r -> r.result,
                ExtremeShapelessRecipe::new);

        @Override
        public MapCodec<ExtremeShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtremeShapelessRecipe> streamCodec() {
            return STREAM;
        }
    }
}
