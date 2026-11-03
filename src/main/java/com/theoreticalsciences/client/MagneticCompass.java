package com.theoreticalsciences.client;

import com.theoreticalsciences.registry.QHEffects;
import com.theoreticalsciences.science.Sciences;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Round 39b (1.2.8): a magnetic compass follows the local magnetic field, and a strong magnet close by outweighs the
 * planet's. Within 8 blocks of a magnet (block tag theoreticalsciences:magnetic - magnetite, lodestones) a compass you
 * hold swings towards it, shivering; while Magnetized it spins. Anywhere else it behaves exactly as vanilla's (the
 * original function is kept and called). Lodestone-bound compasses are left alone.
 */
public final class MagneticCompass {
    private static ItemPropertyFunction vanilla;
    private static long lastTick = Long.MIN_VALUE;
    private static BlockPos lastFound;
    private static int lastEntity;

    public static void register() {
        ResourceLocation angle = ResourceLocation.withDefaultNamespace("angle");
        vanilla = ItemProperties.getProperty(new ItemStack(Items.COMPASS), angle);
        if (vanilla == null) return;
        ItemProperties.register(Items.COMPASS, angle, (ClampedItemPropertyFunction) MagneticCompass::angle);
    }

    private static float angle(ItemStack stack, ClientLevel level, LivingEntity e, int seed) {
        if (e != null && level != null && !stack.has(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER)) {
            if (e.hasEffect(QHEffects.MAGNETIZED)) return Mth.positiveModulo((level.getGameTime() + e.getId()) * 0.137F, 1F);
            BlockPos m = nearestMagnet(level, e);
            if (m != null) {
                double yaw = Mth.positiveModulo(e.getVisualRotationYInDegrees() / 360.0, 1.0);
                double to = Math.atan2(m.getZ() + 0.5 - e.getZ(), m.getX() + 0.5 - e.getX()) / (Math.PI * 2);
                double shiver = Math.sin((level.getGameTime() + e.getId()) * 0.9) * 0.015;
                return Mth.positiveModulo((float) (0.5 - (yaw - 0.25 - to) + shiver), 1F);
            }
        }
        return vanilla == null ? 0 : vanilla.call(stack, level, e, seed);
    }

    /** Nearest magnet within 8 blocks, looked up at most once a tick. */
    private static BlockPos nearestMagnet(ClientLevel level, LivingEntity e) {
        long t = level.getGameTime();
        if (t == lastTick && e.getId() == lastEntity) return lastFound;
        lastTick = t;
        lastEntity = e.getId();
        BlockPos c = e.blockPosition(), best = null;
        double bd = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx <= 8; dx++)
            for (int dy = -4; dy <= 4; dy++)
                for (int dz = -8; dz <= 8; dz++) {
                    if (!level.getBlockState(p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz)).is(Sciences.MAGNETIC_BLOCKS)) continue;
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d < bd) {
                        bd = d;
                        best = p.immutable();
                    }
                }
        return lastFound = best;
    }

    private MagneticCompass() {}
}
