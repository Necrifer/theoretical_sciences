package com.theoreticalsciences.engineering;

import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * 1.4.0: the Rust Mite. An iron-eating pest the size of a fist, with a shell of flaking oxide: it lives on the charge of
 * machines, as rust lives on iron. It never breaks a block. It looks for a machine with energy in it within 16 blocks,
 * clings to it and drains its charge, a little each second; but a live Sacrificial Anode within 12 blocks draws it
 * first, and it eats the anode away instead (the anode corrodes so the machine does not). It shies from players, and
 * bites only when cornered or struck. Found in Titan, the Antiverse, and the derelict orbital rings.
 */
public class RustMite extends PathfinderMob {
    /** Client side: ticks left of the feeding pose (entity event 10). */
    public int feedAnim;
    @Nullable
    BlockPos meal;
    boolean mealIsAnode;
    int lookCooldown;

    public RustMite(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));            // only once struck
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 5F, 1.0, 1.3, p -> getTarget() == null));
        goalSelector.addGoal(3, new Feed(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());   // a swarm turns together
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 10) feedAnim = 30;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (feedAnim > 0) feedAnim--;
    }

    /** The nearest live anode within 12 blocks (a few hundred block reads, every few seconds). */
    @Nullable
    BlockPos findAnode(ServerLevel level) {
        BlockPos me = blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-12, -4, -12), me.offset(12, 4, 12))) {
            if (!SacrificialAnodeBlock.live(level.getBlockState(p))) continue;
            double d = p.distSqr(me);
            if (d < bd) {
                bd = d;
                best = p.immutable();
            }
        }
        return best;
    }

    /** The nearest machine with charge in it within 16 blocks (loaded chunks only). */
    @Nullable
    BlockPos findMachine(ServerLevel level) {
        BlockPos me = blockPosition();
        int cx = me.getX() >> 4, cz = me.getZ() >> 4;
        BlockPos best = null;
        double bd = 16 * 16;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                for (BlockEntity be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values()) {
                    if (!(be instanceof MachineBlockEntity m) || m.energy.stored() <= 0) continue;
                    double d = be.getBlockPos().distSqr(me);
                    if (d < bd) {
                        bd = d;
                        best = be.getBlockPos();
                    }
                }
            }
        return best;
    }

    /** One bite at the meal it is clinging to. */
    void bite(ServerLevel level) {
        if (meal == null) return;
        if (mealIsAnode) {
            if (!SacrificialAnodeBlock.corrode(level, meal)) meal = null;
        } else if (level.getBlockEntity(meal) instanceof MachineBlockEntity m && m.energy.stored() > 0) {
            // a bite is 2% of what is there and at least 2,000 FE: a nuisance to a small machine, a slow leak to a big one
            m.energy.drain(Math.max(2_000, m.energy.stored() / 50));
        } else {
            meal = null;
        }
        level.broadcastEntityEvent(this, (byte) 10);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.62F, 0.30F, 0.12F), 0.9F), getX(), getY() + 0.2, getZ(), 6, 0.2, 0.1, 0.2, 0.01);
        SoundEvent s = QHSounds.mob("rust_mite", "chew");
        if (s != null) playSound(s, 0.6F, 0.9F + random.nextFloat() * 0.3F);
    }

    /**
     * Its own feeding: find a meal (an anode before a machine), walk to it, and chew while beside it. It looks again every
     * few seconds, so a new anode draws it off a machine.
     */
    static class Feed extends Goal {
        final RustMite m;
        int chew;

        Feed(RustMite m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (m.getTarget() != null || !(m.level() instanceof ServerLevel level)) return false;
            if (m.lookCooldown-- > 0 && m.meal != null) return true;
            m.lookCooldown = 60 + m.random.nextInt(40);
            BlockPos a = m.findAnode(level);
            if (a != null) {
                m.meal = a;
                m.mealIsAnode = true;
                return true;
            }
            m.meal = m.findMachine(level);
            m.mealIsAnode = false;
            return m.meal != null;
        }

        @Override
        public boolean canContinueToUse() {
            return m.meal != null && m.getTarget() == null;
        }

        @Override
        public void tick() {
            if (m.meal == null || !(m.level() instanceof ServerLevel level)) return;
            m.getLookControl().setLookAt(m.meal.getX() + 0.5, m.meal.getY() + 0.5, m.meal.getZ() + 0.5);
            double d = m.distanceToSqr(m.meal.getX() + 0.5, m.meal.getY() + 0.5, m.meal.getZ() + 0.5);
            if (d > 2.6) {
                if (m.tickCount % 10 == 0) m.getNavigation().moveTo(m.meal.getX() + 0.5, m.meal.getY(), m.meal.getZ() + 0.5, 1.0);
                return;
            }
            m.getNavigation().stop();
            if (++chew >= 20) {                                   // a bite a second
                chew = 0;
                m.bite(level);
            }
        }

        @Override
        public void stop() {
            chew = 0;
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0));          // iron in the blood, briefly worse for wear
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.mob("rust_mite", "ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.mob("rust_mite", "hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.mob("rust_mite", "death");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (meal != null) {
            t.putLong("Meal", meal.asLong());
            t.putBoolean("MealAnode", mealIsAnode);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Meal")) {
            meal = BlockPos.of(t.getLong("Meal"));
            mealIsAnode = t.getBoolean("MealAnode");
        }
    }
}
