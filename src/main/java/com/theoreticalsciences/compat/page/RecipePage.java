package com.theoreticalsciences.compat.page;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineLayout;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.science.Wear;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.2.0: one machine recipe's page, described once - where every slot and tank goes, and every line of text with its
 * tooltip - for both recipe viewers to draw (compat/jei/MachineRecipeCategory and compat/emi/MachineEmiRecipe). A new
 * mechanic adds its lines here and shows in JEI and EMI alike.
 * <p>
 * Laid out like the machine's own screen: the tanks and the input grid, the progress arrow with the gold catalyst slot
 * under it, the outputs; then the lines. Built once per recipe and kept (nothing is worked out while drawing). A pack
 * (or a later round) can give any recipe a note of its own with the lang key
 * {@code recipe_note.<namespace>.<recipe path, '/' as '.'>}.
 */
public final class RecipePage {
    /** A band at the top for the energy per tick and the time. */
    public static final int HEAD = 11;
    public static final int LINE_H = 10;

    public enum Kind { ITEM_IN, CATALYST, ITEM_OUT, FLUID_IN, FLUID_OUT, EMPTY }

    /**
     * A slot or tank at page coordinates (the top-left of its 16x16 content; a tank is 16 wide and {@link #tankH} high).
     * {@code index} is into the recipe's list of that kind (catalysts: into {@code r.catalysts}).
     */
    public record Slot(Kind kind, int index, int x, int y, boolean catalystSlot, List<Component> tip) {}

    public record Line(Component text, int colour, List<Component> tip) {}

    public final MachineType machine;
    public final QHRecipe r;
    @Nullable public final ResourceLocation id;
    public final MachineLayout layout;
    public final int width, tankH;
    public final List<Slot> slots = new ArrayList<>();
    public final List<Line> lines = new ArrayList<>();
    /** Where the progress arrow is (page coordinates), and where the lines start. */
    public final int arrowX, arrowY, linesTop;
    private List<List<FormattedCharSequence>> wrapped;
    private int height = -1;

    private static final Map<QHRecipe, RecipePage> CACHE = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<QHRecipe, ResourceLocation> IDS = Collections.synchronizedMap(new IdentityHashMap<>());

    /** The viewers tell which id each recipe has (for its own note), as they register them. */
    public static void know(QHRecipe r, ResourceLocation id) {
        IDS.put(r, id);
    }

    public static RecipePage of(MachineType machine, QHRecipe r) {
        return CACHE.computeIfAbsent(r, k -> new RecipePage(machine, k, IDS.get(k)));
    }

    /** After a resource or recipe reload: everything is laid out again. */
    public static void clear() {
        CACHE.clear();
    }

    private static int ox(int x) {
        return x - 4;
    }

    private static int oy(int y) {
        return y - (MachineLayout.TOP - 4) + HEAD;
    }

    private RecipePage(MachineType machine, QHRecipe r, @Nullable ResourceLocation id) {
        this.machine = machine;
        this.r = r;
        this.id = id;
        this.layout = new MachineLayout(machine);
        this.width = Math.max(160, layout.width - 4);
        this.tankH = layout.tankH;
        this.arrowX = ox(layout.arrowX);
        this.arrowY = oy(layout.arrowY);
        int bottom = Math.max(MachineLayout.TOP + layout.areaH, layout.catY + 17);
        this.linesTop = oy(bottom) + 4;
        buildSlots();
        buildLines();
    }

    // ------------------------------------------------------------------ slots
    private void buildSlots() {
        int nIn = Math.min(r.itemInputs.size(), machine.itemInputs);
        for (int i = 0; i < nIn; i++) slots.add(new Slot(Kind.ITEM_IN, i, ox(layout.itemInX[i]), oy(layout.itemInY[i]), false, List.of()));
        // the catalyst that wears goes in the catalyst slot (or else the first); any other one in the input grid, as before
        int first = 0;
        for (int c = 0; c < r.catalysts.size(); c++) if (profile(r.catalysts.get(c)) != null) { first = c; break; }
        int used = nIn;
        for (int c = 0; c < r.catalysts.size(); c++) {
            List<Component> tip = catalystTip(r.catalysts.get(c), c == first);
            if (c == first) slots.add(new Slot(Kind.CATALYST, c, ox(layout.catX), oy(layout.catY), true, tip));
            else if (used < machine.itemInputs) {
                slots.add(new Slot(Kind.CATALYST, c, ox(layout.itemInX[used]), oy(layout.itemInY[used]), false, tip));
                used++;
            }
        }
        for (int i = used; i < machine.itemInputs; i++) slots.add(new Slot(Kind.EMPTY, i, ox(layout.itemInX[i]), oy(layout.itemInY[i]), false, List.of()));
        if (r.catalysts.isEmpty()) slots.add(new Slot(Kind.EMPTY, -1, ox(layout.catX), oy(layout.catY), true, List.of()));
        for (int i = 0; i < machine.itemOutputs; i++) {
            List<Component> tip = new ArrayList<>();
            if (i < r.itemOutputs.size()) {
                float ch = r.chance(i);
                if (ch < 1F) tip.add(Component.translatable("jei.theoreticalsciences.chance", String.format(Locale.ROOT, "%.1f", ch * 100)).withStyle(ChatFormatting.AQUA));
                if (machine == MachineType.VACUUM_HARVESTER) tip.add(Component.translatable("jei.theoreticalsciences.vacuum_slot").withStyle(ChatFormatting.AQUA));
            }
            slots.add(new Slot(i < r.itemOutputs.size() ? Kind.ITEM_OUT : Kind.EMPTY, i, ox(layout.itemOutX[i]), oy(layout.itemOutY[i]), false, tip));
        }
        for (int i = 0; i < Math.min(r.fluidInputs.size(), machine.fluidInputs); i++)
            slots.add(new Slot(Kind.FLUID_IN, i, ox(layout.fluidInX[i]), oy(layout.fluidInY[i]), false, List.of()));
        for (int i = 0; i < Math.min(r.fluidOutputs.size(), machine.fluidOutputs); i++)
            slots.add(new Slot(Kind.FLUID_OUT, i, ox(layout.fluidOutX[i]), oy(layout.fluidOutY[i]), false, List.of()));
    }

