package com.theoreticalsciences.block;

import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The edges of the Quantum Foam: Schrödinger's Limit below, Relativity Breakdown above. A liquid you cannot pass -
 * it is solid to anything moving through it - and that never runs out: fill a cell from it and it is still there.
 */
public class BoundaryFluidBlock extends QHLiquidBlock {
    public BoundaryFluidBlock(FlowingFluid fluid, Properties p) {
        super(fluid, p);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        // the boundary regenerates instantly: you get a cellful, the block stays (never for a dispenser - that would be endless)
        if (player == null) return ItemStack.EMPTY;
        // round 37 (1.1): only the Quantum Foam holds a boundary up. Anywhere else a cell takes it for good.
        if (!home(level)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        return new ItemStack(fluid.getBucket());
    }

    /** Is this the Quantum Foam, where the boundaries belong (and regenerate)? */
    static boolean home(LevelAccessor level) {
        return level instanceof Level l && l.dimension() == com.theoreticalsciences.world.QHDimensions.FOAM;
    }

    // ---- round 37 (1.1): a boundary that ended up outside the Quantum Foam (poured from a cell before 1.1) unravels on its own:
    // on its next random tick, or at once when anything next to it changes.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!home(level)) unravel(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && !home(level)) {
            unravel(level, pos);
            return;
        }
        super.neighborChanged(state, level, pos, block, from, moving);
    }

    private static void unravel(Level level, BlockPos pos) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (level instanceof ServerLevel sl)
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.QUANTUM_FOAM.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.02);
    }

    @SuppressWarnings("unused")
    private static boolean unused(LivingEntity e) {
        return false;
    }
}
