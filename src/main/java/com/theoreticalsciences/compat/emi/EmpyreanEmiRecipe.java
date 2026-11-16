package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.home.EmpyreanRecipe;
import com.theoreticalsciences.home.HomeClient;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A 15 by 15 Empyrean Assembly Table recipe for EMI (1.8.0). The inputs EMI counts with are the totals per item (real
 * amounts, however large); the picture is drawn at twelve pixels a slot and only its used slots are widgets.
 */
public class EmpyreanEmiRecipe extends BasicEmiRecipe {
    private static final int CELL = 12, N = EmpyreanRecipe.SIZE;
    private final EmpyreanRecipe recipe;

    public EmpyreanEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, EmpyreanRecipe recipe) {
        super(cat, id, N * CELL + 46, N * CELL + 2);
        this.recipe = recipe;
        recipe.totals().forEach((item, n) -> inputs.add(EmiStack.of(item, n)));
        outputs.add(EmiStack.of(recipe.result.item(), recipe.result.count()));
    }

    /** A slot twelve pixels wide whose item is drawn at three quarters size, without its count. */
    private static final class Small extends SlotWidget {
        Small(EmiIngredient stack, int x, int y) {
            super(stack, x, y);
            drawBack(false);
        }

        @Override
        public Bounds getBounds() {
            return new Bounds(x, y, CELL - 1, CELL - 1);
        }

        @Override
        public void drawStack(GuiGraphics g, int mx, int my, float delta) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.6875F, 0.6875F, 1F);
            getStack().render(g, 0, 0, delta, EmiIngredient.RENDER_ICON);
            g.pose().popPose();
        }
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        w.addDrawable(0, 0, N * CELL + 1, N * CELL + 1, (g, mx, my, delta) -> {
            g.fill(0, 0, N * CELL + 1, N * CELL + 1, 0xFF6A5A38);
            for (int i = 0; i < N * N; i++) {
                int x = 1 + (i % N) * CELL, y = 1 + (i / N) * CELL;
                g.fill(x, y, x + CELL - 1, y + CELL - 1, 0xFF221A0C);
            }
            int ax = N * CELL + 5, ay = N * CELL / 2 - 2;
            g.fill(ax, ay, ax + 12, ay + 3, 0xFFFFE9A8);
            for (int k = 0; k < 4; k++) g.fill(ax + 12 + k, ay - 3 + k, ax + 13 + k, ay + 6 - k, 0xFFFFE9A8);
            var font = Minecraft.getInstance().font;
            String n = "x" + HomeClient.compact(recipe.result.count());
            g.drawString(font, n, N * CELL + 34 - font.width(n) / 2, N * CELL / 2 + 13, 0xFFB89C5C, false);
            if (recipe.needsHome) {
                int ty = N * CELL / 2 + 26;
                for (var line : font.split(Component.translatable("gui.theoreticalsciences.empyrean.needs_home_short"), 44)) {
                    g.drawString(font, line, N * CELL + 3, ty, 0xFFFF9A6A, false);
                    ty += 9;
                }
            }
        });
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                EmpyreanRecipe.Slot s = recipe.cell(x, y);
                if (s == null) continue;
                long total = recipe.totals().get(s.item());
                w.add(new Small(EmiStack.of(s.item(), s.count()), 1 + x * CELL, 1 + y * CELL)
                        .appendTooltip(Component.translatable("gui.theoreticalsciences.empyrean.tip_slot", HomeClient.full(s.count())).withStyle(ChatFormatting.GOLD))
                        .appendTooltip(Component.translatable("gui.theoreticalsciences.empyrean.tip_recipe_total", HomeClient.full(total)).withStyle(ChatFormatting.GRAY)));
            }
        w.addSlot(outputs.get(0), N * CELL + 25, N * CELL / 2 - 9).recipeContext(this)
                .appendTooltip(Component.literal("x" + HomeClient.full(recipe.result.count())).withStyle(ChatFormatting.GOLD));
    }
}
