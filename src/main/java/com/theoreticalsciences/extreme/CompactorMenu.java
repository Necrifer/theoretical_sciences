package com.theoreticalsciences.extreme;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CompactorMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176, HEIGHT = 186, IN_X = 26, IN_Y = 46, OUT_X = 134, OUT_Y = 46, INV_X = 8, INV_Y = 104;
    public final CompactorBlockEntity be;
    private final ContainerData data;

    public CompactorMenu(int id, Inventory inv, CompactorBlockEntity be) {
        this(id, inv, be, be.data);
    }

    private CompactorMenu(int id, Inventory inv, CompactorBlockEntity be, ContainerData data) {
        super(ExtremeRegistry.COMPACTOR_MENU.get(), id);
        this.be = be;
        this.data = data;
        addSlot(new SlotItemHandler(be.slots, 0, IN_X, IN_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return be.accepts(s);
            }
        });
        addSlot(new SlotItemHandler(be.slots, 1, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    public static CompactorMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        if (inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof CompactorBlockEntity be)
            return new CompactorMenu(id, inv, be, new SimpleContainerData(6));
        throw new IllegalStateException("No Singularity Compactor there");
    }

    public int absorbed() {
        return (data.get(0) & 0xFFFF) | (data.get(1) & 0xFFFF) << 16;
    }

    public int required() {
        return (data.get(2) & 0xFFFF) | (data.get(3) & 0xFFFF) << 16;
    }

    public ItemStack target() {
        int id = ((data.get(4) & 0xFFFF) | (data.get(5) & 0xFFFF) << 16) - 1;
        return id < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(id));
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (!be.accepts(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        be.setChanged();          // partial moves change the stack in place: make sure the compactor gets saved
        return copy;
    }

    @Override
    public boolean stillValid(Player p) {
        return !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
