package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Censer of Entropy (endgame): lays any harmful effect on a creature - or on a place, as a lingering cloud.
 * <ul>
 *   <li>use on a creature: it suffers the chosen effect;</li>
 *   <li>use in the air: a cloud of the effect where you look (up to 48 blocks), of the chosen radius;</li>
 *   <li>sneak-use: choose the effect, its strength, how long it lasts and the cloud's size.</li>
 * </ul>
 * The settings are kept on the item. Round 32: a touch costs 200 Eschaton Charge, a cloud or burst 400; the settings
 * screen still opens when empty.
 */
public class EntropyCenserItem extends Item implements EschatonPowered {
    public static final int MAX_CHARGE = 10_000;
    public static final int MAX_SECONDS = 3600, MAX_LEVEL = 255, MAX_RADIUS = 16;
    /** Instant effects (Instant Damage...) work out their strength by doubling: past this they overflow and stop working. */
    public static final int MAX_INSTANT_LEVEL = 28;
    /** Clouds last at most this long (ticks) and each player may have this many at once. */
    public static final int MAX_CLOUD_TICKS = 20 * 600, MAX_CLOUDS = 4;

    public EntropyCenserItem(Properties p) {
        super(p.stacksTo(1));
    }

    public record Settings(String effect, int seconds, int level, int radius) {}

    public static Settings settings(ItemStack s) {
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new Settings(t.contains("Effect") ? t.getString("Effect") : "minecraft:wither", t.contains("Seconds") ? t.getInt("Seconds") : 30,
                t.contains("Level") ? t.getInt("Level") : 1, t.contains("Radius") ? t.getInt("Radius") : 4);
    }

