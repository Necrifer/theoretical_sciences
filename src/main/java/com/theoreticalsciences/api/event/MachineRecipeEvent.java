package com.theoreticalsciences.api.event;

import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on {@code NeoForge.EVENT_BUS} (server side) when a Theoretical Sciences machine starts or finishes a recipe.
 * KubeJS scripts receive the same information through {@code TheoreticalSciencesEvents.recipeStarted / recipeFinished}.
 */
public abstract class MachineRecipeEvent extends Event {
    private final MachineBlockEntity machine;
    private final ResourceLocation recipeId;
    private final QHRecipe recipe;

    protected MachineRecipeEvent(MachineBlockEntity machine, ResourceLocation recipeId, QHRecipe recipe) {
        this.machine = machine;
        this.recipeId = recipeId;
        this.recipe = recipe;
    }

    public MachineBlockEntity getMachine() {
        return machine;
    }

    public ResourceLocation getRecipeId() {
        return recipeId;
    }

    public QHRecipe getRecipe() {
        return recipe;
    }

    /** Cancel to stop the machine from starting this recipe (it will retry later). */
    public static class Start extends MachineRecipeEvent implements ICancellableEvent {
        public Start(MachineBlockEntity machine, ResourceLocation recipeId, QHRecipe recipe) {
            super(machine, recipeId, recipe);
        }
    }

    public static class Finish extends MachineRecipeEvent {
        public Finish(MachineBlockEntity machine, ResourceLocation recipeId, QHRecipe recipe) {
            super(machine, recipeId, recipe);
        }
    }
}
