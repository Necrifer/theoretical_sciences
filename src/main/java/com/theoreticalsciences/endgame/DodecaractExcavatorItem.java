package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;
import java.util.Locale;

/**
 * Round 32: the Dodecaract Excavator - a pickaxe shaped like a twelve-dimensional cube. It mines whatever a netherite
 * pickaxe can, very fast, and never wears out; instead it spends Eschaton Charge (5 per block, 20 per ore extracted).
 * <p>Three modes (sneak and use to switch; sneak and scroll to change the reach, from 3x3x3 up to 33x33x33):
 * <ul>
 *   <li><b>Single</b>: an ordinary (very fast) pickaxe.</li>
 *   <li><b>Area</b>: breaking a block also breaks everything in a cube of the chosen reach, reaching into the wall
 *   from the face you mined (drops go straight to your inventory).</li>
 *   <li><b>Ore Extraction</b>: use it on a block (or in the air: around yourself) and every ore in the cube around it
 *   is pulled out into your inventory - with Fortune or Silk Touch if the tool has them - and replaced with the stone
 *   it grew in. The rest of the rock is left alone.</li>
 * </ul>
 * The big jobs are spread over several ticks (see {@link ExcavatorJobs}).
 */
public class DodecaractExcavatorItem extends PickaxeItem implements EschatonPowered {
    public enum Mode {SINGLE, AREA, EXTRACT}

    public static final int MAX_CHARGE = 50_000, MIN_RADIUS = 1, MAX_RADIUS = 16, DEFAULT_RADIUS = 2;

    static final Tier TIER = new Tier() {
        @Override public int getUses() { return 1_000_000; }      // must be positive; the tool is Unbreakable anyway
        @Override public float getSpeed() { return 64F; }
        @Override public float getAttackDamageBonus() { return 0F; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
        @Override public int getEnchantmentValue() { return 30; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    };

    public DodecaractExcavatorItem(Properties p) {
        super(TIER, p.stacksTo(1).fireResistant().component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .attributes(DiggerItem.createAttributes(TIER, 8F, -2.6F)));
    }

    @Override
    public int maxCharge() {
        return MAX_CHARGE;
    }

    // ---- settings, kept on the stack
    public static Mode mode(ItemStack s) {
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mode.values()[Math.floorMod(t.getInt("Mode"), Mode.values().length)];
    }

    public static int radius(ItemStack s) {
        CompoundTag t = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return t.contains("Radius") ? Mth.clamp(t.getInt("Radius"), MIN_RADIUS, MAX_RADIUS) : DEFAULT_RADIUS;
    }

    static Component modeName(Mode m) {
        return Component.translatable("item.theoreticalsciences.dodecaract_excavator.mode." + m.name().toLowerCase(Locale.ROOT));
    }

    static Component size(int r) {
        int d = 2 * r + 1;
        return Component.literal(d + "×" + d + "×" + d);
    }

    /** Server: the player scrolled while sneaking. */
    public static void changeRadius(ServerPlayer sp, int delta) {
        ItemStack s = sp.getMainHandItem();
        if (!(s.getItem() instanceof DodecaractExcavatorItem) || delta == 0) return;
        int r = Mth.clamp(radius(s) + Integer.signum(delta), MIN_RADIUS, MAX_RADIUS);
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt("Radius", r));
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.radius", size(r)).withStyle(ChatFormatting.AQUA), true);
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.PLAYERS, 0.5F, 0.5F + r / 12F);
    }

    private static void cycleMode(Player player, ItemStack s) {
        Mode next = Mode.values()[(mode(s).ordinal() + 1) % Mode.values().length];
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt("Mode", next.ordinal()));
        player.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.mode", modeName(next), size(radius(s))).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.PLAYERS, 0.8F, 0.6F + next.ordinal() * 0.3F);
    }

    // ---- use: sneaking switches mode; in Ore Extraction, use starts a scan
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack s = ctx.getItemInHand();
        if (player.isShiftKeyDown()) {
            if (!ctx.getLevel().isClientSide) cycleMode(player, s);
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        if (mode(s) != Mode.EXTRACT || ctx.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) startExtraction(sp, s, ctx.getClickedPos());
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycleMode(player, s);
            return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        }
        if (mode(s) != Mode.EXTRACT || hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(s);
        if (player instanceof ServerPlayer sp) startExtraction(sp, s, sp.blockPosition());     // in the air: around yourself
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    private static void startExtraction(ServerPlayer sp, ItemStack s, BlockPos centre) {
        if (!EschatonCharge.active(sp, s)) {
            EschatonCharge.sayDormant(sp, s);
            return;
        }
        if (ExcavatorJobs.busy(sp)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.busy").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int r = radius(s);
        ExcavatorJobs.start(sp, centre, r, true, null);
        sp.displayClientMessage(Component.translatable("message.theoreticalsciences.excavator.scanning", size(r)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        sp.level().playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.6F, 1.8F);
    }

    // ---- mining
    @Override
    public float getDestroySpeed(ItemStack s, BlockState state) {
        return EschatonCharge.get(s) <= 0 ? 1F : super.getDestroySpeed(s, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack s, BlockState state) {
        return EschatonCharge.get(s) > 0 && super.isCorrectToolForDrops(s, state);
    }

    @Override
    public boolean mineBlock(ItemStack s, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0F) EschatonCharge.drain(miner, s, EschatonCharge.EXCAVATOR_BLOCK);
        return true;
    }

    /**
     * Area mode: a block broken by hand (survival or creative) sets off the rest of the cube. The cube reaches into
     * the wall: its near face is the layer of the block you broke. Breaks made by the Excavator's own jobs are ignored.
     */
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || ExcavatorJobs.working() || !(e.getPlayer() instanceof ServerPlayer sp)) return;
        ItemStack s = sp.getMainHandItem();
        if (!(s.getItem() instanceof DodecaractExcavatorItem) || mode(s) != Mode.AREA || !EschatonCharge.active(sp, s)) return;
        if (ExcavatorJobs.busy(sp)) return;
        int r = radius(s);
        Vec3 look = sp.getLookAngle();
        Direction into = Direction.getNearest(look.x, look.y, look.z);
        BlockPos centre = e.getPos().relative(into, r);
        ExcavatorJobs.start(sp, centre, r, false, e.getPos().immutable());
    }

    // ---- looks
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

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        Mode m = mode(s);
        tip.add(Component.translatable("message.theoreticalsciences.excavator.mode", modeName(m), size(radius(s))).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.theoreticalsciences.dodecaract_excavator.help." + m.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.theoreticalsciences.dodecaract_excavator.switch").withStyle(ChatFormatting.DARK_GRAY));
    }
}
