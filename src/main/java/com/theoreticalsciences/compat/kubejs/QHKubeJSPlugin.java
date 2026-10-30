package com.theoreticalsciences.compat.kubejs;

import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.api.event.MultiblockFormedEvent;
import com.theoreticalsciences.api.event.ResearchUnlockedEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.neoforged.neoforge.common.NeoForge;

/**
 * KubeJS integration (loaded by KubeJS through kubejs.plugins.txt, never otherwise).
 * <ul>
 *   <li>Recipe schemas live in data/theoreticalsciences/kubejs/recipe_schema - e.g.
 *       {@code event.recipes.theoreticalsciences.entangling([outputs], [inputs]).energy(512).duration(200)}</li>
 *   <li>Server events: {@code TheoreticalSciencesEvents.recipeStarted / recipeFinished / researchUnlocked / multiblockFormed}</li>
 *   <li>Binding {@code TheoreticalSciences}: research and insight helpers.</li>
 *   <li>Round 31: data folders a script can fill with {@code ServerEvents.generateData('after_mods', e => e.json(...))}:
 *       ts_component_tiers (new coil / emitter tiers), ts_forge (Core Forge recipes), ts_machines (per-machine numbers).
 *       See docs/KUBEJS.md.</li>
 * </ul>
 */
public class QHKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("TheoreticalSciencesEvents");
    public static final EventHandler RECIPE_STARTED = GROUP.server("recipeStarted", () -> QHKubeEvents.RecipeStarted.class).hasResult();
    public static final EventHandler RECIPE_FINISHED = GROUP.server("recipeFinished", () -> QHKubeEvents.RecipeFinished.class);
    public static final EventHandler RESEARCH_UNLOCKED = GROUP.server("researchUnlocked", () -> QHKubeEvents.ResearchUnlocked.class);
    public static final EventHandler MULTIBLOCK_FORMED = GROUP.server("multiblockFormed", () -> QHKubeEvents.MultiblockChanged.class);

    @Override
    public void init() {
        NeoForge.EVENT_BUS.addListener(MachineRecipeEvent.Start.class, e -> {
            if (!RECIPE_STARTED.hasListeners()) return;
            EventResult r = RECIPE_STARTED.post(ScriptType.SERVER, new QHKubeEvents.RecipeStarted(e));
            if (r.interruptFalse()) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(MachineRecipeEvent.Finish.class, e -> {
            if (RECIPE_FINISHED.hasListeners()) RECIPE_FINISHED.post(ScriptType.SERVER, new QHKubeEvents.RecipeFinished(e));
        });
        NeoForge.EVENT_BUS.addListener(ResearchUnlockedEvent.class, e -> {
            if (RESEARCH_UNLOCKED.hasListeners()) RESEARCH_UNLOCKED.post(ScriptType.SERVER, new QHKubeEvents.ResearchUnlocked(e));
        });
        NeoForge.EVENT_BUS.addListener(MultiblockFormedEvent.class, e -> {
            if (MULTIBLOCK_FORMED.hasListeners()) MULTIBLOCK_FORMED.post(ScriptType.SERVER, new QHKubeEvents.MultiblockChanged(e));
        });
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("TheoreticalSciences", QHKubeBindings.class);
    }
}
