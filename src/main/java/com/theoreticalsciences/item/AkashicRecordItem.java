package com.theoreticalsciences.item;

import com.theoreticalsciences.research.ResearchManager;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.ChatFormatting;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Akashic Record (creative only): every research entry, and every symbol, learned at once. Like Laplace's Demon and
 * the Oracle's Lemma it cannot be crafted, cannot come out of the Omnipotence Core, and does nothing outside creative.
 */
public class AkashicRecordItem extends LoreItem {
    public AkashicRecordItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (!sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.akashic_creative").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        int n = ResearchService.grantAll(sp, List.copyOf(ResearchManager.INSTANCE.entries().keySet()));
        ResearchService.learnAllGlyphs(sp);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.akashic_done", n).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        level.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 1F, 0.6F);
        return InteractionResultHolder.consume(stack);
    }
}
