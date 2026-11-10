package com.theoreticalsciences.endgame;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.extreme.Singularities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;

/**
 * 1.7.0: the Field Syntheses, the endgame's first tier. One per field, each made by a chain of that field's machines.
 * Like the Singularity clusters they compact into lettered levels: nine of one level, in a crafting grid, make one of
 * the next. Level 0 is the plain item; 1 to 24 are lettered in Greek (Alpha to Omega), 25 to 46 in Hebrew (Aleph to
 * Tav). Later tiers ask for higher letters: cosmic engineering takes the plain item, beyond physics Alpha, the final
 * tier Beta.
 */
public final class FieldSynthesis {
    public static final Set<String> IDS = Set.of("unified_field_lattice", "perennial_catalyst_matrix", "proof_lattice", "living_archive",
            "engineered_mind", "planetary_survey", "deep_record");
    public static final int MAX = Singularities.GREEK.length + Singularities.HEBREW.length;

    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheoreticalSciences.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEVEL = COMPONENTS.registerComponentType("synthesis_level",
            b -> b.persistent(Codec.intRange(0, MAX)).networkSynchronized(ByteBufCodecs.VAR_INT));
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<Compact>> COMPACT = SERIALIZERS.register("synthesis_compaction",
            () -> new SimpleCraftingRecipeSerializer<>(Compact::new));

    public static int level(ItemStack s) {
        Integer l = s.get(LEVEL.get());
        return l == null ? 0 : l;
    }

    /** "Alpha α", "Aleph א"... for a level of 1 or more. */
    public static String letter(int level) {
        if (level <= 0) return "";
        int g = Singularities.GREEK.length;
        if (level <= g) return Singularities.GREEK[level - 1] + " " + Singularities.GREEK_SYM[level - 1];
        int h = Math.min(level - g, Singularities.HEBREW.length);
        return Singularities.HEBREW[h - 1] + " " + Singularities.HEBREW_SYM[h - 1];
    }

    public static class SynthesisItem extends Item {
        public SynthesisItem(Properties p) {
            super(p.fireResistant());
        }

        @Override
        public Component getName(ItemStack stack) {
            int l = level(stack);
            Component base = super.getName(stack);
            return l <= 0 ? base : base.copy().append(" " + letter(l));
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            int l = level(stack);
            tip.add(Component.translatable(l < MAX ? "tooltip.theoreticalsciences.synthesis_level" : "tooltip.theoreticalsciences.synthesis_level_max",
                    String.format("%,d", (long) Math.pow(9, Math.min(l, 19)))).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** Nine Field Syntheses of one kind and one level make one of the next level. */
    public static class Compact extends CustomRecipe {
        public Compact(CraftingBookCategory cat) {
            super(cat);
        }

        private static ItemStack first(CraftingInput in) {
            ItemStack f = ItemStack.EMPTY;
            int n = 0;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (!(s.getItem() instanceof SynthesisItem)) return ItemStack.EMPTY;
                if (f.isEmpty()) f = s;
                else if (s.getItem() != f.getItem() || level(s) != level(f)) return ItemStack.EMPTY;
                n++;
            }
            return n == 9 && level(f) < MAX ? f : ItemStack.EMPTY;
        }

        @Override
        public boolean matches(CraftingInput in, Level level) {
            return !first(in).isEmpty();
        }

        @Override
        public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
            ItemStack f = first(in);
            if (f.isEmpty()) return ItemStack.EMPTY;
            ItemStack out = new ItemStack(f.getItem());
            out.set(LEVEL.get(), level(f) + 1);
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return w * h >= 9;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return COMPACT.get();
        }
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        SERIALIZERS.register(bus);
    }

    private FieldSynthesis() {}
}
