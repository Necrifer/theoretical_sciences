package com.theoreticalsciences.machine.physics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The physics of one multiblock: what the operator sets (parameters), what follows from them (derived quantities, with
 * real equations), what must hold for the machine to survive (constraints), how fast it works (rate) and how much heat
 * it dumps into its coolant. Loaded from data/&lt;ns&gt;/ts_physics/&lt;machine&gt;.json. Every recipe has a
 * reachable operating point.
 * <p>
 * Every expression may use these built-in names:
 * <ul>
 *   <li>{@code T} machine (cold-mass) temperature in K, {@code T_c} coolant temperature, {@code T_amb} ambient (300 K)</li>
 *   <li>{@code coil}, {@code emitter} - component tiers of the formed structure</li>
 *   <li>{@code sc_Tc}, {@code sc_B0} - critical temperature (K) and upper critical field at 0 K (T) of the coil material</li>
 *   <li>{@code oc}, {@code par}, {@code thr} - overclock, parallels and throughput (2^oc x par)</li>
 *   <li>{@code d} - the disturbance (-1..1): field errors, drifting phases, impurities - what the machine fights against</li>
 *   <li>{@code on} - 1 while a recipe is running, else 0</li>
 * </ul>
 */
public final class PhysicsModel {
    public static final String[] BUILTINS = {"T", "T_c", "T_amb", "coil", "emitter", "sc_Tc", "sc_B0", "oc", "par", "thr", "d", "on"};
    public static final int B_T = 0, B_TC = 1, B_TAMB = 2, B_COIL = 3, B_EMIT = 4, B_SCTC = 5, B_SCB0 = 6, B_OC = 7, B_PAR = 8, B_THR = 9, B_D = 10, B_ON = 11;

    public enum Kind {
        /** the machine simply cannot work outside it (the beam is lost, the plasma will not ignite) */
        STOP,
        /** it works, but wears its containment four times as fast */
        WEAR,
        /** a sudden failure: superconductors go normal, stored energy dumps as heat, the containment takes a hit */
        QUENCH,
        /** a sustained failure: the structure overheats and loses integrity every second until fixed */
        DAMAGE
    }

    public record Param(String id, String unit, double min, double max, double step, double def, String bind, int slot) {}

    public record Named(String id, String unit, String formula, Expr expr, int slot, boolean show) {}

    public record State(String id, String unit, Expr init, Expr ddt, double min, double max, int slot, Kind outOfRange) {}

    public record Constraint(String id, Expr expr, Kind kind, String formula) {}

    public final String machine;
    public final List<Param> params = new ArrayList<>();
    public final Map<String, Double> recipeVars = new LinkedHashMap<>();
    public final Map<String, Integer> recipeVarSlots = new LinkedHashMap<>();
    public final List<Named> consts = new ArrayList<>();
    public final List<State> states = new ArrayList<>();
    public final List<Named> derived = new ArrayList<>();
    public final List<Constraint> constraints = new ArrayList<>();
    public final Expr rate, heat, idleHeat, quenchEnergy;
    public final String rateFormula;
    public final double kappa, heatCapacity, timescale, rateMax;
    public final Map<String, Integer> slots = new LinkedHashMap<>();
    public final int size;

