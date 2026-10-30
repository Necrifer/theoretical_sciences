package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.transcend.ForgeDrift;
import com.theoreticalsciences.transcend.ForgeLaws;

import java.util.HashMap;
import java.util.Map;

/** Round 36: the Forge's drifting laws and re-seeding waits, as the server last told this client. */
public final class ForgeDriftClient {
    private static final Map<String, Integer> EPOCH = new HashMap<>();
    private static final Map<String, Long> READY = new HashMap<>();

    public static void accept(String data) {
        long now = System.currentTimeMillis();
        for (String part : data.split(",")) {
            String[] f = part.split(":");
            if (f.length < 3) continue;
            try {
                EPOCH.put(f[0], Integer.parseInt(f[1]));
                READY.put(f[0], now + Integer.parseInt(f[2]) * 1000L);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static int epoch(String id) {
        return ForgeDrift.drifts(id) ? EPOCH.getOrDefault(id, 0) : 0;
    }

    public static ForgeLaws.Problem problem(String id) {
        return ForgeLaws.problem(id, epoch(id));
    }

    /** Seconds until the recipe can be compiled again (0: now). */
    public static int secondsLeft(String id) {
        long t = READY.getOrDefault(id, 0L) - System.currentTimeMillis();
        return t <= 0 ? 0 : (int) ((t + 999) / 1000);
    }

    private ForgeDriftClient() {}
}
