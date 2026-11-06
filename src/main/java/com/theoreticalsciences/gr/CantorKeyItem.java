package com.theoreticalsciences.gr;

import com.theoreticalsciences.item.BraneShifterItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Opens the Transfinite Hierarchy, where every infinity has its own place: since 1.2.0, the Platonic Realm's upper strata. */
public class CantorKeyItem extends BraneShifterItem {
    public CantorKeyItem(Properties p) {
        super(p);
    }

    /** 1.2.0: the Hierarchy is the Platonic Realm's upper strata now. */
    @Override
    protected ResourceKey<Level> destination() {
        return QHDimensions.PLATONIC;
    }

    @Override
    protected boolean leaving(net.minecraft.server.level.ServerPlayer sp) {
        return sp.level().dimension() == QHDimensions.PLATONIC && sp.getY() >= com.theoreticalsciences.world.Strata.Y;
    }

    @Override
    protected int[] band() {
        return com.theoreticalsciences.world.Strata.Layer.INFINITE.band();
    }

    @Override
    protected ResourceKey<Level> transitKey() {
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
