package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.physics.PhysicsManager;
import com.theoreticalsciences.machine.physics.PhysicsModel;
import com.theoreticalsciences.machine.physics.PhysicsState;
import com.theoreticalsciences.machine.physics.RecipePhysics;
import com.theoreticalsciences.machine.tuning.TuningProfile;
import com.theoreticalsciences.machine.tuning.TuningState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The operator's console of a physics-driven multiblock (replaces the old tuning dial), in the spirit of QMD's
 * accelerator screens: three pages.
 * <ul>
 *   <li><b>Controls</b> - the parameters the operator sets: buttons, the mouse wheel (Shift x10, Ctrl x100) or typing a value.</li>
 *   <li><b>Readout</b> - every derived quantity with its formula (hover), the limits the machine must respect, and the
 *       current recipe's operating window.</li>
 *   <li><b>Plant</b> - temperature, coolant loop, heat balance, containment integrity, disturbance, overclock and parallels.</li>
 * </ul>
 */
public final class PhysicsPanel {
    public static final int W = 204, H = 176;
    private static final int ROW = 22;
    private static int tab;          // remembered between openings

    private final ControllerBlockEntity be;
    private final Font font;
    private final int accent;
    /** Machine tier look ({@link HoloStyle#tier}); physics consoles belong to multiblocks, so in practice always holographic. */
    private final int style;
    private final boolean holo;
    private int x, y;
    private final List<AbstractWidget>[] pages;
    private EditBox edit;
    private int editing = -1;
    private int scroll;
    /** screen rects of the coolant / spent tanks (for clicks with a container) */
    int coolX, coolY, spentX, spentY;
    static final int TANK_W = 16, TANK_H = 40;

    @SuppressWarnings("unchecked")
    public PhysicsPanel(ControllerBlockEntity be, Font font, int accent) {
        this.be = be;
        this.font = font;
        this.accent = accent;
        this.style = HoloStyle.tier(be.type);
        this.holo = true;            // every machine has the holographic look now
        this.pages = new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    }

    private PhysicsModel model() {
        return be.physics.model();
    }

    private static String key(String m, String kind, String id) {
        return "physics.theoreticalsciences." + m + "." + kind + "." + id;
    }

    private int a(int alpha) {
        return (alpha << 24) | (accent & 0xFFFFFF);
    }

    public void init(int x, int y, Consumer<AbstractWidget> add) {
        this.x = x;
        this.y = y;
        for (List<AbstractWidget> p : pages) p.clear();
        String[] names = {"controls", "readout", "plant"};
        for (int i = 0; i < 3; i++) {
            int t = i;
            tabs[i] = HoloButton.of(x + 4 + i * 66, y + 16, 64, 13, Component.translatable("gui.theoreticalsciences.phys.tab." + names[i]), accent, () -> select(t));
            add.accept(tabs[i]);
        }
        PhysicsModel m = model();
        if (m == null) return;
        int ry = y + 34;
        for (int i = 0; i < m.params.size() && i < 6; i++, ry += ROW) {
            int idx = i;
            pages[0].add(btn(x + W - 70, ry + 9, 16, "«", () -> nudge(idx, -10), "gui.theoreticalsciences.phys.step10"));
            pages[0].add(btn(x + W - 53, ry + 9, 16, "‹", () -> nudge(idx, -1), "gui.theoreticalsciences.phys.step1"));
            pages[0].add(btn(x + W - 36, ry + 9, 16, "›", () -> nudge(idx, 1), "gui.theoreticalsciences.phys.step1"));
            pages[0].add(btn(x + W - 19, ry + 9, 16, "»", () -> nudge(idx, 10), "gui.theoreticalsciences.phys.step10"));
        }
        edit = new EditBox(font, x + 6, y + 34, 80, 12, Component.empty());
        edit.setMaxLength(24);
        edit.setVisible(false);
        add.accept(edit);
        // plant: overclock and parallels
        if (be.tuningProfile() != null) {
            pages[2].add(btn(x + W - 34, y + 129, 14, "-", () -> sendTuning(1, -1), null));
            pages[2].add(btn(x + W - 18, y + 129, 14, "+", () -> sendTuning(1, 1), null));
            pages[2].add(btn(x + W - 34, y + 142, 14, "-", () -> sendTuning(2, -1), null));
            pages[2].add(btn(x + W - 18, y + 142, 14, "+", () -> sendTuning(2, 1), null));
        }
        for (List<AbstractWidget> p : pages) p.forEach(add);
        select(tab);
    }

    private final HoloButton[] tabs = new HoloButton[3];

    private Button btn(int bx, int by, int w, String label, Runnable r, String tip) {
        return HoloButton.of(bx, by, w, 12, label, accent, r).tipKey(tip);
    }

    private void select(int t) {
        tab = t;
        for (int i = 0; i < 3; i++) if (tabs[i] != null) tabs[i].selected(i == t);
        stopEdit();
        for (int i = 0; i < 3; i++) for (AbstractWidget w : pages[i]) w.visible = i == t;
        scroll = 0;
    }

