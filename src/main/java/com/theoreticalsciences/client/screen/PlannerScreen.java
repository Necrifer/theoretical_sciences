package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.orbit.MissionMath;
import com.theoreticalsciences.orbit.Planner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.12.0: the Mission Planner. Left, the destinations (or the Mission Bay's targets); right, a porkchop plot for the one
 * chosen: departure day (from now, across) against time of flight (as a fraction of the Hohmann time, down), coloured
 * by what the trip costs against its best: green at the window, red far from it. Below, the numbers, and the button.
 * A crewed trip always takes the Hohmann time; a probe's can be chosen by clicking the plot's first column.
 */
public class PlannerScreen extends Screen {
    static final int W = 360, H = 220, PX = 150, PY = 30, PW = 196, PH = 104, CELL = 4;
    static final double TOF_MIN = 0.5, TOF_MAX = 2.0;
    static final int TEXT = 0xFFCFE8FF, DIM = 0xFF7A93AA, GOOD = 0xFF6CFF9A, BAD = 0xFFFF6A5A, AMBER = 0xFFFFC24A, ACCENT = 0x66CCFF;

    private Planner.Open o;
    private int sel;
    private double tof = 1;
    private int ticks;
    private HoloButton go;
    private final List<HoloButton> rows = new ArrayList<>();

    public PlannerScreen(Planner.Open o) {
        super(Component.translatable(o.bay() ? "screen.theoreticalsciences.mission_bay" : "screen.theoreticalsciences.planner"));
        this.o = o;
    }

    public boolean same(Planner.Open other) {
        return other.bay() == o.bay() && other.pos().equals(o.pos());
    }

    public void refresh(Planner.Open other) {
        String id = entry() == null ? null : entry().id();
        o = other;
        sel = 0;
        for (int i = 0; i < o.entries().size(); i++) if (o.entries().get(i).id().equals(id)) sel = i;
        ticks = 0;
        rebuildWidgets();
    }

    Planner.Entry entry() {
        return o.entries().isEmpty() ? null : o.entries().get(Math.min(sel, o.entries().size() - 1));
    }

    int left() {
        return (width - W) / 2;
    }

    int top() {
        return (height - H) / 2;
    }

    double day() {
        return o.day() + ticks / 24000.0;
    }

    @Override
    protected void init() {
        rows.clear();
        int x = left() + 8, y = top() + 24;
        for (int i = 0; i < Math.min(9, o.entries().size()); i++) {
            final int k = i;
            HoloButton b = HoloButton.of(x, y + i * 18, 134, 16, Component.translatable(o.entries().get(i).name()), ACCENT, () -> {
                sel = k;
                tof = 1;
            });
            rows.add(addRenderableWidget(b));
        }
        go = addRenderableWidget(HoloButton.of(left() + W - 98, top() + H - 24, 90, 18,
                Component.translatable(o.bay() ? "screen.theoreticalsciences.planner.launch" : "screen.theoreticalsciences.planner.go"), ACCENT, this::send));
    }

    void send() {
        Planner.Entry e = entry();
        if (e == null) return;
        PacketDistributor.sendToServer(new Planner.Go(o.bay(), o.pos(), e.id(), o.bay() ? tof : 1));
        if (!o.bay()) onClose();
    }

    @Override
    public void tick() {
        ticks++;
        Planner.Entry e = entry();
        go.active = e != null && (!o.bay() || (o.probe() && o.busyTicks() - ticks <= 0));
    }

    double span(Planner.Entry e) {
        return e.synodic() > 0 ? Math.max(8, e.synodic() * 2) : 8;
    }

    static int heat(double ratio) {
        double t = Math.max(0, Math.min(1, (ratio - 1) / 2));
        int g = t < 0.5 ? HoloStyle.mix(0x3CE07A, 0xF0D040, (float) (t * 2)) : HoloStyle.mix(0xF0D040, 0xE04A3A, (float) ((t - 0.5) * 2));
        return 0xFF000000 | g;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x0 = left() + PX, y0 = top() + PY;
        if (o.bay() && entry() != null && mx >= x0 && mx < x0 + PW && my >= y0 && my < y0 + PH) {
            tof = TOF_MIN + (my - y0) / PH * (TOF_MAX - TOF_MIN);
            tof = Math.round(tof * 20) / 20.0;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    static String kms(double ms) {
        return String.format(Locale.ROOT, "%.1f", ms / 1000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        int x = left(), y = top();
        HoloStyle.chamferFill(g, x, y, W, H, 6, 0xE00A1420);
        HoloStyle.chamferFrame(g, x, y, W, H, 6, 0xFF2E6A8F);
        g.drawString(font, title, x + 8, y + 8, TEXT, false);
        for (int i = 0; i < rows.size(); i++) rows.get(i).selected(i == sel);
        super.render(g, mx, my, pt);
        Planner.Entry e = entry();
        if (e == null) return;
        double day = day();
        // the porkchop plot
        int x0 = x + PX, y0 = y + PY;
        double span = span(e);
        for (int cx = 0; cx < PW; cx += CELL)
            for (int cy = 0; cy < PH; cy += CELL) {
                double d = day + (cx + CELL / 2.0) / PW * span;
                double f = TOF_MIN + (cy + CELL / 2.0) / PH * (TOF_MAX - TOF_MIN);
                double r = MissionMath.windowFactor(e.synodic(), e.penalty(), e.offset(), d) * MissionMath.tofFactor(f);
                g.fill(x0 + cx, y0 + cy, x0 + cx + CELL, y0 + cy + CELL, e.free() ? 0xFF2A5A3A : heat(r));
            }
        g.renderOutline(x0 - 1, y0 - 1, PW + 2, PH + 2, 0xFF2E6A8F);
        int ty = y0 + (int) (((o.bay() ? tof : 1) - TOF_MIN) / (TOF_MAX - TOF_MIN) * PH);
        g.fill(x0, ty, x0 + PW, ty + 1, 0xAAFFFFFF);                      // the chosen time of flight
        g.fill(x0, y0, x0 + 1, y0 + PH, 0xFFFFFFFF);                      // now
        g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.porkchop"), x0, y0 + PH + 3, DIM, false);
        // the numbers
        int ly = y0 + PH + 15;
        double now = MissionMath.cost(e.deltaV(), e.synodic(), e.penalty(), e.offset(), day, o.bay() ? tof : 1);
        if (e.deltaV() <= 0) {
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.home"), x0, ly, GOOD, false);
        } else if (e.free()) {
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.charted"), x0, ly, GOOD, false);
        } else {
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.base", kms(e.deltaV())), x0, ly, TEXT, false);
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.now", kms(now)), x0, ly + 10, now <= o.stageDv() ? GOOD : BAD, false);
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.stages", kms(o.stageDv())), x0, ly + 20, TEXT, false);
            double wait = MissionMath.daysToWindow(e.synodic(), e.penalty(), e.offset(), day);
            Component win = e.synodic() <= 0 || e.penalty() <= 0 ? Component.translatable("screen.theoreticalsciences.planner.always")
                    : wait <= 0 ? Component.translatable("screen.theoreticalsciences.planner.window_open")
                    : Component.translatable("screen.theoreticalsciences.planner.window_in", String.format(Locale.ROOT, "%.1f", wait));
            g.drawString(font, win, x0, ly + 30, wait <= 0 ? GOOD : AMBER, false);
        }
        if (o.bay()) {
            int by = y + H - 46;
            int busy = o.busyTicks() - ticks;
            if (busy > 0)
                g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.busy", String.valueOf(Math.max(1, Math.round(busy / 1200.0)))), x + 8, by, AMBER, false);
            else if (!o.probe())
                g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.no_probe"), x + 8, by, BAD, false);
            g.drawString(font, Component.translatable("screen.theoreticalsciences.planner.tof", String.format(Locale.ROOT, "%.2f", tof),
                    String.valueOf(Math.max(1, Math.round(MissionMath.missionTicks(e.transferDays(), tof) / 1200.0)))), x + 8, by + 12, DIM, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
