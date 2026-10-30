package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: every pack-added and automatic singularity, and the cluster names, as JSON. */
public record SingularityDefsPayload(String json) implements CustomPacketPayload {
    public static final Type<SingularityDefsPayload> TYPE = new Type<>(TheoreticalSciences.id("singularity_defs"));
    public static final StreamCodec<ByteBuf, SingularityDefsPayload> CODEC =
            ByteBufCodecs.stringUtf8(1 << 21).map(SingularityDefsPayload::new, SingularityDefsPayload::json);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
