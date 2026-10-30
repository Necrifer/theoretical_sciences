package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * A flying grey-goo swarm of runaway nanites. It hunts players, villagers and animals alike and infects whatever it
 * touches (an infected creature that dies bursts into a new swarm). When destroyed it splits into smaller swarms,
 * again and again, until only single-unit swarms are left - those die normally.
 */
public class NaniteSwarmEntity extends Monster {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(NaniteSwarmEntity.class, EntityDataSerializers.INT);

    public NaniteSwarmEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SIZE, 1);
    }

    public int getSize() {
        return entityData.get(SIZE);
    }

    /** Bigger swarms are tougher, hit harder and are slower. */
    public void setSize(int size, boolean resetHealth) {
        size = Mth.clamp(size, 1, 8);
        entityData.set(SIZE, size);
        reapplyPosition();
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(size * size * 2);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1 + size);
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(0.55 - size * 0.04);
        if (resetHealth) setHealth(getMaxHealth());
        xpReward = size * 2;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(getSize());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getSize() - 1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        setSize(tag.getInt("Size") + 1, false);
        super.readAdditionalSaveData(tag);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new SwarmChargeGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, true));
        // anything else alive is raw material: swarms go after other creatures too, and replicate from what they kill
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> !(e instanceof NaniteSwarmEntity) && e.isAlive() && !e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                        && !(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) && !(e instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                        && !(e instanceof com.theoreticalsciences.boss.AlphaOmegaEntity)));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean wasInfected = target instanceof LivingEntity l0 && l0.hasEffect(QHEffects.NANITE_INFECTION);
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            if (le.isAlive()) le.addEffect(new MobEffectInstance(QHEffects.NANITE_INFECTION, 400, getSize() > 2 ? 1 : 0));
            // a creature the swarm kills outright is disassembled into a new swarm (an infected one already bursts into one on death)
            else if (!wasInfected && com.theoreticalsciences.QHConfig.FAILURE_HAZARDS.get() && level() instanceof ServerLevel sl) {
                NaniteSwarmEntity child = spawn(sl, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), le.getBbWidth() > 0.9F ? 2 : 1);
                if (child != null) sl.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 40,
                        le.getBbWidth() * 0.4, le.getBbHeight() * 0.4, le.getBbWidth() * 0.4, 0.15);
            }
        }
        return hit;
    }

    /** How many more swarms this dimension may hold before the hard cap (config naniteSwarmCap). */
    public static int room(ServerLevel level) {
        int cap = com.theoreticalsciences.QHConfig.NANITE_CAP.get();
        int n = 0;
        for (Entity e : level.getAllEntities()) if (e instanceof NaniteSwarmEntity && e.isAlive() && ++n >= cap) return 0;
        return cap - n;
    }

    /** Spawns a swarm if the cap allows; returns it (or null). */
    @javax.annotation.Nullable
    public static NaniteSwarmEntity spawn(ServerLevel level, double x, double y, double z, int size) {
        if (room(level) <= 0) return null;
        NaniteSwarmEntity sw = com.theoreticalsciences.registry.QHEntities.NANITE_SWARM.get().create(level);
        if (sw == null) return null;
        sw.setSize(size, true);
        sw.moveTo(x, y, z, level.random.nextFloat() * 360F, 0);
        level.addFreshEntity(sw);
        return sw;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return com.theoreticalsciences.registry.QHSounds.SWARM_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 50;        // a swarm is never quiet for long
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource src) {
        return com.theoreticalsciences.registry.QHSounds.SWARM_HURT.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return com.theoreticalsciences.registry.QHSounds.SWARM_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F + 0.15F * getSize();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (1.3F - 0.1F * getSize());     // smaller swarms sound higher
    }

    // ---- flying: no fall damage, no gravity ----
    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean isFlapping() {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(4) == 0)
            level().addParticle(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
    }

    /** A big swarm does not die: it breaks into 2-3 smaller swarms. Only the smallest units play the death animation. */
    @Override
    protected void tickDeath() {
        int size = getSize();
        if (size > 1) {
            if (level() instanceof ServerLevel sl && !isRemoved()) {
                int n = Math.min(2 + random.nextInt(2), room(sl));    // at the cap, a broken swarm simply falls apart
                for (int i = 0; i < n; i++) {
                    NaniteSwarmEntity child = (NaniteSwarmEntity) getType().create(sl);
                    if (child == null) continue;
                    child.setSize(size - 1, true);
                    if (isPersistenceRequired()) child.setPersistenceRequired();
                    child.setTarget(getTarget());
                    child.moveTo(getX() + (random.nextDouble() - 0.5) * size * 0.4, getY() + getBbHeight() * 0.3,
                            getZ() + (random.nextDouble() - 0.5) * size * 0.4, random.nextFloat() * 360F, 0);
                    child.setDeltaMovement((random.nextDouble() - 0.5) * 0.6, 0.15 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.6);
                    sl.addFreshEntity(child);
                }
                sl.playSound(null, getX(), getY(), getZ(), com.theoreticalsciences.registry.QHSounds.SWARM_SPLIT.get(), getSoundSource(), 1.0F, 1.2F - size * 0.1F);
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), getX(), getY() + getBbHeight() / 2, getZ(), 25 * size, 0.3 * size, 0.3 * size, 0.3 * size, 0.2);
                remove(RemovalReason.KILLED);
            }
            return;
        }
        super.tickDeath();
    }

    /** Flies straight at its target and bites when close enough. */
    class SwarmChargeGoal extends Goal {
        private int cooldown;

        SwarmChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t, 30F, 30F);
            getMoveControl().setWantedPosition(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1.2);
            if (cooldown > 0) cooldown--;
            double reach = getBbWidth() * 0.5 + t.getBbWidth() * 0.5 + 0.7;
            if (cooldown <= 0 && distanceToSqr(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ()) < reach * reach + 0.5) {
                doHurtTarget(t);
                cooldown = 20;
            }
        }
    }
}
