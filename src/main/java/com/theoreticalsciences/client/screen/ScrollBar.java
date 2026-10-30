package com.theoreticalsciences.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A plain vertical scroll bar for our screens: shows where you are in a long list, can be dragged, clicked to jump,
 * and follows the mouse wheel. The owning screen keeps the scroll offset in pixels through {@link #value}.
 */
final class ScrollBar {
    static final int W = 6;
    int x, y, h;
    /** total height of the content and of the visible window, in pixels */
    int content, view;
    int value;
    private boolean grabbed;
    private int grabOffset;

    void set(int x, int y, int h, int content, int view) {
        this.x = x;
        this.y = y;
        this.h = h;
        this.content = content;
        this.view = view;
        clamp();
    }

    int max() {
        return Math.max(0, content - view);
    }

    boolean needed() {
        return max() > 0;
    }

    void clamp() {
        value = Math.max(0, Math.min(value, max()));
    }

    void scroll(double wheel, int step) {
        value -= (int) Math.round(wheel * step);
        clamp();
    }

    private int thumbH() {
        return content <= 0 ? h : Math.max(12, Math.min(h, (int) ((long) h * view / content)));
    }

    private int thumbY() {
        int m = max();
        return m == 0 ? y : y + (int) ((long) (h - thumbH()) * value / m);
    }

    boolean over(double mx, double my) {
        return needed() && mx >= x && mx < x + W && my >= y && my < y + h;
    }

    /** A click on the bar: grabbing the thumb drags it; clicking the track jumps there. */
    boolean click(double mx, double my) {
        if (!over(mx, my)) return false;
        int ty = thumbY(), th = thumbH();
        grabbed = true;
        if (my >= ty && my < ty + th) grabOffset = (int) my - ty;
        else {
            grabOffset = th / 2;
            drag(my);
        }
        return true;
    }

    boolean drag(double my) {
        if (!grabbed) return false;
        int m = max(), room = h - thumbH();
        if (room > 0) value = (int) Math.round((my - grabOffset - y) * m / room);
        clamp();
        return true;
    }

    boolean release() {
        boolean was = grabbed;
        grabbed = false;
        return was;
    }

    void draw(GuiGraphics g, int mx, int my, int track, int thumb, int thumbHover) {
        if (!needed()) return;
        g.fill(x, y, x + W, y + h, track);
        int ty = thumbY(), th = thumbH();
        boolean hov = grabbed || (mx >= x && mx < x + W && my >= ty && my < ty + th);
        g.fill(x + 1, ty, x + W - 1, ty + th, hov ? thumbHover : thumb);
    }
}
