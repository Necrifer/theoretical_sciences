package com.theoreticalsciences.boss;

import java.util.List;

/**
 * Alpha Omega's examination. Every question has exactly one right answer (listed first here; the order is shuffled
 * when it is asked). Grouped by the chamber of the Living Proof that asks them; the boss itself draws from all of them.
 */
public final class ProofQuestions {
    public record Q(String prompt, String right, String w1, String w2, String w3) {
        public List<String> answers() {
            return List.of(right, w1, w2, w3);
        }
    }

    /** Chamber 4 - Hilbert's Hotel: infinities. */
    public static final List<Q> TRANSFINITE = List.of(
            new Q("Every room of Hilbert's Hotel is taken. One new guest arrives. Where do they sleep?",
                    "Room 1 - every guest moves from room n to n+1", "Nowhere: the hotel is full", "Room ∞", "They share someone's room"),
            new Q("The full hotel now meets a coach with infinitely many new guests. Which rooms do they get?",
                    "The odd rooms - every guest moves from n to 2n", "The even rooms", "None: there is no room left", "Room ω and beyond"),
            new Q("Which set is bigger: all counting numbers, or only the even ones?",
                    "Neither - both have size ℵ₀", "The counting numbers", "The even numbers", "They cannot be compared"),
            new Q("What comes right after every counting number 1, 2, 3, ...?",
                    "ω (omega), the first infinite ordinal", "∞ + 1", "ℵ₁", "Nothing: they never end"),
            new Q("Can the real numbers be listed one after another, like the counting numbers?",
                    "No - the diagonal argument always finds a missing one", "Yes, given infinite time", "Yes, by size", "Only the rational ones are missing"),
            new Q("Is 1 + ω equal to ω + 1?",
                    "No: 1 + ω = ω, but ω + 1 is larger than ω", "Yes, addition always commutes", "Both equal ω", "Both equal ω + 1"),
            new Q("What does Cantor's diagonal argument prove?", "The real numbers cannot be listed - there are more of them than counting numbers",
                    "Every infinite set is the same size", "The rationals are uncountable", "Infinity is a number"),
            new Q("Which is bigger: the rational numbers or the counting numbers?", "Neither - both are countable (ℵ₀)", "The rationals",
                    "The counting numbers", "They cannot be compared"),
            new Q("Is there a largest ordinal?", "No - add one to any ordinal and you get a bigger one", "Yes, ω", "Yes, ε₀", "Yes, ℵ₁"),
            new Q("What is an inaccessible cardinal?",
                    "An infinity too big to reach from smaller ones by sums and power sets", "An infinity no one has named yet",
                    "A number larger than every ordinal", "The size of the real numbers"));

    /** Chamber 5 - the Platonic Forms: geometry, shape and higher dimensions. */
    public static final List<Q> FORMS = List.of(
            new Q("How many perfectly regular convex solids - Platonic solids - exist?", "Five", "Four", "Seven", "Infinitely many"),
            new Q("For any convex polyhedron, vertices − edges + faces equals:", "2", "0", "1", "It depends on the shape"),
            new Q("After infinitely many steps, the Koch snowflake has:", "An infinite perimeter but a finite area", "A finite perimeter and area",
                    "An infinite area", "No area at all"),
            new Q("A tesseract is to a cube as a cube is to a:", "Square", "Sphere", "Point", "Triangle"),
            new Q("How many corners (vertices) does a tesseract have?", "16", "8", "12", "24"),
            new Q("What makes a Klein bottle strange?", "It has only one side and no edge", "It has infinite volume",
                    "It is hollow in four places", "It cannot be drawn at all"),
            new Q("A fractal's dimension can be:", "A non-whole number, like 1.26", "Only 1, 2 or 3", "Always larger than 3", "Negative"),
            new Q("The golden ratio φ satisfies:", "φ² = φ + 1", "φ² = 2", "φ = π / 2", "φ + 1 = 0"),
            new Q("A Möbius strip has how many sides?", "One", "Two", "Zero", "Infinitely many"),
            new Q("What is a Menger sponge's volume after infinitely many steps?", "Zero - yet its surface area is infinite", "One cube",
                    "Infinite", "Half the original"),
            new Q("How many regular polytopes exist in four dimensions?", "Six", "Five", "Three", "Infinitely many"));

