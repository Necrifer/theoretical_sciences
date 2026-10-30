package com.theoreticalsciences.gr;

import com.theoreticalsciences.item.BraneShifterItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Opens the Transfinite Hierarchy, where every infinity has its own place. */
public class CantorKeyItem extends BraneShifterItem {
    public CantorKeyItem(Properties p) {
        super(p);
    }

    @Override
    protected ResourceKey<Level> destination() {
        return QHDimensions.TRANSFINITE;
    }

    @Override
    protected String researchId() {
        return "transfinite_hierarchy";
    }

    @Override
    protected String platformBlock() {
        return "ordinal_stone";
    }

    @Override
    protected String coreBlock() {
        return "aleph_glass";
    }
}
