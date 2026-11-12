package com.theoreticalsciences.compat.magic;

import com.theoreticalsciences.machine.MachineStatus;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

/**
 * 1.7.3: recipes that draw on another mod's own mechanic in place of that mod's items.
 * <ul>
 *   <li><b>mana</b> (Botania): taken from a Mana Pool, or anything else that holds mana, standing against the machine
 *       (for a structure: against its controller);</li>
 *   <li><b>aura</b> (Nature's Aura): taken from the air round the machine, and never below half of what the land
 *       holds by nature.</li>
 * </ul>
 * This class names nothing of those mods: {@link BotaniaMana} and {@link NaturesAuraAura} do, and they are only
 * touched once the mod is known to be installed, so the game loads without either.
 */
public final class Magic {
    private Magic() {}

    private static Boolean botania, naturesAura;

    public static boolean hasBotania() {
        if (botania == null) botania = ModList.get().isLoaded("botania");
        return botania;
    }

    public static boolean hasNaturesAura() {
        if (naturesAura == null) naturesAura = ModList.get().isLoaded("naturesaura");
        return naturesAura;
    }

    /** The mana a machine can draw on: its structure's Mana Ports, then whatever holds mana against the machine itself. */
    private static long mana(ServerLevel level, BlockPos pos) {
        long sum = 0;
        for (com.theoreticalsciences.machine.ManaPortBlockEntity p : manaPorts(level, pos)) sum += p.mana();
        return sum + BotaniaMana.stored(level, pos);
    }

    private static java.util.List<com.theoreticalsciences.machine.ManaPortBlockEntity> manaPorts(ServerLevel level, BlockPos pos) {
        java.util.List<com.theoreticalsciences.machine.ManaPortBlockEntity> out = new java.util.ArrayList<>();
        if (level.getBlockEntity(pos) instanceof com.theoreticalsciences.machine.ControllerBlockEntity c && c.isFormed())
            for (BlockPos p : c.ports())
                if (level.isLoaded(p) && level.getBlockEntity(p) instanceof com.theoreticalsciences.machine.ManaPortBlockEntity m) out.add(m);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values())      // and one set against a single-block machine
            if (level.getBlockEntity(pos.relative(d)) instanceof com.theoreticalsciences.machine.ManaPortBlockEntity m && !out.contains(m)) out.add(m);
        return out;
    }

    private static boolean drainMana(ServerLevel level, BlockPos pos, int amount) {
        if (mana(level, pos) < amount) return false;
        int left = amount;
        for (com.theoreticalsciences.machine.ManaPortBlockEntity p : manaPorts(level, pos)) {
            int take = Math.min(left, p.mana());
            p.add(-take);
            left -= take;
            if (left <= 0) return true;
        }
        return BotaniaMana.drain(level, pos, left);
    }

    /** What the recipe still lacks of the mechanics it draws on, for {@code runs} runs; null if nothing. */
    public static MachineStatus lacking(ServerLevel level, BlockPos pos, QHRecipe r, int runs) {
        if (r.mana > 0 && !(hasBotania() && guard(() -> mana(level, pos) >= (long) r.mana * runs))) return MachineStatus.NO_MANA;
        if (r.aura > 0 && !(hasNaturesAura() && guard(() -> NaturesAuraAura.spare(level, pos) >= (long) r.aura * runs))) return MachineStatus.NO_AURA;
        return null;
    }

    /** Takes one run's worth; false (and nothing taken) if it is not there. */
    public static boolean take(ServerLevel level, BlockPos pos, QHRecipe r) {
        if (r.mana <= 0 && r.aura <= 0) return true;
        if (lacking(level, pos, r, 1) != null) return false;
        if (r.mana > 0 && !guard(() -> drainMana(level, pos, r.mana))) return false;
        if (r.aura > 0) guard(() -> NaturesAuraAura.drain(level, pos, r.aura));
        return true;
    }

    /** Mod bus: gives the Mana Port to Botania as a mana receiver, when Botania is there. */
    public static void registerCaps(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e) {
        if (hasBotania()) guard(() -> {
            BotaniaMana.registerPort(e);
            return true;
        });
    }

    private interface Check {
        boolean get();
    }

    /** Another mod's interface may change under us: a failure there stops the recipe, never the game. */
    private static boolean guard(Check c) {
        try {
            return c.get();
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                com.theoreticalsciences.TheoreticalSciences.LOGGER.warn("A magic mod's interface did not answer as expected; recipes that draw on it will wait.", t);
            }
            return false;
        }
    }

    private static boolean warned;
}
