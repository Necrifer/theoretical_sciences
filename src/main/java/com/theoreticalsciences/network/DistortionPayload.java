package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player is inside a distorted structure (mode, 0..1 strength, its heart and size). */
public record DistortionPayload(int mode, float intensity, BlockPos centre, int radius) implements CustomPacketPayload {
    public static final Type<DistortionPayload> TYPE = new Type<>(TheoreticalSciences.id("distortion"));
    public static final StreamCodec<ByteBuf, DistortionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DistortionPayload::mode,
            ByteBufCodecs.FLOAT, DistortionPayload::intensity,
            BlockPos.STREAM_CODEC, DistortionPayload::centre,
            ByteBufCodecs.VAR_INT, DistortionPayload::radius,
            DistortionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
