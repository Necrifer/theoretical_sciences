package com.theoreticalsciences.client.render;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: Titan's sky - an ordinary day and night (the biomes paint it orange), seen through haze. Titan's smog of tholins
 * is so thick that from the ground the Sun is a dim smudge and Saturn cannot be seen at all, so the fog closes in as it
 * does in the Nether and takes the haze's colour, darker at night.
 */
public class TitanSkyEffects extends DimensionSpecialEffects {
    public TitanSkyEffects() {
        super(192.0F, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(brightness * 0.9F + 0.1F, brightness * 0.85F + 0.08F, brightness * 0.7F + 0.05F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }
}
