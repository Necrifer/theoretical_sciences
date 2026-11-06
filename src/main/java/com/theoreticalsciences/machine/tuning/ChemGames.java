package com.theoreticalsciences.machine.tuning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.2.0: the chemistry calibration games (see {@link CalibrationGames}). Four kinds, each built from (seed, tier) on
 * both sides:
 * <ul>
 *   <li>BALANCE: give an equation coefficients so atoms (and, for ionic and redox equations, charge) balance.</li>
 *   <li>BUILD: write the equation yourself: pick the reactants and products from a pool that has wrong species in it,
 *       then balance it. Precipitation reactions ask for the net ionic equation, so the spectator ions stay out.</li>
 *   <li>OXIDATION: give the oxidation state of the marked element in a few compounds and ions.</li>
 *   <li>EQUILIBRIUM: write the equilibrium constant: each species' power in K (products up, reactants down, pure solids
 *       and liquids left out).</li>
 * </ul>
 * Species are written like {@code MnO4^-(aq)}, {@code Fe^2+(aq)}, {@code H2O(l)}: formula, then ^charge, then (phase).
 */
public final class ChemGames {
    private ChemGames() {}

    public static final int MAX_COEF = 14;

    /** One species in an equation: what it is, its phase ("s", "l", "g", "aq") and its coefficient. */
    public record Species(String formula, String phase, int coef, boolean product) {
        public int charge() {
            return ChemGames.charge(formula);
        }
    }

    /** id (for its name), the species, whether it is an equilibrium, and for a precipitation the two salts mixed and the spectators. */
    public record Reaction(String id, List<Species> species, boolean equilibrium, String[] mixed, String[] spectators) {
        public int reactants() {
            int n = 0;
            for (Species s : species) if (!s.product()) n++;
            return n;
        }
    }

    /** Parses "2 H2(g) + 1 O2(g) -> 2 H2O(l)" ("<=>" for an equilibrium). */
    static Reaction parse(String id, String eq, String[] mixed, String[] spect) {
        boolean eqm = eq.contains("<=>");
        String[] sides = eq.split(eqm ? "<=>" : "->");
        List<Species> out = new ArrayList<>();
        for (int side = 0; side < 2; side++)
            for (String term : sides[side].trim().split(" \\+ ")) {
                term = term.trim();
                int sp = term.indexOf(' ');
                int coef = Integer.parseInt(term.substring(0, sp));
                String f = term.substring(sp + 1).trim(), phase = "";
                int p = f.lastIndexOf('(');
                if (p > 0 && f.endsWith(")") && f.substring(p + 1, f.length() - 1).matches("s|l|g|aq")) {
                    phase = f.substring(p + 1, f.length() - 1);
                    f = f.substring(0, p);
                }
                out.add(new Species(f, phase, coef, side == 1));
            }
        return new Reaction(id, out, eqm, mixed, spect);
    }

    private static Reaction r(String id, String eq) {
        return parse(id, eq, null, null);
    }

    private static Reaction ppt(String id, String a, String b, String eq, String... spect) {
        return parse(id, eq, new String[]{a, b}, spect);
    }

    /** Molecular equations, easier first. */
    public static final Reaction[] MOLECULAR = {
            r("water", "2 H2(g) + 1 O2(g) -> 2 H2O(l)"),
            r("haber", "1 N2(g) + 3 H2(g) <=> 2 NH3(g)"),
            r("salt", "2 Na(s) + 1 Cl2(g) -> 2 NaCl(s)"),
            r("magnesium_acid", "1 Mg(s) + 2 HCl(aq) -> 1 MgCl2(aq) + 1 H2(g)"),
            r("peroxide", "2 H2O2(aq) -> 2 H2O(l) + 1 O2(g)"),
            r("contact", "2 SO2(g) + 1 O2(g) <=> 2 SO3(g)"),
            r("methane", "1 CH4(g) + 2 O2(g) -> 1 CO2(g) + 2 H2O(g)"),
            r("chlorate", "2 KClO3(s) -> 2 KCl(s) + 3 O2(g)"),
            r("rust", "4 Fe(s) + 3 O2(g) -> 2 Fe2O3(s)"),
            r("alumina", "4 Al(s) + 3 O2(g) -> 2 Al2O3(s)"),
            r("limestone_acid", "1 CaCO3(s) + 2 HCl(aq) -> 1 CaCl2(aq) + 1 H2O(l) + 1 CO2(g)"),
            r("blast_furnace", "1 Fe2O3(s) + 3 CO(g) -> 2 Fe(s) + 3 CO2(g)"),
            r("propane", "1 C3H8(g) + 5 O2(g) -> 3 CO2(g) + 4 H2O(g)"),
            r("nitric", "3 NO2(g) + 1 H2O(l) -> 2 HNO3(aq) + 1 NO(g)"),
            r("ethane", "2 C2H6(g) + 7 O2(g) -> 4 CO2(g) + 6 H2O(g)"),
            r("ostwald", "4 NH3(g) + 5 O2(g) <=> 4 NO(g) + 6 H2O(g)"),
            r("respiration", "1 C6H12O6(s) + 6 O2(g) -> 6 CO2(g) + 6 H2O(l)"),
            r("copper_nitric", "3 Cu(s) + 8 HNO3(aq) -> 3 Cu(NO3)2(aq) + 2 NO(g) + 4 H2O(l)"),
    };

