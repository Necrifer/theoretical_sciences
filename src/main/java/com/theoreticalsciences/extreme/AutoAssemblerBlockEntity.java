package com.theoreticalsciences.extreme;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Autonomous Extreme Assembler (round 25): an Extreme Assembly Table without a player. Anything may push items in
 * (an AE2 Pattern Provider with a processing pattern, hoppers, pipes) - it finds the Extreme recipe those items make,
 * lays them out itself and crafts, and the result (with any leftovers) waits in its output for whatever pulls it out.
 * Every Extreme recipe works: shaped, shapeless and the ones that follow the game (singularities, clusters,
 * collections), which are matched by exactly what was delivered.
 */
public class AutoAssemblerBlockEntity extends BlockEntity {
    public static final int IN = 81, OUT = 9, INTERVAL = 10;
    final ItemStackHandler input = new ItemStackHandler(IN) {
        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
            setChanged();
        }
    };
    final ItemStackHandler output = new ItemStackHandler(OUT) {
        @Override
        protected void onContentsChanged(int slot) {
            dirty = true;
            setChanged();
        }
    };
    /** What the outside world sees: insert into the input, extract from the output. */
    public final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return IN + OUT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < IN ? input.getStackInSlot(slot) : output.getStackInSlot(slot - IN);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < IN ? input.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot < IN ? ItemStack.EMPTY : output.extractItem(slot - IN, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < IN;
        }
    };
    boolean dirty = true;
    int cooldown;
    /** the last thing that happened, for the player who looks (0 idle, 1 crafted, 2 no recipe matches, 3 output full) */
    int status;
    @Nullable
    ItemStack lastMade = ItemStack.EMPTY;
    @Nullable
    private RecipeHolder<ExtremeRecipe> lastRecipe;
    private int lastTotal = -1, stable;

    public AutoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ExtremeRegistry.AUTO_ASSEMBLER_BE.get(), pos, state);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        if (--cooldown > 0 || !dirty) return;
        cooldown = INTERVAL;
        dirty = false;
        boolean any = false;
        for (int i = 0; i < IN; i++) if (!input.getStackInSlot(i).isEmpty()) { any = true; break; }
        if (!any) {
            status = 0;
            return;
        }
        // First a recipe that uses up everything delivered (what a pattern provider pushed was one whole craft - so
        // a smaller recipe hiding inside a bigger one's ingredients is never made by mistake), then any recipe.
        // Each pass tries the last recipe first (a pattern provider usually repeats the same craft).
        int total = 0;
        for (int i = 0; i < IN; i++) total += input.getStackInSlot(i).getCount();
        var all = sl.getRecipeManager().getAllRecipesFor(ExtremeRegistry.EXTREME_TYPE.get());
        // the "any recipe" pass for anything but the last recipe waits until the buffer has stopped changing for two
        // checks (more may still be arriving, and a smaller recipe could hide inside a bigger one's ingredients)
        stable = total == lastTotal ? stable + 1 : 0;
        lastTotal = total;
        for (int pass = 0; pass < 2; pass++) {
            int exact = pass == 0 ? total : -1;
            if (lastRecipe != null && tryCraft(sl, lastRecipe, exact)) return;
            if (pass == 1 && stable < 2) {
                dirty = true;                       // look again next interval
                break;
            }
            for (RecipeHolder<ExtremeRecipe> h : all)
                if (h != lastRecipe && tryCraft(sl, h, exact)) {
                    lastRecipe = h;
                    return;
                }
        }
        if (status != 3) status = 2;
    }

    /** One unit per buffered item (for the recipes that are matched by exactly what was delivered). */
    private List<int[]> units() {
        List<int[]> out = new ArrayList<>();          // {slot}
        for (int i = 0; i < IN; i++)
            for (int k = 0; k < input.getStackInSlot(i).getCount(); k++) out.add(new int[]{i});
        return out;
    }

    /** Crafts h if the buffer makes it (and, with exact >= 0, only if it would take exactly that many items). */
    private boolean tryCraft(ServerLevel sl, RecipeHolder<ExtremeRecipe> h, int exact) {
        ExtremeRecipe r = h.value();
        int[] take = new int[IN];
        CraftingInput grid;
        if (r instanceof ExtremeShapedRecipe s) {
            grid = layOut(s.cells(), s.width(), s.height(), take);
        } else if (r instanceof ExtremeShapelessRecipe s) {
            NonNullList<Ingredient> ing = s.getIngredients();
            grid = layOut(ing, 9, (ing.size() + 8) / 9, take);
        } else {
            // dynamic recipes (singularities, clusters, collections): everything delivered, one per cell
            List<int[]> u = units();
            if (u.isEmpty() || u.size() > 81) return false;
            List<ItemStack> cells = new ArrayList<>();
            for (int[] x : u) {
                cells.add(input.getStackInSlot(x[0]).copyWithCount(1));
                take[x[0]]++;
            }
            while (cells.size() < 81) cells.add(ItemStack.EMPTY);
            grid = CraftingInput.of(9, 9, cells);
        }
        if (grid == null) return false;
        if (exact >= 0) {
            int n = 0;
            for (int t : take) n += t;
            if (n != exact) return false;
        }
        if (!r.matches(grid, sl)) return false;
        ItemStack result = r.assemble(grid, sl.registryAccess());
        if (result.isEmpty()) return false;
        NonNullList<ItemStack> rest = r.getRemainingItems(grid);
        // room for the result and the leftovers?
        ItemStackHandler sim = new ItemStackHandler(OUT);
        for (int i = 0; i < OUT; i++) sim.setStackInSlot(i, output.getStackInSlot(i).copy());
        List<ItemStack> outs = new ArrayList<>();
        outs.add(result.copy());
        for (ItemStack x : rest) if (!x.isEmpty()) outs.add(x.copy());
        for (ItemStack o : outs) if (!net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(sim, o, false).isEmpty()) {
            status = 3;
            return true;                                // the right recipe, but nowhere to put it: wait
        }
        for (int i = 0; i < IN; i++) if (take[i] > 0) input.extractItem(i, take[i], false);
        for (ItemStack o : outs) net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(output, o, false);
        status = 1;
        lastMade = result.copy();
        dirty = true;                                   // there may be enough for another
        return true;
    }

    /**
     * Fills a grid for these cells from the buffer: cells needing the same ingredient are served together, the
     * ingredient with the fewest candidates first. Records how many to take from each slot. Null if it cannot.
     */
    @Nullable
    private CraftingInput layOut(List<Ingredient> cells, int w, int h, int[] take) {
        Map<Ingredient, List<Integer>> groups = new IdentityHashMap<>();
        for (int i = 0; i < cells.size(); i++) if (!cells.get(i).isEmpty()) groups.computeIfAbsent(cells.get(i), k -> new ArrayList<>()).add(i);
        if (groups.isEmpty()) return null;
        int[] left = new int[IN];
        for (int i = 0; i < IN; i++) left[i] = input.getStackInSlot(i).getCount();
        List<Map.Entry<Ingredient, List<Integer>>> order = new ArrayList<>(groups.entrySet());
        order.sort(java.util.Comparator.comparingInt(e -> candidates(e.getKey())));
        ItemStack[] placed = new ItemStack[w * h];
        for (var e : order) {
            int need = e.getValue().size(), k = 0;
            for (int s = 0; s < IN && k < need; s++) {
                ItemStack st = input.getStackInSlot(s);
                if (left[s] <= 0 || !e.getKey().test(st)) continue;
                while (left[s] > 0 && k < need) {
                    placed[e.getValue().get(k++)] = st.copyWithCount(1);
                    left[s]--;
                    take[s]++;
                }
            }
            if (k < need) return null;
        }
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack p : placed) items.add(p == null ? ItemStack.EMPTY : p);
        return CraftingInput.of(w, h, items);
    }

    private int candidates(Ingredient ing) {
        int n = 0;
        for (int i = 0; i < IN; i++) if (!input.getStackInSlot(i).isEmpty() && ing.test(input.getStackInSlot(i))) n++;
        return n;
    }

    public Component describe() {
        return switch (status) {
            case 1 -> Component.translatable("message.theoreticalsciences.auto_assembler.made", lastMade == null ? Component.empty() : lastMade.getHoverName());
            case 2 -> Component.translatable("message.theoreticalsciences.auto_assembler.no_recipe");
            case 3 -> Component.translatable("message.theoreticalsciences.auto_assembler.full");
            default -> Component.translatable("message.theoreticalsciences.auto_assembler.idle");
        };
    }

    void dropAll() {
        if (level == null) return;
        for (ItemStackHandler hd : new ItemStackHandler[]{input, output})
            for (int i = 0; i < hd.getSlots(); i++)
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), hd.getStackInSlot(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("In", input.serializeNBT(reg));
        tag.put("Out", output.serializeNBT(reg));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("In")) input.deserializeNBT(reg, tag.getCompound("In"));
        if (tag.contains("Out")) output.deserializeNBT(reg, tag.getCompound("Out"));
        dirty = true;
    }
}
