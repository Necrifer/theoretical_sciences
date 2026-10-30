package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A translucent copy of someone from a timeline that should never have happened. When badly hurt it can
 * rewind itself to where it stood a few seconds earlier, healed - so it must be finished off quickly.
 */
public class ParadoxEcho extends Zombie {
    private final Vec3[] history = new Vec3[60];
    private int cursor;
    private int rewinds = 2;

    public ParadoxEcho(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.theoreticalsciences.registry.QHSounds.ECHO_AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource src) {
        return com.theoreticalsciences.registry.QHSounds.ECHO_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.theoreticalsciences.registry.QHSounds.ECHO_DEATH.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getStepSound() {
        return com.theoreticalsciences.registry.QHSounds.ECHO_STEP.get();
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            history[cursor] = position();
            cursor = (cursor + 1) % history.length;
        } else if (random.nextInt(3) == 0) {
            level().addParticle(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.1, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && isAlive() && rewinds > 0 && getHealth() < getMaxHealth() * 0.35F && level() instanceof ServerLevel sl) {
            Vec3 past = history[cursor];      // the oldest stored position: ~3 seconds ago
            if (past != null) {
                rewinds--;
                sl.playSound(null, getX(), getY(), getZ(), com.theoreticalsciences.registry.QHSounds.ECHO_REWIND.get(), getSoundSource(), 1.0F, 1.0F);
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX(), getY() + 1, getZ(), 40, 0.3, 0.6, 0.3, 0.1);
                teleportTo(past.x, past.y, past.z);
                setHealth(getMaxHealth() * 0.8F);
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), getX(), getY() + 1, getZ(), 60, 0.4, 0.8, 0.4, 0.5);
            }
        }
        return r;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(QHEffects.TEMPORAL_DISSONANCE, 200, 0));
        return hit;
    }
}
