package com.theoreticalsciences.interior;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Round 40 (1.2.0): what the Planetary Interior does to you. Checked once a second per player, only for players inside
 * it; nothing here changes or moves a block.
 * <ul>
 *   <li><b>Lithostatic Pressure</b> everywhere below y 100, stronger with depth (P = ρgh).</li>
 *   <li><b>Mantle plumes</b> in the Mantle Convection Zone: now and then hot rock rises under you and throws you up.</li>
 *   <li><b>Geomagnetic storms</b> in the Geodynamo Core: Magnetized for a while (compasses spin).</li>
 *   <li><b>Tremors</b> in the Crustal Caverns: a short Seismic Vertigo.</li>
 *   <li>The Natural Reactor's radiation is its background dose (RadiationService).</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class InteriorHazards {
    public static ResourceKey<Biome> biome(String id) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, TheoreticalSciences.id(id));
    }

    public static final ResourceKey<Biome> CRUST = biome("crustal_caverns"), REACTOR = biome("natural_reactor"), MAGMA = biome("magma_chambers"),
            MANTLE = biome("mantle_convection_zone"), CORE = biome("geodynamo_core");

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || p.isCreative() || (p.tickCount + p.getId()) % 20 != 3) return;
        ServerLevel level = p.serverLevel();
        if (level.dimension() != QHDimensions.INTERIOR) return;
        BlockPos pos = p.blockPosition();
        // pressure: one level for every 25 blocks below y 100
        int amp = Math.min(3, (100 - pos.getY()) / 25) - InteriorGear.pressureRelief(p);     // 1.3.7: the Pressure Suit takes levels off
        if (amp >= 0) p.addEffect(new MobEffectInstance(QHEffects.LITHOSTATIC_PRESSURE, 60, amp, false, true));
        var b = level.getBiome(pos);
        var r = level.random;
        if (b.is(MANTLE) && p.onGround() && r.nextInt(12) == 0) {
            p.setDeltaMovement(p.getDeltaMovement().x, 1.1, p.getDeltaMovement().z);
            p.hurtMarked = true;
            level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY(), p.getZ(), 20, 0.6, 0.2, 0.6, 0.1);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(), p.getZ(), 15, 0.6, 0.4, 0.6, 0.05);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.AMBIENT, 1F, 0.5F);
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.mantle_plume").withStyle(ChatFormatting.GOLD), true);
        } else if (b.is(CORE) && r.nextInt(90) == 0 && !p.hasEffect(QHEffects.MAGNETIZED)) {
            p.addEffect(new MobEffectInstance(QHEffects.MAGNETIZED, 400, 1, false, true));
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.geomagnetic_storm").withStyle(ChatFormatting.AQUA), true);
        } else if (b.is(CRUST) && r.nextInt(150) == 0) {
            p.addEffect(new MobEffectInstance(QHEffects.SEISMIC_VERTIGO, 100, 0, false, true));
            level.playSound(null, pos, SoundEvents.DEEPSLATE_BREAK, SoundSource.AMBIENT, 1F, 0.4F);
        }
    }

    private InteriorHazards() {}
}
