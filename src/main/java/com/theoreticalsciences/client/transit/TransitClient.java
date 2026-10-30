package com.theoreticalsciences.client.transit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.transit.TransitPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The player's side of a trip between dimensions. It runs a little timeline:
 * <pre>
 *   server says "trip starts, jump in 35 ticks"  ->  the scene builds up over the screen (0 -> 1)
 *   the jump happens                              ->  the game opens its loading page; ours replaces it
 *   the new dimension is ready                    ->  the page closes and the scene fades away over 15 ticks
 * </pre>
 * Times are measured on the real clock (milliseconds), so the animation stays smooth even while the game is busy
 * loading the new world.
 */
public final class TransitClient {
    /** 15 ticks of fade-out after arriving. */
    private static final long FADE_MS = 750;
    /** If the loading page never shows (the jump failed silently), give up holding the full scene after this long. */
    private static final long HOLD_MS = 4000;
    /** A trip the server told us about is "fresh" for this long; the loading page then uses its style. */
    private static final long FRESH_MS = 15000;

    private static final long EPOCH = System.currentTimeMillis();

    private static TransitStyle style;
    private static boolean leaving;
    private static long startMs, buildMs;
    private static long fadeStartMs = -1;
    private static float fadeFrom = 1;
    private static boolean pageShown;

    /** Seconds since the game started (wrapped every hour to keep the numbers small), for the animations. */
    public static float seconds() {
        return ((System.currentTimeMillis() - EPOCH) % 3_600_000L) / 1000F;
    }

    // ------------------------------------------------------------------ from the server

    public static void receive(TransitPayload p) {
        TransitStyle s = TransitStyle.of(p.style());
        if (s == null) return;
        long now = System.currentTimeMillis();
        if (p.ticks() == TransitPayload.CANCEL) {
            if (style != null && fadeStartMs < 0) beginFade(now, intensity(now));
            return;
        }
        if (p.ticks() == TransitPayload.NOW && style == s && leaving == p.leaving() && fadeStartMs < 0) return;   // already running
        style = s;
        leaving = p.leaving();
        startMs = now;
        buildMs = p.ticks() * 50L;     // 0 = show the full scene straight away
        fadeStartMs = -1;
        pageShown = false;
    }

    private static void beginFade(long now, float from) {
        fadeStartMs = now;
        fadeFrom = from;
    }

    /** How much of the scene to show right now (0..1); also ends the trip when it has fully faded. */
    private static float intensity(long now) {
        if (style == null) return 0;
        if (fadeStartMs >= 0) {
            float f = (now - fadeStartMs) / (float) FADE_MS;
            if (f >= 1) {
                style = null;
                return 0;
            }
            return fadeFrom * (1 - TransitScenes.smooth(f));
        }
        if (buildMs <= 0) return 1;
        float p = (now - startMs) / (float) buildMs;
        if (p >= 1 && !pageShown && now - startMs - buildMs > HOLD_MS) {
            beginFade(now, 1);
            return 1;
        }
        // ease in: slow at first, then gathering pace into the jump
        return TransitScenes.clamp(p * p * (2 - p));
    }

    // ------------------------------------------------------------------ the overlay (drawn over the game, under menus)

    private static void renderLayer(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (style == null || mc.screen instanceof TransitLoadingScreen) return;
        float p = intensity(System.currentTimeMillis());
        if (p <= 0.001F || style == null) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);   // above everything else on the HUD
        TransitScenes.draw(g, style, g.guiWidth(), g.guiHeight(), seconds(), p, leaving);
        g.pose().popPose();
    }

    public static void registerLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(TheoreticalSciences.id("transit"), TransitClient::renderLayer);
    }

    // ------------------------------------------------------------------ the loading page

    /**
     * NeoForge lets a mod choose the loading page shown when arriving in, or leaving, a dimension. For each of ours we
     * register our page; it is used for every way in or out (items, bridges, falling through the void, the arena...).
     */
    public static void registerScreens(RegisterDimensionTransitionScreenEvent e) {
        for (TransitStyle s : TransitStyle.values()) {
            ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, TheoreticalSciences.id(s.id));
            e.registerIncomingEffect(dim, (levelReady, reason) -> page(levelReady, reason, s, false));
            e.registerOutgoingEffect(dim, (levelReady, reason) -> page(levelReady, reason, s, true));
        }
    }

    /** Builds our page. If the server just told us about this trip, its style wins (it knows the whole story). */
    private static ReceivingLevelScreen page(java.util.function.BooleanSupplier levelReady, ReceivingLevelScreen.Reason reason, TransitStyle s, boolean out) {
        long now = System.currentTimeMillis();
        boolean fresh = style != null && fadeStartMs < 0 && now - startMs < FRESH_MS;
        if (!fresh) {
            style = s;
            leaving = out;
            startMs = now;
            buildMs = 0;
            fadeStartMs = -1;
        }
        pageShown = true;
        return new TransitLoadingScreen(levelReady, reason, style, leaving);
    }

    /** The loading page has closed: we have arrived. Fade the scene away over the new world. */
    static void arrived(TransitStyle s, boolean out) {
        style = s;
        leaving = out;
        beginFade(System.currentTimeMillis(), 1);
    }

    /** Disconnecting: forget everything. */
    public static void reset() {
        style = null;
        fadeStartMs = -1;
        pageShown = false;
    }

    /** For tests and the cinematic code: is a trip animation on screen? */
    public static boolean active() {
        return style != null;
    }

    private TransitClient() {}
}
