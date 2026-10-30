package com.theoreticalsciences.registry;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creature sounds. The audio is listed in assets/theoreticalsciences/sounds.json. */
public final class QHSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheoreticalSciences.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> s(String name) {
        return SOUNDS.register("entity." + name, () -> SoundEvent.createVariableRangeEvent(TheoreticalSciences.id("entity." + name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent>
            HUSK_AMBIENT = s("destabilised_husk.ambient"), HUSK_HURT = s("destabilised_husk.hurt"),
            HUSK_DEATH = s("destabilised_husk.death"), HUSK_STEP = s("destabilised_husk.step"),
            WRAITH_AMBIENT = s("void_wraith.ambient"), WRAITH_HURT = s("void_wraith.hurt"), WRAITH_DEATH = s("void_wraith.death"),
            WRAITH_CHARGE = s("void_wraith.charge"), WRAITH_BLINK = s("void_wraith.blink"),
            SWARM_AMBIENT = s("nanite_swarm.ambient"), SWARM_HURT = s("nanite_swarm.hurt"), SWARM_DEATH = s("nanite_swarm.death"),
            SWARM_SPLIT = s("nanite_swarm.split"),
            ECHO_AMBIENT = s("paradox_echo.ambient"), ECHO_HURT = s("paradox_echo.hurt"), ECHO_DEATH = s("paradox_echo.death"),
            ECHO_STEP = s("paradox_echo.step"), ECHO_REWIND = s("paradox_echo.rewind"),
            LAW_AMBIENT = s("law_wraith.ambient"), LAW_HURT = s("law_wraith.hurt"), LAW_DEATH = s("law_wraith.death"),
            LAW_SHOOT = s("law_wraith.shoot"),
            WISP_AMBIENT = s("graviton_wisp.ambient"), WISP_HURT = s("graviton_wisp.hurt"), WISP_DEATH = s("graviton_wisp.death"),
            WISP_PULL = s("graviton_wisp.pull"),
            STRIDER_AMBIENT = s("brane_strider.ambient"), STRIDER_HURT = s("brane_strider.hurt"), STRIDER_DEATH = s("brane_strider.death"),
            STRIDER_STEP = s("brane_strider.step"), STRIDER_PHASE = s("brane_strider.phase"),
            CRAWLER_AMBIENT = s("moduli_crawler.ambient"), CRAWLER_HURT = s("moduli_crawler.hurt"), CRAWLER_DEATH = s("moduli_crawler.death"),
            CRAWLER_STEP = s("moduli_crawler.step"),
            MACHINE_STARTUP = s("machine.startup"), MACHINE_SHUTDOWN = s("machine.shutdown"),
            FOAM_POP = s("foam.pop"), OMNILEX_WARNING = s("omnilex.warning"), OMNILEX_COLLAPSE = s("omnilex.collapse"),
            HYPEROBJECT_HUM = s("hyperobject.hum"), OMNIPOTENCE_GRANT = s("omnipotence.grant"),
            WYRM_AMBIENT = s("fractal_wyrm.ambient"), WYRM_HURT = s("fractal_wyrm.hurt"), WYRM_DEATH = s("fractal_wyrm.death"),
            SENTINEL_AMBIENT = s("axiom_sentinel.ambient"), SENTINEL_HURT = s("axiom_sentinel.hurt"), SENTINEL_DEATH = s("axiom_sentinel.death"),
            SENTINEL_CHARGE = s("axiom_sentinel.charge"), SENTINEL_FIRE = s("axiom_sentinel.fire");

    /**
     * Round 36: the mod's own effect sounds - everything it used to borrow from vanilla (beacons, amethyst, wardens,
     * portals, buckets...) now has a synthesised voice of its own.
     */
    public static final DeferredHolder<SoundEvent, SoundEvent>
            UI_CLICK = s("fx.ui_click"), POWER_UP = s("fx.power_up"), POWER_DOWN = s("fx.power_down"), CONFIRM = s("fx.confirm"),
            RESONATE = s("fx.resonate"), CHIME = s("fx.chime"), SHATTER = s("fx.shatter"), PHASE_SHIFT = s("fx.phase_shift"),
            RIFT_HUM = s("fx.rift_hum"), CHARGE = s("fx.charge"), PULSE = s("fx.pulse"), EMERGENCE = s("fx.emergence"),
            SHOCKWAVE = s("fx.shockwave"), ASCEND = s("fx.ascend"), GATEWAY = s("fx.gateway"), VENT = s("fx.vent"),
            FLUID_FILL = s("fx.fluid_fill"), FLUID_EMPTY = s("fx.fluid_empty"), MUTATE = s("fx.mutate"),
            HATCH_OPEN = s("fx.hatch_open"), HATCH_CLOSE = s("fx.hatch_close"), EQUIP = s("fx.equip"), ALARM = s("fx.alarm");

    private static final DeferredHolder<SoundEvent, SoundEvent>
            MB_BREAK = s("machine_block.break"), MB_STEP = s("machine_block.step"), MB_PLACE = s("machine_block.place"),
            MB_HIT = s("machine_block.hit"), MB_FALL = s("machine_block.fall"),
            CB_BREAK = s("crystal_block.break"), CB_STEP = s("crystal_block.step"), CB_PLACE = s("crystal_block.place"),
            CB_HIT = s("crystal_block.hit"), CB_FALL = s("crystal_block.fall");

    /** Machines, casings and structure parts: servos and magnetic latches instead of netherite. */
    public static final net.minecraft.world.level.block.SoundType MACHINE = new net.neoforged.neoforge.common.util.DeferredSoundType(1.0F, 1.0F,
            MB_BREAK, MB_STEP, MB_PLACE, MB_HIT, MB_FALL);
    /** Crystals and glass of the mod. */
    public static final net.minecraft.world.level.block.SoundType CRYSTAL = new net.neoforged.neoforge.common.util.DeferredSoundType(1.0F, 1.0F,
            CB_BREAK, CB_STEP, CB_PLACE, CB_HIT, CB_FALL);

    /** ambient / hurt / death for the expansion's creatures, looked up by entity id. */
    public static final String[] NEW_MOBS = {"virtual_pair", "planck_mite", "vacuum_drifter", "hilbert_bellhop", "ordinal_hound", "hypercube_drifter",
            "tidal_horror", "photon_moth", "annihilator", "positronium_skitterer", "antihydrogen_wisp"};
    private static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> MOB = new java.util.HashMap<>();

    static {
        for (String m : NEW_MOBS) for (String k : new String[]{"ambient", "hurt", "death"}) MOB.put(m + "." + k, s(m + "." + k));
    }

    /** The soundtrack: one piece per dimension, the Living Proof, and Alpha Omega. */
    public static final String[] TRACKS = {"the_bulk", "hyperdimensional_reality", "platonic_realm", "quantum_foam", "transfinite_hierarchy",
            "black_hole_interior", "white_hole_interior", "transcendental", "living_proof", "alpha_omega", "antiverse"};
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> MUSIC = new java.util.HashMap<>();

    static {
        for (String t : TRACKS)
            MUSIC.put(t, SOUNDS.register("music." + t, () -> SoundEvent.createVariableRangeEvent(TheoreticalSciences.id("music." + t))));
    }

    public static SoundEvent mob(String mob, String kind) {
        DeferredHolder<SoundEvent, SoundEvent> h = MOB.get(mob + "." + kind);
        return h == null ? null : h.get();
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private QHSounds() {}
}
