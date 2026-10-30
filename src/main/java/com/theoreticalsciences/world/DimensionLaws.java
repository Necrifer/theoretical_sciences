package com.theoreticalsciences.world;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every dimension of the mod has a law that is hostile to a visitor from ordinary space.
 * <ul>
 *   <li>Carry the dimension's own <b>attunement item</b> (anywhere in your inventory) and you live with a residual
 *       "gimmick" of the place.</li>
 *   <li>Without it the law turns on you: a debuff that follows the same gimmick and makes staying long very hard
 *       (inside the black and white holes: death within two seconds).</li>
 * </ul>
 * <pre>
 *  dimension                attunement item        without it                        with it (still)
 *  the Bulk                 Brane Tether           Dimensional Bleed, gravity x0.25  gravity x0.5, knockback x2
 *  Hyperdimensional Reality Primordial Charter     Law Flux: a new bad law every 8s  a new law (good or bad) every 90s
 *  the Platonic Realm       Form of the Self       Imperfection                      only swords can dig; other tools barely scratch
 *  the Quantum Foam         Coherence Stabiliser   constant Decoherence              you blink a few blocks every 20-40s
 *  the Transfinite Hier.    Well-Ordering Charm    Infinite Regress stacking forever permanent Regress I and hunger
 *  black hole interior      Geodesic Anchor        death in 2s                       gravity x2.5, Time Dilation II, singularity
 *  white hole interior      Geodesic Anchor        death in 2s                       gravity inverted (sneak to fall), Time Dilation
 *  the Transcendental       Identity Kernel        Dissolution (max health melts)    Weakness and Mining Fatigue
 * </pre>
 * Also handles Vacuum Decay (no healing) and Infinite Regress (more damage taken).
 */
