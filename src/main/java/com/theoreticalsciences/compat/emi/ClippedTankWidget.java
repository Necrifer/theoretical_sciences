package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.compat.TankDraw;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TankWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Round 38 (1.1.1): an EMI tank that can never draw outside its own box (see {@link TankDraw}). */
public class ClippedTankWidget extends TankWidget {
    private final long capacity;

    public ClippedTankWidget(EmiIngredient stack, int x, int y, int w, int h, long capacity) {
        super(stack, x, y, w, h, capacity);
        this.capacity = Math.max(1, capacity);
    }

    /**
     * 1.1.1: EMI draws a tank's frame as one 18x18 slot whatever the tank's size, so a tall tank had a short frame at its
     * top and the fluid seemed to run out of it. The frame is drawn here at the tank's full size, like a slot.
     */
    @Override
    public void drawBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        var b = getBounds();
        int x = b.x(), y = b.y(), w = b.width(), h = b.height();
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        g.fill(x, y, x + w - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h - 1, 0xFF373737);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y + 1, x + w, y + h, 0xFFFFFFFF);
    }

    @Override
    public void drawStack(GuiGraphics g, int mouseX, int mouseY, float delta) {
        EmiIngredient ing = getStack();
        List<EmiStack> stacks = ing.getEmiStacks();
        if (stacks.isEmpty()) return;
        EmiStack s = stacks.get((int) ((System.currentTimeMillis() / 1000) % stacks.size()));    // a tag cycles, one at a time
        if (!(s.getKey() instanceof Fluid fluid)) return;
        int amount = (int) Math.max(1, Math.min(Integer.MAX_VALUE, ing.getAmount()));
        FluidStack fs = new FluidStack(fluid.builtInRegistryHolder(), amount, s.getComponentChanges());
        var b = getBounds();
        TankDraw.draw(g, fs, b.x() + 1, b.y() + 1, b.width() - 2, b.height() - 2, ing.getAmount() / (double) capacity);
    }
}
