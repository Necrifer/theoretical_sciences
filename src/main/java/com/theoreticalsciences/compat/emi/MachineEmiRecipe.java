package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.machine.MachineLayout;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One machine recipe, laid out like the machine's own screen (the same slot positions the JEI page uses): item and
 * fluid inputs, catalysts (marked "not consumed"), outputs with their chances, then the energy, time and every
 * special requirement as lines of text with tooltips.
 */
public class MachineEmiRecipe extends dev.emi.emi.api.recipe.BasicEmiRecipe {
    /** A band at the top for the energy and time. */
    private static final int HEAD = 11;
    private final MachineType machine;
    private final QHRecipe r;
    private final MachineLayout layout;
    private final List<EmiIngredient> itemIns = new ArrayList<>(), fluidIns = new ArrayList<>(), cats = new ArrayList<>();
    private final List<EmiStack> itemOuts = new ArrayList<>(), fluidOuts = new ArrayList<>();
    private final List<Component[]> lines = new ArrayList<>();      // [text, tooltip...]
    private final List<Integer> colours = new ArrayList<>();

    public MachineEmiRecipe(EmiRecipeCategory cat, ResourceLocation id, MachineType machine, QHRecipe r) {
        super(cat, id, 0, 0);
        this.machine = machine;
        this.r = r;
        this.layout = new MachineLayout(machine);
        for (int i = 0; i < Math.min(r.itemInputs.size(), machine.itemInputs); i++) {
            var s = r.itemInputs.get(i);
            itemIns.add(QHEmi.item(s.ingredient(), s.count()));
        }
        int used = itemIns.size();
        for (int c = 0; c < r.catalysts.size() && used < machine.itemInputs; c++, used++) cats.add(QHEmi.item(r.catalysts.get(c)));
        for (int i = 0; i < Math.min(r.itemOutputs.size(), machine.itemOutputs); i++) {
            EmiStack s = EmiStack.of(r.itemOutputs.get(i));
            float ch = r.chance(i);
            if (ch < 1F) s.setChance(ch);        // EMI shows the chance on the slot and in its tooltip
            itemOuts.add(s);
        }
        for (int i = 0; i < Math.min(r.fluidInputs.size(), machine.fluidInputs); i++) fluidIns.add(QHEmi.fluid(r.fluidInputs.get(i)));
        for (int i = 0; i < Math.min(r.fluidOutputs.size(), machine.fluidOutputs); i++) {
            FluidStack fs = r.fluidOutputs.get(i);
            fluidOuts.add(QHEmi.fluid(fs));
        }
        inputs.addAll(itemIns);
        inputs.addAll(fluidIns);
        catalysts.addAll(cats);
        outputs.addAll(itemOuts);
        outputs.addAll(fluidOuts);
        buildLines();
        this.width = Math.max(160, layout.width - 4);
        // round 35: long lines wrap (and the page grows to hold them) instead of being cut
        int rows = 0;
        for (Component[] l : lines) rows += QHEmi.rows(l[0], width - 3);
        this.height = HEAD + layout.areaH + 8 + rows * 10;
    }

    private void line(int colour, Component text, Component... tip) {
        Component[] l = new Component[tip.length + 1];
        l[0] = text;
        System.arraycopy(tip, 0, l, 1, tip.length);
        lines.add(l);
        colours.add(colour);
    }

    /** Everything the JEI page writes under the slots, one line each (with the details in tooltips). */
    private void buildLines() {
        if (!r.secret) {
            long total = (long) r.energy * r.duration;
            line(machine.generator ? 0xFF40C060 : 0xFFC04040, Component.translatable("emi.theoreticalsciences.total_energy",
                    (machine.generator ? "+" : "") + String.format("%,d", total)));
        }
        if (r.hasResearch()) line(0xFF9060C0, QHEmi.research(r.research));
        if (r.minTier > 0) line(0xFFFFB040, Component.translatable("jei.theoreticalsciences.min_tier", r.minTier));
        if (r.insight > 0) line(0xFF60D0FF, Component.translatable("jei.theoreticalsciences.insight", r.insight));
        if (!r.environment.isAny()) {
            var env = r.environment.describe();
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jei.theoreticalsciences.env_title"));
            tip.addAll(env);
            tip.add(Component.translatable("jei.theoreticalsciences.env_hint").withStyle(ChatFormatting.DARK_GRAY));
            line(0xFFC080FF, Component.literal("◈ ").append(env.size() == 1 ? env.get(0) : Component.translatable("jei.theoreticalsciences.env_label", env.size())),
                    tip.toArray(new Component[0]));
        }
        if (machine == MachineType.RESEARCH_NEXUS) line(0xFF80C0FF, Component.translatable("jei.theoreticalsciences.nexus_note"));
        if (machine == MachineType.VACUUM_HARVESTER) line(0xFF60C0D0, Component.translatable("jei.theoreticalsciences.vacuum_note_1"));
        if (!r.physics.isNone() && !r.secret) {
            // the QMD-style operating window: the first window on the page, every window (and the variables) on hover
            List<Component> windows = physicsLines();
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jei.theoreticalsciences.physics_title"));
            tip.addAll(windows);
            r.physics.vars().forEach((k, v) -> tip.add(Component.translatable("physics.theoreticalsciences." + machine.id() + ".v." + k)
                    .append(" = " + com.theoreticalsciences.client.screen.PhysicsPanel.fmt(v)).withStyle(ChatFormatting.GRAY)));
            tip.add(Component.translatable("jei.theoreticalsciences.physics_hint").withStyle(ChatFormatting.DARK_GRAY));
            for (int i = 0; i < windows.size() && i < 2; i++) line(0xFF60E0C0, windows.get(i), tip.toArray(new Component[0]));
        }
        if (r.secret) {
            // the impossible recipe: no result shown, only warnings
            String[] warn = {"jei.theoreticalsciences.secret_1", "jei.theoreticalsciences.secret_2", "jei.theoreticalsciences.secret_3", "jei.theoreticalsciences.secret_4"};
            for (int i = 0; i < warn.length; i++)
                line(i == 0 ? 0xFFFF3040 : 0xFFFF8080, Component.literal("⚠ ").append(Component.translatable(warn[i])));
        }
        var onlyIn = MachineType.allowedDimensions(machine);
        if (onlyIn != null) line(0xFFFFB040, Component.translatable(machine.isMultiblock() ? "jei.theoreticalsciences.multiblock_only_in" : "jei.theoreticalsciences.only_in", MachineType.dimensionList(onlyIn)));
        else if (machine.isMultiblock()) line(0xFF6080A0, Component.translatable("jei.theoreticalsciences.multiblock"));
    }

