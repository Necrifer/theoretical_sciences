package com.theoreticalsciences.home;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.theoreticalsciences.home.PortalOpening.*;

/** Draws the Homeward Portal opening (see {@link PortalOpening} for the order of it). Client only. */
public final class PortalOpeningRenderer {
    private PortalOpeningRenderer() {}

    private static final Map<BlockPos, Long> OPENINGS = new ConcurrentHashMap<>();
    private static Vec3 cam;

    public static void start(PortalOpening.Start p) {
        OPENINGS.put(p.centre(), p.start());
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0F, 1F);
        return x * x * (3 - 2 * x);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || OPENINGS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            OPENINGS.clear();
            return;
        }
        cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        try {
            for (var en : OPENINGS.entrySet()) {
                float age = mc.level.getGameTime() - en.getValue() + partial;
                if (age > END + 5 || age < -40) {
                    OPENINGS.remove(en.getKey());
                    continue;
                }
                if (en.getKey().distToCenterSqr(cam) < 320 * 320) draw(en.getKey(), Math.max(0, age));
            }
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static void draw(BlockPos centre, float age) {
        Vec3 c = new Vec3(centre.getX() + 0.5, centre.getY() + 0.6, centre.getZ() + 0.5);
        Vec3 apex = c.add(0, HEIGHT, 0);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        // the twelve: up, one after another round the ring; then leaning in to meet; gone once the great beam has taken them
        float thin = age < STRIKE ? 1F : 1F - ease((age - STRIKE) / (float) (SWELL - STRIKE));
        if (thin > 0.01F) {
            int i = 0;
            for (Direction d : Direction.Plane.HORIZONTAL)
                for (int o = -1; o <= 1; o++, i++) {
                    BlockPos f = centre.relative(d, 2).relative(d.getClockWise(), o);
                    Vec3 foot = new Vec3(f.getX() + 0.5, f.getY() + 1.0, f.getZ() + 0.5);
                    float rise = ease((age - i * 1.5F) / (RISE - 18F));
                    if (rise <= 0) continue;
                    float lean = ease((age - RISE) / (float) (MEET - RISE));
                    Vec3 top = new Vec3(Mth.lerp(lean, foot.x, apex.x), foot.y + (apex.y - foot.y) * rise, Mth.lerp(lean, foot.z, apex.z));
                    float flick = 0.85F + 0.15F * Mth.sin(age * 0.9F + i * 2.1F);
                    ribbon(bb, foot, top, 0.16F, 0xFFFFFF, 0.95F * thin * flick, 0xFFF4D0, 0.8F * thin * flick);
                    ribbon(bb, foot, top, 0.6F, 0xFFD870, 0.3F * thin, 0xFFE9A8, 0.22F * thin);
                    glow(bb, foot, 0.5F, 0xFFE9A8, 0.7F * thin);
                    if (lean < 0.98F) glow(bb, top, 0.5F + 0.3F * rise, 0xFFFFFF, 0.8F * thin);
                }
        }
        // what gathers where they meet
        if (age > RISE + 10 && age < SWELL) {
            float g = ease((age - RISE - 10) / (float) (GATHER - RISE - 10)) * (age < STRIKE ? 1F : 1F - ease((age - STRIKE) / (float) (SWELL - STRIKE)));
            float pulse = 1F + 0.12F * Mth.sin(age * 0.8F);
            glow(bb, apex, 2.5F + 7F * g * pulse, 0xFFE9A8, 0.55F * g);
            glow(bb, apex, 1.0F + 3.5F * g * pulse, 0xFFFFFF, 0.95F * g);
            for (int r = 0; r < 3; r++) {                                   // halos turning round it
                float rad = (3F + r * 2.2F) * g;
                int n = 40;
                for (int k = 0; k < n; k++) {
                    double a0 = k * Math.PI * 2 / n + age * 0.05 * (r % 2 == 0 ? 1 : -1), a1 = a0 + Math.PI * 2 / n;
                    ribbon(bb, apex.add(Math.cos(a0) * rad, (r - 1) * 0.8, Math.sin(a0) * rad), apex.add(Math.cos(a1) * rad, (r - 1) * 0.8, Math.sin(a1) * rad),
                            0.18F, 0xFFF4D0, 0.6F * g, 0xFFF4D0, 0.6F * g);
                }
            }
        }
        // the great beam: down from where they met, onto the opening; it swells until the ring is inside it; it sinks into the portal
        if (age >= GATHER && age < END) {
            float fall = ease((age - GATHER) / (float) (STRIKE - GATHER));
            float swell = ease((age - STRIKE) / (float) (SWELL - STRIKE));
            float sink = ease((age - SWELL) / (float) (END - SWELL));
            Vec3 top = apex.add(0, -HEIGHT * sink, 0);                      // its top comes down after it
            Vec3 bottom = apex.add(0, -HEIGHT * fall, 0);
            float w = (1.4F + 6.2F * swell) * (1F - 0.85F * sink);
            float a = 1F - 0.6F * sink * sink;
            ribbon(bb, top, bottom, w * 0.45F, 0xFFFFFF, 0.95F * a, 0xFFFFFF, 0.95F * a);
            ribbon(bb, top, bottom, w, 0xFFE9A8, 0.6F * a, 0xFFF4D0, 0.7F * a);
            ribbon(bb, top, bottom, w * 1.7F, 0xFFD870, 0.22F * a, 0xFFD870, 0.28F * a);
            if (fall >= 1F) {
                // where it lands: light spreading over the frames, and rings running out along the ground
                glow(bb, c.add(0, 0.6, 0), (3F + 6F * swell) * (1F - 0.7F * sink), 0xFFFFFF, 0.8F * a);
                for (int r = 0; r < 3; r++) {
                    float q = ((age - STRIKE) / 18F + r / 3F) % 1F;
                    if (q < 0) continue;
                    float rad = 1.5F + q * 9F;
                    int n = 48;
                    for (int k = 0; k < n; k++) {
                        double a0 = k * Math.PI * 2 / n, a1 = a0 + Math.PI * 2 / n;
                        ribbon(bb, c.add(Math.cos(a0) * rad, 0.5, Math.sin(a0) * rad), c.add(Math.cos(a1) * rad, 0.5, Math.sin(a1) * rad),
                                0.35F, 0xFFE9A8, 0.7F * (1 - q) * (1 - sink), 0xFFE9A8, 0.7F * (1 - q) * (1 - sink));
                    }
                }
            }
        }
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static void v(BufferBuilder bb, double x, double y, double z, int rgb, float a) {
        bb.addVertex((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, Mth.clamp(a, 0F, 1F));
    }

    /** A band of light from a to b that always faces the camera. */
    private static void ribbon(BufferBuilder bb, Vec3 a, Vec3 b, float width, int rgbA, float alphaA, int rgbB, float alphaB) {
        Vec3 side = b.subtract(a).cross(a.add(b).scale(0.5).subtract(cam));
        double len = side.length();
        if (len < 1e-6) return;
        side = side.scale(width / 2 / len);
        v(bb, a.x - side.x, a.y - side.y, a.z - side.z, rgbA, alphaA);
        v(bb, a.x + side.x, a.y + side.y, a.z + side.z, rgbA, alphaA);
        v(bb, b.x + side.x, b.y + side.y, b.z + side.z, rgbB, alphaB);
        v(bb, b.x - side.x, b.y - side.y, b.z - side.z, rgbB, alphaB);
    }

    /** A soft round light: an eight-sided fan, bright in the middle, facing the camera. */
    private static void glow(BufferBuilder bb, Vec3 p, float r, int rgb, float alpha) {
        Vec3 look = p.subtract(cam);
        double d = look.length();
        if (d < 1e-4) return;
        look = look.scale(1 / d);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 upv = right.cross(look).normalize();
        int n = 8;
        for (int k = 0; k < n; k++) {
            double a0 = k * Math.PI * 2 / n, a1 = a0 + Math.PI * 2 / n;
            Vec3 e0 = p.add(right.scale(Math.cos(a0) * r)).add(upv.scale(Math.sin(a0) * r)), e1 = p.add(right.scale(Math.cos(a1) * r)).add(upv.scale(Math.sin(a1) * r));
            v(bb, p.x, p.y, p.z, rgb, alpha);
            v(bb, p.x, p.y, p.z, rgb, alpha);
            v(bb, e0.x, e0.y, e0.z, rgb, 0);
            v(bb, e1.x, e1.y, e1.z, rgb, 0);
        }
    }
}
