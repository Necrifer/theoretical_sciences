package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.DysonControllerBlockEntity;
import com.theoreticalsciences.machine.MachineType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Draws every large-scale physics effect after the level has rendered:
 * <ol>
 *   <li>Emissive 3D geometry (accretion disks, tesseracts, Dyson swarms, Matrioshka shells, rifts, collider beams).</li>
 *   <li>A copy of the frame is taken and lens billboards resample it (black holes, white holes, dark matter, rifts).</li>
 *   <li>Geometry that sits in front of a horizon is drawn again on top, so the near side of a disk crosses the shadow.</li>
 * </ol>
 * All GL state touched here is restored afterwards so other mods' rendering (and lighting) is unaffected.
 */
public final class EffectRenderer {
    private static TextureTarget sceneCopy;

    private record Lens(Vec3 pos, float worldRadius, float quadRadius, int mode, float strength) {}

    private static final List<Lens> LENSES = new ArrayList<>();
    private static final List<Runnable> FOREGROUND = new ArrayList<>();

    private static Vec3 cam;
    /** Brightness multiplier for the effect being drawn (start-up / shut-down fades). */
    private static float FX_A = 1F;
    private static Vector3f left, up;
    private static float time;

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft mc = Minecraft.getInstance();
        boolean selfGlitch = mc.player != null && mc.player.hasEffect(com.theoreticalsciences.registry.QHEffects.REALITY_DESTABILISATION);
        if (mc.level == null || (ClientHooks.CONTROLLERS.isEmpty() && ClientHooks.FAILURES.isEmpty() && ClientHooks.BUBBLES.isEmpty() && ClientHooks.RUPTURES.isEmpty()
                && ClientHooks.WRAITHS.isEmpty() && ClientHooks.FRACTURES.isEmpty() && ClientHooks.DECOR.isEmpty() && ClientHooks.HYPEROBJECTS.isEmpty() && ClientHooks.SENTINELS.isEmpty() && ClientHooks.SINGULARITIES.isEmpty() && !selfGlitch)) return;

        Camera camera = event.getCamera();
        cam = camera.getPosition();
        left = new Vector3f(camera.getLeftVector());
        up = new Vector3f(camera.getUpVector());
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        time = (mc.level.getGameTime() + partial) / 20.0F;
        double maxDist = QHConfig.LENSING_MAX_DISTANCE.get();

        List<ControllerBlockEntity> visible = new ArrayList<>();
        List<ControllerBlockEntity> sweeping = new ArrayList<>();
        double tick = mc.level.getGameTime() + partial;
        for (ControllerBlockEntity c : List.copyOf(ClientHooks.CONTROLLERS)) {
            if (c.isRemoved() || c.getLevel() != mc.level || !c.isFormed()) continue;
            PowerAnimator.State ps = PowerAnimator.update(c, tick);
            Vec3 ctr = c.effectCenter();
            if (ctr.distanceToSqr(cam) > maxDist * maxDist) continue;
            double since = ps.sinceChange(tick);
            if (since < (ps.on ? PowerAnimator.WAVE_UP : PowerAnimator.WAVE_DOWN) + 0.2) sweeping.add(c);
            if (ps.power(tick) <= 0.001F) continue;
            if (c.type.effect == MachineType.Effect.NONE) continue;
            if (!event.getFrustum().isVisible(new AABB(ctr, ctr).inflate(6))) continue;
            visible.add(c);
        }
        List<Vec3> bubbles = new ArrayList<>();
        for (var b : List.copyOf(ClientHooks.BUBBLES)) {
            if (b.isRemoved() || b.getLevel() != mc.level) continue;
            Vec3 bp = Vec3.atCenterOf(b.getBlockPos());
            if (bp.distanceToSqr(cam) > 96 * 96 || !event.getFrustum().isVisible(new AABB(bp, bp).inflate(2))) continue;
            bubbles.add(bp);
        }
        List<com.theoreticalsciences.entity.ContainmentRupture> ruptures = new ArrayList<>();
        for (var en : List.copyOf(ClientHooks.RUPTURES))
            if (en instanceof com.theoreticalsciences.entity.ContainmentRupture r && !r.isRemoved() && r.level() == mc.level && r.distanceToSqr(cam) < 256 * 256) ruptures.add(r);
        List<net.minecraft.world.entity.Entity> wraiths = new ArrayList<>();
        for (var en : List.copyOf(ClientHooks.WRAITHS)) {
            if (en.isRemoved() || en.level() != mc.level || en.distanceToSqr(cam) > 128 * 128) { ClientHooks.WRAITHS.remove(en); continue; }
            if (event.getFrustum().isVisible(en.getBoundingBox().inflate(2))) wraiths.add(en);
        }
        // the nearest few fractured blocks bend the view around them
        List<Vec3> fractures = new ArrayList<>();
        for (var f : List.copyOf(ClientHooks.FRACTURES)) {
            if (f.isRemoved() || f.getLevel() != mc.level) continue;
            Vec3 fp = Vec3.atCenterOf(f.getBlockPos());
            if (fp.distanceToSqr(cam) < 48 * 48 && event.getFrustum().isVisible(new AABB(fp, fp).inflate(1))) fractures.add(fp);
        }
        fractures.sort(java.util.Comparator.comparingDouble(f -> f.distanceToSqr(cam)));
        if (fractures.size() > 4) fractures = fractures.subList(0, 4);
        // a destabilised player's own vision tears for a moment every so often
        boolean glitchNow = selfGlitch && ((int) (time * 3) * 2654435761L >>> 7 & 7) == 0;
        List<Object[]> melting = new ArrayList<>();
        for (var en : ClientHooks.FAILURES.entrySet()) {
            double age = tick - en.getValue()[0];
            if (age > MELTDOWN + 50 || age < -5) { ClientHooks.FAILURES.remove(en.getKey()); continue; }
            if (mc.level.getBlockEntity(en.getKey()) instanceof ControllerBlockEntity c && Vec3.atCenterOf(en.getKey()).distanceToSqr(cam) < 256 * 256)
                melting.add(new Object[]{c, en.getValue()});
        }
        if (melting.isEmpty() && visible.isEmpty() && sweeping.isEmpty() && bubbles.isEmpty() && ruptures.isEmpty() && wraiths.isEmpty() && fractures.isEmpty() && !glitchNow && ClientHooks.DECOR.isEmpty() && ClientHooks.HYPEROBJECTS.isEmpty() && ClientHooks.SENTINELS.isEmpty() && ClientHooks.SINGULARITIES.isEmpty()) return;
        final List<Vec3> shownFractures = fractures;
        bubbles.sort(java.util.Comparator.comparingDouble(b -> b.distanceToSqr(cam)));
        if (bubbles.size() > 64) bubbles = bubbles.subList(0, 64);
        final List<Vec3> shownBubbles = bubbles;

        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        mc.getMainRenderTarget().bindWrite(false);

