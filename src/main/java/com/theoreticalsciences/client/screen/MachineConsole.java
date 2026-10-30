package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.client.screen.forge.CoreCode;
import com.theoreticalsciences.client.transit.Fx2D;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.machine.MachineStatus;
import com.theoreticalsciences.machine.MachineType;
import com.theoreticalsciences.machine.physics.PhysicsModel;
import com.theoreticalsciences.machine.physics.PhysicsState;
import com.theoreticalsciences.machine.physics.RecipePhysics;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The operator console of a late-game machine, in the manner of the Omnipotence Core's terminal: around the machine's
 * own panel and its physics console it adds
 * <ul>
 *   <li>a <b>simulation</b> of what the machine is doing - the recipe's inputs streaming into a schematic of the
 *       machine's process (a collider ring, an accretion disk, a rotating tesseract, a plasma core or a reaction vessel,
 *       chosen by the machine's effect), its products forming as the cycle runs, and an oscilloscope trace of the
 *       quantity the recipe needs held in its window;</li>
 *   <li>a <b>terminal log</b> of what happens (status changes, recipes loaded and finished, every setting changed, the
 *       window entered or lost, quenches, wear);</li>
 *   <li>the <b>program listing</b> the console runs, typed out live like the Core's.</li>
 * </ul>
 * Everything here only reads the machine's synced state; nothing is sent to the server.
 */
public final class MachineConsole {
    private final MachineBlockEntity be;
    private final int accent;
    private final long opened = System.currentTimeMillis();
    private final List<String> log = new ArrayList<>();
    final CoreCode code = new CoreCode();
    private final Arch arch;

    // oscilloscope: the last samples of the traced quantity (raw values) and of the energy level
    private static final int SAMPLES = 160;
    private final double[] trace = new double[SAMPLES];
    private final float[] power = new float[SAMPLES];
    private int head, filled;

    // what was seen last tick, to notice changes
    private MachineStatus lastStatus;
    private ResourceLocation lastRecipe;
    private float lastFrac;
    private double[] lastParams = new double[0];
    private boolean lastInWindow = true, lastQuench;
    private int lastIntegrityStep = 20, lastOc = -1, lastPar = -1, cycles;
    private int ticks;
    private float phase;           // animation phase: runs fast while working, crawls while idle
    private long lastFrame = System.nanoTime();

    enum Arch { RING, WELL, HYPER, STAR, VESSEL }

    MachineConsole(MachineBlockEntity be, int accent) {
        this.be = be;
        this.accent = accent & 0xFFFFFF;
        this.arch = arch(be.type.effect);
        code.cCall = 0xFF000000 | HoloStyle.bright(this.accent);
        String name = Component.translatable("block.theoreticalsciences." + be.type.id()).getString();
        say("console online :: " + name.toUpperCase(Locale.ROOT));
        var p = be.getBlockPos();
        say("link " + p.getX() + " " + p.getY() + " " + p.getZ() + " :: " + (be.getLevel() == null ? "?" : be.getLevel().dimension().location().getPath()));
        code.emit("import machine." + be.type.id() + ", physics, sim  // operator console");
        code.emit("let m = Machine.attach(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")");
        code.emit("let s = sim.model(\"" + arch.name().toLowerCase(Locale.ROOT) + "\")  // " + effectName());
        if (phys() != null && model() != null) code.emit("m.physics.load(\"" + be.type.id() + "\")  // " + model().params.size() + " controls");
    }

    static Arch arch(MachineType.Effect e) {
        return switch (e) {
            case COLLIDER, SYNCHROTRON, ANNIHILATION, PULSAR -> Arch.RING;
            case BLACK_HOLE, BLACK_HOLE_DISK, WHITE_HOLE, DARK_LENS, EYE, WORMHOLE -> Arch.WELL;
            case TESSERACT, MATRIX, POCKET_UNIVERSE, CONCEPT, OMNILEX, BREACH, RIFT, TIME_LOOP, EXPANSION, FOAM -> Arch.HYPER;
            case PLASMA, STAR_FORGE, DYSON, MATRIOSHKA -> Arch.STAR;
            default -> Arch.VESSEL;
        };
    }

