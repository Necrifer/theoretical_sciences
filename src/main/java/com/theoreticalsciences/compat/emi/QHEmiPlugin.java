package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.converge.ConvergenceRegistry;
import com.theoreticalsciences.extreme.Collections;
import com.theoreticalsciences.extreme.CompactionRecipe;
import com.theoreticalsciences.extreme.ExtremeBundleRecipe;
import com.theoreticalsciences.extreme.ExtremeClusterRecipe;
import com.theoreticalsciences.extreme.ExtremeRecipe;
import com.theoreticalsciences.extreme.ExtremeRegistry;
import com.theoreticalsciences.extreme.Singularities;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.multiblock.Multiblocks;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.registry.QHRecipes;
import com.theoreticalsciences.transcend.ConfluenceBlockEntity;
import com.theoreticalsciences.transcend.CoreForge;
import com.theoreticalsciences.transcend.Essences;
import com.theoreticalsciences.transcend.TranscendRegistry;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * EMI integration - everything the JEI plugin (compat/jei) shows, built with EMI's own API.
 * <p>
 * EMI finds this class by itself: on NeoForge it scans every mod file for the {@link EmiEntrypoint} annotation and
 * creates the class with its no-argument constructor. Nothing else in the mod refers to this package, so without
 * EMI installed none of it is ever loaded.
 * <p>
 * With JEI installed as well, EMI also reads JEI's plugins through its "JEMI" layer. JEMI skips every JEI tab whose
 * id belongs to a mod that has its own EMI plugin (it collects the mod ids of all EMI plugins and drops JEI
 * categories in those namespaces), so our JEI tabs are not shown twice. Two things JEMI copies regardless of mod are
 * JEI's info pages and the catalysts of the vanilla crafting tab - so when JEI is present we leave those two out
 * here and let JEMI bring them over, instead of showing them twice.
 */
@EmiEntrypoint
public class QHEmiPlugin implements EmiPlugin {
    private static final java.util.concurrent.atomic.AtomicBoolean HOVER_ADDED = new java.util.concurrent.atomic.AtomicBoolean();
    /** One tab per machine, with the same ids as the JEI tabs. */
    static final Map<MachineType, EmiRecipeCategory> MACHINES = new EnumMap<>(MachineType.class);
    static EmiRecipeCategory MULTIBLOCKS, EXTREME, COMPACTION, PLEROMA, CONVERGENCE, FORGE;

