package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * Fractal sky. The Platonic Realm's sky is the Mandelbrot set's seahorse valley in blue and gold, turning slowly with
 * a second, counter-rotating copy laid over it; the Transcendental's is a white-gold Julia set. The images are computed
 * once on the client (no texture files) the first time the sky is drawn.
 */
public class FractalSkyEffects extends DimensionSpecialEffects {
    /** 0 Platonic, 1 Transcendental, 2 inside a black hole, 3 inside a white hole, 4 Transfinite Hierarchy, 5 the Living Proof. */
    private final int mode;
    private final boolean transcendent;
    private ResourceLocation tex;

    public FractalSkyEffects(boolean transcendent) {
        this(transcendent ? 1 : 0);
    }

    public FractalSkyEffects(int mode) {
        super(Float.NaN, false, SkyType.NONE, mode == 1 || mode == 3, false);
        this.mode = mode;
        this.transcendent = mode == 1;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return switch (mode) {
            case 1 -> new Vec3(0.95, 0.9, 0.78);
            case 2 -> new Vec3(0.12, 0.03, 0.02);
            case 3 -> new Vec3(1.0, 0.97, 0.9);
            case 4 -> new Vec3(0.08, 0.06, 0.2);
            case 5 -> new Vec3(0.10, 0.04, 0.10);
            default -> new Vec3(0.55, 0.62, 0.85);
        };
    }

    /** Sky images for the relativity and transfinite dimensions (pixel colour, 0xRRGGBB). */
    private int special(int px, int py, int n) {
        double u = px / (double) n * 2 - 1, v = py / (double) n * 2 - 1, r = Math.sqrt(u * u + v * v), a = Math.atan2(v, u);
        switch (mode) {
            case 2 -> {      // the black hole's own sky: a black shadow ringed by the photon sphere, red-shifted starlight streaming in
                double ring = Math.exp(-Math.pow((r - 0.42) / 0.03, 2)) + 0.4 * Math.exp(-Math.pow((r - 0.48) / 0.08, 2));
                double streak = Math.pow(0.5 + 0.5 * Math.cos(a * 40 + r * 30), 30) * Math.max(0, r - 0.45);
                double b = r < 0.38 ? 0 : ring + streak * 0.6 + 0.04;
                return rgb(b * 1.0, b * 0.45, b * 0.15);
            }
            case 3 -> {      // the white hole: everything pours out of a blinding centre
                double rays = Math.pow(0.5 + 0.5 * Math.cos(a * 24), 8) * 0.35;
                double b = Math.min(1, 0.55 + 0.45 * Math.exp(-r * 3) + rays * (1 - r));
                return rgb(b, b * 0.97, b * 0.88);
            }
            case 5 -> {      // the Living Proof: the inside of a mind - cells of thought joined by golden veins
                double best = 9, second = 9;
                java.util.Random rnd = new java.util.Random(77);
                for (int k = 0; k < 48; k++) {
                    double cx = rnd.nextDouble() * 2 - 1, cy = rnd.nextDouble() * 2 - 1, d = Math.hypot(u - cx, v - cy);
                    if (d < best) { second = best; best = d; } else if (d < second) second = d;
                }
                double vein = Math.exp(-Math.pow((second - best) / 0.012, 2));
                double glow = 0.08 + 0.1 * Math.exp(-best * 8);
                return rgb(glow * 1.2 + vein * 1.0, glow * 0.4 + vein * 0.75, glow * 1.1 + vein * 0.25);
            }
            default -> {     // transfinite: Cantor bars nested toward the centre, an ordinal spiral of points
                double t = Math.log(Math.max(r, 1e-3)) / Math.log(0.5);
                int level = (int) Math.floor(t);
                double frac = (a / (Math.PI * 2) + 0.5) * Math.pow(3, Math.max(0, Math.min(6, level)));
                boolean cantor = true;
                double f = frac % 1.0;
                for (int k = 0; k < 4; k++) {
                    f *= 3;
                    if ((int) f % 3 == 1) cantor = false;
                    f %= 1.0;
                }
                double band = 0.5 + 0.5 * Math.cos((t - level) * Math.PI * 2);
                double b = cantor ? 0.25 + 0.6 * band : 0.04;
                return rgb(b * 0.6, b * 0.5, b * 1.0);
            }
        }
    }