    private String effectName() {
        return be.type.effect == MachineType.Effect.NONE ? "reaction vessel" : be.type.effect.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void say(String s) {
        double t = (System.currentTimeMillis() - opened) / 1000.0;
        log.add(String.format(Locale.ROOT, "[%06.1f] %s", t, s));
        while (log.size() > 80) log.remove(0);
    }

    // ------------------------------------------------------------------ machine state
    @Nullable
    private ControllerBlockEntity ctrl() {
        return be instanceof ControllerBlockEntity c ? c : null;
    }

    @Nullable
    private PhysicsState phys() {
        ControllerBlockEntity c = ctrl();
        return c != null && c.hasPhysics() ? c.physics : null;
    }

    @Nullable
    private PhysicsModel model() {
        PhysicsState p = phys();
        return p == null ? null : p.model();
    }

    private boolean running() {
        return be.getStatus() == MachineStatus.RUNNING || be.getStatus() == MachineStatus.GENERATING;
    }

    private float frac() {
        int m = be.getMaxProgress();
        return m > 0 ? Mth.clamp(be.getProgress() / (float) m, 0F, 1F) : 0F;
    }

    private ResourceLocation cachedId;
    private QHRecipe cached;

    @Nullable
    private QHRecipe recipe() {
        ResourceLocation id = be.shownRecipe();
        if (id == null || be.getLevel() == null) return null;
        if (!id.equals(cachedId)) {
            cachedId = id;
            var h = be.getLevel().getRecipeManager().byKey(id).orElse(null);
            cached = h != null && h.value() instanceof QHRecipe q ? q : null;
        }
        return cached;
    }

    private String pname(String kind, String id) {
        return Component.translatable("physics.theoreticalsciences." + be.type.id() + "." + kind + "." + id).getString();
    }

    @Nullable
    private RecipePhysics.Window window() {
        PhysicsState p = phys();
        return p == null || p.recipe.windows().isEmpty() ? null : p.recipe.windows().get(0);
    }

    // ------------------------------------------------------------------ once per tick: sample and notice changes
    void tick() {
        ticks++;
        PhysicsState ps = phys();
        PhysicsModel m = model();
        RecipePhysics.Window w = window();
        double sample = w != null && m != null ? m.value(ps.env, w.q()) : ps != null ? ps.rate : frac();
        trace[head] = sample;
        long cap = Math.max(1L, be.energy.capacity());
        power[head] = (float) Math.min(1.0, (double) be.energy.stored() / cap);
        head = (head + 1) % SAMPLES;
        filled = Math.min(SAMPLES, filled + 1);

        MachineStatus st = be.getStatus();
        if (st != lastStatus) {
            say("status -> " + st.label(be.type).getString());
            code.emit("m.status  // " + st.name());
            lastStatus = st;
        }
        ResourceLocation r = be.shownRecipe();
        if (r != null && !r.equals(lastRecipe)) {
            QHRecipe q = recipe();
            String out = q == null ? r.getPath() : outName(q);
            say("recipe loaded: " + out);
            code.emit("let r = recipes.get(\"" + r + "\")");
            if (q != null) code.emit("r.time = " + q.duration + "t; r.energy = " + q.energy + " FE/t");
            lastRecipe = r;
        }
        float f = frac();
        if (lastFrac > 0.75F && f < 0.25F && r != null && r.equals(lastRecipe)) {
            cycles++;
            int par = be.shownParallel();
            QHRecipe q = recipe();
            say("cycle " + cycles + " complete" + (par > 1 ? " x" + par : "") + (q != null ? " :: " + outName(q) : ""));
            code.emit("yield r.outputs()  // cycle " + cycles + " OK");
        }
        lastFrac = f;
        // settings
        if (ps != null && m != null) {
            if (lastParams.length != ps.params.length) lastParams = ps.params.clone();
            for (int i = 0; i < ps.params.length && i < m.params.size(); i++) {
                if (Math.abs(ps.params[i] - lastParams[i]) > 1e-9 * Math.max(1, Math.abs(lastParams[i]))) {
                    var p = m.params.get(i);
                    String v = PhysicsPanel.fmt(ps.params[i]) + " " + p.unit();
                    say("set " + pname("p", p.id()) + " = " + v);
                    code.emit("set" + i, "m.physics." + p.id() + " = " + PhysicsPanel.fmtCode(ps.params[i]) + "  // " + p.unit());
                    lastParams[i] = ps.params[i];
                }
            }
            if (w != null) {
                boolean in = w.test(m.value(ps.env, w.q()));
                if (in != lastInWindow) {
                    say((in ? "window held: " : "window lost: ") + pname(kindOf(m, w.q()), w.q()));
                    code.emit("assert window(\"" + w.q() + "\").contains(" + PhysicsPanel.fmtCode(m.value(ps.env, w.q())) + ") -> " + (in ? "OK" : "FAIL"));
                    lastInWindow = in;
                }
            }
            boolean q = ps.quenchTicks > 0;
            if (q && !lastQuench) {
                say("!! QUENCH :: containment lost, magnets recovering");
                code.emit("throw Quench(\"" + be.type.id() + "\")  // ERROR");
            }
            lastQuench = q;
        }
        ControllerBlockEntity c = ctrl();
        if (c != null) {
            int step = (int) (c.tuning.integrity / 5);
            if (step < lastIntegrityStep) {
                say("integrity " + Math.round(c.tuning.integrity) + "%");
                if (step <= 5) code.emit("m.containment.integrity  // WARNING " + Math.round(c.tuning.integrity) + "%");
            }
            lastIntegrityStep = step;
            int oc = c.tuning.overclock, par = c.tuning.parallel;
            if (lastOc >= 0 && oc != lastOc) {
                say("overclock -> " + oc);
                code.emit("oc", "m.overclock = " + oc);
            }
            if (lastPar >= 0 && par != lastPar) {
                say("parallel -> " + par);
                code.emit("par", "m.parallel = " + par);
            }
            lastOc = oc;
            lastPar = par;
        }
    }

    private static String kindOf(PhysicsModel m, String id) {
        if (m.paramIndex(id) >= 0) return "p";
        if (m.derivedIndex(id) >= 0) return "d";
        return "s";
    }

    private static String outName(QHRecipe q) {
        if (!q.itemOutputs.isEmpty()) return q.itemOutputs.get(0).getHoverName().getString();
        if (!q.fluidOutputs.isEmpty()) return q.fluidOutputs.get(0).getHoverName().getString();
        return "-";
    }

    /** What the program does while nobody touches anything. */
    private String idle(int n) {
        PhysicsState ps = phys();
        if (!running()) return switch (n % 3) {
            case 0 -> "await m.ready()  // " + be.getStatus().name();
            case 1 -> "  sleep(1000)";
            default -> null;
        };
        float f = frac();
        return switch (n % 4) {
            case 0 -> String.format(Locale.ROOT, "s.step(dt = 0.05)  // %d%%", Math.round(f * 100));
            case 1 -> ps != null ? "  rate = m.physics.rate() -> " + PhysicsPanel.fmtCode(ps.rate) : "  m.energy.draw()  // OK";
            case 2 -> ps != null ? "  T = " + PhysicsPanel.fmtCode(ps.temperature) + "  // K" : "  m.progress = " + be.getProgress() + "/" + be.getMaxProgress();
            default -> window() != null ? "  assert window.held() -> " + (lastInWindow ? "OK" : "FAIL") : null;
        };
    }

    // ------------------------------------------------------------------ the monitor
    void monitor(GuiGraphics g, Font font, int L, int T, int W, int H, int style, float time, int reserveRight) {
        boolean run = running();
        g.drawManaged(() -> {
            HoloStyle.panel(g, L, T, W, H, 8, accent, style, time, T + 18, T + H - 4, 0x08);
            // a faint measuring grid
            for (int x = L + 16; x < L + W - 8; x += 16) g.fill(x, T + 18, x + 1, T + H - 4, HoloStyle.argb(0x07, accent));
            for (int y = T + 32; y < T + H - 4; y += 16) g.fill(L + 4, y, L + W - 4, y + 1, HoloStyle.argb(0x07, accent));
            HoloStyle.header(g, L, T, W, 15, 8, accent, be.getMaxProgress() > 0 ? frac() : -1, run, time);
            HoloStyle.frame(g, L, T, W, H, 8, accent, style, time);
            HoloStyle.lamp(g, L + W - 14, T + 7, be.getStatus().color, run, time);
        });
        String name = Component.translatable("block.theoreticalsciences." + be.type.id()).getString().toUpperCase(Locale.ROOT);
        String head = name + "  //  OPERATOR CONSOLE";
        String clock = String.format(Locale.ROOT, "t+%07.1fs", (System.currentTimeMillis() - opened) / 1000.0);
        int cw = font.width(clock);
        boolean clockFits = W - 40 - reserveRight - cw - font.width(head) >= 8;
        g.drawString(font, cut(font, head, W - 40 - reserveRight - (clockFits ? cw + 8 : 0)), L + 12, T + 5, HoloStyle.bright(accent), false);
        if (clockFits) g.drawString(font, clock, L + W - 22 - reserveRight - cw, T + 5, HoloStyle.mix(0x8899AA, accent, 0.3F), false);
    }

    /** A thin window with its title written into the top edge, like the Core's terminal. */
    void frame(GuiGraphics g, Font font, int x, int y, int w, int h, String title) {
        g.drawManaged(() -> {
            g.fill(x, y, x + w, y + h, 0x60000000);
            HoloStyle.outline(g, x, y, w, h, HoloStyle.argb(0x50, accent));
            HoloStyle.brackets(g, x, y, w, h, 5, HoloStyle.argb(0xE0, HoloStyle.bright(accent)));
        });
        if (title != null && w > 30) {
            String t = cut(font, title, w - 16);
            g.fill(x + 6, y - 1, x + 10 + font.width(t), y + 1, 0xFF05080C);
            g.drawString(font, t, x + 8, y - 4, HoloStyle.bright(accent), false);
        }
    }

    // ------------------------------------------------------------------ the simulation
    void sim(GuiGraphics g, Font font, int x, int y, int w, int h, int mx, int my, List<Component> tipOut) {
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (now - lastFrame) / 1e9F);
        lastFrame = now;
        PhysicsState ps = phys();
        boolean run = running();
        float rate = ps == null ? 1F : (float) Mth.clamp(Double.isFinite(ps.rate) ? ps.rate : 0, 0, 4);
        phase += dt * 20F * (run ? 0.35F + 0.65F * rate : 0.05F);

        frame(g, font, x, y, w, h, "SIMULATION :: " + effectName().toUpperCase(Locale.ROOT));
        int traceH = h >= 150 ? 40 : h >= 110 ? 30 : 0;
        int readH = 11;
        int dx = x + 3, dy = y + 6, dw = w - 6, dh = h - 9 - traceH - readH - (traceH > 0 ? 4 : 0);
        if (dh < 30) {
            traceH = 0;
            dh = h - 9 - readH;
        }
        QHRecipe r = recipe();
        boolean trouble = ps != null && (ps.quenchTicks > 0 || ps.stopIdx >= 0 || !lastInWindow);
        float f = frac();

        // ---- inputs / outputs: little nodes on both sides, with the recipe's ingredients in them
        List<ItemStack> ins = new ArrayList<>(), outs = new ArrayList<>();
        List<FluidStack> fins = new ArrayList<>(), fouts = new ArrayList<>();
        if (r != null) {
            for (var si : r.itemInputs) {
                ItemStack[] it = si.ingredient().getItems();
                if (it.length > 0) ins.add(it[(int) (System.currentTimeMillis() / 1000 % it.length)]);
            }
            for (var sf : r.fluidInputs) {
                FluidStack[] fl = sf.getFluids();
                if (fl.length > 0) fins.add(fl[0]);
            }
            outs.addAll(r.itemOutputs);
            fouts.addAll(r.fluidOutputs);
        }
        int nIn = Math.min(5, ins.size() + fins.size()), nOut = Math.min(5, outs.size() + fouts.size());
        boolean nodes = dw >= 130;
        int inX = dx + 4, outX = dx + dw - 22;
        int coreL = nodes ? inX + 30 : dx + 4, coreR = nodes ? outX - 12 : dx + dw - 4;
        float cx = (coreL + coreR) / 2F, cy = dy + dh / 2F + 2;
        float R = Math.max(8, Math.min((coreR - coreL) * 0.42F, dh * 0.42F));
        float jit = trouble ? (float) (Math.sin(phase * 7.3) * 0.8) : 0;
        int col = trouble ? 0xFF5040 : accent;
        int hot = HoloStyle.bright(col);

        Fx2D fx = Fx2D.begin(g, Fx2D.Blend.ADD);
        // feed lines from each input node into the core, product lines out of it
        if (nodes) {
            for (int i = 0; i < nIn; i++) {
                float ny = nodeY(dy, dh, i, nIn) + 8;
                feed(fx, inX + 18, ny, cx - R * 0.9F, cy, i, run ? 1F : 0.25F, col);
            }
            for (int i = 0; i < nOut; i++) {
                float ny = nodeY(dy, dh, i, nOut) + 8;
                feed(fx, cx + R * 0.9F, cy, outX - 2, ny, i + 7, run ? Math.max(0.15F, f) : 0.15F, outCol());
            }
        }
        switch (arch) {
            case RING -> ring(fx, cx + jit, cy, R, col, hot, run, f);
            case WELL -> well(fx, cx + jit, cy, R, col, hot, run, f);
            case HYPER -> hyper(fx, cx + jit, cy, R, col, hot, run, f);
            case STAR -> star(fx, cx + jit, cy, R, col, hot, run, f);
            default -> vessel(fx, cx + jit, cy, R, col, hot, run, f);
        }
        fx.end();
        // nodes and the stacks in them
        if (nodes) {
            int k = 0;
            for (int i = 0; i < ins.size() && k < nIn; i++, k++) node(g, inX, nodeY(dy, dh, k, nIn), ins.get(i), null, 1F, mx, my, tipOut);
            for (int i = 0; i < fins.size() && k < nIn; i++, k++) node(g, inX, nodeY(dy, dh, k, nIn), ItemStack.EMPTY, fins.get(i), 1F, mx, my, tipOut);
            k = 0;
            float shown = run ? 0.35F + 0.65F * f : 0.35F;
            for (int i = 0; i < outs.size() && k < nOut; i++, k++) node(g, outX, nodeY(dy, dh, k, nOut), outs.get(i), null, shown, mx, my, tipOut);
            for (int i = 0; i < fouts.size() && k < nOut; i++, k++) node(g, outX, nodeY(dy, dh, k, nOut), ItemStack.EMPTY, fouts.get(i), shown, mx, my, tipOut);
        }
        // the state, written over the scene
        String state = ps != null && ps.quenchTicks > 0 ? "QUENCH" : run ? (trouble ? "UNSTABLE" : "RUNNING " + Math.round(f * 100) + "%") : "STANDBY :: " + be.getStatus().label(be.type).getString();
        HoloStyle.miniText(g, font, cut(font, state, (int) ((dw - 8) / 0.75F)), dx + 4, dy + 2, trouble ? 0xFF6050 : run ? HoloStyle.bright(accent) : 0x8090A0, 0.75F, false);
        if (r == null) HoloStyle.miniText(g, font, "no recipe loaded", (int) cx, dy + dh - 8, 0x707888, 0.75F, true);

        // ---- the readout line
        int ry = dy + dh + 1;
        String read;
        if (ps != null) read = String.format(Locale.ROOT, "rate x%s   T %s K   d %+.3f   %s", PhysicsPanel.fmt(ps.rate), PhysicsPanel.fmt(ps.temperature), ps.disturbance,
                be.getMaxProgress() > 0 ? Math.round(f * 100) + "%" : "");
        else read = String.format(Locale.ROOT, "cycle %d%%   %s/%s t   x%d", Math.round(f * 100), be.getProgress(), be.getMaxProgress(), be.shownParallel());
        g.drawString(font, cut(font, read, dw - 4), dx + 2, ry, 0xA0B0C0, false);

        // ---- the oscilloscope
        if (traceH > 0) scope(g, font, dx, ry + readH + 2, dw, traceH, mx, my, tipOut);
    }

