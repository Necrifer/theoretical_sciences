package com.theoreticalsciences.world;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class QHDimensions {
    /** The Bulk: higher-dimensional space between branes (defined by datapack JSON). */
    public static final ResourceKey<Level> BULK = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("the_bulk"));

    /** Hyperdimensional Reality Space: the World Tree of bubble universes above the Ocean of Primordial Law. */
    public static final ResourceKey<Level> HYPERREAL = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("hyperdimensional_reality"));

    /** The Platonic Realm: floating ideal solids of axiom stone, where prime, transcendental and fractal ores grow. */
    public static final ResourceKey<Level> PLATONIC = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("platonic_realm"));

    /** The Quantum Foam: Planck-scale spacetime, membranes of foam between bubbles of nothing, where matter blinks in and out. */
    public static final ResourceKey<Level> FOAM = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("quantum_foam"));

    /** The Transcendental: what is left when the Omnilex collapses. Everything, already made. */
    public static final ResourceKey<Level> TRANSCENDENTAL = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("transcendental"));

    /** Inside a black hole: debris falling forever toward the singularity. */
    public static final ResourceKey<Level> BLACK_HOLE = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("black_hole_interior"));

    /** Inside a white hole: the time-reverse of a black hole, where everything is thrown outward. */
    public static final ResourceKey<Level> WHITE_HOLE = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("white_hole_interior"));

    /** The Transfinite Hierarchy: ordinals, cardinals and the objects of higher dimensions. */
    public static final ResourceKey<Level> TRANSFINITE = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("transfinite_hierarchy"));

    /** The Living Proof: Alpha Omega's own mind, where every law must be understood before it lets you go. */
    public static final ResourceKey<Level> LIVING_PROOF = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("living_proof"));

    /** The Antiverse: a universe exactly like ours, made of antimatter (reached through the CPT Mirror). */
    public static final ResourceKey<Level> ANTIVERSE = ResourceKey.create(Registries.DIMENSION, TheoreticalSciences.id("antiverse"));

    private QHDimensions() {}
}
