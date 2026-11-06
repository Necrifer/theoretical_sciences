package com.theoreticalsciences.inner;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.2.0: the landmarks of Inner Space (and the Overworld's BSL-4 Field Station), built only from the mod's own blocks.
 * <ul>
 *   <li>Tissue: <b>Cell Clusters</b> (walls of cells: membrane spheres, half sunk, each with a nucleus inside) and the
 *   <b>Ribosome Workshop</b> (a ribosome's two subunits, big and small, with a cache between them).</li>
 *   <li>The Lymph Node: the <b>Germinal Centre</b> (a dome with a dark zone and a light zone, where antibodies are
 *   matured; a cache at its heart).</li>
 *   <li>The Nucleus: the <b>Helix Spire</b> (DNA's double helix, its rungs of nucleotides) and the <b>Nuclear Pore</b> (a
 *   ring of eight spokes in the envelope; a cache under its channel).</li>
 *   <li>The Tumour: <b>Tumour Growths</b> (lumps of overgrown tissue, studded with nodules).</li>
 *   <li>The Overworld: the <b>BSL-4 Field Station</b>, an abandoned high-containment lab where the branch's first samples are found.</li>
 * </ul>
 */
public final class InnerFeatures {
    private InnerFeatures() {}

    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    abstract static class Base extends Feature<NoneFeatureConfiguration> {
        Base(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        void put(WorldGenLevel l, BlockPos p, BlockState s) {
            setBlock(l, p, s);
        }

        void clear(WorldGenLevel l, BlockPos p) {
            setBlock(l, p, Blocks.AIR.defaultBlockState());
        }

        /** Standing on dry ground (not on the plasma). */
        static boolean dry(WorldGenLevel l, BlockPos o) {
            return l.getFluidState(o.below()).isEmpty() && l.getFluidState(o).isEmpty() && !l.getBlockState(o.below()).isAir();
        }

        /** A sphere shell (or ball, when hollow is false) of radius r round c. */
        void sphere(WorldGenLevel l, BlockPos c, double r, BlockState shell, BlockState inside, boolean hollow) {
            int R = (int) Math.ceil(r);
            for (int x = -R; x <= R; x++)
                for (int y = -R; y <= R; y++)
                    for (int z = -R; z <= R; z++) {
                        double d = Math.sqrt(x * x + y * y + z * z);
                        if (d > r) continue;
                        BlockPos p = c.offset(x, y, z);
                        if (d > r - 1.2) put(l, p, shell);
                        else if (!hollow) put(l, p, shell);
                        else if (inside == null) clear(l, p);
                        else put(l, p, inside);
                    }
        }
    }

    // ================================================================== Tissue
    public static class CellCluster extends Base {
        public CellCluster(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState mem = b("cell_membrane"), nuc = b("chromatin"), glow = b("bioluminescent_membrane");
            int n = 2 + r.nextInt(3);
            for (int i = 0; i < n; i++) {
                double rad = 3 + r.nextDouble() * 3;
                BlockPos c = o.offset(r.nextInt(11) - 5, (int) (rad * 0.3), r.nextInt(11) - 5);
                sphere(l, c, rad, mem, null, true);
                sphere(l, c.offset(0, -1, 0), Math.max(1.2, rad * 0.3), nuc, null, false);
                put(l, c.offset(0, (int) rad - 1, 0), glow);
            }
            return true;
        }
    }

    public static class RibosomeWorkshop extends Base {
        public RibosomeWorkshop(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState big = b("lymph_tissue"), small = b("cell_membrane"), glow = b("bioluminescent_membrane"), strand = b("chromatin");
            // the large subunit below, the small one above, a tunnel between them where the message runs through
            sphere(l, o.offset(0, 3, 0), 6, big, null, true);
            sphere(l, o.offset(0, 9, 0), 4, small, null, true);
            for (int x = -9; x <= 9; x++)
                for (int y = 5; y <= 7; y++) clear(l, o.offset(x, y, 0));
            for (int x = -9; x <= 9; x++) put(l, o.offset(x, 5, 0), strand);           // the messenger RNA, threaded through
            for (int y = 0; y < 2; y++) clear(l, o.offset(0, 1 + y, 6));                // a door
            put(l, o.offset(0, 8, 0), glow);
            put(l, o.offset(0, 1, 0).below(), big);
            com.theoreticalsciences.dungeon.Caches.place(l, o.offset(0, 1, 0), "chests/ribosome_workshop", r);
            return true;
        }
    }

    // ================================================================== the Lymph Node
    public static class GerminalCentre extends Base {
        public GerminalCentre(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState wall = b("lymph_tissue"), glow = b("bioluminescent_membrane"), floor = b("cell_membrane");
            int R = 9;
            for (int x = -R; x <= R; x++)
                for (int z = -R; z <= R; z++)
                    for (int y = -1; y <= R; y++) {
                        double d = Math.sqrt(x * x + y * y * 1.0 + z * z);
                        BlockPos p = o.offset(x, y, z);
                        if (y == -1 && x * x + z * z <= R * R) put(l, p, floor);
                        else if (d <= R && d > R - 1.2) put(l, p, (x > 0) == (y % 3 == 0) ? glow : wall);   // the light zone on one side
                        else if (d <= R - 1.2 && y >= 0) clear(l, p);
                    }
            for (int y = 0; y < 3; y++)
                for (int x = -1; x <= 1; x++) for (int z = R - 2; z <= R; z++) clear(l, o.offset(x, y, z));
            com.theoreticalsciences.dungeon.Caches.place(l, o, "chests/germinal_centre", r);
            return true;
        }
    }

    // ================================================================== the Nucleus
    public static class HelixSpire extends Base {
        public HelixSpire(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        /**
         * DNA's double helix, built whole: two thick sugar-phosphate backbones winding round each other (a full turn every ten
         * levels, as B-DNA turns every ten base pairs) and a base pair across them on every level, the rungs of Nucleotide
         * Clusters with a lit pair now and then. The Polymerase climbs it rung by rung.
         */
        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState back = b("chromatin"), rung = b("nucleotide_cluster"), glow = b("bioluminescent_membrane");
            int h = 32 + r.nextInt(21);
            double turn = r.nextDouble() * Math.PI * 2, R = 5.0;
            for (int y = -2; y < h; y++) {
                double a = turn + y * Math.PI * 2 / 10;
                double x1 = Math.cos(a) * R, z1 = Math.sin(a) * R;
                // the backbones, three blocks thick
                for (int s = -1; s <= 1; s += 2)
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx * dx + dz * dz > 1) continue;
                            put(l, o.offset((int) Math.round(s * x1) + dx, y, (int) Math.round(s * z1) + dz), back);
                        }
                if (y < 0) continue;
                // a base pair on every level, the two halves meeting in the middle
                for (int k = -4; k <= 4; k++) {
                    double f = k / 4.5;
                    BlockPos p = o.offset((int) Math.round(x1 * f), y, (int) Math.round(z1 * f));
                    put(l, p, k == 0 && y % 5 == 0 ? glow : rung);
                }
            }
            // its foot sunk in chromatin
            for (int x = -7; x <= 7; x++)
                for (int z = -7; z <= 7; z++)
                    if (x * x + z * z <= 49) put(l, o.offset(x, -1, z), back);
            return true;
        }
    }

    public static class NuclearPore extends Base {
        public NuclearPore(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState ring = b("cell_membrane"), spoke = b("chromatin"), glow = b("bioluminescent_membrane");
            int R = 8;
            for (int x = -R - 1; x <= R + 1; x++)
                for (int z = -R - 1; z <= R + 1; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    for (int y = 0; y <= 2; y++) {
                        BlockPos p = o.offset(x, y, z);
                        if (d <= R + 1 && d > R - 1) put(l, p, ring);
                    }
                }
            // eight spokes, the pore's eight-fold symmetry, meeting at a glowing channel
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                for (int s = 2; s < R; s++) put(l, o.offset((int) Math.round(Math.cos(a) * s), 2, (int) Math.round(Math.sin(a) * s)), spoke);
            }
            for (int y = 0; y <= 2; y++) put(l, o.offset(0, y, 0), y == 2 ? glow : Blocks.AIR.defaultBlockState());
            // the channel goes down: a room under the pore, with the cache
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++)
                    for (int y = -4; y <= -1; y++) {
                        BlockPos p = o.offset(x, y, z);
                        if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == -4) put(l, p, ring);
                        else clear(l, p);
                    }
            clear(l, o);
            com.theoreticalsciences.dungeon.Caches.place(l, o.offset(0, -3, 0), "chests/nuclear_pore", r);
            return true;
        }
    }

    // ================================================================== the Tumour
    public static class TumourGrowth extends Base {
        public TumourGrowth(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState mass = b("tumour_mass"), nodule = b("tumour_nodule");
            // lumps on lumps: each growth buds smaller ones off itself
            BlockPos c = o;
            double rad = 4 + r.nextDouble() * 3;
            for (int i = 0; i < 6 && rad > 1.5; i++) {
                sphere(l, c, rad, mass, null, false);
                put(l, c.offset(r.nextInt(3) - 1, (int) rad - 1, r.nextInt(3) - 1), nodule);
                c = c.offset(r.nextInt(7) - 3, (int) (rad * 0.6), r.nextInt(7) - 3);
                rad *= 0.75;
            }
            return true;
        }
    }

    // ================================================================== the Overworld
    public static class FieldStation extends Base {
        public FieldStation(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            if (!dry(l, o)) return false;
            BlockState wall = b("biosafety_panel"), glass = b("leaded_glass"), lamp = b("bioluminescent_membrane");
            int X = 7, Z = 5, H = 5;
            for (int x = -X; x <= X; x++)
                for (int z = -Z; z <= Z; z++)
                    for (int y = -1; y <= H; y++) {
                        BlockPos p = o.offset(x, y, z);
                        boolean shell = Math.abs(x) == X || Math.abs(z) == Z || y == -1 || y == H;
                        boolean window = y == 2 && Math.abs(z) == Z && x % 3 == 0 && Math.abs(x) < X;
                        if (shell) put(l, p, window ? glass : y == H && x == 0 && z == 0 ? lamp : wall);
                        else clear(l, p);
                    }
            // an inner wall with its airlock: two doors, never both open at once (here, both long since broken)
            for (int z = -Z + 1; z < Z; z++)
                for (int y = 0; y < H; y++) put(l, o.offset(2, y, z), wall);
            for (int y = 0; y < 2; y++) {
                clear(l, o.offset(2, y, 0));
                clear(l, o.offset(-X, y, 0));
            }
            for (int z = -1; z <= 1; z += 2) put(l, o.offset(3, H - 1, z * 3), lamp);
            // a ring of foundations round it, and the cache in the hot lab
            for (int x = -X; x <= X; x++)
                for (int z = -Z; z <= Z; z++)
                    for (int y = -5; y < -1; y++) {
                        BlockPos p = o.offset(x, y, z);
                        if (l.getBlockState(p).isAir() || !l.getFluidState(p).isEmpty()) put(l, p, wall);
                    }
            com.theoreticalsciences.dungeon.Caches.place(l, o.offset(5, 0, -3), "chests/bsl4_field_station", r);
            return true;
        }
    }
}
