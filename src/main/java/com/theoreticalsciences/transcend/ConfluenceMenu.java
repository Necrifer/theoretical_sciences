package com.theoreticalsciences.transcend;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Read-only view of the confluence's totals (the numbers come from the block entity, kept in sync by block updates). */
public class ConfluenceMenu extends AbstractContainerMenu {
    public static final int WIDTH = 256, HEIGHT = 230, INV_X = 48, INV_Y = 148;
    public final ConfluenceBlockEntity be;

    public ConfluenceMenu(int id, Inventory inv, ConfluenceBlockEntity be) {
        super(TranscendRegistry.CONFLUENCE_MENU.get(), id);
        this.be = be;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
    }

    public static ConfluenceMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        if (inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof ConfluenceBlockEntity be) return new ConfluenceMenu(id, inv, be);
        throw new IllegalStateException("No Pleroma Confluence there");
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }
}
