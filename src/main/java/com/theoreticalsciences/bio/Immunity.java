package com.theoreticalsciences.bio;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 1.2.0: infection and immunity. The creatures that containment failures release now leave a lasting infection when
 * they hit a player (an effect only; milk does not wash it out). An Anomaly Ward keeps that infection away: at first for three
 * in-game days, and one renewed while more than half of that is left overloads instead. After Immune Memory, a
 * lasting ward makes a player immune for good. Immunity is kept with the
 * player (copied over on respawn).
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class Immunity {
    private Immunity() {}

    public enum Infection {
        FRACTURE("fracture", () -> QHEntities.DESTABILISED_HUSK.get(), QHEffects.FRACTURE),
        GREY_GOO("grey_goo", () -> QHEntities.NANITE_SWARM.get(), QHEffects.GREY_GOO),
        REWIND("rewind", () -> QHEntities.PARADOX_ECHO.get(), QHEffects.REWIND),
        HOLLOWING("hollowing", () -> QHEntities.VOID_WRAITH.get(), QHEffects.HOLLOWING),
        /** 1.2.0, the Escape chapter: the Tumour Hydra's bite. It hides from plain wards (immune evasion): only a ward matured on the Clonal Selector holds it. */
        METASTASIS("metastasis", () -> com.theoreticalsciences.inner.InnerRegistry.TUMOUR_HYDRA.get(), QHEffects.METASTASIS),
        /** 1.2.0: the Antiverse's Mirror Microbes. Built mirror-handed, nothing in the body can grip them; only a Mirror Antibody wards it. */
        MIRROR_PLAGUE("mirror_plague", () -> com.theoreticalsciences.inner.InnerRegistry.MIRROR_MICROBE.get(), QHEffects.MIRROR_PLAGUE);

        public final String id;
        final Supplier<EntityType<?>> source;
        final Holder<MobEffect> effect;

        Infection(String id, Supplier<EntityType<?>> source, Holder<MobEffect> effect) {
            this.id = id;
            this.source = source;
            this.effect = effect;
        }

        public boolean isSource(EntityType<?> t) {
            return t == source.get();
        }

        public static Infection of(String id) {
            for (Infection i : values()) if (i.id.equals(id)) return i;
            return null;
        }
    }

    /** How long an infection lasts (10 minutes) and a first Anomaly Ward (3 in-game days). */
    public static final int INFECTION_TICKS = 12000, VACCINE_TICKS = 3 * 24000;
    static final String TAG = "ts_immunity";

    static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(TAG)) root.put(TAG, new CompoundTag());
        return root.getCompound(TAG);
    }

    /** -1: immune for good; otherwise the game time immunity runs out (0: none). */
    public static long until(ServerPlayer p, Infection i) {
        return data(p).getLong(i.id);
    }

    public static boolean immune(ServerPlayer p, Infection i) {
        long u = until(p, i);
        return u == -1 || u > p.level().getGameTime();
    }

    /** A ward used. Returns false when it was renewed too soon (an overload, and no extra immunity). */
    public static boolean vaccinate(ServerPlayer p, Infection i, boolean lasting) {
        long now = p.level().getGameTime(), u = until(p, i);
        if (u == -1) {
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.immune_already", name(i)).withStyle(ChatFormatting.AQUA), true);
            return true;
        }
        if (!lasting && u - now > VACCINE_TICKS / 2) {
            p.addEffect(new MobEffectInstance(QHEffects.WARD_OVERLOAD, 600, 0));
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.ward_overload").withStyle(ChatFormatting.RED), false);
            return false;
        }
        data(p).putLong(i.id, lasting ? -1 : now + VACCINE_TICKS);
        p.displayClientMessage(Component.translatable(lasting ? "message.theoreticalsciences.immune_lasting" : "message.theoreticalsciences.immune_days",
                name(i)).withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    static Component name(Infection i) {
        return Component.translatable("effect.theoreticalsciences." + i.id);
    }

    /** Carrying any infection now. */
    public static boolean infected(net.minecraft.world.entity.player.Player p) {
        for (Infection i : Infection.values()) if (p.hasEffect(i.effect)) return true;
        return false;
    }

    /** Infected by a creature's bite, unless immune. */
    public static void infect(ServerPlayer p, Infection i) {
        if (p.isCreative() || immune(p, i)) return;
        p.addEffect(new MobEffectInstance(i.effect, INFECTION_TICKS, 0));
    }

    /**
     * 1.2.0, Epidemic Dynamics: an infection passes between players. Each second, an infected player near one who is not
     * infected (within 4 blocks) passes it on with chance BETA, and each case runs its ten minutes: S, I and R, as in the
     * SIR model (R0 = BETA x contacts x duration). A ward stops it, as herd immunity does.
     */
    public static final double BETA = 0.02;

    static void spread(ServerPlayer p) {
        for (Infection i : Infection.values()) {
            MobEffectInstance m = p.getEffect(i.effect);
            if (m == null) continue;
            for (ServerPlayer o : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(4))) {
                if (o == p || o.isCreative() || o.isSpectator() || o.hasEffect(i.effect) || immune(o, i)) continue;
                if (p.getRandom().nextDouble() < BETA) {
                    o.addEffect(new MobEffectInstance(i.effect, INFECTION_TICKS, 0));
                    o.displayClientMessage(Component.translatable("message.theoreticalsciences.caught_infection", name(i), p.getDisplayName())
                            .withStyle(ChatFormatting.RED), false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative()) return;
        Entity by = e.getSource().getEntity();
        if (by == null) return;
        for (Infection i : Infection.values()) {
            if (by.getType() != i.source.get()) continue;
            if (immune(p, i)) return;
            p.addEffect(new MobEffectInstance(i.effect, INFECTION_TICKS, 0));
            return;
        }
    }

    // ------------------------------------------------------------------ Rewind: pulled back to where you stood ten seconds ago
    static final Map<UUID, ArrayDeque<Vec3>> TRAIL = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (!p.isSpectator()) spread(p);
        if (!p.hasEffect(QHEffects.REWIND)) {
            TRAIL.remove(p.getUUID());
            return;
        }
        ArrayDeque<Vec3> t = TRAIL.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        t.addLast(p.position());
        while (t.size() > 11) t.removeFirst();
        if (p.tickCount % 300 == 0 && t.size() >= 11) {
            Vec3 back = t.peekFirst();
            p.teleportTo(back.x, back.y, back.z);
            p.fallDistance = 0;
            t.clear();
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData(), now = e.getEntity().getPersistentData();
        if (old.contains(TAG)) now.put(TAG, old.getCompound(TAG).copy());
        // 1.2.0: the grants of flight go with the player (so taking off the Raiment or putting down the Omnilex still
        // ends the flight afterwards; the next check gives it back if it is still due)
        for (String k : new String[]{"theoreticalsciences_raiment_flight", "theoreticalsciences_omnilex_flight"})
            if (old.contains(k)) now.putBoolean(k, old.getBoolean(k));
        // leaving the End alive is no cure: the dose and what it has started stay (a death clears them)
        if (!e.isWasDeath())
            for (String k : new String[]{"theoreticalsciences_rad_dose", "ts_oncogenic", "ts_tumour_told"})
                if (old.contains(k)) now.put(k, old.get(k).copy());
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent e) {
        TRAIL.remove(e.getEntity().getUUID());
    }
}
