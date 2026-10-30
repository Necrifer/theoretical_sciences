package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the current laws of the dimension the player stands in, for the Codex screen. */
public record CodexStatePayload(int gravity, boolean hostile, boolean passive, int health, int damage, int speed) implements CustomPacketPayload {
    public static final Type<CodexStatePayload> TYPE = new Type<>(TheoreticalSciences.id("codex_state"));
    public static final StreamCodec<ByteBuf, CodexStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CodexStatePayload::gravity,
            ByteBufCodecs.BOOL, CodexStatePayload::hostile,
            ByteBufCodecs.BOOL, CodexStatePayload::passive,
            ByteBufCodecs.VAR_INT, CodexStatePayload::health,
            ByteBufCodecs.VAR_INT, CodexStatePayload::damage,
            ByteBufCodecs.VAR_INT, CodexStatePayload::speed,
            CodexStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
