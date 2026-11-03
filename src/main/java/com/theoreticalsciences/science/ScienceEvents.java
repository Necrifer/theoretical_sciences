package com.theoreticalsciences.science;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Round 38 (1.2): small world rules of the new sciences. */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class ScienceEvents {
    /** An Atomic Clock carried in the off hand keeps your own time in step: it clears Time Dilation. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 5) return;
        if (p.hasEffect(QHEffects.TIME_DILATION) && p.getOffhandItem().is(QHItems.get("atomic_clock"))) p.removeEffect(QHEffects.TIME_DILATION);
    }

    private ScienceEvents() {}
}
