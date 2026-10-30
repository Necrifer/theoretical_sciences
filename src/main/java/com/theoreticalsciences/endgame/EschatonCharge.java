package com.theoreticalsciences.endgame;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Round 32: Eschaton Charge - what every endgame tool, weapon and piece of armour now runs on.
 * <p>
 * The charge is kept on the item itself (the {@code theoreticalsciences:eschaton_charge} data component). An item that
 * has never had the component is full: everything comes out of the Core Forge charged. Using an item spends charge;
 * at zero it is <i>dormant</i> and does nothing special until an Eschaton Cell fills it again. Players in creative
 * mode never spend any, and their items work even when empty.
 */
public final class EschatonCharge {
    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TheoreticalSciences.MODID);

    /** The charge left on an item (absent: full). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE = COMPONENTS.registerComponentType("eschaton_charge",
            b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

    // ---- what things cost (Eschaton Charge)
    public static final int EDGE_HIT = 20, EDGE_ERASURE = 400, EDGE_STASIS = 600, EDGE_HARVEST = 5;
    public static final int CENSER_TOUCH = 200, CENSER_CLOUD = 400;
    public static final int HAND_ACTION = 250;
    public static final int CODEX_LAW = 500;
    public static final int EXCAVATOR_BLOCK = 5, EXCAVATOR_ORE = 20;
    /** Each worn piece of the Raiment spends this much every {@link #ARMOUR_DRAIN_TICKS} ticks (1 per second). */
    public static final int ARMOUR_DRAIN = 5, ARMOUR_DRAIN_TICKS = 100;
    /** ...and this much per point of damage it keeps away (at most {@link #ARMOUR_HIT_MAX} per blow). */
    public static final int ARMOUR_PER_DAMAGE = 10, ARMOUR_HIT_MAX = 2000;

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    public static int max(ItemStack s) {
        return s.getItem() instanceof EschatonPowered p ? p.maxCharge() : 0;
    }

    public static int get(ItemStack s) {
        int max = max(s);
        return Math.min(max, s.getOrDefault(CHARGE.get(), max));
    }

    public static void set(ItemStack s, int v) {
        s.set(CHARGE.get(), Mth.clamp(v, 0, max(s)));
    }

    /** Creative (and spectator) players are never drained, and use their items even when empty. */
    public static boolean free(LivingEntity e) {
        return e instanceof Player p && (p.getAbilities().instabuild || p.isSpectator());
    }

    /** Does the item work in this holder's hands (or on their body)? */
    public static boolean active(LivingEntity holder, ItemStack s) {
        return free(holder) || get(s) > 0;
    }

    /**
     * Spend {@code cost}: true if there was enough (nothing is taken if not). Creative players always succeed for free.
     */
    public static boolean spend(LivingEntity holder, ItemStack s, int cost) {
        if (free(holder)) return true;
        int c = get(s);
        if (c < cost || c <= 0) return false;
        set(s, c - cost);
        return true;
    }

    /** Spend up to {@code cost}, as much as there is (for drains that should empty the item rather than stop early). */
    public static void drain(LivingEntity holder, ItemStack s, int cost) {
        if (free(holder) || cost <= 0) return;
        int c = get(s);
        if (c > 0) set(s, c - Math.min(c, cost));
    }

    /** "The X is dormant: it needs Eschaton Charge." on the action bar. */
    public static void sayDormant(Player p, ItemStack s) {
        p.displayClientMessage(Component.translatable("message.theoreticalsciences.eschaton.dormant", s.getHoverName()).withStyle(ChatFormatting.RED), true);
    }

    /** Spend, or tell the player why not. */
    public static boolean spendOrSay(Player p, ItemStack s, int cost) {
        if (spend(p, s, cost)) return true;
        if (!p.level().isClientSide) {
            if (get(s) <= 0) sayDormant(p, s);
            else p.displayClientMessage(Component.translatable("message.theoreticalsciences.eschaton.too_little", cost, get(s)).withStyle(ChatFormatting.RED), true);
        }
        return false;
    }

    // ---- the bar under the item (shown while it is not full)
    public static boolean barVisible(ItemStack s) {
        return get(s) < max(s);
    }

    public static int barWidth(ItemStack s) {
        int max = max(s);
        return max <= 0 ? 0 : Math.round(13F * get(s) / max);
    }

    /** From deep red when nearly empty, through gold, to a pale violet-white when full. */
    public static int barColour(ItemStack s) {
        int max = max(s);
        float f = max <= 0 ? 0 : (float) get(s) / max;
        return f < 0.5F ? Mth.hsvToRgb(Mth.lerp(f * 2, 0.98F, 1.11F) % 1F, 0.9F, 1F) : Mth.hsvToRgb(Mth.lerp((f - 0.5F) * 2, 0.13F, 0.80F), Mth.lerp((f - 0.5F) * 2, 0.8F, 0.35F), 1F);
    }

    /** Every endgame item's tooltip: how much charge it holds, or that it is dormant. */
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!(s.getItem() instanceof EschatonPowered)) return;
        int c = get(s), max = max(s);
        int at = Math.min(1, e.getToolTip().size());          // right under the name
        if (c <= 0) e.getToolTip().add(at, Component.translatable("tooltip.theoreticalsciences.eschaton.dormant").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        e.getToolTip().add(at, Component.translatable("tooltip.theoreticalsciences.eschaton.charge", String.format("%,d", c), String.format("%,d", max))
                .withStyle(c <= 0 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE));
    }

    private EschatonCharge() {}
}
