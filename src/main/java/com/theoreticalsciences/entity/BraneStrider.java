package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A tall, thin walker made of stretched membrane, striding across the Bulk on four stilt legs. It does not quite
 * live on our brane: when hurt it may slip sideways into the next one and step back out somewhere nearby, and its
 * blow leaves you seeing double.
 */
public class BraneStrider extends Monster {
    public BraneStrider(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
        return hit;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (r && isAlive() && random.nextInt(10) < 3 && level() instanceof ServerLevel sl) phase(sl);
        return r;
    }

    /** Slips into a neighbouring brane and steps back out a few blocks away. */
    private void phase(ServerLevel sl) {
        for (int tries = 0; tries < 8; tries++) {
            double x = getX() + (random.nextDouble() - 0.5) * 14, z = getZ() + (random.nextDouble() - 0.5) * 14;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (y <= sl.getMinBuildHeight() + 1 || Math.abs(y - getY()) > 8) continue;
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX(), getY() + 1.5, getZ(), 40, 0.4, 1.2, 0.4, 0.15);
            playSound(QHSounds.STRIDER_PHASE.get(), 1.2F, 1.0F);
            teleportTo(x, y, z);
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), x, y + 1.5, z, 40, 0.4, 1.2, 0.4, 0.15);
            return;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.STRIDER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.STRIDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.STRIDER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(QHSounds.STRIDER_STEP.get(), 0.6F, 1.0F);
    }
}
