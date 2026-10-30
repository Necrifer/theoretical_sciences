package com.theoreticalsciences.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.List;

/**
 * The Ocean of Primordial Law. Laws of nature bubble up out of it and pop out of existence again;
 * anything swimming in it is subject to whichever law happens to be in force at that moment.
 */
public class LawSeaBlock extends QHLiquidBlock {
    private static final List<Holder<MobEffect>> LAWS = List.of(MobEffects.LEVITATION, MobEffects.SLOW_FALLING, MobEffects.MOVEMENT_SPEED,
            MobEffects.MOVEMENT_SLOWDOWN, MobEffects.JUMP, MobEffects.NIGHT_VISION, MobEffects.GLOWING, MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE, MobEffects.INVISIBILITY, MobEffects.DIG_SPEED, MobEffects.CONFUSION);

    public LawSeaBlock(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity le)) return;
        // the ocean is breathable: it decides what "breathing" means
        if (level.getGameTime() % 20 == 0) le.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
        if ((level.getGameTime() + entity.getId()) % 60 == 0) {
            Holder<MobEffect> law = LAWS.get(level.random.nextInt(LAWS.size()));
            le.addEffect(new MobEffectInstance(law, 100, level.random.nextInt(2), true, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!level.getBlockState(pos.above()).isAir() || random.nextInt(5) != 0) return;
        double x = pos.getX() + random.nextDouble(), y = pos.getY() + 0.95, z = pos.getZ() + random.nextDouble();
        // newly-born laws rise as glyphs; others pop as they collapse
        level.addParticle(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), x, y + 0.4, z, (random.nextDouble() - 0.5) * 0.3, 0.6 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.3);
        if (random.nextInt(4) == 0) level.addParticle(com.theoreticalsciences.registry.QHParticles.QUANTUM_FOAM.get(), x, y, z, 0, 0.04, 0);
        if (random.nextInt(30) == 0) level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), x, y, z, 0, 0.12, 0);
    }
}
