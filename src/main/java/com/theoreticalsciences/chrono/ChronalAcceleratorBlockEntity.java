package com.theoreticalsciences.chrono;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.machine.QHEnergyStorage;
import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

/**
 * The field. Every tick, while switched on and fed, each block entity inside the cube of the chosen radius is ticked
 * (speed - 1) extra times, and each randomly-ticking block (crops, saplings, ores growing...) gets (speed - 1) times its
 * normal chance of a random tick. Every extra tick costs energy (config: chronalEnergyPerTick, chronalEnergyPerRandomTick),
 * and every 8 extra ticks one mB of Chronon Gas. Short of either, the field does nothing that tick.
 * Never accelerated: other accelerators, anything in the tag theoreticalsciences:not_accelerated.
 */
public class ChronalAcceleratorBlockEntity extends BlockEntity {
    public static final int[] SPEEDS = {2, 4, 8, 16, 32, 64, 128, 256};
    public static final TagKey<net.minecraft.world.level.block.Block> BLACKLIST =
            TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "not_accelerated"));
    public final QHEnergyStorage energy = new QHEnergyStorage(4_000_000_000L, Integer.MAX_VALUE, 0, this::setChanged);
    public final FluidTank chronons = new FluidTank(256_000, s -> s.getFluid().isSame(chrononFluid())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int speedIdx, radius = 1;
    private double gasDebt;
    private final List<BlockPos> field = new ArrayList<>();
    private long lastScan = -1000;
    private long lastCost;
    private int litHold;

    public ChronalAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.CHRONAL_ACCELERATOR.get(), pos, state);
    }

    private static net.minecraft.world.level.material.Fluid chrononFluid() {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "chronon_gas"));
    }

    public int speed() {
        return Math.min(SPEEDS[speedIdx], QHConfig.CHRONAL_MAX_SPEED.get());
    }

    public void use(Player p, boolean sneak) {
        if (sneak) {
            radius = radius >= QHConfig.CHRONAL_MAX_RADIUS.get() ? 1 : radius + 1;
            lastScan = -1000;
        } else speedIdx = (speedIdx + 1) % SPEEDS.length;
        if (SPEEDS[speedIdx] > QHConfig.CHRONAL_MAX_SPEED.get()) speedIdx = 0;
        setChanged();
        int side = 2 * radius + 1;
        p.displayClientMessage(Component.translatable("message.theoreticalsciences.chronal", speed(), side, side, side,
                String.format("%,d", energy.stored()), String.format("%,d", chronons.getFluidAmount()), String.format("%,d", lastCost)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        boolean off = sl.hasNeighborSignal(worldPosition);
        long now = sl.getGameTime();
        if (now - lastScan >= 40) rescan(sl);
        boolean lit = false;
        lastCost = 0;
        if (!off) {
            int extra = speed() - 1;
            long perTick = QHConfig.CHRONAL_ENERGY_PER_TICK.get(), perRandom = QHConfig.CHRONAL_ENERGY_PER_RANDOM_TICK.get();   // (widened from int)
            int randomSpeed = sl.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            for (BlockPos p : field) {
                if (!sl.shouldTickBlocksAt(p)) continue;          // only chunks that tick by themselves
                BlockState st = sl.getBlockState(p);
                BlockEntity be = st.hasBlockEntity() ? sl.getBlockEntity(p) : null;
                if (be != null && !be.isRemoved() && be.getType().isValid(st)) {
                    @SuppressWarnings("unchecked") BlockEntityTicker<BlockEntity> t = (BlockEntityTicker<BlockEntity>) st.getTicker(sl, be.getType());
                    if (t != null) {
                        long cost = perTick > Long.MAX_VALUE / Math.max(1, extra) ? Long.MAX_VALUE : perTick * extra;
                        if (!pay(cost, extra)) break;
                        net.minecraft.world.level.block.Block block = st.getBlock();
                        for (int k = 0; k < extra && !be.isRemoved(); k++) {
                            BlockState now2 = sl.getBlockState(p);          // the block may change itself (a furnace lighting up)
                            if (now2.getBlock() != block || sl.getBlockEntity(p) != be) break;
                            t.tick(sl, p, now2, be);
                        }
                        lit = true;
                    }
                }
                if (st.isRandomlyTicking()) {
                    // the chance of a random tick per block per tick is randomTickSpeed/4096: here (speed - 1) times that
                    double expect = extra * randomSpeed / 4096.0;
                    int n = (int) expect + (sl.random.nextDouble() < expect - (int) expect ? 1 : 0);
                    if (n > 0) {
                        if (!pay(perRandom > Long.MAX_VALUE / n ? Long.MAX_VALUE : perRandom * n, n)) break;
                        for (int k = 0; k < n; k++) {
                            BlockState cur = sl.getBlockState(p);
                            if (!cur.isRandomlyTicking()) break;
                            cur.randomTick(sl, p, sl.random);
                        }
                        lit = true;
                    }
                }
            }
        }
        // glow while working; held for a second so a crop field (random ticks now and then) does not flicker the light
        if (lit) litHold = 20;
        else if (litHold > 0) {
            litHold--;
            lit = !off;
        }
        BlockState me = getBlockState();
        if (me.hasProperty(ChronalAcceleratorBlock.LIT) && me.getValue(ChronalAcceleratorBlock.LIT) != lit)
            sl.setBlock(worldPosition, me.setValue(ChronalAcceleratorBlock.LIT, lit), Block.UPDATE_CLIENTS);
    }

    /** Takes the energy and Chronon Gas for this many extra ticks; false (and nothing taken) if either is short. */
    private boolean pay(long fe, int ticks) {
        double gas = gasDebt + ticks / 8.0;
        int mb = (int) gas;
        if (energy.stored() < fe || chronons.getFluidAmount() < mb) return false;
        energy.consume(fe);
        if (mb > 0) chronons.drain(mb, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        gasDebt = gas - mb;
        lastCost += fe;
        return true;
    }

    private void rescan(ServerLevel sl) {
        lastScan = sl.getGameTime();
        field.clear();
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-radius, -radius, -radius), worldPosition.offset(radius, radius, radius))) {
            if (p.equals(worldPosition) || !sl.isLoaded(p)) continue;
            BlockState st = sl.getBlockState(p);
            if (st.isAir() || st.is(BLACKLIST) || st.getBlock() instanceof ChronalAcceleratorBlock) continue;
            // Round 29: never a multiblock controller - each of its ticks can re-scan and re-sync the whole structure,
            // and doing that up to 256 times a tick stalls the server
            if (st.hasBlockEntity() && sl.getBlockEntity(p) instanceof com.theoreticalsciences.machine.ControllerBlockEntity) continue;
            if (st.hasBlockEntity() || st.isRandomlyTicking()) field.add(p.immutable());
        }
    }

    public QHEnergyStorage energy() {
        return energy;
    }

    /** What its dropped item keeps (stored energy and Chronon Gas, and its settings), or null when it is empty. */
    public CompoundTag keptData() {
        return (energy.stored() > 0 || chronons.getFluidAmount() > 0) && level != null ? saveCustomOnly(level.registryAccess()) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putLong("Energy", energy.stored());
        tag.put("Chronons", chronons.writeToNBT(regs, new CompoundTag()));
        tag.putInt("Speed", speedIdx);
        tag.putInt("Radius", radius);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        energy.setRaw(tag.getLong("Energy"));
        chronons.readFromNBT(regs, tag.getCompound("Chronons"));
        speedIdx = Math.max(0, Math.min(SPEEDS.length - 1, tag.getInt("Speed")));
        radius = Math.max(1, tag.contains("Radius") ? tag.getInt("Radius") : 1);
    }

    @SuppressWarnings("unused")
    private static FluidStack none() {
        return FluidStack.EMPTY;
    }
}
