package com.theoreticalsciences.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Round 27: text that keeps inside its panel. Long lines are wrapped where there is room for more lines, or cut
 * with "…" where there is not (and then the whole text shows when the mouse is over it); tooltips are wrapped to
 * a sensible width and never run off the screen.
 */
public final class TextFit {
    public static final int LINE = 10;

    /** The text cut to fit {@code w} pixels, ending in "…" if anything was cut. */
    public static FormattedCharSequence fit(Font f, Component t, int w) {
        if (f.width(t) <= w) return t.getVisualOrderText();
        int ell = f.width("…");
        FormattedText cut = f.substrByWidth(t, Math.max(0, w - ell));
        return net.minecraft.locale.Language.getInstance().getVisualOrder(FormattedText.composite(cut, Component.literal("…").withStyle(t.getStyle())));
    }

    /** Draws the text cut to fit; true if it had to be cut. */
    public static boolean draw(GuiGraphics g, Font f, Component t, int x, int y, int w, int color) {
        g.drawString(f, fit(f, t, w), x, y, color, false);
        return f.width(t) > w;
    }

    /** Draws the text wrapped to {@code w}, at most {@code maxLines} lines (the last cut with "…"); returns the lines drawn. */
    public static int wrapped(GuiGraphics g, Font f, Component t, int x, int y, int w, int color, int maxLines, boolean centred) {
        List<FormattedCharSequence> l = lines(f, t, w, maxLines);
        for (int i = 0; i < l.size(); i++) {
            int lx = centred ? x - f.width(l.get(i)) / 2 : x;
            g.drawString(f, l.get(i), lx, y + i * LINE, color, false);
        }
        return l.size();
    }

    public static List<FormattedCharSequence> lines(Font f, Component t, int w, int maxLines) {
        List<FormattedText> parts = f.getSplitter().splitLines(t, w, t.getStyle());
        List<FormattedCharSequence> out = new ArrayList<>();
        for (int i = 0; i < parts.size() && i < maxLines; i++) {
            FormattedText p = parts.get(i);
            if (i == maxLines - 1 && parts.size() > maxLines) {
                int ell = f.width("…");
                p = FormattedText.composite(f.substrByWidth(p, Math.max(0, w - ell)), Component.literal("…"));
            }
            out.add(net.minecraft.locale.Language.getInstance().getVisualOrder(p));
        }
        return out;
    }

    public static int lineCount(Font f, Component t, int w) {
        return Math.max(1, f.getSplitter().splitLines(t, w, t.getStyle()).size());
    }

    /** A tooltip with every line wrapped so it fits on the screen beside the mouse. */
    public static void tooltip(GuiGraphics g, Font f, List<Component> lines, int mx, int my) {
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int room = Math.max(mx - 16, sw - mx - 16);          // the tooltip goes to whichever side has more room
        int w = Math.max(100, Math.min(260, room - 8));
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component c : lines) {
            if (f.width(c) <= w) out.add(c.getVisualOrderText());
            else out.addAll(f.split(c, w));
        }
        g.renderTooltip(f, out, mx, my);
    }

    private TextFit() {}
}
