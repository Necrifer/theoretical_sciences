package com.theoreticalsciences.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Automation view: insert into input slots, extract from output slots only. */
public class MachineItemHandler implements IItemHandler {
    private final MachineBlockEntity be;

    public MachineItemHandler(MachineBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getSlots() {
        return be.items.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return be.items.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot >= be.type.itemInputs || be.isRemoved()) return stack;     // (a pipe still holding a removed machine's handler)
        return be.items.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < be.type.itemInputs || be.isRemoved()) return ItemStack.EMPTY;
        return be.items.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return be.items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot < be.type.itemInputs;
    }
}
