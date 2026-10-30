package com.theoreticalsciences.endgame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Chronal Stasis: mobs near the wielder of the Omnipotent Edge lose their AI for a while.
 * <p>
 * The release time (in game ticks) lives in the mob's own persistent data, so the freeze survives the mob unloading or
 * the world closing and is always undone: on every 10th tick of the mob and whenever it joins a level we check whether
 * it has expired. Mobs that were already NoAI (set by a map maker, say) are left alone and never "released".
 */
public final class ChronalStasis {
    static final String KEY = "theoreticalsciences_stasis_until";
    /** never hold anything longer than this, even if the world clock jumped backwards */
    private static final long MAX_HOLD = 20 * 60;

    public static int freeze(ServerLevel level, Player user, double radius, int ticks) {
        long until = level.getGameTime() + ticks;
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, user.getBoundingBox().inflate(radius),
                m -> m.isAlive() && m.distanceToSqr(user) <= radius * radius)) {
            CompoundTag data = m.getPersistentData();
            if (m.isNoAi() && !data.contains(KEY)) continue;             // frozen by someone else: not ours to touch
            data.putLong(KEY, until);
            m.setNoAi(true);
            m.setTarget(null);
            m.setDeltaMovement(Vec3.ZERO);
            m.getNavigation().stop();
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), m.getX(), m.getY(0.5), m.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            n++;
        }
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), user.getX(), user.getY(1), user.getZ(), 60, radius / 3, 1.5, radius / 3, 0.01);
        level.playSound(null, user.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, user.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
        return n;
    }

    public static void onEntityTick(EntityTickEvent.Pre e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Mob m) || ent.level().isClientSide || (ent.tickCount + ent.getId()) % 10 != 0) return;
        check(m, false);
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof Mob m) check(m, true);
    }

    private static void check(Mob m, boolean joining) {
        CompoundTag data = m.getPersistentData();
        if (!data.contains(KEY)) return;
        long now = m.level().getGameTime(), until = data.getLong(KEY);
        if (now >= until || until - now > MAX_HOLD) {
            data.remove(KEY);
            m.setNoAi(false);
            if (!joining && m.level() instanceof ServerLevel sl)
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), m.getX(), m.getY(0.5), m.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
        } else if (!m.isNoAi()) m.setNoAi(true);              // still within its stasis
    }

    private ChronalStasis() {}
}
