package com.theoreticalsciences.client.render.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theoreticalsciences.block.CasingBlock;
import com.theoreticalsciences.client.ClientHooks;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 1.2.0: the moving parts of the mod's own multiblocks ({@code "moving"} in their multiblock JSON), built for each
 * formed controller from the blocks actually in the world: so a slot that takes several tiers shows the tier that was
 * built. Only blocks the server has marked as moving (hidden in the world while formed) are taken, so a block is
 * never drawn twice. Rebuilt when those blocks change; freed when the controller goes.
 */
public final class MovingParts {
    private static final class Live {
        Animations.Baked baked;
        long sig = Long.MIN_VALUE;
        long checked;
        boolean fresh = true;
    }

    private static final Map<ControllerBlockEntity, Live> LIVE = new HashMap<>();
    /** The frame counter, and the last frame anything was baked in (one machine's parts a frame, so none hitch). */
    public static long frame;
    private static long bakedIn = -1;

    /** Look again at once (the structure formed or broke). */
    public static void invalidate(ControllerBlockEntity c) {
        Live l = LIVE.get(c);
        if (l != null) l.fresh = true;
    }

    /** The baked parts for c now (null when it has none showing). Rechecks the world every ten seconds, and at once when
     *  the structure forms or breaks. */
    @Nullable
    public static Animations.Baked get(ControllerBlockEntity c) {
        Level lv = c.getLevel();
        MultiblockDefinition def = c.definition();
        if (lv == null || def == null || def.moving.isEmpty() || !c.isFormed()) {
            drop(c);
            return null;
        }
        Live l = LIVE.computeIfAbsent(c, k -> new Live());
        long now = lv.getGameTime();
        // (the first look is at once; then every two seconds. Comparing with a "long ago" of Long.MIN_VALUE overflowed,
        // so the first look never came and the hidden blocks were never drawn)
        if (l.fresh || now - l.checked >= 200 || now < l.checked) {
            if (bakedIn == frame) return l.baked;      // another machine baked this frame: this one waits for the next
            l.fresh = false;
            l.checked = now;
            Collected col = collect(c, def, lv);
            if (col.sig != l.sig) {
                l.sig = col.sig;
                Animations.close(l.baked);
                l.baked = col.cells.isEmpty() ? null : Animations.bakeLive(build(c, def, col));
                bakedIn = frame;
                if (l.baked != null)
                    com.theoreticalsciences.TheoreticalSciences.LOGGER.info("Moving parts of {} at {}: {} parts, {} quads", def.id, c.getBlockPos().toShortString(),
                            l.baked.parts.length, l.baked.quads);
            }
        }
        return l.baked;
    }

    /** Frees the parts of controllers that are gone (called every frame). */
    public static void sweep() {
        if (LIVE.isEmpty()) return;
        Level lv = Minecraft.getInstance().level;
        for (Iterator<Map.Entry<ControllerBlockEntity, Live>> it = LIVE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<ControllerBlockEntity, Live> e = it.next();
            ControllerBlockEntity c = e.getKey();
            if (c.isRemoved() || c.getLevel() != lv || !ClientHooks.CONTROLLERS.contains(c) || !c.isFormed()) {
                Animations.close(e.getValue().baked);
                it.remove();
            }
        }
    }

    public static void clear() {
        for (Live l : LIVE.values()) Animations.close(l.baked);
        LIVE.clear();
    }

    private static void drop(ControllerBlockEntity c) {
        Live l = LIVE.remove(c);
        if (l != null) Animations.close(l.baked);
    }

    private record Collected(List<List<int[]>> cells, List<BlockState[]> states, long sig) {}

    /** For each spec, its cells (pattern coordinates) whose block in the world is marked moving. */
    private static Collected collect(ControllerBlockEntity c, MultiblockDefinition def, Level lv) {
        Direction f = c.facing();
        BlockPos at = c.getBlockPos();
        List<List<int[]>> cells = new ArrayList<>();
        List<BlockState[]> states = new ArrayList<>();
        long sig = 1;
        boolean any = false;
        List<List<int[]>> bySpec = def.movingCellsBySpec();
        for (int si = 0; si < def.moving.size(); si++) {
            List<int[]> cs = new ArrayList<>();
            List<BlockState> ss = new ArrayList<>();
            for (int[] q : bySpec.get(si)) {
                int x = q[0], y = q[1], z = q[2];
                BlockPos p = def.toWorld(at, f, x, y, z);
                BlockState st = lv.getBlockState(p);
                if (!st.hasProperty(CasingBlock.MOVING) || !st.getValue(CasingBlock.MOVING)) continue;
                cs.add(new int[]{x, y, z});
                ss.add(st);
                sig = sig * 31 + p.asLong() * 17 + st.hashCode();
            }
            any |= !cs.isEmpty();
            cells.add(cs);
            states.add(ss.toArray(new BlockState[0]));
        }
        return new Collected(any ? cells : List.of(), states, sig * 31 + f.ordinal());
    }

