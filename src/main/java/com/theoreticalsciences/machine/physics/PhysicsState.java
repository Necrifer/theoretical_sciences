package com.theoreticalsciences.machine.physics;

import com.theoreticalsciences.machine.tuning.TuningProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The live physics of one multiblock: the operator's settings, the machine's internal state (temperature, black-hole
 * mass, spin...), its coolant loop, and the result of the last evaluation. Runs on the server; a copy is synced to
 * the client for the screen.
 */
public final class PhysicsState {
    public static final int COOLANT_CAPACITY = 16000;
    public static final double DT = 0.05;              // seconds per tick
    public static final int QUENCH_RECOVERY = 160;     // ticks the magnets need to recover after a quench

    public final String machine;
    @Nullable private PhysicsModel model;
    public double[] params = new double[0];
    public double[] states = new double[0];
    private boolean statesInit;
    public double temperature = 300;
    public double disturbance;
    private long runTicks;
    private double oscCentre;
    public int quenchTicks;
    public int quenchCount;

    public final FluidTank coolant;
    public final FluidTank spent;

    // ---- last evaluation (also what the client shows)
    public double[] env = new double[0];
    public double[] margins = new double[0];
    public double rate = 1, heatKw, coolingKw, coolantFlow;
    public RecipePhysics recipe = RecipePhysics.NONE;
    /** the first violated constraint by kind, -1 = none */
    public int stopIdx = -1, quenchIdx = -1, damageIdx = -1, wearIdx = -1;
    /** the first recipe window not met, -1 = none */
    public int windowIdx = -1;
    /** set when a state variable left its allowed range */
    public int stateIdx = -1;

    private final java.util.function.BooleanSupplier clientSide;
    /** saved values read before the model was available (client before the sync arrives): applied once it is */
    @Nullable private CompoundTag pending;

    public PhysicsState(String machine, Runnable onChange, java.util.function.BooleanSupplier clientSide) {
        this.machine = machine;
        this.clientSide = clientSide;
        this.coolant = new FluidTank(COOLANT_CAPACITY, s -> PhysicsManager.coolant(s) != null) {
            @Override
            protected void onContentsChanged() {
                onChange.run();
            }
        };
        this.spent = new FluidTank(COOLANT_CAPACITY) {
            @Override
            protected void onContentsChanged() {
                onChange.run();
            }
        };
        model();
    }

    /** The model (the client's own synced copy on the client), re-fetched after a reload: values carried over by name. */
    @Nullable
    public PhysicsModel model() {
        PhysicsModel m = PhysicsManager.model(machine, clientSide.getAsBoolean());
        if (m != model) {
            PhysicsModel old = model;
            double[] np = new double[m == null ? 0 : m.params.size()];
            for (int i = 0; i < np.length; i++) {
                PhysicsModel.Param p = m.params.get(i);
                int o = old == null ? -1 : old.paramIndex(p.id());
                np[i] = o >= 0 && o < params.length ? Mth.clamp(params[o], p.min(), p.max()) : p.def();
            }
            double[] ns = new double[m == null ? 0 : m.states.size()];
            boolean all = old != null;
            for (int i = 0; i < ns.length; i++) {
                int o = -1;
                if (old != null) for (int k = 0; k < old.states.size(); k++) if (old.states.get(k).id().equals(m.states.get(i).id())) o = k;
                if (o >= 0 && o < states.length) ns[i] = states[o];
                else all = false;
            }
            if (!all) statesInit = false;
            params = np;
            states = ns;
            model = m;
            env = new double[m == null ? 0 : m.size];
            margins = new double[m == null ? 0 : m.constraints.size()];
            if (m != null && pending != null) {
                CompoundTag t = pending;
                pending = null;
                readValues(t);
            }
        }
        return model;
    }

