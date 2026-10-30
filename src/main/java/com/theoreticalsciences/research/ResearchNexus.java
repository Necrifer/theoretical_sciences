package com.theoreticalsciences.research;

import com.theoreticalsciences.machine.MachineStatus;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.UUID;

/**
 * The Research Nexus studies for you. Each Research Dossier it digests shortens its owner's current study:
 * the dossier has to be at least as advanced as the entry (empirical for early entries, absolute for the last ones).
 */
public final class ResearchNexus {
    /** Dossier tier (1-4) of an item, or 0 if it is not a dossier. */
    public static int tier(ItemStack s) {
        String p = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return switch (p) {
            case "empirical_dossier" -> 1;
            case "theoretical_dossier" -> 2;
            case "unified_dossier" -> 3;
            case "absolute_dossier" -> 4;
            default -> 0;
        };
    }

    /** Seconds of study one dossier of the given tier replaces. */
    public static int seconds(int tier) {
        return switch (tier) {
            case 1 -> 120;
            case 2 -> 300;
            case 3 -> 600;
            case 4 -> 1200;
            default -> 0;
        };
    }

    static int recipeTier(QHRecipe r) {
        int t = 0;
        for (SizedIngredient in : r.itemInputs)
            for (ItemStack s : in.ingredient().getItems()) t = Math.max(t, tier(s));
        return t;
    }

    public static MachineStatus gate(ServerLevel level, UUID owner, QHRecipe r) {
        if (owner == null) return MachineStatus.NO_ACTIVE_RESEARCH;
        ResearchSavedData.Entry e = ResearchSavedData.get(level.getServer()).peek(owner);
        if (e == null || e.active == null) return MachineStatus.NO_ACTIVE_RESEARCH;
        ResearchDefinition def = ResearchManager.INSTANCE.get(e.active);
        if (def != null && recipeTier(r) < def.dossierTier()) return MachineStatus.DOSSIER_TOO_WEAK;
        return null;
    }

    public static void apply(ServerLevel level, UUID owner, QHRecipe r) {
        apply(level, owner, r, 1);
    }

    /** {@code runs}: how many copies of the recipe finished at once (parallel) - each one's dossiers count. */
    public static void apply(ServerLevel level, UUID owner, QHRecipe r, int runs) {
        if (owner == null) return;
        int secs = 0;
        for (SizedIngredient in : r.itemInputs) {
            int t = 0;
            for (ItemStack s : in.ingredient().getItems()) t = Math.max(t, tier(s));
            secs += seconds(t) * in.count();
        }
        secs = (int) Math.min(Integer.MAX_VALUE, (long) secs * Math.max(1, runs));
        int done = ResearchService.accelerate(level.getServer(), owner, secs);
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
        if (p != null && done > 0)
            p.displayClientMessage(Component.translatable("message.theoreticalsciences.nexus_progress", ResearchService.formatTime(done)), true);
    }

    private ResearchNexus() {}
}
