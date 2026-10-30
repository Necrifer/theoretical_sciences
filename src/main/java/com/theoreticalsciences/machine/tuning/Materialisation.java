package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.entity.NaniteSwarmEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Nothing a containment failure lets loose just appears: it takes shape. Each creature forms over a couple of seconds
 * at its spot - a vortex of its failure's particles spiralling in and condensing, with its own sounds - and then
 * steps out in a burst. A creature caught by the failure and changed by it (a villager into a Destabilised Husk, a
 * monster into a Void Wraith) is held in place and glitches while the change takes it.
 */
public final class Materialisation {
    public static final int LENGTH = 40;

    private static final class Job {
        final ServerLevel level;
        Vec3 at;
        final TuningProfile.Hazard kind;
        final EntityType<? extends Mob> type;
        final UUID convert;
        final int size;
        int delay, age;

        Job(ServerLevel level, Vec3 at, TuningProfile.Hazard kind, EntityType<? extends Mob> type, @Nullable UUID convert, int size, int delay) {
            this.level = level;
            this.at = at;
            this.kind = kind;
            this.type = type;
            this.convert = convert;
            this.size = size;
            this.delay = delay;
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    /** A new creature forms at `at` after `delay` ticks. */
    public static void spawn(ServerLevel level, Vec3 at, TuningProfile.Hazard kind, EntityType<? extends Mob> type, int delay) {
        JOBS.add(new Job(level, at, kind, type, null, 1, delay));
    }

    /** A Nanite Swarm of the given size assembles at `at` (subject to the swarm cap when it completes). */
    public static void swarm(ServerLevel level, Vec3 at, int size, int delay) {
        JOBS.add(new Job(level, at, TuningProfile.Hazard.INFECTIOUS, null, null, size, delay));
    }

    /** An existing creature is changed into `into`. */
    public static void convert(ServerLevel level, Mob victim, TuningProfile.Hazard kind, EntityType<? extends Mob> into, int delay) {
        JOBS.add(new Job(level, victim.position(), kind, into, victim.getUUID(), 1, delay));
    }

    public static void clear() {
        JOBS.clear();
    }

    public static void tick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level) || JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job j = it.next();
            if (j.level != level) {
                if (j.level.getServer() != level.getServer()) it.remove();         // a stale job from a closed world
                continue;
            }
            if (!level.isLoaded(net.minecraft.core.BlockPos.containing(j.at))) {     // its chunk unloaded: it never forms
                it.remove();
                continue;
            }
            if (j.delay > 0) {
                j.delay--;
                continue;
            }
            Mob victim = null;
            if (j.convert != null) {
                Entity v = level.getEntity(j.convert);
                if (!(v instanceof Mob m) || !m.isAlive()) {
                    it.remove();
                    continue;
                }
                victim = m;
                j.at = m.position();
                // held in place while it changes
                m.setDeltaMovement(Vec3.ZERO);
                m.getNavigation().stop();
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 9, true, false));
                if (j.age % 4 == 0) m.setYRot(m.getYRot() + (level.random.nextFloat() - 0.5F) * 60F);    // glitching
            }
            form(level, j);
            if (++j.age >= LENGTH) {
                finish(level, j, victim);
                it.remove();
            }
        }
    }

    /** One tick of the forming effect. */
    private static void form(ServerLevel level, Job j) {
        float k = j.age / (float) LENGTH;
        double cy = j.at.y + 0.9;
        ParticleOptions main = mainParticle(j.kind), accent = accent(j.kind);
        // three arms spiralling inward and upward
        for (int arm = 0; arm < 3; arm++) {
            double a = j.age * 0.45 + arm * Math.PI * 2 / 3;
            double r = 2.6 * (1 - k) + 0.25;
            double y = cy - 0.9 + 1.8 * ((j.age * 0.07 + arm / 3.0) % 1.0);
            level.sendParticles(main, j.at.x + Math.cos(a) * r, y, j.at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            if (arm == 0 || k > 0.5) level.sendParticles(accent, j.at.x + Math.cos(a + 0.4) * r * 0.8, y, j.at.z + Math.sin(a + 0.4) * r * 0.8, 1, 0, 0, 0, 0);
        }
        // a condensing core that grows into the creature's shape
        level.sendParticles(main, j.at.x, cy, j.at.z, 1 + (int) (k * 5), 0.12 + 0.3 * k, 0.3 + 0.6 * k, 0.12 + 0.3 * k, 0.005);
        if (j.age == 0) level.playSound(null, j.at.x, cy, j.at.z, startSound(j.kind), SoundSource.HOSTILE, 1.6F, 0.6F);
        if (j.age % 8 == 4) level.playSound(null, j.at.x, cy, j.at.z, com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.HOSTILE, 1.0F, 0.5F + k * 1.2F);
    }

    private static void finish(ServerLevel level, Job j, @Nullable Mob victim) {
        double cy = j.at.y + 0.9;
        Mob made = null;
        if (j.kind == TuningProfile.Hazard.INFECTIOUS && j.type == null) {
            made = NaniteSwarmEntity.spawn(level, j.at.x, cy, j.at.z, j.size);
        } else if (victim != null) {
            made = victim.convertTo(j.type, false);
        } else if (j.type != null) {
            Mob m = j.type.create(level);
            if (m != null) {
                m.moveTo(j.at.x, j.at.y, j.at.z, level.random.nextFloat() * 360F, 0);
                level.addFreshEntity(m);
                made = m;
            }
        }
        level.sendParticles(mainParticle(j.kind), j.at.x, cy, j.at.z, 60, 0.4, 0.7, 0.4, 0.25);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), j.at.x, cy, j.at.z, 1, 0, 0, 0, 0);
        if (made != null) level.playSound(null, j.at.x, cy, j.at.z, endSound(j.kind), SoundSource.HOSTILE, 1.4F, 0.8F);
    }

    private static ParticleOptions mainParticle(TuningProfile.Hazard h) {
        return switch (h) {
            case SPATIAL -> com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get();
            case DIMENSIONAL -> com.theoreticalsciences.registry.QHParticles.GLYPH.get();
            case TEMPORAL -> com.theoreticalsciences.registry.QHParticles.PHOTON.get();
            case INFECTIOUS -> com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get();
            case DESTABILISATION -> com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get();
        };
    }

    private static ParticleOptions accent(TuningProfile.Hazard h) {
        return switch (h) {
            case SPATIAL -> com.theoreticalsciences.registry.QHParticles.VOID_INK.get();
            case DIMENSIONAL -> com.theoreticalsciences.registry.QHParticles.PHOTON.get();
            case TEMPORAL -> com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get();
            case INFECTIOUS -> com.theoreticalsciences.registry.QHGlowOptions.of(0.62F, 0.64F, 0.7F, 1.2F);
            case DESTABILISATION -> com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get();
        };
    }

    private static SoundEvent startSound(TuningProfile.Hazard h) {
        return switch (h) {
            case SPATIAL -> com.theoreticalsciences.registry.QHSounds.EMERGENCE.get();
            case DIMENSIONAL -> com.theoreticalsciences.registry.QHSounds.CONFIRM.get();
            case TEMPORAL -> com.theoreticalsciences.registry.QHSounds.POWER_UP.get();
            case INFECTIOUS -> com.theoreticalsciences.registry.QHSounds.SWARM_AMBIENT.get();
            case DESTABILISATION -> com.theoreticalsciences.registry.QHSounds.MUTATE.get();
        };
    }

    private static SoundEvent endSound(TuningProfile.Hazard h) {
        return switch (h) {
            case SPATIAL -> com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get();
            case DIMENSIONAL -> com.theoreticalsciences.registry.QHSounds.CHARGE.get();
            case TEMPORAL -> com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get();
            case INFECTIOUS -> com.theoreticalsciences.registry.QHSounds.SWARM_SPLIT.get();
            case DESTABILISATION -> com.theoreticalsciences.registry.QHSounds.MUTATE.get();
        };
    }

    private Materialisation() {}
}