    public void setParam(int i, double v) {
        PhysicsModel m = model();
        if (m == null || i < 0 || i >= params.length) return;
        PhysicsModel.Param p = m.params.get(i);
        if (Double.isNaN(v)) return;
        v = Mth.clamp(v, p.min(), p.max());
        if (p.step() > 0) v = Math.round(v / p.step()) * p.step();
        params[i] = Mth.clamp(v, p.min(), p.max());
    }

    // ------------------------------------------------------------------ evaluation
    /**
     * Evaluates the whole model for the given structure. Returns false if the model is missing.
     */
    public boolean evaluate(int coil, int emitter, int oc, int par, boolean on, boolean autoCal) {
        // round 35: a pack's model that fails in an unexpected way stops this machine (with one log line) instead of
        // taking the server down with it
        try {
            broken = false;
            return evaluate0(coil, emitter, Math.max(0, Math.min(oc, 30)), par, on, autoCal);
        } catch (RuntimeException ex) {
            broken = true;
            rate = 0;
            heatKw = 0;
            if (!warned) {
                warned = true;
                com.theoreticalsciences.TheoreticalSciences.LOGGER.error("Machine physics model {} failed to evaluate - the machine stops until the model is fixed", machine, ex);
            }
            return true;
        }
    }

    /** True while the model cannot be evaluated (see {@link #evaluate}). */
    private boolean broken, warned;

    private boolean evaluate0(int coil, int emitter, int oc, int par, boolean on, boolean autoCal) {
        PhysicsModel m = model();
        if (m == null) return false;
        double[] e = env;
        e[PhysicsModel.B_T] = temperature;
        PhysicsManager.Coolant cl = PhysicsManager.coolant(coolant.getFluid());
        e[PhysicsModel.B_TC] = cl == null ? 300 : cl.temperature();
        e[PhysicsModel.B_TAMB] = 300;
        e[PhysicsModel.B_COIL] = coil;
        e[PhysicsModel.B_EMIT] = emitter;
        e[PhysicsModel.B_SCTC] = PhysicsModel.scTc(coil);
        e[PhysicsModel.B_SCB0] = PhysicsModel.scB0(coil);
        e[PhysicsModel.B_OC] = oc;
        e[PhysicsModel.B_PAR] = par;
        e[PhysicsModel.B_THR] = (1 << oc) * par;
        e[PhysicsModel.B_D] = autoCal ? disturbance * 0.3 : disturbance;
        e[PhysicsModel.B_ON] = on ? 1 : 0;
        for (int i = 0; i < params.length; i++) e[m.params.get(i).slot()] = params[i];
        m.recipeVars.forEach((k, v) -> e[m.recipeVarSlots.get(k)] = recipe.vars().getOrDefault(k, v));
        for (PhysicsModel.Named c : m.consts) e[c.slot()] = c.expr().eval(e);
        if (!statesInit) {
            for (int i = 0; i < states.length; i++) states[i] = m.states.get(i).init().eval(e);
            statesInit = true;
        }
        for (int i = 0; i < states.length; i++) e[m.states.get(i).slot()] = states[i];
        for (PhysicsModel.Named d : m.derived) e[d.slot()] = d.expr().eval(e);
        stopIdx = quenchIdx = damageIdx = wearIdx = -1;
        for (int i = 0; i < m.constraints.size(); i++) {
            PhysicsModel.Constraint c = m.constraints.get(i);
            double v = c.expr().eval(e);
            margins[i] = v;
            if (v >= 0) continue;                              // NaN counts as broken too
            switch (c.kind()) {
                case STOP -> stopIdx = stopIdx < 0 ? i : stopIdx;
                case QUENCH -> quenchIdx = quenchIdx < 0 ? i : quenchIdx;
                case DAMAGE -> damageIdx = damageIdx < 0 ? i : damageIdx;
                case WEAR -> wearIdx = wearIdx < 0 ? i : wearIdx;
            }
        }
        double r = m.rate.eval(e);
        rate = Double.isFinite(r) ? Mth.clamp(r, 0, m.rateMax) : 0;
        double h = m.heat.eval(e) * (on ? 1 : 0) + m.idleHeat.eval(e);
        heatKw = Double.isFinite(h) ? Math.max(0, h) : 0;
        windowIdx = -1;
        for (int i = 0; i < recipe.windows().size(); i++) {
            RecipePhysics.Window w = recipe.windows().get(i);
            if (!w.test(m.value(e, w.q()))) {
                windowIdx = i;
                break;
            }
        }
        return true;
    }

