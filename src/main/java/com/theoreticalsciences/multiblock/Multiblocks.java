package com.theoreticalsciences.multiblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineType;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Multiblock shapes ship inside the mod jar (not datapacks) so that the server, the client,
 * JEI and the in-world hologram always agree on them without any syncing.
 */
public final class Multiblocks {
    private static Map<String, MultiblockDefinition> cache;

    public static synchronized Map<String, MultiblockDefinition> all() {
        if (cache == null) {
            Map<String, MultiblockDefinition> map = new LinkedHashMap<>();
            for (MachineType t : MachineType.values()) {
                if (!t.isMultiblock()) continue;
                String path = "/assets/theoreticalsciences/multiblocks/" + t.multiblock + ".json";
                try (InputStream in = open(path)) {
                    if (in == null) {
                        TheoreticalSciences.LOGGER.error("Missing multiblock definition {}", path);
                        continue;
                    }
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    map.put(t.multiblock, MultiblockDefinition.parse(t.multiblock, json));
                } catch (Exception e) {
                    TheoreticalSciences.LOGGER.error("Failed to load multiblock {}", path, e);
                }
            }
            cache = Collections.unmodifiableMap(map);
        }
        return cache;
    }

    private static InputStream open(String path) {
        InputStream in = Multiblocks.class.getResourceAsStream(path);
        if (in == null) in = Multiblocks.class.getClassLoader().getResourceAsStream(path.substring(1));
        if (in == null) in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1));
        return in;
    }

    public static Optional<MultiblockDefinition> get(String id) {
        return Optional.ofNullable(all().get(id));
    }

    private Multiblocks() {}
}
