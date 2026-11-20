package com.theoreticalsciences.orbit;

import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.13.0, Patch C2: the station grows.
 * <ul>
 *   <li><b>Modules</b>: the Habitat Ring Module (aboard the station, within 12 blocks your weight comes back and
 *   deconditioning does not build up) and the Storm Shelter Module (within 2 blocks, a fiftieth of a storm's dose). The
 *   station keeps where they stand, so nothing is searched for.</li>
 *   <li><b>Deconditioning</b> (config): in the station's free fall bone and muscle waste, a Minecraft day for a week.
 *   Back in gravity you are slow after a day's worth, weak too after three, until you recover at four times the rate.</li>
 *   <li><b>Satellites</b>, launched by the Rocket Assembly: weather forecasts, solar storm warnings, a relay (probes
 *   bring home more), and power beamed to Microwave Rectennas (four per satellite).</li>
 *   <li><b>Debris</b> (config): every launch adds one; the air drags off a fiftieth a day; strikes every minute with a chance
 *   of debris/30, hurting players outside the hub without a Whipple bumper and destroying satellites (a satellite's
 *   wreck adds three). A Debris Sweeper takes away most of it. It never breaks blocks.</li>
 *   <li><b>grav</b>, the local gravity in m/s², for machine consoles (the Microgravity Factory's Rayleigh number).</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class SpaceModules {
    private SpaceModules() {}

    public static final String RING = "habitat_ring_module", SHELTER = "storm_shelter_module";
    static final int RING_REACH = 12;
    static final String DECON = "ts_decon", DECON_TOLD = "ts_decon_told";
    static final int DAY = 24000, SLOW_AT = DAY, WEAK_AT = 3 * DAY, CAP = 10 * DAY;

    // ------------------------------------------------------------------ gravity
    static final Map<String, Double> GRAVITY = Map.of(
            "theoreticalsciences:orbital_station", 9.81e-5,          // at the centre of mass, isolated: ten micro-g
            "theoreticalsciences:titan", 1.35,
            "ad_astra:moon", 1.62, "ad_astra:mars", 3.72, "ad_astra:venus", 8.87, "ad_astra:mercury", 3.70, "ad_astra:glacio", 3.72,
            "ad_astra:earth_orbit", 9.81e-5, "ad_astra:moon_orbit", 9.81e-5, "ad_astra:mars_orbit", 9.81e-5);

    public static double gravity(Level level) {
        return GRAVITY.getOrDefault(level.dimension().location().toString(), 9.81);
    }

    /** 1.4.0: the surroundings a machine rejects its heat to (K): a refrigerator on Titan's 94 K surface works far cheaper. */
    static final Map<String, Double> AMBIENT = Map.of(
            "theoreticalsciences:titan", 94.0, "theoreticalsciences:titan_ocean", 273.0,
            "ad_astra:moon", 250.0, "ad_astra:mars", 210.0, "ad_astra:venus", 737.0, "ad_astra:mercury", 440.0, "ad_astra:glacio", 250.0);

    public static double ambient(Level level) {
        return AMBIENT.getOrDefault(level.dimension().location().toString(), 300.0);
    }

    // ------------------------------------------------------------------ the module blocks
    public static class ModuleBlock extends Block {
        public ModuleBlock(Properties p) {
            super(p);
        }

        String kind() {
            return BuiltInRegistries.BLOCK.getKey(this).getPath();
        }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
            super.onPlace(state, level, pos, old, moved);
            if (!level.isClientSide && level.getServer() != null && !old.is(this)) OrbitData.get(level.getServer()).place(kind(), level.dimension(), pos.asLong(), true);
        }

        @Override
        protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState now, boolean moved) {
            if (!level.isClientSide && level.getServer() != null && !now.is(this)) OrbitData.get(level.getServer()).place(kind(), level.dimension(), pos.asLong(), false);
            super.onRemove(state, level, pos, now, moved);
        }
    }

    public static boolean near(Player p, String kind, int reach) {
        if (p.level().getServer() == null) return false;
        double r2 = (reach + 0.5) * (reach + 0.5);
        for (long l : OrbitData.get(p.level().getServer()).placed(kind, p.level().dimension()))
            if (BlockPos.of(l).getCenter().distanceToSqr(p.position()) <= r2) return true;
        return false;
    }

    public static boolean nearRing(Player p) {
        return near(p, RING, RING_REACH);
    }

    // ------------------------------------------------------------------ the power beam
    /** Rectennas fed lately, by dimension and position, and when. */
    static final Map<String, Long> BEAMS = com.theoreticalsciences.world.SessionMaps.perServer(new HashMap<>());   // 1.3.0: cleared when the server stops

    /** Whether this rectenna may have a beam: each Power Satellite in orbit feeds four. */
    public static boolean claimBeam(ServerLevel level, BlockPos pos) {
        int n = OrbitData.get(level.getServer()).satellites("power_satellite") * 4;
        long now = level.getServer().overworld().getGameTime();
        BEAMS.values().removeIf(t -> now - t > 1200);
        String k = level.dimension().location() + "@" + pos.asLong();
        if (!BEAMS.containsKey(k) && BEAMS.size() >= n) return false;
        BEAMS.put(k, now);
        return true;
    }

    // ------------------------------------------------------------------ launches
    static final String LAUNCH = "orbital_launch/r1130_launch_";
    /** 1.4.0: the same launches on rocket kerosene (orbital_launch/r1400_kerolox_launch_<kind>). */
    static final String KEROLOX = "orbital_launch/r1400_kerolox_launch_";

    @SubscribeEvent
    public static void onLaunch(MachineRecipeEvent.Finish e) {
        if (e.getMachine().type != MachineType.ROCKET_ASSEMBLY || e.getMachine().getLevel() == null || e.getMachine().getLevel().getServer() == null) return;
        MinecraftServer server = e.getMachine().getLevel().getServer();
        OrbitData data = OrbitData.get(server);
        if (QHConfig.SPACE_DEBRIS.get()) data.debris(data.debris() + 1);        // a spent upper stage, bolts, shrouds
        String path = e.getRecipeId().getPath();
        if (!e.getRecipeId().getNamespace().equals(TheoreticalSciences.MODID)) return;
        String kind;
        if (path.startsWith(LAUNCH)) kind = path.substring(LAUNCH.length());
        else if (path.startsWith(KEROLOX)) kind = path.substring(KEROLOX.length());
        else return;
        ServerPlayer owner = e.getMachine().getOwner() == null ? null : server.getPlayerList().getPlayer(e.getMachine().getOwner());
        if (kind.equals("debris_sweeper")) {
            data.debris(data.debris() * 0.15);
            if (owner != null) owner.displayClientMessage(Component.translatable("message.theoreticalsciences.sweeper_done").withStyle(ChatFormatting.AQUA), false);
            return;
        }
        int n = data.addSatellite(kind, 1);
        if (owner != null) owner.displayClientMessage(Component.translatable("message.theoreticalsciences.satellite_launched",
                Component.translatable("satellite.theoreticalsciences." + kind), n).withStyle(ChatFormatting.AQUA), false);
    }

    // ------------------------------------------------------------------ the world's clock: debris, forecasts
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel ow = server.overworld();
        long now = ow.getGameTime();
        if (now % 20 != 0) return;
        OrbitData data = OrbitData.get(server);
        // the Solar Monitor at L1 sees the storm leave the Sun
        if (data.satellites("solar_monitor_satellite") > 0) {
            long next = com.theoreticalsciences.helio.SpaceWeather.nextStorm(server);
            if (next - now > 3580 && next - now <= 3600) broadcast(server, Component.translatable("message.theoreticalsciences.storm_warning").withStyle(ChatFormatting.GOLD));
        }
        // the Weather Satellite watches the clouds
        if (data.satellites("weather_satellite") > 0 && ow.getLevelData() instanceof ServerLevelData w && w.getClearWeatherTime() <= 0) {
            if (w.getRainTime() > 5980 && w.getRainTime() <= 6000)
                tell(ow, Component.translatable(w.isRaining() ? "message.theoreticalsciences.forecast_clear" : "message.theoreticalsciences.forecast_rain"));
            if (w.isRaining() && !w.isThundering() && w.getThunderTime() > 5980 && w.getThunderTime() <= 6000)
                tell(ow, Component.translatable("message.theoreticalsciences.forecast_thunder"));
        }
        if (!QHConfig.SPACE_DEBRIS.get() || now % 1200 != 0 || data.debris() <= 0) return;
        double before = data.debris();
        data.debris(before * Math.pow(0.98, 1 / 20.0));                      // a fiftieth a day, dragged into the air
        if (ow.random.nextDouble() >= Math.min(0.6, data.debris() / 30)) return;
        // a debris pass
        ServerLevel station = server.getLevel(QHDimensions.ORBITAL_STATION);
        if (station != null)
            for (ServerPlayer p : station.players()) {
                if (p.isCreative() || p.isSpectator() || OrbitBuilder.inside(p.blockPosition())) continue;
                if (InteriorGear.pieces(p, InteriorGear.Suit.WHIPPLE) > ow.random.nextInt(4)) continue;      // each bumper piece stops a quarter
                p.hurt(p.damageSources().magic(), 4F);
                station.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.3, 0.3, 0.3, 0.6);
                station.playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.MICROMETEORITE.get(), SoundSource.AMBIENT, 1F, 1F);
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.debris_strike").withStyle(ChatFormatting.GRAY), true);
            }
        for (String kind : new java.util.ArrayList<>(data.satellites.keySet())) {
            int n = data.satellites(kind);
            int lost = 0;
            for (int i = 0; i < n; i++) if (ow.random.nextDouble() < data.debris() / 600) lost++;
            if (lost == 0) continue;
            data.addSatellite(kind, -lost);
            data.debris(data.debris() + 3 * lost);                             // Kessler: the wreck is more debris
            broadcast(server, Component.translatable("message.theoreticalsciences.satellite_lost", Component.translatable("satellite.theoreticalsciences." + kind))
                    .withStyle(ChatFormatting.RED));
        }
        if (before < 10 && data.debris() >= 10 && station != null)
            for (ServerPlayer p : station.players())
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.debris_warning").withStyle(ChatFormatting.YELLOW), false);
    }

    static void broadcast(MinecraftServer server, Component c) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) p.displayClientMessage(c, false);
    }

    static void tell(ServerLevel level, Component c) {
        for (ServerPlayer p : level.players()) p.displayClientMessage(Component.empty().append(c).withStyle(ChatFormatting.AQUA), false);
    }

    /** What is in orbit (a sneak-use of the Transfer Console). */
    public static void status(ServerPlayer sp) {
        OrbitData data = OrbitData.get(sp.server);
        MutableComponent sats = Component.empty();
        boolean any = false;
        for (var en : data.satellites.entrySet()) {
            if (any) sats.append(", ");
            sats.append(en.getValue() + " ").append(Component.translatable("satellite.theoreticalsciences." + en.getKey()));
            any = true;
        }
        if (!any) sats = Component.translatable("message.theoreticalsciences.orbit_status_none");
        Component deb;
        if (!QHConfig.SPACE_DEBRIS.get()) deb = Component.translatable("message.theoreticalsciences.debris_off");
        else {
            double d = data.debris();
            String how = d < 3 ? "rare" : d < 12 ? "some" : "often";
            deb = Component.translatable("message.theoreticalsciences.debris_level", String.format(Locale.ROOT, "%.1f", d),
                    Component.translatable("message.theoreticalsciences.debris_" + how));
        }
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.orbit_status", sats, deb).withStyle(ChatFormatting.AQUA), false);
    }

    /** 1.15.0: the Core's recognition of the signal stays with the player through death. */
    @SubscribeEvent
    public static void onClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e) {
        String k = com.theoreticalsciences.transcend.OmnipotenceCoreBlock.HEARD_HOME;
        if (e.getOriginal().getPersistentData().getBoolean(k)) e.getEntity().getPersistentData().putBoolean(k, true);
    }

    // ------------------------------------------------------------------ deconditioning
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % 20 != 0 || !QHConfig.DECONDITIONING.get()) return;
        var tag = p.getPersistentData();
        int v = tag.getInt(DECON);
        boolean station = p.level().dimension() == QHDimensions.ORBITAL_STATION;
        if (station && !p.isCreative() && !p.isSpectator()) v = nearRing(p) ? Math.max(0, v - 20) : Math.min(CAP, v + 20);
        else if (!station) v = Math.max(0, v - 80);
        if (v == 0 && !tag.contains(DECON)) return;
        tag.putInt(DECON, v);
        if (station) return;
        if (v >= SLOW_AT) {
            if (!tag.getBoolean(DECON_TOLD)) {
                tag.putBoolean(DECON_TOLD, true);
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.deconditioned").withStyle(ChatFormatting.YELLOW), false);
            }
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, false, true));
            if (v >= WEAK_AT) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, false, true));
        } else if (v == 0) {
            if (tag.getBoolean(DECON_TOLD))
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.reconditioned").withStyle(ChatFormatting.GREEN), false);
            tag.remove(DECON);
            tag.remove(DECON_TOLD);
        }
    }
}
