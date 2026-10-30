package com.theoreticalsciences.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * A multiblock shape, defined in {@code assets/theoreticalsciences/multiblocks/<id>.json}.
 * <ul>
 *   <li>{@code layers}: bottom to top; each layer is a list of rows (front to back), each row a string (left to right).</li>
 *   <li>{@code key}: character to list of accepted block ids. {@code .} = must be air, space = anything.</li>
 *   <li>{@code controller}: the character marking the controller (default {@code C}).</li>
 *   <li>{@code effect_center}: pattern coordinates of the visual effect centre.</li>
 * </ul>
 * Patterns are written for a controller facing NORTH (its front looks toward -Z, the structure extends to +Z).
 */
public final class MultiblockDefinition {
    public final String id;
    public final ResourceLocation controllerBlock;
    public final List<List<String>> layers;
    public final Map<Character, List<ResourceLocation>> key;
    public final char controllerChar;
    public final int cx, cy, cz;
    public final double ex, ey, ez;
    public final int sizeX, sizeY, sizeZ;
    /**
     * GT-style tiered components: a key character whose accepted blocks are listed lowest tier first.
     * {@code "tiers": {"K": {"family": "coil", "base": 1}}} - the structure's tier in a family is the LOWEST tier found.
     * Datapacks can add more parts to a family (see {@link com.theoreticalsciences.custom.ComponentTiers}).
     */
    public final Map<Character, TierSpec> tiers;

    public record TierSpec(String family, int base) {}

