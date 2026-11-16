package com.theoreticalsciences.home;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.9.0: what stands in Omnipotency. A Wayside Shrine, a Hamlet round a well, a Choir Hall among the crystal spires,
 * the Temple of Remembrance, and the Beacon of Return over the Sea of Clouds. Every one is built of the dimension's
 * own blocks, sits on a footing carried down to the ground under it, and is laid out a little differently each time.
 */
public final class HomeStructures {
    private HomeStructures() {}

    /** What every one of them builds with. */
    static final class Site {
        final WorldGenLevel level;
        final RandomSource rnd;
        final BlockPos o;                 // the middle of the floor
        final BlockState marble = b("empyrean_marble"), brick = b("empyrean_marble_bricks"), tile = b("empyrean_marble_tiles"), trim = b("seraph_gold_trim"),
                gold = b("seraph_gold_block"), glass = b("halo_glass"), lantern = b("glory_lantern"), crystal = b("choir_crystal_block"), cloud = b("cloudstone"),
                pillar = b("empyrean_pillar").setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y),
                urn = HomeRegistry.URN.get().defaultBlockState(), bell = HomeRegistry.BELL.get().defaultBlockState(), stone = HomeRegistry.STONE.get().defaultBlockState();

        Site(WorldGenLevel level, RandomSource rnd, BlockPos o) {
            this.level = level;
            this.rnd = rnd;
            this.o = o;
        }

        static BlockState b(String id) {
            return QHBlocks.get(id).defaultBlockState();
        }

        void set(int x, int y, int z, BlockState s) {
            level.setBlock(o.offset(x, y, z), s, 2);
        }

