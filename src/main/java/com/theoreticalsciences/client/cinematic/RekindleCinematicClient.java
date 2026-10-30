package com.theoreticalsciences.client.cinematic;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.world.transit.RekindleCinematic;
import com.theoreticalsciences.world.transit.RekindlePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

/**
 * The Pleroma Reconstitutor's 10 second finale, as seen by a player near the machine.
 * <pre>
 *   0 - 3.5 s    seven pillars of light climb out of the machine, motes spiralling up between them
 *   2.5 - 4.5 s  the Transcendental's central spire is drawn upward, then its crown
 *   3.5 - 5.5 s  its great halo is traced around the sky
 *   4.5 - 8 s    five concentric rings are drawn one after another, flickering like an old recording
 *                (the Echo of the Transcendental being played back)
 *   8.25 s       a flash: it is whole again; the title "THE TRANSCENDENTAL IS REKINDLED" fades in
 *   9 - 10.75 s  everything fades away
 * </pre>
 * The wireframe is drawn in the world (so it stands above the machine and you can walk around it); the title, the
 * flash and a faint golden vignette are drawn on the screen.
 */
public final class RekindleCinematicClient {
    private static final float END = RekindleCinematic.LENGTH + 15;     // the title lingers a little after the visuals
    private static final float SKY = (float) RekindleCinematic.SKY;
    private static final int GOLD = 0xFFE8A0, WHITE = 0xFFFFFF, AMBER = 0xFFB040;

    private static Vec3 centre;
    private static ResourceKey<Level> dim;
    private static long start;

    public static void receive(RekindlePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        centre = new Vec3(p.x(), p.y(), p.z());
        dim = mc.level.dimension();
        start = mc.level.getGameTime();
        // the plain "rekindled" title the server sent a moment ago would sit on top of the cinematic; ours comes at the climax
        mc.gui.clear();
    }

    public static void reset() {
        centre = null;
    }

    /** Ticks since the cinematic began (with the fraction of the current tick), or -1 if none is playing here. */
    private static float age(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (centre == null || mc.level == null || mc.level.dimension() != dim) return -1;
        float a = mc.level.getGameTime() - start + partial;
        if (a > END || a < -5) {
            centre = null;
            return -1;
        }
        return Math.max(0, a);
    }

    // ================================================================== in the world

