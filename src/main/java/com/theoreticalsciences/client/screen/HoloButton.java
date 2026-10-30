package com.theoreticalsciences.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The machines' own button: a chamfered glass key in the machine's colour instead of Minecraft's grey stone button.
 * Resting it is a dark plate with a faint frame; under the mouse the frame lights up and a sheen comes over it; pressed,
 * it sinks a pixel and a flash runs across it (so every press is seen to land, even when the server answers later).
 * A key can be "selected" (a tab that is open) - then it stays lit with a bar underneath.
 */
public class HoloButton extends Button {
    private int accent;
    private boolean selected;
    private long pressedAt = -10_000;
    /** Drawn as a console tab: no plate, just the label and an underline. */
    private boolean flat;

    public HoloButton(int x, int y, int w, int h, Component label, int accent, OnPress press) {
        super(x, y, w, h, label, press, DEFAULT_NARRATION);
        this.accent = accent & 0xFFFFFF;
    }

    public static HoloButton of(int x, int y, int w, int h, Component label, int accent, Runnable action) {
        return new HoloButton(x, y, w, h, label, accent, b -> action.run());
    }

    public static HoloButton of(int x, int y, int w, int h, String label, int accent, Runnable action) {
        return of(x, y, w, h, Component.literal(label), accent, action);
    }

    public HoloButton tip(Component c) {
        setTooltip(c == null ? null : Tooltip.create(c));
        return this;
    }

    public HoloButton tipKey(String key) {
        return tip(key == null ? null : Component.translatable(key));
    }

    public HoloButton selected(boolean s) {
        selected = s;
        return this;
    }

    public HoloButton flat(boolean f) {
        flat = f;
        return this;
    }

    public HoloButton accent(int a) {
        accent = a & 0xFFFFFF;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    /** The mod's own console click instead of Minecraft's stone button. */
    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager sm) {
        sm.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(com.theoreticalsciences.registry.QHSounds.UI_CLICK.get(), 1.0F, 0.6F));
    }

    @Override
    public void onPress() {
        pressedAt = Util.getMillis();
        super.onPress();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        int x = getX(), y = getY(), w = width, h = height;
        boolean hover = active && isHovered();
        long since = Util.getMillis() - pressedAt;
        float flash = since < 260 ? 1F - since / 260F : 0F;
        boolean down = hover && Minecraft.getInstance().mouseHandler.isLeftPressed();
        int c = h >= 14 ? 3 : 2;
        int lift = down ? 1 : 0;
        float t = (Util.getMillis() % 100_000L) / 50F;
        int al = Mth.floor(alpha * 255);
        g.drawManaged(() -> {
            if (!flat) {
                // plate, and the glass sheen over its upper half
                HoloStyle.chamferFill(g, x, y + lift, w, h, c, fade(!active ? 0xC0080A10 : down ? 0xF0101824 : 0xE0070B12, al));
                int tint = !active ? 0x04 : selected ? 0x40 : hover ? 0x34 : 0x14;
                g.fillGradient(x + 1, y + lift + 1, x + w - 1, y + lift + h - 1, HoloStyle.argb(tint, accent), HoloStyle.argb(tint / 3, accent));
                if (active) g.fill(x + c, y + lift + 1, x + w - c, y + lift + Math.max(2, h / 2), HoloStyle.argb(hover ? 0x18 : 0x0A, 0xFFFFFF));
                // frame: faint at rest, bright under the mouse or when selected; the corners always a little brighter
                int fr = !active ? 0x30 : selected || hover ? 0xE0 : 0x78;
                HoloStyle.chamferFrame(g, x, y + lift, w, h, c, HoloStyle.argb(fr, accent));
                if (active) HoloStyle.chamferCorners(g, x, y + lift, w, h, c, Math.max(1, Math.min(4, w / 6)), HoloStyle.argb(hover || selected ? 0xFF : 0xB0, HoloStyle.bright(accent)));
                // the indicator notch on the left edge: lit when selected, breathing under the mouse
                if (h >= 10 && w >= 20) {
                    int lamp = selected ? 0xFF : hover ? 0x80 + (int) (0x60 * (0.5F + 0.5F * Mth.sin(t * 0.6F))) : 0x30;
                    g.fill(x + 1, y + lift + c + 1, x + 2, y + lift + h - c - 1, HoloStyle.argb(active ? lamp : 0x18, HoloStyle.bright(accent)));
                }
                if (selected) g.fill(x + c, y + h + lift - 2, x + w - c, y + h + lift - 1, HoloStyle.argb(0xFF, HoloStyle.bright(accent)));
                // hover: a slow scan running across
                if (hover && w > 8) {
                    int span = w - 2;
                    int p = (int) (t * 1.6F) % (span + 12) - 6;
                    if (p > -4 && p < span) g.fill(x + 1 + Math.max(0, p), y + lift + 1, x + 1 + Math.min(span, p + 3), y + lift + h - 1, HoloStyle.argb(0x22, 0xFFFFFF));
                }
            } else {
                int ul = selected ? 0xFF : hover ? 0x90 : 0x30;
                g.fill(x + 1, y + h - 1, x + w - 1, y + h, HoloStyle.argb(ul, selected ? HoloStyle.bright(accent) : accent));
                if (selected) g.fill(x + w / 2 - w / 6, y + h - 2, x + w / 2 + w / 6, y + h - 1, 0xC0FFFFFF);
                if (hover || selected) g.fillGradient(x, y, x + w, y + h, HoloStyle.argb(0x00, accent), HoloStyle.argb(selected ? 0x30 : 0x1C, accent));
            }
            // the press flash: a bright wash that fades, and a line sweeping outwards from the centre
            if (flash > 0) {
                g.fill(x + 1, y + lift + 1, x + w - 1, y + lift + h - 1, HoloStyle.argb((int) (0x70 * flash), HoloStyle.bright(accent)));
                int half = (int) ((1 - flash) * (w / 2F));
                int my0 = y + lift + h / 2;
                g.fill(x + w / 2 - half, my0, x + w / 2 + half, my0 + 1, HoloStyle.argb((int) (0xFF * flash), 0xFFFFFF));
            }
        });
        // label
        Font font = Minecraft.getInstance().font;
        Component msg = active ? getMessage() : Component.literal(getMessage().getString());
        boolean styled = msg.getStyle().getColor() != null;
        int col = !active ? 0x5A6270 : flash > 0.4F ? 0xFFFFFF : (hover || selected) ? 0xFFFFFF : HoloStyle.bright(accent);
        int tw = font.width(msg);
        int avail = w - (flat ? 2 : 6);
        int ty = y + lift + (h - 8) / 2 + (h % 2 == 0 ? 0 : 1) - (flat ? 1 : 0);
        if (tw <= avail) {
            g.drawString(font, msg, x + (w - tw) / 2, ty, styled && active ? 0xFFFFFFFF : 0xFF000000 | col, false);
        } else {
            TextFit.draw(g, font, msg, x + (flat ? 1 : 3), ty, avail, col);
        }
    }

    private static int fade(int argb, int alpha) {
        int a = (argb >>> 24) * alpha / 255;
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
