package com.theoreticalsciences.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Fired on {@code NeoForge.EVENT_BUS} after a player gains a research entry (by terminal, command or script). */
public class ResearchUnlockedEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation research;

    public ResearchUnlockedEvent(ServerPlayer player, ResourceLocation research) {
        this.player = player;
        this.research = research;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getResearch() {
        return research;
    }
}
