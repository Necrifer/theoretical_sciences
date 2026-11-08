package com.theoreticalsciences.compat;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.theoreticalsciences.client.preview.PreviewRenderer;
import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.machine.MachineBlock;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * GregTech / MMCR-style structure viewer, shared by the JEI and EMI pages (round 35: moved out of the JEI widget so EMI
 * gets the same 3D view; each recipe viewer only forwards its mouse events here, in recipe-relative coordinates):
 * <ul>
 *   <li>left-drag rotates, right-drag pans, scroll zooms;</li>
 *   <li>the arrows below step through the structure one layer at a time;</li>
 *   <li>hover a block to see what goes there - including whether an Interface Port may replace it and which tiers
 *       of a tiered component (coils, emitters) are accepted;</li>
 *   <li>positions that accept an Interface Port carry a pulsing cyan frame; tiered positions cycle through their tiers.</li>
 * </ul>
 */
public class MultiblockView {
    private final MultiblockDefinition def;
    private final int x0, y0, w, h;          // preview viewport (recipe-relative)
    private final int controlsY;
    private final ResourceLocation portId = com.theoreticalsciences.TheoreticalSciences.id("interface_port");

    private float yaw = 35, pitch = 28, zoom = 1, panX, panY;
    private boolean userMoved;
    private int layer = -1;                  // -1 = whole structure
    private boolean showPorts;               // GregTech-style: no frames unless asked for
    private int dragButton = -1;
    private int hx = -1, hy = -1, hz = -1;   // hovered pattern coordinates
    /** Draw "-" / "+" zoom buttons (for a recipe viewer that does not pass the scroll wheel on). */
    public boolean zoomButtons;

    public MultiblockView(MultiblockDefinition def, int x0, int y0, int w, int h) {
        this.def = def;
        this.x0 = x0;
        this.y0 = y0;
        this.w = w;
        this.h = h;
        this.controlsY = y0 + h + 2;
    }

    // ------------------------------------------------------------------ transform

    private float baseScale() {
        int size = Math.max(def.sizeX, Math.max(def.sizeY, def.sizeZ));
        return Math.min(w, h) * 0.62F / (size * 1.05F);
    }

    /** Pattern space -> recipe-relative GUI space. */
    private Matrix4f matrix(float time) {
        float s = baseScale() * zoom;
        float yawNow = userMoved ? yaw : yaw + time * 0.6F;
        Matrix4f m = new Matrix4f();
        m.translate(x0 + w / 2F + panX, y0 + h / 2F + panY, 200);
        m.scale(s, -s, s);
        m.rotate(Axis.XP.rotationDegrees(-pitch));
        m.rotate(Axis.YP.rotationDegrees(yawNow));
        m.translate(-def.sizeX / 2F, -def.sizeY / 2F, -def.sizeZ / 2F);
        return m;
    }