    /** Ionic and redox equations: charge must balance too. */
    public static final Reaction[] IONIC = {
            r("neutralisation", "1 H^+(aq) + 1 OH^-(aq) -> 1 H2O(l)"),
            r("zinc_copper", "1 Zn(s) + 1 Cu^2+(aq) -> 1 Zn^2+(aq) + 1 Cu(s)"),
            r("copper_silver", "1 Cu(s) + 2 Ag^+(aq) -> 1 Cu^2+(aq) + 2 Ag(s)"),
            r("halogen", "1 Cl2(aq) + 2 Br^-(aq) -> 2 Cl^-(aq) + 1 Br2(aq)"),
            r("iron_iodide", "2 Fe^3+(aq) + 2 I^-(aq) -> 2 Fe^2+(aq) + 1 I2(aq)"),
            r("permanganate", "1 MnO4^-(aq) + 5 Fe^2+(aq) + 8 H^+(aq) -> 1 Mn^2+(aq) + 5 Fe^3+(aq) + 4 H2O(l)"),
            r("dichromate", "1 Cr2O7^2-(aq) + 6 Fe^2+(aq) + 14 H^+(aq) -> 2 Cr^3+(aq) + 6 Fe^3+(aq) + 7 H2O(l)"),
    };

    /** Equilibria (for the K game), easier first. */
    public static final Reaction[] EQUILIBRIA = {
            r("dinitrogen_tetroxide", "1 N2O4(g) <=> 2 NO2(g)"),
            r("hydrogen_iodide", "1 H2(g) + 1 I2(g) <=> 2 HI(g)"),
            r("haber", "1 N2(g) + 3 H2(g) <=> 2 NH3(g)"),
            r("lime_kiln", "1 CaCO3(s) <=> 1 CaO(s) + 1 CO2(g)"),
            r("water_gas_shift", "1 CO(g) + 1 H2O(g) <=> 1 CO2(g) + 1 H2(g)"),
            r("acetic_acid", "1 CH3COOH(aq) <=> 1 CH3COO^-(aq) + 1 H^+(aq)"),
            r("ammonia_base", "1 NH3(aq) + 1 H2O(l) <=> 1 NH4^+(aq) + 1 OH^-(aq)"),
            r("silver_chloride", "1 AgCl(s) <=> 1 Ag^+(aq) + 1 Cl^-(aq)"),
            r("contact", "2 SO2(g) + 1 O2(g) <=> 2 SO3(g)"),
            r("phosphorus_pentachloride", "1 PCl5(g) <=> 1 PCl3(g) + 1 Cl2(g)"),
            r("thiocyanate", "1 Fe^3+(aq) + 1 SCN^-(aq) <=> 1 FeSCN^2+(aq)"),
            r("ostwald", "4 NH3(g) + 5 O2(g) <=> 4 NO(g) + 6 H2O(g)"),
    };

    /** Precipitations: two salt solutions mixed; the answer is the net ionic equation. */
    public static final Reaction[] PRECIPITATION = {
            ppt("silver_chloride_ppt", "AgNO3", "NaCl", "1 Ag^+(aq) + 1 Cl^-(aq) -> 1 AgCl(s)", "Na^+", "NO3^-"),
            ppt("barium_sulfate_ppt", "BaCl2", "Na2SO4", "1 Ba^2+(aq) + 1 SO4^2-(aq) -> 1 BaSO4(s)", "Na^+", "Cl^-"),
            ppt("calcium_carbonate_ppt", "CaCl2", "Na2CO3", "1 Ca^2+(aq) + 1 CO3^2-(aq) -> 1 CaCO3(s)", "Na^+", "Cl^-"),
            ppt("lead_iodide_ppt", "Pb(NO3)2", "KI", "1 Pb^2+(aq) + 2 I^-(aq) -> 1 PbI2(s)", "K^+", "NO3^-"),
            ppt("copper_hydroxide_ppt", "CuSO4", "NaOH", "1 Cu^2+(aq) + 2 OH^-(aq) -> 1 Cu(OH)2(s)", "Na^+", "SO4^2-"),
            ppt("iron_hydroxide_ppt", "FeCl3", "NaOH", "1 Fe^3+(aq) + 3 OH^-(aq) -> 1 Fe(OH)3(s)", "Na^+", "Cl^-"),
    };

