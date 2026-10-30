package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A floating icosahedron that guards the Platonic Realm. It keeps its distance, spends two seconds "proving" its
 * target - a line of light grows from it - and if the proof completes with the target still in sight, the target takes it.
 */
public class AxiomSentinel extends Monster {
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(AxiomSentinel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(AxiomSentinel.class, EntityDataSerializers.INT);
    public static final int CHARGE_TICKS = 40;

    public AxiomSentinel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FLYING_SPEED, 0.4).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CHARGE, 0);
        b.define(TARGET_ID, -1);
    }

    public int charge() {
        return entityData.get(CHARGE);
    }

    public int targetId() {
        return entityData.get(TARGET_ID);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new ProofGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.SENTINEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.SENTINEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.SENTINEL_DEATH.get();
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    class ProofGoal extends Goal {
        private int cooldown;

        ProofGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void stop() {
            entityData.set(CHARGE, 0);
            entityData.set(TARGET_ID, -1);
        }

        @Override
        public void tick() {
            LivingEntity t = getTarget();
            if (t == null) return;
            getLookControl().setLookAt(t);
            // hold position about 10 blocks away and a little above
            Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
            Vec3 want = t.position().add(away.lengthSqr() < 1e-4 ? new Vec3(10, 0, 0) : away.normalize().scale(10)).add(0, 4, 0);
            getMoveControl().setWantedPosition(want.x, want.y, want.z, 1.0);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            boolean sees = hasLineOfSight(t);
            int c = charge();
            if (!sees) {
                if (c > 0) entityData.set(CHARGE, Math.max(0, c - 2));
                return;
            }
            if (c == 0) {
                entityData.set(TARGET_ID, t.getId());
                level().playSound(null, blockPosition(), QHSounds.SENTINEL_CHARGE.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
            }
            entityData.set(CHARGE, c + 1);
            if (c + 1 >= CHARGE_TICKS) {
                t.hurt(damageSources().indirectMagic(AxiomSentinel.this, AxiomSentinel.this), 10.0F);
                level().playSound(null, blockPosition(), QHSounds.SENTINEL_FIRE.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
                if (level() instanceof ServerLevel sl) {
                    Vec3 a = position().add(0, getBbHeight() / 2, 0), b = t.position().add(0, t.getBbHeight() / 2, 0);
                    for (int i = 0; i <= 20; i++) {
                        Vec3 p = a.lerp(b, i / 20.0);
                        sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                    }
                }
                entityData.set(CHARGE, 0);
                entityData.set(TARGET_ID, -1);
                cooldown = 50 + random.nextInt(30);
            }
        }
    }
}
