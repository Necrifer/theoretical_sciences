package com.theoreticalsciences.client.render.effects;

import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: the Pleroma Reconstitutor at work: the Transcendental's rekindling (the cinematic after the Shutdown) traced
 * over and over above the machine, at the machine's scale, as if it were rehearsing it: seven pillars of light climb,
 * motes spiral up between them, the central spire is drawn and crowned, the great halo is traced round, then the five
 * rings one after another with their spokes, flickering like an old recording; it holds whole a moment, fades, and
 * begins again.
 */
public final class TranscendentalEcho {
    private static final int GOLD = 0xFFE8A0, WHITE = 0xFFFFFF, AMBER = 0xFFB040;
    /** One loop: build up (as the cinematic, 170 ticks), hold, fade. */
    private static final float BUILD = 170, HOLD = 80, FADE = 40, LOOP = BUILD + HOLD + FADE;

    public static void draw(Vec3 c, float s, float sky, float timeSeconds, float power) {
        if (power <= 0.003F) return;
        float a = (timeSeconds * 20) % LOOP;
        float fade = (1 - smooth((a - BUILD - HOLD) / FADE)) * power;
        double top = c.y + sky * s;
        float grow = smooth(a / 50F);
        float beamA = fade * (a < 70 ? 1 : 0.55F);
        double height = grow * (sky + 10) * s;
        for (int i = 0; i < 7; i++) {
            double ang = i * Math.PI * 2 / 6 + a * 0.01;
            double r = i == 6 ? 0 : 1.6 * s;
            Vec3 b = new Vec3(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r);
            Vec3 t = b.add(0, height, 0);
            float w = (i == 6 ? 0.8F : 0.35F) * s;
            FxDraw.ribbon(b, t, w * 3, GOLD, 0.15F * beamA, GOLD, 0);
            FxDraw.ribbon(b, t, w, WHITE, 0.8F * beamA, GOLD, 0.1F * beamA);
        }
        for (int i = 0; i < 40; i++) {
            double q = frac(rnd(i, 1) + a * 0.01F * (0.6F + rnd(i, 2)));
            double ang = a * 0.05 + i, r = (1 + rnd(i, 3) * 2.5) * s;
            FxDraw.glow(new Vec3(c.x + Math.cos(ang) * r, c.y + q * (sky + 10) * s, c.z + Math.sin(ang) * r), 0.15F * s, GOLD, 0.8F * fade * (float) (1 - q));
        }
        float echo = a < BUILD - 10 ? 0.82F + 0.18F * (float) Math.sin(a * 1.7) : 1;
        float sa = fade * echo;
        float spire = smooth((a - 50) / 40F);
        if (spire > 0) {
            Vec3 s0 = new Vec3(c.x, top - 6 * s, c.z), s1 = s0.add(0, spire * 20 * s, 0);
            FxDraw.ribbon(s0, s1, 0.9F * s, GOLD, 0.2F * sa, WHITE, 0.2F * sa);
            FxDraw.ribbon(s0, s1, 0.18F * s, WHITE, 0.95F * sa, WHITE, 0.95F * sa);
            FxDraw.glow(s1, 0.6F * s, WHITE, sa);
            if (spire >= 1) crown(new Vec3(c.x, top + 14 * s, c.z), 1.3F * s, a * 0.04F, smooth((a - 90) / 12F) * sa, s);
        }
        arc(new Vec3(c.x, top + 10 * s, c.z), 9 * s, 0.12, a * 0.004, smooth((a - 70) / 40F), GOLD, sa, s);
        double[] radii = {3, 5.5, 8, 11, 14};
        for (int i = 0; i < radii.length; i++) {
            float k = smooth((a - 90 - i * 14) / 18F);
            if (k <= 0) continue;
            double rot = a * 0.01 * (i % 2 == 0 ? 1 : -1), R = radii[i] * s;
            Vec3 rc = new Vec3(c.x, top - i * 0.6 * s, c.z);
            arc(rc, R, 0, rot, k, i % 2 == 0 ? WHITE : AMBER, sa, s);
            for (int sp = 0; sp < 12; sp++) {
                if (sp / 12F > k) break;
                double an = rot + sp * Math.PI / 6;
                Vec3 p0 = rc.add(Math.cos(an) * (R - 0.5 * s), 0, Math.sin(an) * (R - 0.5 * s));
                Vec3 p1 = rc.add(Math.cos(an) * (R + 0.5 * s), 0, Math.sin(an) * (R + 0.5 * s));
                FxDraw.ribbon(p0, p1, 0.1F * s, GOLD, 0.8F * sa, GOLD, 0.8F * sa);
            }
        }
        // whole: a soft flash, then a steady glow while it holds
        float flash = Math.max(0, 1 - Math.abs(a - 168) / 12F);
        if (flash > 0) FxDraw.glow(new Vec3(c.x, top, c.z), (2 + Math.max(0, a - 156) * 1.2F) * s, WHITE, flash * power);
        if (a > 168) FxDraw.glow(new Vec3(c.x, top, c.z), 6 * s, GOLD, 0.35F * fade);
    }

    private static void arc(Vec3 c, double r, double tilt, double rot, float k, int rgb, float alpha, float s) {
        if (k <= 0) return;
        int seg = 64, n = (int) Math.ceil(seg * k);
        Vec3 prev = null;
        for (int j = 0; j <= n; j++) {
            double an = rot + Math.PI * 2 * Math.min(k, j / (double) seg);
            Vec3 p = c.add(Math.cos(an) * r, Math.sin(an) * r * tilt, Math.sin(an) * r);
            if (prev != null) {
                FxDraw.ribbon(prev, p, 0.6F * s, rgb, 0.15F * alpha, rgb, 0.15F * alpha);
                FxDraw.ribbon(prev, p, 0.12F * s, rgb, 0.9F * alpha, rgb, 0.9F * alpha);
            }
            prev = p;
        }
        if (k < 1 && prev != null) FxDraw.glow(prev, 0.7F * s, WHITE, alpha);
    }

    private static void crown(Vec3 c, float sz, float rot, float alpha, float s) {
        if (alpha <= 0) return;
        Vec3[] v = new Vec3[6];
        for (int i = 0; i < 4; i++) v[i] = c.add(Math.cos(rot + i * Math.PI / 2) * sz, 0, Math.sin(rot + i * Math.PI / 2) * sz);
        v[4] = c.add(0, sz * 1.6, 0);
        v[5] = c.add(0, -sz * 1.6, 0);
        for (int i = 0; i < 4; i++) {
            FxDraw.ribbon(v[i], v[(i + 1) % 4], 0.12F * s, WHITE, alpha, WHITE, alpha);
            FxDraw.ribbon(v[i], v[4], 0.12F * s, GOLD, alpha, WHITE, alpha);
            FxDraw.ribbon(v[i], v[5], 0.12F * s, GOLD, alpha, WHITE, alpha);
        }
        FxDraw.glow(c, 2.5F * s, GOLD, 0.4F * alpha);
    }

    static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    static double frac(double x) {
        return x - Math.floor(x);
    }

    static float rnd(int i, int k) {
        long h = (i * 0x9E3779B97F4A7C15L) ^ (k * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1 << 24);
    }

    private TranscendentalEcho() {}
}