    /** Compounds and ions with one element marked, and its oxidation state; easier first. */
    public static final String[][] OXIDATION = {
            {"H2O", "O", "-2"}, {"NH3", "N", "-3"}, {"CO2", "C", "4"}, {"N2", "N", "0"}, {"CH4", "C", "-4"}, {"SO2", "S", "4"},
            {"Fe2O3", "Fe", "3"}, {"HNO3", "N", "5"}, {"H2SO4", "S", "6"}, {"NO2", "N", "4"}, {"Cu2O", "Cu", "1"}, {"PbO2", "Pb", "4"},
            {"H2O2", "O", "-1"}, {"NaH", "H", "-1"}, {"NH4^+", "N", "-3"}, {"ClO^-", "Cl", "1"}, {"ClO3^-", "Cl", "5"},
            {"H3PO4", "P", "5"}, {"KMnO4", "Mn", "7"}, {"MnO4^-", "Mn", "7"}, {"K2Cr2O7", "Cr", "6"}, {"Cr2O7^2-", "Cr", "6"},
            {"S2O3^2-", "S", "2"}, {"OF2", "O", "2"},
    };

    // ------------------------------------------------------------------ formulas
    /** The charge written after ^ (MnO4^- = -1, Fe^2+ = +2), or 0. */
    public static int charge(String f) {
        int i = f.indexOf('^');
        if (i < 0) return 0;
        String c = f.substring(i + 1);
        int sign = c.endsWith("-") ? -1 : 1;
        String n = c.replace("+", "").replace("-", "");
        return sign * (n.isEmpty() ? 1 : Integer.parseInt(n));
    }

    /** Element counts (charge and phase ignored). */
    public static Map<String, Integer> atoms(String f) {
        int i = f.indexOf('^');
        return CalibrationGames.atoms(i < 0 ? f : f.substring(0, i));
    }

    /** Atoms and charge left over (reactants minus products) for these coefficients; all zero when balanced. */
    public static Map<String, Integer> net(List<Species> sp, int[] coef) {
        Map<String, Integer> net = new LinkedHashMap<>();
        for (int s = 0; s < sp.size(); s++) {
            int sign = sp.get(s).product() ? -1 : 1;
            for (var e : atoms(sp.get(s).formula()).entrySet()) net.merge(e.getKey(), sign * coef[s] * e.getValue(), Integer::sum);
            int q = sp.get(s).charge();
            if (q != 0) net.merge("charge", sign * coef[s] * q, Integer::sum);
        }
        return net;
    }

    private static int unbalanced(List<Species> sp, int[] coef) {
        int n = 0;
        for (int v : net(sp, coef).values()) if (v != 0) n++;
        return n;
    }

    // ------------------------------------------------------------------ BALANCE
    /** The equation to balance: molecular ones first, ionic and redox ones from stage 3. */
    public static Reaction balance(long seed, int tier) {
        Random r = new Random(seed);
        List<Reaction> pool = new ArrayList<>();
        int lo = Math.max(0, Math.min(MOLECULAR.length - 6, (tier - 2) * 3)), hi = Math.min(MOLECULAR.length, 6 + 3 * tier);
        for (int i = lo; i < hi; i++) pool.add(MOLECULAR[i]);
        if (tier >= 3) for (int i = 0; i < Math.min(IONIC.length, 2 + 2 * (tier - 2)); i++) pool.add(IONIC[i]);
        return pool.get(r.nextInt(pool.size()));
    }

    public static int balanceErrors(long seed, int tier, int[] a) {
        Reaction rx = balance(seed, tier);
        if (a.length != rx.species().size()) return 99;
        for (int v : a) if (v < 1 || v > MAX_COEF) return 99;
        return unbalanced(rx.species(), a);
    }

    // ------------------------------------------------------------------ BUILD
    /** The reaction to write, and the pool of species offered (the right ones and some wrong ones, shuffled). */
    public record Build(Reaction reaction, List<String> pool) {}

