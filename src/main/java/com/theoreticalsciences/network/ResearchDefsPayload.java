package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: every research definition, as JSON. */
public record ResearchDefsPayload(String json) implements CustomPacketPayload {
    public static final Type<ResearchDefsPayload> TYPE = new Type<>(TheoreticalSciences.id("research_defs"));
    public static final StreamCodec<ByteBuf, ResearchDefsPayload> CODEC =
            ByteBufCodecs.stringUtf8(1 << 21).map(ResearchDefsPayload::new, ResearchDefsPayload::json);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