    /** A hard condition keeps the machine from running at all. */
    public boolean stopped() {
        return broken || stopIdx >= 0 || stateIdx >= 0 && model != null && model.states.get(stateIdx).outOfRange() == PhysicsModel.Kind.STOP;
    }

    // ------------------------------------------------------------------ time evolution
    /** The disturbance the operator fights, moving the way the machine's drift profile says. */
    /** Round 25: the controller's stabiliser tier (see TuningState). */
    public int stabiliser;

    public void tickDisturbance(@Nullable TuningProfile p, boolean running, RandomSource rand, double difficulty) {
        if (!running) return;
        runTicks++;
        int st = Mth.clamp(stabiliser, 0, 4);
        if (st >= com.theoreticalsciences.machine.tuning.TuningState.PERFECT) {
            disturbance = 0;                         // held perfectly still
            return;
        }
        TuningProfile.Drift drift = p == null ? TuningProfile.Drift.WALK : p.drift();
        double vol = (p == null ? 0.5 : p.volatility() * p.driftScale()) * difficulty * com.theoreticalsciences.machine.tuning.TuningState.DRIFT[st];
        int per = Math.round(com.theoreticalsciences.machine.tuning.TuningState.PERIOD[st] * 100);
        if (vol <= 0) {
            disturbance = 0;
            return;
        }
        switch (drift) {
            case WALK -> {
                if (runTicks % per == 0) disturbance += rand.nextGaussian() * 0.12 * vol;
                disturbance *= 0.9995;                  // a slow pull back towards nominal
            }
            case OSCILLATE -> {
                if (runTicks % (per * 24) == 0) oscCentre = Mth.clamp(oscCentre + rand.nextGaussian() * 0.15 * com.theoreticalsciences.machine.tuning.TuningState.DRIFT[st], -0.4, 0.4);
                disturbance = oscCentre + 0.6 * com.theoreticalsciences.machine.tuning.TuningState.DRIFT[st] * Math.sin(runTicks / (60.0 * per / Math.max(0.05, vol)) * Math.PI * 2);
            }
            case DECAY -> {
                disturbance -= 0.00006 * vol * 100.0 / per;
                if (disturbance < -1) disturbance = 0.8 + rand.nextDouble() * 0.2;
            }
            case JUMP -> {
                if (runTicks % per == 0 && rand.nextFloat() < 0.02F * vol) disturbance = (rand.nextDouble() * 2 - 1) * com.theoreticalsciences.machine.tuning.TuningState.DRIFT[st];
            }
        }
        disturbance = Mth.clamp(disturbance, -1, 1);
    }

