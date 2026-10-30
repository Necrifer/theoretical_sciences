package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the impossible recipe is ready on the Omnilex Engine at pos; ask the player before it starts. */
public record CollapseConfirmPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<CollapseConfirmPayload> TYPE = new Type<>(TheoreticalSciences.id("collapse_confirm"));
    public static final StreamCodec<ByteBuf, CollapseConfirmPayload> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CollapseConfirmPayload::pos, CollapseConfirmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
