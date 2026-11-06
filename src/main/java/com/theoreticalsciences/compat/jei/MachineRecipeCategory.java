package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.compat.page.RecipePage;
import com.theoreticalsciences.machine.MachineLayout;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.Arrays;
import java.util.List;

public class MachineRecipeCategory implements IRecipeCategory<QHRecipe> {
    private static final int Y0 = MachineLayout.TOP - 4;
    /** Round 23: a band at the top for the energy and time, so no tank is ever drawn over them. */
    private static final int HEAD = 11;
    private final MachineType machine;
    private final RecipeType<QHRecipe> type;
    private final IDrawable icon;
    private final MachineLayout layout;
    private final int width;

    public MachineRecipeCategory(IGuiHelper gui, MachineType machine, RecipeType<QHRecipe> type) {
        this.machine = machine;
        this.type = type;
        this.icon = gui.createDrawableItemStack(new ItemStack(QHItems.get(machine.id())));
        this.layout = new MachineLayout(machine);
        this.width = Math.max(160, layout.width - 4);
    }

    @Override
    public RecipeType<QHRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.theoreticalsciences." + machine.id());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return width;
    }

    private int height = -1;

    /** JEI gives a category one height: the tallest of its recipes' pages (1.2.0: each page lays itself out, compat/page). */
    @Override
    public int getHeight() {
        if (height < 0) {
            int h = 60;
            var level = Minecraft.getInstance().level;
            if (level != null) {
                for (var holder : level.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.registry.QHRecipes.type(machine.recipeType))) {
                    RecipePage.know(holder.value(), holder.id());
                    h = Math.max(h, page(holder.value()).height());
                }
                if (machine == MachineType.ESSENCE_DISTILLERY)
                    for (var holder : com.theoreticalsciences.transcend.Essences.recipes()) {
                        RecipePage.know(holder.value(), holder.id());
                        h = Math.max(h, page(holder.value()).height());
                    }
            }
            height = h;
        }
        return height;
    }

    private RecipePage page(QHRecipe r) {
        return RecipePage.of(machine, r);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, QHRecipe r, IFocusGroup focuses) {
        RecipePage p = page(r);
        for (RecipePage.Slot s : p.slots) {
            switch (s.kind()) {
                case ITEM_IN -> {
                    SizedIngredient in = r.itemInputs.get(s.index());
                    b.addSlot(RecipeIngredientRole.INPUT, s.x(), s.y()).setStandardSlotBackground().addItemStacks(Arrays.asList(in.getItems()));
                }
                case CATALYST -> b.addSlot(RecipeIngredientRole.CATALYST, s.x(), s.y()).setStandardSlotBackground().addIngredients(r.catalysts.get(s.index()))
                        .addRichTooltipCallback((view, tip) -> tip.addAll(s.tip()));
                case ITEM_OUT -> {
                    if (r.secret) break;
                    var slot = b.addSlot(RecipeIngredientRole.OUTPUT, s.x(), s.y()).setStandardSlotBackground().addItemStack(r.itemOutputs.get(s.index()));
                    if (!s.tip().isEmpty()) slot.addRichTooltipCallback((view, tip) -> tip.addAll(s.tip()));
                }
                case FLUID_IN -> {
                    SizedFluidIngredient in = r.fluidInputs.get(s.index());
                    b.addSlot(RecipeIngredientRole.INPUT, s.x(), s.y())
                            .setCustomRenderer(NeoForgeTypes.FLUID_STACK, new ClippedTankRenderer(Math.max(1, in.amount()), 16, p.tankH))
                            .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(in.getFluids()));
                }
                case FLUID_OUT -> {
                    if (r.secret) break;
                    FluidStack fs = r.fluidOutputs.get(s.index());
                    b.addSlot(RecipeIngredientRole.OUTPUT, s.x(), s.y())
                            .setCustomRenderer(NeoForgeTypes.FLUID_STACK, new ClippedTankRenderer(Math.max(1, fs.getAmount()), 16, p.tankH))
                            .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(fs));
                }
                case EMPTY -> {}
            }
        }
    }

    @Override
    public void draw(QHRecipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        RecipePage p = page(r);
        int accent = 0xFF000000 | machine.color;
        var font = Minecraft.getInstance().font;
        long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        for (RecipePage.Slot s : p.slots) {
            switch (s.kind()) {
                case FLUID_IN, FLUID_OUT -> g.renderOutline(s.x() - 1, s.y() - 1, 18, p.tankH + 2, accent);
                case EMPTY -> {
                    g.fill(s.x() - 1, s.y() - 1, s.x() + 17, s.y() + 17, 0xFF8B8B8B);
                    g.fill(s.x(), s.y(), s.x() + 16, s.y() + 16, 0xFF373737);
                }
                case ITEM_OUT -> {
                    if (r.secret) {
                        int red = (time / 10) % 2 == 0 ? 0xFFFF3040 : 0xFFB00020;
                        g.fill(s.x(), s.y(), s.x() + 16, s.y() + 16, 0xFF200008);
                        g.drawString(font, "?", s.x() + 5, s.y() + 4, red, false);
                    }
                }
                default -> {}
            }
            if (s.catalystSlot()) g.renderOutline(s.x() - 2, s.y() - 2, 20, 20, 0xFFFFC060);       // the gold catalyst slot
        }
        // animated beam
        int ax = p.arrowX, ay = p.arrowY + 6;
        g.fill(ax, ay, ax + 24, ay + 4, 0xFF101420);
        int pr = (int) (time % 40) * 24 / 40;
        g.fill(ax, ay, ax + pr, ay + 4, accent);
        String energy = String.format("%,d FE/t", r.energy);
        String dur = String.format("%.1fs", r.duration / 20.0);
        g.drawString(font, (machine.generator ? "+" : "") + energy, 2, 0, machine.generator ? 0x40C060 : 0xC04040, false);
        g.drawString(font, dur, width - 2 - font.width(dur), 0, 0x606878, false);
        int y = p.linesTop;
        var wrapped = p.wrapped();
        for (int i = 0; i < wrapped.size(); i++) {
            int colour = p.lines.get(i).colour();
            for (var part : wrapped.get(i)) {
                g.drawString(font, part, 2, y, colour, false);
                y += RecipePage.LINE_H;
            }
        }
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tip, QHRecipe r, IRecipeSlotsView view, double mx, double my) {
        RecipePage p = page(r);
        int i = p.lineAt(my);
        if (i >= 0) tip.addAll(p.lines.get(i).tip());
    }
}
