package com.theoreticalsciences.interior;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Round 40 (1.3.0): the Core Drill - the first tool that cuts bedrock. Hold it against bedrock for three seconds. In the
 * floor of the Overworld, or the roof or floor of the Planetary Interior, the hole becomes a Mantle Breach (the way
 * through); anywhere else the bedrock is simply cut away.
 */
public class CoreDrillItem extends LoreItem {
    private static final int TIME = 60;

    public CoreDrillItem(Properties p) {
        super(p.durability(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null || !ctx.getLevel().getBlockState(ctx.getClickedPos()).is(Blocks.BEDROCK)) return InteractionResult.PASS;
        if (p instanceof ServerPlayer sp && QHConfig.BRANE_SHIFTER_NEEDS_RESEARCH.get()
                && !ResearchService.has(sp, TheoreticalSciences.id("plate_tectonics"))) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.core_drill_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        p.startUsingItem(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity e) {
        return TIME;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private static BlockHitResult target(Level level, LivingEntity e) {
        var eye = e.getEyePosition();
        var end = eye.add(e.getViewVector(1F).scale(5));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, e));
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack stack, int left) {
        BlockHitResult hit = target(level, e);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(hit.getBlockPos()).is(Blocks.BEDROCK)) {
            e.stopUsingItem();
            return;
        }
        if (level instanceof ServerLevel sl && left % 4 == 0) {
            BlockPos b = hit.getBlockPos();
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BEDROCK.defaultBlockState()), b.getX() + 0.5, b.getY() + 0.5,
                    b.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, b, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.8F, 0.6F + sl.random.nextFloat() * 0.3F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (!(level instanceof ServerLevel sl) || !(e instanceof ServerPlayer sp)) return stack;
        BlockHitResult hit = target(level, e);
        if (hit.getType() != HitResult.Type.BLOCK) return stack;
        BlockPos b = hit.getBlockPos();
        if (!sl.getBlockState(b).is(Blocks.BEDROCK)) return stack;
        boolean floor = b.getY() <= sl.getMinBuildHeight() + 4;
        boolean interior = sl.dimension() == QHDimensions.INTERIOR;
        boolean breach = (sl.dimension() == Level.OVERWORLD && floor) || interior;
        sl.setBlockAndUpdate(b, breach ? QHBlocks.get("mantle_breach").defaultBlockState() : Blocks.AIR.defaultBlockState());
        sl.playSound(null, b, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 0.6F);
        if (breach) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.core_drill_through").withStyle(ChatFormatting.GOLD), true);
        stack.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        return stack;
    }
}