        try {
            LENSES.clear();
            FOREGROUND.clear();
            for (ControllerBlockEntity c : sweeping) powerSweep(c, tick);
            for (Object[] m : melting) meltdown((ControllerBlockEntity) m[0], tick, ((long[]) m[1])[0], (int) ((long[]) m[1])[1]);
            for (ControllerBlockEntity c : visible) drawAnimated(c, tick, mvs);
            drawDecor(tick, mvs, event.getFrustum(), partial);
            for (var fx : ClientHooks.SINGULARITIES) {
                double age = tick - fx.start();
                if (fx.dim() != mc.level.dimension() || age > fx.ticks() || age < -5) {
                    ClientHooks.SINGULARITIES.remove(fx);
                    continue;
                }
                if (fx.pos().distanceToSqr(cam) < 192 * 192 && event.getFrustum().isVisible(new AABB(fx.pos(), fx.pos()).inflate(10)))
                    scepterSingularity(fx, (float) age, mvs);
            }
            for (Vec3 b : shownBubbles) bubbleUniverse(b);
            for (var r : ruptures) rupture(r.getPosition(partial), r.radius(), r.progress(), r.collapse(), r.peak());
            for (var w : wraiths) wraith(w, partial);
            for (int i = 0; i < shownFractures.size(); i++) {
                Vec3 fp = shownFractures.get(i);
                // each fracture flickers in and out of glitching on its own rhythm
                if (Math.sin(time * (2.3 + i * 0.7) + fp.x * 1.7 + fp.z) > -0.2)
                    LENSES.add(new Lens(fp, 0.45F, 1.1F, (i & 1) == 0 ? 5 : 3, 0.6F));
            }
            if (glitchNow) {
                Vec3 look = new Vec3(camera.getLookVector());
                LENSES.add(new Lens(cam.add(look.scale(0.6)), 0.6F, 1.6F, 5, 1.0F));
            }
            if (!LENSES.isEmpty() && QHConfig.LENSING_ENABLED.get() && QHShaders.LENS != null) {
                drawLenses(mc, event.getModelViewMatrix(), event.getProjectionMatrix());
            }
            for (Runnable r : FOREGROUND) r.run();
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    // ------------------------------------------------------------------ start-up / shut-down

    private static float easeOutBack(float x) {
        float c1 = 1.70158F, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    /** Draws a controller's effect scaled and faded by its power level, with ignition stutter and shut-down flicker. */
    private static void drawAnimated(ControllerBlockEntity c, double tick, Matrix4fStack mvs) {
        PowerAnimator.State ps = PowerAnimator.update(c, tick);
        float p = ps.power(tick);
        float alpha, scale;
        long h = c.getBlockPos().asLong() * 31L;
        if (p >= 0.999F) {
            alpha = 1F;
            scale = 1F;
        } else if (ps.on) {
            float stutter = p < 0.4F && (((long) (tick * 1.5) + h) % 3 == 0) ? 0.2F : 1F;   // ignition: it catches, fails, catches
            alpha = p * p * (3 - 2 * p) * stutter;
            scale = 0.12F + 0.88F * easeOutBack(p);                                          // swells past full size, settles
        } else {
            boolean drop = p < 0.7F && new Random((long) (tick * 0.8) + h).nextFloat() < 0.35F;  // brown-out flicker
            alpha = p * (drop ? 0.15F : 1F);
            scale = 0.25F + 0.75F * (float) Math.sqrt(p);
        }
        Vec3 ctr = c.effectCenter();
        int lensStart = LENSES.size(), fgStart = FOREGROUND.size();
        Vec3 rel = ctr.subtract(cam);
        boolean transform = scale != 1F;
        if (transform) {
            mvs.pushMatrix();
            mvs.translate((float) rel.x, (float) rel.y, (float) rel.z);
            mvs.scale(scale);
            mvs.translate((float) -rel.x, (float) -rel.y, (float) -rel.z);
            RenderSystem.applyModelViewMatrix();
        }
        FX_A = alpha;
        try {
            drawEffect(c);
        } finally {
            FX_A = 1F;
            if (transform) {
                mvs.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }
        // lenses and the foreground half of disks are drawn later: carry the same scale and fade over to them
        for (int i = lensStart; i < LENSES.size(); i++) {
            Lens l = LENSES.get(i);
            LENSES.set(i, new Lens(l.pos(), l.worldRadius() * scale, l.quadRadius() * scale, l.mode(), l.strength() * alpha));
        }
        for (int i = fgStart; i < FOREGROUND.size(); i++) {
            Runnable r = FOREGROUND.get(i);
            final float a = alpha, sc = scale;
            FOREGROUND.set(i, () -> {
                if (sc != 1F) {
                    mvs.pushMatrix();
                    mvs.translate((float) rel.x, (float) rel.y, (float) rel.z);
                    mvs.scale(sc);
                    mvs.translate((float) -rel.x, (float) -rel.y, (float) -rel.z);
                    RenderSystem.applyModelViewMatrix();
                }
                FX_A = a;
                try {
                    r.run();
                } finally {
                    FX_A = 1F;
                    if (sc != 1F) {
                        mvs.popMatrix();
                        RenderSystem.applyModelViewMatrix();
                    }
                }
            });
        }
        // the very end of a shut-down: everything falls into one last flash
        if (!ps.on && p > 0F && p < 0.14F) glow(ctr, 1.6F * (0.14F - p) / 0.14F + 0.2F, 0xFFFFFF, (0.14F - p) / 0.14F);
    }

    /** The wave of light that runs through the structure's blocks when it powers up (outward) or down (inward). */
    private static void powerSweep(ControllerBlockEntity c, double tick) {
        PowerAnimator.State ps = PowerAnimator.update(c, tick);
        double since = ps.sinceChange(tick);
        boolean up = ps.on;
        double dur = up ? PowerAnimator.WAVE_UP : PowerAnimator.WAVE_DOWN;
        if (since > dur + 0.2 || ps.parts.isEmpty()) return;
        double f = Math.min(1, since / dur);
        double front = up ? f * (ps.maxDist + 2) : (1 - f) * (ps.maxDist + 2);
        int rgb = up ? brighten(c.type.color) : 0xFF6040;
        float fade = (float) (up ? 1.0 : 0.75);
        net.minecraft.core.BlockPos o = c.getBlockPos();
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n = 0;
        Random rnd = new Random((long) (tick * 7));
        for (net.minecraft.core.BlockPos bp : ps.parts) {
            double d = Math.sqrt(bp.distSqr(o));
            double k = 1 - Math.abs(d - front) / 2.2;
            if (k <= 0) continue;
            cube(bb, bp, (float) (k * k) * 0.55F * fade, rgb);
            if (++n > 4000) break;
            if (!up && rnd.nextInt(40) == 0 && Minecraft.getInstance().level != null)     // sparks jump off the casing as it powers down
                Minecraft.getInstance().level.addParticle(com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(),
                        bp.getX() + rnd.nextDouble(), bp.getY() + rnd.nextDouble(), bp.getZ() + rnd.nextDouble(), 0, 0.05, 0);
        }
        // the controller lights first
        if (up && since < 0.4) cube(bb, o, (float) (1 - since / 0.4) * 0.8F, 0xFFFFFF);
        draw(bb);
    }

    // ------------------------------------------------------------------ containment failure

    static final int MELTDOWN = com.theoreticalsciences.machine.ControllerBlockEntity.MELTDOWN_TICKS;
    static final int[] HAZARD_RGB = {0x80FFE0, 0x8060FF, 0x40FF80, 0xFFC23D, 0xFF60FF};

    /**
     * A multiblock losing containment: for four seconds every block of the structure flickers red, faster and faster,
     * while cracks of light split out of its heart and the core swells; then a white flash and a shockwave ring in
     * the colour of what is escaping.
     */
    private static void meltdown(ControllerBlockEntity c, double tick, long start, int hazard) {
        double t = tick - start;
        PowerAnimator.State ps = PowerAnimator.update(c, tick);
        int col = HAZARD_RGB[Math.max(0, Math.min(HAZARD_RGB.length - 1, hazard))];
        Vec3 ctr = c.effectCenter();
        FX_A = 1F;
        if (t < MELTDOWN) {
            float k = (float) (t / MELTDOWN);
            float pulse = 0.5F + 0.5F * (float) Math.sin(t * (0.25 + k * 1.4));
            beginAdditive();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int n = 0;
            long frame = (long) (t / 2);
            for (net.minecraft.core.BlockPos bp : ps.parts) {
                long h = (bp.asLong() * 6364136223846793005L + frame * 1442695040888963407L) >>> 58;
                float a = pulse * (0.12F + 0.45F * k) * (h < 20 ? 1.6F : 1F);
                cube(bb, bp, Math.min(0.9F, a), h < 6 ? col : 0xFF2018);
                if (++n > 4000) break;
            }
            draw(bb);
            beginAdditive();
            BufferBuilder rb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Random rnd = new Random(c.getBlockPos().asLong());
            int cracks = 4 + (int) (k * 32);
            for (int i = 0; i < cracks; i++) {
                Vec3 dir = new Vec3(rnd.nextGaussian(), rnd.nextGaussian() * 0.6, rnd.nextGaussian()).normalize();
                double len = (0.4 + rnd.nextDouble()) * (ps.maxDist + 2) * Math.min(1, k * 1.4 + 0.1);
                float flick = 0.6F + 0.4F * (float) Math.sin(t * 0.9 + i * 1.7);
                Vec3 mid = ctr.add(dir.scale(len * 0.5)).add(rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.4);
                ribbon(rb, ctr, mid, 0.12F + 0.25F * k, 0xFFFFFF, flick, col, flick * 0.7F);
                ribbon(rb, mid, ctr.add(dir.scale(len)), 0.08F + 0.2F * k, col, flick * 0.7F, col, 0F);
            }
            draw(rb);
            glow(ctr, 1.2F + 3.5F * k + 0.4F * pulse, col, 0.35F + 0.55F * k);
            glow(ctr, 0.5F + 1.5F * k, 0xFFFFFF, 0.3F + 0.6F * k * pulse);
            // a singularity machine keeps showing its black hole while containment fails - swelling and unstable, until the
            // rupture (which starts at exactly this size) takes over
            if (hazard == com.theoreticalsciences.machine.tuning.TuningProfile.Hazard.SPATIAL.ordinal()) {
                float wob = 1F + 0.08F * (float) Math.sin(t * (0.6 + k * 2.4));
                rupture(ctr, (0.6F + (com.theoreticalsciences.entity.ContainmentRupture.START_RADIUS - 0.6F) * k) * wob, 0.5F * k, 0F, 1.5F);
            }
        } else {
            float k = (float) Math.min(1, (t - MELTDOWN) / 45.0);
            float reach = (float) (ps.maxDist * 2.5 + 10);
            glow(ctr, 2 + k * reach, 0xFFFFFF, (1 - k) * (1 - k));
            glow(ctr, 1 + k * reach * 0.6F, col, (1 - k) * 0.8F);
            beginAdditive();
            BufferBuilder rb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int ring = 0; ring < 2; ring++) {
                double r = k * reach * (ring == 0 ? 1 : 0.7);
                double y = ctr.y + (ring == 0 ? 0 : 0.6);
                int seg = 72;
                for (int i = 0; i < seg; i++) {
                    double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
                    ribbon(rb, new Vec3(ctr.x + Math.cos(a0) * r, y, ctr.z + Math.sin(a0) * r), new Vec3(ctr.x + Math.cos(a1) * r, y, ctr.z + Math.sin(a1) * r),
                            0.9F * (1 - k) + 0.2F, ring == 0 ? 0xFFFFFF : col, 1 - k, col, 1 - k);
                }
            }
            draw(rb);
        }
    }

    /** How hard the camera shakes for someone near a meltdown (0 = not at all). */
    public static float shake(Vec3 eye, double tick) {
        float best = 0;
        for (var en : ClientHooks.FAILURES.entrySet()) {
            double t = tick - en.getValue()[0];
            double d = Math.sqrt(Vec3.atCenterOf(en.getKey()).distanceToSqr(eye));
            if (d > 64) continue;
            float amp = t < MELTDOWN ? (float) (t / MELTDOWN) * 0.6F : t < MELTDOWN + 20 ? 2.5F * (float) (1 - (t - MELTDOWN) / 20) : 0;
            best = Math.max(best, amp * (float) (1 - d / 64));
        }
        return best;
    }

    private static int brighten(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return (Math.min(255, r + (255 - r) / 2) << 16) | (Math.min(255, g + (255 - g) / 2) << 8) | Math.min(255, b + (255 - b) / 2);
    }

    /** A glowing shell slightly larger than the block. */
    private static void cube(BufferBuilder bb, net.minecraft.core.BlockPos p, float a, int rgb) {
        double e = 0.02, x0 = p.getX() - e, y0 = p.getY() - e, z0 = p.getZ() - e, x1 = p.getX() + 1 + e, y1 = p.getY() + 1 + e, z1 = p.getZ() + 1 + e;
        v(bb, x0, y0, z0, rgb, a); v(bb, x1, y0, z0, rgb, a); v(bb, x1, y0, z1, rgb, a); v(bb, x0, y0, z1, rgb, a);
        v(bb, x0, y1, z0, rgb, a); v(bb, x0, y1, z1, rgb, a); v(bb, x1, y1, z1, rgb, a); v(bb, x1, y1, z0, rgb, a);
        v(bb, x0, y0, z0, rgb, a); v(bb, x0, y1, z0, rgb, a); v(bb, x1, y1, z0, rgb, a); v(bb, x1, y0, z0, rgb, a);
        v(bb, x0, y0, z1, rgb, a); v(bb, x1, y0, z1, rgb, a); v(bb, x1, y1, z1, rgb, a); v(bb, x0, y1, z1, rgb, a);
        v(bb, x0, y0, z0, rgb, a); v(bb, x0, y0, z1, rgb, a); v(bb, x0, y1, z1, rgb, a); v(bb, x0, y1, z0, rgb, a);
        v(bb, x1, y0, z0, rgb, a); v(bb, x1, y1, z0, rgb, a); v(bb, x1, y1, z1, rgb, a); v(bb, x1, y0, z1, rgb, a);
    }

    // ------------------------------------------------------------------ miniatures, hyperobjects, proofs

    /** Draws something scaled about a centre, carrying the scale over to the lenses and foreground halves it queues. */
    private static void scaled(Vec3 ctr, float scale, Matrix4fStack mvs, Runnable body) {
        int lensStart = LENSES.size(), fgStart = FOREGROUND.size();
        Vec3 rel = ctr.subtract(cam);
        mvs.pushMatrix();
        mvs.translate((float) rel.x, (float) rel.y, (float) rel.z);
        mvs.scale(scale);
        mvs.translate((float) -rel.x, (float) -rel.y, (float) -rel.z);
        RenderSystem.applyModelViewMatrix();
        try {
            body.run();
        } finally {
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        for (int i = lensStart; i < LENSES.size(); i++) {
            Lens l = LENSES.get(i);
            LENSES.set(i, new Lens(l.pos(), l.worldRadius() * scale, l.quadRadius() * scale, l.mode(), l.strength()));
        }
        for (int i = fgStart; i < FOREGROUND.size(); i++) {
            Runnable r = FOREGROUND.get(i);
            FOREGROUND.set(i, () -> {
                mvs.pushMatrix();
                mvs.translate((float) rel.x, (float) rel.y, (float) rel.z);
                mvs.scale(scale);
                mvs.translate((float) -rel.x, (float) -rel.y, (float) -rel.z);
                RenderSystem.applyModelViewMatrix();
                try {
                    r.run();
                } finally {
                    mvs.popMatrix();
                    RenderSystem.applyModelViewMatrix();
                }
            });
        }
    }

    private static void drawDecor(double tick, Matrix4fStack mvs, net.minecraft.client.renderer.culling.Frustum frustum, float partial) {
        Minecraft mc = Minecraft.getInstance();
        // display miniatures: nearest 24 within 48 blocks
        List<com.theoreticalsciences.transcend.DecorBlockEntity> shown = new ArrayList<>();
        for (var be : List.copyOf(ClientHooks.DECOR)) {
            if (be.isRemoved() || be.getLevel() != mc.level || !(be instanceof com.theoreticalsciences.transcend.DecorBlockEntity d)) continue;
            Vec3 p = Vec3.atCenterOf(be.getBlockPos());
            if (p.distanceToSqr(cam) > 48 * 48 || !frustum.isVisible(new AABB(p, p).inflate(1.5))) continue;
            shown.add(d);
        }
        shown.sort(java.util.Comparator.comparingDouble(d -> Vec3.atCenterOf(d.getBlockPos()).distanceToSqr(cam)));
        if (shown.size() > 24) shown = shown.subList(0, 24);
        for (var d : shown) {
            Vec3 p = Vec3.atCenterOf(d.getBlockPos()).add(0, 0.45, 0);
            switch (d.kind()) {
                case BLACK_HOLE -> scaled(p, 0.17F, mvs, () -> {
                    accretionDisk(p, 0.75F, 2.7F, 0.18F, false, 0);
                    FOREGROUND.add(() -> accretionDisk(p, 0.75F, 2.7F, 0.18F, true, 0));
                    LENSES.add(new Lens(p, 0.42F, 3.4F, 0, 1.0F));
                });
                case WHITE_HOLE -> scaled(p, 0.18F, mvs, () -> {
                    jets(p, 2.6F, 0xFFF2D0);
                    glow(p, 1.8F, 0xFFF6E0, 0.7F);
                    LENSES.add(new Lens(p, 0.38F, 2.8F, 1, 1.0F));
                });
                case WORMHOLE -> scaled(p, 0.3F, mvs, () -> {
                    bubbleRim(p, 1.25F, 0x80C0FF);
                    LENSES.add(new Lens(p, 0.75F, 2.8F, 4, 1.0F));
                });
                case POCKET_UNIVERSE -> bubbleUniverse(p);
                case TESSERACT -> scaled(p, 0.3F, mvs, () -> tesseract(p, 1.15F));
                case PULSAR -> scaled(p, 0.3F, mvs, () -> {
                    pulsar(p);
                    LENSES.add(new Lens(p, 0.25F, 1.4F, 2, 0.8F));
                });
                case GALAXY -> scaled(p, 0.22F, mvs, () -> galaxy(p, d.getBlockPos().asLong()));
                case QUASAR -> scaled(p, 0.2F, mvs, () -> {
                    accretionDisk(p, 0.3F, 2.2F, 0.3F, false, 0);
                    FOREGROUND.add(() -> accretionDisk(p, 0.3F, 2.2F, 0.3F, true, 0));
                    jets(p, 3.4F, 0xA0C8FF);
                    glow(p, 0.8F, 0xFFFFFF, 0.9F);
                });
                case DYSON_SPHERE -> scaled(p, 0.2F, mvs, () -> {
                    glow(p, 1.4F, 0xFFE8A0, 1.0F);
                    glow(p, 0.5F, 0xFFFFFF, 1.0F);
                    dyson(p, 48, 2);
                });
                case PENTERACT -> scaled(p, 0.28F, mvs, () -> penteract(p, 1.1F, 1F));
            }
        }
        // collapsed Omnilex Engines
        for (var en : List.copyOf(ClientHooks.HYPEROBJECTS)) {
            if (en.isRemoved() || en.level() != mc.level || en.distanceToSqr(cam) > 256 * 256) {
                ClientHooks.HYPEROBJECTS.remove(en);
                continue;
            }
            if (!(en instanceof com.theoreticalsciences.transcend.HyperobjectEntity h)) continue;
            Vec3 c = h.getPosition(partial);
            float age = h.age() + partial;
            float grow = age < com.theoreticalsciences.transcend.HyperobjectEntity.COLLAPSE_TICKS
                    ? 0.4F + 2.6F * age / com.theoreticalsciences.transcend.HyperobjectEntity.COLLAPSE_TICKS : 3.0F;
            boolean pulling = age < com.theoreticalsciences.transcend.HyperobjectEntity.PULL_TICKS;
            if (h.sealed()) {             // the Transcendental is closed: a dim, still shadow of itself
                penteract(c, grow * 0.8F, 0.18F);
                glow(c, grow * 1.2F, 0x6A5A80, 0.08F);
                continue;
            }
            penteract(c, grow, pulling ? 1.0F : 0.7F);
            glow(c, grow * 1.6F, 0xFFFFFF, pulling ? 0.35F : 0.2F);
            LENSES.add(new Lens(c, 0.25F * grow, 1.8F * grow, 4, 1.0F));
        }
        // Axiom Sentinels proving something: a line of light grows from the sentinel to its target
        for (var en : List.copyOf(ClientHooks.SENTINELS)) {
            if (en.isRemoved() || en.level() != mc.level || !(en instanceof com.theoreticalsciences.entity.AxiomSentinel sn)) {
                ClientHooks.SENTINELS.remove(en);
                continue;
            }
            int ch = sn.charge();
            var t = mc.level.getEntity(sn.targetId());
            if (ch <= 0 || t == null) continue;
            float f = Math.min(1F, (ch + partial) / com.theoreticalsciences.entity.AxiomSentinel.CHARGE_TICKS);
            Vec3 a = sn.getPosition(partial).add(0, sn.getBbHeight() / 2, 0), b = t.getPosition(partial).add(0, t.getBbHeight() / 2, 0);
            Vec3 tip = a.lerp(b, f);
            beginAdditive();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            ribbon(bb, a, tip, 0.05F + 0.1F * f, 0xFFFFFF, 0.9F, 0xFFE080, 0.9F);
            ribbon(bb, a, tip, 0.3F * f, 0x80C0FF, 0.25F, 0xFFE080, 0.25F);
            draw(bb);
            glow(tip, 0.3F + 0.4F * f, 0xFFF0C0, 0.8F);
        }
    }

    /** A spiral galaxy: two logarithmic arms of stars around a bright bulge, turning slowly. */
    private static void galaxy(Vec3 c, long seed) {
        Random r = new Random(seed);
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float spin = time * 0.15F;
        for (int i = 0; i < 260; i++) {
            int arm = i & 1;
            double t = r.nextDouble();
            double rad = 0.2 + t * 2.4, ang = arm * Math.PI + Math.log(rad + 0.3) * 2.6 + spin + r.nextGaussian() * 0.25;
            double y = r.nextGaussian() * 0.06 * (1.2 - t);
            Vec3 p = c.add(Math.cos(ang) * rad, y, Math.sin(ang) * rad);
            int col = t < 0.25 ? 0xFFE0A0 : (r.nextInt(4) == 0 ? 0xFF90C0 : 0xA0C0FF);
            glowInto(pts, p, 0.05F + (float) (0.06 * (1 - t)), col, 0.7F);
        }
        draw(pts);
        glow(c, 0.9F, 0xFFE8B0, 0.8F);
    }

    /** A 5-cube (penteract): 32 vertices, 80 edges, rotating in three planes at once and projected 5D -> 4D -> 3D. */
    private static void penteract(Vec3 c, float scale, float alpha) {
        double[][] v = new double[32][5];
        double a1 = time * 0.5, a2 = time * 0.37, a3 = time * 0.23;
        float[][] p3 = new float[32][4];
        for (int i = 0; i < 32; i++) {
            for (int k = 0; k < 5; k++) v[i][k] = ((i >> k) & 1) == 0 ? -1 : 1;
            rot(v[i], 0, 3, a1);
            rot(v[i], 1, 4, a2);
            rot(v[i], 2, 4, a3);
            rot(v[i], 0, 2, a1 * 0.5);
            double f5 = 3.0 / (3.6 - v[i][4]);
            double x = v[i][0] * f5, y = v[i][1] * f5, z = v[i][2] * f5, w = v[i][3] * f5;
            double f4 = 2.4 / (3.4 - w);
            p3[i] = new float[]{(float) (x * f4), (float) (y * f4), (float) (z * f4), (float) v[i][4]};
        }
        int[] hues = {0x60E0FF, 0xFF60D0, 0xFFD060, 0x80FF90, 0xC090FF};
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = scale * 0.6F;
        for (int i = 0; i < 32; i++)
            for (int k = 0; k < 5; k++) {
                int j = i ^ (1 << k);
                if (j < i) continue;
                Vec3 pa = c.add(p3[i][0] * s, p3[i][1] * s, p3[i][2] * s), pb = c.add(p3[j][0] * s, p3[j][1] * s, p3[j][2] * s);
                ribbon(bb, pa, pb, 0.04F * scale, hues[k], 0.95F * alpha, hues[k], 0.95F * alpha);
                ribbon(bb, pa, pb, 0.16F * scale, hues[k], 0.15F * alpha, hues[k], 0.15F * alpha);
            }
        draw(bb);
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (float[] p : p3) glowInto(pts, c.add(p[0] * s, p[1] * s, p[2] * s), 0.08F * scale, 0xFFFFFF, 0.8F * alpha);
        draw(pts);
    }

    private static void rot(double[] v, int i, int j, double a) {
        double ca = Math.cos(a), sa = Math.sin(a), x = v[i], y = v[j];
        v[i] = x * ca - y * sa;
        v[j] = x * sa + y * ca;
    }

    /** Vacuum Fluctuation Harvester: virtual pairs flicker into existence around the core and annihilate. */
    private static void foam(Vec3 c) {
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Random r = new Random(77);
        for (int i = 0; i < 60; i++) {
            double life = (time * (0.6 + r.nextDouble()) + r.nextDouble()) % 1.0;
            double th = r.nextDouble() * Math.PI, ph = r.nextDouble() * Math.PI * 2, rad = 0.6 + r.nextDouble() * 2.0;
            Vec3 mid = c.add(Math.sin(th) * Math.cos(ph) * rad, Math.cos(th) * rad, Math.sin(th) * Math.sin(ph) * rad);
            double sep = Math.sin(life * Math.PI) * 0.25;
            Vec3 off = new Vec3(Math.cos(ph + 1.3), 0.3, Math.sin(ph + 1.3)).scale(sep);
            float a = (float) Math.sin(life * Math.PI);
            glowInto(pts, mid.add(off), 0.09F, 0x80F0FF, a);
            glowInto(pts, mid.subtract(off), 0.09F, 0xFF80E0, a);
            if (life > 0.92) glowInto(pts, mid, 0.3F, 0xFFFFFF, (float) ((1 - life) / 0.08));
        }
        draw(pts);
        glow(c, 1.2F, 0x80F0FF, 0.35F);
        bubbleRim(c, 1.6F, 0x80F0FF);
    }

    // ------------------------------------------------------------------ per-machine effects

    /** What the Scepter of Singularities made: it grows in, lives, and collapses back to nothing. */
    private static void scepterSingularity(ClientHooks.SingularityFx fx, float age, Matrix4fStack mvs) {
        float life = fx.ticks();
        float in = Math.min(1F, age / 8F), out = Math.min(1F, (life - age) / 8F);
        float env = Math.max(0F, Math.min(easeOutBack(in), out));
        if (env <= 0.01F) return;
        Vec3 p = fx.pos();
        switch (fx.kind()) {
            case 0 -> scaled(p, 2.6F * env, mvs, () -> {           // black hole: disc, shadow, light bent round it
                accretionDisk(p, 0.75F, 2.7F, 0.25F, false, 0);
                FOREGROUND.add(() -> accretionDisk(p, 0.75F, 2.7F, 0.25F, true, 0));
                LENSES.add(new Lens(p, 0.42F, 3.4F, 0, 1.0F));
            });
            case 1 -> scaled(p, 2.4F * env, mvs, () -> {           // white hole: everything pours out
                jets(p, 2.6F, 0xFFF2D0);
                glow(p, 1.8F, 0xFFF6E0, 0.8F);
                glow(p, 0.6F, 0xFFFFFF, 1.0F);
                LENSES.add(new Lens(p, 0.38F, 2.8F, 1, 1.0F));
            });
            case 2 -> scaled(p, 1.8F * env, mvs, () -> {           // tesseract: a turning 4-cube
                tesseract(p, 1.15F, time * 1.6F);
                glow(p, 0.8F, 0x80E0FF, 0.5F);
            });
            case 3 -> scaled(p, 1.6F * env, mvs, () -> {           // entanglement: two particles that are one
                double a = time * 4;
                Vec3 q1 = p.add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2), q2 = p.add(-Math.cos(a) * 1.2, 0, -Math.sin(a) * 1.2);
                glow(q1, 0.5F, 0xFF80FF, 0.9F);
                glow(q2, 0.5F, 0x80FFFF, 0.9F);
                ring3D(p, 1.2F, 0xC080FF, time * 2F, 0.3F, 0.05F);
                ring3D(p, 1.2F, 0x80C0FF, -time * 2F, -0.3F, 0.05F);
            });
            default -> {                                            // big bang: a point that becomes a universe
                float g = age / life;
                scaled(p, 1F, mvs, () -> {
                    glow(p, 0.4F + 9F * g, 0xFFF0C0, (1 - g) * 0.9F);
                    glow(p, 0.3F + 3F * g, 0xFFFFFF, (1 - g));
                    shell(p, 0.5F + 7F * g, 0xFFB060, time, 0.4F, 16);
                    if (g < 0.3F) LENSES.add(new Lens(p, 0.3F + 2F * g, 2.5F + 6F * g, 1, 1 - g / 0.3F));
                });
            }
        }
    }

    private static void drawEffect(ControllerBlockEntity c) {
        Vec3 p = c.effectCenter();
        switch (c.type.effect) {
            case BLACK_HOLE -> {
                glow(p, 1.3F, 0xFFEFC0, 0.25F);
                LENSES.add(new Lens(p, 0.32F, 2.6F, 0, 1.0F));
            }
            case BLACK_HOLE_DISK -> {
                accretionDisk(p, 0.75F, 2.7F, 0.18F, false, 0);
                FOREGROUND.add(() -> accretionDisk(p, 0.75F, 2.7F, 0.18F, true, 0));
                LENSES.add(new Lens(p, 0.42F, 3.4F, 0, 1.0F));
            }
            case WHITE_HOLE -> {
                jets(p, 2.6F, 0xFFF2D0);
                glow(p, 1.8F, 0xFFF6E0, 0.7F);
                LENSES.add(new Lens(p, 0.38F, 2.8F, 1, 1.0F));
            }
            case DARK_LENS -> LENSES.add(new Lens(p, 0.9F, 2.4F, 2, 1.0F));
            case TESSERACT -> tesseract(p, 1.15F);
            case COLLIDER -> colliderBeams(p, 3.0F);
            case RIFT -> {
                rift(p);
                LENSES.add(new Lens(p, 0.7F, 2.6F, 3, 1.0F));
            }
            case POCKET_UNIVERSE -> {
                accretionDisk(p, 0.05F, 1.35F, 0.0F, false, 2);
                FOREGROUND.add(() -> accretionDisk(p, 0.05F, 1.35F, 0.0F, true, 2));
                bubbleRim(p, 1.5F, 0xFFD080);
                LENSES.add(new Lens(p, 0.8F, 1.9F, 2, 0.6F));
            }
            case MATRIOSHKA -> {
                glow(p, 1.0F, 0xFFF0B0, 0.9F);
                shell(p, 1.0F, 0x60FF9A, time * 0.4F, 0.3F, 10);
                shell(p, 1.6F, 0x60E0FF, -time * 0.25F, 1.1F, 14);
                shell(p, 2.2F, 0xFFE060, time * 0.15F, -0.6F, 18);
            }
            case DYSON -> {
                glow(p, 1.4F, 0xFFE8A0, 1.0F);
                glow(p, 0.5F, 0xFFFFFF, 1.0F);
                if (c instanceof DysonControllerBlockEntity d) dyson(p, d.satellites(), d.shells());
            }
            case ANNIHILATION -> {
                float pulse = 0.6F + 0.4F * (float) Math.sin(time * 9);
                glow(p, 1.6F * pulse, 0xFF60D0, 0.6F);
                glow(p, 0.45F, 0xFFFFFF, 1.0F);
                ring3D(p, 1.25F, 0xFF40C0, time * 1.5F, 0.4F, 0.06F);
                ring3D(p, 1.25F, 0x80C0FF, -time * 1.5F, -0.4F, 0.06F);
                if ((time * 1.3F) % 1 < 0.08F) glow(p, 2.6F, 0xFFFFFF, 0.8F);
            }
            case PULSAR -> {
                pulsar(p);
                LENSES.add(new Lens(p, 0.25F, 1.4F, 2, 0.8F));
            }
            case WORMHOLE -> {
                bubbleRim(p, 1.25F, 0x80C0FF);
                LENSES.add(new Lens(p, 0.75F, 2.8F, 4, 1.0F));
            }
            case TIME_LOOP -> timeLoop(p);
            case EXPANSION -> expansion(p);
            case MATRIX -> {
                digitalRain(p);
                LENSES.add(new Lens(p, 0.8F, 2.2F, 5, 1.0F));
            }
            case ASSEMBLY -> assemblyLine(p, c.facing());
            case PLASMA -> {
                accretionDisk(p, 0.2F, 1.6F, 0.0F, false, 0);
                FOREGROUND.add(() -> accretionDisk(p, 0.2F, 1.6F, 0.0F, true, 0));
                glow(p, 1.4F, 0xFF8040, 0.6F);
                glow(p, 0.5F, 0xFFFFFF, 0.9F);
                ring3D(p, 1.8F, 0xFFB060, time * 2F, 0.0F, 0.05F);
            }
            case FRACTIONATION -> fractionation(p);
            case SYNCHROTRON -> colliderBeams(p, 6.5F);
            case CONCEPT -> {
                // whole fields of knowledge falling into one point: nine streams converge, reality shatters around the core
                glow(p, 2.6F, 0xC0C0FF, 0.35F);
                glow(p, 0.5F, 0xFFFFFF, 1.0F);
                for (int k = 0; k < 3; k++) ring3D(p, 2.2F + k * 0.5F, k == 0 ? 0xFFFFFF : k == 1 ? 0xA0C0FF : 0xFFC0F0, time * (1.2F - k * 0.3F), k * 1.1F - 1.1F, 0.05F);
                LENSES.add(new Lens(p, 0.5F, 3.2F, 6, 0.8F));
            }
            case STAR_FORGE -> {
                // a hypernova in a bottle: white-hot core, four plasma jets feeding it, two crossed confinement rings
                float pulse = 0.75F + 0.25F * (float) Math.sin(time * 5);
                glow(p, 3.2F * pulse, 0xFF4080, 0.45F);
                glow(p, 1.4F, 0xFFD0E0, 0.9F);
                glow(p, 0.6F, 0xFFFFFF, 1.0F);
                accretionDisk(p, 0.4F, 2.6F, 0.0F, false, 0);
                FOREGROUND.add(() -> accretionDisk(p, 0.4F, 2.6F, 0.0F, true, 0));
                ring3D(p, 3.0F, 0xFF6090, time * 2.2F, 0.5F, 0.08F);
                ring3D(p, 3.0F, 0xFFC060, -time * 2.2F, -0.5F, 0.08F);
                jets(p, 3.5F, 0xFFB0C0);
            }
            case EYE -> {
                // a black hole the size of a room, with an accretion disk, lensing and three slow orbital rings
                accretionDisk(p, 1.3F, 5.2F, 0.22F, false, 0);
                FOREGROUND.add(() -> accretionDisk(p, 1.3F, 5.2F, 0.22F, true, 0));
                ring3D(p, 6.2F, 0xFFB040, time * 0.4F, 0.9F, 0.1F);
                ring3D(p, 6.6F, 0x80C0FF, -time * 0.3F, -0.7F, 0.1F);
                ring3D(p, 7.0F, 0xC080FF, time * 0.2F, 0.2F, 0.1F);
                LENSES.add(new Lens(p, 0.8F, 6.5F, 0, 1.0F));
            }
            case BREACH -> breach(p);
            case OMNILEX -> omnilex(p);
            case FOAM -> foam(p);
            default -> {}
        }
    }

    /** Welding sparks and laser heads travelling down the assembly line. */
    private static void assemblyLine(Vec3 c, net.minecraft.core.Direction facing) {
        Vec3 dir = new Vec3(-facing.getStepX(), 0, -facing.getStepZ());   // the line runs away from the controller's front
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int k = -3; k <= 3; k++) {
            Vec3 head = c.add(dir.scale(k * 3)).add(0, 1.0, 0);
            float on = (float) Math.max(0, Math.sin(time * 5 + k * 1.7));
            if (on > 0.2F) ribbon(bb, head, head.add(0, -1.4, 0), 0.08F, 0x60FFD0, on, 0xFFFFFF, on * 0.6F);
        }
        float travel = (time * 2.5F) % 18F - 9F;
        ribbon(bb, c.add(dir.scale(-9)).add(0, -0.4, 0), c.add(dir.scale(9)).add(0, -0.4, 0), 0.04F, 0x60FFD0, 0.25F, 0x60FFD0, 0.25F);
        draw(bb);
        glow(c.add(dir.scale(travel)).add(0, -0.3, 0), 0.4F, 0xA0FFE0, 0.8F);
    }

    /** Fractions rising up the spire, each condensing out at its own height and colour. */
    private static void fractionation(Vec3 c) {
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int[] cols = {0x6A55FF, 0xFF40C0, 0xFFD080, 0xFFC23D};
        for (int i = 0; i < 24; i++) {
            float life = (time * 0.4F + i / 24F) % 1F;
            int band = i % 4;
            double y = -7 + life * (3 + band * 3.5);
            glowInto(pts, c.add(Math.sin(i * 2.1 + time) * 0.4, y, Math.cos(i * 2.1 + time) * 0.4), 0.18F, cols[band], 1 - life * 0.6F);
        }
        draw(pts);
    }

    /** A containment rupture: a growing black horizon in a violet maelstrom, flickering faster as detonation nears. */
    /**
     * A spacetime rupture. While it grows: a black hole with an accretion disk, lensing the view behind it. While it
     * collapses (q 0 -> 1): the horizon shrinks to a point, the disk is swallowed, rings of light rush inward, and the
     * lensing spreads outward and deepens - space itself is dragged in - until the flash at the end.
     */
    private static void rupture(Vec3 c, float r, float progress, float q, float peak) {
        if (q <= 0) {
            float flicker = 0.7F + 0.3F * (float) Math.sin(time * (6 + progress * 30));
            glow(c, r * 3.2F, 0x6020C0, 0.35F * flicker);
            accretionDisk(c, r * 1.05F, r * 2.8F, 0.35F + 0.2F * (float) Math.sin(time * 0.7), false, 1);
            FOREGROUND.add(() -> accretionDisk(c, r * 1.05F, r * 2.8F, 0.35F + 0.2F * (float) Math.sin(time * 0.7), true, 1));
            ring3D(c, r * 1.6F, 0xD080FF, time * 2.5F, 0.8F, 0.06F + r * 0.01F);
            if (progress > 0.85F) glow(c, r * (1 + (progress - 0.85F) * 8), 0xD0B0FF, Math.min(0.6F, (progress - 0.85F) * 4));
            LENSES.add(new Lens(c, Math.max(0.2F, r * 0.75F), Math.max(1.5F, r * 3.5F), 0, 1.2F));
            return;
        }
        float inner = Math.max(0.02F, r * 1.05F), outer = Math.max(0.05F, r * 2.8F * (1 - q * 0.5F));
        float spin = 0.35F + 0.2F * (float) Math.sin(time * (0.7 + q * 6));
        accretionDisk(c, inner, outer, spin, false, 1);
        FOREGROUND.add(() -> accretionDisk(c, inner, outer, spin, true, 1));
        glow(c, peak * 3.2F * (1 - q * 0.7F), 0x6020C0, 0.35F + 0.4F * q);
        // shock rings converging on the singularity, each one faster than the last
        for (int i = 0; i < 3; i++) {
            float ph = (float) ((time * (1.2 + q * 3) + i / 3.0) % 1.0);
            float rr = peak * 4.5F * (1 - ph) * (1 - q * 0.4F) + 0.1F;
            ring3D(c, rr, i == 1 ? 0xFFFFFF : 0xD080FF, time * 4F + i, 0.3F + i * 0.5F, 0.04F + 0.08F * ph);
        }
        // the last instant: everything is a point of light
        if (q > 0.8F) glow(c, 0.3F + (q - 0.8F) * peak * 6, 0xFFFFFF, (q - 0.8F) * 5);
        // the horizon shrinks while the region of bent space widens and bends harder
        LENSES.add(new Lens(c, Math.max(0.05F, r * 0.75F), Math.max(1.5F, peak * 3.5F * (1 + 1.5F * q)), 0, 1.2F + 2.6F * q));
    }

    /** A universe in a bubble: a tiny spiral galaxy inside a shimmering membrane. */
    private static void bubbleUniverse(Vec3 c) {
        float seed = (float) ((c.x * 13.1 + c.y * 7.7 + c.z * 3.3) % 6.283);
        accretionDisk(c, 0.02F, 0.42F, 0.4F + 0.3F * (float) Math.sin(seed), false, 2);
        accretionDisk(c, 0.02F, 0.42F, 0.4F + 0.3F * (float) Math.sin(seed), true, 2);
        bubbleRim(c, 0.5F, 0xA0C8FF);
        glow(c, 0.9F, 0x8090FF, 0.18F);
    }

    /** Axiomatic breach: reality shatters around a white-hot tear where every law fails at once. */
    private static void breach(Vec3 c) {
        glow(c, 2.4F, 0xFFF0C0, 0.35F);
        glow(c, 0.7F, 0xFFFFFF, 1.0F);
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(31337);
        for (int i = 0; i < 26; i++) {
            // jagged cracks radiating from the breach, flickering in and out of existence
            double th = Math.acos(2 * rnd.nextDouble() - 1), ph = rnd.nextDouble() * Math.PI * 2;
            float on = (float) Math.max(0, Math.sin(time * (1.5 + rnd.nextDouble() * 3) + i));
            if (on < 0.15F) continue;
            Vec3 prev = c;
            double len = 1.4 + rnd.nextDouble() * 1.6;
            for (int k = 1; k <= 4; k++) {
                double rr = len * k / 4;
                Vec3 nx = sph(c, rr, th + (rnd.nextDouble() - 0.5) * 0.4, ph + (rnd.nextDouble() - 0.5) * 0.4, 0);
                ribbon(bb, prev, nx, 0.07F * (5 - k) / 4, 0xFFF4D0, on, i % 3 == 0 ? 0x80FFE0 : 0xD0A0FF, on * 0.6F);
                prev = nx;
            }
        }
        draw(bb);
        ring3D(c, 1.6F, 0xFFE0A0, time * 0.7F, 0.5F, 0.05F);
        ring3D(c, 1.9F, 0x80FFE0, -time * 0.4F, -0.9F, 0.04F);
        LENSES.add(new Lens(c, 0.55F, 3.2F, 6, 1.0F));
    }

    /** Omnilex: a hologram of the World Tree, bubble universes on its branches, over a churning ocean of law. */
    private static void omnilex(Vec3 c) {
        Vec3 sea = c.add(0, -1.6, 0);
        accretionDisk(sea, 0.0F, 2.8F, 0.0F, false, 3);
        FOREGROUND.add(() -> accretionDisk(sea, 0.0F, 2.8F, 0.0F, true, 3));
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        List<Vec3> tips = new ArrayList<>();
        holoBranch(bb, tips, sea, new Vec3(0, 1, 0), 1.25, 0, 5, 0.0);
        draw(bb);
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < tips.size(); i++) {
            float tw = 0.5F + 0.5F * (float) Math.sin(time * 2 + i * 1.7);
            glowInto(pts, tips.get(i), 0.13F + 0.05F * tw, i % 3 == 0 ? 0xFFD080 : 0xA0C8FF, 0.6F + 0.4F * tw);
        }
        // laws rising out of the ocean and popping
        Random rnd = new Random(99);
        for (int i = 0; i < 40; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 2.6;
            double life = (time * (0.3 + rnd.nextDouble() * 0.4) + rnd.nextDouble()) % 1.0;
            glowInto(pts, sea.add(Math.cos(a) * r, life * 1.2, Math.sin(a) * r), 0.06F + (float) life * 0.08F, rnd.nextBoolean() ? 0x80FFE0 : 0xFFE080, (float) (1 - life));
        }
        draw(pts);
        glow(c.add(0, 0.6, 0), 2.2F, 0x80FFE0, 0.15F);
    }