    private void sendTuning(int field, int delta) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.TuningPayload(be.getBlockPos(), field, delta));
    }

    private void send(int idx, double v) {
        PhysicsModel m = model();
        if (m == null || idx < 0 || idx >= m.params.size() || idx >= be.physics.params.length) return;
        PhysicsModel.Param p = m.params.get(idx);
        v = Mth.clamp(v, p.min(), p.max());
        be.physics.params[idx] = v;          // shown at once; the server confirms
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.PhysicsSetPayload(be.getBlockPos(), idx, v));
    }

    private void nudge(int idx, double steps) {
        PhysicsModel m = model();
        if (m == null || idx >= be.physics.params.length) return;
        PhysicsModel.Param p = m.params.get(idx);
        double mul = Screen.hasShiftDown() ? 10 : 1;
        send(idx, be.physics.params[idx] + steps * mul * p.step());
    }

    // ------------------------------------------------------------------ input
    public boolean mouseClicked(double mx, double my, int button) {
        PhysicsModel m = model();
        if (m == null) return false;
        if (tab == 0) {
            int ry = y + 34;
            for (int i = 0; i < m.params.size() && i < 6; i++, ry += ROW) {
                if (editing != i && in(mx, my, gaugeX0() - 2, ry + 17, gaugeW() + 4, 6)) {     // the gauge: slide it
                    dragParam = i;
                    dragSent = 0;
                    return mouseDragged(mx, my);
                }
                if (in(mx, my, x + 4, ry + 9, W - 78, 12)) {
                    if (editing == i) return edit.mouseClicked(mx, my, button);     // place the cursor in the box being typed in
                    startEdit(i, ry + 9);
                    return true;
                }
            }
        }
        if (editing >= 0 && !in(mx, my, edit.getX(), edit.getY(), edit.getWidth(), edit.getHeight())) stopEdit();
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double dy) {
        PhysicsModel m = model();
        if (m == null || !in(mx, my, x, y, W, H)) return false;
        if (tab == 0) {
            int ry = y + 34;
            for (int i = 0; i < m.params.size() && i < 6; i++, ry += ROW) {
                if (in(mx, my, x, ry, W, ROW)) {
                    PhysicsModel.Param p = m.params.get(i);
                    double mul = Screen.hasControlDown() ? 100 : Screen.hasShiftDown() ? 10 : 1;
                    send(i, be.physics.params[i] + Math.signum(dy) * mul * p.step());
                    return true;
                }
            }
        } else if (tab == 1) {
            scroll = Math.max(0, scroll - (int) Math.signum(dy) * 10);
            return true;
        }
        return false;
    }

    /** While a value is being typed every key goes to the box (so 'E' does not close the screen). */
    public boolean keyPressed(int key, int scan, int mods) {
        if (editing < 0) return false;
        if (key == 256) {                // escape
            stopEdit();
            return true;
        }
        if (key == 257 || key == 335) {  // enter
            try {
                send(editing, Double.parseDouble(edit.getValue().trim().replace(',', '.').replace("×10^", "e")));
            } catch (NumberFormatException ignored) {
            }
            stopEdit();
            return true;
        }
        edit.keyPressed(key, scan, mods);
        return true;
    }

    public boolean charTyped(char c, int mods) {
        return editing >= 0 && edit.charTyped(c, mods);
    }

    private void startEdit(int i, int ey) {
        editing = i;
        edit.setX(x + 6);
        edit.setY(ey);
        edit.setWidth(W - 82);
        edit.setValue(fmtPlain(be.physics.params[i]));
        edit.setVisible(true);
        edit.setFocused(true);
        edit.moveCursorToEnd(false);
    }

    private void stopEdit() {
        editing = -1;
        if (edit != null) {
            edit.setVisible(false);
            edit.setFocused(false);
        }
    }

    public boolean editing() {
        return editing >= 0;
    }

    // ------------------------------------------------------------------ drawing
    public void render(GuiGraphics g, int mx, int my, float time) {
        smooth.frame();
        PhysicsModel m = model();
        TuningState s = be.tuning;
        int bx = x + W - 64, bw = 58;
        int icol = s.integrity > 50 ? 0xFF40C080 : s.integrity > 25 ? 0xFFE0B030 : 0xFFE04040;
        if (holo) {
            g.drawManaged(() -> {
                HoloStyle.panel(g, x, y, W, H, 5, accent, style, time, y + 31, y + H - 2, 0x0C);
                HoloStyle.header(g, x, y, W, 13, 5, accent, -1, false, time);
                HoloStyle.frame(g, x, y, W, H, 5, accent, style, time);
                g.fill(x + 4, y + 30, x + W - 4, y + 31, a(0x30));          // the rail the tab underline runs on
                HoloStyle.hbar(g, bx, y + 5, bw, 6, s.integrity / 100F, icol, accent);
            });
        } else {
            g.fill(x, y, x + W, y + H, 0xF00A0D14);
            g.renderOutline(x, y, W, H, a(0x90));
        }
        TextFit.draw(g, font, Component.translatable("gui.theoreticalsciences.phys.title"), x + (holo ? 9 : 5), y + 5, bx - x - (holo ? 13 : 9), holo ? HoloStyle.bright(accent) : accent);
        // integrity at a glance, top right
        if (!holo) {
            g.fill(bx, y + 5, bx + bw, y + 11, 0xFF05070C);
            g.fill(bx, y + 5, bx + (int) (s.integrity / 100F * bw), y + 11, icol);
            g.renderOutline(bx - 1, y + 4, bw + 2, 8, a(0x70));
        }
        if (m == null) {
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.phys.no_model"), x + 6, y + 40, W - 12, 0xFF8070, 6, false);
            return;
        }
        // the tab underline
        g.fill(x + 4 + tab * 66, y + 30, x + 68 + tab * 66, y + 31, a(0xFF));
        if (holo) {
            g.fill(x + 4 + tab * 66, y + 31, x + 68 + tab * 66, y + 32, a(0x50));
            g.fill(x + 4 + tab * 66 + 20, y + 30, x + 48 + tab * 66, y + 31, 0xC0FFFFFF);
        }
        switch (tab) {
            case 0 -> controls(g, m, mx, my);
            case 1 -> readout(g, m, mx, my);
            default -> plant(g, m, mx, my);
        }
        // why it is (not) running, along the bottom
        Component why = reason(m);
        if (why != null) {
            // wrapped (two lines at most: the bar grows upwards); hover it for the whole message
            int n = Math.min(2, TextFit.lineCount(font, why, W - 10));
            int bh = 13 + (n - 1) * TextFit.LINE, by = y + H - 2 - bh;
            if (holo) {
                int al = 0xA0 + (int) (0x5F * (0.5F + 0.5F * Mth.sin(time * 0.3F)));
                g.drawManaged(() -> {
                    HoloStyle.chamferFill(g, x + 2, by, W - 4, bh, 3, 0xD0280808);
                    HoloStyle.chamferFrame(g, x + 2, by, W - 4, bh, 3, HoloStyle.argb(al, 0xFF5040));
                    g.fill(x + 3, by + 3, x + 4, by + bh - 3, HoloStyle.argb(al, 0xFF8070));
                });
            } else
            g.fill(x + 1, by + 1, x + W - 1, y + H - 1, 0xC0200808);
            TextFit.wrapped(g, font, why, x + 5, by + 3, W - 10, 0xFFB0A0, 2, false);
            if (in(mx, my, x, by, W, bh)) tooltip = List.of(why);
        }
        if (s.faulted) {
            g.fill(x + 1, y + 34, x + W - 1, y + 64, 0xE0300808);
            if (holo) g.drawManaged(() -> {
                HoloStyle.outline(g, x + 2, y + 34, W - 4, 30, 0xC0FF4030);
                for (int k = 0; k < W - 8; k += 8) g.fill(x + 4 + k, y + 35, x + 8 + k, y + 37, 0x80FF6030);
                for (int k = 0; k < W - 8; k += 8) g.fill(x + 4 + k, y + 61, x + 8 + k, y + 63, 0x80FF6030);
            });
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.fault"), x + W / 2, y + 38, W - 8, 0xFF6050, 1, true);
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.fault_hint"), x + W / 2, y + 48, W - 8, 0xFFB0A0, 2, true);
        }
    }

    private Component reason(PhysicsModel m) {
        PhysicsState p = be.physics;
        String mid = be.type.id();
        if (p.quenchTicks > 0)
            return Component.translatable("gui.theoreticalsciences.phys.quenched", (p.quenchTicks + 19) / 20);
        if (p.stopIdx >= 0 && p.stopIdx < m.constraints.size())
            return Component.translatable("gui.theoreticalsciences.phys.stop", Component.translatable(key(mid, "c", m.constraints.get(p.stopIdx).id())));
        if (p.stateIdx >= 0 && p.stateIdx < m.states.size())
            return Component.translatable("gui.theoreticalsciences.phys.state_range", Component.translatable(key(mid, "s", m.states.get(p.stateIdx).id())));
        if (p.quenchIdx >= 0 && p.quenchIdx < m.constraints.size())
            return Component.translatable("gui.theoreticalsciences.phys.quench_risk", Component.translatable(key(mid, "c", m.constraints.get(p.quenchIdx).id())));
        if (p.damageIdx >= 0 && p.damageIdx < m.constraints.size())
            return Component.translatable("gui.theoreticalsciences.phys.damage", Component.translatable(key(mid, "c", m.constraints.get(p.damageIdx).id())));
        if (p.windowIdx >= 0 && p.windowIdx < p.recipe.windows().size())
            return Component.translatable("gui.theoreticalsciences.phys.window", name(m, p.recipe.windows().get(p.windowIdx).q()));
        if (p.wearIdx >= 0 && p.wearIdx < m.constraints.size())
            return Component.translatable("gui.theoreticalsciences.phys.wear", Component.translatable(key(mid, "c", m.constraints.get(p.wearIdx).id())));
        return null;
    }

    /** Display name of any model quantity. */
    private Component name(PhysicsModel m, String id) {
        String mid = be.type.id();
        if (m.paramIndex(id) >= 0) return Component.translatable(key(mid, "p", id));
        if (m.derivedIndex(id) >= 0) return Component.translatable(key(mid, "d", id));
        for (PhysicsModel.State s : m.states) if (s.id().equals(id)) return Component.translatable(key(mid, "s", id));
        return Component.literal(id);
    }

    private String unit(PhysicsModel m, String id) {
        for (PhysicsModel.Param p : m.params) if (p.id().equals(id)) return p.unit();
        for (PhysicsModel.Named d : m.derived) if (d.id().equals(id)) return d.unit();
        for (PhysicsModel.State s : m.states) if (s.id().equals(id)) return s.unit();
        return "";
    }

    private void controls(GuiGraphics g, PhysicsModel m, int mx, int my) {
        int ry = y + 34;
        String mid = be.type.id();
        for (int i = 0; i < m.params.size() && i < 6; i++, ry += ROW) {
            PhysicsModel.Param p = m.params.get(i);
            TextFit.draw(g, font, Component.translatable(key(mid, "p", p.id())), x + 6, ry, W - 12, 0xC0C8D8);
            if (editing != i) {
                if (holo) {
                    int fy = ry;
                    g.drawManaged(() -> HoloStyle.field(g, x + 4, fy + 8, W - 76, 14, accent));
                } else
                g.fill(x + 5, ry + 9, x + W - 73, ry + 21, 0xFF05070C);
                double sv = shown(m, i);
                String v = fmt(sv) + " " + p.unit();
                TextFit.draw(g, font, Component.literal(v), x + 8, ry + 10, W - 82, 0xFFFFFF);
                // the gauge: where the value sits between its limits (drag it to slide the value)
                int gx = gaugeX0(), gw = gaugeW(), gy = ry + 19;
                g.fill(gx, gy, gx + gw, gy + 2, a(0x30));
                double f = frac(p, sv);
                int fx = gx + (int) Math.round(f * (gw - 1));
                g.fill(gx, gy, fx + 1, gy + 2, a(dragParam == i ? 0xFF : 0xB0));
                g.fill(fx - 1, gy - 1, fx + 2, gy + 3, 0xFFFFFFFF);
                if (!holo) g.renderOutline(x + 4, ry + 8, W - 76, 14, a(0x60));
            }
            if (in(mx, my, x + 4, ry, W - 76, ROW)) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(key(mid, "p", p.id())).withStyle(ChatFormatting.AQUA));
                tip.add(Component.translatable(key(mid, "p", p.id()) + ".desc").withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.theoreticalsciences.phys.range", fmt(p.min()), fmt(p.max()), p.unit(), fmt(p.step())).withStyle(ChatFormatting.DARK_GRAY));
                tip.add(Component.translatable("gui.theoreticalsciences.phys.edit_hint").withStyle(ChatFormatting.DARK_GRAY));
                tooltip = tip;
            }
        }
        // the recipe's window, right under the controls: the thing the operator is aiming at
        ry = Math.max(ry + 2, y + 34 + 4 * ROW + 2);
        PhysicsState ps = be.physics;
        for (int i = 0; i < ps.recipe.windows().size() && ry < y + H - 26; i++, ry += 10) {
            RecipePhysics.Window w = ps.recipe.windows().get(i);
            double v = m.value(ps.env, w.q());
            boolean ok = w.test(v);
            fit(g, windowText(m, w, v), x + 6, ry, W - 10, ok ? 0x60FF90 : 0xFFC040, mx, my);
        }
    }

    private MutableComponent windowText(PhysicsModel m, RecipePhysics.Window w, double v) {
        String u = unit(m, w.q());
        String lo = w.min() > -1e300 ? fmt(w.min()) + " ≤ " : "";
        String hi = w.max() < 1e300 ? " ≤ " + fmt(w.max()) : "";
        return Component.empty().append(name(m, w.q())).append(": " + lo + fmt(v) + hi + " " + u);
    }

    private List<Component> tooltip;

    // ---- round 28: the controls glide. Each value on screen follows the real setting smoothly (a shift-click of
    // ten steps sweeps there), and the gauge under each value can be dragged like a slider.
    private final Smooth smooth = new Smooth();
    private double[] shown = new double[0];
    private int dragParam = -1;
    private long dragSent;

    /** A parameter spanning many powers of ten is shown (and dragged) on a log scale. */
    private static boolean logScale(PhysicsModel.Param p) {
        return p.min() > 0 && p.max() / p.min() >= 1000;
    }

    /** Where a value sits on its gauge, 0..1. */
    private static double frac(PhysicsModel.Param p, double v) {
        if (logScale(p)) return Mth.clamp(Math.log(v / p.min()) / Math.log(p.max() / p.min()), 0, 1);
        return p.max() > p.min() ? Mth.clamp((v - p.min()) / (p.max() - p.min()), 0, 1) : 0;
    }

    /** The value at a point of the gauge, on the parameter's step grid. */
    private static double at(PhysicsModel.Param p, double f) {
        f = Mth.clamp(f, 0, 1);
        double v = logScale(p) ? p.min() * Math.pow(p.max() / p.min(), f) : p.min() + f * (p.max() - p.min());
        if (p.step() > 0 && !logScale(p)) v = p.min() + Math.round((v - p.min()) / p.step()) * p.step();
        return Mth.clamp(v, p.min(), p.max());
    }

    private double shown(PhysicsModel m, int i) {
        if (shown.length != m.params.size()) {
            shown = new double[m.params.size()];
            for (int k = 0; k < shown.length; k++) shown[k] = k < be.physics.params.length ? be.physics.params[k] : m.params.get(k).def();
        }
        double target = i < be.physics.params.length ? be.physics.params[i] : m.params.get(i).def();
        PhysicsModel.Param p = m.params.get(i);
        shown[i] = logScale(p) ? smooth.towardLog(shown[i], target) : smooth.toward(shown[i], target);
        return shown[i];
    }

    private int gaugeX0() {
        return x + 6;
    }

    private int gaugeW() {
        return W - 80;
    }

    public boolean mouseDragged(double mx, double my) {
        PhysicsModel m = model();
        if (dragParam < 0 || m == null || dragParam >= m.params.size()) return false;
        PhysicsModel.Param p = m.params.get(dragParam);
        double v = at(p, (mx - gaugeX0()) / (double) gaugeW());
        if (dragParam < be.physics.params.length) be.physics.params[dragParam] = v;     // shown at once
        long now = System.currentTimeMillis();
        if (now - dragSent > 80) {                // not a packet every frame: about twelve a second while dragging
            dragSent = now;
            send(dragParam, v);
        }
        return true;
    }

    public boolean mouseReleased() {
        if (dragParam < 0) return false;
        if (dragParam < be.physics.params.length) send(dragParam, be.physics.params[dragParam]);    // the last position, for sure
        dragParam = -1;
        return true;
    }

    /** Draws a line cut to fit; when cut and the mouse is over it, the whole line becomes the tooltip. */
    private void fit(GuiGraphics g, Component t, int tx, int ty, int w, int color, int mx, int my) {
        if (TextFit.draw(g, font, t, tx, ty, w, color) && in(mx, my, tx, ty - 1, w, 10) && tooltip == null) tooltip = List.of(t);
    }

    private void readout(GuiGraphics g, PhysicsModel m, int mx, int my) {
        PhysicsState ps = be.physics;
        String mid = be.type.id();
        Component why = reason(m);
        int top = y + 34, bottom = y + H - 16 - (why != null && TextFit.lineCount(font, why, W - 10) > 1 ? TextFit.LINE : 0);
        g.enableScissor(x + 1, top, x + W - 1, bottom);
        int ry = top - scroll;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.phys.derived").withStyle(ChatFormatting.UNDERLINE), x + 5, ry, accent, false);
        ry += 11;
        for (PhysicsModel.Named d : m.derived) {
            if (!d.show()) continue;
            double v = d.slot() < ps.env.length ? ps.env[d.slot()] : Double.NaN;
            String label = Component.translatable(key(mid, "d", d.id())).getString();
            String val = fmt(v) + " " + d.unit();
            int vw = font.width(val);
            TextFit.draw(g, font, Component.literal(label), x + 6, ry, W - 14 - vw, 0xB0B8C8);
            g.drawString(font, val, x + W - 6 - vw, ry, Double.isFinite(v) ? 0xFFFFFF : 0xFF6050, false);
            if (in(mx, my, x, ry - 1, W, 10) && my >= top && my < bottom) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(label).withStyle(ChatFormatting.AQUA));
                if (!d.formula().isEmpty()) tip.add(Component.literal(d.formula()).withStyle(ChatFormatting.WHITE));
                tip.add(Component.translatable(key(mid, "d", d.id()) + ".desc").withStyle(ChatFormatting.GRAY));
                tooltip = tip;
            }
            ry += 10;
        }
        for (int i = 0; i < m.states.size(); i++) {
            PhysicsModel.State s = m.states.get(i);
            double v = s.slot() < ps.env.length ? ps.env[s.slot()] : Double.NaN;
            String label = Component.translatable(key(mid, "s", s.id())).getString();
            String val = fmt(v) + " " + s.unit();
            int vw = font.width(val);
            TextFit.draw(g, font, Component.literal(label), x + 6, ry, W - 14 - vw, 0xD0B0FF);
            g.drawString(font, val, x + W - 6 - vw, ry, 0xFFFFFF, false);
            if (in(mx, my, x, ry - 1, W, 10) && my >= top && my < bottom)
                tooltip = List.of(Component.literal(label).withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable(key(mid, "s", s.id()) + ".desc").withStyle(ChatFormatting.GRAY));
            ry += 10;
        }
        ry += 3;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.phys.limits").withStyle(ChatFormatting.UNDERLINE), x + 5, ry, accent, false);
        ry += 11;
        for (int i = 0; i < m.constraints.size(); i++) {
            PhysicsModel.Constraint c = m.constraints.get(i);
            double mg = i < ps.margins.length ? ps.margins[i] : Double.NaN;
            boolean ok = mg >= 0;
            int col = ok ? 0x60FF90 : switch (c.kind()) {
                case STOP -> 0xFFC040;
                case WEAR -> 0xFFE070;
                default -> 0xFF5040;
            };
            String label = (ok ? "✔ " : "✖ ") + Component.translatable(key(mid, "c", c.id())).getString();
            TextFit.draw(g, font, Component.literal(label), x + 6, ry, W - 12, col);
            if (in(mx, my, x, ry - 1, W, 10) && my >= top && my < bottom) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(key(mid, "c", c.id())).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
                if (!c.formula().isEmpty()) tip.add(Component.literal(c.formula()).withStyle(ChatFormatting.WHITE));
                tip.add(Component.translatable("gui.theoreticalsciences.phys.kind." + c.kind().name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
                tip.add(Component.translatable("gui.theoreticalsciences.phys.margin", fmt(mg)).withStyle(ChatFormatting.DARK_GRAY));
                tooltip = tip;
            }
            ry += 10;
        }
        if (!ps.recipe.windows().isEmpty()) {
            ry += 3;
            g.drawString(font, Component.translatable("gui.theoreticalsciences.phys.window_head").withStyle(ChatFormatting.UNDERLINE), x + 5, ry, accent, false);
            ry += 11;
            for (RecipePhysics.Window w : ps.recipe.windows()) {
                double v = m.value(ps.env, w.q());
                if (my >= top && my < bottom) fit(g, windowText(m, w, v), x + 6, ry, W - 12, w.test(v) ? 0x60FF90 : 0xFFC040, mx, my);
                else TextFit.draw(g, font, windowText(m, w, v), x + 6, ry, W - 12, w.test(v) ? 0x60FF90 : 0xFFC040);
                ry += 10;
            }
        }
        {
            ry += 3;
            String rt = Component.translatable("gui.theoreticalsciences.phys.rate", fmt(ps.rate)).getString();
            TextFit.draw(g, font, Component.literal(rt), x + 6, ry, W - 12, 0xFFD070);
            if (in(mx, my, x, ry - 1, W, 10) && my >= top && my < bottom && !m.rateFormula.isEmpty())
                tooltip = List.of(Component.literal(m.rateFormula).withStyle(ChatFormatting.WHITE),
                        Component.translatable("physics.theoreticalsciences." + mid + ".rate.desc").withStyle(ChatFormatting.GRAY));
            ry += 10;
        }
        g.disableScissor();
        int content = ry + scroll - top;
        scroll = Math.min(scroll, Math.max(0, content - (bottom - top)));
        if (content > bottom - top) {          // a scroll bar
            int bh = Math.max(8, (bottom - top) * (bottom - top) / content);
            int by = top + (int) ((long) (bottom - top - bh) * scroll / Math.max(1, content - (bottom - top)));
            if (holo) g.fill(x + W - 4, top, x + W - 3, bottom, a(0x30));
            g.fill(x + W - 3, by, x + W - 1, by + bh, a(0xA0));
        }
    }

    private void plant(GuiGraphics g, PhysicsModel m, int mx, int my) {
        PhysicsState ps = be.physics;
        TuningState s = be.tuning;
        String mid = be.type.id();
        int ty = y + 36;
        // temperature
        double T = ps.temperature;
        fit(g, Component.translatable("gui.theoreticalsciences.phys.temperature", fmt(T)), x + 6, ty, W - 12, T > 1000 ? 0xFF6050 : 0xC0E0FF, mx, my);
        PhysicsManager.Coolant cl = PhysicsManager.coolant(ps.coolant.getFluid());
        fit(g, cl == null ? Component.translatable("gui.theoreticalsciences.phys.no_coolant").withStyle(ChatFormatting.RED)
                : Component.translatable("gui.theoreticalsciences.phys.coolant_at", ps.coolant.getFluid().getHoverName(), fmt(cl.temperature())), x + 6, ty + 10, W - 12, 0xA0B0C0, mx, my);
        // temperature bar on a log scale from 1 K to 10^8 K, with the coil's critical temperature marked
        int gx = x + 6, gw = W - 12, gy = ty + 21;
        g.fill(gx, gy, gx + gw, gy + 5, 0xFF05070C);
        g.fillGradient(gx, gy, gx + (int) (gw * logPos(T)), gy + 5, 0xFF4080FF, 0xFFFF6040);
        if (holo) g.drawManaged(() -> {               // decade graduations (1 K .. 10^8 K) and a frame
            for (int d = 0; d <= 8; d++) g.fill(gx + (gw - 1) * d / 8, gy + 5, gx + (gw - 1) * d / 8 + 1, gy + (d % 2 == 0 ? 8 : 7), a(0x90));
            int hx = gx + (int) (gw * logPos(T));
            g.fill(gx, gy, hx, gy + 1, 0x50FFFFFF);
            if (hx > gx) g.fill(hx - 1, gy - 1, hx, gy + 6, 0xE0FFFFFF);
            HoloStyle.brackets(g, gx - 1, gy - 1, gw + 2, 7, 2, a(0xE0));
        });
        if (be.coilTier() > 0) {
            int cx = gx + (int) (gw * logPos(PhysicsModel.scTc(be.coilTier())));
            g.fill(cx, gy - 2, cx + 1, gy + 7, 0xFFFFE060);
            if (in(mx, my, cx - 2, gy - 2, 5, 9))
                tooltip = List.of(Component.translatable("gui.theoreticalsciences.phys.coil_tc", be.coilTier(), fmt(PhysicsModel.scTc(be.coilTier())), fmt(PhysicsModel.scB0(be.coilTier()))));
        }
        // heat balance
        fit(g, Component.translatable("gui.theoreticalsciences.phys.heat", fmt(ps.heatKw), fmt(ps.coolingKw)), x + 6, ty + 31, W - 52, 0xFFB080, mx, my);
        fit(g, Component.translatable("gui.theoreticalsciences.phys.flow", fmt(ps.coolantFlow)), x + 6, ty + 41, W - 52, 0x90A0B0, mx, my);
        // tanks
        coolX = x + W - 44;
        coolY = ty + 50;
        spentX = x + W - 24;
        spentY = ty + 50;
        drawTank(g, ps.coolant, coolX, coolY);
        drawTank(g, ps.spent, spentX, spentY);
        fit(g, Component.translatable("gui.theoreticalsciences.phys.tanks"), x + 6, ty + 54, W - 52, 0x8090A0, mx, my);
        if (in(mx, my, coolX, coolY, TANK_W, TANK_H)) tooltip = tankTip(ps.coolant, "gui.theoreticalsciences.phys.coolant_tank");
        if (in(mx, my, spentX, spentY, TANK_W, TANK_H)) tooltip = tankTip(ps.spent, "gui.theoreticalsciences.phys.spent_tank");
        // disturbance gauge
        int dy = ty + 64;
        TextFit.draw(g, font, Component.translatable("gui.theoreticalsciences.phys.disturbance"), x + 6, dy, W - 52, 0xA0A8B8);
        int dx0 = x + 6, dw = W - 60;
        g.fill(dx0, dy + 10, dx0 + dw, dy + 14, 0xFF05070C);
        if (holo) g.drawManaged(() -> {
            HoloStyle.outline(g, dx0 - 1, dy + 9, dw + 2, 6, a(0x50));
            for (int k = 1; k < 10; k++) g.fill(dx0 + dw * k / 10, dy + 13, dx0 + dw * k / 10 + 1, dy + 14, a(k == 5 ? 0x00 : 0x70));
            float d = Math.abs((float) ps.disturbance);
            int warn = d > 0.66F ? 0xFF5040 : d > 0.33F ? 0xFFC040 : 0x40FF90;
            g.fill(dx0 + dw + 4, dy + 9, dx0 + dw + 7, dy + 15, HoloStyle.argb(0xD0, warn));
        });
        g.fill(dx0 + dw / 2, dy + 9, dx0 + dw / 2 + 1, dy + 15, 0x80FFFFFF);
        int dpos = dx0 + (int) ((ps.disturbance + 1) / 2 * (dw - 2));
        g.fill(dpos, dy + 9, dpos + 2, dy + 15, s.autoCalibrator ? 0xFF60FFD0 : 0xFFFFD040);
        if (in(mx, my, dx0, dy, dw, 16))
            tooltip = List.of(Component.translatable("gui.theoreticalsciences.phys.disturbance").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("physics.theoreticalsciences." + mid + ".disturb").withStyle(ChatFormatting.GRAY),
                    Component.literal("d = " + String.format("%+.3f", ps.disturbance)).withStyle(ChatFormatting.WHITE));
        // overclock / parallel (as before)
        TuningProfile tp = be.tuningProfile();
        if (tp != null) {
            fit(g, s.omega ? Component.translatable("gui.theoreticalsciences.overclock_omega").withStyle(ChatFormatting.DARK_RED)
                    : Component.translatable("gui.theoreticalsciences.overclock", s.overclock, tp.maxOverclock()), x + 6, y + 131, W - 12, 0xC0C8D8, mx, my);
            fit(g, Component.translatable("gui.theoreticalsciences.parallel", s.parallel, be.maxParallel()), x + 6, y + 144, W - 12, 0xC0C8D8, mx, my);
        }
        fit(g, Component.translatable("gui.theoreticalsciences.phys.integrity", Math.round(s.integrity), ps.quenchCount), x + 6, y + 119, W - 12, 0xA0A8B8, mx, my);
        if (be.coilTier() > 0 || be.emitterTier() > 0)
            fit(g, Component.translatable("gui.theoreticalsciences.tiers", be.coilTier(), be.emitterTier()), x + 6, y + 154, W - 12, 0xFFC860, mx, my);
    }

    private List<Component> tankTip(FluidTank t, String label) {
        FluidStack fs = t.getFluid();
        List<Component> l = new ArrayList<>();
        l.add(Component.translatable(label).withStyle(ChatFormatting.AQUA));
        l.add(fs.isEmpty() ? Component.translatable("gui.theoreticalsciences.empty") : fs.getHoverName());
        l.add(Component.literal(String.format("%,d / %,d mB", fs.getAmount(), t.getCapacity())).withStyle(ChatFormatting.GRAY));
        PhysicsManager.Coolant c = PhysicsManager.coolant(fs);
        if (c != null)
            l.add(Component.translatable("gui.theoreticalsciences.phys.coolant_props", fmt(c.temperature()), fmt(c.capacity())).withStyle(ChatFormatting.DARK_AQUA));
        l.add(Component.translatable(label + ".hint").withStyle(ChatFormatting.DARK_GRAY));
        return l;
    }

    private void drawTank(GuiGraphics g, FluidTank t, int tx, int ty) {
        if (holo) g.drawManaged(() -> HoloStyle.tubeBack(g, tx, ty, TANK_W, TANK_H, accent));
        else g.fill(tx, ty, tx + TANK_W, ty + TANK_H, 0xFF05070C);
        FluidStack fs = t.getFluid();
        int fluidTop = -1;
        if (!fs.isEmpty()) {
            int fh = Math.max(1, fs.getAmount() * TANK_H / t.getCapacity());
            fluidTop = ty + TANK_H - fh;
            var ext = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fs.getFluid());
            var sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
            int tint = ext.getTintColor(fs);
            float r = ((tint >> 16) & 255) / 255F, gg = ((tint >> 8) & 255) / 255F, b = (tint & 255) / 255F;
            int drawn = 0;
            while (drawn < fh) {
                int seg = Math.min(16, fh - drawn);
                g.blit(tx, ty + TANK_H - drawn - seg, 0, 16, seg, sprite, r, gg, b, 1.0F);
                drawn += seg;
            }
        }
        if (holo) {
            int ft = fluidTop;
            g.drawManaged(() -> HoloStyle.tubeFront(g, tx, ty, TANK_W, TANK_H, accent, true, ft));
        } else
        g.renderOutline(tx - 1, ty - 1, TANK_W + 2, TANK_H + 2, a(0x80));
    }

    /** Index of the clicked physics tank in the machine's fluid handler (recipe tanks come first), or -1. */
    public int tankAt(double mx, double my) {
        if (tab != 2 || model() == null) return -1;
        if (in(mx, my, coolX, coolY, TANK_W, TANK_H)) return be.tanks.length;
        if (in(mx, my, spentX, spentY, TANK_W, TANK_H)) return be.tanks.length + 1;
        return -1;
    }

    public void renderTooltip(GuiGraphics g, int mx, int my) {
        if (tooltip != null) TextFit.tooltip(g, font, tooltip, mx, my);
        tooltip = null;
    }

    private static double logPos(double T) {
        return Mth.clamp(Math.log10(Math.max(1, T)) / 8.0, 0, 1);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    /** Four significant figures, scientific notation outside 0.001 .. 99999. */
    public static String fmt(double v) {
        if (Double.isNaN(v)) return "—";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        double a = Math.abs(v);
        if (a == 0) return "0";
        if (a >= 1e-3 && a < 1e5) {
            String s = String.format(java.util.Locale.ROOT, "%.4g", v);
            if (s.contains(".") && !s.contains("e")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
            return s;
        }
        int e = (int) Math.floor(Math.log10(a));
        double mant = v / Math.pow(10, e);
        return String.format(java.util.Locale.ROOT, "%.3f×10%s", mant, sup(e));
    }

    /** A value as a program would print it (plain digits or 1.2345e-7). */
    public static String fmtCode(double v) {
        if (!Double.isFinite(v)) return Double.isNaN(v) ? "NaN" : v > 0 ? "inf" : "-inf";
        return fmtPlain(v);
    }

    /** Shows the console again after it was hidden (the open page's widgets only). */
    public void refresh() {
        int keep = scroll;
        select(tab);
        scroll = keep;
    }

    /** Hides every widget of the console (the machine screen shows something else in its place). */
    public void hide() {
        stopEdit();
        for (HoloButton b : tabs) if (b != null) b.visible = false;
        for (List<AbstractWidget> p : pages) for (AbstractWidget w : p) w.visible = false;
    }

    public void showTabs() {
        for (HoloButton b : tabs) if (b != null) b.visible = true;
    }

    private static String fmtPlain(double v) {
        double a = Math.abs(v);
        if (a != 0 && (a < 1e-3 || a >= 1e6)) return String.format(java.util.Locale.ROOT, "%.4e", v);
        String s = String.format(java.util.Locale.ROOT, "%.6f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    private static String sup(int e) {
        String d = "⁰¹²³⁴⁵⁶⁷⁸⁹";
        StringBuilder b = new StringBuilder();
        String s = Integer.toString(e);
        for (char c : s.toCharArray()) b.append(c == '-' ? '⁻' : d.charAt(c - '0'));
        return b.toString();
    }
}
