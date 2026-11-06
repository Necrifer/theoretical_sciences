package com.theoreticalsciences.titan;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 1.2.0: the Ouroboros, Titan's mini-boss - a serpent biting its own tail, the dream in which Kekulé saw the ring of
 * benzene. It is a ring of six, and like benzene it is <b>aromatic</b>: closed, its bonds share every blow round the
 * ring (resonance), so it takes a quarter of the damage dealt. Every fifteen seconds the ring opens for five - the head
 * lets go of the tail - and then it takes double damage, but the head lunges. Closed, it spits tholin at everyone near.
 * At half health two Catalyst Sprites come to it. Summoned by Kekulé's Dream, used in Selk Crater. No block is changed.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class OuroborosBoss {
    public static final ResourceKey<Biome> SELK = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, TheoreticalSciences.id("selk_crater"));
    static final int CYCLE = 400, OPEN = 100;
    /** How long it takes to rise and close its ring when called, and to break apart when it dies (ticks). */
    public static final int RISE_TICKS = 80, DEATH_TICKS = 50;

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem());
        if (!key.getNamespace().equals(TheoreticalSciences.MODID) || !key.getPath().equals("kekules_dream")) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (sp.level().dimension() != QHDimensions.TITAN || !sp.level().getBiome(sp.blockPosition()).is(SELK)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.ouroboros_not_here").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel sl = sp.serverLevel();
        Serpent s = TitanRegistry.OUROBOROS.get().create(sl);
        if (s == null) return;
        Vec3 c = sp.position().add(sp.getLookAngle().multiply(8, 0, 8)).add(0, 3, 0);
        s.moveTo(c.x, c.y - 5, c.z, 0, 0);                     // it rises out of the crater floor
        s.home = c;
        sl.addFreshEntity(s);
        if (!sp.isCreative()) e.getItemStack().shrink(1);
        sl.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.OUROBOROS_SUMMON.get(), SoundSource.HOSTILE, 2F, 1F);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.ouroboros_summoned").withStyle(ChatFormatting.GOLD), false);
    }

    public static class Serpent extends InteriorMobs.Base {
        static final EntityDataAccessor<Integer> CLOCK = SynchedEntityData.defineId(Serpent.class, EntityDataSerializers.INT);
        static final EntityDataAccessor<Integer> RISE = SynchedEntityData.defineId(Serpent.class, EntityDataSerializers.INT);
        Vec3 home;
        boolean called;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.theoreticalsciences.ouroboros"),
                BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_6);

        public Serpent(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            setNoGravity(true);
            this.xpReward = 80;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 8.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder b) {
            super.defineSynchedData(b);
            b.define(CLOCK, 0);
            b.define(RISE, 0);
        }

        @Override
        protected void registerGoals() {
            com.theoreticalsciences.entity.OwnAI.hunter(this);
        }

        /** Ticks into the current cycle: open for the last OPEN of every CYCLE. */
        public int clock() {
            return entityData.get(CLOCK);
        }

        public boolean open() {
            return rising() ? false : clock() % CYCLE >= CYCLE - OPEN;
        }

        /** Still rising out of the crater: untouchable, and doing nothing else. */
        public boolean rising() {
            return entityData.get(RISE) < RISE_TICKS;
        }

        /** How far it has risen and closed its ring, 0 to 1 (for the renderer). */
        public float rise(float partial) {
            return Math.min(1F, (entityData.get(RISE) + (rising() ? partial : 0)) / RISE_TICKS);
        }

        @Override
        public boolean hurt(DamageSource src, float amount) {
            if (src.getEntity() == this || rising() && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            // aromatic: closed, the blow is shared by all six bonds; open, it falls on one broken bond
            return super.hurt(src, amount * (open() ? 2F : 0.25F));
        }

        @Override
        public boolean causeFallDamage(float d, float m, DamageSource s) {
            return false;
        }

        @Override
        public boolean canFreeze() {
            return false;
        }


        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) return;
            if (home == null) home = position().add(0, 5, 0);
            ServerLevel sl0 = (ServerLevel) level();
            if (rising()) {
                // the summoning: it spirals up out of the crater, tholin smoke coiling round it, until the ring closes
                int r = entityData.get(RISE) + 1;
                entityData.set(RISE, r);
                double f = r / (double) RISE_TICKS;
                setDeltaMovement(Vec3.ZERO);
                setPos(home.x, home.y - 5 * (1 - f), home.z);
                for (int i = 0; i < 3; i++) {
                    double a = r * 0.35 + i * Math.PI * 2 / 3, rr = 2.5 * (1 - f) + 1.2;
                    sl0.sendParticles(TitanMobs.THOLIN_DUST, getX() + Math.cos(a) * rr, getY() + 0.5 + i * 0.3, getZ() + Math.sin(a) * rr, 2, 0.05, 0.05, 0.05, 0);
                }
                if (r % 13 == 0)                                   // a segment joins the ring
                    sl0.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.OUROBOROS_OPEN.get(), SoundSource.HOSTILE, 0.8F, 0.7F + r / (float) RISE_TICKS);
                if (r == RISE_TICKS) {
                    sl0.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                    var roar = com.theoreticalsciences.registry.QHSounds.mob("ouroboros", "ambient");
                    if (roar != null) sl0.playSound(null, blockPosition(), roar, SoundSource.HOSTILE, 2F, 0.8F);
                }
                return;
            }
            int c = clock() + 1;
            entityData.set(CLOCK, c);
            ServerLevel sl = (ServerLevel) level();
            LivingEntity t = getTarget();
            int phase = c % CYCLE;
            if (phase == CYCLE - OPEN) {
                sl.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.OUROBOROS_OPEN.get(), SoundSource.HOSTILE, 1.5F, 1F);
                for (ServerPlayer p : bar.getPlayers())
                    p.displayClientMessage(Component.translatable("message.theoreticalsciences.ouroboros_open").withStyle(ChatFormatting.YELLOW), true);
            }
            Vec3 v = getDeltaMovement().scale(0.8);
            if (open() && t != null && t.isAlive()) {
                // the head lets go of the tail and strikes
                v = v.add(t.position().add(0, 1, 0).subtract(position()).normalize().scale(0.06));
                if (distanceTo(t) < 2.5 && c % 10 == 0 && doHurtTarget(t))
                    sl.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.OUROBOROS_ATTACK.get(), SoundSource.HOSTILE, 1.5F, 1F);
            } else {
                // closed, it turns slowly over its crater
                double a = c * 0.01;
                Vec3 want = home.add(Math.cos(a) * 4, 1 + Math.sin(a * 1.7), Math.sin(a) * 4);
                v = v.add(want.subtract(position()).scale(0.01));
                if (phase % 60 == 30) spit(sl);
            }
            setDeltaMovement(v);
            if (t != null) getLookControl().setLookAt(t);
            if (!called && getHealth() < getMaxHealth() * 0.5F) {
                called = true;
                for (int i = 0; i < 2; i++) {
                    var sprite = TitanRegistry.CATALYST_SPRITE.get().create(sl);
                    if (sprite == null) continue;
                    sprite.moveTo(getX() + (i * 2 - 1) * 3, getY() + 1, getZ(), 0, 0);
                    sl.addFreshEntity(sprite);
                }
            }
            bar.setProgress(getHealth() / getMaxHealth());
            bar.setColor(open() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
            if (c % 20 == 0)
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) bar.addPlayer(p);
            for (ServerPlayer p : java.util.List.copyOf(bar.getPlayers()))
                if (!p.isAlive() || p.level() != level() || p.distanceTo(this) > 64) bar.removePlayer(p);
        }

        /** Tholin, spat at everyone within 24 blocks it can see: a little damage, and darkness. */
        void spit(ServerLevel sl) {
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24))) {
                if (p.isCreative() || p.isSpectator() || !hasLineOfSight(p)) continue;
                Vec3 a = position().add(0, 1, 0), b = p.position().add(0, 1, 0);
                for (int i = 0; i <= 10; i++) {
                    Vec3 q = a.lerp(b, i / 10.0);
                    sl.sendParticles(TitanMobs.THOLIN_DUST, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0);
                }
                p.hurt(damageSources().mobAttack(this), 3F);
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true));
            }
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 10, 0.5, 0.3, 0.5, 0.02);
            sl.playSound(null, blockPosition(), com.theoreticalsciences.registry.QHSounds.OUROBOROS_ATTACK.get(), SoundSource.HOSTILE, 1.2F, 1.3F);
        }

        /** Its death: the ring breaks and its segments fly apart (drawn by the renderer), over DEATH_TICKS. */
        @Override
        protected void tickDeath() {
            ++deathTime;
            if (level() instanceof ServerLevel sl) {
                if (deathTime % 8 == 1)
                    sl.sendParticles(ParticleTypes.EXPLOSION, getX() + random.nextGaussian(), getY() + 1 + random.nextGaussian() * 0.5, getZ() + random.nextGaussian(), 1, 0, 0, 0, 0);
                sl.sendParticles(TitanMobs.THOLIN_DUST, getX(), getY() + 1, getZ(), 6, 1.2, 0.6, 1.2, 0.02);
                if (deathTime >= DEATH_TICKS && !isRemoved()) {
                    sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
                    sl.broadcastEntityEvent(this, (byte) 60);
                    remove(RemovalReason.KILLED);
                }
            }
        }

        @Override
        public void remove(RemovalReason r) {
            super.remove(r);
            bar.removeAllPlayers();
        }

        @Override
        public boolean removeWhenFarAway(double dist) {
            return false;
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (home != null) {
                tag.putDouble("HomeX", home.x);
                tag.putDouble("HomeY", home.y);
                tag.putDouble("HomeZ", home.z);
            }
            tag.putBoolean("Called", called);
            tag.putInt("Rise", entityData.get(RISE));
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("HomeX")) home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
            called = tag.getBoolean("Called");
            entityData.set(RISE, tag.contains("Rise") ? tag.getInt("Rise") : RISE_TICKS);
        }
    }

    private OuroborosBoss() {}
}
