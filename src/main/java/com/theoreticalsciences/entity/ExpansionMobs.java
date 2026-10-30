package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The creatures of the Quantum Foam, the Transfinite Hierarchy and the black and white holes.
 * Passive ones drift or wander and flee when hurt; hostile ones each carry their own effect.
 */
public final class ExpansionMobs {
    static String id(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(id(e), kind);
    }

    static void inflict(Entity target, Holder<MobEffect> effect, int ticks, int amp) {
        if (target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(effect, ticks, amp));
    }

    // ================================================================== passive floaters: Virtual Pair, Hypercube Drifter, Photon Moth
    /** Harmless things that drift through the air, and scatter when struck. */
    public static class Floater extends PathfinderMob {
        public Floater(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.FLYING_SPEED, 0.4)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanFloat(true);
            return nav;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new PanicGoal(this, 1.6));
            goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 0.8));
            goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8F));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}

        @Override
        public boolean removeWhenFarAway(double dist) {
            return dist > 128 * 128;
        }
    }

    // ================================================================== passive walker: Hilbert Bellhop
    /** The bellhop of Hilbert's Hotel. Always full, always room for one more: it just asks every guest to move up one. */
    public static class Bellhop extends PathfinderMob {
        public Bellhop(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new PanicGoal(this, 1.4));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }
    }

    // ================================================================== Planck Mite (hostile, Decoherence)
    /** A spider-sized knot of foam that bites. Its bite decoheres you: you keep reappearing a little way off. */
    public static class PlanckMite extends Spider {
        public PlanckMite(EntityType<? extends Spider> type, Level level) {
            super(type, level);
            this.xpReward = 6;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.36);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) inflict(target, BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath().equals("positronium_skitterer")
                    ? net.minecraft.world.effect.MobEffects.WITHER : QHEffects.DECOHERENCE, 120, 0);
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }
    }

    // ================================================================== Ordinal Hound (hostile, Infinite Regress)
    /** It is always one step after the last one. Each bite deepens Infinite Regress: everything hurts you more, without limit. */
    public static class OrdinalHound extends Monster {
        public OrdinalHound(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 12;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new FloatGoal(this));
            goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
            goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12F));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity le) {
                MobEffectInstance cur = le.getEffect(QHEffects.INFINITE_REGRESS);
                int amp = cur == null ? 0 : Math.min(9, cur.getAmplifier() + 1);
                le.addEffect(new MobEffectInstance(QHEffects.INFINITE_REGRESS, 300, amp));
            }
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }
    }

    // ================================================================== flying strikers: Vacuum Drifter (neutral), Tidal Horror (hostile)
    /**
     * A flying thing that hangs back, then strikes. The Vacuum Drifter only fights back (Vacuum Decay); the Tidal Horror
     * hunts anything it sees inside the black hole (Spaghettification).
     */
    public static class Striker extends Monster {
        private final boolean neutral;
        private final Holder<MobEffect> effect;

        public Striker(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.moveControl = new FlyingMoveControl(this, 20, true);
            this.setNoGravity(true);
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            this.neutral = id.equals("vacuum_drifter");
            this.effect = neutral ? QHEffects.VACUUM_DECAY : id.equals("annihilator") ? net.minecraft.world.effect.MobEffects.WITHER : QHEffects.SPAGHETTIFICATION;
            this.xpReward = neutral ? 8 : 18;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                    .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanFloat(true);
            return nav;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(2, new StrikeGoal());
            goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            // hostile ones look for prey themselves; neutral ones wait to be provoked
            if (!neutral && getTarget() == null && tickCount % 20 == 0) {
                Player p = level().getNearestPlayer(this, 32);
                if (p != null && !p.isCreative() && !p.isSpectator()) setTarget(p);
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) inflict(target, effect, 160, 0);
            return hit;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return snd(this, "ambient");
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource src) {
            return snd(this, "hurt");
        }

        @Override
        protected SoundEvent getDeathSound() {
            return snd(this, "death");
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        protected void checkFallDamage(double y, boolean g, BlockState s, BlockPos p) {}

        class StrikeGoal extends Goal {
            private int cooldown;
            private double angle;

            StrikeGoal() {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                LivingEntity t = getTarget();
                return t != null && t.isAlive();
            }

            @Override
            public void tick() {
                LivingEntity t = getTarget();
                if (t == null) return;
                getLookControl().setLookAt(t);
                if (cooldown > 0) {
                    cooldown--;
                    angle += 0.08;
                    Vec3 c = t.position().add(Math.cos(angle) * 6, 3 + Math.sin(angle * 3), Math.sin(angle) * 6);
                    getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.0);
                    return;
                }
                getMoveControl().setWantedPosition(t.getX(), t.getY() + 0.6, t.getZ(), 1.8);
                if (distanceToSqr(t) < 3.5) {
                    doHurtTarget(t);
                    cooldown = 40 + random.nextInt(40);
                }
            }
        }
    }

    private ExpansionMobs() {}
}
