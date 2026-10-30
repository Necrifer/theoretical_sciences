package com.theoreticalsciences.extreme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Gathers a collection on the Extreme Assembly Table (round 24, see {@link Collections}): one mod's items (one of
 * each) plus {@code extra} make that mod's Specimen Bundle; nine bundles plus {@code extra} make an Archive, and so
 * on. The recipe file also defines the collection: {@code "collection": "biosphere", "tags": [...]}.
 */
public class ExtremeBundleRecipe implements ExtremeRecipe {
    final String collection;
    final List<String> tags;
    final Ingredient extra;

    public ExtremeBundleRecipe(String collection, List<String> tags, Ingredient extra) {
        this.collection = collection;
        this.tags = List.copyOf(tags);
        this.extra = extra;
        Collections.define(collection, tags);
    }

    private ItemStack find(CraftingInput in) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) if (!in.getItem(i).isEmpty()) grid.add(in.getItem(i));
        if (grid.size() < 2 || grid.size() > Collections.BUNDLE + 1) return ItemStack.EMPTY;
        ItemStack probe = ItemStack.EMPTY;
        for (ItemStack g : grid) if (!extra.test(g)) { probe = g; break; }
        if (probe.isEmpty()) return ItemStack.EMPTY;
        List<Predicate<ItemStack>> need = new ArrayList<>();
        need.add(extra);
        String pid = Collections.idOf(probe);
        if (pid == null) {
            // level 1: the bundle that holds this item
            List<ItemStack> bundles = Collections.level(collection, 1);
            for (int b = 0; b < bundles.size(); b++) {
                List<Item> items = Collections.bundleItems(collection, b);
                if (!items.contains(probe.getItem())) continue;
                if (grid.size() != items.size() + 1) return ItemStack.EMPTY;
                for (Item it : items) need.add(x -> x.is(it));
                return Matching.assign(need, grid) != null ? bundles.get(b).copy() : ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
        for (int lv = 2; lv <= 8; lv++) {
            List<ItemStack> below = Collections.level(collection, lv - 1);
            if (below.size() <= 1) break;
            int at = -1;
            for (int i = 0; i < below.size(); i++) if (Collections.same(probe, below.get(i))) { at = i; break; }
            if (at < 0) continue;
            int g = at / Collections.GROUP;
            List<ItemStack> mem = below.subList(g * Collections.GROUP, Math.min(below.size(), g * Collections.GROUP + Collections.GROUP));
            if (grid.size() != mem.size() + 1) return ItemStack.EMPTY;
            for (ItemStack m : mem) need.add(x -> Collections.same(x, m));
            List<ItemStack> here = Collections.level(collection, lv);
            return Matching.assign(need, grid) != null && g < here.size() ? here.get(g).copy() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        return !find(in).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
        return find(in);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return new ItemStack(com.theoreticalsciences.registry.QHItems.get("specimen_bundle"));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    /** JEI: one page per bundle, and per archive of the levels some recipe would need (room 79: two fixed items). */
    public List<ExtremeShapelessRecipe> displays() {
        List<ExtremeShapelessRecipe> out = new ArrayList<>();
        List<ItemStack> l1 = Collections.level(collection, 1);
        for (int b = 0; b < l1.size(); b++) {
            List<Ingredient> ing = new ArrayList<>();
            for (Item it : Collections.bundleItems(collection, b)) ing.add(Ingredient.of(it));
            ing.add(extra);
            out.add(new ExtremeShapelessRecipe(ing, l1.get(b)));
        }
        int need = Collections.neededLevel(collection, 79);
        for (int lv = 2; lv <= need; lv++) {
            List<ItemStack> below = Collections.level(collection, lv - 1), here = Collections.level(collection, lv);
            for (int g = 0; g < here.size(); g++) {
                List<Ingredient> ing = new ArrayList<>();
                for (ItemStack m : below.subList(g * Collections.GROUP, Math.min(below.size(), g * Collections.GROUP + Collections.GROUP))) ing.add(Ingredient.of(m));
                ing.add(extra);
                out.add(new ExtremeShapelessRecipe(ing, here.get(g)));
            }
        }
        return out;
    }

    /** Every bundle and archive there is (for JEI's ingredient list). */
    public List<ItemStack> stacks() {
        List<ItemStack> out = new ArrayList<>();
        int need = Collections.neededLevel(collection, 79);
        for (int lv = 1; lv <= need; lv++) for (ItemStack s : Collections.level(collection, lv)) out.add(s.copy());
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.BUNDLE.get();
    }

    public static class Serializer implements RecipeSerializer<ExtremeBundleRecipe> {
        public static final MapCodec<ExtremeBundleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("collection").forGetter(r -> r.collection),
                Codec.STRING.listOf().fieldOf("tags").forGetter(r -> r.tags),
                Ingredient.CODEC_NONEMPTY.fieldOf("extra").forGetter(r -> r.extra)).apply(i, ExtremeBundleRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeBundleRecipe> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, r -> r.collection,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), r -> r.tags,
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.extra, ExtremeBundleRecipe::new);

        @Override
        public MapCodec<ExtremeBundleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtremeBundleRecipe> streamCodec() {
            return STREAM;
        }
    }
}