    private int outCol() {
        return HoloStyle.outputColour(accent);
    }

    private static int nodeY(int dy, int dh, int i, int n) {
        int span = Math.min(dh - 18, n * 22);
        int top = dy + (dh - span) / 2;
        return n <= 1 ? dy + dh / 2 - 8 : top + i * (span - 16) / (n - 1);
    }

    private void node(GuiGraphics g, int x, int y, ItemStack s, @Nullable FluidStack fs, float alpha, int mx, int my, List<Component> tip) {
        g.drawManaged(() -> HoloStyle.slot(g, x - 1, y - 1, accent, false, true, false, 0));
        if (!s.isEmpty()) {
            g.renderItem(s, x, y);
            if (s.getCount() > 1) {
                String c = InfinityCacheScreen.compact(s.getCount());
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                HoloStyle.miniText(g, Minecraft.getInstance().font, c, x + 17 - (int) (Minecraft.getInstance().font.width(c) * 0.75F), y + 11, 0xFFFFFF, 0.75F, false);
                g.pose().popPose();
            }
        } else if (fs != null && !fs.isEmpty()) {
            var ext = IClientFluidTypeExtensions.of(fs.getFluid());
            var sprite = Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
            int t = ext.getTintColor(fs);
            g.blit(x, y, 0, 16, 16, sprite, ((t >> 16) & 255) / 255F, ((t >> 8) & 255) / 255F, (t & 255) / 255F, 1F);
        }
        if (alpha < 1F) g.fill(x, y, x + 16, y + 16, ((int) ((1 - alpha) * 0xB0) << 24) | 0x05070C);
        if (mx >= x && my >= y && mx < x + 16 && my < y + 16) {
            if (!s.isEmpty()) tip.addAll(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), s));
            else if (fs != null) {
                tip.add(fs.getHoverName());
                tip.add(Component.literal(String.format("%,d mB", fs.getAmount())).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }

    /** A curved conduit with packets running along it. */
    private void feed(Fx2D fx, float x0, float y0, float x1, float y1, int seed, float lit, int col) {
        float mxp = (x0 + x1) / 2F;
        float px = x0, py = y0;
        for (int s = 1; s <= 16; s++) {
            float u = s / 16F;
            float qx = bez(x0, mxp, mxp, x1, u), qy = bez(y0, y0, y1, y1, u);
            fx.line(px, py, qx, qy, 1F, col, 0.18F + 0.25F * lit);
            px = qx;
            py = qy;
        }
        for (int k = 0; k < 3; k++) {
            float u = ((phase * 0.035F + k / 3F + seed * 0.137F) % 1F + 1F) % 1F;
            float qx = bez(x0, mxp, mxp, x1, u), qy = bez(y0, y0, y1, y1, u);
            fx.disc(qx, qy, 2.2F, 8, 0xFFFFFF, 0.9F * lit, col, 0F);
        }
    }

    private static float bez(float a, float b, float c, float d, float u) {
        float v = 1 - u;
        return v * v * v * a + 3 * v * v * u * b + 3 * v * u * u * c + u * u * u * d;
    }

    // ---- the five schematics
    private void ring(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f) {
        float sq = 0.55F;
        fx.band(cx, cy, R - 2, R + 2, sq, 0, (float) (Math.PI * 2), 64, col, 0.25F, col, 0.55F);
        fx.band(cx, cy, R - 5, R - 4, sq, 0, (float) (Math.PI * 2), 64, col, 0.2F, col, 0.2F);
        // magnets around the ring
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            float mx = cx + (float) Math.cos(a) * R, my = cy + (float) Math.sin(a) * R * sq;
            fx.rect(mx - 1.5F, my - 2.5F, mx + 1.5F, my + 2.5F, col, 0.55F);
        }
        // two bunches going opposite ways, meeting at the interaction point (top)
        float sp = 0.06F;
        for (int b = 0; b < 2; b++) {
            float dir = b == 0 ? 1 : -1;
            for (int k = 0; k < 7; k++) {
                double a = -Math.PI / 2 + dir * (phase * sp - k * 0.09);
                float px = cx + (float) Math.cos(a) * R, py = cy + (float) Math.sin(a) * R * sq;
                fx.disc(px, py, k == 0 ? 3F : 2F, 8, k == 0 ? 0xFFFFFF : hot, (run ? 1F : 0.35F) * (1 - k / 7F), col, 0F);
            }
        }
        // collision flash when they cross
        double a = (phase * sp) % Math.PI;
        float near = (float) Math.min(a, Math.PI - a);
        float ipx = cx, ipy = cy - R * sq;
        fx.polygon(ipx, ipy, 6, 4, (float) (Math.PI / 4), 1F, col, 0.6F);
        if (run && near < 0.25F) {
            float s = 1 - near / 0.25F;
            fx.disc(ipx, ipy, 10 * s + 3, 16, 0xFFFFFF, 0.9F * s, hot, 0F);
            for (int k = 0; k < 8; k++) {
                double t = k * Math.PI / 4 + phase * 0.3;
                fx.glowLine(ipx, ipy, ipx + (float) Math.cos(t) * 14 * s, ipy + (float) Math.sin(t) * 14 * s, 1F, hot, 0.6F * s);
            }
        }
        // detector layers round the interaction point
        fx.band(ipx, ipy, 8, 9, 1, 0, (float) (Math.PI * 2), 24, col, 0.3F, col, 0.3F);
        // the beam pipe feeding in
        fx.line(cx - R, cy, cx - R - 6, cy, 2F, col, 0.4F);
    }

