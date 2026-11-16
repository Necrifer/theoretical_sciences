package com.theoreticalsciences.compat.magic;

import de.ellpeck.naturesaura.api.aura.chunk.IAuraChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Nature's Aura's side of {@link Magic}: only loaded when Nature's Aura is installed. */
final class NaturesAuraAura {
    private NaturesAuraAura() {}

    static final int RADIUS = 20;

    /** Aura in the air round the machine above half of what the land holds by nature: what a recipe may take. */
    static long spare(ServerLevel level, BlockPos pos) {
        return (long) IAuraChunk.getAuraInArea(level, pos, RADIUS) - IAuraChunk.DEFAULT_AURA / 2;
    }

    static boolean drain(ServerLevel level, BlockPos pos, int amount) {
        int left = amount;
        for (int i = 0; i < 8 && left > 0; i++) {       // from the richest spot each time, as Nature's Aura's own machines do
            BlockPos spot = IAuraChunk.getHighestSpot(level, pos, RADIUS, pos);
            int got = IAuraChunk.getAuraChunk(level, spot).drainAura(spot, left);
            if (got <= 0) break;
            left -= got;
        }
        return true;
    }
}
