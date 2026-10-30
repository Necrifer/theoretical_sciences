package com.theoreticalsciences.converge;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Round 19: the Convergence Altar's recipe type (its blocks are content blocks of kinds conv_altar / conv_pedestal). */
public final class ConvergenceRegistry {
    static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TheoreticalSciences.MODID);
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ConvergenceRecipe>> TYPE = TYPES.register("convergence",
            () -> RecipeType.simple(TheoreticalSciences.id("convergence")));
    public static final DeferredHolder<RecipeSerializer<?>, ConvergenceRecipe.Serializer> SERIALIZER = SERIALIZERS.register("convergence",
            ConvergenceRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ConvergenceRegistry() {}
}
