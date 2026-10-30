package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the holder of the Dodecaract Excavator scrolled while sneaking (+1 / -1: a bigger / smaller reach). */
public record ExcavatorPayload(int delta) implements CustomPacketPayload {
    public static final Type<ExcavatorPayload> TYPE = new Type<>(TheoreticalSciences.id("excavator_radius"));
    public static final StreamCodec<ByteBuf, ExcavatorPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ExcavatorPayload::delta, ExcavatorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
