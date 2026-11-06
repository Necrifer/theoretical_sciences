package com.theoreticalsciences.client.render.effects;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * 1.2.0: the structure animation a machine (or a /tsanim preview) shows. The working effects (star, shell, orbiters,
 * beams and the rest) are a multiblock's {@code "fx"} entries instead, see {@link MachineFx}.
 * (The GregTech dynamic renders that once set star, shell and beams here were removed before release.)
 */
public record FxSpec(@Nullable ResourceLocation animation) {
    public static final int STAR_ORANGE = 0xFF8A2A;

    public static FxSpec animationOnly(@Nullable ResourceLocation anim) {
        return new FxSpec(anim);
    }

    /** How far from the controller the animation can reach, in blocks (for culling). */
    public float extent(float animationExtent) {
        return animationExtent + 2;
    }

    public static int parseColor(String s, int dflt) {
        if (s == null || s.isEmpty()) return dflt;
        try {
            return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }
}
