package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.entity.OwnAI;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.science.RadiationService;
import com.theoreticalsciences.science.Sciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 1.3.7: the Planetary Interior's creatures. Each acts out a piece of the planet's physics, and each thinks for itself
 * (no vanilla mob's mind - see {@link OwnAI}).
 * <ul>
 *   <li><b>Magma Crawler</b> (Magma Chambers): a basalt centipede at home in Basaltic Magma, swimming through it as
 *   through water; its bite burns.</li>
 *   <li><b>Plume Elemental</b> (Mantle Convection Zone): a blob of hot mantle rock that convects - it heats near the
 *   bottom, rises while it is hotter than its surroundings, cools and sinks - and launches whoever stands under it.</li>
 *   <li><b>Fission Husk</b> (Natural Reactor): an irradiated husk that splits in two when killed, twice, releasing a burst
 *   of neutrons each time - fission.</li>
 *   <li><b>Geodynamo Wraith</b> (Geodynamo Core): a loop of current in the core. Ferromagnetic armour near it is
 *   magnetised and drawn in, and it can pull a ferromagnetic item out of your hand.</li>
 * </ul>
 */
public final class InteriorMobs {
    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    static boolean inMagma(Entity e) {
        var m = QHFluids.get("basaltic_magma");
        return m != null && m.type != null && e.isInFluidType(m.type.get());
    }

    /** Adds to a player's radiation dose (mSv), as standing near a source would. */
    static void irradiate(Entity target, double mSv) {
        if (target instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator())
            RadiationService.setDose(p, RadiationService.dose(p) + mSv * InteriorGear.radiationFactor(p));
    }

    /** A hostile base: no vanilla habits, its own sounds. */
    public abstract static class Base extends Monster {
        protected Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
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
            return dist > 96 * 96;
        }
    }

    // ================================================================== Magma Crawler
    public static class MagmaCrawler extends Base {
        public MagmaCrawler(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
        }

        /** It hunts only what comes near its sea: prey more than a few blocks from the magma's edge is left alone. */
        boolean worthChasing(LivingEntity t) {
            if (t == null || !t.isAlive()) return false;
            if (inMagma(t)) return true;
            return t.distanceToSqr(this) < 6 * 6;
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(1, new MagmaSwim(this));
            goalSelector.addGoal(2, new ReturnToMagma(this));
            goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true) {
                @Override
                public boolean canUse() {
                    return !inMagma(mob) && worthChasing(mob.getTarget()) && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                    return worthChasing(mob.getTarget()) && super.canContinueToUse();
                }
            });
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
            goalSelector.addGoal(7, new RandomLookAroundGoal(this));
            OwnAI.hunter(this);
        }

        /** Nearest magma within reach (a few tries along random directions), or null. */
        @javax.annotation.Nullable
        Vec3 magmaNear(int range) {
            var m = QHFluids.get("basaltic_magma");
            if (m == null) return null;
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            BlockPos o = blockPosition();
            for (int i = 0; i < 40; i++) {
                BlockPos p = o.offset(random.nextInt(range * 2 + 1) - range, random.nextInt(7) - 4, random.nextInt(range * 2 + 1) - range);
                if (level().getFluidState(p).getFluidType() == m.type.get()) {
                    double d = p.distSqr(o);
                    if (d < bd) {
                        bd = d;
                        best = p;
                    }
                }
            }
            return best == null ? null : Vec3.atCenterOf(best);
        }

        /** Out of the magma with nothing worth chasing, it crawls back in. */
        static class ReturnToMagma extends Goal {
            final MagmaCrawler m;
            Vec3 to;

            ReturnToMagma(MagmaCrawler m) {
                this.m = m;
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                if (inMagma(m) || m.worthChasing(m.getTarget()) || m.random.nextInt(10) != 0) return false;
                to = m.magmaNear(16);
                return to != null;
            }

            @Override
            public void start() {
                m.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
            }

            @Override
            public boolean canContinueToUse() {
                return !inMagma(m) && !m.getNavigation().isDone() && !m.worthChasing(m.getTarget());
            }

            @Override
            public void tick() {
                // the last stretch: pathfinding will not step into a fluid it does not know, so it simply crawls in
                if (m.position().distanceToSqr(to) < 9) m.setDeltaMovement(m.getDeltaMovement().add(to.subtract(m.position()).normalize().scale(0.06)));
            }
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (inMagma(this)) {
                // neutral buoyancy in rock as dense as itself, and a little drag
                setDeltaMovement(getDeltaMovement().multiply(0.9, 0.85, 0.9).add(0, 0.075, 0));
                if (level().isClientSide && random.nextInt(4) == 0)
                    level().addParticle(ParticleTypes.LAVA, getRandomX(0.6), getY() + 0.3, getRandomZ(0.6), 0, 0, 0);
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) target.igniteForSeconds(4);
            return hit;
        }

        @Override
        public boolean isSensitiveToWater() {
            return true;
        }

        @Override
        public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState fluid) {
            return false;
        }

        /** In the magma it swims: straight at its prey if it has one, otherwise in long slow arcs. */
        static class MagmaSwim extends Goal {
            final MagmaCrawler m;
            Vec3 wander;

            MagmaSwim(MagmaCrawler m) {
                this.m = m;
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                return inMagma(m);
            }

            @Override
            public void tick() {
                LivingEntity t = m.getTarget();
                Vec3 to;
                if (m.worthChasing(t)) to = t.position().add(0, t.getBbHeight() * 0.5, 0);
                else {
                    // idle: it cruises its sea in long slow arcs, only ever choosing a point that is still magma
                    if (wander == null || m.position().distanceToSqr(wander) < 4 || m.random.nextInt(120) == 0) {
                        Vec3 pick = null;
                        var mag = QHFluids.get("basaltic_magma");
                        for (int i = 0; i < 12 && pick == null; i++) {
                            Vec3 c = m.position().add((m.random.nextDouble() - 0.5) * 14, (m.random.nextDouble() - 0.6) * 4, (m.random.nextDouble() - 0.5) * 14);
                            if (mag != null && m.level().getFluidState(BlockPos.containing(c)).getFluidType() == mag.type.get()) pick = c;
                        }
                        wander = pick != null ? pick : m.position().add(0, -1, 0);
                    }
                    to = wander;
                }
                Vec3 d = to.subtract(m.position());
                double len = d.length();
                if (len > 0.1) {
                    Vec3 v = d.scale((m.worthChasing(t) ? 0.06 : 0.03) / len);
                    m.setDeltaMovement(m.getDeltaMovement().add(v));
                    m.getLookControl().setLookAt(to.x, to.y, to.z);
                    m.setYRot((float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90F);
                    m.yBodyRot = m.getYRot();
                }
                if (m.worthChasing(t) && len < 2.2 && m.isWithinMeleeAttackRange(t) && m.tickCount % 20 == 0) m.doHurtTarget(t);
            }
        }
    }

    // ================================================================== Plume Elemental
    public static class PlumeElemental extends Base {
        /** Its own temperature, in kelvin above the rock around it: buoyant while positive. */
        float excess = 200F;
        int plumeCooldown;

        public PlumeElemental(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 10;
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 6.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16F));
            OwnAI.hunter(this);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(2) == 0)
                    level().addParticle(excess > 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, getRandomX(0.5), getY() + random.nextDouble() * 1.6,
                            getRandomZ(0.5), 0, excess > 0 ? 0.04 : -0.02, 0);
                return;
            }
            // convection: the bottom of the mantle heats it, the top cools it; buoyancy follows its excess temperature
            BlockPos below = blockPosition().below(2);
            boolean hotBelow = getY() < 50 || inMagma(this) || level().getFluidState(below).getFluidType() == QHFluids.get("basaltic_magma").type.get();
            excess += hotBelow ? 6F : -1.5F - Math.max(0, (float) (getY() - 60) * 0.08F);
            excess = Math.max(-150F, Math.min(300F, excess));
            double vy = Math.max(-0.12, Math.min(0.18, excess * 0.0007));
            Vec3 v = getDeltaMovement().multiply(0.85, 0.6, 0.85).add(0, vy * 0.4, 0);
            LivingEntity t = getTarget();
            if (t != null && t.isAlive()) {
                Vec3 d = t.position().subtract(position());
                double h = Math.hypot(d.x, d.z);
                if (h > 1.0) v = v.add(d.x / h * 0.03, 0, d.z / h * 0.03);
                // whoever stands over or beside it is caught in the upwelling
                if (--plumeCooldown <= 0 && h < 3.0 && d.y > -1.5 && d.y < 4.0) {
                    plumeCooldown = 60;
                    t.setDeltaMovement(t.getDeltaMovement().x, 1.25, t.getDeltaMovement().z);
                    t.hurtMarked = true;
                    t.igniteForSeconds(3);
                    t.hurt(damageSources().mobAttack(this), 4F);
                    excess = Math.min(300F, excess + 40F);
                    ServerLevel sl = (ServerLevel) level();
                    sl.sendParticles(ParticleTypes.LAVA, t.getX(), t.getY(), t.getZ(), 20, 0.6, 0.2, 0.6, 0.1);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, t.getX(), t.getY(), t.getZ(), 15, 0.6, 0.4, 0.6, 0.05);
                    level().playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1F, 0.5F);
                    if (t instanceof ServerPlayer p)
                        p.displayClientMessage(Component.translatable("message.theoreticalsciences.plume_elemental").withStyle(ChatFormatting.GOLD), true);
                }
            } else if (random.nextInt(60) == 0) {
                v = v.add((random.nextDouble() - 0.5) * 0.2, 0, (random.nextDouble() - 0.5) * 0.2);
            }
            setDeltaMovement(v);
            if (v.horizontalDistanceSqr() > 1e-4) setYRot((float) (Math.atan2(v.z, v.x) * 180 / Math.PI) - 90F);
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
            super.addAdditionalSaveData(t);
            t.putFloat("Excess", excess);
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
            super.readAdditionalSaveData(t);
            excess = t.getFloat("Excess");
        }
    }

    // ================================================================== Fission Husk
    public static class FissionHusk extends Base {
        static final EntityDataAccessor<Integer> GEN = SynchedEntityData.defineId(FissionHusk.class, EntityDataSerializers.INT);
        public static final int MAX_GEN = 2;

        public FissionHusk(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 6;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 24.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(GEN, 0);
        }

        public int generation() {
            return entityData.get(GEN);
        }

        void setGeneration(int g) {
            entityData.set(GEN, g);
            float hp = 24F / (1 << g);
            var mh = getAttribute(Attributes.MAX_HEALTH);
            if (mh != null) mh.setBaseValue(hp);
            setHealth(hp);
            var sp = getAttribute(Attributes.MOVEMENT_SPEED);
            if (sp != null) sp.setBaseValue(0.24 + 0.05 * g);
            refreshDimensions();
        }

        public float scale() {
            return generation() == 0 ? 1F : generation() == 1 ? 0.72F : 0.5F;
        }

        @Override
        public EntityDimensions getDefaultDimensions(Pose pose) {
            return super.getDefaultDimensions(pose).scale(scale());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
            super.onSyncedDataUpdated(key);
            if (GEN.equals(key)) refreshDimensions();
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, false);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!level().isClientSide && tickCount % 20 == 0)       // it glows with what is decaying inside it
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(4)))
                    irradiate(p, 2.0 / (1 + generation()));
            if (level().isClientSide && random.nextInt(6) == 0)
                level().addParticle(ParticleTypes.GLOW, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.02, 0);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) irradiate(target, 12.0 / (1 + generation()));
            return hit;
        }

        /** Killed, it splits: two lighter husks fly apart, and a burst of neutrons goes off. */
        @Override
        public void remove(RemovalReason reason) {
            if (!level().isClientSide && reason == RemovalReason.KILLED && isDeadOrDying() && generation() < MAX_GEN && level() instanceof ServerLevel sl) {
                for (int i = 0; i < 2; i++) {
                    FissionHusk child = (FissionHusk) getType().create(sl);
                    if (child == null) continue;
                    child.setGeneration(generation() + 1);
                    double a = random.nextDouble() * Math.PI * 2 + i * Math.PI;
                    child.moveTo(getX(), getY() + 0.2, getZ(), random.nextFloat() * 360F, 0);
                    child.setDeltaMovement(Math.cos(a) * 0.35, 0.3, Math.sin(a) * 0.35);
                    if (getTarget() != null) child.setTarget(getTarget());
                    sl.addFreshEntity(child);
                }
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(3.5), e -> !(e instanceof FissionHusk))) {
                    le.hurt(damageSources().magic(), 3F);
                    irradiate(le, 20.0);
                }
                sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.8, getZ(), 30, 0.8, 0.8, 0.8, 0.2);
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.8, getZ(), 1, 0, 0, 0, 0);
                sl.playSound(null, blockPosition(), snd(this, "death"), SoundSource.HOSTILE, 1.2F, 0.6F + 0.3F * generation());
            }
            super.remove(reason);
        }

        @Override
        public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
            super.addAdditionalSaveData(t);
            t.putInt("Generation", generation());
        }

        @Override
        public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
            super.readAdditionalSaveData(t);
            if (t.contains("Generation")) entityData.set(GEN, t.getInt("Generation"));
        }

        @Override
        public float getVoicePitch() {
            return super.getVoicePitch() * (1F + 0.3F * generation());
        }
    }

    // ================================================================== Geodynamo Wraith
    public static class GeodynamoWraith extends Base {
        double phase;
        int pulse, strike;

        public GeodynamoWraith(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 12;
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 35.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16F));
            OwnAI.hunter(this);
        }

        static int ferromagnetic(LivingEntity e) {
            int n = 0;
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                if (e.getItemBySlot(s).is(Sciences.FERROMAGNETIC)) n++;
            return n;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(3) == 0)
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0, 0);
                return;
            }
            // it follows a field line: a loop round its prey (or round where it is), tilted, never still
            phase += 0.06;
            LivingEntity t = getTarget();
            Vec3 c = t != null && t.isAlive() ? t.position().add(0, 2.0, 0) : position();
            Vec3 want = t != null ? c.add(Math.cos(phase) * 5, Math.sin(phase * 2) * 1.5, Math.sin(phase) * 5) : c.add(Math.cos(phase) * 0.3, 0, Math.sin(phase) * 0.3);
            Vec3 d = want.subtract(position());
            setDeltaMovement(getDeltaMovement().scale(0.8).add(d.scale(0.04)));
            if (getDeltaMovement().horizontalDistanceSqr() > 1e-4)
                setYRot((float) (Math.atan2(getDeltaMovement().z, getDeltaMovement().x) * 180 / Math.PI) - 90F);
            if (++pulse >= 40) {
                pulse = 0;
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(10), pl -> !pl.isCreative() && !pl.isSpectator())) {
                    int n = ferromagnetic(p);
                    double dist = Math.max(1.5, p.distanceTo(this));
                    if (n > 0) {
                        p.addEffect(new MobEffectInstance(QHEffects.MAGNETIZED, 200, Math.min(3, n - 1), false, true));
                        Vec3 pull = position().subtract(p.position()).normalize().scale(0.25 * n / (dist * dist) * 9);
                        p.setDeltaMovement(p.getDeltaMovement().add(pull));
                        p.hurtMarked = true;
                    }
                    ItemStack held = p.getMainHandItem();
                    if (!held.isEmpty() && held.is(Sciences.FERROMAGNETIC) && dist < 7 && random.nextInt(4) == 0) {
                        ItemStack taken = held.copy();
                        p.getInventory().removeItem(held);
                        ItemEntity ie = new ItemEntity(level(), p.getX(), p.getEyeY() - 0.3, p.getZ(), taken);
                        ie.setDeltaMovement(position().subtract(ie.position()).normalize().scale(0.6));
                        ie.setPickUpDelay(30);
                        level().addFreshEntity(ie);
                        if (p instanceof ServerPlayer sp)
                            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.geodynamo_wraith_pull", taken.getHoverName())
                                    .withStyle(ChatFormatting.AQUA), true);
                    }
                }
                level().playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.6F, 1.8F);
            }
            if (t != null && --strike <= 0 && distanceTo(t) < 2.0) {
                strike = 30;
                doHurtTarget(t);
            }
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }
    }

    private InteriorMobs() {}

    static {
        TheoreticalSciences.LOGGER.debug("Interior creatures loaded");
    }

    // ================================================================== 1.7.0: the Random Walker
    /**
     * A cloud of particles taking an honest random walk: every tick a random kick, damped by drag - diffusion. Near prey
     * the kicks lean its way (a drift on top of the diffusion). It hovers a little above the ground. Its touch brings
     * Variance.
     */
    public static class RandomWalker extends Base {
        int strike;

        public RandomWalker(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 6;
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        protected void registerGoals() {
            OwnAI.hunter(this);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            // diffusion: a Gaussian kick each tick, half of it lost to drag
            Vec3 kick = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6, random.nextGaussian()).scale(0.05);
            LivingEntity t = getTarget();
            Vec3 drift = Vec3.ZERO;
            if (t != null && t.isAlive() && distanceToSqr(t) < 16 * 16)
                drift = t.position().add(0, 1, 0).subtract(position()).normalize().scale(0.022);     // the drift toward prey
            // keep a metre or two above the ground
            int ground = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, blockPosition().getX(), blockPosition().getZ());
            double lift = getY() < ground + 0.8 ? 0.03 : getY() > ground + 3 ? -0.02 : 0;
            setDeltaMovement(getDeltaMovement().scale(0.5).add(kick).add(drift).add(0, lift, 0));
            if (t != null && t.isAlive() && --strike <= 0 && distanceTo(t) < 1.6) {
                strike = 25;
                if (doHurtTarget(t))
                    t.addEffect(new MobEffectInstance(QHEffects.VARIANCE, 200, 0, false, true));
            }
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }
    }
}
