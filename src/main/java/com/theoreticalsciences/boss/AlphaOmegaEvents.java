package com.theoreticalsciences.boss;

import com.theoreticalsciences.registry.QHItems;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

/**
 * The rules around Alpha Omega:
 * <ul>
 *   <li>an Omnilex dropped (thrown) into the Quantum Foam lies there for three seconds, trembling - then the genesis begins;</li>
 *   <li>the Living Proof cannot be broken or built in (except in creative);</li>
 *   <li>dying inside it costs nothing but the attempt: your inventory comes back with you, and so (once you are out) does the Omnilex;</li>
 *   <li>entering a black or white hole makes sure its Einstein-Rosen bridge exists.</li>
 * </ul>
 */
public final class AlphaOmegaEvents {
    private static final Map<ItemEntity, Integer> THROWN = new WeakHashMap<>();
    /**
     * Where a player's kept inventory waits between dying in the Living Proof and respawning.
     * It is written into the player's own saved data (not a plain in-memory list), so a crash or
     * restart does not lose it and it can never leak into another world.
     */
    private static final String STASH_KEY = "theoreticalsciences_proof_stash";

    public static void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener((EntityJoinLevelEvent e) -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof ItemEntity ie)) return;
            if (e.getLevel().dimension() == QHDimensions.FOAM && ie.getItem().is(QHItems.get("omnilex"))) THROWN.put(ie, 0);
        });
        bus.addListener((LevelTickEvent.Post e) -> {
            if (!(e.getLevel() instanceof ServerLevel l)) return;
            if (l.dimension() == QHDimensions.FOAM && !THROWN.isEmpty()) tickThrown(l);
            if (l.dimension() == QHDimensions.LIVING_PROOF) ProofArena.tick(l);
        });
        bus.addListener((BlockEvent.BreakEvent e) -> {
            if (e.getLevel() instanceof ServerLevel l && l.dimension() == QHDimensions.LIVING_PROOF && !e.getPlayer().isCreative()) e.setCanceled(true);
        });
        bus.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getLevel() instanceof ServerLevel l && l.dimension() == QHDimensions.LIVING_PROOF
                    && !(e.getEntity() instanceof Player p && p.isCreative())) e.setCanceled(true);
        });
        // lowest priority: if another mod cancels the death, the inventory must not have been taken
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, (LivingDeathEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || p.level().dimension() != QHDimensions.LIVING_PROOF) return;
            // keep everything: the proof only takes the attempt
            // the inventory is written into the player's saved data, then emptied so nothing drops.
            // (Inventory.save uses the world's registries, so enchantments etc. survive.)
            var inv = p.getInventory();
            net.minecraft.nbt.ListTag saved = inv.save(new net.minecraft.nbt.ListTag());
            if (!saved.isEmpty()) {
                var old = p.getPersistentData().getList(STASH_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND);
                // a stash that was somehow never restored is kept too (added to, not replaced)
                if (!old.isEmpty()) {
                    var merged = new net.minecraft.nbt.ListTag();
                    merged.addAll(old);
                    merged.addAll(saved);
                    saved = merged;
                }
                p.getPersistentData().put(STASH_KEY, saved);
            }
            inv.clearContent();
            ProofArena a = ProofArena.get(p.server);
            ProofArena.Session s = a.of(p.getUUID());
            if (s != null && a.active(p.getUUID())) ProofArena.fail(p.server, s, "died");
        });
        bus.addListener((PlayerEvent.Clone e) -> {
            if (!e.isWasDeath()) return;
            // the stash lives on the old (dead) player's saved data; move it into the new body
            var oldData = e.getOriginal().getPersistentData();
            if (!oldData.contains(STASH_KEY)) return;
            var stash = oldData.getList(STASH_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND);
            oldData.remove(STASH_KEY);
            e.getEntity().getPersistentData().remove(STASH_KEY);
            restoreStash(e.getEntity(), stash);
        });
        // forget the thrown-Omnilex watch list when a world/server closes (single player can switch worlds)
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent e) -> THROWN.clear());
        bus.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && sp.tickCount % 40 == 0) ProofArena.returnLoans(sp);
            // round 29: a second after logging in, and every 5 seconds after, make sure nobody is stuck in a proof
            // that has ended or been abandoned (checked from the tick rather than inside the login itself, so the
            // player is fully in the world before being moved)
            if (e.getEntity() instanceof ServerPlayer sp && (sp.tickCount == 20 || sp.tickCount % 100 == 0)) ProofArena.rescueStranded(sp);
        });
        bus.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer sp)) return;
            if (e.getTo() == QHDimensions.BLACK_HOLE || e.getTo() == QHDimensions.WHITE_HOLE) {
                ServerLevel l = sp.server.getLevel(e.getTo());
                if (l != null) com.theoreticalsciences.gr.BridgeBuilder.ensureBuilt(l);
            }
        });
    }

    /** Puts stashed items back. Items whose slot is already taken go to a free slot, or drop at the player's feet. */
    private static void restoreStash(Player p, net.minecraft.nbt.ListTag stash) {
        var inv = p.getInventory();
        for (int i = 0; i < stash.size(); i++) {
            var tag = stash.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            ItemStack st = ItemStack.parse(p.registryAccess(), tag).orElse(ItemStack.EMPTY);
            if (st.isEmpty()) continue;
            // the same slot numbering Inventory.save uses: 0-35 main, 100+ armour, 150 offhand
            int idx = slot < inv.items.size() ? slot
                    : slot >= 100 && slot < 100 + inv.armor.size() ? inv.items.size() + slot - 100
                    : slot >= 150 && slot < 150 + inv.offhand.size() ? inv.items.size() + inv.armor.size() + slot - 150 : -1;
            if (idx >= 0 && inv.getItem(idx).isEmpty()) inv.setItem(idx, st);
            else if (!inv.add(st)) p.drop(st, false);
        }
    }

    private static void tickThrown(ServerLevel l) {
        Iterator<Map.Entry<ItemEntity, Integer>> it = THROWN.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ItemEntity ie = en.getKey();
            if (ie.isRemoved() || ie.level() != l || !ie.getItem().is(QHItems.get("omnilex"))) {
                it.remove();
                continue;
            }
            int t = en.getValue() + 1;
            en.setValue(t);
            ie.setNeverPickUp();
            ie.setNoGravity(true);
            ie.setDeltaMovement(ie.getDeltaMovement().scale(0.8).add(0, 0.01, 0));
            l.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), ie.getX(), ie.getY() + 0.3, ie.getZ(), 3, 0.2 + t * 0.01, 0.2 + t * 0.01, 0.2 + t * 0.01, 0.02);
            if (t % 15 == 0) l.playSound(null, ie.blockPosition(), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.HOSTILE, 2F, 0.5F + t / 60F);
            if (t < 60) continue;
            it.remove();
            UUID owner = ie.getOwner() instanceof ServerPlayer sp ? sp.getUUID() : null;
            // Round 29: an Omnilex nobody (online) threw - dispensed, or dropped back by a failed genesis - just floats
            // and can be picked up again; only a challenger can start a genesis
            if (owner == null) {
                ie.setPickUpDelay(10);
                continue;
            }
            if (ProofArena.get(l.getServer()).active(owner)) {
                ie.setPickUpDelay(10);
                ie.setNoGravity(false);
                ServerPlayer sp = l.getServer().getPlayerList().getPlayer(owner);
                if (sp != null) sp.displayClientMessage(Component.translatable("proof.theoreticalsciences.already").withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            GenesisEntity g = com.theoreticalsciences.transcend.TranscendRegistry.GENESIS.get().create(l);
            if (g == null) continue;
            g.moveTo(ie.getX(), ie.getY() + 1.5, ie.getZ(), 0, 0);
            g.owner = owner;
            l.addFreshEntity(g);
            ie.discard();
        }
    }

    private AlphaOmegaEvents() {}
}
