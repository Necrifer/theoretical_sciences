package com.theoreticalsciences.client.render.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.theoreticalsciences.QHConfig;
import com.theoreticalsciences.client.render.effects.FxFrame;
import com.theoreticalsciences.client.render.effects.FxRenderer;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * 1.2.0: draws a formed multiblock's moving parts, as the game draws a moving piston's block: from the controller's
 * block entity renderer, through the game's own block buffers. The parts' movement (how far each has turned) is kept by
 * {@link FxRenderer}; this only draws them where they are now. Whenever the structure is formed its moving blocks are
 * hidden in the world, so this must draw them. Each part is drawn from its buffers already on the graphics card (baked
 * once), culled on its own by distance and view; with Sable, or the client config's movingPartsCompatibility, they go
 * through the game's own block buffers instead.
 */
public class MovingPartsRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    public MovingPartsRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MachineBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (!(be instanceof ControllerBlockEntity c) || !c.isFormed() || be.getLevel() == null) return;
        MultiblockDefinition def = c.definition();
        if (def == null) return;
        if (!def.moving.isEmpty()) draw(c, MovingParts.get(c), false, ps, buf);
        if (def.animation != null) draw(c, Animations.baked(def.animation), true, ps, buf);
    }

    private static void draw(ControllerBlockEntity c, Animations.Baked b, boolean extra, PoseStack ps, MultiBufferSource buf) {
        if (b == null) return;
        Matrix4f[] tr = FxRenderer.transformsFor(c, extra, b.anim);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double maxD = QHConfig.ANIMATION_DISTANCE.get();
        net.minecraft.client.renderer.culling.Frustum fr = FxRenderer.frustum;
        float sky = mc.level != null ? mc.level.getSkyDarken(1F) : 1F;
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        Matrix4f basis = FxFrame.basis(c.facing());
        ps.last().pose().mul(basis);
        ps.last().normal().mul(new org.joml.Matrix3f(basis));
        BlockPos at = c.getBlockPos();
        Matrix4f base = new Matrix4f().translate(at.getX() + 0.5F, at.getY() + 0.5F, at.getZ() + 0.5F).mul(FxFrame.basis(c.facing()));
        for (StructureAnimation.Part p : b.anim.ordered) {
            Animations.BakedPart bp = b.parts[p.index];
            if (bp == null) continue;
            Matrix4f m = tr != null ? tr[p.index] : new Matrix4f();
            Vector4f w = new Matrix4f(base).mul(m).transform(new Vector4f(p.pivot, 1));
            // each part on its own: too far, or out of view, and it is not drawn
            double dx = w.x - cam.x, dy = w.y - cam.y, dz = w.z - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz) - bp.radius;
            if (dist > maxD) continue;
            if (fr != null && !fr.isVisible(new AABB(w.x, w.y, w.z, w.x, w.y, w.z).inflate(bp.radius + 1))) continue;
            int lt;
            if (p.fullBright) lt = 0xF000F0;
            else {
                lt = LevelRenderer.getLightColor(c.getLevel(), BlockPos.containing(w.x, w.y, w.z));
                lt = Math.max(lt & 0xFFFF, 0x50) | (lt & 0xFFFF0000);       // never pitch black
            }
            ps.pushPose();
            ps.last().pose().mul(m);
            ps.last().normal().mul(new org.joml.Matrix3f(m));
            boolean drawn = false;
            if (!Animations.cpuOnly()) {
                // the fast way: the part's buffers already on the graphics card, drawn where the pose says
                float light = p.fullBright ? 1F : 0.25F + 0.75F * Math.max(net.minecraft.client.renderer.LightTexture.block(lt) / 15F,
                        net.minecraft.client.renderer.LightTexture.sky(lt) / 15F * sky);
                Matrix4f mv = new Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(ps.last().pose());
                for (int L = 0; L < Animations.LAYERS.length; L++) {
                    com.mojang.blaze3d.vertex.VertexBuffer vb = bp.layers[L];
                    if (vb == null) continue;
                    drawn = true;
                    net.minecraft.client.renderer.RenderType rt = Animations.LAYERS[L];
                    rt.setupRenderState();
                    net.minecraft.client.renderer.ShaderInstance sh = com.mojang.blaze3d.systems.RenderSystem.getShader();
                    if (sh != null) {
                        if (sh.CHUNK_OFFSET != null) sh.CHUNK_OFFSET.set(0F, 0F, 0F);
                        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(light, light, light, 1F);
                        vb.bind();
                        vb.drawWithShader(mv, com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix(), sh);
                        com.mojang.blaze3d.vertex.VertexBuffer.unbind();
                        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
                    }
                    rt.clearRenderState();
                }
            }
            if (!drawn)
                for (int L = 0; L < Animations.LAYERS.length; L++) {
                    Animations.Rec r = bp.cpu[L];
                    if (r == null || r.n == 0) continue;
                    r.emit(buf.getBuffer(Animations.LAYERS[L]), ps.last(), lt, OverlayTexture.NO_OVERLAY);
                }
            ps.popPose();
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineBlockEntity be) {
        // only a machine with moving parts needs drawing while its controller's own chunk is out of view
        if (!(be instanceof ControllerBlockEntity c) || c.definition() == null) return false;
        return !c.definition().moving.isEmpty() || c.definition().animation != null;
    }

    @Override
    public int getViewDistance() {
        return Math.max(64, QHConfig.ANIMATION_DISTANCE.get());
    }

    @Override
    public AABB getRenderBoundingBox(MachineBlockEntity be) {
        if (be instanceof ControllerBlockEntity c && c.definition() != null) {
            MultiblockDefinition d = c.definition();
            double r = Math.max(d.sizeX, Math.max(d.sizeY, d.sizeZ)) + 4;
            return new AABB(Vec3.atCenterOf(be.getBlockPos()), Vec3.atCenterOf(be.getBlockPos())).inflate(r);
        }
        return new AABB(be.getBlockPos()).inflate(2);
    }
}
