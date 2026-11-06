package com.theoreticalsciences.science;

import com.theoreticalsciences.dungeon.CacheBlockEntity;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * 1.2.0: a Containment Cache whose field also holds time still. A radioactive source put in it stops decaying (its clock
 * is held at the age it had), and starts again where it left off when taken out - by hand, a hopper, a pipe or an ME
 * storage bus. Everything else is kept as in any cache.
 */
public class ChronostaticVaultBlockEntity extends CacheBlockEntity {
    public ChronostaticVaultBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.CHRONOSTATIC_VAULT.get(), pos, state);
    }

    private long now() {
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, Wear.hold(stack, now()));
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return Wear.release(super.removeItem(slot, count), now());
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return Wear.release(super.removeItemNoUpdate(slot), now());
    }

    /** Pipes and hoppers: what goes in is held first (so it stacks with what is there), what comes out runs again. */
    public final IItemHandler automation = new InvWrapper(this) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack rest = super.insertItem(slot, Wear.hold(stack, now()), simulate);
            return rest.isEmpty() ? rest : Wear.release(rest, now());
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return Wear.release(super.extractItem(slot, amount, simulate), now());
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return super.getStackInSlot(slot);
        }
    };
}
