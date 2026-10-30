package com.theoreticalsciences.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-machine numbers a modpack can change, from {@code data/<any namespace>/ts_machines/<machine id>.json}.
 * The file name is the machine's id (the block id without "theoreticalsciences:", e.g. {@code quark_gluon_collider.json}).
 * Every field is optional; a missing field keeps the mod's own value:
 * <pre>
 * {
 *   "energy_capacity": 500000000,    // FE the machine's buffer holds (it still grows on its own for a hungry recipe)
 *   "max_energy_input": 1000000,     // FE/t it accepts from cables (default: no limit)
 *   "max_energy_output": 2000000,    // generators: FE/t it pushes out (default: the mod's own)
 *   "speed_multiplier": 2.0,         // recipes take half as long
 *   "energy_multiplier": 1.5,        // recipes draw 1.5x the FE per tick (generators: make 1.5x)
 *   "max_parallel": 8,               // tuned multiblocks: parallel recipes before the emitter tier multiplies it
 *   "max_overclock": 4,              // tuned multiblocks: overclock levels (each = 2x speed, 4x power)
 *   "coil_speed_bonus": 0.5,         // multiblocks: extra speed per coil tier above 1 (the mod's own is 0.25)
 *   "integrity_wear_multiplier": 0.5,// tuned multiblocks: containment wears half as fast
 *   "drift_multiplier": 0.5,         // tuned multiblocks: the ideal setting / disturbance moves half as much
 *   "dimensions": ["theoreticalsciences:quantum_foam"]  // round 35: only works in these dimensions ([] = anywhere)
 * }
 * </pre>
 * Applied on /reload (machines already in the world pick it up within a tick). Sent to clients so tooltips and
 * screens show the same numbers. These stack with the config file's multipliers.
 */
public final class MachineParams extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final MachineParams INSTANCE = new MachineParams();

    /** The values one file sets ({@code null} / NaN = not set). */
    public record Params(@Nullable Long energyCapacity, @Nullable Long maxInput, @Nullable Long maxOutput, double speed, double energy,
                         @Nullable Integer maxParallel, @Nullable Integer maxOverclock, @Nullable Double coilSpeedBonus, double wear, double drift,
                         @Nullable List<String> dimensions) {
        public static final Params NONE = new Params(null, null, null, 1, 1, null, null, null, 1, 1, null);
    }

    private static volatile Map<MachineType, Params> params = Map.of();
    private static volatile String syncJson = "{}";

    private MachineParams() {
        super(GSON, "ts_machines");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject all = new JsonObject();
        new TreeMap<>(map).forEach((id, json) -> all.add(id.toString(), json));
        load(all);
    }

    public static void load(JsonObject all) {
        Map<MachineType, Params> m = new EnumMap<>(MachineType.class);
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(e.getKey());
            String path = id == null ? e.getKey() : id.getPath();
            path = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            MachineType type = null;
            for (MachineType t : MachineType.values()) if (t.id().equals(path)) type = t;
            if (type == null) {
                TheoreticalSciences.LOGGER.warn("ts_machines file {}: there is no machine called {}", e.getKey(), path);
                continue;
            }
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                Params p = new Params(lng(o, "energy_capacity", 1), lng(o, "max_energy_input", 0), lng(o, "max_energy_output", 0),
                        dbl(o, "speed_multiplier", 1, 0.0001, 10000), dbl(o, "energy_multiplier", 1, 0, 10000),
                        integer(o, "max_parallel", 1, 4096), integer(o, "max_overclock", 0, 16),
                        o.has("coil_speed_bonus") ? dbl(o, "coil_speed_bonus", 0.25, 0, 1000) : null,
                        dbl(o, "integrity_wear_multiplier", 1, 0, 1000), dbl(o, "drift_multiplier", 1, 0, 1000), strings(o, "dimensions"));
                // two packs editing the same machine: the later file wins field by field
                Params old = m.get(type);
                m.put(type, old == null ? p : merge(old, p, o));
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad ts_machines file {}: {}", e.getKey(), ex.getMessage());
            }
        }
        params = Map.copyOf(m);
        syncJson = all.toString();
        if (!m.isEmpty()) TheoreticalSciences.LOGGER.info("Loaded custom parameters for {} machines", m.size());
        PackData.changed();
    }

    private static Params merge(Params a, Params b, JsonObject bj) {
        return new Params(b.energyCapacity() != null ? b.energyCapacity() : a.energyCapacity(), b.maxInput() != null ? b.maxInput() : a.maxInput(),
                b.maxOutput() != null ? b.maxOutput() : a.maxOutput(), bj.has("speed_multiplier") ? b.speed() : a.speed(),
                bj.has("energy_multiplier") ? b.energy() : a.energy(), b.maxParallel() != null ? b.maxParallel() : a.maxParallel(),
                b.maxOverclock() != null ? b.maxOverclock() : a.maxOverclock(), b.coilSpeedBonus() != null ? b.coilSpeedBonus() : a.coilSpeedBonus(),
                bj.has("integrity_wear_multiplier") ? b.wear() : a.wear(), bj.has("drift_multiplier") ? b.drift() : a.drift(),
                b.dimensions() != null ? b.dimensions() : a.dimensions());
    }

    /** Round 35: "dimensions": ["ns:dim", ...] (or one string); [] = anywhere. Null when the file does not say. */
    @Nullable
    private static List<String> strings(JsonObject o, String k) {
        if (!o.has(k)) return null;
        JsonElement e = o.get(k);
        if (e.isJsonPrimitive()) return List.of(e.getAsString());
        List<String> out = new java.util.ArrayList<>();
        for (JsonElement x : e.getAsJsonArray()) out.add(x.getAsString());
        return List.copyOf(out);
    }

    @Nullable
    private static Long lng(JsonObject o, String k, long min) {
        if (!o.has(k)) return null;
        return Math.max(min, Math.min(Long.MAX_VALUE / 4, o.get(k).getAsLong()));
    }

    @Nullable
    private static Integer integer(JsonObject o, String k, int min, int max) {
        if (!o.has(k)) return null;
        return Math.max(min, Math.min(max, o.get(k).getAsInt()));
    }

    private static double dbl(JsonObject o, String k, double def, double min, double max) {
        if (!o.has(k)) return def;
        double v = o.get(k).getAsDouble();
        return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : def;
    }

    public static String syncJson() {
        return syncJson;
    }

    public static void clear() {
        params = Map.of();
        syncJson = "{}";
        PackData.changed();
    }

    /** What the pack set for this machine ({@link Params#NONE} if nothing). */
    public static Params get(MachineType type) {
        Params p = params.get(type);
        return p == null ? Params.NONE : p;
    }

    /** The machine's energy buffer size: the pack's value, or the mod's own. */
    public static long energyCapacity(MachineType type) {
        Long c = get(type).energyCapacity();
        return c != null ? c : type.energyCapacity;
    }

    /** Coil speed bonus per tier above the first (the mod's own: +25%). */
    public static double coilSpeedBonus(MachineType type) {
        Double d = get(type).coilSpeedBonus();
        return d != null ? d : 0.25;
    }
}
