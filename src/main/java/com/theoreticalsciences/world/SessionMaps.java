package com.theoreticalsciences.world;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Round 36: the small per-player maps the mod keeps in memory (cooldowns, passes, last safe ground, sent states...)
 * forget a player when they log out, and everything is cleared when the server stops - so nothing carries over from
 * one singleplayer world into the next, and nothing grows over a long server run.
 */
@EventBusSubscriber(modid = com.theoreticalsciences.TheoreticalSciences.MODID)
public final class SessionMaps {
    private static final List<Map<?, ?>> PER_PLAYER = new CopyOnWriteArrayList<>();
    private static final List<Object> PER_SERVER = new CopyOnWriteArrayList<>();

    /** A map keyed by player UUID: the entry goes when the player logs out. */
    public static <K, V, M extends Map<K, V>> M perPlayer(M map) {
        PER_PLAYER.add(map);
        return map;
    }

    /** A map or set that only lives as long as the server. */
    public static <T> T perServer(T mapOrCollection) {
        PER_SERVER.add(mapOrCollection);
        return mapOrCollection;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        var id = e.getEntity().getUUID();
        for (Map<?, ?> m : PER_PLAYER) synchronized (m) {
            m.remove(id);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        for (Map<?, ?> m : PER_PLAYER) synchronized (m) {
            m.clear();
        }
        for (Object o : PER_SERVER) synchronized (o) {
            if (o instanceof Map<?, ?> m) m.clear();
            else if (o instanceof Collection<?> c) c.clear();
        }
    }

    private SessionMaps() {}
}
