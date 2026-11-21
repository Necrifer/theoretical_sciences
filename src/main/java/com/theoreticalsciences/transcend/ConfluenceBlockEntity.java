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
 * 1.2.0: Pleroma is distilled from five fluids, one for each branch of knowledge and one for the world itself:
 * Quintessence (Physics), Alkahest (Chemistry), Mathesis (Mathematics), Panacea (Biology) and Anima Mundi (water,
 * lava and every other mod's Essence). Each holds up to {@link #BUFFER} mB; whenever all five have {@link #TARGET} mB,
 * that much of each is used up and {@link #BATCH} mB of Pleroma appears in the output tank. Accepts fluids from pipes
 * and AE2 on any side; Pleroma can be drained from any side.
 * <p>
 * Until 1.2 it asked for every fluid of the mod (and every mod's Essence) at once, a list so long that its screen and
 * JEI could hardly show it. A confluence part filled under the old list is credited on load (see {@link #legacyCredit}).
 */
public class ConfluenceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TARGET = 1_000, BUFFER = 16_000, BATCH = 1_000, OUT_CAP = 64_000;
    /** The five fluids, in the order they are shown. */
    public static final String[] FIVE = {"quintessence", "alkahest", "mathesis", "panacea", "anima_mundi"};
    /** Before 1.2: how much of each fluid (and of each mod's Essence) one batch took, and about how many fluids there were. */
    static final long OLD_TARGET = 1_000_000L, OLD_ESSENCE_TARGET = 64_000L;
    static final int OLD_COUNT = 69;
    public final List<FluidStack> required = new ArrayList<>();
    public final long[] stored;
    public int pleroma;
    private boolean dirty;

    public ConfluenceBlockEntity(BlockPos pos, BlockState state) {
        super(TranscendRegistry.CONFLUENCE_BE.get(), pos, state);
        required.addAll(wanted());
        stored = new long[required.size()];
    }

    /** Everything Pleroma is made of (also for JEI and EMI). */
    public static List<FluidStack> wanted() {
        List<FluidStack> l = new ArrayList<>();
        for (String id : FIVE) {
            var e = QHFluids.ALL.get(id);
            if (e != null) l.add(new FluidStack(e.source.get(), 1));
        }
        return l;
    }

    public static int target(FluidStack template) {
        return TARGET;
    }

    public int target(int i) {
        return TARGET;
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
            return tank == required.size() ? OUT_CAP : BUFFER;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank < required.size() && FluidStack.isSameFluidSameComponents(required.get(tank), stack);
        }

        @Override
        public int fill(FluidStack res, FluidAction action) {
            int i = index(res);
            if (i < 0 || res.isEmpty()) return 0;
            int n = (int) Math.min(res.getAmount(), BUFFER - stored[i]);
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
        boolean full = stored.length > 0;
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
        java.util.Set<String> now = new java.util.HashSet<>();
        for (int i = 0; i < required.size(); i++) {
            String k = key(required.get(i));
            now.add(k);
            stored[i] = Math.min(BUFFER, f.getLong(k));
        }
        pleroma = tag.getInt("Pleroma");
        int credit = legacyCredit(f, now);
        if (credit > 0) {
            pleroma = Math.min(OUT_CAP, pleroma + credit);
            dirty = true;
        }
    }

    /**
     * 1.2.0: what a confluence filled under the old list (every fluid of the mod and every mod's Essence) had put by, as
     * Pleroma: the share of one batch it had gathered, averaged over the old list. The old totals are not saved again,
     * so this is paid once. (A full batch was always turned into Pleroma at once, so this is less than one batch.)
     */
    static int legacyCredit(CompoundTag f, java.util.Set<String> now) {
        double share = 0;
        for (String k : f.getAllKeys()) {
            if (now.contains(k)) continue;
            long n = f.getLong(k);
            if (n <= 0) continue;
            share += Math.min(1.0, n / (double) (k.contains("#") ? OLD_ESSENCE_TARGET : OLD_TARGET));
        }
        return (int) Math.min(BATCH, Math.floor(BATCH * share / OLD_COUNT));
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
