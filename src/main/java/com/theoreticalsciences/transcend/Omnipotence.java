package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The fate of the Omnipotence Core (round 15), kept for the whole world.
 * <pre>
 *   INTACT  --(Shutdown Sequence, 60 s)-->  SHUTDOWN  -->  SUNDERED  --(Pleroma Reconstitutor)-->  REBORN
 * </pre>
 * During the sequence the Transcendental is taken apart from the rim inwards and the Core speaks; at the end everyone
 * inside is carried out, the initiator gets what is left of the Core, and everyone learns The Sundering. While sundered
 * nobody can enter. The Reconstitutor rebuilds it, and the reborn Core has a Forge as well as its Omnipotence.
 */
public final class Omnipotence extends SavedData {
    public enum Phase {INTACT, SHUTDOWN, SUNDERED, REBORN}

    public static final int SEQUENCE_TICKS = 1200;
    public static final ResourceLocation SUNDERING = TheoreticalSciences.id("the_sundering");
    public static final ResourceLocation REKINDLING = TheoreticalSciences.id("the_rekindling");
    private static final int RADIUS = 58, Y_MIN = 66, Y_MAX = 150;

    private Phase phase = Phase.INTACT;
    private long started;
    private UUID initiator;
    private boolean giftPending;

    // ---- the sweep (not saved: a restart mid-sequence just carries on from the elapsed time)
    private List<long[]> columns;
    private int swept;

