package com.theoreticalsciences.dungeon;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

/** 27 slots, a loot table until first opened, and an open/closed look while someone is inside. */
public class CacheBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 0.4F, 1.8F);
            level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.HATCH_OPEN.get(), SoundSource.BLOCKS, 0.6F, 0.7F);
            set(level, pos, state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 0.4F, 1.8F);
            level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.HATCH_CLOSE.get(), SoundSource.BLOCKS, 0.6F, 0.7F);
            set(level, pos, state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {}

        @Override
        protected boolean isOwnContainer(Player p) {
            return p.containerMenu instanceof ChestMenu m && m.getContainer() == CacheBlockEntity.this;
        }
    };

    private static void set(Level level, BlockPos pos, BlockState state, boolean open) {
        if (state.hasProperty(CacheBlock.OPEN) && state.getValue(CacheBlock.OPEN) != open) level.setBlock(pos, state.setValue(CacheBlock.OPEN, open), 3);
    }

    public CacheBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.CACHE.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        items = list;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    public void startOpen(Player p) {
        if (!remove && !p.isSpectator() && level != null) openers.incrementOpeners(p, level, worldPosition, getBlockState());
    }

    @Override
    public void stopOpen(Player p) {
        if (!remove && !p.isSpectator() && level != null) openers.decrementOpeners(p, level, worldPosition, getBlockState());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, regs);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, regs);
    }

    /** Scheduled by the openers counter: re-counts who has it open, so a missed close cannot leave it stuck open. */
    public void recheckOpen() {
        if (!remove && level != null) openers.recheckOpeners(level, worldPosition, getBlockState());
    }
}
