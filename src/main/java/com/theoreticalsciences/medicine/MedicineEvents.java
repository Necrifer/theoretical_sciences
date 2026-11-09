package com.theoreticalsciences.medicine;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** 1.5.0: while a dose protects someone, the effects it treats cannot take hold again. */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class MedicineEvents {
    @SubscribeEvent
    public static void onApplicable(MobEffectEvent.Applicable e) {
        var effect = e.getEffectInstance().getEffect();
        var key = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if (key == null) return;
        String id = key.toString();
        boolean vestibular = id.equals("theoreticalsciences:seismic_vertigo") || effect.is(MobEffects.CONFUSION);
        boolean inflammatory = id.equals("theoreticalsciences:inflammation");
        if ((vestibular && MedicineItem.guarded(e.getEntity(), "vestibular_suppressant"))
                || (inflammatory && MedicineItem.guarded(e.getEntity(), "anti_inflammatory")))
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    private MedicineEvents() {}
}
