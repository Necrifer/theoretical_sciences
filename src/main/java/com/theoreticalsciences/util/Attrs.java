package com.theoreticalsciences.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 1.3.0: setting a transient attribute modifier only when it really changes. Replacing it with a new but equal modifier
 * still marks the attribute dirty, and that sends an attributes packet to the player and everyone tracking them; the
 * gravity and jump laws do it every tick.
 */
public final class Attrs {
    private Attrs() {}

    /** Sets a multiply-total modifier of this amount (0 removes it). */
    public static void mulTotal(AttributeInstance inst, ResourceLocation id, double amount) {
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur != null && cur.amount() == amount && cur.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
