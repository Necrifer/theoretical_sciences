package com.theoreticalsciences.world;

import com.mojang.serialization.Codec;
import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHBlocks;
import net.minecraft.core.BlockPos;
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

/**
 * A Collapsed Calabi-Yau Knot: a hidden dimension that uncurled and froze as a trefoil knot of hyperframe hanging over
 * the Moduli Wastes. Its moduli crystallised where the knot crosses itself; a cache sits at its heart.
 */
public class CalabiYauKnotFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/calabi_yau_knot"));

    public CalabiYauKnotFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos o = ctx.origin();
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (ground <= level.getMinBuildHeight() + 5 || ground > 200) return false;
        BlockPos c = new BlockPos(o.getX(), ground + 14 + r.nextInt(8), o.getZ());
        BlockState frame = QHBlocks.get("hyperframe_casing").defaultBlockState();
        BlockState crystal = QHBlocks.get("bulk_crystal").defaultBlockState();
        BlockState moduli = QHBlocks.get("moduli_ore").defaultBlockState();
        double scale = 3.2 + r.nextDouble();
        double tilt = r.nextDouble() * Math.PI;
        for (int i = 0; i < 720; i++) {
            double t = i * Math.PI * 2 / 720;
            double x = Math.sin(t) + 2 * Math.sin(2 * t), z = Math.cos(t) - 2 * Math.cos(2 * t), y = -Math.sin(3 * t);
            double rx = x * Math.cos(tilt) - y * Math.sin(tilt), ry = x * Math.sin(tilt) + y * Math.cos(tilt);
            BlockPos p = c.offset((int) Math.round(rx * scale), (int) Math.round(ry * scale), (int) Math.round(z * scale));
            setBlock(level, p, i % 40 == 0 ? moduli : (i % 9 == 0 ? crystal : frame));
            setBlock(level, p.above(), i % 13 == 0 ? crystal : frame);
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) setBlock(level, c.offset(dx, -1, dz), frame);
        com.theoreticalsciences.dungeon.Caches.place(level, c, LOOT, r);
        com.theoreticalsciences.world.zones.StructureZones.mark(level, c, (int) (scale * 3) + 4, com.theoreticalsciences.world.zones.StructureZones.Mode.TWIST);
        return true;
    }
}