    @Nullable
    private static Wear.Profile profile(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        return items.length == 0 ? null : Wear.profile(items[0]);
    }

    private static Component machineName(String id) {
        return Component.translatable("block." + TheoreticalSciences.MODID + "." + id);
    }

    /** What the catalyst does to itself: wears (and is restored where), decays, or never changes. */
    private static Component wearText(Ingredient ing) {
        Wear.Profile p = profile(ing);
        if (p == null || (!p.source() && p.life() <= 0)) return Component.translatable("page.theoreticalsciences.cat.permanent");
        if (p.exposed()) return Component.translatable("page.theoreticalsciences.cat.exposed");
        if (p.source()) {
            ItemStack s = ing.getItems()[0];
            double half = Wear.halfLife(s, p) / Wear.DECAY_SCALE;
            return Component.translatable("page.theoreticalsciences.cat.decays", com.theoreticalsciences.science.Wear.span(half));
        }
        // 1.2.0: the Living Catalyst is not used up: each run that feeds it heals a run of its wear
        boolean heals = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ing.getItems()[0].getItem()).getPath().equals("living_catalyst");
        Component c = heals ? Component.translatable("page.theoreticalsciences.cat.wears_heals", p.life())
                : p.restore().isEmpty() ? Component.translatable("page.theoreticalsciences.cat.wears_used", p.life())
                : Component.translatable("page.theoreticalsciences.cat.wears", p.life(), machineName(p.restore()));
        return p.poison() ? c.copy().append(Component.translatable("page.theoreticalsciences.cat.poison", Wear.POISON_RUNS)) : c;
    }

    private static List<Component> catalystTip(Ingredient ing, boolean inSlot) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable("jei.theoreticalsciences.not_consumed").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable(inSlot ? "page.theoreticalsciences.cat.in_slot" : "page.theoreticalsciences.cat.in_input").withStyle(ChatFormatting.YELLOW));
        tip.add(wearText(ing).copy().withStyle(ChatFormatting.GRAY));
        return tip;
    }

    // ------------------------------------------------------------------ lines
    private void line(int colour, Component text, List<Component> tip) {
        lines.add(new Line(text, colour, tip));
    }

    private void line(int colour, Component text) {
        line(colour, text, List.of());
    }

    private void buildLines() {
        if (!r.secret) {
            long total = (long) r.energy * r.duration;
            line(machine.generator ? 0xFF40C060 : 0xFFC04040, Component.translatable("emi.theoreticalsciences.total_energy",
                    (machine.generator ? "+" : "") + String.format(Locale.ROOT, "%,d", total)));
        }
        if (r.hasResearch()) {
            ResourceLocation rid = ResourceLocation.tryParse(r.research);
            line(0xFF9060C0, Component.translatable("jei.theoreticalsciences.research",
                    rid == null ? Component.literal(r.research) : Component.translatableWithFallback("research." + rid.getNamespace() + "." + rid.getPath(), rid.getPath())));
        }
        if (r.minTier > 0) line(0xFFFFB040, Component.translatable("jei.theoreticalsciences.min_tier", r.minTier));
        if (r.insight > 0) line(0xFF60D0FF, Component.translatable("jei.theoreticalsciences.insight", r.insight));
        // the catalyst, and what happens to it
        for (Slot s : slots) {
            if (s.kind() != Kind.CATALYST || !s.catalystSlot()) continue;
            Ingredient ing = r.catalysts.get(s.index());
            ItemStack[] items = ing.getItems();
            Component name = items.length == 0 ? Component.literal("?") : items[0].getHoverName();
            line(0xFFFFC060, Component.literal("◆ ").append(Component.translatable("page.theoreticalsciences.cat.line", name, wearText(ing))), s.tip());
        }
        // a restoring recipe: the worn item in, the same item out new
        if (r.itemInputs.size() >= 1 && r.itemOutputs.size() == 1) {
            ItemStack out = r.itemOutputs.get(0);
            Wear.Profile p = Wear.profile(out);
            if (p != null && !p.source() && r.itemInputs.get(0).ingredient().test(out))
                line(0xFF80FFC0, Component.translatable("page.theoreticalsciences.restores", out.getHoverName()));
        }
        // anything Technetium-99m in or out
        boolean tc = false;
        for (ItemStack o : r.itemOutputs) tc |= isExposed(o);
        for (var in : r.itemInputs) for (ItemStack s : in.ingredient().getItems()) tc |= isExposed(s);
        if (tc) line(0xFF80E0FF, Component.translatable("page.theoreticalsciences.technetium"));
        // the purity tag: an assay made here, or a feed that ought to be assayed for this recipe's catalyst
        boolean assays = false;
        for (var o : r.fluidOutputs) assays |= o.has(Wear.ASSAYED.get());
        if (assays) line(0xFF80FF80, Component.translatable("page.theoreticalsciences.assays"));
        if (!r.environment.isAny()) {
            var env = r.environment.describe();
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jei.theoreticalsciences.env_title"));
            tip.addAll(env);
            tip.add(Component.translatable("jei.theoreticalsciences.env_hint").withStyle(ChatFormatting.DARK_GRAY));
            line(0xFFC080FF, Component.literal("◈ ").append(env.size() == 1 ? env.get(0) : Component.translatable("jei.theoreticalsciences.env_label", env.size())), tip);
        }
        if (machine == MachineType.RESEARCH_NEXUS) line(0xFF80C0FF, Component.translatable("jei.theoreticalsciences.nexus_note"));
        if (machine == MachineType.VACUUM_HARVESTER) line(0xFF60C0D0, Component.translatable("jei.theoreticalsciences.vacuum_note_1"));
        if (id != null) {
            String key = "recipe_note." + id.getNamespace() + "." + id.getPath().replace('/', '.');
            if (I18n.exists(key)) line(0xFFE0E0A0, Component.literal("✎ ").append(Component.translatable(key)));
        }
        if (!r.physics.isNone() && !r.secret) {
            // the operating window: every window on the page (the reaction conditions), the variables on hover
            List<Component> windows = physicsLines();
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("jei.theoreticalsciences.physics_title"));
            tip.addAll(windows);
            r.physics.vars().forEach((k, v) -> tip.add(Component.translatable("physics.theoreticalsciences." + machine.id() + ".v." + k)
                    .append(" = " + com.theoreticalsciences.client.screen.PhysicsPanel.fmt(v)).withStyle(ChatFormatting.GRAY)));
            tip.add(Component.translatable("jei.theoreticalsciences.physics_hint").withStyle(ChatFormatting.DARK_GRAY));
            for (Component w : windows) line(0xFF60E0C0, w, tip);
        }
        if (r.secret) {
            String[] warn = {"jei.theoreticalsciences.secret_1", "jei.theoreticalsciences.secret_2", "jei.theoreticalsciences.secret_3", "jei.theoreticalsciences.secret_4"};
            for (int i = 0; i < warn.length; i++)
                line(i == 0 ? 0xFFFF3040 : 0xFFFF8080, Component.literal("⚠ ").append(Component.translatable(warn[i])));
        }
        var onlyIn = MachineType.allowedDimensions(machine);
        if (onlyIn != null) line(0xFFFFB040, Component.translatable(machine.isMultiblock() ? "jei.theoreticalsciences.multiblock_only_in" : "jei.theoreticalsciences.only_in", MachineType.dimensionList(onlyIn)));
        else if (machine.isMultiblock()) line(0xFF6080A0, Component.translatable("jei.theoreticalsciences.multiblock"));
    }

    private static boolean isExposed(ItemStack s) {
        Wear.Profile p = Wear.profile(s);
        return p != null && p.exposed();
    }

    /** "⚛ Beam kinetic energy: 2.0 – 3.5 GeV" for every window of the recipe. */
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

    // ------------------------------------------------------------------ text layout (worked out once)
    /** Each line wrapped to the page. */
    public List<List<FormattedCharSequence>> wrapped() {
        if (wrapped == null) {
            Font font = Minecraft.getInstance().font;
            List<List<FormattedCharSequence>> w = new ArrayList<>();
            for (Line l : lines) {
                List<FormattedCharSequence> parts = font.split(l.text(), Math.max(20, width - 4));
                w.add(parts.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : parts);
            }
            wrapped = w;
        }
        return wrapped;
    }

    public int height() {
        if (height < 0) {
            int rows = 0;
            for (var l : wrapped()) rows += l.size();
            height = linesTop + rows * LINE_H + 2;
        }
        return height;
    }

    /** The line under page point y (or -1). */
    public int lineAt(double y) {
        int yy = linesTop;
        var w = wrapped();
        for (int i = 0; i < w.size(); i++) {
            int h = w.get(i).size() * LINE_H;
            if (y >= yy - 1 && y < yy + h - 1) return i;
            yy += h;
        }
        return -1;
    }
}
