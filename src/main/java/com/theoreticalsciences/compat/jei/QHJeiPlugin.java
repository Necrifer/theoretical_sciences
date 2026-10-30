package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.multiblock.Multiblocks;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.registry.QHRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.*;

/** JEI integration: one category per machine plus a 3D multiblock structure viewer. Only loaded by JEI. */
@JeiPlugin
public class QHJeiPlugin implements IModPlugin {
    public static final Map<MachineType, RecipeType<QHRecipe>> TYPES = new EnumMap<>(MachineType.class);
    public static final RecipeType<MultiblockDefinition> MULTIBLOCKS = RecipeType.create(TheoreticalSciences.MODID, "multiblock_structures", MultiblockDefinition.class);

    static {
        for (MachineType t : MachineType.values()) TYPES.put(t, RecipeType.create(TheoreticalSciences.MODID, t.recipeType, QHRecipe.class));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return TheoreticalSciences.id("jei");
    }

    /** Round 35: the Codex key works on the items JEI shows (its list, bookmarks, recipe pages). */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime rt) {
        RUNTIME = rt;
        // round 35: blocks that cannot be had in survival and do nothing in an inventory (the Living Proof's walls,
        // the Transcendental's own Core...) are left out of the item list - tag #theoreticalsciences:hidden_from_viewers
        try {
            java.util.List<net.minecraft.world.item.ItemStack> hide = new java.util.ArrayList<>();
            for (var it : net.minecraft.core.registries.BuiltInRegistries.ITEM)
                if (new net.minecraft.world.item.ItemStack(it).is(HIDDEN)) hide.add(new net.minecraft.world.item.ItemStack(it));
            if (!hide.isEmpty()) rt.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hide);
        } catch (Throwable t) {
            com.theoreticalsciences.TheoreticalSciences.LOGGER.warn("Theoretical Sciences: could not hide items from JEI", t);
        }
        if (HOVER_ADDED.compareAndSet(false, true))
            com.theoreticalsciences.client.codex.CodexKeys.HOVERED.add(() -> {
                var r = RUNTIME;
                if (r == null) return net.minecraft.world.item.ItemStack.EMPTY;
                var t = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
                net.minecraft.world.item.ItemStack s = r.getIngredientListOverlay().getIngredientUnderMouse(t);
                if (s == null) s = r.getBookmarkOverlay().getIngredientUnderMouse(t);
                if (s == null) s = r.getRecipesGui().getIngredientUnderMouse(t).orElse(null);
                return s == null ? net.minecraft.world.item.ItemStack.EMPTY : s;
            });
    }

    @Override
    public void onRuntimeUnavailable() {
        RUNTIME = null;
    }

    private static volatile mezz.jei.api.runtime.IJeiRuntime RUNTIME;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> HIDDEN = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, com.theoreticalsciences.TheoreticalSciences.id("hidden_from_viewers"));
    private static final java.util.concurrent.atomic.AtomicBoolean HOVER_ADDED = new java.util.concurrent.atomic.AtomicBoolean();

    /** Pack-added / automatic singularities and clusters are one item each, told apart by their data. */
    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration reg) {
        mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack> by = new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                return com.theoreticalsciences.extreme.Singularities.idOf(s);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                String id = com.theoreticalsciences.extreme.Singularities.idOf(s);
                return id == null ? "" : id;
            }
        };
        reg.registerSubtypeInterpreter(QHItems.get("dynamic_singularity"), by);
        reg.registerSubtypeInterpreter(QHItems.get("singularity_cluster"), by);
        // every mod's Essence (and each part of one) is its own fluid to JEI: looking one up shows only how that one is made
        reg.registerSubtypeInterpreter(NeoForgeTypes.FLUID_STACK, com.theoreticalsciences.transcend.Essences.essenceFluid(), new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(net.neoforged.neoforge.fluids.FluidStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                return com.theoreticalsciences.transcend.Essences.key(s);
            }

            @Override
            public String getLegacyStringSubtypeInfo(net.neoforged.neoforge.fluids.FluidStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                String k = com.theoreticalsciences.transcend.Essences.key(s);
                return k == null ? "" : k;
            }
        });
        reg.registerSubtypeInterpreter(QHItems.get("specimen_bundle"), new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                return com.theoreticalsciences.extreme.Collections.idOf(s);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack s, mezz.jei.api.ingredients.subtypes.UidContext ctx) {
                String id = com.theoreticalsciences.extreme.Collections.idOf(s);
                return id == null ? "" : id;
            }
        });
    }

    /** Every pack-added and automatic singularity, and every cluster, shows in the ingredient list. */
    @Override
    public void registerExtraIngredients(mezz.jei.api.registration.IExtraIngredientRegistration reg) {
        List<ItemStack> extra = new ArrayList<>();
        for (var d : com.theoreticalsciences.extreme.Singularities.all(true)) extra.add(com.theoreticalsciences.extreme.Singularities.stack(d));
        for (int[] c : com.theoreticalsciences.extreme.Singularities.shownClusters(true))
            extra.add(com.theoreticalsciences.extreme.Singularities.clusterStack(c[0], c[1], true).copy());
        var lvl = net.minecraft.client.Minecraft.getInstance().level;
        if (lvl != null)
            for (var h : lvl.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_TYPE.get()))
                if (h.value() instanceof com.theoreticalsciences.extreme.ExtremeBundleRecipe b) extra.addAll(b.stacks());
        reg.addExtraItemStacks(extra);
        // every mod's Essence and its parts in the ingredient list, so each can be looked up by itself
        List<net.neoforged.neoforge.fluids.FluidStack> ess = new ArrayList<>();
        for (var st : com.theoreticalsciences.transcend.Essences.steps()) ess.add(st.output().copyWithAmount(1000));
        reg.addExtraIngredients(NeoForgeTypes.FLUID_STACK, ess);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        var gui = reg.getJeiHelpers().getGuiHelper();
        for (MachineType t : MachineType.values()) reg.addRecipeCategories(new MachineRecipeCategory(gui, t, TYPES.get(t)));
        reg.addRecipeCategories(new MultiblockCategory(gui));
        reg.addRecipeCategories(new ExtremeCraftingCategory(gui), new CompactionCategory(gui), new PleromaCategory(gui));
        reg.addRecipeCategories(new ConvergenceCategory(gui), new ForgeCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (MachineType t : MachineType.values()) {
            List<QHRecipe> list = new ArrayList<>();
            for (RecipeHolder<QHRecipe> h : level.getRecipeManager().getAllRecipesFor(QHRecipes.type(t.recipeType))) list.add(h.value());
            if (t == MachineType.ESSENCE_DISTILLERY) for (RecipeHolder<QHRecipe> h : com.theoreticalsciences.transcend.Essences.recipes()) list.add(h.value());
            reg.addRecipes(TYPES.get(t), list);
        }
        reg.addRecipes(MULTIBLOCKS, new ArrayList<>(Multiblocks.all().values()));
        List<com.theoreticalsciences.extreme.ExtremeRecipe> ext = new ArrayList<>();
        for (var h : level.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_TYPE.get())) {
            if (h.value() instanceof com.theoreticalsciences.extreme.ExtremeClusterRecipe c) ext.addAll(c.displays(true));     // one page per cluster
            else if (h.value() instanceof com.theoreticalsciences.extreme.ExtremeBundleRecipe b) ext.addAll(b.displays());    // one per bundle
            else ext.add(h.value());
        }
        reg.addRecipes(ExtremeCraftingCategory.TYPE, ext);
        reg.addRecipes(PleromaCategory.TYPE, List.of(PleromaCategory.Recipe.build()));
        reg.addRecipes(ConvergenceCategory.TYPE, level.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.converge.ConvergenceRegistry.TYPE.get()).stream().map(RecipeHolder::value).toList());
        reg.addRecipes(ForgeCategory.TYPE, com.theoreticalsciences.transcend.CoreForge.all());
        // where the fluids that no machine makes come from
        for (String f : new String[]{"schrodinger_limit", "relativity_breakdown", "pleroma"}) {
            var e = com.theoreticalsciences.registry.QHFluids.ALL.get(f);
            if (e == null) continue;
            reg.addIngredientInfo(new net.neoforged.neoforge.fluids.FluidStack(e.source.get(), 1000), mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,
                    net.minecraft.network.chat.Component.translatable("jei.theoreticalsciences.info." + f));
            reg.addIngredientInfo(new ItemStack(e.bucket.get()), mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    net.minecraft.network.chat.Component.translatable("jei.theoreticalsciences.info." + f));
        }
        List<com.theoreticalsciences.extreme.CompactionRecipe> comp = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.extreme.ExtremeRegistry.COMPACTION_TYPE.get()).stream().map(RecipeHolder::value).toList());
        for (var d : com.theoreticalsciences.extreme.Singularities.all(true))          // the pack-added and automatic singularities
            comp.add(new com.theoreticalsciences.extreme.CompactionRecipe(d.ingredient(), d.count(), com.theoreticalsciences.extreme.Singularities.stack(d)));
        reg.addRecipes(CompactionCategory.TYPE, comp);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        for (MachineType t : MachineType.values()) {
            ItemStack machine = new ItemStack(QHItems.get(t.id()));
            reg.addRecipeCatalyst(machine, TYPES.get(t));
            if (t.isMultiblock()) reg.addRecipeCatalyst(machine, MULTIBLOCKS);
        }
        reg.addRecipeCatalyst(new ItemStack(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_TABLE_ITEM.get()), ExtremeCraftingCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_TABLE_ITEM.get()), mezz.jei.api.constants.RecipeTypes.CRAFTING);   // it crafts ordinary recipes too
        reg.addRecipeCatalyst(new ItemStack(com.theoreticalsciences.extreme.ExtremeRegistry.COMPACTOR_ITEM.get()), CompactionCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(com.theoreticalsciences.extreme.ExtremeRegistry.AUTO_ASSEMBLER_ITEM.get()), ExtremeCraftingCategory.TYPE);
        reg.addRecipeCatalyst(new ItemStack(com.theoreticalsciences.transcend.TranscendRegistry.CONFLUENCE_ITEM.get()), PleromaCategory.TYPE);
        for (String id : new String[]{"convergence_altar", "resonance_pedestal", "harmonic_pedestal", "absolute_pedestal"})
            reg.addRecipeCatalyst(new ItemStack(QHItems.get(id)), ConvergenceCategory.TYPE);
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration reg) {
        // the machine screens reach past their own panel (the physics console, and the full operator console of the
        // late-game machines): JEI keeps its item list out of that space
        reg.addGuiContainerHandler(com.theoreticalsciences.client.screen.MachineScreen.class, new mezz.jei.api.gui.handlers.IGuiContainerHandler<>() {
            @Override
            public java.util.List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(com.theoreticalsciences.client.screen.MachineScreen s) {
                return s.extraAreas();
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(com.theoreticalsciences.extreme.ExtremeTableMenu.class, com.theoreticalsciences.extreme.ExtremeRegistry.EXTREME_MENU.get(),
                ExtremeCraftingCategory.TYPE, 0, 81, 82, 36);
    }
}
