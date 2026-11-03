package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A creature unravelled by the reality destabilisation of a containment failure. Tougher than a zombie, never burns in daylight,
 * and its very presence destabilises reality around it; its bite makes it worse.
 */
public class DestabilisedHusk extends Zombie {
    public DestabilisedHusk(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    // round 37 (1.1): its own mind, not a zombie's - see OwnAI
    @Override
    protected void registerGoals() {
        OwnAI.melee(this, 1.0, false);
    }

    @Override
    protected void addBehaviourGoals() {}

    /** A zombie turns the villagers it kills into zombie villagers; this does not. */
    @Override
    public boolean killedEntity(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity victim) {
        return true;
    }

    /** No babies, chicken jockeys, armour, loot pickup, door breaking or reinforcement bonus. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
                                                                     net.minecraft.world.entity.MobSpawnType type, @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data) {
        setCanPickUpLoot(false);
        return data;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {}

    @Override
    public boolean canBreakDoors() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 34.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.theoreticalsciences.registry.QHSounds.HUSK_AMBIENT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource src) {
        return com.theoreticalsciences.registry.QHSounds.HUSK_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.theoreticalsciences.registry.QHSounds.HUSK_DEATH.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getStepSound() {
        return com.theoreticalsciences.registry.QHSounds.HUSK_STEP.get();
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
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(QHEffects.REALITY_DESTABILISATION, 300, 1));
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(4) == 0)
                level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
        } else if (tickCount % 40 == 0) {
            // destabilisation aura
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(3),
                    e -> !(e instanceof DestabilisedHusk) && e.isAlive()))
                e.addEffect(new MobEffectInstance(QHEffects.REALITY_DESTABILISATION, 100, 0));
        }
    }
}
