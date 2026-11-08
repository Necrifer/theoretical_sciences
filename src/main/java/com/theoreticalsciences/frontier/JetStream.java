package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.3.0: the Jet Stream - the air above the Accretion Frontier's islands, from Y {@value #FLOOR} up. It is not a biome
 * (the terrain below is generated exactly as before): it is a layer with laws of its own.
 * <ul>
 *   <li>A steady wind from the west pushes whoever is off the ground there towards the east, harder the higher you go.
 *   Elytra flight with the wind is fast; against it, slow.</li>
 *   <li>Kármán Wraiths ride it (topped up near players: at most {@value #CAP} within 48 blocks, every ten seconds).</li>
 *   <li>The Bussard Ramscoop only works up here, where the nebula streams past fastest.</li>
 * </ul>
 * Per player: one dimension and height check a tick; the top-up counts nearby wraiths once every 200 ticks.
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class JetStream {
    public static final int FLOOR = 200;
    public static final int CAP = 3;
    /** The wind's direction (west to east, a little south) and strength in blocks a tick at the floor of the stream. */
    public static final Vec3 WIND = new Vec3(1, 0, 0.25).normalize();

    public static boolean inStream(ServerPlayer p) {
        return p.level().dimension() == QHDimensions.FRONTIER && p.getY() >= FLOOR;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || !inStream(p)) return;
        if (!p.onGround() && !p.getAbilities().flying) {
            double k = 0.012 + 0.00015 * Math.min(55, p.getY() - FLOOR);       // stronger higher up
            p.setDeltaMovement(p.getDeltaMovement().add(WIND.scale(k)));
            p.hurtMarked = true;
        }
        if ((p.tickCount + p.getId()) % 200 == 0) {
            if (p.tickCount % 2400 < 200)
                p.displayClientMessage(Component.translatable("message.theoreticalsciences.jet_stream").withStyle(ChatFormatting.AQUA), true);
            populate(p, p.serverLevel());
        }
    }

    static void populate(ServerPlayer p, ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        AABB box = p.getBoundingBox().inflate(48);
        if (level.getEntitiesOfClass(KarmanWraith.class, box).size() >= CAP) return;
        var r = p.getRandom();
        double a = r.nextDouble() * Math.PI * 2, d = 20 + r.nextDouble() * 20;
        BlockPos at = BlockPos.containing(p.getX() + Math.cos(a) * d, Math.max(FLOOR + 6, p.getY() + r.nextInt(13) - 6), p.getZ() + Math.sin(a) * d);
        if (!level.isLoaded(at) || !level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) return;
        KarmanWraith w = FrontierRegistry.KARMAN_WRAITH.get().create(level);
        if (w == null) return;
        w.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360, 0);
        w.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null);
        level.addFreshEntity(w);
    }

    private JetStream() {}
}
