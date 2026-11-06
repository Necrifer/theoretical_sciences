package com.theoreticalsciences.client.render.effects;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 1.2.0: what one frame of the large effects is drawn with: the camera's view and projection, where the camera is, and
 * the time. Effects take nothing else from the game, so any renderer (the mod's own multiblocks, another mod's machine,
 * a preview) can call them.
 * <p>
 * Machine space: a machine's effects and animations are written in blocks from the centre of its controller, as
 * {@code [right, up, back]}: back is away from the controller's front, right is to the machine's own right (its front's
 * clockwise neighbour), up is up. {@link #basis} turns that into world directions for a facing.
 */
public final class FxFrame {
    public final Matrix4f view, proj;
    public final Vec3 cam;
    /** Seconds of game time (smooth: partial ticks included). */
    public final float time;
    public final float partial;
    public final double tick;
    /** The camera's left and up directions (for light that faces the camera); set by the renderer. */
    public org.joml.Vector3f left = new org.joml.Vector3f(1, 0, 0), up = new org.joml.Vector3f(0, 1, 0);

    public FxFrame(Matrix4f view, Matrix4f proj, Vec3 cam, double tick, float partial) {
        this.view = view;
        this.proj = proj;
        this.cam = cam;
        this.tick = tick;
        // seconds, wrapped every hour: a float of a world's whole age loses the fraction of a tick after a few days
        // (and the shaders take floats), so movement went in steps on old worlds
        this.time = wrapSeconds(tick);
        this.partial = partial;
    }

    /** Game time (ticks, with the partial tick) as seconds, wrapped every hour so a float keeps its precision. */
    public static float wrapSeconds(double tick) {
        return (float) ((tick / 20.0) % 3600.0);
    }

    /** The rotation from machine space ([right, up, back]) to world space, for a controller whose front faces front. */
    public static Matrix4f basis(Direction front) {
        if (front.getAxis() == Direction.Axis.Y) front = Direction.NORTH;
        Direction r = front.getClockWise(), b = front.getOpposite();
        return new Matrix4f(r.getStepX(), r.getStepY(), r.getStepZ(), 0,
                0, 1, 0, 0,
                b.getStepX(), b.getStepY(), b.getStepZ(), 0,
                0, 0, 0, 1);
    }

    /** A machine-space point (blocks from the controller's centre) in world space. */
    public static Vec3 toWorld(Vec3 controllerCentre, Direction front, double right, double up, double back) {
        if (front.getAxis() == Direction.Axis.Y) front = Direction.NORTH;
        Direction r = front.getClockWise(), b = front.getOpposite();
        return controllerCentre.add(r.getStepX() * right + b.getStepX() * back, up, r.getStepZ() * right + b.getStepZ() * back);
    }

    /** Machine space round controllerCentre, relative to the camera (for a PoseStack drawn under the view matrix). */
    public Matrix4f machineLocal(Vec3 controllerCentre, Direction front) {
        return new Matrix4f().translate((float) (controllerCentre.x - cam.x), (float) (controllerCentre.y - cam.y), (float) (controllerCentre.z - cam.z))
                .mul(basis(front));
    }

    /** The model-view matrix for drawing in machine space round controllerCentre. */
    public Matrix4f machine(Vec3 controllerCentre, Direction front) {
        return new Matrix4f(view).translate((float) (controllerCentre.x - cam.x), (float) (controllerCentre.y - cam.y), (float) (controllerCentre.z - cam.z))
                .mul(basis(front));
    }
}
