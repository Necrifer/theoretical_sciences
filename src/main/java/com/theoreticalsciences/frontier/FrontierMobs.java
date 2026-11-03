package com.theoreticalsciences.frontier;

import com.theoreticalsciences.entity.OwnAI;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.science.RadiationService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.10.0: the Accretion Frontier's creatures, and the Black Swan.
 * <ul>
 *   <li><b>Nebuloid</b> (Molecular Cloud Nebula): a drifting knot of gas and dust. It drifts to whoever it senses, and
 *   inside it you cannot see (Blindness).</li>
 *   <li><b>Pulsar Wisp</b> (Pulsar Field): a spinning neutron star in miniature. Its beam sweeps round on an exact beat -
 *   once every 1.5 seconds - and whoever it crosses takes a burst of radiation. Dodge between pulses.</li>
 *   <li><b>Tidal Stretcher</b> (Accretion Rim): whatever it grabs, it stretches (Tidal Strain) and drags in.</li>
 *   <li><b>Lens Phantom</b> (Lensing Void): never where it appears - its light reaches you bent round the void's mass, so
 *   its image is drawn a block or two away from its body.</li>
 *   <li><b>Hawking Imp</b> (anywhere in the Frontier, rare): a tiny black hole. It loses mass to Hawking radiation, and
 *   the smaller it gets the hotter and faster it burns (T ∝ 1/M) until it pops.</li>
 *   <li><b>Boltzmann Brain</b> (very rare): a mind fluctuated out of nothing. It mutters, and is gone within half a minute
 *   unless caught.</li>
 *   <li><b>Black Swan</b> (anywhere, extremely rare): improbable by design - see {@link FrontierHazards}.</li>
 * </ul>
 */
public final class FrontierMobs {
    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    static void irradiate(Entity target, double mSv) {
        if (target instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator())
            RadiationService.setDose(p, RadiationService.dose(p) + mSv * com.theoreticalsciences.interior.InteriorGear.radiationFactor(p));
    }

