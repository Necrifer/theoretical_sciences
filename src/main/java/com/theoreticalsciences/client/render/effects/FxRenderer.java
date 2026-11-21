package com.theoreticalsciences.client.render.effects;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.client.render.PowerAnimator;
import com.theoreticalsciences.client.render.animation.Animations;
import com.theoreticalsciences.client.render.animation.StructureAnimation;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

/**
 * 1.2.0: draws the animated multiblocks each frame, after the block entities (before translucent blocks, as the other
 * large effects are, so glass is laid over them).
 * <p>
 * Two things, on two different rules:
 * <ul>
 *   <li>The <b>structure animation</b>: drawn whenever the machine is formed (its moving parts stand where real blocks
 *       would), and set moving only while a recipe runs. Each part speeds up and slows down over its own
 *       {@code ramp_seconds}; its angle is added up tick by tick from its speed, so it never jumps, and when the machine
 *       stops the part comes to rest where it is and holds there.</li>
 *   <li>The <b>effects</b> (star, shell, orbiters, beams): shown on the spec's trigger (by default while a recipe runs),
 *       swelling in and fading out with the same start-up and shut-down timing as the mod's own machines.</li>
 * </ul>
 * Nothing is sent over the network for any of this: each machine says what the client already knows. Only this mod's
 * own multiblocks (and /tsanim previews) are drawn here.
 */
public final class FxRenderer {
    private static final Map<Object, Instance> INSTANCES = new WeakHashMap<>();
    private static final Map<ControllerBlockEntity, FxSource> OWN = new WeakHashMap<>();
    /** Previews placed with /tsanim (kept until cleared or the world is left). */
    public static final List<Preview> PREVIEWS = new ArrayList<>();
    private static final Map<String, List<BlockState>> ORBITER_BLOCKS = new HashMap<>();

    // the last frames' cost, for /tsanim stats
    static long nanosSum, frames;
    static int lastJobs, lastParts, lastQuads;
    public static double lastMs;

    /** The view's frustum this frame (the moving parts' renderer culls each part against it). */
    @javax.annotation.Nullable
    public static net.minecraft.client.renderer.culling.Frustum frustum;

