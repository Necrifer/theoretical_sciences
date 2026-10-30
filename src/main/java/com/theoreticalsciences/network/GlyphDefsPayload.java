package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the research glyph glossary, as JSON. */
public record GlyphDefsPayload(String json) implements CustomPacketPayload {
    public static final Type<GlyphDefsPayload> TYPE = new Type<>(TheoreticalSciences.id("glyph_defs"));
    public static final StreamCodec<ByteBuf, GlyphDefsPayload> CODEC =
            ByteBufCodecs.stringUtf8(1 << 21).map(GlyphDefsPayload::new, GlyphDefsPayload::json);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
