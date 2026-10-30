package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Client -> server: a research puzzle action. action 0 = submit derivation (tokens), 1 = submit answer, 2 = buy hint (kind), 3 = creative skip. */
public record PuzzlePayload(ResourceLocation id, int action, List<String> tokens, double answer, String kind) implements CustomPacketPayload {
    public static final Type<PuzzlePayload> TYPE = new Type<>(TheoreticalSciences.id("puzzle"));
    public static final StreamCodec<ByteBuf, PuzzlePayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PuzzlePayload::id, ByteBufCodecs.VAR_INT, PuzzlePayload::action,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(400)), PuzzlePayload::tokens, ByteBufCodecs.DOUBLE, PuzzlePayload::answer,
            ByteBufCodecs.STRING_UTF8, PuzzlePayload::kind, PuzzlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
