package com.theoreticalsciences.ocean;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: the ecosystem of Titan's hidden ocean, the first for the Ecosphere to model: a predator and its prey, whose
 * numbers really do rise and fall round each other (Lotka-Volterra), with no number written in anywhere.
 * <ul>
 *   <li><b>Vent Grazer</b> (prey): a pale shrimp that farms the bacteria on the hydrothermal chimneys. Near a chimney it
 *   feeds, and a grazer that has fed long enough splits off another (while there are not too many about). It flees eels.</li>
 *   <li><b>Ice Eel</b> (predator): a long glowing eel that hunts grazers, and bites whoever comes too close. An eel that has
 *   eaten three grazers spawns another; one that finds nothing to eat for a day starves.</li>
 * </ul>
 * So grazers multiply where eels are few, eels multiply where grazers are many, and the eels' feast ends in their famine.
 */
public final class OceanMobs {
    static SoundEvent snd(Entity e, String kind) {
        return QHSounds.mob(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath(), kind);
    }

    /** A swimmer with its own steering (no vanilla fish mind): it moves by what aiStep sets, weightless in the water. */
    public abstract static class Swimmer extends WaterAnimal {
        Vec3 wander = Vec3.ZERO;

        protected Swimmer(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
        }

        @Override
        protected void registerGoals() {}

        @Override
        public void travel(Vec3 in) {
            if (isInWater()) {
                setNoGravity(true);
                move(MoverType.SELF, getDeltaMovement());
            } else {
                setNoGravity(false);
                super.travel(in);
            }
        }

        /** Most degrees a swimmer turns in a tick: fish bank round, they do not snap to face a new way. */
        protected float turnRate() {
            return 9F;
        }

        void steer(Vec3 dir, double speed) {
            if (dir.lengthSqr() < 1e-6) return;
            Vec3 v = getDeltaMovement().scale(0.88).add(dir.normalize().scale(speed));
            setDeltaMovement(v);
            face(v);
        }

        /**
         * Turns towards the way it is moving, at most {@link #turnRate()} degrees a tick, and only when it is really
         * moving: a swimmer hanging almost still keeps its heading (turning to face every tiny drift is what spun it).
         */
        void face(Vec3 v) {
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            if (h > 0.01) {
                float yaw = net.minecraft.util.Mth.approachDegrees(getYRot(), (float) (Math.atan2(-v.x, v.z) * 180 / Math.PI), turnRate());
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
            }
            if (v.lengthSqr() > 1e-4)
                setXRot(net.minecraft.util.Mth.approach(getXRot(), (float) (-Math.atan2(v.y, Math.max(h, 1e-4)) * 180 / Math.PI), turnRate()));
        }

        /** A slow random drift, turning now and then; kept off the ice ceiling and the sea floor. */
        void drift(double speed) {
            if (wander.lengthSqr() < 0.01 || random.nextInt(80) == 0)
                wander = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.3, random.nextGaussian());
            Vec3 d = wander;
            if (level().getFluidState(blockPosition().above(2)).isEmpty()) d = d.add(0, -0.6, 0);      // the ice above
            if (level().getFluidState(blockPosition().below(2)).isEmpty()) d = d.add(0, 0.6, 0);       // the floor below
            steer(d, speed);
        }

