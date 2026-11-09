package com.theoreticalsciences.medicine;

import com.theoreticalsciences.item.LoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.5.0: a medicine with a dosing window (pharmacology: the Hill curve). One dose cures what it treats and keeps it off
 * for a while; a second dose before the first has cleared (within its window) is an overdose: slowness and poison, and no
 * second cure. Every dose shows when it is safe to take the next.
 */
public class MedicineItem extends LoreItem {
    final List<ResourceLocation> treats;
    final List<Holder<MobEffect>> vanilla;
    final int guardTicks, windowTicks;
    final String key;

    public MedicineItem(Properties p, String key, List<ResourceLocation> treats, List<Holder<MobEffect>> vanilla, int guardTicks, int windowTicks) {
        super(p);
        this.key = key;
        this.treats = treats;
        this.vanilla = vanilla;
        this.guardTicks = guardTicks;
        this.windowTicks = windowTicks;
    }

    /** Cures vertigo, the Seismic Vertigo of the Planetary Interior and the Dreaming Colony's daze. */
    public static MedicineItem vestibular(Properties p) {
        return new MedicineItem(p, "vestibular_suppressant", List.of(ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "seismic_vertigo")),
                List.of(MobEffects.CONFUSION), 3600, 2400);
    }

    /** Cures inflammation and the Inner Space hazards that ride on it; eases weakness. */
    public static MedicineItem antiInflammatory(Properties p) {
        return new MedicineItem(p, "anti_inflammatory", List.of(ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "inflammation")),
                List.of(MobEffects.WEAKNESS), 3600, 2400);
    }

    @Override
    public int getUseDuration(ItemStack s, LivingEntity e) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (level.isClientSide) return stack;
        String tag = "ts_dose_" + key;
        long now = level.getGameTime();
        long last = user.getPersistentData().getLong(tag);
        if (last > 0 && now - last < windowTicks) {
            // an overdose: the second dose came before the first had cleared
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1));
            user.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            if (user instanceof Player p) p.displayClientMessage(Component.translatable("message.theoreticalsciences.overdose").withStyle(ChatFormatting.RED), true);
        } else {
            for (ResourceLocation id : treats) BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(user::removeEffect);
            for (Holder<MobEffect> h : vanilla) user.removeEffect(h);
            user.getPersistentData().putLong("ts_guard_" + key, now + guardTicks);
            if (user instanceof Player p) p.displayClientMessage(Component.translatable("message.theoreticalsciences.dose_taken",
                    String.valueOf(windowTicks / 1200)).withStyle(ChatFormatting.GREEN), true);
        }
        user.getPersistentData().putLong(tag, now);
        level.playSound(null, user.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.6F, 1.3F);
        if (!(user instanceof Player p && p.getAbilities().instabuild)) stack.shrink(1);
        return stack;
    }

    /** Whether a dose is still protecting this entity from what the medicine treats (checked as effects are added). */
    public static boolean guarded(LivingEntity e, String key) {
        return e.getPersistentData().getLong("ts_guard_" + key) > e.level().getGameTime();
    }
}
