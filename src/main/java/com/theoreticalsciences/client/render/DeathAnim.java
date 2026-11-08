package com.theoreticalsciences.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.10.1: how each of the mod's procedural creatures dies, instead of all of them shrinking away. Every death lasts the
 * game's usual 20 ticks and fits what the creature is:
 * <ul>
 *   <li><b>TOPPLE</b> - falls over sideways (animals and walkers);</li>
 *   <li><b>FLIP</b> - hops and lands on its back, legs up (bugs and crawlers);</li>
 *   <li><b>CRUMPLE</b> - knees buckle, then it falls on its face (husks);</li>
 *   <li><b>STRETCH</b> - pulled out into a thread by the tide, thinner and longer until it is gone (tidal things);</li>
 *   <li><b>DISPERSE</b> - spreads out and thins away like smoke (clouds, moths of light);</li>
 *   <li><b>FLASH</b> - swells, flickers, and is gone in a burst of light (annihilating pairs, evaporating holes);</li>
 *   <li><b>FOLD</b> - flattens into a lower dimension as it turns (hypercubes);</li>
 *   <li><b>SINK</b> - deflates and sinks (jellies);</li>
 *   <li><b>COLLAPSE</b> - slumps into a puddle (fire and rock);</li>
 *   <li><b>SPIN_SINK</b> - spins faster and faster and drills down into the ground (currents);</li>
 *   <li><b>SPIN_DOWN</b> - wobbles like a dying top, its tilt growing, until it lies down (pulsars);</li>
 *   <li><b>FLICKER</b> - its image jitters and winks out (phantoms, antimatter);</li>
 *   <li><b>SCATTER</b> - jumps about at random, ever further, then is gone (random walks);</li>
 *   <li><b>FLUCTUATE</b> - its shape shivers at random, larger and larger, until it fluctuates out of existence.</li>
 * </ul>
 * Each also gives off its own particles while it dies.
 */
public final class DeathAnim {
    public enum Style { TOPPLE, FLIP, CRUMPLE, STRETCH, DISPERSE, FLASH, FOLD, SINK, COLLAPSE, SPIN_SINK, SPIN_DOWN, FLICKER, SCATTER, FLUCTUATE }

    private static final Map<LivingEntity, Integer> LAST = new WeakHashMap<>();

    public static Style of(String id) {
        return switch (id) {
            case "black_swan", "ordinal_hound", "hilbert_bellhop" -> Style.TOPPLE;
            case "planck_mite", "positronium_skitterer", "magma_crawler" -> Style.FLIP;
            case "fission_husk" -> Style.CRUMPLE;
            case "tidal_stretcher", "tidal_horror" -> Style.STRETCH;
            case "nebuloid", "photon_moth", "fourier_wisp" -> Style.DISPERSE;
            case "hawking_imp", "virtual_pair", "annihilator" -> Style.FLASH;
            case "hypercube_drifter", "tessellate" -> Style.FOLD;
            case "vacuum_drifter" -> Style.SINK;
            case "plume_elemental" -> Style.COLLAPSE;
            case "geodynamo_wraith", "binary_serpent" -> Style.SPIN_SINK;
            case "karman_wraith" -> Style.DISPERSE;                          // 1.3.0: an eddy, spreading out
            case "hadal_anglerfish" -> Style.SINK;                          // 1.3.0
            case "siphonophore" -> Style.DISPERSE;
            case "storm_elemental" -> Style.DISPERSE;
            case "pulsar_wisp" -> Style.SPIN_DOWN;
            case "lens_phantom", "antihydrogen_wisp", "paradox_shade" -> Style.FLICKER;
            case "random_walker" -> Style.SCATTER;
            case "boltzmann_brain" -> Style.FLUCTUATE;
            default -> Style.TOPPLE;
        };
    }

