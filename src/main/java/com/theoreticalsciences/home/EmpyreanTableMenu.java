package com.theoreticalsciences.home;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Empyrean Assembly Table's menu. Only the player's own inventory is in it as slots: the table's 225 slots are a
 * picture drawn from a few counts ({@link HomePayloads.TableSync}), and every click on it is a request to the server
 * ({@link HomePayloads.TableAction}). That keeps opening it as cheap as opening a chest.
 */
public class EmpyreanTableMenu extends AbstractContainerMenu {
    /** Where the player's inventory sits in the screen (the screen draws its frame from these too). */
    public static final int INV_X = 204, INV_Y = 124;
    public final EmpyreanTableBlockEntity be;
    private final Player player;
    private int sent = -1;

    public EmpyreanTableMenu(int id, Inventory inv, EmpyreanTableBlockEntity be) {
        super(HomeRegistry.TABLE_MENU.get(), id);
        this.be = be;
        this.player = inv.player;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, INV_X + col * 18, INV_Y + 58));
    }

    public static EmpyreanTableMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        if (inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof EmpyreanTableBlockEntity be) return new EmpyreanTableMenu(id, inv, be);
        throw new IllegalStateException("No Empyrean Assembly Table there");
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer sp && sent != be.version()) {
            sent = be.version();
            PacketDistributor.sendToPlayer(sp, new HomePayloads.TableSync(be.getBlockPos(), be.write(new net.minecraft.nbt.CompoundTag()),
                    be.blocked() == null ? "" : be.blocked()));
        }
    }

    /** Shift-click in the inventory: everything of that stack the table will take goes in. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        ItemStack st = slot.getItem();
        if (st.isEmpty() || !st.getComponentsPatch().isEmpty() || p.level().isClientSide) return ItemStack.EMPTY;
        long n = be.put(st.getItem(), st.getCount());
        if (n > 0) {
            st.shrink((int) n);
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p) {
        return !be.isRemoved() && p.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) <= 64;
    }
}
