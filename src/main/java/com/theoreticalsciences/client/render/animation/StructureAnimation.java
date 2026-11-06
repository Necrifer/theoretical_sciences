package com.theoreticalsciences.client.render.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.2.0: a structure animation, read from {@code assets/<ns>/ts_animations/<path>.json}: the moving parts of a
 * multiblock, each drawn by the renderer instead of placed as blocks, turning or sliding about its own pivot, and
 * carried along by its parent's movement. All positions are machine space: blocks from the controller's centre, as
 * {@code [right, up, back]}. The format is documented in docs/ANIMATIONS.md.
 */
public final class StructureAnimation {
    public final ResourceLocation id;
    public final List<Part> parts;
    /** Parts in an order where each comes after its parent. */
    public final List<Part> ordered;

    public enum SourceType { BLOCKS, MESH, MODEL, CELLS }

    public static final class Part {
        public final String id;
        public final JsonObject source;
        public final SourceType type;
        public final Vector3f pivot;
        @Nullable public final String parentId;
        public Part parent;
        public int index;
        public final List<Motion> motions;
        public final float ramp;
        /** For a CELLS part (a multiblock's own moving blocks, read from the world): its blocks, machine space. */
        public List<Animations.Cell> cells = List.of();
        /** Lit by the world's light where it is (sampled once at its pivot), or always full bright. */
        public final boolean fullBright;

        Part(String id, JsonObject src, Vector3f pivot, String parentId, List<Motion> motions, float ramp, boolean fullBright) {
            this.id = id;
            this.source = src;
            String t = src.has("type") ? src.get("type").getAsString().toLowerCase(Locale.ROOT) : "blocks";
            this.type = switch (t) {
                case "mesh" -> SourceType.MESH;
                case "model" -> SourceType.MODEL;
                case "cells" -> SourceType.CELLS;
                default -> SourceType.BLOCKS;
            };
            this.pivot = pivot;
            this.parentId = parentId;
            this.motions = motions;
            this.ramp = ramp;
            this.fullBright = fullBright;
        }
    }

    public enum Mode { ROTATE, MOVE }

    /** One movement of a part. Its clock is seconds of running (it stops with the machine, so nothing ever jumps). */
    public static final class Motion {
        final String type;
        final Mode mode;
        final Vector3f axis;
        final float rate, amplitude, period, phase, loop;
        final float[] keyT, keyV;
        final boolean smooth;

        Motion(String type, Mode mode, Vector3f axis, float rate, float amplitude, float period, float phase, float loop, float[] keyT, float[] keyV, boolean smooth) {
            this.type = type;
            this.mode = mode;
            this.axis = axis;
            this.rate = rate;
            this.amplitude = amplitude;
            this.period = period;
            this.phase = phase;
            this.loop = loop;
            this.keyT = keyT;
            this.keyV = keyV;
            this.smooth = smooth;
        }

        /** The amount at clock c: degrees for ROTATE, blocks for MOVE. */
        public float value(float c) {
            switch (type) {
                case "spin":
                    return c * rate;
                case "oscillate":
                    return amplitude * Mth.sin(Mth.TWO_PI * (c / Math.max(0.05F, period) + phase));
                default: {
                    if (keyT.length == 0) return 0;
                    float t = loop > 0 ? c % loop : Math.min(c, keyT[keyT.length - 1]);
                    if (t <= keyT[0]) return keyV[0];
                    for (int i = 1; i < keyT.length; i++) {
                        if (t <= keyT[i]) {
                            float u = (t - keyT[i - 1]) / Math.max(1e-4F, keyT[i] - keyT[i - 1]);
                            if (smooth) u = u * u * (3 - 2 * u);
                            return Mth.lerp(u, keyV[i - 1], keyV[i]);
                        }
                    }
                    return keyV[keyV.length - 1];
                }
            }
        }

        /** Applies this motion at clock c to m (already moved to the pivot). */
        public void apply(Matrix4f m, float c) {
            float v = value(c);
            if (mode == Mode.ROTATE) m.rotate((float) Math.toRadians(v), axis);
            else m.translate(axis.x * v, axis.y * v, axis.z * v);
        }
    }

    StructureAnimation(ResourceLocation id, List<Part> parts) {
        this.id = id;
        this.parts = parts;
        for (Part p : parts) {
            if (p.parentId != null) for (Part q : parts) if (q.id.equals(p.parentId) && q != p) p.parent = q;
        }
        // a parent loop is broken (the part that closes it has no parent); then parents are put before their children
        java.util.Map<Part, Integer> depth = new java.util.HashMap<>();
        for (Part p : parts) {
            java.util.Set<Part> seen = new java.util.HashSet<>();
            Part q = p;
            int d = 0;
            while (q.parent != null) {
                if (!seen.add(q)) break;
                if (seen.contains(q.parent)) {
                    q.parent = null;
                    break;
                }
                q = q.parent;
                d++;
            }
            depth.put(p, d);
        }
        List<Part> ord = new ArrayList<>(parts);
        ord.sort(java.util.Comparator.comparingInt(depth::get));
        for (int i = 0; i < ord.size(); i++) ord.get(i).index = i;
        this.ordered = ord;
    }

