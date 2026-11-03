package com.theoreticalsciences.science;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * Round 37 (1.1): hands this mod's radiation to the radiation systems of other mods, when they are installed, so their
 * detectors, protective gear and cures see it too. Reached through each mod's own public interface by name, so nothing
 * here is needed (or loaded) when the mod is absent, and a changed interface only switches that one link off.
 * <ul>
 *   <li>Mekanism: {@code IRadiationManager.INSTANCE.radiate(entity, sieverts)}.</li>
 *   <li>GregTech CEu: progresses the Carcinogen medical condition (GregTech's radioactive materials use it too).</li>
 *   <li>NuclearRadiation (NuclearCraft): no code - it reads data/theoreticalsciences/nuclear_radiation/bindings.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public final class RadiationCompat {
    /** Round 38 (1.2): the dose is in mSv now, so Mekanism (which counts in sieverts) is handed exactly the same dose. */
    static final double MEK_SV_PER_MSV = 1.0e-3;
    /** Progress of GregTech's carcinogen condition per mSv (as much per weakly radioactive item as in 1.1). */
    static final float GT_PER_MSV = 1.0F;

    private static Object mekManager;
    private static Method mekRadiate;
    private static boolean mekTried, mekDead;

    private static Method gtTracker, gtProgress;
    private static Object gtCarcinogen;
    private static boolean gtTried, gtDead;

    /**
     * {@code mSv}: dose from carried items and radioactive blocks; {@code fromMachines}: dose from running machines,
     * which Mekanism already applies itself through the world source each machine reports ({@link #worldSource}).
     */
    static void share(ServerPlayer p, double mSv, double fromMachines) {
        if (!mekDead && ModList.get().isLoaded("mekanism") && mSv > 0) mekanism(p, mSv);
        if (!gtDead && ModList.get().isLoaded("gtceu")) gregtech(p, mSv + fromMachines);
    }

    // ------------------------------------------------------------------ 1.2.1: machines as world sources
    /** Becquerels this mod's dose scale gives per mSv/s at one block (as the 1.1 item bindings: radium salt ≈ 3.6×10⁸ Bq). */
    static final double BQ_PER_MSV_S = 1.4e9;
    private static boolean mekWorldDead, nrDead;
    /** 1.2.2: what each link last did, for /theoreticalsciences radiation debug. */
    static String lastMekEntity = "never", lastMekWorld = "never", lastNr = "never";
    static long mekEntityCalls, mekWorldCalls, nrCalls;

    /**
     * A running machine (the Decay Chamber) reports itself, once a second, to the radiation systems that keep their own
     * map of sources, so their detectors (Mekanism's Geiger Counter, Nuclear Radiation's counters) read it and their
     * own exposure applies: Mekanism as a radiation source of strength (mSv/s at one block × 3.6) Sv/h, kept topped up;
     * Nuclear Radiation as a container source of caesium-137 with the same activity. {@code mSvPerSecond} 0 removes it.
     */
    public static void worldSource(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, double mSvPerSecond) {
        if (!com.theoreticalsciences.QHConfig.RADIATION_SHARE.get()) mSvPerSecond = 0;
        // 1.2.3: Nuclear Radiation takes Mekanism's world radiation over (it cancels Mekanism's radiate and turns each call into
        // a lingering contamination source of its own), so with it installed the chamber reports to Nuclear Radiation only
        if (!mekWorldDead && ModList.get().isLoaded("mekanism") && !ModList.get().isLoaded("nuclear_radiation"))
            mekanismWorld(level, pos, mSvPerSecond * 3.6);
        if (!nrDead && ModList.get().isLoaded("nuclear_radiation")) nuclearRadiation(level, pos, mSvPerSecond * 3.6);
    }

    private static void mekanismWorld(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, double svPerHour) {
        try {
            Class<?> mi = cls("mekanism.api.radiation.IRadiationManager");
            Object mgr = field(mi, "INSTANCE");
            if (svPerHour <= 0) {
                meth(mi, "removeRadiationSource", net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class).invoke(mgr, level, pos);
                return;
            }
            double now = 0;
            java.util.List<?> sources = (java.util.List<?>) meth(mi, "getRadiationSources", net.minecraft.world.level.Level.class, int.class, int.class)
                    .invoke(mgr, level, pos.getX() >> 4, pos.getZ() >> 4);
            Class<?> src = cls("mekanism.api.radiation.IRadiationSource");
            for (Object o : sources)
                if (pos.equals(meth(src, "getPosition").invoke(o))) now = (double) meth(src, "getMagnitude").invoke(o);
            if (svPerHour > now) meth(mi, "radiate", net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class, double.class)
                    .invoke(mgr, level, pos, svPerHour - now);           // Mekanism's sources decay by themselves: only top it up
            if (mekWorldCalls++ == 0) TheoreticalSciences.LOGGER.info("Theoretical Sciences: the Mekanism world-source link is working ({} Sv/h at {})", svPerHour, pos);
            lastMekWorld = String.format(java.util.Locale.ROOT, "%.3g Sv/h at %s (had %.3g) t=%d", svPerHour, pos.toShortString(), now, level.getGameTime());
        } catch (Throwable t) {
            mekWorldDead = true;
            TheoreticalSciences.LOGGER.warn("Theoretical Sciences: the Mekanism world-radiation link is off ({})", t.toString());
        }
    }

    /** Our Nuclear Radiation sources, by dimension@block. */
    private static final java.util.Map<String, Object> OURS = com.theoreticalsciences.world.SessionMaps.perServer(new java.util.HashMap<>());   // 1.1.0: cleared when the server stops

    /**
     * 1.2.3: removes Nuclear Radiation's lingering contamination sources ("left-over" sources) within {@code radius} blocks -
     * 1.2.1 and 1.2.2 created one every second at each running Decay Chamber, through Mekanism. Returns how many.
     */
    public static int cleanupLeftovers(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos at, double radius) {
        if (!ModList.get().isLoaded("nuclear_radiation")) return 0;
        try {
            Class<?> regC = cls("igentuman.nr.radiation.source.WorldSourceRegistry");
            Object reg = meth(regC, "get", net.minecraft.server.level.ServerLevel.class).invoke(null, level);
            Class<?> left = cls("igentuman.nr.radiation.source.LeftOverRadSource");
            Class<?> abs = cls("igentuman.nr.api.AbstractWorldRadSource");
            int n = 0;
            for (Object o : new java.util.ArrayList<>((java.util.Collection<?>) meth(regC, "all").invoke(reg))) {
                if (!left.isInstance(o)) continue;
                net.minecraft.core.BlockPos p = (net.minecraft.core.BlockPos) meth(abs, "getPosition").invoke(o);
                if (p.distSqr(at) > radius * radius) continue;
                meth(abs, "markDead").invoke(o);
                meth(regC, "remove", java.util.UUID.class).invoke(reg, meth(abs, "getId").invoke(o));
                n++;
            }
            return n;
        } catch (Throwable t) {
            TheoreticalSciences.LOGGER.warn("Theoretical Sciences: could not clean up Nuclear Radiation sources ({})", t.toString());
            return -1;
        }
    }

    private static void nuclearRadiation(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, double bq) {
        try {
            Class<?> regC = cls("igentuman.nr.radiation.source.WorldSourceRegistry");
            Object reg = meth(regC, "get", net.minecraft.server.level.ServerLevel.class).invoke(null, level);
            // our own source for this block (tracked by its id: another source - contamination - may sit on the same block)
            String key = level.dimension().location() + "@" + pos.asLong();
            Object existing = OURS.get(key);
            Class<?> contC = cls("igentuman.nr.radiation.source.ContainerRadSource");
            Class<?> abs = cls("igentuman.nr.api.AbstractWorldRadSource");
            java.util.Collection<?> all = (java.util.Collection<?>) meth(regC, "all").invoke(reg);
            if (existing != null && (!(boolean) meth(abs, "isActive").invoke(existing) || !all.contains(existing))) existing = null;
            if (bq <= 0) {
                if (existing != null) {
                    meth(abs, "markDead").invoke(existing);
                    meth(regC, "remove", java.util.UUID.class).invoke(reg, meth(abs, "getId").invoke(existing));
                }
                OURS.remove(key);
                return;
            }
            long now = level.getGameTime();
            // 1.2.5: the same isotope mix Nuclear Radiation itself builds for a Mekanism radiation source of this strength (Sv/h) -
            // the way the chamber first showed up in its counters. (A source weaker than Nuclear Radiation's world_source_min_bq,
            // 5×10⁹ Bq by default, is refused by its registry without a word: the caesium-only source of 1.2.3/1.2.4 was.)
            double svPerHour = bq;
            Object profile = meth(cls("igentuman.nr.integration.mekanism.MekanismHelper"), "buildProfile", double.class, long.class)
                    .invoke(null, svPerHour, now);
            Class<?> profC = cls("igentuman.nr.api.RadiationProfile");
            bq = (double) meth(profC, "totalActivityBq").invoke(profile);
            if (nrCalls++ == 0) TheoreticalSciences.LOGGER.info("Theoretical Sciences: the Nuclear Radiation world-source link is working ({} Bq at {})", bq, pos);
            lastNr = String.format(java.util.Locale.ROOT, "%.3g Bq at %s t=%d", bq, pos.toShortString(), now);
            if (existing != null) meth(abs, "refresh", profC).invoke(existing, profile);
            else {
                // 1.2.4: a free-standing source that does not spread contamination. (A container source on the chamber's own block
                // is removed again by Nuclear Radiation's container scanner, which only knows vanilla-style inventories.)
                Class<?> leftC = cls("igentuman.nr.radiation.source.LeftOverRadSource");
                Object srcObj = leftC.getConstructor(java.util.UUID.class, net.minecraft.resources.ResourceKey.class, net.minecraft.core.BlockPos.class, profC,
                        long.class, boolean.class).newInstance(java.util.UUID.randomUUID(), level.dimension(), pos.immutable(), profile, now, false);
                meth(regC, "register", cls("igentuman.nr.api.DecayGraph$WorldRadSource")).invoke(reg, srcObj);
                if (((java.util.Collection<?>) meth(regC, "all").invoke(reg)).contains(srcObj)) OURS.put(key, srcObj);
                else lastNr += " - REFUSED by Nuclear Radiation (below its world_source_min_bq)";
            }
        } catch (Throwable t) {
            nrDead = true;
            TheoreticalSciences.LOGGER.warn("Theoretical Sciences: the Nuclear Radiation world-source link is off ({})", t.toString());
        }
    }

    private static void mekanism(ServerPlayer p, double mSv) {
        try {
            if (!mekTried) {
                mekTried = true;
                Class<?> c = cls("mekanism.api.radiation.IRadiationManager");
                mekManager = field(c, "INSTANCE");
                mekRadiate = meth(c, "radiate", LivingEntity.class, double.class);
            }
            mekRadiate.invoke(mekManager, p, mSv * MEK_SV_PER_MSV);
            if (mekEntityCalls++ == 0) TheoreticalSciences.LOGGER.info("Theoretical Sciences: the Mekanism dose link is working ({} Sv to {})", mSv * MEK_SV_PER_MSV, p.getName().getString());
            lastMekEntity = String.format(java.util.Locale.ROOT, "%.3g Sv to %s at t=%d", mSv * MEK_SV_PER_MSV, p.getName().getString(), p.level().getGameTime());
        } catch (Throwable t) {
            mekDead = true;
            TheoreticalSciences.LOGGER.warn("Theoretical Sciences: the Mekanism radiation link is off ({})", t.toString());
        }
    }

    private static void gregtech(ServerPlayer p, double mSv) {
        try {
            if (!gtTried) {
                gtTried = true;
                Class<?> helper = cls("com.gregtechceu.gtceu.api.capability.GTCapabilityHelper");
                gtTracker = meth(helper, "getMedicalConditionTracker", Player.class);
                Class<?> conds;
                try {
                    conds = cls("com.gregtechceu.gtceu.data.medicalcondition.GTMedicalConditions");
                } catch (ClassNotFoundException old) {
                    conds = cls("com.gregtechceu.gtceu.common.data.GTMedicalConditions");      // GregTech 1.x / 7.x layout
                }
                gtCarcinogen = field(conds, "CARCINOGEN");
                Class<?> cond = cls("com.gregtechceu.gtceu.api.medicalcondition.MedicalCondition");
                gtProgress = meth(cls("com.gregtechceu.gtceu.api.capability.IMedicalConditionTracker"), "progressCondition", cond, float.class);
            }
            Object tracker = gtTracker.invoke(null, p);
            if (tracker != null) gtProgress.invoke(tracker, gtCarcinogen, (float) (mSv * GT_PER_MSV));
        } catch (Throwable t) {
            gtDead = true;
            TheoreticalSciences.LOGGER.warn("Theoretical Sciences: the GregTech radiation link is off ({})", t.toString());
        }
    }

    /** 1.2.2: what the links have done, and what Mekanism itself reads at the player - for /theoreticalsciences radiation debug. */
    public static java.util.List<String> debug(ServerPlayer p) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("share=" + com.theoreticalsciences.QHConfig.RADIATION_SHARE.get() + " mekanism=" + ModList.get().isLoaded("mekanism")
                + " gtceu=" + ModList.get().isLoaded("gtceu") + " nuclear_radiation=" + ModList.get().isLoaded("nuclear_radiation"));
        out.add("Mekanism dose link: " + (mekDead ? "OFF (failed)" : "ok") + ", calls " + mekEntityCalls + ", last " + lastMekEntity);
        out.add("Mekanism world link: " + (mekWorldDead ? "OFF (failed)" : "ok") + ", calls " + mekWorldCalls + ", last " + lastMekWorld);
        out.add("Nuclear Radiation link: " + (nrDead ? "OFF (failed)" : "ok") + ", calls " + nrCalls + ", last " + lastNr);
        if (ModList.get().isLoaded("mekanism")) {
            try {
                Class<?> mi = cls("mekanism.api.radiation.IRadiationManager");
                Object mgr = field(mi, "INSTANCE");
                out.add("Mekanism manager: " + mgr.getClass().getName() + ", enabled " + meth(mi, "isRadiationEnabled").invoke(mgr));
                out.add("Mekanism level at you: " + meth(mi, "getRadiationLevel", net.minecraft.world.entity.Entity.class).invoke(mgr, p) + " Sv/h");
                java.util.List<?> src = (java.util.List<?>) meth(mi, "getRadiationSources", net.minecraft.world.level.Level.class, int.class, int.class)
                        .invoke(mgr, p.level(), p.blockPosition().getX() >> 4, p.blockPosition().getZ() >> 4);
                out.add("Mekanism sources in your chunk: " + src.size());
                Object cap = field(cls("mekanism.common.capabilities.Capabilities"), "RADIATION_ENTITY");
                Object rad = p.getCapability((net.neoforged.neoforge.capabilities.EntityCapability<Object, Void>) cap);
                out.add("Mekanism dose on you: " + (rad == null ? "no capability" :
                        meth(cls("mekanism.api.radiation.capability.IRadiationEntity"), "getRadiation").invoke(rad) + " Sv"));
            } catch (Throwable t) {
                out.add("Mekanism query failed: " + t);
            }
        }
        return out;
    }

    /** Clears the dose the linked mods hold for this entity (used by /theoreticalsciences radiation reset). */
    public static void reset(LivingEntity e) {
        if (ModList.get().isLoaded("mekanism")) {
            try {
                Object cap = field(cls("mekanism.common.capabilities.Capabilities"), "RADIATION_ENTITY");
                Object rad = e.getCapability((net.neoforged.neoforge.capabilities.EntityCapability<Object, Void>) cap);
                if (rad != null) {
                    Class<?> mi = cls("mekanism.api.radiation.IRadiationManager");     // methods looked up on the public interfaces
                    Object mgr = field(mi, "INSTANCE");
                    double base = (double) meth(mi, "baselineRadiation").invoke(mgr);
                    meth(cls("mekanism.api.radiation.capability.IRadiationEntity"), "set", double.class).invoke(rad, base);
                }
            } catch (Throwable t) {
                TheoreticalSciences.LOGGER.warn("Theoretical Sciences: could not reset Mekanism radiation ({})", t.toString());
            }
        }
        if (ModList.get().isLoaded("gtceu") && e instanceof Player p) {
            try {
                Object tracker = meth(cls("com.gregtechceu.gtceu.api.capability.GTCapabilityHelper"), "getMedicalConditionTracker", Player.class)
                        .invoke(null, p);
                if (tracker != null) {
                    Class<?> conds;
                    try {
                        conds = cls("com.gregtechceu.gtceu.data.medicalcondition.GTMedicalConditions");
                    } catch (ClassNotFoundException old) {
                        conds = cls("com.gregtechceu.gtceu.common.data.GTMedicalConditions");
                    }
                    Object carcinogen = field(conds, "CARCINOGEN");
                    Class<?> cond = cls("com.gregtechceu.gtceu.api.medicalcondition.MedicalCondition");
                    meth(cls("com.gregtechceu.gtceu.api.capability.IMedicalConditionTracker"), "removeMedicalCondition", cond)
                            .invoke(tracker, carcinogen);
                }
            } catch (Throwable t) {
                TheoreticalSciences.LOGGER.warn("Theoretical Sciences: could not reset GregTech's carcinogen condition ({})", t.toString());
            }
        }
    }

    // ------------------------------------------------------------------ 1.1.0 (audit): looked up once, not every second
    // Every machine reports to these links once a second; finding a class or method by name each time is slow, so each
    // lookup is remembered (a class that is missing is remembered as missing and throws again at once - the link then
    // switches itself off, with one warning, as before).
    private static final java.util.Map<String, Object> LOOKUPS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Object MISSING = new Object();

    private static Class<?> cls(String name) throws ClassNotFoundException {
        Object c = LOOKUPS.computeIfAbsent("c:" + name, k -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                return MISSING;
            }
        });
        if (c == MISSING) throw new ClassNotFoundException(name);
        return (Class<?>) c;
    }

    private static Method meth(Class<?> c, String name, Class<?>... params) throws NoSuchMethodException {
        String key = "m:" + c.getName() + "#" + name + java.util.Arrays.toString(params);
        Object m = LOOKUPS.get(key);
        if (m == null) LOOKUPS.put(key, m = c.getMethod(name, params));
        return (Method) m;
    }

    private static Object field(Class<?> c, String name) throws ReflectiveOperationException {
        String key = "f:" + c.getName() + "#" + name;
        Object v = LOOKUPS.get(key);
        if (v == null) LOOKUPS.put(key, v = c.getField(name).get(null));
        return v;
    }

    private RadiationCompat() {}
}
