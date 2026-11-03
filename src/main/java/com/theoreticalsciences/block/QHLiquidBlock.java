package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import javax.annotation.Nullable;

/** Our fluids' blocks. Only a player with a Containment Cell can take them: a dispenser's bucket (no player) gets nothing. */
public class QHLiquidBlock extends LiquidBlock {
    public QHLiquidBlock(FlowingFluid fluid, Properties p) {
        super(fluid, p);
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        if (player == null) return ItemStack.EMPTY;
        return super.pickupBlock(player, level, pos, state);
    }

    /** Round 37: the reason a bucket refuses it is what it does to you - heat burns, cold freezes, the exotic twists. */
    @Override
    protected void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        super.entityInside(state, level, pos, entity);
        FluidHazard.entityInside(state, level, pos, entity);
    }
}
