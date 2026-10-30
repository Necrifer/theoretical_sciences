package com.theoreticalsciences.extreme;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Condenses singularities into a Singularity Cluster (and clusters into hyperclusters) on the Extreme Assembly Table:
 * the cluster's members (up to nine) plus the {@code extra} ingredient. Only exists when there are more singularities
 * than the Omniscience Catalyst has room for (round 21).
 */
public class ExtremeClusterRecipe implements ExtremeRecipe {
    final Ingredient extra;

    public ExtremeClusterRecipe(Ingredient extra) {
        this.extra = extra;
    }

    /**
     * Every level of clusters can be made, whatever recipe needs them (so no recipe has to tell this one how much room
     * it has): the result is worked out from what is in the grid, never remembered between calls.
     */
    private static ItemStack find(CraftingInput in, Level level, Ingredient extra) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) if (!in.getItem(i).isEmpty()) grid.add(in.getItem(i));
        if (grid.size() < 2 || grid.size() > Singularities.CLUSTER + 1) return ItemStack.EMPTY;
        boolean client = level != null ? level.isClientSide : net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                && net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() == null;
        // which singularity / cluster is in the grid tells us where to look: no need to try every cluster
        ItemStack probe = ItemStack.EMPTY;
        for (ItemStack g : grid) if (Singularities.idOf(g) != null && !extra.test(g)) { probe = g; break; }
        if (probe.isEmpty()) return ItemStack.EMPTY;
        for (int lv = 1; lv <= 6; lv++) {
            List<ItemStack> below = Singularities.level(lv - 1, client);
            if (below.size() <= 1) break;
            int at = -1;
            for (int i = 0; i < below.size(); i++) if (Singularities.same(probe, below.get(i))) { at = i; break; }
            if (at < 0) continue;
            int idx = at / Singularities.CLUSTER;
            List<ItemStack> mem = Singularities.members(lv, idx, client);
            if (grid.size() != mem.size() + 1) return ItemStack.EMPTY;
            List<Predicate<ItemStack>> need = new ArrayList<>();
            need.add(extra);
            for (ItemStack m : mem) need.add(x -> Singularities.same(x, m));
            return Matching.assign(need, grid) != null ? Singularities.clusterStack(lv, idx, client).copy() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput in, Level level) {
        return !find(in, level, extra).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
        return find(in, null, extra);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider regs) {
        return new ItemStack(com.theoreticalsciences.registry.QHItems.get("singularity_cluster"));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    /** JEI: one ordinary shapeless recipe per cluster. */
    public List<ExtremeShapelessRecipe> displays(boolean client) {
        List<ExtremeShapelessRecipe> out = new ArrayList<>();
        // only the levels a catalyst could need (with all of the grid free): the rest would never be asked for
        for (int[] c : Singularities.shownClusters(client)) {
            List<ItemStack> mem = Singularities.members(c[0], c[1], client);
            List<Ingredient> ing = new ArrayList<>();
            for (ItemStack m : mem) ing.add(Ingredient.of(m));
            ing.add(extra);
            out.add(new ExtremeShapelessRecipe(ing, Singularities.clusterStack(c[0], c[1], client)));
        }
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ExtremeRegistry.CLUSTER.get();
    }

    public static class Serializer implements RecipeSerializer<ExtremeClusterRecipe> {
        public static final MapCodec<ExtremeClusterRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("extra").forGetter(r -> r.extra)).apply(i, ExtremeClusterRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ExtremeClusterRecipe> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, r -> r.extra, ExtremeClusterRecipe::new);

        @Override
        public MapCodec<ExtremeClusterRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtremeClusterRecipe> streamCodec() {
            return STREAM;
        }
    }
}
