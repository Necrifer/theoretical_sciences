package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * The Omnipotent Edge. Huge damage, and every hit also cuts straight through armour and invulnerability.
 * Use: Erasure - every hostile creature in a cone in front of you stops having existed.
 * Sneak-use: Chronal Stasis - time stops for every mob nearby for ten seconds.
 * Round 32: it runs on Eschaton Charge (20 a hit, 400 an Erasure, 600 a Stasis). Empty, it is just a blade
 * (4 damage). Charged, what it kills - drops and experience - goes straight into the wielder's inventory (5 a kill).
 */
public class OmnipotentEdgeItem extends SwordItem implements EschatonPowered {
    public static final int MAX_CHARGE = 20_000;
    static final Tier TIER = new Tier() {
        @Override public int getUses() { return 1_000_000; }      // must be positive; the item is Unbreakable anyway
        @Override public float getSpeed() { return 40F; }
        @Override public float getAttackDamageBonus() { return 0F; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public int getEnchantmentValue() { return 30; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };
    private static final int ERASE_RANGE = 16;

    public OmnipotentEdgeItem(Properties p) {
        // (no attributes on the properties: they depend on the charge, see getDefaultAttributeModifiers below)
        super(TIER, p.fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }

    private static final ItemAttributeModifiers CHARGED = SwordItem.createAttributes(TIER, 199, -2.0F);
    private static final ItemAttributeModifiers DORMANT = SwordItem.createAttributes(TIER, 3, -2.4F);

    @Override
    public int maxCharge() {
        return MAX_CHARGE;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return EschatonCharge.get(stack) > 0 ? CHARGED : DORMANT;
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
    public boolean isFoil(ItemStack s) {
        return EschatonCharge.get(s) > 0;
    }

    /** A charged Edge in the killer's main hand: it pays for the harvest and the drops / experience come to them. */
    private static ServerPlayer harvester(DamageSource src) {
        if (!(src.getEntity() instanceof ServerPlayer sp)) return null;
        ItemStack s = sp.getMainHandItem();
        return s.getItem() instanceof OmnipotentEdgeItem && EschatonCharge.active(sp, s) ? sp : null;
    }

    /** Round 32: what the Edge kills goes straight into the wielder's inventory (overflow lands at their feet). */
    public static void onDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent e) {
        if (e.getEntity() instanceof Player) return;            // a fallen player's belongings stay where they fell
        ServerPlayer sp = harvester(e.getSource());
        if (sp == null || e.getDrops().isEmpty()) return;
        if (!EschatonCharge.spend(sp, sp.getMainHandItem(), EschatonCharge.EDGE_HARVEST)) return;
        var it = e.getDrops().iterator();
        while (it.hasNext()) {
            net.minecraft.world.entity.item.ItemEntity ie = it.next();
            ItemStack st = ie.getItem().copy();
            sp.getInventory().add(st);
            if (st.isEmpty()) it.remove();
            else {
                ie.setItem(st);
                ie.setPos(sp.getX(), sp.getY(), sp.getZ());
                ie.setNoPickUpDelay();
            }
        }
        if (sp.level() instanceof ServerLevel sl)
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), e.getEntity().getX(), e.getEntity().getY(0.5), e.getEntity().getZ(), 12, 0.3, 0.4, 0.3, 0.05);
    }

    /** ...and so does the experience. */
    public static void onExperience(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent e) {
        if (e.getEntity() instanceof Player || !(e.getAttackingPlayer() instanceof ServerPlayer sp) || e.getDroppedExperience() <= 0) return;
        ItemStack s = sp.getMainHandItem();
        if (!(s.getItem() instanceof OmnipotentEdgeItem) || !EschatonCharge.active(sp, s)) return;
        sp.giveExperiencePoints(e.getDroppedExperience());
        e.setDroppedExperience(0);
    }

    /** Damage that ignores armour and invulnerability, but is still credited to the wielder. */
    static DamageSource unmaking(Level level, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC_KILL), attacker);
    }

    /** Players in creative / spectator are never touched, and other players only when PvP allows it. */
    static boolean mayUnmake(Player user, LivingEntity target) {
        if (target == user || !target.isAlive()) return false;
        if (target instanceof Player p) return !p.isCreative() && !p.isSpectator() && user.canHarmPlayer(p);
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // the unmaking costs charge; with none left the Edge is only a blade
        if (!attacker.level().isClientSide && attacker instanceof Player p && mayUnmake(p, target) && EschatonCharge.spend(p, stack, EschatonCharge.EDGE_HIT)) {
            float extra = Math.max(50F, target.getMaxHealth() * 0.25F);
            target.invulnerableTime = 0;
            target.hurt(unmaking(attacker.level(), attacker), extra);
            if (attacker.level() instanceof ServerLevel sl)
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), target.getX(), target.getY(0.5), target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            if (!EschatonCharge.spendOrSay(player, stack, player.isShiftKeyDown() ? EschatonCharge.EDGE_STASIS : EschatonCharge.EDGE_ERASURE))
                return InteractionResultHolder.fail(stack);
            if (player.isShiftKeyDown()) {
                int n = ChronalStasis.freeze(sl, player, 24, 200);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.edge.stasis", n).withStyle(ChatFormatting.AQUA), true);
                player.getCooldowns().addCooldown(this, 100);
            } else {
                int n = erase(sl, player);
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.edge.erasure", n).withStyle(ChatFormatting.GOLD), true);
                player.getCooldowns().addCooldown(this, 40);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static int erase(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        // the cone itself, drawn in light
        com.theoreticalsciences.registry.QHGlowOptions gold = com.theoreticalsciences.registry.QHGlowOptions.of(1F, 0.86F, 0.4F, 1.4F);
        for (int d = 1; d <= ERASE_RANGE; d++) {
            Vec3 c = eye.add(look.scale(d));
            double spread = d * 0.35;
            level.sendParticles(gold, c.x, c.y, c.z, 3 + d / 2, spread, spread, spread, 0);
            if (d % 3 == 0) level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), c.x, c.y, c.z, 2, spread * 0.5, spread * 0.5, spread * 0.5, 0.02);
        }
        level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.SHOCKWAVE.get(), SoundSource.PLAYERS, 1.2F, 1.6F);
        level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
        int killed = 0;
        var box = player.getBoundingBox().inflate(ERASE_RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive())) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > ERASE_RANGE || dist < 1e-3) continue;
            if (to.normalize().dot(look) < 0.82) continue;               // about 35 degrees either side
            if (!mayUnmake(player, e)) continue;
            e.invulnerableTime = 0;
            e.hurt(unmaking(level, player), Math.max(1000F, e.getMaxHealth() * 10F));
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.SHOCKWAVE.get(), e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
            if (!e.isAlive()) killed++;
        }
        return killed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.omnipotent_edge.hit").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.omnipotent_edge.use").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.omnipotent_edge.sneak").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.omnipotent_edge.harvest").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
