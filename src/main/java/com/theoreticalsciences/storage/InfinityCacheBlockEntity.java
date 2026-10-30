package com.theoreticalsciences.storage;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Transcendent Cache (round 20): an Avaritia-style infinity chest. 243 slots, each holding up to 2,147,483,647 of
 * one item. Anything put in joins the slot that already holds that item. It keeps its contents when broken.
 */
public class InfinityCacheBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOTS = 243, ROWS = SLOTS / 9;
    public final Infinite items = new Infinite();

    /** No cache inside a cache (a filled one could grow past what a save or a packet can hold). */
    public static boolean allowed(ItemStack s) {
        return !(s.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof InfinityCacheBlock
                && s.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA));
    }
    int openers;

    public InfinityCacheBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.INFINITY_CACHE.get(), pos, state);
    }

    public class Infinite extends ItemStackHandler {
        Infinite() {
            super(SLOTS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return Integer.MAX_VALUE;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        /** Puts a stack where that item already is, else in the first empty slot; returns what did not fit. */
        public ItemStack add(ItemStack s, boolean simulate) {
            if (s.isEmpty() || !allowed(s)) return s;
            int left = s.getCount();
            int empty = -1;
            for (int i = 0; i < getSlots() && left > 0; i++) {
                ItemStack cur = getStackInSlot(i);
                if (cur.isEmpty()) {
                    if (empty < 0) empty = i;
                } else if (ItemStack.isSameItemSameComponents(cur, s)) {
                    int put = (int) Math.min(Integer.MAX_VALUE - (long) cur.getCount(), left);
                    if (!simulate && put > 0) {
                        cur.grow(put);
                        onContentsChanged(i);
                    }
                    left -= put;                    // a full pile: keep looking (another pile, or an empty slot)
                }
            }
            if (left > 0 && empty >= 0) {
                if (!simulate) setStackInSlot(empty, s.copyWithCount(left));
                left = 0;
            }
            return left == 0 ? ItemStack.EMPTY : s.copyWithCount(left);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowed(stack);
        }

        public int used() {
            int n = 0;
            for (int i = 0; i < getSlots(); i++) if (!getStackInSlot(i).isEmpty()) n++;
            return n;
        }

        public long total() {
            long n = 0;
            for (int i = 0; i < getSlots(); i++) n += getStackInSlot(i).getCount();
            return n;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider regs) {
            ListTag list = new ListTag();
            for (int i = 0; i < getSlots(); i++) {
                ItemStack s = getStackInSlot(i);
                if (s.isEmpty()) continue;
                CompoundTag t = new CompoundTag();
                t.putInt("Slot", i);
                t.put("Item", s.copyWithCount(1).save(regs));
                t.putInt("BigCount", s.getCount());
                list.add(t);
            }
            CompoundTag out = new CompoundTag();
            out.put("Items", list);
            return out;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider regs, CompoundTag nbt) {
            for (int i = 0; i < getSlots(); i++) stacks.set(i, ItemStack.EMPTY);
            ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                int slot = t.getInt("Slot");
                if (slot < 0 || slot >= getSlots()) continue;
                ItemStack s = ItemStack.parseOptional(regs, t.getCompound("Item"));
                if (!s.isEmpty()) s.setCount(Math.max(1, t.getInt("BigCount")));
                stacks.set(slot, s);
            }
            onLoad();
        }
    }

    /** What pipes see: insertion always merges; extraction gives at most one normal stack at a time. */
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() {
            return SLOTS;
        }

        public ItemStack getStackInSlot(int s) {
            return items.getStackInSlot(s);
        }

        public ItemStack insertItem(int s, ItemStack st, boolean sim) {
            return items.add(st, sim);
        }

        public ItemStack extractItem(int s, int n, boolean sim) {
            ItemStack cur = items.getStackInSlot(s);
            if (cur.isEmpty()) return ItemStack.EMPTY;
            return items.extractItem(s, Math.min(n, cur.getMaxStackSize()), sim);
        }

        public int getSlotLimit(int s) {
            return Integer.MAX_VALUE;
        }

        public boolean isItemValid(int s, ItemStack st) {
            return true;
        }
    };

    public int comparator() {
        int u = items.used();
        return u == 0 ? 0 : 1 + (int) Math.floor(u * 14.0 / SLOTS);
    }

    void opened(boolean open) {
        openers = Math.max(0, openers + (open ? 1 : -1));
        if (level != null && !level.isClientSide && !isRemoved()) {
            BlockState s = level.getBlockState(worldPosition);
            if (!(s.getBlock() instanceof InfinityCacheBlock)) return;      // broken meanwhile: never put it back
            if (s.hasProperty(InfinityCacheBlock.OPEN) && s.getValue(InfinityCacheBlock.OPEN) != openers > 0)
                level.setBlock(worldPosition, s.setValue(InfinityCacheBlock.OPEN, openers > 0), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.transcendent_cache");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new InfinityCacheMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Store", items.serializeNBT(regs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        items.deserializeNBT(regs, tag.getCompound("Store"));
    }
}
