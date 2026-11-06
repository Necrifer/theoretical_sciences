package com.theoreticalsciences.client.render.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * 1.2.0: working effects a multiblock asks for in its JSON ({@code "fx"}), drawn while it runs. Each entry has a
 * {@code type}; positions are pattern coordinates (as the layers are written), so they turn with the machine.
 * <ul>
 *   <li>{@code glow} ({@code chars}, {@code color}, {@code hot}): the blocks of those key characters heat up while the
 *       machine runs, glowing from {@code color} to {@code hot}, and cool slowly after it stops.</li>
 *   <li>{@code inflow} ({@code radius}, {@code color}, {@code waves}, {@code period}): rings of energy closing in on the
 *       centre from a radius, one after another, with sparks running in along the spokes.</li>
 *   <li>{@code inject} ({@code points}, {@code color}, {@code width}): beams from each point into the centre, pulses
 *       running along them, flaring where they meet: energy feeding what is held there.</li>
 *   <li>{@code universe} ({@code radius}, {@code color}): a little universe in a sphere: starry space, a few turning
 *       spiral galaxies and a young star at its heart.</li>
 *   <li>{@code swirl} ({@code radius}): what the running recipe is made of, swirling round the centre.</li>
 *   <li>{@code bubbles} ({@code points}, {@code color}, {@code rise}): bubble universes swelling at the points, breaking
 *       free and drifting up, starry inside.</li>
 *   <li>{@code drips} ({@code points}, {@code color}, {@code fall}): drops gathering at the points and falling.</li>
 *   <li>{@code echo} ({@code scale}): the Transcendental's rekindling, traced again and again above the centre.</li>
 *   <li>{@code star} ({@code radius}, {@code color}, {@code at}), {@code shell} ({@code radius}, {@code color}, {@code at}),
 *       {@code orbiters} ({@code count}, {@code inner}, {@code outer}, {@code blocks}, {@code at}) and {@code beam}
 *       ({@code start_back}, {@code end_back}, {@code up}, {@code width}, {@code color}): the plain effects, for any
 *       multiblock that wants them ({@code at}, {@code up} and the backs in pattern coordinates).</li>
 *   <li>the late and endgame machines' own animations, one each: see {@link EndgameFx}.</li>
 * </ul>
 */
public final class MachineFx {
    record Entry(String type, JsonObject o, List<Vec3> points, List<BlockPos> cells) {}

    private static final Map<MultiblockDefinition, List<Entry>> PARSED = new WeakHashMap<>();
    private static final Map<Object, float[]> HEAT = new WeakHashMap<>();

