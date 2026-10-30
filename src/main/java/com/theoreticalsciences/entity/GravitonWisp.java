package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * A closed loop of gravity drifting through the Bulk. It hangs a few blocks from its prey and every few seconds
 * pulses: everything nearby is dragged toward it and briefly loses its footing. Up close it crushes.
 */
public class GravitonWisp extends Monster {
    public GravitonWisp(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FLYING_SPEED, 0.45).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new PulseGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.WISP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.WISP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.WISP_DEATH.get();
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(2) == 0) {
            double a = random.nextDouble() * Math.PI * 2, r = 1.2;
            level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX() + Math.cos(a) * r, getY() + 0.5, getZ() + Math.sin(a) * r, -Math.cos(a) * r, 0, -Math.sin(a) * r);
        }
    }

    /** Hovers at 4-6 blocks and pulses gravity; crushes if the prey gets too close. */
    class PulseGoal extends Goal {
        private int cooldown = 60;

        PulseGoal() {
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
            double a = tickCount * 0.04 + getId();
            getMoveControl().setWantedPosition(t.getX() + Math.cos(a) * 5, t.getY() + 2.5, t.getZ() + Math.sin(a) * 5, 1.0);
            if (--cooldown <= 0 && level() instanceof ServerLevel sl) {
                cooldown = 70 + random.nextInt(40);
                playSound(QHSounds.WISP_PULL.get(), 1.5F, 1.0F);
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX(), getY() + 0.5, getZ(), 60, 1.5, 1.5, 1.5, 0.3);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(9), e -> e != GravitonWisp.this && e.isAlive())) {
                    if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                    Vec3 d = position().subtract(e.position());
                    double len = d.length();
                    if (len < 0.5) continue;
                    e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(Math.min(1.2, 6 / len) * 0.6)));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0));
                }
            }
            if (distanceToSqr(t) < 4 && tickCount % 20 == 0) doHurtTarget(t);
        }
    }
}
