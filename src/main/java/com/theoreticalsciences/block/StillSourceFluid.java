package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** A fluid that does not obey gravity: it never flows or spreads, wherever it is placed. */
public class StillSourceFluid extends BaseFlowingFluid.Source {
    public StillSourceFluid(Properties p) {
        super(p);
    }

    @Override
    protected void spread(Level level, BlockPos pos, FluidState state) {}
}
