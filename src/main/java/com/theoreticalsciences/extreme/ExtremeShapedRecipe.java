package com.theoreticalsciences.extreme;

import com.mojang.serialization.Codec;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shaped recipe up to 9x9. Matches anywhere in the grid, and mirrored. */
public class ExtremeShapedRecipe implements ExtremeRecipe {
    final int width, height;
    final NonNullList<Ingredient> cells;    // row-major, Ingredient.EMPTY for a blank cell
    final ItemStack result;
    final List<String> pattern;
    final Map<String, Ingredient> key;

    public ExtremeShapedRecipe(List<String> pattern, Map<String, Ingredient> key, ItemStack result) {
        this.pattern = List.copyOf(pattern);
        this.key = Map.copyOf(key);
        this.result = result;
        this.height = pattern.size();
        this.width = pattern.stream().mapToInt(String::length).max().orElse(0);
        this.cells = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++) {
            String row = pattern.get(y);
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != ' ') cells.set(y * width + x, key.getOrDefault(String.valueOf(c), Ingredient.EMPTY));
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public NonNullList<Ingredient> cells() {
        return cells;
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        if (in.width() != width || in.height() != height) return false;
        return test(in, false) || test(in, true);
    }

    private boolean test(CraftingInput in, boolean mirror) {
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                Ingredient ing = cells.get(y * width + (mirror ? width - 1 - x : x));
                ItemStack s = in.getItem(x, y);
                if (ing.isEmpty() ? !s.isEmpty() : !ing.test(s)) return false;
            }
        return true;
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
        return cells;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.SHAPED.get();
    }

    public static class Serializer implements RecipeSerializer<ExtremeShapedRecipe> {
        private static final Codec<List<String>> PATTERN = Codec.STRING.listOf().flatXmap(l -> l.size() <= 9 && l.stream().allMatch(s -> s.length() <= 9)
                ? DataResult.success(l) : DataResult.error(() -> "Extreme patterns are at most 9x9"), DataResult::success);
        public static final MapCodec<ExtremeShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                PATTERN.fieldOf("pattern").forGetter(r -> r.pattern),
                Codec.unboundedMap(Codec.STRING, Ingredient.CODEC_NONEMPTY).fieldOf("key").forGetter(r -> r.key),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, ExtremeShapedRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeShapedRecipe> STREAM = StreamCodec.of((buf, r) -> {
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, r.pattern);
            buf.writeVarInt(r.key.size());
            r.key.forEach((k, v) -> {
                buf.writeUtf(k);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, v);
            });
            ItemStack.STREAM_CODEC.encode(buf, r.result);
        }, buf -> {
            List<String> p = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);
            int n = buf.readVarInt();
            Map<String, Ingredient> k = new java.util.HashMap<>();
            for (int j = 0; j < n; j++) k.put(buf.readUtf(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new ExtremeShapedRecipe(new ArrayList<>(p), k, ItemStack.STREAM_CODEC.decode(buf));
        });

        @Override
        public MapCodec<ExtremeShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtremeShapedRecipe> streamCodec() {
            return STREAM;
        }
    }
}
