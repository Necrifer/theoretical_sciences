package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.dungeon.AnomalyEngineBlockEntity;
import com.theoreticalsciences.dungeon.AnomalyEngineMenu;
import com.theoreticalsciences.dungeon.SpawnTuning;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The built Breached Anomaly Engine: the template (a genome sample) and the serum, four controls and four readings
 * of how close the breach is to the creature's form. All four readings centred: it works.
 */
public class AnomalyEngineScreen extends AbstractContainerScreen<AnomalyEngineMenu> {
    private static final int ACCENT = 0xC060FF;
    private static final int GREEN = 0xFF60FF90, AMBER = 0xFFFFC040, RED = 0xFFFF5040, DIM = 0xFF6A5A80;

    public AnomalyEngineScreen(AnomalyEngineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 194;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        String[] lab = {"«", "‹", "›", "»"};
        for (int i = 0; i < SpawnTuning.N; i++)
            for (int k = 0; k < 4; k++) {
                int id = i * 4 + k;
                int bx = leftPos + 48 + (k < 2 ? k * 12 : 92 + (k - 2) * 12), by = topPos + 18 + i * 20;
                addRenderableWidget(HoloButton.of(bx, by, 11, 12, lab[k], ACCENT, () -> {
                    if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                }));
            }
    }

    private String species() {
        Slot s = menu.slots.get(0);
        return AnomalyEngineBlockEntity.species(s.getItem());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        float time = HoloStyle.time(pt);
        HoloStyle.window(g, x, y, imageWidth, imageHeight, ACCENT, HoloStyle.LATE, time, 14);
        HoloStyle.slots(g, x, y, menu.slots, 2, i -> i == 1, ACCENT, menu.status() == 1, time);
        HoloStyle.divider(g, x, y + inventoryLabelY - 3, imageWidth, x + inventoryLabelX, font.width(playerInventoryTitle), ACCENT);
        String sp = species();
        smooth.frame();
        for (int i = 0; i < SpawnTuning.N; i++) shownKnobs[i] = smooth.toward(shownKnobs[i], menu.knob(i));   // round 28: the bars glide
        double[] res = sp == null ? new double[SpawnTuning.N] : SpawnTuning.residuals(SpawnTuning.problem(sp), shownKnobs);
        for (int i = 0; i < SpawnTuning.N; i++) {
            int by = y + 18 + i * 20;
            int cx = x + 106, half = 34;
            int bcx = cx, bby = by;
            g.drawManaged(() -> HoloStyle.field(g, bcx - half - 2, bby + 1, half * 2 + 4, 10, ACCENT));
            g.fill(cx - 1, by + 1, cx + 1, by + 11, DIM);
            if (sp != null) {
                double v = res[i];
                boolean ok = Math.abs(v) <= SpawnTuning.TOLERANCE;
                int len = (int) Math.round(Mth.clamp(v / 60.0, -1, 1) * half);
                if (len != 0) g.fill(Math.min(cx, cx + len), by + 3, Math.max(cx, cx + len), by + 9, ok ? GREEN : Math.abs(v) < 15 ? AMBER : RED);
                else g.fill(cx - 2, by + 3, cx + 2, by + 9, GREEN);
            }
        }
    }

    private final Smooth smooth = new Smooth();
    private final double[] shownKnobs = new double[SpawnTuning.N];

    private int[] knobs() {
        int[] k = new int[SpawnTuning.N];
        for (int i = 0; i < SpawnTuning.N; i++) k[i] = menu.knob(i);
        return k;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        TextFit.draw(g, font, title, titleLabelX, 5, imageWidth - titleLabelX - 8, 0xFFE0C0FF);
        for (int i = 0; i < SpawnTuning.N; i++) {
            TextFit.draw(g, font, Component.translatable("gui.theoreticalsciences.engine_param." + i), 44, 30 + i * 20, imageWidth - 52, DIM);
            String v = String.format("%+d", Math.round(shownKnobs[i]));
            g.drawString(font, v, 172 - font.width(v), 20 + i * 20, 0xFFE0D0F0, false);
        }
        String sp = species();
        Component who;
        if (sp == null) who = Component.translatable("gui.theoreticalsciences.engine_no_template");
        else {
            var t = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(sp)).orElse(null);
            who = t == null ? Component.literal(sp) : t.getDescription();
        }
        g.drawString(font, font.plainSubstrByWidth(who.getString(), 160), 8, 88, AMBER, false);
        int st = menu.status();
        TextFit.draw(g, font, Component.translatable("gui.theoreticalsciences.engine_status." + st), 8, 98, imageWidth - 16, st == 1 ? GREEN : st == 0 ? DIM : RED);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (mx >= leftPos + 8 && mx < leftPos + 120 && my >= topPos + 96 && my < topPos + 106)
            g.renderTooltip(font, Component.literal(String.format("%,d kFE", menu.energyK())), mx, my);
    }
}
