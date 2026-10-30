package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the Censer of Entropy's new settings. */
public record CenserPayload(String effect, int seconds, int level, int radius) implements CustomPacketPayload {
    public static final Type<CenserPayload> TYPE = new Type<>(TheoreticalSciences.id("censer"));
    public static final StreamCodec<ByteBuf, CenserPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), CenserPayload::effect, ByteBufCodecs.VAR_INT, CenserPayload::seconds,
            ByteBufCodecs.VAR_INT, CenserPayload::level, ByteBufCodecs.VAR_INT, CenserPayload::radius, CenserPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
