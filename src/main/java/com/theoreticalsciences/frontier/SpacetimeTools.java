package com.theoreticalsciences.frontier;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.api.event.MachineRecipeEvent;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * 1.12.0: the General Relativity and Geography tools of the expansion.
 * <ul>
 *   <li><b>Warp Bubble Core</b> - one return trip. Used, it anchors the spot you stand on and becomes an <b>Anchored Warp
 *   Bubble</b>, a separate item that remembers that spot; using that carries you back there and is spent. Recipes only ever
 *   take the plain core, which holds no data, so an anchored one is never mistaken for it.</li>
 *   <li><b>Climate Engine</b> weather: its rain, clear-sky and storm recipes set the weather when they finish (Minecraft's
 *   weather is the whole world's).</li>
 * </ul>
 */
@EventBusSubscriber(modid = TheoreticalSciences.MODID)
public final class SpacetimeTools {
    public static class WarpBubbleCore extends Item {
        public WarpBubbleCore(Properties p) {
            super(p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            ItemStack anchored = new ItemStack(QHItems.get("anchored_warp_bubble"));
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", level.dimension().location().toString());
            tag.putDouble("X", sp.getX());
            tag.putDouble("Y", sp.getY());
            tag.putDouble("Z", sp.getZ());
            anchored.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            st.shrink(1);
            if (!sp.getInventory().add(anchored)) sp.drop(anchored, false);
            level.playSound(null, sp.blockPosition(), com.theoreticalsciences.registry.QHSounds.POWER_UP.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.warp_anchored").withStyle(ChatFormatting.AQUA), true);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable("tooltip.theoreticalsciences.warp_bubble_core").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }

    public static class AnchoredWarpBubble extends Item {
        public AnchoredWarpBubble(Properties p) {
            super(p.stacksTo(1));
        }

        static CompoundTag anchor(ItemStack st) {
            CustomData d = st.get(DataComponents.CUSTOM_DATA);
            return d == null ? null : d.copyTag();
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(st);
            CompoundTag a = anchor(st);
            if (a == null || !a.contains("Dim")) return InteractionResultHolder.fail(st);
            ResourceLocation dim = ResourceLocation.tryParse(a.getString("Dim"));
            ServerLevel target = dim == null ? null : sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (target == null) {
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.warp_lost").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(st);
            }
            Vec3 to = new Vec3(a.getDouble("X"), a.getDouble("Y"), a.getDouble("Z"));
            target.getChunk(BlockPos.containing(to));
            st.shrink(1);
            sp.stopRiding();
            sp.changeDimension(new DimensionTransition(target, to, Vec3.ZERO, sp.getYRot(), sp.getXRot(), DimensionTransition.DO_NOTHING));
            sp.fallDistance = 0;
            target.playSound(null, BlockPos.containing(to), com.theoreticalsciences.registry.QHSounds.PHASE_SHIFT.get(), SoundSource.PLAYERS, 0.6F, 1.4F);
            return InteractionResultHolder.consume(st);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return true;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            CompoundTag a = anchor(stack);
            if (a != null && a.contains("Dim"))
                tip.add(Component.translatable("tooltip.theoreticalsciences.anchored_warp_bubble", (int) a.getDouble("X"), (int) a.getDouble("Y"),
                        (int) a.getDouble("Z"), a.getString("Dim")).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @SubscribeEvent
    public static void onRecipe(MachineRecipeEvent.Finish e) {
        String path = e.getRecipeId().getPath();
        if (!path.startsWith("r53_weather_") || !(e.getMachine().getLevel() instanceof ServerLevel sl)) return;
        int t = 12000;
        switch (path) {
            case "r53_weather_clear" -> sl.setWeatherParameters(t, 0, false, false);
            case "r53_weather_rain" -> sl.setWeatherParameters(0, t, true, false);
            case "r53_weather_storm" -> sl.setWeatherParameters(0, t, true, true);
            default -> {}
        }
    }

    private SpacetimeTools() {}
}
