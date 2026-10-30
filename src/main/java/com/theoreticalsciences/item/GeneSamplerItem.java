package com.theoreticalsciences.item;

import com.theoreticalsciences.registry.QHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Takes a genome sample from any living creature: a Genome Sample from creatures of our world, a Xenogenome Sample
 * from the creatures of the mod's other dimensions (their code is not DNA). It pricks for half a heart.
 */
public class GeneSamplerItem extends LoreItem {
    public GeneSamplerItem(Properties p) {
        super(p.stacksTo(1).durability(256));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (target instanceof Player || !target.isAlive()) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.sample_cooldown").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        boolean xeno = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getNamespace().equals(com.theoreticalsciences.TheoreticalSciences.MODID);
        ItemStack sample = new ItemStack(QHItems.get(xeno ? "xenogenome_sample" : "genome_sample"));
        // round 21: the sample remembers whose it is (a Breached Anomaly Engine can grow that creature from it)
        net.minecraft.nbt.CompoundTag sp = new net.minecraft.nbt.CompoundTag();
        sp.putString("Species", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
        sample.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(sp));
        if (!player.getInventory().add(sample)) player.drop(sample, false);
        target.hurt(player.damageSources().playerAttack(player), 1.0F);
        ((ServerLevel) player.level()).sendParticles(xeno ? com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get() : com.theoreticalsciences.registry.QHParticles.RESOLVE.get(), target.getX(), target.getY() + target.getBbHeight() / 2,
                target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        player.level().playSound(null, target.blockPosition(), com.theoreticalsciences.registry.QHSounds.FLUID_FILL.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.translatable(xeno ? "message.theoreticalsciences.sampled_xeno" : "message.theoreticalsciences.sampled", target.getName())
                .withStyle(xeno ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN), true);
        player.getCooldowns().addCooldown(this, 30);
        if (!player.isCreative()) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResult.CONSUME;
    }
}
