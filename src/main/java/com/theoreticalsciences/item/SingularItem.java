package com.theoreticalsciences.item;

import net.minecraft.world.item.ItemStack;

/** Items rendered by a custom 3D renderer (black hole, white hole, tesseract). */
public class SingularItem extends LoreItem {
    public final String kind;

    public SingularItem(Properties props, String kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