    private void well(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f) {
        boolean white = be.type.effect == MachineType.Effect.WHITE_HOLE;
        float sq = 0.32F, rh = R * 0.3F;
        // accretion disk: back half, horizon, front half
        diskHalf(fx, cx, cy, rh * 1.3F, R, sq, (float) Math.PI, col, hot, white);
        if (white) fx.disc(cx, cy, rh * 1.6F, 32, 0xFFFFFF, 1F, hot, 0F);
        else {
            fx.band(cx, cy, rh, rh * 1.18F, 1, 0, (float) (Math.PI * 2), 40, hot, 0.9F, col, 0F);   // photon ring
        }
        diskHalf(fx, cx, cy, rh * 1.3F, R, sq, 0, col, hot, white);
        // lensed image of the far disk, arching over the hole
        if (!white) fx.band(cx, cy, rh * 1.25F, rh * 1.5F, 0.9F, (float) Math.PI, (float) Math.PI, 24, hot, 0.35F, col, 0F);
        // infalling (or, for a white hole, outflowing) matter
        int n = 22;
        for (int i = 0; i < n; i++) {
            float u = ((phase * 0.012F + i / (float) n) % 1F);
            float rr = white ? rh + (R - rh) * u : R - (R - rh) * u;
            double a = i * 2.39996 + (white ? 1 : -1) * phase * 0.05 * (R / Math.max(rr, 1));
            float px = cx + (float) Math.cos(a) * rr, py = cy + (float) Math.sin(a) * rr * sq;
            fx.disc(px, py, 1.6F, 6, 0xFFFFFF, (run ? 0.9F : 0.3F) * (white ? 1 - u : u), hot, 0F);
        }
        // jets along the axis
        if (run) {
            float j = R * (0.6F + 0.3F * (float) Math.sin(phase * 0.2));
            fx.line(cx, cy - rh, cx, cy - rh - j, 3F, hot, 0.5F, col, 0F);
            fx.line(cx, cy + rh, cx, cy + rh + j * 0.6F, 3F, hot, 0.35F, col, 0F);
        }
    }

