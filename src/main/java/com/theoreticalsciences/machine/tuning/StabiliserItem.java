package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Round 25: a multiblock stabiliser, installed by using it on the controller. Tiers 1-3 are engineered (less drift,
 * less often, less wear and damage); tier 4, the Equilibrium Lattice, comes only from the rebuilt Core's Forge and
 * makes the machine perfect and completely hands off. A better one replaces a lesser one (which is handed back).
 */
public class StabiliserItem extends Item {
    public static final String[] IDS = {"", "phase_locked_stabiliser", "adaptive_feedback_core", "predictive_lattice_governor", "equilibrium_lattice"};
    final int tier;

    public StabiliserItem(Properties p, int tier) {
        super(p);
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || c.tuningProfile() == null && !c.hasPhysics()) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.calibrator_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        int had = c.tuning.stabiliser;
        if (had >= tier) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.stabiliser_present", Component.translatable("item.theoreticalsciences." + IDS[Math.min(4, had)]))
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        c.tuning.stabiliser = tier;
        c.tuning.calibrate();
        if (c.hasPhysics()) c.physics.calibrate();
        if (!player.isCreative()) {
            ctx.getItemInHand().shrink(1);
            if (had > 0) {                      // the lesser one comes back out
                ItemStack old = new ItemStack(com.theoreticalsciences.registry.QHItems.get(IDS[had]));
                if (!player.getInventory().add(old)) player.drop(old, false);
            }
        }
        player.displayClientMessage(Component.translatable(tier >= TuningState.PERFECT ? "message.theoreticalsciences.stabiliser_perfect" : "message.theoreticalsciences.stabiliser_installed",
                getName(ctx.getItemInHand().isEmpty() ? new ItemStack(this) : ctx.getItemInHand()),
                Math.round((1 - TuningState.DRIFT[tier]) * 100), Math.round((1 - TuningState.WEAR[tier]) * 100)).withStyle(ChatFormatting.AQUA), false);
        c.setChanged();
        level.sendBlockUpdated(c.getBlockPos(), c.getBlockState(), c.getBlockState(), 2);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        if (tier >= TuningState.PERFECT) tip.add(Component.translatable("tooltip.theoreticalsciences.stabiliser_perfect").withStyle(ChatFormatting.LIGHT_PURPLE));
        else tip.add(Component.translatable("tooltip.theoreticalsciences.stabiliser", Math.round((1 - TuningState.DRIFT[tier]) * 100),
                Math.round(TuningState.PERIOD[tier] * 100) / 100.0, Math.round((1 - TuningState.WEAR[tier]) * 100)).withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("tooltip.theoreticalsciences.stabiliser_use").withStyle(ChatFormatting.GRAY));
    }
}
