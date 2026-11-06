package com.theoreticalsciences.inner;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.registry.QHFluids;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: Inner Space, a living body seen at the scale of its cells (the Biology branch's world). Five biomes: the
 * Bloodstream (channels of moving plasma), Tissue (walls of cells), the Lymph Node (where antibodies are matured), the
 * Nucleus (chromatin and the helix) and the Tumour (where an anomaly has taken hold). Shared helpers.
 */
public final class Inner {
    public static final ResourceKey<Biome> BLOODSTREAM = key("bloodstream"), TISSUE = key("tissue"), LYMPH_NODE = key("lymph_node"),
            NUCLEUS = key("nucleus"), TUMOUR = key("tumour");

    static ResourceKey<Biome> key(String id) {
        return ResourceKey.create(Registries.BIOME, TheoreticalSciences.id(id));
    }

    public static boolean in(Level l) {
        return l.dimension() == QHDimensions.INNER;
    }

    /** The biome's id path at a place in Inner Space ("" anywhere else). */
    public static String biome(Level l, BlockPos p) {
        if (!in(l)) return "";
        return l.getBiome(p).unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    /**
     * The Bloodstream's current at (x, z): a smooth field of flow that turns slowly with time, like blood pushed round by
     * a heartbeat. Its speed rises and falls once a second (the pulse). Blocks per tick.
     */
    public static Vec3 current(Level l, double x, double z) {
        double t = l.getGameTime();
        double a = 1.3 * Math.sin(x / 90.0 + t * 0.0002) + 1.1 * Math.cos(z / 110.0 - t * 0.00015);
        double pulse = 0.75 + 0.25 * Math.sin(t * Math.PI * 2 / 20.0);
        return new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.045 * pulse);
    }

    public static boolean inPlasma(Entity e) {
        var f = QHFluids.ALL.get("blood_plasma");
        return f != null && f.type != null && e.isInFluidType(f.type.value());
    }

    static boolean live(Player p) {
        return !p.isCreative() && !p.isSpectator();
    }

    private Inner() {}
}
