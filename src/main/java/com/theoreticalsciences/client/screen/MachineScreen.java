package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.machine.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

/** Holographic machine interface, drawn procedurally so every machine gets the same sci-fi look in its own colour. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final MachineLayout layout;
    private final int accent;
    /** {@link HoloStyle#CLASSIC}, {@link HoloStyle#LATE} or {@link HoloStyle#ENDGAME} (see {@link HoloStyle#tier}). */
    private final int style;
    /** Every machine now gets the holographic look (the early ones a calmer version of it). */
    private final boolean holo = true;

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.layout = menu.layout;
        this.imageWidth = layout.width;
        this.imageHeight = layout.height;
        this.inventoryLabelX = layout.invX;
        this.inventoryLabelY = layout.invY - 11;
        this.accent = menu.be.type.color;
        this.style = HoloStyle.tier(menu.be.type);
    }

    private int a(int alpha) {
        return (alpha << 24) | accent;
    }

    // ------------------------------------------------------------------ tuning panel (multiblocks only)
    private static final int PW = 124, PH = 158;

    private com.theoreticalsciences.machine.tuning.TuningProfile profile() {
        return menu.be instanceof ControllerBlockEntity c ? c.tuningProfile() : null;
    }

    private int px() {
        return console ? rightX : leftPos + imageWidth + 4;
    }

    /** The physics console (multiblocks with a physics model); replaces the old tuning dial. */
    private PhysicsPanel phys;

    // ------------------------------------------------------------------ the operator console (late game)
    /** True when the late-game console fills the window (it needs about 480 x 262 GUI pixels; smaller, the plain layout). */
    private boolean console;
    private MachineConsole con;
    /** monitor, right column (physics / tuning), simulation column (or -1 = shares the right column), bottom strip */
    private int ML, MT, MW, MH, rightX, rightW, simX = -1, simY, simW, simH, botX, botY, botW, botH;
    /** in a narrow console the right column switches between the physics controls and the simulation */
    private static boolean showSim;
    private final List<net.minecraft.client.gui.components.AbstractWidget> rightWidgets = new ArrayList<>();
    private HoloButton viewPhys, viewSim;

    private int rightColumnW() {
        if (menu.be instanceof ControllerBlockEntity cb && cb.hasPhysics()) return PhysicsPanel.W;
        return profile() != null ? PW : 0;
    }

    /** Works out the console's layout; false when the window is too small for it. */
    private boolean layoutConsole() {
        if (style < HoloStyle.LATE || consoleBroken) return false;
        int rw = rightColumnW();
        int head = 19, minBottom = 34;
        int availW = width - 8, availH = height - 8;
        int needW = 4 + imageWidth + 3 + (rw > 0 ? rw + 3 : 120 + 3) + 1;
        int contentH = imageHeight;
        if (needW > availW || head + contentH + 6 + minBottom + 4 > availH) return false;
        int simPref = rw > 0 ? 230 : 0;
        MW = Math.min(availW, Math.max(needW, 4 + imageWidth + 3 + simPref + (rw > 0 ? 3 + rw : 230) + 4));
        MH = Math.min(availH, head + contentH + 6 + 110 + 4);
        ML = (width - MW) / 2;
        MT = (height - MH) / 2;
        leftPos = ML + 4;
        topPos = MT + head;
        int ioR = leftPos + imageWidth;
        if (rw > 0) {
            rightW = rw;
            rightX = ML + MW - 4 - rw;
            int free = rightX - 3 - (ioR + 3);
            if (free >= 120) {
                simX = ioR + 3;
                simW = free;
            } else simX = -1;
        } else {
            rightW = 0;
            rightX = ML + MW - 4;
            simX = ioR + 3;
            simW = ML + MW - 4 - simX;
        }
        simY = topPos + 4;
        simH = imageHeight - 4;
        botX = ML + 4;
        botY = topPos + imageHeight + 8;
        botW = MW - 8;
        botH = MT + MH - 4 - botY;
        return true;
    }

    /** The physics console is on screen (not swapped for the simulation). */
    private boolean physLive() {
        return phys != null && !(simInRight() && showSim);
    }

    private boolean simInRight() {
        return console && simX < 0;
    }

    /** Shows the physics/tuning widgets or the simulation in the right column. */
    private void applyView() {
        boolean sim = simInRight() && showSim;
        for (var w : rightWidgets) w.visible = !sim;
        if (phys != null) {
            if (sim) phys.hide();
            else {
                phys.showTabs();
                phys.refresh();
            }
        }
        if (viewPhys != null) {
            viewPhys.selected(!sim);
            viewSim.selected(sim);
        }
    }

    private <T extends net.minecraft.client.gui.components.AbstractWidget> T addRight(T w) {
        rightWidgets.add(w);
        return addRenderableWidget(w);
    }

    @Override
    protected void init() {
        super.init();
        phys = null;
        omegaBtn = null;
        viewPhys = viewSim = null;
        rightWidgets.clear();
        console = layoutConsole();
        if (console && con == null) con = new MachineConsole(menu.be, accent);
        if (simInRight()) {
            int bw = 62, bx = ML + MW - 20 - 2 * bw - 2;
            viewPhys = addRenderableWidget(HoloButton.of(bx, MT + 3, bw, 11, Component.translatable("gui.theoreticalsciences.console.view_physics"), accent, () -> {
                showSim = false;
                applyView();
            }).flat(true));
            viewSim = addRenderableWidget(HoloButton.of(bx + bw + 2, MT + 3, bw, 11, Component.translatable("gui.theoreticalsciences.console.view_sim"), accent, () -> {
                showSim = true;
                applyView();
            }).flat(true));
        }
        if (menu.be instanceof ControllerBlockEntity cb && cb.hasPhysics()) {
            if (!console) leftPos -= (PhysicsPanel.W + 4) / 2;
            phys = new PhysicsPanel(cb, font, accent);
            phys.init(px(), topPos, this::addRenderableWidget);
            if (omegaOffered()) {
                omegaBtn = addRight(HoloButton.of(px() + PhysicsPanel.W - 86, topPos + 3, 16, 11, Component.literal("\u03A9").withStyle(ChatFormatting.DARK_RED),
                        0xFF3050, () -> send(3, 0)).tipKey("gui.theoreticalsciences.omega_tip"));
                addRight(HoloButton.of(px() + 4, topPos + PhysicsPanel.H + 4, PhysicsPanel.W - 8, 13, Component.translatable("gui.theoreticalsciences.collapse_review"),
                        0xFF3050, () -> com.theoreticalsciences.client.ClientHooks.confirmCollapse(menu.be.getBlockPos())));
            }
            applyView();
            return;
        }
        if (profile() == null) {
            applyView();
            return;
        }
        if (!console) leftPos -= (PW + 4) / 2;                        // keep the machine + panel centred
        int x = px(), y = topPos;
        // fine and coarse tuning of the setting
        addRight(btn(x + 4, y + 62, 22, "<<", () -> send(0, -50)));
        addRight(btn(x + 27, y + 62, 18, "<", () -> send(0, -5)));
        addRight(btn(x + PW - 45, y + 62, 18, ">", () -> send(0, 5)));
        addRight(btn(x + PW - 26, y + 62, 22, ">>", () -> send(0, 50)));
        addRight(btn(x + PW - 34, y + 110, 14, "-", () -> send(1, -1)));
        addRight(btn(x + PW - 18, y + 110, 14, "+", () -> send(1, 1)));
        addRight(btn(x + PW - 34, y + 126, 14, "-", () -> send(2, -1)));
        addRight(btn(x + PW - 18, y + 126, 14, "+", () -> send(2, 1)));
        // The hidden overclock: only offered to someone carrying the Omnilex itself.
        if (omegaOffered()) {
            omegaBtn = addRight(HoloButton.of(x + PW - 52, y + 110, 16, 13, Component.literal("\u03A9").withStyle(ChatFormatting.DARK_RED),
                    0xFF3050, () -> send(3, 0)).tipKey("gui.theoreticalsciences.omega_tip"));
            addRight(HoloButton.of(x + 4, y + PH + 16, PW - 8, 13, Component.translatable("gui.theoreticalsciences.collapse_review"),
                    0xFF3050, () -> com.theoreticalsciences.client.ClientHooks.confirmCollapse(menu.be.getBlockPos())));
        }
        applyView();
    }

    /** Disabled for good after any error: logged once, and the machine falls back to the plain layout (never a crash). */
    private static boolean consoleBroken;

    private void consoleFailed(RuntimeException e) {
        if (!consoleBroken) com.theoreticalsciences.TheoreticalSciences.LOGGER.error("Machine console failed; using the plain layout from now on", e);
        consoleBroken = true;
        con = null;
        if (console && minecraft != null) minecraft.execute(() -> init(minecraft, width, height));
        console = false;
    }

    /** Screen areas the recipe viewers (JEI / EMI) must keep clear of. */
    public List<net.minecraft.client.renderer.Rect2i> extraAreas() {
        List<net.minecraft.client.renderer.Rect2i> out = new ArrayList<>();
        if (console) out.add(new net.minecraft.client.renderer.Rect2i(ML, MT, MW, MH));
        else {
            int rw = rightColumnW();
            if (rw > 0) out.add(new net.minecraft.client.renderer.Rect2i(px(), topPos - 2, rw, imageHeight + 20));
        }
        return out;
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        if (console) return mx < ML || my < MT || mx >= ML + MW || my >= MT + MH;
        int rw = rightColumnW();
        if (rw > 0 && mx >= px() && mx < px() + rw && my >= topPos && my < topPos + imageHeight + 20) return false;
        return super.hasClickedOutside(mx, my, left, top, button);
    }

    private HoloButton omegaBtn;
    private Boolean omegaShown;

    /**
     * The hidden overclock is offered on the Omnilex Engine to whoever carries the Omnilex - or has studied Beyond the
     * Omega (round 23: then it shows, greyed out until the Omnilex is in the inventory, so it is never a mystery).
     */
    private boolean omegaOffered() {
        if (menu.be.type != MachineType.OMNILEX_ENGINE || minecraft == null || minecraft.player == null) return false;
        return ControllerBlockEntity.carriesOmnilex(minecraft.player)
                || com.theoreticalsciences.client.ClientResearch.unlocked.contains(com.theoreticalsciences.TheoreticalSciences.id("beyond_the_omega"));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (con != null) {
            try {
                con.tick();
            } catch (RuntimeException e) {
                consoleFailed(e);
            }
        }
        if (omegaBtn != null && minecraft != null && minecraft.player != null) {
            boolean has = ControllerBlockEntity.carriesOmnilex(minecraft.player);
            // round 35: the button says whether the overclock is set (the physics console never showed it, so after
            // reopening the screen it looked off while it was still on)
            boolean on = menu.be instanceof ControllerBlockEntity cb && cb.tuning.omega;
            if (omegaBtn.active != has || !Boolean.valueOf(on).equals(omegaShown)) {     // (null until first set)
                omegaBtn.active = has;
                omegaShown = on;
                omegaBtn.setMessage(Component.literal("\u03A9").withStyle(on ? new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD} : new ChatFormatting[]{ChatFormatting.DARK_RED}));
                omegaBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(!has ? "gui.theoreticalsciences.omega_needs_omnilex"
                        : on ? "gui.theoreticalsciences.omega_tip_on" : "gui.theoreticalsciences.omega_tip")));
            }
        }
    }

    private HoloButton btn(int x, int y, int w, String label, Runnable action) {
        return HoloButton.of(x, y, w, 13, label, accent, action);
    }

    private void send(int field, int delta) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.TuningPayload(menu.be.getBlockPos(), field, delta));
    }

    private void drawTuning(GuiGraphics g, float time) {
        com.theoreticalsciences.machine.tuning.TuningProfile p = profile();
        if (p == null || !(menu.be instanceof ControllerBlockEntity c)) return;
        com.theoreticalsciences.machine.tuning.TuningState s = c.tuning;
        int x = px(), y = topPos;
        if (holo) {
            g.drawManaged(() -> {
                HoloStyle.panel(g, x, y, PW, PH, 5, accent, style, time, y + 60, y + PH - 2, 0x0E);
                HoloStyle.header(g, x, y, PW, 13, 5, accent, -1, false, time);
                HoloStyle.frame(g, x, y, PW, PH, 5, accent, style, time);
                HoloStyle.brackets(g, x + 4, y + 27, PW - 8, 32, 4, a(0xE0));
            });
        } else {
            g.fill(x, y, x + PW, y + PH, 0xF00A0D14);
            g.renderOutline(x, y, PW, PH, a(0x90));
        }
        fit(g, Component.translatable("gui.theoreticalsciences.tuning"), x + (holo ? 9 : 5), y + 5, PW - 14, holo ? HoloStyle.bright(accent) : accent);
        fit(g, Component.translatable("tuning.theoreticalsciences." + p.param()), x + 5, y + 16, PW - 10, 0xC0C8D8);
        // interference display: the reference wave (machine colour) against your setting (white).
        // When they line up the signal is clean; the further off, the more they drift apart and the noisier it gets.
        int wx = x + 5, wy = y + 28, ww = PW - 10, wh = 30;
        g.fill(wx, wy, wx + ww, wy + wh, 0xFF05070C);
        smooth.frame();
        shownSetting = (float) smooth.toward(Float.isNaN(shownSetting) ? s.setting : shownSetting, s.setting);    // round 28: it glides
        float off = (shownSetting - s.target) / 100F;
        float noise = Math.min(1F, Math.abs(off) * 3F);
        java.util.Random rnd = new java.util.Random((long) (time / 2));
        for (int i = 0; i < ww; i++) {
            double ph = i / (double) ww * Math.PI * 4 + time * 0.15;
            int ry = wy + wh / 2 + (int) (Math.sin(ph) * 10);
            int my2 = wy + wh / 2 + (int) (Math.sin(ph + off * Math.PI * 4) * 10 + (rnd.nextFloat() - 0.5F) * noise * 10);
            g.fill(wx + i, Mth.clamp(ry, wy, wy + wh - 1), wx + i + 1, Mth.clamp(ry, wy, wy + wh - 1) + 1, a(0xC0));
            g.fill(wx + i, Mth.clamp(my2, wy, wy + wh - 1), wx + i + 1, Mth.clamp(my2, wy, wy + wh - 1) + 1, 0xE0FFFFFF);
        }
        if (s.revealTicks > 0) {            // a calibrator was just used: mark the ideal value
            int tx = wx + (int) (s.target / 100F * (ww - 1));
            g.fill(tx, wy, tx + 1, wy + wh, 0xFFFFD040);
        }
        String set = String.format("%.1f", shownSetting);
        g.drawString(font, set, x + PW / 2 - font.width(set) / 2, y + 65, 0xFFFFFF, false);
        float eff = s.efficiency();
        fit(g, Component.translatable("gui.theoreticalsciences.efficiency", Math.round(eff * 100)), x + 5, y + 80, PW - 10,
                eff > 0.85F ? 0x60FF90 : eff > 0.5F ? 0xFFD040 : 0xFF6050);
        // containment integrity bar
        fit(g, Component.translatable("gui.theoreticalsciences.integrity"), x + 5, y + 93, 55, 0xA0A8B8);
        int bx = x + 62, bw = PW - 67;
        int icol = s.integrity > 50 ? 0xFF40C080 : s.integrity > 25 ? 0xFFE0B030 : 0xFFE04040;
        if (holo) {
            g.drawManaged(() -> HoloStyle.hbar(g, bx, y + 93, bw, 8, s.integrity / 100F, icol, accent));
        } else {
            g.fill(bx, y + 93, bx + bw, y + 101, 0xFF05070C);
            int fill = (int) (s.integrity / 100F * bw);
            g.fill(bx, y + 93, bx + fill, y + 101, icol);
            g.renderOutline(bx - 1, y + 92, bw + 2, 10, a(0x70));
        }
        // overclock / parallel
        if (s.omega)
            fit(g, Component.translatable("gui.theoreticalsciences.overclock_omega").withStyle(ChatFormatting.DARK_RED), x + 5, y + 113, PW - 10, 0xFF3050);
        else
            fit(g, Component.translatable("gui.theoreticalsciences.overclock", s.overclock, p.maxOverclock()), x + 5, y + 113, PW - 10, 0xC0C8D8);
        fit(g, Component.translatable("gui.theoreticalsciences.parallel", s.parallel, c.maxParallel()), x + 5, y + 129, PW - 10, 0xC0C8D8);
        String rates = s.omega ? "x\u221E speed  x\u221E FE/t" : String.format("x%d speed  x%d FE/t", 1 << s.overclock, (int) Math.pow(4, s.overclock) * s.parallel);
        fit(g, Component.literal(rates), x + 5, y + 142, PW - 10, 0x8090A0);
        if (s.faulted) {
            g.fill(x + 1, y + 26, x + PW - 1, y + 60, 0xD0300808);
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.fault"), x + PW / 2, y + 30, PW - 8, 0xFF6050, 1, true);
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.fault_hint"), x + PW / 2, y + 40, PW - 8, 0xFFB0A0, 2, true);
        }
        int ty = y + PH + 3;          // under the panel: wrapped to its width
        if (!s.faulted && s.autoCalibrator)
            ty += TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.auto_calibrating"), x + 5, ty, PW - 10, 0x60FFD0, 3, false) * TextFit.LINE + 1;
        if (c.coilTier() > 0 || c.emitterTier() > 0)      // GT-style component tiers of the formed structure
            TextFit.wrapped(g, font, Component.translatable("gui.theoreticalsciences.tiers", c.coilTier(), c.emitterTier()), x + 5, ty, PW - 10, 0xFFC860, 3, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = (minecraft.level != null ? minecraft.level.getGameTime() : 0) + partial;
        holoBg(g, time);
        boolean simHere = simInRight() && showSim;
        if (!simHere) {
            if (phys != null) phys.render(g, mx, my, time);
            else drawTuning(g, time);
        }
        if (console) {
            simTip.clear();
            try {
                if (simX >= 0) con.sim(g, font, simX, simY, simW, simH, mx, my, simTip);
                else if (simHere) con.sim(g, font, rightX, topPos + 4, rightW, imageHeight - 4, mx, my, simTip);
                if (botH >= 24) con.bottom(g, font, botX, botY + 4, botW, botH - 4);
            } catch (RuntimeException e) {
                consoleFailed(e);
            }
        }
        // multiblock / dyson info
        if (menu.be instanceof DysonControllerBlockEntity d) {
            fit(g, Component.translatable("gui.theoreticalsciences.dyson", d.satellites(), DysonControllerBlockEntity.MAX_SATELLITES,
                    d.shells(), DysonControllerBlockEntity.SHELL_SEGMENTS), x + 26, y + MachineLayout.TOP + layout.areaH + 2, imageWidth - 32, 0xFFB0A0);
        }
    }

    // ------------------------------------------------------------------ the holographic console (late / endgame)
    private static final int CH = 7;      // chamfer of the main panel

    private boolean running() {
        MachineStatus st = menu.status();
        return st == MachineStatus.RUNNING || st == MachineStatus.GENERATING;
    }

    private float progressFrac() {
        int max = menu.maxProgress();
        return max > 0 ? Mth.clamp(menu.progress() / (float) max, 0F, 1F) : 0F;
    }

    private void holoBg(GuiGraphics g, float time) {
        int x = leftPos, y = topPos;
        // all the plain fills in one batch (one draw call); fluid sprites and text follow outside it
        g.drawManaged(() -> holoFills(g, time));
        MachineType t = menu.be.type;
        for (int i = 0; i < t.fluidInputs; i++) holoTank(g, x + layout.fluidInX[i], y + layout.fluidInY[i], menu.be.tanks[i]);
        for (int i = 0; i < t.fluidOutputs; i++) holoTank(g, x + layout.fluidOutX[i], y + layout.fluidOutY[i], menu.be.tanks[t.fluidInputs + i]);
        // compact readouts: energy level under the bar, progress under the conduit
        long cap = Math.max(1L, menu.capacity());
        int pct = (int) Math.min(100, menu.energy() * 100.0 / cap);
        HoloStyle.miniText(g, font, pct + "%", x + layout.energyX + 6, y + layout.energyY + layout.areaH + 3,
                pct > 0 ? 0xFFE0A0 : 0x707888, 0.75F, true);
        if (menu.maxProgress() > 0) {
            int pp = Math.round(progressFrac() * 100);
            HoloStyle.miniText(g, font, pp + "%", x + layout.arrowX + 12, y + layout.arrowY + 14,
                    running() ? HoloStyle.bright(accent) : 0x8090A0, 0.75F, true);
        }
    }

    private void holoFills(GuiGraphics g, float time) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        MachineType t = menu.be.type;
        boolean run = running();
        int top = y + MachineLayout.TOP, areaBottom = top + layout.areaH;
        HoloStyle.panel(g, x, y, w, h, CH, accent, style, time, y + 16, y + layout.invY - 14, style >= HoloStyle.ENDGAME ? 0x1A : style >= HoloStyle.LATE ? 0x14 : 0x0C);
        // recessed plate behind the machine area
        g.fill(x + 4, top - 1, x + w - 4, areaBottom + 9, 0x30000000);
        HoloStyle.outline(g, x + 4, top - 1, w - 8, layout.areaH + 10, a(0x1C));
        HoloStyle.header(g, x, y, w, 14, CH, accent, menu.maxProgress() > 0 ? progressFrac() : -1, run, time);
        // status lamp + live data bars between the title and the status
        MachineStatus st = menu.status();
        int sw = font.width(st.label(menu.be.type));
        int lampX = x + w - 8 - sw - 6;
        HoloStyle.lamp(g, lampX, y + 6, st.color, run, time);
        int gap0 = x + 8 + font.width(title) + 6, gap1 = lampX - 5;
        if (gap1 - gap0 >= 14) {
            int ew = Math.min(42, gap1 - gap0);
            HoloStyle.equalizer(g, gap1 - ew, y + 4, ew, 8, accent, run, time);
        }
        HoloStyle.frame(g, x, y, w, h, CH, accent, style, time);
        // divider over the inventory, brighter under its label
        int dy = y + layout.invY - 14;
        g.fill(x + 6, dy, x + w - 6, dy + 1, a(0x40));
        int lx = x + inventoryLabelX - 2, lw = font.width(playerInventoryTitle) + 6;
        g.fill(lx, dy, lx + lw, dy + 1, a(0xD0));
        g.fill(lx + lw, dy, lx + lw + 1, dy + 2, a(0x90));
        g.fill(lx + lw + 1, dy + 1, lx + lw + 2, dy + 3, a(0x60));
        for (int k = 0; k < 3; k++) g.fill(x + w - 14 - k * 4, dy - 1, x + w - 12 - k * 4, dy + 2, a(0x90 - k * 0x28));

        // slots
        for (int i = 0; i < t.itemInputs; i++) HoloStyle.slot(g, x + layout.itemInX[i] - 1, y + layout.itemInY[i] - 1, accent, false, false, run, time);
        for (int i = 0; i < t.itemOutputs; i++) HoloStyle.slot(g, x + layout.itemOutX[i] - 1, y + layout.itemOutY[i] - 1, accent, true, false, run, time);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) HoloStyle.slot(g, x + layout.invX - 1 + c * 18, y + layout.invY - 1 + r * 18, accent, false, true, false, time);
        for (int c = 0; c < 9; c++) HoloStyle.slot(g, x + layout.invX - 1 + c * 18, y + layout.invY + 57, accent, false, true, false, time);

        // energy, tank backs, progress conduit
        long cap = Math.max(1L, menu.capacity());
        HoloStyle.energy(g, x + layout.energyX, y + layout.energyY, 12, layout.areaH, (float) Math.min(1.0, (double) menu.energy() / cap), accent, time);
        for (int i = 0; i < t.tanks(); i++) HoloStyle.tubeBack(g, x + layout.tankX(t, i), y + layout.tankY(t, i), 16, layout.tankH, accent);
        int ax = x + layout.arrowX, ay = y + layout.arrowY;
        HoloStyle.conduit(g, ax, ay, progressFrac(), run, accent, time);

        // tier flourish: the free space above the conduit ...
        int above = layout.arrowY - MachineLayout.TOP;
        if (style >= HoloStyle.ENDGAME) {
            int r = Math.min(9, (above - 4) / 2);
            if (r >= 4) HoloStyle.rings(g, ax + 12, top + above / 2 - 1, r, accent, run, time);
        } else if (above >= 10) {
            HoloStyle.equalizer(g, ax + 1, top + 2, 22, Math.min(12, above - 6), accent, run, time);
        }
        // ... and the margins beside the player inventory on the wide (multiblock) screens
        int side = layout.invX - 1 - 8;
        if (side >= 28) {
            int iy = y + layout.invY - 1, ih = 76;
            HoloStyle.dataColumn(g, x + w - 4 - side, iy, side, ih, accent, run, time);
            if (style >= HoloStyle.ENDGAME) {
                int cx = x + 4 + side / 2, cy = iy + ih / 2;
                HoloStyle.rings(g, cx, cy, Math.min(17, side / 2 - 3), accent, run, time);
                HoloStyle.brackets(g, x + 4, iy, side, ih, 3, a(0xB0));
            } else {
                HoloStyle.dataColumn(g, x + 4, iy, side, ih, accent, run, time + 977);
            }
        }
    }

    /** Glass-tube tank: same log-scale fill as {@link #tank}, drawn between the tube's back and front. */
    private void holoTank(GuiGraphics g, int x, int y, FluidTank tank) {
        int h = layout.tankH;
        int fluidTop = drawFluid(g, x, y, h, tank);
        g.drawManaged(() -> HoloStyle.tubeFront(g, x, y, 16, h, accent, !layout.big, fluidTop));
    }

    /** Draws the tank's fluid (log scale); returns the y of the surface, or -1 when empty. */
    private int drawFluid(GuiGraphics g, int x, int y, int h, FluidTank tank) {
        FluidStack fs = tank.getFluid();
        if (fs.isEmpty()) return -1;
        // tanks are practically bottomless now: the level is drawn on a log scale (1 mB .. 2.1 billion mB)
        int fh = Math.max(1, (int) Math.round(h * Math.log1p(fs.getAmount()) / Math.log1p(tank.getCapacity())));
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs.getFluid());
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
        int tint = ext.getTintColor(fs);
        float r = ((tint >> 16) & 255) / 255F, gg = ((tint >> 8) & 255) / 255F, b = (tint & 255) / 255F;
        int drawn = 0;
        while (drawn < fh) {
            int seg = Math.min(16, fh - drawn);
            g.blit(x, y + h - drawn - seg, 0, 16, seg, sprite, r, gg, b, 1.0F);
            drawn += seg;
        }
        return y + h - fh;
    }

    /** Counts above 999 are written short (12k, 3.4M, 2.1G); hover for the exact number. */
    @Override
    protected void renderSlot(GuiGraphics g, net.minecraft.world.inventory.Slot slot) {
        net.minecraft.world.item.ItemStack s = slot.getItem();
        if (s.getCount() <= 999) {
            super.renderSlot(g, slot);
            return;
        }
        g.renderItem(s, slot.x, slot.y);
        g.renderItemDecorations(font, s.copyWithCount(1), slot.x, slot.y);
        String c = InfinityCacheScreen.compact(s.getCount());
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(font, c, (int) ((slot.x + 17) / 0.75F) - font.width(c), (int) ((slot.y + 11) / 0.75F), 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        MachineStatus st = menu.status();
        Component s = st.label(menu.be.type);
        // the status keeps its place on the right (cut only if it alone is too long); the name fits in what is left
        int sw = Math.min(font.width(s), imageWidth - 16 - 40);
        TextFit.draw(g, font, s, imageWidth - 8 - sw, 6, sw, st.color);
        int tw = imageWidth - 16 - sw - 6;
        if (TextFit.draw(g, font, title, 8, 6, tw, holo ? HoloStyle.bright(accent) : accent) && in(lastMx - leftPos, lastMy - topPos, 8, 5, tw, 10))
            fitTip = List.of(title);
        if (font.width(s) > sw && in(lastMx - leftPos, lastMy - topPos, imageWidth - 8 - sw, 5, sw, 10)) fitTip = List.of(s);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, holo ? HoloStyle.mix(0x8899AA, accent, 0.35F) : 0x8899AA, false);
    }

    private final List<Component> simTip = new ArrayList<>();

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        if (!console || con == null) {
            super.renderBackground(g, mx, my, partial);
            return;
        }
        renderTransparentBackground(g);
        con.monitor(g, font, ML, MT, MW, MH, style, HoloStyle.time(partial), simInRight() ? 2 * 62 + 6 : 0);
        renderBg(g, partial, mx, my);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        lastMx = mx;
        lastMy = my;
        fitTip = null;
        super.render(g, mx, my, partial);
        if (console && con != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);          // over the items, under the tooltips
            con.crt(g, ML, MT, MW, MH);
            g.pose().popPose();
            if (!simTip.isEmpty() && menu.getCarried().isEmpty()) TextFit.tooltip(g, font, simTip, mx, my);
        }
        // round 35: while the aleph-null overclock is set, the Ω button glows and says so beside it
        if (omegaBtn != null && omegaBtn.visible && menu.be instanceof ControllerBlockEntity ocb && ocb.tuning.omega) {
            int bx = omegaBtn.getX(), by = omegaBtn.getY(), bw = omegaBtn.getWidth(), bh = omegaBtn.getHeight();
            int al = 0x90 + (int) (0x6F * (0.5F + 0.5F * net.minecraft.util.Mth.sin((System.currentTimeMillis() % 100000) / 160F)));
            g.renderOutline(bx - 1, by - 1, bw + 2, bh + 2, (al << 24) | 0xFF3050);
            g.renderOutline(bx - 2, by - 2, bw + 4, bh + 4, ((al / 3) << 24) | 0xFF3050);
            Component on = Component.translatable("gui.theoreticalsciences.omega_on");
            g.drawString(font, on, bx - 4 - font.width(on), by + (bh - 8) / 2, 0xFFFF3050, true);
        }
        renderTooltip(g, mx, my);
        if (fitTip != null) TextFit.tooltip(g, font, fitTip, mx, my);
        if (physLive()) phys.renderTooltip(g, mx, my);
        int x = leftPos, y = topPos;
        if (in(mx, my, x + layout.energyX, y + layout.energyY, 12, layout.areaH)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(String.format("%,d / %,d FE", menu.energy(), menu.capacity())).withStyle(ChatFormatting.GOLD));
            if (menu.be.type.generator && menu.burnRate() > 0)
                lines.add(Component.translatable("gui.theoreticalsciences.generating", String.format("%,d", menu.burnRate())).withStyle(ChatFormatting.GREEN));
            TextFit.tooltip(g, font, lines, mx, my);
        }
        for (int i = 0; i < menu.be.tanks.length; i++) {
            if (in(mx, my, x + layout.tankX(menu.be.type, i), y + layout.tankY(menu.be.type, i), 16, layout.tankH)) {
                FluidStack fs = menu.be.tanks[i].getFluid();
                List<Component> lines = new ArrayList<>();
                lines.add(fs.isEmpty() ? Component.translatable("gui.theoreticalsciences.empty") : fs.getHoverName());
                lines.add(Component.literal(String.format("%,d / %,d mB", fs.getAmount(), menu.be.tanks[i].getCapacity())).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(i < menu.be.type.fluidInputs ? "gui.theoreticalsciences.tank_hint_in" : "gui.theoreticalsciences.tank_hint_out")
                        .withStyle(ChatFormatting.DARK_GRAY));
                TextFit.tooltip(g, font, lines, mx, my);
            }
        }
        if (in(mx, my, x + layout.arrowX, y + layout.arrowY, 24, 16) && menu.maxProgress() > 0) {
            g.renderTooltip(font, Component.literal(menu.progress() * 100 / Math.max(1, menu.maxProgress()) + "%"), mx, my);
        }
    }

    /** Clicking a tank with a bucket / cell / any fluid container on the cursor fills or drains it (done on the server). */
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (physLive()) {
            int t = menu.getCarried().isEmpty() ? -1 : phys.tankAt(mx, my);
            if (t >= 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.TankClickPayload(t));
                return true;
            }
            if (phys.mouseClicked(mx, my, button)) return true;
        }
        if (!menu.getCarried().isEmpty()) {
            for (int i = 0; i < menu.be.tanks.length; i++) {
                if (in((int) mx, (int) my, leftPos + layout.tankX(menu.be.type, i), topPos + layout.tankY(menu.be.type, i), 16, layout.tankH)) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.theoreticalsciences.network.TankClickPayload(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (physLive() && phys.mouseDragged(mx, my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (physLive()) phys.mouseReleased();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (physLive() && phys.mouseScrolled(mx, my, sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (physLive() && phys.keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (physLive() && phys.charTyped(c, mods)) return true;
        return super.charTyped(c, mods);
    }

    private int lastMx, lastMy;
    private final Smooth smooth = new Smooth();
    private float shownSetting = Float.NaN;
    @javax.annotation.Nullable
    private List<Component> fitTip;

    /** Draws a line cut to fit; hovering a cut one shows the whole of it. */
    private void fit(GuiGraphics g, Component t, int tx, int ty, int w, int color) {
        if (TextFit.draw(g, font, t, tx, ty, w, color) && in(lastMx, lastMy, tx, ty - 1, w, 10)) fitTip = List.of(t);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @SuppressWarnings("unused")
    private static final net.minecraft.resources.ResourceLocation ATLAS = TextureAtlas.LOCATION_BLOCKS;
}
