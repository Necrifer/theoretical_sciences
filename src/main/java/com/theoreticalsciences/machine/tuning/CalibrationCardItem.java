package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.physics.PhysicsModel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * 1.2.0: the Calibration Card. Sneak-use it on a tuned machine to copy its settings (and, for older multiblocks, its
 * tuning dial, overclock and parallel); use it on another machine of the same kind to set it the same way. A card is
 * never used up, so one tuned machine can set any number of copies. Use it in the air while sneaking to wipe it.
 */
public class CalibrationCardItem extends LoreItem {
    public CalibrationCardItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c)
                || !(c.hasPhysics() ? c.physics.model() != null : c.tuningProfile() != null)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.card.nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        ItemStack card = ctx.getItemInHand();
        Component machine = c.getBlockState().getBlock().getName();
        if (sp.isShiftKeyDown()) {
            CompoundTag t = new CompoundTag();
            t.putString("Machine", c.type.id());
            CompoundTag ps = new CompoundTag();
            if (c.hasPhysics()) {
                List<PhysicsModel.Param> params = c.physics.model().params;
                for (int i = 0; i < params.size(); i++) ps.putDouble(params.get(i).id(), c.physics.params[i]);
            }
            t.put("Params", ps);
            t.putFloat("Setting", c.tuning.setting);
            t.putInt("Overclock", c.tuning.overclock);
            t.putInt("Parallel", c.tuning.parallel);
            card.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.card.copied", machine).withStyle(ChatFormatting.AQUA), true);
            level.playSound(null, c.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.3F);
            return InteractionResult.CONSUME;
        }
        CompoundTag t = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!t.contains("Machine")) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.card.blank").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        if (!t.getString("Machine").equals(c.type.id())) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.card.wrong", machineName(t)).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (c.hasPhysics()) {
            CompoundTag ps = t.getCompound("Params");
            List<PhysicsModel.Param> params = c.physics.model().params;
            for (int i = 0; i < params.size(); i++)
                if (ps.contains(params.get(i).id())) c.applyPhysics(i, ps.getDouble(params.get(i).id()));
        }
        if (c.tuningProfile() != null) {
            c.applyTuning(0, Math.round((t.getFloat("Setting") - c.tuning.setting) * 10));
            c.applyTuning(1, t.getInt("Overclock") - c.tuning.overclock);
            c.applyTuning(2, t.getInt("Parallel") - c.tuning.parallel);
        }
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.card.applied", machine).withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, c.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.5F);
        return InteractionResult.CONSUME;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.InteractionHand hand) {
        ItemStack card = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && card.has(DataComponents.CUSTOM_DATA)) {
            if (!level.isClientSide) {
                card.remove(DataComponents.CUSTOM_DATA);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.card.wiped").withStyle(ChatFormatting.GRAY), true);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(card, level.isClientSide);
        }
        return net.minecraft.world.InteractionResultHolder.pass(card);
    }

    private static Component machineName(CompoundTag t) {
        return Component.translatable("block.theoreticalsciences." + t.getString("Machine"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (t.contains("Machine"))
            tip.add(Component.translatable("tooltip.theoreticalsciences.calibration_card.holds", machineName(t), t.getCompound("Params").size())
                    .withStyle(ChatFormatting.AQUA));
        else tip.add(Component.translatable("tooltip.theoreticalsciences.calibration_card.blank").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, ctx, tip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }
}
