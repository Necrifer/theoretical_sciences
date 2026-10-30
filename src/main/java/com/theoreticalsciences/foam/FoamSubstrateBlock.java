package com.theoreticalsciences.foam;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The membrane between bubbles of nothing. In the Quantum Foam it never stops fizzing: now and then a bubble of
 * false vacuum swells on its surface, or a virtual particle condenses into an ore that will not stay for long.
 */
public class FoamSubstrateBlock extends Block {
    public FoamSubstrateBlock(Properties p) {
        super(p.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (level.dimension() != QHDimensions.FOAM) return;
        Direction d = Direction.getRandom(r);
        BlockPos at = pos.relative(d);
        if (!level.getBlockState(at).isAir()) return;
        int roll = r.nextInt(200);
        if (roll < 6) level.setBlock(at, QHBlocks.get("foam_bubble").defaultBlockState(), 3);
        else if (roll == 6) {
            // ore grows inside the membrane, replacing the substrate itself, so it sits flush in the wall
            level.setBlock(pos, FoamField.ore(FoamField.dominant(level, pos)).defaultBlockState(), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(24) == 0 && level.getBlockState(pos.above()).isAir())
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + r.nextDouble(), pos.getY() + 1.05, pos.getZ() + r.nextDouble(), 0, 0.01, 0);
    }
}
