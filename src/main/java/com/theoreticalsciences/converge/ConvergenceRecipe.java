package com.theoreticalsciences.converge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A Convergence Altar recipe: one item on the altar (the core), one item on each of several Resonance Pedestals around
 * it, energy poured into the altar while it works. Like Draconic fusion crafting or the Actually Additions Empowerer.
 * <pre>
 * {"type": "theoreticalsciences:convergence", "core": {"item": ...}, "pedestals": [{"item": ...}, ...],
 *  "result": {"id": ..., "count": n}, "energy": total FE, "duration": ticks, "tier": 1-3 (pedestal grade), "research": "..."}
 * </pre>
 */
public class ConvergenceRecipe implements Recipe<ConvergenceRecipe.Input> {
    public static final int MAX_PEDESTALS = 12;

    /** The altar's core and the pedestals' items (empty pedestals left out). */
    public record Input(ItemStack core, List<ItemStack> pedestals) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return i == 0 ? core : pedestals.get(i - 1);
        }

        @Override
        public int size() {
            return 1 + pedestals.size();
        }
    }

    public final Ingredient core;
    public final List<Ingredient> pedestals;
    public final ItemStack result;
    public final long energy;
    public final int duration, tier;
    public final String research;

    public ConvergenceRecipe(Ingredient core, List<Ingredient> pedestals, ItemStack result, long energy, int duration, int tier, String research) {
        this.core = core;
        this.pedestals = List.copyOf(pedestals);
        this.result = result;
        this.energy = Math.max(0, energy);
        this.duration = Math.max(1, duration);
        this.tier = Math.max(1, Math.min(3, tier));
        this.research = research;
    }

    @Override
    public boolean matches(Input in, Level level) {
        if (!core.test(in.core())) return false;
        return pedestalMatch(in.pedestals()) != null;
    }

    /** Which pedestal item goes with which ingredient (index into the stacks for each ingredient), or null. */
    public int[] pedestalMatch(List<ItemStack> stacks) {
        if (stacks.size() != pedestals.size()) return null;
        int[] used = new int[pedestals.size()];
        boolean[] taken = new boolean[stacks.size()];
        return assign(0, stacks, used, taken) ? used : null;
    }

    private boolean assign(int k, List<ItemStack> stacks, int[] used, boolean[] taken) {
        if (k == pedestals.size()) return true;
        for (int j = 0; j < stacks.size(); j++) {
            if (taken[j] || !pedestals.get(k).test(stacks.get(j))) continue;
            taken[j] = true;
            used[k] = j;
            if (assign(k + 1, stacks, used, taken)) return true;
            taken[j] = false;
        }
        return false;
    }

    @Override
    public ItemStack assemble(Input in, HolderLookup.Provider regs) {
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
        NonNullList<Ingredient> l = NonNullList.create();
        l.add(core);
        l.addAll(pedestals);
        return l;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public boolean hasResearch() {
        return research != null && !research.isEmpty();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ConvergenceRegistry.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ConvergenceRegistry.TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ConvergenceRecipe> {
        public static final MapCodec<ConvergenceRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("core").forGetter(r -> r.core),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("pedestals").forGetter(r -> r.pedestals),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.LONG.optionalFieldOf("energy", 0L).forGetter(r -> r.energy),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("duration", 200).forGetter(r -> r.duration),
                Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(r -> r.tier),
                Codec.STRING.optionalFieldOf("research", "").forGetter(r -> r.research)
        ).apply(i, (c, p, res, e, d, t, rs) -> new ConvergenceRecipe(c, p.subList(0, Math.min(MAX_PEDESTALS, p.size())), res, e, d, t, rs)));
        public static final StreamCodec<RegistryFriendlyByteBuf, ConvergenceRecipe> STREAM = StreamCodec.of((buf, r) -> {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.core);
            buf.writeVarInt(r.pedestals.size());
            for (Ingredient g : r.pedestals) Ingredient.CONTENTS_STREAM_CODEC.encode(buf, g);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
            buf.writeVarLong(r.energy);
            buf.writeVarInt(r.duration);
            buf.writeVarInt(r.tier);
            buf.writeUtf(r.research);
        }, buf -> {
            Ingredient c = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            int n = buf.readVarInt();
            List<Ingredient> p = new ArrayList<>();
            for (int k = 0; k < n; k++) p.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            ItemStack res = ItemStack.STREAM_CODEC.decode(buf);
            long e = buf.readVarLong();
            int d = buf.readVarInt(), t = buf.readVarInt();
            return new ConvergenceRecipe(c, p, res, e, d, t, buf.readUtf());
        });

        @Override
        public MapCodec<ConvergenceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConvergenceRecipe> streamCodec() {
            return STREAM;
        }
    }
}
