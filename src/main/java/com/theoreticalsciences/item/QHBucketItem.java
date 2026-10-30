package com.theoreticalsciences.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

import java.util.List;

/** Containment cell. Gas cells cannot be poured out - opening one just vents the gas. */
public class QHBucketItem extends BucketItem {
    private final boolean gas;

    public QHBucketItem(Fluid fluid, boolean gas, Properties props) {
        super(fluid, props);
        this.gas = gas;
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit, @Nullable ItemStack container) {
        if (!gas) return super.emptyContents(player, level, pos, hit, container);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.08);
            level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.VENT.get(), SoundSource.BLOCKS, 0.6F, 1.8F);
        }
        return true;
    }

    /** Pouring a cell out (or using it in a recipe) leaves an empty cell, not a bucket. */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        var r = super.use(level, player, hand);
        if (r.getObject().is(net.minecraft.world.item.Items.BUCKET) && !player.hasInfiniteMaterials())
            return new net.minecraft.world.InteractionResultHolder<>(r.getResult(), ContainmentCells.empty().copyWithCount(r.getObject().getCount()));
        return r;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ContainmentCells.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(gas ? "tooltip.theoreticalsciences.gas_cell" : "tooltip.theoreticalsciences.fluid_cell").withStyle(ChatFormatting.GRAY));
    }
}
