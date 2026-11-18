package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.GameRenderer;

import java.util.function.Consumer;

/**
 * A part of a sky that never changes (1.3.0). It is built once and kept on the graphics card, instead of being
 * rebuilt from scratch and sent over every frame.
 */
final class BakedSky {
    private final VertexFormat.Mode mode;
    private final Consumer<BufferBuilder> builder;
    private VertexBuffer vb;

    BakedSky(VertexFormat.Mode mode, Consumer<BufferBuilder> builder) {
        this.mode = mode;
        this.builder = builder;
    }

    /** Draws it with the position/colour shader and the current matrices and blend state. */
    void draw() {
        if (vb == null) {
            BufferBuilder bb = Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
            builder.accept(bb);
            MeshData mesh = bb.build();
            if (mesh == null) return;
            vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(mesh);
            VertexBuffer.unbind();
        }
        vb.bind();
        vb.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
    }
}
