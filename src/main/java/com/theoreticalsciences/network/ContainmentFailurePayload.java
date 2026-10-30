package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the multiblock at pos is going into meltdown (hazard = TuningProfile.Hazard ordinal); play the animation. */
public record ContainmentFailurePayload(BlockPos pos, int hazard) implements CustomPacketPayload {
    public static final Type<ContainmentFailurePayload> TYPE = new Type<>(TheoreticalSciences.id("containment_failure"));
    public static final StreamCodec<ByteBuf, ContainmentFailurePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ContainmentFailurePayload::pos, ByteBufCodecs.VAR_INT, ContainmentFailurePayload::hazard, ContainmentFailurePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
