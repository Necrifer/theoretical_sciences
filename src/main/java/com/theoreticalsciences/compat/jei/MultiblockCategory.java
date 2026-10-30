package com.theoreticalsciences.compat.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineBlock;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.registry.QHItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/** Rotating 3D preview of every multiblock with its bill of materials. */
public class MultiblockCategory implements IRecipeCategory<MultiblockDefinition> {
    private static final int W = 176, H = 226, PREVIEW_H = 132, BOM_Y = 170;
    private final IDrawable icon;

    public MultiblockCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(QHItems.get("interface_port")));
    }

    @Override
    public RecipeType<MultiblockDefinition> getRecipeType() {
        return QHJeiPlugin.MULTIBLOCKS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.theoreticalsciences.multiblocks");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return W;
    }

    @Override
    public int getHeight() {
        return H;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, MultiblockDefinition def, IFocusGroup focuses) {
        Map<Block, Integer> bom = def.billOfMaterials();
        int i = 0;
        for (Map.Entry<Block, Integer> e : bom.entrySet()) {
            ItemStack s = new ItemStack(e.getKey().asItem(), e.getValue());
            if (s.isEmpty()) continue;
            if (i >= 27) break;
            int x = 1 + (i % 9) * 19, y = BOM_Y + (i / 9) * 18;
            boolean controller = e.getKey() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(def.controllerBlock);
            b.addSlot(controller ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT, x, y).setStandardSlotBackground().addItemStack(s);
            i++;
        }
    }

    @Override
    public void createRecipeExtras(mezz.jei.api.gui.widgets.IRecipeExtrasBuilder builder, MultiblockDefinition def, IFocusGroup focuses) {
        MultiblockPreviewWidget preview = new MultiblockPreviewWidget(def, 0, 10, W, PREVIEW_H);
        builder.addWidget(preview);
        builder.addGuiEventListener(preview);
    }

    @Override
    public void draw(MultiblockDefinition def, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        Component name = Component.translatable("block." + def.controllerBlock.getNamespace() + "." + def.controllerBlock.getPath());
        g.drawString(font, name, (W - font.width(name)) / 2, 0, 0x40A0C0, false);
        int blocks = def.billOfMaterials().values().stream().mapToInt(Integer::intValue).sum();
        Component size = Component.translatable("jei.theoreticalsciences.mb_size", def.sizeX, def.sizeY, def.sizeZ, blocks);
        g.drawString(font, size, (W - font.width(size)) / 2, BOM_Y - 10, 0x707888, false);
    }

    @Override
    public ResourceLocation getRegistryName(MultiblockDefinition def) {
        return TheoreticalSciences.id("multiblock/" + def.id);
    }
}
