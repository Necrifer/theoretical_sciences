package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.endgame.DemiurgeHandItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Round 34c: the forged Core's attunement screen - the server's state for it, and the player's changes. */
public final class AttunePayloads {
    /** Server -> client: the Core's attunement as it reads (see {@link ForgedCoreBlockEntity#screenState}). */
    public record State(BlockPos core, CompoundTag tag) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(TheoreticalSciences.id("attune_state"));
        public static final StreamCodec<ByteBuf, State> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, State::core, ByteBufCodecs.COMPOUND_TAG, State::tag, State::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: set parameter {@code index} to {@code value}; index -1 = just poll, -2 = wake the Core. */
    public record Set(BlockPos core, int index, int value) implements CustomPacketPayload {
        public static final Type<Set> TYPE = new Type<>(TheoreticalSciences.id("attune_set"));
        public static final StreamCodec<ByteBuf, Set> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Set::core, ByteBufCodecs.VAR_INT, Set::index, ByteBufCodecs.VAR_INT, Set::value, Set::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ServerPlayer sp, Set p) {
            if (!sp.level().isLoaded(p.core()) || !sp.blockPosition().closerThan(p.core(), 10)) return;
            if (!(sp.level().getBlockEntity(p.core()) instanceof ForgedCoreBlockEntity core)) return;
            if (!sp.level().mayInteract(sp, p.core())) return;
            // 1.3.1: only from the attuning screen this player opened with the Hand (which paid its charge and passed the
            // claim checks), and only where they may still act: a modified client cannot attune someone else's Core
            if (!CorePayloads.inSession(sp, p.core())) return;
            if (!com.theoreticalsciences.compat.ClaimChecks.mayAffect(sp, p.core(), null, com.theoreticalsciences.compat.ClaimChecks.Kind.PLACE)
                    || sp.blockActionRestricted(sp.level(), p.core(), sp.gameMode.getGameModeForPlayer())) return;
            ItemStack hand = hand(sp);
            if (hand.isEmpty()) return;                       // only the Hand of the Demiurge can attune a Core
            if (p.index() == -2) DemiurgeHandItem.wake(sp, core, hand);
            else if (p.index() >= 0) core.set(p.index(), p.value());
            core.handled();
            send(sp, core, false);
        }
    }

    /** The Hand of the Demiurge the player holds (main hand first), or empty. */
    public static ItemStack hand(ServerPlayer sp) {
        for (ItemStack s : new ItemStack[]{sp.getMainHandItem(), sp.getOffhandItem()})
            if (s.getItem() instanceof DemiurgeHandItem) return s;
        return ItemStack.EMPTY;
    }

    public static void send(ServerPlayer sp, ForgedCoreBlockEntity core, boolean open) {
        if (open) {
            if (!CorePayloads.begin(sp, core.getBlockPos())) return;      // 1.3.1: the screen's packets need this session
            core.handled();
        }
        PacketDistributor.sendToPlayer(sp, new State(core.getBlockPos(), core.screenState(open)));
    }

    private AttunePayloads() {}
}
