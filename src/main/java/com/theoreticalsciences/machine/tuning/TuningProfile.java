package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.machine.MachineType;

import java.util.EnumMap;
import java.util.Map;

/**
 * How a multiblock must be tuned while it runs (in the spirit of GT: New Horizons, where big machines need
 * overclock / parallel choices, maintenance and attention rather than being "set and forget"),
 * and what escapes when its containment fails.
 *
 * @param param        lang key suffix of the machine's tuning control (e.g. "beam_focus")
 * @param drift        how the ideal setting moves while the machine runs
 * @param volatility   how fast it moves, and how fast containment wears down (1.0 = normal)
 * @param maxOverclock overclock levels available: each doubles speed and quadruples power draw (GT-style)
 * @param maxParallel  recipes that can run at once (inputs, outputs and power scale with it)
 * @param hazard       what a containment failure releases
 * @param severity     how bad it is (radius in blocks, roughly); overclocking makes it worse
 * @param driftScale   round 31: extra factor on how far the setting drifts (a pack's "drift_multiplier"; normally 1)
 * @param wearScale    round 31: extra factor on containment wear (a pack's "integrity_wear_multiplier"; normally 1)
 */
public record TuningProfile(String param, Drift drift, float volatility, int maxOverclock, int maxParallel, Hazard hazard, int severity,
                            float driftScale, float wearScale) {

    public TuningProfile(String param, Drift drift, float volatility, int maxOverclock, int maxParallel, Hazard hazard, int severity) {
        this(param, drift, volatility, maxOverclock, maxParallel, hazard, severity, 1F, 1F);
    }

    public enum Drift {
        /** wanders randomly - check on it now and then */
        WALK,
        /** swings back and forth on a long cycle - find the middle, or follow it */
        OSCILLATE,
        /** steadily falls - keep turning it down */
        DECAY,
        /** mostly still, but now and then jumps somewhere completely new */
        JUMP
    }

    public enum Hazard {
        /** reality destabilises: a blast, then the surroundings fracture into flickering fractal copies of themselves, creatures caught in it glitch and turn hostile */
        DESTABILISATION,
        /** a spacetime rupture grows, drags everything in, eats blocks, then detonates; Void Wraiths crawl out */
        SPATIAL,
        /** self-replicating Nanite Swarms escape; infected creatures burst into more swarms */
        INFECTIOUS,
        /** Paradox Echoes - hostile copies from a timeline that should not exist - and temporal dissonance */
        TEMPORAL,
        /** reality's laws tear: a rupture plus Law Wraiths that rewrite whatever they touch */
        DIMENSIONAL,
        // 1.2.0: the failures of the newer sciences' machines (see ContainmentHazards)
        /** a chemical plant ruptures: a corrosive, poisonous cloud spreads, eating at armour */
        CHEMICAL,
        /** shielding gives way: a burst of radiation (a dose that falls off as 1/r²) and a contaminated, lingering cloud */
        RADIOLOGICAL,
        /** a magnet quenches: its field collapses, flinging everything nearby, arcing, and magnetising whoever is caught */
        MAGNETIC,
        /** a furnace, press or plasma lets go: a blast of heat sets everything near alight */
        THERMAL,
        /** an instrument's optics fail at full power: a blinding flash, and shards of its lenses */
        OPTICAL
    }

    private static final Map<MachineType, TuningProfile> PROFILES = new EnumMap<>(MachineType.class);

    private static void p(MachineType t, String param, Drift d, float vol, int oc, int par, Hazard hz, int sev) {
        PROFILES.put(t, new TuningProfile(param, d, vol, oc, par, hz, sev));
    }

    static {
        p(MachineType.QUARK_GLUON_COLLIDER, "beam_focus", Drift.WALK, 1.0F, 3, 4, Hazard.DESTABILISATION, 6);
        p(MachineType.DARK_MATTER_COLLECTOR, "halo_lensing_angle", Drift.WALK, 0.6F, 2, 4, Hazard.SPATIAL, 4);
        p(MachineType.KUGELBLITZ_CHAMBER, "photon_convergence", Drift.JUMP, 1.2F, 2, 2, Hazard.SPATIAL, 7);
        p(MachineType.PENROSE_ERGOSPHERE, "ergosphere_phase", Drift.DECAY, 1.0F, 0, 1, Hazard.SPATIAL, 8);
        p(MachineType.WHITE_HOLE_EMITTER, "time_reversal_symmetry", Drift.OSCILLATE, 1.0F, 2, 2, Hazard.SPATIAL, 7);
        p(MachineType.TESSERACT_ASSEMBLER, "w_axis_alignment", Drift.OSCILLATE, 1.2F, 2, 2, Hazard.DIMENSIONAL, 5);
        p(MachineType.MULTIVERSE_RIFT, "brane_separation", Drift.JUMP, 1.6F, 2, 2, Hazard.DIMENSIONAL, 8);
        p(MachineType.UNIVERSE_FORGE, "inflation_rate", Drift.WALK, 1.4F, 1, 1, Hazard.SPATIAL, 10);
        p(MachineType.MATRIOSHKA_BRAIN, "shell_synchrony", Drift.OSCILLATE, 0.8F, 2, 2, Hazard.INFECTIOUS, 6);
        p(MachineType.DYSON_ARRAY, "orbital_phasing", Drift.OSCILLATE, 0.6F, 0, 1, Hazard.DESTABILISATION, 10);
        p(MachineType.ANNIHILATION_REACTOR, "dilithium_alignment", Drift.JUMP, 1.8F, 0, 1, Hazard.DESTABILISATION, 9);
        p(MachineType.NEUTRON_STAR_FORGE, "compression_ratio", Drift.DECAY, 1.2F, 3, 4, Hazard.DESTABILISATION, 7);
        p(MachineType.WORMHOLE_PROJECTOR, "throat_radius", Drift.WALK, 1.0F, 2, 2, Hazard.SPATIAL, 6);
        p(MachineType.CAUSALITY_LOOP_ENGINE, "loop_phase", Drift.OSCILLATE, 1.5F, 1, 1, Hazard.TEMPORAL, 7);
        p(MachineType.DARK_ENERGY_SIPHON, "vacuum_polarity", Drift.WALK, 0.8F, 3, 4, Hazard.SPATIAL, 6);
        p(MachineType.REALITY_COMPILER, "instruction_pipeline", Drift.JUMP, 1.0F, 2, 2, Hazard.INFECTIOUS, 7);
        p(MachineType.AXIOMATIC_BREACH_ENGINE, "axiom_interference", Drift.JUMP, 2.0F, 1, 1, Hazard.DIMENSIONAL, 10);
        p(MachineType.OMNILEX_ENGINE, "law_coherence", Drift.OSCILLATE, 1.8F, 2, 2, Hazard.DIMENSIONAL, 12);
        p(MachineType.FEMTOSCALE_ASSEMBLY_LINE, "arm_synchronisation", Drift.WALK, 0.5F, 3, 8, Hazard.INFECTIOUS, 4);
        p(MachineType.STELLAR_PLASMA_CRUCIBLE, "plasma_temperature", Drift.DECAY, 1.0F, 3, 4, Hazard.DESTABILISATION, 5);
        p(MachineType.FRACTIONATION_SPIRE, "reflux_ratio", Drift.WALK, 0.7F, 3, 8, Hazard.DESTABILISATION, 3);
        p(MachineType.SYNCHROTRON, "beam_orbit", Drift.WALK, 1.0F, 3, 4, Hazard.DESTABILISATION, 6);
        p(MachineType.TRANSCENDENT_PLASMA_FORGE, "plasma_confinement", Drift.DECAY, 1.4F, 2, 2, Hazard.DESTABILISATION, 10);
        p(MachineType.CONCEPT_COMPRESSOR, "concept_coherence", Drift.JUMP, 1.4F, 1, 1, Hazard.DIMENSIONAL, 9);
        p(MachineType.EYE_OF_THE_HORIZON, "horizon_symmetry", Drift.OSCILLATE, 1.6F, 1, 1, Hazard.SPATIAL, 12);
        // round 12: the last five multiblocks are tuned too (their physics models drive them)
        p(MachineType.NUCLIDE_FORGE, "beam_energy", Drift.WALK, 1.0F, 2, 2, Hazard.DESTABILISATION, 6);
        p(MachineType.MIRROR_FOUNDRY, "trap_depth", Drift.JUMP, 1.2F, 2, 2, Hazard.DESTABILISATION, 7);
        p(MachineType.EVOLUTION_ENGINE, "selection_pressure", Drift.WALK, 0.8F, 2, 4, Hazard.INFECTIOUS, 5);
        p(MachineType.ABIOGENESIS_CRUCIBLE, "reaction_temperature", Drift.OSCILLATE, 0.8F, 2, 4, Hazard.INFECTIOUS, 5);
        p(MachineType.RESEARCH_NEXUS, "significance", Drift.WALK, 0.4F, 1, 2, Hazard.DIMENSIONAL, 3);
        // its ideal setting is not a drift but a place: the resonance of the particle it is tuned to, where it stands
        p(MachineType.VACUUM_HARVESTER, "vacuum_resonance", Drift.WALK, 0.8F, 2, 4, Hazard.SPATIAL, 5);
        // 1.2.0: every multiblock since 1.0 fails too (their physics models drive their wear; this says what escapes)
        p(MachineType.PLEROMA_RECONSTITUTOR, "law_coherence", Drift.OSCILLATE, 1.2F, 1, 1, Hazard.DIMENSIONAL, 9);
        p(MachineType.ESSENCE_DISTILLERY, "reflux_ratio", Drift.WALK, 0.8F, 2, 2, Hazard.DIMENSIONAL, 5);
        p(MachineType.INFLATION_CHAMBER, "inflation_rate", Drift.WALK, 1.2F, 1, 1, Hazard.SPATIAL, 9);
        p(MachineType.ENTROPY_HORIZON, "horizon_symmetry", Drift.DECAY, 1.2F, 1, 1, Hazard.TEMPORAL, 9);
        p(MachineType.ANOMALY_CONTAINMENT_WARD, "concept_coherence", Drift.JUMP, 1.0F, 1, 2, Hazard.INFECTIOUS, 7);
        p(MachineType.ANOMALY_RESOLUTION_ENGINE, "law_coherence", Drift.JUMP, 1.2F, 1, 1, Hazard.DIMENSIONAL, 8);
        p(MachineType.TECTONIC_PRESS, "compression_ratio", Drift.DECAY, 0.8F, 2, 4, Hazard.THERMAL, 4);
        p(MachineType.DEEP_DRILLING_RIG, "drill_pressure", Drift.WALK, 0.8F, 2, 4, Hazard.THERMAL, 5);
        p(MachineType.GAS_CENTRIFUGE_CASCADE, "rotor_speed", Drift.WALK, 0.9F, 2, 4, Hazard.RADIOLOGICAL, 5);
        p(MachineType.MOLTEN_SALT_REACTOR, "neutron_flux", Drift.WALK, 1.0F, 0, 1, Hazard.RADIOLOGICAL, 8);
        p(MachineType.MHD_GENERATOR, "field_strength", Drift.OSCILLATE, 0.8F, 0, 1, Hazard.MAGNETIC, 5);
        p(MachineType.ESCHATON_FOUNDRY, "law_coherence", Drift.JUMP, 1.6F, 1, 1, Hazard.DIMENSIONAL, 11);
        p(MachineType.REFLECTING_TELESCOPE, "optical_alignment", Drift.WALK, 0.4F, 2, 4, Hazard.OPTICAL, 2);
        p(MachineType.SOLAR_FURNACE, "optical_alignment", Drift.WALK, 0.7F, 2, 4, Hazard.THERMAL, 5);
        p(MachineType.RADIO_TELESCOPE, "optical_alignment", Drift.WALK, 0.4F, 2, 4, Hazard.MAGNETIC, 2);
        p(MachineType.X_RAY_DIFFRACTOMETER, "beam_focus", Drift.WALK, 0.5F, 2, 4, Hazard.RADIOLOGICAL, 3);
        p(MachineType.CRYSTAL_GROWTH_FURNACE, "plasma_temperature", Drift.DECAY, 0.6F, 2, 4, Hazard.THERMAL, 4);
        p(MachineType.OPTICAL_WORKSHOP, "optical_alignment", Drift.WALK, 0.4F, 2, 4, Hazard.OPTICAL, 2);
        p(MachineType.LASER_INTERFEROMETER, "optical_alignment", Drift.OSCILLATE, 0.6F, 2, 2, Hazard.OPTICAL, 4);
        p(MachineType.POUND_REBKA_TOWER, "beam_focus", Drift.WALK, 0.5F, 2, 2, Hazard.RADIOLOGICAL, 3);
        p(MachineType.BROWNIAN_MICROSCOPE, "optical_alignment", Drift.WALK, 0.4F, 2, 4, Hazard.OPTICAL, 2);
        p(MachineType.MONTE_CARLO_ENGINE, "instruction_pipeline", Drift.JUMP, 0.6F, 2, 4, Hazard.INFECTIOUS, 3);
        p(MachineType.CYCLOTRON, "beam_orbit", Drift.WALK, 0.9F, 2, 2, Hazard.RADIOLOGICAL, 5);
        p(MachineType.TESLA_COIL, "field_strength", Drift.OSCILLATE, 0.8F, 2, 2, Hazard.MAGNETIC, 4);
        p(MachineType.NEUTRON_INTERFEROMETER, "beam_focus", Drift.WALK, 0.6F, 2, 2, Hazard.RADIOLOGICAL, 4);
        p(MachineType.ANALOG_BLACK_HOLE, "horizon_symmetry", Drift.OSCILLATE, 0.8F, 2, 2, Hazard.THERMAL, 3);
        p(MachineType.ACCRETION_DISK_REACTOR, "plasma_confinement", Drift.DECAY, 1.4F, 0, 1, Hazard.SPATIAL, 9);
        p(MachineType.TOKAMAK, "plasma_confinement", Drift.DECAY, 1.2F, 0, 1, Hazard.MAGNETIC, 8);
        p(MachineType.PULSED_MAGNET_LAB, "field_strength", Drift.JUMP, 1.0F, 2, 2, Hazard.MAGNETIC, 6);
        p(MachineType.HYPERVELOCITY_IMPACT_FORGE, "compression_ratio", Drift.JUMP, 1.0F, 2, 2, Hazard.THERMAL, 6);
        p(MachineType.BURST_OBSERVATORY, "optical_alignment", Drift.JUMP, 0.5F, 2, 4, Hazard.RADIOLOGICAL, 3);
        p(MachineType.CLIMATE_ENGINE, "orbital_phasing", Drift.OSCILLATE, 0.8F, 2, 2, Hazard.THERMAL, 6);
        p(MachineType.LENSING_CONCENTRATOR, "halo_lensing_angle", Drift.WALK, 0.8F, 0, 1, Hazard.OPTICAL, 6);
        p(MachineType.TIME_DILATION_CHAMBER, "loop_phase", Drift.OSCILLATE, 1.0F, 2, 2, Hazard.TEMPORAL, 6);
        p(MachineType.WARP_FIELD_FORGE, "throat_radius", Drift.WALK, 1.2F, 1, 2, Hazard.SPATIAL, 7);
        p(MachineType.UNRUH_ACCELERATOR, "beam_orbit", Drift.WALK, 1.0F, 2, 2, Hazard.THERMAL, 6);
        p(MachineType.MICRO_BLACK_HOLE_EVAPORATOR, "horizon_symmetry", Drift.DECAY, 1.4F, 0, 1, Hazard.RADIOLOGICAL, 9);
        p(MachineType.ER_EPR_WEAVER, "throat_radius", Drift.OSCILLATE, 1.2F, 1, 1, Hazard.SPATIAL, 8);
        p(MachineType.MARKOV_CHAIN_REFINERY, "reflux_ratio", Drift.WALK, 0.6F, 2, 4, Hazard.CHEMICAL, 4);
        p(MachineType.HABER_BOSCH_REACTOR, "reaction_temperature", Drift.WALK, 0.7F, 2, 4, Hazard.CHEMICAL, 5);
        p(MachineType.OSTWALD_CONVERTER, "reaction_temperature", Drift.WALK, 0.7F, 2, 4, Hazard.CHEMICAL, 5);
        p(MachineType.CONTACT_PROCESS_TOWER, "reaction_temperature", Drift.WALK, 0.7F, 2, 4, Hazard.CHEMICAL, 6);
        p(MachineType.CHROMATOGRAPHY_COLUMN, "reflux_ratio", Drift.WALK, 0.5F, 2, 4, Hazard.CHEMICAL, 3);
        p(MachineType.CRACKING_TOWER, "reaction_temperature", Drift.WALK, 0.8F, 2, 4, Hazard.CHEMICAL, 6);
        p(MachineType.NMR_SPECTROMETER, "field_strength", Drift.WALK, 0.6F, 2, 2, Hazard.MAGNETIC, 5);
        p(MachineType.WIND_TUNNEL, "flow_speed", Drift.OSCILLATE, 0.6F, 2, 4, Hazard.THERMAL, 2);   // 1.3.0
        p(MachineType.ROCKET_ASSEMBLY, "mixture_ratio", Drift.WALK, 0.8F, 1, 2, Hazard.THERMAL, 6);   // 1.4.0
        p(MachineType.BUSSARD_RAMSCOOP, "field_strength", Drift.OSCILLATE, 0.7F, 2, 4, Hazard.MAGNETIC, 4);   // 1.4.0
        p(MachineType.ICE_CORE_DRILL, "rotation_speed", Drift.WALK, 0.5F, 2, 4, Hazard.THERMAL, 2);
        p(MachineType.STORM_CHAMBER, "charge_separation", Drift.JUMP, 0.8F, 2, 4, Hazard.MAGNETIC, 4);
    }

    /** The profiles with a datapack's changes applied (data/<ns>/ts_machines), rebuilt when that data changes. */
    private static volatile Map<MachineType, TuningProfile> adjusted;
    private static volatile int adjustedFor = -1;

    public static TuningProfile of(MachineType type) {
        int gen = com.theoreticalsciences.custom.PackData.generation();
        Map<MachineType, TuningProfile> m = adjusted;
        if (m == null || adjustedFor != gen) {
            m = new EnumMap<>(MachineType.class);
            for (Map.Entry<MachineType, TuningProfile> e : PROFILES.entrySet()) {
                TuningProfile p = e.getValue();
                var c = com.theoreticalsciences.custom.MachineParams.get(e.getKey());
                m.put(e.getKey(), new TuningProfile(p.param, p.drift, p.volatility, c.maxOverclock() != null ? c.maxOverclock() : p.maxOverclock,
                        c.maxParallel() != null ? c.maxParallel() : p.maxParallel, p.hazard, p.severity, (float) c.drift(), (float) c.wear()));
            }
            adjusted = m;
            adjustedFor = gen;
        }
        return m.get(type);
    }
}
