package com.theoreticalsciences.endgame;

import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The laws the Lawgiver's Codex has rewritten, per dimension, and everything that enforces them.
 * <p>
 * Rules are kept in a SavedData on the overworld. Gravity applies to every living thing in the dimension, mob strength
 * to every Mob; both are transient attribute modifiers with fixed ids, refreshed every second for dimensions that have
 * rules and whenever an entity joins a level (which also takes them off again when it goes somewhere without rules).
 */
public final class CodexLaws {
    public static final double[] GRAVITY = {0, 0.1, 0.25, 0.5, 1, 2, 4};
    public static final double[] STRENGTH = {0.25, 0.5, 1, 2, 5, 10};
    static final int GRAVITY_NATURAL = 4, STRENGTH_NATURAL = 2;
    public static final int MAX_CREATE = 16;
    static final String PACIFIED = "theoreticalsciences_pacified";

    private static final ResourceLocation GRAVITY_ID = TheoreticalSciences.id("codex_gravity");
    private static final ResourceLocation HEALTH_ID = TheoreticalSciences.id("codex_health");
    private static final ResourceLocation DAMAGE_ID = TheoreticalSciences.id("codex_damage");
    private static final ResourceLocation SPEED_ID = TheoreticalSciences.id("codex_speed");
    private static final ResourceLocation FLY_SPEED_ID = TheoreticalSciences.id("codex_flying_speed");
    private static final ResourceLocation SCALE_ID = TheoreticalSciences.id("codex_scale");

    // ------------------------------------------------------------------ actions (shared with the screen)
    public static final int REQUEST = 0, SET_GRAVITY = 1, SET_TIME = 2, SET_WEATHER = 3, SET_HOSTILE = 4, SET_PASSIVE = 5,
            SET_HEALTH = 6, SET_DAMAGE = 7, SET_SPEED = 8, CREATE = 9, HEAL = 10, UNMAKE = 11, PACIFY = 12, TRANSMUTE = 13,
            GROW = 14, SHRINK = 15, RESTORE = 16;

    // ------------------------------------------------------------------ the rules

    public static final class Rules {
        public int gravity = GRAVITY_NATURAL, health = STRENGTH_NATURAL, damage = STRENGTH_NATURAL, speed = STRENGTH_NATURAL;
        public boolean hostile = true, passive = true;

        boolean natural() {
            return gravity == GRAVITY_NATURAL && health == STRENGTH_NATURAL && damage == STRENGTH_NATURAL && speed == STRENGTH_NATURAL && hostile && passive;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("gravity", gravity);
            t.putInt("health", health);
            t.putInt("damage", damage);
            t.putInt("speed", speed);
            t.putBoolean("hostile", hostile);
            t.putBoolean("passive", passive);
            return t;
        }

        static Rules load(CompoundTag t) {
            Rules r = new Rules();
            r.gravity = clampIdx(t.getInt("gravity"), GRAVITY.length);
            r.health = clampIdx(t.getInt("health"), STRENGTH.length);
            r.damage = clampIdx(t.getInt("damage"), STRENGTH.length);
            r.speed = clampIdx(t.getInt("speed"), STRENGTH.length);
            r.hostile = t.getBoolean("hostile");
            r.passive = t.getBoolean("passive");
            return r;
        }
    }

    static int clampIdx(int i, int n) {
        return Math.max(0, Math.min(n - 1, i));
    }

    static final class Data extends SavedData {
        static final String NAME = TheoreticalSciences.MODID + "_codex_laws";
        final Map<ResourceKey<Level>, Rules> rules = new HashMap<>();

        static Data load(CompoundTag tag, HolderLookup.Provider regs) {
            Data d = new Data();
            CompoundTag dims = tag.getCompound("dims");
            for (String k : dims.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(k);
                if (id != null) d.rules.put(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id), Rules.load(dims.getCompound(k)));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
            CompoundTag dims = new CompoundTag();
            rules.forEach((k, r) -> {
                if (!r.natural()) dims.put(k.location().toString(), r.save());
            });
            tag.put("dims", dims);
            return tag;
        }
    }