    private static void holoBranch(BufferBuilder bb, List<Vec3> tips, Vec3 a, Vec3 dir, double len, int depth, int maxDepth, double yaw) {
        double sway = Math.sin(time * 0.8 + depth * 1.3 + yaw) * 0.08;
        Vec3 d = dir.add(sway, 0, sway * 0.5).normalize();
        Vec3 b = a.add(d.scale(len));
        float w = 0.16F * (float) Math.pow(0.62, depth);
        ribbon(bb, a, b, w, 0x60FFC0, 0.75F, 0x80FFE0, 0.6F);
        ribbon(bb, a, b, w * 3, 0x60FFC0, 0.12F, 0x80FFE0, 0.08F);
        if (depth == maxDepth) {
            tips.add(b);
            return;
        }
        for (int k = 0; k < (depth == 0 ? 3 : 2); k++) {
            double ny = yaw + k * (depth == 0 ? 2.094 : Math.PI) + 0.7 + time * 0.05;
            Vec3 side = new Vec3(Math.cos(ny), 0, Math.sin(ny));
            Vec3 nd = d.scale(0.72).add(side.scale(0.62)).normalize();
            holoBranch(bb, tips, b, nd, len * 0.68, depth + 1, maxDepth, ny);
        }
    }

    private static void ring3D(Vec3 c, float radius, int rgb, float spin, float tilt, float width) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a0 = spin + i * Math.PI * 2 / seg, a1 = spin + (i + 1) * Math.PI * 2 / seg;
            float bright = 0.4F + 0.6F * (float) Math.max(0, Math.sin(i * 0.5 + time * 6));
            ribbon(bb, sph(c, radius, Math.PI / 2, a0, tilt), sph(c, radius, Math.PI / 2, a1, tilt), width, rgb, bright, rgb, bright);
        }
        draw(bb);
    }

    /** Rotating neutron star with two lighthouse beams along a tilted magnetic axis. */
    private static void pulsar(Vec3 c) {
        glow(c, 0.9F, 0xC0FFFF, 0.8F);
        glow(c, 0.25F, 0xFFFFFF, 1.0F);
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double spin = time * 7.0, tilt = 0.6;
        Vec3 axis = new Vec3(Math.sin(tilt) * Math.cos(spin), Math.cos(tilt), Math.sin(tilt) * Math.sin(spin));
        for (int s = -1; s <= 1; s += 2) {
            Vec3 tip = c.add(axis.scale(s * 3.2));
            ribbon(bb, c, tip, 0.35F, 0x80FFFF, 0.55F, 0x80FFFF, 0.0F);
            ribbon(bb, c, tip, 0.12F, 0xFFFFFF, 0.9F, 0xC0FFFF, 0.0F);
        }
        draw(bb);
        ring3D(c, 0.9F, 0x40A0FF, (float) spin * 0.3F, 0.2F, 0.03F);
    }

    /** Closed timelike curve: an object on a loop, with echoes ahead of and behind itself in time. */
    private static void timeLoop(Vec3 c) {
        ring3D(c, 1.1F, 0xFFC23D, time * 0.5F, 0.3F, 0.04F);
        ring3D(c, 0.8F, 0xFFE080, -time * 1.2F, -0.5F, 0.03F);
        ring3D(c, 1.4F, 0xC08A10, time * 0.2F, 1.2F, 0.025F);
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int k = -6; k <= 6; k++) {
            double s = time * 1.6 + k * 0.35;
            double x = 1.1 * Math.cos(s) / (1 + Math.sin(s) * Math.sin(s));
            double z = 1.1 * Math.sin(s) * Math.cos(s) / (1 + Math.sin(s) * Math.sin(s));
            float a = k == 0 ? 1.0F : 0.5F / Math.abs(k);
            glowInto(pts, c.add(x, 0.15 * Math.sin(s * 2), z), k == 0 ? 0.28F : 0.18F, k < 0 ? 0xFFC23D : 0x80C0FF, a);
        }
        draw(pts);
    }

    /** Dark energy: a lattice of points that keeps expanding away from the centre and fading out. */
    private static void expansion(Vec3 c) {
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float f = (time * 0.35F) % 1;
        for (int layer = 0; layer < 2; layer++) {
            float scale = 0.6F + (f + layer * 0.5F) % 1 * 1.6F;
            float alpha = 1.0F - ((f + layer * 0.5F) % 1);
            for (int x = -2; x <= 2; x++)
                for (int y = -2; y <= 2; y++)
                    for (int z = -2; z <= 2; z++) {
                        if (x * x + y * y + z * z > 6) continue;
                        glowInto(pts, c.add(x * scale * 0.5, y * scale * 0.5, z * scale * 0.5), 0.07F, 0x5070FF, alpha * 0.9F);
                    }
        }
        glowInto(pts, c, 0.7F, 0x203080, 0.5F);
        draw(pts);
    }

    /** Simulation hypothesis: columns of falling glyphs wrapped around the compiler core. */
    private static void digitalRain(Vec3 c) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(77);
        for (int col = 0; col < 28; col++) {
            double ang = col * Math.PI * 2 / 28 + time * 0.1;
            double rad = 1.5 + rnd.nextDouble() * 0.4;
            double speed = 0.8 + rnd.nextDouble() * 1.2, phase = rnd.nextDouble() * 4;
            double head = 2.0 - ((time * speed + phase) % 4.0);
            for (int g = 0; g < 8; g++) {
                double y = head + g * 0.18;
                if (y > 2.0 || y < -2.0) continue;
                Vec3 a = c.add(Math.cos(ang) * rad, y, Math.sin(ang) * rad);
                Vec3 b = a.add(0, 0.12, 0);
                float alpha = g == 0 ? 1.0F : 0.8F * (1 - g / 8F);
                ribbon(bb, a, b, 0.09F, g == 0 ? 0xD0FFE0 : 0x40FF80, alpha, 0x40FF80, alpha);
            }
        }
        draw(bb);
        glow(c, 0.9F, 0x60FF9A, 0.6F);
    }

    // ------------------------------------------------------------------ primitives

    private static void beginAdditive() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void draw(BufferBuilder bb) {
        MeshData mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    private static void v(BufferBuilder bb, double x, double y, double z, int rgb, float a) {
        bb.addVertex((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
                .setColor(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, a * FX_A);
    }

    /** Soft radial glow billboard (triangle fan baked into triangles). */
    private static void glow(Vec3 c, float radius, int rgb, float alpha) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        glowInto(bb, c, radius, rgb, alpha);
        draw(bb);
    }

    private static void glowInto(BufferBuilder bb, Vec3 c, float radius, int rgb, float alpha) {
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            v(bb, c.x, c.y, c.z, rgb, alpha);
            v(bb, c.x + (left.x * Math.cos(a0) + up.x * Math.sin(a0)) * radius, c.y + (left.y * Math.cos(a0) + up.y * Math.sin(a0)) * radius,
                    c.z + (left.z * Math.cos(a0) + up.z * Math.sin(a0)) * radius, rgb, 0);
            v(bb, c.x + (left.x * Math.cos(a1) + up.x * Math.sin(a1)) * radius, c.y + (left.y * Math.cos(a1) + up.y * Math.sin(a1)) * radius,
                    c.z + (left.z * Math.cos(a1) + up.z * Math.sin(a1)) * radius, rgb, 0);
        }
    }

    /** Camera-facing ribbon between two points. */
    private static void ribbon(BufferBuilder bb, Vec3 a, Vec3 b, float width, int rgbA, float alphaA, int rgbB, float alphaB) {
        Vec3 dir = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(view);
        double len = side.length();
        if (len < 1e-6) return;
        side = side.scale(width / 2 / len);
        v(bb, a.x - side.x, a.y - side.y, a.z - side.z, rgbA, alphaA);
        v(bb, a.x + side.x, a.y + side.y, a.z + side.z, rgbA, alphaA);
        v(bb, b.x + side.x, b.y + side.y, b.z + side.z, rgbB, alphaB);
        v(bb, b.x - side.x, b.y - side.y, b.z - side.z, rgbB, alphaB);
    }

    /** Keplerian accretion disk using the ts_accretion shader. {@code nearHalf} selects which half to draw (relative to camera). */
    private static void accretionDisk(Vec3 c, float inner, float outer, float tilt, boolean nearHalf, int palette) {
        ShaderInstance sh = QHShaders.ACCRETION;
        if (sh == null) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> sh);
        sh.safeGetUniform("Palette").set((float) palette);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int seg = 96, rings = 6;
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        Vec3 toCam = cam.subtract(c).normalize();
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg, am = (a0 + a1) / 2;
            Vec3 mid = new Vec3(Math.cos(am), -Math.cos(am) * st, Math.sin(am) * ct);
            boolean near = mid.dot(toCam) > 0;
            if (near != nearHalf) continue;
            // Doppler beaming: the side orbiting toward the camera is brighter and bluer
            Vec3 tangent = new Vec3(-Math.sin(am), Math.sin(am) * st, Math.cos(am) * ct);
            float beam = (float) Math.max(0.25, 1.0 + 0.75 * tangent.dot(toCam));
            float cr = Math.min(1, beam), cg = Math.min(1, 0.85F * beam), cb = Math.min(1, 0.7F * beam + (beam > 1 ? 0.25F : 0));
            for (int r = 0; r < rings; r++) {
                float r0 = inner + (outer - inner) * r / rings, r1 = inner + (outer - inner) * (r + 1) / rings;
                float v0 = (float) r / rings, v1 = (float) (r + 1) / rings;
                diskVert(bb, c, a0, r0, ct, st, (float) (a0 / (Math.PI * 2)), v0, cr, cg, cb);
                diskVert(bb, c, a0, r1, ct, st, (float) (a0 / (Math.PI * 2)), v1, cr, cg, cb);
                diskVert(bb, c, a1, r1, ct, st, (float) (a1 / (Math.PI * 2)), v1, cr, cg, cb);
                diskVert(bb, c, a1, r0, ct, st, (float) (a1 / (Math.PI * 2)), v0, cr, cg, cb);
            }
        }
        draw(bb);
    }

    private static void diskVert(BufferBuilder bb, Vec3 c, double ang, float rad, double ct, double st, float u, float v, float r, float g, float b) {
        double x = Math.cos(ang) * rad, z = Math.sin(ang) * rad;
        double y = -x * st;
        z = z * ct;
        bb.addVertex((float) (c.x + x - cam.x), (float) (c.y + y - cam.y), (float) (c.z + z - cam.z)).setUv(u, v).setColor(r * FX_A, g * FX_A, b * FX_A, FX_A);
    }

    private static void jets(Vec3 c, float length, int rgb) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float pulse = 0.6F + 0.4F * (float) Math.sin(time * 6);
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 3; k++) {
                float w = 0.5F - k * 0.14F;
                ribbon(bb, c, c.add(0, s * length * (0.7 + 0.15 * k), 0), w, rgb, 0.5F * pulse, rgb, 0.0F);
            }
        }
        draw(bb);
    }

    /**
     * Void Wraith: a living black hole (even entity id) or white hole (odd id), bending the light around it.
     * Law Wraith: a hypercube turning through the 4th dimension. Both collapse into a point as they die.
     */
    private static void wraith(net.minecraft.world.entity.Entity e, float partial) {
        int deathTime = e instanceof net.minecraft.world.entity.LivingEntity le ? le.deathTime : 0;
        float dying = deathTime > 0 ? Math.min(1F, (deathTime + partial) / 20F) : 0F;
        float k = 1F - dying * dying;                         // accelerating collapse
        Vec3 c = e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
        boolean hurt = e instanceof net.minecraft.world.entity.LivingEntity le && le.hurtTime > 0;
        if (e instanceof com.theoreticalsciences.entity.LawWraith) {
            if (k <= 0.02F) return;
            tesseract(c, 0.38F * k, time * (1 + dying * 8) + e.getId());
            if (hurt) glow(c, 0.9F, 0xFF4040, 0.3F);
            LENSES.add(new Lens(c, 0.2F * k, 1.0F * k, 3, 0.5F));
            return;
        }
        if ((e.getId() & 1) == 0) {
            float tilt = 0.3F + 0.15F * (float) Math.sin(time * 0.9 + e.getId());
            float inner = 0.22F * k, outer = 0.75F * k;
            if (k > 0.02F) {
                accretionDisk(c, inner, outer, tilt, false, 1);
                FOREGROUND.add(() -> accretionDisk(c, inner, outer, tilt, true, 1));
            }
            if (hurt) glow(c, 0.8F, 0xFF3030, 0.25F);
            if (dying > 0.85F) glow(c, 0.3F + (dying - 0.85F) * 12, 0xFFFFFF, (dying - 0.85F) * 6);
            LENSES.add(new Lens(c, Math.max(0.02F, 0.13F * k), Math.max(0.3F, 1.5F * k), 0, 1.0F));
        } else {
            if (k > 0.02F) {
                jets(c, 1.1F * k, 0xFFF2D0);
                glow(c, 0.8F * k, 0xFFF6E0, 0.6F);
            }
            if (hurt) glow(c, 0.8F, 0xFF3030, 0.25F);
            if (dying > 0.85F) glow(c, 0.3F + (dying - 0.85F) * 12, 0xFFFFFF, (dying - 0.85F) * 6);
            LENSES.add(new Lens(c, Math.max(0.02F, 0.14F * k), Math.max(0.3F, 1.3F * k), 1, 1.0F));
        }
    }

    private static void tesseract(Vec3 c, float scale) {
        tesseract(c, scale, time);
    }

    private static void tesseract(Vec3 c, float scale, float spin) {
        float[][] v = Tesseract4D.project(spin, 2.6F);
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = scale * 2.2F;
        for (int[] e : Tesseract4D.EDGES) {
            float[] a = v[e[0]], b = v[e[1]];
            Vec3 pa = c.add(a[0] * s, a[1] * s, a[2] * s), pb = c.add(b[0] * s, b[1] * s, b[2] * s);
            ribbon(bb, pa, pb, 0.06F, wColor(a[3]), 0.95F, wColor(b[3]), 0.95F);
            ribbon(bb, pa, pb, 0.22F, wColor(a[3]), 0.18F, wColor(b[3]), 0.18F);
        }
        draw(bb);
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (float[] p : v) glowInto(pts, c.add(p[0] * s, p[1] * s, p[2] * s), 0.18F, 0xFFFFFF, 0.8F);
        glowInto(pts, c, 0.9F, 0x80E0FF, 0.25F);
        draw(pts);
    }

    /** Cyan for -W, magenta for +W: the colour tells you how far "into" the 4th dimension a vertex is. */
    private static int wColor(float w) {
        float t = Math.max(0, Math.min(1, (w + 1.2F) / 2.4F));
        int r = (int) (64 + 191 * t), g = (int) (224 - 160 * t), b = 255;
        return (r << 16) | (g << 8) | b;
    }

    private static void colliderBeams(Vec3 c, float half) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vec3[] corners = {c.add(-half, 0, -half), c.add(half, 0, -half), c.add(half, 0, half), c.add(-half, 0, half)};
        for (int i = 0; i < 4; i++) ribbon(bb, corners[i], corners[(i + 1) % 4], 0.08F, 0xFF7A20, 0.35F, 0xFF7A20, 0.35F);
        float perim = half * 8;
        for (int bunch = 0; bunch < 2; bunch++) {
            float dirSign = bunch == 0 ? 1 : -1;
            float head = ((time * 9F * dirSign) % perim + perim) % perim;
            for (int k = 0; k < 10; k++) {
                float d0 = head - dirSign * k * 0.25F, d1 = head - dirSign * (k + 1) * 0.25F;
                float alpha = 1.0F - k / 10F;
                ribbon(bb, ringPoint(corners, half, d0), ringPoint(corners, half, d1), 0.16F, bunch == 0 ? 0xFFE0A0 : 0xA0E0FF, alpha, bunch == 0 ? 0xFFE0A0 : 0xA0E0FF, alpha * 0.7F);
            }
        }
        draw(bb);
        // collision flash at the front interaction point
        float f = (float) Math.pow(Math.max(0, Math.sin(time * 4.5)), 12);
        if (f > 0.05F) glow(c.add(0, 0, -half), 1.2F * f + 0.2F, 0xFFFFFF, f);
    }

    private static Vec3 ringPoint(Vec3[] corners, float half, float d) {
        float side = half * 2;
        float perim = side * 4;
        d = ((d % perim) + perim) % perim;
        int i = (int) (d / side);
        float t = (d - i * side) / side;
        Vec3 a = corners[i % 4], b = corners[(i + 1) % 4];
        return a.add(b.subtract(a).scale(t));
    }

    private static void rift(Vec3 c) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(7);
        for (int strand = 0; strand < 5; strand++) {
            Vec3 prev = null;
            float phase = strand * 1.3F;
            int col = strand % 2 == 0 ? 0xC040FF : 0x40FFE0;
            for (int i = 0; i <= 16; i++) {
                float y = -1.6F + i * 0.2F;
                float jag = (float) (Math.sin(time * 3 + i * 1.7 + phase) * 0.12 + (rnd.nextFloat() - 0.5) * 0.08);
                float open = (float) Math.sin(Math.PI * i / 16.0) * (0.25F + 0.1F * (float) Math.sin(time * 2 + phase));
                Vec3 p = c.add(left.x * (jag + (strand - 2) * open * 0.4), y, left.z * (jag + (strand - 2) * open * 0.4));
                if (prev != null) ribbon(bb, prev, p, 0.07F + 0.05F * open, col, 0.9F, col, 0.9F);
                prev = p;
            }
        }
        draw(bb);
        glow(c, 1.6F, 0x8020FF, 0.35F);
    }

    private static void bubbleRim(Vec3 c, float radius, int rgb) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 64;
        for (int i = 0; i < seg; i++) {
            double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
            Vec3 p0 = c.add(left.x * Math.cos(a0) * radius + up.x * Math.sin(a0) * radius, left.y * Math.cos(a0) * radius + up.y * Math.sin(a0) * radius,
                    left.z * Math.cos(a0) * radius + up.z * Math.sin(a0) * radius);
            Vec3 p1 = c.add(left.x * Math.cos(a1) * radius + up.x * Math.sin(a1) * radius, left.y * Math.cos(a1) * radius + up.y * Math.sin(a1) * radius,
                    left.z * Math.cos(a1) * radius + up.z * Math.sin(a1) * radius);
            float shimmer = 0.35F + 0.25F * (float) Math.sin(a0 * 5 + time * 2);
            ribbon(bb, p0, p1, 0.12F, rgb, shimmer, rgb, shimmer);
        }
        draw(bb);
    }

    /** Wireframe computational shell with glowing nodes (Matrioshka Brain layer). */
    private static void shell(Vec3 c, float radius, int rgb, float spin, float tiltAxis, int segs) {
        beginAdditive();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int lat = segs / 2;
        for (int i = 1; i < lat; i++) {
            double th = Math.PI * i / lat;
            for (int j = 0; j < segs; j++) {
                double ph0 = Math.PI * 2 * j / segs + spin, ph1 = Math.PI * 2 * (j + 1) / segs + spin;
                Vec3 a = sph(c, radius, th, ph0, tiltAxis), b = sph(c, radius, th, ph1, tiltAxis);
                ribbon(bb, a, b, 0.025F, rgb, 0.5F, rgb, 0.5F);
                if (i < lat - 1) ribbon(bb, a, sph(c, radius, Math.PI * (i + 1) / lat, ph0, tiltAxis), 0.02F, rgb, 0.3F, rgb, 0.3F);
            }
        }
        draw(bb);
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(segs * 31L);
        for (int k = 0; k < segs * 2; k++) {
            double th = Math.acos(2 * rnd.nextDouble() - 1), ph = rnd.nextDouble() * Math.PI * 2 + spin;
            float blink = (float) Math.max(0, Math.sin(time * (2 + rnd.nextFloat() * 4) + k));
            if (blink > 0.3F) glowInto(pts, sph(c, radius, th, ph, tiltAxis), 0.09F, 0xFFFFFF, blink);
        }
        draw(pts);
    }

    private static Vec3 sph(Vec3 c, double r, double th, double ph, double tilt) {
        double x = r * Math.sin(th) * Math.cos(ph), y = r * Math.cos(th), z = r * Math.sin(th) * Math.sin(ph);
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        double y2 = y * ct - z * st, z2 = y * st + z * ct;
        return c.add(x, y2, z2);
    }

    /** Dyson swarm satellites on inclined orbits, plus shell panels that progressively enclose the star. */
    private static void dyson(Vec3 c, int satellites, int shells) {
        beginAdditive();
        BufferBuilder pts = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Random rnd = new Random(1234);
        int shown = Math.min(satellites, 300);
        for (int i = 0; i < shown; i++) {
            double inc = (rnd.nextDouble() - 0.5) * Math.PI * 0.9, node = rnd.nextDouble() * Math.PI * 2;
            double rad = 2.0 + rnd.nextDouble() * 0.8, speed = 0.6 / Math.pow(rad, 1.5);
            double ang = node + time * speed * 3;
            double x = Math.cos(ang) * rad, z = Math.sin(ang) * rad;
            double y = z * Math.sin(inc);
            z = z * Math.cos(inc);
            glowInto(pts, c.add(x, y, z), 0.07F, 0xFFD070, 0.9F);
        }
        draw(pts);
        if (shells <= 0) return;
        // panels: 8 x 8 grid on a sphere, alpha blended so a finished sphere hides its star
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(true);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = 1.65F;
        int idx = 0;
        boolean complete = shells >= DysonControllerBlockEntity.SHELL_SEGMENTS;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++, idx++) {
                if (idx >= shells) break;
                double t0 = Math.PI * i / 8 + 0.01, t1 = Math.PI * (i + 1) / 8 - 0.01;
                double p0 = Math.PI * 2 * j / 8 + 0.01 + time * 0.05, p1 = Math.PI * 2 * (j + 1) / 8 - 0.01 + time * 0.05;
                int col = complete ? 0x3A2C18 : 0x50402A;
                Vec3 a = sph(c, r, t0, p0, 0), b = sph(c, r, t0, p1, 0), d = sph(c, r, t1, p1, 0), e = sph(c, r, t1, p0, 0);
                v(bb, a.x, a.y, a.z, col, 0.92F);
                v(bb, b.x, b.y, b.z, col, 0.92F);
                v(bb, d.x, d.y, d.z, col, 0.92F);
                v(bb, e.x, e.y, e.z, col, 0.92F);
            }
        draw(bb);
        // glowing seams
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        BufferBuilder seams = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        idx = 0;
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++, idx++) {
                if (idx >= shells) break;
                double t0 = Math.PI * i / 8, p0 = Math.PI * 2 * j / 8 + time * 0.05, p1 = Math.PI * 2 * (j + 1) / 8 + time * 0.05;
                ribbon(seams, sph(c, r * 1.01, t0, p0, 0), sph(c, r * 1.01, t0, p1, 0), 0.03F, 0xFFB040, 0.8F, 0xFFB040, 0.8F);
            }
        draw(seams);
    }

    // ------------------------------------------------------------------ lensing

    private static void drawLenses(Minecraft mc, Matrix4f modelView, Matrix4f proj) {
        RenderTarget main = mc.getMainRenderTarget();
        int w = main.width, h = main.height;
        if (sceneCopy == null) sceneCopy = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        else if (sceneCopy.width != w || sceneCopy.height != h) sceneCopy.resize(w, h, Minecraft.ON_OSX);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);

        ShaderInstance sh = QHShaders.LENS;
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, sceneCopy.getColorTextureId());
        Matrix4f mvp = new Matrix4f(proj).mul(modelView);
        for (Lens l : LENSES) {
            Vector4f ctr = project(mvp, l.pos, w, h);
            if (ctr == null) continue;
            Vec3 edge = l.pos.add(up.x * l.worldRadius, up.y * l.worldRadius, up.z * l.worldRadius);
            Vector4f e = project(mvp, edge, w, h);
            if (e == null) continue;
            float radiusPx = (float) Math.hypot(e.x - ctr.x, e.y - ctr.y);
            sh.safeGetUniform("Center").set(ctr.x, ctr.y);
            sh.safeGetUniform("Radius").set(radiusPx);
            sh.safeGetUniform("Mode").set((float) l.mode);
            sh.safeGetUniform("Strength").set(l.strength);
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            float q = l.quadRadius;
            Vec3 c = l.pos;
            quadVert(bb, c, -q, -q, 0, 0);
            quadVert(bb, c, q, -q, 1, 0);
            quadVert(bb, c, q, q, 1, 1);
            quadVert(bb, c, -q, q, 0, 1);
            draw(bb);
        }
    }

    private static void quadVert(BufferBuilder bb, Vec3 c, float l, float u, float tu, float tv) {
        bb.addVertex((float) (c.x + left.x * l + up.x * u - cam.x), (float) (c.y + left.y * l + up.y * u - cam.y),
                (float) (c.z + left.z * l + up.z * u - cam.z)).setUv(tu, tv);
    }

    /** World position to framebuffer pixel coordinates; null if behind the camera. */
    private static Vector4f project(Matrix4f mvp, Vec3 world, int w, int h) {
        Vector4f clip = new Vector4f((float) (world.x - cam.x), (float) (world.y - cam.y), (float) (world.z - cam.z), 1F).mul(mvp);
        if (clip.w <= 0.05F) return null;
        float nx = clip.x / clip.w, ny = clip.y / clip.w;
        return new Vector4f((nx * 0.5F + 0.5F) * w, (ny * 0.5F + 0.5F) * h, 0, 1);
    }

    private EffectRenderer() {}
}
