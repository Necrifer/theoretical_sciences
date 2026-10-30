package com.theoreticalsciences.world.zones;

import com.theoreticalsciences.network.DistortionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Where space is wrong. Structures of higher-dimensional and impossible objects mark the volume they occupy when they
 * generate; a player inside one has the space around them distorted on their screen (see client DistortionRenderer):
 * folded through a tesseract, recursing into a fractal, mirrored through a Platonic solid's symmetry, stretched into
 * an infinite corridor, twisted round a Calabi-Yau, lensed round a horizon.
 * <p>
 * World generation runs off the server thread, so zones are queued and moved into the level's saved data on the next
 * server tick.
 */
public final class StructureZones extends SavedData {
    public enum Mode {NONE, TESSERACT, FRACTAL, KALEIDOSCOPE, INFINITE, TWIST, LENS, HYPERREAL, GLITCH, TRANSCENDENT}

    public record Zone(int x, int y, int z, int r, Mode mode) {}

    private record Pending(ResourceKey<Level> dim, Zone zone) {}

    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private final Map<Long, List<Zone>> byChunk = new HashMap<>();

    /** Called from a feature while it generates. */
    public static void mark(WorldGenLevel level, BlockPos centre, int radius, Mode mode) {
        QUEUE.add(new Pending(level.getLevel().dimension(), new Zone(centre.getX(), centre.getY(), centre.getZ(), Math.max(3, radius), mode)));
    }

    /** For structures built directly on the server thread rather than by world generation. */
    public static void mark(ServerLevel level, BlockPos centre, int radius, Mode mode) {
        get(level).add(new Zone(centre.getX(), centre.getY(), centre.getZ(), Math.max(3, radius), mode));
    }

    public static StructureZones get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(StructureZones::new, StructureZones::load, null), "theoreticalsciences_zones");
    }

    private void add(Zone z) {
        List<Zone> here = byChunk.get(ChunkPos.asLong(z.x >> 4, z.z >> 4));
        if (here != null && here.contains(z)) return;
        int c0x = (z.x - z.r) >> 4, c1x = (z.x + z.r) >> 4, c0z = (z.z - z.r) >> 4, c1z = (z.z + z.r) >> 4;
        for (int cx = c0x; cx <= c1x; cx++)
            for (int cz = c0z; cz <= c1z; cz++) {
                List<Zone> l = byChunk.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>());
                if (!l.contains(z)) l.add(z);
            }
        setDirty();
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        drain(e.getServer());
    }

    public static void drain(net.minecraft.server.MinecraftServer server) {
        Pending p;
        while ((p = QUEUE.poll()) != null) {
            ServerLevel l = server.getLevel(p.dim());
            if (l != null) get(l).add(p.zone());
        }
    }

    public static void reset() {
        QUEUE.clear();
        SENT.clear();
    }

    // ------------------------------------------------------------------ who is inside what
    private static final Map<UUID, int[]> SENT = com.theoreticalsciences.world.SessionMaps.perPlayer(new ConcurrentHashMap<>());   // {mode, intensity*100}

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.tickCount % 5 != 0) return;
        Mode best = Mode.NONE;
        float bi = 0;
        int bx = 0, by = 0, bz = 0, br = 1;
        List<Zone> zs = get(sp.serverLevel()).byChunk.get(ChunkPos.asLong(sp.getBlockX() >> 4, sp.getBlockZ() >> 4));
        if (zs != null)
            for (Zone z : zs) {
                double d = Math.sqrt(sp.distanceToSqr(z.x + 0.5, z.y + 0.5, z.z + 0.5));
                // strongest at the heart, fading out over the outer third
                float i = (float) Math.min(1, Math.max(0, (z.r - d) / (z.r * 0.35)));
                if (i > bi) {
                    bi = i;
                    best = z.mode;
                    bx = z.x;
                    by = z.y;
                    bz = z.z;
                    br = z.r;
                }
            }
        // round 35: a Euclidean Anchor the player carries holds space flat (and spends its charge only while it does)
        if (best != Mode.NONE && com.theoreticalsciences.endgame.EuclideanAnchorItem.holdsFlat(sp)) {
            best = Mode.NONE;
            bi = 0;
        }
        int q = Math.round(bi * 20);
        int[] prev = SENT.get(sp.getUUID());
        if (prev != null && prev[0] == best.ordinal() && prev[1] == q) return;
        SENT.put(sp.getUUID(), new int[]{best.ordinal(), q});
        PacketDistributor.sendToPlayer(sp, new DistortionPayload(best.ordinal(), q / 20F, new BlockPos(bx, by, bz), br));
    }

    public static void forget(UUID id) {
        SENT.remove(id);
    }

    // ------------------------------------------------------------------ persistence
    private static StructureZones load(CompoundTag tag, HolderLookup.Provider regs) {
        StructureZones z = new StructureZones();
        for (Tag t : tag.getList("Zones", Tag.TAG_INT_ARRAY)) {
            int[] a = ((net.minecraft.nbt.IntArrayTag) t).getAsIntArray();
            if (a.length == 5 && a[4] >= 0 && a[4] < Mode.values().length) z.add(new Zone(a[0], a[1], a[2], a[3], Mode.values()[a[4]]));
        }
        return z;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider regs) {
        Set<Zone> all = new LinkedHashSet<>();
        byChunk.values().forEach(all::addAll);
        ListTag l = new ListTag();
        for (Zone z : all) l.add(new net.minecraft.nbt.IntArrayTag(new int[]{z.x, z.y, z.z, z.r, z.mode.ordinal()}));
        tag.put("Zones", l);
        return tag;
    }
}
