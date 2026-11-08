package com.theoreticalsciences.client.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.theoreticalsciences.TheoreticalSciences;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.3.0: draws a large structure in the JEI / EMI viewer the way the game draws terrain. The blocks are split into
 * 16x16x16 sections; each section is meshed on a background thread (nearest the middle first), handed to the render
 * thread, and uploaded to the graphics card a few sections a frame (a time and size budget), so opening a page with
 * tens of thousands of blocks never stalls a frame. Each section keeps its own buffers and is skipped when it lies
 * outside the viewport. Only real blocks are stored (no air), and faces against a full neighbour are left out.
 * <p>
 * Everything here runs on the render thread except {@link #mesh}, which only reads the (never changed) cell map.
 */
public final class PreviewRenderer implements AutoCloseable {
    /** The block layers, drawn in this order (translucent last). */
    static final RenderType[] LAYERS = {RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent()};
    private static final int TRANSLUCENT = 3;
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final Direction[] DIRS = Direction.values();
    /** Most time spent uploading finished sections in one frame. */
    private static final long UPLOAD_NANOS = 3_000_000L;
    /** Most bytes uploaded in one frame (one section is always allowed, however large). */
    private static final long UPLOAD_BYTES = 8L << 20;

    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2)), r -> {
                Thread t = new Thread(r, "TS structure preview #" + THREAD_ID.incrementAndGet());
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
    private static volatile boolean errorLogged;

    private static final class Section {
        final int x, y, z;                       // block coordinates of the section's corner
        final VertexBuffer[] buffers = new VertexBuffer[LAYERS.length];
        float depth;                              // towards the viewer, for drawing translucent faces back to front

        Section(long key) {
            this.x = SectionPos.x(key) << 4;
            this.y = SectionPos.y(key) << 4;
            this.z = SectionPos.z(key) << 4;
        }

        void close() {
            for (int i = 0; i < buffers.length; i++)
                if (buffers[i] != null) {
                    buffers[i].close();
                    buffers[i] = null;
                }
        }
    }

    /** A meshed section waiting for upload; its memory is freed once uploaded (or thrown away). */
    private record Mesh(long key, MeshData[] data, ByteBufferBuilder[] memory) {
        long bytes() {
            long n = 0;
            for (MeshData d : data) if (d != null) n += d.vertexBuffer().remaining();
            return n;
        }

        void free() {
            for (MeshData d : data) if (d != null) d.close();
            for (ByteBufferBuilder m : memory) if (m != null) m.close();
        }
    }

    private final Long2ObjectOpenHashMap<BlockState> cells;
    private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private final ConcurrentLinkedQueue<Mesh> done = new ConcurrentLinkedQueue<>();
    private final List<Section> drawn = new ArrayList<>();
    private final int total;
    private int received;
    private volatile boolean closed;

    /**
     * Starts meshing straight away. {@code cells} maps {@link BlockPos#asLong} pattern positions to the block drawn
     * there; it must not be changed afterwards (the workers read it).
     */
    public PreviewRenderer(Long2ObjectOpenHashMap<BlockState> cells, float cx, float cy, float cz) {
        this.cells = cells;
        Long2ObjectOpenHashMap<LongArrayList> bySection = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<BlockState> e : cells.long2ObjectEntrySet()) {
            long p = e.getLongKey();
            long key = SectionPos.asLong(BlockPos.getX(p) >> 4, BlockPos.getY(p) >> 4, BlockPos.getZ(p) >> 4);
            bySection.computeIfAbsent(key, k -> new LongArrayList()).add(p);
        }
        this.total = bySection.size();
        // the middle first: the part of the structure most likely in view fills in before its far edges
        LongArrayList order = new LongArrayList(bySection.keySet());
        order.sort((a, b) -> Float.compare(distance(a, cx, cy, cz), distance(b, cx, cy, cz)));
        for (int i = 0; i < order.size(); i++) {
            long key = order.getLong(i);
            LongArrayList list = bySection.get(key);
            POOL.execute(() -> mesh(key, list));
        }
    }

    private static float distance(long key, float cx, float cy, float cz) {
        float dx = (SectionPos.x(key) << 4) + 8 - cx, dy = (SectionPos.y(key) << 4) + 8 - cy, dz = (SectionPos.z(key) << 4) + 8 - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Sections meshed and uploaded, from 0 to 1. */
    public float progress() {
        return total == 0 ? 1F : received / (float) total;
    }

    public boolean isReady() {
        return received >= total;
    }

    // ------------------------------------------------------------------------------------------------ meshing
    /** Worker thread: meshes one section into one buffer per block layer. */
    private void mesh(long key, LongArrayList positions) {
        if (closed) return;
        ByteBufferBuilder[] memory = new ByteBufferBuilder[LAYERS.length];
        BufferBuilder[] builders = new BufferBuilder[LAYERS.length];
        MeshData[] data = new MeshData[LAYERS.length];
        try {
            var disp = Minecraft.getInstance().getBlockRenderer();
            RandomSource rand = RandomSource.create(42L);
            PoseStack ps = new PoseStack();
            float[] shade = new float[4];
            int[] light = {FULL, FULL, FULL, FULL};
            for (int i = 0; i < positions.size(); i++) {
                if (closed) break;
                long p = positions.getLong(i);
                BlockState st = cells.get(p);
                int x = BlockPos.getX(p), y = BlockPos.getY(p), z = BlockPos.getZ(p);
                BakedModel model = disp.getBlockModel(st);
                ps.pushPose();
                ps.translate(x, y, z);
                rand.setSeed(42L);
                for (RenderType rt : model.getRenderTypes(st, rand, ModelData.EMPTY)) {
                    int L = layerOf(rt);
                    for (int di = 0; di <= DIRS.length; di++) {
                        Direction d = di < DIRS.length ? DIRS[di] : null;
                        if (d != null && hidden(st, d, x, y, z)) continue;
                        rand.setSeed(42L);
                        for (BakedQuad q : model.getQuads(st, d, rand, ModelData.EMPTY, rt)) {
                            if (builders[L] == null) {
                                memory[L] = new ByteBufferBuilder(16384);
                                builders[L] = new BufferBuilder(memory[L], VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                            }
                            float r = 1, g = 1, b = 1;
                            if (q.isTinted()) {
                                int col;
                                try {
                                    col = Minecraft.getInstance().getBlockColors().getColor(st, null, null, q.getTintIndex());
                                } catch (RuntimeException ex) {
                                    col = -1;
                                }
                                if (col != -1) {
                                    r = (col >> 16 & 255) / 255F;
                                    g = (col >> 8 & 255) / 255F;
                                    b = (col & 255) / 255F;
                                }
                            }
                            float sh = q.isShade() ? shade(q.getDirection()) : 1F;
                            shade[0] = shade[1] = shade[2] = shade[3] = sh;
                            builders[L].putBulkData(ps.last(), q, shade, r, g, b, 1F, light, OverlayTexture.NO_OVERLAY, false);
                        }
                    }
                }
                ps.popPose();
            }
            if (!closed) for (int L = 0; L < LAYERS.length; L++) if (builders[L] != null) data[L] = builders[L].build();
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                TheoreticalSciences.LOGGER.error("Structure preview: a section could not be meshed", t);
            }
        }
        Mesh m = new Mesh(key, data, memory);
        if (closed) {
            m.free();
            return;
        }
        done.add(m);
        // closed while this was being queued: nothing will upload it, so free whatever is still waiting
        if (closed) drain();
    }

    /** A face is left out when the neighbour on that side covers it completely. */
    private boolean hidden(BlockState st, Direction d, int x, int y, int z) {
        BlockState nb = cells.get(BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ()));
        if (nb == null) return false;
        return st.skipRendering(nb, d) || nb.canOcclude() && Block.isShapeFullBlock(nb.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    private static float shade(Direction d) {
        if (d == null) return 1F;
        return switch (d) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
    }

    private static int layerOf(RenderType rt) {
        if (rt == RenderType.translucent()) return 3;
        if (rt == RenderType.cutout()) return 2;
        if (rt == RenderType.cutoutMipped()) return 1;
        return 0;
    }

    // ------------------------------------------------------------------------------------------------ uploading
    /** Render thread: uploads finished sections, within this frame's budget. */
    public void upload() {
        if (closed) return;
        long start = System.nanoTime();
        long bytes = 0;
        Mesh m;
        while ((bytes == 0 || System.nanoTime() - start < UPLOAD_NANOS && bytes < UPLOAD_BYTES) && (m = done.poll()) != null) {
            bytes += Math.max(1, m.bytes());
            received++;
            Section s = null;
            for (int L = 0; L < LAYERS.length; L++) {
                MeshData d = m.data()[L];
                if (d == null) continue;
                if (s == null) s = sections.computeIfAbsent(m.key(), Section::new);
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vb.bind();
                vb.upload(d);                    // (frees the mesh's own copy)
                VertexBuffer.unbind();
                m.data()[L] = null;
                s.buffers[L] = vb;
            }
            m.free();
        }
    }

    // ------------------------------------------------------------------------------------------------ drawing
    /**
     * Render thread: draws every uploaded section that can be seen. {@code pose} maps pattern space to the GUI;
     * {@code toView} maps pattern space to the coordinates the viewport rectangle is given in.
     */
    public void render(Matrix4f pose, Matrix4f toView, float vx0, float vy0, float vx1, float vy1) {
        if (closed || sections.isEmpty()) return;
        drawn.clear();
        Vector4f c = new Vector4f();
        for (Section s : sections.values()) {
            // the section's 8 corners on screen: drawn only if their box overlaps the viewport
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, depth = 0;
            for (int i = 0; i < 8; i++) {
                toView.transform(c.set(s.x + ((i & 1) << 4), s.y + ((i & 2) << 3), s.z + ((i & 4) << 2), 1));
                minX = Math.min(minX, c.x);
                maxX = Math.max(maxX, c.x);
                minY = Math.min(minY, c.y);
                maxY = Math.max(maxY, c.y);
                depth += c.z;
            }
            if (maxX < vx0 || minX > vx1 || maxY < vy0 || minY > vy1) continue;
            s.depth = depth;
            drawn.add(s);
        }
        if (drawn.isEmpty()) return;
        // back to front, so translucent faces blend over what is behind them
        drawn.sort((a, b) -> Float.compare(a.depth, b.depth));
        Matrix4f mv = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
        Matrix4f proj = RenderSystem.getProjectionMatrix();
        float fogStart = RenderSystem.getShaderFogStart();
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);       // the world's fog has no meaning in a GUI
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        for (int L = 0; L < LAYERS.length; L++) {
            RenderType rt = LAYERS[L];
            rt.setupRenderState();
            ShaderInstance sh = RenderSystem.getShader();
            if (sh != null) {
                if (sh.CHUNK_OFFSET != null) sh.CHUNK_OFFSET.set(0F, 0F, 0F);
                // solid layers front to back (fewer hidden pixels shaded), translucent back to front
                int n = drawn.size();
                for (int k = 0; k < n; k++) {
                    Section s = drawn.get(L == TRANSLUCENT ? k : n - 1 - k);
                    VertexBuffer vb = s.buffers[L];
                    if (vb == null) continue;
                    vb.bind();
                    vb.drawWithShader(mv, proj, sh);
                }
                VertexBuffer.unbind();
            }
            rt.clearRenderState();
        }
        RenderSystem.setShaderFogStart(fogStart);
    }

    /** Stops the workers and frees everything this holds (render thread). */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        drain();
        for (Section s : sections.values()) s.close();
        sections.clear();
        drawn.clear();
    }

    private void drain() {
        Mesh m;
        while ((m = done.poll()) != null) m.free();
    }
}
