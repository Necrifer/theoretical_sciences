package com.theoreticalsciences.bio;

import com.theoreticalsciences.item.LoreItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.2.0: an Anomaly Ward, used on yourself. Its id says which infection it guards against: ward_&lt;infection&gt; for three
 * in-game days (renewed too soon, it overloads), lasting_ward_&lt;infection&gt; for good.
 */
public class VaccineItem extends LoreItem {
    public VaccineItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        boolean lasting = id.startsWith("lasting_");
        // 1.2.0: a Mirror Antibody is the ward against the Mirror Plague (no ordinary ward can grip a mirror microbe)
        Immunity.Infection inf = id.equals("mirror_antibody") ? Immunity.Infection.MIRROR_PLAGUE
                : Immunity.Infection.of(id.substring(id.indexOf("ward_") + "ward_".length()));
        if (inf == null) return InteractionResultHolder.fail(stack);
        Immunity.vaccinate(sp, inf, lasting);
        level.playSound(null, sp.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.4F);
        if (!sp.isCreative()) stack.shrink(1);
        sp.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }
}
