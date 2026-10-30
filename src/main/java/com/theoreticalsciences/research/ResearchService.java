package com.theoreticalsciences.research;

import com.theoreticalsciences.api.event.ResearchUnlockedEvent;
import com.theoreticalsciences.network.ResearchDefsPayload;
import com.theoreticalsciences.network.ResearchStatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-side research logic. Also the backing implementation for the KubeJS bindings. */
public final class ResearchService {

    public static boolean has(MinecraftServer server, UUID player, ResourceLocation id) {
        ResearchSavedData.Entry e = ResearchSavedData.get(server).peek(player);
        return e != null && e.unlocked.contains(id);
    }

    public static boolean has(ServerPlayer player, ResourceLocation id) {
        return has(player.server, player.getUUID(), id);
    }

    public static int insight(ServerPlayer player) {
        return ResearchSavedData.get(player.server).of(player.getUUID()).insight;
    }

    public static void addInsight(ServerPlayer player, int amount) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        ResearchSavedData.Entry e = data.of(player.getUUID());
        e.insight = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) e.insight + amount));
        data.setDirty();
        sync(player);
    }

    /** Insight for a player who may be offline (a machine finishing a thought experiment for its owner). */
    public static void addInsight(net.minecraft.server.MinecraftServer server, java.util.UUID player, int amount) {
        ServerPlayer sp = server.getPlayerList().getPlayer(player);
        if (sp != null) {
            addInsight(sp, amount);
            return;
        }
        ResearchSavedData data = ResearchSavedData.get(server);
        ResearchSavedData.Entry e = data.of(player);
        e.insight = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) e.insight + amount));
        data.setDirty();
    }

    /** Every symbol of the Book of Symbols, at once (the Akashic Record). */
    public static void learnAllGlyphs(ServerPlayer player) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        ResearchSavedData.Entry e = data.of(player.getUUID());
        e.glyphs.addAll(GlyphRegistry.server().keySet());
        data.setDirty();
        sync(player);
    }

    /** Records an observation; returns true the first time this key is observed. */
    public static boolean observe(ServerPlayer player, String key, int insight) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        ResearchSavedData.Entry e = data.of(player.getUUID());
        if (!e.observed.add(key)) return false;
        e.insight += insight;
        data.setDirty();
        sync(player);
        return true;
    }

    public enum UnlockResult { OK, STARTED, UNKNOWN, ALREADY, MISSING_PREREQ, NOT_ENOUGH_INSIGHT, MISSING_ITEMS, BUSY, SECRET, NEEDS_PROOF }

    // ------------------------------------------------------------------ derivation puzzles
    public static boolean puzzleSolved(ResearchSavedData.Entry e, ResourceLocation id) {
        return e.puzzle.getOrDefault(id, "").contains("s");
    }

    static void mark(ResearchSavedData.Entry e, ResourceLocation id, char c) {
        String cur = e.puzzle.getOrDefault(id, "");
        if (cur.indexOf(c) < 0) e.puzzle.put(id, cur + c);
    }

    public static int hintInsight(ResearchDefinition d, String kind) {
        return switch (kind) {
            case "v" -> Math.max(1, d.insight() / 10);
            case "m" -> Math.max(2, d.insight() / 6);
            case "f" -> Math.max(3, d.insight() / 4);
            default -> 0;
        };
    }

    /** The expensive fee for revealing the whole equation. */
    public static net.minecraft.world.item.ItemStack hintFee(ResearchDefinition d) {
        int ins = d.insight();
        if (ins < 70) return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 8);
        String id = ins < 150 ? "empirical_dossier" : ins < 400 ? "theoretical_dossier" : ins < 1000 ? "unified_dossier" : "absolute_dossier";
        return new net.minecraft.world.item.ItemStack(com.theoreticalsciences.registry.QHItems.get(id), 1);
    }

    // Round 29: when each player last tried, measured with the computer's own steady clock (System.nanoTime), not the
    // world's game time - a world with a lower game time (switching worlds in single player) used to make every
    // answer look "too soon" forever.
    private static final long ONE_SECOND_NS = 1_000_000_000L;
    private static final java.util.Map<java.util.UUID, Long> LAST_ANSWER = com.theoreticalsciences.world.SessionMaps.perPlayer(new java.util.HashMap<>());


    private static final java.util.Map<java.util.UUID, Long> LAST_DERIVE = com.theoreticalsciences.world.SessionMaps.perPlayer(new java.util.HashMap<>());

    /** Round 29: forget a player's last-attempt times when they log out (or all of them when the server stops: null). */
    public static void forgetAttempts(java.util.UUID player) {
        if (player == null) {
            LAST_ANSWER.clear();
            LAST_DERIVE.clear();
        } else {
            LAST_ANSWER.remove(player);
            LAST_DERIVE.remove(player);
        }
    }

    public static void handlePuzzle(ServerPlayer p, com.theoreticalsciences.network.PuzzlePayload msg) {
        ResearchDefinition def = ResearchManager.INSTANCE.get(msg.id());
        if (def == null || def.puzzle().isEmpty()) return;
        ResearchPuzzle pz = def.puzzle().get();
        ResearchSavedData data = ResearchSavedData.get(p.server);
        ResearchSavedData.Entry e = data.of(p.getUUID());
        for (ResourceLocation req : def.requires()) if (!e.unlocked.contains(req)) return;
        boolean creative = p.isCreative();
        switch (msg.action()) {
            case 0 -> {
                List<String> t = msg.tokens();
                if (e.puzzle.getOrDefault(msg.id(), "").contains("d")) return;
                // one attempt a second: no brute-forcing the arrangement with a modded client
                long now0 = System.nanoTime();
                Long last0 = LAST_DERIVE.get(p.getUUID());
                if (last0 != null && now0 - last0 < ONE_SECOND_NS) return;
                LAST_DERIVE.put(p.getUUID(), now0);
                if (t.isEmpty() || t.size() > 400 || !pz.fitsTemplate(t)) return;
                // every symbol of the derivation has to have been learned
                if (!GlyphService.knowsAll(p, pz.requiredGlyphs())) {
                    p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.missing_glyphs").withStyle(ChatFormatting.RED), true);
                    return;
                }
                boolean ok;
                try {
                    ok = pz.equivalent(t);
                } catch (RuntimeException ex) {
                    ok = false;
                }
                if (ok) {
                    mark(e, msg.id(), 'd');
                    p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.derived").withStyle(ChatFormatting.GREEN), true);
                    p.level().playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.PLAYERS, 1F, 1.4F);
                } else {
                    p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.not_equal").withStyle(ChatFormatting.RED), true);
                }
            }
            case 1 -> {
                if (!e.puzzle.getOrDefault(msg.id(), "").contains("d")) return;
                // one guess a second: no sweeping the answer with a modded client
                long now = System.nanoTime();
                Long last = LAST_ANSWER.get(p.getUUID());
                if (last != null && now - last < ONE_SECOND_NS) return;
                LAST_ANSWER.put(p.getUUID(), now);
                boolean right;
                try {
                    right = pz.answerOk(msg.answer(), pz.given(puzzleSalt(p.server), msg.id().toString()));
                } catch (RuntimeException ex) {
                    com.theoreticalsciences.TheoreticalSciences.LOGGER.warn("Research puzzle {} cannot be evaluated: {}", msg.id(), ex.toString());
                    right = false;
                }
                if (right) {
                    mark(e, msg.id(), 's');
                    p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.solved").withStyle(ChatFormatting.GREEN), true);
                    p.level().playSound(null, p.blockPosition(), com.theoreticalsciences.registry.QHSounds.ASCEND.get(), SoundSource.PLAYERS, 0.8F, 1.8F);
                    data.setDirty();
                    UnlockResult r = tryUnlock(p, msg.id());
                    if (r != UnlockResult.OK && r != UnlockResult.STARTED && r != UnlockResult.ALREADY)
                        p.displayClientMessage(Component.translatable("message.theoreticalsciences.research_" + r.name().toLowerCase(java.util.Locale.ROOT))
                                .withStyle(ChatFormatting.RED), false);
                } else {
                    p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.wrong_answer").withStyle(ChatFormatting.RED), true);
                }
            }
            case 2 -> {
                String k = msg.kind();
                if (e.unlocked.contains(msg.id()) || puzzleSolved(e, msg.id()) || !com.theoreticalsciences.QHConfig.RESEARCH_PUZZLES.get()) return;
                if (!List.of("v", "m", "f", "e").contains(k) || e.puzzle.getOrDefault(msg.id(), "").contains(k)) return;
                if (!creative) {
                    if (k.equals("e")) {
                        net.minecraft.world.item.ItemStack fee = hintFee(def);
                        int have = p.getInventory().countItem(fee.getItem());
                        if (have < fee.getCount()) {
                            p.displayClientMessage(Component.translatable("puzzle.theoreticalsciences.need_fee", fee.getCount(), fee.getHoverName()).withStyle(ChatFormatting.RED), true);
                            return;
                        }
                        p.getInventory().clearOrCountMatchingItems(st -> st.is(fee.getItem()), fee.getCount(), p.inventoryMenu.getCraftSlots());
                    } else {
                        int cost = hintInsight(def, k);
                        if (e.insight < cost) {
                            p.displayClientMessage(Component.translatable("message.theoreticalsciences.research_not_enough_insight").withStyle(ChatFormatting.RED), true);
                            return;
                        }
                        e.insight -= cost;
                    }
                }
                mark(e, msg.id(), k.charAt(0));
            }
            case 3 -> {
                if (!creative) return;
                mark(e, msg.id(), 'd');
                mark(e, msg.id(), 's');
            }
            default -> {
            }
        }
        data.setDirty();
        sync(p);
    }

    /** Player-initiated unlock from the Research Terminal: checks prerequisites and pays the cost. */
    public static UnlockResult tryUnlock(ServerPlayer player, ResourceLocation id) {
        ResearchDefinition def = ResearchManager.INSTANCE.get(id);
        if (def == null) return UnlockResult.UNKNOWN;
        ResearchSavedData data = ResearchSavedData.get(player.server);
        ResearchSavedData.Entry e = data.of(player.getUUID());
        if (e.unlocked.contains(id)) return UnlockResult.ALREADY;
        if (def.grantOnly()) return UnlockResult.SECRET;
        for (ResourceLocation req : def.requires()) if (!e.unlocked.contains(req)) return UnlockResult.MISSING_PREREQ;
        if (e.active != null) return UnlockResult.BUSY;
        if (def.puzzle().isPresent() && com.theoreticalsciences.QHConfig.RESEARCH_PUZZLES.get() && !puzzleSolved(e, id)) return UnlockResult.NEEDS_PROOF;
        // Creative mode skips the price (insight and items) and the waiting - it is for testing.
        boolean free = player.isCreative();
        if (!free && e.insight < def.insight()) return UnlockResult.NOT_ENOUGH_INSIGHT;
        if (!free && !hasItems(player, def.cost(), false)) return UnlockResult.MISSING_ITEMS;
        if (!free) {
            hasItems(player, def.cost(), true);
            e.insight -= def.insight();
        }
        int seconds = (int) Math.round(def.time() * com.theoreticalsciences.QHConfig.RESEARCH_TIME_MULTIPLIER.get());
        if (!free && seconds > 0) {
            long now = player.server.overworld().getGameTime();
            e.active = id;
            e.activeStart = now;
            e.activeEnd = now + seconds * 20L;
            data.setDirty();
            sync(player);
            player.level().playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.research_started", def.titleComponent(id), formatTime(seconds))
                    .withStyle(ChatFormatting.AQUA), false);
            return UnlockResult.STARTED;
        }
        grant(player, id);
        player.level().playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.ASCEND.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.translatable("message.theoreticalsciences.research_unlocked", def.titleComponent(id)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return UnlockResult.OK;
    }

    public static String formatTime(long seconds) {
        long h = seconds / 3600, m = seconds / 60 % 60, s = seconds % 60;
        return h > 0 ? String.format("%dh %02dm", h, m) : m > 0 ? String.format("%dm %02ds", m, s) : s + "s";
    }

    /** Shortens the owner's current study (Research Nexus). Returns the seconds actually removed. */
    public static int accelerate(net.minecraft.server.MinecraftServer server, UUID owner, int seconds) {
        ResearchSavedData data = ResearchSavedData.get(server);
        ResearchSavedData.Entry e = data.peek(owner);
        if (e == null || e.active == null) return 0;
        long now = server.overworld().getGameTime();
        long before = Math.max(0, e.activeEnd - now);
        e.activeEnd = Math.max(now, e.activeEnd - seconds * 20L);
        data.setDirty();
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) {
            if (!checkActive(p)) sync(p);
        }
        return (int) ((before - Math.max(0, e.activeEnd - now)) / 20);
    }

    /** Completes the study once its time is up. Returns true if it completed. */
    public static boolean checkActive(ServerPlayer player) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        ResearchSavedData.Entry e = data.peek(player.getUUID());
        if (e == null || e.active == null || player.server.overworld().getGameTime() < e.activeEnd) return false;
        ResourceLocation id = e.active;
        e.active = null;
        data.setDirty();
        ResearchDefinition def = ResearchManager.INSTANCE.get(id);
        grant(player, id);
        player.level().playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.ASCEND.get(), SoundSource.PLAYERS, 0.8F, 1.6F);
        if (def != null)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.research_unlocked", def.titleComponent(id)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return true;
    }

    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            ResearchSavedData.Entry e = ResearchSavedData.get(sp.server).peek(sp.getUUID());
            if (e != null && e.active != null) {
                if (!checkActive(sp) && sp.tickCount % 200 == 0) sync(sp);
            }
        }
    }

    private static boolean hasItems(ServerPlayer player, List<SizedIngredient> cost, boolean take) {
        var inv = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) stacks.add(inv.getItem(i));
        boolean ok = com.theoreticalsciences.recipe.RecipeMatcher.matchItems(cost, stacks, take);
        if (take) inv.setChanged();
        return ok;
    }

    /** Unconditional grant (commands, scripts, terminal after payment). */
    public static void grant(ServerPlayer player, ResourceLocation id) {
        grantQuietly(player, id);
        sync(player);
    }

    /** Many at once (the Akashic Record): granted one by one, but the player's client is told once at the end. */
    public static int grantAll(ServerPlayer player, java.util.Collection<ResourceLocation> ids) {
        int n = 0;
        for (ResourceLocation id : ids) if (grantQuietly(player, id)) n++;
        sync(player);
        return n;
    }

    private static boolean grantQuietly(ServerPlayer player, ResourceLocation id) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        boolean added = false;
        if (data.of(player.getUUID()).unlocked.add(id)) {
            data.setDirty();
            AdvancementHolder adv = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "research/" + id.getPath()));
            if (adv != null) {
                for (String crit : adv.value().criteria().keySet()) player.getAdvancements().award(adv, crit);
            }
            NeoForge.EVENT_BUS.post(new ResearchUnlockedEvent(player, id));
            added = true;
        }
        return added;
    }

    public static void revoke(ServerPlayer player, ResourceLocation id) {
        ResearchSavedData data = ResearchSavedData.get(player.server);
        if (data.of(player.getUUID()).unlocked.remove(id)) {
            data.setDirty();
            AdvancementHolder adv = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "research/" + id.getPath()));
            if (adv != null) for (String crit : adv.value().criteria().keySet()) player.getAdvancements().revoke(adv, crit);
        }
        sync(player);
    }

    /**
     * This world's puzzle numbers (round 22): worked out from the world seed, so they are the same every time the
     * world is opened but different in every world. 0 means the book values.
     */
    public static long puzzleSalt(net.minecraft.server.MinecraftServer server) {
        boolean on;
        try {
            on = com.theoreticalsciences.QHConfig.PUZZLE_VALUES_PER_WORLD.get();
        } catch (IllegalStateException e) {
            on = true;
        }
        if (!on) return 0;
        long z = server.getWorldData().worldGenOptions().seed() + 0x5DEECE66DL;
        z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
        z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
        z ^= z >>> 33;
        return z == 0 ? 1 : z;
    }

    public static void sync(ServerPlayer player) {
        ResearchSavedData.Entry e = ResearchSavedData.get(player.server).of(player.getUUID());
        long now = player.server.overworld().getGameTime();
        String active = e.active == null ? "" : e.active.toString();
        int total = e.active == null ? 0 : (int) Math.min(Integer.MAX_VALUE, (e.activeEnd - e.activeStart) / 20);
        int remaining = e.active == null ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.max(0, e.activeEnd - now) / 20);
        PacketDistributor.sendToPlayer(player, new ResearchStatePayload(new ArrayList<>(e.unlocked), e.insight, e.observed.size(), active, total, remaining));
        net.minecraft.nbt.CompoundTag pz = new net.minecraft.nbt.CompoundTag();
        e.puzzle.forEach((k, v) -> pz.putString(k.toString(), v));
        pz.putBoolean("#enabled", com.theoreticalsciences.QHConfig.RESEARCH_PUZZLES.get());
        pz.putLong("#salt", puzzleSalt(player.server));
        net.minecraft.nbt.ListTag gl = new net.minecraft.nbt.ListTag();
        e.glyphs.forEach(g -> gl.add(net.minecraft.nbt.StringTag.valueOf(g)));
        pz.put("#glyphs", gl);
        PacketDistributor.sendToPlayer(player, new com.theoreticalsciences.network.PuzzleStatePayload(pz));
    }

    public static void syncDefinitions(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ResearchDefsPayload(ResearchManager.INSTANCE.syncJson()));
        PacketDistributor.sendToPlayer(player, new com.theoreticalsciences.network.GlyphDefsPayload(GlyphRegistry.INSTANCE.syncJson()));
        PacketDistributor.sendToPlayer(player, new com.theoreticalsciences.network.SingularityDefsPayload(com.theoreticalsciences.extreme.Singularities.syncJson()));
        PacketDistributor.sendToPlayer(player, new com.theoreticalsciences.network.PhysicsDefsPayload(com.theoreticalsciences.machine.physics.PhysicsManager.INSTANCE.syncJson()));
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            syncDefinitions(sp);
            sync(sp);
        }
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(p -> {
            syncDefinitions(p);
            sync(p);
        });
    }

    /** Unused helper kept for API completeness. */
    static boolean anyMatch(Ingredient ing, ItemStack stack) {
        return ing.test(stack);
    }

    private ResearchService() {}
}
