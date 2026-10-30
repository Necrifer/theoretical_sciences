package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: the player's puzzle progress, research id -> letters (d derived, s solved, v/m/f/e hints bought). */
public record PuzzleStatePayload(CompoundTag state) implements CustomPacketPayload {
    public static final Type<PuzzleStatePayload> TYPE = new Type<>(TheoreticalSciences.id("puzzle_state"));
    public static final StreamCodec<ByteBuf, PuzzleStatePayload> CODEC = ByteBufCodecs.COMPOUND_TAG.map(PuzzleStatePayload::new, PuzzleStatePayload::state);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