    /**
     * Heat flows from the cold mass into the coolant (or, with none, slowly into the air). The temperature relaxes
     * exponentially to its equilibrium, so the step is exact whatever the time constant.
     */
    public void tickThermal() {
        PhysicsModel m = model;
        if (m == null) return;
        PhysicsManager.Coolant cl = PhysicsManager.coolant(coolant.getFluid());
        boolean canReturn = cl != null && (cl.returns().isEmpty() || spent.getSpace() > 0
                && (spent.isEmpty() || spent.getFluid().is(returnFluid(cl))));
        double kappa, sink;
        if (cl != null && canReturn) {
            kappa = m.kappa;
            sink = cl.temperature();
        } else {
            kappa = m.kappa * 0.02;                     // stagnant: only the structure itself sheds heat
            sink = 300;
        }
        double tau = m.heatCapacity / kappa;
        double eq = sink + heatKw / kappa;
        double before = temperature;
        temperature = eq + (temperature - eq) * Math.exp(-DT / tau);
        // energy actually carried away this tick (kJ) = heat in - heat stored
        double carried = heatKw * DT - m.heatCapacity * (temperature - before);
        coolingKw = carried / DT;
        coolantFlow = 0;
        if (cl != null && canReturn && carried > 0) {
            double mb = Math.min(COOLANT_CAPACITY, carried / cl.capacity());
            int whole = (int) mb;
            carry += mb - whole;
            if (carry >= 1) {
                whole += (int) carry;
                carry -= (int) carry;
            }
            Fluid back = returnFluid(cl);
            if (back != null) whole = Math.min(whole, spent.getSpace());     // only what can come back out
            if (whole > 0) {
                FluidStack used = coolant.drain(whole, IFluidHandler.FluidAction.EXECUTE);
                if (back != null && !used.isEmpty()) spent.fill(new FluidStack(back, used.getAmount()), IFluidHandler.FluidAction.EXECUTE);
            }
            coolantFlow = mb / DT;                      // mB per second
        }
        if (!Double.isFinite(temperature)) temperature = 300;
        temperature = Math.max(0, temperature);
    }

    private double carry;

