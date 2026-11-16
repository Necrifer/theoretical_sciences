package com.theoreticalsciences.mind;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1.4.0: Mind and Language in the world. The Neural Cortex of Inner Space grows neurons the size of trees; the
 * Overworld keeps the ruins of libraries whose inscriptions nobody can read any more.
 */
public final class MindFeatures {
    static BlockState b(String id) {
        return QHBlocks.get(id).defaultBlockState();
    }

    static boolean open(WorldGenLevel l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.isAir() || s.canBeReplaced();
    }

    /**
     * A neuron: a glowing cell body on the floor, an axon of white matter rising from it and branching at the top into a
     * crown of dendrites, each ending in a glowing synaptic knob; a few dendrites creep out along the ground too.
     */
    public static class NeuronArbor extends Feature<NoneFeatureConfiguration> {
        public NeuronArbor(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        void branch(WorldGenLevel l, RandomSource r, BlockPos from, double dx, double dy, double dz, int len, BlockState white, BlockState soma) {
            double x = from.getX() + 0.5, y = from.getY() + 0.5, z = from.getZ() + 0.5;
            BlockPos last = from;
            for (int i = 0; i < len; i++) {
                dx += (r.nextDouble() - 0.5) * 0.35;
                dz += (r.nextDouble() - 0.5) * 0.35;
                dy += (r.nextDouble() - 0.5) * 0.2;
                double n = Math.sqrt(dx * dx + dy * dy + dz * dz);
                x += dx / n;
                y += dy / n;
                z += dz / n;
                BlockPos p = BlockPos.containing(x, y, z);
                if (!open(l, p)) break;
                setBlock(l, p, white);
                last = p;
            }
            if (open(l, last.above())) setBlock(l, last.above(), soma);
            else setBlock(l, last, soma);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX(), o.getZ());
            BlockPos base = new BlockPos(o.getX(), y, o.getZ());
            if (!l.getBlockState(base.below()).isSolid() || !l.getFluidState(base).isEmpty()) return false;
            BlockState white = b("white_matter"), soma = b("neuron_soma"), grey = b("grey_matter");
            // the cell body: a lumpy knot, glowing
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    for (int dy = 0; dy <= 1; dy++) {
                        if (Math.abs(dx) + Math.abs(dz) + dy > 2) continue;
                        setBlock(l, base.offset(dx, dy, dz), dy == 1 || (dx == 0 && dz == 0) ? soma : grey);
                    }
            // the axon: up, wandering a little
            int h = 8 + r.nextInt(9);
            double x = base.getX() + 0.5, z = base.getZ() + 0.5;
            BlockPos top = base.above(2);
            for (int i = 2; i < h; i++) {
                x += (r.nextDouble() - 0.5) * 0.6;
                z += (r.nextDouble() - 0.5) * 0.6;
                BlockPos p = BlockPos.containing(x, base.getY() + i, z);
                if (!open(l, p)) break;
                setBlock(l, p, white);
                top = p;
            }
            // the crown of dendrites
            int n = 3 + r.nextInt(3);
            for (int k = 0; k < n; k++) {
                double a = k * Math.PI * 2 / n + r.nextDouble() * 0.6;
                branch(l, r, top, Math.cos(a), 0.6 + r.nextDouble() * 0.6, Math.sin(a), 4 + r.nextInt(5), white, soma);
            }
            // dendrites along the floor
            for (int k = 0; k < 2 + r.nextInt(2); k++) {
                double a = r.nextDouble() * Math.PI * 2;
                branch(l, r, base.offset(0, 0, 0).relative(Direction.Plane.HORIZONTAL.getRandomDirection(r), 2), Math.cos(a), 0.05, Math.sin(a), 3 + r.nextInt(4), white, soma);
            }
            return true;
        }
    }

    /**
     * A sunken library: a hall of glyph-stone bricks half buried, its roof fallen in, panels of inscribed glyphs along its
     * walls, rows of archive shelves still holding tablets, and a cache of what the readers left.
     */
    public static class LibraryRuin extends Feature<NoneFeatureConfiguration> {
        public LibraryRuin(Codec<NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel l = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int y = com.theoreticalsciences.deeptime.DeepTimeFeatures.flatGroundPublic(l, o, 7);
            if (y == Integer.MIN_VALUE) return false;
            BlockPos c = new BlockPos(o.getX(), y - 1, o.getZ());          // the floor, a block below the ground
            if (!l.getFluidState(c.above(2)).isEmpty()) return false;
            BlockState brick = b("glyph_stone_bricks"), glyph = b("inscribed_glyph_stone"), shelf = b("archive_shelf");
            int hx = 7, hz = 5, wall = 5;
            for (int dx = -hx; dx <= hx; dx++)
                for (int dz = -hz; dz <= hz; dz++) {
                    BlockPos f = c.offset(dx, 0, dz);
                    for (int k = 1; k <= 3; k++) if (!l.getBlockState(f.below(k)).isSolid()) setBlock(l, f.below(k), brick);
                    setBlock(l, f, brick);
                    boolean edge = Math.abs(dx) == hx || Math.abs(dz) == hz;
                    // the ruin's wall height falls away to one corner, as if the far end had slumped
                    int top = edge ? wall - (dx + hx) / 6 - (r.nextInt(4) == 0 ? 1 : 0) : 0;
                    for (int h = 1; h <= wall + 1; h++) {
                        BlockPos p = f.above(h);
                        if (edge && h <= top) {
                            boolean panel = (h == 2 || h == 3) && (Math.abs(dx) == hx ? Math.abs(dz) % 3 == 1 : Math.abs(dx) % 3 == 1);
                            setBlock(l, p, panel ? glyph : brick);
                        } else if (!edge && h == wall + 1 && r.nextFloat() < 0.3F) {
                            setBlock(l, p, brick);                       // what is left of the roof
                        } else {
                            setBlock(l, p, Blocks.AIR.defaultBlockState());
                        }
                    }
                    // three rows of shelves, two high, broken here and there
                    if (!edge && (dz == -3 || dz == 0 || dz == 3) && Math.abs(dx) <= hx - 2 && dx != 0) {
                        for (int h = 1; h <= 2; h++) if (r.nextFloat() < 0.85F) setBlock(l, f.above(h), shelf);
                    }
                }
            // the doorway, and fallen roof stones on the floor
            for (int h = 1; h <= 3; h++) setBlock(l, c.offset(0, h, -hz), Blocks.AIR.defaultBlockState());
            for (int i = 0; i < 6; i++) {
                BlockPos p = c.offset(r.nextInt(2 * hx - 1) - hx + 1, 1, r.nextInt(2 * hz - 1) - hz + 1);
                if (l.getBlockState(p).isAir()) setBlock(l, p, r.nextInt(3) == 0 ? glyph : brick);
            }
            // the reading table at the far end: an inscribed slab over two shelves
            setBlock(l, c.offset(-1, 1, hz - 1), shelf);
            setBlock(l, c.offset(1, 1, hz - 1), shelf);
            setBlock(l, c.offset(0, 1, hz - 1), glyph);
            com.theoreticalsciences.dungeon.Caches.place(l, c.offset(0, 2, hz - 1), "chests/library_ruin", r);
            return true;
        }
    }

    private MindFeatures() {}
}
