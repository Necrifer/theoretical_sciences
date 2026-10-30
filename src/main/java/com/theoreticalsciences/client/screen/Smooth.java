package com.theoreticalsciences.client.screen;

/**
 * Round 28: values on screen glide to where they are set instead of jumping there. Each shown value follows its
 * real value with an exponential ease (about 90% of the way in a fifth of a second), however the real value
 * changed - a click, a shift-click of ten steps, the wheel, a typed number or the server.
 */
public final class Smooth {
    // one clock per screen: each screen calls frame() once per frame it draws
    /** How fast shown values catch up (per second). */
    public static final double RATE = 12;
    private long last = System.nanoTime();
    private double dt;

    /** Call once per frame before using {@link #toward}. */
    public void frame() {
        long now = System.nanoTime();
        dt = Math.min(0.1, (now - last) / 1e9);
        last = now;
    }

    /** The next shown value on the way from {@code shown} to {@code target}. */
    public double toward(double shown, double target) {
        if (!Double.isFinite(shown) || !Double.isFinite(target)) return target;
        double k = 1 - Math.exp(-RATE * dt);
        double v = shown + (target - shown) * k;
        double span = Math.max(Math.abs(target), 1e-9);
        return Math.abs(v - target) <= span * 1e-4 ? target : v;
    }

    /** Same, on a log scale (for values that span many powers of ten): glides by ratio, not by difference. */
    public double towardLog(double shown, double target) {
        if (shown <= 0 || target <= 0) return toward(shown, target);
        return Math.exp(toward(Math.log(shown), Math.log(target)));
    }

}
