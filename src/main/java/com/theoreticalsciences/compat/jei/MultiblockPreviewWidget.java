package com.theoreticalsciences.compat.jei;

import com.theoreticalsciences.compat.MultiblockView;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** JEI's side of the structure viewer ({@link MultiblockView} does the work). */
public class MultiblockPreviewWidget implements IRecipeWidget, IJeiGuiEventListener {
    private final MultiblockView view;
    private final int x0, y0, w, h;

    public MultiblockPreviewWidget(MultiblockDefinition def, int x0, int y0, int w, int h) {
        this.view = new MultiblockView(def, x0, y0, w, h);
        this.x0 = x0;
        this.y0 = y0;
        this.w = w;
        this.h = h;
    }

    @Override
    public ScreenPosition getPosition() {
        return new ScreenPosition(0, 0);
    }

    @Override
    public ScreenRectangle getScreenRectangle() {
        return new ScreenRectangle(x0, y0, w, h + 14);
    }

    @Override
    public ScreenRectangle getArea() {
        // JEI subtracts this rectangle's corner from every mouse position before calling us. Anchoring it at the
        // recipe origin keeps the coordinates identical to the ones used for drawing.
        return new ScreenRectangle(0, 0, x0 + w, y0 + h + 14);
    }

    @Override
    public void drawWidget(GuiGraphics g, double mx, double my) {
        view.draw(g, mx, my);
    }

    @Override
    public void getTooltip(ITooltipBuilder tip, double mx, double my) {
        List<Component> l = new ArrayList<>();
        view.tooltip(l, mx, my);
        tip.addAll(l);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return view.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return view.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return view.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        return view.mouseScrolled(mx, my, sx, sy);
    }
}
