package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.QHConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The live tuning of one multiblock.
 * <ul>
 *   <li><b>Setting</b> (0-100): the player's dial. <b>Target</b>: the ideal value, which drifts while the machine runs.
 *       The closer they are, the higher the <b>efficiency</b> (speed, or power output for generators).</li>
 *   <li><b>Containment integrity</b> (0-100%): wears down while running (faster when overclocked, out of tune or volatile).
 *       Below 25% the machine loses efficiency; at 0% it <b>faults</b> and stops until recalibrated with a Field Calibrator.</li>
 *   <li><b>Overclock</b>: x2 speed, x4 power per level. <b>Parallel</b>: several recipes per cycle.</li>
 *   <li><b>Auto-calibrator</b>: a late-game upgrade that follows the target and repairs containment by itself, for a power tax.</li>
 * </ul>
 */
public final class TuningState {
    public float setting = 50F;
    public float target = 50F;
    public float integrity = 100F;
    public int overclock;
    public int parallel = 1;
    public boolean autoCalibrator;
    /** What the auto-calibrator mends per running tick on top of wear: a quench's damage heals within the hour. */
    static final float SELF_REPAIR = 100F / 72000F;
    /**
     * Round 25: the stabiliser installed (0 none, 1-3 the three engineered tiers, 4 the Equilibrium Lattice from the
     * Core Forge). Each tier makes the machine drift less and less often, and wear and take damage less; tier 4 is
     * perfect - no drift, no wear, no quench, no fault: completely hands off.
     */
    public int stabiliser;
    public static final int PERFECT = 4;
    /** how much of the drift is left / how much of the wear and damage is left / how many times longer between drift steps */
    public static final float[] DRIFT = {1F, 0.6F, 0.35F, 0.12F, 0F}, WEAR = {1F, 0.7F, 0.45F, 0.2F, 0F}, PERIOD = {1F, 1.5F, 2.5F, 4F, 1F};

    public float driftLeft() {
        return DRIFT[Mth.clamp(stabiliser, 0, 4)];
    }

    public float wearLeft() {
        return WEAR[Mth.clamp(stabiliser, 0, 4)];
    }

    public int period(int base) {
        return Math.max(1, Math.round(base * PERIOD[Mth.clamp(stabiliser, 0, 4)]));
    }

    public boolean perfect() {
        return stabiliser >= PERFECT;
    }
    public boolean faulted;
    /** The Omnilex Engine's hidden overclock: aleph-null. Only the impossible recipe runs at it. */
    public boolean omega;
    public int revealTicks;
    private long runTicks;
    private float oscCentre = 50F;

    /** 0.15 .. 1.0 */
    public float efficiency() {
        float match = 1F - Math.abs(setting - target) / 35F;
        float e = Mth.clamp(match, 0.15F, 1F);
        if (integrity < 25F) e *= Math.max(0.3F, integrity / 25F);
        return e;
    }

