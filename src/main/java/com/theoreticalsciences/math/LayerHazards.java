package com.theoreticalsciences.math;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.world.Strata;
import com.theoreticalsciences.world.zones.StructureZones;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.2.0: what the Platonic Realm's new layers do to a visitor. Effects only: they never change a block.
 * <ul>
 *   <li><b>The Axioms</b>, Contradiction: every half minute or so two opposite effects at once (fast and slow, strong and
 *   weak, quick-handed and fumbling) for a few seconds. A Proof Certificate carried keeps you consistent.</li>
 *   <li><b>Curved Space</b>: walk through a Möbius arch and you come out mirrored (left and right swapped) for ten
 *   seconds.</li>
 *   <li><b>The Continuum</b>, Limits: near a Fourier harp, the closer you get the slower you move; you can reach it,
 *   but only just (Zeno).</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class LayerHazards {
    private LayerHazards() {}

    /** Each player's height last tick in the Realm, to tell climbing across a boundary from being carried over it. */
    private static final java.util.Map<java.util.UUID, Double> LAST_Y = new java.util.HashMap<>();

    /**
     * The Infinite is not reached by counting up: a player who flies, climbs or pearls up across Y 1280 is turned back
     * just below it (only the Cantor Key, whose jump is far longer than any step, carries anyone over). Effects and
     * movement only: no barrier blocks are placed. Coming down out of the Infinite is free.
     */
    @SubscribeEvent
    public static void ceiling(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().dimension() != com.theoreticalsciences.world.QHDimensions.PLATONIC) {
            LAST_Y.remove(sp.getUUID());
            return;
        }
        double y = sp.getY();
        Double was = LAST_Y.put(sp.getUUID(), y);
        if (was == null || sp.isCreative() || sp.isSpectator()) return;
        if (was < Strata.Y && y >= Strata.Y && y - was < 24) {
            sp.teleportTo(sp.getX(), Strata.Y - 2.5, sp.getZ());
            sp.setDeltaMovement(sp.getDeltaMovement().x, -0.4, sp.getDeltaMovement().z);
            sp.hurtMarked = true;
            sp.fallDistance = 0;
            LAST_Y.put(sp.getUUID(), Strata.Y - 2.5);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.infinite_ceiling")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @SubscribeEvent
    public static void onLeave(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        LAST_Y.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.isCreative() || sp.isSpectator() || sp.tickCount % 10 != 0) return;
        Strata.Layer layer = Strata.layer(sp.level(), sp.getY());
        if (layer == null) return;
        switch (layer) {
            case AXIOMS -> {
                if (sp.tickCount % 600 != 0 || sp.getInventory().hasAnyMatching(s -> s.is(QHItems.get("proof_certificate")))) return;
                int pick = sp.getRandom().nextInt(3);
                var a = pick == 0 ? MobEffects.MOVEMENT_SPEED : pick == 1 ? MobEffects.DAMAGE_BOOST : MobEffects.DIG_SPEED;
                var b = pick == 0 ? MobEffects.MOVEMENT_SLOWDOWN : pick == 1 ? MobEffects.WEAKNESS : MobEffects.DIG_SLOWDOWN;
                sp.addEffect(new MobEffectInstance(a, 160, 1, false, true));
                sp.addEffect(new MobEffectInstance(b, 160, 1, false, true));
                sp.addEffect(new MobEffectInstance(QHEffects.CONTRADICTION, 160, 0, false, true));
            }
            case CURVED_SPACE -> {
                if (StructureZones.nearest(sp.serverLevel(), sp.blockPosition(), StructureZones.Mode.TWIST) != null)
                    sp.addEffect(new MobEffectInstance(QHEffects.MIRRORED, 200, 0, false, true));
            }
            case CONTINUUM -> {
                var z = StructureZones.nearest(sp.serverLevel(), sp.blockPosition(), StructureZones.Mode.LENS);
                if (z == null) return;
                double d = Math.sqrt(sp.distanceToSqr(z.x() + 0.5, z.y() + 0.5, z.z() + 0.5));
                int amp = Math.max(0, Math.min(3, (int) ((1 - d / z.r()) * 4.5)));        // a quarter, a half... the closer, the slower
                if (d < z.r()) sp.addEffect(new MobEffectInstance(QHEffects.LIMIT, 20, amp, true, true));
            }
            default -> {}
        }
    }
}
