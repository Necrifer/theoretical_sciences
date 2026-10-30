package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

/**
 * One piece of the Raiment of Transcendence. Unbreakable; the Greaves and Treads carry their gifts as ordinary attribute
 * modifiers (so vanilla adds and removes them with the piece); everything else is done in {@link Raiment}.
 * Round 32: a piece runs on Eschaton Charge. Empty, it is dormant: no armour points, no gifts, no part in the set.
 */
public class TranscendenceArmorItem extends ArmorItem implements EschatonPowered {
    public static final int MAX_CHARGE = 20_000;
    private ItemAttributeModifiers modifiers;

    public TranscendenceArmorItem(Type type, Properties p) {
        super(EndgameRegistry.TRANSCENDENCE, type, p.fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }

    public static TranscendenceArmorItem of(String special, Properties p) {
        Type t = switch (special) {
            case "armor_head" -> Type.HELMET;
            case "armor_chest" -> Type.CHESTPLATE;
            case "armor_legs" -> Type.LEGGINGS;
            default -> Type.BOOTS;
        };
        return new TranscendenceArmorItem(t, p);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (modifiers == null) {
            ItemAttributeModifiers base = super.getDefaultAttributeModifiers();
            if (getType() == Type.LEGGINGS) {
                base = base.withModifierAdded(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(TheoreticalSciences.id("raiment_speed"), 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.LEGS);
                base = base.withModifierAdded(Attributes.STEP_HEIGHT,
                        new AttributeModifier(TheoreticalSciences.id("raiment_step"), 0.65, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.LEGS);
            } else if (getType() == Type.BOOTS) {
                base = base.withModifierAdded(Attributes.JUMP_STRENGTH,
                        new AttributeModifier(TheoreticalSciences.id("raiment_jump"), 0.3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
                base = base.withModifierAdded(Attributes.SAFE_FALL_DISTANCE,
                        new AttributeModifier(TheoreticalSciences.id("raiment_safe_fall"), 1000, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.FEET);
            }
            modifiers = base;
        }
        return modifiers;
    }

    /** Dormant (no charge): no armour, no speed, nothing - vanilla takes the modifiers off as soon as the charge runs out. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return EschatonCharge.get(stack) > 0 ? getDefaultAttributeModifiers() : ItemAttributeModifiers.EMPTY;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.raiment.piece." + getType().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.raiment.set").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
