package com.theoreticalsciences.logistics;

import com.theoreticalsciences.registry.QHLogistics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * 1.3.0: the Laminar Flow Conduit's logic.
 * <p>
 * Built to stay cheap in a large modpack: every segment ticks, but only a line's first segment (its inlet) does any
 * work, and only every {@link #PERIOD} ticks, staggered by position. It then walks its own line (at most
 * {@link #MAX_SEGMENTS} blocks, never into an unloaded chunk) and moves one batch with the source's and target's
 * fluid handlers, which it keeps in capability caches. The segments hold no fluid, store nothing and send nothing to
 * clients, so a line costs nothing when idle beyond the walk.
 * <p>
 * Rate: smooth (laminar) flow through a pipe falls as 1/L (Poiseuille), so a line passes {@link #BASE_RATE} mB a tick up
 * to {@link #FREE_LENGTH} segments and proportionally less beyond.
 */
public class FlowConduitBlockEntity extends BlockEntity {
    public static final int PERIOD = 10;
    public static final int MAX_SEGMENTS = 64;
    public static final int BASE_RATE = 2000;
    public static final int FREE_LENGTH = 16;

    /** A traced line: its length, the block it feeds and the face it feeds into, and whether it loops on itself. */
    public record Line(int segments, BlockPos target, Direction into, boolean closed) {}

    private BlockCapabilityCache<IFluidHandler, Direction> source, sink;
    private Direction sourceFacing;

    public FlowConduitBlockEntity(BlockPos pos, BlockState state) {
        super(QHLogistics.FLOW_CONDUIT.get(), pos, state);
    }

    public static int ratePerTick(int segments) {
        return segments <= FREE_LENGTH ? BASE_RATE : Math.max(1, BASE_RATE * FREE_LENGTH / segments);
    }

    static boolean isConduit(BlockState s) {
        return s.getBlock() instanceof FlowConduitBlock;
    }

    /** True when the block behind {@code pos} is a conduit that flows into it. */
    static boolean fedFromBehind(Level level, BlockPos pos, Direction f) {
        BlockPos back = pos.relative(f.getOpposite());
        if (!level.isLoaded(back)) return false;
        BlockState b = level.getBlockState(back);
        return isConduit(b) && b.getValue(FlowConduitBlock.FACING) == f;
    }

    /** Follows the line from {@code start} to the first block that is not a conduit. */
    public static Line trace(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        BlockPos cur = start;
        BlockState s = level.getBlockState(cur);
        if (!isConduit(s)) return new Line(0, start, Direction.NORTH, true);
        int n = 0;
        while (true) {
            Direction f = s.getValue(FlowConduitBlock.FACING);
            seen.add(cur);
            n++;
            BlockPos next = cur.relative(f);
            if (!level.isLoaded(next) || n >= MAX_SEGMENTS) return new Line(n, next, f.getOpposite(), true);
            BlockState ns = level.getBlockState(next);
            if (!isConduit(ns)) return new Line(n, next, f.getOpposite(), false);
            if (seen.contains(next)) return new Line(n, next, f.getOpposite(), true);   // a loop: nowhere to deliver
            cur = next;
            s = ns;
        }
    }

    /** Walks back from {@code pos} to the start of its line (for the right-click readout). */
    public static BlockPos inlet(Level level, BlockPos pos) {
        Set<BlockPos> seen = new HashSet<>();
        BlockPos cur = pos;
        for (int i = 0; i < MAX_SEGMENTS && seen.add(cur); i++) {
            BlockPos prev = null;
            for (Direction d : Direction.values()) {
                BlockPos p = cur.relative(d);
                if (!level.isLoaded(p)) continue;
                BlockState s = level.getBlockState(p);
                if (isConduit(s) && s.getValue(FlowConduitBlock.FACING) == d.getOpposite()) {
                    prev = p;
                    break;
                }
            }
            if (prev == null) return cur;
            cur = prev;
        }
        return cur;
    }

    void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        if (Math.floorMod(sl.getGameTime() + worldPosition.asLong(), PERIOD) != 0) return;
        Direction f = getBlockState().getValue(FlowConduitBlock.FACING);
        if (fedFromBehind(sl, worldPosition, f)) {          // not the inlet: the inlet does the work
            source = sink = null;
            return;
        }
        BlockPos from = worldPosition.relative(f.getOpposite());
        if (!sl.isLoaded(from)) return;
        Line line = trace(sl, worldPosition);
        if (line.closed() || !sl.isLoaded(line.target())) return;

        if (source == null || sourceFacing != f) {
            source = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, sl, from.immutable(), f);
            sourceFacing = f;
        }
        if (sink == null || !sink.pos().equals(line.target()) || sink.context() != line.into())
            sink = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, sl, line.target().immutable(), line.into());
        IFluidHandler in = source.getCapability(), out = sink.getCapability();
        if (in == null || out == null) return;
        FluidUtil.tryFluidTransfer(out, in, ratePerTick(line.segments()) * PERIOD, true);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        source = sink = null;
    }
}