    @Nullable
    private static Fluid returnFluid(PhysicsManager.Coolant c) {
        if (c.returns().isEmpty()) return null;
        ResourceLocation id = ResourceLocation.tryParse(c.returns());
        return id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    /** Integrates the model's state variables (black-hole mass, spin, ...) while the machine runs. */
    public void tickStates(boolean running) {
        PhysicsModel m = model;
        stateIdx = -1;
        if (m == null) return;
        for (int i = 0; i < states.length; i++) {
            PhysicsModel.State s = m.states.get(i);
            if (running) {
                double dv = s.ddt().eval(env) * DT * m.timescale;
                if (Double.isFinite(dv)) states[i] += dv;
            }
            // pinned at (or past) a bound counts as out of range until it moves back in
            if (statesInit && (states[i] <= s.min() || states[i] >= s.max())) {
                if (stateIdx < 0) stateIdx = i;
                states[i] = Mth.clamp(states[i], s.min(), s.max());
            }
        }
    }

    /** A quench dumps the stored energy as heat and boils off coolant. */
    public void quench() {
        PhysicsModel m = model;
        quenchTicks = QUENCH_RECOVERY;
        quenchCount++;
        statesInit = false;           // whatever was held (a black hole, a burning plasma) is gone: it has to be formed anew
        if (m == null) return;
        double eq = m.quenchEnergy.eval(env);
        if (Double.isFinite(eq) && eq > 0) temperature += eq / m.heatCapacity;
        coolant.drain(Math.min(coolant.getFluidAmount(), 2000), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Laplace's Demon: the state values have been set by hand - use them as they are. */
    public void holdStates() {
        statesInit = true;
    }

    /** Laplace's Demon: form the states afresh from their initial values on the next evaluation. */
    public void resetStates() {
        statesInit = false;
    }

    /** After a calibration: nominal again. */
    public void calibrate() {
        disturbance = 0;
        quenchTicks = 0;
        statesInit = false;
    }

    // ------------------------------------------------------------------ persistence & sync
    public CompoundTag save(HolderLookup.Provider regs) {
        PhysicsModel m = model;
        // no model (yet): keep whatever was loaded, untouched, so nothing is lost
        CompoundTag t = m == null && pending != null ? pending.copy() : new CompoundTag();
        if (m != null) {
            CompoundTag p = new CompoundTag();
            for (int i = 0; i < params.length; i++) p.putDouble(m.params.get(i).id(), params[i]);
            t.put("Params", p);
            CompoundTag s = new CompoundTag();
            if (statesInit) for (int i = 0; i < states.length; i++) s.putDouble(m.states.get(i).id(), states[i]);
            t.put("States", s);
        }
        t.putDouble("T", temperature);
        t.putDouble("D", disturbance);
        t.putInt("Quench", quenchTicks);
        t.putInt("QuenchCount", quenchCount);
        t.put("Coolant", coolant.writeToNBT(regs, new CompoundTag()));
        t.put("Spent", spent.writeToNBT(regs, new CompoundTag()));
        // the readout
        t.putDouble("Rate", rate);
        t.putDouble("Heat", heatKw);
        t.putDouble("Cooling", coolingKw);
        t.putDouble("Flow", coolantFlow);
        ListTag ev = new ListTag();
        for (double v : env) ev.add(net.minecraft.nbt.DoubleTag.valueOf(v));
        t.put("Env", ev);
        ListTag mg = new ListTag();
        for (double v : margins) mg.add(net.minecraft.nbt.DoubleTag.valueOf(v));
        t.put("Margins", mg);
        t.putIntArray("Idx", new int[]{stopIdx, quenchIdx, damageIdx, wearIdx, windowIdx, stateIdx});
        CompoundTag vars = new CompoundTag();
        recipe.vars().forEach(vars::putDouble);
        t.put("Vars", vars);
        ListTag win = new ListTag();
        for (RecipePhysics.Window w : recipe.windows()) {
            CompoundTag c = new CompoundTag();
            c.putString("q", w.q());
            c.putDouble("min", w.min());
            c.putDouble("max", w.max());
            win.add(c);
        }
        t.put("Windows", win);
        return t;
    }

    public void load(CompoundTag t, HolderLookup.Provider regs) {
        PhysicsModel m = model();
        if (m == null) pending = t.copy();
        else readValues(t);
        temperature = t.contains("T") ? t.getDouble("T") : 300;
        disturbance = t.getDouble("D");
        quenchTicks = t.getInt("Quench");
        quenchCount = t.getInt("QuenchCount");
        coolant.readFromNBT(regs, t.getCompound("Coolant"));
        spent.readFromNBT(regs, t.getCompound("Spent"));
        rate = t.getDouble("Rate");
        heatKw = t.getDouble("Heat");
        coolingKw = t.getDouble("Cooling");
        coolantFlow = t.getDouble("Flow");
        int[] idx = t.getIntArray("Idx");
        if (idx.length == 6) {
            stopIdx = idx[0];
            quenchIdx = idx[1];
            damageIdx = idx[2];
            wearIdx = idx[3];
            windowIdx = idx[4];
            stateIdx = idx[5];
        }
        List<RecipePhysics.Window> w = new ArrayList<>();
        for (Tag x : t.getList("Windows", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            w.add(new RecipePhysics.Window(c.getString("q"), c.getDouble("min"), c.getDouble("max")));
        }
        java.util.Map<String, Double> vars = new java.util.LinkedHashMap<>();
        CompoundTag vt = t.getCompound("Vars");
        for (String k : vt.getAllKeys()) vars.put(k, vt.getDouble(k));
        recipe = w.isEmpty() && vars.isEmpty() ? RecipePhysics.NONE : new RecipePhysics(vars, w);
    }

    /** Parameters and state variables by name (needs the model). */
    private void readValues(CompoundTag t) {
        PhysicsModel m = model;
        if (m == null) return;
        CompoundTag p = t.getCompound("Params");
        for (int i = 0; i < params.length; i++) if (p.contains(m.params.get(i).id())) params[i] = p.getDouble(m.params.get(i).id());
        CompoundTag s = t.getCompound("States");
        if (!s.isEmpty()) {
            for (int i = 0; i < states.length; i++) if (s.contains(m.states.get(i).id())) states[i] = s.getDouble(m.states.get(i).id());
            statesInit = true;
        }
        ListTag ev = t.getList("Env", Tag.TAG_DOUBLE);
        if (ev.size() == env.length) for (int i = 0; i < env.length; i++) env[i] = ev.getDouble(i);
        ListTag mg = t.getList("Margins", Tag.TAG_DOUBLE);
        if (mg.size() == margins.length) for (int i = 0; i < margins.length; i++) margins[i] = mg.getDouble(i);
    }
}
