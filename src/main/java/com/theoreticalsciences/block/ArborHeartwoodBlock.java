package com.theoreticalsciences.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Wood of the World Tree. It is made of something that does not obey ordinary physics: every so often a
 * block slips into another "phase", and each phase obeys a different rule.
 * <ul>
 *   <li>0 - dormant: behaves like a very hard wood.</li>
 *   <li>1 - inverted: gravity points the other way for anything standing on it.</li>
 *   <li>2 - luminous: radiates light and hurries whatever touches it.</li>
 *   <li>3 - unreal: gives off no light at all, and things on it fall as if through syrup.</li>
 * </ul>
 */
public class ArborHeartwoodBlock extends Block {
    public static final MapCodec<ArborHeartwoodBlock> CODEC = simpleCodec(ArborHeartwoodBlock::new);
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 3);

    public ArborHeartwoodBlock(Properties props) {
        super(props.randomTicks().lightLevel(s -> switch (s.getValue(PHASE)) {
            case 1 -> 7;
            case 2 -> 15;
            case 3 -> 0;
            default -> 4;
        }));
        registerDefaultState(stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Rare on purpose: each phase change is a light update.
        if (random.nextInt(48) != 0) return;
        int next = random.nextInt(10) < 6 ? 0 : 1 + random.nextInt(3);
        if (next != state.getValue(PHASE)) level.setBlock(pos, state.setValue(PHASE, next), 3);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity le && level.getGameTime() % 10 == 0) {
            switch (state.getValue(PHASE)) {
                case 1 -> le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, true, false));
                case 2 -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, true, false));
                case 3 -> le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
                default -> {}
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int ph = state.getValue(PHASE);
        if (ph == 0 || random.nextInt(6) != 0) return;
        double x = pos.getX() + random.nextDouble(), y = pos.getY() + 1.05, z = pos.getZ() + random.nextDouble();
        if (ph == 1) level.addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), x, y, z, 0, 0.05, 0);
        else if (ph == 2) level.addParticle(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), x, y, z, 0, 0.02, 0);
        else level.addParticle(com.theoreticalsciences.registry.QHParticles.VOID_INK.get(), x, y, z, 0, -0.02, 0);
    }
}
