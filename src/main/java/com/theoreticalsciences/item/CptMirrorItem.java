package com.theoreticalsciences.item;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The CPT Mirror: step through into the Antiverse at the same coordinates - and back out of it into the Overworld.
 * It finds solid ground on the other side (or lays a small platform of antistone if there is none).
 */
public class CptMirrorItem extends LoreItem {
    public CptMirrorItem(Properties p) {
        super(p.durability(128));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (QHConfig.BRANE_SHIFTER_NEEDS_RESEARCH.get() && !ResearchService.has(sp, TheoreticalSciences.id("cpt_symmetry"))) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.cpt_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (com.theoreticalsciences.world.transit.Transit.busy(sp)) return InteractionResultHolder.fail(stack);
        boolean leaving = level.dimension() == QHDimensions.ANTIVERSE;
        ServerLevel target = sp.server.getLevel(leaving ? Level.OVERWORLD : QHDimensions.ANTIVERSE);
        if (target == null) return InteractionResultHolder.fail(stack);
        // round 27: the mirror animation plays for 1.75 s, then the same jump as before (cooldown and wear are
        // charged now, once)
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.ANTIVERSE, leaving, p -> jump(p, target, leaving));
        sp.getCooldowns().addCooldown(this, 100);
        stack.hurtAndBreak(1, sp, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.success(stack);
    }

    /** The actual jump through the mirror (unchanged from before round 27, only run after the animation). */
    private static void jump(ServerPlayer sp, ServerLevel target, boolean leaving) {
        Level level = sp.level();
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        BlockPos dest = com.theoreticalsciences.world.SafeLanding.find(target, x, z, sp.getBlockY(),
                leaving ? Blocks.STONE : com.theoreticalsciences.registry.QHBlocks.get("antistone"), null);
        if (dest == null) {
            dest = new BlockPos(x, target.getSeaLevel() + 2, z);
            var base = leaving ? Blocks.STONE.defaultBlockState() : com.theoreticalsciences.registry.QHBlocks.get("antistone").defaultBlockState();
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    target.setBlockAndUpdate(dest.offset(dx, -1, dz), base);
                    for (int dy = 0; dy < 2; dy++) target.setBlockAndUpdate(dest.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
        }
        ((ServerLevel) level).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.5, 1, 0.5, 0.2);
        level.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.PLAYERS, 1F, 0.5F);
        sp.stopRiding();
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot() + 180F, sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) {
            com.theoreticalsciences.world.transit.Transit.cancel(sp);   // the jump was refused: fade the animation away
            return;
        }
        sp.fallDistance = 0;
        target.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 1, 0, 0, 0, 0);
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1F, 1.6F);
        if (!leaving) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.antiverse_arrival").withStyle(ChatFormatting.GOLD), false);
    }
}
