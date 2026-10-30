package com.theoreticalsciences.client.transit;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * A tiny 2D "pen" for drawing procedural shapes on the screen: coloured quads, lines of any angle and thickness,
 * rings, discs and polygons. Everything drawn between {@link #begin} and {@link #end} goes to the graphics card in a
 * single batch, so even a few thousand shapes per frame are cheap.
 *
 * <p>Three ways of mixing with what is already on screen:
 * <ul>
 *   <li>{@link Blend#NORMAL}: ordinary see-through paint.</li>
 *   <li>{@link Blend#ADD}: light - colours add up and glow (used for beams, rings, sparks).</li>
 *   <li>{@link Blend#INVERT}: white turns whatever is underneath into its photographic negative (the Antiverse).</li>
 * </ul>
 * Colours are 0xRRGGBB plus a separate 0..1 opacity, which is easier to animate.
 */
public final class Fx2D {
    public enum Blend { NORMAL, ADD, INVERT }

    private final Matrix4f m;
    private final BufferBuilder bb;
    private boolean any;

    private Fx2D(GuiGraphics g) {
        this.m = g.pose().last().pose();
        this.bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    /** Starts a batch. Anything the game had queued for the screen is drawn first, so our shapes go on top of it. */
    public static Fx2D begin(GuiGraphics g, Blend blend) {
        g.flush();
        RenderSystem.enableBlend();
        switch (blend) {
            case NORMAL -> RenderSystem.defaultBlendFunc();
            case ADD -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            // result = colour * (1 - screen) + screen * (1 - colour): white inverts, black leaves it alone
            case INVERT -> RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        }
        RenderSystem.disableCull();        // shapes may wind either way round
        RenderSystem.disableDepthTest();   // always on top
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        return new Fx2D(g);
    }

    /** Draws the batch and puts the drawing state back the way the rest of the game expects it. */
    public void end() {
        MeshData mesh = bb.build();
        if (mesh != null && any) BufferUploader.drawWithShader(mesh);
        else if (mesh != null) mesh.close();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    // ------------------------------------------------------------------ colour helpers

    public static int lerpRgb(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int gg = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (gg << 8) | bl;
    }

    private void v(float x, float y, int rgb, float a) {
        int al = (int) (Math.max(0, Math.min(1, a)) * 255);
        bb.addVertex(m, x, y, 0).setColor((al << 24) | (rgb & 0xFFFFFF));
        any = true;
    }

    // ------------------------------------------------------------------ shapes

    /** Any four-cornered shape, each corner with its own colour/opacity. */
    public void quad(float x1, float y1, int c1, float a1, float x2, float y2, int c2, float a2,
                     float x3, float y3, int c3, float a3, float x4, float y4, int c4, float a4) {
        v(x1, y1, c1, a1);
        v(x2, y2, c2, a2);
        v(x3, y3, c3, a3);
        v(x4, y4, c4, a4);
    }

    /** A triangle (a quad with two corners in the same place). */
    public void tri(float x1, float y1, int c1, float a1, float x2, float y2, int c2, float a2, float x3, float y3, int c3, float a3) {
        quad(x1, y1, c1, a1, x2, y2, c2, a2, x3, y3, c3, a3, x3, y3, c3, a3);
    }

    public void rect(float x0, float y0, float x1, float y1, int rgb, float a) {
        quad(x0, y0, rgb, a, x0, y1, rgb, a, x1, y1, rgb, a, x1, y0, rgb, a);
    }

    /** A rectangle fading from one colour at the top to another at the bottom. */
    public void rectV(float x0, float y0, float x1, float y1, int top, float aTop, int bot, float aBot) {
        quad(x0, y0, top, aTop, x0, y1, bot, aBot, x1, y1, bot, aBot, x1, y0, top, aTop);
    }

    /** A rectangle fading from left to right. */
    public void rectH(float x0, float y0, float x1, float y1, int left, float aLeft, int right, float aRight) {
        quad(x0, y0, left, aLeft, x0, y1, left, aLeft, x1, y1, right, aRight, x1, y0, right, aRight);
    }

    /** A straight line of the given thickness; the colour may change from one end to the other. */
    public void line(float x0, float y0, float x1, float y1, float width, int c0, float a0, int c1, float a1) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4F) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        quad(x0 - nx, y0 - ny, c0, a0, x0 + nx, y0 + ny, c0, a0, x1 + nx, y1 + ny, c1, a1, x1 - nx, y1 - ny, c1, a1);
    }

    public void line(float x0, float y0, float x1, float y1, float width, int rgb, float a) {
        line(x0, y0, x1, y1, width, rgb, a, rgb, a);
    }

    /** A line with a soft glow around it (a wide faint stroke under a thin bright one). Best with ADD. */
    public void glowLine(float x0, float y0, float x1, float y1, float width, int rgb, float a) {
        line(x0, y0, x1, y1, width * 4, rgb, a * 0.18F);
        line(x0, y0, x1, y1, width, rgb, a);
    }

    /**
     * A band between two radii around (cx, cy), from angle a0 sweeping {@code sweep} radians, squashed vertically by
     * {@code squash} (1 = circle, less = ellipse seen at an angle). Inner and outer edge can have different colours.
     */
    public void band(float cx, float cy, float rIn, float rOut, float squash, float a0, float sweep, int seg,
                     int cIn, float aIn, int cOut, float aOut) {
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + sweep * i / seg, t1 = a0 + sweep * (i + 1) / seg;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0) * squash, c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1) * squash;
            quad(cx + c0 * rIn, cy + s0 * rIn, cIn, aIn, cx + c0 * rOut, cy + s0 * rOut, cOut, aOut,
                    cx + c1 * rOut, cy + s1 * rOut, cOut, aOut, cx + c1 * rIn, cy + s1 * rIn, cIn, aIn);
        }
    }

    /** A thin circle outline. */
    public void ring(float cx, float cy, float r, float width, int seg, int rgb, float a) {
        band(cx, cy, Math.max(0, r - width / 2), r + width / 2, 1, 0, (float) (Math.PI * 2), seg, rgb, a, rgb, a);
    }

    /** A filled circle, colour {@code centre} in the middle fading to {@code edge} at the rim (a soft glow if edge is 0). */
    public void disc(float cx, float cy, float r, int seg, int centre, float aCentre, int edge, float aEdge) {
        for (int i = 0; i < seg; i++) {
            double t0 = Math.PI * 2 * i / seg, t1 = Math.PI * 2 * (i + 1) / seg;
            tri(cx, cy, centre, aCentre, cx + (float) Math.cos(t0) * r, cy + (float) Math.sin(t0) * r, edge, aEdge,
                    cx + (float) Math.cos(t1) * r, cy + (float) Math.sin(t1) * r, edge, aEdge);
        }
    }

    /** The outline of a regular polygon with n corners, turned by {@code rot} radians. */
    public void polygon(float cx, float cy, float r, int n, float rot, float width, int rgb, float a) {
        for (int i = 0; i < n; i++) {
            double t0 = rot + Math.PI * 2 * i / n, t1 = rot + Math.PI * 2 * (i + 1) / n;
            glowLine(cx + (float) Math.cos(t0) * r, cy + (float) Math.sin(t0) * r, cx + (float) Math.cos(t1) * r, cy + (float) Math.sin(t1) * r, width, rgb, a);
        }
    }
}
