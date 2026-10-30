package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.entity.AxiomSentinel;
import com.theoreticalsciences.entity.FractalWyrm;
import com.theoreticalsciences.transcend.HyperobjectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Renderers for the expansion's creatures and the hyperobject. */
public final class ExpansionRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final double PHI = (1 + Math.sqrt(5)) / 2;

    /** The collapsed Omnilex: drawn by the effect renderer (a projected penteract with lensing). */
    public static class Hyperobject extends EntityRenderer<HyperobjectEntity> {
        public Hyperobject(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0F;
        }

        @Override
        public boolean shouldRender(HyperobjectEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
            ClientHooks.HYPEROBJECTS.add(e);     // always handed over: it is visible from far away
            return true;
        }

        @Override
        public ResourceLocation getTextureLocation(HyperobjectEntity e) {
            return WHITE;
        }
    }

    /**
     * Fractal Wyrm: a head and a tail of segments that follow the path the head took. Each segment is a cube with
     * four smaller cubes at its corners - the wyrm in miniature - and each is 0.86x the one in front.
     */
    public static class Wyrm extends EntityRenderer<FractalWyrm> {
        public Wyrm(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.5F;
        }

        @Override
        public void render(FractalWyrm e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            float die = e.deathTime > 0 ? 1F - Math.min(1F, (e.deathTime + partial) / 20F) : 1F;
            VertexConsumer solid = SafeBuffer.of(buf, RenderType.entitySolid(WHITE));
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            Vec3 base = e.getPosition(partial).add(0, e.getBbHeight() / 2, 0);
            Object[] trail = e.trail.toArray();
            int n = 16;
            for (int k = n - 1; k >= 0; k--) {
                int idx = Math.min(trail.length - 1, k * 4);
                Vec3 p = idx < 0 ? base : (Vec3) trail[idx];
                if (k == 0) p = base;
                Vec3 rel = p.subtract(e.getPosition(partial));
                float s = (float) (0.42 * Math.pow(0.86, k)) * die;
                if (k > 0 && die < 1F && k > n * die) continue;     // the tail unravels first
                ps.pushPose();
                ps.translate(rel.x, rel.y, rel.z);
                ps.mulPose(Axis.YP.rotationDegrees(t * 4 + k * 23));
                ps.mulPose(Axis.XP.rotationDegrees(k * 31 + Mth.sin(t * 0.2F + k) * 15));
                int col = (k & 1) == 0 ? 0xFF2A3A70 : 0xFF6A5420;
                ProcMesh.box(ps, solid, -s, -s, -s, s, s, s, col, light, ov);
                float c = s * 0.42F;
                for (int sx = -1; sx <= 1; sx += 2)
                    for (int sz = -1; sz <= 1; sz += 2) {
                        float ox = sx * s, oz = sz * s;
                        ProcMesh.box(ps, k % 3 == 0 ? glow : solid, ox - c, s - c * 0.5F, oz - c, ox + c, s + c * 1.5F, oz + c,
                                k % 3 == 0 ? 0xFFFFD060 : 0xFF8090C0, k % 3 == 0 ? 0xF000F0 : light, k % 3 == 0 ? OverlayTexture.NO_OVERLAY : ov);
                    }
                if (k == 0) {        // eyes
                    float es = s * 0.18F;
                    ProcMesh.box(ps, glow, -s * 0.6F - es, s * 0.2F, -s - 0.01F, -s * 0.6F + es, s * 0.2F + es * 2, -s + 0.02F, 0xFFFFE080, 0xF000F0, OverlayTexture.NO_OVERLAY);
                    ProcMesh.box(ps, glow, s * 0.6F - es, s * 0.2F, -s - 0.01F, s * 0.6F + es, s * 0.2F + es * 2, -s + 0.02F, 0xFFFFE080, 0xF000F0, OverlayTexture.NO_OVERLAY);
                }
                ps.popPose();
            }
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(FractalWyrm e) {
            return WHITE;
        }
    }

    /** Axiom Sentinel: a slowly tumbling icosahedron of pale stone around a glowing dodecahedral heart. */
    public static class Sentinel extends EntityRenderer<AxiomSentinel> {
        private static final float[][] ICO;
        private static final int[][] FACES;

        static {
            double p = PHI;
            double[][] v = {{-1, p, 0}, {1, p, 0}, {-1, -p, 0}, {1, -p, 0}, {0, -1, p}, {0, 1, p}, {0, -1, -p}, {0, 1, -p},
                    {p, 0, -1}, {p, 0, 1}, {-p, 0, -1}, {-p, 0, 1}};
            ICO = new float[12][3];
            double len = Math.sqrt(1 + p * p);
            for (int i = 0; i < 12; i++) for (int k = 0; k < 3; k++) ICO[i][k] = (float) (v[i][k] / len);
            FACES = new int[][]{{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                    {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        }

        public Sentinel(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.4F;
        }

        @Override
        public void render(AxiomSentinel e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            if (e.charge() > 0) ClientHooks.SENTINELS.add(e);
            float t = e.tickCount + partial;
            float die = e.deathTime > 0 ? 1F - Math.min(1F, (e.deathTime + partial) / 20F) : 1F;
            int ov = OverlayTexture.pack(0, e.hurtTime > 0);
            ps.pushPose();
            ps.translate(0, 0.7F + Mth.sin(t * 0.08F) * 0.1F, 0);
            ps.mulPose(Axis.YP.rotationDegrees(t * 1.5F));
            ps.mulPose(Axis.XP.rotationDegrees(t * 0.9F));
            float r = 0.7F;
            VertexConsumer vc = SafeBuffer.of(buf, RenderType.entityCutoutNoCull(WHITE));
            PoseStack.Pose pose = ps.last();
            for (int f = 0; f < FACES.length; f++) {
                int[] fc = FACES[f];
                // faces fly apart as it dies
                float[] cen = new float[3];
                for (int k = 0; k < 3; k++) cen[k] = (ICO[fc[0]][k] + ICO[fc[1]][k] + ICO[fc[2]][k]) / 3F;
                float push = (1 - die) * 0.8F;
                float shade = 0.75F + 0.25F * cen[1];
                int c = f % 5 == 0 ? 0xFFB0C8FF : 0xFFE8ECF8;
                float cr = ((c >> 16) & 255) / 255F * shade, cg = ((c >> 8) & 255) / 255F * shade, cb = (c & 255) / 255F * shade;
                for (int k = 0; k < 4; k++) {
                    int vi = fc[Math.min(k, 2)];
                    float x = (ICO[vi][0] + cen[0] * push) * r, y = (ICO[vi][1] + cen[1] * push) * r, z = (ICO[vi][2] + cen[2] * push) * r;
                    vc.addVertex(pose, x, y, z).setColor(cr, cg, cb, 1F).setUv(0, 0).setOverlay(ov).setLight(light).setNormal(pose, cen[0], cen[1], cen[2]);
                }
            }
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float pulse = 0.18F + 0.05F * Mth.sin(t * 0.3F) + 0.15F * e.charge() / (float) AxiomSentinel.CHARGE_TICKS;
            ps.mulPose(Axis.ZP.rotationDegrees(-t * 3));
            ProcMesh.box(ps, glow, -pulse, -pulse, -pulse, pulse, pulse, pulse, 0xFFFFE8A0, 0xF000F0, OverlayTexture.NO_OVERLAY);
            for (float[] v : ICO) {
                float s = 0.035F;
                ProcMesh.box(ps, glow, v[0] * r * 1.02F - s, v[1] * r * 1.02F - s, v[2] * r * 1.02F - s, v[0] * r * 1.02F + s, v[1] * r * 1.02F + s, v[2] * r * 1.02F + s,
                        0xFF80C0FF, 0xF000F0, OverlayTexture.NO_OVERLAY);
            }
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(AxiomSentinel e) {
            return WHITE;
        }
    }

    private ExpansionRenderers() {}
}
