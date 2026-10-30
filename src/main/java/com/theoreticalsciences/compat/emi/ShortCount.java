package com.theoreticalsciences.compat.emi;

/** Round 35: a count short enough to sit under a slot (12k, 1.5M) - shared by the JEI and EMI pages. */
public final class ShortCount {
    public static String of(long n) {
        if (n < 1000) return Long.toString(n);
        String[] u = {"k", "M", "G", "T"};
        double v = n;
        int i = -1;
        while (v >= 1000 && i < u.length - 1) {
            v /= 1000;
            i++;
        }
        return (v >= 100 || v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v)) + u[i];
    }

    private ShortCount() {}
}
