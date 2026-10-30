package com.theoreticalsciences.client;

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

    private static ResourceLocation find(String mod) {
        try {
            ItemStack icon = ItemStack.EMPTY;
            for (var e : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
                if (!e.getKey().location().getNamespace().equals(mod)) continue;
                CreativeModeTab t = e.getValue();
                ItemStack i = t.getIconItem();
                if (!i.isEmpty()) {
                    icon = i;
                    break;
                }
            }
            if (icon.isEmpty())
                for (var it : BuiltInRegistries.ITEM)
                    if (BuiltInRegistries.ITEM.getKey(it).getNamespace().equals(mod)) {
                        icon = new ItemStack(it);
                        break;
                    }
            if (icon.isEmpty()) return NONE;
            var sprite = Minecraft.getInstance().getItemRenderer().getModel(icon, null, null, 0).getParticleIcon();
            // only sprites of the block atlas can be drawn as a fluid (item and block textures both live there)
            if (!sprite.atlasLocation().equals(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)) return NONE;
            ResourceLocation name = sprite.contents().name();
            return name.getPath().equals("missingno") ? NONE : name;
        } catch (RuntimeException e) {
            return NONE;
        }
    }

    /** Resource packs reloaded: look again. */
    public static void clear() {
        SPRITE.clear();
    }

    private EssenceLook() {}
}
