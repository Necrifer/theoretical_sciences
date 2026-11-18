package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.14.1: the Structure Assembler's marks. Places it could not fill (something in the way) are outlined in pulsing red
 * for fifteen seconds, drawn through walls so they can be found. (Before, a single particle was spawned inside the block
 * in the way, where nobody could see it.)
 */
public final class ObstructionMarks {
    private ObstructionMarks() {}

    static final int LIFE = 300;
    /** Most marks one message carries; a structure with more obstructions shows the first ones. */
    public static final int MAX = 1024;
    /** The world the marks belong to: they are timed against its clock, so a new world (or logging out) drops them. */
    private static Object markedLevel;
    static final Map<BlockPos, Long> MARKS = new HashMap<>();

    /** Server → client: mark these positions. */
    public record Marks(List<BlockPos> positions) implements CustomPacketPayload {
        public static final Type<Marks> TYPE = new Type<>(TheoreticalSciences.id("obstruction_marks"));
        public static final StreamCodec<FriendlyByteBuf, Marks> CODEC = StreamCodec.of((b, m) -> {
            int n = Math.min(MAX, m.positions.size());     // 1.3.0: write only what the reader takes (left-over bytes disconnect)
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) b.writeBlockPos(m.positions.get(i));
        }, b -> {
            int n = Math.min(MAX, b.readVarInt());
            List<BlockPos> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) l.add(b.readBlockPos());
            return new Marks(l);
        });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void receive(Marks m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (markedLevel != mc.level) {
            MARKS.clear();
            markedLevel = mc.level;
        }
        long until = mc.level.getGameTime() + LIFE;
        for (BlockPos p : m.positions()) MARKS.put(p.immutable(), until);
    }

    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || MARKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != markedLevel) {
            MARKS.clear();
            return;
        }
        long now = mc.level.getGameTime();
        MARKS.values().removeIf(t -> t < now);
        if (MARKS.isEmpty()) return;
        float pulse = 0.55F + 0.45F * (float) Math.sin((now + e.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.35);
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        // seen through walls: the obstruction is often inside the structure
        VertexConsumer lines = buf.getBuffer(RenderType.LINES);
        for (BlockPos p : MARKS.keySet()) {
            AABB box = new AABB(p).inflate(0.004);
            LevelRenderer.renderLineBox(ps, lines, box, 1F, 0.15F, 0.12F, pulse);
            LevelRenderer.renderLineBox(ps, lines, box.deflate(0.12), 1F, 0.55F, 0.2F, pulse * 0.6F);
        }
        buf.endBatch(RenderType.LINES);
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        VertexConsumer through = buf.getBuffer(RenderType.LINES);
        for (BlockPos p : MARKS.keySet()) LevelRenderer.renderLineBox(ps, through, new AABB(p).inflate(0.004), 1F, 0.2F, 0.15F, pulse * 0.45F);
        buf.endBatch(RenderType.LINES);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        ps.popPose();
    }
}
