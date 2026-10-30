package com.theoreticalsciences.client;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

/**
 * The mod's own music player (round 23). Vanilla's music manager cuts a track off the moment the situation changes;
 * here a track fades in when it starts, fades out when you leave its dimension (or the boss appears, or dies), and the
 * next one fades in after it - never an abrupt start or stop. Vanilla music is kept silent in the mod's dimensions.
 */
public final class QHMusic {
    private static final int FADE_IN = 60, FADE_OUT = 80;         // ticks
    @Nullable
    private static Track current;
    private static String currentId;
    private static int delay, waiting;
    private static final RandomSource RNG = RandomSource.create();

    /** What should be playing now, or null (not in one of our dimensions). */
    @Nullable
    static String wanted() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        var box = mc.player.getBoundingBox().inflate(96);
        boolean boss = !mc.level.getEntitiesOfClass(com.theoreticalsciences.boss.AlphaOmegaEntity.class, box, a -> a.isAlive()).isEmpty()
                || !mc.level.getEntitiesOfClass(com.theoreticalsciences.boss.GenesisEntity.class, box, a -> true).isEmpty();
        if (boss) return "alpha_omega";
        var dim = mc.level.dimension().location();
        if (dim.getNamespace().equals(TheoreticalSciences.MODID) && QHSounds.MUSIC.containsKey(dim.getPath())) return dim.getPath();
        return null;
    }

    /** Whether we are somewhere the mod's music is in charge (vanilla must stay quiet). */
    public static boolean ours() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return lastWant != null || mc.level.dimension().location().getNamespace().equals(TheoreticalSciences.MODID);
    }

    @Nullable
    private static String lastWant;
    @Nullable
    private static Object lastLevel;
    private static int scan;

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.isPaused()) return;                    // sounds do not tick while paused: neither do we
        // a new level (dimension change, reload): the game itself has stopped every sound - start afresh, no pause
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            current = null;
            currentId = null;
            delay = 0;
            scan = 0;
        }
        if (mc.level == null) lastWant = null;
        else if (scan-- <= 0) {                       // (the boss search is not cheap: twice a second is plenty)
            lastWant = wanted();
            scan = 10;
        }
        String want = lastWant;
        if (current != null && !current.started && ++waiting > 200) {     // never got going (sound off, device busy...)
            mc.getSoundManager().stop(current);
            current = null;
            currentId = null;
            delay = 200;
        }
        if (current != null && !mc.getSoundManager().isActive(current) && current.started) {
            boolean ended = !current.fadingOut;
            current = null;
            currentId = null;
            // a track that played to its end: a pause before the next (the boss theme comes straight back)
            if (ended) delay = "alpha_omega".equals(want) ? 0 : 600 + RNG.nextInt(1800);
        }
        if (current != null && !java.util.Objects.equals(currentId, want)) current.fadeOut();
        if (current == null && want != null) {
            if (delay > 0 && !"alpha_omega".equals(want)) {
                delay--;
                return;
            }
            var holder = QHSounds.MUSIC.get(want);
            if (holder == null) return;
            current = new Track(holder.get());
            waiting = 0;
            currentId = want;
            mc.getSoundManager().play(current);
        }
    }

    /** Leaving the world: stop at once (there is nothing left to fade over). */
    public static void reset() {
        if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
        currentId = null;
        delay = 0;
    }

    static final class Track extends AbstractTickableSoundInstance {
        int age;
        boolean fadingOut, started;
        float fadeFrom = 1F;
        int fadeAge;

        Track(SoundEvent e) {
            super(e, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = false;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.001F;
        }

        void fadeOut() {
            if (fadingOut) return;
            fadingOut = true;
            fadeFrom = volume;
            fadeAge = 0;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            started = true;
            age++;
            if (fadingOut) {
                fadeAge++;
                volume = Math.max(0F, fadeFrom * (1F - fadeAge / (float) FADE_OUT));
                if (volume <= 0.001F) stop();
            } else volume = Math.min(1F, Math.max(0.001F, age / (float) FADE_IN));
        }
    }

    private QHMusic() {}
}
