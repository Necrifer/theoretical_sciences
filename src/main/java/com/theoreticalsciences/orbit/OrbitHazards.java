package com.theoreticalsciences.orbit;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.3.0: the laws aboard the Orbital Station, and the station's bookkeeping.
 * <ul>
 *   <li><b>Free fall</b>: a tenth of the Overworld's gravity (the station spins slowly), higher jumps, no fall damage.</li>
 *   <li><b>Vacuum</b> outside the hub: your air runs out in seconds unless you wear a full pressure-type suit (Pressure,
 *   Heated Pressure or Frontier suit, four pieces between them). Inside the hub the air holds.</li>
 *   <li>A <b>Station Module</b> launch by any Rocket Assembly docks one more module (the owner is told how many).</li>
 * </ul>
 * Work per player is a dimension check each tick; the vacuum test is one box test.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class OrbitHazards {
    static final ResourceLocation GRAVITY = TheoreticalSciences.id("orbit_gravity");
    static final double GRAVITY_FACTOR = 0.10, JUMP_FACTOR = 1.0, FALL_FACTOR = 0.0;
    /** The recipe that docks a module (data/theoreticalsciences/recipe/orbital_launch/r103_station_module.json). */
    static final ResourceLocation MODULE_RECIPE = TheoreticalSciences.id("orbital_launch/r103_station_module");
    /** 1.4.0: the same module carried up the Space Elevator's tether. */
    static final ResourceLocation MODULE_LIFT = TheoreticalSciences.id("elevator_lift/r140_station_module");

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        boolean here = p.level().dimension() == QHDimensions.ORBITAL_STATION && !p.isSpectator();
        mod(p.getAttribute(Attributes.GRAVITY), here, GRAVITY_FACTOR - 1);
        mod(p.getAttribute(Attributes.JUMP_STRENGTH), here, JUMP_FACTOR - 1);
        mod(p.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), here, FALL_FACTOR - 1);
        if (!here || p.isCreative()) return;
        if (p.isOnFire()) p.clearFire();                       // nothing burns in vacuum
        if (OrbitBuilder.inside(p.blockPosition()) || suited(p)) return;
        // vacuum: the lungs empty in seconds
        p.setAirSupply(Math.max(-20, p.getAirSupply() - 5));
        if (p.getAirSupply() <= -20) {
            p.setAirSupply(0);
            p.hurt(p.damageSources().drown(), 2.0F);
        }
        if ((p.tickCount + p.getId()) % 40 == 0)
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.orbit_vacuum").withStyle(ChatFormatting.RED), true);
    }

    /** 1.10.0: armour of other mods that seals against vacuum (Ad Astra's space suits by default; packs may add their own). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> VACUUM_SUITS =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, TheoreticalSciences.id("vacuum_suits"));

    static boolean suited(Player p) {
        int n = 0;
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            var st = p.getItemBySlot(slot);
            if (st.is(VACUUM_SUITS) || (st.getItem() instanceof InteriorGear.SuitItem si
                    && (si.suit == InteriorGear.Suit.PRESSURE || si.suit == InteriorGear.Suit.HEATED || si.suit == InteriorGear.Suit.FRONTIER)))
                n++;
        }
        return n >= 4;                                         // any mix of sealed pieces, one per slot
    }

    static void mod(AttributeInstance a, boolean on, double amount) {
        if (a == null) return;
        boolean has = a.hasModifier(GRAVITY);
        if (on && !has) a.addTransientModifier(new AttributeModifier(GRAVITY, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!on && has) a.removeModifier(GRAVITY);
    }

    @SubscribeEvent
    public static void onLaunch(MachineRecipeEvent.Finish e) {
        if (!(MODULE_RECIPE.equals(e.getRecipeId()) || MODULE_LIFT.equals(e.getRecipeId())) || e.getMachine().getLevel() == null || e.getMachine().getLevel().getServer() == null) return;
        var server = e.getMachine().getLevel().getServer();
        int n = OrbitData.get(server).addModule();
        if (e.getMachine().getOwner() != null && server.getPlayerList().getPlayer(e.getMachine().getOwner()) instanceof ServerPlayer owner)
            owner.displayClientMessage(Component.translatable(n >= OrbitData.REQUIRED ? "message.theoreticalsciences.station_complete"
                    : "message.theoreticalsciences.station_module", n, OrbitData.REQUIRED).withStyle(ChatFormatting.AQUA), false);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) {
        e.addListener(Destinations.INSTANCE);
    }

    private OrbitHazards() {}
}