    private static float time() {
        Minecraft mc = Minecraft.getInstance();
        return (mc.level != null ? mc.level.getGameTime() : 0) + mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    private boolean visibleLayer(int y) {
        return layer < 0 || y == layer;
    }

    // ------------------------------------------------------------------ drawing

    public void draw(GuiGraphics g, double mx, double my) {
        Minecraft mc = Minecraft.getInstance();
        float t = time();
        g.fill(x0, y0, x0 + w, y0 + h, 0x40000000);
        g.renderOutline(x0, y0, w, h, 0x60406080);

        Matrix4f m = matrix(t);
        pick(m, mx, my);

        // the scissor stack works in absolute screen coordinates
        Matrix4f cur = g.pose().last().pose();
        int ax = (int) cur.m30(), ay = (int) cur.m31();
        g.enableScissor(ax + x0 + 1, ay + y0 + 1, ax + x0 + w - 1, ay + y0 + h - 1);
        g.flush();
        // our own depth range: nothing drawn earlier may hide the model, and the model must not hide what comes later
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.mulPose(m);
        Block port = ControllerBlockEntity.portBlock();
        Cells cells = cells(port);
        // 1.3.0: a big structure's still blocks are meshed in sections on background threads and drawn from the
        // graphics card (see PreviewRenderer); only the tiered parts, which cycle, are drawn one by one
        if (cells.renderer != null) {
            cells.renderer.upload();
            cells.renderer.render(pose.last().pose(), m, x0, y0, x0 + w, y0 + h);
        }
        Lighting.setupFor3DItems();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        java.util.Map<Character, BlockState> frame = new java.util.HashMap<>();
        for (int i = 0; i < cells.drawn.length; i++) {
            long p = cells.drawn[i];
            char c = cells.drawnChars[i];
            BlockState st = frame.computeIfAbsent(c, k -> stateFor(k, t, port));
            if (st == null) continue;
            pose.pushPose();
            pose.translate(net.minecraft.core.BlockPos.getX(p), net.minecraft.core.BlockPos.getY(p), net.minecraft.core.BlockPos.getZ(p));
            mc.getBlockRenderer().renderSingleBlock(st, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
        // GregTech-style: the model is shown clean. Tiered parts (coils, emitters) simply cycle through their accepted
        // tiers; the only frame is the block under the mouse. Interface Port positions are outlined only when toggled on.
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float pulse = 0.35F + 0.25F * Mth.sin(t * 0.25F);
        if (showPorts) for (long p : cells.ports) {
            int x = net.minecraft.core.BlockPos.getX(p), y = net.minecraft.core.BlockPos.getY(p), z = net.minecraft.core.BlockPos.getZ(p);
            LevelRenderer.renderLineBox(pose, lines, x - 0.02, y - 0.02, z - 0.02, x + 1.02, y + 1.02, z + 1.02, 0.25F, 0.9F, 1F, pulse);
        }
        if (hx >= 0)
            LevelRenderer.renderLineBox(pose, lines, hx - 0.02, hy - 0.02, hz - 0.02, hx + 1.02, hy + 1.02, hz + 1.02, 1F, 1F, 1F, 0.85F);
        buffers.endBatch();
        pose.popPose();
        Lighting.setupForFlatItems();
        g.disableScissor();
        com.mojang.blaze3d.systems.RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        // still being built: a thin bar along the bottom of the view
        if (cells.renderer != null && !cells.renderer.isReady()) {
            int bx0 = x0 + 4, bx1 = x0 + w - 4, by = y0 + h - 5;
            g.fill(bx0, by, bx1, by + 2, 0x40000000);
            g.fill(bx0, by, bx0 + Math.round((bx1 - bx0) * cells.renderer.progress()), by + 2, 0xFF40A0E0);
        }

        // controls: [<] Layer [>]    [R]
        var font = mc.font;
        button(g, x0, controlsY, "<", mx, my);
        Component lay = layer < 0 ? Component.translatable("jei.theoreticalsciences.layer_all")
                : Component.translatable("jei.theoreticalsciences.layer", layer + 1, def.sizeY);
        g.drawString(font, lay, x0 + 15, controlsY + 2, 0xA0B0C0, false);
        int rx = x0 + 15 + font.width(lay) + 3;
        button(g, rx, controlsY, ">", mx, my);
        button(g, x0 + w - 12, controlsY, "R", mx, my);
        if (zoomButtons) {
            button(g, x0 + w - 54, controlsY, "-", mx, my);
            button(g, x0 + w - 40, controlsY, "+", mx, my);
        }
        button(g, x0 + w - 26, controlsY, "P", mx, my);
        if (showPorts) g.renderOutline(x0 + w - 27, controlsY - 1, 14, 14, 0xFF40E0FF);
    }

    private void button(GuiGraphics g, int bx, int by, String label, double mx, double my) {
        boolean hover = mx >= bx && mx < bx + 12 && my >= by && my < by + 12;
        g.fill(bx, by, bx + 12, by + 12, hover ? 0xFF305070 : 0xFF1A2430);
        g.renderOutline(bx, by, 12, 12, 0xFF5080A0);
        var font = Minecraft.getInstance().font;
        g.drawString(font, label, bx + 6 - font.width(label) / 2, by + 2, 0xFFFFFF, false);
    }

    /** Structures with at least this many still blocks are drawn by a {@link PreviewRenderer}; smaller ones block by block. */
    private static final int GPU_MIN_BLOCKS = 1500;
    /** Most structure views kept built at once (each holds its meshes on the graphics card). */
    private static final int KEPT = 6;

    /**
     * One structure (one layer of it, or all) read once into lists of the positions that hold something, so a frame
     * never has to scan the whole bounding box (over a million cells for the largest structures).
     */
    private static final class Cells {
        /** Positions ({@link net.minecraft.core.BlockPos#asLong}) drawn one by one each frame, and their characters. */
        final long[] drawn;
        final char[] drawnChars;
        /** Positions that take an Interface Port. */
        final long[] ports;
        /** The still blocks of a large structure, or null when everything is drawn one by one. */

        final PreviewRenderer renderer;

        Cells(long[] drawn, char[] drawnChars, long[] ports, PreviewRenderer renderer) {
            this.drawn = drawn;
            this.drawnChars = drawnChars;
            this.ports = ports;
            this.renderer = renderer;
        }

        void close() {
            if (renderer != null) renderer.close();
        }
    }

    private record CellsKey(MultiblockDefinition def, int layer) {}

    private static final java.util.LinkedHashMap<CellsKey, Cells> CELLS = new java.util.LinkedHashMap<>(16, 0.75F, true);

    /** The positions of the layer shown (read once, kept for the last few views; the least recently shown is dropped). */
    private Cells cells(Block port) {
        CellsKey key = new CellsKey(def, layer);
        Cells c = CELLS.get(key);
        if (c != null) return c;
        it.unimi.dsi.fastutil.longs.LongArrayList tiered = new it.unimi.dsi.fastutil.longs.LongArrayList(), ports = new it.unimi.dsi.fastutil.longs.LongArrayList(),
                still = new it.unimi.dsi.fastutil.longs.LongArrayList();
        StringBuilder tieredChars = new StringBuilder(), stillChars = new StringBuilder();
        for (int y = 0; y < def.sizeY; y++) {
            if (!visibleLayer(y)) continue;
            List<String> rows = def.layers.get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    char ch = row.charAt(x);
                    if (ch == ' ' || ch == '.') continue;
                    long p = net.minecraft.core.BlockPos.asLong(x, y, z);
                    if (def.allowsPort(ch, portId)) ports.add(p);
                    if (def.tiers.containsKey(ch)) {
                        tiered.add(p);
                        tieredChars.append(ch);
                    } else {
                        still.add(p);
                        stillChars.append(ch);
                    }
                }
            }
        }
        if (still.size() >= GPU_MIN_BLOCKS) {
            it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<BlockState> map = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>(still.size());
            java.util.Map<Character, BlockState> states = new java.util.HashMap<>();
            for (int i = 0; i < still.size(); i++) {
                BlockState st = states.computeIfAbsent(stillChars.charAt(i), k -> stateFor(k, 0, port));
                if (st != null) map.put(still.getLong(i), st);
            }
            PreviewRenderer r = new PreviewRenderer(map, def.sizeX / 2F, def.sizeY / 2F, def.sizeZ / 2F);
            c = new Cells(tiered.toLongArray(), tieredChars.toString().toCharArray(), ports.toLongArray(), r);
        } else {
            // a small structure: every block drawn one by one, as before
            still.addAll(tiered);
            stillChars.append(tieredChars);
            c = new Cells(still.toLongArray(), stillChars.toString().toCharArray(), ports.toLongArray(), null);
        }
        CELLS.put(key, c);
        while (CELLS.size() > KEPT) {
            var eldest = CELLS.entrySet().iterator().next();
            CELLS.remove(eldest.getKey());
            eldest.getValue().close();
        }
        return c;
    }

    /** Frees every built view (resources reloaded: block models may have changed). */
    public static void releaseBaked() {
        List<Cells> old = new java.util.ArrayList<>(CELLS.values());
        CELLS.clear();
        if (!old.isEmpty()) com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(() -> old.forEach(Cells::close));
    }

    private BlockState stateFor(char c, float t, Block port) {
        List<Block> blocks = c == def.controllerChar ? def.blocksFor(c) : def.componentsFor(c, port);
        if (blocks.isEmpty()) return null;
        // tiered components cycle through their tiers so you can see what is accepted
        Block b = def.tiers.containsKey(c) ? blocks.get((int) (t / 40) % blocks.size()) : blocks.get(0);
        BlockState st = b.defaultBlockState();
        if (st.hasProperty(MachineBlock.FACING)) st = st.setValue(MachineBlock.FACING, Direction.NORTH);
        return st;
    }

    // ------------------------------------------------------------------ picking

    /** Casts the mouse ray (straight into the screen) back through the transform and finds the nearest block it hits. */
    private void pick(Matrix4f m, double mx, double my) {
        hx = hy = hz = -1;
        if (dragButton >= 0 || mx < x0 || my < y0 || mx >= x0 + w || my >= y0 + h) return;
        Matrix4f inv = new Matrix4f(m).invert();
        Vector4f a = inv.transform(new Vector4f((float) mx, (float) my, 3000, 1));
        Vector4f b = inv.transform(new Vector4f((float) mx, (float) my, -3000, 1));
        // 1.3.0: walk the ray cell by cell from the viewer's side (a 3D DDA) and stop at the first block, instead of
        // testing every cell of the bounding box
        float[] o = {a.x, a.y, a.z}, d = {b.x - a.x, b.y - a.y, b.z - a.z};
        int[] size = {def.sizeX, def.sizeY, def.sizeZ};
        float t0 = 0, t1 = 1;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-7F) {
                if (o[i] < 0 || o[i] > size[i]) return;
                continue;
            }
            float ta = -o[i] / d[i], tb = (size[i] - o[i]) / d[i];
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
            if (t0 > t1) return;
        }
        int[] cell = new int[3], step = new int[3];
        float[] tMax = new float[3], tDelta = new float[3];
        for (int i = 0; i < 3; i++) {
            float p = o[i] + d[i] * t0;
            cell[i] = Mth.clamp(Mth.floor(p), 0, size[i] - 1);
            if (Math.abs(d[i]) < 1e-7F) {
                step[i] = 0;
                tMax[i] = tDelta[i] = Float.MAX_VALUE;
            } else {
                step[i] = d[i] > 0 ? 1 : -1;
                tDelta[i] = Math.abs(1F / d[i]);
                tMax[i] = t0 + ((cell[i] + (step[i] > 0 ? 1 : 0)) - p) / d[i];
            }
        }
        while (true) {
            if (visibleLayer(cell[1])) {
                char c = def.at(cell[0], cell[1], cell[2]);
                if (c != ' ' && c != '.') {
                    hx = cell[0];
                    hy = cell[1];
                    hz = cell[2];
                    return;
                }
            }
            int k = tMax[0] < tMax[1] ? (tMax[0] < tMax[2] ? 0 : 2) : (tMax[1] < tMax[2] ? 1 : 2);
            if (tMax[k] > t1) return;
            cell[k] += step[k];
            if (cell[k] < 0 || cell[k] >= size[k]) return;
            tMax[k] += tDelta[k];
        }
    }

    public void tooltip(List<Component> tip, double mx, double my) {
        if (my >= controlsY) {
            if (mx >= x0 + w - 12) tip.add(Component.translatable("jei.theoreticalsciences.reset_view"));
            else if (zoomButtons && mx >= x0 + w - 54 && mx < x0 + w - 28) tip.add(Component.translatable("emi.theoreticalsciences.zoom"));
            else if (mx >= x0 + w - 26) tip.add(Component.translatable("jei.theoreticalsciences.port_legend"));
            else if (mx < x0 + 12) tip.add(Component.translatable("jei.theoreticalsciences.layer_down"));
            else tip.add(Component.translatable("jei.theoreticalsciences.layer_up"));
            return;
        }
        if (hx < 0) {
            tip.add(Component.translatable(zoomButtons ? "emi.theoreticalsciences.preview_help" : "jei.theoreticalsciences.preview_help").withStyle(ChatFormatting.GRAY));
            return;
        }
        char c = def.at(hx, hy, hz);
        Block port = ControllerBlockEntity.portBlock();
        List<Block> comps = c == def.controllerChar ? def.blocksFor(c) : def.componentsFor(c, port);
        if (comps.isEmpty()) return;
        MultiblockDefinition.TierSpec ts = def.tiers.get(c);
        if (ts == null) tip.add(comps.get(0).getName());
        else {
            tip.add(Component.translatable("jei.theoreticalsciences.tier_family." + ts.family()).withStyle(ChatFormatting.GOLD));
            for (MultiblockDefinition.TierOption o : def.tierOptions(c, port))     // (datapack-added tiers included)
                tip.add(Component.literal(" T" + o.tier() + "  ").append(o.block().getName()).withStyle(ChatFormatting.YELLOW));
            tip.add(Component.translatable("jei.theoreticalsciences.tier_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (def.allowsPort(c, portId))
            tip.add(Component.translatable("jei.theoreticalsciences.port_allowed").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("jei.theoreticalsciences.layer", hy + 1, def.sizeY).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(double mx, double my, int button) {
        if (my >= controlsY && my < controlsY + 12) {
            var font = Minecraft.getInstance().font;
            Component lay = layer < 0 ? Component.translatable("jei.theoreticalsciences.layer_all")
                    : Component.translatable("jei.theoreticalsciences.layer", layer + 1, def.sizeY);
            int rx = x0 + 15 + font.width(lay) + 3;
            if (mx >= x0 && mx < x0 + 12) {
                layer = layer < 0 ? def.sizeY - 1 : layer - 1;           // below the bottom layer wraps to "all"
                click();
                return true;
            }
            if (mx >= rx && mx < rx + 12) {
                layer = layer >= def.sizeY - 1 ? -1 : layer + 1;
                click();
                return true;
            }
            if (zoomButtons && mx >= x0 + w - 54 && mx < x0 + w - 28) {
                zoom = Mth.clamp(zoom * (mx >= x0 + w - 40 ? 1.25F : 1 / 1.25F), 0.3F, 6F);
                if (!userMoved) yaw = yaw + time() * 0.6F;
                userMoved = true;
                click();
                return true;
            }
            if (mx >= x0 + w - 26 && mx < x0 + w - 14) {
                showPorts = !showPorts;
                click();
                return true;
            }
            if (mx >= x0 + w - 12 && mx < x0 + w) {
                yaw = 35;
                pitch = 28;
                zoom = 1;
                panX = panY = 0;
                layer = -1;
                userMoved = false;
                click();
                return true;
            }
            return false;
        }
        if (mx >= x0 && my >= y0 && mx < x0 + w && my < y0 + h) {
            if (!userMoved) yaw = yaw + time() * 0.6F;      // freeze the auto-spin where it is
            userMoved = true;
            dragButton = button;
            return true;
        }
        return false;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                com.theoreticalsciences.registry.QHSounds.UI_CLICK, 1.0F));
    }

    public boolean mouseReleased(double mx, double my, int button) {
        boolean was = dragButton >= 0;
        dragButton = -1;
        return was;
    }

    /** The button being held on the view (-1 none). */
    public int dragging() {
        return dragButton;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragButton < 0) return false;
        if (button == 0 && !net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            yaw += (float) dx * 1.4F;
            pitch = Mth.clamp(pitch + (float) dy * 1.4F, -90F, 90F);
        } else {
            panX += (float) dx;
            panY += (float) dy;
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx < x0 || my < y0 || mx >= x0 + w || my >= y0 + h) return false;
        zoom = Mth.clamp(zoom * (sy > 0 ? 1.15F : 1 / 1.15F), 0.3F, 6F);
        userMoved = true;
        return true;
    }

    @SuppressWarnings("unused")
    private static ResourceLocation id(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b);
    }
}
