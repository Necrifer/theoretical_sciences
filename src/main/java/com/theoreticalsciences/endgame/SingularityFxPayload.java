package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server -> client: the Scepter made a singularity here; draw the real thing (kind = Scepter mode ordinal) for a while. */
public record SingularityFxPayload(double x, double y, double z, int kind, int ticks) implements CustomPacketPayload {
    public static final Type<SingularityFxPayload> TYPE = new Type<>(TheoreticalSciences.id("singularity_fx"));
    public static final StreamCodec<FriendlyByteBuf, SingularityFxPayload> CODEC = StreamCodec.of((b, p) -> {
        b.writeDouble(p.x);
        b.writeDouble(p.y);
        b.writeDouble(p.z);
        b.writeVarInt(p.kind);
        b.writeVarInt(p.ticks);
    }, b -> new SingularityFxPayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, Vec3 at, int kind, int ticks) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 160, new SingularityFxPayload(at.x, at.y, at.z, kind, ticks));
    }
}
