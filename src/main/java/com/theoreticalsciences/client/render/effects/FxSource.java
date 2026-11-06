package com.theoreticalsciences.client.render.effects;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.2.0: something the effects and animations are drawn for: one of this mod's machines, or a /tsanim preview.
 * Asked every frame, so it only needs to read state the client already has (nothing is sent for it).
 */
public interface FxSource {
    /** False once it is gone (broken, unloaded): it is then forgotten. */
    boolean alive();

    Level level();

    /** The centre of the controller block, in world space. */
    Vec3 controllerCentre();

    /** The way the controller's front faces (horizontal). */
    Direction front();

    boolean formed();

    /** A recipe is running right now (gaps of a few ticks between recipes are smoothed over by the renderer). */
    boolean working();

    FxSpec spec();

    /** The mod's own controller behind this, for its data-driven working effects (null for anything else). */
    default com.theoreticalsciences.machine.ControllerBlockEntity ownController() {
        return null;
    }

    /** Moving parts baked for this machine alone (the mod's own multiblocks), or null to use the spec's animation. */
    default com.theoreticalsciences.client.render.animation.Animations.Baked liveBaked() {
        return null;
    }

    /** Fixes the random parts (orbits) so each machine keeps its own. */
    default long seed() {
        Vec3 c = controllerCentre();
        return Double.doubleToLongBits(c.x * 31 + c.y * 977 + c.z * 7919);
    }
}
