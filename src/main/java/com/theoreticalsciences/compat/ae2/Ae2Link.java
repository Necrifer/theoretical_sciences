package com.theoreticalsciences.compat.ae2;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.storage.StorageHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import com.theoreticalsciences.compat.MEAccess;
import com.theoreticalsciences.machine.PortBlockEntity;
import com.theoreticalsciences.registry.QHBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An Interface Port's AE2 node (like the ME input / output busses of MMCE's AE2 addon or GregTech's ME stocking bus):
 * connect an ME cable to the port and the multiblock it belongs to draws recipe ingredients straight from the network
 * and puts its products into it. Uses one channel.
 */
public final class Ae2Link implements MEAccess, IInWorldGridNodeHost, IActionHost {
    private static final IGridNodeListener<PortBlockEntity> LISTENER = (port, node) -> port.setChanged();
    final PortBlockEntity port;
    final IManagedGridNode node;
    private final IActionSource source;
    private boolean created;
    @Nullable private final CompoundTag saved;

    private Ae2Link(PortBlockEntity port, @Nullable CompoundTag saved) {
        this.port = port;
        this.node = GridHelper.createManagedNode(port, LISTENER)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setInWorldNode(true)
                .setIdlePowerUsage(2.0)
                .setTagName("ts_interface_port")
                .setVisualRepresentation(port.getBlockState().getBlock().asItem());
        this.saved = saved;
        if (saved != null && !saved.isEmpty()) node.loadFromNBT(saved);
        this.source = IActionSource.ofMachine(this);
        GridHelper.onFirstTick(port, p -> {
            if (!created && p.getLevel() != null && !p.isRemoved()) {
                node.create(p.getLevel(), p.getBlockPos());
                created = true;
            }
        });
    }

    public static Object create(PortBlockEntity port, @Nullable CompoundTag saved) {
        return new Ae2Link(port, saved);
    }

    public static void destroy(Object o) {
        if (o instanceof Ae2Link l) l.node.destroy();
    }

    public static void save(Object o, CompoundTag into) {
        if (o instanceof Ae2Link l) {
            if (!l.created && l.saved != null) {             // not on the grid yet: keep what was loaded
                into.put("ME", l.saved);
                return;
            }
            CompoundTag t = new CompoundTag();
            l.node.saveToNBT(t);
            into.put("ME", t);
        }
    }

    public static void registerCaps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, QHBlockEntities.PORT.get(),
                (be, ctx) -> be.meLink() instanceof Ae2Link l ? l : null);
    }

    // ------------------------------------------------------------------ AE2 side
    @Override
    @Nullable
    public IGridNode getGridNode(Direction dir) {
        return node.getNode();
    }

    @Override
    @Nullable
    public IGridNode getActionableNode() {
        return node.getNode();
    }

    @Nullable
    private IStorageService storage() {
        if (!node.isActive()) return null;
        IGrid g = node.getGrid();
        return g == null ? null : g.getStorageService();
    }

    // ------------------------------------------------------------------ the machine side
    @Override
    public boolean online() {
        return storage() != null;
    }

    @Override
    public List<ItemStack> items(List<ItemStack> protos) {
        List<ItemStack> out = new ArrayList<>();
        IStorageService s = storage();
        if (s == null) return out;
        KeyCounter c = s.getCachedInventory();
        for (ItemStack p : protos) {
            AEItemKey k = AEItemKey.of(p);
            long n = k == null ? 0 : c.get(k);
            if (n > 0) out.add(k.toStack((int) Math.min(Integer.MAX_VALUE, n)));
        }
        return out;
    }

    @Override
    public List<FluidStack> fluids(List<FluidStack> protos) {
        List<FluidStack> out = new ArrayList<>();
        IStorageService s = storage();
        if (s == null) return out;
        KeyCounter c = s.getCachedInventory();
        for (FluidStack p : protos) {
            AEFluidKey k = AEFluidKey.of(p);
            long n = k == null ? 0 : c.get(k);
            if (n > 0) out.add(k.toStack((int) Math.min(Integer.MAX_VALUE, n)));
        }
        return out;
    }

    @Nullable
    private IEnergyService energy() {
        IGrid g = node.getGrid();
        return g == null ? null : g.getEnergyService();
    }

    @Override
    public boolean extractAll(List<ItemStack> items, List<FluidStack> fluids) {
        IStorageService s = storage();
        IEnergyService e = energy();
        if (s == null || e == null) return false;
        List<AEKey> keys = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        for (ItemStack it : items) {
            AEItemKey k = AEItemKey.of(it);
            if (k == null) return false;
            keys.add(k);
            amounts.add((long) it.getCount());
        }
        for (FluidStack f : fluids) {
            AEFluidKey k = AEFluidKey.of(f);
            if (k == null) return false;
            keys.add(k);
            amounts.add((long) f.getAmount());
        }
        // all there (and the power to move it)?
        for (int i = 0; i < keys.size(); i++)
            if (StorageHelper.poweredExtraction(e, s.getInventory(), keys.get(i), amounts.get(i), source, Actionable.SIMULATE) < amounts.get(i)) return false;
        for (int i = 0; i < keys.size(); i++) {
            long got = StorageHelper.poweredExtraction(e, s.getInventory(), keys.get(i), amounts.get(i), source, Actionable.MODULATE);
            if (got < amounts.get(i)) {                     // it went between the check and the taking: give back, fail
                for (int j = 0; j <= i; j++) {
                    long back = j == i ? got : amounts.get(j);
                    if (back > 0) StorageHelper.poweredInsert(e, s.getInventory(), keys.get(j), back, source, Actionable.MODULATE);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public long insert(ItemStack st) {
        IStorageService s = storage();
        IEnergyService e = energy();
        AEItemKey k = AEItemKey.of(st);
        return s == null || e == null || k == null || st.isEmpty() ? 0 : StorageHelper.poweredInsert(e, s.getInventory(), k, st.getCount(), source, Actionable.MODULATE);
    }

    @Override
    public long insert(FluidStack st) {
        IStorageService s = storage();
        IEnergyService e = energy();
        AEFluidKey k = AEFluidKey.of(st);
        return s == null || e == null || k == null || st.isEmpty() ? 0 : StorageHelper.poweredInsert(e, s.getInventory(), k, st.getAmount(), source, Actionable.MODULATE);
    }
}
