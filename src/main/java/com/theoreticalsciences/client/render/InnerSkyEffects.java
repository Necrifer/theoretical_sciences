package com.theoreticalsciences.client.render;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: Inner Space has no sky: it is the inside of a body. No sun, moon or stars, and the fog closes in close, warm and
 * red (each biome paints its own), as light comes through flesh. The fog brightens a little with each heartbeat.
 */
public class InnerSkyEffects extends DimensionSpecialEffects {
    final boolean heart;

    public InnerSkyEffects() {
        this(true);
    }

    /** heart false: Titan's hidden ocean, as dark and close, without the heartbeat. */
    public InnerSkyEffects(boolean heart) {
        super(Float.NaN, false, SkyType.NONE, false, true);
        this.heart = heart;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        if (!heart) return color;
        long ms = System.currentTimeMillis();
        double beat = Math.pow(Math.max(0, Math.sin(ms / 1000.0 * Math.PI * 2 * 56 / 60.0)), 12);
        return color.scale(0.85 + 0.15 * beat);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
