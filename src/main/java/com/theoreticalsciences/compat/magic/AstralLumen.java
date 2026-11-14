package com.theoreticalsciences.compat.magic;

import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Astral Sorcery's side of {@link Magic}: only loaded when Astral Sorcery is installed. Lumen is starlight held in
 * Astral's network, of one constellation's kind (Aevitas, Armara, Discidia...) or prismatic; a recipe asks for a kind,
 * or for any.
 */
final class AstralLumen {
    private AstralLumen() {}

    private static boolean matches(LumenStack s, String kind) {
        if (s.isEmpty()) return false;
        if (kind.isEmpty() || kind.equals("any")) return true;
        String want = kind.contains(":") ? kind : "astralsorcery:" + kind;
        return s.getLumen().getRegistryKey().map(k -> k.location().toString().equals(want)).orElse(false);
    }

    private static ILumenHandler handler(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.values()) {
            ILumenHandler h = level.getCapability(ILumenHandler.BLOCK, p, d);
            if (h != null) return h;
        }
        return level.getCapability(ILumenHandler.BLOCK, p, null);
    }

    static long held(ServerLevel level, List<BlockPos> around, String kind) {
        long sum = 0;
        for (BlockPos p : around) {
            ILumenHandler h = handler(level, p);
            if (h == null) continue;
            for (LumenStack s : h.getContainedLumen())
                if (matches(s, kind)) sum += s.getAmount();
        }
        return sum;
    }

    static boolean drain(ServerLevel level, List<BlockPos> around, String kind, int amount) {
        if (held(level, around, kind) < amount) return false;
        int left = amount;
        for (BlockPos p : around) {
            if (left <= 0) break;
            ILumenHandler h = handler(level, p);
            if (h == null) continue;
            for (LumenStack s : List.copyOf(h.getContainedLumen())) {
                if (left <= 0) break;
                if (!matches(s, kind)) continue;
                LumenStack got = h.drain(s.getLumen(), Math.min(left, s.getAmount()), ILumenHandler.Action.EXECUTE);
                left -= got.getAmount();
            }
        }
        return left <= 0;
    }
}