    public void tick(TuningProfile profile, boolean running, RandomSource rand) {
        if (revealTicks > 0) revealTicks--;
        if (!running || faulted) return;
        runTicks++;
        if (perfect()) {                        // the Equilibrium Lattice: nothing moves, nothing wears
            target = setting;
            integrity = 100F;
            return;
        }
        float baseVol = profile.volatility() * QHConfig.TUNING_DIFFICULTY.get().floatValue() * driftLeft();
        if (baseVol <= 0) {
            target = setting;
            return;
        }
        float vol = baseVol * profile.driftScale();       // round 31: a pack's drift_multiplier moves the setting more / less
        switch (profile.drift()) {
            case WALK -> {
                if (runTicks % period(100) == 0) target += (float) rand.nextGaussian() * 2.2F * vol;
            }
            case OSCILLATE -> {
                if (runTicks % period(2400) == 0) oscCentre = Mth.clamp(oscCentre + (float) rand.nextGaussian() * 6F * driftLeft(), 25F, 75F);
                target = oscCentre + 18F * driftLeft() * (float) Math.sin(runTicks / (6000.0 * PERIOD[Mth.clamp(stabiliser, 0, 4)] / Math.max(0.05F, vol)) * Math.PI * 2);
            }
            case DECAY -> {
                target -= 0.0012F * vol / PERIOD[Mth.clamp(stabiliser, 0, 4)];
                if (target < 8F) target = 85F + rand.nextFloat() * 10F;   // the cycle resets: retune upward
            }
            case JUMP -> {
                if (runTicks % period(100) == 0 && rand.nextFloat() < 0.02F * vol) target = 10F + rand.nextFloat() * 80F;
            }
        }
        target = Mth.clamp(target, 0F, 100F);
        // containment wear: about 4 hours of running at overclock 0, in tune, normal volatility
        float wear = 100F / 288000F * baseVol / Math.max(0.05F, driftLeft()) * profile.wearScale() * wearLeft() * (1F + overclock * 0.75F) * (efficiency() < 0.5F ? 1.6F : 1F) * (float) (double) QHConfig.TUNING_WEAR.get();
        integrity -= wear;
        if (autoCalibrator) {
            if (runTicks % 40 == 0) setting += Mth.clamp(target - setting, -1F, 1F);
            integrity = Math.min(100F, integrity + wear * 1.5F + SELF_REPAIR);
        }
        if (integrity <= 0F) {
            integrity = 0F;
            faulted = true;
        }
    }

    /**
     * Containment wear for a machine run by its physics model (the dial is gone; the physics says how hard it is
     * being pushed). {@code stress} is 1 when every margin holds, 4 when the machine is run outside a safe limit.
     */
    public void tickPhysics(@javax.annotation.Nullable TuningProfile profile, boolean running, float stress) {
        if (revealTicks > 0) revealTicks--;
        if (!running || faulted) return;
        runTicks++;
        if (perfect()) {
            integrity = 100F;
            return;
        }
        float vol = (profile == null ? 0.5F : profile.volatility()) * QHConfig.TUNING_DIFFICULTY.get().floatValue();
        if (vol <= 0F) return;                      // tuning turned off in the config: no wear either
        float wear = 100F / 288000F * Math.max(0.2F, vol) * (profile == null ? 1F : profile.wearScale()) * wearLeft() * (1F + overclock * 0.75F) * stress * (float) (double) QHConfig.TUNING_WEAR.get();
        integrity -= wear;
        if (autoCalibrator) integrity = Math.min(100F, integrity + wear * 1.5F + SELF_REPAIR);
        if (integrity <= 0F) {
            integrity = 0F;
            faulted = true;
        }
    }

    /** A sudden blow to the containment (a magnet quench, a plasma disruption). */
    public void hit(float percent) {
        if (faulted || perfect()) return;
        integrity -= percent * wearLeft() * (float) (double) QHConfig.TUNING_WEAR.get();
        if (integrity <= 0F) {
            integrity = 0F;
            faulted = true;
        }
    }

    public void calibrate() {
        integrity = 100F;
        faulted = false;
        revealTicks = 600;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putFloat("Setting", setting);
        t.putFloat("Target", target);
        t.putFloat("Integrity", integrity);
        t.putInt("Overclock", overclock);
        t.putInt("Parallel", parallel);
        t.putBoolean("Auto", autoCalibrator);
        t.putInt("Stabiliser", stabiliser);
        t.putBoolean("Fault", faulted);
        t.putBoolean("Omega", omega);
        t.putInt("Reveal", revealTicks);
        t.putLong("Run", runTicks);
        t.putFloat("Osc", oscCentre);
        return t;
    }

    public void load(CompoundTag t) {
        if (t.isEmpty()) return;
        setting = t.getFloat("Setting");
        target = t.getFloat("Target");
        integrity = t.getFloat("Integrity");
        overclock = t.getInt("Overclock");
        parallel = Math.max(1, t.getInt("Parallel"));
        autoCalibrator = t.getBoolean("Auto");
        stabiliser = t.getInt("Stabiliser");
        faulted = t.getBoolean("Fault");
        omega = t.getBoolean("Omega");
        revealTicks = t.getInt("Reveal");
        runTicks = t.getLong("Run");
        oscCentre = t.contains("Osc") ? t.getFloat("Osc") : 50F;
    }
}
