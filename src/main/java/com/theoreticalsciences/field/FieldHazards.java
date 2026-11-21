package com.theoreticalsciences.field;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.world.DimensionLaws;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What a projected field does to a person standing in it. Protection is the same kit you would carry into the
 * dimension that has that nature: a Graviton Harness for gravity, a Brane Tether for extra dimensions, a Chronal
 * Anchor for altered time, a Coherence Stabiliser for an altered vacuum.
 * <pre>
 *                  unprotected                                   protected
 *  10 g            Slowness III, no jumping                      Slowness I
 *  100 g           + 1 heart of damage a second                  Slowness II
 *  1000 g          + 4 hearts a second                           Slowness II, Mining Fatigue
 *  0.01 g          you float (no protection needed)
 *  4+ dimensions   Dimensional Bleed (stronger with more)        Mining Fatigue from 7
 *  slow time       Time Dilation III                             Time Dilation I
 *  fast time       Hunger, Temporal Dissonance; ageing at 100x   Hunger
 *  false / true vacuum  Vacuum Decay                             nothing
 * </pre>
 */
public final class FieldHazards {
    private static final ResourceLocation GRAV = TheoreticalSciences.id("field_gravity"), JUMP = TheoreticalSciences.id("field_no_jump");

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 10 != 0) return;
        AttributeInstance grav = sp.getAttribute(Attributes.GRAVITY);
        FieldEnvironment.State s = sp.isCreative() || sp.isSpectator() ? null : FieldEnvironment.projected(sp.level(), sp.blockPosition());
        if (s == null || Double.isNaN(s.gravity())) {
            if (grav != null && grav.hasModifier(GRAV)) grav.removeModifier(GRAV);
            AttributeInstance jump = sp.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null && jump.hasModifier(JUMP)) jump.removeModifier(JUMP);
        }
        if (s == null) return;
        if (!Double.isNaN(s.gravity())) gravity(sp, s.gravity(), grav);
        if (s.dimensions() >= 4) {
            if (!DimensionLaws.carriesAtLeast(sp, "brane_tether")) eff(sp, QHEffects.DIMENSIONAL_BLEED, Math.min(3, (s.dimensions() - 4) / 2));
            else if (s.dimensions() >= 7) eff(sp, MobEffects.DIG_SLOWDOWN, 0);
        }
        if (!Double.isNaN(s.time()) && s.time() != 1) {
            boolean safe = DimensionLaws.carries(sp, "chronal_anchor");
            if (s.time() < 1) eff(sp, QHEffects.TIME_DILATION, safe ? 0 : 2);
            else {
                eff(sp, MobEffects.HUNGER, safe ? 0 : 2);
                if (!safe) {
                    eff(sp, QHEffects.TEMPORAL_DISSONANCE, 0);
                    if (s.time() >= 100 && sp.tickCount % 20 == 0) sp.hurt(sp.damageSources().wither(), 2F);
                }
            }
        }
        if (s.vacuum() >= 2 && !DimensionLaws.carriesAtLeast(sp, "coherence_stabiliser")) eff(sp, QHEffects.VACUUM_DECAY, s.vacuum() - 2);
    }

    static void gravity(ServerPlayer sp, double g, AttributeInstance grav) {
        if (grav != null) {
            double mul = g < 1 ? -0.9 : g >= 10 ? 2.0 : 0;
            if (mul == 0) {
                if (grav.hasModifier(GRAV)) grav.removeModifier(GRAV);
            } else grav.addOrUpdateTransientModifier(new AttributeModifier(GRAV, mul, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (g < 10) {
            AttributeInstance jump = sp.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null && jump.hasModifier(JUMP)) jump.removeModifier(JUMP);
            return;
        }
        boolean safe = DimensionLaws.carries(sp, "graviton_harness");
        if (safe) {
            AttributeInstance jump = sp.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null && jump.hasModifier(JUMP)) jump.removeModifier(JUMP);
            eff(sp, MobEffects.MOVEMENT_SLOWDOWN, g >= 100 ? 1 : 0);
            if (g >= 1000) eff(sp, MobEffects.DIG_SLOWDOWN, 0);
            return;
        }
        eff(sp, MobEffects.MOVEMENT_SLOWDOWN, 2);
        AttributeInstance jump = sp.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.addOrUpdateTransientModifier(new AttributeModifier(JUMP, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (g >= 100 && sp.tickCount % 20 == 0) {
            sp.hurt(sp.damageSources().cramming(), g >= 1000 ? 8F : 2F);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.crushing").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    static void eff(ServerPlayer sp, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> e, int amp) {
        sp.addEffect(new MobEffectInstance(e, 30, amp, true, false, true));
    }

    private FieldHazards() {}
}
