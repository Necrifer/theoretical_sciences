package com.theoreticalsciences.math;

import com.theoreticalsciences.entity.OwnAI;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 1.2.0: the creatures of the Platonic Realm's new layers. Each thinks for itself (no vanilla mob's mind).
 * <ul>
 *   <li><b>Paradox Shade</b> (the Axioms): "this statement is false". It is true, then false, then true again, every few
 *   seconds. True, it is solid and pale and hunts you; false, it is a dark outline that nothing touches and that
 *   touches nothing. Strike it while it is true.</li>
 *   <li><b>Tessellate</b> (Curved Space): a creature of tiles from the hyperbolic plane, where there is always room for
 *   more tiles. Broken, it falls into two smaller ones (twice), as one tile of the plane holds seven smaller copies.</li>
 *   <li><b>Fourier Wisp</b> (the Continuum): a chain of circles turning at one, three and five times a beat, whose last
 *   point draws a square wave in the air. It drifts, keeps its distance, and flees if struck.</li>
 * </ul>
 */
public final class LayerMobs {
    private LayerMobs() {}

    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    abstract static class Voiced extends Monster {
        Voiced(EntityType<? extends Monster> t, Level l) {
            super(t, l);
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource s) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean removeWhenFarAway(double d) {
            return d > 96 * 96;
        }
    }

    // ================================================================== Paradox Shade
    public static class ParadoxShade extends Voiced {
        private static final EntityDataAccessor<Boolean> TRUE = SynchedEntityData.defineId(ParadoxShade.class, EntityDataSerializers.BOOLEAN);
        public static final int PERIOD = 70;
        private int flip;

        public ParadoxShade(EntityType<? extends Monster> t, Level l) {
            super(t, l);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(TRUE, true);
        }

        public boolean isTrue() {
            return entityData.get(TRUE);
        }

        /** 0..1: how far into its current state (for the renderer's turn from one to the other). */
        public float flipping(float partial) {
            return Math.min(1, (flip + partial) / 10F);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, false);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            flip++;
            if (level().isClientSide) {
                if (!isTrue() && random.nextInt(3) == 0)
                    level().addParticle(ParticleTypes.SMOKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.01, 0);
                return;
            }
            if (tickCount % PERIOD == 0) {
                entityData.set(TRUE, !isTrue());
                flip = 0;
                playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, isTrue() ? 1.4F : 0.6F);
            }
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
            super.onSyncedDataUpdated(key);
            if (TRUE.equals(key)) flip = 0;
        }

        /** False, it cannot be struck: there is nothing there to strike. */
        @Override
        public boolean isInvulnerableTo(DamageSource s) {
            return !isTrue() && !s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(s);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            if (!isTrue()) return false;                          // false: it touches nothing
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(QHEffects.CONTRADICTION, 60, 0, false, true));
            return hit;
        }

        @Override
        public boolean isPushable() {
            return isTrue();
        }

        @Override
        public void addAdditionalSaveData(CompoundTag t) {
            super.addAdditionalSaveData(t);
            t.putBoolean("True", isTrue());
        }

        @Override
        public void readAdditionalSaveData(CompoundTag t) {
            super.readAdditionalSaveData(t);
            if (t.contains("True")) entityData.set(TRUE, t.getBoolean("True"));
        }
    }

    // ================================================================== Tessellate
    public static class Tessellate extends Voiced {
        private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(Tessellate.class, EntityDataSerializers.INT);

        public Tessellate(EntityType<? extends Monster> t, Level l) {
            super(t, l);
            this.xpReward = 5;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(SIZE, 2);
        }

        public int size() {
            return entityData.get(SIZE);
        }

        public void setSize(int s) {
            entityData.set(SIZE, Math.max(0, Math.min(2, s)));
            refreshDimensions();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(6 + 6 * size());
            setHealth(getMaxHealth());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2 + size());
        }

        /** 0.45, 0.7 or 1.0 times its full size. */
        public float scale() {
            return 0.45F + 0.275F * size();
        }

        @Override
        public EntityDimensions getDefaultDimensions(Pose pose) {
            return super.getDefaultDimensions(pose).scale(scale());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
            super.onSyncedDataUpdated(key);
            if (SIZE.equals(key)) refreshDimensions();
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, true);
        }

        /** Broken, it falls into two smaller copies of itself (a big one twice, a middle one once). */
        @Override
        public void remove(RemovalReason why) {
            if (why == RemovalReason.KILLED && !level().isClientSide && size() > 0 && level() instanceof ServerLevel sl) {
                for (int i = 0; i < 2; i++) {
                    Tessellate t = (Tessellate) getType().create(sl);
                    if (t == null) continue;
                    t.setSize(size() - 1);
                    t.moveTo(getX() + (i - 0.5) * 0.6, getY() + 0.2, getZ(), random.nextFloat() * 360F, 0);
                    t.setTarget(getTarget());
                    sl.addFreshEntity(t);
                }
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 12, 0.4, 0.4, 0.4, 0.05);
            }
            super.remove(why);
        }

        /** Only the smallest tiles carry anything worth taking. */
        @Override
        protected boolean shouldDropLoot() {
            return size() == 0 && super.shouldDropLoot();
        }

        @Override
        public void addAdditionalSaveData(CompoundTag t) {
            super.addAdditionalSaveData(t);
            t.putInt("Size", size());
        }

        @Override
        public void readAdditionalSaveData(CompoundTag t) {
            super.readAdditionalSaveData(t);
            if (t.contains("Size")) setSize(t.getInt("Size"));
        }
    }

    // ================================================================== Fourier Wisp
    public static class FourierWisp extends PathfinderMob {
        public FourierWisp(EntityType<? extends PathfinderMob> t, Level l) {
            super(t, l);
            setNoGravity(true);
            this.xpReward = 4;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                    .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5F, 1.0, 1.4));
            goalSelector.addGoal(5, new Drift(this));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10F));
        }

        /** It drifts a little at a time, never far, holding its height above the island it is over. */
        static class Drift extends Goal {
            final FourierWisp w;
            Vec3 to;

            Drift(FourierWisp w) {
                this.w = w;
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                if (w.random.nextInt(40) != 0) return false;
                to = w.position().add((w.random.nextDouble() - 0.5) * 8, (w.random.nextDouble() - 0.5) * 2, (w.random.nextDouble() - 0.5) * 8);
                return true;
            }

            @Override
            public boolean canContinueToUse() {
                return to != null && w.position().distanceToSqr(to) > 0.25;
            }

            @Override
            public void tick() {
                Vec3 d = to.subtract(w.position());
                if (d.lengthSqr() > 1e-4) w.setDeltaMovement(w.getDeltaMovement().scale(0.8).add(d.normalize().scale(0.02)));
            }

            @Override
            public void stop() {
                to = null;
            }
        }

        @Override
        public void aiStep() {
            super.aiStep();
            setDeltaMovement(getDeltaMovement().multiply(0.92, 0.85, 0.92));
            if (level().isClientSide && random.nextInt(3) == 0) {
                // the chain's last point, where the square wave is being drawn
                float t = (tickCount) * 0.08F;
                double x = 0, y = 0;
                for (int k = 0; k < 3; k++) {
                    int m = 2 * k + 1;
                    x += Math.cos(m * t) * 0.35 / m;
                    y += Math.sin(m * t) * 0.35 / m;
                }
                level().addParticle(ParticleTypes.END_ROD, getX() + x, getY() + 0.5 + y, getZ(), 0, 0, 0);
            }
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource s) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean removeWhenFarAway(double d) {
            return d > 96 * 96;
        }
    }
}
