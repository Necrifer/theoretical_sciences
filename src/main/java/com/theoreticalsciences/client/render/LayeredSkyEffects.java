package com.theoreticalsciences.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.2.0: the Platonic Realm's sky, in two layers: the Realm's own pale fractal sky below y 1280, and the Transfinite
 * Hierarchy's dark one in its upper strata (where the Hierarchy is now), chosen by where the camera is.
 */
public class LayeredSkyEffects extends DimensionSpecialEffects {
    private final FractalSkyEffects lower = new FractalSkyEffects(0), upper = new FractalSkyEffects(4);

    public LayeredSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    private FractalSkyEffects now() {
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        return cam.getPosition().y >= com.theoreticalsciences.world.Strata.Y ? upper : lower;
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return now().getBrightnessDependentFogColor(color, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return now().isFoggyAt(x, z);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix,
                             boolean isFoggy, Runnable setupFog) {
        return (camera.getPosition().y >= com.theoreticalsciences.world.Strata.Y ? upper : lower)
                .renderSky(level, ticks, partialTick, modelViewMatrix, camera, projectionMatrix, isFoggy, setupFog);
    }
}
