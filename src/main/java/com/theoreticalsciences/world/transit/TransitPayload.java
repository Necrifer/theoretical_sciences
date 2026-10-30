package com.theoreticalsciences.world.transit;

import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: "a trip between dimensions is happening to you".
 *
 * <ul>
 *   <li>{@code style} - which dimension's look to use (e.g. theoreticalsciences:the_bulk). When entering a dimension this is
 *       where you are going; when going back home it is the dimension you are leaving.</li>
 *   <li>{@code leaving} - true when the player is leaving that dimension (the animation then plays mirrored/reversed).</li>
 *   <li>{@code ticks} - more than 0: the jump will happen in this many ticks, so build the animation up over that time.
 *       0: the jump is happening right now (fall-throughs, arenas...), show the full scene at once.
 *       -1: the trip was cancelled (death, logged out, moved dimension some other way), fade the animation away.</li>
 * </ul>
 */
public record TransitPayload(ResourceLocation style, boolean leaving, int ticks) implements CustomPacketPayload {
    public static final Type<TransitPayload> TYPE = new Type<>(TheoreticalSciences.id("transit"));
    public static final StreamCodec<ByteBuf, TransitPayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TransitPayload::style,
            ByteBufCodecs.BOOL, TransitPayload::leaving,
            ByteBufCodecs.VAR_INT, TransitPayload::ticks,
            TransitPayload::new);

    public static final int CANCEL = -1;
    public static final int NOW = 0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