        void air(int x, int y, int z) {
            level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = x0; x <= x1; x++)
                for (int y = y0; y <= y1; y++)
                    for (int z = z0; z <= z1; z++) set(x, y, z, s);
        }

        void column(int x, int z, int y0, int y1, BlockState s) {
            for (int y = y0; y <= y1; y++) set(x, y, z, s);
        }

        /** Marble under a floor cell, down to whatever is there (ten at most: an island's edge is not built out over). */
        void footing(int x, int z) {
            for (int y = -1; y >= -10; y--) {
                BlockPos p = o.offset(x, y, z);
                if (level.getBlockState(p).isSolidRender(level, p)) return;
                level.setBlock(p, marble, 2);
            }
        }

        /** A floor of {@code top} with a footing, and the air above it cleared. */
        void floor(int x0, int z0, int x1, int z1, int clear, BlockState top) {
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    footing(x, z);
                    set(x, 0, z, top);
                    for (int y = 1; y <= clear; y++) air(x, y, z);
                }
        }

        <T extends Mob> void spawn(EntityType<T> type, double x, int y, double z, float yaw) {
            T mob = type.create(level.getLevel());
            if (mob == null) return;
            mob.moveTo(o.getX() + x + 0.5, o.getY() + y, o.getZ() + z + 0.5, yaw, 0);
            mob.yBodyRot = yaw;
            mob.yHeadRot = yaw;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
        }
    }

    /**
     * Where the floor goes: the ground must be there under the whole footprint, reasonably level, and not a tree or a
     * spire. Returns the floor's middle, or null.
     */
    static BlockPos site(WorldGenLevel level, BlockPos origin, int r) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx += Math.max(1, r / 2))
            for (int dz = -r; dz <= r; dz += Math.max(1, r / 2)) {
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y < 24 || y > 236) return null;
                BlockState g = level.getBlockState(new BlockPos(x, y, z));
                if (!HomeMobs.isBlock(g, "halo_grass", "gilded_marble", "cloudstone", "empyrean_marble", "heaven_soil")) return null;
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        if (hi - lo > 5) return null;
        return new BlockPos(origin.getX(), hi, origin.getZ());
    }

    abstract static class Base extends Feature<NoneFeatureConfiguration> {
        private final int radius;

        Base(Codec<NoneFeatureConfiguration> codec, int radius) {
            super(codec);
            this.radius = radius;
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            BlockPos at = site(ctx.level(), ctx.origin(), radius);
            if (at == null) return false;
            build(new Site(ctx.level(), ctx.random(), at));
            return true;
        }

        abstract void build(Site s);
    }

    // ================================================================== Wayside Shrine
    /** Four pillars, a roof, a light under it, and something left there: an urn, or a bell to ring in passing. */
    public static class Shrine extends Base {
        public Shrine(Codec<NoneFeatureConfiguration> codec) {
            super(codec, 3);
        }

        @Override
        void build(Site s) {
            s.floor(-3, -3, 3, 3, 7, s.tile);
            for (int i = -3; i <= 3; i++) {
                s.set(i, 0, -3, s.trim);
                s.set(i, 0, 3, s.trim);
                s.set(-3, 0, i, s.trim);
                s.set(3, 0, i, s.trim);
            }
            int h = 3 + s.rnd.nextInt(2);
            for (int sx = -2; sx <= 2; sx += 4)
                for (int sz = -2; sz <= 2; sz += 4) {
                    s.set(sx, 1, sz, s.brick);
                    s.column(sx, sz, 2, h, s.pillar);
                }
            s.fill(-3, h + 1, -3, 3, h + 1, 3, s.trim);
            s.fill(-2, h + 1, -2, 2, h + 1, 2, s.brick);
            s.fill(-2, h + 2, -2, 2, h + 2, 2, s.brick);
            s.fill(-1, h + 3, -1, 1, h + 3, 1, s.trim);
            s.set(0, h + 4, 0, s.gold);
            s.set(0, h, 0, s.lantern);                     // the light, hung under the roof
            s.set(0, 1, 0, s.brick);
            s.set(0, 2, 0, s.rnd.nextBoolean() ? s.urn : s.bell);
        }
    }

    // ================================================================== Hamlet
    /** Three or four small houses facing a well, with a Warden kneeling beside it and those who live there about. */
    public static class Hamlet extends Base {
        public Hamlet(Codec<NoneFeatureConfiguration> codec) {
            super(codec, 12);
        }

        @Override
        void build(Site s) {
            // the well
            s.floor(-2, -2, 2, 2, 6, s.tile);
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++) s.set(x, 1, z, x == 0 && z == 0 ? s.glass : s.brick);
            s.set(0, 0, 0, s.lantern);                     // light coming up through the water
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2) s.column(sx, sz, 2, 3, s.pillar);
            s.fill(-1, 4, -1, 1, 4, 1, s.trim);
            s.set(0, 5, 0, s.gold);
            s.set(0, 3, 0, s.bell);
            s.spawn(HomeRegistry.WARDEN.get(), 2, 1, 2, 180F);
            int skip = s.rnd.nextInt(4), n = 0;
            Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int i = 0; i < 4; i++) {
                if (i == skip && s.rnd.nextBoolean()) continue;
                Direction d = dirs[i];
                int cx = d.getStepX() * 9, cz = d.getStepZ() * 9;
                house(s, cx, cz, d.getOpposite(), n++);
                for (int k = 3; k <= 5; k++) {                 // the path from the well to the door
                    s.footing(d.getStepX() * k, d.getStepZ() * k);
                    s.set(d.getStepX() * k, 0, d.getStepZ() * k, s.tile);
                    s.air(d.getStepX() * k, 1, d.getStepZ() * k);
                    s.air(d.getStepX() * k, 2, d.getStepZ() * k);
                }
            }
        }

        /** A house seven by seven, its door towards {@code door}. No two in a hamlet have quite the same roof. */
        private void house(Site s, int cx, int cz, Direction door, int n) {
            s.floor(cx - 3, cz - 3, cx + 3, cz + 3, 8, s.tile);
            for (int x = -3; x <= 3; x++)
                for (int z = -3; z <= 3; z++) {
                    boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3, corner = Math.abs(x) == 3 && Math.abs(z) == 3;
                    if (!edge) continue;
                    for (int y = 1; y <= 3; y++) {
                        boolean window = y == 2 && !corner && (x == 0 || z == 0 || Math.abs(x) == 1 && n % 2 == 1 || Math.abs(z) == 1 && n % 2 == 1);
                        s.set(cx + x, y, cz + z, corner ? s.pillar : window ? s.glass : s.brick);
                    }
                }
            int dx = door.getStepX() * 3, dz = door.getStepZ() * 3;
            s.air(cx + dx, 1, cz + dz);
            s.air(cx + dx, 2, cz + dz);
            s.set(cx + dx, 3, cz + dz, s.trim);
            // the roof: stepped, and capped differently from its neighbours
            s.fill(cx - 4, 4, cz - 4, cx + 4, 4, cz + 4, s.trim);
            s.fill(cx - 3, 4, cz - 3, cx + 3, 4, cz + 3, s.brick);
            s.fill(cx - 2, 5, cz - 2, cx + 2, 5, cz + 2, n % 3 == 0 ? s.tile : s.brick);
            s.fill(cx - 1, 6, cz - 1, cx + 1, 6, cz + 1, n % 3 == 1 ? s.gold : s.trim);
            if (n % 3 != 1) s.set(cx, 7, cz, n % 3 == 0 ? s.lantern : s.gold);
            // inside: a light, a bed of cloud, and perhaps something put by
            s.set(cx - door.getStepX() * 2 + door.getClockWise().getStepX() * 2, 1, cz - door.getStepZ() * 2 + door.getClockWise().getStepZ() * 2, s.lantern);
            int bx = cx - door.getStepX() * 2 - door.getClockWise().getStepX() * 2, bz = cz - door.getStepZ() * 2 - door.getClockWise().getStepZ() * 2;
            s.set(bx, 1, bz, s.cloud);
            s.set(bx + door.getStepX(), 1, bz + door.getStepZ(), s.cloud);
            if (s.rnd.nextInt(3) != 0) s.set(cx - door.getStepX() * 2, 1, cz - door.getStepZ() * 2, s.urn);
            s.spawn(HomeRegistry.DWELLER.get(), cx, 1, cz, s.rnd.nextFloat() * 360F);
            if (s.rnd.nextBoolean()) s.spawn(HomeRegistry.DWELLER.get(), cx + door.getStepX() * 5, 1, cz + door.getStepZ() * 5, s.rnd.nextFloat() * 360F);
        }
    }

    // ================================================================== Choir Hall
    /** A ring of twelve pillars under a stone circle, open to the sky, round a great crystal. Bells hang from the circle. */
    public static class ChoirHall extends Base {
        public ChoirHall(Codec<NoneFeatureConfiguration> codec) {
            super(codec, 8);
        }

        @Override
        void build(Site s) {
            int h = 6 + s.rnd.nextInt(2);
            for (int x = -8; x <= 8; x++)
                for (int z = -8; z <= 8; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > 8.3) continue;
                    s.footing(x, z);
                    s.set(x, 0, z, d > 7.3 ? s.trim : (x + z) % 2 == 0 ? s.tile : s.brick);
                    for (int y = 1; y <= h + 4; y++) s.air(x, y, z);
                    if (d > 5.6 && d < 7.3) s.set(x, h + 1, z, d > 6.6 ? s.trim : s.brick);         // the circle overhead
                }
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                int x = (int) Math.round(Math.cos(a) * 6.4), z = (int) Math.round(Math.sin(a) * 6.4);
                s.set(x, 1, z, s.brick);
                s.column(x, z, 2, h, s.pillar);
                if (i % 3 == 0) {                                // a bell at each quarter, hung inside the circle
                    int bx = (int) Math.round(Math.cos(a) * 5.0), bz = (int) Math.round(Math.sin(a) * 5.0);
                    s.set(bx, h + 1, bz, s.brick);
                    s.set(bx, h, bz, s.bell);
                }
            }
            // the crystal
            int ch = 7 + s.rnd.nextInt(4);
            s.set(0, 1, 0, s.gold);
            s.column(0, 0, 2, ch, s.crystal);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                s.set(d.getStepX(), 1, d.getStepZ(), s.trim);
                s.column(d.getStepX(), d.getStepZ(), 2, 2 + s.rnd.nextInt(ch - 3), s.crystal);
            }
            s.set(3, 1, 3, s.urn);
            if (s.rnd.nextBoolean()) s.set(-3, 1, -3, s.urn);
            s.spawn(HomeRegistry.WARDEN.get(), 0, 1, 4, 0F);
            s.spawn(HomeRegistry.SERAPH.get(), 2, 4, -2, 0F);
            s.spawn(HomeRegistry.SERAPH.get(), -3, 5, 1, 90F);
        }
    }

    // ================================================================== Temple of Remembrance
    /**
     * The largest: a stepped platform, a colonnade all the way round, a roof with a golden ridge, and an inner room
     * where three stones stand that can be read. Two Wardens kneel either side of the door.
     */
    public static class Temple extends Base {
        public Temple(Codec<NoneFeatureConfiguration> codec) {
            super(codec, 10);
        }

        @Override
        void build(Site s) {
            s.floor(-10, -10, 10, 10, 12, s.trim);                         // the lowest step
            s.fill(-9, 1, -9, 9, 1, 9, s.brick);
            s.fill(-8, 2, -8, 8, 2, 8, s.tile);
            for (int i = -8; i <= 8; i++) {                                   // the colonnade
                boolean post = i % 2 == 0;
                if (!post) continue;
                for (int[] p : new int[][]{{i, -8}, {i, 8}, {-8, i}, {8, i}}) {
                    if (p[0] == 0 && p[1] == 8) continue;                 // the way in is left open
                    s.set(p[0], 3, p[1], s.brick);
                    s.column(p[0], p[1], 4, 8, s.pillar);
                }
            }
            s.fill(-9, 9, -9, 9, 9, 9, s.trim);
            s.fill(-8, 9, -8, 8, 9, 8, s.brick);
            for (int k = 0; k < 4; k++)                                        // the roof, rising to a ridge that runs north to south
                s.fill(-7 + k * 2, 10 + k, -8, 7 - k * 2, 10 + k, 8, k == 3 ? s.gold : k % 2 == 0 ? s.tile : s.brick);
            s.set(0, 14, -8, s.lantern);
            s.set(0, 14, 8, s.lantern);
            // the inner room
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++) {
                    boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4, corner = Math.abs(x) == 4 && Math.abs(z) == 4;
                    if (!edge) continue;
                    for (int y = 3; y <= 8; y++)
                        s.set(x, y, z, corner ? s.pillar : (y == 5 || y == 6) && (Math.abs(x) == 2 || Math.abs(z) == 2) ? s.glass : s.brick);
                }
            for (int y = 3; y <= 5; y++) s.air(0, y, 4);                       // the door, to the south
            s.set(0, 6, 4, s.gold);
            for (int z = 5; z <= 10; z++) {                                    // the stair up to it
                int y = z >= 10 ? 0 : z >= 9 ? 1 : 2;
                for (int x = -1; x <= 1; x++) {
                    s.set(x, y, z, s.tile);
                    for (int yy = y + 1; yy <= 6; yy++) s.air(x, yy, z);
                }
            }
            for (int x = -2; x <= 2; x += 2) {                                 // the three stones, each on a step, against the far wall
                s.set(x, 3, -3, s.trim);
                s.set(x, 4, -3, s.stone);
            }
            s.set(-3, 3, -3, s.lantern);
            s.set(3, 3, -3, s.lantern);
            s.set(-3, 3, 0, s.urn);
            s.set(3, 3, 0, s.urn);
            s.set(-3, 3, 2, s.rnd.nextBoolean() ? s.urn : s.bell);
            s.set(0, 8, 0, s.lantern);
            s.spawn(HomeRegistry.WARDEN.get(), -2, 3, 6, 0F);
            s.spawn(HomeRegistry.WARDEN.get(), 2, 3, 6, 0F);
            s.spawn(HomeRegistry.DWELLER.get(), 0, 3, 0, 0F);
            if (s.rnd.nextBoolean()) s.spawn(HomeRegistry.DWELLER.get(), 6, 3, -6, 90F);
        }
    }

    // ================================================================== Beacon of Return
    /** A round tower standing in the Sea of Clouds with a lamp room at its top, so that anything lost out there can find the way in. */
    public static class Beacon extends Base {
        public Beacon(Codec<NoneFeatureConfiguration> codec) {
            super(codec, 3);
        }

        @Override
        void build(Site s) {
            int h = 14 + s.rnd.nextInt(8);
            s.floor(-4, -4, 4, 4, 2, s.tile);
            for (int y = 1; y <= h; y++)
                for (int x = -3; x <= 3; x++)
                    for (int z = -3; z <= 3; z++) {
                        double d = Math.sqrt(x * x + z * z);
                        if (d > 3.3) continue;
                        boolean wall = d > 2.3;
                        if (!wall) s.air(x, y, z);
                        else s.set(x, y, z, y % 5 == 0 ? s.trim : (y % 5 == 3 && (x == 0 || z == 0)) ? s.glass : s.brick);
                    }
            s.air(0, 1, 3);
            s.air(0, 2, 3);
            for (int y = 1; y < h; y++) {                                      // a stair winding up the inside
                double a = y * Math.PI / 4;
                int x = (int) Math.round(Math.cos(a) * 1.5), z = (int) Math.round(Math.sin(a) * 1.5);
                s.set(x, y, z, s.tile);
            }
            s.fill(-4, h + 1, -4, 4, h + 1, 4, s.trim);                        // the gallery
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++) {
                    boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                    for (int y = h + 2; y <= h + 4; y++) s.set(x, y, z, edge ? (Math.abs(x) == 2 && Math.abs(z) == 2 ? s.pillar : s.glass) : y == h + 3 ? s.lantern : Blocks.AIR.defaultBlockState());
                }
            s.fill(-3, h + 5, -3, 3, h + 5, 3, s.trim);
            s.fill(-1, h + 6, -1, 1, h + 6, 1, s.gold);
            s.set(0, h + 7, 0, s.lantern);
            s.set(1, 1, -1, s.urn);
        }
    }
}
