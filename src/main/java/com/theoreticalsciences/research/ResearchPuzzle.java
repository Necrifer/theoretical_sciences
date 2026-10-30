package com.theoreticalsciences.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * A research node's derivation puzzle (round 13). Step 1: the player fills a TEMPLATE of the full relation - real
 * notation built from {@link FormExpr} - with tiles (glyphs). The law is not named; any arrangement that is the same
 * expression up to commutation is accepted. Step 2: the law's name is revealed and the player computes a number from
 * the given values with {@code expr} (the old numeric grammar below).
 * Tiles are glyphs that are unlocked by progression ({@link GlyphRegistry}).
 */
public record ResearchPuzzle(String goal, String name, String lhs, String form, String expr, String ask, Map<String, String> vars,
                             Map<String, Double> given, List<String> distractors, String method, String unit,
                             Map<String, List<Double>> vary) {

    public static final Codec<ResearchPuzzle> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("goal").forGetter(ResearchPuzzle::goal),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ResearchPuzzle::name),
            Codec.STRING.fieldOf("lhs").forGetter(ResearchPuzzle::lhs),
            Codec.STRING.optionalFieldOf("form", "").forGetter(ResearchPuzzle::form),
            Codec.STRING.fieldOf("expr").forGetter(ResearchPuzzle::expr),
            Codec.STRING.optionalFieldOf("ask", "").forGetter(ResearchPuzzle::ask),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("vars", Map.of()).forGetter(ResearchPuzzle::vars),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("given", Map.of()).forGetter(ResearchPuzzle::given),
            Codec.STRING.listOf().optionalFieldOf("distractors", List.of()).forGetter(ResearchPuzzle::distractors),
            Codec.STRING.optionalFieldOf("method", "").forGetter(ResearchPuzzle::method),
            Codec.STRING.optionalFieldOf("unit", "").forGetter(ResearchPuzzle::unit),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE.listOf()).optionalFieldOf("vary", Map.of()).forGetter(ResearchPuzzle::vary)
    ).apply(i, ResearchPuzzle::new));

    /**
     * The given values in one world (round 22). Each value with a {@code vary} range [low, high, step] is drawn from
     * the world's own seed, so every world asks with different numbers and answers cannot be looked up; constants of
     * nature have no range and stay as they are. A draw that would make the question meaningless (no finite answer,
     * the answer changing sign, or wildly out of scale) is drawn again; after enough tries the book values are used.
     */
    public Map<String, Double> given(long salt, String id) {
        if (vary.isEmpty() || salt == 0) return given;
        double base;
        try {
            base = answer(given);
        } catch (RuntimeException e) {
            return given;
        }
        long seed = salt ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
        for (int attempt = 0; attempt < 32; attempt++) {
            Random r = new Random(seed + attempt * 0x632BE59BD9B4E019L);
            Map<String, Double> out = new LinkedHashMap<>();
            for (var e : given.entrySet()) {
                List<Double> v = vary.get(e.getKey());
                if (v == null || v.size() < 3 || !(v.get(2) > 0) || !(v.get(1) >= v.get(0))) {
                    out.put(e.getKey(), e.getValue());
                    continue;
                }
                double lo = v.get(0), step = v.get(2);
                long n = (long) Math.floor((v.get(1) - lo) / step + 1e-9);
                long k = n <= 0 ? 0 : Math.floorMod(r.nextLong(), n + 1);
                // exact decimal arithmetic: the same digits on every machine, always on the step grid
                java.math.BigDecimal st = java.math.BigDecimal.valueOf(step);
                out.put(e.getKey(), java.math.BigDecimal.valueOf(lo).add(st.multiply(java.math.BigDecimal.valueOf(k))).doubleValue());
            }
            try {
                double a = answer(out);
                if (a != 0 && Math.signum(a) == Math.signum(base) && (base == 0 || Math.abs(StrictMath.log10(Math.abs(a / base))) <= 3))
                    return Collections.unmodifiableMap(out);
            } catch (RuntimeException ignored) {
            }
        }
        return given;
    }

    /** The question's text with this world's values written in ({@code @{name}} in the data). */
    public static String fill(String text, Map<String, Double> values) {
        if (text.indexOf("@{") < 0) return text;
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int s = text.indexOf("@{", i);
            int e = s < 0 ? -1 : text.indexOf('}', s);
            if (s < 0 || e < 0) {
                b.append(text, i, text.length());
                break;
            }
            b.append(text, i, s);
            Double v = values.get(text.substring(s + 2, e));
            b.append(v == null ? text.substring(s + 2, e) : show(v));
            i = e + 1;
        }
        return b.toString();
    }

    /** A number as people write it: 1.602e-19, 0.5, 7000. */
    public static String show(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e9) return String.valueOf((long) v);
        double a = Math.abs(v);
        String s = a >= 1e-3 && a < 1e6 ? String.format(Locale.ROOT, "%.6f", v) : String.format(Locale.ROOT, "%.5e", v);
        int e = s.indexOf('e');
        String m = e < 0 ? s : s.substring(0, e), x = e < 0 ? "" : s.substring(e);
        if (m.contains(".")) m = m.replaceAll("0+$", "").replaceAll("\\.$", "");
        if (!x.isEmpty()) x = x.replace("e+0", "e").replace("e-0", "e-").replace("e+", "e");
        return m + x;
    }

    public double answer(Map<String, Double> values) {
        return eval(List.of(expr.trim().split("\\s+")), values);
    }

    public boolean answerOk(double x, Map<String, Double> values) {
        double a = answer(values);
        return Math.abs(x - a) <= 0.01 * Math.abs(a) + 1e-9;
    }

    /** The template's tokens (an old-style puzzle without a form uses its numeric expression). */
    public List<String> formTokens() {
        return FormExpr.tokens(form.isEmpty() ? expr : form);
    }

    public List<String> lhsTokens() {
        return FormExpr.tokens(lhs);
    }

    /** Positions in {@link #formTokens()} that the player fills. */
    public List<Integer> slotPositions() {
        return FormExpr.slots(formTokens());
    }

    /** The tiles: what the slots need, plus the distractors, in a stable shuffled order. */
    public List<String> tiles(String seed) {
        List<String> f = formTokens();
        List<String> t = new ArrayList<>();
        for (int k : slotPositions()) t.add(f.get(k));
        t.addAll(distractors);
        Collections.shuffle(t, new Random(seed.hashCode()));
        return t;
    }

    /** Every glyph the derivation needs (the template's own symbols and every tile it needs). */
    public Set<String> requiredGlyphs() {
        Set<String> s = new HashSet<>(FormExpr.glyphs(formTokens()));
        s.addAll(FormExpr.glyphs(lhsTokens()));
        return s;
    }

    /**
     * True if {@code placed} is the template with the slots filled from the tiles: identical structure tokens, and
     * each tile used at most as often as it is offered.
     */
    public boolean fitsTemplate(List<String> placed) {
        List<String> f = formTokens();
        if (placed.size() != f.size()) return false;
        Set<Integer> sl = new HashSet<>(slotPositions());
        Map<String, Integer> left = new HashMap<>();
        for (int k : sl) left.merge(f.get(k), 1, Integer::sum);
        for (String d : distractors) left.merge(d, 1, Integer::sum);
        for (int i = 0; i < f.size(); i++) {
            if (!sl.contains(i)) {
                if (!f.get(i).equals(placed.get(i))) return false;
                continue;
            }
            int n = left.getOrDefault(placed.get(i), 0);
            if (n <= 0) return false;
            left.put(placed.get(i), n - 1);
        }
        return true;
    }

    /** The same expression up to commutation of sums and products. */
    public boolean equivalent(List<String> placed) {
        try {
            return FormExpr.canon(FormExpr.parse(placed)).equals(FormExpr.canon(FormExpr.parse(formTokens())));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public double answer() {
        return eval(List.of(expr.trim().split("\\s+")), given);
    }

    public boolean answerOk(double x) {
        double a = answer();
        return Math.abs(x - a) <= 0.01 * Math.abs(a) + 1e-9;
    }

    // ------------------------------------------------------------------ the grammar
    public static double eval(List<String> toks, Map<String, Double> env) {
        Parser p = new Parser(toks, env);
        double v = p.expr();
        if (p.i != toks.size()) throw new IllegalArgumentException("trailing tokens");
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new ArithmeticException("not finite");
        return v;
    }

    static final class Parser {
        final List<String> t;
        final Map<String, Double> env;
        int i;

        Parser(List<String> t, Map<String, Double> env) {
            this.t = t;
            this.env = env;
        }

        String peek() {
            return i < t.size() ? t.get(i) : null;
        }

        String eat(String x) {
            String s = peek();
            if (s == null || (x != null && !x.equals(s))) throw new IllegalArgumentException("expected " + x);
            i++;
            return s;
        }

        double expr() {
            double v = term();
            while ("+".equals(peek()) || "-".equals(peek())) v = "+".equals(eat(null)) ? v + term() : v - term();
            return v;
        }

        double term() {
            double v = unary();
            while ("*".equals(peek()) || "/".equals(peek())) v = "*".equals(eat(null)) ? v * unary() : v / unary();
            return v;
        }

        double unary() {
            if ("-".equals(peek())) {
                eat(null);
                return -unary();
            }
            return power();
        }

        double power() {
            double b = atom();
            if ("^".equals(peek())) {
                eat(null);
                double e = unary();
                double v = StrictMath.pow(b, e);
                if (Double.isNaN(v)) throw new ArithmeticException("bad power");
                return v;
            }
            return b;
        }

        double atom() {
            String s = peek();
            if (s == null) throw new IllegalArgumentException("unexpected end");
            if (s.matches("\\d+(\\.\\d+)?")) {
                eat(null);
                return Double.parseDouble(s);
            }
            if (s.equals("π")) {
                eat(null);
                return Math.PI;
            }
            if (s.equals("(")) {
                eat(null);
                double v = expr();
                eat(")");
                return v;
            }
            switch (s) {
                case "sqrt", "ln", "exp", "sin", "cos" -> {
                    eat(null);
                    eat("(");
                    double a = expr();
                    eat(")");
                    double v = switch (s) {
                        case "sqrt" -> StrictMath.sqrt(a);
                        case "ln" -> StrictMath.log(a);
                        case "exp" -> StrictMath.exp(a);
                        case "sin" -> StrictMath.sin(a);
                        default -> StrictMath.cos(a);
                    };
                    if (Double.isNaN(v)) throw new ArithmeticException("domain");
                    return v;
                }
                default -> {
                }
            }
            if ("+-*/^)".contains(s) && s.length() == 1) throw new IllegalArgumentException("unexpected " + s);
            Double v = env.get(s);
            if (v == null) throw new IllegalArgumentException("unknown " + s);
            eat(null);
            return v;
        }
    }

    /** How a token is drawn on a tile. */
    public static String display(String tok) {
        return switch (tok) {
            case "*" -> "×";
            case "/" -> "÷";
            case "-" -> "−";
            case "sqrt" -> "√";
            default -> tok;
        };
    }
}
