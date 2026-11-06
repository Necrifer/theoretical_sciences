package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.compat.page.RecipePage;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One machine recipe in EMI, drawn from its page (compat/page/RecipePage - the same page JEI draws): the slots and tanks
 * where the machine's screen has them, the gold catalyst slot, then every line with its tooltip.
 */
public class MachineEmiRecipe extends dev.emi.emi.api.recipe.BasicEmiRecipe {
    private final MachineType machine;
    private final QHRecipe r;
    private final RecipePage page;
    private final Map<RecipePage.Slot, EmiIngredient> stacks = new HashMap<>();

    public MachineEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, MachineType machine, QHRecipe r) {
        super(cat, id, 0, 0);
        this.machine = machine;
        this.r = r;
        RecipePage.know(r, id);
        this.page = RecipePage.of(machine, r);
        for (RecipePage.Slot s : page.slots) {
            switch (s.kind()) {
                case ITEM_IN -> {
                    var in = r.itemInputs.get(s.index());
                    EmiIngredient e = QHEmi.item(in.ingredient(), in.count());
                    stacks.put(s, e);
                    inputs.add(e);
                }
                case CATALYST -> {
                    EmiIngredient e = QHEmi.item(r.catalysts.get(s.index()));
                    stacks.put(s, e);
                    catalysts.add(e);
                }
                case ITEM_OUT -> {
                    EmiStack e = EmiStack.of(r.itemOutputs.get(s.index()));
                    float ch = r.chance(s.index());
                    if (ch < 1F) e.setChance(ch);
                    stacks.put(s, e);
                    outputs.add(e);
                }
                case FLUID_IN -> {
                    EmiIngredient e = QHEmi.fluid(r.fluidInputs.get(s.index()));
                    stacks.put(s, e);
                    inputs.add(e);
                }
                case FLUID_OUT -> {
                    EmiStack e = QHEmi.fluid(r.fluidOutputs.get(s.index()));
                    stacks.put(s, e);
                    outputs.add(e);
                }
                case EMPTY -> {}
            }
        }
        this.width = page.width;
        this.height = page.height();
    }

    /** Each tank is drawn full: its capacity is the recipe's own amount. */
    static int cap(EmiIngredient i) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, i.getAmount()));
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        var font = Minecraft.getInstance().font;
        String energy = (machine.generator ? "+" : "") + String.format("%,d FE/t", r.energy);
        String dur = String.format("%.1fs", r.duration / 20.0);
        w.addText(Component.literal(energy), 2, 1, machine.generator ? 0xFF40C060 : 0xFFC04040, false);
        w.addText(Component.literal(dur), width - 2 - font.width(dur), 1, 0xFF606878, false);
        w.addTooltipText(List.of(Component.literal(String.format("%,d ticks", r.duration))), width - 2 - font.width(dur), 0, font.width(dur), 10);
        for (RecipePage.Slot s : page.slots) {
            if (s.catalystSlot())
                w.addDrawable(s.x() - 2, s.y() - 2, 20, 20, (g, mx, my, d) -> g.renderOutline(0, 0, 20, 20, 0xFFFFC060));
            EmiIngredient e = stacks.get(s);
            switch (s.kind()) {
                case ITEM_IN -> w.addSlot(e, s.x() - 1, s.y() - 1);
                case CATALYST -> {
                    var slot = w.addSlot(e, s.x() - 1, s.y() - 1).catalyst(true);
                    for (Component c : s.tip()) slot.appendTooltip(c);
                }
                case ITEM_OUT -> {
                    if (r.secret) {
                        w.addDrawable(s.x() - 1, s.y() - 1, 18, 18, (g, mx, my, d) -> {
                            long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
                            g.fill(0, 0, 18, 18, 0xFF8B8B8B);
                            g.fill(1, 1, 17, 17, 0xFF200008);
                            g.drawString(font, "?", 6, 5, (t / 10) % 2 == 0 ? 0xFFFF3040 : 0xFFB00020, false);
                        });
                    } else {
                        var slot = w.addSlot(e, s.x() - 1, s.y() - 1).recipeContext(this);
                        for (Component c : s.tip()) slot.appendTooltip(c);
                    }
                }
                case FLUID_IN -> w.add(new ClippedTankWidget(e, s.x() - 1, s.y() - 1, 18, page.tankH + 2, cap(e)));
                case FLUID_OUT -> {
                    if (!r.secret) w.add(new ClippedTankWidget(e, s.x() - 1, s.y() - 1, 18, page.tankH + 2, cap(e))).recipeContext(this);
                }
                case EMPTY -> w.addSlot(EmiStack.EMPTY, s.x() - 1, s.y() - 1);
            }
        }
        w.addFillingArrow(page.arrowX, page.arrowY, Math.max(50, r.duration * 50));
        int y = page.linesTop;
        var wrapped = page.wrapped();
        for (int i = 0; i < wrapped.size(); i++) {
            RecipePage.Line l = page.lines.get(i);
            int widest = 1;
            var parts = wrapped.get(i);
            for (int k = 0; k < parts.size(); k++) {
                w.addText(parts.get(k), 2, y + k * RecipePage.LINE_H, l.colour(), false);
                widest = Math.max(widest, font.width(parts.get(k)));
            }
            int h = wrapped.get(i).size() * RecipePage.LINE_H;
            if (!l.tip().isEmpty()) w.addTooltipText(l.tip(), 2, y - 1, Math.min(width - 4, widest), h);
            y += h;
        }
    }
}
