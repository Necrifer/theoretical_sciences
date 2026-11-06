package com.theoreticalsciences.bio;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 1.2.0: cleaning a Biology console. Used on one, a <b>Sterile Pack</b> (from the Autoclave) always brings it back to fully
 * sterile and clears a contamination. An <b>Antibiotic</b> is cheaper, but each dose selects for whatever survives it:
 * the n-th dose on the same machine works with chance 1/n, and a failed dose is used up all the same. Used on a machine
 * that is not contaminated, either one only reports how clean it is (and is not used).
 */
public class CleaningItem extends LoreItem {
    final boolean antibiotic;

    public CleaningItem(Properties p, boolean antibiotic) {
        super(p);
        this.antibiotic = antibiotic;
    }

    public static CleaningItem sterilePack(Properties p) {
        return new CleaningItem(p, false);
    }

    public static CleaningItem antibiotic(Properties p) {
        return new CleaningItem(p, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || !c.type.biology()) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.clean_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        int pct = Math.round(c.sterility * 100);
        if (!c.contaminated && (pct >= 100 || !c.contaminates())) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.clean_already", pct).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.FAIL;
        }
        boolean ok = c.clean(antibiotic, level.random);
        if (!sp.isCreative()) ctx.getItemInHand().shrink(1);
        level.playSound(null, c.getBlockPos(), ok ? SoundEvents.BREWING_STAND_BREW : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
        sp.displayClientMessage(Component.translatable(ok ? "message.theoreticalsciences.cleaned" : "message.theoreticalsciences.resistant", c.resistance)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return InteractionResult.CONSUME;
    }
}
