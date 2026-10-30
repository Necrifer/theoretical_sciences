package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Field Calibrator: use it on a multiblock controller to restore containment integrity, clear a fault and
 * read out the ideal tuning value (it is also marked on the controller's tuning panel for 30 seconds).
 * The Autonomous Calibrator variant installs itself permanently instead.
 */
public class FieldCalibratorItem extends LoreItem {
    private final boolean autonomous;

    public FieldCalibratorItem(Properties props) {
        this(props, false);
    }

    public FieldCalibratorItem(Properties props, boolean autonomous) {
        super(autonomous ? props : props.durability(128));
        this.autonomous = autonomous;
    }

    public static FieldCalibratorItem autonomous(Properties props) {
        return new FieldCalibratorItem(props, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c) || c.tuningProfile() == null) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.calibrator_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        TuningProfile p = c.tuningProfile();
        ItemStack stack = ctx.getItemInHand();
        if (autonomous) {
            if (c.tuning.autoCalibrator) {
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.autocalibrator_present").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            c.tuning.autoCalibrator = true;
            c.tuning.calibrate();
            if (c.hasPhysics()) c.physics.calibrate();
            if (!player.isCreative()) stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.autocalibrator_installed").withStyle(ChatFormatting.AQUA), false);
        } else {
            boolean wasFaulted = c.tuning.faulted;
            c.tuning.calibrate();
            if (!player.isCreative()) stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(ctx.getHand()));
            if (c.hasPhysics()) {
                // physics machines: the field errors are trimmed out and the containment is restored
                String d = String.format("%+.2f", c.physics.disturbance);
                c.physics.calibrate();
                player.displayClientMessage(Component.translatable(wasFaulted ? "message.theoreticalsciences.calibrated_fault_physics" : "message.theoreticalsciences.calibrated_physics", d)
                        .withStyle(ChatFormatting.AQUA), false);
            } else player.displayClientMessage(Component.translatable(wasFaulted ? "message.theoreticalsciences.calibrated_fault" : "message.theoreticalsciences.calibrated",
                    Component.translatable("tuning.theoreticalsciences." + p.param()), String.format("%.1f", c.tuning.target),
                    String.format("%.1f", c.tuning.setting)).withStyle(ChatFormatting.AQUA), false);
        }
        c.setChanged();
        level.sendBlockUpdated(c.getBlockPos(), c.getBlockState(), c.getBlockState(), 2);
        level.playSound(null, c.getBlockPos(), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        return InteractionResult.CONSUME;
    }
}
