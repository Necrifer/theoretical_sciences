package com.theoreticalsciences.client.render;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client-side power state of each multiblock, for its start-up and shut-down sequences.
 * <ul>
 *   <li><b>Start-up</b> (3.5 s): a wave of light runs out from the controller through every block of the structure,
 *       the effect ignites with a stutter, swells past full size and settles.</li>
 *   <li><b>Shut-down</b> (2.5 s): a dimmer wave runs back inward, sparks jump off the casing, the effect flickers,
 *       shrinks and collapses into a last flash.</li>
 * </ul>
 * Nothing is sent over the network: it reacts to the active / idle state the client already knows.
 */
public final class PowerAnimator {
    public static final int STARTUP_TICKS = 70, SHUTDOWN_TICKS = 50, HYSTERESIS = 12;
    public static final float WAVE_UP = 2.4F, WAVE_DOWN = 1.8F;     // seconds

    public static final class State {
        public boolean on;
        float from;
        long changeTick = Long.MIN_VALUE / 2;
        long lastActive = Long.MIN_VALUE / 2;
        public List<BlockPos> parts = List.of();
        public double maxDist = 1;
        BlockPos builtFor;

        /** 0 = off, 1 = fully running, at the given time. */
        public float power(double t) {
            double dt = t - changeTick;
            float p = on ? (float) (from + dt / STARTUP_TICKS) : (float) (from - dt / SHUTDOWN_TICKS);
            return Math.max(0F, Math.min(1F, p));
        }

        /** Seconds since the last switch on or off. */
        public double sinceChange(double t) {
            return (t - changeTick) / 20.0;
        }
    }

    private static final Map<ControllerBlockEntity, State> STATES = new WeakHashMap<>();

    public static State update(ControllerBlockEntity c, double t) {
        boolean active = c.isFormed() && c.isActive();
        State s = STATES.get(c);
        if (s == null) {
            s = new State();
            s.on = active;                     // already running when it came into view: no replay of the start-up
            s.from = active ? 1F : 0F;
            if (active) s.lastActive = (long) t;
            STATES.put(c, s);
        }
        if (active) s.lastActive = (long) t;
        boolean on = t - s.lastActive <= HYSTERESIS;
        if (on != s.on) {
            float now = s.power(t);
            s.on = on;
            s.from = now;
            s.changeTick = (long) t;
            Vec3 ctr = c.effectCenter();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && (on ? now < 0.5F : now > 0.5F))
                mc.level.playLocalSound(ctr.x, ctr.y, ctr.z, (on ? QHSounds.MACHINE_STARTUP : QHSounds.MACHINE_SHUTDOWN).get(), SoundSource.BLOCKS, 1.6F, 1.0F, false);
        }
        if (!c.getBlockPos().equals(s.builtFor) && c.definition() != null) {
            MultiblockDefinition def = c.definition();
            s.parts = def.solidPositions(c.getBlockPos(), c.facing());
            s.builtFor = c.getBlockPos();
            double m = 1;
            for (BlockPos p : s.parts) m = Math.max(m, Math.sqrt(p.distSqr(c.getBlockPos())));
            s.maxDist = m;
        }
        return s;
    }

    private PowerAnimator() {}
}
