package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineBlock;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Arrays;
import java.util.List;

/**
 * A derelict research station somebody else built in the Bulk long ago: a holed casing deck, broken walls of glass and
 * hyperframe, a salvageable machine or two still standing, and a supply locker.
 */
public class DerelictStationFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/derelict_station"));

    public DerelictStationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (y <= level.getMinBuildHeight() + 5 || y > 200) return false;
        BlockPos base = new BlockPos(o.getX(), y, o.getZ());
        BlockState deck = QHBlocks.get("spacetime_casing").defaultBlockState();
        BlockState frame = QHBlocks.get("hyperframe_casing").defaultBlockState();
        BlockState glass = QHBlocks.get("containment_field_glass").defaultBlockState();
        BlockState stone = QHBlocks.get("bulk_stone").defaultBlockState();
        int h = 5;
        for (int dx = -h; dx <= h; dx++)
            for (int dz = -h; dz <= h; dz++) {
                // supports down to the ground, then the deck
                for (int d = 1; d <= 6; d++) {
                    BlockPos p = base.offset(dx, -d, dz);
                    if (!level.isEmptyBlock(p)) break;
                    if ((Math.abs(dx) == h && Math.abs(dz) == h) || r.nextInt(8) == 0) setBlock(level, p, stone);
                }
                if (r.nextInt(7) != 0) setBlock(level, base.offset(dx, 0, dz), deck);
                boolean wall = Math.abs(dx) == h || Math.abs(dz) == h;
                if (wall) {
                    int wh = r.nextInt(4);
                    for (int k = 1; k <= wh; k++) {
                        boolean pillar = Math.abs(dx) == h && Math.abs(dz) == h || (dx + dz) % 4 == 0;
                        setBlock(level, base.offset(dx, k, dz), pillar ? frame : glass);
                    }
                } else {
                    for (int k = 1; k <= 3; k++) setBlock(level, base.offset(dx, k, dz), Blocks.AIR.defaultBlockState());
                }
                if (!wall && r.nextInt(5) == 0) setBlock(level, base.offset(dx, 4, dz), frame);    // what is left of the roof
            }
        // salvage: one or two single-block machines that survived
        List<MachineType> singles = Arrays.stream(MachineType.values()).filter(t -> !t.isMultiblock()
                && t != MachineType.CANTOR_DIAGONALIZER && t != MachineType.AXIOM_DISTILLER).toList();
        int n = 1 + r.nextInt(2);
        for (int i = 0; i < n; i++) {
            MachineType t = singles.get(r.nextInt(Math.min(singles.size(), 22)));
            BlockState m = QHBlocks.MACHINES.get(t).get().defaultBlockState().setValue(MachineBlock.FACING, Direction.from2DDataValue(r.nextInt(4)));
            setBlock(level, base.offset(-2 + i * 4, 1, -2), m);
        }
        BlockPos chest = base.offset(0, 1, 2);
        com.theoreticalsciences.dungeon.Caches.place(level, chest, LOOT, r);
        setBlock(level, base.offset(0, 1, 0), QHBlocks.INTERFACE_PORT.get().defaultBlockState());
        setBlock(level, base.offset(2, 1, 2), QHBlocks.get("breached_anomaly_engine").defaultBlockState());
        return true;
    }
}
