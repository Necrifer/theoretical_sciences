package com.theoreticalsciences.world.transit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pleroma Reconstitutor's finale. When its recipe completes (the Transcendental has been rebuilt), everyone within
 * {@link #RANGE} blocks gets a 10 second cinematic (drawn by their own game, see client/cinematic), and the server adds
 * real particles and sounds on a timeline that matches it, so even players without the visuals see something:
 * <pre>
 *   0 - 3 s   pillars of light climb out of the machine
 *   2 - 8 s   the Transcendental is drawn in the sky above it, ring by ring, as the Echo plays back
 *   8.5 s     a flash: it is whole again
 *   10 s      everything settles
 * </pre>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class RekindleCinematic {
    public static final int RANGE = 96;
    public static final int LENGTH = 200;     // ticks (10 seconds)
    /** How high above the machine the Transcendental's outline is drawn (the client uses the same number). */
    public static final double SKY = 14;

    private static final class Show {
        final ResourceKey<Level> dim;
        final Vec3 c;
        int age;

        Show(ResourceKey<Level> dim, Vec3 c) {
            this.dim = dim;
            this.c = c;
        }
    }

    private static final List<Show> ACTIVE = new ArrayList<>();

    /** Called once by the Reconstitutor when the rekindling recipe finishes. {@code centre} is the machine's middle. */
    public static void play(ServerLevel level, Vec3 centre) {
        for (ServerPlayer p : level.players())
            if (p.position().distanceTo(centre) <= RANGE) PacketDistributor.sendToPlayer(p, new RekindlePayload(centre.x, centre.y, centre.z));
        if (ACTIVE.size() < 8) ACTIVE.add(new Show(level.dimension(), centre));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (ACTIVE.isEmpty()) return;
        for (Show s : new ArrayList<>(ACTIVE)) {
            ServerLevel l = e.getServer().getLevel(s.dim);
            if (l == null || ++s.age > LENGTH) {
                ACTIVE.remove(s);
                continue;
            }
            step(l, s);
        }
    }

    /** One tick of the server-side particles and sounds. */
    private static void step(ServerLevel l, Show s) {
        int a = s.age;
        Vec3 c = s.c;
        BlockPos at = BlockPos.containing(c);
        double top = c.y + SKY;
        if (a == 1) {
            l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 3F, 0.5F);
            l.playSound(null, at, QHSounds.OMNIPOTENCE_GRANT.get(), SoundSource.BLOCKS, 2.5F, 1F);
        }
        // 0 - 3 s: pillars of light (a ring of six around the machine, one in the middle)
        if (a < 70 && a % 2 == 0) {
            double h = Math.min(1, a / 50.0) * (SKY + 10);
            for (int i = 0; i < 7; i++) {
                double ang = i * Math.PI * 2 / 6 + a * 0.02;
                double r = i == 6 ? 0 : 1.6;
                l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x + Math.cos(ang) * r, c.y + l.random.nextDouble() * h, c.z + Math.sin(ang) * r, 2, 0.05, 0.3, 0.05, 0.01);
            }
        }
        if (a == 40) l.playSound(null, at, QHSounds.HYPEROBJECT_HUM.get(), SoundSource.BLOCKS, 3F, 0.7F);
        // 2 - 8 s: the rings are drawn one after another; each new ring chimes a little higher
        if (a >= 60 && a <= 160) {
            int ring = (a - 60) / 20;
            double radius = 3 + ring * 2.6;
            double ang = (a % 20) / 20.0 * Math.PI * 2;
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x + Math.cos(ang) * radius, top - ring * 0.6, c.z + Math.sin(ang) * radius, 3, 0.1, 0.1, 0.1, 0.0);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.GLYPH.get(), c.x, top, c.z, 6, radius * 0.5, 1.5, radius * 0.5, 0.6);
            if ((a - 60) % 20 == 0)
                l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.BLOCKS, 3F, 0.6F + ring * 0.25F);
        }
        // 8.5 s: it is whole again
        if (a == 170) {
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), c.x, top, c.z, 3, 1, 1, 1, 0);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, top, c.z, 400, 6, 4, 6, 0.4);
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.RESOLVE.get(), c.x, top, c.z, 200, 4, 3, 4, 0.6);
            l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.BLOCKS, 2F, 1.4F);
            l.playSound(null, at, com.theoreticalsciences.registry.QHSounds.ASCEND.get(), SoundSource.MASTER, 1.5F, 1F);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        ACTIVE.clear();
    }

    private RekindleCinematic() {}
}
