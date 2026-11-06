package com.theoreticalsciences.client.math;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** 1.2.0: Mirrored (a Möbius arch in Curved Space): left and right are swapped. */
@EventBusSubscriber(modid = TheoreticalSciences.MODID, value = Dist.CLIENT)
public final class MirroredInput {
    private MirroredInput() {}

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!e.getEntity().hasEffect(QHEffects.MIRRORED)) return;
        var in = e.getInput();
        in.leftImpulse = -in.leftImpulse;
        boolean l = in.left;
        in.left = in.right;
        in.right = l;
    }
}
