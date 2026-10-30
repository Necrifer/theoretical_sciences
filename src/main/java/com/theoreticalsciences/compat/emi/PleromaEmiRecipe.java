package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.transcend.ConfluenceBlockEntity;
import com.theoreticalsciences.transcend.Essences;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** The Pleroma Confluence's one "recipe": a million mB of every other fluid (and every Essence) -> Pleroma. */
public class PleromaEmiRecipe extends BasicEmiRecipe {
    private final List<FluidStack> wanted;
    private final int plain, essences;

    public PleromaEmiRecipe(EmiRecipeCategory cat, List<FluidStack> wanted) {
        super(cat, QHEmi.synthetic("pleroma"), 176, 0);
        this.wanted = wanted;
        int e = 0;
        for (FluidStack f : wanted) if (Essences.isEssence(f)) e++;
        this.essences = e;
        this.plain = wanted.size() - e;
        this.height = Math.max(112, essenceTop() + ((e + 7) / 8) * 18 + 2);
        for (FluidStack f : wanted) inputs.add(QHEmi.fluid(f.copyWithAmount(ConfluenceBlockEntity.target(f))));
        outputs.add(QHEmi.fluid(QHFluids.ALL.get("pleroma").source.get(), ConfluenceBlockEntity.BATCH));
    }

    /** Where the Essences' rows start: under this mod's fluids and their own heading. */
    private int essenceTop() {
        return 12 + ((plain + 7) / 8) * 18 + 12;
    }

    private int[] pos(int i) {
        if (i < plain) return new int[]{(i % 8) * 18, 11 + (i / 8) * 18};
        int j = i - plain;
        return new int[]{(j % 8) * 18, essenceTop() - 1 + (j / 8) * 18};
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        w.addText(Component.translatable("jei.theoreticalsciences.pleroma_each", String.format("%,d", ConfluenceBlockEntity.TARGET)), 1, 0, 0xFF606060, false);
        if (essences > 0)
            w.addText(Component.translatable("jei.theoreticalsciences.pleroma_essences", essences, String.format("%,d", ConfluenceBlockEntity.ESSENCE_TARGET)),
                    1, essenceTop() - 10, 0xFF606060, false);
        for (int i = 0; i < wanted.size(); i++) {
            int[] p = pos(i);
            w.addTank(inputs.get(i), p[0], p[1], 18, 18, MachineEmiRecipe.cap(inputs.get(i)));
        }
        w.addDrawable(148, 47, 6, 2, (g, mx, my, d) -> g.fill(0, 0, 6, 2, 0xFFC080FF));
        w.addTank(outputs.get(0), 155, 39, 18, 18, ConfluenceBlockEntity.BATCH).recipeContext(this);
    }
}
