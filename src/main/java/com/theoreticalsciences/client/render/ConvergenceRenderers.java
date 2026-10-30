package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.converge.AltarBlockEntity;
import com.theoreticalsciences.converge.PedestalBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * The Convergence ritual, drawn on the client from the altar's synced progress alone.
 * <p>
 * Each used pedestal i gets its own window of the gathering ({@link AltarBlockEntity#pedestalPhase}); inside it (u = 0..1)
 * light runs out from the altar in the pedestal's tier colour (0-0.3), the item there dissolves into it (0-0.3 .. 0.7),
 * and the light pulls back carrying a glow into the altar (0.7-1). Around the core a hypersphere (a rotating tesseract
 * shadow inside a glome's great circles) thickens with every item taken. Then it rises and throws arcs of lightning
 * (GATHER-SPARK), and collapses into a point with a flash (SPARK-1); the server then puts the result on the altar.
 * <p>
 * The altar renderer publishes each used pedestal's phase in {@link #PHASES}; the pedestal renderer reads it to shrink its item.
 */
public final class ConvergenceRenderers {
    private ConvergenceRenderers() {}

    static final int[] TIER_RGB = {0xB070FF, 0xFFC050, 0xFFFFFF};
    static final int SPARK_RGB = 0x9FD8FF;

    /** Pedestal position -> its phase in a running ritual, as last drawn by the altar (client render thread only). */
    private static final Long2ObjectOpenHashMap<Phase> PHASES = new Long2ObjectOpenHashMap<>();

    private record Phase(float u, long stamp) {}

    static void item(ItemStack s, PoseStack ps, MultiBufferSource buf, int light, float scale, float spin, int seed) {
        if (s.isEmpty() || scale <= 0.005F) return;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf,
                Minecraft.getInstance().level, seed);
        ps.popPose();
    }

    private static float clamp01(float x) {
        return x < 0 ? 0 : x > 1 ? 1 : x;
    }

    private static float smooth(float x) {
        x = clamp01(x);
        return x * x * (3 - 2 * x);
    }

    /** How far the item on a pedestal has dissolved at phase u: 0 whole, 1 gone. */
    static float dissolve(float u) {
        return smooth((u - 0.3F) / 0.4F);
    }

    // ====================================================================== pedestal
    public static class Pedestal implements BlockEntityRenderer<PedestalBlockEntity> {
        public Pedestal(BlockEntityRendererProvider.Context c) {}

        @Override
        public void render(PedestalBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.getLevel() == null || be.stack().isEmpty()) return;
            long now = be.getLevel().getGameTime();
            float t = now + pt;
            float d = 0;                       // 0 whole .. 1 dissolved into the altar's light
            long key = be.getBlockPos().asLong();
            Phase ph = PHASES.get(key);
            if (ph != null && now >= ph.stamp && now - ph.stamp <= 1) d = dissolve(ph.u);
            else {
                if (ph != null) PHASES.remove(key);
                if (now < be.fadeUntil) d = 1 - be.fade;     // (the altar is not being drawn: its tick still tells us)
            }
            if (d >= 0.999F) return;
            ps.pushPose();
            ps.translate(0.5, 1.15 + Math.sin(t / 12.0 + be.getBlockPos().hashCode()) * 0.04 * (1 - d) + 0.18 * d, 0.5);
            float s = 0.45F * (1 - d) * (1 - d * 0.3F);
            item(be.stack(), ps, buf, d > 0 ? LightTexture.FULL_BRIGHT : light, s, (t * (2 + 25 * d)) % 360, (int) key);
            ps.popPose();
        }

        @Override
        public AABB getRenderBoundingBox(PedestalBlockEntity be) {
            return new AABB(be.getBlockPos()).expandTowards(0, 1, 0);
        }
    }

    // ====================================================================== altar
    public static class Altar implements BlockEntityRenderer<AltarBlockEntity> {
        public Altar(BlockEntityRendererProvider.Context c) {}

        @Override
        public void render(AltarBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.getLevel() == null) return;
            float t = be.getLevel().getGameTime() + pt;
            if (!be.output().isEmpty()) {
                ps.pushPose();
                ps.translate(0.5, 0.86, 0.5);
                ps.mulPose(Axis.XP.rotationDegrees(90));
                item(be.output(), ps, buf, light, 0.5F, 0, 2);
                ps.popPose();
            }
            if (!be.crafting()) {
                ps.pushPose();
                ps.translate(0.5, 1.25 + Math.sin(t / 10.0) * 0.05, 0.5);
                item(be.core(), ps, buf, light, 0.6F, (t * 1.5F) % 360, 1);
                ps.popPose();
            } else ritual(be, t, be.progress(pt), ps, buf);
        }

        private void ritual(AltarBlockEntity be, float t, float f, PoseStack ps, MultiBufferSource buf) {
            var used = be.usedPedestals();
            int n = used.size();
            long now = be.getLevel().getGameTime();
            BlockPos o = be.getBlockPos();

            float gathered = 0;
            for (int i = 0; i < n; i++) {
                float u = be.pedestalPhase(i, f);
                PHASES.put(used.get(i).asLong(), new Phase(u, now));
                gathered += dissolve(u);
            }
            gathered = n == 0 ? 1 : gathered / n;
            float lift = smooth((f - AltarBlockEntity.GATHER) / (AltarBlockEntity.RISE - AltarBlockEntity.GATHER));
            float c = clamp01((f - AltarBlockEntity.SPARK) / (1 - AltarBlockEntity.SPARK));      // collapse 0..1
            float keep = (1 - c) * (1 - c);
            double cx = 0.5, cy = 1.3 + 0.8 * lift, cz = 0.5;

            // the core, enveloped (items first: the light below uses one buffer that other render types could flush)
            ps.pushPose();
            ps.translate(cx, cy, cz);
            item(be.core(), ps, buf, LightTexture.FULL_BRIGHT, 0.55F * keep, (t * (3 + 12 * gathered + 40 * c)) % 360, 1);
            ps.popPose();

            VertexConsumer vc = SafeBuffer.of(buf, RenderType.lightning());
            Matrix4f m = ps.last().pose();

            // ---- the beams
            for (int i = 0; i < n; i++) {
                float u = be.pedestalPhase(i, f);
                if (u <= 0 || u >= 1) continue;
                BlockPos p = used.get(i);
                int rgb = TIER_RGB[Mth.clamp(be.pedestalTier(p) - 1, 0, 2)];
                double ax = 0.5, ay = 1.3, az = 0.5;
                double bx = p.getX() - o.getX() + 0.5, by = p.getY() - o.getY() + 1.15 + 0.18 * dissolve(u), bz = p.getZ() - o.getZ() + 0.5;
                // out (0-0.3), held while the item dissolves (0.3-0.7), back (0.7-1)
                float to = smooth(u / 0.3F), from = smooth((u - 0.7F) / 0.3F);
                double hx = ax + (bx - ax) * to, hy = ay + (by - ay) * to, hz = az + (bz - az) * to;          // the head going out
                double tx = bx + (ax - bx) * from, ty = by + (ay - by) * from, tz = bz + (az - bz) * from;    // the tail coming back
                double x1 = from > 0 ? tx : hx, y1 = from > 0 ? ty : hy, z1 = from > 0 ? tz : hz;           // the far end
                float w = 0.04F + 0.015F * (float) Math.sin(t * 0.9 + i * 1.7);
                float a = from > 0 ? 1 - 0.5F * from : 1;
                beam(m, vc, ax, ay, az, x1, y1, z1, w, rgb, 0.8F * a);
                beam(m, vc, ax, ay, az, x1, y1, z1, w * 2.8F, rgb, 0.18F * a);
                float d = dissolve(u);
                // the item's light: grows at the pedestal as it dissolves, then rides the retreating beam home
                if (to >= 1 && from <= 0) star(m, vc, bx, by, bz, 0.06F + 0.16F * d, rgb, 0.25F + 0.5F * d, t + i * 7);
                if (from > 0) star(m, vc, tx, ty, tz, 0.2F * (1 - 0.4F * from), rgb, 0.8F, t + i * 7);
                // motes drawn along the beam towards the altar while the item comes apart
                if (u > 0.3F && from <= 0)
                    for (int k = 0; k < 4; k++) {
                        double s = (t * 0.045 + k / 4.0) % 1.0, s2 = Math.min(1, s + 0.07);
                        beam(m, vc, bx + (ax - bx) * s, by + (ay - by) * s, bz + (az - bz) * s,
                                bx + (ax - bx) * s2, by + (ay - by) * s2, bz + (az - bz) * s2, 0.025F, rgb, 0.7F * d);
                    }
            }

            // ---- the hypersphere
            float r = (0.18F + 0.42F * gathered) * keep;
            if (r > 0.005F) {
                float spin = 1 + 2 * lift + 6 * c;
                int tint = lerpRgb(0xC090FF, 0xFFFFFF, gathered);
                if (f >= AltarBlockEntity.RISE && c <= 0) tint = lerpRgb(tint, SPARK_RGB, 0.6F);
                float fill = 0.3F + 0.7F * gathered;
                tesseract(m, vc, cx, cy, cz, r, t * 0.05 * spin, t * 0.031 * spin, 0.008F + 0.012F * r, tint, 0.85F * fill);
                tesseract(m, vc, cx, cy, cz, r * 0.5F, -t * 0.07 * spin, t * 0.043 * spin, 0.006F + 0.008F * r, tint, 0.6F * fill);
                for (int k = 0; k < 3; k++)
                    ring(m, vc, cx, cy, cz, r * 1.05F, t * 0.02 * spin + k * 2.094, k * 1.05 + 0.4, 0.006F + 0.006F * r, tint, 0.35F * gathered);
                star(m, vc, cx, cy, cz, r * 0.35F, tint, 0.2F + 0.2F * gathered, t);
            }

            // ---- lightning while it hangs in the air
            if (f >= AltarBlockEntity.RISE && c <= 0) {
                long tick = (long) (t / 2);
                for (int k = 0; k < 4; k++) {
                    java.util.Random rnd = new java.util.Random(o.asLong() * 31 + tick * 7 + k);
                    double th = rnd.nextDouble() * Math.PI * 2, ph = (rnd.nextDouble() - 0.5) * Math.PI;
                    double dx = Math.cos(th) * Math.cos(ph), dy = Math.sin(ph), dz = Math.sin(th) * Math.cos(ph);
                    double ex, ey, ez;
                    if (k == 0 && n > 0) {          // one arc each time grounds itself on a pedestal
                        BlockPos p = used.get(rnd.nextInt(n));
                        ex = p.getX() - o.getX() + 0.5; ey = p.getY() - o.getY() + 1.05; ez = p.getZ() - o.getZ() + 0.5;
                    } else {
                        double len = r + 0.5 + rnd.nextDouble() * 0.9;
                        ex = cx + dx * len; ey = cy + dy * len; ez = cz + dz * len;
                    }
                    bolt(m, vc, rnd, cx + dx * r, cy + dy * r, cz + dz * r, ex, ey, ez);
                }
            }

            // ---- the collapse: light falls inward, then a flash
            if (c > 0) {
                for (int k = 0; k < 8; k++) {
                    double th = k * Math.PI / 4 + t * 0.1, ph = (k % 2 == 0 ? 0.5 : -0.5);
                    double dx = Math.cos(th) * Math.cos(ph), dy = Math.sin(ph), dz = Math.sin(th) * Math.cos(ph);
                    double far = 1.6 * (1 - c) + 0.05, near = far * 0.35;
                    beam(m, vc, cx + dx * far, cy + dy * far, cz + dz * far, cx + dx * near, cy + dy * near, cz + dz * near, 0.02F, 0xFFFFFF, 0.6F * (1 - c));
                }
                if (c > 0.7F) {
                    float fl = (float) Math.sin(Math.PI * (c - 0.7F) / 0.3F);
                    star(m, vc, cx, cy, cz, 1.1F * fl, 0xFFFFFF, 0.9F * fl, t * 3);
                    star(m, vc, cx, cy, cz, 0.45F * fl, 0xFFFFFF, 1F, -t * 3);
                }
            }
        }

        @Override
        public AABB getRenderBoundingBox(AltarBlockEntity be) {
            return new AABB(be.getBlockPos()).inflate(AltarBlockEntity.RADIUS + 1, 3, AltarBlockEntity.RADIUS + 1);
        }

        @Override
        public boolean shouldRenderOffScreen(AltarBlockEntity be) {
            return be.crafting();
        }
    }

    // ====================================================================== drawing helpers
    static int lerpRgb(int a, int b, float x) {
        int r = (int) Mth.lerp(x, (a >> 16) & 255, (b >> 16) & 255), g = (int) Mth.lerp(x, (a >> 8) & 255, (b >> 8) & 255),
                bl = (int) Mth.lerp(x, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    /** The shadow of a 4-cube rotating in the xw and yz planes, projected into the world. */
    static void tesseract(Matrix4f m, VertexConsumer vc, double cx, double cy, double cz, float r, double a, double b, float w, int rgb, float alpha) {
        double[] px = new double[16], py = new double[16], pz = new double[16];
        double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b);
        for (int i = 0; i < 16; i++) {
            double x = (i & 1) == 0 ? -1 : 1, y = (i & 2) == 0 ? -1 : 1, z = (i & 4) == 0 ? -1 : 1, q = (i & 8) == 0 ? -1 : 1;
            double x2 = x * ca - q * sa, w2 = x * sa + q * ca;
            double y2 = y * cb - z * sb, z2 = y * sb + z * cb;
            double k = 1.0 / (2.6 - w2);
            px[i] = cx + x2 * k * r; py[i] = cy + y2 * k * r; pz[i] = cz + z2 * k * r;
        }
        for (int i = 0; i < 16; i++)
            for (int bit = 0; bit < 4; bit++) {
                int j = i ^ (1 << bit);
                if (j > i) beam(m, vc, px[i], py[i], pz[i], px[j], py[j], pz[j], w, rgb, alpha);
            }
    }

    /** A great circle of the glome, tilted by {@code tilt} and turned by {@code yaw}. */
    static void ring(Matrix4f m, VertexConsumer vc, double cx, double cy, double cz, float r, double yaw, double tilt, float w, int rgb, float alpha) {
        if (alpha <= 0.01F) return;
        final int seg = 20;
        double cy0 = Math.cos(yaw), sy0 = Math.sin(yaw), ct = Math.cos(tilt), st = Math.sin(tilt);
        double lx = 0, ly = 0, lz = 0;
        for (int s = 0; s <= seg; s++) {
            double an = s * Math.PI * 2 / seg, x = Math.cos(an) * r, z = Math.sin(an) * r;
            double y = z * st;
            z = z * ct;
            double x2 = x * cy0 - z * sy0, z2 = x * sy0 + z * cy0;
            if (s > 0) beam(m, vc, cx + lx, cy + ly, cz + lz, cx + x2, cy + y, cz + z2, w, rgb, alpha);
            lx = x2; ly = y; lz = z2;
        }
    }

    /** A jagged arc of lightning from a to b, with a faint halo. */
    private static void bolt(Matrix4f m, VertexConsumer vc, java.util.Random rnd, double x0, double y0, double z0, double x1, double y1, double z1) {
        final int seg = 6;
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        double jit = 0.12 * Math.min(len, 3);
        double lx = x0, ly = y0, lz = z0;
        for (int s = 1; s <= seg; s++) {
            double q = s / (double) seg;
            double nx = x0 + (x1 - x0) * q, ny = y0 + (y1 - y0) * q, nz = z0 + (z1 - z0) * q;
            if (s < seg) {
                nx += (rnd.nextDouble() - 0.5) * 2 * jit; ny += (rnd.nextDouble() - 0.5) * 2 * jit; nz += (rnd.nextDouble() - 0.5) * 2 * jit;
            }
            beam(m, vc, lx, ly, lz, nx, ny, nz, 0.014F, 0xFFFFFF, 0.95F);
            beam(m, vc, lx, ly, lz, nx, ny, nz, 0.05F, SPARK_RGB, 0.3F);
            lx = nx; ly = ny; lz = nz;
        }
    }

    /** A small burst of light: three crossed beams through a point. */
    static void star(Matrix4f m, VertexConsumer vc, double x, double y, double z, float size, int rgb, float alpha, float t) {
        if (size <= 0.005F || alpha <= 0.01F) return;
        double a = t * 0.07, ca = Math.cos(a) * size, sa = Math.sin(a) * size;
        float w = size * 0.28F;
        beam(m, vc, x - ca, y, z - sa, x + ca, y, z + sa, w, rgb, alpha);
        beam(m, vc, x + sa, y, z - ca, x - sa, y, z + ca, w, rgb, alpha);
        beam(m, vc, x, y - size, z, x, y + size, z, w, rgb, alpha);
    }

    /** A beam of light from a to b: two crossed double-sided quads. */
    static void beam(Matrix4f m, VertexConsumer vc, double x0, double y0, double z0, double x1, double y1, double z1,
                             float w, int rgb, float alpha) {
        if (alpha <= 0.005F) return;
        float r = ((rgb >> 16) & 255) / 255F, g = ((rgb >> 8) & 255) / 255F, b = (rgb & 255) / 255F;
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4) return;
        double ux, uy, uz;
        if (Math.abs(dy) < 0.9 * len) { ux = -dz; uy = 0; uz = dx; } else { ux = 1; uy = 0; uz = 0; }
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
        double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        vx /= vl; vy /= vl; vz /= vl;
        for (int k = 0; k < 2; k++) {
            float sx = (float) ((k == 0 ? ux : vx) * w), sy = (float) ((k == 0 ? uy : vy) * w), sz = (float) ((k == 0 ? uz : vz) * w);
            float ax = (float) x0, ay = (float) y0, az = (float) z0, bx = (float) x1, by = (float) y1, bz = (float) z1;
            vc.addVertex(m, ax - sx, ay - sy, az - sz).setColor(r, g, b, alpha);
            vc.addVertex(m, ax + sx, ay + sy, az + sz).setColor(r, g, b, alpha);
            vc.addVertex(m, bx + sx, by + sy, bz + sz).setColor(r, g, b, alpha);
            vc.addVertex(m, bx - sx, by - sy, bz - sz).setColor(r, g, b, alpha);
            vc.addVertex(m, bx - sx, by - sy, bz - sz).setColor(r, g, b, alpha);
            vc.addVertex(m, bx + sx, by + sy, bz + sz).setColor(r, g, b, alpha);
            vc.addVertex(m, ax + sx, ay + sy, az + sz).setColor(r, g, b, alpha);
            vc.addVertex(m, ax - sx, ay - sy, az - sz).setColor(r, g, b, alpha);
        }
    }
}
