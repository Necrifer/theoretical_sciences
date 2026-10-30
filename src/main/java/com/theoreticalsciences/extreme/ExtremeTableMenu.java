package com.theoreticalsciences.extreme;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 81 grid slots (0-80), the result (81), then the player inventory (82-117). */
public class ExtremeTableMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18, RESULT_X = 206, RESULT_Y = 90, INV_X = 36, INV_Y = 196, WIDTH = 234, HEIGHT = 278;
    public final ExtremeTableBlockEntity be;
    private final Player player;
    private final ResultContainer result = new ResultContainer();

    public ExtremeTableMenu(int id, Inventory inv, ExtremeTableBlockEntity be) {
        super(ExtremeRegistry.EXTREME_MENU.get(), id);
        this.be = be;
        this.player = inv.player;
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new SlotItemHandler(be.grid, r * 9 + c, GRID_X + c * 18, GRID_Y + r * 18));
        addSlot(new Slot(result, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }

            @Override
            public void onTake(Player p, ItemStack taken) {
                consumeGrid();
                super.onTake(p, taken);
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
        be.viewers.add(this);
        gridChanged();
    }

    public static ExtremeTableMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        if (inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof ExtremeTableBlockEntity be) return new ExtremeTableMenu(id, inv, be);
        throw new IllegalStateException("No Extreme Assembly Table there");
    }

    private CraftingInput input() {
        List<ItemStack> items = new ArrayList<>(81);
        for (int i = 0; i < 81; i++) items.add(be.grid.getStackInSlot(i));
        return CraftingInput.of(9, 9, items);
    }

    Optional<RecipeHolder<ExtremeRecipe>> current() {
        if (be.getLevel() == null) return Optional.empty();
        return be.getLevel().getRecipeManager().getRecipeFor(ExtremeRegistry.EXTREME_TYPE.get(), input(), be.getLevel());
    }

    /**
     * Round 24: the table is also an ordinary crafting table - anything laid out within a 3x3 area that no Extreme
     * recipe claims is crafted as at a normal table.
     */
    private CraftingInput smallInput() {
        int l = 9, t = 9, r = -1, b = -1;
        for (int i = 0; i < 81; i++)
            if (!be.grid.getStackInSlot(i).isEmpty()) {
                l = Math.min(l, i % 9);
                r = Math.max(r, i % 9);
                t = Math.min(t, i / 9);
                b = Math.max(b, i / 9);
            }
        if (r < 0 || r - l > 2 || b - t > 2) return null;
        List<ItemStack> items = new ArrayList<>();
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) items.add(be.grid.getStackInSlot(y * 9 + x));
        return CraftingInput.of(r - l + 1, b - t + 1, items);
    }

    Optional<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> vanilla() {
        if (be.getLevel() == null) return Optional.empty();
        CraftingInput in = smallInput();
        if (in == null) return Optional.empty();
        return be.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, in, be.getLevel());
    }

    void gridChanged() {
        if (be.getLevel() == null || be.getLevel().isClientSide) return;
        ItemStack out = current().map(h -> h.value().assemble(input(), be.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        if (out.isEmpty()) {
            CraftingInput small = smallInput();
            if (small != null) out = vanilla().map(h -> h.value().assemble(small, be.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        }
        result.setItem(0, out);         // the server's per-tick menu sync sends it to the client
    }

    private static boolean fits(Player p, ItemStack s) {
        int room = 0;
        for (ItemStack i : p.getInventory().items)
            if (i.isEmpty()) room += s.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(i, s)) room += i.getMaxStackSize() - i.getCount();
        return room >= s.getCount();
    }

    private void consumeGrid() {
        if (be.getLevel() == null || be.getLevel().isClientSide) return;
        CraftingInput in = input();
        Optional<RecipeHolder<ExtremeRecipe>> r = current();
        NonNullList<ItemStack> remaining;
        if (r.isPresent()) remaining = r.get().value().getRemainingItems(in);
        else {
            // an ordinary crafting recipe: its own leftovers (buckets...), laid out over the same area
            CraftingInput small = smallInput();
            var v = small == null ? Optional.<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>>empty() : vanilla();
            if (v.isPresent()) {
                in = small;
                remaining = v.get().value().getRemainingItems(small);
            } else remaining = NonNullList.withSize(in.size(), ItemStack.EMPTY);
        }
        // CraftingInput may be trimmed: map its cells back onto the 9x9 grid
        int left = 9, top = 9;
        for (int i = 0; i < 81; i++)
            if (!be.grid.getStackInSlot(i).isEmpty()) {
                left = Math.min(left, i % 9);
                top = Math.min(top, i / 9);
            }
        be.quiet = true;
        try {
            for (int i = 0; i < 81; i++) be.grid.extractItem(i, 1, false);
        } finally {
            be.quiet = false;
        }
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack rem = remaining.get(i);
            if (rem.isEmpty()) continue;
            int slot = (top + i / in.width()) * 9 + left + i % in.width();
            if (slot < 81 && be.grid.getStackInSlot(slot).isEmpty()) be.grid.setStackInSlot(slot, rem);
            else if (!player.getInventory().add(rem)) player.drop(rem, false);
        }
        gridChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (index == 81) {                      // shift-click the result: craft repeatedly while it fits
            if (p.level().isClientSide) return ItemStack.EMPTY;   // the server does it and syncs back
            for (int n = 0; n < 64 && slot.hasItem(); n++) {
                ItemStack made = slot.getItem().copy();
                if (!fits(p, made)) break;
                slot.onTake(p, made);
                p.getInventory().add(made);
            }
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 81) {
            if (!moveItemStackTo(stack, 82, 118, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 81, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        // topping up a grid stack changes it in place without the table noticing: save it and re-check the result
        be.setChanged();
        for (ExtremeTableMenu m : List.copyOf(be.viewers)) m.gridChanged();
        return copy;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        be.viewers.remove(this);
    }

    @Override
    public boolean stillValid(Player p) {
        return !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