        int nearby(Class<? extends Swimmer> c, double r) {
            return level().getEntitiesOfClass(c, getBoundingBox().inflate(r)).size();
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
            return dist > 128 * 128;
        }
    }

    // ================================================================== Vent Grazer
    public static class VentGrazer extends Swimmer {
        int fed;
        BlockPos vent;

        public VentGrazer(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.6);
        }

        BlockPos findVent() {
            return nearestVent(this, 10);
        }

        /** The nearest hydrothermal chimney or black smoker within r blocks, looked for now and then. */
        static BlockPos nearestVent(Entity e, int r) {
            var chimney = QHBlocks.get("hydrothermal_chimney");
            var smoker = QHBlocks.get("black_smoker");
            BlockPos best = null;
            double bd = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(e.blockPosition().offset(-r, -r, -r), e.blockPosition().offset(r, r, r))) {
                var st = e.level().getBlockState(p);
                if (st.is(chimney) || st.is(smoker)) {
                    double d = p.distSqr(e.blockPosition());
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
            }
            return best;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || !isInWater()) return;
            if ((tickCount + getId()) % 200 == 0) vent = findVent();
            IceEel eel = null;
            double ed = 100;
            for (IceEel e : level().getEntitiesOfClass(IceEel.class, getBoundingBox().inflate(10))) {
                double d = e.distanceToSqr(this);
                if (d < ed) {
                    ed = d;
                    eel = e;
                }
            }
            if (eel != null) {
                steer(position().subtract(eel.position()), 0.06);                    // flee
            } else if (vent != null) {
                // circle the chimney, feeding on its bacteria
                double a = tickCount * 0.03 + getId();
                Vec3 want = Vec3.atCenterOf(vent).add(Math.cos(a) * 3, 1 + Math.sin(a * 0.7), Math.sin(a) * 3);
                steer(want.subtract(position()), 0.02);
                if (position().distanceToSqr(Vec3.atCenterOf(vent)) < 49) fed++;
            } else drift(0.012);
            // fed enough: it splits off another grazer, unless the water is crowded
            if (fed >= 900 && level() instanceof ServerLevel sl) {
                fed = 0;
                if (nearby(VentGrazer.class, 32) < 14) {
                    VentGrazer b = OceanRegistry.VENT_GRAZER.get().create(sl);
                    if (b != null) {
                        b.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
                        sl.addFreshEntity(b);
                        sl.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putInt("Fed", fed);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            fed = tag.getInt("Fed");
        }
    }

    // ================================================================== Ice Eel
    public static class IceEel extends Swimmer {
        int hunger, meals, bite;

        public IceEel(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
            this.xpReward = 5;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.8)
                    .add(Attributes.FOLLOW_RANGE, 20.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || !isInWater()) return;
            hunger++;
            if (bite > 0) bite--;
            LivingEntity prey = null;
            double pd = 20 * 20;
            for (VentGrazer g : level().getEntitiesOfClass(VentGrazer.class, getBoundingBox().inflate(20))) {
                double d = g.distanceToSqr(this);
                if (d < pd) {
                    pd = d;
                    prey = g;
                }
            }
            Player near = level().getNearestPlayer(this, 6);
            if (near != null && (near.isCreative() || near.isSpectator())) near = null;
            if (near != null) prey = near;
            if (prey != null) {
                steer(prey.position().add(0, prey.getBbHeight() / 2, 0).subtract(position()), 0.05);
                if (distanceToSqr(prey) < 2.2 && bite == 0) {
                    bite = 20;
                    if (prey instanceof VentGrazer g) {
                        g.hurt(damageSources().mobAttack(this), 100F);
                        hunger = 0;
                        heal(4F);
                        if (++meals >= 3 && level() instanceof ServerLevel sl) {
                            meals = 0;
                            if (nearby(IceEel.class, 48) < 5) {
                                IceEel b = OceanRegistry.ICE_EEL.get().create(sl);
                                if (b != null) {
                                    b.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
                                    sl.addFreshEntity(b);
                                }
                            }
                        }
                    } else doHurtTarget(prey);
                }
            } else drift(0.02);
            // a day with nothing to eat, and it starves
            if (hunger > 24000 && tickCount % 100 == 0) hurt(damageSources().starve(), 2F);
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putInt("Hunger", hunger);
            tag.putInt("Meals", meals);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            hunger = tag.getInt("Hunger");
            meals = tag.getInt("Meals");
        }
    }

    // ================================================================== Brine Jelly
    /** A drifter: it pulses up and sinks back, carried by the slow currents; whatever brushes it is stung. Eels leave it be. */
    public static class BrineJelly extends Swimmer {
        int sting;

        public BrineJelly(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
        }

        /** Where it is in its stroke, 0 to 1: 0 is the moment the bell snaps shut (from the world's clock, the same on both sides). */
        public static float stroke(Level l, int id, float partial) {
            return (((l.getGameTime() + id * 7L) % 50L) + partial) / 50F;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.2);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || !isInWater()) return;
            if (sting > 0) sting--;
            // it swims bell-first: a stroke every two and a half seconds (the bell snaps shut and drives it along its heading),
            // then it coasts and sinks a little while the bell opens again. The renderer reads the same clock.
            if (wander.lengthSqr() < 0.01 || random.nextInt(200) == 0)
                wander = new Vec3(random.nextGaussian(), 0.35 + random.nextDouble() * 0.6, random.nextGaussian()).normalize();
            Vec3 v = getDeltaMovement().scale(0.95);
            // it turns, slowly, to face the way it means to swim (its bell leads)
            float want = (float) (Math.atan2(-wander.x, wander.z) * 180 / Math.PI);
            setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), want, 3F));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            Vec3 ahead = new Vec3(-Math.sin(Math.toRadians(getYRot())), 0, Math.cos(Math.toRadians(getYRot())));
            if (stroke(level(), getId(), 0) < 0.04) v = v.add(ahead.scale(0.03 * Math.sqrt(1 - wander.y * wander.y)).add(0, wander.y * 0.03, 0));
            else v = v.add(0, -0.0012, 0);
            if (level().getFluidState(blockPosition().above(2)).isEmpty()) v = v.add(0, -0.01, 0);
            setDeltaMovement(v);
            if (sting == 0)
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2, 1.2, 0.2).move(0, -1, 0))) {
                    if (e == this || e instanceof BrineJelly || e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
                    e.hurt(damageSources().mobAttack(this), 1.5F);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 80, 0));
                    sting = 30;
                }
        }
    }

    // ================================================================== Vent Crab
    /**
     * A yeti crab (Kiwa): it walks the floor round the vents, waving its hairy claws to feed the bacteria it farms on them.
     * Harmless unless struck (then it pinches). Near a vent long enough, it broods another (while there are not too many).
     */
    public static class VentCrab extends Swimmer {
        BlockPos vent;
        int fed, pinch, rest;
        float heading = Float.NaN;
        boolean homing;

        public VentCrab(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                    .add(Attributes.ARMOR, 4.0).add(Attributes.STEP_HEIGHT, 1.0);
        }

        @Override
        public void travel(Vec3 in) {
            setNoGravity(false);                         // it walks: weight in the water, on the floor
            if (isInWater()) {
                Vec3 v = getDeltaMovement();
                // it sinks gently until it stands, then stays put (no bouncing on the floor)
                double vy = onGround() ? Math.min(0, v.y) : Math.max(-0.12, v.y - 0.02);
                move(MoverType.SELF, new Vec3(v.x, vy, v.z));
            } else super.travel(in);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            if (pinch > 0) pinch--;
            if ((tickCount + getId()) % 200 == 0) vent = VentGrazer.nearestVent(this, 12);
            Vec3 v = getDeltaMovement();
            if (Float.isNaN(heading)) heading = getYRot();
            Vec3 go = null;
            boolean walk = true;
            LivingEntity foe = getLastHurtByMob();
            if (foe != null && foe.isAlive() && distanceToSqr(foe) < 36) {
                go = foe.position().subtract(position());
                rest = 0;
                if (distanceToSqr(foe) < 2.5 && pinch == 0) {
                    doHurtTarget(foe);
                    pinch = 30;
                }
            } else {
                // home on the vent when it has strayed far; once close, potter about it (a gap between the two, so it never flips)
                if (vent != null) {
                    double d = position().distanceToSqr(Vec3.atCenterOf(vent));
                    if (d > 64) homing = true;
                    else if (d < 16) homing = false;
                } else homing = false;
                if (homing) go = Vec3.atCenterOf(vent).subtract(position());
                else {
                    if (vent != null) fed++;
                    // a crab's day: stand and pick at the rock a while, then amble off in a new direction
                    if (rest > 0) {
                        rest--;
                        walk = false;
                    } else if (random.nextInt(90) == 0) rest = 40 + random.nextInt(80);
                    if (walk && (wander.lengthSqr() < 0.01 || random.nextInt(150) == 0 || horizontalCollision && random.nextInt(10) == 0)) {
                        float a = heading + (random.nextFloat() - 0.5F) * 140F;     // a new way, not too far from the old
                        wander = new Vec3(-Mth.sin(a * Mth.DEG_TO_RAD), 0, Mth.cos(a * Mth.DEG_TO_RAD));
                    }
                    go = wander;
                }
            }
            if (go != null && go.lengthSqr() > 1e-4 && walk) {
                // turn a little each tick toward the way it means to go, and walk where it faces (no snapping side to side)
                float want = (float) (Math.atan2(-go.x, go.z) * 180 / Math.PI);
                heading = Mth.approachDegrees(heading, want, 6F);
                float diff = Math.abs(Mth.wrapDegrees(want - heading));
                double sp = diff > 60 ? 0.012 : 0.045;
                double fx = -Mth.sin(heading * Mth.DEG_TO_RAD) * sp, fz = Mth.cos(heading * Mth.DEG_TO_RAD) * sp;
                setDeltaMovement(v.x * 0.5 + fx, v.y, v.z * 0.5 + fz);
            } else setDeltaMovement(v.x * 0.5, v.y, v.z * 0.5);
            setYRot(heading);
            yBodyRot = heading;
            yHeadRot = heading;                                  // head and body as one, so the body never swings back to meet it
            if (fed >= 2400 && level() instanceof ServerLevel sl) {
                fed = 0;
                if (nearby(VentCrab.class, 24) < 6) {
                    VentCrab b = OceanRegistry.VENT_CRAB.get().create(sl);
                    if (b != null) {
                        b.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
                        sl.addFreshEntity(b);
                    }
                }
            }
        }
    }

    private OceanMobs() {}
}
