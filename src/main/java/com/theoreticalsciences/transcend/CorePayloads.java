package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** The Omnipotence Core's screen (round 15): opened by the server with the Core's state; its Shutdown and Forge buttons. */
public final class CorePayloads {
    /** Server -> client: open the Core's screen. phase = Omnipotence.Phase ordinal; seconds = shutdown time left. */
    public record Open(BlockPos core, int phase, int seconds) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(TheoreticalSciences.id("core_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::core, ByteBufCodecs.VAR_INT, Open::phase, ByteBufCodecs.VAR_INT, Open::seconds, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: 0 = begin the Shutdown Sequence, 1 = forge recipe {@code arg}. */
    public record Action(BlockPos core, int action, String arg) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(TheoreticalSciences.id("core_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Action::core, ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.stringUtf8(2048), Action::arg, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(ServerPlayer sp, Action a) {
            Level l = sp.level();
            if (!sp.blockPosition().closerThan(a.core(), 12)) return;
            if (!inSession(sp, a.core())) return;            // 1.2.0: only from the screen this player opened
            boolean forged = ForgedCoreBlock.awake(l, a.core());      // round 27: a Core forged anew, awake
            if (!forged && !l.getBlockState(a.core()).is(TranscendRegistry.CORE.get())) return;
            switch (a.action()) {
                case 0 -> {
                    if (!forged) Omnipotence.beginShutdown(sp, a.core());      // a forged Core has no Shutdown Sequence
                }
                case 1 -> {
                    // a forged Core cannot forge another Core, or the Hand: those come only from the reborn one
                    // (round 31: also any datapack recipe that makes either of them)
                    if (forged && (a.arg().startsWith("forged_omnipotence_core") || a.arg().startsWith("demiurge_hand") || onlyReborn(a.arg()))) {
                        sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.forged_core.only_reborn")
                                .withStyle(net.minecraft.ChatFormatting.RED), true);
                    } else if (forged || Omnipotence.phase(sp.server) == Omnipotence.Phase.REBORN) {
                        String[] parts = a.arg().split(";", 2);          // "recipe;law1,law2,..."
                        CoreForge.forge(sp, parts[0], ForgeLaws.decode(parts.length > 1 ? parts[1] : ""));
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Whether the recipe named in a forge request makes something only the reborn Core may make. */
    static boolean onlyReborn(String arg) {
        CoreForge.Recipe r = CoreForge.byId(arg.split(";", 2)[0]);
        if (r == null) return false;
        var out = r.out().get();
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(out);
        return key.getNamespace().equals(TheoreticalSciences.MODID) && (key.getPath().equals("forged_omnipotence_core") || key.getPath().equals("demiurge_hand"));
    }

    /** A forged Core's screen: always "rekindled" (its Forge works, it has no Shutdown Sequence). */
    // ------------------------------------------------------------------ 1.2.0: sessions
    /** A player's open Core screen: which Core (and dimension), and until when (game time). */
    private record Session(BlockPos core, net.minecraft.resources.ResourceKey<Level> dim, long until) {}

    private static final java.util.Map<java.util.UUID, Session> SESSIONS =
            com.theoreticalsciences.world.SessionMaps.perPlayer(new java.util.concurrent.ConcurrentHashMap<>());
    private static final long SESSION_TICKS = 20 * 60 * 10;      // ten minutes, renewed by every action

    /** Opening the screen (by using the Core, which the game checks for reach) starts a session, if the land allows. */
    static boolean begin(ServerPlayer sp, BlockPos core) {
        if (sp.isSpectator() || !com.theoreticalsciences.compat.ClaimChecks.mayAffect(sp, core, null, com.theoreticalsciences.compat.ClaimChecks.Kind.PLACE)) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.claimed")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return false;
        }
        SESSIONS.put(sp.getUUID(), new Session(core.immutable(), sp.level().dimension(), sp.level().getGameTime() + SESSION_TICKS));
        return true;
    }

    /**
     * True when sp opened this Core's screen (by using it) and is still near it: the Core's packets are refused
     * otherwise, so a modified client cannot work someone else's Core from behind a wall, in a claim, or as a spectator.
     */
    public static boolean inSession(ServerPlayer sp, BlockPos core) {
        if (sp.isSpectator()) return false;
        Session s = SESSIONS.get(sp.getUUID());
        long now = sp.level().getGameTime();
        if (s == null || !s.core().equals(core) || s.dim() != sp.level().dimension() || now > s.until()) return false;
        SESSIONS.put(sp.getUUID(), new Session(s.core(), s.dim(), now + SESSION_TICKS));
        return true;
    }

    public static void openForged(ServerPlayer sp, BlockPos core) {
        if (!begin(sp, core)) return;
        ForgeDrift.sync(sp);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new Open(core, Omnipotence.Phase.REBORN.ordinal(), 0));
    }

    public static void open(ServerPlayer sp, BlockPos core) {
        if (!begin(sp, core)) return;
        ForgeDrift.sync(sp);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp,
                new Open(core, Omnipotence.phase(sp.server).ordinal(), Omnipotence.secondsLeft(sp.server)));
    }

    private CorePayloads() {}
}
