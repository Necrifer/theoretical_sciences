package com.theoreticalsciences.compat.magic;

import com.leclowndu93150.thaumaturge.api.aspect.AspectCapabilities;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.IAspectContainer;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

/** Thaumaturge's side of {@link Magic}: only loaded when Thaumaturge is installed. */
final class ThaumaturgeAura {
    private ThaumaturgeAura() {}

    /** Vis in the chunk's aura above half of what the land holds by nature: what a recipe may take (as with Nature's Aura). */
    static float spareVis(ServerLevel level, BlockPos pos) {
        return AuraHelper.getVis(level, pos) - AuraHelper.getAuraBase(level, pos) / 2F;
    }

    static boolean drainVis(ServerLevel level, BlockPos pos, int amount) {
        float got = AuraHelper.drainVis(level, pos, amount, true);           // (true: only ask)
        if (got + 1e-3F < amount) return false;
        AuraHelper.drainVis(level, pos, amount, false);
        return true;
    }

    /** The flux a recipe leaves in the aura: magic worked by force, as in Thaumaturge itself, pollutes. */
    static void pollute(ServerLevel level, BlockPos pos, int amount) {
        AuraHelper.polluteAura(level, pos, amount, true);
    }

    private static Holder<IAspect> aspect(ServerLevel level, String id) {
        ResourceLocation rl = ResourceLocation.parse(id.contains(":") ? id : "thaumaturge:" + id);
        return Aspects.resolve(level, ResourceKey.create(IAspect.REGISTRY_KEY, rl));
    }

    private static int held(ServerLevel level, List<BlockPos> around, Holder<IAspect> a) {
        int sum = 0;
        for (BlockPos p : around)
            for (Direction d : Direction.values()) {
                IAspectContainer c = level.getCapability(AspectCapabilities.CONTAINER, p, d);
                if (c != null) {
                    sum += Math.max(0, c.amountOf(a));
                    break;                                   // one container, however many sides it answers on
                }
            }
        return sum;
    }

    /** Whether the jars and other containers against the machine hold every aspect the recipe asks for, {@code runs} times. */
    static boolean hasAspects(ServerLevel level, List<BlockPos> around, Map<String, Integer> want, int runs) {
        for (var e : want.entrySet())
            if (held(level, around, aspect(level, e.getKey())) < (long) e.getValue() * runs) return false;
        return true;
    }

    static boolean drainAspects(ServerLevel level, List<BlockPos> around, Map<String, Integer> want) {
        if (!hasAspects(level, around, want, 1)) return false;
        for (var e : want.entrySet()) {
            Holder<IAspect> a = aspect(level, e.getKey());
            int left = e.getValue();
            for (BlockPos p : around) {
                if (left <= 0) break;
                for (Direction d : Direction.values()) {
                    IAspectContainer c = level.getCapability(AspectCapabilities.CONTAINER, p, d);
                    if (c == null) continue;
                    int take = Math.min(left, Math.max(0, c.amountOf(a)));
                    if (take > 0 && c.drain(a, take)) left -= take;
                    break;
                }
            }
        }
        return true;
    }
}
