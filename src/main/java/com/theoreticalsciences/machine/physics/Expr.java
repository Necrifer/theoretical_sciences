package com.theoreticalsciences.machine.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A compiled arithmetic expression over named variables (the language of the machine physics models).
 * Grammar:
 * <pre>
 *   expr  := term (('+'|'-') term)*
 *   term  := unary (('*'|'/') unary)*
 *   unary := '-' unary | power
 *   power := atom ('^' unary)?
 *   atom  := number | name | name '(' expr (',' expr)* ')' | '(' expr ')'
 * </pre>
 * Numbers may use exponents (6.67e-11). Names are compiled to slots of a {@code double[]} so evaluating every tick is
 * cheap. Functions: sqrt cbrt ln log10 exp sin cos tan atan tanh abs min max clamp pos step.
 * Anything not finite evaluates to NaN, which the model treats as "the physics breaks down here".
 */
public abstract class Expr {
    public abstract double eval(double[] env);

    public static Expr parse(String src, Map<String, Integer> slots) {
        P p = new P(lex(src), slots, src);
        Expr e = p.expr();
        if (p.i != p.t.size()) throw new IllegalArgumentException("trailing '" + p.t.get(p.i) + "' in: " + src);
        return e;
    }

    // ------------------------------------------------------------------ lexer
    static List<String> lex(String s) {
        List<String> out = new ArrayList<>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int j = i;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                if (j < n && (s.charAt(j) == 'e' || s.charAt(j) == 'E')) {
                    int k = j + 1;
                    if (k < n && (s.charAt(k) == '+' || s.charAt(k) == '-')) k++;
                    if (k < n && Character.isDigit(s.charAt(k))) {
                        j = k;
                        while (j < n && Character.isDigit(s.charAt(j))) j++;
                    }
                }
                out.add(s.substring(i, j));
                i = j;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) j++;
                out.add(s.substring(i, j));
                i = j;
            } else if ("+-*/^(),".indexOf(c) >= 0) {
                out.add(String.valueOf(c));
                i++;
            } else throw new IllegalArgumentException("bad character '" + c + "' in: " + s);
        }
        return out;
    }

    // ------------------------------------------------------------------ parser
    private static final class P {
        final List<String> t;
        final Map<String, Integer> slots;
        final String src;
        int i;

        P(List<String> t, Map<String, Integer> slots, String src) {
            this.t = t;
            this.slots = slots;
            this.src = src;
        }

        String peek() {
            return i < t.size() ? t.get(i) : null;
        }

        void eat(String x) {
            if (!x.equals(peek())) throw new IllegalArgumentException("expected '" + x + "' in: " + src);
            i++;
        }

        Expr expr() {
            Expr v = term();
            while ("+".equals(peek()) || "-".equals(peek())) {
                boolean plus = t.get(i++).equals("+");
                Expr a = v, b = term();
                v = plus ? new Bin(a, b, '+') : new Bin(a, b, '-');
            }
            return v;
        }

        Expr term() {
            Expr v = unary();
            while ("*".equals(peek()) || "/".equals(peek())) {
                char op = t.get(i++).charAt(0);
                v = new Bin(v, unary(), op);
            }
            return v;
        }

        Expr unary() {
            if ("-".equals(peek())) {
                i++;
                Expr a = unary();
                return new Fn("neg", new Expr[]{a});
            }
            return power();
        }

        Expr power() {
            Expr b = atom();
            if ("^".equals(peek())) {
                i++;
                return new Bin(b, unary(), '^');
            }
            return b;
        }

        Expr atom() {
            String s = peek();
            if (s == null) throw new IllegalArgumentException("unexpected end of: " + src);
            if (Character.isDigit(s.charAt(0)) || s.charAt(0) == '.') {
                i++;
                return new Num(Double.parseDouble(s));
            }
            if (s.equals("(")) {
                i++;
                Expr v = expr();
                eat(")");
                return v;
            }
            if (Character.isLetter(s.charAt(0)) || s.charAt(0) == '_') {
                i++;
                if ("(".equals(peek())) {
                    i++;
                    List<Expr> args = new ArrayList<>();
                    args.add(expr());
                    while (",".equals(peek())) {
                        i++;
                        args.add(expr());
                    }
                    eat(")");
                    int want = arity(s);
                    if (want < 0) throw new IllegalArgumentException("unknown function " + s + " in: " + src);
                    if (want != args.size()) throw new IllegalArgumentException(s + " takes " + want + " arguments in: " + src);
                    return new Fn(s, args.toArray(new Expr[0]));
                }
                if (s.equals("pi")) return new Num(Math.PI);
                Integer slot = slots.get(s);
                if (slot == null) throw new IllegalArgumentException("unknown name '" + s + "' in: " + src);
                return new Var(slot);
            }
            throw new IllegalArgumentException("unexpected '" + s + "' in: " + src);
        }
    }

    static int arity(String f) {
        return switch (f) {
            case "sqrt", "cbrt", "ln", "log10", "exp", "sin", "cos", "tan", "atan", "tanh", "abs", "pos", "step" -> 1;
            case "min", "max" -> 2;
            case "clamp" -> 3;
            default -> -1;
        };
    }

    // ------------------------------------------------------------------ nodes
    static final class Num extends Expr {
        final double v;

        Num(double v) {
            this.v = v;
        }

        @Override
        public double eval(double[] env) {
            return v;
        }
    }

    static final class Var extends Expr {
        final int slot;

        Var(int slot) {
            this.slot = slot;
        }

        @Override
        public double eval(double[] env) {
            return env[slot];
        }
    }

    static final class Bin extends Expr {
        final Expr a, b;
        final char op;

        Bin(Expr a, Expr b, char op) {
            this.a = a;
            this.b = b;
            this.op = op;
        }

        @Override
        public double eval(double[] env) {
            double x = a.eval(env), y = b.eval(env);
            return switch (op) {
                case '+' -> x + y;
                case '-' -> x - y;
                case '*' -> x * y;
                case '/' -> x / y;
                default -> Math.pow(x, y);
            };
        }
    }

    static final class Fn extends Expr {
        final String f;
        final Expr[] args;

        Fn(String f, Expr[] args) {
            this.f = f;
            this.args = args;
        }

        @Override
        public double eval(double[] env) {
            double x = args[0].eval(env);
            return switch (f) {
                case "neg" -> -x;
                case "sqrt" -> Math.sqrt(x);
                case "cbrt" -> Math.cbrt(x);
                case "ln" -> Math.log(x);
                case "log10" -> Math.log10(x);
                case "exp" -> Math.exp(x);
                case "sin" -> Math.sin(x);
                case "cos" -> Math.cos(x);
                case "tan" -> Math.tan(x);
                case "atan" -> Math.atan(x);
                case "tanh" -> Math.tanh(x);
                case "abs" -> Math.abs(x);
                case "pos" -> Math.max(0, x);
                case "step" -> x >= 0 ? 1 : 0;
                case "min" -> Math.min(x, args[1].eval(env));
                case "max" -> Math.max(x, args[1].eval(env));
                case "clamp" -> Math.max(args[1].eval(env), Math.min(args[2].eval(env), x));
                default -> Double.NaN;
            };
        }
    }
}
