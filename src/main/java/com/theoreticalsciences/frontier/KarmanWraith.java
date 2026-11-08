package com.theoreticalsciences.frontier;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.4.0: the Kármán Wraith, a creature of the Jet Stream. It is the wake of something that is no longer there: a long
 * ribbon of air that rides the wind, rippling from side to side at a steady beat, as flow past any body does at the
 * Strouhal number (St = fd/v, about 0.2), and shedding a trail of eddies on alternate sides - a von Kármán vortex street.
 * <ul>
 *   <li>It drifts east with the wind, weaving across it; when a player comes near it turns into the wind and closes.</li>
 *   <li>Every few seconds it sheds a <b>gust</b> at its prey: a shove downwind and a little damage. Near the edge of an
 *   island that can mean a long fall.</li>
 *   <li>It sings the Aeolian tone of a wire in the wind (its own sounds).</li>
 * </ul>
 * Its own steering (no vanilla flying behaviour); the cosmetic ripple is computed from the world clock on both sides, so
 * nothing extra is synced.
 */
public class KarmanWraith extends FrontierMobs.Floater {
    /** Shedding period in ticks: a ripple left, a ripple right. */
    public static final int PERIOD = 24;
    int gust;

    public KarmanWraith(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.ARMOR, 2.0);
    }

    /** -1..1: where the ribbon is in its shedding cycle (used by the renderer too). */
    public static float shed(long gameTime, int id, float partial) {
        return (float) Math.sin(((gameTime + id * 7) % PERIOD + partial) / PERIOD * Math.PI * 2);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        long t = level().getGameTime();
        float s = shed(t, getId(), 0);
        Vec3 wind = JetStream.WIND;
        if (level().isClientSide) {
            // the vortex street: an eddy shed on alternate sides, every half period
            if ((t + getId() * 7) % (PERIOD / 2) == 0) {
                Vec3 side = new Vec3(-wind.z, 0, wind.x).normalize().scale(s > 0 ? 0.7 : -0.7);
                level().addParticle(ParticleTypes.CLOUD, getX() + side.x, getY() + 0.4, getZ() + side.z, wind.x * 0.4, 0, wind.z * 0.4);
            }
            return;
        }
        LivingEntity target = getTarget();
        Vec3 v = getDeltaMovement().scale(0.9);
        if (target != null && target.isAlive() && distanceToSqr(target) < 28 * 28) {
            // into the wind and towards its prey, still weaving
            Vec3 d = target.position().add(0, target.getBbHeight() + 1.0, 0).subtract(position());
            if (d.lengthSqr() > 0.01) v = v.add(d.normalize().scale(0.03));
            getLookControl().setLookAt(target);
            if (++gust >= 50 && distanceToSqr(target) < 12 * 12 && hasLineOfSight(target)) {
                gust = 0;
                Vec3 push = wind.normalize().scale(1.1).add(0, 0.25, 0);
                target.setDeltaMovement(target.getDeltaMovement().add(push));
                target.hurtMarked = true;
                target.hurt(damageSources().mobAttack(this), 3.0F);
                level().playSound(null, blockPosition(), QHSounds.mob("karman_wraith", "hurt"), SoundSource.HOSTILE, 0.7F, 1.6F);
                ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 14, 0.4, 0.4, 0.4, 0.15);
                if (target instanceof ServerPlayer sp)
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.karman_gust")
                            .withStyle(net.minecraft.ChatFormatting.AQUA), true);
            }
        } else {
            // riding the wind, weaving across it, holding its height
            v = v.add(wind.scale(0.012));
            double holdY = Math.max(JetStream.FLOOR + 8, Math.min(level().getMaxBuildHeight() - 12, getY()));
            v = v.add(0, (holdY - getY()) * 0.002, 0);
        }
        Vec3 across = new Vec3(-wind.z, 0, wind.x).normalize();
        v = v.add(across.scale(0.015 * s));
        setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Math.atan2(v.z, v.x) * 180 / Math.PI) - 90F;
            setYRot(yaw);
            yBodyRot = yaw;
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return dist > 96 * 96;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    /** Only players can be its prey. */
    @Override
    public boolean canAttack(LivingEntity e) {
        return e instanceof Player && super.canAttack(e);
    }
}
