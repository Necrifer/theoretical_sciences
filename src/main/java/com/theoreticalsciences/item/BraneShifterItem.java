package com.theoreticalsciences.item;

import com.theoreticalsciences.QHConfig;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Moves the user between the Overworld and The Bulk (1:1 coordinates). */
public class BraneShifterItem extends LoreItem {
    public BraneShifterItem(Properties props) {
        super(props.durability(256));
    }

    /** The Platonic Compass: same mechanism, but to the realm of mathematical forms. */
    public static class PlatonicCompass extends BraneShifterItem {
        public PlatonicCompass(Properties props) {
            super(props);
        }

        @Override
        protected net.minecraft.resources.ResourceKey<Level> destination() {
            return QHDimensions.PLATONIC;
        }

        @Override
        protected String researchId() {
            return "platonic_realm";
        }

        @Override
        protected String platformBlock() {
            return "axiom_stone";
        }

        @Override
        protected String coreBlock() {
            return "golden_ratio_glass";
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (QHConfig.BRANE_SHIFTER_NEEDS_RESEARCH.get() && !ResearchService.has(sp, TheoreticalSciences.id(researchId()))) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.shifter_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        // round 27: already mid-transit (the animation is playing): one trip at a time
        if (com.theoreticalsciences.world.transit.Transit.busy(sp)) return InteractionResultHolder.fail(stack);
        boolean leaving = level.dimension() == destination();
        ServerLevel target = leaving
                ? sp.server.overworld()
                : sp.server.getLevel(destination());
        if (target == null) {
            sp.displayClientMessage(Component.literal("The " + destination().location() + " dimension is missing (datapack disabled?)").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        // round 27: the jump no longer happens at once. The travel animation for this dimension plays for 1.75 s
        // (Transit.DELAY) and then the very same teleport as before runs. The item's cooldown and its one point of
        // wear are charged now, when the trip starts, so it can never be charged twice.
        ((ServerLevel) level).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        com.theoreticalsciences.world.transit.Transit.start(sp, destination(), leaving, p -> jump(p, target));
        sp.getCooldowns().addCooldown(this, 100);
        if (!sp.isCreative()) stack.hurtAndBreak(1, sp, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.consume(stack);
    }

    /** The actual jump (unchanged from before round 27, only run after the travel animation). */
    private void jump(ServerPlayer sp, ServerLevel target) {
        Level level = sp.level();
        BlockPos dest = findLanding(target, sp.blockPosition(), platformBlock(), coreBlock());
        ((ServerLevel) level).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.5, 1.0, 0.5, 0.15);
        level.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(), DimensionTransition.DO_NOTHING));
        target.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 80, 0.5, 1.0, 0.5, 0.15);
        onArrive(sp, target);
    }

    protected net.minecraft.resources.ResourceKey<Level> destination() {
        return QHDimensions.BULK;
    }

    /** Called after the jump (in either direction). */
    protected void onArrive(ServerPlayer sp, ServerLevel target) {}

    protected String researchId() {
        return "brane_cosmology";
    }

    protected String platformBlock() {
        return "bulk_stone";
    }

    protected String coreBlock() {
        return "bulk_crystal";
    }

    public static BlockPos findLanding(ServerLevel level, BlockPos from, String platform, String core) {
        // round 27: a real search for somewhere safe (not inside blocks, not mid-air, not above a fluid ceiling)
        return com.theoreticalsciences.world.SafeLanding.find(level, from.getX(), from.getZ(), from.getY(), QHBlocks.get(platform), QHBlocks.get(core));
    }

    @SuppressWarnings("unused")
    private static BlockPos oldLanding(ServerLevel level, BlockPos from, String platform, String core) {
        level.getChunk(from); // force generation
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, from.getX(), from.getZ());
        if (top > level.getMinBuildHeight() + 1) return new BlockPos(from.getX(), top, from.getZ());
        // Void: search a little around, then build a platform.
        for (int r = 4; r <= 32; r += 4)
            for (int dx = -r; dx <= r; dx += 4)
                for (int dz = -r; dz <= r; dz += 4) {
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, from.getX() + dx, from.getZ() + dz);
                    if (h > level.getMinBuildHeight() + 1) return new BlockPos(from.getX() + dx, h, from.getZ() + dz);
                }
        BlockPos base = new BlockPos(from.getX(), 100, from.getZ());
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                level.setBlockAndUpdate(base.offset(dx, -1, dz), QHBlocks.get(dx == 0 && dz == 0 ? core : platform).defaultBlockState());
        return base;
    }
}
