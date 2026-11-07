package com.theoreticalsciences.ocean;

import com.theoreticalsciences.registry.QHSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.4.0: the creatures of the Hadal Trench, ten kilometres down, in the dark. Both are the mod's own (no vanilla fish
 * mind), steered here; both glow, because down here nothing else does.
 */
public final class HadalMobs {

    /** The trench floor under a swimmer (or null within 12 blocks): it keeps the anglerfish low, where it waits. */
    static BlockPos floor(OceanMobs.Swimmer s) {
        BlockPos p = s.blockPosition();
        for (int i = 0; i < 12; i++) {
            if (!s.level().getFluidState(p.below(i)).isEmpty()) continue;
            return p.below(i);
        }
        return null;
    }

    // ================================================================== Hadal Anglerfish
    /**
     * A black, big-mouthed ambusher with a glowing lure on a rod over its head. It hangs still a few blocks over the floor
     * with the lure lit; whatever swims close enough to the light, it lunges at and bites. Its lure (the esca) holds the
     * glowing bacteria that light it.
     */
    public static class Anglerfish extends OceanMobs.Swimmer {
        int bite, lunge;

        public Anglerfish(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
            this.xpReward = 6;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.FOLLOW_RANGE, 16.0);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                if (random.nextInt(6) == 0) {
                    Vec3 lure = position().add(Vec3.directionFromRotation(0, getYRot()).scale(0.9)).add(0, 1.0, 0);
                    level().addParticle(ParticleTypes.GLOW, lure.x, lure.y, lure.z, 0, 0.005, 0);
                }
                return;
            }
            if (!isInWater()) return;
            Player prey = level().getNearestPlayer(this, 9);
            if (prey != null && (prey.isCreative() || prey.isSpectator() || !prey.isInWater())) prey = null;
            if (prey != null) {
                Vec3 d = prey.position().add(0, 0.6, 0).subtract(position());
                if (lunge > 0 || d.lengthSqr() < 36) {                  // close to the light: the lunge
                    if (lunge <= 0) {
                        lunge = 12;
                        level().playSound(null, blockPosition(), QHSounds.mob("hadal_anglerfish", "hurt"), SoundSource.HOSTILE, 0.6F, 0.6F);
                    }
                    steer(d, 0.09);
                } else steer(d, 0.012);                                  // drift a little nearer, lure lit
                if (--bite <= 0 && d.lengthSqr() < 2.4) {
                    bite = 30;
                    doHurtTarget(prey);
                }
            } else {
                // hang over the floor, turning slowly; never stray far up
                BlockPos f = floor(this);
                Vec3 w = new Vec3(random.nextGaussian() * 0.2, 0, random.nextGaussian() * 0.2);
                if (f == null) w = w.add(0, -0.6, 0);
                else if (getY() - f.getY() > 5) w = w.add(0, -0.2, 0);
                else if (getY() - f.getY() < 2) w = w.add(0, 0.2, 0);
                if (random.nextInt(4) == 0) steer(w, 0.004);
            }
            lunge--;
        }

        @Override
        public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
            boolean hit = target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (hit && target instanceof ServerPlayer sp)
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.anglerfish_bite")
                        .withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
            return hit;
        }
    }

    // ================================================================== Siphonophore Colony
    /**
     * Not one animal but a colony: a float at the front, swimming bells behind it, and a long trailing stem of feeding
     * and stinging zooids with curtains of tentacles. It drifts slowly; whatever brushes its length is stung (poisoned
     * and slowed). Some grow longer than a blue whale.
     */
    public static class Siphonophore extends OceanMobs.Swimmer {
        /** How far the stinging stem trails behind it, in blocks. */
        public static final double LENGTH = 6.0;
        int sting;

        public Siphonophore(EntityType<? extends WaterAnimal> type, Level level) {
            super(type, level);
            this.xpReward = 5;
        }

        public static AttributeSupplier.Builder createAttributes() {
            return WaterAnimal.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.2);
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide || !isInWater()) return;
            if ((tickCount + getId()) % 3 == 0) drift(0.006);
            if (--sting > 0 || (tickCount + getId()) % 10 != 0) return;
            // the stem trails back along its heading: a box from the float to the tail
            Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-LENGTH);
            AABB stem = getBoundingBox().expandTowards(back).inflate(0.6, 1.2, 0.6);
            for (Player p : level().getEntitiesOfClass(Player.class, stem, pl -> !pl.isCreative() && !pl.isSpectator())) {
                p.hurt(damageSources().mobAttack(this), 2.0F);
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                sting = 40;
                if (p instanceof ServerPlayer sp)
                    sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.theoreticalsciences.siphonophore_sting")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
            }
        }
    }

    private HadalMobs() {}
}
