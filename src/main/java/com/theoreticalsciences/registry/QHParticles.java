package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class QHParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheoreticalSciences.MODID);

    /** Tachyons: the trail is drawn ahead of the particle, since they arrive before they leave. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TACHYON = PARTICLES.register("tachyon", () -> new SimpleParticleType(true) {});
    /** Virtual particle pairs popping in and out of the vacuum. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> QUANTUM_FOAM = PARTICLES.register("quantum_foam", () -> new SimpleParticleType(false) {});
    /** Thermal radiation leaking from a horizon. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAWKING = PARTICLES.register("hawking", () -> new SimpleParticleType(true) {});
    /** Matter spiralling into a singularity. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> INFALL = PARTICLES.register("infall", () -> new SimpleParticleType(true) {});
    /** Faint dark-matter motes. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DARK_MOTE = PARTICLES.register("dark_mote", () -> new SimpleParticleType(false) {});

    // ---- round 36: the mod's own particles, replacing every vanilla one it borrowed
    private static DeferredHolder<ParticleType<?>, SimpleParticleType> fx(String id, boolean always) {
        return PARTICLES.register(id, () -> new SimpleParticleType(always) {});
    }

    /** a bright point of light (was the end rod) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHOTON = fx("photon", false);
    /** a shard of folded space drifting through (was the portal motes) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHASE_MOTE = fx("phase_mote", false);
    /** a jagged discharge (was the electric spark) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARC_SPARK = fx("arc_spark", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLASH = fx("flash", true);
    /** a mathematical glyph rising (was the enchanting runes) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLYPH = fx("glyph", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VOID_INK = fx("void_ink", false);
    /** an expanding pressure ring (was the sonic boom) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE = fx("shockwave", true);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BURST = fx("burst", true);
    /** coolant vapour (was smoke and cloud) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VAPOUR = fx("vapour", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FAULT = fx("fault", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESOLVE = fx("resolve", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLASMA = fx("plasma", false);
    /** a coloured glowing mote (was the coloured dust) */
    public static final DeferredHolder<ParticleType<?>, ParticleType<QHGlowOptions>> GLOW = PARTICLES.register("glow", QHGlowOptions::type);

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }

    private QHParticles() {}
}
