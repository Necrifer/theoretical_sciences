package com.theoreticalsciences.machine;

import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Mana Port's store. It names nothing of Botania: compat.magic.BotaniaMana wraps it as Botania's mana receiver. */
public class ManaPortBlockEntity extends BlockEntity {
    /** As much as a Mana Pool holds. */
    public static final int CAPACITY = 1_000_000;
    private int mana;

    public ManaPortBlockEntity(BlockPos pos, BlockState state) {
        super(QHBlockEntities.MANA_PORT.get(), pos, state);
    }

    public int mana() {
        return mana;
    }

    /** Adds (or, negative, takes) mana, within 0 and the capacity. */
    public void add(int amount) {
        int next = (int) Math.max(0L, Math.min((long) CAPACITY, (long) mana + amount));
        if (next != mana) {
            mana = next;
            setChanged();
            if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putInt("Mana", mana);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        mana = Math.max(0, Math.min(CAPACITY, tag.getInt("Mana")));
    }
}
