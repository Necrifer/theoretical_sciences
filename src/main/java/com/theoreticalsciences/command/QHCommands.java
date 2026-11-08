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
                        })))))
                .then(radiation())
                .then(com.theoreticalsciences.orbit.OrbitCommands.station()));   // 1.4.0
    }

    /**
     * Round 37 (1.1): /theoreticalsciences radiation reset [targets] | get <target> | set <targets> <dose>. Reset also clears the dose
     * the linked mods hold (Mekanism, GregTech), so testing starts clean everywhere.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> radiation() {
        return Commands.literal("radiation")
                .then(Commands.literal("debug").executes(ctx -> {
                    // 1.2.2: what this mod reads where you stand, and what its links to other mods have done
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var r = com.theoreticalsciences.science.RadiationService.reading(p);
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    lines.add(String.format(java.util.Locale.ROOT, "Theoretical Sciences (mSv/s): carried %.4g, around %.4g (machines %.4g), background %.3g; dose %.1f mSv; machines emitting here: %d",
                            r.carried(), r.around(), r.fromMachines(), r.background(), com.theoreticalsciences.science.RadiationService.dose(p),
                            com.theoreticalsciences.science.RadiationService.emitters(p.serverLevel())));
                    lines.addAll(com.theoreticalsciences.science.RadiationCompat.debug(p));
                    for (String l : lines) ctx.getSource().sendSuccess(() -> Component.literal(l), false);
                    return 1;
                }))
                .then(Commands.literal("cleanup").executes(ctx -> cleanup(ctx.getSource(), 8))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64)).executes(ctx -> cleanup(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetRadiation(ctx.getSource(), java.util.List.of(ctx.getSource().getEntityOrException())))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> resetRadiation(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("get").then(Commands.argument("target", EntityArgument.entity()).executes(ctx -> {
                    var e = EntityArgument.getEntity(ctx, "target");
                    double d = e.getPersistentData().getDouble("theoreticalsciences_rad_dose");
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.get", e.getDisplayName(),
                            String.format(java.util.Locale.ROOT, "%.1f", d)), false);
                    return (int) d;
                })))
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("dose", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0, 100000)).executes(ctx -> {
                            double d = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "dose");
                            int n = 0;
                            for (var e : EntityArgument.getEntities(ctx, "targets")) {
                                if (!(e instanceof net.minecraft.world.entity.LivingEntity)) continue;
                                e.getPersistentData().putDouble("theoreticalsciences_rad_dose", d);
                                n++;
                            }
                            int count = n;
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.set", count,
                                    String.format(java.util.Locale.ROOT, "%.1f", d)), true);
                            return n;
                        }))));
    }

    /** 1.2.3: clears Nuclear Radiation's lingering sources around the player (see RadiationCompat.cleanupLeftovers). */
    private static int cleanup(CommandSourceStack src, int radius) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int n = com.theoreticalsciences.science.RadiationCompat.cleanupLeftovers(p.serverLevel(), p.blockPosition(), radius);
        src.sendSuccess(() -> Component.literal(n < 0 ? "Nuclear Radiation is not installed, or its sources could not be read."
                : "Removed " + n + " lingering Nuclear Radiation sources within " + radius + " blocks."), true);
        return Math.max(0, n);
    }

    private static int resetRadiation(CommandSourceStack src, java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        int n = 0;
        for (var e : targets) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            le.getPersistentData().remove("theoreticalsciences_rad_dose");
            le.removeEffect(com.theoreticalsciences.registry.QHEffects.RADIATION_SICKNESS);
            com.theoreticalsciences.science.RadiationCompat.reset(le);
            n++;
        }
        int count = n;
        src.sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.reset", count), true);
        return n;
    }

    private QHCommands() {}
}
