package com.theoreticalsciences.science;

import com.mojang.serialization.Codec;

import java.util.Locale;

/**
 * Round 38 (1.2): the isotopes radiation comes from, with their real half-lives and what they give off. Long-lived ones
 * (uranium, thorium, radium) are steady enough to sit in items; short-lived ones (radon, Tc-99m) only live in tanks,
 * where the Decay Chamber lets them decay on a (scaled) real clock.
 */
public enum Isotope {
    //                half-life (s)        main radiation     what one becomes
    U238("Uranium-238",    1.410e17, Kind.ALPHA,   "Thorium-234 (… → Lead-206)"),
    U235("Uranium-235",    2.221e16, Kind.ALPHA,   "Thorium-231 (… → Lead-207)"),
    TH232("Thorium-232",   4.434e17, Kind.ALPHA,   "Radium-228 (… → Lead-208)"),
    RA226("Radium-226",    5.049e10, Kind.ALPHA,   "Radon-222"),
    RN222("Radon-222",     3.304e5,  Kind.ALPHA,   "Polonium-218 (… → Lead-210)"),
    PB210("Lead-210",      7.006e8,  Kind.BETA,    "Bismuth-210 → Polonium-210"),
    PO210("Polonium-210",  1.196e7,  Kind.ALPHA,   "Lead-206 (stable)"),
    CS137("Caesium-137",   9.492e8,  Kind.GAMMA,   "Barium-137 (stable)"),
    CO60("Cobalt-60",      1.663e8,  Kind.GAMMA,   "Nickel-60 (stable)"),
    TC99M("Technetium-99m", 2.163e4, Kind.GAMMA,   "Technetium-99"),
    H3("Tritium",          3.888e8,  Kind.BETA,    "Helium-3 (stable)"),
    PU239("Plutonium-239", 7.609e11, Kind.ALPHA,   "Uranium-235"),
    CF252("Californium-252", 8.338e7, Kind.NEUTRON, "Curium-248 (or fission)"),
    HF178M2("Hafnium-178m2", 9.783e8, Kind.GAMMA, "Hafnium-178 (stable), all at once if triggered"),
    /** The uranium-238 chain in balance, as in an ore: its radium and lead-214 daughters give off gamma rays. */
    U238_CHAIN("Uranium ore (U-238 chain)", 1.410e17, Kind.GAMMA, "Lead-206, through 13 steps");

    public enum Kind {
        /** Helium nuclei: stopped by paper or skin, so only harmful when carried or breathed in. */
        ALPHA,
        /** Electrons: stopped by any solid block. */
        BETA,
        /** Photons: pass through walls, falling by e^(−μx) through each. */
        GAMMA,
        /** Neutrons: pass through metal; slowed by water and hydrogen-rich blocks. */
        NEUTRON
    }

    public static final Codec<Isotope> CODEC = Codec.STRING.comapFlatMap(s -> {
        for (Isotope i : values()) if (i.name().equalsIgnoreCase(s)) return com.mojang.serialization.DataResult.success(i);
        return com.mojang.serialization.DataResult.error(() -> "unknown isotope " + s);
    }, i -> i.name().toLowerCase(Locale.ROOT));

    public final String label;
    public final double halfLifeSeconds;
    public final Kind kind;
    public final String daughter;

    Isotope(String label, double halfLifeSeconds, Kind kind, String daughter) {
        this.label = label;
        this.halfLifeSeconds = halfLifeSeconds;
        this.kind = kind;
        this.daughter = daughter;
    }

    /** λ = ln 2 / T½, per second. */
    public double decayConstant() {
        return Math.log(2) / halfLifeSeconds;
    }

    /** "1.6 thousand years", "3.8 days" - for tooltips and the Codex. */
    public String halfLifeText() {
        double s = halfLifeSeconds;
        double y = s / 3.156e7;
        if (y >= 1e9) return String.format(Locale.ROOT, "%.2f billion years", y / 1e9);
        if (y >= 1e6) return String.format(Locale.ROOT, "%.0f million years", y / 1e6);
        if (y >= 1e3) return String.format(Locale.ROOT, "%.1f thousand years", y / 1e3);
        if (y >= 1) return String.format(Locale.ROOT, "%.1f years", y);
        if (s >= 86400) return String.format(Locale.ROOT, "%.1f days", s / 86400);
        return String.format(Locale.ROOT, "%.1f hours", s / 3600);
    }
}
