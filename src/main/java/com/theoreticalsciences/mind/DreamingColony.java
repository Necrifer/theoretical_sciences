package com.theoreticalsciences.mind;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * 1.4.0: the Dreaming Colony, a hive mind of the Neural Cortex: a few hundred neurons grown into one floating knot,
 * dreaming aloud. It learns the habits of whoever fights it.
 * <ul>
 *   <li><b>Habituation</b>: it keeps count of how it has been hurt (blades and fists, arrows and other projectiles,
 *   magic, fire, blasts). The more often one kind has hurt it, the less that kind does: down to half, after six
 *   blows of it. The counts fade again over time. A fighter who keeps changing weapons does best.</li>
 *   <li><b>Keeping its distance</b>: it hangs about a player at a range it has learnt: further off from someone who
 *   has been fighting it hand to hand, closer in (and weaving) to someone who shoots.</li>
 *   <li><b>Dreaming aloud</b>: every few seconds it pulses; a player within seven blocks is dazed (nausea and a little
 *   harm), as if waking from a dream.</li>
 * </ul>
 */
public class DreamingColony extends PathfinderMob {
    public static final String[] KINDS = {"melee", "projectile", "magic", "fire", "explosion"};
    final int[] habit = new int[KINDS.length];
    /** Client side: ticks since the last pulse (entity event 11). */
    public int pulseAge = 100;
    int pulseCooldown = 80;
    double orbitAngle;

    public DreamingColony(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 12;
        this.orbitAngle = random.nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Circle(this));
        goalSelector.addGoal(5, new Drift(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16F));
    }

    static int kind(DamageSource s) {
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return 1;
        if (s.is(DamageTypeTags.IS_FIRE)) return 3;
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return 4;
        if (s.is(DamageTypeTags.WITCH_RESISTANT_TO)) return 2;
        return 0;
    }

    /** How much of a blow of this kind gets through: 1 when new to it, down to 0.5 once it has learnt it. */
    public double through(int k) {
        return 1.0 - 0.5 * Math.min(1.0, habit[k] / 6.0);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isInvulnerableTo(src)) return false;
        int k = kind(src);
        float a = (float) (amount * through(k));
        boolean hit = super.hurt(src, a);
        if (hit && !level().isClientSide) {
            habit[k] = Math.min(12, habit[k] + 1);
            if (habit[k] == 6 && level() instanceof ServerLevel sl)       // it has learnt this one: a ripple of understanding
                sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 0.6, getZ(), 30, 0.5, 0.5, 0.5, 0.6);
        }
        return hit;
    }

    /** The range it keeps: 6 blocks, out to 10 against a fighter who closes in, in to 3.5 against an archer. */
    double range() {
        double melee = Math.min(1.0, habit[0] / 6.0), shot = Math.min(1.0, habit[1] / 6.0);
        return 6.0 + 4.0 * melee - 2.5 * shot;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 11) pulseAge = 0;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        setDeltaMovement(getDeltaMovement().multiply(0.9, 0.85, 0.9));
        if (level().isClientSide) {
            pulseAge++;
            if (random.nextInt(4) == 0)
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 0.3 + random.nextDouble() * 0.9,
                        getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.01, 0);
            return;
        }
        if (tickCount % 300 == 0)                                             // it forgets, slowly
            for (int i = 0; i < habit.length; i++) if (habit[i] > 0) habit[i]--;
        if (--pulseCooldown <= 0) {
            pulseCooldown = 90 + random.nextInt(40);
            Player p = level().getNearestPlayer(this, 7.0);
            if (p != null && !p.isCreative() && !p.isSpectator() && hasLineOfSight(p)) {
                level().broadcastEntityEvent(this, (byte) 11);
                SoundEvent s = QHSounds.mob("dreaming_colony", "pulse");
                if (s != null) playSound(s, 1.2F, 0.9F + random.nextFloat() * 0.2F);
                for (Player q : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(7.0))) {
                    if (q.isCreative() || q.isSpectator()) continue;
                    q.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                    q.hurt(damageSources().indirectMagic(this, this), 2.0F);
                }
            }
        }
    }

    /** Circles the nearest player at the range it has learnt, weaving up and down. */
    static class Circle extends Goal {
        final DreamingColony c;
        @Nullable
        Player p;

        Circle(DreamingColony c) {
            this.c = c;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            p = c.level().getNearestPlayer(c, 16.0);
            return p != null && !p.isCreative() && !p.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return p != null && p.isAlive() && !p.isSpectator() && c.distanceToSqr(p) < 20 * 20;
        }

        @Override
        public void tick() {
            if (p == null) return;
            c.orbitAngle += 0.025 + (c.habit[1] > 2 ? 0.03 : 0);              // archers make it weave faster
            double r = c.range();
            Vec3 to = p.position().add(Math.cos(c.orbitAngle) * r, 2.2 + Math.sin(c.tickCount * 0.07) * 0.8, Math.sin(c.orbitAngle) * r);
            if (!c.level().getBlockState(BlockPos.containing(to)).isAir()) to = to.add(0, 1.5, 0);
            Vec3 d = to.subtract(c.position());
            if (d.lengthSqr() > 1e-4) c.setDeltaMovement(c.getDeltaMovement().scale(0.85).add(d.normalize().scale(Math.min(0.05, d.length() * 0.02))));
            c.getLookControl().setLookAt(p, 30F, 30F);
        }
    }

    /** Left alone, it drifts about where it is, never far. */
    static class Drift extends Goal {
        final DreamingColony c;
        Vec3 to;

        Drift(DreamingColony c) {
            this.c = c;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (c.random.nextInt(50) != 0) return false;
            to = c.position().add((c.random.nextDouble() - 0.5) * 10, (c.random.nextDouble() - 0.5) * 3, (c.random.nextDouble() - 0.5) * 10);
            return c.level().getBlockState(BlockPos.containing(to)).isAir();
        }

        @Override
        public boolean canContinueToUse() {
            return to != null && c.position().distanceToSqr(to) > 0.25;
        }

        @Override
        public void tick() {
            Vec3 d = to.subtract(c.position());
            if (d.lengthSqr() > 1e-4) c.setDeltaMovement(c.getDeltaMovement().scale(0.8).add(d.normalize().scale(0.015)));
        }

        @Override
        public void stop() {
            to = null;
        }
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.mob("dreaming_colony", "ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.mob("dreaming_colony", "hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.mob("dreaming_colony", "death");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putIntArray("Habits", habit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        int[] h = t.getIntArray("Habits");
        System.arraycopy(h, 0, habit, 0, Math.min(h.length, habit.length));
    }
}
