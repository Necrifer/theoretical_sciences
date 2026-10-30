package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Round 32: the Eschaton Cell - Eschaton Charge to go. Use it (either hand) and it pours its charge into every endgame
 * item you carry or wear, emptiest first. A cell that is not used up is given back holding what is left.
 */
public class EschatonCellItem extends Item {
    /** How much charge a full cell holds. */
    public static final int CAPACITY = 100_000;

    public EschatonCellItem(Properties p) {
        super(p.fireResistant());
    }

    /** The charge left in a cell (absent: full). */
    public static int stored(ItemStack s) {
        return Mth.clamp(s.getOrDefault(EschatonCharge.CHARGE.get(), CAPACITY), 0, CAPACITY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cell = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(cell);
        // everything that wants charge: main inventory, armour and off-hand, emptiest (by fraction) first
        Inventory inv = player.getInventory();
        List<ItemStack> want = new ArrayList<>();
        for (int k = 0; k < inv.getContainerSize(); k++) {
            ItemStack s = inv.getItem(k);
            if (s.getItem() instanceof EschatonPowered && s.getCount() == 1 && EschatonCharge.get(s) < EschatonCharge.max(s)) want.add(s);
        }
        if (want.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.eschaton_cell.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(cell);
        }
        want.sort(Comparator.comparingDouble(s -> (double) EschatonCharge.get(s) / EschatonCharge.max(s)));
        int pool = stored(cell), start = pool, filled = 0;
        for (ItemStack s : want) {
            if (pool <= 0) break;
            int give = Math.min(pool, EschatonCharge.max(s) - EschatonCharge.get(s));
            EschatonCharge.set(s, EschatonCharge.get(s) + give);
            pool -= give;
            filled++;
        }
        // the cell is spent; whatever it still holds comes back as a partly-filled cell
        if (!player.getAbilities().instabuild) {
            cell.shrink(1);
            if (pool > 0) {
                ItemStack rest = new ItemStack(this);
                rest.set(EschatonCharge.CHARGE.get(), pool);
                if (cell.isEmpty()) player.setItemInHand(hand, rest);
                else if (!inv.add(rest)) player.drop(rest, false);
            }
        }
        player.displayClientMessage(Component.translatable("message.theoreticalsciences.eschaton_cell.used", filled, String.format("%,d", start - pool))
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 1F, 1.4F);
        level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.PLAYERS, 1F, 0.7F);
        if (level instanceof ServerLevel sl)
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.6, 0.4, 0.06);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public boolean isBarVisible(ItemStack s) {
        return stored(s) < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return Math.round(13F * stored(s) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack s) {
        return 0xE0A0FF;
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tip.add(Component.translatable("tooltip.theoreticalsciences.eschaton_cell.holds", String.format("%,d", stored(s)), String.format("%,d", CAPACITY))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable("tooltip.theoreticalsciences.eschaton_cell.use").withStyle(ChatFormatting.GRAY));
    }
}