    private static Vec3 cam;
    private static Vector3f left, up;
    private static BufferBuilder bb;

    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || centre == null) return;
        float a = age(e.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (a < 0 || a > RekindleCinematic.LENGTH) return;
        Minecraft mc = Minecraft.getInstance();
        cam = e.getCamera().getPosition();
        left = e.getCamera().getLeftVector();
        up = e.getCamera().getUpVector();
        // the same camera set-up as the mod's other world effects (EffectRenderer)
        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(e.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(e.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        mc.getMainRenderTarget().bindWrite(false);
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);   // light adds up
            RenderSystem.depthMask(false);
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            scene(a);
            MeshData mesh = bb.build();
            if (mesh != null) BufferUploader.drawWithShader(mesh);
        } finally {
            bb = null;
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void scene(float a) {
        Vec3 c = centre;
        float fade = 1 - smooth((a - 175) / 25F);
        double top = c.y + SKY;
        // --- pillars of light
        float grow = smooth(a / 50F);
        float beamA = fade * (a < 70 ? 1 : 0.55F);
        double height = grow * (SKY + 10);
        for (int i = 0; i < 7; i++) {
            double ang = i * Math.PI * 2 / 6 + a * 0.01;
            double r = i == 6 ? 0 : 1.6;
            Vec3 b = new Vec3(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r);
            Vec3 t = b.add(0, height, 0);
            float wdt = i == 6 ? 0.8F : 0.35F;
            ribbon(b, t, wdt * 3, GOLD, 0.15F * beamA, GOLD, 0);
            ribbon(b, t, wdt, WHITE, 0.8F * beamA, GOLD, 0.1F * beamA);
        }
        for (int i = 0; i < 40; i++) {
            double q = frac(rnd(i, 1) + a * 0.01F * (0.6F + rnd(i, 2)));
            double ang = a * 0.05 + i, r = 1 + rnd(i, 3) * 2.5;
            glow(new Vec3(c.x + Math.cos(ang) * r, c.y + q * (SKY + 10), c.z + Math.sin(ang) * r), 0.15F, GOLD, 0.8F * fade * (float) (1 - q));
        }
        // --- the Transcendental, drawn piece by piece; flickering while the Echo replays it
        float echo = a < 160 ? 0.82F + 0.18F * (float) Math.sin(a * 1.7) : 1;
        float sa = fade * echo;
        float spire = smooth((a - 50) / 40F);
        if (spire > 0) {
            Vec3 s0 = new Vec3(c.x, top - 6, c.z), s1 = s0.add(0, spire * 20, 0);
            ribbon(s0, s1, 0.9F, GOLD, 0.2F * sa, WHITE, 0.2F * sa);
            ribbon(s0, s1, 0.18F, WHITE, 0.95F * sa, WHITE, 0.95F * sa);
            glow(s1, 0.6F, WHITE, sa);
            if (spire >= 1) crown(new Vec3(c.x, top + 14, c.z), 1.3F, a * 0.04F, smooth((a - 90) / 12F) * sa);
        }
        // the great halo
        arc(new Vec3(c.x, top + 10, c.z), 9, 0.12, a * 0.004, smooth((a - 70) / 40F), GOLD, sa, true);
        // five concentric rings, one after another, alternating direction, with spokes every 30 degrees
        double[] radii = {3, 5.5, 8, 11, 14};
        for (int i = 0; i < radii.length; i++) {
            float k = smooth((a - 90 - i * 14) / 18F);
            if (k <= 0) continue;
            double rot = a * 0.01 * (i % 2 == 0 ? 1 : -1);
            Vec3 rc = new Vec3(c.x, top - i * 0.6, c.z);
            arc(rc, radii[i], 0, rot, k, i % 2 == 0 ? WHITE : AMBER, sa, true);
            for (int sp = 0; sp < 12; sp++) {
                if (sp / 12F > k) break;
                double an = rot + sp * Math.PI / 6;
                Vec3 p0 = rc.add(Math.cos(an) * (radii[i] - 0.5), 0, Math.sin(an) * (radii[i] - 0.5));
                Vec3 p1 = rc.add(Math.cos(an) * (radii[i] + 0.5), 0, Math.sin(an) * (radii[i] + 0.5));
                ribbon(p0, p1, 0.1F, GOLD, 0.8F * sa, GOLD, 0.8F * sa);
            }
        }
        // the flash when it is whole
        float flash = Math.max(0, 1 - Math.abs(a - 168) / 12F);
        if (flash > 0) glow(new Vec3(c.x, top, c.z), 2 + (a - 156) * 1.2F, WHITE, flash);
        if (a > 168) glow(new Vec3(c.x, top, c.z), 6, GOLD, 0.35F * fade);
    }

    /** A circle (or part of one: {@code k} = 0..1 of the way round) traced by a bright "pen" point. */
    private static void arc(Vec3 c, double r, double tilt, double rot, float k, int rgb, float alpha, boolean pen) {
        if (k <= 0) return;
        int seg = 64, n = (int) Math.ceil(seg * k);
        Vec3 prev = null;
        for (int j = 0; j <= n; j++) {
            double an = rot + Math.PI * 2 * Math.min(k, j / (double) seg);
            Vec3 p = c.add(Math.cos(an) * r, Math.sin(an) * r * tilt, Math.sin(an) * r);
            if (prev != null) {
                ribbon(prev, p, 0.6F, rgb, 0.15F * alpha, rgb, 0.15F * alpha);
                ribbon(prev, p, 0.12F, rgb, 0.9F * alpha, rgb, 0.9F * alpha);
            }
            prev = p;
        }
        if (pen && k < 1 && prev != null) glow(prev, 0.7F, WHITE, alpha);
    }

    /** The diamond at the top of the spire (an octahedron's twelve edges). */
    private static void crown(Vec3 c, float s, float rot, float alpha) {
        if (alpha <= 0) return;
        Vec3[] v = new Vec3[6];
        for (int i = 0; i < 4; i++) v[i] = c.add(Math.cos(rot + i * Math.PI / 2) * s, 0, Math.sin(rot + i * Math.PI / 2) * s);
        v[4] = c.add(0, s * 1.6, 0);
        v[5] = c.add(0, -s * 1.6, 0);
        for (int i = 0; i < 4; i++) {
            ribbon(v[i], v[(i + 1) % 4], 0.12F, WHITE, alpha, WHITE, alpha);
            ribbon(v[i], v[4], 0.12F, GOLD, alpha, WHITE, alpha);
            ribbon(v[i], v[5], 0.12F, GOLD, alpha, WHITE, alpha);
        }
        glow(c, 2.5F, GOLD, 0.4F * alpha);
    }

    // ------------------------------------------------------------------ world primitives (camera-relative)

    private static void v(Vec3 p, int rgb, float a) {
        bb.addVertex((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, Math.max(0, Math.min(1, a)));
    }

    /** A flat strip between two points that always faces the camera (how beams and lines are drawn). */
    private static void ribbon(Vec3 a, Vec3 b, float width, int rgbA, float alphaA, int rgbB, float alphaB) {
        Vec3 dir = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(view);
        double len = side.length();
        if (len < 1e-6) return;
        side = side.scale(width / 2 / len);
        v(a.subtract(side), rgbA, alphaA);
        v(a.add(side), rgbA, alphaA);
        v(b.add(side), rgbB, alphaB);
        v(b.subtract(side), rgbB, alphaB);
    }

    /** A soft round glow facing the camera. */
    private static void glow(Vec3 c, float radius, int rgb, float alpha) {
        if (alpha <= 0.005F) return;
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            Vec3 p0 = c.add((left.x() * Math.cos(a0) + up.x() * Math.sin(a0)) * radius, (left.y() * Math.cos(a0) + up.y() * Math.sin(a0)) * radius,
                    (left.z() * Math.cos(a0) + up.z() * Math.sin(a0)) * radius);
            Vec3 p1 = c.add((left.x() * Math.cos(a1) + up.x() * Math.sin(a1)) * radius, (left.y() * Math.cos(a1) + up.y() * Math.sin(a1)) * radius,
                    (left.z() * Math.cos(a1) + up.z() * Math.sin(a1)) * radius);
            v(c, rgb, alpha);
            v(p0, rgb, 0);
            v(p1, rgb, 0);
            v(p1, rgb, 0);
        }
    }

    // ================================================================== on the screen

    private static void renderLayer(GuiGraphics g, DeltaTracker dt) {
        float a = age(dt.getGameTimeDeltaPartialTick(false));
        if (a < 0) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float fade = 1 - smooth((a - 175) / 25F);
        g.pose().pushPose();
        g.pose().translate(0, 0, 380);
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.ADD);
        // a faint golden vignette while it plays
        float vig = 0.22F * smooth(a / 20F) * fade;
        if (vig > 0) {
            f.rectV(0, 0, w, h * 0.25F, GOLD, vig, GOLD, 0);
            f.rectV(0, h * 0.75F, w, h, GOLD, 0, GOLD, vig);
        }
        float flash = Math.max(0, 1 - Math.abs(a - 168) / 10F);
        if (flash > 0) f.rect(0, 0, w, h, WHITE, 0.45F * flash);
        f.end();
        // the title: fades in at the climax, stays, then fades out
        float ta = smooth((a - 150) / 15F) * (1 - smooth((a - 190) / 25F));
        if (ta > 0.03F) {
            Font font = Minecraft.getInstance().font;
            Component title = Component.translatable("cinematic.theoreticalsciences.rekindled.title").withStyle(ChatFormatting.BOLD);
            Component sub = Component.translatable("cinematic.theoreticalsciences.rekindled.subtitle");
            int al = (int) (ta * 255);
            g.pose().pushPose();
            g.pose().translate(w / 2F, h * 0.3F, 0);
            g.pose().scale(2.5F, 2.5F, 1);
            g.drawString(font, title, -font.width(title) / 2, -4, (al << 24) | GOLD, true);
            g.pose().popPose();
            g.drawString(font, sub, (w - font.width(sub)) / 2, (int) (h * 0.3F) + 18, (al << 24) | 0xE8DCC0, true);
        }
        g.pose().popPose();
    }

    public static void registerLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(TheoreticalSciences.id("rekindle_cinematic"), RekindleCinematicClient::renderLayer);
    }

    // ------------------------------------------------------------------ maths

    private static float smooth(float v) {
        v = v < 0 ? 0 : v > 1 ? 1 : v;
        return v * v * (3 - 2 * v);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static float rnd(int a, int b) {
        int h = a * 0x27D4EB2D ^ (b + 0x9E3779B9) * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h >>> 8) / 16777216F;
    }

    private RekindleCinematicClient() {}
}