    // ------------------------------------------------------------------------------------------------ parsing
    public static StructureAnimation parse(ResourceLocation id, JsonObject json) {
        List<Part> parts = new ArrayList<>();
        JsonArray arr = json.has("parts") ? json.getAsJsonArray("parts") : new JsonArray();
        int n = 0;
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            String pid = o.has("id") ? o.get("id").getAsString() : "part_" + n;
            n++;
            JsonObject src = o.has("source") ? o.getAsJsonObject("source") : new JsonObject();
            Vector3f pivot = vec(o.get("pivot"), new Vector3f());
            String parent = o.has("parent") && !o.get("parent").isJsonNull() ? o.get("parent").getAsString() : null;
            List<Motion> motions = new ArrayList<>();
            if (o.has("motion")) {
                JsonElement me = o.get("motion");
                if (me.isJsonObject()) motions.add(motion(me.getAsJsonObject()));
                else for (JsonElement m : me.getAsJsonArray()) motions.add(motion(m.getAsJsonObject()));
            }
            float ramp = o.has("ramp_seconds") ? o.get("ramp_seconds").getAsFloat() : 3F;
            boolean full = o.has("light") && "full".equalsIgnoreCase(o.get("light").getAsString());
            parts.add(new Part(pid, src, pivot, parent, motions, Math.max(0.05F, ramp), full));
        }
        return new StructureAnimation(id, parts);
    }

    static Motion motion(JsonObject o) {
        String type = o.has("type") ? o.get("type").getAsString().toLowerCase(Locale.ROOT) : "spin";
        if (!type.equals("spin") && !type.equals("oscillate") && !type.equals("keyframes")) type = "spin";
        Mode mode = o.has("mode") && "move".equalsIgnoreCase(o.get("mode").getAsString()) ? Mode.MOVE : Mode.ROTATE;
        Vector3f axis = axis(o.get("axis"));
        float rate = o.has("deg_per_sec") ? o.get("deg_per_sec").getAsFloat() : 0;
        float amp = o.has("amplitude") ? o.get("amplitude").getAsFloat() : 0;
        float period = o.has("period_seconds") ? o.get("period_seconds").getAsFloat() : 2;
        float phase = o.has("phase") ? o.get("phase").getAsFloat() : 0;
        float loop = o.has("loop_seconds") ? o.get("loop_seconds").getAsFloat() : 0;
        float[] kt = new float[0], kv = new float[0];
        if (o.has("keys")) {
            JsonArray ks = o.getAsJsonArray("keys");
            kt = new float[ks.size()];
            kv = new float[ks.size()];
            for (int i = 0; i < ks.size(); i++) {
                JsonArray k = ks.get(i).getAsJsonArray();
                kt[i] = k.get(0).getAsFloat();
                kv[i] = k.get(1).getAsFloat();
            }
        }
        boolean smooth = !o.has("ease") || !"linear".equalsIgnoreCase(o.get("ease").getAsString());
        return new Motion(type, mode, axis, rate, amp, period, phase, loop, kt, kv, smooth);
    }

    /** "right", "up", "back" (or "-right" and so on, "left", "down", "front"), or [x, y, z] in machine space. */
    public static Vector3f axis(@Nullable JsonElement e) {
        if (e == null || e.isJsonNull()) return new Vector3f(0, 1, 0);
        if (e.isJsonArray()) {
            Vector3f v = vec(e, new Vector3f(0, 1, 0));
            return v.lengthSquared() < 1e-8 ? new Vector3f(0, 1, 0) : v.normalize();
        }
        String s = e.getAsString().toLowerCase(Locale.ROOT).trim();
        float sign = 1;
        if (s.startsWith("-")) {
            sign = -1;
            s = s.substring(1);
        }
        return switch (s) {
            case "right" -> new Vector3f(sign, 0, 0);
            case "left" -> new Vector3f(-sign, 0, 0);
            case "back" -> new Vector3f(0, 0, sign);
            case "front", "forward" -> new Vector3f(0, 0, -sign);
            case "down" -> new Vector3f(0, -sign, 0);
            default -> new Vector3f(0, sign, 0);
        };
    }

    public static Vector3f vec(@Nullable JsonElement e, Vector3f dflt) {
        if (e == null || !e.isJsonArray()) return dflt;
        JsonArray a = e.getAsJsonArray();
        return new Vector3f(a.size() > 0 ? a.get(0).getAsFloat() : 0, a.size() > 1 ? a.get(1).getAsFloat() : 0, a.size() > 2 ? a.get(2).getAsFloat() : 0);
    }
}
