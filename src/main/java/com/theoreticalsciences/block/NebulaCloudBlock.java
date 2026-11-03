package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.10.1: a block of nebular gas or dust. You pass straight through it; its drag slows a fall to a gentle sink, so
 * dropping through a cloud never hurts. Neighbouring clouds of one kind hide the faces between them, as glass does.
 */
public class NebulaCloudBlock extends HalfTransparentBlock {
    public NebulaCloudBlock(Properties p) {
        super(p);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        Vec3 v = e.getDeltaMovement();
        // gas drag: a fall is capped at a slow sink (about 0.15 blocks a tick); sideways motion is barely slowed
        if (v.y < -0.15) e.setDeltaMovement(v.x * 0.95, -0.15, v.z * 0.95);
        e.resetFallDistance();
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState other, Direction dir) {
        return other.is(this) || super.skipRendering(state, other, dir);
    }
}
