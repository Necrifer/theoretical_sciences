package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.boss.AlphaOmegaEntity;
import com.theoreticalsciences.boss.GenesisEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Alpha Omega is drawn as a penteract - a five-dimensional cube - projected down to three dimensions while it turns
 * through planes we cannot see. The materials of the shattered Omnilex ride on its 32 corners.
 */
public final class AlphaOmegaRenderers {
    private static final ResourceLocation WHITE = QHEntityRenderers.WHITE;
    private static final int FULL = 0xF000F0;
    static final float[][] V = new float[32][5];
    static final List<int[]> E = new ArrayList<>();

    static {
        for (int i = 0; i < 32; i++) for (int k = 0; k < 5; k++) V[i][k] = ((i >> k) & 1) == 1 ? 1 : -1;
        for (int i = 0; i < 32; i++) for (int k = 0; k < 5; k++) if (((i >> k) & 1) == 0) E.add(new int[]{i, i | (1 << k)});
    }

    /** Rotates through four planes (two of them involving the hidden 4th and 5th axes) and projects down to 3D. */
    static float[][] project(float t, float scale) {
        float[][] out = new float[32][4];
        float a = t * 0.013F, b = t * 0.009F, c = t * 0.007F, d = t * 0.005F;
        for (int i = 0; i < 32; i++) {
            float[] p = V[i].clone();
            rot(p, 0, 3, a);
            rot(p, 1, 4, b);
            rot(p, 2, 3, c);
            rot(p, 3, 4, d);
            float k5 = 3.2F / (3.2F - p[4]);
            float x = p[0] * k5, y = p[1] * k5, z = p[2] * k5, w = p[3] * k5;
            float k4 = 3.2F / (3.2F - w * 0.5F);
            out[i][0] = x * k4 * scale;
            out[i][1] = y * k4 * scale;
            out[i][2] = z * k4 * scale;
            out[i][3] = p[3] + p[4];
        }
        return out;
    }

    static void rot(float[] p, int i, int j, float ang) {
        float c = Mth.cos(ang), s = Mth.sin(ang), pi = p[i], pj = p[j];
        p[i] = pi * c - pj * s;
        p[j] = pi * s + pj * c;
    }

    static List<ItemStack> materials;