    public static Omnipotence get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Omnipotence::new, Omnipotence::load, null), "theoreticalsciences_omnipotence");
    }

    public static Phase phase(MinecraftServer server) {
        return get(server).phase;
    }

    public static int secondsLeft(MinecraftServer server) {
        Omnipotence o = get(server);
        if (o.phase != Phase.SHUTDOWN) return 0;
        return (int) Math.max(0, (SEQUENCE_TICKS - (server.overworld().getGameTime() - o.started) + 19) / 20);
    }

    private static Omnipotence load(CompoundTag t, HolderLookup.Provider r) {
        Omnipotence o = new Omnipotence();
        try {
            o.phase = Phase.valueOf(t.getString("Phase"));
        } catch (IllegalArgumentException e) {
            o.phase = Phase.INTACT;
        }
        o.started = t.getLong("Started");
        if (t.hasUUID("Initiator")) o.initiator = t.getUUID("Initiator");
        o.giftPending = t.getBoolean("GiftPending");
        return o;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        t.putString("Phase", phase.name());
        t.putLong("Started", started);
        if (initiator != null) t.putUUID("Initiator", initiator);
        t.putBoolean("GiftPending", giftPending);
        return t;
    }

    // ------------------------------------------------------------------ the sequence
    /** The Core's button. Only an intact Core can be shut down, and only by someone standing at it. */
    public static void beginShutdown(ServerPlayer sp, BlockPos core) {
        if (sp.level().dimension() != QHDimensions.TRANSCENDENTAL || !sp.blockPosition().closerThan(core, 12)
                || !sp.level().getBlockState(core).is(TranscendRegistry.CORE.get())) return;
        Omnipotence o = get(sp.server);
        if (o.phase != Phase.INTACT) return;
        o.phase = Phase.SHUTDOWN;
        o.started = sp.server.overworld().getGameTime();
        o.initiator = sp.getUUID();
        o.columns = null;
        o.swept = 0;
        o.setDirty();
        ServerLevel l = (ServerLevel) sp.level();
        for (ServerPlayer p : l.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.theoreticalsciences.shutdown").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("gui.theoreticalsciences.core_shutdown_running", 60).withStyle(ChatFormatting.GOLD)));
        }
        say(sp.server, o, 60);
        l.playSound(null, core, com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 4F, 0.5F);
        TheoreticalSciences.LOGGER.info("{} began the Omnipotence Core's Shutdown Sequence", sp.getGameProfile().getName());
    }

    private static void say(MinecraftServer server, Omnipotence o, int key) {
        Component msg = Component.translatable("message.theoreticalsciences.shutdown." + key).withStyle(key <= 5 ? ChatFormatting.RED : ChatFormatting.GOLD, ChatFormatting.ITALIC);
        ServerLevel l = server.getLevel(QHDimensions.TRANSCENDENTAL);
        List<ServerPlayer> told = new ArrayList<>();
        if (l != null) for (ServerPlayer p : l.players()) {
            p.sendSystemMessage(msg);
            told.add(p);
        }
        ServerPlayer init = o.initiator == null ? null : server.getPlayerList().getPlayer(o.initiator);
        if (init != null && !told.contains(init)) init.sendSystemMessage(msg);
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        Omnipotence o = get(server);
        if (o.phase == Phase.SHUTDOWN) o.tickShutdown(server);
        else if (o.phase == Phase.SUNDERED && server.getTickCount() % 20 == 0) {
            // nobody may linger in a dimension that no longer exists (e.g. logged out inside it during the sequence)
            ServerLevel l = server.getLevel(QHDimensions.TRANSCENDENTAL);
            if (l != null) for (ServerPlayer p : List.copyOf(l.players())) carryOut(server, p);
        }
    }

    private void tickShutdown(MinecraftServer server) {
        long el = server.overworld().getGameTime() - started;
        ServerLevel l = server.getLevel(QHDimensions.TRANSCENDENTAL);
        if (el % 20 == 0) {
            int s = (int) ((SEQUENCE_TICKS - el) / 20);
            if (s == 50 || s == 40 || s == 30 || s == 20 || s == 10 || (s >= 0 && s <= 5)) say(server, this, s);
            if (l != null) {
                for (ServerPlayer p : l.players()) {
                    p.displayClientMessage(Component.translatable("gui.theoreticalsciences.core_shutdown_running", Math.max(0, s)).withStyle(ChatFormatting.RED), true);
                    // the anomaly engine of the whole place tearing - more and more as the end approaches
                    PacketDistributor.sendToPlayer(p, new com.theoreticalsciences.network.DistortionPayload(8, (float) Math.min(1, 0.15 + el / (double) SEQUENCE_TICKS),
                            TranscendentalBuilder.CORE, 60));
                }
                l.playSound(null, TranscendentalBuilder.CORE, el > SEQUENCE_TICKS - 220 ? com.theoreticalsciences.registry.QHSounds.PULSE.get() : com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(),
                        SoundSource.BLOCKS, 6F, 0.4F + 0.6F * el / SEQUENCE_TICKS);
            }
        }
        if (l != null) {
            if (el == 0 || columns == null) forceChunks(l, true);
            sweep(l, el);
            for (ServerPlayer p : List.copyOf(l.players()))
                if (!p.getAbilities().flying && !p.onGround() && p.fallDistance > 3 && !p.isSpectator()) carryOut(server, p);
        }
        if (el >= SEQUENCE_TICKS) finish(server, l);
    }

    /** Keeps the whole Transcendental loaded while it is taken apart, even if everyone has left. */
    private static void forceChunks(ServerLevel l, boolean on) {
        int c = (RADIUS >> 4) + 1;
        for (int cx = -c; cx <= c; cx++)
            for (int cz = -c; cz <= c; cz++) l.setChunkForced(cx, cz, on);
    }

    /** Takes the Transcendental apart from the rim inwards: by the end of the sequence nothing is left. */
    private void sweep(ServerLevel l, long el) {
        boolean resumed = columns == null && el > 1;
        if (columns == null) {
            columns = new ArrayList<>();
            for (int x = -RADIUS; x <= RADIUS; x++)
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    double r = Math.sqrt(x * x + z * z);
                    if (r <= RADIUS + 0.5) columns.add(new long[]{x, z, (long) (r * 1000)});
                }
            columns.sort((a, b) -> Long.compare(b[2], a[2]));   // rim first
            swept = 0;
        }
        // the last two seconds are the Core itself
        int target = (int) Math.min(columns.size(), columns.size() * Math.min(1.0, el / (double) (SEQUENCE_TICKS - 40)));
        // resuming after a restart: what was swept before is already gone - start just behind the front
        if (resumed) swept = Math.max(0, target - 200);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        while (swept < target) {
            long[] c = columns.get(swept++);
            boolean any = false;
            for (int y = Y_MIN; y <= Y_MAX; y++) {
                m.set(c[0], y, c[1]);
                if (l.getBlockState(m).isAir()) continue;
                if (l.getBlockEntity(m) instanceof net.minecraft.world.Container inv) inv.clearContent();   // the relics go too
                l.setBlock(m, Blocks.AIR.defaultBlockState(), flags);
                any = true;
                if (l.random.nextInt(40) == 0)
                    l.sendParticles(l.random.nextBoolean() ? com.theoreticalsciences.registry.QHParticles.PHOTON.get() : com.theoreticalsciences.registry.QHParticles.BURST.get(), c[0] + 0.5, y + 0.5, c[1] + 0.5, 1, 0.2, 0.2, 0.2, 0.05);
            }
            if (any && l.random.nextInt(12) == 0)
                l.playSound(null, new BlockPos((int) c[0], 100, (int) c[1]), com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.BLOCKS, 3F, 0.3F + l.random.nextFloat() * 0.4F);
        }
    }

    private void finish(MinecraftServer server, ServerLevel l) {
        if (l != null) {
            sweep(l, SEQUENCE_TICKS + 1000);   // whatever is left
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), TranscendentalBuilder.CORE.getX() + 0.5, TranscendentalBuilder.CORE.getY() + 0.5, TranscendentalBuilder.CORE.getZ() + 0.5, 6, 2, 2, 2, 0);
            l.playSound(null, TranscendentalBuilder.CORE, com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.BLOCKS, 10F, 0.3F);
            TranscendentalBuilder.forgetBuilt(l);
            for (ServerPlayer p : List.copyOf(l.players())) carryOut(server, p);
            forceChunks(l, false);
        }
        phase = Phase.SUNDERED;
        giftPending = true;
        columns = null;
        setDirty();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("message.theoreticalsciences.shutdown.world").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            ResearchService.grant(p, SUNDERING);
        }
        ServerPlayer init = initiator == null ? null : server.getPlayerList().getPlayer(initiator);
        if (init != null && init.isAlive()) gift(init);      // dead or away: on respawn / next login
    }

    private static void carryOut(MinecraftServer server, ServerPlayer p) {
        ServerLevel home = server.overworld();
        BlockPos spawn = home.getSharedSpawnPos();
        BlockPos to = com.theoreticalsciences.world.SafeLanding.find(home, spawn.getX(), spawn.getZ(), spawn.getY(), null, null);
        p.changeDimension(new DimensionTransition(home, Vec3.atBottomCenterOf(to), Vec3.ZERO, p.getYRot(), p.getXRot(), DimensionTransition.DO_NOTHING));
        p.fallDistance = 0;
        p.sendSystemMessage(Component.translatable("message.theoreticalsciences.sundered_closed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    /** What is left of the Core goes to whoever shut it down. */
    private void gift(ServerPlayer p) {
        if (!giftPending) return;
        giftPending = false;
        setDirty();
        give(p, new ItemStack(QHItems.get("omnipotence_shard"), 9));
        give(p, new ItemStack(QHItems.get("transcendental_echo")));
        p.sendSystemMessage(Component.translatable("message.theoreticalsciences.shutdown.done").withStyle(ChatFormatting.GOLD));
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Omnipotence o = get(sp.server);
        if (o.phase == Phase.SUNDERED || o.phase == Phase.REBORN) ResearchService.grant(sp, SUNDERING);
        if (o.phase == Phase.REBORN) ResearchService.grant(sp, REKINDLING);
        if (o.giftPending && sp.getUUID().equals(o.initiator) && sp.isAlive()) o.gift(sp);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Omnipotence o = get(sp.server);
        if (o.giftPending && sp.getUUID().equals(o.initiator)) o.gift(sp);
    }

    // ------------------------------------------------------------------ entering
    /** Null if the Transcendental may be entered, else why not. */
    public static Component closedReason(MinecraftServer server) {
        return switch (phase(server)) {
            case SHUTDOWN -> Component.translatable("message.theoreticalsciences.shutdown_closed").withStyle(ChatFormatting.RED);
            case SUNDERED -> Component.translatable("message.theoreticalsciences.sundered_closed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            default -> null;
        };
    }

    // ------------------------------------------------------------------ the Rekindling
    /** The Pleroma Reconstitutor may only run while the Transcendental is gone. */
    public static boolean canRekindle(MinecraftServer server) {
        return phase(server) == Phase.SUNDERED;
    }

    /** The Reconstitutor finished: the Transcendental exists again, and its Core can forge. */
    public static void rekindle(MinecraftServer server) {
        Omnipotence o = get(server);
        if (o.phase != Phase.SUNDERED) return;
        o.phase = Phase.REBORN;
        o.setDirty();
        ServerLevel l = server.getLevel(QHDimensions.TRANSCENDENTAL);
        if (l != null) {
            TranscendentalBuilder.forgetBuilt(l);
            TranscendentalBuilder.ensureBuilt(l);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ResearchService.grant(p, SUNDERING);
            ResearchService.grant(p, REKINDLING);
            p.sendSystemMessage(Component.translatable("message.theoreticalsciences.rekindled").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.theoreticalsciences.rekindled").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.level().playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1F, 0.5F);
        }
    }

    private Omnipotence() {}
}