    @Override
    public void register(EmiRegistry reg) {
        // the machine screens reach past their own panel (physics console, late-game operator console): EMI keeps clear
        reg.addExclusionArea(com.theoreticalsciences.client.screen.MachineScreen.class, (screen, out) -> {
            for (var r : screen.extraAreas()) out.accept(new dev.emi.emi.api.widget.Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight()));
        });
        // round 35: what cannot be had in survival and does nothing in an inventory is left out (#theoreticalsciences:hidden_from_viewers)
        var hidden = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, com.theoreticalsciences.TheoreticalSciences.id("hidden_from_viewers"));
        reg.removeEmiStacks(s -> !s.getItemStack().isEmpty() && s.getItemStack().is(hidden));
        // round 35: the Codex key works on the items EMI shows (its list, favourites, recipe pages)
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && HOVER_ADDED.compareAndSet(false, true))
            com.theoreticalsciences.client.codex.CodexKeys.HOVERED.add(() -> {
                var in = dev.emi.emi.api.EmiApi.getHoveredStack(false);
                if (in == null || in.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
                for (var s : in.getStack().getEmiStacks()) {
                    var it = s.getItemStack();
                    if (!it.isEmpty()) return it;
                }
                return net.minecraft.world.item.ItemStack.EMPTY;
            });
        boolean jemi = ModList.get().isLoaded("jei");       // EMI runs its JEI layer exactly when JEI is loaded
        categories(reg);
        safely("comparisons", () -> comparisons(reg));
        safely("extra stacks", () -> extraStacks(reg));
        safely("workstations", () -> workstations(reg, jemi));
        safely("recipes", () -> recipes(reg));
        if (!jemi) safely("info pages", () -> info(reg));
        reg.addRecipeHandler(ExtremeRegistry.EXTREME_MENU.get(), new ExtremeTableRecipeHandler());
    }

    /** One broken recipe or a missing item should not take every other page down with it. */
    private static void safely(String what, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            TheoreticalSciences.LOGGER.error("Theoretical Sciences EMI: failed to register {}", what, t);
        }
    }

    // ------------------------------------------------------------------ tabs

    private static void categories(EmiRegistry reg) {
        MACHINES.clear();
        for (MachineType t : MachineType.values()) {
            EmiRecipeCategory c = new QHEmiCategory(TheoreticalSciences.id(t.recipeType), QHItems.get(t.id()),
                    Component.translatable("block.theoreticalsciences." + t.id()));
            MACHINES.put(t, c);
            reg.addCategory(c);
        }
        MULTIBLOCKS = new QHEmiCategory(TheoreticalSciences.id("multiblock_structures"), QHItems.get("interface_port"),
                Component.translatable("jei.theoreticalsciences.multiblocks"));
        EXTREME = new QHEmiCategory(TheoreticalSciences.id("extreme_crafting"), ExtremeRegistry.EXTREME_TABLE_ITEM.get(),
                Component.translatable("block.theoreticalsciences.extreme_assembly_table"));
        COMPACTION = new QHEmiCategory(TheoreticalSciences.id("compaction"), ExtremeRegistry.COMPACTOR_ITEM.get(),
                Component.translatable("block.theoreticalsciences.singularity_compactor"));
        PLEROMA = new QHEmiCategory(TheoreticalSciences.id("pleroma"), TranscendRegistry.CONFLUENCE_ITEM.get(),
                Component.translatable("block.theoreticalsciences.pleroma_confluence"));
        CONVERGENCE = new QHEmiCategory(TheoreticalSciences.id("convergence"), QHItems.get("convergence_altar"),
                Component.translatable("block.theoreticalsciences.convergence_altar"));
        FORGE = new QHEmiCategory(TheoreticalSciences.id("omnipotence_forge"), QHItems.get("eschaton_core"),
                Component.translatable("jei.theoreticalsciences.forge"));
        for (EmiRecipeCategory c : new EmiRecipeCategory[]{MULTIBLOCKS, EXTREME, COMPACTION, PLEROMA, CONVERGENCE, FORGE}) reg.addCategory(c);
    }

    // ------------------------------------------------------------------ telling stacks apart

    /**
     * Pack-added / automatic singularities and clusters are one item each, told apart by their data; so are the
     * specimen bundles, and every mod's Essence (and each part of one) is the same fluid with different data. Without
     * this EMI would treat them all as one stack each.
     */
    private static void comparisons(EmiRegistry reg) {
        Comparison sing = Comparison.compareData(s -> Singularities.idOf(s.getItemStack()));
        reg.setDefaultComparison(QHItems.get("dynamic_singularity"), sing);
        reg.setDefaultComparison(QHItems.get("singularity_cluster"), sing);
        reg.setDefaultComparison(QHItems.get("specimen_bundle"), Comparison.compareData(s -> Collections.idOf(s.getItemStack())));
        reg.setDefaultComparison(Essences.essenceFluid(), Comparison.compareData(QHEmiPlugin::essenceKey));
    }

    /** Which Essence (or part of one) a fluid stack is, read from its data. */
    private static String essenceKey(EmiStack s) {
        Fluid f = s.getKeyOfType(Fluid.class);
        if (f == null) return null;
        return Essences.key(new FluidStack(f.builtInRegistryHolder(), 1, s.getComponentChanges()));
    }

    // ------------------------------------------------------------------ the index

    /** Every pack-added and automatic singularity, every cluster, every bundle and every Essence in the index. */
    private static void extraStacks(EmiRegistry reg) {
        for (var d : Singularities.all(true)) reg.addEmiStack(EmiStack.of(Singularities.stack(d)));
        for (int[] c : Singularities.shownClusters(true)) reg.addEmiStack(EmiStack.of(Singularities.clusterStack(c[0], c[1], true).copy()));
        for (var h : reg.getRecipeManager().getAllRecipesFor(ExtremeRegistry.EXTREME_TYPE.get()))
            if (h.value() instanceof ExtremeBundleRecipe b) for (ItemStack s : b.stacks()) reg.addEmiStack(EmiStack.of(s));
        for (var st : Essences.steps()) reg.addEmiStack(QHEmi.fluid(st.output().copyWithAmount(1000)));
    }

    // ------------------------------------------------------------------ workstations

    private static void workstations(EmiRegistry reg, boolean jemi) {
        for (MachineType t : MachineType.values()) {
            EmiStack machine = EmiStack.of(QHItems.get(t.id()));
            reg.addWorkstation(MACHINES.get(t), machine);
            if (t.isMultiblock()) reg.addWorkstation(MULTIBLOCKS, machine);
        }
        EmiStack table = EmiStack.of(ExtremeRegistry.EXTREME_TABLE_ITEM.get());
        reg.addWorkstation(EXTREME, table);
        reg.addWorkstation(EXTREME, EmiStack.of(ExtremeRegistry.AUTO_ASSEMBLER_ITEM.get()));
        // the table crafts ordinary recipes too (with JEI present, JEMI already copies this from the JEI plugin)
        if (!jemi) reg.addWorkstation(VanillaEmiRecipeCategories.CRAFTING, table);
        reg.addWorkstation(COMPACTION, EmiStack.of(ExtremeRegistry.COMPACTOR_ITEM.get()));
        reg.addWorkstation(PLEROMA, EmiStack.of(TranscendRegistry.CONFLUENCE_ITEM.get()));
        for (String id : new String[]{"convergence_altar", "resonance_pedestal", "harmonic_pedestal", "absolute_pedestal"})
            reg.addWorkstation(CONVERGENCE, EmiStack.of(QHItems.get(id)));
    }

    // ------------------------------------------------------------------ recipes

    private static void recipes(EmiRegistry reg) {
        RecipeManager rm = reg.getRecipeManager();
        var level = Minecraft.getInstance().level;
        RegistryAccess ra = level == null ? null : level.registryAccess();

        for (MachineType t : MachineType.values()) {
            EmiRecipeCategory cat = MACHINES.get(t);
            List<RecipeHolder<QHRecipe>> list = new ArrayList<>(rm.getAllRecipesFor(QHRecipes.type(t.recipeType)));
            // the Essence distillations are made from the fluid registry, not from any datapack
            if (t == MachineType.ESSENCE_DISTILLERY) list.addAll(Essences.recipes());
            for (RecipeHolder<QHRecipe> h : list) safely("recipe " + h.id(), () -> reg.addRecipe(new MachineEmiRecipe(cat, h.id(), t, h.value())));
        }

        for (MultiblockDefinition def : Multiblocks.all().values())
            safely("structure " + def.id, () -> reg.addRecipe(new MultiblockEmiRecipe(MULTIBLOCKS, def)));

        if (ra != null) {
            for (var h : rm.getAllRecipesFor(ExtremeRegistry.EXTREME_TYPE.get())) {
                safely("extreme recipe " + h.id(), () -> {
                    // cluster and bundle recipes are shown as one ordinary page per cluster / bundle, as in JEI
                    List<? extends ExtremeRecipe> pages = h.value() instanceof ExtremeClusterRecipe c ? c.displays(true)
                            : h.value() instanceof ExtremeBundleRecipe b ? b.displays() : null;
                    if (pages == null) reg.addRecipe(new ExtremeEmiRecipe(EXTREME, h.id(), h.value(), h.value().getResultItem(ra)));
                    else for (int i = 0; i < pages.size(); i++) {
                        ExtremeRecipe p = pages.get(i);
                        reg.addRecipe(new ExtremeEmiRecipe(EXTREME, QHEmi.sub(h.id(), String.valueOf(i)), p, p.getResultItem(ra)));
                    }
                });
            }
        }

        for (var h : rm.getAllRecipesFor(ExtremeRegistry.COMPACTION_TYPE.get())) {
            CompactionRecipe r = h.value();
            safely("compaction " + h.id(), () -> reg.addRecipe(new CompactionEmiRecipe(COMPACTION, h.id(), r.ingredient(), r.count(),
                    ra == null ? r.result() : r.getResultItem(ra))));
        }
        for (var d : Singularities.all(true))          // the pack-added and automatic singularities
            safely("singularity " + d.id(), () -> reg.addRecipe(new CompactionEmiRecipe(COMPACTION, QHEmi.synthetic("compaction/" + d.id()),
                    d.ingredient(), d.count(), Singularities.stack(d))));

        safely("pleroma", () -> reg.addRecipe(new PleromaEmiRecipe(PLEROMA, ConfluenceBlockEntity.wanted())));

        for (var h : rm.getAllRecipesFor(ConvergenceRegistry.TYPE.get()))
            safely("convergence " + h.id(), () -> reg.addRecipe(new ConvergenceEmiRecipe(CONVERGENCE, h.id(), h.value())));

        for (CoreForge.Recipe r : CoreForge.all())
            safely("forge " + r.id(), () -> reg.addRecipe(new ForgeEmiRecipe(FORGE, QHEmi.synthetic("omnipotence_forge/" + r.id()), r)));
    }

    /** Where the fluids that no machine makes come from. */
    private static void info(EmiRegistry reg) {
        for (String f : new String[]{"schrodinger_limit", "relativity_breakdown", "pleroma"}) {
            var e = QHFluids.ALL.get(f);
            if (e == null) continue;
            List<EmiIngredient> stacks = List.of(EmiStack.of(e.source.get(), 1000), EmiStack.of(e.bucket.get()));
            reg.addRecipe(new EmiInfoRecipe(stacks, List.of(Component.translatable("jei.theoreticalsciences.info." + f)), QHEmi.synthetic("info/" + f)));
        }
    }
}
