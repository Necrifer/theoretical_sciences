package com.theoreticalsciences.orbit;

import com.theoreticalsciences.interior.InteriorGear;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.12.0: a solar particle storm out where there is no air and no magnetosphere (the Orbital Station, the Accretion
 * Frontier). Storm protons are many and of modest energy, so a little hydrogen-rich matter stops most of them: the dose
 * through a shield of areal density x falls as e^(−x/λ). Each of the six directions is looked along for 8 blocks and
 * what it passes through weighs: polyethylene 1.6, a water shield tank 1.2, water 1.0, the station's hull 0.35, other
 * solid blocks 0.25. What gets through is the average of e^(−Σw) over the six. Each Spacefarer Suit piece stops a fifth.
 */
public final class SpaceShelter {
    private SpaceShelter() {}

    /** About 5 Sv/h in the open at the height of a big storm (the August 1972 event, outside a spacecraft). */
    public static final double STORM_MICRO_SV_PER_HOUR = 5.0e6;
    static final int REACH = 8;

    public static boolean exposedLevel(Level level) {
        return level.dimension() == QHDimensions.ORBITAL_STATION || level.dimension() == QHDimensions.FRONTIER;
    }

    public static double stormMicroSvPerHour(ServerPlayer p) {
        if (!exposedLevel(p.level()) || !com.theoreticalsciences.helio.SpaceWeather.raging(p.server)) return 0;
        return STORM_MICRO_SV_PER_HOUR * transmission(p.level(), p.blockPosition().above()) * suit(p);
    }

    public static double suit(Player p) {
        int n = 0;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET})
            if (p.getItemBySlot(slot).getItem() instanceof InteriorGear.SuitItem si && si.suit == InteriorGear.Suit.SPACEFARER) n++;
        return Math.pow(0.8, n);
    }

    public static double transmission(Level level, BlockPos head) {
        double sum = 0;
        for (Direction d : Direction.values()) {
            double w = 0;
            BlockPos.MutableBlockPos m = head.mutable();
            for (int i = 0; i < REACH; i++) {
                m.move(d);
                if (!level.isLoaded(m)) break;
                w += weight(level.getBlockState(m));
            }
            sum += Math.exp(-w);
        }
        return sum / 6;
    }

    static double weight(BlockState s) {
        if (s.isAir()) return 0;
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (path.equals("polyethylene_shielding")) return 1.6;
        if (path.equals("water_shield_tank")) return 1.2;
        if (s.getFluidState().is(FluidTags.WATER)) return 1.0;
        if (path.equals("station_hull")) return 0.35;
        return s.canOcclude() ? 0.25 : 0.05;
    }
}
