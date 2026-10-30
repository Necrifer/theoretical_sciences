package com.theoreticalsciences.client.codex;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.converge.ConvergenceRecipe;
import com.theoreticalsciences.extreme.CompactionRecipe;
import com.theoreticalsciences.extreme.ExtremeRecipe;
import com.theoreticalsciences.extreme.ExtremeShapedRecipe;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.transcend.CoreForge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.*;

/**
 * Round 30: recipe pages in the Codex, drawn without JEI. Any recipe the game has loaded can be shown: crafting and
 * Extreme Assembly Table grids (up to 9x9), the machines' recipes (items with their amounts, fluids, energy, coil tier),
 * the Singularity Compactor, Convergence, and the reborn Core's Forge. Each becomes a {@link View}: slots laid out to
 * fit the page width, and lines of text below.
 */
public final class CodexRecipes {
    /** A slot on the page (x, y relative to the recipe's top left; size is the cell size). */
    public record Slot(int x, int y, int size, List<ItemStack> options, String label, boolean output) {
        public ItemStack current() {
            if (options.isEmpty()) return ItemStack.EMPTY;
            return options.get((int) ((System.currentTimeMillis() / 1000) % options.size()));
        }
    }

    public static final class View {
        public Component title = Component.empty();
        public ItemStack station = ItemStack.EMPTY;
        public final List<Slot> slots = new ArrayList<>();
        /** Where the arrow is drawn (relative), or -1. */
        public int arrowX = -1, arrowY;
        public final List<Component> lines = new ArrayList<>();
        public int gridHeight;
        public int height;
    }

    private static Map<Item, List<Object>> index;
    private static Object indexedFor;
    private static int indexedGen = -1;

    public static void clear() {
        index = null;
    }

