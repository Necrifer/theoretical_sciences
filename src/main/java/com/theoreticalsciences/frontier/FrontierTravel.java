package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.research.ResearchService;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
 * 1.10.0: the way to the Accretion Frontier. A Light Sail rides the pressure of starlight: unfurled under open sky high
 * above the Overworld (y 200 or more, where there is almost no air left to drag on it), it carries you out to the
 * Frontier. Unfurled in the Frontier, it brings you home, falling gently. Research: Galactic Dynamics.
 */
public final class FrontierTravel {
    public static final int MIN_Y = 200;

    public static class LightSailItem extends Item {
        public LightSailItem(Properties p) {
            super(p.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            if (!ResearchService.has(sp, TheoreticalSciences.id("galactic_dynamics"))) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.light_sail_locked").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(st);
            }
            boolean out = level.dimension() != QHDimensions.FRONTIER;
            if (out && (level.dimension() != Level.OVERWORLD || sp.getY() < MIN_Y || !level.canSeeSky(sp.blockPosition().above()))) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.light_sail_low").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            travel(sp, out);
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.theoreticalsciences.light_sail").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }

    /** Solid ground with two blocks of air above it, near (x, z), searched outwards; or a small platform at y 100. */
    static BlockPos landing(ServerLevel l, int x, int z) {
        for (int r = 0; r <= 48; r += 4)
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
        BlockPos c = new BlockPos(x, 100, z);
        BlockState rock = QHBlocks.get("frontier_regolith").defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                l.setBlockAndUpdate(c.offset(dx, -1, dz), rock);
                for (int dy = 0; dy < 3; dy++) l.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return c;
    }

    /** 1.10.1: through the travel animation (sailing out past the heliopause, or falling back home). */
    public static void travel(ServerPlayer sp, boolean out) {
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.FRONTIER, !out, p -> travelNow(p, out));
    }

    static void travelNow(ServerPlayer sp, boolean out) {
        ServerLevel target = sp.server.getLevel(out ? QHDimensions.FRONTIER : Level.OVERWORLD);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest = out ? landing(target, x, z) : new BlockPos(x, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 24, z);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        if (!out) sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, true));
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.displayClientMessage(Component.translatable(out ? "message.theoreticalsciences.frontier_arrival" : "message.theoreticalsciences.frontier_return")
                .withStyle(ChatFormatting.AQUA), false);
    }

    private FrontierTravel() {}
}
