package com.theoreticalsciences.item;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Gains Insight by observing phenomena. Each distinct block in {@code #theoreticalsciences:observable},
 * entity in {@code #theoreticalsciences:observable}, running machine type or dimension can be observed once.
 */
public class SpectralAnalyzerItem extends LoreItem {
    public static final TagKey<Block> OBSERVABLE_BLOCKS = TagKey.create(Registries.BLOCK, TheoreticalSciences.id("observable"));
    public static final TagKey<EntityType<?>> OBSERVABLE_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, TheoreticalSciences.id("observable"));

    public SpectralAnalyzerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        BlockState state = level.getBlockState(pos);
        String key;
        int insight;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            if (!be.isActive()) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.analyzer_idle"), true);
                return InteractionResult.CONSUME;
            }
            key = "machine:" + be.type.id();
            insight = be.type.isMultiblock() ? 12 : 4;
        } else if (state.is(OBSERVABLE_BLOCKS)) {
            key = "block:" + BuiltInRegistries.BLOCK.getKey(state.getBlock());
            insight = 3;
        } else {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.analyzer_nothing"), true);
            return InteractionResult.CONSUME;
        }
        report(sp, key, insight, state.getBlock().getName(), pos.getCenter().x, pos.getCenter().y, pos.getCenter().z);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!target.getType().is(OBSERVABLE_ENTITIES)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.analyzer_nothing"), true);
            return InteractionResult.CONSUME;
        }
        report(sp, "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), 6, target.getName(), target.getX(), target.getEyeY(), target.getZ());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Observing the sky of a dimension
        if (player instanceof ServerPlayer sp && player.isShiftKeyDown()) {
            String dim = level.dimension().location().toString();
            report(sp, "dimension:" + dim, 8, Component.literal(dim), player.getX(), player.getEyeY() + 1, player.getZ());
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private static void report(ServerPlayer sp, String key, int insight, Component name, double x, double y, double z) {
        ServerLevel sl = sp.serverLevel();
        if (ResearchService.observe(sp, key, insight)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.analyzer_new", name, insight).withStyle(ChatFormatting.AQUA), false);
            sl.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), x, y, z, 24, 0.4, 0.4, 0.4, 0.04);
            sl.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.RESONATE.get(), SoundSource.PLAYERS, 1.0F, 1.4F);
        } else {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.analyzer_known", name).withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.analyzer_usage").withStyle(ChatFormatting.GRAY));
    }
}
