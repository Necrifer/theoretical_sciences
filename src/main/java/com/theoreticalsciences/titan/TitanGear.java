package com.theoreticalsciences.titan;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.science.Wear;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 1.2.0: what you carry against Titan.
 * <ul>
 *   <li><b>Oxygen Rebreather</b> - twenty minutes of oxygen, a second at a time, from anywhere in your inventory. Spent, it
 *   is refilled by crafting it with an Oxygen Gas Cell.</li>
 *   <li><b>Hydrophobic Coating</b> - used, it coats every piece of armour you are wearing that is not coated yet, one can
 *   a piece. Methane rain runs off a coated piece instead of soaking through its insulation.</li>
 * </ul>
 * The Heated Pressure Suit is a suit like the others ({@link com.theoreticalsciences.interior.InteriorGear.Suit#HEATED}).
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class TitanGear {
    /** Seconds of oxygen in a full rebreather. */
    public static final int REBREATHER_SECONDS = 1200;

    public static class RebreatherItem extends com.theoreticalsciences.item.LoreItem {
        public RebreatherItem(Properties p) {
            super(p.durability(REBREATHER_SECONDS));
        }

        @Override
        public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
            return false;
        }

        @Override
        public boolean isEnchantable(ItemStack stack) {
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            int left = stack.getMaxDamage() - stack.getDamageValue();
            tip.add(Component.translatable("tooltip.theoreticalsciences.rebreather_left", left / 60, String.format("%02d", left % 60))
                    .withStyle(left > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        }
    }

    /** The first rebreather with oxygen left in this player's inventory (off hand first), or empty. */
    public static ItemStack rebreather(Player p) {
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof RebreatherItem && off.getDamageValue() < off.getMaxDamage()) return off;
        for (ItemStack s : p.getInventory().items)
            if (s.getItem() instanceof RebreatherItem && s.getDamageValue() < s.getMaxDamage()) return s;
        return ItemStack.EMPTY;
    }

    public static class CoatingItem extends com.theoreticalsciences.item.LoreItem {
        public CoatingItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            int done = 0;
            for (EquipmentSlot slot : SLOTS) {
                if (st.isEmpty()) break;
                ItemStack armour = player.getItemBySlot(slot);
                if (armour.isEmpty() || !(armour.getItem() instanceof ArmorItem) || armour.has(Wear.COATED.get())) continue;
                if (!level.isClientSide) {
                    armour.set(Wear.COATED.get(), net.minecraft.util.Unit.INSTANCE);
                    st.consume(1, player);
                }
                done++;
            }
            if (done == 0) {
                if (player instanceof ServerPlayer sp)
                    sp.displayClientMessage(Component.translatable("message.theoreticalsciences.coating_none").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            if (player instanceof ServerPlayer sp) {
                level.playSound(null, sp.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8F, 1.4F);
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.coating_done", done).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        }
    }

    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** How many worn pieces carry the coating. */
    public static int coated(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot s : SLOTS) if (e.getItemBySlot(s).has(Wear.COATED.get())) n++;
        return n;
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        if (e.getItemStack().has(Wear.COATED.get()))
            e.getToolTip().add(1, Component.translatable("tooltip.theoreticalsciences.coated").withStyle(ChatFormatting.AQUA));
    }

    private TitanGear() {}
}