    private static float hash(int a, int b) {
        int h = a * 73856093 ^ b * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    /**
     * Applies the death pose to {@code ps} (already turned to face the body's way, origin at the feet). Returns false
     * when nothing should be drawn this frame (the creature has flickered out, or is gone).
     */
    public static boolean apply(PoseStack ps, LivingEntity e, float partial, String id) {
        if (e.deathTime <= 0) return true;
        Style s = of(id);
        float f = Math.min(1F, (e.deathTime + partial) / 20F), h = Math.max(0.3F, e.getBbHeight());
        int tick = e.deathTime, seed = e.getId();
        particles(e, s, f);
        switch (s) {
            case TOPPLE -> ps.mulPose(Axis.ZP.rotationDegrees(Math.min(1F, Mth.sqrt(f * 1.6F)) * 90F));
            case FLIP -> {
                ps.translate(0, 0.4F * Mth.sin(Mth.PI * Math.min(1F, f * 1.4F)), 0);
                ps.translate(0, h / 2, 0);
                ps.mulPose(Axis.XP.rotationDegrees(180F * smooth(Math.min(1F, f * 1.4F))));
                ps.translate(0, -h / 2, 0);
                if (f > 0.7F) ps.translate(0, h * 0.9F, 0);        // lying on its back, legs up
            }
            case CRUMPLE -> {
                float k = Math.min(1F, f * 2), fall = Math.max(0F, f * 2 - 1);
                ps.mulPose(Axis.XP.rotationDegrees(85F * fall * fall));
                ps.scale(1, 1 - 0.35F * k, 1);
            }
            case STRETCH -> {
                if (f > 0.95F) return false;
                float thin = 1 - 0.93F * f;
                ps.scale(thin, 1 + 3F * f * f, thin);
                ps.mulPose(Axis.YP.rotationDegrees(f * 40F));
            }
            case DISPERSE -> {
                if (hash(seed, tick) < (f - 0.35F) * 1.6F) return false;
                float g = 1 + 1.4F * f;
                ps.translate(0, 0.3F * f, 0);
                ps.scale(g, g, g);
            }
            case FLASH -> {
                if (f > 0.7F) return false;
                if (f > 0.45F && (tick & 1) == 0) return false;
                float g = 1 + 0.7F * f / 0.7F;
                ps.scale(g, g, g);
            }
            case FOLD -> {
                if (f > 0.97F) return false;
                ps.translate(0, h / 2, 0);
                ps.mulPose(Axis.YP.rotationDegrees(f * f * 540F));
                ps.mulPose(Axis.ZP.rotationDegrees(f * 30F));
                ps.scale(Math.max(0.02F, 1 - f), 1 - 0.3F * f, 1 + 0.4F * f);
                ps.translate(0, -h / 2, 0);
            }
            case SINK -> {
                ps.translate(0, -h * 0.8F * f, 0);
                ps.scale(1 + 0.4F * f, Math.max(0.1F, 1 - 0.8F * f), 1 + 0.4F * f);
            }
            case COLLAPSE -> ps.scale(1 + 0.7F * f, Math.max(0.06F, 1 - 0.94F * smooth(f)), 1 + 0.7F * f);
            case SPIN_SINK -> {
                ps.translate(0, -h * 1.1F * f * f, 0);
                ps.mulPose(Axis.YP.rotationDegrees(f * f * 1440F));
                ps.scale(1 + 0.2F * f, 1 - 0.4F * f, 1 + 0.2F * f);
            }
            case SPIN_DOWN -> {
                float t = e.tickCount + partial;
                ps.translate(0, -0.35F * smooth(f), 0);
                ps.mulPose(Axis.YP.rotation(t * 0.6F * (1 - f)));            // the precession slows...
                ps.mulPose(Axis.ZP.rotationDegrees(80F * smooth(f)));        // ...as the tilt grows, until it lies down
            }
            case FLICKER -> {
                if (hash(seed, tick * 3 + (int) (partial * 4)) < f * 1.1F - 0.1F) return false;
                float j = 0.25F * f;
                ps.translate((hash(seed + 1, tick) - 0.5F) * j, (hash(seed + 2, tick) - 0.5F) * j, (hash(seed + 3, tick) - 0.5F) * j);
            }
            case SCATTER -> {
                if (f > 0.6F && hash(seed, tick) < (f - 0.6F) * 2.5F) return false;
                float j = 1.4F * f;
                ps.translate((hash(seed + 5, tick) - 0.5F) * j, Math.abs(hash(seed + 6, tick) - 0.5F) * j * 0.5F, (hash(seed + 7, tick) - 0.5F) * j);
            }
            case FLUCTUATE -> {
                if (f > 0.85F && hash(seed, tick) < 0.6F) return false;
                if (f > 0.97F) return false;
                float a = 1.2F * f;
                ps.translate(0, h / 2, 0);
                ps.scale(Math.max(0.05F, 1 + (hash(seed + 1, tick) - 0.5F) * a * 2), Math.max(0.05F, 1 + (hash(seed + 2, tick) - 0.5F) * a * 2),
                        Math.max(0.05F, 1 + (hash(seed + 3, tick) - 0.5F) * a * 2));
                ps.translate(0, -h / 2, 0);
            }
        }
        return true;
    }

    private static float smooth(float v) {
        return v * v * (3 - 2 * v);
    }

    /** Once per tick of the death: the style's particles (client side only; nothing in the world changes). */
    private static void particles(LivingEntity e, Style s, float f) {
        Integer last = LAST.get(e);
        if (last != null && last == e.deathTime) return;
        LAST.put(e, e.deathTime);
        var l = e.level();
        var r = l.random;
        double x = e.getX(), y = e.getY() + e.getBbHeight() * 0.5, z = e.getZ(), w = e.getBbWidth();
        ParticleOptions p;
        int n;
        switch (s) {
            case TOPPLE, FLIP, CRUMPLE -> { p = ParticleTypes.POOF; n = e.deathTime % 4 == 0 ? 1 : 0; }
            case STRETCH -> { p = ParticleTypes.ELECTRIC_SPARK; n = 3; }
            case DISPERSE -> { p = ParticleTypes.WHITE_ASH; n = 8; }
            case FLASH -> {
                if (e.deathTime == 14) {
                    l.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
                    for (int i = 0; i < 24; i++)
                        l.addParticle(ParticleTypes.END_ROD, x, y, z, r.nextGaussian() * 0.2, r.nextGaussian() * 0.2, r.nextGaussian() * 0.2);
                }
                p = ParticleTypes.ELECTRIC_SPARK;
                n = e.deathTime < 14 ? 2 : 0;
            }
            case FOLD -> { p = ParticleTypes.REVERSE_PORTAL; n = 4; }
            case SINK -> { p = ParticleTypes.DRIPPING_WATER; n = 2; }
            case COLLAPSE -> { p = ParticleTypes.LARGE_SMOKE; n = 2; }
            case SPIN_SINK -> { p = ParticleTypes.ELECTRIC_SPARK; n = 5; }
            case SPIN_DOWN -> { p = ParticleTypes.ELECTRIC_SPARK; n = e.deathTime % 3 == 0 ? 6 : 0; }
            case FLICKER -> { p = ParticleTypes.END_ROD; n = 1; }
            case SCATTER -> { p = ParticleTypes.CRIT; n = 3; }
            default -> { p = ParticleTypes.WITCH; n = 3; }
        }
        for (int i = 0; i < n; i++)
            l.addParticle(p, x + (r.nextDouble() - 0.5) * w, y + (r.nextDouble() - 0.5) * e.getBbHeight(), z + (r.nextDouble() - 0.5) * w,
                    (r.nextDouble() - 0.5) * 0.05, r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.05);
    }

    private DeathAnim() {}
}
