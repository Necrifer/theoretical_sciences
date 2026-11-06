package com.theoreticalsciences.titan;

import com.theoreticalsciences.entity.OwnAI;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.registry.QHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1.2.0: Titan's creatures. Each acts out a piece of Titan's chemistry and thinks for itself; none changes a block.
 * <ul>
 *   <li><b>Tholin Drifter</b> (the dunes and the sea): a flake of the haze, carried on Titan's slow wind. Whoever it
 *   settles on is coated in tar: they see less and move slower.</li>
 *   <li><b>Catalyst Sprite</b> (Ontario Lacus): a speck of bare metal surface. It keeps its distance and catalyses: the
 *   oxygen in your rebreather is spent faster near it, and Titan's other creatures heal faster.</li>
 *   <li><b>Methane Skater</b> (Kraken Mare): it stands on liquid methane as a pond skater does on water, skates out to
 *   whoever is on the shore and knocks them in.</li>
 *   <li><b>Cryolava Slug</b> (Sotra): slow, armoured, and cold enough to freeze what it touches; it spits cryolava.</li>
 * </ul>
 */
public final class TitanMobs {
    static final DustParticleOptions THOLIN_DUST = new DustParticleOptions(new Vector3f(0.75F, 0.42F, 0.15F), 1.2F);
    static final DustParticleOptions FROST = new DustParticleOptions(new Vector3f(0.75F, 0.9F, 1.0F), 1.0F);

    static boolean live(Player p) {
        return !p.isCreative() && !p.isSpectator();
    }

    /** Titan's wind: slow, and turning with the day (the moon's tide in its air). */
    static Vec3 wind(Level l) {
        double a = (l.getDayTime() % 24000) / 24000.0 * Math.PI * 2;
        return new Vec3(Math.cos(a), 0, Math.sin(a));
    }