    public static void onFrustum(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            frustum = event.getFrustum();
            com.theoreticalsciences.client.render.animation.MovingParts.frame++;
        }
    }

    /** Per machine: how far each part has turned, and how fast it runs now. */
    static final class Instance {
        final Object fxKey = new Object(), runKey = new Object();
        ResourceLocation anim;
        float[] power = new float[0];
        float[][] clock = new float[0][];
        float fxClock;
        double last = Double.NaN;
        /** A second animation drawn with the first (a multiblock's own moving blocks and its chamber rings). */
        Instance extra;

        void fit(StructureAnimation a) {
            if (a.id.equals(anim) && power.length == a.ordered.size()) return;
            anim = a.id;
            power = new float[a.ordered.size()];
            clock = new float[a.ordered.size()][];
            for (StructureAnimation.Part p : a.ordered) clock[p.index] = new float[p.motions.size()];
        }
    }

    record Job(FxSource src, Instance inst, Vec3 ctr, Direction front, FxSpec spec, Animations.Baked baked, float fxPower, boolean formed,
               ControllerBlockEntity own, boolean running, double dt) {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        onFrustum(event);
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        frustum = event.getFrustum();
        // with animations turned off, a machine's moving blocks are still drawn (the world has them hidden), only still
        final boolean animate = QHConfig.ANIMATED_MULTIBLOCKS.get();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        com.theoreticalsciences.client.render.animation.MovingParts.sweep();
        boolean any = !PREVIEWS.isEmpty();
        if (!any) for (ControllerBlockEntity c : ClientHooks.CONTROLLERS) if (c.definition() != null && (c.definition().animation != null || !c.definition().moving.isEmpty() || !c.definition().fx.isEmpty())) { any = true; break; }
        if (!any) return;
        long t0 = System.nanoTime();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // (a double: as a float, a world some days old lost the fraction of a tick, and things moved in steps)
        double tick = (double) mc.level.getGameTime() + partial;
        Vec3 cam = event.getCamera().getPosition();
        FxFrame f = new FxFrame(new Matrix4f(event.getModelViewMatrix()), new Matrix4f(event.getProjectionMatrix()), cam, tick, partial);
        f.left = new Vector3f(event.getCamera().getLeftVector());
        f.up = new Vector3f(event.getCamera().getUpVector());

        List<Object[]> sources = new ArrayList<>();
        for (ControllerBlockEntity c : List.copyOf(ClientHooks.CONTROLLERS)) {
            MultiblockDefinition def = c.definition();
            if (def == null || (def.animation == null && def.moving.isEmpty() && def.fx.isEmpty()) || c.isRemoved()) continue;
            sources.add(new Object[]{c, OWN.computeIfAbsent(c, ControllerSource::new)});
        }
        for (Preview p : PREVIEWS) sources.add(new Object[]{p, p});

        double maxDist = QHConfig.ANIMATION_DISTANCE.get(), freeze = QHConfig.ANIMATION_FREEZE_DISTANCE.get();
        List<Job> jobs = new ArrayList<>();
        for (Object[] o : sources) {
            FxSource s = (FxSource) o[1];
            try {
                if (s.level() != mc.level) continue;
                FxSpec spec = s.spec();
                boolean formed = s.formed();
                boolean working = formed && s.working();
                Vec3 ctr = s.controllerCentre();
                Direction front = s.front();
                Animations.Baked live = formed ? s.liveBaked() : null;
                Animations.Baked file = formed && spec.animation() != null ? Animations.baked(spec.animation()) : null;
                Animations.Baked baked = live != null ? live : file, second = live != null ? file : null;
                float extent = spec.extent(Math.max(baked == null ? 0 : baked.extent, second == null ? 0 : second.extent));
                double dist = Math.max(0, ctr.distanceTo(cam) - extent);
                Instance inst = INSTANCES.computeIfAbsent(o[0], k -> new Instance());
                ControllerBlockEntity own = s.ownController();
                boolean mfx = own != null && MachineFx.has(own);
                if (mfx) extent = Math.max(extent, MachineFx.reach(own));       // the effect fills the whole structure
                float fxPower = !mfx ? 0 : PowerAnimator.track(inst.fxKey, working, tick).power(tick);
                boolean running = PowerAnimator.track(inst.runKey, working, tick).on;
                double dt = Double.isNaN(inst.last) ? 0 : Math.max(0, Math.min(1.0, (tick - inst.last) / 20.0));
                inst.last = tick;
                if (dist > freeze || !animate) dt = 0;           // far off (or animations off): drawn, but held still
                if (!animate) fxPower = 0;
                inst.fxClock += (float) (dt * fxPower);
                if (baked != null) advance(inst, baked.anim, running, (float) dt);
                if (second != null) {
                    if (inst.extra == null) inst.extra = new Instance();
                    advance(inst.extra, second.anim, running, (float) dt);
                }
                if (dist > maxDist) continue;
                if (fxPower <= 0.001F && baked == null && !mfx) continue;
                AABB box = new AABB(ctr, ctr).inflate(extent);
                if (!event.getFrustum().isVisible(box)) continue;
                jobs.add(new Job(s, inst, ctr, front, spec, baked, fxPower, formed, mfx ? own : null, running, dt));
                if (second != null) jobs.add(new Job(s, inst.extra, ctr, front, FxSpec.animationOnly(spec.animation()), second, 0, formed, null, running, 0));
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.debug("Animated multiblock skipped: {}", ex.toString());
            }
        }
        if (jobs.isEmpty()) {
            lastJobs = 0;
            return;
        }

        Matrix4fStack mvs = RenderSystem.getModelViewStack();
        mvs.pushMatrix();
        mvs.identity();
        mvs.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        int parts = 0, quads = 0;
        // draw into the main picture: at this stage another target can still be bound (the other world effects do the
        // same); without this, everything drawn here went into a buffer nobody looks at, and the moving parts were
        // missing while the blocks they replace were hidden
        mc.getMainRenderTarget().bindWrite(false);
        try {
            MultiBufferSource.BufferSource bs = buffers();
            PoseStack ps = new PoseStack();
            // the opaque pass: shells, star surfaces, solid and cutout parts, orbiters
            for (Job j : jobs) {
                Matrix4f mv = f.machine(j.ctr, j.front);
                if (j.baked != null && !(j.src instanceof ControllerSource)) {      // the mod's own: drawn by MovingPartsRenderer
                    int[] n = drawParts(f, event, j, mv, false);
                    parts += n[0];
                    quads += n[1];
                }
                if (j.own != null) MachineFx.drawOpaque(f, j.own, j.ctr, j.front, j.fxPower, j.src.seed(), ps, bs, j.inst.fxClock);
            }
            bs.endBatch();
            // the translucent pass: coronas, translucent parts, beams
            for (Job j : jobs) {
                Matrix4f mv = f.machine(j.ctr, j.front);
                if (j.baked != null && !(j.src instanceof ControllerSource)) drawParts(f, event, j, mv, true);
                if (j.own != null) MachineFx.drawLight(f, j.own, j.ctr, j.front, j.fxPower, j.running, j.dt, ps, bs, j.src.seed());
            }
            bs.endBatch();
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.setProjectionMatrix(oldProj, oldSort);
            mvs.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
        lastJobs = jobs.size();
        lastParts = parts;
        lastQuads = quads;
        long dtn = System.nanoTime() - t0;
        nanosSum += dtn;
        frames++;
        lastMs = dtn / 1e6;
    }

    private static MultiBufferSource.BufferSource FX_BUFFERS;

    /** A buffer source of its own, so flushing it never draws the game's own queued (translucent) entities early. */
    static MultiBufferSource.BufferSource buffers() {
        if (FX_BUFFERS == null) FX_BUFFERS = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(1 << 18));
        return FX_BUFFERS;
    }

    /** Average cost per frame since the last call, in milliseconds (and resets the average). */
    public static double averageMs() {
        double a = frames == 0 ? 0 : nanosSum / 1e6 / frames;
        nanosSum = 0;
        frames = 0;
        return a;
    }

    public static int[] lastCounts() {
        return new int[]{lastJobs, lastParts, lastQuads};
    }

    /** How far each part of anim has moved, for the machine key (null until the machine has been seen). */
    public static Matrix4f[] transformsFor(Object key, boolean extra, StructureAnimation anim) {
        Instance inst = INSTANCES.get(key);
        if (inst == null) return null;
        Instance use = anim.id.equals(inst.anim) ? inst : inst.extra != null && anim.id.equals(inst.extra.anim) ? inst.extra : null;
        if (use == null || use.power.length != anim.ordered.size()) return null;
        return transforms(use, anim);
    }

    /** Each part eases toward running (1) or still (0), and every motion's clock moves on by its speed. */
    static void advance(Instance inst, StructureAnimation a, boolean running, float dt) {
        inst.fit(a);
        for (StructureAnimation.Part p : a.ordered) {
            int i = p.index;
            float target = running ? 1 : 0, step = dt / p.ramp;
            float pw = inst.power[i];
            pw = pw < target ? Math.min(target, pw + step) : Math.max(target, pw - step);
            inst.power[i] = pw;
            float ease = pw * pw * (3 - 2 * pw);
            for (int k = 0; k < inst.clock[i].length; k++) inst.clock[i][k] += dt * ease;
        }
    }

    /** Each part's transform in machine space, parents first (a child is carried by its parent's movement). */
    static Matrix4f[] transforms(Instance inst, StructureAnimation a) {
        Matrix4f[] out = new Matrix4f[a.ordered.size()];
        for (StructureAnimation.Part p : a.ordered) {
            Matrix4f m = p.parent != null && out[p.parent.index] != null ? new Matrix4f(out[p.parent.index]) : new Matrix4f();
            m.translate(p.pivot);
            for (int k = 0; k < p.motions.size(); k++) p.motions.get(k).apply(m, inst.clock[p.index][k]);
            m.translate(-p.pivot.x, -p.pivot.y, -p.pivot.z);
            out[p.index] = m;
        }
        return out;
    }

    static final RenderType[] LAYERS = Animations.LAYERS;

    /** The parts' opaque layers (translucent false) or their translucent layer; returns parts and quads drawn. */
    static int[] drawParts(FxFrame f, RenderLevelStageEvent event, Job j, Matrix4f mv, boolean translucent) {
        Minecraft mc = Minecraft.getInstance();
        StructureAnimation a = j.baked.anim;
        j.inst.fit(a);
        Matrix4f[] tr = transforms(j.inst, a);
        Matrix4f local = f.machineLocal(j.ctr, j.front);
        int parts = 0, quads = 0;
        float sky = mc.level.getSkyDarken(f.partial);
        for (StructureAnimation.Part p : a.ordered) {
            Animations.BakedPart bp = j.baked.parts[p.index];
            if (bp == null || bp.quads == 0) continue;
            // culled whole when its bounding sphere is out of view
            Vector4f w = new Matrix4f(local).mul(tr[p.index]).transform(new Vector4f(p.pivot, 1));
            Vec3 wp = new Vec3(w.x + f.cam.x, w.y + f.cam.y, w.z + f.cam.z);
            if (!event.getFrustum().isVisible(new AABB(wp, wp).inflate(bp.radius + 1))) continue;
            float light = 1F;
            if (!p.fullBright) {
                int packed = LevelRenderer.getLightColor(mc.level, BlockPos.containing(wp));
                float bl = LightTexture.block(packed) / 15F, sl = LightTexture.sky(packed) / 15F * sky;
                light = 0.25F + 0.75F * Math.max(bl, sl);
            }
            Matrix4f pm = new Matrix4f(mv).mul(tr[p.index]);
            for (int L = translucent ? 3 : 0; L < (translucent ? 4 : 3); L++) {
                VertexBuffer vb = bp.layers[L];
                if (vb == null) continue;
                RenderType rt = LAYERS[L];
                rt.setupRenderState();
                ShaderInstance sh = RenderSystem.getShader();
                if (sh != null) {
                    if (sh.CHUNK_OFFSET != null) sh.CHUNK_OFFSET.set(0F, 0F, 0F);
                    RenderSystem.setShaderColor(light, light, light, 1F);
                    vb.bind();
                    vb.drawWithShader(pm, f.proj, sh);
                    VertexBuffer.unbind();
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                }
                rt.clearRenderState();
            }
            parts++;
            quads += bp.quads;
        }
        return new int[]{parts, quads};
    }

    static final List<String> DEFAULT_ORBITERS = List.of("theoreticalsciences:neutronium_block", "theoreticalsciences:dilithium_block",
            "theoreticalsciences:cherenkov_quartz_block", "theoreticalsciences:chronosteel_block", "theoreticalsciences:spacetime_alloy_block",
            "theoreticalsciences:cosmic_neutronium_block", "theoreticalsciences:magnetite_block", "theoreticalsciences:computronium_block");

    static List<BlockState> orbiterBlocks(List<ResourceLocation> ids) {
        String k = ids.toString();
        return ORBITER_BLOCKS.computeIfAbsent(k, x -> {
            List<BlockState> out = new ArrayList<>();
            List<ResourceLocation> src = new ArrayList<>(ids);
            if (src.isEmpty()) for (String s : DEFAULT_ORBITERS) src.add(ResourceLocation.parse(s));
            for (ResourceLocation r : src) {
                Block b = BuiltInRegistries.BLOCK.get(r);
                if (b != Blocks.AIR) out.add(b.defaultBlockState());
            }
            if (out.isEmpty()) out.add(Blocks.OBSIDIAN.defaultBlockState());
            return out;
        });
    }

    /** Resources reloaded (F3+T): every baked buffer is freed and baked again when next drawn. */
    public static void onReload() {
        FxMesh.release();
        com.theoreticalsciences.compat.MultiblockView.releaseBaked();
        com.theoreticalsciences.client.render.animation.MovingParts.clear();
        ORBITER_BLOCKS.clear();
    }

    public static void clear() {
        com.theoreticalsciences.client.render.animation.MovingParts.clear();
        PREVIEWS.clear();
        INSTANCES.clear();
        OWN.clear();
    }

    // ------------------------------------------------------------------------------------------------ sources
    /** One of the mod's own controllers whose multiblock names an animation. */
    static final class ControllerSource implements FxSource {
        final java.lang.ref.WeakReference<ControllerBlockEntity> ref;
        FxSpec spec;

        ControllerSource(ControllerBlockEntity c) {
            this.ref = new java.lang.ref.WeakReference<>(c);
        }

        @Override
        public boolean alive() {
            ControllerBlockEntity c = ref.get();
            return c != null && !c.isRemoved();
        }

        @Override
        public Level level() {
            ControllerBlockEntity c = ref.get();
            return c == null ? null : c.getLevel();
        }

        @Override
        public Vec3 controllerCentre() {
            return Vec3.atCenterOf(ref.get().getBlockPos());
        }

        @Override
        public Direction front() {
            return ref.get().facing();
        }

        @Override
        public boolean formed() {
            ControllerBlockEntity c = ref.get();
            return c != null && c.isFormed();
        }

        @Override
        public boolean working() {
            ControllerBlockEntity c = ref.get();
            return c != null && c.isActive();
        }

        @Override
        public ControllerBlockEntity ownController() {
            return ref.get();
        }

        @Override
        public Animations.Baked liveBaked() {
            ControllerBlockEntity c = ref.get();
            return c == null ? null : com.theoreticalsciences.client.render.animation.MovingParts.get(c);
        }

        @Override
        public FxSpec spec() {
            ResourceLocation a = ref.get().definition().animation;
            if (spec == null || !Objects.equals(spec.animation(), a)) spec = FxSpec.animationOnly(a);
            return spec;
        }
    }

    /** A preview placed with /tsanim: a machine that is not there, formed, running when asked. */
    public static final class Preview implements FxSource {
        final Level level;
        final Vec3 centre;
        final Direction front;
        final FxSpec spec;
        public boolean running = true;

        public Preview(Level level, BlockPos pos, Direction front, FxSpec spec) {
            this.level = level;
            this.centre = Vec3.atCenterOf(pos);
            this.front = front;
            this.spec = spec;
        }

        @Override public boolean alive() { return true; }
        @Override public Level level() { return level; }
        @Override public Vec3 controllerCentre() { return centre; }
        @Override public Direction front() { return front; }
        @Override public boolean formed() { return true; }
        @Override public boolean working() { return running; }
        @Override public FxSpec spec() { return spec; }
    }

    private FxRenderer() {}
}
