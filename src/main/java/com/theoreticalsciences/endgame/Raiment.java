package com.theoreticalsciences.endgame;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Gifts of the Raiment of Transcendence.
 * <ul>
 *   <li>Crown: night vision and water breathing.</li>
 *   <li>Mantle: immune to fire, all other damage halved.</li>
 *   <li>Greaves: speed and step height (attribute modifiers on the item itself).</li>
 *   <li>Treads: no fall damage, higher jumps (partly attribute modifiers on the item).</li>
 *   <li>Full set: invulnerable to everything that respects invulnerability, flight, no hunger, every dimension's laws held away.</li>
 * </ul>
 * Effects are ambient and hidden, refreshed while worn and taken away only if they are still the ones the Raiment gave.
 * <p>Round 32: every piece runs on Eschaton Charge. A worn piece spends 1 a second, and 10 for each point of damage it
 * keeps away (at most 2000 a blow). A piece with none left is dormant: it counts as not worn at all. Creative players
 * spend nothing.
 */
public final class Raiment {
    private static final String FLIGHT_TAG = "theoreticalsciences_raiment_flight";
    private static final int EFFECT_TICKS = 400;

    /** Wearing that piece, and it is charged (a dormant piece gives nothing). */
    public static boolean wears(LivingEntity e, ArmorItem.Type type) {
        ItemStack s = e.getItemBySlot(type.getSlot());
        return s.getItem() instanceof TranscendenceArmorItem a && a.getType() == type && EschatonCharge.active(e, s);
    }

    private static final String DORMANT_WARNED = "theoreticalsciences_raiment_low";

    /** Round 32: the worn pieces spend their charge over time; a warning when one is nearly empty. */
    private static void drain(ServerPlayer sp) {
        if (EschatonCharge.free(sp) || sp.tickCount % EschatonCharge.ARMOUR_DRAIN_TICKS != 0) return;
        boolean low = false;
        for (ArmorItem.Type t : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            ItemStack s = sp.getItemBySlot(t.getSlot());
            if (!(s.getItem() instanceof TranscendenceArmorItem) || EschatonCharge.get(s) <= 0) continue;
            EschatonCharge.drain(sp, s, EschatonCharge.ARMOUR_DRAIN);
            if (EschatonCharge.get(s) < EschatonCharge.max(s) / 50) low = true;          // under 2%
        }
        CompoundTag data = sp.getPersistentData();
        if (low && Math.abs(sp.tickCount - data.getInt(DORMANT_WARNED)) > 600) {      // every 30 s at most
            data.putInt(DORMANT_WARNED, sp.tickCount);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.raiment.fading")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
        }
    }

    /** The cost of a blow the Raiment kept away, shared by the pieces that did it. */
    private static void pay(LivingEntity e, float amount, ArmorItem.Type... pieces) {
        if (EschatonCharge.free(e) || pieces.length == 0) return;
        int cost = (int) Math.min(EschatonCharge.ARMOUR_HIT_MAX, Math.ceil(amount * EschatonCharge.ARMOUR_PER_DAMAGE));
        int each = Math.max(1, cost / pieces.length);
        for (ArmorItem.Type t : pieces) EschatonCharge.drain(e, e.getItemBySlot(t.getSlot()), each);
    }

    private static final ArmorItem.Type[] ALL = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    public static boolean wearsFullSet(LivingEntity e) {
        return wears(e, ArmorItem.Type.HELMET) && wears(e, ArmorItem.Type.CHESTPLATE)
                && wears(e, ArmorItem.Type.LEGGINGS) && wears(e, ArmorItem.Type.BOOTS);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        drain(sp);
        boolean crown = wears(sp, ArmorItem.Type.HELMET), mantle = wears(sp, ArmorItem.Type.CHESTPLATE);
        boolean full = crown && mantle && wears(sp, ArmorItem.Type.LEGGINGS) && wears(sp, ArmorItem.Type.BOOTS);

        if (sp.tickCount % 20 == 0) {
            gift(sp, MobEffects.NIGHT_VISION, crown);
            gift(sp, MobEffects.WATER_BREATHING, crown);
            gift(sp, MobEffects.FIRE_RESISTANCE, mantle);
        }
        if (mantle && sp.isOnFire()) sp.clearFire();

        if (full) {
            if (sp.getFoodData().getFoodLevel() < 20) sp.getFoodData().setFoodLevel(20);
            if (sp.getFoodData().getSaturationLevel() < 5) sp.getFoodData().setSaturation(5);
            sp.getFoodData().setExhaustion(0);
            if (sp.hasEffect(MobEffects.HUNGER)) sp.removeEffect(MobEffects.HUNGER);
        }

        if (sp.tickCount % 10 != 0) return;
        CompoundTag data = sp.getPersistentData();
        if (full && !sp.getAbilities().mayfly) {
            sp.getAbilities().mayfly = true;
            sp.onUpdateAbilities();
            data.putBoolean(FLIGHT_TAG, true);
        } else if (!full && data.getBoolean(FLIGHT_TAG)) {
            data.remove(FLIGHT_TAG);
            // still carrying the Omnilex: it keeps the flight going (hand the grant over, no drop out of the air)
            if (com.theoreticalsciences.machine.ControllerBlockEntity.carriesOmnilex(sp)) {
                data.putBoolean("theoreticalsciences_omnilex_flight", true);
                return;
            }
            if (!sp.isCreative() && !sp.isSpectator()) {
                // round 32: the set went dormant mid-flight - let its wearer down gently
                if (sp.getAbilities().flying) sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                sp.getAbilities().mayfly = false;
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            }
        }
    }

    /** Keep an effect topped up while worn; remove it when the piece is off, but only if it looks like ours. */
    private static void gift(ServerPlayer sp, Holder<MobEffect> effect, boolean worn) {
        MobEffectInstance cur = sp.getEffect(effect);
        boolean ours = cur != null && cur.isAmbient() && !cur.isVisible() && cur.getAmplifier() == 0 && cur.getDuration() <= EFFECT_TICKS;
        if (worn) {
            if (cur == null || (ours && cur.getDuration() < EFFECT_TICKS - 100))
                sp.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, false));
        } else if (ours) sp.removeEffect(effect);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity target = e.getEntity();
        if (target.level().isClientSide) return;
        boolean bypass = e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypass && wearsFullSet(target)) {
            pay(target, e.getAmount(), ALL);
            e.setCanceled(true);
            return;
        }
        if (wears(target, ArmorItem.Type.CHESTPLATE)) {
            if (!bypass && e.getSource().is(DamageTypeTags.IS_FIRE)) {
                pay(target, e.getAmount(), ArmorItem.Type.CHESTPLATE);
                e.setCanceled(true);
                return;
            }
            pay(target, e.getAmount() * 0.5F, ArmorItem.Type.CHESTPLATE);
            e.setAmount(e.getAmount() * 0.5F);
        }
        if (!bypass && wears(target, ArmorItem.Type.BOOTS) && e.getSource().is(DamageTypeTags.IS_FALL)) {
            pay(target, e.getAmount(), ArmorItem.Type.BOOTS);
            e.setCanceled(true);
        }
    }

    public static void onFall(LivingFallEvent e) {
        if (wears(e.getEntity(), ArmorItem.Type.BOOTS)) e.setDamageMultiplier(0);
    }

    /** Used by world.DimensionLaws: the full set holds every dimension's laws away. */
    public static boolean transcends(Player p) {
        return wearsFullSet(p);
    }

    private Raiment() {}
}
