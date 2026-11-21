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
 * <p>
 * 1.3.5: {@code "repeat": {"from": a, "to": b, "max": n}} makes the structure variable in height, the way GTNH's
 * Distillation Tower is: layers a..b form one unit that may be stacked up to n more times (so the machine is built
 * with 1 to n+1 units). The definition itself is the smallest build; {@link #variant(int)} gives the taller ones.
 */
public final class MultiblockDefinition {
    public final String id;
    public final ResourceLocation controllerBlock;
    public final List<List<String>> layers;
    public final Map<Character, List<ResourceLocation>> key;
    public final char controllerChar;
    public final int cx, cy, cz;
    public final double ex, ey, ez;
    /** 1.3.8: {@code effect_scale} - the effect drawn bigger, for the giant machines (1 = standard). */
    public final double escale;
    /**
     * 1.2.0: {@code animation} - a structure animation ({@code assets/<ns>/ts_animations/<path>.json}) drawn while the
     * structure is formed and set moving while it runs. Null for none. See docs/ANIMATIONS.md.
     */
    public final ResourceLocation animation;

    /**
     * 1.2.0: {@code moving} - the structure's own moving parts. Each names key characters whose blocks move; while
     * the structure is formed those blocks are hidden in the world (their {@code moving} blockstate draws nothing) and
     * the renderer draws them instead, turning or sliding while the machine runs. The blocks stay real and in place, so
     * nothing has to be rebuilt. See docs/ANIMATIONS.md.
     *
     * @param chars   the key characters whose blocks move
     * @param split   "none" (one part), "layers" (each layer its own part) or "components" (each connected piece)
     * @param pivot   "centre" (the effect centre), "self" (each part's own middle) or [x, y, z] in pattern coordinates
     * @param layers  pattern layers allowed (of the smallest build), empty for all
     * @param alternate each further part (of a split) turns the other way
     * @param stagger each further part starts this much later in its swing (0 to 1)
     */
    public record MovingSpec(String id, String chars, String split, JsonElement pivot, JsonArray motion, String parent, float ramp,
                             boolean alternate, float stagger, Set<Integer> layers) {}

    public final List<MovingSpec> moving;
    /** 1.2.0: {@code fx} - working effects drawn from data (glowing coils, energy flowing in, beams, a mini universe...). */
    public final JsonArray fx;
    /** For a taller build (repeat): the layer of the smallest build each layer copies (null: the same layer). */
    int[] baseY;
    private Set<Long> movingCells;
    public final int sizeX, sizeY, sizeZ;
    /**
     * GT-style tiered components: a key character whose accepted blocks are listed lowest tier first.
     * {@code "tiers": {"K": {"family": "coil", "base": 1}}} - the structure's tier in a family is the LOWEST tier found.
     * Datapacks can add more parts to a family (see {@link com.theoreticalsciences.custom.ComponentTiers}).
     */
    public final Map<Character, TierSpec> tiers;

    public record TierSpec(String family, int base) {}

    /** Variable height: the repeated unit (pattern layers {@code repeatFrom..repeatTo}) and how many extra copies are allowed. */
    public final int repeatFrom, repeatTo, repeatMax;
    private final JsonObject source;
    private final MultiblockDefinition[] variants;
    /** 1.3.6: earlier layouts of a reworked structure (same key), so the Structure Assembler can still take old builds apart. */
    public final List<MultiblockDefinition> legacy;

    private MultiblockDefinition(String id, JsonObject json) {
        this.id = id;
        this.source = json;
        if (json.has("repeat")) {
            JsonObject r = json.getAsJsonObject("repeat");
            this.repeatFrom = r.get("from").getAsInt();
            this.repeatTo = r.get("to").getAsInt();
            this.repeatMax = r.get("max").getAsInt();
        } else {
            this.repeatFrom = this.repeatTo = -1;
            this.repeatMax = 0;
        }
        this.variants = new MultiblockDefinition[repeatMax + 1];
        List<MultiblockDefinition> leg = new ArrayList<>();
        if (json.has("legacy")) for (JsonElement l : json.getAsJsonArray("legacy")) {
            JsonObject j = json.deepCopy();
            j.remove("legacy");
            j.remove("repeat");
            j.remove("effect_center");
            j.add("layers", l.getAsJsonArray());
            try {
                leg.add(new MultiblockDefinition(id, j));
            } catch (RuntimeException ignored) {          // a legacy layout without a controller: nothing to align it to
            }
        }
        this.legacy = List.copyOf(leg);
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
        this.escale = json.has("effect_scale") ? json.get("effect_scale").getAsDouble() : 1.0;
        this.animation = json.has("animation") ? ResourceLocation.tryParse(json.get("animation").getAsString()) : null;
        List<MovingSpec> mv = new ArrayList<>();
        if (json.has("moving")) for (JsonElement e : json.getAsJsonArray("moving")) {
            JsonObject o = e.getAsJsonObject();
            Set<Integer> ly = new HashSet<>();
            if (o.has("layers")) for (JsonElement l : o.getAsJsonArray("layers")) ly.add(l.getAsInt());
            mv.add(new MovingSpec(o.has("id") ? o.get("id").getAsString() : "part" + mv.size(), o.get("chars").getAsString(),
                    o.has("split") ? o.get("split").getAsString() : "none", o.has("pivot") ? o.get("pivot") : null,
                    o.has("motion") ? o.getAsJsonArray("motion") : new JsonArray(), o.has("parent") ? o.get("parent").getAsString() : null,
                    o.has("ramp_seconds") ? o.get("ramp_seconds").getAsFloat() : 3F, o.has("alternate") && o.get("alternate").getAsBoolean(),
                    o.has("stagger") ? o.get("stagger").getAsFloat() : 0F, Set.copyOf(ly)));
        }
        this.moving = List.copyOf(mv);
        this.fx = json.has("fx") ? json.getAsJsonArray("fx") : new JsonArray();
    }

    /** The layer of the smallest build that layer y of this one copies. */
    public int baseLayer(int y) {
        return baseY == null || y < 0 || y >= baseY.length ? y : baseY[y];
    }

    /** Whether the cell (pattern coordinates) holds a block of spec s. */
    public boolean inMoving(MovingSpec s, int x, int y, int z) {
        if (y < 0 || y >= layers.size()) return false;
        List<String> rows = layers.get(y);
        if (z < 0 || z >= rows.size() || x < 0 || x >= rows.get(z).length()) return false;
        char c = rows.get(z).charAt(x);
        return c != ' ' && c != '.' && c != controllerChar && s.chars().indexOf(c) >= 0 && (s.layers().isEmpty() || s.layers().contains(baseLayer(y)));
    }

    private List<List<int[]>> specCells;

    /** The cells (pattern coordinates) of each moving spec, in order (worked out once). */
    public synchronized List<List<int[]>> movingCellsBySpec() {
        if (specCells != null) return specCells;
        List<List<int[]>> out = new ArrayList<>();
        for (MovingSpec s : moving) {
            List<int[]> cs = new ArrayList<>();
            for (int y = 0; y < layers.size(); y++)
                for (int z = 0; z < layers.get(y).size(); z++)
                    for (int x = 0; x < layers.get(y).get(z).length(); x++)
                        if (inMoving(s, x, y, z)) cs.add(new int[]{x, y, z});
            out.add(List.copyOf(cs));
        }
        specCells = List.copyOf(out);
        return specCells;
    }

    /** Every cell (pattern coordinates, packed as BlockPos longs) of a moving part. */
    public synchronized Set<Long> movingCells() {
        if (movingCells != null) return movingCells;
        Set<Long> out = new HashSet<>();
        if (!moving.isEmpty())
            for (int y = 0; y < layers.size(); y++)
                for (int z = 0; z < layers.get(y).size(); z++)
                    for (int x = 0; x < layers.get(y).get(z).length(); x++)
                        for (MovingSpec s : moving) if (inMoving(s, x, y, z)) { out.add(BlockPos.asLong(x, y, z)); break; }
        movingCells = Collections.unmodifiableSet(out);
        return movingCells;
    }

    static MultiblockDefinition parse(String id, JsonObject json) {
        return new MultiblockDefinition(id, json);
    }

    /** Whether the structure can be built taller (see the class comment). */
    public boolean variable() {
        return repeatMax > 0;
    }

    /** Units in a build with {@code extra} extra copies of the repeated unit: 1 for the smallest. */
    public int units(int extra) {
        return 1 + Math.max(0, Math.min(extra, repeatMax));
    }

    /** The build with {@code extra} more copies of the repeated unit (0 = this, the smallest). */
    public synchronized MultiblockDefinition variant(int extra) {
        extra = Math.max(0, Math.min(extra, repeatMax));
        if (extra == 0) return this;
        if (variants[extra] == null) {
            JsonObject j = source.deepCopy();
            j.remove("repeat");
            JsonArray src = source.getAsJsonArray("layers"), out = new JsonArray();
            for (int y = 0; y < src.size(); y++) {
                out.add(src.get(y).deepCopy());
                if (y == repeatTo)
                    for (int k = 0; k < extra; k++)
                        for (int r = repeatFrom; r <= repeatTo; r++) out.add(src.get(r).deepCopy());
            }
            j.add("layers", out);
            int unit = repeatTo - repeatFrom + 1;
            JsonArray ec = new JsonArray();
            ec.add(ex);
            ec.add(ey + extra * unit / 2.0);
            ec.add(ez);
            j.add("effect_center", ec);
            variants[extra] = new MultiblockDefinition(id, j);
            int[] by = new int[out.size()];
            int n = 0;
            for (int y = 0; y < src.size(); y++) {
                by[n++] = y;
                if (y == repeatTo)
                    for (int k = 0; k < extra; k++)
                        for (int r = repeatFrom; r <= repeatTo; r++) by[n++] = r;
            }
            variants[extra].baseY = by;
        }
        return variants[extra];
    }

    /** A cheap yes/no check that stops at the first wrong block (top layers first); unloaded chunks count as no. */
    public boolean quickMatch(Level level, BlockPos controller, Direction facing) {
        for (int y = sizeY - 1; y >= 0; y--) {
            List<String> rows = layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == controllerChar) continue;
                    BlockPos p = toWorld(controller, facing, x, y, z);
                    if (!level.isLoaded(p) || !matches(c, level.getBlockState(p))) return false;
                }
            }
        }
        return true;
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
            if (r.equals(INTERFACE_PORT)) out.add(com.theoreticalsciences.registry.QHBlocks.MANA_PORT.get());     // 1.7.4: a Mana Port fits wherever an Interface Port does
        }
        return out;
    }

    private static final ResourceLocation INTERFACE_PORT = ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "interface_port");

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
                    } else if (BuiltInRegistries.BLOCK.getKey(st.getBlock()).equals(portBlock) || st.getBlock() instanceof com.theoreticalsciences.machine.ManaPortBlock) {
                        ports.add(p);                  // (a Mana Port is one of the structure's ports: its recipes draw on it)
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
