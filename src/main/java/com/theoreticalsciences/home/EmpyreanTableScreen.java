package com.theoreticalsciences.home;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Empyrean Assembly Table's screen. The 15 by 15 picture is drawn, not made of slots: 225 item icons at three
 * quarters size, lit where the table holds enough for that slot. The player's inventory is the only real slots here.
 */
public class EmpyreanTableScreen extends AbstractContainerScreen<EmpyreanTableMenu> {
    private static final int GOLD = 0xFFFFE9A8, DIM = 0xFFB89C5C, FAINT = 0xFF6A5A38, BG = 0xF0120E06, CELL_BG = 0xFF221A0C, WARN = 0xFFFF9A6A;
    private static final int GX = 10, GY = 18, CELL = 12, N = EmpyreanRecipe.SIZE;
    private static final int LX = 204, LY = 18, LW = 160, ROW = 13, ROWS = 4;         // the recipe list
    private static final int OY = 75;                                                 // the product and the buttons
    private List<RecipeHolder<EmpyreanRecipe>> recipes = List.of();
    private int scroll;

    public EmpyreanTableScreen(EmpyreanTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 374;
        imageHeight = 210;
        inventoryLabelX = EmpyreanTableMenu.INV_X;
        inventoryLabelY = EmpyreanTableMenu.INV_Y - 10;
        titleLabelX = 10;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        recipes = new ArrayList<>(minecraft.level.getRecipeManager().getAllRecipesFor(HomeRegistry.EMPYREAN_TYPE.get()));
        List<String> order = HomeMaterials.IDS;
        recipes.sort(Comparator.comparingInt((RecipeHolder<EmpyreanRecipe> h) -> {
            int i = HomeMaterials.index(h.value().result.item());
            return i < 0 ? order.size() : i;
        }).thenComparing(h -> h.id().toString()));
        ResourceLocation cur = menu.be.recipeId();
        for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).id().equals(cur)) scroll = Math.max(0, Math.min(i - 2, recipes.size() - ROWS));
    }

    private void send(int action, String arg) {
        PacketDistributor.sendToServer(new HomePayloads.TableAction(menu.be.getBlockPos(), action, arg));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ drawing
    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        g.renderOutline(x, y, imageWidth, imageHeight, DIM);
        g.renderOutline(x + 2, y + 2, imageWidth - 4, imageHeight - 4, FAINT);
        // the picture
        EmpyreanRecipe r = menu.be.recipe();
        Map<Item, Long> left = new HashMap<>(menu.be.held());
        g.fill(x + GX - 2, y + GY - 2, x + GX + N * CELL + 2, y + GY + N * CELL + 2, FAINT);
        for (int cy = 0; cy < N; cy++)
            for (int cx = 0; cx < N; cx++) {
                int px = x + GX + cx * CELL, py = y + GY + cy * CELL;
                g.fill(px, py, px + CELL - 1, py + CELL - 1, CELL_BG);
                EmpyreanRecipe.Slot s = r == null ? null : r.cell(cx, cy);
                if (s == null) continue;
                long have = left.getOrDefault(s.item(), 0L);
                boolean lit = have >= s.count();
                if (lit) left.put(s.item(), have - s.count());
                icon(g, new ItemStack(s.item()), px, py, CELL - 1);
                if (!lit) g.fill(RenderTypeHolder.OVER, px, py, px + CELL - 1, py + CELL - 1, 0xB0120E06);
            }
        // the recipe list
        g.fill(x + LX - 2, y + LY - 2, x + LX + LW + 2, y + LY + ROWS * ROW + 2, FAINT);
        g.fill(x + LX - 1, y + LY - 1, x + LX + LW + 1, y + LY + ROWS * ROW + 1, CELL_BG);
        ResourceLocation cur = menu.be.recipeId();
        for (int i = 0; i < ROWS && scroll + i < recipes.size(); i++) {
            RecipeHolder<EmpyreanRecipe> h = recipes.get(scroll + i);
            int ry = y + LY + i * ROW;
            boolean sel = h.id().equals(cur), over = in(mx, my, x + LX, ry, LW - 6, ROW);
            if (sel || over) g.fill(x + LX, ry, x + LX + LW - 6, ry + ROW - 1, sel ? 0x60FFE9A8 : 0x30FFE9A8);
            icon(g, new ItemStack(h.value().result.item()), x + LX + 1, ry, 12);
            String name = h.value().result.item().getDescription().getString();
            g.drawString(font, font.plainSubstrByWidth(name, LW - 60), x + LX + 16, ry + 2, sel ? GOLD : DIM, false);
            String n = "x" + HomeClient.compact(h.value().result.count());
            g.drawString(font, n, x + LX + LW - 8 - font.width(n), ry + 2, FAINT | 0xFF000000, false);
        }
        if (recipes.size() > ROWS) {
            int track = ROWS * ROW, bar = Math.max(8, track * ROWS / recipes.size());
            int by = y + LY + (track - bar) * scroll / Math.max(1, recipes.size() - ROWS);
            g.fill(x + LX + LW - 4, y + LY, x + LX + LW, y + LY + track, 0xFF30260F);
            g.fill(x + LX + LW - 4, by, x + LX + LW, by + bar, DIM);
        }
        // the product, and the buttons
        int ox = x + LX, oy = y + OY;
        g.renderOutline(ox - 1, oy - 1, 20, 20, GOLD);
        g.fill(ox, oy, ox + 18, oy + 18, CELL_BG);
        if (menu.be.outCount() > 0) {
            g.renderItem(new ItemStack(menu.be.outItem()), ox + 1, oy + 1);
            g.drawString(font, "x" + HomeClient.full(menu.be.outCount()), ox + 24, oy + 1, GOLD, false);
        } else if (r != null) {
            g.drawString(font, r.result.item().getDescription().getString(), ox + 24, oy + 1, FAINT | 0xFF000000, false);
        }
        String why = HomeClient.blocked;
        g.drawString(font, Component.translatable("gui.theoreticalsciences.empyrean." + (why.isEmpty() ? "ready" : why)), ox + 24, oy + 11, why.isEmpty() ? 0xFF9CFFB0 : WARN, false);
        button(g, ox + 0, oy + 22, 44, "craft", mx, my, why.isEmpty());
        button(g, ox + 48, oy + 22, 44, "craft_max", mx, my, why.isEmpty());
        button(g, ox + 96, oy + 22, 64, menu.be.auto() ? "auto_on" : "auto_off", mx, my, true);
        // the player's inventory
        for (var slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, FAINT);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, CELL_BG);
        }
    }

    /** Layer above the item icons, for the shade over a slot that is not yet filled. */
    private static final class RenderTypeHolder {
        static final net.minecraft.client.renderer.RenderType OVER = net.minecraft.client.renderer.RenderType.guiOverlay();
    }

    private void button(GuiGraphics g, int bx, int by, int w, String key, int mx, int my, boolean enabled) {
        boolean over = enabled && in(mx, my, bx, by, w, 12);
        g.fill(bx, by, bx + w, by + 12, over ? 0xFF5A4A20 : 0xFF30260F);
        g.renderOutline(bx, by, w, 12, enabled ? DIM : FAINT);
        Component c = Component.translatable("gui.theoreticalsciences.empyrean." + key);
        g.drawString(font, c, bx + (w - font.width(c)) / 2, by + 2, enabled ? GOLD : FAINT | 0xFF000000, false);
    }

    /** An item icon drawn {@code size} pixels wide instead of 16. */
    private static void icon(GuiGraphics g, ItemStack st, int x, int y, int size) {
        float s = size / 16F;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1F);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        if (!gridTip(g, mx, my)) renderTooltip(g, mx, my);
    }

    private EmpyreanRecipe.Slot cellAt(double mx, double my) {
        EmpyreanRecipe r = menu.be.recipe();
        int cx = (int) Math.floor((mx - leftPos - GX) / CELL), cy = (int) Math.floor((my - topPos - GY) / CELL);
        return r == null || cx < 0 || cy < 0 || cx >= N || cy >= N ? null : r.cell(cx, cy);
    }

    private boolean gridTip(GuiGraphics g, int mx, int my) {
        if (!menu.getCarried().isEmpty()) return false;
        EmpyreanRecipe r = menu.be.recipe();
        EmpyreanRecipe.Slot s = cellAt(mx, my);
        if (s != null) {
            long need = r.totals().get(s.item()), have = menu.be.held().getOrDefault(s.item(), 0L);
            g.renderComponentTooltip(font, List.of(s.item().getDescription(),
                    Component.translatable("gui.theoreticalsciences.empyrean.tip_slot", HomeClient.full(s.count())).withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.translatable("gui.theoreticalsciences.empyrean.tip_total", HomeClient.full(have), HomeClient.full(need))
                            .withStyle(have >= need ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GOLD),
                    Component.translatable("gui.theoreticalsciences.empyrean.tip_take").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mx, my);
            return true;
        }
        if (in(mx, my, leftPos + LX, topPos + OY, 18, 18) && menu.be.outCount() > 0) {
            g.renderComponentTooltip(font, List.of(menu.be.outItem().getDescription(),
                    Component.literal("x" + HomeClient.full(menu.be.outCount())).withStyle(net.minecraft.ChatFormatting.GOLD),
                    Component.translatable("gui.theoreticalsciences.empyrean.tip_take").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)), mx, my);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = leftPos, y = topPos;
        if (in(mx, my, x + LX, y + LY, LW - 6, ROWS * ROW)) {
            int i = scroll + (int) ((my - y - LY) / ROW);
            if (i < recipes.size()) {
                send(0, recipes.get(i).id().toString());
                click();
            }
            return true;
        }
        if (in(mx, my, x + GX, y + GY, N * CELL, N * CELL)) {
            if (!menu.getCarried().isEmpty()) send(1, "");
            else {
                EmpyreanRecipe.Slot s = cellAt(mx, my);
                if (s != null) send(5, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.item()) + (hasShiftDown() ? ";all" : ""));
            }
            return true;
        }
        int ox = x + LX, oy = y + OY;
        if (in(mx, my, ox, oy, 18, 18)) {
            send(2, hasShiftDown() ? "all" : "");
            return true;
        }
        if (in(mx, my, ox, oy + 22, 44, 12)) {
            send(3, "1");
            click();
            return true;
        }
        if (in(mx, my, ox + 48, oy + 22, 44, 12)) {
            send(3, "max");
            click();
            return true;
        }
        if (in(mx, my, ox + 96, oy + 22, 64, 12)) {
            send(4, "");
            click();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void click() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (in(mx, my, leftPos + LX - 2, topPos + LY - 2, LW + 4, ROWS * ROW + 4)) {
            scroll = Math.max(0, Math.min(recipes.size() - ROWS, scroll - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
