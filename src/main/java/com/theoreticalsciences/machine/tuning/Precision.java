package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.machine.physics.PhysicsModel;
import com.theoreticalsciences.machine.physics.RecipePhysics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.Tag;

import java.util.List;

/**
 * 1.2.0: how finely a machine is tuned, and what that earns. The Tuning Fork only gets a machine working (it leaves it a
 * little off its best); the console is for the fine work. The best is found once per recipe (by the same search Laplace's
 * Demon uses, with the coolant the machine holds): the fastest point inside every recipe window. What precision measures
 * is how close the machine's readout is to the readout at that point: each window's quantity, and the speed. A quantity
 * may go past its ideal in the safe direction (further inside a one-sided window, or faster) without losing anything.
 * These targets are listed in the console's Readout; at full precision each cycle makes half as much again of every
 * product.
 * <p>
 * Older multiblocks without physics: precision is how close the tuning dial sits on its (drifting) target.
 */
public final class Precision {
    /** Extra products per cycle at full precision. */
    public static final double MAX_BONUS = 0.5;
    /** A target's tolerance, as a share of the room its window gives (or of its value): one bell width. */
    static final double WIDTH = 0.02;

    /** 0..1, synced to the console. */
    public double value;
    double[] ideal;
    RecipePhysics idealFor;
    public boolean busy;
    public long lastTry = -10_000;
    /** The targets (each recipe window's quantity at the best point, then the speed), which way is safe, and a width. */
    double[] targetQ, scale;
    int[] dir;
    /** Client: the targets for the recipe running now, as last sent (null: not known yet), and their directions. */
    public double[] target;
    public int[] targetDir;

    public double bonus() {
        return MAX_BONUS * value;
    }

    public boolean knows(RecipePhysics r) {
        return ideal != null && idealFor == r && targetQ != null;
    }

    /** params: the best settings found; q: the readout there (see {@link #quantities}). */
    public void set(RecipePhysics r, double[] params, double[] q) {
        idealFor = r;
        ideal = params == null ? null : params.clone();
        targetQ = q == null ? null : q.clone();
        if (q == null) return;
        var ws = r.windows();
        scale = new double[q.length];
        dir = new int[q.length];
        for (int i = 0; i < q.length; i++) {
            if (i < ws.size()) {
                var w = ws.get(i);
                boolean lo = w.min() > -1e300, hi = w.max() < 1e300;
                if (lo && hi) {
                    dir[i] = 0;                                    // a band: be at the value
                    scale[i] = Math.max(WIDTH * (w.max() - w.min()), WIDTH * Math.abs(q[i]));
                } else if (hi) {
                    dir[i] = -1;                                   // at most: the value, or lower
                    scale[i] = WIDTH * Math.max(Math.abs(w.max() - q[i]), Math.abs(w.max()));
                } else {
                    dir[i] = 1;                                    // at least: the value, or higher
                    scale[i] = WIDTH * Math.max(Math.abs(q[i] - w.min()), Math.abs(w.min()));
                }
            } else {
                dir[i] = 1;                                        // the speed: as fast, or faster
                scale[i] = WIDTH * Math.abs(q[i]);
            }
            if (!(scale[i] > 1e-12)) scale[i] = 1e-12;
        }
    }

    /** The ideal settings for r, or null (the Governor steers toward them). */
    public double[] ideal(RecipePhysics r) {
        return knows(r) ? ideal : null;
    }

    public double[] targets(RecipePhysics r) {
        return knows(r) ? targetQ : null;
    }

    public int[] dirs(RecipePhysics r) {
        return knows(r) ? dir : null;
    }

    /** The readout precision is measured on: each recipe window's quantity, then the speed. */
    public static double[] quantities(PhysicsModel m, com.theoreticalsciences.machine.physics.PhysicsState ps) {
        var ws = ps.recipe.windows();
        double[] q = new double[ws.size() + 1];
        for (int i = 0; i < ws.size(); i++) q[i] = m.value(ps.env, ws.get(i).q());
        q[ws.size()] = ps.rate;
        return q;
    }

    /** How close the readout is to the targets: a bell, one width per target (a Stability Margin widens it). */
    public double of(double[] now, double widen) {
        if (targetQ == null || now == null || now.length != targetQ.length) return 0;
        double d2 = 0;
        for (int i = 0; i < now.length; i++) {
            double off = now[i] - targetQ[i];
            if (dir[i] < 0) off = Math.max(0, off);
            else if (dir[i] > 0) off = Math.max(0, -off);
            if (!Double.isFinite(off)) return 0;
            double d = off / (scale[i] * widen);
            d2 += d * d;
        }
        return Math.exp(-0.5 * d2 / now.length);
    }

    /** Whether one target is met (within half a width, or past it in the safe direction): the readout ticks it green. */
    public static boolean met(double now, double target, int dir, double tolerance) {
        double off = now - target;
        if (dir < 0) off = Math.max(0, off);
        else if (dir > 0) off = Math.max(0, -off);
        return Math.abs(off) <= tolerance;
    }

    /** Older multiblocks: the dial (0..100) against its target, one width = 0.4. */
    public static double ofDial(float setting, float target, double widen) {
        double d = (setting - target) / (0.4 * widen);
        return Math.exp(-0.5 * d * d);
    }

    /** Client: the tolerance the readout uses for target i (sent with the targets). */
    public double[] targetScale;

    public CompoundTag save() {
        return save((RecipePhysics) null);
    }

    /** r: the recipe running now; its targets are sent so the console's readout can list what to tune for. */
    public CompoundTag save(RecipePhysics r) {
        CompoundTag t = new CompoundTag();
        t.putDouble("Value", value);
        if (r != null && knows(r)) {
            t.put("Targets", list(targetQ));
            t.put("Scale", list(scale));
            t.putIntArray("Dir", dir);
        }
        return t;
    }

    public void load(CompoundTag t) {
        value = t.getDouble("Value");
        target = t.contains("Targets") ? array(t.getList("Targets", Tag.TAG_DOUBLE)) : null;
        targetScale = t.contains("Scale") ? array(t.getList("Scale", Tag.TAG_DOUBLE)) : null;
        targetDir = t.contains("Dir") ? t.getIntArray("Dir") : null;
    }

    static ListTag list(double[] v) {
        ListTag l = new ListTag();
        for (double d : v) l.add(DoubleTag.valueOf(d));
        return l;
    }

    static double[] array(ListTag l) {
        double[] v = new double[l.size()];
        for (int i = 0; i < v.length; i++) v[i] = l.getDouble(i);
        return v;
    }

    @SuppressWarnings("unused")
    private static final int LIST = Tag.TAG_DOUBLE;
}
