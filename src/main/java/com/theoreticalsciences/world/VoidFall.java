package com.theoreticalsciences.world;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * None of our dimensions has a void you die in. Fall off the bottom of one and you fall through into the next: off the
 * Bulk back onto our own brane, through a black hole's singularity and out of its white hole, out of the Transcendental
 * into Hyperdimensional Reality, and so on. In the Living Proof you land back where you last stood.
 */
public final class VoidFall {
    private static final Map<ResourceKey<Level>, ResourceKey<Level>> BELOW = Map.ofEntries(
            Map.entry(QHDimensions.BULK, Level.OVERWORLD),
            Map.entry(QHDimensions.PLATONIC, QHDimensions.FOAM),     // 1.2.0: the Hierarchy sits above it now
            Map.entry(QHDimensions.TRANSFINITE, QHDimensions.FOAM),
            Map.entry(QHDimensions.FOAM, QHDimensions.BULK),
            Map.entry(QHDimensions.BLACK_HOLE, QHDimensions.WHITE_HOLE),
            Map.entry(QHDimensions.WHITE_HOLE, Level.OVERWORLD),
            Map.entry(QHDimensions.HYPERREAL, QHDimensions.BULK),
            Map.entry(QHDimensions.TRANSCENDENTAL, QHDimensions.HYPERREAL),
            Map.entry(QHDimensions.OMNIPOTENCY, QHDimensions.HYPERREAL),      // 1.8.0: through the cloud under Heaven (the Transcendental only opens its own way)
            Map.entry(QHDimensions.ANTIVERSE, Level.OVERWORLD),
            Map.entry(QHDimensions.FRONTIER, Level.OVERWORLD),
            Map.entry(QHDimensions.TITAN, Level.OVERWORLD),
            Map.entry(QHDimensions.INNER, Level.OVERWORLD),
            Map.entry(QHDimensions.TITAN_OCEAN, QHDimensions.TITAN),
            Map.entry(QHDimensions.ORBITAL_STATION, Level.OVERWORLD),     // 1.3.0: drift off the station and re-enter
            Map.entry(QHDimensions.HADAL_TRENCH, QHDimensions.INTERIOR));    // 1.3.0: through the floor of the deepest sea, into the planet
    private static final Map<UUID, Vec3> LAST_GROUND = com.theoreticalsciences.world.SessionMaps.perPlayer(new HashMap<>());

