package com.theoreticalsciences.transcend;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Round 36: the Forge's materials that the Pleroma Reconstitutor also makes (Omnipotent Ingot, Crystal of
 * Transcendence, Shards, Absolute Singularity, the Echo, Eschaton Cells) stay in the Forge - cheaper there, but slow and
 * fiddly by hand:
 * <ul>
 *   <li>every forging <b>scrambles that recipe's laws</b>: the universe that just converged is spent, and the next one
 *       has new targets to tune onto;</li>
 *   <li>after a forging the Core must <b>re-seed the simulation</b> before that recipe can be compiled again
 *       ({@link #COOLDOWN} ticks).</li>
 * </ul>
 * The Reconstitutor is the machine for making them in quantity. Kept per world (every Core shares the same laws).
 */
public final class ForgeDrift extends SavedData {
    /** The recipes that drift. */
    public static final Set<String> DRIFTING = Set.of("omnipotent_ingot", "transcendence_crystal", "absolute_singularity",
            "resonant_shards", "echo_replay", "eschaton_cell");
    /** Ticks the Core needs after a forging before the same recipe can be compiled again (3 minutes). */
    public static final int COOLDOWN = 3600;

    private final Map<String, Integer> epoch = new HashMap<>();
    private final Map<String, Long> readyAt = new HashMap<>();

    public static boolean drifts(String recipeId) {
        return DRIFTING.contains(recipeId);
    }

    private static ForgeDrift get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ForgeDrift::new, ForgeDrift::load, null), "theoreticalsciences_forge_drift");
    }

    public static int epoch(MinecraftServer s, String id) {
        return get(s).epoch.getOrDefault(id, 0);
    }

    /** Seconds until this recipe may be compiled again (0: now). */
    public static int secondsLeft(MinecraftServer s, String id) {
        long t = get(s).readyAt.getOrDefault(id, 0L) - s.overworld().getGameTime();
        return t <= 0 ? 0 : (int) ((t + 19) / 20);
    }

    /** A drifting recipe was just forged: new laws, and the simulation re-seeds. */
    public static void forged(MinecraftServer s, String id) {
        ForgeDrift d = get(s);
        d.epoch.merge(id, 1, Integer::sum);
        d.readyAt.put(id, s.overworld().getGameTime() + COOLDOWN);
        d.setDirty();
    }

    /** Tells a player's screen the current laws and waits. */
    public static void sync(ServerPlayer sp) {
        StringBuilder b = new StringBuilder();
        for (String id : DRIFTING) {
            if (b.length() > 0) b.append(',');
            b.append(id).append(':').append(epoch(sp.server, id)).append(':').append(secondsLeft(sp.server, id));
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new Sync(b.toString()));
    }

    private static ForgeDrift load(CompoundTag t, HolderLookup.Provider regs) {
        ForgeDrift d = new ForgeDrift();
        CompoundTag e = t.getCompound("Epoch"), r = t.getCompound("ReadyAt");
        for (String k : e.getAllKeys()) d.epoch.put(k, e.getInt(k));
        for (String k : r.getAllKeys()) d.readyAt.put(k, r.getLong(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider regs) {
        CompoundTag e = new CompoundTag(), r = new CompoundTag();
        epoch.forEach(e::putInt);
        readyAt.forEach(r::putLong);
        t.put("Epoch", e);
        t.put("ReadyAt", r);
        return t;
    }

    /** Server -> client: "recipe:epoch:secondsLeft,..." */
    public record Sync(String data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(TheoreticalSciences.id("forge_drift"));
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(4096), Sync::data, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
