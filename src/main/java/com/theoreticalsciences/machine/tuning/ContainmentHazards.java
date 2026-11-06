package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.entity.*;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What escapes when a multiblock's containment fails. Severity grows with the machine's tier and its overclock.
 * Everything here can be toned down: config "containmentFailureHazards" turns the consequences off entirely,
 * "containmentFailureDestroysBlocks" (and the mobGriefing game rule) keeps the terrain intact.
 */
public final class ContainmentHazards {

    public static void release(ServerLevel level, Vec3 c, TuningProfile p, int overclock) {
        release(level, c, p, overclock, 4);
    }

    /** @param reach roughly how far the machine's structure extends from its centre (some hazards escape past it) */
    public static void release(ServerLevel level, Vec3 c, TuningProfile p, int overclock, double reach) {
        if (!QHConfig.FAILURE_HAZARDS.get()) return;
        float s = p.severity() * (1F + overclock * 0.25F);
        boolean grief = ContainmentRupture.destroysBlocks(level);
        RandomSource r = level.random;
        switch (p.hazard()) {
            case DESTABILISATION -> {
                level.explode(null, c.x, c.y, c.z, Math.min(6F, s * 0.4F), false, grief ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
                cloud(level, c, s, 2400, new MobEffectInstance(QHEffects.REALITY_DESTABILISATION, 300, 1), com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get());
                // fractures heal back into the original blocks, so they happen even when failures may not destroy terrain
                com.theoreticalsciences.block.FracturedRealityBlock.seed(level, BlockPos.containing(c), (int) s, r);
                // villagers and the undead lose their identity entirely; every other creature caught is destabilised
                convert(level, c, s * 2, e -> e instanceof AbstractVillager || e instanceof net.minecraft.world.entity.monster.Zombie && !(e instanceof DestabilisedHusk),
                        QHEntities.DESTABILISED_HUSK.get(), TuningProfile.Hazard.DESTABILISATION);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(s * 2), e -> e instanceof Mob && e.isAlive()))
                    e.addEffect(new MobEffectInstance(QHEffects.REALITY_DESTABILISATION, 6000, 0));
            }
            case SPATIAL -> {
                level.addFreshEntity(ContainmentRupture.create(level, c, Math.min(8F, s * 0.6F), Math.min(10F, s * 0.8F), 1 + (int) (s / 4)));
                convert(level, c, s * 1.5F, e -> e instanceof Monster && !(e instanceof VoidWraith), QHEntities.VOID_WRAITH.get(), TuningProfile.Hazard.SPATIAL);
            }
            case INFECTIOUS -> {
                // swarms assemble inside the machine - and some have already crept out through the casing
                int n = Math.max(2, (int) (s / 2));
                for (int i = 0; i < n; i++) {
                    Vec3 at;
                    if (i % 2 == 0) at = c.add(r.nextGaussian() * 1.5, 0, r.nextGaussian() * 1.5);
                    else {
                        double a = r.nextDouble() * Math.PI * 2, d = reach + 3 + r.nextDouble() * 4;
                        at = ground(level, c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
                    }
                    Materialisation.swarm(level, at, i == 0 ? 4 : 2, i * 6);
                }
                cloud(level, c, s, 1200, new MobEffectInstance(QHEffects.NANITE_INFECTION, 400, 0), com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get());
            }
            case TEMPORAL -> {
                level.explode(null, c.x, c.y, c.z, Math.min(5F, s * 0.3F), false, Level.ExplosionInteraction.NONE);
                int echoes = 1 + (int) (s / 3);
                for (ServerPlayer pl : level.players()) if (pl.position().distanceTo(c) < s * 3) echoes++;
                for (int i = 0; i < echoes; i++) spawn(level, QHEntities.PARADOX_ECHO.get(), c, 3, r, TuningProfile.Hazard.TEMPORAL, i * 8);
                cloud(level, c, s, 1800, new MobEffectInstance(QHEffects.TEMPORAL_DISSONANCE, 400, 0), com.theoreticalsciences.registry.QHParticles.GLYPH.get());
            }
            case DIMENSIONAL -> {
                level.addFreshEntity(ContainmentRupture.create(level, c, Math.min(6F, s * 0.4F), Math.min(8F, s * 0.6F), 1));
                for (int i = 0; i < Math.max(1, (int) (s / 3)); i++) spawn(level, QHEntities.LAW_WRAITH.get(), c, 4, r, TuningProfile.Hazard.DIMENSIONAL, i * 10);
                cloud(level, c, s, 1800, new MobEffectInstance(MobEffects.LEVITATION, 60, 0), com.theoreticalsciences.registry.QHParticles.GLYPH.get());
            }
            case CHEMICAL -> {
                // the vessel splits: a burst, then a cloud of corrosive gas that poisons and eats at armour
                level.explode(null, c.x, c.y, c.z, Math.min(4F, s * 0.3F), false, grief ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
                cloud(level, c, s * 1.2F, 1800, new MobEffectInstance(MobEffects.POISON, 200, 1), com.theoreticalsciences.registry.QHParticles.VAPOUR.get());
                cloud(level, c, s * 0.8F, 1200, new MobEffectInstance(MobEffects.WEAKNESS, 400, 1), net.minecraft.core.particles.ParticleTypes.SNEEZE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(s * 1.5), LivingEntity::isAlive)) {
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        var st = e.getItemBySlot(slot);
                        if (!st.isEmpty() && st.isDamageableItem()) st.hurtAndBreak((int) (s * 4), e, slot);   // acid eats at it
                    }
                }
            }
            case RADIOLOGICAL -> {
                // the shielding gives way: a dose that falls off as 1/r², and a contaminated cloud left behind
                for (ServerPlayer pl : level.getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(s * 4))) {
                    if (pl.isCreative() || pl.isSpectator()) continue;
                    double d2 = Math.max(1, pl.position().distanceToSqr(c));
                    com.theoreticalsciences.science.RadiationService.setDose(pl, com.theoreticalsciences.science.RadiationService.dose(pl)
                            + 4000 * s / d2 * com.theoreticalsciences.interior.InteriorGear.radiationFactor(pl));
                }
                level.explode(null, c.x, c.y, c.z, Math.min(3F, s * 0.25F), false, Level.ExplosionInteraction.NONE);
                cloud(level, c, s, 2400, new MobEffectInstance(MobEffects.WITHER, 100, 0), net.minecraft.core.particles.ParticleTypes.GLOW);
                // what the burst caught rises again, still burning with fission products
                convert(level, c, s * 1.5F, e -> e instanceof net.minecraft.world.entity.monster.Zombie || e instanceof AbstractVillager,
                        com.theoreticalsciences.interior.InteriorRegistry.FISSION_HUSK.get(), TuningProfile.Hazard.RADIOLOGICAL);
                for (int i = 0; i < (int) (s / 4); i++)
                    spawn(level, com.theoreticalsciences.interior.InteriorRegistry.FISSION_HUSK.get(), c, 3, r, TuningProfile.Hazard.RADIOLOGICAL, i * 10);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            }
            case MAGNETIC -> {
                // the magnet quenches: its field collapses, flinging everything outward, arcing, magnetising what it catches
                for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(c, c).inflate(s * 2.5), Entity::isAlive)) {
                    Vec3 d = e.position().subtract(c);
                    double dist = Math.max(1, d.length());
                    Vec3 push = d.normalize().scale(Math.min(3, s / dist)).add(0, 0.4, 0);
                    e.push(push.x, push.y, push.z);
                    e.hurtMarked = true;
                    if (e instanceof LivingEntity le) {
                        le.addEffect(new MobEffectInstance(QHEffects.MAGNETIZED, 1200, 1));
                        le.hurt(level.damageSources().lightningBolt(), (float) Math.max(1, s * 2 / dist));
                    }
                }
                for (int i = 0; i < 2 + (int) (s / 3); i++) {
                    var bolt = EntityType.LIGHTNING_BOLT.create(level);
                    if (bolt == null) continue;
                    bolt.moveTo(c.x + r.nextGaussian() * s, c.y, c.z + r.nextGaussian() * s);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), c.x, c.y, c.z, 200, s, s * 0.5, s, 0.5);
                // the collapsing field leaves knots of current that will not die down
                for (int i = 0; i < (int) (s / 4); i++)
                    spawn(level, com.theoreticalsciences.interior.InteriorRegistry.GEODYNAMO_WRAITH.get(), c, 3, r, TuningProfile.Hazard.MAGNETIC, i * 10);
            }
            case THERMAL -> {
                // a blast of heat: everything near is set alight, harder the closer it stands
                level.explode(null, c.x, c.y, c.z, Math.min(5F, s * 0.4F), grief, grief ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(s * 1.8), LivingEntity::isAlive)) {
                    double d = Math.max(1, e.position().distanceTo(c));
                    e.igniteForSeconds((int) Math.max(3, 12 - d));
                    e.hurt(level.damageSources().inFire(), (float) (s * 3 / d));
                }
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.VAPOUR.get(), c.x, c.y, c.z, 160, s * 0.6, s * 0.4, s * 0.6, 0.15);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, c.x, c.y, c.z, 40, s * 0.4, 0.5, s * 0.4, 0.2);
                // the hottest of it rises as a plume that keeps itself burning
                for (int i = 0; i < (int) (s / 5); i++)
                    spawn(level, com.theoreticalsciences.interior.InteriorRegistry.PLUME_ELEMENTAL.get(), c, 3, r, TuningProfile.Hazard.THERMAL, i * 12);
            }
            case OPTICAL -> {
                // its optics fail at full power: a flash that blinds whoever can see it, and flying shards of glass
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, c.x, c.y, c.z, 3, 0.5, 0.5, 0.5, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(s * 4), LivingEntity::isAlive)) {
                    if (!e.hasLineOfSight(new net.minecraft.world.entity.Marker(EntityType.MARKER, level) {{ setPos(c); }})) continue;
                    double d = Math.max(1, e.position().distanceTo(c));
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) (200 * Math.min(1, s / d)) + 40, 0));
                    if (d < s * 1.5) e.hurt(level.damageSources().generic(), (float) (s * 2 / d));
                }
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                        net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()), c.x, c.y, c.z, 120, s * 0.5, s * 0.5, s * 0.5, 0.4);
                level.playSound(null, BlockPos.containing(c), net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.6F);
                // light bent where it should not be: an image that walks off on its own
                for (int i = 0; i < (int) (s / 4); i++)
                    spawn(level, com.theoreticalsciences.frontier.FrontierRegistry.LENS_PHANTOM.get(), c, 3, r, TuningProfile.Hazard.OPTICAL, i * 10);
            }
        }
    }

    private static void cloud(ServerLevel level, Vec3 c, float radius, int duration, MobEffectInstance effect, net.minecraft.core.particles.ParticleOptions particle) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, c.x, c.y - 1, c.z);
        cloud.setRadius(radius);
        cloud.setRadiusPerTick(-radius / duration * 0.5F);
        cloud.setDuration(duration);
        cloud.setWaitTime(10);
        cloud.setParticle(particle);
        cloud.addEffect(effect);
        level.addFreshEntity(cloud);
    }

    /** Creatures caught in the blast are changed by it - each held, glitching, while the change takes it. */
    private static void convert(ServerLevel level, Vec3 c, float radius, java.util.function.Predicate<LivingEntity> which, EntityType<? extends Mob> into,
                                TuningProfile.Hazard kind) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), e -> e instanceof Mob && e.isAlive() && which.test(e))) {
            if (e instanceof Mob m && !m.isPersistenceRequired() && !m.hasCustomName() && e.canChangeDimensions(level, level))
                Materialisation.convert(level, m, kind, into, level.random.nextInt(20));
        }
    }

    /** A creature takes shape near c (it forms over a couple of seconds, after `delay` ticks). */
    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, Vec3 c, double spread, RandomSource r, TuningProfile.Hazard kind, int delay) {
        Vec3 at = ground(level, c.x + r.nextGaussian() * spread, c.y + 1, c.z + r.nextGaussian() * spread);
        Materialisation.spawn(level, at, kind, type, delay);
    }

    /** The nearest standing spot at (x, z), searching a few blocks up and down from y. */
    static Vec3 ground(ServerLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), by = (int) Math.floor(y);
        for (int dy = 0; dy <= 8; dy++)
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                int yy = by + dy * sgn;
                p.set(bx, yy - 1, bz);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) continue;
                p.set(bx, yy, bz);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) continue;
                p.set(bx, yy + 1, bz);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) continue;
                return new Vec3(bx + 0.5, yy, bz + 0.5);
            }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return new Vec3(bx + 0.5, top, bz + 0.5);
    }

    private ContainmentHazards() {}
}
