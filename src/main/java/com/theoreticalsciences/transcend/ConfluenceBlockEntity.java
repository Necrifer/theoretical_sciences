package com.theoreticalsciences.transcend;

import com.theoreticalsciences.registry.QHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps a running total of every fluid poured in. When each one has reached {@link #TARGET} mB, that much of each is
 * used up and {@link #BATCH} mB of Pleroma appears in the output tank. Accepts fluids from pipes and AE2 on any side;
 * Pleroma can be drained from any side.
 */
public class ConfluenceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TARGET = 1_000_000, BATCH = 1_000, OUT_CAP = 64_000;
    /** Round 26: how much of each mod's Essence (one Essence already holds a million mB of that mod's fluids). */
    public static final int ESSENCE_TARGET = 64_000;
    /** What it wants, as templates (fluid and data): water, lava, every fluid of this mod, and every other mod's Essence. */
    public final List<FluidStack> required = new ArrayList<>();
    public final long[] stored;
    public int pleroma;
    private boolean dirty;

    public ConfluenceBlockEntity(BlockPos pos, BlockState state) {
        super(TranscendRegistry.CONFLUENCE_BE.get(), pos, state);
        required.addAll(wanted());
        stored = new long[required.size()];
    }

    /** Everything Pleroma is made of (also for JEI). */
    public static List<FluidStack> wanted() {
        List<FluidStack> l = new ArrayList<>();
        l.add(new FluidStack(Fluids.WATER, 1));
        l.add(new FluidStack(Fluids.LAVA, 1));
        QHFluids.ALL.forEach((id, e) -> {
            if (!id.equals("pleroma") && !id.equals("mod_essence")) l.add(new FluidStack(e.source.get(), 1));
        });
        l.addAll(Essences.finals());
        return l;
    }

    public static int target(FluidStack template) {
        return Essences.isEssence(template) ? ESSENCE_TARGET : TARGET;
    }

    public int target(int i) {
        return target(required.get(i));
    }

    private int index(FluidStack f) {
        for (int i = 0; i < required.size(); i++) if (FluidStack.isSameFluidSameComponents(required.get(i), f)) return i;
        return -1;
    }

    private static String key(FluidStack f) {
        String m = Essences.mod(f);
        return BuiltInRegistries.FLUID.getKey(f.getFluid()) + (m == null ? "" : "#" + m);
    }

    private Fluid pleromaFluid() {
        return QHFluids.ALL.get("pleroma").source.get();
    }

    public final IFluidHandler fluids = new IFluidHandler() {
        @Override
        public int getTanks() {
            return required.size() + 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank == required.size()) return pleroma > 0 ? new FluidStack(pleromaFluid(), pleroma) : FluidStack.EMPTY;
            return stored[tank] > 0 ? required.get(tank).copyWithAmount((int) Math.min(Integer.MAX_VALUE, stored[tank])) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == required.size() ? OUT_CAP : target(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank < required.size() && FluidStack.isSameFluidSameComponents(required.get(tank), stack);
        }

        @Override
        public int fill(FluidStack res, FluidAction action) {
            int i = index(res);
            if (i < 0 || res.isEmpty()) return 0;
            int n = (int) Math.min(res.getAmount(), target(i) - stored[i]);
            if (n <= 0) return 0;
            if (action.execute()) {
                stored[i] += n;
                changed();
            }
            return n;
        }

        @Override
        public FluidStack drain(FluidStack res, FluidAction action) {
            if (res.isEmpty() || !res.getFluid().isSame(pleromaFluid())) return FluidStack.EMPTY;
            return drain(res.getAmount(), action);
        }

        @Override
        public FluidStack drain(int max, FluidAction action) {
            int n = Math.min(max, pleroma);
            if (n <= 0) return FluidStack.EMPTY;
            if (action.execute()) {
                pleroma -= n;
                changed();
            }
            return new FluidStack(pleromaFluid(), n);
        }
    };

    private void changed() {
        dirty = true;
        setChanged();
    }

    void serverTick() {
        if (level == null) return;
        boolean full = true;
        for (int i = 0; i < stored.length; i++) if (stored[i] < target(i)) { full = false; break; }
        if (full && pleroma + BATCH <= OUT_CAP) {
            for (int i = 0; i < stored.length; i++) stored[i] -= target(i);
            pleroma += BATCH;
            changed();
        }
        if (dirty && level.getGameTime() % 10 == 0) {
            dirty = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public int filledCount() {
        int n = 0;
        for (int i = 0; i < stored.length; i++) if (stored[i] >= target(i)) n++;
        return n;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.theoreticalsciences.pleroma_confluence");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ConfluenceMenu(id, inv, this);
    }

    /** What its dropped item keeps (stored fluids and Pleroma), or null when it is empty. */
    public CompoundTag keptData() {
        boolean any = pleroma > 0;
        for (long l : stored) any |= l > 0;
        return any && level != null ? saveCustomOnly(level.registryAccess()) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        CompoundTag f = new CompoundTag();
        for (int i = 0; i < required.size(); i++)
            if (stored[i] > 0) f.putLong(key(required.get(i)), stored[i]);
        tag.put("Fluids", f);
        tag.putInt("Pleroma", pleroma);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        CompoundTag f = tag.getCompound("Fluids");
        for (int i = 0; i < required.size(); i++) stored[i] = f.getLong(key(required.get(i)));
        pleroma = tag.getInt("Pleroma");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, regs);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
