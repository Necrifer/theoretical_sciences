package com.theoreticalsciences.foam;

import com.theoreticalsciences.item.BraneShifterItem;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Shrinks you below the Planck length, into the Quantum Foam - and back. */
public class PlanckDiverItem extends BraneShifterItem {
    public PlanckDiverItem(Properties p) {
        super(p);
    }

    @Override
    protected ResourceKey<Level> destination() {
        return QHDimensions.FOAM;
    }

    @Override
    protected String researchId() {
        return "quantum_foam";
    }

    @Override
    protected String platformBlock() {
        return "foam_substrate";
    }

    @Override
    protected String coreBlock() {
        return "foam_substrate";
    }
}
