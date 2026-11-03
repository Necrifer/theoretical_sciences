package com.theoreticalsciences.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import com.theoreticalsciences.transcend.Essences;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Round 30: what a mod's Essence looks like. Instead of one shared fluid texture, each mod's Essence shows that mod's
 * most recognisable picture - the icon of its own creative tab (the item the mod chose to stand for itself), or failing
 * that its first item - tiled like a fluid. It does not look like water; it looks like the mod.
 */
public final class EssenceLook {
    private static final Map<String, ResourceLocation> SPRITE = new ConcurrentHashMap<>();
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("missingno");

    /** The texture (in the block atlas) for this Essence, or null to use the plain Essence texture. */
    @Nullable
    public static ResourceLocation texture(FluidStack s) {
        String mod = Essences.mod(s);
        if (mod == null) return null;
        ResourceLocation r = SPRITE.computeIfAbsent(mod, EssenceLook::find);
        return r == NONE ? null : r;
    }

    /**
     * Round 37 (1.1): the same picture on every launch, and never an invisible one. The mod's creative tabs are tried in name order
     * (the registry's own order changes from launch to launch), then its items in name order; a picture is taken only if it is in the
     * block atlas and at least half of it is opaque. Mods whose pictures are all see-through keep the plain Essence in their colour.
     */
    private static ResourceLocation find(String mod) {
        try {
            List<ItemStack> candidates = new ArrayList<>();
            BuiltInRegistries.CREATIVE_MODE_TAB.entrySet().stream()
                    .filter(e -> e.getKey().location().getNamespace().equals(mod))
                    .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                    .forEach(e -> {
                        ItemStack i = e.getValue().getIconItem();
                        if (!i.isEmpty()) candidates.add(i);
                    });
            BuiltInRegistries.ITEM.keySet().stream()
                    .filter(k -> k.getNamespace().equals(mod))
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .limit(48)
                    .forEach(k -> candidates.add(new ItemStack(BuiltInRegistries.ITEM.get(k))));
            for (ItemStack icon : candidates) {
                ResourceLocation r = usable(icon);
                if (r != null) return r;
            }
            return NONE;
        } catch (RuntimeException e) {
            return NONE;
        }
    }

    @Nullable
    private static ResourceLocation usable(ItemStack icon) {
        if (icon.isEmpty()) return null;
        TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(icon, null, null, 0).getParticleIcon();
        // only sprites of the block atlas can be drawn as a fluid (item and block textures both live there)
        if (!sprite.atlasLocation().equals(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)) return null;
        ResourceLocation name = sprite.contents().name();
        if (name.getPath().equals("missingno")) return null;
        // mostly transparent (a glass pane, an outline, an empty frame...) would make the fluid invisible
        var c = sprite.contents();
        int w = c.width(), h = c.height(), opaque = 0;
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                if (!c.isTransparent(0, x, y)) opaque++;
        return opaque * 2 >= w * h ? name : null;
    }

    /** Resource packs reloaded: look again. */
    public static void clear() {
        SPRITE.clear();
    }

    private EssenceLook() {}
}
