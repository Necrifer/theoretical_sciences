package com.theoreticalsciences.titan;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.2.0: the way to Titan. A Cryogenic Lander - an insulated capsule under a nylon parachute - is launched from under
 * open sky in the Overworld and comes down through Titan's orange haze; used again on Titan, it lifts off for home and
 * lands you under its parachute. Research: Titan.
 */
public final class TitanTravel {
    public static final net.minecraft.resources.ResourceLocation RESEARCH = TheoreticalSciences.id("titan");

    public static class CryogenicLanderItem extends com.theoreticalsciences.item.LoreItem {
        public CryogenicLanderItem(Properties p) {
            super(p.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            if (!ResearchService.has(sp, RESEARCH)) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.lander_locked").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(st);
            }
            boolean out = level.dimension() != QHDimensions.TITAN;
            if (!level.canSeeSky(sp.blockPosition().above()) || out && level.dimension() != Level.OVERWORLD) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.lander_sky").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            travel(sp, out);
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            tip.add(Component.translatable("tooltip.theoreticalsciences.cryogenic_lander").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Dry ground (no lake under it) with room above, near (x, z), searched outwards; or a pad of ice on the sea. */
    static BlockPos landing(ServerLevel l, int x, int z) {
        for (int r = 0; r <= 64; r += 4)
            for (int dx = -r; dx <= r; dx += 4)
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = x + dx, cz = z + dz;
                    l.getChunk(cx >> 4, cz >> 4);
                    int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    if (y <= l.getMinBuildHeight() + 1) continue;
                    BlockPos p = new BlockPos(cx, y, cz);
                    BlockState under = l.getBlockState(p.below());
                    if (under.isSolid() && under.getFluidState().isEmpty()) return p;
                }
        // all sea: the lander's flotation collar freezes a pad of water ice round it
        int y = Math.max(l.getSeaLevel() + 1, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        BlockPos c = new BlockPos(x, y, z);
        BlockState ice = QHBlocks.get("titan_ice").defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                l.setBlockAndUpdate(c.offset(dx, -1, dz), ice);
                for (int dy = 0; dy < 3; dy++) l.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return c;
    }

    /** Through the travel animation (down through the haze, or up and away from Saturn). */
    public static void travel(ServerPlayer sp, boolean out) {
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.TITAN, !out, p -> travelNow(p, out));
    }

    static void travelNow(ServerPlayer sp, boolean out) {
        ServerLevel target = sp.server.getLevel(out ? QHDimensions.TITAN : Level.OVERWORLD);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest = out ? landing(target, x, z) : new BlockPos(x, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 24, z);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        if (!out) sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, true));      // under the parachute
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
        sp.displayClientMessage(Component.translatable(out ? "message.theoreticalsciences.titan_arrival" : "message.theoreticalsciences.titan_return")
                .withStyle(ChatFormatting.GOLD), false);
    }

    private TitanTravel() {}
}
