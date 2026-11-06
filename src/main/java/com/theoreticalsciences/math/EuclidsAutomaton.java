package com.theoreticalsciences.math;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * 1.2.0: Euclid's Automaton, of the Platonic Realm's Elements: a marble and brass golem that does geometry. It walks only
 * in straight lines and turns only on the spot, slowly and through constructible angles (multiples of 30 degrees),
 * finishing a turn before it sets off. At the end of a line it may make a construction, the way surveyors always have:
 * it drives a pin into the ground, walks once round it at the length of its cord (Postulate 3: a circle), and then
 * walks the six chords of the hexagon round that circle, each as long as the radius (Book IV, Proposition 15), its
 * straightedge on the ground. The lines stay drawn a while, then fade. It fights only whoever hurts it, and then with
 * straight, measured thrusts (its hits push exactly along the line between you).
 */
public class EuclidsAutomaton extends PathfinderMob {
    private static final EntityDataAccessor<Vector3f> CENTRE = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.VECTOR3);
    /** Where on the circle it began (radians). */
    private static final EntityDataAccessor<Float> START = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.FLOAT);
    /** How far the construction has got: 0..1 the circle, 1..7 the hexagon's sides; negative: none. */
    private static final EntityDataAccessor<Float> PROGRESS = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.FLOAT);
    /** 1 while it is drawing (the pin is in the ground; the cord is held while it walks the circle). */
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.INT);
    /** When it finished (game time), for the fade. */
    private static final EntityDataAccessor<Long> DONE_AT = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.LONG);
    /** Roused: it has someone to fight (its eyes go red, steam pours from its vents, its head follows them). */
    private static final EntityDataAccessor<Boolean> ROUSED = SynchedEntityData.defineId(EuclidsAutomaton.class, EntityDataSerializers.BOOLEAN);
    /** How long it takes to fall: a golem does not drop like a mob. */
    public static final int DEATH_TICKS = 44;
    /** Client: how roused it looks, 0..1 (eased in and out). */
    private float rouse, rouseO;

    public static final float RADIUS = 2.5F;
    public static final int STAY = 600, FADE = 100;
    public static final int IDLE = 0, PINNING = 1, CIRCLE = 2, HEXAGON = 3;

    public EuclidsAutomaton(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        this.moveControl = new Measured(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(CENTRE, new Vector3f());
        b.define(START, 0F);
        b.define(PROGRESS, -1F);
        b.define(PHASE, IDLE);
        b.define(DONE_AT, -100_000L);
        b.define(ROUSED, false);
    }

    public boolean roused() {
        return entityData.get(ROUSED);
    }

    /** 0..1: calm to roused, smoothed (client). */
    public float rouse(float partial) {
        return Mth.lerp(partial, rouseO, rouse);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            boolean now = isAlive() && getTarget() != null && getTarget().isAlive();
            if (now != roused()) {
                entityData.set(ROUSED, now);
                if (now) playSound(net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH, 0.6F, 1.5F);     // a hiss as the pressure rises
            }
            return;
        }
        rouseO = rouse;
        rouse = Mth.clamp(rouse + (roused() ? 0.1F : -0.03F), 0, 1);
        // steam from the vents in its shoulders while roused
        if (rouse > 0.3F && isAlive() && random.nextFloat() < rouse * 0.6F) {
            float yaw = yBodyRot * Mth.DEG_TO_RAD;
            for (int side = -1; side <= 1; side += 2) {
                double ox = -Math.cos(yaw) * 0.47 * side, oz = -Math.sin(yaw) * 0.47 * side;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, getX() + ox, getY() + 1.86, getZ() + oz,
                        ox * 0.05 + (random.nextDouble() - 0.5) * 0.02, 0.06 + random.nextDouble() * 0.04, oz * 0.05 + (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    /** A golem's fall: it shudders, its knees buckle, it topples forward, and its head breaks away (see the renderer). */
    @Override
    protected void tickDeath() {
        deathTime++;
        if (level().isClientSide) {
            if (deathTime == 2)                               // the boiler gives: a burst of steam
                for (int i = 0; i < 24; i++)
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, getRandomX(0.6), getY() + 1.2 + random.nextDouble() * 0.8,
                            getRandomZ(0.6), (random.nextDouble() - 0.5) * 0.15, 0.08 + random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.15);
            if (deathTime == 32)                              // it hits the ground
                for (int i = 0; i < 16; i++)
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.POOF, getRandomX(1.4), getY() + 0.1, getRandomZ(1.4), 0, 0.02, 0);
            return;
        }
        if (deathTime == 12) playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.35F, 0.6F);         // the knees give
        if (deathTime == 32) playSound(net.minecraft.sounds.SoundEvents.STONE_BREAK, 1.0F, 0.6F);       // the fall
        if (deathTime == 36) playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.25F, 1.6F);       // the head breaks away
        if (deathTime >= DEATH_TICKS && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    public Vec3 centre() {
        Vector3f v = entityData.get(CENTRE);
        return new Vec3(v.x, v.y, v.z);
    }

    public float start() {
        return entityData.get(START);
    }

    public float progress() {
        return entityData.get(PROGRESS);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    /** Ticks since it finished its last construction (negative or huge: none to show, or still drawing). */
    public float doneAge(float partial) {
        return phase() != IDLE ? -1 : level().getGameTime() - entityData.get(DONE_AT) + partial;
    }

    /** The renderer draws its construction round it: never cull it while that is in view. */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(2 * RADIUS + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ARMOR, 6.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new Construct(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /**
     * Its walk: a turn on the spot first (at most 6 degrees a tick, never stepping while more than 20 degrees off), then
     * a straight walk. A machine's gait, without the vanilla mob's snap turns and corrections.
     */
    static class Measured extends MoveControl {
        Measured(Mob m) {
            super(m);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                super.tick();
                return;
            }
            double dx = wantedX - mob.getX(), dz = wantedZ - mob.getZ(), dy = wantedY - mob.getY();
            double d2 = dx * dx + dz * dz;
            if (d2 < 0.04) {
                mob.setZza(0);
                operation = Operation.WAIT;
                return;
            }
            float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            float off = Mth.wrapDegrees(want - mob.getYRot());
            float turn = mob.getTarget() != null ? 15F : 6F;     // quicker when it is fighting
            mob.setYRot(mob.getYRot() + Mth.clamp(off, -turn, turn));
            mob.yBodyRot = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
            float speed = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            mob.setSpeed(Math.abs(off) > 20F ? 0F : speed * (float) Math.min(1, Math.sqrt(d2) / 0.6 + 0.3));
            if (dy > mob.maxUpStep() && d2 < Math.max(1.0F, mob.getBbWidth())) mob.getJumpControl().jump();
        }
    }

    /** Walk one straight line at a constructible heading, as far as the ground goes; then, now and then, a construction. */
    static class Construct extends Goal {
        final EuclidsAutomaton m;
        Vec3 to, c;
        int phase, ticks, side, wait = 40;
        double swept, lastAngle;

        Construct(EuclidsAutomaton m) {
            this.m = m;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        boolean ground(Vec3 p) {
            BlockPos b = BlockPos.containing(p.x, m.getY() + 0.1, p.z);
            return m.level().getBlockState(b.below()).isSolid() && m.level().getBlockState(b).getCollisionShape(m.level(), b).isEmpty()
                    && m.level().getBlockState(b.above()).getCollisionShape(m.level(), b.above()).isEmpty();
        }

        @Override
        public boolean canUse() {
            if (m.getTarget() != null || --wait > 0) return false;
            wait = 30 + m.random.nextInt(60);
            double a = m.random.nextInt(12) * Math.PI / 6;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            int len = 0;
            for (int s = 1; s <= 10; s++) {                  // as far as the island goes: never off the edge
                if (!ground(m.position().add(dir.scale(s)))) break;
                len = s;
            }
            if (len < 3) return false;
            to = m.position().add(dir.scale(len));
            phase = 0;
            ticks = 0;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return m.getTarget() == null && to != null && ticks < 1200;
        }

        double angleOf(Vec3 p) {
            return Math.atan2(p.z - c.z, p.x - c.x);
        }

        Vec3 onCircle(double a) {
            return new Vec3(c.x + Math.cos(a) * RADIUS, m.getY(), c.z + Math.sin(a) * RADIUS);
        }

        /** After a line: a construction, if there is room for the whole circle, with the pin on its left. */
        boolean plan() {
            if (m.random.nextInt(3) != 0) return false;
            Vec3 f = Vec3.directionFromRotation(0, m.getYRot());
            Vec3 left = new Vec3(f.z, 0, -f.x).normalize();
            Vec3 centre = m.position().add(left.scale(RADIUS));
            for (int k = 0; k < 24; k++) {
                double a = k * Math.PI / 12;
                if (!ground(new Vec3(centre.x + Math.cos(a) * RADIUS, 0, centre.z + Math.sin(a) * RADIUS))) return false;
            }
            if (!ground(centre)) return false;
            c = centre;
            swept = 0;
            lastAngle = angleOf(m.position());
            m.entityData.set(CENTRE, new Vector3f((float) c.x, (float) m.getY(), (float) c.z));
            m.entityData.set(START, (float) lastAngle);
            m.entityData.set(PROGRESS, 0F);
            m.entityData.set(PHASE, PINNING);
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            switch (phase) {
                case 0 -> {                                    // the line
                    m.getMoveControl().setWantedPosition(to.x, to.y, to.z, 0.8);
                    m.getLookControl().setLookAt(to.x, m.getEyeY(), to.z, 6F, 30F);
                    if (m.position().distanceToSqr(to.x, m.getY(), to.z) < 0.3 || ticks > 300) {
                        if (plan()) {
                            phase = 1;
                            ticks = 0;
                        } else to = null;
                    }
                }
                case 1 -> {                                    // turn to face the pin, drive it in
                    m.getNavigation().stop();
                    m.getLookControl().setLookAt(c.x, m.getY(), c.z, 6F, 30F);
                    if (ticks == 20) m.playSound(net.minecraft.sounds.SoundEvents.ANVIL_PLACE, 0.4F, 1.8F);
                    if (ticks >= 30) {
                        phase = 2;
                        m.entityData.set(PHASE, CIRCLE);
                    }
                }
                case 2 -> {                                    // once round the pin, the cord taut, clockwise (the pin on its left)
                    double a = angleOf(m.position());
                    double da = Mth.wrapDegrees((float) Math.toDegrees(a - lastAngle)) * Mth.DEG_TO_RAD;
                    if (da < 0) swept -= da;                   // only progress counts
                    lastAngle = a;
                    Vec3 next = onCircle(a - 0.45);
                    m.getMoveControl().setWantedPosition(next.x, next.y, next.z, 0.55);
                    m.getLookControl().setLookAt(next.x, m.getEyeY() - 0.5, next.z, 6F, 30F);
                    m.entityData.set(PROGRESS, (float) Math.min(1, swept / (Math.PI * 2)));
                    if (swept >= Math.PI * 2 || ticks > 700) {
                        phase = 3;
                        side = 1;
                        m.entityData.set(PHASE, HEXAGON);
                        m.entityData.set(PROGRESS, 1F);
                    }
                }
                case 3 -> {                                    // the hexagon: six straight chords, each as long as the cord
                    double a0 = m.entityData.get(START);
                    Vec3 v = onCircle(a0 - side * Math.PI / 3);
                    m.getMoveControl().setWantedPosition(v.x, v.y, v.z, 0.6);
                    m.getLookControl().setLookAt(v.x, m.getEyeY() - 0.5, v.z, 6F, 30F);
                    double left = Math.sqrt(m.position().distanceToSqr(v.x, m.getY(), v.z));
                    m.entityData.set(PROGRESS, (float) (side + Mth.clamp(1 - left / RADIUS, 0, 1)));
                    if (left < 0.3) side++;
                    if (side > 6 || ticks > 1150) finish();
                }
                default -> {}
            }
        }

        void finish() {
            m.entityData.set(PROGRESS, 7F);
            m.entityData.set(PHASE, IDLE);
            m.entityData.set(DONE_AT, m.level().getGameTime());
            to = null;
            wait = 200 + m.random.nextInt(200);
        }

        @Override
        public void stop() {
            if (m.entityData.get(PHASE) != IDLE) {             // interrupted (struck, or stuck): what is drawn stays and fades
                m.entityData.set(PHASE, IDLE);
                m.entityData.set(DONE_AT, m.level().getGameTime());
            }
            to = null;
            phase = 0;
            m.getNavigation().stop();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            // a measured thrust: straight along the line from it to you, never sideways
            Vec3 d = le.position().subtract(position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1e-4) {
                d = d.normalize().scale(0.6);
                le.setDeltaMovement(d.x, 0.15, d.z);
                le.hurtMarked = true;
            }
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return QHSounds.mob("euclids_automaton", "ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return QHSounds.mob("euclids_automaton", "hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return QHSounds.mob("euclids_automaton", "death");
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return dist > 128 * 128;
    }
}
