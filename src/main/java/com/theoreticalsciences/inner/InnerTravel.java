package com.theoreticalsciences.inner;

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
 * 1.2.0: the way into Inner Space. The Miniaturiser (Fantastic Voyage, 1966) shrinks its user until a body's cells are
 * the size of houses, and sets them down inside one. Used in the Overworld it goes in; used in Inner Space it brings you
 * back out where you stood. Research: Inner Space.
 */
public final class InnerTravel {
    public static final net.minecraft.resources.ResourceLocation RESEARCH = TheoreticalSciences.id("inner_space");

    public static class MiniaturiserItem extends com.theoreticalsciences.item.LoreItem {
        public MiniaturiserItem(Properties p) {
            super(p.stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            if (!ResearchService.has(sp, RESEARCH)) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.miniaturiser_locked").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(st);
            }
            boolean in = level.dimension() != QHDimensions.INNER;
            if (in && level.dimension() != Level.OVERWORLD) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.miniaturiser_where").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.INNER, !in, p -> travelNow(p, in));
            sp.getCooldowns().addCooldown(this, 100);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, tip, flag);
            tip.add(Component.translatable("tooltip.theoreticalsciences.miniaturiser").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Dry ground near (x, z), searched outwards; or, all plasma, a raft of membrane on it. */
    static BlockPos landing(ServerLevel l, int x, int z) {
        for (int r = 0; r <= 64; r += 4)
            for (int dx = -r; dx <= r; dx += 4)
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = x + dx, cz = z + dz;
                    l.getChunk(cx >> 4, cz >> 4);
                    int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    if (y <= l.getMinBuildHeight() + 1 || y >= l.getMaxBuildHeight() - 4) continue;
                    BlockPos p = new BlockPos(cx, y, cz);
                    BlockState under = l.getBlockState(p.below());
                    if (under.isSolid() && under.getFluidState().isEmpty() && l.getBlockState(p.above()).isAir()) return p;
                }
        int y = Math.max(l.getSeaLevel() + 1, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        BlockPos c = new BlockPos(x, y, z);
        BlockState raft = QHBlocks.get("cell_membrane").defaultBlockState();
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                l.setBlockAndUpdate(c.offset(dx, -1, dz), raft);
                for (int dy = 0; dy < 3; dy++) l.setBlockAndUpdate(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
            }
        return c;
    }

    static void travelNow(ServerPlayer sp, boolean in) {
        ServerLevel target = sp.server.getLevel(in ? QHDimensions.INNER : Level.OVERWORLD);
        if (target == null) return;
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest = in ? landing(target, x, z)
                : com.theoreticalsciences.world.SafeLanding.find(target, x, z, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), Blocks.OBSIDIAN, null);
        if (dest == null) dest = new BlockPos(x, target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.PLAYERS, 0.8F, in ? 1.6F : 0.6F);
        sp.displayClientMessage(Component.translatable(in ? "message.theoreticalsciences.inner_arrival" : "message.theoreticalsciences.inner_return")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private InnerTravel() {}
}