    /** A floater: no gravity, drifts toward its target with a little drag. */
    abstract static class Floater extends InteriorMobs.Base {
        Floater(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setNoGravity(true);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16F));
            OwnAI.hunter(this);
        }

        void drift(double accel, double drag, double hover) {
            LivingEntity t = getTarget();
            Vec3 v = getDeltaMovement().scale(drag);
            if (t != null && t.isAlive() && distanceToSqr(t) < 32 * 32) {
                Vec3 d = t.position().add(0, t.getBbHeight() * 0.5 + hover, 0).subtract(position());
                if (d.lengthSqr() > 0.01) v = v.add(d.normalize().scale(accel));
                getLookControl().setLookAt(t);
            } else if (random.nextInt(40) == 0) {
                v = v.add((random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.05, (random.nextDouble() - 0.5) * 0.1);
            }
            setDeltaMovement(v);
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }
    }

    // ================================================================== Nebuloid
    public static class Nebuloid extends Floater {
        int touch;

        public Nebuloid(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 6;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 2.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.15).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(2) == 0)
                    level().addParticle(ParticleTypes.WHITE_ASH, getRandomX(1.2), getRandomY(), getRandomZ(1.2), 0, 0, 0);
                return;
            }
            drift(0.012, 0.9, 0);
            // whoever is inside the cloud is blinded by its dust, and choked a little
            if (--touch <= 0)
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.6))) {
                    if (p.isCreative() || p.isSpectator()) continue;
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
                    doHurtTarget(p);
                    touch = 30;
                }
        }
    }

    // ================================================================== Pulsar Wisp
    public static class PulsarWisp extends Floater {
        /** The beat: one pulse every PERIOD ticks, on the world clock, so every wisp in the field pulses together. */
        public static final int PERIOD = 30;

        public PulsarWisp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ARMOR, 6.0);
        }

        /** The beam's direction now: it sweeps once round per beat, in the horizontal plane. */
        public static double beamAngle(long gameTime, float partial) {
            return ((gameTime % PERIOD) + partial) / PERIOD * Math.PI * 2;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            drift(0.01, 0.85, 2.5);
            long t = level().getGameTime();
            if (t % PERIOD == 0) {
                ServerLevel sl = (ServerLevel) level();
                sl.playSound(null, blockPosition(), QHSounds.PULSAR_BEAT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.4, getZ(), 20, 0.3, 0.3, 0.3, 0.4);
                // the pulse: whoever stands within 10 blocks and in the open, with a line of sight to it, is hit
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(10))) {
                    if (p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) continue;
                    double r = Math.max(1, distanceTo(p));
                    p.hurt(damageSources().magic(), (float) Math.max(1, 6 / r * 2));
                    irradiate(p, 40 / (r * r));                          // the dose falls as 1/r²
                }
            }
        }
    }

    // ================================================================== Tidal Stretcher
    public static class TidalStretcher extends InteriorMobs.Base {
        public TidalStretcher(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 10;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, false);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            // its pull: anyone close is drawn in, harder the closer they are (a tidal force grows as 1/r³)
            LivingEntity t = getTarget();
            if (t != null && t.isAlive() && tickCount % 5 == 0) {
                double r = distanceTo(t);
                if (r < 6 && r > 1.2) {
                    Vec3 d = position().subtract(t.position()).normalize().scale(Math.min(0.25, 2.0 / (r * r * r)));
                    t.setDeltaMovement(t.getDeltaMovement().add(d.x, 0, d.z));
                    t.hurtMarked = true;
                }
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(QHEffects.SPAGHETTIFICATION, 120, 0, false, true));
            return hit;
        }
    }

    // ================================================================== Lens Phantom
    public static class LensPhantom extends InteriorMobs.Base {
        public LensPhantom(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 10;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, true);
        }

        /**
         * Where its image appears, relative to its body: light from it passes the void's unseen mass on one side and is
         * bent toward it, by an angle that grows as the line of sight passes closer (Einstein: α = 4GM/(c²b)). Here a
         * displacement of up to two blocks that slowly turns, seeded by the entity.
         */
        public static Vec3 imageOffset(Entity e, float partial) {
            double t = (e.tickCount + partial) * 0.02 + e.getId() * 1.3;
            double r = 1.4 + 0.6 * Math.sin(t * 0.7);
            return new Vec3(Math.cos(t) * r, 0, Math.sin(t) * r);
        }
    }

    // ================================================================== Hawking Imp
    public static class HawkingImp extends Floater {
        static final EntityDataAccessor<Float> MASS = SynchedEntityData.defineId(HawkingImp.class, EntityDataSerializers.FLOAT);

        public HawkingImp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 12;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(MASS, 1.0F);
        }

        /** Mass, from 1 (fresh) down to 0 (gone). */
        public float mass() {
            return entityData.get(MASS);
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putFloat("Mass", mass());
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("Mass")) entityData.set(MASS, tag.getFloat("Mass"));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            float m = mass();
            if (level().isClientSide) {
                if (random.nextFloat() < 0.2F / Math.max(0.1F, m))
                    level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, random.nextGaussian() * 0.1);
                return;
            }
            // evaporation: dM/dt ∝ −1/M², so the lifetime goes as M³ and the end comes fast
            m -= 0.0004F / Math.max(0.12F, m * m);
            entityData.set(MASS, Math.max(0, m));
            drift(0.006 + 0.02 * (1 - m), 0.88, 0);                   // hotter, faster
            if (tickCount % 20 == 0)                                   // its glow burns anyone close, hotter as it shrinks
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(2.5))) {
                    if (p.isCreative() || p.isSpectator()) continue;
                    p.hurt(damageSources().onFire(), 1F + 3F * (1 - m));
                    p.igniteForSeconds(2);
                }
            if (m <= 0) pop();
        }

        /** The last instant: everything left radiated at once. Hurts what is near; changes no block. */
        void pop() {
            ServerLevel sl = (ServerLevel) level();
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.3, getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 60, 0.2, 0.2, 0.2, 0.5);
            sl.playSound(null, blockPosition(), QHSounds.IMP_BURST.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4), x -> x != this)) {
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                e.hurt(damageSources().explosion(this, this), (float) (10 / Math.max(1, distanceTo(e))));
            }
            // the last of its radiation can be caught
            spawnAtLocation(com.theoreticalsciences.registry.QHItems.get("hawking_quanta"));
            discard();
        }
    }

    // ================================================================== Boltzmann Brain
    public static class BoltzmannBrain extends Floater {
        static final String[] THOUGHTS = {"thought_1", "thought_2", "thought_3", "thought_4", "thought_5", "thought_6"};
        int life = 600;

        public BoltzmannBrain(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 20;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16F));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            // it keeps a little distance, as if unsure that you are real
            Player near = level().getNearestPlayer(this, 12);
            Vec3 v = getDeltaMovement().scale(0.85);
            if (near != null && distanceTo(near) < 4) v = v.add(position().subtract(near.position()).normalize().scale(0.02));
            else if (random.nextInt(30) == 0) v = v.add((random.nextDouble() - 0.5) * 0.08, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.08);
            setDeltaMovement(v);
            if (near instanceof ServerPlayer sp && random.nextInt(140) == 0)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.boltzmann." + THOUGHTS[random.nextInt(THOUGHTS.length)])
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
            // a fluctuation does not last
            if (--life <= 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.4, getZ(), 40, 0.3, 0.3, 0.3, 0.05);
                discard();
            }
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return true;
        }
    }

    // ================================================================== Black Swan
    public static class BlackSwan extends PathfinderMob {
        public BlackSwan(EntityType<? extends PathfinderMob> type, Level level) {
            super(type, level);
            this.xpReward = 30;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(1, new PanicGoal(this, 1.6));
            goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 10F, 1.2, 1.6));
            goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
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
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            // it glides rather than falls
            if (!onGround() && getDeltaMovement().y < -0.08) setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return dist > 128 * 128;
        }
    }

    private FrontierMobs() {}
}