    /** Chamber 6 - the Bulk: strings, branes, extra dimensions, relativity. */
    public static final List<Q> BULK = List.of(
            new Q("Why might gravity be so much weaker than the other forces?", "It leaks into extra dimensions of the bulk",
                    "Gravitons are very heavy", "It only works on large objects", "It is slowly decaying"),
            new Q("Where are string theory's extra dimensions hidden?", "Curled up too small to see - compactified",
                    "Inside black holes", "Beyond the edge of the universe", "In the past"),
            new Q("In string theory, what is a brane?", "A surface in higher-dimensional space on which open strings end",
                    "A particle of gravity", "A kind of black hole", "A loop of time"),
            new Q("One twin flies near light speed and returns. Who has aged less?", "The twin who travelled", "The twin who stayed",
                    "Neither - they age the same", "Whoever is heavier"),
            new Q("What does a geodesic describe?", "The straightest possible path through curved spacetime",
                    "The shortest path on a flat map", "The orbit of a photon around a star", "The edge of a black hole"),
            new Q("Seen from far away, someone falling into a black hole appears to:", "Slow down, redden and freeze at the horizon",
                    "Speed up and vanish", "Bounce back out", "Turn blue and explode"),
            new Q("A white hole is:", "The time-reverse of a black hole: nothing can get in", "A black hole made of light",
                    "A star made of antimatter", "A hole in the Bulk"),
            new Q("What is the speed of light in every inertial frame?", "The same", "Faster for the faster observer",
                    "Slower in moving frames", "Infinite"),
            new Q("Why do clocks on GPS satellites run fast compared with clocks on the ground?", "They sit higher in Earth's gravity well",
                    "They are colder", "Their orbit is retrograde", "Radio waves speed them up"),
            new Q("What does E = mc² say?", "Mass and energy are equivalent", "Energy grows with the speed of light",
                    "Mass can never change", "Light has mass"),
            new Q("In general relativity, what tells spacetime how to curve?", "Matter and energy - the stress-energy tensor",
                    "The speed of light", "Magnetic fields", "The observer's choice of coordinates"));

    /** Chamber 7 - Foundations: quantum mechanics, information and logic. */
    public static final List<Q> FOUNDATIONS = List.of(
            new Q("In the double-slit experiment, what destroys the interference pattern?", "Measuring which slit each particle went through",
                    "Turning off the lights", "Firing particles faster", "Using electrons instead of photons"),
            new Q("If a particle's position is known more precisely, its momentum becomes:", "Less certain", "More certain", "Zero", "Unchanged"),
            new Q("Two entangled particles are measured light-years apart. Can this send a message?", "No - each result alone looks random",
                    "Yes, instantly", "Yes, at light speed", "Only if they are photons"),
            new Q("What does a system's Hamiltonian give you?", "Its energy - and so how its state changes in time",
                    "Its position", "Its mass", "Its temperature only"),
            new Q("Why can't Maxwell's demon beat the second law of thermodynamics?", "Erasing its memory produces entropy",
                    "Demons do not exist", "Molecules are too fast to sort", "The box always leaks"),
            new Q("Gödel showed that any consistent system rich enough for arithmetic:", "Contains true statements it cannot prove",
                    "Can prove every true statement", "Must contradict itself", "Cannot count past infinity"),
            new Q("What is the Planck length?", "The scale below which spacetime itself stops being smooth",
                    "The size of an atom", "The width of a proton", "The distance light travels in a second"),
            new Q("What is quantum tunnelling?", "Passing through a barrier you classically lack the energy to cross",
                    "Travelling faster than light", "Splitting into two particles", "Digging with a qubit"),
            new Q("What did Landauer show about erasing one bit of information?", "It must release at least kT·ln 2 of heat",
                    "It is free", "It creates a bit elsewhere", "It is impossible"),
            new Q("What does the Pauli exclusion principle forbid?", "Two identical fermions sharing one quantum state",
                    "Two photons in one place", "Particles faster than light", "Measuring spin"),
            new Q("Virtual particles in the vacuum:", "Borrow energy briefly and vanish again", "Are just measurement errors",
                    "Travel faster than light", "Are all made of dark matter"));

