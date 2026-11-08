package com.theoreticalsciences.weather;

import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.3.0: the Storm Elemental - a thunderstorm cell come to life. It only forms over warm sea during a thunderstorm (warm
 * water is what feeds a storm's updraft) and dissipates when the storm passes.
 * <ul>
 *   <li>It hangs high over its prey, billowing, its core flickering as charge separates in its updraft.</li>
 *   <li>Every few seconds, with open sky between them, it discharges: a lightning stroke onto its prey. The stroke is
 *   visual only (it never starts fires or changes blocks); the damage is dealt directly.</li>
 *   <li>Drops Fulgurite: the glass a stroke makes where it strikes.</li>
 * </ul>
 */
public class StormElemental extends InteriorMobs.Base {
    int discharge = 60, fading = -1;
    /** Client side: ticks left of the flash that lights the cloud through when it strikes (sent as an entity event). */
    private int flash;
    static final byte FLASH_EVENT = 61;

    /** For the renderer: how long the current flash still lasts (0 when it is not striking). */
    public int flashTicks() {
        return flash;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == FLASH_EVENT) flash = 6;
        else super.handleEntityEvent(id);
    }

    public StormElemental(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24F));
        com.theoreticalsciences.entity.OwnAI.hunter(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (flash > 0) flash--;
            if (random.nextInt(2) == 0)
                level().addParticle(ParticleTypes.CLOUD, getRandomX(1.4), getRandomY(), getRandomZ(1.4), 0, -0.01, 0);
            if (random.nextInt(30) == 0)
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(0.8), getY() + 0.8, getRandomZ(0.8), 0, 0, 0);
            return;
        }
        // the storm passes: it thins out and is gone
        // (the weather flag, not the sky's thunder level, which only ramps up over the first seconds of a storm)
        boolean storm = level().getLevelData().isThundering();
        if (storm) fading = -1;
        else if (fading < 0) fading = 100 + random.nextInt(100);
        if (fading > 0 && --fading == 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.8, getZ(), 40, 1.2, 0.8, 1.2, 0.05);
            discard();
            return;
        }
        LivingEntity t = getTarget();
        Vec3 v = getDeltaMovement().scale(0.85);
        if (t != null && t.isAlive()) {
            Vec3 want = t.position().add(0, 9, 0);                 // hangs high over its prey
            Vec3 d = want.subtract(position());
            if (d.lengthSqr() > 1) v = v.add(d.normalize().scale(0.03));
            getLookControl().setLookAt(t);
            if (--discharge <= 0 && distanceToSqr(t) < 24 * 24 && level().canSeeSky(t.blockPosition().above())) {
                discharge = 80 + random.nextInt(40);
                strike(t);
            }
        } else if (random.nextInt(40) == 0) {
            v = v.add((random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.05, (random.nextDouble() - 0.5) * 0.15);
        }
        setDeltaMovement(v);
    }

    void strike(LivingEntity t) {
        ServerLevel sl = (ServerLevel) level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(t.getX(), t.getY(), t.getZ());
            bolt.setVisualOnly(true);                               // no fire, no changed blocks: the damage is ours
            sl.addFreshEntity(bolt);
        }
        t.hurt(damageSources().lightningBolt(), 5.0F);
        sl.broadcastEntityEvent(this, FLASH_EVENT);
        sl.playSound(null, blockPosition(), QHSounds.mob("storm_elemental", "strike"), SoundSource.HOSTILE, 1.4F, 1.0F);
        if (t instanceof ServerPlayer sp)
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.storm_strike")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }
}