    private MultiblockDefinition(String id, JsonObject json) {
        this.id = id;
        this.controllerBlock = ResourceLocation.parse(json.get("controller_block").getAsString());
        this.controllerChar = json.has("controller") ? json.get("controller").getAsString().charAt(0) : 'C';
        List<List<String>> ls = new ArrayList<>();
        for (JsonElement layer : json.getAsJsonArray("layers")) {
            List<String> rows = new ArrayList<>();
            for (JsonElement row : layer.getAsJsonArray()) rows.add(row.getAsString());
            ls.add(List.copyOf(rows));
        }
        this.layers = List.copyOf(ls);
        Map<Character, List<ResourceLocation>> k = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("key").entrySet()) {
            List<ResourceLocation> ids = new ArrayList<>();
            if (e.getValue().isJsonArray()) for (JsonElement s : e.getValue().getAsJsonArray()) ids.add(ResourceLocation.parse(s.getAsString()));
            else ids.add(ResourceLocation.parse(e.getValue().getAsString()));
            k.put(e.getKey().charAt(0), List.copyOf(ids));
        }
        this.key = Map.copyOf(k);
        Map<Character, TierSpec> t = new HashMap<>();
        if (json.has("tiers")) for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("tiers").entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            t.put(e.getKey().charAt(0), new TierSpec(o.get("family").getAsString(), o.has("base") ? o.get("base").getAsInt() : 1));
        }
        this.tiers = Map.copyOf(t);
        int fx = -1, fy = -1, fz = -1, mx = 0, mz = 0;
        for (int y = 0; y < layers.size(); y++) {
            List<String> rows = layers.get(y);
            mz = Math.max(mz, rows.size());
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                mx = Math.max(mx, row.length());
                int x = row.indexOf(controllerChar);
                if (x >= 0) {
                    fx = x;
                    fy = y;
                    fz = z;
                }
            }
        }
        if (fx < 0) throw new IllegalStateException("Multiblock " + id + " has no controller character");
        this.cx = fx;
        this.cy = fy;
        this.cz = fz;
        this.sizeX = mx;
        this.sizeY = layers.size();
        this.sizeZ = mz;
        JsonArray ec = json.has("effect_center") ? json.getAsJsonArray("effect_center") : null;
        this.ex = ec != null ? ec.get(0).getAsDouble() : (mx - 1) / 2.0;
        this.ey = ec != null ? ec.get(1).getAsDouble() : (layers.size() - 1) / 2.0;
        this.ez = ec != null ? ec.get(2).getAsDouble() : (mz - 1) / 2.0;
    }

    static MultiblockDefinition parse(String id, JsonObject json) {
        return new MultiblockDefinition(id, json);
    }

    /** Character at pattern coordinates, or ' ' outside the pattern. */
    public char at(int x, int y, int z) {
        if (y < 0 || y >= layers.size()) return ' ';
        List<String> rows = layers.get(y);
        if (z < 0 || z >= rows.size()) return ' ';
        String row = rows.get(z);
        return x < 0 || x >= row.length() ? ' ' : row.charAt(x);
    }

    private static int[] rotate(int dx, int dz, Direction facing) {
        return switch (facing) {
            case SOUTH -> new int[]{-dx, -dz};
            case EAST -> new int[]{-dz, dx};
            case WEST -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
        };
    }

    public BlockPos toWorld(BlockPos controller, Direction facing, int x, int y, int z) {
        int[] r = rotate(x - cx, z - cz, facing);
        return controller.offset(r[0], y - cy, r[1]);
    }

    public Vec3 effectCenter(BlockPos controller, Direction facing) {
        double dx = ex - cx, dz = ez - cz;
        double rx, rz;
        switch (facing) {
            case SOUTH -> { rx = -dx; rz = -dz; }
            case EAST -> { rx = -dz; rz = dx; }
            case WEST -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        return new Vec3(controller.getX() + 0.5 + rx, controller.getY() + 0.5 + (ey - cy), controller.getZ() + 0.5 + rz);
    }

    public List<Block> blocksFor(char c) {
        if (c == controllerChar) return List.of(BuiltInRegistries.BLOCK.get(controllerBlock));
        List<ResourceLocation> ids = key.get(c);
        if (ids == null) return List.of();
        List<Block> out = new ArrayList<>();
        for (ResourceLocation r : ids) {
            Block b = BuiltInRegistries.BLOCK.get(r);
            if (b != Blocks.AIR) out.add(b);
        }
        return out;
    }

    public boolean matches(char c, BlockState state) {
        if (c == ' ') return true;
        if (c == '.') return state.isAir();
        for (Block b : blocksFor(c)) if (state.is(b)) return true;
        // round 31: a tiered position also takes any block a datapack added to that family (data/<ns>/ts_component_tiers)
        TierSpec ts = tiers.get(c);
        return ts != null && com.theoreticalsciences.custom.ComponentTiers.tierOf(ts.family(), state) >= 0;
    }

    /** One accepted part of a tiered position and the tier it counts as. */
    public record TierOption(int tier, Block block) {}

    /**
     * Round 31: every part a tiered position accepts, lowest tier first - the mod's own (from the multiblock file) and
     * the ones datapacks added. A block a datapack lists counts as the datapack's tier. The port is left out.
     */
    public List<TierOption> tierOptions(char c, Block port) {
        TierSpec ts = tiers.get(c);
        if (ts == null) return List.of();
        List<TierOption> out = new ArrayList<>();
        List<Block> own = blocksFor(c);
        Map<Block, Integer> extra = new LinkedHashMap<>();
        for (Map.Entry<Integer, Block> e : com.theoreticalsciences.custom.ComponentTiers.blocks(ts.family())) extra.putIfAbsent(e.getValue(), e.getKey());
        for (int i = 0; i < own.size(); i++) {
            Block b = own.get(i);
            if (b == port) continue;
            out.add(new TierOption(extra.containsKey(b) ? extra.remove(b) : ts.base() + i, b));
        }
        extra.forEach((b, t) -> {
            if (b != port) out.add(new TierOption(t, b));
        });
        out.sort(Comparator.comparingInt(TierOption::tier));     // (stable: the mod's own come first within a tier)
        return out;
    }

    /** The tier a block counts as at this tiered position (-1: not a part of that family here). */
    public int tierOf(char c, BlockState st) {
        TierSpec ts = tiers.get(c);
        if (ts == null) return -1;
        int dt = com.theoreticalsciences.custom.ComponentTiers.tierOf(ts.family(), st);
        if (dt >= 0) return dt;
        int idx = blocksFor(c).indexOf(st.getBlock());
        return idx >= 0 ? ts.base() + idx : -1;
    }

    /** World positions of every block of the structure except the controller (for start-up / shut-down animations). */
    public List<BlockPos> solidPositions(BlockPos controller, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int y = 0; y < sizeY; y++) {
            List<String> rows = layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == '.' || c == controllerChar) continue;
                    out.add(toWorld(controller, facing, x, y, z));
                }
            }
        }
        return out;
    }

    /** Result of a structure scan. */
    public record ScanResult(List<BlockPos> missing, List<BlockPos> ports, Map<BlockPos, Character> expected, Map<String, Integer> tiers,
                             boolean partial) {
        public ScanResult(List<BlockPos> missing, List<BlockPos> ports, Map<BlockPos, Character> expected, Map<String, Integer> tiers) {
            this(missing, ports, expected, tiers, false);
        }

        public int tier(String family) {
            return tiers.getOrDefault(family, 0);
        }

        public boolean complete() {
            return missing.isEmpty();
        }
    }

    public ScanResult scan(Level level, BlockPos controller, Direction facing, ResourceLocation portBlock) {
        List<BlockPos> missing = new ArrayList<>();
        List<BlockPos> ports = new ArrayList<>();
        Map<BlockPos, Character> expected = new LinkedHashMap<>();
        Map<String, Integer> tierMin = new HashMap<>();
        boolean partial = false;
        for (int y = 0; y < sizeY; y++) {
            List<String> rows = layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == controllerChar) continue;
                    BlockPos p = toWorld(controller, facing, x, y, z);
                    // round 36: a part in a chunk that is not loaded (the controller's chunk kept loaded by a chunk
                    // loader, its neighbour not) is never read - reading it would load that chunk from inside a block
                    // entity's tick, every scan. The scan says "partial" and the machine keeps the state it had.
                    if (!level.isLoaded(p)) {
                        partial = true;
                        continue;
                    }
                    BlockState st = level.getBlockState(p);
                    if (!matches(c, st)) {
                        missing.add(p);
                        expected.put(p, c);
                    } else if (BuiltInRegistries.BLOCK.getKey(st.getBlock()).equals(portBlock)) {
                        ports.add(p);
                    } else if (tiers.containsKey(c)) {
                        int t = tierOf(c, st);          // the mod's own tier, or the one a datapack gave this block
                        if (t >= 0) tierMin.merge(tiers.get(c).family(), t, Math::min);
                    }
                }
            }
        }
        return new ScanResult(missing, ports, expected, tierMin, partial);
    }

    /** Whether an Interface Port is accepted at this character. */
    public boolean allowsPort(char c, ResourceLocation portBlock) {
        List<ResourceLocation> ids = key.get(c);
        return ids != null && ids.contains(portBlock);
    }

    /** The accepted blocks at this character other than the Interface Port (the "real" component, tiers lowest first). */
    public List<Block> componentsFor(char c, Block port) {
        if (tiers.containsKey(c)) {
            // tiered: lowest tier first, datapack-added parts included (round 31)
            List<Block> out = new ArrayList<>();
            for (TierOption o : tierOptions(c, port)) out.add(o.block());
            return out;
        }
        List<Block> out = new ArrayList<>(blocksFor(c));
        out.remove(port);
        return out;
    }

    /** Counts of each block (first accepted variant) needed to build the structure, controller included. */
    public Map<Block, Integer> billOfMaterials() {
        Map<Block, Integer> counts = new LinkedHashMap<>();
        for (List<String> rows : layers)
            for (String row : rows)
                for (char c : row.toCharArray()) {
                    if (c == ' ' || c == '.') continue;
                    List<Block> bs = blocksFor(c);
                    if (!bs.isEmpty()) counts.merge(bs.get(0), 1, Integer::sum);
                }
        return counts;
    }
}