    /** A floater: no gravity, drifting with a little drag. */
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

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
            return level.isUnobstructed(this);
        }

        /** Titan's own: at home at 94 K and in its liquids - they do not freeze (and, by the entity tag minecraft:can_breathe_under_water, do not drown). */
        @Override
        public boolean canFreeze() {
            return false;
        }

    }

    // ================================================================== Tholin Drifter
    public static class TholinDrifter extends Floater {
        int settle;

        public TholinDrifter(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 4;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 1.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(3) == 0) level().addParticle(THOLIN_DUST, getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, -0.01, 0);
                return;
            }
            // carried downwind, sinking slowly; toward whoever is close, as a flake is drawn into the wake of a walker
            Vec3 v = getDeltaMovement().scale(0.9).add(wind(level()).scale(0.004)).add(0, -0.0015, 0);
            LivingEntity t = getTarget();
            if (t != null && t.isAlive() && distanceToSqr(t) < 8 * 8)
                v = v.add(t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(position()).normalize().scale(0.008));
            if (onGround()) v = v.add(0, 0.03, 0);
            setDeltaMovement(v);
            if (--settle <= 0)
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.5))) {
                    if (!live(p)) continue;
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
                    doHurtTarget(p);
                    settle = 40;
                }
        }
    }

    // ================================================================== Catalyst Sprite
    public static class CatalystSprite extends Floater {
        public CatalystSprite(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0, 0);
                return;
            }
            LivingEntity t = getTarget();
            Vec3 v = getDeltaMovement().scale(0.85);
            boolean scared = getHealth() < getMaxHealth() * 0.5F;
            if (t != null && t.isAlive()) {
                // it keeps five blocks off and circles; hurt, it keeps twelve
                Vec3 d = t.position().add(0, 1.5, 0).subtract(position());
                double r = d.length(), want = scared ? 12 : 5;
                Vec3 radial = d.normalize().scale((r - want) * 0.01);
                Vec3 round = new Vec3(-d.z, 0, d.x).normalize().scale(0.02);
                v = v.add(radial).add(round);
                getLookControl().setLookAt(t);
            }
            setDeltaMovement(v);
            if (tickCount % 60 == 0) {
                ServerLevel sl = (ServerLevel) level();
                // the surface works on what is near it: the rebreather's oxygen goes faster, Titan's creatures mend faster
                if (t instanceof ServerPlayer sp && live(sp) && distanceTo(sp) < 8) {
                    ItemStack rb = TitanGear.rebreather(sp);
                    if (!rb.isEmpty()) rb.setDamageValue(Math.min(rb.getMaxDamage(), rb.getDamageValue() + 30));
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, sp.getX(), sp.getY() + 1.4, sp.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
                for (LivingEntity m : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8),
                        x -> x != this && TitanRegistry.isTitanCreature(x)))
                    m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
            }
        }
    }

    // ================================================================== Methane Skater
    public static class MethaneSkater extends InteriorMobs.Base {
        public MethaneSkater(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 8;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ATTACK_KNOCKBACK, 1.5);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.1, false);
        }

        @Override
        public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
            return level.isUnobstructed(this);
        }

        @Override
        public boolean canFreeze() {
            return false;
        }


        public static boolean methane(FluidState f) {
            var e = QHFluids.ALL.get("liquid_methane");
            return e != null && !f.isEmpty() && f.getFluidType() == e.type.value();
        }

        /** Surface tension holds it up: it stands on liquid methane as on ground. */
        @Override
        public boolean canStandOnFluid(FluidState f) {
            return methane(f);
        }

        boolean onSea() {
            return methane(level().getFluidState(BlockPos.containing(getX(), getY() - 0.1, getZ())))
                    || methane(level().getFluidState(blockPosition()));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || !onSea()) return;
            // out on the sea the walking goals have no path: it skates straight at what it hunts, or wanders on the surface
            LivingEntity t = getTarget();
            Vec3 v = getDeltaMovement();
            Vec3 push;
            if (t != null && t.isAlive() && distanceToSqr(t) < 24 * 24) push = t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.06);
            else push = random.nextInt(30) == 0 ? new Vec3(random.nextGaussian(), 0, random.nextGaussian()).normalize().scale(0.15) : Vec3.ZERO;
            setDeltaMovement(v.x * 0.92 + push.x, Math.max(v.y, 0), v.z * 0.92 + push.z);
            if (t != null) getLookControl().setLookAt(t);
            if (tickCount % 6 == 0) ((ServerLevel) level()).sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 3, 0.4, 0, 0.4, 0);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) {                                        // shoved off the shore: away from it
                Vec3 d = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.6);
                target.push(d.x, 0.2, d.z);
                target.hurtMarked = true;
            }
            return hit;
        }
    }

    // ================================================================== Cryolava Slug
    public static class CryolavaSlug extends InteriorMobs.Base {
        int spit = 60;

        public CryolavaSlug(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.xpReward = 10;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 10.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
        }

        @Override
        protected void registerGoals() {
            OwnAI.melee(this, 1.0, false);
        }

        @Override
        public boolean canFreeze() {
            return false;
        }


        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(2) == 0) level().addParticle(FROST, getRandomX(0.9), getY() + 0.1, getRandomZ(0.9), 0, 0, 0);
                return;
            }
            LivingEntity t = getTarget();
            if (t != null && t.isAlive() && --spit <= 0 && distanceTo(t) < 12 && hasLineOfSight(t)) {
                // a glob of ammonia water at 176 K: it freezes where it lands
                spit = 80;
                ServerLevel sl = (ServerLevel) level();
                Vec3 a = position().add(0, 0.5, 0), b = t.position().add(0, t.getBbHeight() * 0.5, 0);
                for (int i = 0; i <= 12; i++) {
                    Vec3 p = a.lerp(b, i / 12.0);
                    sl.sendParticles(FROST, p.x, p.y + Math.sin(i / 12.0 * Math.PI) * 0.8, p.z, 2, 0.05, 0.05, 0.05, 0);
                }
                t.hurt(damageSources().mobAttack(this), 3F);
                t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 60, t.getTicksFrozen() + 100));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true));
            }
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, target.getTicksFrozen() + 80));
            return hit;
        }
    }

    private TitanMobs() {}
}
