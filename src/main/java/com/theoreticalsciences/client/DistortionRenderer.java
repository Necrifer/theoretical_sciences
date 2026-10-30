package com.theoreticalsciences.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Inside a higher-dimensional structure, space itself is drawn wrong (round 14). The server tells us which kind of
 * structure we are in and how deep (StructureZones); a full-screen post shader (assets/theoreticalsciences/shaders) folds
 * the view to match:
 * <ol>
 *   <li>TESSERACT - nested cubes folding through each other, the inner cube becoming the outer</li>
 *   <li>FRACTAL - the view recursing into itself (a Droste spiral)</li>
 *   <li>KALEIDOSCOPE - mirrored through a Platonic solid's five-fold symmetry</li>
 *   <li>INFINITE - an endless corridor: the same space repeating, smaller and smaller</li>
 *   <li>TWIST - space wound round itself (Calabi-Yau, Hopf)</li>
 *   <li>LENS - light bent round a horizon: an Einstein ring, blue-shifted in, red-shifted out</li>
 *   <li>HYPERREAL - waves of a reality whose laws flow</li>
 *   <li>GLITCH - the anomaly engine tearing the picture</li>
 *   <li>TRANSCENDENT - all of it at once</li>
 * </ol>
 * The camera breathes and rolls with it. Strength fades in and out smoothly; any shader failure turns it off for good.
 */
public final class DistortionRenderer {
    private static int mode;
    private static float target, current;
    private static BlockPos centre = BlockPos.ZERO;
    private static int radius = 1;
    private static TextureTarget copy;
    private static boolean broken;
    private static long last;

    public static void receive(int m, float intensity, BlockPos c, int r) {
        if (m != 0) mode = m;
        target = m == 0 ? 0 : intensity;
        centre = c;
        radius = r;
    }

    private static float strength(float partial) {
        return Math.max(0, Math.min(1, current));
    }

    /** Leaving a world: no distortion carries over into the next one. */
    public static void reset() {
        mode = 0;
        target = current = 0;
        last = 0;
    }

    /** The camera's breathing and roll obey the same switch as the shader. */
    private static boolean cameraOff() {
        return broken || mode == 0 || !com.theoreticalsciences.QHConfig.distortionEnabled();
    }

    /**
     * Drawn right after the world, before the hand and the HUD. The frame is copied, then a full-screen quad redraws it
     * through the distortion shader - the same proven path the gravitational lenses use, so nothing else in the frame
     * (render targets, depth, other mods' HUD batching) is touched.
     */
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            target = current = 0;
            return;
        }
        // ease towards the target over about a second, whatever the frame rate
        long now = System.nanoTime();
        float dt = last == 0 ? 0 : Math.min(0.25F, (now - last) / 1e9F);
        last = now;
        current += (target - current) * (1 - (float) Math.exp(-dt * 2.5));
        if (Math.abs(target - current) < 0.002F) current = target;
        float s = strength(0);
        ShaderInstance sh = com.theoreticalsciences.client.render.QHShaders.DISTORT;
        if (s < 0.01F || broken || mode == 0 || sh == null || !com.theoreticalsciences.QHConfig.distortionEnabled()) return;
        RenderTarget main = mc.getMainRenderTarget();
        try {
            int w = main.width, h = main.height;
            if (copy == null) {
                copy = new TextureTarget(w, h, false, Minecraft.ON_OSX);
                copy.setFilterMode(GL11.GL_LINEAR);
            } else if (copy.width != w || copy.height != h) {
                copy.resize(w, h, Minecraft.ON_OSX);
                copy.setFilterMode(GL11.GL_LINEAR);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(true);

            float t = (mc.level.getGameTime() % 24000L) + e.getPartialTick().getGameTimeDeltaPartialTick(false);
            sh.safeGetUniform("ScreenSize").set((float) w, (float) h);
            sh.safeGetUniform("Mode").set((float) mode);
            sh.safeGetUniform("Intensity").set(s);
            sh.safeGetUniform("Clock").set(t / 20F);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(() -> sh);
            RenderSystem.setShaderTexture(0, copy.getColorTextureId());
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bb.addVertex(-1F, -1F, 0F).setUv(0F, 0F);
            bb.addVertex(1F, -1F, 0F).setUv(1F, 0F);
            bb.addVertex(1F, 1F, 0F).setUv(1F, 1F);
            bb.addVertex(-1F, 1F, 0F).setUv(0F, 1F);
            BufferUploader.drawWithShader(bb.buildOrThrow());
        } catch (Exception ex) {
            TheoreticalSciences.LOGGER.warn("Space distortion unavailable, turning it off: {}", ex.toString());
            broken = true;
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setShaderTexture(0, 0);
            main.bindWrite(false);
        }
    }

    /** The view breathes in a tesseract and an endless corridor, and pulls in near a horizon. */
    public static void onFov(ViewportEvent.ComputeFov e) {
        float s = strength(0);
        if (s < 0.01F || cameraOff()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double t = (mc.level.getGameTime() + e.getPartialTick()) / 20.0;
        double k = switch (mode) {
            case 1 -> 12 * Math.sin(t * 0.7);
            case 4 -> 8 * Math.sin(t * 0.35) + 6;
            case 6 -> -10;
            case 9 -> 10 * Math.sin(t * 0.5);
            default -> 0;
        };
        e.setFOV(e.getFOV() + k * s);
    }

    /** Space wound round itself rolls the view with it. */
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        float s = strength(0);
        if (s < 0.01F || cameraOff()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        double t = (mc.level.getGameTime() + e.getPartialTick()) / 20.0;
        double roll = switch (mode) {
            case 5 -> 9 * Math.sin(t * 0.45);
            case 7 -> 3 * Math.sin(t * 0.9);
            case 9 -> 6 * Math.sin(t * 0.3);
            default -> 0;
        };
        e.setRoll(e.getRoll() + (float) (roll * s));
    }

    private DistortionRenderer() {}
}
