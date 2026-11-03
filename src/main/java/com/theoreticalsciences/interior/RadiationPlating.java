package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 1.3.8: radiation plating, as NuclearCraft does it. Any piece of armour can carry one plate - put the armour and a plate
 * together in a crafting grid. Better plates stop more: Light 10%, Medium 18%, Heavy 25% of the dose per piece, so four
 * heavy-plated pieces stop all but 5% of it. A better plate replaces a worse one; a worse one is refused.
 */
public final class RadiationPlating {
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, TheoreticalSciences.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<Apply>> APPLY = SERIALIZERS.register("armor_radiation_plating",
            () -> new SimpleCraftingRecipeSerializer<>(Apply::new));

    public static final String[] PLATES = {"light_radiation_plating", "medium_radiation_plating", "heavy_radiation_plating"};
    public static final double[] STOPS = {0.10, 0.18, 0.25};
    static final String KEY = "RadiationPlating";

    /** 0 = none, 1..3 = light, medium, heavy. */
    public static int tier(ItemStack armour) {
        CustomData d = armour.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getInt(KEY);
    }

    static int plateTier(ItemStack s) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());
        if (!key.getNamespace().equals(TheoreticalSciences.MODID)) return 0;
        for (int i = 0; i < PLATES.length; i++) if (PLATES[i].equals(key.getPath())) return i + 1;
        return 0;
    }

    /** The share of a radiation dose that still gets through this entity's plated armour (never below 5%). */
    public static double factor(LivingEntity e) {
        double stop = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            int t = tier(e.getItemBySlot(s));
            if (t > 0) stop += STOPS[t - 1];
        }
        return Math.max(0.05, 1.0 - stop);
    }

    public static class Apply extends CustomRecipe {
        public Apply(CraftingBookCategory cat) {
            super(cat);
        }

        private static ItemStack[] find(CraftingInput in) {
            ItemStack armour = ItemStack.EMPTY, plate = ItemStack.EMPTY;
            for (int i = 0; i < in.size(); i++) {
                ItemStack s = in.getItem(i);
                if (s.isEmpty()) continue;
                if (s.getItem() instanceof ArmorItem) {
                    if (!armour.isEmpty()) return null;
                    armour = s;
                } else if (plateTier(s) > 0) {
                    if (!plate.isEmpty()) return null;
                    plate = s;
                } else return null;
            }
            if (armour.isEmpty() || plate.isEmpty() || plateTier(plate) <= tier(armour)) return null;
            return new ItemStack[]{armour, plate};
        }

        @Override
        public boolean matches(CraftingInput in, Level level) {
            return find(in) != null;
        }

        @Override
        public ItemStack assemble(CraftingInput in, HolderLookup.Provider regs) {
            ItemStack[] f = find(in);
            if (f == null) return ItemStack.EMPTY;
            ItemStack out = f[0].copyWithCount(1);
            int t = plateTier(f[1]);
            CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putInt(KEY, t));
            return out;
        }

        @Override
        public boolean canCraftInDimensions(int w, int h) {
            return w * h >= 2;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return APPLY.get();
        }
    }

    static void tooltip(ItemTooltipEvent e) {
        int t = tier(e.getItemStack());
        if (t > 0)
            e.getToolTip().add(1, Component.translatable("tooltip.theoreticalsciences.radiation_plating." + t, (int) Math.round(STOPS[t - 1] * 100))
                    .withStyle(ChatFormatting.GREEN));
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(RadiationPlating::tooltip);
    }

    private RadiationPlating() {}
}
