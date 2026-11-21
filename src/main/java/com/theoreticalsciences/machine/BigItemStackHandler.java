package com.theoreticalsciences.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Item storage whose first {@code bigSlots} slots hold far more than a normal stack (like a GregTech input bus), so
 * recipes can ask for hundreds of an item. Vanilla can't save a stack above 99, so big stacks are saved as
 * "one of the item + a separate count" and read back the same way (old saves still load).
 */
public class BigItemStackHandler extends ItemStackHandler {
    public static final int BIG_LIMIT = Integer.MAX_VALUE;     // round 20: as far as an item count can go
    private final int bigSlots;

    public BigItemStackHandler(int size, int bigSlots) {
        super(size);
        this.bigSlots = bigSlots;
    }

    public boolean isBig(int slot) {
        return slot < bigSlots;
    }

    @Override
    public int getSlotLimit(int slot) {
        return isBig(slot) ? BIG_LIMIT : 64;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return isBig(slot) ? BIG_LIMIT : Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider regs) {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s.isEmpty()) continue;
            CompoundTag t = new CompoundTag();
            t.putInt("Slot", i);
            t.put("Item", s.copyWithCount(1).save(regs));
            t.putInt("BigCount", s.getCount());
            list.add(t);
        }
        CompoundTag out = new CompoundTag();
        out.put("Items", list);
        out.putInt("Size", stacks.size());
        return out;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider regs, CompoundTag nbt) {
        setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : stacks.size());
        ListTag list = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            int slot = t.getInt("Slot");
            if (slot < 0 || slot >= stacks.size()) continue;
            ItemStack s;
            if (t.contains("Item")) {
                s = ItemStack.parseOptional(regs, t.getCompound("Item"));
                if (!s.isEmpty()) s.setCount(Math.max(1, t.getInt("BigCount")));
            } else s = ItemStack.parseOptional(regs, t);          // saved before big slots existed
            stacks.set(slot, s);
        }
        onLoad();
    }
}
