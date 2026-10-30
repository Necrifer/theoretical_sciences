package com.theoreticalsciences.entity;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.List;

/**
 * A fragment of a broken law of nature given will, shaped like a tesseract. It hovers at a distance and casts
 * homing law-bolts (whatever they hit floats away - gravity no longer applies to it), and whatever it touches
 * starts obeying a different law: gravity reverses, time slows, light leaves.
 */
public class LawWraith extends Monster {
    private static final List<Holder<MobEffect>> LAWS = List.of(MobEffects.LEVITATION, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.BLINDNESS,
            MobEffects.WEAKNESS, MobEffects.CONFUSION, MobEffects.DIG_SLOWDOWN, MobEffects.DARKNESS, MobEffects.HUNGER);

    public LawWraith(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.FLYING_SPEED, 0.4).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        nav.setCanOpenDoors(false);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new CastGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(LAWS.get(random.nextInt(LAWS.size())), 120, 1));
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.LAW_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.LAW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.LAW_DEATH.get();
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && random.nextInt(2) == 0)
            level().addParticle(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.3, 0);
    }

    /** Keeps 5-9 blocks from its target, circling it, and casts a homing law-bolt every few seconds; strikes if cornered. */
    class CastGoal extends Goal {
        private int cooldown = 40, melee;

        CastGoal() {
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
            double d = distanceTo(t);
            double ang = (tickCount * 0.03) + getId();
            double want = 7;
            double tx = t.getX() + Math.cos(ang) * want, tz = t.getZ() + Math.sin(ang) * want;
            getMoveControl().setWantedPosition(tx, t.getY() + 2.5, tz, d > 10 ? 1.3 : 1.0);
            if (--cooldown <= 0 && hasLineOfSight(t)) {
                cooldown = 50 + random.nextInt(30);
                level().addFreshEntity(new ShulkerBullet(level(), LawWraith.this, t, Direction.Axis.Y));
                playSound(QHSounds.LAW_SHOOT.get(), 1.5F, 0.9F + random.nextFloat() * 0.2F);
            }
            if (--melee <= 0 && d < 2.2) {
                melee = 20;
                doHurtTarget(t);
            }
        }
    }
}
