package com.theoreticalsciences.transcend;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A Simulated Universe (round 22): one other mod's whole world, run to completion inside the rebuilt Core's Forge.
 * Each mod has its own (custom data {@code Universe: <mod id>}), made from that mod's own most advanced things, and
 * the Forge needs it to make that mod's creative items - so every creative item costs a simulation of the universe
 * it belongs to, not the same handful of materials.
 */
public class SimulatedUniverseItem extends Item {
    public SimulatedUniverseItem(Properties p) {
        super(p.fireResistant());
    }

    public static ItemStack of(String mod, int count) {
        ItemStack s = new ItemStack(com.theoreticalsciences.registry.QHItems.get("simulated_universe"), count);
        CompoundTag t = new CompoundTag();
        t.putString("Universe", mod);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public static String mod(ItemStack s) {
        var cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.getUnsafe();
        return t.contains("Universe") ? t.getString("Universe") : null;
    }

    /** The mod's display name (or its id). */
    public static String modName(String mod) {
        return net.neoforged.fml.ModList.get().getModContainerById(mod).map(c -> c.getModInfo().getDisplayName()).orElse(mod);
    }

    /** A colour of its own for each mod. */
    public static int colour(@Nullable String mod) {
        if (mod == null) return 0x9AD0FF;
        int h = mod.hashCode();
        return net.minecraft.util.Mth.hsvToRgb(((h >>> 4) & 0xFFF) / 4096F, 0.55F, 1F) & 0xFFFFFF;
    }

    @Override
    public Component getName(ItemStack stack) {
        String m = mod(stack);
        return m == null ? super.getName(stack) : Component.translatable("item.theoreticalsciences.simulated_universe.of", modName(m));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String m = mod(stack);
        tip.add(Component.translatable(m == null ? "tooltip.theoreticalsciences.simulated_universe.empty" : "tooltip.theoreticalsciences.simulated_universe", m == null ? "" : modName(m))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
