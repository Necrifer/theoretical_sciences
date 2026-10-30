package com.theoreticalsciences.dungeon;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned containment vault, half sunk into the ground: cracked walls of the local stone, a fallen-in roof,
 * scattered wreckage of machinery - and in the middle, still running, a Breached Anomaly Engine that keeps pulling
 * the dimension's creatures into being. Supply chests sit in the corners. Built from whatever the dimension is made of.
 */
public class AnomalyVaultFeature extends Feature<NoneFeatureConfiguration> {
    public AnomalyVaultFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** wall, floor, casing, glass, lamp, local loot table */
    private record Palette(String wall, String floor, String casing, String glass, String lamp, String loot) {}

    private static Palette palette(ResourceKey<Level> d) {
        if (d == QHDimensions.BULK) return new Palette("void_bricks", "bulk_stone", "hyperframe_casing", "containment_field_glass", "dilithium_lamp", "derelict_station");
        if (d == QHDimensions.PLATONIC) return new Palette("axiom_stone", "axiom_stone", "fractal_lattice_block", "golden_ratio_glass", "koch_crystal", "euclids_vault");
        if (d == QHDimensions.FOAM) return new Palette("foam_substrate", "foam_substrate", "planck_casing", "containment_field_glass", "cherenkov_lamp", "anomaly_vault");
        if (d == QHDimensions.TRANSFINITE) return new Palette("hilbert_brick", "ordinal_stone", "transfinite_casing", "aleph_glass", "cherenkov_lamp", "hilbert_hotel");
        if (d == QHDimensions.BLACK_HOLE) return new Palette("infall_rock", "infall_rock", "event_horizon_casing", "horizon_glass", "stellar_lamp", "anomaly_vault");
        if (d == QHDimensions.WHITE_HOLE) return new Palette("ejecta_rock", "ejecta_rock", "stellar_frame_casing", "horizon_glass", "stellar_lamp", "anomaly_vault");
        if (d == QHDimensions.HYPERREAL) return new Palette("arbor_planks", "arbor_planks", "hyperframe_casing", "containment_field_glass", "computronium_lamp", "anomaly_vault");
        if (d == QHDimensions.TRANSCENDENTAL) return new Palette("transcendent_stone", "transcendent_stone", "omnipotence_casing", "transcendent_glass", "computronium_lamp", "anomaly_vault");
        if (d == QHDimensions.ANTIVERSE) return new Palette("antistone_bricks", "antistone", "magnetic_confinement_casing", "containment_field_glass", "cherenkov_lamp", "antiverse_vault");
        return new Palette("neutronium_bricks", "neutronium_tiles", "spacetime_casing", "containment_field_glass", "cherenkov_lamp", "observatory_ruin");
    }

    private static BlockState b(String id, BlockState fallback) {
        try {
            var block = QHBlocks.get(id);
            return block == null ? fallback : block.defaultBlockState();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX(), o.getZ());
        if (ground <= level.getMinBuildHeight() + 8 || ground >= level.getMaxBuildHeight() - 12) return false;
        // needs solid ground under most of it
        int solid = 0;
        for (int dx = -5; dx <= 5; dx += 5)
            for (int dz = -5; dz <= 5; dz += 5) {
                int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, o.getX() + dx, o.getZ() + dz);
                if (Math.abs(h - ground) <= 4) solid++;
            }
        if (solid < 7) return false;
        Palette pal = palette(level.getLevel().dimension());
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState wall = b(pal.wall(), stone), floor = b(pal.floor(), stone), casing = b(pal.casing(), stone), glass = b(pal.glass(), Blocks.GLASS.defaultBlockState()),
                lamp = b(pal.lamp(), Blocks.SEA_LANTERN.defaultBlockState()), coil = b("superconducting_coil_block", casing), air = Blocks.AIR.defaultBlockState();
        BlockPos base = new BlockPos(o.getX(), ground - 2, o.getZ());     // half sunk into the ground
        int R = 5, H = 6;
        Direction door = Direction.from2DDataValue(r.nextInt(4));
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++) {
                boolean edge = Math.abs(dx) == R || Math.abs(dz) == R;
                boolean corner = Math.abs(dx) == R && Math.abs(dz) == R;
                // a foundation under the floor, so it never hangs in the air
                for (int d = 1; d <= 4; d++) {
                    BlockPos p = base.offset(dx, -d, dz);
                    if (!level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) break;
                    setBlock(level, p, wall);
                }
                setBlock(level, base.offset(dx, 0, dz), r.nextInt(9) == 0 ? casing : floor);
                for (int y = 1; y <= H; y++) {
                    BlockPos p = base.offset(dx, y, dz);
                    if (edge) {
                        boolean doorway = isDoor(door, dx, dz, R) && y <= 3;
                        boolean ruined = y > 2 + r.nextInt(5) && !corner;     // walls crumble toward the top
                        if (doorway || ruined) setBlock(level, p, air);
                        else setBlock(level, p, corner ? casing : y == 3 && (dx + dz) % 3 == 0 ? glass : wall);
                    } else if (y == H) {
                        setBlock(level, p, r.nextInt(5) < 2 ? air : wall);    // the roof has fallen in
                    } else setBlock(level, p, air);
                }
            }
        // pillars, lamps, wreckage of machinery along the walls
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int y = 1; y < H; y++) setBlock(level, base.offset(sx * 3, y, sz * 3), y == 3 ? lamp : casing);
            }
        for (int i = 0; i < 7; i++) {
            int dx = r.nextInt(2 * R - 1) - (R - 1), dz = r.nextInt(2 * R - 1) - (R - 1);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
            BlockState junk = switch (r.nextInt(4)) {
                case 0 -> coil;
                case 1 -> glass;
                default -> casing;
            };
            setBlock(level, base.offset(dx, 1, dz), junk);
            if (r.nextBoolean()) setBlock(level, base.offset(dx, 2, dz), junk);
        }
        // the engine on its plinth, sometimes a second one
        setBlock(level, base.offset(0, 1, 0), casing);
        setBlock(level, base.offset(0, 2, 0), b("breached_anomaly_engine", casing));
        if (r.nextInt(3) == 0) {
            int sx = r.nextBoolean() ? 2 : -2, sz = r.nextBoolean() ? 2 : -2;
            setBlock(level, base.offset(sx, 1, sz), b("breached_anomaly_engine", casing));
        }
        // supplies in two corners
        chest(level, r, base.offset(-(R - 1), 1, -(R - 1)), pal.loot());
        chest(level, r, base.offset(R - 1, 1, R - 1), "anomaly_vault");
        com.theoreticalsciences.world.zones.StructureZones.mark(level, base.above(3), R + 4, com.theoreticalsciences.world.zones.StructureZones.Mode.GLITCH);
        return true;
    }

    private static boolean isDoor(Direction d, int dx, int dz, int R) {
        return switch (d) {
            case NORTH -> dz == -R && Math.abs(dx) <= 1;
            case SOUTH -> dz == R && Math.abs(dx) <= 1;
            case WEST -> dx == -R && Math.abs(dz) <= 1;
            default -> dx == R && Math.abs(dz) <= 1;
        };
    }

    private void chest(WorldGenLevel level, RandomSource r, BlockPos p, String loot) {
        Caches.place(level, p, "chests/" + loot, r);
    }
}