    private static HolderLookup.Provider regs() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.registryAccess() : null;
    }

    private static RecipeManager manager() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getRecipeManager() : null;
    }

    /** Every recipe that makes the item (the mod's own first; at most a few of the rest). */
    public static List<Object> producing(Item item) {
        RecipeManager rm = manager();
        if (rm == null) return List.of();
        // (round 31: rebuilt too when a datapack changes the Core Forge's recipes)
        if (index == null || indexedFor != rm || indexedGen != com.theoreticalsciences.custom.PackData.generation()) build(rm);
        return index.getOrDefault(item, List.of());
    }

    /** A recipe by id (from a recipe page), or null. */
    public static Object byId(String id) {
        RecipeManager rm = manager();
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rm == null || rl == null) return null;
        return rm.byKey(rl).orElse(null);
    }

    private static int rank(Object r) {
        if (r instanceof CoreForge.Recipe) return 0;
        if (!(r instanceof RecipeHolder<?> h)) return 9;
        Recipe<?> v = h.value();
        if (v instanceof QHRecipe || v instanceof ExtremeRecipe || v instanceof ConvergenceRecipe || v instanceof CompactionRecipe) return 1;
        if (v instanceof CraftingRecipe) return 2;
        if (v instanceof AbstractCookingRecipe) return 3;
        return 4;               // stonecutting and the rest
    }

    private static void build(RecipeManager rm) {
        Map<Item, List<Object>> m = new HashMap<>();
        HolderLookup.Provider regs = regs();
        for (RecipeHolder<?> h : rm.getRecipes()) {
            Recipe<?> r = h.value();
            boolean ours = h.id().getNamespace().equals(TheoreticalSciences.MODID);
            List<ItemStack> outs = new ArrayList<>();
            try {
                if (r instanceof QHRecipe q) {
                    if (q.secret) continue;
                    outs.addAll(q.itemOutputs);
                    for (FluidStack f : q.fluidOutputs) {
                        Item cell = cellOf(f);
                        if (cell != Items.AIR) outs.add(new ItemStack(cell));
                    }
                } else if (regs != null) outs.add(r.getResultItem(regs));
            } catch (Exception ignored) {
                continue;
            }
            for (ItemStack o : outs) {
                if (o.isEmpty()) continue;
                if (!ours && !BuiltInRegistries.ITEM.getKey(o.getItem()).getNamespace().equals(TheoreticalSciences.MODID)) continue;
                m.computeIfAbsent(o.getItem(), k -> new ArrayList<>()).add(h);
            }
        }
        try {
            for (CoreForge.Recipe r : CoreForge.base()) {          // the mod's own, with any datapack changes (round 31)
                ItemStack o = r.result();
                if (!o.isEmpty()) m.computeIfAbsent(o.getItem(), k -> new ArrayList<>()).add(r);
            }
        } catch (Exception ignored) {}
        for (List<Object> l : m.values()) l.sort(Comparator.comparingInt(CodexRecipes::rank));
        index = m;
        indexedFor = rm;
        indexedGen = com.theoreticalsciences.custom.PackData.generation();
    }

    /** The containment cell that shows a fluid of this mod (its page in the Codex). */
    private static Item cellOf(FluidStack f) {
        ResourceLocation k = BuiltInRegistries.FLUID.getKey(f.getFluid());
        if (!k.getNamespace().equals(TheoreticalSciences.MODID)) return f.getFluid().getBucket();
        String p = k.getPath().replace("flowing_", "");
        return BuiltInRegistries.ITEM.get(TheoreticalSciences.id(p + "_cell"));
    }

    private static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }

    public static String amount(long n) {
        if (n >= 10_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return (n / 1000) + "k";
        if (n >= 1000) return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        return Long.toString(n);
    }

    private static String grouped(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    private static List<ItemStack> opts(Ingredient i) {
        if (i == null || i.isEmpty()) return List.of();
        return Arrays.asList(i.getItems());
    }

    private static List<ItemStack> one(ItemStack s) {
        return s.isEmpty() ? List.of() : List.of(s);
    }

    /** Places a grid of inputs, an arrow, and the outputs; returns the grid's height. */
    private static void grid(View v, int width, int cols, int rows, List<List<ItemStack>> cells, List<String> labels, List<List<ItemStack>> outs, List<String> outLabels) {
        int outCols = Math.max(1, Math.min(2, outs.size()));
        int cell = 18;
        int need = cols * cell + 26 + outCols * cell;
        if (need > width) cell = Math.max(8, (width - 26 - outCols * 18) / Math.max(1, cols));
        int gw = cols * cell;
        int total = gw + 26 + outCols * 18;
        int x0 = Math.max(0, (width - total) / 2);
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int k = r * cols + c;
                if (k >= cells.size()) continue;
                v.slots.add(new Slot(x0 + c * cell, r * cell, cell, cells.get(k), labels == null ? null : labels.get(k), false));
            }
        int gh = rows * cell;
        int orows = (outs.size() + outCols - 1) / outCols;
        int oy = Math.max(0, (gh - orows * 18) / 2);
        int ox = x0 + gw + 26;
        for (int k = 0; k < outs.size(); k++)
            v.slots.add(new Slot(ox + (k % outCols) * 18, oy + (k / outCols) * 18, 18, outs.get(k), outLabels == null ? null : outLabels.get(k), true));
        v.arrowX = x0 + gw + 5;
        v.arrowY = Math.max(0, gh / 2 - 4);
        v.gridHeight = Math.max(gh, orows * 18);
    }

    /** Lays a recipe out for a page {@code width} pixels wide. */
    public static View view(Object recipe, int width, Font font) {
        View v = new View();
        HolderLookup.Provider regs = regs();
        if (recipe instanceof CoreForge.Recipe fr) {
            v.title = Component.translatable("gui.theoreticalsciences.codex.forge");
            v.station = new ItemStack(item(TheoreticalSciences.MODID + ":forged_omnipotence_core"));
            List<List<ItemStack>> cells = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            boolean kept = false;
            for (CoreForge.Ing g : fr.in()) {
                cells.add(one(g.stack(1)));
                labels.add(g.count() == 0 ? "∞" : g.count() > 1 ? amount(g.count()) : null);
                kept |= g.count() == 0;
            }
            int cols = Math.min(cells.size(), Math.max(1, Math.min(4, (width - 62) / 18)));
            grid(v, width, cols, (cells.size() + cols - 1) / cols, cells, labels, List.of(one(fr.result())), List.of(fr.out().count() > 1 ? amount(fr.out().count()) : ""));
            if (kept) v.lines.add(Component.literal("∞ = ").append(Component.translatable("gui.theoreticalsciences.codex.kept")).withStyle(ChatFormatting.GRAY));
            finish(v);
            return v;
        }
        if (!(recipe instanceof RecipeHolder<?> h)) return null;
        Recipe<?> r = h.value();
        ItemStack result = ItemStack.EMPTY;
        try {
            if (regs != null) result = r.getResultItem(regs);
        } catch (Exception ignored) {}
        String resLabel = result.getCount() > 1 ? amount(result.getCount()) : null;
        if (r instanceof QHRecipe q) {
            MachineType mt = null;
            String tid = q.typeId().contains(":") ? q.typeId().substring(q.typeId().indexOf(':') + 1) : q.typeId();
            for (MachineType t : MachineType.values()) if (t.recipeType.equals(tid)) { mt = t; break; }
            if (mt != null) {
                v.station = new ItemStack(item(TheoreticalSciences.MODID + ":" + mt.id()));
                v.title = v.station.isEmpty() ? Component.literal(mt.id()) : v.station.getHoverName();
            } else v.title = Component.literal(tid);
            List<List<ItemStack>> cells = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (SizedIngredient s : q.itemInputs) {
                cells.add(opts(s.ingredient()));
                labels.add(s.count() > 1 ? amount(s.count()) : null);
            }
            for (Ingredient c : q.catalysts) {
                cells.add(opts(c));
                labels.add("∞");
            }
            List<List<ItemStack>> outs = new ArrayList<>();
            List<String> outLabels = new ArrayList<>();
            for (int k = 0; k < q.itemOutputs.size(); k++) {
                ItemStack o = q.itemOutputs.get(k);
                outs.add(one(o));
                float ch = q.chance(k);
                outLabels.add(ch < 1F ? Math.round(ch * 100) + "%" : o.getCount() > 1 ? amount(o.getCount()) : null);
            }
            int cols = cells.isEmpty() ? 0 : Math.min(cells.size(), Math.max(1, Math.min(4, (width - 62) / 18)));
            if (cols > 0 || !outs.isEmpty())
                grid(v, width, Math.max(1, cols), cols == 0 ? 0 : (cells.size() + cols - 1) / cols, cells, labels, outs, outLabels);
            for (SizedFluidIngredient f : q.fluidInputs) {
                FluidStack[] fs = f.getFluids();
                if (fs.length > 0) v.lines.add(Component.literal("+ " + grouped(f.amount()) + " mB ").append(fs[0].getHoverName()).withStyle(ChatFormatting.AQUA));
            }
            for (FluidStack f : q.fluidOutputs)
                v.lines.add(Component.literal("→ " + grouped(f.getAmount()) + " mB ").append(f.getHoverName()).withStyle(ChatFormatting.GREEN));
            if (!q.catalysts.isEmpty()) v.lines.add(Component.literal("∞ = ").append(Component.translatable("gui.theoreticalsciences.codex.kept")).withStyle(ChatFormatting.GRAY));
            if (q.energy > 0)
                v.lines.add(Component.translatable("gui.theoreticalsciences.codex.energy", grouped(q.energy),
                        com.theoreticalsciences.research.ResearchService.formatTime(Math.max(1, q.duration / 20))).withStyle(ChatFormatting.YELLOW));
            if (q.minTier > 0) v.lines.add(Component.translatable("gui.theoreticalsciences.codex.min_tier", q.minTier + 1).withStyle(ChatFormatting.GOLD));
            if (q.hasResearch()) {
                ResourceLocation rid = ResourceLocation.tryParse(q.research.contains(":") ? q.research : TheoreticalSciences.MODID + ":" + q.research);
                var d = rid == null ? null : com.theoreticalsciences.client.ClientResearch.defs.get(rid);
                v.lines.add(Component.translatable("gui.theoreticalsciences.codex.research_needed", d != null ? d.titleComponent(rid) : Component.literal(q.research))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            finish(v);
            return v;
        }
        if (r instanceof CompactionRecipe c) {
            v.station = new ItemStack(item(TheoreticalSciences.MODID + ":singularity_compactor"));
            v.title = v.station.getHoverName();
            grid(v, width, 1, 1, List.of(opts(c.ingredient())), List.of(amount(c.count())), List.of(one(c.result())), Collections.singletonList(null));
            List<ItemStack> o = opts(c.ingredient());
            v.lines.add(Component.literal(grouped(c.count()) + " ").append(o.isEmpty() ? Component.literal("?") : o.get(0).getHoverName()).withStyle(ChatFormatting.AQUA));
            finish(v);
            return v;
        }
        if (r instanceof ConvergenceRecipe c) {
            v.station = new ItemStack(item(TheoreticalSciences.MODID + ":convergence_altar"));
            v.title = v.station.getHoverName();
            List<List<ItemStack>> cells = new ArrayList<>();
            cells.add(opts(c.core));
            for (Ingredient p : c.pedestals) cells.add(opts(p));
            int cols = Math.min(4, cells.size());
            grid(v, width, cols, (cells.size() + cols - 1) / cols, cells, null, List.of(one(c.result)), Collections.singletonList(null));
            if (c.energy > 0) v.lines.add(Component.translatable("gui.theoreticalsciences.codex.energy_total", grouped(c.energy),
                    com.theoreticalsciences.research.ResearchService.formatTime(Math.max(1, c.duration / 20))).withStyle(ChatFormatting.YELLOW));
            finish(v);
            return v;
        }
        // grids: crafting tables, the Extreme Assembly Table, and anything else with ingredients
        int w = 0, hgt = 0;
        List<Ingredient> ings;
        if (r instanceof ExtremeShapedRecipe es) {
            w = es.width();
            hgt = es.height();
            ings = es.cells();
        } else if (r instanceof ShapedRecipe sr) {
            w = sr.getWidth();
            hgt = sr.getHeight();
            ings = sr.getIngredients();
        } else ings = r.getIngredients();
        if (r instanceof ExtremeRecipe) v.station = new ItemStack(item(TheoreticalSciences.MODID + ":extreme_assembly_table"));
        else if (r instanceof CraftingRecipe) v.station = new ItemStack(Items.CRAFTING_TABLE);
        else if (r instanceof StonecutterRecipe) v.station = new ItemStack(Items.STONECUTTER);
        else if (r instanceof BlastingRecipe) v.station = new ItemStack(Items.BLAST_FURNACE);
        else if (r instanceof AbstractCookingRecipe) v.station = new ItemStack(Items.FURNACE);
        v.title = v.station.isEmpty() ? Component.literal(h.id().toString()) : v.station.getHoverName();
        if (w <= 0) {
            int n = Math.max(1, ings.size());
            w = Math.min(9, (int) Math.ceil(Math.sqrt(n)));
            hgt = (n + w - 1) / w;
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        for (Ingredient i : ings) cells.add(opts(i));
        grid(v, width, w, hgt, cells, null, List.of(one(result)), Collections.singletonList(resLabel));
        finish(v);
        return v;
    }

    private static void finish(View v) {
        v.height = 14 + v.gridHeight + (v.lines.isEmpty() ? 0 : 4 + v.lines.size() * 10);
    }

    /** Draws a recipe at x, y (its title row first); returns the slot under the mouse, or null. */
    public static Slot render(GuiGraphics g, Font font, View v, int x, int y, int width, double mx, double my, int accent) {
        // title row: the station's icon at half size and its name
        int tx = x;
        if (!v.station.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.75F, 0.75F, 1F);
            g.renderItem(v.station, 0, 0);
            g.pose().popPose();
            tx += 14;
        }
        com.theoreticalsciences.client.screen.TextFit.draw(g, font, v.title.copy().withStyle(ChatFormatting.BOLD), tx, y + 2, width - (tx - x), 0xFFFFC870);
        int gy = y + 14;
        Slot hover = null;
        for (Slot s : v.slots) {
            int sx = x + s.x, sy = gy + s.y, S = s.size;
            g.fill(sx, sy, sx + S, sy + S, s.output ? 0x60402810 : 0x60102030);
            g.renderOutline(sx, sy, S, S, s.output ? 0xA0FFC060 : com.theoreticalsciences.client.screen.HoloStyle.argb(0x90, accent));
            ItemStack st = s.current();
            if (!st.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(sx + 1, sy + 1, 0);
                float k = (S - 2) / 16F;
                g.pose().scale(k, k, 1F);
                g.renderItem(st, 0, 0);
                if (s.label != null && !s.label.isEmpty()) g.renderItemDecorations(font, st, 0, 0, s.label);
                g.pose().popPose();
            }
            if (mx >= sx && my >= sy && mx < sx + S && my < sy + S) {
                hover = s;
                g.fill(sx + 1, sy + 1, sx + S - 1, sy + S - 1, 0x40FFFFFF);
            }
        }
        if (v.arrowX >= 0) {
            int ax = x + v.arrowX, ay = gy + v.arrowY;
            int col = com.theoreticalsciences.client.screen.HoloStyle.argb(0xE0, accent);
            g.fill(ax, ay + 3, ax + 12, ay + 5, col);
            for (int k = 0; k < 4; k++) g.fill(ax + 12 + k, ay + k, ax + 13 + k, ay + 8 - k, col);
        }
        int ly = gy + v.gridHeight + 4;
        for (Component c : v.lines) {
            com.theoreticalsciences.client.screen.TextFit.draw(g, font, c, x, ly, width, 0xFFD8E4F0);
            ly += 10;
        }
        return hover;
    }

    private CodexRecipes() {}
}
