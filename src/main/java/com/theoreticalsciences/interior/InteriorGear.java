package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * 1.3.7: the counters to the Interior's hazards, each a suit of four pieces.
 * <ul>
 *   <li><b>Refractory Suit</b> - corundum and moissanite over basalt fibre. A full suit is magma-proof (tag
 *   {@code theoreticalsciences:magma_proof}) and shrugs off every kind of fire: Basaltic Magma, Deep Brine, lava, burning.</li>
 *   <li><b>Pressure Suit</b> - a high-pressure shell of bridgmanite and iron. Each piece lowers Lithostatic Pressure by a
 *   level; a full suit cancels it at every depth.</li>
 *   <li>(1.3.8: the Lead-Lined Suit is gone - any armour can now carry {@link RadiationPlating}.)</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class InteriorGear {
    static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, TheoreticalSciences.MODID);

    private static Holder<ArmorMaterial> material(String id, int h, int c, int l, int b, float tough, float kb, String repair) {
        return MATERIALS.register(id, () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
            m.put(ArmorItem.Type.HELMET, h);
            m.put(ArmorItem.Type.CHESTPLATE, c);
            m.put(ArmorItem.Type.LEGGINGS, l);
            m.put(ArmorItem.Type.BOOTS, b);
            m.put(ArmorItem.Type.BODY, c);
        }), 12, com.theoreticalsciences.registry.QHSounds.EQUIP, () -> Ingredient.of(com.theoreticalsciences.registry.QHItems.get(repair)),
                List.of(new ArmorMaterial.Layer(TheoreticalSciences.id(id))), tough, kb));
    }

    public enum Suit {
        REFRACTORY("refractory_suit", 3, 7, 6, 3, 2F, 0.1F, "corundum"),
        PRESSURE("pressure_suit", 3, 8, 6, 3, 3F, 0.15F, "bridgmanite"),
        /** 1.10.0: a thin bumper that shatters micrometeorites before they reach the wall behind it (the Asteroid Belt). */
        WHIPPLE("whipple_shield", 2, 6, 5, 2, 1F, 0F, "ceramic_armour_plate"),   // 1.2.0: mended with its own ceramic
        /** 1.10.1: one piece for each of the Frontier's other hazards (see FrontierHazards). */
        FRONTIER("frontier_suit", 3, 7, 6, 3, 2F, 0.05F, "station_hull"),
        /** 1.2.0: a Pressure Suit lined with aerogel and threaded with heating wire, against Titan's 94 K (see TitanHazards). */
        HEATED("heated_pressure_suit", 3, 8, 6, 3, 3F, 0.15F, "silica_aerogel");

        public final String id;
        final Holder<ArmorMaterial> material;

        Suit(String id, int h, int c, int l, int b, float tough, float kb, String repair) {
            this.id = id;
            this.material = material(id, h, c, l, b, tough, kb, repair);
        }
    }

    static String piece(ArmorItem.Type t) {
        return switch (t) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            default -> "boots";
        };
    }

    public static class SuitItem extends ArmorItem {
        final Suit suit;

        SuitItem(Suit suit, Type type) {
            super(suit.material, type, new Item.Properties().stacksTo(1).durability(type.getDurability(30)).fireResistant());
            this.suit = suit;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.theoreticalsciences." + suit.id).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            if (suit == Suit.FRONTIER)
                tip.add(Component.translatable("item.theoreticalsciences." + suit.id + "_" + piece(getType()) + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static int pieces(LivingEntity e, Suit s) {
        int n = 0;
        for (EquipmentSlot slot : SLOTS)
            if (e.getItemBySlot(slot).getItem() instanceof SuitItem si && si.suit == s) n++;
        return n;
    }

    /** Whether the player wears this one piece of a suit. */
    public static boolean wears(LivingEntity e, Suit s, EquipmentSlot slot) {
        return e.getItemBySlot(slot).getItem() instanceof SuitItem si && si.suit == s;
    }

    /** What part of a radiation dose still reaches this player through the radiation plating on their armour (1.3.8). */
    public static double radiationFactor(Player p) {
        return RadiationPlating.factor(p);
    }

    /** Levels of Lithostatic Pressure the player's pressure suit takes off. */
    public static int pressureRelief(Player p) {
        int n = pieces(p, Suit.PRESSURE) + pieces(p, Suit.HEATED);     // 1.2.0: the heated suit is still a pressure suit underneath
        return n >= 4 ? 99 : n;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || (p.tickCount + p.getId()) % 10 != 0) return;
        if (pieces(p, Suit.REFRACTORY) == 4 && p.isOnFire()) p.clearFire();
        if (pressureRelief(p) >= 99 && p.hasEffect(QHEffects.LITHOSTATIC_PRESSURE)) p.removeEffect(QHEffects.LITHOSTATIC_PRESSURE);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        LivingEntity le = e.getEntity();
        if (pieces(le, Suit.REFRACTORY) == 4 && (e.getSource().is(DamageTypeTags.IS_FIRE) || e.getSource().is(com.theoreticalsciences.block.FluidHazard.MAGMA)))
            e.setCanceled(true);
    }

    private InteriorGear() {}
}
