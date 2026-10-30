package com.theoreticalsciences.block;

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
        return new ItemStack(fluid.getBucket());
    }

    @SuppressWarnings("unused")
    private static boolean unused(LivingEntity e) {
        return false;
    }
}
