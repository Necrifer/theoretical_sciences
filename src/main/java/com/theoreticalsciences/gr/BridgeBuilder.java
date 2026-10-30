package com.theoreticalsciences.gr;

import com.theoreticalsciences.registry.QHBlocks;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Builds the natural Einstein-Rosen bridge at the centre of each hole the first time it is needed: a ring of rock
 * around a 3x3 throat, standing on a platform, with a landing pad beside it.
 */
public final class BridgeBuilder {
    public static final int Y = 120;
    public static final BlockPos ARRIVAL = new BlockPos(0, Y, 4);

    static final class Flag extends SavedData {
        boolean built;

        static Flag load(CompoundTag t, HolderLookup.Provider r) {
            Flag f = new Flag();
            f.built = t.getBoolean("Built");
            return f;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
            t.putBoolean("Built", built);
            return t;
        }
    }

    public static void ensureBuilt(ServerLevel l) {
        Flag f = l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Flag::new, Flag::load, null), "theoreticalsciences_er_bridge");
        Block bridge = QHBlocks.get("einstein_rosen_bridge");
        // the flag alone is not trusted: if the throat is missing (old world, someone broke it all) it is rebuilt
        if (f.built && l.getBlockState(new BlockPos(0, Y + 3, 0)).is(bridge)) return;
        build(l, l.dimension() == QHDimensions.BLACK_HOLE);
        f.built = true;
        f.setDirty();
    }

    /** Builds the natural bridge around x=0, z=0. Works both on a live level and during world generation. */
    public static void build(net.minecraft.world.level.LevelAccessor l, boolean bh) {
        Block rock = QHBlocks.get(bh ? "infall_rock" : "ejecta_rock"), bridge = QHBlocks.get("einstein_rosen_bridge"), glass = QHBlocks.get("horizon_glass");
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++)
                if (x * x + z * z <= 40) l.setBlock(new BlockPos(x, Y - 1, z), (x + z) % 3 == 0 ? glass.defaultBlockState() : rock.defaultBlockState(), 2);
        // the ring frame, standing upright on the x axis
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI * 2 / 64;
            BlockPos p = new BlockPos((int) Math.round(Math.cos(a) * 3.2), Y + 3 + (int) Math.round(Math.sin(a) * 3.2), 0);
            l.setBlock(p, rock.defaultBlockState(), 2);
        }
        for (int x = -2; x <= 2; x++)
            for (int y = 0; y < 5; y++)
                if (x * x + (y - 2) * (y - 2) <= 5) l.setBlock(new BlockPos(x, Y + 1 + y, 0), bridge.defaultBlockState(), 2);
    }

    /** World generation: the chunk at the origin of each hole carries the bridge. */
    public static class Feature extends net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> {
        public Feature(com.mojang.serialization.Codec<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> c) {
            super(c);
        }

        @Override
        public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> ctx) {
            BlockPos o = ctx.origin();
            if ((o.getX() >> 4) != 0 || (o.getZ() >> 4) != 0) return false;
            boolean bh = ctx.level().getLevel().dimension() == QHDimensions.BLACK_HOLE;
            build(ctx.level(), bh);
            return true;
        }
    }

    private BridgeBuilder() {}
}
