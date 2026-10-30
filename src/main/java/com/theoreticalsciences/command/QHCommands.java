package com.theoreticalsciences.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.theoreticalsciences.research.ResearchManager;
import com.theoreticalsciences.research.ResearchSavedData;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * /theoreticalsciences research grant|revoke &lt;player&gt; &lt;id|*&gt;
 * /theoreticalsciences research list &lt;player&gt;
 * /theoreticalsciences insight add &lt;player&gt; &lt;amount&gt;
 */
public final class QHCommands {
    private static final SuggestionProvider<CommandSourceStack> RESEARCH = (ctx, b) ->
            SharedSuggestionProvider.suggestResource(ResearchManager.INSTANCE.entries().keySet(), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("theoreticalsciences").requires(s -> s.hasPermission(2))
                .then(Commands.literal("research")
                        .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.literal("*").executes(ctx -> {
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player"))
                                        ResearchManager.INSTANCE.entries().keySet().forEach(id -> ResearchService.grant(p, id));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted all research"), true);
                                    return 1;
                                }))
                                .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(RESEARCH).executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.grant(p, id);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted " + id), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.literal("*").executes(ctx -> {
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player"))
                                        ResearchManager.INSTANCE.entries().keySet().forEach(id -> ResearchService.revoke(p, id));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Revoked all research"), true);
                                    return 1;
                                }))
                                .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(RESEARCH).executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.revoke(p, id);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + id), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            ResearchSavedData.Entry e = ResearchSavedData.get(p.server).of(p.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": insight " + e.insight
                                    + ", observations " + e.observed.size() + ", research " + e.unlocked), false);
                            return 1;
                        }))))
                .then(Commands.literal("insight").then(Commands.literal("add").then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.addInsight(p, amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " insight"), true);
                            return 1;
                        }))))));
    }

    private QHCommands() {}
}