    private void diskHalf(Fx2D fx, float cx, float cy, float r0, float r1, float sq, float a0, int col, int hot, boolean white) {
        int bands = 5;
        for (int b = 0; b < bands; b++) {
            float ra = r0 + (r1 - r0) * b / bands, rb = r0 + (r1 - r0) * (b + 1) / bands;
            int c = Fx2D.lerpRgb(white ? 0xFFFFFF : 0xFFF0C0, col, b / (float) bands);
            fx.band(cx, cy, ra, rb, sq, a0, (float) Math.PI, 32, c, 0.55F - b * 0.08F, c, 0.45F - b * 0.08F);
        }
        // a few bright clumps orbiting
        for (int k = 0; k < 4; k++) {
            float rr = r0 + (r1 - r0) * (0.2F + 0.18F * k);
            double a = a0 + ((phase * 0.04 * (r1 / rr) + k * 1.7) % Math.PI);
            fx.band(cx, cy, rr - 1, rr + 1, sq, (float) a, 0.5F, 6, 0xFFFFFF, 0.6F, hot, 0.2F);
        }
    }

    private void hyper(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f) {
        // a tesseract turning in two planes at once, projected to the screen
        float[][] p = new float[16][2];
        float[] depth = new float[16];
        double a = phase * 0.02, b = phase * 0.013;
        for (int i = 0; i < 16; i++) {
            double x = (i & 1) == 0 ? -1 : 1, y = (i & 2) == 0 ? -1 : 1, z = (i & 4) == 0 ? -1 : 1, w = (i & 8) == 0 ? -1 : 1;
            double x1 = x * Math.cos(a) - w * Math.sin(a), w1 = x * Math.sin(a) + w * Math.cos(a);
            double y1 = y * Math.cos(b) - z * Math.sin(b), z1 = y * Math.sin(b) + z * Math.cos(b);
            double pw = 1.0 / (2.6 - w1);                // 4D -> 3D
            double X = x1 * pw, Y = y1 * pw, Z = z1 * pw;
            double c = Math.cos(0.5), s = Math.sin(0.5);
            double X2 = X * c - Z * s, Z2 = X * s + Z * c;
            double pz = 1.0 / (2.2 - Z2);                // 3D -> 2D
            p[i][0] = cx + (float) (X2 * pz * R * 1.5);
            p[i][1] = cy + (float) (Y * pz * R * 1.5);
            depth[i] = (float) w1;
        }
        float pulse = run ? 0.75F + 0.25F * (float) Math.sin(phase * 0.15) : 0.4F;
        for (int i = 0; i < 16; i++)
            for (int k = 0; k < 4; k++) {
                int j = i ^ (1 << k);
                if (j < i) continue;
                float d = (depth[i] + depth[j] + 2) / 4F;
                fx.glowLine(p[i][0], p[i][1], p[j][0], p[j][1], 1F, Fx2D.lerpRgb(col, hot, d), pulse * (0.35F + 0.5F * d));
            }
        for (int i = 0; i < 16; i++) fx.disc(p[i][0], p[i][1], 1.8F, 6, 0xFFFFFF, pulse * 0.8F, hot, 0F);
        // a containment frame, filling with the cycle
        fx.polygon(cx, cy, R * 1.05F, 8, (float) (Math.PI / 8), 1F, col, 0.35F);
        if (run && f > 0) fx.band(cx, cy, R * 1.08F, R * 1.13F, 1, (float) (-Math.PI / 2), (float) (Math.PI * 2 * f), 48, hot, 0.7F, col, 0.5F);
    }

