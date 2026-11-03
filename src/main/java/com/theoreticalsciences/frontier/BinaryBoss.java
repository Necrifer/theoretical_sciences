package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * 1.10.0: The Binary, a mini-boss of the Accretion Frontier. Two serpents - a pair of compact stars - orbiting each other.
 * Gravitational waves carry off their orbital energy, so the separation shrinks as da/dt = −k/a³ (Peters' formula) and
 * they circle faster as they close (Kepler: ω ∝ a^(−3/2)). Kill both before they meet: if they merge, the merger's
 * shockwave hits everything within 16 blocks (no block is changed). Summoned by playing a Binary Black Hole Merger
 * record (a gravitational-wave recording) in the Frontier.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class BinaryBoss {
    static final double A0 = 12, A_MERGE = 1.6, K = 576;               // blocks; with /400 below, about three minutes to merge

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem());
        // a merger record, or its echo (1.10.1), played in the Frontier calls the pair out of the dark
        if (!key.getNamespace().equals(TheoreticalSciences.MODID) || !(key.getPath().equals("gw_record_bbh") || key.getPath().equals("gravitational_wave_echo")))
            return;
        if (sp.level().dimension() != QHDimensions.FRONTIER) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.binary_not_here").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 c = sp.position().add(sp.getLookAngle().multiply(8, 0, 8)).add(0, 4, 0);
        Serpent a = FrontierRegistry.BINARY_SERPENT.get().create(sl), b = FrontierRegistry.BINARY_SERPENT.get().create(sl);
        if (a == null || b == null) return;
        a.setup(c, 0, true, b.getUUID());
        b.setup(c, Math.PI, false, a.getUUID());
        sl.addFreshEntity(a);
        sl.addFreshEntity(b);
        if (!sp.isCreative()) e.getItemStack().shrink(1);
        sl.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.BINARY_SUMMON.get(), SoundSource.HOSTILE, 1.5F, 1F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.binary_summoned").withStyle(ChatFormatting.GOLD), false);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    public static class Serpent extends InteriorMobs.Base {
        static final EntityDataAccessor<Float> SEP = SynchedEntityData.defineId(Serpent.class, EntityDataSerializers.FLOAT);
        Vec3 centre;
        double phase, phi;
        boolean leader;
        UUID partner;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.theoreticalsciences.binary_serpent"),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

        public Serpent(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setNoGravity(true);
            noPhysics = true;
            this.xpReward = 60;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 120.0).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 8.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        }

        void setup(Vec3 c, double phase, boolean leader, UUID partner) {
            this.centre = c;
            this.phase = phase;
            this.leader = leader;
            this.partner = partner;
            entityData.set(SEP, (float) A0);
            setPos(c.x + Math.cos(phase) * A0 / 2, c.y, c.z + Math.sin(phase) * A0 / 2);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(SEP, (float) A0);
        }

        public float separation() {
            return entityData.get(SEP);
        }

        @Override
        protected void registerGoals() {}

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }

        @Override
        public void startSeenByPlayer(ServerPlayer p) {
            super.startSeenByPlayer(p);
            bar.addPlayer(p);
        }

        @Override
        public void stopSeenByPlayer(ServerPlayer p) {
            super.stopSeenByPlayer(p);
            bar.removePlayer(p);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 0, 0, 0);
                return;
            }
            if (centre == null) centre = position();
            ServerLevel sl = (ServerLevel) level();
            Entity other = partner == null ? null : sl.getEntity(partner);
            boolean pair = other instanceof Serpent s && s.isAlive();
            double a = separation();
            if (pair) {
                // Peters: da/dt = −k/a³ (the pair loses orbital energy to gravitational waves)
                a = Math.max(A_MERGE - 0.01, a - K / (a * a * a) / 400.0);
                // Kepler: ω ∝ a^(−3/2)
                phi += 0.9 * Math.pow(a / 4.0, -1.5) * 0.1;
                Vec3 to = centre.add(Math.cos(phi + phase) * a / 2, Math.sin(phi * 0.3) * 0.5, Math.sin(phi + phase) * a / 2);
                setPos(to.x, to.y, to.z);
                setYRot((float) Math.toDegrees(phi + phase));
                entityData.set(SEP, (float) a);
                bar.setName(Component.translatable("message.theoreticalsciences.binary_bar", String.format("%.1f", a)));
                bar.setProgress((float) Math.max(0, Math.min(1, (a - A_MERGE) / (A0 - A_MERGE))));
                if (leader && a <= A_MERGE) merge(sl, (Serpent) other);
            } else {
                // widowed: it hunts the one who killed its partner
                bar.setProgress(getHealth() / getMaxHealth());
                Player t = level().getNearestPlayer(this, 32);
                if (t != null && !t.isCreative() && !t.isSpectator()) {
                    Vec3 d = t.position().add(0, 1, 0).subtract(position());
                    setDeltaMovement(getDeltaMovement().scale(0.8).add(d.normalize().scale(0.06)));
                    move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
                }
            }
            // whatever it passes through is struck
            if (tickCount % 10 == 0)
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8), x -> !(x instanceof Serpent))) {
                    if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                    doHurtTarget(e);
                }
        }

        /** The merger: a ringdown flash and a shockwave. Nothing is left to loot - kill them before it. */
        void merge(ServerLevel sl, Serpent other) {
            sl.sendParticles(ParticleTypes.FLASH, centre.x, centre.y, centre.z, 3, 0.5, 0.5, 0.5, 0);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, centre.x, centre.y, centre.z, 12, 3, 1, 3, 0);
            sl.playSound(null, net.minecraft.core.BlockPos.containing(centre), com.theoreticalsciences.registry.QHSounds.BINARY_WAVE.get(), SoundSource.HOSTILE, 3F, 1F);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(centre, centre).inflate(16),
                    x -> !(x instanceof Serpent))) {
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                double r = Math.max(1, e.position().distanceTo(centre));
                e.hurt(sl.damageSources().magic(), (float) (24 / Math.sqrt(r)));
                Vec3 push = e.position().subtract(centre).normalize().scale(2.0 / Math.sqrt(r));
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.6, push.z));
                e.hurtMarked = true;
                if (e instanceof ServerPlayer p)
                    p.displayClientMessage(Component.translatable("message.theoreticalsciences.binary_merged").withStyle(ChatFormatting.RED), false);
            }
            other.discard();
            discard();
        }

        @Override
        public void remove(RemovalReason reason) {
            super.remove(reason);
            bar.removeAllPlayers();
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (centre != null) {
                tag.putDouble("Cx", centre.x);
                tag.putDouble("Cy", centre.y);
                tag.putDouble("Cz", centre.z);
            }
            tag.putDouble("Phase", phase);
            tag.putDouble("Phi", phi);
            tag.putBoolean("Leader", leader);
            tag.putFloat("Sep", separation());
            if (partner != null) tag.putUUID("Partner", partner);
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("Cx")) centre = new Vec3(tag.getDouble("Cx"), tag.getDouble("Cy"), tag.getDouble("Cz"));
            phase = tag.getDouble("Phase");
            phi = tag.getDouble("Phi");
            leader = tag.getBoolean("Leader");
            if (tag.contains("Sep")) entityData.set(SEP, tag.getFloat("Sep"));
            if (tag.hasUUID("Partner")) partner = tag.getUUID("Partner");
        }
    }

    private BinaryBoss() {}
}
