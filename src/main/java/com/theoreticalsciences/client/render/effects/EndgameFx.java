package com.theoreticalsciences.client.render.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 1.2.0: the working animations of the late and endgame machines, one each, used by no other machine (the machine's
 * structure is built round it, see tools/round96_endgame.py). Entries in a multiblock's {@code "fx"} like the others in
 * {@link MachineFx}; positions are pattern coordinates. While one of these runs, the machine's older shared effect is
 * not drawn: this is its animation now.
 */
public final class EndgameFx {
    public static final Set<String> TYPES = Set.of("annihilation", "causality", "dyson", "matrioshka", "vacuum", "breach", "concept", "eye",
            "plasma_forge", "big_bang", "inflation", "mirror", "entropy", "distillery", "abiogenesis", "ward", "resolution", "compiler");

    /** True when the definition's effects include one of these (its shared effect is then left out). */
    public static boolean owns(MultiblockDefinition d) {
        if (d == null) return false;
        for (JsonElement e : d.fx) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("type") && TYPES.contains(o.get("type").getAsString())) return true;
        }
        return false;
    }

    /** What the machine draws around: pattern coordinates to world and machine directions to world. */
    record Ctx(MultiblockDefinition d, Vec3 ctr, Direction front, Vec3 ec, float t, float power, long seed, Vec3 cam) {
        Vec3 at(double x, double y, double z) { return MachineFx.world(d, ctr, front, x, y, z); }
        Vec3 at(Vec3 p) { return at(p.x, p.y, p.z); }
        Vec3 dir(double x, double y, double z) { return FxFrame.toWorld(Vec3.ZERO, front, x, y, z); }
        Vec3 up() { return dir(0, 1, 0); }
    }

    // ------------------------------------------------------------------------------------------------ helpers
    static float h(long n) {
        n = (n ^ 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 31)) * 0x94D049BB133111EBL;
        return ((n >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    static float smooth(float x) { return MachineFx.smooth(x); }

    static float bell(float x) { return x <= 0 || x >= 1 ? 0 : (float) Math.sin(x * Math.PI); }

    static Vec3 lerp(Vec3 a, Vec3 b, double t) { return a.add(b.subtract(a).scale(t)); }

    static List<Vec3> path(JsonObject o, String k) {
        List<Vec3> out = new ArrayList<>();
        if (o.has(k)) for (JsonElement p : o.getAsJsonArray(k)) {
            JsonArray a = p.getAsJsonArray();
            out.add(new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()));
        }
        return out;
    }

    static Vec3 point(JsonObject o, String k, Vec3 d) {
        if (!o.has(k)) return d;
        JsonArray a = o.getAsJsonArray(k);
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    /** A point along a polyline, u from 0 to 1. */
    static Vec3 along(List<Vec3> p, double u) {
        if (p.isEmpty()) return Vec3.ZERO;
        if (p.size() == 1) return p.get(0);
        double s = Math.max(0, Math.min(0.99999, u)) * (p.size() - 1);
        int i = (int) s;
        return lerp(p.get(i), p.get(i + 1), s - i);
    }

    /** A random direction (unit), the same for the same n. */
    static Vec3 rdir(long n) {
        double z = h(n) * 2 - 1, a = h(n * 31 + 7) * Math.PI * 2, r = Math.sqrt(1 - z * z);
        return new Vec3(Math.cos(a) * r, z, Math.sin(a) * r);
    }

    /** A jagged bolt from a to b, re-struck each time the seed changes. */
    static void bolt(Vec3 a, Vec3 b, long seed, float width, int rgb, float alpha) {
        Vec3 prev = a, d = b.subtract(a);
        double len = d.length();
        int n = Math.max(4, (int) (len / 1.5));
        for (int i = 1; i <= n; i++) {
            double u = i / (double) n;
            Vec3 p = lerp(a, b, u);
            if (i < n) p = p.add(rdir(seed * 131 + i).scale(len * 0.06 * Math.sin(u * Math.PI)));
            FxDraw.ribbon(prev, p, width, 0xFFFFFF, alpha, 0xFFFFFF, alpha);
            FxDraw.ribbon(prev, p, width * 4, rgb, alpha * 0.25F, rgb, alpha * 0.25F);
            prev = p;
        }
    }

    // ------------------------------------------------------------------------------------------------ the solid parts
    public static void drawOpaque(String type, JsonObject o, FxFrame f, Matrix4f mv, MultiblockDefinition d, Vector3f centre, float power, long seed) {
        float t = f.time;
        switch (type) {
            case "dyson", "matrioshka" -> StarEffect.drawSurface(f, mv, centre, num(o, "star", 3.2F), 0xFFD080, power);
            case "plasma_forge" -> StarEffect.drawSurface(f, mv, centre, 4.5F + 1.5F * smooth(power), 0xFFB060, power);
            case "eye" -> darkSphere(f, mv, centre, num(o, "radius", 20) * 0.42F * smooth(power));
            case "big_bang" -> {
                float q = cycle(t, 24, seed);
                if (q > 0.45F) SpaceShellEffect.draw(f, mv, centre, num(o, "radius", 16), 0x6050C0, power * smooth((q - 0.45F) / 0.15F) * (1 - smooth((q - 0.9F) / 0.1F)));
            }
            case "ward" -> {
                // the anomaly: a dark knot that will not hold its shape
                float r = num(o, "radius", 8) * 0.35F;
                darkSphere(f, mv, new Vector3f(centre).add((float) Math.sin(t * 1.7) * 0.6F, (float) Math.sin(t * 2.3) * 0.5F, (float) Math.cos(t * 1.3) * 0.6F),
                        r * (0.8F + 0.25F * (float) Math.sin(t * 3.1)) * smooth(power));
            }
            default -> {}
        }
    }

    static float num(JsonObject o, String k, float d) { return MachineFx.num(o, k, d); }

    /** Where in its cycle a machine is (0..1), each machine its own phase. */
    static float cycle(float t, float period, long seed) {
        return (float) (((t / period) + (seed & 1023) / 1023.0) % 1.0);
    }

    /** A plain black sphere (seen from outside). */
    static void darkSphere(FxFrame f, Matrix4f mv, Vector3f c, float r) {
        if (r <= 0.02F) return;
        ShaderInstance sh = GameRenderer.getPositionColorShader();
        if (sh == null) return;
        Matrix4f m = new Matrix4f(mv).translate(c).scale(r);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(0, 0, 0, 1);
        VertexBuffer vb = FxMesh.sphere();
        vb.bind();
        vb.drawWithShader(m, f.proj, sh);
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ------------------------------------------------------------------------------------------------ the light
    public static void drawLight(String type, JsonObject o, List<Vec3> pts, Ctx c) {
        switch (type) {
            case "annihilation" -> annihilation(o, pts, c);
            case "causality" -> causality(o, c);
            case "dyson" -> dyson(o, pts, c);
            case "matrioshka" -> matrioshka(o, c);
            case "vacuum" -> vacuum(o, c);
            case "breach" -> breach(o, c);
            case "concept" -> concept(o, pts, c);
            case "eye" -> eye(o, c);
            case "plasma_forge" -> plasmaForge(o, c);
            case "big_bang" -> bigBang(o, c);
            case "inflation" -> inflation(o, c);
            case "mirror" -> mirror(o, c);
            case "entropy" -> entropy(o, c);
            case "distillery" -> distillery(o, c);
            case "abiogenesis" -> abiogenesis(o, c);
            case "ward" -> ward(o, pts, c);
            case "resolution" -> resolution(o, c);
            case "compiler" -> compiler(o, c);
            default -> {}
        }
    }

    /** Matter from one tower, antimatter from the other, meeting in the bottle in flashes of gamma light. */
    static void annihilation(JsonObject o, List<Vec3> pts, Ctx c) {
        if (pts.size() < 2) return;
        float t = c.t, p = c.power;
        int[] cols = {0x60A0FF, 0xFF5070};
        for (int s = 0; s < 2; s++) {
            Vec3 a = c.at(pts.get(s));
            FxDraw.beam(a, c.ec, 0.25F, cols[s], 0.5F * p);
            for (int k = 0; k < 14; k++) {
                float q = (t * 0.9F + k / 14F) % 1F;
                Vec3 at = lerp(a, c.ec, q).add(rdir(k * 7 + s).scale(0.25 * Math.sin(t * 6 + k)));
                FxDraw.glow(at, 0.35F, cols[s], 0.9F * p);
            }
            FxDraw.glow(a, 1.6F, cols[s], 0.7F * p);
        }
        // the bottle's field lines, faint
        float R = num(o, "radius", 10);
        for (int j = 0; j < 3; j++) FxDraw.circle(c.ec, c.dir(Math.cos(j * 1.05), 0, Math.sin(j * 1.05)), R * 0.92, 0.08F, 0x9080FF, 0.25F * p, t * 0.4);
        // annihilation: flashes, each a burst of gamma rays
        long slot = (long) (t * 6);
        float q = (t * 6) % 1F;
        for (int k = 0; k < 2; k++) {
            long n = slot * 3 + k;
            Vec3 at = c.ec.add(rdir(n).scale(1.5 * h(n + 5)));
            float a = p * (1 - q);
            FxDraw.glow(at, 2.5F * (0.5F + q), 0xFFFFFF, 0.9F * a);
            for (int r = 0; r < 6; r++) {
                Vec3 dd = rdir(n * 17 + r);
                FxDraw.ribbon(at, at.add(dd.scale(R * (0.4 + 0.6 * q))), 0.06F, 0xFFFFFF, 0.8F * a, 0xB0FFB0, 0);
            }
        }
        FxDraw.glow(c.ec, 3.2F, 0xFFF0FF, 0.45F * p);
    }

    /** A pulse running the figure-eight, its echoes (the same pulse, earlier) following round behind it. */
    static void causality(JsonObject o, Ctx c) {
        List<Vec3> path = path(o, "path");
        if (path.size() < 3) return;
        float t = c.t, p = c.power;
        for (int i = 0; i + 1 < path.size(); i += 2) FxDraw.ribbon(c.at(path.get(i)), c.at(path.get(Math.min(path.size() - 1, i + 2))), 0.3F, 0x40C0A0, 0.18F * p, 0x40C0A0, 0.18F * p);
        float lap = 6F;
        for (int e = 0; e < 7; e++) {
            double u = ((t - e * 0.55) / lap) % 1.0;
            if (u < 0) u += 1;
            float a = p * (float) Math.pow(0.65, e);
            Vec3 at = c.at(along(path, u)), back = c.at(along(path, (u + 0.985) % 1.0));
            int rgb = e == 0 ? 0xC0FFF0 : FxDraw.lerpColor(0x60FFD0, 0x2060FF, e / 6F);
            FxDraw.ribbon(back, at, 0.5F, rgb, 0, rgb, 0.9F * a);
            FxDraw.glow(at, e == 0 ? 1.6F : 1.0F, rgb, 0.9F * a);
        }
        // where the track crosses itself, the pulse meets its own past: a flash each half lap
        float q = ((t / lap) * 2) % 1F;
        float f = (1 - smooth(Math.abs(q - 0.5F) * 6));
        FxDraw.glow(c.ec, 2 + 4 * f, 0xE0FFFF, (0.2F + 0.7F * f) * p);
        if (f > 0.3F) FxDraw.circle(c.ec, c.up(), 3 + 5 * (1 - f), 0.15F, 0x80FFE0, f * p, 0);
    }

    /** A swarm of collectors round the star, each mast's dish catching a beam in turn. */
    static void dyson(JsonObject o, List<Vec3> pts, Ctx c) {
        float t = c.t, p = c.power;
        StarCorona(c, num(o, "star", 3.2F));
        for (int i = 0; i < 220; i++) {
            float r = 9.5F + 26 * h(i), sp = 0.6F / (float) Math.sqrt(r) * 3;
            double a = t * sp + h(i + 999) * Math.PI * 2, inc = (h(i + 77) - 0.5) * 1.2, node = h(i + 55) * Math.PI * 2;
            double x = Math.cos(a) * r, y = Math.sin(a) * r * Math.sin(inc), z = Math.sin(a) * r * Math.cos(inc);
            double xx = x * Math.cos(node) - z * Math.sin(node), zz = x * Math.sin(node) + z * Math.cos(node);
            Vec3 at = c.ec.add(c.dir(xx, y, zz));
            FxDraw.glow(at, 0.22F, 0xFFE8B0, 0.75F * p);
        }
        for (int i = 0; i < pts.size(); i++) {
            float q = (t * 0.25F + i / (float) pts.size()) % 1F;
            float a = p * bell(q / 0.35F);
            if (a <= 0) continue;
            Vec3 m = c.at(pts.get(i));
            FxDraw.beam(c.ec, m, 0.35F, 0xFFC060, a);
            FxDraw.glow(m, 2.2F, 0xFFE0A0, a);
        }
    }

    static void StarCorona(Ctx c, float r) {
        FxDraw.glow(c.ec, r * 3.2F, 0xFFB050, 0.35F * c.power);
        FxDraw.glow(c.ec, r * 1.6F, 0xFFF0C0, 0.6F * c.power);
    }

    /** Thoughts: sparks on each shell, and signals passing between the shells, outward and back. */
    static void matrioshka(JsonObject o, Ctx c) {
        float t = c.t, p = c.power;
        StarCorona(c, num(o, "star", 3.2F));
        float[] radii = {14, 25, 36};
        if (o.has("radii")) {
            JsonArray a = o.getAsJsonArray("radii");
            for (int i = 0; i < Math.min(3, a.size()); i++) radii[i] = a.get(i).getAsFloat();
        }
        int[] cols = {0xFFD080, 0x80E0FF, 0xC090FF};
        for (int s = 0; s < 3; s++) {
            for (int k = 0; k < 40; k++) {
                long n = (long) (t * 2 + h(k * 13 + s) * 8) * 1000 + k * 3 + s;
                float q = (t * 2 + h(k * 13 + s) * 8) % 1F;
                Vec3 at = c.ec.add(c.dir(rdir(n).x, rdir(n).y, rdir(n).z).scale(radii[s] - 0.6));
                FxDraw.glow(at, 0.5F, cols[s], 0.9F * p * bell(q));
            }
        }
        // signals: from the star out through the shells, then back in
        for (int k = 0; k < 10; k++) {
            float q = (t * 0.35F + k / 10F) % 1F;
            Vec3 d = rdir(k * 101 + (long) (t * 0.35F + k / 10F));
            Vec3 dir = c.dir(d.x, d.y, d.z);
            double r = q < 0.5 ? radii[2] * q * 2 : radii[2] * (2 - q * 2);
            int rgb = cols[Math.min(2, (int) (r / radii[2] * 3))];
            Vec3 at = c.ec.add(dir.scale(Math.max(3.5, r)));
            FxDraw.ribbon(at, at.subtract(dir.scale(q < 0.5 ? 2.5 : -2.5)), 0.2F, rgb, 0.9F * p, rgb, 0);
            FxDraw.glow(at, 0.6F, 0xFFFFFF, 0.8F * p);
        }
    }

    /** Pairs of particles flickering out of nothing in the funnel; most meet again and vanish, some are drawn down. */
    static void vacuum(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 20);
        Vec3 throat = c.at(point(o, "throat", new Vec3(c.d.ex, c.d.ey - 10, c.d.ez)));
        for (int k = 0; k < 70; k++) {
            float per = 1.6F + h(k) * 1.2F;
            float q = ((t + h(k + 3) * 10) / per) % 1F;
            long n = (long) ((t + h(k + 3) * 10) / per) * 97 + k;
            double ang = h(n) * Math.PI * 2, rr = R * Math.sqrt(h(n + 1)), yy = (h(n + 2) - 0.3) * R * 0.9;
            Vec3 at = c.ec.add(c.dir(Math.cos(ang) * rr, yy, Math.sin(ang) * rr));
            Vec3 sep = c.dir(rdir(n + 9).x, rdir(n + 9).y, rdir(n + 9).z).scale(0.9 * bell(q));
            boolean caught = h(n + 4) < 0.25F;
            if (!caught || q < 0.4F) {
                float a = p * bell(Math.min(1, q / (caught ? 0.4F : 1F)));
                FxDraw.glow(at.add(sep), 0.3F, 0x80D0FF, a);
                FxDraw.glow(at.subtract(sep), 0.3F, 0xFF90E0, a);
                if (!caught && q > 0.9F) FxDraw.glow(at, 0.8F, 0xFFFFFF, p * (1 - q) * 8);
            } else {
                float k2 = (q - 0.4F) / 0.6F;
                Vec3 to = lerp(at.add(sep), throat, k2 * k2);
                double sw = k2 * 9 + k;
                to = to.add(c.dir(Math.cos(sw), 0, Math.sin(sw)).scale(2 * (1 - k2)));
                FxDraw.glow(to, 0.35F, 0x80D0FF, p * (1 - k2 * 0.5F));
                FxDraw.glow(at.subtract(sep), 0.3F, 0xFF90E0, p * (1 - smooth(k2 * 3)));
            }
        }
        FxDraw.glow(throat, 2.5F + 0.5F * (float) Math.sin(t * 5), 0xA0E0FF, 0.7F * p);
    }

    /** A crack in the world down the middle of the arch, light pouring from it and glyph shards breaking off. */
    static void breach(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, H = num(o, "height", 60) * 0.5F;
        long seed = c.seed;
        Vec3 prev = null;
        int n = 24;
        float open = smooth(p);
        for (int i = 0; i <= n; i++) {
            double u = i / (double) n, y = (u - 0.5) * 2 * H * open;
            double x = (h(seed + i) - 0.5) * 3.5 * Math.sin(u * Math.PI) + Math.sin(t * 2 + i) * 0.15;
            Vec3 q = c.ec.add(c.dir(x, y, 0));
            if (prev != null) {
                float w = (float) (0.4 + 1.6 * Math.sin(u * Math.PI)) * open;
                FxDraw.ribbon(prev, q, w * 3, 0xB060FF, 0.3F * p, 0xB060FF, 0.3F * p);
                FxDraw.ribbon(prev, q, w, 0xFFFFFF, 0.95F * p, 0xFFFFFF, 0.95F * p);
            }
            prev = q;
        }
        // light spilling sideways through the gate
        for (int k = 0; k < 10; k++) {
            double y = (h(k + 40) - 0.5) * 2 * H * 0.8 * open;
            float a = 0.25F * p * (0.6F + 0.4F * (float) Math.sin(t * 3 + k));
            for (int s = -1; s <= 1; s += 2) {
                Vec3 a0 = c.ec.add(c.dir(0, y, 0)), a1 = a0.add(c.dir(0, 0, s * 10));
                FxDraw.ribbon(a0, a1, 1.2F, 0xD0A0FF, a, 0xD0A0FF, 0);
            }
        }
        // shards: little flat glyphs, flaking off the crack and turning away into the air
        for (int k = 0; k < 30; k++) {
            float per = 4 + h(k) * 3, q = ((t + h(k + 1) * 20) / per) % 1F;
            long m = (long) ((t + h(k + 1) * 20) / per) * 53 + k;
            double y = (h(m) - 0.5) * 2 * H * 0.9 * open;
            Vec3 from = c.ec.add(c.dir(0, y, 0)), d = c.dir(h(m + 1) - 0.5, (h(m + 2) - 0.3) * 0.6, (h(m + 3) < 0.5 ? -1 : 1) * (0.6 + h(m + 4)));
            Vec3 at = from.add(d.scale(q * 14));
            double spin = t * 3 + k;
            Vec3 s1 = c.dir(Math.cos(spin), Math.sin(spin * 0.7), Math.sin(spin)).scale(0.45);
            float a = p * (1 - q);
            FxDraw.ribbon(at.subtract(s1), at.add(s1), 0.45F, 0xE0C0FF, 0.9F * a, 0x8040FF, 0.6F * a);
        }
    }

    /** Streams of nine colours spiralling in from the arms, crushed together in the middle into one bright point. */
    static void concept(JsonObject o, List<Vec3> pts, Ctx c) {
        float t = c.t, p = c.power;
        for (int i = 0; i < pts.size(); i++) {
            int rgb = hue(i / (float) pts.size());
            Vec3 a = c.at(pts.get(i));
            for (int k = 0; k < 16; k++) {
                float q = (t * 0.3F + k / 16F + i * 0.07F) % 1F;
                Vec3 on = lerp(a, c.ec, q * q * (3 - 2 * q));
                double sw = q * 12 + i;
                Vec3 off = c.dir(Math.cos(sw), Math.sin(sw * 1.3), Math.sin(sw)).scale(1.4 * (1 - q));
                FxDraw.glow(on.add(off), 0.45F * (1 - q * 0.6F), rgb, 0.9F * p);
            }
            FxDraw.glow(a, 2.4F, rgb, 0.5F * p);
        }
        float beat = (t * 0.5F) % 1F;
        FxDraw.glow(c.ec, 1.8F + 1.5F * smooth(1 - beat * 4), 0xFFFFFF, 0.9F * p);
        if (beat < 0.25F) FxDraw.circle(c.ec, c.ec.subtract(c.cam), 1.5 + beat * 30, 0.2F, 0xFFFFFF, p * (1 - beat * 4), 0);
    }

    /** The black hole: a hot disc turning round it, matter falling into the disc, and its two jets. */
    static void eye(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 20), J = num(o, "jet", 50);
        float hole = R * 0.42F * smooth(p);
        Vec3 up = c.up();
        for (int i = 0; i < 360; i++) {
            float u = h(i), r = hole * 1.3F + u * u * R;
            double a = t * 3.0 / Math.pow(r / hole, 1.5) + h(i + 400) * Math.PI * 2;
            double y = (h(i + 800) - 0.5) * 0.6;
            Vec3 at = c.ec.add(c.dir(Math.cos(a) * r, y, Math.sin(a) * r));
            int rgb = FxDraw.lerpColor(0xFFF4E0, 0xD04010, u);
            // the side turning toward you is brighter
            Vec3 vel = c.dir(-Math.sin(a), 0, Math.cos(a));
            float beam = (float) (0.65 + 0.35 * vel.dot(c.cam.subtract(at).normalize()));
            FxDraw.glow(at, 0.5F + 0.4F * (1 - u), rgb, 0.8F * p * beam);
        }
        FxDraw.circle(c.ec, up, hole * 1.12, 0.35F, 0xFFE8C0, 0.9F * p, 0);
        // infall: streams spiralling in from the cage
        for (int s = 0; s < 6; s++) {
            for (int k = 0; k < 10; k++) {
                float q = (t * 0.18F + k / 10F + s * 0.13F) % 1F;
                double a = s * Math.PI / 3 + q * 4, r = R * 2.1 * (1 - q) + hole * 1.3 * q;
                Vec3 at = c.ec.add(c.dir(Math.cos(a) * r, (1 - q) * (s % 2 == 0 ? 8 : -8), Math.sin(a) * r));
                FxDraw.glow(at, 0.5F, 0xFFA060, 0.7F * p * smooth(q * 5));
            }
        }
        // the jets, with knots running out along them
        for (int s = -1; s <= 1; s += 2) {
            Vec3 a = c.ec.add(up.scale(s * hole)), b = c.ec.add(up.scale(s * J));
            FxDraw.beam(a, b, 1.1F, 0x80A0FF, 0.6F * p);
            for (int k = 0; k < 6; k++) {
                float q = (t * 0.5F + k / 6F) % 1F;
                FxDraw.glow(lerp(a, b, q), 1.4F * (1 - q * 0.5F), 0xC0D8FF, 0.9F * p * (1 - q));
            }
        }
    }

    /** Plasma pouring down each conduit into the chamber, the star in it brightening as it is fed. */
    static void plasmaForge(JsonObject o, Ctx c) {
        float t = c.t, p = c.power;
        if (o.has("paths")) {
            JsonArray all = o.getAsJsonArray("paths");
            for (int i = 0; i < all.size(); i++) {
                List<Vec3> path = new ArrayList<>();
                for (JsonElement e : all.get(i).getAsJsonArray()) {
                    JsonArray a = e.getAsJsonArray();
                    path.add(c.at(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()));
                }
                for (int k = 0; k < 22; k++) {
                    float q = (t * 0.22F + k / 22F + i * 0.25F) % 1F;
                    Vec3 at = along(path, q);
                    float pul = 0.8F + 0.2F * (float) Math.sin(t * 8 + k);
                    FxDraw.glow(at, 1.1F * pul, 0xFF8040, 0.75F * p);
                    FxDraw.glow(at, 0.45F, 0xFFFFE0, 0.9F * p);
                }
                if (!path.isEmpty()) FxDraw.glow(path.get(path.size() - 1), 2.5F, 0xFFC080, 0.7F * p);
            }
        }
        FxDraw.glow(c.ec, 10, 0xFF9040, 0.35F * p);
        for (int k = 0; k < 8; k++) {
            float q = (t * 0.8F + k / 8F) % 1F;
            Vec3 d = rdir((long) (t * 0.8F + k / 8F) * 11 + k);
            Vec3 a0 = c.ec.add(c.dir(d.x, d.y, d.z).scale(6)), a1 = c.ec.add(c.dir(d.x, d.y, d.z).scale(6 + 4 * q));
            FxDraw.ribbon(a0, a1, 0.25F, 0xFFE0A0, p * (1 - q), 0xFF6020, 0);
        }
    }

    /** Over and over: a point, a flash, a fireball cooling as it spreads, and out of it a few galaxies. */
    static void bigBang(JsonObject o, Ctx c) {
        float R = num(o, "radius", 16), q = cycle(c.t, 24, c.seed), p = c.power;
        if (q < 0.08F) FxDraw.glow(c.ec, 0.3F + q * 6, 0xFFFFFF, p * (0.5F + q * 6));
        else if (q < 0.5F) {
            float k = (q - 0.08F) / 0.42F, r = R * (float) Math.sqrt(k);
            int rgb = FxDraw.lerpColor(0xFFFFFF, 0xFF4010, k);
            FxDraw.glow(c.ec, r * 1.2F, rgb, p * (1 - k) * 0.8F);
            for (int i = 0; i < 260; i++) {
                Vec3 d = rdir(i + (c.seed & 255) * 1000);
                FxDraw.glow(c.ec.add(c.dir(d.x, d.y, d.z).scale(r * (0.3 + 0.7 * h(i + 3)))), 0.25F, FxDraw.lerpColor(rgb, 0xFFD0A0, h(i)), p * (1 - k * 0.5F));
            }
            if (k < 0.15F) FxDraw.glow(c.ec, R * 1.6F, 0xFFFFFF, p * (1 - k / 0.15F));
        } else {
            float k = (q - 0.5F) / 0.5F;
            MachineFx.galaxies(c.ec, R * 0.9F, c.t, c.seed + (long) ((c.t / 24)), p * smooth(k * 4) * (1 - smooth((k - 0.85F) / 0.15F)), 0x8070FF);
        }
    }

    /** A lattice of points, space itself, swelling outward faster and faster, then beginning again. */
    static void inflation(JsonObject o, Ctx c) {
        float R = num(o, "radius", 15), q = cycle(c.t, 10, c.seed), p = c.power;
        double s = 0.15 * Math.exp(q * 3.6);
        int n = 3;
        for (int x = -n; x <= n; x++)
            for (int y = -n; y <= n; y++)
                for (int z = -n; z <= n; z++) {
                    Vec3 at = c.ec.add(c.dir(x, y, z).scale(s * R / n));
                    float dist = (float) (Math.sqrt(x * x + y * y + z * z) / n * s);
                    float a = p * (1 - smooth((dist - 0.7F) / 0.6F)) * smooth(q * 10);
                    if (a <= 0.01F) continue;
                    FxDraw.glow(at, 0.35F, 0xA0E0FF, a);
                    if (x < n) FxDraw.ribbon(at, c.ec.add(c.dir(x + 1, y, z).scale(s * R / n)), 0.05F, 0x6090FF, a * 0.5F, 0x6090FF, a * 0.5F);
                    if (y < n) FxDraw.ribbon(at, c.ec.add(c.dir(x, y + 1, z).scale(s * R / n)), 0.05F, 0x6090FF, a * 0.5F, 0x6090FF, a * 0.5F);
                    if (z < n) FxDraw.ribbon(at, c.ec.add(c.dir(x, y, z + 1).scale(s * R / n)), 0.05F, 0x6090FF, a * 0.5F, 0x6090FF, a * 0.5F);
                }
        FxDraw.glow(c.ec, 2F, 0xFFFFFF, p * (1 - q));
    }

    /** A left-handed helix on one side of the glass and its mirror image, right-handed, on the other; the glass shimmers. */
    static void mirror(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 14);
        for (int side = -1; side <= 1; side += 2) {
            for (int strand = 0; strand < 2; strand++) {
                Vec3 prev = null;
                for (int i = 0; i <= 60; i++) {
                    double u = i / 60.0, a = side * (u * Math.PI * 6 + t * 1.2) + strand * Math.PI;
                    Vec3 q = c.ec.add(c.dir(side * (8 + u * 26), Math.cos(a) * 3.5, Math.sin(a) * 3.5 * side));
                    int rgb = side < 0 ? 0x70C0FF : 0xFF80C0;
                    if (prev != null) FxDraw.ribbon(prev, q, 0.25F, rgb, 0.8F * p, rgb, 0.8F * p);
                    prev = q;
                }
            }
        }
        // the mirror: a sheet of light in the glass, with ripples crossing it
        for (int k = 0; k < 5; k++) {
            float q = (t * 0.3F + k / 5F) % 1F;
            FxDraw.circle(c.ec, c.dir(1, 0, 0), q * R * 1.4, 0.3F, 0xFFFFFF, 0.6F * p * (1 - q), 0);
        }
        FxDraw.glow(c.ec, R * 0.6F, 0xE0E8FF, 0.25F * p);
    }

    /** Stars across the disc, going out from the rim inward, reddening and drifting down the well, until all is even. */
    static void entropy(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 40), q = cycle(t, 40, c.seed);
        for (int i = 0; i < 240; i++) {
            float u = (float) Math.sqrt(h(i)), r = 12 + u * (R - 12);
            double a = h(i + 500) * Math.PI * 2 + t * 0.02;
            float dies = 1 - u;                   // the outer ones go first
            float k = smooth((q - dies * 0.8F) / 0.2F);
            double rr = r * (1 - 0.6 * k);
            Vec3 at = c.ec.add(c.dir(Math.cos(a) * rr, 6 + h(i + 900) * 6 - k * 8, Math.sin(a) * rr));
            int rgb = FxDraw.lerpColor(0xE0E8FF, 0x601008, k);
            FxDraw.glow(at, 0.5F * (1 - 0.6F * k), rgb, p * (1 - k * 0.85F));
        }
        // the well: a dull even glow that grows as the stars go out
        FxDraw.glow(c.ec, 10, 0x502010, p * (0.15F + 0.4F * q));
        FxDraw.circle(c.ec.add(c.up().scale(9)), c.up(), 12, 0.3F, 0x804020, 0.5F * p * q, 0);
    }

    /** The boiler glows, vapour climbs the neck, beads along the arm, and falls drop by drop into the receiver. */
    static void distillery(JsonObject o, Ctx c) {
        float t = c.t, p = c.power;
        Vec3 col = c.at(point(o, "column", new Vec3(c.d.ex, c.d.ey + 14, c.d.ez))), top = c.at(point(o, "top", new Vec3(c.d.ex, c.d.ey + 40, c.d.ez)));
        Vec3 rec = c.at(point(o, "receiver", new Vec3(c.d.ex + 40, c.d.ey, c.d.ez)));
        List<Vec3> arm = new ArrayList<>();
        for (Vec3 v : path(o, "path")) arm.add(c.at(v));
        FxDraw.glow(c.ec, 11 + (float) Math.sin(t * 1.5), 0xC060FF, 0.3F * p);
        for (int k = 0; k < 30; k++) {
            float q = (t * 0.6F + h(k) * 4) % 1F;
            Vec3 d = rdir(k + (long) (t * 0.6F + h(k) * 4) * 31);
            FxDraw.glow(c.ec.add(c.dir(d.x, d.y, d.z).scale(9 * h(k + 7))).add(c.up().scale(q * 3)), 0.45F, 0xE0A0FF, p * bell(q));
        }
        for (int k = 0; k < 16; k++) {
            float q = (t * 0.25F + k / 16F) % 1F;
            Vec3 at = lerp(col, top, q).add(c.dir(Math.sin(t * 3 + k), 0, Math.cos(t * 3 + k)).scale(1.2));
            FxDraw.glow(at, 0.7F, 0xD0B0FF, 0.6F * p * (1 - q * 0.4F));
        }
        for (int k = 0; k < 12; k++) {
            float q = (t * 0.15F + k / 12F) % 1F;
            if (!arm.isEmpty()) FxDraw.glow(along(arm, q), 0.35F + 0.25F * q, FxDraw.lerpColor(0xD0B0FF, 0xFFE070, q), 0.9F * p);
        }
        float q = (t / 1.4F) % 1F;
        if (!arm.isEmpty()) {
            Vec3 lip = arm.get(arm.size() - 1);
            Vec3 at = lerp(lip, rec, q * q);
            FxDraw.glow(at, 0.4F, 0xFFE070, p);
        }
        FxDraw.glow(rec, 5, 0xFFD050, 0.4F * p * (0.8F + 0.2F * (float) Math.sin(t)));
    }

    /** Lightning from the crown into the sea; where it strikes, little cells form, drift and divide. */
    static void abiogenesis(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 30), sea = num(o, "sea", 2);
        Vec3 crown = c.at(point(o, "crown", new Vec3(c.d.ex, c.d.ey + 20, c.d.ez)));
        Vec3 seaC = c.at(c.d.ex, sea, c.d.ez);
        long slot = (long) (t * 2.5);
        float q = (t * 2.5F) % 1F;
        for (int k = 0; k < 2; k++) {
            long n = slot * 7 + k;
            double a = h(n) * Math.PI * 2, r = R * Math.sqrt(h(n + 1));
            Vec3 hit = seaC.add(c.dir(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (q < 0.35F) bolt(crown, hit, n, 0.25F, 0xA0C0FF, p * (1 - q / 0.35F));
            FxDraw.glow(hit, 3 * (1 - q), 0xC0E0FF, p * (1 - q));
        }
        FxDraw.glow(crown, 3 + (float) Math.sin(t * 9), 0xC0D0FF, 0.6F * p);
        for (int i = 0; i < 40; i++) {
            float per = 14 + h(i) * 8, u = ((t + h(i + 1) * 40) / per) % 1F;
            long n = (long) ((t + h(i + 1) * 40) / per) * 41 + i;
            double a = h(n) * Math.PI * 2 + u * 1.5, r = R * Math.sqrt(h(n + 1)) * 0.95;
            Vec3 at = seaC.add(c.dir(Math.cos(a) * r, 0.3, Math.sin(a) * r));
            float size = 0.25F + 0.35F * smooth(u * 3);
            float a2 = p * bell(u);
            if (u > 0.6F) {                     // it divides
                Vec3 sep = c.dir(Math.cos(a + 1.5), 0, Math.sin(a + 1.5)).scale((u - 0.6) * 2);
                FxDraw.circle(at.add(sep), c.up(), size * 0.8, 0.07F, 0x80FF90, 0.9F * a2, 0);
                FxDraw.circle(at.subtract(sep), c.up(), size * 0.8, 0.07F, 0x80FF90, 0.9F * a2, 0);
            } else FxDraw.circle(at, c.up(), size, 0.07F, 0x80FF90, 0.9F * a2, 0);
            FxDraw.glow(at, size * 0.6F, 0x40C060, 0.6F * a2);
        }
    }

    /** The anomaly writhing at the middle, held in a flickering shield the six pylons pour themselves into. */
    static void ward(JsonObject o, List<Vec3> pts, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 8);
        for (int i = 0; i < 12; i++) {
            Vec3 d = c.dir(Math.sin(t * 0.9 + i), Math.cos(t * 1.3 + i * 2), Math.sin(t * 0.7 + i * 3)).scale(R * 0.4);
            FxDraw.glow(c.ec.add(d), 1.2F, 0xD040FF, 0.5F * p);
            FxDraw.ribbon(c.ec, c.ec.add(d.scale(1.6)), 0.25F, 0xFF60C0, 0.5F * p, 0x6000FF, 0);
        }
        for (int j = 0; j < 6; j++) {
            float fl = h((long) (t * 12) * 7 + j) > 0.15F ? 1 : 0.2F;
            double lat = (j - 2.5) / 3.0;
            Vec3 cc = c.ec.add(c.up().scale(lat * R));
            FxDraw.circle(cc, c.up(), R * Math.sqrt(1 - lat * lat), 0.12F, 0x80E0FF, 0.6F * p * fl, t * 0.5);
        }
        for (int i = 0; i < pts.size(); i++) {
            Vec3 tip = c.at(pts.get(i));
            Vec3 hit = c.ec.add(tip.subtract(c.ec).normalize().scale(R));
            FxDraw.beam(tip, hit, 0.3F, 0x80E0FF, p * (0.8F + 0.2F * (float) Math.sin(t * 7 + i)));
            FxDraw.glow(hit, 1.5F, 0xC0F0FF, 0.6F * p);
        }
    }

    /** The anomaly at the foot of the spire, its strands drawn out of it and up, unwound, to settle at the top. */
    static void resolution(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 6);
        Vec3 top = c.at(point(o, "top", new Vec3(c.d.ex, c.d.ey + 80, c.d.ez)));
        FxDraw.glow(c.ec, R * (1 + 0.15F * (float) Math.sin(t * 4)), 0xFF40A0, 0.6F * p);
        for (int s = 0; s < 5; s++) {
            Vec3 prev = null;
            double ph = s * Math.PI * 2 / 5;
            for (int i = 0; i <= 50; i++) {
                double u = i / 50.0;
                double wind = (1 - u) * 3.2, a = ph + t * 1.5 + u * 14;
                Vec3 q = lerp(c.ec, top, u).add(c.dir(Math.cos(a) * wind, 0, Math.sin(a) * wind));
                int rgb = FxDraw.lerpColor(0xFF40A0, 0x80FFE0, (float) u);
                if (prev != null) FxDraw.ribbon(prev, q, 0.2F, rgb, 0.7F * p, rgb, 0.7F * p);
                prev = q;
            }
            for (int k = 0; k < 4; k++) {
                float q = (t * 0.2F + k / 4F + s * 0.1F) % 1F;
                double a = ph + t * 1.5 + q * 14, wind = (1 - q) * 3.2;
                FxDraw.glow(lerp(c.ec, top, q).add(c.dir(Math.cos(a) * wind, 0, Math.sin(a) * wind)), 0.6F, 0xFFFFFF, 0.8F * p);
            }
        }
        FxDraw.glow(top, 3 + 0.5F * (float) Math.sin(t), 0x80FFE0, 0.7F * p);
    }

    static final double[][] CUBE = {{-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    static final int[][] CUBE_E = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    static final double[][] OCTA = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    static final int[][] OCTA_E = {{0, 2}, {0, 3}, {0, 4}, {0, 5}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {2, 4}, {4, 3}, {3, 5}, {5, 2}};
    static final double[][] TETRA = {{1, 1, 1}, {1, -1, -1}, {-1, 1, -1}, {-1, -1, 1}};
    static final int[][] TETRA_E = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}};

    /** Lines of code falling into the frame and compiling, edge by edge, into a shape, which holds, then dissolves. */
    static void compiler(JsonObject o, Ctx c) {
        float t = c.t, p = c.power, R = num(o, "radius", 7);
        float per = 9, q = cycle(t, per, c.seed);
        int which = (int) Math.floorMod((long) ((t / per) + (c.seed & 1023) / 1023.0), 3L);
        double[][] v = which == 0 ? CUBE : which == 1 ? OCTA : TETRA;
        int[][] e = which == 0 ? CUBE_E : which == 1 ? OCTA_E : TETRA_E;
        double s = which == 0 ? R * 0.55 : R * 0.85;
        // the code: short dashes falling in columns round the shape
        for (int k = 0; k < 60; k++) {
            float fq = (t * (0.4F + h(k) * 0.3F) + h(k + 1)) % 1F;
            double x = (h(k + 2) - 0.5) * R * 2, z = (h(k + 3) - 0.5) * R * 2;
            Vec3 at = c.ec.add(c.dir(x, R * 1.6 * (1 - 2 * fq), z));
            FxDraw.ribbon(at, at.add(c.dir(0.6 + h(k + 4), 0, 0)), 0.15F, 0x60FF90, 0.8F * p * bell(fq), 0x60FF90, 0.3F * p * bell(fq));
        }
        double spin = t * 0.4;
        float drawn = Math.min(1, q / 0.5F), fade = 1 - smooth((q - 0.85F) / 0.15F);
        for (int i = 0; i < e.length; i++) {
            float part = Math.max(0, Math.min(1, drawn * e.length - i));
            if (part <= 0) continue;
            Vec3 a = vert(c, v[e[i][0]], s, spin), b = vert(c, v[e[i][1]], s, spin);
            Vec3 b2 = lerp(a, b, part);
            FxDraw.ribbon(a, b2, 0.18F, 0xA0FFC0, 0.95F * p * fade, 0xFFFFFF, 0.95F * p * fade);
            if (part < 1) FxDraw.glow(b2, 0.6F, 0xFFFFFF, p);
        }
        if (q > 0.5F) FxDraw.glow(c.ec, (float) s * 1.1F, 0x60FFA0, 0.3F * p * fade * smooth((q - 0.5F) / 0.1F));
    }

    static Vec3 vert(Ctx c, double[] v, double s, double spin) {
        double x = v[0] * Math.cos(spin) - v[2] * Math.sin(spin), z = v[0] * Math.sin(spin) + v[2] * Math.cos(spin);
        return c.ec.add(c.dir(x * s, v[1] * s, z * s));
    }

    /** A bright colour round the colour wheel (0..1). */
    static int hue(float u) {
        float r = 0.5F + 0.5F * (float) Math.cos(Math.PI * 2 * u), g = 0.5F + 0.5F * (float) Math.cos(Math.PI * 2 * (u - 1 / 3F)),
                b = 0.5F + 0.5F * (float) Math.cos(Math.PI * 2 * (u - 2 / 3F));
        return (int) (80 + 175 * r) << 16 | (int) (80 + 175 * g) << 8 | (int) (80 + 175 * b);
    }

    private EndgameFx() {}
}
