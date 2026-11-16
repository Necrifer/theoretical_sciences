package com.theoreticalsciences.home;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 1.8.1: the Homeward Portal does not simply appear. When the twelfth keystone is set, each keystone sends a beam of
 * light up into the sky; the twelve lean together and meet high over the ring; what gathers there comes down as one
 * great beam onto the opening, swells until the whole ring is inside it, and sinks away into the portal it leaves.
 * The server keeps the time and fills the opening when the beam lands; clients draw it ({@link PortalOpeningRenderer}).
 */
public final class PortalOpening {
    private PortalOpening() {}

    /** In ticks from the twelfth keystone. */
    public static final int RISE = 40, MEET = 70, GATHER = 92, STRIKE = 102, FILL = 112, SWELL = 124, END = 160;
    /** How high over the ring the beams meet. */
    public static final float HEIGHT = 44F;

    private record Pending(ResourceKey<Level> dim, BlockPos centre, long start) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    public record Start(BlockPos centre, long start) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(TheoreticalSciences.id("home_portal_opening"));
        public static final StreamCodec<FriendlyByteBuf, Start> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Start::centre, ByteBufCodecs.VAR_LONG, Start::start, Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static boolean opening(ServerLevel level, BlockPos centre) {
        for (Pending p : PENDING) if (p.dim == level.dimension() && p.centre.equals(centre)) return true;
        return false;
    }

    public static void begin(ServerLevel level, BlockPos centre) {
        if (opening(level, centre)) return;
        PENDING.add(new Pending(level.dimension(), centre.immutable(), level.getGameTime()));
        PacketDistributor.sendToPlayersNear(level, null, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 256, new Start(centre, level.getGameTime()));
        level.playSound(null, centre, QHSounds.HOME_PORTAL_OPEN.get(), SoundSource.BLOCKS, 6F, 1F);
    }

    public static void onLevelTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level) || PENDING.isEmpty()) return;
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.dim != level.dimension()) continue;
            long age = level.getGameTime() - p.start;
            double x = p.centre.getX() + 0.5, y = p.centre.getY(), z = p.centre.getZ() + 0.5;
            if (!PortalFrameBlock.ringComplete(level, p.centre)) {          // a frame was broken while the light was coming
                it.remove();
                continue;
            }
            if (age < RISE && age % 4 == 0) {
                for (Direction d : Direction.Plane.HORIZONTAL)
                    for (int o = -1; o <= 1; o++) {
                        BlockPos f = p.centre.relative(d, 2).relative(d.getClockWise(), o);
                        level.sendParticles(ParticleTypes.END_ROD, f.getX() + 0.5, f.getY() + 1.1, f.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.06);
                    }
            }
            if (age == STRIKE) level.sendParticles(ParticleTypes.FLASH, x, y + 1.5, z, 3, 0.5, 0.5, 0.5, 0);
            if (age == FILL) {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) level.setBlock(p.centre.offset(dx, 0, dz), HomeRegistry.PORTAL.get().defaultBlockState(), 2);
                level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 400, 1.6, 0.6, 1.6, 0.25);
            }
            if (age > FILL && age < END && age % 2 == 0) level.sendParticles(ParticleTypes.END_ROD, x, y + 4, z, 12, 2.2, 3.0, 2.2, 0.01);
            if (age >= END) it.remove();
        }
    }
}