    /** Chamber 8 - the Codex: what the player has learned in this mod. */
    public static final List<Q> CODEX = List.of(
            new Q("What must you carry to survive inside a black hole?", "A Geodesic Anchor", "A Brane Tether", "A Cantor Key", "Nothing - just armour"),
            new Q("What lies at the very bottom of the Quantum Foam?", "Schrödinger's Limit", "Relativity Breakdown", "Bedrock", "The void"),
            new Q("What is Pleroma made from?", "Every other fluid, all at once", "Liquid light", "Dark energy and water", "Molten Omnium"),
            new Q("Which instrument opens the Transfinite Hierarchy?", "The Cantor Key", "The Horizon Diver", "The Brane Shifter", "The Planck Diver"),
            new Q("Inside the white hole, which way does gravity pull?", "Up - sneak to fall back down", "Down, as usual", "Toward the centre",
                    "Nowhere: there is none"),
            new Q("In the Platonic Realm, what can still dig?", "Only a sword", "Only a pickaxe", "Only your hand", "Nothing at all"),
            new Q("What did the Einstein-Rosen bridge connect?", "The black hole and the white hole", "The Bulk and the Overworld",
                    "The Foam and the Transfinite", "The Nether and the End"),
            new Q("Which dimension has five spatial dimensions by nature?", "The Bulk", "The Quantum Foam", "The Overworld", "The white hole"),
            new Q("How much study time does one Empirical Dossier save?", "Two minutes", "Twenty minutes", "One hour", "Five seconds"),
            new Q("How far does a Field Projector's field reach?", "16 blocks around it", "4 blocks", "The whole dimension", "Only its own block"),
            new Q("What happens to a block fractured by reality destabilisation?", "It heals back into what it was", "It turns to air forever",
                    "It becomes an ore", "It explodes"),
            new Q("Whose bite stacks Infinite Regress?", "The Ordinal Hound", "The Planck Mite", "The Tidal Horror", "The Photon Moth"),
            new Q("What keeps you whole in the Quantum Foam?", "A Coherence Stabiliser", "A Geodesic Anchor", "A Well-Ordering Charm", "A Brane Tether"),
            new Q("In the Platonic Realm, what can dig?", "Only a sword", "Only a pickaxe", "Any tool", "Nothing"),
            new Q("What speeds up a long study in the Research Matrix?", "A Research Nexus fed with Research Dossiers", "Waiting in bed",
                    "A Spectral Analyzer", "Nothing can"));

    // ================================================================== actual mathematics, generated fresh every time
    static String num(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9) return String.valueOf((long) Math.rint(d));
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    /** Builds a question from a right number and plausible wrong numbers (duplicates replaced by nearby values). */
    static Q numeric(String prompt, double right, double... wrong) {
        java.util.LinkedHashSet<String> w = new java.util.LinkedHashSet<>();
        String r = num(right);
        for (double x : wrong) if (!num(x).equals(r)) w.add(num(x));
        double step = Math.abs(right) < 2 ? 0.1 : Math.max(1, Math.abs(right) * 0.25);
        for (int k = 1; w.size() < 3; k++) {
            String c = num(right + k * step);
            if (!c.equals(r)) w.add(c);
        }
        java.util.Iterator<String> it = w.iterator();
        return new Q(prompt, r, it.next(), it.next(), it.next());
    }