    public static Build build(long seed, int tier) {
        Random r = new Random(seed);
        Reaction rx;
        List<String> wrong = new ArrayList<>();
        if (tier >= 2 && r.nextInt(3) == 0 || tier >= 4 && r.nextBoolean()) {
            rx = PRECIPITATION[r.nextInt(PRECIPITATION.length)];
            wrong.addAll(List.of(rx.spectators()));
            String other = PRECIPITATION[r.nextInt(PRECIPITATION.length)].species().get(r.nextInt(2)).formula();
            wrong.add(other);
            wrong.add(rx.mixed()[r.nextInt(2)]);                         // the whole salt: not part of the net ionic equation
        } else {
            int hi = Math.min(MOLECULAR.length, 5 + 3 * tier);
            rx = MOLECULAR[r.nextInt(hi)];
            for (int k = 0; k < 2 + Math.min(2, tier / 2); k++) {
                Reaction o = MOLECULAR[r.nextInt(MOLECULAR.length)];
                wrong.add(o.species().get(r.nextInt(o.species().size())).formula());
            }
        }
        List<String> pool = new ArrayList<>();
        for (Species s : rx.species()) if (!pool.contains(s.formula())) pool.add(s.formula());
        for (String w : wrong) if (!pool.contains(w)) pool.add(w);
        java.util.Collections.shuffle(pool, r);
        return new Build(rx, pool);
    }

    /** The answer: for each pool species, its side (0 unused, 1 reactant, 2 product), then its coefficient. */
    public static int buildErrors(long seed, int tier, int[] a) {
        Build b = build(seed, tier);
        int n = b.pool().size();
        if (a.length != 2 * n) return 99;
        int wrong = 0;
        List<Species> sp = new ArrayList<>();
        List<Integer> coef = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String f = b.pool().get(i);
            int want = 0;
            for (Species s : b.reaction().species()) if (s.formula().equals(f)) want = s.product() ? 2 : 1;
            if (a[i] != want) wrong++;
            if (a[i] != 0) {
                int c = a[n + i];
                if (c < 1 || c > MAX_COEF) return 99;
                sp.add(new Species(f, "", c, a[i] == 2));
                coef.add(c);
            }
        }
        if (wrong > 0) return wrong;
        return unbalanced(sp, coef.stream().mapToInt(Integer::intValue).toArray());
    }

    // ------------------------------------------------------------------ OXIDATION
    /** Which compounds (indices into OXIDATION) to ask about. */
    public static int[] oxidation(long seed, int tier) {
        Random r = new Random(seed);
        int n = Math.min(4, 2 + tier / 2);
        int hi = Math.min(OXIDATION.length, 8 + 4 * tier);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            int v;
            boolean dup;
            do {
                v = r.nextInt(hi);
                dup = false;
                for (int k = 0; k < i; k++) dup |= out[k] == v;
            } while (dup);
            out[i] = v;
        }
        return out;
    }

    public static int oxidationErrors(long seed, int tier, int[] a) {
        int[] q = oxidation(seed, tier);
        if (a.length != q.length) return 99;
        int wrong = 0;
        for (int i = 0; i < q.length; i++) if (a[i] != Integer.parseInt(OXIDATION[q[i]][2])) wrong++;
        return wrong;
    }

    // ------------------------------------------------------------------ EQUILIBRIUM
    public static Reaction equilibrium(long seed, int tier) {
        int hi = Math.min(EQUILIBRIA.length, 4 + 2 * tier);
        return EQUILIBRIA[new Random(seed).nextInt(hi)];
    }

    /** A species' power in K: its coefficient, up for products and down for reactants, none for a pure solid or liquid. */
    public static int power(Species s) {
        if (s.phase().equals("s") || s.phase().equals("l")) return 0;
        return s.product() ? s.coef() : -s.coef();
    }

    public static int equilibriumErrors(long seed, int tier, int[] a) {
        Reaction rx = equilibrium(seed, tier);
        if (a.length != rx.species().size()) return 99;
        int wrong = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != power(rx.species().get(i))) wrong++;
        return wrong;
    }

    // ------------------------------------------------------------------ display
    /** H2O -> H₂O, Fe^2+ -> Fe²⁺. */
    public static String pretty(String f) {
        StringBuilder sb = new StringBuilder();
        boolean sup = false;
        for (char c : f.toCharArray()) {
            if (c == '^') { sup = true; continue; }
            if (sup) sb.append(switch (c) {
                case '+' -> '⁺';
                case '-' -> '⁻';
                case '1' -> '¹';
                case '2' -> '²';
                case '3' -> '³';
                default -> (char) ('⁰' + (c - '0'));
            });
            else sb.append(Character.isDigit(c) ? (char) ('₀' + (c - '0')) : c);
        }
        return sb.toString();
    }
}
