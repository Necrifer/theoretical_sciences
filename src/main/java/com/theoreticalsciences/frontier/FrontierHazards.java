package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.science.RadiationService;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.10.0: what the Accretion Frontier does to you, checked once a second per player and only for players inside it.
 * Nothing here changes or moves a block.
 * <ul>
 *   <li><b>Molecular Cloud Nebula</b> - dust: now and then you cannot see.</li>
 *   <li><b>Asteroid Belt</b> - low gravity (you fall slowly, jump high); micrometeorites strike unless you wear a
 *   Whipple Shield (each piece stops a quarter of them; a full set, all).</li>
 *   <li><b>Pulsar Field</b> - a radiation pulse on a fixed beat (every 5 s); high gravity (slower) and Time Dilation.</li>
 *   <li><b>Accretion Rim</b> - heat (you catch fire) and the disk's tidal pull (Tidal Strain).</li>
 *   <li><b>Lensing Void</b> - frame-dragging currents push you sideways (Frame Drag); sight lines warp.</li>
 *   <li>1.10.1: the Frontier Suit answers them piece by piece - visor (sight), shroud (heat), brace (tidal pull),
 *   compensators (gravity).</li>
 * </ul>
 * Also: the debuff counters carried in the off hand, and the Black Swan.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class FrontierHazards {
    public static ResourceKey<Biome> biome(String id) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, TheoreticalSciences.id(id));
    }

    public static final ResourceKey<Biome> NEBULA = biome("molecular_cloud_nebula"), BELT = biome("asteroid_belt"), PULSAR = biome("pulsar_field"),
            RIM = biome("accretion_rim"), VOID = biome("lensing_void");

    static boolean offhand(Player p, String id) {
        var key = BuiltInRegistries.ITEM.getKey(p.getOffhandItem().getItem());
        return key.getNamespace().equals(TheoreticalSciences.MODID) && key.getPath().equals(id);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        // the pulsar's beat is on the world clock, every 5 seconds exactly
        if (level.dimension() == QHDimensions.FRONTIER && !p.isCreative() && level.getGameTime() % 100 == 0
                && level.getBiome(p.blockPosition()).is(PULSAR) && level.canSeeSky(p.blockPosition().above())) {
            RadiationService.setDose(p, RadiationService.dose(p) + 5 * InteriorGear.radiationFactor(p));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 1, 0.5, 0.3);
            level.playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.PULSAR_BEAT.get(), SoundSource.AMBIENT, 1F, 0.7F);
        }
        if ((p.tickCount + p.getId()) % 20 != 7) return;
        counters(p);
        blackSwan(p, level);
        if (p.isCreative() || level.dimension() != QHDimensions.FRONTIER) return;
        BlockPos pos = p.blockPosition();
        var b = level.getBiome(pos);
        var r = level.random;
        var F = InteriorGear.Suit.FRONTIER;
        boolean visor = InteriorGear.wears(p, F, EquipmentSlot.HEAD), shroud = InteriorGear.wears(p, F, EquipmentSlot.CHEST),
                brace = InteriorGear.wears(p, F, EquipmentSlot.LEGS), compensators = InteriorGear.wears(p, F, EquipmentSlot.FEET);
        // the Adaptive Optics Visor unbends the light reaching you: no dust blindness, no warped sight, no Nebuloid blinding
        if (visor) {
            p.removeEffect(MobEffects.BLINDNESS);
            p.removeEffect(MobEffects.CONFUSION);
        }
        if (b.is(NEBULA)) {
            if (!visor && r.nextInt(25) == 0) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, true));
        } else if (b.is(BELT)) {
            if (!compensators) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
            }
            int shield = InteriorGear.pieces(p, InteriorGear.Suit.WHIPPLE);
            if (r.nextInt(5) == 0 && r.nextInt(4) >= shield) {
                p.hurt(level.damageSources().magic(), 3F);
                level.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.3, 0.3, 0.3, 0.6);
                level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.MICROMETEORITE.get(), SoundSource.AMBIENT, 1F, 1F);
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.micrometeorite").withStyle(ChatFormatting.GRAY), true);
            }
        } else if (b.is(PULSAR)) {
            if (!compensators) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            if (!p.hasEffect(QHEffects.TIME_DILATION)) p.addEffect(new MobEffectInstance(QHEffects.TIME_DILATION, 200, 0, false, true));
        } else if (b.is(RIM)) {
            if (shroud) p.clearFire();
            else if (r.nextInt(6) == 0 && !p.hasEffect(MobEffects.FIRE_RESISTANCE) && InteriorGear.pieces(p, InteriorGear.Suit.REFRACTORY) < 4)
                p.igniteForSeconds(2);
            if (brace) p.removeEffect(QHEffects.SPAGHETTIFICATION);
            else if (r.nextInt(15) == 0) p.addEffect(new MobEffectInstance(QHEffects.SPAGHETTIFICATION, 100, 0, false, true));
        } else if (b.is(VOID)) {
            if (!p.hasEffect(QHEffects.FRAME_DRAG)) p.addEffect(new MobEffectInstance(QHEffects.FRAME_DRAG, 200, 0, false, true));
            if (!visor && r.nextInt(40) == 0) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));
        }
    }

    /** The counters to the new debuffs, each carried in the off hand (as the Atomic Clock is for Time Dilation). */
    static void counters(ServerPlayer p) {
        if (offhand(p, "precision_gyroscope")) {
            p.removeEffect(QHEffects.FRAME_DRAG);
            p.removeEffect(QHEffects.SPAGHETTIFICATION);
        }
        if (offhand(p, "mu_metal_shield")) p.removeEffect(QHEffects.MAGNETIZED);
        if (offhand(p, "faraday_cage_mesh")) p.removeEffect(QHEffects.DECOHERENCE);
        if (offhand(p, "page_curve_record")) p.removeEffect(QHEffects.INFORMATION_LOSS);
        if (offhand(p, "confidence_interval_gauge")) p.removeEffect(QHEffects.VARIANCE);
    }

    /**
     * The Black Swan is improbable by design: it comes only after nothing improbable has happened to a player for a long
     * time - three days of quiet in the Overworld - and then only by chance. Its coming resets the clock.
     */
    static void blackSwan(ServerPlayer p, ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        var tag = p.getPersistentData();
        long quiet = tag.getLong("ts_quiet") + 20;
        tag.putLong("ts_quiet", quiet);
        if (quiet < 72000 || level.random.nextInt(1200) != 0) return;
        double a = level.random.nextDouble() * Math.PI * 2;
        int x = (int) (p.getX() + Math.cos(a) * 24), z = (int) (p.getZ() + Math.sin(a) * 24);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        var swan = FrontierRegistry.BLACK_SWAN.get().create(level);
        if (swan == null) return;
        swan.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360, 0);
        level.addFreshEntity(swan);
        tag.putLong("ts_quiet", 0);
    }

    private FrontierHazards() {}
}
