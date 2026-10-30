package com.theoreticalsciences.compat.jei;

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

    @Override
    public int getHeight() {
        int dy = layout.areaH - MachineLayout.AREA_H + HEAD;        // the many-slot form is taller
        if (machine == com.theoreticalsciences.machine.MachineType.OMNILEX_ENGINE) return 138 + dy;
        // round 35: room for the harvester's full explanation of what to tune
        if (machine == com.theoreticalsciences.machine.MachineType.VACUUM_HARVESTER) return 132 + dy;
        return (machine.isMultiblock() ? 108 : 88) + dy;        // multiblocks: room for the physics operating window    // room for the impossible recipe's warnings
    }

    private int ox(int x) {
        return x - 4;
    }

    private int oy(int y) {
        return y - Y0 + HEAD;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, QHRecipe r, IFocusGroup focuses) {
        for (int i = 0; i < Math.min(r.itemInputs.size(), machine.itemInputs); i++) {
            SizedIngredient s = r.itemInputs.get(i);
            b.addSlot(RecipeIngredientRole.INPUT, ox(layout.itemInX[i]), oy(layout.itemInY[i]))
                    .setStandardSlotBackground().addItemStacks(Arrays.asList(s.getItems()));
        }
        int used = Math.min(r.itemInputs.size(), machine.itemInputs);
        for (int c = 0; c < r.catalysts.size() && used < machine.itemInputs; c++, used++) {
            b.addSlot(RecipeIngredientRole.CATALYST, ox(layout.itemInX[used]), oy(layout.itemInY[used]))
                    .setStandardSlotBackground().addIngredients(r.catalysts.get(c))
                    .addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.theoreticalsciences.not_consumed").withStyle(net.minecraft.ChatFormatting.GOLD)));
        }
        for (int i = 0; i < Math.min(r.itemOutputs.size(), machine.itemOutputs); i++) {
            float ch = r.chance(i);
            var slot = b.addSlot(RecipeIngredientRole.OUTPUT, ox(layout.itemOutX[i]), oy(layout.itemOutY[i]))
                    .setStandardSlotBackground().addItemStack(r.itemOutputs.get(i));
            if (ch < 1F) slot.addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.theoreticalsciences.chance",
                    String.format("%.1f", ch * 100)).withStyle(net.minecraft.ChatFormatting.AQUA)));
            if (machine == com.theoreticalsciences.machine.MachineType.VACUUM_HARVESTER)
                slot.addRichTooltipCallback((view, tip) -> tip.add(Component.translatable("jei.theoreticalsciences.vacuum_slot").withStyle(net.minecraft.ChatFormatting.AQUA)));
        }
        int cap = 1;          // (each recipe's tank is drawn full)
        for (int i = 0; i < Math.min(r.fluidInputs.size(), machine.fluidInputs); i++) {
            SizedFluidIngredient s = r.fluidInputs.get(i);
            List<FluidStack> stacks = Arrays.asList(s.getFluids());
            b.addSlot(RecipeIngredientRole.INPUT, ox(layout.fluidInX[i]), oy(layout.fluidInY[i]))
                    .setFluidRenderer(Math.max(cap, s.amount()), false, 16, layout.tankH)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, stacks);
        }
        for (int i = 0; i < Math.min(r.fluidOutputs.size(), machine.fluidOutputs); i++) {
            FluidStack fs = r.fluidOutputs.get(i);
            b.addSlot(RecipeIngredientRole.OUTPUT, ox(layout.fluidOutX[i]), oy(layout.fluidOutY[i]))
                    .setFluidRenderer(Math.max(cap, fs.getAmount()), false, 16, layout.tankH)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(fs));
        }
    }

    @Override
    public void draw(QHRecipe r, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        int accent = 0xFF000000 | machine.color;
        // tank frames
        int dy = layout.areaH - MachineLayout.AREA_H + HEAD;
        for (int i = 0; i < machine.fluidInputs; i++) g.renderOutline(ox(layout.fluidInX[i]) - 1, oy(layout.fluidInY[i]) - 1, 18, layout.tankH + 2, accent);
        for (int i = 0; i < machine.fluidOutputs; i++) g.renderOutline(ox(layout.fluidOutX[i]) - 1, oy(layout.fluidOutY[i]) - 1, 18, layout.tankH + 2, accent);
        // animated beam
        int ax = ox(layout.arrowX), ay = oy(layout.arrowY) + 6;
        g.fill(ax, ay, ax + 24, ay + 4, 0xFF101420);
        long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        int p = (int) (time % 40) * 24 / 40;
        g.fill(ax, ay, ax + p, ay + 4, accent);
        var font = Minecraft.getInstance().font;
        String energy = String.format("%,d FE/t", r.energy);
        String dur = String.format("%.1fs", r.duration / 20.0);
        g.drawString(font, (machine.generator ? "+" : "") + energy, 2, 0, machine.generator ? 0x40C060 : 0xC04040, false);
        g.drawString(font, dur, width - 2 - font.width(dur), 0, 0x606878, false);
        if (r.hasResearch()) {
            ResourceLocation id = ResourceLocation.tryParse(r.research);
            Component c = Component.translatable("jei.theoreticalsciences.research",
                    id == null ? Component.literal(r.research) : Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath(), id.getPath()));
            g.drawString(font, c, 2, 70 + dy, 0x9060C0, false);
        }
        if (r.insight > 0) {      // a thought experiment: Insight for the machine's owner
            Component ic = Component.translatable("jei.theoreticalsciences.insight", r.insight);
            g.drawString(font, ic, width - 2 - font.width(ic), 80 + dy, 0xFF60D0FF, false);
        }
        if (r.minTier > 0) {      // needs a better grade of coils in the structure
            Component t = Component.translatable("jei.theoreticalsciences.min_tier", r.minTier);
            g.drawString(font, t, 2, 80 + dy, 0xFFB040, false);
        }
        if (r.secret) {
            // the impossible recipe: no result shown, only warnings
            long t = time;
            int red = (t / 10) % 2 == 0 ? 0xFFFF3040 : 0xFFB00020;
            for (int i = 0; i < machine.itemOutputs; i++) {
                int ox = ox(layout.itemOutX[i]), oy = oy(layout.itemOutY[i]);
                g.fill(ox, oy, ox + 16, oy + 16, 0xFF200008);
                g.drawString(font, "?", ox + 5, oy + 4, red, false);
            }
            String[] warn = {"jei.theoreticalsciences.secret_1", "jei.theoreticalsciences.secret_2", "jei.theoreticalsciences.secret_3", "jei.theoreticalsciences.secret_4"};
            for (int i = 0; i < warn.length; i++)
                com.theoreticalsciences.client.screen.TextFit.draw(g, font, Component.literal("\u26A0 ").append(Component.translatable(warn[i])), 2, 90 + dy + i * 9, width - 4, i == 0 ? red : 0xFFFF8080);
        }
        if (!r.environment.isAny()) {         // needs a projected (or natural) field around the controller
            var lines = r.environment.describe();
            Component lbl = Component.literal("\u25C8 ").append(lines.size() == 1 ? lines.get(0) : Component.translatable("jei.theoreticalsciences.env_label", lines.size()));
            com.theoreticalsciences.client.screen.TextFit.draw(g, font, lbl, 2, 60 + dy, width - 4, 0xFFC080FF);
        }
        if (machine == com.theoreticalsciences.machine.MachineType.RESEARCH_NEXUS)
            com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable("jei.theoreticalsciences.nexus_note"), 2, 60 + dy, width - 4, 0xFF80C0FF, 2, false);
        if (machine == com.theoreticalsciences.machine.MachineType.VACUUM_HARVESTER) {
            com.theoreticalsciences.client.screen.TextFit.wrapped(g, font, Component.translatable("jei.theoreticalsciences.vacuum_note_1"), 2, 60 + dy, width - 4, 0xFF60C0D0, 5, false);
        }
        if (!r.physics.isNone() && !r.secret) {      // the QMD-style operating window
            int yy = 90 + dy;
            for (Component c : physicsLines(r)) {
                if (yy > 99 + dy) break;
                g.drawString(font, font.plainSubstrByWidth(c.getString(), width - 4), 2, yy, 0xFF60E0C0, false);
                yy += 9;
            }
        }
        var onlyIn = com.theoreticalsciences.machine.MachineType.allowedDimensions(machine);
        if (onlyIn != null) {
            // round 35: a structure that only works in one dimension says so, on its own line at the bottom
            com.theoreticalsciences.client.screen.TextFit.draw(g, font, Component.translatable(machine.isMultiblock() ? "jei.theoreticalsciences.multiblock_only_in" : "jei.theoreticalsciences.only_in",
                    com.theoreticalsciences.machine.MachineType.dimensionList(onlyIn)), 2, getHeight() - 10, width - 4, 0xFFFFB040);
        } else if (machine.isMultiblock()) {
            Component mb = Component.translatable("jei.theoreticalsciences.multiblock");
            g.drawString(font, mb, width - 2 - font.width(mb), 70 + dy, 0x6080A0, false);
        }
    }

    /** "⚛ Beam kinetic energy: 2.0 – 3.5 GeV" for every window of the recipe. */
    private java.util.List<Component> physicsLines(QHRecipe r) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        var m = com.theoreticalsciences.machine.physics.PhysicsManager.model(machine.id(), true);
        for (var w : r.physics.windows()) {
            String unit = "";
            Component name = Component.literal(w.q());
            if (m != null) {
                String k = m.derivedIndex(w.q()) >= 0 ? "d" : m.paramIndex(w.q()) >= 0 ? "p" : "s";
                name = Component.translatable("physics.theoreticalsciences." + machine.id() + "." + k + "." + w.q());
                for (var d : m.derived) if (d.id().equals(w.q())) unit = d.unit();
                for (var d : m.params) if (d.id().equals(w.q())) unit = d.unit();
                for (var d : m.states) if (d.id().equals(w.q())) unit = d.unit();
            }
            String f = com.theoreticalsciences.client.screen.PhysicsPanel.fmt(w.min()), t = com.theoreticalsciences.client.screen.PhysicsPanel.fmt(w.max());
            String range = w.min() <= -1e300 ? "\u2264 " + t : w.max() >= 1e300 ? "\u2265 " + f : f + " \u2013 " + t;
            out.add(Component.literal("\u269B ").append(name).append(": " + range + " " + unit));
        }
        return out;
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tip, QHRecipe r, IRecipeSlotsView view, double mx, double my) {
        int dy = layout.areaH - MachineLayout.AREA_H + HEAD;
        if (!r.environment.isAny() && my >= 58 + dy && my < 70 + dy && mx < width / 2.0 + 40) {
            tip.add(Component.translatable("jei.theoreticalsciences.env_title"));
            tip.addAll(r.environment.describe());
            tip.add(Component.translatable("jei.theoreticalsciences.env_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        if (!r.physics.isNone() && !r.secret && my >= 88 + dy && my < 108 + dy) {
            tip.add(Component.translatable("jei.theoreticalsciences.physics_title"));
            tip.addAll(physicsLines(r));
            r.physics.vars().forEach((k, v) -> tip.add(Component.translatable("physics.theoreticalsciences." + machine.id() + ".v." + k)
                    .append(" = " + com.theoreticalsciences.client.screen.PhysicsPanel.fmt(v)).withStyle(net.minecraft.ChatFormatting.GRAY)));
            tip.add(Component.translatable("jei.theoreticalsciences.physics_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
