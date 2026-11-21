package com.theoreticalsciences.custom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

/**
 * Round 31: the three pack-editable data folders (component tiers, Core Forge recipes, machine parameters) - loading
 * them on /reload and sending them to the players.
 * <p>
 * How it reaches a client: once while the player is still joining (so JEI and the Forge screen have it before they
 * build their lists), and again after every /reload. A client that leaves a server forgets that server's data.
 */
public final class PackData {
    /** Bumped every time any of the data changes (machines in the world notice it and re-apply their numbers). */
    private static volatile int generation;

    public static int generation() {
        return generation;
    }

    static void changed() {
        generation++;
    }

    /** Server -> client: everything in one message, as JSON text. */
    public record Payload(String json) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(TheoreticalSciences.id("pack_data"));
        public static final StreamCodec<ByteBuf, Payload> CODEC = ByteBufCodecs.stringUtf8(1 << 22).map(Payload::new, Payload::json);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Sent during the join ("configuration") phase, like the singularity list. */
    public record ConfigTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(TheoreticalSciences.id("pack_data"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            try {
                sender.accept(payload());
            } finally {
                listener.finishCurrentTask(TYPE);
            }
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }

    public static Payload payload() {
        // each part is the raw files (id -> JSON); the client reads them with the same code the server used
        return new Payload("{\"tiers\":" + ComponentTiers.syncJson() + ",\"forge\":" + ForgeRecipes.syncJson()
                + ",\"machines\":" + MachineParams.syncJson() + "}");
    }

    /** Client: the server's data arrived. */
    public static void receive(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            ComponentTiers.load(o.has("tiers") ? o.getAsJsonObject("tiers") : new JsonObject());
            ForgeRecipes.load(o.has("forge") ? o.getAsJsonObject("forge") : new JsonObject());
            MachineParams.load(o.has("machines") ? o.getAsJsonObject("machines") : new JsonObject());
        } catch (RuntimeException e) {
            TheoreticalSciences.LOGGER.error("Bad pack data from the server: {}", e.getMessage());
        }
    }

    /** Client: left the server (the next one may have other data, or none). */
    public static void clearClient() {
        ComponentTiers.clear();
        ForgeRecipes.clear();
        MachineParams.clear();
    }

    public static void registerPayload(PayloadRegistrar r) {
        r.commonToClient(Payload.TYPE, Payload.CODEC, (p, ctx) -> receive(p.json()));
    }

    /** Hooks everything up (called from the mod's constructor). */
    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterConfigurationTasksEvent e) -> {
            if (e.getListener().hasChannel(Payload.TYPE)) e.register(new ConfigTask(e.getListener()));
        });
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> {
            e.addListener(ComponentTiers.INSTANCE);
            e.addListener(ForgeRecipes.INSTANCE);
            e.addListener(MachineParams.INSTANCE);
        });
        // fired for a player who joins (getPlayer) and for everyone after /reload (getRelevantPlayers covers both)
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> {
            Payload p = payload();
            e.getRelevantPlayers().forEach(sp -> {
                if (sp.connection.hasChannel(Payload.TYPE)) PacketDistributor.sendToPlayer(sp, p);
            });
        });
    }

    private PackData() {}
}
