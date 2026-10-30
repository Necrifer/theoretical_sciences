package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.recipe.QHRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** One recipe type + serializer per machine, all sharing the same JSON format (see QHRecipe). */
public final class QHRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, TheoreticalSciences.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);

    public static final Map<String, DeferredHolder<RecipeType<?>, RecipeType<QHRecipe>>> RECIPE_TYPES = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<RecipeSerializer<?>, QHRecipeSerializer>> RECIPE_SERIALIZERS = new LinkedHashMap<>();

    static {
        for (MachineType m : MachineType.values()) {
            String id = m.recipeType;
            if (RECIPE_TYPES.containsKey(id)) continue;
            DeferredHolder<RecipeType<?>, RecipeType<QHRecipe>> type = TYPES.register(id, () -> RecipeType.simple(TheoreticalSciences.id(id)));
            RECIPE_TYPES.put(id, type);
            RECIPE_SERIALIZERS.put(id, SERIALIZERS.register(id, () -> new QHRecipeSerializer(id)));
        }
    }

    public static RecipeType<QHRecipe> type(String id) {
        return RECIPE_TYPES.get(id).get();
    }

    public static QHRecipeSerializer serializer(String id) {
        return RECIPE_SERIALIZERS.get(id).get();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private QHRecipes() {}
}
