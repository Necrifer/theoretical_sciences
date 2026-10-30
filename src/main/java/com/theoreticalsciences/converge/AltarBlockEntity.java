package com.theoreticalsciences.converge;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.machine.QHEnergyStorage;
import com.theoreticalsciences.registry.QHLogistics;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Convergence Altar. One item sits on it (the core); one item on each Resonance Pedestal within 4 blocks (one block
 * up or down). Started by hand (empty hand, not sneaking) or by a redstone pulse, it draws its energy over the
 * recipe's time while beams run from the pedestals; then the pedestals are emptied and the core becomes the result.
 * <p>
 * Automation: pipes may put the core in (slot 0) and take the result out (slot 1); pedestals take one item each from
 * pipes; a comparator reads 15 when a result waits, 7 while working; a redstone pulse starts it.
 */
public class AltarBlockEntity extends BlockEntity {
    public static final int RADIUS = 4;
    public final QHEnergyStorage energy = new QHEnergyStorage(16_000_000_000L, Integer.MAX_VALUE, 0, this::setChanged);
    public final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            sync();
        }
    };
    @Nullable private ResourceLocation active;
    private int progress, duration;
    private long perTick;
    private final List<BlockPos> used = new ArrayList<>();
    @Nullable private UUID owner;
    private boolean powered;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.CONV_ALTAR.get(), pos, state);
    }

    public void setOwner(UUID id) {
        owner = id;
        setChanged();
    }

    public boolean crafting() {
        return active != null;
    }

    public float progress() {
        return duration <= 0 ? 0 : Math.min(1F, progress / (float) duration);
    }

    /** Smooth progress for drawing (the client counts on between the server's updates). */
    public float progress(float partial) {
        return duration <= 0 ? 0 : Math.min(1F, (progress + (crafting() ? partial : 0)) / (float) duration);
    }

    public int pedestalTier(BlockPos p) {
        return level != null && level.getBlockEntity(p) instanceof PedestalBlockEntity pe ? pe.tier() : 1;
    }

    /** The ritual's timeline: 0-0.7 the pedestals are drawn in one after another; then the hypersphere rises, sparks, collapses. */
    public static final float GATHER = 0.7F, RISE = 0.85F, SPARK = 0.95F;

    /** Where pedestal i is in its own part of the gathering: <0 not yet, 0-1 under way, >1 taken. */
    public float pedestalPhase(int i, float f) {
        int n = Math.max(1, used.size());
        return (f - GATHER * i / n) / (GATHER / n);
    }

    /** Client: count on smoothly, show the pedestals' items fading, throw sparks. */
    void clientTick() {
        if (level == null || !crafting()) return;
        if (progress < duration) progress++;
        float f = progress();
        for (int i = 0; i < used.size(); i++)
            if (level.getBlockEntity(used.get(i)) instanceof PedestalBlockEntity pe) {
                float u = pedestalPhase(i, f);
                pe.fade = u < 0.3F ? 1F : u > 0.7F ? 0F : 1F - (u - 0.3F) / 0.4F;
                pe.fadeUntil = level.getGameTime() + 30;
            }
        if (f > RISE && f < SPARK) {
            var r = level.random;
            double y = worldPosition.getY() + 2.1;
            for (int k = 0; k < 3; k++)
                level.addParticle(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), worldPosition.getX() + 0.5 + (r.nextDouble() - 0.5) * 1.6,
                        y + (r.nextDouble() - 0.5) * 1.6, worldPosition.getZ() + 0.5 + (r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.4);
        }
    }

    public ItemStack core() {
        return items.getStackInSlot(0);
    }

    public ItemStack output() {
        return items.getStackInSlot(1);
    }

    public List<BlockPos> usedPedestals() {
        return used;
    }

    void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ------------------------------------------------------------------ starting
    List<PedestalBlockEntity> pedestals() {
        List<PedestalBlockEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-RADIUS, -1, -RADIUS), worldPosition.offset(RADIUS, 1, RADIUS)))
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof PedestalBlockEntity pe && !pe.stack().isEmpty() && !pe.locked()) out.add(pe);   // not another altar's
        return out;
    }

    /** Tries to start; tells the player (if any) why it cannot. */
    public void start(@Nullable Player who) {
        if (!(level instanceof ServerLevel sl) || crafting()) return;
        if (core().isEmpty()) {
            tell(who, "message.theoreticalsciences.altar_no_core");
            return;
        }
        List<PedestalBlockEntity> peds = pedestals();
        List<ItemStack> stacks = new ArrayList<>();
        for (PedestalBlockEntity pe : peds) stacks.add(pe.stack());
        ConvergenceRecipe.Input in = new ConvergenceRecipe.Input(core(), stacks);
        RecipeHolder<ConvergenceRecipe> found = null;
        for (RecipeHolder<ConvergenceRecipe> h : sl.getRecipeManager().getAllRecipesFor(ConvergenceRegistry.TYPE.get()))
            if (h.value().matches(in, sl)) {
                found = h;
                break;
            }
        if (found == null) {
            tell(who, "message.theoreticalsciences.altar_no_recipe");
            return;
        }
        ConvergenceRecipe r = found.value();
        for (PedestalBlockEntity pe : peds)
            if (pe.tier() < r.tier) {
                tell(who, "message.theoreticalsciences.altar_weak", r.tier);
                return;
            }
        if (r.hasResearch() && QHConfig.RESEARCH_REQUIRED.get()) {
            ResourceLocation id = ResourceLocation.tryParse(r.research);
            UUID u = who != null ? who.getUUID() : owner;
            if (id != null && (u == null || !ResearchService.has(sl.getServer(), u, id))) {
                tell(who, "message.theoreticalsciences.altar_research");
                return;
            }
        }
        ItemStack out = output();
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, r.result) || out.getCount() + r.result.getCount() > out.getMaxStackSize())) {
            tell(who, "message.theoreticalsciences.altar_full");
            return;
        }
        active = found.id();
        duration = r.duration;
        progress = 0;
        perTick = (r.energy + r.duration - 1) / r.duration;
        used.clear();
        for (PedestalBlockEntity pe : peds) {
            used.add(pe.getBlockPos().immutable());
            pe.lockedUntil = Long.MAX_VALUE;
        }
        sl.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 1F, 0.7F);
        sync();
    }

    private void tell(@Nullable Player p, String key, Object... args) {
        if (p != null) p.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    public void setPowered(boolean now) {
        powered = now;
    }

    public void redstone(boolean now) {
        if (now && !powered) start(null);
        powered = now;
    }

    // ------------------------------------------------------------------ working
    void serverTick() {
        if (!(level instanceof ServerLevel sl) || active == null) return;
        RecipeHolder<?> h = sl.getRecipeManager().byKey(active).orElse(null);
        if (!(h != null && h.value() instanceof ConvergenceRecipe r)) {
            abort(sl);
            return;
        }
        // round 36: a pedestal in a chunk that is not loaded (only the altar's chunk kept by a chunk loader) - wait for
        // it instead of loading its chunk or calling the ritual off
        for (BlockPos p : used) if (!sl.isLoaded(p)) return;
        if ((sl.getGameTime() % 20 == 0 || progress + 1 >= duration) && !stillMatches(sl, r)) {
            abort(sl);
            return;
        }
        if (energy.stored() < perTick) {                  // waits for power
            if (sl.getGameTime() % 20 == 0) sync();
            return;
        }
        energy.consume(perTick);
        progress++;
        if (progress % 4 == 0) beams(sl);
        if (progress >= duration) finish(sl, r);
        else if (progress % 10 == 0) sync();
        else if (sl.getGameTime() % 20 == 0) sync();          // (also while it waits for power, so the client stops counting)
    }

    private boolean stillMatches(ServerLevel sl, ConvergenceRecipe r) {
        if (!r.core.test(core())) return false;
        List<ItemStack> stacks = new ArrayList<>();
        for (BlockPos p : used) {
            if (!(sl.getBlockEntity(p) instanceof PedestalBlockEntity pe) || pe.stack().isEmpty()) return false;
            pe.lockedUntil = Long.MAX_VALUE;          // (again, in case the pedestal was reloaded)
            stacks.add(pe.stack());
        }
        return r.pedestalMatch(stacks) != null;
    }

    /** (The beams, hypersphere and sparks are drawn by the client renderer; only a faint shimmer at the core here.) */
    private void beams(ServerLevel sl) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 1.3, cz = worldPosition.getZ() + 0.5;
        sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), cx, cy, cz, 2, 0.2, 0.2, 0.2, 0.03);
    }

    private void finish(ServerLevel sl, ConvergenceRecipe r) {
        // last look: every pedestal still holds its item, and the result still fits
        ItemStack o = output();
        if (!stillMatches(sl, r) || (!o.isEmpty() && (!ItemStack.isSameItemSameComponents(o, r.result) || o.getCount() + r.result.getCount() > o.getMaxStackSize()))) {
            abort(sl);
            return;
        }
        for (BlockPos p : used)
            if (sl.getBlockEntity(p) instanceof PedestalBlockEntity pe) {
                pe.lockedUntil = 0;
                pe.item.setStackInSlot(0, ItemStack.EMPTY);
            }
        items.getStackInSlot(0).shrink(1);
        ItemStack out = output();
        if (out.isEmpty()) items.setStackInSlot(1, r.result.copy());
        else out.grow(r.result.getCount());
        sl.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, 1, 0, 0, 0, 0);
        sl.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.BLOCKS, 1.2F, 0.6F);
        reset();
    }

    private void abort(ServerLevel sl) {
        sl.playSound(null, worldPosition, com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 1F, 0.8F);
        for (BlockPos p : used) if (sl.getBlockEntity(p) instanceof PedestalBlockEntity pe) pe.lockedUntil = 0;
        reset();
    }

    private void reset() {
        active = null;
        progress = 0;
        duration = 0;
        used.clear();
        sync();
    }

    public int comparator() {
        return !output().isEmpty() ? 15 : crafting() ? 7 : 0;
    }

    /** Pipes: core in (when idle and empty), result out. */
    public final IItemHandler automation = new IItemHandler() {
        public int getSlots() {
            return 2;
        }

        public ItemStack getStackInSlot(int s) {
            return items.getStackInSlot(s);
        }

        public ItemStack insertItem(int s, ItemStack st, boolean sim) {
            return s != 0 || crafting() ? st : items.insertItem(0, st, sim);
        }

        public ItemStack extractItem(int s, int n, boolean sim) {
            return s != 1 ? ItemStack.EMPTY : items.extractItem(1, n, sim);
        }

        public int getSlotLimit(int s) {
            return s == 0 ? 1 : 64;
        }

        public boolean isItemValid(int s, ItemStack st) {
            return s == 0;
        }
    };

    // ------------------------------------------------------------------ saving
    /**
     * What its dropped item keeps: only the stored energy (the items on it are dropped separately when it breaks),
     * or null when it has none.
     */
    public CompoundTag keptData() {
        if (energy.stored() <= 0) return null;
        CompoundTag t = new CompoundTag();
        t.putLong("Energy", energy.stored());
        return t;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("Items", items.serializeNBT(regs));
        tag.putLong("Energy", energy.stored());
        if (active != null) {
            tag.putString("Active", active.toString());
            tag.putInt("Progress", progress);
            tag.putInt("Duration", duration);
            tag.putLong("PerTick", perTick);
            tag.putLongArray("Used", used.stream().mapToLong(BlockPos::asLong).toArray());
        }
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putBoolean("Powered", powered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        items.deserializeNBT(regs, tag.getCompound("Items"));
        energy.setRaw(tag.getLong("Energy"));
        active = tag.contains("Active") ? ResourceLocation.tryParse(tag.getString("Active")) : null;
        progress = tag.getInt("Progress");
        duration = tag.getInt("Duration");
        perTick = tag.getLong("PerTick");
        used.clear();
        for (long l : tag.getLongArray("Used")) used.add(BlockPos.of(l));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        powered = tag.getBoolean("Powered");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // pedestals forget their lock on reload: lock them again while this craft goes on
        if (level != null && !level.isClientSide && active != null)
            for (BlockPos p : used) if (level.isLoaded(p) && level.getBlockEntity(p) instanceof PedestalBlockEntity pe) pe.lockedUntil = Long.MAX_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag t = new CompoundTag();
        t.put("Items", items.serializeNBT(regs));
        if (active != null) {
            t.putString("Active", active.toString());
            t.putInt("Progress", progress);
            t.putInt("Duration", duration);
            t.putLongArray("Used", used.stream().mapToLong(BlockPos::asLong).toArray());
        }
        return t;
    }

    @Override
    public void handleUpdateTag(CompoundTag t, HolderLookup.Provider regs) {
        items.deserializeNBT(regs, t.getCompound("Items"));
        active = t.contains("Active") ? ResourceLocation.tryParse(t.getString("Active")) : null;
        progress = t.getInt("Progress");
        duration = t.getInt("Duration");
        used.clear();
        for (long l : t.getLongArray("Used")) used.add(BlockPos.of(l));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, regs) -> ((AltarBlockEntity) be).getUpdateTag(regs));
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        handleUpdateTag(pkt.getTag(), regs);
    }

    public void status(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("message.theoreticalsciences.altar_status", Math.round(progress() * 100),
                String.format("%,d", energy.stored()), String.format("%,d", perTick)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
