package com.theoreticalsciences.item;

import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GregTech-style fluid handling. Our fluids are too hot, too cold or too strange for a bucket: buckets refuse them (with a
 * warning saying why), and the Empty Containment Cell is what scoops them up - from the world or from a tank. A drained
 * cell goes back to being an empty cell, never a bucket.
 */
public final class ContainmentCells {
    public static ItemStack empty() {
        return new ItemStack(QHItems.get("containment_cell"));
    }

    @Nullable
    public static QHFluids.Entry entryFor(Fluid f) {
        for (QHFluids.Entry e : QHFluids.ALL.values())
            if (e.source.get() == f || e.flowing.get() == f) return e;
        return null;
    }

    /** Why a bucket can't hold this fluid. */
    public static Component hazard(QHFluids.Entry e) {
        int t = e.def.temperature();
        if (e.def.gas()) return Component.translatable("message.theoreticalsciences.hazard.gas");
        if (t >= 500) return Component.translatable("message.theoreticalsciences.hazard.hot", t);
        if (t <= 200) return Component.translatable("message.theoreticalsciences.hazard.cold", t);
        return Component.translatable("message.theoreticalsciences.hazard.exotic");
    }

    /** A vanilla bucket aimed at one of our fluids: refuse, and say why. */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem ev) {
        if (!ev.getItemStack().is(Items.BUCKET)) return;
        Player p = ev.getEntity();
        // aim exactly the way a bucket does (source blocks only)
        var eye = p.getEyePosition();
        HitResult hit = ev.getLevel().clip(new ClipContext(eye, eye.add(p.getViewVector(1F).scale(p.blockInteractionRange())),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, p));
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return;
        FluidState fs = ev.getLevel().getFluidState(bh.getBlockPos());
        QHFluids.Entry e = fs.isEmpty() ? null : entryFor(fs.getType());
        if (e == null) return;
        ev.setCanceled(true);
        ev.setCancellationResult(InteractionResult.FAIL);
        if (!ev.getLevel().isClientSide) {
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.bucket_refused",
                    Component.translatable("fluid_type.theoreticalsciences." + e.def.id()), hazard(e)).withStyle(ChatFormatting.GOLD), true);
            ev.getLevel().playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.VENT.get(), SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    /** The empty cell: right-click a source block of one of our fluids to fill it. */
    public static class EmptyCellItem extends Item {
        public EmptyCellItem(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack held = player.getItemInHand(hand);
            BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
            BlockPos pos = hit.getBlockPos();
            if (!level.mayInteract(player, pos)) return InteractionResultHolder.fail(held);
            FluidState fs = level.getFluidState(pos);
            if (fs.isEmpty()) return InteractionResultHolder.pass(held);
            QHFluids.Entry e = entryFor(fs.getType());
            if (e == null) {
                if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.theoreticalsciences.cell_ordinary").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(held);
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BucketPickup pickup)) return InteractionResultHolder.fail(held);
            ItemStack filled = pickup.pickupBlock(player, level, pos, state);
            if (filled.isEmpty()) return InteractionResultHolder.fail(held);
            // whatever the block hands back, the cell becomes this fluid's cell
            filled = new ItemStack(e.bucket.get());
            level.playSound(player, pos, com.theoreticalsciences.registry.QHSounds.FLUID_FILL.get(), SoundSource.PLAYERS, 1F, 1.3F);
            return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(held, player, filled), level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.theoreticalsciences.empty_cell").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Fluid handler for cells, empty and full: like a bucket's, but it only ever holds our fluids and drains back to an
     * empty cell.
     */
    public static class CellWrapper extends FluidBucketWrapper {
        public CellWrapper(ItemStack stack) {
            super(stack);
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            // (a mod's Essence lives only in tanks: a cell could not remember whose it is)
            return entryFor(fluid.getFluid()) != null && !com.theoreticalsciences.transcend.Essences.isEssence(fluid);
        }

        @Override
        protected void setFluid(FluidStack fluid) {
            if (fluid.isEmpty()) container = empty();
            else {
                QHFluids.Entry e = entryFor(fluid.getFluid());
                container = e == null ? empty() : new ItemStack(e.bucket.get());
            }
        }
    }

    private ContainmentCells() {}
}