    /** The animation for c: each spec split into its parts, pivots and motions in machine space. */
    private static StructureAnimation build(ControllerBlockEntity c, MultiblockDefinition def, Collected col) {
        JsonArray parts = new JsonArray();
        List<List<Animations.Cell>> partCells = new ArrayList<>();
        Map<String, String> firstOf = new HashMap<>();
        for (int si = 0; si < def.moving.size(); si++) {
            MultiblockDefinition.MovingSpec s = def.moving.get(si);
            List<int[]> cs = col.cells.get(si);
            BlockState[] ss = col.states.get(si);
            if (cs.isEmpty()) continue;
            List<List<Integer>> pieces = split(s.split(), cs);
            for (int k = 0; k < pieces.size(); k++) {
                List<Integer> piece = pieces.get(k);
                List<Animations.Cell> pc = new ArrayList<>();
                double sx = 0, sy = 0, sz = 0;
                for (int i : piece) {
                    int[] p = cs.get(i);
                    int mx = p[0] - def.cx, my = p[1] - def.cy, mz = p[2] - def.cz;
                    pc.add(new Animations.Cell(mx, my, mz, ss[i].setValue(CasingBlock.MOVING, false)));
                    sx += mx;
                    sy += my;
                    sz += mz;
                }
                int n = piece.size();
                JsonObject o = new JsonObject();
                String id = s.id() + "_" + k;
                if (k == 0) firstOf.put(s.id(), id);
                o.addProperty("id", id);
                JsonObject src = new JsonObject();
                src.addProperty("type", "cells");
                o.add("source", src);
                JsonArray pv = new JsonArray();
                JsonElement pe = s.pivot();
                if (pe != null && pe.isJsonArray()) {
                    JsonArray a = pe.getAsJsonArray();
                    pv.add(a.get(0).getAsDouble() - def.cx);
                    pv.add(a.get(1).getAsDouble() - def.cy);
                    pv.add(a.get(2).getAsDouble() - def.cz);
                } else if (pe != null && pe.isJsonPrimitive() && pe.getAsString().equals("self")) {
                    pv.add(sx / n);
                    pv.add(sy / n);
                    pv.add(sz / n);
                } else {
                    pv.add(def.ex - def.cx);
                    pv.add(def.ey - def.cy);
                    pv.add(def.ez - def.cz);
                }
                o.add("pivot", pv);
                if (s.parent() != null && firstOf.containsKey(s.parent())) o.addProperty("parent", firstOf.get(s.parent()));
                o.addProperty("ramp_seconds", s.ramp());
                JsonArray mo = new JsonArray();
                for (JsonElement m : s.motion()) {
                    JsonObject mm = m.getAsJsonObject().deepCopy();
                    if (s.alternate() && (k & 1) == 1) {
                        for (String key : new String[]{"deg_per_sec", "amplitude"})
                            if (mm.has(key)) mm.addProperty(key, -mm.get(key).getAsFloat());
                    }
                    if (s.stagger() != 0) mm.addProperty("phase", (mm.has("phase") ? mm.get("phase").getAsFloat() : 0) + k * s.stagger());
                    mo.add(mm);
                }
                o.add("motion", mo);
                parts.add(o);
                partCells.add(pc);
            }
        }
        JsonObject j = new JsonObject();
        j.add("parts", parts);
        StructureAnimation a = StructureAnimation.parse(ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "live/" + def.id), j);
        for (int i = 0; i < a.parts.size(); i++) a.parts.get(i).cells = partCells.get(i);
        return a;
    }

    /** Indices of the cells, grouped: all together, by layer, or by connected piece (faces touching). */
    private static List<List<Integer>> split(String how, List<int[]> cs) {
        List<List<Integer>> out = new ArrayList<>();
        switch (how) {
            case "layers" -> {
                TreeMap<Integer, List<Integer>> by = new TreeMap<>();
                for (int i = 0; i < cs.size(); i++) by.computeIfAbsent(cs.get(i)[1], k -> new ArrayList<>()).add(i);
                out.addAll(by.values());
            }
            case "components" -> {
                Map<Long, Integer> idx = new HashMap<>();
                for (int i = 0; i < cs.size(); i++) idx.put(BlockPos.asLong(cs.get(i)[0], cs.get(i)[1], cs.get(i)[2]), i);
                boolean[] seen = new boolean[cs.size()];
                for (int i = 0; i < cs.size(); i++) {
                    if (seen[i]) continue;
                    List<Integer> g = new ArrayList<>();
                    Deque<Integer> q = new ArrayDeque<>();
                    q.add(i);
                    seen[i] = true;
                    while (!q.isEmpty()) {
                        int u = q.poll();
                        g.add(u);
                        int[] p = cs.get(u);
                        for (Direction d : Direction.values()) {
                            Integer v = idx.get(BlockPos.asLong(p[0] + d.getStepX(), p[1] + d.getStepY(), p[2] + d.getStepZ()));
                            if (v != null && !seen[v]) {
                                seen[v] = true;
                                q.add(v);
                            }
                        }
                    }
                    out.add(g);
                }
            }
            default -> {
                List<Integer> all = new ArrayList<>();
                for (int i = 0; i < cs.size(); i++) all.add(i);
                out.add(all);
            }
        }
        return out;
    }

    private MovingParts() {}
}
