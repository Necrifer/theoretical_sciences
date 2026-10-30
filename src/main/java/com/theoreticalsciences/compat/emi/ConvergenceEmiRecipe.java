package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.converge.ConvergenceRecipe;
import com.theoreticalsciences.registry.QHItems;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The Convergence Altar: the core in the middle, the pedestals' items in a ring around it. */
public class ConvergenceEmiRecipe extends BasicEmiRecipe {
    private static final int CX = 56, CY = 52, R = 40;
    private final ConvergenceRecipe r;

    public ConvergenceEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, ConvergenceRecipe r) {
        super(cat, id, 170, 118);
        this.r = r;
        inputs.add(QHEmi.item(r.core));
        for (var p : r.pedestals) inputs.add(QHEmi.item(p));
        // the pedestal grade it needs: kept, not used up
        catalysts.add(EmiStack.of(new ItemStack(QHItems.get(r.tier >= 3 ? "absolute_pedestal" : r.tier == 2 ? "harmonic_pedestal" : "resonance_pedestal"))));
        outputs.add(EmiStack.of(r.result));
    }

    private static int[] ring(int i, int n) {
        double a = -Math.PI / 2 + 2 * Math.PI * i / Math.max(1, n);
        return new int[]{CX + (int) Math.round(Math.cos(a) * R) - 8, CY + (int) Math.round(Math.sin(a) * R) - 8};
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        int n = r.pedestals.size();
        // dotted lines from each pedestal to the core, and the purple core frame
        w.addDrawable(0, 0, 170, 118, (g, mx, my, d) -> {
            for (int i = 0; i < n; i++) {
                int[] p = ring(i, n);
                line(g, p[0] + 8, p[1] + 8, CX, CY, 0x60B080FF);
            }
            g.fill(CX - 10, CY - 10, CX + 10, CY + 10, 0xFFB080FF);
            g.fill(110, CY - 1, 136, CY + 1, 0xFFB060FF);
        });
        w.addSlot(inputs.get(0), CX - 9, CY - 9).appendTooltip(Component.translatable("jei.theoreticalsciences.altar_core"));
        for (int i = 0; i < n; i++) {
            int[] p = ring(i, n);
            w.addSlot(inputs.get(1 + i), p[0] - 1, p[1] - 1).appendTooltip(Component.translatable("jei.theoreticalsciences.altar_pedestal"));
        }
        w.addSlot(outputs.get(0), 139, CY - 9).recipeContext(this);
        w.addSlot(catalysts.get(0), 139, 3).catalyst(true)
                .appendTooltip(Component.translatable("jei.theoreticalsciences.not_consumed"));
        w.addText(Component.literal(QHEmi.fe(r.energy)), 104, CY + 16, 0xFFC04040, false);
        w.addText(Component.literal(String.format("%.1fs", r.duration / 20.0)), 104, CY + 26, 0xFF606878, false);
        w.addText(Component.translatable("jei.theoreticalsciences.altar_tier", r.tier), 100, 24, 0xFF6020A0, false);
        if (r.hasResearch()) QHEmi.line(w, QHEmi.research(r.research), 0, 108, 0xFF9060C0, List.of());
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int s = 0; s <= steps; s += 2) {
            int x = x0 + (x1 - x0) * s / Math.max(1, steps), y = y0 + (y1 - y0) * s / Math.max(1, steps);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }
}
