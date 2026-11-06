package com.theoreticalsciences.ocean;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.2.0: the Cryobot, a probe that melts its way down through ice with the heat of a radium core, paying out a tether
 * behind it (NASA's design for Europa's ice). Used at the bottom of Titan, standing on the Subsurface Seal, it melts
 * through into the hidden ocean beneath; used in the ocean, it climbs its tether back up to Titan's surface. Research:
 * Titan's Hidden Ocean.
 */
public class Cryobot extends com.theoreticalsciences.item.LoreItem {
    public static final net.minecraft.resources.ResourceLocation RESEARCH = TheoreticalSciences.id("subsurface_ocean");

    public Cryobot(Properties p) {
        super(p.stacksTo(1));
    }

    /** Standing on (or just above) the Subsurface Seal. */
    static boolean onSeal(ServerPlayer sp) {
        var seal = QHBlocks.get("subsurface_seal");
        for (int dy = 1; dy <= 3; dy++) if (sp.level().getBlockState(sp.blockPosition().below(dy)).is(seal)) return true;
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
        if (!ResearchService.has(sp, RESEARCH)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.cryobot_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(st);
        }
        boolean down = level.dimension() == QHDimensions.TITAN;
        if (!(down && onSeal(sp)) && level.dimension() != QHDimensions.TITAN_OCEAN) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.cryobot_where").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(st);
        }
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.TITAN_OCEAN, !down, p -> travelNow(p, down));
        sp.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        tip.add(Component.translatable("tooltip.theoreticalsciences.cryobot").withStyle(ChatFormatting.GRAY));
    }

    /** Just under the ice ceiling at (x, z): the first water below the ice, searched from the top down. */
    static BlockPos underIce(ServerLevel l, int x, int z) {
        l.getChunk(x >> 4, z >> 4);
        boolean ice = false;
        for (int y = l.getMaxBuildHeight() - 2; y > l.getMinBuildHeight() + 2; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (!l.getFluidState(p).isEmpty()) {
                if (ice) return p.below();
            } else if (!l.getBlockState(p).isAir()) ice = true;
        }
        return new BlockPos(x, 160, z);
    }

    static void travelNow(ServerPlayer sp, boolean down) {
        ServerLevel target = sp.server.getLevel(down ? QHDimensions.TITAN_OCEAN : QHDimensions.TITAN);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        BlockPos dest = down ? underIce(target, x, z)
                : com.theoreticalsciences.world.SafeLanding.find(target, x, z, 120, QHBlocks.get("titan_ice"), null);
        if (dest == null) dest = new BlockPos(x, 120, z);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        // the probe's own air: enough to find your rebreather, or to swim back up the shaft
        if (down) sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0, false, true));
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 0.5F);
        sp.displayClientMessage(Component.translatable(down ? "message.theoreticalsciences.ocean_arrival" : "message.theoreticalsciences.ocean_return")
                .withStyle(ChatFormatting.AQUA), false);
    }
}
