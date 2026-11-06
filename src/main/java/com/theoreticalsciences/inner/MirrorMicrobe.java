package com.theoreticalsciences.inner;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.bio.Immunity;
import com.theoreticalsciences.interior.InteriorMobs;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.2.0: the Mirror Microbe, a late hazard of the Antiverse: a bacterium built mirror-handed, as everything there is.
 * Nothing alive can digest it, and nothing in a body can grip it: its touch gives the Mirror Plague, which no Anomaly Ward
 * holds. Only a Mirror Antibody (the Mirror Culture Vat) wards it. It drifts toward the warmth of a player. At most four
 * near anyone; no block is changed.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public class MirrorMicrobe extends InteriorMobs.Base {
    int touch;

    public MirrorMicrobe(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        com.theoreticalsciences.entity.OwnAI.hunter(this);
    }

    @Override
    public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.MobSpawnType reason) {
        return true;
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        LivingEntity t = getTarget();
        Vec3 v = getDeltaMovement().scale(0.9);
        if (t != null && t.isAlive()) v = v.add(t.position().add(0, 1, 0).subtract(position()).normalize().scale(0.012));
        else v = v.add(Math.sin(tickCount * 0.05 + getId()) * 0.003, Math.cos(tickCount * 0.03) * 0.002, Math.cos(tickCount * 0.05 + getId()) * 0.003);
        setDeltaMovement(v);
        if (touch > 0) touch--;
        if (t instanceof ServerPlayer p && touch == 0 && distanceToSqr(p) < 1.6) {
            touch = 40;
            p.hurt(damageSources().mobAttack(this), 2F);
            Immunity.infect(p, Immunity.Infection.MIRROR_PLAGUE);
        }
    }

    @Override
    public boolean doHurtTarget(Entity t) {
        boolean hit = super.doHurtTarget(t);
        if (hit && t instanceof ServerPlayer p) Immunity.infect(p, Immunity.Infection.MIRROR_PLAGUE);
        return hit;
    }

    /** A few drift in round any player in the Antiverse (it is late, and far from life). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.isCreative() || p.level().dimension() != QHDimensions.ANTIVERSE) return;
        if ((p.tickCount + p.getId()) % 400 != 0) return;
        ServerLevel l = p.serverLevel();
        if (l.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        if (l.getEntitiesOfClass(MirrorMicrobe.class, p.getBoundingBox().inflate(48)).size() >= 4) return;
        var r = l.random;
        double a = r.nextDouble() * Math.PI * 2, d = 16 + r.nextDouble() * 16;
        int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
        if (!l.hasChunkAt(new BlockPos(x, 0, z))) return;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2 + r.nextInt(4);
        MirrorMicrobe m = InnerRegistry.MIRROR_MICROBE.get().create(l);
        if (m == null) return;
        m.moveTo(x + 0.5, y, z + 0.5, r.nextFloat() * 360, 0);
        if (!l.noCollision(m)) return;
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.NATURAL, null);
        l.addFreshEntity(m);
    }
}
