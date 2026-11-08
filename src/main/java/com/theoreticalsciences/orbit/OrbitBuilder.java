package com.theoreticalsciences.orbit;

import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.3.0: puts the Orbital Station together the first time a crew arrives (it is one object, not terrain).
 * <ul>
 *   <li>A pressurised hub, 15 blocks square and 9 high, of Station Hull with leaded-glass windows and a glass cupola
 *   looking up at the stars; Cherenkov lamps in the ceiling.</li>
 *   <li>Four Transfer Consoles, one on each wall, that send you on (see {@link OrbitTravel}).</li>
 *   <li>Four trusses of Service Tower lattice out to wide Solar Array wings, and a docking port of Rocket Hull on the
 *   south side where the crew capsule berths.</li>
 * </ul>
 * Inside the hub the air holds; outside it is vacuum (see {@link OrbitHazards}).
 */
public final class OrbitBuilder {
    public static final int Y = 100;
    /** Where a crew comes aboard: inside the hub, by the docking port. */
    public static final BlockPos ARRIVAL = new BlockPos(0, Y + 1, 4);
    /** The pressurised volume (the hub's inside). */
    public static final AABB HULL = new AABB(-6, Y + 1, -6, 7, Y + 8, 7);

    public static void ensureBuilt(ServerLevel level) {
        OrbitData d = OrbitData.get(level.getServer());
        if (d.built) return;
        build(level);
        d.built = true;
        d.setDirty();
    }

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    static void build(ServerLevel l) {
        BlockState hull = b("station_hull"), glass = b("leaded_glass"), lamp = b("cherenkov_lamp"), truss = b("service_tower"),
                solar = b("solar_array"), port = b("rocket_hull"), console = b("orbital_transfer_console"), air = Blocks.AIR.defaultBlockState();
        // the hub: a hollow box from y Y to Y+8
        for (int x = -7; x <= 7; x++)
            for (int z = -7; z <= 7; z++)
                for (int y = Y; y <= Y + 8; y++) {
                    boolean wall = Math.abs(x) == 7 || Math.abs(z) == 7, floor = y == Y, roof = y == Y + 8;
                    BlockPos p = new BlockPos(x, y, z);
                    if (roof && Math.abs(x) <= 2 && Math.abs(z) <= 2) l.setBlock(p, glass, 2);                  // the cupola
                    else if (roof && (Math.abs(x) == 5 && Math.abs(z) == 5)) l.setBlock(p, lamp, 2);
                    else if (floor || roof) l.setBlock(p, hull, 2);
                    else if (wall) {
                        boolean window = y >= Y + 3 && y <= Y + 5 && (Math.abs(x) == 7 ? Math.abs(z) : Math.abs(x)) % 3 != 0 && Math.max(Math.abs(x), Math.abs(z)) == 7
                                && Math.min(Math.abs(x), Math.abs(z)) < 6;
                        l.setBlock(p, window ? glass : hull, 2);
                    } else l.setBlock(p, air, 2);
                }
        // a Transfer Console against each wall
        l.setBlock(new BlockPos(0, Y + 1, -6), console, 2);
        l.setBlock(new BlockPos(6, Y + 1, 0), console, 2);
        l.setBlock(new BlockPos(-6, Y + 1, 0), console, 2);
        l.setBlock(new BlockPos(3, Y + 1, 6), console, 2);
        // trusses out to the solar wings, east and west; shorter ones north and south
        for (Direction d : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH}) {
            int len = d.getAxis() == Direction.Axis.X ? 22 : 12;
            for (int k = 8; k <= len; k++) l.setBlock(new BlockPos(d.getStepX() * k, Y + 4, d.getStepZ() * k), truss, 2);
            if (d.getAxis() == Direction.Axis.X)
                for (int k = 12; k <= len + 6; k++)
                    for (int w = -4; w <= 4; w++)
                        if (k != len + 1 || w == 0) l.setBlock(new BlockPos(d.getStepX() * k, Y + 4, w), solar, 2);
        }
        // the docking port: a short tube of Rocket Hull on the south wall, open into the hub
        for (int k = 8; k <= 11; k++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = 2; dy <= 4; dy++) {
                    boolean edge = Math.abs(dx) == 1 || dy == 2 || dy == 4;
                    l.setBlock(new BlockPos(dx, Y + dy, k), edge ? port : (k == 11 ? port : air), 2);
                }
        l.setBlock(new BlockPos(0, Y + 3, 7), air, 2);
    }

    public static boolean inside(BlockPos feet) {
        return HULL.contains(feet.getX() + 0.5, feet.getY() + 0.1, feet.getZ() + 0.5);
    }

    private OrbitBuilder() {}
}
