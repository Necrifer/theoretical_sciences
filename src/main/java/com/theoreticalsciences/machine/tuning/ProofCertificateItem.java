package com.theoreticalsciences.machine.tuning;

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
 * 1.2.0: a Proof Certificate, the first applied mathematics: a checked proof that the machine stays inside its limits.
 * Used on a multiblock's controller it is filed with the machine (up to four); each one catches one containment
 * failure, so nothing is released (the machine still shuts down).
 */
public class ProofCertificateItem extends LoreItem {
    public ProofCertificateItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || c.tuningProfile() == null) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.proof_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        if (c.proofs >= ControllerBlockEntity.MAX_PROOFS) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.proof_full", c.proofs).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        c.proofs++;
        c.setChanged();
        if (!sp.isCreative()) ctx.getItemInHand().shrink(1);
        level.playSound(null, c.getBlockPos(), SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1F, 1.1F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.proof_filed", c.proofs, ControllerBlockEntity.MAX_PROOFS)
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }
}
