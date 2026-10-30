package com.theoreticalsciences.machine.physics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the machine physics models (data/&lt;ns&gt;/ts_physics/&lt;machine&gt;.json) and the coolant table
 * (ts_physics/coolants.json). The same JSON is sent to clients so the machine screens can show names, units and formulas.
 */
public class PhysicsManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final PhysicsManager INSTANCE = new PhysicsManager();

    /**
     * A coolant: the temperature it holds the cold mass at, how much heat each mB carries away (kJ, including the
     * cryoplant's recirculation), and what comes back out warm (empty = it is vented).
     */
    public record Coolant(double temperature, double capacity, String returns) {}

    // server and client keep separate stores: in single player both live in one JVM, and the client's copy (rebuilt
    // whenever the sync payload arrives) must never replace the instances the server's machines are running on
    private static Map<String, PhysicsModel> models = Map.of(), clientModels = Map.of();
    private static Map<String, Coolant> coolants = Map.of(), clientCoolants = Map.of();
    private String syncJson = "{}";

    private PhysicsManager() {
        super(GSON, "ts_physics");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        JsonObject sync = new JsonObject();
        map.forEach((id, json) -> {
            if (json.isJsonObject()) sync.add(id.getPath(), json);
        });
        syncJson = GSON.toJson(sync);
        load(sync, false);
    }

    public String syncJson() {
        return syncJson;
    }

    /** Parses the whole set (on the server after a reload, on the client from the sync payload). */
    public static void load(JsonObject all, boolean client) {
        Map<String, PhysicsModel> m = new HashMap<>();
        Map<String, Coolant> c = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            try {
                if (e.getKey().equals("coolants")) {
                    for (Map.Entry<String, JsonElement> f : e.getValue().getAsJsonObject().entrySet()) {
                        JsonObject o = f.getValue().getAsJsonObject();
                        double cap = o.get("capacity").getAsDouble();
                        if (!(cap > 0) || Double.isInfinite(cap)) throw new IllegalArgumentException("coolant " + f.getKey() + " needs a positive capacity");
                        c.put(f.getKey(), new Coolant(o.get("temperature").getAsDouble(), cap, o.has("returns") ? o.get("returns").getAsString() : ""));
                    }
                } else {
                    JsonObject o = e.getValue().getAsJsonObject();
                    // round 35: {"remove": true} takes the machine's physics away (it runs like a plain tuned machine)
                    if (o.has("remove") && o.get("remove").getAsBoolean()) continue;
                    m.put(e.getKey(), new PhysicsModel(e.getKey(), o));
                }
            } catch (RuntimeException | StackOverflowError ex) {      // (an absurdly deep expression: rejected, not fatal)
                TheoreticalSciences.LOGGER.error("Bad machine physics model {}: {}", e.getKey(), ex.toString());
            }
        }
        if (client) {
            clientModels = Map.copyOf(m);
            clientCoolants = Map.copyOf(c);
            return;
        }
        models = Map.copyOf(m);
        coolants = Map.copyOf(c);
        TheoreticalSciences.LOGGER.info("Loaded {} machine physics models and {} coolants", m.size(), c.size());
    }

    public static void loadClient(String json) {
        try {
            load(JsonParser.parseString(json).getAsJsonObject(), true);
        } catch (RuntimeException e) {
            TheoreticalSciences.LOGGER.error("Bad physics sync: {}", e.getMessage());
        }
    }

    /** The server's model (or, on a client with no server here, the synced one). */
    @Nullable
    public static PhysicsModel model(String machine) {
        PhysicsModel m = models.get(machine);
        return m != null ? m : clientModels.get(machine);
    }

    @Nullable
    public static PhysicsModel model(String machine, boolean client) {
        return client ? clientModels.get(machine) : models.get(machine);
    }

    @Nullable
    public static Coolant coolant(Fluid f) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f);
        Coolant c = coolants.get(id.toString());
        return c != null ? c : clientCoolants.get(id.toString());
    }

    /** Every coolant the server knows (fluid id -> properties). */
    public static Map<String, Coolant> coolants() {
        return coolants;
    }

    @Nullable
    public static Coolant coolant(FluidStack s) {
        return s.isEmpty() ? null : coolant(s.getFluid());
    }
}