    static List<Entry> entries(MultiblockDefinition def) {
        return PARSED.computeIfAbsent(def, d -> {
            List<Entry> out = new ArrayList<>();
            for (JsonElement e : d.fx) {
                JsonObject o = e.getAsJsonObject();
                List<Vec3> pts = new ArrayList<>();
                if (o.has("points")) for (JsonElement p : o.getAsJsonArray("points")) {
                    JsonArray a = p.getAsJsonArray();
                    pts.add(new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()));
                }
                List<BlockPos> cells = new ArrayList<>();
                if (o.has("chars")) {
                    String ch = o.get("chars").getAsString();
                    for (int y = 0; y < d.layers.size(); y++)
                        for (int z = 0; z < d.layers.get(y).size(); z++) {
                            String row = d.layers.get(y).get(z);
                            for (int x = 0; x < row.length(); x++) if (ch.indexOf(row.charAt(x)) >= 0) cells.add(new BlockPos(x, y, z));
                        }
                }
                out.add(new Entry(o.has("type") ? o.get("type").getAsString() : "", o, pts, cells));
            }
            return out;
        });
    }

    static int col(JsonObject o, String k, int d) {
        return FxSpec.parseColor(o.has(k) ? o.get(k).getAsString() : null, d);
    }

    static float num(JsonObject o, String k, float d) {
        return o.has(k) ? o.get(k).getAsFloat() : d;
    }

    /** Pattern coordinates (a block's centre at integer values) to the world. */
    static Vec3 world(MultiblockDefinition d, Vec3 ctr, Direction f, double x, double y, double z) {
        return FxFrame.toWorld(ctr, f, x - d.cx, y - d.cy, z - d.cz);
    }

    /** True when the machine has any effects of its own. */
    public static boolean has(ControllerBlockEntity c) {
        MultiblockDefinition d = c.definition();
        return d != null && !d.fx.isEmpty();
    }

    /** How far from the controller the structure (and so its effects) can reach. */
    public static float reach(ControllerBlockEntity c) {
        MultiblockDefinition d = c.definition();
        if (d == null || d.layers.isEmpty()) return 0;
        int w = 0, h = d.layers.size(), dz = 0;
        for (var l : d.layers) {
            dz = Math.max(dz, l.size());
            for (String r : l) w = Math.max(w, r.length());
        }
        return (float) Math.sqrt(w * w + h * h + dz * dz) + 4;
    }

    /** The parts drawn solid (they hide what is behind them): the spheres of space. */
    public static void drawOpaque(FxFrame f, ControllerBlockEntity c, Vec3 ctr, Direction front, float power, long seed, PoseStack ps,
                                  MultiBufferSource buf, float clock) {
        MultiblockDefinition d = c.definition();
        if (d == null || power <= 0.002F) return;
        Matrix4f mv = f.machine(ctr, front);
        lastDef = d;
        Vector3f centre = new Vector3f((float) (d.ex - d.cx), (float) (d.ey - d.cy), (float) (d.ez - d.cz));
        for (Entry e : entries(d)) {
            switch (e.type) {
                case "star" -> StarEffect.drawSurface(f, mv, at(e.o, centre), num(e.o, "radius", 3), col(e.o, "color", FxSpec.STAR_ORANGE), power);
                case "shell" -> SpaceShellEffect.draw(f, mv, at(e.o, centre), num(e.o, "radius", 8), col(e.o, "color", 0x8060FF), power);
                case "orbiters" -> {
                    List<net.minecraft.world.level.block.state.BlockState> blocks = new ArrayList<>();
                    if (e.o.has("blocks")) for (JsonElement b : e.o.getAsJsonArray("blocks")) {
                        var bl = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(b.getAsString()));
                        if (bl != net.minecraft.world.level.block.Blocks.AIR) blocks.add(bl.defaultBlockState());
                    }
                    if (blocks.isEmpty()) blocks.add(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
                    float in = num(e.o, "inner", 3), out = num(e.o, "outer", in + 4);
                    OrbiterEffect.draw(ps, f.machineLocal(ctr, front), buf, at(e.o, centre), Math.min(16, (int) num(e.o, "count", 8)), in, out, blocks, seed, clock, power);
                }
                case "universe" -> {
                    float r = num(e.o, "radius", 4);
                    SpaceShellEffect.draw(f, mv, centre, r, col(e.o, "color", 0x8060FF), power);
                    StarEffect.drawSurface(f, mv, centre, r * 0.1F, 0xFFD890, power);
                }
                case "bubbles" -> {
                    for (int i = 0; i < e.points.size(); i++) {
                        Bubble b = bubble(e, i, f.time, seed);
                        if (b.r <= 0.02F) continue;
                        Vec3 p = e.points.get(i);
                        Vector3f at = new Vector3f((float) (p.x - d.cx + b.dx), (float) (p.y - d.cy + b.dy), (float) (p.z - d.cz + b.dz));
                        SpaceShellEffect.draw(f, mv, at, b.r, col(e.o, "color", 0x80FFE0), power * b.a);
                    }
                }
                default -> {
                    if (EndgameFx.TYPES.contains(e.type)) EndgameFx.drawOpaque(e.type, e.o, f, mv, d, centre, power, seed);
                }
            }
        }
    }

    /** An entry's own centre ({@code at}, pattern coordinates) in machine space, or the machine's effect centre. */
    static Vector3f at(JsonObject o, Vector3f centre) {
        if (!o.has("at")) return centre;
        JsonArray a = o.getAsJsonArray("at");
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()).sub(lastDef == null ? 0 : lastDef.cx,
                lastDef == null ? 0 : lastDef.cy, lastDef == null ? 0 : lastDef.cz);
    }

    private static MultiblockDefinition lastDef;

    record Bubble(float r, float dx, float dy, float dz, float a) {}

    /** Bubble i: swells at its point, breaks free, drifts up and fades; then another forms. */
    static Bubble bubble(Entry e, int i, float t, long seed) {
        float life = num(e.o, "period", 9), rise = num(e.o, "rise", 6), size = num(e.o, "size", 0.9F);
        float off = (float) ((i * 0.6180339887 + (seed & 255) / 255.0) % 1.0);
        float q = ((t / life) + off) % 1F;
        float r, dy = 0, a = 1;
        if (q < 0.35F) r = size * smooth(q / 0.35F);
        else {
            float k = (q - 0.35F) / 0.65F;
            r = size * (1 + 0.25F * k);
            dy = rise * k * k * (3 - 2 * k) + r;
            a = 1 - smooth((k - 0.7F) / 0.3F);
        }
        float sway = (float) Math.sin(t * 0.7 + i * 2.1) * 0.4F * (q > 0.35F ? 1 : 0);
        return new Bubble(r, sway, dy, (float) Math.cos(t * 0.6 + i * 1.3) * 0.4F * (q > 0.35F ? 1 : 0), Math.max(0, a));
    }

    static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    /** The light: drawn added onto everything else. */
    public static void drawLight(FxFrame f, ControllerBlockEntity c, Vec3 ctr, Direction front, float power, boolean running, double dtSeconds,
                                 PoseStack ps, MultiBufferSource buf, long seed) {
        MultiblockDefinition d = c.definition();
        if (d == null) return;
        float[] heat = HEAT.computeIfAbsent(c, k -> new float[1]);
        // the coils take about ten seconds to come up to heat, and longer to cool
        heat[0] = running ? Math.min(1, heat[0] + (float) dtSeconds / 10F) : Math.max(0, heat[0] - (float) dtSeconds / 25F);
        if (power <= 0.002F && heat[0] <= 0.002F) return;
        Vec3 ec = world(d, ctr, front, d.ex, d.ey, d.ez);
        Vec3 up = FxFrame.toWorld(Vec3.ZERO, front, 0, 1, 0);
        float t = f.time;
        lastDef = d;
        Vector3f centreM = new Vector3f((float) (d.ex - d.cx), (float) (d.ey - d.cy), (float) (d.ez - d.cz));
        boolean began = !FxDraw.active();
        if (began) FxDraw.begin(f.cam, f.left, f.up);
        List<ItemStack> swirl = null;
        float swirlR = 0;
        for (Entry e : entries(d)) {
            JsonObject o = e.o;
            switch (e.type) {
                case "star" -> StarEffect.drawCorona(f, f.machine(ctr, front), at(o, centreM), num(o, "radius", 3), col(o, "color", FxSpec.STAR_ORANGE), power);
                case "beam" -> {
                    // along the machine's depth (back), at a height (up): from start_back to end_back
                    if (power > 0.01F) {
                        float z0 = num(o, "start_back", 2) - d.cz, z1 = num(o, "end_back", (float) d.ez) - d.cz, y = num(o, "up", (float) d.ey) - d.cy;
                        float w = num(o, "width", 0.8F), a = Math.min(1F, power), scroll = t * 0.6F;
                        int rgb = col(o, "color", FxSpec.STAR_ORANGE);
                        Matrix4f local = f.machineLocal(ctr, front);
                        BeamEffect.draw(ps, local, buf, y, z0, z1, w * a, rgb, 0.55F * a, scroll, true);
                        BeamEffect.draw(ps, local, buf, y, z0, z1, w * 0.3F * a, 0xFFFFFF, 0.9F * a, scroll * 1.7F, false);
                    }
                }
                case "glow" -> {
                    float h = heat[0];
                    int cold = col(o, "color", 0xFF4020), hot = col(o, "hot", 0xFFF0C0);
                    float pulse = 0.9F + 0.1F * (float) Math.sin(t * 3);
                    int rgb = FxDraw.lerpColor(cold, hot, h * h);
                    float a = (0.08F + 0.5F * h) * pulse;
                    for (BlockPos p : e.cells) {
                        Vec3 w = world(d, ctr, front, p.getX(), p.getY(), p.getZ());
                        if (w.distanceToSqr(f.cam) > 160 * 160) continue;
                        FxDraw.box(w.add(-0.51, -0.51, -0.51), 1.02, rgb, a);
                    }
                }
                case "inflow" -> {
                    float R = num(o, "radius", 6), per = num(o, "period", 2.5F);
                    int n = (int) num(o, "waves", 4), rgb = col(o, "color", 0x80C0FF);
                    for (int k = 0; k < n; k++) {
                        float q = ((t / per) + k / (float) n) % 1F;
                        double r = R * (1 - q);
                        float a = power * smooth(q / 0.15F) * (1 - smooth((q - 0.8F) / 0.2F));
                        FxDraw.circle(ec, up, r, 0.12F + 0.25F * (1 - q), rgb, 0.7F * a, t * 0.3);
                        FxDraw.circle(ec, up, r, 0.5F + 0.6F * (1 - q), rgb, 0.12F * a, t * 0.3);
                    }
                    int spokes = 12;
                    for (int i = 0; i < spokes; i++) {
                        double an = i * Math.PI * 2 / spokes;
                        Vec3 dir = FxFrame.toWorld(Vec3.ZERO, front, Math.cos(an), 0, Math.sin(an));
                        for (int j = 0; j < 3; j++) {
                            float q = ((t / per) * 1.3F + j / 3F + i * 0.17F) % 1F;
                            Vec3 a0 = ec.add(dir.scale(R * (1 - q))), a1 = ec.add(dir.scale(Math.max(0, R * (1 - q) - 1.2)));
                            FxDraw.ribbon(a0, a1, 0.12F, rgb, 0, 0xFFFFFF, 0.8F * power * (1 - q));
                        }
                    }
                    FxDraw.glow(ec, 1.2F + 0.3F * (float) Math.sin(t * 4), rgb, 0.5F * power);
                }
                case "inject" -> {
                    int rgb = col(o, "color", 0xFFB060);
                    float w = num(o, "width", 0.35F);
                    for (int i = 0; i < e.points.size(); i++) {
                        Vec3 p = e.points.get(i), s = world(d, ctr, front, p.x, p.y, p.z);
                        float flick = 0.85F + 0.15F * (float) Math.sin(t * 9 + i * 1.7);
                        FxDraw.beam(s, ec, w * flick, rgb, power);
                        for (int j = 0; j < 4; j++) {
                            float q = (t * 0.8F + j / 4F + i * 0.13F) % 1F;
                            FxDraw.glow(s.add(ec.subtract(s).scale(q)), w * 2.2F, 0xFFFFFF, 0.7F * power * (float) Math.sin(q * Math.PI));
                        }
                        FxDraw.glow(s, w * 4, rgb, 0.8F * power);
                    }
                    FxDraw.glow(ec, w * 8 * (0.9F + 0.1F * (float) Math.sin(t * 7)), rgb, 0.5F * power);
                    FxDraw.glow(ec, w * 3, 0xFFFFFF, 0.8F * power);
                }
                case "universe" -> {
                    float r = num(o, "radius", 4);
                    galaxies(ec, r, t, seed, power, col(o, "color", 0x8060FF));
                    FxDraw.glow(ec, r * 0.35F, 0xFFE0A0, 0.5F * power);
                }
                case "swirl" -> {
                    swirlR = num(o, "radius", 4);
                    swirl = ingredients(c);
                }
                case "bubbles" -> {
                    int rgb = col(o, "color", 0x80FFE0);
                    for (int i = 0; i < e.points.size(); i++) {
                        Bubble b = bubble(e, i, t, seed);
                        if (b.r <= 0.02F) continue;
                        Vec3 p = e.points.get(i);
                        Vec3 w = world(d, ctr, front, p.x + b.dx, p.y + b.dy, p.z + b.dz);
                        FxDraw.circle(w, w.subtract(f.cam), b.r * 1.02, 0.06F, rgb, 0.8F * power * b.a, 0);
                        FxDraw.glow(w, b.r * 1.6F, rgb, 0.15F * power * b.a);
                    }
                }
                case "drips" -> {
                    int rgb = col(o, "color", 0xFFE080);
                    float fall = num(o, "fall", 6), per = num(o, "period", 3);
                    for (int i = 0; i < e.points.size(); i++) {
                        Vec3 p = e.points.get(i), s = world(d, ctr, front, p.x, p.y, p.z);
                        float fall_i = o.has("floor") ? (float) Math.max(0.5, p.y - o.get("floor").getAsDouble()) : fall;
                        float q = ((t / per) + (float) ((i * 0.37 + (seed & 63) / 63.0) % 1.0)) % 1F;
                        if (q < 0.45F) FxDraw.glow(s, 0.08F + 0.14F * (q / 0.45F), rgb, 0.9F * power);          // gathering
                        else {
                            float k = (q - 0.45F) / 0.55F, y = fall_i * k * k;
                            Vec3 at = s.subtract(up.scale(y)), tail = at.add(up.scale(Math.min(1.2, 0.3 + y * 0.3)));
                            if (k < 0.92F) {
                                FxDraw.ribbon(tail, at, 0.1F, rgb, 0, rgb, 0.8F * power);
                                FxDraw.glow(at, 0.18F, rgb, 0.9F * power);
                            } else {
                                float sp = (k - 0.92F) / 0.08F;                                                     // the splash
                                Vec3 floor = s.subtract(up.scale(fall_i));
                                FxDraw.circle(floor, up, 0.2 + sp * 0.9, 0.06F, rgb, 0.8F * power * (1 - sp), 0);
                            }
                        }
                    }
                }
                case "echo" -> TranscendentalEcho.draw(ec, num(o, "scale", 1), num(o, "sky", 14), t, power);
                default -> {
                    if (EndgameFx.TYPES.contains(e.type) && power > 0.002F)
                        EndgameFx.drawLight(e.type, o, e.points, new EndgameFx.Ctx(d, ctr, front, ec, t, power, seed, f.cam));
                }
            }
        }
        if (began) FxDraw.end();
        if (swirl != null && !swirl.isEmpty() && power > 0.01F) swirlItems(f, ec, swirlR, swirl, t, power, ps, buf);
    }

    /** Three spiral galaxies turning in the universe, each a spray of stars on two arms. */
    static void galaxies(Vec3 c, float r, float t, long seed, float power, int tint) {
        Random rnd = new Random(seed ^ 0x5EEDL);
        for (int g = 0; g < 3; g++) {
            Vec3 at = c.add((rnd.nextDouble() - 0.5) * r * 1.1, (rnd.nextDouble() - 0.5) * r * 0.8, (rnd.nextDouble() - 0.5) * r * 1.1);
            double tilt = rnd.nextDouble() * Math.PI, yaw = rnd.nextDouble() * Math.PI * 2;
            float size = r * (0.18F + 0.12F * rnd.nextFloat());
            double spin = t * (0.15 + 0.1 * g) * (g % 2 == 0 ? 1 : -1);
            int core = g == 1 ? 0xFFD0A0 : 0xC0D0FF;
            for (int i = 0; i < 60; i++) {
                double u = i / 60.0, arm = (i % 2) * Math.PI;
                double a = spin + arm + u * 4.5, rr = size * u;
                double x = Math.cos(a) * rr, z = Math.sin(a) * rr, y = (rnd.nextDouble() - 0.5) * size * 0.08;
                double y2 = y * Math.cos(tilt) - z * Math.sin(tilt), z2 = y * Math.sin(tilt) + z * Math.cos(tilt);
                double x3 = x * Math.cos(yaw) - z2 * Math.sin(yaw), z3 = x * Math.sin(yaw) + z2 * Math.cos(yaw);
                FxDraw.glow(at.add(x3, y2, z3), 0.05F + 0.06F * (float) (1 - u), FxDraw.lerpColor(core, tint, (float) u), 0.8F * power * (float) (1 - u * 0.6));
            }
            FxDraw.glow(at, size * 0.25F, core, 0.6F * power);
        }
    }

    /** What the running recipe uses, one stack per ingredient (from the recipe the machine says it is on). */
    static List<ItemStack> ingredients(ControllerBlockEntity c) {
        ResourceLocation id = c.shownRecipe();
        var lv = Minecraft.getInstance().level;
        if (id == null || lv == null) return List.of();
        var h = lv.getRecipeManager().byKey(id);
        if (h.isEmpty() || !(h.get().value() instanceof QHRecipe r)) return List.of();
        List<ItemStack> out = new ArrayList<>();
        for (var si : r.itemInputs) {
            ItemStack[] it = si.getItems();
            if (it.length > 0) out.add(it[0]);
        }
        return out;
    }

    /** The ingredients ride round the centre on tilted, slowly shrinking orbits, as if being drawn in. */
    static void swirlItems(FxFrame f, Vec3 ec, float R, List<ItemStack> items, float t, float power, PoseStack ps, MultiBufferSource buf) {
        var ir = Minecraft.getInstance().getItemRenderer();
        int n = Math.min(24, Math.max(items.size() * 3, 6));
        for (int i = 0; i < n; i++) {
            ItemStack st = items.get(i % items.size());
            double q = ((t * 0.05) + i / (double) n) % 1.0;
            double r = R * (1.0 - 0.45 * q), a = t * (0.6 + 0.3 * (i % 3)) / Math.max(0.5, r / R) + i * 2.399;
            double tilt = (i % 5 - 2) * 0.25;
            double x = Math.cos(a) * r, z = Math.sin(a) * r, y = Math.sin(a) * r * tilt;
            ps.pushPose();
            ps.translate(ec.x - f.cam.x + x, ec.y - f.cam.y + y, ec.z - f.cam.z + z);
            ps.mulPose(new org.joml.Quaternionf().rotateY(t * 2 + i));
            float s = (0.55F + 0.25F * (float) Math.sin(t * 2 + i)) * Math.min(1, power) * (float) (1 - 0.6 * q);
            ps.scale(s, s, s);
            ir.renderStatic(st, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ps, buf, Minecraft.getInstance().level, i);
            ps.popPose();
        }
    }

    private MachineFx() {}
}
