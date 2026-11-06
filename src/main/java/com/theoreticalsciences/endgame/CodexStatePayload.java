package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the current laws of the dimension the player stands in, and whether this player may change them. */
public record CodexStatePayload(int gravity, boolean hostile, boolean passive, int health, int damage, int speed, boolean worldLaws) implements CustomPacketPayload {
    public static final Type<CodexStatePayload> TYPE = new Type<>(TheoreticalSciences.id("codex_state"));
    public static final StreamCodec<ByteBuf, CodexStatePayload> CODEC = StreamCodec.of((buf, p) -> {
        ByteBufCodecs.VAR_INT.encode(buf, p.gravity());
        buf.writeBoolean(p.hostile());
        buf.writeBoolean(p.passive());
        ByteBufCodecs.VAR_INT.encode(buf, p.health());
        ByteBufCodecs.VAR_INT.encode(buf, p.damage());
        ByteBufCodecs.VAR_INT.encode(buf, p.speed());
        buf.writeBoolean(p.worldLaws());                   // 1.2.0: may this player set the dimension-wide laws
    }, buf -> new CodexStatePayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean(), buf.readBoolean(), ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
