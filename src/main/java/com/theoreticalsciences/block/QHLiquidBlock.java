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
}
