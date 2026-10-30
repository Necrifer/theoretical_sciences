package com.theoreticalsciences.logistics;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * Links Entangled Transceivers. Use on a transceiver to copy its frequency (or give it a fresh one); use on another to
 * tune it to the same channel. Sneak-use on a transceiver switches it between sending and receiving. Sneak-use in the
 * air forgets the stored frequency.
 */
public class EntanglementTunerItem extends Item {
    public EntanglementTunerItem(Properties p) {
        super(p.stacksTo(1));
    }

    static int freq(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getInt("Frequency");
    }

    static void setFreq(ItemStack s, int f) {
        CompoundTag t = new CompoundTag();
        t.putInt("Frequency", f);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level l = ctx.getLevel();
        if (!(l.getBlockEntity(ctx.getClickedPos()) instanceof TransceiverBlockEntity be)) return InteractionResult.PASS;
        if (l.isClientSide) return InteractionResult.SUCCESS;
        Player p = ctx.getPlayer();
        ItemStack s = ctx.getItemInHand();
        if (p != null && p.isShiftKeyDown()) {
            var st = l.getBlockState(ctx.getClickedPos()).cycle(BlockStateProperties.ENABLED);
            l.setBlock(ctx.getClickedPos(), st, 3);
            p.displayClientMessage(Component.translatable(st.getValue(BlockStateProperties.ENABLED) ? "message.theoreticalsciences.tx_send" : "message.theoreticalsciences.tx_receive")
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        int f = freq(s);
        if (f == 0) {
            f = be.frequency != 0 ? be.frequency : 1 + l.random.nextInt(Integer.MAX_VALUE - 1);
            setFreq(s, f);
            if (be.frequency == 0) be.setFrequency(f);
            if (p != null) p.displayClientMessage(Component.translatable("message.theoreticalsciences.tx_copied", Integer.toHexString(f).toUpperCase()).withStyle(ChatFormatting.AQUA), true);
        } else {
            be.setFrequency(f);
            if (p != null) p.displayClientMessage(Component.translatable("message.theoreticalsciences.tx_linked", Integer.toHexString(f).toUpperCase()).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (p.isShiftKeyDown() && freq(s) != 0) {
            if (!l.isClientSide) {
                s.remove(DataComponents.CUSTOM_DATA);
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.tx_cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
        }
        return InteractionResultHolder.pass(s);
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int f = freq(s);
        tip.add(f == 0 ? Component.translatable("tooltip.theoreticalsciences.tuner_empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.theoreticalsciences.tuner_freq", Integer.toHexString(f).toUpperCase()).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return freq(s) != 0;
    }
}
