package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.TheoreticalSciences;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small shared helpers for the EMI pages. Only EMI's plugin ever reaches this class. */
final class QHEmi {
    private QHEmi() {}

    /**
     * An id for a recipe that has no datapack id of its own (an expanded cluster page, a structure...). EMI's habit
     * is to start such ids with a "/" so they can never clash with a real recipe.
     */
    static ResourceLocation synthetic(String path) {
        return TheoreticalSciences.id("/" + clean(path));
    }

    /** A real recipe id with a suffix, for one of the several pages a single recipe is shown as. */
    static ResourceLocation sub(ResourceLocation base, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), "/" + clean(base.getPath() + "/" + suffix));
    }

    /** Lower-case and swap anything a resource path may not contain for "_". */
    static String clean(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray())
            b.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
        return b.toString();
    }

    /** A fluid stack for EMI, keeping its data (an Essence is told apart by its data). */
    static EmiStack fluid(FluidStack fs) {
        return NeoForgeEmiStack.of(fs);
    }

    static EmiStack fluid(Fluid f, int amount) {
        return EmiStack.of(f, amount);
    }

    /** Every fluid a sized fluid ingredient accepts, each keeping its data, with the amount the recipe needs. */
    static EmiIngredient fluid(SizedFluidIngredient s) {
        List<EmiStack> out = new ArrayList<>();
        for (FluidStack f : s.getFluids()) out.add(NeoForgeEmiStack.of(f.copyWithAmount(s.amount())));
        return out.isEmpty() ? EmiStack.EMPTY : out.size() == 1 ? out.get(0) : EmiIngredient.of(out, s.amount());
    }

    static EmiIngredient item(Ingredient ing) {
        return ing.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(ing);
    }

    static EmiIngredient item(Ingredient ing, long count) {
        return ing.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(ing, count);
    }

    /** "Research: <name>" - the same line the JEI pages show. */
    static Component research(String research) {
        ResourceLocation id = ResourceLocation.tryParse(research);
        return Component.translatable("jei.theoreticalsciences.research",
                id == null ? Component.literal(research) : Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath(), id.getPath()));
    }

    /** 1.2 MFE style energy text, as on the Convergence Altar's JEI page. */
    static String fe(long e) {
        String[] u = {"", "k", "M", "G", "T", "P", "E"};
        double v = e;
        int i = 0;
        while (v >= 1000 && i < u.length - 1) {
            v /= 1000;
            i++;
        }
        return (i == 0 ? String.valueOf(e) : String.format("%.1f %s", v, u[i])) + "FE";
    }

    /** How many rows a line of text takes when wrapped to {@code room} pixels. */
    static int rows(Component text, int room) {
        return Math.max(1, Minecraft.getInstance().font.getSplitter().splitLines(text, Math.max(20, room), text.getStyle()).size());
    }

    /**
     * A line of text wrapped to the page (round 35: wrapped instead of cut), with a hover tooltip of any extra lines.
     * Returns the y below it.
     */
    static int line(WidgetHolder w, Component text, int x, int y, int color, List<Component> tooltip) {
        var font = Minecraft.getInstance().font;
        int room = Math.max(20, w.getWidth() - x - 1);
        var parts = font.split(text, room);
        int widest = 1;
        for (int i = 0; i < parts.size(); i++) {
            w.addText(parts.get(i), x, y + i * 10, color, false);
            widest = Math.max(widest, font.width(parts.get(i)));
        }
        int h = Math.max(1, parts.size()) * 10;
        if (!tooltip.isEmpty()) w.addTooltipText(tooltip, x, y - 1, Math.min(room, widest), h);
        return y + h;
    }
}
