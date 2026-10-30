package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * What the Scepter of Singularities does. Only ever affects non-player living things (never players, armour stands or
 * anyone's pets), and never breaks a block.
 */
public final class ScepterEffects {
    private static final com.theoreticalsciences.registry.QHGlowOptions VIOLET = com.theoreticalsciences.registry.QHGlowOptions.of(0.55F, 0.2F, 0.9F, 1.6F);
    private static final com.theoreticalsciences.registry.QHGlowOptions GOLD = com.theoreticalsciences.registry.QHGlowOptions.of(1F, 0.85F, 0.35F, 1.6F);
    private static final com.theoreticalsciences.registry.QHGlowOptions CYAN = com.theoreticalsciences.registry.QHGlowOptions.of(0.4F, 1F, 1F, 1.2F);

    /** A black hole that lingers for a few seconds, pulling things in, and crushes whatever is left at the end. */
    private record BlackHole(ResourceKey<Level> dim, Vec3 pos, UUID owner, int[] age) {}

    private static final List<BlackHole> HOLES = new ArrayList<>();
    private static final int HOLE_TICKS = 60;
    private static final double HOLE_RADIUS = 8;

    static boolean affects(Player user, Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e instanceof Player || e instanceof ArmorStand) return false;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;
        return !e.isAlliedTo(user);
    }

    private static List<LivingEntity> around(ServerLevel level, Player user, Vec3 at, double r) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                e -> affects(user, e) && e.position().distanceToSqr(at) <= r * r);
    }

    private static DamageSource source(ServerLevel level, Player user) {
        return level.damageSources().indirectMagic(user, user);
    }

    // ---------------------------------------------------------------- Black Hole

    static boolean blackHole(ServerLevel level, Player user, Vec3 at) {
        if (HOLES.stream().filter(h -> h.owner.equals(user.getUUID())).count() >= 3) return false;
        HOLES.add(new BlackHole(level.dimension(), at, user.getUUID(), new int[1]));
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.CHARGE.get(), SoundSource.PLAYERS, 2.0F, 0.5F);
        return true;
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (HOLES.isEmpty()) return;
        Iterator<BlackHole> it = HOLES.iterator();
        while (it.hasNext()) {
            BlackHole h = it.next();
            ServerLevel level = e.getServer().getLevel(h.dim);
            ServerPlayer user = e.getServer().getPlayerList().getPlayer(h.owner);
            if (level == null || user == null || !level.isLoaded(BlockPos.containing(h.pos))) {
                it.remove();
                continue;
            }
            int age = ++h.age[0];
            Vec3 c = h.pos;
            // accretion disc and the dark core
            double turn = age * 0.35;
            for (int i = 0; i < 6; i++) {
                double a = turn + i * Math.PI / 3, r = HOLE_RADIUS * (1 - (age % 20) / 20.0) + 0.5;
                level.sendParticles(VIOLET, c.x + Math.cos(a) * r, c.y + Math.sin(a * 2) * 0.3, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.VOID_INK.get(), c.x, c.y, c.z, 4, 0.15, 0.15, 0.15, 0.0);
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), c.x, c.y, c.z, 6, 1.5, 1.5, 1.5, 0.3);
            if (age % 20 == 0) level.playSound(null, BlockPos.containing(c), com.theoreticalsciences.registry.QHSounds.RIFT_HUM.get(), SoundSource.PLAYERS, 1.5F, 0.4F);

            for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(HOLE_RADIUS),
                    t -> affects(user, t))) {
                Vec3 to = c.subtract(t.position().add(0, t.getBbHeight() / 2, 0));
                double d = to.length();
                if (d > HOLE_RADIUS) continue;
                double pull = Mth.clamp(0.12 + (HOLE_RADIUS - d) * 0.06, 0.12, 0.6);
                Vec3 v = d < 0.6 ? Vec3.ZERO : to.normalize().scale(pull);
                t.setDeltaMovement(t.getDeltaMovement().scale(0.5).add(v));
                t.hurtMarked = true;
                t.fallDistance = 0;
                if (d < 1.5 && age % 10 == 0)
                    t.hurt(source(level, user), 8F);
            }
            if (age >= HOLE_TICKS) {
                for (LivingEntity t : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(3),
                        t -> affects(user, t))) {
                    t.invulnerableTime = 0;
                    t.hurt(source(level, user), 150F);
                }
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.SHOCKWAVE.get(), c.x, c.y, c.z, 1, 0, 0, 0, 0);
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), c.x, c.y, c.z, 120, 0.2, 0.2, 0.2, 2.0);
                level.playSound(null, BlockPos.containing(c), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 2.0F, 0.4F);
                it.remove();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent e) {
        HOLES.clear();
    }

    // ---------------------------------------------------------------- White Hole

    static boolean whiteHole(ServerLevel level, Player user, Vec3 at) {
        double r = 10;
        for (LivingEntity t : around(level, user, at, r)) {
            Vec3 away = t.position().add(0, t.getBbHeight() / 2, 0).subtract(at);
            double d = Math.max(0.5, away.length());
            Vec3 dir = away.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : away.normalize();
            double strength = 1.5 + (r - Math.min(r, d)) * 0.35;
            t.invulnerableTime = 0;
            t.hurt(source(level, user), (float) (20 + (r - Math.min(r, d)) * 3));
            t.setDeltaMovement(dir.x * strength, Math.max(0.6, dir.y * strength + 0.8), dir.z * strength);
            t.hurtMarked = true;
        }
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), at.x, at.y, at.z, 150, 0.2, 0.2, 0.2, 0.6);
        level.sendParticles(GOLD, at.x, at.y, at.z, 60, 2, 2, 2, 0);
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 2.0F, 1.6F);
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 1.0F, 1.8F);
        return true;
    }

    // ---------------------------------------------------------------- Tesseract

    static boolean tesseract(ServerLevel level, Player user, HitResult hit) {
        Vec3 target;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK)
            target = Vec3.atBottomCenterOf(bh.getBlockPos().relative(bh.getDirection()));
        else if (hit instanceof EntityHitResult eh) target = eh.getEntity().position();
        else target = hit.getLocation();
        Vec3 safe = findSafe(level, user, target);
        if (safe == null) {
            user.displayClientMessage(Component.translatable("message.theoreticalsciences.scepter.no_room").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Vec3 from = user.position();
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), from.x, from.y + 1, from.z, 40, 0.3, 0.8, 0.3, 0.1);
        level.sendParticles(CYAN, from.x, from.y + 1, from.z, 30, 0.5, 1, 0.5, 0);
        user.stopRiding();
        user.teleportTo(safe.x, safe.y, safe.z);
        user.resetFallDistance();
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), safe.x, safe.y + 1, safe.z, 40, 0.3, 0.8, 0.3, 0.1);
        level.sendParticles(CYAN, safe.x, safe.y + 1, safe.z, 30, 0.5, 1, 0.5, 0);
        level.playSound(null, BlockPos.containing(from), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, BlockPos.containing(safe), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    /** Somewhere near the target where the player's bounding box fits and that is inside the world. */
    private static Vec3 findSafe(ServerLevel level, Player user, Vec3 target) {
        for (int dy = 0; dy <= 8; dy++) {
            for (int sign : new int[]{1, -1}) {
                if (dy == 0 && sign < 0) continue;
                Vec3 p = new Vec3(Math.floor(target.x) + 0.5, Math.floor(target.y) + dy * sign, Math.floor(target.z) + 0.5);
                BlockPos bp = BlockPos.containing(p);
                if (!level.getWorldBorder().isWithinBounds(bp) || level.isOutsideBuildHeight(bp) || !level.isLoaded(bp)) continue;
                AABB box = user.getDimensions(user.getPose()).makeBoundingBox(p);
                if (level.noCollision(user, box) && level.getBlockStates(box).noneMatch(s -> s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)))
                    return p;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- Entanglement

    static boolean entangle(ServerLevel level, Player user, Vec3 at) {
        List<LivingEntity> near = new ArrayList<>(around(level, user, at, 12));
        if (near.isEmpty()) {
            user.displayClientMessage(Component.translatable("message.theoreticalsciences.scepter.nothing").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        // chain from the hit point to the nearest, then always to the nearest unvisited one
        List<LivingEntity> chain = new ArrayList<>();
        Vec3 cur = at;
        while (!near.isEmpty() && chain.size() < 8) {
            final Vec3 from = cur;
            LivingEntity next = near.stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(from))).get();
            near.remove(next);
            chain.add(next);
            cur = next.position().add(0, next.getBbHeight() / 2, 0);
        }
        Vec3 prev = user.getEyePosition().add(user.getLookAngle().scale(0.8)).subtract(0, 0.3, 0);
        for (LivingEntity t : chain) {
            Vec3 mid = t.position().add(0, t.getBbHeight() / 2, 0);
            line(level, prev, mid, com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), CYAN);
            prev = mid;
            t.invulnerableTime = 0;
            t.hurt(source(level, user), 60F);
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), mid.x, mid.y, mid.z, 10, 0.3, 0.3, 0.3, 0.08);
        }
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 1.0F, 1.7F);
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
        return true;
    }

    private static void line(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions p1, ParticleOptions p2) {
        Vec3 d = b.subtract(a);
        int steps = Math.max(2, (int) (d.length() * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(d.scale(i / (double) steps));
            level.sendParticles(i % 2 == 0 ? p1 : p2, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ---------------------------------------------------------------- Big Bang

    static boolean bigBang(ServerLevel level, Player user, Vec3 at) {
        double r = 12;
        for (LivingEntity t : around(level, user, at, r)) {
            double d = t.position().distanceTo(at);
            float dmg = (float) (100 * (1 - 0.6 * d / r));
            t.invulnerableTime = 0;
            t.hurt(source(level, user), dmg);
            Vec3 away = t.position().subtract(at);
            Vec3 dir = away.lengthSqr() < 1e-4 ? new Vec3(0, 1, 0) : away.normalize();
            t.setDeltaMovement(t.getDeltaMovement().add(dir.x * 1.6, 0.7, dir.z * 1.6));
            t.hurtMarked = true;
            t.igniteForSeconds(4);
        }
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.BURST.get(), at.x, at.y, at.z, 3, 2, 2, 2, 0);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), at.x, at.y, at.z, 2, 0, 0, 0, 0);
        level.sendParticles(GOLD, at.x, at.y, at.z, 200, r / 3, r / 3, r / 3, 0);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), at.x, at.y, at.z, 200, 0.5, 0.5, 0.5, 0.8);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PLASMA.get(), at.x, at.y, at.z, 120, 0.5, 0.5, 0.5, 0.5);
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 4.0F, 0.5F);
        level.playSound(null, BlockPos.containing(at), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 3.0F, 0.6F);
        return true;
    }

    private ScepterEffects() {}
}
