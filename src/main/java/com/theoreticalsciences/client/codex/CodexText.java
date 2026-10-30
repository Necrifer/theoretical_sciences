package com.theoreticalsciences.client.codex;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Round 30: the Codex's text. The entries use the formatting codes the Patchouli book always used:
 * <ul>
 *   <li>{@code $(item)...$()} an item or block (cyan, and a link if there is an entry about it),
 *       {@code $(thing)...$()} a concept (amber), {@code $(fluid)...$()} a fluid (green), {@code $(#rrggbb)} any colour</li>
 *   <li>{@code $(l)} or {@code $(bold)} bold, {@code $(o)} italic, {@code $()} back to plain text</li>
 *   <li>{@code $(br)} a new line, {@code $(br2)} a new paragraph, {@code $(li)} a bullet point</li>
 *   <li>{@code $(l:target)...$(/l)} a link to another entry</li>
 * </ul>
 * {@link #layout} breaks the text into lines that fit the given width (a word too long for a line is split), so
 * nothing ever runs past the edge; every word remembers the entry it links to, for clicks.
 */
public final class CodexText {
    public static final int TEXT = 0xFFD8E4F0, ITEM = 0xFF7FE6FF, THING = 0xFFFFC870, FLUID = 0xFF80FFB8, LINK_HOVER = 0xFFFFFFFF;
    public static final int LINE = 10;

    /** One word (or space) placed on a line. */
    public record Seg(int x, int line, int width, Component text, String link) {}

    public record Layout(List<Seg> segs, int lines, int height) {
        /** The link under a point relative to the text's top left corner, or null. */
        public String linkAt(double mx, double my) {
            int line = (int) Math.floor(my / LINE);
            for (Seg s : segs)
                if (s.link != null && s.line == line && mx >= s.x && mx < s.x + s.width) return s.link;
            return null;
        }

        public void render(GuiGraphics g, Font f, int x, int y, double mx, double my) {
            String hover = linkAt(mx - x, my - y);
            for (Seg s : segs) {
                Component c = s.text;
                if (s.link != null && s.link.equals(hover)) c = c.copy().withStyle(st -> st.withUnderlined(true).withColor(LINK_HOVER & 0xFFFFFF));
                g.drawString(f, c, x + s.x, y + s.line * LINE, TEXT, false);
            }
        }
    }

    /** The text without its formatting codes (for searching). */
    public static String plain(String s) {
        if (s == null) return "";
        return s.replace("$(br2)", " ").replace("$(br)", " ").replace("$(li)", " ").replaceAll("\\$\\([^)]*\\)", "");
    }

    private record Run(String text, Style style, String link, int kind) {}

    private static final int WORD = 0, SPACE = 1, BREAK = 2, BULLET = 3;

    /** Splits the text into words, spaces and breaks, each with its style. */
    private static List<Run> parse(String s, Style base) {
        List<Run> out = new ArrayList<>();
        Style st = base;
        String link = null;
        // a highlighted run ($(item)Name$()) links to the entry it names; its words are collected so the whole
        // name is looked up once at the end of the run
        int runStart = -1;
        boolean autoLink = false;
        StringBuilder word = new StringBuilder();
        int i = 0, n = s.length();
        while (i <= n) {
            boolean code = i < n - 1 && s.charAt(i) == '$' && s.charAt(i + 1) == '(';
            char c = i < n ? s.charAt(i) : '\n';
            boolean end = i == n;
            if (end || code || c == ' ' || c == '\n') {
                if (!word.isEmpty()) {
                    out.add(new Run(word.toString(), st, link, WORD));
                    word.setLength(0);
                }
            }
            if (end) break;
            if (code) {
                int close = s.indexOf(')', i);
                if (close < 0) { word.append(c); i++; continue; }
                String k = s.substring(i + 2, close);
                i = close + 1;
                switch (k) {
                    case "br" -> out.add(new Run("", st, null, BREAK));
                    case "br2" -> { out.add(new Run("", st, null, BREAK)); out.add(new Run("", st, null, BREAK)); }
                    case "li", "li2", "li3" -> out.add(new Run("", st, null, BULLET));
                    case "", "/l", "/t", "r" -> {
                        if (autoLink) finishAutoLink(out, runStart);
                        autoLink = false;
                        st = base;
                        link = null;
                    }
                    case "item" -> { if (autoLink) finishAutoLink(out, runStart); st = base.withColor(TextColor.fromRgb(ITEM & 0xFFFFFF)); autoLink = true; runStart = out.size(); }
                    case "thing" -> { if (autoLink) finishAutoLink(out, runStart); st = base.withColor(TextColor.fromRgb(THING & 0xFFFFFF)); autoLink = true; runStart = out.size(); }
                    case "fluid" -> { if (autoLink) finishAutoLink(out, runStart); st = base.withColor(TextColor.fromRgb(FLUID & 0xFFFFFF)); autoLink = true; runStart = out.size(); }
                    case "l", "bold" -> st = st.withBold(true);
                    case "o", "italic" -> st = st.withItalic(true);
                    case "n", "u" -> st = st.withUnderlined(true);
                    default -> {
                        if (k.startsWith("#")) {
                            String hex = k.substring(1);
                            if (hex.length() == 3) hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                            try {
                                st = st.withColor(TextColor.fromRgb(Integer.parseInt(hex, 16)));
                            } catch (NumberFormatException ignored) {}
                        } else if (k.startsWith("l:")) {
                            link = k.substring(2);
                            st = st.withColor(TextColor.fromRgb(ITEM & 0xFFFFFF));
                        }
                        // anything else (obfuscation, Patchouli's own macros) is left out
                    }
                }
                continue;
            }
            if (c == ' ') out.add(new Run(" ", st, link, SPACE));
            else if (c == '\n') out.add(new Run("", st, null, BREAK));
            else word.append(c);
            i++;
        }
        if (autoLink) finishAutoLink(out, runStart);
        return out;
    }

    /** The words since {@code from} were one highlighted name: if an entry has that name, they all link to it. */
    private static void finishAutoLink(List<Run> out, int from) {
        if (from < 0 || from >= out.size()) return;
        StringBuilder sb = new StringBuilder();
        for (int k = from; k < out.size(); k++) if (out.get(k).kind == WORD || out.get(k).kind == SPACE) sb.append(out.get(k).text);
        String target = CodexData.entryNamed(sb.toString());
        if (target == null) return;
        for (int k = from; k < out.size(); k++) {
            Run r = out.get(k);
            if (r.link == null && (r.kind == WORD || r.kind == SPACE)) out.set(k, new Run(r.text, r.style, target, r.kind));
        }
    }

    /** Lays the text out in lines of at most {@code width} pixels. */
    public static Layout layout(Font f, String text, int width) {
        return layout(f, text, width, Style.EMPTY);
    }

    public static Layout layout(Font f, String text, int width, Style base) {
        width = Math.max(20, width);
        List<Seg> segs = new ArrayList<>();
        int line = 0, x = 0, indent = 0;
        boolean lineEmpty = true;
        int bulletW = f.width("• ");
        for (Run r : parse(text == null ? "" : text, base)) {
            switch (r.kind) {
                case BREAK -> {
                    line++;
                    x = 0;
                    indent = 0;
                    lineEmpty = true;
                }
                case BULLET -> {
                    if (!lineEmpty) line++;
                    segs.add(new Seg(0, line, bulletW, Component.literal("•").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ITEM & 0xFFFFFF))), null));
                    x = indent = bulletW;
                    lineEmpty = false;
                }
                case SPACE -> {
                    if (x == indent) continue;            // no spaces at the start of a line
                    int w = f.width(" ");
                    if (x + w <= width) segs.add(new Seg(x, line, w, Component.literal(" ").withStyle(r.style), r.link));
                    x += w;
                }
                default -> {
                    Component c = Component.literal(r.text).withStyle(r.style);
                    int w = f.width(c);
                    if (x + w > width && x > indent) {      // wrap before the word
                        line++;
                        x = indent;
                    }
                    if (w > width - indent) {               // a word longer than the whole line: split it
                        String rest = r.text;
                        while (!rest.isEmpty()) {
                            int room = width - x;
                            String part = f.plainSubstrByWidth(rest, Math.max(f.width("W"), room));
                            if (part.isEmpty()) part = rest.substring(0, 1);
                            Component pc = Component.literal(part).withStyle(r.style);
                            int pw = f.width(pc);
                            if (x + pw > width && x > indent) {
                                line++;
                                x = indent;
                                continue;
                            }
                            segs.add(new Seg(x, line, pw, pc, r.link));
                            x += pw;
                            rest = rest.substring(part.length());
                            if (!rest.isEmpty()) {
                                line++;
                                x = indent;
                            }
                        }
                    } else {
                        segs.add(new Seg(x, line, w, c, r.link));
                        x += w;
                    }
                    lineEmpty = false;
                }
            }
        }
        // trailing empty lines (from a final $(br2)) do not count
        int lines = 0;
        for (Seg s : segs) lines = Math.max(lines, s.line + 1);
        return new Layout(segs, lines, lines * LINE);
    }

    private CodexText() {}
}
