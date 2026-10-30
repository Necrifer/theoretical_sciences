package com.theoreticalsciences.world.transit;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the Pleroma Reconstitutor has just rebuilt the Transcendental. Play the ~10 second cinematic
 * centred on the machine (x, y, z = the centre of the multiblock, in the player's current dimension).
 */
public record RekindlePayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<RekindlePayload> TYPE = new Type<>(TheoreticalSciences.id("rekindle_cinematic"));
    public static final StreamCodec<ByteBuf, RekindlePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RekindlePayload::x,
            ByteBufCodecs.DOUBLE, RekindlePayload::y,
            ByteBufCodecs.DOUBLE, RekindlePayload::z,
            RekindlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
