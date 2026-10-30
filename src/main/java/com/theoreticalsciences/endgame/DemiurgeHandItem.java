package com.theoreticalsciences.endgame;

import com.theoreticalsciences.transcend.AttunePayloads;
import com.theoreticalsciences.transcend.ForgedCoreBlockEntity;
import com.theoreticalsciences.transcend.TranscendRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Round 27: the Hand of the Demiurge - the one tool that can handle a forged Omnipotence Core. It has three modes
 * (sneak and use it in the air to switch):
 * <ul>
 *   <li><b>Tune</b>: use it on the Core to open its attunement (round 34c: 21 parameters across every branch).</li>
 *   <li><b>Activate</b>: wakes a Core that is in tune (or puts an awake one to sleep).</li>
 *   <li><b>Move</b>: lifts the Core out of the world into your inventory, to be placed somewhere else (and tuned again).</li>
 * </ul>
 * Round 32: each action on a Core costs 250 Eschaton Charge (switching mode is free).
 */
public class DemiurgeHandItem extends Item implements EschatonPowered {
    public static final int MAX_CHARGE = 5_000;
    public enum Mode {TUNE, ACTIVATE, MOVE}

    public DemiurgeHandItem(Properties p) {
        super(p.stacksTo(1).fireResistant());
    }

    public static Mode mode(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        @SuppressWarnings("deprecation") int m = cd == null ? 0 : cd.getUnsafe().getInt("Mode");
        return Mode.values()[Math.floorMod(m, Mode.values().length)];
    }

    private static void setMode(ItemStack s, Mode m) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putInt("Mode", m.ordinal()));
    }

    private static Component modeName(Mode m) {
        return Component.translatable("item.theoreticalsciences.demiurge_hand.mode." + m.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(s);
        if (!level.isClientSide) {
            Mode next = Mode.values()[(mode(s).ordinal() + 1) % Mode.values().length];
            setMode(s, next);
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.demiurge_hand.mode", modeName(next)).withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.PLAYERS, 0.8F, 0.6F + next.ordinal() * 0.3F);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (level.getBlockState(pos).is(TranscendRegistry.CORE.get())) {
            if (ctx.getPlayer() != null && !level.isClientSide)
                ctx.getPlayer().displayClientMessage(Component.translatable("message.theoreticalsciences.demiurge_hand.original").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof ForgedCoreBlockEntity core)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel sl) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        // Round 29: respect spawn protection, adventure mode and claim mods - the Hand may only touch a Core the player
        // would be allowed to touch with their bare hands
        if (!sl.mayInteract(sp, pos) || !sp.mayUseItemAt(pos, ctx.getClickedFace(), ctx.getItemInHand())
                || sp.blockActionRestricted(sl, pos, sp.gameMode.getGameModeForPlayer())) return InteractionResult.FAIL;
        Mode m = mode(ctx.getItemInHand());
        if (!EschatonCharge.active(sp, ctx.getItemInHand()) || (!EschatonCharge.free(sp) && EschatonCharge.get(ctx.getItemInHand()) < EschatonCharge.HAND_ACTION)) {
            EschatonCharge.spendOrSay(sp, ctx.getItemInHand(), EschatonCharge.HAND_ACTION);
            return InteractionResult.FAIL;
        }
        // lifting a Core out is like breaking it: ask protection mods first (they cancel this event to forbid it)
        if (m == Mode.MOVE && net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(sl, pos, sl.getBlockState(pos), sp)).isCanceled()) return InteractionResult.FAIL;
        EschatonCharge.spend(sp, ctx.getItemInHand(), EschatonCharge.HAND_ACTION);
        switch (m) {
            case TUNE -> {
                // round 34c: attuning is a screen of its own (transcend/ForgedCoreBlockEntity has the laws)
                AttunePayloads.send(sp, core, true);
                sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.BLOCKS, 1F, 0.5F + 1.5F * core.match());
            }
            case ACTIVATE -> {
                if (core.active()) {
                    core.setActive(false);
                    sp.displayClientMessage(Component.translatable("message.theoreticalsciences.forged_core.sleeps").withStyle(ChatFormatting.GRAY), true);
                    sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_DOWN.get(), SoundSource.BLOCKS, 1F, 0.6F);
                } else wakeNow(sp, sl, pos, core);
            }
            case MOVE -> {
                // the only way a forged Core leaves the world: lifted out whole, dormant (its next place has other laws)
                sl.removeBlock(pos, false);
                ItemStack item = new ItemStack(TranscendRegistry.FORGED_CORE_ITEM.get());
                if (!sp.getInventory().add(item)) sp.drop(item, false);
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.forged_core.lifted").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.BLOCKS, 1F, 0.5F);
                sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHASE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 120, 0.4, 0.4, 0.4, 0.2);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** The attunement screen's Wake button: costs one action, like the Activate mode. */
    public static void wake(ServerPlayer sp, ForgedCoreBlockEntity core, ItemStack hand) {
        if (core.active() || !(sp.level() instanceof ServerLevel sl)) return;
        if (!EschatonCharge.active(sp, hand) || !EschatonCharge.spendOrSay(sp, hand, EschatonCharge.HAND_ACTION)) return;
        wakeNow(sp, sl, core.getBlockPos(), core);
    }

    private static void wakeNow(ServerPlayer sp, ServerLevel sl, BlockPos pos, ForgedCoreBlockEntity core) {
        if (core.inTune()) {
            core.setActive(true);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.forged_core.wakes").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.BLOCKS, 1.5F, 0.5F);
            sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.OMNIPOTENCE_GRANT.get(), SoundSource.BLOCKS, 1F, 0.7F);
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 200, 0.4, 0.4, 0.4, 0.4);
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.FLASH.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        } else {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.forged_core.not_in_tune", Math.round(core.match() * 100)).withStyle(ChatFormatting.RED), true);
            sl.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.SHATTER.get(), SoundSource.BLOCKS, 1F, 0.5F);
        }
    }

    @Override
    public void appendHoverText(ItemStack s, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("message.theoreticalsciences.demiurge_hand.mode", modeName(mode(s))).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.theoreticalsciences.demiurge_hand.help." + mode(s).name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.theoreticalsciences.demiurge_hand.switch").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return EschatonCharge.get(s) > 0;
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
}
