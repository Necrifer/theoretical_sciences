package com.theoreticalsciences.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * 1.2.0: which hidden (moving) casing blocks a formed controller is drawing right now, per world, on the server.
 * A casing that is hidden but claimed by no controller (its machine changed shape in an update, its controller was
 * lost, the structure was taken apart some other way) shows itself again on its next random tick, so no block is ever
 * left invisible.
 */
public final class MovingClaims {
    private static final Map<Level, Map<Long, Object>> CLAIMS = new WeakHashMap<>();

    public static synchronized void claim(Level level, Object owner, Collection<Long> positions) {
        Map<Long, Object> m = CLAIMS.computeIfAbsent(level, k -> new HashMap<>());
        for (long p : positions) m.put(p, owner);
    }

    public static synchronized void release(Level level, Object owner) {
        Map<Long, Object> m = CLAIMS.get(level);
        if (m != null) m.values().removeIf(o -> o == owner);
    }

    public static synchronized boolean claimed(Level level, BlockPos pos) {
        Map<Long, Object> m = CLAIMS.get(level);
        return m != null && m.containsKey(pos.asLong());
    }

    private MovingClaims() {}
}
