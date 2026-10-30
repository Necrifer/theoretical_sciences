package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Round 35: the Euclidean Anchor - an endgame trinket that holds space flat around its bearer. Carried anywhere in the
 * inventory and switched on (use it to switch), the structures of impossible geometry (tesseracts, fractals, Calabi-Yau
 * folds, horizons...) no longer distort the bearer's view (world/zones/StructureZones asks {@link #holdsFlat}).
 * <p>
 * It runs on Eschaton Charge like every endgame item, but only spends it while it is actually holding space flat
 * (inside a distortion zone): {@link #DRAIN} per check, four checks a second.
 */
public class EuclideanAnchorItem extends Item implements EschatonPowered {
    public static final int MAX_CHARGE = 20_000, DRAIN = 1;

    public EuclideanAnchorItem(Properties p) {
        super(p.stacksTo(1).fireResistant());
    }

    public static boolean on(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        @SuppressWarnings("deprecation") boolean off = cd != null && cd.getUnsafe().getBoolean("Off");
        return !off;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean now = !on(s);
            CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean("Off", !now));
            player.displayClientMessage(Component.translatable(now ? "message.theoreticalsciences.euclidean_anchor.on" : "message.theoreticalsciences.euclidean_anchor.off")
                    .withStyle(now ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
            level.playSound(null, player.blockPosition(), now ? com.theoreticalsciences.registry.QHSounds.CONFIRM.get() : com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.PLAYERS, 0.7F, now ? 1.4F : 0.8F);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    /**
     * The player is inside a distortion zone: does an Anchor they carry hold space flat? Spends its charge if so; an
     * empty Anchor (or one switched off) does nothing.
     */
    public static boolean holdsFlat(ServerPlayer sp) {
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!(s.getItem() instanceof EuclideanAnchorItem) || !on(s)) continue;
            if (EschatonCharge.spend(sp, s, DRAIN)) return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tip.add(Component.translatable(on(s) ? "tooltip.theoreticalsciences.euclidean_anchor.on" : "tooltip.theoreticalsciences.euclidean_anchor.off")
                .withStyle(on(s) ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.theoreticalsciences.euclidean_anchor.use").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int maxCharge() {
        return MAX_CHARGE;
    }

    @Override
    public boolean isBarVisible(ItemStack s) {
        return EschatonCharge.barVisible(s);
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return EschatonCharge.barWidth(s);
    }

    @Override
    public int getBarColor(ItemStack s) {
        return EschatonCharge.barColour(s);
    }
}
