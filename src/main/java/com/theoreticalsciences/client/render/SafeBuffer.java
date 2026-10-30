package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Round 35: a vertex consumer that asks the buffer source for its render type's buffer on every call instead of
 * holding one. A renderer that took several buffers up front (solid, translucent, glowing) and then drew into them in
 * turn crashed with "Not building!" in packs whose buffer source keeps only one shared buffer open at a time (taking
 * the second buffer ended the first - seen with Sable/Veil in the Quantum Foam). Asking again each time always gets
 * the open one; for the same render type the source just hands back the buffer it already has.
 * <p>
 * The procedural meshes write each quad's four vertices in one go to one consumer, so a switch of buffer only ever
 * happens between quads, never inside one.
 */
public final class SafeBuffer implements VertexConsumer {
    private final MultiBufferSource src;
    private final RenderType type;
    private VertexConsumer cur;

    private SafeBuffer(MultiBufferSource src, RenderType type) {
        this.src = src;
        this.type = type;
    }

    public static VertexConsumer of(MultiBufferSource src, RenderType type) {
        return new SafeBuffer(src, type);
    }

    private VertexConsumer b() {
        return src.getBuffer(type);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        cur = b();
        cur.addVertex(x, y, z);
        return this;
    }

    private VertexConsumer c() {
        return cur != null ? cur : b();
    }

    @Override
    public VertexConsumer setColor(int r, int g, int bl, int a) {
        c().setColor(r, g, bl, a);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        c().setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        c().setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        c().setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        c().setNormal(x, y, z);
        return this;
    }
}
