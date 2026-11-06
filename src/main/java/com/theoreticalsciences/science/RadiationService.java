package com.theoreticalsciences.science;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Radiation exposure. Round 37 (1.1) counted carried items; round 38 (1.2) follows the physics:
 * <ul>
 *   <li><b>Carried</b>: each radioactive item adds its strength (data map radiation_source, or the tags) - at contact
 *   every kind of radiation counts, alpha included.</li>
 *   <li><b>Around you</b>: radioactive blocks within 6 and running machines within 16 (see {@link #emit}). Each gives
 *   strength / r² (inverse square), times what gets through the blocks in between: alpha nothing, beta nothing past a
 *   solid block, gamma e^(−Σμx), neutrons e^(−Σμₙx) (data map radiation_shielding).</li>
 *   <li><b>Background</b>: each dimension's natural dose rate (cosmic rays and the rock), {@link #backgroundMicroSvPerHour}.</li>
 * </ul>
 * Everything is worked out once a second, per player, near that player only. Dose is in mSv; it fades by 1% a second
 * as the body repairs itself. Past 100, 400, 1000 and 2500 mSv the Radiation Sickness gets worse.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class RadiationService {
    static final String KEY = "theoreticalsciences_rad_dose";
    private static final int[] LEVELS = {100, 400, 1000, 2500};
    static final int BLOCK_RANGE = 6, EMITTER_RANGE = 16;

    /** The last second's reading for a player: dose rates in mSv/s. */
    public record Reading(double carried, double around, double background, double fromMachines) {
        public double total() {
            return carried + around + background;
        }
    }

    private static final Map<UUID, Reading> LAST = new HashMap<>();

    /** Machines giving off radiation right now, per dimension: position → source, and the game time it lapses. */
    private record Emission(RadiationData.Source source, long until) {}

    // 1.2.0: cleared when the server stops (a younger world opened next in the same game had phantom dose)
    private static final Map<ResourceKey<Level>, Map<BlockPos, Emission>> EMITTERS = com.theoreticalsciences.world.SessionMaps.perServer(new HashMap<>());

    /** 1.2.2: how many machines give off radiation in this dimension right now. */
    public static int emitters(ServerLevel level) {
        Map<BlockPos, Emission> em = EMITTERS.get(level.dimension());
        if (em == null) return 0;
        long now = level.getGameTime();
        return (int) em.values().stream().filter(e -> e.until() >= now).count();
    }

    /** A running machine calls this (at least every second) while it gives off radiation. */
    public static void emit(ServerLevel level, BlockPos pos, Isotope iso, double strength) {
        EMITTERS.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .put(pos.immutable(), new Emission(new RadiationData.Source(iso, (float) strength), level.getGameTime() + 40));
    }

    // ------------------------------------------------------------------ background
    /**
     * 1.2.0: the background where you stand - in the Planetary Interior it depends on the biome: the Natural Reactor's
     * rock still holds its fission products (scaled up like every dose here); the others carry the crust's uranium.
     */
    public static double backgroundMicroSvPerHour(Level level, BlockPos pos) {
        if (level.dimension() == com.theoreticalsciences.world.QHDimensions.INTERIOR) {
            var b = level.getBiome(pos);
            if (b.is(com.theoreticalsciences.interior.InteriorHazards.REACTOR)) return 1.0e6;
            if (b.is(com.theoreticalsciences.interior.InteriorHazards.CORE)) return 2;
            return 0.6;
        }
        if (level.dimension() == com.theoreticalsciences.world.QHDimensions.FRONTIER) {
            var b = level.getBiome(pos);
            if (b.is(com.theoreticalsciences.frontier.FrontierHazards.PULSAR)) return 5.0e4;
            if (b.is(com.theoreticalsciences.frontier.FrontierHazards.RIM)) return 200;
            return 3;                                                    // open space: cosmic rays with no air to stop them
        }
        return backgroundMicroSvPerHour(level);
    }

    /** Natural dose rate by dimension, µSv/h (the Overworld's is the real world average, about 0.1 µSv/h at sea level). */
    public static double backgroundMicroSvPerHour(Level level) {
        ResourceLocation d = level.dimension().location();
        if (level.dimension() == Level.OVERWORLD) return 0.1;
        if (level.dimension() == Level.NETHER) return 0.25;              // hot rock: more radiogenic heat, more radiation
        if (level.dimension() == Level.END) return 1.5;                  // no atmosphere between you and the cosmic rays
        if (!d.getNamespace().equals(TheoreticalSciences.MODID)) return 0.1;
        return switch (d.getPath()) {
            case "quantum_foam", "platonic_realm" -> 0;                    // nothing there decays
            case "black_hole_interior" -> 40;
            case "dyson_sphere", "stellar_core" -> 25;
            default -> 0.5;
        };
    }

    // ------------------------------------------------------------------ what a player carries
    /** Dose rate from the inventory, mSv/s. */
    public static double carried(Player p) {
        double n = 0;
        var inv = p.getInventory();
        long now = p.level().getGameTime();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) n += carried(s) * Wear.activity(s, now);       // 1.2.0: a source grows weaker as it decays
        }
        return n;
    }

    public static double carried(ItemStack s) {
        RadiationData.Source src = RadiationData.source(s);
        return src == null ? 0 : src.strength() * s.getCount();
    }

    /** Kept for 1.1 callers: what the inventory gives off, in mSv/s. */
    public static double emission(Player p) {
        return carried(p);
    }

    // ------------------------------------------------------------------ the world around
    /** Dose rate at the player's eyes from blocks and machines around, mSv/s. */
    public static double around(ServerLevel level, Player p) {
        return aroundParts(level, p)[0];
    }

    /** {total, the part from machines} - the machines' part is also reported to Mekanism as a world source. */
    static double[] aroundParts(ServerLevel level, Player p) {
        Vec3 eye = p.getEyePosition();
        BlockPos c = p.blockPosition();
        double sum = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r = BLOCK_RANGE;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (!level.isLoaded(m.set(c.getX() + dx, c.getY(), c.getZ() + dz))) continue;
                for (int dy = -r; dy <= r + 1; dy++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir()) continue;
                    RadiationData.Source src = RadiationData.source(s);
                    if (src != null) sum += received(level, Vec3.atCenterOf(m), eye, src, m.immutable());
                }
            }
        double machines = 0;
        Map<BlockPos, Emission> em = EMITTERS.get(level.dimension());
        if (em != null) {
            long now = level.getGameTime();
            for (Iterator<Map.Entry<BlockPos, Emission>> it = em.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (e.getValue().until() < now) {
                    it.remove();
                    continue;
                }
                if (e.getKey().distSqr(c) > EMITTER_RANGE * EMITTER_RANGE) continue;
                machines += received(level, Vec3.atCenterOf(e.getKey()), eye, e.getValue().source(), e.getKey());
            }
        }
        return new double[]{sum + machines, machines};
    }

    /** What reaches {@code to} from a source at {@code from}: inverse square, times what the blocks between let through. */
    static double received(Level level, Vec3 from, Vec3 to, RadiationData.Source src, BlockPos self) {
        double d = from.distanceTo(to);
        double r = Math.max(0.5, d);
        Isotope.Kind kind = src.isotope().kind;
        if (kind == Isotope.Kind.ALPHA) return 0;                                // stopped by a few cm of air, or the skin
        double through = transmission(level, from, to, self, kind);
        return through <= 0 ? 0 : src.strength() / (r * r) * through;
    }

    /** e^(−Σμx) along the straight line, one sample every quarter block; beta: anything solid stops it. */
    static double transmission(Level level, Vec3 from, Vec3 to, BlockPos self, Isotope.Kind kind) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 1e-6) return 1;
        int steps = (int) Math.ceil(len / 0.25);
        double ds = len / steps, mu = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos last = null;
        RadiationData.Shielding sh = RadiationData.NONE;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            m.set(Math.floor(from.x + dir.x * t), Math.floor(from.y + dir.y * t), Math.floor(from.z + dir.z * t));
            if (m.equals(self)) continue;
            if (last == null || !last.equals(m)) {
                last = m.immutable();
                if (!level.isLoaded(m)) return 0;
                BlockState s = level.getBlockState(m);
                sh = RadiationData.shielding(s, level, m);
                if (kind == Isotope.Kind.BETA && sh.gamma() > 0) return 0;
            }
            mu += (kind == Isotope.Kind.NEUTRON ? sh.neutron() : sh.gamma()) * ds;
            if (mu > 30) return 0;
        }
        return Math.exp(-mu);
    }

    // ------------------------------------------------------------------ dose
    public static double dose(Player p) {
        return p.getPersistentData().getDouble(KEY);
    }

    public static void setDose(Player p, double d) {
        p.getPersistentData().putDouble(KEY, Math.max(0, d));
    }

    /** The last reading (mSv/s), or all zero before the first. */
    public static Reading reading(Player p) {
        return LAST.getOrDefault(p.getUUID(), new Reading(0, 0, 0, 0));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        double[] ar = aroundParts(level, p);
        Reading r = new Reading(carried(p), ar[0], backgroundMicroSvPerHour(level, p.blockPosition()) / 3.6e6, ar[1]);
        LAST.put(p.getUUID(), r);
        double rate = r.total();
        if (rate > 1e-6 && QHConfig.RADIATION_SHARE.get()) RadiationCompat.share(p, r.carried() + r.around() - r.fromMachines(), r.fromMachines());
        if (!QHConfig.RADIATION_SICKNESS.get()) {
            if (dose(p) > 0) setDose(p, 0);
            return;
        }
        double d = dose(p) * 0.99 + rate * com.theoreticalsciences.interior.InteriorGear.radiationFactor(p);   // 1.3.7: lead-lined armour
        setDose(p, d < 0.01 ? 0 : d);
        int amp = -1;
        for (int i = 0; i < LEVELS.length; i++) if (d >= LEVELS[i]) amp = i;
        if (amp >= 0) p.addEffect(new MobEffectInstance(QHEffects.RADIATION_SICKNESS, 100, amp, false, true));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        LAST.remove(e.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ units for people
    /** "0.10 µSv/h", "3.2 mSv/h", "1.4 Sv/h", from mSv/s. */
    public static String rateText(double mSvPerSecond) {
        double uSvh = mSvPerSecond * 3.6e6;
        if (uSvh < 1000) return String.format(java.util.Locale.ROOT, uSvh < 10 ? "%.2f µSv/h" : "%.0f µSv/h", uSvh);
        if (uSvh < 1e6) return String.format(java.util.Locale.ROOT, "%.1f mSv/h", uSvh / 1e3);
        return String.format(java.util.Locale.ROOT, "%.2f Sv/h", uSvh / 1e6);
    }

    /** Counts a Geiger-Müller tube gives: about 1.7 per second per µSv/h, until its dead time (0.1 ms) saturates it. */
    public static double countsPerSecond(double mSvPerSecond) {
        double n = 1.7 * mSvPerSecond * 3.6e6;
        return n / (1 + n * 1e-4);
    }

    private RadiationService() {}
}
