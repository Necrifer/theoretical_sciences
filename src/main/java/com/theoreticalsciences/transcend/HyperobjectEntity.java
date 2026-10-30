package com.theoreticalsciences.transcend;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What the Omnilex Engine becomes once it has done the impossible: a five-dimensional hypercube, only ever seen as a
 * shadow turning through itself.
 * <ol>
 *   <li><b>Collapse</b> (5 s): the engine's structure is eaten from the outside in; everything that falls in is gone.</li>
 *   <li><b>Pull</b> (until 30 s): every creature within 64 blocks is dragged toward it. Players who reach its heart
 *       are taken into the Transcendental.</li>
 *   <li><b>Rest</b> (forever after, also after the world is reloaded): it hangs where the engine was, a doorway that
 *       draws in anyone within 16 blocks; touching its heart takes a player into the Transcendental.</li>
 * </ol>
 */
public class HyperobjectEntity extends Entity {
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(HyperobjectEntity.class, EntityDataSerializers.INT);
    /** The Transcendental is closed (Shutdown Sequence running, or Sundered and not yet Rekindled): the doorway is dark. */
    private static final EntityDataAccessor<Boolean> SEALED = SynchedEntityData.defineId(HyperobjectEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int COLLAPSE_TICKS = 100, PULL_TICKS = 600;
    private final List<BlockPos> structure = new ArrayList<>();

    public HyperobjectEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static HyperobjectEntity spawn(ServerLevel level, Vec3 at, List<BlockPos> parts) {
        HyperobjectEntity e = new HyperobjectEntity(TranscendRegistry.HYPEROBJECT.get(), level);
        e.setPos(at);
        Vec3 c = at;
        e.structure.addAll(parts);
        e.structure.sort(Comparator.comparingDouble((BlockPos p) -> p.distToCenterSqr(c)));    // nearest last: eaten from the outside in
        java.util.Collections.reverse(e.structure);
        level.addFreshEntity(e);
        level.playSound(null, BlockPos.containing(at), QHSounds.OMNILEX_COLLAPSE.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
        return e;
    }

    public boolean sealed() {
        return entityData.get(SEALED);
    }

    public int age() {
        return entityData.get(AGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(AGE, 0);
        b.define(SEALED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        int age = age() + 1;
        entityData.set(AGE, age);
        Vec3 c = position();
        // 1. collapse: remove the structure from the outside in, no drops
        if (!structure.isEmpty()) {
            int per = Math.max(1, (int) Math.ceil(structure.size() / (double) Math.max(1, COLLAPSE_TICKS - age)));
            for (int i = 0; i < per && !structure.isEmpty(); i++) {
                BlockPos p = structure.remove(0);
                if (!sl.getBlockState(p).isAir()) {
                    sl.removeBlockEntity(p);
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                    if (sl.random.nextInt(4) == 0) sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.2);
                }
            }
        }
        // 2. pull. While it collapses (the first 30 s) it drags everything within 64 blocks; after that it rests as a
        // doorway that still draws in whoever comes within 16 blocks. Round 36: the resting doorway used to skip
        // creative players entirely and only reached 7 blocks - so in a world loaded again later (after an update, or
        // any restart past the first 30 s) it looked dead. Now anyone but a spectator is drawn in and can step through.
        boolean rest = age >= PULL_TICKS;
        // round 36: while the Transcendental is closed the doorway is sealed - it pulls nobody and says why (it used to
        // keep refusing silently, which looked like a broken Hyperobject). It opens again by itself after the Rekindling.
        net.minecraft.network.chat.Component closed = Omnipotence.closedReason(sl.getServer());
        if ((closed != null) != sealed()) entityData.set(SEALED, closed != null);
        if (closed != null) {
            if (age % 60 == 0)
                for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(16)))
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.hyperobject_sealed", closed)
                            .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC), true);
            if (age % 2 == 0) sl.sendParticles(com.theoreticalsciences.registry.QHParticles.VAPOUR.get(), c.x, c.y, c.z, 2, 1.5, 1.5, 1.5, 0.01);
            return;
        }
        double radius = rest ? 16 : 64;
        double strength = rest ? 0.08 : Math.min(0.35, 0.02 + age * 0.0015);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius))) {
            if (e.isSpectator()) continue;
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist > radius) continue;
            if (dist < 2.4 && age > 20) {
                if (e instanceof ServerPlayer sp && !sp.isOnPortalCooldown()) {
                    sp.setPortalCooldown();
                    TranscendentalTravel.enter(sp);
                }
                continue;
            }
            if (rest && e instanceof ServerPlayer sp && age % 60 == 0)
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.hyperobject_waits")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE, net.minecraft.ChatFormatting.ITALIC), true);
            if (rest && e instanceof ServerPlayer sp && sp.getAbilities().flying && dist > 6) continue;   // a flyer may still turn away
            e.setDeltaMovement(e.getDeltaMovement().scale(0.8).add(d.normalize().scale(strength * (1.2 - dist / radius))));
            e.hurtMarked = true;
            e.fallDistance = 0;
        }
        if (age % 80 == 0) sl.playSound(null, BlockPos.containing(c), QHSounds.HYPEROBJECT_HUM.get(), SoundSource.AMBIENT, age < PULL_TICKS ? 3F : 1.2F, 1F);
        if (age % 2 == 0) sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y, c.z, 2, 2, 2, 2, 0.05);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(AGE, t.getInt("Age"));
        structure.clear();
        for (long l : t.getLongArray("Structure")) structure.add(BlockPos.of(l));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Age", age());
        t.putLongArray("Structure", structure.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @SuppressWarnings("unused")
    private static Entity unused() {
        return null;
    }
}
