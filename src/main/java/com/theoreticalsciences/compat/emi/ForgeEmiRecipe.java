package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.transcend.CoreForge;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** A recipe of the rebuilt Omnipotence Core's Forge, drawn as a page of its green terminal. */
public class ForgeEmiRecipe extends BasicEmiRecipe {
    private static final int GREEN = 0xFF5CFF8A, DIM = 0xFF2F8A4C, W = 170, H = 148;
    /** Up to 20 ingredients: five across, four down. */
    private static final int COLS = 5;
    private final CoreForge.Recipe r;
    /** Every ingredient in its own slot, in order (kept ones too). */
    private final List<EmiIngredient> slots = new ArrayList<>();

    public ForgeEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, CoreForge.Recipe r) {
        super(cat, id, W, H);
        this.r = r;
        for (CoreForge.Ing in : r.in()) {
            EmiStack s = EmiStack.of(in.stack(1), Math.max(1, in.count()));
            slots.add(s);
            if (in.count() == 0) catalysts.add(s);      // needed, but not used up
            else inputs.add(s);
        }
        outputs.add(EmiStack.of(r.result()));
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        var font = Minecraft.getInstance().font;
        w.addDrawable(0, 0, W, H, (g, mx, my, d) -> {
            g.fill(0, 0, W, H, 0xFF020A05);
            for (int y = 0; y < H; y += 2) g.fill(0, y, W, y + 1, 0x18000000);
            g.renderOutline(0, 0, W, H, DIM);
            g.drawString(font, font.plainSubstrByWidth("> forge " + r.id(), W - 8), 4, 4, GREEN, false);
            int i = 0;
            for (CoreForge.Ing in : r.in()) {
                int x = 6 + (i % COLS) * 24, y = 18 + (i / COLS) * 26;
                String n = in.count() == 0 ? "kept" : in.count() > 99 ? ShortCount.of(in.count()) : "";
                if (!n.isEmpty()) g.drawString(font, n, x + 8 - font.width(n) / 2, y + 17, DIM, false);
                i++;
            }
            g.fill(128, 47, 142, 48, GREEN);
            com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable(com.theoreticalsciences.transcend.ForgeDrift.drifts(r.id()) ? "jei.theoreticalsciences.forge_laws_drift" : "jei.theoreticalsciences.forge_laws"), 4, H - 22, W - 8, DIM, 2, false);
        });
        int i = 0;
        for (CoreForge.Ing in : r.in()) {
            int x = 6 + (i % COLS) * 24, y = 18 + (i / COLS) * 26;
            var slot = w.addSlot(slots.get(i), x - 1, y - 1);
            if (in.count() == 0) slot.catalyst(true).appendTooltip(Component.translatable("jei.theoreticalsciences.not_consumed"));
            else slot.appendTooltip(Component.literal(String.format("× %,d", in.count())));
            i++;
        }
        w.addSlot(outputs.get(0), 145, 39).recipeContext(this);
    }
}
