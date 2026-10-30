package com.theoreticalsciences.foam;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Matter borrowed from the vacuum: an ore that condensed out of the foam and will soon give itself back.
 * Each random tick there is a good chance it simply stops existing (and turns back into substrate), so mine it while it's there.
 */
public class EphemeralOreBlock extends DropExperienceBlock {
    public EphemeralOreBlock(Properties p) {
        super(UniformInt.of(3, 7), p.randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (r.nextInt(4) != 0) return;
        level.setBlock(pos, com.theoreticalsciences.registry.QHBlocks.get("foam_substrate").defaultBlockState(), 3);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, pos, QHSounds.FOAM_POP.get(), SoundSource.BLOCKS, 0.8F, 0.5F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        // it flickers: the particles show it half-existing
        if (r.nextInt(3) == 0)
            level.addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0.02, 0);
    }
}
