package com.theoreticalsciences.transcend;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Beyond Omnipotence cannot be copied.
 * <p>
 * The Omnipotence Core can make anything, including other mods' creative duplicators. Everything that comes after the
 * Sundering (item tag {@code theoreticalsciences:beyond_omnipotence}, fluid tag of the same name) must stay scarce:
 * <ul>
 * <li><b>Duplicator blocks</b> (config {@code creativeDuplicatorBlocks}: creative chests, bins, tanks, drums): their item and
 *     fluid handlers refuse those items and fluids, right-clicking one with them is refused, and any that get in anyway
 *     (through the block's own screen) are wiped from it within half a second when a player is near.</li>
 * <li><b>Storage that becomes a duplicator through an installed part</b> (config {@code creativeDuplicatorHosts}: drawers with
 *     a Creative Vending Upgrade): the same, but only while the part is installed - ordinary drawers are left alone.</li>
 * <li><b>Duplicating parts</b> (item tag {@code theoreticalsciences:creative_duplicator_parts}: creative storage cells...): a part
 *     configured to copy such an item has that setting removed - only inside the part's own data, wherever it is (an ME
 *     drive, a chest, a player's inventory). Nothing else in the container is touched.</li>
 * </ul>
 */
public final class Uncopyable {
    public static final TagKey<Item> BEYOND = OmnipotencePayload.BEYOND;
    public static final TagKey<Fluid> BEYOND_FLUIDS = TagKey.create(Registries.FLUID, TheoreticalSciences.id("beyond_omnipotence"));
    public static final TagKey<Item> PARTS = TagKey.create(Registries.ITEM, TheoreticalSciences.id("creative_duplicator_parts"));

    public static final List<String> DEFAULT_BLOCKS = List.of(
            "excessive_utilities:creative_chest", "excessive_utilities:creative_drum",
            "gtceu:creative_chest", "gtceu:creative_tank",
            "mekanism:creative_bin", "mekanism:creative_fluid_tank", "mekanism:creative_chemical_tank",
            "oritech:creative_storage_block", "oritech:creative_tank_block",
            "woot_revived:creative_tank", "thaumaturge:jar_creative", "ars_nouveau:creative_source_jar");
    /** Mods whose storage blocks turn into duplicators with a vending-style upgrade installed. */
    public static final List<String> DEFAULT_HOSTS = List.of("storagedrawers", "functionalstorage");
    /** Blocks that hold duplicating parts (cells): checked for configured parts only. */
    private static final Set<String> CELL_HOLDERS = Set.of("ae2:drive", "ae2:chest", "ae2:cell_workbench", "extendedae:ex_drive", "advanced_ae:quantum_drive");

    private static final ThreadLocal<Boolean> INSIDE = ThreadLocal.withInitial(() -> false);

    private Uncopyable() {}

    // ------------------------------------------------------------------ which blocks
    static Set<ResourceLocation> duplicatorBlocks() {
        Set<ResourceLocation> out = new HashSet<>();
        List<? extends String> ids;
        try {
            ids = QHConfig.CREATIVE_DUPLICATOR_BLOCKS.get();
        } catch (IllegalStateException e) {
            ids = DEFAULT_BLOCKS;               // the config is not loaded yet
        }
        for (String s : ids) {
            ResourceLocation r = ResourceLocation.tryParse(s);
            if (r != null) out.add(r);
        }
        return out;
    }

    static Set<String> hosts() {
        try {
            return new HashSet<>(QHConfig.CREATIVE_DUPLICATOR_HOSTS.get());
        } catch (IllegalStateException e) {
            return new HashSet<>(DEFAULT_HOSTS);
        }
    }

    private static Set<ResourceLocation> cachedBlocks;
    private static Set<String> cachedHosts;
    private static int cachedAt = Integer.MIN_VALUE;
    /** Host blocks seen with a vending part installed, and when that was checked (server tick). */
    private static final Map<BlockEntity, long[]> VENDING = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Set<BlockPos> WARNED = com.theoreticalsciences.world.SessionMaps.perServer(new HashSet<>());   // (round 36: cleared when the server stops)

    private static void refresh(int now) {
        if (cachedBlocks != null && now - cachedAt < 200 && now >= cachedAt) return;
        cachedAt = now;
        cachedBlocks = duplicatorBlocks();
        cachedHosts = hosts();
    }

    private static ResourceLocation key(BlockEntity be) {
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock());
    }

    static boolean isDuplicatorBlock(BlockEntity be) {
        return cachedBlocks != null && cachedBlocks.contains(key(be));
    }

    static boolean isHost(BlockEntity be) {
        return cachedHosts != null && cachedHosts.contains(key(be).getNamespace());
    }

    static boolean isCellHolder(BlockEntity be) {
        return CELL_HOLDERS.contains(key(be).toString());
    }

    public static boolean beyond(ItemStack s) {
        return !s.isEmpty() && s.is(BEYOND);
    }

    public static boolean beyond(FluidStack s) {
        return !s.isEmpty() && s.getFluid().builtInRegistryHolder().is(BEYOND_FLUIDS);
    }

    // ------------------------------------------------------------------ 1) the handlers refuse
    /**
     * Mod bus, highest priority: these providers are asked first, so theirs is the handler every pipe and interface
     * gets. Registered per block (not per block-entity type, which may be shared with ordinary tiers).
     */
    public static void registerCaps(RegisterCapabilitiesEvent e) {
        List<Block> dup = new ArrayList<>(), host = new ArrayList<>();
        for (ResourceLocation id : duplicatorBlocks()) BuiltInRegistries.BLOCK.getOptional(id).ifPresent(dup::add);
        Set<String> hs = hosts();
        for (Block b : BuiltInRegistries.BLOCK) if (hs.contains(BuiltInRegistries.BLOCK.getKey(b).getNamespace()) && b.defaultBlockState().hasBlockEntity()) host.add(b);
        if (!dup.isEmpty()) {
            e.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
                if (INSIDE.get() || be == null) return null;
                IItemHandler in = original(Capabilities.ItemHandler.BLOCK, be, side);
                return in == null ? null : new Items(in, null);
            }, dup.toArray(Block[]::new));
            e.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, side) -> {
                if (INSIDE.get() || be == null) return null;
                IFluidHandler in = original(Capabilities.FluidHandler.BLOCK, be, side);
                return in == null ? null : new Fluids(in);
            }, dup.toArray(Block[]::new));
        }
        if (!host.isEmpty())
            e.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
                if (INSIDE.get() || be == null) return null;
                IItemHandler in = original(Capabilities.ItemHandler.BLOCK, be, side);
                return in == null ? null : new Items(in, be);        // refuses only while a vending part is installed
            }, host.toArray(Block[]::new));
    }

    /** The handler the block's own mod provides (asked with ours switched off). */
    static <T, C> T original(net.neoforged.neoforge.capabilities.BlockCapability<T, C> cap, BlockEntity be, C side) {
        if (be.getLevel() == null) return null;
        INSIDE.set(true);
        try {
            return cap.getCapability(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, side);
        } finally {
            INSIDE.set(false);
        }
    }

    /** {@code host == null}: always a duplicator; otherwise only while that block has a vending part installed. */
    record Items(IItemHandler in, BlockEntity host) implements IItemHandler {
        private boolean copies() {
            return host == null || vending(host);
        }

        public int getSlots() {
            return in.getSlots();
        }

        public ItemStack getStackInSlot(int slot) {
            return in.getStackInSlot(slot);
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return beyond(stack) && copies() ? stack : in.insertItem(slot, stack, simulate);
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return beyond(in.getStackInSlot(slot)) && copies() ? ItemStack.EMPTY : in.extractItem(slot, amount, simulate);
        }

        public int getSlotLimit(int slot) {
            return in.getSlotLimit(slot);
        }

        public boolean isItemValid(int slot, ItemStack stack) {
            return !(beyond(stack) && copies()) && in.isItemValid(slot, stack);
        }
    }

    record Fluids(IFluidHandler in) implements IFluidHandler {
        public int getTanks() {
            return in.getTanks();
        }

        public FluidStack getFluidInTank(int tank) {
            return in.getFluidInTank(tank);
        }

        public int getTankCapacity(int tank) {
            return in.getTankCapacity(tank);
        }

        public boolean isFluidValid(int tank, FluidStack stack) {
            return !beyond(stack) && in.isFluidValid(tank, stack);
        }

        public int fill(FluidStack resource, FluidAction action) {
            return beyond(resource) ? 0 : in.fill(resource, action);
        }

        public FluidStack drain(FluidStack resource, FluidAction action) {
            return beyond(resource) ? FluidStack.EMPTY : in.drain(resource, action);
        }

        public FluidStack drain(int maxDrain, FluidAction action) {
            for (int i = 0; i < in.getTanks(); i++) if (beyond(in.getFluidInTank(i))) return FluidStack.EMPTY;
            return in.drain(maxDrain, action);
        }
    }

    /** Whether a host block has a vending-style part installed (its saved data names one); re-checked every 2 s. */
    static boolean vending(BlockEntity be) {
        if (be.getLevel() == null || be.getLevel().getServer() == null) return false;
        int now = be.getLevel().getServer().getTickCount();
        long[] c = VENDING.get(be);
        if (c != null && now - c[0] < 40 && now >= c[0]) return c[1] != 0;
        boolean v = namesPart(be.saveWithFullMetadata(be.getLevel().registryAccess()));
        VENDING.put(be, new long[]{now, v ? 1 : 0});
        return v;
    }

    // ------------------------------------------------------------------ 2) no putting it in by hand
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack held = e.getItemStack();
        boolean b = beyond(held) || FluidUtil.getFluidContained(held).map(Uncopyable::beyond).orElse(false);
        if (!b) return;
        BlockEntity be = e.getLevel().getBlockEntity(e.getPos());
        if (be == null) return;
        refresh(sp.server.getTickCount());
        if (isDuplicatorBlock(be) || (isHost(be) && vending(be))) {
            e.setCanceled(true);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.uncopyable_refuse").withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ------------------------------------------------------------------ 3) watch the duplicators near players
    public static void onServerTick(ServerTickEvent.Post e) {
        var server = e.getServer();
        int now = server.getTickCount();
        if (now % 10 != 3) return;
        refresh(now);
        boolean slow = now % 40 == 3;
        Map<ServerLevel, Set<Long>> seen = new HashMap<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            ServerLevel sl = sp.serverLevel();
            if (slow) cleanParts(sp);
            Set<Long> done = seen.computeIfAbsent(sl, k -> new HashSet<>());
            ChunkPos c = sp.chunkPosition();
            for (int dx = -4; dx <= 4; dx++)
                for (int dz = -4; dz <= 4; dz++) {
                    if (!done.add(ChunkPos.asLong(c.x + dx, c.z + dz))) continue;
                    LevelChunk ch = sl.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                    if (ch == null || ch.getBlockEntities().isEmpty()) continue;
                    List<BlockEntity> list = null;
                    for (BlockEntity be : ch.getBlockEntities().values()) {
                        if (be.isRemoved()) continue;
                        if (isDuplicatorBlock(be) || (slow && (isHost(be) || isCellHolder(be)))) {
                            if (list == null) list = new ArrayList<>();
                            list.add(be);
                        }
                    }
                    if (list == null) continue;
                    for (BlockEntity be : list) {
                        if (be.isRemoved()) continue;
                        if (isDuplicatorBlock(be)) {
                            if (holdsBeyond(be)) wipeAll(be, sl, sp);
                        } else if (isHost(be)) {
                            if (vending(be)) {
                                CompoundTag t = be.saveWithFullMetadata(sl.registryAccess());
                                if (mentionsBeyond(t)) wipeAll(be, sl, sp);
                            }
                        } else wipeParts(be, sl, sp);
                    }
                }
        }
    }

    /** Quick look through the duplicator's own handlers, then its saved data (a template slot, a filter). */
    private static boolean holdsBeyond(BlockEntity be) {
        IItemHandler ih = original(Capabilities.ItemHandler.BLOCK, be, null);
        if (ih != null)
            for (int i = 0; i < ih.getSlots(); i++) if (beyond(ih.getStackInSlot(i))) return true;
        IFluidHandler fh = original(Capabilities.FluidHandler.BLOCK, be, null);
        if (fh != null)
            for (int i = 0; i < fh.getTanks(); i++) if (beyond(fh.getFluidInTank(i))) return true;
        return mentionsBeyond(be.saveWithFullMetadata(be.getLevel().registryAccess()));
    }

    // ------------------------------------------------------------------ the saved-data side
    private static boolean namesPart(Tag t) {
        if (t instanceof CompoundTag c) {
            if (isPartStack(c)) return true;
            for (String k : c.getAllKeys()) if (namesPart(c.get(k))) return true;
        } else if (t instanceof ListTag l) {
            for (Tag x : l) if (namesPart(x)) return true;
        } else if (t instanceof StringTag s) {
            return isPartId(s.getAsString());
        }
        return false;
    }

    private static boolean isPartStack(CompoundTag c) {
        return c.get("id") instanceof StringTag s && isPartId(s.getAsString());
    }

    private static boolean isPartId(String s) {
        if (s.indexOf(':') < 0) return false;
        ResourceLocation r = ResourceLocation.tryParse(s);
        if (r == null) return false;
        Item it = BuiltInRegistries.ITEM.getOptional(r).orElse(null);
        return it != null && it != net.minecraft.world.item.Items.AIR && it.builtInRegistryHolder().is(PARTS);
    }

    private static boolean mentionsBeyond(Tag t) {
        if (t instanceof StringTag s) return isBeyondId(s.getAsString());
        if (t instanceof CompoundTag c) {
            for (String k : c.getAllKeys()) if (mentionsBeyond(c.get(k))) return true;
        } else if (t instanceof ListTag l) {
            for (Tag x : l) if (mentionsBeyond(x)) return true;
        }
        return false;
    }

    static boolean isBeyondId(String s) {
        if (s.indexOf(':') < 0) return false;
        ResourceLocation r = ResourceLocation.tryParse(s);
        if (r == null) return false;
        Item it = BuiltInRegistries.ITEM.getOptional(r).orElse(null);
        if (it != null && it != net.minecraft.world.item.Items.AIR && it.builtInRegistryHolder().is(BEYOND)) return true;
        Fluid f = BuiltInRegistries.FLUID.getOptional(r).orElse(null);
        return f != null && f != net.minecraft.world.level.material.Fluids.EMPTY && f.builtInRegistryHolder().is(BEYOND_FLUIDS);
    }

    /** A duplicator (or a host with a vending part): every trace of what lies beyond Omnipotence goes. */
    private static void wipeAll(BlockEntity be, ServerLevel sl, ServerPlayer near) {
        CompoundTag t = be.saveWithFullMetadata(sl.registryAccess());
        List<String> gone = new ArrayList<>();
        if (!strip(t, gone)) {
            if (WARNED.add(be.getBlockPos().immutable()))
                TheoreticalSciences.LOGGER.warn("Beyond Omnipotence in the duplicator at {} could not be removed from its saved data", be.getBlockPos());
            return;
        }
        reload(be, t, sl, near, gone);
    }

    /** A block holding parts (an ME drive...): only the parts' own settings are cleaned. */
    private static void wipeParts(BlockEntity be, ServerLevel sl, ServerPlayer near) {
        CompoundTag t = be.saveWithFullMetadata(sl.registryAccess());
        List<String> gone = new ArrayList<>();
        if (stripInParts(t, gone)) reload(be, t, sl, near, gone);
    }

    private static void reload(BlockEntity be, CompoundTag t, ServerLevel sl, ServerPlayer near, List<String> gone) {
        be.loadWithComponents(t, sl.registryAccess());
        be.setChanged();
        BlockPos p = be.getBlockPos();
        sl.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), 3);
        tell(near, gone);
        TheoreticalSciences.LOGGER.info("Beyond Omnipotence cannot be copied: removed {} from {} at {} in {}", gone, key(be), p, sl.dimension().location());
    }

    private static void tell(ServerPlayer near, List<String> gone) {
        String what = gone.isEmpty() ? "?" : gone.get(0);
        Item it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(what)).orElse(null);
        Component name = it != null && it != net.minecraft.world.item.Items.AIR ? new ItemStack(it).getHoverName() : Component.literal(what);
        near.displayClientMessage(Component.translatable("message.theoreticalsciences.uncopyable", name).withStyle(ChatFormatting.GOLD), false);
    }

    /** Parts in a player's inventory (a creative cell configured in a workbench and carried off). */
    private static void cleanParts(ServerPlayer sp) {
        HolderLookup.Provider regs = sp.registryAccess();
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.is(PARTS) || s.getComponentsPatch().isEmpty()) continue;
            Tag saved = s.save(regs);
            if (!(saved instanceof CompoundTag ct) || !mentionsBeyond(ct)) continue;
            List<String> gone = new ArrayList<>();
            if (!strip(ct.contains("components") ? ct.getCompound("components") : ct, gone)) continue;
            ItemStack fixed = ItemStack.parseOptional(regs, ct);
            if (fixed.isEmpty()) continue;
            inv.setItem(i, fixed);
            tell(sp, gone);
        }
    }

    /** Strips beyond ids inside every part-item compound found (and nowhere else). */
    static boolean stripInParts(Tag t, List<String> gone) {
        boolean changed = false;
        if (t instanceof CompoundTag c) {
            if (isPartStack(c)) {
                Tag comp = c.get("components");
                return comp != null && strip(comp, gone);
            }
            for (String k : c.getAllKeys()) changed |= stripInParts(c.get(k), gone);
        } else if (t instanceof ListTag l) {
            for (Tag x : l) changed |= stripInParts(x, gone);
        }
        return changed;
    }

    /**
     * Drops every compound naming a beyond item or fluid ("id"/"Name"/"FluidName"/"item"/"fluid") and every bare string
     * naming one. In lists the entry is replaced by an empty one, so positional lists (tank N, slot N) keep their order.
     */
    static boolean strip(Tag t, List<String> gone) {
        boolean changed = false;
        if (t instanceof CompoundTag c) {
            for (String k : new ArrayList<>(c.getAllKeys())) {
                Tag v = c.get(k);
                if (v instanceof StringTag s && isBeyondId(s.getAsString())) {
                    gone.add(s.getAsString());
                    c.remove(k);
                    changed = true;
                } else if (v instanceof CompoundTag sub && names(sub)) {
                    gone.add(nameOf(sub));
                    c.remove(k);
                    changed = true;
                } else if (v != null) changed |= strip(v, gone);
            }
        } else if (t instanceof ListTag l) {
            for (int i = 0; i < l.size(); i++) {
                Tag v = l.get(i);
                if (v instanceof CompoundTag sub && names(sub)) {
                    gone.add(nameOf(sub));
                    l.set(i, new CompoundTag());
                    changed = true;
                } else if (v instanceof StringTag s && isBeyondId(s.getAsString())) {
                    gone.add(s.getAsString());
                    l.set(i, StringTag.valueOf("minecraft:air"));
                    changed = true;
                } else changed |= strip(v, gone);
            }
        }
        return changed;
    }

    private static final String[] NAME_KEYS = {"id", "Name", "FluidName", "item", "fluid"};

    private static boolean names(CompoundTag c) {
        for (String k : NAME_KEYS)
            if (c.get(k) instanceof StringTag s && isBeyondId(s.getAsString())) return true;
        return false;
    }

    private static String nameOf(CompoundTag c) {
        for (String k : NAME_KEYS)
            if (c.get(k) instanceof StringTag s && isBeyondId(s.getAsString())) return s.getAsString();
        return "?";
    }
}
