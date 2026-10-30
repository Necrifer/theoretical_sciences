package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the holder of the Lawgiver's Codex rewrote a law (see the action constants in {@link CodexLaws}). */
public record CodexActionPayload(int action, int arg, int target, String text) implements CustomPacketPayload {
    public static final Type<CodexActionPayload> TYPE = new Type<>(TheoreticalSciences.id("codex_action"));
    public static final StreamCodec<ByteBuf, CodexActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CodexActionPayload::action,
            ByteBufCodecs.VAR_INT, CodexActionPayload::arg,
            ByteBufCodecs.VAR_INT, CodexActionPayload::target,
            ByteBufCodecs.stringUtf8(128), CodexActionPayload::text,
            CodexActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
