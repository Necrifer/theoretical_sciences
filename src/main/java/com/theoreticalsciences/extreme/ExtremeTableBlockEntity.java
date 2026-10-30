package com.theoreticalsciences.extreme;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ExtremeTableBlockEntity extends BlockEntity implements MenuProvider {
    final java.util.List<ExtremeTableMenu> viewers = new java.util.ArrayList<>();
    /** While a craft takes one of everything, hold the 81 "grid changed" checks back and do one at the end. */
    boolean quiet;
    public final ItemStackHandler grid = new ItemStackHandler(81) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (quiet) return;
            for (ExtremeTableMenu m : java.util.List.copyOf(viewers)) m.gridChanged();
        }
    };

    public ExtremeTableBlockEntity(BlockPos pos, BlockState state) {
        super(ExtremeRegistry.EXTREME_TABLE_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.extreme_assembly_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ExtremeTableMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Grid", grid.serializeNBT(regs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        if (tag.contains("Grid")) grid.deserializeNBT(regs, tag.getCompound("Grid"));
    }
}
