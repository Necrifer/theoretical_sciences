package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the player's Yes / No to collapsing the Omnilex Engine. */
public record CollapseAnswerPayload(BlockPos pos, boolean yes) implements CustomPacketPayload {
    public static final Type<CollapseAnswerPayload> TYPE = new Type<>(TheoreticalSciences.id("collapse_answer"));
    public static final StreamCodec<ByteBuf, CollapseAnswerPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CollapseAnswerPayload::pos, ByteBufCodecs.BOOL, CollapseAnswerPayload::yes, CollapseAnswerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