    static long choose(int n, int k) {
        long c = 1;
        for (int i = 0; i < k; i++) c = c * (n - i) / (i + 1);
        return c;
    }

    static Q transfiniteMath(java.util.Random r) {
        switch (r.nextInt(4)) {
            case 0 -> {
                int n = 3 + r.nextInt(6);
                return numeric("A set has " + n + " elements. How many subsets does it have (the size of its power set)?", Math.pow(2, n), n * n, 2 * n, Math.pow(2, n) - 1);
            }
            case 1 -> {
                int n = 5 + r.nextInt(6), k = 2 + r.nextInt(3);
                return numeric("In how many ways can you choose " + k + " things from " + n + " (order does not matter)?", choose(n, k), (double) n * k,
                        choose(n, k) * 2, Math.pow(n, k));
            }
            case 2 -> {
                int n = 2 + r.nextInt(8);
                return new Q("Simplify the ordinal sum " + n + " + ω.", "ω", "ω + " + n, "ω · " + n, "ω^" + n);
            }
            default -> {
                int n = 2 + r.nextInt(8);
                return new Q("Which is larger: ω + " + n + " or " + n + " + ω?", "ω + " + n, n + " + ω", "They are equal", "Neither is defined");
            }
        }
    }

    static Q formsMath(java.util.Random r) {
        switch (r.nextInt(5)) {
            case 0 -> {
                int[] ns = {5, 6, 8, 9, 10, 12};
                int n = ns[r.nextInt(ns.length)];
                double a = (n - 2) * 180.0 / n;
                return numeric("What is each interior angle of a regular " + n + "-sided polygon, in degrees?", a, 360.0 / n, a + 10, 180 - 360.0 / n + 20);
            }
            case 1 -> {
                int[][] solids = {{8, 12}, {6, 12}, {20, 30}, {12, 30}, {4, 6}, {6, 9}, {10, 15}, {12, 18}};
                int[] v = solids[r.nextInt(solids.length)];
                int f = 2 - v[0] + v[1];
                return numeric("A convex polyhedron has " + v[0] + " vertices and " + v[1] + " edges. How many faces does it have?", f, f + 2, v[1] - v[0], f - 2);
            }
            case 2 -> {
                int n = 3 + r.nextInt(5);
                return numeric("How many vertices does a " + n + "-dimensional cube have?", Math.pow(2, n), 2 * n, n * n, Math.pow(2, n - 1));
            }
            case 3 -> {
                int n = 3 + r.nextInt(4);
                double e = n * Math.pow(2, n - 1);
                return numeric("How many edges does a " + n + "-dimensional cube have?", e, Math.pow(2, n), 2 * n * n, e / 2);
            }
            default -> {
                int[][] fr = {{3, 2}, {4, 3}, {20, 3}, {8, 3}, {5, 3}};
                int[] f = fr[r.nextInt(fr.length)];
                double d = Math.log(f[0]) / Math.log(f[1]);
                return numeric("A fractal is made of " + f[0] + " copies of itself, each scaled down by 1/" + f[1] + ". What is its dimension (log N / log s)?",
                        d, (double) f[0] / f[1], d + 0.5, Math.max(0.5, d - 0.6));
            }
        }
    }