    /** Round 29: a player logged out - drop their remembered ground spot (TheoreticalSciences's logout listener calls this). */
    public static void forget(UUID player) {
        LAST_GROUND.remove(player);
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.isSpectator() || !sp.isAlive()) return;
        ServerLevel level = sp.serverLevel();
        ResourceKey<Level> dim = level.dimension();
        if (!dim.location().getNamespace().equals(TheoreticalSciences.MODID)) return;
        int min = level.getMinBuildHeight();
        if (dim != QHDimensions.LIVING_PROOF) LAST_GROUND.remove(sp.getUUID());
        if (dim == QHDimensions.LIVING_PROOF) {
            // remember the last spot with real ground under the player's feet (not an edge they were leaning over)
            BlockPos under = BlockPos.containing(sp.getX(), sp.getY() - 0.2, sp.getZ());
            if (sp.onGround() && !level.getBlockState(under).getCollisionShape(level, under).isEmpty())
                LAST_GROUND.put(sp.getUUID(), sp.position());
            if (sp.getY() < min - 8) {
                Vec3 back = LAST_GROUND.get(sp.getUUID());
                if (back == null) {
                    // nowhere recorded: a raft at the arena's height, straight above
                    BlockPos raft = BlockPos.containing(sp.getX(), com.theoreticalsciences.boss.ProofArena.Y + 1, sp.getZ());
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++)
                            if (level.getBlockState(raft.offset(dx, -1, dz)).isAir()) level.setBlockAndUpdate(raft.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                    back = Vec3.atBottomCenterOf(raft);
                }
                sp.stopRiding();
                sp.teleportTo(back.x, back.y + 0.1, back.z);
                sp.setDeltaMovement(Vec3.ZERO);
                sp.fallDistance = 0;
                sp.hurtMarked = true;
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.void_proof").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return;
        }
        // 1.2.0: the Transfinite Hierarchy is the Platonic Realm's upper strata now; anyone still in the old dimension
        // (an old save) is carried across to the same place in the strata, once a second
        if (dim == QHDimensions.TRANSFINITE && sp.tickCount % 20 == 0) {
            ServerLevel realm = sp.server.getLevel(QHDimensions.PLATONIC);
            if (realm != null) {
                int x = sp.getBlockX(), z = sp.getBlockZ();
                realm.getChunk(x >> 4, z >> 4);
                BlockPos dest = com.theoreticalsciences.world.SafeLanding.find(realm, x, z, Strata.Y + (int) Math.max(4, sp.getY()),
                        com.theoreticalsciences.registry.QHBlocks.get("ordinal_stone"), com.theoreticalsciences.registry.QHBlocks.get("aleph_glass"),
                        Strata.Y + 2, realm.getMaxBuildHeight() - 4);
                sp.stopRiding();
                sp.teleportTo(realm, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, java.util.Set.of(), sp.getYRot(), sp.getXRot());
                sp.fallDistance = 0;
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.transfinite_merged").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            return;
        }
        boolean fall = sp.getY() < min - 8
                // the singularity at the bottom of a black hole: crushed through it, out of the white hole
                || (dim == QHDimensions.BLACK_HOLE && sp.getY() < min + 4);
        // above y=200 a white hole lets go of you (DimensionLaws); rise past this and you are out
        boolean expelled = dim == QHDimensions.WHITE_HOLE && sp.getY() > level.getMaxBuildHeight() - 40;
        // 1.3.0: rise out of the top of the Hadal Trench's water and the current carries you up to the Overworld's sea
        boolean surfaced = dim == QHDimensions.HADAL_TRENCH && sp.getY() > level.getChunkSource().getGenerator().getSeaLevel() + 2;
        // (Level.getSeaLevel() is always 63 in 1.21.1, whatever the dimension: the real sea level is the generator's, 240 here)
        if (!fall && !expelled && !surfaced) return;
        ResourceKey<Level> to = expelled || surfaced ? Level.OVERWORLD : BELOW.get(dim);
        ServerLevel target = to == null ? null : sp.server.getLevel(to);
        if (target == null) return;
        if (to == QHDimensions.INTERIOR) {                         // inside the planet: under its roof, never on it
            interior(sp, target, "message.theoreticalsciences.void_" + dim.location().getPath());
            return;
        }
        send(sp, target, "message.theoreticalsciences.void_" + dim.location().getPath() + (surfaced ? "_up" : ""));
    }

    /** 1.3.0: a fall into the Planetary Interior lands in one of its caverns (the same place its own travel uses). */
    static void interior(ServerPlayer sp, ServerLevel target, String msg) {
        BlockPos dest = com.theoreticalsciences.interior.InteriorTravel.landing(target, sp.getBlockX(), sp.getBlockZ());
        ((ServerLevel) sp.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.5, 1.0, 0.5, 0.2);
        sp.stopRiding();
        sp.fallDistance = 0;
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        target.playSound(null, dest, net.minecraft.sounds.SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, 1F, 0.5F);
        sp.displayClientMessage(Component.translatable(msg).withStyle(ChatFormatting.GOLD), false);
    }

    static void send(ServerPlayer sp, ServerLevel target, String msg) {
        int x = sp.getBlockX(), z = sp.getBlockZ();
        target.getChunk(x >> 4, z >> 4);
        // falling in from above: the highest safe place (round 27: never inside blocks or on a fluid ceiling)
        BlockPos dest = com.theoreticalsciences.world.SafeLanding.find(target, x, z, target.getMaxBuildHeight(), Blocks.OBSIDIAN, null);
        if (dest == null) {
            // nothing to land on: a small raft of obsidian, with room to stand
            // nothing dry to land on (open sea, or the void): a raft at sea level, or mid-height where there is no sea
            int sea = target.getSeaLevel();
            int ry = sea > target.getMinBuildHeight() + 8 ? sea + 1 : target.getMinBuildHeight() + 64;
            dest = new BlockPos(x, Math.min(target.getMaxBuildHeight() - 8, ry), z);
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    target.setBlockAndUpdate(dest.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                    for (int dy = 0; dy < 2; dy++) target.setBlockAndUpdate(dest.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
        }
        ((ServerLevel) sp.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 80, 0.5, 1.0, 0.5, 0.2);
        sp.stopRiding();
        sp.fallDistance = 0;
        if (sp.changeDimension(new DimensionTransition(target, Vec3.atBottomCenterOf(dest), Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                DimensionTransition.DO_NOTHING)) == null) return;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        target.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 120, 0.6, 1.0, 0.6, 0.5);
        target.playSound(null, dest, com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.4F, 1.4F);
        sp.displayClientMessage(Component.translatable(msg).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /**
     * The highest place a player can stand in this column: two free blocks over something solid, searched downward from
     * just under the build limit (so a fluid ceiling like the Foam's Relativity Breakdown is skipped). Null if none.
     */
    public static BlockPos landing(ServerLevel l, int x, int z) {
        // walk down from just under the build limit (so a fluid ceiling like the Foam's is skipped) to the first open
        // air, then on to the first thing below it: solid ground is a landing; water or a sea is not (a raft is built)
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean seenAir = false;
        for (int y = l.getMaxBuildHeight() - 4; y > l.getMinBuildHeight(); y--) {
            p.set(x, y, z);
            BlockState s = l.getBlockState(p);
            boolean open = s.getCollisionShape(l, p).isEmpty() && s.getFluidState().isEmpty();
            if (open) {
                seenAir = true;
                continue;
            }
            if (!seenAir) continue;                       // still inside something solid at the top
            if (!s.getFluidState().isEmpty()) return null;   // the surface here is a sea
            if (free(l, x, y + 1, z) && free(l, x, y + 2, z)) return new BlockPos(x, y + 1, z);
            return null;
        }
        return null;
    }

    private static boolean free(ServerLevel l, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        BlockState s = l.getBlockState(p);
        return s.getCollisionShape(l, p).isEmpty() && s.getFluidState().isEmpty();
    }

    private VoidFall() {}
}
