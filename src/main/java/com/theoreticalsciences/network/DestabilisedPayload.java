package com.theoreticalsciences.network;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server -> client: a creature started / stopped being reality-destabilised. Vanilla only tells a player about
 * their OWN potion effects, so without this other players could not see a mob glitching.
 */
public record DestabilisedPayload(int entityId, boolean on) implements CustomPacketPayload {
    public static final Type<DestabilisedPayload> TYPE = new Type<>(TheoreticalSciences.id("destabilised"));
    public static final StreamCodec<ByteBuf, DestabilisedPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DestabilisedPayload::entityId,
            ByteBufCodecs.BOOL, DestabilisedPayload::on,
            DestabilisedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean ours(Holder<MobEffect> e) {
        return e != null && e.is(QHEffects.REALITY_DESTABILISATION.getKey());
    }

    private static void send(LivingEntity e, boolean on) {
        if (!e.level().isClientSide) PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, new DestabilisedPayload(e.getId(), on));
    }

    public static void registerServerEvents() {
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Added e) -> {
            if (ours(e.getEffectInstance().getEffect()) && e.getOldEffectInstance() == null) send(e.getEntity(), true);
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Remove e) -> {
            if (ours(e.getEffect())) send(e.getEntity(), false);
        });
        NeoForge.EVENT_BUS.addListener((MobEffectEvent.Expired e) -> {
            if (e.getEffectInstance() != null && ours(e.getEffectInstance().getEffect())) send(e.getEntity(), false);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.StartTracking e) -> {
            if (e.getTarget() instanceof LivingEntity le && le.hasEffect(QHEffects.REALITY_DESTABILISATION) && e.getEntity() instanceof ServerPlayer sp)
                PacketDistributor.sendToPlayer(sp, new DestabilisedPayload(le.getId(), true));
        });
    }
}
