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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * 1.2.0: applied mathematics as a machine upgrade. Use it on a multiblock's controller (or a console machine) to fit
 * it; a machine holds one, and fitting another hands the old one back. It comes out with the machine's contents when
 * the machine is broken. Upgrades never wear. Kinds:
 * <ul>
 *   <li>rate_table: +20% products each cycle, on top of precision (so a perfectly tuned machine goes past perfect);</li>
 *   <li>stability_margin: precision falls off half as fast (fine tuning is easier);</li>
 *   <li>confidence_seal: every chance product is halfway closer to certain;</li>
 *   <li>error_code: catalysts wear on only every other run;</li>
 *   <li>optimised_pathway: each run takes a quarter less power.</li>
 * </ul>
 */
public class UpgradeItem extends LoreItem {
    public final String kind;

    public UpgradeItem(Properties p, String kind) {
        super(p);
        this.kind = kind;
    }

    public static UpgradeItem rateTable(Properties p) {
        return new UpgradeItem(p, "rate_table");
    }

    public static UpgradeItem stabilityMargin(Properties p) {
        return new UpgradeItem(p, "stability_margin");
    }

    public static UpgradeItem confidenceSeal(Properties p) {
        return new UpgradeItem(p, "confidence_seal");
    }

    public static UpgradeItem errorCode(Properties p) {
        return new UpgradeItem(p, "error_code");
    }

    public static UpgradeItem optimisedPathway(Properties p) {
        return new UpgradeItem(p, "optimised_pathway");
    }

    /** 1.2.0: a Biology console with a Probiotic Seal is never contaminated (friendly microbes hold the ground). */
    public static UpgradeItem probioticSeal(Properties p) {
        return new UpgradeItem(p, "probiotic_seal");
    }

    /** 1.2.0: a living lining of helpful microbes: the console's catalysts wear 40% slower. */
    public static UpgradeItem symbiontLining(Properties p) {
        return new UpgradeItem(p, "symbiont_lining");
    }

    /** 1.4.0: a lattice of trained synapses: the machine learns from its runs. +10% products, and precision falls off a third slower. */
    public static UpgradeItem synapticLattice(Properties p) {
        return new UpgradeItem(p, "synaptic_lattice");
    }

    /** 1.4.0: a diamond-like carbon coating on every sliding part: catalysts wear half as fast. */
    public static UpgradeItem wearCoating(Properties p) {
        return new UpgradeItem(p, "wear_coating");
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || !TuningForkItem.tunable(c)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.upgrade_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        ItemStack hand = ctx.getItemInHand();
        ItemStack old = c.upgrade;
        c.fitUpgrade(hand.copyWithCount(1));
        if (!sp.isCreative()) hand.shrink(1);
        if (!old.isEmpty() && !sp.getInventory().add(old)) sp.drop(old, false);
        level.playSound(null, c.getBlockPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.4F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.upgrade_fitted", getDescription(),
                c.getBlockState().getBlock().getName()).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.theoreticalsciences.upgrade." + kind).withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, ctx, tip, flag);
    }
}
