package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: every machine physics model and the coolant table, as JSON (for the machine screens). */
public record PhysicsDefsPayload(String json) implements CustomPacketPayload {
    public static final Type<PhysicsDefsPayload> TYPE = new Type<>(TheoreticalSciences.id("physics_defs"));
    public static final StreamCodec<ByteBuf, PhysicsDefsPayload> CODEC =
            ByteBufCodecs.stringUtf8(1 << 22).map(PhysicsDefsPayload::new, PhysicsDefsPayload::json);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
