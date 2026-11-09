package com.theoreticalsciences.helio;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

/**
 * 1.5.0: space weather. Every few days the Sun throws a coronal mass ejection at the world; a day or so later (here, at
 * once) it strikes the magnetosphere and a geomagnetic storm follows for about five minutes.
 * <ul>
 *   <li>It changes no block and harms no one. It affects machines and the sky only.</li>
 *   <li>Machines in the Overworld that run a console or a multiblock run at 70% while it lasts, the currents it induces
 *   upsetting their controls, unless they carry a Faraday Mesh upgrade.</li>
 *   <li>At night, under open sky, aurorae hang over every player in the Overworld: curtains of green and violet light.</li>
 *   <li>Players are told when one begins and ends.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class SpaceWeather extends SavedData {
    private static final String NAME = "theoreticalsciences_space_weather";
    /** Between storms: three to seven days. A storm lasts five minutes. */
    static final long MIN_GAP = 72_000, MAX_GAP = 168_000, LENGTH = 6_000;

    long next = -1, end = -1;

    static SpaceWeather load(CompoundTag t, HolderLookup.Provider r) {
        SpaceWeather w = new SpaceWeather();
        w.next = t.getLong("Next");
        w.end = t.getLong("End");
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putLong("Next", next);
        t.putLong("End", end);
        return t;
    }

    static SpaceWeather get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SpaceWeather::new, SpaceWeather::load, null), NAME);
    }

    /** Whether a geomagnetic storm is raging over this level now (only the Overworld has a magnetosphere to strike). */
    public static boolean storm(Level level) {
        if (!(level instanceof ServerLevel sl) || level.dimension() != Level.OVERWORLD) return false;
        SpaceWeather w = get(sl.getServer());
        return w.end > sl.getGameTime();
    }

    /** What a storm leaves of a machine's speed: 0.7 in a storm without a Faraday Mesh, otherwise 1. */
    public static double factor(Level level, String upgradeKind) {
        return "faraday_mesh".equals(upgradeKind) || !storm(level) ? 1.0 : 0.7;
    }

    /** Starts a storm now, and sets the next one three to seven days after it ends. */
    public static void begin(MinecraftServer server) {
        SpaceWeather w = get(server);
        long now = server.overworld().getGameTime();
        w.end = now + LENGTH;
        w.next = w.end + MIN_GAP + server.overworld().random.nextInt((int) (MAX_GAP - MIN_GAP));
        w.setDirty();
        for (ServerPlayer p : server.overworld().players())
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.storm_begins").withStyle(ChatFormatting.GREEN), false);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel ow = server.overworld();
        long now = ow.getGameTime();
        if (now % 20 != 0) return;
        SpaceWeather w = get(server);
        if (w.next < 0) {
            w.next = now + MIN_GAP + ow.random.nextInt((int) (MAX_GAP - MIN_GAP));
            w.setDirty();
        }
        if (now >= w.next && w.end <= now) begin(server);
        if (w.end > 0 && w.end <= now && w.end > now - 20) {
            for (ServerPlayer p : ow.players())
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.storm_ends").withStyle(ChatFormatting.GRAY), false);
        }
        if (w.end > now && ow.isNight()) aurora(ow);
    }

    static final Vector3f GREEN = new Vector3f(0.25F, 1.0F, 0.45F), VIOLET = new Vector3f(0.65F, 0.35F, 1.0F);

    /** Curtains of light thirty to fifty blocks above each player under open sky. */
    static void aurora(ServerLevel ow) {
        for (ServerPlayer p : ow.players()) {
            int top = ow.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
            if (top > p.getBlockY() + 2) continue;                                   // under a roof: no sky to see it in
            double t = ow.getGameTime() * 0.01;
            for (int i = 0; i < 40; i++) {
                double u = (i / 40.0 - 0.5) * 120;
                double x = p.getX() + u, z = p.getZ() + 40 + Math.sin(u * 0.05 + t) * 12;
                double y = p.getY() + 32 + ow.random.nextDouble() * 18;
                boolean hi = y > p.getY() + 44;
                ow.sendParticles(p, new DustParticleOptions(hi ? VIOLET : GREEN, 4.0F), true, x, y, z, 1, 0.5, 2.5, 0.5, 0);
            }
        }
    }
}
