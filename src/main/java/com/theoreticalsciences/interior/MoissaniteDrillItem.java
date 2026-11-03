package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/**
 * Round 41 (1.3.3): the Moissanite Drill - a pick of its own tier, above netherite. Iron-Nickel and Core Magnetite Ore are
 * listed as wrong for every vanilla tier (block tags minecraft:incorrect_for_*_tool), and nothing is wrong for this one,
 * so it is the first pick that gets their drops.
 */
public class MoissaniteDrillItem extends PickaxeItem {
    public static final TagKey<Block> INCORRECT = TagKey.create(Registries.BLOCK, TheoreticalSciences.id("incorrect_for_moissanite_tool"));
    public static final SimpleTier TIER = new SimpleTier(INCORRECT, 3200, 12.0F, 5.0F, 16, () -> Ingredient.of(QHItems.get("moissanite")));

    public MoissaniteDrillItem(Item.Properties p) {
        super(TIER, p.attributes(PickaxeItem.createAttributes(TIER, 1.0F, -2.8F)).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.theoreticalsciences.moissanite_drill.lore").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
