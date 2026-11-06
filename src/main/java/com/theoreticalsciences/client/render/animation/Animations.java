package com.theoreticalsciences.client.render.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.*;

/**
 * 1.2.0: every structure animation in the resource packs, and their parts baked into GPU buffers.
 * <p>
 * Each part is baked once, the first time it is drawn, into one vertex buffer per block layer (solid, cutout mipped,
 * cutout, translucent), on the block atlas, from the blocks' own models: so a moving ring reads as the machine's own
 * casing moving. Faces between two blocks of the same part are left out (a ring of thousands of blocks would otherwise
 * be mostly hidden faces). The buffers are shared by every machine that uses the animation, and freed and rebaked
 * when resources reload (F3+T).
 */
public final class Animations {
    public static final RenderType[] LAYERS = {RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent()};
    static final int FULL = LightTexture.FULL_BRIGHT;

    private static final Map<ResourceLocation, StructureAnimation> DEFS = new HashMap<>();
    private static final Map<ResourceLocation, Baked> BAKED = new HashMap<>();

    /** One block of a CELLS part: where it is (machine space) and what it is. */
    public record Cell(int x, int y, int z, BlockState state) {}

    /**
     * Vertices kept in memory: x y z, colour (ARGB as a float's bits), u v, normal x y z; nine floats each. A part is
     * drawn from these through the game's own block buffers (as moving pistons are), so it needs nothing special of
     * the graphics card.
     */
    public static final class Rec implements VertexConsumer {
        public float[] v = new float[9 * 64];
        public int n;            // vertices written

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if ((n + 1) * 9 > v.length) v = java.util.Arrays.copyOf(v, v.length * 2);
            int i = n * 9;
            v[i] = x;
            v[i + 1] = y;
            v[i + 2] = z;
            v[i + 3] = Float.intBitsToFloat(-1);
            v[i + 7] = 0;
            v[i + 8] = 1;
            n++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            v[(n - 1) * 9 + 3] = Float.intBitsToFloat((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float vv) {
            v[(n - 1) * 9 + 4] = u;
            v[(n - 1) * 9 + 5] = vv;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int vv) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int vv) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            v[(n - 1) * 9 + 6] = x;
            v[(n - 1) * 9 + 7] = y;
            v[(n - 1) * 9 + 8] = z;
            return this;
        }