    private void star(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f) {
        float core = R * 0.42F;
        float fl = run ? 0.85F + 0.15F * (float) Math.sin(phase * 0.6) : 0.4F;
        fx.disc(cx, cy, core * 2.1F, 40, hot, 0.25F * fl, col, 0F);
        fx.disc(cx, cy, core, 40, 0xFFFFFF, fl, hot, 0.6F * fl);
        // magnetic loops / field lines turning slowly
        for (int k = 0; k < 6; k++) {
            float rot = (float) (phase * 0.01 + k * Math.PI / 6);
            loop(fx, cx, cy, core * 1.15F, R * (0.85F + 0.05F * (k % 3)), rot, col, 0.25F + 0.1F * fl);
        }
        boolean shells = be.type.effect == MachineType.Effect.DYSON || be.type.effect == MachineType.Effect.MATRIOSHKA;
        if (shells) {
            int layers = be.type.effect == MachineType.Effect.MATRIOSHKA ? 3 : 1;
            for (int l = 0; l < layers; l++) {
                float rr = R * (0.78F + 0.12F * l);
                int seg = 12 + 6 * l;
                double off = phase * 0.008 * (l % 2 == 0 ? 1 : -1);
                for (int s = 0; s < seg; s++) {
                    if ((s * 7 + l) % 5 == 0) continue;           // gaps between the collectors
                    double a0 = off + Math.PI * 2 * s / seg;
                    fx.band(cx, cy, rr, rr + 2.5F, 1, (float) a0, (float) (Math.PI * 2 / seg * 0.8), 3, col, 0.65F, hot, 0.45F);
                }
            }
        } else {
            // a tokamak's torus around it
            fx.band(cx, cy, R * 0.9F, R, 0.35F, 0, (float) (Math.PI * 2), 48, col, 0.5F, col, 0.15F);
        }
        // prominences
        if (run) for (int k = 0; k < 3; k++) {
            double a = phase * 0.03 + k * 2.1;
            float h = core * (0.3F + 0.3F * (float) Math.abs(Math.sin(phase * 0.1 + k)));
            fx.band(cx + (float) Math.cos(a) * core, cy + (float) Math.sin(a) * core, 1, h, 1, (float) a - 1.2F, 2.4F, 10, hot, 0.5F, col, 0F);
        }
    }

    private static void loop(Fx2D fx, float cx, float cy, float r0, float r1, float rot, int col, float a) {
        float px = 0, py = 0;
        for (int s = 0; s <= 20; s++) {
            double t = Math.PI * s / 20;
            float r = r0 + (r1 - r0) * (float) Math.sin(t);
            double ang = rot + (t - Math.PI / 2) * 0.35;
            float qx = cx + (float) Math.cos(ang) * r, qy = cy + (float) Math.sin(ang) * r * 0.6F;
            if (s > 0) fx.line(px, py, qx, qy, 1F, col, a);
            px = qx;
            py = qy;
        }
    }

