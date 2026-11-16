package com.theoreticalsciences.orbit;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.12.0: the arithmetic of the Mission Planner, shared by the server (which charges for trips) and the client (which
 * draws the porkchop plot). All in m/s and Minecraft days.
 * <ul>
 *   <li><b>Windows.</b> A destination's planets line up every synodic period S. Leaving at phase φ of it (φ = 0 in the
 *   middle of the window) costs the base Δv times 1 + p·sin²(πφ): the base inside the window, up to (1 + p) times it
 *   halfway between windows.</li>
 *   <li><b>Time of flight.</b> Taking a fraction f of the Hohmann time costs 1 + 2.5·(ln f)² times more: faster or slower than
 *   the ellipse that just touches both orbits wastes propellant. Together they make the porkchop plot's valleys.</li>
 *   <li><b>Staging.</b> Stages carried add their Δv (each burns and is dropped); a trip takes the fewest that pay for it.</li>
 * </ul>
 */
public final class MissionMath {
    private MissionMath() {}

    static double frac(double x) {
        return x - Math.floor(x);
    }

    /** Where in its cycle a destination's windows stand, so they do not all open at once. */
    public static double offset(String id, double synodic) {
        return synodic <= 0 ? 0 : Math.floorMod(id.hashCode(), 1000) / 1000.0 * synodic;
    }

    public static double windowFactor(double synodic, double penalty, double offset, double day) {
        if (synodic <= 0 || penalty <= 0) return 1;
        double s = Math.sin(Math.PI * frac((day + offset) / synodic));
        return 1 + penalty * s * s;
    }

    public static double tofFactor(double f) {
        double l = Math.log(f);
        return 1 + 2.5 * l * l;
    }

    public static double cost(double base, double synodic, double penalty, double offset, double day, double tof) {
        return base * windowFactor(synodic, penalty, offset, day) * tofFactor(tof);
    }

    /** Days until the window opens (0 if it is open now: within 10% of the base). */
    public static double daysToWindow(double synodic, double penalty, double offset, double day) {
        if (synodic <= 0 || penalty <= 0.1) return 0;
        double w = Math.asin(Math.sqrt(0.1 / penalty)) / Math.PI;           // the window's half-width, as a phase
        double ph = frac((day + offset) / synodic);
        if (ph <= w || ph >= 1 - w) return 0;
        return (1 - w - ph) * synodic;
    }

    /** A mission's round trip in ticks: there and back on its transfer orbit, the longest (Saturn's, six years) a day. */
    public static int missionTicks(double transferDays, double tof) {
        return (int) Math.max(1200, 2 * transferDays * tof * 24000 / 4400.0);
    }

    public static double day(long gameTime) {
        return gameTime / 24000.0;
    }

    // ------------------------------------------------------------------ staging
    public record Plan(List<Integer> slots, double dv) {}

    /** The Δv of a stack, if it is a Transfer Stage (else 0). */
    public static double stageDv(ItemStack s) {
        return s.getItem() instanceof SpaceRegistry.TransferStageItem t ? t.dv() : 0;
    }

    /** What the best three stages in these stacks give together. */
    public static double best(List<ItemStack> stacks) {
        List<Double> u = units(stacks, null);
        u.sort((a, b) -> Double.compare(b, a));
        double sum = 0;
        for (int i = 0; i < Math.min(3, u.size()); i++) sum += u.get(i);
        return sum;
    }

    private static List<Double> units(List<ItemStack> stacks, List<Integer> slotOf) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            double dv = stageDv(stacks.get(i));
            if (dv <= 0) continue;
            for (int k = 0; k < Math.min(3, stacks.get(i).getCount()); k++) {
                out.add(dv);
                if (slotOf != null) slotOf.add(i);
            }
        }
        return out;
    }

    /** The fewest stages (at most three) whose Δv adds up to the cost, and of those the least Δv spent; null if none do. */
    public static Plan choose(List<ItemStack> stacks, double cost) {
        if (cost <= 0) return new Plan(List.of(), 0);
        List<Integer> slot = new ArrayList<>();
        List<Double> u = units(stacks, slot);
        Plan best = null;
        int n = u.size();
        for (int a = 0; a < n; a++) {
            if (u.get(a) >= cost) best = better(best, new Plan(List.of(slot.get(a)), u.get(a)));
            for (int b = a + 1; b < n; b++) {
                double ab = u.get(a) + u.get(b);
                if (ab >= cost) best = better(best, new Plan(List.of(slot.get(a), slot.get(b)), ab));
                for (int c = b + 1; c < n; c++) {
                    double abc = ab + u.get(c);
                    if (abc >= cost) best = better(best, new Plan(List.of(slot.get(a), slot.get(b), slot.get(c)), abc));
                }
            }
        }
        return best;
    }

    private static Plan better(Plan a, Plan b) {
        if (a == null) return b;
        if (b.slots().size() != a.slots().size()) return b.slots().size() < a.slots().size() ? b : a;
        return b.dv() < a.dv() ? b : a;
    }
}
