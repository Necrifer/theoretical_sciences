package com.theoreticalsciences.machine;

import com.theoreticalsciences.registry.QHMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MachineMenu extends AbstractContainerMenu {
    public final MachineBlockEntity be;
    public final ContainerData data;
    public final MachineLayout layout;
    private final int machineSlots;

    public MachineMenu(int id, Inventory inv, MachineBlockEntity be, ContainerData data) {
        super(QHMenus.MACHINE.get(), id);
        this.be = be;
        this.data = data;
        this.layout = new MachineLayout(be.type);
        MachineType t = be.type;
        for (int i = 0; i < t.itemInputs; i++)
            addSlot(new SlotItemHandler(be.items, i, layout.itemInX[i], layout.itemInY[i]) {
                @Override
                public ItemStack remove(int amount) {      // big input slots: never hand out more than one normal stack
                    return super.remove(Math.min(amount, getItem().getMaxStackSize()));
                }

                // The storage holds up to 1024 of an item per input slot, but the stock slot also caps what a player can
                // put in by hand at one normal stack (64). Let clicks and shift-clicks keep topping it up to 1024.
                @Override
                public int getMaxStackSize() {
                    return BigItemStackHandler.BIG_LIMIT;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    // 0 for anything that cannot join this slot: no swapping a different item for a 1024 stack
                    if (!mayPlace(stack)) return 0;
                    ItemStack cur = getItem();
                    if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, stack)) return 0;
                    return BigItemStackHandler.BIG_LIMIT;
                }
            });
        for (int i = 0; i < t.itemOutputs; i++)
            addSlot(new SlotItemHandler(be.items, t.itemInputs + i, layout.itemOutX[i], layout.itemOutY[i]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        machineSlots = t.itemSlots();
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, layout.invX + c * 18, layout.invY + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, layout.invX + c * 18, layout.invY + 58));
        addDataSlots(data);
    }

    public static MachineMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        if (inv.player.level().getBlockEntity(pos) instanceof MachineBlockEntity be)
            return new MachineMenu(id, inv, be, new SimpleContainerData(13));
        throw new IllegalStateException("No Theoretical Sciences machine at " + pos);
    }

    public long energy() {
        return (data.get(0) & 0xFFFFL) | (data.get(1) & 0xFFFFL) << 16 | (data.get(9) & 0xFFFFL) << 32 | (data.get(10) & 0xFFFFL) << 48;
    }

    public long capacity() {
        return (data.get(2) & 0xFFFFL) | (data.get(3) & 0xFFFFL) << 16 | (data.get(11) & 0xFFFFL) << 32 | (data.get(12) & 0xFFFFL) << 48;
    }

    public int progress() {
        return data.get(4);
    }

    public int maxProgress() {
        return data.get(5);
    }

    public MachineStatus status() {
        int s = data.get(6);
        return s >= 0 && s < MachineStatus.values().length ? MachineStatus.values()[s] : MachineStatus.IDLE;
    }

    public int burnRate() {
        return (data.get(7) & 0xFFFF) | (data.get(8) & 0xFFFF) << 16;
    }

    /**
     * A stack bigger than a normal one (up to 1024) must never land whole in a player's inventory or on the cursor - it
     * could not be saved. Number-key / offhand swaps and cursor swaps on such a slot are handled here: only one normal
     * stack moves.
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId >= 0 && slotId < be.type.itemInputs) {
            Slot s = slots.get(slotId);
            ItemStack in = s.getItem();
            if (!in.isEmpty() && in.getCount() > in.getMaxStackSize()) {
                if (type == net.minecraft.world.inventory.ClickType.SWAP) {
                    ItemStack target = button == 40 ? player.getInventory().offhand.get(0) : button >= 0 && button < 9 ? player.getInventory().getItem(button) : null;
                    if (target != null && target.isEmpty()) {
                        ItemStack part = s.remove(in.getMaxStackSize());
                        if (button == 40) player.getInventory().offhand.set(0, part);
                        else player.getInventory().setItem(button, part);
                        s.setChanged();
                        broadcastChanges();
                    }
                    return;                      // swapping a different item in is refused outright
                }
                if (type == net.minecraft.world.inventory.ClickType.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameComponents(getCarried(), in)) return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int total = slots.size();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, total, true)) return ItemStack.EMPTY;
        } else {
            // into the input slots through the storage itself (its limit arithmetic is safe near 2^31; vanilla's merge is not)
            ItemStack rest = stack.copy();
            for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++)
                for (int i = 0; i < be.type.itemInputs && !rest.isEmpty(); i++) {
                    ItemStack cur = be.items.getStackInSlot(i);
                    if (pass == 0 ? cur.isEmpty() || !ItemStack.isSameItemSameComponents(cur, rest) : !cur.isEmpty()) continue;
                    rest = be.items.insertItem(i, rest, false);
                }
            if (rest.getCount() == stack.getCount()) return ItemStack.EMPTY;
            stack.setCount(rest.getCount());
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        // a machine slot's own "changed" call does nothing, so when only part of a stack moved the machine would
        // not notice (and might not save it). Tell the machine directly.
        if (!player.level().isClientSide) be.onInventoryChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return !be.isRemoved() && inReach(player);
    }

    /**
     * Close enough to use this screen: near the controller - or (round 29), for a multiblock, within 8 blocks of any of
     * its Interface Ports, so a screen opened at a far port of a big structure stays open and its settings still apply.
     */
    public boolean inReach(Player player) {
        if (player.distanceToSqr(be.getBlockPos().getCenter()) < 64.0 + (be.type.isMultiblock() ? 200 : 0)) return true;
        if (be instanceof ControllerBlockEntity c)
            for (net.minecraft.core.BlockPos port : c.ports())
                if (player.distanceToSqr(port.getCenter()) < 64.0) return true;
        return false;
    }
}