        /** Writes every vertex through pose into out, lit by light. */
        public void emit(VertexConsumer out, PoseStack.Pose pose, int light, int overlay) {
            org.joml.Matrix4f m = pose.pose();
            for (int k = 0; k < n; k++) {
                int i = k * 9;
                int c = Float.floatToRawIntBits(v[i + 3]);
                out.addVertex(m, v[i], v[i + 1], v[i + 2]).setColor(c >> 16 & 255, c >> 8 & 255, c & 255, c >>> 24 & 255).setUv(v[i + 4], v[i + 5])
                        .setOverlay(overlay).setLight(light).setNormal(pose, v[i + 6], v[i + 7], v[i + 8]);
            }
        }
    }

    /**
     * True when parts are drawn from memory through the game's block buffers rather than from cached buffers on the
     * graphics card: asked for in the client config, or with Sable (which carries block entities about) installed.
     */
    public static boolean cpuOnly() {
        if (cpuOnly == null) {
            boolean c;
            try {
                c = com.theoreticalsciences.QHConfig.MOVING_PARTS_COMPAT.get();
            } catch (RuntimeException e) {
                c = false;
            }
            cpuOnly = c || net.neoforged.fml.ModList.get().isLoaded("sable");
        }
        return cpuOnly;
    }

    private static Boolean cpuOnly;
    private static final Set<ResourceLocation> MISSING = new HashSet<>();

    public static final class BakedPart {
        public final VertexBuffer[] layers = new VertexBuffer[LAYERS.length];
        /** The same, in memory: kept only when parts are drawn from memory ({@link #cpuOnly}). */
        public final Rec[] cpu = new Rec[LAYERS.length];
        public int quads;
        /** Furthest any vertex lies from the part's pivot (for culling while it turns). */
        public float radius;
    }

    public static final class Baked {
        public final StructureAnimation anim;
        public final BakedPart[] parts;
        /** Furthest any part can reach from the controller's centre, in blocks. */
        public final float extent;
        public final int quads;

        Baked(StructureAnimation anim, BakedPart[] parts) {
            this.anim = anim;
            this.parts = parts;
            float e = 0;
            int q = 0;
            for (StructureAnimation.Part p : anim.ordered) {
                BakedPart b = parts[p.index];
                // a child can be carried round by its parents: add up the chain
                float reach = b.radius;
                StructureAnimation.Part c = p;
                while (c != null) {
                    StructureAnimation.Part par = c.parent;
                    reach += par == null ? c.pivot.length() : new Vector3f(c.pivot).sub(par.pivot).length();
                    c = par;
                }
                e = Math.max(e, reach);
                q += b.quads;
            }
            this.extent = e;
            this.quads = q;
        }
    }

    // ------------------------------------------------------------------------------------------------ loading
    /** Re-reads every ts_animations file (on the game thread, from the client reload listener). */
    public static void reload(ResourceManager rm) {
        release();
        cpuOnly = null;
        MISSING.clear();
        DEFS.clear();
        Map<ResourceLocation, Resource> found = rm.listResources("ts_animations", rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
            ResourceLocation f = e.getKey();
            String path = f.getPath().substring("ts_animations/".length(), f.getPath().length() - 5);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(f.getNamespace(), path);
            try (Reader r = e.getValue().openAsReader()) {
                DEFS.put(id, StructureAnimation.parse(id, JsonParser.parseReader(r).getAsJsonObject()));
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.error("Bad structure animation {}: {}", id, ex.toString());
            }
        }
        TheoreticalSciences.LOGGER.info("Loaded {} structure animations", DEFS.size());
    }

    /** Frees every baked buffer (they are baked again when next drawn). Safe to call off the render thread. */
    public static void release() {
        List<Baked> old = new ArrayList<>(BAKED.values());
        BAKED.clear();
        if (old.isEmpty()) return;
        RenderSystem.recordRenderCall(() -> {
            for (Baked b : old)
                for (BakedPart p : b.parts)
                    for (VertexBuffer vb : p.layers) if (vb != null) vb.close();
        });
    }

    public static Set<ResourceLocation> ids() {
        return Collections.unmodifiableSet(DEFS.keySet());
    }

    @Nullable
    public static StructureAnimation get(ResourceLocation id) {
        return DEFS.get(id);
    }

    /** The baked meshes for an animation (baked now if they are not yet), or null when there is no such animation. */
    @Nullable
    public static Baked baked(ResourceLocation id) {
        Baked b = BAKED.get(id);
        if (b != null) return b;
        StructureAnimation a = DEFS.get(id);
        if (a == null) {
            // 1.2.0: a name that matches no animation file: said once, not left to draw nothing silently
            if (MISSING.add(id)) TheoreticalSciences.LOGGER.warn("No structure animation called {} (looked for assets/{}/ts_animations/{}.json)", id, id.getNamespace(), id.getPath());
            return null;
        }
        BakedPart[] parts = new BakedPart[a.ordered.size()];
        for (StructureAnimation.Part p : a.ordered) {
            try {
                parts[p.index] = bake(p);
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Could not bake part {} of animation {}: {}", p.id, id, ex.toString());
                parts[p.index] = new BakedPart();
            }
        }
        b = new Baked(a, parts);
        BAKED.put(id, b);
        return b;
    }

    /** Bakes an animation that is not shared (a multiblock's own parts, read from the world); free it with {@link #close}. */
    public static Baked bakeLive(StructureAnimation a) {
        BakedPart[] parts = new BakedPart[a.ordered.size()];
        for (StructureAnimation.Part p : a.ordered) {
            try {
                parts[p.index] = bake(p, !cpuOnly());
            } catch (RuntimeException ex) {
                TheoreticalSciences.LOGGER.error("Could not bake moving part {}: {}", p.id, ex.toString());
                parts[p.index] = new BakedPart();
            }
        }
        return new Baked(a, parts);
    }

    public static void close(@Nullable Baked b) {
        if (b == null) return;
        RenderSystem.recordRenderCall(() -> {
            for (BakedPart p : b.parts) if (p != null) for (VertexBuffer vb : p.layers) if (vb != null) vb.close();
        });
    }

    public static int bakedCount() {
        return BAKED.size();
    }

    // ------------------------------------------------------------------------------------------------ baking
    private static final class Builder {
        final Rec[] rec = new Rec[LAYERS.length];
        final Vector3f pivot;
        final boolean gpu;
        float radius;
        int quads;

        Builder(Vector3f pivot, boolean gpu) {
            this.pivot = pivot;
            this.gpu = gpu;
        }

        VertexConsumer get(int layer) {
            if (rec[layer] == null) rec[layer] = new Rec();
            return rec[layer];
        }

        void reach(float x, float y, float z) {
            float dx = x - pivot.x, dy = y - pivot.y, dz = z - pivot.z;
            radius = Math.max(radius, Mth.sqrt(dx * dx + dy * dy + dz * dz));
        }

        BakedPart finish() {
            BakedPart out = new BakedPart();
            for (int i = 0; i < LAYERS.length; i++) {
                Rec r = rec[i];
                if (r == null || r.n == 0) continue;
                if (!gpu || cpuOnly()) out.cpu[i] = r;           // (no copy in memory when it is on the graphics card)
                if (!gpu) continue;
                try (ByteBufferBuilder mem = new ByteBufferBuilder(r.n * DefaultVertexFormat.BLOCK.getVertexSize() + 256)) {
                    BufferBuilder bb = new BufferBuilder(mem, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                    for (int k = 0; k < r.n; k++) {
                        int j = k * 9;
                        bb.addVertex(r.v[j], r.v[j + 1], r.v[j + 2], Float.floatToRawIntBits(r.v[j + 3]), r.v[j + 4], r.v[j + 5], OverlayTexture.NO_OVERLAY, FULL,
                                r.v[j + 6], r.v[j + 7], r.v[j + 8]);
                    }
                    MeshData md = bb.build();
                    if (md != null) {
                        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        vb.bind();
                        vb.upload(md);
                        VertexBuffer.unbind();
                        out.layers[i] = vb;
                    }
                }
            }
            out.quads = quads;
            out.radius = radius;
            return out;
        }
    }

    static BakedPart bake(StructureAnimation.Part p) {
        return bake(p, true);
    }

    static BakedPart bake(StructureAnimation.Part p, boolean gpu) {
        Builder b = new Builder(p.pivot, gpu);
        JsonObject s = p.source;
        switch (p.type) {
            case BLOCKS -> blocks(b, s);
            case MODEL -> {
                BlockState st = state(s.get("block"));
                Vector3f at = StructureAnimation.vec(s.get("position"), new Vector3f(p.pivot));
                float sc = s.has("scale") ? s.get("scale").getAsFloat() : 1F;
                if (st != null) emitBlock(b, st, at, sc, null, 0, 0, 0);
            }
            case MESH -> mesh(b, s, p.pivot);
            case CELLS -> {
                Map<Long, BlockState> map = new HashMap<>();
                for (Cell c : p.cells) map.put(BlockPos.asLong(c.x(), c.y(), c.z()), c.state());
                for (Cell c : p.cells) emitBlock(b, c.state(), new Vector3f(c.x(), c.y(), c.z()), 1F, map, c.x(), c.y(), c.z());
            }
        }
        return b.finish();
    }

    /** The layers of blocks: layers bottom to top, rows front to back, characters left to right, from "origin". */
    static void blocks(Builder b, JsonObject s) {
        Map<Character, BlockState> key = new HashMap<>();
        if (s.has("key")) for (Map.Entry<String, JsonElement> e : s.getAsJsonObject("key").entrySet()) {
            if (e.getKey().isEmpty()) continue;
            BlockState st = state(e.getValue());
            if (st != null && !st.isAir()) key.put(e.getKey().charAt(0), st);
        }
        Vector3f o = StructureAnimation.vec(s.get("origin"), new Vector3f());
        int ox = Math.round(o.x), oy = Math.round(o.y), oz = Math.round(o.z);
        Map<Long, BlockState> cells = new HashMap<>();
        JsonArray layers = s.has("layers") ? s.getAsJsonArray("layers") : new JsonArray();
        for (int y = 0; y < layers.size(); y++) {
            JsonArray rows = layers.get(y).getAsJsonArray();
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z).getAsString();
                for (int x = 0; x < row.length(); x++) {
                    BlockState st = key.get(row.charAt(x));
                    if (st != null) cells.put(BlockPos.asLong(ox + x, oy + y, oz + z), st);
                }
            }
        }
        for (Map.Entry<Long, BlockState> e : cells.entrySet()) {
            BlockPos pos = BlockPos.of(e.getKey());
            emitBlock(b, e.getValue(), new Vector3f(pos.getX(), pos.getY(), pos.getZ()), 1F, cells, pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /**
     * A key entry: "ns:block", "ns:block[prop=value]", or a list of those (a slot that takes several tiers: the first,
     * the lowest tier, is the one drawn).
     */
    @Nullable
    static BlockState state(@Nullable JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonArray()) return e.getAsJsonArray().isEmpty() ? null : state(e.getAsJsonArray().get(0));
        String str = e.isJsonObject() ? e.getAsJsonObject().get("block").getAsString() : e.getAsString();
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), str, false).blockState();
        } catch (Exception ex) {
            ResourceLocation rl = ResourceLocation.tryParse(str);
            Block bl = rl == null ? Blocks.AIR : BuiltInRegistries.BLOCK.get(rl);
            if (bl == Blocks.AIR) TheoreticalSciences.LOGGER.warn("Structure animation: unknown block {}", str);
            return bl == Blocks.AIR ? null : bl.defaultBlockState();
        }
    }

    private static final Direction[] DIRS = Direction.values();

    static float shade(@Nullable Direction d) {
        if (d == null) return 1F;
        return switch (d) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
    }

    static int layerOf(RenderType rt) {
        if (rt == RenderType.translucent()) return 3;
        if (rt == RenderType.cutout()) return 2;
        if (rt == RenderType.cutoutMipped()) return 1;
        return 0;
    }

    /** One block at cell (centre) at, of size sc; faces against a full neighbour in cells are left out. */
    static void emitBlock(Builder b, BlockState st, Vector3f at, float sc, @Nullable Map<Long, BlockState> cells, int x, int y, int z) {
        var disp = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = disp.getBlockModel(st);
        RandomSource rand = RandomSource.create(42L);
        PoseStack ps = new PoseStack();
        ps.translate(at.x, at.y, at.z);
        ps.scale(sc, sc, sc);
        ps.translate(-0.5F, -0.5F, -0.5F);
        float h = 0.866F * sc;
        b.reach(at.x + h, at.y + h, at.z + h);
        b.reach(at.x - h, at.y - h, at.z - h);
        int tintDefault = -1;
        for (RenderType rt : model.getRenderTypes(st, rand, ModelData.EMPTY)) {
            int L = layerOf(rt);
            for (int di = 0; di <= DIRS.length; di++) {
                Direction d = di < DIRS.length ? DIRS[di] : null;
                if (d != null && cells != null) {
                    BlockState nb = cells.get(BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ()));
                    if (nb != null && (st.skipRendering(nb, d) || nb.canOcclude()
                            && Block.isShapeFullBlock(nb.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)))) continue;
                }
                rand.setSeed(42L);
                for (BakedQuad q : model.getQuads(st, d, rand, ModelData.EMPTY, rt)) {
                    int col = tintDefault;
                    if (q.isTinted()) {
                        try {
                            col = Minecraft.getInstance().getBlockColors().getColor(st, null, null, q.getTintIndex());
                        } catch (RuntimeException ex) {
                            col = -1;
                        }
                    }
                    float r = 1, g = 1, bl = 1;
                    if (col != -1) {
                        r = (col >> 16 & 255) / 255F;
                        g = (col >> 8 & 255) / 255F;
                        bl = (col & 255) / 255F;
                    }
                    float sh = q.isShade() ? shade(q.getDirection()) : 1F;
                    b.get(L).putBulkData(ps.last(), q, new float[]{sh, sh, sh, sh}, r, g, bl, 1F, new int[]{FULL, FULL, FULL, FULL}, OverlayTexture.NO_OVERLAY, false);
                    b.quads++;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ meshes
    /** Procedural shapes on a block texture: "hoop" (a flat ring round an axis) and "box". */
    static void mesh(Builder b, JsonObject s, Vector3f pivot) {
        String shape = s.has("shape") ? s.get("shape").getAsString().toLowerCase(Locale.ROOT) : "hoop";
        ResourceLocation tex = ResourceLocation.tryParse(s.has("texture") ? s.get("texture").getAsString() : "theoreticalsciences:block/neutronium_block");
        TextureAtlasSprite sp = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(tex);
        int col = com.theoreticalsciences.client.render.effects.FxSpec.parseColor(s.has("color") ? s.get("color").getAsString() : null, 0xFFFFFF);
        boolean translucent = s.has("translucent") && s.get("translucent").getAsBoolean();
        VertexConsumer bb = b.get(translucent ? 3 : 0);
        if (shape.equals("box")) {
            Vector3f a = StructureAnimation.vec(s.get("from"), new Vector3f(-0.5F)), c = StructureAnimation.vec(s.get("to"), new Vector3f(0.5F));
            box(b, bb, sp, col, a, c);
            return;
        }
        Vector3f centre = StructureAnimation.vec(s.get("centre"), StructureAnimation.vec(s.get("center"), new Vector3f(pivot)));
        Vector3f w = StructureAnimation.axis(s.get("axis"));
        float ri = s.has("inner_radius") ? s.get("inner_radius").getAsFloat() : 4, ro = s.has("outer_radius") ? s.get("outer_radius").getAsFloat() : ri + 1;
        float hh = (s.has("height") ? s.get("height").getAsFloat() : 1) / 2;
        int seg = s.has("segments") ? Mth.clamp(s.get("segments").getAsInt(), 8, 512) : Mth.clamp((int) (ro / 2), 24, 128);
        Vector3f u = Math.abs(w.y) < 0.9F ? new Vector3f(0, 1, 0).cross(w).normalize() : new Vector3f(1, 0, 0).cross(w).normalize();
        Vector3f v = new Vector3f(w).cross(u).normalize();
        float tile = s.has("tile") ? s.get("tile").getAsFloat() : 1;      // blocks per texture repeat
        // one quad per block of surface (as near as the sizes allow), so the texture keeps its scale
        int nh = Math.max(1, Math.round(2 * hh / tile)), nr = Math.max(1, Math.round((ro - ri) / tile));
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            int n = Math.max(1, Math.round(ro * (a1 - a0) / tile));
            for (int k = 0; k < n; k++) {
                float b0 = Mth.lerp(k / (float) n, a0, a1), b1 = Mth.lerp((k + 1) / (float) n, a0, a1);
                Vector3f e0 = dir(u, v, b0), e1 = dir(u, v, b1);
                for (int j = 0; j < nh; j++) {
                    float h0 = -hh + 2 * hh * j / nh, h1 = -hh + 2 * hh * (j + 1) / nh;
                    quad(b, bb, sp, col, 0.7F, pt(centre, e0, ro, w, h0), pt(centre, e1, ro, w, h0), pt(centre, e1, ro, w, h1), pt(centre, e0, ro, w, h1));
                    quad(b, bb, sp, col, 0.6F, pt(centre, e1, ri, w, h0), pt(centre, e0, ri, w, h0), pt(centre, e0, ri, w, h1), pt(centre, e1, ri, w, h1));
                }
                for (int j = 0; j < nr; j++) {
                    float r0 = Mth.lerp(j / (float) nr, ri, ro), r1 = Mth.lerp((j + 1) / (float) nr, ri, ro);
                    quad(b, bb, sp, col, 1.0F, pt(centre, e0, r0, w, hh), pt(centre, e0, r1, w, hh), pt(centre, e1, r1, w, hh), pt(centre, e1, r0, w, hh));
                    quad(b, bb, sp, col, 0.5F, pt(centre, e1, r0, w, -hh), pt(centre, e1, r1, w, -hh), pt(centre, e0, r1, w, -hh), pt(centre, e0, r0, w, -hh));
                }
            }
        }
    }

    private static Vector3f dir(Vector3f u, Vector3f v, float a) {
        return new Vector3f(u).mul(Mth.cos(a)).add(new Vector3f(v).mul(Mth.sin(a)));
    }

    private static Vector3f pt(Vector3f c, Vector3f d, float r, Vector3f w, float h) {
        return new Vector3f(c).add(new Vector3f(d).mul(r)).add(new Vector3f(w).mul(h));
    }

    static void box(Builder b, VertexConsumer bb, TextureAtlasSprite sp, int col, Vector3f a, Vector3f c) {
        float x0 = Math.min(a.x, c.x), y0 = Math.min(a.y, c.y), z0 = Math.min(a.z, c.z), x1 = Math.max(a.x, c.x), y1 = Math.max(a.y, c.y), z1 = Math.max(a.z, c.z);
        quad(b, bb, sp, col, 1.0F, new Vector3f(x0, y1, z0), new Vector3f(x0, y1, z1), new Vector3f(x1, y1, z1), new Vector3f(x1, y1, z0));
        quad(b, bb, sp, col, 0.5F, new Vector3f(x0, y0, z1), new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y0, z1));
        quad(b, bb, sp, col, 0.8F, new Vector3f(x0, y0, z0), new Vector3f(x0, y1, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y0, z0));
        quad(b, bb, sp, col, 0.8F, new Vector3f(x1, y0, z1), new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1), new Vector3f(x0, y0, z1));
        quad(b, bb, sp, col, 0.6F, new Vector3f(x0, y0, z1), new Vector3f(x0, y1, z1), new Vector3f(x0, y1, z0), new Vector3f(x0, y0, z0));
        quad(b, bb, sp, col, 0.6F, new Vector3f(x1, y0, z0), new Vector3f(x1, y1, z0), new Vector3f(x1, y1, z1), new Vector3f(x1, y0, z1));
    }

    /** A quad, counter-clockwise from its front, with the whole sprite on it and shading s baked into its colour. */
    static void quad(Builder b, VertexConsumer bb, TextureAtlasSprite sp, int col, float s, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
        int r = (int) ((col >> 16 & 255) * s), g = (int) ((col >> 8 & 255) * s), bl = (int) ((col & 255) * s);
        int argb = 0xFF000000 | r << 16 | g << 8 | bl;
        Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p1)).normalize();
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        bb.addVertex(p0.x, p0.y, p0.z, argb, u0, v1, OverlayTexture.NO_OVERLAY, FULL, n.x, n.y, n.z);
        bb.addVertex(p1.x, p1.y, p1.z, argb, u1, v1, OverlayTexture.NO_OVERLAY, FULL, n.x, n.y, n.z);
        bb.addVertex(p2.x, p2.y, p2.z, argb, u1, v0, OverlayTexture.NO_OVERLAY, FULL, n.x, n.y, n.z);
        bb.addVertex(p3.x, p3.y, p3.z, argb, u0, v0, OverlayTexture.NO_OVERLAY, FULL, n.x, n.y, n.z);
        for (Vector3f p : new Vector3f[]{p0, p1, p2, p3}) b.reach(p.x, p.y, p.z);
        b.quads++;
    }

    private Animations() {}
}