    /** 32 of the mod's own items, spread evenly through the registry: the Omnilex, taken apart. */
    static List<ItemStack> materials() {
        if (materials != null) return materials;
        List<Item> all = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM)
            if (BuiltInRegistries.ITEM.getKey(it).getNamespace().equals(TheoreticalSciences.MODID) && !(it instanceof net.minecraft.world.item.SpawnEggItem)) all.add(it);
        materials = new ArrayList<>();
        for (int i = 0; i < 32 && !all.isEmpty(); i++) materials.add(new ItemStack(all.get(i * all.size() / 32)));
        return materials;
    }

    static int colour(float depth, float t) {
        float h = 0.12F + 0.08F * Mth.sin(t * 0.03F + depth);
        return 0xFF000000 | Mth.hsvToRgb(h, 0.55F, 1.0F);
    }

    static void wire(PoseStack ps, VertexConsumer glow, float[][] v, float w, float t) {
        for (int[] e : E) {
            float[] a = v[e[0]], b = v[e[1]];
            BulkCreatureRenderers.Strider.limb(ps, glow, a[0], a[1], a[2], b[0], b[1], b[2], w, colour(a[3] + b[3], t), FULL, OverlayTexture.NO_OVERLAY);
        }
    }

    static void item(PoseStack ps, MultiBufferSource buf, ItemStack s, float x, float y, float z, float spin, float size) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.scale(size, size, size);
        Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.GROUND, FULL, OverlayTexture.NO_OVERLAY, ps, buf, Minecraft.getInstance().level, 0);
        ps.popPose();
    }

    // ------------------------------------------------------------------ the genesis
    public static class Genesis extends EntityRenderer<GenesisEntity> {
        public Genesis(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0F;
        }

        @Override
        public void render(GenesisEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.age() + partial;
            List<ItemStack> mats = materials();
            int n = mats.size();
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            float[][] shape = project(t, 1.8F);
            for (int i = 0; i < n; i++) {
                // a Fibonacci sphere of directions for the shattering
                float y = 1 - (i + 0.5F) / n * 2, r = Mth.sqrt(1 - y * y), th = i * 2.39996F;
                float dx = Mth.cos(th) * r, dz = Mth.sin(th) * r;
                float px, py, pz;
                if (t < GenesisEntity.SHATTER) {
                    float k = t / GenesisEntity.SHATTER, out = 0.3F + 7.5F * (1 - (1 - k) * (1 - k));
                    px = dx * out;
                    py = y * out;
                    pz = dz * out;
                } else {
                    float k = Math.min(1F, (t - GenesisEntity.SHATTER) / (GenesisEntity.ASSEMBLE - GenesisEntity.SHATTER));
                    k = k * k * (3 - 2 * k);
                    float[] v = shape[i % 32];
                    px = Mth.lerp(k, dx * 7.8F, v[0]);
                    py = Mth.lerp(k, y * 7.8F, v[1]);
                    pz = Mth.lerp(k, dz * 7.8F, v[2]);
                }
                item(ps, buf, mats.get(i), px, py, pz, t * 6 + i * 40, 0.9F);
            }
            glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));     // item rendering may have ended the earlier batch
            if (t >= GenesisEntity.ASSEMBLE - 20) {
                float grow = Math.min(1F, (t - GenesisEntity.ASSEMBLE + 20) / 30F);
                ps.pushPose();
                ps.scale(grow, grow, grow);
                wire(ps, glow, shape, 0.04F, t);
                ps.popPose();
            }
            if (t < 12) {        // the flash of the Omnilex breaking
                float s = 0.5F + t * 0.4F;
                ProcMesh.box(ps, glow, -s, -s, -s, s, s, s, ProcMesh.withAlpha(0xFFFFF0, 1 - t / 12F), FULL, OverlayTexture.NO_OVERLAY);
            }
            super.render(e, yaw, partial, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(GenesisEntity e) {
            return WHITE;
        }
    }

    // ------------------------------------------------------------------ the boss
    public static class Boss extends EntityRenderer<AlphaOmegaEntity> {
        public Boss(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 1.2F;
        }

        @Override
        public void render(AlphaOmegaEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.tickCount + partial;
            VertexConsumer glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));
            ps.pushPose();
            ps.translate(0, 2.0F, 0);
            float hurt = e.hurtTime > 0 ? 1.15F : 1F;
            float[][] shape = project(t * (e.attack() != 0 ? 2.5F : 1F), 1.5F * hurt);
            wire(ps, glow, shape, 0.06F, t);
            List<ItemStack> mats = materials();
            for (int i = 0; i < 32 && i < mats.size(); i++) item(ps, buf, mats.get(i), shape[i][0], shape[i][1], shape[i][2], t * 4 + i * 20, 0.8F);
            glow = SafeBuffer.of(buf, RenderType.eyes(WHITE));     // item rendering may have ended the earlier batch
            // a white core that pulses with its heartbeat
            float beat = 0.35F + 0.08F * Mth.sin(t * 0.3F);
            ProcMesh.box(ps, glow, -beat, -beat, -beat, beat, beat, beat, 0xFFFFF4D0, FULL, OverlayTexture.NO_OVERLAY);
            if (e.attack() != 0) {
                int col = switch (e.attack()) {
                    case 1 -> 0x80F0FF;
                    case 2 -> 0xFF6020;
                    case 3 -> 0xFFD060;
                    case 4 -> 0x4040A0;
                    default -> 0xC080FF;
                };
                float s = 1.0F + 0.2F * Mth.sin(t);
                ProcMesh.box(ps, glow, -s, -s, -s, s, s, s, ProcMesh.withAlpha(col, 0.35F), FULL, OverlayTexture.NO_OVERLAY);
            }
            if (e.shielded()) {      // closed into a proof: a cage of glowing faces
                VertexConsumer trans = SafeBuffer.of(buf, RenderType.entityTranslucentEmissive(WHITE));
                float s = 3.6F;
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(t * 2));
                ps.mulPose(Axis.XP.rotationDegrees(t * 1.3F));
                ProcMesh.box(ps, trans, -s, -s, -s, s, s, s, 0x40C080FF, FULL, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            if (e.deathTime > 0) rays(ps, buf, (e.deathTime + partial) / AlphaOmegaEntity.DEATH_TICKS);
            ps.popPose();
            super.render(e, yaw, partial, ps, buf, light);
        }

        /** Rays of light pouring out of it as it dies. */
        static void rays(PoseStack ps, MultiBufferSource buf, float f) {
            float fade = Math.min(f > 0.8F ? (f - 0.8F) / 0.2F : 0F, 1F);
            RandomSource r = RandomSource.create(432L);
            VertexConsumer vc = SafeBuffer.of(buf, RenderType.dragonRays());
            ps.pushPose();
            for (int i = 0; i < (f + f * f) / 2F * 90F; i++) {
                ps.mulPose(Axis.XP.rotationDegrees(r.nextFloat() * 360F));
                ps.mulPose(Axis.YP.rotationDegrees(r.nextFloat() * 360F));
                ps.mulPose(Axis.ZP.rotationDegrees(r.nextFloat() * 360F));
                ps.mulPose(Axis.XP.rotationDegrees(r.nextFloat() * 360F));
                ps.mulPose(Axis.YP.rotationDegrees(r.nextFloat() * 360F));
                ps.mulPose(Axis.ZP.rotationDegrees(r.nextFloat() * 360F + f * 90F));
                float len = r.nextFloat() * 24F + 6F + fade * 12F, w = r.nextFloat() * 2F + 1F + fade * 2F;
                Matrix4f m = ps.last().pose();
                int a = (int) (255F * (1F - fade));
                float h = 0.8660254F;
                tri(vc, m, a, -h * w, len, -0.5F * w, h * w, len, -0.5F * w);
                tri(vc, m, a, h * w, len, -0.5F * w, 0, len, w);
                tri(vc, m, a, 0, len, w, -h * w, len, -0.5F * w);
            }
            ps.popPose();
        }

        static void tri(VertexConsumer vc, Matrix4f m, int a, float x1, float y1, float z1, float x2, float y2, float z2) {
            vc.addVertex(m, 0, 0, 0).setColor(255, 250, 220, a);
            vc.addVertex(m, x1, y1, z1).setColor(255, 200, 80, 0);
            vc.addVertex(m, x2, y2, z2).setColor(255, 200, 80, 0);
        }

        @Override
        public ResourceLocation getTextureLocation(AlphaOmegaEntity e) {
            return WHITE;
        }
    }

    private AlphaOmegaRenderers() {}
}