    private static int rgb(double r, double g, double b) {
        return (int) (Math.min(1, r) * 255) << 16 | (int) (Math.min(1, g) * 255) << 8 | (int) (Math.min(1, b) * 255);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    private ResourceLocation texture() {
        if (tex != null) return tex;
        int n = 512;
        NativeImage img = new NativeImage(n, n, false);
        for (int py = 0; py < n; py++)
            for (int px = 0; px < n; px++) {
                if (mode >= 2) {
                    int c = special(px, py, n);
                    img.setPixelRGBA(px, py, 0xFF000000 | (c & 0xFF) << 16 | (c & 0xFF00) | (c >> 16 & 0xFF));
                    continue;
                }
                double it;
                if (transcendent) {
                    double zx = (px / (double) n - 0.5) * 3.0, zy = (py / (double) n - 0.5) * 3.0, cx = -0.8, cy = 0.156;
                    it = smooth(zx, zy, cx, cy, 200);
                } else {
                    double cx = -0.7453 + (px / (double) n - 0.5) * 0.03, cy = 0.1127 + (py / (double) n - 0.5) * 0.03;
                    it = smooth(0, 0, cx, cy, 300);
                }
                int rgb = palette(it);
                // ABGR
                img.setPixelRGBA(px, py, 0xFF000000 | (rgb & 0xFF) << 16 | (rgb & 0xFF00) | (rgb >> 16 & 0xFF));
            }
        tex = TheoreticalSciences.id("dynamic/sky_" + mode);
        Minecraft.getInstance().getTextureManager().register(tex, new DynamicTexture(img));
        return tex;
    }

    private static double smooth(double zx, double zy, double cx, double cy, int max) {
        for (int i = 0; i < max; i++) {
            double x2 = zx * zx, y2 = zy * zy;
            if (x2 + y2 > 256) return i + 1 - Math.log(Math.log(Math.sqrt(x2 + y2))) / Math.log(2);
            zy = 2 * zx * zy + cy;
            zx = x2 - y2 + cx;
        }
        return -1;
    }

    private int palette(double it) {
        if (it < 0) return transcendent ? 0xFFF8E8 : 0x081030;
        double t = Math.sqrt(it / 60.0);
        double s = 0.5 + 0.5 * Math.cos(t * 6.283 * 1.3);
        int r, g, b;
        if (transcendent) {
            r = (int) (200 + 55 * s);
            g = (int) (170 + 70 * s);
            b = (int) (110 + 120 * (1 - s));
        } else {
            r = (int) (20 + 230 * s * s);
            g = (int) (40 + 170 * s);
            b = (int) (110 + 140 * (1 - s * s));
        }
        return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        if (!QHConfig.FANCY_SKY.get()) return false;
        float time = (ticks + partialTick) / 20F;
        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture());
            RenderSystem.defaultBlendFunc();
            box(time * 0.4F, 1F, 1F);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            if (mode != 2) box(-time * 0.25F + 37, transcendent ? 0.35F : 0.25F, 1.7F);
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        return true;
    }

    /** A sky cube whose six faces all show the fractal, slowly turned by {@code degrees}. */
    private void box(float degrees, float alpha, float tile) {
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.YP.rotationDegrees(degrees));
        ps.mulPose(Axis.XP.rotationDegrees(degrees * 0.3F));
        for (int face = 0; face < 6; face++) {
            ps.pushPose();
            switch (face) {
                case 1 -> ps.mulPose(Axis.XP.rotationDegrees(90));
                case 2 -> ps.mulPose(Axis.XP.rotationDegrees(-90));
                case 3 -> ps.mulPose(Axis.XP.rotationDegrees(180));
                case 4 -> ps.mulPose(Axis.ZP.rotationDegrees(90));
                case 5 -> ps.mulPose(Axis.ZP.rotationDegrees(-90));
                default -> {}
            }
            Matrix4f m = ps.last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            float d = 100F;
            bb.addVertex(m, -d, -d, -d).setUv(0, 0).setColor(1F, 1F, 1F, alpha);
            bb.addVertex(m, -d, -d, d).setUv(0, tile).setColor(1F, 1F, 1F, alpha);
            bb.addVertex(m, d, -d, d).setUv(tile, tile).setColor(1F, 1F, 1F, alpha);
            bb.addVertex(m, d, -d, -d).setUv(tile, 0).setColor(1F, 1F, 1F, alpha);
            MeshData mesh = bb.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
            ps.popPose();
        }
    }
}
