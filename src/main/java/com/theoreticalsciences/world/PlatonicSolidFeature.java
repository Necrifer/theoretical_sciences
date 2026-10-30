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
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

/**
 * One of the five Platonic solids, floating in the Platonic Realm: a hollow shell of axiom stone whose edges are
 * golden-ratio glass, with a vault ("Euclid's Vault") at its exact centre. Built from the face planes of the solid,
 * so the geometry is exact.
 */
public class PlatonicSolidFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheoreticalSciences.id("chests/euclids_vault"));
    private static final double PHI = (1 + Math.sqrt(5)) / 2;

    public PlatonicSolidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static Vec3[] normals(int kind) {
        java.util.List<Vec3> n = new java.util.ArrayList<>();
        switch (kind) {
            case 0 -> {      // tetrahedron
                n.add(new Vec3(1, 1, 1)); n.add(new Vec3(1, -1, -1)); n.add(new Vec3(-1, 1, -1)); n.add(new Vec3(-1, -1, 1));
            }
            case 1 -> {      // cube
                for (int s = -1; s <= 1; s += 2) { n.add(new Vec3(s, 0, 0)); n.add(new Vec3(0, s, 0)); n.add(new Vec3(0, 0, s)); }
            }
            case 2 -> {      // octahedron
                for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) for (int c = -1; c <= 1; c += 2) n.add(new Vec3(a, b, c));
            }
            case 3 -> {      // dodecahedron: face normals point at the vertices of an icosahedron
                for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
                    n.add(new Vec3(0, a, b * PHI)); n.add(new Vec3(a, b * PHI, 0)); n.add(new Vec3(a * PHI, 0, b));
                }
            }
            default -> {     // icosahedron: face normals point at the vertices of a dodecahedron
                for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
                    for (int c = -1; c <= 1; c += 2) n.add(new Vec3(a, b, c));
                    n.add(new Vec3(0, a / PHI, b * PHI)); n.add(new Vec3(a / PHI, b * PHI, 0)); n.add(new Vec3(a * PHI, 0, b / PHI));
                }
            }
        }
        return n.stream().map(Vec3::normalize).toArray(Vec3[]::new);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        int kind = r.nextInt(5);
        Vec3[] ns = normals(kind);
        // inradius per solid, chosen so the corners stay inside the area a feature may write to (15 blocks out)
        double radius = switch (kind) {
            case 0 -> 4 + r.nextInt(2);
            case 1, 2 -> 5 + r.nextInt(3);
            default -> 6 + r.nextInt(4);
        };
        BlockPos c = ctx.origin().atY(70 + r.nextInt(70));
        BlockState stone = QHBlocks.get("axiom_stone").defaultBlockState();
        BlockState glass = QHBlocks.get("golden_ratio_glass").defaultBlockState();
        BlockState prime = QHBlocks.get("prime_number_ore").defaultBlockState();
        int R = 15;
        for (int x = -R; x <= R; x++)
            for (int y = -R; y <= R; y++)
                for (int z = -R; z <= R; z++) {
                    Vec3 p = new Vec3(x, y, z);
                    double worst = -1e9;
                    int near = 0;
                    for (Vec3 n : ns) {
                        double d = n.dot(p) - radius;
                        worst = Math.max(worst, d);
                        if (Math.abs(d) < 0.75) near++;
                    }
                    if (worst > 0.5) continue;                       // outside
                    if (worst < -1.2) continue;                      // hollow inside
                    BlockState s = near >= 2 ? glass : (r.nextInt(30) == 0 ? prime : stone);
                    setBlock(level, c.offset(x, y, z), s);
                }
        setBlock(level, c.below(), glass);
        com.theoreticalsciences.dungeon.Caches.place(level, c, LOOT, r);
        com.theoreticalsciences.world.zones.StructureZones.mark(level, c, (int) radius + 4, com.theoreticalsciences.world.zones.StructureZones.Mode.KALEIDOSCOPE);
        return true;
    }
}