    private void vessel(Fx2D fx, float cx, float cy, float R, int col, int hot, boolean run, float f) {
        boolean column = be.type.effect == MachineType.Effect.FRACTIONATION;
        float hw = column ? R * 0.35F : R * 0.7F, hh = R * 0.95F;
        float x0 = cx - hw, x1 = cx + hw, y0 = cy - hh, y1 = cy + hh;
        // the vessel walls
        fx.line(x0, y0, x0, y1, 2F, col, 0.7F);
        fx.line(x1, y0, x1, y1, 2F, col, 0.7F);
        fx.line(x0, y1, x1, y1, 2F, col, 0.7F);
        fx.line(x0 - 3, y0, x1 + 3, y0, 1F, col, 0.5F);
        // contents: the level rises with the cycle, sloshing a little
        float level = y1 - 2 - (hh * 2 - 6) * (run ? 0.25F + 0.65F * f : 0.2F);
        for (int s = 0; s < 12; s++) {
            float ux0 = x0 + 1 + (x1 - x0 - 2) * s / 12F, ux1 = x0 + 1 + (x1 - x0 - 2) * (s + 1) / 12F;
            float w0 = (float) Math.sin(phase * 0.15 + s * 0.6) * 1.2F, w1 = (float) Math.sin(phase * 0.15 + (s + 1) * 0.6) * 1.2F;
            fx.quad(ux0, level + w0, hot, 0.45F, ux0, y1 - 1, col, 0.3F, ux1, y1 - 1, col, 0.3F, ux1, level + w1, hot, 0.45F);
        }
        if (column) {
            // trays, with coloured fractions between them
            int trays = 6;
            for (int t = 1; t < trays; t++) {
                float ty = y0 + (y1 - y0) * t / trays;
                fx.line(x0 + 1, ty, x1 - 1 - (t % 2) * 4, ty, 1F, col, 0.6F);
                fx.rect(x1 + 2, ty - 1, x1 + 7, ty + 1, Fx2D.lerpRgb(col, 0xFFFFFF, t / (float) trays), 0.6F);
            }
        } else {
            // a stirrer / emitter rod coming down from the lid
            float rodY = y0 + 4 + (float) (Math.sin(phase * 0.1) * 0.5 + 0.5) * (level - y0 - 10);
            fx.line(cx, y0, cx, rodY, 2F, col, 0.6F);
            float sw = hw * 0.6F * (float) Math.cos(phase * 0.25);
            fx.line(cx - sw, rodY, cx + sw, rodY, 2F, hot, 0.8F);
        }
        // bubbles rising through the contents
        if (run) for (int i = 0; i < 10; i++) {
            float u = (phase * 0.02F + i * 0.1F) % 1F;
            float bx = x0 + 3 + ((i * 37) % 100) / 100F * (x1 - x0 - 6) + (float) Math.sin(phase * 0.2 + i) * 1.5F;
            float by = y1 - 3 - u * (y1 - 3 - level);
            fx.ring(bx, by, 1.4F, 0.8F, 8, 0xFFFFFF, 0.6F * (1 - u));
        }
    }