    public PhysicsModel(String machine, JsonObject j) {
        this.machine = machine;
        for (String b : BUILTINS) slots.put(b, slots.size());
        JsonArray ps = j.getAsJsonArray("params");
        if (ps != null) for (JsonElement e : ps) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            int s = add(id);
            double lo = o.get("min").getAsDouble(), hi = o.get("max").getAsDouble(), step = o.has("step") ? o.get("step").getAsDouble() : 1.0,
                    def = o.get("default").getAsDouble();
            // round 35: a dial must have a real range, so the screen, the machine and Laplace's Demon can all use it
            if (!Double.isFinite(lo) || !Double.isFinite(hi) || lo > hi)
                throw new IllegalArgumentException("parameter " + id + ": min and max must be numbers with min <= max");
            if (!Double.isFinite(step) || step < 0) throw new IllegalArgumentException("parameter " + id + ": step must be 0 or more");
            if (!Double.isFinite(def)) throw new IllegalArgumentException("parameter " + id + ": default must be a number");
            params.add(new Param(id, str(o, "unit"), lo, hi, step, Math.max(lo, Math.min(hi, def)), str(o, "bind"), s));
        }
        if (j.has("recipe_vars")) for (Map.Entry<String, JsonElement> e : j.getAsJsonObject("recipe_vars").entrySet()) {
            recipeVars.put(e.getKey(), e.getValue().getAsDouble());
            recipeVarSlots.put(e.getKey(), add(e.getKey()));
        }
        JsonArray st = j.getAsJsonArray("states");
        List<JsonObject> stJson = new ArrayList<>();
        if (st != null) for (JsonElement e : st) {
            stJson.add(e.getAsJsonObject());
            add(e.getAsJsonObject().get("id").getAsString());
        }
        JsonArray cs = j.getAsJsonArray("consts");
        List<JsonObject> csJson = new ArrayList<>();
        if (cs != null) for (JsonElement e : cs) {
            csJson.add(e.getAsJsonObject());
            add(e.getAsJsonObject().get("id").getAsString());
        }
        JsonArray ds = j.getAsJsonArray("derived");
        List<JsonObject> dsJson = new ArrayList<>();
        if (ds != null) for (JsonElement e : ds) {
            dsJson.add(e.getAsJsonObject());
            add(e.getAsJsonObject().get("id").getAsString());
        }
        // every name is known now: compile
        for (JsonObject o : csJson) consts.add(named(o, false));
        for (JsonObject o : stJson) {
            String id = o.get("id").getAsString();
            states.add(new State(id, str(o, "unit"), Expr.parse(o.get("init").getAsString(), slots), Expr.parse(o.get("ddt").getAsString(), slots),
                    o.has("min") ? o.get("min").getAsDouble() : -Double.MAX_VALUE, o.has("max") ? o.get("max").getAsDouble() : Double.MAX_VALUE,
                    slots.get(id), o.has("out_of_range") ? Kind.valueOf(o.get("out_of_range").getAsString().toUpperCase(java.util.Locale.ROOT)) : Kind.STOP));
        }
        for (JsonObject o : dsJson) derived.add(named(o, !o.has("show") || o.get("show").getAsBoolean()));
        JsonArray cn = j.getAsJsonArray("constraints");
        if (cn != null) for (JsonElement e : cn) {
            JsonObject o = e.getAsJsonObject();
            constraints.add(new Constraint(o.get("id").getAsString(), Expr.parse(o.get("expr").getAsString(), slots),
                    Kind.valueOf(o.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT)), str(o, "formula")));
        }
        rate = Expr.parse(j.has("rate") ? j.get("rate").getAsString() : "1", slots);
        rateFormula = str(j, "rate_formula");
        heat = Expr.parse(j.has("heat") ? j.get("heat").getAsString() : "0", slots);
        idleHeat = Expr.parse(j.has("idle_heat") ? j.get("idle_heat").getAsString() : "0", slots);
        quenchEnergy = Expr.parse(j.has("quench_energy") ? j.get("quench_energy").getAsString() : "0", slots);
        kappa = j.has("kappa") ? j.get("kappa").getAsDouble() : 40;
        heatCapacity = j.has("heat_capacity") ? j.get("heat_capacity").getAsDouble() : 40;
        timescale = j.has("timescale") ? j.get("timescale").getAsDouble() : 1;
        rateMax = j.has("rate_max") ? j.get("rate_max").getAsDouble() : 4;
        size = slots.size();
        // round 35: numbers the simulation divides by or caps with must make sense
        if (!(kappa > 0) || !Double.isFinite(kappa)) throw new IllegalArgumentException("kappa must be a positive number");
        if (!(heatCapacity > 0) || !Double.isFinite(heatCapacity)) throw new IllegalArgumentException("heat_capacity must be a positive number");
        if (!(timescale > 0) || !Double.isFinite(timescale)) throw new IllegalArgumentException("timescale must be a positive number");
        if (!(rateMax >= 0) || !Double.isFinite(rateMax)) throw new IllegalArgumentException("rate_max must be 0 or more");
        for (State st2 : states)
            if (st2.min() > st2.max()) throw new IllegalArgumentException("state " + st2.id() + ": min must be <= max");
    }

    private int add(String id) {
        if (slots.containsKey(id)) throw new IllegalArgumentException("duplicate name " + id + " in physics model " + machine);
        slots.put(id, slots.size());
        return slots.get(id);
    }

    private Named named(JsonObject o, boolean show) {
        String id = o.get("id").getAsString();
        return new Named(id, str(o, "unit"), str(o, "formula"), Expr.parse(o.get("expr").getAsString(), slots), slots.get(id), show);
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    public int paramIndex(String id) {
        for (int i = 0; i < params.size(); i++) if (params.get(i).id().equals(id)) return i;
        return -1;
    }

    public int derivedIndex(String id) {
        for (int i = 0; i < derived.size(); i++) if (derived.get(i).id().equals(id)) return i;
        return -1;
    }

    /** Value of any name in an evaluated environment (for recipe windows). NaN if unknown. */
    public double value(double[] env, String id) {
        Integer s = slots.get(id);
        return s == null ? Double.NaN : env[s];
    }

    /**
     * Critical temperature (K) and upper critical field at 0 K (T) of each coil tier's conductor.
     * Round 31: a datapack coil tier (data/&lt;ns&gt;/ts_component_tiers) may set its own "critical_temperature" /
     * "critical_field"; a tier above 5 that does not is extrapolated (each tier 4x the one below), so a pack's new
     * coils are always better superconductors than the mod's best.
     */
    public static double scTc(int coil) {
        Double d = com.theoreticalsciences.custom.ComponentTiers.criticalTemperature(coil);
        if (d != null) return d;
        return switch (coil) {
            case 1 -> 9.2;      // Nb-Ti
            case 2 -> 18.3;     // Nb3Sn
            case 3 -> 92.0;     // REBCO tape
            case 4 -> 290.0;    // neutronium hydride: a room-temperature superconductor
            case 5 -> 4000.0;   // transfinite lattice
            default -> coil > 5 ? 4000.0 * Math.pow(4, Math.min(coil - 5, 200)) : 1e-9;    // above 5: extrapolated; 0 = no superconductor
        };
    }

    public static double scB0(int coil) {
        Double d = com.theoreticalsciences.custom.ComponentTiers.criticalField(coil);
        if (d != null) return d;
        return switch (coil) {
            case 1 -> 14.5;
            case 2 -> 30.0;
            case 3 -> 120.0;
            case 4 -> 400.0;
            case 5 -> 5000.0;
            default -> coil > 5 ? 5000.0 * Math.pow(4, Math.min(coil - 5, 200)) : 0.0;
        };
    }
}
