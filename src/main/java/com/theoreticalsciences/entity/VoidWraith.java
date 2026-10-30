package com.theoreticalsciences.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Something that crawled out of a spacetime rupture: a tiny living black hole (or its time-reversed twin, a white hole). It flies through walls, and every few seconds it blinks a
 * short distance through space. Its touch drains light from the world (darkness + wither).
 */
public class VoidWraith extends Vex {
    private int blinkCooldown = 60;

    public VoidWraith(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vex.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
        }
        return hit;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.theoreticalsciences.registry.QHSounds.WRAITH_AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource src) {
        return com.theoreticalsciences.registry.QHSounds.WRAITH_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.theoreticalsciences.registry.QHSounds.WRAITH_DEATH.get();
    }

    /** The vex's charge sound is played through here: swap it for the wraith's own. */
    @Override
    public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (sound == com.theoreticalsciences.registry.QHSounds.CHARGE.get()) sound = com.theoreticalsciences.registry.QHSounds.WRAITH_CHARGE.get();
        super.playSound(sound, volume, pitch);
    }

    /** As it dies it collapses into itself; at the very end the collapse rebounds as a small creeper-sized blast. */
    @Override
    protected void tickDeath() {
        if (!level().isClientSide && deathTime == 18) {
            boolean grief = net.neoforged.neoforge.event.EventHooks.canEntityGrief(level(), this)
                    && com.theoreticalsciences.QHConfig.FAILURE_DESTROYS_BLOCKS.get();
            level().explode(this, getX(), getY() + getBbHeight() / 2, getZ(), 1.8F, false,
                    grief ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        }
        if (level().isClientSide && deathTime < 18)   // matter being pulled into the collapsing core
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 1.5;
                level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX() + Math.cos(a) * r, getY() + 0.5, getZ() + Math.sin(a) * r,
                        -Math.cos(a) * r, 0, -Math.sin(a) * r);
            }
        super.tickDeath();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
            return;
        }
        if (--blinkCooldown <= 0 && getTarget() != null) {
            blinkCooldown = 60 + random.nextInt(60);
            Vec3 to = getTarget().position().add((random.nextDouble() - 0.5) * 6, 1 + random.nextDouble() * 2, (random.nextDouble() - 0.5) * 6);
            ((net.minecraft.server.level.ServerLevel) level()).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            teleportTo(to.x, to.y, to.z);
            level().playSound(null, to.x, to.y, to.z, com.theoreticalsciences.registry.QHSounds.WRAITH_BLINK.get(), getSoundSource(), 1.0F, 1.0F);
        }
    }
}
