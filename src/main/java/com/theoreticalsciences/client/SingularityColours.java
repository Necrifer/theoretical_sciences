package com.theoreticalsciences.client;

import com.theoreticalsciences.extreme.DynamicSingularityItem;
import com.theoreticalsciences.extreme.Singularities;
import com.theoreticalsciences.registry.QHItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Tints for pack-added and automatic singularities (layer 0: the dark core, layer 1: the glow). A colour the pack
 * did not give is read from the source item's own texture: its average colour, darkened for the core and brightened
 * for the glow.
 */
public final class SingularityColours {
    private static final Map<String, int[]> FROM_TEXTURE = new java.util.concurrent.ConcurrentHashMap<>();

    public static void register(RegisterColorHandlersEvent.Item e) {
        e.register((stack, layer) -> FastColor.ARGB32.opaque(tint(stack, layer)), QHItems.get("dynamic_singularity"), QHItems.get("singularity_cluster"));
        // round 22: each mod's simulated universe in a colour of its own (body dark, galaxy bright, glass untinted)
        e.register((stack, layer) -> {
            int c = com.theoreticalsciences.transcend.SimulatedUniverseItem.colour(com.theoreticalsciences.transcend.SimulatedUniverseItem.mod(stack));
            if (layer == 0) return FastColor.ARGB32.opaque(((c >> 16 & 0xFF) / 6) << 16 | ((c >> 8 & 0xFF) / 6) << 8 | (c & 0xFF) / 5 + 16);
            return FastColor.ARGB32.opaque(layer == 1 ? c : 0xFFFFFF);
        }, QHItems.get("simulated_universe"));
        // round 24: specimen bundles, in their mod's colour (layer 1)
        e.register((stack, layer) -> FastColor.ARGB32.opaque(layer == 0 ? com.theoreticalsciences.extreme.Collections.colour(stack) : 0xFFFFFF), QHItems.get("specimen_bundle"));
    }

    public static void clear() {
        FROM_TEXTURE.clear();
    }

    static int tint(ItemStack stack, int layer) {
        int[] c = DynamicSingularityItem.colours(stack);
        int v = layer <= 0 ? c[0] : c[1];
        if (v >= 0) return v;
        String id = Singularities.idOf(stack);
        if (Singularities.any().isEmpty()) return layer <= 0 ? 0x202020 : 0xFFFFFF;   // list not here yet: don't remember a guess
        int[] t = FROM_TEXTURE.computeIfAbsent(id == null ? "" : id, k -> fromTexture(k));
        return layer <= 0 ? t[0] : t[1];
    }

    private static int[] fromTexture(String id) {
        Singularities.Def d = null;
        for (Singularities.Def x : Singularities.any()) if (x.id().equals(id)) d = x;
        if (d == null) return new int[]{0x202020, 0xFFFFFF};
        ItemStack src = d.source();
        try {
            TextureAtlasSprite sp = Minecraft.getInstance().getItemRenderer().getModel(src, null, null, 0).getParticleIcon();
            long r = 0, g = 0, b = 0, n = 0;
            int bright = 0, bl = -1;
            int w = sp.contents().width(), h = sp.contents().height();
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int abgr = sp.getPixelRGBA(0, x, y);
                    if (((abgr >>> 24) & 0xFF) < 128) continue;
                    int rr = abgr & 0xFF, gg = (abgr >> 8) & 0xFF, bb = (abgr >> 16) & 0xFF;
                    r += rr;
                    g += gg;
                    b += bb;
                    n++;
                    int lum = rr * 3 + gg * 6 + bb;
                    if (lum > bl) {
                        bl = lum;
                        bright = (rr << 16) | (gg << 8) | bb;
                    }
                }
            if (n == 0) return new int[]{0x202020, 0xFFFFFF};
            int ar = (int) (r / n), ag = (int) (g / n), ab = (int) (b / n);
            int core = ((ar / 4) << 16) | ((ag / 4) << 8) | (ab / 4);
            int glow = (Math.min(255, (ar + ((bright >> 16) & 0xFF)) / 2 + 40) << 16) | (Math.min(255, (ag + ((bright >> 8) & 0xFF)) / 2 + 40) << 8)
                    | Math.min(255, (ab + (bright & 0xFF)) / 2 + 40);
            return new int[]{core, glow};
        } catch (RuntimeException ex) {
            return new int[]{0x202020, 0xFFFFFF};
        }
    }

    private SingularityColours() {}
}
