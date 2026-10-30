package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the operator set one of a multiblock's physics parameters to a value. */
public record PhysicsSetPayload(BlockPos pos, int index, double value) implements CustomPacketPayload {
    public static final Type<PhysicsSetPayload> TYPE = new Type<>(TheoreticalSciences.id("physics_set"));
    public static final StreamCodec<ByteBuf, PhysicsSetPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PhysicsSetPayload::pos,
            ByteBufCodecs.VAR_INT, PhysicsSetPayload::index,
            ByteBufCodecs.DOUBLE, PhysicsSetPayload::value,
            PhysicsSetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
