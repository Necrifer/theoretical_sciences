package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status effects released by containment failures. */
public final class QHEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TheoreticalSciences.MODID);

    /**
     * Reality destabilisation: your existence is coming apart. You keep glitching a short way through space and take
     * steady damage. Creatures carrying it hit harder and move faster - and are drawn split into flickering fractal copies.
     */
    public static final DeferredHolder<MobEffect, MobEffect> REALITY_DESTABILISATION = EFFECTS.register("reality_destabilisation",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x80FFE0) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    if (!(e instanceof net.minecraft.world.entity.Mob) || e.getRandom().nextInt(3) == 0)
                        e.hurt(e.damageSources().magic(), 1.0F + amp);
                    if (e instanceof Player p) p.causeFoodExhaustion(1.0F * (amp + 1));
                    if (e.getRandom().nextInt(4) == 0) {
                        // a glitch: you briefly exist a little way off from where you were
                        double dx = (e.getRandom().nextDouble() - 0.5) * 1.6, dz = (e.getRandom().nextDouble() - 0.5) * 1.6;
                        if (e.level().noCollision(e, e.getBoundingBox().move(dx, 0, dz))) e.teleportTo(e.getX() + dx, e.getY(), e.getZ() + dz);
                    }
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % Math.max(10, 40 >> amp) == 0;
                }
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, TheoreticalSciences.id("destabilised_damage"), 0.5,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
             .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TheoreticalSciences.id("destabilised_speed"), 0.15,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

    /** Nanite infection: the nanites eat their host; if it dies infected, a new swarm bursts out of it. */
    public static final DeferredHolder<MobEffect, MobEffect> NANITE_INFECTION = EFFECTS.register("nanite_infection",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x40FFE0) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    e.hurt(e.damageSources().magic(), 1.0F + amp * 0.5F);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 30 == 0;
                }
            });

    /** Temporal dissonance: you keep slipping a moment forwards or backwards along your own path. */
    public static final DeferredHolder<MobEffect, MobEffect> TEMPORAL_DISSONANCE = EFFECTS.register("temporal_dissonance",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFC23D) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    Vec3 v = e.getDeltaMovement();
                    double k = e.getRandom().nextBoolean() ? -6.0 : 6.0;
                    Vec3 to = e.position().add(v.x * k, 0, v.z * k);
                    if (e.level().noCollision(e, e.getBoundingBox().move(to.subtract(e.position())))) e.teleportTo(to.x, to.y, to.z);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 50 == 0;
                }
            });

    // ------------------------------------------------------------------ inflicted by the expansion's creatures and dimensions
    private static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation MUL = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

    /** Decoherence (Planck Mites): your wavefunction is collapsing in the wrong places - you keep re-appearing a little way off. */
    public static final DeferredHolder<MobEffect, MobEffect> DECOHERENCE = EFFECTS.register("decoherence",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x80F0FF) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    double dx = (e.getRandom().nextDouble() - 0.5) * (2.0 + amp), dz = (e.getRandom().nextDouble() - 0.5) * (2.0 + amp);
                    if (e.level().noCollision(e, e.getBoundingBox().move(dx, 0, dz))) e.teleportTo(e.getX() + dx, e.getY(), e.getZ() + dz);
                    e.hurt(e.damageSources().magic(), 0.5F + amp * 0.5F);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 25 == 0;
                }
            });

    /** Vacuum Decay (Vacuum Drifters): a bubble of truer vacuum is eating you. Steady damage, and you cannot heal while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> VACUUM_DECAY = EFFECTS.register("vacuum_decay",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x202040) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    e.hurt(e.damageSources().magic(), 1.0F + amp);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 30 == 0;
                }
            });

    /** Infinite Regress (Ordinal Hounds): every hit leads to a bigger one. Damage you take is increased by 25% per level. */
    public static final DeferredHolder<MobEffect, MobEffect> INFINITE_REGRESS = EFFECTS.register("infinite_regress",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFD060) {});

    /** Spaghettification (Tidal Horrors, and falling toward a singularity): stretched thin - slow, and moving up or down hurts. */
    public static final DeferredHolder<MobEffect, MobEffect> SPAGHETTIFICATION = EFFECTS.register("spaghettification",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFF5020) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    if (Math.abs(e.getDeltaMovement().y) > 0.1) e.hurt(e.damageSources().magic(), 1.5F + amp);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 10 == 0;
                }
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TheoreticalSciences.id("spaghetti_speed"), -0.35, MUL));

    /** Time Dilation (inside the black and white holes): your clock runs slow against the world's. Slower moving, attacking and digging. */
    public static final DeferredHolder<MobEffect, MobEffect> TIME_DILATION = EFFECTS.register("time_dilation",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x8060FF) {}
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TheoreticalSciences.id("dilation_speed"), -0.3, MUL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, TheoreticalSciences.id("dilation_attack"), -0.4, MUL)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED, TheoreticalSciences.id("dilation_dig"), -0.5, MUL));

    // ------------------------------------------------------------------ dimension laws (what a dimension does to the unprotected)
    /** Dimensional Bleed (the Bulk, no Brane Tether): your mass leaks into the extra dimensions. Lighter, slower, and slowly emptied. */
    public static final DeferredHolder<MobEffect, MobEffect> DIMENSIONAL_BLEED = EFFECTS.register("dimensional_bleed",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6040C0) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    e.hurt(e.damageSources().magic(), 1.0F + amp);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 40 == 0;
                }
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TheoreticalSciences.id("bleed_speed"), -0.25, MUL)
             .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, TheoreticalSciences.id("bleed_health"), -0.3, MUL));

    /** Law Flux (Hyperdimensional Reality, no Primordial Charter): the laws of physics keep being rewritten around you. */
    public static final DeferredHolder<MobEffect, MobEffect> LAW_FLUX = EFFECTS.register("law_flux",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x40FFC0) {});

    /** Imperfection (the Platonic Realm, no Form of the Self): among perfect forms, a flawed copy is worn away. */
    public static final DeferredHolder<MobEffect, MobEffect> IMPERFECTION = EFFECTS.register("imperfection",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xE0E8FF) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    e.hurt(e.damageSources().magic(), 1.0F);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 60 == 0;
                }
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, TheoreticalSciences.id("imperfect_damage"), -0.5, MUL)
             .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED, TheoreticalSciences.id("imperfect_dig"), -0.7, MUL));

    /** Dissolution (the Transcendental, no Identity Kernel): surrounded by everything, you stop being anything in particular. */
    public static final DeferredHolder<MobEffect, MobEffect> DISSOLUTION = EFFECTS.register("dissolution",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xFFFFFF) {
                @Override
                public boolean applyEffectTick(LivingEntity e, int amp) {
                    if (e.getHealth() <= 4.0F) e.hurt(e.damageSources().magic(), 1.0F);
                    return true;
                }

                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
                    return duration % 40 == 0;
                }
            }.addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, TheoreticalSciences.id("dissolution_health"), -0.15, MUL));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private QHEffects() {}
}
