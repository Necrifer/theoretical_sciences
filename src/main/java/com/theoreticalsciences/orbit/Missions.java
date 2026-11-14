package com.theoreticalsciences.orbit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 1.12.0: where a Mission Bay can send a Sample Return Probe, from {@code data/<ns>/ts_missions/*.json}:
 * <pre>
 * { "name": "mission.theoreticalsciences.mars", "delta_v": 10500, "synodic_days": 26, "penalty": 0.9, "transfer_days": 259,
 *   "research": "theoreticalsciences:sample_return", "mod_absent": "ad_astra",   (only offered without that mod)
 *   "mod": "...",                                                                  (only offered with that mod)
 *   "samples": [{"item": "theoreticalsciences:martian_regolith", "count": 8}], "order": 30 }
 * </pre>
 * {@code {"remove": true}} drops one.
 */
public final class Missions extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final Missions INSTANCE = new Missions();

    public record Mission(ResourceLocation id, String name, double deltaV, double synodic, double penalty, double transferDays,
                          @Nullable ResourceLocation research, @Nullable String mod, @Nullable String modAbsent, List<ItemStack> samples, int order) {
        public boolean offered(ServerPlayer sp) {
            if (mod != null && !ModList.get().isLoaded(mod)) return false;
            if (modAbsent != null && ModList.get().isLoaded(modAbsent)) return false;
            return research == null || ResearchService.has(sp, research);
        }

        public double offset() {
            return MissionMath.offset(id.toString(), synodic);
        }
    }

    private List<Mission> all = List.of();

    private Missions() {
        super(GSON, "ts_missions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller profiler) {
        List<Mission> out = new ArrayList<>();
        map.forEach((id, el) -> {
            try {
                JsonObject o = el.getAsJsonObject();
                if (GsonHelper.getAsBoolean(o, "remove", false)) return;
                List<ItemStack> samples = new ArrayList<>();
                for (JsonElement s : GsonHelper.getAsJsonArray(o, "samples", new com.google.gson.JsonArray())) {
                    JsonObject so = s.getAsJsonObject();
                    BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(GsonHelper.getAsString(so, "item")))
                            .ifPresent(it -> samples.add(new ItemStack(it, GsonHelper.getAsInt(so, "count", 1))));
                }
                String research = GsonHelper.getAsString(o, "research", null);
                out.add(new Mission(id, GsonHelper.getAsString(o, "name", "mission." + id.getNamespace() + "." + id.getPath()),
                        GsonHelper.getAsDouble(o, "delta_v", 5000), GsonHelper.getAsDouble(o, "synodic_days", 0), GsonHelper.getAsDouble(o, "penalty", 0),
                        GsonHelper.getAsDouble(o, "transfer_days", 100), research == null ? null : ResourceLocation.parse(research),
                        GsonHelper.getAsString(o, "mod", null), GsonHelper.getAsString(o, "mod_absent", null), List.copyOf(samples),
                        GsonHelper.getAsInt(o, "order", 100)));
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Bad mission {}: {}", id, ex.getMessage());
            }
        });
        out.sort(Comparator.comparingInt(Mission::order).thenComparing(m -> m.id().toString()));
        all = List.copyOf(out);
        TheoreticalSciences.LOGGER.info("Loaded {} sample-return missions", out.size());
    }

    public List<Mission> offered(ServerPlayer sp) {
        List<Mission> out = new ArrayList<>();
        for (Mission m : all) if (m.offered(sp)) out.add(m);
        return out;
    }

    @Nullable
    public Mission byId(ResourceLocation id) {
        for (Mission m : all) if (m.id().equals(id)) return m;
        return null;
    }
}
