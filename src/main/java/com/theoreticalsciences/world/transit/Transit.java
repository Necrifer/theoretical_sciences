package com.theoreticalsciences.world.transit;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The "transit" scheduler: instead of teleporting a player between dimensions the instant they use an item, we
 * <ol>
 *   <li>tell their game to start the travel animation for the destination ({@link TransitPayload}),</li>
 *   <li>wait {@link #DELAY} ticks (1.75 seconds) while the animation builds up on their screen,</li>
 *   <li>then run the original teleport code, unchanged (the "jump").</li>
 * </ol>
 * While a player is waiting they cannot start a second trip. The trip is called off if they die, log out, or end up
 * in another dimension some other way before the jump.
 *
 * <p>It also watches every dimension change (even ones that do not go through here, like falling out of the bottom of
 * a dimension, or being pulled into the Living Proof) and, when one of the mod's dimensions is involved, tells the
 * player's game at that moment so it can show the matching loading page.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class Transit {
    /** How long the animation plays before the jump: 35 ticks = 1.75 seconds. */
    public static final int DELAY = 35;

    /** One player's trip that is waiting for its jump. */
    private record Pending(ResourceKey<Level> from, long due, ResourceKey<Level> style, boolean leaving, Consumer<ServerPlayer> jump) {}

    /** Waiting trips, by player. */
    private static final Map<UUID, Pending> PENDING = com.theoreticalsciences.world.SessionMaps.perPlayer(new HashMap<>());
    /** Players whose jump is running right now (so the dimension-change watcher below does not report it twice). */
    private static final Map<UUID, Pending> JUMPING = com.theoreticalsciences.world.SessionMaps.perPlayer(new HashMap<>());

    /** True while this player has a trip waiting to happen (items should refuse to start another one). */
    public static boolean busy(ServerPlayer sp) {
        return PENDING.containsKey(sp.getUUID());
    }

    /**
     * Starts a trip. {@code style} is the dimension whose animation to play (the destination, or the one being left
     * when {@code leaving}). {@code jump} is the original teleport code; it runs after {@link #DELAY} ticks with the
     * same player. Returns false (and does nothing) if the player already has a trip waiting.
     */
    public static boolean start(ServerPlayer sp, ResourceKey<Level> style, boolean leaving, Consumer<ServerPlayer> jump) {
        if (busy(sp)) return false;
        PENDING.put(sp.getUUID(), new Pending(sp.level().dimension(), sp.server.getTickCount() + DELAY, style, leaving, jump));
        PacketDistributor.sendToPlayer(sp, new TransitPayload(style.location(), leaving, DELAY));
        // a rising hum that everyone nearby can hear while the traveller "charges up"
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.35F, 1.8F);
        return true;
    }

    /** True if the dimension belongs to this mod (only those get the special animations and loading pages). */
    public static boolean ours(ResourceKey<Level> dim) {
        return dim != null && dim.location().getNamespace().equals(TheoreticalSciences.MODID);
    }

    // ------------------------------------------------------------------ the clock

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        MinecraftServer server = e.getServer();
        long now = server.getTickCount();
        for (Map.Entry<UUID, Pending> en : new ArrayList<>(PENDING.entrySet())) {
            UUID id = en.getKey();
            Pending p = en.getValue();
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp == null) {                     // logged out: nothing to tell anyone
                PENDING.remove(id);
                continue;
            }
            if (!sp.isAlive() || sp.level().dimension() != p.from()) {
                cancel(sp);                       // died, or already somewhere else
                continue;
            }
            if (now < p.due()) continue;
            PENDING.remove(id);
            JUMPING.put(id, p);
            try {
                p.jump().accept(sp);
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Dimension transit for {} failed", sp.getGameProfile().getName(), ex);
                PacketDistributor.sendToPlayer(sp, new TransitPayload(p.style().location(), p.leaving(), TransitPayload.CANCEL));
            } finally {
                JUMPING.remove(id);
            }
        }
    }

    /**
     * Calls off a waiting trip (or one whose jump code decided not to go after all) and tells the player's game to
     * fade the animation away.
     */
    public static void cancel(ServerPlayer sp) {
        Pending p = PENDING.remove(sp.getUUID());
        if (p == null) p = JUMPING.get(sp.getUUID());
        if (p != null) PacketDistributor.sendToPlayer(sp, new TransitPayload(p.style().location(), p.leaving(), TransitPayload.CANCEL));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        PENDING.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        PENDING.clear();
        JUMPING.clear();
    }

    // ------------------------------------------------------------------ every other way in or out

    /**
     * Fired by NeoForge just before any player changes dimension (for any reason, including other mods and commands).
     * If one of our dimensions is involved, the player's game is told "this is happening now", so the loading page
     * and the arrival fade match the dimension. Runs last, and only if nobody cancelled the trip.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        ResourceKey<Level> from = sp.level().dimension(), to = e.getDimension();
        if (from == to || JUMPING.containsKey(sp.getUUID())) return;
        // moving some other way while a trip was waiting: that trip is off (its payload is replaced by this one)
        PENDING.remove(sp.getUUID());
        ResourceKey<Level> style;
        boolean leaving;
        if (ours(to)) {
            style = to;
            leaving = false;
        } else if (ours(from)) {
            style = from;
            leaving = true;
        } else return;
        PacketDistributor.sendToPlayer(sp, new TransitPayload(style.location(), leaving, TransitPayload.NOW));
    }

    private Transit() {}
}
