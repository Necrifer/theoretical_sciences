package com.theoreticalsciences.home;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHSounds;
import com.theoreticalsciences.world.DimensionGate;
import com.theoreticalsciences.world.QHDimensions;
import com.theoreticalsciences.world.SafeLanding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Through the Homeward Portal. Only a player whose own Recreating Home checklist is complete may go in (whoever built
 * the ring); the arrival terrace, with the portal back, is built the first time anyone arrives.
 */
public final class HomeTravel {
    private HomeTravel() {}

    /** Where every arrival stands: the middle of the terrace's floor. */
    public static final BlockPos TERRACE = new BlockPos(0, 200, 0);

    public static void through(ServerPlayer sp) {
        if (sp.level().dimension() == QHDimensions.OMNIPOTENCY) leave(sp);
        else enter(sp);
    }

    private static void enter(ServerPlayer sp) {
        if (!HomeProgress.complete(sp.server, sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.home.not_yet").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            return;
        }
        ServerLevel target = sp.server.getLevel(QHDimensions.OMNIPOTENCY);
        if (target == null) {
            sp.displayClientMessage(Component.literal("Omnipotency is missing (datapack disabled?)").withStyle(ChatFormatting.RED), true);
            return;
        }
        HomeProgress.rememberWayBack(sp);
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.OMNIPOTENCY, false, p -> enterNow(p, target));
    }

    private static void enterNow(ServerPlayer sp, ServerLevel target) {
        if (!HomeProgress.complete(sp.server, sp.getUUID())) {
            com.theoreticalsciences.world.transit.Transit.cancel(sp);
            return;
        }
        buildTerrace(target);
        BlockPos dest = TERRACE.offset(0, 1, 4);
        ((ServerLevel) sp.level()).sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.4, 0.8, 0.4, 0.1);
        DimensionGate.allow(sp, QHDimensions.OMNIPOTENCY);
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, 180F, 0F, DimensionTransition.DO_NOTHING));
        DimensionGate.revoke(sp);
        sp.setPortalCooldown(100);
        target.playSound(null, dest, QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.2F, 0.5F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.home.arrival").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
    }

    private static void leave(ServerPlayer sp) {
        com.theoreticalsciences.world.transit.Transit.start(sp, QHDimensions.OMNIPOTENCY, true, HomeTravel::leaveNow);
    }

    private static void leaveNow(ServerPlayer sp) {
        var back = HomeProgress.wayBack(sp.server, sp.getUUID());
        ResourceLocation id = back.getKey().isEmpty() ? null : ResourceLocation.tryParse(back.getKey());
        ResourceKey<Level> key = id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel target = sp.server.getLevel(key);
        BlockPos at = back.getValue();
        if (sp.level().dimension() != QHDimensions.OMNIPOTENCY) {      // (moved some other way during the animation)
            com.theoreticalsciences.world.transit.Transit.cancel(sp);
            return;
        }
        if (target == null || id == null || key == QHDimensions.OMNIPOTENCY) {
            target = sp.server.overworld();
            key = Level.OVERWORLD;
            at = target.getSharedSpawnPos();
        }
        // beside the ring it was entered by, never on its light (or it would send the player straight back)
        BlockPos dest = SafeLanding.find(target, at.getX() + 3, at.getZ() + 3, at.getY(), null, null);
        DimensionGate.allow(sp, key);              // (the way back may lie in the Transcendental, which is guarded)
        sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(), DimensionTransition.DO_NOTHING));
        DimensionGate.revoke(sp);
        sp.setPortalCooldown(100);
        target.playSound(null, dest, QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** A round marble terrace with a low wall, four lanterns, and the ring with the way back at its middle. Built once. */
    private static void buildTerrace(ServerLevel level) {
        if (level.getBlockState(TERRACE).is(HomeRegistry.PORTAL.get())) return;
        BlockState marble = QHBlocks.get("empyrean_marble").defaultBlockState();
        BlockState brick = QHBlocks.get("empyrean_marble_bricks").defaultBlockState();
        BlockState gold = QHBlocks.get("seraph_gold_block").defaultBlockState();
        BlockState lantern = QHBlocks.get("glory_lantern").defaultBlockState();
        int r = 9;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + 0.5) continue;
                for (int dy = 1; dy <= 6; dy++) level.setBlock(TERRACE.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                int depth = (int) Math.max(1, (r - d) * 0.9 + 1);                    // a shallow bowl underneath
                for (int dy = 0; dy > -depth; dy--)
                    level.setBlock(TERRACE.offset(dx, dy, dz), dy == 0 ? ((dx + dz) % 2 == 0 && d < r - 1 ? marble : brick) : marble, 2);
                if (d > r - 0.6) level.setBlock(TERRACE.offset(dx, 1, dz), d > r - 0.2 && (Math.abs(dx) == Math.abs(dz) || dx == 0 || dz == 0) ? gold : brick, 2);
            }
        for (int[] c : new int[][]{{6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) {
            level.setBlock(TERRACE.offset(c[0], 1, c[1]), brick, 2);
            level.setBlock(TERRACE.offset(c[0], 2, c[1]), gold, 2);
            level.setBlock(TERRACE.offset(c[0], 3, c[1]), lantern, 2);
        }
        // the ring, set into the floor, already keyed; its light is the way back
        BlockState frame = HomeRegistry.FRAME.get().defaultBlockState().setValue(PortalFrameBlock.KEYSTONE, true);
        for (Direction d : Direction.Plane.HORIZONTAL)
            for (int o = -1; o <= 1; o++)
                level.setBlock(TERRACE.relative(d, 2).relative(d.getClockWise(), o), frame.setValue(PortalFrameBlock.FACING, d.getOpposite()), 2);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(TERRACE.offset(dx, -1, dz), gold, 2);
                level.setBlock(TERRACE.offset(dx, 0, dz), HomeRegistry.PORTAL.get().defaultBlockState(), 2);
            }
    }

    /** In Omnipotency nobody hungers, and wounds close slowly by themselves. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.level().dimension() != QHDimensions.OMNIPOTENCY) return;
        if (sp.tickCount % 40 != 0) return;
        var food = sp.getFoodData();
        if (food.getFoodLevel() < 20) food.setFoodLevel(20);
        if (sp.getHealth() < sp.getMaxHealth() && sp.isAlive()) sp.heal(1.0F);
    }
}
