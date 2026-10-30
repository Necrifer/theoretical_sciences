package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;

import java.util.function.Consumer;

/**
 * Round 24: the singularity list (pack-added plus one for every c:ingots item) is sent while the player is still
 * joining - before the client receives the recipes and JEI builds its lists. Sent only in the play phase (as before),
 * it arrived after JEI had already listed the Omniscience Catalyst with no singularities in it.
 */
public record SingularityConfigTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {
    public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(TheoreticalSciences.id("singularities"));

    public static void register(net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent e) {
        if (e.getListener().hasChannel(SingularityDefsPayload.TYPE)) e.register(new SingularityConfigTask(e.getListener()));
    }

    @Override
    public void run(Consumer<CustomPacketPayload> sender) {
        try {
            sender.accept(new SingularityDefsPayload(com.theoreticalsciences.extreme.Singularities.syncJson()));
        } finally {
            listener.finishCurrentTask(TYPE);
        }
    }

    @Override
    public ConfigurationTask.Type type() {
        return TYPE;
    }
}
