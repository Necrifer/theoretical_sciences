package com.theoreticalsciences.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Scepter of Singularities. Use: put a singularity where you are looking (up to 64 blocks). Sneak-use: change which.
 * The mode is kept in the stack's custom data. Round 32: every singularity costs Eschaton Charge (see {@link Mode#cost});
 * an empty scepter does nothing.
 */
public class SingularityScepterItem extends Item implements EschatonPowered {
    public static final int MAX_CHARGE = 25_000;
    public enum Mode {
        BLACK_HOLE(50, 500), WHITE_HOLE(20, 300), TESSERACT(10, 150), ENTANGLEMENT(20, 300), BIG_BANG(80, 1500);
        final int cooldown;
        /** Round 32: Eschaton Charge per use. */
        public final int cost;

        Mode(int cooldown, int cost) {
            this.cooldown = cooldown;
            this.cost = cost;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting colour() {
            return switch (this) {
                case BLACK_HOLE -> ChatFormatting.DARK_PURPLE;
                case WHITE_HOLE -> ChatFormatting.WHITE;
                case TESSERACT -> ChatFormatting.AQUA;
                case ENTANGLEMENT -> ChatFormatting.LIGHT_PURPLE;
                case BIG_BANG -> ChatFormatting.GOLD;
            };
        }
    }

    private static final String MODE_KEY = "theoreticalsciences_scepter_mode";
    static final double RANGE = 64;

    public SingularityScepterItem(Properties p) {
        super(p.fireResistant());
    }

    public static Mode mode(ItemStack stack) {
        int i = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        Mode[] all = Mode.values();
        return all[Math.floorMod(i, all.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode next = Mode.values()[(mode(stack).ordinal() + 1) % Mode.values().length];
                CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt(MODE_KEY, next.ordinal()));
                player.displayClientMessage(Component.translatable("message.theoreticalsciences.scepter.mode",
                        Component.translatable("scepter.theoreticalsciences.mode." + next.key()).withStyle(next.colour(), ChatFormatting.BOLD)), true);
                level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CHIME.get(), SoundSource.PLAYERS, 1.0F, 0.6F + next.ordinal() * 0.2F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level instanceof ServerLevel sl) {
            Mode m = mode(stack);
            if (!EschatonCharge.active(player, stack) || (!EschatonCharge.free(player) && EschatonCharge.get(stack) < m.cost)) {
                EschatonCharge.spendOrSay(player, stack, m.cost);
                return InteractionResultHolder.fail(stack);
            }
            HitResult hit = aim(player, RANGE);
            Vec3 at = hit.getLocation();
            boolean ok = switch (m) {
                case BLACK_HOLE -> ScepterEffects.blackHole(sl, player, at);
                case WHITE_HOLE -> ScepterEffects.whiteHole(sl, player, at);
                case TESSERACT -> ScepterEffects.tesseract(sl, player, hit);
                case ENTANGLEMENT -> ScepterEffects.entangle(sl, player, at);
                case BIG_BANG -> ScepterEffects.bigBang(sl, player, at);
            };
            if (ok) {
                EschatonCharge.spend(player, stack, m.cost);
                player.getCooldowns().addCooldown(this, m.cooldown);
                // and the thing itself appears: a real black hole, white hole, tesseract...
                int ticks = switch (m) {
                    case BLACK_HOLE -> 60;
                    case WHITE_HOLE -> 36;
                    case TESSERACT -> 40;
                    case ENTANGLEMENT -> 40;
                    case BIG_BANG -> 50;
                };
                SingularityFxPayload.send(sl, at, m.ordinal(), ticks);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** What the player is looking at within range: the nearer of the first block and the first entity, or a point in the air. */
    static HitResult aim(Player player, double range) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle(), end = eye.add(look.scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 stop = eye.add(look.scale(reach));
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(player, eye, stop,
                player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0), (Entity e) -> !e.isSpectator() && e.isPickable() && e != player, reach * reach);
        if (ent != null) return ent;
        if (block.getType() == HitResult.Type.MISS) return BlockHitResult.miss(end, player.getDirection().getOpposite(), BlockPos.containing(end));
        return block;
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
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        Mode m = mode(stack);
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.singularity_scepter.mode",
                Component.translatable("scepter.theoreticalsciences.mode." + m.key()).withStyle(m.colour(), ChatFormatting.BOLD)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("scepter.theoreticalsciences.mode." + m.key() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.singularity_scepter.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