    static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), Data.NAME);
    }

    /** The rewritten rules of this level, or null if its laws are the natural ones. */
    static Rules rulesFor(Level level) {
        if (!(level instanceof ServerLevel sl)) return null;
        Rules r = data(sl.getServer()).rules.get(sl.dimension());
        return r == null || r.natural() ? null : r;
    }

    private static Rules edit(ServerLevel level) {
        Data d = data(level.getServer());
        d.setDirty();
        return d.rules.computeIfAbsent(level.dimension(), k -> new Rules());
    }

    // ------------------------------------------------------------------ enforcement

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 0) return;
        Data d = data(server);
        if (d.rules.isEmpty()) return;
        for (ServerLevel level : server.getAllLevels()) {
            Rules r = d.rules.get(level.dimension());
            if (r == null || r.natural()) continue;
            for (Entity ent : level.getAllEntities())
                if (ent instanceof LivingEntity le) apply(le, r);
        }
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof LivingEntity le)) return;
        apply(le, rulesFor(e.getLevel()));
    }

    /** Bring an entity in line with the given rules (null: natural laws, i.e. take every modifier off). */
    static void apply(LivingEntity e, Rules r) {
        if (!e.isAlive()) return;
        modifier(e, Attributes.GRAVITY, GRAVITY_ID, r == null ? 0 : GRAVITY[r.gravity] - 1);
        if (!(e instanceof Mob)) return;
        double hp = r == null ? 0 : STRENGTH[r.health] - 1;
        // The health modifier is not saved with the mob, and on loading vanilla clamps its health to the natural
        // maximum. So the fraction of health it has left is kept in its own data (refreshed every second) and is what
        // gets restored whenever the modifier is (re)applied - never a second rescale of an already-scaled value.
        var pd = e.getPersistentData();
        float oldMax = e.getMaxHealth();
        float frac = pd.contains(HEALTH_FRAC) ? pd.getFloat(HEALTH_FRAC) : (oldMax > 0 ? e.getHealth() / oldMax : 1F);
        if (modifier(e, Attributes.MAX_HEALTH, HEALTH_ID, hp)) {
            float newMax = e.getMaxHealth();
            if (newMax > 0) e.setHealth(Math.max(1F, Math.min(newMax, frac * newMax)));
        }
        if (hp == 0) pd.remove(HEALTH_FRAC);
        else if (e.getMaxHealth() > 0) pd.putFloat(HEALTH_FRAC, Math.max(0.0001F, Math.min(1F, e.getHealth() / e.getMaxHealth())));
        modifier(e, Attributes.ATTACK_DAMAGE, DAMAGE_ID, r == null ? 0 : STRENGTH[r.damage] - 1);
        double sp = r == null ? 0 : STRENGTH[r.speed] - 1;
        modifier(e, Attributes.MOVEMENT_SPEED, SPEED_ID, sp);
        modifier(e, Attributes.FLYING_SPEED, FLY_SPEED_ID, sp);
    }

    /** Sets a multiply-total transient modifier (0 removes it). Returns whether anything changed. */
    private static boolean modifier(LivingEntity e, Holder<Attribute> attr, ResourceLocation id, double amount) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return false;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) return cur != null && inst.removeModifier(id);
        if (cur != null && cur.amount() == amount) return false;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return true;
    }

    private static boolean forbidden(Rules r, Mob mob) {
        MobCategory c = mob.getType().getCategory();
        if (c == MobCategory.MISC) return false;
        return c.isFriendly() ? !r.passive : !r.hostile;
    }

    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        MobSpawnType t = e.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION) return;
        Rules r = rulesFor(e.getLevel().getLevel());
        if (r != null && forbidden(r, e.getEntity())) e.setSpawnCancelled(true);
    }

    public static void onPositionCheck(MobSpawnEvent.PositionCheck e) {
        if (e.getSpawnType() != MobSpawnType.NATURAL && e.getSpawnType() != MobSpawnType.CHUNK_GENERATION) return;
        Rules r = rulesFor(e.getLevel().getLevel());
        if (r != null && forbidden(r, e.getEntity())) e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() instanceof Player && e.getEntity().getPersistentData().getBoolean(PACIFIED))
            e.setNewAboutToBeSetTarget(null);
    }

    // ------------------------------------------------------------------ requests from the screen

    static boolean holdsCodex(Player p) {
        return p.getMainHandItem().getItem() instanceof LawgiversCodexItem || p.getOffhandItem().getItem() instanceof LawgiversCodexItem;
    }

    static void sendState(ServerPlayer sp) {
        Rules r = data(sp.server).rules.getOrDefault(sp.level().dimension(), new Rules());
        PacketDistributor.sendToPlayer(sp, new CodexStatePayload(r.gravity, r.hostile, r.passive, r.health, r.damage, r.speed));
    }

    private static final String HEALTH_FRAC = "theoreticalsciences_codex_hpfrac";
    /** Per-player: game time before which the next request is refused (creating things is slower). */
    private static final Map<java.util.UUID, Long> NEXT = com.theoreticalsciences.world.SessionMaps.perPlayer(new java.util.concurrent.ConcurrentHashMap<>());

    public static void forget(java.util.UUID id) {
        NEXT.remove(id);
    }

    private static void say(ServerPlayer sp, String key, ChatFormatting colour, Object... args) {
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.codex." + key, args).withStyle(colour), true);
    }

    private static void chime(ServerPlayer sp, float pitch) {
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 1.0F, pitch);
    }

    public static void handle(ServerPlayer sp, CodexActionPayload p) {
        if (!holdsCodex(sp) || sp.isSpectator()) return;
        ServerLevel level = sp.serverLevel();
        if (p.action() != REQUEST) {
            long now = level.getGameTime();
            if (now < NEXT.getOrDefault(sp.getUUID(), 0L)) return;
            NEXT.put(sp.getUUID(), now + (p.action() == CREATE || p.action() == TRANSMUTE ? 20 : 4));
            // round 32: every law written costs Eschaton Charge
            ItemStack codex = sp.getMainHandItem().getItem() instanceof LawgiversCodexItem ? sp.getMainHandItem() : sp.getOffhandItem();
            if (!EschatonCharge.spendOrSay(sp, codex, EschatonCharge.CODEX_LAW)) return;
        }
        switch (p.action()) {
            case REQUEST -> {}
            case SET_GRAVITY -> {
                edit(level).gravity = clampIdx(p.arg(), GRAVITY.length);
                sweep(level);
                say(sp, "gravity", ChatFormatting.AQUA, fmt(GRAVITY[clampIdx(p.arg(), GRAVITY.length)]));
                chime(sp, 0.6F);
            }
            case SET_HEALTH, SET_DAMAGE, SET_SPEED -> {
                Rules r = edit(level);
                int i = clampIdx(p.arg(), STRENGTH.length);
                if (p.action() == SET_HEALTH) r.health = i;
                else if (p.action() == SET_DAMAGE) r.damage = i;
                else r.speed = i;
                sweep(level);
                String which = p.action() == SET_HEALTH ? "health" : p.action() == SET_DAMAGE ? "damage" : "speed";
                say(sp, "strength", ChatFormatting.RED, Component.translatable("gui.theoreticalsciences.codex." + which), fmt(STRENGTH[i]));
                chime(sp, 0.8F);
            }
            case SET_HOSTILE, SET_PASSIVE -> {
                Rules r = edit(level);
                boolean on = p.arg() != 0;
                if (p.action() == SET_HOSTILE) r.hostile = on;
                else r.passive = on;
                say(sp, (p.action() == SET_HOSTILE ? "hostile_" : "passive_") + (on ? "on" : "off"), ChatFormatting.GREEN);
                chime(sp, 1.0F);
            }
            case SET_TIME -> {
                long target = switch (clampIdx(p.arg(), 4)) {
                    case 0 -> 0L;
                    case 1 -> 6000L;
                    case 2 -> 12000L;
                    default -> 18000L;
                };
                ServerLevel clock = level.getLevelData() instanceof DerivedLevelData ? sp.server.overworld() : level;
                long now = clock.getDayTime(), day = now - Math.floorMod(now, 24000L), t = day + target;
                if (t < now) t += 24000L;
                clock.setDayTime(t);
                say(sp, "time." + clampIdx(p.arg(), 4), ChatFormatting.YELLOW);
                chime(sp, 1.2F);
            }
            case SET_WEATHER -> {
                ServerLevel sky = level.getLevelData() instanceof DerivedLevelData ? sp.server.overworld() : level;
                int w = clampIdx(p.arg(), 3);
                switch (w) {
                    case 0 -> sky.setWeatherParameters(12000, 0, false, false);
                    case 1 -> sky.setWeatherParameters(0, 12000, true, false);
                    default -> sky.setWeatherParameters(0, 12000, true, true);
                }
                say(sp, "weather." + w, ChatFormatting.YELLOW);
                chime(sp, 1.2F);
            }
            case CREATE -> create(sp, level, p.text(), Math.max(1, Math.min(MAX_CREATE, p.arg())));
            case HEAL, UNMAKE, PACIFY, TRANSMUTE, GROW, SHRINK -> rewrite(sp, level, p);
            case RESTORE -> {
                Data d = data(sp.server);
                d.rules.remove(level.dimension());
                d.setDirty();
                sweep(level);
                say(sp, "restored", ChatFormatting.GOLD);
                level.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            default -> {}
        }
        sendState(sp);
    }

    /** Re-apply this level's rules to everything in it right now. */
    private static void sweep(ServerLevel level) {
        Rules r = rulesFor(level);
        for (Entity ent : level.getAllEntities())
            if (ent instanceof LivingEntity le) apply(le, r);
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Only living, summonable, non-player types - no Ender Dragon, no bosses, nothing tagged not_spawnable. */
    static EntityType<?> creatable(String text, ServerLevel level) {
        if (text == null) return null;
        ResourceLocation id = ResourceLocation.tryParse(text.trim().toLowerCase(Locale.ROOT));
        if (id == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null || type == EntityType.PLAYER || type == EntityType.ENDER_DRAGON || !type.canSummon()
                || !type.isEnabled(level.enabledFeatures()) || !DefaultAttributes.hasSupplier(type)) return null;
        // Round 29: no bosses (Alpha Omega, the Wither...) and nothing on the mod's "not spawnable" list - the same
        // rule the Anomaly Engine uses. This covers both CREATE and TRANSMUTE, which both ask here.
        if (type.is(com.theoreticalsciences.dungeon.AnomalyEngineBlockEntity.FORBIDDEN)
                || type.is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) return null;
        return type;
    }

    private static void create(ServerPlayer sp, ServerLevel level, String text, int count) {
        EntityType<?> type = creatable(text, level);
        if (type == null) {
            say(sp, "unknown", ChatFormatting.RED, text);
            return;
        }
        HitResult hit = SingularityScepterItem.aim(sp, 64);
        BlockPos pos = hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK
                ? bh.getBlockPos().relative(bh.getDirection()) : BlockPos.containing(hit.getLocation());
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)) {
            say(sp, "nowhere", ChatFormatting.RED);
            return;
        }
        int made = 0;
        for (int i = 0; i < count; i++) {
            Entity e = type.spawn(level, pos, MobSpawnType.COMMAND);
            if (e == null) continue;
            if (!(e instanceof LivingEntity)) {
                e.discard();
                continue;
            }
            if (count > 1) e.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.3, 0.2, (level.random.nextDouble() - 0.5) * 0.3);
            made++;
        }
        Vec3 c = Vec3.atBottomCenterOf(pos);
        level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y + 0.5, c.z, 40, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.PLAYERS, 1.5F, 1.2F);
        say(sp, "created", ChatFormatting.GREEN, made, type.getDescription());
    }

    private static void rewrite(ServerPlayer sp, ServerLevel level, CodexActionPayload p) {
        Entity ent = level.getEntity(p.target());
        if (!(ent instanceof LivingEntity t) || !t.isAlive() || t.distanceToSqr(sp) > 48 * 48) {
            say(sp, "no_target", ChatFormatting.RED);
            return;
        }
        if (t instanceof Player && p.action() != HEAL) {
            say(sp, "not_players", ChatFormatting.RED);
            return;
        }
        // only what you can actually see, and never someone else's companion
        if (!sp.hasLineOfSight(t)) {
            say(sp, "no_target", ChatFormatting.RED);
            return;
        }
        if (p.action() != HEAL && t instanceof net.minecraft.world.entity.OwnableEntity own && own.getOwnerUUID() != null && !own.getOwnerUUID().equals(sp.getUUID())) {
            say(sp, "not_yours", ChatFormatting.RED, t.getDisplayName());
            return;
        }
        Vec3 c = t.position().add(0, t.getBbHeight() / 2, 0);
        switch (p.action()) {
            case HEAL -> {
                t.setHealth(t.getMaxHealth());
                t.clearFire();
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.RESOLVE.get(), c.x, c.y + 0.5, c.z, 8, 0.4, 0.4, 0.4, 0);
                say(sp, "healed", ChatFormatting.GREEN, t.getDisplayName());
            }
            case UNMAKE -> {
                Component name = t.getDisplayName();
                t.invulnerableTime = 0;
                t.hurt(OmnipotentEdgeItem.unmaking(level, sp), Math.max(1000F, t.getMaxHealth() * 10F));
                if (t.isAlive()) t.kill();
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.SHOCKWAVE.get(), c.x, c.y, c.z, 1, 0, 0, 0, 0);
                say(sp, "unmade", ChatFormatting.DARK_RED, name);
            }
            case PACIFY -> {
                t.getPersistentData().putBoolean(PACIFIED, true);
                if (t instanceof Mob m && m.getTarget() instanceof Player) m.setTarget(null);
                t.setLastHurtByMob(null);
                t.setLastHurtByPlayer(null);
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.RESOLVE.get(), c.x, c.y + 0.5, c.z, 12, 0.4, 0.4, 0.4, 0);
                say(sp, "pacified", ChatFormatting.GREEN, t.getDisplayName());
            }
            case TRANSMUTE -> {
                // a creature carrying a chest, saddle-bags or picked-up items would lose them: refuse
                if (t instanceof net.minecraft.world.entity.animal.horse.AbstractHorse || t instanceof net.minecraft.world.entity.npc.InventoryCarrier
                        || t instanceof net.minecraft.world.entity.vehicle.ContainerEntity) {
                    say(sp, "carries", ChatFormatting.RED, t.getDisplayName());
                    return;
                }
                EntityType<?> type = creatable(p.text(), level);
                if (type == null) {
                    say(sp, "unknown", ChatFormatting.RED, p.text());
                    return;
                }
                Entity n = type.create(level);
                if (!(n instanceof LivingEntity nl)) {
                    if (n != null) n.discard();
                    say(sp, "unknown", ChatFormatting.RED, p.text());
                    return;
                }
                nl.moveTo(t.getX(), t.getY(), t.getZ(), t.getYRot(), t.getXRot());
                nl.setYHeadRot(t.getYHeadRot());
                if (nl instanceof Mob m) {
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.CONVERSION, null);
                    if (t instanceof Mob old && old.isPersistenceRequired()) m.setPersistenceRequired();
                }
                if (t.hasCustomName()) {
                    nl.setCustomName(t.getCustomName());
                    nl.setCustomNameVisible(t.isCustomNameVisible());
                }
                if (t.getPersistentData().getBoolean(PACIFIED)) nl.getPersistentData().putBoolean(PACIFIED, true);
                Component from = t.getDisplayName();
                if (!level.tryAddFreshEntityWithPassengers(nl)) {
                    say(sp, "unknown", ChatFormatting.RED, p.text());
                    return;
                }
                t.discard();
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), c.x, c.y, c.z, 30, 0.5, 0.6, 0.5, 0.05);
                say(sp, "transmuted", ChatFormatting.LIGHT_PURPLE, from, type.getDescription());
            }
            case GROW, SHRINK -> {
                AttributeInstance inst = t.getAttribute(Attributes.SCALE);
                if (inst == null) return;
                AttributeModifier cur = inst.getModifier(SCALE_ID);
                double factor = (cur == null ? 1 : 1 + cur.amount()) * (p.action() == GROW ? 2 : 0.5);
                factor = Math.max(0.125, Math.min(8, factor));
                if (Math.abs(factor - 1) < 1e-6) inst.removeModifier(SCALE_ID);
                else inst.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, factor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y, c.z, 20, 0.5, 0.5, 0.5, 0.05);
                say(sp, "scaled", ChatFormatting.AQUA, t.getDisplayName(), fmt(factor));
            }
            default -> {}
        }
        chime(sp, 1.4F);
    }

    private CodexLaws() {}
}
