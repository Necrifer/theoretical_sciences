package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: request to research an entry. */
public record UnlockResearchPayload(ResourceLocation id) implements CustomPacketPayload {
    public static final Type<UnlockResearchPayload> TYPE = new Type<>(TheoreticalSciences.id("unlock_research"));
    public static final StreamCodec<ByteBuf, UnlockResearchPayload> CODEC =
            ResourceLocation.STREAM_CODEC.map(UnlockResearchPayload::new, UnlockResearchPayload::id);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
