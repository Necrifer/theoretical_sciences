package com.theoreticalsciences.client.transit;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * The loading page shown while the game loads one of our dimensions (or loads the world you return to from one).
 *
 * <p>It is the vanilla loading screen underneath - so it closes exactly when vanilla's would (when the new world is
 * ready, or after 30 seconds at most) - but instead of the plain dirt/portal background it draws the dimension's own
 * scene at full strength, its name ("Entering The Bulk"), a one-line flavour text, and a small animated progress
 * element in the same style.
 */
public class TransitLoadingScreen extends ReceivingLevelScreen {
    private final TransitStyle style;
    private final boolean leaving;
    private final long opened = System.currentTimeMillis();

    public TransitLoadingScreen(BooleanSupplier levelReceived, Reason reason, TransitStyle style, boolean leaving) {
        super(levelReceived, reason);
        this.style = style;
        this.leaving = leaving;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // our scene is the background (drawn in render)
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = TransitClient.seconds();
        TransitScenes.draw(g, style, width, height, t, 1, leaving);

        // a soft band behind the text so it stays readable over the busy scene
        float ty = height * 0.68F;
        int band = style.bright ? 0xFFFFFF : 0x000000;
        Fx2D f = Fx2D.begin(g, Fx2D.Blend.NORMAL);
        f.rectV(0, ty - 34, width, ty, band, 0, band, 0.5F);
        f.rectV(0, ty, width, ty + 60, band, 0.5F, band, 0);
        f.end();

        Component name = Component.translatable(style.key() + ".name");
        Component title = Component.translatable(leaving ? "transit.theoreticalsciences.leaving" : "transit.theoreticalsciences.entering", name);
        Component sub = Component.translatable(style.key() + (leaving ? ".flavour_leaving" : ".flavour"));
        boolean shadow = !style.bright;
        g.pose().pushPose();
        g.pose().translate(width / 2F, ty - 14, 0);
        g.pose().scale(2, 2, 1);
        g.drawString(font, title, -font.width(title) / 2, -4, 0xFF000000 | style.title, shadow);
        g.pose().popPose();
        g.drawString(font, sub, (width - font.width(sub)) / 2, (int) ty + 6, 0xFF000000 | style.subtitle, shadow);

        // the game does not report how far loading is, so the bar creeps towards the end while the page is up
        float done = 1 - (float) Math.exp(-(System.currentTimeMillis() - opened) / 1500.0);
        TransitScenes.progress(g, style, width / 2F, ty + 32, t, done);
    }

    @Override
    public void removed() {
        super.removed();
        TransitClient.arrived(style, leaving);   // fade the scene out over the new world
    }
}
