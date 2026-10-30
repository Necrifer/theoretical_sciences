package com.theoreticalsciences.entity;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.registry.QHEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A spacetime rupture left by a failed singularity / spatial machine - in the spirit of a Draconic Reactor meltdown.
 * <ol>
 *   <li>It grows over ~10 seconds, with escalating warnings to everyone nearby.</li>
 *   <li>It drags loose items and creatures in; anything inside the horizon is torn apart.</li>
 *   <li>It eats the blocks inside its radius (unless disabled in the config or by the mobGriefing game rule).</li>
 *   <li>It detonates, and Void Wraiths crawl out of the wound.</li>
 * </ol>
 * The client draws it with the black-hole lensing shader (see EffectRenderer).
 */
public class ContainmentRupture extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ContainmentRupture.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(ContainmentRupture.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> PEAK = SynchedEntityData.defineId(ContainmentRupture.class, EntityDataSerializers.FLOAT);
    public static final int LIFETIME = 200;
    /** After growing for LIFETIME ticks it collapses into itself over this many, then detonates. */
    public static final int COLLAPSE = 30;
    /** Radius it starts at: the size of the unstable singularity the failing machine was showing a moment before. */
    public static final float START_RADIUS = 1.5F;

    private float maxRadius = 5F;
    private float blast = 6F;
    private int wraiths = 2;

    public ContainmentRupture(EntityType<? extends ContainmentRupture> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static ContainmentRupture create(ServerLevel level, Vec3 at, float maxRadius, float blast, int wraiths) {
        ContainmentRupture r = new ContainmentRupture(QHEntities.CONTAINMENT_RUPTURE.get(), level);
        r.setPos(at);
        r.maxRadius = maxRadius;
        r.blast = blast;
        r.wraiths = wraiths;
        return r;
    }

    public float radius() {
        return entityData.get(RADIUS);
    }

    /** 0..1 while growing; above 1 while collapsing (see collapse()). */
    public float progress() {
        return entityData.get(AGE) / (float) LIFETIME;
    }

    /** 0 until the collapse starts, then rises to 1 at the moment of detonation. */
    public float collapse() {
        return Math.max(0F, (entityData.get(AGE) - LIFETIME) / (float) COLLAPSE);
    }

    /** The largest radius it reached, before collapsing. */
    public float peak() {
        return entityData.get(PEAK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(RADIUS, START_RADIUS);
        b.define(AGE, 0);
        b.define(PEAK, START_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            float r = radius(), q = collapse();
            for (int i = 0; i < (q > 0 ? 8 : 3); i++) {
                // while it collapses, space streams inward much faster, from much further out
                Vec3 d = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(q > 0 ? peak() * (2.5 + 3 * q) : r * 2.5);
                double sp = q > 0 ? 0.18 + 0.3 * q : 0.08;
                level().addParticle(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), getX() + d.x, getY() + d.y, getZ() + d.z, -d.x * sp, -d.y * sp, -d.z * sp);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        int age = entityData.get(AGE) + 1;
        entityData.set(AGE, age);
        if (age > LIFETIME) {
            collapseTick(level, age - LIFETIME);
            return;
        }
        float r = Math.max(START_RADIUS, maxRadius * (float) Math.min(1.0, Math.pow(age / (double) (LIFETIME * 0.8), 0.6)));
        entityData.set(RADIUS, r);
        entityData.set(PEAK, r);

        if (age == 1 || age == LIFETIME / 2 || age == LIFETIME - 40) warn(level, age);
        if (age % 20 == 0) level.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.PULSE.get(), SoundSource.HOSTILE, 3.0F, 0.5F + age / (float) LIFETIME);

        // gravity well
        double reach = r * 3 + 4;
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(reach), e -> e instanceof LivingEntity || e instanceof ItemEntity)) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = position().subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist < 0.01) continue;
            e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(0.06 * (1 - dist / reach) + 0.015)));
            e.hurtMarked = true;
            if (dist < r && e instanceof LivingEntity le && age % 10 == 0) le.hurt(level.damageSources().fellOutOfWorld(), 4.0F);
            if (dist < r * 0.5 && e instanceof ItemEntity) e.discard();
        }
        // it eats the world around it
        if (age % 5 == 0 && destroysBlocks(level)) {
            BlockPos c = blockPosition();
            int ir = (int) Math.ceil(r);
            int budget = 400;
            for (int x = -ir; x <= ir && budget > 0; x++)
                for (int y = -ir; y <= ir && budget > 0; y++)
                    for (int z = -ir; z <= ir && budget > 0; z++) {
                        if (x * x + y * y + z * z > r * r) continue;
                        BlockPos p = c.offset(x, y, z);
                        BlockState st = level.getBlockState(p);
                        if (st.isAir() || st.getDestroySpeed(level, p) < 0) continue;      // never bedrock / unbreakables
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        budget--;
                    }
        }
    }

    /**
     * The end: the singularity collapses into itself. Its horizon shrinks to nothing while the space around it is
     * dragged in ever harder (the client draws the lensing spreading outward); only at the very end does the blast go off,
     * and the Void Wraiths take shape in the wound it leaves.
     */
    private void collapseTick(ServerLevel level, int t) {
        float peak = entityData.get(PEAK);
        float q = t / (float) COLLAPSE;
        entityData.set(RADIUS, Math.max(0.02F, peak * (1 - q) * (1 - q)));
        if (t == 1) {
            level.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.HOSTILE, 4F, 0.5F);
            level.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.CHARGE.get(), SoundSource.HOSTILE, 4F, 0.5F);
        }
        if (t % 6 == 0) level.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.HOSTILE, 2.5F, 0.5F + q);
        // the pull grows violent as it goes
        double reach = peak * 4 + 6;
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(reach), e -> e instanceof LivingEntity || e instanceof ItemEntity)) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = position().subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double dist = d.length();
            if (dist < 0.01) continue;
            e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale((0.05 + 0.12 * q) * (1 - dist / reach) + 0.02)));
            e.hurtMarked = true;
        }
        if (t < COLLAPSE) return;
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), getX(), getY(), getZ(), 2, 0, 0, 0, 0);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.SHOCKWAVE.get(), getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        level.explode(this, getX(), getY(), getZ(), blast, false,
                destroysBlocks(level) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        for (int i = 0; i < wraiths; i++)
            com.theoreticalsciences.machine.tuning.Materialisation.spawn(level, position().add(random.nextGaussian(), 0, random.nextGaussian()),
                    com.theoreticalsciences.machine.tuning.TuningProfile.Hazard.SPATIAL, QHEntities.VOID_WRAITH.get(), 10 + i * 12);
        discard();
    }

    public static boolean destroysBlocks(ServerLevel level) {
        return QHConfig.FAILURE_DESTROYS_BLOCKS.get() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private void warn(ServerLevel level, int age) {
        String key = age == 1 ? "message.theoreticalsciences.rupture_start" : age < LIFETIME - 50 ? "message.theoreticalsciences.rupture_growing" : "message.theoreticalsciences.rupture_imminent";
        for (ServerPlayer p : level.players())
            if (p.distanceToSqr(this) < 96 * 96)
                p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
        level.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.GATEWAY.get(), SoundSource.HOSTILE, 4.0F, 0.4F);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level().isClientSide) com.theoreticalsciences.client.ClientHooks.RUPTURES.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (level().isClientSide) com.theoreticalsciences.client.ClientHooks.RUPTURES.remove(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        maxRadius = tag.getFloat("MaxRadius");
        blast = tag.getFloat("Blast");
        wraiths = tag.getInt("Wraiths");
        entityData.set(AGE, tag.getInt("Age"));
        entityData.set(PEAK, Math.max(START_RADIUS, tag.getFloat("Peak")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("MaxRadius", maxRadius);
        tag.putFloat("Blast", blast);
        tag.putInt("Wraiths", wraiths);
        tag.putInt("Age", entityData.get(AGE));
        tag.putFloat("Peak", entityData.get(PEAK));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }
}
