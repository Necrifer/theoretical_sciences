package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.research.FormExpr;
import com.theoreticalsciences.research.FormExpr.Kind;
import com.theoreticalsciences.research.FormExpr.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Typesets research derivations the way a paper prints them: stacked fractions, raised exponents, radicals, big
 * integral and summation signs with their limits, d/dx and ∂/∂x, ∇ with ·/×, vector arrows, hats and bars, bras and
 * kets, brackets that grow with their contents. Variables are italic with trailing characters as subscripts.
 * <p>
 * In a template every SLOT (variables, numbers, function names, binary + and −; see {@link FormExpr#slots}) is drawn
 * by the caller's {@link SlotFn} - an empty box, or the tile that fills it. A long sum breaks over several lines.
 */
public final class MathTypeset {
    // ------------------------------------------------------------------ boxes
    /** A laid-out piece: width, height above the baseline, depth below it. */
    public abstract static class Box {
        public float w, asc, desc;

        abstract void draw(GuiGraphics g, Font f, float x, float base, int col);

        public float h() {
            return asc + desc;
        }
    }

    static final class Text extends Box {
        final Component c;
        final float s;

        Text(Font f, Component c, float s) {
            this.c = c;
            this.s = s;
            w = f.width(c) * s;
            asc = 7 * s;
            desc = 2 * s;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            g.pose().pushPose();
            g.pose().translate(x, base - 7 * s, 0);
            g.pose().scale(s, s, 1);
            g.drawString(f, c, 0, 0, col, false);
            g.pose().popPose();
        }
    }

    /** A glyph stretched vertically (big ∫, Σ, brackets). */
    static final class Tall extends Box {
        final String c;
        final float s, k;

        Tall(Font f, String c, float s, float k, float asc, float desc) {
            this.c = c;
            this.s = s;
            this.k = k;
            w = f.width(c) * s * Math.min(1.4F, Math.max(1F, k * 0.7F));
            this.asc = asc;
            this.desc = desc;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float top = base - asc, h = asc + desc;
            g.pose().pushPose();
            g.pose().translate(x, top, 0);
            g.pose().scale(w / Math.max(1, f.width(c)), h / 9F, 1);
            g.drawString(f, c, 0, 0, col, false);
            g.pose().popPose();
        }
    }

    /**
     * A big operator (∑, ∏, ∫, ∮...). It grows with what it applies to, but evenly in both directions and only up to
     * about twice the text size - as printed mathematics does - instead of being pulled into a thin sliver as tall as
     * the whole expression. It sits centred on the maths axis (the height of a minus sign).
     */
    static final class BigOp extends Box {
        final String c;
        final float u;

        BigOp(Font f, String c, float s, Box body, float lo, float hi) {
            this.c = c;
            float bodyH = Math.max(body.h(), 9 * s);
            float H = Math.max(lo * 9 * s, Math.min(hi * 9 * s, bodyH + 2 * s));
            u = H / 9F;
            w = f.width(c) * u;
            float axis = 3.5F * s;
            asc = H / 2 + axis;
            desc = Math.max(0, H / 2 - axis);
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            g.pose().pushPose();
            g.pose().translate(x, base - asc, 0);
            g.pose().scale(u, u, 1);
            g.drawString(f, c, 0, 0, col, false);
            g.pose().popPose();
        }
    }

    static final class Row extends Box {
        final List<Box> kids = new ArrayList<>();
        final List<Float> gaps = new ArrayList<>();

        Row add(Box b, float gap) {
            if (!kids.isEmpty()) w += gap;
            gaps.add(kids.isEmpty() ? 0F : gap);
            kids.add(b);
            w += b.w;
            asc = Math.max(asc, b.asc);
            desc = Math.max(desc, b.desc);
            return this;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            for (int k = 0; k < kids.size(); k++) {
                x += gaps.get(k);
                kids.get(k).draw(g, f, x, base, col);
                x += kids.get(k).w;
            }
        }
    }

    /** Lines of an equation stacked, the first carrying "lhs =", later ones indented under it. */
    static final class Lines extends Box {
        final List<Box> lines;
        final float indent, lead;

        Lines(List<Box> lines, float indent, float lead) {
            this.lines = lines;
            this.indent = indent;
            this.lead = lead;
            float hsum = 0;
            for (int i = 0; i < lines.size(); i++) {
                Box b = lines.get(i);
                w = Math.max(w, b.w + (i == 0 ? 0 : indent));
                hsum += b.h() + (i == 0 ? 0 : lead);
            }
            asc = lines.get(0).asc;
            desc = hsum - asc;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float y = base;
            for (int i = 0; i < lines.size(); i++) {
                Box b = lines.get(i);
                if (i > 0) y += lines.get(i - 1).desc + lead + b.asc;
                b.draw(g, f, x + (i == 0 ? 0 : indent), y, col);
            }
        }
    }

    /** Stacked fraction (or, without a rule, a binomial's stack). */
    static final class Frac extends Box {
        final Box n, d;
        final float axis;
        final boolean rule;

        Frac(Box n, Box d, float s, boolean rule) {
            this.n = n;
            this.d = d;
            this.rule = rule;
            axis = 3 * s;
            w = Math.max(n.w, d.w) + 4 * s;
            asc = axis + 1.5F * s + n.h();
            desc = Math.max(0, d.h() + 1.5F * s - axis);
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float bar = base - axis;
            float gap = 1.5F * axis / 3F;
            n.draw(g, f, x + (w - n.w) / 2, bar - gap - n.desc, col);
            d.draw(g, f, x + (w - d.w) / 2, bar + gap + d.asc, col);
            if (rule) rect(g, x + 1, bar - 0.5F, x + w - 1, bar + 0.5F, col);
        }
    }

    /** Superscript and/or subscript on a base. */
    static final class Scripts extends Box {
        final Box b;
        @Nullable final Box sup, sub;
        final float raise, drop;

        Scripts(Box b, @Nullable Box sup, @Nullable Box sub, float s) {
            this.b = b;
            this.sup = sup;
            this.sub = sub;
            raise = sup == null ? 0 : Math.max(b.asc - sup.asc * 0.4F, 4 * s);
            drop = sub == null ? 0 : Math.max(2.5F * s, b.desc + sub.asc * 0.3F - 1 * s);
            w = b.w + 0.5F * s + Math.max(sup == null ? 0 : sup.w, sub == null ? 0 : sub.w);
            asc = Math.max(b.asc, sup == null ? 0 : raise + sup.asc);
            desc = Math.max(b.desc, sub == null ? 0 : drop + sub.desc);
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            b.draw(g, f, x, base, col);
            if (sup != null) sup.draw(g, f, x + b.w + 0.5F, base - raise, col);
            if (sub != null) sub.draw(g, f, x + b.w + 0.5F, base + drop, col);
        }
    }

    /** Something with a limit stacked under it and/or over it (Σ, Π, lim). */
    static final class Limits extends Box {
        final Box op;
        @Nullable final Box under, over;
        final float gap;

        Limits(Box op, @Nullable Box under, @Nullable Box over, float s) {
            this.op = op;
            this.under = under;
            this.over = over;
            gap = 1 * s;
            w = Math.max(op.w, Math.max(under == null ? 0 : under.w, over == null ? 0 : over.w));
            asc = op.asc + (over == null ? 0 : gap + over.h());
            desc = op.desc + (under == null ? 0 : gap + under.h());
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            op.draw(g, f, x + (w - op.w) / 2, base, col);
            if (over != null) over.draw(g, f, x + (w - over.w) / 2, base - op.asc - gap - over.desc, col);
            if (under != null) under.draw(g, f, x + (w - under.w) / 2, base + op.desc + gap + under.asc, col);
        }
    }

    /** An accent drawn over a box: arrow, hat, bar or dot. */
    static final class Accent extends Box {
        final Box a;
        final String kind;
        final float s;

        Accent(Box a, String kind, float s) {
            this.a = a;
            this.kind = kind;
            this.s = s;
            w = Math.max(a.w, 4 * s);
            asc = a.asc + 2.5F * s;
            desc = a.desc;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            a.draw(g, f, x + (w - a.w) / 2, base, col);
            float y = base - a.asc - 1.2F * s, x0 = x + 0.5F * s, x1 = x + w - 0.5F * s, mid = (x0 + x1) / 2;
            switch (kind) {
                case "vec" -> {
                    rect(g, x0, y - 0.4F, x1, y + 0.4F, col);
                    line(g, x1 - 1.5F * s, y - 1F * s, x1, y, col);
                    line(g, x1 - 1.5F * s, y + 1F * s, x1, y, col);
                }
                case "hat" -> {
                    line(g, mid - 1.5F * s, y + 0.8F * s, mid, y - 0.8F * s, col);
                    line(g, mid, y - 0.8F * s, mid + 1.5F * s, y + 0.8F * s, col);
                }
                case "bar" -> rect(g, x0, y - 0.4F, x1, y + 0.4F, col);
                default -> rect(g, mid - 0.6F * s, y - 0.6F * s, mid + 0.6F * s, y + 0.6F * s, col);   // a dot
            }
        }
    }

    /** Radical sign with a bar over the argument (and an optional small index). */
    static final class Root extends Box {
        final Box a;
        @Nullable final Box idx;
        final float s, off;

        Root(Box a, @Nullable Box idx, float s) {
            this.a = a;
            this.idx = idx;
            this.s = s;
            off = idx == null ? 0 : Math.max(0, idx.w - 2 * s);
            w = a.w + 7 * s + off;
            asc = Math.max(a.asc + 2.5F * s, idx == null ? 0 : idx.h() + 3 * s);
            desc = a.desc + 0.5F * s;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float top = base - a.asc - 2F * s, bot = base + desc, mid = base - 2 * s;
            if (idx != null) idx.draw(g, f, x, mid - 1.5F * s - idx.desc, col);
            x += off;
            line(g, x + 0.5F * s, mid, x + 2 * s, mid - 0.8F * s, col);
            line(g, x + 2 * s, mid - 0.8F * s, x + 3.5F * s, bot, col);
            line(g, x + 3.5F * s, bot, x + 6 * s, top, col);
            rect(g, x + 6 * s, top - 0.5F, x + w - off, top + 0.5F, col);
            a.draw(g, f, x + 6.5F * s, base, col);
        }
    }

    /** Delimiters that grow with what they enclose: ( ), [ ], { }, | |, ‖ ‖, ⟨ |, | ⟩, ⟨ ⟩. */
    static final class Delim extends Box {
        final Box a;
        final String l, r;
        final float s, lw, rw;

        Delim(Font f, Box a, String l, String r, float s) {
            this.a = a;
            this.l = l;
            this.r = r;
            this.s = s;
            asc = Math.max(a.asc, 7 * s) + 0.5F * s;
            desc = Math.max(a.desc, 2 * s) + 0.5F * s;
            float k = thick(asc + desc, s);
            lw = l.isEmpty() ? 0 : Math.max(3.5F, f.width(l) * s * k + 0.5F * s);
            rw = r.isEmpty() ? 0 : Math.max(3.5F, f.width(r) * s * k + 0.5F * s);
            w = a.w + lw + rw;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float top = base - asc, h = asc + desc;
            if (!l.isEmpty()) glyph(g, f, x, top, h, l, col);
            a.draw(g, f, x + lw, base, col);
            if (!r.isEmpty()) glyph(g, f, x + lw + a.w + 0.5F * s, top, h, r, col);
        }

        static float thick(float h, float s) {
            return Math.min(1.6F, Math.max(1F, h / (9F * s) * 0.45F));
        }

        private void glyph(GuiGraphics g, Font f, float x, float top, float h, String c, int col) {
            g.pose().pushPose();
            g.pose().translate(x, top, 0);
            // tall brackets thicken a little so they do not turn into hairlines
            g.pose().scale(s * thick(h, s), h / 9F, 1);
            g.drawString(f, c, 0, 0, col, false);
            g.pose().popPose();
        }
    }

    static void rect(GuiGraphics g, float x0, float y0, float x1, float y1, int col) {
        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(Math.max(0.01F, x1 - x0), Math.max(0.01F, y1 - y0), 1);
        g.fill(0, 0, 1, 1, col);
        g.pose().popPose();
    }

    static void line(GuiGraphics g, float x0, float y0, float x1, float y1, int col) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        int n = Math.max(2, (int) (len * 2));
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n, x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t;
            rect(g, x - 0.5F, y - 0.5F, x + 0.5F, y + 0.5F, col);
        }
    }

    // ------------------------------------------------------------------ layout
    /** Supplies the box drawn for the slot at token position {@code tok} in a template. */
    public interface SlotFn {
        Box slot(int tok, float scale);
    }

    @Nullable private static SlotFn SLOTS;

    static Box slotOr(Font f, Node n, float s, java.util.function.Supplier<Box> normal) {
        if (SLOTS != null && n.tok() >= 0) return SLOTS.slot(n.tok(), s);
        return normal.get();
    }

    static Box layout(Font f, Node n, float s) {
        switch (n.kind()) {
            case NUM:
                return slotOr(f, n, s, () -> new Text(f, Component.literal(n.s()), s));
            case LEAF:
                return slotOr(f, n, s, () -> variable(f, n.s(), s));
            case PAR:
                return new Delim(f, layout(f, n.a(), s), "(", ")", s);
            case NEG:
                return new Row().add(new Text(f, Component.literal("−"), s), 0).add(layout(f, n.a(), s), 0.5F * s);
            case ADD:
            case SUB: {
                Box op = slotOr(f, n, s, () -> new Text(f, Component.literal(n.kind() == Kind.SUB ? "−" : "+"), s));
                return new Row().add(layout(f, n.a(), s), 0).add(op, 3 * s).add(layout(f, n.b(), s), 3 * s);
            }
            case MUL: {
                Box l = layout(f, n.a(), s), r = layout(f, n.b(), s);
                if (SLOTS == null && startsNumeric(n.b()))
                    return new Row().add(l, 0).add(new Text(f, Component.literal("·"), s), 1.5F * s).add(r, 1.5F * s);
                return new Row().add(l, 0).add(r, (SLOTS != null ? 1.6F : 1.2F) * s);
            }
            case DOT:
                return new Row().add(layout(f, n.a(), s), 0).add(new Text(f, Component.literal("·"), s), 1.5F * s).add(layout(f, n.b(), s), 1.5F * s);
            case CROSS:
                return new Row().add(layout(f, n.a(), s), 0).add(new Text(f, Component.literal("×"), s), 1.5F * s).add(layout(f, n.b(), s), 1.5F * s);
            case DIV: {
                float inner = Math.min(s, Math.max(0.6F, s * (n.a().kind() == Kind.DIV || n.b().kind() == Kind.DIV ? 0.85F : 1F)));
                return new Frac(layout(f, strip(n.a()), inner), layout(f, strip(n.b()), inner), s, true);
            }
            case POW: {
                // x_i^2: one base with both scripts
                if (n.a().kind() == Kind.SUBSCRIPT)
                    return new Scripts(layout(f, n.a().a(), s), layout(f, strip(n.b()), s * 0.7F), index(f, strip(n.a().b()), s * 0.7F), s);
                return new Scripts(layout(f, n.a(), s), layout(f, strip(n.b()), s * 0.7F), null, s);
            }
            case SUBSCRIPT:
                return new Scripts(layout(f, n.a(), s), null, index(f, strip(n.b()), s * 0.7F), s);
            case FACT:
                return new Row().add(layout(f, n.a(), s), 0).add(new Text(f, Component.literal("!"), s), 0.5F * s);
            case FN: {
                Box name = slotOr(f, n, s, () -> new Text(f, Component.literal(n.s()), s));
                return new Row().add(name, 0).add(new Delim(f, layout(f, strip(n.a()), s), "(", ")", s), 0.5F * s);
            }
            case HEAD:
                return head(f, n, s);
            default:
                throw new IllegalArgumentException();
        }
    }

    /** A subscript: a multi-letter index (eq, ij, BH) is written whole, not split into a further subscript. */
    private static Box index(Font f, Node n, float s) {
        if (n.kind() == Kind.LEAF) return slotOr(f, n, s, () -> new Text(f, Component.literal(n.s()).withStyle(ChatFormatting.ITALIC), s));
        return layout(f, n, s);
    }

    private static Box small(Font f, Node n, float s) {
        return layout(f, strip(n), s * 0.65F);
    }

    private static Box head(Font f, Node n, float s) {
        List<Node> a = n.kids();
        switch (n.s()) {
            case "int", "oint", "iint", "iiint" -> {
                String sign = switch (n.s()) {
                    case "oint" -> "∮";
                    case "iint" -> "∬";
                    case "iiint" -> "∭";
                    default -> "∫";
                };
                boolean bounds = n.s().equals("int") && a.size() == 4;
                Node body = bounds ? a.get(2) : a.get(0), var = bounds ? a.get(3) : a.get(1);
                Box b = layout(f, body, s);
                Box big = new BigOp(f, sign, s, b, 1.5F, 2.2F);
                Box op = bounds ? new Scripts(big, small(f, a.get(1), s), small(f, a.get(0), s), s)
                        : n.s().equals("int") || n.s().equals("oint") ? big : new Scripts(big, null, small(f, var, s), s);
                Row r = new Row().add(op, 0).add(b, 1 * s);
                if (n.s().equals("int") || n.s().equals("oint"))
                    r.add(new Text(f, Component.literal("d"), s), 1.5F * s).add(layout(f, var, s), 0.3F * s);
                return r;
            }
            case "sum", "prod", "sumk" -> {
                String sign = n.s().equals("prod") ? "∏" : "∑";
                Node body = n.s().equals("sumk") ? a.get(1) : a.get(3);
                Box b = layout(f, body, s);
                Box big = new BigOp(f, sign, s, b, 1.4F, 2.0F);
                Box under, over = null;
                if (n.s().equals("sumk")) under = small(f, a.get(0), s);
                else {
                    under = new Row().add(small(f, a.get(0), s), 0).add(new Text(f, Component.literal("="), s * 0.65F), 0.5F * s).add(small(f, a.get(1), s), 0.5F * s);
                    over = small(f, a.get(2), s);
                }
                return new Row().add(new Limits(big, under, over, s), 0).add(b, 1.5F * s);
            }
            case "lim" -> {
                Box under = new Row().add(small(f, a.get(0), s), 0).add(new Text(f, Component.literal("→"), s * 0.65F), 0.5F * s).add(small(f, a.get(1), s), 0.5F * s);
                return new Row().add(new Limits(new Text(f, Component.literal("lim"), s), under, null, s), 0).add(layout(f, a.get(2), s), 2 * s);
            }
            case "d", "d2", "pd", "pd2", "pdm" -> {
                boolean partial = n.s().startsWith("p");
                String D = partial ? "∂" : "d";
                Box num, den;
                if (n.s().equals("pdm")) {
                    num = new Scripts(new Text(f, Component.literal(D), s), new Text(f, Component.literal("2"), s * 0.7F), null, s);
                    den = new Row().add(new Text(f, Component.literal(D), s), 0).add(layout(f, a.get(1), s), 0.3F * s)
                            .add(new Text(f, Component.literal(D), s), 0.8F * s).add(layout(f, a.get(2), s), 0.3F * s);
                } else if (n.s().endsWith("2")) {
                    num = new Scripts(new Text(f, Component.literal(D), s), new Text(f, Component.literal("2"), s * 0.7F), null, s);
                    den = new Row().add(new Text(f, Component.literal(D), s), 0).add(new Scripts(layout(f, a.get(1), s), new Text(f, Component.literal("2"), s * 0.7F), null, s), 0.3F * s);
                } else {
                    num = new Text(f, Component.literal(D), s);
                    den = new Row().add(new Text(f, Component.literal(D), s), 0).add(layout(f, a.get(1), s), 0.3F * s);
                }
                Node body = a.get(0);
                boolean simple = body.kind() == Kind.LEAF || body.kind() == Kind.NUM || body.kind() == Kind.HEAD;
                if (simple) {
                    // df/dx with f on top
                    Row top = new Row().add(num, 0).add(layout(f, body, s), 0.3F * s);
                    return new Frac(top, den, s, true);
                }
                return new Row().add(new Frac(num, den, s, true), 0).add(new Delim(f, layout(f, strip(body), s), "(", ")", s), 0.5F * s);
            }
            case "grad", "div", "curl", "lap", "dal" -> {
                String op = switch (n.s()) {
                    case "grad" -> "∇";
                    case "div" -> "∇·";
                    case "curl" -> "∇×";
                    case "lap" -> "∇²";
                    default -> "□";
                };
                Node body = a.get(0);
                Box b = layout(f, body, s);
                if (!(body.kind() == Kind.LEAF || body.kind() == Kind.HEAD || body.kind() == Kind.SUBSCRIPT)) b = new Delim(f, layout(f, strip(body), s), "(", ")", s);
                return new Row().add(new Text(f, Component.literal(op), s), 0).add(b, 0.5F * s);
            }
            case "vec", "hat", "bar", "tdot" -> {
                return new Accent(layout(f, a.get(0), s), n.s(), s);
            }
            case "conj", "dag" -> {
                Box b = layout(f, a.get(0), s);
                if (!(a.get(0).kind() == Kind.LEAF || a.get(0).kind() == Kind.HEAD || a.get(0).kind() == Kind.SUBSCRIPT))
                    b = new Delim(f, layout(f, strip(a.get(0)), s), "(", ")", s);
                return new Scripts(b, new Text(f, Component.literal(n.s().equals("conj") ? "*" : "†"), s * 0.7F), null, s);
            }
            case "abs" -> {
                return new Delim(f, layout(f, strip(a.get(0)), s), "|", "|", s);
            }
            case "norm" -> {
                return new Delim(f, layout(f, strip(a.get(0)), s), "‖", "‖", s);
            }
            case "bra" -> {
                return new Delim(f, layout(f, a.get(0), s), "⟨", "|", s);
            }
            case "ket" -> {
                return new Delim(f, layout(f, a.get(0), s), "|", "⟩", s);
            }
            case "braket" -> {
                Row r = new Row().add(layout(f, a.get(0), s), 0).add(new Text(f, Component.literal("|"), s), 1 * s).add(layout(f, a.get(1), s), 1 * s);
                return new Delim(f, r, "⟨", "⟩", s);
            }
            case "mel" -> {
                Row r = new Row().add(layout(f, a.get(0), s), 0).add(new Text(f, Component.literal("|"), s), 1 * s).add(layout(f, a.get(1), s), 1 * s)
                        .add(new Text(f, Component.literal("|"), s), 1 * s).add(layout(f, a.get(2), s), 1 * s);
                return new Delim(f, r, "⟨", "⟩", s);
            }
            case "avg" -> {
                return new Delim(f, layout(f, a.get(0), s), "⟨", "⟩", s);
            }
            case "comm", "acomm" -> {
                Row r = new Row().add(layout(f, a.get(0), s), 0).add(new Text(f, Component.literal(","), s), 0.5F * s).add(layout(f, a.get(1), s), 2 * s);
                return n.s().equals("comm") ? new Delim(f, r, "[", "]", s) : new Delim(f, r, "{", "}", s);
            }
            case "sqrt" -> {
                return new Root(layout(f, strip(a.get(0)), s), null, s);
            }
            case "root" -> {
                return new Root(layout(f, strip(a.get(1)), s), small(f, a.get(0), s), s);
            }
            case "binom" -> {
                return new Delim(f, new Frac(layout(f, strip(a.get(0)), s * 0.85F), layout(f, strip(a.get(1)), s * 0.85F), s, false), "(", ")", s);
            }
            case "hc" -> {
                return new Text(f, Component.literal("h.c."), s);
            }
            default -> {
                return new Text(f, Component.literal(n.s()), s);
            }
        }
    }

    /** A paper would not keep brackets around a whole numerator, denominator, exponent or argument. */
    private static Node strip(Node n) {
        return n.kind() == Kind.PAR ? strip(n.a()) : n;
    }

    private static boolean startsNumeric(Node n) {
        if (n.kind() == Kind.NUM || n.kind() == Kind.NEG) return true;
        if (n.kind() == Kind.MUL || n.kind() == Kind.DIV || n.kind() == Kind.POW || n.kind() == Kind.SUBSCRIPT) return startsNumeric(n.a());
        return false;
    }

    /** Italic variable, with everything after its first symbol as a subscript (kB -> k_B), Greek index pairs whole. */
    public static Box variable(Font f, String v, float s) {
        if (v.equals("π") || v.equals("∞") || v.equals("ħ") || v.length() == 1)
            return new Text(f, Component.literal(v).withStyle(v.equals("∞") ? ChatFormatting.RESET : ChatFormatting.ITALIC), s);
        if (v.codePoints().allMatch(c -> c >= 0x3B1 && c <= 0x3C9)) return new Text(f, Component.literal(v).withStyle(ChatFormatting.ITALIC), s);
        int cut = v.charAt(0) == 'Δ' || v.charAt(0) == '∂' ? 2 : 1;
        if (Character.isHighSurrogate(v.charAt(0))) cut = 2;
        Box main = new Text(f, Component.literal(v.substring(0, Math.min(cut, v.length()))).withStyle(ChatFormatting.ITALIC), s);
        if (v.length() <= cut) return main;
        String rest = v.substring(cut);
        Component sub = rest.matches("\\d+") ? Component.literal(rest) : Component.literal(rest).withStyle(ChatFormatting.ITALIC);
        return new Scripts(main, null, new Text(f, sub, s * 0.65F), s);
    }

    /** How a single tile looks. */
    public static Box tile(Font f, String tok, float s) {
        return switch (tok) {
            case "*" -> new Text(f, Component.literal("×"), s);
            case "-" -> new Text(f, Component.literal("−"), s);
            case "+" -> new Text(f, Component.literal("+"), s);
            default -> {
                String k = FormExpr.kind(tok);
                if (k.equals("num")) yield new Text(f, Component.literal(tok), s);
                if (k.equals("func")) yield new Text(f, Component.literal(tok), s);
                if (k.equals("head")) yield new Text(f, Component.literal(headSymbol(tok)), s);
                if (k.equals("punct")) yield new Text(f, Component.literal(tok), s);
                yield variable(f, tok, s);
            }
        };
    }

    /** What a structural glyph looks like in the glossary. */
    public static String headSymbol(String h) {
        return switch (h) {
            case "int" -> "∫";
            case "oint" -> "∮";
            case "iint" -> "∬";
            case "iiint" -> "∭";
            case "sum", "sumk" -> "∑";
            case "prod" -> "∏";
            case "lim" -> "lim";
            case "d" -> "d/dx";
            case "d2" -> "d²/dx²";
            case "pd" -> "∂/∂x";
            case "pd2" -> "∂²/∂x²";
            case "pdm" -> "∂²/∂x∂y";
            case "grad" -> "∇";
            case "div" -> "∇·";
            case "curl" -> "∇×";
            case "lap" -> "∇²";
            case "dal" -> "□";
            case "vec" -> "x⃗";
            case "hat" -> "x̂";
            case "bar" -> "x̄";
            case "tdot" -> "ẋ";
            case "conj" -> "z*";
            case "dag" -> "A†";
            case "abs" -> "|x|";
            case "norm" -> "‖x‖";
            case "bra" -> "⟨ψ|";
            case "ket" -> "|ψ⟩";
            case "braket" -> "⟨φ|ψ⟩";
            case "mel" -> "⟨φ|A|ψ⟩";
            case "avg" -> "⟨A⟩";
            case "comm" -> "[A,B]";
            case "acomm" -> "{A,B}";
            case "sqrt" -> "√";
            case "root" -> "ⁿ√";
            case "binom" -> "(n k)";
            case "hc" -> "h.c.";
            default -> h;
        };
    }

    // ------------------------------------------------------------------ whole equations
    /** "lhs = rhs" fully typeset, breaking a long top-level sum over lines no wider than maxW. Null if it does not parse. */
    @Nullable
    public static Box equation(Font f, List<String> lhs, List<String> rhs, float s, float maxW) {
        return build(f, lhs, rhs, s, maxW, null);
    }

    /** The same, with every slot drawn by {@code slots}: the template the player fills. */
    @Nullable
    public static Box template(Font f, List<String> lhs, List<String> rhs, float s, float maxW, SlotFn slots) {
        return build(f, lhs, rhs, s, maxW, slots);
    }

    @Nullable
    private static Box build(Font f, List<String> lhs, List<String> rhs, float s, float maxW, @Nullable SlotFn slots) {
        Node l = FormExpr.tryParse(lhs), r = FormExpr.tryParse(rhs);
        if (r == null) return null;
        Box lb = l == null ? new Text(f, Component.literal(String.join(" ", lhs)), s) : layout(f, l, s);
        Text eq = new Text(f, Component.literal("="), s);
        SLOTS = slots;
        try {
            Box whole = layout(f, r, s);
            Row first = new Row().add(lb, 0).add(eq, 3 * s);
            if (lb.w + eq.w + 6 * s + whole.w <= maxW || !(r.kind() == Kind.ADD || r.kind() == Kind.SUB)) return first.add(whole, 3 * s);
            // break a long sum between its terms; each continuation line starts with its sign
            List<Node> terms = new ArrayList<>();
            List<Node> ops = new ArrayList<>();
            split(r, terms, ops);
            List<Box> lines = new ArrayList<>();
            Row cur = first;
            float indent = lb.w + eq.w + 6 * s, limit = maxW;
            boolean empty = true;
            for (int i = 0; i < terms.size(); i++) {
                Box t = layout(f, terms.get(i), s);
                Box op = null;
                if (i > 0) {
                    Node o = ops.get(i - 1);
                    op = slotOr(f, o, s, () -> new Text(f, Component.literal(o.kind() == Kind.SUB ? "−" : "+"), s));
                }
                float need = (op == null ? 0 : op.w + 3 * s) + t.w + 3 * s;
                float used = cur.w + (lines.isEmpty() ? 0 : indent);
                if (!empty && used + need > limit) {
                    lines.add(cur);
                    cur = new Row();
                    empty = true;
                }
                if (op != null) cur.add(op, empty ? 0 : 3 * s);
                cur.add(t, empty && op == null ? (lines.isEmpty() ? 3 * s : 0) : 3 * s);
                empty = false;
            }
            lines.add(cur);
            return lines.size() == 1 ? lines.get(0) : new Lines(lines, indent, 3 * s);
        } finally {
            SLOTS = null;
        }
    }

    /** The top-level terms of a sum, left to right, and the + / − nodes between them. */
    private static void split(Node n, List<Node> terms, List<Node> ops) {
        if (n.kind() == Kind.ADD || n.kind() == Kind.SUB) {
            split(n.a(), terms, ops);
            ops.add(n);
            terms.add(n.b());
        } else terms.add(n);
    }

    /** A drop target in a template: an empty dashed box, or the tile that fills it on a tinted ground. */
    public static final class Slot extends Box {
        public final int index;
        final Box inner;
        final int bg, edge;
        /** Where each slot was last drawn, in screen coordinates: index -> {x0, y0, x1, y1}. */
        public static final java.util.Map<Integer, float[]> RECTS = new java.util.HashMap<>();

        public Slot(Font f, int index, @Nullable String tok, float s, int bg, int edge) {
            this.index = index;
            this.inner = tok == null ? null : tile(f, tok, s);
            this.bg = bg;
            this.edge = edge;
            w = Math.max(inner == null ? 0 : inner.w, 9 * s) + 3 * s;
            asc = Math.max(inner == null ? 0 : inner.asc, 7 * s) + 1.5F * s;
            desc = Math.max(inner == null ? 0 : inner.desc, 2 * s) + 1.5F * s;
        }

        @Override
        void draw(GuiGraphics g, Font f, float x, float base, int col) {
            float y0 = base - asc, y1 = base + desc;
            rect(g, x, y0, x + w, y1, bg);
            rect(g, x, y0, x + w, y0 + 0.75F, edge);
            rect(g, x, y1 - 0.75F, x + w, y1, edge);
            rect(g, x, y0, x + 0.75F, y1, edge);
            rect(g, x + w - 0.75F, y0, x + w, y1, edge);
            if (inner != null) inner.draw(g, f, x + (w - inner.w) / 2, base, col);
            var m = g.pose().last().pose();
            org.joml.Vector3f a = m.transformPosition(new org.joml.Vector3f(x, y0, 0)), b = m.transformPosition(new org.joml.Vector3f(x + w, y1, 0));
            RECTS.put(index, new float[]{Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y)});
        }
    }

    /** Draws a box with its left edge at x and its baseline at base, scaled down to fit maxW if needed. */
    public static void draw(GuiGraphics g, Font f, Box b, float x, float base, float maxW, int col) {
        float k = b.w > maxW ? maxW / b.w : 1F;
        g.pose().pushPose();
        g.pose().translate(x, base, 0);
        g.pose().scale(k, k, 1);
        b.draw(g, f, 0, 0, col);
        g.pose().popPose();
    }

    private MathTypeset() {}
}
