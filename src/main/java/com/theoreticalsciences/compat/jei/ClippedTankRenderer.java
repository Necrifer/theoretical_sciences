package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.compat.TankDraw;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/** Round 38 (1.1.1): a JEI tank that can never draw outside its own box (see {@link TankDraw}). */
public final class ClippedTankRenderer implements IIngredientRenderer<FluidStack> {
    private final int width, height;
    private final long capacity;

    public ClippedTankRenderer(long capacity, int width, int height) {
        this.capacity = Math.max(1, capacity);
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(GuiGraphics g, FluidStack fs) {
        TankDraw.draw(g, fs, 0, 0, width, height, fs.getAmount() / (double) capacity);
    }

    @Override
    public List<Component> getTooltip(FluidStack fs, TooltipFlag flag) {
        List<Component> tip = new ArrayList<>();
        if (fs.isEmpty()) return tip;
        tip.add(fs.getHoverName());
        tip.add(Component.literal(String.format("%,d mB", fs.getAmount())).withStyle(ChatFormatting.GRAY));
        return tip;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
