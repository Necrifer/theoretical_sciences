package com.theoreticalsciences.compat.emi;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A multiblock structure: the controller's name, the rotating 3D view the JEI page has (round 35; drag to turn,
 * shift-drag or right-drag to pan, - / + to zoom, arrows for one layer at a time, hover a block to see what goes there),
 * its size, and the bill of materials as ordinary slots (so looking up a block finds the structures that use it).
 */
public class MultiblockEmiRecipe extends BasicEmiRecipe {
    private static final int W = 176, H = 226, VIEW_Y = 10, VIEW_H = 132, BOM_Y = 170;
    private final MultiblockDefinition def;
    private final List<EmiStack> bom = new ArrayList<>();
    private final List<Boolean> isController = new ArrayList<>();

    public MultiblockEmiRecipe(EmiRecipeCategory cat, MultiblockDefinition def) {
        super(cat, QHEmi.synthetic("multiblock/" + def.id), W, H);
        this.def = def;
        Block controller = BuiltInRegistries.BLOCK.get(def.controllerBlock);
        for (Map.Entry<Block, Integer> e : def.billOfMaterials().entrySet()) {
            ItemStack s = new ItemStack(e.getKey().asItem());
            if (s.isEmpty()) continue;
            EmiStack es = EmiStack.of(s, e.getValue());
            boolean c = e.getKey() == controller;
            bom.add(es);
            isController.add(c);
            // the controller is what the page is "for"; every other block is something it needs
            if (c) outputs.add(EmiStack.of(s));
            else inputs.add(es);
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;       // a structure is not crafted, so it has no place in a crafting tree
    }

    @Override
    public void addWidgets(WidgetHolder w) {
        var font = Minecraft.getInstance().font;
        Component name = Component.translatable("block." + def.controllerBlock.getNamespace() + "." + def.controllerBlock.getPath());
        w.addText(name, (W - font.width(name)) / 2, 0, 0xFF40A0C0, false);
        w.add(new View3D(def, 0, VIEW_Y, W, VIEW_H));
        int blocks = def.billOfMaterials().values().stream().mapToInt(Integer::intValue).sum();
        Component size = Component.translatable("jei.theoreticalsciences.mb_size", def.sizeX, def.sizeY, def.sizeZ, blocks);
        w.addText(size, (W - font.width(size)) / 2, BOM_Y - 10, 0xFF707888, false);
        for (int i = 0; i < bom.size() && i < 27; i++) {
            var slot = w.addSlot(bom.get(i), (i % 9) * 19, BOM_Y + (i / 9) * 18);
            if (isController.get(i)) slot.recipeContext(this);
        }
    }

    /**
     * Round 35: the same rotating 3D view as the JEI page ({@link com.theoreticalsciences.compat.MultiblockView}). EMI only
     * tells a widget about clicks, so a drag is followed here by watching the held mouse button each frame, and zoom
     * has its own - / + buttons instead of the scroll wheel.
     */
    static final class View3D extends Widget {
        private final com.theoreticalsciences.compat.MultiblockView view;
        private final int x0, y0, w, h;
        private int lastX, lastY;

        View3D(MultiblockDefinition def, int x0, int y0, int w, int h) {
            this.view = new com.theoreticalsciences.compat.MultiblockView(def, x0, y0, w, h);
            this.view.zoomButtons = true;
            this.x0 = x0;
            this.y0 = y0;
            this.w = w;
            this.h = h;
        }

        @Override
        public Bounds getBounds() {
            return new Bounds(x0, y0, w, h + 14);
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float delta) {
            int b = view.dragging();
            if (b >= 0) {
                long win = Minecraft.getInstance().getWindow().getWindow();
                if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, b) != org.lwjgl.glfw.GLFW.GLFW_PRESS) view.mouseReleased(mx, my, b);
                else if (mx != lastX || my != lastY) view.mouseDragged(mx, my, b, mx - lastX, my - lastY);
            }
            lastX = mx;
            lastY = my;
            view.draw(g, mx, my);
        }

        @Override
        public List<ClientTooltipComponent> getTooltip(int mx, int my) {
            if (view.dragging() >= 0) return List.of();
            List<Component> tip = new ArrayList<>();
            view.tooltip(tip, mx, my);
            List<ClientTooltipComponent> out = new ArrayList<>();
            for (Component c : tip) out.add(ClientTooltipComponent.create(c.getVisualOrderText()));
            return out;
        }

        @Override
        public boolean mouseClicked(int mx, int my, int button) {
            lastX = mx;
            lastY = my;
            return view.mouseClicked(mx, my, button);
        }
    }
}