    /** Server: new settings from the screen (checked). */
    public static void set(ServerPlayer sp, Settings st) {
        ItemStack s = sp.getMainHandItem().getItem() instanceof EntropyCenserItem ? sp.getMainHandItem() : sp.getOffhandItem();
        if (!(s.getItem() instanceof EntropyCenserItem)) return;
        if (harmful(st.effect()) == null) return;
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        t.putString("Effect", st.effect());
        t.putInt("Seconds", Math.max(1, Math.min(MAX_SECONDS, st.seconds())));
        t.putInt("Level", Math.max(1, Math.min(MAX_LEVEL, st.level())));
        t.putInt("Radius", Math.max(1, Math.min(MAX_RADIUS, st.radius())));
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** The harmful effects there are (any mod's). */
    public static List<ResourceLocation> effects() {
        List<ResourceLocation> out = new ArrayList<>();
        for (MobEffect e : BuiltInRegistries.MOB_EFFECT) if (e.getCategory() == MobEffectCategory.HARMFUL) out.add(BuiltInRegistries.MOB_EFFECT.getKey(e));
        out.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    static Holder<MobEffect> harmful(String id) {
        ResourceLocation r = ResourceLocation.tryParse(id);
        if (r == null) return null;
        var h = BuiltInRegistries.MOB_EFFECT.getHolder(r).orElse(null);
        return h != null && h.value().getCategory() == MobEffectCategory.HARMFUL ? h : null;
    }

    private static MobEffectInstance instance(Settings st) {
        Holder<MobEffect> h = harmful(st.effect());
        if (h == null) return null;
        int ticks = h.value().isInstantenous() ? 1 : st.seconds() * 20;
        int lv = h.value().isInstantenous() ? Math.min(st.level(), MAX_INSTANT_LEVEL) : st.level();
        return new MobEffectInstance(h, ticks, lv - 1);
    }

    /** A Censer cloud spares its owner, and other players unless PvP (and teams) allow it. */
    public static void cloudApplies(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable e) {
        if (!(e.getEffectSource() instanceof AreaEffectCloud c) || !c.getTags().contains("ts_censer")) return;
        if (!(e.getEntity() instanceof Player target)) return;             // creatures: always
        String who = ownerTag(c);
        // the owner is known by name, not by body: still spared after respawning, and with the owner gone
        // (offline, another dimension) no player is touched at all
        Player owner = who == null || e.getEntity().getServer() == null ? null
                : e.getEntity().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(who));
        if (who == null || target.getUUID().toString().equals(who) || owner == null || !mayAffect(owner, target))
            e.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    @javax.annotation.Nullable
    static String ownerTag(AreaEffectCloud c) {
        for (String t : c.getTags()) if (t.startsWith("ts_owner:")) return t.substring(9);
        return null;
    }

    /** Other players only where the server allows PvP and teams allow it. */
    static boolean mayAffect(Player user, LivingEntity target) {
        if (!(target instanceof Player other) || other == user) return true;
        var server = user.getServer();
        return (server == null || server.isPvpAllowed()) && user.canHarmPlayer(other);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.CONSUME;
        MobEffectInstance e = instance(settings(stack));
        if (e == null) return InteractionResult.PASS;
        if (!mayAffect(player, target)) return InteractionResult.FAIL;
        if (!EschatonCharge.spendOrSay(player, stack, EschatonCharge.CENSER_TOUCH)) return InteractionResult.FAIL;
        target.addEffect(e, player);
        ((ServerLevel) player.level()).sendParticles(com.theoreticalsciences.registry.QHParticles.VOID_INK.get(), target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 20, 0.3, 0.4, 0.3, 0.02);
        player.level().playSound(null, target.blockPosition(), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 0.5F, 1.6F);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) com.theoreticalsciences.client.ClientHooks.openCenser(stack);
            return InteractionResultHolder.success(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.consume(stack);
        Settings st = settings(stack);
        MobEffectInstance e = instance(st);
        if (e == null) return InteractionResultHolder.fail(stack);
        HitResult hit = SingularityScepterItem.aim(player, 48);
        boolean onCreature = hit instanceof EntityHitResult eh0 && eh0.getEntity() instanceof LivingEntity le0 && mayAffect(player, le0);
        if (!(hit instanceof EntityHitResult eh1 && eh1.getEntity() instanceof LivingEntity) || onCreature)
            if (!EschatonCharge.spendOrSay(player, stack, onCreature ? EschatonCharge.CENSER_TOUCH : EschatonCharge.CENSER_CLOUD))
                return InteractionResultHolder.fail(stack);
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le) {
            if (!mayAffect(player, le)) return InteractionResultHolder.fail(stack);
            le.addEffect(e, player);
        } else if (e.getEffect().value().isInstantenous()) {
            // an instant effect cannot linger (and a cloud would not ask whose side anyone is on): one burst, now
            Vec3 at = hit.getLocation();
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(st.radius()),
                    t -> t != player && t.isAlive() && t.distanceToSqr(at) <= st.radius() * st.radius() && mayAffect(player, t)))
                e.getEffect().value().applyInstantenousEffect(player, player, le, e.getAmplifier(), 1.0);
            ((ServerLevel) level).sendParticles(com.theoreticalsciences.registry.QHParticles.VOID_INK.get(), at.x, at.y + 0.5, at.z, 60, st.radius() / 2.0, 0.5, st.radius() / 2.0, 0.02);
        } else {
            // only a few clouds per player at once: the oldest one fades when another is laid
            List<AreaEffectCloud> mine = ((ServerLevel) level).getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox().inflate(256),
                    c -> c.getTags().contains("ts_censer") && player.getUUID().toString().equals(ownerTag(c)));
            mine.sort(java.util.Comparator.comparingInt(c -> -c.tickCount));
            for (int i = 0; i + MAX_CLOUDS - 1 < mine.size(); i++) mine.get(i).discard();
            Vec3 at = hit.getLocation();
            AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
            cloud.setOwner(player);
            cloud.addTag("ts_owner:" + player.getUUID());
            cloud.setRadius(st.radius());
            cloud.setRadiusOnUse(0);
            cloud.setRadiusPerTick(0);
            cloud.setWaitTime(5);
            cloud.setDuration(Math.max(60, Math.min(MAX_CLOUD_TICKS, st.seconds() * 20)));
            cloud.addTag("ts_censer");
            cloud.addEffect(new MobEffectInstance(e.getEffect(), Math.max(1, Math.min(e.getDuration(), 200)), e.getAmplifier()));
            level.addFreshEntity(cloud);
        }
        level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.FLUID_EMPTY.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int maxCharge() {
        return MAX_CHARGE;
    }

    @Override
    public boolean isBarVisible(ItemStack s) {
        return EschatonCharge.barVisible(s);
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return EschatonCharge.barWidth(s);
    }

    @Override
    public int getBarColor(ItemStack s) {
        return EschatonCharge.barColour(s);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Settings st = settings(stack);
        Holder<MobEffect> h = harmful(st.effect());
        tip.add(Component.translatable("tooltip.theoreticalsciences.censer_set", h == null ? Component.literal(st.effect()) : h.value().getDisplayName(),
                st.level(), st.seconds(), st.radius()).withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(Component.translatable("tooltip.theoreticalsciences.censer_use").withStyle(ChatFormatting.GRAY));
    }
}
