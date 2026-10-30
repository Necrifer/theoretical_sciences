package com.theoreticalsciences.dungeon;

import com.theoreticalsciences.registry.QHMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/** The built Breached Anomaly Engine: template and serum slots, four tuning controls (menu buttons), status. */
public class AnomalyEngineMenu extends AbstractContainerMenu {
    @Nullable public final AnomalyEngineBlockEntity be;
    public final ContainerData data;
    final ItemStackHandler items;

    public AnomalyEngineMenu(int id, Inventory inv, @Nullable AnomalyEngineBlockEntity be, ContainerData data) {
        super(QHMenus.ANOMALY_ENGINE.get(), id);
        this.be = be;
        this.data = data;
        this.items = be != null ? be.items : new ItemStackHandler(2);
        addSlot(new SlotItemHandler(items, 0, 20, 30));
        addSlot(new SlotItemHandler(items, 1, 20, 62));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 112 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 170));
        addDataSlots(data);
    }

    public static AnomalyEngineMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        return new AnomalyEngineMenu(id, inv, inv.player.level().getBlockEntity(pos) instanceof AnomalyEngineBlockEntity a ? a : null,
                new SimpleContainerData(7));
    }

    public int knob(int i) {
        return data.get(i) - 1000;
    }

    public int status() {
        return data.get(4);
    }

    public long energyK() {
        return (data.get(5) & 0xFFFFL) | (data.get(6) & 0xFFFFL) << 16;
    }

    /** Buttons: control i, step k (0: -10, 1: -1, 2: +1, 3: +10) = id 4*i + k. */
    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (be == null) return false;
        int i = id / 4, k = id % 4;
        be.knob(i, k == 0 ? -10 : k == 1 ? -1 : k == 2 ? 1 : 10);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (be != null) be.setChanged();   // partial moves change the stack in place: make sure the engine gets saved
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return be != null && !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
