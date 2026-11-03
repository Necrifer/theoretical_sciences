package com.theoreticalsciences.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Round 38 (1.1.1): draws a recipe viewer's fluid tank ourselves, the same way in JEI and EMI. Every piece of the fluid is
 * cut to the tank's inside (16 pixel tiles, the last one cropped, never stretched), and a scissor around the tank stops
 * anything at all from being drawn outside it - whatever size another mod's fluid picture (or a borrowed Essence look) has.
 */
public final class TankDraw {
    private TankDraw() {}

    /** Fills the box x, y, w, h (in the current pose) from the bottom, fraction 0..1 of it. */
    public static void draw(GuiGraphics g, FluidStack fs, int x, int y, int w, int h, double fraction) {
        if (fs == null || fs.isEmpty() || w <= 0 || h <= 0) return;
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs.getFluid());
        ResourceLocation tex = ext.getStillTexture(fs);
        if (tex == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
        int tint = ext.getTintColor(fs);
        float a = ((tint >>> 24) & 255) / 255F, r = ((tint >> 16) & 255) / 255F, gr = ((tint >> 8) & 255) / 255F, b = (tint & 255) / 255F;
        if (a <= 0) a = 1;
        int fill = (int) Math.max(1, Math.min(h, Math.round(h * Math.max(0, Math.min(1, fraction)))));

        Matrix4f m = g.pose().last().pose();
        Vector3f p0 = m.transformPosition(x, y, 0, new Vector3f()), p1 = m.transformPosition(x + w, y + h, 0, new Vector3f());
        g.flush();
        g.enableScissor((int) Math.floor(Math.min(p0.x, p1.x)), (int) Math.floor(Math.min(p0.y, p1.y)),
                (int) Math.ceil(Math.max(p0.x, p1.x)), (int) Math.ceil(Math.max(p0.y, p1.y)));
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        int bottom = y + h;
        for (int dy = 0; dy < fill; dy += 16) {
            int th = Math.min(16, fill - dy);
            float y1 = bottom - dy, y0 = y1 - th;
            float tv0 = v1 - (v1 - v0) * th / 16F;                     // the tile's lower part, so the surface is the picture's
            for (int dx = 0; dx < w; dx += 16) {
                int tw = Math.min(16, w - dx);
                float x0 = x + dx, x1 = x0 + tw, tu1 = u0 + (u1 - u0) * tw / 16F;
                bb.addVertex(m, x0, y1, 0).setUv(u0, v1).setColor(r, gr, b, a);
                bb.addVertex(m, x1, y1, 0).setUv(tu1, v1).setColor(r, gr, b, a);
                bb.addVertex(m, x1, y0, 0).setUv(tu1, tv0).setColor(r, gr, b, a);
                bb.addVertex(m, x0, y0, 0).setUv(u0, tv0).setColor(r, gr, b, a);
            }
        }
        var mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.disableBlend();
        g.disableScissor();
    }
}
