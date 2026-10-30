package com.theoreticalsciences.machine.tuning;

import com.theoreticalsciences.item.LoreItem;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.recipe.QHRecipe;
import com.theoreticalsciences.registry.QHRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Laplace's Demon (creative only): "an intellect which knows all forces and all positions". Used on a multiblock
 * controller it puts the machine exactly where the recipe needs it - see {@link ControllerBlockEntity#demonTune}.
 * The recipe is the one its inputs currently match; or hold that recipe's product in the off hand to choose it;
 * otherwise the recipe it last ran.
 * It cannot be crafted or made by the Omnipotence Core, and does nothing outside creative mode.
 */
public class LaplaceDemonItem extends LoreItem {
    public LaplaceDemonItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_creative").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ControllerBlockEntity c)) {
            sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_nothing").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        QHRecipe target = pick(level, c, sp.getOffhandItem());
        Component what = target == null ? Component.translatable("message.theoreticalsciences.demon_generic")
                : target.itemOutputs.isEmpty() ? Component.literal("?") : target.itemOutputs.get(0).getHoverName();
        boolean physics = c.hasPhysics();
        boolean started = c.demonTune(level, target, res -> {
            if (!physics)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_tuned_simple").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            else if (res.violations() < 0)
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_failed").withStyle(ChatFormatting.RED), false);
            else if (res.ok())
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_tuned", what, String.format("%.2f", res.rate()),
                        res.coolant().isEmpty() ? Component.literal("-") : Component.translatable("fluid_type." + res.coolant().replace(':', '.'))).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            else
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_partial", what).withStyle(ChatFormatting.GOLD), false);
            if (target != null && !target.environment.isAny())
                sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_environment").withStyle(ChatFormatting.GRAY), false);
            level.playSound(null, c.getBlockPos(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.BLOCKS, 1F, 1.6F);
        });
        if (!started) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_busy").withStyle(ChatFormatting.GRAY), true);
        else if (physics) sp.displayClientMessage(Component.translatable("message.theoreticalsciences.demon_thinking", what).withStyle(ChatFormatting.DARK_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    private static QHRecipe pick(ServerLevel level, ControllerBlockEntity c, ItemStack want) {
        List<RecipeHolder<QHRecipe>> all = level.getRecipeManager().getAllRecipesFor(QHRecipes.type(c.type.recipeType));
        if (!want.isEmpty())
            for (RecipeHolder<QHRecipe> h : all)
                for (ItemStack o : h.value().itemOutputs) if (ItemStack.isSameItem(o, want)) return h.value();
        RecipeHolder<QHRecipe> m = c.matchingRecipe(level);
        if (m != null) return m.value();
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.creative_only").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.theoreticalsciences.laplaces_demon").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
