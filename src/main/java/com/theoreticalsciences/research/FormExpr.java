package com.theoreticalsciences.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The FORM grammar of research derivations: real mathematical notation - integrals,
 * derivatives, ∇, vectors, complex conjugates, bra-kets, sums, limits - written as space-separated tokens. Parsed to a
 * tree whose nodes remember the token they came from, so a template can put a box exactly where each tile goes.
 * <p>
 * Two expressions are the same derivation when their {@link #canon canonical forms} match: sums and ordinary / dot
 * products compare as unordered collections, a − b is a + (−b), everything else is positional.
 */
public final class FormExpr {
    public static final Set<String> FUNCS = Set.of("sin", "cos", "tan", "exp", "ln", "log", "sinh", "cosh", "tanh", "arcsin", "arccos", "arctan",
            "Re", "Im", "det", "tr", "erf", "Γ", "arg");
    public static final Map<String, int[]> HEADS = Map.ofEntries(
            Map.entry("int", new int[]{2, 4}), Map.entry("oint", new int[]{2}), Map.entry("iint", new int[]{2}), Map.entry("iiint", new int[]{2}),
            Map.entry("sum", new int[]{4}), Map.entry("prod", new int[]{4}), Map.entry("sumk", new int[]{2}), Map.entry("lim", new int[]{3}),
            Map.entry("d", new int[]{2}), Map.entry("d2", new int[]{2}), Map.entry("pd", new int[]{2}), Map.entry("pd2", new int[]{2}), Map.entry("pdm", new int[]{3}),
            Map.entry("grad", new int[]{1}), Map.entry("div", new int[]{1}), Map.entry("curl", new int[]{1}), Map.entry("lap", new int[]{1}), Map.entry("dal", new int[]{1}),
            Map.entry("vec", new int[]{1}), Map.entry("hat", new int[]{1}), Map.entry("bar", new int[]{1}), Map.entry("tdot", new int[]{1}),
            Map.entry("conj", new int[]{1}), Map.entry("dag", new int[]{1}), Map.entry("abs", new int[]{1}), Map.entry("norm", new int[]{1}),
            Map.entry("bra", new int[]{1}), Map.entry("ket", new int[]{1}), Map.entry("braket", new int[]{2}), Map.entry("mel", new int[]{3}),
            Map.entry("avg", new int[]{1}), Map.entry("comm", new int[]{2}), Map.entry("acomm", new int[]{2}),
            Map.entry("sqrt", new int[]{1}), Map.entry("root", new int[]{2}), Map.entry("binom", new int[]{2}), Map.entry("hc", new int[]{0}));
    private static final Set<String> PUNCT = Set.of("(", ")", ",", "^", "_", "!", "/", "*", "·", "×", "+", "-");

    public enum Kind {NUM, LEAF, NEG, ADD, SUB, MUL, DOT, CROSS, DIV, POW, SUBSCRIPT, FACT, FN, HEAD, PAR}

    /** A tree node. {@code tok} is the index of the token that is this node's slot (leaf, number, function name, or binary ±), else −1. */
    public record Node(Kind kind, String s, int tok, List<Node> kids) {
        public Node a() {
            return kids.get(0);
        }

        public Node b() {
            return kids.get(1);
        }
    }

    public static boolean isNumber(String t) {
        return t.matches("\\d+(\\.\\d+)?");
    }

    public static String kind(String t) {
        if (isNumber(t)) return "num";
        if (PUNCT.contains(t)) return "punct";
        if (FUNCS.contains(t)) return "func";
        if (HEADS.containsKey(t)) return "head";
        return "leaf";
    }

    // ------------------------------------------------------------------ parser
    private static final class P {
        final List<String> t;
        int i;

        P(List<String> t) {
            this.t = t;
        }

        String peek() {
            return i < t.size() ? t.get(i) : null;
        }

        String eat(String x) {
            String s = peek();
            if (s == null || (x != null && !x.equals(s))) throw new IllegalArgumentException("expected " + x + " at " + i + ", found " + s);
            i++;
            return s;
        }

        Node expr() {
            Node v = term();
            while ("+".equals(peek()) || "-".equals(peek())) {
                int at = i;
                boolean plus = eat(null).equals("+");
                v = new Node(plus ? Kind.ADD : Kind.SUB, plus ? "+" : "-", at, List.of(v, term()));
            }
            return v;
        }

        Node term() {
            Node v = unary();
            while (true) {
                String s = peek();
                if (s == null) return v;
                Kind k = switch (s) {
                    case "*" -> Kind.MUL;
                    case "/" -> Kind.DIV;
                    case "·" -> Kind.DOT;
                    case "×" -> Kind.CROSS;
                    default -> null;
                };
                if (k == null) return v;
                eat(null);
                v = new Node(k, s, -1, List.of(v, unary()));
            }
        }

        Node unary() {
            if ("-".equals(peek())) {
                eat(null);
                return new Node(Kind.NEG, "-", -1, List.of(unary()));
            }
            return postfix();
        }

        Node postfix() {
            Node v = atom();
            while ("^".equals(peek()) || "_".equals(peek()) || "!".equals(peek())) {
                String op = eat(null);
                if (op.equals("^")) v = new Node(Kind.POW, "^", -1, List.of(v, unary()));
                else if (op.equals("_")) v = new Node(Kind.SUBSCRIPT, "_", -1, List.of(v, atom()));
                else v = new Node(Kind.FACT, "!", -1, List.of(v));
            }
            return v;
        }

        Node atom() {
            String s = peek();
            if (s == null) throw new IllegalArgumentException("unexpected end");
            String k = kind(s);
            int at = i;
            switch (k) {
                case "num" -> {
                    eat(null);
                    return new Node(Kind.NUM, s, at, List.of());
                }
                case "func" -> {
                    eat(null);
                    eat("(");
                    Node v = expr();
                    eat(")");
                    return new Node(Kind.FN, s, at, List.of(v));
                }
                case "head" -> {
                    eat(null);
                    eat("(");
                    List<Node> args = new ArrayList<>();
                    if (!")".equals(peek())) {
                        args.add(expr());
                        while (",".equals(peek())) {
                            eat(null);
                            args.add(expr());
                        }
                    }
                    eat(")");
                    int[] ok = HEADS.get(s);
                    boolean good = false;
                    for (int n : ok) good |= n == args.size();
                    if (!good) throw new IllegalArgumentException(s + " takes " + Arrays.toString(ok) + " arguments");
                    return new Node(Kind.HEAD, s, -1, List.copyOf(args));
                }
                case "leaf" -> {
                    eat(null);
                    return new Node(Kind.LEAF, s, at, List.of());
                }
                default -> {
                }
            }
            if (s.equals("(")) {
                eat(null);
                Node v = expr();
                eat(")");
                return new Node(Kind.PAR, "(", -1, List.of(v));
            }
            throw new IllegalArgumentException("unexpected " + s + " at " + i);
        }
    }

    public static Node parse(List<String> toks) {
        P p = new P(toks);
        Node n = p.expr();
        if (p.i != toks.size()) throw new IllegalArgumentException("trailing tokens at " + p.i);
        return n;
    }

    public static Node tryParse(List<String> toks) {
        try {
            return toks.isEmpty() ? null : parse(toks);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static List<String> tokens(String src) {
        String s = src.trim();
        return s.isEmpty() ? List.of() : List.of(s.split("\\s+"));
    }

    // ------------------------------------------------------------------ canonical form
    public static String canon(Node n) {
        switch (n.kind()) {
            case PAR:
                return canon(n.a());
            case NUM:
            case LEAF:
                return n.s();
            case ADD:
            case SUB:
            case NEG: {
                List<String> terms = new ArrayList<>();
                sum(n, 1, terms);
                if (terms.size() == 1 && terms.get(0).startsWith("+")) return terms.get(0).substring(1);
                terms.sort(null);
                return "S[" + String.join(",", terms) + "]";
            }
            case MUL: {
                List<Node> fs = new ArrayList<>();
                flatten(n, Kind.MUL, fs);
                List<String> c = new ArrayList<>();
                for (Node f : fs) c.add(canon(f));
                c.sort(null);
                return "P[" + String.join(",", c) + "]";
            }
            case DOT: {
                List<String> c = new ArrayList<>(List.of(canon(n.a()), canon(n.b())));
                c.sort(null);
                return "D[" + String.join(",", c) + "]";
            }
            case CROSS:
                return "X[" + canon(n.a()) + "," + canon(n.b()) + "]";
            case DIV:
                return "Q[" + canon(n.a()) + "," + canon(n.b()) + "]";
            case POW:
                return "W[" + canon(n.a()) + "," + canon(n.b()) + "]";
            case SUBSCRIPT:
                return "U[" + canon(n.a()) + "," + canon(n.b()) + "]";
            case FACT:
                return "F[" + canon(n.a()) + "]";
            case FN:
                return n.s() + "(" + canon(n.a()) + ")";
            case HEAD: {
                List<String> c = new ArrayList<>();
                for (Node k : n.kids()) c.add(canon(k));
                return n.s() + "{" + String.join(";", c) + "}";
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void flatten(Node n, Kind k, List<Node> out) {
        if (n.kind() == k) {
            flatten(n.a(), k, out);
            flatten(n.b(), k, out);
        } else if (n.kind() == Kind.PAR && n.a().kind() == k) flatten(n.a(), k, out);
        else out.add(n);
    }

    private static void sum(Node n, int sign, List<String> out) {
        switch (n.kind()) {
            case PAR -> {
                Kind k = n.a().kind();
                if (k == Kind.ADD || k == Kind.SUB || k == Kind.NEG) sum(n.a(), sign, out);
                else out.add((sign > 0 ? "+" : "-") + canon(n));
            }
            case ADD -> {
                sum(n.a(), sign, out);
                sum(n.b(), sign, out);
            }
            case SUB -> {
                sum(n.a(), sign, out);
                sum(n.b(), -sign, out);
            }
            case NEG -> sum(n.a(), -sign, out);
            default -> out.add((sign > 0 ? "+" : "-") + canon(n));
        }
    }

    // ------------------------------------------------------------------ slots
    /** Token positions that are boxes in the template: leaves, numbers, function names and binary + / −. */
    public static List<Integer> slots(List<String> toks) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < toks.size(); i++) {
            String t = toks.get(i), k = kind(t);
            if (k.equals("num") || k.equals("leaf") || k.equals("func")) out.add(i);
            else if (t.equals("+") || t.equals("-")) {
                if (i == 0) continue;
                String prev = toks.get(i - 1), pk = kind(prev);
                if (pk.equals("num") || pk.equals("leaf") || prev.equals(")") || prev.equals("!")) out.add(i);
            }
        }
        return out;
    }

    /** Glyphs a form needs (numbers and bare punctuation excepted). */
    public static Set<String> glyphs(List<String> toks) {
        Set<String> out = new HashSet<>();
        for (String t : toks) if (!t.equals("(") && !t.equals(")") && !t.equals(",") && !isNumber(t)) out.add(t);
        return out;
    }

    private FormExpr() {}
}
