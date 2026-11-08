package com.theoreticalsciences.mind;

import com.theoreticalsciences.item.LoreItem;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.4.0: the Low-Light Visor. A cultured retina of rod cells (rhodopsin, the pigment that lets an eye count single
 * photons) behind an image intensifier, fed by an anglerfish's bacteria. Worn on the head it gives night vision, and in
 * the Hadal Trench it is the gear that sees the trench as it is (it carries the theoreticalsciences:hadal_vision tag).
 */
public class LowLightVisorItem extends LoreItem implements Equipable {
    public LowLightVisorItem(Properties p) {
        super(p);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_CHAIN;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide || !(holder instanceof LivingEntity le) || le.getItemBySlot(EquipmentSlot.HEAD) != stack) return;
        if (le.tickCount % 40 != 0) return;
        MobEffectInstance cur = le.getEffect(MobEffects.NIGHT_VISION);
        // long enough that night vision never starts to flicker (it flashes in its last 10 seconds)
        if (cur == null || cur.getDuration() < 260) le.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
    }
}
