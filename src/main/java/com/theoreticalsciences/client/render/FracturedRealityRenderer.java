package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.block.FracturedRealityBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A block caught in reality destabilisation. It is still the original block, but:
 * it fades in and out, jitters out of place, and splits into smaller copies of itself (which split again) -
 * like the 3D shadow of an object that really lives in more dimensions.
 */
public class FracturedRealityRenderer implements BlockEntityRenderer<FracturedRealityBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public FracturedRealityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.blocks = ctx.getBlockRenderDispatcher();
    }

    private static float hash(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    @Override
    public void render(FracturedRealityBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState state = be.original();
        if (state == null || state.getRenderShape() != RenderShape.MODEL) return;
        float t = (be.getLevel() == null ? 0 : be.getLevel().getGameTime()) + partial;
        long seed = be.getBlockPos().asLong();
        VertexConsumer base = SafeBuffer.of(buf, RenderType.translucent());

        // main body: mostly present, but it phases out and glitches sideways now and then
        int slot = (int) (t / 3);
        float alpha = 0.55F + 0.4F * Mth.sin(t * 0.13F + hash(seed, 1) * 6.28F);
        if (hash(seed, slot) < 0.12F) alpha *= 0.15F;
        ps.pushPose();
        if (hash(seed + 3, slot) < 0.2F)
            ps.translate((hash(seed, slot * 5L) - 0.5F) * 0.25F, (hash(seed, slot * 7L) - 0.5F) * 0.12F, (hash(seed, slot * 11L) - 0.5F) * 0.25F);
        drawBlock(state, ps, new Fade(base, alpha), light);
        ps.popPose();

        // fractal splitting: half-size copies drift out of the faces, each with quarter-size copies of its own
        fractal(state, ps, base, light, seed, t, 1, 0.5F);
    }

    private void fractal(BlockState state, PoseStack ps, VertexConsumer base, int light, long seed, float t, int level, float scale) {
        if (level > 2) return;
        int copies = level == 1 ? 3 : 2;
        for (int i = 0; i < copies; i++) {
            long s = seed * 31 + level * 7 + i;
            float speed = 0.02F + hash(s, 2) * 0.03F;
            float cyc = (t * speed + hash(s, 1)) % 1F;            // 0 -> 1 lifecycle: split off, drift, fade
            float fade = Mth.sin(cyc * Mth.PI);
            if (fade < 0.05F) continue;
            float dx = hash(s, 3) - 0.5F, dy = hash(s, 4) - 0.5F, dz = hash(s, 5) - 0.5F;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz) + 1e-4F;
            float dist = (0.25F + cyc * 0.55F) / level;
            ps.pushPose();
            ps.translate(0.5F + dx / len * dist, 0.5F + dy / len * dist, 0.5F + dz / len * dist);
            ps.mulPose(Axis.YP.rotationDegrees(cyc * 90F * (hash(s, 6) - 0.5F) * 4));
            ps.mulPose(Axis.XP.rotationDegrees(cyc * 60F * (hash(s, 7) - 0.5F) * 4));
            ps.scale(scale, scale, scale);
            ps.translate(-0.5F, -0.5F, -0.5F);
            drawBlock(state, ps, new Fade(base, 0.7F * fade), light);
            fractal(state, ps, base, light, s, t, level + 1, 0.5F);
            ps.popPose();
        }
    }

    private void drawBlock(BlockState state, PoseStack ps, VertexConsumer vc, int light) {
        var model = blocks.getBlockModel(state);
        int col = Minecraft.getInstance().getBlockColors().getColor(state, null, null, 0);
        float r = 1, g = 1, b = 1;
        if (col != -1) {
            r = ((col >> 16) & 255) / 255F;
            g = ((col >> 8) & 255) / 255F;
            b = (col & 255) / 255F;
        }
        blocks.getModelRenderer().renderModel(ps.last(), vc, state, model, r, g, b, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, RenderType.translucent());
    }

    @Override
    public AABB getRenderBoundingBox(FracturedRealityBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.5);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    /** Wraps a vertex consumer and multiplies every vertex's alpha, so a solid block model can be drawn see-through. */
    private record Fade(VertexConsumer inner, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            inner.setColor(r, g, b, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }
}