    // ---- the oscilloscope
    private void scope(GuiGraphics g, Font font, int x, int y, int w, int h, int mx, int my, List<Component> tip) {
        PhysicsState ps = phys();
        PhysicsModel m = model();
        RecipePhysics.Window win = window();
        String label;
        double lo, hi;
        boolean log = false;
        if (win != null && m != null) {
            label = pname(kindOf(m, win.q()), win.q());
            double a = win.min() > -1e300 ? win.min() : Double.NaN, b = win.max() < 1e300 ? win.max() : Double.NaN;
            double cur = trace[(head + SAMPLES - 1) % SAMPLES];
            if (!Double.isNaN(a) && !Double.isNaN(b) && a > 0 && b / a > 100) log = true;
            if (log) {
                lo = a / 10;
                hi = b * 10;
            } else if (!Double.isNaN(a) && !Double.isNaN(b)) {
                double sp = Math.max(1e-12, b - a);
                lo = a - sp * 0.75;
                hi = b + sp * 0.75;
            } else {
                double bound = Double.isNaN(a) ? b : a;
                double sp = Math.max(Math.abs(bound) * 0.5, Math.abs(cur - bound) * 1.5 + 1e-9);
                lo = bound - sp;
                hi = bound + sp;
            }
        } else if (ps != null) {
            label = "rate";
            lo = 0;
            hi = 2;
        } else {
            label = "cycle";
            lo = 0;
            hi = 1;
        }
        final double L0 = lo, H0 = hi;
        final boolean LOG = log;
        g.drawManaged(() -> {
            g.fill(x, y, x + w, y + h, 0xC0020406);
            HoloStyle.outline(g, x, y, w, h, HoloStyle.argb(0x40, accent));
            for (int k = 1; k < 4; k++) g.fill(x + 1, y + h * k / 4, x + w - 1, y + h * k / 4 + 1, HoloStyle.argb(0x10, accent));
            for (int k = 1; k < 8; k++) g.fill(x + w * k / 8, y + 1, x + w * k / 8 + 1, y + h - 1, HoloStyle.argb(0x10, accent));
            if (win != null) {           // the window: where the trace has to stay
                int wy0 = win.max() < 1e300 ? yOf(win.max(), L0, H0, LOG, y, h) : y + 1;
                int wy1 = win.min() > -1e300 ? yOf(win.min(), L0, H0, LOG, y, h) : y + h - 1;
                g.fill(x + 1, Math.max(y + 1, wy0), x + w - 1, Math.min(y + h - 1, wy1), 0x2840FF90);
                if (wy0 > y && wy0 < y + h) g.fill(x + 1, wy0, x + w - 1, wy0 + 1, 0x8040FF90);
                if (wy1 > y && wy1 < y + h) g.fill(x + 1, wy1, x + w - 1, wy1 + 1, 0x8040FF90);
            }
        });
        if (filled > 1) {
            Fx2D fx = Fx2D.begin(g, Fx2D.Blend.ADD);
            float step = (w - 2) / (float) (SAMPLES - 1);
            float px = 0, py = 0, ex = 0, ey = 0;
            for (int i = 0; i < filled; i++) {
                int idx = (head - filled + i + SAMPLES) % SAMPLES;
                double v = trace[idx];
                float qx = x + 1 + (SAMPLES - filled + i) * step;
                float qy = Mth.clamp(yOf(v, L0, H0, LOG, y, h), y + 1, y + h - 2);
                float rx = qx, ry = y + h - 2 - power[idx] * (h - 4);
                if (i > 0) {
                    boolean ok = win == null || win.test(v);
                    fx.line(ex, ey, rx, ry, 1F, 0xFFC040, 0.25F);
                    fx.glowLine(px, py, qx, qy, 1F, ok ? (win == null ? HoloStyle.bright(accent) : 0x60FF90) : 0xFF5040, 0.9F);
                }
                px = qx;
                py = qy;
                ex = rx;
                ey = ry;
            }
            fx.disc(px, py, 2.5F, 8, 0xFFFFFF, 0.9F, accent, 0F);
            fx.end();
        }
        double cur = trace[(head + SAMPLES - 1) % SAMPLES];
        String v = label.equals("cycle") ? Math.round(cur * 100) + "%" : PhysicsPanel.fmt(cur) + (m != null && win != null ? " " + unitOf(m, win.q()) : "");
        HoloStyle.miniText(g, font, cut(font, label, (int) ((w / 2) / 0.75F)), x + 3, y + 2, 0xA0B0C0, 0.75F, false);
        HoloStyle.miniText(g, font, v, x + w - 3 - (int) (font.width(v) * 0.75F), y + 2, 0xFFFFFF, 0.75F, false);
        if (mx >= x && my >= y && mx < x + w && my < y + h) {
            tip.add(Component.translatable("gui.theoreticalsciences.console.scope", label).withStyle(net.minecraft.ChatFormatting.AQUA));
            tip.add(Component.translatable(win != null ? "gui.theoreticalsciences.console.scope_window" : "gui.theoreticalsciences.console.scope_plain").withStyle(net.minecraft.ChatFormatting.GRAY));
            tip.add(Component.translatable("gui.theoreticalsciences.console.scope_power").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    private static String unitOf(PhysicsModel m, String id) {
        for (PhysicsModel.Param p : m.params) if (p.id().equals(id)) return p.unit();
        for (PhysicsModel.Named d : m.derived) if (d.id().equals(id)) return d.unit();
        for (PhysicsModel.State s : m.states) if (s.id().equals(id)) return s.unit();
        return "";
    }

    private static int yOf(double v, double lo, double hi, boolean log, int y, int h) {
        double t;
        if (!Double.isFinite(v)) return y + h / 2;
        if (log) t = (Math.log10(Math.max(v, lo)) - Math.log10(lo)) / Math.max(1e-9, Math.log10(hi) - Math.log10(lo));
        else t = (v - lo) / Math.max(1e-300, hi - lo);
        return y + h - 2 - (int) Math.round(Mth.clamp(t, -0.05, 1.05) * (h - 4));
    }

    // ------------------------------------------------------------------ the bottom strip: log and listing
    void bottom(GuiGraphics g, Font font, int x, int y, int w, int h) {
        int logW = w * 45 / 100;
        frame(g, font, x, y, logW - 3, h, "TERMINAL");
        frame(g, font, x + logW + 3, y, w - logW - 3, h, "PROGRAM");
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        int rows = Math.max(1, (h - 8) / 9);
        int from = Math.max(0, log.size() - rows);
        int ly = y + h - 4 - (log.size() - from) * 9;
        for (int i = from; i < log.size(); i++) {
            boolean last = i == log.size() - 1;
            String s = log.get(i);
            int c = s.contains("!!") || s.contains("lost") ? 0xFF6050 : last ? HoloStyle.bright(accent) : HoloStyle.mix(0x7A8494, accent, 0.25F);
            g.drawString(font, cut(font, "> " + s, logW - 14) + (last && blink ? "_" : ""), x + 5, ly, c, false);
            ly += 9;
        }
        code.frame(this::idle, running() ? 1.0 : 2.5);
        code.render(g, font, x + logW + 6, y + 4, w - logW - 12, h - 7);
    }

    /** The glass: faint scanlines, a slow sweep and darker edges, in the machine's colour. */
    void crt(GuiGraphics g, int L, int T, int W, int H) {
        g.drawManaged(() -> {
            for (int sy = T + 1; sy < T + H - 1; sy += 2) g.fill(L + 1, sy, L + W - 1, sy + 1, 0x0C000000);
            int sweep = (int) ((System.currentTimeMillis() / 14) % (H + 40)) - 20;
            if (sweep > 0 && sweep < H) g.fill(L + 1, T + sweep, L + W - 1, T + Math.min(H - 1, sweep + 3), HoloStyle.argb(0x0A, accent));
        });
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.NORMAL);
        int e = Math.min(24, Math.min(W, H) / 10);
        f.rectH(L, T, L + e, T + H, 0, 0.3F, 0, 0);
        f.rectH(L + W - e, T, L + W, T + H, 0, 0, 0, 0.3F);
        f.rectV(L, T, L + W, T + e, 0, 0.25F, 0, 0);
        f.rectV(L, T + H - e, L + W, T + H, 0, 0, 0, 0.25F);
        f.end();
    }

    static String cut(Font font, String s, int w) {
        if (w <= 0) return "";
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, Math.max(0, w - font.width("…"))) + "…";
    }
}
