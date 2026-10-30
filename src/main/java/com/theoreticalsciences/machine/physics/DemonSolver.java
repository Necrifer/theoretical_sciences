package com.theoreticalsciences.machine.physics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Laplace's Demon's search (creative tuning), run off the server thread on a private copy of a machine's physics.
 * For every coolant (the machine's own first) it samples the controls at random, then searches locally - one or two
 * controls at a time, on several scales - for a point that breaks no limit, sits inside every recipe window and runs
 * as fast as possible. Machines with state variables (a plasma, a held black hole) are then scored at the steady state
 * those variables relax to, like the in-game machine reaches after running a while. The same method was used to
 * confirm that every recipe has a working point.
 */
public final class DemonSolver {
    public record Solution(boolean ok, double[] params, @Nullable Fluid coolant, double temperature, double rate) {}

    private final String machine;
    private final RecipePhysics recipe;
    private final int coil, emitter, oc, par;
    private final double[] start;
    @Nullable private final Fluid current;
    private final long seed;
    private PhysicsState ps;
    private PhysicsModel m;
    private double sink, kappa;

    public DemonSolver(String machine, RecipePhysics recipe, int coil, int emitter, int oc, int par, double[] start, @Nullable Fluid current, long seed) {
        this.machine = machine;
        this.recipe = recipe;
        this.coil = coil;
        this.emitter = emitter;
        this.oc = oc;
        this.par = par;
        this.start = start;
        this.current = current;
        this.seed = seed;
    }

    /** Round 35: the search gives up after this long and answers with the best it found (a pack's huge model). */
    private static final long BUDGET_NANOS = 15_000_000_000L;
    private long deadline;

    private boolean late() {
        return System.nanoTime() > deadline;
    }

    @Nullable
    public Solution solve() {
        deadline = System.nanoTime() + BUDGET_NANOS;
        ps = new PhysicsState(machine, () -> {}, () -> false);
        m = ps.model();
        if (m == null) return null;
        ps.recipe = recipe;
        RandomSource rnd = RandomSource.create(seed);
        List<Fluid> fluids = new ArrayList<>();
        if (current != null) fluids.add(current);
        for (String id : PhysicsManager.coolants().keySet()) {
            Fluid f = BuiltInRegistries.FLUID.getOptional(ResourceLocation.tryParse(id)).orElse(null);
            if (f != null && !fluids.contains(f)) fluids.add(f);
        }
        if (fluids.isEmpty()) fluids.add(null);
        int n = m.params.size();
        double[] best = start.length == n ? start.clone() : defaults();
        double bestScore = Double.NEGATIVE_INFINITY;
        Fluid bestFluid = current;
        for (Fluid fl : fluids) {
            setCoolant(fl);
            double[] loc = start.length == n ? start.clone() : defaults();
            double ls = score(loc, false);
            // 1) quick: random samples, then local moves
            for (int s = 0; s < 1000 && !late(); s++) {
                double[] c = random(rnd);
                double sc = score(c, false);
                if (sc > ls) {
                    ls = sc;
                    loc = c;
                }
            }
            for (int s = 0; s < 3000 && !late(); s++) {
                if (ls >= 1000 && s > 1500) break;
                double[] c = s % 4 == 0 ? random(rnd) : local(loc, rnd);
                double sc = score(c, false);
                if (sc > ls) {
                    ls = sc;
                    loc = c;
                }
            }
            // 2) with state variables: judge at the steady state they settle to
            if (!m.states.isEmpty()) {
                ls = score(loc, true);
                for (int s = 0; s < 600 && ls < 1000 && !late(); s++) {
                    double[] c = s % 4 == 0 ? random(rnd) : local(loc, rnd);
                    double sc = score(c, true);
                    if (sc > ls) {
                        ls = sc;
                        loc = c;
                    }
                }
            }
            if (ls > bestScore) {
                bestScore = ls;
                best = loc;
                bestFluid = fl;
            }
            if (bestScore >= 1000 || late()) break;
        }
        setCoolant(bestFluid);
        score(best, !m.states.isEmpty());
        return new Solution(bestScore >= 1000, best, bestFluid, ps.temperature, ps.rate);
    }

    private double[] defaults() {
        double[] d = new double[m.params.size()];
        for (int i = 0; i < d.length; i++) d[i] = m.params.get(i).def();
        return d;
    }

    private void setCoolant(@Nullable Fluid fl) {
        ps.coolant.setFluid(fl == null ? FluidStack.EMPTY : new FluidStack(fl, ps.coolant.getCapacity()));
        PhysicsManager.Coolant cl = PhysicsManager.coolant(ps.coolant.getFluid());
        sink = cl == null ? 300 : cl.temperature();
        kappa = cl == null ? m.kappa * 0.02 : m.kappa;
    }

    // ------------------------------------------------------------------ moves (always snapped to what a player can set)
    private double snap(PhysicsModel.Param p, double v) {
        v = Mth.clamp(v, p.min(), p.max());
        if (p.step() > 0) v = Math.round(v / p.step()) * p.step();
        return Mth.clamp(v, p.min(), p.max());
    }

    private static boolean wide(PhysicsModel.Param p) {
        double lo = p.min() > 0 ? p.min() : Math.max(p.step(), 1e-9);
        return p.max() / lo > 50;
    }

    private double[] random(RandomSource r) {
        double[] c = new double[m.params.size()];
        for (int i = 0; i < c.length; i++) {
            PhysicsModel.Param p = m.params.get(i);
            double lo = p.min() > 0 ? p.min() : Math.max(p.step(), 1e-9);
            if (wide(p) && r.nextDouble() < 0.7) c[i] = snap(p, Math.exp(Math.log(lo) + r.nextDouble() * (Math.log(p.max()) - Math.log(lo))));
            else c[i] = snap(p, p.min() + r.nextDouble() * (p.max() - p.min()));
        }
        return c;
    }

    private static final double[] LOG_SCALES = {0.02, 0.2, 1.0}, LIN_SCALES = {0.001, 0.01, 0.1};

    private double[] local(double[] from, RandomSource r) {
        double[] c = from.clone();
        int k = r.nextInt(3) == 0 ? 2 : 1;
        for (int j = 0; j < k; j++) {
            int i = r.nextInt(c.length);
            PhysicsModel.Param p = m.params.get(i);
            double v = c[i];
            if (wide(p) && v > 0 && r.nextDouble() < 0.6) v *= Math.exp(r.nextGaussian() * LOG_SCALES[r.nextInt(3)]);
            else v += r.nextGaussian() * (p.max() - p.min()) * LIN_SCALES[r.nextInt(3)];
            c[i] = snap(p, v);
        }
        return c;
    }

    // ------------------------------------------------------------------ judging a point
    /** 1000 + rate for a point that breaks nothing; below zero, the further off the worse. */
    private double score(double[] p, boolean steady) {
        for (int i = 0; i < p.length; i++) ps.params[i] = p[i];
        ps.disturbance = 0;
        double T = sink;
        int hit = 0;
        for (int it = 0; it < (steady ? 10 : 4); it++) {
            ps.temperature = T;
            ps.resetStates();
            ps.evaluate(coil, emitter, oc, par, true, false);
            if (steady) {
                hit = settle();
                ps.evaluate(coil, emitter, oc, par, true, false);
            }
            double T2 = sink + ps.heatKw / kappa;
            if (Math.abs(T2 - T) < 1e-6 * Math.max(1, T)) break;
            T = steady ? 0.5 * T + 0.5 * T2 : T2;
        }
        double bad = hit * 3;
        for (int i = 0; i < m.constraints.size(); i++) {
            double v = ps.margins[i];
            if (Double.isNaN(v)) bad += 10;
            else if (v < 0 && m.constraints.get(i).kind() != PhysicsModel.Kind.WEAR) bad += Math.min(10, 1 + -v / (1 - v));
        }
        for (RecipePhysics.Window w : recipe.windows()) {
            double v = m.value(ps.env, w.q());
            if (Double.isNaN(v)) bad += 10;
            else if (v < w.min()) bad += 1 + Math.min(5, (w.min() - v) / (Math.abs(w.min()) + 1e-30));
            else if (v > w.max()) bad += 1 + Math.min(5, (v - w.max()) / (Math.abs(w.max()) + 1e-30));
        }
        if (bad > 0) return -bad;
        if (ps.rate <= 0.001) return -0.5;
        return 1000 + ps.rate - (ps.wearIdx >= 0 ? 100 : 0);
    }

    /**
     * Relaxes each state variable (except an operator-held mass M) to the stable equilibrium reached from its initial
     * value, following the sign of its rate of change. Returns how many ended pinned at a bound.
     */
    private int settle() {
        double[] st = ps.states.clone();
        int hit = 0;
        for (int sweep = 0; sweep < 3; sweep++) {
            hit = 0;
            for (int i = 0; i < st.length; i++) {
                PhysicsModel.State sd = m.states.get(i);
                if (sd.id().equals("M")) continue;
                double lo = sd.min(), hi = sd.max(), x = st[i];
                double v = ddt(st, i, x);
                if (v == 0) continue;
                boolean up = v > 0;
                double a = x, b = x, step = Math.max(Math.abs(x) * 0.25, 1e-3);
                boolean found = false, pinned = false;
                for (int k = 0; k < 200; k++) {
                    b = up ? a + step : a - step;
                    if (b >= hi || b <= lo) {
                        b = Mth.clamp(b, lo, hi);
                        if ((ddt(st, i, b) > 0) == up) {
                            st[i] = b;
                            pinned = true;
                        } else found = true;
                        break;
                    }
                    if ((ddt(st, i, b) > 0) != up) {
                        found = true;
                        break;
                    }
                    a = b;
                    step *= 1.3;
                }
                if (pinned) hit++;
                if (found) {
                    for (int k = 0; k < 60; k++) {
                        double mid = 0.5 * (a + b);
                        if ((ddt(st, i, mid) > 0) == up) a = mid;
                        else b = mid;
                    }
                    st[i] = 0.5 * (a + b);
                }
            }
        }
        System.arraycopy(st, 0, ps.states, 0, st.length);
        ps.holdStates();
        return hit;
    }

    private double ddt(double[] st, int i, double x) {
        System.arraycopy(st, 0, ps.states, 0, st.length);
        ps.states[i] = x;
        ps.holdStates();
        ps.evaluate(coil, emitter, oc, par, true, false);
        double v = m.states.get(i).ddt().eval(ps.env);
        return Double.isNaN(v) ? 0 : v;
    }
}
