package com.theoreticalsciences.converge;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A Resonance Pedestal: holds exactly one item for a Convergence Altar nearby. Pipes may fill and empty it. */
public class PedestalBlockEntity extends BlockEntity {
    public final ItemStackHandler item = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };
    /** Set by an altar while it draws on this pedestal: nothing may be taken out. */
    long lockedUntil;
    /** Client only: how much of the item is still there while an altar draws it in (1 = all). */
    public float fade = 1F;
    public long fadeUntil;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.CONV_PEDESTAL.get(), pos, state);
    }

    /** 1 = Resonance, 2 = Harmonic, 3 = Absolute. */
    public int tier() {
        String p = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
        return p.startsWith("absolute") ? 3 : p.startsWith("harmonic") ? 2 : 1;
    }

    public ItemStack stack() {
        return item.getStackInSlot(0);
    }

    public boolean locked() {
        return level != null && level.getGameTime() < lockedUntil;
    }

    void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** What pipes see: no taking out while an altar is using it. */
    public final net.neoforged.neoforge.items.IItemHandler automation = new net.neoforged.neoforge.items.IItemHandler() {
        public int getSlots() {
            return 1;
        }

        public ItemStack getStackInSlot(int s) {
            return item.getStackInSlot(s);
        }

        public ItemStack insertItem(int s, ItemStack st, boolean sim) {
            return locked() ? st : item.insertItem(s, st, sim);
        }

        public ItemStack extractItem(int s, int n, boolean sim) {
            return locked() ? ItemStack.EMPTY : item.extractItem(s, n, sim);
        }

        public int getSlotLimit(int s) {
            return 1;
        }

        public boolean isItemValid(int s, ItemStack st) {
            return true;
        }
    };

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Item", item.serializeNBT(regs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        item.deserializeNBT(regs, tag.getCompound("Item"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        return saveWithoutMetadata(regs);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
