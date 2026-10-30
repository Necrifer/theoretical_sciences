package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a tuning control on a multiblock's panel was changed. field 0 = setting (delta in tenths), 1 = overclock, 2 = parallel. */
public record TuningPayload(BlockPos pos, int field, int delta) implements CustomPacketPayload {
    public static final Type<TuningPayload> TYPE = new Type<>(TheoreticalSciences.id("tuning"));
    public static final StreamCodec<ByteBuf, TuningPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TuningPayload::pos,
            ByteBufCodecs.VAR_INT, TuningPayload::field,
            ByteBufCodecs.VAR_INT, TuningPayload::delta,
            TuningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
