package com.theoreticalsciences.client.screen.forge;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Round 29: the "code display" of the Omnipotence Core - a listing of the program the Core is pretending to run,
 * typed out live. Purely for show: nothing here is sent anywhere or changes the game.
 *
 * <p>The screen hands it lines whenever the player does something ({@link #emit}); they wait in a queue and are typed
 * out one character at a time, faster when many are waiting so the listing never lags far behind. When nothing is
 * happening the screen supplies "idle" lines (the simulation loop ticking), so the program always looks alive.
 *
 * <p>A line can carry a tag. A new line with the same tag replaces one still waiting in the queue: dragging a slider
 * produces one line that keeps updating rather than hundreds of lines.
 *
 * <p>Lines are coloured like a code editor: keywords, function calls, numbers, "strings", // comments, and the words
 * OK / FAIL / ACCEPTED / REJECTED stand out.
 */
public final class CoreCode {
    private static final int MAX_DONE = 160, MAX_QUEUE = 40;
    private static final Set<String> KEYWORDS = Set.of("let", "for", "in", "while", "if", "else", "return", "assert", "await",
            "break", "fn", "yield", "emit", "exec", "import", "true", "false", "null", "loop", "const", "try", "catch");
    private static final Set<String> GOOD = Set.of("OK", "ACCEPTED", "STABLE", "LOCKED", "ok", "online");
    private static final Set<String> BAD = Set.of("FAIL", "REJECTED", "DENIED", "DIVERGENT", "ERROR", "WARNING", "ABORT");

    // colours of the listing
    public int cPlain = 0xFF9CE8B0, cKeyword = 0xFFC79CFF, cCall = 0xFF5CFF8A, cNumber = 0xFF8AD8FF, cString = 0xFFFFD27A,
            cComment = 0xFF3F7A52, cPunct = 0xFF4FA86A, cGood = 0xFF7CFFA0, cBad = 0xFFFF6A5A, cGutter = 0xFF1F5A34, cMark = 0xFFFFC24A;

    private record Tok(String s, int colour) {}

    private static final class Line {
        String text, tag;
        List<Tok> toks;
        int number;

        Line(String tag, String text) {
            this.tag = tag;
            set(text);
        }

        void set(String t) {
            text = t;
            toks = null;
        }
    }

    private final ArrayDeque<Line> queue = new ArrayDeque<>();
    private final ArrayList<Line> done = new ArrayList<>();
    private Line typing;
    private double typed;              // how many characters of the current line are on screen
    private double idleWait;           // seconds until the next idle line
    // round 32b: after the player does something the program runs on for a few lines, then waits quietly for the next
    // touch - instead of scrolling forever while nobody is using the screen
    private static final int IDLE_AFTER_TOUCH = 4;
    private int idleBudget = IDLE_AFTER_TOUCH;
    private int idleStep, lineNo = 1;
    private long last = System.nanoTime();

    /** Queues a line. */
    public void emit(String text) {
        emit(null, text);
    }

    /** Queues a line; a waiting line with the same tag is replaced instead (so a drag gives one updating line). */
    public void emit(String tag, String text) {
        if (tag != null) {
            if (typing != null && tag.equals(typing.tag) && typed < 3) {   // the line was only just started: retype it
                typing.set(text);
                return;
            }
            for (Line l : queue)
                if (tag.equals(l.tag)) {
                    l.set(text);
                    return;
                }
        }
        queue.add(new Line(tag, text));
        // far too much waiting (someone is clicking wildly): the oldest lines are finished at once
        while (queue.size() > MAX_QUEUE) finish(queue.poll());
        idleWait = Math.max(idleWait, 0.5);
        idleBudget = IDLE_AFTER_TOUCH;
    }

    private void finish(Line l) {
        l.number = lineNo++;
        done.add(l);
        while (done.size() > MAX_DONE) done.remove(0);
    }

    /**
     * Advances the typing; call once per frame. {@code idle} gives the n-th idle line (or null for none) and
     * {@code idleEvery} how many seconds apart they come.
     */
    public void frame(IntFunction<String> idle, double idleEvery) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1e9);
        last = now;
        if (typing == null && !queue.isEmpty()) {
            typing = queue.poll();
            typed = 0;
        }
        if (typing != null) {
            double speed = 70 + 45 * queue.size();          // characters per second
            typed += dt * speed;
            if (typed >= typing.text.length() + 2) {         // a short pause at the end of each line
                finish(typing);
                typing = null;
            }
            return;
        }
        idleWait -= dt;
        if (idleWait <= 0 && idle != null && idleBudget > 0) {
            String s = idle.apply(idleStep++);
            if (s != null) {
                queue.add(new Line(null, s));
                idleBudget--;
            }
            idleWait = idleEvery;
        }
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the listing into (x, y, w, h): finished lines above, the line being typed last, highlighted. */
    public void render(GuiGraphics g, Font font, int x, int y, int w, int h) {
        int lh = 9, rows = Math.max(1, h / lh);
        int gutter = font.width("0000") + 6;
        int shownDone = Math.min(done.size(), rows - 1);
        int ly = y + h - rows * lh;
        // so the newest line is always on the bottom row
        ly += (rows - 1 - shownDone) * lh;
        for (int i = done.size() - shownDone; i < done.size(); i++) {
            Line l = done.get(i);
            drawLine(g, font, l, l.number, Integer.MAX_VALUE, x, ly, w, gutter, false);
            ly += lh;
        }
        boolean blink = (System.currentTimeMillis() / 350) % 2 == 0;
        g.fill(x, ly - 1, x + w, ly + lh - 1, 0x2A5CFF8A);               // the line being executed
        if (typing != null) {
            int end = drawLine(g, font, typing, lineNo, (int) typed, x, ly, w, gutter, true);
            if (blink) g.fill(end + 1, ly, end + 6, ly + 8, 0xC05CFF8A);
        } else {
            g.drawString(font, ">", x + gutter - 6, ly, cMark, false);
            if (blink) g.fill(x + gutter + 1, ly, x + gutter + 6, ly + 8, 0xC05CFF8A);
        }
    }

    /** Draws one line (only its first {@code chars} characters); returns where the text ends. */
    private int drawLine(GuiGraphics g, Font font, Line l, int number, int chars, int x, int y, int w, int gutter, boolean current) {
        String num = current ? ">" : String.format("%04d", number % 10000);
        g.drawString(font, num, x + gutter - 4 - font.width(num), y, current ? cMark : cGutter, false);
        if (l.toks == null) l.toks = tokenize(l.text);
        int cx = x + gutter, right = x + w - 2, left = chars;
        int ell = font.width("…");
        for (Tok t : l.toks) {
            if (left <= 0) break;
            String s = t.s.length() > left ? t.s.substring(0, left) : t.s;
            left -= s.length();
            int tw = font.width(s);
            if (cx + tw > right) {                           // no more room: cut the line with "…"
                String c = font.plainSubstrByWidth(s, Math.max(0, right - ell - cx));
                g.drawString(font, c + "…", cx, y, t.colour, false);
                return cx + font.width(c + "…");
            }
            if (!s.isBlank()) g.drawString(font, s, cx, y, t.colour, false);
            cx += tw;
        }
        return cx;
    }

    /** Splits a line into coloured pieces, like a code editor's syntax colouring. */
    private List<Tok> tokenize(String s) {
        List<Tok> out = new ArrayList<>();
        int i = 0, n = s.length();
        boolean afterWord = false;
        while (i < n) {
            char c = s.charAt(i);
            int j = i + 1;
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                out.add(new Tok(s.substring(i), cComment));
                break;
            }
            if (c == '"') {
                while (j < n && s.charAt(j) != '"') j++;
                j = Math.min(n, j + 1);
                out.add(new Tok(s.substring(i, j), cString));
                afterWord = true;
            } else if (Character.isDigit(c) || (c == '+' || c == '-') && !afterWord && i + 1 < n && Character.isDigit(s.charAt(i + 1))) {
                boolean hex = c == '0' && i + 1 < n && s.charAt(i + 1) == 'x';
                if (hex) j = i + 2;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.' || s.charAt(j) == ','
                        && j + 1 < n && Character.isDigit(s.charAt(j + 1)) || hex && "abcdefABCDEF".indexOf(s.charAt(j)) >= 0)) j++;
                while (j < n && "kMs%".indexOf(s.charAt(j)) >= 0) j++;      // units: 1.2k, 60s, 40%
                out.add(new Tok(s.substring(i, j), cNumber));
                afterWord = true;
            } else if (Character.isLetter(c) || c == '_') {
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_' || s.charAt(j) == '²')) j++;
                String w = s.substring(i, j);
                int col = KEYWORDS.contains(w) ? cKeyword : GOOD.contains(w) ? cGood : BAD.contains(w) ? cBad
                        : j < n && s.charAt(j) == '(' ? cCall : cPlain;
                out.add(new Tok(w, col));
                afterWord = true;
            } else if (c == ' ') {
                while (j < n && s.charAt(j) == ' ') j++;
                out.add(new Tok(s.substring(i, j), cPlain));
                afterWord = false;          // after a space, +12 is a number, not a plus sign
            } else {
                out.add(new Tok(String.valueOf(c), cPunct));
                afterWord = c == ')' || c == ']';
            }
            i = j;
        }
        return out;
    }

    /** How many lines have run so far (shown in the panel title). */
    public int lines() {
        return lineNo - 1;
    }

    /** True while lines are waiting or being typed. */
    public boolean busy() {
        return typing != null || !queue.isEmpty();
    }

    /** Drops waiting lines with a tag (e.g. a hover that no longer applies). */
    public void cancel(String tag) {
        for (Iterator<Line> it = queue.iterator(); it.hasNext(); ) if (tag.equals(it.next().tag)) it.remove();
    }
}
