package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server -> client: the player's unlocked research and insight. */
public record ResearchStatePayload(List<ResourceLocation> unlocked, int insight, int observations, String active, int total, int remaining) implements CustomPacketPayload {
    public static final Type<ResearchStatePayload> TYPE = new Type<>(TheoreticalSciences.id("research_state"));
    public static final StreamCodec<ByteBuf, ResearchStatePayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchStatePayload::unlocked,
            ByteBufCodecs.VAR_INT, ResearchStatePayload::insight,
            ByteBufCodecs.VAR_INT, ResearchStatePayload::observations,
            ByteBufCodecs.STRING_UTF8, ResearchStatePayload::active,
            ByteBufCodecs.VAR_INT, ResearchStatePayload::total,
            ByteBufCodecs.VAR_INT, ResearchStatePayload::remaining,
            ResearchStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
