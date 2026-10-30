package com.theoreticalsciences.machine;

import com.theoreticalsciences.registry.QHItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Dyson Array: a star (stellar core, burned by "theoreticalsciences:dyson" recipes) surrounded by a swarm.
 * Slot 0: star fuel. Slot 1: Dyson swarm satellites (absorbed, up to {@link #MAX_SATELLITES}).
 * Slot 2: Dyson shell segments (absorbed, up to {@link #SHELL_SEGMENTS}); a complete shell turns the swarm into a Dyson Sphere.
 * Output = recipe FE/t x (satellites / 50) x (1 + shells / 16), doubled again when the sphere is complete.
 */
public class DysonControllerBlockEntity extends ControllerBlockEntity {
    public static final int MAX_SATELLITES = 500;
    public static final int SHELL_SEGMENTS = 64;

    private int satellites;
    private int shells;

    public DysonControllerBlockEntity(MachineType type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int satellites() {
        return satellites;
    }

    public int shells() {
        return shells;
    }

    public boolean isSphere() {
        return shells >= SHELL_SEGMENTS;
    }

    @Override
    public List<ItemStack> inputStacks() {
        return List.of(items.getStackInSlot(0));
    }

    @Override
    protected void preTick(ServerLevel level) {
        super.preTick(level);
        // Von Neumann probes in the satellite slot self-replicate the swarm from asteroid matter.
        ItemStack probe = items.getStackInSlot(1);
        if (!probe.isEmpty() && probe.is(QHItems.get("von_neumann_probe")) && isFormed() && satellites < MAX_SATELLITES
                && level.getGameTime() % Math.max(20, 400 / probe.getCount()) == 0) {
            satellites++;
            onInventoryChanged();
        }
        ItemStack sat = items.getStackInSlot(1);
        if (!sat.isEmpty() && sat.is(QHItems.get("dyson_swarm_satellite")) && satellites < MAX_SATELLITES) {
            int n = Math.min(sat.getCount(), MAX_SATELLITES - satellites);
            satellites += n;
            sat.shrink(n);
            onInventoryChanged();
        }
        ItemStack sh = items.getStackInSlot(2);
        if (!sh.isEmpty() && sh.is(QHItems.get("dyson_shell_segment")) && shells < SHELL_SEGMENTS && satellites >= MAX_SATELLITES / 5) {
            int n = Math.min(sh.getCount(), SHELL_SEGMENTS - shells);
            shells += n;
            sh.shrink(n);
            onInventoryChanged();
        }
    }

    @Override
    public int effectiveBurnRate() {
        double mult = (satellites / 50.0) * (1.0 + shells / 16.0) * (isSphere() ? 2.0 : 1.0);
        return (int) Math.min(Integer.MAX_VALUE, burnRate * mult);
    }

    /** The absorbed satellites and shell segments cannot be spilled as items, so the controller's item keeps them. */
    @Override
    public boolean keepsContentsInItem() {
        return super.keepsContentsInItem() || satellites > 0 || shells > 0;
    }

    @Override
    protected void writeShared(CompoundTag tag, HolderLookup.Provider regs) {
        super.writeShared(tag, regs);
        tag.putInt("Satellites", satellites);
        tag.putInt("Shells", shells);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        satellites = tag.getInt("Satellites");
        shells = tag.getInt("Shells");
    }
}
