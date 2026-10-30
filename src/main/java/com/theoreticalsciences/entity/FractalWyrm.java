package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
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
 * A serpent that is made of smaller copies of itself. It swims through the air of the Platonic Realm in long
 * curves and dives at whatever it hunts. Its body (drawn client-side) is a chain of self-similar segments that
 * follows the path its head has taken.
 */
public class FractalWyrm extends Monster {
    /** Client-side trail of head positions, newest first. */
    public final java.util.ArrayDeque<Vec3> trail = new java.util.ArrayDeque<>();

    public FractalWyrm(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FLYING_SPEED, 0.7).add(Attributes.MOVEMENT_SPEED, 0.35).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new DiveGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 p = position().add(0, getBbHeight() / 2, 0);
            if (trail.isEmpty() || trail.peekFirst().distanceToSqr(p) > 0.04) trail.addFirst(p);
            while (trail.size() > 80) trail.removeLast();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.WYRM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.WYRM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.WYRM_DEATH.get();
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    /** Circles above its prey, then dives through it. */
    class DiveGoal extends Goal {
        private int cooldown;
        private double angle;

        DiveGoal() {
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
                angle += 0.06;
                Vec3 c = t.position().add(Math.cos(angle) * 8, 6 + Math.sin(angle * 2) * 2, Math.sin(angle) * 8);
                getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.2);
                return;
            }
            getMoveControl().setWantedPosition(t.getX(), t.getY() + 0.5, t.getZ(), 2.0);
            if (distanceToSqr(t) < 4.0) {
                doHurtTarget(t);
                cooldown = 60 + random.nextInt(40);
            }
        }
    }
}
