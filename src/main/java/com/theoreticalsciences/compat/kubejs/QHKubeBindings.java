package com.theoreticalsciences.compat.kubejs;

import com.theoreticalsciences.research.ResearchManager;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Exposed to scripts as {@code TheoreticalSciences}. */
public final class QHKubeBindings {
    public static boolean hasResearch(ServerPlayer player, String id) {
        return ResearchService.has(player, ResourceLocation.parse(id));
    }

    public static void grantResearch(ServerPlayer player, String id) {
        ResearchService.grant(player, ResourceLocation.parse(id));
    }

    public static void revokeResearch(ServerPlayer player, String id) {
        ResearchService.revoke(player, ResourceLocation.parse(id));
    }

    public static int getInsight(ServerPlayer player) {
        return ResearchService.insight(player);
    }

    public static void addInsight(ServerPlayer player, int amount) {
        ResearchService.addInsight(player, amount);
    }

    public static List<String> researchIds() {
        return ResearchManager.INSTANCE.entries().keySet().stream().map(ResourceLocation::toString).toList();
    }

    private QHKubeBindings() {}
}
