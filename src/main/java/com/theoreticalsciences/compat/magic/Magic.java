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
 * <li>1.11.0: <b>vis</b>, <b>flux</b> and <b>aspects</b> (Thaumaturge): vis from the chunk's aura (never below half of what
 *       the land holds), flux left in it, aspects from jars against the machine or its structure's ports;</li>
 * <li>1.11.0: <b>lumen</b> (Astral Sorcery): Lumen of one constellation's kind, or any, from what holds it against the
 *       machine or its ports.</li>
 * This class names nothing of those mods: {@link BotaniaMana}, {@link NaturesAuraAura}, {@link ThaumaturgeAura} and {@link AstralLumen} do, and they are only
 * touched once the mod is known to be installed, so the game loads without either.
 */
public final class Magic {
    private Magic() {}

    private static Boolean botania, naturesAura, thaumaturge, astral;

    public static boolean hasThaumaturge() {
        if (thaumaturge == null) thaumaturge = ModList.get().isLoaded("thaumaturge");
        return thaumaturge;
    }

    public static boolean hasAstral() {
        if (astral == null) astral = ModList.get().isLoaded("astralsorcery");
        return astral;
    }

    /** 1.11.0: where jars and Lumen holders may stand: against the machine, and against each of its structure's ports. */
    private static java.util.List<BlockPos> around(ServerLevel level, BlockPos pos) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        java.util.List<BlockPos> centres = new java.util.ArrayList<>();
        centres.add(pos);
        if (level.getBlockEntity(pos) instanceof com.theoreticalsciences.machine.ControllerBlockEntity c && c.isFormed()) centres.addAll(c.ports());
        for (BlockPos c : centres)
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos p = c.relative(d);
                if (level.isLoaded(p) && !out.contains(p)) out.add(p);
            }
        return out;
    }

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
        if (r.vis > 0 && !(hasThaumaturge() && guard(() -> ThaumaturgeAura.spareVis(level, pos) >= (double) r.vis * runs))) return MachineStatus.NO_VIS;
        if (!r.aspects.isEmpty() && !(hasThaumaturge() && guard(() -> ThaumaturgeAura.hasAspects(level, around(level, pos), r.aspects, runs)))) return MachineStatus.NO_ASPECTS;
        if (r.lumen > 0 && !(hasAstral() && guard(() -> AstralLumen.held(level, around(level, pos), r.lumenKind) >= (long) r.lumen * runs))) return MachineStatus.NO_LUMEN;
        if (r.flux > 0 && !hasThaumaturge()) return MachineStatus.NO_VIS;
        return null;
    }

    /** Takes one run's worth; false (and nothing taken) if it is not there. */
    public static boolean take(ServerLevel level, BlockPos pos, QHRecipe r) {
        if (!r.magical()) return true;
        if (lacking(level, pos, r, 1) != null) return false;
        if (r.mana > 0 && !guard(() -> drainMana(level, pos, r.mana))) return false;
        if (r.aura > 0) guard(() -> NaturesAuraAura.drain(level, pos, r.aura));
        if (r.vis > 0 && !guard(() -> ThaumaturgeAura.drainVis(level, pos, r.vis))) return false;
        if (!r.aspects.isEmpty() && !guard(() -> ThaumaturgeAura.drainAspects(level, around(level, pos), r.aspects))) return false;
        if (r.lumen > 0 && !guard(() -> AstralLumen.drain(level, around(level, pos), r.lumenKind, r.lumen))) return false;
        if (r.flux > 0) guard(() -> {
            ThaumaturgeAura.pollute(level, pos, r.flux);
            return true;
        });
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
