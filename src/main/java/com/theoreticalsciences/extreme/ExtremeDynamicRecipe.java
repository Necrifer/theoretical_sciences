package com.theoreticalsciences.extreme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An Extreme Assembly Table recipe whose ingredients follow the game it is in (round 21).
 * <ul>
 *   <li>{@code ingredients}: always needed;</li>
 *   <li>{@code singularities: true}: one of every singularity a pack added or the mod made from an ingot - or, when they
 *   do not fit any more, their Singularity Clusters (the Omniscience Catalyst);</li>
 *   <li>{@code tags}: the rest of the grid filled with different items from these tags - every crop, every meat, every
 *   mob drop of every installed mod (the Primordial Harvest, the Biosphere Compendium).</li>
 * </ul>
 */
public class ExtremeDynamicRecipe implements ExtremeRecipe {
    final List<Ingredient> fixed;
    final List<String> tags;
    final boolean singularities;
    final ItemStack result;
    /** Round 24: a collection (see {@link Collections}) - one Specimen Bundle per mod, or archives of them. */
    final String collection;

    public ExtremeDynamicRecipe(List<Ingredient> fixed, List<String> tags, boolean singularities, ItemStack result, String collection) {
        this.fixed = List.copyOf(fixed);
        this.tags = List.copyOf(tags);
        this.singularities = singularities;
        this.result = result;
        this.collection = collection;
    }

    /** The bundles (or archives) this game's collection needs, in the room left. */
    List<ItemStack> collectionStacks(int room) {
        return collection.isEmpty() ? List.of() : Collections.requirements(collection, room);
    }

    private record Members(int version, List<Item> items) {}

    /** One reference, so a thread never sees a new version with an old list (singleplayer shares this object). */
    private volatile Members membersCache;

    private static boolean client(Level level) {
        return level != null && level.isClientSide;
    }

    /** Different items from the tags (sorted by id), as many as fit. */
    List<Item> members(int room) {
        Set<Item> out = new LinkedHashSet<>();
        for (String t : tags) {
            ResourceLocation r = ResourceLocation.tryParse(t.replace("#", ""));
            if (r == null) continue;
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, r))) out.add(h.value());
        }
        List<Item> l = new ArrayList<>(out);
        l.sort(java.util.Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return l.subList(0, Math.min(l.size(), Math.max(0, room)));
    }

    List<ItemStack> singularityStacks(boolean clientSide) {
        return singularities ? Singularities.requirements(Math.max(1, 81 - fixed.size()), clientSide) : List.of();
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) if (!in.getItem(i).isEmpty()) grid.add(in.getItem(i));
        if (grid.size() < fixed.size()) return false;
        List<ItemStack> sing = new ArrayList<>(singularityStacks(client(level)));
        sing.addAll(collectionStacks(81 - fixed.size() - sing.size()));
        int room = 81 - fixed.size() - sing.size();
        List<Item> all = tags.isEmpty() ? List.of() : allMembers();
        int fill = Math.min(all.size(), Math.max(0, room));
        if (grid.size() != fixed.size() + sing.size() + fill) return false;
        List<Predicate<ItemStack>> need = new ArrayList<>();
        for (Ingredient g : fixed) need.add(g);
        for (ItemStack s : sing) need.add(x -> Singularities.same(x, s) || Collections.same(x, s));
        int[] match = Matching.assign(need, grid);
        if (match == null) return false;
        // what is left: different items, every one from the tags
        boolean[] used = new boolean[grid.size()];
        for (int m : match) used[m] = true;
        Set<Item> members = new HashSet<>(all), seen = new HashSet<>();
        for (int i = 0; i < grid.size(); i++) {
            if (used[i]) continue;
            Item it = grid.get(i).getItem();
            if (!members.contains(it) || !seen.add(it)) return false;
        }
        return seen.size() == fill;
    }

    /** Tag contents only change on a reload; work the sorted list out once per reload. */
    private List<Item> allMembers() {
        int v = Singularities.tagVersion() * 31 + Collections.version();
        Members m = membersCache;
        if (m == null || m.version() != v) {
            m = new Members(v, List.copyOf(members(Integer.MAX_VALUE)));
            membersCache = m;
        }
        return m.items();
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
        return result.copy();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return result;
    }

    /** For JEI: what it asks for in this game. */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> l = NonNullList.create();
        l.addAll(fixed);
        boolean c = net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
        List<ItemStack> sing = new ArrayList<>(singularityStacks(c));
        sing.addAll(collectionStacks(81 - fixed.size() - sing.size()));
        for (ItemStack s : sing) l.add(Ingredient.of(s));
        int room = 81 - fixed.size() - sing.size();
        List<Item> all = allMembers();
        for (Item it : all.subList(0, Math.min(all.size(), Math.max(0, room)))) l.add(Ingredient.of(it));
        return l;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.DYNAMIC.get();
    }

    public static class Serializer implements RecipeSerializer<ExtremeDynamicRecipe> {
        public static final MapCodec<ExtremeDynamicRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.listOf().optionalFieldOf("ingredients", List.of()).forGetter(r -> r.fixed),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(r -> r.tags),
                Codec.BOOL.optionalFieldOf("singularities", false).forGetter(r -> r.singularities),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.STRING.optionalFieldOf("collection", "").forGetter(r -> r.collection)
        ).apply(i, ExtremeDynamicRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeDynamicRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.fixed,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), r -> r.tags,
                ByteBufCodecs.BOOL, r -> r.singularities,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.STRING_UTF8, r -> r.collection,
                ExtremeDynamicRecipe::new);

        @Override
        public MapCodec<ExtremeDynamicRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtremeDynamicRecipe> streamCodec() {
            return STREAM;
        }
    }
}
