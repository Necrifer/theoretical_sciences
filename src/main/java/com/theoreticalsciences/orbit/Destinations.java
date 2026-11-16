package com.theoreticalsciences.orbit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 1.3.0: where the Orbital Station's Transfer Console can send you, read from {@code data/<ns>/ts_destinations/*.json},
 * so a pack or an addon can add planets:
 * <pre>
 * { "dimension": "theoreticalsciences:titan", "name": "destination.theoreticalsciences.titan",
 *   "research": "theoreticalsciences:titan",       (optional: needed to go there)
 *   "mod": "ad_astra",                              (optional: only offered when that mod is installed)
 *   "landing_y": 80, "platform": "theoreticalsciences:titan_ice", "slow_fall": true, "order": 20,
 *   "delta_v": 8000, "synodic_days": 12.6, "penalty": 0.8 }   (1.12.0: what the Mission Planner charges; see MissionMath)
 * </pre>
 * A destination whose dimension does not exist on the server is skipped. {@code {"remove": true}} drops one.
 */
public final class Destinations extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final Destinations INSTANCE = new Destinations();

    public record Destination(ResourceLocation id, ResourceKey<Level> dimension, String name, @Nullable ResourceLocation research,
                              @Nullable String mod, int landingY, @Nullable Block platform, boolean slowFall, int order,
                              double deltaV, double synodic, double penalty) {
        /** The Overworld, and any destination with no Δv: always free (the way home must be). */
        public boolean home() {
            return deltaV <= 0 || dimension == Level.OVERWORLD;
        }

        public double offset() {
            return MissionMath.offset(id.toString(), synodic);
        }

        public boolean offered(ServerPlayer sp) {
            if (mod != null && !ModList.get().isLoaded(mod)) return false;
            if (sp.server.getLevel(dimension) == null) return false;
            return research == null || ResearchService.has(sp, research);
        }
    }

    private List<Destination> all = List.of();

    private Destinations() {
        super(GSON, "ts_destinations");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        List<Destination> out = new ArrayList<>();
        map.forEach((id, el) -> {
            try {
                JsonObject o = el.getAsJsonObject();
                if (GsonHelper.getAsBoolean(o, "remove", false)) return;
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(GsonHelper.getAsString(o, "dimension")));
                String research = GsonHelper.getAsString(o, "research", null);
                String platform = GsonHelper.getAsString(o, "platform", null);
                Block pb = platform == null ? null : BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(platform)).orElse(null);
                out.add(new Destination(id, dim, GsonHelper.getAsString(o, "name", "destination." + id.getNamespace() + "." + id.getPath()),
                        research == null ? null : ResourceLocation.parse(research), GsonHelper.getAsString(o, "mod", null),
                        GsonHelper.getAsInt(o, "landing_y", 100), pb, GsonHelper.getAsBoolean(o, "slow_fall", false),
                        GsonHelper.getAsInt(o, "order", 100), GsonHelper.getAsDouble(o, "delta_v", 0), GsonHelper.getAsDouble(o, "synodic_days", 0),
                        GsonHelper.getAsDouble(o, "penalty", 0)));
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad orbital destination {}: {}", id, ex.getMessage());
            }
        });
        out.sort(Comparator.comparingInt(Destination::order).thenComparing(d -> d.id().toString()));
        all = List.copyOf(out);
        TheoreticalSciences.LOGGER.info("Loaded {} orbital destinations", out.size());
    }

    /** The destination in this dimension, if any (for the Return Beacon). */
    @Nullable
    public Destination forDimension(ResourceKey<Level> dim) {
        for (Destination d : all) if (d.dimension().equals(dim)) return d;
        return null;
    }

    @Nullable
    public Destination byId(ResourceLocation id) {
        for (Destination d : all) if (d.id().equals(id)) return d;
        return null;
    }

    public List<Destination> all() {
        return all;
    }

    /** The destinations this player may go to now, in order. */
    public List<Destination> offered(ServerPlayer sp) {
        List<Destination> out = new ArrayList<>();
        for (Destination d : all) if (d.offered(sp)) out.add(d);
        return out;
    }
}