    static Q bulkMath(java.util.Random r) {
        double[][] g = {{0.6, 1.25}, {0.8, 5.0 / 3}, {0.28, 1 / Math.sqrt(1 - 0.28 * 0.28)}, {0.96, 1 / Math.sqrt(1 - 0.96 * 0.96)}, {0.866, 2.0}};
        switch (r.nextInt(4)) {
            case 0 -> {
                double[] v = g[r.nextInt(g.length)];
                return numeric("A ship moves at " + num(v[0]) + "c. What is its Lorentz factor γ = 1/√(1 − v²/c²)?", v[1], 1 + v[0], 1 / v[0], v[1] * v[1]);
            }
            case 1 -> {
                double[] v = g[r.nextInt(2)];
                int t = 10 + 5 * r.nextInt(4);
                return numeric("A twin travels at " + num(v[0]) + "c (γ = " + num(v[1]) + ") while " + t + " years pass on Earth. How many years does the traveller age?",
                        t / v[1], t * v[1], t, t - v[0] * 10);
            }
            case 2 -> {
                int k = 2 + r.nextInt(6);
                return numeric("A black hole's mass is multiplied by " + k + ". By what factor does its event horizon's radius grow?", k, k * k, Math.sqrt(k), 1.0 / k);
            }
            default -> {
                int[] ds = {4, 5, 7};
                int n = ds[r.nextInt(ds.length)];
                return numeric("With " + n + " spatial dimensions, gravity falls off as 1/r^" + (n - 1) + ". Doubling the distance divides it by?",
                        Math.pow(2, n - 1), 4, Math.pow(2, n), n);
            }
        }
    }

    static Q foundationsMath(java.util.Random r) {
        switch (r.nextInt(4)) {
            case 0 -> {
                double[] ps = {0.36, 0.64, 0.25, 0.81, 0.5, 0.9};
                double p = ps[r.nextInt(ps.length)];
                return numeric("A qubit is α|0⟩ + β|1⟩ with |α|² = " + num(p) + ". What is the chance of measuring 1?", 1 - p, p, Math.sqrt(1 - p), 0.5);
            }
            case 1 -> {
                double[] as = {0.6, 0.8, 0.5, 0.3, 0.9};
                double a = as[r.nextInt(as.length)];
                return numeric("A qubit's amplitude for |0⟩ is α = " + num(a) + ". What is the probability of measuring 0?", a * a, a, 1 - a, 2 * a * a);
            }
            case 2 -> {
                int[] ns = {5, 9, 17, 33, 100, 1000, 300};
                int n = ns[r.nextInt(ns.length)];
                int b = (int) Math.ceil(Math.log(n) / Math.log(2));
                return numeric("How many bits are needed to give " + n + " different states each their own label?", b, b - 1, b + 2, n / 2);
            }
            default -> {
                int n = 3 + r.nextInt(8);
                return numeric(n + " qubits can be in a superposition of how many basis states at once?", Math.pow(2, n), n, 2 * n, n * n);
            }
        }
    }

    /**
     * Picks a question for a chamber: about half are generated calculations, the rest come from the bank. A question
     * already asked in this chamber is not asked again until the bank runs out.
     */
    public static Q pick(int chamber, java.util.Random r, java.util.Set<String> asked) {
        java.util.function.Function<java.util.Random, Q> gen = switch (chamber) {
            case 3 -> ProofQuestions::transfiniteMath;
            case 4 -> ProofQuestions::formsMath;
            case 5 -> ProofQuestions::bulkMath;
            case 6 -> ProofQuestions::foundationsMath;
            default -> null;
        };
        List<Q> bank = switch (chamber) {
            case 3 -> TRANSFINITE;
            case 4 -> FORMS;
            case 5 -> BULK;
            case 6 -> FOUNDATIONS;
            case 7 -> CODEX;
            default -> all();
        };
        if (gen != null && r.nextBoolean()) {
            for (int i = 0; i < 10; i++) {
                Q q = gen.apply(r);
                if (asked.add(q.prompt())) return q;
            }
        }
        List<Q> fresh = new java.util.ArrayList<>();
        for (Q q : bank) if (!asked.contains(q.prompt())) fresh.add(q);
        if (fresh.isEmpty()) {
            asked.clear();
            fresh.addAll(bank);
        }
        Q q = fresh.get(r.nextInt(fresh.size()));
        asked.add(q.prompt());
        return q;
    }

    public static List<Q> all() {
        java.util.ArrayList<Q> l = new java.util.ArrayList<>();
        l.addAll(TRANSFINITE);
        l.addAll(FORMS);
        l.addAll(BULK);
        l.addAll(FOUNDATIONS);
        l.addAll(CODEX);
        return l;
    }

    private ProofQuestions() {}
}
