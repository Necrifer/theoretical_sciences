package com.theoreticalsciences.storage;

import com.theoreticalsciences.registry.QHMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/**
 * Six rows of the cache's 27 at a time (scroll to move the window). Slots hold up to 2^31-1 of an item; a click or
 * shift-click takes at most one normal stack, so nothing oversized ever reaches a player's inventory.
 */
public class InfinityCacheMenu extends AbstractContainerMenu {
    public static final int VIEW_ROWS = 6, VIEW = VIEW_ROWS * 9;
    @Nullable public final InfinityCacheBlockEntity be;
    final InfinityCacheBlockEntity.Infinite store;
    final DataSlot offset = DataSlot.standalone();

    public InfinityCacheMenu(int id, Inventory inv, @Nullable InfinityCacheBlockEntity be) {
        super(QHMenus.INFINITY_CACHE.get(), id);
        this.be = be;
        this.store = be != null ? be.items : new InfinityCacheBlockEntity(net.minecraft.core.BlockPos.ZERO,
                com.theoreticalsciences.registry.QHBlocks.get("transcendent_cache").defaultBlockState()).items;
        addDataSlot(offset);
        for (int r = 0; r < VIEW_ROWS; r++)
            for (int c = 0; c < 9; c++) addSlot(new View(store, r * 9 + c, 8 + c * 18, 18 + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 140 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 198));
        if (be != null) be.opened(true);
    }

    public static InfinityCacheMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        return new InfinityCacheMenu(id, inv, inv.player.level().getBlockEntity(pos) instanceof InfinityCacheBlockEntity c ? c : null);
    }

    public int offset() {
        return offset.get();
    }

    /** A window slot: shows store slot (row offset * 9 + its place). */
    class View extends SlotItemHandler {
        final int place;

        View(IItemHandler h, int place, int x, int y) {
            super(h, place, x, y);
            this.place = place;
        }

        int index() {
            return Math.min(InfinityCacheBlockEntity.SLOTS - 1, offset.get() * 9 + place);
        }

        @Override
        public ItemStack getItem() {
            return store.getStackInSlot(index());
        }

        @Override
        public void set(ItemStack s) {
            store.setStackInSlot(index(), s);
            setChanged();
        }

        @Override
        public void initialize(ItemStack s) {
            store.setStackInSlot(index(), s);
        }

        @Override
        public ItemStack remove(int n) {
            ItemStack cur = getItem();
            return store.extractItem(index(), Math.min(n, cur.isEmpty() ? n : cur.getMaxStackSize()), false);
        }

        @Override
        public boolean mayPlace(ItemStack s) {
            return InfinityCacheBlockEntity.allowed(s);
        }

        @Override
        public boolean mayPickup(Player p) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack s) {
            ItemStack cur = getItem();
            return !cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, s) ? 0 : Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        int max = InfinityCacheBlockEntity.ROWS - VIEW_ROWS;
        int o = offset.get();
        if (id == 0) o--;
        else if (id == 1) o++;
        else o = id - 2;
        offset.set(Math.max(0, Math.min(max, o)));
        if (!p.level().isClientSide) broadcastFullState();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < VIEW) {
            Slot s = slots.get(slotId);
            ItemStack in = s.getItem();
            if (!in.isEmpty() && in.getCount() > in.getMaxStackSize()) {
                if (type == ClickType.SWAP) {
                    ItemStack target = button == 40 ? player.getInventory().offhand.get(0) : button >= 0 && button < 9 ? player.getInventory().getItem(button) : null;
                    if (target != null && target.isEmpty()) {
                        ItemStack part = s.remove(in.getMaxStackSize());
                        if (button == 40) player.getInventory().offhand.set(0, part);
                        else player.getInventory().setItem(button, part);
                        broadcastChanges();
                    }
                    return;
                }
                if (type == ClickType.CLONE) return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (index < VIEW) {
            ItemStack cur = slot.getItem();
            int take = Math.min(cur.getCount(), cur.getMaxStackSize());
            ItemStack part = cur.copyWithCount(take);
            moveItemStackTo(part, VIEW, slots.size(), true);
            int moved = take - part.getCount();
            if (moved > 0) slot.remove(moved);
            return ItemStack.EMPTY;
        }
        ItemStack st = slot.getItem();
        ItemStack left = store.add(st, false);
        slot.set(left);
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (be != null && !p.level().isClientSide) be.opened(false);
    }

    @Override
    public boolean stillValid(Player p) {
        return be != null && !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
