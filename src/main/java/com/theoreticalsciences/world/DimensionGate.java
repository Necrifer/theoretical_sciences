package com.theoreticalsciences.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Round 35: the Transcendental and the Living Proof can only be entered the intended way - the Transcendental through
 * the Omnilex Engine's impossible recipe (the collapse), the Living Proof through Alpha Omega's challenge. Neither
 * builds itself for a visitor who arrives any other way (a command, another mod's teleporter), so such a visitor is
 * told why and sent back to the Overworld, creative players included.
 * <p>
 * The mod's own ways in call {@link #allow} just before moving a player; that one-time pass is spent on arrival.
 */
public final class DimensionGate {
    private static final Set<ResourceKey<Level>> GUARDED = Set.of(QHDimensions.TRANSCENDENTAL, QHDimensions.LIVING_PROOF);
    private static final Map<UUID, ResourceKey<Level>> PASSES = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());
    /** Players to send back to the Overworld on their next tick (never from inside a dimension change). */
    private static final Map<UUID, ResourceKey<Level>> KICK = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());

    /** The intended way in: let this player into {@code dim} once. */
    public static void allow(ServerPlayer sp, ResourceKey<Level> dim) {
        PASSES.put(sp.getUUID(), dim);
    }

    /** Before the trip: no pass, no trip - and the player is told why (and sent to the Overworld if elsewhere). */
    public static void onTravel(EntityTravelToDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !GUARDED.contains(e.getDimension())) return;
        if (e.getDimension().equals(PASSES.get(sp.getUUID()))) return;      // the intended way: spent on arrival
        e.setCanceled(true);
        refuse(sp, e.getDimension());
        if (sp.level().dimension() != Level.OVERWORLD) KICK.put(sp.getUUID(), e.getDimension());
    }

    /** After the trip: anything that slipped past the first check (moved without the usual event) is sent back too. */
    public static void onArrived(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        ResourceKey<Level> to = e.getTo();
        if (!GUARDED.contains(to)) return;
        if (to.equals(PASSES.remove(sp.getUUID()))) return;
        refuse(sp, to);
        KICK.put(sp.getUUID(), to);
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || KICK.isEmpty()) return;
        if (KICK.remove(sp.getUUID()) == null) return;
        ServerLevel ow = sp.server.overworld();
        BlockPos spawn = ow.getSharedSpawnPos();
        BlockPos dest = SafeLanding.find(ow, spawn.getX(), spawn.getZ(), spawn.getY(), null, null);
        sp.changeDimension(new DimensionTransition(ow, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(), DimensionTransition.DO_NOTHING));
    }

    /** After the mod's own move (whether or not it went through): no pass is left lying around. */
    public static void revoke(ServerPlayer sp) {
        PASSES.remove(sp.getUUID());
    }

    public static void forget(UUID id) {
        PASSES.remove(id);
        KICK.remove(id);
    }

    private static void refuse(ServerPlayer sp, ResourceKey<Level> dim) {
        String k = dim == QHDimensions.LIVING_PROOF ? "living_proof" : "transcendental";
        sp.sendSystemMessage(Component.translatable("message.theoreticalsciences.gate." + k).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private DimensionGate() {}
}