public final class DimensionLaws {
    private static final ResourceLocation GRAVITY = TheoreticalSciences.id("warped_gravity");
    private static final ResourceLocation LAW_GRAVITY = TheoreticalSciences.id("law_gravity");
    private static final Map<UUID, Integer> UNANCHORED = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());
    private static final Map<UUID, Integer> EXPOSURE = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());
    private static final Map<UUID, long[]> LAW = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());   // {law index, ends at tick}
    private static final Map<UUID, Integer> NEXT_BLINK = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());
    private static final java.util.Set<UUID> SHIELDED = com.theoreticalsciences.world.SessionMaps.perServer(ConcurrentHashMap.newKeySet());

    public static void forget(UUID id) {
        SHIELDED.remove(id);
        NEXT_BLINK.remove(id);
    }

    /** When a stronger attunement takes over, everything this place had done to you is lifted at once. */
    private static void lift(ServerPlayer sp) {
        for (Holder<MobEffect> e : List.of(QHEffects.DIMENSIONAL_BLEED, QHEffects.IMPERFECTION, QHEffects.DECOHERENCE, QHEffects.INFINITE_REGRESS,
                QHEffects.DISSOLUTION, QHEffects.TIME_DILATION, QHEffects.SPAGHETTIFICATION, QHEffects.LAW_FLUX))
            sp.removeEffect(e);
        // vanilla effects only when this place gave them (ours are always ambient), never a potion the player drank
        for (Holder<MobEffect> e : List.of(MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DARKNESS)) {
            MobEffectInstance i = sp.getEffect(e);
            if (i != null && i.isAmbient()) sp.removeEffect(e);
        }
        NEXT_BLINK.remove(sp.getUUID());
    }

    public record Law(ResourceKey<Level> dim, String item) {}

    public static final Law[] LAWS = {
            new Law(QHDimensions.BULK, "brane_tether"),
            new Law(QHDimensions.HYPERREAL, "primordial_charter"),
            new Law(QHDimensions.PLATONIC, "form_of_the_self"),
            new Law(QHDimensions.FOAM, "coherence_stabiliser"),
            new Law(QHDimensions.TRANSFINITE, "well_ordering_charm"),
            new Law(QHDimensions.BLACK_HOLE, "geodesic_anchor"),
            new Law(QHDimensions.WHITE_HOLE, "geodesic_anchor"),
            new Law(QHDimensions.TRANSCENDENTAL, "identity_kernel"),
            new Law(QHDimensions.ANTIVERSE, "penning_harness"),
    };

    /**
     * The attunement items in the order the player earns them. Each one also SHIELDS its carrier completely - no
     * debuff and no residual gimmick - in every dimension whose own item comes earlier in this list (the place they had
     * to survive to make it). The Primordial Charter shields from everything but the Transcendental; the Identity
     * Kernel from everything.
     */
    public static final String[] TIERS = {"form_of_the_self", "penning_harness", "coherence_stabiliser", "brane_tether",
            "geodesic_anchor", "well_ordering_charm", "primordial_charter", "identity_kernel"};

    private static int tierOf(String item) {
        for (int i = 0; i < TIERS.length; i++) if (TIERS[i].equals(item)) return i;
        return -1;
    }

    /** True if the player carries an attunement item from a later tier than this dimension's own: the place cannot touch them. */
    public static boolean shielded(Player p, ResourceKey<Level> dim) {
        if (com.theoreticalsciences.endgame.Raiment.transcends(p)) return true;   // the full Raiment of Transcendence
        Law law = lawFor(dim);
        if (law == null) return false;
        int own = tierOf(law.item);
        for (int i = TIERS.length - 1; i > own; i--) if (carries(p, TIERS[i])) return true;
        return false;
    }

    /** Carries this attunement item or any later one (later items include every earlier protection). */
    public static boolean carriesAtLeast(Player p, String item) {
        if (com.theoreticalsciences.endgame.Raiment.transcends(p)) return true;   // the full Raiment of Transcendence
        int t = tierOf(item);
        if (t < 0) return carries(p, item);
        for (int i = t; i < TIERS.length; i++) if (carries(p, TIERS[i])) return true;
        return false;
    }

    public static boolean carries(Player p, String item) {
        var it = QHItems.get(item);
        for (ItemStack s : p.getInventory().items) if (s.is(it)) return true;
        for (ItemStack s : p.getInventory().offhand) if (s.is(it)) return true;
        for (ItemStack s : p.getInventory().armor) if (s.is(it)) return true;
        return false;
    }

    /** Kept for the round-4 API. */
    public static boolean anchored(Player p) {
        return carriesAtLeast(p, "geodesic_anchor");
    }

    private static Law lawFor(ResourceKey<Level> dim) {
        for (Law l : LAWS) if (l.dim == dim) return l;
        return null;
    }

    private static void effect(ServerPlayer sp, Holder<MobEffect> e, int ticks, int amp) {
        MobEffectInstance cur = sp.getEffect(e);
        if (cur == null || cur.getAmplifier() != amp || cur.getDuration() < ticks / 2) sp.addEffect(new MobEffectInstance(e, ticks, amp, true, false, true));
    }

    private static void gravity(AttributeInstance grav, double mul) {
        if (grav == null) return;
        if (mul == 0) {
            if (grav.hasModifier(GRAVITY)) grav.removeModifier(GRAVITY);
        } else grav.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY, mul, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        var dim = sp.level().dimension();
        Law law = lawFor(dim);
        AttributeInstance grav = sp.getAttribute(Attributes.GRAVITY);
        UUID id = sp.getUUID();
        boolean shield = law != null && shielded(sp, dim);
        if (shield) {
            if (SHIELDED.add(id)) {
                lift(sp);
                // round 30: said once - when the shield takes hold (entering the dimension, or putting the gear on) -
                // not every ten seconds for as long as it lasts
                if (!sp.isCreative() && !sp.isSpectator())
                    sp.displayClientMessage(Component.translatable("message.theoreticalsciences.law_shielded").withStyle(ChatFormatting.AQUA), true);
            }
        } else SHIELDED.remove(id);
        if (law == null || sp.isCreative() || sp.isSpectator() || shield) {
            gravity(grav, 0);
            if (grav != null && grav.hasModifier(LAW_GRAVITY)) grav.removeModifier(LAW_GRAVITY);
            UNANCHORED.remove(id);
            EXPOSURE.remove(id);
            LAW.remove(id);
            return;
        }
        boolean safe = carries(sp, law.item);
        int exposed = safe ? 0 : EXPOSURE.merge(id, 1, Integer::sum);
        if (safe) EXPOSURE.remove(id);
        if (!safe && exposed == 1)
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.law_warning." + dim.location().getPath(),
                    Component.translatable("item.theoreticalsciences." + law.item)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);

        if (dim == QHDimensions.BLACK_HOLE || dim == QHDimensions.WHITE_HOLE) {
            holes(sp, dim == QHDimensions.BLACK_HOLE, safe, grav);
            return;
        }
        UNANCHORED.remove(id);
        if (dim == QHDimensions.BULK) {
            gravity(grav, safe ? -0.5 : -0.75);
            if (!safe) effect(sp, QHEffects.DIMENSIONAL_BLEED, 100, 0);
        } else if (dim == QHDimensions.HYPERREAL) {
            hyperreal(sp, safe, grav);
        } else if (dim == QHDimensions.PLATONIC) {
            gravity(grav, 0);
            if (!safe) effect(sp, QHEffects.IMPERFECTION, 100, 0);
        } else if (dim == QHDimensions.FOAM) {
            gravity(grav, 0);
            if (!safe) effect(sp, QHEffects.DECOHERENCE, 100, 1);
            else {
                int next = NEXT_BLINK.computeIfAbsent(id, k -> sp.tickCount + 400 + sp.getRandom().nextInt(400));
                if (sp.tickCount >= next) {
                    NEXT_BLINK.put(id, sp.tickCount + 400 + sp.getRandom().nextInt(400));
                    double dx = (sp.getRandom().nextDouble() - 0.5) * 6, dz = (sp.getRandom().nextDouble() - 0.5) * 6;
                    if (sp.level().noCollision(sp, sp.getBoundingBox().move(dx, 0, dz))) {
                        sp.teleportTo(sp.getX() + dx, sp.getY(), sp.getZ() + dz);
                        sp.serverLevel().sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
                    }
                }
            }
        } else if (dim == QHDimensions.ANTIVERSE) {
            gravity(grav, 0);
            antiverse(sp, safe, exposed);
        } else if (dim == QHDimensions.TRANSFINITE) {
            gravity(grav, 0);
            if (safe) {
                effect(sp, QHEffects.INFINITE_REGRESS, 100, 0);
                effect(sp, MobEffects.HUNGER, 100, 0);
            } else if (exposed % 200 == 1) {                     // every ten seconds the regress goes one level deeper
                MobEffectInstance cur = sp.getEffect(QHEffects.INFINITE_REGRESS);
                int amp = cur == null ? 0 : Math.min(9, cur.getAmplifier() + 1);
                sp.addEffect(new MobEffectInstance(QHEffects.INFINITE_REGRESS, 260, amp, true, false, true));
            }
            if (!safe && exposed % 100 == 0) sp.hurt(sp.damageSources().magic(), 1.0F);
        } else if (dim == QHDimensions.TRANSCENDENTAL) {
            gravity(grav, 0);
            if (safe) {
                effect(sp, MobEffects.WEAKNESS, 100, 0);
                effect(sp, MobEffects.DIG_SLOWDOWN, 100, 0);
            } else if (exposed % 200 == 1) {
                MobEffectInstance cur = sp.getEffect(QHEffects.DISSOLUTION);
                int amp = cur == null ? 0 : Math.min(5, cur.getAmplifier() + 1);
                sp.addEffect(new MobEffectInstance(QHEffects.DISSOLUTION, 260, amp, true, false, true));
            }
        }
    }

    private static void holes(ServerPlayer sp, boolean bh, boolean safe, AttributeInstance grav) {
        if (!safe) {
            gravity(grav, 0);
            int t = UNANCHORED.merge(sp.getUUID(), 1, Integer::sum);
            if (t >= 40) {
                UNANCHORED.remove(sp.getUUID());
                sp.hurt(sp.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
            return;
        }
        UNANCHORED.remove(sp.getUUID());
        if (bh) gravity(grav, 1.5);
        else {
            // a white hole only ever expels: gravity points up. Sneak to let yourself fall; above y=200 the hole lets go.
            gravity(grav, sp.isShiftKeyDown() || sp.getY() > 200 ? -0.5 : -1.5);
        }
        if (sp.tickCount % 40 == 0) sp.addEffect(new MobEffectInstance(QHEffects.TIME_DILATION, 80, bh ? 1 : 0, true, false, true));
        if (bh && sp.getY() < 16) {
            sp.addEffect(new MobEffectInstance(QHEffects.SPAGHETTIFICATION, 60, 3));
            // below y=4 the singularity takes you through to the white hole (VoidFall)
        }
        if (!bh && sp.tickCount % 10 == 0) {
            Vec3 out = new Vec3(sp.getX(), 0, sp.getZ());
            if (out.lengthSqr() > 1) sp.push(out.normalize().x * 0.02, 0, out.normalize().z * 0.02);
            sp.hurtMarked = true;
        }
        if (!bh) sp.fallDistance = Math.min(sp.fallDistance, 6);
    }

    // ------------------------------------------------------------------ the Antiverse: everything is antimatter
    /**
     * Without a Penning Harness the visitor's matter touches the anti-air, the anti-ground, everything: annihilation
     * burns them away (2 damage a second, rising). With one, only the sea still gets through: antiwater burns.
     */
    private static void antiverse(ServerPlayer sp, boolean safe, int exposed) {
        var sl = sp.serverLevel();
        if (!safe) {
            if (exposed % 20 == 0) {
                sp.hurt(sp.damageSources().magic(), 2.0F + Math.min(6, exposed / 200));
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), sp.getX(), sp.getY() + 1, sp.getZ(), 1, 0, 0, 0, 0);
            }
            if (exposed % 2 == 0)
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), sp.getX(), sp.getY() + 0.9, sp.getZ(), 6, 0.35, 0.8, 0.35, 0.15);
            return;
        }
        var anti = com.theoreticalsciences.registry.QHFluids.get("antiwater");
        if (anti != null && sp.isInFluidType(anti.type.get()) && sp.tickCount % 10 == 0) {
            sp.hurt(sp.damageSources().magic(), 2.0F);
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), sp.getX(), sp.getY() + 0.5, sp.getZ(), 12, 0.4, 0.4, 0.4, 0.2);
            if (sp.tickCount % 60 == 0)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.antiwater_burns").withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ------------------------------------------------------------------ Hyperdimensional Reality: the laws keep changing
    static final String[] LAW_NAMES = {"gravity_halved", "gravity_doubled", "light_slowed", "entropy_reversed", "friction_lost", "causality_loosened"};

    private static void hyperreal(ServerPlayer sp, boolean safe, AttributeInstance grav) {
        gravity(grav, 0);
        long now = sp.serverLevel().getGameTime();
        long[] cur = LAW.get(sp.getUUID());
        int period = safe ? 1800 : 160, length = safe ? 600 : 120;
        if (cur == null || now >= cur[1] + (period - length)) {
            int idx = safe ? sp.getRandom().nextInt(LAW_NAMES.length) : sp.getRandom().nextInt(3);
            cur = new long[]{idx, now + length};
            LAW.put(sp.getUUID(), cur);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.law." + LAW_NAMES[idx]).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);
        }
        boolean on = now < cur[1];
        if (!safe) effect(sp, QHEffects.LAW_FLUX, 100, 0);
        if (grav != null) {
            double g = !on ? 0 : cur[0] == 0 ? -0.5 : cur[0] == 1 ? (safe ? 1.0 : 2.0) : 0;
            if (g == 0) {
                if (grav.hasModifier(LAW_GRAVITY)) grav.removeModifier(LAW_GRAVITY);
            } else grav.addOrUpdateTransientModifier(new AttributeModifier(LAW_GRAVITY, g, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (!on) return;
        switch ((int) cur[0]) {
            case 2 -> {
                effect(sp, MobEffects.MOVEMENT_SLOWDOWN, 40, safe ? 0 : 2);
                if (!safe) effect(sp, MobEffects.DARKNESS, 60, 0);
            }
            case 3 -> effect(sp, MobEffects.REGENERATION, 60, 0);
            case 4 -> effect(sp, MobEffects.MOVEMENT_SPEED, 40, 2);
            case 5 -> effect(sp, MobEffects.DIG_SPEED, 40, 1);
            default -> {}
        }
        if (!safe && sp.tickCount % 60 == 0) {
            @SuppressWarnings("unchecked") Holder<MobEffect>[] bad = new Holder[]{MobEffects.CONFUSION, MobEffects.POISON, MobEffects.LEVITATION, MobEffects.HUNGER, MobEffects.WEAKNESS};
            sp.addEffect(new MobEffectInstance(bad[sp.getRandom().nextInt(bad.length)], 100, 0));
        }
    }

    // ------------------------------------------------------------------ the Platonic Realm: only the ideal line cuts
    private static boolean platonic(Player p) {
        return p.level().dimension() == QHDimensions.PLATONIC && !p.isCreative();
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (!platonic(p) || shielded(p, QHDimensions.PLATONIC)) return;
        ItemStack held = p.getMainHandItem();
        if (held.getItem() instanceof SwordItem sw) {
            float speed = sw.getTier().getSpeed();
            if (!p.onGround()) speed /= 5;
            e.setNewSpeed(Math.max(e.getNewSpeed(), speed));
        } else e.setNewSpeed(e.getNewSpeed() * 0.02F);
    }

    public static void onHarvestCheck(PlayerEvent.HarvestCheck e) {
        Player p = e.getEntity();
        if (!platonic(p) || shielded(p, QHDimensions.PLATONIC)) return;
        ItemStack held = p.getMainHandItem();
        if (held.getItem() instanceof SwordItem sw) e.setCanHarvest(!e.getTargetBlock().is(sw.getTier().getIncorrectBlocksForDrops()));
        else e.setCanHarvest(false);
    }

    // ------------------------------------------------------------------ shared effect rules
    public static void onHeal(LivingHealEvent e) {
        if (e.getEntity().hasEffect(QHEffects.VACUUM_DECAY)) e.setCanceled(true);
    }

    public static void onDamage(LivingIncomingDamageEvent e) {
        MobEffectInstance r = e.getEntity().getEffect(QHEffects.INFINITE_REGRESS);
        if (r != null) e.setAmount(e.getAmount() * (1.25F + 0.25F * r.getAmplifier()));
    }

    public static void onKnockback(LivingKnockBackEvent e) {
        if (e.getEntity() instanceof Player p && p.level().dimension() == QHDimensions.BULK && !p.isCreative() && !shielded(p, QHDimensions.BULK)) e.setStrength(e.getStrength() * 2);
    }

    private DimensionLaws() {}
}