    /** "⚛ Beam kinetic energy: 2.0 – 3.5 GeV" for every window of the recipe (as on the JEI page). */
    private List<Component> physicsLines() {
        List<Component> out = new ArrayList<>();
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
            String range = w.min() <= -1e300 ? "≤ " + t : w.max() >= 1e300 ? "≥ " + f : f + " – " + t;
            out.add(Component.literal("⚛ ").append(name).append(": " + range + " " + unit));
        }
        return out;
    }

    /** Each tank is drawn full: its capacity is the recipe's own amount. */
    static int cap(EmiIngredient i) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, i.getAmount()));
    }

    private int ox(int x) {
        return x - 4;
    }

    private int oy(int y) {
        return y - (MachineLayout.TOP - 4) + HEAD;
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        var font = Minecraft.getInstance().font;
        // energy per tick (left) and time (right) in the band at the top
        String energy = (machine.generator ? "+" : "") + String.format("%,d FE/t", r.energy);
        String dur = String.format("%.1fs", r.duration / 20.0);
        w.addText(Component.literal(energy), 2, 1, machine.generator ? 0xFF40C060 : 0xFFC04040, false);
        w.addText(Component.literal(dur), width - 2 - font.width(dur), 1, 0xFF606878, false);
        w.addTooltipText(List.of(Component.literal(String.format("%,d ticks", r.duration))), width - 2 - font.width(dur), 0, font.width(dur), 10);

        int tankH = layout.tankH;
        for (int i = 0; i < fluidIns.size(); i++)
            w.addTank(fluidIns.get(i), ox(layout.fluidInX[i]) - 1, oy(layout.fluidInY[i]) - 1, 18, tankH + 2, cap(fluidIns.get(i)));
        for (int i = 0; i < itemIns.size(); i++) w.addSlot(itemIns.get(i), ox(layout.itemInX[i]) - 1, oy(layout.itemInY[i]) - 1);
        for (int c = 0; c < cats.size(); c++) {
            int s = itemIns.size() + c;
            w.addSlot(cats.get(c), ox(layout.itemInX[s]) - 1, oy(layout.itemInY[s]) - 1).catalyst(true)
                    .appendTooltip(Component.translatable("jei.theoreticalsciences.not_consumed").withStyle(ChatFormatting.GOLD));
        }
        // the unused slots of the machine, empty, so the page looks like the machine
        for (int i = itemIns.size() + cats.size(); i < machine.itemInputs; i++) w.addSlot(EmiStack.EMPTY, ox(layout.itemInX[i]) - 1, oy(layout.itemInY[i]) - 1);
        w.addFillingArrow(ox(layout.arrowX), oy(layout.arrowY), Math.max(50, r.duration * 50));
        if (r.secret) {
            // the impossible recipe: its results stay hidden
            for (int i = 0; i < machine.itemOutputs; i++) {
                int x = ox(layout.itemOutX[i]), y = oy(layout.itemOutY[i]);
                w.addDrawable(x - 1, y - 1, 18, 18, (g, mx, my, d) -> {
                    long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
                    g.fill(0, 0, 18, 18, 0xFF8B8B8B);
                    g.fill(1, 1, 17, 17, 0xFF200008);
                    g.drawString(font, "?", 6, 5, (t / 10) % 2 == 0 ? 0xFFFF3040 : 0xFFB00020, false);
                });
            }
        } else {
            for (int i = 0; i < machine.itemOutputs; i++)
            {
                var os = w.addSlot(i < itemOuts.size() ? itemOuts.get(i) : EmiStack.EMPTY, ox(layout.itemOutX[i]) - 1, oy(layout.itemOutY[i]) - 1).recipeContext(this);
                if (machine == MachineType.VACUUM_HARVESTER) os.appendTooltip(Component.translatable("jei.theoreticalsciences.vacuum_slot").withStyle(ChatFormatting.AQUA));
            }
            for (int i = 0; i < fluidOuts.size(); i++)
                w.addTank(fluidOuts.get(i), ox(layout.fluidOutX[i]) - 1, oy(layout.fluidOutY[i]) - 1, 18, tankH + 2, cap(fluidOuts.get(i)))
                        .recipeContext(this);
        }
        int y = HEAD + layout.areaH + 7;
        for (int i = 0; i < lines.size(); i++) {
            Component[] l = lines.get(i);
            y = QHEmi.line(w, l[0], 2, y, colours.get(i), List.of(l).subList(1, l.length));
        }
    }
}
