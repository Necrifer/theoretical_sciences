package com.theoreticalsciences.compat;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * A storage network an Interface Port is connected to (AE2 today). No AE2 types here, so the mod loads without AE2.
 * Stacks returned are copies, their counts capped at Integer.MAX_VALUE.
 */
public interface MEAccess {
    boolean online();

    /** How much the network holds of each of these (only those it has), looked up directly - not a scan. */
    List<ItemStack> items(List<ItemStack> protos);

    List<FluidStack> fluids(List<FluidStack> protos);

    /** Takes all of these, or (if anything is short) nothing. */
    boolean extractAll(List<ItemStack> items, List<FluidStack> fluids);

    /** Stores what it can; returns how many went in. */
    long insert(ItemStack s);

    long insert(FluidStack s);
}
