package com.theoreticalsciences.archaeology;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 1.6.0: a Buried Tell. A low mound that is really a town, rebuilt on its own ruins for thousands of years: layer on
 * layer of trodden soil, each settlement's burnt end a band of ash, the stubs of mud-brick walls standing in every layer.
 * An old trial trench cuts into one flank, its stepped section showing the layers, the survey stakes still round its rim.
 * The finds are where they fell: brush the Suspicious Tell Soil on the floors of the rooms, and the Suspicious Ash in the
 * burnt layers. Most of the tell lies hidden, which is what the Survey Radar is for. Built of the mod's own blocks.
 */
public final class BuriedTell extends Feature<NoneFeatureConfiguration> {
    static final ResourceKey<LootTable> SOIL_FINDS = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("archaeology/tell_soil"));
    static final ResourceKey<LootTable> ASH_FINDS = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("archaeology/ash_layer"));

    public BuriedTell(Codec<NoneFeatureConfiguration> c) {
        super(c);
    }

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel l = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int y = com.theoreticalsciences.deeptime.DeepTimeFeatures.flatGroundPublic(l, o, 8);
        if (y == Integer.MIN_VALUE) return false;
        BlockPos c = new BlockPos(o.getX(), y, o.getZ());                  // the old ground surface under the mound
        if (!l.getBlockState(c).isSolid() || !l.getFluidState(c.above()).isEmpty()) return false;
        BlockState soil = b("tell_soil"), ash = b("ash_layer"), brick = b("ancient_mud_brick"), stake = b("excavation_grid");
        int rx = 9 + r.nextInt(3), rz = 7 + r.nextInt(3), top = 5 + r.nextInt(2);
        int[] ashLevels = {1, 3, 5};                                       // three settlements burnt out, one on another
        // the room plan: one grid of walls, rebuilt on the same lines in every layer
        int cell = 4, gx0 = r.nextInt(cell), gz0 = r.nextInt(cell);
        int trenchSide = r.nextInt(4);                                     // which flank the trial trench cuts
        int finds = 0;
        for (int dx = -rx - 1; dx <= rx + 1; dx++)
            for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                double e = (double) dx * dx / (rx * rx) + (double) dz * dz / (rz * rz);
                if (e > 1.0) continue;
                int h = (int) Math.round(top * Math.sqrt(1 - e) + (r.nextFloat() < 0.15F ? 1 : 0));   // a rounded mound
                BlockPos col = c.offset(dx, 0, dz);
                for (int k = 1; k <= 2; k++) if (!l.getBlockState(col.below(k)).isSolid()) setBlock(l, col.below(k), soil);
                boolean trench = inTrench(trenchSide, dx, dz, rx, rz);
                int cut = trench ? trenchFloor(trenchSide, dx, dz, rx, rz, top) : Integer.MAX_VALUE;
                for (int k = 0; k <= h; k++) {
                    BlockPos p = col.above(k);
                    if (k > cut) { setBlock(l, p, Blocks.AIR.defaultBlockState()); continue; }
                    boolean wall = k < h && (Math.floorMod(dx + gx0, cell) == 0 || Math.floorMod(dz + gz0, cell) == 0) && e < 0.75
                            && r.nextFloat() < 0.8F && !isAsh(ashLevels, k);
                    boolean ashBand = isAsh(ashLevels, k) && k < h;
                    BlockState st = wall ? brick : ashBand ? ash : soil;
                    setBlock(l, p, st);
                    // the finds: on room floors (just above each ash band, where the rebuilt town's floor lay) and in the ash
                    if (k < h - 1 && k >= 1 && !wall) {
                        boolean exposed = trench && k == cut;                  // the trench floor and section show a few
                        float chance = exposed ? 0.25F : ashBand ? 0.035F : isAsh(ashLevels, k - 1) ? 0.05F : 0.004F;
                        if (r.nextFloat() < chance && l.getBlockState(p.below()).isSolid()) {
                            find(l, p, ashBand, r);
                            finds++;
                        }
                    }
                }
                for (int k = h + 1; k <= h + 4; k++) {                      // clear plants and snow off the mound
                    BlockPos p = col.above(k);
                    if (!l.getBlockState(p).isAir() && !l.getBlockState(p).is(net.minecraft.tags.BlockTags.LOGS)) setBlock(l, p, Blocks.AIR.defaultBlockState());
                }
                if (!trench && k0(h) && r.nextFloat() < 0.55F) setBlock(l, col.above(h + 1), Blocks.SHORT_GRASS.defaultBlockState());
            }
        // survey stakes round the trench's rim, and the excavators' cache at its foot
        int[] t = trenchBox(trenchSide, rx, rz);
        for (int dx = t[0] - 1; dx <= t[1] + 1; dx++)
            for (int dz = t[2] - 1; dz <= t[3] + 1; dz++) {
                boolean rim = dx == t[0] - 1 || dx == t[1] + 1 || dz == t[2] - 1 || dz == t[3] + 1;
                if (!rim || (dx + dz) % 2 != 0) continue;
                BlockPos g = surface(l, c.offset(dx, 0, dz));
                if (g != null) setBlock(l, g, stake);
            }
        BlockPos foot = surface(l, c.offset((t[0] + t[1]) / 2, 0, (t[2] + t[3]) / 2));
        if (foot != null) com.theoreticalsciences.dungeon.Caches.place(l, foot, "chests/buried_tell", r);
        // at least a handful of finds in every tell
        for (int i = 0; finds < 8 && i < 40; i++) {
            BlockPos p = c.offset(r.nextInt(2 * rx - 3) - rx + 2, 1 + r.nextInt(3), r.nextInt(2 * rz - 3) - rz + 2);
            if (l.getBlockState(p).is(soil.getBlock()) && l.getBlockState(p.below()).isSolid()) {
                find(l, p, false, r);
                finds++;
            }
        }
        return true;
    }

    private static boolean k0(int h) {
        return h > 0;
    }

    private static boolean isAsh(int[] levels, int k) {
        for (int a : levels) if (a == k) return true;
        return false;
    }

    /** The trial trench: a 4-wide cut from the flank towards the middle. */
    private static int[] trenchBox(int side, int rx, int rz) {
        return switch (side) {
            case 0 -> new int[]{rx / 3, rx, -2, 1};
            case 1 -> new int[]{-rx, -rx / 3, -2, 1};
            case 2 -> new int[]{-2, 1, rz / 3, rz};
            default -> new int[]{-2, 1, -rz, -rz / 3};
        };
    }

    private static boolean inTrench(int side, int dx, int dz, int rx, int rz) {
        int[] t = trenchBox(side, rx, rz);
        return dx >= t[0] && dx <= t[1] && dz >= t[2] && dz <= t[3];
    }

    /** The trench steps down towards the middle of the mound, so its sides show the layers. */
    private static int trenchFloor(int side, int dx, int dz, int rx, int rz, int top) {
        double inward = switch (side) {
            case 0 -> (rx - dx) / (double) rx;
            case 1 -> (rx + dx) / (double) rx;
            case 2 -> (rz - dz) / (double) rz;
            default -> (rz + dz) / (double) rz;
        };
        return Math.max(0, top - 2 - (int) Math.floor(inward * 3));
    }

    private static BlockPos surface(WorldGenLevel l, BlockPos col) {
        for (int k = 10; k >= -3; k--) {
            BlockPos p = col.above(k);
            if (l.getBlockState(p).isAir() && l.getBlockState(p.below()).isSolid()) return p;
        }
        return null;
    }

    private void find(WorldGenLevel l, BlockPos p, boolean ash, RandomSource r) {
        setBlock(l, p, b(ash ? "suspicious_ash_layer" : "suspicious_tell_soil"));
        l.getBlockEntity(p, BlockEntityType.BRUSHABLE_BLOCK).ifPresent(be -> be.setLootTable(ash ? ASH_FINDS : SOIL_FINDS, p.asLong() ^ r.nextLong()));
    }
}
